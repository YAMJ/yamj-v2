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
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Scanner for personal backdrop files in local directory
 *
 * @author ilgizar
 *
 */
public class BackdropScanner {

    private static final Logger logger = Logger.getLogger(BackdropScanner.class);
    private static final String LOG_MESSAGE = "BackdropScanner: ";
    protected static final List<String> backdropExtensions = new ArrayList<String>();
    protected static final boolean BACKDROP_OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forceBackdropOverwrite", FALSE);
    protected static List<String> backdropImageName;
    protected static String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    protected static final String PEOPLE_FOLDER = getPeopleFolder();
    protected static final String BACKDROP_TOKEN = PropertiesUtil.getProperty("mjb.scanner.backdropToken", ".backdrop");
    protected static int backdropWidth;
    protected static int backdropHeight;

    static {
        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("backdrop.scanner.backdropExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            backdropExtensions.add(st.nextToken());
        }
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
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param person
     */
    public static boolean scan(Jukebox jukebox, Person person) {
        String localBackdropBaseFilename = person.getFilename();
        boolean foundLocalBackdrop = false;

        // Try searching the fileCache for the filename.
        File localBackdropFile = FileTools.findFilenameInCache(localBackdropBaseFilename + BACKDROP_TOKEN, backdropExtensions, jukebox, LOG_MESSAGE, Boolean.TRUE);
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
        backdropFile.getParentFile().mkdirs();
        tmpDestFile.getParentFile().mkdirs();

        if (StringTools.isValidString(person.getBackdropURL())) {
            // Do not overwrite existing backdrop unless ForceBackdropOverwrite = true
            if (BACKDROP_OVERWRITE || person.isDirtyBackdrop() || (!backdropFile.exists() && !tmpDestFile.exists())) {
                try {
                    logger.debug(LOG_MESSAGE + "Downloading backdrop for " + person.getName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the backdrop using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, person.getBackdropURL());
                    BufferedImage backdropImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (backdropImage != null) {
//                        backdropImage = imagePlugin.generate(person, backdropImage, "backdrops", null);
                        GraphicTools.saveImageToDisk(backdropImage, tmpDestFileName);
                        logger.debug(LOG_MESSAGE + "Downloaded backdrop for " + person.getBackdropURL());
                    } else {
                        person.setBackdropFilename(Movie.UNKNOWN);
                        person.setBackdropURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed to download backdrop: " + person.getBackdropURL());
                    person.setBackdropURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug(LOG_MESSAGE + "Backdrop exists for " + person.getName());
            }
        } else if ((BACKDROP_OVERWRITE || (!backdropFile.exists() && !tmpDestFile.exists()))) {
            person.clearBackdropFilename();
            if (prevBackdropFilename.equals(Movie.UNKNOWN)) {
                person.setDirty(false);
            }
        }
    }
}