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

package com.moviejukebox.plugin.poster;

import java.util.logging.Logger;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;

public abstract class AbstractMoviePosterPlugin implements IMoviePosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                logger.finest(this.getName() + " posterPlugin: id found setting it to movie " + id);
                ident.setId(getName(), id);
            }
        }else{
            logger.finest(this.getName() + " posterPlugin: id already in movie using it, skipping id search : " + id);
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
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
