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

import com.moviejukebox.thetvdb.model.Series;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

/**
 * This class will take an object and try to initialise all the collections
 * within that object
 *
 * @author stuart.boston
 */
public class CacheInitializer {
    private final static Logger logger = Logger.getLogger(CacheInitializer.class);

    protected CacheInitializer() {
        throw new UnsupportedOperationException("Class cannot be initialised");
    }

    public static <T> void initialize(T dbObject) {
        if (dbObject == null) {
            logger.warn("Object of type " + dbObject.getClass().getSimpleName() + " is null");
            return;
        }

        if (dbObject.getClass() == Series.class) {
            initialize((Series) dbObject);
        } else {
            logger.warn("No initializer found for class of type " + dbObject.getClass().getSimpleName());
        }
    }

    private static void initialize(Series series) {
        logger.info("Initialising Series " + series.getSeriesName());
        Hibernate.initialize(series.getActors());
        Hibernate.initialize(series.getGenres());
    }
}
