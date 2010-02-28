/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import junit.framework.TestCase;

public class CaratulasdecinePosterPluginTestCase extends TestCase {

    public void testGetId() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Millennium I", null, -1);
        assertEquals("4504", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas5/millennium1_loshombresquenoamabanalasmujeres.jpg", posterUrl);
    }

    public void testGetList() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("El ultimátum de Bourne", null, 0);
        assertEquals("3654", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas4/elultimatumdebourne.jpg", posterUrl);
    }

    public void testGetTroya() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Troya", null, 0);
        assertEquals("2141", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas1/Troya.jpg", posterUrl);
    }

    public void testGetEnTierraHostil() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("En tierra hostil", null, 0);
        assertEquals("4783", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas5/entierrahostil.jpg", posterUrl);
    }

    public void testGetEnTierraHostilLong() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("En tierra hostil (The Hurt Locker)", null, 0);
//        assertEquals("4783", idFromMovieInfo);
//
//        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
//        assertEquals("http://www.caratulasdecine.com/Caratulas5/entierrahostil.jpg", posterUrl);
    }

    public void testGetUp() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Up", null, 0);
        assertEquals("3754", idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas4/shootemup.jpg", posterUrl);
    }

    //    

}
