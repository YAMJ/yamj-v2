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

package com.moviejukebox.mjbsqldb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

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
import com.moviejukebox.mjbsqldb.tools.SQLTools;

public class dbReader {
    
    /**
     * Get the last used ID for the table 
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
            rs = stmt.executeQuery("select MAX(" + idColumnName + ") from " + tableName);
    
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

            rs = stmt.executeQuery("SELECT SITE_ID FROM " + VideoSiteDTO.TABLE_NAME + " WHERE VIDEO_ID=" + videoId +
                            " AND SITE='" + StringEscapeUtils.escapeSql(site) + "'");

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

            rs = stmt.executeQuery("SELECT ID FROM " + tableName + " WHERE " + 
                    columnName1 + "='" + StringEscapeUtils.escapeSql(searchTerm1) + "' AND " + 
                    columnName2 + "='" + StringEscapeUtils.escapeSql(searchTerm2) + "'");

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

            rs = stmt.executeQuery("SELECT ID FROM " + tableName + " WHERE " + columnName + "='" + 
                    StringEscapeUtils.escapeSql(searchTerm) + "'");

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
     * Get the VideoFile ID using the video's fileLocation
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

            rs = stmt.executeQuery("SELECT " + VideoFilePartDTO.TABLE_KEY + " FROM " + VideoFilePartDTO.TABLE_NAME + " WHERE FILE_ID=" + fileId + " AND PART=" + part);

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
            rs = stmt.executeQuery("select " + VideoDTO.TABLE_KEY + " from " + VideoDTO.TABLE_NAME + " where TITLE='"
                + StringEscapeUtils.escapeSql(title) + "'");

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
            rs = stmt.executeQuery("SELECT * FROM " + ArtworkDTO.TABLE_NAME + " WHERE " + ArtworkDTO.TABLE_KEY + "=" + artworkId);
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
            rs = stmt.executeQuery("SELECT * FROM " + CertificationDTO.TABLE_NAME + " WHERE " + CertificationDTO.TABLE_KEY + "=" + certId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + CodecDTO.TABLE_NAME + " WHERE " + CodecDTO.TABLE_KEY + "=" + codecId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + CompanyDTO.TABLE_NAME + " WHERE " + CompanyDTO.TABLE_KEY + "=" + companyId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + CountryDTO.TABLE_NAME + " WHERE " + CountryDTO.TABLE_KEY + "=" + countryId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + GenreDTO.TABLE_NAME + " WHERE " + GenreDTO.TABLE_KEY + "=" + genreId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + LanguageDTO.TABLE_NAME + " WHERE " + LanguageDTO.TABLE_KEY + "=" + languageId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + PersonDTO.TABLE_NAME + " WHERE " + PersonDTO.TABLE_KEY + "=" + personId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoDTO.TABLE_NAME + " WHERE " + VideoDTO.TABLE_KEY + "=" + videoId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoFileDTO.TABLE_NAME + " WHERE " + VideoFileDTO.TABLE_KEY + "=" + videoFileId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoFileDTO.TABLE_NAME + " WHERE VIDEO_ID=" + videoId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoFilePartDTO.TABLE_NAME + " WHERE " + VideoFilePartDTO.TABLE_KEY + "=" + videoFilePartId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoFilePartDTO.TABLE_NAME + " WHERE VIDEO_ID=" + videoId);
            
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
            rs = stmt.executeQuery("SELECT * FROM " + VideoSiteDTO.TABLE_NAME + " WHERE " + VideoSiteDTO.TABLE_KEY + "=" + videoId + 
                            "SITE='" + StringEscapeUtils.escapeSql(site) + "'");
            
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
