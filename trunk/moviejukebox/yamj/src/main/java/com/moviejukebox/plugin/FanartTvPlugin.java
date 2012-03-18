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
import com.moviejukebox.fanarttv.model.FanartTvArtwork;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

public class FanartTvPlugin {

    private FanartTv ft = new FanartTv();
    private List<FanartTvArtwork> ftArtwork = new ArrayList<FanartTvArtwork>();
    private static String logMessage = "FanartTvPlugin: ";
    protected static Logger logger = Logger.getLogger(FanartTvPlugin.class);
    private static final String webhost = "fanart.tv";
    private static HashMap<String, Integer> artworkTypes = new HashMap<String, Integer>();
    private static int totalRequired = 0;

    public FanartTvPlugin() {
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        // Read the properties for the artwork required and the quantities
        List<String> requiredArtworkTypes = Arrays.asList(PropertiesUtil.getProperty("fanarttv.types", "clearart,clearlogo,seasonthumb,tvthumb").toLowerCase().split(","));
        logger.debug(logMessage+"Looking for " + requiredArtworkTypes.toString() + " Fanart.TV Types");
        for (String artworkType : requiredArtworkTypes) {
            // For the time being limit the max to 1
            int artworkQuantity = Math.min(1, PropertiesUtil.getIntProperty("fanarttv.quantity." + artworkType, "0"));
            artworkTypes.put(artworkType, artworkQuantity);
            if (artworkQuantity > 0) {
                logger.debug(logMessage + "Getting maximum of " + artworkQuantity + " " + artworkType);
                totalRequired += artworkQuantity;
            } else {
                logger.debug(logMessage + "No " + artworkType + " required");
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
            ftArtwork = getFanartTvArtwork(tvdbid, artworkType);
            logger.debug(logMessage + "Found " + ftArtwork.size() + (StringTools.isValidString(artworkType) ? artworkType : "") + " artwork items");

//            Artwork movieArtwork;
            String ftType;
            int ftQuantity;
            int requiredQuantity = totalRequired;

            for (FanartTvArtwork ftSingle : ftArtwork) {
                logger.info("Processing: " + ftSingle.toString());
                ftType = ftSingle.getType();

                if (artworkTypes.containsKey(ftType)) {
                    ftQuantity = artworkTypes.get(ftType);

                    if (ftQuantity > 0) {
                        artworkTypes.put(ftType, --ftQuantity);

                        /*
                         * TODO: Add this back in when we use the new artwork
                         * movieArtwork = new Artwork();
                         * movieArtwork.setSourceSite("fanarttv");
                         * movieArtwork.setType(ArtworkType.fromString(ftSingle.getType()));
                         * movieArtwork.setUrl(ftSingle.getUrl());
                         * movie.addArtwork(movieArtwork);
                         * logger.info(logMessage + "Added " +
                         * movieArtwork.toString());
                         */

                        if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_CLEARART)) {
                            movie.setClearartURL(ftSingle.getUrl());
                            movie.setClearartFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_CLEARART));
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_CLEARLOGO)) {
                            movie.setClearlogoURL(ftSingle.getUrl());
                            movie.setClearlogoFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_CLEARLOGO));
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_TVTHUMB)) {
                            movie.setTvthumbURL(ftSingle.getUrl());
                            movie.setTvthumbFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_TVTHUMB));
                        } else if (ftType.equalsIgnoreCase(FanartTvArtwork.TYPE_SEASONTHUMB)) {
                            movie.setSeasonThumbFilename(ftSingle.getUrl());
                            movie.setSeasonThumbFilename(makeSafeFilename(movie, FanartTvArtwork.TYPE_SEASONTHUMB));
                        } else {
                            logger.debug("Unrecognised artwork type '" + ftType + "'");
                        }

                        // Performance check, stop looking if there is no more artwork to find
                        if (--requiredQuantity <= 0) {
                            logger.info(logMessage + "All required artwork was found");
                            break;
                        }
                    } else {
                        logger.info(logMessage + "No more " + ftType + " are required, skipping.");
                    }
                }
            }

            if (requiredQuantity > 0) {
                logger.info(logMessage + "Not all required artwork was found for " + movie.getBaseName());
            }

            return true;
        } else {
            logger.debug(logMessage + "No artwork found for " + movie.getBaseName() + " with TVDBID: " + tvdbidString);
            return false;
        }
    }

    public List<FanartTvArtwork> getFanartTvArtwork(int tvdbid, String artworkType) {
        ThreadExecutor.enterIO(webhost);
        try {
            return ft.getArtwork(tvdbid, artworkType, null);
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    private String makeSafeFilename(Movie movie, String artworkType) {
        StringBuilder filename = new StringBuilder();

        filename.append(FileTools.makeSafeFilename(movie.getBaseName()));
        filename.append(".").append(artworkType);
        filename.append(".").append(PropertiesUtil.getProperty(artworkType + ".format", "png"));

        return filename.toString();
    }
}
