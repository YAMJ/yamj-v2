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
 *  VideoImage Scanner
 *
 * Routines for locating and downloading VideoImage for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Scanner for video image files in local directory
 * 
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 * 
 */
public class VideoImageScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String skinHome;
    protected static Collection<String> videoimageExtensions = new ArrayList<String>();
    protected static String videoimageToken;
    protected static boolean videoimageOverwrite;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("videoimage.scanner.videoimageExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            videoimageExtensions.add(st.nextToken());
        }
        
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        videoimageToken = PropertiesUtil.getProperty("videoimage.scanner.videoimageToken", ".videoimage");
        videoimageOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceVideoImagesOverwrite", "false");
    }

    /**
     * Try to locate a local video image and if that fails, download the image from the Internet.
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    public static void scan(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie) {
        // Check to see if this is a TV show.
        if (!movie.isTVShow()) {
            return;
        }

        String genericVideoImageFilename = null;
        File   genericVideoImageFile = null;
        String localVideoImageBaseFilename = null;
        File   localVideoImageFile = null;
        String fullVideoImageFilename = null;
        String videoimageExtension = null;
        String originalFullVideoImageFilename = null;
        boolean foundLocalVideoImage = false;
        boolean localOverwrite = false;
        int firstPart, lastPart;
        
        logger.debug("VideoImageScanner: Checking for videoimages for " + movie.getTitle() + " [Season " + movie.getSeason() + "]");

        // Check for the generic video image for use in the loop later.
        localVideoImageBaseFilename = FileTools.getParentFolder(movie.getFile());

        localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1));
        genericVideoImageFilename = movie.getFile().getParent() + File.separator + localVideoImageBaseFilename + videoimageToken;

        // Look for the various versions of the file with different image extensions
        genericVideoImageFile = FileTools.findFileFromExtensions(genericVideoImageFilename, videoimageExtensions);
        if (genericVideoImageFile.exists()) {
            genericVideoImageFilename = genericVideoImageFile.getAbsolutePath();
        } else {
            genericVideoImageFilename = null;
        }
        
        // Now we look through all the movie files to locate a local video image
        for (MovieFile mf : movie.getFiles()) {
            firstPart = mf.getFirstPart();  // The first part of the moviefile
            lastPart = mf.getLastPart();    // The last part of the moviefile
            
            // Check to see if the file is null, this might be the case if the file is in the middle of a series
            if (mf.getFile() == null) {
                logger.debug("VideoImageScanner: Missing file - " + mf.getFilename());
                continue;
            }
            
            // Loop round each of the parts looking for the video_name.videoimageToken.Extension
            for (int part = firstPart; part <= lastPart; part++ ) {
                foundLocalVideoImage = false;   // Reset the "found" variable
                String partSuffix = "_" + (part - firstPart + 1);
                
                if (mf.getFile().isDirectory()) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator)+1)) + videoimageToken;
                } else if (mf.getFile().getParent().toString().contains("BDMV" + File.separator + "STREAM")) {
                    localVideoImageBaseFilename = mf.getFile().getPath().toString();
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.indexOf("BDMV" + File.separator + "STREAM") - 1)); 
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator)+1)) + videoimageToken;
                } else {
                    localVideoImageBaseFilename = mf.getFile().getName();
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.lastIndexOf("."))) + videoimageToken;
                }

                fullVideoImageFilename = FileTools.getParentFolder(mf.getFile()) + File.separator + localVideoImageBaseFilename;
                
                originalFullVideoImageFilename = fullVideoImageFilename;

                // Check for the videoimage filename with the suffix
                if (firstPart < lastPart) {
                    String suffixFullVideoImageFilename = originalFullVideoImageFilename + partSuffix;
                    
                    // Check for the videoimage filename with the "_part" suffix
                    localVideoImageFile = FileTools.findFileFromExtensions(suffixFullVideoImageFilename, videoimageExtensions);
                    if (localVideoImageFile.exists()) {
                        logger.debug("VideoImageScanner: File " + localVideoImageFile.getName() + " found");
                        foundLocalVideoImage = true;
                        
                        // Copy the found videoimage filenames to the originals to ensure the correct filenames are used
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                    }
                }
                
                // If the wasn't a specific "videoimage_{part}" then look for a more generic videoimage filename
                if (!foundLocalVideoImage) {
                    // Check for the videoimage filename without the "_part" suffix
                    localVideoImageFile = FileTools.findFileFromExtensions(fullVideoImageFilename, videoimageExtensions);
                    if (localVideoImageFile.exists()) {
                        logger.debug("VideoImageScanner: File " + localVideoImageFile.getName() + " found");
                        foundLocalVideoImage = true;
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                    }
                }

                // If we don't have a filename, then fill it in here.
                if (StringTools.isNotValidString(mf.getVideoImageFilename(part))) {
                    logger.debug("VideoImageScanner: No valid filename was found for part " + part);
                    
                    if (videoimageExtension == null) {
                        videoimageExtension = "." + PropertiesUtil.getProperty("videoimages.format", "jpg");
                    }
                    if (firstPart < lastPart) {
                        localVideoImageBaseFilename += partSuffix + videoimageExtension;
                    } else {
                        localVideoImageBaseFilename += videoimageExtension;
                    }
                    
                    // This is the YAMJ generated filename.
                    mf.setVideoImageFilename(part, localVideoImageBaseFilename);
                    
                }
                
                // If we haven't found a local image, but a generic image exists, use that now.
                if (!foundLocalVideoImage && genericVideoImageFilename != null) {
                    logger.debug("VideoImageScanner: Generic Video Image used from " + genericVideoImageFilename);
                    // Copy the generic filename over.
                    fullVideoImageFilename = genericVideoImageFilename;
                    foundLocalVideoImage = true;
                }
                
                // If we've found the VideoImage, copy it to the jukebox, otherwise download it.
                if (foundLocalVideoImage) {
                    // Check to see if the URL is UNKNOWN and the local file is found, in which case 
                    // the videoimage in the jukebox should be overwritten with this file
                    localOverwrite = false; // first reset the check variable
                    if (StringTools.isNotValidString(mf.getVideoImageURL(part))) {
                        localOverwrite = true;  // Means the file will be overwritten regardless of any other checks
                    }
                    
                    // Derive the URL to the local file
                    if (StringTools.isNotValidString(mf.getVideoImageURL(part))) {
                        // This occurs when there isn't a videoimage URL in the XML
                        if (localVideoImageFile != null)  {
                            mf.setVideoImageURL(part, localVideoImageFile.toURI().toString());
                            mf.setVideoImageFilename(part, localVideoImageFile.getName());
                        } else {
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
                    if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile) || videoimageOverwrite || localOverwrite || movie.isDirty()) {
                        if (processImage(imagePlugin, movie, fullVideoImageFilename, tmpDestFilename)) {
                            logger.debug("VideoImageScanner: " + fullVideoImageFile.getName() + " has been copied to " + tmpDestFilename);
                        } else {
                            // Processing failed, so try backup image
                            logger.debug("VideoImageScanner: Failed loading videoimage : " + fullVideoImageFilename);
                            
                            // Copy the dummy videoimage to the temp folder
                            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);
                            
                            // Process the dummy videoimage in the temp folder
                            if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                                logger.debug("VideoImageScanner: Using default videoimage");
                                mf.setVideoImageURL(part, Movie.UNKNOWN);   // So we know this is a dummy videoimage
                            } else {
                                logger.debug("VideoImageScanner: Failed loading default videoimage");
                                // Copying the default image failed, so leave everything blank
                                mf.setVideoImageFilename(part, Movie.UNKNOWN);
                                mf.setVideoImageURL(part, Movie.UNKNOWN);
                            }
                        }
                    } else {
                        logger.debug("VideoImageScanner: " + finalDestinationFileName + " already exists");
                    }
                } else {
                    // logger.debug("VideoImageScanner : No local VideoImage found for " + movie.getBaseName() + " attempting to download");
                    downloadVideoImage(imagePlugin, jukebox, movie, mf, part);
                }
            }
        }
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
    private static void downloadVideoImage(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie, MovieFile mf, int part) {

        if (StringTools.isValidString(mf.getVideoImageURL(part))) {
            String safeVideoImageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
            String videoimageFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeVideoImageFilename;
            File videoimageFile = FileTools.fileCache.getFile(videoimageFilename);
            String tmpDestFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + safeVideoImageFilename;
            File tmpDestFile = new File(tmpDestFilename);
            boolean fileOK = true;
            
            // Do not overwrite existing videoimage unless ForceVideoImageOverwrite = true
            if ((!videoimageFile.exists() && !tmpDestFile.exists()) || videoimageOverwrite) {
                videoimageFile.getParentFile().mkdirs();

                // Download the videoimage using the proxy save downloadImage
                try {
                    FileTools.downloadImage(tmpDestFile, mf.getVideoImageURL(part));
                } catch (Exception error) {
                    logger.debug("VideoImageScanner: Failed to download videoimage : " + mf.getVideoImageURL(part) + " error: " + error.getMessage());
                    fileOK = false;
                }
                    
                if (fileOK && processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                    logger.debug("VideoImageScanner: Downloaded videoimage for " + mf.getVideoImageFilename(part) + " to " + tmpDestFilename);
                } else {
                    // failed use dummy
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                    if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
                        logger.debug("VideoImageScanner: Using default videoimage");
                        mf.setVideoImageURL(part, Movie.UNKNOWN); // So we know this is a dummy videoimage
                    } else {
                        // Copying the default image failed, so leave everything blank
                        logger.debug("VideoImageScanner: Failed loading default videoimage");
                        mf.setVideoImageFilename(part, Movie.UNKNOWN);
                        mf.setVideoImageURL(part, Movie.UNKNOWN);
                    }
                }
            } else {
                logger.debug("VideoImageScanner: VideoImage exists for " + mf.getVideoImageFilename(part));
            }
        }
        return;
    }
}