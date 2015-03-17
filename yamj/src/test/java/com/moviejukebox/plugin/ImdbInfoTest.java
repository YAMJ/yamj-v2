/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import com.moviejukebox.tools.PropertiesUtil;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbInfoTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbInfoTest.class);

//    @Test
    public void testImdbPersonId() {
        LOG.info("testImdbPersonId");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);

            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbPersonId("Renée Zellweger");
            assertEquals("nm0000250", id);
        }
    }

//    @Test
    public void testImdbMovieId_VariableOff() {
        LOG.info("testImdbMovieId_VariableOff");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertNotEquals("Search site " + site, "tt1611224", id); // correct one
        }
    }

//    @Test
    public void testImdbMovieId_VariableOn() {
        LOG.info("testImdbMovieId_VariableOn");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", true);
            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertEquals("Search site " + site, "tt1611224", id); // correct one
        }
    }

//    @Test
    public void testImdbMovieIdFirstMatch() {
        LOG.info("testImdbMovieIdFirstMatch");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("Search site " + site, "tt0499549", id); // correct one
        }
    }

//    @Test
    public void testImdbMovieIdRegularMatch() {
        LOG.info("testImdbMovieIdRegularMatch");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "regular");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("Search site " + site, "tt0499549", id); // correct one
        }
    }

    @Test
    public void testImdbMovieIdExactMatch() {
        LOG.info("testImdbMovieIdExactMatch");
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            LOG.info("Testing site '{}'", site);
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "exact");
            PropertiesUtil.setProperty("imdb.id.search.variable", true);
            ImdbInfo imdbInfo = new ImdbInfo();

            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("Search site " + site, "tt0499549", id);
        }
    }
}
