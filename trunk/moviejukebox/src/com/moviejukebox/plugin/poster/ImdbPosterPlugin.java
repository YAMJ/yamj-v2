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

import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbInfo;
import com.moviejukebox.tools.WebBrowser;

public class ImdbPosterPlugin implements IMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;
    private ImdbInfo imdbInfo;

    public ImdbPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
        imdbInfo = new ImdbInfo();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;

        try {

            String imdbId = imdbInfo.getImdbId(title, year);
            if (!Movie.UNKNOWN.equals(imdbId)) {
                response = imdbId;
            }
        } catch (Exception error) {
            logger.severe("PosterScanner: Imdb Error: " + error.getMessage());
            return Movie.UNKNOWN;
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String imdbXML;

        try {
            if (!Movie.UNKNOWN.equals(id)) {
                imdbXML = webBrowser.request(imdbInfo.getSiteDef().getSite() + "title/" + id + "/", imdbInfo.getSiteDef().getCharset());

                StringTokenizer st;

                int castIndex = imdbXML.indexOf("<h3>Cast</h3>");
                int beginIndex = imdbXML.indexOf("src=\"http://ia.media-imdb.com/images");

                // Search the XML from IMDB for a poster
                if ((beginIndex < castIndex) && (beginIndex != -1)) {
                    st = new StringTokenizer(imdbXML.substring(beginIndex + 5), "\"");
                    posterURL = st.nextToken();
                    int index = posterURL.indexOf("_SX");
                    if (index != -1) {
                        posterURL = posterURL.substring(0, index) + "_SX600_SY800_.jpg";
                    } else {
                        posterURL = Movie.UNKNOWN;
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("PosterScanner: Imdb Error: " + error.getMessage());
            return Movie.UNKNOWN;
        }
        return posterURL;
    }

    @Override
    public String getName() {
        return "imdb";
    }

}
