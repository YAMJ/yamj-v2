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

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tvrage.TVRage;
import com.moviejukebox.tvrage.model.Episode;
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

        String id = movie.getId(TVRAGE_PLUGIN_ID);

        if (!FileTools.isValidString(id)) {
            ThreadExecutor.enterIO(webhost);
            try{
                if (!movie.getTitle().equals(TVRage.UNKNOWN)) {
                    showList = tvRage.searchShow(movie.getTitle());
                }
                
                if (showList == null || showList.isEmpty()) {
                    showList = tvRage.searchShow(movie.getBaseName());
                }
                
                if (showList != null && !showList.isEmpty()) {
                    showInfo = tvRage.getShowInfo(showInfo.getShowID());
                }
            } finally {
                ThreadExecutor.leaveIO();
            }
            
            if (showInfo == null || showInfo.getShowID() == 0) {
                logger.finer("TVRage Plugin: Show '" + movie.getTitle() + "' not found");
                return false;
            } else {
                movie.setId(TVRAGE_PLUGIN_ID, id);
                movie.setPlot(showInfo.getSummary());
                movie.setGenres(showInfo.getGenres());
                scanTVShowTitles(movie);
            }
            
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
        List<Episode> episodeList = null;
        
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
                    Episode episode = showInfo.getEpisode(movie.getSeason(), part);

                    if (episode != null) {
                        // Set the title of the episode
                        if (file.getTitle(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            file.setTitle(part, episode.getTitle());
                        }

                        if (includeEpisodePlots) {
                            if (file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                String episodePlot = episode.getSummary();
                                if (episodePlot != null) {
                                    if (episodePlot.length() > preferredPlotLength) {
                                        episodePlot = episodePlot.substring(0, Math.min(episodePlot.length(), preferredPlotLength - 3)) + "...";
                                    }
                                    file.setPlot(part, episodePlot);
                                }
                            }
                        }

                        if (includeVideoImages) {
                            // This plugin doesn't support videoimages
                            // FIXME We need to set this to a value other than unknown so the recheck function doesn't keep overwriting it
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    } else {
                        // This occurs if the episode is not found
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            // This sets the zero part's title to be either the filename title or blank rather than the next episode's title
                            file.setTitle(part, "Special");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie);

        // There are two formats for the URL. The first is a vanity URL with the show name in it,
        // the second is an id based URL
        
        logger.finest("Scanning NFO for TVRage Id");
        logger.finest("Not supported Yet");
        
    }
    
}
