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

import static com.moviejukebox.tools.StringTools.cleanString;
import static com.moviejukebox.tools.StringTools.isValidString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.anidb.Anime;
import net.anidb.Episode;
import net.anidb.checksum.Ed2kChecksum;
import net.anidb.udp.AniDbException;
import net.anidb.udp.UdpConnection;
import net.anidb.udp.UdpConnectionException;
import net.anidb.udp.UdpConnectionFactory;
import net.anidb.udp.UdpReturnCodes;
import net.anidb.udp.mask.AnimeFileMask;
import net.anidb.udp.mask.AnimeMask;
import net.anidb.udp.mask.FileMask;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.Cache;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * AniDB Plugin
 * @author stuart.boston
 * @author Xaanin
 * @version 2
 * 
 */
public class AniDbPlugin implements MovieDatabasePlugin {
    // Issue 258: http://code.google.com/p/moviejukebox/issues/detail?id=258
    // API Documentation: http://grizzlyxp.bplaced.net/projects/javaanidbapi/index.html
    // AniDb Documentation: http://wiki.anidb.info/w/UDP_API_Definition
    
    // TODO: Keep the plugin logged in until the end of the run
    
    private static Logger logger = Logger.getLogger("moviejukebox");
    public static final String ANIDB_PLUGIN_ID = "anidb";
    private static final String ANIDB_CLIENT_NAME = "yamj";
    private static final int ANIDB_CLIENT_VERSION = 1;
    private static int anidbPort;
    private static final int ED2K_CHUNK_SIZE = 9728000;
    @SuppressWarnings("unused")
    private static final String WEBHOST = "anidb.net";
    private AnimeMask anidbMask;
    private AnimeMask categoryMask;
    private AnimeFileMask animeFileMask;
    private FileMask fileMask;
    @SuppressWarnings("unused")
    private static final String PICTURE_URL_BASE = "http://1.2.3.12/bmi/img7.anidb.net/pics/anime/";
    
    private static final String THETVDB_ANIDB_MAPPING_URL = "e:\\downloads\\anime-list.xml";//"http://sites.google.com/site/anidblist/anime-list.xml";
    
    @SuppressWarnings("unused")
    private int preferredPlotLength;
    private static final String LOG_MESSAGE = "AniDbPlugin: ";
    
    private static UdpConnection anidbConn = null;
    private static boolean anidbConnectionProtection = false;   // Set this to true to stop further calls
    private static String anidbUsername;
    private static String anidbPassword;

    private static boolean hash;
    private boolean getAdditionalInformationFromTheTvDB = false;

    private static final int TABLE_VERSION = 1;
    private Dao<AnidbLocalFile, String> localFileDao;
    private Dao<AnidbFile, String> anidbFileDao;
    private Dao<AnidbAnime, String> animeDao;
    private Dao<AnidbEpisode, String> episodeDao;
    private Dao<AnidbCategory, String> categoryDao;
    private Dao<AnidbTvdbMapping, String> mappingDao;
    private Dao<AnidbTvdbEpisodeMapping, String> episodeMappingDao;

    private Pattern tvshowRegex;
    private Pattern movieRegex;
    private int tvshowRegexTitleIndex;
    private int tvshowRegexEpisodeNumberIndex;
    private int movieRegexTitleIndex;
    
    private int minimumCategoryWeight;
    private int maxGenres;
    
    private TheTvDBPlugin tvdb;
    
    public AniDbPlugin() {
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        
        try {
            anidbPort = PropertiesUtil.getIntProperty("anidb.port", "1025");
        } catch (Exception ignore) {
            anidbPort = 1025;
            logger.error(LOG_MESSAGE + "Error setting the port to '" + PropertiesUtil.getProperty("anidb.port") + "' using default");
        }
        
        anidbMask = new AnimeMask(true, true, true, false, false, true, true, true, true, true, false, false, false, true, true, true, 
                        true, false, false, false, true, true, false, false, false, false, false, true, true, false, false, false, false, 
                        false, true, false, true, true, false, false, false, false, false);
        categoryMask = new AnimeMask(false, false, false, false, false, true, true, false, false, false, false, false, false, false, false,
                        false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
                        false, false, false, false, false, false, false, false, false, false, false);
        animeFileMask = new AnimeFileMask(true, false, true, true, false, false, true, true, true, true, false, false, false, true, true, true, true, true, true, false, false, false);

        fileMask = new FileMask(true, true, true, false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false);
        anidbUsername = PropertiesUtil.getProperty("anidb.username", null);
        anidbPassword = PropertiesUtil.getProperty("anidb.password", null);
        //String str = PropertiesUtil.getProperty("anidb.useHashIdentification", null);
        hash = PropertiesUtil.getBooleanProperty("anidb.useHashIdentification", "false");
        
        minimumCategoryWeight = PropertiesUtil.getIntProperty("anidb.minimumCategoryWeight", "0");
        maxGenres = PropertiesUtil.getIntProperty("anidb.maxGenres", "5");
        
        String tvshowRegexOverride = PropertiesUtil.getProperty("anidb.regex.tvshow", null);
        String movieRegexOverride = PropertiesUtil.getProperty("anidb.regex.movie", null);
        if (isValidString(tvshowRegexOverride)) {
            tvshowRegex = Pattern.compile(tvshowRegexOverride);
            // This is a temporary workaround, it should be replaced with named capture groups once
            // java gets them
            String tvshowIndexes = PropertiesUtil.getProperty("anidb.regex.tvshow.index", null);
            if (tvshowIndexes != null) {
                String[] t = tvshowIndexes.split(",");
                if (t.length != 2) {
                    logger.error(LOG_MESSAGE + "Invalid anidb.regex.tvshow.index variable in properties file. Ignoring custom regex");
                    tvshowRegex = null;
                } else {
                    tvshowRegexTitleIndex = Integer.parseInt(t[0]);
                    tvshowRegexEpisodeNumberIndex = Integer.parseInt(t[1]);
                }
            }            
        }
        if (isValidString(movieRegexOverride)) {
            movieRegex = Pattern.compile(movieRegexOverride);
            movieRegexTitleIndex = PropertiesUtil.getIntProperty("anidb.regex.movie.index", "-1");
            if(movieRegexTitleIndex < 0) {
                logger.error(LOG_MESSAGE + "Invalid anidb.regex.movie.index variable in properties file. Ignoring custom regex");
                movieRegex = null;
            }
        }

        if (anidbUsername == null || anidbPassword == null) {
            logger.error(LOG_MESSAGE + "You need to add your AniDb Username & password to the anidb.username & anidb.password properties");
            anidbConnectionProtection = true;
        }
        setupDatabase();
        if (getAdditionalInformationFromTheTvDB) {
            //loadAnidbTvdbMappings();
            tvdb = new TheTvDBPlugin();
        }
    }
    
    private void setupDatabase() {
        /*
         * Lets just pray that the current directory is writable
         * TODO: Implement an override for this in the properties file
         */
        String dbUrl = "jdbc:sqlite:yamj_anidb.db";
        ConnectionSource connectionSource;
        try {
            connectionSource = new JdbcConnectionSource(dbUrl);
            updateTables(connectionSource);
            localFileDao = DaoManager.<Dao<AnidbLocalFile, String>, AnidbLocalFile>createDao(connectionSource, AnidbLocalFile.class);
            animeDao = DaoManager.<Dao<AnidbAnime, String>, AnidbAnime>createDao(connectionSource, AnidbAnime.class);
            episodeDao = DaoManager.<Dao<AnidbEpisode, String>, AnidbEpisode>createDao(connectionSource, AnidbEpisode.class);
            anidbFileDao = DaoManager.<Dao<AnidbFile, String>, AnidbFile>createDao(connectionSource, AnidbFile.class);
            categoryDao = DaoManager.<Dao<AnidbCategory, String>, AnidbCategory>createDao(connectionSource, AnidbCategory.class);
            mappingDao = DaoManager.<Dao<AnidbTvdbMapping, String>, AnidbTvdbMapping>createDao(connectionSource, AnidbTvdbMapping.class);
            episodeMappingDao = DaoManager.<Dao<AnidbTvdbEpisodeMapping, String>, AnidbTvdbEpisodeMapping>createDao(connectionSource, AnidbTvdbEpisodeMapping.class);
        } catch (SQLException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /*
     * Method to check if the database tables are old and need updating, and performs
     * the update if it's needed.
     */
    private static synchronized void updateTables(ConnectionSource connectionSource) {
        try {
            Dao<AnidbTableInfo, String> tableDao = DaoManager.<Dao<AnidbTableInfo, String>, AnidbTableInfo>createDao(connectionSource, AnidbTableInfo.class);
            boolean dbUpdate = true;
            AnidbTableInfo info = null;
            int version = -1;
            if (tableDao.isTableExists()) {
                info = tableDao.queryForId(Integer.toString(AniDbPlugin.TABLE_VERSION));
                if (info != null) {
                    dbUpdate = false;
                } else {
                    info = tableDao.queryForAll().get(0);
                    if (info.getVersion() == AniDbPlugin.TABLE_VERSION) {
                        dbUpdate = false;
                    } else {
                        version = info.getVersion();
                    }
                }
            }
            if (dbUpdate) {
                switch (version) {
                case -1: // DB Doesn't exist, create from scratch
                    TableUtils.createTable(connectionSource, AnidbLocalFile.class);
                    TableUtils.createTable(connectionSource, AnidbEpisode.class);
                    TableUtils.createTable(connectionSource, AnidbAnime.class);
                    TableUtils.createTable(connectionSource, AnidbTableInfo.class);
                    TableUtils.createTable(connectionSource, AnidbFile.class);
                    TableUtils.createTable(connectionSource, AnidbCategory.class);
                    TableUtils.createTable(connectionSource, AnidbTvdbMapping.class);
                    TableUtils.createTable(connectionSource, AnidbTvdbEpisodeMapping.class);
                    info = new AnidbTableInfo();
                    info.setVersion(AniDbPlugin.TABLE_VERSION);
                    tableDao.create(info);
                    break;
                default:
                    break;
                }
            }
            
        } catch (SQLException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }
    
    @Override
    public boolean scan(Movie movie) {
        logger.info(LOG_MESSAGE + "Scanning as a Movie");
        return anidbScan(movie);
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        logger.info(LOG_MESSAGE + "Scanning as a TV Show");
        anidbScan(movie);
        return;
    }
    
    /**
     * Generic scan routine
     * 
     * @param movie
     */
    private boolean anidbScan(Movie movie) {
        // This is required to prevent the client from being banned through overuse
        if (!anidbConnectionProtection) {
            anidbOpen();
        } else {
            logger.info(LOG_MESSAGE + "There was an error with the connection, no more connections will be attempted!");
            return false;
        }

        if (anidbConn == null) {
            return false;
        }

        // Now process the movie
        logger.info(LOG_MESSAGE + "Logged in and searching for " + movie.getBaseFilename());
        String episodeNumber = "";
        if (hash) {
            AnidbFile af = anidbHashScan(movie); 
            if (af == null) {
                return false;
            }
            try {
                AnidbEpisode ae = getEpisodeByEid(af.getEpisodeId());
                episodeNumber = ae.getEpisodeNumber();
            } catch (UdpConnectionException e) {
                processUdpError(e);
            } catch (AniDbException e) {
                logger.info(LOG_MESSAGE + "Unknown AniDb Exception erorr");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                e.printStackTrace(printWriter);
                logger.error(eResult.toString());
            } catch (SQLException e) {
                logException("Sql error when performing episode lookup",e);
            }
        } else {
            if (tvshowRegex != null) {
                Matcher m = tvshowRegex.matcher(movie.getBaseFilename());
                if (m.find()) { // TV-show
                    episodeNumber = m.group(tvshowRegexEpisodeNumberIndex);
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    movie.setTitle(cleanString(m.group(tvshowRegexTitleIndex)));
                } else if(movieRegex != null) {
                    m = movieRegex.matcher(movie.getBaseFilename());
                    if (m.find()) {
                        movie.setTitle(cleanString(m.group(movieRegexTitleIndex)));
                        movie.setMovieType(Movie.TYPE_MOVIE);
                    }
                }
            } 
        }

        AnidbAnime anime = null;
        String id = movie.getId(ANIDB_PLUGIN_ID);
        long animeId = 0;
        if (isValidString(id)) {
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
                logger.info(LOG_MESSAGE + "Unknown AniDb Exception erorr");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
        } catch (SQLException error) {
            logException("SQL error when looking up anime id", error);
        }

        if (anime != null) {
            if (anime.getType().equals("Movie")) { // Assume anything not a movie is a TV show
                movie.setMovieType(Movie.TYPE_MOVIE);
            }
            else {
                movie.setMovieType(Movie.TYPE_TVSHOW);
            }
            // XXX: DEBUG
            logger.info("getAnimeId         : " + anime.getAnimeId());
            logger.info("getEnglishName     : " + anime.getEnglishName());
            // logger.info("getPicname         : " + anime.getPicname());
            logger.info("getType            : " + anime.getType());
            logger.info("getYear            : " + anime.getYear());
            logger.info("getAirDate         : " + anime.getAirDate());
            logger.info("Date               : " + new DateTime(anime.getAirDate()).toString("dd-MM-yyyy"));
            // logger.info("getAwardList       : " + anime.getAwardList());
            // logger.info("getCategoryList    : " + anime.getCategoryList());
            // logger.info("getCharacterIdList : " + anime.getCharacterIdList());
            logger.info("getEndDate         : " + anime.getEndDate());
            logger.info("getEpisodes        : " + anime.getEpisodeCount());
            // logger.info("getProducerNameList: " + anime.getProducerNameList());
            logger.info("getRating          : " + anime.getRating());
            // XXX: DEBUG END

            movie.setId(ANIDB_PLUGIN_ID, "" + anime.getAnimeId());

            /*if (tvdbMappings.get(anime.getAnimeId()) != null) {
                movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, Long.toString(tvdbMappings.get(anime.getAnimeId()).tvDbId));
                movie.setSeason(1);
            }*/
            if (getAdditionalInformationFromTheTvDB) {
                movie.setTitle(anime.getRomajiName());
                tvdb.scan(movie);
            }

            if (isValidString(anime.getEnglishName())) {
                movie.setOriginalTitle(anime.getEnglishName());
            } else if (isValidString(anime.getRomajiName())) {
                movie.setOriginalTitle(anime.getRomajiName());
            } else {
                logger.error(LOG_MESSAGE + "Encountered an anime without a valid title. Anime ID: " + anime.getAnimeId());
            }

            if (isValidString(anime.getYear())) {
                movie.setYear(new String(anime.getYear().substring(0, 4)));
            }

            final List<String> categories = new ArrayList<String>();
            for (int i = 0; i < anime.getCategories().size() && i < maxGenres && anime.getCategories().get(i).getWeight() >= minimumCategoryWeight; ++i) {
                categories.add(anime.getCategories().get(i).getCategoryName());
            }
            movie.setGenres(categories);
            
            if (anime.getAirDate() > 0) {
                DateTime rDate = new DateTime(anime.getAirDate());
                movie.setReleaseDate(rDate.toString("yyyy-MM-dd"));
            }

            if (anime.getRating() > 0) {
                movie.setRating((int)(anime.getRating() / 10));
            }

            if (!getAdditionalInformationFromTheTvDB) {
                movie.setPlot(anime.getDescription());
                movie.setOutline(anime.getDescription());
            }

            for (MovieFile mf : movie.getFiles()) {
                logger.info("File : " + mf.getFilename()); // XXX: DEBUG
                logger.info("First: " + mf.getFirstPart()); // XXX: DEBUG
                logger.info("Last : " + mf.getLastPart()); // XXX: DEBUG
            }

        } else {
            logger.info(LOG_MESSAGE + "Anime not found: " + movie.getTitle());
        }

        logger.info(LOG_MESSAGE + "Finished " + movie.getBaseFilename());
        return true;
    }
    
    // TODO: Create a separate class to handle all these database queries
    private AnidbFile anidbHashScan(Movie movie) {
        try {
            AnidbLocalFile localFile = loadLocalFile(movie.getFile());

            String ed2kHash = null;
            AnidbFile file = null;
            if (localFile == null) {
                ed2kHash = getEd2kChecksum(movie.getFile());
                if (ed2kHash.equals("")) {
                    return null;
                }
                localFile = loadLocalFile(movie.getFile(), ed2kHash);
            }

            file = getAnimeEpisodeByHash(movie.getFile().length(), localFile == null ? ed2kHash : localFile.getEd2k());
            return file;
        } catch (UdpConnectionException error) {
            logger.info(LOG_MESSAGE + "UDP Connection Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return null;
        } catch (AniDbException error) {
            logger.info(LOG_MESSAGE + "AniDb Exception Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return null;
        } catch (SQLException error) {
            logger.info(LOG_MESSAGE + "AniDb Exception Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return null;
        }
    }

    private String getEd2kChecksum(File file) {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(file);
            Ed2kChecksum ed2kChecksum = new Ed2kChecksum();
            byte[] buffer = new byte[ED2K_CHUNK_SIZE];
            int k = -1;
            while ((k = fi.read(buffer, 0, buffer.length)) > 0) {
                ed2kChecksum.update(buffer, 0, k);
            }
            return ed2kChecksum.getHexDigest();
        } catch (FileNotFoundException e) {
            // This shouldn't happen
            logger.error(LOG_MESSAGE + "Unable to find the file " + file.getAbsolutePath());
        } catch (IOException error) {
            logger.error(LOG_MESSAGE + "Encountered an IO-error while reading file " + file.getAbsolutePath());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        } finally {
            if (fi != null) {
                IOUtils.closeQuietly(fi);
            }
        }
        return "";
    }

    public AnidbAnime getAnimeByAid(long animeId) throws UdpConnectionException, AniDbException, SQLException {
        return loadAnidbAnime(animeId);
    }
    
    public AnidbAnime getAnimeByName(String animeName) throws UdpConnectionException, AniDbException, SQLException {
        return loadAnidbAnime(animeName);
    }
    
    public AnidbFile getAnimeEpisodeByHash(long size, String hash) throws UdpConnectionException, AniDbException, SQLException {
        return loadAnidbFile(hash, size);
    }
    
    public AnidbEpisode getEpisodeByEid(long eid) throws UdpConnectionException, AniDbException, SQLException{
        return loadAnidbEpisode(eid);
    }
    
    /**
     * Get the episode details by AnimeID and Episode Number
     * @param animeId
     * @param episodeNumber
     * @return
     * @throws UdpConnectionException
     * @throws AniDbException
     */
    @Deprecated
    public Episode getEpisode(long animeId, long episodeNumber) throws UdpConnectionException, AniDbException {
        return anidbConn.getEpisode(animeId, episodeNumber);
    }
    
    /**
     * Get the episode details by Anime Name and Episode Number
     * @param animeName
     * @param episodeNumber
     * @return
     * @throws UdpConnectionException
     * @throws AniDbException
     */
    public Episode getEpisode(String animeName, long episodeNumber) throws UdpConnectionException, AniDbException {
        return anidbConn.getEpisode(animeName, episodeNumber);
    }
    
    public net.anidb.Character getCharacter(long characterId) throws UdpConnectionException, AniDbException {
        return anidbConn.getCharacter(characterId);
    }
    
    /**
     * Output a nice message for the UDP exception
     * @param error
     */
    private void processUdpError(UdpConnectionException error) {
        logger.info(LOG_MESSAGE + "Error: " + error.getMessage());
    }
    
    /**
     * Output a nice message for the AniDb Exception
     * @param error
     */
    private void processAnidbError(AniDbException error) {
        // We should use the return code here, but it doesn't seem to work
        logger.info(LOG_MESSAGE + "Error: " + error.getReturnString());

        int rc = error.getReturnCode();
        String rs = error.getReturnString();
        // Refactor to switch when the getReturnCode() works
        if (rc == UdpReturnCodes.NO_SUCH_ANIME || "NO SUCH ANIME".equals(rs)) {
            logger.info(LOG_MESSAGE + "Anime not found");
        } else if (rc == UdpReturnCodes.NO_SUCH_ANIME_DESCRIPTION || "NO SUCH ANIME DESCRIPTION".equals(rs)) {
            logger.info(LOG_MESSAGE + "Anime description not found");
        } else {
            logger.info(LOG_MESSAGE + "Unknown error occured: " + rc + " - "+ rs);
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.debug(LOG_MESSAGE + "Scanning NFO for AniDb Id");
        int beginIndex = nfo.indexOf("aid=");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + 4)), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
            movie.setId(ANIDB_PLUGIN_ID, st.nextToken());
            logger.debug(LOG_MESSAGE + "AniDb Id found in nfo = " + movie.getId(ANIDB_PLUGIN_ID));
        } else {
            logger.debug(LOG_MESSAGE + "No AniDb Id found in nfo!");
        }
    }
    
    private void loadAnidbTvdbMappings() throws SQLException {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
           
            AnidbHandler handler = new AnidbHandler();
                
            saxParser.parse(THETVDB_ANIDB_MAPPING_URL, handler);
            
            DeleteBuilder<AnidbTvdbMapping, String> db = mappingDao.deleteBuilder();
            PreparedDelete<AnidbTvdbMapping> pd = db.prepare();
            mappingDao.delete(pd);
            
            DeleteBuilder<AnidbTvdbEpisodeMapping, String> db2 = episodeMappingDao.deleteBuilder();
            PreparedDelete<AnidbTvdbEpisodeMapping> pd2 = db2.prepare();
            episodeMappingDao.delete(pd2);
            
            for (AnidbTvdbMapping m : handler.mappings) {
                mappingDao.create(m);
            }
            for (AnidbTvdbEpisodeMapping m : handler.episodeMappings) {
                episodeMappingDao.create(m);
            }
        } catch(SAXParseException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        } catch (ParserConfigurationException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        } catch (SAXException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        } catch (IOException e) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    
    /**
     * Open the connection to the website
     * @return a connection object, or null if there was a failure.
     */
    public static synchronized void anidbOpen() {
        if (anidbConn != null) {
            // No need to open again
            return;
        }
        
        UdpConnectionFactory factory;
        
        factory = UdpConnectionFactory.getInstance();
        try {
            anidbConn = factory.connect(anidbPort);
            anidbConn.authenticate(anidbUsername, anidbPassword, ANIDB_CLIENT_NAME, ANIDB_CLIENT_VERSION);
            anidbConnectionProtection = false;
        } catch (IllegalArgumentException error) {
            logger.error(LOG_MESSAGE + "Error logging in, please check you username & password");
            logger.error(LOG_MESSAGE + error.getMessage());
            anidbConn = null;
            anidbConnectionProtection = true;
        } catch (UdpConnectionException error) {
            logger.error(LOG_MESSAGE + "Error with UDP Connection, please try again later");
            logger.error(LOG_MESSAGE + error.getMessage());
            anidbConn = null;
            anidbConnectionProtection = true;
        } catch (AniDbException error) {
            logger.error(LOG_MESSAGE + "Error with AniDb: " + error.getMessage());
            anidbConn = null;
            anidbConnectionProtection = true;
        } catch (Exception error) {
            anidbConn = null;
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return;
    }

    /**
     * Close the connection to the website
     * @param conn
     */
    public static void anidbClose() {
        anidbLogout(anidbConn);
        // Now close the connection
        try {
            if (anidbConn != null) {
                anidbConn.close();
                logger.info(LOG_MESSAGE + "Logged out and leaving now.");
            }
        } catch (Exception error) {
            error.printStackTrace();    // XXX Debug
        }
    }

    /**
     * Try and log the user out
     * @param conn
     */
    private static void anidbLogout(UdpConnection conn) {
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
    
    /**
     * Hold information about the AniDb video file
     * @author stuart.boston
     *
     */
    @SuppressWarnings("unused")
    private class AniDbVideo {
        String  originalFilename;   // The unedited filename
        String  sceneGroup;         // The scene group (probably unused)
        String  title;              // The derived title of the video
        int     episodeNumber;      // An episode number (optional)
        String  episodeName;        // The derived episode name
        String  crc;                // CRC number (optional)
        String  otherTags;          // Any other information from the filename, e.g. resolution
        int     anidbID;            // The AniDb ID (from NFO)
    }
    
    // TODO: Here be dragons! Everything below should probably be refactored in time
    
    
    /**
     * Parses the xml document containing mappings from anidb id
     * to thetvdb id.
     * @author Xaanin
     *
     */
    private class AnidbHandler extends DefaultHandler{ 
        public List<AnidbTvdbMapping> mappings = new ArrayList<AnidbTvdbMapping>();
        public List<AnidbTvdbEpisodeMapping> episodeMappings = new ArrayList<AnidbTvdbEpisodeMapping>();
        AnidbTvdbMapping current;
        
        boolean name = false;
        boolean mapping = false;
        String lastMapping = "";
        StringBuilder nameString = new StringBuilder();
        
        int anidbMappingSeason;
        int tvdbMappingSeason;
        
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equalsIgnoreCase("anime")) {
                current = new AnidbTvdbMapping();
                String s = attributes.getValue(attributes.getIndex("tvdbid"));
                if (!s.equalsIgnoreCase("unknown")) {
                    current.setAnidbId(Long.parseLong(attributes.getValue(attributes.getIndex("anidbid"))));
                    current.setTvdbId(Long.parseLong(attributes.getValue(attributes.getIndex("tvdbid"))));
                    current.setDefaultTvdbSeason(Integer.parseInt(attributes.getValue(attributes.getIndex("defaulttvdbseason"))));
                    mappings.add(current);
                }
            } else if (qName.equalsIgnoreCase("name")) {
                name = true;
            } else if (qName.equalsIgnoreCase("mapping")) {
                mapping = true;
            }
        }
        
        public void endElement(String uri, String localName, String qName) {
            logger.info(LOG_MESSAGE + "End of element: " + qName);
            if (qName.equalsIgnoreCase("mapping")) {
                mapping = false;
                String[] split = lastMapping.split(";");
                for(String s : split) {
                    if (s.length() > 0) {
                        String[] res =  s.split("-");
                        logger.info(LOG_MESSAGE + lastMapping + " >> " + s);
                        String[] tvdbres = res[1].split("\\+");   // For certain series such as Bokusatsu Tenshi Dokuro-chan where one 
                                                                // anidb episode maps to two episodes at the tvdb.
                                                                // For now we only use the first one.
                        episodeMappings.add(new AnidbTvdbEpisodeMapping(anidbMappingSeason, Integer.parseInt(res[0]), 
                                                                        tvdbMappingSeason, Integer.parseInt(tvdbres[0]), current));
                    }
                }
                lastMapping = "";
            } else if (qName.equalsIgnoreCase("name")) {
                current.setName(nameString.toString());
                name = false;
            }
        }
        
        public void characters(char ch[], int start, int length) {
            if (name) {
                nameString.append(ch, start, length);
            } else if (mapping) {
                lastMapping += new String(ch, start, length); 
            }
        }
    }

    public boolean scan(Person person) {
        return false;
    }

    public AnidbLocalFile loadLocalFile(java.io.File file) {
        try {
            QueryBuilder<AnidbLocalFile, String> qb = localFileDao.queryBuilder();
            qb.where().eq(AnidbLocalFile.FILENAME_COLUMN_NAME, file.getAbsolutePath()).and().eq(AnidbLocalFile.SIZE_COLUMN_NAME, file.length());
            PreparedQuery<AnidbLocalFile> pq = qb.prepare();
            return localFileDao.query(pq).get(0);
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error when loading local file data");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return null;
    }
    
    public AnidbLocalFile loadLocalFile(File file, String ed2kHash) {
        try {
            AnidbLocalFile localFile = loadLocalFile(file);
            if (localFile != null) {
                return localFile;
            }
            QueryBuilder<AnidbLocalFile, String> qb = localFileDao.queryBuilder();
            qb.where().eq(AnidbLocalFile.ED2K_COLUMN_NAME, file.getAbsolutePath()).and().eq(AnidbLocalFile.SIZE_COLUMN_NAME, file.length());
            PreparedQuery<AnidbLocalFile> pq = qb.prepare();
            List<AnidbLocalFile> res = localFileDao.query(pq);
            if (res.size() > 0) {
                return res.get(0);
            }
            localFile = new AnidbLocalFile();
            localFile.setEd2k(ed2kHash);
            localFile.setLastSeen(new Date());
            localFile.setOriginalFilename(file.getAbsolutePath());
            localFile.setSize(file.length());
            localFileDao.create(localFile);
            return localFile;
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error when loading local file data");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return null;
    }

    public AnidbFile loadAnidbFile(String ed2kHash, long size) throws SQLException, UdpConnectionException, AniDbException {
        AnidbFile af = null;
        QueryBuilder<AnidbFile, String> qb = anidbFileDao.queryBuilder();
        qb.where().eq(AnidbFile.ED2K_COLUMN_NAME, ed2kHash).and().eq(AnidbFile.SIZE_COLUMN_NAME, size);
        PreparedQuery<AnidbFile> pq = qb.prepare();
        af = anidbFileDao.queryForFirst(pq);
        if (af != null) {
            return af;
        }
        List<net.anidb.File> ret = anidbConn.getFiles(size, ed2kHash, fileMask, animeFileMask);
        if (ret.size() > 1) {
            logger.error(LOG_MESSAGE + " Got multiple results for file with ed2k hash " + ed2kHash);
        }
        if (size > 0) {
            af = new AnidbFile(ret.get(0));
            anidbFileDao.create(af);
        }
        return af;
    }
    
    public AnidbEpisode loadAnidbEpisode(long eid) {
        try {
            AnidbEpisode episode = episodeDao.queryForId(Long.toString(eid));
            if (episode != null) {
                return episode;
            }
            Episode ep = anidbConn.getEpisode(eid);
            AnidbEpisode ae = new AnidbEpisode(ep);
            episodeDao.create(ae);
            return ae;
        } catch (SQLException e) {
            logException("Encountered an SQL error when loading episode data", e);
        } catch (UdpConnectionException e) {
            logException("Encountered UDP Connection error when loading episode information for eid " + eid, e);
        } catch (AniDbException e) {
            logException("Encountered an Anidb error when loading episode information for eid " + eid, e);
        }
        return null;
    }
    
    public AnidbAnime loadAnidbAnime(long aid) throws SQLException, UdpConnectionException, AniDbException {
        AnidbAnime res = getAnimeFromCache(aid);
        if (res != null) {
            return res;
        }
        res = animeDao.queryForId(Long.toString(aid));
        if (res != null) {
            addAnimeToCache(res);
            return res;
        }
        Anime _anime = anidbConn.getAnime(aid, anidbMask);
        AnidbAnime anime = null;
        if (_anime != null) {
            anime = new AnidbAnime(_anime);
            String animePlot = anidbConn.getAnimeDescription(aid);
            anime.setDescription(animePlot);
            animeDao.create(anime);    
            
            /* Add categories and reload anime from database */
            createCategories(anime, _anime);
            anime = animeDao.queryForId(Long.toString(anime.getAnimeId()));
            addAnimeToCache(anime);
        }
        return anime;
    }
    
    public AnidbAnime loadAnidbAnime(String name) throws SQLException, UdpConnectionException, AniDbException {
        AnidbAnime anime = getAnimeFromCache(name);
        if (anime != null) {
            return anime;
        }
        QueryBuilder<AnidbAnime, String> qb = animeDao.queryBuilder();
        qb.where().eq(AnidbAnime.ROMAJI_NAME_COLUMN, name).or().eq(AnidbAnime.ENGLISH_NAME_COLUMN, name);
        PreparedQuery<AnidbAnime> pq = qb.prepare();
        List<AnidbAnime> res = animeDao.query(pq); 
        if (res.size() > 0) {
            anime = res.get(0);
            addAnimeToCache(anime);
            return anime;
        }
        Anime _anime = anidbConn.getAnime(name, anidbMask);
        if (_anime != null) {
            anime = new AnidbAnime(_anime);
            String animePlot = anidbConn.getAnimeDescription(anime.getAnimeId());
            anime.setDescription(animePlot);
            animeDao.create(anime);
            createCategories(anime, _anime);
            anime = animeDao.queryForId(Long.toString(anime.getAnimeId()));
            addAnimeToCache(anime);
        }
        return anime;
    }    
    
    private void createCategories(AnidbAnime anime, Anime anidbResult) throws SQLException, UdpConnectionException, AniDbException {
        /* Add categories and reload anime from database */
        /* Response was most likely truncated if we got less than five categories */
        if (anidbResult.getCategoryList() == null || anidbResult.getCategoryWeightList() == null || anidbResult.getCategoryList().size() < 5) {
            anidbResult = anidbConn.getAnime(anime.getAnimeId(), categoryMask);
        }
        for(int i = 0; i < anidbResult.getCategoryList().size(); ++i) {
            AnidbCategory category = new AnidbCategory();
            category.setAnime(anime);
            category.setCategoryName(anidbResult.getCategoryList().get(i));
            category.setWeight(Integer.parseInt(anidbResult.getCategoryWeightList().get(i)));
            categoryDao.create(category);
        }
    }
    
    private AnidbAnime getAnimeFromCache(String name) {
        AnidbAnime anime = null;
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameEnglish", name))) != null) {
            return anime;
        }        
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameRomaji", name))) != null) {
            return anime;
        }        
        return null;
    }
    
    
    private AnidbAnime getAnimeFromCache(long aid) {
        AnidbAnime anime = null;
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeId", Long.toString(aid)))) != null) {
            return anime;
        }
        return null;
    }
    
    private static void addAnimeToCache(AnidbAnime anime) {
        Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeId", Long.toString(anime.getAnimeId())), anime);
        if (StringTools.isValidString(anime.getEnglishName())) {
            Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameEnglish", anime.getEnglishName()), anime);
        }
        if (StringTools.isValidString(anime.getRomajiName())) {
            Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameRomaji", anime.getRomajiName()), anime);
        }
    }
    
    private void logException(String message, Exception e) {
        logger.error(LOG_MESSAGE + message);
        final Writer eResult = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(eResult);
        e.printStackTrace(printWriter);
        logger.error(eResult.toString());        
    }
}

/**
 * Holds information about a scanned file on the local system
 */
@DatabaseTable(tableName = "anidb_local_file")
class AnidbLocalFile {
    public static final String ID_COLUMN_NAME = "id";
    public static final String FILENAME_COLUMN_NAME = "filename";
    public static final String ED2K_COLUMN_NAME = "ed2khash";
    public static final String SIZE_COLUMN_NAME = "size";
    public static final String LAST_SEEN_COLUMN_NAME = "lastseen";
    

    public AnidbLocalFile() {}
    @DatabaseField(generatedId = true, columnName = ID_COLUMN_NAME)
    private int id;
    // We need to store filenames as bytes in order to allow special characters such as '
    @DatabaseField(columnName = FILENAME_COLUMN_NAME, dataType = DataType.STRING_BYTES) 
    private String originalFilename;
    @DatabaseField(index = true, columnName = ED2K_COLUMN_NAME)
    private String ed2k;
    @DatabaseField(index = true, columnName = SIZE_COLUMN_NAME)
    private long size;
    @DatabaseField(index = true, columnName = LAST_SEEN_COLUMN_NAME)
    private Date lastSeen; // Date this file was last seen, to allow periodic cleanup of stale records

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getOriginalFilename() {
        return originalFilename;
    }
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    public String getEd2k() {
        return ed2k;
    }
    public void setEd2k(String ed2k) {
        this.ed2k = ed2k;
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    public Date getLastSeen() {
        return lastSeen;
    }
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
}

/**
 * Hold information about a file from anidb
 * @author Xaanin
 */
@DatabaseTable(tableName = "anidb_file")
class AnidbFile {
    public static final String ED2K_COLUMN_NAME = "ed2k";
    public static final String SIZE_COLUMN_NAME = "size";

    @DatabaseField(id = true)
    private long fileId;
    @DatabaseField()
    private long animeId;
    @DatabaseField()
    private long episodeId;
    @DatabaseField()
    private long groupId;
    @DatabaseField()
    private String MD5;
    @DatabaseField()
    private String ed2k;
    @DatabaseField()
    private String SHA1;
    @DatabaseField()
    private String CRC32;
    @DatabaseField()
    private long size; // This might seem redundant, but it's needed so we can query for AnidbFiles using hash+size without consulting anidb
    @DatabaseField(index = true)
    private Date retrieved; // We should be able to periodically recheck the anidb information
    
    public AnidbFile() {}
    
    public AnidbFile(net.anidb.File file) {
        setAnimeId(file.getEpisode().getAnime().getAnimeId());
        setCRC32(file.getCrc32());
        setEd2k(file.getEd2k());
        setEpisodeId(file.getEpisode().getEpisodeId());
        setFileId(file.getFileId());
        setGroupId(file.getGroup().getGroupId());
        setMD5(file.getMd5());
        setRetrieved(new Date());
        setSHA1(file.getSha1());
        setSize(file.getSize());
    }

    public long getFileId() {
        return fileId;
    }
    public void setFileId(long fileId) {
        this.fileId = fileId;
    }
    public long getAnimeId() {
        return animeId;
    }
    public void setAnimeId(long animeId) {
        this.animeId = animeId;
    }
    public long getEpisodeId() {
        return episodeId;
    }
    public void setEpisodeId(long episodeId) {
        this.episodeId = episodeId;
    }
    public long getGroupId() {
        return groupId;
    }
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
    public String getMD5() {
        return MD5;
    }
    public void setMD5(String mD5) {
        MD5 = mD5;
    }
    public String getEd2k() {
        return ed2k;
    }
    public void setEd2k(String ed2k) {
        this.ed2k = ed2k;
    }
    public String getSHA1() {
        return SHA1;
    }
    public void setSHA1(String sHA1) {
        SHA1 = sHA1;
    }
    public String getCRC32() {
        return CRC32;
    }
    public void setCRC32(String cRC32) {
        CRC32 = cRC32;
    }
    public Date getRetrieved() {
        return retrieved;
    }
    public void setRetrieved(Date retrieved) {
        this.retrieved = retrieved;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
/**
 * Hold information about an episode from anidb
 * @author Xaanin
 */
@DatabaseTable(tableName = "anidb_episode")
class AnidbEpisode {
    public static final String ANIME_ID_COLUMN = "animeid";
    public static final String EPISODE_ID_COLUMN = "episodeid";
    public static final String EPISODE_NUMBER_COLUMN = "episodenumber";

    @DatabaseField(id = true, columnName = EPISODE_ID_COLUMN)
    private long episodeId;
    @DatabaseField(index = true, columnName = ANIME_ID_COLUMN)
    private long animeId;
    @DatabaseField()
    private long length;
    @DatabaseField()
    private long rating;
    @DatabaseField()
    private long votes;
    @DatabaseField(index = true, columnName = EPISODE_NUMBER_COLUMN)
    private String episodeNumber; // This can contain letters for things like trailers, specials and such.
    @DatabaseField()
    private String englishName;
    @DatabaseField()
    private String romajiName;
    @DatabaseField()
    private String kanjiName;
    @DatabaseField()
    private Date aired;
    @DatabaseField(index = true)
    private Date retrieved; // We should be able to periodically recheck the anidb information
    
    public AnidbEpisode() {}

    public AnidbEpisode(Episode ep) {
        setAnimeId(ep.getAnime().getAnimeId());
        setAired(new Date(ep.getAired()));
        setEpisodeId(ep.getEpisodeId());
        setEnglishName(ep.getEnglishTitle());
        setEpisodeNumber(ep.getEpisodeNumber());
        setKanjiName(ep.getKanjiTitle());
        setLength(ep.getLength());
        setRating(ep.getRating());
        setRetrieved(new Date());
        setRomajiName(ep.getRomajiTitle());
        setVotes(ep.getVotes());
    }
    public long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(long episodeId) {
        this.episodeId = episodeId;
    }

    public long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(long animeId) {
        this.animeId = animeId;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getRating() {
        return rating;
    }

    public void setRating(long rating) {
        this.rating = rating;
    }

    public long getVotes() {
        return votes;
    }

    public void setVotes(long votes) {
        this.votes = votes;
    }

    public String getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(String episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getRomajiName() {
        return romajiName;
    }

    public void setRomajiName(String romajiName) {
        this.romajiName = romajiName;
    }

    public String getKanjiName() {
        return kanjiName;
    }

    public void setKanjiName(String kanjiName) {
        this.kanjiName = kanjiName;
    }

    public Date getAired() {
        return aired;
    }

    public void setAired(Date aired) {
        this.aired = aired;
    }

    public Date getRetrieved() {
        return retrieved;
    }

    public void setRetrieved(Date retrieved) {
        this.retrieved = retrieved;
    }

 
    
}

/**
 * @author Xaanin
 */
@DatabaseTable(tableName = "anidb_anime")
class AnidbAnime {
    public static final String ROMAJI_NAME_COLUMN = "romaji_title";
    public static final String ENGLISH_NAME_COLUMN = "english_title";

    @DatabaseField(id = true)
    private long animeId;
    @DatabaseField()
    private long airDate;
    @DatabaseField()
    private String year;
    @DatabaseField()
    private String type;

    @DatabaseField(index = true, columnName = ROMAJI_NAME_COLUMN)
    private String romajiName;

    @DatabaseField(index = true, columnName = ENGLISH_NAME_COLUMN)
    private String englishName;

    @DatabaseField()
    private String kanjiName;

    @DatabaseField()
    private String description;

    @DatabaseField(index = true)
    private Date retrieved; // We should be able to periodically recheck the anidb information
    
    @DatabaseField()
    private long endDate;
    
    @DatabaseField()
    private long rating;
    
    @DatabaseField()
    private long episodeCount;
    
    @ForeignCollectionField(eager = true)
    private ForeignCollection<AnidbCategory> categories;
    
    private List<AnidbCategory> _categories;
    
    public AnidbAnime() {
        endDate = 0;
        rating = 0;
        episodeCount = 0;
    }

    public AnidbAnime(Anime anime) {
        setAnimeId(anime.getAnimeId());
        setEnglishName(anime.getEnglishName());
        setKanjiName(anime.getKanjiName());
        setRetrieved(new Date());
        setRomajiName(anime.getRomajiName());
        setType(anime.getType());
        setYear(anime.getYear());
        if (anime.getAirDate() != null) {
            setAirDate(anime.getAirDate());
        }
        if (anime.getEndDate() != null) {
            setEndDate(anime.getEndDate());
        }
        if (anime.getRating() != null) {
            setRating(anime.getRating());
        }
    }
    
    public void setAnimeId(long aid) {
        this.animeId = aid;
    }

    public long getAnimeId() {
        return animeId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRomajiName() {
        return romajiName;
    }

    public void setRomajiName(String romajiName) {
        this.romajiName = romajiName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getKanjiName() {
        return kanjiName;
    }

    public void setKanjiName(String kanjiName) {
        this.kanjiName = kanjiName;
    }

    public String getDescription() {
        return description.replaceAll("\\[[\\w:/=\\.]*\\]", ""); // Remove what appears to be bbcode tags in description
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getRetrieved() {
        return retrieved;
    }

    public void setRetrieved(Date retrieved) {
        this.retrieved = retrieved;
    }

    public void setAirDate(long airDate) {
        this.airDate = airDate;
    }

    public long getAirDate() {
        return airDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setRating(Long rating) {
        this.rating = rating;
    }

    public long getRating() {
        return rating;
    }

    public void setEpisodeCount(long episodeCount) {
        this.episodeCount = episodeCount;
    }

    public long getEpisodeCount() {
        return episodeCount;
    }
    
    // Comparator to sort AnidbCategory in descending order based on weight.
    private class CategoryComparator implements Comparator<AnidbCategory> {
        @Override
        public int compare(AnidbCategory o1, AnidbCategory o2) {
            if(o1.getWeight() < o2.getWeight()) {
                return 1;
            } else if (o1.getWeight() > o2.getWeight()) {
                return -1;
            } else {
                return o1.getCategoryName().compareTo(o2.getCategoryName());
            }
        }
    }

    public List<AnidbCategory> getCategories() {
        if (_categories == null) {
            _categories = new ArrayList<AnidbCategory>(categories);
            Collections.sort(_categories, new CategoryComparator());
        }
        return _categories;
    }

    public void setCategories(ForeignCollection<AnidbCategory> categories) {
        this.categories = categories;
    }
}

/**
 * Category class
 * @author Xaanin
 */
@DatabaseTable(tableName = "anidb_category")
class AnidbCategory {
    public static final String CATEGORY_NAME_COLUMN = "name";
    @DatabaseField(generatedId = true)
    private int id;
    
    @DatabaseField(columnName = CATEGORY_NAME_COLUMN, canBeNull = false)
    private String categoryName;

    @DatabaseField(foreign = true, canBeNull=false) // Doing it like this will use more db space but requires less coding.
    private AnidbAnime anime;
    
    @DatabaseField()
    private long weight;
    
    public AnidbCategory(int id, String categoryName) {
        super();
        this.id = id;
        this.categoryName = categoryName;
        anime = null;
        weight = -1;
    }
    
    public AnidbCategory() {
        categoryName = "";
        anime = null;
        weight = -1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public AnidbAnime getAnime() {
        return anime;
    }

    public void setAnime(AnidbAnime anime) {
        this.anime = anime;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }
}

@DatabaseTable(tableName = "anidb_tableinfo")
class AnidbTableInfo {
    @DatabaseField(id = true)
    private int version;
    @DatabaseField()
    private Date lastTvdbMappingDownload;
    @DatabaseField()
    private Date lastAnidbDataDumpDownload;
    
    public AnidbTableInfo() {
        
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Date getLastTvdbMappingDownload() {
        return lastTvdbMappingDownload;
    }

    public void setLastTvdbMappingDownload(Date lastTvdbMappingDownload) {
        this.lastTvdbMappingDownload = lastTvdbMappingDownload;
    }

    public Date getLastAnidbDataDumpDownload() {
        return lastAnidbDataDumpDownload;
    }

    public void setLastAnidbDataDumpDownload(Date lastAnidbDataDumpDownload) {
        this.lastAnidbDataDumpDownload = lastAnidbDataDumpDownload;
    }
}

@DatabaseTable(tableName = "anidb_tvdb_mapping")
class AnidbTvdbMapping {
    public static final String ID_COLUMN_NAME = "id";
    public static final String ANIDB_ID_COLUMN_NAME = "anidb_id";
    public static final String TVDB_ID_COLUMN_NAME = "tvdb_id";
    public static final String TVDB_DEFAULT_SEASON_COLUMN_NAME = "tvdb_default_season";
    public static final String NAME_COLUMN_NAME = "name";
    @DatabaseField(id=true, columnName = ID_COLUMN_NAME)
    private int id;
    
    @DatabaseField(columnName = ANIDB_ID_COLUMN_NAME)
    private long anidbId;
    @DatabaseField(columnName = TVDB_ID_COLUMN_NAME)
    private long tvdbId;

    @DatabaseField(columnName = TVDB_DEFAULT_SEASON_COLUMN_NAME)
    private int defaultTvdbSeason;
    
    @DatabaseField(columnName = NAME_COLUMN_NAME)
    private String name;
    
    @ForeignCollectionField(eager=true)
    private ForeignCollection<AnidbTvdbEpisodeMapping> mappings;
    
    public AnidbTvdbMapping() {}

    public AnidbTvdbMapping(long anidbId, long tvdbId) {
        super();
        this.anidbId = anidbId;
        this.tvdbId = tvdbId;
    }

    public long getAnidbId() {
        return anidbId;
    }

    public void setAnidbId(long anidbId) {
        this.anidbId = anidbId;
    }

    public long getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(long tvdbId) {
        this.tvdbId = tvdbId;
    }

    public int getDefaultTvdbSeason() {
        return defaultTvdbSeason;
    }

    public void setDefaultTvdbSeason(int defaultTvdbSeason) {
        this.defaultTvdbSeason = defaultTvdbSeason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ForeignCollection<AnidbTvdbEpisodeMapping> getMappings() {
        return mappings;
    }
}

@DatabaseTable(tableName = "anidb_tvdb_episode_mapping")
class AnidbTvdbEpisodeMapping {
    public static final String ID_COLUMN_NAME = "id";
    public static final String ANIDB_SEASON_COLUMN_NAME = "anidb_season";
    public static final String ANIDB_EPISODE_NUMBER_COLUMN_NAME = "anidb_episode_number";
    public static final String TVDB_SEASON_COLUMN_NAME = "tvdb_season";
    public static final String TVDB_EPISODE_NUMBER_COLUMN_NAME = "tvdb_episode_number";
    @DatabaseField(id = true, columnName=ID_COLUMN_NAME)
    private int id;
    @DatabaseField(columnName = ANIDB_SEASON_COLUMN_NAME)
    private int anidbSeason;
    @DatabaseField(columnName = ANIDB_EPISODE_NUMBER_COLUMN_NAME)
    private int anidbEpisodeNumber;
    
    @DatabaseField(columnName = TVDB_SEASON_COLUMN_NAME)
    private int tvdbSeason;
    @DatabaseField(columnName = TVDB_EPISODE_NUMBER_COLUMN_NAME)
    private int tvdbEpisodeNumber;
    
    @DatabaseField(foreign = true)
    private AnidbTvdbMapping mapping;
    
    public AnidbTvdbEpisodeMapping() {}

    public AnidbTvdbEpisodeMapping(int anidbSeason, int anidbEpisodeNumber, int tvdbSeason, int tvdbEpisodeNumber, AnidbTvdbMapping mapping) {
        super();
        this.anidbSeason = anidbSeason;
        this.anidbEpisodeNumber = anidbEpisodeNumber;
        this.tvdbSeason = tvdbSeason;
        this.tvdbEpisodeNumber = tvdbEpisodeNumber;
        this.mapping = mapping;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnidbSeason() {
        return anidbSeason;
    }

    public void setAnidbSeason(int anidbSeason) {
        this.anidbSeason = anidbSeason;
    }

    public int getAnidbEpisodeNumber() {
        return anidbEpisodeNumber;
    }

    public void setAnidbEpisodeNumber(int anidbEpisodeNumber) {
        this.anidbEpisodeNumber = anidbEpisodeNumber;
    }

    public int getTvdbSeason() {
        return tvdbSeason;
    }

    public void setTvdbSeason(int tvdbSeason) {
        this.tvdbSeason = tvdbSeason;
    }

    public int getTvdbEpisodeNumber() {
        return tvdbEpisodeNumber;
    }

    public void setTvdbEpisodeNumber(int tvdbEpisodeNumber) {
        this.tvdbEpisodeNumber = tvdbEpisodeNumber;
    }

    public AnidbTvdbMapping getMapping() {
        return mapping;
    }

    public void setMapping(AnidbTvdbMapping mapping) {
        this.mapping = mapping;
    }
    
    
}
