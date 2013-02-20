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
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;

public class FilmUpITPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmUpITPlugin.class);
    public static final String FILMUPIT_PLUGIN_ID = "filmupit";

    public FilmUpITPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
    }

    @Override
    public String getPluginID() {
        return FILMUPIT_PLUGIN_ID;
    }

    /**
     * Scan FilmUp.IT HTML page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://filmup.leonardo.it/sc_" + movie.getId(FILMUPIT_PLUGIN_ID) + ".htm");

            if (OverrideTools.checkOverwriteTitle(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setTitle(removeHtmlTags(HTMLTools.extractTag(xml, "<font face=\"arial, helvetica\" size=\"3\"><b>", "</b>")), FILMUPIT_PLUGIN_ID);
            }

            // limit plot to FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT char
            if (OverrideTools.checkOverwritePlot(movie, FILMUPIT_PLUGIN_ID)) {
                String tmpPlot = removeHtmlTags(HTMLTools.extractTag(xml, "Trama:<br>", "</font><br>"));
                movie.setPlot(tmpPlot, FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteDirectors(movie, FILMUPIT_PLUGIN_ID)) {
                String director = removeHtmlTags(removeHtmlTags(HTMLTools.extractTag(xml,
                        "Regia:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")));
                movie.setDirector(director, FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setReleaseDate(removeHtmlTags(HTMLTools.extractTag(xml,
                        "Data di uscita:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setRuntime(removeHtmlTags(HTMLTools.extractTag(xml, "Durata:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setCountry(removeHtmlTags(HTMLTools.extractTag(xml, "Nazione:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setCompany(removeHtmlTags(HTMLTools.extractTag(xml,
                        "Distribuzione:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteGenres(movie, FILMUPIT_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<String>();
                for (String tmpGenre : HTMLTools.extractTag(xml, "Genere:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>").split(",")) {
                    newGenres.addAll(Arrays.asList(tmpGenre.split("/")));
                }
                movie.setGenres(newGenres, FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteYear(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setYear(removeHtmlTags(HTMLTools.extractTag(xml, "Anno:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteActors(movie, FILMUPIT_PLUGIN_ID)) {
                List<String> newActors = Arrays.asList(removeHtmlTags(
                		HTMLTools.extractTag(xml, "Cast:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")).split(","));
                movie.setCast(newActors, FILMUPIT_PLUGIN_ID);
            }

            String opinionsPageID = HTMLTools.extractTag(xml, "/opinioni/op.php?uid=", "\"");
            if (StringTools.isValidString(opinionsPageID)) {
                int pageID = Integer.parseInt(opinionsPageID);
                updateRate(movie, pageID);
                logger.debug("Opinions page UID = " + pageID);
            }

            if (downloadFanart && StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (StringTools.isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }

        } catch (IOException error) {
            logger.error("Failed retreiving FilmUP infos for movie : " + movie.getId(FILMUPIT_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
        }
        return true;
    }

    private void updateRate(Movie movie, int opinionsPageID) {
        String baseUrl = "http://filmup.leonardo.it/opinioni/op.php?uid=";
        try {
            String xml = webBrowser.request(baseUrl + opinionsPageID);
            float rating = Float.parseFloat(HTMLTools.extractTag(xml, "Media Voto:&nbsp;&nbsp;&nbsp;</td><td align=\"left\"><b>", "</b>")) * 10;
            movie.addRating(FILMUPIT_PLUGIN_ID, (int) rating);
        } catch (IOException error) {
            logger.error("Failed retreiving rating for : " + movie.getId(FILMUPIT_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = false;
        try {
            String filmUpITId = mediaFile.getId(FILMUPIT_PLUGIN_ID);
            if (StringTools.isNotValidString(filmUpITId)) {
                filmUpITId = getFilmUpITId(mediaFile.getTitle(), mediaFile.getYear());
            }

            // we also get imdb Id for extra infos
            if (StringTools.isNotValidString(mediaFile.getId(IMDB_PLUGIN_ID))) {
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.isTVShow()));
                logger.debug("Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }

            if (StringTools.isValidString(filmUpITId)) {
                mediaFile.setId(FILMUPIT_PLUGIN_ID, filmUpITId);
                if (mediaFile.isTVShow()) {
                    super.scan(mediaFile);
                } else {
                    retval = updateMovieInfo(mediaFile);
                }
            } else {
                // If no FilmUpITId found fallback to Imdb
                logger.debug("No Filmup Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException error) {
            // If no FilmUpITId found fallback to Imdb
            logger.debug("Parse error in FilmUpITPlugin we fall back to ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the FilmUpITId matching the specified movie name. This routine
     * is base on a FilmUpIT search.
     *
     * @throws ParseException
     */
    public String getFilmUpITId(String movieName, String year) throws ParseException {
        String filmUpITId = Movie.UNKNOWN;

        try {
            StringBuilder sb = new StringBuilder("http://filmup.leonardo.it/cgi-bin/search.cgi?ps=10&fmt=long&q=");
            sb.append(URLEncoder.encode(movieName.replace(' ', '+'), "iso-8859-1"));
            sb.append("&ul=%25%2Fsc_%25&x=0&y=0&m=any&wf=0020&wm=wrd&sy=0");
            String xml = webBrowser.request(sb.toString());

            String filmUpITStartResult;
            String filmUpITMediaPrefix;
            filmUpITStartResult = "<DT>1.";
            filmUpITMediaPrefix = "sc_";

            for (String searchResult : extractTags(xml, filmUpITStartResult, "<DD>", filmUpITMediaPrefix, ".htm")) {
                // logger.debug("SearchResult = " + searchResult);
                return searchResult;
            }

            logger.debug("No ID Found with request : " + sb.toString());
            return Movie.UNKNOWN;

        } catch (Exception error) {
            logger.error("Failed to retrieve FilmUp ID for movie : " + movieName);
            logger.error("We fall back to ImdbPlugin");
            throw new ParseException(filmUpITId, 0);
        }
    }

    private String removeHtmlTags(String src) {
        return src.replaceAll("\\<.*?>", "");
    }

    private ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
        ArrayList<String> tags = new ArrayList<String>();
        int index = src.indexOf(sectionStart);
        if (index == -1) {
            // logger.debug("extractTags no sectionStart Tags found");
            return tags;
        }
        index += sectionStart.length();
        int endIndex = src.indexOf(sectionEnd, index);
        if (endIndex == -1) {
            // logger.debug("extractTags no sectionEnd Tags found");
            return tags;
        }

        String sectionText = src.substring(index, endIndex);
        int lastIndex = sectionText.length();
        index = 0;
        int startLen = 0;
        int endLen = endTag.length();

        if (startTag != null) {
            index = sectionText.indexOf(startTag);
            startLen = startTag.length();
        }
        // logger.debug("extractTags sectionText = " + sectionText);
        // logger.debug("extractTags startTag = " + startTag);
        // logger.debug("extractTags startTag index = " + index);
        while (index != -1) {
            index += startLen;
            endIndex = sectionText.indexOf(endTag, index);
            if (endIndex == -1) {
                logger.debug("extractTags no endTag found");
                endIndex = lastIndex;
            }
            String text = sectionText.substring(index, endIndex);
            // logger.debug("extractTags Tag found text = [" + text+"]");

            // replaceAll used because trim() does not trim unicode space
            tags.add(HTMLTools.decodeHtml(text.trim()).replaceAll("^[\\s\\p{Zs}\\p{Zl}\\p{Zp}]*\\b(.*)\\b[\\s\\p{Zs}\\p{Zl}\\p{Zp}]*$", "$1"));
            endIndex += endLen;
            if (endIndex > lastIndex) {
                break;
            }
            if (startTag != null) {
                index = sectionText.indexOf(startTag, endIndex);
            } else {
                index = endIndex;
            }
        }
        return tags;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        boolean result = false;
        // If we use FilmUpIT plugin look for
        // http://www.FilmUpIT.fr/...=XXXXX.html
        logger.debug("Scanning NFO for Filmup Id");
        int beginIndex = nfo.indexOf("http://filmup.leonardo.it/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("sc_", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf('.', beginIdIndex);
                if (endIdIndex != -1) {
                    logger.debug("Filmup Id found in nfo = " + nfo.substring(beginIdIndex + 3, endIdIndex));
                    movie.setId(FilmUpITPlugin.FILMUPIT_PLUGIN_ID, nfo.substring(beginIdIndex + 3, endIdIndex));
                    result = true;
                } else {
                    logger.debug("No Filmup Id found in nfo !");
                }
            } else {
                logger.debug("No Filmup Id found in nfo !");
            }
        } else {
            logger.debug("No Filmup Id found in nfo !");
        }
        return result;
    }
}
