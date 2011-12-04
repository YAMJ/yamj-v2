/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moviejukebox.writer;

import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.ImdbPlugin;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Stuart
 */
public class MovieJukeboxXMLWriterTest {

    private static final String testDir = "src/test/java/TestFiles/";

    public MovieJukeboxXMLWriterTest() {
    }

    /**
     * Test of parseMovieXML method, of class MovieJukeboxXMLWriter.
     */
    @Test
    public void testParseMovieXML() {
        File xmlFile = getTestFile("ExampleMovieXML.xml");
        Movie movie = new Movie();
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        boolean result = xmlWriter.parseMovieXML(xmlFile, movie);

        // Check that the scan was sucessful
        assertTrue(result);

//        System.out.println(movie.toString().replace("][", "\n").replace("[Movie [", "").replace("]]", ""));
        assertEquals("The Godfather", movie.getTitle());
        assertEquals("tt0068646", movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        assertNotNull(movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID));
        assertTrue(movie.getDirectors().size() > 0);
        assertTrue(movie.getCast().size() > 0);
        assertEquals(1, movie.getFiles().size());

        assertTrue(movie.getCodecs().size() == 2);
        
        MovieFile mf = movie.getFiles().iterator().next();
        assertEquals("99", mf.getAirsAfterSeason(1));
        assertEquals("Part Title", mf.getTitle(1));

//        System.out.println(mf.toString().replace("][", "\n").replace("[MovieFile [", "").replace("]]", "")); // XXX DEBUG

    }

    /**
     * Test of parsePersonXML method, of class MovieJukeboxXMLWriter.
     */
//    @Test
    public void testParsePersonXML() {
        System.out.println("parsePersonXML");
        File xmlFile = null;
        Person person = null;
        MovieJukeboxXMLWriter instance = new MovieJukeboxXMLWriter();
        boolean expResult = false;
        boolean result = instance.parsePersonXML(xmlFile, person);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    private File getTestFile(String filename) {
        File file = new File(testDir + filename);
        System.out.print("File:" + file.getAbsolutePath());
        System.out.print(" Length:" + file.length());
        System.out.println(" Exists: " + file.exists());
        return file;
    }
}
