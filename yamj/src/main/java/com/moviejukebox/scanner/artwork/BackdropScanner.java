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
 * BackdropScanner
 *
 * Routines for locating and downloading backdrop for people
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
    protected static boolean backdropOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBackdropOverwrite", FALSE);
    protected static List<String> backdropImageName;
    protected static String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    protected static String peopleFolder = getPeopleFolder();
    protected static String backdropToken = PropertiesUtil.getProperty("mjb.scanner.backdropToken", ".backdrop");
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
        File localBackdropFile = FileTools.findFilenameInCache(localBackdropBaseFilename + backdropToken, backdropExtensions, jukebox, LOG_MESSAGE, Boolean.TRUE);
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
        String backdropFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + safeBackdropFilename;
        File backdropFile = FileTools.fileCache.getFile(backdropFilename);
        String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + safeBackdropFilename;
        File tmpDestFile = new File(tmpDestFileName);
        backdropFile.getParentFile().mkdirs();
        tmpDestFile.getParentFile().mkdirs();

        if (StringTools.isValidString(person.getBackdropURL())) {
            // Do not overwrite existing backdrop unless ForceBackdropOverwrite = true
            if (backdropOverwrite || person.isDirtyBackdrop() || (!backdropFile.exists() && !tmpDestFile.exists())) {
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
        } else if ((backdropOverwrite || (!backdropFile.exists() && !tmpDestFile.exists()))) {
            person.clearBackdropFilename();
            if (prevBackdropFilename.equals(Movie.UNKNOWN)) {
                person.setDirty(false);
            }
        }
    }
}