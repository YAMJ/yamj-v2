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

package com.moviejukebox.plugin.poster;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.tools.WebBrowser;

public class GooglePosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    // private int nbRetry;

    public GooglePosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
        // String retry = PropertiesUtil.getProperty("poster.scanner.google.retry", "3");
        // try {
        // nbRetry = Integer.parseInt(retry);
        // } catch (Exception ex) {
        // nbRetry = 3;
        // }
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        // No id from google search, return title
        return title;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        Image posterImage = new Image();
        try {
            StringBuffer sb = new StringBuffer("http://images.google.fr/images?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&gbv=2");

            String xml = webBrowser.request(sb.toString());
            // int tryLeft = nbRetry;
            int startSearch = 0;
            // while (tryLeft-- > 0 && Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
            // logger.finest("GooglePosterPlugin: Try " + (nbRetry - tryLeft) + "/" + nbRetry);
            String searchString = "imgurl=";
            int beginIndex = xml.indexOf(searchString, startSearch) + 7;

            if (beginIndex != -1) {
                startSearch = beginIndex + searchString.length();
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"&");
                // QuickFix to "too much translation issue" space char -> %20 -> %2520
                posterImage.setUrl(st.nextToken().replace("%2520", "%20"));
                if (!PosterScanner.validatePoster(posterImage)) {
                    posterImage.setUrl(Movie.UNKNOWN);
                }
            }
            // } else {
            // // Stop try no more result.
            // tryLeft = 0;
            // }
            // }
        } catch (Exception error) {
            logger.severe("GooglePosterPlugin: Failed retreiving poster URL from google images : " + title);
            logger.severe("Error : " + error.getMessage());
        }
        return posterImage;
    }

    @Override
    public IImage getPosterUrl(String id) {
        return getPosterUrl(id, null);
    }

    @Override
    public String getName() {
        return "google";
    }

    @SuppressWarnings("unused")
    private boolean checkPosterUrl(String posterURL) {
        try {
            URL url = new URL(posterURL);
            InputStream in = url.openStream();
            in.close();
            return true;
        } catch (IOException ignore) {
            logger.finest("GooglePosterPlugin: ValidatePoster error: " + ignore.getMessage() + ": can't open url");
            return false; // Quit and return a false poster
        }
    }

}
