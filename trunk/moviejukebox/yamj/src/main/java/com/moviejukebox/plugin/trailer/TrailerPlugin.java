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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.*;
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
    private static boolean trailersDownload = PropertiesUtil.getBooleanProperty("trailers.download", Boolean.FALSE);
    private static boolean trailersSafeFilename = PropertiesUtil.getBooleanProperty("trailers.safeFilename", Boolean.FALSE);
    private static boolean trailersOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceTrailersOverwrite", Boolean.FALSE);
    private static boolean trailersShowProgress = PropertiesUtil.getBooleanProperty("trailers.showProgress", Boolean.TRUE);
    private static boolean trailersScanHdOnly = PropertiesUtil.getBooleanProperty("trailers.scanHdOnly", Boolean.FALSE);
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

    public boolean downloadTrailer(Movie movie, String trailerUrl, String title, ExtraFile extra) {
        if (!trailersDownload) {
            return Boolean.FALSE;
        }
        boolean isExchangeOk = Boolean.FALSE;

        MovieFile mf = movie.getFirstFile();
        String parentPath = mf.getFile().getParent();
        String name = mf.getFile().getName();
        String basename;

        if (mf.getFilename().toUpperCase().endsWith("/VIDEO_TS")) {
            parentPath += File.separator + name;
            basename = name;
        } else if (mf.getFile().getAbsolutePath().toUpperCase().contains("BDMV")) {
            parentPath = parentPath.substring(0, parentPath.toUpperCase().indexOf("BDMV") - 1);
            basename = parentPath.substring(parentPath.lastIndexOf(File.separator) + 1);
        } else {
            int index = name.lastIndexOf('.');
            basename = index == -1 ? name : name.substring(0, index);
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
        String playPath = slash == -1 ? mf.getFilename() : mf.getFilename().substring(0, slash);
        if (StringTools.isValidString(trailersPlayerPath)) {
            playPath = trailersPlayerPath;
        }
        String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);

        if (logger.isDebugEnabled()) {
            logger.debug(LOG_MESSAGE + "Found trailer: " + trailerUrl);
            logger.debug(LOG_MESSAGE + "Download path: " + trailerFileName);
            logger.debug(LOG_MESSAGE + "     Play URL: " + trailerPlayFileName);
        }
        
        File trailerFile = new File(trailerFileName);

        // Check if the file already exists - after jukebox directory was deleted for example
        if (trailerFile.exists()) {
            logger.debug(LOG_MESSAGE + "Trailer file (" + trailerPlayFileName + ") already exists for " + movie.getBaseName());
            extra.setFilename(trailerPlayFileName);
            movie.addExtraFile(extra);
            isExchangeOk = Boolean.TRUE;
        } else if (trailerDownload(movie, trailerUrl, trailerFile)) {
            extra.setFilename(trailerPlayFileName);
            movie.addExtraFile(extra);
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
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

}
