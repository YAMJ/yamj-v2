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
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.ComingSoonPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.Date;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * base on ComingSoonPlugin
 * @author iuk
 *
 */

public class ComingSoonTrailersPlugin extends TrailerPlugin {

    private static final Logger logger = Logger.getLogger(ComingSoonTrailersPlugin.class);
    private static String COMINGSOON_VIDEO_URL = "Film/Scheda/Video/?";
    private ComingSoonPlugin csPlugin = new ComingSoonPlugin();

    protected String trailerMaxResolution;
    protected String trailerPreferredFormat;
    protected boolean trailerSetExchange;
    protected String trailerLabel;

    public ComingSoonTrailersPlugin() {
        super();
        trailersPluginName = "ComingSoonTrailers";
        logMessage = "ComingSoonTrailersPlugin: ";

        trailerMaxResolution = PropertiesUtil.getProperty("comingsoontrailers.maxResolution", "1080p");
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
            logger.debug(logMessage + "No trailer found");
            return false;
        }

        logger.debug(logMessage + "Found trailer at URL " + trailerUrl);

        MovieFile tmf = new MovieFile();
        tmf.setTitle("TRAILER-" + trailerLabel);

        if (isDownload()) {
            return downloadTrailer(movie, trailerUrl, trailerLabel, tmf);
        } else {
            tmf.setFilename(trailerUrl);
            movie.addExtraFile(new ExtraFile(tmf));
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
            String searchUrl = ComingSoonPlugin.COMINGSOON_BASE_URL + COMINGSOON_VIDEO_URL + ComingSoonPlugin.COMINGSOON_KEY_PARAM + comingSoonId;
            logger.debug(logMessage + "Searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);
            if (xml.indexOf(ComingSoonPlugin.COMINGSOON_SEARCH_URL + ComingSoonPlugin.COMINGSOON_KEY_PARAM + comingSoonId) < 0) {
                // No link to movie page found. We have been redirected to the general video page
                logger.debug(logMessage + "No video found for movie " + movie.getTitle());
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
                logger.debug("No downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("http://");
            if (beginUrl >= 0) {
                trailerUrl = new String(trailerXml.substring(beginUrl, trailerXml.indexOf('\"', beginUrl)));
            } else {
                logger.error(logMessage + "Cannot find trailer URL in XML. Layout changed?");
            }


        } catch (Exception error) {
            logger.error(logMessage + "Failed retreiving trailer for movie: " + movie.getTitle());
            logger.error(SystemTools.getStackTrace(error));
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
            logger.info(logMessage + "Unknown trailer format " + format);
            return Movie.UNKNOWN;
        }

        if (trailerMaxResolution.equalsIgnoreCase("1080p")) {
            indexOfTrailer = xml.indexOf("1080P." + extension);
        }

        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase("720p") || trailerMaxResolution.equalsIgnoreCase("1080p"))) {
            indexOfTrailer = xml.indexOf("720P." + extension);
        }

        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase("480p") || trailerMaxResolution.equalsIgnoreCase("720p") || trailerMaxResolution.equalsIgnoreCase("1080p"))) {
            indexOfTrailer = xml.indexOf("480P." + extension);
        }

        if (indexOfTrailer >= 0 ) {
            int beginUrl = new String(xml.substring(0, indexOfTrailer)).lastIndexOf("http://");
            trailerUrl = new String(xml.substring(beginUrl, xml.indexOf("\"", beginUrl)));
            logger.debug(logMessage + "Found trailer XML URL " + trailerUrl);
        }

        return trailerUrl;
    }
}
