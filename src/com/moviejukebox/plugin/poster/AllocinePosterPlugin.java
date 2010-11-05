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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class AllocinePosterPlugin extends AbstractMoviePosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;
    private AllocinePlugin allocinePlugin;

    public AllocinePosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
        allocinePlugin = new AllocinePlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            response = allocinePlugin.getAllocineId(title, year, -1);
        } catch (ParseException error) {
            logger.severe("AllocinePosterPlugin: Failed retreiving poster id movie : " + title);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            String xml = "";
            try {
                xml = webBrowser.request("http://www.allocine.fr/film/fichefilm-" + id + "/affiches/");
                String posterMediaId = HTMLTools.extractTag(xml, "<a href=\"/film/fichefilm-" + id + "/affiches/detail/?cmediafile=", "\" ><img");
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterMediaId)) {
                    String mediaFileURL = "http://www.allocine.fr/film/fichefilm-" + id + "/affiches/detail/?cmediafile=" + posterMediaId;
                    logger.finest("AllocinePlugin: mediaFileURL : " + mediaFileURL);
                    xml = webBrowser.request(mediaFileURL);

                    String posterURLTag = HTMLTools.extractTag(xml, "<div class=\"tac\" style=\"\">", "</div>");
                    // logger.finest("AllocinePlugin: posterURLTag : " + posterURLTag);
                    posterURL = HTMLTools.extractTag(posterURLTag, "<img src=\"", "\"");

                    if (!posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
                        logger.finest("AllocinePlugin: Movie PosterURL from Allocine: " + posterURL);
                    }
                }
            } catch (Exception error) {
                logger.severe("AllocinePlugin: Failed retreiving poster for movie : " + id);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "allocine";
    }
}
