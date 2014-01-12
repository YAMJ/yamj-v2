/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
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
import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.model.Person;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.omertron.themoviedbapi.model.CollectionInfo;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author stuart.boston
 */
public class TheMovieDbPluginTest {

    private static final Logger LOG = Logger.getLogger(TheMovieDbPluginTest.class);
    private static TheMovieDbPlugin TMDb;

    public TheMovieDbPluginTest() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        TMDb = new TheMovieDbPlugin();
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMovieNames() {
        LOG.info("Test Movie Names");

        Map<String, Integer> filenames = new HashMap<String, Integer>();
        filenames.put("Escape from LA (1996).avi", 10061);
        filenames.put("AI Artificial Intelligence.mkv", 644);
        filenames.put("Unknown (2006).mkv", 9828);
        filenames.put("Unknown (2011).mkv", 48138);
        filenames.put("In the Line of Duty 4 (1989).ts", 39854);

        for (Map.Entry<String, Integer> entry : filenames.entrySet()) {
            LOG.info("Testing: '" + entry.getKey() + "'");
            MovieFileNameDTO x = scan(entry.getKey());
            Movie movie = new Movie();
            movie.mergeFileNameDTO(x);
            boolean result = TMDb.scan(movie);
            assertTrue("Failed to scan movie: " + movie.getTitle(), result);
            assertEquals(String.valueOf(entry.getValue()), movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID));
        }
    }

    private static MovieFileNameDTO scan(String filename) {
        File file = new File(filename) {
            @Override
            public boolean isFile() {
                return true;
            }
        };

        return MovieFilenameScanner.scan(file);
    }

    /**
     * Test of getPluginID method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetPluginID() {
        LOG.info("getPluginID");
        String expResult = "themoviedb";
        String result = TMDb.getPluginID();
        assertEquals("Plugin ID has changed", expResult, result);
    }

    /**
     * Test of scan method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testScan_Movie() {
        LOG.info("scan Movie");
        Movie movie = new Movie();
        movie.setTitle("Blade Runner", "NFO");
        movie.setYear("1982", "NFO");
        movie.setBaseName("Blade Runner");

        boolean result = TMDb.scan(movie);
        assertTrue("Failed to scan movie", result);
        assertEquals("Wrong result returned", "78", movie.getId(TMDb.getPluginID()));
    }

    /**
     * Test of scanTVShowTitles method, of class TheMovieDbPlugin.
     */
    @Ignore("TMDB does not have TV shows yet")
    public void testScanTVShowTitles() {
        LOG.info("scanTVShowTitles");
    }

    /**
     * Test of scan method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testScan_Person() {
        LOG.info("scan Person");
        Person person = new Person();
        person.setName("Vin Diesel");

        boolean result = TMDb.scan(person);
        assertTrue("Failed to scan person", result);
//        assertEquals("Failed to get correct ID", "3", person.getId(TMDb.getPluginID()));
//        assertEquals("Failed to get correct person", "Harrison Ford", person.getName());
    }

    /**
     * Test of getPersonId method, of class TheMovieDbPlugin. //
     */
    @Ignore
    public void testGetPersonId_Person() {
        LOG.info("getPersonId");
        Person person = new Person();
        person.setName("Chloë Moretz");

        String result = TMDb.getPersonId(person);
        assertEquals("Failed to get correct ID", "56734", result);
        assertEquals("Failed to get correct person", "Chloë Moretz", person.getName());
    }

    /**
     * Test of getPersonId method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetPersonId_String() {
        LOG.info("getPersonId");
        String name = "Chloë Moretz";
        String expResult = "56734";
        String result = TMDb.getPersonId(name);
        assertEquals("Failed to get correct ID", expResult, result);
    }

    /**
     * Test of getCollectionInfo method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionInfo_int() {
        LOG.info("getCollectionInfo");
        int collectionId = 119;
        CollectionInfo result = TMDb.getCollectionInfo(collectionId);

        assertEquals("Incorrect collection returned", "The Lord of the Rings Collection", result.getName());
        assertNotNull("Collection parts is null", result.getParts());
        assertFalse("No collection parts", result.getParts().isEmpty());
    }

    /**
     * Test of getCollectionInfo method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionInfo_int_String() {
        LOG.info("getCollectionInfo");
        int collectionId = 119;
        String languageCode = "de";
        CollectionInfo result = TMDb.getCollectionInfo(collectionId, languageCode);
        assertEquals("Incorrect collection returned", "Der Herr der Ringe Trilogie", result.getName());
        assertNotNull("Collection parts is null", result.getParts());
        assertFalse("No collection parts", result.getParts().isEmpty());
    }

    /**
     * Test of getCollectionPoster method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionPoster() {
        LOG.info("getCollectionPoster");
        int collectionId = 119;
        String languageCode = "en";
        String result = TMDb.getCollectionPoster(collectionId, languageCode);
        LOG.info("result: " + result);
        assertTrue("No/Invalid poster returned", StringTools.isValidString(result));
    }

    /**
     * Test of getCollectionFanart method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionFanart() {
        LOG.info("getCollectionFanart");
        int collectionId = 119;
        String languageCode = "en";
        String result = TMDb.getCollectionFanart(collectionId, languageCode);
        LOG.info("result: " + result);
        assertTrue("No/Invalid fanart returned", StringTools.isValidString(result));
    }

    /**
     * Test of getCollectionCacheKey method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionCacheKey() {
        LOG.info("getCollectionCacheKey");
        int collectionId = 666;
        String languageCode = "en";
        String expResult = TheMovieDbPlugin.CACHE_COLLECTION + "-666-en";
        String result = TheMovieDbPlugin.getCollectionCacheKey(collectionId, languageCode);
        assertEquals("Invalid cache key generated for collection", expResult, result);
    }

    /**
     * Test of getCollectionImagesCacheKey method, of class TheMovieDbPlugin.
     */
    @Ignore
    public void testGetCollectionImagesCacheKey() {
        LOG.info("getCollectionImagesCacheKey");
        int collectionId = 666;
        String languageCode = "en";
        String expResult = TheMovieDbPlugin.CACHE_COLLECTION_IMAGES + "-666-en";
        String result = TheMovieDbPlugin.getCollectionImagesCacheKey(collectionId, languageCode);
        assertEquals("Invalid cache key generated for collection images ", expResult, result);
    }
}
