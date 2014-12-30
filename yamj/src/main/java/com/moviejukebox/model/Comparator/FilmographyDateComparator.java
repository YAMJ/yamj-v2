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

import com.moviejukebox.model.Filmography;
import com.moviejukebox.tools.DateTimeTools;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Compare two filmographies by date
 * @author stuart.boston
 */
public class FilmographyDateComparator implements Comparator<Filmography>, Serializable {

    private static final long serialVersionUID = 1L;
    private final boolean ascending;

    public FilmographyDateComparator() {
        this.ascending = Boolean.FALSE;
    }

    public FilmographyDateComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Filmography f1, Filmography f2) {
        return compare(f1, f2, ascending);
    }

    /**
     * Compare two filmographies based on the respective years
     *
     * @param film1
     * @param film2
     * @param ascending
     * @return
     */
    public int compare(Filmography film1, Filmography film2, boolean ascending) {
        boolean valid1 = isValidString(film1.getYear());
        boolean valid2 = isValidString(film2.getYear());

        if (!valid1 && !valid2) {
            return 0;
        }

        if (!valid1) {
            return ascending ? -1 : 1;
        }

        if (!valid2) {
            return ascending ? 1 : -1;
        }

        int year1 = DateTimeTools.extractYear(film1.getYear());
        int year2 = DateTimeTools.extractYear(film2.getYear());
        return ascending ? (year1 - year2) : (year2 - year1);
    }
}
