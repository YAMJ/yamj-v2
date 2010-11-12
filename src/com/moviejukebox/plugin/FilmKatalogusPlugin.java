/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

/* FilmKatalogus plugin
 * 
 * Contains code for an alternate plugin for fetching information on 
 * movies in Hungarian
 * 
 */

package com.moviejukebox.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Film Katalogus Plugin for Hungarian language
 * @author pbando12@gmail.com
 *
 */
public class FilmKatalogusPlugin extends ImdbPlugin {

    public static String FILMKAT_PLUGIN_ID = "filmkatalogus";
    private static Logger logger = Logger.getLogger("moviejukebox");
    boolean getplot = true;
    boolean gettitle = true;
    protected int maxLength;
    protected int outlineLength;
    protected TheTvDBPlugin tvdb;

    public FilmKatalogusPlugin() {
        super(); // use IMDB as basis
        init();
        tvdb = new TheTvDBPlugin();
    }

    public void init() {
        try {
            String temp = PropertiesUtil.getProperty("plugin.plot.maxlength", "500");
            maxLength = Integer.parseInt(temp);
            if (maxLength < 50) {
                maxLength = 500;
            }
        } catch (NumberFormatException ex) {
            maxLength = 500;
        } 
        
        try {
            String temp = PropertiesUtil.getProperty("filmkatalogus.outline.length", "150");
            outlineLength = Integer.parseInt(temp);
        } catch (NumberFormatException ex) {
            outlineLength = 150;
        }
        
        gettitle = Boolean.parseBoolean(PropertiesUtil.getProperty("filmkatalogus.gettitle", "true"));
        getplot = Boolean.parseBoolean(PropertiesUtil.getProperty("filmkatalogus.getplot", "true"));
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean result;
        // If Plot  is already filled from XML based NFO dont overwrite
        if (!mediaFile.getPlot().equals(Movie.UNKNOWN)) {
            getplot = false;
        }
        
        result = super.scan(mediaFile); // use IMDB as basis
        if (result == false && mediaFile.isTVShow()) {
            result = tvdb.scan(mediaFile);
        }

        if (getplot || gettitle) {
            logger.fine("FilmKatalogusPlugin: Id found in nfo = " + mediaFile.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            getHunPlot(mediaFile);
        }

        return result;
    }

    private String getHunPlot(Movie movie) {
        try {
            //logger.fine("Running getHunPlot");
            
            String filmKatURL;
            
            if (StringTools.isNotValidString(movie.getId(FILMKAT_PLUGIN_ID))) {
                filmKatURL = "http://filmkatalogus.hu/kereses?keres0=1&szo0=";
                filmKatURL = filmKatURL.concat(URLEncoder.encode(movie.getTitle(), "ISO-8859-2"));
            } else {
                filmKatURL = "http://filmkatalogus.hu/f";
                filmKatURL = filmKatURL.concat(movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            }

            String xml = webBrowser.request(filmKatURL);
            //logger.fine(filmKatURL);
            //logger.finest(xml);

            // name
            int beginIndex = xml.indexOf("<H1>");
            if (beginIndex > 0) { // exact match is found
                int endIndex = xml.indexOf("</H1>", beginIndex);
                if (gettitle) {
                    movie.setTitle(xml.substring((beginIndex + 4), endIndex));
                }

                // PLOT
                beginIndex = xml.indexOf("<DIV ALIGN=JUSTIFY>", beginIndex);
                endIndex = xml.indexOf("</DIV>", beginIndex);
                if (getplot) {
                    String plot = Movie.UNKNOWN;
                    plot = xml.substring((beginIndex + 19), endIndex);

                    plot = StringTools.trimToLength(plot, maxLength, true, plotEnding);
                    movie.setPlot(plot);

                    // movie.setPlot(xml.substring((beginIndex + 19), endIndex));
                }
                return null;
            }

            beginIndex = xml.indexOf("Találat(ok) filmek között");
            if (beginIndex != -1) { // more then one entry found use the first one
                beginIndex = xml.indexOf("HREF='/", beginIndex);
                int endIndex = xml.indexOf("TITLE", beginIndex);
                filmKatURL = "http://filmkatalogus.hu";
                filmKatURL = filmKatURL.concat(xml.substring((beginIndex + 6), endIndex - 2));
                xml = webBrowser.request(filmKatURL);
                //logger.fine(filmKatURL);
                //logger.finest(xml);

                // name
                beginIndex = xml.indexOf("<H1>");
                if (beginIndex != -1) {
                    endIndex = xml.indexOf("</H1>", beginIndex);
                    if (gettitle) {
                        movie.setTitle(xml.substring((beginIndex + 4), endIndex));
                    }

                    // PLOT
                    beginIndex = xml.indexOf("<DIV ALIGN=JUSTIFY>", beginIndex);
                    endIndex = xml.indexOf("</DIV>", beginIndex);
                    if (getplot) {
                        String plot = Movie.UNKNOWN;
                        plot = xml.substring((beginIndex + 19), endIndex);

                        plot = StringTools.trimToLength(plot, maxLength, true, plotEnding);
                        movie.setPlot(plot);

                        // movie.setPlot(xml.substring((beginIndex + 19), endIndex));
                    }
                    // if (getplot) movie.setPlot(xml.substring((beginIndex + 19), endIndex));
                }
                return null;
            }

            return null;

        } catch (Exception error) {
            logger.severe("FilmKatalogusPlugin: Failed retreiving information for " + movie.getTitle());
            
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return null;
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB as basis

        int beginIndex = nfo.indexOf("filmkatalogus.hu/");
        if (beginIndex != -1) {
            beginIndex = nfo.indexOf("--f", beginIndex);
            if (beginIndex != -1) {
                StringTokenizer filmKatID = new StringTokenizer(nfo.substring(beginIndex + 3), "/ \n,:!&é\"'(--è_çà)=$<>");
                movie.setId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID, filmKatID.nextToken());
                logger.finest("FilmKatalogusPlugin: Id found in nfo = " + movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            }
        }
    }
}
