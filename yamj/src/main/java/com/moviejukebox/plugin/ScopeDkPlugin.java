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

import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.HTMLTools.*;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class ScopeDkPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(ScopeDkPlugin.class);
    public static final String SCOPEDK_PLUGIN_ID = "scopedk";
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

    /**
     * Scan Scope.Dk HTML page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://www.scope.dk/film/" + movie.getId(SCOPEDK_PLUGIN_ID), Charset.forName("ISO-8859-1"));

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

        } catch (IOException error) {
            logger.error("Failed retreiving ScopeDk infos for movie : " + movie.getId(SCOPEDK_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
        }
        return true;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval;
        try {
            String scopeDkId = mediaFile.getId(SCOPEDK_PLUGIN_ID);

            if (StringTools.isNotValidString(scopeDkId)) {
                scopeDkId = getScopeDkId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile);
            }

            // we also get imdb Id for extra infos
            if (StringTools.isNotValidString(mediaFile.getId(IMDB_PLUGIN_ID))) {
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.isTVShow()));
                logger.debug("Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }

            if (StringTools.isValidString(scopeDkId)) {
                mediaFile.setId(SCOPEDK_PLUGIN_ID, scopeDkId);
                logger.debug("Scope.dk Id available (" + scopeDkId + "), updating media info");
                retval = updateMovieInfo(mediaFile);
            } else {
                logger.debug("No Scope.dk Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (Exception error) {
            logger.debug("Parse error in ScopeDkPlugin we fall back to ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the Scope.dk id matching the specified movie name. This routine
     * is base on a Scope.dk search.
     *
     * @throws ParseException
     */
    private String getScopeDkId(String movieName, String year, Identifiable mediaFile) throws ParseException {
        String filmUpITId = Movie.UNKNOWN;

        try {
            StringBuilder sb = new StringBuilder("http://www.scope.dk/sogning.php?sog=");// 9&type=film");
            sb.append(URLEncoder.encode(movieName.replace(' ', '+'), "iso-8859-1"));
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
                            return new String(tmp.get(i).substring(startIndex + strRef.length(), endIndex));
                        }
                    } else {
                        // No year, so take the first one :(
                        return new String(tmp.get(i).substring(startIndex + strRef.length(), endIndex));
                    }
                } else {
                    logger.warn("Not matching data for search film result : " + tmp.get(i));
                }
                i++; // Step of 2
            }
            logger.debug("No ID Found with request : " + sb.toString());
            return Movie.UNKNOWN;

        } catch (Exception error) {
            logger.error("Failed to retrieve Scope ID for movie : " + movieName);
            logger.error("We fall back to ImdbPlugin");
            throw new ParseException(filmUpITId, 0);
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // If we use Scope.dk plugin look for
        // http://www.scope.dk/...=XXXXX.html
        logger.debug("ScopeDk: Scanning NFO for ScopeDk Id");
        Matcher idMatcher = patternScopeDkIp.matcher(nfo);
        if (!idMatcher.matches()) {
            idMatcher = patternScopeDkIpMovidedb.matcher(nfo);
        }

        boolean result = false;
        if (idMatcher.matches()) {
            String idMovie = idMatcher.group(3);
            logger.debug("ScopeDkPlugin: Scope.dk Id found in nfo = " + idMovie);
            movie.setId(SCOPEDK_PLUGIN_ID, idMovie);
            result = true;
        } else {
            logger.debug("ScopeDkPlugin: No Scope.dk Id found in nfo !");
        }
        return result;
    }
}
