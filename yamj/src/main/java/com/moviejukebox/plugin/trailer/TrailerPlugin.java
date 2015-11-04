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
package com.moviejukebox.plugin.trailer;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.YamjHttpClient;
import com.moviejukebox.tools.YamjHttpClientBuilder;
import com.moviejukebox.tools.downloader.Downloader;

public class TrailerPlugin implements ITrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerPlugin.class);
    protected YamjHttpClient httpClient;
    protected String trailersPluginName = "Abstract";
    private static final String SCANNER_PATH = PropertiesUtil.getProperty("trailers.path.scaner", "");
    private static final String PLAYER_PATH = PropertiesUtil.getProperty("trailers.path.player", "");
    private static final boolean DOWNLOAD = PropertiesUtil.getBooleanProperty("trailers.download", Boolean.FALSE);
    private static final boolean SAFE_FILENAME = PropertiesUtil.getBooleanProperty("trailers.safeFilename", Boolean.FALSE);
    private static final boolean OVERWRITE = PropertiesUtil.getBooleanProperty("mjb.forceTrailersOverwrite", Boolean.FALSE);
    private static final boolean SHOW_PROGRESS = PropertiesUtil.getBooleanProperty("trailers.showProgress", Boolean.TRUE);
    private static final boolean SCAN_LOCAL_ONLY = PropertiesUtil.getBooleanProperty("trailers.scanHdOnly", Boolean.FALSE);
    // Resolutions Available
    protected static final String RESOLUTION_1080P = "1080p";
    protected static final String RESOLUTION_720P = "720p";
    protected static final String RESOLUTION_SD = "sd";

    public TrailerPlugin() {
        httpClient = YamjHttpClientBuilder.getHttpClient();
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
        return SCANNER_PATH;
    }

    public String getPlayerPath() {
        return PLAYER_PATH;
    }

    public boolean isDownload() {
        return DOWNLOAD;
    }

    public boolean isSafeFilename() {
        return SAFE_FILENAME;
    }

    public boolean isOverwrite() {
        return OVERWRITE;
    }

    public static boolean isScanHdOnly() {
        return SCAN_LOCAL_ONLY;
    }

    /**
     * Should trailers be searched for the video?
     *
     * @param movie
     * @return
     */
    public boolean isScanForTrailer(Movie movie) {
        if (SCAN_LOCAL_ONLY && !movie.isHD()) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public boolean downloadTrailer(Movie movie, String trailerUrl, String title, ExtraFile extra) {
        if (!DOWNLOAD) {
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

        if (StringTools.isValidString(SCANNER_PATH)) {
            parentPath = SCANNER_PATH;
            FileTools.makeDirs(new File(parentPath));
        }

        String trailerExt = FilenameUtils.getExtension(trailerUrl);
        String trailerBasename = basename + ".[TRAILER-" + title + "]." + trailerExt;
        if (SAFE_FILENAME) {
            trailerBasename = FileTools.makeSafeFilename(trailerBasename);
        }
        String trailerFileName = parentPath + File.separator + trailerBasename;

        int slash = mf.getFilename().lastIndexOf("/");
        String playPath = slash == -1 ? mf.getFilename() : mf.getFilename().substring(0, slash);
        if (StringTools.isValidString(PLAYER_PATH)) {
            playPath = PLAYER_PATH;
        }
        String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found trailer: {}", trailerUrl);
            LOG.debug("Download path: {}", trailerFileName);
            LOG.debug("     Play URL: {}", trailerPlayFileName);
        }

        File trailerFile = new File(trailerFileName);

        // Check if the file already exists - after jukebox directory was deleted for example
        if (trailerFile.exists()) {
            LOG.debug("Trailer file ({}) already exists for {}", trailerPlayFileName, movie.getBaseName());
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
        if (!movie.getExtraFiles().isEmpty() && DOWNLOAD) {
            String trailersPath = (StringUtils.isNotBlank(SCANNER_PATH)) ? SCANNER_PATH : movie.getFirstFile().getFile().getParent();
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
            LOG.debug("{} - Attempting to download URL '{}', saving to {}", movie.getTitle(), trailerUrlString, trailerFile.getAbsolutePath());

            Downloader dl = new Downloader(trailerFile.getAbsolutePath(), trailerUrlString, SHOW_PROGRESS);

            if (dl.isDownloadOk()) {
                LOG.info("Trailer downloaded in {}", dl.getDownloadTime());
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

}
