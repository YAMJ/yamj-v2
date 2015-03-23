/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.tools.PropertiesUtil;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class TheTvDbPosterPluginTestCase {

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "thetvdb");
    }

    @Test
    public void testGetId() {
        TheTvDBPosterPlugin posterPlugin = new TheTvDBPosterPlugin();
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Friends", null, 1);
        assertEquals("79168", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo, 1).getUrl();
        assertEquals("http://thetvdb.com/banners/seasons/79168-1.jpg", posterUrl);

        posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo, 2).getUrl();
        assertEquals("http://thetvdb.com/banners/seasons/79168-2-4.jpg", posterUrl);
    }
}
