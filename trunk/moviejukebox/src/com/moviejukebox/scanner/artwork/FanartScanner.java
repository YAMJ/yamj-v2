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
 *  FanartScanner
 *
 * Routines for locating and downloading Fanart for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.Artwork;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

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

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.fanartExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        while (st.hasMoreTokens()) {
            fanartExtensions.add(st.nextToken());
        }

        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");

        fanartOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceFanartOverwrite", "false"));
        
        // See if we use background.* or fanart.*
        useFolderBackground = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.scanner.useFolderImage", "false"));
        if (useFolderBackground) {
            st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.imageName", "fanart,backdrop,background"), ",;|");
            fanartImageName = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                fanartImageName.add(st.nextToken());
            }
        }

    }

    public static boolean scan(MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie) {
        String localFanartBaseFilename = movie.getBaseFilename();
        String fullFanartFilename = null;
        String parentPath = FileTools.getParentFolder(movie.getFile());
        File localFanartFile = null;
        boolean foundLocalFanart = false;

        // Look for the videoname.fanartToken.Extension
        fullFanartFilename = parentPath + File.separator + localFanartBaseFilename + fanartToken;
        localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
        foundLocalFanart = localFanartFile.exists();

        // if no fanart has been found, try the foldername.fanartToken.Extension
        if (!foundLocalFanart) {
            localFanartBaseFilename = FileTools.getParentFolderName(movie.getFile());

            // Checking for the MovieFolderName.*
            fullFanartFilename = parentPath + File.separator + localFanartBaseFilename + fanartToken;
            localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
            foundLocalFanart = localFanartFile.exists();
        }

        // Check for fanart.* and background.* fanart.
        if (!foundLocalFanart && useFolderBackground) {
            // Check for each of the farnartImageName.* files
            for (String fanartFilename : fanartImageName) {
                fullFanartFilename = parentPath + File.separator + fanartFilename;
                localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
                foundLocalFanart = localFanartFile.exists();

                if (!foundLocalFanart && movie.isTVShow()) {
                    // Get the parent directory and check that
                    fullFanartFilename = FileTools.getParentFolder(movie.getFile().getParentFile().getParentFile()) + File.separator + fanartFilename;
                    System.out.println("SCANNER: " + fullFanartFilename);
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

            if (movie.getFanartFilename().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setFanartFilename(movie.getBaseFilename() + fanartToken + "." + FileTools.getFileExtension(localFanartFile.getName()));
            }
            if (movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setFanartURL(localFanartFile.toURI().toString());
            }
            String fanartFilename = movie.getFanartFilename();
            String finalDestinationFileName = jukebox.getJukeboxRootLocationDetails() + File.separator + fanartFilename;
            String destFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + fanartFilename;

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
                        if (Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.perspective", "false"))) {
                            destFileName = destFileName.subSequence(0, destFileName.lastIndexOf(".") + 1) + "png";
                            movie.setFanartFilename(destFileName);
                        }
                        GraphicTools.saveImageToDisk(fanartImage, destFileName);
                        logger.finer("FanartScanner: " + fullFanartFilename + " has been copied to " + destFileName);
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
        if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            String safeFanartFilename = movie.getFanartFilename();
            String fanartFilename = jukebox.getJukeboxRootLocationDetails() + File.separator + safeFanartFilename;
            File fanartFile = FileTools.fileCache.getFile(fanartFilename);
            String tmpDestFileName = jukebox.getJukeboxTempLocationDetails() + File.separator + safeFanartFilename;
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

        //TODO move these Ids to a preferences file.
        imdbID = movie.getId("imdb");
        tmdbID = movie.getId("themoviedb");

        if (FileTools.isValidString(tmdbID)) {
            moviedb = TMDb.moviedbGetInfo(tmdbID, language);
        } else if (FileTools.isValidString(imdbID)) {
            // The ImdbLookup contains images
            moviedb = TMDb.moviedbImdbLookup(imdbID, language);
        } else {
            List<MovieDB> movieList = TMDb.moviedbSearch(movie.getOriginalTitle(), language);
            moviedb = TheMovieDb.findMovie(movieList, movie.getTitle(), movie.getYear());
        }

        // Check that the returned movie bean isn't null
        if (moviedb == null) {
            logger.finer("FanartScanner: Error getting fanart for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
        try {
            Artwork fanartArtwork = moviedb.getFirstArtwork(Artwork.ARTWORK_TYPE_BACKDROP, Artwork.ARTWORK_SIZE_ORIGINAL);
            if (fanartArtwork == null || fanartArtwork.getUrl() == null || fanartArtwork.getUrl().equalsIgnoreCase(MovieDB.UNKNOWN)) {
                logger.finer("FanartScanner: Error no fanart found for " + movie.getBaseFilename());
                return Movie.UNKNOWN;
            } else {
                movie.setDirtyFanart(true);
                return fanartArtwork.getUrl();
            }
        } catch (Exception error) {
            logger.severe("PosterScanner: TheMovieDB.org API Error: " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Checks for older fanart property in case the skin hasn't been updated. 
     * TODO: Remove this procedure at some point
     * 
     * @return true if the fanart is to be downloaded, or false otherwise
     */
    public static boolean checkDownloadFanart(boolean isTvShow) {
        String fanartProperty = null;
        boolean downloadFanart = false;

        if (isTvShow) {
            fanartProperty = PropertiesUtil.getProperty("fanart.tv.download", null);
        } else {
            fanartProperty = PropertiesUtil.getProperty("fanart.movie.download", null);
        }
        

        // If this is null, then the property wasn't found, so look for the original
        if (fanartProperty == null) {
            logger.severe("The property moviedb.fanart.download needs to be changed to 'fanart.tv.download' AND 'fanart.movie.download' ");
            downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));
        } else {
            try {
                downloadFanart = Boolean.parseBoolean(fanartProperty);
            } catch (Exception ignore) {
                logger.severe("ImdbPlugin: Error with fanart property, should be true/false and not '" + fanartProperty + "'");
                downloadFanart = false;
            }
        }

        return downloadFanart;
    }
}