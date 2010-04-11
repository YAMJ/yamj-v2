/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
import static com.moviejukebox.writer.MovieJukeboxHTMLWriter.getTransformer;
import static java.lang.Boolean.parseBoolean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.AppleTrailersPlugin;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultImagePlugin;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.MovieListingPlugin;
import com.moviejukebox.plugin.MovieListingPluginBase;
import com.moviejukebox.plugin.OpenSubtitlesPlugin;
import com.moviejukebox.scanner.BannerScanner;
import com.moviejukebox.scanner.FanartScanner;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.scanner.OutputDirectoryScanner;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.scanner.VideoImageScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.LogFormatter;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.PropertiesUtil.KeywordMap;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class MovieJukebox {

    private static Logger logger = Logger.getLogger("moviejukebox");
    Collection<MediaLibraryPath> movieLibraryPaths;
    private String movieLibraryRoot;
    private String jukeboxRoot;
    private String skinHome;
    private String detailsDirName;

    // Timestamps
    private static long timeStart = System.currentTimeMillis();
    private static long timeEnd;

    // Overwrite flags
    private boolean forcePosterOverwrite;
    private boolean forceThumbnailOverwrite;
    private boolean forceBannerOverwrite;

    // Scanner Tokens
    private static String posterToken;
    private static String thumbnailToken;
    private static String bannerToken;
    @SuppressWarnings("unused")
    private static String videoimageToken;
    private static String fanartToken;

    private static boolean jukeboxClean = false;
    
    private boolean fanartMovieDownload;
    private boolean fanartTvDownload;
    private static boolean videoimageDownload;
    private boolean bannerDownload;
    private String jukeboxTempDir;
    private boolean moviejukeboxListing;
    private boolean setIndexFanart;
    private boolean recheckXML;
    private static int recheckCount = 0;
    private static boolean skipIndexGeneration = false;
    private static boolean dumpLibraryStructure = false;

        // These are pulled from the Manifest.MF file that is created by the Ant build script
    public static String mjbVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
    public static String mjbRevision = MovieJukebox.class.getPackage().getImplementationVersion();
    public static String mjbBuildDate = MovieJukebox.class.getPackage().getImplementationTitle();

    public static void main(String[] args) throws Throwable {
        String logFilename = "moviejukebox.log";
        LogFormatter mjbFormatter = new LogFormatter();

        FileHandler fh = new FileHandler(logFilename);
        fh.setFormatter(mjbFormatter);
        fh.setLevel(Level.ALL);

        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(mjbFormatter);
        ch.setLevel(Level.FINE);

        logger.setUseParentHandlers(false);
        logger.addHandler(fh);
        logger.addHandler(ch);
        logger.setLevel(Level.ALL);

        // Just create a pretty underline.
        String mjbTitle = "";
        if (mjbVersion == null)
            mjbVersion = "";
        for (int i = 1; i <= mjbVersion.length(); i++) {
            mjbTitle += "~";
        }

        logger.fine("Yet Another Movie Jukebox " + mjbVersion);
        logger.fine("~~~ ~~~~~~~ ~~~~~ ~~~~~~~ " + mjbTitle);
        logger.fine("http://code.google.com/p/moviejukebox/");
        logger.fine("Copyright (c) 2004-2010 YAMJ Members");
        logger.fine("");
        logger.fine("This software is licensed under a Creative Commons License");
        logger.fine("See this page: http://code.google.com/p/moviejukebox/wiki/License");
        logger.fine("");

        // Print the revision information if it was populated by Hudson CI
        if (!((mjbRevision == null) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            logger.fine("  Revision: r" + mjbRevision);
            logger.fine("Build Date: " + mjbBuildDate);
            logger.fine("");
        }
        logger.fine("Processing started at " + new Date());
        logger.fine("");

        String movieLibraryRoot = null;
        String jukeboxRoot = null;
        boolean jukeboxPreserve = false;
        String propertiesName = "./moviejukebox.properties";
        Map<String, String> cmdLineProps = new LinkedHashMap<String, String>();

        if (args.length == 0) {
            help();
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = (String)args[i];
                if ("-o".equalsIgnoreCase(arg)) {
                    jukeboxRoot = args[++i];
                } else if ("-c".equalsIgnoreCase(arg)) {
                    jukeboxClean = true;
                } else if ("-k".equalsIgnoreCase(arg)) {
                    jukeboxPreserve = true;
                } else if ("-p".equalsIgnoreCase(arg)) {
                    propertiesName = args[++i];
                } else if ("-i".equalsIgnoreCase(arg)) {
                    skipIndexGeneration = true;
                } else if ("-dump".equalsIgnoreCase(arg)) {
                    dumpLibraryStructure = true;
                } else if (arg.startsWith("-D")) {
                    String propLine = arg.length() > 2 ? arg.substring(2) : args[++i];
                    int propDiv = propLine.indexOf("=");
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
            logger.severe("Wrong arguments specified");
            help();
            return;
        }

        // Load the moviejukebox-default.properties file
        if (!setPropertiesStreamName("./properties/moviejukebox-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        setPropertiesStreamName(propertiesName);

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
            // This is needed to update the static reference for the API Keys in the log formatted because the log formatter is initialised before the
            // properties files are read
            LogFormatter.addApiKeys();
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
        logger.finer("Properties: " + sb.toString());

        MovieFilenameScanner.setSkipKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords", ""), ",;| "));
        MovieFilenameScanner.setExtrasKeywords(tokenizeToArray(getProperty("filename.extras.keywords", "trailer,extra,bonus"), ",;| "));
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray(getProperty("filename.movie.versions.keywords",
                        "remastered,directors cut,extended cut,final cut"), ",;|"));
        MovieFilenameScanner.setLanguageDetection(parseBoolean(getProperty("filename.scanner.language.detection", "true")));
        final KeywordMap languages = PropertiesUtil.getKeywordMap("filename.scanner.language.keywords", null);
        if (languages.size() > 0) {
            MovieFilenameScanner.clearLanguages();
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    MovieFilenameScanner.addLanguage(lang, values, values);
                } else {
                    logger.fine("MovieFilenameScanner: No values found for language code " + lang);
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
                String token = st.nextToken();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                Movie.addSortIgnorePrefixes(token.toLowerCase());
            }
        }

        if (movieLibraryRoot == null) {
            movieLibraryRoot = getProperty("mjb.libraryRoot");
            logger.fine("Got libraryRoot from properties file: " + movieLibraryRoot);
        }

        if (jukeboxRoot == null) {
            jukeboxRoot = getProperty("mjb.jukeboxRoot");
            if (jukeboxRoot == null) {
                logger.fine("jukeboxRoot is null in properties file. Please fix this as it may cause errors.");
            } else {
                logger.fine("Got jukeboxRoot from properties file: " + jukeboxRoot);
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

        if (!new File(movieLibraryRoot).exists()) {
            logger.severe("Directory not found : " + movieLibraryRoot);
            return;
        }
        // make canonical names
        jukeboxRoot = new File(jukeboxRoot).getCanonicalPath();
        movieLibraryRoot = new File(movieLibraryRoot).getCanonicalPath(); 
        MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
        if (dumpLibraryStructure) {
            logger.warning("WARNING !!! A dump of your library directory structure will be generated for debug purpose. !!! Library won't be built or updated");
            ml.makeDumpStructure();
        } else {
            ml.generateLibrary(jukeboxPreserve);
        }

        fh.close();
        if (Boolean.parseBoolean(getProperty("mjb.appendDateToLogFile", "false"))) {
            // File (or directory) with old name
            File file = new File(logFilename);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kkmmss");
            logFilename = "moviejukebox_" + dateFormat.format(timeStart) + ".log";

            // File with new name
            File file2 = new File(logFilename);

            // Rename file (or directory)
            if (!file.renameTo(file2)) {
                logger.severe("Error renaming log file.");
            }
        }

        return;
    }

    private void makeDumpStructure() {
        logger.finest("Dumping library directory structure for debug");

        for (final MediaLibraryPath mediaLibrary : movieLibraryPaths) {
            String mediaLibraryRoot = mediaLibrary.getPath();
            logger.finest("Dumping media library " + mediaLibraryRoot);
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

                        // Copy nfo / properties / .xml
                        if (ArrayUtils.contains(extensionToCopy, fileName.substring(fileName.length() - 3))) {
                            logger.info("Coyping " + file + " to " + newFile);
                            FileTools.copyFile(file, newFile);
                        } else {
                            logger.info("Creating dummy for " + file);
                        }
                    }
                    //newFile.deleteOnExit();
                } else {
                    logger.finest("Excluding : " + file);
                }
            } catch (IOException e) {
                logger.severe("Dump error : " + e.getMessage());
            }
        }
    }

    public static String[] tokenizeToArray(String str, String delim) {
        StringTokenizer st = new StringTokenizer(str, delim);
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        final String[] array = keywords.toArray(new String[] {});
        return array;
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
    }

    private MovieJukebox(String source, String jukeboxRoot) {
        this.movieLibraryRoot = source;
        this.jukeboxRoot = jukeboxRoot;
        this.detailsDirName = getProperty("mjb.detailsDirName", "Jukebox");
        this.forcePosterOverwrite = parseBoolean(getProperty("mjb.forcePostersOverwrite", "false"));
        this.forceThumbnailOverwrite = parseBoolean(getProperty("mjb.forceThumbnailsOverwrite", "false"));
        this.forceBannerOverwrite = parseBoolean(getProperty("mjb.forceBannersOverwrite", "false"));
        this.skinHome = getProperty("mjb.skin.dir", "./skins/default");

        this.fanartMovieDownload = parseBoolean(getProperty("fanart.movie.download", "false"));
        this.fanartTvDownload = parseBoolean(getProperty("fanart.tv.download", "false"));

        this.setIndexFanart = parseBoolean(getProperty("mjb.sets.indexFanart", "false"));
        
        this.recheckXML = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.recheck.XML", "true"));

        fanartToken = getProperty("mjb.scanner.fanartToken", ".fanart");
        bannerToken = getProperty("mjb.scanner.bannerToken", ".banner");
        posterToken = getProperty("mjb.scanner.posterToken", "_large");
        thumbnailToken = getProperty("mjb.scanner.thumbnailToken", "_small");
        videoimageToken = getProperty("mjb.scanner.videoimageToken", ".videoimage");

        File f = new File(source);
        if (f.exists() && f.isFile() && source.toUpperCase().endsWith("XML")) {
            logger.finest("Parsing library file : " + source);
            movieLibraryPaths = parseMovieLibraryRootFile(f);
        } else if (f.exists() && f.isDirectory()) {
            logger.finest("Library path is : " + source);
            movieLibraryPaths = new ArrayList<MediaLibraryPath>();
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
            movieLibraryPaths.add(mlp);
        }
    }

    @XmlRootElement(name = "jukebox")
    public static class JukeboxXml {
        @SuppressWarnings("unused")
        @XmlElement
        private Collection<Movie> movies;
    }

    private void generateLibrary(boolean jukeboxPreserve) throws Throwable {

        /********************************************************************************
         * @author Gabriel Corneanu: the tools used for parallel processing are NOT thread safe (some operations are, but not all) therefore all are added to a
         *         container which is instantiated one per thread
         * 
         *         - xmlWriter looks thread safe - htmlWriter was not thread safe, getTransformer is fixed (simple workaround) - MovieImagePlugin : not clear,
         *         made thread specific for safety - MediaInfoScanner : not sure, made thread specific
         * 
         *         Also important: - the library itself is not thread safe for modifications (API says so) it could be adjusted with concurrent versions, but it
         *         needs many changes it seems that it is safe for subsequent reads (iterators), so leave for now... - DatabasePluginController is also fixed to
         *         be thread safe (plugins map for each thread)
         * 
         */
        class ToolSet {
            public MovieImagePlugin imagePlugin = getImagePlugin(getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
            public MovieImagePlugin backgroundPlugin = getBackgroundPlugin(getProperty("mjb.background.plugin",
                            "com.moviejukebox.plugin.DefaultBackgroundPlugin"));
            public MediaInfoScanner miScanner = new MediaInfoScanner();
            public AppleTrailersPlugin trailerPlugin = new AppleTrailersPlugin();
            public OpenSubtitlesPlugin subtitlePlugin = new OpenSubtitlesPlugin();
        }

        final ThreadLocal<ToolSet> threadTools = new ThreadLocal<ToolSet>() {
            protected ToolSet initialValue() {
                return new ToolSet();
            };
        };

        // final ToolSet localtools = threadTools.get();

        final MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        File mediaLibraryRoot = new File(movieLibraryRoot);
        final String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;
        final File jukeboxDetailsRootFile = new FileTools.FileEx(jukeboxDetailsRoot);

        MovieListingPlugin listingPlugin = getListingPlugin(getProperty("mjb.listing.plugin", "com.moviejukebox.plugin.MovieListingPluginBase"));
        this.moviejukeboxListing = parseBoolean(getProperty("mjb.listing.generate", "false"));

        videoimageDownload = parseBoolean(getProperty("mjb.includeVideoImages", "false"));
        bannerDownload = parseBoolean(getProperty("mjb.includeWideBanners", "false"));
        jukeboxTempDir = PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp");
        
        // Multi-thread: Processing thread settings
        int MaxThreadsScan = Integer.parseInt(getProperty("mjb.MaxThreadsScan", "4"));
        int MaxThreadsProcess = Integer.parseInt(getProperty("mjb.MaxThreadsProcess", Integer.toString(Runtime.getRuntime().availableProcessors())));
        logger.fine("Using " + MaxThreadsScan + " scanning threads and " + MaxThreadsProcess + " processing threads...");
        if (MaxThreadsScan + MaxThreadsProcess == 2) {
            // Display the note about the performance, otherwise assume that the user knows how to change
            // these parameters as they aren't set to the minimum
            logger.fine("See README.TXT for increasing performance using these settings.");
        }
        int nbFiles = 0;

        /********************************************************************************
         * 
         * PART 1 : Preparing the temporary environment
         * 
         */
        logger.fine("Preparing environment...");
        
        // Create the ".mjbignore" file in the jukebox folder
        jukeboxDetailsRootFile.mkdirs();
        new File(jukeboxDetailsRootFile, ".mjbignore").createNewFile();
        FileTools.addJukeboxFile(".mjbignore");

        logger.fine("Initializing...");
        final String tempJukeboxRoot = jukeboxTempDir;
        FileTools.deleteDir(tempJukeboxRoot);
        
        final String tempJukeboxDetailsRoot = tempJukeboxRoot + File.separator + detailsDirName;
        File tempJukeboxDetailsRootFile = new File(tempJukeboxDetailsRoot);

        // Try and create the temp directory
        boolean status = tempJukeboxDetailsRootFile.mkdirs();
        int i = 1;
        while (!status && i++ <= 10) {
            Thread.sleep(1000);
            status = tempJukeboxDetailsRootFile.mkdirs();
        }

        /********************************************************************************
         * 
         * PART 2 : Scan movie libraries for files...
         * 
         */
        logger.fine("Scanning library directory " + mediaLibraryRoot);
        logger.fine("Jukebox output goes to " + jukeboxRoot);
        FileTools.fileCache.addDir(jukeboxDetailsRootFile, false);

        int threadsMaxDirScan = movieLibraryPaths.size();
        if (threadsMaxDirScan < 1)
            threadsMaxDirScan = 1;

        ThreadExecutor<Void> tasks = new ThreadExecutor<Void>(threadsMaxDirScan);
        final Library library = new Library();
        for (final MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
            // Multi-thread parallel processing
            tasks.submit(new Callable<Void>() {
                public Void call() {
                    logger.finer("Scanning media library " + mediaLibraryPath.getPath());
                    MovieDirectoryScanner mds = new MovieDirectoryScanner();
                    // scan uses synchronized method Library.addMovie
                    mds.scan(mediaLibraryPath, library);
                    System.out.print("\n");
                    return null;
                };
            });
        }
        ;
        tasks.waitFor();

        // If the user asked to preserve the existing movies, scan the output directory as well
        if (jukeboxPreserve) {
            logger.fine("Scanning output directory for additional movies");
            OutputDirectoryScanner ods = new OutputDirectoryScanner(jukeboxRoot + File.separator + detailsDirName);
            ods.scan(library);
        }

        // Now that everything's been scanned, merge the trailers into the movies
        library.mergeExtras();

        logger.fine("Found " + library.size() + " movies in your media library");
        logger.fine("Stored " + FileTools.fileCache.size() + " files in the info cache");

        tasks = new ThreadExecutor<Void>(MaxThreadsScan);

        if (library.size() > 0) {
            logger.fine("Searching for movies information...");
            int movieCounter = 0;
            for (final Movie movie : library.values()) {
                final int count = ++movieCounter;
                final String movieTitleExt = movie.getOriginalTitle() + (movie.isTVShow() ? (" [Season " + movie.getSeason() + "]") : "")
                                + (movie.isExtra() ? " [Extra]" : "");

                // Multi-thread parallel processing
                tasks.submit(new Callable<Void>() {

                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();
                        // First get movie data (title, year, director, genre, etc...)
                        logger.fine("Updating: " + movieTitleExt);

                        updateMovieData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                        // Then get this movie's poster
                        logger.finer("Updating poster for: " + movieTitleExt);
                        updateMoviePoster(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                        // Download episode images if required
                        if (videoimageDownload) {
                            VideoImageScanner.scan(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                        }

                        // TODO remove these checks once all skins have transitioned to the new format
                        fanartMovieDownload = ImdbPlugin.checkDownloadFanart(movie.isTVShow());
                        fanartTvDownload = ImdbPlugin.checkDownloadFanart(movie.isTVShow());

                        // Get Fanart only if requested
                        // Note that the FanartScanner will check if the file is newer / different
                        if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                            FanartScanner.scan(tools.backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                        }

                        // Get Banner if requested and is a TV show
                        if (bannerDownload && movie.isTVShow()) {
                            if (!BannerScanner.scan(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                                updateTvBanner(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                            }
                        }

                        // Get subtitle
                        tools.subtitlePlugin.generate(movie);

                        // Get Trailer
                        tools.trailerPlugin.generate(movie);

                        logger.fine("Finished: " + movieTitleExt + " (" + count + "/" + library.size() + ")");

                        return null;
                    };
                });
            }
            tasks.waitFor();

            // re-run the merge in case there were additional trailers that were downloaded
            library.mergeExtras();

            OpenSubtitlesPlugin.logOut();

            /********************************************************************************
             * 
             * PART 3 : Indexing the library
             * 
             */

            // This is for programs like NMTServer where they don't need the indexes.
            if (skipIndexGeneration) {
                logger.fine("Indexing of libraries skipped.");
            } else {
                logger.fine("Indexing libraries...");
                library.buildIndex(MaxThreadsProcess);
            }

            logger.fine("Indexing masters...");
            /*
             * This is kind of a hack -- library.values() are the movies that were found in the library and library.getMoviesList() are the ones that are there
             * now. So the movies that are in getMoviesList but not in values are the index masters.
             */
            Collection<Movie> movies = library.values();
            List<Movie> indexMasters = new ArrayList<Movie>(library.getMoviesList());
            indexMasters.removeAll(movies);

            JAXBContext context = JAXBContext.newInstance(JukeboxXml.class);
            final JukeboxXml jukeboxXml = new JukeboxXml();
            jukeboxXml.movies = library.values();

            // Multi-thread: Parallel Executor
            tasks = new ThreadExecutor<Void>(MaxThreadsProcess);

            for (final Movie movie : indexMasters) {
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {
                        ToolSet tools = threadTools.get();

                        String safeSetMasterBaseName = FileTools.makeSafeFilename(movie.getBaseName());

                        // The master's movie XML is used for generating the
                        // playlist it will be overwritten by the index XML
                        
                        logger.finest("Writing index data for master: " + movie.getBaseName());
                        xmlWriter.writeMovieXML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, library);

                        logger.finer("Updating poster for index master: " + movie.getOriginalTitle() + "...");

                        // If we can find a set poster file, use it; otherwise, stick with the first movie's poster
                        String oldPosterFilename = movie.getPosterFilename();

                        // Set a default poster name in case it's not found during the scan
                        movie.setPosterFilename(safeSetMasterBaseName + "." + getProperty("posters.format", "jpg"));
                        if (!PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                            logger.finest("Local set poster (" + safeSetMasterBaseName + ") not found, using " + oldPosterFilename);
                            movie.setPosterFilename(oldPosterFilename);
                        }

                        // If this is a TV Show and we want to download banners, then also check for a banner Set file
                        if (movie.isTVShow() && bannerDownload) {
                            // Set a default banner filename in case it's not found during the scan
                            movie.setBannerFilename(safeSetMasterBaseName + bannerToken + ".jpg");
                            if (!BannerScanner.scan(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                                updateTvBanner(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                                logger.finest("Local set banner (" + safeSetMasterBaseName + bannerToken + ") not found, using "
                                                + oldPosterFilename);
                            } else {
                                logger.finest("Local set banner found, using " + movie.getBannerFilename());
                            }
                        }

                        // Check for Set Fanart
                        if (setIndexFanart) {
                            // Set a default fanart filename in case it's not found during the scan
                            movie.setFanartFilename(safeSetMasterBaseName + fanartToken + ".jpg");
                            if (!FanartScanner.scan(tools.backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                                logger.finest("Local set fanart (" + safeSetMasterBaseName + fanartToken + ") not found, using "
                                                + oldPosterFilename);
                            } else {
                                logger.finest("Local set fanart found, using " + movie.getFanartFilename());
                            }
                        }

                        String thumbnailExtension = getProperty("thumbnails.format", "png");
                        movie.setThumbnailFilename(safeSetMasterBaseName + thumbnailToken + "." + thumbnailExtension);
                        String posterExtension = getProperty("posters.format", "png");
                        movie.setDetailPosterFilename(safeSetMasterBaseName + posterToken + "." + posterExtension);

                        if (Boolean.parseBoolean(getProperty("mjb.sets.createPosters", "false"))) {
                            // Create a detail poster for each movie
                            logger.finest("Creating detail poster for index master: " + movie.getBaseName());
                            createPoster(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);
                        }

                        // Create a thumbnail for each movie
                        logger.finest("Creating thumbnail for index master: " + movie.getBaseName() + ", isTV: " + movie.isTVShow() + ", isHD: " + movie.isHD());
                        createThumbnail(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);

                        // No playlist for index masters
                        // htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    };
                });
            }
            tasks.waitFor();

            logger.fine("Writing movie data...");
            // Multi-thread: Parallel Executor
            tasks.restart();
            for (final Movie movie : movies) {
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();
                        // Update movie XML files with computed index information
                        logger.finest("Writing index data to movie: " + movie.getBaseName());
                        xmlWriter.writeMovieXML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, library);

                        // Create a detail poster for each movie
                        logger.finest("Creating detail poster for movie: " + movie.getBaseName());
                        createPoster(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);

                        // Create a thumbnail for each movie
                        logger.finest("Creating thumbnails for movie: " + movie.getBaseName());
                        createThumbnail(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);

                        if (!skipIndexGeneration) {
                            // write the movie details HTML
                            htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                            // write the playlist for the movie if needed
                            FileTools.addJukeboxFiles(htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie));
                        }
                        // Add all the movie files to the exclusion list
                        FileTools.addMovieToJukeboxFilenames(movie);

                        return null;
                    };
                });
            }
            tasks.waitFor();

            if (!skipIndexGeneration) {
                logger.fine("Writing Indexes XML...");
                xmlWriter.writeIndexXML(tempJukeboxDetailsRoot, detailsDirName, library, MaxThreadsProcess);
                logger.fine("Writing Category XML...");
                xmlWriter.writeCategoryXML(tempJukeboxRoot, detailsDirName, library);
                logger.fine("Writing Indexes HTML...");
                htmlWriter.generateMoviesIndexHTML(tempJukeboxRoot, detailsDirName, library, MaxThreadsProcess);
                logger.fine("Writing Category HTML...");
                htmlWriter.generateMoviesCategoryHTML(tempJukeboxRoot, detailsDirName, library);
            }

            try {
                final String totalMoviesXmlFileName = "CompleteMovies.xml";
                final File totalMoviesXmlFile = new File(tempJukeboxDetailsRoot, totalMoviesXmlFileName);
                
                OutputStream marStream = FileTools.createFileOutputStream(totalMoviesXmlFile, 1024*1024);
                context.createMarshaller().marshal(jukeboxXml, marStream);
                marStream.close();
                FileTools.addJukeboxFile(totalMoviesXmlFileName);
                Transformer transformer = getTransformer(new File("rss.xsl"), jukeboxDetailsRoot);

                final String rssXmlFileName = "RSS.xml";
                FileTools.addJukeboxFile(rssXmlFileName);
                Result xmlResult = new StreamResult(new File(tempJukeboxDetailsRoot, rssXmlFileName));
                transformer.transform(new StreamSource(totalMoviesXmlFile), xmlResult);
            } catch (Exception e) {
                logger.finest("RSS is not generated." /* + e.getStackTrace().toString() */);
            }

            /********************************************************************************
             * 
             * PART 4 : Copy files to target directory
             * 
             */
            logger.fine("Copying new files to Jukebox directory...");
            String index = getProperty("mjb.indexFile", "index.htm");
            FileTools.copyDir(tempJukeboxDetailsRoot, jukeboxDetailsRoot, true);
            FileTools.copyFile(new File(tempJukeboxRoot + File.separator + index), new File(jukeboxRoot + File.separator + index));

            logger.fine("Copying resources to Jukebox directory...");
            FileTools.copyDir(skinHome + File.separator + "html", jukeboxDetailsRoot, true);

            /********************************************************************************
             * 
             * PART 5: Clean-up the jukebox directory
             * 
             */
            if (jukeboxClean) {
                logger.fine("Cleaning up the jukebox directory...");
                Collection<String> generatedFileNames = FileTools.getJukeboxFiles();
                
                File[] cleanList = new File(jukeboxDetailsRoot).listFiles();
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
                            logger.finest("Deleted: " + cleanList[nbFiles].getName() + " from library");
                            cleanDeletedTotal++;
                            cleanList[nbFiles].delete();
                        }
                    }
                }
                logger.fine(Integer.toString(nbFiles) + " files in the jukebox directory");
                if (cleanDeletedTotal > 0 )
                    logger.fine("Deleted " + Integer.toString(cleanDeletedTotal) + " unused " + (cleanDeletedTotal==1?"file":"files") + " from the jukebox directory");
            } else {
                logger.fine("Jukebox cleaning skipped");
            }

            if (moviejukeboxListing) {
                logger.fine("Generating listing output...");
                listingPlugin.generate(tempJukeboxRoot, jukeboxRoot, library);
            }

            logger.fine("Clean up temporary files");
            File rootIndex = new File(tempJukeboxRoot + File.separator + index);
            rootIndex.delete();

            tasks.waitFor();
            FileTools.deleteDir(jukeboxTempDir);
        }
        timeEnd = System.currentTimeMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        logger.fine("");
        logger.fine("MovieJukebox process completed at " + new Date());
        logger.fine("Processing took " + dateFormat.format(new Date(timeEnd - timeStart)));

        return;
    }

    /**
     * Generates a movie XML file which contains data in the <tt>Movie</tt> bean.
     * 
     * When an XML file exists for the specified movie file, it is loaded into the specified <tt>Movie</tt> object.
     * 
     * When no XML file exist, scanners are called in turn, in order to add information to the specified <tt>movie</tt> object. Once scanned, the <tt>movie</tt>
     * object is persisted.
     */
    public void updateMovieData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot,
                    String tempJukeboxDetailsRoot, Movie movie) throws FileNotFoundException, XMLStreamException {

        boolean forceXMLOverwrite = parseBoolean(getProperty("mjb.forceXMLOverwrite", "false"));
        boolean checkNewer = parseBoolean(getProperty("filename.nfo.checknewer", "true"));

        /*
         * For each video in the library, if an XML file for this video already exists, then there is no need to search for the video file information, just
         * parse the XML data.
         */
        String safeBaseName = FileTools.makeSafeFilename(movie.getBaseName());
        File xmlFile = FileTools.fileCache.getFile(jukeboxDetailsRoot + File.separator + safeBaseName + ".xml");

        // See if we can find the NFO associated with this video file.
        List<File> nfoFiles = MovieNFOScanner.locateNFOs(movie);

        // Only check the NFO files if the XML exists and the CheckNewer parameter is set
        if (checkNewer && xmlFile.exists() ) {
            for (File nfoFile : nfoFiles) {
                // Only re-scan the nfo files if one of them is newer
                if (FileTools.isNewer(nfoFile, xmlFile)) {
                    logger.fine("NFO for " + movie.getOriginalTitle() + " (" + nfoFile.getAbsolutePath() + ") has changed, will rescan file.");
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
        
        // Only parse the XML file if we mean to update the XML file.
        if (xmlFile.exists() && !forceXMLOverwrite) {
            // parse the XML file
            logger.finer("XML file found for " + movie.getBaseName());
            xmlWriter.parseMovieXML(xmlFile, movie);
            if (recheckXML && mjbRecheck(movie)) {
                logger.fine("Recheck of " + movie.getBaseName() + " required");
                forceXMLOverwrite = true;
                movie.setDirty(true);
            }
        }

        // ForceBannerOverwrite is set here to force the re-load of TV Show data including the banners
        if (xmlFile.exists() && !forceXMLOverwrite && !(movie.isTVShow() && forceBannerOverwrite)) {
            // *** START of routine to check if the file has changed location
            // Set up some arrays to store the directory scanner files and the xml files
            Collection<MovieFile> scannedFiles = new ArrayList<MovieFile>();
            Collection<MovieFile> xmlFiles = new ArrayList<MovieFile>();

            // Copy the current movie files to a new collection (
            for (MovieFile part : movie.getMovieFiles())
                scannedFiles.add(part);

            // Copy the XML movie files to a new collection
            for (MovieFile part : movie.getMovieFiles())
                xmlFiles.add(part);

            // Now compare the before and after files
            Iterator<MovieFile> scanLoop = scannedFiles.iterator();
            String scannedFilename;

            for (MovieFile xmlLoop : xmlFiles) {
                //TODO: Detect the change of library location and update the path accordingly
                
                if (scanLoop.hasNext())
                    scannedFilename = scanLoop.next().getFilename();
                else
                    break; // No more files, so quit

                if (!scannedFilename.equalsIgnoreCase(xmlLoop.getFilename())) {
                    logger.finest("Detected change of file location to: " + scannedFilename);
                    xmlLoop.setFilename(scannedFilename);
                    movie.addMovieFile(xmlLoop);
                }
            }
            // *** END of file location change

            // update new episodes titles if new MovieFiles were added
            DatabasePluginController.scanTVShowTitles(movie);

            // Update thumbnails format if needed
            String thumbnailExtension = getProperty("thumbnails.format", "png");
            movie.setThumbnailFilename(movie.getBaseName() + thumbnailToken + "." + thumbnailExtension);
            // Update poster format if needed
            String posterExtension = getProperty("posters.format", "png");
            movie.setDetailPosterFilename(movie.getBaseName() + posterToken + "." + posterExtension);

            // Check for local CoverArt
            PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

        } else {
            // No XML file for this movie. 
            // We've got to find movie information where we can (filename, IMDb, NFO, etc...) Add here extra scanners if needed.
            if (forceXMLOverwrite) {
                logger.finer("Rescanning internet for information on " + movie.getBaseName());
            } else {
                logger.finer("Jukebox XML file not found. Scanning for information on " + movie.getBaseName());
            }

            // Changing call order, first MediaInfo then NFO. NFO will overwrite every info it will contains.
            miScanner.scan(movie);
            
            MovieNFOScanner.scan(movie, nfoFiles);

            // Added forceXMLOverwrite for issue 366
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN) || movie.isDirtyPoster()) {
                PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }
            
            DatabasePluginController.scan(movie);
            // Issue 1323:      Posters not picked up from NFO file
            // Only search for poster if we didn't have already
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                PosterScanner.scan(movie);
            }

        }
    }

    /**
     * Update the movie poster for the specified movie. When an existing thumbnail is found for the movie, it is not overwritten, unless the
     * mjb.forceThumbnailOverwrite is set to true in the property file. When the specified movie does not contain a valid URL for the poster, a dummy image is
     * used instead.
     * 
     * @param tempJukeboxDetailsRoot
     */
    public void updateMoviePoster(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String posterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
        File posterFile = new File(jukeboxDetailsRoot + File.separator + posterFilename);
        File tmpDestFile = new File(tempJukeboxDetailsRoot + File.separator + posterFilename);

        // Check to see if there is a local poster.
        // Check to see if there are posters in the jukebox directories (target and temp)
        // Check to see if the local poster is newer than either of the jukebox posters
        // Download poster

        // Do not overwrite existing posters, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !posterFile.exists()) || (movie.isDirtyPoster()) || forcePosterOverwrite) {
            posterFile.getParentFile().mkdirs();

            if (movie.getPosterURL() == null || movie.getPosterURL().equals(Movie.UNKNOWN)) {
                logger.finest("Dummy image used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), tmpDestFile);
            } else {
                try {
                    // Issue 201 : we now download to local temp dir
                    logger.finest("Downloading poster for " + movie.getBaseName() + " to " + tmpDestFile.getName() + " [calling plugin]");
                    FileTools.downloadImage(tmpDestFile, movie.getPosterURL());
                    logger.finest("Downloaded poster for " + movie.getBaseName());
                } catch (Exception error) {
                    logger.finer("Failed downloading movie poster : " + movie.getPosterURL());
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
    public void updateTvBanner(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String bannerFilename = FileTools.makeSafeFilename(movie.getBannerFilename());
        File bannerFile = FileTools.fileCache.getFile(jukeboxDetailsRoot + File.separator + bannerFilename);
        File tmpDestFile = new File(tempJukeboxDetailsRoot + File.separator + bannerFilename);

        // Check to see if there is a local banner.
        // Check to see if there are banners in the jukebox directories (target and temp)
        // Check to see if the local banner is newer than either of the jukebox banners
        // Download banner

        // Do not overwrite existing banners, unless there is a new poster URL in the nfo file.
        if ((!tmpDestFile.exists() && !bannerFile.exists()) || (movie.isDirtyBanner()) || forceBannerOverwrite) {
            bannerFile.getParentFile().mkdirs();

            if (movie.getBannerURL() == null || movie.getBannerURL().equals(Movie.UNKNOWN)) {
                logger.finest("Dummy banner used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), tmpDestFile);
            } else {
                try {
                    logger.finest("Downloading banner for " + movie.getBaseName() + " to " + tmpDestFile.getName() + " [calling plugin]");
                    FileTools.downloadImage(tmpDestFile, movie.getBannerURL());
                } catch (Exception error) {
                    logger.finer("Failed downloading banner: " + movie.getBannerURL());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_banner.jpg"), tmpDestFile);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) {
        Collection<MediaLibraryPath> mlp = new ArrayList<MediaLibraryPath>();

        if (!f.exists() || f.isDirectory()) {
            logger.severe("The moviejukebox library input file you specified is invalid: " + f.getName());
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
                if (scrapeLibraryString != null && !scrapeLibraryString.isEmpty()) {
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
                        logger.fine("Found media library: " + description);
                    } else {
                        logger.fine("Found media library: " + path);
                    }
                    // Save the media library to the log file for reference.
                    logger.finest("Media library: " + medlib);

                } else {
                    logger.fine("Skipped invalid media library: " + path);
                }
            }
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe("Failed parsing moviejukebox library input file: " + f.getName());
            logger.severe(eResult.toString());
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
            logger.severe("Failed instanciating imagePlugin: " + className);
            logger.severe("Default poster plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
            logger.severe("Failed instanciating BackgroundPlugin: " + className);
            logger.severe("Default background plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
            logger.severe("Failed instantiating ListingPlugin: " + className);
            logger.severe("NULL listing plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
    public static void createThumbnail(MovieImagePlugin imagePlugin, String rootPath, String tempRootPath, String skinHome, Movie movie,
                    boolean forceThumbnailOverwrite) {
        try {
            // TODO Move all temp directory code to FileTools for a cleaner method
            // Issue 201 : we now download to local temp directory
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String safeThumbnailFilename = FileTools.makeSafeFilename(movie.getThumbnailFilename());
            File src = new File(tempRootPath + File.separator + safePosterFilename);
            File oldsrc = FileTools.fileCache.getFile(rootPath + File.separator + safePosterFilename);
            String dst = tempRootPath + File.separator + safeThumbnailFilename;
            String olddst = rootPath + File.separator + safeThumbnailFilename;
            File fin;

            if (forceThumbnailOverwrite || !FileTools.fileCache.fileExists(olddst) || src.exists()) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (src.exists()) {
                    logger.finest("New file exists");
                    fin = src;
                } else {
                    logger.finest("Use old file");
                    fin = oldsrc;
                }

                BufferedImage bi = GraphicTools.loadJPEGImage(fin);
                if (bi == null) {
                    logger.info("Using dummy thumbnail image for " + movie.getOriginalTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator
                                    + safePosterFilename));
                    bi = GraphicTools.loadJPEGImage(src);
                }

                // Perspective code.
                String perspectiveDirection = getProperty("thumbnails.perspectiveDirection", "right");

                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror thumbnail name.
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));

                    // Generate left & save as copy
                    logger.finest("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = imagePlugin.generate(movie, bi, "thumbnails", "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }

                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                }

                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = imagePlugin.generate(movie, bi, "thumbnails", "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left thumbnail from " + src + " to " + dst);
                }
            }
        } catch (Exception error) {
            logger.severe("Failed creating thumbnail for " + movie.getOriginalTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
    public static void createPoster(MovieImagePlugin posterManager, String rootPath, String tempRootPath, String skinHome, Movie movie,
                    boolean forcePosterOverwrite) {
        try {
            // Issue 201 : we now download to local temporary directory
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String safeDetailPosterFilename = FileTools.makeSafeFilename(movie.getDetailPosterFilename());
            File   src = new File(tempRootPath + File.separator + safePosterFilename);
            File   oldsrc = FileTools.fileCache.getFile(rootPath + File.separator + safePosterFilename);
            String dst = tempRootPath + File.separator + safeDetailPosterFilename;
            String olddst = rootPath + File.separator + safeDetailPosterFilename;
            File fin;

            if (forcePosterOverwrite || !FileTools.fileCache.fileExists(olddst) || src.exists()){
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (src.exists()) {
                    logger.finest("New file exists (" + src + ")");
                    fin = src;
                } else {
                    logger.finest("Using old file (" + oldsrc + ")");
                    fin = oldsrc;
                }

                BufferedImage bi = GraphicTools.loadJPEGImage(fin);

                if (bi == null) {
                    logger.info("Using dummy poster image for " + movie.getOriginalTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), oldsrc);
                    bi = GraphicTools.loadJPEGImage(src);
                }
                logger.finest("Generating poster from " + src + " to " + dst);

                // Perspective code.
                String perspectiveDirection = getProperty("posters.perspectiveDirection", "right");

                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror poster name.
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));

                    // Generate left & save as copy
                    logger.finest("Generating mirror poster from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = posterManager.generate(movie, bi, "posters", "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.finest("Generating right poster from " + src + " to " + dst);
                    bi = posterManager.generate(movie, bi, "posters", "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }

                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = posterManager.generate(movie, bi, "posters", "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right poster from " + src + " to " + dst);
                }

                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = posterManager.generate(movie, bi, "posters", "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left poster from " + src + " to " + dst);
                }
            }
        } catch (Exception error) {
            logger.severe("Failed creating poster for " + movie.getOriginalTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    /**
     * This function will validate the current movie object and return true if the movie needs to be re-scanned.
     * @param movie
     * @return
     */
    private static boolean mjbRecheck(Movie movie) {
        // Property variables
        int recheckMax = Integer.parseInt(PropertiesUtil.getProperty("mjb.recheck.Max", "50"));
        
        // Skip Extras (Trailers, etc)
        if (movie.isExtra()) {
            return false;
        }
        
        if (recheckCount >= recheckMax) {
            // We are over the recheck maximum, so we won't recheck again this run
            return false;
        } else if (recheckCount == recheckMax) {
            logger.finest("Recheck: Threshold of " + recheckMax + " rechecked movies reached. No more will be checked until the next run.");
            recheckCount++; // By incrementing this variable we will only display this message once.
            return false;
        }
        int recheckDays     = Integer.parseInt(PropertiesUtil.getProperty("mjb.recheck.Days", "30"));
        int recheckRevision = Integer.parseInt(PropertiesUtil.getProperty("mjb.recheck.Revision", "25"));
        boolean recheckVersion = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.recheck.Version", "true"));
        boolean recheckUnknown = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.recheck.Unknown", "true"));
        
        // Check for the version of YAMJ that wrote the XML file vs the current version
        //System.out.println("- mjbVersion : " + movie.getMjbVersion() + " (" + movie.getCurrentMjbVersion() + ")");
        if (recheckVersion && !movie.getMjbVersion().equalsIgnoreCase(movie.getCurrentMjbVersion())) {
            logger.finest("Recheck: " + movie.getBaseName() + " XML is from a previous version, will rescan");
            recheckCount++;
            return true;
        }
        
        // Check the revision of YAMJ that wrote the XML file vs the current revisions
        //System.out.println("- mjbRevision: " + movie.getMjbRevision() + " (" + movie.getCurrentMjbRevision() + ")");
        //System.out.println("- Difference : " + (Integer.parseInt(movie.getCurrentMjbRevision()) - Integer.parseInt(movie.getMjbRevision())) );
        String currentRevision = movie.getCurrentMjbRevision();
        String mjbRevision = movie.getMjbRevision();
        int revDiff = Integer.parseInt(currentRevision.equalsIgnoreCase(Movie.UNKNOWN) ? "0" : currentRevision) - Integer.parseInt(mjbRevision.equalsIgnoreCase(Movie.UNKNOWN) ? "0" : mjbRevision); 
        if (revDiff > recheckRevision) {
            logger.finest("Recheck: " + movie.getBaseName() + " XML is " + revDiff + " revisions old, will rescan");
            recheckCount++;
            return true;
        }
        
        // Check the date the xml file was written vs the current date
        if (recheckDays > 0) {
            //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currentDate = new Date();
            long dateDiff = (currentDate.getTime() - movie.getMjbGenerationDate().getTime()) / (1000 * 60 * 60 * 24);
            //System.out.println("- mjbGenDate : " + dateFormat.format(movie.getMjbGenerationDate()));
            //System.out.println("- CurrentDate: " + dateFormat.format(currentDate));
            //System.out.println("- Difference : " + dateDiff);
            if (dateDiff > recheckDays) {
                logger.finest("Recheck: " + movie.getBaseName() + " XML is " + dateDiff + " days old, will rescan");
                recheckCount++;
                return true;
            }
        }
        
        // Check for "UNKNOWN" values in the XML
        if (recheckUnknown) {
            if (movie.getTitle().equalsIgnoreCase(Movie.UNKNOWN)) {
                logger.finest("Recheck: " + movie.getBaseName() + " XML is missing the title, will rescan");
                recheckCount++;
                return true;
            }
            
            if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)) {
                logger.finest("Recheck: " + movie.getBaseName() + " XML is missing plot, will rescan");
                recheckCount++;
                return true;
            }
            
            if (movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                logger.finest("Recheck: " + movie.getBaseName() + " XML is missing year, will rescan");
                recheckCount++;
                return true;
            }
            
            if (movie.getGenres().isEmpty()) {
                logger.finest("Recheck: " + movie.getBaseName() + "XML is missing genres, will rescan");
                recheckCount++;
                return true;
            }
            
            if (movie.isTVShow()) {
                boolean recheckEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
                boolean recheckVideoImages  = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
                
                if (recheckEpisodePlots || recheckVideoImages) {
                    // scan the TV episodes
                    for (MovieFile mf : movie.getMovieFiles()) {
                        for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                            if (recheckEpisodePlots && mf.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                logger.finest("Recheck: " + movie.getBaseName() + " XML is missing TV plot, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            }
                            
                            if (recheckVideoImages && mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                logger.finest("Recheck: " + movie.getBaseName() + " XML is missing TV video image, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            }
                        }
                    }
                    //System.out.println(" TV Show checks out ok");
                }
            }
        }
        return false;
    }
}
