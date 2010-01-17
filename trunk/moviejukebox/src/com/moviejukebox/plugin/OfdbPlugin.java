/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 *
 * @author Durin
 */
public class OfdbPlugin implements MovieDatabasePlugin {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    public static String OFDB_PLUGIN_ID = "ofdb";
    boolean getplot;
    boolean gettitle;
    private int preferredPlotLength;
    
    com.moviejukebox.plugin.ImdbPlugin imdbp;

    public OfdbPlugin() {
        // TODO Auto-generated method stub
        imdbp = new com.moviejukebox.plugin.ImdbPlugin();

        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
        
        getplot = Boolean.parseBoolean(PropertiesUtil.getProperty("ofdb.getplot", "true"));
        gettitle = Boolean.parseBoolean(PropertiesUtil.getProperty("ofdb.gettitle", "true"));
    }

    @Override
    public boolean scan(Movie mediaFile) {

        imdbp.scan(mediaFile);

        if (mediaFile.getId(OFDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
            getOfdbId(mediaFile);
        }

        this.updateOfdbMediaInfo(mediaFile);
        return true;
    }

    public void getOfdbId(Movie mediaFile){
        if (mediaFile.getId(OFDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
            if (!mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(OFDB_PLUGIN_ID, getOfdbIdFromOfdb(mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID)));
            }
            else {
                mediaFile.setId(OFDB_PLUGIN_ID,getofdbIDfromGoogle(mediaFile.getTitle(), mediaFile.getYear()));
            }
        }
    }

    public String getOfdbIdFromOfdb(String imdbId) {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            // URL of CGI-Bin script.
            url = new URL("http://www.ofdb.de/view.php?page=suchergebnis");
            // URL connection channel.
            urlConn = url.openConnection();
            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput(true);
            // Let the RTS know that we want to do output.
            urlConn.setDoOutput(true);
            // No caching, we want the real thing.
            urlConn.setUseCaches(false);
            // Specify the content type.
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // Send POST output.
            printout = new DataOutputStream(urlConn.getOutputStream());
            String content = "&SText=" + imdbId + "&Kat=IMDb";
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            // Get response data.
            StringWriter site = new StringWriter();
            BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
            String line;
            while ((line = input.readLine()) != null) {
                site.write(line);
            }
            input.close();
            String xml = site.toString();
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
        } catch (Exception error) {
            logger.severe("Failed retreiving ofdb URL for movie : ");
            logger.severe("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private String getofdbIDfromGoogle(String movieName, String year) {
        try {
            String ofdbID = Movie.UNKNOWN;
            StringBuffer sb = new StringBuffer("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Awww.ofdb.de/film");

            String xml = request(new URL(sb.toString()));
            int beginIndex = xml.indexOf("http://www.ofdb.de/film");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                ofdbID = st.nextToken();
            } else {
                ofdbID = Movie.UNKNOWN;
            }
            return ofdbID;

        } catch (Exception error) {
            logger.severe("Failed retreiving ofdb Id for movie : " + movieName);
            logger.severe("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan OFDB html page for the specified movie
     */
    private void updateOfdbMediaInfo(Movie movie) {
        try {
            if(movie.getId(OFDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)){
                return;
            }
            String xml = request(new URL(movie.getId(OFDB_PLUGIN_ID)));

            if(gettitle){
                movie.setTitleSort(extractTag(xml, "<title>OFDb - ", 0, "("));
                movie.setTitle(movie.getTitleSort());
            }

            if (getplot) {
                //plot url auslesen:
                URL plotURL = new URL("http://www.ofdb.de/plot/" + extractTag(xml, "<a href=\"plot/", 0, "\""));
                String plot = getPlot(plotURL);
                movie.setPlot(plot);
                String outline = plot;
                if (outline.length() > 150) {
                    outline = outline.substring(0, 150) + "... ";
                }
                movie.setOutline(outline);
            }

        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    private static String request(URL url) throws IOException {
        StringWriter content = null;

        try {
            content = new StringWriter();

            BufferedReader in = null;
            try {
                URLConnection cnx = url.openConnection();
                cnx.setRequestProperty("User-Agent",
                        "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
                in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    content.write(line);
                }

            } finally {
                if (in != null) {
                    in.close();
                }
            }
            return content.toString();
        } finally {
            if (content != null) {
                content.close();
            }
        }
    }

    protected String extractTag(String src, String findStr, int skip) {
        return this.extractTag(src, findStr, skip, "><");
    }

    protected String extractTag(String src, String findStr, int skip, String separator) {
        int beginIndex = src.indexOf(findStr);
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

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag,
            String endTag) {
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

    private String getPlot(URL plotURL) {
        String plot;

        try {
            String xml = request(plotURL);

            int firstindex = xml.indexOf("gelesen</b></b><br><br>") + 23;
            int lastindex = xml.indexOf("</font>", firstindex);
            plot = xml.substring(firstindex, lastindex);
            plot = plot.replaceAll("<br />", " ");

            if (plot.length() > preferredPlotLength) {
                plot = plot.substring(0, preferredPlotLength - 3) + "...";
            }

        } catch (Exception error) {
            plot = "None";
        }

        return plot;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.finest("Scanning NFO for Imdb Id");
        int beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$<>");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$<>");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
            } else {
                logger.finer("No Imdb Id found in nfo !");
            }
        }
        beginIndex = nfo.indexOf("http://www.ofdb.de/film/");

        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex), " \n\t\r\f!&é\"'(èçà)=$<>");
            movie.setId(OfdbPlugin.OFDB_PLUGIN_ID, st.nextToken());
            logger.finer("Ofdb Id found in nfo = " + movie.getId(OfdbPlugin.OFDB_PLUGIN_ID));
        } else {
            logger.finer("No Ofdb Id found in nfo !");
        }
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        imdbp.scanTVShowTitles(movie);
    }

}