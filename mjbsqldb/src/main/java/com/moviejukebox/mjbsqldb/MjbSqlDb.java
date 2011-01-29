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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sqlite.SQLiteJDBCLoader;

import com.moviejukebox.mjbsqldb.tools.DatabaseTools;
import com.moviejukebox.mjbsqldb.tools.SQLTools;

public class MjbSqlDb {
    private static final float VERSION = 1.35f;
    protected static Connection connection = null;

    static {
        // Set up the driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Throwable tw) {
            throw new RuntimeException("Error initializing the database driver: " + tw.getMessage(), tw);
        }
        System.out.println(String.format("Driver running in %s mode", SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java"));

    }

    /**
     * Create or Open the database specified
     * @param dbPath
     * @param dbName
     */
    public MjbSqlDb(String dbPath, String dbName) {
        if (StringUtils.isBlank(dbPath) || StringUtils.isBlank(dbName)) {
            throw new RuntimeException("Error: Path or database name is blank: ");
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
                dbFile.createNewFile();
            }
            
            // Create the connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
            connection.setAutoCommit(false);

            // Check the version of the database against the program version
            Float dbVersion = 0.0f;
            try {
                dbVersion = DatabaseTools.getDatabaseVersion(connection);
            } catch (Exception error) {
                System.out.println("Database version out of date. Updating...");
                dbVersion = 0.0f;
            }
            
            if (VERSION > dbVersion) {
                System.out.println("Updating database structure...");
                // This is overkill, but OK for the time being
                DatabaseTools.deleteTables(connection);
            }
            // Create the tables (if they don't exist)
            DatabaseTools.createTables(connection, VERSION);
            
        } catch (Throwable tw) {
            SQLTools.close(connection);
            throw new RuntimeException("Error opening the database: " + tw.getMessage(), tw);
        } finally {
            // Anything to do here?
        }
    }

    /**
     * Close the connection to the database
     */
    public void close() {
        if (connection != null) {
            SQLTools.close(connection);
        }
    }

    /**
     * Return the connection to the database
     * @return
     */
    public static Connection getConnection() {
        return connection;
    }

}
