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

import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.Genre;
import com.omertron.themoviedbapi.model.Language;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.model.Person;
import com.omertron.themoviedbapi.model.PersonType;
import com.omertron.themoviedbapi.model.ProductionCompany;
import com.omertron.themoviedbapi.model.ProductionCountry;
import com.omertron.themoviedbapi.model.ReleaseInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;

/**
 * @author Stuart.Boston
 * @version 2.0 (18th October 2010)
 */
public class TheMovieDbPlugin implements MovieDatabasePlugin {

    private static final Logger logger = Logger.getLogger(TheMovieDbPlugin.class);
    private static final String LOG_MESSAGE = "TheMovieDbPlugin: ";
    public static final String TMDB_PLUGIN_ID = "themoviedb";
    public static final String IMDB_PLUGIN_ID = "imdb";
    private static final String webhost = "themoviedb.org";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
    private TheMovieDbApi TMDb;
    private String languageCode;
    private String countryCode;
    private boolean downloadFanart;
    private static String fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
    private String fanartExtension;
    private int preferredPlotLength;
    private int preferredOutlineLength;
    private static final Boolean INCLUDE_ADULT = PropertiesUtil.getBooleanProperty("themoviedb.includeAdult", FALSE);
    public static final int SEARCH_MATCH = PropertiesUtil.getIntProperty("themoviedb.searchMatch", "3");

    public TheMovieDbPlugin() {
        try {
            TMDb = new TheMovieDbApi(API_KEY);
        } catch (MovieDbException ex) {
            logger.warn(LOG_MESSAGE + "Failed to initialise TheMovieDB API: " + ex.getMessage());
            return;
        }

        // Set the proxy
        TMDb.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeouts
        TMDb.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        languageCode = PropertiesUtil.getProperty("themoviedb.language", "en");
        countryCode = PropertiesUtil.getProperty("themoviedb.country", "");     // Don't default this as we might get it from the language (old setting)

        if (languageCode.length() > 2) {
            if (StringUtils.isBlank(countryCode)) {
                // Guess that the last 2 characters of the language code is the country code.
                countryCode = new String(languageCode.substring(languageCode.length() - 2)).toUpperCase();
            }
            languageCode = new String(languageCode.substring(0, 2)).toLowerCase();
        }
        logger.debug(LOG_MESSAGE + "Using `" + languageCode + "` as the language code");
        logger.debug(LOG_MESSAGE + "Using `" + countryCode + "` as the country code");

        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.movie.download", FALSE);
        fanartExtension = PropertiesUtil.getProperty("fanart.format", "jpg");
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");
    }

    @Override
    public String getPluginID() {
        return TMDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        String tmdbID = movie.getId(TMDB_PLUGIN_ID);
        List<MovieDb> movieList;
        List<ReleaseInfo> movieReleaseInfo = new ArrayList<ReleaseInfo>();
        List<Person> moviePeople = new ArrayList<Person>();
        MovieDb moviedb = null;
        boolean retval = false;

        ThreadExecutor.enterIO(webhost);
        try {
            // First look to see if we have a TMDb ID as this will make looking the film up easier
            if (StringTools.isValidString(tmdbID)) {
                // Search based on TMdb ID
                logger.debug(LOG_MESSAGE + "Using TMDb ID (" + tmdbID + ") for " + movie.getBaseFilename());
                try {
                    moviedb = TMDb.getMovieInfo(Integer.parseInt(tmdbID), languageCode);
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to get movie info using TMDB ID: " + tmdbID + " - " + ex.getMessage());
                    moviedb = null;
                }
            }

            if (moviedb == null && StringTools.isValidString(imdbID)) {
                // Search based on IMDb ID
                logger.debug(LOG_MESSAGE + "Using IMDb ID (" + imdbID + ") for " + movie.getBaseFilename());
                try {
                    moviedb = TMDb.getMovieInfoImdb(imdbID, languageCode);
                    tmdbID = String.valueOf(moviedb.getId());
                    if (StringTools.isNotValidString(tmdbID)) {
                        logger.debug(LOG_MESSAGE + "No TMDb ID found for movie!");
                    }
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to get movie info using IMDB ID: " + imdbID + " - " + ex.getMessage());
                    moviedb = null;
                }
            }

            if (moviedb == null) {
                try {
                    // Search using movie name
                    int movieYear = 0;
                    if (StringTools.isValidString(movie.getYear()) && StringUtils.isNumeric(movie.getYear())) {
                        movieYear = Integer.parseInt(movie.getYear());
                    }
                    movieList = TMDb.searchMovie(movie.getTitle(), movieYear, languageCode, INCLUDE_ADULT, 0);
                    // Iterate over the list until we find a match
                    for (MovieDb m : movieList) {
                        logger.debug(LOG_MESSAGE + "Checking " + m.getTitle() + " (" + m.getReleaseDate().substring(0,4) + ")");
                        if (TheMovieDbApi.compareMovies(m, movie.getTitle(), Integer.toString(movieYear), SEARCH_MATCH)) {
                            moviedb = m;
                            break;
                        }

                        // See if the original title is different and then compare it too
                        if (!movie.getTitle().equals(movie.getOriginalTitle())
                                && TheMovieDbApi.compareMovies(m, movie.getOriginalTitle(), Integer.toString(movieYear))) {
                            moviedb = m;
                            break;
                        }
                    }
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to get movie info for " + movie.getTitle() + " - " + ex.getMessage());
                    moviedb = null;
                }
            }

            if (moviedb == null) {
                logger.debug(LOG_MESSAGE + "Movie " + movie.getBaseName() + " not found!");
                logger.debug(LOG_MESSAGE + "Try using a NFO file to specify the movie");
            } else {
                try {
                    // Get the full information on the film
                    moviedb = TMDb.getMovieInfo(moviedb.getId(), languageCode);
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to download remaining information for " + movie.getBaseName());
                }
                logger.debug(LOG_MESSAGE + "Found id (" + moviedb.getId() + ") for " + moviedb.getTitle());
            }


            if (moviedb != null) {
                try {
                    // Get the release information
                    movieReleaseInfo = TMDb.getMovieReleaseInfo(moviedb.getId(), countryCode);
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to get release information");
                }

                try {
                    // Get the cast information
                    moviePeople = TMDb.getMovieCasts(moviedb.getId());
                } catch (MovieDbException ex) {
                    logger.debug(LOG_MESSAGE + "Failed to get cast information");
                }
            }
        } finally {
            // the rest is not web search anymore
            ThreadExecutor.leaveIO();
        }

        if (moviedb != null) {
            if (moviedb.getId() > 0) {
                movie.setMovieType(Movie.TYPE_MOVIE);
            }

            if (StringTools.isValidString(moviedb.getTitle())) {
                copyMovieInfo(moviedb, movie);
                retval = true;
            }

            // Set the release information
            if (movieReleaseInfo.size() > 0) {
                logger.debug(LOG_MESSAGE + "Found release information: " + movieReleaseInfo.get(0).toString());
                movie.setCertification(movieReleaseInfo.get(0).getCertification());
            }

            // Add the cast information
            // TODO: Add the people to the cast/crew
            if (moviePeople.size() > 0) {
                logger.debug(LOG_MESSAGE + "Adding " + moviePeople.size() + " people to the cast list");
                for (Person person : moviePeople) {
                    if (person.getPersonType() == PersonType.CAST) {
                        logger.trace(LOG_MESSAGE + "Adding cast member " + person.toString());
                        movie.addActor(person.getName());
                    } else if (person.getPersonType() == PersonType.CREW) {
                        logger.trace(LOG_MESSAGE + "Adding crew member " + person.toString());
                        if ("Director".equalsIgnoreCase(person.getJob())) {
                            logger.trace(LOG_MESSAGE + person.getName() + " is a Director");
                            movie.addDirector(person.getName());
                        } else if ("Author".equalsIgnoreCase(person.getJob())) {
                            logger.trace(LOG_MESSAGE + person.getName() + " is a Writer");
                            movie.addWriter(person.getName());
                            continue;
                        } else {
                            logger.trace(LOG_MESSAGE + "Unknown job  " + person.getJob() + " for " + person.toString());
                        }
                    } else {
                        logger.trace(LOG_MESSAGE + "Unknown person type " + person.getPersonType() + " for " + person.toString());
                    }
                }
            } else {
                logger.debug(LOG_MESSAGE + "No cast or crew members found");
            }

            // Update TheMovieDb Id if needed
            if (StringTools.isNotValidString(movie.getId(TMDB_PLUGIN_ID))) {
                movie.setId(TMDB_PLUGIN_ID, moviedb.getId());
            }

            // Update IMDb Id if needed
            if (StringTools.isNotValidString(movie.getId(IMDB_PLUGIN_ID))) {
                movie.setId(IMDB_PLUGIN_ID, moviedb.getImdbID());
            }
        }
        if (downloadFanart
                && StringTools.isNotValidString(movie.getFanartURL())) {
            movie.setFanartURL(getFanartURL(movie));
            if (StringTools.isValidString(movie.getFanartURL())) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
            }
        }
        return retval;
    }

    /**
     * Copy the movie info from the MovieDB bean to the YAMJ movie bean
     *
     * @param moviedb The MovieDB source
     * @param movie The YAMJ target
     * @return The altered movie bean
     */
    private void copyMovieInfo(MovieDb moviedb, Movie movie) {

        // Title
        //if (overwriteCheck(moviedb.getTitle(), movie.getTitle())) {
        movie.setTitle(moviedb.getTitle());

        // We're overwriting the title, so we should do the original name too
        movie.setOriginalTitle(moviedb.getOriginalTitle());
        //}

        // TMDb ID
        movie.setId(TMDB_PLUGIN_ID, moviedb.getId());

        // IMDb ID
        movie.setId(IMDB_PLUGIN_ID, moviedb.getImdbID());

        // plot
        if (overwriteCheck(moviedb.getOverview(), movie.getPlot())) {
            String plot = moviedb.getOverview();
            plot = StringTools.trimToLength(plot, preferredPlotLength, true, "...");
            movie.setPlot(plot);

            plot = StringTools.trimToLength(plot, preferredOutlineLength, true, "...");
            movie.setOutline(plot);
        }

        // rating
        if (overwriteCheck(String.valueOf(moviedb.getVoteAverage()), String.valueOf(movie.getRating()))) {
            try {
                float rating = moviedb.getVoteAverage() * 10; // Convert rating to integer
                movie.addRating(TMDB_PLUGIN_ID, (int) rating);
            } catch (Exception error) {
                logger.debug(LOG_MESSAGE + "Error converting rating for " + movie.getBaseName());
            }
        }

        // Release Date
        if (overwriteCheck(moviedb.getReleaseDate(), movie.getReleaseDate())) {
            movie.setReleaseDate(moviedb.getReleaseDate());
            String year = moviedb.getReleaseDate();
            // Check if this is the default year and skip it
            if (!"1900-01-01".equals(year)) {
                year = (new DateTime(year)).toString("yyyy");
                movie.setYear(year);
            } else {
                movie.setYear(Movie.UNKNOWN);
            }
        }

        // runtime
        if (overwriteCheck(String.valueOf(moviedb.getRuntime()), movie.getRuntime())) {
            movie.setRuntime(String.valueOf(moviedb.getRuntime()));
        }

        // tagline
        if (overwriteCheck(moviedb.getTagline(), movie.getTagline())) {
            movie.setTagline(moviedb.getTagline());
        }

        // Country
        List<ProductionCountry> countries = moviedb.getProductionCountries();
        if (!countries.isEmpty()) {
            String country = countries.get(0).getName();
            if (overwriteCheck(country, movie.getCountry())) {
                // This only returns one country.
                movie.setCountry(country);
            }
        }

        // Company
        List<ProductionCompany> studios = moviedb.getProductionCompanies();
        if (!studios.isEmpty()) {
            String studio = studios.get(0).getName();
            if (overwriteCheck(studio, movie.getCompany())) {
                movie.setCompany(studio);
            }
        }

        // Language
        if (moviedb.getSpokenLanguages().size() > 0) {
            String movieLanguage = moviedb.getSpokenLanguages().get(0).getIsoCode();
            if (overwriteCheck(movieLanguage, movie.getLanguage())) {
                movie.setLanguage(MovieFilenameScanner.determineLanguage(movieLanguage));
            }
            if (moviedb.getSpokenLanguages().size() > 1) {
                // There was more than one language, so output a message
                StringBuilder spokenLanguages = new StringBuilder();
                for (Language lang : moviedb.getSpokenLanguages()) {
                    spokenLanguages.append(lang.getIsoCode());
                    spokenLanguages.append("/");
                }
                spokenLanguages.deleteCharAt(spokenLanguages.length() - 1);
                logger.debug(LOG_MESSAGE + "Additional languages found and not used - " + spokenLanguages.toString());
            }
        }

        // Genres
        List<Genre> genres = moviedb.getGenres();
        if (!genres.isEmpty()) {
            if (movie.getGenres().isEmpty()) {
                for (Genre genre : genres) {
                    movie.addGenre(genre.getName());
                }
            }
        }
    }

    /**
     * Checks to see if the source string is null or "UNKNOWN" and that target string ISN'T null or "UNKNOWN"
     *
     * @param sourceString The source string to check
     * @param targetString The destination string to check
     * @return True if valid to overwrite
     */
    private boolean overwriteCheck(String sourceString, String targetString) {
        // false if the source is null or UNKNOWN
        if (StringTools.isValidString(sourceString)) {
            // sourceString is valid, check target string IS null OR UNKNOWN
            if (StringTools.isNotValidString(targetString) || targetString.equals("-1")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        int beginIndex;

        boolean result = Boolean.FALSE;

        if (StringTools.isValidString(movie.getId(TMDB_PLUGIN_ID))) {
            logger.debug(LOG_MESSAGE + "TheMovieDb ID exists for " + movie.getBaseName() + " = " + movie.getId(TMDB_PLUGIN_ID));
            result = Boolean.TRUE;
        } else {
            logger.debug(LOG_MESSAGE + "Scanning NFO for TheMovieDb ID");
            beginIndex = nfo.indexOf("/movie/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + 7)), "/ \n,:!&é\"'(--è_çà)=$");
                movie.setId(TMDB_PLUGIN_ID, st.nextToken());
                logger.debug(LOG_MESSAGE + "TheMovieDb ID found in NFO = " + movie.getId(TMDB_PLUGIN_ID));
                result = Boolean.TRUE;
            } else {
                logger.debug(LOG_MESSAGE + "No TheMovieDb ID found in NFO!");
            }
        }

        // We might as well look for the IMDb ID as well
        if (StringTools.isValidString(movie.getId(IMDB_PLUGIN_ID))) {
            logger.debug(LOG_MESSAGE + "IMDB ID exists for " + movie.getBaseName() + " = " + movie.getId(IMDB_PLUGIN_ID));
            result = Boolean.TRUE;
        } else {
            beginIndex = nfo.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + 1)), "/ \n,:!&é\"'(--è_çà)=$");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
                logger.debug(LOG_MESSAGE + "IMDB ID found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
            } else {
                beginIndex = nfo.indexOf("/Title?");
                if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                    StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + 7)), "/ \n,:!&é\"'(--è_çà)=$");
                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
                    logger.debug(LOG_MESSAGE + "IMDB ID found in NFO = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
                }
            }
        }
        return result;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        // TheMovieDB.org does not have any TV Shows, so just return
    }

    /**
     * Locate the FanartURL for the movie. This should probably be skipped as this uses TheMovieDb.org anyway
     *
     * @param movie Movie bean for the movie to locate
     * @return The URL of the fanart
     */
    protected String getFanartURL(Movie movie) {
        return FanartScanner.getFanartURL(movie);
    }

    public boolean scan(Person person) {
        return false;
    }

    @Override
    public boolean scan(com.moviejukebox.model.Person person) {
        // TODO Auto-generated method stub
        return false;
    }
}
