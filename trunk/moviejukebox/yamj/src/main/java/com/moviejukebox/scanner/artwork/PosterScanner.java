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
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.*;
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.poster.IMoviePosterPlugin;
import com.moviejukebox.plugin.poster.IPosterPlugin;
import com.moviejukebox.plugin.poster.ITvShowPosterPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

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

    private static final Logger logger = Logger.getLogger(PosterScanner.class);
    private static final String logMessage = "PosterScanner: ";
    private static Map<String, IPosterPlugin> posterPlugins;
    private static Map<String, IMoviePosterPlugin> moviePosterPlugins = new HashMap<String, IMoviePosterPlugin>();
    private static Map<String, ITvShowPosterPlugin> tvShowPosterPlugins = new HashMap<String, ITvShowPosterPlugin>();
    private static final String EXISTING_MOVIE = "moviename";
    private static final String EXISTING_FIXED = "fixedcoverartname";
    private static final String EXISTING_NO = "no";
    // We get covert art scanner behaviour
    protected static final String searchForExistingPoster = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", EXISTING_MOVIE);
    // See if we use folder.* image or not
    // Note: We need the useFolderImage because of the special "folder.jpg" case in windows.
    protected static final Boolean useFolderImage = PropertiesUtil.getBooleanProperty("poster.scanner.useFolderImage", "false");
    // We get the fixed name property
    protected static final String fixedPosterName = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
    protected static final Collection<String> posterExtensions = new ArrayList<String>();
    protected static String posterDirectory;
    protected static final Collection<String> posterImageName = new ArrayList<String>();
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
        StringTokenizer st;

        if (useFolderImage) {
            st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.imageName", "folder,poster"), ",;|");
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

        posterWidth = PropertiesUtil.getIntProperty("posters.width", "0");
        posterHeight = PropertiesUtil.getIntProperty("posters.height", "0");
        posterValidate = PropertiesUtil.getBooleanProperty("poster.scanner.Validate", "true");
        posterValidateMatch = PropertiesUtil.getIntProperty("poster.scanner.ValidateMatch", "75");
        posterValidateAspect = PropertiesUtil.getBooleanProperty("poster.scanner.ValidateAspect", "true");

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
        if (searchForExistingPoster.equalsIgnoreCase(EXISTING_NO)) {
            // nothing to do we return
            return Movie.UNKNOWN;
        }

        String localPosterBaseFilename;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullPosterFilename = parentPath;
        File localPosterFile;

        if (searchForExistingPoster.equalsIgnoreCase(EXISTING_MOVIE)) {
            // Encode the basename to ensure that non-usable file system characters are replaced
            // Issue 1155 : YAMJ refuses to pickup fanart and poster for a movie -
            // Do not make safe file name before searching.
            localPosterBaseFilename = movie.getBaseFilename();
        } else if (searchForExistingPoster.equalsIgnoreCase(EXISTING_FIXED)) {
            localPosterBaseFilename = fixedPosterName;
        } else {
            logger.info(logMessage + "Wrong value for 'poster.scanner.searchForExistingCoverArt' property ('" + searchForExistingPoster + "')!");
            logger.info(logMessage + "Expected '" + EXISTING_MOVIE + "' or '" + EXISTING_FIXED + "'");
            return Movie.UNKNOWN;
        }

        if (StringUtils.isNotBlank(posterDirectory)) {
            fullPosterFilename = StringTools.appendToPath(fullPosterFilename, posterDirectory);
        }

        // Check to see if the fullPosterFilename ends with a "\/" and only add it if needed
        // Usually this occurs because the files are at the root of a folder
        fullPosterFilename = StringTools.appendToPath(fullPosterFilename, localPosterBaseFilename);
        localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
        boolean foundLocalPoster = localPosterFile.exists();

        // Try searching the fileCache for the filename, but only for non-fixed filenames
        if (!foundLocalPoster && !searchForExistingPoster.equalsIgnoreCase(EXISTING_FIXED)) {
            localPosterFile = FileTools.findFilenameInCache(localPosterBaseFilename, posterExtensions, jukebox, logMessage, Boolean.TRUE);
            if (localPosterFile != null) {
                foundLocalPoster = true;
            }
        }

        /**
         * This part will look for a filename with the same name as the
         * directory for the poster or for folder.* poster The intention is for
         * you to be able to create the season / TV series art for the whole
         * series and not for the first show. Useful if you change the files
         * regularly.
         *
         * @author Stuart.Boston
         * @version 1.0 @date 18th October 2008
         */
        if (!foundLocalPoster) {
            // If no poster has been found, try the foldername
            // No need to check the poster directory
            localPosterBaseFilename = FileTools.getParentFolderName(movie.getFile());

            if (useFolderImage) {
                // Checking for MovieFolderName.* AND folder.*
                logger.debug(logMessage + "Checking for '" + localPosterBaseFilename + ".*' posters AND " + posterImageName + ".* posters");
            } else {
                // Only checking for the MovieFolderName.* and not folder.*
                logger.debug(logMessage + "Checking for '" + localPosterBaseFilename + ".*' posters");
            }

            // Check for the directory name with extension for poster
            fullPosterFilename = StringTools.appendToPath(parentPath, localPosterBaseFilename);
            localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, posterExtensions);
            foundLocalPoster = localPosterFile.exists();
            if (!foundLocalPoster && useFolderImage) {
                for (String imageFileName : posterImageName) {
                    // logger.debug("Checking for '" + imageFileName + ".*' poster");
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
            Dimension imageSize = getFileImageSize(localPosterFile);
            logger.debug(logMessage + "Local poster file " + fullPosterFilename + " found, size " + imageSize.width + " x " + imageSize.height);

            String safePosterFilename = movie.getPosterFilename();
            String finalJukeboxPosterFileName = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), safePosterFilename);
            String tempJukeboxPosterFileName = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), safePosterFilename);

            File finalJukeboxFile = FileTools.fileCache.getFile(finalJukeboxPosterFileName);
            File tempJukeboxFile = new File(tempJukeboxPosterFileName);
            boolean copyLocalPoster = false;

//            logger.debug(logMessage + "finalJukeboxFile exists: " + finalJukeboxFile.exists());
//            logger.debug(logMessage + "Local newer than temp? : " + (tempJukeboxFile.exists() && FileTools.isNewer(localPosterFile, tempJukeboxFile)));
//            logger.debug(logMessage + "Posters same size?     : " + (localPosterFile.length() != finalJukeboxFile.length()));
//            logger.debug(logMessage + "Local newer than final?: " + (FileTools.isNewer(localPosterFile, finalJukeboxFile)));

            if (!finalJukeboxFile.exists()
                    || // temp jukebox file exists and is newer ?
                    (tempJukeboxFile.exists() && FileTools.isNewer(localPosterFile, tempJukeboxFile))
                    || // file size is different ?
                    (localPosterFile.length() != finalJukeboxFile.length())
                    || // local file is newer ?
                    (FileTools.isNewer(localPosterFile, finalJukeboxFile))) {
                // Force copy of local poster file
                copyLocalPoster = true;
            }

            if (copyLocalPoster) {
                FileTools.copyFile(localPosterFile, tempJukeboxFile);
                logger.debug(logMessage + fullPosterFilename + " has been copied to " + tempJukeboxPosterFileName);
            }
            // Update poster URL with local poster
            String posterURI = localPosterFile.toURI().toString();
            movie.setPosterURL(posterURI);

            return posterURI;
        } else {
            logger.debug(logMessage + "No local poster found for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
    }

    private static String getPluginsCode() {
        StringBuilder response = new StringBuilder();

        Set<String> keySet = posterPlugins.keySet();
        for (String string : keySet) {
            response.append(string).append(Movie.SPACE_SLASH_SPACE);
        }
        return response.toString();
    }

    /**
     * Locate the PosterURL from the Internet. This is the main method and
     * should be called instead of the individual getPosterFrom* methods.
     *
     * @param movie The movieBean to search for
     * @return The posterImage with poster url that was found (Maybe
     * Image.UNKNOWN)
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
                logger.error(logMessage + "'" + posterSearchToken + "' plugin doesn't exist, please check your moviejukebox properties. Valid plugins are : "
                        + getPluginsCode());
            }

            String msg;

            if (movie.isTVShow()) {
                iPosterPlugin = tvShowPosterPlugins.get(posterSearchToken);
                msg = "TvShow";
            } else {
                iPosterPlugin = moviePosterPlugins.get(posterSearchToken);
                msg = "Movie";
            }

            if (iPosterPlugin == null) {
                logger.info(logMessage + posterSearchToken + " is not a " + msg + " Poster plugin - skipping");
            } else {
                logger.debug(logMessage + "Using " + posterSearchToken + " to search for a " + msg + " poster for " + movie.getTitle());
                posterImage = iPosterPlugin.getPosterUrl(movie, movie);
            }

            // Validate the poster- No need to validate if we're UNKNOWN
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl()) && posterValidate && !validatePoster(posterImage, posterWidth, posterHeight, posterValidateAspect)) {
                posterImage = Image.UNKNOWN;
            } else {
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
                    logger.debug(logMessage + "Poster URL found at " + posterSearchToken + ": " + posterImage.getUrl());
                    posterImage.setSubimage(posterSearchToken);     // TODO: This is a hack, but seeing as only one poster scanner uses it, it should be safe until it's all refactored to use the Artwork class
                    movie.setDirty(DirtyFlag.POSTER, true);
                }
            }
        }

        return posterImage;
    }

    public static boolean validatePoster(IImage posterImage) {
        return validatePoster(posterImage, posterWidth, posterHeight, posterValidateAspect);
    }

    /**
     * Get the size of the file at the end of the URL Taken from:
     * http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     *
     * @param posterImage Poster image to check
     * @param posterWidth The width to check
     * @param posterHeight The height to check
     * @param checkAspect Should the aspect ratio be checked
     * @return True if the poster is good, false otherwise
     */
    public static boolean validatePoster(IImage posterImage, int posterWidth, int posterHeight, boolean checkAspect) {
        float urlAspect;
        if (!posterValidate) {
            return true;
        }

        if (StringTools.isNotValidString(posterImage.getUrl())) {
            return false;
        }

        Dimension imageDimension = getUrlDimensions(posterImage.getUrl());
        double urlWidth = imageDimension.getWidth();
        double urlHeight = imageDimension.getHeight();

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

        urlAspect = (float) urlWidth / (float) urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            logger.debug(posterImage + " rejected: URL is landscape format");
            return false;
        }

        // Adjust poster width / height by the ValidateMatch figure
        int newPosterWidth = (posterWidth * posterValidateMatch) / 100;
        int newPosterHeight = (posterHeight * posterValidateMatch) / 100;

        if (urlWidth < newPosterWidth) {
            logger.debug(logMessage + posterImage + " rejected: URL width (" + urlWidth + ") is smaller than poster width (" + newPosterWidth + ")");
            return false;
        }

        if (urlHeight < newPosterHeight) {
            logger.debug(logMessage + posterImage + " rejected: URL height (" + urlHeight + ") is smaller than poster height (" + newPosterHeight + ")");
            return false;
        }
        return true;
    }

    /**
     * Read an URL and get the dimensions of the image
     *
     * @param imageUrl
     * @return
     */
    public static Dimension getUrlDimensions(String imageUrl) {
        Dimension imageDimension = new Dimension(0, 0);

        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader) readers.next();

        InputStream in = null;
        ImageInputStream iis = null;

        try {
            URL url = new URL(imageUrl);
            in = url.openStream();
            iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);

            imageDimension.setSize(reader.getWidth(0), reader.getHeight(0));
            return imageDimension;
        } catch (IOException error) {
            logger.debug(logMessage + "getUrlDimensions error: " + error.getMessage() + ": can't open url");
            return imageDimension; // Quit and return a false poster
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                // Ignore the error, it's already closed
            }

            try {
                if (iis != null) {
                    iis.close();
                }
            } catch (IOException e) {
                // Ignore the error, it's already closed
            }
        }

    }

    public static void register(String key, IPosterPlugin posterPlugin) {
        posterPlugins.put(key, posterPlugin);
    }

    private static void register(String key, IMoviePosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            logger.debug(logMessage + posterPlugin.getClass().getName() + " registered as Movie Poster Plugin with key '" + key + "'");
            moviePosterPlugins.put(key, posterPlugin);
            register(key, (IPosterPlugin) posterPlugin);
        } else {
            logger.debug(logMessage + posterPlugin.getClass().getName() + " available, but not loaded use key '" + key + "' to enable it.");
        }
    }

    public static void register(String key, ITvShowPosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            logger.debug(logMessage + posterPlugin.getClass().getName() + " registered as TvShow Poster Plugin with key '" + key + "'");
            tvShowPosterPlugins.put(key, posterPlugin);
            register(key, (IPosterPlugin) posterPlugin);
        } else {
            logger.debug(logMessage + posterPlugin.getClass().getName() + " available, but not loaded use key '" + key + "' to enable it.");
        }
    }

    public static void scan(Movie movie) {
        // check the default ID for a 0 or -1 and skip poster processing
        String id = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (!movie.isScrapeLibrary() || id.equals("0") || id.equals("-1")) {
            logger.debug(logMessage + "Skipping online poster search for " + movie.getBaseFilename());
            return;
        }

        logger.debug(logMessage + "Searching online for " + movie.getBaseFilename());
        IImage posterImage = getPosterURL(movie);
        if (StringTools.isValidString(posterImage.getUrl())) {
            movie.setPosterURL(posterImage.getUrl());
            ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, Movie.UNKNOWN, false);
            movie.addArtwork(new Artwork(ArtworkType.Poster, posterImage.getSubimage(), posterImage.getUrl(), artworkFile));
        }
    }

    /**
     * Return the dimensions of a local image file
     *
     * @param imageFile
     * @return Dimension
     */
    public static Dimension getFileImageSize(File imageFile) {
        Dimension imageSize = new Dimension(0, 0);

        ImageInputStream in = null;
        try {
            in = ImageIO.createImageInputStream(imageFile);
            @SuppressWarnings("rawtypes")
            Iterator readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = (ImageReader) readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } catch (IOException e) {
                    logger.error(logMessage + "Failed to read image dimensions for " + imageFile.getName());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            return imageSize;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        return imageSize;
    }
}
