package com.moviejukebox.plugin;

import junit.framework.TestCase;

public class CaratulasdecinePosterPluginTestCase extends TestCase {

    public void testGetId() {
        CaratulasdecinePosterPlugin toTest = new CaratulasdecinePosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Millennium I", null, false);
        assertEquals("4504", idFromMovieInfo);
        
        String posterUrl = toTest.getPosterUrl(idFromMovieInfo);
        assertEquals("http://www.caratulasdecine.com/Caratulas5/millennium1_loshombresquenoamabanalasmujeres.jpg", posterUrl);
    }
}
