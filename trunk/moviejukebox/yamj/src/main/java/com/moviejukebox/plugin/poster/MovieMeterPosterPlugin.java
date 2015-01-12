/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieMeterPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;
    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPosterPlugin.class);
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
            LOG.debug("{}MovieMeterPosterPlugin: Failed to create session - {}", LOG_MESSAGE, error.getMessage());
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

            if (StringUtils.isNumeric(moviemeterId)) {
                return moviemeterId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (IOException error) {
            LOG.error("{}Failed retreiving moviemeter Id from Google for movie: {}", LOG_MESSAGE, movieName);
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            LOG.debug("{}Preferred search engine for moviemeter id: {}", LOG_MESSAGE, preferredSearchEngine);
            if ("google".equalsIgnoreCase(preferredSearchEngine)) {
                // Get moviemeter website from google
                LOG.debug("{}Searching google.nl to get moviemeter.nl id", LOG_MESSAGE);
                response = getMovieMeterIdFromGoogle(title, year);
                LOG.debug("{}Returned id: {}", LOG_MESSAGE, response);
            } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
                response = Movie.UNKNOWN;
            } else {
                LOG.debug("{}Searching moviemeter.nl for title: {}", LOG_MESSAGE, title);

                Map filmInfo = session.getMovieDetailsByTitleAndYear(title, year);
                response = filmInfo.get("filmId").toString();
            }

        } catch (Exception ex) {
            LOG.error("{}Failed retreiving CaratulasdecinePoster Id for movie : {}", LOG_MESSAGE, title);
            LOG.error(SystemTools.getStackTrace(ex));
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

            } catch (Exception ex) {
                LOG.error("{}Failed retreiving CaratulasdecinePoster url for movie : {}", LOG_MESSAGE, id);
                LOG.error(SystemTools.getStackTrace(ex));
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
