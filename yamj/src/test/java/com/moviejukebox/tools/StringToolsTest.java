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
package com.moviejukebox.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of utility functions for strings
 *
 * @author Stuart
 */
public class StringToolsTest {

    private static final Logger LOG = LoggerFactory.getLogger(StringToolsTest.class);

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

        List<String[]> tests = new ArrayList<>();
        // base path, additional path, expected result
        tests.add(new String[]{"base", "additional", "base" + File.separator + "additional"});
        tests.add(new String[]{"\\base", "additional", "" + File.separator + "base" + File.separator + "additional"});
        tests.add(new String[]{"/base", "additional", "" + File.separator + "base" + File.separator + "additional"});
        tests.add(new String[]{"base", "\\additional", "base" + File.separator + "additional"});
        tests.add(new String[]{"base", "/additional", "base" + File.separator + "additional"});

        String base, additional, actual, expected;
        int count = 1;
        // Process the test cases
        for (String[] test : tests) {
            base = test[0];
            additional = test[1];
            expected = test[2];
            LOG.info("Test #{}: '{}' + '{}'", count, base, additional);
            actual = StringTools.appendToPath(base, additional);
            LOG.info("Test #{}: '{}'", count, actual);
            assertEquals("Failed appendToPath, test #" + count, expected, actual);
            count++;
        }
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
        // Test for a different start to the certification string
        String mpaa = "Certificate 12 for something silly";
        String result = StringTools.processMpaaCertification("Certificate", mpaa);
        assertEquals("12", result);

        // Test the standard certification Strings
        Map<String, String> certs = new HashMap<>();
        certs.put("Rated PG-13 for some violent images and brief nudity", "PG-13");
        certs.put("Rated R for some violence (Redux version)", "R");

        for (Map.Entry<String, String> entry : certs.entrySet()) {
            LOG.info("Checking '{}'", entry.getKey());
            result = StringTools.processMpaaCertification(entry.getKey());
            assertEquals("Failed to get correct certification", entry.getValue(), result);
        }
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

    /**
     * Test of parseRating method, of class StringTools.
     */
    @Test
    public void testParseRating() {
        LOG.info("replaceQuotes");
        assertEquals(41, StringTools.parseRating("4.1"));
        assertEquals(89, StringTools.parseRating("8.88"));
        assertEquals(0, StringTools.parseRating("0"));
        assertEquals(99, StringTools.parseRating("9,9"));
        assertEquals(-1, StringTools.parseRating("UNKNOWN"));
        assertEquals(-1, StringTools.parseRating("-100"));
        assertEquals(100, StringTools.parseRating("11"));
    }

    /**
     * Test of getWords method, of class StringTools.
     */
    @Test
    public void testGetWords() {
        System.out.println("getWords");
        String result = StringTools.getWords("This is a test sentance", 2);
        assertEquals("This is", result);

        result = StringTools.getWords("Single", 2);
        assertEquals("Single", result);

        result = StringTools.getWords("Single", 1);
        assertEquals("Single", result);

        result = StringTools.getWords("Test sentance", 5);
        assertEquals("Test sentance", result);
    }

}
