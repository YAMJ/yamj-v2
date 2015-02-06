/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.TestData;
import com.moviejukebox.model.IImage;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stuart.Boston
 */
public class MovieMeterPosterPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPosterPluginTest.class);
    private static MovieMeterPosterPlugin plugin;
    private static final List<TestData> testData = new ArrayList<>();

    public MovieMeterPosterPluginTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "moviemeterposter");
        plugin = new MovieMeterPosterPlugin();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        testData.add(new TestData("Avatar", "2009", "17552"));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getIdFromMovieInfo method, of class MovieMeterPosterPlugin.
     */
    @Test
    public void testGetIdFromMovieInfo() {
        LOG.info("getIdFromMovieInfo");

        for (TestData td : testData) {
            String result = plugin.getIdFromMovieInfo(td.title, td.year);
            assertEquals("Wrong ID found for " + td.title, td.id, result);
        }
    }

    /**
     * Test of getPosterUrl method, of class MovieMeterPosterPlugin.
     */
    @Test
    public void testGetPosterUrl_String() {
        LOG.info("getPosterUrl");

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
        LOG.info("getPosterUrl");

        for (TestData td : testData) {
            IImage result = plugin.getPosterUrl(td.title, td.year);
            assertTrue("No image returned", StringTools.isValidString(result.getUrl()));
        }
    }

    /**
     * Test of getName method, of class MovieMeterPosterPlugin.
     */
    @Ignore("No need to test")
    public void testGetName() {
    }

}
