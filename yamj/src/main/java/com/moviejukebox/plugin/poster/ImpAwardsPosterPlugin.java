/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin.poster;

import java.net.URLEncoder;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.YamjHttpClient;
import com.moviejukebox.tools.YamjHttpClientBuilder;

public class ImpAwardsPosterPlugin extends AbstractMoviePosterPlugin {

    private static final String URL_PREFIX = "http://www.impawards.com/googlesearch.html?cx=partner-pub-6811780361519631%3A48v46vdqqnk&cof=FORID%3A9&ie=ISO-8859-1&q=";
    private static final String URL_SUFFIX = "&sa=Search&siteurl=www.impawards.com%252F";

    private YamjHttpClient httpClient;

    public ImpAwardsPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        httpClient = YamjHttpClientBuilder.getHttpClient();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String returnString = Movie.UNKNOWN;
        String content;

        try {
            StringBuilder searchURL = new StringBuilder();
            searchURL.append(URL_PREFIX);
            searchURL.append(URLEncoder.encode(title + " " + year, "UTF-8"));
            searchURL.append(URL_SUFFIX);

            content = httpClient.request(searchURL.toString());
        } catch (Exception error) {
            // The movie doesn't exists, so return unknown
            return Movie.UNKNOWN;
        }

        if (content != null) {
            int indexMovieLink = content.indexOf("<a href=\"http://www.impawards.com/" + year + "/");
            if (indexMovieLink != -1) {
                int endIndex = content.indexOf("\"", indexMovieLink + 39);
                if (endIndex != -1) {
                    String tmp = content.substring(indexMovieLink + 39, endIndex);
                    if (!tmp.endsWith("standard.html")) {
                        returnString = year + "/posters/" + content.substring(indexMovieLink + 39, endIndex);
                    }
                }
            }
        }

        return returnString;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;

        if (StringTools.isValidString(id) && id.endsWith(".html")) {
            posterURL = "http://www.impawards.com/" + id.substring(0, id.length() - 4) + "jpg";
        }

        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }

        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "impawards";
    }
}
