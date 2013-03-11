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
import org.junit.Test;

public class SearchEngineToolsTest {

    @Test
    public void roundTripMovieOFDB() {
        SearchEngineTools search = new SearchEngineTools();
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.ofdb.de/film");
            assertEquals("http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora", url);
        }
    }

    @Test
    public void roundTripMovieIMDB() {
        SearchEngineTools search = new SearchEngineTools();
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.imdb.com/title");
            assertEquals("http://www.imdb.com/title/tt0499549/", url);
        }
    }

    @Test
    public void roundTripMovieFilmweb() {
        SearchEngineTools search = new SearchEngineTools();
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.filmweb.pl");
            assertEquals("http://www.filmweb.pl/Avatar", url);
        }
    }

    @Test
    public void roundTripMovieFilmDelta() {
        SearchEngineTools search = new SearchEngineTools();
        
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.filmdelta.se/filmer");
            assertEquals("http://www.filmdelta.se/filmer/144938/avatar/", url);
        }
    }

    @Test
    public void roundTripMovieSratim() {
        SearchEngineTools search = new SearchEngineTools();
        search.setSearchSuffix("/view.php");
        
        for (int i=0;i<search.countSearchSites();i++) {
            String url = search.searchMovieURL("Avatar", "2009", "www.sratim.co.il");
            assertTrue(url.startsWith("http://www.sratim.co.il/view.php"));
            assertTrue(url.contains("id=143628"));
        }
    }
}
