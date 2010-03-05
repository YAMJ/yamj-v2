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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class CaratulasdecinePosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public CaratulasdecinePosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    private String getMovieUrl(String xml) throws IOException {
        String response = Movie.UNKNOWN;

        String searchString = "caratula.php?pel=";
        int beginIndex = xml.indexOf(searchString);
        if (beginIndex > -1) {
            response = xml.substring(beginIndex + searchString.length(), xml.indexOf("\"", beginIndex + searchString.length()));
        }
        return response;
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://www.google.es/custom?hl=es&domains=caratulasdecine.com&ie=ISO-8859-1&oe=ISO-8859-1&q=");

            sb.append(URLEncoder.encode(title, "ISO-8859-1"));
            sb.append("&btnG=Buscar&sitesearch=caratulasdecine.com&meta=");
            String xml = webBrowser.request(sb.toString());

            response = getMovieUrl(xml);

            if (Movie.UNKNOWN.equals(response)) {

                // Did we've a link to the movie list
                String searchString = "http://www.caratulasdecine.com/listado.php";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    String url = xml.substring(beginIndex, xml.indexOf("\"", beginIndex + searchString.length()));
                    // Need to find a better way to do this
                    url = url.replaceAll("&amp;", "&");
                    xml = webBrowser.request(url, Charset.forName("ISO-8859-1"));
                    String sectionStart = " <a class='pag' href='listado.php?";
                    String sectionEnd = "</p>";
                    String extractTag = HTMLTools.extractTag(xml, sectionStart, sectionEnd);// , startTag, endTag);
                    String[] extractTags = extractTag.split("<a class=\"A\"");
                    for (String string : extractTags) {
                        if (string.contains(title)) {
                            response = getMovieUrl(string);
                            break;
                        }
                    }
                }
                // }else{
                // logger.info("Movie " + title + " not found on www.caratulasdecine.com");
                // }

            }

        } catch (Exception e) {
            logger.severe("Failed retreiving CaratulasdecinePoster Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        // <td><img src="
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.caratulasdecine.com/caratula.php?pel=");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                String searchString = "<td><img src=\"";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    response = "http://www.caratulasdecine.com/"
                                    + xml.substring(beginIndex + searchString.length(), xml.indexOf("\"", beginIndex + searchString.length()));
                }

            } catch (Exception e) {
                logger.severe("Failed retreiving CaratulasdecinePoster url for movie : " + id);
                logger.severe("Error : " + e.getMessage());
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "Caratulasdecine";
    }
}
