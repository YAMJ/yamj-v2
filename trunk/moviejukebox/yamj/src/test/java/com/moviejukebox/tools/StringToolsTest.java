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
package com.moviejukebox.tools;

import java.io.File;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author Stuart
 */
public class StringToolsTest {

    private static final Logger LOG = Logger.getLogger(StringToolsTest.class);

    public StringToolsTest() {
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
     * Test of characterMapReplacement method, of class StringTools.
     */
    @Ignore("Not tested")
    public void testCharacterMapReplacement() {
        LOG.info("characterMapReplacement");
        Character charToReplace = null;
        String expResult = "";
        String result = StringTools.characterMapReplacement(charToReplace);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of stringMapReplacement method, of class StringTools.
     */
    @Ignore("Not tested")
    public void testStringMapReplacement() {
        LOG.info("stringMapReplacement");
        String stringToReplace = "";
        String expResult = "";
        String result = StringTools.stringMapReplacement(stringToReplace);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of appendToPath method, of class StringTools.
     */
    @Test
    public void testAppendToPath() {
        LOG.info("appendToPath");
        String basePath = "base/";
        String additionalPath = "/additional/";

        String expResult = "base" + File.separator + "additional" + File.separator;
        String result = StringTools.appendToPath(basePath, additionalPath);
        assertEquals(expResult, result);
    }

    /**
     * Test of cleanString method, of class StringTools.
     */
    @Test
    public void testCleanString() {
        LOG.info("cleanString");
        String sourceString = "C-l(e)a#n'ed@ T[e]x[t]";
        String expResult = "C l e a n ed  T e x t";
        String result = StringTools.cleanString(sourceString);
        assertEquals(expResult, result);
    }

    /**
     * Test of formatFileSize method, of class StringTools.
     */
    @Test
    public void testFormatFileSize() {
        LOG.info("formatFileSize");
        long fileSize;
        String result;

        // Test Bytes
        fileSize = 900L;
        result = StringTools.formatFileSize(fileSize);
        assertEquals("Failed Bytes Test", "900 Bytes", result);
        // Test KB
        fileSize = 900000L;
        result = StringTools.formatFileSize(fileSize);
        assertEquals("Failed KB Test", "879 KB", result);
        // Test MB
        fileSize = 900000000L;
        result = StringTools.formatFileSize(fileSize);
        assertEquals("Failed MB Test", "858 MB", result);
        // Test GB
        fileSize = 900000000000L;
        result = StringTools.formatFileSize(fileSize);
        assertEquals("Failed GB Test", "838 GB", result);

    }

    /**
     * Test of isNotValidString method, of class StringTools.
     */
    @Test
    public void testIsNotValidString() {
        LOG.info("isNotValidString");
        String testString = "UNKOWN";
        boolean expResult = false;
        boolean result = StringTools.isNotValidString(testString);
        assertEquals(expResult, result);
    }

    /**
     * Test of isValidString method, of class StringTools.
     */
    @Test
    public void testIsValidString() {
        LOG.info("isValidString");
        String testString = "UNKNOWN";
        boolean expResult = false;
        boolean result = StringTools.isValidString(testString);
        assertEquals(expResult, result);
    }

    /**
     * Test of trimToLength method, of class StringTools.
     */
    @Test
    public void testTrimToLength_String_int() {
        LOG.info("trimToLength");
        int requiredLength = 10;

        String sourceString = "abc def ghi jkl mno pqr stu vwx yz";
        String expResult = "abc def...";
        String result = StringTools.trimToLength(sourceString, requiredLength);
        assertEquals(expResult, result);

        sourceString = "abcdefg hi jkl mno pqr stu vwx yz";
        expResult = "abcdefg...";
        result = StringTools.trimToLength(sourceString, requiredLength);
        assertEquals(expResult, result);

    }

    /**
     * Test of trimToLength method, of class StringTools.
     */
    @Test
    public void testTrimToLength_4args() {
        LOG.info("trimToLength");
        String sourceString = "abcdefghijklmnopqrstuvwxyz";
        int requiredLength = 10;
        boolean trimToWord = false;
        String endingSuffix = "???";
        String expResult = "abcdefg???";
        String result = StringTools.trimToLength(sourceString, requiredLength, trimToWord, endingSuffix);
        assertEquals(expResult, result);
    }

    /**
     * Test of castList method, of class StringTools.
     */
    @Ignore("Not tested")
    public void testCastList() {
    }

    /**
     * Test of splitList method, of class StringTools.
     */
    @Ignore("Not tested")
    public void testSplitList() {
    }

    /**
     * Test of tokenizeToArray method, of class StringTools.
     */
    @Ignore("Not tested")
    public void testTokenizeToArray() {
    }

    /**
     * Test of processMpaaCertification method, of class StringTools.
     */
    @Test
    public void testProcessMpaaCertification() {
        LOG.info("processMpaaCertification");
        String rating = "Rated";
        String mpaa = "Rated 12 for something silly";
        String expResult = "12";
        String result = StringTools.processMpaaCertification(rating, mpaa);
        assertEquals(expResult, result);
    }

    /**
     * Test of replaceQuotes method, of class StringTools.
     */
    @Test
    public void testReplaceQuotes() {
        LOG.info("replaceQuotes");
        String original = "Quote’s, Quote\"s, Quote`s, Quote's";
        String expResult = "Quote's, Quote's, Quote's, Quote's";

        String result = StringTools.replaceQuotes(original);
        assertEquals(expResult, result);
    }

}
