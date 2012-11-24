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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.downloader.Downloader;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class TrailerPlugin implements ITrailerPlugin {

    private static final Logger logger = Logger.getLogger(TrailerPlugin.class);
    protected String LOG_MESSAGE = "TrailerPlugin: ";
    protected WebBrowser webBrowser;
    protected String trailersPluginName = "Abstract";
    private static String trailersScanerPath = PropertiesUtil.getProperty("trailers.path.scaner", "");
    private static String trailersPlayerPath = PropertiesUtil.getProperty("trailers.path.player", "");
    private static boolean trailersDownload = PropertiesUtil.getBooleanProperty("trailers.download", FALSE);
    private static boolean trailersSafeFilename = PropertiesUtil.getBooleanProperty("trailers.safeFilename", FALSE);
    private static boolean trailersOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceTrailersOverwrite", FALSE);
    private static boolean trailersShowProgress = PropertiesUtil.getBooleanProperty("trailers.showProgress", TRUE);
    private static boolean trailersScanHdOnly = PropertiesUtil.getBooleanProperty("trailers.scanHdOnly", FALSE);
    // Resolutions Available
    protected static final String RESOLUTION_1080P = "1080p";
    protected static final String RESOLUTION_720P = "720p";
    protected static final String RESOLUTION_SD = "sd";

    public TrailerPlugin() {
        webBrowser = new WebBrowser();
    }

    @Override
    public boolean generate(Movie movie) {
        return Boolean.FALSE;
    }

    @Override
    public String getName() {
        return "abstract";
    }

    public String getScanerPath() {
        return trailersScanerPath;
    }

    public String getPlayerPath() {
        return trailersPlayerPath;
    }

    public boolean isDownload() {
        return trailersDownload;
    }

    public boolean isSafeFilename() {
        return trailersSafeFilename;
    }

    public boolean isOverwrite() {
        return trailersOverwrite;
    }

    public static boolean isScanHdOnly() {
        return trailersScanHdOnly;
    }

    /**
     * Should trailers be searched for the video?
     *
     * @param movie
     * @return
     */
    public boolean isScanForTrailer(Movie movie) {
        if (trailersScanHdOnly && !movie.isHD()) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public boolean downloadTrailer(Movie movie, String trailerUrl, String title, MovieFile tmf) {
        if (!trailersDownload) {
            return Boolean.FALSE;
        }
        boolean isExchangeOk = Boolean.FALSE;

        // Copied from AppleTrailersPlugin.java
        MovieFile mf = movie.getFirstFile();
        String parentPath = mf.getFile().getParent();
        String name = mf.getFile().getName();
        String basename;

        if (mf.getFilename().toUpperCase().endsWith("/VIDEO_TS")) {
            parentPath += File.separator + name;
            basename = name;
        } else if (mf.getFile().getAbsolutePath().toUpperCase().contains("BDMV")) {
            parentPath = new String(parentPath.substring(0, parentPath.toUpperCase().indexOf("BDMV") - 1));
            basename = new String(parentPath.substring(parentPath.lastIndexOf(File.separator) + 1));
        } else {
            int index = name.lastIndexOf('.');
            basename = index == -1 ? name : new String(name.substring(0, index));
        }

        if (StringTools.isValidString(trailersScanerPath)) {
            parentPath = trailersScanerPath;
            (new File(parentPath)).mkdirs();
        }

        String trailerExt = FilenameUtils.getExtension(trailerUrl);
        String trailerBasename = basename + ".[TRAILER-" + title + "]." + trailerExt;
        if (trailersSafeFilename) {
            trailerBasename = FileTools.makeSafeFilename(trailerBasename);
        }
        String trailerFileName = parentPath + File.separator + trailerBasename;

        int slash = mf.getFilename().lastIndexOf("/");
        String playPath = slash == -1 ? mf.getFilename() : new String(mf.getFilename().substring(0, slash));
        if (StringTools.isValidString(trailersPlayerPath)) {
            playPath = trailersPlayerPath;
        }
        String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);

        logger.debug(LOG_MESSAGE + "Found trailer: " + trailerUrl);          // XXX DEBUG
        logger.debug(LOG_MESSAGE + "Download path: " + trailerFileName);     // XXX DEBUG
        logger.debug(LOG_MESSAGE + "     Play URL: " + trailerPlayFileName); // XXX DEBUG
        File trailerFile = new File(trailerFileName);

        // Check if the file already exists - after jukebox directory was deleted for example
        if (trailerFile.exists()) {
            logger.debug(LOG_MESSAGE + "Trailer file (" + trailerPlayFileName + ") already exists for " + movie.getBaseName());
            tmf.setFilename(trailerPlayFileName);
            movie.addExtraFile(new ExtraFile(tmf));
            isExchangeOk = Boolean.TRUE;
        } else if (trailerDownload(movie, trailerUrl, trailerFile)) {
            tmf.setFilename(trailerPlayFileName);
            movie.addExtraFile(new ExtraFile(tmf));
            isExchangeOk = Boolean.TRUE;
        }

        movie.setTrailerExchange(Boolean.TRUE);

        return isExchangeOk;
    }

    public boolean existsTrailerFiles(Movie movie) {
        boolean fileExists = Boolean.TRUE;
        if (!movie.getExtraFiles().isEmpty() && trailersDownload) {
            String trailersPath = (StringUtils.isNotBlank(trailersScanerPath)) ? trailersScanerPath : movie.getFirstFile().getFile().getParent();
            for (ExtraFile extraFile : movie.getExtraFiles()) {
                File trailerFile = new File(trailersPath + "/" + HTMLTools.decodeUrl(new File(extraFile.getFilename()).getName()));
                fileExists &= trailerFile.exists();
                if (!fileExists) {
                    break;
                }
            }
        }
        return fileExists;
    }

    /**
     * Download the trailer into a local file.
     *
     * @param movie
     * @param trailerUrlString
     * @param trailerFile
     * @return Doe
     */
    public boolean trailerDownload(final IMovieBasicInformation movie, String trailerUrlString, File trailerFile) {
        ThreadExecutor.enterIO(trailerUrlString);
        try {
            logger.debug(LOG_MESSAGE + "Attempting to download URL " + trailerUrlString + ", saving to " + trailerFile.getAbsolutePath());

            Downloader dl = new Downloader(trailerFile.getAbsolutePath(), trailerUrlString, trailersShowProgress);

            if (dl.isDownloadOk()) {
                logger.info("Trailer downloaded in " + dl.getDownloadTime());

                // Looks like it was downloaded OK
                return Boolean.TRUE;
            }
            // Looks like the download failed for some reason
            return Boolean.FALSE;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

}
