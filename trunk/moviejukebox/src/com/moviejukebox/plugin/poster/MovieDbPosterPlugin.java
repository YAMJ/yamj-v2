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
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.Artwork;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.tools.PropertiesUtil;

public class MovieDbPosterPlugin implements IMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private String API_KEY;
    private String language;
    private int posterPosition;
    private TheMovieDb theMovieDb;

    public MovieDbPosterPlugin() {
        super();
        API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
        language = PropertiesUtil.getProperty("themoviedb.language", "en");
        posterPosition = Integer.parseInt(PropertiesUtil.getProperty("themoviedb.posterPosition", "1"));
        theMovieDb = new TheMovieDb(API_KEY);
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        theMovieDb = new TheMovieDb(API_KEY);
        MovieDB moviedb = theMovieDb.moviedbSearch(title, language);
        if (moviedb != null) {
            String response = moviedb.getId();
            return response;
        } else {
            return Movie.UNKNOWN;
        }
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getPosterUrl(String id) {
        String returnString = Movie.UNKNOWN;
        if (id.equalsIgnoreCase(Movie.UNKNOWN)) {
            return returnString;
        }

        MovieDB moviedb = theMovieDb.moviedbGetImages(id, language);

        try {
            if (moviedb != null) {
                Artwork artwork = moviedb.getArtwork(Artwork.ARTWORK_TYPE_POSTER, Artwork.ARTWORK_SIZE_ORIGINAL, posterPosition);
                if (!(artwork == null || artwork.getUrl() == null || artwork.getUrl().equals(MovieDB.UNKNOWN))) {
                    logger.finest("MovieDbPosterPlugin : Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/" + id);
                    returnString = artwork.getUrl();
                }
            } else {
                logger.finer("MovieDbPosterPlugin: Unable to find posters for " + id);
            }
        } catch (Exception error) {
            logger.severe("MovieDbPosterPlugin: TheMovieDB.org API Error: " + error.getMessage());
        }
        return returnString;
    }

    @Override
    public String getName() {
        return "moviedb";
    }

    @Override
    public String getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                ident.setId(getName(), id);
            }
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            return getPosterUrl(id);
        }
        return Movie.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String imdbID = ident.getId(TheMovieDbPlugin.IMDB_PLUGIN_ID);
            String tmdbID = ident.getId(TheMovieDbPlugin.IMDB_PLUGIN_ID);
            MovieDB moviedb;
            // First look to see if we have a TMDb ID as this will make looking the film up easier
            if (tmdbID != null && !tmdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
                response = tmdbID;
            } else if (imdbID != null && !imdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
                // Search based on IMDb ID
                moviedb = theMovieDb.moviedbImdbLookup(imdbID, language);
                if (moviedb != null) {
                    tmdbID = moviedb.getId();
                    if (tmdbID != null && !tmdbID.equals("")) {
                        response = tmdbID;
                    } else {
                        logger.fine("MovieDvPosterPlugin: No TMDb ID found for movie!");
                    }
                }
            }
        }
        return response;
    }
}
