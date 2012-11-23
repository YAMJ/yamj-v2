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

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.reader.MovieNFOReader;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GenericFileFilter;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTimeConfig;

/**
 * NFO file parser.
 *
 * Search a NFO file for IMDb URL.
 *
 * @author jjulien
 */
public class MovieNFOScanner {

    private static final Logger logger = Logger.getLogger(MovieNFOScanner.class);
    private static final String logMessage = "MovieNFOScanner: ";
    // Other properties
    private static final String xbmcTvNfoName = "tvshow";
    // For now, this is deprecated and we should see if there are issues before looking at a solution as the DOM Parser seems a lot more stable
//    private static String forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", "AUTO");
    private static String NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
    private static final boolean acceptAllNFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", FALSE);
    private static String nfoExtRegex;
    private static final String[] NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
    private static Pattern partPattern = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
    private static boolean archiveScanRar = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", FALSE);
    private static boolean skipTvNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.skipTVNFOFiles", FALSE);

    static {
        if (acceptAllNFO) {
            logger.info(logMessage + "Accepting all NFO files in the directory");
        }

        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        {
            boolean first = Boolean.TRUE;
            StringBuilder regexBuilder = new StringBuilder("(?i).*\\.("); // Start of REGEX
            for (String ext : NFOExtensions) {
                if (first) {
                    first = Boolean.FALSE;
                } else {
                    regexBuilder.append("|"); // Add seperator
                }
                regexBuilder.append(ext).append("$"); // Add extension
            }
            regexBuilder.append(")"); // End of REGEX
            nfoExtRegex = regexBuilder.toString();
        }

        // Set the date format to dd-MM-yyyy
        DateTimeConfig.globalEuropeanDateFormat();
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
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.debug(logMessage + "Scanning NFO file for information: " + nfoFile.getName());
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
        List<File> nfoFiles = new ArrayList<File>();
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

        if (archiveScanRar && pathFileName.toLowerCase().contains(".rar")) {
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
            checkNFO(nfoFiles, pathFileName + new String(pathFileName.substring(pathFileName.lastIndexOf(File.separator))));
        }

        // TV Show specific scanning
        if (movie.isTVShow()) {
            // Check for the "tvshow.nfo" filename in the parent directory
            checkNFO(nfoFiles, movie.getFile().getParentFile().getParent() + File.separator + xbmcTvNfoName);

            // Check for individual episode files
            if (!skipTvNfoFiles) {
                String mfFilename;

                for (MovieFile mf : movie.getMovieFiles()) {
                    mfFilename = mf.getFile().getParent().toUpperCase();

                    if (mfFilename.contains("BDMV")) {
                        mfFilename = FileTools.getParentFolder(mf.getFile());
                        mfFilename = new String(mfFilename.substring(mfFilename.lastIndexOf(File.separator) + 1));
                    } else {
                        mfFilename = FilenameUtils.removeExtension(mf.getFile().getName());
                    }

                    checkNFO(nfoFiles, mf.getFile().getParent() + File.separator + mfFilename);
                }
            }
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfoFiles, pathFileName);

        if (isValidString(NFOdirectory)) {
            // *** Next step if we still haven't found the nfo file is to
            // search the NFO directory as specified in the moviejukebox.properties file
            String sNFOPath = FileTools.getDirPathWithSeparator(movie.getLibraryPath()) + NFOdirectory;
            checkNFO(nfoFiles, sNFOPath + File.separator + baseFileName);
        }

        // *** Next step is to check for a directory wide NFO file.
        if (acceptAllNFO) {
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
            fFilter = new GenericFileFilter(nfoExtRegex);
            checkRNFO(nfoFiles, currentDir.getParentFile(), fFilter);

            // Also check the directory above, for the case where movies are in a multi-part named directory (CD/PART/DISK/Etc.)
            Matcher allNfoMatch = partPattern.matcher(currentDir.getAbsolutePath());
            if (allNfoMatch.find()) {
                logger.debug(logMessage + "Found multi-part directory, checking parent directory for NFOs");
                checkRNFO(nfoFiles, currentDir.getParentFile().getParentFile(), fFilter);
            }
        } else {
            // This file should be named the same as the directory that it is in
            // E.G. C:\TV\Chuck\Season 1\Season 1.nfo
            // We search up through all containing directories up to the library root

            if (currentDir != null) {
                // Check the current directory for the video filename
                fFilter = new GenericFileFilter("(?i)" + movie.getBaseFilename() + nfoExtRegex);
                checkRNFO(nfoFiles, currentDir, fFilter);
            }
        }

        // Recurse through the directories to the library root looking for NFO files
        String libraryRootPath = new File(movie.getLibraryPath()).getAbsolutePath();
        while (currentDir != null && !currentDir.getAbsolutePath().equals(libraryRootPath)) {
            //fFilter.setPattern("(?i)" + currentDir.getName() + nfoExtRegex);
            //checkRNFO(nfos, currentDir, fFilter);
            currentDir = currentDir.getParentFile();
            if (currentDir != null) {
                final String path = currentDir.getPath();
                // Path is not empty
                if (!path.isEmpty()) {
                    // Path is not the root
                    if (!path.endsWith(File.separator)) {
                        checkNFO(nfoFiles, appendToPath(path, currentDir.getName()));
                    }
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
                logger.debug(logMessage + "Found " + foundFile.getName());
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

        for (String ext : NFOExtensions) {
            nfoFile = FileTools.fileCache.getFile(checkNFOfilename + "." + ext);
            if (nfoFile.exists()) {
                logger.debug(logMessage + "Found " + nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            }
        }
    }
}
