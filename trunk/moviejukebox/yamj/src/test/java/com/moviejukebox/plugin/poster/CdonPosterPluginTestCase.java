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

import junit.framework.TestCase;

public class CdonPosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "http://cdon.se/film/gladiator-89625";

    public void testGetId() {
        CdonPosterPlugin toTest = new CdonPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator", null, -1);
        assertEquals(ID_MOVIE, idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(ID_MOVIE).getUrl();
        assertEquals("http://cdon.se/media-dynamic/images/product/000/440/440892.jpg", posterUrl);
    }
}
