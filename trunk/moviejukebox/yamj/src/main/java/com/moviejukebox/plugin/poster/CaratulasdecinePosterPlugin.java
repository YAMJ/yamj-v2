/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaratulasdecinePosterPlugin extends AbstractMoviePosterPlugin {

    private final WebBrowser webBrowser = new WebBrowser();
    private static final Logger LOG = LoggerFactory.getLogger(CaratulasdecinePosterPlugin.class);

    private static final String SEARCH_START = "La Web";
    private static final String SEARCH_END = "Sugerencias de búsqueda";
    private static final String TITLE_START = "Carátula de la película: ";
    private static final String TITLE_END = "</a>";
    private static final String SEARCH_ID_START = "caratula.php?pel=";

    public CaratulasdecinePosterPlugin() {
        super();

        // Check to see if we are needed
//        if (!isNeeded()) {
//            return;
//        }
    }

    /**
     * Look for the movie URL in the XML. If there is no title, or the title is
     * not found, return the first movie URL
     *
     * @param xml
     * @param title
     * @return
     * @throws IOException
     */
    private String getMovieId(String xml, String title) throws IOException {
        String movieId = Movie.UNKNOWN;

        List<String> foundTitles = HTMLTools.extractTags(xml, SEARCH_START, SEARCH_END, TITLE_START, TITLE_END, false);

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
            movieId = xml.substring(beginIndex + SEARCH_ID_START.length(), xml.indexOf(" ", beginIndex + SEARCH_ID_START.length()));
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
                            response = getMovieId(string, title);
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed retreiving CaratulasdecinePoster Id for movie '{}'", title);
            LOG.error(SystemTools.getStackTrace(ex));
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
                            + xml.substring(beginIndex + searchString.length(), xml.indexOf(" ", beginIndex + searchString.length()) - 1);
                }

            } catch (Exception ex) {
                LOG.error("Failed retreiving CaratulasdecinePoster url for movie '{}'", id);
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
        return "caratulasdecine";
    }
}
