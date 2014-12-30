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
package com.moviejukebox.mjbsqldb.tools;

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class to set up the database and the tables.
 *
 * @author stuart.boston
 *
 */
public final class DatabaseTools {

    private DatabaseTools() {
        throw new IllegalArgumentException("Class cannot be initialised!");
    }

    /**
     * Create the database at the end of the connection The database file must already exist and be open
     *
     * @param connection
     * @param version
     * @throws SQLException
     */
    public static void createTables(Connection connection, float version) throws SQLException {
        if (connection == null) {
            throw new SQLException("Error: No connection specified!");
        }

        Statement stmt = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dbDate = sdf.format(new Date());
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
        } catch (SQLException ex) {
            throw new SQLException("Error creating database tables: " + ex.getMessage(), ex);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Remove all the tables from the database The connection must be open
     *
     * @param connection
     * @throws SQLException
     */
    public static void deleteTables(Connection connection) throws SQLException {
        if (connection == null) {
            throw new SQLException("Error: No connection specified!");
        }

        Statement stmt = null;

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
        } catch (SQLException ex) {
            throw new SQLException("Error deleting the tables: " + ex.getMessage(), ex);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the database version
     *
     * @param connection
     * @return (float) database version
     * @throws SQLException
     */
    public static float getDatabaseVersion(Connection connection) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select DB_VERSION from DB_VERSION");

            if (rs.next()) {
                return rs.getFloat("db_version");
            }
        } catch (SQLException ex) {
            throw new SQLException("Error: Unable to get database version: " + ex.getMessage(), ex);
        } finally {
            if (stmt != null) {
                stmt.close();
            }

            if (rs != null) {
                rs.close();
            }
        }

        throw new SQLException("Error: Unable to get database version.");
    }
}
