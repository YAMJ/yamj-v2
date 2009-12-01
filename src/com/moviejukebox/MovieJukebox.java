/*
 *      Copyright (c) 2004-2009 YAMJ Members
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
import static java.lang.Boolean.parseBoolean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

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
    private static String videoimageToken;
    private static String fanartToken;

    private boolean fanartMovieDownload;
    private boolean fanartTvDownload;
    private boolean videoimageDownload;
    private boolean bannerDownload;
    private boolean moviejukeboxListing;
    private static boolean skipIndexGeneration = false;

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

        // These are pulled from the Manifest.MF file that is created by the Ant build script
        String mjbVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
        String mjbRevision = MovieJukebox.class.getPackage().getImplementationVersion();
        String mjbBuildDate = MovieJukebox.class.getPackage().getImplementationTitle();
        // Just create a pretty underline.
        String mjbTitle = "";
        for (int i=1; i <= mjbVersion.length(); i++) {
            mjbTitle += "~";
        }
        
        logger.fine("Yet Another Movie Jukebox " + mjbVersion);
        logger.fine("~~~ ~~~~~~~ ~~~~~ ~~~~~~~ " + mjbTitle);
        logger.fine("http://code.google.com/p/moviejukebox/");
        logger.fine("Copyright (c) 2004-2009 YAMJ Members");
        logger.fine("");
        logger.fine("This software is licensed under a Creative Commons License");
        logger.fine("See this page: http://code.google.com/p/moviejukebox/wiki/License");
        logger.fine("");

        // Print the revision information if it was populated by Hudson CI
        if ( !((mjbRevision == null) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}"))) ) {
            logger.fine("  Revision: r" + mjbRevision);
            logger.fine("Build Date: " + mjbBuildDate);
            logger.fine("");
        }
        logger.fine("Processing started at " + new Date());
        logger.fine("");

        String movieLibraryRoot = null;
        String jukeboxRoot = null;
        boolean jukeboxClean = false;
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
        if (!PropertiesUtil.setPropertiesStreamName("./properties/moviejukebox-default.properties")) {
            return;
        }

        // Load the user properties file "moviejukebox.properties"
        // No need to abort if we don't find this file
        // Must be read before the skin, because this may contain an override skin
        PropertiesUtil.setPropertiesStreamName(propertiesName);
        
        // Grab the skin from the command-line properties
        if (cmdLineProps.containsKey("mjb.skin.dir")) {
            PropertiesUtil.setProperty("mjb.skin.dir", cmdLineProps.get("mjb.skin.dir"));
        }

        // Load the skin.properties file
        if (!PropertiesUtil.setPropertiesStreamName(getProperty("mjb.skin.dir", "./skins/default") + "/skin.properties")) {
            return;
        }

        // Load the apikeys.properties file
        if (!PropertiesUtil.setPropertiesStreamName("./properties/apikeys.properties")) {
            return;
        } else {
            // This is needed to update the static reference for the API Keys in the log formatted because the log formatter is initialised before the properties files are read
            LogFormatter.addApiKeys();
        }

        // Load the rest of the command-line properties
        for (Map.Entry<String, String> propEntry : cmdLineProps.entrySet()) {
            PropertiesUtil.setProperty(propEntry.getKey(), propEntry.getValue());
        }

        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
            sb.append(propEntry.getKey() + "=" + propEntry.getValue() + ",");
        }
        sb.replace(sb.length() - 1, sb.length(), "}");
        logger.finer("Properties: " + sb.toString());

        MovieNFOScanner.setForceNFOEncoding(getProperty("mjb.forceNFOEncoding", null));
        if ("AUTO".equalsIgnoreCase(MovieNFOScanner.getForceNFOEncoding())) {
            MovieNFOScanner.setForceNFOEncoding(null);
        }
        MovieNFOScanner.setFanartToken(fanartToken);
        MovieNFOScanner.setNFOdirectory(getProperty("filename.nfo.directory", ""));
        MovieNFOScanner.setParentDirs(parseBoolean(getProperty("filename.nfo.parentDirs", "false")));

        MovieFilenameScanner.setSkipKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords", ""), ",;| "));
        MovieFilenameScanner.setExtrasKeywords(tokenizeToArray(getProperty("filename.extras.keywords", "trailer,extra,bonus"), ",;| "));
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray(getProperty("filename.movie.versions.keywords", "remastered,directors cut,extended cut,final cut"), ",;|"));
        MovieFilenameScanner.setLanguageDetection(parseBoolean(getProperty("filename.scanner.language.detection", "true")));

        String temp = getProperty("sorting.strip.prefixes");
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                Movie.getSortIgnorePrefixes().add(token.toLowerCase());
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
        MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
        ml.generateLibrary(jukeboxClean, jukeboxPreserve);

        fh.close();
        if (Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.appendDateToLogFile", "false"))) {
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
            mlp.setNmtRootPath(getProperty("mjb.nmtRootPath", "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/"));
            mlp.setScrapeLibrary(true);
            mlp.setExcludes(new ArrayList<String>());
            movieLibraryPaths.add(mlp);
        }
    }

    private void generateLibrary(boolean jukeboxClean, boolean jukeboxPreserve) throws Throwable {

        /********************************************************************************
         * @author Gabriel Corneanu:
         * the tools used for parallel processing are NOT thread safe (some operations are, but not all)
         * therefore all are added to a container which is instantiated one per thread
         * 
         * - xmlWriter looks thread safe
         * - htmlWriter was not thread safe, getTransformer is fixed (simple workaround)
         * - MovieImagePlugin : not clear, made thread specific for safety
         * - MediaInfoScanner : not sure, made thread specific
         * 
         * Also important:
         * - the library itself is not thread safe for modifications (API says so)
         * it could be adjusted with concurrent versions, but it needs many changes
         * it seems that it is safe for subsequent reads (iterators), so leave for now...
         * - DatabasePluginController is also fixed to be thread safe (plugins map for each thread) 
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
            protected ToolSet initialValue() {return new ToolSet(); };
        };

//     final ToolSet localtools = threadTools.get();

        final MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        File mediaLibraryRoot = new File(movieLibraryRoot);
        final String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

        MovieListingPlugin listingPlugin = getListingPlugin(getProperty("mjb.listing.plugin",
                        "com.moviejukebox.plugin.MovieListingPluginBase"));
        this.moviejukeboxListing = parseBoolean(getProperty("mjb.listing.generate", "false"));

        videoimageDownload = parseBoolean(getProperty("mjb.includeVideoImages", "false"));
        bannerDownload = parseBoolean(getProperty("mjb.includeWideBanners", "false"));

        // Multi-thread: Processing thread settings
        int MaxThreadsScan = Integer.parseInt(getProperty("mjb.MaxThreadsScan", "4"));
        int MaxThreadsProcess = Integer.parseInt(getProperty("mjb.MaxThreadsProcess", Integer.toString(Runtime.getRuntime().availableProcessors())));
        logger.fine("Using " + MaxThreadsScan + " scanning threads and " + MaxThreadsProcess + " processing threads...");
        logger.fine("See README.TXT for increasing performance using these settings.");
        int nbFiles = 0;
        String cleanCurrent = "";
        String cleanCurrentExt = "";

        /********************************************************************************
         * 
         * PART 1 : Preparing the temporary environment
         * 
         */
        logger.fine("Preparing environment...");
        File tempJukeboxCleanFile = new File(jukeboxDetailsRoot);

        if (jukeboxClean && tempJukeboxCleanFile.exists()) {
            // Clear out the jukebox generated files to force them to be re-created.

            File[] cleanList = tempJukeboxCleanFile.listFiles();
            String skipPattStr = getProperty("mjb.clean.skip");
            Pattern skipPatt = null != skipPattStr ? Pattern.compile(skipPattStr, Pattern.CASE_INSENSITIVE) : null;

            //  an exception when trying to read the Jukebox directory Issue 850
            try {
                // This does nothing, but will generate an exception if the variable is null
                nbFiles = cleanList.length;
            } catch (Exception error) {
                logger.fine("Error writing to jukebox directory: " + jukeboxDetailsRoot);
                logger.fine("Possibly read-only or you don't have permission to write to the directory.");
                logger.fine("Process terminated with errors.");
                return;
            }

            for (nbFiles = 0; nbFiles < cleanList.length; nbFiles++) {
                // Scan each file in here
                if (cleanList[nbFiles].isFile()) {
                    cleanCurrent = cleanList[nbFiles].getName().toUpperCase();
                    if (cleanCurrent.indexOf(".") > 0) {
                        cleanCurrentExt = cleanCurrent.substring(cleanCurrent.lastIndexOf("."));
                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("."));
                    } else {
                        cleanCurrentExt = "";
                    }

                    boolean skip = true;
                    if (".XML".equals(cleanCurrentExt) || ".HTML".equals(cleanCurrentExt)) {
                        for (String prefix : Library.getPrefixes()) {
                            if (cleanCurrent.toUpperCase().startsWith(prefix + "_")) {
                                skip = false;
                                break;
                            }
                        }

                        if (skip && "CATEGORIES".equals(cleanCurrent)) {
                            skip = false;
                        }
                    }

                    if (skip && null != skipPatt) {
                        skip = !skipPatt.matcher(cleanList[nbFiles].getName()).matches();
                    }

                    if (!skip) {
                        logger.finer("Pre-scan, deleting " + cleanList[nbFiles].getName());
                        cleanList[nbFiles].delete();
                    }
                }
            }
        }

        logger.fine("Initializing...");
        final String tempJukeboxRoot = "./temp";
        final String tempJukeboxDetailsRoot = tempJukeboxRoot + File.separator + detailsDirName;

        File tempJukeboxDetailsRootFile = new File(tempJukeboxDetailsRoot);
        if (tempJukeboxDetailsRootFile.exists()) {
            // Clean up
            File[] isoList = tempJukeboxDetailsRootFile.listFiles();
            for (nbFiles = 0; nbFiles < isoList.length; nbFiles++) {
                isoList[nbFiles].delete();
            }
            tempJukeboxDetailsRootFile.delete();
        }
        tempJukeboxDetailsRootFile.mkdirs();

        /********************************************************************************
         * 
         * PART 2 : Scan movie libraries for files...
         * 
         */
        logger.fine("Scanning library directory " + mediaLibraryRoot);
        logger.fine("Jukebox output goes to " + jukeboxRoot);

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
                    return null;
                };
            });
        };
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

        tasks = new ThreadExecutor<Void>(MaxThreadsScan);

        if (library.size() > 0) {
            logger.fine("Searching for movies information...");
            for (final Movie movie : library.values()) {

                // Multi-thread parallel processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();
                        // First get movie data (title, year, director, genre, etc...)
                        if (movie.isTVShow()) {
                            logger.fine("Updating data for: " + movie.getTitle() + " [Season " + movie.getSeason() + "]");
                        } else if (movie.isExtra()) {
                            logger.fine("Updating data for: " + movie.getTitle() + " [Extra]");
                        } else {
                            logger.fine("Updating data for: " + movie.getTitle());
                        }

                        updateMovieData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                        // Then get this movie's poster
                        logger.finer("Updating poster for: " + movie.getTitle() + "...");
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

                        if (movie.isTVShow()) {
                            logger.fine("Finished: " + movie.getTitle() + " [Season " + movie.getSeason() + "]");
                        } else if (movie.isExtra()) {
                            logger.fine("Finished: " + movie.getTitle() + " [Extra]");
                        } else {
                            logger.fine("Finished: " + movie.getTitle());
                        }

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
            List<Movie> indexMasters = new ArrayList<Movie>();
            indexMasters.addAll(library.getMoviesList());
            indexMasters.removeAll(movies);

            // Multi-thread: Parallel Executor
            tasks = new ThreadExecutor<Void>(MaxThreadsProcess);

            for (final Movie movie : indexMasters) {
                // Multi-tread: Start Parallel Processing
                tasks.submit(new Callable<Void>() {
                    public Void call() throws FileNotFoundException, XMLStreamException {

                        ToolSet tools = threadTools.get();
                        // The master's movie XML is used for generating the
                        // playlist it will be overwritten by the index XML
                        logger.finest("Writing index data for master: " + movie.getBaseName());
                        xmlWriter.writeMovieXML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, library);

                        logger.finer("Updating poster for index master: " + movie.getTitle() + "...");

                        // If we can find a set poster file, use it; otherwise, stick with the first movie's poster
                        String oldPosterFilename = movie.getPosterFilename();

                        // Set a default poster name in case it's not found during the scan
                        movie.setPosterFilename(movie.getBaseName() + "." + PropertiesUtil.getProperty("posters.format", "jpg"));
                        if (!PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                            logger.finest("Local set poster (" + FileTools.makeSafeFilename(movie.getBaseName()) + ") not found, using " + oldPosterFilename);
                            movie.setPosterFilename(oldPosterFilename);
                        }
                        
                        // If this is a TV Show and we want to download banners, then also check for a banner Set file
                        if (movie.isTVShow() && bannerDownload) {
                            // Set a default banner filename in case it's not found during the scan
                            movie.setBannerFilename(movie.getBaseName() + bannerToken + ".jpg");
                            if (!BannerScanner.scan(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                                updateTvBanner(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                                logger.finest("Local set banner (" + FileTools.makeSafeFilename(movie.getBaseName() + bannerToken) + ") not found, using " + oldPosterFilename);
                            } else {
                                logger.finest("Local set banner found, using " + movie.getBannerFilename());
                            }
                        }

                        String thumbnailExtension = getProperty("thumbnails.format", "png");
                        movie.setThumbnailFilename(movie.getBaseName() + thumbnailToken + "." + thumbnailExtension);
                        String posterExtension = getProperty("posters.format", "png");
                        movie.setDetailPosterFilename(movie.getBaseName() + posterToken + "." + posterExtension);

                        if (getProperty("mjb.sets.createPosters", "false").equalsIgnoreCase("true")) {
                            // Create a detail poster for each movie
                            logger.finest("Creating detail poster for index master: " + movie.getBaseName());
                            createPoster(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);
                        }

                        // Create a thumbnail for each movie
                        logger.finest("Creating thumbnail for index master: " + movie.getBaseName() + ", isTV: " + movie.isTVShow() + ", isHD: " + movie.isHD());
                        createThumbnail(tools.imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);

                        // No playlist for index masters
                        // htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                        return null;
                    };
                });
            }
            tasks.waitFor();

            xmlWriter.writePreferences(jukeboxDetailsRoot);

            logger.fine("Writing movie data...");

            final Collection<String> generatedFileNames = Collections.synchronizedCollection(new ArrayList<String>());
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
                            generatedFileNames.addAll(htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie));
                        }
                        return null;
                    };
                });
            }
            tasks.waitFor();

            if (!skipIndexGeneration) {
                logger.fine("Writing Indexes XML...");
                xmlWriter.writeIndexXML(tempJukeboxDetailsRoot, detailsDirName, library, MaxThreadsProcess);
                xmlWriter.writeCategoryXML(tempJukeboxRoot, detailsDirName, library);
                logger.fine("Writing Indexes HTML...");
                htmlWriter.generateMoviesIndexHTML(tempJukeboxRoot, detailsDirName, library, MaxThreadsProcess);
                htmlWriter.generateMoviesCategoryHTML(tempJukeboxRoot, detailsDirName, library);
            }

            /********************************************************************************
             * 
             * PART 4 : Copy files to target directory
             * 
             */
            logger.fine("Copying new files to Jukebox directory...");
            String index = getProperty("mjb.indexFile", "index.htm");
            FileTools.copyDir(tempJukeboxDetailsRoot, jukeboxDetailsRoot);
            FileTools.copyFile(new File(tempJukeboxRoot + File.separator + index), new File(jukeboxRoot + File.separator + index));

            logger.fine("Copying resources to Jukebox directory...");
            FileTools.copyDir(skinHome + File.separator + "html", jukeboxDetailsRoot);

            logger.fine("Clean up temporary files");
            File[] isoList = tempJukeboxDetailsRootFile.listFiles();
            for (nbFiles = 0; nbFiles < isoList.length; nbFiles++) {
                isoList[nbFiles].delete();
            }
            tempJukeboxDetailsRootFile.delete();
            File rootIndex = new File(tempJukeboxRoot + File.separator + index);
            rootIndex.delete();

            /********************************************************************************
             * 
             * PART 5: Clean-up the jukebox directory
             * 
             */
            if (jukeboxClean) {
                logger.fine("Cleaning up the jukebox directory...");

                // Change all the tokens to uppercase here to speed up processing.
                posterToken = posterToken.toUpperCase();
                thumbnailToken = thumbnailToken.toUpperCase();
                fanartToken = fanartToken.toUpperCase();
                bannerToken = bannerToken.toUpperCase();
                videoimageToken = videoimageToken.toUpperCase();

                // File tempJukeboxCleanFile = new File(jukeboxDetailsRoot);
                File[] cleanList = tempJukeboxCleanFile.listFiles();
                int cleanDeletedTotal = 0;

                String skipPattStr = getProperty("mjb.clean.skip");
                Pattern skipPatt = null != skipPattStr ? Pattern.compile(skipPattStr, Pattern.CASE_INSENSITIVE) : null;

                for (nbFiles = 0; nbFiles < cleanList.length; nbFiles++) {
                    // Scan each file in here
                    if (cleanList[nbFiles].isFile() && !generatedFileNames.contains(cleanList[nbFiles].getName())) {
                        cleanCurrent = cleanList[nbFiles].getName().toUpperCase();
                        if (cleanCurrent.indexOf(".") > 0) {
                            cleanCurrentExt = cleanCurrent.substring(cleanCurrent.lastIndexOf("."));
                            cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("."));
                        } else {
                            cleanCurrentExt = "";
                        }

                        boolean skip = false;

                        // Skip files related to the index pages
                        for (String prefix : Library.getPrefixes()) {
                            if (cleanCurrent.startsWith(prefix + "_")) {
                                skip = true;
                                break;
                            }
                        }

                        // If we're dealing with anything other than an HTML or XML file, its basename may need adjusting.
                        if (!skip) {
                            if (!".HTML".equals(cleanCurrentExt) && !".XML".equals(cleanCurrentExt)) {
                                if (!searchLibrary(cleanCurrent, library)) {
                                    if (cleanCurrent.endsWith(".PLAYLIST") && ".JSP".equals(cleanCurrentExt)) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - ".PLAYLIST".length());
                                    } else if (cleanCurrent.endsWith(posterToken)) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - posterToken.length());
                                    } else if (cleanCurrent.endsWith(thumbnailToken)) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - thumbnailToken.length());
                                    } else if (cleanCurrent.endsWith(fanartToken) && (fanartTvDownload || fanartMovieDownload)) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - fanartToken.length());
                                    } else if (cleanCurrent.endsWith(bannerToken) && bannerDownload) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - bannerToken.length());
                                    } else if ((cleanCurrent.indexOf(videoimageToken) > 0) && videoimageDownload) {
                                        cleanCurrent = cleanCurrent.substring(0, cleanCurrent.length() - videoimageToken.length());
                                        // This will skip all videoimages. not really what we want
                                        // Need to create an exclusion list that can be added to
                                        // during the regular scans
                                        skip = true; // TODO This is a hack
                                    }
                                }
                            } else if ("CATEGORIES".equals(cleanCurrent)) { // don't clean the categories.[xh]t?ml files
                                skip = true;
                            }
                        }

                        // If the file is in the skin's exclusion regex, skip it
                        if (!skip && null != skipPatt) {
                            skip = skipPatt.matcher(cleanList[nbFiles].getName()).matches();
                        }

                        // If the file isn't skipped and it's not part of the library, delete it
                        if (!skip && !searchLibrary(cleanCurrent, library)) {
                            logger.finest("Deleted: " + cleanList[nbFiles].getName() + " from library");
                            cleanDeletedTotal++;
                            cleanList[nbFiles].delete();
                        }
                    }
                }
                logger.fine(Integer.toString(nbFiles) + " files in the jukebox directory");
                logger.fine("Deleted " + Integer.toString(cleanDeletedTotal) + " files");
            } else {
                logger.fine("Jukebox cleaning skipped");
            }

            if (moviejukeboxListing) {
                logger.fine("Generating listing output...");
                listingPlugin.generate(tempJukeboxRoot, jukeboxRoot, library);
            }
        }

        timeEnd = System.currentTimeMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        logger.fine("");
        logger.fine("MovieJukebox process completed at " + new Date());
        logger.fine("Processing took " + dateFormat.format(new Date(timeEnd - timeStart)));
    }

    /**
     * Search the movie library for the passed movie name
     * 
     * @param slMovieName
     *            The name of the movie to match
     * @param library
     *            The library to search
     * @return true if found, false if not.
     */
    private Boolean searchLibrary(String slMovieName, Library library) {
        slMovieName = slMovieName.toUpperCase();
        for (Movie movie : library.values()) {
            if (FileTools.makeSafeFilename(movie.getBaseName()).toUpperCase().equals(slMovieName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a movie XML file which contains data in the <tt>Movie</tt> bean.
     * 
     * When an XML file exists for the specified movie file, it is loaded into the specified <tt>Movie</tt> object.
     * 
     * When no XML file exist, scanners are called in turn, in order to add information to the specified <tt>movie</tt> object. Once scanned, the <tt>movie</tt>
     * object is persisted.
     */
    private void updateMovieData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot,
                    String tempJukeboxDetailsRoot, Movie movie) throws FileNotFoundException, XMLStreamException {

        boolean forceXMLOverwrite = parseBoolean(getProperty("mjb.forceXMLOverwrite", "false"));
        boolean checkNewer = parseBoolean(getProperty("filename.nfo.checknewer", "true"));

        /*
         * For each video in the library, if an XML file for this video already exists, then there is no need to search for the video file information, just
         * parse the XML data.
         */
        String safeBaseName = FileTools.makeSafeFilename(movie.getBaseName());
        File xmlFile = new File(jukeboxDetailsRoot + File.separator + safeBaseName + ".xml");

        // See if we can find the NFO associated with this video file.
        List<File> nfoFiles = MovieNFOScanner.locateNFOs(movie);

        for (File nfoFile : nfoFiles) {
            // Only re-scan the nfo files if one of them is newer and the xml file exists (2nd run or greater)
            if (FileTools.isNewer(nfoFile, xmlFile) && checkNewer && xmlFile.exists()) {
                logger.fine("NFO for " + movie.getTitle() + " has changed, will rescan file.");
                movie.setDirtyNFO(true);
                movie.setDirtyPoster(true);
                movie.setDirtyFanart(true);
                movie.setDirtyBanner(true);
                forceXMLOverwrite = true;
                break; // one is enough
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

            // parse the XML file
            logger.finer("XML file found for " + movie.getBaseName());
            xmlWriter.parseMovieXML(xmlFile, movie);

            // Copy the xml movie files to a new collection
            for (MovieFile part : movie.getMovieFiles())
                xmlFiles.add(part);

            // Now compare the before and after files
            Iterator<MovieFile> scanLoop = scannedFiles.iterator();
            String scannedFilename;

            for (MovieFile xmlLoop : xmlFiles) {
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
            /*
             * No XML file for this movie. We've got to find movie information where we can (filename, IMDb, NFO, etc...) Add here extra scanners if needed.
             */
            if (forceXMLOverwrite) {
                logger.finer("Rescanning internet for information on " + movie.getBaseName());
            } else {
                logger.finer("XML file not found. Scanning internet for information on " + movie.getBaseName());
            }

            MovieNFOScanner.scan(movie, nfoFiles);

            // Added forceXMLOverwrite for issue 366
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN) || movie.isDirtyPoster()) {
                PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }
            
            miScanner.scan(movie);
            DatabasePluginController.scan(movie);
        }
    }

    /**
     * Update the movie poster for the specified movie. When an existing thumbnail is found for the movie, it is not overwritten, unless the
     * mjb.forceThumbnailOverwrite is set to true in the property file. When the specified movie does not contain a valid URL for the poster, a dummy image is
     * used instead.
     * 
     * @param tempJukeboxDetailsRoot
     */
    private void updateMoviePoster(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
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
    private void updateTvBanner(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String bannerFilename = FileTools.makeSafeFilename(movie.getBannerFilename());
        File bannerFile = new File(jukeboxDetailsRoot + File.separator + bannerFilename);
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
                String nmtpath = sub.getString("nmtpath");
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

                // Check that the nmtpath terminates with a "/" or "\"
                if (!(nmtpath.endsWith("/") || nmtpath.endsWith("\\"))) {
                    // This is the NMTPATH so add the unix path separator rather than File.separator
                    nmtpath = nmtpath + "/";
                }

                List<String> excludes = sub.getList("exclude[@name]");

                if (new File(path).exists()) {
                    MediaLibraryPath medlib = new MediaLibraryPath();
                    medlib.setPath(path);
                    medlib.setNmtRootPath(nmtpath);
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
            // Issue 201 : we now download to local temp directory
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String safeThumbnailFilename = FileTools.makeSafeFilename(movie.getThumbnailFilename());
            String src = tempRootPath + File.separator + safePosterFilename;
            String oldsrc = rootPath + File.separator + safePosterFilename;
            String dst = tempRootPath + File.separator + safeThumbnailFilename;
            String olddst = rootPath + File.separator + safeThumbnailFilename;
            FileInputStream fis;

            if (!(new File(olddst).exists()) || forceThumbnailOverwrite || (new File(src).exists())) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (new File(src).exists()) {
                    logger.finest("New file exists");
                    fis = new FileInputStream(src);
                } else {
                    logger.finest("Use old file");
                    fis = new FileInputStream(oldsrc);
                }

                BufferedImage bi = GraphicTools.loadJPEGImage(fis);
                if (bi == null) {
                    logger.info("Using dummy thumbnail image for " + movie.getTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator
                                    + safePosterFilename));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
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
            logger.severe("Failed creating thumbnail for " + movie.getTitle());
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
            String src = tempRootPath + File.separator + safePosterFilename;
            String oldsrc = rootPath + File.separator + safePosterFilename;
            String dst = tempRootPath + File.separator + safeDetailPosterFilename;
            String olddst = rootPath + File.separator + safeDetailPosterFilename;
            FileInputStream fis;

            if (!(new File(olddst).exists()) || forcePosterOverwrite || (new File(src).exists())) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefore check to see if they exist in the original directory
                if (new File(src).exists()) {
                    logger.finest("New file exists (" + src + ")");
                    fis = new FileInputStream(src);
                } else {
                    logger.finest("Using old file (" + oldsrc + ")");
                    fis = new FileInputStream(oldsrc);
                }

                BufferedImage bi = GraphicTools.loadJPEGImage(fis);

                if (bi == null) {
                    logger.info("Using dummy poster image for " + movie.getTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator
                                    + safePosterFilename));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
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
            logger.severe("Failed creating poster for " + movie.getTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }
}
