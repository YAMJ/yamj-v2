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
import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;

public class WatchedScanner {

    private static Logger logger = Logger.getLogger(WatchedScanner.class);
    private static Collection<String> watchedExtensions = Arrays.asList(PropertiesUtil.getProperty("mjb.watchedExtensions", "watched").split(",;\\|"));
    private static String watchedLocation = PropertiesUtil.getProperty("mjb.watchedLocation", "withVideo");
    private static String withExtension = PropertiesUtil.getProperty("mjb.watched.withExtension", "true");

    protected WatchedScanner() {
        throw new UnsupportedOperationException("Watched Scanner cannot be initialised");
    }

    /**
     * Calculate the watched state of a movie based on the files
     * <filename>.watched & <filename>.unwatched Always assumes that the file is
     * unwatched if nothing is found.
     *
     * @param movie
     */
    public static boolean checkWatched(Jukebox jukebox, Movie movie) {
        int fileWatchedCount = 0;                       // The number of watched files found
        boolean movieWatched = Boolean.TRUE;            // Status of all of the movie files, to be saved in the movie bean
        boolean movieFileWatchChanged = Boolean.FALSE;  // Have the movie files changed status?
        boolean fileWatched;
        boolean returnStatus = Boolean.FALSE;           // Assume no changes

        boolean withJukebox = Boolean.FALSE;
        
        if ("withVideo".equalsIgnoreCase(watchedLocation)) {
            // Normal scanning
        } else if("withJukebox".equalsIgnoreCase(watchedLocation)) {
            // Fixed scanning in the jukebox folder
            withJukebox = Boolean.TRUE;
        } else {
            logger.warn("Watched Scanner: Custom file location not supported for watched scanner");
        }
        
        File foundFile = null;

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

            if ("true".equalsIgnoreCase(withExtension) || "both".equalsIgnoreCase(withExtension) || movie.isBluray()) {
                if (withJukebox) {
                    foundFile = FileTools.findFilenameInCache(filename, watchedExtensions, jukebox, "Watched Scanner: ", Boolean.TRUE);
                } else {
                    foundFile = FileTools.findFilenameInCache(filename, watchedExtensions, jukebox, "Watched Scanner: ", Boolean.FALSE);
                }
            }

            if (foundFile == null && ("false".equalsIgnoreCase(withExtension) || "both".equalsIgnoreCase(withExtension)) && !movie.isBluray()) {
                // Remove the extension from the filename
                filename = FilenameUtils.removeExtension(filename);
                // Check again without the extension
                foundFile = FileTools.findFilenameInCache(filename, watchedExtensions, jukebox, "Watched Scanner: ");
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
