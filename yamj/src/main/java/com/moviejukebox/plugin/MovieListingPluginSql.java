/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.mjbsqldb.DatabaseWriter;
import com.moviejukebox.mjbsqldb.MjbSqlDb;
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
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.pojava.datetime.DateTime;

public class MovieListingPluginSql extends MovieListingPluginBase implements MovieListingPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieListingPluginSql.class);
    private static final String LOG_MESSAGE = "MovieListingPluginSql: ";
    private static final String DB_LOCATION = PropertiesUtil.getProperty("mjb.sql.location", "./");
    private static final String DB_NAME = PropertiesUtil.getProperty("mjb.sql.dbname", "listing.db");
    private Connection mjbConn = null;

    @SuppressWarnings("deprecation")
    @Override
    public void generate(Jukebox jukebox, Library library) {
        MjbSqlDb mjbSqlDb;
        try {
            mjbSqlDb = new MjbSqlDb(DB_LOCATION, DB_NAME);
        } catch (SQLException ex) {
            LOG.error(LOG_MESSAGE + "Failed to generate the listing: " + ex.getMessage());
            return;
        }
        mjbConn = mjbSqlDb.getConnection();

        int videoId;
        int joinId;
        VideoDTO videoDTO;

        for (Movie movie : library.values()) {
            LOG.info(LOG_MESSAGE + "Writing movie to Database - " + movie.getTitle());
            videoDTO = new VideoDTO();

            videoDTO.setId(0);  // Set the ID to 0 to get the database to generate the next available ID
            videoDTO.setMjbVersion(movie.getMjbVersion());
            if (StringTools.isValidString(movie.getMjbRevision())) {
                videoDTO.setMjbRevision(Integer.parseInt(movie.getMjbRevision()));
            } else {
                videoDTO.setMjbRevision(0);
            }
            videoDTO.setMjbUpdateDate(DateTimeTools.convertDateToString(new Date(), DateTimeTools.getDateFormatLongString()));
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
            videoDTO.setRuntime(DateTimeTools.processRuntime(movie.getRuntime()));
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
                videoId = DatabaseWriter.insertVideo(mjbConn, videoDTO);
                mjbConn.commit();
            } catch (SQLException error) {
                LOG.error(LOG_MESSAGE + "Error adding the video to the database: " + error.getMessage());
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
                    DatabaseWriter.insertVideoSite(mjbConn, vsDTO);
                }
                mjbConn.commit();
            } catch (SQLException ex) {
                LOG.error(LOG_MESSAGE + "Error adding Video Site IDs to the database: " + ex.getMessage());
            }

            try {
                // Add the country
                if (StringTools.isValidString(movie.getCountriesAsString())) {
                    joinId = DatabaseWriter.insertCountry(mjbConn, new CountryDTO(0, movie.getCountriesAsString(), ""));
                    DatabaseWriter.joinCountry(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (SQLException ex) {
                LOG.error(LOG_MESSAGE + "Error adding Country to the database: " + ex.getMessage());
            }

            try {
                // Add the company
                if (StringTools.isValidString(movie.getCompany())) {
                    joinId = DatabaseWriter.insertCompany(mjbConn, new CompanyDTO(0, movie.getCompany(), ""));
                    DatabaseWriter.joinCompany(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (SQLException ex) {
                LOG.error(LOG_MESSAGE + "Error adding Company to the database: " + ex.getMessage());
            }

            try {
                // Add the language
                if (StringTools.isValidString(movie.getLanguage())) {
                    joinId = DatabaseWriter.insertLanguage(mjbConn, determineLanguage(movie.getLanguage()));
                    DatabaseWriter.joinLanguage(mjbConn, videoId, joinId);
                }
                mjbConn.commit();
            } catch (SQLException ex) {
                LOG.error(LOG_MESSAGE + "Error adding Languages to the database: " + ex.getMessage());
            }

            // Process the video files

            VideoFileDTO vfDTO;
            VideoFilePartDTO vfpDTO;
            int videoFileId;
            int audioCodecId = 0;
            int videoCodecId = 0;

            try {
                // Add the audio codec
                if (StringTools.isValidString(movie.getAudioCodec())) {
                    audioCodecId = DatabaseWriter.insertCodec(mjbConn, new CodecDTO(0, movie.getAudioCodec(), CodecDTO.TYPE_AUDIO));
                }

                // Add the video codec
                if (StringTools.isValidString(movie.getVideoCodec())) {
                    videoCodecId = DatabaseWriter.insertCodec(mjbConn, new CodecDTO(0, movie.getVideoCodec(), CodecDTO.TYPE_VIDEO));
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
                    vfDTO.setFileDate((new DateTime(mf.getLastModified())).toString(DateTimeTools.getDateFormatLongString()));
                    vfDTO.setFileSize(mf.getSize());
                    vfDTO.setFileLocation(mf.getFile().getAbsolutePath());
                    vfDTO.setFileUrl(mf.getFilename());
                    vfDTO.setNumberParts(mf.getLastPart() - mf.getFirstPart() + 1);
                    vfDTO.setFirstPart(mf.getFirstPart());
                    vfDTO.setLastPart(mf.getLastPart());

                    videoFileId = DatabaseWriter.insertVideoFile(mjbConn, vfDTO);
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
                        DatabaseWriter.insertVideoFilePart(mjbConn, vfpDTO);
                    }
                    mjbConn.commit();
                }
                mjbConn.commit();

            } catch (SQLException error) {
                LOG.error(LOG_MESSAGE + "Error updating VideoFile & VideoFilePart: " + error.getMessage());
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
            joinId = DatabaseWriter.insertCertification(mjbConn, certDTO);
            mjbConn.commit();

            return joinId;
        } catch (SQLException ex) {
            return joinId;
        }

    }

    private LanguageDTO determineLanguage(String language) {
        LanguageDTO lang = new LanguageDTO();
        lang.setId(0);
        lang.setLanguage(language);

        String shortCode = "";
        String mediumCode = "";
        String longCode = "";

        for (String langStr : MovieFilenameScanner.getLanguageList(language).split(" ")) {
            if (langStr.length() == 2) {
                shortCode = langStr;
                continue;
            }

            if (langStr.length() == 3) {
                mediumCode = langStr;
                continue;
            }

            longCode = langStr;
        }

        lang.setShortCode(shortCode);
        lang.setMediumCode(mediumCode);
        lang.setLongCode(longCode);
        return lang;
    }

    /**
     * Add a genre to the database
     *
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
            joinId = DatabaseWriter.insertGenre(mjbConn, genreDTO);
            DatabaseWriter.joinGenre(mjbConn, videoId, joinId);
            mjbConn.commit();
        } catch (SQLException ex) {
            LOG.error(LOG_MESSAGE + "Error adding Genres to the database: " + ex.getMessage());
        }

        return joinId;
    }

    /**
     * Add an artwork to the database
     *
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
            joinId = DatabaseWriter.updateArtwork(mjbConn, artwork);

            mjbConn.commit();
        } catch (SQLException ex) {
            LOG.error(LOG_MESSAGE + "Error adding Artwork to the database: " + ex.getMessage());
        }
        return joinId;
    }

    /**
     * Add a person to the person list
     *
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
            joinId = DatabaseWriter.insertPerson(mjbConn, personDTO);
            DatabaseWriter.joinPerson(mjbConn, videoId, joinId);
            mjbConn.commit();
        } catch (SQLException ex) {
            LOG.error(LOG_MESSAGE + "Error adding People to the database: " + ex.getMessage());
        }
        return joinId;
    }
}
