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

import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.processRuntime;

import java.sql.Connection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.pojava.datetime.DateTime;

import com.moviejukebox.mjbsqldb.MjbSqlDb;
import com.moviejukebox.mjbsqldb.dbWriter;
import com.moviejukebox.mjbsqldb.dto.ArtworkDTO;
import com.moviejukebox.mjbsqldb.dto.CertificationDTO;
import com.moviejukebox.mjbsqldb.dto.CodecDTO;
import com.moviejukebox.mjbsqldb.dto.CompanyDTO;
import com.moviejukebox.mjbsqldb.dto.CountryDTO;
import com.moviejukebox.mjbsqldb.dto.GenreDTO;
import com.moviejukebox.mjbsqldb.dto.LanguageDTO;
import com.moviejukebox.mjbsqldb.dto.PersonDTO;
import com.moviejukebox.mjbsqldb.dto.VideoDTO;
import com.moviejukebox.mjbsqldb.dto.VideoFileDTO;
import com.moviejukebox.mjbsqldb.dto.VideoFilePartDTO;
import com.moviejukebox.mjbsqldb.dto.VideoSiteDTO;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

public class MovieListingPluginSql extends MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Connection mjbConn = null;
    private static String dbLocation = PropertiesUtil.getProperty("mjb.sql.location", "./");
    private static String dbName = PropertiesUtil.getProperty("mjb.sql.dbname", "listing.db");
    
    public void generate(Jukebox jukebox, Library library) {
        MjbSqlDb mjbSqlDb = new MjbSqlDb(dbLocation, dbName);
        mjbConn = MjbSqlDb.getConnection();
        
        int videoId = 0;
        int joinId = 0;
        VideoDTO videoDTO;
        
        for (Movie movie : library.values()) {
            logger.fine("MovieListingPluginSql: Writing movie to Database - " + movie.getTitle());
            videoDTO = new VideoDTO();
            videoId = 0;

            videoDTO.setId(0);  // Set the ID to 0 to get the database to generate the next available ID
            videoDTO.setMjbVersion(movie.getMjbVersion());
            if (StringTools.isValidString(movie.getMjbRevision())) {
                videoDTO.setMjbRevision(Integer.parseInt(movie.getMjbRevision()));
            } else {
                videoDTO.setMjbRevision(0);
            }
            videoDTO.setMjbUpdateDate(StringTools.convertDateToString(new Date(), Movie.dateFormatLong));
            videoDTO.setBaseFilename(movie.getBaseFilename());
            videoDTO.setTitle(movie.getTitle());
            videoDTO.setTitleSort(movie.getTitleSort());
            videoDTO.setTitleOriginal(movie.getOriginalTitle());
            
            if (isValidString(movie.getReleaseDate())) {
                // A bit kludgy, but it works. Use DateTime to parse the string date into a Date
                videoDTO.setReleaseDate(movie.getReleaseDate());
            }
            
            videoDTO.setRating(movie.getRating());
            videoDTO.setTop250(movie.getTop250());
            videoDTO.setPlot(movie.getPlot());
            videoDTO.setOutline(movie.getOutline());
            videoDTO.setQuote(movie.getQuote());
            videoDTO.setTagline(movie.getTagline());
            videoDTO.setRuntime(processRuntime(movie.getRuntime()));
            videoDTO.setSeason(movie.getSeason());
            videoDTO.setSubtitles(movie.getSubtitles());
            videoDTO.setLibraryDescription(movie.getLibraryDescription());
            
            if (movie.isTVShow()) {
                videoDTO.setVideoType(VideoDTO.TYPE_TVSHOW);
            } else {
                videoDTO.setVideoType(VideoDTO.TYPE_MOVIE);
            }

            // Certification
            videoDTO.setCertificationId(addCertification(movie.getCertification()));

            // *** Commit the Video to the database and get the Video ID
            try {
                videoId = dbWriter.insertVideo(mjbConn, videoDTO);
                mjbConn.commit();
            } catch (Throwable error) {
                logger.severe("SQL: Error adding the video to the database: " + error.getMessage());
                continue;
            }

            // Save the artwork to the DB
            addArtwork(movie.getPosterFilename(), movie.getPosterURL(), ArtworkDTO.TYPE_POSTER, videoId);
            addArtwork(movie.getFanartFilename(), movie.getFanartURL(), ArtworkDTO.TYPE_FANART, videoId);
            addArtwork(movie.getBannerFilename(), movie.getBannerURL(), ArtworkDTO.TYPE_BANNER, videoId);
            
            // Add the genres to the database
            for (String genre : movie.getGenres()) {
                addGenre(genre, videoId);
            }

            // Add the actors to the database
            for (String person : movie.getCast()) {
                addPerson(person, "Actor", videoId);
            }
            
            // Add the directors to the database
            for (String person : movie.getDirectors()) {
                addPerson(person, "Director", videoId);
            }

            // Add the writers to the database
            for (String person : movie.getWriters()) {
                addPerson(person, "Writer", videoId);
            }
            
            // Use the video id to write the other data to the tables
            try {
                // Create the video site IDs
                for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
                    VideoSiteDTO vsDTO = new VideoSiteDTO(videoId, e.getKey(), e.getValue());
                    dbWriter.insertVideoSite(mjbConn, vsDTO);
                }
                mjbConn.commit();
            } catch (Throwable tw) {
                logger.severe("SQL: Error adding Video Site IDs to the database: " + tw.getMessage());
            }
            
            try {    
                // Add the country
                if (StringTools.isValidString(movie.getCountry())) {
                    joinId = dbWriter.insertCountry(mjbConn, new CountryDTO(0, movie.getCountry(), ""));
                    dbWriter.joinCountry(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (Throwable tw) {
                logger.severe("SQL: Error adding Country to the database: " + tw.getMessage());
            }
            
            try {    
                // Add the company
                if (StringTools.isValidString(movie.getCompany())) {
                    joinId = dbWriter.insertCompany(mjbConn, new CompanyDTO(0, movie.getCompany(), ""));
                    dbWriter.joinCompany(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (Throwable tw) {
                logger.severe("SQL: Error adding Company to the database: " + tw.getMessage());
            }
            
            try {    
                // Add the language
                if (StringTools.isValidString(movie.getLanguage())) {
                    joinId = dbWriter.insertLanguage(mjbConn, determineLanguage(movie.getLanguage()));
                    dbWriter.joinLanguage(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (Throwable tw) {
                logger.severe("SQL: Error adding Languages to the database: " + tw.getMessage());
            }
            
            // Process the video files
            
            VideoFileDTO vfDTO;
            VideoFilePartDTO vfpDTO;
            int videoFileId = 0;
            int audioCodecId = 0;
            int videoCodecId = 0;
            
            try {
                // Add the audio codec
                if (StringTools.isValidString(movie.getAudioCodec())) {
                    audioCodecId = dbWriter.insertCodec(mjbConn, new CodecDTO(0, movie.getAudioCodec(), CodecDTO.TYPE_AUDIO));
                }
                
                // Add the video codec
                if (StringTools.isValidString(movie.getVideoCodec())) {
                    videoCodecId = dbWriter.insertCodec(mjbConn, new CodecDTO(0, movie.getVideoCodec(), CodecDTO.TYPE_VIDEO));
                }
                
                for (MovieFile mf : movie.getFiles()) {
                    vfDTO = new VideoFileDTO();
                    vfDTO.setId(0);
                    vfDTO.setVideoId(videoId);
                    vfDTO.setContainer(movie.getContainer());
                    if (StringTools.isValidString(movie.getAudioChannels())) {
                        vfDTO.setAudioChannels(Integer.parseInt(movie.getAudioChannels()));
                    } else {
                        vfDTO.setAudioChannels(0);
                    }
                    vfDTO.setVideoCodecId(videoCodecId);
                    vfDTO.setAudioCodecId(audioCodecId);
                    vfDTO.setResolution(movie.getResolution());
                    vfDTO.setVideoSource(movie.getVideoSource());
                    vfDTO.setVideoOutput(movie.getVideoOutput());
                    vfDTO.setAspect(movie.getAspectRatio());
                    vfDTO.setFps(movie.getFps());
                    vfDTO.setFileDate((new DateTime(mf.getLastModified())).toString(Movie.dateFormatLongString));
                    vfDTO.setFileSize(mf.getSize());
                    vfDTO.setFileLocation(mf.getFile().getAbsolutePath());
                    vfDTO.setFileUrl(mf.getFilename());
                    vfDTO.setNumberParts(mf.getLastPart() - mf.getFirstPart() + 1);
                    vfDTO.setFirstPart(mf.getFirstPart());
                    vfDTO.setLastPart(mf.getLastPart());
                    
                    videoFileId = dbWriter.insertVideoFile(mjbConn, vfDTO);
                    mjbConn.commit();
                    
                    // Process the video file parts
                    for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                        vfpDTO = new VideoFilePartDTO();
                        vfpDTO.setId(0);
                        vfpDTO.setFileId(videoFileId);
                        vfpDTO.setPart(part);
                        vfpDTO.setTitle(mf.getTitle(part));
                        vfpDTO.setPlot(mf.getPlot(part));
                        vfpDTO.setSeason(movie.getSeason());
                        dbWriter.insertVideoFilePart(mjbConn, vfpDTO);
                    }
                    mjbConn.commit();
                }
                mjbConn.commit();
                
            } catch (Throwable error) {
                logger.severe("SQL: Error updating VideoFile & VideoFilePart: " + error.getMessage());
            }
            
        } // For movie loop
        
        mjbSqlDb.close();
    }
    
    private int addCertification(String certification) {
        int joinId = 0;
        
        if (!isValidString(certification)) {
            return joinId;
        }
        
        CertificationDTO certDTO = new CertificationDTO(0, certification);
        
        try {
            joinId = dbWriter.insertCertification(mjbConn, certDTO);
            mjbConn.commit();
            
            return joinId;
        } catch (Throwable tw) {
            return joinId;
        }

    }

    private LanguageDTO determineLanguage(String language) {
        LanguageDTO lang = new LanguageDTO();
        lang.setId(0);
        lang.setLanguage(language);
        
        String short_code = "", medium_code = "", long_code = "";
        
        for (String langStr : MovieFilenameScanner.getLanguageList(language).split(" ")) {
            if (langStr.length() == 2) {
                short_code = langStr;
                continue;
            }
            
            if (langStr.length() == 3) {
                medium_code = langStr;
                continue;
            }
            
            long_code = langStr;
        }
        
        lang.setShortCode(short_code);
        lang.setMediumCode(medium_code);
        lang.setLongCode(long_code);
        return lang;
    }
    
    /**
     * Add a genre to the database
     * @param genre
     */
    private int addGenre(String genre, int videoId) {
        int joinId = 0;
        
        if (!isValidString(genre)) {
            return joinId;
        }
        
        GenreDTO genreDTO = new GenreDTO(0, Library.getIndexingGenre(genre), "");
        
        try {    
            // Write the Genres to the database
            joinId = dbWriter.insertGenre(mjbConn, genreDTO);
            dbWriter.joinGenre(mjbConn, videoId, joinId);
            mjbConn.commit();
        } catch (Throwable tw) {
            logger.severe("SQL: Error adding Genres to the database: " + tw.getMessage());
        }
        
        return joinId;
    }
    
    /**
     * Add an artwork to the database
     * @param artworkFilename
     * @param artworkUrl
     * @param artworkType
     * @param videoId
     * @return The ID of the created artwork if new, 0 if a failure or the existing ID number 
     */
    private int addArtwork(String artworkFilename, String artworkUrl, String artworkType, int videoId) {
        int joinId = 0;
        
        if (!isValidString(artworkFilename)) {
            return joinId;
        }
        
        ArtworkDTO artwork = new ArtworkDTO(0, artworkFilename, artworkUrl, artworkType, videoId, "");

        try {
            // Write the Artwork to the database
            joinId = dbWriter.updateArtwork(mjbConn, artwork);
            
            mjbConn.commit();
        } catch (Throwable tw) {
            logger.severe("SQL: Error adding Artwork to the database: " + tw.getMessage());
        }
        return joinId;
    }

    /**
     * Add a person to the person list
     * @param person
     * @param job
     */
    private int addPerson(String person, String job, int videoId) {
        if (!isValidString(person) || !isValidString(job)) {
            return 0;
        }
        
        int joinId = 0;
        
        PersonDTO personDTO = new PersonDTO();
        personDTO.setId(0);
        personDTO.setName(person);
        personDTO.setJob(job);
        personDTO.setBiography("");
        personDTO.setBirthday("1970-01-01");
        personDTO.setForeignKey("");
        personDTO.setUrl("");
        
        try {    
            // Write the People to the database
            joinId = dbWriter.insertPerson(mjbConn, personDTO);
            dbWriter.joinPerson(mjbConn, videoId, joinId);
            mjbConn.commit();
        } catch (Throwable tw) {
            logger.severe("SQL: Error adding People to the database: " + tw.getMessage());
        }
        return joinId;
    }
}
