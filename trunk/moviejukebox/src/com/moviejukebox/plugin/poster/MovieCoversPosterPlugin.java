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
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.WebBrowser;


public class MovieCoversPosterPlugin extends AbstractMoviePosterPlugin {
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
            StringBuffer sb = new StringBuffer("http://www.moviecovers.com/multicrit.html?titre=");
            sb.append(URLEncoder.encode(title, "iso-8859-1"));
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("&anneemin=");
                sb.append(URLEncoder.encode(year, "iso-8859-1"));
                sb.append("&anneemax=");
                sb.append(URLEncoder.encode(year, "iso-8859-1"));
            }
            sb.append("&slow=1&tri=Titre&listes=1");
            String content = webBrowser.request(sb.toString());
            if (content != null) {
                int indexStartLink = content.indexOf("/film/titre_");
                if (indexStartLink >= 0) {
                    String subContent = content.substring(indexStartLink + 12);
                    int indexEndLink = subContent.indexOf(".html\">");
                    if (indexEndLink >= 0) {
                        returnString = subContent.substring(0, indexEndLink);
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
    public IImage getPosterUrl(String title, String year) {
       return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        try {
        if (id != null && !Movie.UNKNOWN.equalsIgnoreCase(id)) {
            logger.finer("MovieCoversPosterPlugin : Movie found on moviecovers.com" + id);
            posterURL = "http://www.moviecovers.com/getjpg.html/" + id.replace("+", "%20");
        } else {
            logger.finer("MovieCoversPosterPlugin: Unable to find posters for " + id);
        }
        } catch (Exception error) {
            logger.finer("MovieCoversPosterPlugin: MovieCovers.com API Error: " + error.getMessage());
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "moviecovers";
    }

}
