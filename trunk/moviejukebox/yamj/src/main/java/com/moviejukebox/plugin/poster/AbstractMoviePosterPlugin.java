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

import org.apache.log4j.Logger;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.PropertiesUtil;

public abstract class AbstractMoviePosterPlugin implements IMoviePosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String searchPriorityMovie = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie","").toLowerCase();
    protected static String searchPriorityTv = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv","").toLowerCase();

    public AbstractMoviePosterPlugin() {
    }
    
    @Override
    public boolean isNeeded() {
        if (searchPriorityMovie.contains(this.getName())) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                logger.debug(this.getName() + " posterPlugin: id found setting it to movie " + id);
                ident.setId(getName(), id);
            }
        } else {
            logger.debug(this.getName() + " posterPlugin: id already in movie using it, skipping id search : " + id);
        }

        if (!(Movie.UNKNOWN.equalsIgnoreCase(id) || "-1".equals(id) || "0".equals(id))) {
            return getPosterUrl(id);
        }
        
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }

}
