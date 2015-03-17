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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author ilgizar
 */
public class IndexComparator implements Comparator<Map.Entry<String, List<Movie>>>, Serializable {

    private static final long serialVersionUID = 1L;
    private transient Library library = null;
    private transient String categoryName = null;

    public IndexComparator(Library library, String categoryName) {
        this.library = library;
        this.categoryName = categoryName;
    }

    @Override
    public int compare(Map.Entry<String, List<Movie>> first, Map.Entry<String, List<Movie>> second) {
        if (library == null || categoryName == null) {
            return 0;
        }
        return library.getMovieCountForIndex(categoryName, FileTools.createCategoryKey(second.getKey())) - library.getMovieCountForIndex(categoryName, FileTools.createCategoryKey(first.getKey()));
    }
}
