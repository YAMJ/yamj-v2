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
import com.moviejukebox.tools.PropertiesUtil;

import org.apache.log4j.BasicConfigurator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ImdbPluginTest {

    public ImdbPluginTest() {
        BasicConfigurator.configure();
    }

    @Test
    public void testImdbMoviePlot() {
        PropertiesUtil.setProperty("imdb.site", "us");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setYear("2012", null);
        movie.setTitle("Skyfall", null);

        assertTrue(imdbPlugin.scan(movie));
        assertNotNull(movie.getPlot());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
    }

    @Test
    public void testImdbMoviePlotLong() {
        PropertiesUtil.setProperty("imdb.site", "us");
        PropertiesUtil.setProperty("imdb.plot", "long");
        PropertiesUtil.setProperty("imdb.full.info", true);
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt1515091");

        assertTrue(imdbPlugin.scan(movie));
        assertNotNull(movie.getPlot());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
        System.err.println(movie.getPlot());
    }

    @Test
    public void testImdbMovieGeoLocalization() {
        PropertiesUtil.setProperty("imdb.site", "es");
        PropertiesUtil.setProperty("imdb.preferredCountry", "USA");
        PropertiesUtil.setProperty("imdb.aka.scrape.title", true);
        PropertiesUtil.setProperty("imdb.aka.ignore.versions", "IMAX version,longer title,promotional title,working title,version IMAX,Arbeitstitel,Titel zu Werbezwecken,IMAX Fassung,längere Fassung,version longue,titre promotionnel,titre provisoire");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");

        assertTrue(imdbPlugin.scan(movie));
        assertNotNull(movie.getPlot());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
    }

    @Test
    public void testImdbMovieValues() {
        PropertiesUtil.setProperty("imdb.site", "us");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0120737");

        assertTrue(imdbPlugin.scan(movie));
        assertEquals("New Zealand", movie.getCountry());
        assertEquals("New Line Cinema", movie.getCompany());
    }

    @Test
    public void testImdbPerson() {
        PropertiesUtil.setProperty("imdb.site", "us");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Person person = new Person();
        person.setName("Daniel Craig");

        assertTrue(imdbPlugin.scan(person));
        assertNotNull(person.getBiography());
        assertNotEquals(Movie.UNKNOWN, person.getBiography());
    }

    @Test
    public void testImdbRating() {
        PropertiesUtil.setProperty("imdb.site", "us");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt1232829");
        
        assertTrue(imdbPlugin.scan(movie));
        assertEquals(72, movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID));
    }
}
