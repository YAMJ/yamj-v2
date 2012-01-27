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
package com.moviejukebox.plugin.poster;

import java.text.ParseException;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import org.apache.log4j.Logger;

public class PassionXbmcPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;
    private AllocinePlugin allocinePlugin;
    private static Logger logger = Logger.getLogger(PassionXbmcPosterPlugin.class);

    public PassionXbmcPosterPlugin() {
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
            logger.error("PassionXbmcPlugin: Failed retrieving poster id movie : " + title);
            logger.error(SystemTools.getStackTrace(error));
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            String xml = "";
            try {
                xml = webBrowser.request("http://passion-xbmc.org/scraper/index2.php?Page=ViewMovie&ID=" + id);
                String baseUrl = "http://passion-xbmc.org/scraper/Gallery/main/";
                String posterImg = HTMLTools.extractTag(xml, "href=\"" + baseUrl, "\"");
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterImg)) {
                    // logger.fine("PassionXbmcPlugin: posterImg : " + posterImg);
                    posterURL = baseUrl + posterImg;
                }
            } catch (Exception error) {
                logger.error("PassionXbmcPlugin: Failed retrieving poster for movie : " + id);
                logger.error(SystemTools.getStackTrace(error));
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
        return "passionxbmc";
    }
}
