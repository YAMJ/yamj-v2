/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import static com.moviejukebox.model.Movie.UNKNOWN;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.YamjHttpClientBuilder;
import com.omertron.moviemeter.MovieMeterApi;
import com.omertron.moviemeter.MovieMeterException;
import com.omertron.moviemeter.model.Actor;
import com.omertron.moviemeter.model.FilmInfo;
import com.omertron.moviemeter.model.SearchResult;

/**
 * MovieMeter.nl plugin using new API interface
 *
 * @author Stuart.Boston
 */
public class MovieMeterPlugin extends ImdbPlugin {

    public static final String MOVIEMETER_PLUGIN_ID = "moviemeter";
    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPlugin.class);
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
    private static MovieMeterApi api;

    public MovieMeterPlugin() {
        super();

        try {
            api = new MovieMeterApi(API_KEY, YamjHttpClientBuilder.getHttpClient());
        } catch (MovieMeterException ex) {
            LOG.warn("Failed to initialise MovieMeter API: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public String getPluginID() {
        return MOVIEMETER_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String moviemeterId = getMovieId(movie);

        if (!StringUtils.isNumeric(moviemeterId)) {
            LOG.debug("Moviemeter ID '{}' is invalid for: {} ({})", moviemeterId, movie.getTitle(), movie.getYear());
            return Boolean.FALSE;
        }

        LOG.debug("Moviemeter ID available '{}', updating info", moviemeterId);
        return updateMediaInfo(movie, moviemeterId);
    }

    private static boolean updateMediaInfo(Movie movie, String moviemeterId) {
        FilmInfo filmInfo;
        try {
            filmInfo = api.getFilm(NumberUtils.toInt(moviemeterId));
        } catch (MovieMeterException ex) {
            LOG.warn("Failed to get MovieMeter information for ID '{}', error: {}", moviemeterId, ex.getMessage(), ex);
            return false;
        }

        movie.setId(IMDB_PLUGIN_ID, filmInfo.getImdbId());

        if (OverrideTools.checkOverwriteTitle(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setTitle(filmInfo.getDisplayTitle(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setOriginalTitle(filmInfo.getAlternativeTitle(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteYear(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setYear(Integer.toString(filmInfo.getYear()), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwritePlot(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setPlot(filmInfo.getPlot(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteOutline(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setOutline(filmInfo.getPlot(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteRuntime(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setRuntime(Integer.toString(filmInfo.getDuration()), MOVIEMETER_PLUGIN_ID);
        }

        movie.addRating(MOVIEMETER_PLUGIN_ID, (int) (filmInfo.getAverage() * 10f));

        if (OverrideTools.checkOverwriteCountry(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setCountries(filmInfo.getCountries(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteGenres(movie, MOVIEMETER_PLUGIN_ID)) {
            movie.setGenres(filmInfo.getGenres(), MOVIEMETER_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteActors(movie, MOVIEMETER_PLUGIN_ID)) {
            for (Actor a : filmInfo.getActors()) {
                movie.addActor(a.getName(), MOVIEMETER_PLUGIN_ID);
            }
        }

        if (OverrideTools.checkOverwriteDirectors(movie, MOVIEMETER_PLUGIN_ID)) {
            for (String d : filmInfo.getDirectors()) {
                movie.addDirector(d, MOVIEMETER_PLUGIN_ID);
            }
        }

        return true;
    }

    /**
     * Get the ID for the movie
     *
     * @param movie Movie to get the ID for
     * @return The ID, or empty if no idea found
     */
    public String getMovieId(Movie movie) {
        // Try to get the MovieMeter ID from the movie
        String id = movie.getId(MOVIEMETER_PLUGIN_ID);
        if (isValidString(id) && StringUtils.isNumeric(id)) {
            return id;
        }

        // Try to get the MovieMeter ID using the IMDB ID
        id = movie.getId(IMDB_PLUGIN_ID);
        if (isValidString(id)) {
            try {
                // Get the Movie Meter ID using IMDB ID
                FilmInfo mm = api.getFilm(id);
                id = Integer.toString(mm.getId());
                movie.setId(MOVIEMETER_PLUGIN_ID, mm.getId());
                return id;
            } catch (MovieMeterException ex) {
                LOG.warn("Failed to get MovieMeter ID for {}: {}", movie.getBaseName(), ex.getMessage(), ex);
            }
        }

        // Try to get the MovieMeter ID using the title/year
        if (isNotValidString(id)) {
            id = getMovieId(movie.getTitle(), movie.getYear());

            // Try the original title next
            if (isNotValidString(id)) {
                id = getMovieId(movie.getOriginalTitle(), movie.getYear());
            }
        }

        return id;
    }

    /**
     * Get the ID for the movie
     *
     * @param title Movie title to get the ID for
     * @param year Movie year to get the ID for
     * @return The ID, or empty if no idea found
     */
    public String getMovieId(final String title, final String year) {
        String id = UNKNOWN;

        LOG.debug("Looking for MovieMeter ID for {} ({})", title, year);
        List<SearchResult> results;
        try {
            results = api.search(title);
        } catch (MovieMeterException ex) {
            LOG.warn("Failed to get Movie Meter search results for {} ({}): {}", title, year, ex.getMessage(), ex);
            return id;
        }

        if (results.isEmpty()) {
            return id;
        }

        int fYear = NumberUtils.toInt(year, 0);
        double maxMatch = 0.0;

        for (SearchResult sr : results) {
            // if we have a year, check that first
            if (fYear > 0 && sr.getYear() != fYear) {
                continue;
            }

            // Check for best text similarity
            double result = StringUtils.getJaroWinklerDistance(title, sr.getTitle());
            if (result > maxMatch) {
                LOG.trace("Better match found for {} ({}) = {} ({}) [{}]", title, year, sr.getTitle(), sr.getYear(), maxMatch);
                maxMatch = result;
                // Update the best result
                id = Integer.toString(sr.getId());
            }
        }

        if (isValidString(id)) {
            LOG.debug("MovieMeter ID '{}' found for {} ({}), Match confidence: {}", id, title, year, maxMatch);
        }

        return id;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // ID already present
        if (StringTools.isValidString(movie.getId(MOVIEMETER_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        LOG.debug("Scanning NFO for Moviemeter id");
        int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(MOVIEMETER_PLUGIN_ID, st.nextToken());
            LOG.debug("Moviemeter id found in NFO = {}", movie.getId(MOVIEMETER_PLUGIN_ID));
            return Boolean.TRUE;
        }

        LOG.debug("No Moviemeter id found in NFO : {}", movie.getTitle());
        return Boolean.FALSE;
    }
}
