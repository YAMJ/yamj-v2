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
package com.moviejukebox.model.Comparator;

import com.moviejukebox.model.Movie;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author altman.matthew
 */
public class LastModifiedComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private final boolean ascending;//Sort the videos in ascending date order (oldest first)

    public LastModifiedComparator() {
        // Use default sort of descending
        this.ascending = Boolean.FALSE;
    }

    public LastModifiedComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        int retVal = 0;

        if (movie1.getLastModifiedTimestamp() > movie2.getLastModifiedTimestamp()) {
            retVal = (ascending ? 1 : -1);
        } else if (movie1.getLastModifiedTimestamp() < movie2.getLastModifiedTimestamp()) {
            retVal = (ascending ? -1 : 1);
        }
        return retVal;
    }
}
