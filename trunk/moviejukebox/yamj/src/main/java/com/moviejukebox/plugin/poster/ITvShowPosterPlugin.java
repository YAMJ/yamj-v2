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

public interface ITvShowPosterPlugin extends IPosterPlugin {
    
    public String getIdFromMovieInfo(String title, String year, int tvSeason);

    public IImage getPosterUrl(String title, String year, int tvSeason);

    public IImage getPosterUrl(String id, int season);

}
