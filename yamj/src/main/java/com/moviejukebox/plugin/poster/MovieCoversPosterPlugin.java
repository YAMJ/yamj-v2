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

import java.io.IOException;
import java.net.URLEncoder;
import java.text.Normalizer;

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

public class MovieCoversPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieCoversPosterPlugin.class);
    private static final String CHARSET = "ISO-8859-1";
    
    private YamjHttpClient httpClient;

    public MovieCoversPosterPlugin() {
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

        try {
            StringBuilder sb = new StringBuilder("http://www.moviecovers.com/multicrit.html?titre=");
            sb.append(URLEncoder.encode(title.replace("\u0153", "oe"), "ISO-8859-1"));

            if (StringTools.isValidString(year)) {
                sb.append("&anneemin=");
                sb.append(URLEncoder.encode(Integer.toString(Integer.parseInt(year) - 1), CHARSET));
                sb.append("&anneemax=");
                sb.append(URLEncoder.encode(Integer.toString(Integer.parseInt(year) + 1), CHARSET));
            }
            sb.append("&slow=0&tri=Titre&listes=1");
            LOG.debug("Searching for: {}", sb.toString());

            String content = httpClient.request(sb.toString());

            if (content != null) {
                String formattedTitle = Normalizer.normalize(title.replace("\u0153", "oe").toUpperCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
                if (formattedTitle.endsWith(" (TV)")) {
                    formattedTitle = formattedTitle.substring(0, formattedTitle.length() - 5);
                }
                String formattedTitleNormalized = formattedTitle;
                for (String prefix : Movie.getSortIgnorePrefixes()) {
                    if (formattedTitle.startsWith(prefix.toUpperCase())) {
                        formattedTitleNormalized = formattedTitle.substring(prefix.length()) + " (" + prefix.toUpperCase().replace(" ", "") + ")";
                        break;
                    }
                }
                // logger.debug(LOG_MESSAGE+"Looking for a poster for: " + formattedTitleNormalized);
                // Checking for "no result" message...
                if (!content.contains("/forum/index.html?forum=MovieCovers&vue=demande")) {
                    // There is some results
                    for (String filmURL : HTMLTools.extractTags(content, "<TD bgcolor=\"#339900\"", "<FORM action=\"/multicrit.html\"", "<LI><A href=\"/film/titre", "</A>", false)) {
                        if ((filmURL.endsWith(formattedTitleNormalized)) || (filmURL.endsWith(formattedTitle))) {
                            returnString = HTMLTools.extractTag(filmURL, "_", ".html\">");
                            // logger.debug(LOG_MESSAGE+"Seems to find something: " + returnString + " - " + filmURL);
                            break;
                        }
                    }
                }
                // Search the forum if no answer
                if (returnString.equalsIgnoreCase(Movie.UNKNOWN)) {
                    sb = new StringBuilder("http://www.moviecovers.com/forum/search-mysql.html?forum=MovieCovers&query=");
                    sb.append(URLEncoder.encode(formattedTitle, CHARSET));
                    // logger.debug(LOG_MESSAGE+"We have to explore the forums: " + sb);
                    content = httpClient.request(sb.toString());
                    if (content != null) {
                        // Loop through the search results
                        for (String filmURL : HTMLTools.extractTags(content, "<TABLE border=\"0\" cellpadding=\"0\" cellspacing=\"0\">", "<FORM action=\"search-mysql.html\">", "<TD><A href=\"fil.html?query=", "</A></TD>", false)) {
                            // logger.debug(LOG_MESSAGE+"examining: " + filmURL);
                            if ((filmURL.endsWith(formattedTitleNormalized)) || (filmURL.endsWith(formattedTitle))) {
                                content = httpClient.request("http://www.moviecovers.com/forum/fil.html?query=" + filmURL.substring(0, filmURL.length() - formattedTitle.length() - 2));
                                if (content != null) {
                                    int sizePoster;
                                    int oldSizePoster = 0;
                                    // A quick trick to find a " fr " reference in the comments
                                    int indexFR = content.toUpperCase().indexOf(" FR ");
                                    if (indexFR != -1) {
                                        content = "</STRONG></B></FONT>" + content.substring(indexFR);
                                    }
                                    // Search the biggest picture
                                    for (String poster : HTMLTools.extractTags(content, "</STRONG></B></FONT>", ">MovieCovers Team<", "<LI><A TARGET=\"affiche\" ", "Ko)", false)) {
                                        sizePoster = Integer.parseInt(HTMLTools.extractTag(poster, ".jpg\">Image .JPG</A> ("));
                                        if (sizePoster > oldSizePoster) {
                                            oldSizePoster = sizePoster;
                                            returnString = HTMLTools.extractTag(poster, "HREF=\"/getjpg.html/", ".jpg\">Image .JPG</A>");
                                            if (indexFR != -1) {
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (NumberFormatException | IOException ex) {
            LOG.error("Failed retreiving Moviecovers poster URL: {}", title);
            LOG.error(SystemTools.getStackTrace(ex));
            return Movie.UNKNOWN;
        }
        LOG.debug("Retreiving Moviecovers poster URL: {}", returnString);
        return returnString;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;

        if (id != null && !Movie.UNKNOWN.equalsIgnoreCase(id)) {
            LOG.debug("Movie found on moviecovers.com: {}", id);
            posterURL = "http://www.moviecovers.com/getjpg.html/" + id.replace("+", "%20");
        } else {
            LOG.debug("Unable to find posters for {}", id);
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public String getName() {
        return "moviecovers";
    }

}
