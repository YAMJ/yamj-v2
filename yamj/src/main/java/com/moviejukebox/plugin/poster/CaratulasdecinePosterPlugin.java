/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.util.ArrayList;

public class CaratulasdecinePosterPlugin extends AbstractMoviePosterPlugin {
//    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser = new WebBrowser();
    private static final String SEARCH_START = "La Web";
    private static final String SEARCH_END = "Sugerencias de búsqueda";
    private static final String TITLE_START = "Carátula de la película: ";
    private static final String TITLE_END = "</a>";
    private static final String SEARCH_ID_START = "caratula.php?pel=";

    public CaratulasdecinePosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
    }

    /**
     * Look for the movie URL in the XML.
     * If there is no title, or the title is not found, return the first movie URL
     * @param xml
     * @param title
     * @return
     * @throws IOException 
     */
    private String getMovieId(String xml, String title) throws IOException {
        String movieId = Movie.UNKNOWN;

        ArrayList<String> foundTitles = HTMLTools.extractTags(xml, SEARCH_START, SEARCH_END, TITLE_START, TITLE_END, false);

        for (String searchTitle : foundTitles) {
            String cleanTitle = HTMLTools.stripTags(searchTitle);
            if (title != null && title.equalsIgnoreCase(cleanTitle)) {
                movieId = findIdInXml(xml, searchTitle);
                if (StringTools.isValidString(movieId)) {
                    // Found the movie ID, so quit
                    break;
                }
            }
        }

        // If we didn't find the title, try just getting the first returned one
        if (StringTools.isNotValidString(movieId) && foundTitles.size() > 0) {
            movieId = findIdInXml(xml, foundTitles.get(0));
        }

        return movieId;
    }

    private String findIdInXml(String xml, String searchTitle) {
        String movieId = Movie.UNKNOWN;
        int beginIndex = xml.indexOf(SEARCH_ID_START, xml.indexOf(TITLE_START + searchTitle + TITLE_END));
        if (beginIndex > -1) {
            movieId = new String(xml.substring(beginIndex + SEARCH_ID_START.length(), xml.indexOf(" ", beginIndex + SEARCH_ID_START.length())));
        }
        
        return movieId;
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            StringBuilder sb = new StringBuilder("http://www.google.es/custom?hl=es&domains=caratulasdecine.com&sa=Search&sitesearch=caratulasdecine.com&client=pub-8773978869337108&forid=1&q=");
            sb.append(URLEncoder.encode(title, "ISO-8859-1"));
            String xml = webBrowser.request(sb.toString());
            response = getMovieId(xml, title);

            if (StringTools.isNotValidString(response)) {
                // Did we've a link to the movie list
                String searchString = "http://www.caratulasdecine.com/listado.php";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    String url = new String(xml.substring(beginIndex, xml.indexOf("\"", beginIndex + searchString.length())));
                    // Need to find a better way to do this
                    url = url.replaceAll("&amp;", "&");
                    xml = webBrowser.request(url, Charset.forName("ISO-8859-1"));
                    String sectionStart = " <a class='pag' href='listado.php?";
                    String sectionEnd = "</p>";
                    String extractTag = HTMLTools.extractTag(xml, sectionStart, sectionEnd);// , startTag, endTag);
                    String[] extractTags = extractTag.split("<a class=\"A\"");
                    for (String string : extractTags) {
                        if (string.contains(title)) {
                            response = getMovieId(string, title);
                            break;
                        }
                    }
                }
            }
        } catch (Exception error) {
            logger.error("Failed retreiving CaratulasdecinePoster Id for movie: " + title);
            logger.error("Error : " + error.getMessage());
        }

        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuilder sb = new StringBuilder("http://www.caratulasdecine.com/caratula.php?pel=");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                String searchString = "<td><img src=\"";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    posterURL = "http://www.caratulasdecine.com/"
                            + new String(xml.substring(beginIndex + searchString.length(), xml.indexOf(" ", beginIndex + searchString.length()) - 1 ));
                }

            } catch (Exception error) {
                logger.error("Failed retreiving CaratulasdecinePoster url for movie : " + id);
                logger.error("Error : " + error.getMessage());
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
        return "caratulasdecine";
    }
}
