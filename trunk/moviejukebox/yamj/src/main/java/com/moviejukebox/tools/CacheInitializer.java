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
        throw new UnsupportedOperationException("This class cannot be initialised");
    }

    public static <T> void initialize(T dbObject) {
        if (dbObject == null) {
            logger.warn("Object of type " + dbObject.getClass().getSimpleName() + " is null");
            return;
        }

        Class clazz = dbObject.getClass();

        if (clazz == com.moviejukebox.thetvdb.model.Series.class) {
            initialize((com.moviejukebox.thetvdb.model.Series) dbObject);
        } else if (clazz == com.moviejukebox.thetvdb.model.Episode.class) {
            initialize((com.moviejukebox.thetvdb.model.Episode) dbObject);
        } else if (clazz == com.moviejukebox.thetvdb.model.Banners.class) {
            initialize((com.moviejukebox.thetvdb.model.Banners) dbObject);
        } else {
            // Not sure if this warning should be here. Some Classes do not need to be initilized
            logger.debug("No initializer found for class of type " + dbObject.getClass().getSimpleName());
        }
    }

    private static void initialize(com.moviejukebox.thetvdb.model.Series series) {
        Hibernate.initialize(series.getActors());
        Hibernate.initialize(series.getGenres());
    }

    private static void initialize(com.moviejukebox.thetvdb.model.Episode episode) {
        Hibernate.initialize(episode.getDirectors());
        Hibernate.initialize(episode.getGuestStars());
        Hibernate.initialize(episode.getWriters());
    }

    private static void initialize(com.moviejukebox.thetvdb.model.Banners banners) {
        Hibernate.initialize(banners.getFanartList());
        Hibernate.initialize(banners.getPosterList());
        Hibernate.initialize(banners.getSeasonList());
        Hibernate.initialize(banners.getSeriesList());
    }
}
