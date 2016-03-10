/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
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
package com.moviejukebox.tools;

import com.moviejukebox.AbstractTests;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stuart
 */
public class DateTimeToolsTest extends AbstractTests {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeToolsTest.class);

    @BeforeClass
    public static void configure() {
        doConfiguration();
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testConvertDateToString_Date() {
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
    public void testParseDateToString() {
        LOG.info("parseDateTo");
        String dateToParse = "30 September 2010 (China)";
        String expResult = "2010-09-30";
        String result = DateTimeTools.parseDateToString(dateToParse);
        assertEquals(expResult, result);
    }
}
