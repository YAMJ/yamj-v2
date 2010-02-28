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

package com.moviejukebox.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

public class ScopeDkPlugin extends FilmUpITPlugin {
    public static String SCOPEDK_PLUGIN_ID = "scopedk";

    private final static Pattern patternScopeDkIp = Pattern.compile("^(.*)(http://www.scope.dk/film/)([0-9]+)(.*)");
    private final static Pattern patternScopeDkIpMovidedb = Pattern.compile("^(.*)(<id moviedb=\"scopedk\")>([0-9]+)(</id>.*)");

    public ScopeDkPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Danish");
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
                movie.setDirector(removeHtmlTags(extractTag(xml, "<th>Instruktør", "</td>")));
            }
            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(removeHtmlTags(extractTag(xml, "<th>Spilletid", ".</td>")));
            }

            if (movie.getGenres().size() == 0) {
                for (String tmp_genre : extractTag(xml, "<th>Genre</th>", "</td>").split(",")) {
                    movie.addGenre(removeHtmlTags(tmp_genre));

                }
            }

            if (movie.getCast().size() == 0) {
                for (String actor : extractTag(xml, "<th>Medvirkende</th>", "</td>").split(",")) {
                    movie.addActor(removeHtmlTags(actor).trim());
                }
            }

            // Removing Poster info from plugins. Use of PosterScanner routine instead.
            
            // String posterPageUrl = extractTag(xml, "<div id=\"film-top-left\">", "</div>");
            // posterPageUrl = extractTag(posterPageUrl, "<a href=\"#\"", "</a>");
            // posterPageUrl = posterPageUrl.substring(posterPageUrl.indexOf("src=\"") + 5, posterPageUrl.indexOf("height") - 2);
            // if (!posterPageUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
            // movie.setPosterURL(posterPageUrl);
            // }
        } catch (IOException error) {
            logger.severe("Failed retreiving ScopeDk infos for movie : " + movie.getId(SCOPEDK_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        try {
            String scopeDkId = mediaFile.getId(SCOPEDK_PLUGIN_ID);
            if (scopeDkId.equalsIgnoreCase(Movie.UNKNOWN)) {
                scopeDkId = getScopeDkId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile);
            }
            // we also get imdb Id for extra infos
            if (mediaFile.getId(IMDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear()));
                logger.finest("Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }
            if (!scopeDkId.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(SCOPEDK_PLUGIN_ID, scopeDkId);
                logger.finer("Scope.dk Id available (" + scopeDkId + "), updating media info");
                retval = updateMovieInfo(mediaFile);
            } else {
                logger.finer("No Scope.dk Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (Exception error) {
            logger.finer("Parse error in ScopeDkPlugin we fall back to ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the Scope.dk id matching the specified movie name. This routine is base on a Scope.dk search.
     * 
     * @throws ParseException
     */
    private String getScopeDkId(String movieName, String year, Movie mediaFile) throws ParseException {
        String FilmUpITId = Movie.UNKNOWN;

        try {
            StringBuffer sb = new StringBuffer("http://www.scope.dk/sogning.php?sog=");// 9&type=film");
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
                    if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                        // Found the same year. Ok
                        if (year.equalsIgnoreCase(tmp.get(i + 1).trim())) {
                            return tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                        }
                    } else {
                        // No year, so take the first one :(
                        return tmp.get(i).substring(startIndex + strRef.length(), endIndex);
                    }
                } else {
                    logger.warning("Not matching data for search film result : " + tmp.get(i));
                }
                i++; // Step of 2
            }
            logger.finer("No ID Found with request : " + sb.toString());
            return Movie.UNKNOWN;

        } catch (Exception error) {
            logger.severe("Failed to retrieve Scope ID for movie : " + movieName);
            logger.severe("We fall back to ImdbPlugin");
            throw new ParseException(FilmUpITId, 0);
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // If we use Scope.dk plugin look for
        // http://www.scope.dk/...=XXXXX.html
        logger.finest("ScopeDk: Scanning NFO for ScopeDk Id");
        Matcher idMatcher = patternScopeDkIp.matcher(nfo);
        if (!idMatcher.matches()) {
            idMatcher = patternScopeDkIpMovidedb.matcher(nfo);
        }

        if (idMatcher.matches()) {
            String idMovie = idMatcher.group(3);
            logger.finer("ScopeDkPlugin: Scope.dk Id found in nfo = " + idMovie);
            movie.setId(SCOPEDK_PLUGIN_ID, idMovie);
        } else {
            logger.finer("ScopeDkPlugin: No Scope.dk Id found in nfo !");
        }
    }

}
