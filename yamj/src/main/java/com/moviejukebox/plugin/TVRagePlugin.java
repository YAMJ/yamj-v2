/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.plugin;

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.YamjHttpClientBuilder;
import com.omertron.tvrageapi.TVRageApi;
import com.omertron.tvrageapi.TVRageException;
import com.omertron.tvrageapi.model.CountryDetail;
import com.omertron.tvrageapi.model.Episode;
import com.omertron.tvrageapi.model.EpisodeList;
import com.omertron.tvrageapi.model.ShowInfo;

/**
 * @author Stuart.Boston
 */
public class TVRagePlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(TVRagePlugin.class);
    public static final String TVRAGE_PLUGIN_ID = "tvrage";
    private final TVRageApi tvRage;
    private final boolean includeVideoImages;

    public TVRagePlugin() {
        super();

        String API_KEY = PropertiesUtil.getProperty("API_KEY_TVRage");
        tvRage = new TVRageApi(API_KEY, YamjHttpClientBuilder.getHttpClient());

        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
    }

    @Override
    public String getPluginID() {
        return TVRAGE_PLUGIN_ID;
    }

    private ShowInfo getShowInfo(int tvrageID) {
        try {
            return tvRage.getShowInfo(tvrageID);
        } catch (TVRageException ex) {
            LOG.info("Failed to get TVRage information for '{}' - error: {}", tvrageID, ex.getMessage(), ex);
        }
        return new ShowInfo();
    }

    private List<ShowInfo> getShowList(String query) {
        try {
            tvRage.searchShow(query);
        } catch (TVRageException ex) {
            LOG.info("Failed to get TVRage information for '{}' - error: {}", query, ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean scan(Movie movie) {
        ShowInfo showInfo = new ShowInfo();
        List<ShowInfo> showList = null;

        // Note: The ID might be a vanity ID (A String rather than an Integer)
        String id = movie.getId(TVRAGE_PLUGIN_ID);
        int tvrageID = NumberUtils.toInt(id, 0);

        // Try and search using the ID
        if (tvrageID > 0) {
            LOG.debug("Searching using TVRage ID '{}'", tvrageID);
            showInfo = getShowInfo(tvrageID);
        }

        // Try using the vanity ID
        if (!showInfo.isValid() && (tvrageID == 0 && isValidString(id))) {
            LOG.debug("Searching using Vanity URL '{}'", id);
            showList = getShowList(id);
        }

        // Try using the title
        if ((showList == null || showList.isEmpty()) && (isValidString(movie.getTitle()))) {
            LOG.debug("Searching using title '{}'", movie.getTitle());
            showList = getShowList(movie.getTitle());
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

        // Update the show specific information
        if (!showInfo.isValid() || showInfo.getShowID() == 0) {
            LOG.debug("Show '{}' not found", movie.getBaseName());
            return false;
        }
        
        id = String.valueOf(showInfo.getShowID());
        movie.setId(TVRAGE_PLUGIN_ID, id);

        try {
            showInfo = tvRage.getShowInfo(id);
        } catch (TVRageException ex) {
            LOG.info("Failed to get TVRage information for '{}' - error: {}", movie.getBaseName(), ex.getMessage(), ex);
            return false;
        }

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
            movie.setCountries(showInfo.getCountry(), TVRAGE_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteRuntime(movie, TVRAGE_PLUGIN_ID)) {
            movie.setRuntime(movie.getRuntime(), TVRAGE_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteReleaseDate(movie, TVRAGE_PLUGIN_ID)) {
            movie.setReleaseDate(DateTimeTools.convertDateToString(showInfo.getStartDate()), TVRAGE_PLUGIN_ID);
        }

        scanTVShowTitles(movie);

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
            showInfo = tvRage.getShowInfo(id);

            if (showInfo != null && showInfo.getShowID() > 0) {
                episodeList = tvRage.getEpisodeList(Integer.toString(showInfo.getShowID()));
            }
        } catch (TVRageException ex) {
            LOG.info("Failed to get TVRage information for '{}' - error: {}", movie.getBaseName(), ex.getMessage(), ex);
        }

        if (episodeList == null) {
            LOG.debug("Episodes not found for '{}'", movie.getTitle());
            return;
        }

        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {

                    Episode episode = episodeList.getEpisode(movie.getSeason(), part);

                    if (episode == null) {
                        LOG.debug("Episode not found!");
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
            LOG.debug("ID already found for TVRage ({}), skipping NFO check", movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        int beginIndex;
        String text;

        LOG.debug("Scanning NFO for TVRage Id");

        text = "/shows/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&é\"'(è_çà)=$");
            // Remove the "id-" from the front of the ID
            String id = st.nextToken().substring("id-".length());
            movie.setId(TVRAGE_PLUGIN_ID, id);
            LOG.debug("TVRage Id found in nfo = {}", movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        text = "tvrage.com/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&\"'=$");
            movie.setId(TVRAGE_PLUGIN_ID, st.nextToken());
            LOG.debug("TVRage Vanity Id found in nfo = {}", movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        LOG.debug("No TVRage Id found in nfo!");
        return Boolean.FALSE;
    }
}
