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

public class SratimPosterPluginTestCase extends TestCase {
    SratimPosterPlugin posterPlugin = new SratimPosterPlugin();

    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://www.sratim.co.il/movies/view.aspx?id=43628", idFromMovieInfo);
    }

    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("The Lost Islands", null, 1);
        assertEquals("http://www.sratim.co.il/movies/series/view.aspx?id=116", idFromMovieInfo);
    }

    public void testGetPosterUrl() {
        String posterUrl = posterPlugin.getPosterUrl("http://www.sratim.co.il/movies/view.aspx?id=43628").getUrl();
        assertEquals("http://www.sratim.co.il/movies/images/8/43628.jpg", posterUrl);
    }
}
