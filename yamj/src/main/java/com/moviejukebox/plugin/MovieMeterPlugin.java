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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.net.URLEncoder;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

/**
 * The MovieMeterPlugin uses the XML-RPC API of www.moviemeter.nl
 * (http://wiki.moviemeter.nl/index.php/API).
 *
 * Version 0.1 : Initial release Version 0.2 : Fixed google search Version 0.3 :
 * Fixed a problem when the moviemeter webservice returned no movie duration
 * (Issue 676) Version 0.4 : Fixed a problem when the moviemeter webservice
 * returned no actors (Issue 677) Added extra checks if values returned from the
 * webservice doesn't exist Version 0.5 : Added Fanart download based on imdb id
 * returned from moviemeter
 *
 * @author RdeTuinman
 *
 */
public class MovieMeterPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(MovieMeterPlugin.class);
    private static final String LOG_MESSAGE = "MovieMeterPlugin: ";
    public static final String MOVIEMETER_PLUGIN_ID = "moviemeter";
    private MovieMeterPluginSession session;
    private String preferredSearchEngine;
    private int preferredPlotLength;

    public MovieMeterPlugin() {
        super();

        preferredSearchEngine = PropertiesUtil.getProperty("moviemeter.id.search", "moviemeter");
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");

        try {
            session = new MovieMeterPluginSession();
        } catch (XmlRpcException error) {
            logger.error(SystemTools.getStackTrace(error));
        }

    }

    @Override
    public String getPluginID() {
        return MOVIEMETER_PLUGIN_ID;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean scan(Movie mediaFile) {
        logger.debug(LOG_MESSAGE + "Start fetching info from moviemeter.nl for : year=" + mediaFile.getYear() + ", title=" + mediaFile.getTitle());
        String moviemeterId = mediaFile.getId(MOVIEMETER_PLUGIN_ID);

        Map filmInfo = Collections.EMPTY_MAP;

        if (StringTools.isNotValidString(moviemeterId)) {
            logger.debug(LOG_MESSAGE + "Preferred search engine for moviemeter id: " + preferredSearchEngine);
            if ("google".equalsIgnoreCase(preferredSearchEngine)) {
                // Get moviemeter website from google
                logger.debug(LOG_MESSAGE + "Searching google.nl to get moviemeter.nl id");
                moviemeterId = getMovieMeterIdFromGoogle(mediaFile.getTitle(), mediaFile.getYear());
                logger.debug(LOG_MESSAGE + "Returned id: " + moviemeterId);
                if (StringTools.isValidString(moviemeterId)) {
                    filmInfo = session.getMovieDetailsById(Integer.parseInt(moviemeterId));
                }
            } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
                moviemeterId = Movie.UNKNOWN;
            } else {
                logger.debug(LOG_MESSAGE + "Searching moviemeter.nl for title: " + mediaFile.getTitle());
                filmInfo = session.getMovieDetailsByTitleAndYear(mediaFile.getTitle(), mediaFile.getYear());
            }
        } else {
            logger.debug(LOG_MESSAGE + "Searching moviemeter.nl for id: " + moviemeterId);
            filmInfo = session.getMovieDetailsById(Integer.parseInt(moviemeterId));
        }

        if (filmInfo != null) {
            mediaFile.setId(MOVIEMETER_PLUGIN_ID, filmInfo.get("filmId").toString());

            if (filmInfo.get("imdb") != null) {
                // if moviemeter returns the imdb id, add it to the mediaFile
                mediaFile.setId(IMDB_PLUGIN_ID, "tt" + filmInfo.get("imdb").toString());
                logger.debug(LOG_MESSAGE + "Fetched imdb id: " + mediaFile.getId(IMDB_PLUGIN_ID));
            }

            Object movieMeterTitle = filmInfo.get("title");
            if (movieMeterTitle != null) {
                if (OverrideTools.checkOverwriteTitle(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                    mediaFile.setTitle(movieMeterTitle.toString(), MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched title: " + mediaFile.getTitle());
                }
                if (OverrideTools.checkOverwriteOriginalTitle(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                    mediaFile.setOriginalTitle(movieMeterTitle.toString(), MOVIEMETER_PLUGIN_ID);
                }
            }

            if (mediaFile.getRating() == -1) {
                if (filmInfo.get("average") != null) {
                    mediaFile.addRating(MOVIEMETER_PLUGIN_ID, Math.round(Float.parseFloat(filmInfo.get("average").toString()) * 20));
                }
                logger.debug(LOG_MESSAGE + "Fetched rating: " + mediaFile.getRating());
            }

            if (OverrideTools.checkOverwriteReleaseDate(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                Object[] dates = (Object[]) filmInfo.get("dates_cinema");
                if (dates != null && dates.length > 0) {
                    HashMap dateshm = (HashMap) dates[0];
                    mediaFile.setReleaseDate(dateshm.get("date").toString(), MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched releasedate: " + mediaFile.getReleaseDate());
                }
            }

            if (OverrideTools.checkOverwriteRuntime(mediaFile, MOVIEMETER_PLUGIN_ID))  {
                if (filmInfo.get("durations") != null) {
                    Object[] durationsArray = (Object[]) filmInfo.get("durations");
                    if (durationsArray.length > 0) {
                        HashMap durations = (HashMap) (durationsArray[0]);
                        mediaFile.setRuntime(durations.get("duration").toString(), MOVIEMETER_PLUGIN_ID);
                        logger.debug(LOG_MESSAGE + "Fetched runtime: " + mediaFile.getRuntime());
                    }
                }
            }

            if (OverrideTools.checkOverwriteCountry(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("countries_text") != null) {
                    mediaFile.setCountry(filmInfo.get("countries_text").toString(), MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched country: " + mediaFile.getCountry());
                }
            }

            if (OverrideTools.checkOverwriteGenres(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("genres") != null) {
                    Object[] genres = (Object[]) filmInfo.get("genres");
                    List<String> newGenres = new ArrayList<String>();
                    for (int i = 0; i < genres.length; i++) {
                        newGenres.add(Library.getIndexingGenre(genres[i].toString()));
                    }
                    mediaFile.setGenres(newGenres, MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched genres: " + mediaFile.getGenres().toString());
                }
            }

            if (OverrideTools.checkOverwritePlot(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("plot") != null) {
                    String tmpPlot = filmInfo.get("plot").toString();
                    tmpPlot = StringTools.trimToLength(tmpPlot, preferredPlotLength, true, plotEnding);
                    mediaFile.setPlot(tmpPlot, MOVIEMETER_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteYear(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("year") != null) {
                    mediaFile.setYear(filmInfo.get("year").toString(), MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched year: " + mediaFile.getYear());
                }
            }

            if (OverrideTools.checkOverwriteActors(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                if (filmInfo.get("actors") != null) {
                    // If no actor is known, false is returned instead of an array
                    // This results in a ClassCastException
                    // So first check the Class, before casting it to an Object array
                    if (filmInfo.get("actors").getClass().equals(Object[].class)) {
                        Object[] actors = (Object[]) filmInfo.get("actors");
                        List<String> newActors = new ArrayList<String>();
                        for (int i = 0; i < actors.length; i++) {
                            newActors.add((String) (((HashMap) actors[i]).get("name")));
                        }
                        mediaFile.setCast(newActors, MOVIEMETER_PLUGIN_ID);
                        logger.debug(LOG_MESSAGE + "Fetched actors: " + mediaFile.getCast().toString());
                    }
                }
            }

            if (OverrideTools.checkOverwriteDirectors(mediaFile, MOVIEMETER_PLUGIN_ID)) {
                Object[] directors = (Object[]) filmInfo.get("directors");
                if (directors != null && directors.length > 0) {
                    HashMap directorshm = (HashMap) directors[0];
                    mediaFile.setDirector(directorshm.get("name").toString(), MOVIEMETER_PLUGIN_ID);
                    logger.debug(LOG_MESSAGE + "Fetched director: " + mediaFile.getDirector());
                }
            }

            if (downloadFanart && StringTools.isNotValidString(mediaFile.getFanartURL())) {
                mediaFile.setFanartURL(getFanartURL(mediaFile));
                if (StringTools.isValidString(mediaFile.getFanartURL())) {
                    mediaFile.setFanartFilename(mediaFile.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }

        } else {
            logger.debug(LOG_MESSAGE + "No info found");
            return false;
        }

        return true;
    }

    /**
     * Searches www.google.nl for the moviename and retreives the movie id for
     * www.moviemeter.nl.
     *
     * Only used when moviemeter.id.search=google
     *
     * @param movieName
     * @param year
     * @return
     */
    private String getMovieMeterIdFromGoogle(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.nl/search?hl=nl&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Amoviemeter.nl/film");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("www.moviemeter.nl/film/");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 23), "/\"");
            String moviemeterId = st.nextToken();

            if (isInteger(moviemeterId)) {
                return moviemeterId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving moviemeter Id from Google for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean result = false;
        logger.debug(LOG_MESSAGE + "Scanning NFO for Moviemeter Id");
        int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(MOVIEMETER_PLUGIN_ID, st.nextToken());
            logger.debug(LOG_MESSAGE + "Moviemeter Id found in nfo = " + movie.getId(MOVIEMETER_PLUGIN_ID));
            result = true;
        } else {
            logger.debug(LOG_MESSAGE + "No Moviemeter Id found in nfo !");
        }
        return result;
    }
}
