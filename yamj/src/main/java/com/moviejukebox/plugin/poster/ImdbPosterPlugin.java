/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbInfo;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbPosterPlugin.class);
    private WebBrowser webBrowser;
    private ImdbInfo imdbInfo;

    public ImdbPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        webBrowser = new WebBrowser();
        imdbInfo = new ImdbInfo();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;

        try {
            String imdbId = imdbInfo.getImdbId(title, year);
            if (StringTools.isValidString(imdbId)) {
                response = imdbId;
            }
        } catch (Exception error) {
            LOG.error("Imdb Error: {}", error.getMessage());
            return Movie.UNKNOWN;
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String imdbXML;

        try {
            if (StringTools.isValidString(id)) {
                imdbXML = webBrowser.request(imdbInfo.getSiteDef().getSite() + "title/" + id + "/", imdbInfo.getSiteDef().getCharset());

                StringTokenizer st;

                // Use cast token to avoid internalization trouble
                int castIndex, beginIndex;
                castIndex = imdbXML.indexOf("<h3>" + imdbInfo.getSiteDef().getCast() + "</h3>");

                if (castIndex > -1) {
                    // Use the old format
                    beginIndex = imdbXML.indexOf("src=\"http://ia.media-imdb.com/images");
                    st = new StringTokenizer(imdbXML.substring(beginIndex + 5), "\"");
                } else {
                    // Try the new format
                    castIndex = imdbXML.indexOf("<h2>" + imdbInfo.getSiteDef().getCast() + "</h2>");
                    beginIndex = imdbXML.indexOf("href='http://ia.media-imdb.com/images");
                    st = new StringTokenizer(imdbXML.substring(beginIndex + 6), "'");
                }

                // Search the XML from IMDB for a poster
                if ((beginIndex < castIndex) && (beginIndex != -1)) {
                    posterURL = st.nextToken();
                    int index = posterURL.indexOf("_SX");
                    if (index != -1) {
                        posterURL = posterURL.substring(0, index) + "_SX600_SY800_.jpg";
                    } else {
                        posterURL = Movie.UNKNOWN;
                    }
                    LOG.debug("Imdb found poster @: {}", posterURL);
                }

            }
        } catch (IOException ex) {
            LOG.error("Imdb Error: {}", ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
            return Image.UNKNOWN;
        }

        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }

        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "imdb";
    }

}
