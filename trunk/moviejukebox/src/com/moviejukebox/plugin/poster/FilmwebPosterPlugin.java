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

package com.moviejukebox.plugin.poster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.WebBrowser;

public class FilmwebPosterPlugin implements IMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Pattern posterUrlPattern = Pattern.compile("artshow[^>]+(http://gfx.filmweb.pl[^\"]+)\"");

    private WebBrowser webBrowser;

    public FilmwebPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        FilmwebPlugin filmWebPlugin = new FilmwebPlugin();
        response = filmWebPlugin.getFilmwebUrl(title, year);
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        String response = Movie.UNKNOWN;
        String xml = "";
        try {
            xml = webBrowser.request(id);
            Matcher m = posterUrlPattern.matcher(xml);
            if (m.find()) {
                response = m.group(1);
            }
        } catch (Exception error) {
            logger.severe("Failed retreiving filmweb poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "filmweb";
    }

}
