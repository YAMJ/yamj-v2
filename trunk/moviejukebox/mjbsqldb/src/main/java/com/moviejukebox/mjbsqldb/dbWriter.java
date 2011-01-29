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
import java.sql.PreparedStatement;

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

public class dbWriter {
    // Join tables
    private static final String JOIN_GENRE    = "insert into VIDEO_GENRE    (VIDEO_ID, GENRE_ID)    values (?, ?)";
    private static final String JOIN_COMPANY  = "insert into VIDEO_COMPANY  (VIDEO_ID, COMPANY_ID)  values (?, ?)";
    private static final String JOIN_COUNTRY  = "insert into VIDEO_COUNTRY  (VIDEO_ID, COUNTRY_ID)  values (?, ?)";
    private static final String JOIN_LANGUAGE = "insert into VIDEO_LANGUAGE (VIDEO_ID, LANGUAGE_ID) values (?, ?)";
    private static final String JOIN_PERSON   = "insert into VIDEO_PERSON   (VIDEO_ID, PERSON_ID)   values (?, ?)";

    public static int deleteIdFromTable(Connection connection, String tableName, String columnName, int tableId) {
        return deleteIdFromTable(connection, tableName, columnName, tableId, null, null);
    }
    
    public static int deleteIdFromTable(Connection connection, String tableName, String columnName1, int tableId1, String columnName2, String tableId2) {
        PreparedStatement pstmt = null;
        try {
            if (columnName2 == null) {
                pstmt = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName1 + "=" + tableId1);
            } else {
                pstmt = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + columnName1 + "=" + tableId1 + " AND " + columnName2 + "=" + tableId2);
            }
            pstmt.execute();
        } catch (Throwable tw) {
            throw new RuntimeException("Error deleting row from " + tableName + " table: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }

        return 0;
    }
    
    public static int insertArtwork(Connection connection, ArtworkDTO artworkDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (artworkDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getArtworkId(connection, artworkDTO.getFilename());
            if (id == 0) {
                // Create a new ID
                artworkDTO.setId(dbReader.getNextArtworkId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(ArtworkDTO.INSERT_TABLE);
            pstmt.setInt   (1, artworkDTO.getId());
            pstmt.setString(2, artworkDTO.getFilename());
            pstmt.setString(3, artworkDTO.getUrl());
            pstmt.setString(4, artworkDTO.getType());
            pstmt.setInt   (5, artworkDTO.getRelatedId());
            pstmt.setString(6, artworkDTO.getForeignKey());
            pstmt.executeUpdate();
            
            return artworkDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Artwork:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertCertification(Connection connection, CertificationDTO certificationDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (certificationDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getCertificationId(connection, certificationDTO.getCertification());
            if (id == 0) {
                // Create a new ID
                certificationDTO.setId(dbReader.getNextCertificationId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CertificationDTO.INSERT_TABLE);
            pstmt.setInt   (1, certificationDTO.getId());
            pstmt.setString(2, certificationDTO.getCertification());
            pstmt.executeUpdate();
            connection.commit();
            
            return certificationDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Certification:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertCodec(Connection connection, CodecDTO codecDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (codecDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getCodecId(connection, codecDTO.getCodec());
            if (id == 0) {
                // Create a new ID
                codecDTO.setId(dbReader.getNextCodecId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CodecDTO.INSERT_TABLE);
            pstmt.setInt   (1, codecDTO.getId());
            pstmt.setString(2, codecDTO.getCodec());
            pstmt.setString(3, codecDTO.getType());
            pstmt.executeUpdate();
            connection.commit();
            
            return codecDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Codec " + codecDTO.toString() + ":  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertCompany(Connection connection, CompanyDTO companyDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (companyDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getCompanyId(connection, companyDTO.getCompany());
            if (id == 0) {
                // Create a new ID
                companyDTO.setId(dbReader.getNextCompanyId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CompanyDTO.INSERT_TABLE);
            pstmt.setInt   (1, companyDTO.getId());
            pstmt.setString(2, companyDTO.getCompany());
            pstmt.setString(3, companyDTO.getUrl());
            pstmt.executeUpdate();
            
            return companyDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Company:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }

    public static int insertCountry(Connection connection, CountryDTO countryDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (countryDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getCountryId(connection, countryDTO.getCountry());
            if (id == 0) {
                // Create a new ID
                countryDTO.setId(dbReader.getNextCountryId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(CountryDTO.INSERT_TABLE);
            pstmt.setInt   (1, countryDTO.getId());
            pstmt.setString(2, countryDTO.getCountry());
            pstmt.setString(3, countryDTO.getUrl());
            pstmt.executeUpdate();
            
            return countryDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Country:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertGenre(Connection connection, GenreDTO genreDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (genreDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getGenreId(connection, genreDTO.getName());
            if (id == 0) {
                // Create a new ID
                genreDTO.setId(dbReader.getNextGenreId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(GenreDTO.INSERT_TABLE);
            pstmt.setInt   (1, genreDTO.getId());
            pstmt.setString(2, genreDTO.getName());
            pstmt.setString(3, genreDTO.getForeignKey());
            pstmt.executeUpdate();
            
            return genreDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Genre:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertLanguage(Connection connection, LanguageDTO languageDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (languageDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getLanguageId(connection, languageDTO.getLanguage());
            if (id == 0) {
                // Create a new ID
                languageDTO.setId(dbReader.getNextLanguageId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(LanguageDTO.INSERT_TABLE);
            pstmt.setInt   (1, languageDTO.getId());
            pstmt.setString(2, languageDTO.getLanguage());
            pstmt.setString(3, languageDTO.getShortCode());
            pstmt.setString(4, languageDTO.getMediumCode());
            pstmt.setString(5, languageDTO.getLongCode());
            pstmt.executeUpdate();
            
            return languageDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Language:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertPerson(Connection connection, PersonDTO personDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (personDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getPersonId(connection, personDTO.getName(), personDTO.getJob());
            if (id == 0) {
                // Create a new ID
                personDTO.setId(dbReader.getNextPersonId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(PersonDTO.INSERT_TABLE);
            pstmt.setInt    (1, personDTO.getId());
            pstmt.setString (2, personDTO.getName());
            pstmt.setString (3, personDTO.getJob());
            pstmt.setString (4, personDTO.getForeignKey());
            pstmt.setString (5, personDTO.getUrl());
            pstmt.setString (6, personDTO.getBiography());
            pstmt.setString (7, personDTO.getBirthday());
            pstmt.executeUpdate();
            
            return personDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Person:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertVideo(Connection connection, VideoDTO videoDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (videoDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getVideoId(connection, videoDTO.getTitle());
            if (id == 0) {
                // Create a new ID
                videoDTO.setId(dbReader.getNextVideoId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoDTO.INSERT_TABLE);
            
            pstmt.setInt    ( 1, videoDTO.getId());
            pstmt.setString ( 2, videoDTO.getMjbVersion());
            pstmt.setInt    ( 3, videoDTO.getMjbRevision());
            pstmt.setObject ( 4, videoDTO.getMjbUpdateDate());
            pstmt.setString ( 5, videoDTO.getBaseFilename());
            pstmt.setString ( 6, videoDTO.getTitle());
            pstmt.setString ( 7, videoDTO.getTitleSort());
            pstmt.setString ( 8, videoDTO.getTitleOriginal());
            pstmt.setObject ( 9, videoDTO.getReleaseDate());
            pstmt.setInt    (10, videoDTO.getRating());
            pstmt.setInt    (11, videoDTO.getTop250());
            pstmt.setString (12, videoDTO.getPlot());
            pstmt.setString (13, videoDTO.getOutline());
            pstmt.setString (14, videoDTO.getQuote());
            pstmt.setString (15, videoDTO.getTagline());
            pstmt.setInt    (16, videoDTO.getRuntime());
            pstmt.setString (17, videoDTO.getVideoType());
            pstmt.setInt    (18, videoDTO.getSeason());
            pstmt.setString (19, videoDTO.getSubtitles());
            pstmt.setString (20, videoDTO.getLibraryDescription());
            pstmt.setInt    (21, videoDTO.getCertificationId());
            pstmt.executeUpdate();
            
            return videoDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Video:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertVideoFile(Connection connection, VideoFileDTO videoFileDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (videoFileDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getVideoFileId(connection, videoFileDTO.getFileLocation());
            if (id == 0) {
                // Create a new ID
                videoFileDTO.setId(dbReader.getNextVideoFileId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoFileDTO.INSERT_TABLE);
            pstmt.setInt   ( 1, videoFileDTO.getId());
            pstmt.setInt   ( 2, videoFileDTO.getVideoId());
            pstmt.setString( 3, videoFileDTO.getFileLocation());
            pstmt.setString( 4, videoFileDTO.getFileUrl());
            pstmt.setString( 5, videoFileDTO.getContainer());
            pstmt.setInt   ( 6, videoFileDTO.getAudioChannels());
            pstmt.setInt   ( 7, videoFileDTO.getVideoCodecId());
            pstmt.setInt   ( 8, videoFileDTO.getAudioCodecId());
            pstmt.setString( 9, videoFileDTO.getResolution());
            pstmt.setString(10, videoFileDTO.getVideoSource());
            pstmt.setString(11, videoFileDTO.getVideoOutput());
            pstmt.setString(12, videoFileDTO.getAspect());
            pstmt.setFloat (13, videoFileDTO.getFps());
            pstmt.setObject(14, videoFileDTO.getFileDate());
            pstmt.setLong  (15, videoFileDTO.getFileSize());
            pstmt.setInt   (16, videoFileDTO.getNumberParts());
            pstmt.setInt   (17, videoFileDTO.getFirstPart());
            pstmt.setInt   (18, videoFileDTO.getLastPart());
            pstmt.executeUpdate();
            
            return videoFileDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Video File:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static int insertVideoFilePart(Connection connection, VideoFilePartDTO videoFilePartDTO) throws Throwable {
        // See if we have a value (non-zero) ID
        if (videoFilePartDTO.getId() == 0) {
            // See if the ID exists already
            int id = dbReader.getVideoFilePartId(connection, videoFilePartDTO.getFileId(), videoFilePartDTO.getPart());
            if (id == 0) {
                // Create a new ID
                videoFilePartDTO.setId(dbReader.getNextVideoFilePartId(connection));
            } else {
                // Return the existing ID
                return id;
            }
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(VideoFilePartDTO.INSERT_TABLE);
            pstmt.setInt   (1, videoFilePartDTO.getId());
            pstmt.setInt   (2, videoFilePartDTO.getFileId());
            pstmt.setInt   (3, videoFilePartDTO.getPart());
            pstmt.setString(4, videoFilePartDTO.getTitle());
            pstmt.setString(5, videoFilePartDTO.getPlot());
            pstmt.setInt   (6, videoFilePartDTO.getSeason());
            pstmt.executeUpdate();
            
            return videoFilePartDTO.getId();
        } catch (Throwable tw) {
            throw new RuntimeException("Error inserting Video File Part:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }
    
    public static String insertVideoSite(Connection connection, VideoSiteDTO videoSiteDTO) throws Throwable {
        if (videoSiteDTO.getVideoId() == 0) {
            return "";
        }
        
        String siteID = dbReader.getVideoSiteId(connection, videoSiteDTO.getVideoId(), videoSiteDTO.getSite()); 
        if (siteID.equals("")) {
            PreparedStatement pstmt = null;
            try {
                pstmt = connection.prepareStatement(VideoSiteDTO.INSERT_TABLE);
                pstmt.setInt   (1, videoSiteDTO.getVideoId());
                pstmt.setString(2, videoSiteDTO.getSite());
                pstmt.setString(3, videoSiteDTO.getSiteId());
                pstmt.executeUpdate();
                
                return videoSiteDTO.getSiteId();
            } catch (Throwable tw) {
                throw new RuntimeException("Error inserting Video Site: " + tw.getMessage(), tw);
            } finally {
                SQLTools.close(pstmt);
            }
        } else {
            return siteID;
        }
    }
    
    public static void joinCompany(Connection connection, int videoId, int companyId) {
        joinTable(connection, JOIN_COMPANY, videoId, companyId);
    }
    
    public static void joinCountry(Connection connection, int videoId, int countryId) {
        joinTable(connection, JOIN_COUNTRY, videoId, countryId);
    }
    
    public static void joinGenre(Connection connection, int videoId, int genreId) {
        joinTable(connection, JOIN_GENRE, videoId, genreId);
    }
    
    public static void joinLanguage(Connection connection, int videoId, int languageId) {
        joinTable(connection, JOIN_LANGUAGE, videoId, languageId);
    }

    public static void joinPerson(Connection connection, int videoId, int personId) {
        joinTable(connection, JOIN_PERSON, videoId, personId);
    }
    
    /**
     * Generic method to update a join table
     * @param connection
     * @param insertCommand
     * @param videoId
     * @param joinId
     */
    private static void joinTable(Connection connection, String insertCommand, int videoId, int joinId) {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(insertCommand);
            pstmt.setInt(1, videoId);
            pstmt.setInt(2, joinId);
            pstmt.executeUpdate();
        } catch (Throwable tw) {
            throw new RuntimeException("Error writing to join table:  " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(pstmt);
        }
    }

    /**
     * Update the artwork table
     * @param connection
     * @param artwork
     * @return
     * @throws Throwable
     */
    public static int updateArtwork (Connection connection, ArtworkDTO artwork) throws Throwable {
        try {
            // Check for the ID. If > 0 then we need to delete first
            if (artwork.getId() > 0) {
                deleteIdFromTable(connection, "artwork", "id", artwork.getId());
            }
        
            insertArtwork(connection, artwork);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating artwork table: " + tw.getMessage(), tw);
        }
        return artwork.getId();
    }

    public static int updateCertification(Connection connection, CertificationDTO certification) throws Throwable {
        try {
            if (certification.getId() > 0) {
                deleteIdFromTable(connection, "certification", "id", certification.getId());
            }
            
            insertCertification(connection, certification);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating certification table: " + tw.getMessage(), tw);
        }
        return certification.getId();
    }

    public static int updateCodec(Connection connection, CodecDTO codec) throws Throwable {
        try {
            if (codec.getId() > 0) {
                deleteIdFromTable(connection, "codec", "id", codec.getId());
            }
            
            insertCodec(connection, codec);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating codec table: " + tw.getMessage(), tw);
        }
        return codec.getId();
    }
    
    public static int updateCompany(Connection connection, CompanyDTO company) throws Throwable {
        try {
            if (company.getId() > 0) {
                deleteIdFromTable(connection, "company", "id", company.getId());
            }

            insertCompany(connection, company);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating company table: " + tw.getMessage(), tw);
        }
        return company.getId();
    }

    public static int updateCountry(Connection connection, CountryDTO country) throws Throwable {
        try {
            if (country.getId() > 0) {
                deleteIdFromTable(connection, "country", "id", country.getId());
            }

            insertCountry(connection, country);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating country table: " + tw.getMessage(), tw);
        }
        return country.getId();
    }

    public static int updateGenre(Connection connection, GenreDTO genre) throws Throwable {
        try {
            if (genre.getId() > 0) {
                deleteIdFromTable(connection, "genre", "id", genre.getId());
            }

            insertGenre(connection, genre);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating genre table: " + tw.getMessage(), tw);
        }
        return genre.getId();
    }

    public static int updateLanguage(Connection connection, LanguageDTO language) throws Throwable {
        try {
            if (language.getId() > 0) {
                deleteIdFromTable(connection, "language", "id", language.getId());
            }

            insertLanguage(connection, language);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating language table: " + tw.getMessage(), tw);
        }
        return language.getId();
    }

    public static int updatePerson(Connection connection, PersonDTO person) throws Throwable {
        try {
            if (person.getId() > 0) {
                deleteIdFromTable(connection, "person", "id", person.getId());
            }

            insertPerson(connection, person);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating person table: " + tw.getMessage(), tw);
        }
        return person.getId();
    }

    public static int updateVideo(Connection connection, VideoDTO video) throws Throwable {
        try {
            if (video.getId() > 0) {
                deleteIdFromTable(connection, "video", "id", video.getId());
            }

            insertVideo(connection, video);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating video table: " + tw.getMessage(), tw);
        }
        return video.getId();
    }

    public static int updateVideoFile(Connection connection, VideoFileDTO videoFile) throws Throwable {
        try {
            if (videoFile.getId() > 0) {
                deleteIdFromTable(connection, "video_file", "id", videoFile.getId());
            }

            insertVideoFile(connection, videoFile);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating Video File table: " + tw.getMessage(), tw);
        }
        return videoFile.getId();
    }

    public static int updateVideoFilePart(Connection connection, VideoFilePartDTO videoFilePart) throws Throwable {
        try {
            if (videoFilePart.getId() > 0) {
                deleteIdFromTable(connection, "video_file_part", "id", videoFilePart.getId());
            }

            insertVideoFilePart(connection, videoFilePart);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating artwork table: " + tw.getMessage(), tw);
        }
        return videoFilePart.getId();
    }

    public static int updateVideoSite(Connection connection, VideoSiteDTO videoSite) throws Throwable {
        try {
            try {
                deleteIdFromTable(connection, "video_site", "video_id", videoSite.getVideoId(), "site", videoSite.getSite());
            } catch (Exception ignore) {
                // Just delete it. We don't care.
            }
            insertVideoSite(connection, videoSite);
            
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error updating Video Site table: " + tw.getMessage(), tw);
        }
        return videoSite.getVideoId();
    }
    
}