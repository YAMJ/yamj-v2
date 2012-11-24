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
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import java.net.URL;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class MovieDbPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger logger = Logger.getLogger(MovieDbPosterPlugin.class);
    private static final String LOG_MESSAGE = "MovieDbPosterPlugin: ";
    private String apiKey = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
    private String languageCode;
    private TheMovieDbApi TMDb;
    private static final String DEFAULT_POSTER_SIZE = "original";

    public MovieDbPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        languageCode = PropertiesUtil.getProperty("themoviedb.language", "en");

        if (languageCode.length() > 2) {
            languageCode = new String(languageCode.substring(0, 2)).toLowerCase();
        }
        logger.debug(LOG_MESSAGE + "Using `" + languageCode + "` as the language code");

        try {
            TMDb = new TheMovieDbApi(apiKey);
        } catch (MovieDbException ex) {
            logger.warn(LOG_MESSAGE + "Failed to initialise TheMovieDB API.");
            return;
        }

        // Set the proxy
        TMDb.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeouts
        TMDb.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        List<MovieDb> movieList;
        try {
            movieList = TMDb.searchMovie(title, languageCode, false);
        } catch (MovieDbException ex) {
            logger.warn(LOG_MESSAGE + "Failed to get TMDB ID for " + title + "(" + year + ")");
            return Movie.UNKNOWN;
        }

        if (movieList.isEmpty()) {
            return Movie.UNKNOWN;
        } else {
            if (movieList.size() == 1) {
                // Only one movie so return that id
                return String.valueOf(movieList.get(0).getId());
            }

            for (MovieDb moviedb : movieList) {
                if (TheMovieDbApi.compareMovies(moviedb, title, year)) {
                    return String.valueOf(moviedb.getId());
                }
            }
        }
        return Movie.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        URL posterURL;

        if (StringUtils.isNumeric(id)) {
            try {
                MovieDb moviedb = TMDb.getMovieInfo(Integer.parseInt(id), languageCode);
                logger.debug(LOG_MESSAGE + "Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/" + id);
                posterURL = TMDb.createImageUrl(moviedb.getPosterPath(), DEFAULT_POSTER_SIZE);
                return new Image(posterURL.toString());
            } catch (MovieDbException ex) {
                logger.warn(LOG_MESSAGE + "Failed to get the poster URL for TMDB ID " + id + " " + ex.getMessage());
                return Image.UNKNOWN;
            }
        } else {
            return Image.UNKNOWN;
        }
    }

    @Override
    public String getName() {
        return "themoviedb";
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);

        if (StringTools.isNotValidString(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // Id found
            if (StringTools.isValidString(id)) {
                ident.setId(getName(), id);
            }
        }

        if (StringTools.isValidString(id)) {
            return getPosterUrl(id);
        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;

        if (ident != null) {
            String imdbID = ident.getId(TheMovieDbPlugin.IMDB_PLUGIN_ID);
            String tmdbID = ident.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID);

            // First look to see if we have a TMDb ID as this will make looking the film up easier
            if (StringTools.isValidString(tmdbID)) {
                response = tmdbID;
            } else if (StringTools.isValidString(imdbID)) {
                // Search based on IMDb ID
                MovieDb moviedb;
                try {
                    moviedb = TMDb.getMovieInfoImdb(imdbID, languageCode);
                } catch (MovieDbException ex) {
                    logger.warn(LOG_MESSAGE + "Failed to get TMDB ID for " + imdbID + " - " + ex.getMessage());
                    return response;
                }
                if (moviedb != null) {
                    tmdbID = String.valueOf(moviedb.getId());
                    if (StringUtils.isNumeric(tmdbID)) {
                        response = tmdbID;
                    } else {
                        logger.info(LOG_MESSAGE + "No TMDb ID found for movie!");
                    }
                }
            }
        }
        return response;
    }
}
