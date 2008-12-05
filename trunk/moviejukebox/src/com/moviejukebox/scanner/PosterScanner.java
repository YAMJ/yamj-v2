/**
 * 
 */
package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Scanner for poster files in local directory
 * 
 * @author groll.troll
 * @version 1.0, 7 oct. 2008
 */
public class PosterScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");

    protected String[] coverArtExtensions;
    protected String searchForExistingCoverArt;
    protected String fixedCoverArtName;
    protected String coverArtDirectory;
    protected Boolean useFolderImage;

    public PosterScanner() {
        // We get covert art scanner behaviour
        searchForExistingCoverArt = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", "moviename");
        // We get the fixed name property
        fixedCoverArtName = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
        // Stuart.Boston: See if we user folder.* or not
        useFolderImage = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.useFolderImage", "true"));

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.coverArtExtensions", ""), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        coverArtExtensions = extensions.toArray(new String[] {});

        // We get coverart Directory if needed
        coverArtDirectory = PropertiesUtil.getProperty("poster.scanner.coverArtDirectory", "");
    }

    public void scan(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {

        if (searchForExistingCoverArt.equalsIgnoreCase("no")) {
            // nothing to do we return
            return;
        }

        String localPosterBaseFilename = Movie.UNKNOWN;
        String fullPosterFilename = null;
        File localPosterFile = null;

        if (searchForExistingCoverArt.equalsIgnoreCase("moviename")) {
            localPosterBaseFilename = movie.getBaseName();
        } else if (searchForExistingCoverArt.equalsIgnoreCase("fixedcoverartname")) {
            localPosterBaseFilename = fixedCoverArtName;
        } else {
            logger.fine("Wrong value for poster.scanner.searchForExistingCoverArt properties !");
            return;
        }

        boolean foundLocalCoverArt = false;

        for (String extension : coverArtExtensions) {
            if (movie.getFile().isDirectory()) { // for VIDEO_TS
                fullPosterFilename = movie.getFile().getPath();
            } else {
                fullPosterFilename = movie.getFile().getParent();
            }
            if (!coverArtDirectory.equals("")) {
                fullPosterFilename += File.separator + coverArtDirectory;
            }
            fullPosterFilename += File.separator + localPosterBaseFilename + "." + extension;
            // logger.finest("Checking for "+ fullPosterFilename);
            localPosterFile = new File(fullPosterFilename);
            if (localPosterFile.exists()) {
                logger.finest("The file " + fullPosterFilename + " exists");
                foundLocalCoverArt = true;
                break;
            }
        }

        /***
         * This part will look for a filename with the same name as the directory for the cover art or for folder.* coverart The intention is for you to be able
         * to create the season / Tv series art for the whole series and not for the first show. useful if you change the files regularly
         * 
         * @author Stuart.Boston
         * @version 1.0
         * @date 18th October 2008
         */
        if (!foundLocalCoverArt) {
            // if no coverart has been found, try the foldername
            // no need to check the coverart directory
            localPosterBaseFilename = movie.getFile().getParent();
            localPosterBaseFilename = localPosterBaseFilename.substring(localPosterBaseFilename.lastIndexOf("\\") + 1);

            if (useFolderImage) {
                // Checking for MovieFolderName.* AND folder.*
                logger.finest("Checking for '" + localPosterBaseFilename + ".*' coverart AND folder.* coverart");
            } else {
                // Only checking for the MovieFolderName.* and not folder.*
                logger.finest("Checking for '" + localPosterBaseFilename + ".*' coverart");
            }

            for (String extension : coverArtExtensions) {
                // Check for the directory name with extension for coverart
                fullPosterFilename = movie.getFile().getParent() + File.separator + localPosterBaseFilename + "." + extension;
                localPosterFile = new File(fullPosterFilename);
                if (localPosterFile.exists()) {
                    logger.finest("The file " + fullPosterFilename + " found");
                    foundLocalCoverArt = true;
                    break;
                }

                if (useFolderImage) {
                    // logger.finest("Checking for 'folder.*' coverart");
                    // Check for folder.jpg if it exists
                    fullPosterFilename = movie.getFile().getParent() + File.separator + "folder." + extension;
                    localPosterFile = new File(fullPosterFilename);
                    if (localPosterFile.exists()) {
                        logger.finest("The file " + fullPosterFilename + " found");
                        foundLocalCoverArt = true;
                        break;
                    }
                }
            }
        }
        /***
                    * END OF Folder CoverArt
                    */

        if (foundLocalCoverArt) {
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + movie.getPosterFilename();
            String destFileName = tempJukeboxDetailsRoot + File.separator + movie.getPosterFilename();

            File finalDestinationFile = new File(finalDestinationFileName);
            File destFile = new File(destFileName);
            boolean checkAgain = false;
            
            if ( ( finalDestinationFile.length() != localPosterFile.length() ) ||
                 ( finalDestinationFile.lastModified() < localPosterFile.lastModified() ) ){
                // Poster size is different OR Local Poster is newer
                checkAgain = true;
            }
            
            if (!finalDestinationFile.exists() || checkAgain) {
                FileTools.copyFile(localPosterFile, destFile);
                logger.finer("PosterScanner : " + fullPosterFilename + " has been copied to " + destFileName);
            } else {
                logger.finer("PosterScanner : " + finalDestinationFileName + " is different to " + fullPosterFilename);
            }
        } else {
            logger.finer("PosterScanner : No local covertArt found for " + movie.getBaseName());
        }
    }
}
