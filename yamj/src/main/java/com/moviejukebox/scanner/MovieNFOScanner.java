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
package com.moviejukebox.scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.pojava.datetime.DateTime;
import org.pojava.datetime.DateTimeConfig;

import com.moviejukebox.model.EpisodeDetail;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GenericFileFilter;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.XMLHelper;
import com.moviejukebox.tvrage.tools.StringTools;

/**
 * NFO file parser.
 * 
 * Search a NFO file for IMDb URL.
 * 
 * @author jjulien
 */
public class MovieNFOScanner {

    private static Logger logger = Logger.getLogger("moviejukebox");
    static final int BUFF_SIZE = 100000;
    static final byte[] buffer = new byte[BUFF_SIZE];
    private static String fanartToken;
    private static String fanartExtension;
    private static String forceNFOEncoding;
    private static String NFOdirectory;
    private static boolean getCertificationFromMPAA;
    private static String imdbPreferredCountry;
    private static boolean acceptAllNFO;
    private static String nfoExtRegex;
    private static String[] NFOExtensions;
    private static Pattern partPattern;
    
    private static boolean archiveScanRar;

    static {
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        fanartExtension = PropertiesUtil.getProperty("fanart.format", "jpg");
        
        forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", "AUTO");
        if (forceNFOEncoding.equalsIgnoreCase("AUTO")) {
            forceNFOEncoding = null;
        }

        NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
        NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
        getCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", "true");
        imdbPreferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        acceptAllNFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", "false");
        if (acceptAllNFO) {
            logger.info("NFOScanner: Accepting all NFO files in the directory");
        }
        
        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        nfoExtRegex = "";
        for (String ext : NFOExtensions) {
            nfoExtRegex += "|" + ext + "$";
        }
        // Skip beginning "|" and sandwich extensions between rest of regex
        nfoExtRegex = "(?i).*\\.(" + new String(nfoExtRegex.substring(1)) + ")";
        
        partPattern = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
        
        // Set the date format to dd-MM-yyyy
        DateTimeConfig.globalEuropeanDateFormat();
        
        archiveScanRar = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", "false");
    }

    /**
     * Search the IMDBb id of the specified movie in the NFO file if it exists.
     * 
     * @param movie
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.debug("NFOScanner: Scanning NFO file for IDs: " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirtyNFO(true);

            String nfo = FileTools.readFileToString(nfoFile);

            if (!parseXMLNFO(nfo, movie, nfoFile)) {
                DatabasePluginController.scanNFO(nfo, movie);

                logger.debug("NFOScanner: Scanning NFO for Poster URL");
                int urlStartIndex = 0;
                while (urlStartIndex >= 0 && urlStartIndex < nfo.length()) {
                    int currentUrlStartIndex = nfo.indexOf("http://", urlStartIndex);
                    if (currentUrlStartIndex >= 0) {
                        int currentUrlEndIndex = nfo.indexOf("jpg", currentUrlStartIndex);
                        if (currentUrlEndIndex < 0) {
                            currentUrlEndIndex = nfo.indexOf("JPG", currentUrlStartIndex);
                        }
                        if (currentUrlEndIndex >= 0) {
                            int nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex);
                            // look for shortest http://
                            while ((nextUrlStartIndex != -1) && (nextUrlStartIndex < currentUrlEndIndex + 3)) {
                                currentUrlStartIndex = nextUrlStartIndex;
                                nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex + 1);
                            }

                            // Check to see if the URL has <fanart> at the beginning and ignore it if it does (Issue 706)
                            if ((currentUrlStartIndex < 8)
                                            || (new String(nfo.substring(currentUrlStartIndex - 8, currentUrlStartIndex)).compareToIgnoreCase("<fanart>") != 0)) {
                                String foundUrl = new String(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                                
                                // Check for some invalid characters to see if the URL is valid
                                if (foundUrl.contains(" ") || foundUrl.contains("*")) {
                                    urlStartIndex = currentUrlStartIndex + 3;
                                } else {
                                    logger.debug("NFOScanner: Poster URL found in nfo = " + foundUrl);
                                    movie.setPosterURL(new String(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3)));
                                    urlStartIndex = -1;
                                    movie.setDirtyPoster(true);
                                }
                            } else {
                                logger.debug("NFOScanner: Poster URL ignored in NFO because it's a fanart URL");
                                // Search for the URL again
                                urlStartIndex = currentUrlStartIndex + 3;
                            }
                        } else {
                            urlStartIndex = currentUrlStartIndex + 3;
                        }
                    } else {
                        urlStartIndex = -1;
                    }
                }
            }
        }
    }

    /**
     * Search for the NFO file in the library structure
     * 
     * @param movie
     *            The movie bean to locate the NFO for
     * @return A List structure of all the relevant NFO files
     */
    public static List<File> locateNFOs(Movie movie) {
        List<File> nfos = new ArrayList<File>();
        GenericFileFilter fFilter = null;
        
        File currentDir = movie.getFirstFile().getFile();
        
        if (currentDir == null) {
            return nfos;
        }
        
        String baseFileName = currentDir.getName();
        String pathFileName = currentDir.getAbsolutePath();
        
        // Get the folder if it's a BluRay disk
        if (pathFileName.toUpperCase().contains(File.separator + "BDMV" + File.separator)) {
            currentDir = new File(FileTools.getParentFolder(currentDir));
            baseFileName = currentDir.getName();
            pathFileName = currentDir.getAbsolutePath();
        }
        
        if (archiveScanRar && pathFileName.toLowerCase().contains(".rar")) {
            currentDir = new File(FileTools.getParentFolder(currentDir));
            baseFileName = currentDir.getName();
            pathFileName = currentDir.getAbsolutePath();
        }

        // If "pathFileName" is a file then strip the extension from the file.
        if (currentDir.isFile()) {
            pathFileName = new String( pathFileName.substring(0, pathFileName.lastIndexOf(".")));
            baseFileName = new String(baseFileName.substring(0, baseFileName.lastIndexOf(".")));
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfos, pathFileName + new String(pathFileName.substring(pathFileName.lastIndexOf(File.separator))));
        }

        if (movie.isTVShow()) {
            String mfFilename;
            int pos;
            
            for (MovieFile mf : movie.getMovieFiles()) {
                mfFilename = mf.getFile().getParent().toUpperCase();
                
                if (mfFilename.contains("BDMV")) {
                    mfFilename = FileTools.getParentFolder(mf.getFile());
                    mfFilename = new String(mfFilename.substring(mfFilename.lastIndexOf(File.separator) + 1));
                } else {
                    mfFilename = mf.getFile().getName();
                    pos = mfFilename.lastIndexOf(".");
                    if (pos > 0) {
                        mfFilename = new String(mfFilename.substring(0, pos));
                    }
                }

                checkNFO(nfos, mf.getFile().getParent() + File.separator + mfFilename);
            }
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfos, pathFileName);
        
        if (isValidString(NFOdirectory)) {
            // *** Next step if we still haven't found the nfo file is to
            // search the NFO directory as specified in the moviejukebox.properties file
            String sNFOPath = FileTools.getDirPathWithSeparator(movie.getLibraryPath()) + NFOdirectory;
            checkNFO(nfos, sNFOPath + File.separator + baseFileName);
        }

        // *** Next step is to check for a directory wide NFO file.
        if (acceptAllNFO) {
            /* If any NFO file in this directory will do, then we search for all we can find
             * NOTE: for scanning efficiency, it is better to first search for specific
             * filenames before we start doing filtered "listfiles" which scans all the files;
             * A movie collection with all moviefiles in one directory could take tremendously
             * longer if for each moviefile found, the entire directory must be listed!!
             * Therefore, we first check for specific filenames (cfr. old behaviour) before
             * doing an entire scan of the directory -- and only if the user has decided to
             * accept any NFO file!
            */

            // Check the current directory
            fFilter = new GenericFileFilter(nfoExtRegex);
            checkRNFO(nfos, currentDir.getParentFile(), fFilter);
            
            // Also check the directory above, for the case where movies are in a multi-part named directory (CD/PART/DISK/Etc.)
            Matcher allNfoMatch = partPattern.matcher(currentDir.getAbsolutePath());
            if (allNfoMatch.find()) {
                logger.debug("NFOScanner: Found multi-part directory, checking parent directory for NFOs");
                checkRNFO(nfos, currentDir.getParentFile().getParentFile(), fFilter);
            }

         } else {
            // This file should be named the same as the directory that it is in
            // E.G. C:\TV\Chuck\Season 1\Season 1.nfo
            // We search up through all containing directories up to the library root

             if (currentDir != null) {
                 // Check the current directory for the video filename
                 fFilter = new GenericFileFilter("(?i)" + movie.getBaseFilename() + nfoExtRegex);
                 checkRNFO(nfos, currentDir, fFilter);
             }
         }
        
        // Recurse through the directories to the library root looking for NFO files
         String libraryRootPath = new File(movie.getLibraryPath()).getAbsolutePath();
         while (currentDir != null && !currentDir.getAbsolutePath().equals(libraryRootPath)) {
             //fFilter.setPattern("(?i)" + currentDir.getName() + nfoExtRegex);
             //checkRNFO(nfos, currentDir, fFilter);
             currentDir = currentDir.getParentFile();
             if (currentDir != null) {
                 final String path = currentDir.getPath();
                 // Path is not empty
                 if (!path.isEmpty()) {
                     // Path is not the root
                     if (!path.endsWith(File.separator)) {
                         checkNFO(nfos, appendToPath(path, currentDir.getName()));
                     }
                 }
             }
         }

        // we added the most specific ones first, and we want to parse those the last,
        // so nfo files in sub-directories can override values in directories above.
        Collections.reverse(nfos);
        
        return nfos;
    }

    /**
     * Search the current directory for all NFO files using a file filter
     * @param nfoFiles
     * @param currentDir
     * @param fFilter
     */
    private static void checkRNFO(List<File> nfoFiles, File currentDir, GenericFileFilter fFilter) {
        File[] fFiles = currentDir.listFiles(fFilter);
        if (fFiles != null && fFiles.length > 0) {
            for (File foundFile : fFiles ) {
                logger.debug("NFOScanner: Found " + foundFile.getName());
                nfoFiles.add(foundFile);
            }
        }
        return;
    }

    /**
     * Check to see if the passed filename exists with nfo extensions
     * 
     * @param checkNFOfilename
     *            (NO EXTENSION)
     * @return blank string if not found, filename if found
     */
    private static void checkNFO(List<File> nfoFiles, String checkNFOfilename) {
        File nfoFile;
        
        for (String ext : NFOExtensions) {
            nfoFile = FileTools.fileCache.getFile(checkNFOfilename + "." + ext);
            if (nfoFile.exists()) {
                logger.debug("NFOScanner: Found " + nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            }
        }
    }

    /**
     * Check the NFO file for any of the XML triggers that indicate it's a XML file
     * If found, process the NFO file as a XML NFO
     * @param nfo
     * @param movie
     * @param nfoFile
     * @return
     */
    private static boolean parseXMLNFO(String nfo, Movie movie, File nfoFile) {
        if (nfo.indexOf("<movie") > -1 && parseMovieNFO(nfoFile, movie, nfo)) {
            return true;
        } else if (nfo.indexOf("<tvshow") > -1 && parseTVNFO(nfoFile, movie, nfo)) {
            return true;
        } else if (nfo.indexOf("<episodedetails") > -1 && parseTVNFO(nfoFile, movie, nfo)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create an XML reader for file. Use forced encoding if specified.
     * 
     * @param nfoFile
     *            File to read.
     * @param nfo
     *            Content of the XML file. Used only for the encoding detection.
     * @return New XML reader.
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private static XMLEventReader createXMLReader(File nfoFile, String nfo) throws FactoryConfigurationError, XMLStreamException, FileNotFoundException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        // TODO Make the encoding detection more explicit
        boolean fileContainsEncoding = false;
        if (nfo != null) {
            int i = nfo.indexOf("encoding");
            fileContainsEncoding = (i > 0 && i < 100);
        }

        XMLEventReader r = (forceNFOEncoding != null && !fileContainsEncoding) 
                        ? factory.createXMLEventReader(FileTools.createFileInputStream(nfoFile), forceNFOEncoding)
                        : factory.createXMLEventReader(FileTools.createFileInputStream(nfoFile));
        return r;
    }

    /**
     * Used to parse out the XBMC NFO XML data for movies The specification is here: http://xbmc.org/wiki/?title=Import_-_Export_Library
     * 
     * @param xmlFile
     * @param movie
     */
    private static boolean parseMovieNFO(File nfoFile, Movie movie, String nfo) {
        try {
            XMLEventReader r = createXMLReader(nfoFile, nfo);

            boolean isMovieTag = false;

            // Before we can set the title and the title sort, we need to see if we have both, so store them here.
            String titleMain = null;
            String titleSort = null;

            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement()) {
                    String tag = e.asStartElement().getName().toString();
                    // logger.debug("In parseMovieNFO found new startElement=" + tag);
                    if (tag.equalsIgnoreCase("movie")) {
                        isMovieTag = true;
                    }

                    if (isMovieTag) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                titleMain = val;
                            }
                        } else if (tag.equalsIgnoreCase("originaltitle")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setOriginalTitle(val);
                            }
                        } else if (tag.equalsIgnoreCase("sorttitle")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                titleSort = val;
                            }
                        } else if (tag.equalsIgnoreCase("set")) {
                            String set = XMLHelper.getCData(r);
                            Attribute orderAttribute = e.asStartElement().getAttributeByName(new QName("order"));
                            movie.addSet(set, orderAttribute == null ? null : Integer.parseInt(orderAttribute.getValue()));
                        } else if (tag.equalsIgnoreCase("rating")) {
                            float val = XMLHelper.parseFloat(r);
                            if (val != 0.0f) {
                                movie.setRating(Math.round(val * 10f));
                            }
                        } else if (tag.equalsIgnoreCase("year")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setOverrideYear(true);
                                movie.setYear(val);
                            }
                        } else if (tag.equalsIgnoreCase("premiered") || tag.equalsIgnoreCase("releasedate")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                try {
                                    if (val.length() == 4) {
                                        // Assume just the year an append "-01-01" to the end
                                        val += "-01-01";
                                        // Warn the user
                                        logger.debug("NFOScanner: Partial date detected in premiered field of NFO for " + nfo); 
                                    }
                                    
                                    DateTime dateTime = new DateTime(val);

                                    movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                                    movie.setOverrideYear(true);
                                    movie.setYear(dateTime.toString("yyyy"));
                                } catch (Exception error) {
                                    logger.error("NFOScanner: Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
                                    logger.error("NFOScanner: premiered or releasedate does not contain a valid date.");
                                    final Writer eResult = new StringWriter();
                                    final PrintWriter printWriter = new PrintWriter(eResult);
                                    error.printStackTrace(printWriter);
                                    logger.error(eResult.toString());
                                }
                            }
                        } else if (tag.equalsIgnoreCase("quote")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setQuote(val);
                            }
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setTagline(val);
                            }
                        } else if (tag.equalsIgnoreCase("top250")) {
                            int val = XMLHelper.parseInt(r);
                            if (val > 0) {
                                movie.setTop250(val);
                            }
                        //} else if (tag.equalsIgnoreCase("votes")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("outline")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setOutline(val);
                            }
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setPlot(val);
                            }
                        //} else if (tag.equalsIgnoreCase("tagline")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("runtime")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setRuntime(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) { // This is a custom YAMJ tag
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                            }
                        } else if (tag.equalsIgnoreCase("mpaa") && getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = new String(val.substring(start, pos));
                                    } else {
                                        val = new String(val.substring(start));
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("certification") && !getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                int countryPos = val.lastIndexOf(imdbPreferredCountry);
                                if (countryPos > 0) {
                                    // We've found the country, so extract just that tag
                                    val = new String(val.substring(countryPos));
                                    int pos = val.indexOf(":");
                                    if (pos > 0) {
                                        int endPos = val.indexOf(" /");
                                        if (endPos > 0) {
                                            // This is in the middle of the string
                                            val = new String(val.substring(pos + 1, endPos));
                                        } else {
                                            // This is at the end of the string
                                            val = new String(val.substring(pos + 1));
                                        }
                                    }
                                } else {
                                    // The country wasn't found in the value, so grab the last one
                                    int pos = val.lastIndexOf(":");
                                    if (pos > 0) {
                                        // Strip the country code from the rating for certification like "UK:PG-12"
                                        val = new String(val.substring(pos + 1));
                                    }
                                }
                                movie.setCertification(val);
                            }
                        //} else if (tag.equalsIgnoreCase("playcount")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("watched")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                try {
                                    movie.setWatched(Boolean.parseBoolean(val));
                                } catch (Exception ignore) {
                                    // Don't change the watched status
                                }
                            }
                        } else if (tag.equalsIgnoreCase("tvdbid")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, val);
                            }
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.debug("NFOScanner: In parseMovieNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, val); // without attribute we assume it's an IMDB Id
                                    logger.debug("NFOScanner: In parseMovieNFO Id=" + val + " found for default IMDB");
                                }
                                
                                // Any value of 0 (Zero) or -1 will stop the movie being scraped
                                if (val.equals("0") || val.equals("-1")) {
                                    movie.setScrapeLibrary(false);
                                }
                            }
                        //} else if (tag.equalsIgnoreCase("filenameandpath")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("trailer")) {
                            String trailer = XMLHelper.getCData(r).trim();
                            if (!trailer.isEmpty()) {
                                ExtraFile ef = new ExtraFile();
                                ef.setNewFile(false);
                                ef.setFilename(trailer);
                                // The title isn't contained in the NFO file so we'll need to default one
                                if (trailer.contains("youtube")) {
                                    ef.setTitle(ef.getFirstPart(), "TRAILER-YouTube");
                                } else {
                                    ef.setTitle(ef.getFirstPart(), "TRAILER-NFO");
                                }

                                movie.addExtraFile(ef);
                            }
                        } else if (tag.equalsIgnoreCase("genre")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("credits")) {
                            String event = r.nextEvent().toString();
                            while (!event.equalsIgnoreCase("</credits>")) {
                                if (event.equalsIgnoreCase("<writer>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        movie.addWriter(Movie.UNKNOWN, val, Movie.UNKNOWN);
                                    }
                                //} else if (event.equalsIgnoreCase("<otherCredits>")) {
                                    // Not currently used
                                }
                                if (r.hasNext()) {
                                    event = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                        } else if (tag.equalsIgnoreCase("director")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.addDirector(Movie.UNKNOWN, val, Movie.UNKNOWN);
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
                            String event = r.nextEvent().toString();
                            String name = Movie.UNKNOWN;
                            String role = Movie.UNKNOWN;
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        name = val;
                                    }
                                } else if (event.equalsIgnoreCase("<role>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        role = val;
                                    }
                                }
                                if (r.hasNext()) {
                                    event = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                            if (isValidString(name)) {
                                movie.addActor(Movie.UNKNOWN, name, role, Movie.UNKNOWN);
                            }
                        } else if (tag.equalsIgnoreCase("fileinfo")) { // File Info Section
                            String fiEvent = r.nextEvent().toString();
                            String finalCodec = Movie.UNKNOWN;
                            String finalLanguage = Movie.UNKNOWN;
                            String finalChannels = Movie.UNKNOWN;
                            String tmpSubtitleLanguage = Movie.UNKNOWN;
                            while (!fiEvent.equalsIgnoreCase("</fileinfo>")) {
                                if (fiEvent.equalsIgnoreCase("<video>")) {
                                    String nfoWidth = null;
                                    String nfoHeight = null;
                                    while (!fiEvent.equalsIgnoreCase("</video>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setVideoCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<aspect>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setAspectRatio(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<width>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                nfoWidth = val;
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<height>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                nfoHeight = val;
                                            }
                                        }

                                        if (nfoWidth != null && nfoHeight != null) {
                                            movie.setResolution(nfoWidth + "x" + nfoHeight);
                                            nfoWidth = null;
                                            nfoHeight = null;
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                }

                                // Issue 1251 - Multiple audio info - the last override all previous
                                if (fiEvent.equalsIgnoreCase("<audio>")) {
                                    // Start of audio info, using temp data to concatains codec + languague
                                    String tmpCodec = Movie.UNKNOWN;
                                    String tmpLanguage = Movie.UNKNOWN;
                                    String tmpChannels = Movie.UNKNOWN;

                                    while (!fiEvent.equalsIgnoreCase("</audio>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                // codec come first
                                                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                                                if (val.toLowerCase().equals(val)) {
                                                    val = val.toUpperCase();
                                                }
                                                
                                                if (isNotValidString(tmpCodec)) {
                                                    tmpCodec = val;
                                                } else {
                                                    // We already have language info, need to concatenate
                                                    tmpCodec = val + " " + tmpCodec;
                                                }
                                                // movie.setAudioCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                tmpLanguage = MovieFilenameScanner.determineLanguage(val);
                                                if (isNotValidString(tmpCodec)) {
                                                    tmpCodec = "(" + val + ")";
                                                } else {
                                                    tmpCodec += " (" + val + ")";
                                                }
                                                // movie.setLanguage(MovieFilenameScanner.determineLanguage(val));
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<channels>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                tmpChannels = val;
                                                // movie.setAudioChannels(val);
                                            }
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                    // Parsing of audio end - setting data to movie.
                                    if (isValidString(tmpCodec)) {
                                        // First one.
                                        if (isNotValidString(finalCodec)) {
                                            finalCodec = tmpCodec;
                                        } else {
                                            finalCodec = finalCodec + " / " + tmpCodec;
                                        }

                                    }
                                    
                                    if (isValidString(tmpLanguage)) {
                                        // First one.
                                        if (isNotValidString(finalLanguage)) {
                                            finalLanguage = tmpLanguage;
                                        } else {
                                            finalLanguage = finalLanguage + " / " + tmpLanguage;
                                        }

                                    }
                                    
                                    if (isValidString(tmpChannels)) {
                                        // First one.
                                        if (isNotValidString(finalChannels)) {
                                            finalChannels = tmpChannels;
                                        } else {
                                            finalChannels = finalChannels + " / " + tmpChannels;
                                        }

                                    }
                                }

                                if (fiEvent.equalsIgnoreCase("<subtitle>")) {
                                    while (!fiEvent.equalsIgnoreCase("</subtitle>")) {
                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                if (isNotValidString(tmpSubtitleLanguage)) {
                                                    tmpSubtitleLanguage = val;
                                                } else {
                                                    tmpSubtitleLanguage = tmpSubtitleLanguage + " / " + val;
                                                }
                                            }
                                            // Unused
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                }

                                if (r.hasNext()) {
                                    fiEvent = r.nextEvent().toString();
                                } else {
                                    break;
                                }

                            }
                            
                            // Everything is parsed, override all audio info. - NFO Always right.
                            if (isValidString(finalChannels)) {
                                movie.setAudioChannels(finalChannels);
                            }
                            
                            if (isValidString(finalCodec)) {
                                movie.setAudioCodec(finalCodec);
                            }
                            
                            if (isValidString(finalLanguage)) {
                                movie.setLanguage(finalLanguage);
                            }
                            
                            if (isValidString(tmpSubtitleLanguage)) {
                                movie.setSubtitles(tmpSubtitleLanguage);
                            }

                        } else if (tag.equalsIgnoreCase("VideoSource")) {
                            // Issue 506 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setVideoSource(val);
                            }
                        } else if (tag.equalsIgnoreCase("VideoOutput")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setVideoOutput(val);
                            }
                        } else if (tag.equalsIgnoreCase("Company")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("Studio")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("Country")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCountry(val);
                            }
                        }
                    }
                } else if (e.isEndElement()) {
                    // logger.debug("In parseMovieNFO found new endElement=" + e.asEndElement().getName().toString());
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("movie")) {
                        break;
                    }
                }
            }
            
            // We've processed all the NFO file, so work out what to do with the title and titleSort
            if (StringTools.isValidString(titleMain)) {
                // We have a valid title, so set that for title and titleSort
                movie.setTitle(titleMain);
                movie.setTitleSort(titleMain);
                movie.setOverrideTitle(true);
            }
            
            // Now check the titleSort and overwrite it if necessary.
            if (StringTools.isValidString(titleSort)) {
                movie.setTitleSort(titleSort);
            }

            return isMovieTag;
        } catch (Exception error) {
            logger.error("NFOScanner: Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        return false;
    }

    /**
     * Used to parse out the XBMC nfo xml data for tv series
     * 
     * @param xmlFile
     * @param movie
     */
    private static boolean parseTVNFO(File nfoFile, Movie movie, String nfo) {

        try {
            XMLEventReader r = createXMLReader(nfoFile, nfo);

            boolean isTVTag = false;
            boolean isEpisode = false;
            boolean isOK = false;
            EpisodeDetail episodedetail = new EpisodeDetail();
            
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                
                if (e.isStartElement()) {
                    String tag = e.asStartElement().getName().toString();
                    if (tag.equalsIgnoreCase("tvshow")) {
                        isTVTag = true;
                    }
                    if (tag.equalsIgnoreCase("episodedetails")) {
                        isEpisode = true;
                        episodedetail = new EpisodeDetail();
                    }

                    /************************************************************
                     * Process the main TV show details section
                     */
                    if (isTVTag) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setTitle(val);
                                movie.setOverrideTitle(true);
                            }
                        } else if (tag.equalsIgnoreCase("tvdbid")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, val);
                            }
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.debug("NFOScanner: In parseTVNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, val); // without attribute we assume it's a TheTVDB Id
                                    logger.debug("NFOScanner: In parseTVNFO Id=" + val + " found for default TheTVDB");
                                }
                            }
                        } else if (tag.equalsIgnoreCase("watched")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                try {
                                    movie.setWatched(Boolean.parseBoolean(val));
                                } catch (Exception ignore) {
                                }
                            }
                        } else if (tag.equalsIgnoreCase("set")) {
                            String set = XMLHelper.getCData(r);
                            Attribute orderAttribute = e.asStartElement().getAttributeByName(new QName("order"));
                            Integer order = null;
                            // Check the attribute is found before getting the value
                            if (orderAttribute != null) {
                                order = Integer.valueOf(orderAttribute.getValue());
                            }
                            movie.addSet(set, order);
                        } else if (tag.equalsIgnoreCase("rating")) {
                            float val = XMLHelper.parseFloat(r);
                            if (val != 0.0f) {
                                movie.setRating(Math.round(val * 10f));
                            }
                        } else if (tag.equalsIgnoreCase("mpaa") && getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = new String(val.substring(start, pos));
                                    } else {
                                        val = new String(val.substring(start));
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("certification") && !getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCertification(val);
                            }
                        //} else if (tag.equalsIgnoreCase("season")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("episode")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("votes")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("displayseason")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("displayepisode")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setPlot(val);
                            }
                        } else if (tag.equalsIgnoreCase("genre")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("premiered") || tag.equalsIgnoreCase("releasedate")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                try {
                                    DateTime dateTime = new DateTime(val);

                                    movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                                    movie.setOverrideYear(true);
                                    movie.setYear(dateTime.toString("yyyy"));
                                } catch (Exception ignore) {
                                    // Set the release date if there is an exception
                                    movie.setReleaseDate(val);
                                }
                            }
                        } else if (tag.equalsIgnoreCase("quote")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setQuote(val);
                            }
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setTagline(val);
                            }
                        } else if (tag.equalsIgnoreCase("studio")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("company")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("country")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setCountry(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) {
                            // TODO Validate the URL and see if it's a local file
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                            }
                        } else if (tag.equalsIgnoreCase("trailer")) {
                            String trailer = XMLHelper.getCData(r).trim();
                            if (!trailer.isEmpty()) {
                                ExtraFile ef = new ExtraFile();
                                ef.setNewFile(false);
                                ef.setFilename(trailer);
                                movie.addExtraFile(ef);
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
                            String event = r.nextEvent().toString();
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        movie.addActor(val);
                                    }
                                //} else if (event.equalsIgnoreCase("<role>")) {
                                    // Not currently used
                                //} else if (event.equalsIgnoreCase("<thumb>")) {
                                    // Not currently used
                                }
                                if (r.hasNext()) {
                                    event = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                        } else if (tag.equalsIgnoreCase("fileinfo")) { // File Info Section
                            String fiEvent = r.nextEvent().toString();
                            while (!fiEvent.equalsIgnoreCase("</fileinfo>")) {
                                if (fiEvent.equalsIgnoreCase("<video>")) {
                                    String nfoWidth = null;
                                    String nfoHeight = null;
                                    while (!fiEvent.equalsIgnoreCase("</video>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setVideoCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<aspect>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setAspectRatio(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<width>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                nfoWidth = val;
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<height>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                nfoHeight = val;
                                            }
                                        }

                                        if (nfoWidth != null && nfoHeight != null) {
                                            movie.setResolution(nfoWidth + "x" + nfoHeight);
                                            nfoWidth = null;
                                            nfoHeight = null;
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                }

                                if (fiEvent.equalsIgnoreCase("<audio>")) {
                                    while (!fiEvent.equalsIgnoreCase("</audio>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setAudioCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setLanguage(MovieFilenameScanner.determineLanguage(val));
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<channels>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setAudioChannels(val);
                                            }
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                }

                                /* We don't check for subtitles at this time
                                if (fiEvent.equalsIgnoreCase("<subtitle>")) {
                                    while (!fiEvent.equalsIgnoreCase("</subtitle>")) {
                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            // Unused
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                }*/

                                if (r.hasNext()) {
                                    fiEvent = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                        } else if (tag.equalsIgnoreCase("VideoSource")) {
                            // Issue 506 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setVideoSource(val);
                            }
                        } else if (tag.equalsIgnoreCase("VideoOutput")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setVideoOutput(val);
                            }
                        }
                    }
                    
                    /************************************************************
                     * Process the episode details section
                     * 
                     * These details should be added to the movie file and 
                     * not the movie itself
                     */
                    if (isEpisode) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                episodedetail.setTitle(val);
                            }
                        //} else if (tag.equalsIgnoreCase("rating")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("season")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                episodedetail.setSeason(Integer.parseInt(val));
                            }
                        } else if (tag.equalsIgnoreCase("episode")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                episodedetail.setEpisode(Integer.parseInt(val));
                            }
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                episodedetail.setPlot(val);
                            }
                        //} else if (tag.equalsIgnoreCase("credits")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("director")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("actor")) {
                            // Not currently used
                        }
                    }                    
                } else if (e.isEndElement()) {
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("tvshow")) {
                        isTVTag = false;
                        isOK = true;
                    }
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("episodedetails")) {
                        isEpisode = false;
                        episodedetail.updateMovie(movie);
                        isOK = true;
                    }
                }
            }
            return isOK;
        } catch (Exception error) {
            logger.error("NFOScanner: Failed parsing NFO file: " + nfoFile.getAbsolutePath() + ". Please fix or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        return false;
    }

}
