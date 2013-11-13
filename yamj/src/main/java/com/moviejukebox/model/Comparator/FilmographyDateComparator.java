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

import com.moviejukebox.model.Filmography;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author stuart.boston
 */
public class FilmographyDateComparator implements Comparator<Filmography>, Serializable {

    private static final long serialVersionUID = 1L;
    private final boolean ascending;

    public FilmographyDateComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Filmography f1, Filmography f2) {
        return compare(f1, f2, ascending);
    }

    public int compare(Filmography f1, Filmography f2, boolean ascending) {
        if (isValidString(f1.getYear()) && isValidString(f2.getYear())) {
            try {
                return ascending ? (Integer.parseInt(f1.getYear()) - Integer.parseInt(f2.getYear())) : (Integer.parseInt(f2.getYear()) - Integer.parseInt(f1.getYear()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return isValidString(f1.getYear()) ? ascending ? 1 : -1 : isValidString(f2.getYear()) ? ascending ? -1 : 1 : 0;
    }

}
