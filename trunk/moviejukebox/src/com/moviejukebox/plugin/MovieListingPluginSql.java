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
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
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
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.StringTools;

public class MovieListingPluginSql extends MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static HashMap<String, ArtworkDTO> artworkList = new HashMap<String, ArtworkDTO>();
    private static HashMap<String, GenreDTO> genreList = new HashMap<String, GenreDTO>();
    private static HashMap<String, PersonDTO> personList = new HashMap<String, PersonDTO>();
    
    public void generate(Jukebox jukebox, Library library) {

        MjbSqlDb mjbSqlDb = new MjbSqlDb("./", "listing.db");
        Connection mjbConn = MjbSqlDb.getConnection();
        
        int videoId = 0;
        int joinId = 0;
        VideoDTO videoDTO;
        
        for (Movie movie : library.values()) {
            logger.fine("MovieListingPluginSql: Writing movie to Database - " + movie.getTitle());
            videoDTO = new VideoDTO();
            videoId = 0;

            artworkList.clear();
            genreList.clear();
            personList.clear();
            
            videoDTO.setId(0);  // Set the ID to 0 to get the database to generate the next available ID
            videoDTO.setMjbVersion(movie.getMjbVersion());
            if (StringTools.isValidString(movie.getMjbRevision())) {
                videoDTO.setMjbRevision(Integer.parseInt(movie.getMjbRevision()));
            } else {
                videoDTO.setMjbRevision(0);
            }
            videoDTO.setMjbUpdateDate(new Date());
            videoDTO.setBaseFilename(movie.getBaseFilename());
            videoDTO.setTitle(movie.getTitle());
            videoDTO.setTitleSort(movie.getTitleSort());
            videoDTO.setTitleOriginal(movie.getOriginalTitle());
            
            if (isValidString(movie.getReleaseDate())) {
                // A bit kludgy, but it works. Use DateTime to parse the string date into a Date
                videoDTO.setReleaseDate((new DateTime(movie.getReleaseDate())).toDate());
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

            for (String genre : movie.getGenres()) {
                addGenre(genre);
            }
            
            for (String person : movie.getCast()) {
                addPerson(person, "Actor");
            }
            
            for (String person : movie.getDirectors()) {
                addPerson(person, "Director");
            }

            for (String person : movie.getWriters()) {
                addPerson(person, "Writer");
            }
            
            addArtwork(movie.getPosterFilename(), movie.getPosterURL(), ArtworkDTO.TYPE_POSTER, videoId);
            addArtwork(movie.getFanartFilename(), movie.getFanartURL(), ArtworkDTO.TYPE_FANART, videoId);
            addArtwork(movie.getBannerFilename(), movie.getBannerURL(), ArtworkDTO.TYPE_BANNER, videoId);

            // Certification
            try {
                if (isValidString(movie.getCertification())) {
                    joinId = dbWriter.insertCertification(mjbConn, addCertification(movie.getCertification()));
                    videoDTO.setCertificationId(joinId);
                } else {
                    videoDTO.setCertificationId(0);
                }
            } catch (Throwable tw) {
                // We can't write the certification, so set to 0 (Zero)
                videoDTO.setCertificationId(0);
            }
            
            // Commit the Video to the database and get the Video ID
            try {
                videoId = dbWriter.insertVideo(mjbConn, videoDTO);
                mjbConn.commit();
            } catch (Throwable error) {
                logger.severe("SQL: Error adding the video to the database: " + error.getMessage());
                continue;
            }
            
            // Use the video id to write the other data to the tables
            // TODO: Break this down into individual try/catch commits
            try {
                // Write the Artwork to the database
                for (ArtworkDTO artwork : artworkList.values()) {
                    joinId = dbWriter.insertArtwork(mjbConn, artwork);
                    dbWriter.joinArtwork(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
                
                // Write the Genres to the database
                for (GenreDTO genre : genreList.values()) {
                    joinId = dbWriter.insertGenre(mjbConn, genre);
                    dbWriter.joinGenre(mjbConn, videoId, joinId);
                }
                mjbConn.commit();

                // Write the People to the database
                for (PersonDTO person : personList.values()) {
                    joinId = dbWriter.insertPerson(mjbConn, person);
                    dbWriter.joinPerson(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
                
                // Add the country
                if (StringTools.isValidString(movie.getCountry())) {
                    joinId = dbWriter.insertCountry(mjbConn, new CountryDTO(0, movie.getCountry(), ""));
                    dbWriter.joinCountry(mjbConn, videoId, joinId);
                }

                // Add the company
                if (StringTools.isValidString(movie.getCompany())) {
                    joinId = dbWriter.insertCompany(mjbConn, new CompanyDTO(0, movie.getCompany(), ""));
                    dbWriter.joinCompany(mjbConn, videoId, joinId);
                }
                
                // Add the language
                if (StringTools.isValidString(movie.getLanguage())) {
                    joinId = dbWriter.insertLanguage(mjbConn, determineLanguage(movie.getLanguage()));
                    dbWriter.joinLanguage(mjbConn, videoId, joinId);
                }
                
            } catch (Throwable error) {
                try {
                    mjbConn.commit();
                } catch (SQLException ignore) {
                }
                logger.severe("SQL: Error adding data to the database: " + error.getMessage());
            }
            
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
                    vfDTO.setFileDate(new Date(mf.getLastModified()));
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
    
    private CertificationDTO addCertification(String certification) {
        CertificationDTO certDTO = new CertificationDTO();
        certDTO.setId(0);
        certDTO.setCertification(certification);
        return certDTO;
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
        
        lang.setShort_code(short_code);
        lang.setMedium_code(medium_code);
        lang.setLong_code(long_code);
        return lang;
    }
    
    /**
     * Add a genre to the genre list
     * @param genre
     */
    private void addGenre(String genre) {
        if (!isValidString(genre)) {
            return;
        }
        
        String indexGenre = Library.getIndexingGenre(genre);

        if (genreList.containsKey(indexGenre.toUpperCase())) {
            return;
        }
        
        GenreDTO g = new GenreDTO();
        g.setId(0);
        g.setName(indexGenre);
        g.setForeignKey("");
        
        genreList.put(genre.toUpperCase(), g);
        return;
    }
    
    /**
     * Add an artwork to the artwork list
     * @param artwork
     * @param url
     * @param type
     * @param videoID
     */
    private void addArtwork(String artwork, String url, String type, int videoID) {
        if (!isValidString(artwork) || !isValidString(url) || !isValidString(type)) {
            return;
        }
        
        if (artworkList.containsKey(artwork.toUpperCase())) {
            return;
        }
        
        ArtworkDTO a = new ArtworkDTO();
        a.setId(0);
        a.setFilename(artwork);
        a.setType(type);
        a.setForeignKey("");
        a.setRelatedKey(videoID);
        a.setUrl(url);
        
        artworkList.put(artwork.toUpperCase(), a);
    }

    /**
     * Add a person to the person list
     * @param person
     * @param job
     */
    private void addPerson(String person, String job) {
        if (!isValidString(person) || !isValidString(job)) {
            return;
        }
        
        if (personList.containsKey((person+job).toUpperCase())) {
            return;
        }
        
        PersonDTO p = new PersonDTO();
        p.setId(0);
        p.setName(person);
        p.setJob(job);
        p.setBiography("");
        p.setBirthday("1970-01-01");
        p.setForeignKey("");
        p.setUrl("");
        
        personList.put((person+job).toUpperCase(), p);
    }
}
