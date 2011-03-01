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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.tools.PropertiesUtil;

import junit.framework.TestCase;

public class MovieMeterPosterPluginTestCase extends TestCase {

    public void testGetId() {
        PropertiesUtil.setProperty("API_KEY_MovieMeter","tyk0awf19uqm65mjfsqw9z9rx6t706pe");
        MovieMeterPosterPlugin toTest = new MovieMeterPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", null);
        assertEquals("17552", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.moviemeter.nl/images/covers/17000/17552.jpg", posterUrl);
    }
}
