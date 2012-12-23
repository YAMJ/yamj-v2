/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Stuart
 */
public class FilmaffinityPluginTest {
    private FilmaffinityPlugin faPlugin = new FilmaffinityPlugin();

    public FilmaffinityPluginTest() {
        BasicConfigurator.configure();
        PropertiesUtil.setProperty("priority.movie.country", "filmaffinity,imdb");
    }

    /**
     * Test of scan method, of class FilmaffinityPlugin.
     */
    @Test
    public void testScan() {
        Movie movie = new Movie();
        movie.setTitle("Blade Runner", Movie.UNKNOWN);
        movie.setYear("1982", Movie.UNKNOWN);

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("film358476.html", movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));
        assertEquals("112m", movie.getRuntime());
        assertTrue(movie.getDirectors().size() > 0);
        assertTrue(movie.getWriters().size() > 0);
        assertTrue(movie.getCast().size() > 0);
        assertTrue(movie.getGenres().size() > 0);
    }

    @Test
    public void testScanNoYear() {
        Movie movie = new Movie();
        movie.setTitle("Avatar", Movie.UNKNOWN);

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("2009", movie.getYear());
    }

    @Test
    public void testScanCountry1() {
        Movie movie = new Movie();
        movie.setTitle("25 grados en invierno", Movie.UNKNOWN);
        assertEquals(true, faPlugin.scan(movie));
        assertEquals("Bélgica",movie.getCountry());
    }

    @Test
    public void testScanCountry2() {
        Movie movie = new Movie();
        movie.setTitle("Matrix Revolutions", Movie.UNKNOWN);
        assertEquals(true, faPlugin.scan(movie));
        assertEquals("Estados Unidos", movie.getCountry());
    }
}
