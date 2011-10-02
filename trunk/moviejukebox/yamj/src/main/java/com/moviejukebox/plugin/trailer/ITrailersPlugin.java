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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.Movie;

public interface ITrailersPlugin {

    public boolean generate(Movie movie);

    public String getName();
}
