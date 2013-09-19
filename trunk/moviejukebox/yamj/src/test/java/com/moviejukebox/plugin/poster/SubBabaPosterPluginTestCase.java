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

import org.apache.log4j.BasicConfigurator;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SubBabaPosterPluginTestCase {

    private SubBabaPosterPlugin posterPlugin;

    @BeforeClass
    public static void configure() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "subbaba");
    }

    @Before
    public void setup() {
        posterPlugin = new SubBabaPosterPlugin();
    }

    @Test
    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Gladiator", null);
        assertEquals("488", idFromMovieInfo);
    }

    @Test
    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Prison Break", null, 1);
        assertEquals("1800", idFromMovieInfo);
    }

    @Test
    public void testGetPosterUrl() {
        IImage posterImage = posterPlugin.getPosterUrl("6415");
        assertEquals("http://www.sub-baba.com/site/download.php?type=1&id=6415", posterImage.getUrl());
        assertEquals(Movie.UNKNOWN, posterImage.getSubimage());
    }

    @Test
    public void testGetPosterUrlWithSubimage() {
        // front-back poster needs to be cut
        IImage posterImage = posterPlugin.getPosterUrl("488");
        assertEquals("http://www.sub-baba.com/site/download.php?type=1&id=488", posterImage.getUrl());
        assertEquals("0, 0, 47, 100", posterImage.getSubimage());
    }
}
