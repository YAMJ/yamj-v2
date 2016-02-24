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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbInfo;
import com.moviejukebox.scanner.artwork.PosterScanner;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class PosterScannerTest {

    private Movie movieTest;

    @Before
    public void setUp() {
        movieTest = new Movie();
    }

    @Test
    public void testGetPosterURLFromMovieDbAPI() throws UnsupportedEncodingException {
        String baseURL = "http://www.moviecovers.com/getjpg.html/";
        String expectedURL = Movie.UNKNOWN;
        String returnURL = Movie.UNKNOWN;
        movieTest.setTitle("Dracula Un Muerto Muy Contento Y Feliz", Movie.UNKNOWN);
        ImdbInfo imdbInfo = new ImdbInfo();
    }

    @Test
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
    
    @Test
    public void testGetUrlDimensions() {
        Dimension dimension = PosterScanner.getUrlDimensions("http://image.tmdb.org/t/p/original/ax2uAmXFw9Myj1hiakJ4n2s4Tbg.jpg");
        Assert.assertEquals(1400, dimension.getWidth(), 10);
        Assert.assertEquals(2100, dimension.getHeight(), 10);
    }
}
