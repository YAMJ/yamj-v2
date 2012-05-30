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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * @author Durin
 */
public class OfdbPlugin implements MovieDatabasePlugin {

    private static final Logger logger = Logger.getLogger(OfdbPlugin.class);
    public static String OFDB_PLUGIN_ID = "ofdb";
    private static final String PLOT_MARKER = "<a href=\"plot/";
    private boolean getplot;
    private boolean gettitle;
    private int preferredPlotLength;
    private int preferredOutlineLength;
    private ImdbPlugin imdbp;
    private WebBrowser webBrowser;

    public OfdbPlugin() {
        imdbp = new com.moviejukebox.plugin.ImdbPlugin();

        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");

        getplot = PropertiesUtil.getBooleanProperty("ofdb.getplot", "true");
        gettitle = PropertiesUtil.getBooleanProperty("ofdb.gettitle", "true");

        webBrowser = new WebBrowser();
    }

    @Override
    public String getPluginID() {
        return OFDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean plotBeforeImdb = !Movie.UNKNOWN.equalsIgnoreCase(mediaFile.getPlot()); // Issue 797 - we don't want to override plot from NFO
        imdbp.scan(mediaFile); // Grab data from imdb

        if (StringTools.isNotValidString(mediaFile.getId(OFDB_PLUGIN_ID))) {
            getOfdbId(mediaFile);
        }

        return this.updateOfdbMediaInfo(mediaFile, plotBeforeImdb);
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
     *
     * @param plotBeforeImdb
     */
    private boolean updateOfdbMediaInfo(Movie movie, boolean plotBeforeImdb) {
        try {
            if (StringTools.isNotValidString(movie.getId(OFDB_PLUGIN_ID))) {
                return false;
            }
            String xml = webBrowser.request(movie.getId(OFDB_PLUGIN_ID));

            if (gettitle) {
                String titleShort = extractTag(xml, "<title>OFDb - ", 0, "(");
                if (StringTools.isValidString(titleShort) && !movie.isOverrideTitle()) {
                    movie.setTitle(titleShort);
                }
            }

            if (getplot) {
                // plot url auslesen:
                if (xml.contains(PLOT_MARKER)) {
                    String plot = getPlot("http://www.ofdb.de/plot/" + extractTag(xml, PLOT_MARKER, 0, "\""));

                    // Issue 797, preserve Plot from NFO
                    // Did we get some translated plot and didn't have previous plotFromNfo ?
                    if (!Movie.UNKNOWN.equalsIgnoreCase(plot) && !plotBeforeImdb) {
                        movie.setPlot(plot);

                        String outline = StringTools.trimToLength(plot, preferredOutlineLength, true, "...");
                        movie.setOutline(outline);
                    }
                } else {
                    logger.debug("No plot found for " + movie.getBaseName());
                    return false;
                }
            }

        } catch (IOException error) {
            logger.error(SystemTools.getStackTrace(error));
            return false;
        }
        return true;
    }

    protected String extractTag(String src, String findStr, int skip) {
        return this.extractTag(src, findStr, skip, "><");
    }

    protected String extractTag(String src, String findStr, int skip, String separator) {
        int beginIndex = src.indexOf(findStr);
        if (beginIndex < 0) {
            return Movie.UNKNOWN;
        }

        StringTokenizer st = new StringTokenizer(src.substring(beginIndex + findStr.length()), separator);
        for (int i = 0; i < skip; i++) {
            st.nextToken();
        }

        String value = HTMLTools.decodeHtml(st.nextToken().trim());
        if (value.indexOf("uiv=\"content-ty") != -1 || value.indexOf("cast") != -1 || value.indexOf("title") != -1 || value.indexOf("<") != -1) {
            value = Movie.UNKNOWN;
        }

        return value;
    }

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd) {
        return extractTags(src, sectionStart, sectionEnd, null, "|");
    }

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
        ArrayList<String> tags = new ArrayList<String>();
        int index = src.indexOf(sectionStart);
        if (index == -1) {
            return tags;
        }
        index += sectionStart.length();
        int endIndex = src.indexOf(sectionEnd, index);
        if (endIndex == -1) {
            return tags;
        }

        String sectionText = src.substring(index, endIndex);
        int lastIndex = sectionText.length();
        index = 0;
        int startLen = 0;
        int endLen = endTag.length();

        if (startTag != null) {
            index = sectionText.indexOf(startTag);
            startLen = startTag.length();
        }

        while (index != -1) {
            index += startLen;
            int close = sectionText.indexOf('>', index);
            if (close != -1) {
                index = close + 1;
            }
            endIndex = sectionText.indexOf(endTag, index);
            if (endIndex == -1) {
                endIndex = lastIndex;
            }
            String text = sectionText.substring(index, endIndex);

            tags.add(HTMLTools.decodeHtml(text.trim()));
            endIndex += endLen;
            if (endIndex > lastIndex) {
                break;
            }
            if (startTag != null) {
                index = sectionText.indexOf(startTag, endIndex);
            } else {
                index = endIndex;
            }
        }
        return tags;
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