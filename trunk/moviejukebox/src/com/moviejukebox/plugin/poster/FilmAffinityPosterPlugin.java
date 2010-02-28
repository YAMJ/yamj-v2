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
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class FilmAffinityPosterPlugin implements IPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public FilmAffinityPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://www.google.es/search?hl=es&q=");

            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (tvSeason > -1) {
                sb.append("+TV");
            }
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+").append(year);
            }

            sb.append("+site%3Awww.filmaffinity.com&btnG=Buscar+con+Google&meta=");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("/es/film");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 8), "/\"");
            String filmAffinityId = st.nextToken();

            if (filmAffinityId != "") {
                logger.finest("FilmAffinity: Found id: " + filmAffinityId);
                response = filmAffinityId;
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving filmaffinity Id for movie : " + title);
            logger.severe("Error : " + error.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        // <td><img src="
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.filmaffinity.com/es/film");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                response = HTMLTools.extractTag(xml, "<a class=\"lightbox\" href=\"", "\"");

            } catch (Exception e) {
                logger.severe("Failed retreiving FilmAffinity poster url for movie : " + id);
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
        return "filmaffinity";
    }

    @Override
    public String getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }
}
