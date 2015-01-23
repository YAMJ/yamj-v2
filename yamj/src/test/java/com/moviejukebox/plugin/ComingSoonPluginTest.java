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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author iuk
 */
public class ComingSoonPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(ComingSoonPluginTest.class);

    private ComingSoonPlugin csPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("comingsoon.imdb.scan", "never");
        PropertiesUtil.setProperty("priority.title", "comingsoon,imdb");
        PropertiesUtil.setProperty("priority.originaltitle", "comingsoon,imdb");
    }

    @Before
    public void setup() {
        csPlugin = new ComingSoonPlugin();
    }

    @Test
    public void testGetIdFromComingSoon() {
        LOG.info("testGetIdFromComingSoon");
        String id = csPlugin.getComingSoonId("Avatar", "2009", "comingsoon");
        assertEquals("Wrong ID", "846", id);
    }

    @Test
    public void testGetIdFromGoogle() {
        LOG.info("testGetIdFromGoogle");
        String id = csPlugin.getComingSoonId("Avatar", "2009", "google");
        assertEquals("Wrong ID", "846", id);
    }

    @Test
    public void testGetIdFromYahoo() {
        LOG.info("testGetIdFromYahoo");
        String id = csPlugin.getComingSoonId("Avatar", "2009", "yahoo");
        assertEquals("Wrong ID", "846", id);
    }

    @Test
    public void testScan() {
        LOG.info("testScan");
        Movie movie = new Movie();
        movie.setId(ComingSoonPlugin.COMINGSOON_PLUGIN_ID, "48891");

        assertTrue("Failed to scan", csPlugin.scan(movie));
        assertEquals("Wrong title", "A Proposito Di Davis", movie.getTitle());
        assertEquals("Wrong original title", "Inside Llewyn Davis", movie.getOriginalTitle());
        assertEquals("Wrong year", "2013", movie.getYear());
        assertTrue("No Directors", movie.getDirectors().size() > 0);
        assertTrue("No Writers", movie.getWriters().size() > 0);
        assertTrue("No Cast", movie.getCast().size() > 0);
        assertTrue("No plot", movie.getPlot().length() > 0);
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
        List<String> titleList = new ArrayList<>();
        titleList.add("Matrix");
        titleList.add("Gli Aristogatti");
        titleList.add("Inception");
        titleList.add("L'arte Del Sogno");
        titleList.add("Lettere Da Iwo Jima");

        for (String title : titleList) {
            LOG.info("Testing {}", title);
            Movie movie = new Movie();
            movie.setTitle(title, csPlugin.getPluginID());
            assertTrue("Scan failed for " + movie.getTitle(), csPlugin.scan(movie));

            LOG.info(movie.toString());

            assertEquals("Wrong title", title, movie.getTitle());
            assertTrue("Wrong number of directors", movie.getDirectors().size() > 0);
            assertTrue("Wrong number of writers", movie.getWriters().size() > 0);

            assertNotNull("No movie object for " + title, movie);
            if (!title.equalsIgnoreCase("Gli Aristogatti")) {
                assertTrue("No cast found for " + title, movie.getCast().size() > 0);
            }
            assertTrue("Invalid release date for " + title, StringTools.isValidString(movie.getReleaseDate()));

            assertTrue("No Plot", StringTools.isValidString(movie.getPlot()));
            assertTrue("No year", StringTools.isValidString(movie.getYear()));
            assertTrue("No runtime", StringTools.isValidString(movie.getRuntime()));
            assertTrue("No country", movie.getCountries().size() > 0);
            assertTrue("No rating", movie.getRating() > -1);
        }
    }
}
