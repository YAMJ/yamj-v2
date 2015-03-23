/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.reader;

import static org.junit.Assert.assertEquals;

import com.moviejukebox.model.Movie;
import org.junit.Test;

/**
 *
 * @author Stuart
 */
public class MovieNFOReaderTest {

    @Test
    public void testMovieDate() {
        System.out.println("testMovieDate");
        String dateTest = "20/10/2005";

        Movie m = new Movie();
        MovieNFOReader.movieDate(m, dateTest);

        assertEquals("Wrong release year", "2005", m.getYear());
        assertEquals("Wrong release date", "2005-10-20", m.getReleaseDate());

        dateTest = "10/20/2005";
        MovieNFOReader.movieDate(m, dateTest);

        assertEquals("Wrong release year", "2005", m.getYear());
        assertEquals("Wrong release date", "2005-10-20", m.getReleaseDate());

    }

    @Test
    public void testParsCrew() {
        StringBuilder nfo = new StringBuilder();
        nfo.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        nfo.append("<movie>");
        nfo.append("<id>-1</id>");
        nfo.append("<id moviedb=\"tmdb\">24021</id>");
        nfo.append("<title>The Twilight Saga: Eclipse</title>");
        nfo.append("<year>2010</year>");
        nfo.append("<credits>");
        nfo.append("<writer>Melissa Rosenberg</writer>");
        nfo.append("<writer>Stephenie Meyer</writer>");
        nfo.append("</credits>");
        nfo.append("<credits>Hans Dampf</credits>");
        nfo.append("<director>David Slade</director>");
        nfo.append("</movie>");

        Movie movie = new Movie();
        MovieNFOReader.readXmlNfo(nfo.toString(), movie, "test.nfo");
        assertEquals(1, movie.getDirectors().size());
        assertEquals(3, movie.getWriters().size());
    }
}
