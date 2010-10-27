/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

/**
 * Scanner for posters.
 * Includes local searches (scan) and Internet Searches
 */
package com.moviejukebox.scanner.artwork;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static java.lang.Boolean.parseBoolean;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.poster.IMoviePosterPlugin;
import com.moviejukebox.plugin.poster.IPosterPlugin;
import com.moviejukebox.plugin.poster.ITvShowPosterPlugin;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Scanner for poster files in local directory and from the Internet
 * 
 * @author groll.troll
 * @author Stuart.Boston
 * 
 * @version 1.0, 7 October 2008
 * @version 2.0 6 July 2009
 */
public class PosterScanner extends ArtworkScanner implements IArtworkScanner {

    private static final Map<String, IPosterPlugin> posterPlugins = new HashMap<String, IPosterPlugin>();
    private static final Map<String, IMoviePosterPlugin>  moviePosterPlugins  = new HashMap<String, IMoviePosterPlugin>();
    private static final Map<String, ITvShowPosterPlugin> tvShowPosterPlugins = new HashMap<String, ITvShowPosterPlugin>();

    protected static String preferredPosterSearchEngine;
    protected static String posterSearchPriority;
    private   static String tvShowPosterSearchPriority;
    private   static String moviePosterSearchPriority;
    // Create a static logger
    private   static Logger sLogger = Logger.getLogger("moviejukebox");

    static {
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");
        tvShowPosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "thetvdb,cdon,filmaffinity");
        moviePosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie",
                        "moviedb,impawards,imdb,moviecovers,google,yahoo,motechnet");

        ServiceLoader<IMoviePosterPlugin> moviePosterPluginsSet = ServiceLoader.load(IMoviePosterPlugin.class);
        for (IMoviePosterPlugin iPosterPlugin : moviePosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

        ServiceLoader<ITvShowPosterPlugin> tvShowPosterPluginsSet = ServiceLoader.load(ITvShowPosterPlugin.class);
        for (ITvShowPosterPlugin iPosterPlugin : tvShowPosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

    }
    
    public PosterScanner() {
        super(ArtworkScanner.POSTER);

        try {
            artworkOverwrite = parseBoolean(getProperty("mjb.forcePostersOverwrite", "false"));
        } catch (Exception ignore) {
            artworkOverwrite = false;
        }
    }

    /**
     * Search for posters stored locally
     * @param jukebox
     * @param movie
     * @return
     */
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        super.scan(jukebox, movie, artworkImagePlugin);
        String filename = copyLocalFile(jukebox, movie);
        return filename;
    }
    
    /**
     * Search for posters online using the plugins
     * @param movie
     */
    public String scanOnlineArtwork(Movie movie) {
        logger.finer(logMessage + "Searching for " + movie.getBaseName());
        IImage posterImage = getPosterURL(movie);
        if (!posterImage.getUrl().equals(Movie.UNKNOWN)) {
            movie.setPosterURL(posterImage.getUrl());
            movie.setPosterSubimage(posterImage.getSubimage());
        }
        return posterImage.getUrl();
    }
    
    /**
     * Locate the PosterURL from the Internet. This is the main method and should 
     * be called instead of the individual getPosterFrom* methods.
     * 
     * @param movie
     *            The movieBean to search for
     * @return The posterImage with poster url that was found (Maybe Image.UNKNOWN)
     */
    private IImage getPosterURL(Movie movie) {
        String posterSearchToken;
        IImage posterImage = Image.UNKNOWN;
        StringTokenizer st;

        if (movie.isTVShow()) {
            st = new StringTokenizer(tvShowPosterSearchPriority, ",");
        } else {
            st = new StringTokenizer(moviePosterSearchPriority, ",");
        }

        while (st.hasMoreTokens() && posterImage.getUrl().equalsIgnoreCase(Movie.UNKNOWN)) {
            posterSearchToken = st.nextToken();

            IPosterPlugin iPosterPlugin = posterPlugins.get(posterSearchToken);
            // Check that plugin is registered as a movie or TV plugin
            if (iPosterPlugin == null) {
                logger.severe(logMessage + "'" + posterSearchToken + "' plugin doesn't exist, please check you moviejukebox properties. Valid plugins are : "
                                + getPluginsCode());
            }
            
            String msg = null;
            
            if (movie.isTVShow()) {
                iPosterPlugin = tvShowPosterPlugins.get(posterSearchToken);
                msg = "TvShow";
            } else {
                iPosterPlugin = moviePosterPlugins.get(posterSearchToken);
                msg = "Movie";
            }
            
            if (iPosterPlugin == null) {
                logger.info(logMessage + posterSearchToken + " is not a " + msg + " Poster plugin - skipping");
            } else {
                logger.finest(logMessage + "Using " + posterSearchToken + " to search for a " + msg + " poster for " + movie.getTitle());
                posterImage = iPosterPlugin.getPosterUrl(movie, movie);
            }

            // Validate the poster- No need to validate if we're UNKNOWN
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl()) && artworkValidate && !validateArtwork(posterImage, artworkWidth, artworkHeight, artworkValidateAspect)) {
                posterImage = Image.UNKNOWN;
            } else {
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
                    logger.finest(logMessage + "Poster URL found at " + posterSearchToken + ": " + posterImage.getUrl());
                }
            }
        }

        return posterImage;
    }

    /**
     * Register the poster plugin
     * @param key           The "name" or key of the plugin, e.g. IMDb
     * @param posterPlugin  The plugin to register 
     */
    public static void register(String key, IPosterPlugin posterPlugin) {
        posterPlugins.put(key, posterPlugin);
    }

    /**
     * Get the list of registered plugins
     * @return
     */
    private String getPluginsCode() {
        String response = "";

        Set<String> keySet = posterPlugins.keySet();
        for (String string : keySet) {
            response += string + " / ";
        }
        return response;
    }

    /**
     * Register a movie poster plugin
     * @param key           The "name" or key of the plugin, e.g. IMDb
     * @param posterPlugin  The plugin to register 
     */
    private static void register(String key, IMoviePosterPlugin posterPlugin) {
        sLogger.finest("Artwork Scanner (poster): " + posterPlugin.getClass().getName() + " registered as Movie Poster Plugin with key " + key);
        moviePosterPlugins.put(key, posterPlugin);
        register(key, (IPosterPlugin)posterPlugin);
    }

    /**
     * Register a TV poster plugin
     * @param key           The "name" or key of the plugin, e.g. IMDb
     * @param posterPlugin  The plugin to register 
     */
    public static void register(String key, ITvShowPosterPlugin posterPlugin) {
        sLogger.finest("Artwork Scanner (poster): " + posterPlugin.getClass().getName() + " registered as TvShow Poster Plugin with key " + key);
        tvShowPosterPlugins.put(key, posterPlugin);
        register(key, (IPosterPlugin)posterPlugin);
    }

    @Override
    public String getArtworkFilename(Movie movie) {
        return movie.getPosterFilename();
    }

    @Override
    public String getArtworkUrl(Movie movie) {
        return movie.getPosterURL();
    }

    @Override
    public void setArtworkFilename(Movie movie, String artworkFilename) {
        movie.setPosterFilename(artworkFilename);
    }
    
    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        movie.setPosterURL(artworkUrl);
    }

    @Override
    public void setArtworkImagePlugin() {
        setImagePlugin(PropertiesUtil.getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
    }

    @Override
    public boolean isOverwrite() {
        return artworkOverwrite;
    }

    @Override
    public boolean isDirtyArtwork(Movie movie) {
        return movie.isDirtyPoster();
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        movie.setDirtyPoster(dirty);
    }

}
