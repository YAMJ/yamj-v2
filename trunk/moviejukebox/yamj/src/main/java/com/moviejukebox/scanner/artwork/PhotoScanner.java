/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SkinProperties;
import com.moviejukebox.tools.StringTools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Scanner for photo files in local directory
 *
 * @author ilgizar Initial code copied from BannerScanner.java
 *
 */
public class PhotoScanner {

    private static final Logger logger = Logger.getLogger(PhotoScanner.class);
    private static final String LOG_MESSAGE = "PhotoScanner: ";
    private static final Collection<String> photoExtensions = setPhotoExtensions(PropertiesUtil.getProperty("photo.scanner.photoExtensions", "jpg,jpeg,gif,bmp,png"));
    private static boolean photoOverwrite = PropertiesUtil.getBooleanProperty("mjb.forcePhotoOverwrite", Boolean.FALSE);
    private static String skinHome = SkinProperties.getSkinHome();
    private static String peopleFolder = setPeopleFolder(PropertiesUtil.getProperty("mjb.people.folder", ""));

    private PhotoScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Get the list of valid extensions
     *
     * @param propertyExtensions
     * @return
     */
    private static List<String> setPhotoExtensions(String propertyExtensions) {
        List<String> extensions = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(propertyExtensions, ",;| ");

        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        return extensions;
    }

    /**
     * Set the people folder
     *
     * @param peopleFolder
     * @return
     */
    private static String setPeopleFolder(String peopleFolder) {
        String newPeopleFolder = peopleFolder;
        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        if (StringTools.isNotValidString(peopleFolder)) {
            newPeopleFolder = "";
        } else if (!peopleFolder.endsWith(File.separator)) {
            newPeopleFolder = peopleFolder + File.separator;
        }
        return newPeopleFolder;
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
        String localPhotoBaseFilename = person.getFilename();
        File localPhotoFile;
        boolean foundLocalPhoto = false;

        // Try searching the fileCache for the filename.
        localPhotoFile = FileTools.findFilenameInCache(localPhotoBaseFilename, photoExtensions, jukebox, LOG_MESSAGE, Boolean.TRUE);
        if (localPhotoFile != null) {
            foundLocalPhoto = true;
        }

        if (!foundLocalPhoto) {
            downloadPhoto(imagePlugin, jukebox, person);
        }
        return foundLocalPhoto;
    }

    /**
     * Download the photo from the URL. Initially this is populated from TheTVDB plugin
     *
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param person
     */
    private static void downloadPhoto(MovieImagePlugin imagePlugin, Jukebox jukebox, Person person) {
        String prevPhotoFilename = person.getPhotoFilename();
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
                    logger.debug(LOG_MESSAGE + "Downloading photo for " + person.getName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the photo using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, person.getPhotoURL());
                    BufferedImage photoImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (photoImage != null) {
//                        photoImage = imagePlugin.generate(person, photoImage, "photos", null);
                        GraphicTools.saveImageToDisk(photoImage, tmpDestFileName);
                        logger.debug(LOG_MESSAGE + "Downloaded photo for " + person.getPhotoURL());
                    } else {
                        person.setPhotoFilename(Movie.UNKNOWN);
                        person.setPhotoURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed to download photo: " + person.getPhotoURL());
                    person.setPhotoURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug(LOG_MESSAGE + "Photo exists for " + person.getName());
            }
        } else if ((photoOverwrite || (!photoFile.exists() && !tmpDestFile.exists()))) {
            if (dummyFile.exists()) {
                logger.debug("Dummy image used for " + person.getName());
                FileTools.copyFile(dummyFile, tmpDestFile);
            } else {
                person.clearPhotoFilename();
                if (prevPhotoFilename.equals(Movie.UNKNOWN)) {
                    person.setDirty(false);
                }
            }
        }
    }
}