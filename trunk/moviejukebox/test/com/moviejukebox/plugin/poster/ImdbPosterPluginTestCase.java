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

public class ImdbPosterPluginTestCase extends TestCase {

    public void testGetId() {
        ImdbPosterPlugin toTest = new ImdbPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar",null);
        assertEquals("tt0499549", idFromMovieInfo);


    }
}
