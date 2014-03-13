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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.trailer.ITrailerPlugin;
import com.moviejukebox.plugin.trailer.TrailerPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stuart
 */
public class TrailerScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerScanner.class);
    private static final String LOG_MESSAGE = "TrailerScanner: ";
    private static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
    // Convert trailers.rescan.days from DAYS to MILLISECONDS for comparison purposes
    private static final long RESCAN_DAYS_MILLIS = PropertiesUtil.getLongProperty("trailers.rescan.days", 15) * MILLIS_IN_DAY;
    private static final boolean SCANNER_ENABLE = PropertiesUtil.getBooleanProperty("trailers.scanner.enable", Boolean.TRUE);
    private static final String TRAILERS_SCANNER = PropertiesUtil.getProperty("trailers.scanner", "apple");
    private String trailerPluginList = Movie.UNKNOWN;
    private static final Map<String, ITrailerPlugin> trailerPlugins = Collections.synchronizedMap(new HashMap<String, ITrailerPlugin>());
    private static final TrailerPlugin TRAILERS_PLUGIN = new TrailerPlugin();

    public TrailerScanner() {
        if (SCANNER_ENABLE) {
            synchronized (trailerPlugins) {
                ServiceLoader<ITrailerPlugin> trailerPluginsSet = ServiceLoader.load(ITrailerPlugin.class);

                for (ITrailerPlugin trailerPlugin : trailerPluginsSet) {
                    trailerPlugins.put(trailerPlugin.getName().toLowerCase().trim(), trailerPlugin);
                }

                trailerPluginList = getTrailerPluginList();
            }
        }
    }

    /**
     * This function will check movie trailers and return true if trailers needs to be re-scanned.
     *
     * @param movie
     * @return
     */
    public boolean isTrailersNeedRescan(Movie movie) {
        // Can the movie have trailers?
        if (!movie.canHaveTrailers()) {
            return Boolean.FALSE;
        }

        if (!TRAILERS_PLUGIN.isScanForTrailer(movie)) {
            LOG.debug(LOG_MESSAGE + "Scanning skipped because " + movie.getBaseName() + " is not HD");
            return Boolean.FALSE;
        }

        // Does the trailer need to be overwritten?
        if (TRAILERS_PLUGIN.isOverwrite()) {
            return Boolean.TRUE;
        }

        // Check if this movie was already checked for trailers
        if (movie.isTrailerExchange()) {
            LOG.debug(LOG_MESSAGE + "Movie " + movie.getTitle() + " has previously been checked for trailers, skipping.");
            return Boolean.FALSE;
        }

        // Check if we need to scan or rescan for trailers
        long now = new Date().getTime();
        if ((now - movie.getTrailerLastScan()) < RESCAN_DAYS_MILLIS) {
            return Boolean.FALSE;
        }

        return true;
    }

    public boolean getTrailers(Movie movie) {
        // Check if we need to scan at all
        if (!isTrailersNeedRescan(movie)) {
            return Boolean.FALSE;
        }

        boolean result = Boolean.FALSE;
        String trailersSearchToken;

        StringTokenizer st = new StringTokenizer(TRAILERS_SCANNER, ",");
        while (st.hasMoreTokens() && !result) {
            trailersSearchToken = st.nextToken();
            ITrailerPlugin trailerPlugin = trailerPlugins.get(trailersSearchToken);
            if (trailerPlugin == null) {
                LOG.error(LOG_MESSAGE + "'" + trailersSearchToken + "' plugin doesn't exist, please check your moviejukebox properties. Valid plugins are : " + trailerPluginList);
            } else {
                result |= trailerPlugin.generate(movie);
            }
        }

        // Update trailerExchange
        if (result == Boolean.FALSE) {
            // Set trailerExchange to true if trailersRescanDaysMillis is < 0 (disable)
            result = RESCAN_DAYS_MILLIS < 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        movie.setTrailerExchange(result);

        return result;
    }

    /**
     * Get a list of the trailer plugins
     *
     * @return
     */
    private static String getTrailerPluginList() {
        StringBuilder response = new StringBuilder();

        Set<String> keySet = trailerPlugins.keySet();
        for (String string : keySet) {
            response.append(string);
            response.append(Movie.SPACE_SLASH_SPACE);
        }

        response.delete(response.length() - 3, response.length());
        return response.toString();
    }
}
