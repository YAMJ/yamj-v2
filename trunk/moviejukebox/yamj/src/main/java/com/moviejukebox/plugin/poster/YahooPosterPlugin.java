/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.log4j.Logger;

public class YahooPosterPlugin extends AbstractMoviePosterPlugin {
    private static final Logger LOG = Logger.getLogger(YahooPosterPlugin.class);
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
            StringBuilder sb = new StringBuilder("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("imgurl=");
            int endIndex = xml.indexOf("%26", beginIndex);

            if (beginIndex != -1 && endIndex > beginIndex) {
                posterURL = URLDecoder.decode(xml.substring(beginIndex + 7, endIndex), "UTF-8");
            }
        } catch (Exception error) {
            LOG.error("YahooPosterPlugin : Failed retreiving poster URL from yahoo images : " + title);
            LOG.error("Error : " + error.getMessage());
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
