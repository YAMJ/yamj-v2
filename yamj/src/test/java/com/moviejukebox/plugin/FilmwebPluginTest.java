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
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilmwebPluginTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(FilmwebPluginTest.class);

    private FilmwebPlugin filmwebPlugin;

    @BeforeClass
    public static void configure() {
        doConfiguration();
        loadMainProperties();
        loadApiProperties();

        PropertiesUtil.setProperty("priority.movie.directors", "imdb,filmweb");
        PropertiesUtil.setProperty("priority.movie.plot", "imdb,filmweb");
        PropertiesUtil.setProperty("priority.movie.runtime", "filmweb,imdb");
    }

    @Before
    public void setup() {
        filmwebPlugin = new FilmwebPlugin();
    }

    @Test
    public void testGetFilmwebUrl01() {
        LOG.info("testGetFilmwebUrl01");
        assertEquals("http://www.filmweb.pl/Seksmisja", filmwebPlugin.getMovieId("Seksmisja", null));
    }

    @Test
    public void testGetFilmwebUrl02() {
        LOG.info("testGetFilmwebUrl02");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testGetFilmwebUrl03() {
        LOG.info("testGetFilmwebUrl03");
        assertEquals("http://www.filmweb.pl/Seksmisja", filmwebPlugin.getMovieId("Seksmisja", null));
    }

    @Test
    public void testGetFilmwebUrl04() {
        LOG.info("testGetFilmwebUrl04");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testGetFilmwebUrl05() {
        LOG.info("testGetFilmwebUrl05");
        assertEquals("http://www.filmweb.pl/John.Rambo", filmwebPlugin.getMovieId("john rambo", null));
    }

    @Test
    public void testGetFilmwebUrl06() {
        LOG.info("testGetFilmwebUrl06");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testScanNFONoUrl() {
        LOG.info("testScanNFONoUrl");
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testScanNFO() {
        LOG.info("testScanNFO");
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("txt\ntxt\nfilmweb url: http://john.rambo.filmweb.pl - txt\ntxt", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("<url>http://john.rambo.filmweb.pl</url>", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("[url]http://john.rambo.filmweb.pl[/url]", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://www.filmweb.pl/f336379/Death+Sentence,2007", movie);
        assertEquals("http://www.filmweb.pl/f336379/Death+Sentence,2007", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://4.pila.filmweb.pl\thttp://www.imdb.com/title/tt0890870/", movie);
        assertEquals("http://4.pila.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://4.pila.filmweb.pl\nhttp://www.imdb.com/title/tt0890870/", movie);
        assertEquals("http://4.pila.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testScanNFOWithId() {
        LOG.info("testScanNFOWithId");
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("txt\ntxt\nfilmweb url: http://www.filmweb.pl/f122684/4400,2004 - txt\ntxt", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testScanNFOWithPoster() {
        LOG.info("testScanNFOWithPoster");
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\ntxt", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\nurl: http://www.filmweb.pl/f122684/4400,2004", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testUpdateMediaInfoTitle01() {
        LOG.info("testUpdateMediaInfoTitle01");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Seksmisja", movie.getTitle());
        assertEquals("Seksmisja", movie.getOriginalTitle());
    }

    //@Test
    public void testUpdateMediaInfoTitle02() {
        LOG.info("testUpdateMediaInfoTitle02");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("John Rambo", movie.getTitle());
        assertEquals("Rambo", movie.getOriginalTitle());
    }

    @Test
    public void testUpdateMediaInfoTitleWithOriginalTitle() {
        LOG.info("testUpdateMediaInfoTitleWithOriginalTitle");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Ojciec chrzestny", movie.getTitle());
        assertEquals("The Godfather", movie.getOriginalTitle());
    }

    @Test
    public void testUpdateMediaInfoRating() {
        LOG.info("testUpdateMediaInfoRating");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        LOG.info("Movie Rating: {}", movie.getRating(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(87, movie.getRating(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testUpdateMediaInfoTop250() {
        LOG.info("testUpdateMediaInfoTop250");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(3, movie.getTop250());
    }

    @Test
    public void testUpdateMediaInfoReleaseDate() {
        LOG.info("testUpdateMediaInfoReleaseDate");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("2008-01-23", movie.getReleaseDate());
    }

    @Test
    public void testUpdateMediaInfoRuntime() {
        LOG.info("testUpdateMediaInfoRuntime");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("92", movie.getRuntime());
    }

    @Test
    public void testUpdateMediaInfoCountry() {
        LOG.info("testUpdateMediaInfoCountry");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Niemcy / USA", movie.getCountriesAsString());
    }

    @Test
    public void testUpdateMediaInfoGenre() {
        LOG.info("testUpdateMediaInfoGenre");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(Arrays.asList(new String[]{"Akcja"}).toString(), movie.getGenres().toString());
    }

    @Test
    public void testUpdateMediaInfoOutline() {
        LOG.info("testUpdateMediaInfoOutline");
        Movie movie = new Movie();
        int outlineLength = PropertiesUtil.getIntProperty("movie.outline.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "9 sierpnia 1991 roku telewizja transmituje epokowy eksperyment: dwóch śmiałków - Maks (Jerzy Stuhr) i Albert (Olgierd Łukaszewicz), zostaje poddanych hibernacji. Podczas ich snu wybucha wojna nuklearna. Uczestnicy eksperymentu budzą się w 2044 roku. Od opiekującej się nimi doktor Lamii dowiadują się, że w ciągu ostatnich kilkudziesięciu lat geny męskie zostały całkowicie zniszczone promieniowaniem, a oni są prawdopodobnie jedynymi osobnikami płci męskiej, którzy przetrwali kataklizm. Niezwykła<span> społeczność kobiet, w jakiej znaleźli się bohaterowie, egzystuje w całkowicie sztucznych warunkach, głęboko pod powierzchnią ziemi. Władzę dyktatorską pełni tu Jej Ekscelencja, która darzy męskich osobników szczególnym zainteresowaniem. Maks i Albert znajdują się pod stałą obserwacją i ścisłą kontrolą. Takie życie na dłuższą metę wydaje im się jednak niemożliwe. Zdesperowani postanawiają więc uciec. ",
                        outlineLength), movie.getOutline());
    }

    @Test
    public void testUpdateMediaInfoPlot01() {
        LOG.info("testUpdateMediaInfoPlot01");
        Movie movie = new Movie();
        int plotLength = PropertiesUtil.getIntProperty("movie.plot.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Waleczne.Serce");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "Pod koniec XIII wieku Szkocja dostaje się pod panowanie angielskiego króla, Edwarda I. Przejęcie władzy odbywa się w wyjątkowo krwawych okolicznościach. Jednym ze świadków gwałtów i morderstw jest kilkunastoletni chłopak, William Wallace. Po latach spędzonych pod opieką wuja dorosły William wraca do rodzinnej wioski. Jedną z pierwszych osób, które spotyka, jest Murron - przyjaciółka z lat dzieciństwa. Dawne uczucie przeradza się w wielką i szczerą miłość. Niestety wkrótce dziewczyna ginie z rąk angielskich żołnierzy. Wydarzenie to staje się to momentem przełomowym w życiu młodego Szkota. William decyduje się bowiem na straceńczą walkę z okupantem i po brawurowym ataku zdobywa warownię wroga. Dzięki ogromnej odwadze zostaje wykreowany na przywódcę powstania przeciw angielskiej tyranii...",
                        plotLength), movie.getPlot());
    }

    @Test
    public void testUpdateMediaInfoPlot02() {
        LOG.info("testUpdateMediaInfoPlot02");
        Movie movie = new Movie();
        int plotLength = PropertiesUtil.getIntProperty("movie.plot.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Agenci-2013-612342");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "Agent wydziału do walki z przemytem narkotyków i oficer wywiadu marynarki są w sytuacji bez wyjścia. Kradną pieniądze gangsterów i zamierzają je przekazać na dobre cele. Okazuje się jednak, że w rzeczywistości ukradli pieniądze CIA, a zleceniodawcami są mafiosi.",
                        plotLength), movie.getPlot());
    }

    @Test
    public void testUpdateMediaInfoYear() {
        LOG.info("testUpdateMediaInfoYear");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("1983", movie.getYear());
    }

    @Test
    public void testUpdateMediaInfoDirector() {
        LOG.info("testUpdateMediaInfoDirector");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Avatar");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testDirectors = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testDirectors.add("James Cameron");

        assertEquals(Arrays.asList(testDirectors.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getDirectors().toArray(), 1)).toString());
    }

    @Test
    public void testUpdateMediaInfoCast01() {
        LOG.info("testUpdateMediaInfoCast01");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Avatar");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testCast = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testCast.add("Sam Worthington");
        testCast.add("Zoe Saldana");

        assertEquals(Arrays.asList(testCast.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
    }

    @Test
    public void testUpdateMediaInfoCast02() {
        LOG.info("testUpdateMediaInfoCast02");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Agenci-2013-612342");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testCast = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testCast.add("Denzel Washington");
        testCast.add("Mark Wahlberg");

        assertEquals(Arrays.asList(testCast.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
    }

    @Test
    public void testUpdateMediaInfoWriters() {
        LOG.info("testUpdateMediaInfoWriters");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Stra%C5%BCnicy+Galaktyki-2014-594357");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testWriters = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testWriters.add("James Gunn");

        assertEquals(Arrays.asList(testWriters.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getWriters().toArray(), 1)).toString());
    }

    @Test
    public void testUpdateMediaInfoUpdateTVShowInfo() {
        LOG.info("testUpdateMediaInfoUpdateTVShowInfo");
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Prison.Break");
        movie.setMovieType(Movie.TYPE_TVSHOW);
        MovieFile episode = new MovieFile();
        episode.setSeason(4);
        episode.setPart(1);
        movie.addMovieFile(episode);
        episode = new MovieFile();
        episode.setSeason(4);
        episode.setPart(2);
        movie.addMovieFile(episode);
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        Iterator<MovieFile> episodesIt = movie.getFiles().iterator();
        assertEquals("Scylla", episodesIt.next().getTitle());
        assertEquals("Breaking and Entering", episodesIt.next().getTitle());
    }
}
