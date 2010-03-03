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

public class FilmwebPosterPluginTestCase extends TestCase {

    public void testGetId() {
        FilmwebPosterPlugin toTest = new FilmwebPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://avatar.filmweb.pl/", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://gfx.filmweb.pl/po/91/13/299113/7307579.3.jpg?l=1262705192000", posterUrl);
    }
}
