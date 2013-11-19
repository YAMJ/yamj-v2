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
package com.moviejukebox.mjbsqldb;

import com.moviejukebox.mjbsqldb.dto.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

/**
 * Get information from the database
 *
 * @author stuart.boston
 */
public class DatabaseReader {

    private static final String SQL_ID = " ID: ";
    private static final String SQL_ERROR = "Error getting ";
    // Prepared statements
    private static final String SELECT_MAX = "select MAX(?) from ?";
    private static final String SELECT_VIDEO_SITE_ID = "SELECT SITE_ID FROM ? WHERE VIDEO_ID=? AND SITE='?'";
    private static final String SELECT_TABLE_ID_1_SEARCH = "select id from ? where ? = '?'";
    private static final String SELECT_TABLE_ID_2_SEARCH = "select id from ? where ? = '?' and ? = '?'";
    private static final String SELECT_VIDEO_FILE_PART_ID = "select ? from ? where FILE_ID=? and PART=?";
    private static final String SELECT_VIDEO_EXISTS = "select ? from ? where TITLE = '?'";
    private static final String SELECT_ALL_TABLE = "select * from ? where ? = ?";
    private static final String SELECT_VIDEO_ID_TABLE = "select * from ? where VIDEO_ID = ?";
    private static final String SELECT_VIDEO_SITE = "select * from ? where ? = ? and SITE='?'";

    /**
     * Get the last used ID for the table
     *
     * @param connection
     * @param tableName
     * @param idColumnName
     * @return
     * @throws java.sql.SQLException
     */
    public static synchronized int getNextId(Connection connection, String tableName, String idColumnName) throws SQLException {
        if (connection == null) {
            throw new SQLException("Error: No connection specified!");
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_MAX);
            pstmt.setString(1, idColumnName);
            pstmt.setString(2, tableName);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                // update the id by 1 to create the next id
                return (rs.getInt(1) + 1);
            }
            return 1;
        } catch (SQLException ex) {
            throw new SQLException("Error getting the ID for " + tableName + ": " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the next artwork ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextArtworkId(Connection connection) throws SQLException {
        return getNextId(connection, ArtworkDTO.TABLE_NAME, ArtworkDTO.TABLE_KEY);
    }

    /**
     * Get the next genre ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextGenreId(Connection connection) throws SQLException {
        return getNextId(connection, GenreDTO.TABLE_NAME, GenreDTO.TABLE_KEY);
    }

    /**
     * Get the next person ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextPersonId(Connection connection) throws SQLException {
        return getNextId(connection, PersonDTO.TABLE_NAME, PersonDTO.TABLE_KEY);
    }

    /**
     * Get the next video ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextVideoId(Connection connection) throws SQLException {
        return getNextId(connection, VideoDTO.TABLE_NAME, VideoDTO.TABLE_KEY);
    }

    /**
     * Get the next certification ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextCertificationId(Connection connection) throws SQLException {
        return getNextId(connection, CertificationDTO.TABLE_NAME, CertificationDTO.TABLE_KEY);
    }

    /**
     * Get the next codec ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextCodecId(Connection connection) throws SQLException {
        return getNextId(connection, CodecDTO.TABLE_NAME, CodecDTO.TABLE_KEY);
    }

    /**
     * Get the next company ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextCompanyId(Connection connection) throws SQLException {
        return getNextId(connection, CompanyDTO.TABLE_NAME, CompanyDTO.TABLE_KEY);
    }

    /**
     * Get the next country ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextCountryId(Connection connection) throws SQLException {
        return getNextId(connection, CountryDTO.TABLE_NAME, CountryDTO.TABLE_KEY);
    }

    /**
     * Get the next language ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextLanguageId(Connection connection) throws SQLException {
        return getNextId(connection, LanguageDTO.TABLE_NAME, LanguageDTO.TABLE_KEY);
    }

    /**
     * Get the next video file ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextVideoFileId(Connection connection) throws SQLException {
        return getNextId(connection, VideoFileDTO.TABLE_NAME, VideoFileDTO.TABLE_KEY);
    }

    /**
     * Get the next file part ID
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static int getNextVideoFilePartId(Connection connection) throws SQLException {
        return getNextId(connection, VideoFilePartDTO.TABLE_NAME, VideoFilePartDTO.TABLE_KEY);
    }

    /**
     * Get any existing Artwork ID with the filename
     *
     * @param connection
     * @param filename
     * @return
     * @throws java.sql.SQLException
     */
    public static int getArtworkId(Connection connection, String filename) throws SQLException {
        return getTableId(connection, ArtworkDTO.TABLE_NAME, "FILENAME", filename);
    }

    /**
     * Get any existing Certification ID with the certification name
     *
     * @param connection
     * @param certification
     * @return
     * @throws java.sql.SQLException
     */
    public static int getCertificationId(Connection connection, String certification) throws SQLException {
        return getTableId(connection, CertificationDTO.TABLE_NAME, "CERTIFICATION", certification);
    }

    /**
     * Get codec ID
     *
     * @param connection
     * @param codec
     * @return
     * @throws SQLException
     */
    public static int getCodecId(Connection connection, String codec) throws SQLException {
        return getTableId(connection, CodecDTO.TABLE_NAME, "CODEC", codec);
    }

    /**
     * Get video site ID
     *
     * @param connection
     * @param videoId
     * @param site
     * @return
     * @throws SQLException
     */
    public static String getVideoSiteId(Connection connection, int videoId, String site) throws SQLException {
        if (StringUtils.isBlank(site) || videoId < 1) {
            return "";
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_VIDEO_SITE_ID);
            pstmt.setString(1, VideoSiteDTO.TABLE_NAME);
            pstmt.setInt(2, videoId);
            pstmt.setString(3, site);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                return rs.getString(1);
            }

            return "";
        } catch (SQLException ex) {
            throw new SQLException(SQL_ERROR + VideoSiteDTO.TABLE_NAME + SQL_ID + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get Company ID
     *
     * @param connection
     * @param company
     * @return
     * @throws SQLException
     */
    public static int getCompanyId(Connection connection, String company) throws SQLException {
        return getTableId(connection, CompanyDTO.TABLE_NAME, "COMPANY", company);
    }

    /**
     * Get country ID
     *
     * @param connection
     * @param country
     * @return
     * @throws SQLException
     */
    public static int getCountryId(Connection connection, String country) throws SQLException {
        return getTableId(connection, CountryDTO.TABLE_NAME, "COUNTRY", country);
    }

    /**
     * Get genre ID
     *
     * @param connection
     * @param name
     * @return
     * @throws SQLException
     */
    public static int getGenreId(Connection connection, String name) throws SQLException {
        return getTableId(connection, GenreDTO.TABLE_NAME, "NAME", name);
    }

    /**
     * Get Language ID
     *
     * @param connection
     * @param language
     * @return
     * @throws SQLException
     */
    public static int getLanguageId(Connection connection, String language) throws SQLException {
        return getTableId(connection, LanguageDTO.TABLE_NAME, "LANGUAGE", language);
    }

    /**
     * Get person ID
     *
     * @param connection
     * @param name
     * @param job
     * @return
     * @throws SQLException
     */
    public static int getPersonId(Connection connection, String name, String job) throws SQLException {
        return getTableId(connection, PersonDTO.TABLE_NAME, "NAME", name, "JOB", job);
    }

    /**
     * Get the ID from a table using a table name
     *
     * @param connection
     * @param tableName
     * @param columnName1
     * @param searchTerm1
     * @param columnName2
     * @param searchTerm2
     * @return
     * @throws SQLException
     */
    private static int getTableId(Connection connection, String tableName, String columnName1, String searchTerm1, String columnName2, String searchTerm2) throws SQLException {
        if (StringUtils.isBlank(searchTerm1) || StringUtils.isBlank(searchTerm2)) {
            return 0;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_TABLE_ID_2_SEARCH);
            pstmt.setString(1, tableName);
            pstmt.setString(2, columnName1);
            pstmt.setString(3, searchTerm1);
            pstmt.setString(4, columnName2);
            pstmt.setString(5, searchTerm2);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException(SQL_ERROR + tableName + SQL_ID + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the ID from a table using the table name
     *
     * @param connection
     * @param tableName
     * @param columnName
     * @param searchTerm
     * @return
     * @throws SQLException
     */
    private static int getTableId(Connection connection, String tableName, String columnName, String searchTerm) throws SQLException {
        if (StringUtils.isBlank(searchTerm)) {
            return 0;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_TABLE_ID_1_SEARCH);
            pstmt.setString(1, tableName);
            pstmt.setString(2, columnName);
            pstmt.setString(3, searchTerm);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException(SQL_ERROR + tableName + SQL_ID + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the VideoFile ID using the video fileLocation
     *
     * @param connection
     * @param fileLocation
     * @return
     * @throws java.sql.SQLException
     */
    public static int getVideoFileId(Connection connection, String fileLocation) throws SQLException {
        return getTableId(connection, VideoFileDTO.TABLE_NAME, "FILE_LOCATION", fileLocation);
    }

    /**
     * Get the video file part ID
     *
     * @param connection
     * @param fileId
     * @param part
     * @return
     * @throws SQLException
     */
    public static int getVideoFilePartId(Connection connection, int fileId, int part) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_VIDEO_FILE_PART_ID);
            pstmt.setString(1, VideoFilePartDTO.TABLE_KEY);
            pstmt.setString(2, VideoFilePartDTO.TABLE_NAME);
            pstmt.setInt(3, fileId);
            pstmt.setInt(4, part);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException(SQL_ERROR + VideoFilePartDTO.TABLE_NAME + SQL_ID + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the Video ID using the title
     *
     * @param connection
     * @param title
     * @return
     * @throws java.sql.SQLException
     */
    public static int getVideoId(Connection connection, String title) throws SQLException {
        return getTableId(connection, VideoDTO.TABLE_NAME, "TITLE", title);
    }

    /**
     * Check to see if the video exists
     *
     * @param connection
     * @param title
     * @return
     * @throws SQLException
     */
    public static boolean isVideoExists(Connection connection, String title) throws SQLException {
        if (StringUtils.isBlank(title)) {
            //throw new IllegalArgumentException ("Check video exists error because path is blank.");
            return false;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_VIDEO_EXISTS);
            pstmt.setString(1, VideoDTO.TABLE_KEY);
            pstmt.setString(2, VideoDTO.TABLE_NAME);
            pstmt.setString(3, title);

            pstmt.execute();
            rs = pstmt.getResultSet();

            return rs.next();
        } catch (SQLException ex) {
            throw new SQLException("Error checking for Video: " + title + ". " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the artwork info
     *
     * @param connection
     * @param artworkId
     * @return
     * @throws SQLException
     */
    public ArtworkDTO getArtwork(Connection connection, int artworkId) throws SQLException {
        ArtworkDTO artwork = new ArtworkDTO();

        if (artworkId == 0) {
            return artwork;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, ArtworkDTO.TABLE_NAME);
            pstmt.setString(2, ArtworkDTO.TABLE_KEY);
            pstmt.setInt(3, artworkId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                artwork.populateDTO(rs);
            }

            return artwork;
        } catch (SQLException ex) {
            throw new SQLException("Error getting artwork: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the certification
     *
     * @param connection
     * @param certId
     * @return
     * @throws SQLException
     */
    public CertificationDTO getCertification(Connection connection, int certId) throws SQLException {
        CertificationDTO cert = new CertificationDTO();

        if (certId == 0) {
            return cert;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, CertificationDTO.TABLE_NAME);
            pstmt.setString(2, CertificationDTO.TABLE_KEY);
            pstmt.setInt(3, certId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                cert.populateDTO(rs);
            }

            return cert;
        } catch (SQLException ex) {
            throw new SQLException("Error getting certification: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the codec info
     *
     * @param connection
     * @param codecId
     * @return
     * @throws SQLException
     */
    public CodecDTO getCodec(Connection connection, int codecId) throws SQLException {
        CodecDTO codec = new CodecDTO();

        if (codecId == 0) {
            return codec;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, CodecDTO.TABLE_NAME);
            pstmt.setString(2, CodecDTO.TABLE_KEY);
            pstmt.setInt(3, codecId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                codec.populateDTO(rs);
            }

            return codec;
        } catch (SQLException ex) {
            throw new SQLException("Error getting codec: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the company info
     *
     * @param connection
     * @param companyId
     * @return
     * @throws SQLException
     */
    public CompanyDTO getCompany(Connection connection, int companyId) throws SQLException {
        CompanyDTO company = new CompanyDTO();

        if (companyId == 0) {
            return company;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, CompanyDTO.TABLE_NAME);
            pstmt.setString(2, CompanyDTO.TABLE_KEY);
            pstmt.setInt(3, companyId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                company.populateDTO(rs);
            }

            return company;
        } catch (SQLException ex) {
            throw new SQLException("Error getting company: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the country info
     *
     * @param connection
     * @param countryId
     * @return
     * @throws SQLException
     */
    public CountryDTO getCountry(Connection connection, int countryId) throws SQLException {
        CountryDTO country = new CountryDTO();

        if (countryId == 0) {
            return country;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, CountryDTO.TABLE_NAME);
            pstmt.setString(2, CountryDTO.TABLE_KEY);
            pstmt.setInt(3, countryId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                country.populateDTO(rs);
            }

            return country;
        } catch (SQLException ex) {
            throw new SQLException("Error getting country: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the genre info
     *
     * @param connection
     * @param genreId
     * @return
     * @throws SQLException
     */
    public GenreDTO getGenre(Connection connection, int genreId) throws SQLException {
        GenreDTO genre = new GenreDTO();

        if (genreId == 0) {
            return genre;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, GenreDTO.TABLE_NAME);
            pstmt.setString(2, GenreDTO.TABLE_KEY);
            pstmt.setInt(3, genreId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                genre.populateDTO(rs);
            }

            return genre;
        } catch (SQLException ex) {
            throw new SQLException("Error getting genre: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the language info
     *
     * @param connection
     * @param languageId
     * @return
     * @throws SQLException
     */
    public LanguageDTO getLanguage(Connection connection, int languageId) throws SQLException {
        LanguageDTO language = new LanguageDTO();

        if (languageId == 0) {
            return language;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, LanguageDTO.TABLE_NAME);
            pstmt.setString(2, LanguageDTO.TABLE_KEY);
            pstmt.setInt(3, languageId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                language.populateDTO(rs);
            }

            return language;
        } catch (SQLException ex) {
            throw new SQLException("Error getting language: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the person info
     *
     * @param connection
     * @param personId
     * @return
     * @throws SQLException
     */
    public PersonDTO getPerson(Connection connection, int personId) throws SQLException {
        PersonDTO person = new PersonDTO();

        if (personId == 0) {
            return person;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, PersonDTO.TABLE_NAME);
            pstmt.setString(2, PersonDTO.TABLE_KEY);
            pstmt.setInt(3, personId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                person.populateDTO(rs);
            }

            return person;
        } catch (SQLException ex) {
            throw new SQLException("Error getting person: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the video info
     *
     * @param connection
     * @param videoId
     * @return
     * @throws SQLException
     */
    public VideoDTO getVideo(Connection connection, int videoId) throws SQLException {
        VideoDTO video = new VideoDTO();

        if (videoId == 0) {
            return video;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, VideoDTO.TABLE_NAME);
            pstmt.setString(2, VideoDTO.TABLE_KEY);
            pstmt.setInt(3, videoId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                video.populateDTO(rs);
            }

            return video;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the video file info
     *
     * @param connection
     * @param videoFileId
     * @return
     * @throws SQLException
     */
    public VideoFileDTO getVideoFile(Connection connection, int videoFileId) throws SQLException {
        VideoFileDTO videoFile = new VideoFileDTO();

        if (videoFileId == 0) {
            return videoFile;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, VideoFileDTO.TABLE_NAME);
            pstmt.setString(2, VideoFileDTO.TABLE_KEY);
            pstmt.setInt(3, videoFileId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                videoFile.populateDTO(rs);
            }

            return videoFile;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video file: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the video files info
     *
     * @param connection
     * @param videoId
     * @return
     * @throws SQLException
     */
    public Collection<VideoFileDTO> getVideoFiles(Connection connection, int videoId) throws SQLException {
        Collection<VideoFileDTO> videoFiles = new ArrayList<VideoFileDTO>();
        VideoFileDTO vf;

        if (videoId == 0) {
            return videoFiles;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            vf = new VideoFileDTO();

            pstmt = connection.prepareStatement(SELECT_VIDEO_ID_TABLE);
            pstmt.setString(1, VideoFileDTO.TABLE_NAME);
            pstmt.setString(2, SQL_ID);
            pstmt.setInt(2, videoId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            while (rs.next()) {
                vf.populateDTO(rs);
                if (vf.getId() > 0) {
                    videoFiles.add(vf);
                }
            }

            return videoFiles;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video files: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * get the video file part info
     *
     * @param connection
     * @param videoFilePartId
     * @return
     * @throws SQLException
     */
    public VideoFilePartDTO getVideoFilePart(Connection connection, int videoFilePartId) throws SQLException {
        VideoFilePartDTO videoPartFile = new VideoFilePartDTO();

        if (videoFilePartId == 0) {
            return videoPartFile;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_ALL_TABLE);
            pstmt.setString(1, VideoFilePartDTO.TABLE_NAME);
            pstmt.setString(2, VideoFilePartDTO.TABLE_KEY);
            pstmt.setInt(3, videoFilePartId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                videoPartFile.populateDTO(rs);
            }

            return videoPartFile;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video file part: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the video file parts info
     *
     * @param connection
     * @param videoId
     * @return
     * @throws SQLException
     */
    public Collection<VideoFilePartDTO> getVideoFileParts(Connection connection, int videoId) throws SQLException {
        Collection<VideoFilePartDTO> videoFileParts = new ArrayList<VideoFilePartDTO>();
        VideoFilePartDTO vf;

        if (videoId == 0) {
            return videoFileParts;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            vf = new VideoFilePartDTO();

            pstmt = connection.prepareStatement(SELECT_VIDEO_ID_TABLE);
            pstmt.setString(1, VideoFilePartDTO.TABLE_NAME);
            pstmt.setInt(2, videoId);

            pstmt.execute();
            rs = pstmt.getResultSet();

            while (rs.next()) {
                vf.populateDTO(rs);
                if (vf.getId() > 0) {
                    videoFileParts.add(vf);
                }
            }

            return videoFileParts;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video file parts: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Get the video site info
     *
     * @param connection
     * @param videoId
     * @param site
     * @return
     * @throws SQLException
     */
    public VideoSiteDTO getVideoSite(Connection connection, int videoId, String site) throws SQLException {
        VideoSiteDTO videoSite = new VideoSiteDTO();

        if (videoId == 0 || StringUtils.isBlank(site)) {
            return videoSite;
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = connection.prepareStatement(SELECT_VIDEO_SITE);
            pstmt.setString(1, VideoSiteDTO.TABLE_NAME);
            pstmt.setString(2, VideoSiteDTO.TABLE_KEY);
            pstmt.setInt(3, videoId);
            pstmt.setString(4, site);

            pstmt.execute();
            rs = pstmt.getResultSet();

            if (rs.next()) {
                videoSite.populateDTO(rs);
            }

            return videoSite;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video site: " + ex.getMessage(), ex);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }
}
