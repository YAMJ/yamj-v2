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
package com.moviejukebox.plugin;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;

public class FilmAffinityInfo {

    private static final Logger LOG = LoggerFactory.getLogger(FilmAffinityInfo.class);
    private final WebBrowser webBrowser;

    /*
     * In filmAffinity there is several possible titles in the search results:
     * Spanish title
     * Spanish title (original title)
     * original title (Spanish title)
     *
     * It extracts one or both titles to find an exact match.
     */
    private final Pattern titlePattern = Pattern.compile("([^\\(]+)(  \\((.+)\\))?", Pattern.CASE_INSENSITIVE);
    /*
     * To test the URL and to determine if we got a redirect due to unique result
     */
    private final Pattern idPattern = Pattern.compile(".+\\/es\\/(film[0-9]{6}\\.html).*");
    /*
     * To isolate every title (with id) from search results
     */
    private final Pattern linkPattern = Pattern.compile("<div class=\"mc-title\"><a href=\"/es/(film[0-9]{6}\\.html)\">([^<]+)</a>");
    public static final String FILMAFFINITY_PLUGIN_ID = "filmaffinity";

    public FilmAffinityInfo() {
        webBrowser = new WebBrowser();
    }

    public String getIdFromMovieInfo(String title, String year) {
        return getIdFromMovieInfo(title, year, -1);
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        String firstResponse = response;
        Matcher titleMatcher;
        Matcher linkMatcher;
        Matcher idMatcher;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("http://www.filmaffinity.com/es/advsearch.php?stext=");

            sb.append(URLEncoder.encode(title, "ISO-8859-1"));
            if (tvSeason > -1) {
                sb.append("+TV");
            }

            sb.append("&stype[]=title&genre=&country=&");

            // It uses the year in URL advanced search (if it knows)
            if (StringTools.isValidString(year)) {
                sb.append("fromyear=").append(year).append("&toyear=").append(year);
            } else {
                sb.append("fromyear=&toyear=");
            }

            String url = webBrowser.getUrl(sb.toString());

            idMatcher = idPattern.matcher(url);
            if (idMatcher.matches()) {
                // we got a redirect due to unique result
                response = idMatcher.group(1);
            } else {
                String xml = webBrowser.request(sb.toString(), Charset.forName("ISO-8859-1"));

                linkMatcher = linkPattern.matcher(xml);

                while (linkMatcher.find() && Movie.UNKNOWN.equals(response)) {
                    xml = xml.substring(linkMatcher.start(1));

                    if (Movie.UNKNOWN.equalsIgnoreCase(firstResponse)) {
                        firstResponse = linkMatcher.group(1);
                    }

                    titleMatcher = titlePattern.matcher(linkMatcher.group(2));
                    if (titleMatcher.matches()
                            && ((titleMatcher.group(1) != null && titleMatcher.group(1).equalsIgnoreCase(title))
                            || (titleMatcher.group(3) != null && titleMatcher.group(3).equalsIgnoreCase(title)))) {
                        response = linkMatcher.group(1);
                    }

                    linkMatcher = linkPattern.matcher(xml);
                }

                if (Movie.UNKNOWN.equalsIgnoreCase(response)) {
                    response = firstResponse;
                }
            }

        } catch (Exception ex) {
            LOG.error("Failed retrieving Id for movie: {}", title);
            LOG.error(SystemTools.getStackTrace(ex));
        }
        return response;
    }

    /**
     * Normalize the FilmAffinity ID.
     *
     * This permits use several types of ID:<br/>
     * film[0-9]{6}.html (this the complete and it returns this)<br/>
     * film[0-9]{6}<br/>
     * [0-9]{6}.html<br/>
     * [0-9]{6}<br/>
     *
     * @param id
     * @return
     */
    public String arrangeId(String id) {
        Matcher matcher = Pattern.compile("(film[0-9]{6}\\.html)|(film[0-9]{6})|([0-9]{6}\\.html)|([0-9]{6})").matcher(id);

        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return matcher.group(1);
            } else if (matcher.group(2) != null) {
                return matcher.group(2) + ".html";
            } else if (matcher.group(3) != null) {
                return "film" + matcher.group(3);
            } else {
                return "film" + matcher.group(4) + ".html";
            }
        }
        return Movie.UNKNOWN;
    }
}
