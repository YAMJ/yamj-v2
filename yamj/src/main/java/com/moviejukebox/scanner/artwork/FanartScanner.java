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
 *  FanartScanner
 *
 * Routines for locating and downloading Fanart for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.Artwork;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Scanner for fanart files in local directory
 * 
 * @author Stuart.Boston
 * @version 1.0, 10th December 2008 - Initial code
 * @version 1.1, 19th July 2009 - Added Internet search
 */
public class FanartScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static Collection<String> fanartExtensions = new ArrayList<String>();
    protected static String fanartToken;
    protected static boolean fanartOverwrite;
    protected static boolean useFolderBackground;
    protected static Collection<String> fanartImageName;

    protected static boolean    artworkValidate;
    protected static int        artworkValidateMatch;
    protected static boolean    artworkValidateAspect;
    protected static int        artworkWidth;
    protected static int        artworkHeight;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.fanartExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            fanartExtensions.add(st.nextToken());
        }

        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");

        fanartOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFanartOverwrite", "false");
        
        // See if we use background.* or fanart.*
        useFolderBackground = PropertiesUtil.getBooleanProperty("fanart.scanner.useFolderImage", "false");
        if (useFolderBackground) {
            st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.imageName", "fanart,backdrop,background"), ",;|");
            fanartImageName = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                fanartImageName.add(st.nextToken());
            }
        }

        artworkWidth = PropertiesUtil.getIntProperty("fanart.width", "0");
        artworkHeight = PropertiesUtil.getIntProperty("fanart.height", "0");
        artworkValidate = PropertiesUtil.getBooleanProperty("fanart.scanner.Validate", "true");
        artworkValidateMatch = PropertiesUtil.getIntProperty("fanart.scanner.ValidateMatch", "75");
        artworkValidateAspect = PropertiesUtil.getBooleanProperty("fanart.scanner.ValidateAspect", "true");

    }

    public static boolean scan(MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie) {
        String localFanartBaseFilename = movie.getBaseFilename();
        String fullFanartFilename = null;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        File localFanartFile = null;
        boolean foundLocalFanart = false;

        // Look for the videoname.fanartToken.Extension
        fullFanartFilename = StringTools.appendToPath(parentPath, localFanartBaseFilename + fanartToken);
        localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
        foundLocalFanart = localFanartFile.exists();

        // Try searching the fileCache for the filename.
        if (!foundLocalFanart) {
            localFanartFile = FileTools.findFilenameInCache(localFanartBaseFilename + fanartToken, fanartExtensions, jukebox, "FanartScanner: ");
            if (localFanartFile != null) {
                foundLocalFanart = true;
            }
        }

        // if no fanart has been found, try the foldername.fanartToken.Extension
        if (!foundLocalFanart) {
            localFanartBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullFanartFilename = StringTools.appendToPath(parentPath, localFanartBaseFilename + fanartToken);
            localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
            foundLocalFanart = localFanartFile.exists();
        }

        // Check for fanart.* and background.* fanart.
        if (!foundLocalFanart && useFolderBackground) {
            // Check for each of the farnartImageName.* files
            for (String fanartFilename : fanartImageName) {
                fullFanartFilename = StringTools.appendToPath(parentPath, fanartFilename);
                localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
                foundLocalFanart = localFanartFile.exists();

                if (!foundLocalFanart && movie.isTVShow()) {
                    // Get the parent directory and check that
                    fullFanartFilename = StringTools.appendToPath(FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()), fanartFilename);
                    //System.out.println("SCANNER: " + fullFanartFilename);
                    localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
                    foundLocalFanart = localFanartFile.exists();
                    if (foundLocalFanart) {
                        break;   // We found the artwork so quit the loop
                    }
                } else {
                    break;    // We found the artwork so quit the loop
                }
            }
        }

        // If we've found the fanart, copy it to the jukebox, otherwise download it.
        if (foundLocalFanart) {
            fullFanartFilename = localFanartFile.getAbsolutePath();
            logger.finest("FanartScanner: File " + fullFanartFilename + " found");

            if (StringTools.isNotValidString(movie.getFanartFilename())) {
                movie.setFanartFilename(movie.getBaseFilename() + fanartToken + "." + FileTools.getFileExtension(localFanartFile.getName()));
            }
            
            if (StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(localFanartFile.toURI().toString());
            }
            String fanartFilename = movie.getFanartFilename();
            String finalDestinationFileName = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), fanartFilename);
            String destFileName = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), fanartFilename);

            File finalDestinationFile = FileTools.fileCache.getFile(finalDestinationFileName);
            File fullFanartFile = new File(fullFanartFilename);

            // Local Fanart is newer OR ForceFanartOverwrite OR DirtyFanart
            // Can't check the file size because the jukebox fanart may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if (FileTools.isNewer(fullFanartFile, finalDestinationFile) || fanartOverwrite || movie.isDirtyFanart()) {
                try {
                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(fullFanartFile);
                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage, "fanart", null);
                        if (PropertiesUtil.getBooleanProperty("fanart.perspective", "false")) {
                            destFileName = destFileName.subSequence(0, destFileName.lastIndexOf(".") + 1) + "png";
                            movie.setFanartFilename(destFileName);
                        }
                        GraphicTools.saveImageToDisk(fanartImage, destFileName);
                        logger.finer("FanartScanner: " + fullFanartFilename + " has been copied to " + destFileName);
                        
                        ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, fullFanartFilename, false);
                        movie.addArtwork(new com.moviejukebox.model.Artwork.Artwork(ArtworkType.Fanart, "local", fullFanartFilename, artworkFile));
                        
                    } else {
                        movie.setFanartFilename(Movie.UNKNOWN);
                        movie.setFanartURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.finer("FanartScanner: Failed loading fanart : " + fullFanartFilename);
                }
            } else {
                logger.finer("FanartScanner: " + finalDestinationFileName + " already exists");
            }
        } else {
            // logger.finer("FanartScanner : No local Fanart found for " + movie.getBaseFilename() + " attempting to download");
            downloadFanart(backgroundPlugin, jukebox, movie);
        }
        
        return foundLocalFanart;
    }

    private static void downloadFanart(MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie) {
        if (StringTools.isValidString(movie.getFanartURL())) {
            String safeFanartFilename = movie.getFanartFilename();
            String fanartFilename = StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), safeFanartFilename);
            File fanartFile = FileTools.fileCache.getFile(fanartFilename);
            String tmpDestFileName = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), safeFanartFilename);
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing fanart unless ForceFanartOverwrite = true
            if (fanartOverwrite || (!fanartFile.exists() && !tmpDestFile.exists())) {
                fanartFile.getParentFile().mkdirs();

                try {
                    logger.finest("FanartScanner: Downloading fanart for " + movie.getBaseFilename() + " to " + tmpDestFileName + " [calling plugin]");

                    FileTools.downloadImage(tmpDestFile, URLDecoder.decode(movie.getFanartURL(), "UTF-8"));
                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(tmpDestFile);

                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage, null, null);
                        GraphicTools.saveImageToDisk(fanartImage, tmpDestFileName);
                    } else {
                        movie.setFanartFilename(Movie.UNKNOWN);
                        movie.setFanartURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.finer("FanartScanner: Failed to download fanart : " + movie.getFanartURL() + " removing from movie details");
                    movie.setFanartFilename(Movie.UNKNOWN);
                    movie.setFanartURL(Movie.UNKNOWN);
                }
            } else {
                logger.finest("FanartScanner: Fanart exists for " + movie.getBaseFilename());
            }
        }
    }

    /**
     * Get the Fanart for the movie from TheMovieDB.org
     * 
     * @author Stuart.Boston
     * @param movie
     *            The movie bean to get the fanart for
     * @return A string URL pointing to the fanart
     */
    public static String getFanartURL(Movie movie) {
        String API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
        String language = PropertiesUtil.getProperty("themoviedb.language", "en");
        String imdbID = null;
        String tmdbID = null;
        TheMovieDb TMDb;
        MovieDB moviedb = null;

        TMDb = new TheMovieDb(API_KEY);

        imdbID = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        tmdbID = movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID);

        if (StringTools.isValidString(tmdbID)) {
            moviedb = TMDb.moviedbGetInfo(tmdbID, language);
        } else if (StringTools.isValidString(imdbID)) {
            // The ImdbLookup contains images
            moviedb = TMDb.moviedbImdbLookup(imdbID, language);
        } else {
            List<MovieDB> movieList = TMDb.moviedbSearch(movie.getOriginalTitle(), language);
            moviedb = TheMovieDb.findMovie(movieList, movie.getTitle(), movie.getYear());
        }

        // Check that the returned movie isn't null
        if (moviedb == null) {
            logger.finer("FanartScanner: Error getting fanart from TheMovieDB.org for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
        
        try {
            List<Artwork> artworkList = moviedb.getArtwork(Artwork.ARTWORK_TYPE_BACKDROP, Artwork.ARTWORK_SIZE_ORIGINAL);

            if (artworkList == null || artworkList.isEmpty()) {
                logger.finer("FanartScanner: Error no fanart found from TheMovieDB.org for " + movie.getBaseFilename());
                return Movie.UNKNOWN;
            }
            
            for (Artwork fanartArtwork : artworkList) {
                IImage imageFanart = new Image(fanartArtwork.getUrl());
                if (validateArtwork(imageFanart, artworkWidth, artworkHeight, true)) {
                    movie.setDirtyFanart(true);
                    return fanartArtwork.getUrl();
                } else {
                    logger.finest("FanartScanner: Skipped invalid artwork " + fanartArtwork.getUrl());
                }
            }
        } catch (Exception error) {
            logger.severe("FanartScanner: TheMovieDB.org API Error: " + error.getMessage());
            return Movie.UNKNOWN;
        }
        return Movie.UNKNOWN;
    }

    /**
     * Checks for older fanart property in case the skin hasn't been updated.
     * TODO: Remove this procedure at some point
     * 
     * @return true if the fanart is to be downloaded, or false otherwise
     */
    public static boolean checkDownloadFanart(boolean isTvShow) {
        String fanartDownloadProperty = null;
        boolean downloadFanart = false;

        if (isTvShow) {
            fanartDownloadProperty = "fanart.tv.download";
        } else {
            fanartDownloadProperty = "fanart.movie.download";
        }

        String fanartDownloadValue = PropertiesUtil.getProperty(fanartDownloadProperty, null);

        // If this is null, then the property wasn't found, so look for the original
        if (fanartDownloadValue == null) {
            logger.severe("The property moviedb.fanart.download needs to be changed to 'fanart.tv.download' AND 'fanart.movie.download' ");
            downloadFanart = PropertiesUtil.getBooleanProperty("moviedb.fanart.download", "false");
        } else {
            try {
                downloadFanart = Boolean.parseBoolean(fanartDownloadValue);
            } catch (Exception ignore) {
                logger.severe("FanartScanner: Error with fanart property '" + fanartDownloadProperty + "' value, should be true or false and not '"
                        + fanartDownloadValue + "'");
                downloadFanart = false;
            }
        }

        return downloadFanart;
    }

    public static boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect) {
        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader)readers.next();
        int urlWidth = 0, urlHeight = 0;
        float urlAspect;

        if (!artworkValidate) {
            return true;
        }

        if (StringTools.isNotValidString(artworkImage.getUrl())) {
            return false;
        }

        try {
            URL url = new URL(artworkImage.getUrl());
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);

            if (in != null) {
                in.close();
            }
            
            if (iis != null) {
                iis.close();
            }
        } catch (IOException error) {
            logger.finest("FanartScanner: ValidateFanart error: " + error.getMessage() + ": can't open url");
            return false; // Quit and return a false fanart
        }

        urlAspect = (float)urlWidth / (float)urlHeight;

        if (checkAspect && urlAspect < 1.0) {
            logger.finest("FanartScanner: ValidateFanart " + artworkImage + " rejected: URL is portrait format");
            return false;
        }

        // Adjust fanart width / height by the ValidateMatch figure
        artworkWidth = artworkWidth * (artworkValidateMatch / 100);
        artworkHeight = artworkHeight * (artworkValidateMatch / 100);

        if (urlWidth < artworkWidth) {
            logger.finest("FanartScanner: " + artworkImage + " rejected: URL width (" + urlWidth + ") is smaller than fanart width (" + artworkWidth + ")");
            return false;
        }

        if (urlHeight < artworkHeight) {
            logger.finest("FanartScanner: " + artworkImage + " rejected: URL height (" + urlHeight + ") is smaller than fanart height (" + artworkHeight + ")");
            return false;
        }
        return true;
    }

}
