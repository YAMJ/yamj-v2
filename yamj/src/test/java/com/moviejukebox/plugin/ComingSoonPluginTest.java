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
import com.moviejukebox.tools.StringTools;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author iuk
 */
public class ComingSoonPluginTest {

    private static final Logger LOG = Logger.getLogger(ComingSoonPluginTest.class);
    private ComingSoonPlugin csPlugin;

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
    }

    @Before
    public void setUp() {
        PropertiesUtil.setProperty("comingsoon.imdb.scan", "nevwe");
        PropertiesUtil.setProperty("priority.title", "comingsoon,imdb");
        PropertiesUtil.setProperty("priority.originaltitle", "comingsoon,imdb");
        csPlugin = new ComingSoonPlugin();
    }

    @Test
    public void testScanNoYear() {
        LOG.info("testScanNoYear");
        Movie movie = new Movie();
        movie.setTitle("L'Incredibile Storia Di Winter Il Delfino", csPlugin.getPluginID());

        assertTrue("Failed to scan", csPlugin.scan(movie));
        assertEquals("Wrong title", "L'incredibile Storia Di Winter Il Delfino", movie.getTitle());
        assertEquals("Wrong original title", "Dolphin Tale", movie.getOriginalTitle());
        assertEquals("Wrong year", "2011", movie.getYear());
        assertTrue("No Directors", movie.getDirectors().size() > 0);
        assertTrue("No Writers", movie.getWriters().size() > 0);
        assertTrue("No Cast", movie.getCast().size() > 0);
        assertTrue("No plot", movie.getPlot().length() > 0);
    }

    @Test
    public void testScanList() {
        LOG.info("testScanList");
        String[] titleList = {
            "Matrix",
            "Gli Aristogatti",
            "Inception",
            "L'arte Del Sogno",
            "Lettere Da Iwo Jima"
        };

        for (int i = 0; i < titleList.length; i++) {
            LOG.info("Testing " + titleList[i]);
            Movie movie = new Movie();
            movie.setTitle(titleList[i], csPlugin.getPluginID());
            assertTrue("Scan failed for " + movie.getTitle(), csPlugin.scan(movie));
            assertEquals("Wrong title", titleList[i], movie.getTitle());
            assertTrue("Wrong number of directors", movie.getDirectors().size() > 0);
            assertTrue("Wrong number of writers", movie.getWriters().size() > 0);

            if (i != 1) {
                assertNotNull("No movie object for " + titleList[i], movie);
                assertTrue("No cast found for " + titleList[i], movie.getCast().size() > 0);
                assertTrue("Invalid release date for " + titleList[i], StringTools.isValidString(movie.getReleaseDate()));
            }

            assertTrue("No Plot", StringTools.isValidString(movie.getPlot()));
            assertTrue("No year", StringTools.isValidString(movie.getYear()));
            assertTrue("No runtime", StringTools.isValidString(movie.getRuntime()));
            assertTrue("No country", StringTools.isValidString(movie.getCountry()));
            assertTrue("No rating", movie.getRating() > -1);
        }
    }
}
