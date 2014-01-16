/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
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
package com.moviejukebox.tools;

import java.util.Date;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pojava.datetime2.DateTime;

/**
 *
 * @author Stuart
 */
public class DateTimeToolsTest {

    private static final Logger LOG = Logger.getLogger(DateTimeToolsTest.class);

    public DateTimeToolsTest() {
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
     * Test of convertDateToString method, of class DateTimeTools.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testConvertDateToString_Date() throws Exception {
        LOG.info("convertDateToString");
        Date convertDate = new Date(0);
        String result = DateTimeTools.convertDateToString(convertDate);
        assertEquals("1970-01-01", result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_DateTime() {
        LOG.info("convertDateToString");
        DateTime convertDate = new DateTime(0);
        String result = DateTimeTools.convertDateToString(convertDate);
        assertEquals("1970-01-01", result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_Date_String() {
        LOG.info("convertDateToString");
        Date convertDate = new Date(0);
        String dateFormat = "dd/MM/yyyy";
        String expResult = "01/01/1970";
        String result = DateTimeTools.convertDateToString(convertDate, dateFormat);
        assertEquals(expResult, result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_DateTime_String() {
        LOG.info("convertDateToString");
        DateTime convertDate = new DateTime(0);
        String dateFormat = "dd/MM/yyyy";
        String expResult = "01/01/1970";
        String result = DateTimeTools.convertDateToString(convertDate, dateFormat);
        assertEquals(expResult, result);
    }

    /**
     * Test of formatDuration method, of class DateTimeTools.
     */
    @Test
    public void testFormatDuration() {
        LOG.info("formatDuration");
        int duration = ((2 * 60) + (12)) * 60;
        String expResult = "2h 12m";
        String result = DateTimeTools.formatDuration(duration);
        assertEquals(expResult, result);
    }

    /**
     * Test of processRuntime method, of class DateTimeTools.
     */
    @Test
    public void testProcessRuntime() {
        LOG.info("processRuntime");
        String runtime = "1h 32m";
        int expResult = 92;
        int result = DateTimeTools.processRuntime(runtime);
        assertEquals(expResult, result);
    }

    /**
     * Test of parseDateTo method, of class DateTimeTools.
     */
    @Test
    public void testParseDateTo() {
        LOG.info("parseDateTo");
        String dateToParse = "30 September 2010 (China)";
        String targetFormat = "dd-MM-yyyy";
        String expResult = "30-09-2010";
        String result = DateTimeTools.parseDateTo(dateToParse, targetFormat);
        assertEquals(expResult, result);
    }
}
