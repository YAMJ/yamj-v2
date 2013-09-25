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
package com.moviejukebox.model;

import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class MovieTest {

    private static int actorMax = 10;
    private static int directorMax = 2;
    private static int writerMax = 3;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetPeopleCast() {
        List<String> actors = createList("Actor", actorMax);

        Movie movie = new Movie();
        movie.setPeopleCast(actors, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_ACTORS);
        assertEquals("Wrong number of actors returned", actorMax, people.size());
    }

    @Test
    public void testSetPeopleDirectors() {
        List<String> directors = createList("Director", directorMax);

        Movie movie = new Movie();
        movie.setPeopleDirectors(directors, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_DIRECTING);
        assertEquals("Wrong number of directors returned", directorMax, people.size());
    }

    @Test
    public void testSetPeopleWriters() {
        List<String> writers = createList("Writer", writerMax);

        Movie movie = new Movie();
        movie.setPeopleWriters(writers, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_WRITING);
        assertEquals("Wrong number of writers returned", writerMax, people.size());
    }

    private List<String> createList(String title, int count) {
        List<String> testList = new ArrayList<String>(count);

        for (int i = 1; i <= count + 2; i++) {
            testList.add(String.format("%s %d", title, i));
        }

        return testList;
    }
}
