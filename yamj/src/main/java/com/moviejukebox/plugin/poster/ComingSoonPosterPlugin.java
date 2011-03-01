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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ComingSoonPlugin;
import com.moviejukebox.tools.StringTools;

public class ComingSoonPosterPlugin extends AbstractMoviePosterPlugin {
    
    private static final String POSTER_BASE_URL = "http://www.comingsoon.it/imgdb/locandine/big/";
    
    public ComingSoonPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }        
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return Movie.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String id) {
        if (StringTools.isNotValidString(id) || id.equals(ComingSoonPlugin.COMINGSOON_NOT_PRESENT)) {
            return Image.UNKNOWN;
        }
        String posterURL;
        posterURL = POSTER_BASE_URL + id + ".jpg";
        return new Image(posterURL);
       
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "comingsoon";
    }
}
