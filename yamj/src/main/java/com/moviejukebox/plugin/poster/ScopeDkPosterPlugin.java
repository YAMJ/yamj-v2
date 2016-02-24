/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.YamjHttpClient;
import com.moviejukebox.tools.YamjHttpClientBuilder;

public class ScopeDkPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ScopeDkPosterPlugin.class);
    private static final String CHARSET = "ISO-8859-1";
    
    private YamjHttpClient httpClient;

    public ScopeDkPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        httpClient = YamjHttpClientBuilder.getHttpClient();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            StringBuilder sb = new StringBuilder("http://www.scope.dk/sogning.php?sog=");// 9&type=film");
            sb.append(URLEncoder.encode(title.replace(' ', '+'), CHARSET));
            sb.append("&type=film");
            String xml = httpClient.request(sb.toString(), Charset.forName(CHARSET));

            List<String> tmp = HTMLTools.extractTags(xml, "<table class=\"table-list\">", "</table>", "<td>", "</td>", false);

            for (int i = 0; i < tmp.size(); i++) {
                String strRef = "<a href=\"film/";
                int startIndex = tmp.get(i).indexOf(strRef);
                int endIndex = tmp.get(i).indexOf("-", startIndex + strRef.length());

                if (startIndex > -1 && endIndex > -1) {
                    // Take care of the year.
                    if (StringTools.isValidString(year)) {
                        // Found the same year. Ok
                        if (year.equalsIgnoreCase(tmp.get(i + 1).trim())) {
                            response = tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                            break;
                        }
                    } else {
                        // No year, so take the first one :(
                        response = tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                        break;
                    }
                } else {
                    LOG.warn("No matching data for search film result: {}", tmp.get(i));
                }
                i++; // Step of 2
            }
        } catch (Exception error) {
            LOG.error("Failed to retrieve Scope ID for movie: {}", title);
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        // <td><img src="
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuilder sb = new StringBuilder("http://www.scope.dk/film/");
                sb.append(id);

                String xml = httpClient.request(sb.toString(), Charset.forName(CHARSET));
                String posterPageUrl = HTMLTools.extractTag(xml, "<div id=\"film-top-left\">", "</div>");
                posterPageUrl = HTMLTools.extractTag(posterPageUrl, "<a href=\"#\"", "</a>");
                posterPageUrl = posterPageUrl.substring(posterPageUrl.indexOf("src=\"") + 5, posterPageUrl.indexOf("height") - 2);
                if (StringTools.isValidString(posterPageUrl)) {
                    posterURL = posterPageUrl;
                }

            } catch (Exception ex) {
                LOG.error("Failed retreiving ScopeDk url for movie : {}", id);
                LOG.error(SystemTools.getStackTrace(ex));
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
        return "scopeDk";
    }

}
