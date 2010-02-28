/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class ScopeDkPosterPlugin implements IPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public ScopeDkPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://www.scope.dk/sogning.php?sog=");// 9&type=film");
            sb.append(URLEncoder.encode(title.replace(' ', '+'), "iso-8859-1"));
            sb.append("&type=film");
            String xml = webBrowser.request(sb.toString(),Charset.forName("ISO-8859-1"));

            List<String> tmp = HTMLTools.extractTags(xml, "<table class=\"table-list\">", "</table>", "<td>", "</td>", false);

            for (int i = 0; i < tmp.size(); i++) {
                String strRef = "<a href=\"film/";
                int startIndex = tmp.get(i).indexOf(strRef);
                int endIndex = tmp.get(i).indexOf("-", startIndex + strRef.length());

                if (startIndex > -1 && endIndex > -1) {
                    // Take care of the year.
                    if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                    logger.warning("Not matching data for search film result : " + tmp.get(i));
                }
                i++; // Step of 2
            }
        } catch (Exception error) {
            logger.severe("Failed to retrieve Scope ID for movie : " + title);
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        // <td><img src="
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.scope.dk/film/");
                sb.append(id);

                String xml = webBrowser.request(sb.toString(), Charset.forName("ISO-8859-1"));
                String posterPageUrl = HTMLTools.extractTag(xml, "<div id=\"film-top-left\">", "</div>");
                posterPageUrl = HTMLTools.extractTag(posterPageUrl, "<a href=\"#\"", "</a>");
                posterPageUrl = posterPageUrl.substring(posterPageUrl.indexOf("src=\"") + 5, posterPageUrl.indexOf("height") - 2);
                if (!posterPageUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
                    response = posterPageUrl;
                }

            } catch (Exception e) {
                logger.severe("Failed retreiving ScopeDk url for movie : " + id);
                logger.severe("Error : " + e.getMessage());
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }

    @Override
    public String getName() {
        return "scopeDk";
    }

    @Override
    public String getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

}
