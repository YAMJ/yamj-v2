/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import static com.moviejukebox.tools.PropertiesUtil.getProperty;

import java.util.List;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tvrage.TVRage;
import com.moviejukebox.tvrage.model.CountryDetail;
import com.moviejukebox.tvrage.model.Episode;
import com.moviejukebox.tvrage.model.EpisodeList;
import com.moviejukebox.tvrage.model.ShowInfo;

/**
 * @author Stuart.Boston
 */
public class TVRagePlugin extends ImdbPlugin {

    public static final String TVRAGE_PLUGIN_ID = "tvrage";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TVRage");
    private static final String webhost = "tvrage.com";
    private TVRage tvRage;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private int preferredPlotLength;

    public TVRagePlugin() {
        super();
        tvRage = new TVRage(API_KEY);
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
        includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.tv.download", "false"));
        Boolean.parseBoolean(getProperty("mjb.forceFanartOverwrite", "false"));
        Boolean.parseBoolean(getProperty("mjb.forceBannersOverwrite", "false"));
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
    }

    @Override
    public boolean scan(Movie movie) {
        ShowInfo showInfo = new ShowInfo();
        List<ShowInfo> showList = null;

        // Note: The ID might be a vanity ID (A String rather than an Integer)
        String id = movie.getId(TVRAGE_PLUGIN_ID);
        int tvrageID = 0;
        
        try {
            if (FileTools.isValidString(id)) {
                tvrageID = Integer.parseInt(id); 
            }
        } catch (Exception ignore) {
            // We failed, so set the ID to 0
            tvrageID = 0;
        }

        ThreadExecutor.enterIO(webhost);
        try{
            // Try and search using the ID
            if (tvrageID > 0) {
                logger.finest("TVRagePlugin: Searching using TVRage ID '" + tvrageID + "'");
                showInfo = tvRage.getShowInfo(tvrageID);
            }
            
            // Try using the vanity ID
            if (!showInfo.isValid() && (tvrageID == 0 && FileTools.isValidString(id))) {
                logger.finest("TVRagePlugin: Searching using Vanity URL '" + id + "'");
                showList = tvRage.searchShow(id);
            }

            // Try using the title
            if ((showList == null || showList.isEmpty()) && (FileTools.isValidString(movie.getTitle()))) {
                logger.finest("TVRagePlugin: Searching using title '" + movie.getTitle() + "'");
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
        if (showInfo == null || showInfo.getShowID() == 0) {
            logger.finer("TVRage Plugin: Show '" + movie.getTitle() + "' not found");
            return false;
        } else {
            id = "" + showInfo.getShowID();
            movie.setId(TVRAGE_PLUGIN_ID, id);
            showInfo = tvRage.getShowInfo(id);

            // Update the plot & outline
            if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setPlot(showInfo.getSummary());
                movie.setOutline(showInfo.getSummary());
            }
            
            // Update the Genres
            if (movie.getGenres().isEmpty()) {
                for (String genre : showInfo.getGenres()) {
                    movie.addGenre(genre);
                }
            }
            
            if (movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setYear("" + showInfo.getStarted());
            }
            
            if (movie.getCompany().equalsIgnoreCase(Movie.UNKNOWN)) {
                CountryDetail cd = showInfo.getNetwork().get(0);
                movie.setCountry(cd.getDetail());
            }
            
            if (movie.getCountry().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setCountry(showInfo.getCountry());
            }
            
            if (movie.getRuntime().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setRuntime("" + showInfo.getRuntime());
            }
            
            if (movie.getReleaseDate().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setReleaseDate(showInfo.getStartDate());
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

        ShowInfo showInfo = null;
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
            logger.finer("TVRage Plugin: Episodes not found for '" + movie.getTitle() + "'");
            return;
        }
        
        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    
                    Episode episode = episodeList.getEpisode(movie.getSeason(), part); 

                    if (episode == null) {
                        logger.finer("Episode not found!");
                        // This occurs if the episode is not found
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            // This sets the zero part's title to be either the filename title or blank rather than the next episode's title
                            file.setTitle(part, "Special");
                        }
                    } else {
                        // Set the title of the episode
                        if (file.getTitle(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            file.setTitle(part, episode.getTitle());
                        }

                        if (includeEpisodePlots) {
                            if (file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                String episodePlot = episode.getSummary();
                                if (FileTools.isValidString(episodePlot)) {
                                    if (episodePlot.length() > preferredPlotLength) {
                                        episodePlot = episodePlot.substring(0, Math.min(episodePlot.length(), preferredPlotLength - 3)) + "...";
                                    }
                                    file.setPlot(part, episodePlot);
                                }
                            }
                        }

                        if (includeVideoImages) {
                            if (file.getVideoImageFilename(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                String episodeImage = episode.getScreenCap();
                                if (FileTools.isValidString(episodeImage)) {
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
    public void scanNFO(String nfo, Movie movie) {
        // There are two formats for the URL. The first is a vanity URL with the show name in it,
        // http://www.tvrage.com/House
        // the second is an id based URL
        // http://www.tvrage.com/shows/id-22771
        
        int beginIndex;
        String text;
        
        logger.finest("Scanning NFO for TVRage Id");

        text = "/shows/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&é\"'(è_çà)=$");
            // Remove the "id-" from the front of the ID
            String id = st.nextToken().substring("id-".length());
            movie.setId(TVRAGE_PLUGIN_ID, id);
            logger.finer("TVRage Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return;
        }
        
        text = "tvrage.com/";
        beginIndex = nfo.indexOf(text);
        if (beginIndex > -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + text.length()), "/ \n,:!&\"'=$");
            movie.setId(TVRAGE_PLUGIN_ID, st.nextToken());
            logger.finer("TVRage Vanity Id found in nfo = " + movie.getId(TVRAGE_PLUGIN_ID));
            return;
        }
        
        logger.finer("No TVRage Id found in nfo!");
    }

}
