/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
import com.moviejukebox.model.Person;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for photo files in local directory
 *
 * @author ilgizar Initial code copied from BannerScanner.java
 *
 */
public final class PhotoScanner {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoScanner.class);
    private static final Collection<String> EXTENSIONS = setPhotoExtensions(PropertiesUtil.getProperty("photo.scanner.photoExtensions", "jpg,jpeg,gif,bmp,png"));
    private static final boolean OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forcePhotoOverwrite", Boolean.FALSE);
    private static final String SKIN_HOME = SkinProperties.getSkinHome();
    private static final String PEOPLE_FOLDER = setPeopleFolder(PropertiesUtil.getProperty("mjb.people.folder", ""));

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
        List<String> extensions = new ArrayList<>();
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
     * @param jukebox
     * @param person
     * @return
     */
    public static boolean scan(MovieImagePlugin imagePlugin, Jukebox jukebox, Person person) {
        String localPhotoBaseFilename = person.getFilename();
        File localPhotoFile;
        boolean foundLocalPhoto = false;

        // Try searching the fileCache for the filename.
        localPhotoFile = FileTools.findFilenameInCache(localPhotoBaseFilename, EXTENSIONS, jukebox, Boolean.TRUE);
        if (localPhotoFile != null) {
            foundLocalPhoto = true;
        }

        if (!foundLocalPhoto) {
            downloadPhoto(imagePlugin, jukebox, person);
        }
        return foundLocalPhoto;
    }

    /**
     * Download the photo from the URL. Initially this is populated from TheTVDB
     * plugin
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
        String photoFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + PEOPLE_FOLDER + safePhotoFilename;
        File photoFile = FileTools.fileCache.getFile(photoFilename);
        String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + PEOPLE_FOLDER + safePhotoFilename;
        File tmpDestFile = new File(tmpDestFileName);
        String dummyFileName = SKIN_HOME + File.separator + "resources" + File.separator + "dummy_photo.jpg";
        File dummyFile = new File(dummyFileName);

        FileTools.makeDirsForFile(photoFile);
        FileTools.makeDirsForFile(tmpDestFile);

        if (StringTools.isValidString(person.getPhotoURL())) {

            // Do not overwrite existing photo unless ForcePhotoOverwrite = true
            if (OVERWRITE || person.isDirtyPhoto() || (!photoFile.exists() && !tmpDestFile.exists())) {
                try {
                    LOG.debug("Downloading photo for {} to {} [calling plugin]", person.getName(), tmpDestFileName);

                    // Download the photo using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, person.getPhotoURL());
                    BufferedImage photoImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (photoImage != null) {
//                        photoImage = imagePlugin.generate(person, photoImage, "photos", null);
                        GraphicTools.saveImageToDisk(photoImage, tmpDestFileName);
                        LOG.debug("Downloaded photo for {}", person.getPhotoURL());
                    } else {
                        person.setPhotoFilename(Movie.UNKNOWN);
                        person.setPhotoURL(Movie.UNKNOWN);
                    }
                } catch (IOException ex) {
                    LOG.debug("Failed to download/process photo: {}, error: {}", person.getPhotoURL(), ex.getMessage());
                    person.setPhotoURL(Movie.UNKNOWN);
                }
            } else {
                LOG.debug("Photo exists for {}", person.getName());
            }
        } else if ((OVERWRITE || (!photoFile.exists() && !tmpDestFile.exists()))) {
            if (dummyFile.exists()) {
                LOG.debug("Dummy image used for {}", person.getName());
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
