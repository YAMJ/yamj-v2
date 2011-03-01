/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * The MovieMeterPluginSession communicates with XML-RPC webservice of www.moviemeter.nl.
 * 
 * The session is stored in a file, since the webservice accepts a maximum of 100 sessions per IP-address and
 * 50 requests per session. So when you rerun the applications, it tries to reuse the session. 
 * 
 * Version 0.1 : Initial release
 * Version 0.2 : Rewrote some log lines
 * Version 0.3 (18-06-2009) : New API key needed for MovieMeter.nl
 * Version 0.4 : Moved API key to properties file
 * @author RdeTuinman
 *
 */
public class MovieMeterPluginSession {

    public static String SESSION_FILENAME = "./temp/moviemeter.session";
    private static String MOVIEMETER_API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
    
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
        } catch (MalformedURLException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("MovieMeterPluginSession: " + eResult.toString());
        }
    }

    /**
     * Creates a new session to www.moviemeter.nl or if a session exists on disk, it is checked and resumed if valid. 
     * @throws XmlRpcException
     */
    public MovieMeterPluginSession() throws XmlRpcException {
        init();

        logger.finest("MovieMeterPluginSession: Getting stored session");
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
        } catch (IOException error) {
        }

        logger.finest("MovieMeterPluginSession: Stored session: " + getKey());

        if (!isValid()) {
            createNewSession(MOVIEMETER_API_KEY);
        }
    }

    /**
     * Creates a new session to www.moviemeter.nl
     * @param API_KEY
     * @throws XmlRpcException
     */
    @SuppressWarnings("rawtypes")
    private void createNewSession(String API_KEY) throws XmlRpcException {
        HashMap session = null;
        Object[] params = new Object[]{API_KEY};
        
        try {
            session = (HashMap) client.execute("api.startSession", params);
        } catch (Exception error) {
            logger.warning("MovieMeterPluginSession: Unable to contact website");
        }
        
        if (session != null) {
            if (session.size() > 0) {
                logger.finest("MovieMeterPluginSession: Created new session with moviemeter.nl");
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
    @SuppressWarnings("rawtypes")
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
                logger.finest("MovieMeterPluginSession: MovieMeterPlugin: Search for " + movieName + " returned " + films.length + " results");
                for (int i=0; i<films.length; i++){
                    logger.fine("Film " + i + ": " + films[i]);
                }
                // Choose first result
                result = (HashMap) films[0];
            }
        } catch (XmlRpcException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("MovieMeterPluginSession: " + eResult.toString());
        }

        return result;
    }

    /**
     * Searches www.moviemeter.nl for the movieName and matches the year. If there is no match on year, the first result is returned
     * @param movieName
     * @param year The year of the movie. If no year is known, specify null
     * @return the summary result as a HashMap
     */
    @SuppressWarnings("rawtypes")
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
                logger.finest("MovieMeterPluginSession: Searching for " + movieName + " returned " + films.length + " results");

                if (StringTools.isValidString(year)) {
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
        } catch (XmlRpcException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("MovieMeterPluginSession: " + eResult.toString());
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
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
    public HashMap getMovieDetailsById(Integer moviemeterId) {

        HashMap result = null;
        Object[] params = new Object[]{getKey(), moviemeterId};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            result = (HashMap) client.execute("film.retrieveDetails", params);
            increaseCounter();
        } catch (XmlRpcException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("MovieMeterPluginSession: " + eResult.toString());
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
        } catch (XmlRpcException error) {
            logger.finest("MovieMeterPluginSession: " + error.getMessage());
            return false;
        } catch (MalformedURLException error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("MovieMeterPluginSession: " + eResult.toString());
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
        try {
            fout = new FileOutputStream(SESSION_FILENAME);
            new PrintStream(fout).println (getKey() + "," + getTimestamp() + "," + getCounter());
            fout.close();
        } catch (FileNotFoundException ignore) {
            logger.finest("MovieMeterPluginSession: " + ignore.getMessage());
        } catch (IOException error) {
            logger.severe("MovieMeterPluginSession: " + error.getMessage());
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
