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
package com.moviejukebox.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * User: JDGJr Date: Feb 14, 2009
 */
public class CSVWriter {

    private static final Logger logger = Logger.getLogger(CSVWriter.class);
    private static final String logMessage = "CSVWriter: ";
    private static final String S_EOL = System.getProperty("line.separator");
    private FileWriter writer;

    /**
     * @param csvFile
     */
    public CSVWriter(File csvFile) {
        try {
            writer = new FileWriter(csvFile);
        } catch (Exception ex) {
            logger.error(logMessage + "Error creating CSV file: " + csvFile);
        }
    }

    /**
     * @param str
     */
    public void line(String str) throws IOException {
        writer.write(str + S_EOL);
    }

    /**
     * @throws IOException
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     *
     */
    public void close() {
        try {
            writer.close();
        } catch (Exception ignore) {
        }
    }
} // class CSVWriter
