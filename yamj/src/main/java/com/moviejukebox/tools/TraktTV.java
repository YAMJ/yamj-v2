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

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.trakttv.TraktTvApi;
import org.yamj.api.trakttv.auth.TokenResponse;
import org.yamj.api.trakttv.model.TrackedMovie;
import org.yamj.api.trakttv.model.TrackedShow;
import org.yamj.api.trakttv.model.enumeration.Extended;

public class TraktTV {

    public static final String SCANNER_ID = "trakttv";
    private static final Logger LOG = LoggerFactory.getLogger(TraktTV.class);
    private static TraktTV INSTANCE;
    private static final ReentrantLock LOCK = new ReentrantLock(true);
    private static final String PROPS_FILE = "properties/trakttv.properties";
    private static final String PROP_ACCESS_TOKEN = "accessToken";
    private static final String PROP_REFRESH_TOKEN = "refreshToken";
    private static final String PROP_EXPIRATION_DATE = "expirationDate";
    
    private TraktTvApi traktTvApi = null;
    private boolean initialized = false;
    private boolean preloadWatchedMovies = false;
    private boolean preloadWatchedShows = false;
    private List<TrackedMovie> watchedMovies;
    private List<TrackedShow> watchedShows;
    private String refreshToken = null;
    private long expirationDate = 0;
    
    private TraktTV() {
        // empty constructor
    }
    
    public static TraktTV getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TraktTV();
        }
        return INSTANCE;
    }
    
    public TraktTV initialize() {
        if (traktTvApi != null) {
            // nothing to do if API already set
            return this;
        }
        
        final String clientId = PropertiesUtil.getProperty("API_KEY_TraktTV_ClientId");
        final String secret = PropertiesUtil.getProperty("API_KEY_TraktTV_Secret");

        try {
            LOG.trace("Initialize Trakt.TV API");
            traktTvApi = new TraktTvApi(clientId, secret, YamjHttpClientBuilder.getHttpClient());

            // set access token from properties
            Properties props = new Properties();
            File propsFile = new File(PROPS_FILE);
            if (propsFile.exists()) {
                try (InputStream is = new FileInputStream(propsFile)) {
                    props.load(is);
                    traktTvApi.setAccessToken(props.getProperty(PROP_ACCESS_TOKEN));
                    refreshToken = props.getProperty(PROP_REFRESH_TOKEN);
                    expirationDate = NumberUtils.toLong(props.getProperty(PROP_EXPIRATION_DATE), 0);
                    
                    // API is now initialized
                    this.initialized = true;
                } catch (Exception e) {
                    LOG.error("Failed to load Trakt.TV properties");
                }
            }
        }  catch (Exception ex) {
            LOG.error("Failed to initialize Trakt.TV API", ex);
        }
        return this;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void authorizeWithPin(String pin) {
        if (traktTvApi != null) {
            try {
                TokenResponse response = this.traktTvApi.requestAccessTokenByPin(pin);
    
                // set access token for API
                traktTvApi.setAccessToken(response.getAccessToken());
                LOG.info("Successfully authorized access to Trakt.TV");

                // store properties
                storeTraktTvProperties(response);
            } catch (Exception e) {
                LOG.error("Failed to authorize to Trakt.TV", e);
            }
        } else {
            LOG.error("Trakt.TV API is not initialized");
        }
    }
    
    public TraktTV refreshIfNecessary() {
        if (initialized && (expirationDate-86400000 < System.currentTimeMillis())) {

            LOG.info("Trakt.TV authorization expired; requesting new access token");
            
            if (StringUtils.isBlank(refreshToken)) {
                LOG.warn("Refresh token not present; please authorize again");
                // Trakt.TV is not initialized any more
                initialized = false;
            } else {       
                try {
                    // retrieve access token via refresh token
                    TokenResponse response = traktTvApi.requestAccessTokenByRefresh(refreshToken);

                    // set access token for API
                    traktTvApi.setAccessToken(response.getAccessToken());
                    LOG.info("Sucessfully refreshed access token");
                    
                    // store properties
                    storeTraktTvProperties(response);
                } catch (Exception ex) {
                    LOG.error("Failed to refresh access token", ex);
                    // Trakt.TV is not initialized any more
                    initialized = false;
                }
            }
        }
        return this;
    }
    
    private static void storeTraktTvProperties(TokenResponse response) {
        // set properties
        Properties props = new Properties();
        props.setProperty(PROP_ACCESS_TOKEN, response.getAccessToken());
        props.setProperty(PROP_REFRESH_TOKEN, response.getRefreshToken());
        props.setProperty(PROP_EXPIRATION_DATE, String.valueOf(buildExpirationDate(response)));
        
        // store properties file
        File propsFile = new File(PROPS_FILE);
        try (OutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "Trakt.TV settings");
        } catch (Exception e ) {
            LOG.error("Failed to store Trakt.TV properties");
        }
    }
    
    private static long buildExpirationDate(TokenResponse response) {
        // expiration date: creation date + expiration period * 1000 (cause given in seconds)
        return (response.getCreatedAt() + response.getExpiresIn()) * 1000L;
    }

    public void preloadWatched() {
        if (this.initialized && !this.preloadWatchedMovies) {
            try {
                // get watched movies from Trakt.TV
                this.watchedMovies = traktTvApi.syncService().getWatchedMovies(Extended.MINIMAL);
                LOG.info("Found {} watched movies on Trakt.TV", this.watchedMovies.size());
                this.preloadWatchedMovies = true;
            } catch (Exception ex) {
                LOG.error("Failed to get watched movies", ex);
            }
        }
        
        if (this.initialized && !this.preloadWatchedShows) {
            try {
                // get watched movies from Trakt.TV
                this.watchedShows = traktTvApi.syncService().getWatchedShows(Extended.MINIMAL);
                LOG.info("Found {} watched shows on Trakt.TV", this.watchedShows.size());
                this.preloadWatchedShows = true;
            } catch (Exception ex) {
                LOG.error("Failed to get watched shows", ex);
            }
        }
    }
    
    public boolean isPreloadWatchedMovies() {
        return preloadWatchedMovies;
    }

    public boolean isPreloadWatchedShows() {
        return preloadWatchedShows;
    }

    public List<TrackedMovie> getWatchedMovies() {
        return watchedMovies;
    }

    public List<TrackedShow> getWatchedShows() {
        return watchedShows;
    }
}
