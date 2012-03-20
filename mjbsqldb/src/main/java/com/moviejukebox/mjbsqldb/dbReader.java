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
    public static synchronized int getNextId(Connection connection, String tableName, String idColumnName) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Error: No connection specified!");
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting the ID for " + tableName + ": " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public static int getNextArtworkId(Connection connection) throws Throwable {
        return getNextId(connection, ArtworkDTO.TABLE_NAME, ArtworkDTO.TABLE_KEY);
    }

    public static int getNextGenreId(Connection connection) throws Throwable {
        return getNextId(connection, GenreDTO.TABLE_NAME, GenreDTO.TABLE_KEY);
    }

    public static int getNextPersonId(Connection connection) throws Throwable {
        return getNextId(connection, PersonDTO.TABLE_NAME, PersonDTO.TABLE_KEY);
    }

    public static int getNextVideoId(Connection connection) throws Throwable {
        return getNextId(connection, VideoDTO.TABLE_NAME, VideoDTO.TABLE_KEY);
    }

    public static int getNextCertificationId(Connection connection) throws Throwable {
        return getNextId(connection, CertificationDTO.TABLE_NAME, CertificationDTO.TABLE_KEY);
    }

    public static int getNextCodecId(Connection connection) throws Throwable {
        return getNextId(connection, CodecDTO.TABLE_NAME, CodecDTO.TABLE_KEY);
    }

    public static int getNextCompanyId(Connection connection) throws Throwable {
        return getNextId(connection, CompanyDTO.TABLE_NAME, CompanyDTO.TABLE_KEY);
    }

    public static int getNextCountryId(Connection connection) throws Throwable {
        return getNextId(connection, CountryDTO.TABLE_NAME, CountryDTO.TABLE_KEY);
    }

    public static int getNextLanguageId(Connection connection) throws Throwable {
        return getNextId(connection, LanguageDTO.TABLE_NAME, LanguageDTO.TABLE_KEY);
    }

    public static int getNextVideoFileId(Connection connection) throws Throwable {
        return getNextId(connection, VideoFileDTO.TABLE_NAME, VideoFileDTO.TABLE_KEY);
    }

    public static int getNextVideoFilePartId(Connection connection) throws Throwable {
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
    public static int getArtworkId(Connection connection, String filename) throws Throwable {
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
    public static int getCertificationId(Connection connection, String certification) throws Throwable {
        return getTableId(connection, CertificationDTO.TABLE_NAME, "CERTIFICATION", certification);
    }

    public static int getCodecId(Connection connection, String codec) throws Throwable {
        return getTableId(connection, CodecDTO.TABLE_NAME, "CODEC", codec);
    }

    public static String getVideoSiteId(Connection connection, int videoId, String site) {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting " + VideoSiteDTO.TABLE_NAME + " ID: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }

    }

    public static int getCompanyId(Connection connection, String company) throws Throwable {
        return getTableId(connection, CompanyDTO.TABLE_NAME, "COMPANY", company);
    }

    public static int getCountryId(Connection connection, String country) throws Throwable {
        return getTableId(connection, CountryDTO.TABLE_NAME, "COUNTRY", country);
    }

    public static int getGenreId(Connection connection, String name) throws Throwable {
        return getTableId(connection, GenreDTO.TABLE_NAME, "NAME", name);
    }

    public static int getLanguageId(Connection connection, String language) throws Throwable {
        return getTableId(connection, LanguageDTO.TABLE_NAME, "LANGUAGE", language);
    }

    public static int getPersonId(Connection connection, String name, String job) throws Throwable {
        return getTableId(connection, PersonDTO.TABLE_NAME, "NAME", name, "JOB", job);
    }

    private static int getTableId(Connection connection, String tableName, String columnName1, String searchTerm1, String columnName2, String searchTerm2) {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting " + tableName + " ID: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    private static int getTableId(Connection connection, String tableName, String columnName, String searchTerm) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting " + tableName + " ID: " + tw.getMessage(), tw);
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
    public static int getVideoFileId(Connection connection, String fileLocation) throws Throwable {
        return getTableId(connection, VideoFileDTO.TABLE_NAME, "FILE_LOCATION", fileLocation);
    }

    public static int getVideoFilePartId(Connection connection, int fileId, int part) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting " + VideoFilePartDTO.TABLE_NAME + " ID: " + tw.getMessage(), tw);
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
    public static int getVideoId(Connection connection, String title) throws Throwable {
        return getTableId(connection, VideoDTO.TABLE_NAME, "TITLE", title);
    }

    public static boolean isVideoExists(Connection connection, String title) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error checking for Video: " + title + ". " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public ArtworkDTO getArtwork(Connection connection, int artworkId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting artwork: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CertificationDTO getCertification(Connection connection, int certId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting certification: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CodecDTO getCodec(Connection connection, int codecId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting codec: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CompanyDTO getCompany(Connection connection, int companyId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting company: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public CountryDTO getCountry(Connection connection, int countryId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting country: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public GenreDTO getGenre(Connection connection, int genreId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting genre: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public LanguageDTO getLanguage(Connection connection, int languageId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting language: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public PersonDTO getPerson(Connection connection, int personId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting person: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoDTO getVideo(Connection connection, int videoId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoFileDTO getVideoFile(Connection connection, int videoFileId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video file: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public Collection<VideoFileDTO> getVideoFiles(Connection connection, int videoId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video files: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoFilePartDTO getVideoFilePart(Connection connection, int videoFilePartId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video file part: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public Collection<VideoFilePartDTO> getVideoFileParts(Connection connection, int videoId) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video file parts: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    public VideoSiteDTO getVideoSite(Connection connection, int videoId, String site) throws Throwable {
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
        } catch (Throwable tw) {
            throw new RuntimeException("Error getting video site: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }
}
