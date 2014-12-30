/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.mjbsqldb;

import com.moviejukebox.mjbsqldb.dto.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public final class DatabaseWriter {

    private static final Logger LOGGER = Logger.getLogger(DatabaseWriter.class);
    private static final String LOG_MESSAGE = "dbWriter: ";
    // Join tables
    private static final String JOIN_TABLES = "insert into ? (VIDEO_ID, ?) values (?, ?)";
    // Delete Ids
    private static final String DELETE_ID_1_COL = "delete from ? where ? = ?";
    private static final String DELETE_ID_2_COL = "delete from ? where ? = ? and ? = ?";

    private DatabaseWriter() {
        throw new IllegalArgumentException("Class cannot be initialised!");
    }

    public static int deleteIdFromTable(Connection connection, String tableName, String columnName, int tableId) throws SQLException {
        return deleteIdFromTable(connection, tableName, columnName, tableId, null, null);
    }

    public static int deleteIdFromTable(Connection connection, String tableName, String columnName1, int tableId1, String columnName2, String tableId2) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            if (columnName2 == null) {
                pstmt = connection.prepareStatement(DELETE_ID_1_COL);
                pstmt.setString(1, tableName);
                pstmt.setString(2, columnName1);
                pstmt.setInt(3, tableId1);
            } else {
                pstmt = connection.prepareStatement(DELETE_ID_2_COL);
                pstmt.setString(1, tableName);
                pstmt.setString(2, columnName1);
                pstmt.setInt(3, tableId1);
                pstmt.setString(2, columnName2);
                pstmt.setString(3, tableId2);
            }
            pstmt.execute();
        } catch (SQLException ex) {
            throw new SQLException("Error deleting row from " + tableName + " table: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }

        return 0;
    }

    public static int insertArtwork(Connection connection, ArtworkDTO artworkDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (artworkDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getArtworkId(connection, artworkDTO.getFilename());
            if (id == 0) {
                // Create a new ID
                artworkDTO.setId(DatabaseReader.getNextArtworkId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(ArtworkDTO.INSERT_TABLE);
            pstmt.setInt(1, artworkDTO.getId());
            pstmt.setString(2, artworkDTO.getFilename());
            pstmt.setString(3, artworkDTO.getUrl());
            pstmt.setString(4, artworkDTO.getType());
            pstmt.setInt(5, artworkDTO.getRelatedId());
            pstmt.setString(6, artworkDTO.getForeignKey());
            pstmt.executeUpdate();

            return artworkDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Artwork:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertCertification(Connection connection, CertificationDTO certificationDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (certificationDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getCertificationId(connection, certificationDTO.getCertification());
            if (id == 0) {
                // Create a new ID
                certificationDTO.setId(DatabaseReader.getNextCertificationId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CertificationDTO.INSERT_TABLE);
            pstmt.setInt(1, certificationDTO.getId());
            pstmt.setString(2, certificationDTO.getCertification());
            pstmt.executeUpdate();
            connection.commit();

            return certificationDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Certification:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertCodec(Connection connection, CodecDTO codecDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (codecDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getCodecId(connection, codecDTO.getCodec());
            if (id == 0) {
                // Create a new ID
                codecDTO.setId(DatabaseReader.getNextCodecId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CodecDTO.INSERT_TABLE);
            pstmt.setInt(1, codecDTO.getId());
            pstmt.setString(2, codecDTO.getCodec());
            pstmt.setString(3, codecDTO.getType());
            pstmt.executeUpdate();
            connection.commit();

            return codecDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Codec " + codecDTO.toString() + ":  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertCompany(Connection connection, CompanyDTO companyDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (companyDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getCompanyId(connection, companyDTO.getCompany());
            if (id == 0) {
                // Create a new ID
                companyDTO.setId(DatabaseReader.getNextCompanyId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CompanyDTO.INSERT_TABLE);
            pstmt.setInt(1, companyDTO.getId());
            pstmt.setString(2, companyDTO.getCompany());
            pstmt.setString(3, companyDTO.getUrl());
            pstmt.executeUpdate();

            return companyDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Company:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertCountry(Connection connection, CountryDTO countryDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (countryDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getCountryId(connection, countryDTO.getCountry());
            if (id == 0) {
                // Create a new ID
                countryDTO.setId(DatabaseReader.getNextCountryId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CountryDTO.INSERT_TABLE);
            pstmt.setInt(1, countryDTO.getId());
            pstmt.setString(2, countryDTO.getCountry());
            pstmt.setString(3, countryDTO.getUrl());
            pstmt.executeUpdate();

            return countryDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Country:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertGenre(Connection connection, GenreDTO genreDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (genreDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getGenreId(connection, genreDTO.getName());
            if (id == 0) {
                // Create a new ID
                genreDTO.setId(DatabaseReader.getNextGenreId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(GenreDTO.INSERT_TABLE);
            pstmt.setInt(1, genreDTO.getId());
            pstmt.setString(2, genreDTO.getName());
            pstmt.setString(3, genreDTO.getForeignKey());
            pstmt.executeUpdate();

            return genreDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Genre:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertLanguage(Connection connection, LanguageDTO languageDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (languageDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getLanguageId(connection, languageDTO.getLanguage());
            if (id == 0) {
                // Create a new ID
                languageDTO.setId(DatabaseReader.getNextLanguageId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(LanguageDTO.INSERT_TABLE);
            pstmt.setInt(1, languageDTO.getId());
            pstmt.setString(2, languageDTO.getLanguage());
            pstmt.setString(3, languageDTO.getShortCode());
            pstmt.setString(4, languageDTO.getMediumCode());
            pstmt.setString(5, languageDTO.getLongCode());
            pstmt.executeUpdate();

            return languageDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Language:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertPerson(Connection connection, PersonDTO personDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (personDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getPersonId(connection, personDTO.getName(), personDTO.getJob());
            if (id == 0) {
                // Create a new ID
                personDTO.setId(DatabaseReader.getNextPersonId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(PersonDTO.INSERT_TABLE);
            pstmt.setInt(1, personDTO.getId());
            pstmt.setString(2, personDTO.getName());
            pstmt.setString(3, personDTO.getJob());
            pstmt.setString(4, personDTO.getForeignKey());
            pstmt.setString(5, personDTO.getUrl());
            pstmt.setString(6, personDTO.getBiography());
            pstmt.setString(7, personDTO.getBirthday());
            pstmt.executeUpdate();

            return personDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Person:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertVideo(Connection connection, VideoDTO videoDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (videoDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getVideoId(connection, videoDTO.getTitle());
            if (id == 0) {
                // Create a new ID
                videoDTO.setId(DatabaseReader.getNextVideoId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoDTO.INSERT_TABLE);

            pstmt.setInt(1, videoDTO.getId());
            pstmt.setString(2, videoDTO.getMjbVersion());
            pstmt.setInt(3, videoDTO.getMjbRevision());
            pstmt.setObject(4, videoDTO.getMjbUpdateDate());
            pstmt.setString(5, videoDTO.getBaseFilename());
            pstmt.setString(6, videoDTO.getTitle());
            pstmt.setString(7, videoDTO.getTitleSort());
            pstmt.setString(8, videoDTO.getTitleOriginal());
            pstmt.setObject(9, videoDTO.getReleaseDate());
            pstmt.setInt(10, videoDTO.getRating());
            pstmt.setInt(11, videoDTO.getTop250());
            pstmt.setString(12, videoDTO.getPlot());
            pstmt.setString(13, videoDTO.getOutline());
            pstmt.setString(14, videoDTO.getQuote());
            pstmt.setString(15, videoDTO.getTagline());
            pstmt.setInt(16, videoDTO.getRuntime());
            pstmt.setString(17, videoDTO.getVideoType());
            pstmt.setInt(18, videoDTO.getSeason());
            pstmt.setString(19, videoDTO.getSubtitles());
            pstmt.setString(20, videoDTO.getLibraryDescription());
            pstmt.setInt(21, videoDTO.getCertificationId());
            pstmt.executeUpdate();

            return videoDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Video:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertVideoFile(Connection connection, VideoFileDTO videoFileDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (videoFileDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getVideoFileId(connection, videoFileDTO.getFileLocation());
            if (id == 0) {
                // Create a new ID
                videoFileDTO.setId(DatabaseReader.getNextVideoFileId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoFileDTO.INSERT_TABLE);
            pstmt.setInt(1, videoFileDTO.getId());
            pstmt.setInt(2, videoFileDTO.getVideoId());
            pstmt.setString(3, videoFileDTO.getFileLocation());
            pstmt.setString(4, videoFileDTO.getFileUrl());
            pstmt.setString(5, videoFileDTO.getContainer());
            pstmt.setInt(6, videoFileDTO.getAudioChannels());
            pstmt.setInt(7, videoFileDTO.getVideoCodecId());
            pstmt.setInt(8, videoFileDTO.getAudioCodecId());
            pstmt.setString(9, videoFileDTO.getResolution());
            pstmt.setString(10, videoFileDTO.getVideoSource());
            pstmt.setString(11, videoFileDTO.getVideoOutput());
            pstmt.setString(12, videoFileDTO.getAspect());
            pstmt.setFloat(13, videoFileDTO.getFps());
            pstmt.setObject(14, videoFileDTO.getFileDate());
            pstmt.setLong(15, videoFileDTO.getFileSize());
            pstmt.setInt(16, videoFileDTO.getNumberParts());
            pstmt.setInt(17, videoFileDTO.getFirstPart());
            pstmt.setInt(18, videoFileDTO.getLastPart());
            pstmt.executeUpdate();

            return videoFileDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Video File:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static int insertVideoFilePart(Connection connection, VideoFilePartDTO videoFilePartDTO) throws SQLException {
        // See if we have a value (non-zero) ID
        if (videoFilePartDTO.getId() == 0) {
            // See if the ID exists already
            int id = DatabaseReader.getVideoFilePartId(connection, videoFilePartDTO.getFileId(), videoFilePartDTO.getPart());
            if (id == 0) {
                // Create a new ID
                videoFilePartDTO.setId(DatabaseReader.getNextVideoFilePartId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoFilePartDTO.INSERT_TABLE);
            pstmt.setInt(1, videoFilePartDTO.getId());
            pstmt.setInt(2, videoFilePartDTO.getFileId());
            pstmt.setInt(3, videoFilePartDTO.getPart());
            pstmt.setString(4, videoFilePartDTO.getTitle());
            pstmt.setString(5, videoFilePartDTO.getPlot());
            pstmt.setInt(6, videoFilePartDTO.getSeason());
            pstmt.executeUpdate();

            return videoFilePartDTO.getId();
        } catch (SQLException ex) {
            throw new SQLException("Error inserting Video File Part:  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    public static String insertVideoSite(Connection connection, VideoSiteDTO videoSiteDTO) throws SQLException {
        if (videoSiteDTO.getVideoId() == 0) {
            return "";
        }

        String siteID = DatabaseReader.getVideoSiteId(connection, videoSiteDTO.getVideoId(), videoSiteDTO.getSite());
        if (siteID.equals("")) {
            PreparedStatement pstmt = null;
            try {
                pstmt = connection.prepareStatement(VideoSiteDTO.INSERT_TABLE);
                pstmt.setInt(1, videoSiteDTO.getVideoId());
                pstmt.setString(2, videoSiteDTO.getSite());
                pstmt.setString(3, videoSiteDTO.getSiteId());
                pstmt.executeUpdate();

                return videoSiteDTO.getSiteId();
            } catch (SQLException ex) {
                throw new SQLException("Error inserting Video Site: " + ex.getMessage(), ex);
            } finally {
                if (pstmt != null) {
                    pstmt.close();
                }
            }
        } else {
            return siteID;
        }
    }

    public static void joinCompany(Connection connection, int videoId, int companyId) throws SQLException {
        joinTable(connection, "VIDEO_COMPANY", "COMPANY_ID", videoId, companyId);
    }

    public static void joinCountry(Connection connection, int videoId, int countryId) throws SQLException {
        joinTable(connection, "VIDEO_COUNTRY", "COUNTRY_ID", videoId, countryId);
    }

    public static void joinGenre(Connection connection, int videoId, int genreId) throws SQLException {
        joinTable(connection, "VIDEO_GENRE", "GENRE_ID", videoId, genreId);
    }

    public static void joinLanguage(Connection connection, int videoId, int languageId) throws SQLException {
        joinTable(connection, "VIDEO_LANGUAGE", "LANGUAGE_ID", videoId, languageId);
    }

    public static void joinPerson(Connection connection, int videoId, int personId) throws SQLException {
        joinTable(connection, "VIDEO_PERSON", "PERSON_ID", videoId, personId);
    }

    /**
     * Generic method to update a join table
     *
     * @param connection
     * @param insertCommand
     * @param videoId
     * @param joinId
     */
    private static void joinTable(Connection connection, String joinTable, String joinColumn, int videoId, int joinId) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(JOIN_TABLES);
            pstmt.setString(1, joinTable);
            pstmt.setString(2, joinColumn);
            pstmt.setInt(3, videoId);
            pstmt.setInt(4, joinId);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            throw new SQLException("Error writing to join table (" + joinTable + "):  " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /**
     * Update the artwork table
     *
     * @param connection
     * @param artwork
     * @return
     * @throws SQLException
     */
    public static int updateArtwork(Connection connection, ArtworkDTO artwork) throws SQLException {
        try {
            // Check for the ID. If > 0 then we need to delete first
            if (artwork.getId() > 0) {
                deleteIdFromTable(connection, "artwork", "id", artwork.getId());
            }

            insertArtwork(connection, artwork);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating artwork table: " + ex.getMessage(), ex);
        }
        return artwork.getId();
    }

    public static int updateCertification(Connection connection, CertificationDTO certification) throws SQLException {
        try {
            if (certification.getId() > 0) {
                deleteIdFromTable(connection, "certification", "id", certification.getId());
            }

            insertCertification(connection, certification);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating certification table: " + ex.getMessage(), ex);
        }
        return certification.getId();
    }

    public static int updateCodec(Connection connection, CodecDTO codec) throws SQLException {
        try {
            if (codec.getId() > 0) {
                deleteIdFromTable(connection, "codec", "id", codec.getId());
            }

            insertCodec(connection, codec);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating codec table: " + ex.getMessage(), ex);
        }
        return codec.getId();
    }

    public static int updateCompany(Connection connection, CompanyDTO company) throws SQLException {
        try {
            if (company.getId() > 0) {
                deleteIdFromTable(connection, "company", "id", company.getId());
            }

            insertCompany(connection, company);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating company table: " + ex.getMessage(), ex);
        }
        return company.getId();
    }

    public static int updateCountry(Connection connection, CountryDTO country) throws SQLException {
        try {
            if (country.getId() > 0) {
                deleteIdFromTable(connection, "country", "id", country.getId());
            }

            insertCountry(connection, country);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating country table: " + ex.getMessage(), ex);
        }
        return country.getId();
    }

    public static int updateGenre(Connection connection, GenreDTO genre) throws SQLException {
        try {
            if (genre.getId() > 0) {
                deleteIdFromTable(connection, "genre", "id", genre.getId());
            }

            insertGenre(connection, genre);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating genre table: " + ex.getMessage(), ex);
        }
        return genre.getId();
    }

    public static int updateLanguage(Connection connection, LanguageDTO language) throws SQLException {
        try {
            if (language.getId() > 0) {
                deleteIdFromTable(connection, "language", "id", language.getId());
            }

            insertLanguage(connection, language);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating language table: " + ex.getMessage(), ex);
        }
        return language.getId();
    }

    public static int updatePerson(Connection connection, PersonDTO person) throws SQLException {
        try {
            if (person.getId() > 0) {
                deleteIdFromTable(connection, "person", "id", person.getId());
            }

            insertPerson(connection, person);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating person table: " + ex.getMessage(), ex);
        }
        return person.getId();
    }

    public static int updateVideo(Connection connection, VideoDTO video) throws SQLException {
        try {
            if (video.getId() > 0) {
                deleteIdFromTable(connection, "video", "id", video.getId());
            }

            insertVideo(connection, video);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating video table: " + ex.getMessage(), ex);
        }
        return video.getId();
    }

    public static int updateVideoFile(Connection connection, VideoFileDTO videoFile) throws SQLException {
        try {
            if (videoFile.getId() > 0) {
                deleteIdFromTable(connection, "video_file", "id", videoFile.getId());
            }

            insertVideoFile(connection, videoFile);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating Video File table: " + ex.getMessage(), ex);
        }
        return videoFile.getId();
    }

    public static int updateVideoFilePart(Connection connection, VideoFilePartDTO videoFilePart) throws SQLException {
        try {
            if (videoFilePart.getId() > 0) {
                deleteIdFromTable(connection, "video_file_part", "id", videoFilePart.getId());
            }

            insertVideoFilePart(connection, videoFilePart);

            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating artwork table: " + ex.getMessage(), ex);
        }
        return videoFilePart.getId();
    }

    public static int updateVideoSite(Connection connection, VideoSiteDTO videoSite) throws SQLException {
        try {
            deleteIdFromTable(connection, "video_site", "video_id", videoSite.getVideoId(), "site", videoSite.getSite());
        } catch (SQLException ex) {
            LOGGER.debug(LOG_MESSAGE + "Error deleting VideoId '" + videoSite.getVideoId() + "' from database: " + ex.getMessage());
        }

        try {
            insertVideoSite(connection, videoSite);
            connection.commit();
        } catch (SQLException ex) {
            throw new SQLException("Error updating Video Site table: " + ex.getMessage(), ex);
        }
        return videoSite.getVideoId();
    }
}
