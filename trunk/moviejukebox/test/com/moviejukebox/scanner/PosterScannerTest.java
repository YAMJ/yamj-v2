/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

package com.moviejukebox.scanner;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbInfo;

public class PosterScannerTest extends TestCase {

    private Movie movieTest;

    protected void setUp() throws Exception {
        super.setUp();
        movieTest = new Movie();
    }

    @SuppressWarnings("unused")
    public void testGetPosterURLFromMovieDbAPI() throws UnsupportedEncodingException {
        String baseURL = "http://www.moviecovers.com/getjpg.html/";
        String expectedURL = Movie.UNKNOWN;
        String returnURL = Movie.UNKNOWN;
        movieTest.setTitle("Dracula Un Muerto Muy Contento Y Feliz");
        ImdbInfo imdbInfo = new ImdbInfo();
    }

    @SuppressWarnings("unused")
    public void testGetPosterURLFromMovieCovers() throws UnsupportedEncodingException {
        String baseURL = "http://www.moviecovers.com/getjpg.html/";
        String expectedURL = Movie.UNKNOWN;
        String returnURL = Movie.UNKNOWN;

        /*
         * movieTest.setTitle("Marius"); movieTest.setYear("1931"); expectedURL = baseURL + "MARIUS.jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("FANNY"); movieTest.setYear("1932"); expectedURL = baseURL + "FANNY.jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("CESAR"); movieTest.setYear("1936"); expectedURL = baseURL + "CESAR.jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("1001 Pattes"); movieTest.setYear("1998"); expectedURL = baseURL + "1001%20PATTES.jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("Ali Baba et les 40 voleurs"); movieTest.setYear("1954"); expectedURL = baseURL + "ALI-BABA%20ET%20LES%2040%20VOLEURS.jpg";
         * returnURL = PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("Ali Baba et les 40 voleurs"); movieTest.setYear("1944"); expectedURL = baseURL +
         * "ALI%20BABA%20ET%20LES%2040%20VOLEURS%20(1944).jpg"; returnURL = PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL,
         * returnURL);
         * 
         * movieTest.setTitle("Ali Baba et les 40 voleurs"); movieTest.setYear("2007"); expectedURL = baseURL +
         * "ALI%20BABA%20ET%20LES%2040%20VOLEURS%20(2007).jpg"; returnURL = PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL,
         * returnURL);
         * 
         * movieTest.setTitle("L'Autre"); movieTest.setYear("2008"); expectedURL = baseURL + "L'AUTRE%20(2008).jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         * 
         * movieTest.setTitle("Looking for Eric"); movieTest.setYear("2009"); expectedURL = baseURL + "LOOKING%20FOR%20ERIC.jpg"; returnURL =
         * PosterScanner.getPosterURLFromMovieCovers(movieTest); assertEquals(expectedURL, returnURL);
         */
    }

}
