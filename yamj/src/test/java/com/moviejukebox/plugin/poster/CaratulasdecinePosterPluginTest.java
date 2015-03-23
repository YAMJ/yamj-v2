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

import static org.junit.Assert.*;

import org.junit.Test;

import com.moviejukebox.tools.PropertiesUtil;
import org.junit.Before;
import org.junit.BeforeClass;

public class CaratulasdecinePosterPluginTest {

    CaratulasdecinePosterPlugin posterPlugin;

    @BeforeClass
    public static void configure() {
        PropertiesUtil.setProperty("poster.scanner.SearchPriority.movie", "caratulasdecine");
    }

    @Before
    public void setup() {
        posterPlugin = new CaratulasdecinePosterPlugin();
    }

    @Test
    public void testGetId() {

        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Millennium 1", null);
        assertEquals("4504", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/millennium1_loshombresquenoamabanalasmujeres.jpg", posterUrl);
    }

    @Test
    public void testGetList() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("El ultimátum de Bourne", null);
        assertEquals("3654", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas4/elultimatumdebourne.jpg", posterUrl);
    }

    @Test
    public void testGetTroya() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Troya", null);
        assertEquals("2141", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas1/Troya.jpg", posterUrl);
    }

    @Test
    public void testGetEnTierraHostil() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("En tierra hostil", null);
        assertEquals("4783", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/entierrahostil.jpg", posterUrl);
    }

    @Test
    public void testGetUp() {
        String idFromMovieInfo = posterPlugin.getIdFromMovieInfo("Up", null);
        assertEquals("4568", idFromMovieInfo);

        String posterUrl = posterPlugin.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/up.jpg", posterUrl);
    }
}
