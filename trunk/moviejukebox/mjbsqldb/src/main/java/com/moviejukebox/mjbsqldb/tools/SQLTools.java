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
package com.moviejukebox.mjbsqldb.tools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

public class SQLTools {

    private static final Logger LOGGER = Logger.getLogger(SQLTools.class);
    private static final String LOG_MESSAGE = "SQLTools: ";

    private SQLTools() {
        throw new RuntimeException("Class cannot be initialised!");
    }

    /**
     * Close a connection after committing.
     *
     * @param connection
     */
    public static void close(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.commit();
            connection.close();
        } catch (SQLException ex) {
            LOGGER.debug(LOG_MESSAGE + "Failed to close connection: " + ex.getMessage());
        }
    }

    /**
     * Close a result set
     *
     * @param resultset
     */
    public static void close(ResultSet resultset) {
        try {
            resultset.close();
        } catch (SQLException ex) {
            LOGGER.debug(LOG_MESSAGE + "Failed to close resultset: " + ex.getMessage());
        }
    }

    /**
     * Close a statement
     *
     * @param statement
     */
    public static void close(Statement statement) {
        try {
            statement.close();
        } catch (SQLException ex) {
            LOGGER.debug(LOG_MESSAGE + "Failed to close statement: " + ex.getMessage());
        }
    }

    /**
     * Rollback a connection
     *
     * @param connection
     */
    public static void rollback(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException ex) {
            LOGGER.debug(LOG_MESSAGE + "Failed rollback connection: " + ex.getMessage());
        }
    }
}
