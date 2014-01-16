/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * @author altman.matthew
 */
public class CertificationComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 1L;
    private transient List<String> ordering = null;

    public CertificationComparator(List<String> ordering) {
        this.ordering = ordering;
    }

    @Override
    public int compare(String obj1, String obj2) {
        int obj1Pos = ordering.indexOf(obj1);
        int obj2Pos = ordering.indexOf(obj2);

        if (obj1Pos < 0) {
            ordering.add(obj1);
            obj1Pos = ordering.indexOf(obj1);
        }
        if (obj2Pos < 0) {
            ordering.add(obj2);
            obj2Pos = ordering.indexOf(obj2);
        }

        return obj1Pos - obj2Pos;
    }
}
