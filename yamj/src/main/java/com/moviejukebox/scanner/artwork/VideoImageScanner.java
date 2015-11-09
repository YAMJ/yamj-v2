/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.tools.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for video image files in local directory
 *
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 *
 */
public final class VideoImageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(VideoImageScanner.class);
    private static final String SKIN_HOME = SkinProperties.getSkinHome();
    private static final Collection<String> VI_EXT = new ArrayList<>();
    private static final String TOKEN;
    private static final boolean OVERWRITE;

    static {
        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("videoimage.scanner.videoimageExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            VI_EXT.add(st.nextToken());
        }

        TOKEN = PropertiesUtil.getProperty("mjb.scanner.videoimageToken", ".videoimage");
        OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forceVideoImagesOverwrite", Boolean.FALSE);
    }

    private VideoImageScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Try to locate a local video image and if that fails, download the image
     * from the Internet.
     *
     * @param imagePlugin
     * @param jukebox
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

        LOG.debug("Checking for videoimages for {} [Season {}]", movie.getTitle(), movie.getSeason());

        // Check for the generic video image for use in the loop later.
        localVideoImageBaseFilename = FileTools.getParentFolder(movie.getFile());

        localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1);
        genericVideoImageFilename = movie.getFile().getParent() + File.separator + localVideoImageBaseFilename + TOKEN;

        // Look for the various versions of the file with different image extensions
        genericVideoImageFile = FileTools.findFileFromExtensions(genericVideoImageFilename, VI_EXT);
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
                LOG.debug("Missing file: {}", mf.getFilename());
                continue;
            }

            // Loop round each of the parts looking for the video_name.videoimageToken.Extension
            for (int part = firstPart; part <= lastPart; part++) {
                foundLocalVideoImage = false;   // Reset the "found" variable
                String partSuffix = "_" + (part - firstPart + 1);

                if (mf.getFile().isDirectory()) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1) + TOKEN;
                } else if (mf.getFile().getParent().contains("BDMV" + File.separator + "STREAM")) {
                    localVideoImageBaseFilename = mf.getFile().getPath();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.indexOf("BDMV" + File.separator + "STREAM") - 1);
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(localVideoImageBaseFilename.lastIndexOf(File.separator) + 1) + TOKEN;
                } else {
                    localVideoImageBaseFilename = mf.getFile().getName();
                    localVideoImageBaseFilename = localVideoImageBaseFilename.substring(0, localVideoImageBaseFilename.lastIndexOf(".")) + TOKEN;
                }

                fullVideoImageFilename = FileTools.getParentFolder(mf.getFile()) + File.separator + localVideoImageBaseFilename;

                originalFullVideoImageFilename = fullVideoImageFilename;

                // Check for the videoimage filename with the suffix
                if (firstPart < lastPart) {
                    String suffixFullVideoImageFilename = originalFullVideoImageFilename + partSuffix;

                    // Check for the videoimage filename with the "_part" suffix
                    localVideoImageFile = FileTools.findFileFromExtensions(suffixFullVideoImageFilename, VI_EXT);
                    if (localVideoImageFile.exists()) {
                        LOG.debug("File {} found", localVideoImageFile.getName());
                        foundLocalVideoImage = true;
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                    }
                }

                // If the wasn't a specific "videoimage_{part}" then look for a more generic videoimage filename
                if (!foundLocalVideoImage) {
                    // Check for the videoimage filename without the "_part" suffix
                    localVideoImageFile = FileTools.findFileFromExtensions(fullVideoImageFilename, VI_EXT);
                    if (localVideoImageFile.exists()) {
                        LOG.debug("File {} found", localVideoImageFile.getName());
                        foundLocalVideoImage = true;
                        fullVideoImageFilename = localVideoImageFile.getAbsolutePath();
                        mf.setVideoImageFilename(part, localVideoImageFile.getName());
                    }
                }

                // Check file attachments
                if (!foundLocalVideoImage) {
                    localVideoImageFile = AttachmentScanner.extractAttachedVideoimage(movie, part);
                    if (localVideoImageFile != null) {
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
                    LOG.debug("No valid filename was found for part {}", part);

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
                    LOG.debug("Generic Video Image used from {}", genericVideoImageFilename);
                    // Copy the generic filename over.
                    fullVideoImageFilename = genericVideoImageFilename;
                    foundLocalVideoImage = true;
                }

                // If we've found the VideoImage, copy it to the jukebox, otherwise download it.
                if (foundLocalVideoImage) {
                    LOG.debug("Found local file: {}", fullVideoImageFilename);
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

//                    String videoimageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
                    String videoimageFilename = mf.getVideoImageFilename(part);
                    String finalDestinationFileName = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), videoimageFilename);
                    String tmpDestFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), videoimageFilename);

                    // Add the filename to the safe list
                    FileTools.addJukeboxFile(videoimageFilename);

                    File tmpDestFile = new File(tmpDestFilename);
                    File finalDestinationFile = new File(finalDestinationFileName);
                    File fullVideoImageFile = new File(fullVideoImageFilename);

                    // Local VideoImage is newer OR ForceVideoImageOverwrite OR DirtyVideoImage
                    // Can't check the file size because the jukebox videoimage may have been re-sized
                    // This may mean that the local art is different to the jukebox art even if the local file date is newer
                    // Also check for DIRTY_WATCHED to see if we need to redo the image for the watched flag
                    if (FileTools.isNewer(fullVideoImageFile, finalDestinationFile)
                            || OVERWRITE
                            || localOverwrite
                            || movie.isDirty(DirtyFlag.RECHECK)
                            || movie.isDirty(DirtyFlag.WATCHED)) {
                        if (processImage(imagePlugin, movie, fullVideoImageFilename, tmpDestFilename, part)) {
                            LOG.debug("{} has been copied to {}", fullVideoImageFile.getName(), tmpDestFilename);
                        } else {
                            // Processing failed, so try backup image
                            LOG.debug("Failed loading videoimage : {}", fullVideoImageFilename);

                            // Copy the dummy videoimage to the temp folder
                            FileTools.copyFile(new File(SKIN_HOME + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                            // Process the dummy videoimage in the temp folder
                            if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                                LOG.debug("Using default videoimage");
                                mf.setVideoImageURL(part, Movie.UNKNOWN);   // So we know this is a dummy videoimage
                            } else {
                                LOG.debug("Failed loading default videoimage");
                                // Copying the default image failed, so leave everything blank
                                mf.setVideoImageFilename(part, Movie.UNKNOWN);
                                mf.setVideoImageURL(part, Movie.UNKNOWN);
                            }
                        }
                    } else {
                        LOG.debug("{} already exists", finalDestinationFileName);
                    }
                } else {
                    // logger.debug("VideoImageScanner : No local VideoImage found for {}", movie.getBaseName() + " attempting to download");
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
        } catch (IOException ex) {
            LOG.trace("Failed to read/process image: {}", ex.getMessage(), ex);
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
//            String safeVideoImageFilename = FileTools.makeSafeFilename(mf.getVideoImageFilename(part));
            String safeVideoImageFilename = mf.getVideoImageFilename(part);
            String videoimageFilename = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), safeVideoImageFilename);
            File videoimageFile = FileTools.fileCache.getFile(videoimageFilename);
            String tmpDestFilename = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), safeVideoImageFilename);
            File tmpDestFile = new File(tmpDestFilename);
            boolean fileOK = true;
            // Add file to safe list
            FileTools.addJukeboxFile(safeVideoImageFilename);

            // Do not overwrite existing videoimage unless ForceVideoImageOverwrite = true
            if ((!videoimageFile.exists() && !tmpDestFile.exists())
                    || OVERWRITE
                    || movie.isDirty(DirtyFlag.RECHECK)
                    || movie.isDirty(DirtyFlag.NFO)
                    || movie.isDirty(DirtyFlag.WATCHED)) {
                FileTools.makeDirsForFile(videoimageFile);

                // Download the videoimage using the proxy save downloadImage
                try {
                    FileTools.downloadImage(tmpDestFile, mf.getVideoImageURL(part));
                } catch (IOException error) {
                    LOG.debug("Failed to download videoimage : {} - error: {}", mf.getVideoImageURL(part), error.getMessage());
                    fileOK = false;
                }

                if (fileOK && processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                    LOG.debug("Downloaded videoimage for {} to {}", mf.getVideoImageFilename(part), tmpDestFilename);
                } else {
                    // failed use dummy
                    FileTools.copyFile(new File(SKIN_HOME + File.separator + "resources" + File.separator + "dummy_videoimage.jpg"), tmpDestFile);

                    if (processImage(imagePlugin, movie, tmpDestFilename, tmpDestFilename, part)) {
                        LOG.debug("Using default videoimage");
                        mf.setVideoImageURL(part, Movie.UNKNOWN); // So we know this is a dummy videoimage
                        mf.setVideoImageFilename(part, safeVideoImageFilename); // See MovieFile.java: setVideoImageURL sets setVideoImageFilename=UNKNOWN !!??
                    } else {
                        // Copying the default image failed, so leave everything blank
                        LOG.debug("Failed loading default videoimage");
                        mf.setVideoImageFilename(part, Movie.UNKNOWN);
                        mf.setVideoImageURL(part, Movie.UNKNOWN);
                    }
                }
            } else {
                LOG.debug("VideoImage exists for {}", mf.getVideoImageFilename(part));
            }
        }
    }
}
