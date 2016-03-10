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
package com.moviejukebox.plugin;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.*;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbPluginTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbPluginTest.class);

    @BeforeClass
    public static void configure() {
        doConfiguration();
        loadMainProperties();
        loadApiProperties();
    }

    @Test
    public void testImdbMoviePlotLong() {
        LOG.info("testImdbMoviePlotLong");
        PropertiesUtil.setProperty("imdb.plot", "long");
        PropertiesUtil.setProperty("imdb.full.info", true);
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt1515091");
        movie.setBaseName("Test_Movie_Avatar");

        assertTrue(imdbPlugin.scan(movie));
        assertNotNull(movie.getPlot());
        assertNotEquals(Movie.UNKNOWN, movie.getPlot());
    }

    @Test
    public void testImdbTvShow() {
        LOG.info("testImdbTvShow");
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
        mf2.setFirstPart(16);
        mf2.setLastPart(16);
        movie.addMovieFile(mf2);

        assertTrue(imdbPlugin.scan(movie));
        assertEquals("Chocolate Diddlers or My Puppy's Dead", mf1.getTitle(12));
        assertTrue(StringUtils.startsWith(mf1.getPlot(12), "When Charlie and Courtney break up"));
        assertEquals("2011-11-15", mf1.getFirstAired(12));
        assertEquals("That Darn Priest", mf2.getTitle(16));
        assertTrue(StringUtils.startsWith(mf2.getPlot(16), "Rose becomes wise to Alan's Ponzi scheme"));
        assertEquals("2011-12-13", mf2.getFirstAired(16));
    }

    @Test
    public void testImdbPerson() {
        LOG.info("testImdbPerson");
        PropertiesUtil.setProperty("plugin.filmography.max", 2);
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Person person = new Person();
        person.setName("Gérard Depardieu");

        assertTrue("Scan failed", imdbPlugin.scan(person));

        assertEquals("Wrong person matched", "nm0000367", person.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        assertTrue("No bio", StringTools.isValidString(person.getBiography()));
        assertEquals("Wrong birth name", "Gérard Xavier Marcel Depardieu", person.getBirthName());
        assertEquals("Wrong birth place", "Châteauroux, Indre, France", person.getBirthPlace());
        assertFalse("No Filmography", person.getFilmography().isEmpty());
        assertEquals("Wrong birthday", "27-12-1948", person.getYear());
        assertTrue("No job", StringTools.isValidString(person.getFilmography().get(0).getJob()));
    }

    /**
     * This uses a slightly different format for the bio
     */
    @Test
    public void testImdbBio() {
        LOG.info("testImdbBio");
        PropertiesUtil.setProperty("plugin.filmography.max", 2);
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Person person = new Person();
        person.setName("Adrian Edmondson");
        assertTrue("Scan failed", imdbPlugin.scan(person));
        assertTrue("No bio", StringTools.isValidString(person.getBiography()));
    }

    @Test
    public void testImdb_Short() {
        LOG.info("testImdb_Short");
        PropertiesUtil.setProperty("imdb.full.info", "false");
        PropertiesUtil.setProperty("mjb.scrapeAwards", "true");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0499549");
        movie.setBaseName("Test_Movie_Avatar");

        assertTrue(imdbPlugin.scan(movie));
        assertEquals("Incorrect year", "2009", movie.getYear());
        assertNotEquals("Incorrect Plot", Movie.UNKNOWN, movie.getPlot());
        assertTrue("Incorrect Rating", movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID) > 0);
        assertEquals("Incorrect Country", "USA / UK", movie.getCountriesAsString());
        assertEquals("Incorrect Company", "Twentieth Century Fox Film Corporation", movie.getCompany());
        assertTrue("Incorrect Tagline", StringTools.isValidString(movie.getTagline()));
        assertEquals("Incorrect number of cast", 10, movie.getCast().size());
        assertEquals("Incorrect Directors", 1, movie.getDirectors().size());
        assertEquals("Incorrect Writers", 1, movie.getWriters().size());
        assertEquals("Wrong release date", "2009-12-16", movie.getReleaseDate());
        assertTrue("No awards scraped", movie.getAwards().size() > 1);
    }

    @Test
    public void testImdb_Combined() {
        LOG.info("testImdb_Combined");
        PropertiesUtil.setProperty("imdb.full.info", "true");
        ImdbPlugin imdbPlugin = new ImdbPlugin();

        Movie movie = new Movie();
        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt0133093");
        movie.setBaseName("Test_Movie_Matrix");

        assertTrue(imdbPlugin.scan(movie));
        assertEquals("Incorrect year", "1999", movie.getYear());
        assertNotEquals("Incorrect Plot", Movie.UNKNOWN, movie.getPlot());
        assertTrue("Incorrect Rating", movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID) > 0);
        assertEquals("Incorrect Country", "USA / Australia", movie.getCountriesAsString());
        assertEquals("Incorrect Company", "Warner Bros.", movie.getCompany());
        assertEquals("Incorrect Tagline", "Free your mind", movie.getTagline());
        assertEquals("Incorrect number of cast", 10, movie.getCast().size());

        assertTrue("Andy W not found in Directors", movie.getDirectors().contains("Andy Wachowski"));
        assertTrue("Lana W not found in Directors", movie.getDirectors().contains("Lana Wachowski"));
        assertTrue("Andy W not found in Writers", movie.getWriters().contains("Andy Wachowski"));
        assertTrue("Lana W not found in Writers", movie.getWriters().contains("Lana Wachowski"));

    }

    @Test
    public void testScanNFO() {
        LOG.info("testScanNFO");
        String nfo = "\nhttp://www.imdb.com/title/tt0458339/\n";
        Movie movie = new Movie();

        ImdbPlugin imdbPlugin = new ImdbPlugin();
        imdbPlugin.scanNFO(nfo, movie);
        assertEquals("tt0458339", movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
    }
}
