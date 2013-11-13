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

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;

public class DateTimeTools {

    private static final Logger LOG = Logger.getLogger(DateTimeTools.class);
    private static final String LOG_MESSAGE = "DateTimeTools: ";
    private static final String DATE_FORMAT_STRING = PropertiesUtil.getProperty("mjb.dateFormat", "yyyy-MM-dd");
    private static final String DATE_FORMAT_LONG_STRING = DATE_FORMAT_STRING + " HH:mm:ss";
    private static final String[] FORMATS = new String[5];
    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");

    static {
        FORMATS[0] = "yyyy-MM-dd";
        FORMATS[1] = "dd-MM-yyyy";
        FORMATS[2] = "yyyy/MM/dd";
        FORMATS[3] = "dd/MM/yyyy";
        FORMATS[4] = "dd MMMM yyyy";
    }

    private DateTimeTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

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
     * Take a string runtime in various formats and try to output this in minutes
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
        } catch (NumberFormatException ignore) {
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

    public static String parseDateTo(String dateToParse, String targetFormat) {
        String parsedDateString = "";

        if (StringTools.isValidString(dateToParse) || StringTools.isValidString(targetFormat)) {
            if (dateToParse.length() <= 4) {
                LOG.trace(LOG_MESSAGE + "Adding '-01-01' to short date");
                parsedDateString = dateToParse + "-01-01";
            } else {
                // look for the date as "dd MMMM yyyy (Country)" and remove the country
                Matcher m = DATE_COUNTRY.matcher(dateToParse);
                if (m.find()) {
                    LOG.trace(LOG_MESSAGE + "Removed '" + m.group(2) + "' from date '" + dateToParse + "'");
                    parsedDateString = m.group(1);
                } else {
                    parsedDateString = dateToParse;
                }
            }

            try {
                Date parsedDate = DateUtils.parseDate(parsedDateString.trim(), FORMATS);
                parsedDateString = convertDateToString(parsedDate, targetFormat);
            } catch (ParseException ex) {
                LOG.debug(LOG_MESSAGE + "Failed to parse date '" + dateToParse + "', error: " + ex.getMessage(), ex);
                parsedDateString = "";
            }
        }

        return parsedDateString;
    }
}
