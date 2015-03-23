/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.poster.IMoviePosterPlugin;
import com.moviejukebox.plugin.poster.IPosterPlugin;
import com.moviejukebox.plugin.poster.ITvShowPosterPlugin;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.awt.Dimension;
import java.awt.color.CMMException;
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
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for poster files in local directory and from the Internet
 *
 * @author groll.troll
 * @author Stuart.Boston
 *
 * @version 1.0, 7 October 2008
 * @version 2.0 6 July 2009
 */
public final class PosterScanner {

    private static final Logger LOG = LoggerFactory.getLogger(PosterScanner.class);
    private static final Map<String, IPosterPlugin> PLUGINS;
    private static final Map<String, IMoviePosterPlugin> MOVIE_PLUGINS = new HashMap<>();
    private static final Map<String, ITvShowPosterPlugin> TV_PLUGINS = new HashMap<>();
    private static final String EXISTING_MOVIE = "moviename";
    private static final String EXISTING_FIXED = "fixedcoverartname";
    private static final String EXISTING_NO = "no";
    // We get covert art scanner behaviour
    private static final String SEARCH_FOR_EXISTING_POSTER = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", EXISTING_MOVIE);
    // See if we use folder.* image or not
    // Note: We need the useFolderImage because of the special "folder.jpg" case in windows.
    private static final Boolean USE_FOLDER_IMAGE = PropertiesUtil.getBooleanProperty("poster.scanner.useFolderImage", Boolean.FALSE);
    // We get the fixed name property
    private static final String FIXED_POSTER_NAME = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
    private static final Collection<String> POSTER_EXTENSIONS = new ArrayList<>();
    private static final String POSTER_DIRECTORY;
    private static final Collection<String> POSTER_IMAGE_NAME = new ArrayList<>();
    private static final boolean POSTER_VALIDATE;
    private static final int POSTER_VALIDATE_MATCH;
    private static final boolean POSTER_VALIDATE_ASPECT;
    private static final int POSTER_WIDTH;
    private static final int POSTER_HEIGHT;
    private static final String TV_POSTER_SEARCH_PRIORITY;
    private static final String MOVIE_POSTER_SEARCH_PRIORITY;

    static {
        StringTokenizer st;

        if (USE_FOLDER_IMAGE) {
            st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.imageName", "folder,poster"), ",;|");
            while (st.hasMoreTokens()) {
                POSTER_IMAGE_NAME.add(st.nextToken());
            }
        }

        // We get valid extensions
        st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.coverArtExtensions", "jpg,png,gif"), ",;| ");
        while (st.hasMoreTokens()) {
            POSTER_EXTENSIONS.add(st.nextToken());
        }

        // We get Poster Directory if needed
        POSTER_DIRECTORY = PropertiesUtil.getProperty("poster.scanner.coverArtDirectory", "");

        TV_POSTER_SEARCH_PRIORITY = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "thetvdb,cdon,filmaffinity");
        MOVIE_POSTER_SEARCH_PRIORITY = PropertiesUtil.getProperty("poster.scanner.SearchPriority.movie",
                "themoviedb,impawards,imdb,moviecovers,google,yahoo,motechnet");

        POSTER_WIDTH = PropertiesUtil.getIntProperty("posters.width", 0);
        POSTER_HEIGHT = PropertiesUtil.getIntProperty("posters.height", 0);
        POSTER_VALIDATE = PropertiesUtil.getBooleanProperty("poster.scanner.Validate", Boolean.TRUE);
        POSTER_VALIDATE_MATCH = PropertiesUtil.getIntProperty("poster.scanner.ValidateMatch", 75);
        POSTER_VALIDATE_ASPECT = PropertiesUtil.getBooleanProperty("poster.scanner.ValidateAspect", Boolean.TRUE);

        // Load plugins
        PLUGINS = new HashMap<>();

        ServiceLoader<IMoviePosterPlugin> moviePosterPluginsSet = ServiceLoader.load(IMoviePosterPlugin.class);
        for (IMoviePosterPlugin iPosterPlugin : moviePosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

        ServiceLoader<ITvShowPosterPlugin> tvShowPosterPluginsSet = ServiceLoader.load(ITvShowPosterPlugin.class);
        for (ITvShowPosterPlugin iPosterPlugin : tvShowPosterPluginsSet) {
            register(iPosterPlugin.getName().toLowerCase().trim(), iPosterPlugin);
        }

    }

    private PosterScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    public static String scan(Jukebox jukebox, Movie movie) {
        if (SEARCH_FOR_EXISTING_POSTER.equalsIgnoreCase(EXISTING_NO)) {
            // nothing to do we return
            return Movie.UNKNOWN;
        }

        String localPosterBaseFilename;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullPosterFilename = parentPath;
        File localPosterFile;

        if (SEARCH_FOR_EXISTING_POSTER.equalsIgnoreCase(EXISTING_MOVIE)) {
            // Encode the basename to ensure that non-usable file system characters are replaced
            // Issue 1155 : YAMJ refuses to pickup fanart and poster for a movie -
            // Do not make safe file name before searching.
            localPosterBaseFilename = movie.getBaseFilename();
        } else if (SEARCH_FOR_EXISTING_POSTER.equalsIgnoreCase(EXISTING_FIXED)) {
            localPosterBaseFilename = FIXED_POSTER_NAME;
        } else {
            LOG.info("Wrong value for 'poster.scanner.searchForExistingCoverArt' property ('{}')!", SEARCH_FOR_EXISTING_POSTER);
            LOG.info("Expected '{}' or '{}'", EXISTING_MOVIE, EXISTING_FIXED);
            return Movie.UNKNOWN;
        }

        if (StringUtils.isNotBlank(POSTER_DIRECTORY)) {
            fullPosterFilename = StringTools.appendToPath(fullPosterFilename, POSTER_DIRECTORY);
        }

        // Check to see if the fullPosterFilename ends with a "\/" and only add it if needed
        // Usually this occurs because the files are at the root of a folder
        fullPosterFilename = StringTools.appendToPath(fullPosterFilename, localPosterBaseFilename);
        localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, POSTER_EXTENSIONS);
        boolean foundLocalPoster = localPosterFile.exists();

        // Try searching the fileCache for the filename, but only for non-fixed filenames
        if (!foundLocalPoster && !SEARCH_FOR_EXISTING_POSTER.equalsIgnoreCase(EXISTING_FIXED)) {
            Boolean searchInJukebox = Boolean.TRUE;
            // if the poster URL is invalid, but the poster filename is valid, then this is likely a recheck, so don't search on the jukebox folder
            if (StringTools.isNotValidString(movie.getPosterURL()) && StringTools.isValidString(movie.getPosterFilename())) {
                searchInJukebox = Boolean.FALSE;
            }

            localPosterFile = FileTools.findFilenameInCache(localPosterBaseFilename, POSTER_EXTENSIONS, jukebox, searchInJukebox);
            if (localPosterFile != null) {
                foundLocalPoster = Boolean.TRUE;
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
         * @version 1.0
         * @date 18th October 2008
         */
        if (!foundLocalPoster) {
            // If no poster has been found, try the foldername
            // No need to check the poster directory
            localPosterBaseFilename = FileTools.getParentFolderName(movie.getFile());

            if (USE_FOLDER_IMAGE) {
                // Checking for MovieFolderName.* AND folder.*
                LOG.debug("Checking for '{}.*' posters AND {}.* posters", localPosterBaseFilename, POSTER_IMAGE_NAME);
            } else {
                // Only checking for the MovieFolderName.* and not folder.*
                LOG.debug("Checking for '{}.*' posters", localPosterBaseFilename);
            }

            // Check for the directory name with extension for poster
            fullPosterFilename = StringTools.appendToPath(parentPath, localPosterBaseFilename);
            localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, POSTER_EXTENSIONS);
            foundLocalPoster = localPosterFile.exists();
            if (!foundLocalPoster && USE_FOLDER_IMAGE) {
                for (String imageFileName : POSTER_IMAGE_NAME) {
                    fullPosterFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile()), imageFileName);
                    localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, POSTER_EXTENSIONS);
                    foundLocalPoster = localPosterFile.exists();

                    if (!foundLocalPoster && movie.isTVShow()) {
                        // Get the parent directory and check that
                        fullPosterFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()), imageFileName);
                        localPosterFile = FileTools.findFileFromExtensions(fullPosterFilename, POSTER_EXTENSIONS);
                        foundLocalPoster = localPosterFile.exists();
                        if (foundLocalPoster) {
                            // We found the artwork so quit the loop
                            break;
                        }
                    } else {
                        // We found the artwork so quit the loop
                        break;
                    }
                }
            }
        }
        /*
         * END OF Folder Poster
         */

        // Check file attachments
        if (!foundLocalPoster) {
            localPosterFile = AttachmentScanner.extractAttachedPoster(movie);
            foundLocalPoster = (localPosterFile != null);
        }

        if (foundLocalPoster) {
            fullPosterFilename = localPosterFile.getAbsolutePath();
            Dimension imageSize = getFileImageSize(localPosterFile);
            LOG.debug("Local poster file {} found, size {} x {}", fullPosterFilename, imageSize.width, imageSize.height);

            String safePosterFilename = movie.getPosterFilename();
            String finalJukeboxPosterFileName = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), safePosterFilename);
            String tempJukeboxPosterFileName = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), safePosterFilename);

            File finalJukeboxFile = FileTools.fileCache.getFile(finalJukeboxPosterFileName);
            File tempJukeboxFile = new File(tempJukeboxPosterFileName);

            FileTools.makeDirsForFile(finalJukeboxFile);
            FileTools.makeDirsForFile(tempJukeboxFile);

            boolean copyLocalPoster = Boolean.FALSE;

//            logger.debug(LOG_MESSAGE + "finalJukeboxFile       : {}", finalJukeboxFile.getAbsolutePath());
//            logger.debug(LOG_MESSAGE + "tempJukeboxFile        : {}", tempJukeboxFile.getAbsolutePath());
//            logger.debug(LOG_MESSAGE + "finalJukeboxFile exists: {}", finalJukeboxFile.exists());
//            logger.debug(LOG_MESSAGE + "Local newer than temp? : {}", (tempJukeboxFile.exists() && FileTools.isNewer(localPosterFile, tempJukeboxFile)));
//            logger.debug(LOG_MESSAGE + "Posters same size?     : {}", (localPosterFile.length() != finalJukeboxFile.length()));
//            logger.debug(LOG_MESSAGE + "Local newer than final?: {}", (FileTools.isNewer(localPosterFile, finalJukeboxFile)));
            if (!finalJukeboxFile.exists()
                    || // temp jukebox file exists and is newer ?
                    (tempJukeboxFile.exists() && FileTools.isNewer(localPosterFile, tempJukeboxFile))
                    || // file size is different ?
                    (localPosterFile.length() != finalJukeboxFile.length())
                    || // local file is newer ?
                    (FileTools.isNewer(localPosterFile, finalJukeboxFile))) {
                // Force copy of local poster file
                copyLocalPoster = Boolean.TRUE;
            }

            if (copyLocalPoster) {
                FileTools.copyFile(localPosterFile, tempJukeboxFile);
                LOG.debug("'{}' has been copied to '{}'", fullPosterFilename, tempJukeboxPosterFileName);
            }
            // Update poster URL with local poster
            String posterURI = localPosterFile.toURI().toString();
            movie.setPosterURL(posterURI);

            return posterURI;
        } else {
            LOG.debug("No local poster found for {}", movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
    }

    private static String getPluginsCode() {
        StringBuilder response = new StringBuilder();

        Set<String> keySet = PLUGINS.keySet();
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
            st = new StringTokenizer(TV_POSTER_SEARCH_PRIORITY, ",");
        } else {
            st = new StringTokenizer(MOVIE_POSTER_SEARCH_PRIORITY, ",");
        }

        while (st.hasMoreTokens() && StringTools.isNotValidString(posterImage.getUrl())) {
            posterSearchToken = st.nextToken();

            IPosterPlugin iPosterPlugin = PLUGINS.get(posterSearchToken);

            // Check that plugin is register even on movie or tv
            if (iPosterPlugin == null) {
                LOG.error("'{}' plugin doesn't exist, please check your moviejukebox properties. Valid plugins are : {}", posterSearchToken, getPluginsCode());
            }

            String msg;

            if (movie.isTVShow()) {
                iPosterPlugin = TV_PLUGINS.get(posterSearchToken);
                msg = "TvShow";
            } else {
                iPosterPlugin = MOVIE_PLUGINS.get(posterSearchToken);
                msg = "Movie";
            }

            if (iPosterPlugin == null) {
                LOG.info("{} is not a {} Poster plugin - skipping", posterSearchToken, msg);
            } else {
                LOG.debug("Using {} to search for a {} poster for {}", posterSearchToken, msg, movie.getTitle());
                posterImage = iPosterPlugin.getPosterUrl(movie, movie);
            }

            // Validate the poster- No need to validate if we're UNKNOWN
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl()) && POSTER_VALIDATE && !validatePoster(posterImage, POSTER_WIDTH, POSTER_HEIGHT, POSTER_VALIDATE_ASPECT)) {
                posterImage = Image.UNKNOWN;
            } else {
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterImage.getUrl())) {
                    LOG.debug("Poster URL found at {}: {}", posterSearchToken, posterImage.getUrl());
                    // TODO: This is a hack, but seeing as only one poster scanner uses it, it should be safe until it's all refactored to use the Artwork class
                    posterImage.setSubimage(posterSearchToken);
                    movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
                }
            }
        }

        return posterImage;
    }

    /**
     * Validate the artwork against the dimensions and aspect.
     *
     * @param posterImage
     * @return
     */
    public static boolean validatePoster(IImage posterImage) {
        return validatePoster(posterImage, POSTER_WIDTH, POSTER_HEIGHT, POSTER_VALIDATE_ASPECT);
    }

    /**
     * Validate the poster against the provided dimensions and aspect
     *
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
        if (!POSTER_VALIDATE) {
            return Boolean.TRUE;
        }

        if (StringTools.isNotValidString(posterImage.getUrl())) {
            return Boolean.FALSE;
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
            LOG.debug("{} rejected: URL is landscape format", posterImage);
            return Boolean.FALSE;
        }

        // Adjust poster width / height by the ValidateMatch figure
        int newPosterWidth = (posterWidth * POSTER_VALIDATE_MATCH) / 100;
        int newPosterHeight = (posterHeight * POSTER_VALIDATE_MATCH) / 100;

        if (urlWidth < newPosterWidth) {
            LOG.debug("{} rejected: URL width ({}) is smaller than poster width ({})", posterImage, urlWidth, newPosterWidth);
            return Boolean.FALSE;
        }

        if (urlHeight < newPosterHeight) {
            LOG.debug("{} rejected: URL height ({}) is smaller than poster height ({})", posterImage, urlHeight, newPosterHeight);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Read an URL and get the dimensions of the image.
     *
     * This will try to determine the image type from the URL, if that fails
     * then it will default to JPEG.
     *
     * If the reading of the image fails, then the other type (PNG or JPEG) will
     * be used instead in case there was an incorrectly named extension
     *
     * @param imageUrl
     * @return
     */
    public static Dimension getUrlDimensions(String imageUrl) {
        String imageExtension = FilenameUtils.getExtension(imageUrl);
        if (StringUtils.isBlank(imageExtension)) {
            imageExtension = "jpeg";
        }

        Dimension imageDimension = getUrlDimensions(imageUrl, imageExtension);

        if (imageDimension.equals(new Dimension(0, 0))) {
            LOG.info("Looks like an invalid image, trying a different reader for URL: {}", imageUrl);
            if ("png".equals(imageExtension)) {
                imageExtension = "jpeg";
            } else {
                imageExtension = "png";
            }
            imageDimension = getUrlDimensions(imageUrl, imageExtension);
        }

        return imageDimension;
    }

    /**
     * Read an URL and get the dimensions of the image using a specific image
     * type
     *
     * @param imageUrl
     * @param imageType
     * @return
     */
    public static Dimension getUrlDimensions(String imageUrl, String imageType) {
        Dimension imageDimension = new Dimension(0, 0);

        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix(imageType);

        if (readers.hasNext()) {
            ImageReader reader = (ImageReader) readers.next();

            InputStream in = null;
            ImageInputStream iis = null;

            try {
                URL url = new URL(imageUrl);
                in = url.openStream();
                iis = ImageIO.createImageInputStream(in);
                reader.setInput(iis, Boolean.TRUE);

                imageDimension.setSize(reader.getWidth(0), reader.getHeight(0));
            } catch (IOException ex) {
                LOG.debug("getUrlDimensions error: {}: can't open url: {}", ex.getMessage(), imageUrl);
            } finally {
                reader.dispose();

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

        return imageDimension;
    }

    public static void register(String key, IPosterPlugin posterPlugin) {
        PLUGINS.put(key, posterPlugin);
    }

    private static void register(String key, IMoviePosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            LOG.debug("{} registered as Movie Poster Plugin with key '{}'", posterPlugin.getClass().getName(), key);
            MOVIE_PLUGINS.put(key, posterPlugin);
            register(key, (IPosterPlugin) posterPlugin);
        } else {
            LOG.debug("{} available, but not loaded use key '{}' to enable it.", posterPlugin.getClass().getName(), key);
        }
    }

    public static void register(String key, ITvShowPosterPlugin posterPlugin) {
        if (posterPlugin.isNeeded()) {
            LOG.debug("{} registered as TvShow Poster Plugin with key '{}'", posterPlugin.getClass().getName(), key);
            TV_PLUGINS.put(key, posterPlugin);
            register(key, (IPosterPlugin) posterPlugin);
        } else {
            LOG.debug("{} available, but not loaded use key '{}' to enable it.", posterPlugin.getClass().getName(), key);
        }
    }

    public static void scan(Movie movie) {
        // check the default ID for a 0 or -1 and skip poster processing
        String id = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        if (!movie.isScrapeLibrary() || "0".equals(id) || "-1".equals(id)) {
            LOG.debug("Skipping online poster search for {}", movie.getBaseFilename());
            return;
        }

        LOG.debug("Searching online for {}", movie.getBaseFilename());
        IImage posterImage = getPosterURL(movie);
        if (StringTools.isValidString(posterImage.getUrl())) {
            movie.setPosterURL(posterImage.getUrl());
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
        ImageReader reader = null;

        try {
            in = ImageIO.createImageInputStream(imageFile);
            @SuppressWarnings("rawtypes")
            Iterator readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                reader = (ImageReader) readers.next();
                if (reader != null) {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                }
            }
        } catch (IOException | CMMException ex) {
            LOG.error("Failed to read image dimensions for {}", imageFile.getName());
            LOG.error("Error: {}", ex.getMessage());
            return imageSize;
        } finally {
            if (reader != null) {
                reader.dispose();
            }

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
