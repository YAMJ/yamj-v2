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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.tools.PropertiesUtil;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class AllocinePosterPluginTestCase {

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties");
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "allocine");
    }

    @Test
    public void testGetId() {
        AllocinePosterPlugin posterPlugin = new AllocinePosterPlugin();
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Avatar", "2009");
        assertEquals("61282", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://fr.web.img1.acsta.net/medias/nmedia/18/78/95/70/19485155.jpg", posterUrl);
    }
}
