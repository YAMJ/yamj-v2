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

public class FilmAffinityPosterPluginTestCase extends TestCase {

    public void testGetId() {
        FilmAffinityPosterPlugin toTest = new FilmAffinityPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", "2009", -1);
        assertEquals("495280.html", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://pics.filmaffinity.com/Avatar-208925608-large.jpg", posterUrl);
        
        idFromMovieInfo = toTest.getIdFromMovieInfo("Troya", "2004", -1);
        assertEquals("564615.html", idFromMovieInfo);

        posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://pics.filmaffinity.com/Troya_Troy-963506535-large.jpg", posterUrl);
        
        toTest.getPosterUrl("Troya", null);
        assertEquals("http://pics.filmaffinity.com/Troya_Troy-963506535-large.jpg", posterUrl);
        
        idFromMovieInfo = toTest.getIdFromMovieInfo("FUTURAMA",null,2);
        assertEquals("826281.html", idFromMovieInfo);

        posterUrl =toTest.getPosterUrl(idFromMovieInfo);
        
        assertEquals("http://pics.filmaffinity.com/Futurama_Serie_de_TV-151391426-large.jpg", posterUrl);
        
        
        idFromMovieInfo = toTest.getIdFromMovieInfo("Crepusculo",null,-1);
        assertEquals("826281.html", idFromMovieInfo);

        posterUrl =toTest.getPosterUrl(idFromMovieInfo);
        
        assertEquals("http://pics.filmaffinity.com/Futurama_Serie_de_TV-151391426-large.jpg", posterUrl);
    }
}
