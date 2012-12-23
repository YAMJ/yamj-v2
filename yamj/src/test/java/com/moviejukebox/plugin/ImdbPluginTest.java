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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;

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
