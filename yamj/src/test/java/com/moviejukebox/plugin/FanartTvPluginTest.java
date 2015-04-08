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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.omertron.fanarttvapi.model.FTMovie;
import com.omertron.fanarttvapi.model.FTSeries;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FanartTvPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(FanartTvPluginTest.class);
    private static FanartTvPlugin ft;

    public FanartTvPluginTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");

        PropertiesUtil.setProperty("clearart.tv.download", true);
        PropertiesUtil.setProperty("clearlogo.tv.download", true);
        PropertiesUtil.setProperty("seasonthumb.tv.download", true);
        PropertiesUtil.setProperty("tvthumb.tv.download", true);
        PropertiesUtil.setProperty("movieart.movie.download", true);
        PropertiesUtil.setProperty("moviedisc.movie.download", true);
        PropertiesUtil.setProperty("movielogo.movie.download", true);

        ft = new FanartTvPlugin();
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

    /**
     * Test of scan method with movie
     */
    @Test
    public void testScan_Movie() {
        LOG.info("scan (movie)");
        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");
        movie.setId(TheMovieDbPlugin.TMDB_PLUGIN_ID, "19995");
        movie.setBaseName("TEST - Avatar");

        boolean expResult = true;
        boolean result = ft.scan(movie);
        assertEquals("Failed to scan for fanart", expResult, result);
        assertTrue("No MovieArt found", StringTools.isValidString(movie.getClearArtURL()));
        assertTrue("No MovieLogo found", StringTools.isValidString(movie.getClearLogoURL()));
        assertTrue("No MovieDisc found", StringTools.isValidString(movie.getMovieDiscURL()));
    }

    /**
     * Test of scan method with TV show
     */
    @Test
    public void testScan_TV() {
        LOG.info("scan (TV)");
        Movie movie = new Movie();
        movie.setBaseName("TEST - Walking Dead");
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt1520211");
        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, "153021");
        MovieFile mf = new MovieFile();
        mf.setSeason(1);
        mf.setFirstPart(1);
        mf.setLastPart(1);
        movie.addMovieFile(mf);

        boolean expResult = true;
        boolean result = ft.scan(movie);
        assertEquals("Failed to scan for fanart", expResult, result);
        assertTrue("No ClearArt found", StringTools.isValidString(movie.getClearArtURL()));
        assertTrue("No ClearLogo found", StringTools.isValidString(movie.getClearLogoURL()));
        assertTrue("No TVThumb found", StringTools.isValidString(movie.getTvThumbURL()));
        assertTrue("No SeasonThumbfound", StringTools.isValidString(movie.getSeasonThumbURL()));
    }

    /**
     * Test of getTvArtwork method, of class FanartTvPlugin.
     */
    @Test
    public void testGetTvArtwork() {
        LOG.info("getTvArtwork");
        // The IT Crowd
        int tvdbId = 79216;
        FTSeries result = ft.getTvArtwork(tvdbId);
        assertEquals("Wrong results found", "The IT Crowd", result.getName());
    }

    /**
     * Test of getMovieArtwork method, of class FanartTvPlugin.
     */
    @Test
    public void testGetMovieArtwork() {
        LOG.info("getMovieArtwork");
        // Blade Runner
        int tmdbId = 78;
        String imdbId = "tt0083658";

        // Search using tmdb
        FTMovie result = ft.getMovieArtwork(tmdbId, null);
        assertEquals("Wrong result found","Blade Runner", result.getName());
        assertEquals("Incorrect IMDB ID", imdbId, result.getImdbId());

        result = ft.getMovieArtwork(0, imdbId);
        assertEquals("Wrong result found","Blade Runner", result.getName());
        assertEquals("Incorrect TMDB ID", Integer.toString(tmdbId), result.getTmdbId());
    }
}
