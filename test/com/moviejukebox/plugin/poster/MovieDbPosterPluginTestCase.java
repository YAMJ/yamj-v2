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

import junit.framework.TestCase;

import com.moviejukebox.tools.PropertiesUtil;

public class MovieDbPosterPluginTestCase extends TestCase {

    public void testGetId() {
        PropertiesUtil.setProperty("API_KEY_TheMovieDB", "5a1a77e2eba8984804586122754f969f");
        MovieDbPosterPlugin toTest = new MovieDbPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator", "2000");
        assertEquals("98", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://images.themoviedb.org/posters/1297/Gladiator.jpg", posterUrl);
    }
}
