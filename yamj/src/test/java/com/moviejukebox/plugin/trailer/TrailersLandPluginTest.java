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
package com.moviejukebox.plugin.trailer;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

/**
 *
 * @author iuk
 */

public class TrailersLandPluginTest {

    static {
        PropertiesUtil.setProperty("trailers.download", "false");
    }

    private TrailersLandPlugin tlPlugin = new TrailersLandPlugin();

    public TrailersLandPluginTest() {
        BasicConfigurator.configure();
    }

    @Test
    public void testGenerate1() {

        Movie movie = new Movie();
        movie.setTitle("Paradiso Amaro");
        movie.setOriginalTitle("The Descendants");
        
        assertTrue(tlPlugin.generate(movie));
    }
    
    @Test
    public void testGenerate2() {

        Movie movie = new Movie();
        movie.setTitle("Bar Sport");
        
        assertTrue(tlPlugin.generate(movie));
    }

}
