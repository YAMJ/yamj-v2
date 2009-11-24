/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author altman.matthew
 */
public class DatabasePluginController {

    private static final Logger logger = Logger.getLogger("moviejukebox");

    /**
     * @author Gabriel Corneanu:
     * Store the map in a thread local field to make it thread safe
     */
    private static ThreadLocal<Map<String, MovieDatabasePlugin>> 
      PluginMap = new ThreadLocal<Map<String, MovieDatabasePlugin>>() {
        @Override protected Map<String, MovieDatabasePlugin> initialValue() {
            HashMap<String, MovieDatabasePlugin> m = new HashMap<String, MovieDatabasePlugin>(2);

            m.put(Movie.TYPE_MOVIE, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin")));
            m.put(Movie.TYPE_TVSHOW, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.TheTvDBPlugin")));
            
            return m;
        }
    };    

    
    public static void scan(Movie movie) {
        // if the movie id was set to 0 or -1 then do not continue with database scanning.
        // the user has disabled scanning for this movie
        boolean ignore = false;
        for (String id : movie.getIdMap().values()) {
            if (id.equals("0") || id.equals("-1")) {
                ignore = true;
                break;
            }
        }

        if (!movie.isScrapeLibrary()) {
            ignore = true;
        }

        if (!ignore) {
            // store off the original type because if it wasn't scanned we need to compare to see if we need to rescan
            String origType = movie.getMovieType();
            if (!origType.equals(Movie.TYPE_UNKNOWN)) {
                boolean isScanned = PluginMap.get().get(origType).scan(movie);
                String newType = movie.getMovieType();
                // so if the movie wasn't scanned and it is now a different valid type, then rescan
                if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(origType)) {
                    isScanned = PluginMap.get().get(newType).scan(movie);
                    if (!isScanned) {
                        logger.warning("Movie '" + movie.getTitle() + "' was not able to be scanned using the current plugins");
                    }
                }
            }
        }
    }

    public static void scanNFO(String nfo, Movie movie) {
        PluginMap.get().get(movie.getMovieType()).scanNFO(nfo, movie);
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
            logger.severe("Failed instantiating MovieDatabasePlugin: " + className);
            logger.severe("Default IMDb plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());

        }
        return movieDB;
    }
}
