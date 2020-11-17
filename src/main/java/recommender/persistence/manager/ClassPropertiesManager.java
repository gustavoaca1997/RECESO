package recommender.persistence.manager;


import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import recommender.persistence.entity.ClassProperties;
import recommender.persistence.entity.User;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class ClassPropertiesManager {
    private final Session session;
    public ClassPropertiesManager(Session session) {
        this.session = session;
    }

    public Integer addClassProperties(ClassProperties classProperties) {
        Integer pid;
        Integer uid = classProperties.getUser().getUid();
        User userEntity = session.get(User.class, uid);
        classProperties.setUser(userEntity);
        Set<ClassProperties> classPropertiesSet =
                userEntity.getClassPropertiesSet();
        classPropertiesSet.add(classProperties);

        session.persist(classProperties);
        pid = classProperties.getPid();
        assert (pid != null);

        session.update(userEntity);
        return pid;
    }

    public  Set<ClassProperties> listClassPropertiesByUserAsSet(Integer uid) {
        session.flush();
        User user = session.get(User.class, uid);
        return user.getClassPropertiesSet();
    }

    public Map<String, ClassProperties> listClassPropertiesByUserAsMap(Integer uid) {
        session.flush();
        Map<String, ClassProperties> classPropertiesMap = new HashMap<>();
        User user = session.get(User.class, uid);
        Set<ClassProperties> classPropertiesSet = user.getClassPropertiesSet();
        classPropertiesSet.forEach(
                p -> classPropertiesMap.put(p.getUri(), p)
        );
        return classPropertiesMap;
    }

    public ClassProperties getClassProperties(Integer pid) {
        ClassProperties classProperties = session.get(ClassProperties.class, pid);
        if (classProperties == null) {
            throw new NoSuchElementException(
                    String.format("Class props with id %s not found", pid)
            );
        }
        return classProperties;
    }

    public ClassProperties getClassProperties(String uri, Integer uid) {
        User user = session.get(User.class, uid);
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ClassProperties> criteriaQuery = cb.createQuery(ClassProperties.class);
        Root<ClassProperties> root = criteriaQuery.from(ClassProperties.class);
        Predicate predicate = cb.and(
                cb.like(root.get("uri"), uri),
                cb.equal(root.get("user"), user)
        );
        criteriaQuery.select(root).where(predicate);
        Query<ClassProperties> query = session.createQuery(criteriaQuery);
        ClassProperties classProperties = query.getSingleResult();

        if (classProperties == null) {
            throw new NoSuchElementException(
                    String.format(
                            "Class Properties for class %s and user %s not found",
                            uri, uid));
        }
        return classProperties;
    }

    public void updateClassProperties(ClassProperties updatedProperties) {
        ClassProperties props;
        Integer pid = updatedProperties.getPid();
        if (pid != null) {
            props = session.get(ClassProperties.class, pid);
        } else {
            Integer uid = updatedProperties.getUser().getUid();
            String uri = updatedProperties.getUri();
            props = getClassProperties(uri, uid);
        }
        if (props == null) {
            throw new NoSuchElementException("Class props not found");
        }
        props.setPreference(updatedProperties.getPreference());
        props.setConfidence(updatedProperties.getConfidence());
        session.update(props);
    }

    void deleteClassProperties(Integer pid) {
        ClassProperties classProperties = session.get(ClassProperties.class, pid);
        if (classProperties == null) {
            throw new NoSuchElementException(
                    String.format(
                            "Class Properties with id %s not found",
                            pid));
        }
        Integer uid = classProperties.getUser().getUid();
        User user = session.get(User.class, uid);
        user.getClassPropertiesSet().remove(classProperties);
        session.delete(classProperties);
    }
}
