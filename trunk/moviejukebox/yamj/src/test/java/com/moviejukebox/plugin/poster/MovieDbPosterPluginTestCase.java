/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

public class MovieDbPosterPluginTestCase {

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "themoviedb");
    }

    @Test
    public void testGetId() {
        PropertiesUtil.setProperty("API_KEY_TheMovieDB", "5a1a77e2eba8984804586122754f969f");
        MovieDbPosterPlugin posterPlugin = new MovieDbPosterPlugin();
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Gladiator", "2000");
        assertEquals("98", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://d3gtl9l2a4fn1j.cloudfront.net/t/p/original/6WBIzCgmDCYrqh64yDREGeDk9d3.jpg", posterUrl);
    }
}
