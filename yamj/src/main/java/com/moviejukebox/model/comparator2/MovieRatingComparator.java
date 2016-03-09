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
package com.moviejukebox.model.comparator2;

import com.moviejukebox.model.Movie;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author altman.matthew
 */
public class MovieRatingComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private final boolean ascending;

    public MovieRatingComparator() {
        this.ascending = Boolean.FALSE;
    }

    public MovieRatingComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return compare(movie1, movie2, ascending);
    }

    /**
     * Compare the rating of two movies.
     *
     * @param movie1
     * @param movie2
     * @param ascending
     * @return
     */
    public int compare(Movie movie1, Movie movie2, boolean ascending) {
        return ascending ? (movie1.getRating() - movie2.getRating()) : (movie2.getRating() - movie1.getRating());
    }
}
