package recommender.persistence.manager;

import org.hibernate.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import recommender.persistence.entity.ContextFactor;
import recommender.persistence.entity.Relevance;
import recommender.persistence.entity.User;

import java.util.HashSet;

public class ContextFactorTest {
    private static ContextManager contextManager;
    private static UserManager userManager;
    private static User admin;
    private static Session session;

    @BeforeClass
    public static void setUp() {
        session = HibernateUtil.openSession();
        contextManager = new ContextManager(session);
        userManager = new UserManager(session);
        admin = User.builder()
                .username("admin")
                .classPropertiesSet(new HashSet<>())
                .build();
        admin.setUid(userManager.addUser(admin));
    }

    @AfterClass
    public static void closeUp() {
        session.close();
    }

    @AfterClass
    public static void cleanUp() {
        userManager.deleteUser(admin.getUid());
    }

    @After
    public void cleanRelevances() {
        contextManager.listContextFactors()
                .stream()
                .map(ContextFactor::getCid)
                .forEach(cid -> contextManager.deleteContextFactor(cid));
    }

    @Test
    public void addContextFactor() {
        ContextFactor contextFactor = ContextFactor.builder()
                .name("temperature")
                .build();
        Integer cid = contextManager.addContextFactor(contextFactor);
        Assert.assertNotNull("Id is null", cid);
    }

    @Test(expected = IllegalStateException.class)
    public void addDuplicateContextFactor() throws IllegalStateException {
        ContextFactor contextFactor0 = ContextFactor.builder()
                .name("happiness")
                .build();
        contextManager.addContextFactor(contextFactor0);
        ContextFactor contextFactor1 = ContextFactor.builder()
                .name("happiness")
                .build();
        contextManager.addContextFactor(contextFactor1);
        Assert.fail();
    }

    @Test
    public void addRelevance() {
        String uri = "www.com";
        ContextFactor contextFactor = ContextFactor.builder()
                .name("weather")
                .build();
        contextFactor.setCid(contextManager.addContextFactor(contextFactor));
        Relevance relevance = Relevance.builder()
                .contextFactor(contextFactor)
                .uri(uri)
                .user(admin)
                .value(12.5D)
                .build();
        Integer rid = contextManager.addRelevance(relevance);
        Assert.assertNotNull("Id is null", rid);
    }

    @Test
    public void deleteRelevance() {
        String uri = "www.com";
        ContextFactor contextFactor = ContextFactor.builder()
                .name("weather")
                .build();
        contextFactor.setCid(contextManager.addContextFactor(contextFactor));
        Relevance relevance = Relevance.builder()
                .contextFactor(contextFactor)
                .uri(uri)
                .user(admin)
                .value(12.5D)
                .build();
        Integer rid = contextManager.addRelevance(relevance);
        Assert.assertNotNull("Id is null", rid);

        contextManager.deleteRelevance(rid);
        ContextFactor getCF = contextManager.getContextFactor(contextFactor.getCid());
        Assert.assertTrue("Set not empty", getCF.getRelevanceSet().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void addDuplicateRelevance() {
        String uri = "www.com";
        ContextFactor contextFactor = ContextFactor.builder()
                .name("weather")
                .build();
        contextFactor.setCid(contextManager.addContextFactor(contextFactor));
        Relevance relevance = Relevance.builder()
                .contextFactor(contextFactor)
                .uri(uri)
                .user(admin)
                .value(12.5D)
                .build();
        contextManager.addRelevance(relevance);
        contextManager.addRelevance(Relevance.builder()
                .contextFactor(contextFactor)
                .uri(uri)
                .user(admin)
                .value(800.78D)
                .build());
        Assert.fail();
    }
}
