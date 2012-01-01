/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.model.Comparator;

import java.util.Comparator;

import com.moviejukebox.model.Movie;

/**
 * @author altman.matthew
 */
public class RatingComparator implements Comparator<Movie> {

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return compare(movie1, movie2, true);
    }
    
    /**
     * Compare the rating of two movies.
     * @param movie1
     * @param movie2
     * @param descending
     * @return
     */
    public int compare(Movie movie1, Movie movie2, boolean descending) {
        if (descending) {
            // Sort descending
            return movie2.getRating() - movie1.getRating();
        } else {
            // Sort ascending
            return movie1.getRating() - movie2.getRating();
        }
    }
}
