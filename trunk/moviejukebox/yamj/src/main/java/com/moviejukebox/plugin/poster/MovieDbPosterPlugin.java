/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.*;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
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
    private static final Boolean INCLUDE_ADULT = PropertiesUtil.getBooleanProperty("themoviedb.includeAdult", FALSE);

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
    public String getIdFromMovieInfo(String title, String searchYear) {
        List<MovieDb> movieList;
        try {
            int movieYear = 0;
            if (StringTools.isValidString(searchYear) && StringUtils.isNumeric(searchYear)) {
                movieYear = Integer.parseInt(searchYear);
            }

            movieList = TMDb.searchMovie(title, movieYear, languageCode, INCLUDE_ADULT, 0);
        } catch (MovieDbException ex) {
            logger.warn(LOG_MESSAGE + "Failed to get TMDB ID for " + title + "(" + searchYear + ")");
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
                if (TheMovieDbApi.compareMovies(moviedb, title, searchYear, TheMovieDbPlugin.SEARCH_MATCH)) {
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
