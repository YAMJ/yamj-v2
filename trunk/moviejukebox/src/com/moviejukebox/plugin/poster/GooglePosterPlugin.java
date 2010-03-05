/*
 *      Copyright (c) 2004-2009 YAMJ Members
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
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.tools.WebBrowser;

public class GooglePosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    // private int nbRetry;

    public GooglePosterPlugin() {
        super();
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
    public String getPosterUrl(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://images.google.fr/images?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&gbv=2");

            String xml = webBrowser.request(sb.toString());
            // int tryLeft = nbRetry;
            int startSearch = 0;
            // while (tryLeft-- > 0 && Movie.UNKNOWN.equalsIgnoreCase(response)) {
            // logger.finest("GooglePosterPlugin: Try " + (nbRetry - tryLeft) + "/" + nbRetry);
            String searchString = "imgurl=";
            int beginIndex = xml.indexOf(searchString, startSearch) + 7;

            if (beginIndex != -1) {
                startSearch = beginIndex + searchString.length();
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"&");
                response = st.nextToken();
                if (!PosterScanner.validatePoster(response)) {
                    response = Movie.UNKNOWN;
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
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        return getPosterUrl(id, null);
    }

    @Override
    public String getName() {
        return "google";
    }

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
