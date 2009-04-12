package com.moviejukebox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import com.moviejukebox.plugin.DefaultPosterPlugin;
import com.moviejukebox.plugin.DefaultThumbnailPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.MovieListingPlugin;
import com.moviejukebox.plugin.MovieListingPluginBase;
import com.moviejukebox.plugin.OpenSubtitlesPlugin;
import com.moviejukebox.plugin.SetThumbnailPlugin;
import com.moviejukebox.scanner.FanartScanner;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class MovieJukebox {

    private static Logger logger = Logger.getLogger("moviejukebox");
    Collection<MediaLibraryPath> movieLibraryPaths;
    private String movieLibraryRoot;
    private String jukeboxRoot;
    private String skinHome;
    private String detailsDirName;
    private boolean forceThumbnailOverwrite;
    private boolean forcePosterOverwrite;
    private boolean fanartDownload;
    private boolean videoImagesDownload;
    private boolean moviejukeboxListing;
    private OpenSubtitlesPlugin subtitlePlugin;
    private AppleTrailersPlugin trailerPlugin;

    public static void main(String[] args) throws XMLStreamException, SecurityException, IOException, ClassNotFoundException {
        // Send logger output to our FileHandler.

        Formatter mjbFormatter = new Formatter() {

            public synchronized String format(LogRecord record) {
                return record.getMessage() + (String) java.security.AccessController.doPrivileged(new PrivilegedAction<Object>() {

                    public Object run() {
                        return System.getProperty("line.separator");
                    }
                });
            }
        };

        FileHandler fh = new FileHandler("moviejukebox.log");
        fh.setFormatter(mjbFormatter);
        fh.setLevel(Level.ALL);

        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(mjbFormatter);
        ch.setLevel(Level.FINE);

        logger.setUseParentHandlers(false);
        logger.addHandler(fh);
        logger.addHandler(ch);
        logger.setLevel(Level.ALL);

        String movieLibraryRoot = null;
        String jukeboxRoot = null;
        boolean jukeboxClean = false;
        String propertiesName = "moviejukebox.properties";

        if (args.length == 0) {
            help();
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = (String) args[i];
                if ("-o".equalsIgnoreCase(arg)) {
                    jukeboxRoot = args[++i];
                } else if ("-c".equalsIgnoreCase(arg)) {
                    jukeboxClean = true;
                } else if ("-p".equalsIgnoreCase(arg)) {
                    propertiesName = args[++i];
                } else if (arg.startsWith("-")) {
                    help();
                    return;
                } else {
                    movieLibraryRoot = args[i];
                }
            }
        } catch (Exception e) {
            System.err.println("Wrong arguments specified");
            help();
            return;
        }

        if (!PropertiesUtil.setPropertiesStreamName(propertiesName)) {
            return;
        }
        
        MovieNFOScanner.setForceNFOEncoding(PropertiesUtil.getProperty("mjb.forceNFOEncoding", null));
        if ("AUTO".equalsIgnoreCase(MovieNFOScanner.getForceNFOEncoding())) {
        	MovieNFOScanner.setForceNFOEncoding(null);
        }
        MovieNFOScanner.setFanartToken(PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart"));
        MovieNFOScanner.setNFOdirectory(PropertiesUtil.getProperty("filename.nfo.directory", ""));

        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("filename.scanner.skip.keywords", ""), ",;| ");
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        MovieFilenameScanner.setSkipKeywords(keywords.toArray(new String[] {}));
        MovieFilenameScanner.setLanguageDetection(
        		Boolean.parseBoolean(PropertiesUtil.getProperty("filename.scanner.language.detection", 
        				"true")));

        String temp = PropertiesUtil.getProperty("sorting.strip.prefixes");
        if (temp != null) {
            st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                Movie.getSortIgnorePrefixes().add(token.toLowerCase());
            }
        }

        if (movieLibraryRoot == null) {
            movieLibraryRoot = PropertiesUtil.getProperty("mjb.libraryRoot");
            logger.fine("Got libraryRoot from properties file: " + movieLibraryRoot);
        }

        if (jukeboxRoot == null) {
            jukeboxRoot = PropertiesUtil.getProperty("mjb.jukeboxRoot");
            logger.fine("Got jukeboxRoot from properties file: " + jukeboxRoot);
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
            System.err.println("Directory not found : " + movieLibraryRoot);
            return;
        }
        MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
        ml.generateLibrary(jukeboxClean);
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
        System.out.println("  -p propertiesFile : OPTIONAL");
        System.out.println("                      The properties file to use instead of moviejukebox.properties");
    }

    private MovieJukebox(String source, String jukeboxRoot) {
        this.movieLibraryRoot = source;
        this.jukeboxRoot = jukeboxRoot;
        this.detailsDirName = PropertiesUtil.getProperty("mjb.detailsDirName", "Jukebox");
        this.forceThumbnailOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceThumbnailsOverwrite", "false"));
        this.forcePosterOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forcePostersOverwrite", "false"));
        this.skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        this.fanartDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));

        File f = new File(source);
        if (f.exists() && f.isFile() && source.toUpperCase().endsWith("XML")) {
            logger.finest("Parsing library file : " + source);
            movieLibraryPaths = parseMovieLibraryRootFile(f);
        } else if (f.exists() && f.isDirectory()) {
            logger.finest("Library path is : " + source);
            movieLibraryPaths = new ArrayList<MediaLibraryPath>();
            MediaLibraryPath mlp = new MediaLibraryPath();
            mlp.setPath(source);
            mlp.setNmtRootPath(PropertiesUtil.getProperty("mjb.nmtRootPath", "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/"));
            mlp.setScrapeLibrary(true);
            mlp.setExcludes(new ArrayList<String>());
            movieLibraryPaths.add(mlp);
        }
    }

    private void generateLibrary(boolean jukeboxClean) throws FileNotFoundException, XMLStreamException, ClassNotFoundException {
        MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        MovieImagePlugin thumbnailPlugin = this.getThumbnailPlugin(PropertiesUtil.getProperty("mjb.thumbnail.plugin",
                "com.moviejukebox.plugin.DefaultThumbnailPlugin"));
        MovieImagePlugin posterPlugin = this.getPosterPlugin(PropertiesUtil.getProperty("mjb.poster.plugin", "com.moviejukebox.plugin.DefaultPosterPlugin"));
        MovieImagePlugin backgroundPlugin = this.getBackgroundPlugin(PropertiesUtil.getProperty("mjb.background.plugin",
                "com.moviejukebox.plugin.DefaultBackgroundPlugin"));

        MovieDirectoryScanner mds = new MovieDirectoryScanner();
        MediaInfoScanner miScanner = new MediaInfoScanner();

        File mediaLibraryRoot = new File(movieLibraryRoot);
        String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

        subtitlePlugin = new OpenSubtitlesPlugin();
        trailerPlugin = new AppleTrailersPlugin();

        MovieListingPlugin listingPlugin = this.getListingPlugin(PropertiesUtil.getProperty("mjb.listing.plugin",
            "com.moviejukebox.plugin.MovieListingPluginBase"));
        this.moviejukeboxListing = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.listing.generate", "false"));

        videoImagesDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));

        int nbFiles = 0;
        String cleanCurrent = "";
        String cleanCurrentExt = "";

        // ////////////////////////////////////////////////////////////////
        // / PASS 0 : Preparing temporary environnement...
        //

        File tempJukeboxCleanFile = new File(jukeboxDetailsRoot);
        if (jukeboxClean && tempJukeboxCleanFile.exists()) {
            // Clear out the jukebox generated files to force them to be re-created.

            File[] cleanList = tempJukeboxCleanFile.listFiles();

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

                    if (cleanCurrent.equals("CATEGORIES")) {
                        cleanList[nbFiles].delete();
                    } else if (cleanCurrentExt.equals(".CSS") ||
                              (cleanCurrent.indexOf("GENRES_") >= 0) ||
                              (cleanCurrent.indexOf("OTHER_") >= 0) ||
                              (cleanCurrent.indexOf("RATING_") >= 0) ||
                              (cleanCurrent.indexOf("TITLE_") >= 0) ||
                              (cleanCurrent.indexOf("YEAR_") >= 0) ||
                              (cleanCurrent.indexOf("TVSERIES_") >= 0) ||
                              (cleanCurrent.indexOf("SET_") >= 0)) {
                        cleanList[nbFiles].delete();
                    }
                }
            }
        }

        logger.fine("Initializing...");
        String tempJukeboxRoot = "./temp";
        String tempJukeboxDetailsRoot = tempJukeboxRoot + File.separator + detailsDirName;

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

        // ////////////////////////////////////////////////////////////////
        // / PASS 1 : Scan movie libraries for files...
        //
        logger.fine("Scanning movies directory " + mediaLibraryRoot);
        logger.fine("Jukebox output goes to " + jukeboxRoot);

        Library library = new Library();
        for (MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
            logger.finer("Scanning media library " + mediaLibraryPath.getPath());
            library = mds.scan(mediaLibraryPath, library);
        }

        logger.fine("Found " + library.size() + " movies in your media library");
        
        if (library.size() > 0) {
            // ////////////////////////////////////////////////////////////////
            // / PASS 2 : Scan movie libraries for files...
            //
            logger.fine("Searching for movies information...");

            for (Movie movie : library.values()) {
                // First get movie data (title, year, director, genre, etc...)
                if ( movie.isTVShow() ) {
                    logger.fine("Updating data for: " + movie.getTitle() + " [Season " + movie.getSeason() + "]");
                } else if ( movie.isTrailer() ) {
                    logger.fine("Updating data for: " + movie.getTitle() + " [Trailer]");
                } else {
                    logger.fine("Updating data for: " + movie.getTitle());
                }

                updateMovieData(xmlWriter, miScanner, backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                // Then get this movie's poster
                logger.finer("Updating poster for: " + movie.getTitle() + "...");
                updateMoviePoster(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                // Download episode images if required
                if (videoImagesDownload) {
                    updateVideoImages(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                }

                // Get Fanart if requested
                // Note that the FanartScanner will check if the file is newer / different
                if (fanartDownload) {
                    FanartScanner.scan(backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
                }

                // Get subtitle
                subtitlePlugin.generate(movie);

                // Get Trailer
                trailerPlugin.generate(movie);
            }
            subtitlePlugin.logOut();
            
            // ////////////////////////////////////////////////////////////////
            // / PASS 3 : Indexing the library
            //
            logger.fine("Indexing libraries...");
            library.buildIndex();

            // This is kind of a hack -- library.values() are the movies that were found in the library
            // and library.getMoviesList() are the ones that are there now. So the movies that are in
            // getMoviesList but not in values are the index masters.
            List<Movie> indexMasters = new ArrayList<Movie>();
            indexMasters.addAll(library.getMoviesList());
            indexMasters.removeAll(library.values());
            
            List<Movie> moviesList = new ArrayList<Movie>();
            moviesList.addAll(library.getMoviesList());
            moviesList.removeAll(indexMasters);

            SetThumbnailPlugin stp = new SetThumbnailPlugin();
            for (Movie movie : indexMasters) {
                logger.finer("Updating poster for index master: " + movie.getTitle() + "...");

                // If we can find a set poster file, use it; otherwise, stick with the first movie's poster
                String oldPosterFilename = movie.getPosterFilename();
                movie.setPosterFilename(movie.getBaseName() + ".jpg");
                if (! PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie)) {
                    movie.setPosterFilename(oldPosterFilename);
                }
                
                String thumbnailExtension = PropertiesUtil.getProperty("thumbnails.format", "png");
                movie.setThumbnailFilename(movie.getBaseName() + "_small." + thumbnailExtension);
                String posterExtension = PropertiesUtil.getProperty("posters.format", "png");
                movie.setDetailPosterFilename(movie.getBaseName() + "_large." + posterExtension);
                
                if (PropertiesUtil.getProperty("mjb.sets.createPosters", "false").equalsIgnoreCase("true")) {
                    // Create a detail poster for each movie
                    logger.finest("Creating detail poster for index master: " + movie.getBaseName());
                    createPoster(posterPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);
                }
                
                // Create a thumbnail for each movie
                logger.finest("Creating thumbnail for index master: " + movie.getBaseName()
                    + ", isTV: " + movie.isTVShow() + ", isHD: " + movie.isHD());
                createThumbnail(stp, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);

                // write the playlist for the movie if needed
                htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }
            
            for (Movie movie : moviesList) {
                // Update movie XML files with computed index information 
                logger.finest("Writing index data to movie: " + movie.getBaseName());
                xmlWriter.writeMovieXML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                // Create a detail poster for each movie
                logger.finest("Creating detail poster for movie: " + movie.getBaseName());
                createPoster(posterPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);

                // Create a thumbnail for each movie
                logger.finest("Creating thumbnails for movie: " + movie.getBaseName());
                createThumbnail(thumbnailPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);

                // write the movie details HTML
                htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

                // write the playlist for the movie if needed
                htmlWriter.generatePlaylist(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }
           
            logger.fine("Generating Indexes...");
            xmlWriter.writeIndexXML(tempJukeboxDetailsRoot, detailsDirName, library);
            xmlWriter.writeCategoryXML(tempJukeboxRoot, detailsDirName, library);
            htmlWriter.generateMoviesIndexHTML(tempJukeboxRoot, detailsDirName, library);
            htmlWriter.generateMoviesCategoryHTML(tempJukeboxRoot, detailsDirName, library);

            logger.fine("Copying new files to Jukebox directory...");
            String index = PropertiesUtil.getProperty("mjb.indexFile", "index.htm");
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

            // ////////////////////////////////////////////////////////////////
            // / PASS 4: Clean-up the jukebox directory
            // / If the command line argument "-c" was passed
            //
            if (jukeboxClean) {
                logger.fine("Cleaning up the jukebox directory...");

                // File tempJukeboxCleanFile = new File(jukeboxDetailsRoot);
                File[] cleanList = tempJukeboxCleanFile.listFiles();
                int cleanDeletedTotal = 0;

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

                        if (cleanCurrent.equals("CATEGORIES")) {
                            // logger.fine(cleanCurrent + " ignored");
                        } else if ((cleanCurrentExt.equals(".CSS")) ||
                                   (cleanCurrent.indexOf("GENRES_") >= 0) ||
                                   (cleanCurrent.indexOf("OTHER_") >= 0) ||
                                   (cleanCurrent.indexOf("RATING_") >= 0) ||
                                   (cleanCurrent.indexOf("TITLE_") >= 0) ||
                                   (cleanCurrent.indexOf("YEAR_") >= 0) ||
                                   (cleanCurrent.indexOf("TVSERIES_") >= 0) ||
                                   (cleanCurrent.indexOf("SET_") >= 0)) {
                            // logger.fine(cleanCurrent + " ignored");
                        } else {
                            // Left with just the generated movie files in the directory now.
                            // We should now check to see if they are in the current movie list
                            // If they are not in this list, then we will delete them.

                            if (cleanCurrent.lastIndexOf(".PLAYLIST") > 0) {
                                cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf(".PLAYLIST"));
                            } else if (cleanCurrent.lastIndexOf("_LARGE") > 0) {
                                cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("_LARGE"));
                            } else if (cleanCurrent.lastIndexOf("_SMALL") > 0) {
                                cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("_SMALL"));
                            } else if (cleanCurrent.lastIndexOf("FANART") > 0) {
                                cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("FANART") - 1);
                            } else if (cleanCurrent.lastIndexOf("VIDEOIMAGE") > 0 ) {
                                cleanCurrent = cleanCurrent.substring(0, cleanCurrent.lastIndexOf("VIDEOIMAGE") - 1);
                            }

                            if (!searchLibrary(cleanCurrent, library)) {
                                logger.finest("Deleted: " + cleanList[nbFiles].getName() + " from library");
                                cleanDeletedTotal++;
                                cleanList[nbFiles].delete();
                            }
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

        logger.fine("Process terminated.");
    }

    /**
     * Search the movie library for the passed movie name
     * 
     * @param slMovieName
     *            - the name of the movie to match
     * @param library
     *            - the library to search
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
     * When an XML file exists for the specified movie file, it is loaded into the
     * specified <tt>Movie</tt> object.
     *
     * When no XML file exist, scanners are called in turn, in order to add information
     * to the specified <tt>movie</tt> object. Once scanned, the <tt>movie</tt> object
     * is persisted.
     */
    private void updateMovieData(MovieJukeboxXMLWriter xmlWriter,
                                 MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin,
                                 String jukeboxDetailsRoot, String tempJukeboxDetailsRoot,
                                 Movie movie) throws FileNotFoundException, XMLStreamException {

        boolean forceXMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceXMLOverwrite", "false"));
        boolean checkNewer = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.nfo.checknewer", "true"));

        // For each movie in the library, if an XML file for this
        // movie already exist, then no need to search for movie
        // information, just parse the XML data.
        String safeBaseName = FileTools.makeSafeFilename(movie.getBaseName());
        File xmlFile = new File(jukeboxDetailsRoot + File.separator + safeBaseName + ".xml");

        // See if we can find the NFO associated with this video file.
        File nfoFile = new File(MovieNFOScanner.locateNFO(movie));

        // Only re-scan the nfo file if the NFO is newer and the xml file exists (2nd run or greater)
        if (FileTools.isNewer(nfoFile, xmlFile) && checkNewer && xmlFile.exists()) {
            logger.fine("NFO for " + movie.getTitle() + " has changed, will rescan file.");
            movie.setDirtyNFO(true);
            movie.setDirtyPoster(true);
            movie.setDirtyFanart(true);
            forceXMLOverwrite = true;
        }

        if (xmlFile.exists() && !forceXMLOverwrite) {
            // parse the XML file
            logger.finer("XML file found for " + movie.getBaseName());
            xmlWriter.parseMovieXML(xmlFile, movie);

            // update new episodes titles if new MovieFiles were added
            DatabasePluginController.scanTVShowTitles(movie);

            // Update thumbnails format if needed
            String thumbnailExtension = PropertiesUtil.getProperty("thumbnails.format", "png");
            movie.setThumbnailFilename(movie.getBaseName() + "_small." + thumbnailExtension);
            // Update poster format if needed
            String posterExtension = PropertiesUtil.getProperty("posters.format", "png");
            movie.setDetailPosterFilename(movie.getBaseName() + "_large." + posterExtension);

            // Check for local CoverArt
            PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);

        } else {
            // No XML file for this movie. We've got to find movie
            // information where we can (filename, IMDb, NFO, etc...)
            // Add here extra scanners if needed.
            if (forceXMLOverwrite) {
                logger.finer("Rescanning internet for information on " + movie.getBaseName());
            } else {
                logger.finer("XML file not found. Scanning internet for information on " + movie.getBaseName());
            }

            MovieNFOScanner.scan(movie);

            // Added forceXMLOverwrite for issue 366
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN) || movie.isDirtyPoster()) {
                PosterScanner.scan(jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
            }

            DatabasePluginController.scan(movie);
            miScanner.scan(movie);
        }
    }

    /**
     * Update the movie poster for the specified movie.
     * 
     * When an existing thumbnail is found for the movie, it is not overwriten,
     * unless the mjb.forceThumbnailOverwrite is set to true in the property file.
     * 
     * When the specified movie does not contain a valid URL for the poster, a dummy image is used instead.
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
        if ((!tmpDestFile.exists() && !posterFile.exists()) || (movie.isDirtyPoster())) {
            posterFile.getParentFile().mkdirs();

            if (movie.getPosterURL() == null || movie.getPosterURL().equals(Movie.UNKNOWN)) {
                logger.finest("Dummy image used for " + movie.getBaseName());
                FileTools.copyFile(
                    new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
                    tmpDestFile
                );
            } else {
                try {
                    // Issue 201 : we now download to local temp dir
                    logger.finest("Downloading poster for " + movie.getBaseName() + " to " + tmpDestFile.getName() + " [calling plugin]");
                    downloadImage(tmpDestFile, movie.getPosterURL());
                } catch (Exception e) {
                    logger.finer("Failed downloading movie poster : " + movie.getPosterURL());
                    FileTools.copyFile(
                        new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
                        tmpDestFile
                    );
                }
            }
        }
    }

    private void updateVideoImages(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String videoImageFilename;
        File videoImageFile;
        File tmpDestFile;

        boolean forceXMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceXMLOverwrite", "false"));

        for (MovieFile moviefile : movie.getMovieFiles()) {
            for (int part = moviefile.getFirstPart(); part <= moviefile.getLastPart(); ++part) {
                // The filename should use the episode number not the part number.
                videoImageFilename = FileTools.makeSafeFilename(movie.getBaseName() + "_VideoImage_" + part + ".jpg");
                videoImageFile = new File(jukeboxDetailsRoot + File.separator + videoImageFilename);
                tmpDestFile = new File(tempJukeboxDetailsRoot + File.separator + videoImageFilename);

                // Do not overwrite existing files - Unless XML overwrite is on.
                if ((!tmpDestFile.exists() && !videoImageFile.exists()) || forceXMLOverwrite) {
                    videoImageFile.getParentFile().mkdirs();
                    if (moviefile.getVideoImageURL(part) == null || moviefile.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                        logger.finest("Dummy video image used for " + movie.getBaseName() + " - part " + part);
                        try {
                            FileTools.copyFile(
                                new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"),
                                tmpDestFile
                                );
                        } catch (Exception ignore) {
                            logger.finer("Failed copying dummy video image file: dummy_videoimage.jpg");
                        }
                    } else {
                        try {
                            // Issue 201 : we now download to local temp dir
                            logger.finest("Downloading video image for " + movie.getBaseName() + " part " + part + " to " + tmpDestFile.getName() + " [calling plugin]");
                            downloadImage(tmpDestFile, moviefile.getVideoImageURL(part));
                            moviefile.setVideoImageFile(part, FileTools.makeSafeFilename(videoImageFilename));
                        } catch (Exception e) {
                            logger.finer("Failed downloading video image : " + moviefile.getVideoImageURL(part));
                            FileTools.copyFile(
                                new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"),
                                tmpDestFile
                                );
                        }
                    }
                }
            }
        }
        return;
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
                    logger.fine("Found media library: " + medlib);
                } else {
                    logger.fine("Skipped invalid media library: " + path);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed parsing moviejukebox library input file: " + f.getName());
            e.printStackTrace();
        }
        return mlp;
    }

    public MovieImagePlugin getThumbnailPlugin(String className) {
        MovieImagePlugin thumbnailPlugin;

        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends DefaultThumbnailPlugin> pluginClass = cl.loadClass(className).asSubclass(DefaultThumbnailPlugin.class);
            thumbnailPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            thumbnailPlugin = new DefaultThumbnailPlugin();
            logger.severe("Failed instanciating ThumbnailPlugin: " + className);
            logger.severe("Default thumbnail plugin will be used instead.");
            e.printStackTrace();
        }

        return thumbnailPlugin;
    }

    public MovieImagePlugin getPosterPlugin(String className) {
        MovieImagePlugin posterPlugin;

        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            posterPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            posterPlugin = new DefaultPosterPlugin();
            logger.severe("Failed instanciating PosterPlugin: " + className);
            logger.severe("Default poster plugin will be used instead.");
            e.printStackTrace();
        }

        return posterPlugin;
    }

    public MovieImagePlugin getBackgroundPlugin(String className) {
        MovieImagePlugin backgroundPlugin;

        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            backgroundPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            backgroundPlugin = new DefaultBackgroundPlugin();
            logger.severe("Failed instanciating BackgroundPlugin: " + className);
            logger.severe("Default background plugin will be used instead.");
            e.printStackTrace();
        }

        return backgroundPlugin;
    }

    public MovieListingPlugin getListingPlugin(String className) {
        MovieListingPlugin listingPlugin;
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieListingPlugin> pluginClass = cl.loadClass(className).asSubclass(MovieListingPlugin.class);
            listingPlugin = pluginClass.newInstance();
        } catch (Exception e) {
            listingPlugin = new MovieListingPluginBase();
            logger.severe("Failed instantiating ListingPlugin: " + className);
            logger.severe("NULL listing plugin will be used instead.");
            e.printStackTrace();
        }

        return listingPlugin;
    } // getListingPlugin()

    /**
     * Download the image for the specified url into the specified file.
     * Utilises the WebBrowser downloadImage function to allow for proxy connections.
     * @throws IOException
     */
    public static void downloadImage(File imageFile, String imageURL) throws IOException {
        WebBrowser webBrowser = new WebBrowser();
        webBrowser.downloadImage(imageFile, imageURL);
    }

    public static void createThumbnail(MovieImagePlugin thumbnailManager, String rootPath, String tempRootPath, String skinHome, Movie movie,
                                       boolean forceThumbnailOverwrite) {
        try {
            // Issue 201 : we now download to local temp dire
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String safeThumbnailFilename = FileTools.makeSafeFilename(movie.getThumbnailFilename());
            String src = tempRootPath + File.separator + safePosterFilename;
            String oldsrc = rootPath + File.separator + safePosterFilename;
            String dst = tempRootPath + File.separator + safeThumbnailFilename;
            String olddst = rootPath + File.separator + safeThumbnailFilename;
            FileInputStream fis;

            if (!(new File(olddst).exists()) || forceThumbnailOverwrite || (new File(src).exists())) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefor check to see if they exist in the original directory
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
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator + safePosterFilename));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
                }

                // Perspective code.
                String perspectiveDirection = PropertiesUtil.getProperty("thumbnails.perspectiveDirection", "right");
                
                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror thumbnail name.
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));

                    // Generate left & save as copy
                    logger.finest("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = thumbnailManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                    bi = thumbnailManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }
                
                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = thumbnailManager.generate(movie, bi, "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                }
                
                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = thumbnailManager.generate(movie, bi, "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left thumbnail from " + src + " to " + dst);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed creating thumbnail for " + movie.getTitle());
            e.printStackTrace();
        }
    }

    public static void createPoster(MovieImagePlugin posterManager, String rootPath, String tempRootPath, String skinHome, Movie movie,
                                    boolean forcePosterOverwrite) {
        try {
            // Issue 201 : we now download to local temp dire
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String safeDetailPosterFilename = FileTools.makeSafeFilename(movie.getDetailPosterFilename());
            String src = tempRootPath + File.separator + safePosterFilename;
            String oldsrc = rootPath + File.separator + safePosterFilename;
            String dst = tempRootPath + File.separator + safeDetailPosterFilename;
            String olddst = rootPath + File.separator + safeDetailPosterFilename;
            FileInputStream fis;

            if (!(new File(olddst).exists()) || forcePosterOverwrite || (new File(src).exists())) {
                // Issue 228: If the PNG files are deleted before running the jukebox this fails. Therefor check to see if they exist in the original directory
                if (new File(src).exists()) {
                    logger.finest("New file exists");
                    fis = new FileInputStream(src);
                } else {
                    logger.finest("Use old file");
                    fis = new FileInputStream(oldsrc);
                }

                BufferedImage bi = GraphicTools.loadJPEGImage(fis);
                if (bi == null) {
                    logger.info("Using dummy poster image for " + movie.getTitle());
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"), new File(rootPath + File.separator + safePosterFilename));
                    fis = new FileInputStream(src);
                    bi = GraphicTools.loadJPEGImage(fis);
                }
                logger.finest("Generating poster from " + src + " to " + dst);

                // Perspective code.
                String perspectiveDirection = PropertiesUtil.getProperty("posters.perspectiveDirection", "right");
                
                // Generate and save both images
                if (perspectiveDirection.equalsIgnoreCase("both")) {
                    // Calculate mirror thumbnail name.
                    String dstMirror = dst.substring(0, dst.lastIndexOf(".")) + "_mirror" + dst.substring(dst.lastIndexOf("."));

                    // Generate left & save as copy
                    logger.finest("Generating mirror thumbnail from " + src + " to " + dstMirror);
                    BufferedImage biMirror = bi;
                    biMirror = posterManager.generate(movie, bi, "left");
                    GraphicTools.saveImageToDisk(biMirror, dstMirror);

                    // Generate right as per normal
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                    bi = posterManager.generate(movie, bi, "right");
                    GraphicTools.saveImageToDisk(bi, dst);
                }
                
                // Only generate the right image
                if (perspectiveDirection.equalsIgnoreCase("right")) {
                    bi = posterManager.generate(movie, bi, "right");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating right thumbnail from " + src + " to " + dst);
                }
                
                // Only generate the left image
                if (perspectiveDirection.equalsIgnoreCase("left")) {
                    bi = posterManager.generate(movie, bi, "left");

                    // Save the right perspective image.
                    GraphicTools.saveImageToDisk(bi, dst);
                    logger.finest("Generating left thumbnail from " + src + " to " + dst);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed creating poster for " + movie.getTitle());
            e.printStackTrace();
        }
    }
}
