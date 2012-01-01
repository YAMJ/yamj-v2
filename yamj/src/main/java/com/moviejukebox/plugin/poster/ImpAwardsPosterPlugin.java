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
package com.moviejukebox.plugin.poster;

import java.net.URLEncoder;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

public class ImpAwardsPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;

    private static final String URL_PREFIX = "http://www.impawards.com/googlesearch.html?cx=partner-pub-6811780361519631%3A48v46vdqqnk&cof=FORID%3A9&ie=ISO-8859-1&q=";
    private static final String URL_SUFFIX = "&sa=Search&siteurl=www.impawards.com%252F";
    
    public ImpAwardsPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String returnString = Movie.UNKNOWN;
        String content = null;

        try {
            StringBuilder searchURL = new StringBuilder();
            searchURL.append(URL_PREFIX);
            searchURL.append(URLEncoder.encode(title + " " + year, "UTF-8"));
            searchURL.append(URL_SUFFIX);
            
            //content = webBrowser.request("http://www.google.com/custom?sitesearch=www.impawards.com&q=" + URLEncoder.encode(title + " " + year, "UTF-8"));
            content = webBrowser.request(searchURL.toString());
        } catch (Exception error) {
            // The movie doesn't exists, so return unknown
            return Movie.UNKNOWN;
        }

        if (content != null) {
            int indexMovieLink = content.indexOf("<a href=\"http://www.impawards.com/" + year + "/");
            if (indexMovieLink != -1) {
                int endIndex = content.indexOf("\"", indexMovieLink + 39);
                if (endIndex != -1) {
                    String tmp = new String(content.substring(indexMovieLink + 39, endIndex));
                    if (!tmp.endsWith("standard.html")) {
                        returnString = year + "/posters/" + new String(content.substring(indexMovieLink + 39, endIndex));
                    }
                }
            }
        }

        return returnString;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        
        if (StringTools.isValidString(id)) {
            if (id.endsWith(".html")) {
                posterURL = "http://www.impawards.com/" + new String(id.substring(0, id.length() - 4)) + "jpg";
            }
        }
        
        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "impawards";
    }

}
