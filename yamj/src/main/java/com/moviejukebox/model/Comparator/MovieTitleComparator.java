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
import java.io.Serializable;
import java.util.Comparator;

public class MovieTitleComparator implements Comparator<Movie>, Serializable {

    private boolean ascending;

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
        } else {
            return movie2.getStrippedTitleSort().compareTo(movie1.getStrippedTitleSort());
        }
    }
}
