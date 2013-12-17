/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * @author iuk
 */
public class TrailersLandPlugin extends TrailerPlugin {

    private static final Logger LOG = Logger.getLogger(TrailersLandPlugin.class);
    private static final String TL_BASE_URL = "http://www.trailersland.com/";
    private static final String TL_SEARCH_URL = "cerca/ricerca=";
    private static final String TL_MOVIE_URL = "film/";
    private static final String TL_TRAILER_URL = "trailer/";
    private static final String TL_TRAILER_FILE_URL = "wrapping/tls.php?";
    private final int trailerMaxCount;
    private final String trailerMaxResolution;
    private final String trailerAllowedFormats;
    private final String trailerPreferredLanguages;
    private final String trailerPreferredTypes;

    public TrailersLandPlugin() {
        super();
        trailersPluginName = "TrailersLand";
        logMessage = "TrailersLandPlugin: ";

        trailerMaxCount = PropertiesUtil.getIntProperty("trailersland.max", 3);
        trailerMaxResolution = PropertiesUtil.getProperty("trailersland.maxResolution", RESOLUTION_1080P);
        trailerAllowedFormats = PropertiesUtil.getProperty("trailersland.allowedFormats", "wmv,mov,mp4,avi,mkv,mpg");
        trailerPreferredLanguages = PropertiesUtil.getProperty("trailersland.preferredLanguages", "ita,sub-ita,en");
        trailerPreferredTypes = PropertiesUtil.getProperty("trailersland.preferredTypes", "trailer,teaser");
    }

    @Override
    public final boolean generate(Movie movie) {
        if (trailerMaxResolution.length() == 0) {
            return false;
        }

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        List<TrailersLandTrailer> trailerList = getTrailerUrls(movie);

        if (trailerList == null) {
            LOG.error(logMessage + "Error while scraping");
            return false;
        } else if (trailerList.isEmpty()) {
            LOG.debug(logMessage + "No trailer found");
            return false;
        }

        for (int i = trailerList.size() - 1; i >= 0; i--) {
            TrailersLandTrailer tr = trailerList.get(i);

            String trailerUrl = tr.getUrl();
            LOG.info(logMessage + "Found trailer at URL " + trailerUrl);

            String trailerLabel = Integer.toString(trailerList.size() - i) + "-" + tr.getLang() + "-" + tr.getType();
            ExtraFile extra = new ExtraFile();
            extra.setTitle("TRAILER-" + trailerLabel);

            if (isDownload()) {
                if (!downloadTrailer(movie, trailerUrl, trailerLabel, extra)) {
                    return false;
                }
            } else {
                extra.setFilename(trailerUrl);
                movie.addExtraFile(extra);
            }
        }

        return true;
    }

    @Override
    public String getName() {
        return "trailersland";
    }

    protected String getTrailersLandIdFromTitle(String title) {
        String trailersLandId = Movie.UNKNOWN;
        String searchUrl;

        try {
            searchUrl = TL_BASE_URL + TL_SEARCH_URL + URLEncoder.encode(title, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            LOG.error(logMessage + "Unsupported encoding, cannot build search URL");
            return Movie.UNKNOWN;
        }

        LOG.debug(logMessage + "Searching for movie at URL " + searchUrl);

        String xml;
        try {
            xml = webBrowser.request(searchUrl);
        } catch (IOException error) {
            LOG.error(logMessage + "Failed retreiving TrailersLand Id for movie : " + title);
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }

        int indexRes = xml.indexOf("<div id=\"film_informazioni_ricerca\"");
        if (indexRes >= 0) {
            int indexMovieUrl = xml.indexOf(TL_BASE_URL + TL_MOVIE_URL, indexRes + 1);
            if (indexMovieUrl >= 0) {
                int endMovieUrl = xml.indexOf('"', indexMovieUrl + 1);
                if (endMovieUrl >= 0) {
                    trailersLandId = xml.substring(indexMovieUrl + TL_BASE_URL.length() + TL_MOVIE_URL.length(), endMovieUrl);
                    LOG.debug(logMessage + "Found Trailers Land Id " + trailersLandId);
                }
            } else {
                LOG.error(logMessage + "Got search result but no movie. Layout has changed?");
            }
        } else {
            LOG.debug(logMessage + "No movie found.");
        }
        return trailersLandId;

    }

    protected String getTrailersLandId(Movie movie) {
        String title = movie.getTitle();
        String origTitle = movie.getOriginalTitle();
        String trailersLandId = getTrailersLandIdFromTitle(title);

        if (StringTools.isNotValidString(trailersLandId) && StringTools.isValidString(origTitle) && !title.equalsIgnoreCase(origTitle)) {
            trailersLandId = getTrailersLandIdFromTitle(origTitle);
        }

        return trailersLandId;
    }

    protected List<TrailersLandTrailer> getTrailerUrls(Movie movie) {

        List<TrailersLandTrailer> trailerList = new ArrayList<TrailersLandTrailer>();

        String trailersLandId = movie.getId(getName());
        if (StringTools.isNotValidString(trailersLandId)) {
            trailersLandId = getTrailersLandId(movie);
            if (StringTools.isNotValidString(trailersLandId)) {
                return trailerList;
            } else {
                movie.setId(getName(), trailersLandId);
            }
        }

        String xml;
        try {
            xml = webBrowser.request(TL_BASE_URL + TL_MOVIE_URL + trailersLandId);
        } catch (IOException error) {
            LOG.error(logMessage + "Failed retreiving movie details for movie : " + movie.getTitle());
            LOG.error(SystemTools.getStackTrace(error));
            return null;
        }

        int indexVideo = xml.indexOf("<div id=\"table_video\"");
        int indexEndVideo = xml.indexOf("</table", indexVideo + 1);

        if (indexVideo >= 0 && indexVideo < indexEndVideo) {
            int nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, indexVideo);
            while (nextIndex >= 0 && nextIndex < indexEndVideo) {
                int endIndex = xml.indexOf('"', nextIndex + 1);
                String trailerPageUrl = xml.substring(nextIndex, endIndex);

                TrailersLandTrailer tr = new TrailersLandTrailer(trailerPageUrl);
                tr.parseName();
                tr.setFoundOrder(nextIndex);

                if (tr.validateLang() && tr.validateType()) {
                    LOG.debug(logMessage + "Found trailer page URL " + trailerPageUrl);
                    trailerList.add(tr);
                    //} else {
                    //    logger.debug(trailersPluginName + " Plugin: discarding page URL " + trailerPageUrl);
                }

                nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, endIndex + 1);
            }
        } else {
            LOG.error(logMessage + "Video section not found. Layout changed?");
        }

        Collections.sort(trailerList);

        int remaining = trailerMaxCount;

        for (int i = trailerList.size() - 1; i >= 0; i--) {

            if (remaining == 0) {
                trailerList.remove(i);
            } else {

                TrailersLandTrailer tr = trailerList.get(i);

                String trailerXml;
                String trailerPageUrl = tr.getPageUrl();

                //logger.debug(trailersPluginName + " Plugin: evaluating page " +  trailerPageUrl);
                try {
                    trailerXml = webBrowser.request(trailerPageUrl);
                } catch (IOException error) {
                    LOG.error(logMessage + "Failed retreiving trailer details for movie : " + movie.getTitle());
                    LOG.error(SystemTools.getStackTrace(error));
                    return null;
                }

                int nextIndex = trailerXml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL);

                if (nextIndex < 0) {
                    LOG.error(logMessage + "No downloadable files found. Layout changed?");
                    trailerList.remove(i);
                } else {
                    boolean found = false;
                    while (nextIndex >= 0) {

                        int endIndex = trailerXml.indexOf('"', nextIndex);
                        String url = trailerXml.substring(nextIndex, endIndex);

                        //logger.debug(trailersPluginName + " Plugin: evaluating url " +  url);
                        if (tr.candidateUrl(url) && !found) {
                            found = true;
                            remaining--;
                            //logger.debug(trailersPluginName + " Plugin: current best url is " + url);
                        }

                        nextIndex = trailerXml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL, endIndex + 1);
                    }
                    if (!found) {
                        trailerList.remove(i);
                        LOG.debug(logMessage + "No valid url found at trailer page " + trailerPageUrl);
                    }
                }
            }
        }
        return trailerList;
    }

    public class TrailersLandTrailer implements Comparable<TrailersLandTrailer> {

        private String pageUrl;
        private String url;
        private String res;
        private String type;
        private String lang;
        private int foundOrder = 0;

        public TrailersLandTrailer(String pageUrl) {
            this.pageUrl = pageUrl;
            this.lang = Movie.UNKNOWN;
            this.res = Movie.UNKNOWN;
            this.type = Movie.UNKNOWN;
            this.url = Movie.UNKNOWN;
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getRes() {
            return res;
        }

        public void setRes(String res) {
            this.res = res;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public int getFoundOrder() {
            return foundOrder;
        }

        public void setFoundOrder(int foundOrder) {
            this.foundOrder = foundOrder;
        }

        public void parseName() {
            String trailerPageUrl = getPageUrl();
            int nameIndex = TL_BASE_URL.length() + TL_TRAILER_URL.length();

            if (trailerPageUrl.indexOf("teaser", nameIndex) >= 0 || trailerPageUrl.indexOf("tesaer", nameIndex) >= 0) { // Some typo are present...
                setType("teaser");
            } else if (trailerPageUrl.indexOf("trailer", nameIndex) >= 0) {
                setType("trailer");
            }

            if (trailerPageUrl.indexOf("sottotitolato", nameIndex) >= 0) {
                setLang("sub-ita");
            } else if (trailerPageUrl.indexOf("italiano", nameIndex) >= 0) {
                setLang("ita");
            } else if (trailerPageUrl.indexOf("francese", nameIndex) >= 0) {
                setLang("fr");
            } else {
                setLang("en");
            }
        }

        private boolean isResValid(String res) {

            if (res.equals(RESOLUTION_SD)) {
                return true;
            }

            if (res.equals(RESOLUTION_720P) && (trailerMaxResolution.equals(RESOLUTION_1080P) || trailerMaxResolution.equals(RESOLUTION_720P))) {
                return true;
            }

            if (res.equals(RESOLUTION_1080P) && trailerMaxResolution.equals(RESOLUTION_1080P)) {
                return true;
            }

            return false;

        }

        private boolean isResBetter(String res) {

            String thisRes = getRes();
            if (StringTools.isNotValidString(res)) {
                return false;
            }
            if (StringTools.isNotValidString(getRes())) {
                return true;
            }

            if (thisRes.equals(RESOLUTION_1080P)) {
                return false;
            }
            if (thisRes.equals(RESOLUTION_720P) && res.equals(RESOLUTION_1080P)) {
                return true;
            }
            if (thisRes.equals(RESOLUTION_SD) && (res.equals(RESOLUTION_1080P) || res.equals(RESOLUTION_720P))) {
                return true;
            }

            return false;
        }

        public boolean candidateUrl(String url) {

            int startIndex = url.indexOf("url=");

            if (startIndex >= 0) {

                String fileUrl = url.substring(startIndex + 4);

                //logger.debug(trailersPluginName + " Plugin: evaulating candidate URL " + fileUrl);
                String ext = fileUrl.substring(fileUrl.lastIndexOf('.') + 1);
                if (this.evaluateAgainstList(ext, trailerAllowedFormats) < 0) {
                    //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " due to invalid extension.");
                    return false;
                }

                String params = url.substring(0, startIndex - 1);

                String resolution;
                if (params.indexOf("sd_file") >= 0) {
                    resolution = RESOLUTION_SD;
                } else if (params.indexOf("480") >= 0) {
                    resolution = RESOLUTION_SD;
                } else if (params.indexOf("720") >= 0) {
                    resolution = RESOLUTION_720P;
                } else if (params.indexOf("1080") >= 0) {
                    resolution = RESOLUTION_1080P;
                } else {
                    LOG.error(logMessage + "Cannot guess trailer resolution for params " + params + ". Layout changed?");
                    return false;
                }

                //logger.debug(trailersPluginName + " Plugin: resolution is " + resolution);
                if (!isResValid(resolution)) {
                    //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " due to resolution.");
                    return false;
                } else {
                    if (!this.isResBetter(resolution)) {
                        //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " as it's not better than actual resolution.");
                        return false;
                    }
                }

                setUrl(fileUrl);
                setRes(resolution);

                return true;
            } else {
                LOG.error(logMessage + "Couldn't find trailer url. Layout changed?");
                return false;
            }
        }

        private int evaluateAgainstList(String what, String list) {
            if (list.indexOf(',') < 0) {
                return what.equalsIgnoreCase(list) ? 1 : -1;
            } else {
                StringTokenizer st = new StringTokenizer(list, ",");
                int w = 1;
                while (st.hasMoreTokens()) {
                    if (what.equalsIgnoreCase(st.nextToken())) {
                        return w;
                    }
                    w++;
                }
                return -1;
            }
        }

        public boolean validateLang() {
            return evaluateAgainstList(getLang(), trailerPreferredLanguages) > 0;
        }

        public boolean validateType() {
            return evaluateAgainstList(getType(), trailerPreferredTypes) > 0;
        }

        @Override
        public int compareTo(TrailersLandTrailer o) {
            String thisLang = this.getLang();
            String thisType = this.getType();
            String thatLang = o.getLang();
            String thatType = o.getType();
            int thisFoundOrder = this.getFoundOrder();
            int thatFoundOder = o.getFoundOrder();

            int diff = evaluateAgainstList(thatLang, trailerPreferredLanguages) - evaluateAgainstList(thisLang, trailerPreferredLanguages);

            if (diff == 0) {
                diff = evaluateAgainstList(thatType, trailerPreferredTypes) - evaluateAgainstList(thisType, trailerPreferredTypes);
                if (diff == 0) {
                    diff = thatFoundOder - thisFoundOrder;
                }
            }
            return diff;

        }
    }
}
