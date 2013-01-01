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
package com.moviejukebox.plugin.poster;

import junit.framework.TestCase;

public class FilmAffinityPosterPluginTestCase extends TestCase {

    public void testGetId() {
        FilmAffinityPosterPlugin toTest = new FilmAffinityPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Avatar", "2009", -1);
        assertEquals("495280.html", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://pics.filmaffinity.com/Avatar-208925608-large.jpg", posterUrl);

        idFromMovieInfo = toTest.getIdFromMovieInfo("Troya", "2004", -1);
        assertEquals("564615.html", idFromMovieInfo);

        posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://pics.filmaffinity.com/Troya_Troy-963506535-large.jpg", posterUrl);

        toTest.getPosterUrl("Troya", null);
        assertEquals("http://pics.filmaffinity.com/Troya_Troy-963506535-large.jpg", posterUrl);

        idFromMovieInfo = toTest.getIdFromMovieInfo("FUTURAMA",null,2);
        assertEquals("826281.html", idFromMovieInfo);

        posterUrl =toTest.getPosterUrl(idFromMovieInfo).getUrl();

        assertEquals("http://pics.filmaffinity.com/Futurama_Serie_de_TV-151391426-large.jpg", posterUrl);


        idFromMovieInfo = toTest.getIdFromMovieInfo("Crepusculo",null,-1);
        assertEquals("826281.html", idFromMovieInfo);

        posterUrl =toTest.getPosterUrl(idFromMovieInfo).getUrl();

        assertEquals("http://pics.filmaffinity.com/Futurama_Serie_de_TV-151391426-large.jpg", posterUrl);
    }
}
