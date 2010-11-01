/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
import java.util.logging.Logger;
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
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.XMLHelper;

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
    private static String forceNFOEncoding;
    private static String NFOdirectory;
    private static boolean getCertificationFromMPAA;
    private static String imdbPreferredCountry;
    private static boolean acceptAllNFO;
    private static String nfoExtRegex;
    private static String[] NFOExtensions;
    private static Pattern partPattern;
    

    static {
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");

        forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", null);
        if (forceNFOEncoding.equalsIgnoreCase("AUTO")) {
            forceNFOEncoding = null;
        }

        NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
        NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
        getCertificationFromMPAA = Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.getCertificationFromMPAA", "true"));
        imdbPreferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        acceptAllNFO = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.nfo.acceptAllNfo", "false"));
        if (acceptAllNFO) {
            logger.fine("MovieNFOScanner: Accepting all NFO files in the directory");
        }
        
        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        nfoExtRegex = "";
        for (String ext : NFOExtensions) {
            nfoExtRegex += "|" + ext + "$";
        }
        // Skip beginning "|" and sandwich extensions between rest of regex
        nfoExtRegex = "(?i).*\\.(" + nfoExtRegex.substring(1) + ")";
        
        partPattern = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
        
        // Set the date format to dd-MM-yyyy
        DateTimeConfig.globalEuropeanDateFormat();
    }

    /**
     * Search the IMDBb id of the specified movie in the NFO file if it exists.
     * 
     * @param movie
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.finest("Scanning NFO file for IDs: " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirtyNFO(true);

            String nfo = FileTools.readFileToString(nfoFile);

            if (!parseXMLNFO(nfo, movie, nfoFile)) {
                DatabasePluginController.scanNFO(nfo, movie);

                logger.finest("Scanning NFO for Poster URL");
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
                                            || (nfo.substring(currentUrlStartIndex - 8, currentUrlStartIndex).compareToIgnoreCase("<fanart>") != 0)) {
                                logger.finer("Poster URL found in nfo = " + nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                                movie.setPosterURL(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                                urlStartIndex = -1;
                                movie.setDirtyPoster(true);
                            } else {
                                logger.finer("Poster URL ignored in NFO because it's a fanart URL");
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
        
        // If "pathFileName" is a file then strip the extension from the file.
        if (currentDir.isFile()) {
            pathFileName = pathFileName.substring(0, pathFileName.lastIndexOf("."));
            baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf("."));
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfos, pathFileName + pathFileName.substring(pathFileName.lastIndexOf(File.separator)));
        }

        if (movie.isTVShow()) {
            String mfFilename;
            int pos;
            
            for (MovieFile mf : movie.getMovieFiles()) {
                mfFilename = mf.getFile().getParent().toUpperCase();
                
                if (mfFilename.contains("BDMV")) {
                    mfFilename = FileTools.getParentFolder(mf.getFile());
                    mfFilename = mfFilename.substring(mfFilename.lastIndexOf(File.separator) + 1);
                } else {
                    mfFilename = mf.getFile().getName();
                    pos = mfFilename.lastIndexOf(".");
                    if (pos > 0) {
                        mfFilename = mfFilename.substring(0, pos);
                    }
                }

                checkNFO(nfos, mf.getFile().getParent() + File.separator + mfFilename);
            }
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfos, pathFileName);

        if (StringTools.isValidString(NFOdirectory)) {
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
                logger.finest("MovieNFOScanner: Found multi-part directory, checking parent directory for NFOs");
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
                         checkNFO(nfos, StringTools.appendToPath(path, currentDir.getName()));
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
                logger.finest("Found NFO: " + foundFile.getName());
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
                logger.finest("Found NFO: " + nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            } else {
                // We put this here, even though, technically, we've already searched for the file
                // so the user will see where they COULD place the file.
                logger.finest("Checking for NFO: " + checkNFOfilename + "." + ext);
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
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement()) {
                    String tag = e.asStartElement().getName().toString();
                    // logger.finest("In parseMovieNFO found new startElement=" + tag);
                    if (tag.equalsIgnoreCase("movie")) {
                        isMovieTag = true;
                    }

                    if (isMovieTag) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setTitle(val);
                                movie.setOverrideTitle(true);
                            }
                        } else if (tag.equalsIgnoreCase("originaltitle")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setOriginalTitle(val);
                            }
                        //} else if (tag.equalsIgnoreCase("sorttitle")) {
                            // Not currently used
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setOverrideYear(true);
                                movie.setYear(val);
                            }
                        } else if (tag.equalsIgnoreCase("premiered") || tag.equalsIgnoreCase("releasedate")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                try {
                                    if (val.length() == 4) {
                                        // Assume just the year an append "-01-01" to the end
                                        val += "-01-01";
                                        // Warn the user
                                        logger.finer("NFOScanner: Partial date detected in premiered field of NFO for " + nfo); 
                                    }
                                    
                                    DateTime dateTime = new DateTime(val);

                                    movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                                    movie.setOverrideYear(true);
                                    movie.setYear(dateTime.toString("yyyy"));
                                } catch (Exception error) {
                                    logger.severe("Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
                                    final Writer eResult = new StringWriter();
                                    final PrintWriter printWriter = new PrintWriter(eResult);
                                    error.printStackTrace(printWriter);
                                    logger.severe(eResult.toString());
                                }
                            }
                        } else if (tag.equalsIgnoreCase("quote")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setQuote(val);
                            }
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setOutline(val);
                            }
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setPlot(val);
                            }
                        //} else if (tag.equalsIgnoreCase("tagline")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("runtime")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setRuntime(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) { // This is a custom YAMJ tag
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                            }
                        } else if (tag.equalsIgnoreCase("mpaa") && getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = val.substring(start, pos);
                                    } else {
                                        val = val.substring(start);
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("certification") && !getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                int countryPos = val.lastIndexOf(imdbPreferredCountry);
                                if (countryPos > 0) {
                                    // We've found the country, so extract just that tag
                                    val = val.substring(countryPos);
                                    int pos = val.indexOf(":");
                                    if (pos > 0) {
                                        int endPos = val.indexOf(" /");
                                        if (endPos > 0) {
                                            // This is in the middle of the string
                                            val = val.substring(pos + 1, endPos);
                                        } else {
                                            // This is at the end of the string
                                            val = val.substring(pos + 1);
                                        }
                                    }
                                } else {
                                    // The country wasn't found in the value, so grab the last one
                                    int pos = val.lastIndexOf(":");
                                    if (pos > 0) {
                                        // Strip the country code from the rating for certification like "UK:PG-12"
                                        val = val.substring(pos + 1);
                                    }
                                }
                                movie.setCertification(val);
                            }
                        //} else if (tag.equalsIgnoreCase("playcount")) {
                            // Not currently used
                        //} else if (tag.equalsIgnoreCase("watched")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.finest("In parseMovieNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, val); // without attribute we assume it's an IMDB Id
                                    logger.finest("In parseMovieNFO Id=" + val + " found for default IMDB");
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
                                    if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                        movie.addWriter(val);
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.addDirector(val);
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
                            String event = r.nextEvent().toString();
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                        movie.addActor(val);
                                    }
                                //} else if (event.equalsIgnoreCase("<role>")) {
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
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setVideoCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<aspect>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setAspectRatio(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<width>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                nfoWidth = val;
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<height>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                // codec come first
                                                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                                                if (val.toLowerCase().equals(val)) {
                                                    val = val.toUpperCase();
                                                }
                                                
                                                if (tmpCodec.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                    tmpCodec = val;
                                                } else {
                                                    // We alerady have language info, need to concat
                                                    tmpCodec = val + " " + tmpCodec;
                                                }
                                                // movie.setAudioCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                tmpLanguage = MovieFilenameScanner.determineLanguage(val);
                                                if (tmpCodec.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                    tmpCodec = "(" + val + ")";
                                                } else {
                                                    tmpCodec += " (" + val + ")";
                                                }
                                                // movie.setLanguage(MovieFilenameScanner.determineLanguage(val));
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<channels>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                tmpChannels = val;
                                                // movie.setAudioChannels(val);
                                            }
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                    // Parsing of audio end - setting data to movie.
                                    if (!tmpCodec.equalsIgnoreCase(Movie.UNKNOWN)) {
                                        // First one.
                                        if (finalCodec.equalsIgnoreCase(Movie.UNKNOWN)) {
                                            finalCodec = tmpCodec;
                                        } else {
                                            finalCodec = finalCodec + " / " + tmpCodec;
                                        }

                                    }
                                    if (!tmpLanguage.equalsIgnoreCase(Movie.UNKNOWN)) {
                                        // First one.
                                        if (finalLanguage.equalsIgnoreCase(Movie.UNKNOWN)) {
                                            finalLanguage = tmpLanguage;
                                        } else {
                                            finalLanguage = finalLanguage + " / " + tmpLanguage;
                                        }

                                    }
                                    if (!tmpChannels.equalsIgnoreCase(Movie.UNKNOWN)) {
                                        // First one.
                                        if (finalChannels.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                if (tmpSubtitleLanguage.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!finalChannels.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setAudioChannels(finalChannels);
                            }
                            if (!finalCodec.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setAudioCodec(finalCodec);
                            }
                            if (!finalLanguage.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setLanguage(finalLanguage);
                            }
                            if (!tmpSubtitleLanguage.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setSubtitles(tmpSubtitleLanguage);
                            }

                        } else if (tag.equalsIgnoreCase("VideoSource")) {
                            // Issue 506 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setVideoSource(val);
                            }
                        } else if (tag.equalsIgnoreCase("VideoOutput")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setVideoOutput(val);
                            }
                        } else if (tag.equalsIgnoreCase("Company")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("Studio")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("Country")) {
                            // Issue 1173 - Even though it's not strictly XBMC standard
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCountry(val);
                            }
                        }
                    }
                } else if (e.isEndElement()) {
                    // logger.finest("In parseMovieNFO found new endElement=" + e.asEndElement().getName().toString());
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("movie")) {
                        break;
                    }
                }
            }

            return isMovieTag;
        } catch (Exception error) {
            logger.severe("Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setTitle(val);
                                movie.setOverrideTitle(true);
                            }
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.finest("In parseTVNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, val); // without attribute we assume it's a TheTVDB Id
                                    logger.finest("In parseTVNFO Id=" + val + " found for default TheTVDB");
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = val.substring(start, pos);
                                    } else {
                                        val = val.substring(start);
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("certification") && !getCertificationFromMPAA) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setPlot(val);
                            }
                        } else if (tag.equalsIgnoreCase("genre")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("premiered") || tag.equalsIgnoreCase("releasedate")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setQuote(val);
                            }
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setTagline(val);
                            }
                        } else if (tag.equalsIgnoreCase("studio")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("company")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("country")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setCountry(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) {
                            // TODO Validate the URL and see if it's a local file
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
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
                                    if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setVideoCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<aspect>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setAspectRatio(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<width>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                nfoWidth = val;
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<height>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setAudioCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                                movie.setLanguage(MovieFilenameScanner.determineLanguage(val));
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<channels>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setVideoSource(val);
                            }
                        } else if (tag.equalsIgnoreCase("VideoOutput")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                episodedetail.setTitle(val);
                            }
                        //} else if (tag.equalsIgnoreCase("rating")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("season")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                episodedetail.setSeason(Integer.parseInt(val));
                            }
                        } else if (tag.equalsIgnoreCase("episode")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
                                episodedetail.setEpisode(Integer.parseInt(val));
                            }
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty() && !val.equalsIgnoreCase(Movie.UNKNOWN)) {
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
            logger.severe("Failed parsing NFO file: " + nfoFile.getAbsolutePath() + ". Please fix or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }

        return false;
    }

}
