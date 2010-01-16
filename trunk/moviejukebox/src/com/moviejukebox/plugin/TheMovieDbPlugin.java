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
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.FanartScanner;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.themoviedb.model.Person;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

/**
 * @author Stuart.Boston
 * @version 1.0 (20th July 2009)
 */
public class TheMovieDbPlugin implements MovieDatabasePlugin {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    public static final String TMDB_PLUGIN_ID = "themoviedb";
    public static final String IMDB_PLUGIN_ID = "imdb";
    private static final String webhost = "themoviedb.org";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
    private TheMovieDb TMDb;
    private String language;
    protected boolean downloadFanart;
    protected static String fanartToken;

    public TheMovieDbPlugin() {
        TMDb = new TheMovieDb(API_KEY);
        language = PropertiesUtil.getProperty("themoviedb.language", "en");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.movie.download", "false"));
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
    }

    public boolean scan(Movie movie) {
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        String tmdbID = movie.getId(TMDB_PLUGIN_ID);
        MovieDB moviedb = null;
        boolean retval = false;

        Semaphore s = WebBrowser.getSemaphore(webhost);
        s.acquireUninterruptibly();      
        // First look to see if we have a TMDb ID as this will make looking the film up easier
        if (tmdbID != null && !tmdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
            // Search based on TMdb ID
            moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
        } else if (imdbID != null && !imdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
            // Search based on IMDb ID
            moviedb = TMDb.moviedbImdbLookup(imdbID, language);
            tmdbID = moviedb.getId();
            if (tmdbID != null && !tmdbID.equals("")) {
                moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
            } else {
                logger.fine("Error: No TMDb ID found for movie!");
            }
        } else {
            // Search using movie name
            moviedb = TMDb.moviedbSearch(movie.getTitle(), language);
            tmdbID = moviedb.getId();
            moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
        }
        //the rest is not web search anymore
        s.release();

        if (moviedb.getId() != null && !moviedb.getId().equalsIgnoreCase(MovieDB.UNKNOWN)) {
            movie.setMovieType(Movie.TYPE_MOVIE);
        }

        if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            movie.setPosterURL(locatePosterURL(movie));
        }

        // TODO: Remove this check at some point when all skins have moved over to the new property
        downloadFanart = ImdbPlugin.checkDownloadFanart(movie.isTVShow());

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
}