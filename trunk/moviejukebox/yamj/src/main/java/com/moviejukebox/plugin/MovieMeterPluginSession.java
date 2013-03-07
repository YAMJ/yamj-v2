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

import static com.moviejukebox.tools.PropertiesUtil.TRUE;

import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * The MovieMeterPluginSession communicates with XML-RPC webservice of www.moviemeter.nl.
 *
 * The session is stored in a file, since the webservice accepts a maximum of 100 sessions per IP-address and 50
 * requests per session. So when you rerun the applications, it tries to reuse the session.
 *
 * Version 0.1 : Initial release Version 0.2 : Rewrote some log lines Version 0.3 (18-06-2009) : New API key needed for
 * MovieMeter.nl Version 0.4 : Moved API key to properties file
 *
 * @author RdeTuinman
 *
 */
public final class MovieMeterPluginSession {

    private static final String SESSION_FILENAME = "./temp/moviemeter.session";
    private static final String MOVIEMETER_API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
    private static final Logger logger = Logger.getLogger(MovieMeterPluginSession.class);
    private static final String LOG_MESSAGE = "MovieMeterPluginSession: ";
    private String key;
    private Integer timestamp;
    private Integer counter;
    private XmlRpcClient client;

    /**
     * Creates the XmlRpcClient
     */
    private void init() {
        try {
            if (StringUtils.isNotBlank(WebBrowser.getMjbProxyHost())) {
                System.getProperties().put("proxySet", TRUE);
                System.getProperties().put("proxyHost", WebBrowser.getMjbProxyHost());
                System.getProperties().put("proxyPort", WebBrowser.getMjbProxyPort());
            }
            
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://www.moviemeter.nl/ws"));
            config.setConnectionTimeout(WebBrowser.getMjbTimeoutConnect());
            
            client = new XmlRpcClient();
            client.setConfig(config);
        } catch (MalformedURLException error) {
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    
    /**
     * Creates a new session to www.moviemeter.nl or if a session exists on disk, it is checked and resumed if valid.
     *
     * @throws XmlRpcException
     */
    public MovieMeterPluginSession() throws XmlRpcException {
        init();

        logger.debug(LOG_MESSAGE + "Getting stored session");
        // Read previous session
        FileReader fileRead = null;
        BufferedReader bufRead = null;

        try {
            fileRead = new FileReader(SESSION_FILENAME);
            bufRead = new BufferedReader(fileRead);

            String line = bufRead.readLine();

            // If there are no more lines of text to read, readLine() will return null.
            if (StringUtils.isNotBlank(line)) {
                String[] savedSession = line.split(",");
                if (savedSession.length == 3) {
                    setKey(savedSession[0]);
                    setTimestamp(Integer.parseInt(savedSession[1]));
                    setCounter(Integer.parseInt(savedSession[2]));
                }
            }
        } catch (IOException ex) {
            logger.debug(LOG_MESSAGE + "Error creating session: " + ex.getMessage());
        } finally {
            if (fileRead != null) {
                try {
                    fileRead.close();
                } catch (IOException ex) {
                    // Ignore the error
                }
            }

            if (bufRead != null) {
                try {
                    bufRead.close();
                } catch (IOException ex) {
                    // Ignore the error
                }
            }
        }

        logger.debug(LOG_MESSAGE + "Stored session: " + getKey());

        if (!isValid()) {
            createNewSession(MOVIEMETER_API_KEY);
        }
    }

    /**
     * Creates a new session to www.moviemeter.nl
     *
     * @param apiKey
     * @throws XmlRpcException
     */
    @SuppressWarnings("rawtypes")
    private void createNewSession(String apiKey) throws XmlRpcException {
        Map session = Collections.EMPTY_MAP;
        Object[] params = new Object[]{apiKey};

        try {
            session = (HashMap) client.execute("api.startSession", params);
        } catch (Exception error) {
            logger.warn(LOG_MESSAGE + "Unable to contact website");
        }

        if (session != null) {
            if (session.size() > 0) {
                logger.debug(LOG_MESSAGE + "Created new session with moviemeter.nl");
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
     *
     * @param movieName
     * @return the first summary result as a HashMap
     */
    @SuppressWarnings("rawtypes")
    public Map getMovieByTitle(String movieName) {

        Map result = Collections.EMPTY_MAP;
        Object[] films;
        Object[] params = new Object[]{getKey(), movieName};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            films = (Object[]) client.execute("film.search", params);
            increaseCounter();
            if (films != null && films.length > 0) {
                logger.debug(LOG_MESSAGE + "MovieMeterPlugin: Search for " + movieName + " returned " + films.length + " results");
                for (int i = 0; i < films.length; i++) {
                    logger.info("Film " + i + ": " + films[i]);
                }
                // Choose first result
                result = (HashMap) films[0];
            }
        } catch (XmlRpcException error) {
            logger.error(SystemTools.getStackTrace(error));
        }

        return result;
    }

    /**
     * Searches www.moviemeter.nl for the movieName and matches the year. If there is no match on year, the first result
     * is returned
     *
     * @param movieName
     * @param year The year of the movie. If no year is known, specify null
     * @return the summary result as a HashMap
     */
    @SuppressWarnings("rawtypes")
    public Map getMovieByTitleAndYear(String movieName, String year) {

        Map result = Collections.EMPTY_MAP;
        Object[] films;
        Object[] params = new Object[]{getKey(), movieName};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            films = (Object[]) client.execute("film.search", params);
            increaseCounter();
            if (films != null && films.length > 0) {
                logger.debug(LOG_MESSAGE + "Searching for " + movieName + " returned " + films.length + " results");

                if (StringTools.isValidString(year)) {
                    for (int i = 0; i < films.length; i++) {
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
            logger.error(SystemTools.getStackTrace(error));
        }

        return result;
    }

    /**
     * Searches www.moviemeter.nl for the movieName and matches the year. If there is no match on year, the first result
     * is returned
     *
     * @param movieName
     * @param year
     * @return the detailed result as a HashMap
     */
    @SuppressWarnings("rawtypes")
    public Map getMovieDetailsByTitleAndYear(String movieName, String year) {

        Map result = Collections.EMPTY_MAP;
        Map filmInfo = getMovieByTitleAndYear(movieName, year);

        if (filmInfo != null) {
            result = getMovieDetailsById(Integer.parseInt((String) filmInfo.get("filmId")));
        }

        return result;
    }

    /**
     * Given the moviemeterId this returns the detailed result of www.moviemeter.nl
     *
     * @param moviemeterId
     * @return the detailed result as a HashMap
     */
    @SuppressWarnings("rawtypes")
    public Map getMovieDetailsById(Integer moviemeterId) {

        Map result = Collections.EMPTY_MAP;
        Object[] params = new Object[]{getKey(), moviemeterId};
        try {
            if (!isValid()) {
                createNewSession(MOVIEMETER_API_KEY);
            }
            result = (HashMap) client.execute("film.retrieveDetails", params);
            increaseCounter();
        } catch (XmlRpcException error) {
            logger.error(SystemTools.getStackTrace(error));
        }

        return result;
    }

    /**
     * Checks if the current session is valid
     *
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
            XmlRpcClientConfigImpl validConfig = new XmlRpcClientConfigImpl();
            validConfig.setServerURL(new URL("http://www.moviemeter.nl/ws"));
            XmlRpcClient validClient = new XmlRpcClient();
            validClient.setConfig(validConfig);

            Object[] params = new Object[]{getKey(), ""};
            validClient.execute("film.search", params);
            increaseCounter();

            return true;
        } catch (XmlRpcException error) {
            logger.debug(LOG_MESSAGE + "" + error.getMessage());
            return false;
        } catch (MalformedURLException error) {
            logger.error(SystemTools.getStackTrace(error));
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
        FileOutputStream fout = null;
        PrintStream ps = null;

        try {
            fout = new FileOutputStream(SESSION_FILENAME);
            ps = new PrintStream(fout);
            ps.println(getKey() + "," + getTimestamp() + "," + getCounter());
        } catch (FileNotFoundException ignore) {
            logger.debug(LOG_MESSAGE + "" + ignore.getMessage());
        } finally {
            if (ps != null) {
                ps.close();
            }

            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    // Ignore the error
                }
            }
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
