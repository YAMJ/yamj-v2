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
package com.moviejukebox.tools;

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pojava.datetime2.DateTime;

public class DateTimeTools {

    private static final String DATE_FORMAT_STRING = PropertiesUtil.getProperty("mjb.dateFormat", "yyyy-MM-dd");
    private static final String DATE_FORMAT_LONG_STRING = DATE_FORMAT_STRING + " HH:mm:ss";

    public static String getDateFormatString() {
        return DATE_FORMAT_STRING;
    }

    public static String getDateFormatLongString() {
        return DATE_FORMAT_LONG_STRING;
    }

    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_FORMAT_STRING);
    }

    public static SimpleDateFormat getDateFormatLong() {
        return new SimpleDateFormat(DATE_FORMAT_LONG_STRING);
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return converted date in the format specified in DATE_FORMAT_STRING
     */
    public static String convertDateToString(Date convertDate) {
        return convertDateToString(convertDate, getDateFormatString());
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return
     */
    public static String convertDateToString(DateTime convertDate) {
        return convertDateToString(convertDate, getDateFormatString());
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(Date convertDate, final String dateFormat) {
        return convertDateToString(new DateTime(convertDate.getTime()), dateFormat);
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(DateTime convertDate, final String dateFormat) {
        return convertDate.toString(dateFormat);
    }

    /**
     * Format the duration passed as ?h?m format
     *
     * @param duration
     * @return
     */
    public static String formatDuration(int duration) {
        StringBuilder returnString = new StringBuilder();

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
     * Take a string runtime in various formats and try to output this in
     * minutes
     *
     * @param runtime
     * @return
     */
    public static int processRuntime(String runtime) {
        int returnValue;
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

            if (isNotValidString(divide)) {
                // No divider value, so assume this is a straight minute value
                returnValue = Integer.parseInt(first);
                return returnValue;
            }

            if (isNotValidString(second) && isValidString(divide)) {
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
}
