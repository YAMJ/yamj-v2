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

import junit.framework.TestCase;

public class MotechnetPosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "king-arthur-2004";

    public void testGetId() {
        MotechnetPosterPlugin toTest = new MotechnetPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("King arthur", null);
        assertEquals(ID_MOVIE, idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(ID_MOVIE);
        assertEquals("http://www.motechposters.com/posters/king-arthur-2004_poster.jpg", posterUrl);
                     
    }
}
