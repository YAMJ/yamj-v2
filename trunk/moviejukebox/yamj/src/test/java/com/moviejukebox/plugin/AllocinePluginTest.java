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
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import static org.junit.Assert.*;

public class AllocinePluginTest {
    
    private AllocinePlugin allocinePlugin;
    
    public AllocinePluginTest() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("API_KEY_Allocine", "YW5kcm9pZC12M3M");
        PropertiesUtil.setProperty("mjb.includeEpisodePlots", "true");
        allocinePlugin = new AllocinePlugin();
    }

    @Test
    public void testMovie() {
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_MOVIE);
        movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, "45322");
        movie.setId(AllocinePlugin.IMDB_PLUGIN_ID, "tt0320691");
        
        allocinePlugin.scan(movie);
        assertEquals("Underworld", movie.getTitle());
        assertEquals("12", movie.getCertification());
    }

    @Test
    public void testTvSeries() {
        Movie movie = new Movie();
        movie.setMovieType(Movie.TYPE_TVSHOW);
        movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, "5676");
        movie.setId(AllocinePlugin.IMDB_PLUGIN_ID, "tt1634549");
        MovieFile mf = new MovieFile();
        mf.setSeason(1);
        mf.setFirstPart(1);
        mf.setLastPart(1);
        movie.addMovieFile(mf);
        
        allocinePlugin.scan(movie);
        assertEquals("Millennium", movie.getOriginalTitle());
        assertTrue(mf.getPlot(1).startsWith("Chaque année depuis quarante-quatre ans, le jour de son anniversaire, le président d’un "));
    }
}
