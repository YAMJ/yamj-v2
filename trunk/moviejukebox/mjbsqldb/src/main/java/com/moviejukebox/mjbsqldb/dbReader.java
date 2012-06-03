/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.mjbsqldb;

import com.moviejukebox.mjbsqldb.dto.*;
import com.moviejukebox.mjbsqldb.tools.SQLTools;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

public class dbReader {

    /**
     * Get the last used ID for the table
     *
     * @param tableName
     * @param idColumnName
     * @return
     * @throws Throwable
     */
    public static synchronized int getNextId(Connection connection, String tableName, String idColumnName) throws SQLException {
        if (connection == null) {
            throw new SQLException("Error: No connection specified!");
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("select MAX(");
            sqlText.append(idColumnName);
            sqlText.append(") from ");
            sqlText.append(tableName);

            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                // update the id by 1 to create the next id
                return (rs.getInt(1) + 1);
            }
            return 1;
        } catch (SQLException ex) {
            throw new SQLException("Error getting the ID for " + tableName + ": " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public static int getNextArtworkId(Connection connection) throws SQLException {
        return getNextId(connection, ArtworkDTO.TABLE_NAME, ArtworkDTO.TABLE_KEY);
    }

    public static int getNextGenreId(Connection connection) throws SQLException {
        return getNextId(connection, GenreDTO.TABLE_NAME, GenreDTO.TABLE_KEY);
    }

    public static int getNextPersonId(Connection connection) throws SQLException {
        return getNextId(connection, PersonDTO.TABLE_NAME, PersonDTO.TABLE_KEY);
    }

    public static int getNextVideoId(Connection connection) throws SQLException {
        return getNextId(connection, VideoDTO.TABLE_NAME, VideoDTO.TABLE_KEY);
    }

    public static int getNextCertificationId(Connection connection) throws SQLException {
        return getNextId(connection, CertificationDTO.TABLE_NAME, CertificationDTO.TABLE_KEY);
    }

    public static int getNextCodecId(Connection connection) throws SQLException {
        return getNextId(connection, CodecDTO.TABLE_NAME, CodecDTO.TABLE_KEY);
    }

    public static int getNextCompanyId(Connection connection) throws SQLException {
        return getNextId(connection, CompanyDTO.TABLE_NAME, CompanyDTO.TABLE_KEY);
    }

    public static int getNextCountryId(Connection connection) throws SQLException {
        return getNextId(connection, CountryDTO.TABLE_NAME, CountryDTO.TABLE_KEY);
    }

    public static int getNextLanguageId(Connection connection) throws SQLException {
        return getNextId(connection, LanguageDTO.TABLE_NAME, LanguageDTO.TABLE_KEY);
    }

    public static int getNextVideoFileId(Connection connection) throws SQLException {
        return getNextId(connection, VideoFileDTO.TABLE_NAME, VideoFileDTO.TABLE_KEY);
    }

    public static int getNextVideoFilePartId(Connection connection) throws SQLException {
        return getNextId(connection, VideoFilePartDTO.TABLE_NAME, VideoFilePartDTO.TABLE_KEY);
    }

    /**
     * Get any existing Artwork ID with the filename
     *
     * @param connection
     * @param filename
     * @return
     * @throws Throwable
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
     * @throws Throwable
     */
    public static int getCertificationId(Connection connection, String certification) throws SQLException {
        return getTableId(connection, CertificationDTO.TABLE_NAME, "CERTIFICATION", certification);
    }

    public static int getCodecId(Connection connection, String codec) throws SQLException {
        return getTableId(connection, CodecDTO.TABLE_NAME, "CODEC", codec);
    }

    public static String getVideoSiteId(Connection connection, int videoId, String site) throws SQLException {
        if (StringUtils.isBlank(site) || videoId < 1) {
            return "";
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("SELECT SITE_ID FROM ");
            sqlText.append(VideoSiteDTO.TABLE_NAME);
            sqlText.append(" WHERE VIDEO_ID=");
            sqlText.append(videoId);
            sqlText.append(" AND SITE='");
            sqlText.append(site);
            sqlText.append("'");

            rs = stmt.executeQuery(sqlText.toString());
            if (rs.next()) {
                return rs.getString(1);
            }
            return "";
        } catch (SQLException ex) {
            throw new SQLException("Error getting " + VideoSiteDTO.TABLE_NAME + " ID: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }

    }

    public static int getCompanyId(Connection connection, String company) throws SQLException {
        return getTableId(connection, CompanyDTO.TABLE_NAME, "COMPANY", company);
    }

    public static int getCountryId(Connection connection, String country) throws SQLException {
        return getTableId(connection, CountryDTO.TABLE_NAME, "COUNTRY", country);
    }

    public static int getGenreId(Connection connection, String name) throws SQLException {
        return getTableId(connection, GenreDTO.TABLE_NAME, "NAME", name);
    }

    public static int getLanguageId(Connection connection, String language) throws SQLException {
        return getTableId(connection, LanguageDTO.TABLE_NAME, "LANGUAGE", language);
    }

    public static int getPersonId(Connection connection, String name, String job)throws SQLException {
        return getTableId(connection, PersonDTO.TABLE_NAME, "NAME", name, "JOB", job);
    }

    private static int getTableId(Connection connection, String tableName, String columnName1, String searchTerm1, String columnName2, String searchTerm2) throws SQLException {
        if (StringUtils.isBlank(searchTerm1) || StringUtils.isBlank(searchTerm2)) {
            return 0;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("SELECT ID FROM ");
            sqlText.append(tableName);
            sqlText.append(" WHERE ");
            sqlText.append(columnName1);
            sqlText.append("='");
            sqlText.append(searchTerm1);
            sqlText.append("' AND ");
            sqlText.append(columnName2);
            sqlText.append("='");
            sqlText.append(searchTerm2);
            sqlText.append("'");

            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException("Error getting " + tableName + " ID: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    private static int getTableId(Connection connection, String tableName, String columnName, String searchTerm) throws SQLException {
        if (StringUtils.isBlank(searchTerm)) {
            return 0;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("SELECT ID FROM ");
            sqlText.append(tableName);
            sqlText.append(" WHERE ");
            sqlText.append(columnName);
            sqlText.append("='");
            sqlText.append(searchTerm);
            sqlText.append("'");

            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException("Error getting " + tableName + " ID: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    /**
     * Get the VideoFile ID using the video fileLocation
     *
     * @param connection
     * @param fileLocation
     * @return
     * @throws Throwable
     */
    public static int getVideoFileId(Connection connection, String fileLocation) throws SQLException {
        return getTableId(connection, VideoFileDTO.TABLE_NAME, "FILE_LOCATION", fileLocation);
    }

    public static int getVideoFilePartId(Connection connection, int fileId, int part) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("SELECT ");
            sqlText.append(VideoFilePartDTO.TABLE_KEY);
            sqlText.append(" FROM ");
            sqlText.append(VideoFilePartDTO.TABLE_NAME);
            sqlText.append(" WHERE FILE_ID=");
            sqlText.append(fileId);
            sqlText.append(" AND PART=");
            sqlText.append(part);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new SQLException("Error getting " + VideoFilePartDTO.TABLE_NAME + " ID: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    /**
     * Get the Video ID using the title
     *
     * @param connection
     * @param title
     * @return
     * @throws Throwable
     */
    public static int getVideoId(Connection connection, String title) throws SQLException {
        return getTableId(connection, VideoDTO.TABLE_NAME, "TITLE", title);
    }

    public static boolean isVideoExists(Connection connection, String title) throws SQLException {
        if (StringUtils.isBlank(title)) {
            //throw new IllegalArgumentException ("Check video exists error because path is blank.");
            return false;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("select ");
            sqlText.append(VideoDTO.TABLE_KEY);
            sqlText.append(" from ");
            sqlText.append(VideoDTO.TABLE_NAME);
            sqlText.append(" where TITLE='");
            sqlText.append(title);
            sqlText.append("'");
            rs = stmt.executeQuery(sqlText.toString());


            return rs.next();
        } catch (SQLException ex) {
            throw new SQLException("Error checking for Video: " + title + ". " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public ArtworkDTO getArtwork(Connection connection, int artworkId) throws SQLException {
        ArtworkDTO artwork = new ArtworkDTO();

        if (artworkId == 0) {
            return artwork;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(ArtworkDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(ArtworkDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(artworkId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                artwork.populateDTO(rs);
            }

            return artwork;
        } catch (SQLException ex) {
            throw new SQLException("Error getting artwork: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CertificationDTO getCertification(Connection connection, int certId) throws SQLException {
        CertificationDTO cert = new CertificationDTO();

        if (certId == 0) {
            return cert;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(CertificationDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(CertificationDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(certId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                cert.populateDTO(rs);
            }

            return cert;
        } catch (SQLException ex) {
            throw new SQLException("Error getting certification: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CodecDTO getCodec(Connection connection, int codecId) throws SQLException {
        CodecDTO codec = new CodecDTO();

        if (codecId == 0) {
            return codec;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(CodecDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(CodecDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(codecId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                codec.populateDTO(rs);
            }

            return codec;
        } catch (SQLException ex) {
            throw new SQLException("Error getting codec: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CompanyDTO getCompany(Connection connection, int companyId) throws SQLException {
        CompanyDTO company = new CompanyDTO();

        if (companyId == 0) {
            return company;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(CompanyDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(CompanyDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(companyId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                company.populateDTO(rs);
            }

            return company;
        } catch (SQLException ex) {
            throw new SQLException("Error getting company: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CountryDTO getCountry(Connection connection, int countryId) throws SQLException {
        CountryDTO country = new CountryDTO();

        if (countryId == 0) {
            return country;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(CountryDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(CountryDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(countryId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                country.populateDTO(rs);
            }

            return country;
        } catch (SQLException ex) {
            throw new SQLException("Error getting country: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public GenreDTO getGenre(Connection connection, int genreId) throws SQLException {
        GenreDTO genre = new GenreDTO();

        if (genreId == 0) {
            return genre;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(GenreDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(GenreDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(genreId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                genre.populateDTO(rs);
            }

            return genre;
        } catch (SQLException ex) {
            throw new SQLException("Error getting genre: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public LanguageDTO getLanguage(Connection connection, int languageId) throws SQLException {
        LanguageDTO language = new LanguageDTO();

        if (languageId == 0) {
            return language;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(LanguageDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(LanguageDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(languageId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                language.populateDTO(rs);
            }

            return language;
        } catch (SQLException ex) {
            throw new SQLException("Error getting language: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public PersonDTO getPerson(Connection connection, int personId) throws SQLException {
        PersonDTO person = new PersonDTO();

        if (personId == 0) {
            return person;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(PersonDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(PersonDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(personId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                person.populateDTO(rs);
            }

            return person;
        } catch (SQLException ex) {
            throw new SQLException("Error getting person: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoDTO getVideo(Connection connection, int videoId) throws SQLException {
        VideoDTO video = new VideoDTO();

        if (videoId == 0) {
            return video;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(VideoDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(videoId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                video.populateDTO(rs);
            }

            return video;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoFileDTO getVideoFile(Connection connection, int videoFileId) throws SQLException {
        VideoFileDTO videoFile = new VideoFileDTO();

        if (videoFileId == 0) {
            return videoFile;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoFileDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(VideoFileDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(videoFileId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                videoFile.populateDTO(rs);
            }

            return videoFile;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video file: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public Collection<VideoFileDTO> getVideoFiles(Connection connection, int videoId) throws SQLException {
        Collection<VideoFileDTO> videoFiles = new ArrayList<VideoFileDTO>();
        VideoFileDTO vf;

        if (videoId == 0) {
            return videoFiles;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            vf = new VideoFileDTO();

            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoFileDTO.TABLE_NAME);
            sqlText.append(" WHERE VIDEO_ID=");
            sqlText.append(videoId);
            rs = stmt.executeQuery(sqlText.toString());

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
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoFilePartDTO getVideoFilePart(Connection connection, int videoFilePartId) throws SQLException {
        VideoFilePartDTO videoPartFile = new VideoFilePartDTO();

        if (videoFilePartId == 0) {
            return videoPartFile;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoFilePartDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(VideoFilePartDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(videoFilePartId);
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                videoPartFile.populateDTO(rs);
            }

            return videoPartFile;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video file part: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public Collection<VideoFilePartDTO> getVideoFileParts(Connection connection, int videoId) throws SQLException {
        Collection<VideoFilePartDTO> videoFileParts = new ArrayList<VideoFilePartDTO>();
        VideoFilePartDTO vf;

        if (videoId == 0) {
            return videoFileParts;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            vf = new VideoFilePartDTO();

            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoFilePartDTO.TABLE_NAME);
            sqlText.append(" WHERE VIDEO_ID=");
            sqlText.append(videoId);
            rs = stmt.executeQuery(sqlText.toString());

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
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoSiteDTO getVideoSite(Connection connection, int videoId, String site) throws SQLException {
        VideoSiteDTO videoSite = new VideoSiteDTO();

        if (videoId == 0 || StringUtils.isBlank(site)) {
            return videoSite;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            StringBuilder sqlText = new StringBuilder("SELECT * FROM ");
            sqlText.append(VideoSiteDTO.TABLE_NAME);
            sqlText.append(" WHERE ");
            sqlText.append(VideoSiteDTO.TABLE_KEY);
            sqlText.append("=");
            sqlText.append(videoId);
            sqlText.append("SITE='");
            sqlText.append(site);
            sqlText.append("'");
            rs = stmt.executeQuery(sqlText.toString());

            if (rs.next()) {
                videoSite.populateDTO(rs);
            }

            return videoSite;
        } catch (SQLException ex) {
            throw new SQLException("Error getting video site: " + ex.getMessage(), ex);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }
}
