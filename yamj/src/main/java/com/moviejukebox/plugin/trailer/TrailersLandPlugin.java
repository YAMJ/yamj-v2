/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.trailer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

/**
 * @author iuk
 */
public class TrailersLandPlugin extends TrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(TrailersLandPlugin.class);
    private static final String TL_BASE_URL = "http://www.trailersland.com/";
    private static final String TL_SEARCH_URL = "cerca?ricerca=";
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

        trailerMaxCount = PropertiesUtil.getIntProperty("trailersland.max", 3);
        trailerMaxResolution = PropertiesUtil.getProperty("trailersland.maxResolution", RESOLUTION_1080P);
        trailerAllowedFormats = PropertiesUtil.getProperty("trailersland.allowedFormats", "wmv,mov,mp4,avi,mkv,mpg");
        trailerPreferredLanguages = PropertiesUtil.getProperty("trailersland.preferredLanguages", "ita,sub-ita,en");
        trailerPreferredTypes = PropertiesUtil.getProperty("trailersland.preferredTypes", "trailer,teaser");
    }

    @Override
    public final boolean generate(Movie movie) {
        if (trailerMaxResolution.length() == 0) {
            LOG.trace("No resolution provided, skipping");
            return false;
        }

        // Set the last scan to now
        movie.setTrailerLastScan(new Date().getTime());

        List<TrailersLandTrailer> trailerList = getTrailerUrls(movie);

        if (trailerList == null) {
            LOG.error("Error while scraping");
            return false;
        } else if (trailerList.isEmpty()) {
            LOG.debug("No trailer found");
            return false;
        } else {
            LOG.debug("Found {} trailers", trailerList.size());
        }

        for (int i = trailerList.size() - 1; i >= 0; i--) {
            TrailersLandTrailer tr = trailerList.get(i);

            String trailerUrl = tr.getUrl();
            LOG.info("Found trailer at URL {}", trailerUrl);

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
        } catch (UnsupportedEncodingException ex) {
            LOG.error("Unsupported encoding, cannot build search URL", ex);
            return Movie.UNKNOWN;
        }

        LOG.debug("Searching for movie at URL {}", searchUrl);

        String xml;
        try {
            xml = httpClient.request(searchUrl);
        } catch (IOException error) {
            LOG.error("Failed retreiving TrailersLand Id for movie: {}", title);
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }

        int indexRes = xml.indexOf("<span class=\"info\"");
        if (indexRes >= 0) {
            int indexMovieUrl = xml.indexOf(TL_BASE_URL + TL_MOVIE_URL, indexRes + 1);
            if (indexMovieUrl >= 0) {
                int endMovieUrl = xml.indexOf('"', indexMovieUrl + 1);
                if (endMovieUrl >= 0) {
                    trailersLandId = xml.substring(indexMovieUrl + TL_BASE_URL.length() + TL_MOVIE_URL.length(), endMovieUrl);
                    LOG.debug("Found Trailers Land Id '{}'", trailersLandId);
                }
            } else {
                LOG.error("Got search result but no movie. Layout has changed?");
            }
        } else {
            LOG.debug("No movie found with title '{}'.", title);
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

    /**
     * Scrape the web page for trailer URLs
     *
     * @param movie
     * @return
     */
    protected List<TrailersLandTrailer> getTrailerUrls(Movie movie) {
        List<TrailersLandTrailer> trailerList = new ArrayList<>();

        String trailersLandId = movie.getId(getName());
        if (StringTools.isNotValidString(trailersLandId)) {
            trailersLandId = getTrailersLandId(movie);
            if (StringTools.isNotValidString(trailersLandId)) {
                LOG.debug("No ID found for movie {}", movie.getBaseName());
                return trailerList;
            }
            movie.setId(getName(), trailersLandId);
        }

        String xml;
        try {
            xml = httpClient.request(TL_BASE_URL + TL_MOVIE_URL + trailersLandId);
        } catch (IOException error) {
            LOG.error("Failed retreiving movie details for movie: {}", movie.getTitle());
            LOG.error(SystemTools.getStackTrace(error));
            return trailerList;
        }

        int indexVideo = xml.indexOf("<div class=\"trailer_container\">");
        int indexEndVideo = xml.indexOf("<div id=\"sidebar\">", indexVideo + 1);

        if (indexVideo >= 0 && indexVideo < indexEndVideo) {
            int nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, indexVideo);
            while (nextIndex >= 0 && nextIndex < indexEndVideo) {
                int endIndex = xml.indexOf('"', nextIndex + 1);
                String trailerPageUrl = xml.substring(nextIndex, endIndex);

                TrailersLandTrailer tr = new TrailersLandTrailer(trailerPageUrl);
                tr.parseName();
                tr.setFoundOrder(nextIndex);

                if (tr.validateLang() && tr.validateType()) {
                    if (trailerList.contains(tr)) {
                        LOG.debug("Duplicate trailer page (Ignoring) - URL {}", trailerPageUrl);
                    } else {
                        LOG.debug("Found trailer page - URL {}", trailerPageUrl);
                        trailerList.add(tr);
                    }
                } else {
                    LOG.trace("Discarding page - URL {}", trailerPageUrl);
                }

                nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, endIndex + 1);
            }
        } else {
            LOG.error("Video section not found. Layout changed?");
        }

        Collections.sort(trailerList);
        LOG.debug("Found {} trailers, maximum required is {}", trailerList.size(), trailerMaxCount);

        int remaining = trailerMaxCount;

        for (int i = trailerList.size() - 1; i >= 0; i--) {
            if (remaining == 0) {
                LOG.trace("Discarding trailer (not required): {}", trailerList.get(i));
                trailerList.remove(i);
            } else {
                TrailersLandTrailer tr = trailerList.get(i);

                String trailerXml;
                String trailerPageUrl = tr.getPageUrl();

                LOG.trace("Evaluating page {}", trailerPageUrl);
                try {
                    trailerXml = httpClient.request(trailerPageUrl);
                } catch (IOException error) {
                    LOG.error("Failed retreiving trailer details for movie: {}", movie.getTitle());
                    LOG.error(SystemTools.getStackTrace(error));
                    return null;
                }

                int nextIndex = trailerXml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL);

                if (nextIndex < 0) {
                    LOG.error("No downloadable files found. Layout changed?");
                    trailerList.remove(i);
                } else {
                    boolean found = false;
                    while (nextIndex >= 0) {

                        int endIndex = trailerXml.indexOf('"', nextIndex);
                        String url = trailerXml.substring(nextIndex, endIndex);

                        LOG.trace("Evaluating url {}", url);
                        if (tr.candidateUrl(url) && !found) {
                            found = true;
                            remaining--;
                            LOG.trace("Current best url is {}", url);
                        }

                        nextIndex = trailerXml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL, endIndex + 1);
                    }
                    if (!found) {
                        trailerList.remove(i);
                        LOG.debug("No valid url found at trailer page {}", trailerPageUrl);
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

            // Some typo are present...
            if (trailerPageUrl.indexOf("teaser", nameIndex) >= 0 || trailerPageUrl.indexOf("tesaer", nameIndex) >= 0) {
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

            return res.equals(RESOLUTION_1080P) && trailerMaxResolution.equals(RESOLUTION_1080P);

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
            return thisRes.equals(RESOLUTION_SD) && (res.equals(RESOLUTION_1080P) || res.equals(RESOLUTION_720P));
        }

        public boolean candidateUrl(String url) {

            int startIndex = url.indexOf("url=");

            if (startIndex >= 0) {

                String fileUrl = url.substring(startIndex + 4);

                LOG.trace("Evaulating candidate URL {}", fileUrl);
                String ext = fileUrl.substring(fileUrl.lastIndexOf('.') + 1);
                if (this.evaluateAgainstList(ext, trailerAllowedFormats) < 0) {
                    LOG.trace("Discarding '{}' due to invalid extension.", fileUrl);
                    return false;
                }

                String params = url.substring(0, startIndex - 1);

                String resolution;
                if (params.contains("sd_file")) {
                    resolution = RESOLUTION_SD;
                } else if (params.contains("480")) {
                    resolution = RESOLUTION_SD;
                } else if (params.contains("720")) {
                    resolution = RESOLUTION_720P;
                } else if (params.contains("1080")) {
                    resolution = RESOLUTION_1080P;
                } else {
                    LOG.error("Cannot guess trailer resolution for params '{}'. Layout changed?", params);
                    return false;
                }

                LOG.trace("Resolution is {}", resolution);
                if (!isResValid(resolution)) {
                    LOG.trace("Discarding '{}' due to resolution.", fileUrl);
                    return false;
                }
                if (!this.isResBetter(resolution)) {
                    LOG.trace("Discarding '{}' as it's not better than actual resolution.", fileUrl);
                    return false;
                }

                setUrl(fileUrl);
                setRes(resolution);

                return true;
            }
            
            LOG.error("Couldn't find trailer url. Layout changed?");
            return false;
        }

        private int evaluateAgainstList(String what, String list) {
            if (list.indexOf(',') < 0) {
                return what.equalsIgnoreCase(list) ? 1 : -1;
            }

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

        public boolean validateLang() {
            return evaluateAgainstList(getLang(), trailerPreferredLanguages) > 0;
        }

        public boolean validateType() {
            return evaluateAgainstList(getType(), trailerPreferredTypes) > 0;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof TrailersLandTrailer) {
                final TrailersLandTrailer other = (TrailersLandTrailer) obj;
                return new EqualsBuilder()
                        .append(res, other.res)
                        .append(type, other.type)
                        .append(lang, other.lang)
                        .isEquals();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(res)
                    .append(type)
                    .append(lang)
                    .toHashCode();
        }

        @Override
        public int compareTo(TrailersLandTrailer o) {
            int diff = evaluateAgainstList(o.getLang(), trailerPreferredLanguages) - evaluateAgainstList(this.getLang(), trailerPreferredLanguages);

            if (diff == 0) {
                diff = evaluateAgainstList(o.getType(), trailerPreferredTypes) - evaluateAgainstList(this.getType(), trailerPreferredTypes);
                if (diff == 0) {
                    diff = o.getFoundOrder() - this.getFoundOrder();
                }
            }
            return diff;

        }
    }
}
