import javafx.util.Pair;
import org.apache.jena.ontology.OntClass;
import org.hibernate.Session;
import recommender.RDFResult;
import recommender.RecommenderSession;
import recommender.persistence.entity.User;
import recommender.persistence.manager.AgingManager;
import recommender.persistence.manager.ClassPropertiesManager;
import recommender.persistence.manager.ContextManager;
import recommender.persistence.manager.HibernateUtil;
import recommender.persistence.manager.UserManager;
import recommender.semantic.network.SemanticNetwork;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Analysis {
    private static SemanticNetwork semanticNetwork;
    private static List<User> users;
    private static ContextManager contextManager;
    private static UserManager userManager;
    private static List<String> weather = Arrays.asList("rainy", "sunny", "snowy");
    private static List<String> day = Arrays.asList("workday", "weekend");
    private static List<String> time = Arrays.asList("morning", "afternoon", "night", "early_morning");

    private static List<List<Double>> samples = Arrays.asList(
        Arrays.asList(0.7235294117647059,0.6823529411764706,0.5588235294117647,0.8647058823529412,0.6882352941176471,0.45882352941176474,0.6529411764705882,0.7411764705882353,0.5411764705882354,0.49411764705882355,0.40588235294117647,0.9058823529411765,0.9058823529411765,0.9352941176470588, 0.9352941176470588,0.7647058823529412),
        Arrays.asList(0.6476190476190476,0.6476190476190476,0.4666666666666667,0.8714285714285714,0.861904761904762,0.7,0.8523809523809524,0.9190476190476191,0.8523809523809524,0.780952380952381,0.7904761904761904,0.8904761904761904,0.9142857142857143,0.8428571428571429, 0.8428571428571429,0.8285714285714286),
        Arrays.asList(0.78,0.52,0.72,0.7,0.82,0.36000000000000004,0.4,0.58,0.68,0.8,0.56,0.7000000000000001,0.76,0.21999999999999997, 0.21999999999999997,0.27999999999999997),
        Arrays.asList(0.9421052631578948,0.8736842105263158,0.7684210526315789,0.9315789473684211,0.8263157894736842,0.7315789473684211,0.7894736842105263,0.8631578947368421,0.7421052631578947,0.7210526315789474,0.5842105263157895,0.6631578947368421,0.9210526315789473,0.631578947368421, 0.631578947368421,0.5684210526315789)
    );

    private static void createAndAddUsers() {
        users = new ArrayList<>();
        for (int i=0; i<samples.size(); i++) {
            String pref = "turista_" + Instant.now();
            String username = pref + i;
            User user;
            try {
                user = userManager.getUser(username);
            } catch (NoSuchElementException | NoResultException e) {
                userManager.addUser(User.builder().username(username).build());
                user = userManager.getUser(username);
                Main.loadRelevances(user, contextManager);
            }
            users.add(user);
        }
    }

    private static Integer distance(OntClass recommendedClass, OntClass newRecommendedClass) {
        Set<OntClass> visitedSet = new HashSet<>();
        Queue<Pair<Integer, OntClass>> queue = new LinkedList<>();
        queue.add(new Pair<>(0, newRecommendedClass));
        while (!queue.isEmpty()) {
            Pair<Integer, OntClass> top = queue.poll();
            Integer topD = top.getKey();
            OntClass topClass = top.getValue();
            if (recommendedClass.equals(topClass)) {
                return topD;
            }
            visitedSet.add(topClass);
            List<OntClass> superClasses = topClass.listSuperClasses(true).toList();
            List<OntClass> subClasses = topClass.listSubClasses(true).toList();
            Stream.concat(superClasses.stream(), subClasses.stream())
                    .filter(c -> !visitedSet.contains(c))
                    .forEach(c -> queue.add(new Pair<>(topD+1, c)));
        }
        return 0;
    }

    private static Integer novelty(Set<OntClass> recommendedClasses, OntClass newRecommendedClass ) {
        if (recommendedClasses.contains(newRecommendedClass))
            return 0;
        return recommendedClasses
                .stream()
                .map(c -> distance(c, newRecommendedClass))
                .min(Integer::compareTo)
                .orElse(0);
    }

    public static void main(String[] args) throws IOException {
        Session session = HibernateUtil.openSession();
        userManager = new UserManager(session);
        ClassPropertiesManager propertiesManager = new ClassPropertiesManager(session);
        contextManager = new ContextManager(session);
        AgingManager agingManager = new AgingManager(session);
        semanticNetwork = new SemanticNetwork();

        Main.loadContextFactors(contextManager);

        Map<String, List<Double>> coordinates = new HashMap<>();
        coordinates.put("Paris", Arrays.asList(48.8589507, 2.2770196));
        coordinates.put("Lyon", Arrays.asList(45.7580539, 4.7650805));
        coordinates.put("Niza", Arrays.asList(43.709622, 7.262428));

        createAndAddUsers();

        // For each sample user
        for (int i=0; i<samples.size(); i++) {
            User user = users.get(i);
            List<Double> sample = samples.get(i);

            System.out.print(" \\hline $T_" + (i+1) + "$ ");

            Set<OntClass> recommendedClasses = new HashSet<>();
            double totalAvgPredictedPreference = 0D;
            double totalAvgActivation = 0D;
            double totalAvgAging = 0D;
            double totalAvgNovelty = 0D;
            double totalAvgDistance = 0D;
            int count = 0;

            RecommenderSession recommenderSession = new RecommenderSession(semanticNetwork, user.getUid());

            // Add their initial
            Map<String, Double> initialPreference = new HashMap<>();
            for (int j=0; j<Main.baseUris.size(); j++) {
                String uri = Main.baseUris.get(j);
                initialPreference.put(uri, sample.get(j));
            }
            recommenderSession.init(initialPreference);

            // For each possible context factors combination, visit two times each location
            for (int k=0; k<2; k++) {
                System.out.print(" & " + (k+1) + " ");

                int context_idx = 0;
                for (String d : day) {
                    for (String w : weather) {
                        for (String t : time) {
                            Map<String, Double> fullfillment = new HashMap<>();
                            for (String cf : Main.contextFactors) {
                                fullfillment.put(cf, 0D);
                            }
                            fullfillment.put(w, 1D);
                            fullfillment.put(d, 1D);
                            fullfillment.put(t, 1D);

                            if (context_idx > 0) {
                                System.out.print(" & ");
                            }
                            context_idx += 1;
                            System.out.print(String.format(" & \\scriptsize{%s} ", w));

                            int location_idx = 0;
                            for (Map.Entry<String, List<Double>> entry : coordinates.entrySet()) {
                                if (location_idx>0) {
                                    System.out.print(" & & ");
                                }
                                location_idx += 1;
                                System.out.print(" & \\scriptsize{" + entry.getKey() + "} ");

                                // Get recommended individuals
                                List<Double> c = entry.getValue();
                                double distance = 8D;
                                List<RDFResult> recommendations =
                                        recommenderSession
                                                .getRecommendedIndividuals(fullfillment, c.get(0), c.get(1), distance);

                                int r_idx=0;
                                for (RDFResult r : recommendations) {
                                    if (r_idx > 0) {
                                        String contextFactorStr = "";
                                        if (location_idx == 1 && r_idx == 1) contextFactorStr = d;
                                        else if (location_idx == 1 && r_idx == 2) contextFactorStr = t;
                                        System.out.print(String.format(" & & \\scriptsize{%s} & ", contextFactorStr));
                                    }
                                    System.out.print(" & " + r.toLatexItem());
                                    r_idx += 1;

                                    System.out.print(" \\\\ ");

                                    if (r_idx == recommendations.size()) {
                                        int first_col=4;
                                        if (location_idx == coordinates.size()) {
                                            first_col -= 1;

                                            if (context_idx == 24) {
                                                first_col -= 1;

                                                if (k == 1) {
                                                    first_col -= 1;
                                                }
                                            }
                                        }

                                        System.out.println(String.format(" \\cline{%s-11} ", first_col));
                                    } else {
                                        System.out.println(" \\cline{5-11} ");
                                    }
                                }

                                if (!recommendations.isEmpty()) {
                                    count += 1;

                                    Double avgPredictedPreference = recommendations
                                            .stream()
                                            .map(RDFResult::getPredictedPreference)
                                            .reduce(Double::sum)
                                            .get() / recommendations.size();
                                    totalAvgPredictedPreference += avgPredictedPreference;

                                    Double avgActivation = recommendations
                                            .stream()
                                            .map(RDFResult::getActivation)
                                            .reduce(Double::sum)
                                            .get() / recommendations.size();
                                    totalAvgActivation += avgActivation;

                                    Double avgAging = recommendations
                                            .stream()
                                            .map(RDFResult::getAgingValue)
                                            .reduce(Double::sum)
                                            .get() / recommendations.size();
                                    totalAvgAging += avgAging;

                                    Double avgNovelty = (double) recommendations
                                            .stream()
                                            .map(RDFResult::getUriClass)
                                            .map(uri -> semanticNetwork.getOntClass(uri))
                                            .map(rc -> novelty(recommendedClasses, rc))
                                            .reduce(Integer::sum)
                                            .get() / recommendations.size();
                                    totalAvgNovelty += avgNovelty;

                                    Double avgDistance = recommendations
                                            .stream()
                                            .map(RDFResult::getDistance)
                                            .reduce(Double::sum)
                                            .get() / recommendations.size();
                                    totalAvgDistance += avgDistance;

//                                    System.out.println(String.format(
//                                            "\n\t\t\t\\item $avg_{pref_c}$: $%s$, $avg_{act_c}$: $%s$, $avg_{\\eta_p}: $%s$, " +
//                                                    "$avg novelty: %s, avg distance: %s\n",
//                                            avgPredictedPreference, avgActivation, avgAging, avgNovelty, avgDistance
//                                    ));

                                    //recommenderSession.updateAndPropagate(topResult.getUriClass(), 1D);
                                }

                                // Track recommendend ontology classes
                                recommendedClasses.addAll(recommendations
                                        .stream()
                                        .map(RDFResult::getUriClass)
                                        .map(uri -> semanticNetwork.getOntClass(uri))
                                        .collect(Collectors.toList()));
                            }
                        }
                    }
                }
            }

//            System.out.println("\\subsection{Resumen}");
//            System.out.println(String.format("\n\t\\begin{itemize}" +
//                            "\\item $avg(pref_R)$: $%s$\n" +
//                            "\\item $avg(act_R)$: $%s$\n" +
//                            "\\item $avg(\\eta_R)$: $%s$\n" +
//                            "\\item $avg(nov_R)$: $%s$\n" +
//                            "\\item $avg(dist_R)$ $%s$\n" +
//                            "\n\t\\end{itemize}\n",
//                    totalAvgPredictedPreference/count, totalAvgActivation/count, totalAvgAging/count,
//                    totalAvgNovelty/(count-1), totalAvgDistance/count));
            recommenderSession.exportJSON(String.format("%s_dump.json", i));
            recommenderSession.close();
        }
    }
}
