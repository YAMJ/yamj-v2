/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.results.TmdbResultsList;
import java.net.URL;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieDbPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieDbPosterPlugin.class);
    private String apiKey = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
    private String languageCode;
    private TheMovieDbApi tmdb;
    private static final String DEFAULT_POSTER_SIZE = "original";
    private static final Boolean INCLUDE_ADULT = PropertiesUtil.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);

    public MovieDbPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        languageCode = PropertiesUtil.getProperty("themoviedb.language", "en");

        if (languageCode.length() > 2) {
            languageCode = languageCode.substring(0, 2).toLowerCase();
        }
        LOG.debug("Using '{}' as the language code", languageCode);

        try {
            tmdb = new TheMovieDbApi(apiKey, WebBrowser.getHttpClient());
        } catch (MovieDbException ex) {
            LOG.warn("Failed to initialise TheMovieDB API.");
            LOG.warn(SystemTools.getStackTrace(ex));
        }
    }

    @Override
    public String getIdFromMovieInfo(String title, String searchYear) {
        List<MovieDb> movieList;
        try {
            int movieYear = 0;
            if (StringTools.isValidString(searchYear) && StringUtils.isNumeric(searchYear)) {
                movieYear = Integer.parseInt(searchYear);
            }

            TmdbResultsList<MovieDb> result = tmdb.searchMovie(title, movieYear, languageCode, INCLUDE_ADULT, 0);
            movieList = result.getResults();
        } catch (MovieDbException ex) {
            LOG.warn("Failed to get TMDB ID for {} ({}) - {}", title, searchYear, ex.getMessage());
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
                MovieDb moviedb = tmdb.getMovieInfo(Integer.parseInt(id), languageCode);
                LOG.debug("Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/{}", id);
                posterURL = tmdb.createImageUrl(moviedb.getPosterPath(), DEFAULT_POSTER_SIZE);
                return new Image(posterURL.toString());
            } catch (MovieDbException ex) {
                LOG.warn("Failed to get the poster URL for TMDB ID {} {}", id, ex.getMessage());
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
                    moviedb = tmdb.getMovieInfoImdb(imdbID, languageCode);
                } catch (MovieDbException ex) {
                    LOG.warn("Failed to get TMDB ID for {} - {}", imdbID, ex.getMessage());
                    return response;
                }
                if (moviedb != null) {
                    tmdbID = String.valueOf(moviedb.getId());
                    if (StringUtils.isNumeric(tmdbID)) {
                        response = tmdbID;
                    } else {
                        LOG.info("No TMDb ID found for movie!");
                    }
                }
            }
        }
        return response;
    }
}
