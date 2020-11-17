package recommender.persistence.manager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import recommender.persistence.entity.User;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class UserManager {
    private final Session session;
    public UserManager(Session session) {
        this.session = session;
    }

    /**
     * Add a new user with `username`.
     *
     * @param user an entity with the user's fields
     * @return New user's id
     */
    public Integer addUser(User user) {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            user.setClassPropertiesSet(new HashSet<>());
            user.setRelevanceSet(new HashSet<>());
            user.setAgingSet(new HashSet<>());

            session.save(user);
            Integer userID = user.getUid();
            assert (userID != null);

            tx.commit();
            return userID;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * List all users.
     *
     * @return List of {@link User User}
     */
    @SuppressWarnings("unchecked")
    public List<User> listUsers() {
        List<User> users;
        users = session.createQuery("FROM User").list();

        return users;
    }

    /**
     * Get user by id.
     *
     * @param uid unique identifier of the user.
     * @return A {@link Optional<User> User} corresponding to the user.
     * @throws NoSuchElementException if there is not such user.
     */
    public User getUser(Integer uid) {
        User user = session.get(User.class, uid);
        if (user == null) {
            throw new
                    NoSuchElementException(String.format(
                    "User with id %s not found",
                    uid
            ));
        }
        return user;
    }

    /**
     * Get user by username.
     *
     * @param username username of the user.
     * @return User entity
     * @throws NoSuchElementException if there is not such user.
     */
    public User getUser(String username) {
        User user;

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = cb.createQuery(User.class);
        Root<User> root = criteriaQuery.from(User.class);
        criteriaQuery.select(root).where(cb.like(root.get("username"), username));
        Query<User> query = session.createQuery(criteriaQuery);

        user = query.getSingleResult();

        if (user == null) {
            throw new
                    NoSuchElementException(String.format(
                    "User with username %s not found",
                    username
            ));
        }

        return user;
    }

    /**
     * Delete user by id
     * @param uid id of user to be deleted
     */
    public void deleteUser(Integer uid) {
        User user = Optional.ofNullable(session.get(User.class, uid))
                .orElseThrow(NoSuchElementException::new);
        session.delete(user);
    }

    /**
     * Update user by id
     *
     * @param updatedUser an object with new fields
     * @throws NoSuchElementException if there is not such user
     */
    void updateUser(User updatedUser) throws NoSuchElementException {
        Integer uid = updatedUser.getUid();
        if (session.get(User.class, uid) == null) {
            throw new NoSuchElementException(
                    String.format(
                            "User with id %s not found",
                            uid));
        }
        session.merge(updatedUser);
    }
}
