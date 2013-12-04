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
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SubBabaPosterPluginTest {

    private SubBabaPosterPlugin posterPlugin;
    private static final int SBID_AVATAR = 6415;
    private static final int SBID_PRISON_BREAK = 1800;
    private static final int SBID_GLADIATOR = 488;
    private static final String IMDBID_AVATAR = "tt0499549";
    private static final String IMDBID_GLADIATOR = "tt0172495";
    private static final String TITLE_AVATAR = "Avatar";
    private static final String TITLE_PRISON_BREAK = "Prison Break";

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "subbaba");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        posterPlugin = new SubBabaPosterPlugin();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo(TITLE_AVATAR, null);
        assertEquals(String.valueOf(SBID_AVATAR), idFromMovieInfo);
    }

    @Test
    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo(TITLE_PRISON_BREAK, null, 1);
        assertEquals(String.valueOf(SBID_PRISON_BREAK), idFromMovieInfo);
    }

    @Test
    public void testGetPosterUrl() {
        IImage posterImage = posterPlugin.getPosterUrl(IMDBID_AVATAR);
        assertEquals("http://www.sub-baba.com/site/posters/6415_avatar_mgvgzr3.jpg", posterImage.getUrl());
        assertEquals(Movie.UNKNOWN, posterImage.getSubimage());
    }

    @Test
    public void testGetPosterUrlWithSubimage() {
        // front-back poster needs to be cut
        IImage posterImage = posterPlugin.getPosterUrl(IMDBID_GLADIATOR);
        assertEquals("http://www.sub-baba.com/site/covers/488_gladiator_dund9h7.jpg", posterImage.getUrl());
        assertEquals("0, 0, 47, 100", posterImage.getSubimage());
    }
}
