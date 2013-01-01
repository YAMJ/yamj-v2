/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.reader;

import com.moviejukebox.model.Person;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author stuart.boston
 */
public class MovieJukeboxXMLReaderTest {

    private static final String testDir = "src/test/java/TestFiles/";

    public MovieJukeboxXMLReaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parseMovieXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParseMovieXML() {
        System.out.println("parseMovieXML - no test written");
    }

    /**
     * Test of parseSetXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParseSetXML() {
        System.out.println("parseSetXML - no test written");
    }

    /**
     * Test of parsePersonXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParsePersonXML() {
        System.out.println("parsePersonXML");
        File xmlFile = new File(testDir + "ParsePersonTest.xml");
        System.out.println("Test file exists: " + xmlFile.exists());

        Person person = new Person();
        MovieJukeboxXMLReader instance = new MovieJukeboxXMLReader();
        boolean expResult = true;
        boolean result = instance.parsePersonXML(xmlFile, person);
        System.out.println("Person: " + person.toString());
        assertEquals(expResult, result);
    }
}
