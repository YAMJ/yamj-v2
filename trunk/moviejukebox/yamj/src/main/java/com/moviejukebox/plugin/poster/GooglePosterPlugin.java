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
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.tools.WebBrowser;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

public class GooglePosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = Logger.getLogger(GooglePosterPlugin.class);
    private static final String LOG_MESSAGE = "GooglePosterPlugin: ";
    private WebBrowser webBrowser;

    // private int nbRetry;
    public GooglePosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        // No id from google search, return title
        return title;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        Image posterImage = new Image();
        try {
            StringBuilder sb = new StringBuilder("http://images.google.fr/images?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&gbv=2");

            String xml = webBrowser.request(sb.toString());
            // int tryLeft = nbRetry;
            int startSearch = 0;
            // while (tryLeft-- > 0 && Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
            // logger.debug(LOG_MESSAGE+"Try " + (nbRetry - tryLeft) + "/" + nbRetry);
            String searchString = "imgurl=";
            int beginIndex = xml.indexOf(searchString, startSearch) + 7;

            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"&");
                // QuickFix to "too much translation issue" space char -> %20 -> %2520
                posterImage.setUrl(st.nextToken().replace("%2520", "%20"));
                if (!PosterScanner.validatePoster(posterImage)) {
                    posterImage.setUrl(Movie.UNKNOWN);
                }
            }
            // } else {
            // // Stop try no more result.
            // tryLeft = 0;
            // }
            // }
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retreiving poster URL from google images : " + title);
            LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
        }
        return posterImage;
    }

    @Override
    public IImage getPosterUrl(String id) {
        return getPosterUrl(id, null);
    }

    @Override
    public String getName() {
        return "google";
    }
}
