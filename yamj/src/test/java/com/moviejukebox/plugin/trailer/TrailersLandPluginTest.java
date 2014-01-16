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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author iuk
 */
public class TrailersLandPluginTest {

    private TrailersLandPlugin tlPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("trailers.download", false);
    }

    @Before
    public void setup() {
        tlPlugin = new TrailersLandPlugin();
    }

    @Test
    public void testGenerate1() {

        Movie movie = new Movie();
        movie.setTitle("Paradiso Amaro", Movie.UNKNOWN);
        movie.setOriginalTitle("The Descendants", Movie.UNKNOWN);

        assertTrue(tlPlugin.generate(movie));
    }

    @Test
    public void testGenerate2() {

        Movie movie = new Movie();
        movie.setTitle("Bar Sport", Movie.UNKNOWN);

        assertTrue(tlPlugin.generate(movie));
    }
}
