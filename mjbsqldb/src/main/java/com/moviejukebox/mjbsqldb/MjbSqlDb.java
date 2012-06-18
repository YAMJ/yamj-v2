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

import com.moviejukebox.mjbsqldb.tools.DatabaseTools;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.sqlite.SQLiteJDBCLoader;

public class MjbSqlDb {

    private static final Logger LOGGER = Logger.getLogger(MjbSqlDb.class);
    private static final String LOG_MESSAGE = "MjbSqlDB: ";
    private static final float VERSION = 1.4f;
    private Connection connection = null;
    private boolean driverOk = initDriver();

    /**
     * Create or Open the database specified
     *
     * @param dbPath
     * @param dbName
     */
    public MjbSqlDb(String dbPath, String dbName) throws SQLException {
        if (!driverOk) {
            throw new SQLException("SQL Driver failed to initialise");
        }

        if (StringUtils.isBlank(dbPath) || StringUtils.isBlank(dbName)) {
            throw new SQLException("Path or database name is blank: ");
        }

        if (StringUtils.isBlank(dbPath)) {
            throw new IllegalArgumentException("Database path is blank");
        }

        String dbLocation = FilenameUtils.separatorsToUnix(dbPath + "/" + dbName);
        File dbFile = new File(dbLocation);

        try {
            if (!dbFile.exists()) {
                // Create the database file
                FileUtils.forceMkdir(dbFile.getParentFile());
                if (!dbFile.createNewFile()) {
                    throw new SQLException("Error opening the database");
                }
            }

            // Create the connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
            connection.setAutoCommit(false);

            if (VERSION > getDbVersion()) {
                LOGGER.info(LOG_MESSAGE + "Updating database structure...");
                // This is overkill, but OK for the time being
                DatabaseTools.deleteTables(connection);
            }
            // Create the tables (if they don't exist)
            DatabaseTools.createTables(connection, VERSION);

        } catch (IOException ex) {
            throw new SQLException("Error opening the database: " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            connection.close();
            throw new SQLException("Error opening the database: " + ex.getMessage(), ex);
        }
    }

    /**
     * Check the version of the database against the program version
     *
     * @return
     */
    private float getDbVersion() {
        try {
            return DatabaseTools.getDatabaseVersion(connection);
        } catch (SQLException error) {
            LOGGER.info(LOG_MESSAGE + "Database version out of date. Updating...");
            return 0.0f;
        }
    }

    /**
     * Set up the driver
     *
     * @throws SQLException
     */
    private boolean initDriver() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            LOGGER.info(LOG_MESSAGE + "Driver running in " + (SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java") + " mode");
            return Boolean.TRUE;
        } catch (ExceptionInInitializerError ex) {
            throw new SQLException("Error initializing the database driver: " + ex.getMessage(), ex);
        } catch (LinkageError ex) {
            throw new SQLException("Error initializing the database driver: " + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Error initializing the database driver: " + ex.getMessage(), ex);
        }
    }

    /**
     * Close the connection to the database
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOGGER.debug(LOG_MESSAGE + "Failed to properly close the connection: " + ex.getMessage());
            }
        }
    }

    /**
     * Return the connection to the database
     *
     * @return
     */
    public Connection getConnection() {
        return connection;
    }
}
