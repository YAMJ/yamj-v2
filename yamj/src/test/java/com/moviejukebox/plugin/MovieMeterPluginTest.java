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
package com.moviejukebox.plugin;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.TestData;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.StringTools;

public class MovieMeterPluginTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPluginTest.class);
    private static MovieMeterPlugin plugin;
    private static final List<TestData> TEST_DATA = new ArrayList<>();

    @BeforeClass
    public static void configure() {
        doConfiguration();
        loadMainProperties();
        loadApiProperties();

        plugin = new MovieMeterPlugin();
        TEST_DATA.add(new TestData("Avatar", "2009", "17552"));
    }

    /**
     * Test of scan method, of class MovieMeterPlugin.
     */
    @Test
    public void testScan() {
        LOG.info("scan");
        Movie movie;

        for (TestData td : TEST_DATA) {
            movie = new Movie();
            movie.setId(MovieMeterPlugin.MOVIEMETER_PLUGIN_ID, td.id);
            assertTrue("Failed to scan " + td.title, plugin.scan(movie));
            if (td.id.equals("17552")) {
                // Do the checks on Avatar
                LOG.info("Testing: {}", td.toString());

                assertEquals("Wrong title", td.title, movie.getTitle());
                assertEquals("Wrong original title", td.title, movie.getOriginalTitle());
                assertEquals("Wrong year", td.year, movie.getYear());
                assertEquals("Incorrect number of countries", 2, movie.getCountries().size());
                assertEquals("Wrong countries", "Verenigde Staten / Verenigd Koninkrijk", movie.getCountriesAsString());
                assertTrue("No plot", StringTools.isValidString(movie.getPlot()));
                assertTrue("No genres", movie.getGenres().size() > 0);
                assertTrue("No actors", movie.getCast().size() > 0);
                assertTrue("No directors", movie.getDirectors().size() > 0);
            }
        }
    }

    /**
     * Test of getMovieId method, of class MovieMeterPlugin.
     */
    @Test
    public void testGetMovieId_Movie() {
        LOG.info("getMovieId - Movie");
        Movie movie;
        String result;

        for (TestData td : TEST_DATA) {
            movie = new Movie();
            movie.setTitle(td.title, "TEST");
            movie.setYear(td.year, "TEST");

            result = plugin.getMovieId(movie);
            assertEquals("Failed to get the correct ID for " + td.title, td.id, result);
        }
    }

    /**
     * Test of getMovieId method, of class MovieMeterPlugin.
     */
    @Test
    public void testGetMovieId_String_String() {
        LOG.info("getMovieId - Title & Year");

        for (TestData td : TEST_DATA) {
            LOG.info("Testing {} ({}), expecting {}", td.title, td.year, td.id);
            String result = plugin.getMovieId(td.title, td.year);
            assertEquals("Failed to get the correct ID for " + td.title, td.id, result);
        }
    }

    /**
     * Test of scanNFO method, of class MovieMeterPlugin.
     */
    @Ignore
    public void testScanNFO() {
        LOG.info("scanNFO");
        String nfo = "";
        Movie movie = null;
        boolean expResult = false;
        boolean result = plugin.scanNFO(nfo, movie);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
