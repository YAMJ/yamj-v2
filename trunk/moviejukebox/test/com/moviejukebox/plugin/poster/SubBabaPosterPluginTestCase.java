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

public class SubBabaPosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "488";

    public void testGetId() {
        SubBabaPosterPlugin toTest = new SubBabaPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator", null, -1);
        assertEquals("488", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(ID_MOVIE);
        assertEquals("http://images.allocine.fr/r_760_x/medias/nmedia/18/64/43/65/19211318.jpg", posterUrl);
    }
}
