/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.tools;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.trakttv.TraktTvApi;
import org.yamj.api.trakttv.model.TrackedMovie;
import org.yamj.api.trakttv.model.TrackedShow;
import org.yamj.api.trakttv.model.enumeration.Extended;

public class TraktTvScanner {

    public static final String SCANNER_ID = "trakttv";
    private static final Logger LOG = LoggerFactory.getLogger(TraktTvScanner.class);
    private static TraktTvScanner INSTANCE;
    
    private TraktTvApi traktTvApi;
    private List<TrackedMovie> watchedMovies;
    private List<TrackedShow> watchedShows;
   
    public static TraktTvScanner getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TraktTvScanner();
        }
        return INSTANCE;
    }
    
    private TraktTvScanner() {
        final String clientId = PropertiesUtil.getProperty("API_KEY_TraktTV_ClientId");
        final String secret = PropertiesUtil.getProperty("API_KEY_TraktTV_Secret");

        try {
            LOG.trace("Initialize TraktTvApi");
            traktTvApi = new TraktTvApi(clientId, secret, YamjHttpClientBuilder.getHttpClient());
            
            // TODO set correct access token
            traktTvApi.setAccessToken("");
        }  catch (Exception ex) {
            LOG.error("Failed to initialize TraktTvApi", ex);
            traktTvApi = null;
        }
    }

    public List<TrackedMovie> getWatchedMovies() {
        if (watchedMovies == null) {
            if (traktTvApi == null) {
                this.watchedMovies = Collections.emptyList();
            } else {
                try {
                    // get watched movies from Trakt.TV
                    this.watchedMovies = traktTvApi.syncService().getWatchedMovies(Extended.MINIMAL);
                } catch (Exception ex) {
                    LOG.error("Failed to get watched movies", ex);
                    this.watchedMovies = Collections.emptyList();
                }
            }
        }
        return this.watchedMovies;
    }

    public List<TrackedShow> getWatchedShows() {
        if (watchedShows == null) {
            if (traktTvApi == null) {
                this.watchedShows = Collections.emptyList();
            } else {
                try {
                    // get watched movies from Trakt.TV
                    this.watchedShows = traktTvApi.syncService().getWatchedShows(Extended.MINIMAL);
                } catch (Exception ex) {
                    LOG.error("Failed to get watched shows", ex);
                    this.watchedShows = Collections.emptyList();
                }
            }
        }
        return watchedShows;
    }
}
