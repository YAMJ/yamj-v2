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
 *  VideoImage Scanner
 *
 * Routines for locating and downloading VideoImage for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static java.lang.Boolean.parseBoolean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Scanner for video image files in local directory
 * 
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 * 
 */
public class VideoImageScanner extends ArtworkScanner implements IArtworkScanner {
    
    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String skinHome;

    public VideoImageScanner() {
        super("videoimage");
        
        try {
            artworkOverwrite = parseBoolean(getProperty("mjb.forceVideoImagesOverwrite", "false"));
        } catch (Exception ignore) {
            artworkOverwrite = false;
        }

        
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    }

    /**
     * Try to locate a local video image and if that fails, download the image from the Internet.
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        // Check to see if this is a TV show.
        if (!movie.isTVShow()) {
            return Movie.UNKNOWN;
        }

        String genericVideoImageFilename = null;
        String localVideoImageBaseFilename = null;
        String fullVideoImageFilename = null;
        String videoimageExtension = null;
        String originalLocalVideoImageBaseFilename = null;
        String originalFullVideoImageFilename = null;
        File localVideoImageFile = null;
        boolean foundLocalVideoImage = false;
        boolean localOverwrite = false;
        int firstPart, lastPart;
        
        logger.finest("Checking for videoimages for " + movie.getTitle() + " [Season " + movie.getSeason() + "]");

        // Check for the generic video image for use in the loop later.
        localVideoImageBaseFilename = FileTools.getParentFolder(movie.getFile());
       
        localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1);
        genericVideoImageFilename = movie.getFile().getParent() + File.separator + localVideoImageBaseFilename + artworkToken;

        // Look for the various versions of the file with different image extensions
        videoimageExtension = findVideoImageFile(genericVideoImageFilename, artworkExtensions);

        if (videoimageExtension != null) {             // The filename and extension was found
            genericVideoImageFilename += videoimageExtension;
        } else {
            genericVideoImageFilename = null;
        }

        // Now we look through all the movie files to locate a local video image
        for (MovieFile mf : movie.getFiles()) {
            firstPart = mf.getFirstPart();  // The first part of the moviefile
            lastPart = mf.getLastPart();    // The last part of the moviefile
            
            // Check to see if the file is null, this might be the case if the file is in the middle of a series
            if (mf.getFile() == null) {
                logger.finest("VideoImage Scanner: Missing file - " + mf.getFilename());
                continue;
            }
            
            // Loop round each of the parts looking for the video_name.videoimageToken.Extension
            for (int part = firstPart; part <= lastPart; part++ ) {
                foundLocalVideoImage = false;   // Reset the "found" variable
                String partSuffix = "_" + (part - firstPart + 1);
                
                if (mf.getFile().isDirectory()) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator)+1) + artworkToken;
                } else if (mf.getFile().getParent().toString().contains("BDMV" + File.separator + "STREAM")) {
                    localVideoImageBaseFilename = mf.getFile().getPath().toString();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.indexOf("BDMV" + File.separator + "STREAM") - 1); 
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator)+1) + artworkToken;
                } else {
                    localVideoImageBaseFilename = mf.getFile().getName();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.lastIndexOf(".")) + artworkToken;
                }
                
                fullVideoImageFilename = FileTools.getParentFolder(mf.getFile()) + File.separator + localVideoImageBaseFilename;
                
                originalLocalVideoImageBaseFilename = localVideoImageBaseFilename;
                originalFullVideoImageFilename = fullVideoImageFilename;

                // Check for the videoimage filename with the suffix
                if (firstPart < lastPart) {
                    String suffixFullVideoImageFilename = originalFullVideoImageFilename + partSuffix;
                    
                    // Check for the videoimage filename with the "_part" suffix
                    videoimageExtension = findVideoImageFile(suffixFullVideoImageFilename, artworkExtensions);
                    
                    if (videoimageExtension != null) {
                        // The filename and extension was found
                        suffixFullVideoImageFilename += videoimageExtension;
                        
                        localVideoImageFile = new File(suffixFullVideoImageFilename); // Double check it still works
                        if (localVideoImageFile.exists()) {
                            logger.finest("VideoImageScanner: File " + suffixFullVideoImageFilename + " found");
                            foundLocalVideoImage = true;
                            
                            // Copy the found videoimage filenames to the originals to ensure the correct filenames are used
                            localVideoImageBaseFilename = originalLocalVideoImageBaseFilename + partSuffix + videoimageExtension;
                            fullVideoImageFilename = suffixFullVideoImageFilename;
                            mf.setVideoImageFilename(part, localVideoImageBaseFilename);
                        }
                    }
                }
                
                // If the wasn't a specific "videoimage_{part}" then look for a more generic videoimage filename
                if (!foundLocalVideoImage) {
                    // Check for the videoimage filename without the "_part" suffix
                    videoimageExtension = findVideoImageFile(fullVideoImageFilename, artworkExtensions);
                    
                    if (videoimageExtension != null) {
                        // The filename and extension was found
                        fullVideoImageFilename += videoimageExtension;
                        
                        localVideoImageFile = new File(fullVideoImageFilename); // Double check it still works
                        if (localVideoImageFile.exists()) {
                            logger.finest("VideoImageScanner: File " + fullVideoImageFilename + " found");
                            foundLocalVideoImage = true;
                            mf.setVideoImageFilename(part, localVideoImageBaseFilename + partSuffix + videoimageExtension);
                        }
                    }
                }

                // If we don't have a filename, then fill it in here.
                if (mf.getVideoImageFilename(part) == null || mf.getVideoImageFilename(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                    if (videoimageExtension == null) {
                        videoimageExtension = "." + artworkFormat;
                        
                        if (firstPart < lastPart) {
                            localVideoImageBaseFilename += partSuffix + videoimageExtension;
                        } else {
                            localVideoImageBaseFilename += videoimageExtension;
                        }
                    }
                    // This is the YAMJ generated filename.
                    mf.setVideoImageFilename(part, localVideoImageBaseFilename);
                    
                }
                
                // If we haven't found a local image, but a generic image exists, use that now.
                if (!foundLocalVideoImage && genericVideoImageFilename != null) {
                    logger.finest("VideoImageScanner: Generic Video Image used from " + genericVideoImageFilename);
                    // Copy the generic filename over.
                    fullVideoImageFilename = genericVideoImageFilename;
                    foundLocalVideoImage = true;
                }
                
                // If we've found the VideoImage, copy it to the jukebox, otherwise download it.
                if (foundLocalVideoImage) {
                    // Check to see if the URL is UNKNOWN and the local file is found, in which case 
                    // the videoimage in the jukebox should be overwritten with this file
                    localOverwrite = false; // first reset the check variable
                    if (mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                        localOverwrite = true;  // Means the file will be overwritten regardless of any other checks
                    }
                    
                    // Derive the URL to the local file
                    if (mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                        // This occurs when there isn't a videoimage URL in the XML
                        if (localVideoImageFile != null)  {
                            mf.setVideoImageURL(part, localVideoImageFile.toURI().toString());
                            mf.setVideoImageFilename(part, localVideoImageFile.getName().toString());
                        }
                        else {
                            mf.setVideoImageURL(part, new File(genericVideoImageFilename).toURI().toString());
                            mf.setVideoImageFilename(part, genericVideoImageFilename);
                        }
                    }
                    String videoimageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
                    String finalDestinationFileName = jukebox.getJukeboxRootLocationDetails() + File.separator + videoimageFilename;
                    String tmpDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + videoimageFilename;
                    
                    File tmpDestFile = new File(tmpDestFilename);
                    File finalDestinationFile = new File(finalDestinationFileName);
                    File fullVideoImageFile = new File(fullVideoImageFilename);
                    
                    // Local VideoImage is newer OR ForceVideoImageOverwrite OR DirtyVideoImage
                    // Can't check the file size because the jukebox videoimage may have been re-sized
                    // This may mean that the local art is different to the jukebox art even if the local file date is newer
                    if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile) || artworkOverwrite || localOverwrite || movie.isDirty()) {
                        if (processImage(artworkImagePlugin, movie, fullVideoImageFilename, tmpDestFilename)) {
                            logger.finer("VideoImageScanner: " + fullVideoImageFile.getName() + " has been copied to " + tmpDestFilename);
                        } else {
                            // Processing failed, so try backup image
                            logger.finer("VideoImageScanner: Failed loading videoimage : " + fullVideoImageFilename);
                            
                            // Copy the dummy videoimage to the temp folder
                            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);
                            
                            // Process the dummy videoimage in the temp folder
                            if (processImage(artworkImagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                                logger.finest("VideoImage Scanner: Using default videoimage");
                                mf.setVideoImageURL(part, Movie.UNKNOWN);   // So we know this is a dummy videoimage
                            } else {
                                logger.finer("VideoImageScanner: Failed loading default videoimage");
                                // Copying the default image failed, so leave everything blank
                                mf.setVideoImageFilename(part, Movie.UNKNOWN);
                                mf.setVideoImageURL(part, Movie.UNKNOWN);
                            }
                        }
                    } else {
                        logger.finer("VideoImageScanner: " + finalDestinationFileName + " already exists");
                    }
                } else {
                    // logger.finer("VideoImageScanner : No local VideoImage found for " + movie.getBaseName() + " attempting to download");
                    downloadVideoImage(artworkImagePlugin, jukebox, movie, mf, part);
                }
            }
        }
        
        return getArtworkUrl(movie);
    }

    /**
     * Try to load, process and save the image file
     * @param imagePlugin       The image plugin to use
     * @param imageFilename     The filename of the image to process
     * @param movie             The movie that the image relates to
     * @return                  True if the image was successfully processed, false otherwise
     */
    private static boolean processImage(MovieImagePlugin imagePlugin, Movie movie, String imageFilename, String targetFilename) {
        boolean imageOK = true;
        
        try {
            BufferedImage videoimageImage = GraphicTools.loadJPEGImage(imageFilename);

            if (videoimageImage != null) {
                videoimageImage = imagePlugin.generate(movie, videoimageImage, "videoimages", null);
                GraphicTools.saveImageToDisk(videoimageImage, targetFilename);
            } else {
                imageOK = false;
            }
        } catch (Exception error) {
            imageOK = false;
        }
        return imageOK;
    }
    
    /**
     * Download the videoimage from the URL. Initially this is populated from TheTVDB plugin
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    private void downloadVideoImage(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie, MovieFile mf, int part) {

        if (mf.getVideoImageURL(part) != null && !mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
            String safeVideoImageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
            String videoimageFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeVideoImageFilename;
            File videoimageFile = FileTools.fileCache.getFile(videoimageFilename);
            String tmpDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + safeVideoImageFilename;
            File tmpDestFile = new File(tmpDestFilename);
            boolean fileOK = true;
            
            // Do not overwrite existing videoimage unless ForceVideoImageOverwrite = true
            if ((!videoimageFile.exists() && !tmpDestFile.exists()) || artworkOverwrite) {
                videoimageFile.getParentFile().mkdirs();

                // Download the videoimage using the proxy save downloadImage
                try {
                    FileTools.downloadImage(tmpDestFile, mf.getVideoImageURL(part));
                } catch (Exception error) {
                    logger.finer("VideoImage Scanner: Failed to download videoimage : " + mf.getVideoImageURL(part) + " error: " + error.getMessage());
                    fileOK = false;
                }
                    
                if (fileOK && processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                    logger.finest("VideoImage Scanner: Downloaded videoimage for " + mf.getVideoImageFilename(part) + " to " + tmpDestFilename);
                } else {
                    // failed use dummy
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                    if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                        logger.finer("VideoImage Scanner: Using default videoimage");
                        mf.setVideoImageURL(part, Movie.UNKNOWN); // So we know this is a dummy videoimage
                    } else {
                        // Copying the default image failed, so leave everything blank
                        logger.finer("VideoImageScanner: Failed loading default videoimage");
                        mf.setVideoImageFilename(part, Movie.UNKNOWN);
                        mf.setVideoImageURL(part, Movie.UNKNOWN);
                    }
                }
            } else {
                logger.finest("VideoImage Scanner: VideoImage exists for " + mf.getVideoImageFilename(part));
            }
        }
        return;
    }
    
    /***
     * Pass in the filename and a list of extensions, this function will scan for the filename plus extensions and return the extension
     * 
     * @param filename
     * @param extensions
     * @return extension of videoimage that was found
     */
    private static String findVideoImageFile(String fullVideoImageFilename, Collection<String> artworkExtensions) {
        File localVideoImageFile;
        String videoimageExtension = null;
        boolean foundLocalVideoImage = false;

        for (String extension : artworkExtensions) {
            localVideoImageFile = new File(fullVideoImageFilename + "." + extension);
            if (localVideoImageFile.exists()) {
                //logger.finest("The file " + fullVideoImageFilename + "." + extension + " found");
                videoimageExtension = "." + extension;
                foundLocalVideoImage = true;
                break;
            }
        }

        // If we've found the filename with extension, return it, otherwise return null
        if (foundLocalVideoImage) {
            return videoimageExtension;
        } else {
            return null;
        }
    }

    @Override
    public String getArtworkFilename(Movie movie) {
        logger.severe(logMessage + this.getClass() + " NOT USED!!!");
        return Movie.UNKNOWN;
    }

    @Override
    public String getArtworkUrl(Movie movie) {
        logger.severe(logMessage + this.getClass() + " NOT USED!!!");
        return Movie.UNKNOWN;
    }

    @Override
    public void setArtworkFilename(Movie movie, String artworkFilename) {
        logger.severe(logMessage + this.getClass() + " NOT USED!!!");
    }

    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        logger.severe(logMessage + this.getClass() + " NOT USED!!!");
    }

    @Override
    public String scanOnlineArtwork(Movie movie) {
        logger.severe(logMessage + this.getClass() + " NOT USED!!!");
        return Movie.UNKNOWN;
    }

    @Override
    public void setArtworkImagePlugin() {
        setImagePlugin(PropertiesUtil.getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
    }

    
    @Override
    public boolean isDirtyArtwork(Movie movie) {
        return false;
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        // TODO Auto-generated method stub
        
    }

}