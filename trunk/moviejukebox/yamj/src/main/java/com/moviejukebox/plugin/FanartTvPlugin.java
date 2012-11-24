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

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.cache.CacheMemory;
import com.omertron.fanarttvapi.FanartTvApi;
import com.omertron.fanarttvapi.FanartTvException;
import com.omertron.fanarttvapi.model.FTArtworkType;
import com.omertron.fanarttvapi.model.FTSourceType;
import com.omertron.fanarttvapi.model.FanartTvArtwork;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class FanartTvPlugin {

    private static final Logger logger = Logger.getLogger(FanartTvPlugin.class);
    private static final String LOG_MESSAGE = "FanartTvPlugin: ";
    public static final String FANARTTV_PLUGIN_ID = "fanarttv";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_FanartTv");
    private FanartTvApi ft = new FanartTvApi(API_KEY);
    private static final String webhost = "fanart.tv";
    private static final Map<FTArtworkType, Integer> artworkTypes = new EnumMap<FTArtworkType, Integer>(FTArtworkType.class);
    private static int totalRequiredTv = 0;
    private static int totalRequireMovie = 0;
    private static final String DEFAULT_LANGUAGE = "en";
    private static String movieLanguage = getMovieLanguage();
    private static String tvLanguage = getTvLanguage();

    static {
        // Read the properties for the artwork required and the quantities

        // The propery name should be named like: "{artworkType}.{artworkSource}.download", e.g. clearart.tv.download
        StringBuilder artworkPropertyName;
        for (FTArtworkType artworkType : EnumSet.allOf(FTArtworkType.class)) {
            artworkPropertyName = new StringBuilder(artworkType.toString().toLowerCase());
            artworkPropertyName.append('.');
            artworkPropertyName.append(artworkType.getSourceType().toString().toLowerCase());
            artworkPropertyName.append(".download");

            if (PropertiesUtil.getBooleanProperty(artworkPropertyName.toString(), FALSE)) {
                artworkTypes.put(artworkType, 1);
            }
        }

        if (artworkTypes.size() > 0) {
            logger.debug(LOG_MESSAGE + "Looking for " + artworkTypes.toString() + " Fanart.TV Types");
        } else {
            logger.debug(LOG_MESSAGE + "No Fanart.TV artwork required.");
        }
    }

    public FanartTvPlugin() {
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        // Calculate the required number of artworks (Only do it once though)
        if (totalRequireMovie + totalRequiredTv == 0) {
            for (FTArtworkType key : artworkTypes.keySet()) {
                if (key == FTArtworkType.MOVIEART || key == FTArtworkType.MOVIEDISC || key == FTArtworkType.MOVIELOGO) {
                    totalRequireMovie += artworkTypes.get(key);
                } else {
                    totalRequiredTv += artworkTypes.get(key);
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

    public boolean scan(Movie movie, FTArtworkType artworkType) {
        if (artworkType != FTArtworkType.ALL && !artworkTypes.containsKey(artworkType)) {
            logger.debug(LOG_MESSAGE + artworkType.toString().toLowerCase() + " not required");
            return true;
        }

        List<FanartTvArtwork> ftArtwork;
        int requiredQuantity;
        String requiredLanguage;

        Map<FTArtworkType, Integer> requiredArtworkTypes = new EnumMap<FTArtworkType, Integer>(artworkTypes);

        if (movie.isTVShow()) {
            int tvdbid;
            try {
                tvdbid = Integer.parseInt(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID));
            } catch (NumberFormatException ex) {
                tvdbid = 0;
            }

            // Remove the non-TV types
            for (FTArtworkType at : requiredArtworkTypes.keySet()) {
                if (at.getSourceType() != FTSourceType.TV) {
                    requiredArtworkTypes.remove(at);
                }
            }

            // Get all the artwork to speed up any subsequent requests
            ftArtwork = getTvArtwork(tvdbid, FTArtworkType.ALL);
            requiredQuantity = totalRequiredTv;
            requiredLanguage = tvLanguage;
        } else {
            int tmdbId;
            try {
                tmdbId = Integer.parseInt(movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID));
            } catch (NumberFormatException ex) {
                tmdbId = 0;
            }

            // Remove the non-Movie types
            for (FTArtworkType at : requiredArtworkTypes.keySet()) {
                if (at.getSourceType() != FTSourceType.MOVIE) {
                    requiredArtworkTypes.remove(at);
                }
            }

            // Get all the artwork to speed up any subsequent requests
            ftArtwork = getMovieArtwork(tmdbId, movie.getId(ImdbPlugin.IMDB_PLUGIN_ID), FTArtworkType.ALL);
            requiredQuantity = totalRequireMovie;
            requiredLanguage = movieLanguage;
        }

        if (!ftArtwork.isEmpty()) {
            logger.debug(LOG_MESSAGE + "Found " + ftArtwork.size() + " artwork items");

            FTArtworkType ftType;
            int ftQuantity;

            for (FanartTvArtwork ftSingle : ftArtwork) {
                ftType = FTArtworkType.fromString(ftSingle.getType());

                if (requiredArtworkTypes.containsKey(ftType)) {

                    ftQuantity = requiredArtworkTypes.get(ftType);
                    if (ftQuantity > 0 && ftSingle.getLanguage().equalsIgnoreCase(requiredLanguage)) {
                        boolean foundOK = Boolean.FALSE;

//                        logger.info(LOG_MESSAGE + "Processing: " + ftSingle.toString());   // XXX DEBUG

                        if (ftType == FTArtworkType.CLEARART) {
                            movie.setClearArtURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else if (ftType == FTArtworkType.CLEARLOGO) {
                            movie.setClearLogoURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else if (ftType == FTArtworkType.SEASONTHUMB) {
                            // Check this is the right season
                            if (ftSingle.getSeason() == movie.getSeason()) {
                                movie.setSeasonThumbURL(ftSingle.getUrl());
                                foundOK = Boolean.TRUE;
                            }
                        } else if (ftType == FTArtworkType.TVTHUMB) {
                            movie.setTvThumbURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else if (ftType == FTArtworkType.MOVIEART) {
                            movie.setClearArtURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else if (ftType == FTArtworkType.MOVIEDISC) {
                            movie.setMovieDiscURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else if (ftType == FTArtworkType.MOVIELOGO) {
                            movie.setClearLogoURL(ftSingle.getUrl());
                            foundOK = Boolean.TRUE;
                        } else {
                            logger.debug("Unrecognised artwork type '" + ftType.toString().toLowerCase() + "', ignoring.");
                        }

                        // Reduce the quantity needed as this artwork was found
                        if (foundOK) {
                            // Reduce the global counter
                            requiredQuantity--;

                            // Update the specific counter
                            requiredArtworkTypes.put(ftType, --ftQuantity);
                        }

                        // Performance check, stop looking if there is no more artwork to find
                        if (requiredQuantity <= 0) {
                            logger.debug(LOG_MESSAGE + "All required artwork was found for " + movie.getBaseName() + " " + requiredArtworkTypes.toString());
                            break;
                        }
                    }
                }
            }

            if (requiredQuantity > 0) {
                logger.debug(LOG_MESSAGE + "Not all required artwork was found for " + movie.getBaseName() + " " + requiredArtworkTypes.toString());
            }

            return true;
        } else {
            logger.debug(LOG_MESSAGE + "No artwork found for " + movie.getBaseName());
            return false;
        }
    }

    public List<FanartTvArtwork> getTvArtwork(int tvdbId, FTArtworkType artworkType) {
        String key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, String.valueOf(tvdbId), artworkType.toString());

        List<FanartTvArtwork> ftArtwork = (List<FanartTvArtwork>) CacheMemory.getFromCache(key);

        if (ftArtwork == null || ftArtwork.isEmpty()) {
            ThreadExecutor.enterIO(webhost);
            try {
                ftArtwork = ft.getTvArtwork(tvdbId, artworkType);

                if (ftArtwork != null && !ftArtwork.isEmpty()) {
                    CacheMemory.addToCache(key, ftArtwork);
                }

                return ftArtwork;
            } catch (FanartTvException ex) {
                logger.warn(LOG_MESSAGE + "Failed to get fanart information");
                logger.warn(LOG_MESSAGE + "Failed to get fanart information for TVDB ID: " + tvdbId + ". Error: " + ex.getMessage());
                return new ArrayList<FanartTvArtwork>();
            } finally {
                ThreadExecutor.leaveIO();
            }
        } else {
            return ftArtwork;
        }
    }

    public List<FanartTvArtwork> getMovieArtwork(int tmdbId, String imdbId, FTArtworkType artworkType) {
        String key;

        if (StringTools.isValidString(imdbId)) {
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, imdbId, artworkType.toString());
        } else if (tmdbId > 0) {
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, String.valueOf(tmdbId), artworkType.toString());
        } else {
            key = Movie.UNKNOWN;
        }

        List<FanartTvArtwork> ftArtwork = (List<FanartTvArtwork>) CacheMemory.getFromCache(key);

        if (ftArtwork == null || ftArtwork.isEmpty()) {
            ThreadExecutor.enterIO(webhost);
            try {
                if (StringTools.isValidString(imdbId)) {
                    ftArtwork = ft.getMovieArtwork(imdbId, artworkType);
                } else {
                    ftArtwork = ft.getMovieArtwork(tmdbId, artworkType);
                }

                if (ftArtwork != null && !ftArtwork.isEmpty()) {
                    CacheMemory.addToCache(key, ftArtwork);
                }

                return ftArtwork;
            } catch (FanartTvException ex) {
                logger.warn(LOG_MESSAGE + "Failed to get fanart information for IMDB ID: " + imdbId + " / TMDB ID: " + tmdbId + ". Error: " + ex.getMessage());
                return new ArrayList<FanartTvArtwork>();
            } finally {
                ThreadExecutor.leaveIO();
            }
        } else {
            return ftArtwork;
        }
    }

    public static boolean isArtworkRequired(FTArtworkType requiredType) {
        if (artworkTypes.containsKey(requiredType)) {
            if (artworkTypes.get(requiredType) > 0) {
                return true;
            } else {
                // None required
                return false;
            }
        } else {
            // Not found, so not required
            return false;
        }
    }

    public static boolean isArtworkRequired(String artworkType) {
        FTArtworkType requiredType;
        try {
            requiredType = FTArtworkType.fromString(artworkType);
        } catch (IllegalArgumentException ex) {
            logger.warn(LOG_MESSAGE + artworkType + " is not a valid Fanart.TV type");
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
