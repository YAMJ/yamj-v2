/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.util.Map;

import org.apache.log4j.Logger;

import com.moviejukebox.fanarttv.tools.WebBrowser;
import com.moviejukebox.model.Movie;
import com.moviejukebox.rottentomatoes.RottenTomatoes;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;

/**
 * API for getting the ratings from RottenTomatoes.com
 * @author stuart.boston
 *
 */
public class RottenTomatoesPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    public static final String ROTTENTOMATOES_PLUGIN_ID = "rottentomatoes";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_RottenTomatoes");
    private static final String webhost = "rottentomatoes.com";
    private static String logMessage = "RottenTomatoesPlugin: ";

    private static String[] priorityList = PropertiesUtil.getProperty("mjb.rottentomatoes.priority", "critics_score,audience_score,critics_rating,audience_rating").split(",");
    
    private RottenTomatoes rt;
    
    public RottenTomatoesPlugin() {
        rt = new RottenTomatoes(API_KEY);

        // We need to set the proxy parameters if set.
        rt.setProxy(WebBrowser.getProxyHost(), WebBrowser.getProxyPort(), WebBrowser.getTvdbProxyUsername(), WebBrowser.getProxyPassword());
        
        // Set the timeout values
        rt.setTimeout(WebBrowser.getWebTimeoutConnect(), WebBrowser.getWebTimeoutRead());
    }
    
    public boolean scan(Movie movie) {
        if (!movie.isScrapeLibrary()) {
            return false;
        }
        
        if (movie.isTVShow()) {
            logger.debug(logMessage + movie.getBaseName() + " is a TV Show, skipping.");
            return false;
        }

        // We seem to have a valid movie, so let's scan
        ThreadExecutor.enterIO(webhost);
        try {
            return doScan(movie);
        } finally {
            ThreadExecutor.leaveIO();
        }
    }
    
    public boolean doScan(Movie movie) {
        int rtId = 0;
        com.moviejukebox.rottentomatoes.model.Movie rtMovie = null;
        
        if (StringTools.isValidString(movie.getId(ROTTENTOMATOES_PLUGIN_ID))) {
            try {
                rtId = Integer.parseInt(movie.getId(ROTTENTOMATOES_PLUGIN_ID));
            } catch (Exception error) {
                rtId = 0;
            }
        }
            
        if (rtId == 0) {
            for (com.moviejukebox.rottentomatoes.model.Movie tmpMovie : rt.moviesSearch(movie.getTitle())) {
                if (movie.getTitle().equalsIgnoreCase(tmpMovie.getTitle()) && (movie.getYear().equals(""+tmpMovie.getYear()))) {
                    rtId = tmpMovie.getId();
                    rtMovie = tmpMovie;
                    movie.setId(ROTTENTOMATOES_PLUGIN_ID, rtId);
                    break;
                }
            }
        } else {
           rtMovie = rt.movieInfo(rtId);
        }
        
        int ratingFound = 0;
        
        if (rtMovie != null) {
            Map<String, Integer> ratings = rtMovie.getRatings();
            
            for (String type : priorityList) {
                if (ratings.containsKey(type)) {
                    ratingFound = ratings.get(type);
                    
                    if (ratingFound > 0) {
                        logger.debug(logMessage + movie.getBaseName() + " - " + type + " found: " + ratingFound);
                        movie.addRating(ROTTENTOMATOES_PLUGIN_ID, ratingFound);
                        break;
                    }
                }
            }
            
            return true;
        } else {
            logger.debug(logMessage + "No RottenTomatoes information found for " + movie.getBaseName());
            return false;
        }
    }
}
