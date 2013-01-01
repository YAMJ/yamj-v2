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
package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;
import junit.framework.TestCase;

public class OverrideToolsTest extends TestCase {

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
