/*
 *      Copyright (c) 2004-2016 YAMJ Members
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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

public class FilmwebPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FilmwebPlugin.class);
    private static final String LOG_ERROR = "Error: {}";
    public static final String FILMWEB_PLUGIN_ID = "filmweb";
    private static final Pattern NFO_PATTERN = Pattern.compile("http://[^\"/?&]*filmweb.pl[^\\s<>`\"\\[\\]]*");
    private SearchEngineTools searchEngineTools;
    // TOP 250 pattern  match
    private static final Pattern P_TOP250 = Pattern.compile(".*?#(\\d*).*?");
    private static final Pattern P_RUNTIME = Pattern.compile("duration:.(\\d*)");
    private static final Pattern P_YEAR = Pattern.compile("year:.*?(\\d*)");

    public FilmwebPlugin() {
        // use IMDB if filmweb doesn't know movie
        super();
        init();
    }

    @Override
    public String getPluginID() {
        return FILMWEB_PLUGIN_ID;
    }

    public void init() {
        searchEngineTools = new SearchEngineTools("pl");

        try {
            // first request to filmweb site to skip welcome screen with ad banner
            httpClient.request("http://www.filmweb.pl");
        } catch (IOException error) {
            LOG.error(LOG_ERROR, error.getMessage(), error);
        }
    }

    public String getMovieId(Movie movie) {
        String filmwebUrl = movie.getId(FILMWEB_PLUGIN_ID);
        if (StringTools.isNotValidString(filmwebUrl)) {
            filmwebUrl = getMovieId(movie.getTitle(), movie.getYear());
            movie.setId(FILMWEB_PLUGIN_ID, filmwebUrl);
        }
        return filmwebUrl;
    }

    public String getMovieId(String title, String year) {
        // try with filmweb search
        String filmwebUrl = getFilmwebUrlFromFilmweb(title, year);
        if (StringTools.isNotValidString(filmwebUrl)) {
            // try with search engines
            filmwebUrl = searchEngineTools.searchMovieURL(title, year, "www.filmweb.pl");
        }
        return filmwebUrl;
    }

    private String getFilmwebUrlFromFilmweb(String title, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.filmweb.pl/search?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("&startYear=").append(year).append("&endYear=").append(year);
            }
            String xml = httpClient.request(sb.toString());

            List<String> tags = HTMLTools.extractTags(xml, "<ul class=\"sep-hr resultsList\">", "</ul>", "<li>", "</li>");
            for (String tag : tags) {
                int beginIndex = tag.indexOf("<a class=\"hdr hdr-medium");
                if (beginIndex > 0) {
                    beginIndex = tag.indexOf("href=\"", beginIndex);
                    String href = tag.substring(beginIndex + 6, tag.indexOf("\"", beginIndex + 6));
                    return "http://www.filmweb.pl" + href;
                }
            }
        } catch (IOException error) {
            LOG.error("Failed retrieving Filmweb url for title : {}", title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    @Override
    public boolean scan(Movie movie) {
        String filmwebUrl = getMovieId(movie);

        boolean retval;
        if (StringTools.isValidString(filmwebUrl)) {
            retval = updateMediaInfo(movie, filmwebUrl);
        } else {
            // use IMDB if filmweb doesn't know movie
            retval = super.scan(movie);
        }
        return retval;
    }

    /**
     * Get a page from FilmWeb or return blank if failed
     *
     * @param filmwebUrl
     * @return
     */
    private String getPage(String filmwebUrl) {
        String xmlPage = "";
        try {
            xmlPage = httpClient.request(filmwebUrl);
        } catch (IOException error) {
            LOG.error("Failed retreiving filmweb informations for URL: {}", filmwebUrl);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return xmlPage;
    }

    /**
     * Scan web page for the specified movie
     *
     * @param movie
     * @param filmwebUrl
     * @return
     */
    protected boolean updateMediaInfo(Movie movie, String filmwebUrl) {

        boolean returnValue = Boolean.TRUE;

        String xml = getPage(filmwebUrl);
        if (StringUtils.isBlank(xml)) {
            return Boolean.FALSE;
        }

        if (HTMLTools.extractTag(xml, "<title>").contains("Serial") && !movie.isTVShow()) {
            movie.setMovieType(Movie.TYPE_TVSHOW);
            return Boolean.FALSE;
        }

        if (OverrideTools.checkOverwriteTitle(movie, FILMWEB_PLUGIN_ID)) {
            movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()></"), FILMWEB_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(movie, FILMWEB_PLUGIN_ID)) {
            String metaTitle = HTMLTools.extractTag(xml, "og:title", ">");
            if (metaTitle.contains("/")) {
                String originalTitle = HTMLTools.extractTag(metaTitle, "/", 0, "()><\"");
                if (originalTitle.endsWith(", The")) {
                    originalTitle = "The " + originalTitle.substring(0, originalTitle.length() - 5);
                }
                movie.setOriginalTitle(originalTitle, FILMWEB_PLUGIN_ID);
            }
        }

        if (movie.getRating() == -1) {
            movie.addRating(FILMWEB_PLUGIN_ID, StringTools.parseRating(HTMLTools.getTextAfterElem(xml, "average")));
        }

        // TOP250
        if (OverrideTools.checkOverwriteTop250(movie, FILMWEB_PLUGIN_ID)) {
            String top250 = HTMLTools.getTextAfterElem(xml, "<a class=worldRanking");
            Matcher m = P_TOP250.matcher(top250);
            if (m.find()) {
                movie.setTop250(m.group(1), FILMWEB_PLUGIN_ID);
            }
        }

        if (OverrideTools.checkOverwriteReleaseDate(movie, FILMWEB_PLUGIN_ID)) {
            movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "filmPremiereWorld"), FILMWEB_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteRuntime(movie, FILMWEB_PLUGIN_ID)) {
            Matcher m = P_RUNTIME.matcher(xml);
            if (m.find()) {
                int intRuntime = DateTimeTools.processRuntime(m.group(1));
                if (intRuntime > 0) {
                    movie.setRuntime(String.valueOf(intRuntime), FILMWEB_PLUGIN_ID);
                }
            }
        }

        if (OverrideTools.checkOverwriteCountry(movie, FILMWEB_PLUGIN_ID)) {
            List<String> countries = HTMLTools.extractTags(xml, "produkcja:", "</tr", "<a ", "</a>");
            movie.setCountries(countries, FILMWEB_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteGenres(movie, FILMWEB_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<>();
            for (String genre : HTMLTools.extractTags(xml, "gatunek:", "produkcja:", "<a ", "</a>")) {
                newGenres.add(Library.getIndexingGenre(genre));
            }
            movie.setGenres(newGenres, FILMWEB_PLUGIN_ID);
        }

        String plot = HTMLTools.extractTag(xml, "v:summary\">", "</p>");
        if (StringTools.isValidString(plot)) {
            int buttonIndex = plot.indexOf("<button");
            if (buttonIndex > 0) {
                plot = plot.substring(0, buttonIndex);
            }
            plot = HTMLTools.removeHtmlTags(plot);

            if (OverrideTools.checkOverwritePlot(movie, FILMWEB_PLUGIN_ID)) {
                movie.setPlot(plot, FILMWEB_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteOutline(movie, FILMWEB_PLUGIN_ID)) {
                movie.setOutline(plot, FILMWEB_PLUGIN_ID);
            }
        }

        if (OverrideTools.checkOverwriteYear(movie, FILMWEB_PLUGIN_ID)) {
            Matcher m = P_YEAR.matcher(xml);
            if (m.find()) {
                movie.setYear(m.group(1), FILMWEB_PLUGIN_ID);
            }
        }

        // Scan cast
        xml = getPage(filmwebUrl + "/cast/actors");
        if (StringUtils.isNotBlank(xml) && !scanCastInformation(movie, xml)) {
            returnValue = Boolean.FALSE;
        }

        // Scan crew
        xml = getPage(filmwebUrl + "/cast/crew");
        if (StringUtils.isNotBlank(xml) && !scanCrewInformation(movie, xml)) {
            returnValue = Boolean.FALSE;
        }

        // scan TV show titles
        if (movie.isTVShow()) {
            scanTVShowTitles(movie);
        }

        if (downloadFanart
                && StringTools.isNotValidString(movie.getFanartURL())) {
            movie.setFanartURL(getFanartURL(movie));
            if (StringTools.isValidString(movie.getFanartURL())) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
            }
        }

        return returnValue;
    }

    /**
     * Get the actor information from the /cast/actors page
     *
     * @param movie
     * @param xml
     * @return
     */
    private static boolean scanCastInformation(Movie movie, String xmlCast) {
        boolean overrideNormal = OverrideTools.checkOverwriteActors(movie, FILMWEB_PLUGIN_ID);
        boolean overridePeople = OverrideTools.checkOverwritePeopleActors(movie, FILMWEB_PLUGIN_ID);

        if (overrideNormal || overridePeople) {
            List<String> actors = new ArrayList<>();

            List<String> tags = HTMLTools.extractHtmlTags(xmlCast, "<table class=filmCast>", "</table>", "<tr id=", "</tr>");
            for (String tag : tags) {
                String actor = HTMLTools.getTextAfterElem(tag, "<a href=\"/person/");
                if (StringTools.isValidString(actor)) {
                    actors.add(actor);
                }
            }

            if (overrideNormal) {
                movie.setCast(actors, FILMWEB_PLUGIN_ID);
            }
            if (overridePeople) {
                movie.setPeopleCast(actors, FILMWEB_PLUGIN_ID);
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Get the crew information from the /cast/crew page
     *
     * @param movie
     * @param xml
     * @return
     */
    private static boolean scanCrewInformation(Movie movie, String xmlCrew) {
        boolean overrideNormal = OverrideTools.checkOverwriteDirectors(movie, FILMWEB_PLUGIN_ID);
        boolean overridePeople = OverrideTools.checkOverwritePeopleDirectors(movie, FILMWEB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            List<String> directors = new ArrayList<>();

            List<String> tags = HTMLTools.extractHtmlTags(xmlCrew, "<h2 class=\"hdr hdr-big\">reżyser", "</table>", "<tr id=", "</tr>");
            for (String tag : tags) {
                String director = HTMLTools.getTextAfterElem(tag, "<a href=\"/person/");
                if (StringTools.isValidString(director)) {
                    directors.add(director);
                }
            }

            if (overrideNormal) {
                movie.setDirectors(directors, FILMWEB_PLUGIN_ID);
            }
            if (overridePeople) {
                movie.setPeopleDirectors(directors, FILMWEB_PLUGIN_ID);
            }
        }

        overrideNormal = OverrideTools.checkOverwriteWriters(movie, FILMWEB_PLUGIN_ID);
        overridePeople = OverrideTools.checkOverwritePeopleWriters(movie, FILMWEB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            List<String> writers = new ArrayList<>();

            List<String> tags = HTMLTools.extractHtmlTags(xmlCrew, "<h2 class=\"hdr hdr-big\">scenariusz", "</table>", "<tr id=", "</tr>");
            for (String tag : tags) {
                String writer = HTMLTools.getTextAfterElem(tag, "<a href=\"/person/");
                if (StringTools.isValidString(writer)) {
                    writers.add(writer);
                }
            }

            if (overrideNormal) {
                movie.setWriters(writers, FILMWEB_PLUGIN_ID);
            }
            if (overridePeople) {
                movie.setWriters(writers, FILMWEB_PLUGIN_ID);
            }
        }

        return Boolean.TRUE;
    }

    private String updateImdbId(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (StringTools.isNotValidString(imdbId)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
            return;
        }
        String filmwebUrl = movie.getId(FILMWEB_PLUGIN_ID);

        if (StringTools.isNotValidString(filmwebUrl)) {
            // use IMDB if filmweb doesn't know episodes titles
            super.scanTVShowTitles(movie);
            return;
        }

        try {
            String xml = httpClient.request(filmwebUrl + "/episodes");
            boolean found = Boolean.FALSE;
            boolean wasSeraching = Boolean.FALSE;
            for (MovieFile file : movie.getMovieFiles()) {

                int fromIndex = xml.indexOf("sezon " + movie.getSeason() + "<");

                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {

                    if (OverrideTools.checkOverwriteEpisodeTitle(file, part, FILMWEB_PLUGIN_ID)) {
                        wasSeraching = Boolean.TRUE;
                        String episodeName = HTMLTools.getTextAfterElem(xml, "odcinek&nbsp;" + part, 4, fromIndex);
                        if (StringTools.isValidString(episodeName)) {
                            found = Boolean.TRUE;
                            file.setTitle(part, episodeName, FILMWEB_PLUGIN_ID);
                        }
                    }
                }
            }

            if (wasSeraching && !found) {
                // use IMDB if filmweb doesn't know episodes titles
                updateImdbId(movie);
                super.scanTVShowTitles(movie);
            }
        } catch (IOException error) {
            LOG.error("Failed retreiving episodes titles for movie : {}", movie.getTitle());
            LOG.error(LOG_ERROR, error.getMessage(), error);
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always scan for IMDb id, look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // ID already present
        if (StringTools.isValidString(movie.getId(FILMWEB_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        LOG.debug("Scanning NFO for filmweb url");
        Matcher m = NFO_PATTERN.matcher(nfo);
        while (m.find()) {
            String url = m.group();
            // Check to see that the URL isn't a picture
            if (!StringUtils.endsWithAny(url, new String[]{".jpg", ".jpeg", ".gif", ".png", ".bmp"})) {
                movie.setId(FILMWEB_PLUGIN_ID, url);
                LOG.debug("Filmweb url found in NFO = {}", url);
                return Boolean.TRUE;
            }
        }

        LOG.debug("No filmweb url found in NFO");
        return Boolean.FALSE;
    }
}
