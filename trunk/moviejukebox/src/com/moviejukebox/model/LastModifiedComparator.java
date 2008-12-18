package com.moviejukebox.model;

import java.util.Comparator;

/**
 *
 * @author altman.matthew
 */
public class LastModifiedComparator implements Comparator<Movie> {

    @Override
    public int compare(Movie movie1, Movie movie2) {
        int retVal = 0;
        if (movie1.getLastModifiedTimestamp() > movie2.getLastModifiedTimestamp()) {
            retVal = -1;
        } else if (movie1.getLastModifiedTimestamp() < movie2.getLastModifiedTimestamp()) {
            retVal = 1;
        }
        return retVal;
    }

}
