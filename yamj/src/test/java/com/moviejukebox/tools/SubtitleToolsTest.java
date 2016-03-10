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
import com.moviejukebox.scanner.MovieFilenameScanner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubtitleToolsTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(SubtitleToolsTest.class);

    @BeforeClass
    public static void configure() {
        doConfiguration();
        PropertiesUtil.setProperty("mjb.subtitle.skip", "nor,it");
    }

    @Before
    public void setUp() {
        // set up the file name scanner
        MovieFilenameScanner.clearLanguages();
        MovieFilenameScanner.addLanguage("English", "ENG EN ENGLISH eng en english Eng", "ENG EN ENGLISH");
        MovieFilenameScanner.addLanguage("German", "GER,DE,GERMAN,ger,de ,german,Ger", "GER,DE,GERMAN");
        MovieFilenameScanner.addLanguage("French", "FRA FR FRENCH VF fra fr french vf Fra", "FRA FR FRENCH");
        MovieFilenameScanner.addLanguage("Italian", "ITA IT ITALIAN ita it italian Ita", "ITA IT ITALIAN");
        MovieFilenameScanner.addLanguage("Norwegian", "NOR NORWEGIAN nor norwegian Norwegian", "NOR NORWEGIAN");
    }

    @Test
    public void testAddMovieSubtitles1() {
        LOG.info("AddMovieSubtitles1");
        String actualSubtitles = Movie.UNKNOWN;
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "eng");
        assertEquals("English", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles2() {
        LOG.info("AddMovieSubtitles2");
        String actualSubtitles = "NO";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "eng");
        assertEquals("English", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles3() {
        LOG.info("AddMovieSubtitles3");
        String newSubtitles = SubtitleTools.addMovieSubtitle("YES", "eng");
        newSubtitles = SubtitleTools.addMovieSubtitle(newSubtitles, "de");
        assertEquals("English / German", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles4() {
        LOG.info("AddMovieSubtitles4");
        String actualSubtitles = "YES";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "NO");
        assertEquals("NO", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles5() {
        LOG.info("AddMovieSubtitles5");
        String actualSubtitles = "NO";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "YES");
        assertEquals("YES", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles6() {
        LOG.info("AddMovieSubtitles6");
        String actualSubtitles = "English / German";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "YES");
        assertEquals("English / German", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles7() {
        LOG.info("AddMovieSubtitles7");
        String actualSubtitles = "English / German";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "German");
        assertEquals("English / German", newSubtitles);
    }

    @Test
    public void testAddMovieSubtitles8() {
        LOG.info("AddMovieSubtitles8");
        String subs = SubtitleTools.addMovieSubtitle("", "en");
        subs = SubtitleTools.addMovieSubtitle(subs, "german");
        subs = SubtitleTools.addMovieSubtitle(subs, "de");
        assertEquals("English / German", subs);
    }

    @Test
    public void testSetMovieSubtitles1() {
        LOG.info("SetMovieSubtitles1");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("");
        subtitles.add("en");
        subtitles.add("de");
        subtitles.add("German");
        subtitles.add("English");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    @Test
    public void testSetMovieSubtitles2() {
        LOG.info("SetMovieSubtitles2");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("");
        subtitles.add("en");
        subtitles.add("de");
        subtitles.add("German");
        subtitles.add("English");
        subtitles.add("YES");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    @Test
    public void testSetMovieSubtitles3() {
        LOG.info("SetMovieSubtitles3");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("en");
        subtitles.add("de");

        Movie movie = new Movie();
        movie.setSubtitles("NO");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    @Test
    public void testSetMovieSubtitles4() {
        LOG.info("SetMovieSubtitles4");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("en");
        subtitles.add("de");

        Movie movie = new Movie();
        movie.setSubtitles("YES");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    @Test
    public void testSetMovieSubtitlesOverride1() {
        LOG.info("SetMovieSubtitlesOverride1");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("en");

        Movie movie = new Movie();
        movie.setSubtitles("German / French");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English", movie.getSubtitles());
    }

    @Test
    public void testSetMovieSubtitlesOverride2() {
        LOG.info("SetMovieSubtitlesOverride2");
        List<String> subtitles = new ArrayList<>();
        subtitles.add(Movie.UNKNOWN);
        subtitles.add("");
        subtitles.add("    ");

        Movie movie = new Movie();
        movie.setSubtitles("German / French");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("German / French", movie.getSubtitles());
    }

    @Test
    public void testSkippedMovieSubtitles() {
        LOG.info("SkippedMovieSubtitles");
        List<String> subtitles = new ArrayList<>();
        subtitles.add("en");
        subtitles.add("it");
        subtitles.add("de");
        subtitles.add("nor");
        subtitles.add("ger");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    @Test
    public void tesGetMovieSubtitles() {
        List<String> subtitles = new ArrayList<>();
        subtitles.add("en");
        subtitles.add("it");
        subtitles.add("de");
        subtitles.add("nor");
        subtitles.add("ger");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals(2, SubtitleTools.getSubtitles(movie).size());
    }
}
