/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.ThreadExecutor;
import com.omertron.tvrageapi.TVRageApi;
import com.omertron.tvrageapi.model.CountryDetail;
import com.omertron.tvrageapi.model.Episode;
import com.omertron.tvrageapi.model.EpisodeList;
import com.omertron.tvrageapi.model.ShowInfo;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * @author Stuart.Boston
 */
public class TVRagePlugin extends ImdbPlugin {

    private static final Logger LOG = Logger.getLogger(TVRagePlugin.class);
    private static final String LOG_MESSAGE = "TVRagePlugin: ";
    public static final String TVRAGE_PLUGIN_ID = "tvrage";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TVRage");
    private static final String webhost = "tvrage.com";
    private TVRageApi tvRage;
    private boolean includeVideoImages;

    public TVRagePlugin() {
        super();
        tvRage = new TVRageApi(API_KEY);
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
    }

    @Override
    public String getPluginID() {
        return TVRAGE_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        ShowInfo showInfo = new ShowInfo();
        List<ShowInfo> showList = null;

        // Note: The ID might be a vanity ID (A String rather than an Integer)
        String id = movie.getId(TVRAGE_PLUGIN_ID);
        int tvrageID = 0;

        try {
            if (isValidString(id)) {
                tvrageID = Integer.parseInt(id);
            }
        } catch (NumberFormatException ignore) {
            // We failed, so set the ID to 0
            tvrageID = 0;
        }

        ThreadExecutor.enterIO(webhost);
        try {
            // Try and search using the ID
            if (tvrageID > 0) {
                LOG.debug(LOG_MESSAGE + "Searching using TVRage ID '" + tvrageID + "'");
                showInfo = tvRage.getShowInfo(tvrageID);
            }

            // Try using the vanity ID
            if (!showInfo.isValid() && (tvrageID == 0 && isValidString(id))) {
                LOG.debug(LOG_MESSAGE + "Searching using Vanity URL '" + id + "'");
                showList = tvRage.searchShow(id);
            }

            // Try using the title
            if ((showList == null || showList.isEmpty()) && (isValidString(movie.getTitle()))) {
                LOG.debug(LOG_MESSAGE + "Searching using title '" + movie.getTitle() + "'");
                showList = tvRage.searchShow(movie.getTitle());
            }

            // If we have some shows, try to find the one that matches our show title
            if (showList != null && !showList.isEmpty()) {
                for (ShowInfo si : showList) {
                    if (movie.getTitle().equalsIgnoreCase(si.getShowName())) {
                        showInfo = si;
                        break;
                    }
                }
            }
        } finally {
            ThreadExecutor.leaveIO();
        }

        // Update the show specific information
        if (!showInfo.isValid() || showInfo.getShowID() == 0) {
            LOG.debug(LOG_MESSAGE + "Show '" + movie.getTitle() + "' not found");
            return false;
        } else {
            id = String.valueOf(showInfo.getShowID());
            movie.setId(TVRAGE_PLUGIN_ID, id);
            showInfo = tvRage.getShowInfo(id);

            if (OverrideTools.checkOverwritePlot(movie, TVRAGE_PLUGIN_ID)) {
                movie.setPlot(showInfo.getSummary(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteOutline(movie, TVRAGE_PLUGIN_ID)) {
                movie.setOutline(showInfo.getSummary(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteGenres(movie, TVRAGE_PLUGIN_ID)) {
                movie.setGenres(showInfo.getGenres(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteYear(movie, TVRAGE_PLUGIN_ID)) {
                movie.setYear(String.valueOf(showInfo.getStarted()), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, TVRAGE_PLUGIN_ID)) {
                CountryDetail cd = showInfo.getNetwork().get(0);
                movie.setCompany(cd.getDetail(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, TVRAGE_PLUGIN_ID)) {
                movie.setCountry(showInfo.getCountry(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, TVRAGE_PLUGIN_ID)) {
                movie.setRuntime(movie.getRuntime(), TVRAGE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, TVRAGE_PLUGIN_ID)) {
                movie.setReleaseDate(DateTimeTools.convertDateToString(showInfo.getStartDate()), TVRAGE_PLUGIN_ID);
            }

            scanTVShowTitles(movie);
        }

        return true;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        String id = movie.getId(TVRAGE_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || id == null) {
            return;
        }

        ShowInfo showInfo;
        EpisodeList episodeList = null;

        try {
            ThreadExecutor.enterIO(webhost);
            showInfo = tvRage.getShowInfo(id);

            if (showInfo != null && showInfo.getShowID() > 0) {
                episodeList = tvRage.getEpisodeList(Integer.toString(showInfo.getShowID()));
            }

        } finally {
            ThreadExecutor.leaveIO();
        }

        if (episodeList == null) {
            LOG.debug(LOG_MESSAGE + "Episodes not found for '" + movie.getTitle() + "'");
            return;
        }

        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {

                    Episode episode = episodeList.getEpisode(movie.getSeason(), part);

                    if (episode == null) {
                        LOG.debug(LOG_MESSAGE + "Episode not found!");
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && isNotValidString(file.getPlot(part))) {
                            file.setTitle(part, "Special", TVRAGE_PLUGIN_ID);
                        }
                    } else {

                        if (OverrideTools.checkOverwriteEpisodeTitle(file, part, TVRAGE_PLUGIN_ID)) {
                            file.setTitle(part, episode.getTitle(), TVRAGE_PLUGIN_ID);
                        }

                        if (OverrideTools.checkOverwriteEpisodePlot(file, part, TVRAGE_PLUGIN_ID)) {
                            file.setPlot(part, episode.getSummary(), TVRAGE_PLUGIN_ID);
                        }

                        if (includeVideoImages && isNotValidString(file.getVideoImageFilename(part))) {
                            String episodeImage = episode.getScreenCap();
                            if (isValidString(episodeImage)) {
                                file.setVideoImageURL(part, episodeImage);
                            }
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // There are two formats for the URL. The first is a vanity URL with the show name in it,
        // http://www.tvrage.com/House
        // the second is an id based URL
        // http://www.tvrage.com/shows/id-22771

        if (StringTools.isValidString(movie.getId(TVRAGE_PLUGIN_ID))) {
            LOG.debug(LOG_MESSAGE + "ID already found for TVRage (" + movie.getId(TVRAGE_PLUGIN_ID) + "), skipping NFO check");
            return Boolean.TRUE;
        }

        int beginIndex;
        String text;

        LOG.debug(LOG_MESSAGE + "Scanning NFO for TVRage Id");

        text = "/shows/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&é\"'(è_çà)=$");
            // Remove the "id-" from the front of the ID
            String id = st.nextToken().substring("id-".length());
            movie.setId(TVRAGE_PLUGIN_ID, id);
            LOG.debug(LOG_MESSAGE + "TVRage Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        text = "tvrage.com/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&\"'=$");
            movie.setId(TVRAGE_PLUGIN_ID, st.nextToken());
            LOG.debug(LOG_MESSAGE + "TVRage Vanity Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        LOG.debug(LOG_MESSAGE + "No TVRage Id found in nfo!");
        return Boolean.FALSE;
    }
}
