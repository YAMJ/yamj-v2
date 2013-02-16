/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import com.moviejukebox.tools.PropertiesUtil;
import java.io.Serializable;
import java.util.Comparator;

public class MovieSetComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private String set;
    private static final boolean SPECIALS_AT_END = PropertiesUtil.getBooleanProperty("mjb.sets.specialsAtEnd", Boolean.FALSE);

    public MovieSetComparator(String set) {
        this.set = set;
    }

    @Override
    public int compare(Movie m1, Movie m2) {
        if (m1.isTVShow() && m2.isTVShow()) {
            if (SPECIALS_AT_END) {
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
