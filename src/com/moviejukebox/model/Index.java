package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import com.moviejukebox.tvrage.tools.StringTools;

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
        if (!StringTools.isValidString(category)) {
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
