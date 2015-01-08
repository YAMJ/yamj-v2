/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pojava.datetime.DateTime;
import org.pojava.datetime.DateTimeConfig;
import org.pojava.datetime.DateTimeConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateTimeTools {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeTools.class);
    private static final String LOG_MESSAGE = "DateTimeTools: ";
    private static final String DATE_FORMAT_STRING = PropertiesUtil.getProperty("mjb.dateFormat", "yyyy-MM-dd");
    private static final String DATE_FORMAT_LONG_STRING = DATE_FORMAT_STRING + " HH:mm:ss";
    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?:.*?)(\\d{4})(?:.*?)");
    // DateTime Config
    private static final DateTimeConfigBuilder DTC_BUILDER;
    private static boolean dtcDmy;

    static {
        // Create the builder and assign a default DMY order
        DTC_BUILDER = DateTimeConfigBuilder.newInstance();
        setDmyOrder(Boolean.FALSE);
    }

    /**
     * Set the DMY parsing to DMY (true) or MDY (false)
     *
     * @param order
     */
    private static void setDmyOrder(boolean order) {
        LOG.debug("Setting date order to {}.", order ? "DMY" : "MDY");
        dtcDmy = order;
        DTC_BUILDER.setDmyOrder(dtcDmy);
        DateTimeConfig.setGlobalDefaultFromBuilder(DTC_BUILDER);
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
     * @param duration Duration in seconds
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

        LOG.trace("Formatted duration " + duration + " to " + returnString.toString());
        return returnString.toString();
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @return
     */
    public static int processRuntime(String runtime) {
        return processRuntime(runtime, -1);
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @param defaultValue
     * @return
     */
    public static int processRuntime(String runtime, int defaultValue) {
        if (StringUtils.isBlank(runtime)) {
            // No string to parse
            return defaultValue;
        }

        int returnValue;
        // See if we can convert this to a number and assume it's correct if we can
        returnValue = NumberUtils.toInt(runtime, defaultValue);

        if (returnValue < 0) {
            // This is for the format xx(hour/hr/min)yy(min), e.g. 1h30, 90mins, 1h30m
            Pattern hrmnPattern = Pattern.compile("(?i)(\\d+)(\\D*)(\\d*)(.*?)");

            Matcher matcher = hrmnPattern.matcher(runtime);
            if (matcher.find()) {
                int first = NumberUtils.toInt(matcher.group(1), -1);
                String divide = matcher.group(2);
                int second = NumberUtils.toInt(matcher.group(3), -1);

                if (first > -1 && second > -1) {
                    returnValue = (first > -1 ? first * 60 : 0) + (second > -1 ? second : 0);
                } else if (isNotValidString(divide)) {
                    // No divider value, so assume this is a straight minute value
                    returnValue = first;
                } else if (second > -1 && isValidString(divide)) {
                    // this is xx(text) so we need to work out what the (text) is
                    if (divide.toLowerCase().contains("h")) {
                        // Assume it is a form of "hours", so convert to minutes
                        returnValue = first * 60;
                    } else {
                        // Assume it's a form of "minutes"
                        returnValue = first;
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Convert a date to a specific format
     *
     * @param dateToParse
     * @param targetFormat
     * @return
     */
    public static String parseDateTo(final String dateToParse, final String targetFormat) {
        String parsedDateString = "";
        Date parsedDate;
        try {
            parsedDate = parseStringToDate(dateToParse);
        } catch (ParseException ex) {
            LOG.debug(LOG_MESSAGE + "Failed to parse date '" + dateToParse + "', error: " + ex.getMessage(), ex);
            return parsedDateString;
        }

        if (StringTools.isValidString(targetFormat)) {
            parsedDateString = convertDateToString(parsedDate, targetFormat);
        } else {
            LOG.debug(LOG_MESSAGE + "Invalid date '" + dateToParse + "' or target format '" + targetFormat + "' passed");
        }

        return parsedDateString;
    }

    /**
     * Convert a string date into a Date object.
     *
     * @param dateToParse
     * @return the date converted
     * @throws ParseException
     */
    public static Date parseStringToDate(final String dateToParse) throws IllegalArgumentException, ParseException {
        Date parsedDate = null;
        String parsedDateString;

        if (StringTools.isValidString(dateToParse)) {
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

            parsedDate = parseDateTime(dateToParse);

        } else {
            throw new IllegalArgumentException("Invalid date '" + dateToParse + "' passed");
        }

        return parsedDate;
    }

    private static Date parseDateTime(String convertDate) {
        Date parsedDate = convertStringDate(convertDate);

        if (parsedDate == null) {
            // Switch the date order and try again
            setDmyOrder(!dtcDmy);
            parsedDate = convertStringDate(convertDate);
        }

        return parsedDate;
    }

    /**
     * Convert the string date using DateTools parsing
     *
     * @param convertDate
     * @return
     */
    private static Date convertStringDate(String convertDate) {
        Date parsedDate;
        try {
            parsedDate = DateTime.parse(convertDate).toDate();
            LOG.trace("Converted date '{}' using {} order", convertDate, dtcDmy ? "DMY" : "MDY");
        } catch (IllegalArgumentException ex) {
            LOG.debug("Failed to convert date '{}' using {} order", convertDate, dtcDmy ? "DMY" : "MDY");
            parsedDate = null;
        }
        return parsedDate;
    }

    /**
     * locate a 4 digit year in a date string
     *
     * @param date
     * @return
     */
    public static int extractYear(String date) {
        int year = 0;
        Matcher m = YEAR_PATTERN.matcher(date);
        if (m.find()) {
            year = Integer.valueOf(m.group(1));
        }

        // Give up and return 0
        return year;
    }

}
