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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.logging.Logger;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.IImage;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class CdonPosterPlugin implements IMoviePosterPlugin, ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public CdonPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    private String getCdonMovieDetailsPage(String movieURL) {
        String cdonMoviePage;
        try {
            // sanity check on result before trying to load details page from url
            if (!movieURL.isEmpty() && movieURL.contains("http")) {
                // fetch movie page from cdon
                StringBuffer buf = new StringBuffer(movieURL);
                cdonMoviePage = webBrowser.request(buf.toString());
                return cdonMoviePage;
            } else {
                // search didn't even find an url to the movie
                logger.finer("Error in fetching movie detail page from CDON for movie: " + movieURL);
                return Movie.UNKNOWN;
            }
        } catch (Exception error) {
            logger.severe("Error while retreiving CDON image for movie : " + movieURL);
            // logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private String extractCdonPosterUrl(String cdonMoviePage) {

        String cdonPosterURL = Movie.UNKNOWN;
        String[] htmlArray = cdonMoviePage.split("<");

        // check if there is an large front cover image for this movie
        if (cdonMoviePage.contains("St&#246;rre framsida")) {
            // first look for a large cover
            cdonPosterURL = findUrlString("St&#246;rre framsida", htmlArray);
        } else if (cdonMoviePage.contains("/media-dynamic/images/product/")) {
            // if not found look for a small cover
            cdonPosterURL = findUrlString("/media-dynamic/images/product/", htmlArray);
        } else {
            logger.info(" No CDON cover was found for movie ");
        }
        return cdonPosterURL;
    }

    private String findUrlString(String searchString, String[] htmlArray) {
        String result;
        String[] posterURL = null;
        // all cdon pages don't look the same so
        // loop over the array to find the string with a link to an image
        int i = 0;
        for (String s : htmlArray) {
            if (s.contains(searchString)) {
                // found a matching string
                posterURL = htmlArray[i].split("\"|\\s");
                break;
            }
            i++;
        }
        // sanity check again (the found url should point to a jpg)
        if (posterURL.length > 2 && posterURL[2].contains(".jpg")) {
            result = "http://cdon.se" + posterURL[2];
            logger.finest("FilmdeltaSE: found cdon cover: " + result);
            return result;
        } else {
            logger.info("(FilmdeltaSE) Error in finding poster url");
            return Movie.UNKNOWN;
        }
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        String xml = null;

        // Search CDON to get an URL to the movie page
        try {
            StringBuffer sb = new StringBuffer("http://cdon.se/search?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (tvSeason >= 0) {
                sb.append("+").append(URLEncoder.encode("säsong", "UTF-8"));
                sb.append("+" + tvSeason);
            }
            xml = webBrowser.request(sb.toString());
            // find the movie url in the search result page
            if (xml.contains("/section-movie.gif\" alt=\"\" />")) {
                int beginIndex = xml.indexOf("/section-movie.gif\" alt=\"\" />") + 28;
                response = HTMLTools.extractTag(xml.substring(beginIndex), "<td class=\"title\">", 0);
                // Split string to extract the url
                if (response.contains("http")) {
                    String[] splitMovieURL = response.split("\\s");
                    response = splitMovieURL[1].replaceAll("href|=|\"", "");
                    logger.finest("CDon.es: found cdon movie url = " + response);
                } else {
                    response = Movie.UNKNOWN;
                    logger.finer("CDon.es: error extracting movie url for: " + title);
                }

            } else {
                response = Movie.UNKNOWN;
                logger.finer("CDon.es: error finding movieURL..");
            }
        } catch (Exception error) {
            logger.severe("Error while retreiving CDON id for movie : " + title);
            logger.severe("Error : " + error.getMessage());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml = "";
        try {
            xml = getCdonMovieDetailsPage(id);
            // extract poster url and return it
            posterURL = extractCdonPosterUrl(xml);
        } catch (Exception error) {
            logger.severe("Failed retreiving Cdon poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }

    @Override
    public String getName() {
        return "cdon";
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return getIdFromMovieInfo(title, year, -1);
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear(), movieInformation.getSeason());
            } else {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            }
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                ident.setId(getName(), id);
            }
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            } else {
                return getPosterUrl(id);
            }

        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }
}
