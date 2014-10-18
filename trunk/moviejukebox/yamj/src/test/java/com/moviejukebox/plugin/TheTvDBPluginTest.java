/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Series;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Stuart
 */
public class TheTvDBPluginTest {

    private static final Logger LOG = Logger.getLogger(TheTvDBPluginTest.class);
    private static TheTvDBPlugin TVDB;

    private static final int ID_BABYLON_5 = 70726;

    public TheTvDBPluginTest() {
        PropertiesUtil.setProperty("mjb.includeVideoImages", true);
        PropertiesUtil.setProperty("mjb.includeEpisodePlots", true);

        TVDB = new TheTvDBPlugin();
    }

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("API_KEY_TheTVDb", "2805AD2873519EC5");
    }

    /**
     * Test of scan method, of class TheTvDBPlugin.
     */
    @Test
    public void testScan() {
        LOG.info("scan");
        Movie movie = new Movie();
        movie.setTitle("Babylon 5", TheTvDBPlugin.THETVDB_PLUGIN_ID);

        boolean result = TVDB.scan(movie);
        assertTrue("Failed to scan", result);
        assertEquals("Wrong title", "Babylon 5", movie.getTitle());
        assertEquals("Wrong ID", String.valueOf(ID_BABYLON_5), movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID));
    }

    /**
     * Test of getBanner method, of class TheTvDBPlugin.
     */
    @Test
    public void testGetBanner() {
        LOG.info("getBanner");
        Movie movie = new Movie();
        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, ID_BABYLON_5);

        String result = TVDB.getBanner(movie);
        assertTrue("No banner returned", result.startsWith("http://thetvdb.com/banners/graphical/70726"));
    }

    /**
     * Test of scanTVShowTitles method, of class TheTvDBPlugin.
     */
    @Test
    public void testScanTVShowTitles() {
        LOG.info("scanTVShowTitles");

        Movie movie = new Movie();
        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, ID_BABYLON_5);

        MovieFile mf1 = new MovieFile();
        mf1.setSeason(1);
        mf1.setFirstPart(1);
        mf1.setLastPart(1);
        movie.addMovieFile(mf1);
        MovieFile mf2 = new MovieFile();
        mf2.setSeason(1);
        mf2.setFirstPart(22);
        mf2.setLastPart(22);
        movie.addMovieFile(mf2);

        TVDB.scanTVShowTitles(movie);

        assertEquals("S01E01: Wrong title", "Midnight on the Firing Line", mf1.getTitle());
        assertEquals("S01E01: Invalid first aired", "1994-01-26", mf1.getFirstAired(mf1.getFirstPart()));
        assertTrue("S01E01: Invalid episode plot", StringTools.isValidString(mf1.getPlot(mf1.getFirstPart())));
        assertTrue("S01E01: Invalid VideoImage", StringTools.isValidString(mf1.getVideoImageURL(mf1.getFirstPart())));
        assertEquals("S01E22: Wrong title", "Chrysalis", mf2.getTitle());
        assertEquals("S01E22: Invalid first aired", "1994-10-26", mf2.getFirstAired(mf2.getFirstPart()));
        assertTrue("S01E22: Invalid episode plot", StringTools.isValidString(mf2.getPlot(mf2.getFirstPart())));
        assertTrue("S01E22: Invalid VideoImage", StringTools.isValidString(mf2.getVideoImageURL(mf2.getFirstPart())));

    }

    /**
     * Test of scanNFO method, of class TheTvDBPlugin.
     */
    @Test
    public void testScanNFO() {
        LOG.info("scanNFO");
        String nfo = "http://www.thetvdb.com/?tab=season&seriesid=70726&seasonid=920&lid=7";
        Movie movie = new Movie();
        assertTrue("Scan failed", TVDB.scanNFO(nfo, movie));
        assertEquals(String.valueOf(ID_BABYLON_5), movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID));

        nfo = "http://www.thetvdb.com/?tab=series&id=70726&lid=7";
        movie = new Movie();
        assertTrue("Scan failed", TVDB.scanNFO(nfo, movie));
        assertEquals(String.valueOf(ID_BABYLON_5), movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID));

    }

    /**
     * Test of getWordseries method, of class TheTvDBPlugin.
     */
    @Test
    public void testGetSeries() {
        LOG.info("getSeries");
        Series result = TheTvDBPlugin.getSeries(String.valueOf(ID_BABYLON_5));

        assertEquals("Wrong Title", "Babylon 5", result.getSeriesName());
        assertTrue("No poster", StringTools.isValidString(result.getPoster()));
        assertTrue("No banner", StringTools.isValidString(result.getBanner()));
        assertTrue("No fanart", StringTools.isValidString(result.getFanart()));
        assertTrue("No Overview", StringTools.isValidString(result.getOverview()));
        assertEquals("Wrong first aired date", "1993-02-22", result.getFirstAired());
        assertEquals("Wrong IMDB ID", "tt0105946", result.getImdbId());
        assertFalse("No Actors", result.getActors().isEmpty());
        assertFalse("No Genres", result.getGenres().isEmpty());
        assertTrue("No rating", StringTools.isValidString(result.getRating()));
        assertEquals("Wrong runtime", "60", result.getRuntime());
    }

    /**
     * Test of findId method, of class TheTvDBPlugin.
     */
    @Test
    public void testFindId() {
        LOG.info("findId");
        Movie movie = new Movie();
        movie.setTitle("Babylon 5", TheTvDBPlugin.THETVDB_PLUGIN_ID);

        String result = TheTvDBPlugin.findId(movie);
        assertEquals("Failed to get ID", String.valueOf(ID_BABYLON_5), result);
    }

    /**
     * Test of getBanners method, of class TheTvDBPlugin.
     */
    @Test
    public void testGetBanners() {
        LOG.info("getBanners");
        Banners result = TheTvDBPlugin.getBanners(String.valueOf(ID_BABYLON_5));

        assertNotNull("Null result", result);
        assertEquals("Invalid ID", 383471, result.getSeriesId());
        assertFalse("No Fanart", result.getFanartList().isEmpty());
        assertFalse("No Posters", result.getPosterList().isEmpty());
        assertFalse("No Seasons", result.getSeasonList().isEmpty());
        assertFalse("No Series", result.getSeriesList().isEmpty());
    }

}
