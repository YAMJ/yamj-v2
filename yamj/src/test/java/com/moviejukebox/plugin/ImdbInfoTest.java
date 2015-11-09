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
package com.moviejukebox.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.moviejukebox.tools.PropertiesUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbInfoTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbInfoTest.class);
    private static final String[] SITES = new String[]{"www","labs","akas"};
    
    @Test
    public void testImdbPersonId() {
        LOG.info("testImdbPersonId");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbPersonId("Renée Zellweger");
            assertEquals("IMDb site "+site, "nm0000250", id);
        }
    }

    @Test
    public void testImdbMovieId_VariableOff() {
        LOG.info("testImdbMovieId_VariableOff");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertNotEquals("IMDb site "+site, "tt1611224", id); // correct one
        }
    }

    @Test
    public void testImdbMovieId_VariableOn() {
        LOG.info("testImdbMovieId_VariableOn");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", true);
            ImdbInfo imdbInfo = new ImdbInfo();
    
            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertEquals("IMDb site "+site, "tt1611224", id);
        }
    }

    @Test
    public void testImdbMovieIdFirstMatch() {
        LOG.info("testImdbMovieIdFirstMatch");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();
    
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("IMDb site "+site, "tt0499549", id);
        }
    }

    @Test
    public void testImdbMovieIdRegularMatch() {
        LOG.info("testImdbMovieIdRegularMatch");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "regular");
            PropertiesUtil.setProperty("imdb.id.search.variable", false);
            ImdbInfo imdbInfo = new ImdbInfo();
    
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("IMDb site "+site, "tt0499549", id);
        }
    }

    @Test
    public void testImdbMovieIdExactMatch() {
        LOG.info("testImdbMovieIdExactMatch");
        for (String site : SITES) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "exact");
            PropertiesUtil.setProperty("imdb.id.search.variable", true);
            ImdbInfo imdbInfo = new ImdbInfo();
    
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("IMDb site "+site, "tt0499549", id);
        }
    }
}
