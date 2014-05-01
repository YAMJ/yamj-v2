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
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin to retrieve movie data from Swedish movie database www.filmdelta.se
 *
 * Modified from imdb plugin and Kinopoisk plugin written by Yury Sidorov.
 *
 * Contains code for an alternate plugin for fetching information on movies in Swedish
 *
 * @author johan.klinge
 * @version 0.5, 30th April 2009
 */
public class FilmDeltaSEPlugin extends ImdbPlugin {

    public static final String FILMDELTA_PLUGIN_ID = "filmdelta";
    private static final Logger LOG = LoggerFactory.getLogger(FilmDeltaSEPlugin.class);
    private static final String LOG_MESSAGE = "FilmDeltaSEPlugin: ";
    private static final String SEARCH_RESULT_UNIQUE = "window.location = \"http://www.filmdelta.se/filmer/";
    private static final String SEARCH_RESULT_LINE = "<a href=\"/filmer/";

    private final TheTvDBPlugin tvdb;
    private final SearchEngineTools searchEngines;

    public FilmDeltaSEPlugin() {
        super();
        tvdb = new TheTvDBPlugin();
        searchEngines = new SearchEngineTools();
    }

    @Override
    public String getPluginID() {
        return FILMDELTA_PLUGIN_ID;
    }

    public String getMovieId(Movie movie) {
        String filmdeltaId = movie.getId(FILMDELTA_PLUGIN_ID);
        if (StringTools.isNotValidString(filmdeltaId)) {
            filmdeltaId = getMovieId(movie.getTitle(), movie.getYear(), movie.getSeason());
            movie.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
        }
        return filmdeltaId;
    }

    public String getMovieId(String title, String year) {
        return getMovieId(title, year, -1);
    }

    public String getMovieId(String title, String year, int season) {
        String filmdeltaId = getFilmdeltaIdFromFilmDelta(title, year, season);
        if (StringTools.isNotValidString(filmdeltaId)) {
            filmdeltaId = getFilmdeltaIdFromSearchEngine(title, year, season);
        }
        return filmdeltaId;
    }

    private String getFilmdeltaIdFromFilmDelta(String title, String year, int season) {
        try {
            StringBuilder sb = new StringBuilder("http://www.filmdelta.se/search.php?string=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&type=movie");
            String xml = webBrowser.request(sb.toString());

            int beginIndex = xml.indexOf(SEARCH_RESULT_UNIQUE);
            if (beginIndex > 0) {
                // found a unique match
                beginIndex = beginIndex + SEARCH_RESULT_UNIQUE.length();
                String filmdeltaId = makeFilmdeltaId(xml, beginIndex, 0);

                // regex to match that a valid filmdeltaId contains at least 3 numbers,
                // a dash, and one or more letters (may contain [-&;])
                if (filmdeltaId.matches("\\d{3,}/.+")) {
                    LOG.debug(LOG_MESSAGE + "Found unique FilmDelta id (" + filmdeltaId + ") for title: " + title);
                    return filmdeltaId;
                }
            }

            beginIndex = xml.indexOf("<div class=\"box\" id=\"search-results\">");
            if (beginIndex > 0) {
                boolean searchYear = StringTools.isValidString(year);
                for (String tag : HTMLTools.extractHtmlTags(xml, "<div class=\"box\" id=\"search-results\">", "<form action=\"/googlesearch.php", "<li>", "</li>")) {
                    beginIndex = tag.indexOf(SEARCH_RESULT_LINE);
                    if (beginIndex > -1 && (!searchYear || (tag.contains("(" + year + ")")))) {
                        // found a match
                        beginIndex = beginIndex + SEARCH_RESULT_LINE.length();
                        String filmdeltaId = makeFilmdeltaId(xml, beginIndex, 0);

                        // regex to match that a valid filmdeltaId contains at least 3 numbers,
                        // a dash, and one or more letters (may contain [-&;])
                        if (filmdeltaId.matches("\\d{3,}/.+")) {
                            LOG.debug(LOG_MESSAGE + "Found unique FilmDelta id (" + filmdeltaId + ") for title: " + title);
                            return filmdeltaId;
                        }
                    }
                }
            }

        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving FilmDelta id for title : " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    private String getFilmdeltaIdFromSearchEngine(String title, String year, int season) {
        String url;
        if (season > -1) {
            url = searchEngines.searchMovieURL(title, year, "www.filmdelta.se/filmer", "sasong_" + season);
        } else {
            url = searchEngines.searchMovieURL(title, year, "www.filmdelta.se/filmer");
        }

        if (StringTools.isNotValidString(url)) {
            return Movie.UNKNOWN;
        }

        // we have a valid FilmDelta link
        int beginIndex = url.indexOf("www.filmdelta.se/filmer/") + 24;
        String filmdeltaId = makeFilmdeltaId(url, beginIndex, 0);

        // regex to match that a valid filmdeltaId contains at least 3 numbers,
        // a dash, and one or more letters (may contain [-&;])
        if (filmdeltaId.matches("\\d{3,}/.+")) {
            LOG.debug(LOG_MESSAGE + "FilmDelta id found: " + filmdeltaId);
            return filmdeltaId;
        }

        LOG.info(LOG_MESSAGE + "Found a FilmDelta id but it's not valid: " + filmdeltaId);
        return Movie.UNKNOWN;
    }

    /*
     * Utility method to make a FilmDelta id from a string containing a FilmDelta URL
     */
    private String makeFilmdeltaId(String string, int beginIndex, int skip) {
        StringTokenizer st = new StringTokenizer(string.substring(beginIndex), "/");
        for (int i = 0; i < skip; i++) {
            st.nextToken();
        }
        String result = st.nextToken() + "/" + st.nextToken();
        try {
            result = URLDecoder.decode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warn(LOG_MESSAGE + "Error in makeFilmDeltaId for string : " + string);
            LOG.warn(LOG_MESSAGE + "Error : " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean scan(Movie movie) {

        // if IMDb id is specified in the NFO scan IMDb first
        // (to get a valid movie title and improve detection rate
        // for getMovieId-function)
        boolean imdbScanned = false;
        String imdbId = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (StringTools.isValidString(imdbId)) {
            super.scan(movie);
            imdbScanned = true;
        }

        // get FilmDelta id
        String filmdeltaId = getMovieId(movie);

        // always scrape info from IMDb or TvDb
        boolean retval = false;
        if (movie.isTVShow()) {
            retval = tvdb.scan(movie);
        } else if (!imdbScanned) {
            retval = super.scan(movie);
        }

        // only scrape FilmDelta if a valid FilmDelta id was found
        // and the movie is not a TV show
        if (StringTools.isValidString(filmdeltaId) && !movie.isTVShow()) {
            retval = updateMediaInfo(movie, filmdeltaId);
        }

        return retval;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always scan for IMDb id, look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // ID already present
        if (StringTools.isValidString(movie.getId(FILMDELTA_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        // Format: http://www.filmdelta.se/filmer/<digits>/<movie_name>/
        // or (for upcoming movies)
        // Format: http://www.filmdelta.se/prevsearch/<text>/filmer/<digits>/<movie_name>
        LOG.debug(LOG_MESSAGE + "Scanning NFO for FilmDelta id");

        boolean result = Boolean.TRUE;
        int beginIndex = nfo.indexOf("www.filmdelta.se/prevsearch");
        if (beginIndex != -1) {
            beginIndex = beginIndex + 27;
            String filmdeltaId = makeFilmdeltaId(nfo, beginIndex, 2);
            movie.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
            LOG.debug(LOG_MESSAGE + "FilmDelta id found in NFO = " + movie.getId(FILMDELTA_PLUGIN_ID));
        } else if (nfo.indexOf("www.filmdelta.se/filmer") != -1) {
            beginIndex = nfo.indexOf("www.filmdelta.se/filmer") + 24;
            String filmdeltaId = makeFilmdeltaId(nfo, beginIndex, 0);
            movie.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
            LOG.debug(LOG_MESSAGE + "FilmDelta id found in NFO = " + movie.getId(FILMDELTA_PLUGIN_ID));
        } else {
            LOG.debug(LOG_MESSAGE + "No FilmDelta id found in NFO : " + movie.getTitle());
            result = Boolean.FALSE;
        }
        return result;
    }

    protected boolean updateMediaInfo(Movie movie, String filmdeltaId) {
        try {
            LOG.debug(LOG_MESSAGE + "searchstring: " + "http://www.filmdelta.se/filmer/" + filmdeltaId);
            String html = webBrowser.request("http://www.filmdelta.se/filmer/" + filmdeltaId + "/");

            if (StringTools.isValidString(html)) {
                html = removeIllegalHtmlChars(html);
                getFilmdeltaTitle(movie, html);
                getFilmdeltaPlot(movie, html);
                getFilmdeltaGenres(movie, html);
                getFilmdeltaDirector(movie, html);
                getFilmdeltaCast(movie, html);
                getFilmdeltaCountry(movie, html);
                getFilmdeltaYear(movie, html);
                getFilmdeltaRating(movie, html);
                getFilmdeltaRuntime(movie, html);
                return Boolean.TRUE;
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving media info : " + filmdeltaId);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Boolean.FALSE;
    }

    /*
     * utility method to remove illegal HTML characters from the page that is scraped by the webbrower.request()
     * Ugly as hell, gotta be a better way to do this..
     */
    protected String removeIllegalHtmlChars(String result) {
        String cleanResult = result.replaceAll("\u0093", "&quot;");
        cleanResult = cleanResult.replaceAll("\u0094", "&quot;");
        cleanResult = cleanResult.replaceAll("\u00E4", "&auml;");
        cleanResult = cleanResult.replaceAll("\u00E5", "&aring;");
        cleanResult = cleanResult.replaceAll("\u00F6", "&ouml;");
        cleanResult = cleanResult.replaceAll("\u00C4", "&Auml;");
        cleanResult = cleanResult.replaceAll("\u00C5", "&Aring;");
        cleanResult = cleanResult.replaceAll("\u00D6", "&Ouml;");
        return cleanResult;
    }

    private void getFilmdeltaTitle(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteTitle(movie, FILMDELTA_PLUGIN_ID)) {
            String newTitle = HTMLTools.extractTag(fdeltaHtml, "title>", 0, "<");
            // check if everything is ok
            if (StringTools.isValidString(newTitle)) {
                //split the string so that we get the title at index 0
                String[] titleArray = newTitle.split("-\\sFilmdelta");
                newTitle = titleArray[0];
                LOG.debug(LOG_MESSAGE + "scraped title: " + newTitle);
                movie.setTitle(newTitle.trim(), FILMDELTA_PLUGIN_ID);
            } else {
                LOG.debug(LOG_MESSAGE + "Error scraping title");
            }
        }

        if (OverrideTools.checkOverwriteOriginalTitle(movie, FILMDELTA_PLUGIN_ID)) {
            String originalTitle = HTMLTools.extractTag(fdeltaHtml, "riginaltitel</h4>", 2);
            LOG.debug(LOG_MESSAGE + "scraped original title: " + originalTitle);
            movie.setOriginalTitle(originalTitle, FILMDELTA_PLUGIN_ID);
        }
    }

    protected void getFilmdeltaPlot(Movie movie, String fdeltaHtml) {
        String extracted = HTMLTools.extractTag(fdeltaHtml, "<div class=\"text\">", "</p>");
        //strip remaining html tags
        extracted = HTMLTools.stripTags(extracted);
        if (StringTools.isValidString(extracted)) {

            if (OverrideTools.checkOverwritePlot(movie, FILMDELTA_PLUGIN_ID)) {
                movie.setPlot(extracted, FILMDELTA_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteOutline(movie, FILMDELTA_PLUGIN_ID)) {
                //CJK 2010-09-15 filmdelta.se has no outlines - set outline to same as plot
                movie.setOutline(extracted, FILMDELTA_PLUGIN_ID);
            }
        } else {
            LOG.info(LOG_MESSAGE + "error finding plot for movie: " + movie.getTitle());
        }
    }

    private void getFilmdeltaGenres(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteGenres(movie, FILMDELTA_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<String>();

            List<String> filmdeltaGenres = HTMLTools.extractTags(fdeltaHtml, "<h4>Genre</h4>", "</div>", "<h5>", "</h5>");
            for (String genre : filmdeltaGenres) {
                if (genre.length() > 0) {
                    genre = genre.substring(0, genre.length() - 5);
                    newGenres.add(genre);
                }
            }

            if (!newGenres.isEmpty()) {
                movie.setGenres(newGenres, FILMDELTA_PLUGIN_ID);
                LOG.debug(LOG_MESSAGE + "scraped genres: " + movie.getGenres().toString());
            }
        }
    }

    private void getFilmdeltaDirector(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteDirectors(movie, FILMDELTA_PLUGIN_ID)) {
            List<String> filmdeltaDirectors = HTMLTools.extractTags(fdeltaHtml, "<h4>Regiss&ouml;r</h4>", "</div>", "<h5>", "</h5>");
            StringBuilder newDirector = new StringBuilder();

            if (!filmdeltaDirectors.isEmpty()) {
                for (String dir : filmdeltaDirectors) {
                    dir = dir.substring(0, dir.length() - 4);
                    newDirector.append(dir).append(Movie.SPACE_SLASH_SPACE);
                }

                movie.setDirector(newDirector.substring(0, newDirector.length() - 3), FILMDELTA_PLUGIN_ID);
                LOG.debug(LOG_MESSAGE + "scraped director: " + movie.getDirector());
            }
        }
    }

    private void getFilmdeltaCast(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteActors(movie, FILMDELTA_PLUGIN_ID)) {
            Collection<String> newCast = new ArrayList<String>();

            for (String actor : HTMLTools.extractTags(fdeltaHtml, "<h4>Sk&aring;despelare</h4>", "</div>", "<h5>", "</h5>")) {
                String[] newActor = actor.split("</a>");
                newCast.add(newActor[0]);
            }
            if (newCast.size() > 0) {
                movie.setCast(newCast, FILMDELTA_PLUGIN_ID);
                LOG.debug(LOG_MESSAGE + "scraped actor: " + movie.getCast().toString());
            }
        }
    }

    private void getFilmdeltaCountry(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteCountry(movie, FILMDELTA_PLUGIN_ID)) {
            String country = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 3);
            movie.setCountries(country, FILMDELTA_PLUGIN_ID);
            LOG.debug(LOG_MESSAGE + "scraped country: " + movie.getCountriesAsString());
        }
    }

    private void getFilmdeltaYear(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteYear(movie, FILMDELTA_PLUGIN_ID)) {
            String year = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 5);
            String[] newYear = year.split("\\s");
            if (newYear.length > 1) {
                movie.setYear(newYear[1], FILMDELTA_PLUGIN_ID);
                LOG.debug(LOG_MESSAGE + "scraped year: " + movie.getYear());
            } else {
                LOG.debug(LOG_MESSAGE + "error scraping year for movie: " + movie.getTitle());
            }
        }
    }

    private void getFilmdeltaRating(Movie movie, String fdeltaHtml) {
        String rating = HTMLTools.extractTag(fdeltaHtml, "<h4>Medlemmarna</h4>", 3, "<");
        int newRating;
        // check if valid rating string is found
        if (rating.indexOf("Snitt") != -1) {
            String[] result = rating.split(":");
            rating = result[result.length - 1];
            LOG.debug(LOG_MESSAGE + "filmdelta rating: " + rating);
            // multiply by 20 to make comparable to IMDB-ratings
            newRating = (int) (Float.parseFloat(rating) * 20);
        } else {
            LOG.warn(LOG_MESSAGE + "error finding filmdelta rating for movie " + movie.getTitle());
            return;
        }

        if (newRating != 0) {
            movie.addRating(FILMDELTA_PLUGIN_ID, newRating);
        }
    }

    private void getFilmdeltaRuntime(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteRuntime(movie, FILMDELTA_PLUGIN_ID)) {
            // Run time
            String runtime = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 7);
            String[] newRunTime = runtime.split("\\s");
            if (newRunTime.length > 2) {
                movie.setRuntime(newRunTime[1], FILMDELTA_PLUGIN_ID);
                LOG.debug(LOG_MESSAGE + "scraped runtime: " + movie.getRuntime());
            }
        }
    }
}
