/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.plugin;

import com.moviejukebox.allocine.*;
import com.moviejukebox.allocine.jaxb.Episode;
import com.moviejukebox.allocine.jaxb.Tvseries;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

public class AllocinePlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(AllocinePlugin.class);
    public static final String CACHE_SEARCH_MOVIE = "AllocineSearchMovie";
    public static final String CACHE_SEARCH_SERIES = "AllocineSearchSeries";
    public static final String CACHE_MOVIE = "AllocineMovie";
    public static final String CACHE_SERIES = "AllocineSeries";
    private XMLAllocineAPIHelper allocineAPI;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    protected TheTvDBPlugin tvdb = null;
    public static String ALLOCINE_PLUGIN_ID = "allocine";

    public AllocinePlugin() {
        super();
        allocineAPI = new XMLAllocineAPIHelper(PropertiesUtil.getProperty("API_KEY_Allocine"));
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "France");
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", "false");
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

        String AllocineId = movie.getId(ALLOCINE_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || isNotValidString(AllocineId)) {
            return;
        }

        try {
            String cacheKey = CacheMemory.generateCacheKey(CACHE_SERIES, AllocineId);
            TvSeriesInfos tvSeriesInfos = (TvSeriesInfos) CacheMemory.getFromCache(cacheKey);
            if (tvSeriesInfos == null) {
                tvSeriesInfos = allocineAPI.getTvSeriesInfos(AllocineId);
                // Add to the cache
                CacheMemory.addToCache(cacheKey, tvSeriesInfos);
            }

            if (tvSeriesInfos.isNotValid()) {
                logger.error("AllocinePlugin: Can't find informations for TvShow with id: " + AllocineId);
                return;
            }

            // Check Title
            if (!movie.isOverrideTitle() && isValidString(tvSeriesInfos.getTitle())) {
                movie.setTitle(tvSeriesInfos.getTitle());
            }

            // Check Rating
            if (movie.getRating() == -1) {
                int rating = tvSeriesInfos.getRating();
                if (rating >= 0) {
                    movie.addRating(ALLOCINE_PLUGIN_ID, rating);
                }
            }

            // Check Year Start and End
            if (!movie.isOverrideYear() && isNotValidString(movie.getYear()) && isValidString(tvSeriesInfos.getYearStart())) {
                if (isValidString(tvSeriesInfos.getYearEnd())) {
                    movie.setYear(tvSeriesInfos.getYearStart() + "-" + tvSeriesInfos.getYearEnd());
                } else {
                    movie.setYear(tvSeriesInfos.getYearStart());
                }
            }

            // Check Plot
            if (isNotValidString(movie.getPlot())) {
                String synopsis = tvSeriesInfos.getSynopsis();
                if (isValidString(synopsis)) {
                    String plot = trimToLength(synopsis, preferredPlotLength, true, plotEnding);
                    movie.setPlot(plot);
                }
            }

            // Check ReleaseDate and Company
            //if (tvSeriesInfos.getRelease() != null) {
            //   if (isNotValidString(movie.getReleaseDate()) && isValidString(tvSeriesInfos.getRelease().getReleaseDate())) {
            //        movie.setReleaseDate(tvSeriesInfos.getRelease().getReleaseDate());
            //    }
            //}

            if (isNotValidString(movie.getCompany()) && isValidString(tvSeriesInfos.getOriginalChannel())) {
                movie.setCompany(tvSeriesInfos.getOriginalChannel());
            }

            // Check country
            if (isNotValidString(movie.getCountry()) && !tvSeriesInfos.getNationalityList().isEmpty()) {
                String firstCountry = tvSeriesInfos.getNationalityList().get(0);
                movie.setCountry(firstCountry);
            }

            // Check Genres
            if (movie.getGenres().isEmpty()) {
                for (String genre : tvSeriesInfos.getGenreList()) {
                    movie.addGenre(genre);
                }
            }

            // Check Casting
            if (movie.getDirectors().isEmpty()) {
                movie.setDirectors(tvSeriesInfos.getDirectors());
            }
            if (movie.getCast().isEmpty()) {
                movie.setCast(tvSeriesInfos.getActors());
            }
            if (movie.getWriters().isEmpty()) {
                movie.setWriters(tvSeriesInfos.getWriters());
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
                        logger.debug("AllocinePlugin: Setting filename for episode Nb " + numEpisode);
                        Episode episode = tvSeasonInfos.getEpisode(numEpisode);
                        if (episode != null) {
                            // Set the title of the episode
                            if (isNotValidString(file.getTitle(numEpisode))) {
                                file.setTitle(numEpisode, episode.getTitle());
                            }

                            if (includeEpisodePlots && isNotValidString(file.getPlot(numEpisode))) {
                                String episodePlot =
                                        trimToLength(episode.getSynopsis(), preferredPlotLength, true, plotEnding);
                                file.setPlot(numEpisode, episodePlot);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("AllocinePlugin: Can't find informations for season " + currentSeason
                        + " for TvSeries with id " + AllocineId + " (" + movie.getBaseName() + ")");
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
            logger.error("AllocinePlugin: Failed retrieving allocine infos for TvShow "
                    + AllocineId + ". Perhaps the allocine XML API has changed ...");
            logger.error(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            logger.error("AllocinePlugin: Failed retrieving allocine infos for TvShow : " + AllocineId);
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Scan Allocine html page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {

        String AllocineId = movie.getId(ALLOCINE_PLUGIN_ID);

        try {

            String cacheKey = CacheMemory.generateCacheKey(CACHE_MOVIE, AllocineId);
            MovieInfos movieInfos = (MovieInfos) CacheMemory.getFromCache(cacheKey);
            if (movieInfos == null) {
                movieInfos = allocineAPI.getMovieInfos(AllocineId);
                // Add to the cache
                CacheMemory.addToCache(cacheKey, movieInfos);
            }

            if (movieInfos.isNotValid()) {
                logger.error("AllocinePlugin: Can't find informations for movie with id: " + AllocineId);
                return false;
            }

            // Check Title
            if (!movie.isOverrideTitle() && isValidString(movieInfos.getTitle())) {
                movie.setTitle(movieInfos.getTitle());
            }

            // Check OriginalTitle
            if (isValidString(movieInfos.getOriginalTitle())) {
                movie.setOriginalTitle(movieInfos.getOriginalTitle());
            }

            // Check Rating
            if (movie.getRating() == -1) {
                int rating = movieInfos.getRating();
                if (rating >= 0) {
                    movie.addRating(ALLOCINE_PLUGIN_ID, rating);
                }
            }

            // Check Year
            if (!movie.isOverrideYear() && isNotValidString(movie.getYear()) && isValidString(movieInfos.getProductionYear())) {
                movie.setYear(movieInfos.getProductionYear());
            }

            // Check Plot
            if (isNotValidString(movie.getPlot())) {
                String synopsis = movieInfos.getSynopsis();
                if (isValidString(synopsis)) {
                    String plot = trimToLength(synopsis, preferredPlotLength, true, plotEnding);
                    movie.setPlot(plot);
                }
            }

            // Check ReleaseDate and Company
            if (movieInfos.getRelease() != null) {
                if (isNotValidString(movie.getReleaseDate()) && isValidString(movieInfos.getRelease().getReleaseDate())) {
                    movie.setReleaseDate(movieInfos.getRelease().getReleaseDate());
                }
                if (isNotValidString(movie.getCompany()) && movieInfos.getRelease().getDistributor() != null
                        && isValidString(movieInfos.getRelease().getDistributor().getName())) {
                    movie.setCompany(movieInfos.getRelease().getDistributor().getName());
                }
            }

            // Check Runtime
            if (isNotValidString(movie.getRuntime())) {
                int runtime = movieInfos.getRuntime();
                if (runtime > 0) {
                    movie.setRuntime(StringTools.formatDuration(runtime));
                }
            }

            // Check country
            if (isNotValidString(movie.getCountry()) && !movieInfos.getNationalityList().isEmpty()) {
                String firstCountry = movieInfos.getNationalityList().get(0);
                movie.setCountry(firstCountry);
            }

            // Check Genres
            if (movie.getGenres().isEmpty()) {
                for (String genre : movieInfos.getGenreList()) {
                    movie.addGenre(genre);
                }
            }

            // Check certification
            if (isNotValidString(movie.getCertification())) {
                movie.setCertification(movieInfos.getCertification());
            }

            // Check Casting
            if (movie.getDirectors().isEmpty()) {
                movie.setDirectors(movieInfos.getDirectors());
            }
            if (movie.getCast().isEmpty()) {
                movie.setCast(movieInfos.getActors());
            }
            if (movie.getWriters().isEmpty()) {
                movie.setWriters(movieInfos.getWriters());
            }

            // Get Fanart
            if (isNotValidString(movie.getFanartURL()) && downloadFanart) {
                movie.setFanartURL(getFanartURL(movie));
                if (isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }
        } catch (JAXBException error) {
            logger.error("AllocinePlugin: Failed retrieving allocine infos for movie "
                    + AllocineId + ". Perhaps the allocine XML API has changed ...");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        } catch (Exception error) {
            logger.error("AllocinePlugin: Failed retrieving allocine infos for movie : " + AllocineId);
            logger.error(SystemTools.getStackTrace(error));
            return false;
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
                logger.debug("AllocinePlugin: Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
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
                logger.debug("AllocinePlugin: No Allocine Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException error) {
            // If no AllocineId found fallback to Imdb
            logger.debug("AllocinePlugin: Parse error. Now using ImdbPlugin");
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
            logger.error("AllocinePlugin: Failed to retrieve alloCine Id for movie : " + movieName);
            logger.error("AllocinePlugin: Now using ImdbPlugin");
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
//            logger.debug("AllocinePlugin: Allocine request via Google : " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            String allocineId = HTMLTools.extractTag(xml, "film/fichefilm_gen_cfilm=", ".html");
//            logger.debug("AllocinePlugin: Allocine found via Google : " + allocineId);
            return allocineId;
        } catch (Exception error) {
            logger.error("AllocinePlugin Failed retreiving AlloCine Id for movie : " + movieName);
            logger.error("AllocinePlugin Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        String result = src.replaceAll("\\<.*?>", "").trim();
        // logger.debug("AllocinePlugin: removeHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    protected String removeOpenedHtmlTags(String src) {
        String result = src.replaceAll("^.*?>", "");
        result = result.replaceAll("<.*?$", "");
        result = result.trim();
        // logger.debug("AllocinePlugin: removeOpenedHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        boolean result = false;
        // If we use allocine plugin look for
        // http://www.allocine.fr/...=XXXXX.html
        logger.debug("AllocinePlugin: Scanning NFO for Allocine Id");
        int beginIndex = nfo.indexOf("http://www.allocine.fr/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("=", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf(".", beginIdIndex);
                if (endIdIndex != -1) {
                    logger.debug("AllocinePlugin: Allocine Id found in nfo = " + new String(nfo.substring(beginIdIndex + 1, endIdIndex)));
                    movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, new String(nfo.substring(beginIdIndex + 1, endIdIndex)));
                    result = true;
                } else {
                    logger.debug("AllocinePlugin: No Allocine Id found in nfo !");
                }
            } else {
                logger.debug("AllocinePlugin: No Allocine Id found in nfo !");
            }
        } else {
            logger.debug("AllocinePlugin: No Allocine Id found in nfo !");
        }
        return result;
    }
}
