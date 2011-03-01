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
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;

public class SubBabaPosterPluginTestCase extends TestCase {

    SubBabaPosterPlugin posterPlugin = new SubBabaPosterPlugin();

    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Gladiator", null);
        assertEquals("488", idFromMovieInfo);
    }

    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Prison Break", null, 1);
        assertEquals("1800", idFromMovieInfo);
    }

    public void testGetPosterUrl() {
        IImage posterImage = posterPlugin.getPosterUrl("6415");
        assertEquals("http://www.sub-baba.com/site/download.php?type=1&id=6415", posterImage.getUrl());
        assertEquals(Movie.UNKNOWN, posterImage.getSubimage());
    }

    public void testGetPosterUrlWithSubimage() {
        // front-back poster needs to be cut
        IImage posterImage = posterPlugin.getPosterUrl("488");
        assertEquals("http://www.sub-baba.com/site/download.php?type=1&id=488", posterImage.getUrl());
        assertEquals("0, 0, 47, 100", posterImage.getSubimage());
    }
}
