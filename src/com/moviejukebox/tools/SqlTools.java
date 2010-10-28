package com.moviejukebox.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;

public class SqlTools {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Connection connection = null;
    
    private static String INSERT_VIDEO = "insert into VIDEO (TITLE, POSTER, PATH) values (?, ?, ?)";
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception error) {
            logger.severe("SqlTools: Error getting database driver");
        }
    }

    public static void openDatabase(String databaseName) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:./" + databaseName);
            connection.setAutoCommit(false);
            createTables();
            connection.commit();
            logger.fine("SqlTools: Opened database - " + databaseName);
        } catch (Exception error) {
            logger.severe("SqlTools: Error opening database: " + error.getMessage());
        }
    }
    
    public static void closeDatabase() {
        try {
            if (connection != null) {
                connection.close();
            }
            logger.fine("SqlTools: Closed database - " + connection.getCatalog());
        } catch (Exception error) {
            logger.severe("SqlTools: Error closing database: " + error.getMessage());
        }
    }
    
    public static void createTables() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            stmt.addBatch("CREATE TABLE MOVIE (TITLE TEXT, POSTER TEXT, PATH TEXT)");
            
            stmt.executeBatch();

        } catch (Exception error) {
            logger.severe("SqlTools: Error creating tables: " + error.getMessage());
        }
    }

    public static void insertIntoVideo(Movie movie) {
        PreparedStatement pstmt = null;
        
        try {
            pstmt = connection.prepareStatement(INSERT_VIDEO);
            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getPosterFilename());
            pstmt.setString(3, movie.getBaseFilename());
            pstmt.executeUpdate();
            connection.commit();
        } catch (Exception error) {
            logger.severe("SqlTools: Error inserting into VIDEO table: " + error.getMessage());
        }
    }
    
    
}
