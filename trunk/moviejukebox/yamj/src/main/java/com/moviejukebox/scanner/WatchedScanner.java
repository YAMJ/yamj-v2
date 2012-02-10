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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;

public class WatchedScanner {

    private static Logger logger = Logger.getLogger(WatchedScanner.class);

    /**
     * Calculate the watched state of a movie based on the files <filename>.watched & <filename>.unwatched
     * Always assumes that the file is unwatched if nothing is found.
     * @param movie
     */
    public static boolean checkWatched(Jukebox jukebox, Movie movie) {
        int fileWatchedCount = 0;                       // The number of watched files found
        boolean movieWatched = Boolean.TRUE;            // Status of all of the movie files, to be saved in the movie bean
        boolean movieFileWatchChanged = Boolean.FALSE;  // Have the movie files changed status?
        boolean fileWatched;
        boolean returnStatus = Boolean.FALSE;           // Assume no changes

        File foundFile;
        Collection<String> extensions = new ArrayList<String>();
        extensions.add("unwatched");
        extensions.add("watched");

        for (MovieFile mf : movie.getFiles()) {
            // Check that the file pointer is valid
            if (mf.getFile() == null) {
                continue;
            }

            fileWatched = Boolean.FALSE;
            String filename;
            // BluRay stores the file differently to DVD and single files, so we need to process the path a little
            if (movie.isBluray()) {
                filename = new File(FileTools.getParentFolder(mf.getFile())).getName();
            } else {
                filename = mf.getFile().getName();
            }

            foundFile = FileTools.findFilenameInCache(filename, extensions, jukebox, "Watched Scanner: ");

            // If we didn't find the file, we should look without the extension
            if (foundFile == null && !movie.isBluray()) {
                // Remove the extension from the filename
                filename = FilenameUtils.removeExtension(filename);
                // Check again without the extension
                foundFile = FileTools.findFilenameInCache(filename, extensions, jukebox, "Watched Scanner: ");
            }

            if (foundFile != null) {
                fileWatchedCount++;
                if (foundFile.getName().toLowerCase().endsWith(".watched")) {
                    fileWatched = Boolean.TRUE;
                    mf.setWatchedDate(new DateTime().toMillis());
                } else {
                    // We've found a specific file <filename>.unwatched, so we clear the settings
                    fileWatched = Boolean.FALSE;
                    mf.setWatchedDate(0); // remove the date if it exists
                }
            }

            if (fileWatched != mf.isWatched()) {
                movieFileWatchChanged = Boolean.TRUE;
            }

            mf.setWatched(fileWatched); // Set the watched status
            // As soon as there is an unwatched file, the whole movie becomes unwatched
            movieWatched = movieWatched && fileWatched;
        }

        if (movieFileWatchChanged) {
            movie.setDirty(Movie.DIRTY_WATCHED, Boolean.TRUE);
        }

        // Only change the watched status if we found at least 1 file
        if ((fileWatchedCount > 0) && (movie.isWatchedFile() != movieWatched)) {
            movie.setWatchedFile(movieWatched);
            movie.setDirty(Movie.DIRTY_WATCHED, Boolean.TRUE);

            // Issue 1949 - Force the artwork to be overwritten (those that can have icons on them)
            movie.setDirty(Movie.DIRTY_POSTER, Boolean.TRUE);
            movie.setDirty(Movie.DIRTY_BANNER, Boolean.TRUE);

            returnStatus = Boolean.TRUE;
        }

        // If there are no files found and the movie is watched(file), reset the status
        if ((fileWatchedCount == 0) && movie.isWatchedFile()) {
            movie.setWatchedFile(movieWatched);
            movie.setDirty(Movie.DIRTY_WATCHED, Boolean.TRUE);

            // Issue 1949 - Force the artwork to be overwritten (those that can have icons on them)
            movie.setDirty(Movie.DIRTY_POSTER, Boolean.TRUE);
            movie.setDirty(Movie.DIRTY_BANNER, Boolean.TRUE);

            returnStatus = Boolean.TRUE;
        }

        returnStatus |= movieFileWatchChanged;
        if (returnStatus) {
            logger.debug("Watched Scanner: The video has one or more files that have changed status.");
        }

        return returnStatus;
    }
}
