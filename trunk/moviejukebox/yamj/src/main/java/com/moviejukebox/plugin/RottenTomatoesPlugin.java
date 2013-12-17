/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.rottentomatoesapi.RottenTomatoesApi;
import com.omertron.rottentomatoesapi.RottenTomatoesException;
import com.omertron.rottentomatoesapi.model.RTMovie;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * API for getting the ratings from RottenTomatoes.com
 *
 * @author stuart.boston
 *
 */
public class RottenTomatoesPlugin {

    private static final Logger LOG = Logger.getLogger(RottenTomatoesPlugin.class);
    private static final String LOG_MESSAGE = "RottenTomatoesPlugin: ";
    public static final String ROTTENTOMATOES_PLUGIN_ID = "rottentomatoes";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_RottenTomatoes");
    private static final String WEBHOST = "rottentomatoes.com";
    private static final String[] PRIORITY_LIST = PropertiesUtil.getProperty("mjb.rottentomatoes.priority", "critics_score,audience_score,critics_rating,audience_rating").split(",");
    private RottenTomatoesApi rt = null;

    public RottenTomatoesPlugin() {
        try {
            rt = new RottenTomatoesApi(API_KEY);
        } catch (RottenTomatoesException ex) {
            LOG.error(LOG_MESSAGE + "Failed to get RottenTomatoes API: " + ex.getMessage());
            return;
        }

        // We need to set the proxy parameters if set.
        rt.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        rt.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    public boolean scan(Movie movie) {
        if (rt == null) {
            LOG.trace(LOG_MESSAGE + "Unable to initialise RT Plugin, no information retrieved.");
            return Boolean.FALSE;
        }

        if (!movie.isScrapeLibrary()) {
            return Boolean.FALSE;
        }

        if (movie.isTVShow()) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is a TV Show, skipping.");
            return Boolean.FALSE;
        }

        // If we have a rating already, skip unless we are rechecking.
        if (movie.getRating(ROTTENTOMATOES_PLUGIN_ID) >= 0 && !movie.isDirty(DirtyFlag.RECHECK)) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " already has a rating");
            return Boolean.TRUE;
        }

        // We seem to have a valid movie, so let's scan
        ThreadExecutor.enterIO(WEBHOST);
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
        RTMovie rtMovie = null;
        int rtId = NumberUtils.toInt(movie.getId(ROTTENTOMATOES_PLUGIN_ID), 0);

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
                LOG.warn(LOG_MESSAGE + "Failed to get RottenTomatoes information: " + ex.getMessage());
            }
        } else {
            try {
                rtMovie = rt.getDetailedInfo(rtId);
            } catch (RottenTomatoesException ex) {
                LOG.warn(LOG_MESSAGE + "Failed to get RottenTomatoes information: " + ex.getMessage());
            }
        }

        int ratingFound = 0;

        if (rtMovie != null) {
            Map<String, String> ratings = rtMovie.getRatings();

            for (String type : PRIORITY_LIST) {
                if (ratings.containsKey(type)) {
                    if (StringUtils.isNumeric(ratings.get(type))) {
                        ratingFound = Integer.parseInt(ratings.get(type));
                    }

                    if (ratingFound > 0) {
                        LOG.debug(LOG_MESSAGE + movie.getBaseName() + " - " + type + " found: " + ratingFound);
                        movie.addRating(ROTTENTOMATOES_PLUGIN_ID, ratingFound);
                        break;
                    }
                }
            }

            return Boolean.TRUE;
        } else {
            LOG.debug(LOG_MESSAGE + "No RottenTomatoes information found for " + movie.getBaseName());
            return Boolean.FALSE;
        }
    }
}
