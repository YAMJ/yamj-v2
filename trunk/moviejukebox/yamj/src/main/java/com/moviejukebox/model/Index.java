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
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import com.moviejukebox.tools.StringTools;

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
            list = new ArrayList<Movie>();
            put(category, list);
        }
        if (!list.contains(movie)) {
            list.add(movie);
        }
    }

    public int getMaxCategories() {
        return maxCategories;
    }

    public void setMaxCategories(int maxCategories) {
        this.maxCategories = maxCategories;
    }

}
