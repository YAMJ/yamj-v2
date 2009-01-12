package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author altman.matthew
 */
public class DatabasePluginController {

    private static final Logger LOG = Logger.getLogger("moviejukebox");
    private static final Map<String, MovieDatabasePlugin> PLUGIN_MAP = new HashMap<String, MovieDatabasePlugin>(2);


    static {
        PLUGIN_MAP.put(Movie.TYPE_MOVIE, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin")));
        PLUGIN_MAP.put(Movie.TYPE_TVSHOW, getMovieDatabasePlugin(PropertiesUtil.getProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.TheTvDBPlugin")));
    }

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

        if (!ignore) {
            // store off the original type because if it wasn't scanned we need to compare to see if we need to rescan
            String origType = movie.getMovieType();
            if (!origType.equals(Movie.TYPE_UNKNOWN)) {
                boolean isScanned = PLUGIN_MAP.get(origType).scan(movie);
                String newType = movie.getMovieType();
                // so if the movie wasn't scanned and it is now a different valid type, then rescan
                if (!isScanned && !newType.equals(Movie.TYPE_UNKNOWN) && !newType.equals(origType)) {
                    isScanned = PLUGIN_MAP.get(newType).scan(movie);
                    if (!isScanned) {
                        LOG.warning("Movie '" + movie.getTitle() + "' was not able to be scanned using the current plugins");
                    }
                }
            }
        }
    }

    public static void scanNFO(String nfo, Movie movie) {
        PLUGIN_MAP.get(movie.getMovieType()).scanNFO(nfo, movie);
    }

    public static void scanTVShowTitles(Movie movie) {
        PLUGIN_MAP.get(Movie.TYPE_TVSHOW).scanTVShowTitles(movie);
    }

    private static MovieDatabasePlugin getMovieDatabasePlugin(String className) {
        MovieDatabasePlugin movieDB = null;

        try {
            Class<? extends MovieDatabasePlugin> pluginClass = Class.forName(className).asSubclass(MovieDatabasePlugin.class);
            movieDB = pluginClass.newInstance();
        } catch (Exception e) {
            movieDB = new ImdbPlugin();
            LOG.severe("Failed instantiating MovieDatabasePlugin: " + className);
            LOG.severe("Default IMDb plugin will be used instead.");
            e.printStackTrace();
        }
        return movieDB;
    }
}
