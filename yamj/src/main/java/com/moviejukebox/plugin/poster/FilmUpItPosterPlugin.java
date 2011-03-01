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
import java.net.URLEncoder;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.StringTools;

public class FilmUpItPosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public FilmUpItPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://filmup.leonardo.it/cgi-bin/search.cgi?ps=10&fmt=long&q=");
            sb.append(URLEncoder.encode(title.replace(' ', '+'), "iso-8859-1"));
            sb.append("&ul=%25%2Fsc_%25&x=0&y=0&m=any&wf=0020&wm=wrd&sy=0");
            String xml = webBrowser.request(sb.toString());

            String FilmUpITStartResult;
            String FilmUpITMediaPrefix;
            FilmUpITStartResult = "<DT>1.";
            FilmUpITMediaPrefix = "sc_";

            for (String searchResult : HTMLTools.extractTags(xml, FilmUpITStartResult, "<DD>", FilmUpITMediaPrefix, ".htm", false)) {
                return searchResult;
            }

            logger.finer("No ID Found with request : " + sb.toString());
            return Movie.UNKNOWN;

        } catch (Exception error) {
            logger.severe("Failed to retrieve FilmUp ID for movie : " + title);
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml = "";

        try {
            xml = webBrowser.request("http://filmup.leonardo.it/sc_" + id + ".htm");
            String posterPageUrl = HTMLTools.extractTag(xml, "href=\"posters/locp/", "\"");

            String baseUrl = "http://filmup.leonardo.it/posters/locp/";
            xml = webBrowser.request(baseUrl + posterPageUrl);
            String tmpPosterURL = HTMLTools.extractTag(xml, "\"../loc/", "\"");
            if (StringTools.isValidString(tmpPosterURL)) {
                posterURL = "http://filmup.leonardo.it/posters/loc/" + tmpPosterURL;
                logger.finest("Movie PosterURL : " + posterPageUrl);
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving poster : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }

        if (StringTools.isValidString(posterURL)) {
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
        return "filmupit";
    }

}
