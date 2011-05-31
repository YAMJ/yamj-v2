/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static com.moviejukebox.tools.PropertiesUtil.setPropertiesStreamName;
import static com.moviejukebox.tools.PropertiesUtil.setProperty;
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.writer.MovieJukeboxHTMLWriter.getTransformer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.AniDbPlugin;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultImagePlugin;
import com.moviejukebox.plugin.FanartTvPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.MovieListingPlugin;
import com.moviejukebox.plugin.MovieListingPluginBase;
import com.moviejukebox.plugin.OpenSubtitlesPlugin;
import com.moviejukebox.plugin.trailer.AppleTrailersPlugin;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.scanner.OutputDirectoryScanner;
import com.moviejukebox.scanner.WatchedScanner;
import com.moviejukebox.scanner.artwork.BannerScanner;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.scanner.artwork.PhotoScanner;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.scanner.artwork.VideoImageScanner;
import com.moviejukebox.tools.Cache;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.FilteringLayout;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.JukeboxProperties;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.PropertiesUtil.KeywordMap;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class MovieJukebox {

    private static String logFilename = "moviejukebox";
    private static Logger logger = Logger.getLogger(logFilename);    // TODO change all instances to "xxx.class" so we can inherit
    private static Collection<MediaLibraryPath> mediaLibraryPaths;
    private String movieLibraryRoot;
    private String skinHome;
    private static String userPropertiesName = "./moviejukebox.properties";
    
    // Jukebox parameters
    protected static Jukebox jukebox;
    protected static String jukeboxLocation;
    protected static String jukeboxTempLocation;
    private static boolean jukeboxPreserve = false;
    private static boolean jukeboxClean = false;

    // Time Stamps
    private static long timeStart = System.currentTimeMillis();
    private static long timeEnd;

    // Overwrite flags
    private boolean forcePosterOverwrite;
    private boolean forceThumbnailOverwrite;
    private boolean forceBannerOverwrite;
    private boolean forceSkinOverwrite;

    // Scanner Tokens
    private static String posterToken;
    private static String thumbnailToken;
    private static String bannerToken;
    @SuppressWarnings("unused")
    private static String videoimageToken;
    private static String fanartToken;
    
    private static String posterExtension;
    private static String thumbnailExtension;
    private static String bannerExtension;
    private static String fanartExtension;

    private static boolean fanartMovieDownload;
    private static boolean fanartTvDownload;
    private static boolean videoimageDownload;
    private static boolean bannerDownload;
    private static boolean photoDownload;
    private static boolean extraArtworkDownload;    // TODO: Rename this property and split it into clearlogo/clearart/tvthumb/seasonthumb
    private boolean moviejukeboxListing;
    private boolean setIndexFanart;
    private boolean recheckXML;
    private static int recheckCount = 0;
    private static boolean skipIndexGeneration = false;
    private static boolean skipHtmlGeneration = false;
    private static boolean dumpLibraryStructure = false;
    private static boolean showMemory = false;
    private static boolean peopleScan = false;
    private static int peopleMax = 10;
    private static int popularity = 5;

    // These are pulled from the Manifest.MF file that is created by the Ant build script
    public static String mjbVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
    public static String mjbRevision = MovieJukebox.class.getPackage().getImplementationVersion();
    public static String mjbBuildDate = MovieJukebox.class.getPackage().getImplementationTitle();

    private static boolean trailersScannerEnable;
    private static long trailersRescanDaysMillis;

    int MaxThreadsProcess = 1;
    int MaxThreadsDownload = 1;
    
    private static boolean enableWatchScanner;

    public static void main(String[] args) throws Throwable {
        // Create the log file name here, so we can change it later (because it's locked
        System.setProperty("file.name", logFilename);
        PropertyConfigurator.configure("properties/log4j.properties");
        
        // Just create a pretty underline.
        StringBuffer mjbTitle = new StringBuffer();
        
        if (mjbVersion == null) {
            mjbVersion = "";
        }
        
        for (int i = 1; i <= mjbVersion.length(); i++) {
            mjbTitle.append("~");
        }

        logger.info("Yet Another Movie Jukebox " + mjbVersion);
        logger.info("~~~ ~~~~~~~ ~~~~~ ~~~~~~~ " + mjbTitle);
        logger.info("http://code.google.com/p/moviejukebox/");
        logger.info("Copyright (c) 2004-2011 YAMJ Members");
        logger.info("");
        logger.info("This software is licensed under a Creative Commons License");
        logger.info("See this page: http://code.google.com/p/moviejukebox/wiki/License");
        logger.info("");

        // Print the revision information if it was populated by Hudson CI
        if (!((mjbRevision == null) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            if (mjbRevision.equals("0000")) {
                logger.info("  Revision: Custom Build (r" + mjbRevision + ")");
            } else {
                logger.info("  Revision: r" + mjbRevision);
            }
            logger.info("Build Date: " + mjbBuildDate);
            logger.info("");
        }

        String movieLibraryRoot = null;
        String jukeboxRoot = null;
        Map<String, String> cmdLineProps = new LinkedHashMap<String, String>();

        if (args.length == 0) {
            help();
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = (String)args[i];
                if ("-v".equalsIgnoreCase(arg)) {
                    // We've printed the version, so quit now
                    return;
                } else if ("-o".equalsIgnoreCase(arg)) {
                    jukeboxRoot = args[++i];
                } else if ("-c".equalsIgnoreCase(arg)) {
                    jukeboxClean = true;
                } else if ("-k".equalsIgnoreCase(arg)) {
                    setJukeboxPreserve(true);
                } else if ("-p".equalsIgnoreCase(arg)) {
                    userPropertiesName = args[++i];
                } else if ("-i".equalsIgnoreCase(arg)) {
                    skipIndexGeneration = true;
                    PropertiesUtil.setProperty("mjb.skipIndexGeneration", "true");
                } else if ("-h".equalsIgnoreCase(arg)) {
                    skipHtmlGeneration = true;
                    PropertiesUtil.setProperty("mjb.skipHtmlGeneration", "true");
                } else if ("-dump".equalsIgnoreCase(arg)) {
                    dumpLibraryStructure = true;
                } else if ("-memory".equalsIgnoreCase(arg)) {
                    showMemory = true;
                    PropertiesUtil.setProperty("mjb.showMemory", "true");
                } else if (arg.startsWith("-D")) {
                    String propLine = arg.length() > 2 ? new String(arg.substring(2)) : args[++i];
                    int propDiv = propLine.indexOf("=");
                    if (-1 != propDiv) {
                        cmdLineProps.put(new String(propLine.substring(0, propDiv)), new String(propLine.substring(propDiv + 1)));
                    }
                } else if (arg.startsWith("-")) {
                    help();
                    return;
                } else {
                    movieLibraryRoot = args[i];
                }
            }
        } catch (Exception error) {
            logger.error("Wrong arguments specified");
            help();
            return;
        }
        
        // Save the name of the properties file for use later
        setProperty("userPropertiesName", userPropertiesName);        
        
        logger.info("Processing started at " + new Date());
        logger.info("");

        // Load the moviejukebox-default.properties file
        if (!setPropertiesStreamName("./properties/moviejukebox-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        setPropertiesStreamName(userPropertiesName);

        // Grab the skin from the command-line properties
        if (cmdLineProps.containsKey("mjb.skin.dir")) {
            setProperty("mjb.skin.dir", cmdLineProps.get("mjb.skin.dir"));
        }

        // Load the skin.properties file
        if (!setPropertiesStreamName(getProperty("mjb.skin.dir", "./skins/default") + "/skin.properties")) {
            return;
        }

        // Load the skin-user.properties file (ignore the error)
        setPropertiesStreamName(getProperty("mjb.skin.dir", "./skins/default") + "/skin-user.properties");

        // Load the apikeys.properties file
        if (!setPropertiesStreamName("./properties/apikeys.properties")) {
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

        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
            sb.append(propEntry.getKey() + "=" + propEntry.getValue() + ",");
        }
        sb.replace(sb.length() - 1, sb.length(), "}");
        logger.debug("Properties: " + sb.toString());

        // Check for mjb.skipIndexGeneration and set as necessary
        // This duplicates the "-i" functionality, but allows you to have it in the property file
        if (PropertiesUtil.getBooleanProperty("mjb.skipIndexGeneration", "false")) {
            skipIndexGeneration = true;
        }
        
        if (PropertiesUtil.getBooleanProperty("mjb.people", "false")) {
            peopleScan = true;
            peopleMax = PropertiesUtil.getIntProperty("mjb.people.maxCount", "10");
            popularity = PropertiesUtil.getIntProperty("mjb.people.popularity", "5");
        }

        // Check for mjb.skipHtmlGeneration and set as necessary
        // This duplicates the "-h" functionality, but allows you to have it in the property file
        if (PropertiesUtil.getBooleanProperty("mjb.skipHtmlGeneration", "false")) {
            skipHtmlGeneration = true;
        }
        
        // Look for the parameter in the properties file if it's not been set on the command line
        // This way we don't overwrite the setting if it's not found and defaults to "false"
        if (PropertiesUtil.getBooleanProperty("mjb.showMemory", "false")) {
            showMemory = true;
        }
        
        // This duplicates the "-c" functionality, but allows you to have it in the property file
        if (PropertiesUtil.getBooleanProperty("mjb.jukeboxClean", "false")) {
            jukeboxClean = true;
        }
        
        MovieFilenameScanner.setSkipKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords", ""), ",;| "),
                        PropertiesUtil.getBooleanProperty("filename.scanner.skip.caseSensitive", "true"));
        MovieFilenameScanner.setSkipRegexKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords.regex", ""), ","),
                        PropertiesUtil.getBooleanProperty("filename.scanner.skip.caseSensitive.regex", "true"));
        MovieFilenameScanner.setExtrasKeywords(tokenizeToArray(getProperty("filename.extras.keywords", "trailer,extra,bonus"), ",;| "));
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray(getProperty("filename.movie.versions.keywords",
                        "remastered,directors cut,extended cut,final cut"), ",;|"));
        MovieFilenameScanner.setLanguageDetection(PropertiesUtil.getBooleanProperty("filename.scanner.language.detection", "true"));
        final KeywordMap languages = PropertiesUtil.getKeywordMap("filename.scanner.language.keywords", null);
        if (languages.size() > 0) {
            MovieFilenameScanner.clearLanguages();
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    MovieFilenameScanner.addLanguage(lang, values, values);
                } else {
                    logger.info("MovieFilenameScanner: No values found for language code " + lang);
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
                    token = new String(token.substring(1, token.length() - 1));
                }
                Movie.addSortIgnorePrefixes(token.toLowerCase());
            }
        }
        
        enableWatchScanner = PropertiesUtil.getBooleanProperty("watched.scanner.enable", "true");

        if (movieLibraryRoot == null) {
            movieLibraryRoot = getProperty("mjb.libraryRoot");
            logger.info("Got libraryRoot from properties file: " + movieLibraryRoot);
        }

        if (jukeboxRoot == null) {
            jukeboxRoot = getProperty("mjb.jukeboxRoot");
            if (jukeboxRoot == null) {
                logger.info("jukeboxRoot is null in properties file. Please fix this as it may cause errors.");
            } else {
                logger.info("Got jukeboxRoot from properties file: " + jukeboxRoot);
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
            System.out.println("Wrong arguments specified: you must define the jukeboxRoot property (-o) !");
            help();
            return;
        }

        if (!f.exists()) {
            logger.error("Directory or library configuration file '" + movieLibraryRoot + "', not found.");
            return;
        }

        // make canonical names
        jukeboxRoot = FileTools.getCanonicalPath(jukeboxRoot);
        movieLibraryRoot = FileTools.getCanonicalPath(movieLibraryRoot); 
        MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
        if (dumpLibraryStructure) {
            logger.warn("WARNING !!! A dump of your library directory structure will be generated for debug purpose. !!! Library won't be built or updated");
            ml.makeDumpStructure();
        } else {
            ml.generateLibrary();
        }

        // Now rename the log files
        renameLogFile();
        
        return;
    }

    /**
     * Append the library filename or the date/time to the log filename
     * @param logFilename
     */
    private static void renameLogFile() {
        StringBuffer newLogFilename = new StringBuffer(logFilename);    // Use the base log filename
        boolean renameFile = false;
        
        String libraryName = "_Library";
        if (PropertiesUtil.getBooleanProperty("mjb.appendLibraryToLogFile", "false")) {
            renameFile = true;
            for (final MediaLibraryPath mediaLibrary : mediaLibraryPaths) {
                if (isValidString(mediaLibrary.getDescription())) {
                    libraryName = "_" + mediaLibrary.getDescription();
                    libraryName = FileTools.makeSafeFilename(libraryName);
                    break;
                }
            }
            
            newLogFilename.append(libraryName);
        }
        
        if (PropertiesUtil.getBooleanProperty("mjb.appendDateToLogFile", "false")) {
            renameFile = true;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kkmmss");
            newLogFilename.append("_").append(dateFormat.format(timeStart));
        }
        
        String logDir = PropertiesUtil.getProperty("mjb.logFileDirectory", "");
        if (StringTools.isValidString(logDir)) {
            renameFile = true;
            // Add the file separator if we need to
            logDir += logDir.trim().endsWith(File.separator)?"":File.separator;
            
            newLogFilename.insert(0, logDir);
        }
        
        if (renameFile) {
            // File (or directory) with old name
            File oldLogFile = new File(logFilename + ".log");

            // File with new name
            File newLogFile = new File(newLogFilename.toString() + ".log");

            // Try and create the directory if needed, but don't stop the rename if we can't
            if (StringTools.isValidString(logDir)) {
                try {
                    newLogFile.getParentFile().mkdirs();
                } catch (Exception error) {
                    // This isn't an important error
                    logger.warn("Error creating log file directory");
                }
            }
            
            // First we need to tell Log4J to change the name of the current log file to something else so it unlocks the file
            System.setProperty("file.name", PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp") + File.separator + logFilename + ".tmp");
            PropertyConfigurator.configure("properties/log4j.properties");

            // Rename file (or directory)
            if (!oldLogFile.renameTo(newLogFile)) {
                System.err.println("Error renaming log file.");
            }
            
            // Try and rename the ERROR file too.
            oldLogFile = new File(logFilename + ".ERROR.log");
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
        
        return;
    }
    
    private void makeDumpStructure() {
        logger.debug("Dumping library directory structure for debug");

        for (final MediaLibraryPath mediaLibrary : mediaLibraryPaths) {
            String mediaLibraryRoot = mediaLibrary.getPath();
            logger.debug("Dumping media library " + mediaLibraryRoot);
            File scan_dir = new File(mediaLibraryRoot);
            if (scan_dir.isFile()) {
                mediaLibraryRoot = scan_dir.getParentFile().getAbsolutePath();
            } else {
                mediaLibraryRoot = scan_dir.getAbsolutePath();
            }
            // Create library root dir into dump (keeping full path)

            String libraryRoot = mediaLibraryRoot.replaceAll(":", "_").replaceAll(Pattern.quote(File.separator), "-");
            File libraryRootDump = new File("./dumpDir/" + libraryRoot);
            libraryRootDump.mkdirs();
            // libraryRootDump.deleteOnExit();
            dumpDir(new File(mediaLibraryRoot), libraryRootDump);
            logger.info("Dumping YAMJ root dir");
            // Dump YAMJ root for properties file
            dumpDir(new File("."), libraryRootDump);
           // libraryRootDump.deleteOnExit();
        }
    }

    private static final String[] excluded = { "dumpDir", ".svn", "src", "test", "bin", "skins" };

    private static boolean isExcluded(File file) {

        for (String string : excluded) {
            if (file.getName().endsWith(string)) {
                return true;
            }
        }
        return false;
    }

    private static void dumpDir(File sourceDir, File destDir) {
        String[] extensionToCopy = { "nfo", "NFO", "properties", "xml", "xsl" };
        logger.info("Dumping  : " + sourceDir + " to " + destDir);
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
                        if (ArrayUtils.contains(extensionToCopy, new String(fileName.substring(fileName.length() - 3)))) {
                            logger.info("Coyping " + file + " to " + newFile);
                            FileTools.copyFile(file, newFile);
                        } else {
                            logger.info("Creating dummy for " + file);
                        }
                    }
                    //newFile.deleteOnExit();
                } else {
                    logger.debug("Excluding : " + file);
                }
            } catch (IOException e) {
                logger.error("Dump error : " + e.getMessage());
            }
        }
    }

    public static String[] tokenizeToArray(String str, String delim) {
        StringTokenizer st = new StringTokenizer(str, delim);
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        return keywords.toArray(new String[] {});
    }

    private static void help() {
        System.out.println("");
        System.out.println("Usage:");
        System.out.println();
        System.out.println("Generates an HTML library for your movies library.");
        System.out.println();
        System.out.println("MovieJukebox libraryRoot [-o jukeboxRoot]");
        System.out.println();
        System.out.println("  libraryRoot       : OPTIONAL");
        System.out.println("                      This parameter must be specified either on the");
        System.out.println("                      command line or as mjb.libraryRoot in the properties file.");
        System.out.println("                      This parameter can be either: ");
        System.out.println("                      - An existing directory (local or network)");
        System.out.println("                        This is where your movie files are stored.");
        System.out.println("                        In this case -o is optional.");
        System.out.println();
        System.out.println("                      - An XML configuration file specifying one or");
        System.out.println("                        many directories to be scanned for movies.");
        System.out.println("                        In this case -o option is MANDATORY.");
        System.out.println("                        Please check README.TXT for further information.");
        System.out.println();
        System.out.println("  -o jukeboxRoot    : OPTIONAL (when not using XML libraries file)");
        System.out.println("                      output directory (local or network directory)");
        System.out.println("                      This is where the jukebox file will be written to");
        System.out.println("                      by default the is the same as the movieLibraryRoot");
        System.out.println();
        System.out.println("  -c                : OPTIONAL");
        System.out.println("                      Clean the jukebox directory after running.");
        System.out.println("                      This will delete any unused files from the jukebox");
        System.out.println("                      directory at the end of the run.");
        System.out.println();
        System.out.println("  -k                : OPTIONAL");
        System.out.println("                      Scan the output directory first. Any movies that already");
        System.out.println("                      exist but aren't found in any of the scanned libraries will");
        System.out.println("                      be preserved verbatim.");
        System.out.println();
        System.out.println("  -i                : OPTIONAL");
        System.out.println("                      Skip the indexing of the library and generation of the");
        System.out.println("                      HTML pages. This should only be used with an external");
        System.out.println("                      front end, such as NMTServer.");
        System.out.println();
        System.out.println("  -p propertiesFile : OPTIONAL");
        System.out.println("                      The properties file to use instead of moviejukebox.properties");
        System.out.println("");
        System.out.println("  -memory           : OPTIONAL");
        System.out.println("                      Display and log the memory used by moviejukebox");
    }

    public MovieJukebox(String source, String jukeboxRoot) throws Exception {
        this.movieLibraryRoot = source;
        String jukeboxTempLocation = FileTools.getCanonicalPath(PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp"));

        String detailsDirName = getProperty("mjb.detailsDirName", "Jukebox");
        
        jukebox = new Jukebox(jukeboxRoot, jukeboxTempLocation, detailsDirName);
        
        this.forcePosterOverwrite = PropertiesUtil.getBooleanProperty("mjb.forcePostersOverwrite", "false");
        this.forceThumbnailOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceThumbnailsOverwrite", "false");
        this.forceBannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", "false");
        this.forceSkinOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceSkinOverwrite", "false");
        this.skinHome = getProperty("mjb.skin.dir", "./skins/default");

        // This is what these properties should look like
        // this.fanartMovieDownload = PropertiesUtil.getBooleanProperty("fanart.movie.download", "false");
        // this.fanartTvDownload = PropertiesUtil.getBooleanProperty("fanart.tv.download", "false");
        fanartMovieDownload = FanartScanner.checkDownloadFanart(false);
        fanartTvDownload = FanartScanner.checkDownloadFanart(true);

        this.setIndexFanart = PropertiesUtil.getBooleanProperty("mjb.sets.indexFanart", "false");
        
        this.recheckXML = PropertiesUtil.getBooleanProperty("mjb.recheck.XML", "true");

        fanartToken = getProperty("mjb.scanner.fanartToken", ".fanart");
        bannerToken = getProperty("mjb.scanner.bannerToken", ".banner");
        posterToken = getProperty("mjb.scanner.posterToken", "_large");
        thumbnailToken = getProperty("mjb.scanner.thumbnailToken", "_small");
        videoimageToken = getProperty("mjb.scanner.videoimageToken", ".videoimage");
        
        posterExtension = getProperty("posters.format", "png");
        thumbnailExtension = getProperty("thumbnails.format", "png");
        bannerExtension = getProperty("banners.format", "jpg");
        fanartExtension = getProperty("fanart.format", "jpg");

        trailersScannerEnable = PropertiesUtil.getBooleanProperty("trailers.scanner.enable", "true");
        try {
            trailersRescanDaysMillis = PropertiesUtil.getIntProperty("trailers.rescan.days", "15");
            // Convert trailers.rescan.days from DAYS to MILLISECONDS for comparison purposes
            trailersRescanDaysMillis *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        } catch (Exception e) {
            logger.error("Error trailers.rescan.days property, should be an integer");
            throw e;
        }

        File f = new File(source);
        if (f.exists() && f.isFile() && source.toUpperCase().endsWith("XML")) {
            logger.debug("Parsing library file : " + source);
            mediaLibraryPaths = parseMovieLibraryRootFile(f);
        } else if (f.exists() && f.isDirectory()) {
            logger.debug("Library path is : " + source);
            mediaLibraryPaths = new ArrayList<MediaLibraryPath>();
            MediaLibraryPath mlp = new MediaLibraryPath();
            mlp.setPath(source);
            // We'll get the new playerpath value first, then the nmt path so it overrides the default player path
            String playerRootPath = getProperty("mjb.playerRootPath", "");
            if (playerRootPath.equals("")) {
                playerRootPath = getProperty("mjb.nmtRootPath", "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/");
            }
            mlp.setPlayerRootPath(playerRootPath);
            mlp.setScrapeLibrary(true);
            mlp.setExcludes(new ArrayList<String>());
            mediaLibraryPaths.add(mlp);
        }
    }

    @XmlRootElement(name = "jukebox")
    public static class JukeboxXml {
        @XmlElement
        public List<Movie> movies;
    }
    
    private void generateLibrary() throws Throwable {

        /********************************************************************************
         * @author Gabriel Corneanu
         * 
         *  The tools used for parallel processing are NOT thread safe (some operations 
         *  are, but not all) therefore all are added to a container which is instantiated 
         *  one per thread
         * 
         *  - xmlWriter looks thread safe 
         *  - htmlWriter was not thread safe, 
         *  - getTransformer is fixed (simple workaround) 
         *  - MovieImagePlugin : not clear, made thread specific for safety 
         *  - MediaInfoScanner : not sure, made thread specific
         * 
         *  Also important: The library itself is not thread safe for modifications 
         *  (API says so) it could be adjusted with concurrent versions, but it needs many 
         *  changes it seems that it is safe for subsequent reads (iterators), so leave for now... 
         *  
         *  - DatabasePluginController is also fixed to be thread safe (plugins map for each thread)
         * 
         */
        class ToolSet {
            public MovieImagePlugin imagePlugin = getImagePlugin(getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
            public MovieImagePlugin backgroundPlugin = getBackgroundPlugin(getProperty("mjb.background.plugin",
                            "com.moviejukebox.plugin.DefaultBackgroundPlugin"));
            public MediaInfoScanner miScanner = new MediaInfoScanner();
            public AppleTrailersPlugin trailerPlugin = new AppleTrailersPlugin();
            public OpenSubtitlesPlugin subtitlePlugin = new OpenSubtitlesPlugin();
            public FanartTvPlugin fanartTvPlugin = new FanartTvPlugin();
        }

        final ThreadLocal<ToolSet> threadTools = new ThreadLocal<ToolSet>() {
            protected ToolSet initialValue() {
                return new ToolSet();
            };
        };

        final MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        File mediaLibraryRoot = new File(movieLibraryRoot);
        final File jukeboxDetailsRootFile = new FileTools.FileEx(jukebox.getJukeboxRootLocationDetails());

        MovieListingPlugin listingPlugin = getListingPlugin(getProperty("mjb.listing.plugin", "com.moviejukebox.plugin.MovieListingPluginBase"));
        this.moviejukeboxListing = PropertiesUtil.getBooleanProperty("mjb.listing.generate", "false");

        videoimageDownload = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
        bannerDownload = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", "false");
        extraArtworkDownload = PropertiesUtil.getBooleanProperty("mjb.includeExtraArtwork", "false");
        photoDownload = PropertiesUtil.getBooleanProperty("mjb.includePhoto", "false");

        boolean processExtras = PropertiesUtil.getBooleanProperty("filename.extras.process","true");

        // Multi-thread: Processing thread settings
        MaxThreadsProcess = Integer.parseInt(getProperty("mjb.MaxThreadsProcess", "0")); 
        if (MaxThreadsProcess <= 0) {
            MaxThreadsProcess = Runtime.getRuntime().availableProcessors();
        }
        
        MaxThreadsDownload = Integer.parseInt(getProperty("mjb.MaxThreadsDownload", "0"));
        if (MaxThreadsDownload <= 0) {
            MaxThreadsDownload = MaxThreadsProcess;
        }
        
        logger.info("Using " + MaxThreadsProcess + " processing threads and " + MaxThreadsDownload + " downloading threads...");
        if (MaxThreadsDownload + MaxThreadsProcess == 2) {
            // Display the note about the performance, otherwise assume that the user knows how to change
            // these parameters as they aren't set to the minimum
            logger.info("See README.TXT for increasing performance using these settings.");
        }
        
        /********************************************************************************
         * 
         * PART 1 : Preparing the temporary environment
         * 
         */
        SystemTools.showMemory();

        logger.info("Preparing environment...");
        
        // Create the ".mjbignore" file in the jukebox folder
        try {
            jukebox.getJukeboxRootLocationDetailsFile().mkdirs();
            new File(jukebox.getJukeboxRootLocationDetailsFile(), ".mjbignore").createNewFile();
            FileTools.addJukeboxFile(".mjbignore");
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error("Failed creating jukebox directory. Ensure this directory is read/write!");
            logger.error(eResult.toString());
            return;
        }
        
        // Delete the existing filecache.txt
        try {
            (new File("filecache.txt")).delete();
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error("Failed to delete the filecache.txt file.");
            logger.debug(eResult.toString());
            return;
        }
        
        // Check to see if we need to read the jukebox_details.xml file and process, otherwise, just create the file.
       JukeboxProperties.readDetailsFile(jukebox, mediaLibraryPaths);

       // Save the current state of the preferences to the skin directory for use by the skin
       // The forceHtmlOverwrite is set by the user or by the JukeboxProperties if there has been a skin change
       
       if (PropertiesUtil.getBooleanProperty("mjb.forceHTMLOverwrite", "false") || 
                       !(new File(PropertiesUtil.getPropertiesFilename(true))).exists() ) {
           PropertiesUtil.writeProperties();
       }

       SystemTools.showMemory();
        
        logger.info("Initializing...");
        try {
            FileTools.deleteDir(jukebox.getJukeboxTempLocation());
        } catch (Exception error) {
            logger.error("Failed deleting the temporary jukebox directory (" + jukebox.getJukeboxTempLocation() + "), please delete this manually and try again");
            return;
        }
        
        // Try and create the temp directory
        logger.debug("Creating temporary jukebox location: " + jukebox.getJukeboxTempLocation());
        boolean status = jukebox.getJukeboxTempLocationDetailsFile().mkdirs();
        int i = 1;
        while (!status && i++ <= 10) {
            Thread.sleep(1000);
            status = jukebox.getJukeboxTempLocationDetailsFile().mkdirs();
        }
        
        if (status && i > 10) {
            logger.error("Failed creating the temporary jukebox directory (" + jukebox.getJukeboxTempLocationDetails() + "). Ensure this directory is read/write!");
            return;
        }

        /********************************************************************************
         * 
         * PART 2 : Scan movie libraries for files...
         * 
         */
        SystemTools.showMemory();
        
        logger.info("Scanning library directory " + mediaLibraryRoot);
        logger.info("Jukebox output goes to " + jukebox.getJukeboxRootLocation());
        FileTools.fileCache.addDir(jukeboxDetailsRootFile, 0);

        ThreadExecutor<Void> tasks = new ThreadExecutor<Void>(MaxThreadsProcess, MaxThreadsDownload);

        final Library library = new Library();
        for (final MediaLibraryPath mediaLibraryPath : mediaLibraryPaths) {
            // Multi-thread parallel processing
            tasks.submit(new Callable<Void>() {
                public Void call() {
                    logger.debug("Scanning media library " + mediaLibraryPath.getPath());
                    MovieDirectoryScanner mds = new MovieDirectoryScanner();
                    // scan uses synchronized method Library.addMovie
                    mds.scan(mediaLibraryPath, library);
                    System.out.print("\n");
                    return null;
                };
            });
        }
        tasks.waitFor();
        
        SystemTools.showMemory();

        // If the user asked to preserve the existing movies, scan the output directory as well
        if (isJukeboxPreserve()) {
            logger.info("Scanning output directory for additional videos");
            OutputDirectoryScanner ods = new OutputDirectoryScanner(jukebox.getJukeboxRootLocationDetails());
            ods.scan(library);
        }

        // Now that everything's been scanned, add all extras to library
        library.mergeExtras();

        logger.info("Found " + library.size() + " videos in your media library");
        logger.info("Stored " + FileTools.fileCache.size() + " files in the info cache");

        tasks.restart();
        if (library.size() > 0) {
            // Issue 1882: Separate index files for each category
            boolean separateCategories = PropertiesUtil.getBooleanProperty("mjb.separateCategories", "false");

            logger.info("Searching for information on the video files...");
            int movieCounter = 0;
            for (final Movie movie : library.values()) {
                // Issue 997: Skip the processing of extras if not required
                if (movie.isExtra() && !processExtras) {
                    continue;
                }

                final int count = ++movieCounter;

                final String movieTitleExt = movie.getOriginalTitle() + (movie.isTVShow() ? (" [Season " + movie.getSeason() + "]") : "")
                                + (movie.isExtra() ? " [Extra]" : "");

                // Multi-thread parallel processing
                tasks.submit(new Callable<Void>() {

                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();
                        
                        // Change the output message depending on the existance of the XML file
                        boolean xmlExists = FileTools.fileCache.fileExists(jukebox.getJukeboxRootLocationDetails() + File.separator + movie.getBaseName() + ".xml");
                        if (xmlExists) {
                            logger.info("Checking existing video: " + movieTitleExt);
                        } else {
                            logger.info("Processing new video: " + movieTitleExt);
                        }

                        // First get movie data (title, year, director, genre, etc...)
                        library.toggleDirty(updateMovieData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukebox, movie));
                        if (!movie.getMovieType().equals(Movie.REMOVE)) {
                            // Then get this movie's poster
                            logger.debug("Updating poster for: " + movieTitleExt);
                            updateMoviePoster(jukebox, movie);
    
                            // Download episode images if required
                            if (videoimageDownload) {
                                VideoImageScanner.scan(tools.imagePlugin, jukebox, movie);
                            }
    
                            // Get Fanart only if requested
                            // Note that the FanartScanner will check if the file is newer / different
                            if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                                FanartScanner.scan(tools.backgroundPlugin, jukebox, movie);
                            }
    
                            // Get Banner if requested and is a TV show
                            if (bannerDownload && movie.isTVShow()) {
                                if (!BannerScanner.scan(tools.imagePlugin, jukebox, movie)) {
                                    updateTvBanner(jukebox, movie);
                                }
                            }
                            
                            // Check for watched and unwatched files
                            if(enableWatchScanner) { // Issue 1938
                                WatchedScanner.checkWatched(jukebox, movie);
                            }
    
                            // Get subtitle
                            tools.subtitlePlugin.generate(movie);
    
                            // Get ClearART/LOGOS/etc
                            if (movie.isTVShow() && extraArtworkDownload) {
                                tools.fanartTvPlugin.scan(movie);
                            }
                            
                            // Get Trailers
                            if (movie.canHaveTrailers() && trailersScannerEnable && isTrailersNeedRescan(movie)) {
                                boolean status = tools.trailerPlugin.generate(movie);
    
                                // Update trailerExchange
                                if (status == false) {
                                    // Set trailerExchange to true if trailersRescanDaysMillis is < 0 (disable)
                                    status = trailersRescanDaysMillis < 0 ? true : false;
                                }
                                movie.setTrailerExchange(status);
                            } 
                        } else {
                            library.remove(movie);
                        }
                        logger.info("Finished: " + movieTitleExt + " (" + count + "/" + library.size() + ")");
                        // Show memory every (processing count) movies
                        if (showMemory && (count % MaxThreadsProcess) == 0) {
                            SystemTools.showMemory();
                        }
                        
                        return null;
                    };
                });
            }
            tasks.waitFor();

            // Add the new extra files (like trailers that were downloaded) to the library and to the corresponding movies
            library.mergeExtras();

            OpenSubtitlesPlugin.logOut();
            AniDbPlugin.anidbClose();

            if (peopleScan) {
                logger.info("Searching for people information...");
                int peopleCounter = 0;
                TreeMap<String, Person> popularPeople = new TreeMap<String, Person>();
                for (Movie movie : library.values()) {
                    // Issue 997: Skip the processing of extras if not required
                    if (movie.isExtra() && !processExtras) {
                        continue;
                    }
                    if (popularity > 0) {
                        for (Filmography person : movie.getPeople()) {
                            boolean exists = false;
                            String name = person.getName();
                            for (String key : popularPeople.keySet()) {
                                if (key.substring(3).equalsIgnoreCase(name)) {
                                    popularPeople.get(key).popularityUp();
                                    exists = true;
                                }
                            }
                            if (!exists) {
                                Person p = new Person(person);
                                popularPeople.put(String.format("%03d", person.getOrder()) + person.getName(), p);
                            }
                        }
                    } else {
                        peopleCounter += movie.getPeople().size();
                    }
                }

                tasks.restart();
                if (popularity > 0) {
                    ArrayList as = new ArrayList(popularPeople.entrySet());

                    Collections.sort(as, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            Map.Entry e1 = (Map.Entry)o1;
                            Map.Entry e2 = (Map.Entry)o2;
                            Person first = (Person)e1.getValue();
                            Person second = (Person)e2.getValue();
                            return second.getPopularity().compareTo(first.getPopularity());
                        }
                    });

                    List<Person> stars = new ArrayList<Person>();
                    Iterator itr = as.iterator();
                    while (itr.hasNext()) {
                        if (peopleCounter >= peopleMax) {
                            break;
                        }
                        Map.Entry e = (Map.Entry)itr.next();
                        Person p = (Person)e.getValue();
                        if (popularity > p.getPopularity()) {
                            break;
                        }
                        stars.add(p);
                        peopleCounter++;
                    }
                    final int peopleCount = peopleCounter;
                    peopleCounter = 0;
                    for (final Person person : stars) {
                        final int count = ++peopleCounter;

                        // Multi-thread parallel processing
                        tasks.submit(new Callable<Void>() {

                            public Void call() throws FileNotFoundException, XMLStreamException {

                                ToolSet tools = threadTools.get();

                                // Set default filename
                                person.setFilename();

                                // Get person data (name, birthday, etc...), download photo
                                updatePersonData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukebox, person, tools.imagePlugin);
                                library.addPerson(person);

                                logger.info("Finished: " + person.getName() + " (" + count + "/" + peopleCount + ")");

                                // Show memory every (processing count) movies
                                if (showMemory && (count % MaxThreadsProcess) == 0) {
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
                        TreeMap<String, Integer> typeCounter = new TreeMap<String, Integer>();
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

                            // Multi-thread parallel processing
                            tasks.submit(new Callable<Void>() {

                                public Void call() throws FileNotFoundException, XMLStreamException {

                                    ToolSet tools = threadTools.get();

                                    // Set default filename
                                    p.setFilename();

                                    // Get person data (name, birthday, etc...), download photo and put to library
                                    updatePersonData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukebox, p, tools.imagePlugin);
                                    library.addPerson(p);

                                    logger.info("Finished: " + p.getName() + " (" + count + "/" + peopleCount + ")");

                                    // Show memory every (processing count) movies
                                    if (showMemory && (count % MaxThreadsProcess) == 0) {
                                        SystemTools.showMemory();
                                    }

                                    return null;
                                }
                            });
                        }
                    }
                }
                tasks.waitFor();
                logger.info("Fill in personal information to the movies...");
                for (Movie movie : library.values()) {
                    // Issue 997: Skip the processing of extras if not required
                    if (movie.isExtra() && !processExtras) {
                        continue;
                    }
                    for (Filmography person : movie.getPeople()) {
                        boolean dirty = false;
                        for (Person p : library.getPeople()) {
                            if (person.getName().equals(p.getName())) {
                                if (!person.getFilename().equals(p.getFilename()) && isValidString(p.getFilename())) {
                                    person.setFilename(p.getFilename());
                                    dirty = true;
                                }
                                if (!person.getUrl().equals(p.getUrl()) && isValidString(p.getUrl())) {
                                    person.setUrl(p.getUrl());
                                    dirty = true;
                                }
                                if (!person.getId().equals(p.getId()) && isValidString(p.getId())) {
                                    person.setId(p.getId());
                                    dirty = true;
                                }
                                break;
                            }
                        }
                        if (dirty) {
                            movie.setDirty(true);
                        }
                    }
                    
                    String name = movie.getOriginalTitle().toUpperCase() + " (" + movie.getYear() + ")";
                    for (Person p : library.getPeople()) {
                        for (Filmography film : p.getFilmography()) {
                            if (film.getName().toUpperCase().startsWith(name)) {
                                film.setFilename(movie.getBaseFilename());
                                if (film.isDirty()) {
                                    p.setDirty();
                                }
                                break;
                            }
                        }
                    }
                }
            }

            /********************************************************************************
             * 
             * PART 3 : Indexing the library
             * 
             */

            SystemTools.showMemory();
            
            // This is for programs like NMTServer where they don't need the indexes.
            if (skipIndexGeneration) {
                logger.info("Indexing of libraries skipped.");
            } else {
                logger.info("Indexing libraries...");
                library.buildIndex(tasks);
            }

            SystemTools.showMemory();
                
            logger.info("Indexing masters...");
            /*
             * This is kind of a hack -- library.values() are the movies that were found in the library and library.getMoviesList() 
             * are the ones that are there now. So the movies that are in getMoviesList but not in values are the index masters.
             */
            List<Movie> indexMasters = new ArrayList<Movie>(library.getMoviesList());
            indexMasters.removeAll(library.values());

            JAXBContext context = JAXBContext.newInstance(JukeboxXml.class);
            final JukeboxXml jukeboxXml = new JukeboxXml();
            jukeboxXml.movies = library.values();

            // Multi-thread: Parallel Executor
            tasks.restart();

            /********************************************************************************
             * 
             * PART 3B - Indexing masters
             */
            for (final Movie movie : indexMasters) {
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {
                        ToolSet tools = threadTools.get();

                        String safeSetMasterBaseName = FileTools.makeSafeFilename(movie.getBaseName());

                        // The master's movie XML is used for generating the
                        // playlist it will be overwritten by the index XML
                        
                        // Issue 1886: Html indexes recreated every time
                        // commented next 2 lines because generated XML file not useful
                        //logger.debug("Writing index data for master: " + movie.getBaseName());
                        //xmlWriter.writeMovieXML(jukebox, movie, library);

                        logger.debug("Updating poster for index master: " + movie.getOriginalTitle() + "...");

                        // If we can find a set poster file, use it; otherwise, stick with the first movie's poster
                        String oldPosterFilename = movie.getPosterFilename();

                        // Set a default poster name in case it's not found during the scan
                        movie.setPosterFilename(safeSetMasterBaseName + "." + posterExtension);
                        if (isNotValidString(PosterScanner.scan(jukebox, movie))) {
                            logger.debug("Local set poster (" + safeSetMasterBaseName + ") not found, using " + oldPosterFilename);
                            movie.setPosterFilename(oldPosterFilename);
                        }
                        
                        // If this is a TV Show and we want to download banners, then also check for a banner Set file
                        if (movie.isTVShow() && bannerDownload) {
                            // Set a default banner filename in case it's not found during the scan
                            movie.setBannerFilename(safeSetMasterBaseName + bannerToken + "." + bannerExtension);
                            if (!BannerScanner.scan(tools.imagePlugin, jukebox, movie)) {
                                updateTvBanner(jukebox, movie);
                                logger.debug("Local set banner (" + safeSetMasterBaseName + bannerToken + ") not found, using "
                                                + oldPosterFilename);
                            } else {
                                logger.debug("Local set banner found, using " + movie.getBannerFilename());
                            }
                        }

                        // Check for Set Fanart
                        if (setIndexFanart) {
                            // Set a default fanart filename in case it's not found during the scan
                            movie.setFanartFilename(safeSetMasterBaseName + fanartToken + "." + fanartExtension);
                            if (!FanartScanner.scan(tools.backgroundPlugin, jukebox, movie)) {
                                logger.debug("Local set fanart (" + safeSetMasterBaseName + fanartToken + ") not found, using "
                                                + oldPosterFilename);
                            } else {
                                logger.debug("Local set fanart found, using " + movie.getFanartFilename());
                            }
                        }

                        movie.setThumbnailFilename(safeSetMasterBaseName + thumbnailToken + "." + thumbnailExtension);
                        movie.setDetailPosterFilename(safeSetMasterBaseName + posterToken + "." + posterExtension);

                        if (PropertiesUtil.getBooleanProperty("mjb.sets.createPosters", "false")) {
                            // Create a detail poster for each movie
                            logger.debug("Creating detail poster for index master: " + movie.getBaseName());
                            createPoster(tools.imagePlugin, jukebox, skinHome, movie, forcePosterOverwrite);
                        }

                        // Create a thumbnail for each movie
                        logger.debug("Creating thumbnail for index master: " + movie.getBaseName() + ", isTV: " + movie.isTVShow() + ", isHD: " + movie.isHD());
                        createThumbnail(tools.imagePlugin, jukebox, skinHome, movie, forceThumbnailOverwrite);

                        // No playlist for index masters
                        // htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    };
                });
            }
            tasks.waitFor();
            
            // Clear the cache if we've used it
            Cache.clear();

            SystemTools.showMemory();

            // Issue 1886: Html indexes recreated every time
            for (Movie movie : library.getMoviesList()) {
                if (movie.isSetMaster()) {
                    File xmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + movie.getBaseName() + ".xml");
                    if (xmlFile.exists()) {
                        xmlWriter.parseSetXML(xmlFile, movie, library.getMoviesList());
                    }
                }
            }

            // Issue 1882: Separate index files for each category
            List<String> categoriesList = Arrays.asList(getProperty("mjb.categories.indexList", "Other,Genres,Title,Rating,Year,Library,Set").split(","));

            if (!skipIndexGeneration) {
                logger.info("Writing Indexes XML...");
                xmlWriter.writeIndexXML(jukebox, library, tasks);
                logger.info("Writing Category XML...");
                xmlWriter.writeCategoryXML(jukebox, library, "Categories", library.isDirty());

                // Issue 1882: Separate index files for each category
                if (separateCategories) {
                    for (String categoryName : categoriesList) {
                        xmlWriter.writeCategoryXML(jukebox, library, categoryName, library.isDirty());
                    }
                }
            }

            SystemTools.showMemory();

            logger.info("Writing movie data...");
            // Multi-thread: Parallel Executor
            tasks.restart();
            for (final Movie movie : library.values()) {
                // Issue 997: Skip the processing of extras if not required
                if (movie.isExtra() && !processExtras) {
                    continue;
                }
                
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {
                        ToolSet tools = threadTools.get();
                        // Update movie XML files with computed index information
                        logger.debug("Writing index data to movie: " + movie.getBaseName());
                        xmlWriter.writeMovieXML(jukebox, movie, library);

                        // Create a detail poster for each movie
                        logger.debug("Creating detail poster for movie: " + movie.getBaseName());
                        createPoster(tools.imagePlugin, jukebox, skinHome, movie, forcePosterOverwrite);

                        // Create a thumbnail for each movie
                        logger.debug("Creating thumbnails for movie: " + movie.getBaseName());
                        createThumbnail(tools.imagePlugin, jukebox, skinHome, movie, forceThumbnailOverwrite);

                        if (!skipIndexGeneration && !skipHtmlGeneration) {
                            // write the movie details HTML
                            logger.debug("Writing detail HTML to movie: " + movie.getBaseName());
                            htmlWriter.generateMovieDetailsHTML(jukebox, movie);

                            // write the playlist for the movie if needed
                            FileTools.addJukeboxFiles(htmlWriter.generatePlaylist(jukebox, movie));
                        }
                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    };
                });
            }
            tasks.waitFor();

            SystemTools.showMemory();

            if (peopleScan) {
                logger.info("Writing people data...");
                // Multi-thread: Parallel Executor
                tasks.restart();
                for (final Person person : library.getPeople()) {
                    // Multi-tread: Start Parallel Processing
                    tasks.submit(new Callable<Void>() {
                        public Void call() throws FileNotFoundException, XMLStreamException {
                            @SuppressWarnings("unused")
                            ToolSet tools = threadTools.get();
                            // Update person XML files with computed index information
                            logger.debug("Writing index data to person: " + person.getName());
                            xmlWriter.writePersonXML(jukebox, person, library);

                            if (!skipIndexGeneration && !skipHtmlGeneration) {
                                // write the person details HTML
                                htmlWriter.generatePersonDetailsHTML(jukebox, person);
                            }

                            return null;
                        };
                    });
                }
                tasks.waitFor();

                SystemTools.showMemory();
            }

            if (!skipIndexGeneration) {
                if (!skipHtmlGeneration) {
                    logger.info("Writing Indexes HTML...");
                    htmlWriter.generateMoviesIndexHTML(jukebox, library, tasks);
                    htmlWriter.generateMoviesCategoryHTML(jukebox, library, "Categories", "categories.xsl", library.isDirty());

                    // Issue 1882: Separate index files for each category
                    if (separateCategories) {
                        for (String categoryName : categoriesList) {
                            htmlWriter.generateMoviesCategoryHTML(jukebox, library, categoryName, "category.xsl", library.isDirty());
                        }
                    }
                }
                
                /*
                 * Generate the index file.
                 * Do not skip this part as it's the index that starts the jukebox
                 */
                htmlWriter.generateMainIndexHTML(jukebox, library);
            }

            final String totalMoviesXmlFileName = "CompleteMovies.xml";
            final String rssXmlFileName = "RSS.xml";
            if (library.isDirty()) {
                try {
                    final File totalMoviesXmlFile = new File(jukebox.getJukeboxTempLocationDetails(), totalMoviesXmlFileName);

                    OutputStream marStream = FileTools.createFileOutputStream(totalMoviesXmlFile);
                    context.createMarshaller().marshal(jukeboxXml, marStream);
                    marStream.close();
                    
                    Transformer transformer = getTransformer(new File("rss.xsl"), jukebox.getJukeboxRootLocationDetails());

                    Result xmlResult = new StreamResult(new File(jukebox.getJukeboxTempLocationDetails(), rssXmlFileName));
                    transformer.transform(new StreamSource(totalMoviesXmlFile), xmlResult);
                } catch (Exception e) {
                    logger.debug("RSS is not generated." /* + e.getStackTrace().toString() */);
                }
            }
            // These should be added to the list of jukebox files regardless of the state of the library
            FileTools.addJukeboxFile(totalMoviesXmlFileName);
            FileTools.addJukeboxFile(rssXmlFileName);

            /********************************************************************************
             * 
             * PART 4 : Copy files to target directory
             * 
             */
            SystemTools.showMemory();
            
            logger.info("Copying new files to Jukebox directory...");
            String index = getProperty("mjb.indexFile", "index.htm");
            
            FileTools.copyDir(jukebox.getJukeboxTempLocationDetails(), jukebox.getJukeboxRootLocationDetails(), true);
            FileTools.copyFile(new File(jukebox.getJukeboxTempLocation() + File.separator + index), new File(jukebox.getJukeboxRootLocation() + File.separator + index));

            String skinDate = jukebox.getJukeboxRootLocationDetails() + File.separator + "pictures" + File.separator + "skin.date";
            File skinFile = new File(skinDate);
            File propFile = new File(userPropertiesName);
            
            // If forceSkinOverwrite is set, the user properties file doesn't exist or is newer than the skin.date file
            if (forceSkinOverwrite || !propFile.exists() || FileTools.isNewer(propFile, skinFile)) {
                if (forceSkinOverwrite) {
                    logger.info("Copying skin files to Jukebox directory (forceSkinOverwrite)...");
                } else if (!propFile.exists()) {
                    logger.info("Copying skin files to Jukebox directory (No property file)...");
                } else if (FileTools.isNewer(propFile, skinFile)) {
                    logger.info("Copying skin files to Jukebox directory (" + propFile.getName() + " is newer)...");
                } else {
                    logger.info("Copying skin files to Jukebox directory...");
                }
                
                StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.skin.copyDirs", "html"), " ,;|");
                
                while (st.hasMoreTokens()) {
                    String skinDirName = st.nextToken();
                    String skinDirFull = skinHome + File.separator + skinDirName;
                    
                    if ((new File(skinDirFull).exists())) {
                        logger.info("Copying the " + skinDirName + " directory...");
                        FileTools.copyDir(skinDirFull, jukebox.getJukeboxRootLocationDetails(), true);
                    }
                }
                
                if (skinFile.exists()) {
                    skinFile.setLastModified(timeStart);
                } else {
                    skinFile.getParentFile().mkdirs();
                    skinFile.createNewFile();
                }
            } else {
                logger.info("Skin copying skipped.");
                logger.debug("Use mjb.forceSkinOverwrite=true to force the overwitting of the skin files");
            }

            FileTools.fileCache.saveFileList("filecache.txt");

            /********************************************************************************
             * 
             * PART 5: Clean-up the jukebox directory
             * 
             */
            SystemTools.showMemory();
            
            // Clean the jukebox folder of unneeded files
            cleanJukeboxFolder();
            
            if (moviejukeboxListing) {
                logger.info("Generating listing output...");
                listingPlugin.generate(jukebox, library);
            }

            logger.info("Clean up temporary files");
            File rootIndex = new File(appendToPath(jukebox.getJukeboxTempLocation(), index));
            rootIndex.delete();

            FileTools.deleteDir(jukebox.getJukeboxTempLocation());
        }

        // Write the jukebox details file at the END of the run (Issue 1830)
        JukeboxProperties.writeFile(jukebox, mediaLibraryPaths);

        timeEnd = System.currentTimeMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        logger.info("");
        logger.info("MovieJukebox process completed at " + new Date());
        logger.info("Processing took " + dateFormat.format(new Date(timeEnd - timeStart)));

        return;
    }

    /**
     * Clean up the jukebox folder of any extra files that are not needed.
     * 
     * If the jukeboxClean parameter is not set, just report on the files that would be cleaned.
     */
    private void cleanJukeboxFolder() {
        boolean cleanReport = PropertiesUtil.getBooleanProperty("mjb.jukeboxCleanReport", "false");
        
        if (jukeboxClean) {
            logger.info("Cleaning up the jukebox directory...");
        } else if (cleanReport) {
            logger.info("Jukebox cleaning skipped, the following files are orphaned (not used anymore):");
        } else {
            logger.info("Jukebox cleaning skipped.");
            return;
        }
        
        Collection<String> generatedFileNames = FileTools.getJukeboxFiles();

        File[] cleanList = jukebox.getJukeboxRootLocationDetailsFile().listFiles();
        int nbFiles = 0;
        int cleanDeletedTotal = 0;
        boolean skip = false;

        String skipPattStr = getProperty("mjb.clean.skip");
        Pattern skipPatt = null != skipPattStr ? Pattern.compile(skipPattStr, Pattern.CASE_INSENSITIVE) : null;

        for (nbFiles = 0; nbFiles < cleanList.length; nbFiles++) {
            // Scan each file in here
            if (cleanList[nbFiles].isFile() && !generatedFileNames.contains(cleanList[nbFiles].getName())) {
                skip = false;

                // If the file is in the skin's exclusion regex, skip it
                if (skipPatt != null) {
                    skip = skipPatt.matcher(cleanList[nbFiles].getName()).matches();
                }

                // If the file isn't skipped and it's not part of the library, delete it
                if (!skip) {
                    if (jukeboxClean) {
                        logger.debug("Deleted: " + cleanList[nbFiles].getName() + " from library");
                        cleanList[nbFiles].delete();
                    } else {
                        logger.debug("Unused: " + cleanList[nbFiles].getName());
                    }
                    cleanDeletedTotal++;
                }
            }
        }
        
        logger.info(Integer.toString(nbFiles) + " files in the jukebox directory");
        if (cleanDeletedTotal > 0 ) {
            if (jukeboxClean) {
                logger.info("Deleted " + Integer.toString(cleanDeletedTotal) + " unused " + (cleanDeletedTotal==1?"file":"files") + " from the jukebox directory");
            } else {
                logger.info("There " + (cleanDeletedTotal==1?"is ":"are ") + Integer.toString(cleanDeletedTotal) + " orphaned " + (cleanDeletedTotal==1?"file":"files") + " in the jukebox directory");
            }
        }

        return;
    }

    /**
     * Generates a movie XML file which contains data in the <tt>Movie</tt> bean.
     * 
     * When an XML file exists for the specified movie file, it is loaded into the specified <tt>Movie</tt> object.
     * 
     * When no XML file exist, scanners are called in turn, in order to add information to the specified <tt>movie</tt> object. 
     * Once scanned, the <tt>movie</tt> object is persisted.
     */
    public boolean updateMovieData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie) throws FileNotFoundException, XMLStreamException {
        boolean forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", "false");
        boolean checkNewer = PropertiesUtil.getBooleanProperty("filename.nfo.checknewer", "true");

        /*
         * For each video in the library, if an XML file for this video already exists, then there is no need to search for the video file information, just
         * parse the XML data.
         */
        String safeBaseName = movie.getBaseName();
        File xmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + safeBaseName + ".xml");

        // See if we can find the NFO associated with this video file.
        List<File> nfoFiles = MovieNFOScanner.locateNFOs(movie);

        // Only check the NFO files if the XML exists and the CheckNewer parameter is set
        if (checkNewer && xmlFile.exists() ) {
            for (File nfoFile : nfoFiles) {
                // Only re-scan the nfo files if one of them is newer
                if (FileTools.isNewer(nfoFile, xmlFile)) {
                    logger.info("NFO for " + movie.getOriginalTitle() + " (" + nfoFile.getAbsolutePath() + ") has changed, will rescan file.");
                    movie.setDirty(true);
                    movie.setDirtyNFO(true);
                    movie.setDirtyPoster(true);
                    movie.setDirtyFanart(true);
                    movie.setDirtyBanner(true);
                    forceXMLOverwrite = true;
                    break; // one is enough
                }
            }
        }

        Collection<MovieFile> scannedFiles = null;
        // Only parse the XML file if we mean to update the XML file.
        if (xmlFile.exists() && !forceXMLOverwrite) {
            // Parse the XML file
            logger.debug("XML file found for " + movie.getBaseName());
            // Copy scanned files BEFORE parsing the existing XML
            scannedFiles = new ArrayList<MovieFile>(movie.getMovieFiles());
            xmlWriter.parseMovieXML(xmlFile, movie);

            // Issue 1886: Html indexes recreated every time
            // after remove NFO set data restoring from XML - compare NFO and XML sets
            Movie movieNFO = new Movie();
            for (String s : movie.getSetsKeys()) {
                movieNFO.addSet(s);
            }
            MovieNFOScanner.scan(movieNFO, nfoFiles);
            if (!Arrays.equals(movieNFO.getSetsKeys().toArray(), movie.getSetsKeys().toArray())) {
                movie.setSets(movieNFO.getSets());
                movie.setDirtyNFO(true);
            }

            // If we are overwiting the indexes, we need to check for an update to the library description
            if (PropertiesUtil.getBooleanProperty("mjb.forceIndexOverwrite", "false")) {
                for (MediaLibraryPath mlp : mediaLibraryPaths) {
                    // Check to see if the paths match and then update the description and quit
                    if (movie.getFile().getAbsolutePath().startsWith(mlp.getPath()) && !movie.getLibraryDescription().equals(mlp.getDescription())) {
                        logger.debug("Changing libray description from " + movie.getLibraryDescription() + " to " + mlp.getDescription()); // XXX DEBUG
                        movie.setLibraryDescription(mlp.getDescription());
                        movie.setDirty(true);
                        break;
                    }
                }
            }
            
            if (recheckXML && mjbRecheck(movie)) {
                logger.info("Recheck of " + movie.getBaseName() + " required");
                forceXMLOverwrite = true;
                movie.setDirty(true);
            }
        }

        // ForceBannerOverwrite is set here to force the re-load of TV Show data including the banners
        if (xmlFile.exists() && !forceXMLOverwrite && !(movie.isTVShow() && forceBannerOverwrite)) {
            // *** START of routine to check if the file has changed location
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

                // VIDEO_TS.IFO check added for Issue 1851
                if (((!scannedFilename.equalsIgnoreCase(xmlLoop.getFilename())) && (!(scannedFilename + "/VIDEO_TS.IFO").equalsIgnoreCase(xmlLoop.getFilename()))) || (!scannedFileLocation.equalsIgnoreCase(xmlLoop.getFile().getAbsolutePath()))) {
                    logger.debug("Detected change of file location to: " + scannedFilename);
                    xmlLoop.setFilename(scannedFilename);
                    xmlLoop.setNewFile(true);
                    movie.addMovieFile(xmlLoop);
                    
                    // if we have more than one path, we'll need to change the library details in the movie
                    if (mediaLibraryPaths.size() > 1) {
                        for (MediaLibraryPath mlp : mediaLibraryPaths) {
                            // Check to see if the paths match and then update the description and quit
                            if (scannedFilename.startsWith(mlp.getPlayerRootPath())) {
                                movie.setLibraryDescription(mlp.getDescription());
                                break;
                            }
                        }
                    }
                }
            }
            // *** END of file location change

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
            if (!isValidString(movie.getPosterURL())) {
                PosterScanner.scan(movie);
            }

        } else {
            // No XML file for this movie. 
            // We've got to find movie information where we can (filename, IMDb, NFO, etc...) Add here extra scanners if needed.
            if (forceXMLOverwrite) {
                logger.debug("Rescanning internet for information on " + movie.getBaseName());
            } else {
                logger.debug("Jukebox XML file not found: " + xmlFile.getAbsolutePath());
                logger.debug("Scanning for information on " + movie.getBaseName());
            }

            // Changing call order, first MediaInfo then NFO. NFO will overwrite any information found by the MediaInfo Scanner.
            miScanner.scan(movie);
            
            MovieNFOScanner.scan(movie, nfoFiles);

            // Added forceXMLOverwrite for issue 366
            if (!isValidString(movie.getPosterURL()) || movie.isDirtyPoster()) {
                PosterScanner.scan(jukebox, movie);
            }
            
            DatabasePluginController.scan(movie);
            // Issue 1323:      Posters not picked up from NFO file
            // Only search for poster if we didn't have already
            if (!isValidString(movie.getPosterURL())) {
                PosterScanner.scan(movie);
            }
            
            // Check for new fanart if we need to (Issue 1563)
            if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                if (!isValidString(movie.getFanartURL()) || movie.isDirtyFanart()) {
                    FanartScanner.scan(backgroundPlugin, jukebox, movie);
                }
            }
        }
        return movie.isDirty() || movie.isDirtyNFO();
    }

    public void updatePersonData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, Jukebox jukebox, Person person, MovieImagePlugin imagePlugin) throws FileNotFoundException, XMLStreamException {
        boolean forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", "false");
        person.setFilename();
        File xmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + FileTools.makeSafeFilename(person.getFilename() + ".xml"));

        // Change the output message depending on the existance of the XML file
        if (xmlFile.exists()) {
            logger.info("Checking existing person: " + person.getName());
        } else {
            logger.info("Processing new person: " + person.getName());
        }

        if (xmlFile.exists() && !forceXMLOverwrite) {
            logger.debug("XML file found for " + person.getName());

            xmlWriter.parsePersonXML(xmlFile, person);
        } else {
            if (forceXMLOverwrite) {
                logger.debug("Rescanning internet for information on " + person.getName());
            } else {
                logger.debug("Jukebox XML file not found: " + xmlFile.getAbsolutePath());
                logger.debug("Scanning for information on " + person.getName());
            }
            DatabasePluginController.scan(person);
        }

        if (photoDownload) {
            PhotoScanner.scan(imagePlugin, jukebox, person);
        }
    }

    /**
     * Update the movie poster for the specified movie. When an existing thumbnail is found for the movie, it is not overwritten, unless the
     * mjb.forceThumbnailOverwrite is set to true in the property file. When the specified movie does not contain a valid URL for the poster, a dummy image is
     * used instead.
     * 
     * @param tempJukeboxDetailsRoot
     */
    public void updateMoviePoster(Jukebox jukebox, Movie movie) {
        String posterFilename = movie.getPosterFilename();
        File posterFile = new File(jukebox.getJukeboxRootLocationDetails() + File.separator + posterFilename);
        File tmpDestFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + posterFilename);

        // Check to see if there is a local poster.
        // Check to see if there are posters in the jukebox directories (target and temp)
        // Check to see if the local poster is newer than either of the jukebox posters
        // Download poster

        // Do not overwrite existing posters, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !posterFile.exists()) || (movie.isDirtyPoster()) || forcePosterOverwrite) {
            posterFile.getParentFile().mkdirs();

            if (!isValidString(movie.getPosterURL())) {
                logger.debug("Dummy image used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), tmpDestFile);
            } else {
                try {
                    // Issue 201 : we now download to local temp dir
                    logger.debug("Downloading poster for " + movie.getBaseName() + " to " + tmpDestFile.getName() + " [calling plugin]");
                    FileTools.downloadImage(tmpDestFile, movie.getPosterURL());
                    logger.debug("Downloaded poster for " + movie.getBaseName());
                } catch (Exception error) {
                    logger.debug("Failed downloading movie poster : " + movie.getPosterURL());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), tmpDestFile);
                }
            }
        }
    }

    /**
     * Update the banner for the specified TV Show. When an existing banner is found for the movie, it is not overwritten, unless the mjb.forcePosterOverwrite
     * is set to true in the property file. When the specified movie does not contain a valid URL for the banner, a dummy image is used instead.
     * 
     * @param tempJukeboxDetailsRoot
     */
    public void updateTvBanner(Jukebox jukebox, Movie movie) {
        String bannerFilename = movie.getBannerFilename();
        File bannerFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + bannerFilename);
        File tmpDestFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + bannerFilename);

        // Check to see if there is a local banner.
        // Check to see if there are banners in the jukebox directories (target and temp)
        // Check to see if the local banner is newer than either of the jukebox banners
        // Download banner

        // Do not overwrite existing banners, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !bannerFile.exists()) || (movie.isDirtyBanner()) || forceBannerOverwrite) {
            bannerFile.getParentFile().mkdirs();

            if (isNotValidString(movie.getBannerURL())) {
                logger.debug("Dummy banner used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), tmpDestFile);
            } else {
                try {
                    logger.debug("Downloading banner for " + movie.getBaseName() + " to " + tmpDestFile.getName() + " [calling plugin]");
                    FileTools.downloadImage(tmpDestFile, movie.getBannerURL());
                } catch (Exception error) {
                    logger.debug("Failed downloading banner: " + movie.getBannerURL());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), tmpDestFile);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) {
        Collection<MediaLibraryPath> mlp = new ArrayList<MediaLibraryPath>();

        if (!f.exists() || f.isDirectory()) {
            logger.error("The moviejukebox library input file you specified is invalid: " + f.getName());
            return mlp;
        }

        try {
            XMLConfiguration c = new XMLConfiguration(f);

            List<HierarchicalConfiguration> fields = c.configurationsAt("library");
            for (Iterator<HierarchicalConfiguration> it = fields.iterator(); it.hasNext();) {
                HierarchicalConfiguration sub = it.next();
                // sub contains now all data about a single medialibrary node
                String path = sub.getString("path");
                String nmtpath = sub.getString("nmtpath"); // This should be depreciated
                String playerpath = sub.getString("playerpath");
                String description = sub.getString("description");
                boolean scrapeLibrary = true;

                String scrapeLibraryString = sub.getString("scrapeLibrary");
                if (isValidString(scrapeLibraryString)) {
                    try {
                        scrapeLibrary = sub.getBoolean("scrapeLibrary");
                    } catch (Exception ignore) {
                    }
                }

                long prebuf = -1;
                String prebufString = sub.getString("prebuf");
                if (prebufString != null && !prebufString.isEmpty()) {
                    try {
                        prebuf = Long.parseLong(prebufString);
                    } catch (Exception ignore) {
                    }
                }

                // Note that the nmtpath should no longer be used in the library file and instead "playerpath" should be used.
                // Check that the nmtpath terminates with a "/" or "\"
                if (nmtpath != null) {
                    if (!(nmtpath.endsWith("/") || nmtpath.endsWith("\\"))) {
                        // This is the NMTPATH so add the unix path separator rather than File.separator
                        nmtpath = nmtpath + "/";
                    }
                }

                // Check that the playerpath terminates with a "/" or "\"
                if (playerpath != null) {
                    if (!(playerpath.endsWith("/") || playerpath.endsWith("\\"))) {
                        // This is the PlayerPath so add the Unix path separator rather than File.separator
                        playerpath = playerpath + "/";
                    }
                }

                List<String> excludes = sub.getList("exclude[@name]");
                File medialibfile = new File(path);
                if (medialibfile.exists()) {
                    MediaLibraryPath medlib = new MediaLibraryPath();
                    medlib.setPath(medialibfile.getCanonicalPath());
                    if (playerpath == null || playerpath.equals("")) {
                        medlib.setPlayerRootPath(nmtpath);
                    } else {
                        medlib.setPlayerRootPath(playerpath);
                    }
                    medlib.setExcludes(excludes);
                    medlib.setDescription(description);
                    medlib.setScrapeLibrary(scrapeLibrary);
                    medlib.setPrebuf(prebuf);
                    mlp.add(medlib);

                    if (description != null && !description.isEmpty()) {
                        logger.info("Found media library: " + description);
                    } else {
                        logger.info("Found media library: " + path);
                    }
                    // Save the media library to the log file for reference.
                    logger.debug("Media library: " + medlib);

                } else {
                    logger.info("Skipped invalid media library: " + path);
                }
            }
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error("Failed parsing moviejukebox library input file: " + f.getName());
            logger.error(eResult.toString());
        }
        return mlp;
    }

    public static MovieImagePlugin getImagePlugin(String className) {
        MovieImagePlugin imagePlugin;

        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            imagePlugin = pluginClass.newInstance();
        } catch (Exception error) {
            imagePlugin = new DefaultImagePlugin();
            logger.error("Failed instanciating imagePlugin: " + className);
            logger.error("Default poster plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        return imagePlugin;
    }

    public static MovieImagePlugin getBackgroundPlugin(String className) {
        MovieImagePlugin backgroundPlugin;

        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            backgroundPlugin = pluginClass.newInstance();
        } catch (Exception error) {
            backgroundPlugin = new DefaultBackgroundPlugin();
            logger.error("Failed instanciating BackgroundPlugin: " + className);
            logger.error("Default background plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        return backgroundPlugin;
    }

    public static MovieListingPlugin getListingPlugin(String className) {
        MovieListingPlugin listingPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieListingPlugin> pluginClass = cl.loadClass(className).asSubclass(MovieListingPlugin.class);
            listingPlugin = pluginClass.newInstance();
        } catch (Exception error) {
            listingPlugin = new MovieListingPluginBase();
            logger.error("Failed instantiating ListingPlugin: " + className);
            logger.error("No listing plugin will be used.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        return listingPlugin;
    } // getListingPlugin()

    /**
     * Create a thumbnail from the original poster file.
     * 
     * @param thumbnailManager
     * @param rootPath
     * @param tempRootPath
     * @param skinHome
     * @param movie
     * @param forceThumbnailOverwrite
     */
    public static void createThumbnail(MovieImagePlugin imagePlugin, Jukebox jukebox, String skinHome, Movie movie,
                    boolean forceThumbnailOverwrite) {
        try {
            // TODO Move all temp directory code to FileTools for a cleaner method
            // Issue 201 : we now download to local temp directory
            String safePosterFilename = movie.getPosterFilename();
            String safeThumbnailFilename = movie.getThumbnailFilename();
            File src = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + safePosterFilename);
            File oldsrc = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + safePosterFilename);
            String dst = jukebox.getJukeboxTempLocationDetails() + File.separator + safeThumbnailFilename;
            String olddst = jukebox.getJukeboxRootLocationDetails() + File.separator + safeThumbnailFilename;
            File fin;

            if (forceThumbnailOverwrite || !FileTools.fileCache.fileExists(olddst) || src.exists()) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (src.exists()) {
                    //logger.debug("New file exists: " + src.getAbsolutePath());
                    fin = src;
                } else {
                    //logger.debug("Use old file: " + oldsrc.getAbsolutePath());
                    fin = oldsrc;
                }

                BufferedImage bi = null;
                try {
                    bi = GraphicTools.loadJPEGImage(fin);
                } catch (Exception error) {
                    logger.warn("Error reading the thumbnail file: " + fin.getAbsolutePath());
                }
                
                if (bi == null) {
                    logger.info("Using dummy thumbnail image for " + movie.getOriginalTitle());
                    // There was an error with the URL, assume it's a bad URL and clear it so we try again
                    movie.setPosterURL(Movie.UNKNOWN);
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), 
                                       new File(jukebox.getJukeboxRootLocationDetails() + File.separator + safePosterFilename));
                    try {
                        bi = GraphicTools.loadJPEGImage(src);
                    } catch (Exception error) {
                        logger.warn("Error reading the dummy image file: " + src.getAbsolutePath());
                    }
                }

                // Perspective code.
                String perspectiveDirection = getProperty("thumbnails.perspectiveDirection", "right");

                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror thumbnail name.
                    String dstMirror = new String(dst.substring(0, dst.lastIndexOf("."))) + "_mirror" + new String(dst.substring(dst.lastIndexOf(".")));

                    // Generate left & save as copy
                    logger.debug("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = imagePlugin.generate(movie, bi, "thumbnails", "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.debug("Generating right thumbnail from " + src + " to " + dst);
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }

                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.debug("Generating right thumbnail from " + src + " to " + dst);
                }

                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.debug("Generating left thumbnail from " + src + " to " + dst);
                }
            }
        } catch (Exception error) {
            logger.error("Failed creating thumbnail for " + movie.getOriginalTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /**
     * Create a detailed poster file from the original poster file
     * 
     * @param posterManager
     * @param rootPath
     * @param tempRootPath
     * @param skinHome
     * @param movie
     * @param forcePosterOverwrite
     */
    public static void createPoster(MovieImagePlugin posterManager, Jukebox jukebox, String skinHome, Movie movie,
                    boolean forcePosterOverwrite) {
        try {
            // Issue 201 : we now download to local temporary directory
            String safePosterFilename = movie.getPosterFilename();
            String safeDetailPosterFilename = movie.getDetailPosterFilename();
            File   src = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + safePosterFilename);
            File   oldsrc = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + safePosterFilename);
            String dst = jukebox.getJukeboxTempLocationDetails() + File.separator + safeDetailPosterFilename;
            String olddst = jukebox.getJukeboxRootLocationDetails() + File.separator + safeDetailPosterFilename;
            File fin;

            if (forcePosterOverwrite || !FileTools.fileCache.fileExists(olddst) || src.exists()){
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (src.exists()) {
                    logger.debug("CreatePoster: New file exists (" + src + ")");
                    fin = src;
                } else {
                    logger.debug("CreatePoster: Using old file (" + oldsrc + ")");
                    fin = oldsrc;
                }

                BufferedImage bi = null;
                try {
                    bi = GraphicTools.loadJPEGImage(fin);
                } catch (Exception error) {
                    logger.warn("Error reading the poster file: " + fin.getAbsolutePath());
                }

                if (bi == null) {
                    logger.info("Using dummy poster image for " + movie.getOriginalTitle());
                    // There was an error with the URL, assume it's a bad URL and clear it so we try again
                    movie.setPosterURL(Movie.UNKNOWN);
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), oldsrc);
                    bi = GraphicTools.loadJPEGImage(src);
                }
                logger.debug("Generating poster from " + src + " to " + dst);

                // Perspective code.
                String perspectiveDirection = getProperty("posters.perspectiveDirection", "right");

                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror poster name.
                    String dstMirror = new String(dst.substring(0, dst.lastIndexOf("."))) + "_mirror" + new String(dst.substring(dst.lastIndexOf(".")));

                    // Generate left & save as copy
                    logger.debug("Generating mirror poster from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = posterManager.generate(movie, bi, "posters", "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.debug("Generating right poster from " + src + " to " + dst);
                    bi = posterManager.generate(movie, bi, "posters", "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }

                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = posterManager.generate(movie, bi, "posters", "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.debug("Generating right poster from " + src + " to " + dst);
                }

                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = posterManager.generate(movie, bi, "posters", "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.debug("Generating left poster from " + src + " to " + dst);
                }
            }
        } catch (Exception error) {
            logger.error("Failed creating poster for " + movie.getOriginalTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /**
     * This function will validate the current movie object and return true if the movie needs to be re-scanned.
     * @param movie
     * @return
     */
    private static boolean mjbRecheck(Movie movie) {
        // Property variables
        int recheckMax = PropertiesUtil.getIntProperty("mjb.recheck.Max", "50");
        boolean recheckVersion = PropertiesUtil.getBooleanProperty("mjb.recheck.Version", "true");
        
        // Skip Extras (Trailers, etc)
        if (movie.isExtra()) {
            return false;
        }
        
        // Check for the version of YAMJ that wrote the XML file vs the current version
        //System.out.println("- mjbVersion : " + movie.getMjbVersion() + " (" + movie.getCurrentMjbVersion() + ")");
        if (recheckVersion && !movie.getMjbVersion().equalsIgnoreCase(movie.getCurrentMjbVersion())) {
            logger.debug("Recheck: " + movie.getBaseName() + " XML is from a previous version, will rescan");
            // We *ALWAYS* want to re-write the XML when the version changes, so don't skip this check
            // recheckCount++;
            return true;
        }
        
        if (recheckCount >= recheckMax) {
            // We are over the recheck maximum, so we won't recheck again this run
            return false;
        } else if (recheckCount == recheckMax) {
            logger.debug("Recheck: Threshold of " + recheckMax + " rechecked movies reached. No more will be checked until the next run.");
            recheckCount++; // By incrementing this variable we will only display this message once.
            return false;
        }
        
        int recheckDays     = PropertiesUtil.getIntProperty("mjb.recheck.Days", "30");
        int recheckMinDays  = PropertiesUtil.getIntProperty("mjb.recheck.minDays", "7");
        int recheckRevision = PropertiesUtil.getIntProperty("mjb.recheck.Revision", "25");
        boolean recheckUnknown = PropertiesUtil.getBooleanProperty("mjb.recheck.Unknown", "true");
        Date currentDate = new Date();
        long dateDiff = (currentDate.getTime() - movie.getMjbGenerationDate().toDate().getTime()) / (1000 * 60 * 60 * 24);
        
        // Check the date the XML file was last written to and skip if it's less than minDays
        if ((recheckMinDays > 0) && (dateDiff <= recheckMinDays)) {
            return false;
        }
        
        // Check the date the XML file was written vs the current date
        if ((recheckDays > 0)  && (dateDiff > recheckDays)) {
            logger.debug("Recheck: " + movie.getBaseName() + " XML is " + dateDiff + " days old, will rescan");
            recheckCount++;
            return true;
        }
        
        // If we don't recheck the version, we don't want to recheck the revision either
        if (recheckVersion) {
            // Check the revision of YAMJ that wrote the XML file vs the current revisions
            //System.out.println("- mjbRevision: " + movie.getMjbRevision() + " (" + movie.getCurrentMjbRevision() + ")");
            //System.out.println("- Difference : " + (Integer.parseInt(movie.getCurrentMjbRevision()) - Integer.parseInt(movie.getMjbRevision())) );
            String currentRevision = movie.getCurrentMjbRevision();
            String mjbRevision = movie.getMjbRevision();
            int revDiff = Integer.parseInt(isNotValidString(currentRevision) ? "0" : currentRevision) - Integer.parseInt(isNotValidString(mjbRevision) ? "0" : mjbRevision); 
            if (revDiff > recheckRevision) {
                logger.debug("Recheck: " + movie.getBaseName() + " XML is " + revDiff + " revisions old (" + recheckRevision + " maximum), will rescan");
                recheckCount++;
                return true;
            }
        }
        
        // Check for "UNKNOWN" values in the XML
        if (recheckUnknown) {
            if (isNotValidString(movie.getTitle()) && isNotValidString(movie.getYear())) {
                logger.debug("Recheck: " + movie.getBaseName() + " XML is missing the title, will rescan");
                recheckCount++;
                return true;
            }
            
            if (isNotValidString(movie.getPlot())) {
                logger.debug("Recheck: " + movie.getBaseName() + " XML is missing plot, will rescan");
                recheckCount++;
                return true;
            }
            
            if (isNotValidString(movie.getYear())) {
                logger.debug("Recheck: " + movie.getBaseName() + " XML is missing year, will rescan");
                recheckCount++;
                return true;
            }
            
            if (movie.getGenres().isEmpty()) {
                logger.debug("Recheck: " + movie.getBaseName() + " XML is missing genres, will rescan");
                recheckCount++;
                return true;
            }
            
            if (isNotValidString(movie.getPosterURL())) {
                logger.debug("Recheck: " + movie.getBaseName() + " is missing poster, will rescan");
                recheckCount++;
                return true;
            }
            
            if (isNotValidString(movie.getFanartURL())) {
                if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                    logger.debug("Recheck: " + movie.getBaseName() + " is missing fanart, will rescan");
                    recheckCount++;
                    return true;
                }
            }
            
            if (movie.isTVShow()) {
                boolean recheckEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
                
                if (bannerDownload && isNotValidString(movie.getBannerURL())) {
                    logger.debug("Recheck: " + movie.getBaseName() + " is missing banner artwork, will rescan");
                    recheckCount++;
                    return true;
                }
                
                // scan the TV episodes
                for (MovieFile mf : movie.getMovieFiles()) {
                    if (isNotValidString(mf.getTitle())) {
                        logger.debug("Recheck: " + movie.getBaseName() + " - Part " + mf.getFirstPart() + " XML is missing Title, will rescan");
                        mf.setNewFile(true); // This forces the episodes to be rechecked
                        recheckCount++;
                        return true;
                    }

                    if (recheckEpisodePlots || videoimageDownload) {
                        for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                            if (recheckEpisodePlots && isNotValidString(mf.getPlot(part))) {
                                logger.debug("Recheck: " + movie.getBaseName() + " - Part " + part + " XML is missing TV plot, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            } // plots
                            
                            if (videoimageDownload && isNotValidString(mf.getVideoImageURL(part))) {
                                logger.debug("Recheck: " + movie.getBaseName() + " - Part " + part + " XML is missing TV video image, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            } // videoimages
                        } // moviefile parts loop
                    } // if
                } // moviefiles loop
            } // isTVShow
        }
        return false;
    }

    /**
     * This function will check movie trailers and return true if trailers needs to be re-scanned.
     * @param movie
     * @return
     */
    private static boolean isTrailersNeedRescan(Movie movie) {

        boolean trailersOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceTrailersOverwrite", "false");
        if (trailersOverwrite) {
            return true;
        }

        // Check if this movie was already checked for trailers
        if (movie.isTrailerExchange()) {
            logger.debug("Trailers Plugin: Movie " + movie.getTitle() + " has previously been checked for trailers, skipping.");
            return false;
        }

        // Check if we need to scan or rescan for trailers
        long now = new Date().getTime();
        if ((now - movie.getTrailerLastScan()) < trailersRescanDaysMillis) {
            return false;
        }

        return true;
    }

    public static boolean isJukeboxPreserve() {
        return jukeboxPreserve;
    }

    public static void setJukeboxPreserve(boolean bJukeboxPreserve) {
        jukeboxPreserve = bJukeboxPreserve;
    }

    /**
     * Return the Jukebox object
     * @return the jukebox
     */
    public static final Jukebox getJukebox() {
        return jukebox;
    }

}
