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

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.subbabaapi.SubBabaApi;
import com.omertron.subbabaapi.model.SearchType;
import com.omertron.subbabaapi.model.SubBabaMovie;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class SubBabaPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {

    private static final Logger LOGGER = Logger.getLogger(SubBabaPosterPlugin.class);
    private static final String LOG_MESSAGE = "SubBabaPosterPlugin: ";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_SubBaba");
    private WebBrowser webBrowser;
    private SubBabaApi subBaba;

    public SubBabaPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        subBaba = new SubBabaApi(API_KEY);
        webBrowser = new WebBrowser();
    }

    /**
     * Get the Sub-Baba ID for the movie
     *
     * @param title
     * @param year
     * @return
     */
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        List<SubBabaMovie> sbMovies = subBaba.searchByEnglishName(title, SearchType.POSTERS);

        if (sbMovies != null && !sbMovies.isEmpty()) {
            for (SubBabaMovie sbm : sbMovies) {
                if (sbm != null && sbm.getId() > 0) {
                    return Integer.toString(sbm.getId());
                }
            }
        }
        return Movie.UNKNOWN;
    }

    /**
     * retrieve the sub-baba.com poster url matching the specified movie name.
     */
    public String getIdFromMovieInfo_OLD(String title, String year) {
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
            LOGGER.error(LOG_MESSAGE + "Failed retreiving SubBaba Id for movie : " + title);
            LOGGER.error(LOG_MESSAGE + "Error : " + error.getMessage());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        if (StringUtils.isNumeric(id)) {
            return Image.UNKNOWN;
        } else {
            return Image.UNKNOWN;
        }
    }

    public IImage getPosterUrl_OLD(String id) {
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
                LOGGER.error(LOG_MESSAGE + "Failed retreiving SubBaba poster information (" + posterUrl + "): " + error.getMessage());
            }

            // Save the filename to the poster.
            posterImage.setUrl(posterUrl.toString());

            // checking poster dimension
            Dimension imageDimension = PosterScanner.getUrlDimensions(posterUrl.toString());

            // DVD Cover
            if (imageDimension.getWidth() > imageDimension.getHeight()) {
                LOGGER.debug(LOG_MESSAGE + "Detected DVD Cover, cropping image to poster size.");
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

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getName() {
        return "subbaba";
    }
}
