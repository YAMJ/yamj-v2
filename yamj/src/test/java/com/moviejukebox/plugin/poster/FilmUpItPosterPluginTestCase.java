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

public class FilmUpItPosterPluginTestCase extends TestCase {

    public void testGetId() {
        FilmUpItPosterPlugin toTest = new FilmUpItPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", null);
        assertEquals("avatar", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://filmup.leonardo.it/posters/loc/500/avatar.jpg", posterUrl);
    }
}
