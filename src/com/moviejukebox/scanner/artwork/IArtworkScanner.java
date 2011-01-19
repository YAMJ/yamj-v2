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

package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieImagePlugin;

public interface IArtworkScanner {
    public String scanLocalArtwork(Jukebox jukebox, Movie movie);
    
    public String scanOnlineArtwork(Movie movie);

    public boolean validateArtwork(IImage artworkImage);
    
    public boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect);
    
    public boolean downloadArtwork(Jukebox jukebox, Movie movie, boolean artworkOverwrite, MovieImagePlugin imagePlugin);

    /**
     * Updates the correct Filename based on the artwork type
     * @param movie
     * @param artworkFilename
     */
    abstract void setArtworkFilename(Movie movie, String artworkFilename);
    
    /**
     * Returns the correct Filename based on the artwork type
     * This should be overridden at the artwork specific class level
     * @param movie
     * @return
     */
    abstract String getArtworkFilename(Movie movie);
    
    /**
     * Updates the correct URL based on the artwork type
     * @param movie
     * @param artworkUrl
     */
    abstract void setArtworkUrl(Movie movie, String artworkUrl);
    
    /**
     * Returns the correct URL based on the artwork type
     * @param movie
     * @return
     */
    abstract String getArtworkUrl(Movie movie);
    
    /**
     * Sets the correct image plugin for the artwork type
     */
    abstract void setArtworkImagePlugin();
    
    /**
     * Return the value of the appropriate "dirty" setting for the artwork and movie
     * @param movie
     * @return
     */
    abstract boolean isDirtyArtwork(Movie movie);
    
    /**
     * Set the appropriate "dirty" setting for the artwork and movie
     * @param movie
     * @param dirty
     */
    abstract void setDirtyArtwork(Movie movie, boolean dirty);

}
