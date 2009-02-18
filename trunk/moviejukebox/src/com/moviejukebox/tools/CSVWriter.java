package com.moviejukebox.tools;

import java.io.*;
import java.util.logging.Logger;

/**
 * User: JDGJr
 * Date: Feb 14, 2009
 */
public class CSVWriter {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private FileWriter writer;
    static private String sEOL = System.getProperty("line.separator");

    /**
     * @param csvFile
     */
    public CSVWriter(File csvFile) {
        try {
            writer = new FileWriter(csvFile);
        } catch (Exception ex) {
            logger.severe("Error creating CSV file: " + csvFile);
        }
    }

    /**
     * @param str
     */
    public void line(String str) throws IOException {
        writer.write(str + sEOL);
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
