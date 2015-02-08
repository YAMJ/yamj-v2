/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.Artwork.ArtworkPriority;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultImagePlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SkinProperties;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for artwork
 *
 * @author Stuart
 */
public abstract class ArtworkScanner implements IArtworkScanner {

    //These are the properties used:
    //  {artworkType}.scanner.artworkSearchLocal - true/false
    //  {artworkType}.scanner.artworkExtensions - The extensions to search for the artwork
    //  {artworkType}.scanner.artworkTokenOriginal - A token that delimits the artwork, such as ".fanart" or ".banner"
    //  {artworkType}.scanner.imageName - List of fixed artwork names
    //  {artworkType}.scanner.Validate -
    //  {artworkType}.scanner.ValidateMatch -
    //  {artworkType}.scanner.ValidateAspect -
    //  {artworkType}.scanner.artworkDirectory -
    //  {artworkType}.scanner.artworkPriority - mjb.skin.dir - Skin directory
    //
    // From skin.properties
    //  ???.width
    //  ???.height
    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScanner.class);
    private static final String SPLITTER = ",;|";
    protected final WebBrowser webBrowser = new WebBrowser();
    protected MovieImagePlugin artworkImagePlugin;
    // Location of the skin files used to get the dummy images from for missing artwork
    protected String skinHome = SkinProperties.getSkinHome();
    // *** Scanner settings
    // The type of the artwork. Will be used to load the other properties. When using for properties, must be lowercase
    protected ArtworkType artworkType;
    // The artwork type name to use in properties (all lowercase)
    protected String artworkTypeName;
    protected boolean artworkOverwrite = Boolean.FALSE;
    // *** Search settings
    // Should we search for local artwork
    protected boolean artworkSearchLocal;
    // Whether or not to download artwork for a Movie
    protected boolean artworkDownloadMovie;
    // Whether or not to download artwork for a TV Show
    protected boolean artworkDownloadTv;
    // *** Local search settings
    protected Collection<String> artworkExtensions = new ArrayList<>();
    // List of fixed artwork names
    protected Collection<String> artworkImageName = new ArrayList<>();
    // The name of the artwork directory to search from from either the video directory or the root of the library
    private final String artworkDirectory;
    // The order of the searches performed for the local artwork.
    private final List<ArtworkPriority> artworkPriority = new ArrayList<>();
    // *** Artwork attributes
    // Format of the artwork to save, e.g. JPG, PNG, etc.
    protected String artworkFormat;
    // The suffix of the artwork filename to search for.
    protected String artworkTokenOriginal;
    // The suffix of the artwork filename to search for.
    protected String artworkTokenJukebox;
    // The width of the image from the skin.properties for use in the validation routine
    protected int artworkWidth;
    // The height of the image from the skin.properties for use in the validation routine
    protected int artworkHeight;
    // *** Artwork validation
    // Should the artwork be validated or not.
    protected boolean artworkValidate;
    // Should the artwork be validated for it's aspect
    protected boolean artworkValidateAspect;
    // How close the image should be to the expected dimensions (artworkWidth & artworkHeight)
    protected int artworkValidateMatch;
    // *** Artwork dummy settings
    protected static boolean useDummy;
    // *** Artwork original file settings
    protected static boolean saveOriginal;

    /**
     * Construct the
     *
     * @param conArtworkType
     */
    public ArtworkScanner(ArtworkType conArtworkType) {
        setArtworkType(conArtworkType);

        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty(conArtworkType + ".scanner.imageName", ""), SPLITTER);
        while (st.hasMoreTokens()) {
            artworkImageName.add(st.nextToken());
        }

        // Get artwork scanner behaviour
        artworkSearchLocal = PropertiesUtil.getBooleanProperty(artworkTypeName + ".scanner.searchForExistingArtwork", Boolean.FALSE);
        artworkDownloadMovie = PropertiesUtil.getBooleanProperty(artworkTypeName + ".movie.download", Boolean.FALSE);
        artworkDownloadTv = PropertiesUtil.getBooleanProperty(artworkTypeName + ".tv.download", Boolean.FALSE);

        setArtworkExtensions(PropertiesUtil.getProperty(artworkTypeName + ".scanner.artworkExtensions", "jpg,png,gif"));
        artworkTokenOriginal = PropertiesUtil.getProperty(artworkTypeName + ".scanner.artworkToken", "");
        if (StringUtils.isBlank(artworkTokenOriginal) && artworkType != ArtworkType.POSTER) {
            // If the token is empty, create a default from the name
            artworkTokenOriginal = "." + artworkTypeName + "_orig";
        }

        artworkTokenJukebox = PropertiesUtil.getProperty(artworkTypeName + ".scanner.artworkToken", "");
        if (StringUtils.isBlank(artworkTokenJukebox) && artworkType != ArtworkType.POSTER) {
            artworkTokenJukebox = "." + artworkTypeName + "_jb";
        }

        artworkFormat = PropertiesUtil.getProperty(artworkTypeName + ".format", "jpg");
        setArtworkImageName(PropertiesUtil.getProperty(artworkTypeName + ".scanner.imageName", ""));

        artworkValidate = PropertiesUtil.getBooleanProperty(artworkTypeName + ".scanner.Validate", Boolean.TRUE);
        artworkValidateMatch = PropertiesUtil.getIntProperty(artworkTypeName + ".scanner.ValidateMatch", 75);
        artworkValidateAspect = PropertiesUtil.getBooleanProperty(artworkTypeName + ".scanner.ValidateAspect", Boolean.TRUE);

        artworkDirectory = PropertiesUtil.getProperty(artworkTypeName + ".scanner.artworkDirectory", "");

        // Get the priority (order) that the artwork is searched for
        setArtworkPriority(PropertiesUtil.getProperty(artworkTypeName + ".scanner.artworkPriority", "video,folder,fixed,series,directory"));

        // Should dummy artwork be created
        useDummy = PropertiesUtil.getBooleanProperty(artworkTypeName + ".scanner.useDummy", Boolean.TRUE);

        // Should the original artwork be saved
        saveOriginal = PropertiesUtil.getBooleanProperty(artworkTypeName + ".scanner.saveOriginal", Boolean.TRUE);

        // Get & set the default artwork dimensions
        setArtworkDimensions();

        // Set the image plugin
        setArtworkImagePlugin();
    }

    /**
     * A catch all routine to scan local artwork and then online artwork.
     *
     * @param jukebox
     * @param movie
     * @return
     */
    @Override
    public final String scan(Jukebox jukebox, Movie movie) {
        // If we are not required, leave
        if (!isSearchRequired()) {
            LOG.debug("{} {} not required", movie.getBaseFilename(), artworkTypeName);
            return getArtworkUrl(movie);
        }

        // If forceOverwrite is set, clear the Url so we will search again
        if (isOverwrite()) {
            LOG.debug("forceOverwite set, clearing URL before search"); // XXX DEBUG
            setArtworkUrl(movie, Movie.UNKNOWN);
            setOriginalFilename(movie, Movie.UNKNOWN);
        } else {
            // Check to see if we have a valid URL and it's not dirty
            if (StringTools.isValidString(getArtworkUrl(movie)) && !(isDirtyArtwork(movie) || movie.isDirty(DirtyFlag.INFO))) {
                // Valid URL, so exit with that
                LOG.debug("URL for {} looks valid, skipping online search: {}", movie.getBaseName(), getArtworkUrl(movie));
                if (StringTools.isNotValidString(getOriginalFilename(movie))) {
                    setOriginalFilename(movie, makeSafeOriginalFilename(movie, Boolean.TRUE));
                }
                return getArtworkUrl(movie);
            } else {
                // Not a valid URL, check to see if the artwork is dirty or the movie is dirty
                if (!(isDirtyArtwork(movie) || movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.RECHECK))) {
                    // Artwork and movie is not dirty, so don't process
                    LOG.debug("URL update not required (not overwrite, dirty or recheck)");
                    return getArtworkUrl(movie);
                }
            }
        }

        String artworkUrl;
        if (isSearchLocal()) {
            LOG.debug("Scanning for local artwork for {}", movie.getBaseName());
            artworkUrl = scanLocalArtwork(jukebox, movie);
            LOG.debug("ScanLocalArtwork returned: {}", artworkUrl);
            if (StringTools.isValidString(artworkUrl)) {
                // Update the movie artwork URL
                setArtworkUrl(movie, artworkUrl);

                // Only set the filename if we have an artwork URL
                setOriginalFilename(movie, makeSafeOriginalFilename(movie, Boolean.TRUE));

                // Save the artwork to the jukebox
                copyLocalArtwork(jukebox, movie);
                return artworkUrl;
            }
        } else {
            artworkUrl = Movie.UNKNOWN;
        }

        if (StringTools.isNotValidString(artworkUrl) && isSearchOnline(movie)) {
            LOG.debug("Scanning for online artwork for {}", movie.getBaseName());
            artworkUrl = scanOnlineArtwork(movie);
            LOG.debug("ScanOnlineArtwork returned: {}", artworkUrl);

            if (StringTools.isValidString(artworkUrl)) {
                // Update the movie artwork URL
                setArtworkUrl(movie, artworkUrl);

                // Only set the filename if we have an artwork URL
                setOriginalFilename(movie, makeSafeOriginalFilename(movie, Boolean.TRUE));

                // Save the artwork to the jukebox
                saveArtworkToJukebox(jukebox, movie);
            } else {
                LOG.debug("No online artwork found for {}", movie.getBaseName());
            }
        }

        return artworkUrl;
    }

    /**
     * Save the artwork to the jukebox
     *
     * @param jukebox
     * @param movie
     * @return
     */
    @Override
    public boolean saveArtworkToJukebox(Jukebox jukebox, Movie movie) {
        boolean returnValue = Boolean.TRUE;
        /*
         * 2) Determing the type of the artwork - either local or online
         * 2a) Download online artwork and save to temp jukebox
         * 2b) Copy local artwork
         * 2c) Use dummy artwork if required
         * 3) Copy original artwork to jukebox if required
         */

        String artworkFilename = getOriginalFilename(movie);
        File artworkFile = FileTools.fileCache.getFile(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename));

        // Does the artwork need to be overwritten?
        if (!artworkOverwrite && artworkFile.exists()) {
            LOG.debug("{}: Artwork does not need to be saved", movie.getBaseFilename());
            return returnValue;
        }

        String artworkUrl = getArtworkUrl(movie);

        // Determine the type of artwork, either local or online and act accordingly
        if (artworkUrl.startsWith("http")) {
            // Looks like a URL so download it
            returnValue = downloadArtwork(jukebox, movie);
        } else {
            // Looks like a file, so copy it
            returnValue = copyLocalArtwork(jukebox, movie);
        }

        // If we Successfully copied or downloaded the artwork, we need to process it
        if (returnValue) {
            returnValue = processArtwork(jukebox, movie);
        }

        return returnValue;
    }

    /**
     * Save the artwork to the jukebox folder
     *
     * This will be the temp folder by default.
     *
     * It will not post process the image.
     *
     * TODO: Parameter to control if the original artwork is saved in the jukebox or not. We should save this in an
     * "originalArtwork" folder or something
     *
     * @param jukebox
     * @param movie
     * @return the status of the save. True if saved correctly, false otherwise.
     */
    public boolean downloadArtwork(Jukebox jukebox, Movie movie) {
        String artworkUrl = getArtworkUrl(movie);
        String artworkFilename = getOriginalFilename(movie);

        if (StringTools.isNotValidString(artworkUrl)) {
            LOG.debug("Invalid artwork URL, artwork not copied.");
            return Boolean.FALSE;
        }

        if (StringTools.isNotValidString(artworkFilename)) {
            // Create the filename if not found
            setOriginalFilename(movie, makeSafeOriginalFilename(movie, Boolean.TRUE));
        }

        String artworkPath = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename);
        if (!artworkOverwrite && FileTools.fileCache.fileExists(artworkPath)) {
            LOG.debug("{} exists for {}", artworkTypeName, movie.getBaseName());
            return Boolean.TRUE;
        }

        artworkPath = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename);
        File artworkFile = new File(artworkPath);   // This is the temp folder, don't use filecache
        LOG.debug("Saving {} for {} to '{}'", artworkTypeName, movie.getBaseName(), artworkPath);

        boolean returnValue;
        try {
            returnValue = FileTools.downloadImage(artworkFile, artworkUrl);
        } catch (IOException ex) {
            LOG.debug("Failed to download {}: {}, Error: {}", artworkType, artworkUrl, ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
            returnValue = Boolean.FALSE;
        }

        return returnValue;

    }

    /**
     * Copy the local artwork file to the jukebox.
     *
     * If there's no URL do not create dummy artwork
     *
     * @param jukebox
     * @param movie
     * @return
     */
    protected boolean copyLocalArtwork(Jukebox jukebox, Movie movie) {
        String artworkUrl = getArtworkUrl(movie);
        boolean returnValue;

        if (artworkUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
            LOG.debug("No local {} found for {}", artworkType, movie.getBaseName());
            returnValue = Boolean.FALSE;
        } else {
            LOG.debug("{}: '{}' found", movie.getBaseName(), artworkUrl);
            if (overwriteArtwork(jukebox, movie)) {
                String destFilename = getOriginalFilename(movie);
                String destFullPath = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), destFilename);

                returnValue = FileTools.copyFile(artworkUrl, destFullPath);
                if (returnValue) {
                    LOG.debug("'{}' has been copied to '{}'", artworkUrl, destFilename);
                } else {
                    LOG.debug("Failed to copy '{}' to '{}'", artworkUrl, destFilename);
                }
            } else {
                // Don't copy the file
                LOG.debug("'{}' does not need to be copied", artworkUrl);
                returnValue = Boolean.TRUE;
            }
        }
        return returnValue;
    }

    /**
     * Process the artwork to apply the graphic manipulations required for display in the skin
     *
     * Will leave the original file in place (if required) and output the changes as a new file
     *
     * @param jukebox
     * @param movie
     * @return
     */
    public boolean processArtwork(Jukebox jukebox, Movie movie) {
        boolean returnValue;

        String originalFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), getOriginalFilename(movie));
        File originalFile = new File(originalFilename);
        if (!originalFile.exists()) {
            LOG.debug("File not found: {}", originalFilename);
            boolean saveValue = saveArtworkToJukebox(jukebox, movie);
            if (!saveValue) {
                LOG.debug("Failed to save file to jukebox: {}", originalFilename);
                return Boolean.FALSE;
            }
        }

        String processedFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), getOriginalFilename(movie));
        File processedFile = new File(processedFilename);

        try {
            BufferedImage artworkImage = GraphicTools.loadJPEGImage(originalFile);

            if (artworkImage != null) {
                artworkImage = artworkImagePlugin.generate(movie, artworkImage, getPropertyName(), null);
                GraphicTools.saveImageToDisk(artworkImage, processedFile.getAbsolutePath());
                returnValue = Boolean.TRUE;
            } else {
                setOriginalFilename(movie, Movie.UNKNOWN);
                setArtworkUrl(movie, Movie.UNKNOWN);
                returnValue = Boolean.FALSE;
            }
        } catch (ImageReadException ex) {
            LOG.debug("Failed to read: {}, Error: {}", originalFile.getAbsolutePath(), ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
            returnValue = Boolean.FALSE;
        } catch (IOException ex) {
            LOG.debug("Failed to process: {}, Error: {}", originalFile.getAbsolutePath(), ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
            returnValue = Boolean.FALSE;
        }

        if (returnValue) {
            LOG.debug("Processed '{}'", originalFile.getAbsolutePath());
        } else {
            LOG.debug("Failed to process artwork '{}'", originalFile.getAbsolutePath());
        }

        return returnValue;
    }

    /**
     * Process the artwork for a Set
     *
     * @param jukebox
     * @param movie
     * @return
     */
    public boolean processSetArtwork(Jukebox jukebox, Movie movie) {
        if (isSearchRequired()) {
            // Store the current artwork file and URL incase nothing is found
            String originalFilename = getOriginalFilename(movie);
            String originalUrl = getArtworkUrl(movie);

            // Set a default banner filename in case it's not found during the scan
            String safeOriginalFilename = makeSafeOriginalFilename(movie, Boolean.TRUE);
            setOriginalFilename(movie, safeOriginalFilename);
            setJukeboxFilename(movie, makeSafeJukeboxFilename(movie, Boolean.TRUE));

            String bannerUrl = scanLocalArtwork(jukebox, movie);
            if (StringTools.isValidString(bannerUrl)) {
                LOG.debug("Local set banner found: {}", bannerUrl);
            } else {
                // updateTvBanner(jukebox, movie, tools.imagePlugin);
                LOG.debug("Local set banner ({}) not found, using '{}'", safeOriginalFilename, originalFilename);
                setOriginalFilename(movie, originalFilename);
                setArtworkUrl(movie, originalUrl);
            }
            saveArtworkToJukebox(jukebox, movie);
            processArtwork(jukebox, movie);
        } else {
            LOG.debug("Artwork not required for {}", movie.getBaseName());
        }

        return false;
    }

    /**
     * Get the original artwork filename from the movie object based on the artwork type
     *
     * @param movie
     * @return the Artwork Filename
     */
    @Override
    public abstract String getOriginalFilename(Movie movie);

    /**
     * Get the artwork filename for the jukebox from the movie object based on the artwork type
     *
     * @param movie
     * @return the Artwork filename
     */
    @Override
    public abstract String getJukeboxFilename(Movie movie);

    /**
     * Get the artwork URL from the movie object based on the artwork type
     *
     * @param movie
     * @return the Artwork URL
     */
    @Override
    public abstract String getArtworkUrl(Movie movie);

    @Override
    public abstract boolean isDirtyArtwork(Movie movie);

    @Override
    public final boolean isSearchRequired() {
        // Assume the artwork is required
        return isSearchLocal() || isSearchOnline();
    }

    @Override
    public final boolean isSearchLocal() {
        return artworkSearchLocal;
    }

    @Override
    public final boolean isSearchOnline() {
        return artworkDownloadTv || artworkDownloadMovie;
    }

    @Override
    public final boolean isSearchOnline(Movie movie) {
        if (!movie.isScrapeLibrary()) {
            return Boolean.FALSE;
        }

        if (!isSearchOnline()) {
            return Boolean.FALSE;
        }

        if (movie.isTVShow() && !artworkDownloadTv) {
            return Boolean.FALSE;
        } else if (!movie.isTVShow() && !artworkDownloadMovie) {
            return Boolean.FALSE;
        }

        for (Entry<String, String> e : movie.getIdMap().entrySet()) {
            if (("0".equals(e.getValue())) || ("-1".equals(e.getValue()))) {
                // Stop and return
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Check to see if the artwork type is required or not
     *
     * @param artworkType
     * @return
     */
    public static boolean isRequired(ArtworkType artworkType) {
        return isRequired(artworkType.toString().toLowerCase());
    }

    /**
     * Determine if the artwork type is required or not
     *
     * @param artworkTypeString
     * @return
     */
    public static boolean isRequired(String artworkTypeString) {
        boolean sArtworkLocalSearch = PropertiesUtil.getBooleanProperty(artworkTypeString + ".scanner.searchForExistingArtwork", Boolean.FALSE);
        boolean sArtworkMovieDownload = PropertiesUtil.getBooleanProperty(artworkTypeString + ".movie.download", Boolean.FALSE);
        boolean sArtworkTvDownload = PropertiesUtil.getBooleanProperty(artworkTypeString + ".tv.download", Boolean.FALSE);

        return (sArtworkLocalSearch || sArtworkMovieDownload || sArtworkTvDownload);
    }

    /**
     * Return a list of the required artwork types
     *
     * @return
     */
    public static Set<ArtworkType> getRequiredArtworkTypes() {
        Set<ArtworkType> artworkTypeRequired = EnumSet.noneOf(ArtworkType.class);
        for (ArtworkType artworkType : EnumSet.allOf(ArtworkType.class)) {
            if (isRequired(artworkType)) {
                artworkTypeRequired.add(artworkType);
            }
        }
        return artworkTypeRequired;
    }

    /**
     * Scan for any local artwork and return the path to it.
     *
     * This should only be called by the scanLocalArtwork method in the derived classes.
     *
     * Note: This will update the movie information for this artwork
     *
     * @param jukebox
     * @param movie
     * @param artworkImagePlugin
     * @return The URL (path) to the first found artwork in the priority list
     */
    protected String scanLocalArtwork(Jukebox jukebox, Movie movie, MovieImagePlugin artworkImagePlugin) {
        // Check to see if we are required
        if (!isSearchLocal()) {
            // return the current URL
            return getArtworkUrl(movie);
        }

        String artworkUrl = Movie.UNKNOWN;
        String artworkFilename = movie.getBaseFilename() + artworkTokenOriginal;

        LOG.debug("Searching for '{}'", artworkFilename);

        for (ArtworkPriority artworkSearch : artworkPriority) {
            if (StringTools.isValidString(artworkUrl)) {
                // We've got a valid URL, stop scanning
                break;
            }

            if (artworkSearch.equals(ArtworkPriority.VIDEO)) {
                artworkUrl = scanVideoArtwork(movie, artworkFilename);
                LOG.trace("{} scanVideoArtwork    : {}", movie.getBaseFilename(), artworkUrl);
                continue;
            }

            if (artworkSearch.equals(ArtworkPriority.FOLDER)) {
                artworkUrl = scanFolderArtwork(movie);
                LOG.trace("{} scanFolderArtwork    : {}", movie.getBaseFilename(), artworkUrl);
                continue;
            }

            if (artworkSearch.equals(ArtworkPriority.FIXED)) {
                artworkUrl = scanFixedArtwork(movie);
                LOG.trace("{} scanFixedArtwork    : {}", movie.getBaseFilename(), artworkUrl);
                continue;
            }

            if (artworkSearch.equals(ArtworkPriority.SERIES)) {
                // This is only for TV Sets as it searches the directory above the one the episode is in for fixed artwork
                if (movie.isTVShow() && movie.isSetMaster()) {
                    artworkUrl = scanTvSeriesArtwork(movie);
                    LOG.trace("{} scanTvSeriesArtwork    : {}", movie.getBaseFilename(), artworkUrl);
                }
                continue;
            }

            if (artworkSearch.equals(ArtworkPriority.DIRECTORY)) {
                artworkUrl = scanArtworkDirectory(movie, artworkFilename);
                LOG.trace("{} scanArtworkDirectory    : {}", movie.getBaseFilename(), artworkUrl);
            }

        }

        if (StringTools.isValidString(artworkUrl)) {
            LOG.debug("Found artwork for {}: {}", movie.getBaseName(), artworkUrl);
        } else {
            LOG.debug("No local artwork found for {}", movie.getBaseName());
        }
        setArtworkUrl(movie, artworkUrl);

        return artworkUrl;
    }

    @Override
    public abstract void setOriginalFilename(Movie movie, String artworkFilename);

    @Override
    public abstract void setArtworkUrl(Movie movie, String artworkUrl);

    /**
     * Updates the artwork by either copying the local file or downloading the artwork
     *
     * USE: saveArtworkToJukebox method instead
     *
     * @param jukebox
     * @param movie
     */
    @Deprecated
    public void updateArtwork(Jukebox jukebox, Movie movie) {
        String artworkFilename = getOriginalFilename(movie);
        String artworkUrl = getArtworkUrl(movie);
        String artworkDummy = getDummyFilename();

        File artworkFile = FileTools.fileCache.getFile(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename));
        File tmpDestFile = new File(StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename));

        // Do not overwrite existing artwork, unless there is a new URL in the nfo file.
        if ((!tmpDestFile.exists() && !artworkFile.exists()) || isDirtyArtwork(movie) || isOverwrite()) {
            FileTools.makeDirsForFile(artworkFile);

            if (artworkUrl == null || artworkUrl.equals(Movie.UNKNOWN)) {
                LOG.debug("Dummy {} used for {}", artworkType, movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + artworkDummy), tmpDestFile);
            } else {
                if (StringTools.isValidString(artworkDummy)) {
                    try {
                        LOG.debug("Saving {} for {} to {}", artworkType, movie.getBaseName(), tmpDestFile.getName());
                        FileTools.downloadImage(tmpDestFile, artworkUrl);
                    } catch (IOException ex) {
                        LOG.debug("Failed downloading {}: {} - {}", artworkType, artworkUrl, ex.getMessage());
                        FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + artworkDummy), tmpDestFile);
                    }
                } else {
                    LOG.debug("No dummy artwork ('{}') for {}", artworkDummy, artworkType);
                }
            }
        } else {
            saveArtworkToJukebox(jukebox, movie);
        }
    }

    @Override
    public boolean validateArtwork(IImage artworkImage) {
        return validateArtwork(artworkImage, artworkWidth, artworkHeight, artworkValidateAspect);
    }

    /**
     * Get the size of the file at the end of the URL
     *
     * Taken from: http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     *
     * @param artworkImage Artwork image to check
     * @param artworkWidth The width to check
     * @param artworkHeight The height to check
     * @param checkAspect Should the aspect ratio be checked
     * @return True if the poster is good, false otherwise
     */
    @Override
    public boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect) {
        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader) readers.next();
        int urlWidth;
        int urlHeight;
        float urlAspect;

        if (!artworkValidate) {
            return Boolean.TRUE;
        }

        if (artworkImage.getUrl().equalsIgnoreCase(Movie.UNKNOWN)) {
            return Boolean.FALSE;
        }

        try {
            URL url = new URL(artworkImage.getUrl());
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, Boolean.TRUE);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);
        } catch (IOException ex) {
            LOG.debug("ValidateArtwork error: {}: can't open URL", ex.getMessage());
            return Boolean.FALSE; // Quit and return a Boolean.FALSE poster
        }

        urlAspect = (float) urlWidth / (float) urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            LOG.debug("{} rejected: URL is wrong aspect (portrait/landscape)", artworkImage);
            return Boolean.FALSE;
        }

        // Adjust artwork width / height by the ValidateMatch figure
        int newArtworkWidth = artworkWidth * (artworkValidateMatch / 100);
        int newArtworkHeight = artworkHeight * (artworkValidateMatch / 100);

        if (urlWidth < newArtworkWidth) {
            LOG.debug("{} rejected: URL width ({}) is smaller than artwork width ({})", artworkImage, urlWidth, newArtworkWidth);
            return Boolean.FALSE;
        }

        if (urlHeight < newArtworkHeight) {
            LOG.debug("{} rejected: URL height ({}) is smaller than artwork height ({})", artworkImage, urlHeight, newArtworkHeight);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * returns the forceOverwite parameter for this artwork type.
     *
     * This is set in the constructor of each of the sub-scanners
     *
     * @return
     */
    protected boolean isOverwrite() {
        return artworkOverwrite;
    }

    /**
     * Determine if the artwork should be overwritten
     *
     * Checks to see if the artwork exists in the jukebox folders (temp and final)
     *
     * Checks the overwrite parameters Checks to see if the local artwork is newer
     *
     * @param jukebox
     * @param movie
     * @return
     */
    protected final boolean overwriteArtwork(Jukebox jukebox, Movie movie) {
        if (isOverwrite()) {
            setDirtyArtwork(movie, Boolean.TRUE);
            LOG.trace("{}: Artwork overwrite set, artwork will be overwritten", movie.getBaseFilename());
            return Boolean.TRUE;
        }

        if (isDirtyArtwork(movie)) {
            LOG.trace("{}: Dirty artwork set, artwork will be overwritten", movie.getBaseFilename());
            return Boolean.TRUE;
        }

        // This is the filename & path of the artwork that was found
        String artworkFilename = getOriginalFilename(movie);
        File artworkFile = new File(artworkFilename);   // Note: We can't use the filecache because the file might not be in there

        // This is the filename & path of the jukebox artwork
        String jukeboxFilename = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename);
        File jukeboxFile = FileTools.fileCache.getFile(jukeboxFilename);

        // This is the filename & path for temporary storage jukebox
        String tempLocFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename);
        File tempLocFile = new File(tempLocFilename);

        // Check to see if the found artwork newer than the temp jukebox
        if (FileTools.fileCache.fileExists(tempLocFile)) {
            if (FileTools.isNewer(artworkFile, tempLocFile)) {
                setDirtyArtwork(movie, Boolean.TRUE);
                LOG.trace("{}: Local artwork is newer, overwritting existing artwork", movie.getBaseName());
                return Boolean.TRUE;
            }
        } else if (FileTools.fileCache.fileExists(jukeboxFile)) {
            // Check to see if the found artwork newer than the existing jukebox
            if (FileTools.isNewer(artworkFile, jukeboxFile)) {
                setDirtyArtwork(movie, Boolean.TRUE);
                LOG.trace("{}: Local artwork is newer, overwritting existing jukebox artwork", movie.getBaseName());
                return Boolean.TRUE;
            } else {
                LOG.trace("{}: Local artwork is older, not copying", movie.getBaseName());
                return Boolean.FALSE;
            }
        } else {
            LOG.trace("{}: No jukebox file found, file will be copied", movie.getBaseName());
            return Boolean.TRUE;
        }

        // All the tests passed so don't overwrite
        return Boolean.FALSE;
    }

    /**
     * Scan an absolute or relative path for the movie images.
     *
     * The relative path should include the directory of the movie as well as the library root
     *
     * @param movie
     * @param artworkFilename
     * @return UNKNOWN or Absolute Path
     */
    protected String scanArtworkDirectory(Movie movie, String artworkFilename) {
        String artworkPath = Movie.UNKNOWN;

        if (StringUtils.isNotBlank(artworkDirectory)) {
            String artworkLibraryPath = StringTools.appendToPath(movie.getLibraryPath(), artworkDirectory);
            artworkPath = scanVideoArtwork(movie, artworkFilename, artworkDirectory);
            if (artworkPath.equalsIgnoreCase(Movie.UNKNOWN)) {
                artworkPath = scanDirectoryForArtwork(artworkFilename, artworkLibraryPath);
            }
        }
        return artworkPath;
    }

    /**
     * Scan the passed directory for the artwork filename
     *
     * @param artworkFilename
     * @param artworkPath
     * @return UNKNOWN or Absolute Path
     */
    protected String scanDirectoryForArtwork(String artworkFilename, String artworkPath) {
        String fullFilename = StringTools.appendToPath(artworkPath, artworkFilename);
        File artworkFile = FileTools.findFileFromExtensions(fullFilename, artworkExtensions);

        if (artworkFile.exists()) {
            return artworkFile.getAbsolutePath();
        }
        return Movie.UNKNOWN;
    }

    /**
     * Scan for fixed name artwork, such as folder.jpg, backdrop.png, etc.
     *
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanFixedArtwork(Movie movie) {
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullFilename;
        File artworkFile;

        for (String imageFileName : artworkImageName) {
            fullFilename = StringTools.appendToPath(parentPath, imageFileName);
            artworkFile = FileTools.findFileFromExtensions(fullFilename, artworkExtensions);

            if (artworkFile.exists()) {
                return artworkFile.getAbsolutePath();
            }
        }
        return Movie.UNKNOWN;
    }

    /**
     * Scan artwork that is named the same as the folder that it is in
     *
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanFolderArtwork(Movie movie) {
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullFilename = StringTools.appendToPath(parentPath, FileTools.getParentFolderName(movie.getFile()) + artworkTokenOriginal);

        File artworkFile = FileTools.findFileFromExtensions(fullFilename, artworkExtensions);

        if (artworkFile.exists()) {
            return artworkFile.getAbsolutePath();
        }

        return Movie.UNKNOWN;
    }

    /**
     * Scan for TV show SERIES artwork.
     *
     * This usually exists in the directory ABOVE the one the files reside in
     *
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanTvSeriesArtwork(Movie movie) {

        if (!movie.isTVShow()) {
            // Don't process if we are a movie.
            return Movie.UNKNOWN;
        }

        String parentPath = FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile());
        String fullFilename;
        File artworkFile;

        for (String imageFileName : artworkImageName) {
            fullFilename = StringTools.appendToPath(parentPath, imageFileName);
            artworkFile = FileTools.findFileFromExtensions(fullFilename, artworkExtensions);
            if (artworkFile.exists()) {
                return artworkFile.getAbsolutePath();
            }
        }

        return Movie.UNKNOWN;
    }

    /**
     * Scan for artwork named like: {videoFileName}{artworkToken}.{artworkExtensions}
     *
     * @param movie
     * @param artworkFilename
     * @return UNKNOWN or Absolute Path
     */
    protected String scanVideoArtwork(Movie movie, String artworkFilename) {
        return scanVideoArtwork(movie, artworkFilename, "");
    }

    /**
     * Scan for artwork named like: {videoFileName}{artworkToken}.{artworkExtensions}
     *
     * @param movie
     * @param artworkFilename
     * @param additionalPath A sub-directory of the movie to scan
     * @return UNKNOWN or Absolute Path
     */
    protected String scanVideoArtwork(Movie movie, String artworkFilename, String additionalPath) {
        String parentPath = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile()), StringUtils.trimToEmpty(additionalPath));
        return scanDirectoryForArtwork(artworkFilename, parentPath);
    }

    /**
     * Set the Image Plugin
     *
     * @param classNameString
     */
    protected void setImagePlugin(String classNameString) {
        String className;
        if (StringTools.isNotValidString(classNameString)) {
            // Use the default image plugin
            className = PropertiesUtil.getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin");
        } else {
            className = classNameString;
        }

        try {
            Thread artThread = Thread.currentThread();
            ClassLoader cl = artThread.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            artworkImagePlugin = pluginClass.newInstance();

            return;
        } catch (ClassNotFoundException ex) {
            LOG.error("Error instantiating imagePlugin: {} - class not found!", className);
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (InstantiationException ex) {
            LOG.error("Failed instantiating imagePlugin: {}", className);
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (IllegalAccessException ex) {
            LOG.error("Unable instantiating imagePlugin: {}", className);
            LOG.error(SystemTools.getStackTrace(ex));
        }

        LOG.error("Default plugin will be used instead.");
        // Use the background plugin for fanart, the image plugin for all others
        if (artworkType == ArtworkType.FANART) {
            artworkImagePlugin = new DefaultBackgroundPlugin();
        } else {
            artworkImagePlugin = new DefaultImagePlugin();
        }
    }

    /**
     * Set the default artwork dimensions for use in the validate routine
     */
    private void setArtworkDimensions() {
        // This should return a value, but if it doesn't then we'll default to "posters"
        String artworkPropertyType = getPropertyName();

        artworkWidth = PropertiesUtil.getIntProperty(artworkPropertyType + ".width", 0);
        artworkHeight = PropertiesUtil.getIntProperty(artworkPropertyType + ".height", 0);

        if ((artworkWidth == 0) || (artworkHeight == 0)) {
            // Get the poster type for looking up the defaults
            artworkPropertyType = ArtworkScanner.getPropertyName(ArtworkType.POSTER);
            // There was an issue with the correct properties, so use poster as a default.
            artworkWidth = PropertiesUtil.getIntProperty(artworkPropertyType + ".width", 400);
            artworkHeight = PropertiesUtil.getIntProperty(artworkPropertyType + ".height", 600);
        }
    }

    /**
     * Set the list of artwork extensions to search for using a delimited string
     *
     * @param extensions
     */
    private void setArtworkExtensions(String extensions) {
        StringTokenizer st = new StringTokenizer(extensions, SPLITTER);
        while (st.hasMoreTokens()) {
            artworkExtensions.add(st.nextToken());
        }
    }

    /**
     * Set the priority (order) that the artwork is searched for
     *
     * @param priority
     */
    private void setArtworkPriority(String priority) {
        StringTokenizer st = new StringTokenizer(priority, SPLITTER);
        while (st.hasMoreTokens()) {
            String tempPriority = st.nextToken();
            try {
                artworkPriority.add(ArtworkPriority.fromString(tempPriority));
            } catch (IllegalArgumentException ex) {
                LOG.warn("Failed to set artwork priority '{}', error: {}", tempPriority, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Set the list of folder artwork image names
     *
     * @param artworkImageNameString
     */
    private void setArtworkImageName(String artworkImageNameString) {
        StringTokenizer st = new StringTokenizer(artworkImageNameString, SPLITTER);

        while (st.hasMoreTokens()) {
            artworkImageName.add(st.nextToken());
        }
    }

    /**
     * Set the name for the artwork type to be used in logger messages
     *
     * @param setArtworkType The artwork type to instantiate the scanner for
     */
    private void setArtworkType(ArtworkType setArtworkType) {
        artworkType = setArtworkType;
        artworkTypeName = artworkType.toString().toLowerCase();
    }

    /**
     * Create a safe filename for the artwork
     *
     * @param movie
     * @param appendFormat Append the file format to the filename if required
     * @return
     */
    @Override
    public final String makeSafeOriginalFilename(Movie movie, boolean appendFormat) {
        StringBuilder filename = new StringBuilder();

        filename.append(FileTools.makeSafeFilename(movie.getBaseName()));
        if (StringTools.isValidString(artworkTokenOriginal)) {
            filename.append(artworkTokenOriginal);
        }

        if (appendFormat) {
            filename.append(".").append(artworkFormat);
        }

        LOG.debug("Safe original filename: {}", filename.toString());
        return filename.toString();
    }

    /**
     * Create a safe filename for the artwork
     *
     * @param movie
     * @param appendFormat Append the file format to the filename if required
     * @return
     */
    @Override
    public final String makeSafeJukeboxFilename(Movie movie, boolean appendFormat) {
        StringBuilder filename = new StringBuilder();

        filename.append(FileTools.makeSafeFilename(movie.getBaseName()));
        if (StringTools.isValidString(artworkTokenJukebox)) {
            filename.append(artworkTokenJukebox);
        }

        if (appendFormat) {
            filename.append(".").append(artworkFormat);
        }

        LOG.debug("Safe jukebox filename: {}", filename.toString());
        return filename.toString();
    }

    //<editor-fold defaultstate="collapsed" desc="Debug Methods">
    /**
     * Pretty print method for the debug property output
     *
     * @param propName
     * @param propValue
     * @param addTypeToOutput
     */
    protected final void debugProperty(String propName, Object propValue, boolean addTypeToOutput) {
        StringBuilder property = new StringBuilder("DEBUG - '");
        if (addTypeToOutput) {
            property.append(artworkTypeName);
        }
        property.append(StringUtils.rightPad(propName + "' ", (addTypeToOutput ? 40 : 40 + artworkTypeName.length()), ".")).append(" = ");
        property.append(propValue);
        LOG.debug(property.toString());
    }

    /**
     * Pretty print method for the debug property output
     *
     * @param propName
     * @param propValue
     */
    protected final void debugProperty(String propName, Object propValue) {
        debugProperty(propName, propValue, Boolean.TRUE);
    }

    /**
     * Output the properties used by this scanner
     */
    public final void debugOutput() {
        debugProperty(" Required?", isSearchRequired());
        debugProperty(" Local Required?", isSearchLocal());
        debugProperty(" TV Download?", artworkDownloadTv);
        debugProperty(" Movie Download?", artworkDownloadMovie);
        debugProperty(".scanner.imageName", artworkImageName);
        debugProperty(".scanner.searchForExistingArtwork", artworkSearchLocal);
        debugProperty(".scanner.artworkToken", artworkTokenOriginal);
        debugProperty(".format", artworkFormat);
        debugProperty(".scanner.Validate", artworkValidate);
        debugProperty(".scanner.ValidateMatch", artworkValidateMatch);
        debugProperty(".scanner.ValidateAspect", artworkValidateAspect);
        debugProperty(".scanner.artworkDirectory", artworkDirectory);
        debugProperty(".scanner.artworkPriority", artworkPriority);
        debugProperty(".movie.download", artworkDownloadMovie);
        debugProperty(".tv.download", artworkDownloadTv);
        debugProperty(".scanner.artworkExtensions", artworkExtensions);
        debugProperty(getPropertyName() + ".width", artworkWidth, Boolean.FALSE);
        debugProperty(getPropertyName() + ".height", artworkHeight, Boolean.FALSE);
    }
    //</editor-fold>

    /**
     * Return the name used in the properties file for this artwork type
     *
     * This is needed because of the disconnection between what was originally in the properties files and making it generic enough
     * for this scanner
     *
     * @return
     */
    protected final String getPropertyName() {
        return ArtworkScanner.getPropertyName(artworkType);
    }

    /**
     * Return the name used in the properties file for this artwork type
     *
     * This is needed because of the disconnection between what was originally in the properties files and making it generic enough
     * for this scanner
     *
     * @param artworkType
     * @return
     */
    public static String getPropertyName(ArtworkType artworkType) {
        if (artworkType == ArtworkType.POSTER) {
            return "posters";
        }

        if (artworkType == ArtworkType.FANART) {
            return "";
        }

        if (artworkType == ArtworkType.BANNER) {
            return "banners";
        }

        if (artworkType == ArtworkType.VIDEOIMAGE) {
            return "videoimages";
        }

        return artworkType.toString().toLowerCase();
    }

    /**
     * Return the filename to be used for the dummy artwork
     *
     * @return
     */
    protected final String getDummyFilename() {
        StringBuilder artworkDummy;
        if (artworkType == ArtworkType.POSTER) {
            artworkDummy = new StringBuilder("dummy.jpg");
        } else if (artworkType == ArtworkType.FANART) {
            // There is no dummy artwork for fanart
            artworkDummy = new StringBuilder("");
        } else {
            // guess a default dummy name
            artworkDummy = new StringBuilder("dummy_");
            artworkDummy.append(artworkTypeName);
            artworkDummy.append(".");
            artworkDummy.append(artworkFormat);
        }

        LOG.trace("Using dummy image name of '{}'", artworkDummy.toString());
        return artworkDummy.toString();
    }
}
