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

/**
 *  VideoImage Scanner
 *
 * Routines for locating and downloading VideoImage for videos
 *
 */
package com.moviejukebox.scanner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;

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
public class VideoImageScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String skinHome;
    protected static String[] videoimageExtensions;
    protected static String videoimageToken;
    protected static boolean videoimageOverwrite;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("videoimage.scanner.videoimageExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        videoimageExtensions = extensions.toArray(new String[] {});
        videoimageToken = PropertiesUtil.getProperty("videoimage.scanner.videoimageToken", ".videoimage");
        videoimageOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceVideoImagesOverwrite", "false"));
    }

    /**
     * Try to locate a local video image and if that fails, download the image from the Internet.
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    public static void scan(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        // Check to see if this is a TV show.
        if (!movie.isTVShow())
            return;

        String genericVideoImageFilename = null;
        String localVideoImageBaseFilename = null;
        String fullVideoImageFilename = null;
        String videoimageExtension = null;
        File localVideoImageFile = null;
        boolean foundLocalVideoImage = false;
        boolean localOverwrite = false;
        int part;
        
        logger.finest("Checking for videoimages for " + movie.getTitle() + " [Season " + movie.getSeason() + "]");

        // Check for the generic video image for use in the loop later.
        if (movie.getFile().isDirectory()) { // for VIDEO_TS
            localVideoImageBaseFilename = movie.getFile().getPath();
        } else {
            localVideoImageBaseFilename = movie.getFile().getParent();
        }
        localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1);
        genericVideoImageFilename = movie.getFile().getParent() + File.separator + localVideoImageBaseFilename + videoimageToken;

        // Look for the various versions of the file with different image extensions
        videoimageExtension = findVideoImageFile(genericVideoImageFilename, videoimageExtensions);

        if (videoimageExtension != null) {             // The filename and extension was found
            genericVideoImageFilename += videoimageExtension;
        } else {
            genericVideoImageFilename = null;
        }

        // Now we look through all the movie files to locate a local video image
        for (MovieFile mf : movie.getFiles()) {
            foundLocalVideoImage = false;   // Reset the "found" variable
            part = mf.getFirstPart();       // Look for the video_name.videoimageToken.Extension
            
            if (mf.getFile().isDirectory()) {
                localVideoImageBaseFilename = mf.getFile().getPath();
                localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator)+1) + videoimageToken;
            } else {
                localVideoImageBaseFilename = mf.getFile().getName();
                localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.lastIndexOf(".")) + videoimageToken;                
            }
            
            if (mf.getFile().isDirectory()) { // for VIDEO_TS
                fullVideoImageFilename = mf.getFile().getPath();
            } else {
                fullVideoImageFilename = mf.getFile().getParent();
            }

            fullVideoImageFilename += File.separator + localVideoImageBaseFilename;
            videoimageExtension = findVideoImageFile(fullVideoImageFilename, videoimageExtensions);

            if (videoimageExtension != null) {
                // The filename and extension was found
                fullVideoImageFilename += videoimageExtension;
                
                localVideoImageFile = new File(fullVideoImageFilename); // Double check it still works
                if (localVideoImageFile.exists()) {
                    logger.finest("VideoImageScanner: File " + fullVideoImageFilename + " found");
                    foundLocalVideoImage = true;
                }
            }
            
            // If we don't have a filename, then fill it in here.
            if (mf.getVideoImageFilename(part) == null || mf.getVideoImageFilename(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                if (videoimageExtension == null) {
                    videoimageExtension = "." + PropertiesUtil.getProperty("videoimages.format", "jpg");
                }
                // This is the YAMJ generated filename.
                mf.setVideoImageFilename(part, localVideoImageBaseFilename + videoimageExtension);
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
                    if (localVideoImageFile != null)
                        mf.setVideoImageURL(part, localVideoImageFile.toURI().toString());
                    else
                        mf.setVideoImageURL(part, new File(genericVideoImageFilename).toURI().toString());
                }
                String videoimageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
                String finalDestinationFileName = jukeboxDetailsRoot + File.separator + videoimageFilename;
                String tmpDestFilename = tempJukeboxDetailsRoot + File.separator + videoimageFilename;

                File tmpDestFile = new File(tmpDestFilename);
                File finalDestinationFile = new File(finalDestinationFileName);
                File fullVideoImageFile = new File(fullVideoImageFilename);

                // Local VideoImage is newer OR ForceVideoImageOverwrite OR DirtyVideoImage
                // Can't check the file size because the jukebox videoimage may have been re-sized
                // This may mean that the local art is different to the jukebox art even if the local file date is newer
                if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile) || videoimageOverwrite || localOverwrite || movie.isDirty()) {
                    if (processImage(imagePlugin, movie, fullVideoImageFilename, tmpDestFilename)) {
                        logger.finer("VideoImageScanner: " + fullVideoImageFile.getName() + " has been copied to " + tmpDestFilename);
                    } else {
                        // Processing failed, so try backup image
                        logger.finer("VideoImageScanner: Failed loading videoimage : " + fullVideoImageFilename);
                        
                        // Copy the dummy videoimage to the temp folder
                        FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);
                        
                        // Process the dummy videoimage in the temp folder
                        if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename)) {
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
                downloadVideoImage(imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, mf);
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
            BufferedImage videoimageImage = GraphicTools.loadJPEGImage(new FileInputStream(new File(imageFilename)));

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
    private static void downloadVideoImage(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie, MovieFile mf) {
        int part = mf.getFirstPart();

        if (mf.getVideoImageURL(part) != null && !mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
            String safeVideoImageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
            String videoimageFilename = jukeboxDetailsRoot + File.separator + safeVideoImageFilename;
            File videoimageFile = new File(videoimageFilename);
            String tmpDestFilename = tempJukeboxDetailsRoot + File.separator + safeVideoImageFilename;
            File tmpDestFile = new File(tmpDestFilename);
            boolean fileOK = true;

            // Do not overwrite existing videoimage unless ForceVideoImageOverwrite = true
            if ((!videoimageFile.exists() && !tmpDestFile.exists()) || videoimageOverwrite) {
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
    private static String findVideoImageFile(String fullVideoImageFilename, String[] videoimageExtensions) {
        File localVideoImageFile;
        String videoimageExtension = null;
        boolean foundLocalVideoImage = false;

        for (String extension : videoimageExtensions) {
            localVideoImageFile = new File(fullVideoImageFilename + "." + extension);
            if (localVideoImageFile.exists()) {
                // logger.finest("The file " + fullVideoImageFilename + "." + extension + " found");
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

    @Deprecated
    public static void scanold(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        // Check to see if this is a TV show.
        if (!movie.isTVShow())
            return;

        String localVideoImageBaseFilename = null; // The root of the file we are looking for
        String fullVideoImageFilename = null; // The local file plus path
        String genericVideoImageFilename = null; // This is the folder video image, if it exists
        String videoimageExtension = null; // The extension of the video image (if found)
        File localVideoImageFile = null; // 
        boolean foundLocalVideoImage = false; // 
        int part;

        logger.fine("Checking for videoimages for " + movie.getTitle() + " [Season " + movie.getSeason() + "]");

        // Check for the generic video image for use in the loop later.
        if (movie.getFile().isDirectory()) {
            // for VIDEO_TS
            localVideoImageBaseFilename = movie.getFile().getPath();
        } else {
            localVideoImageBaseFilename = movie.getFile().getParent();
        }
        localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1);
        genericVideoImageFilename = movie.getFile().getParent() + File.separator + localVideoImageBaseFilename + videoimageToken;

        // Look for the various versions of the file with different image extensions
        videoimageExtension = findVideoImageFile(genericVideoImageFilename, videoimageExtensions);
        if (videoimageExtension != null) {
            // The filename and extension was found
            genericVideoImageFilename += videoimageExtension;
        } else {
            genericVideoImageFilename = null;
        }

        // Now we've located (or not) the generic videoimage filename, we need to process
        // all of the movie files to see if we can find local videoimages

        for (MovieFile mf : movie.getFiles()) {
            foundLocalVideoImage = false; // Reset the "found" variable
            
            part = mf.getFirstPart();

            localVideoImageBaseFilename = mf.getFile().getName();
            // logger.finest("Looking for videoimages for " + localVideoImageBaseFilename);

            localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.lastIndexOf(".")) + videoimageToken;
            // Strip the extension off the filename
            fullVideoImageFilename = mf.getFile().getParent() + File.separator + localVideoImageBaseFilename; // build the path to the videoimage file

            // If we don't have a filename, then fill it in here.
            if (mf.getVideoImageFilename(part) == null || mf.getVideoImageFilename(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                mf.setVideoImageFilename(part, localVideoImageBaseFilename + "." + PropertiesUtil.getProperty("videoimages.format", "jpg"));
            }

            videoimageExtension = findVideoImageFile(fullVideoImageFilename, videoimageExtensions);
            if (videoimageExtension != null) {
                // The filename and extension was found
                localVideoImageBaseFilename += videoimageExtension;
                fullVideoImageFilename += videoimageExtension;
                localVideoImageFile = new File(fullVideoImageFilename);
                logger.finest("VideoImageScanner: File " + fullVideoImageFilename + " found");
                foundLocalVideoImage = true;
            }

            // If we've found the videoimage, copy it to the jukebox, otherwise download it.
            if (foundLocalVideoImage) {
                if (mf.getVideoImageFilename(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                    mf.setVideoImageFilename(part, fullVideoImageFilename);
                }

                // If the URL is UNKNOWN, then set it to the local filename
                if (mf.getVideoImageURL(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                    mf.setVideoImageURL(part, localVideoImageFile.toURI().toString());
                }
                
                String videoimageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
                String finalDestinationFileName = jukeboxDetailsRoot + File.separator + videoimageFilename;
                String destFileName = tempJukeboxDetailsRoot + File.separator + videoimageFilename;

                File finalDestinationFile = new File(finalDestinationFileName);
                File fullVideoImageFile = new File(fullVideoImageFilename);

                // Local VideoImage is newer OR ForcePosterOverwrite OR Movie isDirty
                // Can't check the file size because the jukebox videoimage may have been re-sized
                // This may mean that the local art is different to the jukebox art even if the local file date is newer
                if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile) || videoimageOverwrite || movie.isDirty()) {
                    try {
                        BufferedImage videoimageImage = GraphicTools.loadJPEGImage(new FileInputStream(fullVideoImageFile));
                        if (videoimageImage != null) {
                            videoimageImage = imagePlugin.generate(movie, videoimageImage, "videoimages", null);
                            GraphicTools.saveImageToDisk(videoimageImage, destFileName);
                            logger.finer("VideoImageScanner: " + fullVideoImageFilename + " has been copied to " + destFileName);
                        } else {
                            mf.setVideoImageFilename(part, Movie.UNKNOWN);
                            mf.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    } catch (Exception error) {
                        logger.finer("VideoImageScanner: Failed loading videoimage : " + fullVideoImageFilename);
                    }
                } else {
                    logger.finer("VideoImageScanner: " + finalDestinationFileName + " already exists");
                }
            } else {
                //logger.finer("VideoImageScanner : No local VideoImage found for " + movie.getBaseName() + " attempting to download");
                downloadVideoImage(imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie, mf);
            }
        }
    }
}