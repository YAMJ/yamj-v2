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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.traileraddictapi.TrailerAddictApi;
import com.moviejukebox.traileraddictapi.TrailerAddictException;
import com.moviejukebox.traileraddictapi.model.Trailer;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * @author iuk
 *
 */
public class TrailerAddictPlugin extends TrailerPlugin {

    private static final Logger logger = Logger.getLogger(TrailerAddictPlugin.class);
    private int trailerMaxCount;

    public TrailerAddictPlugin() {
        super();
        trailersPluginName = "TrailerAddict";
        logMessage = "TrailerAddictPlugin: ";
        trailerMaxCount = PropertiesUtil.getIntProperty("traileraddict.max", "3");
    }

    @Override
    public final boolean generate(Movie movie) {
        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        String imdbId = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (StringTools.isNotValidString(imdbId)) {
            logger.debug(logMessage + "No IMDB Id found for " + movie.getBaseName() + ", trailers not downloaded");
            return Boolean.FALSE;
        }

        List<Trailer> trailerList;
        try {
            trailerList = TrailerAddictApi.getFilmImdb(imdbId, trailerMaxCount);
        } catch (TrailerAddictException ex) {
            logger.warn(logMessage + "Failed to get trailer information: " + ex.getResponse());
            return Boolean.FALSE;
        }

        if (trailerList.isEmpty()) {
            logger.debug(logMessage + "No trailers found for " + movie.getBaseName());
            return Boolean.FALSE;
        }

        for (Trailer trailer : trailerList) {
            logger.debug(logMessage + "Found trailer at URL " + trailer.getLink());

            MovieFile tmf = new MovieFile();
            tmf.setTitle("TRAILER-" + trailer.getCombinedTitle());

            String trailerUrl = getDownloadUrl(trailer);
            if (StringTools.isValidString(trailerUrl)) {
                if (isDownload()) {
                    if (!downloadTrailer(movie, trailerUrl, FileTools.makeSafeFilename(trailer.getCombinedTitle()), tmf)) {
                        return Boolean.FALSE;
                    }
                } else {
                    tmf.setFilename(trailerUrl);
                    movie.addExtraFile(new ExtraFile(tmf));
                }
            } else {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    @Override
    public String getName() {
        return trailersPluginName.toLowerCase();
    }

    /**
     * Get the download URL for a trailer
     *
     * @param trailer
     * @return
     */
    private String getDownloadUrl(Trailer trailer) {
        String downloadPage;
        try {
            downloadPage = webBrowser.request(trailer.getTrailerDownloadUrl());
        } catch (IOException ex) {
            logger.warn(logMessage + "Failed to get webpage: " + ex.getMessage());
            return Movie.UNKNOWN;
        }

        int startPos = downloadPage.indexOf("fileurl=");
        if (startPos > -1) {
            return downloadPage.substring(startPos + 8, downloadPage.indexOf("%", startPos));
        } else {
            logger.debug(logMessage + "Download URL not found for " + trailer.getCombinedTitle());
        }

        return Movie.UNKNOWN;
    }
}
