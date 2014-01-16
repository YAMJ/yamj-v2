/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class CdonPosterPluginTestCase {

    private static final String ID_MOVIE = "http://cdon.se/film/gladiator-89625";

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "cdon");
    }

    @Test
    public void testGetId() {
        CdonPosterPlugin posterPlugin = new CdonPosterPlugin();
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Gladiator", null, -1);
        assertEquals(ID_MOVIE, idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(ID_MOVIE).getUrl();
        assertEquals("http://cdon.se/media-dynamic/images/product/000/440/440892.jpg", posterUrl);
    }
}
