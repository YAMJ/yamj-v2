/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.cache.CacheMemory;
import com.omertron.fanarttvapi.FanartTvApi;
import com.omertron.fanarttvapi.FanartTvException;
import com.omertron.fanarttvapi.enumeration.FTArtworkType;
import com.omertron.fanarttvapi.enumeration.FTSourceType;
import com.omertron.fanarttvapi.model.ArtworkList;
import com.omertron.fanarttvapi.model.FTArtwork;
import com.omertron.fanarttvapi.model.FTMovie;
import com.omertron.fanarttvapi.model.FTSeries;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FanartTvPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FanartTvPlugin.class);
    private static final String LOG_MESSAGE = "FanartTvPlugin: ";
    public static final String FANARTTV_PLUGIN_ID = "fanarttv";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_FanartTv");
    private FanartTvApi ft;
    private static final String WEBHOST = "fanart.tv";
    private static final Map<FTArtworkType, Integer> ARTWORK_TYPES = new EnumMap<>(FTArtworkType.class);
    private static int totalRequiredTv = 0;
    private static int totalRequireMovie = 0;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String LANG_MOVIE = getMovieLanguage();
    private static final String LANG_TV = getTvLanguage();

    static {
        // Read the properties for the artwork required and the quantities

        // The propery name should be named like: "{artworkType}.{artworkSource}.download", e.g. clearart.tv.download
        StringBuilder artworkPropertyName;
        for (FTArtworkType artworkType : EnumSet.allOf(FTArtworkType.class)) {
            artworkPropertyName = new StringBuilder(artworkType.toString().toLowerCase());
            artworkPropertyName.append('.');
            artworkPropertyName.append(artworkType.getSourceType().toString().toLowerCase());
            artworkPropertyName.append(".download");

            if (PropertiesUtil.getBooleanProperty(artworkPropertyName.toString(), Boolean.FALSE)) {
                ARTWORK_TYPES.put(artworkType, 1);
                LOG.info("{}{} required", LOG_MESSAGE, artworkPropertyName);
            } else {
                LOG.info("{}{} not required", LOG_MESSAGE, artworkPropertyName);
            }
        }

        if (ARTWORK_TYPES.size() > 0) {
            LOG.debug("{}Looking for {} Fanart.TV Types", LOG_MESSAGE, ARTWORK_TYPES.toString());
        } else {
            LOG.debug("{}No Fanart.TV artwork required.", LOG_MESSAGE);
        }
    }

    public FanartTvPlugin() {
        try {
            this.ft = new FanartTvApi(API_KEY);
        } catch (FanartTvException ex) {
            LOG.warn("{}Failed to initialise FanartTV API: {}", LOG_MESSAGE, ex.getMessage(), ex);
            return;
        }
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        // Calculate the required number of artworks (Only do it once though)
        if (totalRequireMovie + totalRequiredTv == 0) {
            for (FTArtworkType key : ARTWORK_TYPES.keySet()) {
                if (key.getSourceType() == FTSourceType.MOVIE) {
                    totalRequireMovie += ARTWORK_TYPES.get(key);
                } else if (key.getSourceType() == FTSourceType.TV) {
                    totalRequiredTv += ARTWORK_TYPES.get(key);
                } else {
                    LOG.trace("{}Skipped artwork type '{}' as its source type is '{}' and not required", LOG_MESSAGE, key, key.getSourceType());
                }
            }
        }
    }

    /**
     * Scan and return all artwork types (Defaults type to null)
     *
     * @param movie
     * @return
     */
    public boolean scan(Movie movie) {
        return scan(movie, null);
    }

    /**
     * Scan and return the artwork type requested (or all if type is null)
     *
     * @param movie
     * @param artworkType Artwork type required (null is all)
     * @return
     */
    public boolean scan(Movie movie, FTArtworkType artworkType) {
        if (artworkType != null && !ARTWORK_TYPES.containsKey(artworkType)) {
            LOG.debug(LOG_MESSAGE + artworkType.toString().toLowerCase() + " not required");
            return true;
        }

        ArtworkList ftArtwork;
        String requiredLanguage;

        Map<FTArtworkType, Integer> requiredArtworkTypes = new EnumMap<>(ARTWORK_TYPES);

        if (movie.isTVShow()) {
            int tvdbid = NumberUtils.toInt(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID), 0);

            // Remove the non-TV types
            for (FTArtworkType at : requiredArtworkTypes.keySet()) {
                if (at.getSourceType() != FTSourceType.TV) {
                    requiredArtworkTypes.remove(at);
                }
            }

            // Get all the artwork to speed up any subsequent requests
            ftArtwork = getTvArtwork(tvdbid);
            requiredLanguage = LANG_TV;
        } else {
            int tmdbId = NumberUtils.toInt(movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID), 0);

            // Remove the non-Movie types
            for (FTArtworkType at : requiredArtworkTypes.keySet()) {
                if (at.getSourceType() != FTSourceType.MOVIE) {
                    requiredArtworkTypes.remove(at);
                }
            }

            // Get all the artwork to speed up any subsequent requests
            ftArtwork = getMovieArtwork(tmdbId, movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
            requiredLanguage = LANG_MOVIE;
        }

        if (ftArtwork.hasArtwork()) {
            LOG.debug("{}Found {} artwork items", LOG_MESSAGE, ftArtwork.getArtwork().size());

            FTArtworkType ftType;

            for (Map.Entry<FTArtworkType, List<FTArtwork>> entry : ftArtwork.getArtwork().entrySet()) {
                LOG.trace("{}Found '{}' with {} items", LOG_MESSAGE, entry.getKey(), entry.getValue().size());
                ftType = entry.getKey();

                if (requiredArtworkTypes.containsKey(ftType) && requiredArtworkTypes.get(ftType) > 0) {
                    LOG.trace("{}Processing '{}' artwork, {} are requried", LOG_MESSAGE, entry.getKey(), requiredArtworkTypes.get(ftType));
                    int left = processArtworkToMovie(movie, ftType, requiredLanguage, requiredArtworkTypes.get(ftType), entry.getValue());
                    // Update the required artwork counter
                    requiredArtworkTypes.put(ftType, left);
                    // remove the count from the requiredQuantity
                }

            }

            int requiredQuantity = 0;
            for (Map.Entry<FTArtworkType, Integer> entry : requiredArtworkTypes.entrySet()) {
                requiredQuantity += entry.getValue();
            }

            if (requiredQuantity > 0) {
                LOG.debug("{}Not all required artwork was found for '{}' - {}", LOG_MESSAGE, movie.getBaseName(), requiredArtworkTypes.toString());
                return false;
            } else {
                LOG.debug("{}All required artwork was found for '{}'", LOG_MESSAGE, movie.getBaseName());
                return true;
            }
        } else {
            LOG.debug("{}No artwork found for {}", LOG_MESSAGE, movie.getBaseName());
            return false;
        }
    }

    /**
     * Process the artwork into the movie URLs
     *
     * @param movie Movie to add the artwork to
     * @param ftType Type of the artwork
     * @param reqAmount Number of items required
     * @param artworkList List of the artwork found on Fanart.TV
     * @return artwork remaining to be found
     */
    private int processArtworkToMovie(Movie movie, FTArtworkType ftType, final String reqLanguage, int reqAmount, final List<FTArtwork> artworkList) {
        LOG.trace("{}Getting {} of '{}' artwork, there are {} available, looking for '{}' language", LOG_MESSAGE, reqAmount, ftType, artworkList.size(), reqLanguage);

        int remaining = reqAmount;

        for (FTArtwork artwork : artworkList) {
            LOG.trace("{}Artwork: {}", LOG_MESSAGE, artwork);
            if (reqLanguage.equalsIgnoreCase(artwork.getLanguage())) {
                // send to function to add to movie
                if (addArtworkToMovie(movie, ftType, artwork)) {
                    remaining--;
                    if (remaining > 0) {
                        LOG.trace("{}{} has {} remaining", LOG_MESSAGE, ftType, remaining);
                    } else {
                        LOG.trace("{}All artwork for {} found.", LOG_MESSAGE, ftType);
                        break;
                    }
                }
            }
        }

        return remaining;
    }

    /**
     * Add the artwork URL to the movie
     *
     * @param movie
     * @param ftType
     * @param artwork
     * @return
     */
    private boolean addArtworkToMovie(Movie movie, FTArtworkType ftType, FTArtwork artwork) {
        boolean success;
        LOG.debug("{}Adding {} to movie '{}' with URL {}", LOG_MESSAGE, ftType, movie.getBaseName(), artwork.getUrl());

        switch (ftType) {
            case CLEARART:
                movie.setClearArtURL(artwork.getUrl());
                success = true;
                break;
            case CLEARLOGO:
                movie.setClearLogoURL(artwork.getUrl());
                success = true;
                break;
            case SEASONTHUMB:
                // Check this is the right season
                if (NumberUtils.toInt(artwork.getSeason(), -1) == movie.getSeason()) {
                    movie.setSeasonThumbURL(artwork.getUrl());
                    success = true;
                } else {
                    success = false;
                }
                break;
            case TVTHUMB:
                movie.setTvThumbURL(artwork.getUrl());
                success = true;
                break;
            case MOVIEART:
                movie.setClearArtURL(artwork.getUrl());
                success = true;
                break;
            case MOVIEDISC:
                movie.setMovieDiscURL(artwork.getUrl());
                success = true;
                break;
            case MOVIELOGO:
                movie.setClearLogoURL(artwork.getUrl());
                success = true;
                break;
            default:
                LOG.trace("{}Unrecognised artwork type '{}', ignoring.", LOG_MESSAGE, ftType.toString().toLowerCase());
                success = false;
        }

        return success;
    }

    /**
     * Get artwork for the TV
     *
     * @param tvdbId
     * @return
     */
    public FTSeries getTvArtwork(int tvdbId) {
        String key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, String.valueOf(tvdbId));

        @SuppressWarnings("unchecked")
        FTSeries ftArtwork = (FTSeries) CacheMemory.getFromCache(key);

        if (ftArtwork == null || ftArtwork.hasArtwork()) {
            ThreadExecutor.enterIO(WEBHOST);
            try {
                ftArtwork = ft.getTvArtwork(Integer.toString(tvdbId));

                if (ftArtwork != null && ftArtwork.hasArtwork()) {
                    CacheMemory.addToCache(key, ftArtwork);
                }

                return ftArtwork;
            } catch (FanartTvException ex) {
                LOG.warn("{}Failed to get fanart information for TVDB ID: {}. Error: {}", LOG_MESSAGE, tvdbId, ex.getMessage(), ex);
                return new FTSeries();
            } finally {
                ThreadExecutor.leaveIO();
            }
        } else {
            return ftArtwork;
        }
    }

    /**
     * Get artwork for the Movie
     *
     * @param tmdbId
     * @param imdbId
     * @return
     */
    public FTMovie getMovieArtwork(int tmdbId, String imdbId) {
        String key;

        if (StringTools.isValidString(imdbId)) {
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, imdbId);
        } else if (tmdbId > 0) {
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, Integer.toString(tmdbId));
        } else {
            // No valid ID provided, so quit.
            return new FTMovie();
        }

        FTMovie ftArtwork = (FTMovie) CacheMemory.getFromCache(key);

        // If we get nothing back from the cache or it's empty, check for artwork
        if (ftArtwork == null || !ftArtwork.hasArtwork()) {
            ThreadExecutor.enterIO(WEBHOST);
            try {
                if (StringTools.isValidString(imdbId)) {
                    ftArtwork = ft.getMovieArtwork(imdbId);
                } else {
                    ftArtwork = ft.getMovieArtwork(Integer.toString(tmdbId));
                }

                if (ftArtwork != null && ftArtwork.hasArtwork()) {
                    CacheMemory.addToCache(key, ftArtwork);
                }

                return ftArtwork;
            } catch (FanartTvException ex) {
                LOG.warn("{}Failed to get fanart information for IMDB ID: {} / TMDB ID: {}. Error: {}", LOG_MESSAGE, imdbId, tmdbId, ex.getMessage(), ex);
                return new FTMovie();
            } finally {
                ThreadExecutor.leaveIO();
            }
        } else {
            return ftArtwork;
        }
    }

    /**
     * Determine if artwork is required
     *
     * @param requiredType
     * @return
     */
    public static boolean isArtworkRequired(FTArtworkType requiredType) {
        if (ARTWORK_TYPES.containsKey(requiredType)) {
            return ARTWORK_TYPES.get(requiredType) > 0;
        } else {
            // Not found, so not required
            LOG.warn("{}{} is not a valid Fanart.TV type", LOG_MESSAGE, requiredType);
            return false;
        }
    }

    /**
     * Determine if artwork is required
     *
     * @param artworkType
     * @return
     */
    public static boolean isArtworkRequired(String artworkType) {
        FTArtworkType requiredType;
        try {
            requiredType = FTArtworkType.fromString(artworkType);
        } catch (IllegalArgumentException ex) {
            LOG.warn("{}{} is not a valid Fanart.TV type", LOG_MESSAGE, artworkType);
            return false;
        }

        return isArtworkRequired(requiredType);
    }

    /**
     * Get the language for the Movie search.
     *
     * Default to the fanart.tv setting and then try themoviedb setting
     *
     * @return
     */
    private static String getMovieLanguage() {
        String language = PropertiesUtil.getProperty("fanarttv.movie.language", "");
        if (StringUtils.isBlank(language)) {
            language = PropertiesUtil.getProperty("themoviedb.language", DEFAULT_LANGUAGE);
        }
        return language;
    }

    /**
     * Get the kanguage for the TV Search
     *
     * Default to the fanart.tv setting and then try thetvdb setting
     *
     * @return
     */
    private static String getTvLanguage() {
        String language = PropertiesUtil.getProperty("fanarttv.tv.language", "");
        if (StringUtils.isBlank(language)) {
            language = PropertiesUtil.getProperty("thetvdb.language", DEFAULT_LANGUAGE);
        }
        return language;
    }
}
