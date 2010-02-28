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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieMeterPluginSession;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class MovieMeterPosterPlugin implements IPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    private String preferredSearchEngine;
    private MovieMeterPluginSession session;

    public MovieMeterPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
        preferredSearchEngine = PropertiesUtil.getProperty("moviemeter.id.search", "moviemeter");
        try {
            session = new MovieMeterPluginSession();
        } catch (XmlRpcException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    private String getMovieMeterIdFromGoogle(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://www.google.nl/search?hl=nl&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Amoviemeter.nl/film");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("www.moviemeter.nl/film/");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 23), "/\"");
            String moviemeterId = st.nextToken();

            if (isInteger(moviemeterId)) {
                return moviemeterId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving moviemeter Id from Google for movie : " + movieName);
            logger.severe("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        try {
            logger.finest("Preferred search engine for moviemeter id: " + preferredSearchEngine);
            if ("google".equalsIgnoreCase(preferredSearchEngine)) {
                // Get moviemeter website from google
                logger.finest("Searching google.nl to get moviemeter.nl id");
                response = getMovieMeterIdFromGoogle(title, year);
                logger.finest("Returned id: " + response);
            } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
                response = Movie.UNKNOWN;
            } else {
                logger.finest("Searching moviemeter.nl for title: " + title);

                HashMap filmInfo = session.getMovieDetailsByTitleAndYear(title, year);
                response = filmInfo.get("filmId").toString();
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving CaratulasdecinePoster Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        // <td><img src="
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                HashMap filmInfo = session.getMovieDetailsById(Integer.valueOf(id));
                response = filmInfo.get("thumbnail").toString().replaceAll("thumbs/", "");

            } catch (Exception e) {
                logger.severe("Failed retreiving CaratulasdecinePoster url for movie : " + id);
                logger.severe("Error : " + e.getMessage());
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }

    @Override
    public String getName() {
        return "Caratulasdecine";
    }

    @Override
    public String getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }
}
