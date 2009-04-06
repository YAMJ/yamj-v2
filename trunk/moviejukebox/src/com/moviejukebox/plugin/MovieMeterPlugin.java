package com.moviejukebox.plugin;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.xmlrpc.XmlRpcException;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * The MovieMeterPlugin uses the XML-RPC API of www.moviemeter.nl (http://wiki.moviemeter.nl/index.php/API).
 * 
 * Version 0.1 : Initial release
 * Version 0.2 : Fixed google search
 * Version 0.3 : Fixed a problem when the moviemeter webservice returned no movie duration
 * @author RdeTuinman
 *
 */
public class MovieMeterPlugin extends ImdbPlugin {

    public static String MOVIEMETER_PLUGIN_ID = "moviemeter";
    private MovieMeterPluginSession session;
    protected String preferredSearchEngine;

    public MovieMeterPlugin() {
        super();

        preferredSearchEngine = PropertiesUtil.getProperty("moviemeter.id.search", "moviemeter");

        try {
            session = new MovieMeterPluginSession();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean scan(Movie mediaFile) {
        logger.finest("Start fetching info from moviemeter.nl for : year=" +mediaFile.getYear()+ ", title=" +mediaFile.getTitle());
        String moviemeterId = mediaFile.getId(MOVIEMETER_PLUGIN_ID);

        HashMap filmInfo = null;

        if (moviemeterId == null || moviemeterId.equalsIgnoreCase(Movie.UNKNOWN)) {
            logger.finest("Preferred search engine for moviemeter id: " + preferredSearchEngine);
            if ("google".equalsIgnoreCase(preferredSearchEngine)) {
                // Get moviemeter website from google
                logger.finest("Searching google.nl to get moviemeter.nl id");
                moviemeterId = getMovieMeterIdFromGoogle(mediaFile.getTitle(), mediaFile.getYear());
                logger.finest("Returned id: " + moviemeterId);
                if (moviemeterId != Movie.UNKNOWN) {
                    filmInfo = session.getMovieDetailsById(Integer.parseInt(moviemeterId));
                }
            } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
                moviemeterId = Movie.UNKNOWN;
            } else {
                logger.finest("Searching moviemeter.nl for title: " + mediaFile.getTitle());
                filmInfo = session.getMovieDetailsByTitleAndYear(mediaFile.getTitle(), mediaFile.getYear());
            }
        } else {
            logger.finest("Searching moviemeter.nl for id: " + moviemeterId);
            filmInfo = session.getMovieDetailsById(Integer.parseInt(moviemeterId));
        }

        if (filmInfo != null) {
            mediaFile.setId(MOVIEMETER_PLUGIN_ID, filmInfo.get("filmId").toString());

            if (!mediaFile.isOverrideTitle()) {
                mediaFile.setTitle(filmInfo.get("title").toString());
                mediaFile.setOriginalTitle(filmInfo.get("title").toString());
                logger.finest("Fetched title: " + mediaFile.getTitle());
            }

            if (mediaFile.getRating() == -1) {
                mediaFile.setRating(Math.round(Float.parseFloat(filmInfo.get("average").toString())*20));
                logger.finest("Fetched rating: " + mediaFile.getRating());
            }

            if (mediaFile.getReleaseDate().equals(Movie.UNKNOWN)) {
                Object[] dates = (Object[])filmInfo.get("dates_cinema");
                if (dates != null && dates.length > 0) {
                    HashMap dateshm = (HashMap)dates[0];
                    mediaFile.setReleaseDate(dateshm.get("date").toString());
                    logger.finest("Fetched releasedate: " + mediaFile.getReleaseDate());
                }
            }

            if (mediaFile.getRuntime().equals(Movie.UNKNOWN)) {
                if (filmInfo.get("durations") != null) { 
                    Object[] durationsArray = (Object[])filmInfo.get("durations");
                    if (durationsArray.length > 0) {
                        HashMap durations = (HashMap)(durationsArray[0]);
                        mediaFile.setRuntime(durations.get("duration").toString());
                    }
                }
                logger.finest("Fetched runtime: " + mediaFile.getRuntime());
            }

            if (mediaFile.getCountry().equals(Movie.UNKNOWN)) {
                mediaFile.setCountry(filmInfo.get("countries_text").toString());
                logger.finest("Fetched country: " + mediaFile.getCountry());
            }

            if (mediaFile.getGenres().isEmpty()) {
                Object[] genres = (Object[])filmInfo.get("genres");
                for (int i=0; i<genres.length; i++) {
                    mediaFile.addGenre(Library.getIndexingGenre(genres[i].toString()));
                }
                logger.finest("Fetched genres: " + mediaFile.getGenres().toString());
            }

            if (mediaFile.getPlot().equals(Movie.UNKNOWN)) {
                mediaFile.setPlot(filmInfo.get("plot").toString());
            }


            if (mediaFile.getYear() == null || mediaFile.getYear().isEmpty() || mediaFile.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setYear(filmInfo.get("year").toString());
                logger.finest("Fetched year: " + mediaFile.getYear());
            }

            if (mediaFile.getCast().isEmpty()) {
                Collection<String> newCast = new ArrayList<String>();

                Object[] actors = (Object[])filmInfo.get("actors");
                for (int i=0; i<actors.length; i++) {
                    newCast.add((String)( ((HashMap)actors[i]).get("name")));
                }
                if (newCast.size() > 0) { 
                    mediaFile.setCast(newCast);
                    logger.finest("Fetched actors: " + mediaFile.getCast().toString());
                }
            }

            if (mediaFile.getDirector().equals(Movie.UNKNOWN)) {
                Object[] directors = (Object[])filmInfo.get("directors");
                if (directors != null && directors.length > 0) {
                    HashMap directorshm = (HashMap)directors[0];
                    mediaFile.setDirector(directorshm.get("name").toString());
                    logger.finest("Fetched director: " + mediaFile.getDirector());
                }
            }
            if (mediaFile.getPosterURL() == null || mediaFile.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setPosterURL(filmInfo.get("thumbnail").toString().replaceAll("thumbs/", ""));
            }

        } else {
            logger.finest("No info found");
        }




        return true;
    }

    /**
     * Searches www.google.nl for the moviename and retreives the movie id for www.moviemeter.nl.
     * 
     * Only used when moviemeter.id.search=google
     *  
     * @param movieName
     * @param year
     * @return
     */
    private String getMovieMeterIdFromGoogle(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://www.google.nl/search?hl=nl&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
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

        } catch (Exception e) {
            logger.severe("Failed retreiving moviemeter Id from Google for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private boolean isInteger( String input )
    {
        try {
            Integer.parseInt( input );
            return true;
        } catch( Exception e) {
            return false;
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.finest("Scanning NFO for Moviemeter Id");
        int beginIndex = nfo.indexOf("www.moviemeter.nl/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(MOVIEMETER_PLUGIN_ID, st.nextToken());
            logger.finer("Moviemeter Id found in nfo = " + movie.getId(MOVIEMETER_PLUGIN_ID));
        } else {
            logger.finer("No Moviemeter Id found in nfo !");
        }
    }
}
