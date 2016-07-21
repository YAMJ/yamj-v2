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
package com.moviejukebox.scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.moviejukebox.AbstractTests;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.ImdbPluginTest;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
@SuppressWarnings("unused")
public class WatchedScannerTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbPluginTest.class);

    @BeforeClass
    public static void configure() {
        doConfiguration();
        loadApiProperties();
        PropertiesUtil.setProperty("watched.scanner.enable", "false");
        PropertiesUtil.setProperty("watched.trakttv.enable", "true");
    }

    @Test
    public void testMovieWatched() {
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_MOVIE);
        movie.setId(TheMovieDbPlugin.TMDB_PLUGIN_ID, "19995");
        MovieFile mf = new MovieFile();
        mf.setFirstPart(1);
        mf.setLastPart(1);
        mf.setFile(new File("test.mkv"));
        movie.addMovieFile(mf);
                        
        WatchedScanner.checkWatched(null, movie);
        assertTrue(movie.isWatchedFile());
    }

    @Test
    public void testEpisodePartiallyWatched() {
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_TVSHOW);
        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, "261202");
        MovieFile mf1 = new MovieFile();
        mf1.setSeason(1);
        mf1.setFirstPart(1);
        mf1.setLastPart(1);
        mf1.setFile(new File("test1.mkv"));
        movie.addMovieFile(mf1);
        MovieFile mf2 = new MovieFile();
        mf2.setSeason(1);
        mf2.setFirstPart(100);
        mf2.setLastPart(100);
        mf2.setFile(new File("test2.mkv"));
        movie.addMovieFile(mf2);
                        
        WatchedScanner.checkWatched(null, movie);
        assertTrue(mf1.isWatched());
        assertFalse(mf2.isWatched());
        assertFalse(movie.isWatchedFile());
    }

    @Test
    public void testEpisodeFullyWatched() {
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_TVSHOW);
        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, "261202");
        MovieFile mf1 = new MovieFile();
        mf1.setSeason(1);
        mf1.setFirstPart(1);
        mf1.setLastPart(1);
        mf1.setFile(new File("test1.mkv"));
        movie.addMovieFile(mf1);
        MovieFile mf2 = new MovieFile();
        mf2.setSeason(1);
        mf2.setFirstPart(2);
        mf2.setLastPart(2);
        mf2.setFile(new File("test2.mkv"));
        movie.addMovieFile(mf2);
                        
        WatchedScanner.checkWatched(null, movie);
        assertTrue(mf1.isWatched());
        assertTrue(mf2.isWatched());
        assertTrue(movie.isWatchedFile());
    }
}
