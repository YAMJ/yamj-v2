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
package com.moviejukebox;

import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.Comparator.PersonComparator;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.IndexInfo;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.JukeboxStatistic;
import com.moviejukebox.model.JukeboxStatistics;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.plugin.AniDbPlugin;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultImagePlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.MovieListingPlugin;
import com.moviejukebox.plugin.MovieListingPluginBase;
import com.moviejukebox.plugin.OpenSubtitlesPlugin;
import com.moviejukebox.plugin.RottenTomatoesPlugin;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.reader.MovieJukeboxLibraryReader;
import com.moviejukebox.reader.MovieJukeboxXMLReader;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.scanner.OutputDirectoryScanner;
import com.moviejukebox.scanner.RecheckScanner;
import com.moviejukebox.scanner.TrailerScanner;
import com.moviejukebox.scanner.WatchedScanner;
import com.moviejukebox.scanner.artwork.ArtworkScanner;
import com.moviejukebox.scanner.artwork.BackdropScanner;
import com.moviejukebox.scanner.artwork.BannerScanner;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.scanner.artwork.FanartTvScanner;
import com.moviejukebox.scanner.artwork.PhotoScanner;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.scanner.artwork.VideoImageScanner;
import com.moviejukebox.tools.FileLocationChange;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.FilteringLayout;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.JukeboxProperties;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.PropertiesUtil.KeywordMap;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import static com.moviejukebox.tools.PropertiesUtil.getBooleanProperty;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static com.moviejukebox.tools.PropertiesUtil.setPropertiesStreamName;
import static com.moviejukebox.tools.PropertiesUtil.setProperty;
import com.moviejukebox.tools.ScanningLimit;
import com.moviejukebox.tools.SkinProperties;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.tokenizeToArray;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.cache.CacheMemory;
import com.moviejukebox.writer.CompleteMoviesWriter;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.sanselan.ImageReadException;

public class MovieJukebox {

    private static final String LOG_FILENAME = "moviejukebox";
    private static final Logger LOG = Logger.getLogger(MovieJukebox.class);
    private static Collection<MediaLibraryPath> mediaLibraryPaths;
    private static final String SKIN_DIR = "mjb.skin.dir";
    private static final String SPACE_TO_SPACE = " to ";
    private static final String EXT_PNG = "png";
    public static final String EXT_DOT_XML = ".xml";
    private static final String DUMMY_JPG = "dummy.jpg";
    private static final String ERROR_TEXT = " - Error: ";
    private static final String RIGHT = "right";
    private static final String LEFT = "left";
    private static final String THUMBNAILS = "thumbnails";
    private static final String POSTERS = "posters";
    private final String movieLibraryRoot;
    private static String userPropertiesName = "./moviejukebox.properties";
    // Jukebox parameters
    private static Jukebox jukebox;
    private static boolean jukeboxPreserve = Boolean.FALSE;
    private static boolean jukeboxClean = Boolean.FALSE;
    // Overwrite flags
    private final boolean forcePosterOverwrite;
    private final boolean forceThumbnailOverwrite;
    private final boolean forceBannerOverwrite;
    private final boolean forceSkinOverwrite;
    private final boolean forceIndexOverwrite;
    private final boolean forceFooterOverwrite;
    // Scanner Tokens
    private static String posterToken;
    private static String thumbnailToken;
    private static String bannerToken;
    private static String wideBannerToken;
    private static String defaultSource;
    private static String fanartToken;
    private static String footerToken;
    private static String posterExtension;
    private static String thumbnailExtension;
    private static String bannerExtension;
    private static String fanartExtension;
    private static Integer footerCount;
    private static final List<String> FOOTER_NAME = new ArrayList<String>();
    private static final List<Boolean> FOOTER_ENABLE = new ArrayList<Boolean>();
    private static final List<Integer> FOOTER_WIDTH = new ArrayList<Integer>();
    private static final List<Integer> FOOTER_HEIGHT = new ArrayList<Integer>();
    private static final List<String> FOOTER_EXTENSION = new ArrayList<String>();
    private static boolean fanartMovieDownload;
    private static boolean fanartTvDownload;
    private static boolean videoimageDownload;
    private static boolean bannerDownload;
    private static boolean photoDownload;
    private static boolean backdropDownload;
    private static boolean enableRottenTomatoes;
    private final boolean setIndexFanart;
    private static boolean skipIndexGeneration = Boolean.FALSE;
    private static boolean skipHtmlGeneration = Boolean.FALSE;
    private static boolean skipPlaylistGeneration = Boolean.FALSE;
    private static boolean dumpLibraryStructure = Boolean.FALSE;
    private static boolean showMemory = Boolean.FALSE;
    private static boolean peopleScan = Boolean.FALSE;
    private static boolean peopleScrape = Boolean.TRUE;
    private static int peopleMax = 10;
    private static int popularity = 5;
    private static String peopleFolder = "";
    private static final Collection<String> PHOTO_EXTENSIONS = new ArrayList<String>();
    // These are pulled from the Manifest.MF file that is created by the build script
    private static final String MJB_VERSION = SystemTools.getVersion();
    private static final String MJB_REVISION = SystemTools.getRevision();
    private static final String MJB_BUILD_DATE = SystemTools.getBuildDate();
    private static boolean trailersScannerEnable;
    private static int maxThreadsProcess = 1;
    private static int maxThreadsDownload = 1;
    private static boolean enableWatchScanner;
    private static boolean enableCompleteMovies;
    // Exit codes
    private static final int EXIT_NORMAL = 0;
    private static final int EXIT_SCAN_LIMIT = 1;
    // Directories to exclude from dump command
    private static final String[] EXCLUDED = {"dumpDir", ".svn", "src", "test", "bin", "skins"};

    public static void main(String[] args) throws Throwable {
        JukeboxStatistics.setTimeStart(System.currentTimeMillis());

        // Create the log file name here, so we can change it later (because it's locked
        System.setProperty("file.name", LOG_FILENAME);
        PropertyConfigurator.configure("properties/log4j.properties");

        LOG.info("Yet Another Movie Jukebox " + MJB_VERSION);
        LOG.info("~~~ ~~~~~~~ ~~~~~ ~~~~~~~ " + StringUtils.repeat("~", MJB_VERSION.length()));
        LOG.info("http://code.google.com/p/moviejukebox/");
        LOG.info("Copyright (c) 2004-2013 YAMJ Members");
        LOG.info("");
        LOG.info("This software is licensed under the GNU General Public License v3+");
        LOG.info("See this page: http://code.google.com/p/moviejukebox/wiki/License");
        LOG.info("");

        // Print the revision information if it was populated
        if (MJB_REVISION.equals("0000")) {
            LOG.info("     Revision: *Custom Build*");
        } else {
            LOG.info("     Revision: r" + MJB_REVISION);
        }
        LOG.info("   Build Date: " + MJB_BUILD_DATE);
        LOG.info("");

        LOG.info(" Java Version: " + java.lang.System.getProperties().getProperty("java.version"));
        LOG.info("");

        if (!SystemTools.validateInstallation()) {
            LOG.info("ABORTING.");
            return;
        }

        String movieLibraryRoot = null;
        String jukeboxRoot = null;
        Map<String, String> cmdLineProps = new LinkedHashMap<String, String>();

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = (String) args[i];
                if ("-v".equalsIgnoreCase(arg)) {
                    // We've printed the version, so quit now
                    return;
                } else if ("-o".equalsIgnoreCase(arg)) {
                    jukeboxRoot = args[++i];
                    PropertiesUtil.setProperty("mjb.jukeboxRoot", jukeboxRoot);
                } else if ("-c".equalsIgnoreCase(arg)) {
                    jukeboxClean = Boolean.TRUE;
                    PropertiesUtil.setProperty("mjb.jukeboxClean", TRUE);
                } else if ("-k".equalsIgnoreCase(arg)) {
                    setJukeboxPreserve(Boolean.TRUE);
                } else if ("-p".equalsIgnoreCase(arg)) {
                    userPropertiesName = args[++i];
                } else if ("-i".equalsIgnoreCase(arg)) {
                    skipIndexGeneration = Boolean.TRUE;
                    PropertiesUtil.setProperty("mjb.skipIndexGeneration", TRUE);
                } else if ("-h".equalsIgnoreCase(arg)) {
                    skipHtmlGeneration = Boolean.TRUE;
                    PropertiesUtil.setProperty("mjb.skipHtmlGeneration", Boolean.TRUE);
                } else if ("-dump".equalsIgnoreCase(arg)) {
                    dumpLibraryStructure = Boolean.TRUE;
                } else if ("-memory".equalsIgnoreCase(arg)) {
                    showMemory = Boolean.TRUE;
                    PropertiesUtil.setProperty("mjb.showMemory", Boolean.TRUE);
                } else if (arg.startsWith("-D")) {
                    String propLine = arg.length() > 2 ? arg.substring(2) : args[++i];
                    int propDiv = propLine.indexOf('=');
                    if (-1 != propDiv) {
                        cmdLineProps.put(propLine.substring(0, propDiv), propLine.substring(propDiv + 1));
                    }
                } else if (arg.startsWith("-")) {
                    help();
                    return;
                } else {
                    movieLibraryRoot = args[i];
                }
            }
        } catch (Exception error) {
            LOG.error("Wrong arguments specified");
            help();
            return;
        }

        // Save the name of the properties file for use later
        setProperty("userPropertiesName", userPropertiesName);

        LOG.info("Processing started at " + new Date());
        LOG.info("");

        // Load the moviejukebox-default.properties file
        if (!setPropertiesStreamName("./properties/moviejukebox-default.properties", Boolean.TRUE)) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        setPropertiesStreamName(userPropertiesName, Boolean.FALSE);

        // Grab the skin from the command-line properties
        if (cmdLineProps.containsKey(SKIN_DIR)) {
            setProperty(SKIN_DIR, cmdLineProps.get(SKIN_DIR));
        }

        // Load the skin.properties file
        if (!setPropertiesStreamName(getProperty(SKIN_DIR, "./skins/default") + "/skin.properties", Boolean.TRUE)) {
            return;
        }

        // Load the skin-user.properties file (ignore the error)
        setPropertiesStreamName(getProperty(SKIN_DIR, "./skins/default") + "/skin-user.properties", Boolean.FALSE);

        // Load the overlay.properties file (ignore the error)
        String overlayRoot = getProperty("mjb.overlay.dir", Movie.UNKNOWN);
        overlayRoot = (PropertiesUtil.getBooleanProperty("mjb.overlay.skinroot", Boolean.TRUE) ? (getProperty(SKIN_DIR, "./skins/default") + File.separator) : "") + (StringTools.isValidString(overlayRoot) ? (overlayRoot + File.separator) : "");
        setPropertiesStreamName(overlayRoot + "overlay.properties", Boolean.FALSE);

        // Load the apikeys.properties file
        if (!setPropertiesStreamName("./properties/apikeys.properties", Boolean.TRUE)) {
            return;
        } else {
            // This is needed to update the static reference for the API Keys in the pattern formatter
            // because the formatter is initialised before the properties files are read
            FilteringLayout.addApiKeys();
        }

        // Load the rest of the command-line properties
        for (Map.Entry<String, String> propEntry : cmdLineProps.entrySet()) {
            setProperty(propEntry.getKey(), propEntry.getValue());
        }

        // Read the information about the skin
        SkinProperties.readSkinVersion();
        // Display the information about the skin
        SkinProperties.printSkinVersion();

        StringBuilder properties = new StringBuilder("{");
        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
            properties.append(propEntry.getKey());
            properties.append("=");
            properties.append(propEntry.getValue());
            properties.append(",");
        }
        properties.replace(properties.length() - 1, properties.length(), "}");

        // Print out the properties to the log file.
        LOG.debug("Properties: " + properties.toString());

        // Check for mjb.skipIndexGeneration and set as necessary
        // This duplicates the "-i" functionality, but allows you to have it in the property file
        skipIndexGeneration = PropertiesUtil.getBooleanProperty("mjb.skipIndexGeneration", Boolean.FALSE);

        if (PropertiesUtil.getBooleanProperty("mjb.people", Boolean.FALSE)) {
            peopleScan = Boolean.TRUE;
            peopleScrape = PropertiesUtil.getBooleanProperty("mjb.people.scrape", Boolean.TRUE);
            peopleMax = PropertiesUtil.getIntProperty("mjb.people.maxCount", 10);
            popularity = PropertiesUtil.getIntProperty("mjb.people.popularity", 5);

            // Issue 1947: Cast enhancement - option to save all related files to a specific folder
            peopleFolder = PropertiesUtil.getProperty("mjb.people.folder", "");
            if (isNotValidString(peopleFolder)) {
                peopleFolder = "";
            } else if (!peopleFolder.endsWith(File.separator)) {
                peopleFolder += File.separator;
            }
            StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("photo.scanner.photoExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
            while (st.hasMoreTokens()) {
                PHOTO_EXTENSIONS.add(st.nextToken());
            }
        }

        // Check for mjb.skipHtmlGeneration and set as necessary
        // This duplicates the "-h" functionality, but allows you to have it in the property file
        skipHtmlGeneration = PropertiesUtil.getBooleanProperty("mjb.skipHtmlGeneration", Boolean.FALSE);

        // Look for the parameter in the properties file if it's not been set on the command line
        // This way we don't overwrite the setting if it's not found and defaults to FALSE
        showMemory = PropertiesUtil.getBooleanProperty("mjb.showMemory", Boolean.FALSE);

        // This duplicates the "-c" functionality, but allows you to have it in the property file
        jukeboxClean = PropertiesUtil.getBooleanProperty("mjb.jukeboxClean", Boolean.FALSE);

        MovieFilenameScanner.setSkipKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords", ""), ",;| "),
                PropertiesUtil.getBooleanProperty("filename.scanner.skip.caseSensitive", Boolean.TRUE));
        MovieFilenameScanner.setSkipRegexKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords.regex", ""), ","),
                PropertiesUtil.getBooleanProperty("filename.scanner.skip.caseSensitive.regex", Boolean.TRUE));
        MovieFilenameScanner.setExtrasKeywords(tokenizeToArray(getProperty("filename.extras.keywords", "trailer,extra,bonus"), ",;| "));
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray(getProperty("filename.movie.versions.keywords",
                "remastered,directors cut,extended cut,final cut"), ",;|"));
        MovieFilenameScanner.setLanguageDetection(PropertiesUtil.getBooleanProperty("filename.scanner.language.detection", Boolean.TRUE));
        final KeywordMap languages = PropertiesUtil.getKeywordMap("filename.scanner.language.keywords", null);
        if (languages.size() > 0) {
            MovieFilenameScanner.clearLanguages();
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    MovieFilenameScanner.addLanguage(lang, values, values);
                } else {
                    LOG.info("MovieFilenameScanner: No values found for language code " + lang);
                }
            }
        }
        final KeywordMap sourceKeywords = PropertiesUtil.getKeywordMap("filename.scanner.source.keywords",
                "HDTV,PDTV,DVDRip,DVDSCR,DSRip,CAM,R5,LINE,HD2DVD,DVD,DVD5,DVD9,HRHDTV,MVCD,VCD,TS,VHSRip,BluRay,HDDVD,D-THEATER,SDTV");
        MovieFilenameScanner.setSourceKeywords(sourceKeywords.getKeywords(), sourceKeywords);

        String temp = getProperty("sorting.strip.prefixes");
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                Movie.addSortIgnorePrefixes(token.toLowerCase());
            }
        }

        enableWatchScanner = PropertiesUtil.getBooleanProperty("watched.scanner.enable", Boolean.TRUE);
        enableCompleteMovies = PropertiesUtil.getBooleanProperty("complete.movies.enable", Boolean.TRUE);

        // Check to see if don't have a root, check the property file
        if (StringTools.isNotValidString(movieLibraryRoot)) {
            movieLibraryRoot = getProperty("mjb.libraryRoot");
            if (StringTools.isValidString(movieLibraryRoot)) {
                LOG.info("Got libraryRoot from properties file: " + movieLibraryRoot);
            } else {
                LOG.error("No library root found!");
                help();
                return;
            }
        }

        if (jukeboxRoot == null) {
            jukeboxRoot = getProperty("mjb.jukeboxRoot");
            if (jukeboxRoot == null) {
                LOG.info("jukeboxRoot is null in properties file. Please fix this as it may cause errors.");
            } else {
                LOG.info("Got jukeboxRoot from properties file: " + jukeboxRoot);
            }
        }

        File f = new File(movieLibraryRoot);
        if (f.exists() && f.isDirectory() && jukeboxRoot == null) {
            jukeboxRoot = movieLibraryRoot;
        }

        if (movieLibraryRoot == null) {
            help();
            return;
        }

        if (jukeboxRoot == null) {
            LOG.info("Wrong arguments specified: you must define the jukeboxRoot property (-o) !");
            help();
            return;
        }

        if (!f.exists()) {
            LOG.error("Directory or library configuration file '" + movieLibraryRoot + "', not found.");
            return;
        }

        FileTools.initUnsafeChars();
        FileTools.initSubtitleExtensions();

        // make canonical names
        jukeboxRoot = FileTools.getCanonicalPath(jukeboxRoot);
        movieLibraryRoot = FileTools.getCanonicalPath(movieLibraryRoot);
        MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
        if (dumpLibraryStructure) {
            LOG.warn("WARNING !!! A dump of your library directory structure will be generated for debug purpose. !!! Library won't be built or updated");
            ml.makeDumpStructure();
        } else {
            ml.generateLibrary();
        }

        // Now rename the log files
        renameLogFile();

        if (ScanningLimit.isLimitReached()) {
            LOG.warn("Scanning limit of " + ScanningLimit.getLimit() + " was reached, please re-run to complete processing.");
            System.exit(EXIT_SCAN_LIMIT);
        } else {
            System.exit(EXIT_NORMAL);
        }
    }

    /**
     * Append the library filename or the date/time to the log filename
     *
     * @param logFilename
     */
    private static void renameLogFile() {
        StringBuilder newLogFilename = new StringBuilder(LOG_FILENAME);    // Use the base log filename
        boolean renameFile = Boolean.FALSE;

        String libraryName = "_Library";
        if (PropertiesUtil.getBooleanProperty("mjb.appendLibraryToLogFile", Boolean.FALSE)) {
            renameFile = Boolean.TRUE;
            for (final MediaLibraryPath mediaLibrary : mediaLibraryPaths) {
                if (isValidString(mediaLibrary.getDescription())) {
                    libraryName = "_" + mediaLibrary.getDescription();
                    libraryName = FileTools.makeSafeFilename(libraryName);
                    break;
                }
            }

            newLogFilename.append(libraryName);
        }

        if (PropertiesUtil.getBooleanProperty("mjb.appendDateToLogFile", Boolean.FALSE)) {
            renameFile = Boolean.TRUE;
            newLogFilename.append("_").append(JukeboxStatistics.getTime(JukeboxStatistics.JukeboxTimes.START, "yyyy-MM-dd-kkmmss"));
        }

        String logDir = PropertiesUtil.getProperty("mjb.logFileDirectory", "");
        if (StringTools.isValidString(logDir)) {
            renameFile = Boolean.TRUE;
            // Add the file separator if we need to
            logDir += logDir.trim().endsWith(File.separator) ? "" : File.separator;

            newLogFilename.insert(0, logDir);
        }

        if (renameFile) {
            // File (or directory) with old name
            File oldLogFile = new File(LOG_FILENAME + ".log");

            // File with new name
            File newLogFile = new File(newLogFilename.toString() + ".log");

            // Try and create the directory if needed, but don't stop the rename if we can't
            if (StringTools.isValidString(logDir)) {
                FileTools.makeDirsForFile(newLogFile);
            }

            // First we need to tell Log4J to change the name of the current log file to something else so it unlocks the file
            System.setProperty("file.name", PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp") + File.separator + LOG_FILENAME + ".tmp");
            PropertyConfigurator.configure("properties/log4j.properties");

            // Rename file (or directory)
            if (!oldLogFile.renameTo(newLogFile)) {
                System.err.println("Error renaming log file.");
            }

            // Try and rename the ERROR file too.
            oldLogFile = new File(LOG_FILENAME + ".ERROR.log");
            if (oldLogFile.length() > 0) {
                newLogFile = new File(newLogFilename.toString() + ".ERROR.log");
                if (!oldLogFile.renameTo(newLogFile)) {
                    System.err.println("Error renaming ERROR log file.");
                }
            } else {
                if (!oldLogFile.delete()) {
                    System.err.println("Error deleting ERROR log file.");
                }
            }
        }
    }

    private void makeDumpStructure() {
        LOG.debug("Dumping library directory structure for debug");

        for (final MediaLibraryPath mediaLibrary : mediaLibraryPaths) {
            String mediaLibraryRoot = mediaLibrary.getPath();
            LOG.debug("Dumping media library " + mediaLibraryRoot);
            File scanDir = new File(mediaLibraryRoot);
            if (scanDir.isFile()) {
                mediaLibraryRoot = scanDir.getParentFile().getAbsolutePath();
            } else {
                mediaLibraryRoot = scanDir.getAbsolutePath();
            }
            // Create library root dir into dump (keeping full path)

            String libraryRoot = mediaLibraryRoot.replaceAll(":", "_").replaceAll(Pattern.quote(File.separator), "-");
            File libraryRootDump = new File("./dumpDir/" + libraryRoot);
            FileTools.makeDirs(libraryRootDump);
            // libraryRootDump.deleteOnExit();
            dumpDir(new File(mediaLibraryRoot), libraryRootDump);
            LOG.info("Dumping YAMJ root dir");
            // Dump YAMJ root for properties file
            dumpDir(new File("."), libraryRootDump);
            // libraryRootDump.deleteOnExit();
        }
    }

    private static boolean isExcluded(File file) {

        for (String string : EXCLUDED) {
            if (file.getName().endsWith(string)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static void dumpDir(File sourceDir, File destDir) {
        String[] extensionToCopy = {"nfo", "NFO", "properties", "xml", "xsl"};
        LOG.info("Dumping  : " + sourceDir + SPACE_TO_SPACE + destDir);
        File[] files = sourceDir.listFiles();
        for (File file : files) {
            try {
                if (!isExcluded(file)) {
                    String fileName = file.getName();
                    File newFile = new File(destDir.getAbsolutePath() + File.separator + fileName);

                    if (file.isDirectory()) {
                        newFile.mkdir();
                        dumpDir(file, newFile);
                    } else {
                        // Make an empty one.
                        newFile.createNewFile();

                        // Copy NFO / properties / .XML
                        if (ArrayUtils.contains(extensionToCopy, fileName.substring(fileName.length() - 3))) {
                            LOG.info("Coyping " + file + SPACE_TO_SPACE + newFile);
                            FileTools.copyFile(file, newFile);
                        } else {
                            LOG.info("Creating dummy for " + file);
                        }
                    }
                    //newFile.deleteOnExit();
                } else {
                    LOG.debug("Excluding : " + file);
                }
            } catch (IOException e) {
                LOG.error("Dump error : " + e.getMessage());
            }
        }
    }

    private static void help() {
        LOG.info("");
        LOG.info("Usage:");
        LOG.info("");
        LOG.info("Generates an HTML library for your movies library.");
        LOG.info("");
        LOG.info("MovieJukebox libraryRoot [-o jukeboxRoot]");
        LOG.info("");
        LOG.info("  libraryRoot       : OPTIONAL");
        LOG.info("                      This parameter must be specified either on the");
        LOG.info("                      command line or as mjb.libraryRoot in the properties file.");
        LOG.info("                      This parameter can be either: ");
        LOG.info("                      - An existing directory (local or network)");
        LOG.info("                        This is where your movie files are stored.");
        LOG.info("                        In this case -o is optional.");
        LOG.info("");
        LOG.info("                      - An XML configuration file specifying one or");
        LOG.info("                        many directories to be scanned for movies.");
        LOG.info("                        In this case -o option is MANDATORY.");
        LOG.info("                        Please check README.TXT for further information.");
        LOG.info("");
        LOG.info("  -o jukeboxRoot    : OPTIONAL (when not using XML libraries file)");
        LOG.info("                      output directory (local or network directory)");
        LOG.info("                      This is where the jukebox file will be written to");
        LOG.info("                      by default the is the same as the movieLibraryRoot");
        LOG.info("");
        LOG.info("  -c                : OPTIONAL");
        LOG.info("                      Clean the jukebox directory after running.");
        LOG.info("                      This will delete any unused files from the jukebox");
        LOG.info("                      directory at the end of the run.");
        LOG.info("");
        LOG.info("  -k                : OPTIONAL");
        LOG.info("                      Scan the output directory first. Any movies that already");
        LOG.info("                      exist but aren't found in any of the scanned libraries will");
        LOG.info("                      be preserved verbatim.");
        LOG.info("");
        LOG.info("  -i                : OPTIONAL");
        LOG.info("                      Skip the indexing of the library and generation of the");
        LOG.info("                      HTML pages. This should only be used with an external");
        LOG.info("                      front end, such as NMTServer.");
        LOG.info("");
        LOG.info("  -p propertiesFile : OPTIONAL");
        LOG.info("                      The properties file to use instead of moviejukebox.properties");
        LOG.info("");
        LOG.info("  -memory           : OPTIONAL");
        LOG.info("                      Display and log the memory used by moviejukebox");
    }

    public MovieJukebox(String source, String jukeboxRoot) throws Exception {
        this.movieLibraryRoot = source;
        String jukeboxTempLocation = FileTools.getCanonicalPath(PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp"));

        String detailsDirName = getProperty("mjb.detailsDirName", "Jukebox");

        jukebox = new Jukebox(jukeboxRoot, jukeboxTempLocation, detailsDirName);

        MovieJukebox.skipPlaylistGeneration = PropertiesUtil.getBooleanProperty("mjb.skipPlaylistGeneration", Boolean.FALSE);

        MovieJukebox.fanartMovieDownload = PropertiesUtil.getBooleanProperty("fanart.movie.download", Boolean.FALSE);
        MovieJukebox.fanartTvDownload = PropertiesUtil.getBooleanProperty("fanart.tv.download", Boolean.FALSE);

        setIndexFanart = PropertiesUtil.getBooleanProperty("mjb.sets.indexFanart", Boolean.FALSE);

        fanartToken = getProperty("mjb.scanner.fanartToken", ".fanart");
        bannerToken = getProperty("mjb.scanner.bannerToken", ".banner");
        wideBannerToken = getProperty("mjb.scanner.wideBannerToken", ".wide");
        posterToken = getProperty("mjb.scanner.posterToken", "_large");
        thumbnailToken = getProperty("mjb.scanner.thumbnailToken", "_small");
        footerToken = getProperty("mjb.scanner.footerToken", ".footer");

        posterExtension = getProperty("posters.format", EXT_PNG);
        thumbnailExtension = getProperty("thumbnails.format", EXT_PNG);
        bannerExtension = getProperty("banners.format", EXT_PNG);
        fanartExtension = getProperty("fanart.format", "jpg");

        footerCount = PropertiesUtil.getIntProperty("mjb.footer.count", 0);
        for (int i = 0; i < MovieJukebox.footerCount; i++) {
            FOOTER_ENABLE.add(PropertiesUtil.getBooleanProperty("mjb.footer." + i + ".enable", Boolean.FALSE));
            String fName = getProperty("mjb.footer." + i + ".name", "footer." + i);
            FOOTER_NAME.add(fName);
            FOOTER_WIDTH.add(PropertiesUtil.getIntProperty(fName + ".width", 400));
            FOOTER_HEIGHT.add(PropertiesUtil.getIntProperty(fName + ".height", 80));
            FOOTER_EXTENSION.add(getProperty(fName + ".format", EXT_PNG));
        }

        trailersScannerEnable = PropertiesUtil.getBooleanProperty("trailers.scanner.enable", Boolean.TRUE);

        defaultSource = PropertiesUtil.getProperty("filename.scanner.source.default", Movie.UNKNOWN);

        File libraryFile = new File(source);
        if (libraryFile.exists() && libraryFile.isFile() && source.toUpperCase().endsWith("XML")) {
            LOG.debug("Parsing library file : " + source);
            mediaLibraryPaths = MovieJukeboxLibraryReader.parse(libraryFile);
        } else if (libraryFile.exists() && libraryFile.isDirectory()) {
            LOG.debug("Library path is : " + source);
            mediaLibraryPaths = new ArrayList<MediaLibraryPath>();
            MediaLibraryPath mlp = new MediaLibraryPath();
            mlp.setPath(source);
            // We'll get the new playerpath value first, then the nmt path so it overrides the default player path
            String playerRootPath = getProperty("mjb.playerRootPath", "");
            if (StringUtils.isBlank(playerRootPath)) {
                playerRootPath = getProperty("mjb.nmtRootPath", "file:///opt/sybhttpd/localhost.drives/SATA_DISK/Video/");
            }
            mlp.setPlayerRootPath(playerRootPath);
            mlp.setScrapeLibrary(Boolean.TRUE);
            mlp.setExcludes(null);
            mediaLibraryPaths.add(mlp);
        }

        // Check to see if we need to read the jukebox_details.xml file and process, otherwise, just create the file.
        JukeboxProperties.readDetailsFile(jukebox, mediaLibraryPaths);

        // Read these properties after the JukeboxProperties have been read to ensure that changes are picked up
        this.forcePosterOverwrite = PropertiesUtil.getBooleanProperty("mjb.forcePostersOverwrite", Boolean.FALSE);
        this.forceThumbnailOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceThumbnailsOverwrite", Boolean.FALSE);
        this.forceBannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", Boolean.FALSE);
        this.forceSkinOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceSkinOverwrite", Boolean.FALSE);
        this.forceIndexOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceIndexOverwrite", Boolean.FALSE);
        this.forceFooterOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFooterOverwrite", Boolean.FALSE);
    }

    private void generateLibrary() throws Throwable {

        /**
         * ******************************************************************************
         * @author Gabriel Corneanu
         *
         * The tools used for parallel processing are NOT thread safe (some operations are, but not all) therefore all are added to
         * a container which is instantiated one per thread
         *
         * - xmlWriter looks thread safe<br>
         * - htmlWriter was not thread safe<br>
         * - getTransformer is fixed (simple workaround)<br>
         * - MovieImagePlugin : not clear, made thread specific for safety<br>
         * - MediaInfoScanner : not sure, made thread specific
         *
         * Also important: <br>
         * The library itself is not thread safe for modifications (API says so) it could be adjusted with concurrent versions, but
         * it needs many changes it seems that it is safe for subsequent reads (iterators), so leave for now...
         *
         * - DatabasePluginController is also fixed to be thread safe (plugins map for each thread)
         *
         */
        class ToolSet {

            private final MovieImagePlugin imagePlugin = MovieJukebox.getImagePlugin(getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
            private final MovieImagePlugin backgroundPlugin = MovieJukebox.getBackgroundPlugin(getProperty("mjb.background.plugin", "com.moviejukebox.plugin.DefaultBackgroundPlugin"));
            private final MediaInfoScanner miScanner = new MediaInfoScanner();
            private final OpenSubtitlesPlugin subtitlePlugin = new OpenSubtitlesPlugin();
            private final RottenTomatoesPlugin rtPlugin = new RottenTomatoesPlugin();
            private final TrailerScanner trailerScanner = new TrailerScanner();
            // FANART.TV TV Artwork Scanners
            private final ArtworkScanner clearArtScanner = new FanartTvScanner(ArtworkType.CLEARART);
            private final ArtworkScanner clearLogoScanner = new FanartTvScanner(ArtworkType.CLEARLOGO);
            private final ArtworkScanner tvThumbScanner = new FanartTvScanner(ArtworkType.TVTHUMB);
            private final ArtworkScanner seasonThumbScanner = new FanartTvScanner(ArtworkType.SEASONTHUMB);
            // FANART.TV Movie Artwork Scanners
            private final ArtworkScanner movieArtScanner = new FanartTvScanner(ArtworkType.MOVIEART);
            private final ArtworkScanner movieLogoScanner = new FanartTvScanner(ArtworkType.MOVIELOGO);
            private final ArtworkScanner movieDiscScanner = new FanartTvScanner(ArtworkType.MOVIEDISC);
        }

        final ThreadLocal<ToolSet> threadTools = new ThreadLocal<ToolSet>() {
            @Override
            protected ToolSet initialValue() {
                return new ToolSet();
            }
        };

        final MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();
        final MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        File mediaLibraryRoot = new File(movieLibraryRoot);
        final File jukeboxDetailsRootFile = new FileTools.FileEx(jukebox.getJukeboxRootLocationDetails());

        MovieListingPlugin listingPlugin = getListingPlugin(getProperty("mjb.listing.plugin", "com.moviejukebox.plugin.MovieListingPluginBase"));

        videoimageDownload = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
        bannerDownload = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", Boolean.FALSE);
        enableRottenTomatoes = PropertiesUtil.getBooleanProperty("mjb.enableRottenTomatoes", Boolean.FALSE);
        photoDownload = PropertiesUtil.getBooleanProperty("mjb.includePhoto", Boolean.FALSE);
        backdropDownload = PropertiesUtil.getBooleanProperty("mjb.includeBackdrop", Boolean.FALSE);
        boolean processExtras = PropertiesUtil.getBooleanProperty("filename.extras.process", Boolean.TRUE);
        boolean moviejukeboxListing = PropertiesUtil.getBooleanProperty("mjb.listing.generate", Boolean.FALSE);

        // Multi-thread: Processing thread settings
        maxThreadsProcess = Integer.parseInt(getProperty("mjb.MaxThreadsProcess", "0"));
        if (maxThreadsProcess <= 0) {
            maxThreadsProcess = Runtime.getRuntime().availableProcessors();
        }

        maxThreadsDownload = Integer.parseInt(getProperty("mjb.MaxThreadsDownload", "0"));
        if (maxThreadsDownload <= 0) {
            maxThreadsDownload = maxThreadsProcess;
        }

        LOG.info("Using " + maxThreadsProcess + " processing threads and " + maxThreadsDownload + " downloading threads...");
        if (maxThreadsDownload + maxThreadsProcess == 2) {
            // Display the note about the performance, otherwise assume that the user knows how to change
            // these parameters as they aren't set to the minimum
            LOG.info("See README.TXT for increasing performance using these settings.");
        }

        /*
         * ******************************************************************************
         *
         * PART 1 : Preparing the temporary environment
         *
         */
        SystemTools.showMemory();

        LOG.info("Preparing environment...");

        // create the ".mjbignore" and ".no_photo.nmj" file in the jukebox folder
        try {
            FileTools.makeDirs(jukebox.getJukeboxRootLocationDetailsFile());
            new File(jukebox.getJukeboxRootLocationDetailsFile(), ".mjbignore").createNewFile();
            FileTools.addJukeboxFile(".mjbignore");

            if (getBooleanProperty("mjb.nmjCompliant", Boolean.FALSE)) {
                new File(jukebox.getJukeboxRootLocationDetailsFile(), ".no_photo.nmj").createNewFile();
                FileTools.addJukeboxFile(".no_photo.nmj");
            }
        } catch (IOException error) {
            LOG.error("Failed creating jukebox directory. Ensure this directory is read/write!");
            LOG.error(SystemTools.getStackTrace(error));
            return;
        }

        // Delete the existing filecache.txt
        try {
            (new File("filecache.txt")).delete();
        } catch (Exception error) {
            LOG.error("Failed to delete the filecache.txt file.");
            LOG.error(SystemTools.getStackTrace(error));
            return;
        }

        // Save the current state of the preferences to the skin directory for use by the skin
        // The forceHtmlOverwrite is set by the user or by the JukeboxProperties if there has been a skin change
        if (PropertiesUtil.getBooleanProperty("mjb.forceHTMLOverwrite", Boolean.FALSE)
                || !(new File(PropertiesUtil.getPropertiesFilename(Boolean.TRUE))).exists()) {
            PropertiesUtil.writeProperties();
        }

        SystemTools.showMemory();

        LOG.info("Initializing...");
        try {
            FileTools.deleteDir(jukebox.getJukeboxTempLocation());
        } catch (Exception error) {
            LOG.error("Failed deleting the temporary jukebox directory (" + jukebox.getJukeboxTempLocation() + "), please delete this manually and try again");
            return;
        }

        // Try and create the temp directory
        LOG.debug("Creating temporary jukebox location: " + jukebox.getJukeboxTempLocation());
        FileTools.makeDirs(jukebox.getJukeboxTempLocationDetailsFile());

        /*
         * ******************************************************************************
         *
         * PART 2 : Scan movie libraries for files...
         *
         */
        SystemTools.showMemory();

        LOG.info("Scanning library directory " + mediaLibraryRoot);
        LOG.info("Jukebox output goes to " + jukebox.getJukeboxRootLocation());
        if (PropertiesUtil.getBooleanProperty("mjb.dirHash", Boolean.FALSE)) {
            // Add all folders 2 deep to the fileCache
            FileTools.fileCache.addDir(jukeboxDetailsRootFile, 2);
            /*
             * TODO: Need to watch for any issues when we have scanned the whole
             * jukebox, such as the watched folder, NFO folder, etc now existing
             * in the cache
             */
        } else {
            // If the dirHash is not needed, just scan to the root level plus the watched and people folders
            FileTools.fileCache.addDir(jukeboxDetailsRootFile, 0);

            // Add the watched folder
            File watchedFileHandle = new FileTools.FileEx(jukebox.getJukeboxRootLocationDetails() + File.separator + "Watched");
            FileTools.fileCache.addDir(watchedFileHandle, 0);

            // Add the people folder if needed
            if (isValidString(peopleFolder)) {
                File peopleFolderHandle = new FileTools.FileEx(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder);
                FileTools.fileCache.addDir(peopleFolderHandle, 0);
            }
        }

        ThreadExecutor<Void> tasks = new ThreadExecutor<Void>(maxThreadsProcess, maxThreadsDownload);

        final Library library = new Library();
        for (final MediaLibraryPath mediaLibraryPath : mediaLibraryPaths) {
            // Multi-thread parallel processing
            tasks.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    LOG.debug("Scanning media library " + mediaLibraryPath.getPath());
                    MovieDirectoryScanner mds = new MovieDirectoryScanner();
                    // scan uses synchronized method Library.addMovie
                    mds.scan(mediaLibraryPath, library);
                    System.out.print("\n");
                    return null;
                }
            ;
        }
        );
        }
        tasks.waitFor();

        SystemTools.showMemory();

        // If the user asked to preserve the existing movies, scan the output directory as well
        if (isJukeboxPreserve()) {
            LOG.info("Scanning output directory for additional videos");
            OutputDirectoryScanner ods = new OutputDirectoryScanner(jukebox.getJukeboxRootLocationDetails());
            ods.scan(library);
        }

        // Now that everything's been scanned, add all extras to library
        library.mergeExtras();

        LOG.info("Found " + library.size() + " videos in your media library");
        LOG.info("Stored " + FileTools.fileCache.size() + " files in the info cache");

        JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.SCAN_END, System.currentTimeMillis());
        JukeboxStatistics.setStatistic(JukeboxStatistic.VIDEOS, library.size());

        tasks.restart();
        if (library.size() > 0) {
            // Issue 1882: Separate index files for each category
            boolean separateCategories = PropertiesUtil.getBooleanProperty("mjb.separateCategories", Boolean.FALSE);

            LOG.info("Searching for information on the video files...");
            int movieCounter = 0;
            for (final Movie movie : library.values()) {
                // Issue 997: Skip the processing of extras if not required
                if (movie.isExtra() && !processExtras) {
                    continue;
                }

                final int count = ++movieCounter;

                final String movieTitleExt = movie.getOriginalTitle() + (movie.isTVShow() ? (" [Season " + movie.getSeason() + "]") : "")
                        + (movie.isExtra() ? " [Extra]" : "");

                if (movie.isTVShow()) {
                    JukeboxStatistics.increment(JukeboxStatistic.TVSHOWS);
                } else {
                    JukeboxStatistics.increment(JukeboxStatistic.MOVIES);
                }

                // Multi-thread parallel processing
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();

                        // Change the output message depending on the existance of the XML file
                        boolean xmlExists = FileTools.fileCache.fileExists(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), movie.getBaseName()) + EXT_DOT_XML);
                        if (xmlExists) {
                            LOG.info("Checking existing video: " + movieTitleExt);
                            JukeboxStatistics.increment(JukeboxStatistic.EXISTING_VIDEOS);
                        } else {
                            LOG.info("Processing new video: " + movieTitleExt);
                            JukeboxStatistics.increment(JukeboxStatistic.NEW_VIDEOS);
                        }

                        if (ScanningLimit.getToken()) {

                            // First get movie data (title, year, director, genre, etc...)
                            library.toggleDirty(updateMovieData(xmlReader, tools.miScanner, tools.backgroundPlugin, jukebox, movie, library));

                            if (!movie.getMovieType().equals(Movie.REMOVE)) {
                                // Check for watched and unwatched files
                                if (enableWatchScanner) { // Issue 1938
                                    library.toggleDirty(WatchedScanner.checkWatched(jukebox, movie));
                                }

                                // Get subtitle
                                tools.subtitlePlugin.generate(movie);

                                // RottenTomatoes Ratings
                                if (!movie.isTVShow() && enableRottenTomatoes) {
                                    tools.rtPlugin.scan(movie);
                                }

                                // Get Trailers
                                if (trailersScannerEnable) {
                                    tools.trailerScanner.getTrailers(movie);
                                }

                                // Then get this movie's poster
                                LOG.debug("Updating poster for: " + movieTitleExt);
                                updateMoviePoster(jukebox, movie);

                                // Download episode images if required
                                if (videoimageDownload) {
                                    VideoImageScanner.scan(tools.imagePlugin, jukebox, movie);
                                }

                                // Get FANART only if requested
                                // Note that the FanartScanner will check if the file is newer / different
                                if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                                    FanartScanner.scan(tools.backgroundPlugin, jukebox, movie);
                                }

                                // Get BANNER if requested and is a TV show
                                if (bannerDownload && movie.isTVShow()) {
                                    if (!BannerScanner.scan(tools.imagePlugin, jukebox, movie)) {
                                        updateTvBanner(jukebox, movie, tools.imagePlugin);
                                    }
                                }

                                // Get ClearART/LOGOS/etc
                                if (movie.isTVShow()) {
                                    // Only scan using the TV Show artwork scanners
                                    tools.clearArtScanner.scan(jukebox, movie);
                                    tools.clearLogoScanner.scan(jukebox, movie);
                                    tools.tvThumbScanner.scan(jukebox, movie);
                                    tools.seasonThumbScanner.scan(jukebox, movie);
                                } else {
                                    // Only scan using the Movie artwork scanners
                                    tools.movieArtScanner.scan(jukebox, movie);
                                    tools.movieDiscScanner.scan(jukebox, movie);
                                    tools.movieLogoScanner.scan(jukebox, movie);
                                }

                                for (int i = 0; i < footerCount; i++) {
                                    if (FOOTER_ENABLE.get(i)) {
                                        updateFooter(jukebox, movie, tools.imagePlugin, i, forceFooterOverwrite || movie.isDirty());
                                    }
                                }

                                // If we are multipart, we need to make sure all archives have expanded names.
                                if (PropertiesUtil.getBooleanProperty("mjb.scanner.mediainfo.rar.extended.url", Boolean.FALSE)) {

                                    Collection<MovieFile> partsFiles = movie.getFiles();
                                    for (MovieFile mf : partsFiles) {
                                        String filename;

                                        filename = mf.getFile().getAbsolutePath();

                                        // Check the filename is a mediaInfo extension (RAR, ISO) ?
                                        if (tools.miScanner.extendedExtention(filename) == Boolean.TRUE) {

                                            if (mf.getArchiveName() == null) {
                                                LOG.debug("MovieJukebox: Attempting to get archive name for " + filename);
                                                String archive = tools.miScanner.archiveScan(movie, filename);
                                                if (archive != null) {
                                                    LOG.debug("MovieJukebox: Setting archive name to " + archive);
                                                    mf.setArchiveName(archive);
                                                } // got archivename
                                            } // not already set
                                        } // is extension
                                    } // for all files
                                } // property is set
                                if (!movie.isDirty()) {
                                    ScanningLimit.releaseToken();
                                }
                            } else {
                                ScanningLimit.releaseToken();
                                library.remove(movie);
                            }
                            LOG.info("Finished: " + movieTitleExt + " (" + count + "/" + library.size() + ")");
                        } else {
                            movie.setSkipped(true);
                            JukeboxProperties.setScanningLimitReached(Boolean.TRUE);
                            LOG.info("Skipped: " + movieTitleExt + " (" + count + "/" + library.size() + ")");
                        }
                        // Show memory every (processing count) movies
                        if (showMemory && (count % maxThreadsProcess) == 0) {
                            SystemTools.showMemory();
                        }

                        return null;
                    }
                ;
            }
            );
            }
            tasks.waitFor();

            // Add the new extra files (like trailers that were downloaded) to the library and to the corresponding movies
            library.mergeExtras();

            OpenSubtitlesPlugin.logOut();
            AniDbPlugin.anidbClose();

            JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.PROCESSING_END, System.currentTimeMillis());

            if (peopleScan && peopleScrape && !ScanningLimit.isLimitReached()) {
                LOG.info("Searching for people information...");
                int peopleCounter = 0;
                Map<String, Person> popularPeople = new TreeMap<String, Person>();
                for (Movie movie : library.values()) {
                    // Issue 997: Skip the processing of extras if not required
                    if (movie.isExtra() && !processExtras) {
                        continue;
                    }

                    if (popularity > 0) {
                        for (Filmography person : movie.getPeople()) {
                            boolean exists = Boolean.FALSE;
                            String name = person.getName();
                            for (Map.Entry<String, Person> entry : popularPeople.entrySet()) {
                                if (entry.getKey().substring(3).equalsIgnoreCase(name)) {
                                    entry.getValue().addDepartment(person.getDepartment());
                                    entry.getValue().popularityUp(movie);
                                    exists = Boolean.TRUE;
                                }
                            }

                            if (!exists) {
                                Person p = new Person(person);
                                p.addDepartment(p.getDepartment());
                                String key = String.format("%03d", person.getOrder()) + person.getName();
                                popularPeople.put(key, p);
                                popularPeople.get(key).popularityUp(movie);
                            }
                        }
                    } else {
                        peopleCounter += movie.getPeople().size();
                    }
                }

                tasks.restart();
                if (popularity > 0) {
                    List<Person> as = new ArrayList<Person>(popularPeople.values());

                    Collections.sort(as, new PersonComparator());

                    List<Person> stars = new ArrayList<Person>();
                    Iterator<Person> itr = as.iterator();

                    while (itr.hasNext()) {
                        if (peopleCounter >= peopleMax) {
                            break;
                        }

                        Person person = itr.next();

                        if (popularity > person.getPopularity()) {
                            break;
                        }

                        stars.add(person);
                        peopleCounter++;
                    }

                    final int peopleCount = peopleCounter;
                    peopleCounter = 0;
                    for (final Person person : stars) {
                        final int count = ++peopleCounter;
                        final String personName = person.getName();
                        final Person p = new Person(person);

                        // Multi-thread parallel processing
                        tasks.submit(new Callable<Void>() {
                            @Override
                            public Void call() throws FileNotFoundException, XMLStreamException {

                                ToolSet tools = threadTools.get();

                                // Get person data (name, birthday, etc...), download photo
                                updatePersonData(xmlReader, tools.miScanner, tools.backgroundPlugin, jukebox, p, tools.imagePlugin);
                                library.addPerson(p);

                                LOG.info("Finished: " + personName + " (" + count + "/" + peopleCount + ")");

                                // Show memory every (processing count) movies
                                if (showMemory && (count % maxThreadsProcess) == 0) {
                                    SystemTools.showMemory();
                                }

                                return null;
                            }
                        });
                    }
                } else {
                    final int peopleCount = peopleCounter;
                    peopleCounter = 0;
                    for (Movie movie : library.values()) {
                        // Issue 997: Skip the processing of extras if not required
                        if (movie.isExtra() && !processExtras) {
                            continue;
                        }
                        Map<String, Integer> typeCounter = new TreeMap<String, Integer>();
                        for (Filmography person : movie.getPeople()) {
                            final int count = ++peopleCounter;
                            String job = person.getJob();
                            if (!typeCounter.containsKey(job)) {
                                typeCounter.put(job, 1);
                            } else if (typeCounter.get(job) == peopleMax) {
                                continue;
                            } else {
                                typeCounter.put(job, typeCounter.get(job) + 1);
                            }
                            final Person p = new Person(person);
                            final String personName = p.getName();

                            // Multi-thread parallel processing
                            tasks.submit(new Callable<Void>() {
                                @Override
                                public Void call() throws FileNotFoundException, XMLStreamException {

                                    ToolSet tools = threadTools.get();

                                    // Get person data (name, birthday, etc...), download photo and put to library
                                    updatePersonData(xmlReader, tools.miScanner, tools.backgroundPlugin, jukebox, p, tools.imagePlugin);
                                    library.addPerson(p);

                                    LOG.info("Finished: " + personName + " (" + count + "/" + peopleCount + ")");

                                    // Show memory every (processing count) movies
                                    if (showMemory && (count % maxThreadsProcess) == 0) {
                                        SystemTools.showMemory();
                                    }

                                    return null;
                                }
                            });
                        }
                    }
                }
                tasks.waitFor();

                LOG.info("Add/update people information to the videos...");
                boolean dirty;
                for (Movie movie : library.values()) {
                    // Issue 997: Skip the processing of extras if not required
                    if (movie.isExtra() && !processExtras) {
                        continue;
                    }

                    for (Filmography person : movie.getPeople()) {
                        dirty = Boolean.FALSE;
                        for (Person p : library.getPeople()) {
                            if (Filmography.comparePersonName(person, p) || comparePersonId(person, p)) {
                                if (!person.getFilename().equals(p.getFilename()) && isValidString(p.getFilename())) {
                                    person.setFilename(p.getFilename());
                                    dirty = Boolean.TRUE;
                                }
                                if (!person.getUrl().equals(p.getUrl()) && isValidString(p.getUrl())) {
                                    person.setUrl(p.getUrl());
                                    dirty = Boolean.TRUE;
                                }
                                for (Map.Entry<String, String> e : p.getIdMap().entrySet()) {
                                    if (isNotValidString(e.getValue())) {
                                        continue;
                                    }

                                    if (person.getId(e.getKey()).equals(e.getValue())) {
                                        continue;
                                    }

                                    person.setId(e.getKey(), e.getValue());
                                    dirty = Boolean.TRUE;
                                }

                                if (!person.getPhotoFilename().equals(p.getPhotoFilename()) && isValidString(p.getPhotoFilename())) {
                                    person.setPhotoFilename(p.getPhotoFilename());
                                    dirty = Boolean.TRUE;
                                }

                                break;
                            }
                        }

                        if (dirty) {
                            movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                        }
                    }

                    for (Person p : library.getPeople()) {
                        for (Filmography film : p.getFilmography()) {
                            if (Filmography.compareMovieAndFilm(movie, film)) {
                                film.setFilename(movie.getBaseName());
                                film.setTitle(movie.getTitle());
                                if (film.isDirty()) {
                                    p.setDirty();
                                }
                                break;
                            }
                        }
                    }
                }

                for (Person p : library.getPeople()) {
                    for (Filmography film : p.getFilmography()) {
                        if (film.isDirty() || StringTools.isNotValidString(film.getFilename())) {
                            continue;
                        }
                        dirty = Boolean.FALSE;
                        for (Movie movie : library.values()) {
                            if (movie.isExtra() && !processExtras) {
                                continue;
                            }
                            dirty = Filmography.compareMovieAndFilm(movie, film);
                            if (dirty) {
                                break;
                            }
                        }
                        if (!dirty) {
                            film.clearFilename();
                            p.setDirty();
                        }
                    }
                }

                JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.PEOPLE_END, System.currentTimeMillis());
            }

            /*
             * ******************************************************************************
             *
             * PART 3 : Indexing the library
             *
             */
            SystemTools.showMemory();

            // This is for programs like NMTServer where they don't need the indexes.
            if (skipIndexGeneration) {
                LOG.info("Indexing of libraries skipped.");
            } else {
                LOG.info("Indexing libraries...");
                library.buildIndex(tasks);
                JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.INDEXING_END, System.currentTimeMillis());
                SystemTools.showMemory();
            }

            /*
             * ******************************************************************************
             *
             * PART 3B - Indexing masters
             */
            LOG.info("Indexing masters...");
            /*
             * This is kind of a hack -- library.values() are the movies that
             * were found in the library and library.getMoviesList() are the
             * ones that are there now. So the movies that are in getMoviesList
             * but not in values are the index masters.
             */
            List<Movie> indexMasters = new ArrayList<Movie>(library.getMoviesList());
            indexMasters.removeAll(library.values());

            JukeboxStatistics.setStatistic(JukeboxStatistic.SETS, indexMasters.size());

            // Multi-thread: Parallel Executor
            tasks.restart();
            final boolean autoCollection = PropertiesUtil.getBooleanProperty("themoviedb.collection", Boolean.FALSE);
            final TheMovieDbPlugin tmdb = new TheMovieDbPlugin();

            for (final Movie movie : indexMasters) {
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws FileNotFoundException, XMLStreamException {
                        ToolSet tools = threadTools.get();

                        String safeSetMasterBaseName = FileTools.makeSafeFilename(movie.getBaseName());

                        /*
                         * The master's movie XML is used for generating the
                         * playlist it will be overwritten by the index XML
                         */
                        LOG.debug("Updating set artwork for: " + movie.getOriginalTitle() + "...");
                        // If we can find a set artwork file, use it; otherwise, stick with the first movie's artwork
                        String oldArtworkFilename = movie.getPosterFilename();

                        // Set a default poster name in case it's not found during the scan
                        movie.setPosterFilename(safeSetMasterBaseName + "." + posterExtension);
                        if (isNotValidString(PosterScanner.scan(jukebox, movie))) {
                            LOG.debug("Local set poster (" + safeSetMasterBaseName + ") not found.");

                            String collectionId = movie.getId(TheMovieDbPlugin.CACHE_COLLECTION);
                            if (autoCollection && StringUtils.isNumeric(collectionId)) {
                                LOG.debug("MovieDb Collection detected with ID " + collectionId);

                                movie.setPosterURL(tmdb.getCollectionPoster(Integer.parseInt(collectionId)));
                                movie.setFanartURL(tmdb.getCollectionFanart(Integer.parseInt(collectionId)));

                                updateMoviePoster(jukebox, movie);
                            } else {
                                movie.setPosterFilename(oldArtworkFilename);
                            }
                        }

                        // If this is a TV Show and we want to download banners, then also check for a banner Set file
                        if (movie.isTVShow() && bannerDownload) {
                            // Set a default banner filename in case it's not found during the scan
                            movie.setBannerFilename(safeSetMasterBaseName + bannerToken + "." + bannerExtension);
                            movie.setWideBannerFilename(safeSetMasterBaseName + wideBannerToken + "." + bannerExtension);
                            if (!BannerScanner.scan(tools.imagePlugin, jukebox, movie)) {
                                updateTvBanner(jukebox, movie, tools.imagePlugin);
                                LOG.debug("Local set banner (" + safeSetMasterBaseName + bannerToken + ".*) not found.");
                            } else {
                                LOG.debug("Local set banner found, using " + movie.getBannerFilename());
                            }
                        }

                        // Check for Set FANART
                        if (setIndexFanart) {
                            // Set a default fanart filename in case it's not found during the scan
                            movie.setFanartFilename(safeSetMasterBaseName + fanartToken + "." + fanartExtension);
                            if (!FanartScanner.scan(tools.backgroundPlugin, jukebox, movie)) {
                                LOG.debug("Local set fanart (" + safeSetMasterBaseName + fanartToken + ".*) not found.");
                            } else {
                                LOG.debug("Local set fanart found, using " + movie.getFanartFilename());
                            }
                        }

                        StringBuilder artworkFilename = new StringBuilder(safeSetMasterBaseName);
                        artworkFilename.append(thumbnailToken).append(".").append(thumbnailExtension);
                        movie.setThumbnailFilename(artworkFilename.toString());

                        artworkFilename = new StringBuilder(safeSetMasterBaseName);
                        artworkFilename.append(posterToken).append(".").append(posterExtension);
                        movie.setDetailPosterFilename(artworkFilename.toString());

                        // Generate footer filenames
                        for (int inx = 0; inx < footerCount; inx++) {
                            if (FOOTER_ENABLE.get(inx)) {
                                artworkFilename = new StringBuilder(safeSetMasterBaseName);
                                if (FOOTER_NAME.get(inx).contains("[")) {
                                    artworkFilename.append(footerToken).append("_").append(inx);
                                } else {
                                    artworkFilename.append(".").append(FOOTER_NAME.get(inx));
                                }
                                artworkFilename.append(".").append(FOOTER_EXTENSION.get(inx));
                                movie.setFooterFilename(artworkFilename.toString(), inx);
                            }
                        }

                        // No playlist for index masters
                        // htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    }
                ;
            }
            );
            }
            tasks.waitFor();

            // Clear the cache if we've used it
            CacheMemory.clear();
            JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.MASTERS_END, System.currentTimeMillis());
            SystemTools.showMemory();

            // Issue 1886: Html indexes recreated every time
            StringBuilder indexFilename;
            for (Movie setMovie : library.getMoviesList()) {
                if (setMovie.isSetMaster()) {
                    indexFilename = new StringBuilder(jukebox.getJukeboxRootLocationDetails());
                    indexFilename.append(File.separator).append(setMovie.getBaseName()).append(EXT_DOT_XML);
                    File xmlFile = FileTools.fileCache.getFile(indexFilename.toString());
                    if (xmlFile.exists()) {
                        xmlReader.parseSetXML(xmlFile, setMovie, library.getMoviesList());
                    }
                }
            }

            // Issue 1882: Separate index files for each category
            List<String> categoriesList = Arrays.asList(getProperty("mjb.categories.indexList", "Other,Genres,Title,Certification,Year,Library,Set").split(","));

            if (!skipIndexGeneration) {
                LOG.info("Writing Indexes XML...");
                xmlWriter.writeIndexXML(jukebox, library, tasks);

                // Issue 2235: Update artworks after masterSet changed
                ToolSet tools = threadTools.get();
                StringBuilder idxName;
                boolean createPosters = PropertiesUtil.getBooleanProperty("mjb.sets.createPosters", Boolean.FALSE);

                for (IndexInfo idx : library.getGeneratedIndexes()) {
                    if (!idx.canSkip && idx.categoryName.equals(Library.INDEX_SET)) {
                        idxName = new StringBuilder(idx.categoryName);
                        idxName.append("_").append(FileTools.makeSafeFilename(idx.key)).append("_1");

                        for (Movie movie : indexMasters) {
                            if (!movie.getBaseName().equals(idxName.toString())) {
                                continue;
                            }

                            if (createPosters) {
                                // Create/update a detail poster for setMaster
                                LOG.debug("Create/update detail poster for set: " + movie.getBaseName());
                                createPoster(tools.imagePlugin, jukebox, SkinProperties.getSkinHome(), movie, Boolean.TRUE);
                            }

                            // Create/update a thumbnail for setMaster
                            LOG.debug("Create/update thumbnail for set: " + movie.getBaseName() + ", isTV: " + movie.isTVShow() + ", isHD: " + movie.isHD());
                            createThumbnail(tools.imagePlugin, jukebox, SkinProperties.getSkinHome(), movie, Boolean.TRUE);

                            for (int inx = 0; inx < footerCount; inx++) {
                                if (FOOTER_ENABLE.get(inx)) {
                                    LOG.debug("Create/update footer for set: " + movie.getBaseName() + ", footerName: " + FOOTER_NAME.get(inx));
                                    updateFooter(jukebox, movie, tools.imagePlugin, inx, Boolean.TRUE);
                                }
                            }
                        }
                    }
                }

                LOG.info("Writing Category XML...");
                library.setDirty(library.isDirty() || forceIndexOverwrite);
                xmlWriter.writeCategoryXML(jukebox, library, "Categories", library.isDirty());

                // Issue 1882: Separate index files for each category
                if (separateCategories) {
                    for (String categoryName : categoriesList) {
                        xmlWriter.writeCategoryXML(jukebox, library, categoryName, library.isDirty());
                    }
                }
            }

            SystemTools.showMemory();

            LOG.info("Writing Library data...");
            // Multi-thread: Parallel Executor
            tasks.restart();

            int totalCount = library.values().size();
            int currentCount = 1;

            for (final Movie movie : library.values()) {
                System.out.print("\r    Processing library #" + currentCount++ + "/" + totalCount);

                // Issue 997: Skip the processing of extras if not required
                if (movie.isExtra() && !processExtras) {
                    continue;
                }

                if (movie.isSkipped()) {
                    continue;
                }

                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws FileNotFoundException, XMLStreamException {
                        ToolSet tools = threadTools.get();
                        // Update movie XML files with computed index information
                        LOG.debug("Writing index data to movie: " + movie.getBaseName());
                        xmlWriter.writeMovieXML(jukebox, movie, library);

                        // Create a detail poster for each movie
                        LOG.debug("Creating detail poster for movie: " + movie.getBaseName());
                        createPoster(tools.imagePlugin, jukebox, SkinProperties.getSkinHome(), movie, forcePosterOverwrite);

                        // Create a thumbnail for each movie
                        LOG.debug("Creating thumbnails for movie: " + movie.getBaseName());
                        createThumbnail(tools.imagePlugin, jukebox, SkinProperties.getSkinHome(), movie, forceThumbnailOverwrite);

                        if (!skipIndexGeneration && !skipHtmlGeneration) {
                            // write the movie details HTML
                            LOG.debug("Writing detail HTML to movie: " + movie.getBaseName());
                            htmlWriter.generateMovieDetailsHTML(jukebox, movie);

                            // write the playlist for the movie if needed
                            if (!skipPlaylistGeneration) {
                                FileTools.addJukeboxFiles(htmlWriter.generatePlaylist(jukebox, movie));
                            }
                        }
                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    }
                ;
            }
            );
            }
            tasks.waitFor();
            System.out.print("\n");

            SystemTools.showMemory();
            JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.WRITE_INDEX_END, System.currentTimeMillis());

            if (peopleScan) {
                LOG.info("Writing people data...");
                // Multi-thread: Parallel Executor
                tasks.restart();

                totalCount = library.getPeople().size();
                currentCount = 1;
                for (final Person person : library.getPeople()) {
                    // Multi-tread: Start Parallel Processing
                    System.out.print("\r    Processing person #" + currentCount++ + "/" + totalCount);
                    tasks.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws FileNotFoundException, XMLStreamException {
                            // ToolSet tools = threadTools.get();
                            // Update person XML files with computed index information
                            LOG.debug("Writing index data to person: " + person.getName());
                            xmlWriter.writePersonXML(jukebox, person, library);

                            if (!skipIndexGeneration && !skipHtmlGeneration) {
                                // write the person details HTML
                                htmlWriter.generatePersonDetailsHTML(jukebox, person);
                            }

                            return null;
                        }
                    ;
                }
                );
                }
                tasks.waitFor();
                System.out.print("\n");

                SystemTools.showMemory();
                JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.WRITE_PEOPLE_END, System.currentTimeMillis());
            }

            if (!skipIndexGeneration) {
                if (!skipHtmlGeneration) {
                    LOG.info("Writing Indexes HTML...");
                    LOG.info("  Video indexes...");
                    htmlWriter.generateMoviesIndexHTML(jukebox, library, tasks);
                    LOG.info("  Category indexes...");
                    htmlWriter.generateMoviesCategoryHTML(jukebox, library, "Categories", "categories.xsl", library.isDirty());

                    // Issue 1882: Separate index files for each category
                    if (separateCategories) {
                        LOG.info("  Separate category indexes...");
                        for (String categoryName : categoriesList) {
                            htmlWriter.generateMoviesCategoryHTML(jukebox, library, categoryName, "category.xsl", library.isDirty());
                        }
                    }
                }

                /*
                 * Generate the index file.
                 *
                 * Do not skip this part as it's the index that starts the jukebox
                 */
                htmlWriter.generateMainIndexHTML(jukebox, library);
                JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.WRITE_HTML_END, System.currentTimeMillis());

                /*
                 Generate extra pages if required
                 */
                String pageList = PropertiesUtil.getProperty("mjb.customPages", "");
                if (StringUtils.isNotBlank(pageList)) {
                    List<String> newPages = new ArrayList<String>(Arrays.asList(pageList.split(",")));
                    for (String page : newPages) {
                        LOG.info("Transforming skin custom page '" + page + "'");
                        htmlWriter.transformXmlFile(jukebox, page);
                    }
                }
            }

            if (enableCompleteMovies) {
                CompleteMoviesWriter.generate(library, jukebox);
            }

            /**
             * ******************************************************************************
             *
             * PART 4 : Copy files to target directory
             *
             */
            SystemTools.showMemory();

            LOG.info("Copying new files to Jukebox directory...");
            String index = getProperty("mjb.indexFile", "index.htm");

            FileTools.copyDir(jukebox.getJukeboxTempLocationDetails(), jukebox.getJukeboxRootLocationDetails(), Boolean.TRUE);
            FileTools.copyFile(new File(jukebox.getJukeboxTempLocation() + File.separator + index), new File(jukebox.getJukeboxRootLocation() + File.separator + index));

            String skinDate = jukebox.getJukeboxRootLocationDetails() + File.separator + "pictures" + File.separator + "skin.date";
            File skinFile = new File(skinDate);
            File propFile = new File(userPropertiesName);

            // Only check the property file date if the jukebox properties are not being monitored.
            boolean copySkin = JukeboxProperties.isMonitor() ? Boolean.FALSE : FileTools.isNewer(propFile, skinFile);

            // If forceSkinOverwrite is set, the skin file doesn't exist, the user properties file doesn't exist or is newer than the skin.date file
            if (forceSkinOverwrite
                    || !skinFile.exists()
                    || !propFile.exists()
                    || (SkinProperties.getFileDate() > skinFile.lastModified())
                    || copySkin) {
                if (forceSkinOverwrite) {
                    LOG.info("Copying skin files to Jukebox directory (forceSkinOverwrite)...");
                } else if (SkinProperties.getFileDate() > skinFile.lastModified()) {
                    LOG.info("Copying skin files to Jukebox directory (Skin is newer)...");
                } else if (!propFile.exists()) {
                    LOG.info("Copying skin files to Jukebox directory (No property file)...");
                } else if (FileTools.isNewer(propFile, skinFile)) {
                    LOG.info("Copying skin files to Jukebox directory (" + propFile.getName() + " is newer)...");
                } else {
                    LOG.info("Copying skin files to Jukebox directory...");
                }

                StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.skin.copyDirs", "html"), " ,;|");

                while (st.hasMoreTokens()) {
                    String skinDirName = st.nextToken();
                    String skinDirFull = StringTools.appendToPath(SkinProperties.getSkinHome(), skinDirName);

                    if ((new File(skinDirFull).exists())) {
                        LOG.info("Copying the " + skinDirName + " directory...");
                        FileTools.copyDir(skinDirFull, jukebox.getJukeboxRootLocationDetails(), Boolean.TRUE);
                    }
                }

                if (skinFile.exists()) {
                    skinFile.setLastModified(JukeboxStatistics.getTime(JukeboxStatistics.JukeboxTimes.START));
                } else {
                    FileTools.makeDirsForFile(skinFile);
                    skinFile.createNewFile();
                }
            } else {
                LOG.info("Skin copying skipped.");
                LOG.debug("Use mjb.forceSkinOverwrite=true to force the overwitting of the skin files");
            }

            FileTools.fileCache.saveFileList("filecache.txt");
            JukeboxStatistics.setJukeboxTime(JukeboxStatistics.JukeboxTimes.COPYING_END, System.currentTimeMillis());

            /**
             * ******************************************************************************
             *
             * PART 5: Clean-up the jukebox directory
             *
             */
            SystemTools.showMemory();

            // Clean the jukebox folder of unneeded files
            cleanJukeboxFolder();

            if (moviejukeboxListing) {
                LOG.info("Generating listing output...");
                listingPlugin.generate(jukebox, library);
            }

            LOG.info("Clean up temporary files");
            File rootIndex = new File(appendToPath(jukebox.getJukeboxTempLocation(), index));
            rootIndex.delete();

            FileTools.deleteDir(jukebox.getJukeboxTempLocation());

            // clean up extracted attachments
            AttachmentScanner.cleanUp();
        }

        // Set the end time
        JukeboxStatistics.setTimeEnd(System.currentTimeMillis());

        // Write the jukebox details file at the END of the run (Issue 1830)
        JukeboxProperties.writeFile(jukebox, library, mediaLibraryPaths);

        // Output the statistics
        JukeboxStatistics.writeFile(jukebox, library, mediaLibraryPaths);

        LOG.info("");
        LOG.info("MovieJukebox process completed at " + new Date());
        LOG.info("Processing took " + JukeboxStatistics.getProcessingTime());
    }

    private boolean comparePersonId(Filmography aPerson, Filmography bPerson) {
        String aValue, bValue;
        for (Map.Entry<String, String> e : aPerson.getIdMap().entrySet()) {
            aValue = e.getValue();
            if (StringTools.isNotValidString(aValue)) {
                continue;
            }
            bValue = bPerson.getId(e.getKey());
            if (StringTools.isValidString(bValue) && aValue.equals(bValue)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Clean up the jukebox folder of any extra files that are not needed.
     *
     * If the jukeboxClean parameter is not set, just report on the files that would be cleaned.
     */
    private void cleanJukeboxFolder() {
        boolean cleanReport = PropertiesUtil.getBooleanProperty("mjb.jukeboxCleanReport", Boolean.FALSE);

        if (jukeboxClean) {
            if (ScanningLimit.isLimitReached()) {
                LOG.info("Jukebox cleaning skipped as movie limit was reached");
                return;
            } else {
                LOG.info("Cleaning up the jukebox directory...");
            }
        } else if (cleanReport) {
            LOG.info("Jukebox cleaning skipped, the following files are orphaned (not used anymore):");
        } else {
            LOG.info("Jukebox cleaning skipped.");
            return;
        }

        Collection<String> generatedFileNames = FileTools.getJukeboxFiles();

        File[] cleanList = jukebox.getJukeboxRootLocationDetailsFile().listFiles();
        int cleanDeletedTotal = 0;
        boolean skip;

        String skipPattStr = getProperty("mjb.clean.skip");
        Pattern skipPatt;

        if (StringTools.isValidString(skipPattStr)) {
            // Try and convert the string into a pattern
            try {
                skipPatt = Pattern.compile(skipPattStr, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                LOG.warn("Error converting mjb.clean.skip '" + skipPattStr + "'");
                LOG.warn(ex.getMessage());
                skipPatt = null;
            }
        } else {
            skipPatt = null;
        }
        for (File cleanList1 : cleanList) {
            // Scan each file in here
            if (cleanList1.isFile() && !generatedFileNames.contains(cleanList1.getName())) {
                skip = Boolean.FALSE;
                // If the file is in the skin's exclusion regex, skip it
                if (skipPatt != null) {
                    skip = skipPatt.matcher(cleanList1.getName()).matches();
                }
                // If the file isn't skipped and it's not part of the library, delete it
                if (!skip) {
                    if (jukeboxClean) {
                        LOG.debug("Deleted: " + cleanList1.getName() + " from library");
                        cleanList1.delete();
                    } else {
                        LOG.debug("Unused: " + cleanList1.getName());
                    }
                    cleanDeletedTotal++;
                }
            }
        }

        LOG.info(Integer.toString(cleanList.length) + " files in the jukebox directory");
        if (cleanDeletedTotal > 0) {
            if (jukeboxClean) {
                LOG.info("Deleted " + Integer.toString(cleanDeletedTotal) + " unused " + (cleanDeletedTotal == 1 ? "file" : "files") + " from the jukebox directory");
            } else {
                LOG.info("There " + (cleanDeletedTotal == 1 ? "is " : "are ") + Integer.toString(cleanDeletedTotal) + " orphaned " + (cleanDeletedTotal == 1 ? "file" : "files") + " in the jukebox directory");
            }
        }
    }

    /**
     * Generates a movie XML file which contains data in the <tt>Movie</tt>
     * bean.
     *
     * When an XML file exists for the specified movie file, it is loaded into the specified <tt>Movie</tt> object.
     *
     * When no XML file exist, scanners are called in turn, in order to add information to the specified <tt>movie</tt> object. Once
     * scanned, the
     * <tt>movie</tt> object is persisted.
     *
     * @param xmlReader
     * @param miScanner
     * @param backgroundPlugin
     * @param movie
     * @param jukebox
     * @param library
     * @return
     * @throws java.io.FileNotFoundException
     * @throws javax.xml.stream.XMLStreamException
     */
    public boolean updateMovieData(MovieJukeboxXMLReader xmlReader, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie, Library library) throws FileNotFoundException, XMLStreamException {
        boolean forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", Boolean.FALSE);
        boolean checkNewer = PropertiesUtil.getBooleanProperty("filename.nfo.checknewer", Boolean.TRUE);

        /*
         * For each video in the library, if an XML file for this video already
         * exists, then there is no need to search for the video file
         * information, just parse the XML data.
         */
        String safeBaseName = movie.getBaseName();
        File xmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + safeBaseName + EXT_DOT_XML);

        // See if we can find the NFO associated with this video file.
        List<File> nfoFiles = MovieNFOScanner.locateNFOs(movie);

        // Only check the NFO files if the XML exists and the CheckNewer parameter is set
        if (checkNewer && xmlFile.exists()) {
            for (File nfoFile : nfoFiles) {
                // Only re-scan the nfo files if one of them is newer
                if (FileTools.isNewer(nfoFile, xmlFile)) {
                    LOG.info("NFO for " + movie.getOriginalTitle() + " (" + nfoFile.getAbsolutePath() + ") has changed, will rescan file.");
                    movie.setDirty(DirtyFlag.NFO, Boolean.TRUE);
                    movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                    movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
                    movie.setDirty(DirtyFlag.FANART, Boolean.TRUE);
                    movie.setDirty(DirtyFlag.BANNER, Boolean.TRUE);
                    forceXMLOverwrite = Boolean.TRUE;
                    break; // one is enough
                }
            }
        }

        Collection<MovieFile> scannedFiles = Collections.emptyList();
        // Only parse the XML file if we mean to update the XML file.
        if (xmlFile.exists() && !forceXMLOverwrite) {
            // Parse the XML file
            LOG.debug("XML file found for " + movie.getBaseName());
            // Copy scanned files BEFORE parsing the existing XML
            scannedFiles = new ArrayList<MovieFile>(movie.getMovieFiles());

            xmlReader.parseMovieXML(xmlFile, movie);

            // Issue 1886: HTML indexes recreated every time
            // after remove NFO set data restoring from XML - compare NFO and XML sets
            Movie movieNFO = new Movie();
            for (String set : movie.getSetsKeys()) {
                movieNFO.addSet(set);
            }

            MovieNFOScanner.scan(movieNFO, nfoFiles);
            if (!Arrays.equals(movieNFO.getSetsKeys().toArray(), movie.getSetsKeys().toArray())) {
                movie.setSets(movieNFO.getSets());
                movie.setDirty(DirtyFlag.NFO, Boolean.TRUE);
            }

            // If we are overwiting the indexes, we need to check for an update to the library description
            if (forceIndexOverwrite) {
                for (MediaLibraryPath mlp : mediaLibraryPaths) {
                    // Check to see if the paths match and then update the description and quit
                    String mlpPath = mlp.getPath().concat(File.separator);
                    if (movie.getFile().getAbsolutePath().startsWith(mlpPath) && !movie.getLibraryDescription().equals(mlp.getDescription())) {
                        LOG.debug("Changing libray description for video '" + movie.getTitle() + "' from '" + movie.getLibraryDescription() + "' to '" + mlp.getDescription() + "'");
                        library.addDirtyLibrary(movie.getLibraryDescription());
                        movie.setLibraryDescription(mlp.getDescription());
                        movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                        break;
                    }
                }
            }

            // Check to see if the video file needs a recheck
            if (RecheckScanner.scan(movie)) {
                LOG.info("Recheck of " + movie.getBaseName() + " required");
                forceXMLOverwrite = Boolean.TRUE;
                // Don't think we need the DIRTY_INFO with the RECHECK, so long as it is checked for specifically
                //movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                movie.setDirty(DirtyFlag.RECHECK, Boolean.TRUE);
            }

            if (AttachmentScanner.rescan(movie, xmlFile)) {
                forceXMLOverwrite = Boolean.TRUE;
                // TODO Need for new dirty flag ATTACHMENT?
            }

            if (peopleScan && movie.getPeople().isEmpty() && (movie.getCast().size() + movie.getWriters().size() + movie.getDirectors().size()) > 0) {
                forceXMLOverwrite = Boolean.TRUE;
                movie.clearWriters();
                movie.clearDirectors();
                movie.clearCast();
            }
        }

        // ForceBannerOverwrite is set here to force the re-load of TV Show data including the banners
        if (xmlFile.exists() && !forceXMLOverwrite && !(movie.isTVShow() && forceBannerOverwrite)) {
            FileLocationChange.process(movie, library, jukeboxPreserve, scannedFiles, mediaLibraryPaths);

            // update mediainfo values
            miScanner.update(movie);

            // update new episodes titles if new MovieFiles were added
            DatabasePluginController.scanTVShowTitles(movie);

            // Update thumbnails format if needed
            movie.setThumbnailFilename(movie.getBaseName() + thumbnailToken + "." + thumbnailExtension);

            // Update poster format if needed
            movie.setDetailPosterFilename(movie.getBaseName() + posterToken + "." + posterExtension);

            // Check for local CoverArt
            PosterScanner.scan(jukebox, movie);

            // If we don't have a local poster, look online
            // And even though we do "recheck" for a poster URL we should always try and get one
            if (isNotValidString(movie.getPosterURL())) {
                PosterScanner.scan(movie);
            }

        } else {
            // No XML file for this movie.
            // We've got to find movie information where we can (filename, IMDb, NFO, etc...) Add here extra scanners if needed.
            if (forceXMLOverwrite) {
                LOG.debug("Rescanning internet for information on " + movie.getBaseName());
            } else {
                movie.setDirty(DirtyFlag.NEW); // Set a dirty flag so that caller knows we spent time processing the movie
                LOG.debug("Jukebox XML file not found: " + xmlFile.getAbsolutePath());
                LOG.debug("Scanning for information on " + movie.getBaseName());
            }

            // Changing call order, first MediaInfo then NFO. NFO will overwrite any information found by the MediaInfo Scanner.
            miScanner.scan(movie);

            // scan for attachments
            AttachmentScanner.scan(movie);
            // extract attached NFO and add to list of NFO files
            AttachmentScanner.addAttachedNfo(movie, nfoFiles);

            // scan NFO files
            MovieNFOScanner.scan(movie, nfoFiles);

            if (StringTools.isNotValidString(movie.getVideoSource())) {
                movie.setVideoSource(defaultSource, Movie.UNKNOWN);
            }

            // Added forceXMLOverwrite for issue 366
            if (!isValidString(movie.getPosterURL()) || movie.isDirty(DirtyFlag.POSTER)) {
                PosterScanner.scan(jukebox, movie);
            }

            DatabasePluginController.scan(movie);
            // Issue 1323:      Posters not picked up from NFO file
            // Only search for poster if we didn't have already
            if (!isValidString(movie.getPosterURL())) {
                PosterScanner.scan(movie);
            }

            // Removed this extra fanart check.
//            if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
//                if (!isValidString(movie.getFanartURL()) || movie.isDirty(DirtyFlag.FANART)) {
//                    FanartScanner.scan(backgroundPlugin, jukebox, movie);
//                }
//            }
            movie.setCertification(Library.getIndexingCertification(movie.getCertification()), movie.getOverrideSource(OverrideFlag.CERTIFICATION));
        }

        boolean photoFound = Boolean.FALSE;
        for (Filmography person : movie.getPeople()) {
            if (isValidString(person.getPhotoFilename())) {
                continue;
            }
            if (FileTools.findFilenameInCache(person.getName(), PHOTO_EXTENSIONS, jukebox, "MovieJukebox: ", Boolean.TRUE, peopleFolder) != null) {
                person.setPhotoFilename();
                photoFound = Boolean.TRUE;
            }
        }
        if (photoFound) {
            movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
        }

        // Update footer format if needed
        for (int i = 0; i < footerCount; i++) {
            if (FOOTER_ENABLE.get(i)) {
                StringBuilder sb = new StringBuilder(movie.getBaseFilename());
                if (FOOTER_NAME.get(i).contains("[")) {
                    sb.append(footerToken).append(" ").append(i);
                } else {
                    sb.append(".").append(FOOTER_NAME.get(i));
                }
                sb.append(".").append(FOOTER_EXTENSION.get(i));
                movie.setFooterFilename(sb.toString(), i);
            }
        }

        return movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.NFO);
    }

    public void updatePersonData(MovieJukeboxXMLReader xmlReader, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, Jukebox jukebox, Person person, MovieImagePlugin imagePlugin) throws FileNotFoundException, XMLStreamException {
        boolean forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", Boolean.FALSE);
        person.setFilename();
        File xmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + person.getFilename() + EXT_DOT_XML);

        // Change the output message depending on the existance of the XML file
        if (xmlFile.exists()) {
            LOG.info("Checking existing person: " + person.getName());
        } else {
            LOG.info("Processing new person: " + person.getName());
        }

        if (xmlFile.exists() && !forceXMLOverwrite) {
            LOG.debug("XML file found for " + person.getName());
            xmlReader.parsePersonXML(xmlFile, person);
        } else {
            if (forceXMLOverwrite) {
                LOG.debug("Rescanning internet for information on " + person.getName());
            } else {
                LOG.debug("Jukebox XML file not found: " + xmlFile.getAbsolutePath());
                LOG.debug("Scanning for information on " + person.getName());
            }
            DatabasePluginController.scan(person);
        }

        if (photoDownload) {
            PhotoScanner.scan(imagePlugin, jukebox, person);
        }

        if (backdropDownload) {
            BackdropScanner.scan(jukebox, person);
        }
    }

    /**
     * Update the movie poster for the specified movie.
     * <p>
     * When an existing thumbnail is found for the movie, it is not overwritten, unless the mjb.forceThumbnailOverwrite is set to
     * true in the property file.
     * <p>
     * When the specified movie does not contain a valid URL for the poster, a dummy image is used instead.
     *
     * @param jukebox
     * @param movie
     */
    public void updateMoviePoster(Jukebox jukebox, Movie movie) {
        String posterFilename = movie.getPosterFilename();
        String skinHome = SkinProperties.getSkinHome();
        File dummyFile = FileUtils.getFile(skinHome, "resources", DUMMY_JPG);
        File posterFile = new File(FilenameUtils.concat(jukebox.getJukeboxRootLocationDetails(), posterFilename));
        File tmpDestFile = new File(FilenameUtils.concat(jukebox.getJukeboxTempLocationDetails(), posterFilename));

        FileTools.makeDirsForFile(posterFile);
        FileTools.makeDirsForFile(tmpDestFile);

        // Check to see if there is a local poster.
        // Check to see if there are posters in the jukebox directories (target and temp)
        // Check to see if the local poster is newer than either of the jukebox posters
        // Download poster
        // Do not overwrite existing posters, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !posterFile.exists()) || movie.isDirty(DirtyFlag.POSTER) || forcePosterOverwrite) {
            FileTools.makeDirsForFile(posterFile);

            if (!isValidString(movie.getPosterURL())) {
                LOG.debug("Dummy image used for " + movie.getBaseName());
                FileTools.copyFile(dummyFile, tmpDestFile);
            } else {
                try {
                    // Issue 201 : we now download to local temp dir
                    LOG.debug("Downloading poster for " + movie.getBaseName() + " to '" + tmpDestFile.getName() + "'");
                    FileTools.downloadImage(tmpDestFile, movie.getPosterURL());
                    LOG.debug("Downloaded poster for " + movie.getBaseName());
                } catch (IOException error) {
                    LOG.debug("Failed downloading movie poster: " + movie.getPosterURL() + ERROR_TEXT + error.getMessage());
                    FileTools.copyFile(dummyFile, tmpDestFile);
                }
            }
        }
    }

    /**
     * Update the banner for the specified TV Show.
     *
     * When an existing banner is found for the movie, it is not overwritten, unless the mjb.forcePOSTEROverwrite is set to true in
     * the property file.
     *
     * When the specified movie does not contain a valid URL for the banner, a dummy image is used instead.
     *
     * @param jukebox
     * @param movie
     * @param imagePlugin
     */
    public void updateTvBanner(Jukebox jukebox, Movie movie, MovieImagePlugin imagePlugin) {
        String skinHome = SkinProperties.getSkinHome();
        String bannerFilename = movie.getBannerFilename();
        File bannerFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + bannerFilename);
        String tmpDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + bannerFilename;
        File tmpDestFile = new File(tmpDestFilename);
        String origDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + movie.getWideBannerFilename();
        File origDestFile = new File(origDestFilename);

        // Check to see if there is a local banner.
        // Check to see if there are banners in the jukebox directories (target and temp)
        // Check to see if the local banner is newer than either of the jukebox banners
        // Download banner
        // Do not overwrite existing banners, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !bannerFile.exists()) || movie.isDirty(DirtyFlag.BANNER) || forceBannerOverwrite) {
            FileTools.makeDirsForFile(tmpDestFile);

            if (isNotValidString(movie.getBannerURL())) {
                LOG.debug("Dummy banner used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), origDestFile);
            } else {
                try {
                    LOG.debug("Downloading banner for '" + movie.getBaseName() + "' to '" + origDestFile.getName() + "'");
                    FileTools.downloadImage(origDestFile, movie.getBannerURL());
                } catch (IOException error) {
                    LOG.debug("Failed downloading banner: " + movie.getBannerURL() + ERROR_TEXT + error.getMessage());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), origDestFile);
                }
            }

            try {
                BufferedImage bannerImage = GraphicTools.loadJPEGImage(origDestFile);
                if (bannerImage != null) {
                    bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                    GraphicTools.saveImageToDisk(bannerImage, tmpDestFilename);
                }
            } catch (ImageReadException ex) {
                LOG.debug("MovieJukebox: Failed read banner: " + origDestFilename + ERROR_TEXT + ex.getMessage());
            } catch (IOException ex) {
                LOG.debug("MovieJukebox: Failed generate banner: " + tmpDestFilename + ERROR_TEXT + ex.getMessage());
            }
        }
    }

    public void updateFooter(Jukebox jukebox, Movie movie, MovieImagePlugin imagePlugin, Integer inx, boolean forceFooterOverwrite) {
        if (movie.getFooterFilename() == null || movie.getFooterFilename().isEmpty()) {
            LOG.debug("MovieJukebox: Footer update not required for " + movie.getBaseName());
            return;
        }

        String footerFilename = movie.getFooterFilename().get(inx);
        File footerFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + footerFilename);
        String tmpDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + footerFilename;
        File tmpDestFile = new File(tmpDestFilename);

        if (forceFooterOverwrite || (!tmpDestFile.exists() && !footerFile.exists())) {
            FileTools.makeDirsForFile(footerFile);

            BufferedImage footerImage = GraphicTools.createBlankImage(FOOTER_WIDTH.get(inx), FOOTER_HEIGHT.get(inx));
            if (footerImage != null) {
                footerImage = imagePlugin.generate(movie, footerImage, "footer" + FOOTER_NAME.get(inx), null);
                GraphicTools.saveImageToDisk(footerImage, tmpDestFilename);
            }
        }
    }

    public static synchronized MovieImagePlugin getImagePlugin(String className) {
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            return pluginClass.newInstance();
        } catch (InstantiationException ex) {
            LOG.error("Failed instanciating ImagePlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (IllegalAccessException ex) {
            LOG.error("Failed accessing ImagePlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (ClassNotFoundException ex) {
            LOG.error("ImagePlugin class not found: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        }
        LOG.error("Default poster plugin will be used instead.");
        return new DefaultImagePlugin();
    }

    public static MovieImagePlugin getBackgroundPlugin(String className) {
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            return pluginClass.newInstance();
        } catch (InstantiationException ex) {
            LOG.error("Failed instanciating BackgroundPlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (IllegalAccessException ex) {
            LOG.error("Failed accessing BackgroundPlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (ClassNotFoundException ex) {
            LOG.error("BackgroundPlugin class not found: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        }

        LOG.error("Default background plugin will be used instead.");
        return new DefaultBackgroundPlugin();
    }

    public static MovieListingPlugin getListingPlugin(String className) {
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieListingPlugin> pluginClass = cl.loadClass(className).asSubclass(MovieListingPlugin.class);
            return pluginClass.newInstance();
        } catch (InstantiationException ex) {
            LOG.error("Failed instanciating ListingPlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (IllegalAccessException ex) {
            LOG.error("Failed accessing ListingPlugin: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (ClassNotFoundException ex) {
            LOG.error("ListingPlugin class not found: " + className + ERROR_TEXT + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        }

        LOG.error("No listing plugin will be used.");
        return new MovieListingPluginBase();
    } // getListingPlugin()

    /**
     * Create a thumbnail from the original poster file.
     *
     * @param imagePlugin
     * @param skinHome
     * @param jukebox
     * @param movie
     * @param forceThumbnailOverwrite
     */
    public static void createThumbnail(MovieImagePlugin imagePlugin, Jukebox jukebox, String skinHome, Movie movie,
            boolean forceThumbnailOverwrite) {

        // TODO Move all temp directory code to FileTools for a cleaner method
        // Issue 201 : we now download to local temp directory
        String safePosterFilename = movie.getPosterFilename();
        String safeThumbnailFilename = movie.getThumbnailFilename();

        File tmpPosterFile = new File(appendToPath(jukebox.getJukeboxTempLocationDetails(), safePosterFilename));
        File jkbPosterFile = FileTools.fileCache.getFile(appendToPath(jukebox.getJukeboxRootLocationDetails(), safePosterFilename));
        String tmpThumbnailFile = appendToPath(jukebox.getJukeboxTempLocationDetails(), safeThumbnailFilename);
        String jkbThumbnailFile = appendToPath(jukebox.getJukeboxRootLocationDetails(), safeThumbnailFilename);
        File destinationFile;

        if (movie.isDirty(DirtyFlag.POSTER)
                || forceThumbnailOverwrite
                || !FileTools.fileCache.fileExists(jkbThumbnailFile)
                || tmpPosterFile.exists()) {
            // Issue 228: If the PNG files are deleted before running the jukebox this fails.
            // Therefore check to see if they exist in the original directory
            if (tmpPosterFile.exists()) {
                // logger.debug("Use new file: " + tmpPosterFile.getAbsolutePath());
                destinationFile = tmpPosterFile;
            } else {
                // logger.debug("Use jukebox file: " + jkbPosterFile.getAbsolutePath());
                destinationFile = jkbPosterFile;
            }

            BufferedImage bi = null;
            try {
                bi = GraphicTools.loadJPEGImage(destinationFile);
            } catch (IOException ex) {
                LOG.warn("Error reading the thumbnail file: " + destinationFile.getAbsolutePath() + ", error: " + ex.getMessage());
            } catch (ImageReadException ex) {
                LOG.warn("Error processing the thumbnail file: " + destinationFile.getAbsolutePath() + ", error: " + ex.getMessage());
            }

            if (bi == null) {
                LOG.info("Using dummy thumbnail image for " + movie.getBaseName());
                // There was an error with the URL, assume it's a bad URL and clear it so we try again
                movie.setPosterURL(Movie.UNKNOWN);
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + DUMMY_JPG), tmpPosterFile);
                try {
                    bi = GraphicTools.loadJPEGImage(tmpPosterFile);
                } catch (IOException ex) {
                    LOG.warn("Error reading the dummy file: " + tmpPosterFile.getAbsolutePath());
                    LOG.warn("Error: " + ex.getMessage());
                } catch (ImageReadException ex) {
                    LOG.warn("Error reading the dummy image file: " + tmpPosterFile.getAbsolutePath());
                    LOG.warn("Error: " + ex.getMessage());
                }
            }

            // Perspective code.
            String perspectiveDirection = getProperty("thumbnails.perspectiveDirection", RIGHT);

            // Generate and save both images
            if (perspectiveDirection.equalsIgnoreCase("both")) {
                // Calculate mirror thumbnail name.
                String dstMirror = tmpThumbnailFile.substring(0, tmpThumbnailFile.lastIndexOf('.')) + "_mirror" + tmpThumbnailFile.substring(tmpThumbnailFile.lastIndexOf('.'));

                // Generate left & save as copy
                LOG.debug("Generating mirror thumbnail from " + tmpPosterFile + SPACE_TO_SPACE + dstMirror);
                BufferedImage biMirror = imagePlugin.generate(movie, bi, THUMBNAILS, LEFT);
                GraphicTools.saveImageToDisk(biMirror, dstMirror);

                // Generate right as per normal
                LOG.debug("Generating right thumbnail from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
                bi = imagePlugin.generate(movie, bi, THUMBNAILS, RIGHT);
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
            }

            // Only generate the right image
            if (perspectiveDirection.equalsIgnoreCase(RIGHT)) {
                bi = imagePlugin.generate(movie, bi, THUMBNAILS, RIGHT);

                // Save the right perspective image.
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
                LOG.debug("Generating right thumbnail from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
            }

            // Only generate the left image
            if (perspectiveDirection.equalsIgnoreCase(LEFT)) {
                bi = imagePlugin.generate(movie, bi, THUMBNAILS, LEFT);

                // Save the right perspective image.
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
                LOG.debug("Generating left thumbnail from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
            }
        }
    }

    /**
     * Create a detailed poster file from the original poster file
     *
     * @param posterManager
     * @param jukebox
     * @param skinHome
     * @param movie
     * @param forcePosterOverwrite
     */
    public static void createPoster(MovieImagePlugin posterManager, Jukebox jukebox, String skinHome, Movie movie,
            boolean forcePosterOverwrite) {

        // Issue 201 : we now download to local temporary directory
        String safePosterFilename = movie.getPosterFilename();
        String safeDetailPosterFilename = movie.getDetailPosterFilename();
        File tmpPosterFile = new File(appendToPath(jukebox.getJukeboxTempLocationDetails(), safePosterFilename));
        File jkbPosterFile = FileTools.fileCache.getFile(appendToPath(jukebox.getJukeboxRootLocationDetails(), safePosterFilename));
        String tmpThumbnailFile = appendToPath(jukebox.getJukeboxTempLocationDetails(), safeDetailPosterFilename);
        String jkbThumbnailFile = appendToPath(jukebox.getJukeboxRootLocationDetails(), safeDetailPosterFilename);
        File destinationFile;

        if (movie.isDirty(DirtyFlag.POSTER)
                || forcePosterOverwrite
                || !FileTools.fileCache.fileExists(jkbThumbnailFile)
                || tmpPosterFile.exists()) {
            // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
            if (tmpPosterFile.exists()) {
                LOG.debug("CreatePoster: New file exists (" + tmpPosterFile + ")");
                destinationFile = tmpPosterFile;
            } else {
                LOG.debug("CreatePoster: Using old file (" + jkbPosterFile + ")");
                destinationFile = jkbPosterFile;
            }

            BufferedImage bi = null;
            try {
                bi = GraphicTools.loadJPEGImage(destinationFile);
            } catch (IOException ex) {
                LOG.warn("Error processing the poster file: " + destinationFile.getAbsolutePath());
                LOG.error(SystemTools.getStackTrace(ex));
            } catch (ImageReadException ex) {
                LOG.warn("Error reading the poster file: " + destinationFile.getAbsolutePath());
                LOG.error(SystemTools.getStackTrace(ex));
            }

            if (bi == null) {
                // There was an error with the URL, assume it's a bad URL and clear it so we try again
                movie.setPosterURL(Movie.UNKNOWN);
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + DUMMY_JPG), jkbPosterFile);
                try {
                    bi = GraphicTools.loadJPEGImage(tmpPosterFile);
                    LOG.info("Using dummy poster image for " + movie.getOriginalTitle());
                } catch (IOException ex) {
                    LOG.warn("Error processing the dummy poster file: " + tmpPosterFile.getAbsolutePath());
                    LOG.error(SystemTools.getStackTrace(ex));
                } catch (ImageReadException ex) {
                    LOG.warn("Error reading the dummy poster file: " + tmpPosterFile.getAbsolutePath());
                    LOG.error(SystemTools.getStackTrace(ex));
                }
            }
            LOG.debug("Generating poster from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);

            // Perspective code.
            String perspectiveDirection = getProperty("posters.perspectiveDirection", RIGHT);

            // Generate and save both images
            if (perspectiveDirection.equalsIgnoreCase("both")) {
                // Calculate mirror poster name.
                String dstMirror = FilenameUtils.removeExtension(tmpThumbnailFile) + "_mirror." + FilenameUtils.getExtension(tmpThumbnailFile);

                // Generate left & save as copy
                LOG.debug("Generating mirror poster from " + tmpPosterFile + SPACE_TO_SPACE + dstMirror);
                BufferedImage biMirror = posterManager.generate(movie, bi, POSTERS, LEFT);
                GraphicTools.saveImageToDisk(biMirror, dstMirror);

                // Generate right as per normal
                LOG.debug("Generating right poster from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
                bi = posterManager.generate(movie, bi, POSTERS, RIGHT);
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
            }

            // Only generate the right image
            if (perspectiveDirection.equalsIgnoreCase(RIGHT)) {
                bi = posterManager.generate(movie, bi, POSTERS, RIGHT);

                // Save the right perspective image.
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
                LOG.debug("Generating right poster from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
            }

            // Only generate the left image
            if (perspectiveDirection.equalsIgnoreCase(LEFT)) {
                bi = posterManager.generate(movie, bi, POSTERS, LEFT);

                // Save the right perspective image.
                GraphicTools.saveImageToDisk(bi, tmpThumbnailFile);
                LOG.debug("Generating left poster from " + tmpPosterFile + SPACE_TO_SPACE + tmpThumbnailFile);
            }
        }
    }

    public static boolean isJukeboxPreserve() {
        return jukeboxPreserve;
    }

    public static void setJukeboxPreserve(boolean bJukeboxPreserve) {
        jukeboxPreserve = bJukeboxPreserve;
        if (bJukeboxPreserve) {
            LOG.info("Existing jukebox video information will be preserved.");
        }
    }

    @XmlRootElement(name = "jukebox")
    public static class JukeboxXml {

        @XmlElement
        public List<Movie> movies;
    }
}
