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

import java.util.logging.Logger;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.SqlTools;

public class MovieListingPluginSql extends MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    public void generate(Jukebox jukebox, Library library) {
        
        SqlTools.openDatabase("listing.db");
        for (Movie movie : library.values()) {
            logger.fine("MovieListingPluginSql: Writing movie to SQL - " + movie.getTitle());
            SqlTools.insertIntoVideo(movie);
        }
        
        SqlTools.closeDatabase();
    }

}
