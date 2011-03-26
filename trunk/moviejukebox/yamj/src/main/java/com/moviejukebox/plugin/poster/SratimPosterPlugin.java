/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
import org.apache.log4j.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.plugin.SratimPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class SratimPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;
    private SratimPlugin sratimPlugin;

    public SratimPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
        sratimPlugin = new SratimPlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return sratimPlugin.getSratimUrl(new Movie(), title, year);
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        try {
            String xml = webBrowser.request("http://sratim.co.il/album.php?mid=" + id);
            posterURL = HTMLTools.extractTag(xml, "<a href=\"photos/normal/", 0, "\"");

            if (!Movie.UNKNOWN.equals(posterURL)) {
                posterURL = "http://sratim.co.il/photos/normal/" + posterURL;
            }
        } catch (Exception error) {
            logger.error("sratim: Failed retreiving poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
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

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getName() {
        return SratimPlugin.SRATIM_PLUGIN_ID;
    }

}
