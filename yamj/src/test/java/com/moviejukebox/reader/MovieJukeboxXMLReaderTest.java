/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.reader;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.moviejukebox.model.Person;

/**
 *
 * @author stuart.boston
 */
public class MovieJukeboxXMLReaderTest {

    private static final String testDir = "src/test/java/TestFiles/";

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
