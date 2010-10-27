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
package com.moviejukebox.scanner.artwork;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static java.lang.Boolean.parseBoolean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Jukebox;
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
public class BannerScanner extends ArtworkScanner implements IArtworkScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static boolean useFolderBanner;
    protected static Collection<String> bannerImageName;

    public BannerScanner() {
        super(ArtworkScanner.BANNER);
        
        try {
            artworkOverwrite = parseBoolean(getProperty("mjb.forceBannersOverwrite", "false"));
        } catch (Exception ignore) {
            artworkOverwrite = false;
        }

        // See if we use the folder banner artwork
        useFolderBanner = Boolean.parseBoolean(PropertiesUtil.getProperty("banner.scanner.useFolderImage", "false"));
        if (useFolderBanner) {
            StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.imageName", "banner"), ",;|");
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
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        String localBannerBaseFilename = movie.getBaseFilename();
        String fullBannerFilename = null;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        File localBannerFile = null;
        boolean foundLocalBanner = false;

        // Look for the banner.bannerToken.Extension
        fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + artworkToken;
        localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, artworkExtensions);
        foundLocalBanner = localBannerFile.exists();

        // if no banner has been found, try the foldername.bannerToken.Extension
        if (!foundLocalBanner) {
            localBannerBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + artworkToken;
            localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, artworkExtensions);
            foundLocalBanner = localBannerFile.exists();
        }
        
        // Check for folder banners.
        if (!foundLocalBanner && useFolderBanner) {
            // Check for each of the farnartImageName.* files
            for (String fanartFilename : bannerImageName) {
                fullBannerFilename = parentPath + File.separator + fanartFilename;
                localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, artworkExtensions);
                foundLocalBanner = localBannerFile.exists();

                if (!foundLocalBanner && movie.isTVShow()) {
                    // Get the parent directory and check that
                    fullBannerFilename = FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()) + File.separator + fanartFilename;
                    localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, artworkExtensions);
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
                movie.setBannerFilename(movie.getBaseName() + artworkToken + "." + artworkFormat);
            }
            if (movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) {
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
            if (artworkOverwrite || movie.isDirtyPoster() || FileTools.isNewer(fullBannerFile, finalDestinationFile)) {
                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(fullBannerFile);
                    if (bannerImage != null) {
                        bannerImage = artworkImagePlugin.generate(movie, bannerImage, "banners", null);
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
            // logger.finer("BannerScanner : No local Banner found for " + movie.getBaseFilename() + " attempting to download");
            
            // Don't download banners for sets as they will use the first banner from the set
            if (!movie.isSetMaster()) {
                downloadBanner(artworkImagePlugin, jukebox, movie);
            }
        }
        return getArtworkUrl(movie);
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
    private void downloadBanner(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie) {
        if (movie.getBannerURL() != null && !movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            String safeBannerFilename = movie.getBannerFilename();
            String bannerFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeBannerFilename;
            File bannerFile = FileTools.fileCache.getFile(bannerFilename);
            String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + safeBannerFilename;
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing banner unless ForceBannerOverwrite = true
            if (artworkOverwrite || movie.isDirtyBanner() || (!bannerFile.exists() && !tmpDestFile.exists())) {
                bannerFile.getParentFile().mkdirs();

                try {
                    logger.finest("Banner Scanner: Downloading banner for " + movie.getBaseFilename() + " to " + tmpDestFileName + " [calling plugin]");

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
                logger.finest("Banner Scanner: Banner exists for " + movie.getBaseFilename());
            }
        }
        
        return;
    }

    
    @Override
    public String getArtworkFilename(Movie movie) {
        return movie.getBannerFilename();
    }

    
    @Override
    public String getArtworkUrl(Movie movie) {
        return movie.getBannerURL();
    }

    
    @Override
    public void setArtworkFilename(Movie movie, String artworkFilename) {
        movie.setBannerFilename(artworkFilename);
    }

    
    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        movie.setBannerURL(artworkUrl);
    }

    @Override
    public String scanOnlineArtwork(Movie movie) {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Override
    public void setArtworkImagePlugin() {
        setImagePlugin(PropertiesUtil.getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
    }

    @Override
    public boolean isDirtyArtwork(Movie movie) {
        return movie.isDirtyBanner();
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        // TODO Auto-generated method stub
        
    }
    
}