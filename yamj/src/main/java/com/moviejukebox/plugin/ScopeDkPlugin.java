/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.HTMLTools.*;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class ScopeDkPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(ScopeDkPlugin.class);
    public static String SCOPEDK_PLUGIN_ID = "scopedk";
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

            if (!movie.isOverrideTitle()) {
                movie.setTitle(removeHtmlTags(extractTag(xml, "<div class=\"full-box-header\">", "</div>")).replaceAll("\t", ""));
            }

            if (movie.getPlot().equals(Movie.UNKNOWN)) {
                movie.setPlot(removeHtmlTags(extractTag(xml, "<div id=\"film-top-middle\">", "<br />")));
            }

            if (movie.getDirector().equals(Movie.UNKNOWN)) {
                movie.addDirector(removeHtmlTags(extractTag(xml, "<th>Instruktør", "</td>")));
            }
            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(removeHtmlTags(extractTag(xml, "<th>Spilletid", ".</td>")));
            }

            if (movie.getGenres().isEmpty()) {
                for (String tmp_genre : extractTag(xml, "<th>Genre</th>", "</td>").split(",")) {
                    movie.addGenre(removeHtmlTags(tmp_genre));

                }
            }

            if (movie.getCast().isEmpty()) {
                for (String actor : extractTag(xml, "<th>Medvirkende</th>", "</td>").split(",")) {
                    movie.addActor(removeHtmlTags(actor).trim());
                }
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
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear()));
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
        String FilmUpITId = Movie.UNKNOWN;

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
            throw new ParseException(FilmUpITId, 0);
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
