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
 *  BannerScanner
 *
 * Routines for locating and downloading Banner for videos
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
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Scanner for banner files in local directory
 * 
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 * 
 */
public class BannerScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static Collection<String> bannerExtensions = new ArrayList<String>();
    protected static String bannerToken;
    protected static boolean bannerOverwrite;
    protected static boolean useFolderBanner;
    protected static Collection<String> bannerImageName;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.bannerExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            bannerExtensions.add(st.nextToken());
        }

        bannerToken = PropertiesUtil.getProperty("mjb.scanner.bannerToken", ".banner");

        bannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", "false");
        
        // See if we use the folder banner artwork
        useFolderBanner = PropertiesUtil.getBooleanProperty("banner.scanner.useFolderImage", "false");
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
    public static boolean scan(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie) {
        String localBannerBaseFilename = movie.getBaseFilename();
        String fullBannerFilename = null;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        File localBannerFile = null;
        boolean foundLocalBanner = false;

        // Look for the banner.bannerToken.Extension
        fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + bannerToken;
        localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
        foundLocalBanner = localBannerFile.exists();

        // Try searching the fileCache for the filename.
        if (!foundLocalBanner) {
            localBannerFile = FileTools.findFilenameInCache(localBannerBaseFilename + bannerToken, bannerExtensions, jukebox, "BannerScanner: ");
            if (localBannerFile != null) {
                foundLocalBanner = true;
            }
        }

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
                    //System.out.println("SCANNER: " + fullBannerFilename);
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
            logger.debug("BannerScanner: File " + fullBannerFilename + " found");
            
            if (StringTools.isNotValidString(movie.getBannerFilename())) {
                movie.setBannerFilename(movie.getBaseFilename() + bannerToken + "." + PropertiesUtil.getProperty("banners.format", "jpg"));
            }
            
            if (StringTools.isNotValidString(movie.getBannerURL())) {
                movie.setBannerURL(localBannerFile.toURI().toString());
            }
            String bannerFilename = movie.getBannerFilename();
            String finalDestinationFileName = jukebox.getJukeboxRootLocationDetails() + File.separator + bannerFilename;
            String destFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + bannerFilename;

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
                        logger.debug("BannerScanner: " + fullBannerFilename + " has been copied to " + destFileName);
                        
                        ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, Movie.UNKNOWN, false);
                        movie.addArtwork(new Artwork(ArtworkType.Banner, "local", fullBannerFilename, artworkFile));
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug("BannerScanner: Failed loading banner : " + fullBannerFilename);
                }
            } else {
                logger.debug("BannerScanner: " + finalDestinationFileName + " already exists");
            }
        } else {
            // logger.debug("BannerScanner : No local Banner found for " + movie.getBaseFilename() + " attempting to download");
            
            // Don't download banners for sets as they will use the first banner from the set
            if (!movie.isSetMaster()) {
                downloadBanner(imagePlugin, jukebox, movie);
            }
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
    private static void downloadBanner(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie) {
        String id = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID); // This is the default ID
        if (!movie.isScrapeLibrary() || id.equals("0") || id.equals("-1")) {
            logger.debug("PosterScanner: Skipping online banner search for " + movie.getBaseFilename());
            return;
        }
        
        if (StringTools.isValidString(movie.getBannerURL())) {
            String safeBannerFilename = movie.getBannerFilename();
            String bannerFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeBannerFilename;
            File bannerFile = FileTools.fileCache.getFile(bannerFilename);
            String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + safeBannerFilename;
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing banner unless ForceBannerOverwrite = true
            if (bannerOverwrite || movie.isDirtyBanner() || (!bannerFile.exists() && !tmpDestFile.exists())) {
                bannerFile.getParentFile().mkdirs();

                try {
                    logger.debug("BannerScanner: Downloading banner for " + movie.getBaseFilename() + " to " + tmpDestFileName + " [calling plugin]");

                    // Download the banner using the proxy save downloadImage
                    FileTools.downloadImage(tmpDestFile, movie.getBannerURL());
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, tmpDestFileName);
                        logger.debug("BannerScanner: Downloaded banner for " + movie.getBannerURL());
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug("BannerScanner: Failed to download banner: " + movie.getBannerURL());
                    movie.setBannerURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug("BannerScanner: Banner exists for " + movie.getBaseFilename());
            }
        }
        
        return;
    }

}