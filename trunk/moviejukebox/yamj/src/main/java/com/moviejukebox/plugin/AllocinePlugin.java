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

import com.moviejukebox.allocine.*;
import com.moviejukebox.allocine.jaxb.Episode;
import com.moviejukebox.allocine.jaxb.Tvseries;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.cache.CacheMemory;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

public class AllocinePlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(AllocinePlugin.class);
    private static final String LOG_MESSAGE = "AllocinePlugin: ";
    public static final String CACHE_SEARCH_MOVIE = "AllocineSearchMovie";
    public static final String CACHE_SEARCH_SERIES = "AllocineSearchSeries";
    public static final String CACHE_MOVIE = "AllocineMovie";
    public static final String CACHE_SERIES = "AllocineSeries";
    private XMLAllocineAPIHelper allocineAPI;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    protected TheTvDBPlugin tvdb = null;
    public static final String ALLOCINE_PLUGIN_ID = "allocine";

    public AllocinePlugin() {
        super();
        allocineAPI = new XMLAllocineAPIHelper(PropertiesUtil.getProperty("API_KEY_Allocine"));
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "France");
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", FALSE);
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", FALSE);
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", FALSE);
    }

    @Override
    public String getPluginID() {
        return ALLOCINE_PLUGIN_ID;
    }

    /**
     * Scan Allocine html page for the specified TV Show
     */
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
                tvSeriesInfos = allocineAPI.getTvSeriesInfos(allocineId);
                // Add to the cache
                CacheMemory.addToCache(cacheKey, tvSeriesInfos);
            }

            if (tvSeriesInfos.isNotValid()) {
                logger.error(LOG_MESSAGE + "Can't find informations for TvShow with id: " + allocineId);
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
                int rating = tvSeriesInfos.getRating();
                if (rating >= 0) {
                    movie.addRating(ALLOCINE_PLUGIN_ID, rating);
                }
            }

            // Check Year Start and End
            if (isValidString(tvSeriesInfos.getYearStart()) && OverrideTools.checkOverwriteYear(movie, ALLOCINE_PLUGIN_ID)) {
                if (isValidString(tvSeriesInfos.getYearEnd())) {
                    movie.setYear(tvSeriesInfos.getYearStart() + "-" + tvSeriesInfos.getYearEnd(), ALLOCINE_PLUGIN_ID);
                } else {
                    movie.setYear(tvSeriesInfos.getYearStart(), ALLOCINE_PLUGIN_ID);
                }
            }

            // Check Plot
            if (OverrideTools.checkOverwritePlot(movie, ALLOCINE_PLUGIN_ID)) {
                String synopsis = tvSeriesInfos.getSynopsis();
                if (isValidString(synopsis)) {
                    String plot = trimToLength(synopsis, preferredPlotLength, true, plotEnding);
                    movie.setPlot(plot, ALLOCINE_PLUGIN_ID);
                }
            }

            // Check ReleaseDate and Company
            if (tvSeriesInfos.getRelease() != null && OverrideTools.checkOverwriteReleaseDate(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setReleaseDate(tvSeriesInfos.getRelease().getReleaseDate(), ALLOCINE_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setCompany(tvSeriesInfos.getOriginalChannel(), ALLOCINE_PLUGIN_ID);
            }

            // Check country
            if (OverrideTools.checkOverwriteCountry(movie, ALLOCINE_PLUGIN_ID) && !tvSeriesInfos.getNationalityList().isEmpty()) {
                String firstCountry = tvSeriesInfos.getNationalityList().get(0);
                movie.setCountry(firstCountry, ALLOCINE_PLUGIN_ID);
            }

            // Check Genres
            if (OverrideTools.checkOverwriteGenres(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setGenres(tvSeriesInfos.getGenreList(), ALLOCINE_PLUGIN_ID);
            }

            // Check Casting
            if (OverrideTools.checkOverwriteDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setDirectors(tvSeriesInfos.getDirectors(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleDirectors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPeopleDirectors(tvSeriesInfos.getDirectors(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteActors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setCast(tvSeriesInfos.getActors(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleActors(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPeopleCast(tvSeriesInfos.getActors(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteWriters(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setWriters(tvSeriesInfos.getWriters(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleWriters(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setPeopleWriters(tvSeriesInfos.getWriters(), ALLOCINE_PLUGIN_ID);
            }

            int currentSeason = movie.getSeason();
            try {
                if (currentSeason <= 0 || currentSeason > tvSeriesInfos.getSeasonCount()) {
                    throw new Exception();
                }
                TvSeasonInfos tvSeasonInfos = null;
                for (MovieFile file : movie.getFiles()) {
                    if (!file.isNewFile() && file.hasTitle()) {
                        // don't scan episode title if it exists in XML data
                        continue;
                    }
                    if (tvSeasonInfos == null) {
                        tvSeasonInfos = allocineAPI.getTvSeasonInfos(tvSeriesInfos.getSeasonCode(currentSeason));
                    }
                    if (tvSeasonInfos.isNotValid()) {
                        continue;
                    }
                    // A file can have multiple episodes in it
                    for (int numEpisode = file.getFirstPart(); numEpisode <= file.getLastPart(); ++numEpisode) {
                        logger.debug(LOG_MESSAGE + "Setting filename for episode Nb " + numEpisode);
                        Episode episode = tvSeasonInfos.getEpisode(numEpisode);
                        if (episode != null) {
                            // Set the title of the episode
                            if (isNotValidString(file.getTitle(numEpisode))) {
                                file.setTitle(numEpisode, episode.getTitle());
                            }

                            if (includeEpisodePlots && isNotValidString(file.getPlot(numEpisode))) {
                                String episodePlot = trimToLength(HTMLTools.replaceHtmlTags(episode.getSynopsis(), " "), preferredPlotLength, true, plotEnding);
                                file.setPlot(numEpisode, episodePlot);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn(LOG_MESSAGE + "Can't find informations for season " + currentSeason
                        + " for TvSeries with id " + allocineId + " (" + movie.getBaseName() + ")");
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
        } catch (JAXBException error) {
            logger.error(LOG_MESSAGE + "Failed retrieving allocine infos for TvShow "
                    + allocineId + ". Perhaps the allocine XML API has changed ...");
            logger.error(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retrieving allocine infos for TvShow : " + allocineId);
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Get Movie Informations from Allocine ID
     *
     * @param allocineId The allocine ID of the Movie
     * @return The MovieInfo object
     */
    public MovieInfos getMovieInfos(String allocineId) {
        MovieInfos movieInfos;

        String cacheKey = CacheMemory.generateCacheKey(CACHE_MOVIE, allocineId);
        movieInfos = (MovieInfos) CacheMemory.getFromCache(cacheKey);
        if (movieInfos == null) {
            try {
                movieInfos = allocineAPI.getMovieInfos(allocineId);
            } catch (JAXBException error) {
                logger.error(LOG_MESSAGE + "Failed retrieving allocine infos for movie "
                        + allocineId + ". Perhaps the allocine XML API has changed ...");
                logger.error(SystemTools.getStackTrace(error));
            } catch (Exception error) {
                logger.error(LOG_MESSAGE + "Failed retrieving allocine infos for movie : " + allocineId);
                logger.error(SystemTools.getStackTrace(error));
            }
            // Add to the cache
            CacheMemory.addToCache(cacheKey, movieInfos);
        }

        return movieInfos;
    }

    /**
     * Scan Allocine html page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {

        String allocineId = movie.getId(ALLOCINE_PLUGIN_ID);

        MovieInfos movieInfos = getMovieInfos(allocineId);
        if (movieInfos == null) {
            logger.error(LOG_MESSAGE + "Can't find informations for movie with id: " + allocineId);
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
            int rating = movieInfos.getRating();
            if (rating >= 0) {
                movie.addRating(ALLOCINE_PLUGIN_ID, rating);
            }
        }

        // Check Year
        if (OverrideTools.checkOverwriteYear(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setYear(movieInfos.getProductionYear(), ALLOCINE_PLUGIN_ID);
        }

        // Check Plot
        if (OverrideTools.checkOverwritePlot(movie, ALLOCINE_PLUGIN_ID)) {
            String synopsis = movieInfos.getSynopsis();
            if (isValidString(synopsis)) {
                String plot = trimToLength(synopsis, preferredPlotLength, true, plotEnding);
                movie.setPlot(plot, ALLOCINE_PLUGIN_ID);
            }
        }

        // Check ReleaseDate and Company
        if (movieInfos.getRelease() != null) {
            if (OverrideTools.checkOverwriteReleaseDate(movie, ALLOCINE_PLUGIN_ID)) {
                movie.setReleaseDate(movieInfos.getRelease().getReleaseDate(), ALLOCINE_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteCompany(movie, ALLOCINE_PLUGIN_ID) && (movieInfos.getRelease().getDistributor() != null)) {
                movie.setCompany(movieInfos.getRelease().getDistributor().getName(), ALLOCINE_PLUGIN_ID);
            }
        }

        // Check Runtime
        if (OverrideTools.checkOverwriteRuntime(movie, ALLOCINE_PLUGIN_ID)) {
            int runtime = movieInfos.getRuntime();
            if (runtime > 0) {
                movie.setRuntime(DateTimeTools.formatDuration(runtime), ALLOCINE_PLUGIN_ID);
            }
        }

        // Check country
        if (OverrideTools.checkOverwriteCountry(movie, ALLOCINE_PLUGIN_ID) && !movieInfos.getNationalityList().isEmpty()) {
            String firstCountry = movieInfos.getNationalityList().get(0);
            movie.setCountry(firstCountry, ALLOCINE_PLUGIN_ID);
        }

        // Check Genres
        if (OverrideTools.checkOverwriteGenres(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setGenres(movieInfos.getGenreList(), ALLOCINE_PLUGIN_ID);
        }

        // Check certification
        if (OverrideTools.checkOverwriteCertification(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setCertification(movieInfos.getCertification(), ALLOCINE_PLUGIN_ID);
        }

        // Check Casting
        if (OverrideTools.checkOverwriteDirectors(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setDirectors(movieInfos.getDirectors(), ALLOCINE_PLUGIN_ID);
        }
        if (OverrideTools.checkOverwritePeopleDirectors(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setPeopleDirectors(movieInfos.getDirectors(), ALLOCINE_PLUGIN_ID);
        }

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
                        count ++;
                    }
                    if (count == actorMax) {
                        break;
                    }
                }
            }
        }

        if (OverrideTools.checkOverwriteWriters(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setWriters(movieInfos.getWriters(), ALLOCINE_PLUGIN_ID);
        }
        if (OverrideTools.checkOverwritePeopleWriters(movie, ALLOCINE_PLUGIN_ID)) {
            movie.setPeopleWriters(movieInfos.getWriters(), ALLOCINE_PLUGIN_ID);
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
    public boolean scan(Movie mediaFile) {
        boolean retval;
        try {
            String allocineId = mediaFile.getId(ALLOCINE_PLUGIN_ID);
            if (isNotValidString(allocineId)) {
                allocineId = getAllocineId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.isTVShow() ? 0 : -1);
            }

            // we also get imdb Id for extra infos
            if (isNotValidString(mediaFile.getId(IMDB_PLUGIN_ID))) {
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getOriginalTitle(), mediaFile.getYear()));
                logger.debug(LOG_MESSAGE + "Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }

            if (isValidString(allocineId)) {
                mediaFile.setId(ALLOCINE_PLUGIN_ID, allocineId);
                if (mediaFile.isTVShow()) {
                    updateTVShowInfo(mediaFile);
                    retval = true;
                } else {
                    retval = updateMovieInfo(mediaFile);
                }
            } else {
                // If no AllocineId found fallback to Imdb
                logger.debug(LOG_MESSAGE + "No Allocine Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException error) {
            // If no AllocineId found fallback to Imdb
            logger.debug(LOG_MESSAGE + "Parse error. Now using ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the allocineId matching the specified movie name.
     *
     * @throws ParseException
     */
    public String getAllocineId(String movieName, String year, int tvSeason) throws ParseException {
        String allocineId = Movie.UNKNOWN;
        try {
            if (tvSeason > -1) {
                allocineId = getAllocineSerieId(movieName, year);
            } else {
                allocineId = getAllocineMovieId(movieName, year);
            }
            if (isNotValidString(allocineId) && isValidString(year)) {
                // Try to find the allocine id with google
                return getAllocineIdFromGoogle(movieName, year);
            }

            return allocineId;

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed to retrieve alloCine Id for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Now using ImdbPlugin");
            throw new ParseException(allocineId, 0);
        }
    }

    private String getAllocineSerieId(String movieName, String year) throws Exception {
        String cacheKey = CacheMemory.generateCacheKey(CACHE_SEARCH_SERIES, movieName);
        Search searchInfos = (Search) CacheMemory.getFromCache(cacheKey);
        if (searchInfos == null) {
            searchInfos = allocineAPI.searchTvseriesInfos(movieName);
            // Add to the cache
            CacheMemory.addToCache(cacheKey, searchInfos);
        }
        if (searchInfos.isValid() && searchInfos.getTotalResults() > 0) {
            int totalResults = searchInfos.getTotalResults();
            // If we have a valid year try to find the first serie that match
            if (totalResults > 1 && isValidString(year)) {
                int yearSerie = NumberUtils.toInt(year, -1);
                for (Tvseries serie : searchInfos.getTvseries()) {
                    if (serie != null) {
                        int serieStart = NumberUtils.toInt(serie.getYearStart(), -1);
                        if (serieStart == -1) {
                            continue;
                        }
                        int serieEnd = NumberUtils.toInt(serie.getYearEnd(), -1);
                        if (serieEnd == -1) {
                            serieEnd = serieStart;
                        }
                        if (yearSerie >= serieStart && yearSerie <= serieEnd) {
                            return String.valueOf(serie.getCode());
                        }
                    }
                }
            }
            // We don't find a serie or there only one result, return the first
            List<Tvseries> serieList = searchInfos.getTvseries();
            if (!serieList.isEmpty()) {
                Tvseries serie = serieList.get(0);
                if (serie != null) {
                    return String.valueOf(serie.getCode());
                }
            }
        }
        return Movie.UNKNOWN;
    }

    private String getAllocineMovieId(String movieName, String year) throws Exception {
        String cacheKey = CacheMemory.generateCacheKey(CACHE_SEARCH_MOVIE, movieName);
        Search searchInfos = (Search) CacheMemory.getFromCache(cacheKey);
        if (searchInfos == null) {
            searchInfos = allocineAPI.searchMovieInfos(movieName);
            // Add to the cache
            CacheMemory.addToCache(cacheKey, searchInfos);
        }
        if (searchInfos.isValid() && searchInfos.getTotalResults() > 0) {
            int totalResults = searchInfos.getTotalResults();
            // If we have a valid year try to find the first movie that match
            if (totalResults > 1 && isValidString(year)) {
                int yearMovie = NumberUtils.toInt(year, -1);
                for (com.moviejukebox.allocine.jaxb.Movie movie : searchInfos.getMovie()) {
                    if (movie != null) {
                        int movieProductionYear = NumberUtils.toInt(movie.getProductionYear(), -1);
                        if (movieProductionYear == -1) {
                            continue;
                        }
                        if (movieProductionYear == yearMovie) {
                            return String.valueOf(movie.getCode());
                        }
                    }
                }
            }
            // We don't find a movie or there only one result, return the first
            List<com.moviejukebox.allocine.jaxb.Movie> movieList = searchInfos.getMovie();
            if (!movieList.isEmpty()) {
                com.moviejukebox.allocine.jaxb.Movie movie = movieList.get(0);
                if (movie != null) {
                    return String.valueOf(movie.getCode());
                }
            }
        }
        return Movie.UNKNOWN;
    }

    /**
     * Retrieve the AllocineId matching the specified movie name and year. This
     * routine is base on a Google request.
     *
     * @param movieName The name of the Movie to search for
     * @param year The year of the movie
     * @return The AllocineId if it was found
     */
    private String getAllocineIdFromGoogle(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.fr/search?hl=fr&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append("+site%3Awww.allocine.fr&meta=");
//            logger.debug(LOG_MESSAGE + "Allocine request via Google : " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            String allocineId = HTMLTools.extractTag(xml, "film/fichefilm_gen_cfilm=", ".html");
//            logger.debug(LOG_MESSAGE + "Allocine found via Google : " + allocineId);
            return allocineId;
        } catch (Exception error) {
            logger.error("AllocinePlugin Failed retreiving AlloCine Id for movie : " + movieName);
            logger.error("AllocinePlugin Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        String result = src.replaceAll("\\<.*?>", "").trim();
        // logger.debug(LOG_MESSAGE + "removeHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    protected String removeOpenedHtmlTags(String src) {
        String result = src.replaceAll("^.*?>", "");
        result = result.replaceAll("<.*?$", "");
        result = result.trim();
        // logger.debug(LOG_MESSAGE + "removeOpenedHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        boolean result = false;
        // If we use allocine plugin look for
        // http://www.allocine.fr/...=XXXXX.html
        logger.debug(LOG_MESSAGE + "Scanning NFO for Allocine Id");
        int beginIndex = nfo.indexOf("http://www.allocine.fr/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf('=', beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf('.', beginIdIndex);
                if (endIdIndex != -1) {
                    logger.debug(LOG_MESSAGE + "Allocine Id found in nfo = " + new String(nfo.substring(beginIdIndex + 1, endIdIndex)));
                    movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, new String(nfo.substring(beginIdIndex + 1, endIdIndex)));
                    result = true;
                } else {
                    logger.debug(LOG_MESSAGE + "No Allocine Id found in nfo !");
                }
            } else {
                logger.debug(LOG_MESSAGE + "No Allocine Id found in nfo !");
            }
        } else {
            logger.debug(LOG_MESSAGE + "No Allocine Id found in nfo !");
        }
        return result;
    }
}
