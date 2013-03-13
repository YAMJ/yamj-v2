/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.HTMLTools.*;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class ScopeDkPlugin extends ImdbPlugin {

    public static final String SCOPEDK_PLUGIN_ID = "scopedk";
    private static final Logger LOGGER = Logger.getLogger(ScopeDkPlugin.class);
    private static final String LOG_MESSAGE = "ScopeDkPlugin: ";
    private static final Pattern patternScopeDkIp = Pattern.compile("^(.*)(http://www.scope.dk/film/)([0-9]+)(.*)");
    private static final Pattern patternScopeDkIpMovidedb = Pattern.compile("^(.*)(<id moviedb=\"scopedk\")>([0-9]+)(</id>.*)");

    public ScopeDkPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Danish");
    }

    @Override
    public String getPluginID() {
        return SCOPEDK_PLUGIN_ID;
    }

    public String getMovieId(Movie movie) {
        String scopeDkId = movie.getId(SCOPEDK_PLUGIN_ID);
        if (StringTools.isNotValidString(scopeDkId)) {
            scopeDkId = getMovieId(movie.getTitle(), movie.getYear());
            movie.setId(SCOPEDK_PLUGIN_ID, scopeDkId);
        }
        return scopeDkId;
    }
    
    public String getMovieId(String title, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.scope.dk/sogning.php?sog=");
            sb.append(URLEncoder.encode(title.replace(' ', '+'), "iso-8859-1"));
            sb.append("&type=film");
            String xml = webBrowser.request(sb.toString());

            List<String> tmp = extractTags(xml, "<table class=\"table-list\">", "</table>", "<td>", "</td>");

            for (int i = 0; i < tmp.size(); i++) {
                String strRef = "<a href=\"film/";
                int startIndex = tmp.get(i).indexOf(strRef);
                int endIndex = tmp.get(i).indexOf("-", startIndex + strRef.length());

                if (startIndex > -1 && endIndex > -1) {
                    // Take care of the year.
                    if (StringTools.isValidString(year)) {
                        // Found the same year. Ok
                        if (year.equalsIgnoreCase(tmp.get(i + 1).trim())) {
                            return tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                        }
                    } else {
                        // No year, so take the first one :(
                        return tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                    }
                } else {
                    LOGGER.warn(LOG_MESSAGE + "Not matching data for search film result : " + tmp.get(i));
                }
                i++; // Step of 2
            }
            
            LOGGER.debug(LOG_MESSAGE + "No Scope.dk id found with request : " + sb.toString());
            
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving Scope.dk id for title : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    @Override
    public boolean scan(Movie movie) {
        String scopeDkId = getMovieId(movie);

        // we also get IMDb id for extra informations
        if (StringTools.isNotValidString(movie.getId(IMDB_PLUGIN_ID))) {
            movie.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow()));
            LOGGER.debug("Found imdbId = " + movie.getId(IMDB_PLUGIN_ID));
        }

        if (StringTools.isValidString(scopeDkId)) {
            LOGGER.debug(LOG_MESSAGE + "Scope.dk id available (" + scopeDkId + "), updating media info");
            return updateMediaInfo(movie, scopeDkId);
        }
        
        LOGGER.debug(LOG_MESSAGE + "Scope.dk id not available : " + movie.getTitle() + "; fall back to IMDb");
        return super.scan(movie);
    }

    private boolean updateMediaInfo(Movie movie, String scopeDkId) {
        try {
            String xml = webBrowser.request("http://www.scope.dk/film/" + scopeDkId, Charset.forName("ISO-8859-1"));

            if (OverrideTools.checkOverwriteTitle(movie, SCOPEDK_PLUGIN_ID)) {
                movie.setTitle(removeHtmlTags(extractTag(xml, "<div class=\"full-box-header\">", "</div>")).replaceAll("\t", ""), SCOPEDK_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwritePlot(movie, SCOPEDK_PLUGIN_ID)) {
                movie.setPlot(removeHtmlTags(extractTag(xml, "<div id=\"film-top-middle\">", "<br />")), SCOPEDK_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteDirectors(movie, SCOPEDK_PLUGIN_ID)) {
                movie.setDirector(removeHtmlTags(extractTag(xml, "<th>Instruktør", "</td>")), SCOPEDK_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, SCOPEDK_PLUGIN_ID)) {
                movie.setRuntime(removeHtmlTags(extractTag(xml, "<th>Spilletid", ".</td>")), SCOPEDK_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteGenres(movie, SCOPEDK_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<String>();
                for (String tmpGenre : extractTag(xml, "<th>Genre</th>", "</td>").split(",")) {
                    newGenres.add(removeHtmlTags(tmpGenre));
                }
                movie.setGenres(newGenres, SCOPEDK_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteActors(movie, SCOPEDK_PLUGIN_ID)) {
                List<String> newActors = new ArrayList<String>();
                for (String actor : extractTag(xml, "<th>Medvirkende</th>", "</td>").split(",")) {
                    newActors.add(removeHtmlTags(actor).trim());
                }
                movie.setCast(newActors, SCOPEDK_PLUGIN_ID);
            }

            return Boolean.TRUE;
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving media info : " + scopeDkId);
            LOGGER.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always scan for IMDb id, look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // ID already present
        if (StringTools.isValidString(movie.getId(SCOPEDK_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        LOGGER.debug(LOG_MESSAGE + "Scanning NFO for Scope.dk id");

        // If we use Scope.dk plugIn look for
        // http://www.scope.dk/...=XXXXX.html
        Matcher idMatcher = patternScopeDkIp.matcher(nfo);
        if (!idMatcher.matches()) {
            idMatcher = patternScopeDkIpMovidedb.matcher(nfo);
        }
        if (idMatcher.matches()) {
            String idMovie = idMatcher.group(3);
            LOGGER.debug(LOG_MESSAGE + "Scope.dk id found in NFO = " + idMovie);
            movie.setId(SCOPEDK_PLUGIN_ID, idMovie);
            return Boolean.TRUE;
        }
        
        LOGGER.debug(LOG_MESSAGE + "No Scope.dk id found in NFO : " + movie.getTitle());
        return Boolean.FALSE;
    }
}
