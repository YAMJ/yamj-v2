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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ComingSoonPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.Date;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base on ComingSoonPlugin
 * @author iuk
 */
public class ComingSoonTrailersPlugin extends TrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ComingSoonTrailersPlugin.class);
    private static String csVideoUrl = "Film/Scheda/Video/?";
    private ComingSoonPlugin csPlugin = new ComingSoonPlugin();

    private String trailerMaxResolution;
    private String trailerPreferredFormat;
    private String trailerLabel;

    public ComingSoonTrailersPlugin() {
        super();
        trailersPluginName = "ComingSoonTrailers";
        logMessage = "ComingSoonTrailersPlugin: ";

        trailerMaxResolution = PropertiesUtil.getProperty("comingsoontrailers.maxResolution", RESOLUTION_1080P);
        trailerPreferredFormat = PropertiesUtil.getProperty("comingsoontrailers.preferredFormat", "wmv,mov");
        trailerLabel = PropertiesUtil.getProperty("comingsoontrailers.label", "ita");
    }

    @Override
    public final boolean generate(Movie movie) {
        if (trailerMaxResolution.length() == 0) {
            return false;
        }

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        String trailerUrl = getTrailerUrl(movie);

        if (StringTools.isNotValidString(trailerUrl)) {
            LOG.debug(logMessage + "No trailer found");
            return false;
        }

        LOG.debug(logMessage + "Found trailer at URL " + trailerUrl);

        ExtraFile extra = new ExtraFile();
        extra.setTitle("TRAILER-" + trailerLabel);

        if (isDownload()) {
            return downloadTrailer(movie, trailerUrl, trailerLabel, extra);
        } else {
            extra.setFilename(trailerUrl);
            movie.addExtraFile(extra);
            return true;
        }
    }

    @Override
    public String getName() {
        return "comingsoon";
    }

    protected String getTrailerUrl(Movie movie) {
        String trailerUrl = Movie.UNKNOWN;
        String comingSoonId = movie.getId(ComingSoonPlugin.COMINGSOON_PLUGIN_ID);
        if (StringTools.isNotValidString(comingSoonId)) {
            comingSoonId = csPlugin.getComingSoonId(movie.getOriginalTitle(), movie.getYear());
            if (StringTools.isNotValidString(comingSoonId)) {
                return Movie.UNKNOWN;
            }
        }

        try {
            String searchUrl = ComingSoonPlugin.COMINGSOON_BASE_URL + csVideoUrl + ComingSoonPlugin.COMINGSOON_KEY_PARAM + comingSoonId;
            LOG.debug(logMessage + "Searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);
            if (xml.indexOf(ComingSoonPlugin.COMINGSOON_SEARCH_URL + ComingSoonPlugin.COMINGSOON_KEY_PARAM + comingSoonId) < 0) {
                // No link to movie page found. We have been redirected to the general video page
                LOG.debug(logMessage + "No video found for movie " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String xmlUrl = Movie.UNKNOWN;

            if (trailerPreferredFormat.indexOf(',') < 0) {
                xmlUrl = parseVideoPage(xml, trailerPreferredFormat);
            } else {
                StringTokenizer st = new StringTokenizer(trailerPreferredFormat, ",");
                while (st.hasMoreTokens()) {
                    xmlUrl = parseVideoPage(xml, st.nextToken());
                    if (StringTools.isValidString(xmlUrl)) {
                        break;
                    }
                }
            }

            if (StringTools.isNotValidString(xmlUrl)) {
                LOG.debug("No downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("http://");
            if (beginUrl >= 0) {
                trailerUrl = trailerXml.substring(beginUrl, trailerXml.indexOf('\"', beginUrl));
            } else {
                LOG.error(logMessage + "Cannot find trailer URL in XML. Layout changed?");
            }


        } catch (Exception error) {
            LOG.error(logMessage + "Failed retreiving trailer for movie: " + movie.getTitle());
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }

        return trailerUrl;
    }

    private String parseVideoPage(String xml, String format) {
        String trailerUrl = Movie.UNKNOWN;

        int indexOfTrailer = -1;
        String extension;

        if (format.equalsIgnoreCase("mov")) {
            extension = "qtl";
        } else if (format.equalsIgnoreCase("wmv")) {
            extension = "wvx";
        } else {
            LOG.info(logMessage + "Unknown trailer format " + format);
            return Movie.UNKNOWN;
        }

        if (trailerMaxResolution.equalsIgnoreCase(RESOLUTION_1080P)) {
            indexOfTrailer = xml.indexOf("1080P." + extension);
        }

        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase(RESOLUTION_720P) || trailerMaxResolution.equalsIgnoreCase(RESOLUTION_1080P))) {
            indexOfTrailer = xml.indexOf("720P." + extension);
        }

        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase("480p") || trailerMaxResolution.equalsIgnoreCase(RESOLUTION_720P) || trailerMaxResolution.equalsIgnoreCase(RESOLUTION_1080P))) {
            indexOfTrailer = xml.indexOf("480P." + extension);
        }

        if (indexOfTrailer >= 0 ) {
            int beginUrl = xml.substring(0, indexOfTrailer).lastIndexOf("http://");
            trailerUrl = xml.substring(beginUrl, xml.indexOf("\"", beginUrl));
            LOG.debug(logMessage + "Found trailer XML URL " + trailerUrl);
        }

        return trailerUrl;
    }
}
