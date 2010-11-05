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
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

public class FilmwebPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;
    private FilmwebPlugin filmwebPlugin;

    public FilmwebPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        try {
            webBrowser = new WebBrowser();
            filmwebPlugin = new FilmwebPlugin();
            // first request to filmweb site to skip welcome screen with ad banner
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            logger.severe("Error : " + error.getMessage());
        }
    }

    public String getIdFromMovieInfo(String title, String year) {
        return filmwebPlugin.getFilmwebUrl(title, year);
    }

    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml;
        try {
            xml = webBrowser.request(id);
            posterURL = HTMLTools.extractTag(xml, "posterLightbox", 3, "\"");
        } catch (Exception error) {
            logger.severe("Failed retreiving filmweb poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    public String getName() {
        return FilmwebPlugin.FILMWEB_PLUGIN_ID;
    }
}
