/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */
package com.moviejukebox.plugin.poster;

import org.junit.Test;
import static org.junit.Assert.*;

public class CaratulasdecinePosterPluginTest {

    CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();

    public CaratulasdecinePosterPluginTest() {
    }

    @Test
    public void testGetId() {

        String idFromMovieInfo = toTest.getIdFromMovieInfo("Millennium 1", null);
        assertEquals("4504", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/millennium1_loshombresquenoamabanalasmujeres.jpg", posterUrl);
    }

    @Test
    public void testGetList() {
        String idFromMovieInfo = toTest.getIdFromMovieInfo("El ultimátum de Bourne", null);
        assertEquals("3654", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas4/elultimatumdebourne.jpg", posterUrl);
    }

    @Test
    public void testGetTroya() {
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Troya", null);
        assertEquals("2141", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas1/Troya.jpg", posterUrl);
    }

    @Test
    public void testGetEnTierraHostil() {
        String idFromMovieInfo = toTest.getIdFromMovieInfo("En tierra hostil", null);
        assertEquals("4783", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/entierrahostil.jpg", posterUrl);
    }

    @Test
    public void testGetUp() {
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Up", null);
        assertEquals("4568", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo).getUrl();
        assertEquals("http://www.caratulasdecine.com/Caratulas5/up.jpg", posterUrl);
    }
}
