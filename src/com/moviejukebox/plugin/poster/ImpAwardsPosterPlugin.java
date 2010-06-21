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

package com.moviejukebox.plugin.poster;

import java.net.URLEncoder;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.WebBrowser;

public class ImpAwardsPosterPlugin extends AbstractMoviePosterPlugin {

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
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (id != null && !Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (id.endsWith(".html")) {
                posterURL = "http://www.impawards.com/" + id.substring(0, id.length() - 4) + "jpg";
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "impawards";
    }

}
