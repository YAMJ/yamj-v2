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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.OverrideFlag;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * @author Durin
 */
public class OfdbPlugin implements MovieDatabasePlugin {

    private static final Logger logger = Logger.getLogger(OfdbPlugin.class);
    public static final String OFDB_PLUGIN_ID = "ofdb";
    private static final String PLOT_MARKER = "<a href=\"plot/";
    private int preferredPlotLength;
    private int preferredOutlineLength;
    private ImdbPlugin imdbp;
    private WebBrowser webBrowser;

    public OfdbPlugin() {
        imdbp = new com.moviejukebox.plugin.ImdbPlugin();

        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");

        webBrowser = new WebBrowser();
    }

    @Override
    public String getPluginID() {
        return OFDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        imdbp.scan(mediaFile); // Grab data from imdb

        if (StringTools.isNotValidString(mediaFile.getId(OFDB_PLUGIN_ID))) {
            getOfdbId(mediaFile);
        }

        if (OverrideTools.checkOneOverwrite(mediaFile, OFDB_PLUGIN_ID, OverrideFlag.TITLE, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
        	return this.updateOfdbMediaInfo(mediaFile);
    	}

    	return Boolean.TRUE;
    }

    public void getOfdbId(Movie mediaFile) {
        if (StringTools.isNotValidString(mediaFile.getId(OFDB_PLUGIN_ID))) {
            if (StringTools.isValidString(mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID))) {
                mediaFile.setId(OFDB_PLUGIN_ID, getOfdbIdFromOfdb(mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID)));
            } else {
                mediaFile.setId(OFDB_PLUGIN_ID, getofdbIDfromGoogle(mediaFile.getTitle(), mediaFile.getYear()));
            }
        }
    }

    public String getOfdbIdFromOfdb(String imdbId) {
        try {
            String xml = webBrowser.request("http://www.ofdb.de/view.php?page=suchergebnis&SText=" + imdbId + "&Kat=IMDb");
            String ofdbID;
            int beginIndex = xml.indexOf("film/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                ofdbID = st.nextToken();
                ofdbID = "http://www.ofdb.de/" + ofdbID;
            } else {
                ofdbID = Movie.UNKNOWN;
            }
            return ofdbID;
        } catch (IOException error) {
            logger.error("Failed retreiving ofdb URL for movie: " + imdbId);
            logger.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
    }

    private String getofdbIDfromGoogle(String movieName, String year) {
        try {
            String ofdbID;
            StringBuilder sb = new StringBuilder("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Awww.ofdb.de/film");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("http://www.ofdb.de/film");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                ofdbID = st.nextToken();
            } else {
                ofdbID = Movie.UNKNOWN;
            }
            return ofdbID;

        } catch (Exception error) {
            logger.error("Failed retreiving ofdb Id for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan OFDB html page for the specified movie
     */
    private boolean updateOfdbMediaInfo(Movie movie) {
        if (StringTools.isNotValidString(movie.getId(OFDB_PLUGIN_ID))) {
            return Boolean.FALSE;
        }

        try {
            String xml = webBrowser.request(movie.getId(OFDB_PLUGIN_ID));

            if (OverrideTools.checkOverwriteTitle(movie, OFDB_PLUGIN_ID)) {
                String titleShort = HTMLTools.extractTag(xml, "<title>OFDb -", "</title>");
                if (titleShort.indexOf("(") > 0) {
                    // strip year from title
                    titleShort = titleShort.substring(0, titleShort.lastIndexOf("(")).trim();
                }
                movie.setTitle(titleShort, OFDB_PLUGIN_ID);
            }


            if (OverrideTools.checkOneOverwrite(movie, OFDB_PLUGIN_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
                if (xml.contains(PLOT_MARKER)) {
                    String plot = getPlot("http://www.ofdb.de/plot/" + HTMLTools.extractTag(xml, PLOT_MARKER, 0, "\""));
                    if (StringTools.isValidString(plot)) {

                        if (OverrideTools.checkOverwritePlot(movie, OFDB_PLUGIN_ID)) {
                        	movie.setPlot(plot, OFDB_PLUGIN_ID);
                        }

                        if (OverrideTools.checkOverwriteOutline(movie, OFDB_PLUGIN_ID)) {
                        	String outline = StringTools.trimToLength(plot, preferredOutlineLength, true, "...");
                        	movie.setOutline(outline, OFDB_PLUGIN_ID);
                        }
                    }
                } else {
                    logger.debug("No plot found for " + movie.getBaseName());
                    return Boolean.FALSE;
                }
            }
        } catch (IOException error) {
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private String getPlot(String plotURL) {
        String plot;

        try {
            String xml = webBrowser.request(plotURL);

            int firstindex = xml.indexOf("gelesen</b></b><br><br>") + 23;
            int lastindex = xml.indexOf("</font>", firstindex);
            plot = xml.substring(firstindex, lastindex);
            plot = plot.replaceAll("<br />", " ");

            plot = StringTools.trimToLength(plot, preferredPlotLength, true, "...");

        } catch (IOException error) {
            logger.warn("Failed to get plot");
            logger.warn(SystemTools.getStackTrace(error));
            plot = Movie.UNKNOWN;
        }

        return plot;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        logger.debug("Scanning NFO for Imdb Id");
        int beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$<>");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            logger.debug("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$<>");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
            } else {
                logger.debug("No Imdb Id found in nfo !");
            }
        }
        boolean result = false;
        beginIndex = nfo.indexOf("http://www.ofdb.de/film/");

        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex), " \n\t\r\f!&é\"'(èçà)=$<>");
            movie.setId(OfdbPlugin.OFDB_PLUGIN_ID, st.nextToken());
            logger.debug("Ofdb Id found in nfo = " + movie.getId(OfdbPlugin.OFDB_PLUGIN_ID));
            result = true;
        } else {
            logger.debug("No Ofdb Id found in nfo !");
        }
        return result;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        imdbp.scanTVShowTitles(movie);
    }

    @Override
    public boolean scan(Person person) {
        return false;
    }
}