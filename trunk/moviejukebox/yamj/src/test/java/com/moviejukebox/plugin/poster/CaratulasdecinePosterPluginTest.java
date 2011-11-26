/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.Image;
import com.moviejukebox.model.IImage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Stuart
 */
public class CaratulasdecinePosterPluginTest {
    private static final CaratulasdecinePosterPlugin cPlugin = new CaratulasdecinePosterPlugin();
    
    public CaratulasdecinePosterPluginTest() {
    }

    /**
     * Test of getIdFromMovieInfo method, of class CaratulasdecinePosterPlugin.
     */
    @Test
    public void testGetIdFromMovieInfo() {
        String title = "Avatar";
        String year = "2009";
        String expResult = "4736";
        String result = cPlugin.getIdFromMovieInfo(title, year);
        assertEquals(expResult, result);
    }

    /**
     * Test of getPosterUrl method, of class CaratulasdecinePosterPlugin.
     */
    @Test
    public void testGetPosterUrl_String() {
        String idForAvatar = "4736";
        IImage result = cPlugin.getPosterUrl(idForAvatar);
        
        assertEquals("http://www.caratulasdecine.com/Caratulas5/avatar.jpg", result.getUrl());
    }

    /**
     * Test of getPosterUrl method, of class CaratulasdecinePosterPlugin.
     */
    @Test
    public void testGetPosterUrl_String_String() {
        String title = "Avatar";
        String year = "2009";
        IImage result = cPlugin.getPosterUrl(title, year);
        
        assertEquals("http://www.caratulasdecine.com/Caratulas5/avatar.jpg", result.getUrl());
    }

    /**
     * Test of getName method, of class CaratulasdecinePosterPlugin.
     */
    @Test
    public void testGetName() {
        String result = cPlugin.getName();
        assertEquals("caratulasdecine", result);
    }
}
