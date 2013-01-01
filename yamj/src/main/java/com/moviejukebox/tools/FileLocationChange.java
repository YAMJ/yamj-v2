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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class FileLocationChange {

    private static final Logger logger = Logger.getLogger(FileLocationChange.class);
    private static final String LOG_MESSAGE = "FileLocationChange: ";

    /**
     * Routine to check if a file has changed location
     *
     * @param movie
     * @param library
     * @param jukeboxPreserve
     * @param scannedFiles
     * @param mediaLibraryPaths
     */
    public static void process(Movie movie, Library library, boolean jukeboxPreserve, Collection<MovieFile> scannedFiles, Collection<MediaLibraryPath> mediaLibraryPaths) {
        // Set up some arrays to store the directory scanner files and the XML files
        Collection<MovieFile> xmlFiles = new ArrayList<MovieFile>(movie.getMovieFiles());

        // Now compare the before and after files
        Iterator<MovieFile> scanLoop = scannedFiles.iterator();
        MovieFile sMF;
        String scannedFilename;
        String scannedFileLocation;

        for (MovieFile xmlLoop : xmlFiles) {
            if (xmlLoop.getFile() == null && !jukeboxPreserve) {
                // The file from the scanned XML file doesn't exist so delete it from the XML file
                movie.removeMovieFile(xmlLoop);
                continue;
            }

            if (scanLoop.hasNext()) {
                sMF = scanLoop.next();
                scannedFilename = sMF.getFilename();
                scannedFileLocation = sMF.getFile().getAbsolutePath();
            } else {
                break; // No more files, so quit
            }

            boolean matchFilename = scannedFilename.equalsIgnoreCase(xmlLoop.getFilename());
            boolean matchVideoTs = (scannedFilename + "/VIDEO_TS.IFO").equalsIgnoreCase(xmlLoop.getFilename());
            boolean matchArchive = (sMF.getArchiveName() != null) && !(scannedFilename + "/" + sMF.getArchiveName()).equalsIgnoreCase(xmlLoop.getFilename());
            boolean matchPathLoc = scannedFileLocation.equalsIgnoreCase(xmlLoop.getFile().getAbsolutePath());

            // VIDEO_TS.IFO check added for Issue 1851
            if ((!matchFilename && !matchVideoTs && !matchArchive) || !matchPathLoc) {
                logger.debug(LOG_MESSAGE + "Detected change of file location for >" + xmlLoop.getFilename() + "< to: >" + scannedFilename + "<");
                xmlLoop.setFilename(scannedFilename);
                xmlLoop.setNewFile(true);
                movie.addMovieFile(xmlLoop);

                // if we have more than one path, we'll need to change the library details in the movie
                if (mediaLibraryPaths.size() > 1) {
                    for (MediaLibraryPath mlp : mediaLibraryPaths) {
                        // Check to see if the paths match and then update the description and quit
                        if (scannedFilename.startsWith(mlp.getPlayerRootPath())) {
                            boolean flag = Boolean.TRUE;
                            // Check to see if the filename should be excluded
                            for (String exclude : mlp.getExcludes()) {
                                flag &= (scannedFilename.toUpperCase().indexOf(exclude.toUpperCase()) == -1);
                            }

                            if (flag) {
                                logger.debug(LOG_MESSAGE + "Changing libray description for video '" + movie.getTitle() + "' from '" + movie.getLibraryDescription() + "' to '" + mlp.getDescription() + "'");
                                library.addDirtyLibrary(movie.getLibraryDescription());
                                movie.setLibraryDescription(mlp.getDescription());
                                break;
                            }   // ENDIF flag
                        }   // ENDIF Start With Root Path
                    }   // ENDFOR MediaLibraryPath
                }   // ENDIF MLP > 1
            }
        }
    }
}
