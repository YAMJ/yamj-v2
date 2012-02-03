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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class TrailersPlugin implements ITrailersPlugin {
    public String trailersPluginName = "AbstractTrailers";

    protected static Logger logger = Logger.getLogger(TrailersPlugin.class);
    protected WebBrowser webBrowser;

    private static String trailersScanerPath = PropertiesUtil.getProperty("trailers.path.scaner", "");
    private static String trailersPlayerPath = PropertiesUtil.getProperty("trailers.path.player", "");
    private static boolean trailersDownload = PropertiesUtil.getBooleanProperty("trailers.download", "false");
    private static boolean trailersSafeFilename = PropertiesUtil.getBooleanProperty("trailers.safeFilename", "false");
    private static boolean trailersOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceTrailersOverwrite", "false");

    public TrailersPlugin() {
        webBrowser = new WebBrowser();
    }

    @Override
    public boolean generate(Movie movie) {
        return false;
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

    public boolean getDownload() {
        return trailersDownload;
    }

    public boolean getSafeFilename() {
        return trailersSafeFilename;
    }

    public boolean getOverwrite() {
        return trailersOverwrite;
    }

    public boolean downloadTrailer(Movie movie, String trailerUrl, String title, MovieFile tmf) {
        if (!trailersDownload) {
            return false;
        }
        boolean isExchangeOk = false;

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
            int index = name.lastIndexOf(".");
            basename = index == -1 ? name : new String(name.substring(0, index));
        }
        if (StringTools.isValidString(trailersScanerPath)) {
            parentPath = trailersScanerPath;
            (new File(parentPath)).mkdirs();
        }

        String trailerExt = new String(trailerUrl.substring(trailerUrl.lastIndexOf(".")));
        String trailerBasename = basename + ".[TRAILER-" + title + "]" + trailerExt;
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

        logger.debug(trailersPluginName + " Plugin: Found trailer: " + trailerUrl);
        logger.debug(trailersPluginName + " Plugin: Download path: " + trailerFileName);
        logger.debug(trailersPluginName + " Plugin:      Play URL: " + trailerPlayFileName);
        File trailerFile = new File(trailerFileName);

        // Check if the file already exists - after jukebox directory was deleted for example
        if (trailerFile.exists()) {
            logger.debug(trailersPluginName + " Plugin: Trailer file (" + trailerPlayFileName + ") already exist for " + movie.getBaseName());
            tmf.setFilename(trailerPlayFileName);
            movie.addExtraFile(new ExtraFile(tmf));
            isExchangeOk = true;
        } else if (trailerDownload(movie, trailerUrl, trailerFile)) {
            tmf.setFilename(trailerPlayFileName);
            movie.addExtraFile(new ExtraFile(tmf));
            isExchangeOk = true;
        }

        movie.setTrailerExchange(true);

        return isExchangeOk;
    }

    public boolean existsTrailerFiles(Movie movie) {
        boolean fileExists = true;
        if (!movie.getExtraFiles().isEmpty() && trailersDownload) {
            String trailersPath = (trailersScanerPath != "")?trailersScanerPath:movie.getFirstFile().getFile().getParent();
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

    public boolean trailerDownload(final IMovieBasicInformation movie, String trailerUrl, File trailerFile) {
        // Copied from AppleTrailersPlugin.java
        URL url;
        try {
            url = new URL(trailerUrl);
        } catch (MalformedURLException e) {
            return false;
        }

        ThreadExecutor.enterIO(url);
        HttpURLConnection connection = null;
        Timer timer = new Timer();
        try {
            logger.info(trailersPluginName + " Plugin: Download trailer for " + movie.getBaseName());
            final WebStats stats = WebStats.make(url);
            // after make!
            timer.schedule(new TimerTask() {
                private String lastStatus = "";
                public void run() {
                    String status = stats.calculatePercentageComplete();
                    // only print if percentage changed
                    if (status.equals(lastStatus)) {
                        return;
                    }
                    lastStatus = status;
                    // this runs in a thread, so there is no way to output on one line...
                    // try to keep it visible at least...
                    System.out.println("Downloading trailer for " + movie.getTitle() + ": " + stats.statusString());
                }
            }, 1000, 1000);

            connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("User-Agent", "QuickTime/7.6.9");
            InputStream inputStream = connection.getInputStream();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                logger.error(trailersPluginName + " Plugin: Download Failed");
                return false;
            }

            FileTools.copy(inputStream, new FileOutputStream(trailerFile), stats);
            System.out.println("Downloading trailer for " + movie.getTitle() + ": " + stats.statusString()); // Output the final stat information (100%)

            return true;
        } catch (Exception error) {
            logger.error(trailersPluginName + " Plugin: Download Exception");
            return false;
        } finally {
            timer.cancel();         // Close the timer
            if(connection != null){
                connection.disconnect();
            }
            ThreadExecutor.leaveIO();
        }
    }
}
