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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.PropertiesUtil;

public class ImdbPluginTest {

    public ImdbPluginTest() {
        BasicConfigurator.configure();
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
    }

    @Test
    public void testImdbTvShow() {
        PropertiesUtil.setProperty("imdb.site", "us");
        PropertiesUtil.setProperty("mjb.includeEpisodePlots", true);
        PropertiesUtil.setProperty("imdb.full.info", true);
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_TVSHOW);
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0369179");

        MovieFile mf1 = new MovieFile();
        mf1.setSeason(8);
        mf1.setFirstPart(12);
        mf1.setLastPart(12);
        movie.addMovieFile(mf1);
        MovieFile mf2 = new MovieFile();
        mf2.setSeason(8);
        mf2.setFirstPart(14);
        mf2.setLastPart(14);
        movie.addMovieFile(mf2);

        assertTrue(imdbPlugin.scan(movie));
        assertEquals("Chocolate Diddlers or My Puppy's Dead", mf1.getTitle(12));
        assertTrue(StringUtils.startsWith(mf1.getPlot(12), "When Charlie and Courtney break up"));
        assertEquals("2010-12-13", mf1.getFirstAired(12));
        assertEquals("Lookin' for Japanese Subs", mf2.getTitle(14));
        assertTrue(StringUtils.startsWith(mf2.getPlot(14), "Charlie continues to obsess over Rose"));
        assertEquals("2011", mf2.getFirstAired(14));
    }

    @Test
    public void testImdbMovieGeoLocalization() {
        PropertiesUtil.setProperty("imdb.site", "us");
        PropertiesUtil.setProperty("imdb.preferredCountry", "U.S.A.");
        PropertiesUtil.setProperty("imdb.aka.scrape.title", true);
        PropertiesUtil.setProperty("imdb.aka.ignore.versions", "IMAX version,longer title,promotional title,working title,version IMAX,Arbeitstitel,Titel zu Werbezwecken,IMAX Fassung,längere Fassung,version longue,titre promotionnel,titre provisoire");
        PropertiesUtil.setProperty("imdb.aka.fallback.countries", "Taiwan");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");
        assertTrue(imdbPlugin.scan(movie));
        assertEquals("Afanda", movie.getTitle());
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
    public void testImdb_NewLayout() {
        PropertiesUtil.setProperty("imdb.site", "us");
        PropertiesUtil.setProperty("imdb.full.info", "false");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");
        
        assertTrue(imdbPlugin.scan(movie));
        assertEquals("2009", movie.getYear());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
        assertEquals(80, movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID));
        assertEquals("USA", movie.getCountry());
        assertEquals("Twentieth Century Fox Film Corporation", movie.getCompany());
        assertEquals("Enter the World", movie.getTagline());
        assertEquals(10, movie.getCast().size());
        assertEquals(1, movie.getDirectors().size());
        assertEquals(1, movie.getWriters().size());
    }

    @Test
    public void testImdb_Combined() {
        PropertiesUtil.setProperty("imdb.site", "us");
        PropertiesUtil.setProperty("imdb.full.info", "true");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");
        
        assertTrue(imdbPlugin.scan(movie));
        assertEquals("2009", movie.getYear());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
        assertEquals(80, movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID));
        assertEquals("USA", movie.getCountry());
        assertEquals("Twentieth Century Fox Film Corporation", movie.getCompany());
        assertEquals("Enter the World", movie.getTagline());
        assertEquals(10, movie.getCast().size());
        assertEquals(1, movie.getDirectors().size());
        assertEquals(1, movie.getWriters().size());
    }
}
