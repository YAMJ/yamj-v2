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

import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Scanner for banner files in local directory
 *
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 *
 */
public class BannerScanner {

    private static final Logger logger = Logger.getLogger(BannerScanner.class);
    private static final String LOG_MESSAGE = "BannerScanner: ";
    private static Collection<String> bannerExtensions = new ArrayList<String>();
    private static String bannerToken;
    private static String wideBannerToken;
    private static boolean bannerOverwrite;
    private static final boolean useFolderBanner;
    private static Collection<String> bannerImageName;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.bannerExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            bannerExtensions.add(st.nextToken());
        }

        bannerToken = PropertiesUtil.getProperty("mjb.scanner.bannerToken", ".banner");
        wideBannerToken = PropertiesUtil.getProperty("mjb.scanner.wideBannerToken", ".wide");
        bannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", FALSE);

        // See if we use the folder banner artwork
        useFolderBanner = PropertiesUtil.getBooleanProperty("banner.scanner.useFolderImage", FALSE);
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
        File localBannerFile;
        boolean foundLocalBanner = false;

        // Look for the banner.bannerToken.Extension
        fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + wideBannerToken;
        localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, bannerExtensions);
        foundLocalBanner = localBannerFile.exists();

        // Try searching the fileCache for the filename.
        if (!foundLocalBanner) {
            Boolean searchInJukebox = Boolean.TRUE;
            // if the banner URL is invalid, but the banner filename is valid, then this is likely a recheck, so don't search on the jukebox folder
            if (StringTools.isNotValidString(movie.getBannerURL()) && StringTools.isValidString(movie.getBannerFilename())) {
                searchInJukebox = Boolean.FALSE;
            }
            localBannerFile = FileTools.findFilenameInCache(localBannerBaseFilename + wideBannerToken, bannerExtensions, jukebox, LOG_MESSAGE, searchInJukebox);
            if (localBannerFile != null) {
                foundLocalBanner = true;
            }
        }

        // if no banner has been found, try the foldername.bannerToken.Extension
        if (!foundLocalBanner) {
            localBannerBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + wideBannerToken;
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

        // Check file attachments
        if (!foundLocalBanner) {
            localBannerFile = AttachmentScanner.extractAttachedBanner(movie);
            foundLocalBanner = (localBannerFile != null);
        }

        // If we've found the banner, copy it to the jukebox, otherwise download it.
        if (foundLocalBanner) {
            fullBannerFilename = localBannerFile.getAbsolutePath();
            logger.debug(LOG_MESSAGE + "File " + fullBannerFilename + " found");

            if (StringTools.isNotValidString(movie.getBannerFilename())) {
                movie.setBannerFilename(movie.getBaseFilename() + bannerToken + "." + PropertiesUtil.getProperty("banners.format", "png"));
            }

            if (StringTools.isNotValidString(movie.getWideBannerFilename())) {
                movie.setWideBannerFilename(movie.getBaseFilename() + wideBannerToken + "." + PropertiesUtil.getProperty("banners.format", "png"));
            }

            if (StringTools.isNotValidString(movie.getBannerURL())) {
                movie.setBannerURL(localBannerFile.toURI().toString());
            }
            String bannerFilename = movie.getBannerFilename();
            String finalDestinationFileName = jukebox.getJukeboxRootLocationDetails() + File.separator + bannerFilename;
            String destFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + bannerFilename;

            File finalDestinationFile = FileTools.fileCache.getFile(finalDestinationFileName);
            File fullBannerFile = localBannerFile;
            FileTools.makeDirectories(finalDestinationFile);
            FileTools.makeDirectories(fullBannerFile);

            // Local Banner is newer OR ForcePosterOverwrite OR DirtyBanner
            // Can't check the file size because the jukebox banner may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if (bannerOverwrite || movie.isDirty(DirtyFlag.BANNER) || FileTools.isNewer(fullBannerFile, finalDestinationFile)) {
                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(fullBannerFile);
                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, destFileName);
                        logger.debug(LOG_MESSAGE + fullBannerFilename + " has been copied to " + destFileName);

                        ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, Movie.UNKNOWN, false);
                        movie.addArtwork(new Artwork(ArtworkType.Banner, "local", fullBannerFilename, artworkFile));
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setWideBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed loading banner : " + fullBannerFilename);
                }
            } else {
                logger.debug(LOG_MESSAGE + finalDestinationFileName + " already exists");
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
     * Download the banner from the URL. Initially this is populated from
     * TheTVDB plugin
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
            if (bannerOverwrite || movie.isDirty(DirtyFlag.BANNER) || (!bannerFile.exists() && !tmpDestFile.exists())) {
                bannerFile.getParentFile().mkdirs();
                String origDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + movie.getWideBannerFilename();
                File origDestFile = new File(origDestFileName);

                try {
                    logger.debug(LOG_MESSAGE + "Downloading banner for " + movie.getBaseFilename() + " to " + origDestFileName + " [calling plugin]");

                    // Download the banner using the proxy save downloadImage
                    FileTools.downloadImage(origDestFile, movie.getBannerURL());
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(origDestFile);

                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, tmpDestFileName);
                        logger.debug(LOG_MESSAGE + "Downloaded banner for " + movie.getBannerURL());
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setWideBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed to download banner: " + movie.getBannerURL());
                    movie.setBannerURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug(LOG_MESSAGE + "Banner exists for " + movie.getBaseFilename());
            }
        }

        return;
    }
}