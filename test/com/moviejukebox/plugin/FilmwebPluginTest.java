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

package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class FilmwebPluginTest extends TestCase {
    private FilmwebPluginMock filmwebPlugin;
    private Movie movie = new Movie();
    private boolean offline = true;

    static {
        PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties");
    }

    protected void setUp() {
        // uncomment the bottom line to check if tests are still up to date
        // offline = false;
        filmwebPlugin = new FilmwebPluginMock(offline);
        filmwebPlugin.filmwebPlot = "short";
        movie = new Movie();
    }

    public void testGetFilmwebUrlFromGoogle() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("google");
        filmwebPlugin.setRequestResult("<a href=\"http://www.google.pl/search?hl=pl&q=seksmisja+site:filmweb.pl&um=1&ie=UTF-8&cat=gwd/Top&sa=N&tab=wd\"></a><link rel=\"prefetch\" href=\"http://seksmisja.filmweb.pl/\"><h3 class=r><a href=\"http://seksmisja.filmweb.pl/\" class=l onmousedown=\"return clk(this.href,'','','res','1','')\"><em>Seksmisja</em> (1984) - Film - FILMWEB.pl</a></h3>");
        assertEquals("http://seksmisja.filmweb.pl/", filmwebPlugin.getFilmwebUrl("Seksmisja", null));
    }

    public void testGetFilmwebUrlFromGoogleWithId() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("google");
        filmwebPlugin.setRequestResult("<a href=\"http://www.google.pl/search?hl=pl&q=4400+site:filmweb.pl&um=1&ie=UTF-8&cat=gwd/Top&sa=N&tab=wd\"></a><h3 class=r><a href=\"http://www.filmweb.pl/f122684/4400,2004\" class=l onmousedown=\"return clk(this.href,'','','res','1','')\"><em>4400</em> / <em>4400</em>, The (2004) - Film - FILMWEB.pl</a></h3>");
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", filmwebPlugin.getFilmwebUrl("4400", null));
    }

    public void testGetFilmwebUrlFromYahoo() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("yahoo");
        filmwebPlugin.setRequestResult("<a href=\"http://search.yahoo.com/ypredirect;_ylt=A0geu6dkwLFIMTEBu1ZXNyoA?ei=UTF-8&p=john+rambo+site%3Afilmweb.pl&y=Search&fr2=tab-web&fr=\">Local</a><a class=\"yschttl\" href=\"http://rds.yahoo.com/_ylt=A0geu6dkwLFIMTEByFZXNyoA;_ylu=X3oDMTE1aGEzbmUyBHNlYwNzcgRwb3MDMQRjb2xvA2FjMgR2dGlkA01BUDAxMV8xMDg-/SIG=11hviotu2/EXP=1219695076/**http%3a//john.rambo.filmweb.pl/\" ><b>John</b> <b>Rambo</b> / <b>Rambo</b> (2008) - Film - FILMWEB.pl</a>");
        assertEquals("http://john.rambo.filmweb.pl/", filmwebPlugin.getFilmwebUrl("john rambo", null));
    }

    public void testGetFilmwebUrlFromYahooWithId() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("yahoo");
        filmwebPlugin.setRequestResult("<a href=\"http://search.yahoo.com/web/advanced?ei=UTF-8&p=4400+site%3Afilmweb.pl&y=Search\">Advanced Search</a><a class=\"yschttl\" href=\"http://rds.yahoo.com/_ylt=A0geu5RTv7FI.jUB.DtXNyoA;_ylu=X3oDMTE1aGEzbmUyBHNlYwNzcgRwb3MDMQRjb2xvA2FjMgR2dGlkA01BUDAxMV8xMDg-/SIG=11rlibf7n/EXP=1219694803/**http%3a//www.filmweb.pl/f122684/4400,2004\" ><b>4400</b> / <b>4400</b>, The (2004) - Film - FILMWEB.pl</a>");
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", filmwebPlugin.getFilmwebUrl("4400", null));
    }

    public void testGetFilmwebUrlFromFilmweb() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("filmweb");
        filmwebPlugin.setRequestResult("<img src=\"http://gfx.filmweb.pl/po/98/06/219806/7186729.1.jpg\"/><a class=\"searchResultTitle\" href=\"http://john.rambo.filmweb.pl/\"><b>John</b> <b>Rambo</b> / <b>Rambo</b> (2008)</a>");
        assertEquals("http://john.rambo.filmweb.pl/", filmwebPlugin.getFilmwebUrl("john rambo", null));
    }

    public void testGetFilmwebUrlFromFilmwebWithId() {
        filmwebPlugin.imdbInfo.setPreferredSearchEngine("filmweb");
        filmwebPlugin.setRequestResult("<a href=\"http://www.filmweb.pl/szukaj/film?q=400&type=&startYear=&endYear=&countryIds=&genreIds=&startRate=&endRate=&startCount=&endCount=&sort=TEXT_SCORE&sortAscending=false\">400</a><a class=\"searchResultTitle\" href=\"http://www.filmweb.pl/f122684/4400,2004\"><b>4400</b> / <b>4400</b>, The (2004)</a>");
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", filmwebPlugin.getFilmwebUrl("4400", null));
    }

    public void testScanNFONoUrl() {
        filmwebPlugin.scanNFO("", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    public void testScanNFO() {
        filmwebPlugin.scanNFO("txt\ntxt\nfilmweb url: http://john.rambo.filmweb.pl - txt\ntxt", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("<url>http://john.rambo.filmweb.pl</url>", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("[url]http://john.rambo.filmweb.pl[/url]", movie);
        assertEquals("http://john.rambo.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://www.filmweb.pl/f336379/Death+Sentence,2007", movie);
        assertEquals("http://www.filmweb.pl/f336379/Death+Sentence,2007", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://4.pila.filmweb.pl\thttp://www.imdb.com/title/tt0890870/", movie);
        assertEquals("http://4.pila.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, Movie.UNKNOWN);
        filmwebPlugin.scanNFO("http://4.pila.filmweb.pl\nhttp://www.imdb.com/title/tt0890870/", movie);
        assertEquals("http://4.pila.filmweb.pl", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    public void testScanNFOWithId() {
        filmwebPlugin.scanNFO("txt\ntxt\nfilmweb url: http://www.filmweb.pl/f122684/4400,2004 - txt\ntxt", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    public void testScanNFOWithPoster() {
        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\ntxt", movie);
        assertEquals(Movie.UNKNOWN, movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

        filmwebPlugin.scanNFO("txt\ntxt\nimg: http://gfx.filmweb.pl/po/18/54/381854/7131155.3.jpg - txt\nurl: http://www.filmweb.pl/f122684/4400,2004", movie);
        assertEquals("http://www.filmweb.pl/f122684/4400,2004", movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
    }

    public void testUpdateMediaInfoTitle() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin.setRequestResult("<title>Seksmisja (1984)  - Film - FILMWEB.pl</title>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Seksmisja", movie.getTitle());
        assertEquals("Seksmisja", movie.getOriginalTitle());
    }

    public void testUpdateMediaInfoTitleWithOriginalTitle() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://ojciec.chrzestny.filmweb.pl");
        filmwebPlugin.setRequestResult("<title>Ojciec chrzestny / Godfather, The (1972)  - Film - FILMWEB.pl</title>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Ojciec chrzestny", movie.getTitle());
        assertEquals("The Godfather", movie.getOriginalTitle());
    }

    public void testUpdateMediaInfoRating() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://ojciec.chrzestny.filmweb.pl");
        filmwebPlugin.setRequestResult("<div class=\"film-rating\">\n\t\t\t\t    \t\t        \t\t<p>Średnia ocena: <b>Rewelacyjny</b></p>\n        \t\t<div class=\"film-rating-precise\">\n\n\t\t\t\t\t        \t\t\t<div class=\"film-rating-fill\" style=\"width: 90.8260726928711%\">\n\t\t\t\t\t\t<span><strong class=\"value\">9,08</strong>/10</span></div>\n        \t\t</div>\n    \t\t    \t\t</div>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals(91, movie.getRating());
    }

    public void testUpdateMediaInfoTop250() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://ojciec.chrzestny.filmweb.pl");
        filmwebPlugin.setRequestResult("<p class=\"film-top-world\">\n\t\t\t\t\t\t\t\t\t<span class=\"value         \t    \t\tsame\n    \t    \">\n\t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/film/top/world#Ojciec chrzestny\">top świat: #1</a>\t\t\t\t\t\t</span>\n\n\t\t</p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals(1, movie.getTop250());
    }

    public void testUpdateMediaInfoDirector() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        filmwebPlugin.setRequestResult("<p class=\"film-info\">\nreżyseria\t<a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>scenariusz<a href=\"http://www.filmweb.pl/o70013/Art+Monterastelli\" title=\"Art Monterastelli- filmografia - FILMWEB.pl\">Art Monterastelli</a>, <a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>zdjęcia<a href=\"http://www.filmweb.pl/o11625/Glen+MacPherson\" title=\"Glen MacPherson- filmografia - FILMWEB.pl\">Glen MacPherson</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\nmuzyka<a href=\"http://www.filmweb.pl/o48282/Brian+Tyler\" title=\"Brian Tyler- filmografia - FILMWEB.pl\">Brian Tyler</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\n\nczas trwania: 91<abbr title=\"Dystrybucja\">dyst.:</abbr>\n<a rel=\"nofollow\" class=\"external\" href=\"http://www.monolith.pl\">Monolith Films</a></p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Sylvester Stallone", movie.getDirector());
    }

    public void testUpdateMediaInfoReleaseDate() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        filmwebPlugin.setRequestResult("<p class=\"film-premieres\">\ndata premiery:\t\t<strong>2008-03-07</strong>(Polska),<strong>2008-01-23</strong> (Świat)</p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("2008-03-07", movie.getReleaseDate());
    }

    public void testUpdateMediaInfoRuntime() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        filmwebPlugin.setRequestResult("<p class=\"film-info\">\nreżyseria\t<a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>scenariusz<a href=\"http://www.filmweb.pl/o70013/Art+Monterastelli\" title=\"Art Monterastelli- filmografia - FILMWEB.pl\">Art Monterastelli</a>, <a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>zdjęcia<a href=\"http://www.filmweb.pl/o11625/Glen+MacPherson\" title=\"Glen MacPherson- filmografia - FILMWEB.pl\">Glen MacPherson</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\nmuzyka<a href=\"http://www.filmweb.pl/o48282/Brian+Tyler\" title=\"Brian Tyler- filmografia - FILMWEB.pl\">Brian Tyler</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\n\nczas trwania: 91<abbr title=\"Dystrybucja\">dyst.:</abbr>\n<a rel=\"nofollow\" class=\"external\" href=\"http://www.monolith.pl\">Monolith Films</a></p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("91", movie.getRuntime());
    }

    public void testUpdateMediaInfoCountry() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        filmwebPlugin.setRequestResult("\t\t\tprodukcja:\t\t<strong>\t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?countryIds=38&amp;sort=COUNT&amp;sortAscending=false\">Niemcy</a>\t\t\t\t, \t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?countryIds=53&amp;sort=COUNT&amp;sortAscending=false\">USA</a>\t\t\t\t\t\t\t\t\t</strong>\t\t\t\tgatunek:\t\t\t\t\t<strong>\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?genreIds=26&amp;sort=COUNT&amp;sortAscending=false\">Wojenny</a>\t\t\t</strong>\t\t\t, \t\t\t\t\t<strong>\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?genreIds=28&amp;sort=COUNT&amp;sortAscending=false\">Akcja</a>\t\t\t</strong>\t\t\t\t\t\t\t</p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Niemcy, USA", movie.getCountry());
    }

    public void testUpdateMediaInfoGenre() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        filmwebPlugin.setRequestResult("<p class=\"film-production-genre\">\n\t\t\tprodukcja:\t\t<strong>\t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?countryIds=38&amp;sort=COUNT&amp;sortAscending=false\">Niemcy</a>\t\t\t\t, \t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?countryIds=53&amp;sort=COUNT&amp;sortAscending=false\">USA</a>\t\t\t\t\t\t\t\t\t</strong>\t\t\t\tgatunek:\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<strong>\t\t\t<a href=\"http://www.filmweb.pl/szukaj/film?sort=COUNT&sortAscending=false&genreIds=26&amp;genreIds=28\"> Wojenny, Akcja</a>\t\t</strong>\t\t</p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals(Arrays.asList(new String[] { "Akcja", "Wojenny" }).toString(), movie.getGenres().toString());
    }

    public void testUpdateMediaInfoShortPlot() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin.filmwebPlot = "short";
        filmwebPlugin.setRequestResult("<h2 id=\"o-filmie-header\" class=\"replace\">Film - Seksmisja<span></span></h2>\n\t\t\t<p>\n\t\t\tAkcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efekt&oacute;w b\t\t\t\t\t\t\t\t... <a href=\"http://seksmisja.filmweb.pl/f1163/Seksmisja,1984/opisy\" title=\"więcej o Seksmisja\">więcej</a>\n\n\t\t\t\t\t\t</p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Akcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efektów b\t\t\t\t\t\t\t\t...",
                     movie.getPlot());
        assertEquals(movie.getPlot(), movie.getOutline());
    }

    public void testUpdateMediaInfoLongPlot() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin = new FilmwebPluginMock(offline) {
            public String getRequestResult(URL url) {
                if ("http://seksmisja.filmweb.pl/f1163/Seksmisja,1984/opisy".equals(url.toString())) {
                    return "<h2 class=\"replace\" id=\"opisy-header\">Opisy - Seksmisja<span></span></h2>\n\t\t\t&#160;<a class=\"n\" href=\"/Login\">zgłoś poprawkę</a>\n\t  </div>\n  \t\t<ul>\n\t\t<li><p style=\"text-align:justify\">\n\n\t\t\tAkcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efekt&oacute;w było całkowite zniszczenie gen&oacute;w męskich, w związku z czym są obecnie prawdopodobnie jedynymi mężczyznami na planecie. \n\t\t\t</p></li>\n\t\t\t\t</ul>";
                } else {
                    return "<li><a title=\"Seksmisja (1984)  - opisy - FILMWEB.pl\" href=\"http://seksmisja.filmweb.pl/f1163/Seksmisja,1984/opisy\">opisy</a> [6] &raquo;</li><h2 id=\"o-filmie-header\" class=\"replace\">Film - Seksmisja<span></span></h2>\n\t\t\t<p>\n\t\t\tAkcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efekt&oacute;w b\t\t\t\t\t\t\t\t... <a href=\"http://seksmisja.filmweb.pl/f1163/Seksmisja,1984/opisy\" title=\"więcej o Seksmisja\">więcej</a>\n\n\t\t\t\t\t\t</p>";
                }
            }
        };
        filmwebPlugin.filmwebPlot = "long";
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("Akcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efektów było całkowite zniszczenie genów męskich, w związku z czym są obecnie prawdopodobnie jedynymi mężczyznami na planecie.",
                     movie.getPlot());
        assertEquals("Akcja filmu rozpoczyna się w sierpniu 1991 roku. Telewizja transmituje epokowy eksperyment. Maks i Albert, dwaj śmiałkowie, dobrowolnie poddają się hibernacji. Budzą się dopiero w roku 2044. Od opiekującej się nimi doktor Lamii dowiadują się, że w czasie ich snu wybuchła na Ziemi wojna nuklearna. Jednym z jej efektów b\t\t\t\t\t\t\t\t...",
                     movie.getOutline());
    }

    public void testUpdateMediaInfoYear() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin.setRequestResult("<title>Seksmisja (1984)  - Film - FILMWEB.pl</title>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("1984", movie.getYear());
    }

    public void testUpdateMediaInfoCast() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin.setRequestResult("<table class=\"film-starring\">\n\t\t<thead>\n\t\t\t<tr>\n\t\t\t\t<th scope=\"col\">Aktor</th>\n\n\t\t\t\t<th scope=\"col\">Bohater</th>\n\t\t\t</tr>\n\t\t</thead>\n\t\t<tbody>\n\t\t\t\t\t\t\t        \t\t\t<tr >\n        \t\t\t\t<td class=\"film-actor\">\n\t\t\t\t\t\t\t<a href=\"http://jerzy.stuhr.filmweb.pl/\">\n                    \t\t                    \t\t\t<img width=\"23\" height=\"32\" src=\"http://gfx.filmweb.pl/p/01/10/110/145434.0.jpg\" title=\"Jerzy Stuhr - filmografia - FILMWEB.pl\" alt=\"Jerzy Stuhr\" />\n\n                    \t\t\t\t\t\t\t\t\t\tJerzy Stuhr\n                    \t\t</a>\n\t\t\t\t\t\t</td>\n        \t\t\t\t<td class=\"film-protagonist\">\n    \t\t\t\t\t                    \t\tMaks\n                    \t\t<span>\n                    \t\t\t                    \t\t</span>\n                    \t\t\t\t\t\t\t\t\t</td>\n        \t\t\t</tr>\n\t\t\t\t\t\t\t\t\t\t\t        \t\t\t<tr class=\"even\">\n\n        \t\t\t\t<td class=\"film-actor\">\n\t\t\t\t\t\t\t<a href=\"http://www.filmweb.pl/o405/Olgierd+%C5%81ukaszewicz\">\n                    \t\t                    \t\t\t<img width=\"23\" height=\"32\" src=\"http://gfx.filmweb.pl/p/04/05/405/146170.0.jpg\" title=\"Olgierd Łukaszewicz - filmografia - FILMWEB.pl\" alt=\"Olgierd Łukaszewicz\" />\n                    \t\t\t\t\t\t\t\t\t\tOlgierd Łukaszewicz\n                    \t\t</a>\n\t\t\t\t\t\t</td>\n        \t\t\t\t<td class=\"film-protagonist\">\n    \t\t\t\t\t                    \t\tAlbert\n                    \t\t<span>\n                    \t\t\t                    \t\t</span>\n\n                    \t\t\t\t\t\t\t\t\t</td>\n        \t\t\t</tr>\n\t\t\t\t\t\t\t\t\t</tbody>\n\t</table>");
        filmwebPlugin.updateMediaInfo(movie);
        
        LinkedHashSet<String> testCast = new LinkedHashSet<String>();
        // These need to be in the same order as the web page
        testCast.add("Jerzy Stuhr");
        testCast.add("Olgierd Łukaszewicz");

        assertEquals(Arrays.asList(testCast.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
    }

    public void testUpdateMediaInfoPosterURL() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://seksmisja.filmweb.pl");
        filmwebPlugin.setRequestResult("<div class=\"film-poster\">\n\n            \t\t\t\t\t                        \t\t\t            \t\t\t\t\t            \t\t\t\t\t            \t\t\t\t\t\t<a rel=\"artshow\" href=\"http://gfx.filmweb.pl/po/11/63/1163/6900169.3.jpg?l=1185190085000\">\n\t\t\t\t\t\t\t\t\t\t<img src=\"http://gfx.filmweb.pl/po/11/63/1163/6900169.2.jpg?l=1185190085000\" alt=\"Seksmisja (1984)\" title=\"Seksmisja (1984)\" />\n\t\t\t\t\t\t\t\t\t</a>\n            \t\t\t\t\t\t\t\t\t\t\t\t</div>");
        filmwebPlugin.updateMediaInfo(movie);
        // assertEquals("http://gfx.filmweb.pl/po/11/63/1163/6900169.3.jpg?l=1185190085000", movie.getPosterURL());
    }

    public void testUpdateMediaInfoUpdateTVShowInfo() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://prison.break.filmweb.pl");
        movie.setSeason(4);
        MovieFile episode = new MovieFile();
        episode.setPart(1);
        movie.addMovieFile(episode);
        episode = new MovieFile();
        episode.setPart(2);
        movie.addMovieFile(episode);
        filmwebPlugin = new FilmwebPluginMock(offline) {
            public String getRequestResult(URL url) {
                if ("http://prison.break.filmweb.pl/f236096/Skazany+na+%C5%9Bmier%C4%87,2005/odcinki".equals(url.toString())) {
                    return "<tr>\n\t\t\t\t\t\t\t\t\t\t<td colspan=\"3\"><h2 id=\"seria4\" style=\"padding-top:10px\">sezon 4</h2></td>\n\t\t\t\t</tr>\n\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"text-align:right\">\n\t\t\t\t\t\t\t\t\todcinek 1\n\t\t\t\t\t\n\t\t\t\t\t\t\t\t\t\t\t</td>\n\t\t\t<td style=\"text-align:right\">\n\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1\n\t\t\t\t\t\twrześnia\t\t\t\t2008\n\t USA<br/>\n\t\t\t\t\t\t\t\t\t\t\t\t</td>\n\t\t\t<td style=\"text-align:right\">\n\t\t\t\t\t\t\t\t\tScylla\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t<tr>\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"text-align:right\">\n\t\t\t\t\t\t\t\t\todcinek 2\n\t\t\t\t\t\n\t\t\t\t\t\t\t\t\t\t\t</td>\n\n\t\t\t<td style=\"text-align:right\">\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1\n\t\t\t\t\t\twrześnia\t\t\t\t2008\n\t USA<br/>\n\t\t\t\t\t\t\t\t\t\t\t\t</td>\n\t\t\t<td style=\"text-align:right\">\n\t\t\t\t\t\t\t\t\tBreaking and Entering\n\t\t\t\t\t\t\t</td>\n\t\t\t\t\t\t\t\t\t</tr>\n\t\t\t\t\t<tr>";
                } else {
                    return "<li><a title=\"Skazany na śmierć / Prison Break (2005)  - odcinki - FILMWEB.pl\" href=\"http://prison.break.filmweb.pl/f236096/Skazany+na+%C5%9Bmier%C4%87,2005/odcinki\">odcinki</a> [66] &raquo;</li>";
                }
            }
        };
        filmwebPlugin.filmwebPlot = "short";
        filmwebPlugin.updateMediaInfo(movie);

        Iterator<MovieFile> episodesIt = movie.getFiles().iterator();
        assertEquals("Scylla", episodesIt.next().getTitle());
        assertEquals("Breaking and Entering", episodesIt.next().getTitle());
    }

    public void testUpdateMediaInfoNotOverwrite() {
        movie.setId(FilmwebPlugin.FILMWEB_PLUGIN_ID, "http://john.rambo.filmweb.pl");
        movie.setDirector("John Doe");
        movie.setRating(30);
        movie.setPlot("Ble ble ble");
        filmwebPlugin.setRequestResult("<p class=\"film-info\">\nreżyseria\t<a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>scenariusz<a href=\"http://www.filmweb.pl/o70013/Art+Monterastelli\" title=\"Art Monterastelli- filmografia - FILMWEB.pl\">Art Monterastelli</a>, <a href=\"http://sylvester.stallone.filmweb.pl/\" title=\"Sylvester Stallone- filmografia - FILMWEB.pl\">Sylvester Stallone</a>zdjęcia<a href=\"http://www.filmweb.pl/o11625/Glen+MacPherson\" title=\"Glen MacPherson- filmografia - FILMWEB.pl\">Glen MacPherson</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\nmuzyka<a href=\"http://www.filmweb.pl/o48282/Brian+Tyler\" title=\"Brian Tyler- filmografia - FILMWEB.pl\">Brian Tyler</a><a href=\"http://john.rambo.filmweb.pl/f219806/John+Rambo,2008/obsada\">(więcej...)</a>\n\n\nczas trwania: 91<abbr title=\"Dystrybucja\">dyst.:</abbr>\n<a rel=\"nofollow\" class=\"external\" href=\"http://www.monolith.pl\">Monolith Films</a></p>");
        filmwebPlugin.updateMediaInfo(movie);
        assertEquals("John Doe", movie.getDirector());
        assertEquals(30, movie.getRating());
        assertEquals("Ble ble ble", movie.getPlot());
    }

    class FilmwebPluginMock extends FilmwebPlugin {
        private String requestResult;
        private boolean offline;

        public FilmwebPluginMock(boolean offline) {
            this.offline = offline;
            super.init();
        }

        public void init() {
            webBrowser = new WebBrowser() {
                public String request(URL url) throws IOException {
                    if (offline) {
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
