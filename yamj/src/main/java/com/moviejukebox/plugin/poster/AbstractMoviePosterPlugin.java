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

import com.moviejukebox.model.*;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import org.apache.log4j.Logger;

public abstract class AbstractMoviePosterPlugin implements IMoviePosterPlugin {

    private static final Logger logger = Logger.getLogger(AbstractMoviePosterPlugin.class);
    protected static final String SEARCH_PRIORITY_MOVIE = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie", "").toLowerCase();
    protected static final String SEARCH_PRIORITY_TV = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "").toLowerCase();

    public AbstractMoviePosterPlugin() {
    }

    @Override
    public boolean isNeeded() {
        if (SEARCH_PRIORITY_MOVIE.contains(this.getName())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (StringTools.isNotValidString(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // ID found
            if (StringTools.isValidString(id)) {
                logger.debug(this.getName() + " posterPlugin: ID found setting it to '" + id + "'");
                ident.setId(getName(), id);
            } else if (!movieInformation.getOriginalTitle().equalsIgnoreCase(movieInformation.getTitle())) {
                // Didn't find the movie with the original title, try the normal title if it's different
                id = getIdFromMovieInfo(movieInformation.getTitle(), movieInformation.getYear());
                // ID found
                if (StringTools.isValidString(id)) {
                    logger.debug(this.getName() + " posterPlugin: ID found setting it to '" + id + "'");
                    ident.setId(getName(), id);
                }
            }
        } else {
            logger.debug(this.getName() + " posterPlugin: ID already in movie using it, skipping ID search: '" + id + "'");
        }

        if (!(StringTools.isNotValidString(id) || "-1".equals(id) || "0".equals(id))) {
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
