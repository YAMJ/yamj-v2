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
package com.moviejukebox.tools;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertEquals;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverrideToolsTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(OverrideToolsTest.class);

    @BeforeClass
    public static void configure() {
        doConfiguration();
        loadApiProperties();

        PropertiesUtil.setProperty("mjb.includeEpisodePlots", true);
    }

    @Test
    public void testPriority1() {
        LOG.info("Priority1");
        Movie movie = new Movie();
        if (OverrideTools.checkOverwriteRuntime(movie, Movie.UNKNOWN)) {
            movie.setRuntime("123", Movie.UNKNOWN);
        }
        assertEquals("123", movie.getRuntime());
        if (OverrideTools.checkOverwriteRuntime(movie, "nfo")) {
            movie.setRuntime("456", "nfo");
        }
        assertEquals("456", movie.getRuntime());
        if (OverrideTools.checkOverwriteRuntime(movie, "imdb")) {
            movie.setRuntime("789", "imdb");
        }
        assertEquals("456", movie.getRuntime());
    }

    @Test
    public void testPriority2() {
        LOG.info("Priority2");
        Movie movie = new Movie();
        if (OverrideTools.checkOverwriteRuntime(movie, "nfo")) {
            movie.setRuntime("123", "nfo");
        }
        assertEquals("123", movie.getRuntime());
        if (OverrideTools.checkOverwriteRuntime(movie, Movie.UNKNOWN)) {
            movie.setRuntime("456", Movie.UNKNOWN);
        }
        assertEquals("123", movie.getRuntime());
    }

    @Test
    public void testPriority3() {
        LOG.info("Priority3");
        Movie movie = new Movie();
        if (OverrideTools.checkOverwriteRuntime(movie, "nfo")) {
            movie.setRuntime("123", "nfo");
        }
        assertEquals("123", movie.getRuntime());
        if (OverrideTools.checkOverwriteRuntime(movie, "allocine")) {
            movie.setRuntime("456", "allocine");
        }
        assertEquals("123", movie.getRuntime());
    }

    @Test
    public void testPriority4() {
        LOG.info("Priority4");
        Movie movie = new Movie();
        if (OverrideTools.checkOverwriteRuntime(movie, "imdb")) {
            movie.setRuntime("456", "imdb");
        }
        assertEquals("456", movie.getRuntime());
        if (OverrideTools.checkOverwriteRuntime(movie, "nfo")) {
            movie.setRuntime("123", "nfo");
        }
        assertEquals("123", movie.getRuntime());
    }

    @Test
    public void testFilePriority1() {
        LOG.info("FilePriority1");
        MovieFile file = new MovieFile();
        if (OverrideTools.checkOverwriteEpisodePlot(file, 1, "imdb")) {
            file.setPlot(1, "123", "imdb");
        }
        assertEquals("123", file.getPlot(1));
        if (OverrideTools.checkOverwriteEpisodePlot(file, 1, "nfo")) {
            file.setPlot(1, "456", "nfo");
        }
        assertEquals("456", file.getPlot(1));
    }

    @Test
    public void testFilePriority2() {
        LOG.info("FilePriority2");
        MovieFile file = new MovieFile();
        if (OverrideTools.checkOverwriteEpisodePlot(file, 1, "nfo")) {
            file.setPlot(1, "123", "nfo");
        }
        assertEquals("123", file.getPlot(1));
        if (OverrideTools.checkOverwriteEpisodePlot(file, 1, "imdb")) {
            file.setPlot(1, "456", "imdb");
        }
        assertEquals("123", file.getPlot(1));
    }
}
