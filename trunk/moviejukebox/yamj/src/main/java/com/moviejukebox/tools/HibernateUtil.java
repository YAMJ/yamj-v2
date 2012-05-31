/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.tools;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public class HibernateUtil {

    private static final Logger logger = Logger.getLogger(HibernateUtil.class);
    private static final String logMessage = "HibernateUtil: ";
    private static SessionFactory sessionFactory = buildSessionFactory();
    private static ServiceRegistry serviceRegistry;

    /**
     * Create a session factory from the hibernate.cfg.xml file See here:
     * http://stackoverflow.com/questions/1921865/how-to-connect-to-mutiple-databases-in-hibernate When we need more
     * databases
     *
     * @return
     */
    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("./properties/hibernate-cache.cfg.xml");
            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            return sessionFactory;
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println(logMessage + "Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Get the single session factory
     *
     * @return
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Close the session factory
     */
    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    }

    /**
     * Save a single object to the database
     *
     * TODO: Add a session to the parameters so you can save to each database
     *
     * @param objectToSave
     * @param objectKey
     * @return true if the object was saved, false otherwise
     */
    public static boolean saveObject(Object objectToSave, Serializable objectKey) {
        if (objectToSave == null) {
            logger.info(logMessage + "Object is null, not saving");
            return false;
        }

        Session session = sessionFactory.getCurrentSession();
        try {
            session.beginTransaction();

            if (session.get(objectToSave.getClass(), objectKey) == null) {
                logger.info(logMessage + "Saving " + objectToSave.getClass().getSimpleName() + ": '" + objectKey + "'");
                session.save(objectToSave);
                return true;
            } else {
                logger.info(logMessage + objectToSave.getClass().getSimpleName() + " '" + objectKey + "' already exists.");
                return false;
            }
        } finally {
            session.getTransaction().commit();
        }
    }

    /**
     * Load a single object from the database
     *
     * TODO: Add a session to the parameters so you can load from each database
     *
     * @param clazz
     * @param key
     * @return object will be null if it wasn't found
     */
    public static <T> T loadObject(Class<T> clazz, Serializable key) {
        Session session = sessionFactory.getCurrentSession();
        String className = clazz.getSimpleName();
        T dbObject;

        try {
            session.beginTransaction();
            dbObject = clazz.cast(session.get(clazz, key));
            if (dbObject != null) {
                CacheInitializer.initialize(dbObject);
            }
        } finally {
            session.getTransaction().commit();
        }

        if (dbObject == null) {
            logger.info(logMessage + "No " + className + " with id '" + key + "' found");
        } else {
            logger.info(logMessage + "Found " + className + " with id '" + key + "'");
        }

        return dbObject;
    }
}