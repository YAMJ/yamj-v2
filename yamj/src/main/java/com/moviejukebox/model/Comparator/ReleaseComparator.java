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
package com.moviejukebox.model.Comparator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.StringTools.isValidString;

/**
 * @author ilgizar
 */
public class ReleaseComparator extends YearComparator {
    private Locale locale = Locale.ENGLISH;

    public ReleaseComparator() {
        super(true);
    }

    public ReleaseComparator(boolean ascending) {
        super(ascending);
    }

    @Override
    public int compare(Movie movie1, Movie movie2, boolean ascending) {
        int res = super.compare(movie1, movie2, ascending);
        String date1 = movie1.getReleaseDate();
        String date2 = movie2.getReleaseDate();
        if (res == 0 && isValidString(date1) && isValidString(date2)) {
            date1 = convertDate(date1);
            date2 = convertDate(date2);
            if (isValidString(date1) && isValidString(date2)) {
                try {
                    return ascending ? Integer.parseInt(date1) - Integer.parseInt(date2) : Integer.parseInt(date2) - Integer.parseInt(date1);
                } catch (NumberFormatException e) {
                }
            }
            return isValidString(date1) ? ascending ? 1 : - 1 : isValidString(date2) ? ascending ? -1 : 1 : 0;
        }
        return res;
    }

    private String convertDate(String date) {
        // output date pattern: 19931205
        SimpleDateFormat dstDate = new SimpleDateFormat("yyyyMMdd");

        // pattern for date: 05 December 1993
        Pattern dateRegex = Pattern.compile("\\d{2} \\S+ \\d{4}");
        Matcher dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("dd MMM yyyy", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                 e.printStackTrace();
            }
        }

        // pattern for date: 1993-12-05
        dateRegex = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("yyyy-MM-dd", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                 e.printStackTrace();
            }
        }

        // pattern for date: 05.12.1993
        dateRegex = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
        dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("dd.MM.yyyy", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                 e.printStackTrace();
            }
        }

        // pattern for date: 05.12.93
        dateRegex = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{2}");
        dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("dd.MM.yy", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                 e.printStackTrace();
            }
        }

        // pattern for date: 05/12/1993
        dateRegex = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
        dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("dd/MM/yyyy", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                 e.printStackTrace();
            }
        }

        // pattern for date: December 1993
        dateRegex = Pattern.compile("\\S+ \\d{4}");
        dateMatch = dateRegex.matcher(date);
        if (dateMatch.find()) {
            SimpleDateFormat srcDate = new SimpleDateFormat("MMM yyyy", locale);
            try {
                return dstDate.format(srcDate.parse(dateMatch.group(0)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return Movie.UNKNOWN;
    }
}
