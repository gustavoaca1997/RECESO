package recommender.persistence.manager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import recommender.persistence.entity.ContextFactor;
import recommender.persistence.entity.Relevance;
import recommender.persistence.entity.User;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

public class ContextManager {
    private final Session session;
    public ContextManager(Session session) {
        this.session = session;
    }

    public Integer addContextFactor(ContextFactor contextFactor) {
        Transaction tx = null;
        try {
            try {
                contextFactor = getContextFactor(contextFactor.getName());
                return contextFactor.getCid();
            } catch (NoSuchElementException e) {
                tx = session.beginTransaction();

                contextFactor.setRelevanceSet(new HashSet<>());
                session.save(contextFactor);
                assert(contextFactor.getCid() != null);

                tx.commit();
                return contextFactor.getCid();
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public Integer addRelevance(Relevance relevance) {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Integer rid;

            Integer cid = relevance.getContextFactor().getCid();
            ContextFactor contextFactor = session.get(ContextFactor.class, cid);
            relevance.setContextFactor(contextFactor);

            User user = session.get(User.class, relevance.getUser().getUid());
            relevance.setUser(user);
            contextFactor.getRelevanceSet().add(relevance);
            user.getRelevanceSet().add(relevance);

            session.save(relevance);
            rid = relevance.getRid();
            assert (rid != null);

            session.update(contextFactor);
            session.update(user);

            tx.commit();
            return rid;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public ContextFactor getContextFactor(Integer cid) {
        ContextFactor contextFactor = session.get(ContextFactor.class, cid);
        if (contextFactor == null) {
            throw new NoSuchElementException(
                    String.format("Context factor with id %s not found", cid)
            );
        }
        return contextFactor;

    }

    public ContextFactor getContextFactor(String name) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ContextFactor> criteriaQuery = cb.createQuery(ContextFactor.class);
        Root<ContextFactor> root = criteriaQuery.from(ContextFactor.class);
        Predicate predicate = cb.like(root.get("name"), name);
        criteriaQuery.select(root).where(predicate);
        Query<ContextFactor> query = session.createQuery(criteriaQuery);

        ContextFactor contextFactor = query.getSingleResult();

        if (contextFactor == null) {
            throw new NoSuchElementException(
                    String.format("Context factor with name %s not found", name)
            );
        }
        return contextFactor;

    }

    public Relevance getRelevance(Integer rid) {
        Relevance relevance = session.get(Relevance.class, rid);
        if (relevance == null) {
            throw new NoSuchElementException(
                    String.format("Relevance with id %s not found", rid)
            );
        }
        return relevance;
    }

    public Relevance getRelevance(String uri, Integer cid, Integer uid) {
        ContextFactor contextFactor = session.get(ContextFactor.class, cid);
        User user = session.get(User.class, uid);

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Relevance> criteriaQuery = cb.createQuery(Relevance.class);
        Root<Relevance> root = criteriaQuery.from(Relevance.class);
        Predicate predicate = cb.and(
                cb.like(root.get("uri"), uri),
                cb.equal(root.get("contextFactor"), contextFactor),
                cb.equal(root.get("user"), user)
        );
        criteriaQuery.select(root).where(predicate);
        Query<Relevance> query = session.createQuery(criteriaQuery);

        Relevance relevance = query.getSingleResult();

        if (relevance == null) {
            throw new NoSuchElementException(
                    String.format(
                            "Relevance for class %s and context factor %s not found",
                            uri, cid));
        }
        return relevance;
    }

    public void updateRelevance(Relevance updateRelevance) {
        Relevance relevance;
        Integer rid = updateRelevance.getRid();
        if (rid != null) {
            relevance = session.get(Relevance.class, rid);
        } else {
            String uri = updateRelevance.getUri();
            Integer cid = updateRelevance.getContextFactor().getCid();
            Integer uid = updateRelevance.getUser().getUid();
            relevance = getRelevance(uri, cid, uid);
        }
        if (relevance == null) {
            throw new NoSuchElementException("Relevance not found");
        }
        updateRelevance.setRid(relevance.getRid());
        session.merge(updateRelevance);
    }

    public void deleteContextFactor(Integer cid) {
        ContextFactor contextFactor = session.get(ContextFactor.class, cid);
        session.delete(contextFactor);
    }

    public void deleteRelevance(Integer rid) {
        Relevance relevance = getRelevance(rid);
        relevance.getContextFactor().getRelevanceSet().remove(relevance);
        session.remove(relevance);
    }

    public List<ContextFactor> listContextFactors() {
        //noinspection unchecked
        List<ContextFactor> contextFactors =
                session.createQuery("FROM ContextFactor").list();
        return contextFactors;
    }

    public List<Relevance> listRelevancesByUserId(Integer uid) {
        Query<Relevance> query;
        User user = session.get(User.class, uid);

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Relevance> criteriaQuery = cb.createQuery(Relevance.class);
        Root<Relevance> root = criteriaQuery.from(Relevance.class);
        Predicate predicate = cb.and(
                cb.equal(root.get("user"), user)
        );
        criteriaQuery.select(root).where(predicate);
        query = session.createQuery(criteriaQuery);
        List<Relevance> result = query.getResultList();
        return result;
    }
}
