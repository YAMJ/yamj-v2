/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.pojava.datetime.DateTime;

import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.Category;
import com.moviejukebox.themoviedb.model.Country;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.themoviedb.model.Person;
import com.moviejukebox.themoviedb.model.Studio;
import com.moviejukebox.thetvdb.tools.WebBrowser;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;


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
    private int preferredPlotLength;

    public TheMovieDbPlugin() {
        TMDb = new TheMovieDb(API_KEY);
        // Set the proxy
        TMDb.setProxy(WebBrowser.getProxyHost(), WebBrowser.getProxyPort(), WebBrowser.getProxyUsername(), WebBrowser.getProxyPassword());
        
        // Set the timeouts
        TMDb.setTimeout(WebBrowser.getWebTimeoutConnect(), WebBrowser.getWebTimeoutRead());

        language = PropertiesUtil.getProperty("themoviedb.language", "en-US");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.movie.download", "false"));
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
    }

    public boolean scan(Movie movie) {
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        String tmdbID = movie.getId(TMDB_PLUGIN_ID);
        List<MovieDB> movieList;
        MovieDB moviedb = new MovieDB();
        boolean retval = false;

        ThreadExecutor.enterIO(webhost);
        try {
            // First look to see if we have a TMDb ID as this will make looking the film up easier
            if (FileTools.isValidString(tmdbID)) {
                // Search based on TMdb ID
                moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
            } else if (FileTools.isValidString(imdbID)) {
                // Search based on IMDb ID
                moviedb = TMDb.moviedbImdbLookup(imdbID, language);
                tmdbID = moviedb.getId();
                if (FileTools.isValidString(tmdbID)) {
                    moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
                } else {
                    logger.fine("Error: No TMDb ID found for movie!");
                }
            } else {
                String yearSuffix = "";
                if (FileTools.isValidString(movie.getYear())) {
                    yearSuffix = " " + movie.getYear();
                }
                
                // Search using movie name
                movieList = TMDb.moviedbSearch(movie.getTitle() + yearSuffix, language);
                moviedb = TheMovieDb.findMovie(movieList, movie.getTitle(), movie.getYear());
                if (moviedb != null) {
                    tmdbID = moviedb.getId();
                    moviedb = TMDb.moviedbGetInfo(tmdbID, moviedb, language);
                }
            }
        } finally {
            // the rest is not web search anymore
            ThreadExecutor.leaveIO();
        }

        if (moviedb != null) {
            if (FileTools.isValidString(moviedb.getId())) {
                movie.setMovieType(Movie.TYPE_MOVIE);
            }
    
            if (FileTools.isValidString(moviedb.getTitle())) {
                copyMovieInfo(moviedb, movie);
                retval = true;
            } else {
                retval = false;
            }
        } else {
            retval = false;
        }
        // TODO: Remove this check at some point when all skins have moved over to the new property
        downloadFanart = FanartScanner.checkDownloadFanart(movie.isTVShow());

        if (downloadFanart && !FileTools.isValidString(movie.getFanartURL())) {
            movie.setFanartURL(getFanartURL(movie));
            if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
            }
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
    private void copyMovieInfo(MovieDB moviedb, Movie movie) {

        // Title
        //if (overwriteCheck(moviedb.getTitle(), movie.getTitle())) {
            movie.setTitle(moviedb.getTitle());
            
            // We're overwriting the title, so we should do the original name too
            movie.setOriginalTitle(moviedb.getOriginalName());
        //}

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
            String plot = moviedb.getOverview();
            if (plot.length() > preferredPlotLength) {
                plot = plot.substring(0, Math.min(plot.length(), preferredPlotLength - 3)) + "...";
            }
            movie.setPlot(plot);
            movie.setOutline(plot);
        }

        // rating
        if (overwriteCheck(moviedb.getRating(), String.valueOf(movie.getRating()))) {
            try {
                float rating = Float.valueOf(moviedb.getRating()) * 10; // Convert rating to integer
                movie.setRating((int) rating);
            } catch (Exception error) {
                logger.finer("TheMovieDbPlugin: Error converting rating for " + movie.getBaseName());
                movie.setRating(-1);
            }
        }

        // Release Date
        if (overwriteCheck(moviedb.getReleaseDate(), movie.getReleaseDate())) {
            movie.setReleaseDate(moviedb.getReleaseDate());
            try {
                String year = moviedb.getReleaseDate();
                // Check if this is the default year and skip it
                if (!"1900-01-01".equals(year)) {
                    year = (new DateTime(year)).toString("yyyy");
                    movie.setYear(year);
                } else {
                    movie.setYear(Movie.UNKNOWN);
                }
            } catch (Exception ignore) {
                // Don't set the year
            }
        }

        // runtime
        if (overwriteCheck(moviedb.getRuntime(), movie.getRuntime())) {
            movie.setRuntime(moviedb.getRuntime());
        }

        // people section
        if (!moviedb.getPeople().isEmpty()) {
            for (Person person : moviedb.getPeople()) {
                // Save the information to the cast/actor/writers sections
                if ("Actor".equalsIgnoreCase(person.getJob())) {
                    //logger.fine(person.getName() + " is an Actor/Cast");
                    movie.addActor(person.getName());
                    continue;
                }

                if ("Director".equalsIgnoreCase(person.getJob())) {
                    //logger.fine(person.getName() + " is a Director");
                    movie.addDirector(person.getName());
                    continue;
                }

                if ("Author".equalsIgnoreCase(person.getJob())) {
                    //logger.fine(person.getName() + " is a Writer");
                    movie.addWriter(person.getName());
                    continue;
                }
                //logger.fine("Skipped job " + job + " for " +person.getName());
            }
        }
        
        // quote / tagline
        if (overwriteCheck(moviedb.getTagline(), movie.getQuote())) {
            movie.setQuote(moviedb.getTagline());
        }
        
        // Country
        List<Country> countries = moviedb.getCountries();
        if (!countries.isEmpty()) {
            String country = countries.get(0).getName();
            if (overwriteCheck(country, movie.getCountry())) {
                // This only returns one country.
                movie.setCountry(country);
            }
        }
        
        // Company
        List<Studio> studios = moviedb.getStudios();
        if (!studios.isEmpty()) {
            String studio = studios.get(0).getName();
            if (overwriteCheck(studio, movie.getCompany())) {
                movie.setCompany(studio);
            }
        }
        
        // Certification
        if (overwriteCheck(moviedb.getCertification(), movie.getCertification())) {
            movie.setCertification(moviedb.getCertification());
        }
        
        // Language
        if (overwriteCheck(moviedb.getLanguage(), movie.getLanguage())) {
            String language = MovieFilenameScanner.determineLanguage(moviedb.getLanguage());
            movie.setLanguage(language);
        }
        
        // Genres
        List<Category> genres = moviedb.getCategories();
        if (!genres.isEmpty()) {
            if (movie.getGenres().isEmpty()) {
                for (Category genre : genres) {
                    if (genre.getType().equalsIgnoreCase("genre")) {
                        movie.addGenre(genre.getName());
                    }
                }
            }
        }

        return;
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
        if (FileTools.isValidString(sourceString)) {
            // sourceString is valid, check target string IS null OR UNKNOWN
            if (!FileTools.isValidString(targetString) || targetString.equals("-1")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        int beginIndex;
        
        logger.finest("Scanning NFO for TheMovieDb Id");
        beginIndex = nfo.indexOf("/movie/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(TMDB_PLUGIN_ID, st.nextToken());
            logger.finer("TheMovieDb Id found in nfo = " + movie.getId(TMDB_PLUGIN_ID));
        } else {
            logger.finer("No TheMovieDb Id found in nfo!");
        }
        
        // We might as well look for the IMDb ID as well
        beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
                logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
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
}