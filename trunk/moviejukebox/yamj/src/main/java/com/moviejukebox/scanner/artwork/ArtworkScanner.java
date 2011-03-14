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
package com.moviejukebox.scanner.artwork;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;
import com.moviejukebox.plugin.DefaultImagePlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

/**
 * Scanner for artwork.
 * @author Stuart.Boston
 * @version 1.0 12th September 2010
 *
 * These are the properties used:
 *      {artworkType}.scanner.searchForExistingArtwork    - true/false
 *      {artworkType}.scanner.artworkExtensions           - The extensions to search for the artwork
 *      {artworkType}.scanner.artworkToken                - A token that delimits the artwork, such as ".fanart" or ".banner"
 *      {artworkType}.scanner.imageName                   - List of fixed artwork names
 *      {artworkType}.scanner.Validate                    - 
 *      {artworkType}.scanner.ValidateMatch               - 
 *      {artworkType}.scanner.ValidateAspect              - 
 *      {artworkType}.scanner.artworkDirectory            - 
 *      {artworkType}.scanner.artworkPriority             - 
 *      mjb.skin.dir                                      - Skin directory
 *  From skin.properties
 *      ???.width
 *      ???.height
 */
public abstract class ArtworkScanner implements IArtworkScanner {
    public final static String BANNER     = "banner";
    public final static String FANART     = "fanart";
    public final static String POSTER     = "poster";
    public final static String VIDEOIMAGE = "videoimage";
    
    protected Collection<String>     artworkExtensions = new ArrayList<String>();
    protected String                 artworkFormat;         // Format of the artwork to save, e.g. JPG, PNG, etc.
    protected int                    artworkHeight;         // The height of the image from the skin.properties for use in the validation routine
    protected Collection<String>     artworkImageName = new ArrayList<String>();    // List of fixed artwork names
    protected MovieImagePlugin       artworkImagePlugin;
    protected boolean                artworkOverwrite = false;
    protected String                 artworkToken;          // The suffix of the artwork filename to search for.
    protected String                 artworkType;           // The type of the artwork. Will be used to load the other properties
    protected static boolean         artworkValidate;       // Should the artwork be validated or not.
    protected boolean                artworkValidateAspect; // Should the artwork be validated for it's aspect
    protected int                    artworkWidth;          // The width of the image from the skin.properties for use in the validation routine
    protected static Logger          logger = Logger.getLogger("moviejukebox");
    protected String                 logMessage;            // The start of the log message
    protected boolean                searchForExistingArtwork;  // Should we search for local artwork
    protected String                 skinHome;              // Location of the skin files used to get the dummy images from for missing artwork
    protected final WebBrowser       webBrowser = new WebBrowser();
    private   HashMap<String,String> ARTWORK_TYPES;         // First value is the property value, the second is the graphic image value
    private   String                 artworkDirectory;      // The name of the artwork directory to search from from either the video directory or the root of the library
    private   String                 artworkPriority;       // The order of the searches performed for the local artwork.
    protected static int             artworkValidateMatch;  // How close the image should be to the expected dimensions (artworkWidth & artworkHeight)
    protected static boolean         artworkMovieDownload;  // Whether or not to download artwork for a Movie 
    protected boolean                artworkTvDownload;     // Whether or not to download artwork for a TV Show

    public ArtworkScanner(String conArtworkType) {
        // TODO: change this to use the ArtworkType Enum
        // Define the allowable artwork types
        ARTWORK_TYPES = new HashMap<String, String>();
        ARTWORK_TYPES.put(POSTER,     "posters");
        ARTWORK_TYPES.put(FANART,     "");
        ARTWORK_TYPES.put(BANNER,     "banners");
        ARTWORK_TYPES.put(VIDEOIMAGE, "videoimages");

        setArtworkType(conArtworkType);
        // If we failed to set the artwork type, quit 
        if (artworkType == null) {
            logger.severe("ArtworkScanner: Failed to set artwork type to " + conArtworkType);
            return;
        }

        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty(conArtworkType + ".scanner.imageName", ""), ",;|");
        while (st.hasMoreTokens()) {
            artworkImageName.add(st.nextToken());
        }

        // We get artwork scanner behaviour
        try {
            searchForExistingArtwork = PropertiesUtil.getBooleanProperty(artworkType + ".scanner.searchForExistingArtwork", "true");
        } catch (Exception ignore) {
            searchForExistingArtwork = true;
        }
        
        setArtworkExtensions(PropertiesUtil.getProperty(artworkType + ".scanner.artworkExtensions", "jpg,png,gif"));
        artworkToken  = PropertiesUtil.getProperty(artworkType + ".scanner.artworkToken","");
        artworkFormat = PropertiesUtil.getProperty(artworkType + ".format", "jpg");
        setArtworkImageName(PropertiesUtil.getProperty(artworkType + ".scanner.imageName", ""));
        
        try {
            artworkValidate = PropertiesUtil.getBooleanProperty(artworkType + ".scanner.Validate", "true");
        } catch (Exception ignore) {
            artworkValidate = true;
        }
        
        try {
            artworkValidateMatch = PropertiesUtil.getIntProperty(artworkType + ".scanner.ValidateMatch", "75");
        } catch (Exception ignore) {
            artworkValidateMatch = 75;
        }
        
        try {
            artworkValidateAspect = PropertiesUtil.getBooleanProperty(artworkType + ".scanner.ValidateAspect", "true");
        } catch (Exception ignore) {
            artworkValidateAspect = true;
        }
        
        artworkDirectory = PropertiesUtil.getProperty(artworkType + ".scanner.artworkDirectory", "");
        
        // Get the priority (order) that the artwork is searched for
        artworkPriority = PropertiesUtil.getProperty(artworkType + ".scanner.artworkPriority", "video,folder,fixed,series,directory");

        // Get & set the default artwork dimensions
        setArtworkDimensions();

        // Set the image plugin
        setArtworkImagePlugin();
        
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        
        artworkMovieDownload = PropertiesUtil.getBooleanProperty(artworkType + ".movie.download", "true");
        artworkTvDownload = PropertiesUtil.getBooleanProperty(artworkType + ".tv.download" ,"true");
    }

    /**
     * Download the artwork to the jukebox
     * 
     * @return the status of the download. True if downloaded, false otherwise.
     */
    public boolean downloadArtwork(Jukebox jukebox, Movie movie, boolean artworkOverwrite, MovieImagePlugin imagePlugin) {
        
        String artworkUrl = getArtworkUrl(movie);
        String artworkFilename = getArtworkFilename(movie);
        
         if (!StringTools.isValidString(artworkUrl)) {
            logger.finest(logMessage + "Invalid " + artworkType + " URL - " + artworkUrl);
            return false;
        }
        
        if (!StringTools.isValidString(artworkFilename)) {
            logger.finest(logMessage + "Invalid " + artworkType + " filename - " + artworkFilename);
            return false;
        }
        
        String artworkPath = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename);
        if (!artworkOverwrite && FileTools.fileCache.fileExists(artworkPath)) {
            logger.finest(logMessage + "Artwork exists for " + movie.getBaseName());
            return true;
        }
        artworkPath = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename);
        
        File artworkFile = FileTools.fileCache.getFile(artworkPath);
        
        try {
            logger.finest(logMessage + "Downloading " + artworkType + " for " + movie.getBaseName() + " to " + artworkPath);

            // Download the artwork using the proxy save downloadImage
            FileTools.downloadImage(artworkFile, artworkUrl);
            BufferedImage artworkImage = GraphicTools.loadJPEGImage(artworkFile);
            
            String pluginType = ARTWORK_TYPES.get(artworkType);

            if ((artworkImage != null) || (pluginType != null)) {
                artworkImage = imagePlugin.generate(movie, artworkImage, pluginType, null);
                GraphicTools.saveImageToDisk(artworkImage, artworkPath);
            } else {
                setArtworkFilename(movie, Movie.UNKNOWN);
                setArtworkUrl(movie, Movie.UNKNOWN);
                return false;
            }
        } catch (Exception error) {
            logger.finer(logMessage + "Failed to download " + artworkType + ": " + artworkUrl);
            return false;
        }

        return true;
    }

    /**
     * Get the artwork filename from the movie object based on the artwork type
     * @param movie
     * @return the Artwork Filename
     */
    public abstract String getArtworkFilename(Movie movie);
    
    /**
     * Get the artwork URL from the movie object based on the artwork type
     * @param movie
     * @return the Artwork URL
     */
    public abstract String getArtworkUrl(Movie movie);

    public abstract boolean isDirtyArtwork(Movie movie);
    
    /**
     * A catch all routine to scan local artwork and then online artwork.
     */
    public String scan(Jukebox jukebox, Movie movie) {
        /*
         * We need to check some things before we start scanning
         *  1) If the artwork exists, do we need to overwrite it? (force overwrite?)
         *  2) Force overwrite should NOT check the jukebox for artwork. 
         */
        
        // If forceOverwrite is set, clear the Url so we will search again
        if (isOverwrite()) {
            logger.finest(logMessage + "forceOverwite set, clearing URL before search"); // XXX DEBUG
            setArtworkUrl(movie, Movie.UNKNOWN);
        }
        
        
        
        String artworkUrl = scanLocalArtwork(jukebox, movie);
        logger.fine(logMessage + "ScanLocalArtwork returned: " + artworkUrl); // XXX DEBUG
        
        if (StringTools.isValidString(artworkUrl)) {
            logger.fine(logMessage + "ArtworkUrl found, so CopyLocalFile triggered"); // XXX DEBUG
            copyLocalFile(jukebox, movie);
            return artworkUrl;
        }
        
        if (StringTools.isNotValidString(artworkUrl)) {
            logger.fine(logMessage + "ArtworkUrl NOT found"); // XXX DEBUG
            if (movie.isScrapeLibrary()) {
                logger.fine(logMessage + "Scanning for online artwork"); // XXX DEBUG
                artworkUrl = scanOnlineArtwork(movie);
                
                if (StringTools.isValidString(artworkUrl)) {
                    downloadArtwork(jukebox, movie, artworkOverwrite, artworkImagePlugin);
                }
            } else {
                logger.fine(logMessage + "Online scanning skipped due to scrapeLibrary=false");
            }
        }
        
        return artworkUrl;
    }
   
    /**
     * Scan for any local artwork and return the path to it. This should only be called by the scanLocalArtwork
     * method in the derived classes.
     * Note: This will update the movie information for this artwork
     * @param jukebox
     * @param movie
     * @return The URL (path) to the first found artwork in the priority list
     */
    public String scanLocalArtwork(Jukebox jukebox, Movie movie, MovieImagePlugin artworkImagePlugin) {
        String movieArtwork = Movie.UNKNOWN;
        String artworkFilename = movie.getBaseFilename() + artworkToken;
        
        StringTokenizer st = new StringTokenizer(artworkPriority, ",;|");
        
        while (st.hasMoreTokens() && movieArtwork.equalsIgnoreCase(Movie.UNKNOWN)) {
            String artworkSearch = st.nextToken().toLowerCase().trim();
            
            if (artworkSearch.equals("video")) {
                movieArtwork = scanVideoArtwork(movie, artworkFilename);
                //logger.fine(logMessage + movie.getBaseFilename() + " scanVideoArtwork    : " + movieArtwork);
                continue;
            }
            
            if (artworkSearch.equals("folder")) {
                movieArtwork = scanFolderArtwork(movie);
                //logger.fine(logMessage + movie.getBaseFilename() + " scanFolderArtwork   : " + movieArtwork);
                continue;
            }
            
            if (artworkSearch.equals("fixed")) {
                movieArtwork = scanFixedArtwork(movie);
                //logger.fine(logMessage + movie.getBaseFilename() + " scanFixedArtwork    : " + movieArtwork);
                continue;
            }
            
            if (artworkSearch.equals("series")) {
                // This is only for TV Sets as it searches the directory above the one the episode is in for fixed artwork
                if (movie.isTVShow() && movie.isSetMaster()) {
                    movieArtwork = scanTvSeriesArtwork(movie);
                    //logger.fine(logMessage + movie.getBaseFilename() + " scanTvSeriesArtwork : " + movieArtwork);
                }
                continue;
            }
            
            if (artworkSearch.equals("directory")) {
                movieArtwork = scanArtworkDirectory(movie, artworkFilename);
                //logger.fine(logMessage + movie.getBaseFilename() + " scanArtworkDirectory: " + movieArtwork);
                continue;
            }
            
        }
        
        if (StringTools.isValidString(movieArtwork)) {
            logger.finest(logMessage + "Found artwork for " + movie.getBaseName() + ": " + movieArtwork);
        } else {
            logger.finest(logMessage + "No local artwork found for " + movie.getBaseName());
        }
        setArtworkFilename(movie, artworkFilename + "." + artworkFormat);
        setArtworkUrl(movie, movieArtwork);
        
        return movieArtwork;
    }
    
    public abstract void setArtworkFilename(Movie movie, String artworkFilename);
    
    public abstract void setArtworkImagePlugin();
    
    public abstract void setArtworkUrl(Movie movie, String artworkUrl);
    
    /**
     * Updates the artwork by either copying the local file or downloading the artwork
     * @param jukebox
     * @param movie
     */
    public void updateArtwork(Jukebox jukebox, Movie movie) {
        String artworkFilename = getArtworkFilename(movie);
        String artworkUrl = getArtworkUrl(movie);
        String artworkDummy = "";
        
        if (artworkType.equals(POSTER)) {
            artworkDummy = "dummy.jpg";
        } else if (artworkType.equals(FANART)) {
            // There is no dummy artwork for fanart
            artworkDummy = "";
        } else if (artworkType.equals(BANNER)) {
            artworkDummy = "dummy_banner.jpg";
        } else if (artworkType.equals(VIDEOIMAGE)) {
            artworkDummy = "dummy_videoimage.jpg";
        }
        
        File artworkFile = FileTools.fileCache.getFile(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename));
        File tmpDestFile = new File(StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename));

        // Do not overwrite existing artwork, unless there is a new URL in the nfo file.
        if ((!tmpDestFile.exists() && !artworkFile.exists()) || isDirtyArtwork(movie) || isOverwrite()) {
            artworkFile.getParentFile().mkdirs();

            if (artworkUrl == null || artworkUrl.equals(Movie.UNKNOWN)) {
                logger.finest("Dummy " + artworkType + " used for " + movie.getBaseName());
                FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + artworkDummy), tmpDestFile);
            } else {
                if (StringTools.isValidString(artworkDummy)) {
                    try {
                        logger.finest("Downloading " + artworkType + " for " + movie.getBaseName() + " to " + tmpDestFile.getName());
                        FileTools.downloadImage(tmpDestFile, artworkUrl);
                    } catch (Exception error) {
                        logger.finer("Failed downloading " + artworkType + ": " + artworkUrl);
                        FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + artworkDummy), tmpDestFile);
                    }
                } else {
                    logger.finer(logMessage + "No dummy artwork for " + artworkType);
                }
            }
        } else {
            downloadArtwork(jukebox, movie, artworkOverwrite, artworkImagePlugin);
        }
    }
    
    public boolean validateArtwork(IImage artworkImage) {
        return validateArtwork(artworkImage, artworkWidth, artworkHeight, artworkValidateAspect);
    }

    /**
     * Get the size of the file at the end of the URL
     * Taken from: http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     * 
     * @param posterImage Artwork image to check
     * @param posterWidth The width to check
     * @param posterHeight The height to check
     * @param checkAspect Should the aspect ratio be checked
     * @return True if the poster is good, false otherwise
     */
    public boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect) {
        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader)readers.next();
        int urlWidth = 0, urlHeight = 0;
        float urlAspect;

        if (!artworkValidate) {
            return true;
        }

        if (artworkImage.getUrl().equalsIgnoreCase(Movie.UNKNOWN)) {
            return false;
        }

        try {
            URL url = new URL(artworkImage.getUrl());
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);
        } catch (IOException ignore) {
            logger.finest(logMessage + "ValidateArtwork error: " + ignore.getMessage() + ": can't open URL");
            return false; // Quit and return a false poster
        }

        urlAspect = (float)urlWidth / (float)urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            logger.finest(logMessage + artworkImage + " rejected: URL is wrong aspect (portrait/landscape)");
            return false;
        }

        // Adjust artwork width / height by the ValidateMatch figure
        artworkWidth = artworkWidth * (artworkValidateMatch / 100);
        artworkHeight = artworkHeight * (artworkValidateMatch / 100);

        if (urlWidth < artworkWidth) {
            logger.finest(logMessage + artworkImage + " rejected: URL width (" + urlWidth + ") is smaller than artwork width (" + artworkWidth + ")");
            return false;
        }

        if (urlHeight < artworkHeight) {
            logger.finest(logMessage + artworkImage + " rejected: URL height (" + urlHeight + ") is smaller than artwork height (" + artworkHeight + ")");
            return false;
        }
        return true;
    }

    /**
     * Copy the local artwork file to the jukebox. If there's no URL do not create dummy artwork
     * @param jukebox
     * @param movie
     * @return
     */
    protected String copyLocalFile(Jukebox jukebox, Movie movie) {
        String fullArtworkFilename = getArtworkUrl(movie);
        
        if (fullArtworkFilename.equalsIgnoreCase(Movie.UNKNOWN)) {
            logger.finer(logMessage + "No local " + artworkType + " found for " + movie.getBaseName());
            return Movie.UNKNOWN;
        } else {
            logger.finest(logMessage + fullArtworkFilename + " found");
            if (overwriteArtwork(jukebox, movie)) {
                String destFilename = getArtworkFilename(movie);
                String destFullPath = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), destFilename);
                
                FileTools.copyFile(fullArtworkFilename, destFullPath);
                logger.finer(logMessage + fullArtworkFilename + " has been copied to " + destFilename);
            } else {
                // Don't copy the file
                logger.fine(logMessage + fullArtworkFilename + " does not need to be copied");
            }

            return fullArtworkFilename;
        }
    }

    /**
     * returns the forceOverwite parameter for this artwork type.
     * This is set in the constructor of each of the sub-scanners
     * @return
     */
    protected boolean isOverwrite() {
        return artworkOverwrite;
    }
    
    /**
     * Determine if the artwork should be overwritten
     *   Checks to see if the artwork exists in the jukebox folders (temp and final)
     *   Checks the overwrite parameters
     *   Checks to see if the local artwork is newer
     * @param movie
     * @return
     */
    protected boolean overwriteArtwork(Jukebox jukebox, Movie movie) {
        if (isOverwrite()) {
            setDirtyArtwork(movie, true);
            logger.fine(logMessage + "Artwork overwrite"); // XXX DEBUG
            return true;
        }
        
        if (isDirtyArtwork(movie)) {
            logger.fine(logMessage + "Dirty artwork"); // XXX DEBUG
            return true;
        }
        
        // This is the filename & path of the artwork that was found
        String artworkFilename = getArtworkFilename(movie);
        File   artworkFile     = new File(artworkFilename);
        
        // This is the filename & path of the jukebox artwork
        String jukeboxFilename = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), artworkFilename);
        File jukeboxFile = FileTools.fileCache.getFile(jukeboxFilename);
        
        // This is the filename & path for temporary storage jukebox
        String tempLocFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), artworkFilename);
        File tempLocFile = new File(tempLocFilename);

        // Check to see if the found artwork newer than the temp jukebox
        if (FileTools.fileCache.fileExists(tempLocFile)) {
            if (FileTools.isNewer(artworkFile, tempLocFile)) {
                setDirtyArtwork(movie, true);
                logger.finest(logMessage + movie.getBaseName() + ": Local artwork is newer, overwritting existing artwork"); // XXX DEBUG
                return true;
            }
        } else if (FileTools.fileCache.fileExists(jukeboxFile)) {
            // Check to see if the found artwork newer than the existing jukebox
            if (FileTools.isNewer(artworkFile, jukeboxFile)) {
                setDirtyArtwork(movie, true);
                logger.finest(logMessage + movie.getBaseName() + ": Local artwork is newer, overwritting existing jukebox artwork"); // XXX DEBUG
                return true;
            } else {
                logger.fine(logMessage + "Local artwork is older, not copying"); // XXX DEBUG
                return false;
            }
        } else {
            logger.fine(logMessage + "No jukebox file found, file will be copied"); // XXX DEBUG
            return true;
        }
        
        // TODO: Any other checks that can be made for the artwork?

        // All the tests passed so don't overwrite
        return false;
    }

    /**
     * Scan an absolute or relative path for the movie images.
     * The relative path should include the directory of the movie as well as the library root
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanArtworkDirectory(Movie movie, String artworkFilename) {
        String artworkPath = Movie.UNKNOWN;

        if (!artworkDirectory.equals("")) {
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
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanFixedArtwork(Movie movie) {
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullFilename;
        File artworkFile = null;
        
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
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanFolderArtwork(Movie movie) {
        String parentPath = FileTools.getParentFolder(movie.getFile());
        String fullFilename = StringTools.appendToPath(parentPath, FileTools.getParentFolderName(movie.getFile()) + artworkToken);
            
        File artworkFile = FileTools.findFileFromExtensions(fullFilename, artworkExtensions);
        
        if (artworkFile.exists()) {
            return artworkFile.getAbsolutePath();
        }
        
        return Movie.UNKNOWN;
    }

    /**
     * Scan for TV show SERIES artwork. This usually exists in the directory ABOVE the one the files reside in
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
        File artworkFile = null;
        
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
     * Scan for artwork named like <videoFileName><artworkToken>.<artworkExtensions
     * @param movie
     * @return UNKNOWN or Absolute Path
     */
    protected String scanVideoArtwork(Movie movie, String artworkFilename) {
        return scanVideoArtwork(movie, artworkFilename, "");
    }
    
    /**
     * Scan for artwork named like <videoFileName><artworkToken>.<artworkExtensions>
     * @param movie
     * @param additionalPath A sub-directory of the movie to scan
     * @return UNKNOWN or Absolute Path
     */
    protected String scanVideoArtwork(Movie movie, String artworkFilename, String additionalPath) {
        if (additionalPath == null) {
            additionalPath = "";
        }
        
        String parentPath = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile()), additionalPath);
        return scanDirectoryForArtwork(artworkFilename, parentPath);
    }
    
    protected void setImagePlugin(String className) {
        try {
            Thread t = Thread.currentThread();
            ClassLoader cl = t.getContextClassLoader();
            Class<? extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
            artworkImagePlugin = pluginClass.newInstance();
        } catch (Exception error) {
            // Use the background plugin for fanart, the image plugin for all others
            if (artworkType.equals(FANART)) {
                artworkImagePlugin = new DefaultBackgroundPlugin();
            } else {
                artworkImagePlugin = new DefaultImagePlugin();
            }
            logger.severe("Failed instanciating imagePlugin: " + className);
            logger.severe("Default plugin will be used instead.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }

        return;
    }
    
    /**
     * Set the default artwork dimensions for use in the validate routine
     */
    private void setArtworkDimensions() {
        // This should return a value, but if it doesn't then we'll default to "posters"
        String dimensionType = ARTWORK_TYPES.get(artworkType);
        
        try {
            artworkWidth = PropertiesUtil.getIntProperty(dimensionType + ".width", "0");
            artworkHeight = PropertiesUtil.getIntProperty(dimensionType + ".height", "0");
        } catch (Exception ignore) {
            logger.severe(logMessage + "Number format error with " + dimensionType + ".width and/or " + dimensionType + ".height");
            logger.severe(logMessage + "Please ensure these are set correctly in your skin.properties file.");
            artworkWidth = 0;
            artworkHeight = 0;
        }
        
        if ((artworkWidth == 0) || (artworkHeight == 0)) {
            // There was an issue with the correct properties, so use poster as a default.
            try {
                artworkWidth = PropertiesUtil.getIntProperty("posters.width", "400");
                artworkHeight = PropertiesUtil.getIntProperty("posters.height", "600");
            } catch (Exception ignore) {
                // Just in case there is an issue with the poster settings too!
                artworkWidth = 400;
                artworkHeight = 600;
            }
        }
    }
    
    /**
     * Set the list of artwork extensions to search for using a delimited string
     * 
     * @param artworkExtensions
     */
    private void setArtworkExtensions(String extensions) {
        StringTokenizer st = new StringTokenizer(extensions, ",;|");
        while (st.hasMoreTokens()) {
            artworkExtensions.add(st.nextToken());
        }
    }

    /**
     * Set the list of folder artwork image names
     * 
     * @param artworkImageName
     */
    private void setArtworkImageName(String artworkImageNameString) {
        StringTokenizer st = new StringTokenizer(artworkImageNameString, ",;|");

        while (st.hasMoreTokens()) {
            artworkImageName.add(st.nextToken());
        }
    }
 
    /**
     * Set the name for the artwork type to be used in logger messages
     * 
     * @param artworkType The string name of the artwork type. Valid values: poster, fanart, banner, videoimage
     */
    private void setArtworkType(String setArtworkType) {
        if (!ARTWORK_TYPES.containsKey(setArtworkType)) {
            // This is an error with the calling function
            logger.severe("YAMJ Error with calling function in ArtworkScanner.java: Unknown Artwork Type (" + setArtworkType + ")");
            artworkType = null;
            return;
        }

        artworkType = setArtworkType;
        logMessage = "ArtworkScanner (" + setArtworkType + "): ";

        //logger.fine(logMessage + "Using " + setArtworkType + " as the Artwork Type");
   }

    /**
     * Check the scrapeLibrary and ID settings for the movie to see if we should scrape online sources
     * As soon as we hit a "false" then we should return.
     */
    public boolean getOnlineArtwork(Movie movie) {
        if (!movie.isScrapeLibrary()) {
            return false;
        }
        
        if (!movie.isTVShow() && !artworkMovieDownload) {
            return false;
        }
        
        if (movie.isTVShow() && !artworkTvDownload) {
            return false;
        }
        
        for (Entry<String, String> e : movie.getIdMap().entrySet()) {
            if ((e.getValue() == "0") || (e.getValue() == "-1") ) {
                // Stop and return
                return false;
            }
        }

        return true;
    }
}   