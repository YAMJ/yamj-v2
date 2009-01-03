/**
 *  Fanart Scanner
 *
 * Routines for locating and downloading Fanart for videos
 *
 */
package com.moviejukebox.scanner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.plugin.MovieImagePlugin;
import java.io.FileInputStream;

/**
 * Scanner for fanart files in local directory
 * 
 * @author  stuart.boston
 * @version 1.0, 10th December 2008
 */
public class FanartScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");

    protected static String[] fanartExtensions;
    protected static String fanartToken;
    protected static boolean fanartOverwrite;

    static {

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("fanart.scanner.fanartExtensions", "jpg,jpeg,gif,bmp,png"), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        fanartExtensions = extensions.toArray(new String[] {});
        
        fanartToken = PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart");
        
        fanartOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceFanartOverwrite", "false"));
    }

    public static void scan(MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        String localFanartBaseFilename = movie.getBaseName();
        String fullFanartFilename = null;
        String foundExtension = null;
        File localFanartFile = null;
        boolean foundLocalFanart = false;
        
        // Look for the videoname.fanartToken.Extension
        if (movie.getFile().isDirectory()) { // for VIDEO_TS
            fullFanartFilename = movie.getFile().getPath();
        } else {
            fullFanartFilename = movie.getFile().getParent();
        }

        fullFanartFilename += File.separator + localFanartBaseFilename + fanartToken;
        foundExtension = findFanartFile(fullFanartFilename, fanartExtensions);
        if(!foundExtension.equals("")) {
            // The filename and extension was found
            fullFanartFilename += foundExtension;

            localFanartFile = new File(fullFanartFilename); // Double check it still works
            if (localFanartFile.exists()) {
                logger.finest("FanartScanner: File " + fullFanartFilename + " found");
                foundLocalFanart = true;
            }
        }
        
        // if no fanart has been found, try the foldername.fanartToken.Extension
        if (!foundLocalFanart) {
            localFanartBaseFilename = movie.getFile().getParent();
            localFanartBaseFilename = localFanartBaseFilename.substring(localFanartBaseFilename.lastIndexOf("\\") + 1);

            // Checking for the MovieFolderName.*
            fullFanartFilename = movie.getFile().getParent() + File.separator + localFanartBaseFilename + fanartToken;
            foundExtension = findFanartFile(fullFanartFilename, fanartExtensions);
            if (!foundExtension.equals("")) {
                // The filename and extension was found
                fullFanartFilename += foundExtension;
                localFanartFile = new File(fullFanartFilename);
                if (localFanartFile.exists()) {
                    logger.finest("FanartScanner: File " + fullFanartFilename + " found");
                    foundLocalFanart = true;
                }
            }
        }
        
        // If we've found the fanart, copy it to the jukebox, otherwise download it.
        if (foundLocalFanart) {
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + movie.getFanartFilename();
            String destFileName = tempJukeboxDetailsRoot + File.separator + movie.getFanartFilename();

            File finalDestinationFile = new File(finalDestinationFileName);
            File fullFanartFile = new File(fullFanartFilename);

            // Local Fanart is newer OR ForceFanartOverwrite OR DirtyFanart
            // Can't check the file size because the jukebox fanart may have been re-sized
            // This may mean that the local art is different to the jukebox art even if the local file date is newer
            if ( FileTools.isNewer(fullFanartFile, finalDestinationFile) || fanartOverwrite || movie.isDirtyFanart() ) {
                try {
                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(new FileInputStream(fullFanartFile));
                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage);
                        GraphicTools.saveImageToDisk(fanartImage, destFileName);
                        logger.finer("FanartScanner: " + fullFanartFilename + " has been copied to " + destFileName);
                    } else {
                        movie.setFanartFilename(Movie.UNKNOWN);
                        movie.setFanartURL(Movie.UNKNOWN);
                    }
                } catch (Exception e) {
                    logger.finer("FanartScanner: Failed loading fanart : " + fullFanartFilename);
                }
            } else {
                logger.finer("FanartScanner: " + finalDestinationFileName + " already exists");
            }
        } else {
            //logger.finer("FanartScanner : No local Fanart found for " + movie.getBaseName() + " attempting to download");
            downloadFanart(backgroundPlugin, jukeboxDetailsRoot, tempJukeboxDetailsRoot, movie);
        }
    }
    
    private static void downloadFanart(MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            String fanartFilename = jukeboxDetailsRoot + File.separator + movie.getFanartFilename();
            File fanartFile = new File(fanartFilename);
            String tmpDestFileName = tempJukeboxDetailsRoot + File.separator + movie.getFanartFilename();
            File tmpDestFile = new File(tmpDestFileName);

            // Do not overwrite existing fanart unless ForceFanartOverwrite = true
            if ( (!fanartFile.exists() && !tmpDestFile.exists()) || fanartOverwrite ) {
                fanartFile.getParentFile().mkdirs();

                try {
                    logger.finest("Fanart Scanner: Downloading fanart for " + movie.getBaseName() + " to " + tmpDestFileName + " [calling plugin]");

                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(movie.getFanartURL());

                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage);
                        GraphicTools.saveImageToDisk(fanartImage, tmpDestFileName);
                    } else {
                        movie.setFanartFilename(Movie.UNKNOWN);
                        movie.setFanartURL(Movie.UNKNOWN);
                    }
                } catch (Exception e) {
                    logger.finer("Fanart Scanner: Failed to download fanart : " + movie.getFanartURL());
                }
            } else {
                logger.finest("Fanart Scanner: Fanart exists for " + movie.getBaseName());
            }
        }
    }
    
    /***
     * Pass in the filename and a list of extensions,
     * this function will scan for the filename plus extensions
     * and return the extension
     * 
     * @param   filename
     * @param   extensions
     * @return  extension of fanart that was found
     */
    private static String findFanartFile(String fullFanartFilename, String[] fanartExtensions) {
        File localFanartFile;
        boolean foundLocalFanart = false;
        
        for (String extension : fanartExtensions) {
            localFanartFile = new File(fullFanartFilename + "." + extension);
            if (localFanartFile.exists()) {
                logger.finest("The file " + fullFanartFilename + "." + extension + " exists");
                fullFanartFilename = "." + extension;
                foundLocalFanart = true;
                break;
            }
        }

        // If we've found the filename with extension, return it, otherwise return ""
        if (foundLocalFanart) {
            return fullFanartFilename;
        } else {
            return "";
        }
    }
}

