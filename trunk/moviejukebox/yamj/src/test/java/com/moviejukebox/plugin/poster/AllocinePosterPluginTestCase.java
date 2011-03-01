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

public class AllocinePosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "61282";

    public void testGetId() {
        AllocinePosterPlugin toTest = new AllocinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", null);
        assertEquals("61282", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(ID_MOVIE).getUrl();
        assertEquals("http://images.allocine.fr/r_760_x/medias/nmedia/18/64/43/65/19211318.jpg", posterUrl);
    }
}
