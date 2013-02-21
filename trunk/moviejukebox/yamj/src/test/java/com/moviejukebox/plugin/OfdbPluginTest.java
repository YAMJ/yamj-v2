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

import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

public class OfdbPluginTest extends TestCase {

	private OfdbPlugin ofdbPlugin;
	
    static {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("mjb.internet.plugin", "com.moviejukebox.plugin.OfdbPlugin");
        PropertiesUtil.setProperty("mjb.ProxyHost", "wsw_b0");
        PropertiesUtil.setProperty("mjb.ProxyPort", 8080);
    }

    @Override
    protected void setUp() {
        ofdbPlugin = new OfdbPlugin();
    }

    public void testScan() {
    	Movie movie = new Movie();
    	movie.setId(OfdbPlugin.OFDB_PLUGIN_ID,"http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora");
    	ofdbPlugin.scan(movie);
    	
    	assertEquals("Avatar - Aufbruch nach Pandora", movie.getTitle());
    	assertEquals("Avatar", movie.getOriginalTitle());
    	assertEquals("2009", movie.getYear());
    	assertEquals("Großbritannien", movie.getCountry());
    	assertFalse(Movie.UNKNOWN.equals(movie.getPlot()));
    	assertFalse(Movie.UNKNOWN.equals(movie.getOutline()));
    	assertTrue(movie.getGenres().contains("Abenteuer"));
    	assertTrue(movie.getGenres().contains("Action"));
    	assertTrue(movie.getGenres().contains("Science-Fiction"));
    	
        LinkedHashSet<String> testList = new LinkedHashSet<String>();
        testList.add("James Cameron");
        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getDirectors().toArray(), 1)).toString());
    	
        testList.clear();
        testList.add("Sam Worthington");
        testList.add("Zoe Saldana");
        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());

        testList.clear();
        testList.add("James Cameron");
        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getWriters().toArray(), 1)).toString());
    }
}
