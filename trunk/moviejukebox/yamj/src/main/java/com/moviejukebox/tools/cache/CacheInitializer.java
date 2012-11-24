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
package com.moviejukebox.tools.cache;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

/**
 * This class will take an object and try to initialise all the collections within that object
 *
 * @author stuart.boston
 */
public class CacheInitializer {

    private static final Logger logger = Logger.getLogger(CacheInitializer.class);
    private static final String LOG_MESSAGE = "CacheInitializer: ";

    protected CacheInitializer() {
        throw new UnsupportedOperationException("This class cannot be initialised");
    }

    public static <T> void initialize(T dbObject) {
        if (dbObject == null) {
            logger.warn(LOG_MESSAGE + "Object is null");
            return;
        }

        Class clazz = dbObject.getClass();

        if (clazz == com.omertron.thetvdbapi.model.Series.class) {
            initialize((com.omertron.thetvdbapi.model.Series) dbObject);
        } else if (clazz == com.omertron.thetvdbapi.model.Episode.class) {
            initialize((com.omertron.thetvdbapi.model.Episode) dbObject);
        } else if (clazz == com.omertron.thetvdbapi.model.Banners.class) {
            initialize((com.omertron.thetvdbapi.model.Banners) dbObject);
        } else {
            // Not sure if this warning should be here. Some Classes do not need to be initilized
            logger.debug(LOG_MESSAGE + "No initializer found for class of type " + dbObject.getClass().getSimpleName());
        }
    }

    private static void initialize(com.omertron.thetvdbapi.model.Series series) {
        Hibernate.initialize(series.getActors());
        Hibernate.initialize(series.getGenres());
    }

    private static void initialize(com.omertron.thetvdbapi.model.Episode episode) {
        Hibernate.initialize(episode.getDirectors());
        Hibernate.initialize(episode.getGuestStars());
        Hibernate.initialize(episode.getWriters());
    }

    private static void initialize(com.omertron.thetvdbapi.model.Banners banners) {
        Hibernate.initialize(banners.getFanartList());
        Hibernate.initialize(banners.getPosterList());
        Hibernate.initialize(banners.getSeasonList());
        Hibernate.initialize(banners.getSeriesList());
    }
}
