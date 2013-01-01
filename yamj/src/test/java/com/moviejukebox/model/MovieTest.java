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
import junit.framework.TestCase;
import org.junit.Test;

public class MovieTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        PropertiesUtil.setProperty("plugin.people.maxCount.director", "2");
        PropertiesUtil.setProperty("plugin.people.maxCount.writer", "1");
        PropertiesUtil.setProperty("plugin.people.maxCount.actor", "10");
    }

    @Test
    public void testSetPeopleCast() {
        List<String> actors = new ArrayList<String>();
        actors.add("Bruce Willis");
        actors.add("Renée Zellweger");
        actors.add("Lee Majors");
        actors.add("Bolle Pumpernickel");
        actors.add("Dustin Hoffman");
        actors.add("Keanuu Reeves");
        actors.add("Tom Selleck");
        actors.add("Robin Williams");
        actors.add("Free Willy");
        actors.add("Roger Roger");
        actors.add("Humprey Bogart");
        actors.add("Eeen Meene Zwei");
        actors.add("Nobody Else");
        actors.add("Someone Else");

        Movie movie = new Movie();
        movie.setPeopleCast(actors, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_ACTORS);
        assertEquals(10, people.size());
    }

    @Test
    public void testSetPeopleDirectors() {
        List<String> directors = new ArrayList<String>();
        directors.add("This Director");
        directors.add("That Director");
        directors.add("Another Director");

        Movie movie = new Movie();
        movie.setPeopleDirectors(directors, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_DIRECTING);
        assertEquals(2, people.size());
    }

    @Test
    public void testSetPeopleWriters() {
        List<String> directors = new ArrayList<String>();
        directors.add("This Writer");
        directors.add("That Writer");
        directors.add("Another Writer");

        Movie movie = new Movie();
        movie.setPeopleWriters(directors, ImdbPlugin.IMDB_PLUGIN_ID);
        Collection<String> people = movie.getPerson(Filmography.DEPT_WRITING);
        assertEquals(1, people.size());
    }
}
