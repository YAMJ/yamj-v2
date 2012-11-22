package org.pojava.datetime;

/*
 Copyright 2010 John Pile

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * <p>
 * DateTime provides an immutable representation of Date and Time to the nearest nanosecond. You can access DateTime properties
 * either in milliseconds or in seconds and nanoseconds. Both the seconds and milliseconds values can be understood as being
 * truncated to their respective precisions. Nanos holds the fractional portion of a second in the range 0-999999999. Note that
 * whether seconds is positive or negative, the internal values will be adjusted if necessary to support a positive value for
 * nanos.
 * </p>
 * <p>
 * You may think of a DateTime object as a fixed offset of System time measured from the Unix epoch in non-leap milliseconds or
 * non-leap seconds and nanoseconds. Leap years are calculated according to the Gregorian Calendar, matching the same
 * interpretation as the java.util.Date object (every 4th year is a leap year, except for years divisible by 100 but not divisible
 * by 400). The times are stored according to the UTC (aka GMT) time zone, and a TimeZone object is referenced to translate to a
 * local time zone.
 * </p>
 * <p>
 * DateTime includes a robust parser for interpreting date and time from a String. It parses a date and time using heuristics
 * rather than comparing against preset formats, so it is point-and-shoot simple. The following, for example, are interpreted the
 * same:
 * <ul>
 * <li>3:21pm on January 26, 1969</li>
 * <li>26-Jan-1969 03:21 PM</li>
 * <li>1/26/69 15:21</li>
 * <li>1969.01.26 15.21</li>
 * <li>el 26 de enero de 1969 15.21</li>
 * </ul>
 * <p>
 * Some notes on the date interpretations:
 * </p>
 * 
 * <p>
 * All dates are interpreted in your local time zone, unless a time zone is specified in the String. Time zones are configurable
 * in the DateTimeConfig object, so you can determine for your own application whether CST, for example, would adjust to Central
 * Standard Time or Chinese Standard Time.
 * </p>
 * 
 * <p>
 * A two-digit year will assume up to 80 years in the past and 20 years in the future. It is prudent in many cases to follow this
 * with a check based on whether you know the date to represent a past or future date. If you know you parsed a birthday, you can
 * compare with today's date and subtract 100 yrs if needed (references to birthdays 20 years in the future are rare). Similarly,
 * if you're dealing with an annuity date, you can add 100 years if the parsed date occurred in the past.
 * </p>
 * 
 * <p>
 * If you're parsing European dates expecting DD/MM/YYYY instead of MM/DD/YYYY, then you can alter the global DateTimeConfig
 * setting by first calling, " <code>DateTimeConfig.globalEuropeanDateFormat();</code>".
 * </p>
 * 
 * @author John Pile
 * 
 */
public class DateTime implements Serializable, Comparable<DateTime> {

    private static final long serialVersionUID = 201L;

    /**
     * These months have less than 31 days
     */
    private static final int FEB = 1;
    private static final int APR = 3;
    private static final int JUN = 5;
    private static final int SEP = 8;
    private static final int NOV = 10;

    /**
     * Config contains info specific to zoning and formatting.
     */
    protected IDateTimeConfig config;

    /**
     * System time is a lazy calculation of milliseconds from Unix epoch 1970-01-01 00:00:00, assuming no leap seconds and a leap
     * year every year evenly divisible by 4, except for years divisible by 100 but not divisible by 400.
     */
    protected Duration systemDur = null;

    private static final Pattern partsPattern = Pattern.compile("[^A-Z0-9_]+");

    /**
     * Default constructor gives current time to millisecond.
     */
    public DateTime() {
        this.systemDur = new Duration(System.currentTimeMillis());
        config();
    }

    /**
     * DateTime constructed from time in milliseconds since epoch.
     * 
     * @param millis
     *            time
     */
    public DateTime(long millis) {
        config();
        this.systemDur = new Duration(millis);
    }

    /**
     * DateTime constructed from time in milliseconds since epoch.
     * 
     * @param millis
     * @param config
     *            time
     */
    public DateTime(long millis, IDateTimeConfig config) {
        this.config = config;
        this.systemDur = new Duration(millis);
    }

    /**
     * DateTime constructed from time in milliseconds since epoch.
     * 
     * @param millis
     *            Number of milliseconds since epoch
     * @param tz
     *            Override the output Time Zone
     */
    public DateTime(long millis, TimeZone tz) {
        DateTimeConfig newConfig = DateTimeConfig.getGlobalDefault().clone();
        newConfig.setOutputTimeZone(tz);
        this.config = newConfig;
        this.systemDur = new Duration(millis);
    }

    /**
     * DateTime constructed from time in milliseconds since epoch.
     * 
     * @param millis
     *            Number of milliseconds since epoch
     * @param tzId
     *            Override the output time zone
     */
    public DateTime(long millis, String tzId) {
        DateTimeConfig newConfig = DateTimeConfig.getGlobalDefault().clone();
        newConfig.setOutputTimeZone(newConfig.lookupTimeZone(tzId));
        this.config = newConfig;
        this.systemDur = new Duration(millis);
    }

    /**
     * Construct a DateTime from seconds and fractional seconds.
     * 
     * @param seconds
     *            Number of seconds since epoch (typically 1970-01-01)
     * @param nanos
     *            Nanosecond offset in range +/- 999999999
     */
    public DateTime(long seconds, int nanos) {
        config();
        this.systemDur = new Duration(seconds, nanos);
    }

    /**
     * Construct a DateTime from seconds and fractional seconds.
     * 
     * @param seconds
     *            Number of seconds since epoch (typically 1970-01-01)
     * @param nanos
     *            Nanosecond offset in range +/- 999999999
     * @param tz
     *            Override the output time zone
     */
    public DateTime(long seconds, int nanos, TimeZone tz) {
        DateTimeConfig newConfig = DateTimeConfig.getGlobalDefault().clone();
        newConfig.setOutputTimeZone(tz);
        this.config = newConfig;
        this.systemDur = new Duration(seconds, nanos);
    }

    /**
     * Construct a DateTime from seconds and fractional seconds.
     * 
     * @param seconds
     *            Number of seconds since epoch (typically 1970-01-01)
     * @param nanos
     *            Nanosecond offset in range +/- 999999999
     * @param tzId
     *            Override the output time zone
     */
    public DateTime(long seconds, int nanos, String tzId) {
        DateTimeConfig newConfig = DateTimeConfig.getGlobalDefault().clone();
        newConfig.setOutputTimeZone(newConfig.lookupTimeZone(tzId));
        this.config = newConfig;
        this.systemDur = new Duration(seconds, nanos);
    }

    /**
     * Construct a DateTime from seconds and fractional seconds.
     * 
     * @param seconds
     *            Number of seconds since epoch (typically 1970-01-01)
     * @param nanos
     *            Nanosecond offset in range +/- 999999999
     * @param config
     *            Provide custom configuration options
     */
    public DateTime(long seconds, int nanos, IDateTimeConfig config) {
        this.config = config;
        this.systemDur = new Duration(seconds, nanos);
    }

    /**
     * DateTime constructed from a string using global defaults.
     * 
     * @param str
     */
    public DateTime(String str) {
        this.config = DateTimeConfig.getGlobalDefault();
        DateTime dt = parse(str, config);
        this.systemDur = dt.systemDur;
    }

    /**
     * DateTime constructed from a string using global defaults.
     * 
     * @param str
     *            String to parse
     * @param config
     *            Custom configuration options
     */
    public DateTime(String str, IDateTimeConfig config) {
        this.config = config;
        DateTime dt = parse(str, config);
        this.systemDur = dt.systemDur;
    }

    /**
     * DateTime constructed from a Timestamp includes nanos.
     * 
     * @param ts
     *            Timestamp
     */
    public DateTime(Timestamp ts) {
        config();
        this.systemDur = new Duration(ts.getTime() / 1000, ts.getNanos());
    }

    /**
     * Derive a time zone descriptor from the right side of the date/time string.
     * 
     * @param str
     *            String to parse date/time
     * @return
     */
    private static final String tzParse(String str) {
        char[] chars = str.toCharArray();
        int min = 7;
        int max = str.length() - 1;
        int idx = max;
        char c = '\0';
        while (idx > min) {
            c = chars[idx];
            if (c >= '0' && c <= '9' || c == ':') {
                idx--;
            } else {
                break;
            }
        }
        if (idx >= min && (c == '+' || c == '-')) {
            return str.substring(idx);
        }
        while (idx >= min) {
            c = chars[idx];
            if (c >= 'A' && c <= 'Z' || c == '/' || c >= '0' && c <= '9') {
                idx--;
            } else {
                ++idx;
                while (idx < max && chars[idx] >= '0' && chars[idx] <= '9') {
                    if (++idx == max) {
                        break;
                    }
                }
                break;
            }
        }
        if (idx < min || idx > max) {
            return null;
        }
        c = chars[idx];
        if (c >= 'A' && c <= 'Z') {
            return str.substring(idx);
        }
        return null;
    }

    /**
     * Compare two DateTime objects to determine ordering.
     * 
     * @param other
     *            DateTime to compare to this
     * @return -1, 0, or 1 based on comparison to another DateTime.
     */
    public int compareTo(DateTime other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare DateTime to null.");
        }
        return this.systemDur.compareTo(other.systemDur);
    }

    /**
     * Get a timestamp useful for JDBC
     * 
     * @return This DateTime as a Timestamp object.
     */
    public Timestamp toTimestamp() {
        Timestamp ts = new Timestamp(this.systemDur.toMillis());
        if (this.systemDur.getNanos() > 0) {
            ts.setNanos(this.systemDur.getNanos());
        }
        return ts;
    }

    /**
     * Get Date/Time as a Java Date object.
     * 
     * @return this DateTime truncated and converted to a java.util.Date object.
     */
    public Date toDate() {
        return new Date(this.systemDur.toMillis());
    }

    /**
     * Get the TimeZone
     * 
     * @return this TimeZone.
     */
    public TimeZone timeZone() {
        return config().getOutputTimeZone();
    }

    /**
     * By default, the toString method gives a sortable ISO 8601 date and time to nearest second in the same time zone as the
     * system. The default format can be redefined in DateTimeConfig.
     * 
     * @return DateTime using the default config options
     */
    @Override
    public String toString() {
        String formatStr = config().getFormat();
        String str = DateTimeFormat.format(formatStr, this, config.getOutputTimeZone(), config.getLocale());
        return str;
    }

    /**
     * Return a String according to the provided format.
     * 
     * @param format
     * @return A formatted string version of the current DateTime.
     */
    public String toString(String format) {
        return DateTimeFormat.format(format, this, config().getOutputTimeZone(), config().getLocale());
    }

    /**
     * Return a String according to the provided format.
     * 
     * @param format
     * @param tz
     *            Show formatted date & time at the given TimeZone
     * @return A formatted string version of the current DateTime.
     */
    public String toString(String format, TimeZone tz) {
        return DateTimeFormat.format(format, this, tz, config().getLocale());
    }

    /**
     * Return a String according to the provided format.
     * 
     * @param format
     * @param locale
     *            Show formatted date & time at the given TimeZone
     * @return A formatted string version of the current DateTime.
     */
    public String toString(String format, Locale locale) {
        return DateTimeFormat.format(format, this, this.timeZone(), locale);
    }

    /**
     * Return a String according to the provided format.
     * 
     * @param tz
     *            Show formatted date & time at the given TimeZone
     * @return A formatted string version of the current DateTime.
     */
    public String toString(TimeZone tz) {
        return DateTimeFormat.format(config().getFormat(), this, tz, config().getLocale());
    }

    /**
     * Return a String according to the provided format.
     * 
     * @param format
     * @param tz
     *            Show formatted date & time at the given TimeZone
     * @param locale
     *            Display date words like month or day of week in a given language.
     * @return A formatted string version of the current DateTime.
     */
    public String toString(String format, TimeZone tz, Locale locale) {
        return DateTimeFormat.format(format, this, tz, locale);
    }

    /**
     * Add a fixed duration of time
     * 
     * @param dur
     * @return Newly calculated DateTime object.
     */
    public DateTime add(Duration dur) {
        Duration calcDur = dur.add(this.getSeconds(), this.getNanos());
        return new DateTime(calcDur.getSeconds(), calcDur.getNanos(), config());
    }

    /**
     * Add a fixed duration in milliseconds. The Duration object provides fixed multipliers such as SECOND or HOUR.
     * 
     * @param milliseconds
     * @return Newly calculated DateTime object.
     */
    public DateTime add(long milliseconds) {
        Duration dur = this.systemDur.add(milliseconds);
        return new DateTime(dur.getSeconds(), dur.getNanos(), config());
    }

    /**
     * Add +/- a block of time to a date in it's OutputTimeZone.
     * 
     * @param calUnit
     * @param qty
     * @return recalculated DateTime
     */
    public DateTime add(CalendarUnit calUnit, int qty) {
        return shift(calUnit, qty);
    }

    /**
     * Add increments of any calendar time unit from a nanosecond to a century. This is different from a Duration in that it will
     * make adjustments to preserve non-linear values such as daylight saving or day-of-month offsets.
     * 
     * @param calUnit
     * @param qty
     *            May be positive or negative.
     * @return Newly calculated DateTime object.
     */
    public DateTime shift(CalendarUnit calUnit, int qty) {
        /* Fixed durations */
        if (calUnit.compareTo(CalendarUnit.DAY) < 0) {
            if (calUnit == CalendarUnit.HOUR) {
                return this.add(qty * 3600000L);
            }
            if (calUnit == CalendarUnit.MINUTE) {
                return this.add(qty * 60000L);
            }
            if (calUnit == CalendarUnit.SECOND) {
                return this.add(qty * 1000L);
            }
            if (calUnit == CalendarUnit.MILLISECOND) {
                return this.add(qty);
            }
            if (calUnit == CalendarUnit.MICROSECOND) {
                return this.add(new Duration(0, qty * 1000));
            }
            if (calUnit == CalendarUnit.NANOSECOND) {
                return this.add(new Duration(0, qty));
            }
        }
        /* Calendar periods (same time, different day) */
        Calendar cal = Calendar.getInstance(config().getOutputTimeZone(), config().getLocale());
        cal.setTimeInMillis(this.systemDur.millis);
        if (calUnit == CalendarUnit.DAY) {
            cal.add(Calendar.DATE, qty);
        } else if (calUnit == CalendarUnit.WEEK) {
            cal.add(Calendar.DATE, qty * 7);
        } else if (calUnit == CalendarUnit.MONTH) {
            cal.add(Calendar.MONTH, qty);
        } else if (calUnit == CalendarUnit.QUARTER) {
            cal.add(Calendar.MONTH, qty * 3);
        } else if (calUnit == CalendarUnit.YEAR) {
            cal.add(Calendar.YEAR, qty);
        } else if (calUnit == CalendarUnit.CENTURY) {
            cal.add(Calendar.YEAR, 100 * qty);
        }
        return new DateTime(cal.getTimeInMillis() / 1000, systemDur.getNanos(), config);
    }

    /**
     * Shift this DateTime +/- a Shift offset.
     * 
     * @param shift
     *            a pre-defined shift of various calendar time increments.
     * @return a new DateTime offset by the values specified.
     */
    public DateTime shift(Shift shift) {
        if (shift == null) {
            return this;
        }
        Calendar cal = Calendar.getInstance(config().getOutputTimeZone(), config().getLocale());
        cal.setTimeInMillis(this.systemDur.millis);
        if (shift.getYear() != 0) {
            cal.add(Calendar.YEAR, shift.getYear());
        }
        if (shift.getMonth() != 0) {
            cal.add(Calendar.MONTH, shift.getMonth());
        }
        if (shift.getWeek() != 0) {
            cal.add(Calendar.DATE, shift.getWeek() * 7);
        }
        if (shift.getDay() != 0) {
            cal.add(Calendar.DATE, shift.getDay());
        }
        if (shift.getHour() != 0) {
            cal.add(Calendar.HOUR, shift.getHour());
        }
        if (shift.getMinute() != 0) {
            cal.add(Calendar.MINUTE, shift.getMinute());
        }
        if (shift.getSecond() != 0) {
            cal.add(Calendar.SECOND, shift.getSecond());
        }
        return new DateTime(cal.getTimeInMillis() / 1000, systemDur.getNanos() + shift.getNanosec(), config);
    }

    /**
     * Shift this DateTime +/- a Shift offset specified as an ISO 8601 string.
     * 
     * @param iso8601
     *            A string of format "P[#Y][#M][#D][T[#H][#M][#S[.#]]" holding a list of offsets.
     * @return a new DateTime shifted by the specified amounts.
     */
    public DateTime shift(String iso8601) {
        return this.shift(new Shift(iso8601));
    }

    /**
     * Return numeric day of week, usually Sun=1, Mon=2, ... , Sat=7;
     * 
     * @return Numeric day of week, usually Sun=1, Mon=2, ... , Sat=7. See DateTimeConfig.
     */
    public int weekday() {
        long leftover = 0;
        // Adding 2000 years in weeks makes all calculations positive.
        // Adding epoch DOW shifts us into phase with start of week.
        long offset = config().getEpochDOW() * Duration.DAY + 52 * Duration.WEEK * 2000;
        leftover = offset + this.toMillis() + config().getOutputTimeZone().getOffset(this.toMillis());
        leftover %= Duration.WEEK;
        leftover /= Duration.DAY;
        // Convert from zero to one based
        leftover++;
        return (int) leftover;
    }

    /**
     * Parse a time reference that fits in a single word. Supports: YYYYMMDD, [+-]D, [0-9]+Y
     * 
     * @param str
     *            Date/Time string to be parsed.
     * @param config
     *            Configuration parameters governing parsing and presentation.
     * @return New DateTime interpreted from string.
     */
    private static DateTime parseRelativeDate(String str, IDateTimeConfig config) {
        char firstChar = str.charAt(0);
        char lastChar = str.charAt(str.length() - 1);
        DateTime dt = new DateTime();
        if ((firstChar == '+' || firstChar == '-') && lastChar >= '0' && lastChar <= '9') {
            if (ParseTool.onlyDigits(str.substring(1))) {
                int offset = (new Integer((firstChar == '+') ? str.substring(1) : str)).intValue();
                return dt.add(CalendarUnit.DAY, offset);
            }
        }
        if ((lastChar == 'D' || lastChar == 'Y' || lastChar == 'M')) {
            CalendarUnit unit = null;
            if (lastChar == 'D') {
                unit = CalendarUnit.DAY;
            } else if (lastChar == 'Y') {
                unit = CalendarUnit.YEAR;
            } else if (lastChar == 'M') {
                unit = CalendarUnit.MONTH;
            }
            String inner = str.substring((firstChar >= '0' && firstChar <= '9') ? 0 : 1, str.length() - 1);
            // ^[+-][0-9]+$
            if (firstChar == '+') {
                if (ParseTool.onlyDigits(inner)) {
                    int offset = (new Integer(inner)).intValue();
                    return dt.add(unit, offset);
                }
            }
            if (firstChar == '-' || firstChar >= '0' && firstChar <= '9') {
                if (ParseTool.isInteger(inner)) {
                    String offset = firstChar == '-' ? "-" + inner : inner;
                    int innerVal = (new Integer(offset)).intValue();
                    return dt.add(unit, innerVal);
                }
            }
        }
        throw new IllegalArgumentException("Could not parse date from '" + str + "'");
    }

    /**
     * Interpret a DateTime from a String using global defaults.
     * 
     * @param str
     *            Date/Time string to be parsed.
     * 
     * @return New DateTime interpreted from string.
     */
    public static DateTime parse(String str) {
        DateTimeConfig config = DateTimeConfig.getGlobalDefault();
        return parse(str, config);
    }

    /**
     * Interpret a DateTime from a String.
     * 
     * @param str
     *            Date/Time string to be parsed.
     * @param config
     *            Configuration parameters governing parsing and presentation.
     * @return New DateTime interpreted from string according to alternate rules.
     */
    public static DateTime parse(String str, IDateTimeConfig config) {
        boolean isYearFirst = false;

        // boolean guessedYear = false;
        boolean isTwoDigitYear = false;

        boolean hasYear = false, hasMonth = false, hasDay = false;
        boolean hasHour = false, hasMinute = false, hasSecond = false;
        boolean hasNanosecond = false;
        boolean isBC = false;

        if (config == null) {
            config = DateTimeConfig.getGlobalDefault().clone();
        }
        if (str == null) {
            return new DateTime(System.currentTimeMillis(), config);
        }
        // Normalize the string a bit
        str = str.trim().toUpperCase(config.getLocale()).replace('\u00c9', 'E');
        if (str.length() == 0) {
            throw new IllegalArgumentException("Cannot parse DateTime from empty string.");
        }
        if (str.indexOf('T') > 0) {
            // Replace a T separator with a space separator.
            str = str.replaceFirst("([0-9])T([0-9])", "$1 $2");
        }
        if (str.charAt(0) == '+' || str.charAt(0) == '-') {
            return parseRelativeDate(str, config);
        }
        if (str.matches(".*([0-9][A-Z]|[A-Z][0-9]).*")) {
            // Expand dates that use number-to-alpha as implied separator
            // Chars DMY are reserved for day, month, year relative dates
            str = str.replaceAll("([0-9])(Z|[A-Y]{2})", "$1 $2");
            str = str.replaceAll("([A-Z]{3})([0-9])", "$1 $2");
        }
        String tzString = tzParse(str);
        if ("AM".equals(tzString) || "PM".equals(tzString)) {
            tzString = null;
        }
        if ("BC".equals(tzString) || "BCE".equals(tzString)) {
            tzString = null;
            isBC = true;
        }
        if (tzString != null && tzString.endsWith("0")) {
            if (tzString.matches("[+-][0-9]{2}:[0-9]{2}")) {
                tzString = "GMT" + tzString;
                str = str.substring(0, str.length() - 6);
            } else if (tzString.matches("[+-][0-9]{4}")) {
                tzString = "GMT" + tzString.substring(0, 3) + ":" + tzString.substring(3);
                str = str.substring(0, str.length() - 5);
            }
        }
        TimeZone tz = config.lookupTimeZone(tzString);
        Tm tm = new Tm(System.currentTimeMillis(), tz);
        String[] parts = partsPattern.split(str);
        int year = 0, month = 0, day = 1;
        int hour = 0, minute = 0, second = 0, nanosecond = 0;
        int thisYear = tm.getYear();
        int centuryTurn = thisYear - (thisYear % 100);
        // Build a table describing which fields are integers.
        boolean[] integers = new boolean[parts.length];
        boolean[] usedint = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (ParseTool.startsWithDigit(parts[i])) {
                integers[i] = true;
            }
        }
        // First, scan for text month
        for (int i = 0; i < parts.length; i++) {
            if (!integers[i] && parts[i].length() > 2) {
                Object[] langs = config.getSupportedLanguages();
                for (Object lang2 : langs) {
                    String[] mos = config.getMonthArray((String) lang2);
                    int mo = ParseTool.indexedStartMatch(mos, parts[i]);
                    if (mo >= 0) {
                        month = mo;
                        hasMonth = true;
                        break;
                    }
                }
            }
            if (hasMonth) {
                break;
            }
        }
        // Next scan for 4-digit year or an 8 digit YYYYMMDD
        for (int i = 0; i < parts.length; i++) {
            if (integers[i] && !usedint[i]) {
                if (!hasYear && (parts[i].length() == 4 || parts[i].length() == 5)) {
                    char c = parts[i].charAt(parts[i].length() - 1);
                    if (c >= '0' && c <= '9') {
                        year = ParseTool.parseIntFragment(parts[i]);
                        hasYear = true;
                        usedint[i] = true;
                        isYearFirst = (i == 0);
                        // If integer is to the immediate left of year, use now.
                        if (config.isDmyOrder()) {
                            if (!hasMonth && i > 0 && integers[i - 1] && !usedint[i - 1]) {
                                month = ParseTool.parseIntFragment(parts[i - 1]);
                                month--;
                                hasMonth = true;
                                usedint[i - 1] = true;
                            }
                        } else {
                            if (!hasDay && i > 0 && integers[i - 1] && !usedint[i - 1]) {
                                day = ParseTool.parseIntFragment(parts[i - 1]);
                                hasDay = true;
                                usedint[i - 1] = true;
                            }
                        }
                        break;
                    }
                }
                if (!hasYear && !hasMonth && !hasDay && parts[i].length() == 8) {
                    year = Integer.parseInt(parts[i].substring(0, 4));
                    month = Integer.parseInt(parts[i].substring(4, 6));
                    month--;
                    day = Integer.parseInt(parts[i].substring(6, 8));
                    hasYear = true;
                    hasMonth = true;
                    hasDay = true;
                    usedint[i] = true;
                }
            }
        }
        if (hasYear && year == 0) {
            throw new IllegalArgumentException("Invalid zero year parsed.");
        }
        // One more scan for Date.toString() style
        if (!hasYear && str.endsWith("T " + parts[parts.length - 1])) {
            year = Integer.parseInt(parts[parts.length - 1]);
            hasYear = true;
            usedint[usedint.length - 1] = true;
        }
        if (!hasYear) {
            /* Remove time and alpha */
            String masked = str.replaceAll("[0-9]+:[0-9:]+|[a-zA-Z]+", "");
            if (masked.matches("^\\s*[0-9]+\\s*$")) {
                /* No year given. We'll use this year and test for Dec/Jan at end. */
                year = thisYear;
                hasYear = true;
                // guessedYear=true;
            }
        }
        // Assign integers to remaining slots in order
        for (int i = 0; i < parts.length; i++) {
            if (integers[i] && !usedint[i]) {
                int part = ParseTool.parseIntFragment(parts[i]);
                if (!hasDay && part < 32 && config.isDmyOrder()) {
                    /*
                     * If one sets the isDmyOrder to true in DateTimeConfig, then this will properly interpret DD before MM in
                     * DD-MM-yyyy dates. If the first number is a year, then an ISO 8601 date is assumed, in which MM comes before
                     * DD.
                     */
                    if (!isYearFirst) {
                        day = part;
                        hasDay = true;
                        usedint[i] = true;
                        continue;
                    }
                }
                if (!hasMonth) {
                    if (part < 1 || part > 12) {
                        throw new IllegalArgumentException("Invalid month parsed from [" + part + "].");
                    }
                    month = part - 1;
                    hasMonth = true;
                    usedint[i] = true;
                    continue;
                }
                if (!hasDay) {
                    if (part < 1 || part > 31) {
                        throw new IllegalArgumentException("Invalid day parsed from [" + part + "].");
                    }
                    day = part;
                    hasDay = true;
                    usedint[i] = true;
                    continue;
                }
                if (!hasYear && part < 1000) {
                    if (part > 99) {
                        year = 1900 + part;
                    } else {
                        isTwoDigitYear = true;
                        if (centuryTurn + part - thisYear > 20) {
                            year = centuryTurn + part - 100;
                        } else {
                            year = centuryTurn + part;
                        }
                    }
                    hasYear = true;
                    usedint[i] = true;
                    continue;
                }
                if (!hasDay || !hasYear) {
                    throw new IllegalArgumentException("Unable to determine valid placement for parsed value [" + part + "].");
                }
                if (!hasHour) {
                    if (part >= 24) {
                        throw new IllegalArgumentException("Invalid hour parsed from [" + part + "].");
                    }
                    hour = part;
                    hasHour = true;
                    usedint[i] = true;
                    continue;
                }
                if (!hasMinute) {
                    if (part >= 60) {
                        throw new IllegalArgumentException("Invalid minute parsed from [" + part + "].");
                    }
                    minute = part;
                    hasMinute = true;
                    usedint[i] = true;
                    continue;
                }
                if (!hasSecond) {
                    if (part < 60 || part == 60 && minute == 59 && hour == 23 && day >= 30 && (month == 11 || month == 5)) {
                        second = part;
                        hasSecond = true;
                        usedint[i] = true;
                        continue;
                    } else {
                        throw new IllegalArgumentException("Invalid second parsed from [" + part + "].");
                    }
                }
                if (!hasNanosecond) {
                    if (part >= 1000000000) {
                        throw new IllegalArgumentException("Invalid nanosecond parsed from [" + part + "].");
                    }
                    nanosecond = Integer.parseInt((parts[i].split("[^0-9]+")[0] + "00000000").substring(0, 9));
                    hasNanosecond = true;
                    usedint[i] = true;
                    continue;
                }
            }
        }
        /**
         * Adjust 12AM and 1-11PM.
         */
        for (String part : parts) {
            if (part.endsWith("M")) {
                if (part.endsWith("PM") && hour > 0 && hour < 12) {
                    hour += 12;
                } else if (part.endsWith("AM") && hour == 12) {
                    hour = 0;
                }
            }
        }

        /**
         * Validate
         */
        if (!hasYear || !hasMonth) {
            throw new IllegalArgumentException("Could not determine Year, Month, and Day from '" + str + "'");
        }
        if (month == FEB) {
            if (day > 28 + (year % 4 == 0 ? 1 : 0)) {
                throw new IllegalArgumentException("February " + day + " does not exist in " + year);
            }
        } else if (month == SEP || month == APR || month == JUN || month == NOV) {
            if (day > 30) {
                throw new IllegalArgumentException("30 days hath Sep, Apr, Jun, and Nov... not " + day);
            }
        } else {
            if (day > 31) {
                throw new IllegalArgumentException("No month has " + day + " days in it.");
            }
        }
        if (isBC && year >= 0) {
            year = -year + 1;
        }
        DateTime returnDt = new DateTime(Tm.calcTime(year, 1 + month, day, hour, minute, second, nanosecond / 1000000, tz),
                config);
        if (isTwoDigitYear && config.isUnspecifiedCenturyAlwaysInPast()) {
            if (returnDt.getSeconds() * 1000 > System.currentTimeMillis()) {
                returnDt = returnDt.shift(CalendarUnit.CENTURY, -1);
            }
        }

        returnDt.systemDur.nanos = nanosecond;
        return returnDt;
    }

    /**
     * Truncate DateTime down to its nearest time unit as a time. CalendarUnit.(WEEK|DAY|HOUR|MINUTE|SECOND|MILLISECOND)
     * 
     * @param unit
     *            Unit of time to which new DateTime will be truncated.
     * @return A newly calculated DateTime.
     */
    public DateTime truncate(CalendarUnit unit) {
        long trim = 0;
        if (unit.compareTo(CalendarUnit.HOUR) < 0) {
            if (unit == CalendarUnit.MINUTE) {
                trim = this.systemDur.millis % Duration.MINUTE;
                if (trim < 0) {
                    trim += Duration.MINUTE;
                }
                return new DateTime(this.systemDur.millis - trim, config());
            }
            if (unit == CalendarUnit.SECOND) {
                trim = this.systemDur.millis % Duration.SECOND;
                if (trim < 0) {
                    trim += Duration.SECOND;
                }
                return new DateTime(this.systemDur.millis - trim, config.getOutputTimeZone());
            }
            if (unit == CalendarUnit.MILLISECOND) {
                return new DateTime(this.systemDur.millis, config.getOutputTimeZone());
            }
            if (unit == CalendarUnit.MICROSECOND) {
                int nanotrim = this.systemDur.nanos % 1000000;
                if (nanotrim < 0) {
                    nanotrim += 1000000;
                }
                return new DateTime(this.getSeconds(), this.systemDur.nanos - nanotrim, config);
            }
            return new DateTime(this.systemDur.millis, config);
        }
        // Shift to same time of day at Rose line
        long calcTime = this.systemDur.millis + config().getOutputTimeZone().getOffset(this.systemDur.millis);
        // Truncate and shift back to local time
        if (unit == CalendarUnit.HOUR) {
            trim = calcTime % Duration.HOUR;
            if (trim < 0) {
                trim += Duration.HOUR;
            }
            calcTime -= trim;
            calcTime -= config().getOutputTimeZone().getOffset(calcTime);
            return new DateTime(calcTime, config());
        }
        if (unit == CalendarUnit.DAY) {
            trim = calcTime % Duration.DAY;
            if (trim < 0) {
                trim += Duration.DAY;
            }
            calcTime -= trim;
            calcTime -= config().getOutputTimeZone().getOffset(calcTime);
            return new DateTime(calcTime, config());
        }
        if (unit == CalendarUnit.WEEK) {
            long dow = ((calcTime / Duration.DAY) + config().getEpochDOW()) % 7;
            calcTime -= (calcTime % Duration.DAY + Duration.DAY * dow);
            calcTime -= config().getOutputTimeZone().getOffset(calcTime);
            return new DateTime(calcTime, config());
        }
        Tm tm = new Tm(this.systemDur.millis, config().getOutputTimeZone());
        if (unit == CalendarUnit.MONTH) {
            return new DateTime(Tm.calcTime(tm.getYear(), tm.getMonth(), 1, 0, 0, 0, 0, config.getOutputTimeZone()), config);
        }
        if (unit == CalendarUnit.QUARTER) {
            int monthOffset = (tm.getMonth() - 1) % 3;
            return new DateTime(
                    Tm.calcTime(tm.getYear(), tm.getMonth() - monthOffset, 1, 0, 0, 0, 0, config.getOutputTimeZone()), config);
        }
        if (unit == CalendarUnit.YEAR) {
            return new DateTime(Tm.calcTime(tm.getYear(), 1, 1, 0, 0, 0, 0, config.getOutputTimeZone()), config);
        }
        if (unit == CalendarUnit.CENTURY) {
            return new DateTime(Tm.calcTime(tm.getYear() - tm.getYear() % 100, 1, 1, 0, 0, 0, 0, config.getOutputTimeZone()),
                    config);
        }
        throw new IllegalArgumentException("That precision is still unsupported.  Sorry, my bad.");
    }

    /**
     * Whole seconds offset from epoch.
     * 
     * @return Whole seconds offset from epoch (1970-01-01 00:00:00).
     */
    public long getSeconds() {
        return systemDur.getSeconds();
    }

    /**
     * Whole milliseconds offset from epoch.
     * 
     * @return Milliseconds offset from epoch (1970-01-01 00:00:00).
     */
    public long toMillis() {
        return systemDur.toMillis();
    }

    /**
     * Positive nanosecond offset from Seconds.
     * 
     * @return Fractional second in nanoseconds for the given time.
     */
    public int getNanos() {
        int nanos = systemDur.getNanos();
        return nanos >= 0 ? nanos : 1000000000 + nanos;
    }

    /**
     * This compares a DateTime with another DateTime.
     * 
     * @param dateTime
     *            DateTime to which this DateTime will be compared.
     * @return True if DateTime values represent the same point in time.
     */
    @Override
    public boolean equals(Object dateTime) {
        if (dateTime == null) {
            return false;
        }
        if (dateTime.getClass() == this.getClass()) {
            DateTime dt = (DateTime) dateTime;
            return systemDur.toMillis() == dt.toMillis() && systemDur.getNanos() == dt.getNanos();
        }
        return false;
    }

    @Override
    /*
     * * Reasonably unique hashCode, since we're providing an equals method.
     * 
     * @return a hashCode varying by the most significant fields, millis and nanos.
     */
    public int hashCode() {
        return systemDur.hashCode();
    }

    /**
     * Return the global configuration used by DateTime.
     * 
     * @return the global DateTimeConfig object used by DateTime.
     */
    public IDateTimeConfig config() {
        if (this.config == null) {
            this.config = DateTimeConfig.getGlobalDefault();
        }
        return this.config;
    }

}