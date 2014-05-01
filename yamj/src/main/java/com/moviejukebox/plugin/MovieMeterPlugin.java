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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.xmlrpc.XmlRpcException;

/**
 * The MovieMeterPlugin uses the XML-RPC API of www.moviemeter.nl
 * (http://wiki.moviemeter.nl/index.php/API).
 *
 * @author RdeTuinman
 */
public class MovieMeterPlugin extends ImdbPlugin {

    public static final String MOVIEMETER_PLUGIN_ID = "moviemeter";
    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPlugin.class);
    private static final String LOG_MESSAGE = "MovieMeterPlugin: ";

    private MovieMeterPluginSession session;
    private final SearchEngineTools searchEngines;

    public MovieMeterPlugin() {
        super();

        try {
            session = new MovieMeterPluginSession();
        } catch (XmlRpcException error) {
            LOG.error(SystemTools.getStackTrace(error));
        }

        searchEngines = new SearchEngineTools("nl");
    }

    @Override
    public String getPluginID() {
        return MOVIEMETER_PLUGIN_ID;
    }

    public String getMovieId(Movie movie) {
        String moviemeterId = movie.getId(MOVIEMETER_PLUGIN_ID);
        if (!StringUtils.isNumeric(moviemeterId)) {
            moviemeterId = getMovieId(movie.getTitle(), movie.getYear());
            movie.setId(MOVIEMETER_PLUGIN_ID, moviemeterId);
        }
        return moviemeterId;
    }

    @SuppressWarnings("rawtypes")
    public String getMovieId(String title, String year) {

        Map filmInfo = session.getMovieByTitleAndYear(title, year);
        if (filmInfo != null) {
            String moviemeterId = String.valueOf(filmInfo.get("filmId"));
            if (StringUtils.isNumeric(moviemeterId)) {
                return moviemeterId;
            }
        }

        String url = searchEngines.searchMovieURL(title, year, "www.moviemeter.nl/film");

        int beginIndex = url.indexOf("www.moviemeter.nl/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(url.substring(beginIndex + 23), "/\"");
            String moviemeterId = st.nextToken();
            if (StringUtils.isNumeric(moviemeterId)) {
                return moviemeterId;
            }
        }

        return Movie.UNKNOWN;
    }

    @Override
    public boolean scan(Movie movie) {
        String moviemeterId = getMovieId(movie);

        if (!StringUtils.isNumeric(moviemeterId)) {
            LOG.debug(LOG_MESSAGE + "Moviemeter id not available : " + movie.getTitle());
            return Boolean.FALSE;
        }

        LOG.debug(LOG_MESSAGE + "Moviemeter id available (" + moviemeterId + "), updating media info");
        return updateMediaInfo(movie, moviemeterId);
    }

    @SuppressWarnings("rawtypes")
    private boolean updateMediaInfo(Movie movie, String moviemeterId) {
        LOG.debug(LOG_MESSAGE + "Start fetching info from moviemeter.nl: " + moviemeterId);

        try {
            Map filmInfo = session.getMovieDetailsById(Integer.parseInt(moviemeterId));
            if (filmInfo == null || filmInfo.isEmpty()) {
                return Boolean.FALSE;
            }

            movie.setId(MOVIEMETER_PLUGIN_ID, filmInfo.get("filmId").toString());

            if (filmInfo.get("imdb") != null) {
                // if IMDb id contained, then add it to the media file
                movie.setId(IMDB_PLUGIN_ID, "tt" + filmInfo.get("imdb").toString());
                LOG.debug(LOG_MESSAGE + "Fetched imdb id: " + movie.getId(IMDB_PLUGIN_ID));
            }

            Object movieMeterTitle = filmInfo.get("title");
            if (movieMeterTitle != null) {
                if (OverrideTools.checkOverwriteTitle(movie, MOVIEMETER_PLUGIN_ID)) {
                    movie.setTitle(movieMeterTitle.toString(), MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched title: " + movie.getTitle());
                }
                if (OverrideTools.checkOverwriteOriginalTitle(movie, MOVIEMETER_PLUGIN_ID)) {
                    movie.setOriginalTitle(movieMeterTitle.toString(), MOVIEMETER_PLUGIN_ID);
                }
            }

            if (movie.getRating() == -1) {
                if (filmInfo.get("average") != null) {
                    movie.addRating(MOVIEMETER_PLUGIN_ID, Math.round(Float.parseFloat(filmInfo.get("average").toString()) * 20));
                }
                LOG.debug(LOG_MESSAGE + "Fetched rating: " + movie.getRating());
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, MOVIEMETER_PLUGIN_ID)) {
                Object[] dates = (Object[]) filmInfo.get("dates_cinema");
                if (dates != null && dates.length > 0) {
                    Map dateshm = (HashMap) dates[0];
                    movie.setReleaseDate(dateshm.get("date").toString(), MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched releasedate: " + movie.getReleaseDate());
                }
            }

            if (OverrideTools.checkOverwriteRuntime(movie, MOVIEMETER_PLUGIN_ID))  {
                if (filmInfo.get("durations") != null) {
                    Object[] durationsArray = (Object[]) filmInfo.get("durations");
                    if (durationsArray.length > 0) {
                        Map durations = (HashMap) (durationsArray[0]);
                        movie.setRuntime(durations.get("duration").toString(), MOVIEMETER_PLUGIN_ID);
                        LOG.debug(LOG_MESSAGE + "Fetched runtime: " + movie.getRuntime());
                    }
                }
            }

            if (OverrideTools.checkOverwriteCountry(movie, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("countries_text") != null) {
                    movie.setCountries(filmInfo.get("countries_text").toString(), MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched countries: " + movie.getCountries());
                }
            }

            if (OverrideTools.checkOverwriteGenres(movie, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("genres") != null) {
                    Object[] genres = (Object[]) filmInfo.get("genres");
                    List<String> newGenres = new ArrayList<String>();
                    for (Object genre : genres) {
                        newGenres.add(Library.getIndexingGenre(genre.toString()));
                    }
                    movie.setGenres(newGenres, MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched genres: " + movie.getGenres().toString());
                }
            }

            if (OverrideTools.checkOverwritePlot(movie, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("plot") != null) {
                    movie.setPlot(filmInfo.get("plot").toString(), MOVIEMETER_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteYear(movie, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("year") != null) {
                    movie.setYear(filmInfo.get("year").toString(), MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched year: " + movie.getYear());
                }
            }

            if (OverrideTools.checkOverwriteActors(movie, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("actors") != null) {
                    // If no actor is known, false is returned instead of an array
                    // This results in a ClassCastException
                    // So first check the Class, before casting it to an Object array
                    if (filmInfo.get("actors").getClass().equals(Object[].class)) {
                        Object[] actors = (Object[]) filmInfo.get("actors");
                        List<String> newActors = new ArrayList<String>();
                        for (Object actor : actors) {
                            newActors.add((String) (((HashMap) actor).get("name")));
                        }
                        movie.setCast(newActors, MOVIEMETER_PLUGIN_ID);
                        LOG.debug(LOG_MESSAGE + "Fetched actors: " + movie.getCast().toString());
                    }
                }
            }

            if (OverrideTools.checkOverwriteDirectors(movie, MOVIEMETER_PLUGIN_ID)) {
                Object[] directors = (Object[]) filmInfo.get("directors");
                if (directors != null && directors.length > 0) {
                    Map directorshm = (HashMap) directors[0];
                    movie.setDirector(directorshm.get("name").toString(), MOVIEMETER_PLUGIN_ID);
                    LOG.debug(LOG_MESSAGE + "Fetched director: " + movie.getDirector());
                }
            }

            if (downloadFanart && StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (StringTools.isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }

            return Boolean.TRUE;
        } catch (NumberFormatException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving media info : " + moviemeterId);
            LOG.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // ID already present
        if (StringTools.isValidString(movie.getId(MOVIEMETER_PLUGIN_ID))) {
            return Boolean.TRUE;
        }

        LOG.debug(LOG_MESSAGE + "Scanning NFO for Moviemeter id");
        int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(MOVIEMETER_PLUGIN_ID, st.nextToken());
            LOG.debug(LOG_MESSAGE + "Moviemeter id found in NFO = " + movie.getId(MOVIEMETER_PLUGIN_ID));
            return Boolean.TRUE;
        }

        LOG.debug(LOG_MESSAGE + "No Moviemeter id found in NFO : " + movie.getTitle());
        return Boolean.FALSE;
    }
}
