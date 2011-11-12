/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.model;

import com.moviejukebox.tools.StringTools;

/**
 * Class to hold the episode information scraped from the XBMC style TV Episode NFO file.
 * 
 * @author Stuart Boston
 *
 */
public class EpisodeDetail {
    String title = Movie.UNKNOWN;
    int season = -1;
    int episode = -1;
    String plot = Movie.UNKNOWN;
    String firstAired = Movie.UNKNOWN;
    String airsAfterSeason = Movie.UNKNOWN;
    String airsBeforeSeason = Movie.UNKNOWN;
    String airsBeforeEpisode = Movie.UNKNOWN;
    String rating = Movie.UNKNOWN;
    
    /**
     * Set the title of the episode
     * @param title
     */
    public void setTitle(String title) {
        if(title == null || title.isEmpty()) {
            this.title = Movie.UNKNOWN;
        } else {
            this.title = title;
        }
    }
    
    /**
     * return the title of the episode
     * @return
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the season of the episode
     * @param season
     */
    public void setSeason(int season) {
        if(season < -1) {
            this.season = -1;
        } else {
            this.season = season;
        }
    }
    
    /**
     * Return the season of the episode
     * @return
     */
    public int getSeason() {
        return season;
    }
    
    /**
     * Set the Episode number
     * @param episode
     */
    public void setEpisode(int episode) {
        if (episode < 0) {
            this.episode = 0;
        } else {
            this.episode = episode;
        }
    }
    
    /**
     * return the episode number
     * @return
     */
    public int getEpisode() {
        return episode;
    }
    
    /**
     * Set the plot
     * @param plot
     */
    public void setPlot(String plot) {
        if (plot == null || plot.isEmpty()) {
            this.plot = Movie.UNKNOWN;
        } else {
            this.plot = plot;
        }
    }
    
    /**
     * Return the plot
     * @return
     */
    public String getPlot() {
        return plot;
    }
    
    /**
     * Set the rating
     * @param rating
     */
    public void setRating(String rating) {
        if (rating == null || rating.isEmpty()) {
            this.rating = Movie.UNKNOWN;
        } else {
            this.rating = rating;
        }
    }
    
    /**
     * Return the rating
     * @return
     */
    public String getRating() {
        return rating;
    }
    
    /**
     * Update the movie object with the episode details
     * @param movie
     */
    public void updateMovie(Movie movie) {
        if (episode < 0) {
            return;
        }

        for (MovieFile mf : movie.getMovieFiles()) {
            if (episode >= mf.getFirstPart() && episode <= mf.getLastPart()) {
                mf.setSeason(season);
                
                if (StringTools.isValidString(title)) {
                    mf.setTitle(episode, title);
                }
                
                if (StringTools.isValidString(plot)) {
                    mf.setPlot(episode, plot);
                }
                
                if (StringTools.isValidString(rating)) {
                    mf.setRating(episode, rating);
                }
                
                if (StringTools.isValidString(firstAired)) {
                    mf.setFirstAired(episode, firstAired);
                }
                
                if (StringTools.isValidString(airsAfterSeason)) {
                    mf.setAirsAfterSeason(episode, airsAfterSeason);
                }
                
                if (StringTools.isValidString(airsBeforeSeason)) {
                    mf.setAirsBeforeSeason(episode, airsBeforeSeason);
                }
                
                if (StringTools.isValidString(airsBeforeEpisode)) {
                    mf.setAirsBeforeEpisode(episode, airsBeforeEpisode);
                }
                
            }
        }
    }

    public String getFirstAired() {
        return firstAired;
    }

    public String getAirsAfterSeason() {
        return airsAfterSeason;
    }

    public String getAirsBeforeSeason() {
        return airsBeforeSeason;
    }

    public String getAirsBeforeEpisode() {
        return airsBeforeEpisode;
    }

    public void setFirstAired(String firstAired) {
        this.firstAired = firstAired;
    }

    public void setAirsAfterSeason(String airsAfterSeason) {
        this.airsAfterSeason = airsAfterSeason;
    }

    public void setAirsBeforeSeason(String airsBeforeSeason) {
        this.airsBeforeSeason = airsBeforeSeason;
    }

    public void setAirsBeforeEpisode(String airsBeforeEpisode) {
        this.airsBeforeEpisode = airsBeforeEpisode;
    }

}