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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import org.apache.log4j.BasicConfigurator;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ImdbPluginTest {
	public ImdbPluginTest() {
		BasicConfigurator.configure();
	}

	@Test
	public void testImdbMovie() {
		ImdbPlugin imdbPlugin = new ImdbPlugin();
		Movie movie = new Movie();
		movie.setYear("2012", null);
		movie.setTitle("Skyfall", null);

		assertTrue(imdbPlugin.scan(movie));

		assertNotNull(movie.getPlot());

		assertNotEquals(Movie.UNKNOWN, movie.getPlot());

	}

	@Test
	public void testImdbPerson() {
		ImdbPlugin imdbPlugin = new ImdbPlugin();
		Person person = new Person();
		person.setName("Daniel Craig");

		assertTrue(imdbPlugin.scan(person));

		assertNotNull(person.getBiography());

		assertNotEquals(Movie.UNKNOWN, person.getBiography());

	}
}
