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

public class SubBabaPosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public SubBabaPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    /**
     * retrieve the sub-baba.com poster url matching the specified movie name.
     */
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            String searchURL = "http://www.sub-baba.com/search?page=search&type=all&submit=%E7%F4%F9&search=" + URLEncoder.encode(title, "iso-8859-8");

            String xml = webBrowser.request(searchURL);

            String posterID = Movie.UNKNOWN;
            boolean dvdCover = false;

            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("<div align=\"center\"><a href=\"content?id=", index);
                if (index == -1)
                    break;

                index += 40;

                index = xml.indexOf("width=\"", index);
                if (index == -1)
                    break;

                index += 7;

                endIndex = xml.indexOf("\"", index);
                if (endIndex == -1)
                    break;

                String scanWidth = xml.substring(index, endIndex);

                index = endIndex + 1;

                index = xml.indexOf("height=\"", index);
                if (index == -1)
                    break;

                index += 8;

                endIndex = xml.indexOf("\"", index);
                if (endIndex == -1)
                    break;

                String scanHeight = xml.substring(index, endIndex);

                index = endIndex + 1;

                index = xml.indexOf("<a href=\"content?id=", index);
                if (index == -1)
                    break;

                index += 20;

                endIndex = xml.indexOf("\">", index);
                if (endIndex == -1)
                    break;

                String scanPosterID = xml.substring(index, endIndex);

                index = endIndex + 2;

                index = xml.indexOf("<span dir=\"ltr\">", index);
                if (index == -1)
                    break;

                index += 16;

                endIndex = xml.indexOf("</span>", index);
                if (endIndex == -1)
                    break;

                String scanName = xml.substring(index, endIndex).trim();

                index = endIndex + 7;

                if (scanName.equalsIgnoreCase(title)) {
                    posterID = scanPosterID;

                    if (Integer.parseInt(scanWidth) > Integer.parseInt(scanHeight))
                        dvdCover = true;
                    else
                        dvdCover = false;
                }
                // FIXME - Handle DVD Cover
                // if (dvdCover) {
                // // Cut the dvd cover into normal poster using the left side of the image
                // movie.setPosterSubimage("0, 0, 47, 100");
                // }
            }
            if (!Movie.UNKNOWN.equals(posterID)) {
                response = posterID;
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving SubBaba Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            response = "http://www.sub-baba.com/site/download.php?type=1&id=" + id;
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "subbaba";
    }

}
