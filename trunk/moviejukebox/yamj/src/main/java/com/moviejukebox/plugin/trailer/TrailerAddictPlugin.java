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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.omertron.traileraddictapi.TrailerAddictApi;
import com.omertron.traileraddictapi.TrailerAddictException;
import com.omertron.traileraddictapi.model.Trailer;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author iuk
 *
 */
public class TrailerAddictPlugin extends TrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerAddictPlugin.class);
    private int trailerMaxCount;

    public TrailerAddictPlugin() {
        super();
        trailersPluginName = "TrailerAddict";
        logMessage = "TrailerAddictPlugin: ";
        trailerMaxCount = PropertiesUtil.getIntProperty("traileraddict.max", 3);
    }

    @Override
    public final boolean generate(Movie movie) {
        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        String imdbId = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (StringTools.isNotValidString(imdbId)) {
            LOG.debug(logMessage + "No IMDB Id found for " + movie.getBaseName() + ", trailers not downloaded");
            return Boolean.FALSE;
        }

        List<Trailer> trailerList;
        try {
            trailerList = TrailerAddictApi.getFilmImdb(imdbId, trailerMaxCount);
        } catch (TrailerAddictException ex) {
            LOG.warn(logMessage + "Failed to get trailer information: " + ex.getResponse());
            return Boolean.FALSE;
        }

        if (trailerList.isEmpty()) {
            LOG.debug(logMessage + "No trailers found for " + movie.getBaseName());
            return Boolean.FALSE;
        }

        for (Trailer trailer : trailerList) {
            LOG.debug(logMessage + "Found trailer at URL " + trailer.getLink());

            ExtraFile extra = new ExtraFile();
            extra.setTitle("TRAILER-" + trailer.getCombinedTitle());

            String trailerUrl = getDownloadUrl(trailer);
            if (StringTools.isValidString(trailerUrl)) {
                if (isDownload()) {
                    if (!downloadTrailer(movie, trailerUrl, FileTools.makeSafeFilename(trailer.getCombinedTitle()), extra)) {
                        return Boolean.FALSE;
                    }
                } else {
                    extra.setFilename(trailerUrl);
                    movie.addExtraFile(extra);
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
            LOG.warn(logMessage + "Failed to get webpage: " + ex.getMessage());
            return Movie.UNKNOWN;
        }

        int startPos = downloadPage.indexOf("fileurl=");
        if (startPos > -1) {
            return downloadPage.substring(startPos + 8, downloadPage.indexOf('%', startPos));
        } else {
            LOG.debug(logMessage + "Download URL not found for " + trailer.getCombinedTitle());
        }

        return Movie.UNKNOWN;
    }
}
