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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class YouTubeTrailersPlugin extends TrailerPlugin {

    private static final Logger logger = Logger.getLogger(YouTubeTrailersPlugin.class);
    private static final String TRAILER_TITLE = "TRAILER-";
    private static final String TEXT_FAILED = "Failed to trailer information for ";
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
    private boolean hdWanted = PropertiesUtil.getBooleanProperty("youtubetrailer.hdwanted", TRUE);
//    private String trailerLanguage = PropertiesUtil.getProperty("youtubetrailer.language", "en");
    private int maxTrailers = PropertiesUtil.getIntProperty("youtubetrailer.maxtrailers", "1");

    public YouTubeTrailersPlugin() {
        super();
        trailersPluginName = "YouTubeTrailers";
        LOG_MESSAGE = "YouTubeTrailersPlugin: ";
    }

    @Override
    public final boolean generate(Movie movie) {
        if (movie.isTVShow()) {
            logger.debug(LOG_MESSAGE + movie.getBaseName() + " is a TV Show, skipping trailer search");
            return Boolean.FALSE;
        }

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        List<YouTubeTrailer> ytTrailers = getTrailerUrl(movie);

        StringBuilder sb = new StringBuilder(LOG_MESSAGE);
        if (ytTrailers.isEmpty()) {
            sb.append("No trailers found for ");
            sb.append(movie.getBaseName());
            logger.debug(sb.toString());
            return Boolean.FALSE;
        } else {
            sb.append("Found ");
            sb.append(ytTrailers.size());
            sb.append(" trailers for ");
            sb.append(movie.getBaseName());
            if (ytTrailers.size() >= maxTrailers) {
                sb.append(" saving the first ");
                sb.append(maxTrailers);
                sb.append(" to movie");
            } else {
                sb.append(" saving them all to the movie");
            }
            logger.debug(sb.toString());
        }

        for (YouTubeTrailer ytTrailer : ytTrailers) {
            addTrailer(movie, ytTrailer);

            sb = new StringBuilder(LOG_MESSAGE);
            sb.append("Saving ");
            sb.append(ytTrailer.title);
            sb.append(" URL: ");
            sb.append(ytTrailer.url);
            sb.append(" to ");
            sb.append(movie.getBaseName());
            logger.debug(sb.toString());
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
        tef.setTitle(TRAILER_TITLE + ytTrailer.getTitle());
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
        List<YouTubeTrailer> trailers = new ArrayList<YouTubeTrailer>();
        String searchUrl = buildUrl(movie.getTitle());

        String webPage;
        try {
            webPage = webBrowser.request(searchUrl);
        } catch (IOException ex) {
            logger.warn(LOG_MESSAGE + "Failed to retrieve webpage. Error: " + ex.getMessage());
            return trailers;
        }

        Document doc;

        try {
            doc = DOMHelper.getDocFromString(webPage);
        } catch (MalformedURLException error) {
            logger.error(LOG_MESSAGE + TEXT_FAILED + movie.getBaseName());
            logger.error(SystemTools.getStackTrace(error));
            return trailers;
        } catch (IOException error) {
            logger.error(LOG_MESSAGE + TEXT_FAILED + movie.getBaseName());
            logger.error(SystemTools.getStackTrace(error));
            return trailers;
        } catch (ParserConfigurationException error) {
            logger.error(LOG_MESSAGE + TEXT_FAILED + movie.getBaseName());
            logger.error(SystemTools.getStackTrace(error));
            return trailers;
        } catch (SAXException error) {
            logger.error(LOG_MESSAGE + TEXT_FAILED + movie.getBaseName());
            logger.error(SystemTools.getStackTrace(error));
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
            logger.warn(LOG_MESSAGE + "Failed to encode movie title: " + title + " - " + ex.getMessage());
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

        logger.info(LOG_MESSAGE + "Trailer Search URL: " + stringUrl.toString());
        return stringUrl.toString();
    }

    /**
     * Class to store the information on the trailer
     */
    private class YouTubeTrailer {

        private String url;
        private String title;

        public YouTubeTrailer() {
            url = Movie.UNKNOWN;
            title = Movie.UNKNOWN;
        }

        public YouTubeTrailer(String url, String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "YouTubeTrailer{" + "url=" + url + ", title=" + title + '}';
        }
    }
}
