/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.util.Comparator;

import com.moviejukebox.model.Movie;

public class MovieSetComparator implements Comparator<Movie> {

    private String set;

    public MovieSetComparator(String set) {
        this.set = set;
    }

    @Override
    public int compare(Movie m1, Movie m2) {
        Integer o1 = m1.getSetOrder(set);
        Integer o2 = m2.getSetOrder(set);

        // If one is explicitly ordered and the other isn't, the ordered one comes first
        if (o1 == null && o2 != null || o1 != null && o2 == null) {
            return o2 == null ? -1 : 1;

            // If they're both ordered and the value is different, order by that
        } else if (o1 != null && !o1.equals(o2)) {
            return o1.compareTo(o2);

            // Either the order is the same, or neither have an order, so fall back to releaseDate, then titleSort
        } else {
            int c = m1.getYear().compareTo(m2.getYear());
            if (c == 0) {
                c = m1.getTitleSort().compareTo(m2.getTitleSort());
            }
            return c;
        }
    }

}
