package com.moviejukebox.model;

import java.util.Comparator;

/**
 *
 * @author altman.matthew
 */
public class Top250Comparator implements Comparator<Movie> {

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return movie1.getTop250() - movie2.getTop250();
    }

}
