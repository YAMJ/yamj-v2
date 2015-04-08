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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author iuk
 */
public class TrailersLandPluginTest {

    private static final Logger LOG = LoggerFactory.getLogger(TrailersLandPluginTest.class);
    private static TrailersLandPlugin tlPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("trailers.download", false);
        tlPlugin = new TrailersLandPlugin();
    }

    @Before
    public void setup() {
    }

//    @Test
    public void testGenerateTitleOriginalTitle() {
        LOG.info("testGenerateTitleOriginalTitle");
        Movie movie = new Movie();
        movie.setTitle("Paradiso Amaro", Movie.UNKNOWN);
        movie.setOriginalTitle("The Descendants", Movie.UNKNOWN);

        assertTrue(tlPlugin.generate(movie));
    }

    @Test
    public void testTitle() {
        LOG.info("testTitle");
        Movie movie = new Movie();
        movie.setTitle("Operazione U.N.C.L.E.", Movie.UNKNOWN);

        assertTrue(tlPlugin.generate(movie));
    }
}
