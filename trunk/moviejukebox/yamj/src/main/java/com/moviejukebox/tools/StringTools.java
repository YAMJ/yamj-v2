/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.tools;

import java.io.File;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.model.Movie;

public class StringTools {
    private static final Pattern CLEAN_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    private static final long KB = 1024;
    private static final long MB = KB*KB;
    private static final long GB = KB*KB*KB;
    private static final DecimalFormat FILESIZE_FORMAT_0 = new DecimalFormat("0");
    private static final DecimalFormat FILESIZE_FORMAT_1 = new DecimalFormat("0.#");
    private static final DecimalFormat FILESIZE_FORMAT_2 = new DecimalFormat("0.##");
    
    /**
     * Append a string to the end of a path ensuring that there are the correct number of File.separators
     * @param basePath
     * @param additionalPath
     * @return
     */
    public static String appendToPath(String basePath, String additionalPath) {
        return (basePath.trim() + (basePath.trim().endsWith(File.separator)?"":File.separator) + additionalPath.trim());
    }

    public static String cleanString(String sourceString) {
        return CLEAN_STRING_PATTERN.matcher(sourceString).replaceAll(" ").trim();
    }

    /**
     * Convert a date to a string using the Movie dateFormat
     * @param convertDate
     * @return converted date in the format specified in Movie.dateFormatString
     */
    public static String convertDateToString(Date convertDate) {
        return convertDateToString(convertDate, Movie.dateFormat);
    }
    
    /**
     * Convert a date to a string using a Simple Date Format
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(Date convertDate, SimpleDateFormat dateFormat) {
        try {
            return dateFormat.format(convertDate);
        } catch (Exception ignore) {
            return Movie.UNKNOWN;
        }
    }

    /**
     * Convert a date to a string using a String date format
     * @param convertDate
     * @param dateFormatString
     * @return
     */
    public static String convertDateToString(Date convertDate, String dateFormatString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        return convertDateToString(convertDate, dateFormat);
    }
    
    public static String formatDuration(int duration) {
        StringBuffer returnString = new StringBuffer("");

        int nbHours = duration / 3600;
        if (nbHours != 0) {
            returnString.append(nbHours).append("h");
        }

        int nbMinutes = (duration - (nbHours * 3600)) / 60;
        if (nbMinutes != 0) {
            if (nbHours != 0) {
                returnString.append(" ");
            }
            returnString.append(nbMinutes).append("m");
        }

        return returnString.toString();
    }
    
    /**
     * Format the file size
     */
    public static String formatFileSize(long fileSize) {
        
        String returnSize = Movie.UNKNOWN;
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
            returnSize = df.format( (float) ((float) fileSize / (float) divider)) + appendText;  
        }
        
        return returnSize;
    }
    
    /**
     * Check the string passed to see if it is invalid.
     * Invalid strings are "UNKNOWN", null or blank
     * @param testString The string to test
     * @return True if the string is invalid, false otherwise
     */
    public static boolean isNotValidString(String testString) {
        return !isValidString(testString);
    }

    /**
     * Check the string passed to see if it contains a value.
     * @param testString The string to test
     * @return False if the string is empty, null or UNKNOWN, True otherwise
     */
    public static boolean isValidString(String testString) {
        // Checks if a String is whitespace, empty ("") or null.
        if (StringUtils.isBlank(testString)) {
            return false;
        }
        
        if (testString.equalsIgnoreCase(Movie.UNKNOWN)) {
            return false;
        }
        
        return true;
    }
 
    /**
     * Take a string runtime in various formats and try to output this in minutes
     * @param runtime
     * @return
     */
    public static int processRuntime(String runtime) {
        int returnValue = -1;
        // See if we can convert this to a number and assume it's correct if we can
        try {
            returnValue = Integer.parseInt(runtime);
            return returnValue;
        } catch (Exception ignore) {
            returnValue = -1;
        }

        // This is for the format xx(hour/hr/min)yy(min), e.g. 1h30, 90mins, 1h30m
        Pattern hrmnPattern = Pattern.compile("(?i)(\\d+)(\\D*)(\\d*)(.*?)");
        
        Matcher matcher = hrmnPattern.matcher(runtime);
        if (matcher.find()) {
            String first = matcher.group(1);
            String divide = matcher.group(2);
            String second = matcher.group(3);
            
            if (isValidString(second)) {
                // Assume that this is HH(text)MM
                returnValue = (Integer.parseInt(first) * 60) + Integer.parseInt(second);
                return returnValue;
            }
            
            if (!isValidString(divide)) {
                // No divider value, so assume this is a straight minute value
                returnValue = Integer.parseInt(first);
                return returnValue;
            }
            
            if (!isValidString(second) && isValidString(divide)) {
                // this is xx(text) so we need to work out what the (text) is
                if (divide.toLowerCase().contains("h")) {
                    // Assume it is a form of "hours", so convert to minutes
                    returnValue = Integer.parseInt(first) * 60;
                } else {
                    // Assume it's a form of "minutes"
                    returnValue = Integer.parseInt(first);
                }
                return returnValue;
            }
        }
        
        return returnValue;
    }

    public static String trimToLength(String sourceString, int requiredLength) {
        return trimToLength(sourceString, requiredLength, true, "...");
    }

    /**
     * Check that the passed string is no longer than the required length and trim it if necessary
     * @param sourceString      The string to check
     * @param requiredLength    The required length (Maximum)
     * @param trimToWord        Trim the source string to the last space to avoid partial words
     * @param endingSuffix      The ending to append if the string is longer than the required length
     * @return
     */
    public static String trimToLength(String sourceString, int requiredLength, boolean trimToWord, String endingSuffix) {
        if (isValidString(sourceString)) {
            if (sourceString.length() <= requiredLength) {
                // No need to do anything
                return sourceString;
            } else {
                if (trimToWord) {
                    BreakIterator bi = BreakIterator.getWordInstance();
                    bi.setText(sourceString);
                    int biLength = bi.preceding(requiredLength - endingSuffix.length());
                    return new String(sourceString.substring(0, biLength)).trim() + endingSuffix;
                } else {
                    // We know that the source string is longer that the required length, so trim it to size
                    return new String(sourceString.substring(0, requiredLength - endingSuffix.length())).trim() + endingSuffix;
                }
            }
        }
        return sourceString;
    }
}