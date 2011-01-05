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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

public class YahooPosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    public YahooPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        // No id from yahoo search, return title
        return title;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        String posterURL = Movie.UNKNOWN;
        try {
            // TODO Change out the French Yahoo for English
            StringBuffer sb = new StringBuffer("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("imgurl=");
            int endIndex = xml.indexOf("%26", beginIndex);

            if (beginIndex != -1 && endIndex > beginIndex) {
                posterURL = URLDecoder.decode(xml.substring(beginIndex + 7, endIndex), "UTF-8");
            }
        } catch (Exception error) {
            logger.severe("YahooPosterPlugin : Failed retreiving poster URL from yahoo images : " + title);
            logger.severe("Error : " + error.getMessage());
        }
        
        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }
        
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String id) {
        return getPosterUrl(id, null);
    }

    @Override
    public String getName() {
        return "yahoo";
    }

}
