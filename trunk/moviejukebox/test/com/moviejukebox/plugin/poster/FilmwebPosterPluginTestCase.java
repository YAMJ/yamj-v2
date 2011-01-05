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

public class FilmwebPosterPluginTestCase extends TestCase {
    FilmwebPosterPlugin posterPlugin = new FilmwebPosterPlugin();

    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://www.filmweb.pl/Avatar", idFromMovieInfo);
    }

    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Prison Break", null, 1);
        assertEquals("http://www.filmweb.pl/Prison.Break", idFromMovieInfo);
    }

    public void testGetPosterUrl() {
        String posterUrl = posterPlugin.getPosterUrl("http://www.filmweb.pl/Avatar").getUrl();
        assertEquals("http://gfx.filmweb.pl/po/91/13/299113/7322782.3.jpg?l=1270132598000", posterUrl);
    }
}
