package recommender;

import org.hibernate.Session;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import recommender.persistence.entity.User;
import recommender.persistence.manager.AgingManager;
import recommender.persistence.manager.ClassPropertiesManager;
import recommender.persistence.manager.ContextManager;
import recommender.persistence.manager.HibernateUtil;
import recommender.persistence.manager.UserManager;
import recommender.semantic.network.SemanticNetwork;
import recommender.semantic.util.constants.OntologyConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RecommenderSessionTests {
    private static RecommenderSession recommenderSession;
    private static Session session;

    @BeforeClass
    public static void init() throws IOException {
        session = HibernateUtil.openSession();
        UserManager userManager = new UserManager(session);
        Integer uid = userManager.addUser(User.builder()
                .username("gustavoaca")
                .build());

        SemanticNetwork semanticNetwork = new SemanticNetwork();
        recommenderSession = new RecommenderSession(semanticNetwork, uid);
    }

    @AfterClass
    public static void cleanUp() {
        session.close();
        recommenderSession
                .getUserManager().deleteUser(recommenderSession.getUid());
    }


//    @Test
//    public void initRecommenderSessionTest() {
//        Random rand = new Random();
//        Map<String, Double> initialPreferences = new HashMap<>();
//        recommenderSession
//            .getSemanticNetwork()
//            .getOntModel()
//            .getOntClass(OntologyConstants.PLACE_URI)
//            .listSubClasses(true)
//            .forEachRemaining(
//                    c -> initialPreferences.put(c.getURI(), rand.nextDouble()));
//
//        recommenderSession.init(initialPreferences);
//
//        ClassPropertiesManager propertiesManager = recommenderSession.getPropertiesManager();
//        Integer uid = recommenderSession.getUid();
//
//        initialPreferences.forEach(
//                (uri, pref) -> {
//                    Assert.assertEquals(String.format("Preferences don't match for %s", uri),
//                            pref, propertiesManager.getClassProperties(uri, uid).getPreference());
//                    Assert.assertEquals(String.format("Confidences don't match for %s", uri),
//                            Double.valueOf(1D), propertiesManager.getClassProperties(uri, uid).getConfidence());
//                }
//        );
//        Double prefOfArcheologicalSite =
//                propertiesManager.getClassProperties("https://www.datatourisme.gouv.fr/ontology/core#ArcheologicalSite",
//                        uid).getPreference();
//        Assert.assertTrue("Preference is <= 0 or == 1",
//                prefOfArcheologicalSite > 0 && prefOfArcheologicalSite < 1);
//    }
}
