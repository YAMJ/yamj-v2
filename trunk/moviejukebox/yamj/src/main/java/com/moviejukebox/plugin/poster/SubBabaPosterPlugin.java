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

import java.awt.Dimension;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.tools.WebBrowser;

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
//            String searchURL = "http://www.sub-baba.com/search?page=search&type=all&submit=%E7%F4%F9&search=" + URLEncoder.encode(title, "iso-8859-8");
            StringBuilder searchURL = new StringBuilder("http://www.sub-baba.com/search/query/");
            searchURL.append(URLEncoder.encode(title, "iso-8859-8"));
            searchURL.append("/type/1/");
            
            String xml = webBrowser.request(searchURL.toString());
            String posterID = Movie.UNKNOWN;

            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("<div align=\"center\"><a href=\"/content/poster/", index);
                if (index == -1) {
                    break;
                }

                index += 45;

                index = xml.indexOf("<a href=\"/content/poster/", index);
                if (index == -1) {
                    break;
                }

                index += 25;

                endIndex = xml.indexOf("/\" ", index);
                if (endIndex == -1) {
                    break;
                }

                String scanPosterID = new String(xml.substring(index, endIndex));

                index = endIndex + 3;

                index = xml.indexOf("<span dir=\"ltr\">", index);
                if (index == -1) {
                    break;
                }

                index += 16;

                endIndex = xml.indexOf("</span></a>", index);
                if (endIndex == -1) {
                    break;
                }

                String scanName = new String(xml.substring(index, endIndex)).trim();
                index = endIndex + 11;

                if (scanName.equalsIgnoreCase(title)) {
                    posterID = scanPosterID;
                    // We have a correct ID, so quit
                    break;
                }
            }
            
            if (!Movie.UNKNOWN.equals(posterID)) {
                response = posterID;
            }

        } catch (Exception error) {
            logger.error("SubBabaPosterPlugin: Failed retreiving SubBaba Id for movie : " + title);
            logger.error("SubBabaPosterPlugin: Error : " + error.getMessage());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        if (!Movie.UNKNOWN.equals(id)) {
            Image posterImage = new Image();

            // This is the page that contains the poster URL
            StringBuilder posterUrl = new StringBuilder("http://www.sub-baba.com/download/");
            posterUrl.append(id).append("/2");
            
            // Load the poster page and extract the image filename
            try {
                String xml = webBrowser.request(posterUrl.toString());
                
                int startIndex = xml.indexOf("<img src=\"");
                if (startIndex == -1) {
                    return posterImage;
                }
                startIndex += 10;
                
                int endIndex = xml.indexOf("\" width=", startIndex);
                if (endIndex == -1) {
                    return posterImage;
                }
                
                posterUrl = new StringBuilder("http://www.sub-baba.com");
                posterUrl.append(xml.substring(startIndex, endIndex));
                
            } catch (IOException error) {
                logger.error("SubBabaPosterPlugin: Failed retreiving SubBaba poster information (" + posterUrl + "): " + error.getMessage());
            }

            // Save the filename to the poster.
            posterImage.setUrl(posterUrl.toString());

            // checking poster dimension
            Dimension imageDimension = PosterScanner.getUrlDimensions(posterUrl.toString());
            
            // DVD Cover
            if (imageDimension.getWidth() > imageDimension.getHeight()) {
                logger.debug("SubBabaPosterPlugin: Detected DVD Cover, cropping image to poster size.");
                posterImage.setSubimage("0, 0, 47, 100");
            }

            return posterImage;
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
