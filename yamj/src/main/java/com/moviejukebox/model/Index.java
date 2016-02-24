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
package com.moviejukebox.model;

import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class Index extends TreeMap<String, List<Movie>> {
    private int maxCategories = -1;
    private boolean display = true;

    private static final long serialVersionUID = -6240040588085931654L;

    public Index(Comparator<? super String> comp) {
        super(comp);
    }

    public Index() {
        super();
    }

    public Index(boolean display) {
        this();
        this.display = display;
    }

    public boolean display() {
        return display;
    }

    protected void addMovie(String category, Movie movie) {
        if (StringTools.isNotValidString(category)) {
            return;
        }

        if (movie == null) {
            return;
        }

        List<Movie> list = get(category);

        if (list == null) {
            if (maxCategories > 0 && size() >= maxCategories) {
                return;
            }
            list = new ArrayList<>();
            put(category, list);
        }

        if (!list.contains(movie)) {
            list.add(movie);
        }
    }

    public void removeMovie(String category, Movie movie) {
        if (StringTools.isNotValidString(category)) {
            return;
        }

        if (movie == null) {
            return;
        }

        List<Movie> list = get(category);

        if (list == null || list.isEmpty()) {
            return;
        }

        if (list.contains(movie)) {
            list.remove(movie);
        }
    }

    public int getMaxCategories() {
        return maxCategories;
    }

    public void setMaxCategories(int maxCategories) {
        this.maxCategories = maxCategories;
    }

}
