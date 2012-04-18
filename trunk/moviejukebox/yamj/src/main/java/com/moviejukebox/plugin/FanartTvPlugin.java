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

import com.moviejukebox.fanarttv.FanartTv;
import com.moviejukebox.fanarttv.FanartTvException;
import com.moviejukebox.fanarttv.model.FTArtworkType;
import com.moviejukebox.fanarttv.model.FanartTvArtwork;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.*;
import java.util.*;
import org.apache.log4j.Logger;

public class FanartTvPlugin {

    private static final Logger logger = Logger.getLogger(FanartTvPlugin.class);
    private static final String logMessage = "FanartTvPlugin: ";
    public static final String FANARTTV_PLUGIN_ID = "fanarttv";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_FanartTv");
    private FanartTv ft = new FanartTv(API_KEY);
    private static final String webhost = "fanart.tv";
    private static final boolean extraArtworkDownload = PropertiesUtil.getBooleanProperty("mjb.includeExtraArtwork", "false");
    private static final Map<FTArtworkType, Integer> artworkTypes = new EnumMap<FTArtworkType, Integer>(FTArtworkType.class);
    private static int totalRequiredTv = 0;
    private static int totalRequireMovie = 0;
    private static final String movieLanguage = PropertiesUtil.getProperty("themoviedb.language", "en");
    private static final String tvLanguage = PropertiesUtil.getProperty("thetvdb.language", "en");
    private static boolean versionInfoShown = Boolean.FALSE;

    static {
        logger.debug(logMessage + "Available Fanart.TV types: " + EnumSet.allOf(FTArtworkType.class).toString().toLowerCase());
        // Read the properties for the artwork required and the quantities
        List<String> requiredArtworkTypes = Arrays.asList(PropertiesUtil.getProperty("fanarttv.types", "clearart,clearlogo,seasonthumb,tvthumb,movielogo,moviedisc").toLowerCase().split(","));
        for (String artworkType : requiredArtworkTypes) {
            try {
                // For the time being limit the max to 1
                int artworkQuantity = Math.min(1, PropertiesUtil.getIntProperty("fanarttv.quantity." + artworkType, "0"));
                artworkTypes.put(FTArtworkType.fromString(artworkType), artworkQuantity);
//                if (artworkQuantity > 0) {
//                    logger.debug(logMessage + "Getting maximum of " + artworkQuantity + " " + artworkType);
//                } else {
//                    logger.debug(logMessage + "No " + artworkType + " required");
//                }
            } catch (IllegalArgumentException ex) {
                logger.warn(logMessage + "Artwork type '" + artworkType + "' not recognised");
            }
        }
        logger.debug(logMessage + "Looking for " + artworkTypes.toString() + " Fanart.TV Types");
    }

    public FanartTvPlugin() {
        if (!versionInfoShown) {
            FanartTv.showVersion();
            versionInfoShown = Boolean.TRUE;
        }

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
            requiredArtworkTypes.remove(FTArtworkType.MOVIEART);
            requiredArtworkTypes.remove(FTArtworkType.MOVIEDISC);
            requiredArtworkTypes.remove(FTArtworkType.MOVIELOGO);

            ftArtwork = getTvArtwork(tvdbid, artworkType);
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
            requiredArtworkTypes.remove(FTArtworkType.CLEARART);
            requiredArtworkTypes.remove(FTArtworkType.CLEARLOGO);
            requiredArtworkTypes.remove(FTArtworkType.SEASONTHUMB);
            requiredArtworkTypes.remove(FTArtworkType.TVTHUMB);

            ftArtwork = getMovieArtwork(tmdbId, movie.getId(ImdbPlugin.IMDB_PLUGIN_ID), artworkType);
            requiredQuantity = totalRequireMovie;
            requiredLanguage = movieLanguage;
        }

        if (!ftArtwork.isEmpty()) {
            logger.debug(logMessage + "Found " + ftArtwork.size() + (artworkType == FTArtworkType.ALL ? "" : artworkType.toString().toLowerCase()) + " artwork items");

            FTArtworkType ftType;
            int ftQuantity;

            for (FanartTvArtwork ftSingle : ftArtwork) {
                ftType = FTArtworkType.fromString(ftSingle.getType());

                if (requiredArtworkTypes.containsKey(ftType)) {

                    ftQuantity = requiredArtworkTypes.get(ftType);
                    if (ftQuantity > 0 && ftSingle.getLanguage().equalsIgnoreCase(requiredLanguage)) {
//                        logger.info("Need " + ftQuantity + " more");
                        boolean foundOK = Boolean.FALSE;

//                        logger.info(logMessage + "Processing: " + ftSingle.toString());   // XXX DEBUG

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
                            logger.debug(logMessage + "All required artwork was found for " + movie.getBaseName() + " " + requiredArtworkTypes.toString());
                            break;
                        }
                    } else {
//                        logger.debug(logMessage + "No more " + ftType.toString().toLowerCase() + " are required, skipping.");
                    }
                }
            }

            if (requiredQuantity > 0) {
                logger.debug(logMessage + "Not all required artwork was found for " + movie.getBaseName() + " " + requiredArtworkTypes.toString());
            }

            return true;
        } else {
            logger.debug(logMessage + "No artwork found for " + movie.getBaseName());
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
                logger.warn(logMessage + "Failed to get fanart information");
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
                logger.warn(logMessage + "Failed to get fanart information");
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
            logger.warn(logMessage + artworkType + " is not a valid Fanart.TV type");
            return false;
        }

        return isArtworkRequired(requiredType);
    }
}
