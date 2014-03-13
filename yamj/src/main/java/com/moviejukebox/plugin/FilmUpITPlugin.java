/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilmUpITPlugin extends ImdbPlugin {

    public static final String FILMUPIT_PLUGIN_ID = "filmupit";
    private static final Logger LOG = LoggerFactory.getLogger(FilmUpITPlugin.class);
    private static final String LOG_MESSAGE = "FilmUpITPlugin: ";

    public FilmUpITPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
    }

    @Override
    public String getPluginID() {
        return FILMUPIT_PLUGIN_ID;
    }

    public String getMovieId(Movie movie) {
        String filmUpITId = movie.getId(FILMUPIT_PLUGIN_ID);
        if (StringTools.isNotValidString(filmUpITId)) {
            filmUpITId = getMovieId(movie.getTitle(), movie.getYear());
            movie.setId(FILMUPIT_PLUGIN_ID, filmUpITId);
        }
        return filmUpITId;
    }

    public String getMovieId(String title, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://filmup.leonardo.it/cgi-bin/search.cgi?ps=10&fmt=long&q=");
            sb.append(URLEncoder.encode(title.replace(' ', '+'), "iso-8859-1"));
            sb.append("&ul=%25%2Fsc_%25&x=0&y=0&m=any&wf=0020&wm=wrd&sy=0");
            String xml = webBrowser.request(sb.toString());

            for (String searchResult : HTMLTools.extractTags(xml, "<DT>1.", "<DD>", "sc_", ".htm")) {
                // return first search result
                return searchResult;
            }
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving FilmUpIT id for title : " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    @Override
    public boolean scan(Movie movie) {
        String filmUpITId = getMovieId(movie);

        // we also get IMDb id for extra informations
        if (StringTools.isNotValidString(movie.getId(IMDB_PLUGIN_ID))) {
            movie.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow()));
            LOG.debug("Found imdbId = " + movie.getId(IMDB_PLUGIN_ID));
        }

        if (StringTools.isValidString(filmUpITId)) {
            LOG.debug(LOG_MESSAGE + "FilmUpIT id available (" + filmUpITId + "), updating media info");
            return updateMediaInfo(movie, filmUpITId);
        }

        LOG.debug(LOG_MESSAGE + "FilmUpIT id not available : " + movie.getTitle() + "; fall back to IMDb");
        return super.scan(movie);
    }

    private boolean updateMediaInfo(Movie movie, String filmUpITId) {
        try {
            String xml = webBrowser.request("http://filmup.leonardo.it/sc_" + filmUpITId + ".htm");

            if (OverrideTools.checkOverwriteTitle(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setTitle(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "<font face=\"arial, helvetica\" size=\"3\"><b>", "</b>")), FILMUPIT_PLUGIN_ID);
            }

            // limit plot to FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT char
            if (OverrideTools.checkOverwritePlot(movie, FILMUPIT_PLUGIN_ID)) {
                String tmpPlot = HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "Trama:<br>", "</font><br>"));
                movie.setPlot(tmpPlot, FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteDirectors(movie, FILMUPIT_PLUGIN_ID)) {
                String director = HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml,
                        "Regia:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>"));
                movie.setDirector(director, FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setReleaseDate(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml,
                        "Data di uscita:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setRuntime(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "Durata:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setCountry(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "Nazione:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, FILMUPIT_PLUGIN_ID)) {
                movie.setCompany(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml,
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
                movie.setYear(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "Anno:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                        "</font></td></tr>")), FILMUPIT_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteActors(movie, FILMUPIT_PLUGIN_ID)) {
                List<String> newActors = Arrays.asList(HTMLTools.removeHtmlTags(
                		HTMLTools.extractTag(xml, "Cast:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")).split(","));
                movie.setCast(newActors, FILMUPIT_PLUGIN_ID);
            }

            String opinionsPageID = HTMLTools.extractTag(xml, "/opinioni/op.php?uid=", "\"");
            if (StringTools.isValidString(opinionsPageID)) {
                int pageID = Integer.parseInt(opinionsPageID);
                updateRating(movie, pageID);
                LOG.debug("Opinions page UID = " + pageID);
            }

            if (downloadFanart && StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (StringTools.isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }

            return Boolean.TRUE;
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving media info : " + filmUpITId);
            LOG.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }
    }

    private void updateRating(Movie movie, int opinionsPageID) {
        String baseUrl = "http://filmup.leonardo.it/opinioni/op.php?uid=";
        try {
            String xml = webBrowser.request(baseUrl + opinionsPageID);
            float rating = Float.parseFloat(HTMLTools.extractTag(xml, "Media Voto:&nbsp;&nbsp;&nbsp;</td><td align=\"left\"><b>", "</b>")) * 10;
            movie.addRating(FILMUPIT_PLUGIN_ID, (int) rating);
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retreiving rating : " + movie.getId(FILMUPIT_PLUGIN_ID));
            LOG.error(SystemTools.getStackTrace(error));
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always scan for IMDb id, look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // ID already present
        if (StringTools.isValidString(movie.getId(FILMUPIT_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        LOG.debug(LOG_MESSAGE + "Scanning NFO for FilmUpIT id");

        // If we use FilmUpIT plugIn look for
        // http://www.FilmUpIT.fr/...=XXXXX.html
        int beginIndex = nfo.indexOf("http://filmup.leonardo.it/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("sc_", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf('.', beginIdIndex);
                if (endIdIndex != -1) {
                    LOG.debug(LOG_MESSAGE + "FilmUpIT id found in NFO = " + nfo.substring(beginIdIndex + 3, endIdIndex));
                    movie.setId(FILMUPIT_PLUGIN_ID, nfo.substring(beginIdIndex + 3, endIdIndex));
                    return Boolean.TRUE;
                }
            }
        }

        LOG.debug(LOG_MESSAGE + "No FilmUpIT id found in NFO : " + movie.getTitle());
        return Boolean.FALSE;
    }
}
