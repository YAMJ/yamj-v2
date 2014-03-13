/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.model.Movie;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class StringTools {

    private static final Logger LOG = LoggerFactory.getLogger(StringTools.class);
    private static final Pattern CLEAN_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    // Number formatting
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * KB * KB;
    private static final DecimalFormat FILESIZE_FORMAT_0 = new DecimalFormat("0");
    private static final DecimalFormat FILESIZE_FORMAT_1 = new DecimalFormat("0.#");
    private static final DecimalFormat FILESIZE_FORMAT_2 = new DecimalFormat("0.##");
    private static final Map<Character, Character> CHAR_REPLACEMENT_MAP = new HashMap<Character, Character>();
    // Quote replacements
    private static final String QUOTE_SINGLE = "\'";
    private static final Pattern QUOTE_PATTERN = Pattern.compile(generateQuoteList());
    // Literals
    private static final String MPPA_RATED = "Rated";

    private StringTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    static {
        // Populate the charReplacementMap
        String temp = PropertiesUtil.getProperty("indexing.character.replacement", "");
        //String temp = PropertiesUtil.getProperty("mjb.charset.filename.translate", "");
        StringTokenizer tokenizer = new StringTokenizer(temp, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            int idx = token.indexOf('-');
            if (idx > 0) {
                String key = token.substring(0, idx).trim();
                String value = token.substring(idx + 1).trim();
                if (key.length() == 1 && value.length() == 1) {
                    CHAR_REPLACEMENT_MAP.put(Character.valueOf(key.charAt(0)), Character.valueOf(value.charAt(0)));
                }
            }
        }
    }

    /**
     * Generate the pattern string for all available quote marks
     *
     * @return
     */
    private static String generateQuoteList() {
        Set<String> quotes = new HashSet<String>();
        // Double quote - "
        quotes.add("\"");
        // Single left quote - ‘
        quotes.add("\u2018");
        // Single right quote - ’
        quotes.add("\u2019");
        // Double left quote - “
        quotes.add("\u201C");
        // Double right quote - ”
        quotes.add("\u201D");
        // Low single quote - ‚
        quotes.add("\u201A");
        // Low double quote - „
        quotes.add("\u201E");
        // Backtick "quote" - `
        quotes.add("`");
        // Odd quote character that comes across from TheTVDB
        quotes.add("â€™");
        // Add the XML version of '
        quotes.add("&#x27;");

        StringBuilder quoteString = new StringBuilder();
        for (String quote : quotes) {
            quoteString.append(quote).append("|");
        }
        quoteString.deleteCharAt(quoteString.length() - 1);
        return quoteString.toString();
    }

    /**
     * Check the passed character against the replacement list.
     *
     * @param charToReplace
     * @return
     */
    public static String characterMapReplacement(Character charToReplace) {
        Character tempC = CHAR_REPLACEMENT_MAP.get(charToReplace);
        if (tempC == null) {
            return charToReplace.toString();
        } else {
            return tempC.toString();
        }
    }

    /**
     * Change all the characters in a string to the safe replacements
     *
     * @param stringToReplace
     * @return
     */
    public static String stringMapReplacement(String stringToReplace) {
        Character tempC;
        StringBuilder sb = new StringBuilder();

        for (Character c : stringToReplace.toCharArray()) {
            tempC = CHAR_REPLACEMENT_MAP.get(c);
            if (tempC == null) {
                sb.append(c);
            } else {
                sb.append(tempC);
            }
        }
        return sb.toString();
    }

    /**
     * Append a string to the end of a path ensuring that there are the correct number of File.separators
     *
     * @param basePath
     * @param additionalPath
     * @return
     */
    public static String appendToPath(final String basePath, final String additionalPath) {
        String tmpAdditionalPath;
        if (additionalPath.startsWith("\\") || additionalPath.startsWith("/")) {
            // Remove any path characters from the additional path as this interferes with the conncat
            tmpAdditionalPath = additionalPath.substring(1);
        } else {
            tmpAdditionalPath = additionalPath;
        }

        return FilenameUtils.concat(basePath, tmpAdditionalPath);
    }

    /**
     * Strip all non-alphanumeric characters from a string replacing with a space
     *
     * @param sourceString
     * @return
     */
    public static String cleanString(String sourceString) {
        return CLEAN_STRING_PATTERN.matcher(sourceString).replaceAll(" ").trim();
    }

    /**
     * Format the file size
     *
     * @param fileSize
     * @return
     */
    public static String formatFileSize(long fileSize) {

        String returnSize;
        if (fileSize < KB) {
            returnSize = fileSize + " Bytes";
        } else {
            String appendText;
            long divider;

            // resolve text to append and divider
            if (fileSize < MB) {
                appendText = " KB";
                divider = KB;
            } else if (fileSize < GB) {
                appendText = " MB";
                divider = MB;
            } else {
                appendText = " GB";
                divider = GB;
            }

            // resolve decimal format
            DecimalFormat df;
            long checker = (fileSize / divider);
            if (checker < 10) {
                df = FILESIZE_FORMAT_2;
            } else if (checker < 100) {
                df = FILESIZE_FORMAT_1;
            } else {
                df = FILESIZE_FORMAT_0;
            }

            // build string
            returnSize = df.format((float) ((float) fileSize / (float) divider)) + appendText;
        }

        return returnSize;
    }

    /**
     * Check the string passed to see if it is invalid. Invalid strings are "UNKNOWN", null or blank
     *
     * @param testString The string to test
     * @return True if the string is invalid, Boolean.FALSE otherwise
     */
    public static boolean isNotValidString(String testString) {
        return !isValidString(testString);
    }

    /**
     * Check the string passed to see if it contains a value.
     *
     * @param testString The string to test
     * @return False if the string is empty, null or UNKNOWN, True otherwise
     */
    public static boolean isValidString(String testString) {
        // Checks if a String is whitespace, empty ("") or null.
        if (StringUtils.isBlank(testString)) {
            return Boolean.FALSE;
        }

        if (testString.equalsIgnoreCase(Movie.UNKNOWN)) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    /**
     * Check that the passed string is not longer than the required length and trim it if necessary.
     *
     * @param sourceString
     * @param requiredLength
     * @return
     */
    public static String trimToLength(String sourceString, int requiredLength) {
        return trimToLength(sourceString, requiredLength, Boolean.TRUE, "...");
    }

    /**
     * Check that the passed string is not longer than the required length and trim it if necessary
     *
     * @param sourceString The string to check
     * @param requiredLength The required length (Maximum)
     * @param trimToWord Trim the source string to the last space to avoid partial words
     * @param endingSuffix The ending to append if the string is longer than the required length
     * @return
     */
    public static String trimToLength(String sourceString, int requiredLength, boolean trimToWord, String endingSuffix) {
        String changedString = sourceString.trim();

        if (isValidString(changedString)) {
            if (changedString.length() <= requiredLength) {
                // No need to do anything
                return changedString;
            } else {
                if (trimToWord) {
                    BreakIterator bi = BreakIterator.getWordInstance();
                    bi.setText(changedString);
                    int biLength = bi.preceding(requiredLength - endingSuffix.length() + 1);
                    return changedString.substring(0, biLength).trim() + endingSuffix;
                } else {
                    // We know that the source string is longer that the required length, so trim it to size
                    return changedString.substring(0, requiredLength - endingSuffix.length()).trim() + endingSuffix;
                }
            }
        }
        return changedString;
    }

    /**
     * Cast a generic list to a specific class
     *
     * See: http://stackoverflow.com/questions/367626/how-do-i-fix-the-expression-of-type-list-needs-unchecked-conversion
     *
     * @param <T>
     * @param objClass
     * @param c
     * @return
     */
    public static <T> List<T> castList(Class<? extends T> objClass, Collection<?> c) {
        List<T> r = new ArrayList<T>(c.size());
        for (Object o : c) {
            r.add(objClass.cast(o));
        }
        return r;
    }

    /**
     * Split a list using a regex and return a list of trimmed strings
     *
     * @param stringToSplit
     * @param regexDelim
     * @return
     */
    public static List<String> splitList(String stringToSplit, String regexDelim) {
        List<String> finalValues = new ArrayList<String>();

        for (String output : stringToSplit.split(regexDelim)) {
            finalValues.add(output.trim());
        }

        return finalValues;
    }

    public static String[] tokenizeToArray(String sourceString, String delimiter) {
        StringTokenizer st = new StringTokenizer(sourceString, delimiter);
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        return keywords.toArray(new String[keywords.size()]);
    }

    /**
     * Get the certification from the MPAA string
     *
     * @param mpaaCertification
     * @return
     */
    public static String processMpaaCertification(String mpaaCertification) {
        return processMpaaCertification(MPPA_RATED, mpaaCertification);
    }

    /**
     * Get the certification from the MPAA rating string
     *
     * @param mpaaRated
     * @param mpaaCertification
     * @return
     */
    public static String processMpaaCertification(String mpaaRated, String mpaaCertification) {
        // Strip out the "Rated " and extra words at the end of the MPAA certification
        Pattern mpaaPattern = Pattern.compile("(?:" + (isValidString(mpaaRated) ? mpaaRated : MPPA_RATED) + "\\s)?(.*?)(?:($|\\s).*?)");
        Matcher m = mpaaPattern.matcher(mpaaCertification);
        if (m.find()) {
            return m.group(1).trim();
        } else {
            return mpaaCertification.trim();
        }
    }

    /**
     * Replace all the non-standard quote marks with a single quote
     *
     * @param original
     * @return
     */
    public static String replaceQuotes(String original) {
        return QUOTE_PATTERN.matcher(original).replaceAll(QUOTE_SINGLE);
    }

    /**
     * Parse a string value and convert it into an integer rating
     *
     * The rating should be between 0 and 10 inclusive.<br/>
     * Invalid values or values less than 0 will return -1
     *
     * @param rating the converted rating or -1 if there was an error
     * @return
     */
    public static int parseRating(String rating) {
        return parseRating(NumberUtils.toFloat(rating.replace(',', '.'), -1));
    }

    /**
     * Parse a float rating into an integer
     *
     * The rating should be between 0 and 10 inclusive.<br/>
     * Invalid values or values less than 0 will return -1
     *
     * @param rating the converted rating or -1 if there was an error
     * @return
     */
    public static int parseRating(float rating) {
        float tmp;
        if (rating < 0) {
            return -1;
        } else if (rating > 10f) {
            tmp = 10;
        } else {
            tmp = rating;
        }

        return Math.round(tmp * 10f);
    }

    /**
     * Trim a string to the 'n'th occurrence of a space.
     *
     * @param sentanceToTrim
     * @param numOfWords
     * @return
     */
    public static String getWords(final String sentanceToTrim, int numOfWords) {
        String returnSentance = sentanceToTrim;
        // count the number of spaces in the original string
        int numSpaces = StringUtils.countMatches(sentanceToTrim, " ");
        LOG.trace("Found " + numSpaces + " space(s) in '" + sentanceToTrim + "', want " + numOfWords + " word(s).");

        if (numSpaces > 0 && numSpaces >= numOfWords) {
            // ensure that the number of spaces is no larger than the count
            if (numSpaces > numOfWords) {
                LOG.trace("Number of spaces limited to " + numOfWords);
                numSpaces = numOfWords;
            }

            int pos = -1;
            for (int i = 1; i <= numSpaces; i++) {
                pos = sentanceToTrim.indexOf(' ', pos + 1);
            }

            returnSentance = sentanceToTrim.substring(0, pos);
            LOG.trace("Final: '" + returnSentance + "'");
        }

        return returnSentance;
    }
}
