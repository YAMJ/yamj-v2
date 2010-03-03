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
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.WebBrowser;

public class MovieCoversPosterPlugin implements IMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    public MovieCoversPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String returnString = Movie.UNKNOWN;

        try {
            StringBuffer sb = new StringBuffer("http://www.google.com/search?meta=&q=site%3Amoviecovers.com+");
            // tryout another google layout Issue #1250
            sb.append('\"');
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append('\"');
            // adding movie year in search could reduce ambiguities
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append(URLEncoder.encode(" ", "UTF-8"));
                sb.append(URLEncoder.encode(year, "UTF-8"));
            }
            String content = webBrowser.request(sb.toString());
            if (content != null) {

                int indexEndLink = content.indexOf("html\"><b>");
                if (indexEndLink >= 0) {
                    String subContent = content.substring(0, indexEndLink);
                    int indexStartLink = subContent.lastIndexOf("<a href=\"http://www.moviecovers.com/film/titre_");
                    if (indexStartLink >= 0) {
                        returnString = content.substring(indexStartLink + 47, indexEndLink);
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("MovieCoversPosterPlugin: Failed retreiving Moviecovers poster URL: " + title);
            logger.severe("MovieCoversPosterPlugin: Error : " + error.getMessage());
            return Movie.UNKNOWN;
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
            returnString = "http://www.moviecovers.com/getjpg.html/" + id.substring(0, id.lastIndexOf('.')).replace("+", "%20");
        }
        return returnString;
    }

    @Override
    public String getName() {
        return "moviecovers";
    }

}
