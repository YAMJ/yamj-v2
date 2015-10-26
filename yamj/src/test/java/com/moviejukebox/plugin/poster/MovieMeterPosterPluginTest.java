/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.plugin.poster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.TestData;
import com.moviejukebox.model.IImage;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 *
 * @author Stuart.Boston
 */
public class MovieMeterPosterPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPosterPluginTest.class);
    private static MovieMeterPosterPlugin plugin;
    private static final List<TestData> testData = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "moviemeterposter");
        plugin = new MovieMeterPosterPlugin();
    }

    @Before
    public void setUp() {
        testData.add(new TestData("Avatar", "2009", "17552"));
    }

    /**
     * Test of getIdFromMovieInfo method, of class MovieMeterPosterPlugin.
     */
    @Test
    public void testGetIdFromMovieInfo() {
        LOG.info("getIdFromMovieInfo");

        for (TestData td : testData) {
            LOG.info("Testing {} ({})", td.title, td.year);
            String result = plugin.getIdFromMovieInfo(td.title, td.year);
            assertEquals("Wrong ID found for " + td.title, td.id, result);
        }
    }

    /**
     * Test of getPosterUrl method, of class MovieMeterPosterPlugin.
     */
    @Test
    public void testGetPosterUrl_String() {
        LOG.info("getPosterUrl (ID)");

        for (TestData td : testData) {
            IImage result = plugin.getPosterUrl(td.id);
            assertTrue("No image returned", StringTools.isValidString(result.getUrl()));
        }
    }

    /**
     * Test of getPosterUrl method, of class MovieMeterPosterPlugin.
     */
    @Ignore
    public void testGetPosterUrl_String_String() {
        LOG.info("getPosterUrl (Title/Year)s");

        for (TestData td : testData) {
            IImage result = plugin.getPosterUrl(td.title, td.year);
            assertTrue("No image returned", StringTools.isValidString(result.getUrl()));
        }
    }
}
