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

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.assertEquals;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class SratimPluginTest {
    
    private SratimPlugin sratimPlugin;
    
    public SratimPluginTest() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("mjb.internet.plugin", "com.moviejukebox.plugin.SratimPlugin");
        sratimPlugin = new SratimPlugin();
    }
    
    @Test
    public void testMovie() {
        Movie movie = new Movie();
        movie.addMovieFile(new MovieFile());
        movie.setMovieType(Movie.TYPE_MOVIE);
        movie.setTitle("The Croods", Movie.UNKNOWN);
        movie.setId(AllocinePlugin.IMDB_PLUGIN_ID, "tt0481499");
        
        sratimPlugin.scan(movie);
        assertEquals("1123786", movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
        assertEquals("כריס סנדרס", movie.getDirector());
    }
}
