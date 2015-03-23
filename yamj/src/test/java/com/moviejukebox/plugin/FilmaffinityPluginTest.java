/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.OverrideTools;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Stuart
 */
public class FilmaffinityPluginTest {

    private FilmaffinityPlugin faPlugin = new FilmaffinityPlugin();

    @BeforeClass
    public static void configure() {
        OverrideTools.putMoviePriorities(OverrideFlag.COUNTRY, "filmaffinity,imdb");
        OverrideTools.putMoviePriorities(OverrideFlag.RUNTIME, "filmaffinity,imdb");
        OverrideTools.putMoviePriorities(OverrideFlag.PLOT, "filmaffinity,imdb");
    }

    @Before
    public void setup() {
        faPlugin = new FilmaffinityPlugin();
    }

    /**
     * Test of scan method, of class FilmaffinityPlugin.
     */
    @Test
    public void testScan() {
        Movie movie = new Movie();
        movie.setTitle("Blade Runner", Movie.UNKNOWN);
        movie.setYear("1982", Movie.UNKNOWN);

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("film358476.html", movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));
        assertEquals("112 m", movie.getRuntime());
        assertEquals("Estados Unidos", movie.getCountriesAsString());
        assertTrue(movie.getDirectors().size() > 0);
        assertTrue(movie.getWriters().size() > 0);
        assertTrue(movie.getCast().size() > 0);
        assertTrue(movie.getGenres().size() > 0);
    }

    @Test
    public void testScanNoYear() {
        Movie movie = new Movie();
        movie.setTitle("Cloverfield", Movie.UNKNOWN);

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("2008", movie.getYear());
    }

    @Test
    public void testScanYear() {
        Movie movie = new Movie();
        movie.setTitle("Avatar", Movie.UNKNOWN);
        movie.setYear("2009", Movie.UNKNOWN);

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("2009", movie.getYear());
    }

    @Test
    public void testScanCountry1() {
        Movie movie = new Movie();
        movie.setTitle("25 grados en invierno", Movie.UNKNOWN);
        movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, "730391");
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0356317");
        assertEquals(true, faPlugin.scan(movie));
        assertEquals("Bélgica", movie.getCountriesAsString());
    }

    @Test
    public void testScanCountry2() {
        Movie movie = new Movie();
        movie.setTitle("Matrix Revolutions", Movie.UNKNOWN);
        assertEquals(true, faPlugin.scan(movie));
        assertEquals("Estados Unidos", movie.getCountriesAsString());
    }
}
