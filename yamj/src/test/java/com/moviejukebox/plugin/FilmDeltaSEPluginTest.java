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
import com.moviejukebox.tools.WebBrowser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilmDeltaSEPluginTest {

    private FilmDeltaPluginMock toTest;
    private static final Logger LOG = Logger.getLogger(FilmDeltaSEPluginTest.class);
    /* offline = true, run tests with mocked pages to verify that
     * no changes in project code has been made that breaks the
     * plugin code
     *
     * offline = false, run tests against the actual web pages to
     * check that no changes in their page structure has been made
     */
    private boolean offline;

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("priority.title", "filmdelta,imdb");
        PropertiesUtil.setProperty("priority.originaltitle", "filmdelta,imdb");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // uncomment the line below to check if tests are still up to date
        offline = true;
        toTest = new FilmDeltaPluginMock(offline);
        toTest.init();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreate() {
        assertNotNull(toTest);
    }

    @Test
    public void testScanNFONoUrl() {
        LOG.info("testScanNFONoUrl");
        Movie movie = new Movie();
        toTest.scanNFO("", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
    }

    @Test
    public void testScanNFO() {
        LOG.info("testScanNFO");
        Movie movie = new Movie();
        toTest.scanNFO("http://www.filmdelta.se/prevsearch/aristocats/filmer/20892/aristocats/", movie);
        assertEquals("Failed prevsearch test", "20892/aristocats", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        LOG.info("Testing filmer");
        movie = new Movie();
        toTest.scanNFO("http://www.filmdelta.se/filmer/22481/djungelboken/", movie);
        assertEquals("Failed filmer test", "22481/djungelboken", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
    }

    @Test
    public void testScanNFOWithImdbAndFilmdeltaUrl() {
        LOG.info("testScanNFOWithImdbAndFilmdeltaUrl");
        Movie movie = new Movie();
        toTest.scanNFO("http://www.imdb.com/title/tt0065421/\n\nhttp://www.filmdelta.se/prevsearch/aristocats/filmer/20892/aristocats/", movie);
        assertEquals("20892/aristocats", movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
    }

    @Test
    public void testGetFilmdeltaIdWithName() {
        LOG.info("testGetFilmdeltaIdWithName");
        toTest.setRequestResult("<p><a href=\"/url?q=http://www.filmdelta.se/filmer/20892/aristocats/&amp;sa=U&amp;ei=FXI0TPOCNNCcOOnekKMC&amp;ved=0CAUQFjAA&amp;usg=AFQjCNGLoWMaWaUe85eWov0O7odTgg9WMg\"><b>Aristocats</b> - Filmdelta - Filmdatabas på svenska</a><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
        assertEquals("20892/aristocats", toTest.getMovieId("aristocats", null));
    }

    @Test
    public void testGetFilmdeltaIdWithNameAndYear() {
        LOG.info("testGetFilmdeltaIdWithNameAndYear");
        toTest.setRequestResult("</table><p><a href=\"/url?q=http://www.filmdelta.se/filmer/147238/barbie_mariposa/&amp;sa=U&amp;ei=KXQ0TKvkI42XOI28xIIC&amp;ved=0CAUQFjAA&amp;usg=AFQjCNFkufV1Q48m6uKgaLa0MHxSAEWbAQ\"><b>Barbie</b> i en julsaga - Filmdelta - Filmdatabas på svenska</a><table");
        assertEquals("147238/barbie_mariposa", toTest.getMovieId("barbie mariposa", "2007"));
    }

    @Test
    public void testGetFilmdeltaIdNotFound() {
        LOG.info("testGetFilmdeltaIdNotFound");
        toTest.setRequestResult("<br>Din sökning - <b>xxyyzz+site:filmdelta.se/filmer</b> - matchade inte något dokument.<br><br>Förslag:<ul><li>Kontrollera att alla ord är rättstavade.</li>");
        assertEquals(Movie.UNKNOWN, toTest.getMovieId("xxyyzz", null));
    }

    @Test
    public void testGetFilmdeltaIdTheMatrix() {
        LOG.info("testGetFilmdeltaIdTheMatrix");
        toTest.setRequestResult("</table></p><p><a href=\"/url?q=http://www.filmdelta.se/filmer/74403/the_matrix/&amp;sa=U&amp;ei=B5pWTNTFC4nLOLHD2LIO&amp;ved=0CAcQFjAB&amp;usg=AFQjCNEhfuVFg77dObKQcs-uc2pxE-7s3g\">The <b>Matrix</b> - Filmdelta - Filmdatabas på svenska</a><table");
        assertEquals("74403/the_matrix", toTest.getMovieId("matrix", "1999"));
    }

    @Test
    public void testGetFilmdeltaIdAliensVsAvatars() {
        LOG.info("testGetFilmdeltaIdAliensVsAvatars");
        toTest.setRequestResult(null);
        assertEquals("174073/aliens_vs_avatars", toTest.getMovieId("Aliens vs Avatars", "2011"));
    }

    @Test
    public void testUpdateFilmdeltaMediaInfo() {
        LOG.info("testUpdateFilmdeltaMediaInfo");
        // Set the plot and outline lengths
        PropertiesUtil.setProperty("movie.plot.maxLength", 500);
        PropertiesUtil.setProperty("movie.outline.maxLength", 300);
        Movie movie = new Movie();

        // First read the testdata - a sample page from Filmdelta.se into variable testpage
        String testpage;
        try {
            testpage = FileUtils.readFileToString(new File("src/test/java/com/moviejukebox/plugin/FilmDeltaSEPluginTest_page_aristocats.txt"));
        } catch (IOException ex) {
            LOG.info(System.getProperty("user.dir"));
            fail("Testfile for testUpdateFilmdeltaMediaInfo not found");
            return;
        }

        // Prepare the mock to use this testdata
        toTest.setRequestResult(testpage);
        String filmdeltaId = "20892/aristocats";

        // Do the testing
        toTest.updateMediaInfo(movie, filmdeltaId);
        assertEquals("Aristocats", movie.getTitle());
        assertEquals("USA", movie.getCountriesAsString());
        assertEquals("1970", movie.getYear());
        assertEquals(70, movie.getRating());
        assertEquals("78", movie.getRuntime());
        String expectedPlot = "I hjärtat av Paris bor den tjusiga katten Duchesse med sina tre kattungar hos den vänliga miljonärskan Madame Bonfamille. När den girige betjänten Edgar råkar få höra att att katterna skall få ärva alla pengar, bestämmer han sig för att kidnappa dem!Han söver ner Duchess och hennes ungar, tar med sig dem långt ut på landet och överger dem där...Plötsligt dyker den coola vildkatten Thomas O´Malley upp till deras undsättning.";
        String expectedOutline = StringTools.trimToLength(expectedPlot, 300);

        assertEquals(expectedPlot, movie.getPlot());
        assertEquals(expectedOutline, movie.getOutline());
    }

    @Test
    public void testSetOutline550() {
        LOG.info("testSetOutline550");

        // Set the plot and outline lengths
        PropertiesUtil.setProperty("movie.plot.maxLength", 550);
        PropertiesUtil.setProperty("movie.outline.maxLength", 550);

        Movie movie = new Movie();
        movie.setTitle("Testing outlines-550chars", FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID);
        String plot550 = "<div class=\"text\"><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. <br /> In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, vehicula et mauris. Integer ultrices enim et mi feugiat malesuada. Sed quam ligula, adipiscing non euismod quis, luctus a dui. Maecenas pulvinar dui nec velit aliquam a gravida risus faucibus. Nulla sit amet arcu nisl. <br />Sed pulvinar volutpat erat id.</p>";
        //String expectedOutline550 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.  In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, v...";
        String expectedOutline550 = StringTools.trimToLength("Lorem ipsum dolor sit amet, consectetur adipiscing elit.  In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, vehicula et mauris. Integer ultrices enim et mi feugiat malesuada. Sed quam ligula, adipiscing non euismod quis, luctus a dui. Maecenas pulvinar dui nec velit aliquam a gravida risus faucibus. Nulla sit amet arcu nisl. Sed pulvinar volutpat erat id.", 300);
        toTest.getFilmdeltaPlot(movie, plot550);
        assertEquals(expectedOutline550, movie.getOutline());
    }

    @Test
    public void testSetOutline350() {
        LOG.info("testSetOutline350");

        // Set the plot and outline lengths
        PropertiesUtil.setProperty("movie.plot.maxLength", 350);
        PropertiesUtil.setProperty("movie.outline.maxLength", 300);

        Movie movie = new Movie();
        movie.setTitle("Testing outlines-350chars", FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID);
        String plot350 = "<div class=\"text\"><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. <br /> In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, vehicula et mauris. Integer ultrices enim et mi feugiat malesuada.</p>";
        //String expectedOutline550 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.  In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, v...";
        String expectedOutline300 = StringTools.trimToLength("Lorem ipsum dolor sit amet, consectetur adipiscing elit.  In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus elit, luctus ac mattis eu, vehicula et mauris. Integer ultrices enim et mi feugiat malesuada.", 300);
        toTest.getFilmdeltaPlot(movie, plot350);
        //expect same result as with 550 characters cause outlines are cut at 300 chars
        assertEquals(expectedOutline300, movie.getOutline());
    }

    @Test
    public void testSetOutline250() {
        LOG.info("testSetOutline250");

        // Set the plot and outline lengths
        PropertiesUtil.setProperty("movie.plot.maxLength", 250);
        PropertiesUtil.setProperty("movie.outline.maxLength", 250);

        Movie movie = new Movie();
        movie.setTitle("Testing outlines-250chars", FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID);
        String plot250 = "<div class=\"text\"><p>Testa ipsum dolor sit amet, consectetur adipiscing elit. <br /> In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus eli</p>";
        String expectedOutline250 = "Testa ipsum dolor sit amet, consectetur adipiscing elit.  In ut nisi et nibh scelerisque pellentesque. Duis lacinia, libero et semper feugiat, mi tellus aliquet sapien, ac fringilla lacus ipsum eget mauris. Donec suscipit velit in odio gravida consectetur. Fusce metus eli";
        toTest.getFilmdeltaPlot(movie, plot250);
        assertEquals(expectedOutline250, movie.getOutline());
    }

    @Test
    public void testRemoveIllegalHtmlChars() {
        LOG.info("testRemoveIllegalHtmlChars");
        String test = "\u0093\u0094\u00E4\u00E5\u00F6\u00C4\u00C5\u00D6";
        String result = "&quot;&quot;&auml;&aring;&ouml;&Auml;&Aring;&Ouml;";
        assertEquals(result, toTest.removeIllegalHtmlChars(test));
    }

    /* Create a mock FilmDeltaSEPlugin to test code
     * without depending on getting pages over http
     */
    class FilmDeltaPluginMock extends FilmDeltaSEPlugin {

        private String requestResult;
        private final boolean offline;

        public FilmDeltaPluginMock(boolean offline) {
            this.offline = offline;
        }

        public void init() {
            webBrowser = new WebBrowser() {
                @Override
                public String request(URL url) throws IOException {
                    if (offline && (getRequestResult(url) != null)) {
                        return getRequestResult(url);
                    } else {
                        return super.request(url);
                    }
                }
            };
        }

        public String getRequestResult(URL url) {
            return requestResult;
        }

        public void setRequestResult(String requestResult) {
            this.requestResult = requestResult;
        }
    }
}
