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

package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;

/**
 * MovieDatabasePlugin classes must implement this interface in order 
 * to be integrated to moviejukebox.
 * 
 * Custom implementations classes must be registered into the 
 * moviejukebox.properties file (see property "mjb.internet.plugin").
 * 
 * Once the class is registered, is called once at the program init,
 * then for each movie in the library.
 * 
 * @author Julien
 */
public interface MovieDatabasePlugin {

    /**
     * Called by movie jukebox when processing a movie.
     *
     * Scan information for the specified movie.
     * The provided movie object conatins at least
     * the following data:
     * <ul>
     * <li>Title
     * <li>Year (can be unknown)
     * <li>Season (can be unknown)
     * <li>movieFiles (at least one)
     * </ul>
     *
     * @param movie a <tt>Movie</tt> object to update.
     * @return boolean true if the movie was successfully scanned, or false if it needs to be rescanned by a different plugin
     * if false, then the plugin should set appropriate type by calling movie.setMovieType() before returning
     */
    public boolean scan(Movie movie);

    /**
     * Called by jukebox when there are new episodes files added.
     * Method checks if movie id exists and scan only for new MovieFiles.
     *
     *
     * @param movie a <tt>Movie</tt> object to update.
     */
    public void scanTVShowTitles(Movie movie);

    /**
     * Scan NFO file for movie id
     *
     * @param nfo
     * @param movie
     */
    public void scanNFO(String nfo, Movie movie);
}
