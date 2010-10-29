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

            stmt.addBatch("CREATE TABLE VIDEO (VIDEO_ID INTEGER PRIMARY KEY, TITLE TEXT, SEARCH_TITLE TEXT, RELEASE_DATE TEXT DEFAULT '9999-01-01', PATH TEXT, DETAIL_POSTER TEXT, THUMBNAIL TEXT, RUNTIME INTEGER, RATING INTEGER DEFAULT 0, RESOLUTION TEXT, WIDTH TEXT, HEIGHT TEXT, ASPECT_RATIO TEXT, PARENTAL_RATING TEXT, SYSTEM TEXT, VIDEO_CODEC TEXT, FPS INTEGER, SIZE INTEGER, PLAY_COUNT INTEGER DEFAULT 0, SHOW_ID INTEGER, EPISODE INTEGER, CREATE_TIME TEXT, WALLPAPER TEXT, CAST_ID TEXT, UPDATE_STATE INTEGER, MOUNT_DEVICE_ID INTEGER, SEASON INTEGER, TTID TEXT, TITLE_TYPE TEXT, VIDEO1 TEXT, VIDEO2 TEXT, VIDEO3 TEXT, VIDEO4 TEXT, VIDEO5 TEXT)");
            stmt.addBatch("CREATE TABLE SHOW (SHOW_ID INTEGER PRIMARY KEY, TITLE TEXT, SEARCH_TITLE TEXT, LAST_PLAY_ITEM INTEGER, TOTAL_ITEM INTEGER DEFAULT 0, RELEASE_DATE TEXT DEFAULT '9999-01-01', DETAIL_POSTER TEXT, THUMBNAIL TEXT, RATING INTEGER DEFAULT 0, RESOLUTION TEXT, WIDTH TEXT, HEIGHT TEXT, PARENTAL_RATING TEXT, SYSTEM TEXT, VIDEO_CODEC TEXT, FPS TEXT, CREATE_TIME TEXT, SHOW_TYPE INTEGER DEFAULT 0, WALLPAPER TEXT, CAST_ID TEXT, UPDATE_STATE INTEGER, TTID TEXT, TOTAL_EPISODES INTEGER, SHOW1 TEXT, SHOW2 TEXT, SHOW3 TEXT, SHOW4 TEXT, SHOW5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_GROUP (VIDEO_GROUP_ID INTEGER PRIMARY KEY, VIDEO_GROUP_NAME_ID INTEGER, VIDEO_ID INTEGER, SHOW_ID INTEGER, PARENTAL_RATING TEXT, SHOW_TYPE INTEGER DEFAULT 0, VIDEO_GROUP1 TEXT, VIDEO_GROUP2 TEXT, VIDEO_GROUP3 TEXT, VIDEO_GROUP4 TEXT, VIDEO_GROUP5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_GROUP_NAME (VIDEO_GROUP_NAME_ID INTEGER PRIMARY KEY, NAME TEXT, LANGUAGE TEXT, VIDEO_GROUP_NAME1 TEXT, VIDEO_GROUP_NAME2 TEXT, VIDEO_GROUP_NAME3 TEXT, VIDEO_GROUP_NAME4 TEXT, VIDEO_GROUP_NAME5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_GENRE (VIDEO_GENRE_ID INTEGER PRIMARY KEY, VIDEO_SHOW_ID INTEGER, GENRE TEXT, TYPE INTEGER, VIDEO_GENRE1 TEXT, VIDEO_GENRE2 TEXT, VIDEO_GENRE3 TEXT, VIDEO_GENRE4 TEXT, VIDEO_GENRE5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_ATTR (VIDEO_ATTR_ID INTEGER PRIMARY KEY, VIDEO_ID INTEGER, SHOW_ID INTEGER, TYPE TEXT, VALUE TEXT, VIDEO_PERSON_ID INTEGER, LANGUAGE TEXT, VIDEO_ATTR1 TEXT, VIDEO_ATTR2 TEXT, VIDEO_ATTR3 TEXT, VIDEO_ATTR4 TEXT, VIDEO_ATTR5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_PERSON (VIDEO_PERSON_ID INTEGER PRIMARY KEY, NAME TEXT, THUMBNAIL TEXT, BIOGRAPHY TEXT, EXTRA_INFO TEXT, VIDEO_PERSON1 TEXT, VIDEO_PERSON2 TEXT, VIDEO_PERSON3 TEXT, VIDEO_PERSON4 TEXT, VIDEO_PERSON5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_CHAPTER (VIDEO_CHAPTER_ID INTEGER PRIMARY KEY, VIDEO_ID INTEGER, TITLE TEXT, CHAPTER_POINT INTEGER, THUMBNAIL TEXT, TYPE INTEGER, VIDEO_CHAPTER1 TEXT, VIDEO_CHAPTER2 TEXT, VIDEO_CHAPTER3 TEXT, VIDEO_CHAPTER4 TEXT, VIDEO_CHAPTER5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_BOOKMARK (VIDEO_BOOKMARK_ID INTEGER PRIMARY KEY, VIDEO_ID INTEGER, BOOKMARK_TIME INTEGER, THUMBNAIL TEXT, VIDEO_BOOKMARK1 TEXT, VIDEO_BOOKMARK2 TEXT, VIDEO_BOOKMARK3 TEXT, VIDEO_BOOKMARK4 TEXT, VIDEO_BOOKMARK5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_LAST_OPEN (VIDEO_LAST_OPEN_ID INTEGER PRIMARY KEY, VIDEO_ID INTEGER, SHOW_ID INTEGER, CREATE_TIME TEXT, VIDEO_LAST_OPEN1 TEXT, VIDEO_LAST_OPEN2 TEXT, VIDEO_LAST_OPEN3 TEXT, VIDEO_LAST_OPEN4 TEXT, VIDEO_LAST_OPEN5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_SYNOPSIS (VIDEO_SYNOPSIS_ID INTEGER PRIMARY KEY, VIDEO_ID INTEGER, SHOW_ID INTEGER, LANGUAGE TEXT, CONTENT TEXT, VIDEO_SYNOPSIS1 TEXT, VIDEO_SYNOPSIS2 TEXT, VIDEO_SYNOPSIS3 TEXT, VIDEO_SYNOPSIS4 TEXT, VIDEO_SYNOPSIS5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_PLS (VIDEO_PLS_ID INTEGER PRIMARY KEY, NAME TEXT, LAST_PLAY_ITEM INTEGER, TOTAL_ITEM INTEGER, CREATE_TIME TEXT, VIDEO_PLS1 TEXT, VIDEO_PLS2 TEXT, VIDEO_PLS3 TEXT, VIDEO_PLS4 TEXT, VIDEO_PLS5 TEXT)");
            stmt.addBatch("CREATE TABLE VIDEO_PLS_ITEM (VIDEO_PLS_ITEM_ID INTEGER PRIMARY KEY, VIDEO_PLS_ID INTEGER, VIDEO_ID INTEGER, SEQUENCE INTEGER, VIDEO_PLS_ITEM1 TEXT, VIDEO_PLS_ITEM2 TEXT, VIDEO_PLS_ITEM3 TEXT, VIDEO_PLS_ITEM4 TEXT, VIDEO_PLS_ITEM5 TEXT)");
            
            stmt.executeBatch();

        } catch (Exception error) {
            logger.severe("SqlTools: Error creating tables: " + error.getMessage());
        }
    }
    
    public static void deleteTables() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            stmt.addBatch("DELETE FROM VIDEO");
            stmt.addBatch("DELETE FROM SHOW");
            stmt.addBatch("DELETE FROM VIDEO_GROUP");
            stmt.addBatch("DELETE FROM VIDEO_GROUP_NAME");
            stmt.addBatch("DELETE FROM VIDEO_GENRE");
            stmt.addBatch("DELETE FROM VIDEO_ATTR");
            stmt.addBatch("DELETE FROM VIDEO_PERSON");
            stmt.addBatch("DELETE FROM VIDEO_CHAPTER");
            stmt.addBatch("DELETE FROM VIDEO_BOOKMARK");
            stmt.addBatch("DELETE FROM VIDEO_LAST_OPEN");
            stmt.addBatch("DELETE FROM VIDEO_SYNOPSIS");
            stmt.addBatch("DELETE FROM VIDEO_PLS");
            stmt.addBatch("DELETE FROM VIDEO_PLS_ITEM");
            
            stmt.executeBatch();
        } catch (Throwable tw) {
            throw new RuntimeException("Delete photo tables error. "
                + tw.getMessage(), tw);
        } finally {
            closeDatabase();
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
