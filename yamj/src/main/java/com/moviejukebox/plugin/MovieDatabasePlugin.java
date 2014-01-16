/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;

/**
 * MovieDatabasePlugin classes must implement this interface in order to be integrated to moviejukebox.
 *
 * Custom implementations classes must be registered into the moviejukebox.properties file (see property "mjb.internet.plugin").
 *
 * Once the class is registered, is called once at the program init, then for each movie in the library.
 *
 * @author Julien
 */
public interface MovieDatabasePlugin {

    /**
     * Get the name if the plugIn
     *
     * @return plugIn name
     */
    String getPluginID();

    /**
     * Called by movie jukebox when processing a movie.
     *
     * Scan information for the specified movie. The provided movie object contains at least the following data:
     * <ul>
     * <li>Title
     * <li>Year (can be unknown)
     * <li>Season (can be unknown)
     * <li>movieFiles (at least one)
     * </ul>
     *
     * @param movie a <tt>Movie</tt> object to update.
     * @return boolean true if the movie was successfully scanned, or false if it needs to be rescanned by a different plugIn if
     * false, then the plugIn should set appropriate type by calling movie.setMovieType() before returning
     */
    boolean scan(Movie movie);

    boolean scan(Person person);

    /**
     * Called by jukebox when there are new episodes files added. Method checks if movie id exists and scan only for new MovieFiles.
     *
     * @param movie a <tt>Movie</tt> object to update.
     */
    void scanTVShowTitles(Movie movie);

    /**
     * Scan NFO file for movie id
     *
     * @param nfo the NFO string representation
     * @param movie a <tt>Movie</tt> object to update.
     * @return
     */
    boolean scanNFO(String nfo, Movie movie);
}
