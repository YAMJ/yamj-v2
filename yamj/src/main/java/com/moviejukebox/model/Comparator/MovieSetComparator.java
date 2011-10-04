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
import com.moviejukebox.tools.PropertiesUtil;

public class MovieSetComparator implements Comparator<Movie> {
    private String set;
    private static boolean specialsAtEnd = PropertiesUtil.getBooleanProperty("mjb.sets.specialsAtEnd", "false");

    public MovieSetComparator(String set) {
        this.set = set;
    }

    @Override
    public int compare(Movie m1, Movie m2) {
        if (m1.isTVShow() && m2.isTVShow()) {
            if (specialsAtEnd) {
                // Sort Season 0 to the end of the list (i.e. Season 0 always wins the comparison)
                if (m1.getSeason() == 0) {
                    return 1;
                }
                
                if (m2.getSeason() == 0) {
                    return -1;
                }
            }
        }
        
        Integer o1 = m1.getSetOrder(set);
        Integer o2 = m2.getSetOrder(set);

        // If one is explicitly ordered and the other isn't, the ordered one comes first
        if (o1 == null && o2 != null || o1 != null && o2 == null) {
            return o2 == null ? -1 : 1;
        }

        // If they're both ordered and the value is different, order by that
        if (o1 != null && !o1.equals(o2)) {
            return o1.compareTo(o2);
        }

        // If these are TV shows, then use the season to sort
        if (m1.isTVShow() && m2.isTVShow()) {
            // Sort the season list by season number
            return (m1.getSeason() - m2.getSeason());
        } else {
            // Either the order is the same, or neither have an order, so fall back to releaseDate, then titleSort
            int c = m1.getYear().compareTo(m2.getYear());
            if (c == 0) {
                c = m1.getTitleSort().compareTo(m2.getTitleSort());
            }
            return c;
        }
    }

}
