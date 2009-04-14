package com.moviejukebox.plugin;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.moviejukebox.model.Movie;

/**
 * The MovieMeterPluginSession communicates with XML-RPC webservice of www.moviemeter.nl.
 * 
 * The session is stored in a file, since the webservice accepts a maximum of 100 sessions per IP-address and
 * 50 requests per session. So when you rerun the applications, it tries to reuse the session. 
 * 
 * Version 0.1 : Initial release
 * Version 0.2 : Rewrote some log lines
 * @author RdeTuinman
 *
 */
public class MovieMeterPluginSession {

    public static String SESSION_FILENAME = "./temp/moviemeter.session";
    private static String MOVIEMETER_API_KEY = "yyzpp3k74vvg159zwxzyxgmweafxby1x";
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private String key;
    private Integer timestamp;
    private Integer counter;
    private XmlRpcClientConfigImpl config;
    private XmlRpcClient client;

    /**
    * Creates the XmlRpcClient
    */
    private void init() {
        try {
            config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://www.moviemeter.nl/ws"));
            client = new XmlRpcClient();
            client.setConfig(config);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new session to www.moviemeter.nl or if a session exists on disk, it is checked and resumed if valid. 
     * @throws XmlRpcException
     */
    public MovieMeterPluginSession() throws XmlRpcException {
        init();

        logger.finest("MovieMeterPlugin: Getting stored session");
        // Read previous session
        FileReader fread;        
        try
        {
            fread = new FileReader (SESSION_FILENAME);
            String line = new BufferedReader(fread).readLine();

            String[] savedSession = line.split(",");
            if (savedSession.length == 3) {
                setKey(savedSession[0]);
                setTimestamp(Integer.parseInt(savedSession[1]));
                setCounter(Integer.parseInt(savedSession[2]));
            }
            fread.close();        
        } catch (IOException e) {
        }

        logger.finest("MovieMeterPlugin: Stored session: " + getKey());

        if (!isValid()) {
            createNewSession(MOVIEMETER_API_KEY);
        }
    }

    /**
     * Creates a new session to www.moviemeter.nl
     * @param apiKey
     * @throws XmlRpcException
     */
    @SuppressWarnings("unchecked")
    private void createNewSession(String apiKey) throws XmlRpcException {
        Object[] params = new Object[]{apiKey};
        HashMap session = (HashMap) client.execute("api.startSession", params);
        if (session != null) {
            if (session.size() > 0) {
            	logger.finest("Created new session with moviemeter.nl");
                setKey((String) session.get("session_key"));
                setTimestamp((Integer) session.get("valid_till"));
                setCounter(0);
                // use of this API is free for non-commercial use
                // see http://wiki.moviemeter.nl/index.php/API for more info
                saveSessionToFile();
            }
        } else {
            throw new XmlRpcException("api.startSession returned null");
        }
    }

    /**
     * Searches www.moviemeter.nl for the movieName 
     * @param movieName
     * @return the first summary result as a HashMap
     */
    @SuppressWarnings("unchecked")
    public HashMap getMovieByTitle(String movieName) {

        HashMap result = null;
        Object[] films = null;
        Object[] params = new Object[]{getKey(), movieName};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            films = (Object[]) client.execute("film.search", params);
            increaseCounter();
            if (films != null && films.length>0) {
                logger.finest("MovieMeterPlugin: Search for " + movieName + " returned " + films.length + " results");
                for (int i=0; i<films.length; i++){
                    logger.fine("Film " + i + ": " + films[i]);
                }
                // Choose first result
                result = (HashMap) films[0];
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Searches www.moviemeter.nl for the movieName and matches the year. If there is no match on year, the first result is returned
     * @param movieName
     * @param year The year of the movie. If no year is known, specify null
     * @return the summary result as a HashMap
     */
    @SuppressWarnings("unchecked")
    public HashMap getMovieByTitleAndYear(String movieName, String year) {

        HashMap result = null;
        Object[] films = null;
        Object[] params = new Object[]{getKey(), movieName};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            films = (Object[]) client.execute("film.search", params);
            increaseCounter();
            if (films != null && films.length>0) {
                logger.finest("Moviemeter search for " + movieName + " returned " + films.length + " results");
                if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                    for (int i=0; i<films.length; i++){
                        HashMap film = (HashMap) films[i];
                        if (film.get("year").toString().equals(year)) {
                            // Probably best match
                            return film;
                        }
                    }
                }
                // Choose first result
                result = (HashMap) films[0];
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Searches www.moviemeter.nl for the movieName and matches the year. If there is no match on year, the first result is returned
     * 
     * @param movieName
     * @param year
     * @return the detailed result as a HashMap
     */
    @SuppressWarnings("unchecked")
    public HashMap getMovieDetailsByTitleAndYear(String movieName, String year) {

        HashMap result = null;
        HashMap filmInfo = getMovieByTitleAndYear(movieName, year);

        if (filmInfo != null) {
            result = getMovieDetailsById(Integer.parseInt((String)filmInfo.get("filmId")));
        }

        return result;
    }

    /**
     * Given the moviemeterId this returns the detailed result of www.moviemeter.nl  
     * @param moviemeterId
     * @return the detailed result as a HashMap
     */
    @SuppressWarnings("unchecked")
    public HashMap getMovieDetailsById(Integer moviemeterId) {

        HashMap result = null;
        Object[] params = new Object[]{getKey(), moviemeterId};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            result = (HashMap) client.execute("film.retrieveDetails", params);
            increaseCounter();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Checks if the current session is valid
     * @return true of false
     */
    public boolean isValid() {
        if (getKey() == null || getKey().equals("")) {
            return false;
        }

        if ((System.currentTimeMillis() / 1000) < getTimestamp()) {
            // Timestamp still valid
            if (counter < 48) {
                return true;
            } else {
                return false;
            }
        }

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://www.moviemeter.nl/ws"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            Object[] params = new Object[]{getKey(), new String("")};
            client.execute("film.search", params);
            increaseCounter();

            return true;
        } catch (XmlRpcException e) {
            logger.finest(e.getMessage());
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void increaseCounter() {
        counter++;
        saveSessionToFile();
    }

    /**
     * Saves the session details to disk
     */
    private void saveSessionToFile() {
        FileOutputStream fout;
        try
        {
            fout = new FileOutputStream (SESSION_FILENAME);
            new PrintStream(fout).println (getKey() + "," + getTimestamp() + "," + getCounter());
            fout.close();        
        } catch (IOException e) {
        	logger.severe(e.getMessage());
        }        
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public Integer getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getCounter() {
        return counter;
    }

    private void setCounter(Integer counter) {
        this.counter = counter;
    }

}
