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

public class ScopeDkPosterPluginTestCase extends TestCase {

    public void testGetId() {
        ScopeDkPosterPlugin toTest = new ScopeDkPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator", null);
        assertEquals("1", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.scope.dk/images/film/000001/00000333_gladiator_ukendt_360.jpg", posterUrl);
    }
}
