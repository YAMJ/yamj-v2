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
package com.moviejukebox.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SystemTools;

/**
 *
 * @author altman.matthew
 */
public class DatabasePluginController {

    private static final Logger logger = Logger.getLogger("moviejukebox");
    private static boolean autoDetect = false;
    private static ArrayList<String> autoDetectList = new ArrayList<String>();
    /**
     * @author Gabriel Corneanu:
     * Store the map in a thread local field to make it thread safe
     */
    private static ThreadLocal<Map<String, MovieDatabasePlugin>> PluginMap = new ThreadLocal<Map<String, MovieDatabasePlugin>>() {

        @Override
        protected Map<String, MovieDatabasePlugin> initialValue() {
            HashMap<String, MovieDatabasePlugin> movieDatabasePlugin = new HashMap<String, MovieDatabasePlugin>(2);

            movieDatabasePlugin.put(Movie.TYPE_MOVIE, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin").trim()));
            movieDatabasePlugin.put(Movie.TYPE_TVSHOW, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.TheTvDBPlugin").trim()));
            movieDatabasePlugin.put(Movie.TYPE_PERSON, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.person.plugin", "com.moviejukebox.plugin.ImdbPlugin").trim()));
            String alternatePlugin = PropertiesUtil.getProperty("mjb.internet.alternate.plugin", "").trim();
            if (!alternatePlugin.equals("")) {
                movieDatabasePlugin.put("ALTERNATE", getMovieDatabasePlugin(alternatePlugin));
            }

            String tmpAutoDetect = PropertiesUtil.getProperty("mjb.internet.plugin.autodetect", "false").toLowerCase();
            autoDetect = !tmpAutoDetect.equalsIgnoreCase("false");
            if (autoDetect) {
                String pluginID;
                boolean emptyList = tmpAutoDetect.equalsIgnoreCase("true");
                ServiceLoader<MovieDatabasePlugin> movieDBPluginsSet = ServiceLoader.load(MovieDatabasePlugin.class);
                for (MovieDatabasePlugin movieDBPlugin : movieDBPluginsSet) {
                    pluginID = movieDBPlugin.getPluginID().toLowerCase();
                    if (emptyList || (autoDetectList.indexOf(pluginID) > -1)) {
                        movieDatabasePlugin.put(pluginID, movieDBPlugin);
                        autoDetectList.add(pluginID);
                    }
                }
            }

            return movieDatabasePlugin;
        }
    };

    public static void scan(Movie movie) {
        boolean ignore = false;

        if (!movie.isScrapeLibrary()) {
            ignore = true;
        }

        // if the movie id was set to 0 or -1 then do not continue with database scanning.
        // the user has disabled scanning for this movie
        for (String id : movie.getIdMap().values()) {
            if (id.equals("0") || id.equals("-1")) {
                ignore = true;
                break;
            }
        }

        if (ignore) {
            logger.debug("Skipping internet search for " + movie.getBaseFilename());
            return;
        } else {
            // store off the original type because if it wasn't scanned we need to compare to see if we need to rescan
            String origType = movie.getMovieType();
            if (!origType.equals(Movie.TYPE_UNKNOWN)) {
                boolean isScanned = false;
                if (movie.getMovieScanner() != null) {
                    isScanned = movie.getMovieScanner().scan(movie);
                }
                if (!isScanned) {
                    isScanned = PluginMap.get().get(origType).scan(movie);
                    String newType = movie.getMovieType();
                    // so if the movie wasn't scanned and it is now a different valid type, then rescan
                    if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(Movie.REMOVE) && !newType.equals(origType)) {
                        isScanned = PluginMap.get().get(newType).scan(movie);
                    }
                    if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(Movie.REMOVE)) {
                        MovieDatabasePlugin alternatePlugin = PluginMap.get().get("ALTERNATE");
                        if (alternatePlugin != null) {
                            isScanned = alternatePlugin.scan(movie);
                        }
                    }
                    if (!isScanned) {
                        logger.warn("Video '" + movie.getBaseName() + "' was not able to be scanned using the current plugins");
                    }
                }
            }
        }
    }

    public static void scan(Person person) {
        if (!person.isScrapeLibrary()) {
            logger.debug("Skipping internet search for " + person.getName());
            return;
        }
        if (!PluginMap.get().get(Movie.TYPE_PERSON).scan(person)) {
            logger.warn("Person '" + person.getName() + "' was not able to be scanned using the current plugins");
        }
    }

    public static void scanNFO(String nfo, Movie movie) {
        if (!PluginMap.get().get(movie.getMovieType()).scanNFO(nfo, movie) && autoDetect) {
            for (String pluginID : autoDetectList) {
                MovieDatabasePlugin movieDBPlugin = PluginMap.get().get(pluginID);
                if (movieDBPlugin.scanNFO(nfo, movie)) {
                    movie.setMovieScanner(movieDBPlugin);
                    break;
                }
            }
        }
    }

    public static void scanTVShowTitles(Movie movie) {
        PluginMap.get().get(Movie.TYPE_TVSHOW).scanTVShowTitles(movie);
    }

    private static MovieDatabasePlugin getMovieDatabasePlugin(String className) {
        MovieDatabasePlugin movieDB = null;

        try {
            Class<? extends MovieDatabasePlugin> pluginClass = Class.forName(className).asSubclass(MovieDatabasePlugin.class);
            movieDB = pluginClass.newInstance();
        } catch (Exception error) {
            movieDB = new ImdbPlugin();
            logger.error("Failed instantiating MovieDatabasePlugin: " + className);
            logger.error("Default IMDb plugin will be used instead.");
            logger.error(SystemTools.getStackTrace(error));
        }
        return movieDB;
    }
}
