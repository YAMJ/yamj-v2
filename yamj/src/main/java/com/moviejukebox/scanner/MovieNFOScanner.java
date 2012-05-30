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
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GenericFileFilter;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isValidString;
import com.moviejukebox.writer.MovieNFOReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTimeConfig;

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
    // Types of nodes
    private static final String TYPE_MOVIE = "movie";
    private static final String TYPE_TVSHOW = "tvshow";
    private static final String TYPE_EPISODE = "episodedetails";
    // Other properties
    private static final String xbmcTvNfoName = "tvshow";
    // For now, this is deprecated and we should see if there are issues before looking at a solution as the DOM Parser seems a lot more stable
//    private static String forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", "AUTO");
    private static String NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
    private static final boolean acceptAllNFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", "false");
    private static String nfoExtRegex;
    private static final String[] NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
    private static Pattern partPattern = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
    private static boolean archiveScanRar = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", "false");
    private static boolean skipTvNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.skipTVNFOFiles", "false");

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
     * Will either process the file as an XML NFO file or just pick out the poster and fanart URLs
     *
     * Scanning for site specific URLs should be done by each plugin
     *
     * @param movie
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.debug(logMessage + "Scanning NFO file for IDs: " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirty(DirtyFlag.NFO);

            String nfo = FileTools.readFileToString(nfoFile);
            boolean parsedXmlNfo = Boolean.FALSE;   // Was the NFO XML parsed correctly or at all

            if (StringUtils.containsIgnoreCase(nfo, "<" + TYPE_MOVIE + ">")
                    || StringUtils.containsIgnoreCase(nfo, "<" + TYPE_TVSHOW + ">")
                    || StringUtils.containsIgnoreCase(nfo, "<" + TYPE_EPISODE + ">")) {
                parsedXmlNfo = MovieNFOReader.parseNfo(nfoFile, movie);
            }

            // If the XML wasn't found or parsed correctly, then fall back to the old method
            if (parsedXmlNfo) {
                logger.debug(logMessage + "Successfully scanned " + nfoFile.getName() + " as XBMC format");
            } else {
                DatabasePluginController.scanNFO(nfo, movie);

                logger.debug(logMessage + "Scanning NFO for Poster URL");
                int urlStartIndex = 0;
                while (urlStartIndex >= 0 && urlStartIndex < nfo.length()) {
                    int currentUrlStartIndex = nfo.indexOf("http://", urlStartIndex);
                    if (currentUrlStartIndex >= 0) {
                        int currentUrlEndIndex = nfo.indexOf("jpg", currentUrlStartIndex);
                        if (currentUrlEndIndex < 0) {
                            currentUrlEndIndex = nfo.indexOf("JPG", currentUrlStartIndex);
                        }
                        if (currentUrlEndIndex >= 0) {
                            int nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex);
                            // look for shortest http://
                            while ((nextUrlStartIndex != -1) && (nextUrlStartIndex < currentUrlEndIndex + 3)) {
                                currentUrlStartIndex = nextUrlStartIndex;
                                nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex + 1);
                            }

                            // Check to see if the URL has <fanart> at the beginning and ignore it if it does (Issue 706)
                            if ((currentUrlStartIndex < 8)
                                    || (new String(nfo.substring(currentUrlStartIndex - 8, currentUrlStartIndex)).compareToIgnoreCase("<fanart>") != 0)) {
                                String foundUrl = new String(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));

                                // Check for some invalid characters to see if the URL is valid
                                if (foundUrl.contains(" ") || foundUrl.contains("*")) {
                                    urlStartIndex = currentUrlStartIndex + 3;
                                } else {
                                    logger.debug(logMessage + "Poster URL found in nfo = " + foundUrl);
                                    movie.setPosterURL(new String(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3)));
                                    urlStartIndex = -1;
                                    movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
                                }
                            } else {
                                logger.debug(logMessage + "Poster URL ignored in NFO because it's a fanart URL");
                                // Search for the URL again
                                urlStartIndex = currentUrlStartIndex + 3;
                            }
                        } else {
                            urlStartIndex = currentUrlStartIndex + 3;
                        }
                    } else {
                        urlStartIndex = -1;
                    }
                }
            }
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
