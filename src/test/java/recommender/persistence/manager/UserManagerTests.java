package recommender.persistence.manager;

import org.hibernate.Session;
import org.junit.*;
import recommender.persistence.entity.User;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class UserManagerTests {
    private static UserManager userManager;
    private static Session session;

    @BeforeClass
    public static void setUp() {
        session = HibernateUtil.openSession();
        userManager = new UserManager(session);
    }

    @AfterClass
    public static void closeUp() {
        session.close();
    }

    @Before
    @After
    public void cleanUp() {
        userManager.listUsers()
                .forEach(
                        user -> userManager.deleteUser(user.getUid())
                );
    }

    @Test
    public void addUserTest() {
        User user = User.builder()
                .username("gustavoaca")
                .build();
        Integer uid = userManager.addUser(user);
        Assert.assertNotNull("User's id is null", uid);
    }

    @Test
    public void getUserTest() {
        User user = User.builder()
                .username("gustavoaca")
                .build();
        Integer uid = userManager.addUser(user);
        User fetchedUser = userManager.getUser(uid);
        Assert.assertEquals("Usernames don't match",
                "gustavoaca", fetchedUser.getUsername());
    }

    @Test(expected = NoSuchElementException.class)
    public void getUserNotFoundTest() {
        userManager.getUser(0);
    }

    @Test
    public void listUsersTest() {
        List<User> createdUsers = Arrays.asList(
                User.builder()
                        .username("gustavoaca")
                        .build(),
                User.builder()
                        .username("lisalfonzo")
                        .build(),
                User.builder()
                        .username("gcastellanos")
                        .build()
        );
        createdUsers.forEach(
                user -> userManager.addUser(user));

        List<User> fetchedUsers = userManager.listUsers();
        Assert.assertNotNull("List of users is null", fetchedUsers);

        List<String> usernames = fetchedUsers.stream()
                .map(User::getUsername).collect(Collectors.toList());

        createdUsers.forEach(
                user ->
                        Assert.assertTrue(
                                String.format("Username %s is not in the list",
                                        user.getUsername()),
                                usernames.contains(user.getUsername())));
    }

    @Test(expected = NoSuchElementException.class)
    public void deleteUserTest() {
        User createdUser = User.builder()
                .username("gustavoaca")
                .build();
        Integer uid = userManager.addUser(createdUser);
        userManager.deleteUser(uid);
        try {
            userManager.getUser(uid);
        } catch (NoSuchElementException e) {
            Assert.assertEquals("Error messages don't match",
                    String.format("User with id %s not found", uid),
                    e.getMessage());
            throw e;
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void deleteUserNotFoundTest() {
        userManager.deleteUser(0);
    }

    @Test
    public void updateUserTest() {
        User createdUser = User.builder()
                .username("gustavoaca")
                .build();
        Integer uid = userManager.addUser(createdUser);
        User updatedUser = User.builder()
                .username("gcastellanos")
                .uid(uid)
                .build();
        userManager.updateUser(updatedUser);

        User fetchedUser = userManager.getUser(uid);
        Assert.assertEquals("Usernames don't match",
                "gcastellanos",
                fetchedUser.getUsername());
    }

    @Test(expected = Exception.class)
    public void updateUserNotFoundTest() {
        User updatedUser = User.builder()
                .username("gcastellanos")
                .uid(0)
                .build();
        userManager.updateUser(updatedUser);
    }
}
