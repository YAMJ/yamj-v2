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
     * Update the movie object with the episode details
     * @param movie
     */
    public void updateMovie(Movie movie) {
        if ((movie.getSeason() != season) || (episode < 0)) {
            return;
        }

        for (MovieFile mf : movie.getMovieFiles()) {
            if (episode >= mf.getFirstPart() && episode <= mf.getLastPart()) {
                if (title != Movie.UNKNOWN) {
                    mf.setTitle(episode, title);
                }
                
                if (plot != Movie.UNKNOWN) {
                    mf.setPlot(episode, plot);
                }
            }
        }
    }

}