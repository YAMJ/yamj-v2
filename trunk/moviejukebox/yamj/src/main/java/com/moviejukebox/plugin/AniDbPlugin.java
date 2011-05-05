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
    private AnimeFileMask animeFileMask;
    private FileMask fileMask;
    // 5 groups: 1=Scene Group, 2=Anime Name, 3=Episode, 4=Episode Title, 5=Remainder
    private static final String REGEX_TVSHOW = "(?i)(\\[.*?\\])?+(\\w.*?)(?:[\\. _-]|ep)(\\d{1,3})(\\w+)(.+)";
    // 4 groups: 1=Scene Group, 2=Anime Name, 3=CRC, 4=Remainder
    private static final String REGEX_MOVIE = "(\\[.*?\\])?+([\\w-]+)(\\[\\w{8}\\])?+(.*)";
    private static final String CRC_REGEX = "(.*)(\\[\\w{8}\\])(.*)";
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
    private Map<Long, AnimeIdMapping> tvdbMappings;
    
    
    private static final int TABLE_VERSION = 1;
    private Dao<AnidbLocalFile, String> localFileDao;
    private Dao<AnidbFile, String> anidbFileDao;
    private Dao<AnidbAnime, String> animeDao;
    private Dao<AnidbEpisode, String> episodeDao;
    private Dao<AnidbCategory, String> categoryDao;
    
    private TheTvDBPlugin tvdb;
    
    public AniDbPlugin() {
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        
        try {
            anidbPort = PropertiesUtil.getIntProperty("anidb.port", "1025");
        } catch (Exception ignore) {
            anidbPort = 1025;
            logger.error(LOG_MESSAGE + "Error setting the port to '" + PropertiesUtil.getProperty("anidb.port") + "' using default");
        }
        
        anidbMask = new AnimeMask(true, true, true, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, true, true, true, true, false, false, false, false, false, true, true, false, false, false, false, false, true, false, true, true, false, false, false, true, false);
        
        animeFileMask = new AnimeFileMask(true, false, true, true, false, false, true, true, true, true, false, false, false, true, true, true, true, true, true, false, false, false);

        fileMask = new FileMask(true, true, true, false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false);
        anidbUsername = PropertiesUtil.getProperty("anidb.username", null);
        anidbPassword = PropertiesUtil.getProperty("anidb.password", null);
        //String str = PropertiesUtil.getProperty("anidb.useHashIdentification", null);
        hash = PropertiesUtil.getBooleanProperty("anidb.useHashIdentification", "false");
        
        if (anidbUsername == null || anidbPassword == null) {
            logger.error(LOG_MESSAGE + "You need to add your AniDb Username & password to the anidb.username & anidb.password properties");
            anidbConnectionProtection = true;
        }
        setupDatabase();
        tvdbMappings = new HashMap<Long, AniDbPlugin.AnimeIdMapping>();
        if (getAdditionalInformationFromTheTvDB) {
            loadAniDbTvDbMappings();
            tvdb = new TheTvDBPlugin();
        }
    }
    
    private void setupDatabase()
    {
        /*
         * Lets just pray that the current directory is writable
         * TODO: Implement an override for this in the properties file
         */
        String dbUrl = "jdbc:sqlite:yamj_anidb.db";
        ConnectionSource connectionSource;
        try {
            connectionSource = new JdbcConnectionSource(dbUrl);
            updateTables(connectionSource);
            localFileDao = DaoManager.createDao(connectionSource, AnidbLocalFile.class);
            animeDao = DaoManager.createDao(connectionSource, AnidbAnime.class);
            episodeDao = DaoManager.createDao(connectionSource, AnidbEpisode.class);
            anidbFileDao = DaoManager.createDao(connectionSource, AnidbFile.class);
            categoryDao = DaoManager.createDao(connectionSource, AnidbCategory.class);
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
            Dao<AnidbTableInfo, String> tableDao = DaoManager.createDao(connectionSource, AnidbTableInfo.class);
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
        if (hash) {
            if (!anidbHashScan(movie)) {
                return false;
            }
        } else {
            Matcher titleMatch = Pattern.compile(REGEX_TVSHOW).matcher(movie.getBaseFilename());
            String episode = Movie.UNKNOWN;
            String remainder = Movie.UNKNOWN;
            String crc = Movie.UNKNOWN;

            if (titleMatch.find()) {
                // If this matches then this is a TV Show
                logger.info(LOG_MESSAGE + "Matched as a TV Show");
                movie.setMovieType(Movie.TYPE_TVSHOW);
                movie.setSeason(1);
                movie.setTitle(cleanString(titleMatch.group(2)));
                movie.setOriginalTitle(movie.getTitle());
                episode = titleMatch.group(3);
                remainder = titleMatch.group(5);

                if (isValidString(remainder)) {
                    Matcher crcMatch = Pattern.compile(CRC_REGEX).matcher(remainder);
                    if (crcMatch.find()) {
                        crc = crcMatch.group(2);
                        remainder = remainder.replace(crc, "");
                    }
                }
            } else {
                logger.info(LOG_MESSAGE + "Assuming a movie");
                titleMatch = Pattern.compile(REGEX_MOVIE).matcher(movie.getBaseFilename());
                if (titleMatch.find()) {
                    // 4 groups: 1=Scene Group, 2=Anime Name, 3=CRC, 4=Remainder
                    movie.setTitle(cleanString(titleMatch.group(2)));
                    movie.setOriginalTitle(movie.getTitle());
                    crc = titleMatch.group(3);
                    remainder = titleMatch.group(4);
                }
                movie.setMovieType(Movie.TYPE_MOVIE);
            }

            logger.info("Title  : " + movie.getTitle()); // XXX: DEBUG
            logger.info("Episode: " + (isValidString(episode) ? episode : Movie.UNKNOWN)); // XXX: DEBUG
            logger.info("CRC    : " + (isValidString(crc) ? crc : Movie.UNKNOWN)); // XXX: DEBUG
            logger.info("Remain : " + (isValidString(remainder) ? remainder : Movie.UNKNOWN)); // XXX: DEBUG
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
            logger.info(LOG_MESSAGE + "Unknown AniDb Exception erorr");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
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

            if (tvdbMappings.get(anime.getAnimeId()) != null) {
                movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, Long.toString(tvdbMappings.get(anime.getAnimeId()).tvDbId));
                movie.setSeason(1);
            }
            if (getAdditionalInformationFromTheTvDB) {
                movie.setTitle(anime.getRomajiName());
                tvdb.scan(movie);
            }

            /*
             * if (isValidString(anime.getPicname())) { movie.setPosterURL(PICTURE_URL_BASE + anime.getPicname()); }
             */

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

            /*
             * if (!anime.getCategoryList().isEmpty()) { movie.setGenres(anime.getCategoryList()); }
             */

            if (anime.getAirDate() > 0) {
                DateTime rDate = new DateTime(anime.getAirDate());
                movie.setReleaseDate(rDate.toString("yyyy-MM-dd"));
            }

            if (anime.getRating() > 0) {
                movie.setRating((int)(anime.getRating() / 10));
            }

            //String plot = HTMLTools.stripTags(getAnimeDescription(animeId));
            // This plot may contain the information on the director and this needs to be stripped from the plot
            //logger.info("Plot: " + plot); // XXX: DEBUG
            logger.info("Plot: " + anime.getDescription());
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
    private boolean anidbHashScan(Movie movie) {
        try {
            AnidbLocalFile localFile = null;
            QueryBuilder<AnidbLocalFile, String> qb = localFileDao.queryBuilder();
            qb.where().eq(AnidbLocalFile.SIZE_COLUMN_NAME, movie.getFile().length()).and()
                            .eq(AnidbLocalFile.FILENAME_COLUMN_NAME, movie.getFile().getAbsolutePath());
            PreparedQuery<AnidbLocalFile> pq = qb.prepare();
            List<AnidbLocalFile> res = localFileDao.query(pq);
            if (res.size() > 0) {
                localFile = res.get(0);
            }

            String ed2kHash = null;
            AnidbFile file = null;
            if (localFile == null) {
                ed2kHash = getEd2kChecksum(movie.getFile());
                if (ed2kHash.equals("")) {
                    return false;
                }
                localFile = PojoConverter.create(movie.getFile(), ed2kHash, localFileDao, logger);
            }

            file = getAnimeEpisodeByHash(movie.getFile().length(), localFile == null ? ed2kHash : localFile.getEd2k());

            if (file != null) {
                movie.setId(ANIDB_PLUGIN_ID, Long.toString(file.getAnimeId()));
                AnidbEpisode ep;
                ep = getEpisodeByEid(file.getEpisodeId());
                
                if (ep != null) { // TODO: Determine if this works as intended
                    for (MovieFile mf : movie.getMovieFiles()) {
                        if (mf.getFirstPart() != Integer.parseInt(ep.getEpisodeNumber()) && movie.getMovieFiles().size() == 1) {
                            mf.setNewFile(true);
                            mf.setTitle(ep.getEnglishName());
                            mf.setFirstPart(Integer.parseInt(ep.getEpisodeNumber()));
                            mf.setLastPart(Integer.parseInt(ep.getEpisodeNumber()));
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (UdpConnectionException error) {
            logger.info(LOG_MESSAGE + "UDP Connection Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
        } catch (AniDbException error) {
            logger.info(LOG_MESSAGE + "AniDb Exception Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
        } catch (SQLException error) {
            logger.info(LOG_MESSAGE + "AniDb Exception Error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
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

    private String getAnimeDescription(long animeId) {
        String animePlot = null;
        try {
            animePlot = anidbConn.getAnimeDescription(animeId);
        } catch (UdpConnectionException error) {
            processUdpError(error);
        } catch (AniDbException error) {
            processAnidbError(error);
        }
        return animePlot;
    }
    
    public AnidbAnime getAnimeByAid(long animeId) throws UdpConnectionException, AniDbException, SQLException {
        AnidbAnime anime = AnimeFactory.load(animeId, animeDao, logger);
        if (anime == null) {
            anime = AnimeFactory.create(anidbConn.getAnime(animeId, anidbMask), getAnimeDescription(animeId), animeDao, categoryDao, logger);
        }
        return anime;
    }
    
    public AnidbAnime getAnimeByName(String animeName) throws UdpConnectionException, AniDbException, SQLException {
        AnidbAnime anime = AnimeFactory.load(animeName, animeDao, logger);
        if (anime != null) {
            return anime;
        }
        Anime _anime = anidbConn.getAnime(animeName, anidbMask);
        anime = AnimeFactory.create(_anime, getAnimeDescription(_anime.getAnimeId()), animeDao, categoryDao, logger);
        return anime;
    }
    
    public AnidbFile getAnimeEpisodeByHash(long size, String hash) throws UdpConnectionException, AniDbException, SQLException {
        QueryBuilder<AnidbFile, String> qb = anidbFileDao.queryBuilder();
        qb.where().eq(AnidbFile.ED2K_COLUMN_NAME, hash).and().eq(AnidbFile.SIZE_COLUMN_NAME, size);
        PreparedQuery<AnidbFile> pq = qb.prepare();
        List<AnidbFile> res = anidbFileDao.query(pq);
        if (res.size() > 0) {
            return res.get(0);
        }
        List<net.anidb.File> results = anidbConn.getFiles(size, hash, fileMask, animeFileMask);
        return PojoConverter.create(results.get(0), anidbFileDao, logger); // Unsure how we'd get more than one result here.
    }
    
    public AnidbEpisode getEpisodeByEid(long eid) throws UdpConnectionException, AniDbException, SQLException{
        // Don't cache episode in memory, we're unlikely to need them more than once per run
        AnidbEpisode episode = null;
        episode = AnidbEpisodeFactory.load(eid, episodeDao, logger);
        if (episode != null) {
            return episode;
        }
        Episode ep = anidbConn.getEpisode(eid);
        if (ep != null) {
            episode = AnidbEpisodeFactory.create(ep, episodeDao, logger);
            return episode;
        }
        return null;
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
    
    private void loadAniDbTvDbMappings() {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
           
            AnidbHandler handler = new AnidbHandler();
                
            saxParser.parse(THETVDB_ANIDB_MAPPING_URL, handler);
            for (AnimeIdMapping m : handler.mappings) {
                // We work on the assumption that multiple anidb ids can map to
                // the same tvdb id, but not the other way around.
                if (tvdbMappings.containsKey(m.aniDbId)) {
                    logger.error(LOG_MESSAGE + "Duplicate anidb ids found while setting up tvdb mappings: " + m.aniDbId + " mapping to " + tvdbMappings.get(m.aniDbId).tvDbId + " and " + m.tvDbId);
                }
                tvdbMappings.put(m.aniDbId, m);
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
     * Hold information about mapping of AnimeId of AniDB and
     * TheTVDB ids for a given show.
     * @author Xaanin
     * 
     */
    
    @SuppressWarnings("unused")
    private class AnimeIdMapping {
        long aniDbId;
        long tvDbId;
        int defaultSeason;
        String name;
        HashMap<String, String> mappings; // Map Anidb season/episode to tvdb season/episode 
        
        public AnimeIdMapping() {
            mappings = new HashMap<String, String>();
        }
        
        public void addMapping(int anidbSeason, int anidbEpisode, int tvdbSeason, int tvdbEpisode) {
            mappings.put("" + anidbSeason + "|" + anidbEpisode, "" + tvdbSeason + "|" + tvdbEpisode);
        }
    }
    /**
     * Parses the xml document containing mappings from anidb id
     * to thetvdb id.
     * @author Xaanin
     *
     */
    private class AnidbHandler extends DefaultHandler{ 
        public List<AnimeIdMapping> mappings = new ArrayList<AnimeIdMapping>();
        AnimeIdMapping current;
        
        boolean name = false;
        boolean mapping = false;
        String lastMapping = "";
        int anidbMappingSeason;
        int tvdbMappingSeason;
        
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equalsIgnoreCase("anime")) {
                current = new AnimeIdMapping();
                String s = attributes.getValue(attributes.getIndex("tvdbid"));
                if (!s.equalsIgnoreCase("unknown")) {
                    current.aniDbId = Long.parseLong(attributes.getValue(attributes.getIndex("anidbid")));
                    mappings.add(current);
                    current.tvDbId = Long.parseLong(attributes.getValue(attributes.getIndex("tvdbid")));
                    current.defaultSeason = Integer.parseInt(attributes.getValue(attributes.getIndex("defaulttvdbseason")));                    
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
                        current.addMapping(anidbMappingSeason, Integer.parseInt(res[0]), tvdbMappingSeason, Integer.parseInt(tvdbres[0]));
                    }
                }
                lastMapping = "";
            } else if (qName.equalsIgnoreCase("name")) {
                name = false;
            }
        }
        
        public void characters(char ch[], int start, int length) {
            if (name) {
                current.name += new String(ch, start, length);
            } else if (mapping) {
                lastMapping += new String(ch, start, length); 
            }
        }
    }
    
    /*
     * Create our database objects from anidb api objects
     */
    private static class PojoConverter {
        private PojoConverter() {}
        
        public static AnidbLocalFile create(java.io.File file, String ed2kHash, Dao<AnidbLocalFile, String> dao, Logger logger) {
            AnidbLocalFile res = new AnidbLocalFile();
            res.setEd2k(ed2kHash);
            res.setLastSeen(new Date());
            res.setOriginalFilename(file.getAbsolutePath());
            res.setSize(file.length());
            try {
                dao.create(res);
            } catch (SQLException e) {
                logger.error(LOG_MESSAGE + "Encountered an SQL error");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                e.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
            return res;
        }
        
        public static AnidbFile create(net.anidb.File file, Dao<AnidbFile, String> dao, Logger logger) {
            AnidbFile res = new AnidbFile();
            res.setAnimeId(file.getEpisode().getAnime().getAnimeId());
            res.setCRC32(file.getCrc32());
            res.setEd2k(file.getEd2k());
            res.setEpisodeId(file.getEpisode().getEpisodeId());
            res.setFileId(file.getFileId());
            res.setGroupId(file.getGroup().getGroupId());
            res.setMD5(file.getMd5());
            res.setRetrieved(new Date());
            res.setSHA1(file.getSha1());
            res.setSize(file.getSize());
            try {
                dao.create(res);
            } catch (SQLException e) {
                logger.error(LOG_MESSAGE + "Encountered an SQL error");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                e.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
            return res;
        }
    }

    public boolean scan(Person person) {
        return true;
    }
}
class AnidbEpisodeFactory {
    private static final String LOG_MESSAGE = "AniDbPlugin:AnidbEpisodeFactory: ";
    public static AnidbEpisode create(Episode ep, Dao<AnidbEpisode, String> dao, Logger logger) {
        AnidbEpisode res = new AnidbEpisode();
        res.setAnimeId(ep.getAnime().getAnimeId());
        res.setAired(new Date(ep.getAired()));
        res.setEpisodeId(ep.getEpisodeId());
        res.setEnglishName(ep.getEnglishTitle());
        res.setEpisodeNumber(ep.getEpisodeNumber());
        res.setKanjiName(ep.getKanjiTitle());
        res.setLength(ep.getLength());
        res.setRating(ep.getRating());
        res.setRetrieved(new Date());
        res.setRomajiName(ep.getRomajiTitle());
        res.setVotes(ep.getVotes());
        
        try {
            dao.create(res);
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return res;
    }
    
    public static AnidbEpisode load(long eid, Dao<AnidbEpisode, String> dao, Logger logger) {
        try {
            AnidbEpisode episode = dao.queryForId(Long.toString(eid));
            return episode;
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error while loading episode id " + eid);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return null;
    }
    
    public static AnidbEpisode load(long animeId, long episodeNumber, Dao<AnidbEpisode, String> dao, Logger logger) {
        try {
            QueryBuilder<AnidbEpisode, String> qb = dao.queryBuilder();
            qb.where().eq(AnidbEpisode.ANIME_ID_COLUMN, Long.toString(animeId)).and().eq(AnidbEpisode.EPISODE_NUMBER_COLUMN, Long.toString(episodeNumber));
            PreparedQuery<AnidbEpisode> pq = qb.prepare();
            List<AnidbEpisode> res = dao.query(pq);
            if (res.size() > 1) {
                logger.error(LOG_MESSAGE + "Duplicate entries for anime " + animeId + " episode number " + episodeNumber);
            }
            if (res.size() > 0) {
                return res.get(0);
            }
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error while loading anime " + animeId + " episode number " + episodeNumber);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return null;
    }
}

class AnimeFactory {
    private static final String LOG_MESSAGE = "AniDbPlugin:AnimeFactory: ";
    
    private AnimeFactory() {}
    public static AnidbAnime create(Anime anime, String description, Dao<AnidbAnime, String> dao,Dao<AnidbCategory, String> categoryDao, 
        Logger logger){
        AnidbAnime ret = new AnidbAnime();
        ret.setAnimeId(anime.getAnimeId());
        ret.setEnglishName(anime.getEnglishName());
        ret.setKanjiName(anime.getKanjiName());
        ret.setRetrieved(new Date());
        ret.setRomajiName(anime.getRomajiName());
        ret.setType(anime.getType());
        ret.setYear(anime.getYear());
        ret.setAirDate(anime.getAirDate());
        if (anime.getEndDate() != null) {
            ret.setEndDate(anime.getEndDate());
        }
        if (anime.getRating() != null) {
            ret.setRating(anime.getRating());
        }
        ret.setDescription(description);
        try {
            dao.create(ret);            
            @SuppressWarnings("unused")
            List<AnidbCategory> list = new ArrayList<AnidbCategory>();
            for(int i = 0; i < anime.getCategoryList().size(); ++i) {
                AnidbCategory category = new AnidbCategory();
                category.setAnime(ret);
                category.setCategoryName(anime.getCategoryList().get(i));
                category.setWeight(Integer.parseInt(anime.getCategoryWeightList().get(i)));
                categoryDao.create(category);
            }
            ret = dao.queryForId(Long.toString(ret.getAnimeId())); // To populate the categories collection
            addToCache(ret);
            return ret;
        } catch (SQLException e) {
            logger.error(LOG_MESSAGE + "Encountered an SQL error");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
      
        return null;
    }
    
    public static AnidbAnime load(long aid, Dao<AnidbAnime, String> dao, Logger logger) throws SQLException {
        AnidbAnime anime = getAnimeFromCache(aid);
        if (anime != null) {
            return anime;
        }
        anime = dao.queryForId(Long.toString(aid));
        
        if (anime != null) {
            addToCache(anime);
        }
        return anime;
    }
    
    public static AnidbAnime load(String name, Dao<AnidbAnime, String> dao, Logger logger) throws SQLException {
        AnidbAnime anime = getAnimeFromCache(name);
        if (anime != null) {
            return anime;
        }
        QueryBuilder<AnidbAnime, String> qb = dao.queryBuilder();
        qb.where().eq(AnidbAnime.ROMAJI_NAME_COLUMN, name).or().eq(AnidbAnime.ENGLISH_NAME_COLUMN, name);
        PreparedQuery<AnidbAnime> pq = qb.prepare();
        List<AnidbAnime> res = dao.query(pq); 
        if (res.size() > 0) {
            anime = res.get(0);
            addToCache(anime);
            return anime;
        }
        return null;
    }
    private static AnidbAnime getAnimeFromCache(String name) {
        AnidbAnime anime = null;
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameEnglish", name))) != null) {
            return anime;
        }        
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameRomaji", name))) != null) {
            return anime;
        }        
        return null;
    }
    
    private static AnidbAnime getAnimeFromCache(long aid) {
        AnidbAnime anime = null;
        if ((anime = (AnidbAnime)Cache.getFromCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeId", Long.toString(aid)))) != null) {
            return anime;
        }
        return null;
    }
    
    private static void addToCache(AnidbAnime anime) {
        Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeId", Long.toString(anime.getAnimeId())), anime);
        if (StringTools.isValidString(anime.getEnglishName())) {
            Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameEnglish", anime.getEnglishName()), anime);
        }
        if (StringTools.isValidString(anime.getRomajiName())) {
            Cache.addToCache(Cache.generateCacheKey(AniDbPlugin.ANIDB_PLUGIN_ID, "AnimeNameRomaji", anime.getRomajiName()), anime);
        }
    }
}

/**
 * Holds information about a scanned file on the local system
 */
@DatabaseTable(tableName = "local_files")
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
    public AnidbAnime() {
        endDate = 0;
        rating = 0;
        episodeCount = 0;
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

    public ForeignCollection<AnidbCategory> getCategories() {
        return categories;
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
    
    @DatabaseField(uniqueIndex = true, columnName = CATEGORY_NAME_COLUMN, canBeNull = false)
    private String categoryName;

    @DatabaseField(foreign = true) // Doing it like this will use more db space but requires less coding.
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
    
    public AnidbTableInfo() {
        
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}