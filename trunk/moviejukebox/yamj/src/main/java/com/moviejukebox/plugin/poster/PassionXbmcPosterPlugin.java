/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import org.apache.log4j.Logger;

public class PassionXbmcPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;
    private AllocinePlugin allocinePlugin;
    private static final Logger logger = Logger.getLogger(PassionXbmcPosterPlugin.class);

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
        return allocinePlugin.getMovieId(title, year, -1);
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
