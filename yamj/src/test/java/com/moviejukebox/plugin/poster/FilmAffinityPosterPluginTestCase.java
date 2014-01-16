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

import com.moviejukebox.plugin.FilmAffinityInfo;
import com.moviejukebox.tools.PropertiesUtil;

import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Test;

public class FilmAffinityPosterPluginTestCase {

    private FilmAffinityPosterPlugin posterPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
    }

    @Before
    public void setup() {
        posterPlugin = new FilmAffinityPosterPlugin();
    }

    @Test
    public void testGetId_1() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", "2009", -1);
        assertEquals("film495280.html", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://pics.filmaffinity.com/Avatar-208925608-large.jpg", posterUrl);
    }

    @Test
    public void testGetId_2() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Troya", "2004", -1);
        assertEquals("film564615.html", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://pics.filmaffinity.com/Troya-963506535-large.jpg", posterUrl);

        posterPlugin.getPosterUrl("Troya", null);
        assertEquals("http://pics.filmaffinity.com/Troya-963506535-large.jpg", posterUrl);
    }

    @Test
    public void testGetId_3() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("FUTURAMA", null, 2);
        assertEquals("film826281.html", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://pics.filmaffinity.com/Futurama_Serie_de_TV-151391426-large.jpg", posterUrl);
    }
}
