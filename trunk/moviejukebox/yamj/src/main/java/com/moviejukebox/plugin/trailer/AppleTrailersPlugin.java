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
package com.moviejukebox.plugin.trailer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.WebStats;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.FileTools;

public class AppleTrailersPlugin {

    private static Logger logger = Logger.getLogger("moviejukebox");

    private static String  configResolution   = PropertiesUtil.getProperty("appletrailers.resolution", "");
    private static boolean configDownload     = PropertiesUtil.getBooleanProperty("appletrailers.download", "false");
    private static String  configTrailerTypes = PropertiesUtil.getProperty("appletrailers.trailertypes", "tlr,clip,tsr,30sec,640w");
    private static int     configMax;
    private static boolean configTypesInclude = PropertiesUtil.getBooleanProperty("appletrailers.typesinclude", "true");
    private static String  configReplaceUrl   = PropertiesUtil.getProperty("appletrailers.replaceurl", "www.apple.com");

    static {
        try {
            configMax = PropertiesUtil.getIntProperty("appletrailers.max", "0");
        } catch (Exception ignored) {
            configMax = 0;
        }
    };

    protected WebBrowser webBrowser;

    public AppleTrailersPlugin() {
        webBrowser = new WebBrowser();
    }

    public final boolean generate(Movie movie) {

        // Check if trailer resolution was selected
        if (configResolution.equals("")) {
            return false;
        }
        
        String movieName = movie.getOriginalTitle();

        String trailerPageUrl = getTrailerPageUrl(movieName);

        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now

        if (trailerPageUrl == Movie.UNKNOWN) {
            logger.finer("AppleTrailers Plugin: Trailer not found for " + movie.getBaseName());
            return false;
        }

        LinkedHashSet<String> trailersUrl     = new LinkedHashSet<String>();
        LinkedHashSet<String> bestTrailersUrl = new LinkedHashSet<String>();

        getTrailerSubUrl(trailerPageUrl, trailersUrl);

        selectBestTrailer(trailersUrl, bestTrailersUrl);

        int trailerDownloadCnt = 0;

        if (bestTrailersUrl.isEmpty()) {
            logger.finest("AppleTrailers Plugin: No trailers found for " + movie.getBaseName());
            return false;
        }

        boolean isExchangeOk = false;

        for (String trailerRealUrl : bestTrailersUrl) {

            if (trailerDownloadCnt >= configMax) {
                logger.finest("AppleTrailers Plugin: Downloaded maximum of " + configMax + (configMax == 1 ? " trailer" : " trailers"));
                break;
            }

            // Add the trailer URL to the movie
            MovieFile tmf = new MovieFile();
            tmf.setTitle("TRAILER-" + getTrailerTitle(trailerRealUrl));
            
            // Is the found trailer one of the types to download/link to?
            if (!isValidTrailer(getFilenameFromUrl(trailerRealUrl))) {
                logger.finer("AppleTrailers Plugin: Trailer skipped: " + getFilenameFromUrl(trailerRealUrl));
                continue;           // Quit the rest of the trailer loop.
            }
            
            // Issue with the naming of URL for trailer download
            // See: http://www.hd-trailers.net/blog/how-to-download-hd-trailers-from-apple/
            trailerRealUrl = trailerRealUrl.replace("www.apple.com", configReplaceUrl);
            trailerRealUrl = trailerRealUrl.replace("images.apple.com", configReplaceUrl);
            trailerRealUrl = trailerRealUrl.replace("movies.apple.com", configReplaceUrl);
            
            logger.finer("AppleTrailers Plugin: Trailer found for " + movie.getBaseName() + " (" + getFilenameFromUrl(trailerRealUrl) + ")");
            trailerDownloadCnt++;
            
            // Check if we need to download the trailer, or just link to it
            if (configDownload) {
                // Download the trailer
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
                
                String trailerAppleName = getFilenameFromUrl(trailerRealUrl);
                String trailerAppleExt  = trailerAppleName.substring(trailerAppleName.lastIndexOf('.'));
                trailerAppleName        = trailerAppleName.substring(0, trailerAppleName.lastIndexOf('.'));
                String trailerBasename  = FileTools.makeSafeFilename(basename + ".[TRAILER-" + trailerAppleName + "]" + trailerAppleExt);
                String trailerFileName  = parentPath + File.separator + trailerBasename;

                int slash                  = mf.getFilename().lastIndexOf('/');
                String playPath            = slash == -1 ? mf.getFilename() : mf.getFilename().substring(0, slash);
                String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);
                
                logger.finest("AppleTrailers Plugin: Found trailer: " + trailerRealUrl);
                logger.finest("AppleTrailers Plugin: Download path: " + trailerFileName);
                logger.finest("AppleTrailers Plugin:      Play URL: " + trailerPlayFileName);
                
                File trailerFile = new File(trailerFileName);
                
                // Check if the file already exists - after jukebox directory was deleted for example
                if (trailerFile.exists()) {
                    logger.finer("AppleTrailers Plugin: Trailer file (" + trailerPlayFileName + ") already exist for " + movie.getBaseName());
                
                    tmf.setFilename(trailerPlayFileName);
                    movie.addExtraFile(new ExtraFile(tmf));
                    isExchangeOk = true;
                } else if (trailerDownload(movie, trailerRealUrl, trailerFile)) {
                    tmf.setFilename(trailerPlayFileName);
                    movie.addExtraFile(new ExtraFile(tmf));
                    isExchangeOk = true;
                }
            } else {
                // Just link to the trailer
                int underscore = trailerRealUrl.lastIndexOf('_');
                if (underscore > 0 && trailerRealUrl.substring(underscore + 1, underscore + 2).equals("h")) {
                    // remove the "h" from the trailer url for streaming
                    trailerRealUrl = trailerRealUrl.substring(0, underscore + 1) + trailerRealUrl.substring(underscore + 2);
                }
                tmf.setFilename(trailerRealUrl);
                movie.addExtraFile(new ExtraFile(tmf));
                isExchangeOk = true;
            }
        }
        
        return isExchangeOk;
    }
    
    private String getTrailerPageUrl(String movieName) {
        String doubleQuoteComma = "\",";
        String titleKey         = "\"title\":\"";
        String locationKey      = "\"location\":\"";

        try {
            String searchURL = "http://trailers.apple.com/trailers/home/scripts/quickfind.php?callback=searchCallback&q="
                    + URLEncoder.encode(movieName, "UTF-8");

            String xml = webBrowser.request(searchURL);

            int index = 0;
            int endIndex = 0;
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

                String trailerTitle = decodeEscapeICU(xml.substring(index, endIndex));

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

                String trailerLocation = decodeEscapeICU( xml.substring(index, endIndex) );

                index = endIndex + doubleQuoteComma.length();
                
                if (trailerTitle.equalsIgnoreCase(movieName)) {
                    String trailerUrl;
                    
                    int itmsIndex = trailerLocation.indexOf("itms://");
                    if (itmsIndex == -1) {
                        // Convert relative URL to absolute URL - some urls are already absolute, and some relative
                        trailerUrl = getAbsUrl("http://www.apple.com/trailers/" , trailerLocation);
                    }
                    else {
                        trailerUrl = "http" + trailerLocation.substring(itmsIndex+4);
                    }
                    
                    return trailerUrl;
                }
            }

        } catch (Exception error) {
            logger.severe("AppleTrailers Plugin: Failed retreiving trailer for movie : " + movieName);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
            String trailerPageUrlNew = trailerPageUrl.replace("//www.apple.com/","//trailers.apple.com/");

            String trailerPageUrlHD = getAbsUrl(trailerPageUrlNew, "hd");
            String xmlHD = getSubPage(trailerPageUrlHD);
            // Try to find the movie link on the HD page
            getTrailerMovieUrl(xmlHD, trailersUrl);

            String trailerPageUrlWebInc = getAbsUrl(trailerPageUrlNew, "includes/playlists/web.inc");
            String xmlWebInc = getSubPage(trailerPageUrlWebInc);
            // Try to find the movie link on the WebInc page
            getTrailerMovieUrl(xmlWebInc, trailersUrl);

            String trailerPageUrlHDWebInc = getAbsUrl(trailerPageUrlNew, "hd/includes/playlists/web.inc");
            String xmlHDWebInc = getSubPage(trailerPageUrlHDWebInc);
            // Try to find the movie link on the WebInc HD page
            getTrailerMovieUrl(xmlHDWebInc, trailersUrl);

            // // Go over the href links and check the sub pages
            // 
            // int index = 0;
            // int endIndex = 0;
            // while (true) {
            //     index = xml.indexOf("href=\"", index);
            //     if (index == -1) {
            //         break;
            //     }
            // 
            //     index += 6;
            // 
            //     endIndex = xml.indexOf("\"", index);
            //     if (endIndex == -1) {
            //         break;
            //     }
            // 
            //     String href = xml.substring(index, endIndex);
            // 
            //     index = endIndex + 1;
            //     
            //     String absHref = getAbsUrl(trailerPageUrl, href);
            //     
            //     // Check if this href is a sub page of this trailer
            //     if (absHref.startsWith(trailerPageUrl)) {
            // 
            //         String subXml = getSubPage(absHref);
            //         
            //         // Try to find the movie link on the sub page
            //         getTrailerMovieUrl(subXml, trailersUrl);
            //     }
            // }

        } catch (Exception error) {
            logger.severe("AppleTrailers Plugin: Error : " + error.getMessage());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return;
        }
    }

    // Get sub page url - if error return empty page
    private String getSubPage(String url) {
    
        String ret = "";
        Level oldlevel = logger.getLevel();

        try {
            // Don't log error getting URL
            logger.setLevel(Level.OFF);
            ret = webBrowser.request(url);
            logger.setLevel(oldlevel);
            return ret;
        } catch (Exception error) {
            logger.setLevel(oldlevel);
            return ret;
        }
    }

    private void getTrailerMovieUrl(String xml, Set<String> trailersUrl) {
        Matcher m = Pattern.compile("http://(movies|images|trailers).apple.com/movies/[^\"]+?-(tlr|trailer)[^\"]+?\\.(mov|m4v)").matcher(xml);
        while (m.find()) {
            String movieUrl = m.group();
            trailersUrl.add(movieUrl);
        }
    }

    private void selectBestTrailer(Set<String> trailersUrl, Set<String> bestTrailersUrl) {

        String[] resolutionArray = { "1080p", "720p", "480p", "640", "480" };
        boolean startSearch = false;

        for (String resolution : resolutionArray) {
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
            if (len==1024) {
                return trailerUrl;
            }
        
            String mov = new String(buf);

            int pos = 44;        
            StringBuffer realUrl = new StringBuffer();
            
            while (mov.charAt(pos)!=0) {
                realUrl.append(mov.charAt(pos));
                
                pos++;
            }
            
            String absRealURL = getAbsUrl(trailerUrl, realUrl.toString());
            
            return absRealURL;
            
        } catch (Exception error) {
            logger.severe("AppleTrailers Plugin: Error : " + error.getMessage());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return Movie.UNKNOWN;
        }
    }

    private String getTrailerTitle(String url) {
        int start = url.lastIndexOf('/');
        int end = url.indexOf(".mov",start);
        
        if ((start == -1) || (end == -1)) {
            return Movie.UNKNOWN;
        }
            
        StringBuffer title = new StringBuffer();
        
        for (int i=start+1;i<end;i++) {
            if ((url.charAt(i) == '-') || (url.charAt(i) == '_')) {
                title.append(' ');
            } else {
                if (i == start+1) {
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
        StringBuffer newString = new StringBuffer();

        int loop = 0;
        while (loop < str.length()) {
            // Check ICU escaping
            if ((str.charAt(loop) == '%') && (loop+5 < str.length()) && (str.charAt(loop+1) == 'u')) {

                String value=str.substring(loop+2,loop+6);
                int intValue = Integer.parseInt(value,16);
                
                // fix for ' char
                if (intValue == 0x2019) {
                    intValue = 0x0027;
                }
                
                char c = (char)intValue;

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

    private boolean trailerDownload(final IMovieBasicInformation movie, String trailerUrl, File trailerFile) {
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
            logger.fine("AppleTrailers Plugin: Download trailer for " + movie.getBaseName());
            final WebStats stats = WebStats.make(url);
            final long reportDelay = 1000; // 1 second
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
            }, reportDelay, reportDelay);

            connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("User-Agent", "QuickTime/7.6.2");
            InputStream inputStream = connection.getInputStream();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                logger.severe("AppleTrailers Plugin: Download Failed");
                return false;
            }

            FileTools.copy(inputStream, new FileOutputStream(trailerFile), stats);
            System.out.println("Downloading trailer for " + movie.getTitle() + ": " + stats.statusString()); // Output the final stat information (100%)

            return true;

        } catch (Exception error) {
            logger.severe("AppleTrailers Plugin: Download Exception: " + error);
            return false;
        } finally {
            timer.cancel();         // Close the timer
            if(connection != null){
                connection.disconnect();
            }
            ThreadExecutor.leaveIO();
        }
    }
 
    // Extract the filename from the URL
    private String getFilenameFromUrl(String fullUrl) {
        int nameStart = fullUrl.lastIndexOf('/') + 1;
        return fullUrl.substring(nameStart);
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
