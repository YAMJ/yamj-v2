/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.moviejukebox.model.Library.Index;
import com.moviejukebox.tools.PropertiesUtil;


public class LibraryTest extends TestCase {

    Library lib;
    final List<Movie> movies = new ArrayList<Movie>();

    @Override
    protected void setUp() throws Exception {
        // TODO Artem: Decouple library from config.
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        lib = new Library();
        lib.clear();

        movies.add(tv("The Sopranos", 1));
        movies.add(tv("The Sopranos", 2));
        movies.add(tv("The Sopranos", 3));
        movies.add(tv("The Sopranos", 4));
        movies.add(tv("M*A*S*H", 1));
        movies.add(tv("M*A*S*H", 2));
        movies.add(movie("Shrek"));
        movies.add(tv("Star Trek", 3));
        movies.add(tv("Star Trek", 7));
        movies.add(tv("Star Trek", 55));
        movies.add(movie("Shrek 2"));
        movies.add(tv("Doctor Who", 345));

        for (Movie movie : movies) {
            lib.addMovie(movie);
        }
    }

    public void testIndex() {
        Index index = new Index();
        assertEquals(0, index.size());
        add10Movies(index);
        assertEquals(10, index.size());
        index.clear();
        assertEquals(0, index.size());
        index.setMaxCategories(5);
        add10Movies(index);
        assertEquals(5, index.size());
    }

    private void add10Movies(Index index) {
        for (int i = 0; i < 10; i++) {
            index.addMovie("i" + i, new Movie());
        }
    }

    public void testIndexByTVShowSeasons() {
        Library.setSingleSeriesPage(true);
        Index index = Library.indexBySets(movies);
        assertEquals(4, index.size());
        assertTrue(index.containsKey("The Sopranos"));
        assertTrue(index.containsKey("Star Trek"));
        assertTrue(index.containsKey("M*A*S*H"));
        assertEquals(4, index.get("The Sopranos").size());
    }

    private static Movie tv(String title, int season) {
        Movie m = new Movie();
        m.setTitle(title);
        m.setSeason(season);
        MovieFile mf = new MovieFile();
        m.addMovieFile(mf);
        return m;
    }

    private static Movie movie(String title) {
        return tv(title, -1);
    }
}
