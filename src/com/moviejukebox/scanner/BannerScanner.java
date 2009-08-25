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
 *  Banner Scanner
 *
 * Routines for locating and downloading Banner for videos
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
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Scanner for banner files in local directory
 * 
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 * 
 */
public class BannerScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String[] bannerExtensions;
    protected static String bannerToken;
    protected static boolean bannerOverwrite;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.bannerExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        bannerExtensions = extensions.toArray(new String[] {});

        bannerToken = PropertiesUtil.getProperty("banner.scanner.bannerToken", ".banner");

        bannerOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forcePosterOverwrite", "false"));
    }

    public static void scan(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String localBannerBaseFilename = FileTools.makeSafeFilename(movie.getBaseName());
        String fullBannerFilename = null;
        String foundExtension = null;
        File localBannerFile = null;
        boolean foundLocalBanner = false;

        // Look for the banner.bannerToken.Extension
        if (movie.getFile().isDirectory()) { // for VIDEO_TS
            fullBannerFilename = movie.getFile().getPath();
        } else {
            fullBannerFilename = movie.getFile().getParent();
        }

        fullBannerFilename += File.separator + localBannerBaseFilename + bannerToken;
        foundExtension = findBannerFile(fullBannerFilename, bannerExtensions);
        if (!foundExtension.equals("")) {
            // The filename and extension was found
            fullBannerFilename += foundExtension;

            localBannerFile = new File(fullBannerFilename); // Double check it still works
            if (localBannerFile.exists()) {
                logger.finest("BannerScanner: File " + fullBannerFilename + " found");
                foundLocalBanner = true;
            }
        }

        // if no banner has been found, try the foldername.bannerToken.Extension
        if (!foundLocalBanner) {
            localBannerBaseFilename = movie.getFile().getParent();
            localBannerBaseFilename = localBannerBaseFilename.substring(localBannerBaseFilename.lastIndexOf(File.separator) + 1);

            // Checking for the MovieFolderName.*
            fullBannerFilename = movie.getFile().getParent() + File.separator + localBannerBaseFilename + bannerToken;
            foundExtension = findBannerFile(fullBannerFilename, bannerExtensions);
            if (!foundExtension.equals("")) {
                // The filename and extension was found
                fullBannerFilename += foundExtension;
                localBannerFile = new File(fullBannerFilename);
                if (localBannerFile.exists()) {
                    logger.finest("BannerScanner: File " + fullBannerFilename + " found");
                    foundLocalBanner = true;
                }
            }
        }

        // If we've found the banner, copy it to the jukebox, otherwise download it.
        if (foundLocalBanner) {
            if (movie.getBannerFilename().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setBannerFilename(movie.getBaseName() + bannerToken + ".jpg");
            }
            if (movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setBannerURL(localBannerFile.toURI().toString());
            }
            String bannerFilename = FileTools.makeSafeFilename(movie.getBannerFilename());
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + bannerFilename;
            String destFileName = tempJukeboxDetailsRoot + File.separator + bannerFilename;

            File finalDestinationFile = new File(finalDestinationFileName);
            File fullBannerFile = new File(fullBannerFilename);

            // Local Banner is newer OR ForcePosterOverwrite OR DirtyPoster
            // Can't check the file size because the jukebox banner may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if (FileTools.isNewer(fullBannerFile, finalDestinationFile) || bannerOverwrite || movie.isDirtyPoster()) {
                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(new FileInputStream(fullBannerFile));
                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, destFileName);
                        logger.finer("BannerScanner: " + fullBannerFilename + " has been copied to " + destFileName);
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception e) {
                    logger.finer("BannerScanner: Failed loading banner : " + fullBannerFilename);
                }
            } else {
                logger.finer("BannerScanner: " + finalDestinationFileName + " already exists");
            }
        } else {
            // logger.finer("BannerScanner : No local Banner found for " + movie.getBaseName() + " attempting to download");
            downloadBanner(imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
        }
    }

    /**
     * Download the banner from the URL.
     * Initially this is populated from TheTVDB plugin
     * 
     * @param imagePlugin  
     * @param jukeboxDetailsRoot   
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    private static void downloadBanner(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        if (movie.getBannerURL() != null && !movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            String safeBannerFilename = FileTools.makeSafeFilename(movie.getBannerFilename());
            String bannerFilename = jukeboxDetailsRoot + File.separator + safeBannerFilename;
            File bannerFile = new File(bannerFilename);
            String tmpDestFileName = tempJukeboxDetailsRoot + File.separator + safeBannerFilename;
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing banner unless ForceBannerOverwrite = true
            if ((!bannerFile.exists() && !tmpDestFile.exists()) || bannerOverwrite) {
                bannerFile.getParentFile().mkdirs();

                try {
                    logger.finest("Banner Scanner: Downloading banner for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the banner using the proxy save downloadImage
                    FileTools.downloadImage(bannerFile, movie.getBannerURL());
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(new FileInputStream(bannerFilename));

                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, tmpDestFileName);
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception e) {
                    logger.finer("Banner Scanner: Failed to download banner : " + movie.getBannerURL());
                }
            } else {
                logger.finest("Banner Scanner: Banner exists for " + movie.getBaseName());
            }
        }
        
        return;
    }

    /***
     * Pass in the filename and a list of extensions, this function will scan for the filename plus extensions and return the extension
     * 
     * @param filename
     * @param extensions
     * @return extension of banner that was found
     */
    private static String findBannerFile(String fullBannerFilename, String[] bannerExtensions) {
        File localBannerFile;
        boolean foundLocalBanner = false;

        for (String extension : bannerExtensions) {
            localBannerFile = new File(fullBannerFilename + "." + extension);
            if (localBannerFile.exists()) {
                logger.finest("The file " + fullBannerFilename + "." + extension + " found");
                fullBannerFilename = "." + extension;
                foundLocalBanner = true;
                break;
            }
        }

        // If we've found the filename with extension, return it, otherwise return ""
        if (foundLocalBanner) {
            return fullBannerFilename;
        } else {
            return "";
        }
    }
}