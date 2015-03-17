/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilmaffinityPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FilmaffinityPlugin.class);
    /*
     * Literals of web of each movie info
     */
    private static final String FA_ORIGINAL_TITLE = "<dt>Título original</dt>";
    private static final String FA_YEAR = "<dt>A&ntilde;o</dt>";
    private static final String FA_RUNTIME = "<dt>Duración</dt>";
    private static final String FA_DIRECTOR = "<dt>Director</dt>";
    private static final String FA_WRITER = "<dt>Guión</dt>";
    private static final String FA_CAST = "<dt>Reparto</dt>";
    private static final String FA_GENRE = "<dt>Género</dt>";
    private static final String FA_COMPANY = "<dt>Productora</dt>";
    private static final String FA_PLOT = "<dt>Sinopsis</dt>";
    private final FilmAffinityInfo filmAffinityInfo;

    public FilmaffinityPlugin() {
        super();  // use IMDB if FilmAffinity doesn't know movie
        filmAffinityInfo = new FilmAffinityInfo();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getPluginID() {
        return FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String filmAffinityId = filmAffinityInfo.arrangeId(movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));

        if (StringTools.isNotValidString(filmAffinityId)) {
            filmAffinityId = filmAffinityInfo.getIdFromMovieInfo(movie.getTitle(), movie.getYear(), movie.getSeason());
        }

        if (StringTools.isValidString(filmAffinityId)) {
            movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityId);
        }

        return updateFilmAffinityMediaInfo(movie);
    }

    /**
     * Scan FilmAffinity html page for the specified movie
     */
    private boolean updateFilmAffinityMediaInfo(Movie movie) {
        Boolean returnStatus = true;
        Pattern countryPattern = Pattern.compile("<img src=\"/imgs/countries/[A-Z]{2}\\.jpg\" title=\"([\\w áéíóúñüàè]+)\"");

        String filmAffinityId = movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);

        if (StringTools.isNotValidString(filmAffinityId)) {
            LOG.debug("No valid FilmAffinity ID for movie {}", movie.getBaseName());
            return false;
        }

        try {
            String xml = webBrowser.request("http://www.filmaffinity.com/es/" + filmAffinityId, Charset.forName("UTF-8"));

            if (xml.contains("Serie de TV")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    LOG.debug("{} is a TV Show, skipping.", movie.getTitle());
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (OverrideTools.checkOverwriteTitle(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String spanishTitle = HTMLTools.getTextAfterElem(xml, "<h1 id=\"main-title\">").replaceAll("\\s{2,}", " ");
                movie.setTitle(spanishTitle, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                movie.setOriginalTitle(HTMLTools.getTextAfterElem(xml, FA_ORIGINAL_TITLE), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteYear(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String year = HTMLTools.getTextAfterElem(xml, FA_YEAR);
                // check to see if the year is numeric, if not, try a different approach
                if (!StringUtils.isNumeric(year)) {
                    year = HTMLTools.getTextAfterElem(xml, FA_YEAR, 1);
                }
                movie.setYear(year, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String runTime = HTMLTools.getTextAfterElem(xml, FA_RUNTIME).replace("min.", "m");
                if (!"min.".equals(runTime)) {
                    movie.setRuntime(runTime, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteCountry(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                Matcher countryMatcher = countryPattern.matcher(xml);
                if (countryMatcher.find()) {
                    movie.setCountries(countryMatcher.group(1), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteDirectors(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newDirectors = Arrays.asList(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_DIRECTOR, "</a></dd>")).split(","));
                movie.setDirectors(newDirectors, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            /*
             * Sometimes FilmAffinity includes the writer of novel in the form:
             * screenwriter (Novela: writer) OR screenwriter1, screenwriter2,
             * ... (Novela: writer) OR even!! screenwriter1, screenwriter2, ...
             * (Story: writer1, writer2)
             *
             * The info between parenthesis doesn't add.
             */
            if (OverrideTools.checkOverwriteWriters(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newWriters = Arrays.asList(HTMLTools.getTextAfterElem(xml, FA_WRITER).split("\\(")[0].split(","));
                movie.setWriters(newWriters, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteActors(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newActors = Arrays.asList(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_CAST, "</a></dd>")).split(","));
                movie.setCast(newActors, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                // TODO: Save more than one company.
                movie.setCompany(HTMLTools.getTextAfterElem(xml, FA_COMPANY).split("/")[0].trim(), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteGenres(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<>();
                for (String genre : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_GENRE, "</dd>")).split("\\.|\\|")) {
                    newGenres.add(Library.getIndexingGenre(cleanStringEnding(genre.trim())));
                }
                movie.setGenres(newGenres, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            try {
                movie.addRating(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, (int) (Float.parseFloat(HTMLTools.extractTag(xml, "<div id=\"movie-rat-avg\">", "</div>").replace(",", ".")) * 10));
            } catch (Exception e) {
                // Don't set a rating
            }

            if (OverrideTools.checkOverwritePlot(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String plot = HTMLTools.getTextAfterElem(xml, FA_PLOT);
                if (plot.endsWith("(FILMAFFINITY)")) {
                    plot = plot.substring(0, plot.length() - 14);
                }
                movie.setPlot(plot, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            /*
             * Fill the rest of the fields from IMDB, taking care not to allow
             * the title to get overwritten.
             *
             * I change temporally: title = Original title to improve the chance
             * to find the right movie in IMDb.
             */
            String title = movie.getTitle();
            String titleSource = movie.getOverrideSource(OverrideFlag.TITLE);
            movie.setTitle(movie.getOriginalTitle(), movie.getOverrideSource(OverrideFlag.ORIGINALTITLE));
            super.scan(movie);
            // Change the title back to the way it was
            if (OverrideTools.checkOverwriteTitle(movie, titleSource)) {
                movie.setTitle(title, titleSource);
            }
        } catch (Exception error) {
            LOG.error("Failed retreiving movie info: {}", filmAffinityId);
            LOG.error(SystemTools.getStackTrace(error));
            returnStatus = false;
        }
        return returnStatus;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        Pattern filtroFAiD = Pattern.compile("http://www.filmaffinity.com/es/film([0-9]{6})\\.html|filmaffinity=((?:film)?[0-9]{6}(?:\\.html)?)|<id moviedb=\"filmaffinity\">((?:film)?[0-9]{6}(?:\\.html)?)</id>", Pattern.CASE_INSENSITIVE);
        Matcher nfoMatcher = filtroFAiD.matcher(nfo);

        boolean result = false;
        if (nfoMatcher.find()) {
            if (nfoMatcher.group(1) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(1)));
            } else if (nfoMatcher.group(2) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(2)));
            } else {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(3)));
            }
            result = true;
        }

        // Look for IMDb id
        super.scanNFO(nfo, movie);
        return result;
    }
}
