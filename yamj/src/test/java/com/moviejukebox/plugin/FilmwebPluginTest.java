/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilmwebPluginTest {

    private FilmwebPluginMock filmwebPlugin;
    private boolean offline;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("priority.movie.directors", "imdb,filmweb");
        PropertiesUtil.setProperty("priority.movie.plot", "imdb,filmweb");
        PropertiesUtil.setProperty("priority.movie.runtime", "filmweb,imdb");
    }

    @Before
    public void setup() {
        // uncomment the line below to check if tests are still up to date
        offline = false;
        filmwebPlugin = new FilmwebPluginMock(offline);
    }

    @Test
    public void testGetFilmwebUrl01() {
        filmwebPlugin.setRequestResult("<font color=\"green\">http://www.filmweb.pl/Seksmisja - 84k</font>");
        assertEquals("http://www.filmweb.pl/Seksmisja", filmwebPlugin.getMovieId("Seksmisja", null));
    }

    @Test
    public void testGetFilmwebUrl02() {
        filmwebPlugin.setRequestResult("<font color=\"green\">http://www.filmweb.pl/serial/4400-2004-122684 - 90k</font>");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testGetFilmwebUrl03() {
        filmwebPlugin.setRequestResult("<a data-bk=\"5034.1\" href=\"http://www.filmweb.pl/Seksmisja\" class=\"yschttl spt\" dirtyhref=\"http://rds.yahoo.com/_ylt=A0oG7hxsZvVMxwEAUx9XNyoA;_ylu=X3oDMTE1azRuN3ZwBHNlYwNzcgRwb3MDMQRjb2xvA2FjMgR2dGlkA1NSVDAwMV8xODc-/SIG=11jrti008/EXP=1291237356/**http%3a//www.filmweb.pl/Seksmisja\"><b>Seksmisja</b> (1983) - Filmweb</a>");
        assertEquals("http://www.filmweb.pl/Seksmisja", filmwebPlugin.getMovieId("Seksmisja", null));
    }

    @Test
    public void testGetFilmwebUrl04() {
        filmwebPlugin.setRequestResult("<a href=\"http://search.yahoo.com/web/advanced?ei=UTF-8&p=4400+site%3Afilmweb.pl&y=Search\">Advanced Search</a><a class=\"yschttl\" href=\"http://rds.yahoo.com/_ylt=A0geu5RTv7FI.jUB.DtXNyoA;_ylu=X3oDMTE1aGEzbmUyBHNlYwNzcgRwb3MDMQRjb2xvA2FjMgR2dGlkA01BUDAxMV8xMDg-/SIG=11rlibf7n/EXP=1219694803/**http%3a//www.filmweb.pl/serial/4400-2004-122684\" ><b>4400</b> / <b>4400</b>, The (2004) - Film - FILMWEB.pl</a>");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testGetFilmwebUrl05() {
        filmwebPlugin.setRequestResult("<a class=\"searchResultTitle\" href=\"/John.Rambo\"><b>John</b> <b>Rambo</b> / <b>Rambo</b> </a>");
        assertEquals("http://www.filmweb.pl/John.Rambo", filmwebPlugin.getMovieId("john rambo", null));
    }

    @Test
    public void testGetFilmwebUrl06() {
        filmwebPlugin.setRequestResult("<a class=\"searchResultTitle\" href=\"/serial/4400-2004-122684\"><b>4400</b> / <b>4400</b>, The </a>");
        assertEquals("http://www.filmweb.pl/serial/4400-2004-122684", filmwebPlugin.getMovieId("The 4400", null));
    }

    @Test
    public void testScanNFONoUrl() {
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testScanNFO() {
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
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("txt\ntxt\nfilmweb url: http://www.filmweb.pl/f122684/4400,2004 - txt\ntxt", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testScanNFOWithPoster() {
        Movie movie = new Movie();
        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\ntxt", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\nurl: http://www.filmweb.pl/f122684/4400,2004", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testUpdateMediaInfoTitle01() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.setRequestResult("<title>Seksmisja (1984)  - Film - FILMWEB.pl</title>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Seksmisja", movie.getTitle());
        assertEquals("Seksmisja", movie.getOriginalTitle());
    }

    //@Test
    public void testUpdateMediaInfoTitle02() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.setRequestResult("<title>John Rambo (2008) - Filmweb</title><meta property=\"og:title\" content=\"John Rambo / Rambo\">");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("John Rambo", movie.getTitle());
        assertEquals("Rambo", movie.getOriginalTitle());
    }

    @Test
    public void testUpdateMediaInfoTitleWithOriginalTitle() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.setRequestResult("<title>Ojciec chrzestny (1972) - Filmweb</title><meta property=\"og:title\" content=\"Ojciec chrzestny / Godfather, The\">");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Ojciec chrzestny", movie.getTitle());
        assertEquals("The Godfather", movie.getOriginalTitle());
    }

    @Test
    public void testUpdateMediaInfoRating() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.setRequestResult("<span class=\"filmRate\"><strong rel=\"v:rating\" property=\"v:average\"> 8,7</strong>/10</span>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        System.err.println(movie.getRating(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(87, movie.getRating(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    @Test
    public void testUpdateMediaInfoTop250() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Ojciec.Chrzestny");
        filmwebPlugin.setRequestResult("<span class=worldRanking>2. <a href=\"/rankings/film/world#Ojciec chrzestny\">w rankingu światowym</a></span>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(3, movie.getTop250());
    }

    @Test
    public void testUpdateMediaInfoReleaseDate() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.setRequestResult("<span id =\"filmPremierePoland\" style=\"display:none\">2008-03-07</span><span style=\"display: none;\" id=\"filmPremiereWorld\">2008-01-23</span>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("2008-01-23", movie.getReleaseDate());
    }

    @Test
    public void testUpdateMediaInfoRuntime() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.setRequestResult("<div class=filmTime><i class=icon-small-clock></i> 1 godz. 32 min.</div>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("92", movie.getRuntime());
    }

    @Test
    public void testUpdateMediaInfoCountry() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.setRequestResult("<tr><th>produkcja:</th><td><a href=\"/search/film?countryIds=38\">Niemcy</a>, <a href=\"/search/film?countryIds=53\">USA</a></td></tr>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("Niemcy / USA", movie.getCountriesAsString());
    }

    @Test
    public void testUpdateMediaInfoGenre() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/John.Rambo");
        filmwebPlugin.setRequestResult("<th>gatunek:</th><td><a href=\"/search/film?genreIds=26\">Wojenny</a>, <a href=\"/search/film?genreIds=28\">Akcja</a></td></tr><tr><th>premiera:</th>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals(Arrays.asList(new String[]{"Akcja"}).toString(), movie.getGenres().toString());
    }

    @Test
    public void testUpdateMediaInfoOutline() {
        Movie movie = new Movie();
        int outlineLength = PropertiesUtil.getIntProperty("movie.outline.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.setRequestResult("<span class=imgRepInNag>Seksmisja</span></h2><p class=cl><span class=filmDescrBg property=\"v:summary\">9 sierpnia 1991 roku telewizja transmituje epokowy eksperyment: dwóch śmiałków - Maks (Jerzy Stuhr) i Albert (Olgierd Łukaszewicz), zostaje poddanych hibernacji. Podczas ich snu wybucha wojna nuklearna. Uczestnicy eksperymentu budzą się w 2044 roku. Od opiekującej się nimi doktor Lamii dowiadują się, że w ciągu ostatnich kilkudziesięciu lat geny męskie zostały całkowicie zniszczone promieniowaniem, a oni są prawdopodobnie jedynymi osobnikami płci męskiej, którzy przetrwali kataklizm. Niezwykła<span> społeczność kobiet, w jakiej znaleźli się bohaterowie, egzystuje w całkowicie sztucznych warunkach, głęboko pod powierzchnią ziemi. Władzę dyktatorską pełni tu Jej Ekscelencja, która darzy męskich osobników szczególnym zainteresowaniem. Maks i Albert znajdują się pod stałą obserwacją i ścisłą kontrolą. Takie życie na dłuższą metę wydaje im się jednak niemożliwe. Zdesperowani postanawiają więc uciec. </span> <a href=\"#\" class=see-more>więcej </a></span>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "9 sierpnia 1991 roku telewizja transmituje epokowy eksperyment: dwóch śmiałków - Maks (Jerzy Stuhr) i Albert (Olgierd Łukaszewicz), zostaje poddanych hibernacji. Podczas ich snu wybucha wojna nuklearna. Uczestnicy eksperymentu budzą się w 2044 roku. Od opiekującej się nimi doktor Lamii dowiadują się, że w ciągu ostatnich kilkudziesięciu lat geny męskie zostały całkowicie zniszczone promieniowaniem, a oni są prawdopodobnie jedynymi osobnikami płci męskiej, którzy przetrwali kataklizm. Niezwykła<span> społeczność kobiet, w jakiej znaleźli się bohaterowie, egzystuje w całkowicie sztucznych warunkach, głęboko pod powierzchnią ziemi. Władzę dyktatorską pełni tu Jej Ekscelencja, która darzy męskich osobników szczególnym zainteresowaniem. Maks i Albert znajdują się pod stałą obserwacją i ścisłą kontrolą. Takie życie na dłuższą metę wydaje im się jednak niemożliwe. Zdesperowani postanawiają więc uciec. ",
                        outlineLength), movie.getOutline());
    }

    @Test
    public void testUpdateMediaInfoPlot01() {
        Movie movie = new Movie();
        int plotLength = PropertiesUtil.getIntProperty("movie.plot.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Waleczne.Serce");
        filmwebPlugin.setRequestResult("<div class=\"filmDescription comBox\">\n  \t<h2>\n  \t\t<a href=\"/Waleczne.Serce/descs\" class=\"hdrBig icoBig icoBigArticles\">\n  \t\t\t opisy filmu   \t\t</a>\t\t\n\t\t\t\t\t<span class=\"hdrAddInfo\">(10)</span>\n\t\t\t\t\n  \t\t  \t\t<a href=\"\t\t\t/Waleczne.Serce/contribute/descriptions\t\" class=\"add-button\" title=\"dodaj  opis filmu \" rel=\"nofollow\">  \t\t\t<span>dodaj  opis filmu </span>\n\n  \t\t</a>\n\t\t<span class=\"imgRepInNag\">Braveheart - Waleczne Serce</span>\n  \t</h2>\n\t\n\t\t\t\t\t   \t   \t\t<p class=\"cl\"><span class=\"filmDescrBg\" property=\"v:summary\">Pod koniec XIII wieku Szkocja dostaje się pod panowanie angielskiego króla, Edwarda I. Przejęcie władzy odbywa się w wyjątkowo krwawych okolicznościach. Jednym ze świadków gwałtów i morderstw jest kilkunastoletni chłopak, William Wallace. Po latach spędzonych pod opieką wuja dorosły William wraca do rodzinnej wioski. Jedną z pierwszych osób, które spotyka, jest Murron - przyjaciółka z lat dzieciństwa. Dawne uczucie przeradza się w wielką i szczerą miłość. Niestety wkrótce dziewczyna ginie z rąk<span> angielskich żołnierzy. Wydarzenie to staje się to momentem przełomowym w życiu młodego Szkota. William decyduje się bowiem na straceńczą walkę z okupantem i po brawurowym ataku zdobywa warownię wroga. Dzięki ogromnej odwadze zostaje wykreowany na przywódcę powstania przeciw angielskiej tyranii...</span> <a href=\"#\" class=\"see-more\">więcej </a></span></p>\n   \t\t  </div>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "Pod koniec XIII wieku Szkocja dostaje się pod panowanie angielskiego króla, Edwarda I. Przejęcie władzy odbywa się w wyjątkowo krwawych okolicznościach. Jednym ze świadków gwałtów i morderstw jest kilkunastoletni chłopak, William Wallace. Po latach spędzonych pod opieką wuja dorosły William wraca do rodzinnej wioski. Jedną z pierwszych osób, które spotyka, jest Murron - przyjaciółka z lat dzieciństwa. Dawne uczucie przeradza się w wielką i szczerą miłość. Niestety wkrótce dziewczyna ginie z rąk angielskich żołnierzy. Wydarzenie to staje się to momentem przełomowym w życiu młodego Szkota. William decyduje się bowiem na straceńczą walkę z okupantem i po brawurowym ataku zdobywa warownię wroga. Dzięki ogromnej odwadze zostaje wykreowany na przywódcę powstania przeciw angielskiej tyranii...",
                        plotLength), movie.getPlot());
    }

    @Test
    public void testUpdateMediaInfoPlot02() {
        Movie movie = new Movie();
        int plotLength = PropertiesUtil.getIntProperty("movie.plot.maxLength", 500);
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Agenci-2013-612342");
        filmwebPlugin.setRequestResult("<div class=\"filmDescription comBox\">\n  \t<h2>\n  \t\t<a href=\"/Waleczne.Serce/descs\" class=\"hdrBig icoBig icoBigArticles\">\n  \t\t\t opisy filmu   \t\t</a>\t\t\n\t\t\t\t\t<span class=\"hdrAddInfo\">(10)</span>\n\t\t\t\t\n  \t\t  \t\t<a href=\"\t\t\t/Waleczne.Serce/contribute/descriptions\t\" class=\"add-button\" title=\"dodaj  opis filmu \" rel=\"nofollow\">  \t\t\t<span>dodaj  opis filmu </span>\n\n  \t\t</a>\n\t\t<span class=\"imgRepInNag\">Braveheart - Waleczne Serce</span>\n  \t</h2>\n\t\n\t\t\t\t\t   \t   \t\t<p class=\"cl\"><span class=\"filmDescrBg\" property=\"v:summary\">Pod koniec XIII wieku Szkocja dostaje się pod panowanie angielskiego króla, Edwarda I. Przejęcie władzy odbywa się w wyjątkowo krwawych okolicznościach. Jednym ze świadków gwałtów i morderstw jest kilkunastoletni chłopak, William Wallace. Po latach spędzonych pod opieką wuja dorosły William wraca do rodzinnej wioski. Jedną z pierwszych osób, które spotyka, jest Murron - przyjaciółka z lat dzieciństwa. Dawne uczucie przeradza się w wielką i szczerą miłość. Niestety wkrótce dziewczyna ginie z rąk<span> angielskich żołnierzy. Wydarzenie to staje się to momentem przełomowym w życiu młodego Szkota. William decyduje się bowiem na straceńczą walkę z okupantem i po brawurowym ataku zdobywa warownię wroga. Dzięki ogromnej odwadze zostaje wykreowany na przywódcę powstania przeciw angielskiej tyranii...</span> <a href=\"#\" class=\"see-more\">więcej </a></span></p>\n   \t\t  </div>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        assertEquals(
                StringTools.trimToLength(
                        "Agent wydziału do walki z przemytem narkotyków i oficer wywiadu marynarki są w sytuacji bez wyjścia. Kradną pieniądze gangsterów i zamierzają je przekazać na dobre cele. Okazuje się jednak, że w rzeczywistości ukradli pieniądze CIA, a zleceniodawcami są mafiosi.",
                        plotLength), movie.getPlot());
    }

    @Test
    public void testUpdateMediaInfoYear() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Seksmisja");
        filmwebPlugin.setRequestResult("<span id=filmYear class=filmYear> (1983) </span>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
        assertEquals("1983", movie.getYear());
    }

    @Test
    public void testUpdateMediaInfoDirector() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Avatar");
        filmwebPlugin.setRequestResult(null); // no offline test
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testDirectors = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testDirectors.add("James Cameron");

        assertEquals(Arrays.asList(testDirectors.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getDirectors().toArray(), 1)).toString());
    }

    @Test
    public void testUpdateMediaInfoCast01() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/Avatar");
        filmwebPlugin.setRequestResult(null); // no offline test
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testCast = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testCast.add("Sam Worthington");
        testCast.add("Zoe Saldana");

        assertEquals(Arrays.asList(testCast.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
    }

    @Test
    public void testUpdateMediaInfoCast02() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Agenci-2013-612342");
        filmwebPlugin.setRequestResult(null); // no offline test
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testCast = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testCast.add("Denzel Washington");
        testCast.add("Mark Wahlberg");

        assertEquals(Arrays.asList(testCast.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
    }

    @Test
    public void testUpdateMediaInfoWriters() {
        Movie movie = new Movie();
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://www.filmweb.pl/film/Stra%C5%BCnicy+Galaktyki-2014-594357");
        filmwebPlugin.setRequestResult(null); // no offline test
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        LinkedHashSet<String> testWriters = new LinkedHashSet<>();
        // These need to be in the same order as the web page
        testWriters.add("James Gunn");

        assertEquals(Arrays.asList(testWriters.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getWriters().toArray(), 1)).toString());
    }

    @Test
    public void testUpdateMediaInfoUpdateTVShowInfo() {
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
        filmwebPlugin.setRequestResult("<h3>sezon 4</h3>\n   \t\t\t\t\t\t\t\t<a href=\"#\" class=\"seasonWatched common-button-third\" style=\"display:none\">\n\n   \t\t\t\t\t\t\t\t\t<span class=\"lBtbL\"></span>\n                                       <span class=\"lBtbR\"></span>\n                                       <span>Oglądałem</span>\n   \t\t\t\t\t\t\t\t</a>\n               \t\t\t\t</th>\n       \t\t\t\t\t</tr>\n               \t\t\t               \t\t\t\t   \t<tr>\n   \t\t<td>\n\n   \t\t\t   \t\t\t\todcinek&nbsp;1\n   \t\t\t   \t\t\t   \t\t</td>\n   \t\t<td>\n   \t\t\t   \t\t\t   \t\t\t\t   \t\t\t\t\t   \t\t\t\t\t   \t\t\t\t\t<div>\t\t\t\t\t\t\t\t\t\t1 września\t\t\t\t\t\t\t2008\n\t<br><span class=\"countryName\">(USA)</span></div>\n   \t\t\t\t   \t\t\t   \t\t</td>\n   \t\t<td>\n   \t\t\t   \t\t\t\tScylla\n   \t\t\t   \t\t</td>\n\n   \t\t   \t\t   \t\t   \t</tr>\n               \t\t\t               \t\t\t\t   \t<tr>\n   \t\t<td>\n   \t\t\t   \t\t\t\todcinek&nbsp;2\n   \t\t\t   \t\t\t   \t\t</td>\n   \t\t<td>\n   \t\t\t   \t\t\t   \t\t\t\t   \t\t\t\t\t   \t\t\t\t\t   \t\t\t\t\t<div>\t\t\t\t\t\t\t\t\t\t1 września\t\t\t\t\t\t\t2008\n\t<br><span class=\"countryName\">(USA)</span></div>\n\n   \t\t\t\t   \t\t\t   \t\t</td>\n   \t\t<td>\n   \t\t\t   \t\t\t\tBreaking and Entering\n   \t\t\t   \t\t</td>\n   \t\t   \t\t   \t\t   \t</tr>");
        filmwebPlugin.updateMediaInfo(movie, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        Iterator<MovieFile> episodesIt = movie.getFiles().iterator();
        assertEquals("Scylla", episodesIt.next().getTitle());
        assertEquals("Breaking and Entering", episodesIt.next().getTitle());
    }

    class FilmwebPluginMock extends FilmwebPlugin {

        private String requestResult;
        private final boolean offline;

        public FilmwebPluginMock(boolean offline) {
            this.offline = offline;
            super.init();
        }

        @Override
        public void init() {
            webBrowser = new WebBrowser() {
                @Override
                public String request(URL url) throws IOException {
                    if (offline && (getRequestResult(url) != null)) {
                        return getRequestResult(url);
                    } else {
                        return super.request(url);
                    }
                }
            };
        }

        public String getRequestResult(URL url) {
            return requestResult;
        }

        public void setRequestResult(String requestResult) {
            this.requestResult = requestResult;
        }
    }
}
