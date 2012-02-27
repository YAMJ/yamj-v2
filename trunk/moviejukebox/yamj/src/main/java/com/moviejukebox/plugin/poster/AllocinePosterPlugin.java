/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import java.text.ParseException;

import com.moviejukebox.allocine.*;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.Cache;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import org.apache.log4j.Logger;

public class AllocinePosterPlugin extends AbstractMoviePosterPlugin {

    private AllocinePlugin allocinePlugin;
    private static Logger logger = Logger.getLogger(AllocinePosterPlugin.class);
    
    public AllocinePosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        allocinePlugin = new AllocinePlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            response = allocinePlugin.getAllocineId(title, year, -1);
        } catch (ParseException error) {
            logger.error("AllocinePosterPlugin: Failed retreiving poster id movie : " + title);
            logger.error(SystemTools.getStackTrace(error));
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            try {
                String cacheKey = Cache.generateCacheKey(AllocinePlugin.CACHE_MOVIE, id);
                MovieInfos movieInfos = (MovieInfos) Cache.getFromCache(cacheKey);
                if (movieInfos == null) {
                    movieInfos = XMLAllocineAPIHelper.getMovieInfos(id);
                    // Add to the cache
                    Cache.addToCache(cacheKey, movieInfos);
                }

                if (movieInfos.isNotValid()) {
                    logger.error("AllocinePlugin: Can't find informations for movie with id: " + id);
                    return Image.UNKNOWN;
                }

                if (movieInfos.getPosterUrls().size() > 0) {
                    posterURL = movieInfos.getPosterUrls().iterator().next();
                    if (StringTools.isValidString(posterURL)) {
                        logger.debug("AllocinePlugin: Movie PosterURL from Allocine: " + posterURL);
                    }
                }

            } catch (Exception error) {
                logger.error("AllocinePlugin: Failed retreiving poster for movie : " + id);
                logger.error(SystemTools.getStackTrace(error));
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "allocine";
    }
}
