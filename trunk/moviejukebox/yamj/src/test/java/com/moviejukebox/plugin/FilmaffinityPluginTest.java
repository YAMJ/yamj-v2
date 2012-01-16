/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
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
    }

    /**
     * Test of scan method, of class FilmaffinityPlugin.
     */
    @Test
    public void testScan() {
        Movie movie = new Movie();
        movie.setTitle("Blade Runner");
        movie.setYear("1982");

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
        movie.setTitle("Avatar");

        assertEquals(true, faPlugin.scan(movie));
        assertEquals("2009", movie.getYear());
    }
}
