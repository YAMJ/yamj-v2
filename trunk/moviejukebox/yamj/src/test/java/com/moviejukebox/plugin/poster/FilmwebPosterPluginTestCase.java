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

public class FilmwebPosterPluginTestCase extends TestCase {
    FilmwebPosterPlugin posterPlugin = new FilmwebPosterPlugin();

    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://www.filmweb.pl/Avatar", idFromMovieInfo);
    }

    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Prison Break", null, 1);
        assertEquals("http://www.filmweb.pl/Prison.Break", idFromMovieInfo);
    }

    public void testGetPosterUrl() {
        String posterUrl = posterPlugin.getPosterUrl("http://www.filmweb.pl/Avatar").getUrl();
        assertEquals("http://gfx.filmweb.pl/po/91/13/299113/7322782.3.jpg?l=1270132598000", posterUrl);
    }
}
