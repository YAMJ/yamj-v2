/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class YouTubeTrailersPlugin extends TrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(YouTubeTrailersPlugin.class);
    private static final String TRAILER_TITLE = "TRAILER-";
    private static final String TEXT_FAILED = "Failed to find trailer information for {}";
    // API Key
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_YouTube");
    // URL settings
    private static final String TRAILER_SEARCH_URL = "https://gdata.youtube.com/feeds/api/videos?q=";
    private static final String TRAILER_BASE_URL = "http://www.youtube.com/watch?v=";
    private static final String TRAILER_KEY = "&key=";
    private static final String TRAILER_HD_FLAG = "&hd=true";
    private static final String TRAILER_SAFE_SEARCH = "&safesearch=none";
    private static final String TRAILER_MAX_RESULTS = "&max-results=";
    private static final String TRAILER_VERSION = "&v=2";
    // Properties
    private final boolean hdWanted = PropertiesUtil.getBooleanProperty("youtubetrailer.hdwanted", Boolean.TRUE);
//    private String trailerLanguage = PropertiesUtil.getProperty("youtubetrailer.language", "en");
    private final int maxTrailers = PropertiesUtil.getIntProperty("youtubetrailer.maxtrailers", 1);

    public YouTubeTrailersPlugin() {
        super();
        trailersPluginName = "YouTubeTrailers";
    }

    @Override
    public final boolean generate(Movie movie) {
        if (movie.isTVShow()) {
            LOG.debug("{} is a TV Show, skipping trailer search", movie.getBaseName());
            return Boolean.FALSE;
        }

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        List<YouTubeTrailer> ytTrailers = getTrailerUrl(movie);

        if (ytTrailers.isEmpty()) {
            LOG.debug("No trailers found for {}", movie.getBaseName());
            return Boolean.FALSE;
        } else {
            LOG.debug("Found {} trailers for {} saving upto {} to the movie.", ytTrailers.size(), movie.getBaseName(), maxTrailers);
        }

        for (YouTubeTrailer ytTrailer : ytTrailers) {
            LOG.debug("Saving {} URL: {} to {}", ytTrailer.title, ytTrailer.url, movie.getBaseName());
            addTrailer(movie, ytTrailer);
        }
        movie.setTrailerExchange(Boolean.TRUE);
        return Boolean.TRUE;
    }

    @Override
    public String getName() {
        return "youtube";
    }

    /**
     * Add the trailer to the movie
     *
     * @param movie
     * @param ytTrailer
     */
    private void addTrailer(Movie movie, YouTubeTrailer ytTrailer) {
        ExtraFile tef = new ExtraFile();
        tef.setTitle(TRAILER_TITLE + ytTrailer.getTitle(), getName());
        tef.setFilename(ytTrailer.getUrl());
        movie.addExtraFile(tef);
    }

    /**
     * Get a list of the trailers for a movie
     *
     * @param movie
     * @return
     */
    private List<YouTubeTrailer> getTrailerUrl(Movie movie) {
        List<YouTubeTrailer> trailers = new ArrayList<>();
        String searchUrl = buildUrl(movie.getTitle());

        String webPage;
        try {
            webPage = webBrowser.request(searchUrl);
        } catch (IOException ex) {
            LOG.warn("Failed to retrieve webpage. Error: {}", ex.getMessage());
            return trailers;
        }

        Document doc;

        try {
            doc = DOMHelper.getDocFromString(webPage);
        } catch (MalformedURLException error) {
            LOG.error(TEXT_FAILED, movie.getBaseName());
            LOG.error(SystemTools.getStackTrace(error));
            return trailers;
        } catch (IOException error) {
            LOG.error(TEXT_FAILED, movie.getBaseName());
            LOG.error(SystemTools.getStackTrace(error));
            return trailers;
        } catch (ParserConfigurationException | SAXException error) {
            LOG.error(TEXT_FAILED, movie.getBaseName());
            LOG.error(SystemTools.getStackTrace(error));
            return trailers;
        }

        if (doc == null) {
            return trailers;
        }

        NodeList nlTrailers = doc.getElementsByTagName("media:group");
        Node nTrailer;
        for (int loop = 0; loop < nlTrailers.getLength(); loop++) {
            nTrailer = nlTrailers.item(loop);
            if (nTrailer.getNodeType() == Node.ELEMENT_NODE) {
                Element eTrailer = (Element) nTrailer;
                String title = DOMHelper.getValueFromElement(eTrailer, "media:title");
                String ytId = DOMHelper.getValueFromElement(eTrailer, "yt:videoid");

                addTrailer(movie, new YouTubeTrailer(TRAILER_BASE_URL + ytId, title));
            }
        }

        return trailers;
    }

    private String buildUrl(String title) {
        StringBuilder stringUrl = new StringBuilder(TRAILER_SEARCH_URL);
        try {
            stringUrl.append(URLEncoder.encode(title, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            LOG.warn("Failed to encode movie title: {} - {}", title, ex.getMessage());
            // Something went wrong with the encoding, try the default string
            stringUrl.append(title);
        }
        stringUrl.append("+trailer");

        if (hdWanted) {
            stringUrl.append(TRAILER_HD_FLAG);
        }

        stringUrl.append(TRAILER_SAFE_SEARCH);
        stringUrl.append(TRAILER_MAX_RESULTS).append(maxTrailers);
        stringUrl.append(TRAILER_VERSION);
        stringUrl.append(TRAILER_KEY).append(API_KEY);

        LOG.debug("Trailer Search URL: {}", stringUrl.toString());
        return stringUrl.toString();
    }

    /**
     * Class to store the information on the trailer
     */
    private class YouTubeTrailer {

        private final String url;
        private final String title;

        public YouTubeTrailer(String url, String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
