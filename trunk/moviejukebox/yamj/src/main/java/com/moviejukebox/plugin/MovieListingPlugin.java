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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;

/**
 * User: JDGJr Date: Feb 15, 2009
 */
public interface MovieListingPlugin {

    String typeMovie = "Movie";
    String typeTVShow = "TV Show";
    String typeTVShowNoSpace = "TVShow";
    String typeExtra = "Extra";
    String typeAll = "All";
    String UNKNOWN = Movie.UNKNOWN;

    void generate(Jukebox jukebox, Library library);
} // interface MovieListingPlugin
