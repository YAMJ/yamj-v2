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

package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

public class FilmwebPosterPlugin extends AbstractMoviePosterPlugin implements IMoviePosterPlugin, ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public FilmwebPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
        try {
            // first request to filmweb site to skip welcome screen with ad banner
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            logger.severe("Error : " + error.getMessage());
        }
    }

    public String getIdFromMovieInfo(String title, String year) {
        FilmwebPlugin filmWebPlugin = new FilmwebPlugin();
        return filmWebPlugin.getFilmwebUrl(title, year);
    }

    public String getPosterUrl(String id) {
        String response = Movie.UNKNOWN;
        String xml;
        try {
            xml = webBrowser.request(id);
            response = HTMLTools.extractTag(xml, "posterLightbox", 3, "\"");
        } catch (Exception error) {
            logger.severe("Failed retreiving filmweb poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return response;
    }

    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    public String getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    public String getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    public String getName() {
        return FilmwebPlugin.FILMWEB_PLUGIN_ID;
    }
}
