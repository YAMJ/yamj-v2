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
import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.HTMLTools;

public class SubBabaPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public SubBabaPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
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

            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("<div align=\"center\"><a href=\"content?id=", index);
                if (index == -1) {
                    break;
                }

                index += 40;

                index = xml.indexOf("<a href=\"content?id=", index);
                if (index == -1) {
                    break;
                }

                index += 20;

                endIndex = xml.indexOf("\">", index);
                if (endIndex == -1) {
                    break;
                }

                String scanPosterID = xml.substring(index, endIndex);

                index = endIndex + 2;

                index = xml.indexOf("<span dir=\"ltr\">", index);
                if (index == -1) {
                    break;
                }

                index += 16;

                endIndex = xml.indexOf("</span>", index);
                if (endIndex == -1) {
                    break;
                }

                String scanName = xml.substring(index, endIndex).trim();

                index = endIndex + 7;

                if (scanName.equalsIgnoreCase(title)) {
                    posterID = scanPosterID;
                }
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
    public IImage getPosterUrl(String id) {
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                // poster URL
                Image posterImage = new Image("http://www.sub-baba.com/site/download.php?type=1&id=" + id);

                // checking poster dimension
                String xml = webBrowser.request("http://www.sub-baba.com/content?id=" + id);
                String imgTag = HTMLTools.extractTag(xml, "<img src=\"site/thumbs/", "/>");
                String width = HTMLTools.extractTag(imgTag, "width=\"", 0, "\"");
                String height = HTMLTools.extractTag(imgTag, "height=\"", 0, "\"");

                // DVD Cover
                if (Integer.parseInt(width) > Integer.parseInt(height)) {
                    posterImage.setSubimage("0, 0, 47, 100");
                }

                return posterImage;
            } catch (IOException error) {
                logger.severe("Failed retreiving SubBaba poster for movie : " + id);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getName() {
        return "subbaba";
    }

}
