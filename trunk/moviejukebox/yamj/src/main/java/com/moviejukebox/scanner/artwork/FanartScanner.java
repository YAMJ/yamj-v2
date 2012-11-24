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
 * FanartScanner
 *
 * Routines for locating and downloading Fanart for videos
 *
 */
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Series;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Scanner for fanart files in local directory
 *
 * @author Stuart.Boston
 * @version 1.0, 10th December 2008 - Initial code
 * @version 1.1, 19th July 2009 - Added Internet search
 */
public class FanartScanner {

    private static final Logger logger = Logger.getLogger(FanartScanner.class);
    private static final String LOG_MESSAGE = "FanartScanner: ";
    private static final Collection<String> fanartExtensions = Collections.synchronizedList(new ArrayList<String>());
    private static String fanartToken;
    private static boolean fanartOverwrite;
    private static final boolean useFolderBackground = PropertiesUtil.getBooleanProperty("fanart.scanner.useFolderImage", FALSE);
    private static final Collection<String> fanartImageName = Collections.synchronizedList(new ArrayList<String>());
    private static boolean artworkValidate;
    private static int artworkValidateMatch;
    private static boolean artworkValidateAspect;
    private static int artworkWidth;
    private static int artworkHeight;
    private static TheMovieDbApi TMDb;
    private static TheTVDBApi tvDB;

    static {

        // We get valid extensions
        synchronized (fanartExtensions) {
            if (fanartExtensions.isEmpty()) {
                StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.fanartExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
                while (st.hasMoreTokens()) {
                    fanartExtensions.add(st.nextToken());
                }
            }
        }

        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        fanartOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFanartOverwrite", FALSE);

        // See if we use background.* or fanart.*
        synchronized (fanartImageName) {
            if (useFolderBackground && fanartImageName.isEmpty()) {
                StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.imageName", "fanart,backdrop,background"), ",;|");
                while (st.hasMoreTokens()) {
                    fanartImageName.add(st.nextToken());
                }
            }
        }

        artworkWidth = PropertiesUtil.getIntProperty("fanart.width", "0");
        artworkHeight = PropertiesUtil.getIntProperty("fanart.height", "0");
        artworkValidate = PropertiesUtil.getBooleanProperty("fanart.scanner.Validate", TRUE);
        artworkValidateMatch = PropertiesUtil.getIntProperty("fanart.scanner.ValidateMatch", "75");
        artworkValidateAspect = PropertiesUtil.getBooleanProperty("fanart.scanner.ValidateAspect", TRUE);

        try {
            TMDb = new TheMovieDbApi(PropertiesUtil.getProperty("API_KEY_TheMovieDB"));
        } catch (MovieDbException ex) {
            logger.warn(LOG_MESSAGE + "Failed to initialise TheMovieDB API. Fanart will not be downloaded.");
            logger.warn(SystemTools.getStackTrace(ex));
            TMDb = null;
        }

        tvDB = new TheTVDBApi(PropertiesUtil.getProperty("API_KEY_TheTvDB"));
    }

    protected FanartScanner() {
        throw new UnsupportedOperationException("FanartScanner is a utility class and cannot be instatiated");
    }

    /**
     * Get the Fanart URL for the movie from the source sites
     *
     * @param movie
     * @return
     */
    public static String getFanartURL(Movie movie) {
        if (movie.isTVShow()) {
            return getTvFanartURL(movie);
        } else {
            return getMovieFanartURL(movie);
        }

    }

    public static boolean scan(MovieImagePlugin backgroundPlugin, Jukebox jukebox, Movie movie) {
        String localFanartBaseFilename = movie.getBaseFilename();
        String parentPath = FileTools.getParentFolder(movie.getFile());

        // Look for the videoname.fanartToken.Extension
        String fullFanartFilename = StringTools.appendToPath(parentPath, localFanartBaseFilename + fanartToken);
        File localFanartFile = FileTools.findFileFromExtensions(fullFanartFilename, fanartExtensions);
        boolean foundLocalFanart = localFanartFile.exists();

        // Try searching the fileCache for the filename.
        if (!foundLocalFanart) {
            localFanartFile = FileTools.findFilenameInCache(localFanartBaseFilename + fanartToken, fanartExtensions, jukebox, LOG_MESSAGE, Boolean.TRUE);
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
            logger.debug(LOG_MESSAGE + "File " + fullFanartFilename + " found");

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
            if (FileTools.isNewer(fullFanartFile, finalDestinationFile) || fanartOverwrite || movie.isDirty(DirtyFlag.FANART)) {
                try {
                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(fullFanartFile);
                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage, "fanart", null);
                        if (PropertiesUtil.getBooleanProperty("fanart.perspective", FALSE)) {
                            destFileName = destFileName.subSequence(0, destFileName.lastIndexOf('.') + 1) + "png";
                            movie.setFanartFilename(destFileName);
                        }
                        GraphicTools.saveImageToDisk(fanartImage, destFileName);
                        logger.debug(LOG_MESSAGE + fullFanartFilename + " has been copied to " + destFileName);

                        ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, fullFanartFilename, false);
                        movie.addArtwork(new com.moviejukebox.model.Artwork.Artwork(ArtworkType.Fanart, "local", fullFanartFilename, artworkFile));

                    } else {
                        movie.setFanartFilename(Movie.UNKNOWN);
                        movie.setFanartURL(Movie.UNKNOWN);
                    }
                } catch (Exception error) {
                    logger.debug(LOG_MESSAGE + "Failed loading fanart : " + fullFanartFilename);
                }
            } else {
                logger.debug(LOG_MESSAGE + finalDestinationFileName + " already exists");
            }
        } else {
            // logger.debug("FanartScanner : No local Fanart found for " + movie.getBaseFilename() + " attempting to download");
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
                    logger.debug(LOG_MESSAGE + "Downloading fanart for " + movie.getBaseFilename() + " to " + tmpDestFileName + " [calling plugin]");

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
                    logger.debug(LOG_MESSAGE + "Failed to download fanart : " + movie.getFanartURL() + " removing from movie details");
                    movie.setFanartFilename(Movie.UNKNOWN);
                    movie.setFanartURL(Movie.UNKNOWN);
                }
            } else {
                logger.debug(LOG_MESSAGE + "Fanart exists for " + movie.getBaseFilename());
            }
        }
    }

    /**
     * Get the Fanart for the movie from TheMovieDB.org
     *
     * @author Stuart.Boston
     * @param movie The movie bean to get the fanart for
     * @return A string URL pointing to the fanart
     */
    private static String getMovieFanartURL(Movie movie) {
        // Unable to scan for fanart because TheMovieDB wasn't initialised
        if (TMDb == null) {
            return Movie.UNKNOWN;
        }

        String language = PropertiesUtil.getProperty("themoviedb.language", "en-US");
        MovieDb moviedb = null;

        String imdbID = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
        String tmdbIDstring = movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID);
        int tmdbID;

        if (StringUtils.isNumeric(tmdbIDstring)) {
            tmdbID = Integer.parseInt(tmdbIDstring);
        } else {
            tmdbID = 0;
        }

        if (tmdbID > 0) {
            try {
                moviedb = TMDb.getMovieInfo(tmdbID, language);
            } catch (MovieDbException ex) {
                logger.debug(LOG_MESSAGE + "Failed to get fanart using TMDB ID: " + tmdbID + " - " + ex.getMessage());
                moviedb = null;
            }
        }

        if (moviedb == null && StringTools.isValidString(imdbID)) {
            try {
                // The ImdbLookup contains images
                moviedb = TMDb.getMovieInfoImdb(imdbID, language);
            } catch (MovieDbException ex) {
                logger.debug(LOG_MESSAGE + "Failed to get fanart using IMDB ID: " + imdbID + " - " + ex.getMessage());
                moviedb = null;
            }
        }

        if (moviedb == null) {
            try {
                List<MovieDb> movieList = TMDb.searchMovie(movie.getOriginalTitle(), language, false);
                for (MovieDb m : movieList) {
                    if (m.getTitle().equals(movie.getTitle())
                            || m.getTitle().equalsIgnoreCase(movie.getOriginalTitle())
                            || m.getOriginalTitle().equalsIgnoreCase(movie.getTitle())
                            || m.getOriginalTitle().equalsIgnoreCase(movie.getOriginalTitle())) {
                        if (StringTools.isNotValidString(movie.getYear())) {
                            // We don't have a year for the movie, so assume this is the correct movie
                            moviedb = m;
                            break;
                        } else if (m.getReleaseDate().contains(movie.getYear())) {
                            // found the movie name and year
                            moviedb = m;
                            break;
                        }
                    }
                }
            } catch (MovieDbException ex) {
                logger.debug(LOG_MESSAGE + "Failed to get fanart using IMDB ID: " + imdbID + " - " + ex.getMessage());
                moviedb = null;
            }
        }

        // Check that the returned movie isn't null
        if (moviedb == null) {
            logger.debug(LOG_MESSAGE + "Error getting fanart from TheMovieDB.org for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }

        try {
            URL fanart = TMDb.createImageUrl(moviedb.getBackdropPath(), "original");
            if (fanart == null) {
                return Movie.UNKNOWN;
            } else {
                return fanart.toString();
            }
        } catch (MovieDbException ex) {
            logger.debug(LOG_MESSAGE + "Error getting fanart from TheMovieDB.org for " + movie.getBaseFilename());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Get the Fanart for the movie from TheTVDb.com
     *
     * @author Stuart.Boston
     * @param movie The movie bean to get the fanart for
     * @return A string URL pointing to the fanart
     */
    private static String getTvFanartURL(Movie movie) {
        String url = null;

        String id = TheTvDBPlugin.findId(movie);

        if (StringTools.isNotValidString(id)) {
            return Movie.UNKNOWN;
        }
        Series series = TheTvDBPlugin.getSeries(id);
        if (series == null) {
            return Movie.UNKNOWN;
        }

        Banners banners = TheTvDBPlugin.getBanners(id);

        if (!banners.getFanartList().isEmpty()) {
            // Pick a fanart that is not likely to be the same as a previous one.
            int index = movie.getSeason();
            if (index < 0) {
                index = 0;
            }

            // Make sure that the index is not more than the list of available banners
            // We may still run into issues, if there are less HD than this number
            index = (index % banners.getFanartList().size());

            Banner bannerSD = null;
            Banner bannerHD = null;
            int countSD = 0;
            int countHD = 0;

            for (Banner banner : banners.getFanartList()) {
                if (banner.getBannerType2() == BannerType.FanartHD) {
                    bannerHD = banner;  // Save the current banner
                    countHD++;
                    if (countHD >= index) {
                        // We have a HD banner, so quit
                        break;
                    }
                } else {
                    // This is a SD banner, So save it in case we can't find a HD one
                    if (countSD <= index) {
                        // Only update the banner if we want a later one
                        bannerSD = banner;
                    }
                }
            }

            if (bannerHD != null) {
                url = bannerHD.getUrl();
            } else if (bannerSD != null) {
                url = bannerSD.getUrl();
            }

        }

        if (StringTools.isNotValidString(url) && StringTools.isValidString(series.getFanart())) {
            url = series.getFanart();
        }

//        if (StringTools.isValidString(movie.getFanartURL())) {
//            String artworkFilename = movie.getBaseName() + fanartToken + "." + fanartExtension;
//            movie.setFanartFilename(artworkFilename);
//        }

        return url;
    }

    public static boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect) {
        @SuppressWarnings("rawtypes")
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader) readers.next();
        int urlWidth, urlHeight;
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
            logger.debug(LOG_MESSAGE + "ValidateFanart error: " + error.getMessage() + ": can't open url");
            return false; // Quit and return a false fanart
        }

        urlAspect = (float) urlWidth / (float) urlHeight;

        if (checkAspect && urlAspect < 1.0) {
            logger.debug(LOG_MESSAGE + "ValidateFanart " + artworkImage + " rejected: URL is portrait format");
            return false;
        }

        // Adjust fanart width / height by the ValidateMatch figure
        int newArtworkWidth = artworkWidth * (artworkValidateMatch / 100);
        int newArtworkHeight = artworkHeight * (artworkValidateMatch / 100);

        if (urlWidth < newArtworkWidth) {
            logger.debug(LOG_MESSAGE + artworkImage + " rejected: URL width (" + urlWidth + ") is smaller than fanart width (" + newArtworkWidth + ")");
            return false;
        }

        if (urlHeight < newArtworkHeight) {
            logger.debug(LOG_MESSAGE + artworkImage + " rejected: URL height (" + urlHeight + ") is smaller than fanart height (" + newArtworkHeight + ")");
            return false;
        }
        return true;
    }
}
