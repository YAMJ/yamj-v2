/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.FanartScanner;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.themoviedb.model.Person;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * @author Stuart.Boston
 * @version 1.0 (20th July 2009)
 */
public class TheMovieDbPlugin implements MovieDatabasePlugin {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    public static final String TMDB_PLUGIN_ID = "themoviedb";
    public static final String IMDB_PLUGIN_ID = "imdb";
    private static final String API_KEY = PropertiesUtil.getProperty("TheMovieDB");
    private TheMovieDb TMDb;
    @SuppressWarnings("unused")
    private String language; // This is used in v2.1 of the API
    protected boolean downloadFanart;
    protected static String fanartToken;

    public TheMovieDbPlugin() {
        TMDb = new TheMovieDb(API_KEY);
        language = PropertiesUtil.getProperty("themoviedb.language", "en");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));
        fanartToken = PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart");
    }

    public boolean scan(Movie movie) {
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        String tmdbID = movie.getId(TMDB_PLUGIN_ID);
        MovieDB moviedb = null;
        boolean retval = false;

        // First look to see if we have a TMDb ID as this will make looking the film up easier
        if (tmdbID != null && !tmdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
            // Search based on TMdb ID
            System.out.println(">>moviedbGetInfo for " + movie.getTitle());
            moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb);
        } else if (imdbID != null && !imdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
            // Search based on IMDb ID
            System.out.println(">>moviedbImdbLookup for " + movie.getTitle());
            moviedb = TMDb.moviedbImdbLookup(imdbID);
            tmdbID = moviedb.getId();
            if (tmdbID != null && !tmdbID.equals("")) {
                System.out.println("TMDb ID: " + tmdbID);
                moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb);
            } else {
                logger.fine("Error: No TMDb ID found for movie!");
            }
        } else {
            // Search using movie name
            System.out.println(">>moviedbSearch for " + movie.getTitle());
            moviedb = TMDb.moviedbSearch(movie.getTitle());
            tmdbID = moviedb.getId();
            moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb);
        }

        if (moviedb.getId() != null && !moviedb.getId().equalsIgnoreCase(MovieDB.UNKNOWN)) {
            System.out.println(">>Found: (" + moviedb.getId() + ") " + moviedb.getTitle());
            movie.setMovieType(Movie.TYPE_MOVIE);
        } else {
            System.out.println(">> Not found: " + movie.getTitle());
        }

        if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            movie.setPosterURL(locatePosterURL(movie));
        }

        if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
            movie.setFanartURL(getFanartURL(movie));
            if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
            }
        }

        if (moviedb.getTitle() != null && !moviedb.getTitle().equalsIgnoreCase(MovieDB.UNKNOWN)) {
            movie = copyMovieInfo(moviedb, movie);
            retval = true;
        } else {
            retval = false;
        }

        return retval;
    }

    /**
     * Copy the movie info from the MovieDB bean to the YAMJ movie bean
     * 
     * @param moviedb
     *            The MovieDB source
     * @param movie
     *            The YAMJ target
     * @return The altered movie bean
     */
    private Movie copyMovieInfo(MovieDB moviedb, Movie movie) {

        // Title
        if (overwriteCheck(moviedb.getTitle(), movie.getTitle())) {
            movie.setTitle(moviedb.getTitle());
        }

        // TMDb ID
        if (overwriteCheck(moviedb.getId(), movie.getId(TMDB_PLUGIN_ID))) {
            movie.setId(TMDB_PLUGIN_ID, moviedb.getId());
        }

        // IMDb ID
        if (overwriteCheck(moviedb.getImdb(), movie.getId(IMDB_PLUGIN_ID))) {
            movie.setId(IMDB_PLUGIN_ID, moviedb.getImdb());
        }

        // plot
        if (overwriteCheck(moviedb.getOverview(), movie.getPlot())) {
            movie.setPlot(moviedb.getOverview());
            movie.setOutline(moviedb.getOverview());
        }

        // rating
        if (overwriteCheck(moviedb.getRating(), String.valueOf(movie.getRating()))) {
            movie.setRating(Integer.valueOf(moviedb.getRating()));
        }

        // Release Date
        if (overwriteCheck(moviedb.getReleaseDate(), movie.getReleaseDate())) {
            movie.setReleaseDate(moviedb.getReleaseDate());
        }

        // runtime
        if (overwriteCheck(moviedb.getRuntime(), movie.getRuntime())) {
            movie.setRuntime(moviedb.getRuntime());
        }

        // people section
        if (!moviedb.getPeople().isEmpty()) {
            for (Person person : moviedb.getPeople()) {
                System.out.println("Name: " + person.getName());
                System.out.println("Job : " + person.getJob());
                System.out.println("URL : " + person.getUrl());
            }
        } else
            System.out.println(">>No people");

        return movie;
    }

    /**
     * Checks to see if the source string is null or "UNKNOWN" and that target string ISN'T null or "UNKNOWN"
     * 
     * @param sourceString
     *            The source string to check
     * @param targetString
     *            The destination string to check
     * @return True if valid to overwrite
     */
    private boolean overwriteCheck(String sourceString, String targetString) {
        // false if the source is null or UNKNOWN
        if (sourceString != null && !sourceString.equalsIgnoreCase(Movie.UNKNOWN)) {
            // sourceString is valid, check target string IS null OR UNKNOWN
            if (targetString == null || targetString.equalsIgnoreCase(Movie.UNKNOWN) || targetString == "-1")
                return true;
            else
                return false;
        } else
            return false;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        // TODO Scan the NFO for TMDb ID
        logger.finest("Scanning NFO for Imdb Id");
        int beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
            } else {
                logger.finer("No Imdb Id found in nfo !");
            }
        }
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        // TheMovieDB.org does not have any TV Shows, so just return
        System.out.println("No TV Shows");
        return;
    }

    /**
     * Locate the FanartURL for the movie. This should probably be skipped as this uses TheMovieDb.org anyway
     * 
     * @param movie
     *            Movie bean for the movie to locate
     * @return The URL of the fanart
     */
    protected String getFanartURL(Movie movie) {
        return FanartScanner.getFanartURL(movie);
    }

    /**
     * Locate the poster URL from online sources
     * 
     * @param movie
     *            Movie bean for the video to locate
     * @param imdbXML
     *            XML page from IMDB to search for a URL
     * @return The URL of the poster if found.
     */
    protected String locatePosterURL(Movie movie) {
        // Note: Second parameter is null, because there is no IMDb XML
        return PosterScanner.getPosterURL(movie, null, IMDB_PLUGIN_ID);
    }
    
    /**
     * @Override public boolean scan(Movie movie) { List<Series> seriesList = null;
     * 
     * String id = movie.getId(THEMOVIEDB_PLUGIN_ID); if (id == null || id.equals(Movie.UNKNOWN)) { if (!movie.getTitle().equals(Movie.UNKNOWN)) { seriesList =
     * TMDb.searchSeries(movie.getTitle(), language); } if (seriesList == null || seriesList.isEmpty()) { seriesList = TMDb.searchSeries(movie.getBaseName(),
     * language); } if (seriesList != null && !seriesList.isEmpty()) { Series series = null; for (Series s : seriesList) { if (s.getFirstAired() != null &&
     * !s.getFirstAired().isEmpty()) { if (movie.getYear() != null && !movie.getYear().equals(Movie.UNKNOWN)) { try { Date firstAired =
     * dateFormat.parse(s.getFirstAired()); Calendar cal = Calendar.getInstance(); cal.setTime(firstAired); if (cal.get(Calendar.YEAR) ==
     * Integer.parseInt(movie.getYear())) { series = s; break; } } catch (Exception ignore) { } } else { series = s; break; } } } if (series == null) { series =
     * seriesList.get(0); } id = series.getId(); movie.setId(THEMOVIEDB_PLUGIN_ID, id); if (series.getImdbId() != null && !series.getImdbId().isEmpty()) {
     * movie.setId(IMDB_PLUGIN_ID, series.getImdbId()); } } }
     * 
     * if (id != null && !id.equals(Movie.UNKNOWN)) { Series series = TMDb.getSeries(id, language); if (series != null) {
     * 
     * Banners banners = TMDb.getBanners(id);
     * 
     * if (!movie.isOverrideTitle()) { movie.setTitle(series.getSeriesName()); movie.setOriginalTitle(series.getSeriesName()); } if
     * (movie.getYear().equals(Movie.UNKNOWN)) { String year = TMDb.getSeasonYear(id, movie.getSeason()); if (year != null && !year.isEmpty()) {
     * movie.setYear(year); } } if (movie.getRating() == -1 && series.getRating() != null && !series.getRating().isEmpty()) {
     * movie.setRating((int)(Float.parseFloat(series.getRating()) * 10)); } if (movie.getRuntime().equals(Movie.UNKNOWN)) {
     * movie.setRuntime(series.getRuntime()); } if (movie.getCompany().equals(Movie.UNKNOWN)) { movie.setCompany(series.getNetwork()); } if
     * (movie.getGenres().isEmpty()) { movie.setGenres(series.getGenres()); } if (movie.getPlot().equals(Movie.UNKNOWN)) { movie.setPlot(series.getOverview());
     * } if (movie.getCertification().equals(Movie.UNKNOWN)) { movie.setCertification(series.getContentRating()); } if (movie.getCast().isEmpty()) {
     * movie.setCast(series.getActors()); } if (movie.getPosterURL().equals(Movie.UNKNOWN)) { String url = null; if (!banners.getSeasonList().isEmpty()) { for
     * (Banner banner : banners.getSeasonList()) { if (banner.getSeason() == movie.getSeason() && banner.getBannerType2().equalsIgnoreCase("season")) { url =
     * banner.getUrl(); break; } } } if (url == null && !banners.getPosterList().isEmpty()) { url = banners.getPosterList().get(0).getUrl(); } if (url == null
     * && series.getPoster() != null && !series.getPoster().isEmpty()) { url = series.getPoster(); } if (url != null) { movie.setPosterURL(url); } } if
     * (downloadFanart && movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) { String url = null; if (!banners.getFanartList().isEmpty()) { int index =
     * movie.getSeason(); if (index <= 0) { index = 1; } else if (index > banners.getFanartList().size()) { index = banners.getFanartList().size(); } index--;
     * 
     * url = banners.getFanartList().get(index).getUrl(); } if (url == null && series.getFanart() != null && !series.getFanart().isEmpty()) { url =
     * series.getFanart(); } if (url != null) { movie.setFanartURL(url); }
     * 
     * if (movie.getFanartURL() != null && !movie.getFanartURL().isEmpty() && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
     * movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg"); } } scanTVShowTitles(movie); } }
     * 
     * return true; }
     * 
     * @Override public void scanTVShowTitles(Movie movie) { String id = movie.getId(THEMOVIEDB_PLUGIN_ID); if (!movie.isTVShow() || !movie.hasNewMovieFiles()
     * || id == null) { return; }
     * 
     * for (MovieFile file : movie.getMovieFiles()) { if (movie.getSeason() >= 0) { StringBuilder sb = new StringBuilder(); boolean first = true; for (int part
     * = file.getFirstPart(); part <= file.getLastPart(); ++part) { Episode episode = null; if (dvdEpisodes) { episode = TMDb.getDVDEpisode(id,
     * movie.getSeason(), part, language); } if (episode == null) { episode = TMDb.getEpisode(id, movie.getSeason(), part, language); }
     * 
     * if (episode != null) { if (first) { first = false; } else { sb.append(" / "); }
     * 
     * // We only get the writers for the first episode, otherwise we might overwhelm the skins with data // TODO Assign the writers on a per-episode basis,
     * rather than series. if ((movie.getWriters().equals(Movie.UNKNOWN)) || ( movie.getWriters().isEmpty())){ movie.setWriters(episode.getWriters()); }
     * 
     * // TODO Assign the director to each episode. if (((movie.getDirector().equals(Movie.UNKNOWN)) || (movie.getDirector().isEmpty())) &&
     * !episode.getDirectors().isEmpty()) { // Director is a single entry, not a list, so only get the first director
     * movie.setDirector(episode.getDirectors().get(0)); }
     * 
     * sb.append(episode.getEpisodeName());
     * 
     * if (includeEpisodePlots) { file.setPlot(part, episode.getOverview()); }
     * 
     * if (includeVideoImages) { file.setVideoImageURL(part, episode.getFilename()); } else { file.setVideoImageURL(part, Movie.UNKNOWN); } } } String title =
     * sb.toString(); if (!"".equals(sb) && !file.hasTitle()) { file.setTitle(title); } } } }
     * 
     * @Override public void scanNFO(String nfo, Movie movie) { super.scanNFO(nfo, movie);
     * 
     * logger.finest("Scanning NFO for TheTVDB Id"); String compareString = nfo.toUpperCase(); int idx = compareString.indexOf("THETVDB.COM"); if (idx > -1) {
     * int beginIdx = compareString.indexOf("&ID="); int length = 4; if (beginIdx < idx) { beginIdx = compareString.indexOf("?ID="); } if (beginIdx < idx) {
     * beginIdx = compareString.indexOf("&SERIESID="); length = 10; } if (beginIdx < idx) { beginIdx = compareString.indexOf("?SERIESID="); length = 10; }
     * 
     * if (beginIdx > idx) { int endIdx = compareString.indexOf("&", beginIdx + 1); String id = null; if (endIdx > -1) { id = compareString.substring(beginIdx +
     * length, endIdx); } else { id = compareString.substring(beginIdx + length); } if (id != null && !id.isEmpty()) { movie.setId(THEMOVIEDB_PLUGIN_ID, id);
     * logger.finer("TheMovieDB Id found in nfo = " + id); } } } }
     */
}
