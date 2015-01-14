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
package com.moviejukebox.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: JDGJr Date: Feb 14, 2009
 */
public class CSVWriter {

    private static final Logger LOG = LoggerFactory.getLogger(CSVWriter.class);
    private static final String S_EOL = System.getProperty("line.separator");
    private FileWriter writer;

    /**
     * Create the CSV file and leave it open for writing
     *
     * @param csvFile
     */
    public CSVWriter(File csvFile) {
        try {
            writer = new FileWriter(csvFile);
        } catch (IOException ex) {
            LOG.error("Error creating CSV file: {},error: {}", csvFile, ex.getMessage());
        }
    }

    /**
     * Write the line to the file
     *
     * @param str
     * @throws java.io.IOException
     */
    public void line(String str) throws IOException {
        writer.write(str + S_EOL);
    }

    /**
     * Flush the file
     */
    public void flush() {
        try {
            writer.flush();
        } catch (IOException ex) {
            LOG.trace("Failed to flush CSVWriter: {}", ex.getMessage());
        }

    }

    /**
     * Close the file
     */
    public void close() {
        try {
            writer.close();
        } catch (IOException ex) {
            LOG.trace("Failed to close CSVWriter: {}", ex.getMessage());
        }
    }
}
