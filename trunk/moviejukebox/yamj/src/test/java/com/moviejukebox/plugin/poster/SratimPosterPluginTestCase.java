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
import org.apache.log4j.BasicConfigurator;

import com.moviejukebox.tools.PropertiesUtil;

public class SratimPosterPluginTestCase extends TestCase {

    private SratimPosterPlugin posterPlugin;
    
    public SratimPosterPluginTestCase() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "sratim");
        posterPlugin = new SratimPosterPlugin();
    }

    public void testGetIdFromMovieInfo() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", null);
        assertEquals("http://www.sratim.co.il/movies/view.aspx?id=43628", idFromMovieInfo);
    }

    public void testGetIdFromMovieInfoTV() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("The Lost Islands", null, 1);
        assertEquals("http://www.sratim.co.il/movies/series/view.aspx?id=116", idFromMovieInfo);
    }

    public void testGetPosterUrl() {
        String posterUrl = posterPlugin.getPosterUrl("http://www.sratim.co.il/movies/view.aspx?id=43628").getUrl();
        assertEquals("http://www.sratim.co.il/movies/images/8/43628.jpg", posterUrl);
    }
}
