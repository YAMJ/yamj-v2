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
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
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

    private static final Logger logger = Logger.getLogger(TVRagePlugin.class);
    private static final String LOG_MESSAGE = "TVRagePlugin: ";
    public static final String TVRAGE_PLUGIN_ID = "tvrage";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TVRage");
    private static final String webhost = "tvrage.com";
    private TVRageApi tvRage;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private int preferredPlotLength;
    private int preferredOutlineLength;

    public TVRagePlugin() {
        super();
        tvRage = new TVRageApi(API_KEY);
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", FALSE);
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", FALSE);
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");
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
        } catch (Exception ignore) {
            // We failed, so set the ID to 0
            tvrageID = 0;
        }

        ThreadExecutor.enterIO(webhost);
        try {
            // Try and search using the ID
            if (tvrageID > 0) {
                logger.debug(LOG_MESSAGE + "Searching using TVRage ID '" + tvrageID + "'");
                showInfo = tvRage.getShowInfo(tvrageID);
            }

            // Try using the vanity ID
            if (!showInfo.isValid() && (tvrageID == 0 && isValidString(id))) {
                logger.debug(LOG_MESSAGE + "Searching using Vanity URL '" + id + "'");
                showList = tvRage.searchShow(id);
            }

            // Try using the title
            if ((showList == null || showList.isEmpty()) && (isValidString(movie.getTitle()))) {
                logger.debug(LOG_MESSAGE + "Searching using title '" + movie.getTitle() + "'");
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
            logger.debug(LOG_MESSAGE + "Show '" + movie.getTitle() + "' not found");
            return false;
        } else {
            id = String.valueOf(showInfo.getShowID());
            movie.setId(TVRAGE_PLUGIN_ID, id);
            showInfo = tvRage.getShowInfo(id);

            // Update the plot & outline
            if (isNotValidString(movie.getPlot())) {
                String plot = StringTools.trimToLength(showInfo.getSummary(), preferredPlotLength);

                if (isValidString(plot)) {
                    movie.setPlot(plot);

                    plot = StringTools.trimToLength(showInfo.getSummary(), preferredOutlineLength);
                    movie.setOutline(plot);
                } else {
                    movie.setPlot(Movie.UNKNOWN);
                    movie.setOutline(Movie.UNKNOWN);
                }
            }

            // Update the Genres
            if (movie.getGenres().isEmpty()) {
                for (String genre : showInfo.getGenres()) {
                    movie.addGenre(genre);
                }
            }

            if (isNotValidString(movie.getYear())) {
                movie.setYear(String.valueOf(showInfo.getStarted()));
            }

            if (isNotValidString(movie.getCompany())) {
                CountryDetail cd = showInfo.getNetwork().get(0);
                movie.setCountry(cd.getDetail());
            }

            if (isNotValidString(movie.getCountry())) {
                movie.setCountry(showInfo.getCountry());
            }

            if (isNotValidString(movie.getRuntime())) {
                movie.setRuntime(String.valueOf(showInfo.getRuntime()));
            }

            if (isNotValidString(movie.getReleaseDate())) {
                movie.setReleaseDate(DateTimeTools.convertDateToString(showInfo.getStartDate()));
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
            logger.debug(LOG_MESSAGE + "Episodes not found for '" + movie.getTitle() + "'");
            return;
        }

        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {

                    Episode episode = episodeList.getEpisode(movie.getSeason(), part);

                    if (episode == null) {
                        logger.debug(LOG_MESSAGE + "Episode not found!");
                        // This occurs if the episode is not found
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && isNotValidString(file.getPlot(part))) {
                            // This sets the zero part's title to be either the filename title or blank rather than the next episode's title
                            file.setTitle(part, "Special");
                        }
                    } else {
                        // Set the title of the episode
                        if (isNotValidString(file.getTitle(part))) {
                            file.setTitle(part, episode.getTitle());
                        }

                        if (includeEpisodePlots && isNotValidString(file.getPlot(part))) {
                            String episodePlot = episode.getSummary();
                            if (isValidString(episodePlot)) {
                                episodePlot = trimToLength(episodePlot, preferredPlotLength, true, plotEnding);
                                file.setPlot(part, episodePlot);
                                }
                        }

                        if (includeVideoImages) {
                            if (isNotValidString(file.getVideoImageFilename(part))) {
                                String episodeImage = episode.getScreenCap();
                                if (isValidString(episodeImage)) {
                                    file.setVideoImageURL(part, episodeImage);
                                }
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
            logger.debug(LOG_MESSAGE + "ID already found for TVRage (" + movie.getId(TVRAGE_PLUGIN_ID) + "), skipping NFO check");
            return Boolean.TRUE;
        }

        int beginIndex;
        String text;

        logger.debug(LOG_MESSAGE + "Scanning NFO for TVRage Id");

        text = "/shows/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + text.length())), "/ \n,:!&é\"'(è_çà)=$");
            // Remove the "id-" from the front of the ID
            String id = new String(st.nextToken().substring("id-".length()));
            movie.setId(TVRAGE_PLUGIN_ID, id);
            logger.debug(LOG_MESSAGE + "TVRage Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        text = "tvrage.com/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + text.length())), "/ \n,:!&\"'=$");
            movie.setId(TVRAGE_PLUGIN_ID, st.nextToken());
            logger.debug(LOG_MESSAGE + "TVRage Vanity Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return Boolean.TRUE;
        }

        logger.debug(LOG_MESSAGE + "No TVRage Id found in nfo!");
        return Boolean.FALSE;
    }
}
