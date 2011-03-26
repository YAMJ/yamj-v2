/**
 * @(#)FilePropertiesMovie.java 1.0 26.01.06 (dd.mm.yy)
 *
 * Copyright (2003) Mediterranean
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Boston, MA 02111.
 * 
 * Contact: mediterranean@users.sourceforge.net
 * -----------------------------------------------------
 * gaelead modifications :
 * - org.apache.log4j.Logger switched to org.apache.log4j.Logger
 * - restricted use of FilePropertiesIFO only
 * - won't use mediainfo here
 * - removed all unused code
 **/
package net.sf.xmm.moviemanager.fileproperties;

import java.io.File;
import java.io.RandomAccessFile;
import org.apache.log4j.Logger;

/**
 * @author Bro3@sf.net
 */
public class FilePropertiesMovie {

    static Logger logger = Logger.getLogger("moviejukebox");
    /**
     * The filesize.
     */
    private int _fileSize = -1;
    /**
     * The video duration (seconds).
     */
    private int _duration = -1;
    /**
     * The file location.
     */
    private String _location = "";
    protected String fileName = "";
    private boolean infoAvailable = false;
    private boolean supported = false;

    /**
     * Class gets all info available from the media file.
     *
     * Class constructor specifying number of objects to create.
     *
     * @param filePath
     *            a path for the file.
     */
    public FilePropertiesMovie(String filePath) throws Exception {

        FileProperties fileProperties = null;

        /**
         * The respective objects.
         */
        fileProperties = new FilePropertiesIFO();

        _fileSize = Math.round((new File(filePath).length()) / 1024F / 1024F);

        /* The input stream... */
        RandomAccessFile dataStream = new RandomAccessFile(filePath, "r");

        /* Gets the header for filetype identification... */
        int[] header = new int[4];

        for (int i = 0; i < header.length; i++) {
            header[i] = dataStream.readUnsignedByte();
        }

        try {

            /* Starts parsing the file... */
            fileProperties.process(dataStream);

            supported = fileProperties.isSupported();

            if (supported) {
                infoAvailable = true;
            } else {
                throw new Exception("Unable to retrieve info");
            }

            /* Gets the processed info... */
            _duration = fileProperties.getDuration();
            _location = filePath;

            fileName = new File(filePath).getName();

            /* Closes the input stream... */
            dataStream.close();

        } catch (Exception error) {
            logger.info("Error processing - " + filePath + " : " + error.getMessage());

            /*
             * The file is corrupted, tries to save the info that may have been
             * found
             */
            if (fileProperties != null) {
                _duration = fileProperties.getDuration();
                _location = filePath;

                fileName = new File(filePath).getName();
            }
            throw new Exception("File could be corrupted. Some info may have been saved.");
        }
    }

    /**
     * @return all the info in a string
     */
    public String toString() {
        return "MovieProperties [ fileSize:" + _fileSize + ", " + "duration:(" + _duration + ") " + "  ]";
    }

    /**
     * @return if the info is available
     */
    public boolean getInfoAvailable() {
        return infoAvailable;
    }

    /**
     * @return if the info is available
     */
    public boolean getFileFormatSupported() {
        return supported;
    }

    /**
     * @return the filesize in MiB
     */
    public int getFileSize() {
        return _fileSize;
    }

    /**
     * @return gets the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * sets the file name
     */
    public void setFileName(String newName) {
        fileName = newName;
    }

    /**
     * @return the duration in seconds
     */
    public int getDuration() {
        return _duration;
    }

    /**
     * @return the file location
     */
    public String getLocation() {
        return _location;
    }
}
