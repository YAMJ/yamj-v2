/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author altman.matthew
 */
public final class DatabasePluginController {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasePluginController.class);
    public static final String TYPE_ALTERNATE = "ALTERNATE";
    private static boolean autoDetect = false;
    private static List<String> autoDetectList = new ArrayList<String>();

    private DatabasePluginController() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }
    /**
     * @author Gabriel Corneanu: Store the map in a thread local field to make it thread safe
     */
    private static final ThreadLocal<Map<String, MovieDatabasePlugin>> PLUGIN_MAP = new ThreadLocal<Map<String, MovieDatabasePlugin>>() {
        @Override
        protected Map<String, MovieDatabasePlugin> initialValue() {
            Map<String, MovieDatabasePlugin> movieDatabasePlugin = new HashMap<String, MovieDatabasePlugin>(2);

            movieDatabasePlugin.put(Movie.TYPE_MOVIE, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin").trim()));
            movieDatabasePlugin.put(Movie.TYPE_TVSHOW, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.TheTvDBPlugin").trim()));
            movieDatabasePlugin.put(Movie.TYPE_PERSON, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.person.plugin", "com.moviejukebox.plugin.ImdbPlugin").trim()));
            String alternatePlugin = PropertiesUtil.getProperty("mjb.internet.alternate.plugin", "").trim();
            if (StringUtils.isNotBlank(alternatePlugin)) {
                movieDatabasePlugin.put(TYPE_ALTERNATE, getMovieDatabasePlugin(alternatePlugin));
            }

            String tmpAutoDetect = PropertiesUtil.getProperty("mjb.internet.plugin.autodetect", FALSE).toLowerCase();
            autoDetect = !tmpAutoDetect.equalsIgnoreCase(FALSE);
            if (autoDetect) {
                String pluginID;
                boolean emptyList = tmpAutoDetect.equalsIgnoreCase(TRUE);
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
            LOG.debug("Skipping internet search for " + movie.getBaseFilename());
        } else {
            // store off the original type because if it wasn't scanned we need to compare to see if we need to rescan
            String origType = movie.getMovieType();
            if (!origType.equals(Movie.TYPE_UNKNOWN)) {
                boolean isScanned = false;
                if (movie.getMovieScanner() != null) {
                    isScanned = movie.getMovieScanner().scan(movie);
                }
                if (!isScanned) {
                    isScanned = PLUGIN_MAP.get().get(origType).scan(movie);
                    String newType = movie.getMovieType();
                    // so if the movie wasn't scanned and it is now a different valid type, then rescan
                    if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(Movie.REMOVE) && !newType.equals(origType)) {
                        isScanned = PLUGIN_MAP.get().get(newType).scan(movie);
                    }
                    if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(Movie.REMOVE)) {
                        MovieDatabasePlugin alternatePlugin = PLUGIN_MAP.get().get(TYPE_ALTERNATE);
                        if (alternatePlugin != null) {
                            isScanned = alternatePlugin.scan(movie);
                        }
                    }
                    if (!isScanned) {
                        LOG.warn("Video '" + movie.getBaseName() + "' was not able to be scanned using the current plugins");
                    }
                }
            }
        }
    }

    public static void scan(Person person) {
        if (!person.isScrapeLibrary()) {
            LOG.debug("Skipping internet search for " + person.getName());
            return;
        }
        if (!PLUGIN_MAP.get().get(Movie.TYPE_PERSON).scan(person)) {
            LOG.warn("Person '" + person.getName() + "' was not able to be scanned using the current plugins");
        }
    }

    public static boolean scanNFO(String nfo, Movie movie) {
        boolean scannedOk = Boolean.FALSE;
        if (!PLUGIN_MAP.get().get(movie.getMovieType()).scanNFO(nfo, movie) && autoDetect) {
            for (String pluginID : autoDetectList) {
                MovieDatabasePlugin movieDBPlugin = PLUGIN_MAP.get().get(pluginID);
                scannedOk = movieDBPlugin.scanNFO(nfo, movie);
                if (scannedOk) {
                    movie.setMovieScanner(movieDBPlugin);
                    break;
                }
            }
        }
        return scannedOk;
    }

    public static void scanTVShowTitles(Movie movie) {
        PLUGIN_MAP.get().get(Movie.TYPE_TVSHOW).scanTVShowTitles(movie);
    }

    private static MovieDatabasePlugin getMovieDatabasePlugin(String className) {
        try {
            Class<? extends MovieDatabasePlugin> pluginClass = Class.forName(className).asSubclass(MovieDatabasePlugin.class);
            return pluginClass.newInstance();
        } catch (Exception error) {
            LOG.error("Failed instantiating MovieDatabasePlugin: " + className);
            LOG.error("Default IMDb plugin will be used instead.");
            LOG.error(SystemTools.getStackTrace(error));
            return new ImdbPlugin();
        }
    }

    public static String getMovieDatabasePluginName(String movieType) {
        String pluginName = null;
        try {
            pluginName = PLUGIN_MAP.get().get(movieType).getPluginID();
        } catch (Exception ignore) {
            // ignore this error
        }

        if (StringTools.isNotValidString(pluginName)) {
            pluginName = Movie.UNKNOWN;
        }
        return pluginName;
    }
}
