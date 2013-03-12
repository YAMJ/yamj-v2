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
package com.moviejukebox.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class SearchEngineToolsTest {

    public SearchEngineToolsTest() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("mjb.ProxyHost", "wsw_b0");
        PropertiesUtil.setProperty("mjb.ProxyPort", 8080);
    }
    
    @Test
    public void roundTripIMDB() {
        SearchEngineTools search = new SearchEngineTools();

        // movie
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.imdb.com/title");
            assertEquals("http://www.imdb.com/title/tt0499549/", url);
        }
        // TV show, must leave out the year and search for TV series
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Two and a Half Men", null, "www.imdb.com/title", "TV series");
            assertEquals("http://www.imdb.com/title/tt0369179/", url);
        }
    }

    @Test
    public void roundTripOFDB() {
        SearchEngineTools search = new SearchEngineTools("de");
        
        // movie
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.ofdb.de/film");
            assertEquals("http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora", url);
        }
        // TV show
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Two and a Half Men", "2005", "www.ofdb.de/film");
            assertEquals("http://www.ofdb.de/film/66192,Mein-cooler-Onkel-Charlie", url);
        }
    }

    @Test
    public void roundTripMovieFilmDelta() {
        SearchEngineTools search = new SearchEngineTools("se");
        search.setSearchSites("google,blekko,lycos");
        
        // movie
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.filmdelta.se/filmer");
            assertEquals("http://www.filmdelta.se/filmer/144938/avatar/", url);
        }
        // TV show, must search for season
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Two and a Half Men", "2005", "www.filmdelta.se/filmer", "sasong_3");
            assertEquals("http://www.filmdelta.se/filmer/148233/two_and_a_half_men-sasong_3/", url);
        }
    }

    @Test
    public void roundTripAllocine() {
        SearchEngineTools search = new SearchEngineTools("fr");
 
        // movie, must set search suffix
        search.setSearchSuffix("/fichefilm_gen_cfilm");
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.allocine.fr/film");
            assertEquals("http://www.allocine.fr/film/fichefilm_gen_cfilm=61282.html", url);
        }
        // TV show, must set search suffix
        search.setSearchSuffix("/ficheserie_gen_cserie");
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Two and a Half Men", "2005", "www.allocine.fr/series");
            assertTrue(url, url.startsWith("http://www.allocine.fr/series/ficheserie_gen_cserie=132.html"));
        }
    }

    @Test
    public void roundTripMoviemeter() {
        SearchEngineTools search = new SearchEngineTools("no");
        
        // movie
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.moviemeter.nl/film");
            assertEquals("http://www.moviemeter.nl/film/17552", url);
        }
        // TV shows not supported
    }

    
    @Test
    public void fixRoundTripFilmweb() {
        SearchEngineTools search = new SearchEngineTools("pl");
        
        // movie
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.filmweb.pl");
            assertEquals("http://www.filmweb.pl/Avatar", url);
        }
        
        // TV show
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Two and a Half Men", "2005", "www.filmweb.pl");
            assertTrue(url.startsWith("http://www.filmweb.pl/serial/Dw%"));
        }
    }

    @Test
    public void fixRoundTripMovieSratim() {
        SearchEngineTools search = new SearchEngineTools("il");
        search.setSearchSuffix("/view.php");
        
        search.setSearchSites("google,yahoo,lycos,blekko");
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.sratim.co.il");
            assertTrue(url, url.startsWith("http://www.sratim.co.il/view.php"));
            assertTrue(url, url.contains("id=143628"));
        }
    }

    @Test
    public void fixRoundTripMovieComingSoon() {
        SearchEngineTools search = new SearchEngineTools("it");
        search.setSearchSites("google,yahoo,blekko");
        
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.comingsoon.it/Film/Scheda/Trama");
            assertTrue(url, url.startsWith("http://www.comingsoon.it/Film/Scheda/Trama/"));
            assertTrue(url, url.contains("key=846"));
        }
    }
}
