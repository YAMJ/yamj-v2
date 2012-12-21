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

import junit.framework.TestCase;

import com.moviejukebox.model.Movie;

public class OverrideToolsTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        // set property for runtime priority
        PropertiesUtil.setProperty("priority.checks.enable", "true");
        PropertiesUtil.setProperty("priority.runtime", "mediainfo,nfo,imdb");
    }
    
    public void testPriority1() {
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

    public void testPriority2() {
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

    public void testPriority3() {
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

    public void testPriority4() {
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
}
