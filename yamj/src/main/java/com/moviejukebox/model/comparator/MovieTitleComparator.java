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

import java.io.Serializable;
import java.util.Comparator;

import com.moviejukebox.model.Movie;

public class MovieTitleComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private final boolean ascending;

    public MovieTitleComparator() {
        this.ascending = Boolean.TRUE;
    }

    public MovieTitleComparator(Boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        if (ascending) {
            return movie1.getStrippedTitleSort().compareTo(movie2.getStrippedTitleSort());
        }
        return movie2.getStrippedTitleSort().compareTo(movie1.getStrippedTitleSort());
    }
}
