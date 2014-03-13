/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SkinProperties;
import com.moviejukebox.tools.StringTools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.apache.sanselan.ImageReadException;
import org.slf4j.LoggerFactory;

/**
 * Scanner for personal backdrop files in local directory
 *
 * @author ilgizar
 *
 */
public final class BackdropScanner {

    private static final Logger LOG = LoggerFactory.getLogger(BackdropScanner.class);
    private static final String LOG_MESSAGE = "BackdropScanner: ";
    protected static final List<String> EXT = new ArrayList<String>();
    protected static final boolean BACKDROP_OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forceBackdropOverwrite", Boolean.FALSE);
    protected static List<String> backdropImageName;
    protected static String skinHome = SkinProperties.getSkinHome();
    protected static final String PEOPLE_FOLDER = getPeopleFolder();
    protected static final String BACKDROP_TOKEN = PropertiesUtil.getProperty("mjb.scanner.backdropToken", ".backdrop");
    protected static int backdropWidth;
    protected static int backdropHeight;

    static {
        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("backdrop.scanner.backdropExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            EXT.add(st.nextToken());
        }
    }

    private BackdropScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    private static String getPeopleFolder() {
        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        String pFolder = PropertiesUtil.getProperty("mjb.people.folder", "");
        if (StringTools.isNotValidString(pFolder)) {
            return "";
        } else if (!pFolder.endsWith(File.separator)) {
            pFolder += File.separator;
        }
        return pFolder;
    }

    /**
     * Scan for local backdrop and download if necessary
     *
     * @param jukebox
     * @param person
     * @return
     */
    public static boolean scan(Jukebox jukebox, Person person) {
        String localBackdropBaseFilename = person.getFilename();
        boolean foundLocalBackdrop = false;

        // Try searching the fileCache for the filename.
        File localBackdropFile = FileTools.findFilenameInCache(localBackdropBaseFilename + BACKDROP_TOKEN, EXT, jukebox, LOG_MESSAGE, Boolean.TRUE);
        if (localBackdropFile != null) {
            foundLocalBackdrop = true;
            person.setBackdropFilename();
        } else {
            downloadBackdrop(jukebox, person);
        }
        return foundLocalBackdrop;
    }

    /**
     * Download the backdrop from the URL. Initially this is populated from TheTVDB plugin
     *
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param person
     */
    private static void downloadBackdrop(Jukebox jukebox, Person person) {
        String prevBackdropFilename = person.getBackdropFilename();
        person.setBackdropFilename();
        String safeBackdropFilename = person.getBackdropFilename();
        String backdropFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + PEOPLE_FOLDER + safeBackdropFilename;
        File backdropFile = FileTools.fileCache.getFile(backdropFilename);
        String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + PEOPLE_FOLDER + safeBackdropFilename;
        File tmpDestFile = new File(tmpDestFileName);

        FileTools.makeDirsForFile(backdropFile);
        FileTools.makeDirsForFile(tmpDestFile);

        if (StringTools.isValidString(person.getBackdropURL())) {
            // Do not overwrite existing backdrop unless ForceBackdropOverwrite = true
            if (BACKDROP_OVERWRITE || person.isDirtyBackdrop() || (!backdropFile.exists() && !tmpDestFile.exists())) {
                try {
                    LOG.debug(LOG_MESSAGE + "Downloading backdrop for " + person.getName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the backdrop using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, person.getBackdropURL());
                    BufferedImage backdropImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (backdropImage != null) {
                        GraphicTools.saveImageToDisk(backdropImage, tmpDestFileName);
                        LOG.debug(LOG_MESSAGE + "Downloaded backdrop for " + person.getBackdropURL());
                    } else {
                        person.setBackdropFilename(Movie.UNKNOWN);
                        person.setBackdropURL(Movie.UNKNOWN);
                    }
                } catch (IOException ex) {
                    LOG.debug(LOG_MESSAGE + "Failed to download/process backdrop: " + person.getBackdropURL() + ", error: " + ex.getMessage());
                    person.setBackdropURL(Movie.UNKNOWN);
                } catch (ImageReadException ex) {
                    LOG.debug(LOG_MESSAGE + "Failed to process backdrop: " + person.getBackdropURL() + ", error: " + ex.getMessage());
                    person.setBackdropURL(Movie.UNKNOWN);
                }
            } else {
                LOG.debug(LOG_MESSAGE + "Backdrop exists for " + person.getName());
            }
        } else if ((BACKDROP_OVERWRITE || (!backdropFile.exists() && !tmpDestFile.exists()))) {
            person.clearBackdropFilename();
            if (prevBackdropFilename.equals(Movie.UNKNOWN)) {
                person.setDirty(false);
            }
        }
    }
}
