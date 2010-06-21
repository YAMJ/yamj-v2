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

import com.moviejukebox.tools.PropertiesUtil;

public class TheTvDbPosterPluginTestCase extends TestCase {

    public void testGetId() {
        PropertiesUtil.setProperty("API_KEY_TheTVDb", "2805AD2873519EC5");
        TheTvDBPosterPlugin toTest = new TheTvDBPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Friends", null, 1);
        assertEquals("79168", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo,1).getUrl();
        assertEquals("http://thetvdb.com/banners/seasons/79168-1.jpg", posterUrl);
        
        posterUrl = toTest.getPosterUrl(idFromMovieInfo,2).getUrl();
        assertEquals("http://thetvdb.com/banners/seasons/79168-2-4.jpg", posterUrl);
    }
}
