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
/**
 * VideoImage Scanner
 *
 * Routines for locating and downloading VideoImage for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Scanner for video image files in local directory
 *
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 *
 */
public class VideoImageScanner {

    private static final Logger logger = Logger.getLogger(VideoImageScanner.class);
    private static final String LOG_MESSAGE = "VideoImageScanner: ";
    private static String skinHome;
    private static final Collection<String> videoimageExtensions = new ArrayList<String>();
    private static String videoimageToken;
    private static boolean videoimageOverwrite;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("videoimage.scanner.videoimageExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            videoimageExtensions.add(st.nextToken());
        }

        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        videoimageToken = PropertiesUtil.getProperty("videoimage.scanner.videoimageToken", ".videoimage");
        videoimageOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceVideoImagesOverwrite", FALSE);
    }

    /**
     * Try to locate a local video image and if that fails, download the image
     * from the Internet.
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

        String genericVideoImageFilename;
        File genericVideoImageFile;
        String localVideoImageBaseFilename;
        File localVideoImageFile = null;
        String fullVideoImageFilename;
        String videoimageExtension = null;
        String originalFullVideoImageFilename;
        boolean foundLocalVideoImage;
        boolean localOverwrite;
        int firstPart, lastPart;

        logger.debug(LOG_MESSAGE + "Checking for videoimages for " + movie.getTitle() + " [Season " + movie.getSeason() + "]");

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
                logger.debug(LOG_MESSAGE + "Missing file - " + mf.getFilename());
                continue;
            }

            // Loop round each of the parts looking for the video_name.videoimageToken.Extension
            for (int part = firstPart; part <= lastPart; part++) {
                foundLocalVideoImage = false;   // Reset the "found" variable
                String partSuffix = "_" + (part - firstPart + 1);

                if (mf.getFile().isDirectory()) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1)) + videoimageToken;
                } else if (mf.getFile().getParent().contains("BDMV" + File.separator + "STREAM")) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.indexOf("BDMV" + File.separator + "STREAM") - 1));
                    localVideoImageBaseFilename = new String(localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1)) + videoimageToken;
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
                        logger.debug(LOG_MESSAGE + "File " + localVideoImageFile.getName() + " found");
                        foundLocalVideoImage = true;
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                    }
                }

                // If the wasn't a specific "videoimage_{part}" then look for a more generic videoimage filename
                if (!foundLocalVideoImage) {
                    // Check for the videoimage filename without the "_part" suffix
                    localVideoImageFile = FileTools.findFileFromExtensions(fullVideoImageFilename, videoimageExtensions);
                    if (localVideoImageFile.exists()) {
                        logger.debug(LOG_MESSAGE + "File " + localVideoImageFile.getName() + " found");
                        foundLocalVideoImage = true;
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                    }
                }

                // Check file attachments
                if (!foundLocalVideoImage) {
                    localVideoImageFile = AttachmentScanner.extractAttachedVideoimage(movie, part);
                    if (localVideoImageFile != null ) {
                        foundLocalVideoImage = true;
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                        // need to create the commonly used local video image file name
                        String extension = "." + FileTools.getFileExtension(fullVideoImageFilename);
                        String attachedImageFilename = localVideoImageBaseFilename;
                        if (firstPart < lastPart) {
                            attachedImageFilename += partSuffix + extension;
                        } else {
                            attachedImageFilename += extension;
                        }
                        mf.setVideoImageFilename(part, attachedImageFilename);
                    }
                }

                // If we don't have a filename, then fill it in here.
                if (StringTools.isNotValidString(mf.getVideoImageFilename(part))) {
                    logger.debug(LOG_MESSAGE + "No valid filename was found for part " + part);

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
                    logger.debug(LOG_MESSAGE + "Generic Video Image used from " + genericVideoImageFilename);
                    // Copy the generic filename over.
                    fullVideoImageFilename = genericVideoImageFilename;
                    foundLocalVideoImage = true;
                }

                // If we've found the VideoImage, copy it to the jukebox, otherwise download it.
                if (foundLocalVideoImage) {
                    logger.debug(LOG_MESSAGE + "Found local file: " + fullVideoImageFilename);
                    // Check to see if the URL is UNKNOWN and the local file is found, in which case
                    // the videoimage in the jukebox should be overwritten with this file
                    localOverwrite = false; // first reset the check variable
                    if (StringTools.isNotValidString(mf.getVideoImageURL(part))) {
                        localOverwrite = true;  // Means the file will be overwritten regardless of any other checks
                    }

                    // Derive the URL to the local file
                    if (StringTools.isNotValidString(mf.getVideoImageURL(part))) {
                        // This occurs when there isn't a videoimage URL in the XML
                        if (localVideoImageFile != null) {
                            // holds the old file name cause setVideoImageURL overrides videoImageFileName
                            String oldVideoImageFilename = mf.getVideoImageFilename(part);
                            mf.setVideoImageURL(part, localVideoImageFile.toURI().toString());
                            mf.setVideoImageFilename(part, oldVideoImageFilename);
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
                    // Also check for DIRTY_WATCHED to see if we need to redo the image for the watched flag
                    if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile) || videoimageOverwrite || localOverwrite || movie.isDirty(DirtyFlag.RECHECK) || movie.isDirty(DirtyFlag.WATCHED)) {
                        if (processImage(imagePlugin, movie, fullVideoImageFilename, tmpDestFilename, part)) {
                            logger.debug(LOG_MESSAGE + fullVideoImageFile.getName() + " has been copied to " + tmpDestFilename);
                        } else {
                            // Processing failed, so try backup image
                            logger.debug(LOG_MESSAGE + "Failed loading videoimage : " + fullVideoImageFilename);

                            // Copy the dummy videoimage to the temp folder
                            FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                            // Process the dummy videoimage in the temp folder
                            if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                                logger.debug(LOG_MESSAGE + "Using default videoimage");
                                mf.setVideoImageURL(part, Movie.UNKNOWN);   // So we know this is a dummy videoimage
                            } else {
                                logger.debug(LOG_MESSAGE + "Failed loading default videoimage");
                                // Copying the default image failed, so leave everything blank
                                mf.setVideoImageFilename(part, Movie.UNKNOWN);
                                mf.setVideoImageURL(part, Movie.UNKNOWN);
                            }
                        }
                    } else {
                        logger.debug(LOG_MESSAGE + finalDestinationFileName + " already exists");
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
     *
     * @param imagePlugin The image plugin to use
     * @param imageFilename The filename of the image to process
     * @param movie The movie that the image relates to
     * @return True if the image was successfully processed, false otherwise
     */
    private static boolean processImage(MovieImagePlugin imagePlugin, Movie movie, String imageFilename, String targetFilename, int part) {
        boolean imageOK = true;

        try {
            BufferedImage videoimageImage = GraphicTools.loadJPEGImage(imageFilename);

            if (videoimageImage != null) {
                videoimageImage = imagePlugin.generate(movie, videoimageImage, "videoimages" + Integer.toString(part), null);
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
     * Download the videoimage from the URL. Initially this is populated from
     * TheTVDB plugin
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
            if ((!videoimageFile.exists() && !tmpDestFile.exists()) || videoimageOverwrite || movie.isDirty(DirtyFlag.RECHECK) || movie.isDirty(DirtyFlag.NFO) || movie.isDirty(DirtyFlag.WATCHED)) {
                FileTools.makeDirectories(videoimageFile);

                // Download the videoimage using the proxy save downloadImage
                try {
                    FileTools.downloadImage(tmpDestFile, mf.getVideoImageURL(part));
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed to download videoimage : " + mf.getVideoImageURL(part) + " error: " + error.getMessage());
                    fileOK = false;
                }

                if (fileOK && processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                    logger.debug(LOG_MESSAGE + "Downloaded videoimage for " + mf.getVideoImageFilename(part) + " to " + tmpDestFilename);
                } else {
                    // failed use dummy
                    FileTools.copyFile(new File(skinHome + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                    if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                        logger.debug(LOG_MESSAGE + "Using default videoimage");
                        mf.setVideoImageURL(part, Movie.UNKNOWN); // So we know this is a dummy videoimage
                        mf.setVideoImageFilename(part, safeVideoImageFilename); // See MovieFile.java: setVideoImageURL sets setVideoImageFilename=UNKNOWN !!??
                    } else {
                        // Copying the default image failed, so leave everything blank
                        logger.debug(LOG_MESSAGE + "Failed loading default videoimage");
                        mf.setVideoImageFilename(part, Movie.UNKNOWN);
                        mf.setVideoImageURL(part, Movie.UNKNOWN);
                    }
                }
            } else {
                logger.debug(LOG_MESSAGE + "VideoImage exists for " + mf.getVideoImageFilename(part));
            }
        }
    }
}
