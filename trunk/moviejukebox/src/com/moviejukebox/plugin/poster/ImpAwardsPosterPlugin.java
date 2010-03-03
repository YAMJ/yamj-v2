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

import java.net.URLEncoder;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.WebBrowser;

public class ImpAwardsPosterPlugin implements IMoviePosterPlugin {

    private WebBrowser webBrowser;

    public ImpAwardsPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String returnString = Movie.UNKNOWN;
        String content = null;

        try {
            content = webBrowser.request("http://www.google.com/custom?sitesearch=www.impawards.com&q=" + URLEncoder.encode(title + " " + year, "UTF-8"));
        } catch (Exception error) {
            // The movie doesn't exists, so return unknown
            return Movie.UNKNOWN;
        }

        if (content != null) {
            int indexMovieLink = content.indexOf("<a href=\"http://www.impawards.com/" + year + "/");
            if (indexMovieLink != -1) {
                int endIndex = content.indexOf("\"", indexMovieLink + 39);
                if (endIndex != -1) {
                    String tmp = content.substring(indexMovieLink + 39, endIndex);
                    if (!tmp.endsWith("standard.html")) {
                        returnString = year + "/posters/" + content.substring(indexMovieLink + 39, endIndex);
                    }
                }
            }
        }

        return returnString;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getPosterUrl(String id) {
        String returnString = Movie.UNKNOWN;
        if (id != null && !Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (id.endsWith(".html")) {
                returnString = "http://www.impawards.com/" + id.substring(0, id.length() - 4) + "jpg";
            }
        }
        return returnString;
    }

    @Override
    public String getName() {
        return "impawards";
    }

}
