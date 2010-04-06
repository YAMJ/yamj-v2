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

/**
 * Scanner for posters.
 * Includes local searches (scan) and Internet Searches
 */
package com.moviejukebox.scanner;

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

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.poster.IMoviePosterPlugin;
import com.moviejukebox.plugin.poster.IPosterPlugin;
import com.moviejukebox.plugin.poster.ITvShowPosterPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
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

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String[] coverArtExtensions;
    protected static String searchForExistingCoverArt;
    protected static String fixedCoverArtName;
    protected static String coverArtDirectory;
    protected static Boolean useFolderImage;
    protected static WebBrowser webBrowser;
    protected static String preferredPosterSearchEngine;
    protected static String posterSearchPriority;
    protected static boolean posterValidate;
    protected static int posterValidateMatch;
    protected static boolean posterValidateAspect;
    protected static int posterWidth;
    protected static int posterHeight;
    private static String tvShowPosterSearchPriority;
    private static String moviePosterSearchPriority;

    static {
        // We get covert art scanner behaviour
        searchForExistingCoverArt = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", "moviename");
        // We get the fixed name property
        fixedCoverArtName = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
        // See if we use folder.* image or not
        useFolderImage = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.useFolderImage", "false"));

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.coverArtExtensions", "jpg,png,gif"), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        coverArtExtensions = extensions.toArray(new String[] {});

        // We get coverart Directory if needed
        coverArtDirectory = PropertiesUtil.getProperty("poster.scanner.coverArtDirectory", "");

        webBrowser = new WebBrowser();
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");
        posterWidth = Integer.parseInt(PropertiesUtil.getProperty("posters.width", "0"));
        posterHeight = Integer.parseInt(PropertiesUtil.getProperty("posters.height", "0"));
        tvShowPosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "thetvdb,cdon,filmaffinity");
        moviePosterSearchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie",
                        "moviedb,impawards,imdb,moviecovers,google,yahoo,motechnet");
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

    public static boolean scan(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        if (searchForExistingCoverArt.equalsIgnoreCase("no")) {
            // nothing to do we return
            return false;
        }

        String localPosterBaseFilename = Movie.UNKNOWN;
        String fullPosterFilename = null;
        File localPosterFile = null;

        if (searchForExistingCoverArt.equalsIgnoreCase("moviename")) {
            // Encode the basename to ensure that non-usable file system characters are replaced
            // Issue 1155 : YAMJ refuses to pickup fanart and poster for a movie -
            // Do not make safe file name before searching.
            localPosterBaseFilename = FileTools.makeSafeFilename(movie.getBaseName());
        } else if (searchForExistingCoverArt.equalsIgnoreCase("fixedcoverartname")) {
            localPosterBaseFilename = fixedCoverArtName;
        } else {
            logger.fine("PosterScanner: Wrong value for poster.scanner.searchForExistingCoverArt properties!");
            logger.fine("PosterScanner: Expected 'moviename' or 'fixedcoverartname'");
            return false;
        }

        boolean foundLocalCoverArt = false;

        for (String extension : coverArtExtensions) {
            fullPosterFilename = FileTools.getParentFolder(movie.getFile());

            if (!coverArtDirectory.equals("")) {
                fullPosterFilename += File.separator + coverArtDirectory;
            }
            fullPosterFilename += File.separator + localPosterBaseFilename + "." + extension;
            // logger.finest("PosterScanner: Checking for "+ fullPosterFilename);
            localPosterFile = new File(fullPosterFilename);
            if (localPosterFile.exists()) {
                logger.finest("PosterScanner: Poster file " + fullPosterFilename + " found");
                foundLocalCoverArt = true;
                break;
            }
        }

        /**
         * This part will look for a filename with the same name as the directory for the cover art or for folder.* coverart The intention is for you to be able
         * to create the season / TV series art for the whole series and not for the first show. Useful if you change the files regularly.
         * 
         * @author Stuart.Boston
         * @version 1.0
         * @date 18th October 2008
         */
        if (!foundLocalCoverArt) {
            // if no coverart has been found, try the foldername
            // no need to check the coverart directory
            localPosterBaseFilename = movie.getFile().getParent();
            localPosterBaseFilename = localPosterBaseFilename.substring(localPosterBaseFilename.lastIndexOf("\\") + 1);

            if (useFolderImage) {
                // Checking for MovieFolderName.* AND folder.*
                logger.finest("PosterScanner: Checking for '" + localPosterBaseFilename + ".*' coverart AND folder.* coverart");
            } else {
                // Only checking for the MovieFolderName.* and not folder.*
                logger.finest("PosterScanner: Checking for '" + localPosterBaseFilename + ".*' coverart");
            }

            for (String extension : coverArtExtensions) {
                // Check for the directory name with extension for coverart
                fullPosterFilename = movie.getFile().getParent() + File.separator + localPosterBaseFilename + "." + extension;
                localPosterFile = new File(fullPosterFilename);
                if (localPosterFile.exists()) {
                    logger.finest("PosterScanner: Poster file " + fullPosterFilename + " found");
                    foundLocalCoverArt = true;
                    break;
                }

                if (useFolderImage) {
                    // logger.finest("Checking for 'folder.*' coverart");
                    // Check for folder.jpg if it exists
                    fullPosterFilename = movie.getFile().getParent() + File.separator + "folder." + extension;
                    localPosterFile = new File(fullPosterFilename);
                    if (localPosterFile.exists()) {
                        logger.finest("PosterScanner: Poster file " + fullPosterFilename + " found");
                        foundLocalCoverArt = true;
                        break;
                    }
                }
            }
        }
        /*
         * END OF Folder CoverArt
         */

        if (foundLocalCoverArt) {
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + safePosterFilename;
            String destFileName = tempJukeboxDetailsRoot + File.separator + safePosterFilename;

            File finalDestinationFile = new File(finalDestinationFileName);
            File destFile = new File(destFileName);
            boolean checkAgain = false;

            // Overwrite the jukebox files if the local file is newer
            // First check the temp jukebox file
            if (localPosterFile.exists() && destFile.exists()) {
                if (FileTools.isNewer(localPosterFile, destFile)) {
                    checkAgain = true;
                }
            } else if (localPosterFile.exists() && finalDestinationFile.exists()) {
                // Check the target jukebox file
                if (FileTools.isNewer(localPosterFile, finalDestinationFile)) {
                    checkAgain = true;
                }
            }

            if ((localPosterFile.length() != finalDestinationFile.length()) || (FileTools.isNewer(localPosterFile, finalDestinationFile))) {
                // Poster size is different OR Local Poster is newer
                checkAgain = true;
            }

            if (!finalDestinationFile.exists() || checkAgain) {
                FileTools.copyFile(localPosterFile, destFile);
                logger.finer("PosterScanner: " + fullPosterFilename + " has been copied to " + destFileName);
            } else {
                logger.finer("PosterScanner: " + finalDestinationFileName + " is different to " + fullPosterFilename);
            }

            // Update poster url with local poster
            movie.setPosterURL(localPosterFile.toURI().toString());
            return true;
        } else {
            logger.finer("PosterScanner: No local covertArt found for " + movie.getBaseName());
            return false;
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
     * @param imdbXML
     *            The IMDb XML page (for the IMDb poster search)
     * @return The posterURL that was found (Maybe Movie.UNKNOWN)
     */
    public static String getPosterURL(Movie movie) {
        String posterSearchToken;
        String posterURL = Movie.UNKNOWN;
        StringTokenizer st;

        if (movie.isTVShow()) {
            st = new StringTokenizer(tvShowPosterSearchPriority, ",");
        } else {
            st = new StringTokenizer(moviePosterSearchPriority, ",");
        }

        while (st.hasMoreTokens() && posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
            posterSearchToken = st.nextToken();

            IPosterPlugin iPosterPlugin = posterPlugins.get(posterSearchToken);
            // Check that plugin is register even on movie or tv
            if (iPosterPlugin == null) {
                logger.severe("Posterscanner: '" + posterSearchToken + "' plugin doesn't exist, please check you moviejukebox properties. Valid plugins are : "
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
                logger.info("Posterscanner: " + posterSearchToken + " is not a " + msg + " Poster plugin - skipping");
            } else {
                posterURL = iPosterPlugin.getPosterUrl(movie, movie);
            }

            // Validate the poster- No need to validate if we're UNKNOWN
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL) && posterValidate && !validatePoster(posterURL, posterWidth, posterHeight, posterValidateAspect)) {
                posterURL = Movie.UNKNOWN;
            } else {
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
                    logger.finest("PosterScanner: Poster URL found at " + posterSearchToken + ": " + posterURL);
                }
            }
        }

        return posterURL;
    }

    public static boolean validatePoster(String posterURL) {
        return validatePoster(posterURL, posterWidth, posterHeight, posterValidateAspect);
    }

    /**
     * Get the size of the file at the end of the URL Taken from: http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     * 
     * @param posterURL
     *            The URL to check as a string
     * @param posterWidth
     *            The width to check
     * @param posterHeight
     *            The height to check
     * @param checkAspect
     *            Should the aspect ratio be checked
     * @return True if the poster is good, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean validatePoster(String posterURL, int posterWidth, int posterHeight, boolean checkAspect) {
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader)readers.next();
        int urlWidth = 0, urlHeight = 0;
        float urlAspect;

        if (!posterValidate) {
            return true;
        }

        if (posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
            return false;
        }

        try {
            URL url = new URL(posterURL);
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);
        } catch (IOException ignore) {
            logger.finest("PosterScanner: ValidatePoster error: " + ignore.getMessage() + ": can't open url");
            return false; // Quit and return a false poster
        }

        urlAspect = (float)urlWidth / (float)urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            logger.finest(posterURL + " rejected: URL is landscape format");
            return false;
        }

        // Adjust poster width / height by the ValidateMatch figure
        posterWidth = posterWidth * (posterValidateMatch / 100);
        posterHeight = posterHeight * (posterValidateMatch / 100);

        if (urlWidth < posterWidth) {
            logger.finest("PosterScanner: " + posterURL + " rejected: URL width (" + urlWidth + ") is smaller than poster width (" + posterWidth + ")");
            return false;
        }

        if (urlHeight < posterHeight) {
            logger.finest("PosterScanner: " + posterURL + " rejected: URL height (" + urlHeight + ") is smaller than poster height (" + posterHeight + ")");
            return false;
        }
        return true;
    }

    public static void register(String key, IPosterPlugin posterPlugin) {
        posterPlugins.put(key, posterPlugin);
    }

    private static void register(String key, IMoviePosterPlugin posterPlugin) {
        logger.finest("Posterscanner: " + posterPlugin.getClass().getName() + " register as Movie Poster Plugin with key " + key);
        moviePosterPlugins.put(key, posterPlugin);
        register(key, (IPosterPlugin)posterPlugin);
    }

    public static void register(String key, ITvShowPosterPlugin posterPlugin) {
        logger.finest("PosterScanner: " + posterPlugin.getClass().getName() + " register as TvShow Poster Plugin with key " + key);
        tvShowPosterPlugins.put(key, posterPlugin);
        register(key, (IPosterPlugin)posterPlugin);
    }

    public static void scan(Movie movie) {
        logger.finer("PosterScanner: Searching for " + movie.getBaseName());
        String posterURL = getPosterURL(movie);
        if (!Movie.UNKNOWN.equals(posterURL)) {
            movie.setPosterURL(posterURL);
        }
    }
}
