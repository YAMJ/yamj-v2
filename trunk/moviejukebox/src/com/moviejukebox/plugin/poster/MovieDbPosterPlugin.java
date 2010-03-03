/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import com.moviejukebox.model.Movie;
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
        String response = moviedb.getId();
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getPosterUrl(String id) {
        String returnString = Movie.UNKNOWN;

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

}
