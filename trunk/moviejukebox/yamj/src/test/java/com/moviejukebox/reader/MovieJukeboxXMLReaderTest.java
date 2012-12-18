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
