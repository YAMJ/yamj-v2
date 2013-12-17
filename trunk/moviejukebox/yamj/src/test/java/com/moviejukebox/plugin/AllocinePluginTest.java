/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AllocinePluginTest {

    private AllocinePlugin allocinePlugin;
    private static final Logger LOG = Logger.getLogger(AllocinePluginTest.class);

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("mjb.internet.plugin", "com.moviejukebox.plugin.AllocinePlugin");
        PropertiesUtil.setProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.AllocinePlugin");
        PropertiesUtil.setProperty("mjb.includeEpisodePlots", true);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setup() {
        allocinePlugin = new AllocinePlugin();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMovie() {
        LOG.info("testMovie");
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_MOVIE);
        movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, "45322");
        movie.setId(AllocinePlugin.IMDB_PLUGIN_ID, "tt0320691");

        allocinePlugin.scan(movie);
        assertEquals("Underworld", movie.getTitle());
        assertEquals("12", movie.getCertification());
    }

    @Test
    public void testTvSeries() {
        LOG.info("testTvSeries");
        // Change the override priorities for allocine
        OverrideTools.putTvPriorities(OverrideFlag.TITLE, "allocine,imdb");
        OverrideTools.putTvPriorities(OverrideFlag.ORIGINALTITLE, "allocine,imdb");
        OverrideTools.putTvPriorities(OverrideFlag.PLOT, "allocine,imdb");
        OverrideTools.putTvPriorities(OverrideFlag.OUTLINE, "allocine,imdb");

        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_TVSHOW);
        movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, "5676");
        movie.setId(AllocinePlugin.IMDB_PLUGIN_ID, "tt1634549");
        MovieFile mf = new MovieFile();
        mf.setSeason(1);
        mf.setFirstPart(1);
        mf.setLastPart(1);
        movie.addMovieFile(mf);

        assertTrue("Failed to scan correctly", allocinePlugin.scan(movie));
        assertEquals("Incorrect series title", "Millennium", movie.getOriginalTitle());
        assertFalse("No video files found", movie.getFiles().isEmpty());

        mf = movie.getFirstFile();
        assertTrue(StringUtils.isNotBlank(mf.getPlot(1)));
        String plotStart = "Chaque année depuis quarante-quatre ans, le jour de son anniversaire, le président d'un ";
        assertEquals(plotStart, mf.getPlot(1).substring(0, plotStart.length()));
    }
}
