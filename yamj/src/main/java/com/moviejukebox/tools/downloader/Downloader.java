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
package com.moviejukebox.tools.downloader;

import com.moviejukebox.tools.WebBrowser;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Logger;

/**
 * This is the downloader class.
 *
 * It will display the download progress of the file being processed.
 *
 * Taken from http://stackoverflow.com/a/11068356/443283
 *
 * @author stuart.boston
 */
public final class Downloader implements RBCWrapperDelegate {

    private static final Logger LOGGER = Logger.getLogger(Downloader.class);
    private static final String logMessage = "Downloader: ";
    private static final String FORMAT_PERCENTAGE = "Download progress %,d Kb received, %.02f%%";
    private static final String FORMAT_NOPER = "Download progress %,d Kb received";
    private boolean showProgress = Boolean.TRUE;
    private boolean downloadOk;
    private long downloadTime;
    public static final String USER_AGENT_APPLE = "QuickTime/7.6.2";
    public static final String USER_AGENT_NORMAL = "Mozilla/4.0 (compatible)";

    public Downloader(String localPath, String remoteURL, boolean showProgress) {
        this.showProgress = showProgress;
        this.downloadOk = Boolean.FALSE;
        this.downloadTime = 0L;
        // The time the download started
        long startTime = System.currentTimeMillis();

        FileOutputStream fos = null;
        ReadableByteChannel rbc = null;
        URL url;

        try {
            url = new URL(remoteURL);
            int contentLength = contentLength(url);

            if (contentLength < 0) {
                LOGGER.warn(logMessage + "WARNING: Remote URL is not a file! " + remoteURL);
                return;
            }

            WebBrowser wb = new WebBrowser();
            HttpURLConnection connection = (HttpURLConnection) wb.openProxiedConnection(url);

            if (remoteURL.toLowerCase().contains(".apple.")) {
                LOGGER.debug(logMessage + "Using Apple user agent - '" + USER_AGENT_APPLE + "'");
                connection.setRequestProperty("User-Agent", USER_AGENT_APPLE);
            } else {
                LOGGER.debug(logMessage + "Using normal user agent - '" + USER_AGENT_NORMAL + "'");
                connection.setRequestProperty("User-Agent", USER_AGENT_NORMAL);
            }

            rbc = new RBCWrapper(Channels.newChannel(connection.getInputStream()), contentLength, this);
//            rbc = new RBCWrapper(Channels.newChannel(url.openStream()), contentLength, this);
            fos = new FileOutputStream(localPath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            fos.flush();
        } catch (MalformedURLException ex) {
            LOGGER.debug(logMessage + "Failed to transform URL: " + ex.getMessage());
        } catch (IOException ex) {
            LOGGER.debug(logMessage + "Output error: " + ex.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                LOGGER.trace(logMessage + "Failed to close output stream: " + ex.getMessage());
            }

            try {
                if (rbc != null) {
                    rbc.close();
                }
            } catch (IOException ex) {
                LOGGER.trace(logMessage + "Failed to close ReadableByteChannel: " + ex.getMessage());
            }
        }

        downloadOk = Boolean.TRUE;

        // Calculate the download time.
        downloadTime = System.currentTimeMillis() - startTime;
    }

    /**
     * Format the download time
     *
     * @return
     */
    public String getDownloadTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(downloadTime));
    }

    @Override
    public void rbcProgressCallback(RBCWrapper rbc, double progress) {
        if (showProgress) {
            if (progress > 0) {
                System.out.println(String.format(FORMAT_PERCENTAGE, rbc.getKbReadSoFar(), progress));
            } else {
                System.out.println(String.format(FORMAT_NOPER, rbc.getKbReadSoFar()));
            }
        }
    }

    /**
     * Get the content length from the header
     *
     * @param url
     * @return
     */
    private int contentLength(URL url) {
        HttpURLConnection connection;
        int contentLength = -1;

        try {
            HttpURLConnection.setFollowRedirects(false);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            contentLength = connection.getContentLength();
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.trace(logMessage + "Failed to get length from header: " + ex.getMessage());
        }

        return contentLength;
    }

    public boolean isDownloadOk() {
        return downloadOk;
    }
}
