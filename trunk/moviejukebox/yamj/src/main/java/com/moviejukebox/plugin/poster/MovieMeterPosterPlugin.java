/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieMeterPluginSession;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.net.URLEncoder;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

public class MovieMeterPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;
    private static final Logger LOG = Logger.getLogger(MovieMeterPosterPlugin.class);
    private static final String LOG_MESSAGE = "MovieMeterPosterPlugin:";

    private String preferredSearchEngine;
    private MovieMeterPluginSession session;

    public MovieMeterPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        webBrowser = new WebBrowser();
        preferredSearchEngine = PropertiesUtil.getProperty("moviemeter.id.search", "moviemeter");
        try {
            session = new MovieMeterPluginSession();
        } catch (XmlRpcException error) {
            LOG.debug(LOG_MESSAGE + "MovieMeterPosterPlugin: Failed to create session");
//            logger.error(SystemTools.getStackTrace(error));
        }
    }

    private String getMovieMeterIdFromGoogle(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.nl/search?hl=nl&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
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
            LOG.error(LOG_MESSAGE + "Failed retreiving moviemeter Id from Google for movie : " + movieName);
            LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
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

    @SuppressWarnings("rawtypes")
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            LOG.debug(LOG_MESSAGE + "Preferred search engine for moviemeter id: " + preferredSearchEngine);
            if ("google".equalsIgnoreCase(preferredSearchEngine)) {
                // Get moviemeter website from google
                LOG.debug(LOG_MESSAGE + "Searching google.nl to get moviemeter.nl id");
                response = getMovieMeterIdFromGoogle(title, year);
                LOG.debug(LOG_MESSAGE + "Returned id: " + response);
            } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
                response = Movie.UNKNOWN;
            } else {
                LOG.debug(LOG_MESSAGE + "Searching moviemeter.nl for title: " + title);

                Map filmInfo = session.getMovieDetailsByTitleAndYear(title, year);
                response = filmInfo.get("filmId").toString();
            }

        } catch (Exception e) {
            LOG.error(LOG_MESSAGE + "Failed retreiving CaratulasdecinePoster Id for movie : " + title);
            LOG.error(LOG_MESSAGE + "Error : " + e.getMessage());
        }
        return response;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IImage getPosterUrl(String id) {
        // <td><img src="
        String posterURL = Movie.UNKNOWN;
        if (StringTools.isValidString(id)) {
            try {
                Map filmInfo = session.getMovieDetailsById(Integer.valueOf(id));
                posterURL = filmInfo.get("thumbnail").toString().replaceAll("thumbs/", "");

            } catch (Exception e) {
                LOG.error(LOG_MESSAGE + "Failed retreiving CaratulasdecinePoster url for movie : " + id);
                LOG.error(LOG_MESSAGE + "Error : " + e.getMessage());
            }
        }

        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "movieposter";
    }
}
