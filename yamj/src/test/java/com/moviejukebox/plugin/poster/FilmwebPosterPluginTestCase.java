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

import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilmwebPosterPluginTestCase {

    private FilmwebPosterPlugin posterPlugin;

    @BeforeClass
    public static void configure() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", FilmwebPlugin.FILMWEB_PLUGIN_ID);
    }

    @Before
    public void setup() {
        posterPlugin = new FilmwebPosterPlugin();
    }

    @Test
    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://www.filmweb.pl/Avatar", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl("http://www.filmweb.pl/Avatar").getUrl();
        assertEquals("http://gfx.filmweb.pl/po/91/13/299113/7322782.3.jpg?l=1270132598000", posterUrl);
    }

    @Test
    public void testGetIdFromTVInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Prison Break", null, 1);
        assertEquals("http://www.filmweb.pl/Prison.Break", idFromMovieInfo);
    }
}
