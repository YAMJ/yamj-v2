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

package com.moviejukebox.mjbsqldb.tools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

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

/**
 * Class to set up the database and the tables.
 * @author stuart.boston
 *
 */
public class DatabaseTools {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Create the database at the end of the connection
     * The database file must already exist and be open
     * @throws Throwable
     */
    public static void createTables(Connection connection, float version) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Error: No connection specified!");
        }
        
        Statement stmt = null;
        try {
            String dbDate = dateFormat.format(new Date());
            stmt = connection.createStatement();
            
            stmt.addBatch(ArtworkDTO.CREATE_TABLE);
            stmt.addBatch(CertificationDTO.CREATE_TABLE);
            stmt.addBatch(CodecDTO.CREATE_TABLE);
            stmt.addBatch(CompanyDTO.CREATE_TABLE);
            stmt.addBatch(CountryDTO.CREATE_TABLE);
            stmt.addBatch(GenreDTO.CREATE_TABLE);
            stmt.addBatch(LanguageDTO.CREATE_TABLE);
            stmt.addBatch(PersonDTO.CREATE_TABLE);
            stmt.addBatch(VideoDTO.CREATE_TABLE);
            stmt.addBatch(VideoFileDTO.CREATE_TABLE);
            stmt.addBatch(VideoFilePartDTO.CREATE_TABLE);
            stmt.addBatch(VideoSiteDTO.CREATE_TABLE);
            
            // Create the join tables
            stmt.addBatch("CREATE TABLE IF NOT EXISTS VIDEO_GENRE    (VIDEO_ID INTEGER, GENRE_ID INTEGER)");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS VIDEO_COMPANY  (VIDEO_ID INTEGER, COMPANY_ID INTEGER)");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS VIDEO_COUNTRY  (VIDEO_ID INTEGER, COUNTRY_ID INTEGER)");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS VIDEO_LANGUAGE (VIDEO_ID INTEGER, LANGUAGE_ID INTEGER)");
            stmt.addBatch("CREATE TABLE IF NOT EXISTS VIDEO_PERSON   (VIDEO_ID INTEGER, PERSON_ID INTEGER)");

            // Create the database version table and insert this version information
            stmt.addBatch("CREATE TABLE IF NOT EXISTS DB_VERSION    (DB_VERSION FLOAT PRIMARY KEY, DB_DATE TEXT)");
            stmt.addBatch("DELETE FROM DB_VERSION");
            stmt.addBatch("INSERT INTO DB_VERSION VALUES(" + version + ", '" + dbDate + "')");
            
            stmt.executeBatch();
            connection.commit();
        } catch (Throwable tw) {
            throw new RuntimeException("Error creating database tables: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(stmt);
        }
    }

    /**
     * Remove all the tables from the database
     * The connection must be open
     * @throws Throwable
     */
    public static void deleteTables(Connection connection) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Error: No connection specified!");
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();

            stmt.addBatch("DROP TABLE IF EXISTS ARTWORK");
            stmt.addBatch("DROP TABLE IF EXISTS CERTIFICATION");
            stmt.addBatch("DROP TABLE IF EXISTS CODEC");
            stmt.addBatch("DROP TABLE IF EXISTS COMPANY");
            stmt.addBatch("DROP TABLE IF EXISTS COUNTRY");
            stmt.addBatch("DROP TABLE IF EXISTS GENRE");
            stmt.addBatch("DROP TABLE IF EXISTS LANGUAGE");
            stmt.addBatch("DROP TABLE IF EXISTS PERSON");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_FILE");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_FILE_PART");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_GENRE");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_COMPANY");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_COUNTRY");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_LANGUAGE");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_PERSON");
            stmt.addBatch("DROP TABLE IF EXISTS DB_VERSION");
            stmt.addBatch("DROP TABLE IF EXISTS VIDEO_SITE");

            stmt.executeBatch();
        } catch (Throwable tw) {
            throw new RuntimeException("Error deleting the tables: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }
    }

    /**
     * Get the database version
     * @return (float) database version
     * @throws Throwable
     */
    public static float getDatabaseVersion(Connection connection) {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select DB_VERSION from DB_VERSION");

            if (rs.next()) {
                return rs.getFloat("db_version");
            }
        } catch (Throwable tw) {
            throw new RuntimeException("Error: Unable to get database version: " + tw.getMessage(), tw);
        } finally {
            SQLTools.close(rs);
            SQLTools.close(stmt);
        }

        throw new RuntimeException("Error: Unable to get database version.");
    }
    
}
