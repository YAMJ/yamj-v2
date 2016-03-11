/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.writer;

import com.moviejukebox.AbstractTests;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.reader.MovieJukeboxXMLReader;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Stuart
 */
public class MovieJukeboxXMLWriterTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(MovieJukeboxXMLWriterTest.class);

    public MovieJukeboxXMLWriterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        doConfiguration();
    }

    /**
     * Test of parseMovieXML method, of class MovieJukeboxXMLWriter.
     */
    @Test
    public void testParseMovieXML() {
        LOG.info("ParseMovieXML");
        File xmlFile = getTestFile("ExampleMovieXML.xml");
        Movie movie = new Movie();

        // Set up the extra files
        ExtraFile ef1 = new ExtraFile();
        ef1.setFilename("file:///opt/sybhttpd/localhost.drives/SATA_DISK/Movies2/The%20Godfather%20%281972%29.%5BBONUS-Making%20of%5D.avi");

        ExtraFile ef2 = new ExtraFile();
        ef2.setFilename("file:///opt/sybhttpd/localhost.drives/SATA_DISK/Movies2/The%20Godfather%20%281972%29.%5BTRAILER-Theatrical%20Trailer%5D.avi");
        movie.addExtraFile(ef1);
        movie.addExtraFile(ef2);
        // END of extra files

        MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();
        boolean result = xmlReader.parseMovieXML(xmlFile, movie);

        // Check that the scan was sucessful
        assertTrue(result);

        assertEquals("The Godfather", movie.getTitle());
        assertEquals("tt0068646", movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        assertNotNull(movie.getRating(ImdbPlugin.IMDB_PLUGIN_ID));
        assertTrue(movie.getDirectors().size() > 0);
        assertTrue(movie.getCast().size() > 0);
        assertTrue(movie.getCodecs().size() == 2);

        // Check the movie files
        assertEquals(1, movie.getFiles().size());
        for (MovieFile mf : movie.getFiles()) {
            assertEquals("99", mf.getAirsAfterSeason(1));
            assertEquals("Part Title", mf.getTitle(1));
        }
        // Check the extra files
        assertEquals(2, movie.getExtraFiles().size());
    }

    /**
     * Test of parsePersonXML method, of class MovieJukeboxXMLWriter.
     */
    @Ignore("Need to write test")
    public void testParsePersonXML() {
        LOG.info("ParsePersonXML");
        File xmlFile = null;
        Person person = null;
        MovieJukeboxXMLReader instance = new MovieJukeboxXMLReader();
        boolean expResult = false;
        boolean result = instance.parsePersonXML(xmlFile, person);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    public void testMultiPartFileXML() {
        LOG.info("MultiPartFileXML");

        File xmlFile = getTestFile("ExampleMultiPartFile.xml");
        Movie movie = new Movie();

        MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();
        boolean result = xmlReader.parseMovieXML(xmlFile, movie);

        // Check that the scan was sucessful
        assertTrue(result);
        assertEquals("Incorrect number of files", 2, movie.getFiles().size());

        for (MovieFile mf : movie.getFiles()) {
            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                assertTrue("Missing part title", !Movie.UNKNOWN.equalsIgnoreCase(mf.getTitle(part)));
            }
        }
    }

}
