/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.scanner.AttachmentScanner;
import com.moviejukebox.tools.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for banner files in local directory
 *
 * @author Stuart.Boston
 * @version 1.0, 25th August 2009 - Initial code copied from FanartScanner.java
 *
 */
public final class BannerScanner {

    private static final Logger LOG = LoggerFactory.getLogger(BannerScanner.class);
    private static final Collection<String> EXTENSIONS = new ArrayList<>();
    private static final String BANNER_TOKEN = PropertiesUtil.getProperty("mjb.scanner.bannerToken", ".banner");
    private static final String WIDE_BANNER_TOKEN = PropertiesUtil.getProperty("mjb.scanner.wideBannerToken", ".wide");
    private static final boolean OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", Boolean.FALSE);
    private static final boolean USE_FOLDER_BANNER;
    private static final Collection<String> IMAGE_NAME;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.bannerExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            EXTENSIONS.add(st.nextToken());
        }

        // See if we use the folder banner artwork
        USE_FOLDER_BANNER = PropertiesUtil.getBooleanProperty("banner.scanner.useFolderImage", Boolean.FALSE);
        if (USE_FOLDER_BANNER) {
            st = new StringTokenizer(PropertiesUtil.getProperty("banner.scanner.imageName", "banner"), ",;|");
            IMAGE_NAME = new ArrayList<>();
            while (st.hasMoreTokens()) {
                IMAGE_NAME.add(st.nextToken());
            }
        } else {
            IMAGE_NAME = Collections.emptyList();
        }
    }

    private BannerScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Scan for local banners and download if necessary
     *
     * @param imagePlugin
     * @param jukebox
     * @param movie
     * @return
     */
    public static boolean scan(MovieImagePlugin imagePlugin, Jukebox jukebox, Movie movie) {
        String localBannerBaseFilename = movie.getBaseFilename();
        String parentPath = FileTools.getParentFolder(movie.getFile());

        // Look for the banner.bannerToken.Extension
        String fullBannerFilename = parentPath + File.separator + localBannerBaseFilename + WIDE_BANNER_TOKEN;
        File localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, EXTENSIONS);
        boolean foundLocalBanner = localBannerFile.exists();

        // Try searching the fileCache for the filename.
        if (!foundLocalBanner) {
            Boolean searchInJukebox = Boolean.FALSE;

            // Look for the local file in the cache but NOT the jukebox
            localBannerFile = FileTools.findFilenameInCache(localBannerBaseFilename + BANNER_TOKEN, EXTENSIONS, jukebox, searchInJukebox);

            if (localBannerFile != null) {
                foundLocalBanner = Boolean.TRUE;
            } else {
                searchInJukebox = Boolean.TRUE;
                // if the banner URL is invalid, but the banner filename is valid, then this is likely a recheck, so don't search on the jukebox folder
                if (StringTools.isNotValidString(movie.getBannerURL()) && StringTools.isValidString(movie.getBannerFilename())) {
                    searchInJukebox = Boolean.FALSE;
                }

                localBannerFile = FileTools.findFilenameInCache(localBannerBaseFilename + WIDE_BANNER_TOKEN, EXTENSIONS, jukebox, searchInJukebox);
                if (localBannerFile != null) {
                    foundLocalBanner = Boolean.TRUE;
                }
            }
        }

        // if no banner has been found, try the foldername.bannerToken.Extension
        if (!foundLocalBanner) {
            localBannerBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullBannerFilename = StringTools.appendToPath(parentPath, localBannerBaseFilename + BANNER_TOKEN);
            localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, EXTENSIONS);
            foundLocalBanner = localBannerFile.exists();

            // Try the wide banner
            if (!foundLocalBanner) {
                fullBannerFilename = StringTools.appendToPath(parentPath, localBannerBaseFilename + WIDE_BANNER_TOKEN);
                localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, EXTENSIONS);
                foundLocalBanner = localBannerFile.exists();
            }
        }

        // Check for folder banners.
        if (!foundLocalBanner && USE_FOLDER_BANNER) {
            // Check for each of the farnartImageName.* files
            for (String imageFilename : IMAGE_NAME) {
                fullBannerFilename = StringTools.appendToPath(parentPath, imageFilename);
                localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, EXTENSIONS);
                foundLocalBanner = localBannerFile.exists();

                if (!foundLocalBanner && movie.isTVShow()) {
                    // Get the parent directory and check that
                    fullBannerFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()), imageFilename);
                    localBannerFile = FileTools.findFileFromExtensions(fullBannerFilename, EXTENSIONS);
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
        if (foundLocalBanner && localBannerFile != null) {
            fullBannerFilename = localBannerFile.getAbsolutePath();
            LOG.debug("File {} found", fullBannerFilename);

            if (StringTools.isNotValidString(movie.getBannerFilename())) {
                movie.setBannerFilename(movie.getBaseFilename() + BANNER_TOKEN + "." + PropertiesUtil.getProperty("banners.format", "png"));
            }

            if (StringTools.isNotValidString(movie.getWideBannerFilename())) {
                movie.setWideBannerFilename(movie.getBaseFilename() + WIDE_BANNER_TOKEN + "." + PropertiesUtil.getProperty("banners.format", "png"));
            }

            if (StringTools.isNotValidString(movie.getBannerURL())) {
                movie.setBannerURL(localBannerFile.toURI().toString());
            }
            String bannerFilename = movie.getBannerFilename();
            String finalDestinationFileName = jukebox.getJukeboxRootLocationDetails() + File.separator + bannerFilename;
            String destFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + bannerFilename;

            File finalDestinationFile = FileTools.fileCache.getFile(finalDestinationFileName);
            File fullBannerFile = localBannerFile;
            FileTools.makeDirsForFile(finalDestinationFile);
            FileTools.makeDirsForFile(fullBannerFile);

            // Local Banner is newer OR ForcePosterOverwrite OR DirtyBanner
            // Can't check the file size because the jukebox banner may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if (OVERWRITE || movie.isDirty(DirtyFlag.BANNER) || FileTools.isNewer(fullBannerFile, finalDestinationFile)) {
                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(fullBannerFile);
                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, destFileName);
                        LOG.debug("{} has been copied to {}", fullBannerFilename, destFileName);
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setWideBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (IOException ex) {
                    LOG.debug("Failed loading banner '{}', error: {}", fullBannerFilename, ex.getMessage());
                }
            } else {
                LOG.debug("{} already exists", finalDestinationFileName);
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
        if (!movie.isScrapeLibrary() || "0".equals(id) || "-1".equals(id)) {
            LOG.debug("Skipping online banner search for {}", movie.getBaseFilename());
            return;
        }

        if (StringTools.isValidString(movie.getBannerURL())) {
            String safeBannerFilename = movie.getBannerFilename();
            String bannerFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeBannerFilename;
            File bannerFile = FileTools.fileCache.getFile(bannerFilename);
            String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + safeBannerFilename;
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing banner unless ForceBannerOverwrite = true
            if (OVERWRITE || movie.isDirty(DirtyFlag.BANNER) || (!bannerFile.exists() && !tmpDestFile.exists())) {
                FileTools.makeDirsForFile(bannerFile);
                String origDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + movie.getWideBannerFilename();
                File origDestFile = new File(origDestFileName);

                try {
                    LOG.debug("Downloading banner for {} to {} [calling plugin]", movie.getBaseFilename(), origDestFileName);

                    // Download the banner using the proxy save downloadImage
                    FileTools.downloadImage(origDestFile, movie.getBannerURL());
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(origDestFile);

                    if (bannerImage != null) {
                        bannerImage = imagePlugin.generate(movie, bannerImage, "banners", null);
                        GraphicTools.saveImageToDisk(bannerImage, tmpDestFileName);
                        LOG.debug("Downloaded banner for {}", movie.getBannerURL());
                    } else {
                        movie.setBannerFilename(Movie.UNKNOWN);
                        movie.setWideBannerFilename(Movie.UNKNOWN);
                        movie.setBannerURL(Movie.UNKNOWN);
                    }
                } catch (IOException error) {
                    LOG.debug("Failed to download banner: {}", movie.getBannerURL());
                    movie.setBannerURL(Movie.UNKNOWN);
                }
            } else {
                LOG.debug("Banner exists for {}", movie.getBaseFilename());
            }
        }
    }
}
