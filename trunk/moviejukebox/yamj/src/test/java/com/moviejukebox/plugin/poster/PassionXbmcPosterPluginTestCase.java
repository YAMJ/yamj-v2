/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import junit.framework.TestCase;

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
