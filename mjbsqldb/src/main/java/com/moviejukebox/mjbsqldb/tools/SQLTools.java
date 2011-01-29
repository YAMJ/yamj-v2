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

public class SQLTools {

    /**
     * Close a connection after committing.
     * @param connection
     */
    public static void close(Connection connection) {
        if (connection == null) {
            return;
        }
        
        try {
            connection.commit();
            connection.close();
        } catch (Throwable tw) {
            // ignore
        }
    }
    
    /**
     * Close a result set
     * @param resultset
     */
    public static void close(ResultSet resultset) {
        try {
            resultset.close();
        } catch (Exception ignore) {
            // ignore
        }
    }
    
    /**
     * Close a statement
     * @param statement
     */
    public static void close(Statement statement) {
        try {
            statement.close();
        } catch (Exception ignore) {
            // ignore
        }
    }

    /** 
     * Rollback a connection
     * @param connection
     */
    public static void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        
        try {
            connection.rollback();
        } catch (Exception ignore) {
            // ignore
        }
    }

}
