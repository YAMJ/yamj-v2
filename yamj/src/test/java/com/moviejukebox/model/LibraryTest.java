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
package com.moviejukebox.model;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LibraryTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryTest.class);
    private static final Library LIBRARY = new Library();
    private static final List<Movie> MOVIES = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        doConfiguration();
        loadMainProperties();

        MOVIES.add(tv("The Sopranos", 1));
        MOVIES.add(tv("The Sopranos", 2));
        MOVIES.add(tv("The Sopranos", 3));
        MOVIES.add(tv("The Sopranos", 4));
        MOVIES.add(tv("M*A*S*H", 1));
        MOVIES.add(tv("M*A*S*H", 2));
        MOVIES.add(movie("Shrek"));
        MOVIES.add(tv("Star Trek", 3));
        MOVIES.add(tv("Star Trek", 7));
        MOVIES.add(tv("Star Trek", 55));
        MOVIES.add(movie("Shrek 2"));
        MOVIES.add(tv("Doctor Who", 345));

        for (Movie movie : MOVIES) {
            LIBRARY.addMovie(movie);
        }
    }

    @Test
    public void testIndex() {
        LOG.info("testIndex");
        Index index = new Index();
        assertEquals(0, index.size());
        addMovies(index, 10);
        assertEquals(10, index.size());
        index.clear();
        assertEquals(0, index.size());
        index.setMaxCategories(5);
        addMovies(index, 10);
        assertEquals(5, index.size());
    }

    @Test
    public void testIndexByTVShowSeasons() {
        LOG.info("testIndexByTVShowSeasons");
        Library.setSingleSeriesPage(true);
        Index index = Library.indexBySets(MOVIES);
        assertEquals(4, index.size());
        assertTrue(index.containsKey("The Sopranos"));
        assertTrue(index.containsKey("Star Trek"));
        assertTrue(index.containsKey("M*A*S*H"));
        assertEquals(4, index.get("The Sopranos").size());
    }

    private static void addMovies(Index index, int number) {
        for (int i = 0; i < number; i++) {
            index.addMovie("i" + i, new Movie());
        }
    }

    private static Movie tv(String title, int season) {
        Movie movie = new Movie();
        movie.setTitle(title, Movie.UNKNOWN);
        MovieFile mf = new MovieFile();
        mf.setSeason(season);
        mf.setFirstPart(1);
        movie.addMovieFile(mf);
        return movie;
    }

    private static Movie movie(String title) {
        return tv(title, -1);
    }
}
