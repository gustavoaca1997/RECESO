package recommender.persistence.manager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import recommender.persistence.entity.Aging;
import recommender.persistence.entity.ClassProperties;
import recommender.persistence.entity.ContextFactor;
import recommender.persistence.entity.Relevance;
import recommender.persistence.entity.User;

import java.util.Arrays;

public final class HibernateUtil {
    private static SessionFactory factory;

    static {
        try {
            Configuration config = new Configuration()
                    .configure()
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(ClassProperties.class)
                    .addAnnotatedClass(ContextFactor.class)
                    .addAnnotatedClass(Relevance.class)
                    .addAnnotatedClass(Aging.class);
            factory = config.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }


    /**
     * Instantiate the session factory with the corresponded class.
     *
     * @param annotatedClasses annotated entity classes
     */
    public static void build(Class... annotatedClasses) {
        try {
            Configuration config = new Configuration()
                    .configure();
            Arrays.stream(annotatedClasses)
                    .forEach(config::addAnnotatedClass);
            factory = config.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return factory;
    }

    public static Session openSession() {
        return factory.openSession();
    }
}
