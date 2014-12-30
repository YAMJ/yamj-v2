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

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;

import com.moviejukebox.allocine.AllocineApi;
import com.moviejukebox.allocine.model.*;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.*;
import com.moviejukebox.tools.cache.CacheMemory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocinePlugin extends ImdbPlugin {

    public static final String ALLOCINE_PLUGIN_ID = "allocine";
    private static final Logger LOG = LoggerFactory.getLogger(AllocinePlugin.class);
    private static final String LOG_MESSAGE = "AllocinePlugin: ";
    private static final String CACHE_SEARCH_MOVIE = "AllocineSearchMovie";
    private static final String CACHE_SEARCH_SERIES = "AllocineSearchSeries";
    private static final String CACHE_MOVIE = "AllocineMovie";
    private static final String CACHE_SERIES = "AllocineSeries";
    private static final String API_KEY_PARTNER = PropertiesUtil.getProperty("API_KEY_Allocine_Partner");
    private static final String API_KEY_SECRET = PropertiesUtil.getProperty("API_KEY_Allocine_Secret");
    private AllocineApi allocineApi;
    private SearchEngineTools searchEngines;
    private boolean includeVideoImages;
    private TheTvDBPlugin tvdb = null;

    public AllocinePlugin() {
        super();

        allocineApi = new AllocineApi(API_KEY_PARTNER, API_KEY_SECRET);
        allocineApi.setProxy(WebBrowser.getMjbProxy(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        searchEngines = new SearchEngineTools("fr");

        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "France");
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", Boolean.FALSE);
    }

    @Override
    public String getPluginID() {
        return ALLOCINE_PLUGIN_ID;
    }

    public String getMovieId(Movie movie) {
        String allocineId = movie.getId(ALLOCINE_PLUGIN_ID);
        if (StringTools.isNotValidString(allocineId)) {
            allocineId = getMovieId(movie.getTitle(), movie.getYear(), movie.getSeason());
            movie.setId(ALLOCINE_PLUGIN_ID, allocineId);
        }
        return allocineId;
    }

    public String getMovieId(String title, String year, int season) {
        String allocineId = Movie.UNKNOWN;
        try {
            if (season > -1) {
                allocineId = getAllocineSerieId(title, year);
            } else {
                allocineId = getAllocineMovieId(title, year);
            }
            if (isNotValidString(allocineId)) {
                // try to find the id with search engine
                allocineId = getAllocineIdFromSearchEngine(title, year, season);
            }
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed to retrieve Allocine id for movie : " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return allocineId;
    }

    private String getAllocineSerieId(String title, String year) throws Exception {
        String cacheKey = CacheMemory.generateCacheKey(CACHE_SEARCH_SERIES, title);
        Search search = (Search) CacheMemory.getFromCache(cacheKey);
        if (search == null) {
            search = allocineApi.searchTvSeries(title);
            // Add to the cache
            CacheMemory.addToCache(cacheKey, search);
        }
        
        if (!search.isValid()) {
            return Movie.UNKNOWN;
        }
        
        // If we have a valid year try to find the first serie that match
        if (search.getTotalResults() > 1 && isValidString(year)) {
            int yearSerie = NumberUtils.toInt(year, -1);
            for (TvSeries serie : search.getTvSeries()) {
                if (serie != null) {
                    int serieStart = serie.getYearStart();
                    if (serieStart <= 0) {
                        continue;
                    }
                    int serieEnd = serie.getYearEnd();
                    if (serieEnd <= 0) {
                        serieEnd = serieStart;
                    }
                    if (yearSerie >= serieStart && yearSerie <= serieEnd) {
                        return String.valueOf(serie.getCode());
                    }
                }
            }
        }
        
        // We don't find a serie or there only one result, return the first
        List<TvSeries> serieList = search.getTvSeries();
        if (!serieList.isEmpty()) {
            TvSeries serie = serieList.get(0);
            if (serie != null) {
                return String.valueOf(serie.getCode());
            }
        }
        
        return Movie.UNKNOWN;
    }

    private String getAllocineMovieId(String title, String year) throws Exception {
        String cacheKey = CacheMemory.generateCacheKey(CACHE_SEARCH_MOVIE, title);
        Search search = (Search) CacheMemory.getFromCache(cacheKey);
        if (search == null) {
            search = allocineApi.searchMovies(title);
            // Add to the cache
            CacheMemory.addToCache(cacheKey, search);
        }
        
        if (!search.isValid()) {
            return Movie.UNKNOWN;
        }

        // If we have a valid year try to find the first movie that match
        if (search.getTotalResults() > 1 && isValidString(year)) {
            int yearMovie = NumberUtils.toInt(year, -1);
            for (com.moviejukebox.allocine.model.Movie movie : search.getMovies()) {
                if (movie != null) {
                    int movieProductionYear = movie.getProductionYear();
                    if (movieProductionYear <= 0) {
                        continue;
                    }
                    if (movieProductionYear == yearMovie) {
                        return String.valueOf(movie.getCode());
                    }
                }
            }
        }
        // We don't find a movie or there only one result, return the first
        if (!search.getMovies().isEmpty()) {
            com.moviejukebox.allocine.model.Movie movie = search.getMovies().get(0);
            if (movie != null) {
                return String.valueOf(movie.getCode());
            }
        }

        return Movie.UNKNOWN;
    }

    private String getAllocineIdFromSearchEngine(String title, String year, int season) {
        String allocineId;
        if (season == -1) {
            // movie
            searchEngines.setSearchSuffix("/fichefilm_gen_cfilm");
            String url = searchEngines.searchMovieURL(title, year, "www.allocine.fr/film");
            allocineId = HTMLTools.extractTag(url, "fichefilm_gen_cfilm=", ".html");
        } else {
            // TV show
            searchEngines.setSearchSuffix("/ficheserie_gen_cserie");
            String url = searchEngines.searchMovieURL(title, year, "www.allocine.fr/series");
            allocineId = HTMLTools.extractTag(url, "ficheserie_gen_cserie=", ".html");
        }
        return allocineId;
    }

    @Override
    public boolean scan(Movie movie) {
        String allocineId = getMovieId(movie);

        // we also get imdb Id for extra infos
        if (isNotValidString(movie.getId(IMDB_PLUGIN_ID))) {
            movie.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(movie.getOriginalTitle(), movie.getYear(), movie.isTVShow()));
            LOG.debug(LOG_MESSAGE + "Found imdbId = " + movie.getId(IMDB_PLUGIN_ID));
        }

        boolean retval;
        if (isValidString(allocineId)) {
            if (movie.isTVShow()) {
                updateTVShowInfo(movie);
                retval = true;
            } else {
                retval = updateMovieInfo(movie, allocineId);
            }
        } else {
            // If no AllocineId found fallback to Imdb
            LOG.debug(LOG_MESSAGE + "No Allocine id available, we fall back to ImdbPlugin");
            retval = super.scan(movie);
        }
        return retval;
    }

    private boolean updateMovieInfo(Movie movie, String allocineId) {

        MovieInfos movieInfos = getMovieInfos(allocineId);
        if (movieInfos == null) {
            LOG.error(LOG_MESSAGE + "Can't find informations for movie with id: " + allocineId);
            return false;
        }

        // Check Title
        if (OverrideTools.checkOverwriteTitle(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setTitle(movieInfos.getTitle(), ALLOCINE_PLUGIN_ID);
        }

        // Check OriginalTitle
        if (OverrideTools.checkOverwriteOriginalTitle(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setOriginalTitle(movieInfos.getOriginalTitle(), ALLOCINE_PLUGIN_ID);
        }

        // Check Rating
        if (movie.getRating() == -1) {
            int rating = movieInfos.getUserRating();
            if (rating >= 0) {
                movie.addRating(ALLOCINE_PLUGIN_ID, rating);
            }
        }

        // Check Year
        if (OverrideTools.checkOverwriteYear(movie, ALLOCINE_PLUGIN_ID)) {
            if (movieInfos.getProductionYear() > 0) {
                movie.setYear(String.valueOf(movieInfos.getProductionYear()), ALLOCINE_PLUGIN_ID);
            }
        }

        // Check Plot
        if (OverrideTools.checkOverwritePlot(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setPlot(movieInfos.getSynopsis(), ALLOCINE_PLUGIN_ID);
        }

        // Check Outline
        if (OverrideTools.checkOverwriteOutline(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setOutline(movieInfos.getSynopsisShort(), ALLOCINE_PLUGIN_ID);
        }

        // Check ReleaseDate
        if (OverrideTools.checkOverwriteReleaseDate(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setReleaseDate(movieInfos.getReleaseDate(), ALLOCINE_PLUGIN_ID);
        }
        
        // Check Company
        if (OverrideTools.checkOverwriteCompany(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setCompany(movieInfos.getDistributor(), ALLOCINE_PLUGIN_ID);
        }

        // Check Runtime
        if (OverrideTools.checkOverwriteRuntime(movie, ALLOCINE_PLUGIN_ID)) {
            int runtime = movieInfos.getRuntime();
            if (runtime > 0) {
                movie.setRuntime(DateTimeTools.formatDuration(runtime), ALLOCINE_PLUGIN_ID);
            }
        }

        // Check country
        if (OverrideTools.checkOverwriteCountry(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setCountries(movieInfos.getNationalities(), ALLOCINE_PLUGIN_ID);
        }

        // Check Genres
        if (OverrideTools.checkOverwriteGenres(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setGenres(movieInfos.getGenres(), ALLOCINE_PLUGIN_ID);
        }

        // Check certification
        if (OverrideTools.checkOverwriteCertification(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setCertification(movieInfos.getCertification(), ALLOCINE_PLUGIN_ID);
        }

        // Directors
        if (!movieInfos.getDirectors().isEmpty()) {
            Set<String> directors = new HashSet<String>();
            for (MoviePerson person : movieInfos.getDirectors()) {
                directors.add(person.getName());
            }
            if (OverrideTools.checkOverwriteDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setDirectors(directors, ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPeopleDirectors(directors, ALLOCINE_PLUGIN_ID);
            }
        }
            
        // Writers
        if (!movieInfos.getWriters().isEmpty()) {
            Set<String> writers = new HashSet<String>();
            for (MoviePerson person : movieInfos.getWriters()) {
                writers.add(person.getName());
            }
            if (OverrideTools.checkOverwriteWriters(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setWriters(writers, ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleWriters(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPeopleWriters(writers, ALLOCINE_PLUGIN_ID);
            }
        }
        
        // Actors
        if (!movieInfos.getActors().isEmpty()) {
            if (OverrideTools.checkOverwriteActors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.clearCast();
                for (MoviePerson person : movieInfos.getActors()) {
                    movie.addActor(person.getName(), ALLOCINE_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwritePeopleActors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.clearPeopleCast();
                int count = 0;
                for (MoviePerson person : movieInfos.getActors()) {
                    if (movie.addActor(Movie.UNKNOWN, person.getName(), person.getRole(), Movie.UNKNOWN, Movie.UNKNOWN, ALLOCINE_PLUGIN_ID)) {
                        count++;
                    }
                    if (count == actorMax) {
                        break;
                    }
                }
            }
        }

        // Get Fanart
        if (isNotValidString(movie.getFanartURL()) && downloadFanart) {
            movie.setFanartURL(getFanartURL(movie));
            if (isValidString(movie.getFanartURL())) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
            }
        }

        return true;
    }

    @Override
    protected void updateTVShowInfo(Movie movie) {

        String allocineId = movie.getId(ALLOCINE_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || isNotValidString(allocineId)) {
            return;
        }

        try {
            String cacheKey = CacheMemory.generateCacheKey(CACHE_SERIES, allocineId);
            TvSeriesInfos tvSeriesInfos = (TvSeriesInfos) CacheMemory.getFromCache(cacheKey);
            if (tvSeriesInfos == null) {
                tvSeriesInfos = allocineApi.getTvSeriesInfos(allocineId);
                // Add to the cache
                CacheMemory.addToCache(cacheKey, tvSeriesInfos);
            }

            if (tvSeriesInfos.isNotValid()) {
                LOG.error(LOG_MESSAGE + "Can't find informations for TvShow with id: " + allocineId);
                return;
            }

            // Check Title
            if (OverrideTools.checkOverwriteTitle(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setTitle(tvSeriesInfos.getTitle(), ALLOCINE_PLUGIN_ID);
            }

            // Check Original Title
            if (OverrideTools.checkOverwriteOriginalTitle(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setOriginalTitle(tvSeriesInfos.getOriginalTitle(), ALLOCINE_PLUGIN_ID);
            }

            // Check Rating
            if (movie.getRating() == -1) {
                int rating = tvSeriesInfos.getUserRating();
                if (rating >= 0) {
                    movie.addRating(ALLOCINE_PLUGIN_ID, rating);
                }
            }

            // Check Year Start and End
            if (tvSeriesInfos.getYearStart() > 0 && OverrideTools.checkOverwriteYear(movie, ALLOCINE_PLUGIN_ID)) {
                if (tvSeriesInfos.getYearEnd() > 0) {
                    movie.setYear(tvSeriesInfos.getYearStart() + "-" + tvSeriesInfos.getYearEnd(), ALLOCINE_PLUGIN_ID);
                } else {
                    movie.setYear(String.valueOf(tvSeriesInfos.getYearStart()), ALLOCINE_PLUGIN_ID);
                }
            }

            // Check Plot
            if (OverrideTools.checkOverwritePlot(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPlot(tvSeriesInfos.getSynopsis(), ALLOCINE_PLUGIN_ID);
            }

            // Check Outline
            if (OverrideTools.checkOverwriteOutline(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setOutline(tvSeriesInfos.getSynopsisShort(), ALLOCINE_PLUGIN_ID);
            }

            // Check ReleaseDate
            if (OverrideTools.checkOverwriteReleaseDate(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setReleaseDate(tvSeriesInfos.getReleaseDate(), ALLOCINE_PLUGIN_ID);
            }

            // Check Company
            if (OverrideTools.checkOverwriteCompany(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setCompany(tvSeriesInfos.getOriginalChannel(), ALLOCINE_PLUGIN_ID);
            }

            // Check country
            if (OverrideTools.checkOverwriteCountry(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setCountries(tvSeriesInfos.getNationalities(), ALLOCINE_PLUGIN_ID);
            }

            // Check Genres
            if (OverrideTools.checkOverwriteGenres(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setGenres(tvSeriesInfos.getGenres(), ALLOCINE_PLUGIN_ID);
            }

            // Directors
            if (!tvSeriesInfos.getDirectors().isEmpty()) {
                Set<String> directors = new HashSet<String>();
                for (MoviePerson person : tvSeriesInfos.getDirectors()) {
                    directors.add(person.getName());
                }
                if (OverrideTools.checkOverwriteDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.setDirectors(directors, ALLOCINE_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwritePeopleDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.setPeopleDirectors(directors, ALLOCINE_PLUGIN_ID);
                }
            }

            // Writers
            if (!tvSeriesInfos.getWriters().isEmpty()) {
                Set<String> writers = new HashSet<String>();
                for (MoviePerson person : tvSeriesInfos.getWriters()) {
                    writers.add(person.getName());
                }
                if (OverrideTools.checkOverwriteWriters(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.setWriters(writers, ALLOCINE_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwritePeopleWriters(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.setPeopleWriters(writers, ALLOCINE_PLUGIN_ID);
                }
            }

            // Actors
            if (!tvSeriesInfos.getActors().isEmpty()) {
                if (OverrideTools.checkOverwriteActors(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.clearCast();
                    for (MoviePerson person : tvSeriesInfos.getActors()) {
                        movie.addActor(person.getName(), ALLOCINE_PLUGIN_ID);
                    }
                }

                if (OverrideTools.checkOverwritePeopleActors(movie, ALLOCINE_PLUGIN_ID)) {
                    movie.clearPeopleCast();
                    int count = 0;
                    for (MoviePerson person : tvSeriesInfos.getActors()) {
                        if (movie.addActor(Movie.UNKNOWN, person.getName(), person.getRole(), Movie.UNKNOWN, Movie.UNKNOWN, ALLOCINE_PLUGIN_ID)) {
                            count++;
                        }
                        if (count == actorMax) {
                            break;
                        }
                    }
                }
            }

            int currentSeason = movie.getSeason();
            try {
                if (currentSeason <= 0 || currentSeason > tvSeriesInfos.getSeasonCount()) {
                    throw new Exception("Invalid season " + movie.getSeason());
                }

                TvSeasonInfos tvSeasonInfos = allocineApi.getTvSeasonInfos(tvSeriesInfos.getSeasonCode(currentSeason));
                if (tvSeasonInfos.isValid()) {
                    for (MovieFile file : movie.getFiles()) {

                        for (int numEpisode = file.getFirstPart(); numEpisode <= file.getLastPart(); ++numEpisode) {
                            Episode episode = tvSeasonInfos.getEpisode(numEpisode);
                            if (episode != null) {

                                if (OverrideTools.checkOverwriteEpisodeTitle(file, numEpisode, ALLOCINE_PLUGIN_ID)) {
                                    file.setTitle(numEpisode, episode.getTitle(), ALLOCINE_PLUGIN_ID);
                                }

                                if (StringTools.isValidString(episode.getSynopsis()) && OverrideTools.checkOverwriteEpisodePlot(file, numEpisode, ALLOCINE_PLUGIN_ID)) {
                                    String episodePlot = HTMLTools.replaceHtmlTags(episode.getSynopsis(), " ");
                                    file.setPlot(numEpisode, episodePlot, ALLOCINE_PLUGIN_ID);
                                }
                            }
                        }
                    }
                }
            } catch (Exception error) {
                LOG.warn(LOG_MESSAGE + "Can't find informations for season " + currentSeason
                        + " for TvSeries with id " + allocineId + " (" + movie.getBaseName() + ")");
                LOG.warn(SystemTools.getStackTrace(error));
            }

            // Call the TvDBPlugin to download fanart and/or videoimages
            if (downloadFanart || includeVideoImages) {
                if (tvdb == null) {
                    tvdb = new TheTvDBPlugin();
                }
                String tvDBid = TheTvDBPlugin.findId(movie);
                if (StringTools.isValidString(tvDBid)) {
                    // This needs to check if we should overwrite the artwork or not.
                    movie.setFanartURL(FanartScanner.getFanartURL(movie));
                    tvdb.scanTVShowTitles(movie);
                }
            }
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving Allocine infos for TvShow : " + allocineId);
            LOG.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Get Movie Informations from AlloCine ID
     *
     * @param allocineId The AlloCine ID of the Movie
     * @return The MovieInfo object
     */
    public MovieInfos getMovieInfos(String allocineId) {
        MovieInfos movieInfos;

        String cacheKey = CacheMemory.generateCacheKey(CACHE_MOVIE, allocineId);
        movieInfos = (MovieInfos) CacheMemory.getFromCache(cacheKey);
        if (movieInfos == null) {
            try {
                movieInfos = allocineApi.getMovieInfos(allocineId);
            } catch (Exception error) {
                LOG.error(LOG_MESSAGE + "Failed retrieving Allocine infos for movie : " + allocineId);
                LOG.error(SystemTools.getStackTrace(error));
            }
            if (movieInfos != null && movieInfos.isValid()) {
                // Add to the cache
                CacheMemory.addToCache(cacheKey, movieInfos);
            }
        }

        return movieInfos;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // If we use allocine plugin look for
        // http://www.allocine.fr/...=XXXXX.html
        LOG.debug(LOG_MESSAGE + "Scanning NFO for Allocine id");
        int beginIndex = nfo.indexOf("http://www.allocine.fr/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf('=', beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf('.', beginIdIndex);
                if (endIdIndex != -1) {
                    LOG.debug(LOG_MESSAGE + "Allocine id found in NFO = " + nfo.substring(beginIdIndex + 1, endIdIndex));
                    movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, nfo.substring(beginIdIndex + 1, endIdIndex));
                    return Boolean.TRUE;
                }
            }
        }

        LOG.debug(LOG_MESSAGE + "No Allocine id found in NFO");
        return Boolean.FALSE;
    }
}
