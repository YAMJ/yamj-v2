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
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class AppleTrailersPlugin extends TrailerPlugin {

    private static final Logger logger = Logger.getLogger(AppleTrailersPlugin.class);
    private static String configResolution = PropertiesUtil.getProperty("appletrailers.resolution", "");
    private static boolean configDownload = PropertiesUtil.getBooleanProperty("appletrailers.download", Boolean.FALSE);
    private static String configTrailerTypes = PropertiesUtil.getProperty("appletrailers.trailertypes", "tlr,clip,tsr,30sec,640w");
    private static int configMax = PropertiesUtil.getIntProperty("appletrailers.max", 0);
    private static boolean configTypesInclude = PropertiesUtil.getBooleanProperty("appletrailers.typesinclude", Boolean.TRUE);
    private static String configReplaceUrl = PropertiesUtil.getProperty("appletrailers.replaceurl", "www.apple.com");
    private static final String EXTENSIONS = "mov|m4v";
    private static final String SUBDOMAINS = "movies|images|trailers";
    private static final Pattern TRAILER_URL_PATTERN = Pattern.compile("http://(" + SUBDOMAINS + ").apple.com/movies/[^\\\"]+\\.(" + EXTENSIONS + ")");
    private static final String[] RESOLUTION_ARRAY = {"1080p", "720p", "480p", "640", "480"};

    public AppleTrailersPlugin() {
        super();
        trailersPluginName = "AppleTrailers";
        LOG_MESSAGE = "AppleTrailersPlugin: ";
    }

    @Override
    public final boolean generate(Movie movie) {
        // Check if trailer resolution was selected
        if (configResolution.equals("")) {
            return false;
        }

        String movieName = movie.getOriginalTitle();

        String trailerPageUrl = getTrailerPageUrl(movieName);

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        if (Movie.UNKNOWN.equalsIgnoreCase(trailerPageUrl)) {
            logger.debug(LOG_MESSAGE + "Trailer not found for " + movie.getBaseName());
            return false;
        }

        LinkedHashSet<String> trailersUrl = new LinkedHashSet<String>();
        LinkedHashSet<String> bestTrailersUrl = new LinkedHashSet<String>();

        getTrailerSubUrl(trailerPageUrl, trailersUrl);

        selectBestTrailer(trailersUrl, bestTrailersUrl);

        int trailerDownloadCnt = 0;

        if (bestTrailersUrl.isEmpty()) {
            logger.debug(LOG_MESSAGE + "No trailers found for " + movie.getBaseName());
            return false;
        }

        boolean isExchangeOk = false;

        for (String trailerRealUrl : bestTrailersUrl) {

            if (trailerDownloadCnt >= configMax) {
                logger.debug(LOG_MESSAGE + "Downloaded maximum of " + configMax + (configMax == 1 ? " trailer" : " trailers"));
                break;
            }

            // Add the trailer URL to the movie
            ExtraFile extra = new ExtraFile();
            extra.setTitle("TRAILER-" + getTrailerTitle(trailerRealUrl));

            // Is the found trailer one of the types to download/link to?
            if (!isValidTrailer(getFilenameFromUrl(trailerRealUrl))) {
                logger.debug(LOG_MESSAGE + "Trailer skipped: " + getFilenameFromUrl(trailerRealUrl));
                continue; // Quit the rest of the trailer loop.
            }

            // Issue with the naming of URL for trailer download
            // See: http://www.hd-trailers.net/blog/how-to-download-hd-trailers-from-apple/
            trailerRealUrl = trailerRealUrl.replace("www.apple.com", configReplaceUrl);
            trailerRealUrl = trailerRealUrl.replace("images.apple.com", configReplaceUrl);
            trailerRealUrl = trailerRealUrl.replace("movies.apple.com", configReplaceUrl);

            logger.debug(LOG_MESSAGE + "Trailer found for " + movie.getBaseName() + " (" + getFilenameFromUrl(trailerRealUrl) + ")");
            trailerDownloadCnt++;

            // Check if we need to download the trailer, or just link to it
            if (configDownload) {
                String trailerAppleName = getFilenameFromUrl(trailerRealUrl);
                trailerAppleName = trailerAppleName.substring(0, trailerAppleName.lastIndexOf('.'));
                isExchangeOk = downloadTrailer(movie, trailerRealUrl, trailerAppleName, extra);
            } else {
                // Just link to the trailer
                int underscore = trailerRealUrl.lastIndexOf('_');
                if (underscore > 0 && trailerRealUrl.substring(underscore + 1, underscore + 2).equals("h")) {
                    // remove the "h" from the trailer URL for streaming
                    trailerRealUrl = trailerRealUrl.substring(0, underscore + 1) + trailerRealUrl.substring(underscore + 2);
                }
                extra.setFilename(trailerRealUrl);
                movie.addExtraFile(extra);
                isExchangeOk = true;
            }
        }

        return isExchangeOk;
    }

    @Override
    public String getName() {
        return "apple";
    }

    private String getTrailerPageUrl(String movieName) {
        String doubleQuoteComma = "\",";
        String titleKey = "\"title\":\"";
        String locationKey = "\"location\":\"";

        try {
            String searchURL = "http://trailers.apple.com/trailers/home/scripts/quickfind.php?callback=searchCallback&q="
                    + URLEncoder.encode(movieName, "UTF-8");

            String xml = webBrowser.request(searchURL);

            int index = 0;
            int endIndex;
            while (true) {
                index = xml.indexOf(titleKey, index);
                if (index == -1) {
                    break;
                }

                index += titleKey.length();

                endIndex = xml.indexOf(doubleQuoteComma, index);
                if (endIndex == -1) {
                    break;
                }

                String trailerTitle = decodeEscapeICU(new String(xml.substring(index, endIndex)));

                index = endIndex + doubleQuoteComma.length();

                index = xml.indexOf(locationKey, index);
                if (index == -1) {
                    break;
                }

                index += locationKey.length();

                endIndex = xml.indexOf(doubleQuoteComma, index);
                if (endIndex == -1) {
                    break;
                }

                String trailerLocation = decodeEscapeICU(new String(xml.substring(index, endIndex)));

                index = endIndex + doubleQuoteComma.length();

                if (trailerTitle.equalsIgnoreCase(movieName)) {
                    String trailerUrl;

                    int itmsIndex = trailerLocation.indexOf("itms://");
                    if (itmsIndex == -1) {
                        // Convert relative URL to absolute URL - some urls are already absolute, and some relative
                        trailerUrl = getAbsUrl("http://www.apple.com/trailers/", trailerLocation);
                    } else {
                        trailerUrl = "http" + new String(trailerLocation.substring(itmsIndex + 4));
                    }

                    return trailerUrl;
                }
            }

        } catch (IOException error) {
            logger.error(LOG_MESSAGE + "Failed retreiving trailer for movie : " + movieName);
            logger.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }

        return Movie.UNKNOWN;
    }

    private void getTrailerSubUrl(String trailerPageUrl, Set<String> trailersUrl) {
        try {

            String xml = webBrowser.request(trailerPageUrl);

            // Try to find the movie link on the main page
            getTrailerMovieUrl(xml, trailersUrl);

            // New URL
            String trailerPageUrlNew = trailerPageUrl.replace("//www.apple.com/", "//trailers.apple.com/");

            String trailerPageUrlWebInc = getAbsUrl(trailerPageUrlNew, "includes/playlists/web.inc");
            String xmlWebInc = getSubPage(trailerPageUrlWebInc);
            // Try to find the movie link on the WebInc page
            getTrailerMovieUrl(xmlWebInc, trailersUrl);

            // Search for the 'HD' Page
            String trailerPageUrlHD = getAbsUrl(trailerPageUrlNew, "hd");
            String xmlHD = getSubPage(trailerPageUrlHD);

            // Only search if the HD URL is valid
            if (StringTools.isValidString(xmlHD)) {
                // Try to find the movie link on the HD page
                getTrailerMovieUrl(xmlHD, trailersUrl);

                String trailerPageUrlHDWebInc = getAbsUrl(trailerPageUrlHD, "includes/playlists/web.inc");
                String xmlHDWebInc = getSubPage(trailerPageUrlHDWebInc);
                // Try to find the movie link on the WebInc HD page
                getTrailerMovieUrl(xmlHDWebInc, trailersUrl);
            } else {
                logger.debug(LOG_MESSAGE + "No valid HD URL found for " + trailerPageUrl);
            }
        } catch (IOException ex) {
            logger.error(LOG_MESSAGE + "Error : " + ex.getMessage());
            logger.error(SystemTools.getStackTrace(ex));
        }
    }

    // Get sub page url - if error return empty page
    private String getSubPage(String url) {
        Level oldlevel = logger.getLevel();

        try {
            // Don't log error getting URL
            logger.setLevel(Level.OFF);
            return webBrowser.request(url);
        } catch (Exception error) {
            return "";
        } finally {
            logger.setLevel(oldlevel);
        }
    }

    private void getTrailerMovieUrl(String xml, Set<String> trailersUrl) {
        Matcher m = TRAILER_URL_PATTERN.matcher(xml);
        while (m.find()) {
            String movieUrl = m.group();
            trailersUrl.add(movieUrl);
        }
    }

    private void selectBestTrailer(Set<String> trailersUrl, Set<String> bestTrailersUrl) {

        boolean startSearch = false;

        for (String resolution : RESOLUTION_ARRAY) {
            if (configResolution.equals(resolution)) {
                startSearch = true;
            }
            if (startSearch) {
                for (String curURL : trailersUrl) {
                    // Search for a specific resolution
                    if (curURL.indexOf(resolution) != -1) {
                        addTailerRealUrl(bestTrailersUrl, curURL);
                    }
                }
            }

            if (!bestTrailersUrl.isEmpty()) {
                break;
            }
        }
    }

    private void addTailerRealUrl(Set<String> bestTrailersUrl, String trailerUrl) {
        String trailerRealUrl = getTrailerRealUrl(trailerUrl);
        bestTrailersUrl.add(trailerRealUrl);
    }

    private String getTrailerRealUrl(String trailerUrl) {
        try {
            URL url = new URL(trailerUrl);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
            InputStream inputStream = connection.getInputStream();

            byte buf[] = new byte[1024];
            int len;
            len = inputStream.read(buf);

            // Check if too much data read, that this is the real url already
            if (len == 1024) {
                return trailerUrl;
            }

            String mov = new String(buf);

            int pos = 44;
            StringBuilder realUrl = new StringBuilder();

            while (mov.charAt(pos) != 0) {
                realUrl.append(mov.charAt(pos));

                pos++;
            }

            String absRealURL = getAbsUrl(trailerUrl, realUrl.toString());

            return absRealURL;

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            logger.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
    }

    private String getTrailerTitle(String url) {
        int start = url.lastIndexOf('/');
        int end = url.indexOf(".mov", start);

        if ((start == -1) || (end == -1)) {
            return Movie.UNKNOWN;
        }

        StringBuilder title = new StringBuilder();

        for (int i = start + 1; i < end; i++) {
            if ((url.charAt(i) == '-') || (url.charAt(i) == '_')) {
                title.append(' ');
            } else {
                if (i == start + 1) {
                    title.append(Character.toUpperCase(url.charAt(i)));
                } else {
                    title.append(url.charAt(i));
                }
            }
        }

        return title.toString();
    }

    private String getAbsUrl(String baseUrl, String relativeUrl) {
        try {
            URL baseURL = new URL(baseUrl);
            URL absURL = new URL(baseURL, relativeUrl);
            return absURL.toString();
        } catch (Exception error) {
            return Movie.UNKNOWN;
        }
    }

    private String decodeEscapeICU(String str) {
        StringBuilder newString = new StringBuilder();

        int loop = 0;
        while (loop < str.length()) {
            // Check ICU escaping
            if ((str.charAt(loop) == '%') && (loop + 5 < str.length()) && (str.charAt(loop + 1) == 'u')) {

                String value = new String(str.substring(loop + 2, loop + 6));
                int intValue = Integer.parseInt(value, 16);

                // fix for ' char
                if (intValue == 0x2019) {
                    intValue = 0x0027;
                }

                char c = (char) intValue;

                newString.append(c);
                loop += 6;
            } else {
                if (str.charAt(loop) == '\\') {
                    loop++;
                } else {
                    newString.append(str.charAt(loop));
                    loop++;
                }
            }
        }

        return newString.toString();
    }

    // Extract the filename from the URL
    private String getFilenameFromUrl(String fullUrl) {
        int nameStart = fullUrl.lastIndexOf('/') + 1;
        return new String(fullUrl.substring(nameStart));
    }

    // Check the trailer filename against the valid trailer types from appletrailers.trailertypes
    private boolean isValidTrailer(String trailerFilename) {
        boolean validTrailer;

        if (configTypesInclude) {
            validTrailer = false;
        } else {
            validTrailer = true;
        }

        for (String ttype : configTrailerTypes.split(",")) {
            if (trailerFilename.lastIndexOf(ttype) > 0) {
                if (configTypesInclude) {
                    // Found the trailer type, so this is a valid trailer
                    validTrailer = true;
                } else {
                    // Found the trailer type, so this trailer should be excluded
                    validTrailer = false;
                }
                break;
            }
        }

        return validTrailer;
    }
}
