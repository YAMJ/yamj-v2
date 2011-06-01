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
 *  PhotoScanner
 *
 * Routines for locating and downloading Photo for people
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
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Scanner for photo files in local directory
 * 
 * @author ilgizar
 * Initial code copied from BannerScanner.java
 * 
 */
public class PhotoScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static Collection<String> photoExtensions = new ArrayList<String>();
    protected static boolean photoOverwrite = PropertiesUtil.getBooleanProperty("mjb.forcePhotoOverwrite", "false");
    protected static Collection<String> photoImageName;
    protected static String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    protected static String peopleFolder;

    static {
        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("photo.scanner.photoExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            photoExtensions.add(st.nextToken());
        }

        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        peopleFolder = PropertiesUtil.getProperty("mjb.people.folder", "");
        if (StringTools.isNotValidString(peopleFolder)) {
            peopleFolder = "";
        } else if (!peopleFolder.endsWith(File.separator)) {
            peopleFolder += File.separator;
        }
    }

    /**
     * Scan for local photo and download if necessary
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param person
     */
    public static boolean scan(MovieImagePlugin imagePlugin, Jukebox jukebox, Person person) {
        String localPhotoBaseFilename = person.getName();
        File localPhotoFile = null;
        boolean foundLocalPhoto = false;

        // Try searching the fileCache for the filename.
        localPhotoFile = FileTools.findFilenameInCache(localPhotoBaseFilename, photoExtensions, jukebox, "PhotoScanner: ");
        if (localPhotoFile != null) {
            foundLocalPhoto = true;
        }

        if (!foundLocalPhoto) {
            downloadPhoto(imagePlugin, jukebox, person);
        }
        return foundLocalPhoto;
    }

    /**
     * Download the photo from the URL.
     * Initially this is populated from TheTVDB plugin
     * 
     * @param imagePlugin  
     * @param jukeboxDetailsRoot   
     * @param tempJukeboxDetailsRoot
     * @param person
     */
    private static void downloadPhoto(MovieImagePlugin imagePlugin, Jukebox jukebox, Person person) {
        person.setPhotoFilename();
        String safePhotoFilename = person.getPhotoFilename();
        String photoFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + safePhotoFilename;
        File photoFile = FileTools.fileCache.getFile(photoFilename);
        String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + safePhotoFilename;
        File tmpDestFile = new File(tmpDestFileName);
        String dummyFileName = skinHome + File.separator + "resources" + File.separator + "dummy_photo.jpg";
        File dummyFile = new File(dummyFileName);
        photoFile.getParentFile().mkdirs();
        tmpDestFile.getParentFile().mkdirs();

        if (StringTools.isValidString(person.getPhotoURL())) {

            // Do not overwrite existing photo unless ForcePhotoOverwrite = true
            if (photoOverwrite || person.isDirtyPhoto() || (!photoFile.exists() && !tmpDestFile.exists())) {
                try {
                    logger.debug("PhotoScanner: Downloading photo for " + person.getName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the photo using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, person.getPhotoURL());
                    BufferedImage photoImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (photoImage != null) {
//                        photoImage = imagePlugin.generate(person, photoImage, "photos", null);
                        GraphicTools.saveImageToDisk(photoImage, tmpDestFileName);
                        logger.debug("PhotoScanner: Downloaded photo for " + person.getPhotoURL());
                    } else {
                        person.setPhotoFilename(Movie.UNKNOWN);
                        person.setPhotoURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug("PhotoScanner: Failed to download photo: " + person.getPhotoURL());
                    person.setPhotoURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug("PhotoScanner: Photo exists for " + person.getName());
            }
        } else if ((photoOverwrite || (!photoFile.exists() && !tmpDestFile.exists())) && dummyFile.exists()) {
            logger.debug("Dummy image used for " + person.getName());
            FileTools.copyFile(dummyFile, tmpDestFile);
        }

        return;
    }

}