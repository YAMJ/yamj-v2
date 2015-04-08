/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.model.Comparator;

import com.moviejukebox.tools.PropertiesUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

public class SortIgnorePrefixesComparator implements Comparator<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    private final List<String> sortIgnorePrefixes = new ArrayList<>();
    private boolean inited = false;

    @Override
    public int compare(Object o1, Object o2) {
        if (!inited) {
            initSortIgnorePrefixes();
        }

        return getStrippedTitle((String) o1).compareToIgnoreCase(getStrippedTitle((String) o2));
    }

    private String getStrippedTitle(String title) {
        String lowerTitle = title.toLowerCase();

        for (String prefix : sortIgnorePrefixes) {
            if (lowerTitle.startsWith(prefix.toLowerCase())) {
                return title.substring(prefix.length());
            }
        }

        return title;
    }

    public void initSortIgnorePrefixes() {
        String temp = PropertiesUtil.getProperty("sorting.strip.prefixes", null);
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                sortIgnorePrefixes.add(token.toLowerCase());
            }
        }
        inited = true;
    }
}
