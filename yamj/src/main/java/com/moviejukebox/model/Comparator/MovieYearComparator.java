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

import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.util.Comparator;

/**
 * @author ilgizar
 */
public class MovieYearComparator implements Comparator<Movie> {

    private boolean ascending = Boolean.TRUE;

    public MovieYearComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return compare(movie1, movie2, ascending);
    }

    public int compare(Movie movie1, Movie movie2, boolean ascending) {
        if (isValidString(movie1.getYear()) && isValidString(movie2.getYear())) {
            try {
                return ascending ? (Integer.parseInt(movie1.getYear()) - Integer.parseInt(movie2.getYear())) : (Integer.parseInt(movie2.getYear()) - Integer.parseInt(movie1.getYear()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return isValidString(movie1.getYear()) ? ascending ? 1 : -1 : isValidString(movie2.getYear()) ? ascending ? -1 : 1 : 0;
    }
}
