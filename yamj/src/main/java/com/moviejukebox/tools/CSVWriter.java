/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import org.apache.log4j.Logger;

/**
 * User: JDGJr Date: Feb 14, 2009
 */
public class CSVWriter {

    private static final Logger LOG = Logger.getLogger(CSVWriter.class);
    private static final String LOG_MESSAGE = "CSVWriter: ";
    private static final String S_EOL = System.getProperty("line.separator");
    private FileWriter writer;

    /**
     * @param csvFile
     */
    public CSVWriter(File csvFile) {
        try {
            writer = new FileWriter(csvFile);
        } catch (IOException ex) {
            LOG.error(LOG_MESSAGE + "Error creating CSV file: " + csvFile);
        }
    }

    /**
     * @param str
     * @throws java.io.IOException
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
        } catch (IOException ignore) {
        }
    }
} // class CSVWriter
