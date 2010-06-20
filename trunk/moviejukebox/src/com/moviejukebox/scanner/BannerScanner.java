/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
    protected static boolean useFolderBanner;
    protected static Collection<String> bannerImageName;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.bannerExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        bannerExtensions = extensions.toArray(new String[] {});

        bannerToken = PropertiesUtil.getProperty("mjb.scanner.bannerToken", ".banner");

        bannerOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceBannersOverwrite", "false"));
        
        // See if we use the folder banner artwork
        useFolderBanner = Boolean.parseBoolean(PropertiesUtil.getProperty("banner.scanner.useFolderImage", "false"));
        if (useFolderBanner) {
	        st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.imageName", "banner"), ",;|");
	        bannerImageName = new ArrayList<String>();
	        while (st.hasMoreTokens()) {
	        	bannerImageName.add(st.nextToken());
	        }
        }

    }

    /**
     * Scan for local banners and download if necessary
     * 
     * @param imagePlugin
     * @param jukeboxDetailsRoot
     * @param tempJukeboxDetailsRoot
     * @param movie
     */
    public static boolean scan(MovieImagePlugin imagePlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String localBannerBaseFilename = movie.getBaseName();
        String fullBannerFilename = null;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        File localBannerFile = null;
        boolean foundLocalBanner = false;

        // Look for the banner.bannerToken.Extension
        fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + bannerToken;
        localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
        foundLocalBanner = localBannerFile.exists();

        // if no banner has been found, try the foldername.bannerToken.Extension
        if (!foundLocalBanner) {
            localBannerBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + bannerToken;
            localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
            foundLocalBanner = localBannerFile.exists();
        }
        
        // Check for folder banners.
        if (!foundLocalBanner && useFolderBanner) {
        	// Check for each of the farnartImageName.* files
        	for (String fanartFilename : bannerImageName) {
        		fullBannerFilename = parentPath + File.separator + fanartFilename;
                localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
                foundLocalBanner = localBannerFile.exists();

                if (!foundLocalBanner && movie.isTVShow()) {
                    // Get the parent directory and check that
                    fullBannerFilename = FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()) + File.separator + fanartFilename;
                    System.out.println("SCANNER: " + fullBannerFilename);
                    localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
                    foundLocalBanner = localBannerFile.exists();
                    if (foundLocalBanner) {
                        break;   // We found the artwork so quit the loop
                    }
                } else {
                    break;    // We found the artwork so quit the loop
                }
        	}
        }

        // If we've found the banner, copy it to the jukebox, otherwise download it.
        if (foundLocalBanner) {
            fullBannerFilename = localBannerFile.getAbsolutePath();
            logger.finest("BannerScanner: File " + fullBannerFilename + " found");
            if (movie.getBannerFilename().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setBannerFilename(movie.getBaseName() + bannerToken + "." + PropertiesUtil.getProperty("banners.format", "jpg"));
            }
            if (movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setBannerURL(localBannerFile.toURI().toString());
            }
            String bannerFilename = movie.getBannerFilename();
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + bannerFilename;
            String destFileName = tempJukeboxDetailsRoot + File.separator + bannerFilename;

            File finalDestinationFile = FileTools.fileCache.getFile(finalDestinationFileName);
            File fullBannerFile = localBannerFile;

            // Local Banner is newer OR ForcePosterOverwrite OR DirtyPoster
            // Can't check the file size because the jukebox banner may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if (bannerOverwrite || movie.isDirtyPoster() || FileTools.isNewer(fullBannerFile, finalDestinationFile)) {
                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(fullBannerFile);
                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, destFileName);
                        logger.finer("BannerScanner: " + fullBannerFilename + " has been copied to " + destFileName);
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.finer("BannerScanner: Failed loading banner : " + fullBannerFilename);
                }
            } else {
                logger.finer("BannerScanner: " + finalDestinationFileName + " already exists");
            }
        } else {
            // logger.finer("BannerScanner : No local Banner found for " + movie.getBaseName() + " attempting to download");
            
            // Don't download banners for sets as they will use the first banner from the set
            if (!movie.isSetMaster()) 
                downloadBanner(imagePlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
        }
        return foundLocalBanner;
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
            String safeBannerFilename = movie.getBannerFilename();
            String bannerFilename = jukeboxDetailsRoot + File.separator + safeBannerFilename;
            File bannerFile = FileTools.fileCache.getFile(bannerFilename);
            String tmpDestFileName = tempJukeboxDetailsRoot + File.separator + safeBannerFilename;
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing banner unless ForceBannerOverwrite = true
            if (bannerOverwrite || movie.isDirtyBanner() || (!bannerFile.exists() && !tmpDestFile.exists())) {
                bannerFile.getParentFile().mkdirs();

                try {
                    logger.finest("Banner Scanner: Downloading banner for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the banner using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, movie.getBannerURL());
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, tmpDestFileName);
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.finer("Banner Scanner: Failed to download banner : " + movie.getBannerURL());
                }
            } else {
                logger.finest("Banner Scanner: Banner exists for " + movie.getBaseName());
            }
        }
        
        return;
    }

}