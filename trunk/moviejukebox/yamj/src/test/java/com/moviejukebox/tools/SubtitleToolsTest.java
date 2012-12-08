/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.tools;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieFilenameScanner;

public class SubtitleToolsTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        // set up the file name scanner
        MovieFilenameScanner.clearLanguages();
        MovieFilenameScanner.addLanguage("English", "ENG EN ENGLISH eng en english Eng", "ENG EN ENGLISH");
        MovieFilenameScanner.addLanguage("German", "GER,DE,GERMAN,ger,de ,german,Ger", "GER,DE,GERMAN");
        MovieFilenameScanner.addLanguage("French", "FRA FR FRENCH VF fra fr french vf Fra", "FRA FR FRENCH");
        MovieFilenameScanner.addLanguage("Italian", "ITA IT ITALIAN ita it italian Ita", "ITA IT ITALIAN");
        MovieFilenameScanner.addLanguage("Norwegian", "NOR NORWEGIAN nor norwegian Norwegian", "NOR NORWEGIAN");

        // set property for subtitle restriction
        PropertiesUtil.setProperty("mjb.subtitle.skip", "nor,it");
    }
    
    public void testAddMovieSubtitles1() {
        String actualSubtitles = Movie.UNKNOWN;
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "eng");
        assertEquals("English", newSubtitles);
    }

    public void testAddMovieSubtitles2() {
        String actualSubtitles = "NO";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "eng");
        assertEquals("English", newSubtitles);
    }

    public void testAddMovieSubtitles3() {
        String actualSubtitles = "YES";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "eng");
        newSubtitles = SubtitleTools.addMovieSubtitle(newSubtitles, "de");
        assertEquals("English / German", newSubtitles);
    }

    public void testAddMovieSubtitles4() {
        String actualSubtitles = "YES";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "NO");
        assertEquals("NO", newSubtitles);
    }

    public void testAddMovieSubtitles5() {
        String actualSubtitles = "NO";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "YES");
        assertEquals("YES", newSubtitles);
    }

    public void testAddMovieSubtitles6() {
        String actualSubtitles = "English / German";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "YES");
        assertEquals("English / German", newSubtitles);
    }

    public void testAddMovieSubtitles7() {
        String actualSubtitles = "English / German";
        String newSubtitles = SubtitleTools.addMovieSubtitle(actualSubtitles, "German");
        assertEquals("English / German", newSubtitles);
    }

    public void testSetMovieSubtitles1() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add("");
        subtitles.add("en");
        subtitles.add("de");
        subtitles.add("German");
        subtitles.add("English");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    public void testSetMovieSubtitles2() {
        List<String> subtitles = new ArrayList<String>();
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

    public void testSetMovieSubtitles3() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add("en");
        subtitles.add("de");

        Movie movie = new Movie();
        movie.setSubtitles("NO");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    public void testSetMovieSubtitles4() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add("en");
        subtitles.add("de");

        Movie movie = new Movie();
        movie.setSubtitles("YES");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }

    public void testSetMovieSubtitlesOverride1() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add("en");

        Movie movie = new Movie();
        movie.setSubtitles("German / French");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English", movie.getSubtitles());
    }

    public void testSetMovieSubtitlesOverride2() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add(Movie.UNKNOWN);
        subtitles.add("");
        subtitles.add("    ");
        
        Movie movie = new Movie();
        movie.setSubtitles("German / French");
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("German / French", movie.getSubtitles());
    }

    public void testSkippedMovieSubtitles() {
        List<String> subtitles = new ArrayList<String>();
        subtitles.add("en");
        subtitles.add("it");
        subtitles.add("de");
        subtitles.add("nor");
        subtitles.add("ger");

        Movie movie = new Movie();
        SubtitleTools.setMovieSubtitles(movie, subtitles);
        assertEquals("English / German", movie.getSubtitles());
    }
}
