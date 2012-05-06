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

import com.moviejukebox.allocine.MovieInfos;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.text.ParseException;
import org.apache.log4j.Logger;

public class AllocinePosterPlugin extends AbstractMoviePosterPlugin {

    private AllocinePlugin allocinePlugin;
    private static final Logger logger = Logger.getLogger(AllocinePosterPlugin.class);

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
            logger.error("AllocinePosterPlugin: Failed to get Allocine ID for " + title + "(" + year + ")");
            logger.error(SystemTools.getStackTrace(error));
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {

            MovieInfos movieInfos = allocinePlugin.getMovieInfos(id);

            if (movieInfos.isValid() && movieInfos.getPosterUrls().size() > 0 ) {
                String posterURL = movieInfos.getPosterUrls().iterator().next();
                if (StringTools.isValidString(posterURL)) {
                    return new Image(posterURL);
                }
            }
        }
        logger.debug("AllocinePosterPlugin: No poster found at allocine for movie id " + id);
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
