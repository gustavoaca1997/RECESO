package recommender;

import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import math.GeoLocation;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import recommender.persistence.entity.Aging;
import recommender.persistence.entity.ClassProperties;
import recommender.persistence.entity.Relevance;
import recommender.persistence.entity.User;
import recommender.persistence.manager.AgingManager;
import recommender.persistence.manager.ClassPropertiesManager;
import recommender.persistence.manager.ContextManager;
import recommender.persistence.manager.HibernateUtil;
import recommender.persistence.manager.Proxy;
import recommender.persistence.manager.UserManager;
import recommender.semantic.network.SemanticNetwork;
import recommender.semantic.util.constants.OntologyConstants;

import javax.persistence.NoResultException;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Double.max;
import static org.apache.commons.lang3.ObjectUtils.min;

/**
 * This class represents a session of a user, with connection to the MySQL and Fuseki server.
 */

@Data
@Builder
@AllArgsConstructor
public class RecommenderSession implements Closeable {
    private final UserManager userManager;
    private final ClassPropertiesManager propertiesManager;
    private final ContextManager contextManager;
    private final AgingManager agingManager;
    private final SemanticNetwork semanticNetwork;
    private final Proxy proxyManager;
    private final Session session;

    // TODO: Test differente parameters.
    public static double preferenceCoefficient = 1D/4;
    public static double activationCoefficient = 1D/4;
    public static double agingCoefficient = 17D/36D;
    public static double distanceCoefficient = -1D/36D;
    public static double distance = 8D;

//    public static double preferenceCoefficient = 13D/36;
//    public static double activationCoefficient = 13D/36;
//    public static double agingCoefficient = 1D/4;
//    public static double distanceCoefficient = -1D/36D;
//    public static double distance = 8D;

    // user id
    private final Integer uid;

    // how much does confidence decrease in initial spreading
    private Double decreaseRate;

    // cooperation scale between current values and new values
    private Double coopScale;

    // how much does a place ages for a specific user.
    private Double agingRate;
    // Lower bound of a place's aging value. If value's less or equal than it, restart it to 1.
    private Double agingValueLowerBound;

    private final Set<String> higherClasses;

    private final Map<String, Double> activationMap;

    private Map<String, ClassProperties> propertiesMap;

    public RecommenderSession(SemanticNetwork semanticNetwork,
                              Integer uid) {
        this.session = HibernateUtil.openSession();

        this.userManager = new UserManager(session);
        this.propertiesManager = new ClassPropertiesManager(session);
        this.contextManager = new ContextManager(session);
        this.agingManager = new AgingManager(session);
        this.semanticNetwork = semanticNetwork;

        this.proxyManager = new Proxy(propertiesManager);

        this.uid = uid;
        this.decreaseRate = 0.25;
        this.coopScale = 0.1;
        this.agingRate = 1D/5D;
        this.agingValueLowerBound = 0.1D;
        this.activationMap = new HashMap<>();

        this.higherClasses = new HashSet<>(OntologyConstants.baseUris);
        this.propertiesMap = new HashMap<>();
    }

    @Override
    public void close() {
        session.close();
    }

    public User getUser() {
        return userManager.getUser(uid);
    }

    public void init(Map<String, Double> initialPreferences) {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            initialPreferences.forEach(
                    (uri, pref) ->
                            proxyManager.addClassProperties(
                                    ClassProperties.builder()
                                            .uri(uri)
                                            .user(getUser())
                                            .confidence(1D)
                                            .preference(pref)
                                            .build()));
            OntClass culturalSiteOntClass = semanticNetwork.getOntClass(OntologyConstants.CULTURAL_SITE_URI);
            propagate(culturalSiteOntClass, culturalSiteOntClass.listSubClasses(true));
            downwardsPropagation(OntologyConstants.formUris);

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Computes new confidence of a child node relative to its ancestors.
     *
     * @param ancestors list of ancestors of the node
     * @return (Sum of ancestors' confidences) / (Number of ancestors) - alpha
     */
    private Double aggregatedConfidence(List<ClassProperties> ancestors) {
        Double sumOfConfidences = ancestors.stream()
                .map(ClassProperties::getConfidence)
                .reduce((c1, c2) -> c1 + c2).orElse(0D);
        Integer numberOfAncestors = ancestors.size();
        return max(0, sumOfConfidences / numberOfAncestors - decreaseRate);
    }

    /**
     * Computes new preference of a child node relative to its ancestors.
     *
     * @param ancestors list of ancestors of the node
     * @return (Weighted sum of preferences with confidence)/(sum of confidences)
     */
    private Double aggregatedPreference(List<ClassProperties> ancestors) {
        Double weightedSumOfPreferences = ancestors.stream()
                .map(a -> a.getConfidence() * a.getPreference())
                .reduce((a1, a2) -> a1 + a2).orElse(0D);
        Double sumOfConfidences = ancestors.stream()
                .map(ClassProperties::getConfidence)
                .reduce((c1, c2) -> c1 + c2).orElse(0D);
        return weightedSumOfPreferences / sumOfConfidences;
    }

    /**
     * Computes new preference of a node relative to an assigned new preference.
     *
     * @param oldPreference      current value of the preference
     * @param assignedPreference assigned value
     * @return new preference
     */
    private Double updatedPreference(Double oldPreference, Double assignedPreference) {
        return (1 - coopScale) * oldPreference + coopScale*assignedPreference;
    }

    /**
     * Computes new confidence of a node relative to an assigned new confidence.
     *
     * @param oldConfidence      current value of the confidence
     * @param newConfidence     new or aggregated value of the confidence
     * @return new confidence
     */
    private Double updatedConfidence(Double oldConfidence, Double newConfidence) {
        return (1 - coopScale) * oldConfidence + coopScale*newConfidence;
    }

    //  TODO: test

    /**
     * From user feedback, update properties in the semantic network.
     * @param uri                URI of the origin node
     * @param newPreference     new preference set to the node
     */
    public void updateAndPropagate(String uri, Double newPreference) {
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            // Update preference and confidence
            OntClass ontClass = semanticNetwork.getOntModel().getOntClass(uri);
            ClassProperties properties = proxyManager.getClassProperties(ontClass.getURI(), uid);
            properties.setPreference(updatedPreference(properties.getPreference(), newPreference));
            properties.setConfidence(updatedConfidence(properties.getConfidence(), 1D));
            proxyManager.updateClassProperties(properties);

            downwardsPropagation(Collections.singletonList(uri));

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }


    /**
     * Propagate preference and confidence downwards
     */
    private void downwardsPropagation(Iterator<OntClass> iter) {
        int i = 1;
        while(iter.hasNext()){
            OntClass ontClass = iter.next();
            propagate(ontClass, ontClass.listSuperClasses(true));
            if (i % 20 == 0) session.flush();
            i++;
        }
    }

    private void downwardsPropagation() {
        Iterator<OntClass> iter = semanticNetwork.iterator();
        downwardsPropagation(iter);
    }

    private void downwardsPropagation(List<String> uris) {
        Iterator<OntClass> iter = semanticNetwork.iterator(uris);
        downwardsPropagation(iter);
    }

    /**
     * Propagate preference and confidence to ancestors in semantic network.
     * @param ontClass      source node
     * @param visitedSet    set of visited nodes
     */
    private void upwardsPropagation(OntClass ontClass, Set<OntClass> visitedSet) {
        for (OntClass superClass : ontClass.listSuperClasses(true).toList()) {
            String namespace = ontClass.getNameSpace();
            if (namespace.equals(superClass.getNameSpace()) &&
                    !superClass.getURI().equals(OntologyConstants.PLACE_URI) &&
                    !visitedSet.contains(superClass)
            ) {
                visitedSet.add(superClass);
                propagate(superClass, superClass.listSubClasses(true));
                // TODO: El tope son las clases de baseURIs
                upwardsPropagation(superClass, visitedSet);
            }
        }
    }

    /**
     * Propagate confidence and preference using relative ancestor classes. If one class does not
     * exist in DB, create it.
     * @param ontClass          source node
     * @param ancestorClassesIterator   ancestors of the source node
     */
    private void propagate(OntClass ontClass, ExtendedIterator<OntClass> ancestorClassesIterator) {
        String namespace = ontClass.getNameSpace();
        List<OntClass> ancestorClasses = ancestorClassesIterator.toList();
        if (!( ancestorClasses.stream().map(OntClass::getURI).collect(Collectors.toSet()).contains(OntologyConstants.PLACE_URI) ) &&
                !OntologyConstants.PLACE_URI.equals(ontClass.getURI())
        ) {
            List<ClassProperties> ancestors = filterByNamespace(namespace, ancestorClasses)
                    .stream()
                    .map(OntClass::getURI)
                    .map(uri -> {
                        try {
                            return proxyManager.getClassProperties(uri, uid);
                        } catch (NoResultException | NoSuchElementException e) {
                            return null;
                        }
                    } )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Double confidence = aggregatedConfidence(ancestors);
            Double preference = aggregatedPreference(ancestors);

            try {
                ClassProperties existentProperties =
                        proxyManager.getClassProperties(ontClass.getURI(), uid);

                existentProperties
                        .setConfidence(updatedConfidence(
                                existentProperties.getConfidence(),
                                confidence));
                existentProperties
                        .setPreference(updatedPreference(
                                existentProperties.getPreference(),
                                preference));

                proxyManager.updateClassProperties(existentProperties);

            } catch (NoSuchElementException | NoResultException e) {
                proxyManager.addClassProperties(ClassProperties.builder()
                        .uri(ontClass.getURI())
                        .user(getUser())
                        .confidence(confidence)
                        .preference(preference)
                        .build());
            }
        }
    }

    /**
     * Filter a group of {@code OntClass} by a specific namespace.
     * @param namespace             ontology namespace
     * @param ancestorClasses       ontology classes to filter
     * @return                      list of ontology classes
     */
    private List<OntClass> filterByNamespace(String namespace, List<OntClass> ancestorClasses) {
        return ancestorClasses
                .stream()
                .filter(ontClass1 -> namespace.equals(ontClass1.getNameSpace()))
                .collect(Collectors.toList());
    }

    /**
     * Write to a file state of the semantic network as a JSON.
     * @throws IOException
     */
    public void exportJSON(String filename) throws IOException {
        JSONObject json = toJSONObject(
                semanticNetwork.getOntModel().getOntClass(OntologyConstants.PLACE_URI),
                new HashSet<>());

        try (FileWriter file = new FileWriter(filename)) {
            assert json != null;
            file.write(json.toJSONString());
        }
    }

    /**
     * Recursively transform a node of the network into a JSON object.
     * @param ontClass          node to transform
     * @param visitedSet        set of visited notes for the DFS
     * @return                  A new JSON object representing the sub tree with the node as root.
     */
    private JSONObject toJSONObject(OntClass ontClass, Set<OntClass> visitedSet) {
        JSONObject obj = new JSONObject();
        obj.put("name", ontClass.getURI().split("#")[1]);

        JSONArray children = new JSONArray();
        for (OntClass subClass : ontClass.listSubClasses(true).toList()) {
            String namespace = ontClass.getNameSpace();
            if (subClass.getNameSpace().equals(namespace) &&
                    !visitedSet.contains(subClass)
            ) {
                visitedSet.add(subClass);
                JSONObject child = toJSONObject(subClass, visitedSet);
                if (child != null) children.add(child);
            }
        }

        obj.put("subclasses", children);

        if (!ontClass.getURI().equals(OntologyConstants.PLACE_URI)) {
            try {
                ClassProperties properties = proxyManager.getClassProperties(ontClass.getURI(), uid);
                obj.put("preference", properties.getPreference());
                obj.put("confidence", properties.getConfidence());
            } catch (NoResultException | NoSuchElementException e) {
                return null;
            }
        }

        return obj;
    }

    /**
     * Return a list of {@code OntClass} based on their preference and confidence and
     * taking into account the fulfillment of each context factor.
     * @param fulfillment       A map of {@code ContextFactor} to level of fulfillment.
     * @return                  A list ordered by activion * preference * confidence.
     */
    private Map<String, Pair<Double, Double> > getClassRatings(Map<String, Double> fulfillment) {
        resetActivations();

        // Update activations for ontology classes related explicitly with the
        // context factors.
        List<Relevance> relevanceList = contextManager.listRelevancesByUserId(uid);
        relevanceList.forEach(r ->
                activationMap.put(
                        r.getUri(),
                        activationMap.get(r.getUri()) + r.getValue() * fulfillment.get(r.getContextFactor().getName())));

        spreadActivation();

        // Filter not sink nodes out
        List<Map.Entry<String, Double> > entryList = new ArrayList<>(activationMap.entrySet());
        Map<String, Double> sinkActivationMap = entryList
                .stream()
                .filter(entry ->
                        semanticNetwork
                                .getOntClass(entry.getKey())
                                .listSubClasses(true)
                                .toList()
                                .isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Add preference and confidence
        Map<String, ClassProperties> classPropertiesMap = proxyManager.listClassPropertiesByUserAsMap(uid);
        Map<String, Pair<Double, Double> > ratingsMap = new HashMap<>();
        for ( Map.Entry<String, Double> entry : sinkActivationMap.entrySet() ) {
            if (entry.getKey().equals(OntologyConstants.PLACE_URI)) continue;

            ClassProperties properties = classPropertiesMap.get(entry.getKey());
            ratingsMap.put(entry.getKey(), new Pair<>(entry.getValue(), properties.getPreference()));
        }

        // return OntClasses
        return ratingsMap;

    }

    private List<RDFResult> getIndividualsInsideRadius(
            Map<String, Pair<Double, Double> > classRatings, double latitude, double longitude, double distance
    ) {
        // Get results of specific class inside radius.
        GeoLocation center = GeoLocation.fromDegrees(latitude, longitude);
        GeoLocation[] boundingCoords = center.boundingCoordinates(distance, GeoLocation.EARTH_RAD);
        GeoLocation minBound = boundingCoords[0], maxBound = boundingCoords[1];

        List<String> uriSuperClasses = new ArrayList<>(classRatings.keySet());
        List<RDFResult> results = IndividualsService
                .getResults(uriSuperClasses, maxBound, minBound);

        // Set distance, predicted rating and aging for user to each place.
        results.forEach(r -> {
                    // Set distance
                    GeoLocation loc = GeoLocation.fromDegrees(r.getLatitude(), r.getLongitude());
                    r.setDistance(center.distanceTo(loc, GeoLocation.EARTH_RAD));

                    // Set predicted rating
                    String uriClass = r.getUriClass();
                    Double predictedPreference = classRatings.get(uriClass).getValue();
                    r.setPredictedPreference(predictedPreference);

                    // Set activation
                    Double activation = classRatings.get(uriClass).getKey();
                    r.setActivation(activation);
                    // Set aging value
                    r.setAgingValue(agingManager.getAgingValue(r.getUri(), uid));
                });

        // Filter places inside specified radius.
        return results
                    .stream()
                    .filter(r -> r.getDistance() <= distance)
                    .collect(Collectors.toList());
    }

    public List<RDFResult> getRecommendedIndividuals(
            Map<String, Double> fulfillment, double latitude,
            double longitude, double distance
    ) {
        RecommenderSession.distance = distance;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Map<String, Pair<Double, Double> > classRatings = getClassRatings(fulfillment);
            List<RDFResult> results = getIndividualsInsideRadius(classRatings, latitude, longitude, distance);

            // TODO: Remove duplicates
            // Sort places by their aging value.
            results.sort((r1, r2) -> {
                Double left = r2.score();
                Double right = r1.score();
                return left.compareTo(right);
            });

            results = results.subList(0, min(5, results.size()));

            // Update aging
            results.forEach(r -> {
                String puri = r.getUri();
                User user = userManager.getUser(uid);

                Optional<Aging> optionalAging = agingManager.getAging(puri, uid);
                // If it's not present, add it with new value.
                if (!optionalAging.isPresent()) {
                    agingManager.addAging(Aging.builder()
                            .puri(r.getUri())
                            .user(user)
                            .value(1 - agingRate)
                            .build());
                }
                // Update it with new value or delete it if lower bound is reached.
                else {
                    Aging aging = optionalAging.get();
                    Double newValue = aging.getValue() - agingRate;
                    if (newValue <= agingValueLowerBound) {
                        agingManager.deleteAging(aging.getAid());
                    } else {
                        aging.setValue(newValue);
                        agingManager.updateAging(aging);
                    }
                }
            });

            tx.commit();

            return results;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Spread activation to a node relative to its ancestors.
     * @param ontClass          node to set activation
     * @param ancestorClasses   ancestors of the node
     */
    private void activate(OntClass ontClass, ExtendedIterator<OntClass> ancestorClasses) {
        String namespace = ontClass.getNameSpace();
        List<OntClass> ancestors = filterByNamespace(namespace, ancestorClasses.toList());
        Double currActivation = activationMap.getOrDefault(ontClass.getURI(), 0D);
        Double add = ancestors.stream()
                .map(OntClass::getURI)
                .map(a -> activationMap.getOrDefault(a, 0D))
                .reduce(0D, (x,y) -> x+y);
        activationMap.put(ontClass.getURI(), currActivation + add/ancestors.size());
    }

    /**
     * Turn all activations into 0.
     */
    private void resetActivations() {
        @SuppressWarnings("unchecked") Iterable<OntClass> iterable = (Iterable<OntClass >) semanticNetwork;
        for (OntClass ontClass : iterable) {
            activationMap.put(ontClass.getURI(), 0D);
        }
    }

    /**
     * Spread activation to the network.
     */
    private void spreadActivation() {
        @SuppressWarnings("unchecked") Iterable<OntClass> iterable = (Iterable<OntClass >) semanticNetwork;
        for (OntClass ontClass : iterable) {
            activate(ontClass, ontClass.listSuperClasses(true));
        }
    }

}
