package recommender.persistence.manager;


import org.hibernate.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import recommender.persistence.entity.ClassProperties;
import recommender.persistence.entity.User;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassPropertiesTest {
    private static ClassPropertiesManager classPropertiesManager;
    private static UserManager userManager;
    private static User admin;
    private static Session session;

    @BeforeClass
    public static void setUp() {
        session = HibernateUtil.openSession();
        classPropertiesManager = new ClassPropertiesManager(session);
        userManager = new UserManager(session);
        admin = User.builder().username("admin")
                .classPropertiesSet(new HashSet<>()).build();
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
    public void cleanPropertiesUp() {
        User user = userManager.getUser(admin.getUid());
        Set<ClassProperties> set = classPropertiesManager.listClassPropertiesByUserAsSet(user.getUid());
        set.forEach(
                props -> classPropertiesManager.deleteClassProperties(props.getPid())
        );
    }

    @Test
    public void addPropsTest() {
        User user = userManager.getUser(admin.getUid());
        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        Assert.assertNotNull("Id is null", props.getPid());
    }

    @Test
    public void getPropsByIdTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        ClassProperties fetchedProps = classPropertiesManager.getClassProperties(props.getPid());
        Assert.assertEquals("Users are not equal",
                props.getUser().getUsername(), fetchedProps.getUser().getUsername());
        Assert.assertEquals("URIs don't match", props.getUri(), fetchedProps.getUri());
        Assert.assertEquals("Preference don't match",
                props.getPreference(), fetchedProps.getPreference());
        Assert.assertEquals("Confidence don't match",
                props.getConfidence(), fetchedProps.getConfidence());
    }

    @Test
    public void getPropsByUriTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        ClassProperties fetchedProps = classPropertiesManager
                .getClassProperties(props.getUri(), props.getUser().getUid());
        Assert.assertEquals("Users are not equal",
                props.getUser().getUsername(), fetchedProps.getUser().getUsername());
        Assert.assertEquals("URIs don't match", props.getUri(), fetchedProps.getUri());
        Assert.assertEquals("Preference don't match",
                props.getPreference(), fetchedProps.getPreference());
        Assert.assertEquals("Confidence don't match",
                props.getConfidence(), fetchedProps.getConfidence());
    }

    @Test
    public void listPropsTest() {
        User user = userManager.getUser(admin.getUid());

        List<ClassProperties> createdPropsList = Arrays.asList(
                ClassProperties.builder()
                        .user(user)
                        .uri("uri.gov")
                        .confidence(0.)
                        .preference(0.)
                        .build(),
                ClassProperties.builder()
                        .user(user)
                        .uri("uri.com")
                        .confidence(0.)
                        .preference(0.)
                        .build(),
                ClassProperties.builder()
                        .user(user)
                        .uri("uri.org")
                        .confidence(0.)
                        .preference(0.)
                        .build()
        );
        createdPropsList.forEach(classPropertiesManager::addClassProperties);
        List<String> fetchedPropsList = classPropertiesManager.listClassPropertiesByUserAsSet(admin.getUid())
                .stream().map(ClassProperties::getUri).collect(Collectors.toList());
        Assert.assertNotNull("Fetched Props are null", fetchedPropsList);
        Assert.assertEquals("List don't match", 3, fetchedPropsList.size());
        createdPropsList.forEach(
                props -> Assert.assertTrue(
                        String.format("Props for %s is not in fetched props", props.getUri()),
                        fetchedPropsList.contains(props.getUri()))
        );
    }

    @Test
    public void updatePropsTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        props.setConfidence(1.0);
        classPropertiesManager.updateClassProperties(props);

        ClassProperties fetchedProps = classPropertiesManager.getClassProperties(props.getPid());
        Assert.assertNotNull("Fetched props is null", fetchedProps);
        Assert.assertEquals("Users are not equal",
                props.getUser().getUsername(), fetchedProps.getUser().getUsername());
        Assert.assertEquals("URIs don't match", props.getUri(), fetchedProps.getUri());
        Assert.assertEquals("Preference don't match",
                props.getPreference(), fetchedProps.getPreference());
        Assert.assertEquals("Confidence don't match",
                props.getConfidence(), fetchedProps.getConfidence());
    }

    @Test(expected = NoSuchElementException.class)
    public void deletePropsTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        classPropertiesManager.deleteClassProperties(props.getPid());
        ClassProperties fetchedProps = null;
        try {
            fetchedProps = classPropertiesManager.getClassProperties(props.getPid());
            Assert.fail();
        } catch (NoSuchElementException e) {
            Assert.assertNull("Fetched props is not null", fetchedProps);
            throw e;
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void deletePropsByIdTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .user(user)
                .uri("uri.com")
                .confidence(0.)
                .preference(0.)
                .build();
        props.setPid(classPropertiesManager.addClassProperties(props));
        classPropertiesManager.deleteClassProperties(props.getPid());
        ClassProperties fetchedProps = null;
        try {
            fetchedProps = classPropertiesManager.getClassProperties(props.getPid());
        } catch (NoSuchElementException e) {
            Assert.assertNull("Fetched props is not null", fetchedProps);
            throw e;
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void deletePropsNotFoundTest() {
        classPropertiesManager.deleteClassProperties(0);
    }

    @Test(expected = Exception.class)
    public void updatePropsNotFoundTest() {
        User user = userManager.getUser(admin.getUid());

        ClassProperties props = ClassProperties.builder()
                .pid(0)
                .uri("uri.com")
                .user(user)
                .preference(0.)
                .confidence(0.)
                .build();
        classPropertiesManager.updateClassProperties(props);
    }

    @Test(expected = NoSuchElementException.class)
    public void getPropsNotFoundTest() {
        classPropertiesManager.getClassProperties(0);
    }
}
