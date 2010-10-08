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

import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.anidb.Anime;
import net.anidb.udp.AniDbException;
import net.anidb.udp.UdpConnection;
import net.anidb.udp.UdpConnectionException;
import net.anidb.udp.UdpConnectionFactory;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * AniDB Plugin
 * @author stuart.boston
 * @version 1
 * 
 */
public class AniDbPlugin implements MovieDatabasePlugin {
    // API Documentation: http://grizzlyxp.bplaced.net/projects/javaanidbapi/index.html
    // AniDb Documentation: http://wiki.anidb.info/w/UDP_API_Definition
    
    protected static Logger logger = Logger.getLogger("moviejukebox");
    public static final String ANIDB_PLUGIN_ID = "anidb";
    private static final String anidbClientName = "yamj";
    private static final int anidbClientVersion = 1;
    private static final String webhost = "anidb.net";
    private static boolean anidbConnectionProtection = false;   // Set this to true to stop further calls
    
    private int preferredPlotLength;
    private static final String logMessage = "AniDB: ";
    
    private String anidbUsername;
    private String anidbPassword;

    public AniDbPlugin() {
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
        anidbUsername = PropertiesUtil.getProperty("anidb.username", null);
        anidbPassword = PropertiesUtil.getProperty("anidb.password", null);
        
        //XXX Debug
        if (anidbUsername == null && anidbPassword == null) {
            logger.severe(logMessage + "USING DEFAULT USERNAME & PASSWORD");
            anidbUsername = "omertron";
            anidbPassword = "anidb";
        }
    }
    
    @Override
    public boolean scan(Movie movie) {
        logger.fine(logMessage + "Scanning as a Movie");
        return anidbScan(movie);
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        logger.fine(logMessage + "Scanning as a TV Show");
        anidbScan(movie);
        return;
    }
    
    /**
     * Generic scan routine
     * @param movie
     */
    private boolean anidbScan(Movie movie) {
        UdpConnection anidbConn;
        
        // This is required to prevent the client from being banned through overuse
        if (!anidbConnectionProtection) {
            anidbConn = anidbOpen();
        } else {
            logger.fine("There was an error with the connection, no more connections will be attempted!");
            return false;
        }
        
        if (anidbConn == null) {
            return false;
        }
        
        // Now process the movie
        logger.fine(logMessage + "Logged in and searching for " + movie.getBaseFilename());
        
        String id = movie.getId(ANIDB_PLUGIN_ID);
        if (movie.getId(ANIDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
            // Search for the anime by name
            // If we find it, set the movie id
            // set the local id variable
        }
        
        if (!id.equals(Movie.UNKNOWN)) {
            long animeId = Long.parseLong(id);
            try {
                Anime anime = anidbConn.getAnime(animeId);
            } catch (UdpConnectionException error) {
                // TODO Auto-generated catch block
                error.printStackTrace();
            } catch (AniDbException error) {
                // TODO Auto-generated catch block
                error.printStackTrace();
            }
        }
        
        
        anidbClose(anidbConn);
        logger.fine(logMessage + "Logged out and leaving now.");
        return true;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.finest(logMessage + "Scanning NFO for AniDb Id");
        int beginIndex = nfo.indexOf("aid=");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 4), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
            movie.setId(ANIDB_PLUGIN_ID, st.nextToken());
            logger.finer(logMessage + "AniDb Id found in nfo = " + movie.getId(ANIDB_PLUGIN_ID));
        } else {
            logger.finer(logMessage + "No AniDb Id found in nfo!");
        }
    }

    /**
     * Open the connection to the website
     * @return a connection object, or null if there was a failure.
     */
    private UdpConnection anidbOpen() {
        UdpConnectionFactory factory;
        UdpConnection conn = null;
        
        factory = UdpConnectionFactory.getInstance();
        try {
            conn = factory.connect(1025);
            conn.authenticate(anidbUsername, anidbPassword, anidbClientName, anidbClientVersion);
        } catch (IllegalArgumentException error) {
            logger.severe(logMessage + "Error logging in, please check you username & password");
            logger.severe(logMessage + error.getMessage());
            anidbConnectionProtection = true;
        } catch (UdpConnectionException error) {
            logger.severe(logMessage + "Error with UDP Connection, please try again later");
            logger.severe(logMessage + error.getMessage());
            anidbConnectionProtection = true;
        } catch (AniDbException error) {
            logger.severe(logMessage + "Error with AniDb: " + error.getMessage());
            anidbConnectionProtection = true;
        } catch (Exception error) {
            error.printStackTrace();
        } finally {
            anidbClose(conn);
        }
        return conn;
    }

    /**
     * Close the connection to the website
     * @param conn
     */
    private void anidbClose(UdpConnection conn) {
        anidbLogout(conn);
        // Now close the connection
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Try and log the user out
     * @param conn
     */
    private void anidbLogout(UdpConnection conn) {
        if (conn == null) {
            return;
        }
        
        // If the user isn't logged in an exception is thrown which we can ignore
        try {
            conn.logout();
        } catch (Exception ignore) {
            // We don't care about this exception
        }
    }

}
