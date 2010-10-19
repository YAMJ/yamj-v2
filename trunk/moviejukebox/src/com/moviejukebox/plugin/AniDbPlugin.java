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
import java.util.logging.Logger;

import net.anidb.Anime;
import net.anidb.udp.AniDbException;
import net.anidb.udp.UdpConnection;
import net.anidb.udp.UdpConnectionException;
import net.anidb.udp.UdpConnectionFactory;
import net.anidb.udp.UdpReturnCodes;
import net.anidb.udp.mask.AnimeMask;

import org.pojava.datetime.DateTime;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * AniDB Plugin
 * @author stuart.boston
 * @version 1
 * 
 */
public class AniDbPlugin implements MovieDatabasePlugin {
    // Issue 258: http://code.google.com/p/moviejukebox/issues/detail?id=258
    // API Documentation: http://grizzlyxp.bplaced.net/projects/javaanidbapi/index.html
    // AniDb Documentation: http://wiki.anidb.info/w/UDP_API_Definition
    
    // TODO: Keep the plugin logged in until the end of the run
    
    protected static Logger logger = Logger.getLogger("moviejukebox");
    public static final String ANIDB_PLUGIN_ID = "anidb";
    private static final String anidbClientName = "yamj";
    private static final int anidbClientVersion = 1;
    private static int anidbPort = 1025;
    @SuppressWarnings("unused")
    private static final String webhost = "anidb.net";
    private static boolean anidbConnectionProtection = false;   // Set this to true to stop further calls
    private UdpConnection anidbConn;
    private AnimeMask anidbMask;
    
    @SuppressWarnings("unused")
    private int preferredPlotLength;
    private static final String logMessage = "AniDB: ";
    
    private String anidbUsername;
    private String anidbPassword;

    public AniDbPlugin() {
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
        
        try {
            anidbPort = Integer.parseInt(PropertiesUtil.getProperty("anidb.port", "1025"));
        } catch (Exception ignore) {
            anidbPort = 1025;
            logger.severe("Error setting the port to '" + PropertiesUtil.getProperty("anidb.port") + "' using default");
        }
        
        anidbMask = new AnimeMask(true, true, true, false, false, true, false, false, false, true, true, true, true, true, true, true, true, false, true, true, true, true, false, false, false, false, false, true, true, false, false, false, false, false, true, false, true, true, false, false, false, true, false);
        
        anidbUsername = PropertiesUtil.getProperty("anidb.username", null);
        anidbPassword = PropertiesUtil.getProperty("anidb.password", null);
        
        //XXX Debug
        if (anidbUsername == null && anidbPassword == null) {
            logger.severe(logMessage + "!!! USING DEFAULT USERNAME & PASSWORD !!!");
            anidbUsername = "";
            anidbPassword = "";
        }
        //XXX Debug end
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
        
        Anime anime = null;
        String id = movie.getId(ANIDB_PLUGIN_ID);
        long animeId = 0;
        if (StringTools.isValidString(id)) {
            animeId = Long.parseLong(id);
        }

        try {
            if (animeId > 0) {
                anime = getAnimeByAid(animeId);
            } else {
                anime = getAnimeByName(movie.getTitle());
                if (anime != null) {
                    animeId = anime.getAnimeId();
                    // Update the movie's Id
                    movie.setId(ANIDB_PLUGIN_ID, "" + animeId);
                }
            }
        } catch (UdpConnectionException error) {
            processUdpError(error);
        } catch (AniDbException error) {
            // We should use the return code here, but it doesn't seem to work
            if (error.getReturnCode() == UdpReturnCodes.NO_SUCH_ANIME || "NO SUCH ANIME".equals(error.getReturnString())) {
                anime = null;
            } else {
                logger.fine("Unknown AniDb Exception erorr");
                error.printStackTrace();
            }
        }
        
        if (anime != null) {
            /*
            logger.fine("getEnglishName     : " + anime.getEnglishName());
            logger.fine("getPicname         : " + anime.getPicname());
            logger.fine("getType            : " + anime.getType());
            logger.fine("getYear            : " + anime.getYear());
            logger.fine("getAirDate         : " + anime.getAirDate());
            logger.fine("Date: " + new DateTime(anime.getAirDate()).toString("dd-MM-yyyy"));
            logger.fine("getAnimeId         : " + anime.getAnimeId());
            logger.fine("getAwardList       : " + anime.getAwardList());
            logger.fine("getCategoryList    : " + anime.getCategoryList());
            logger.fine("getCharacterIdList : " + anime.getCharacterIdList());
            logger.fine("getEndDate         : " + anime.getEndDate());
            logger.fine("getEpisodes        : " + anime.getEpisodes());
            logger.fine("getProducerNameList: " + anime.getProducerNameList());
            logger.fine("getRating          : " + anime.getRating());
            */
            
            movie.setOriginalTitle(anime.getEnglishName());
            movie.setYear(anime.getYear().substring(0, 4));
            movie.setGenres(anime.getCategoryList());
            DateTime rDate = new DateTime(anime.getAirDate());
            movie.setReleaseDate(rDate.toString("yyyy-MM-dd"));
            movie.setRating((int) (anime.getRating() / 10));
            
            String plot = getAnimeDescription(animeId);
            // This plot may contain the information on the director and this needs to be stripped from the plot
            logger.fine("Plot: " + plot);
            
            movie.setPlot(plot);
            movie.setOutline(plot);
        } else {
            logger.fine("Anime not found: " + movie.getTitle());
        }
        
        // Not sure we should close this here just yet
        anidbClose(anidbConn);
        logger.fine(logMessage + "Logged out and leaving now.");
        return true;
    }
    
    /**
     * Search for the Anime description
     * @param animeId
     * @return
     */
    public String getAnimeDescription(long animeId) {
        String animePlot = Movie.UNKNOWN;
        try {
            animePlot = anidbConn.getAnimeDescription(animeId);
        } catch (UdpConnectionException error) {
            processUdpError(error);
        } catch (AniDbException error) {
            processAnidbError(error);
        }
        return animePlot;
    }
    
    public Anime getAnimeByAid(long animeId) throws UdpConnectionException, AniDbException {
        Anime anime = anidbConn.getAnime(animeId, anidbMask);
        return anime;
    }
    
    public Anime getAnimeByName(String animeName) throws UdpConnectionException, AniDbException {
        Anime anime = anidbConn.getAnime(animeName, anidbMask);
        return anime;
    }
    
    /**
     * Output a nice message for the UDP exception
     * @param error
     */
    private void processUdpError(UdpConnectionException error) {
        logger.fine("AniDbPlugin Error: " + error.getMessage());
    }
    
    /**
     * Output a nice message for the AniDb Exception
     * @param error
     */
    private void processAnidbError(AniDbException error) {
        // We should use the return code here, but it doesn't seem to work
        logger.fine("AniDbPlugin Error: " + error.getReturnString());

        int rc = error.getReturnCode();
        String rs = error.getReturnString();
        // Refactor to switch when the getReturnCode() works
        if (rc == UdpReturnCodes.NO_SUCH_ANIME || "NO SUCH ANIME".equals(rs)) {
            logger.fine("Anime not found");
        } else if (rc == UdpReturnCodes.NO_SUCH_ANIME_DESCRIPTION || "NO SUCH ANIME DESCRIPTION".equals(rs)) {
            logger.fine("Anime description not found");
        } else {
            logger.fine("Unknown error occured: " + rc + " - "+ rs);
        }
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
            conn = factory.connect(anidbPort);
            conn.authenticate(anidbUsername, anidbPassword, anidbClientName, anidbClientVersion);
            anidbConnectionProtection = false;
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
