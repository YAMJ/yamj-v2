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

import com.moviejukebox.model.IImage;

public class PassionXbmcPosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "61282";

    public void testGetId() {
        PassionXbmcPosterPlugin toTest = new PassionXbmcPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", null);
        assertEquals("61282", idFromMovieInfo);

        IImage posterImage = toTest.getPosterUrl(ID_MOVIE);
        assertEquals("http://passion-xbmc.org/scraper/Gallery/main/Poster-357939.jpg", posterImage.getUrl());
    }
}
