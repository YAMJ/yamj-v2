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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MovieMeterPluginTest {

    private MovieMeterPlugin movieMeterPlugin;

    @BeforeClass
    public static void setUpClass() {

        PropertiesUtil.setProperty("mjb.internet.plugin", "com.moviejukebox.plugin.OfdbPlugin");
        PropertiesUtil.setProperty("API_KEY_MovieMeter", "tyk0awf19uqm65mjfsqw9z9rx6t706pe");
    }

    @Before
    public void setUp() {
        movieMeterPlugin = new MovieMeterPlugin();
    }

    @Test
    public void testGetMovieId() {
        String id = movieMeterPlugin.getMovieId("Avatar", "2009");
        assertEquals("17552", id);
    }

    @Test
    public void testScan() {
        Movie movie = new Movie();
        movie.setId(MovieMeterPlugin.MOVIEMETER_PLUGIN_ID, "17552");
        movieMeterPlugin.scan(movie);
        assertEquals("Avatar", movie.getTitle());
        assertEquals("Avatar", movie.getOriginalTitle());
        assertEquals("2009", movie.getYear());
        assertEquals(2, movie.getCountries().size());
        assertEquals("Verenigde Staten / Verenigd Koninkrijk", movie.getCountriesAsString());
    }
}
