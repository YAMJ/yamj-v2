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

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.rottentomatoes.RottenTomatoes;
import com.moviejukebox.rottentomatoes.RottenTomatoesException;
import com.moviejukebox.rottentomatoes.model.RTMovie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * API for getting the ratings from RottenTomatoes.com
 *
 * @author stuart.boston
 *
 */
public class RottenTomatoesPlugin {

    private static final Logger logger = Logger.getLogger(RottenTomatoesPlugin.class);
    public static final String ROTTENTOMATOES_PLUGIN_ID = "rottentomatoes";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_RottenTomatoes");
    private static final String webhost = "rottentomatoes.com";
    private static String logMessage = "RottenTomatoesPlugin: ";
    private static String[] priorityList = PropertiesUtil.getProperty("mjb.rottentomatoes.priority", "critics_score,audience_score,critics_rating,audience_rating").split(",");
    private RottenTomatoes rt;
    private static boolean versionInfoShown = Boolean.FALSE;

    public RottenTomatoesPlugin() {
        try {
            rt = new RottenTomatoes(API_KEY);
            if (!versionInfoShown) {
                RottenTomatoes.showVersion();
                versionInfoShown = Boolean.TRUE;
            }
        } catch (RottenTomatoesException ex) {
            logger.error(logMessage + "Failed to get RottenTomatoes API: " + ex.getMessage());
        }

        // We need to set the proxy parameters if set.
        rt.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        rt.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    public boolean scan(Movie movie) {
        if (!movie.isScrapeLibrary()) {
            return false;
        }

        if (movie.isTVShow()) {
            logger.debug(logMessage + movie.getBaseName() + " is a TV Show, skipping.");
            return false;
        }

        // If we have a rating already, skip unless we are rechecking.
        if (movie.getRating(ROTTENTOMATOES_PLUGIN_ID) >= 0 && !movie.isDirty(DirtyFlag.RECHECK)) {
            logger.debug(logMessage + movie.getBaseName() + " already has a rating");
            return true;
        }

        // We seem to have a valid movie, so let's scan
        ThreadExecutor.enterIO(webhost);
        try {
            return doScan(movie);
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    /**
     * Perform the scan through the RottenTomatoes API.
     *
     * @param movie
     * @return
     */
    private boolean doScan(Movie movie) {
        int rtId = 0;
        RTMovie rtMovie = null;

        if (StringTools.isValidString(movie.getId(ROTTENTOMATOES_PLUGIN_ID))) {
            try {
                rtId = Integer.parseInt(movie.getId(ROTTENTOMATOES_PLUGIN_ID));
            } catch (Exception error) {
                rtId = 0;
            }
        }

        if (rtId == 0) {
            List<RTMovie> rtMovies;
            try {
                rtMovies = rt.getMoviesSearch(movie.getTitle());
                for (RTMovie tmpMovie : rtMovies) {
                    if (movie.getTitle().equalsIgnoreCase(tmpMovie.getTitle()) && (movie.getYear().equals("" + tmpMovie.getYear()))) {
                        rtId = tmpMovie.getId();
                        rtMovie = tmpMovie;
                        movie.setId(ROTTENTOMATOES_PLUGIN_ID, rtId);
                        break;
                    }
                }
            } catch (RottenTomatoesException ex) {
                logger.warn(logMessage + "Failed to get RottenTomatoes information: " + ex.getMessage());
            }
        } else {
            try {
                rtMovie = rt.getDetailedInfo(rtId);
            } catch (RottenTomatoesException ex) {
                logger.warn(logMessage + "Failed to get RottenTomatoes information: " + ex.getMessage());
            }
        }

        int ratingFound = 0;

        if (rtMovie != null) {
            Map<String, String> ratings = rtMovie.getRatings();

            for (String type : priorityList) {
                if (ratings.containsKey(type)) {
                    if (StringUtils.isNumeric(ratings.get(type))) {
                        ratingFound = Integer.parseInt(ratings.get(type));
                    }

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
