/**
 * Scanner for posters.
 * Includes local searches (scan) and Internet Searches
 */
package com.moviejukebox.scanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

/**
 * Scanner for poster files in local directory
 * 
 * @author groll.troll
 * @version 1.0, 7 oct. 2008
 */
public class PosterScanner {

    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected static String[]   coverArtExtensions;
    protected static String     searchForExistingCoverArt;
    protected static String     fixedCoverArtName;
    protected static String     coverArtDirectory;
    protected static Boolean    useFolderImage;
    protected static WebBrowser webBrowser;
    protected static String     preferredPosterSearchEngine;
    protected static String     posterSearchPriority;
    protected static boolean    posterValidate;
    protected static boolean    posterValidateAspect;
    protected static int        posterWidth;
    protected static int        posterHeight;

    static {
        // We get covert art scanner behaviour
        searchForExistingCoverArt = PropertiesUtil.getProperty("poster.scanner.searchForExistingCoverArt", "moviename");
        // We get the fixed name property
        fixedCoverArtName = PropertiesUtil.getProperty("poster.scanner.fixedCoverArtName", "folder");
        // See if we use folder.* or not
        useFolderImage = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.useFolderImage", "true"));

        // We get valid extensions
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("poster.scanner.coverArtExtensions", ""), ",;| ");
        Collection<String> extensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            extensions.add(st.nextToken());
        }
        coverArtExtensions = extensions.toArray(new String[]{});

        // We get coverart Directory if needed
        coverArtDirectory = PropertiesUtil.getProperty("poster.scanner.coverArtDirectory", "");

        webBrowser = new WebBrowser();
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");
        posterWidth                 = Integer.parseInt(PropertiesUtil.getProperty("posters.width", "0"));
        posterHeight                = Integer.parseInt(PropertiesUtil.getProperty("posters.height", "0"));
        posterSearchPriority        = PropertiesUtil.getProperty("poster.scanner.SearchPriority", "imdb,motechnet,impawards,moviedb,moviecovers,google,yahoo");
        posterValidate              = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.Validate", "true"));
        posterValidateAspect        = Boolean.parseBoolean(PropertiesUtil.getProperty("poster.scanner.ValidateAspect", "true"));
    }
    
    public static boolean scan(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
        if (searchForExistingCoverArt.equalsIgnoreCase("no")) {
            // nothing to do we return
            return false;
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
            return false;
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
            logger.finest("Checking for "+ fullPosterFilename);
            localPosterFile = new File(fullPosterFilename);
            if (localPosterFile.exists()) {
                logger.finest("The file " + fullPosterFilename + " exists");
                foundLocalCoverArt = true;
                break;
            }
        }

        /**
         * This part will look for a filename with the same name as the directory for the 
         * cover art or for folder.* coverart The intention is for you to be able
         * to create the season / TV series art for the whole series and not for the first show. 
         * Useful if you change the files regularly.
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
        /*
         * END OF Folder CoverArt
         */

        if (foundLocalCoverArt) {
            String safePosterFilename = FileTools.makeSafeFilename(movie.getPosterFilename());
            String finalDestinationFileName = jukeboxDetailsRoot + File.separator + safePosterFilename;
            String destFileName = tempJukeboxDetailsRoot + File.separator + safePosterFilename;

            File finalDestinationFile = new File(finalDestinationFileName);
            File destFile = new File(destFileName);
            boolean checkAgain = false;

            // Overwrite the jukebox files if the local file is newer
            // First check the temp jukebox file
            if (localPosterFile.exists() && destFile.exists()) {
                if (FileTools.isNewer(localPosterFile, destFile)) {
                    checkAgain = true;
                }
            } else if (localPosterFile.exists() && finalDestinationFile.exists()) {
                // Check the target jukebox file
                if (FileTools.isNewer(localPosterFile, finalDestinationFile)) {
                    checkAgain = true;
                }
            }

            if ((localPosterFile.length() != finalDestinationFile.length()) ||
                    (FileTools.isNewer(localPosterFile, finalDestinationFile))) {
                // Poster size is different OR Local Poster is newer
                checkAgain = true;
            }

            if (!finalDestinationFile.exists() || checkAgain) {
                FileTools.copyFile(localPosterFile, destFile);
                logger.finer("PosterScanner : " + fullPosterFilename + " has been copied to " + destFileName);
            } else {
                logger.finer("PosterScanner : " + finalDestinationFileName + " is different to " + fullPosterFilename);
            }
            
            // Update poster url with local poster
            movie.setPosterURL(localPosterFile.toURI().toString());
            return true;
        } else {
            logger.finer("PosterScanner : No local covertArt found for " + movie.getBaseName());
            return false;
        }
    }

    /**
     * Locate the PosterURL from the Internet.
     * This is the main method and should be called instead of the individual getPosterFrom* methods.
     * 
     * @param movie     The movieBean to search for
     * @param imdbXML   The IMDb XML page (for the IMDb poster search)
     * @return          The posterURL that was found (Maybe Movie.UNKNOWN)
     * 
     * TODO Move this to PosterScanner.java
     */
    public static String getPosterURL(Movie movie, String imdbXML, String pluginID) {
        String posterSearchToken;
        String posterURL = Movie.UNKNOWN;
        StringTokenizer st;

        st = new StringTokenizer(posterSearchPriority, ",");
        
        while (st.hasMoreTokens() && posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
            posterSearchToken = st.nextToken();
            
            if (posterSearchToken.equalsIgnoreCase("google")) {
                posterURL = PosterScanner.getPosterURLFromGoogle(movie.getTitle());
            } else if (posterSearchToken.equalsIgnoreCase("yahoo")) {
                posterURL = PosterScanner.getPosterURLFromYahoo(movie.getTitle());
            } else if (posterSearchToken.equalsIgnoreCase("motechnet")) {
                posterURL = PosterScanner.getPosterURLFromMotechnet(movie.getId(pluginID));
            } else if (posterSearchToken.equalsIgnoreCase("impawards")) {
                posterURL = PosterScanner.getPosterURLFromImpAwards(movie.getTitle(), movie.getYear());
            } else if (posterSearchToken.equalsIgnoreCase("moviecovers")) {
                posterURL = PosterScanner.getPosterURLFromMovieCovers(movie.getTitle());
            } else if (posterSearchToken.equalsIgnoreCase("moviedb")) {
                posterURL = PosterScanner.getPosterURLFromMoviedb(movie.getTitle());
            } else if (posterSearchToken.equalsIgnoreCase("imdb")) {
                posterURL = PosterScanner.getPosterURLFromImdb(imdbXML);
            }
            
            // Validate the poster
            if (posterValidate && !validatePoster(posterURL, posterWidth, posterHeight, posterValidateAspect))
                posterURL = Movie.UNKNOWN;
            else
                logger.finest("Poster URL found at " + posterSearchToken + ": " + posterURL);
        }
        
        return posterURL;
    }

    /**
     * Get the size of the file at the end of the URL
     * Taken from: http://forums.sun.com/thread.jspa?threadID=528155&messageID=2537096
     * 
     * @param   posterURL       The URL to check as a string
     * @param   posterWidth     The width to check
     * @param   posterHeight    The height to check
     * @param   checkAspect     Should the aspect ratio be checked
     * @return                  True if the poster is good, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean validatePoster(String posterURL, int posterWidth, int posterHeight, boolean checkAspect) {
        Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
        ImageReader reader = (ImageReader) readers.next();
        int urlWidth = 0, urlHeight = 0;
        float urlAspect;
        
        if (!posterValidate) {
            return true;
        }
        
        try {
            URL url = new URL(posterURL);
            InputStream in = url.openStream();
            ImageInputStream iis = ImageIO.createImageInputStream(in);
            reader.setInput(iis, true);
            urlWidth = reader.getWidth(0);
            urlHeight = reader.getHeight(0);
        } catch (IOException ignore) {
            logger.finest(ignore.getMessage() + ": can't open");
            return false; // Quit and return a false poster
        }

        urlAspect = (float) urlWidth / (float) urlHeight;

        if (checkAspect && urlAspect > 1.0) {
            logger.finest(posterURL + " rejected: URL is landscape format");
            return false;
        }
        
        if (urlWidth < posterWidth) {
            logger.finest(posterURL + " rejected: URL width (" + urlWidth + ") is smaller than poster width (" + posterWidth + ")");
            return false;
        }
        
        if (urlHeight < posterHeight) {
            logger.finest(posterURL + " rejected: URL height (" + urlHeight + ") is smaller than poster height (" + posterHeight + ")");
            return false;
        }
        return true;
    }    

    /**
     * Search Google images for a poster to use for the movie
     * 
     * @param   movieName   The name of the movie to search for
     * @return              A URL of the poster to use.
     * 
     * TODO     Change out the French Google for English
     */
    public static String getPosterURLFromGoogle(String movieName) {
        try {
            StringBuffer sb = new StringBuffer("http://images.google.fr/images?q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            sb.append("&gbv=2");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("imgurl=") + 7;

            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"&");
                return st.nextToken();
            } else {
                return Movie.UNKNOWN;
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving poster URL from google images : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }
    
    /**
     * Search Yahoo images for a poster to use for the movie
     * 
     * @param   movieName   The name of the movie to search for
     * @return              A URL of the poster to use.
     * 
     * TODO     Change out the French Yahoo for English
     */
    public static String getPosterURLFromYahoo(String movieName) {
        try {
            StringBuffer sb = new StringBuffer("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("imgurl=");
            int endIndex = xml.indexOf("%26", beginIndex);

            if (beginIndex != -1 && endIndex > beginIndex) {
                return URLDecoder.decode(xml.substring(beginIndex + 7, endIndex), "UTF-8");
            } else {
                return Movie.UNKNOWN;
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving poster URL from yahoo images : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }
    
    /**
     * Search Motechnet for a poster to use for the movie
     * 
     * @param   movieID The IMDb ID for the movie
     * @return          A poster URL to use for the movie.
     */
    public static String getPosterURLFromMotechnet(String movieID) {
        try {
            webBrowser.request("http://posters.motechnet.com/title/" + movieID + "/");
        } catch (Exception e) {
            // The URL wasn't found, so the poster doesn't exist
            return Movie.UNKNOWN;
        }

        return "http://posters.motechnet.com/covers/" + movieID + "_largeCover.jpg";
    }
    
    /**
     * Search IMP Awards site for a poster to use for the movie
     * 
     * @param   movieName   The name of the movie to look for      
     * @param   movieYear   The year of the movie
     * @return              A poster URL to use for the movie.
     */
    public static String getPosterURLFromImpAwards(String movieName, String movieYear) {
        String returnString = Movie.UNKNOWN;
        String content = null;
        
        try {
            content = webBrowser.request("http://www.google.com/custom?sitesearch=www.impawards.com&q=" + URLEncoder.encode(movieName + " " + movieYear, "UTF-8"));
        } catch (Exception e) {
            // The movie doesn't exists, so return unknown
            return Movie.UNKNOWN;
        }

        if (content != null) {
            int indexMovieLink = content.indexOf("<a href=\"http://www.impawards.com/" + movieYear + "/");
            if (indexMovieLink != -1) {
                String imageUrl = content.substring(indexMovieLink + 9, indexMovieLink + 39) + "posters/";
                int endIndex = content.indexOf("\"", indexMovieLink + 39);
                if (endIndex != -1) {
                    imageUrl += content.substring(indexMovieLink + 39, endIndex);
                    if (imageUrl.endsWith("standard.html")) {
                        imageUrl = null;
                    } else if (imageUrl.endsWith(".html")) {
                        imageUrl = imageUrl.substring(0, imageUrl.length() - 4) + "jpg";
                    } else {
                        imageUrl = null;
                    }
                } else {
                    imageUrl = null;
                }

                if (imageUrl != null) {
                    returnString = imageUrl;
                }
            }
        }

        return returnString;
    }
    
    /**
     * Search MovieCovers.com for a poster to use for the movie
     * 
     * @param   movieName   The name of the movie to search for
     * @return              A poster URL to use for the movie.
     */
    public static String getPosterURLFromMovieCovers(String movieName) {
        String returnString = Movie.UNKNOWN;

        try {
            StringBuffer sb = new StringBuffer("http://www.google.com/search?meta=&q=site%3Amoviecovers.com+");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            String content = webBrowser.request(sb.toString());
            if (content != null) {
                int indexStartLink = content.indexOf("<a href=\"http://www.moviecovers.com/film/titre_");
                if (indexStartLink >= 0) {
                    int indexEndLink = content.indexOf("\" class=l>", indexStartLink);
                    if (indexEndLink > 0) {
                        String findMovieUrl = content.substring(indexStartLink + 47, indexEndLink);
                        returnString = "http://www.moviecovers.com/getjpg.html/" + 
                                       findMovieUrl.substring(0, findMovieUrl.lastIndexOf('.')).replace("+", "%20") + ".jpg";
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving Moviecovers poster URL: " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
        
        return returnString;
    }
    
    /**
     * Search MovieDB.org for a poster to use for the movie
     * 
     * @param   movieName   The name of the movie to search for
     * @return              A poster URL to use for the movie.
     */
    public static String getPosterURLFromMoviedb(String movieName) {
        String returnString = Movie.UNKNOWN;
        String tmdbContent = "";
        StringBuffer tmdbSB = null;
        int indexStartLink = -1;
        int indexEndLink = -1;
        int tmdbID = 0;
        
        try {
            tmdbSB = new StringBuffer("http://www.themoviedb.org/search?search[text]=");
            tmdbSB.append(URLEncoder.encode(movieName, "UTF-8"));
            tmdbContent = webBrowser.request(tmdbSB.toString());

            // Locate TheMovieDB ID from the search page
            indexStartLink = tmdbContent.indexOf("<a href=\"/movie/");
            indexEndLink = tmdbContent.indexOf("\">", indexStartLink + 16); // Start at the end of the last search

            // If we've found it, then extract the ID
            if ((indexStartLink > 0) && (indexEndLink > 0)) {
                tmdbID = Integer.parseInt(tmdbContent.substring(indexStartLink + 16, indexEndLink));
                logger.finest("Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/" + tmdbID);
            } else {
                // Not found, so exit
                return returnString;
            }
            
            tmdbSB = new StringBuffer("http://www.themoviedb.org/movie/" + tmdbID + "/posters");
            tmdbContent = webBrowser.request(tmdbSB.toString());
            
            Matcher tmdbMatch = Pattern.compile("http://images\\.themoviedb\\.org/posters/.+?\\.(jpg|jpeg)").matcher(tmdbContent);
            
            while (tmdbMatch.find() && returnString.equalsIgnoreCase(Movie.UNKNOWN)) {
                if (validatePoster(tmdbMatch.group(), posterWidth, posterHeight, posterValidateAspect)) {
                    returnString = tmdbMatch.group();
                }
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving TheMovieDB.org poster URL: " + movieName);
            logger.severe("Error : " + e.getMessage());
            returnString = Movie.UNKNOWN;
        }
        return returnString;
    }
    
    /**
     * Search the IMDb XML page for a poster
     * 
     * @param imdbXML   The XML page for the movie that contains the posters
     * @return          A poster URL to use for the movie.
     */
    public static String getPosterURLFromImdb(String imdbXML) {
        String posterURL = Movie.UNKNOWN;
        StringTokenizer st;
        
        int castIndex = imdbXML.indexOf("<h3>Cast</h3>");
        int beginIndex = imdbXML.indexOf("src=\"http://ia.media-imdb.com/images");
        
        // Search the XML from IMDB for a poster
        if ((beginIndex < castIndex) && (beginIndex != -1)) {
            st = new StringTokenizer(imdbXML.substring(beginIndex + 5), "\"");
            posterURL = st.nextToken();
            int index = posterURL.indexOf("_SX");
            if (index != -1) {
                posterURL = posterURL.substring(0, index) + "_SX600_SY800_.jpg";
            }
        } 

        return posterURL;
    }
}
