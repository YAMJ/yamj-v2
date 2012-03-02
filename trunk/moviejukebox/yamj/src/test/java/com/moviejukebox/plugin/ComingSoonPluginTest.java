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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import org.apache.log4j.BasicConfigurator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author iuk
 */

public class ComingSoonPluginTest {

    static {
        PropertiesUtil.setProperty("comingsoon.imdb.scan", "never");
    }

    private ComingSoonPlugin csPlugin = new ComingSoonPlugin();

    public ComingSoonPluginTest() {
        BasicConfigurator.configure();
    }


    @Test
    public void testScanNoYear() {

        Movie movie = new Movie();
        movie.setTitle("L'Incredibile Storia Di Winter Il Delfino");

        assertTrue(csPlugin.scan(movie));
        assertEquals("L'incredibile storia di Winter il delfino", movie.getTitle());
        assertEquals("Dolphin Tale", movie.getOriginalTitle());
        assertEquals("2011", movie.getYear());
        assertTrue(movie.getDirectors().size() > 0);
        assertTrue(movie.getWriters().size() > 0);
        assertTrue(movie.getCast().size() > 0);
        assertTrue(movie.getPlot().length() > 0);
    }

    @Test
    public void testScanList() {
        String[] titleList = {
                        "Matrix",
                        "Gli Aristogatti",
                        "Inception",
                        "L'arte del sogno"
        };

        for (int i = 0; i < titleList.length; i++) {
            Movie movie = new Movie();
            movie.setTitle(titleList[i]);
            assertTrue(csPlugin.scan(movie));
            assertEquals(titleList[i], movie.getTitle());
        }

    }
}
