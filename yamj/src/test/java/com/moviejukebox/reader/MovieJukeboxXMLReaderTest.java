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
package com.moviejukebox.reader;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.moviejukebox.model.Person;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author stuart.boston
 */
public class MovieJukeboxXMLReaderTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(MovieJukeboxXMLReaderTest.class);

    @BeforeClass
    public static void setUpClass() {
        doConfiguration();
    }

    /**
     * Test of parseMovieXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParseMovieXML() {
        LOG.info("parseMovieXML - no test written");
    }

    /**
     * Test of parseSetXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParseSetXML() {
        LOG.info("parseSetXML - no test written");
    }

    /**
     * Test of parsePersonXML method, of class MovieJukeboxXMLReader.
     */
    @Test
    public void testParsePersonXML() {
        LOG.info("parsePersonXML");
        File xmlFile = getTestFile("ParsePersonTest.xml");

        Person person = new Person();
        MovieJukeboxXMLReader instance = new MovieJukeboxXMLReader();
        boolean expResult = true;
        boolean result = instance.parsePersonXML(xmlFile, person);
        LOG.info("Person: " + person.toString());
        assertEquals(expResult, result);
    }
}
