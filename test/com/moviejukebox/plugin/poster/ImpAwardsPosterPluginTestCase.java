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

public class ImpAwardsPosterPluginTestCase extends TestCase {

    public void testGetId() {
        ImpAwardsPosterPlugin toTest = new ImpAwardsPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator","2000");
        assertEquals("2000/posters/gladiator_ver1.html", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.impawards.com/2000/posters/gladiator_ver1.jpg", posterUrl);
    }
}
