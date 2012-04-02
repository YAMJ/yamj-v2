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
import com.moviejukebox.fanarttv.model.FanartTvArtwork;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

public class FanartTvPlugin {

    private static final Logger logger = Logger.getLogger(FanartTvPlugin.class);
    private static final String logMessage = "FanartTvPlugin: ";
    public static final String FANARTTV_PLUGIN_ID = "fanarttv";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_FanartTv");
    private FanartTv ft = new FanartTv(API_KEY);
    private static final String webhost = "fanart.tv";
    private static final HashMap<String, Integer> artworkTypes = new HashMap<String, Integer>();
    private static int totalRequired = 0;
    private static final String movieLanguage = PropertiesUtil.getProperty("themoviedb.language", "en");
    private static final String tvLanguage = PropertiesUtil.getProperty("thetvdb.language", "en");

    static {
        // Read the properties for the artwork required and the quantities
        List<String> requiredArtworkTypes = Arrays.asList(PropertiesUtil.getProperty("fanarttv.types", "clearart,clearlogo,seasonthumb,tvthumb,cdart").toLowerCase().split(","));
        logger.debug(logMessage + "Looking for " + requiredArtworkTypes.toString() + " Fanart.TV Types");
        for (String artworkType : requiredArtworkTypes) {
            // For the time being limit the max to 1
            int artworkQuantity = Math.min(1, PropertiesUtil.getIntProperty("fanarttv.quantity." + artworkType, "0"));
            artworkTypes.put(artworkType, artworkQuantity);
            if (artworkQuantity > 0) {
                logger.debug(logMessage + "Getting maximum of " + artworkQuantity + " " + artworkType);
            } else {
                logger.debug(logMessage + "No " + artworkType + " required");
            }
        }
    }

    public FanartTvPlugin() {
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        // Calculate the required number of artworks
        for (String key : artworkTypes.keySet()) {
            totalRequired += artworkTypes.get(key);
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

    public boolean scan(Movie movie, String artworkType) {
        String tvdbidString = movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID);
        int tvdbid = 0;

        if (StringTools.isValidString(tvdbidString)) {
            try {
                tvdbid = Integer.parseInt(tvdbidString);
            } catch (Exception error) {
                tvdbid = 0;
            }
        }

        if (tvdbid > 0) {
            List<FanartTvArtwork> ftArtwork = getFanartTvArtwork(tvdbid, artworkType);

            logger.debug(logMessage + "Found " + ftArtwork.size() + (StringTools.isValidString(artworkType) ? artworkType : "") + " artwork items");

            String ftType;
            int ftQuantity;
            int requiredQuantity = totalRequired;
            HashMap<String, Integer> requiredArtworkTypes = new HashMap<String, Integer>(artworkTypes);

            for (FanartTvArtwork ftSingle : ftArtwork) {
                ftType = ftSingle.getType();

                if (requiredArtworkTypes.containsKey(ftType)) {

                    ftQuantity = requiredArtworkTypes.get(ftType);
                    if (ftQuantity > 0 && ftSingle.getLanguage().equalsIgnoreCase(tvLanguage)) {
                        boolean foundOK = Boolean.FALSE;

                        if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_CLEARART)) {
                            movie.setClearArtURL(ftSingle.getUrl());
                            movie.setClearArtFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_CLEARART));
                            foundOK = Boolean.TRUE;
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_CLEARLOGO)) {
                            movie.setClearLogoURL(ftSingle.getUrl());
                            movie.setClearLogoFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_CLEARLOGO));
                            foundOK = Boolean.TRUE;
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_TVTHUMB)) {
                            movie.setTvThumbURL(ftSingle.getUrl());
                            movie.setTvThumbFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_TVTHUMB));
                            foundOK = Boolean.TRUE;
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_SEASONTHUMB)) {
                            if (ftSingle.getSeason() == movie.getSeason()) {
                                movie.setSeasonThumbURL(ftSingle.getUrl());
                                movie.setSeasonThumbFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_SEASONTHUMB));
                                foundOK = Boolean.TRUE;
                            }
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_CDART)) {
                            movie.setCdArtURL(ftSingle.getUrl());
                            movie.setCdArtFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_CDART));
                            foundOK = Boolean.TRUE;
                        } else {
                            logger.debug("Unrecognised artwork type '" + ftType + "', ignoring.");
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
                            logger.debug(logMessage + "All required artwork was found");
                            break;
                        }
                    } else {
                        logger.debug(logMessage + "No more " + ftType + " are required, skipping.");
                    }
                }
            }

            if (requiredQuantity > 0) {
                logger.debug(logMessage + "Not all required artwork was found for " + movie.getBaseName());
            }

            return true;
        } else {
            logger.debug(logMessage + "No artwork found for " + movie.getBaseName() + " with TVDBID: " + tvdbidString);
            return false;
        }
    }

    public List<FanartTvArtwork> getFanartTvArtwork(int tvdbId, String artworkType) {
        String key;
        if (StringTools.isValidString(artworkType)) {
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, String.valueOf(tvdbId), artworkType);
        } else {
            // Default to the "all" value for artwork
            key = CacheMemory.generateCacheKey(FANARTTV_PLUGIN_ID, String.valueOf(tvdbId), FanartTvArtwork.TYPE_ALL);
        }

        List<FanartTvArtwork> ftArtwork = (List<FanartTvArtwork>) CacheMemory.getFromCache(key);

        if (ftArtwork == null || ftArtwork.isEmpty()) {
            ThreadExecutor.enterIO(webhost);
            try {
                ftArtwork = ft.getArtwork(tvdbId, artworkType);

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

    private String makeSafeFilename(Movie movie, String artworkType) {
        StringBuilder filename = new StringBuilder();

        filename.append(FileTools.makeSafeFilename(movie.getBaseName()));
        filename.append(".").append(artworkType);
        filename.append(".").append(PropertiesUtil.getProperty(artworkType + ".format", "png"));

        return filename.toString();
    }

    public static boolean isArtworkRequired(String artworkType) {
        if (artworkTypes.containsKey(artworkType)) {
            if (artworkTypes.get(artworkType) > 0) {
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
}
