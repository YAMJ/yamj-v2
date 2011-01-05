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

/**
 * Scanner for posters.
 * Includes local searches (scan) and Internet Searches
 */
package com.moviejukebox.scanner.artwork;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.poster.IMoviePosterPlugin;
import com.moviejukebox.plugin.poster.IPosterPlugin;
import com.moviejukebox.plugin.poster.ITvShowPosterPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

/**
 * Scanner for poster files in local directory and from the Internet
 * 
 * @author groll.troll
 * @author Stuart.Boston
 * 
 * @version 1.0, 7 October 2008
 * @version 2.0 6 July 2009
 */
public class PosterScanner {

    private static Map<String, IPosterPlugin> posterPlugins;
    private static Map<String, IMoviePosterPlugin> moviePosterPlugins = new HashMap<String, IMoviePosterPlugin>();
    private static Map<String, ITvShowPosterPlugin> tvShowPosterPlugins = new HashMap<String, ITvShowPosterPlugin>();

    protected static Logger     logger = Logger.getLogger("moviejukebox");
    protected static Collection<String> posterExtensions  = new ArrayList<String>();
    protected static String     searchForExistingPoster;
    protected static String     fixedPosterName;
    protected static String     posterDirectory;
    protected static Boolean    useFolderImage;
    protected static Collection<String> posterImageName;
    protected static WebBrowser webBrowser;
    protected static String     preferredPosterSearchEngine;
    protected static String     posterSearchPriority;
    protected static boolean    posterValidate;
    protected static int        posterValidateMatch;
    protected static boolean    posterValidateAspect;
    protected static int        posterWidth;
    protected static int        posterHeight;
    private static String       tvShowPosterSearchPriority;
    private static String       moviePosterSearchPriority;

    static {
        StringTokenizer st;
        
        // We get covert art scanner behaviour
        searchForExistingPoster = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", "moviename");
        // We get the fixed name property
        fixedPosterName = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
        // See if we use folder.* image or not
        // Note: We need the useFolderImage because of the special "folder.jpg" case in windows.
        useFolderImage = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.useFolderImage", "false"));

        if (useFolderImage) {
            st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.imageName", "folder,poster"), ",;|");
            posterImageName = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                posterImageName.add(st.nextToken());
            }
        }
        
        // We get valid extensions
        st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.coverArtExtensions", "jpg,png,gif"), ",;| ");
        while (st.hasMoreTokens()) {
            posterExtensions.add(st.nextToken());
        }

        // We get Poster Directory if needed
        posterDirectory = PropertiesUtil.getProperty("poster.scanner.coverArtDirectory", "");

        webBrowser = new WebBrowser();
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");
        tvShowPosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "thetvdb,cdon,filmaffinity");
        moviePosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie",
                        "themoviedb,impawards,imdb,moviecovers,google,yahoo,motechnet");

        posterWidth = Integer.parseInt(PropertiesUtil.getProperty("posters.width", "0"));
        posterHeight = Integer.parseInt(PropertiesUtil.getProperty("posters.height", "0"));
        posterValidate = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.Validate", "true"));
        posterValidateMatch = Integer.parseInt(PropertiesUtil.getProperty("poster.scanner.ValidateMatch", "75"));
        posterValidateAspect = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.ValidateAspect", "true"));

        // Load plugins
        posterPlugins = new HashMap<String, IPosterPlugin>();

        ServiceLoader<IMoviePosterPlugin> moviePosterPluginsSet = ServiceLoader.load(IMoviePosterPlugin.class);
        for (IMoviePosterPlugin iPosterPlugin : moviePosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

        ServiceLoader<ITvShowPosterPlugin> tvShowPosterPluginsSet = ServiceLoader.load(ITvShowPosterPlugin.class);
        for (ITvShowPosterPlugin iPosterPlugin : tvShowPosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

    }

    public static String scan(Jukebox jukebox, Movie movie) {
        if (searchForExistingPoster.equalsIgnoreCase("no")) {
            // nothing to do we return
            return Movie.UNKNOWN;
        }

        String localPosterBaseFilename = Movie.UNKNOWN;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullPosterFilename = parentPath;
        File localPosterFile = null;
        
        if (searchForExistingPoster.equalsIgnoreCase("moviename")) {
            // Encode the basename to ensure that non-usable file system characters are replaced
            // Issue 1155 : YAMJ refuses to pickup fanart and poster for a movie -
            // Do not make safe file name before searching.
            localPosterBaseFilename = movie.getBaseFilename();
        } else if (searchForExistingPoster.equalsIgnoreCase("fixedcoverartname")) {
            localPosterBaseFilename = fixedPosterName;
        } else {
            logger.fine("PosterScanner: Wrong value for poster.scanner.searchForExistingCoverArt properties!");
            logger.fine("PosterScanner: Expected 'moviename' or 'fixedcoverartname'");
            return Movie.UNKNOWN;
        }

        if (!posterDirectory.equals("")) {
            fullPosterFilename = StringTools.appendToPath(fullPosterFilename, posterDirectory);
        }

        // Check to see if the fullPosterFilename ends with a "\/" and only add it if needed
        // Usually this occurs because the files are at the root of a folder
        fullPosterFilename = StringTools.appendToPath(fullPosterFilename, localPosterBaseFilename);
        localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
        boolean foundLocalPoster = localPosterFile.exists();               

        // Try searching the fileCache for the filename.
        if (!foundLocalPoster) {
            localPosterFile = FileTools.findFilenameInCache(localPosterBaseFilename, posterExtensions, jukebox, "PosterScanner: ");
            if (localPosterFile != null) {
                foundLocalPoster = true;
            }
        }

        /**
         * This part will look for a filename with the same name as the directory for the poster or for folder.* poster The intention is for you to be able
         * to create the season / TV series art for the whole series and not for the first show. Useful if you change the files regularly.
         * 
         * @author Stuart.Boston
         * @version 1.0
         * @date 18th October 2008
         */
        if (!foundLocalPoster) {
            // If no poster has been found, try the foldername
            // No need to check the poster directory
            localPosterBaseFilename = FileTools.getParentFolderName(movie.getFile());

            if (useFolderImage) {
                // Checking for MovieFolderName.* AND folder.*
                logger.finest("PosterScanner: Checking for '" + localPosterBaseFilename + ".*' posters AND " + posterImageName + ".* posters");
            } else {
                // Only checking for the MovieFolderName.* and not folder.*
                logger.finest("PosterScanner: Checking for '" + localPosterBaseFilename + ".*' posters");
            }

            // Check for the directory name with extension for poster
            fullPosterFilename = StringTools.appendToPath(parentPath, localPosterBaseFilename);
            localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
            foundLocalPoster = localPosterFile.exists();
            if(!foundLocalPoster && useFolderImage){
                for (String imageFileName : posterImageName) {
                    // logger.finest("Checking for '" + imageFileName + ".*' poster");
                    fullPosterFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile()), imageFileName);
                    localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
                    foundLocalPoster = localPosterFile.exists();
                    
                    if (!foundLocalPoster && movie.isTVShow()) {
                        // Get the parent directory and check that
                        fullPosterFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()), imageFileName);
                        //System.out.println("SCANNER: " + fullPosterFilename);
                        localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
                        foundLocalPoster = localPosterFile.exists();
                        if (foundLocalPoster) {
                            break;   // We found the artwork so quit the loop
                        }
                    } else {
                        break;    // We found the artwork so quit the loop
                    }
                }
            }
        }
        /*
         * END OF Folder Poster
         */

        if (foundLocalPoster) {
            fullPosterFilename = localPosterFile.getAbsolutePath();
            logger.finer("PosterScanner: Local poster file " + fullPosterFilename + " found");

            String safePosterFilename         = movie.getPosterFilename();
            String finalJukeboxPosterFileName = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), safePosterFilename);
            String tempJukeboxPosterFileName  = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), safePosterFilename);

            File finalJukeboxFile = FileTools.fileCache.getFile(finalJukeboxPosterFileName);
            File tempJukeboxFile  = new File(tempJukeboxPosterFileName);
            boolean copyLocalPoster = false;

            if ( ! finalJukeboxFile.exists() ||
                 // temp jukebox file exists and is newer ?
                 (tempJukeboxFile.exists() && FileTools.isNewer(localPosterFile, tempJukeboxFile)) ||
                 // file size is different ?
                 (localPosterFile.length() != finalJukeboxFile.length()) ||
                 // local file is newer ?
                 (FileTools.isNewer(localPosterFile, finalJukeboxFile))
                ) {
              // Force copy of local poster file
              copyLocalPoster = true;
            }

            // logger.fine("PosterScanner: tempJukeboxPosterFileName:"+tempJukeboxPosterFileName);
            // logger.fine("PosterScanner: finalJukeboxPosterFileName:"+finalJukeboxPosterFileName);
            // logger.fine("copyLocalPoster:" + copyLocalPoster);
            if ( copyLocalPoster ) {
                 FileTools.copyFile(localPosterFile, tempJukeboxFile);
                 logger.finer("PosterScanner: " + fullPosterFilename + " has been copied to " + tempJukeboxPosterFileName);
            }
            // Update poster url with local poster
            String posterURI = localPosterFile.toURI().toString();
            movie.setPosterURL(posterURI);
            return posterURI;
        } else {
            logger.finer("PosterScanner: No local poster found for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
    }

    private static String getPluginsCode() {
        String response = "";

        Set<String> keySet = posterPlugins.keySet();
        for (String string : keySet) {
            response += string + " / ";
        }
        return response;
    }

    /**
     * Locate the PosterURL from the Internet. This is the main method and should be called instead of the individual getPosterFrom* methods.
     * 
     * @param movie
     *            The movieBean to search for
     * @return The posterImage with poster url that was found (Maybe Image.UNKNOWN)
     */
    public static IImage getPosterURL(Movie movie) {
        String posterSearchToken;
        IImage posterImage = Image.UNKNOWN;
        StringTokenizer st;

        if (movie.isTVShow()) {
            st = new StringTokenizer(tvShowPosterSearchPriority, ",");
        } else {
            st = new StringTokenizer(moviePosterSearchPriority, ",");
        }

        while (st.hasMoreTokens() && StringTools.isNotValidString(posterImage.getUrl())) {
            posterSearchToken = st.nextToken();

            IPosterPlugin iPosterPlugin = posterPlugins.get(posterSearchToken);
            
            // Check that plugin is register even on movie or tv
            if (iPosterPlugin == null) {
                logger.severe("PosterScanner: '" + posterSearchToken + "' plugin doesn't exist, please check you moviejukebox properties. Valid plugins are : "
                                + getPluginsCode());
            }
            
            String msg = null;
            
            if (movie.isTVShow()) {
                iPosterPlugin = tvShowPosterPlugins.get(posterSearchToken);
                msg = "TvShow";
            } else {
                iPosterPlugin = moviePosterPlugins.get(posterSearchToken);
                msg = "Movie";
            }
            
            if (iPosterPlugin == null) {
                logger.info("PosterScanner: " + posterSearchToken + " is not a " + msg + " Poster plugin - skipping");
            } else {
                logger.finest("PosterScanner: Using " + posterSearchToken + " to search for a " + msg + " poster for " + movie.getTitle());
                posterImage = iPosterPlugin.getPosterUrl(movie, movie);
            }

            // Validate the poster- No need to validate if we're UNKNOWN
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl()) && posterValidate && !validatePoster(posterImage, posterWidth, posterHeight, posterValidateAspect)) {
                posterImage = Image.UNKNOWN;
            } else {
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
                    logger.finest("PosterScanner: Poster URL found at " + posterSearchToken + ": " + posterImage.getUrl());
                }
            }
        }

        return posterImage;
    }

    public static boolean validatePoster(IImage posterImage) {
        return validatePoster(posterImage, posterWidth, posterHeight, posterValidateAspect);
    }

    /**
     * Get the size of the file at the end of the URL Taken from: http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     * 
     * @param posterImage
     *            Poster image to check
     * @param posterWidth
     *            The width to check
     * @param posterHeight
     *            The height to check
     * @param checkAspect
     *            Should the aspect ratio be checked
     * @return True if the poster is good, false otherwise
     */
    public static boolean validatePoster(IImage posterImage, int posterWidth, int posterHeight, boolean checkAspect) {
        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader)readers.next();
        int urlWidth = 0, urlHeight = 0;
        float urlAspect;

        if (!posterValidate) {
            return true;
        }

        if (StringTools.isNotValidString(posterImage.getUrl())) {
            return false;
        }

        try {
            URL url = new URL(posterImage.getUrl());
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);
            
            if (in != null) {
                in.close();
            }
            
            if (iis != null) {
                iis.close();
            }
        } catch (IOException error) {
            logger.finest("PosterScanner: ValidatePoster error: " + error.getMessage() + ": can't open url");
            return false; // Quit and return a false poster
        }

        // Check if we need to cut the poster into a sub image
        if (StringTools.isValidString(posterImage.getSubimage())) {
            StringTokenizer st = new StringTokenizer(posterImage.getSubimage(), ", ");
            int x = Integer.parseInt(st.nextToken());
            int y = Integer.parseInt(st.nextToken());
            int l = Integer.parseInt(st.nextToken());
            int h = Integer.parseInt(st.nextToken());

            urlWidth = urlWidth * l / 100 - urlWidth * x / 100;
            urlHeight = urlHeight * h / 100 - urlHeight * y / 100;
        }

        urlAspect = (float)urlWidth / (float)urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            logger.finest(posterImage + " rejected: URL is landscape format");
            return false;
        }

        // Adjust poster width / height by the ValidateMatch figure
        posterWidth = (posterWidth * posterValidateMatch) / 100;
        posterHeight = (posterHeight * posterValidateMatch) / 100;

        if (urlWidth < posterWidth) {
            logger.finest("PosterScanner: " + posterImage + " rejected: URL width (" + urlWidth + ") is smaller than poster width (" + posterWidth + ")");
            return false;
        }

        if (urlHeight < posterHeight) {
            logger.finest("PosterScanner: " + posterImage + " rejected: URL height (" + urlHeight + ") is smaller than poster height (" + posterHeight + ")");
            return false;
        }
        return true;
    }

    public static void register(String key, IPosterPlugin posterPlugin) {
        posterPlugins.put(key, posterPlugin);
    }

    private static void register(String key, IMoviePosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            logger.finest("PosterScanner: " + posterPlugin.getClass().getName() + " registered as Movie Poster Plugin with key '" + key + "'");
            moviePosterPlugins.put(key, posterPlugin);
            register(key, (IPosterPlugin)posterPlugin);
        } else {
            logger.finest("PosterScanner: " + posterPlugin.getClass().getName() + " available, but not loaded use key '" + key + "' to enable it.");
        }
    }

    public static void register(String key, ITvShowPosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            logger.finest("PosterScanner: " + posterPlugin.getClass().getName() + " registered as TvShow Poster Plugin with key '" + key + "'");
            tvShowPosterPlugins.put(key, posterPlugin);
            register(key, (IPosterPlugin)posterPlugin);
        } else {
            logger.finest("PosterScanner: " + posterPlugin.getClass().getName() + " available, but not loaded use key '" + key + "' to enable it.");
        }
    }

    public static void scan(Movie movie) {
        // check the default ID for a 0 or -1 and skip poster processing
        String id = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (!movie.isScrapeLibrary() || id.equals("0") || id.equals("-1")) {
            logger.finer("PosterScanner: Skipping online poster search for " + movie.getBaseFilename());
            return;
        }
        
        logger.finer("PosterScanner: Searching online for " + movie.getBaseFilename());
        IImage posterImage = getPosterURL(movie);
        if (!Movie.UNKNOWN.equals(posterImage.getUrl())) {
            movie.setPosterURL(posterImage.getUrl());
            movie.setPosterSubimage(posterImage.getSubimage());
        }
    }
}
