/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.tools.PropertiesUtil;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SratimPosterPluginTestCase {

    private SratimPosterPlugin posterPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "sratim");
    }

    @Before
    public void setup() {
        posterPlugin = new SratimPosterPlugin();
    }

    @Test
    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Matrix", null);
        assertEquals("1147", idFromMovieInfo);
    }

    @Test
    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("The Lost Islands", null, 1);
        assertEquals("2116", idFromMovieInfo);
    }

    @Test
    public void testGetPosterUrl() {
        String posterUrl = posterPlugin.getPosterUrl("143628").getUrl();
        assertEquals("http://sratim.co.il/photos/titles/normal/8/m43628_20111205145500_824823715264.jpg", posterUrl);
    }
}
