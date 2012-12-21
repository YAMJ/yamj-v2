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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 *
 * @author iuk
 */

public class ComingSoonPluginTest {

    static {
        PropertiesUtil.setProperty("comingsoon.imdb.scan", "nevwe");
        PropertiesUtil.setProperty("priority.title", "comingsoon,imdb");
        PropertiesUtil.setProperty("priority.originaltitle", "comingsoon,imdb");
    }

    private ComingSoonPlugin csPlugin = new ComingSoonPlugin();

    public ComingSoonPluginTest() {
        BasicConfigurator.configure();
    }


    @Test
    public void testScanNoYear() {

        Movie movie = new Movie();
        movie.setTitle("L'Incredibile Storia Di Winter Il Delfino", csPlugin.getPluginID());

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
                        "L'arte del sogno",
                        "Lettere da Iwo Jima"
        };

        for (int i = 0; i < titleList.length; i++) {
            Movie movie = new Movie();
            movie.setTitle(titleList[i], csPlugin.getPluginID());
            assertTrue(csPlugin.scan(movie));
            assertEquals(titleList[i], movie.getTitle());
            assertTrue(movie.getDirectors().size() > 0);
            assertTrue(movie.getWriters().size() > 0);
            if (i != 1) {
                assertTrue(movie.getCast().size() > 0);
                assertTrue(StringTools.isValidString(movie.getReleaseDate()));

            }
            assertTrue(StringTools.isValidString(movie.getPlot()));
            assertTrue(StringTools.isValidString(movie.getYear()));
            assertTrue(StringTools.isValidString(movie.getRuntime()));
            assertTrue(StringTools.isValidString(movie.getCountry()));
            assertTrue(movie.getRating() > -1);
            
        }

    }
}
