/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.reader.MovieNFOReader;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GenericFileFilter;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.pojava.datetime.DateTimeConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NFO file parser.
 *
 * Search a NFO file for IMDb URL.
 *
 * @author jjulien
 */
public final class MovieNFOScanner {

    private static final Logger LOG = LoggerFactory.getLogger(MovieNFOScanner.class);
    // Other properties
    private static final String XBMC_TV_NFO_NAME = "tvshow";
    // For now, this is deprecated and we should see if there are issues before looking at a solution as the DOM Parser seems a lot more stable
    private static final String NFO_DIR = PropertiesUtil.getProperty("filename.nfo.directory", "");
    private static final boolean ACCEPT_ALL_NFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", Boolean.FALSE);
    private static final String NFO_EXT_REGEX;
    private static final String[] NFO_EXTENSIONS = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
    private static final Pattern PART_PATTERN = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
    private static final boolean ARCHIVE_SCAN_RAR = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", Boolean.FALSE);
    private static final boolean SKIP_TV_NFO_FILES = PropertiesUtil.getBooleanProperty("filename.nfo.skipTVNFOFiles", Boolean.FALSE);

    static {
        if (ACCEPT_ALL_NFO) {
            LOG.info("Accepting all NFO files in the directory");
        }

        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        {
            boolean first = Boolean.TRUE;
            StringBuilder regexBuilder = new StringBuilder("(?i).*\\.("); // Start of REGEX
            for (String ext : NFO_EXTENSIONS) {
                if (first) {
                    first = Boolean.FALSE;
                } else {
                    regexBuilder.append("|"); // Add seperator
                }
                regexBuilder.append(ext).append("$"); // Add extension
            }
            regexBuilder.append(")"); // End of REGEX
            NFO_EXT_REGEX = regexBuilder.toString();
        }

        // Set the date format to dd-MM-yyyy
//        DateTimeConfig.globalEuropeanDateFormat();
        DateTimeConfigBuilder.newInstance().setDmyOrder(true);
    }

    private MovieNFOScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Process the NFO file.
     *
     * Will either process the file as an XML NFO file or just pick out the
     * poster and fanart URLs
     *
     * Scanning for site specific URLs should be done by each plugin
     *
     * @param movie
     * @param nfoFiles
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            LOG.debug("Scanning NFO file for information: {}", nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirty(DirtyFlag.NFO);
            MovieNFOReader.readNfoFile(nfoFile, movie);
        }
    }

    /**
     * Search for the NFO file in the library structure
     *
     * @param movie The movie bean to locate the NFO for
     * @return A List structure of all the relevant NFO files
     */
    public static List<File> locateNFOs(Movie movie) {
        List<File> nfoFiles = new ArrayList<>();
        GenericFileFilter fFilter;

        File currentDir = movie.getFirstFile().getFile();

        if (currentDir == null) {
            return nfoFiles;
        }

        String baseFileName = currentDir.getName();
        String pathFileName = currentDir.getAbsolutePath();

        // Get the folder if it's a BluRay disk
        if (pathFileName.toUpperCase().contains(File.separator + "BDMV" + File.separator)) {
            currentDir = new File(FileTools.getParentFolder(currentDir));
            baseFileName = currentDir.getName();
            pathFileName = currentDir.getAbsolutePath();
        }

        if (ARCHIVE_SCAN_RAR && pathFileName.toLowerCase().contains(".rar")) {
            currentDir = new File(FileTools.getParentFolder(currentDir));
            baseFileName = currentDir.getName();
            pathFileName = currentDir.getAbsolutePath();
        }

        // If "pathFileName" is a file then strip the extension from the file.
        if (currentDir.isFile()) {
            pathFileName = FilenameUtils.removeExtension(pathFileName);
            baseFileName = FilenameUtils.removeExtension(baseFileName);
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfoFiles, pathFileName + pathFileName.substring(pathFileName.lastIndexOf(File.separator)));
        }

        // TV Show specific scanning
        if (movie.isTVShow()) {
            String nfoFilename;

            // Check for the "tvshow.nfo" filename in the parent directory
            if (movie.getFile().getParentFile().getParent() != null) {
                nfoFilename = StringTools.appendToPath(movie.getFile().getParentFile().getParent(), XBMC_TV_NFO_NAME);
                checkNFO(nfoFiles, nfoFilename);
            }

            // Check for the "tvshow.nfo" filename in the current directory
            nfoFilename = StringTools.appendToPath(movie.getFile().getParent(), XBMC_TV_NFO_NAME);
            checkNFO(nfoFiles, nfoFilename);

            // Check for individual episode files
            if (!SKIP_TV_NFO_FILES) {
                for (MovieFile mf : movie.getMovieFiles()) {
                    nfoFilename = mf.getFile().getParent().toUpperCase();

                    if (nfoFilename.contains("BDMV")) {
                        nfoFilename = FileTools.getParentFolder(mf.getFile());
                        nfoFilename = nfoFilename.substring(nfoFilename.lastIndexOf(File.separator) + 1);
                    } else {
                        nfoFilename = FilenameUtils.removeExtension(mf.getFile().getName());
                    }

                    checkNFO(nfoFiles, StringTools.appendToPath(mf.getFile().getParent(), nfoFilename));
                }
            }
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfoFiles, pathFileName);

        if (isValidString(NFO_DIR)) {
            // *** Next step if we still haven't found the nfo file is to
            // search the NFO directory as specified in the moviejukebox.properties file
            String sNFOPath = FileTools.getDirPathWithSeparator(movie.getLibraryPath()) + NFO_DIR;
            checkNFO(nfoFiles, sNFOPath + File.separator + baseFileName);
        }

        // *** Next step is to check for a directory wide NFO file.
        if (ACCEPT_ALL_NFO) {
            /*
             * If any NFO file in this directory will do, then we search for all we can find
             *
             * NOTE: for scanning efficiency, it is better to first search for specific filenames before we start doing
             * filtered "listfiles" which scans all the files;
             *
             * A movie collection with all moviefiles in one directory could take tremendously longer if for each
             * moviefile found, the entire directory must be listed!!
             *
             * Therefore, we first check for specific filenames (cfr. old behaviour) before doing an entire scan of the
             * directory -- and only if the user has decided to accept any NFO file!
             */

            // Check the current directory
            fFilter = new GenericFileFilter(NFO_EXT_REGEX);
            checkRNFO(nfoFiles, currentDir.getParentFile(), fFilter);

            // Also check the directory above, for the case where movies are in a multi-part named directory (CD/PART/DISK/Etc.)
            Matcher allNfoMatch = PART_PATTERN.matcher(currentDir.getAbsolutePath());
            if (allNfoMatch.find()) {
                LOG.debug("Found multi-part directory, checking parent directory for NFOs");
                checkRNFO(nfoFiles, currentDir.getParentFile().getParentFile(), fFilter);
            }
        } else {
            // This file should be named the same as the directory that it is in
            // E.G. C:\TV\Chuck\Season 1\Season 1.nfo
            // We search up through all containing directories up to the library root

            // Check the current directory for the video filename
            fFilter = new GenericFileFilter("(?i)" + movie.getBaseFilename() + NFO_EXT_REGEX);
            checkRNFO(nfoFiles, currentDir, fFilter);
        }

        // Recurse through the directories to the library root looking for NFO files
        String libraryRootPath = new File(movie.getLibraryPath()).getAbsolutePath();
        while (currentDir != null && !currentDir.getAbsolutePath().equals(libraryRootPath)) {
            //fFilter.setPattern("(?i)" + currentDir.getName() + nfoExtRegex);
            //checkRNFO(nfos, currentDir, fFilter);
            currentDir = currentDir.getParentFile();
            if (currentDir != null) {
                final String path = currentDir.getPath();
                // Path is not empty & is not the root
                if (!path.isEmpty() && !path.endsWith(File.separator)) {
                    checkNFO(nfoFiles, appendToPath(path, currentDir.getName()));
                }
            }
        }

        // we added the most specific ones first, and we want to parse those the last,
        // so nfo files in sub-directories can override values in directories above.
        Collections.reverse(nfoFiles);

        return nfoFiles;
    }

    /**
     * Search the current directory for all NFO files using a file filter
     *
     * @param nfoFiles
     * @param currentDir
     * @param fFilter
     */
    private static void checkRNFO(List<File> nfoFiles, File currentDir, GenericFileFilter fFilter) {
        File[] fFiles = currentDir.listFiles(fFilter);
        if (fFiles != null && fFiles.length > 0) {
            for (File foundFile : fFiles) {
                LOG.debug("Found {}", foundFile.getName());
                nfoFiles.add(foundFile);
            }
        }
    }

    /**
     * Check to see if the passed filename exists with nfo extensions
     *
     * @param checkNFOfilename (NO EXTENSION)
     * @return blank string if not found, filename if found
     */
    private static void checkNFO(List<File> nfoFiles, String checkNFOfilename) {
        File nfoFile;

        for (String ext : NFO_EXTENSIONS) {
            nfoFile = FileTools.fileCache.getFile(checkNFOfilename + "." + ext);
            if (nfoFile.exists()) {
                LOG.debug("Found {}", nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            }
        }
    }
}
