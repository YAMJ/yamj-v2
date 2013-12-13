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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class FilmwebPlugin extends ImdbPlugin {

    private static final Logger LOG = Logger.getLogger(FilmwebPlugin.class);
    private static final String LOG_MESSAGE = "FilmwebPlugin: ";
    public static final String FILMWEB_PLUGIN_ID = "filmweb";
    private static final Pattern NFO_PATTERN = Pattern.compile("http://[^\"/?&]*filmweb.pl[^\\s<>`\"\\[\\]]*");
    private SearchEngineTools searchEngineTools;

    public FilmwebPlugin() {
        super(); // use IMDB if filmweb doesn't know movie
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
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
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
            String xml = webBrowser.request(sb.toString());

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
            LOG.error(LOG_MESSAGE + "Failed retrieving Filmweb url for title : " + title);
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
     * Scan web page for the specified movie
     *
     * @param movie
     * @param filmwebUrl
     * @return
     */
    protected boolean updateMediaInfo(Movie movie, String filmwebUrl) {

        boolean returnValue = Boolean.TRUE;

        try {
            String xml = webBrowser.request(filmwebUrl);

            if (HTMLTools.extractTag(xml, "<title>").contains("Serial") && !movie.isTVShow()) {
                movie.setMovieType(Movie.TYPE_TVSHOW);
                return Boolean.FALSE;
            }

            if (OverrideTools.checkOverwriteTitle(movie, FILMWEB_PLUGIN_ID)) {
                movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()></"), FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteOriginalTitle(movie, FILMWEB_PLUGIN_ID)) {
                String metaTitle = HTMLTools.extractTag(xml, "og:title", "\">");
                if (metaTitle.contains("/")) {
                    String originalTitle = HTMLTools.extractTag(metaTitle, "/", 0, "()><");
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
                movie.setTop250(HTMLTools.extractTag(xml, "worldRanking", 0, ">."), FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, FILMWEB_PLUGIN_ID)) {
                movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "filmPremiereWorld"), FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FILMWEB_PLUGIN_ID)) {
                String runtime = HTMLTools.getTextAfterElem(xml, "<div class=filmTime>");
                int intRuntime = DateTimeTools.processRuntime(runtime);
                if (intRuntime > 0) {
                    movie.setRuntime(String.valueOf(intRuntime), FILMWEB_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteCountry(movie, FILMWEB_PLUGIN_ID)) {
                String country = StringUtils.join(HTMLTools.extractTags(xml, "produkcja:", "</tr", "<a ", "</a>"), ", ");
                if (country.endsWith(", ")) {
                    movie.setCountry(country.substring(0, country.length() - 2), FILMWEB_PLUGIN_ID);
                } else {
                    movie.setCountry(country, FILMWEB_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteGenres(movie, FILMWEB_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<String>();
                for (String genre : HTMLTools.extractTags(xml, "gatunek:", "produkcja:", "<a ", "</a>")) {
                    newGenres.add(Library.getIndexingGenre(genre));
                }
                movie.setGenres(newGenres, FILMWEB_PLUGIN_ID);
            }

            String plot = HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "v:summary\">", "</span>"));
            if (StringTools.isValidString(plot)) {
                if (OverrideTools.checkOverwritePlot(movie, FILMWEB_PLUGIN_ID)) {
                    movie.setPlot(plot, FILMWEB_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwriteOutline(movie, FILMWEB_PLUGIN_ID)) {
                    movie.setOutline(plot, FILMWEB_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteYear(movie, FILMWEB_PLUGIN_ID)) {
                String year = HTMLTools.getTextAfterElem(xml, "filmYear");
                if (!Movie.UNKNOWN.equals(year)) {
                    year = year.replaceAll("[^0-9]", "");
                }
                movie.setYear(year, FILMWEB_PLUGIN_ID);
            }

            // scan cast/director/writers
            if (!scanPersonInformations(movie)) {
                returnValue = Boolean.FALSE;
            }

            // scan TV show titles
            if (movie.isTVShow()) {
                scanTVShowTitles(movie);
            }

            if (downloadFanart && StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (StringTools.isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }

        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retreiving filmweb informations for movie : " + movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
            LOG.error(SystemTools.getStackTrace(error));
            returnValue = Boolean.FALSE;
        }

        return returnValue;
    }

    private boolean scanPersonInformations(Movie movie) {
        try {
            String xml = webBrowser.request(movie.getId(FILMWEB_PLUGIN_ID) + "/cast");

            boolean overrideNormal = OverrideTools.checkOverwriteDirectors(movie, FILMWEB_PLUGIN_ID);
            boolean overridePeople = OverrideTools.checkOverwritePeopleDirectors(movie, FILMWEB_PLUGIN_ID);
            if (overrideNormal || overridePeople) {
                List<String> directors = new ArrayList<String>();

                List<String> tags = HTMLTools.extractHtmlTags(xml, "<dt id=role-director>", "</dd>", "<li>", "</li>");
                for (String tag : tags) {
                    directors.add(HTMLTools.getTextAfterElem(tag, "<span class=personName>"));
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
                List<String> writers = new ArrayList<String>();

                List<String> tags = HTMLTools.extractHtmlTags(xml, "<dt id=role-screenwriter", "</dd>", "<li>", "</li>");
                for (String tag : tags) {
                    writers.add(HTMLTools.getTextAfterElem(tag, "<span class=personName>"));
                }

                if (overrideNormal) {
                    movie.setWriters(writers, FILMWEB_PLUGIN_ID);
                }
                if (overridePeople) {
                    movie.setWriters(writers, FILMWEB_PLUGIN_ID);
                }
            }

            overrideNormal = OverrideTools.checkOverwriteActors(movie, FILMWEB_PLUGIN_ID);
            overridePeople = OverrideTools.checkOverwritePeopleActors(movie, FILMWEB_PLUGIN_ID);
            if (overrideNormal || overridePeople) {
                List<String> actors = new ArrayList<String>();

                List<String> tags = HTMLTools.extractHtmlTags(xml, "<dt id=role-actors", "</dd>", "<li>", "</li>");
                for (String tag : tags) {
                    actors.add(HTMLTools.getTextAfterElem(tag, "<span class=personName>"));
                }

                if (overrideNormal) {
                    movie.setCast(actors, FILMWEB_PLUGIN_ID);
                }
                if (overridePeople) {
                    movie.setPeopleCast(actors, FILMWEB_PLUGIN_ID);
                }
            }

            return Boolean.TRUE;
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retreiving person informations for movie : " + movie.getTitle());
            LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Boolean.FALSE;
        }
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
            String xml = webBrowser.request(filmwebUrl + "/episodes");
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
            LOG.error(LOG_MESSAGE + "Failed retreiving episodes titles for movie : " + movie.getTitle());
            LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
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

        LOG.debug(LOG_MESSAGE + "Scanning NFO for filmweb url");
        Matcher m = NFO_PATTERN.matcher(nfo);
        while (m.find()) {
            String url = m.group();
            if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
                movie.setId(FILMWEB_PLUGIN_ID, url);
                LOG.debug(LOG_MESSAGE + "Filmweb url found in NFO = " + url);
                return Boolean.TRUE;
            }
        }

        LOG.debug(LOG_MESSAGE + "No filmweb url found in NFO");
        return Boolean.FALSE;
    }
}
