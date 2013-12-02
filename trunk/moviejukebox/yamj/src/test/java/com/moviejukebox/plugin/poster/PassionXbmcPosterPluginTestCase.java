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
import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class PassionXbmcPosterPluginTestCase {

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "passionxbmc");
    }

    @Test
    public void testGetId() {
        PassionXbmcPosterPlugin posterPlugin = new PassionXbmcPosterPlugin();
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("61282", idFromMovieInfo);

        IImage posterImage = posterPlugin.getPosterUrl(idFromMovieInfo);
        assertEquals("http://passion-xbmc.org/scraper/Gallery/main/Poster-357939.jpg", posterImage.getUrl());
    }
}
