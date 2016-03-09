/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
package com.moviejukebox.model.comparator;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.DateTimeTools;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author ilgizar
 */
public class MovieYearComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private boolean ascending = Boolean.TRUE;

    public MovieYearComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return compare(movie1, movie2, ascending);
    }

    /**
     * Compare two movies based on the respective years
     *
     * @param movie1
     * @param movie2
     * @param ascending
     * @return
     */
    public int compare(Movie movie1, Movie movie2, boolean ascending) {
        boolean valid1 = isValidString(movie1.getYear());
        boolean valid2 = isValidString(movie2.getYear());

        if (!valid1 && !valid2) {
            return 0;
        }

        if (!valid1) {
            return ascending ? -1 : 1;
        }

        if (!valid2) {
            return ascending ? 1 : -1;
        }

        int year1 = DateTimeTools.extractYear(movie1.getYear());
        int year2 = DateTimeTools.extractYear(movie2.getYear());
        return ascending ? (year1 - year2) : (year2 - year1);
    }
}
