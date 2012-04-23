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
package com.moviejukebox.scanner;

import com.moviejukebox.model.*;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;
import org.pojava.datetime.DateTimeConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * NFO file parser.
 *
 * Search a NFO file for IMDb URL.
 *
 * @author jjulien
 */
public class MovieNFOScanner {

    private static final Logger logger = Logger.getLogger(MovieNFOScanner.class);
    private static final String logMessage = "MovieNFOScanner: ";
    // Types of nodes
    private static final String TYPE_MOVIE = "movie";
    private static final String TYPE_TVSHOW = "tvshow";
    private static final String TYPE_EPISODE = "episodedetails";
    private static final String TYPE_ROOT = "xml";
    // Other properties
    private static final String splitPattern = "\\||,|/";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // Caters for the case where "-/" is not wanted as part of the split
    private static boolean skipNfoUrl;
    private static boolean skipNfoTrailer;
    private static String fanartToken;
    private static String fanartExtension;
    private static String forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", "AUTO");
    private static String NFOdirectory;
    private static boolean getCertificationFromMPAA;
    private static String imdbPreferredCountry;
    private static final boolean acceptAllNFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", "false");
    private static String nfoExtRegex;
    private static final String[] NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
    private static Pattern partPattern;
    private static String NFO_PLUGIN_ID = "NFO";
    private static boolean archiveScanRar;
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    private static String languageDelimiter = PropertiesUtil.getProperty("mjb.language.delimiter", Movie.SPACE_SLASH_SPACE);
    private static String subtitleDelimiter = PropertiesUtil.getProperty("mjb.subtitle.delimiter", Movie.SPACE_SLASH_SPACE);
    private static boolean skipTvNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.skipTVNFOFiles", "false");

    static {
        skipNfoUrl = PropertiesUtil.getBooleanProperty("filename.nfo.skipUrl", "true");
        skipNfoTrailer = PropertiesUtil.getBooleanProperty("filename.nfo.skipTrailer", "false");

        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        fanartExtension = PropertiesUtil.getProperty("fanart.format", "jpg");

        NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
        getCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", "true");
        imdbPreferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        if (acceptAllNFO) {
            logger.info(logMessage + "Accepting all NFO files in the directory");
        }

        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        {
            boolean first = true;
            StringBuilder sb = new StringBuilder("(?i).*\\.("); // Start of REGEX
            for (String ext : NFOExtensions) {
                if (first) {
                    first = false;
                } else {
                    sb.append("|"); // Add seperator
                }
                sb.append(ext).append("$"); // Add extension
            }
            sb.append(")"); // End of REGEX
            nfoExtRegex = sb.toString();
        }


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
            logger.debug(logMessage + "Scanning NFO file for IDs: " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirty(DirtyFlag.NFO, true);

            String nfo = FileTools.readFileToString(nfoFile);

            if (!parseXMLNFO(nfo, movie, nfoFile)) {
                DatabasePluginController.scanNFO(nfo, movie);

                logger.debug(logMessage + "Scanning NFO for Poster URL");
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
                                    logger.debug(logMessage + "Poster URL found in nfo = " + foundUrl);
                                    movie.setPosterURL(new String(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3)));
                                    urlStartIndex = -1;
                                    movie.setDirty(DirtyFlag.POSTER, true);
                                }
                            } else {
                                logger.debug(logMessage + "Poster URL ignored in NFO because it's a fanart URL");
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
     * @param movie The movie bean to locate the NFO for
     * @return A List structure of all the relevant NFO files
     */
    public static List<File> locateNFOs(Movie movie) {
        List<File> nfos = new ArrayList<File>();
        GenericFileFilter fFilter;

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
            pathFileName = new String(pathFileName.substring(0, pathFileName.lastIndexOf(".")));
            baseFileName = new String(baseFileName.substring(0, baseFileName.lastIndexOf(".")));
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfos, pathFileName + new String(pathFileName.substring(pathFileName.lastIndexOf(File.separator))));
        }

        if (movie.isTVShow() && !skipTvNfoFiles) {
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
            /*
             * If any NFO file in this directory will do, then we search for all
             * we can find NOTE: for scanning efficiency, it is better to first
             * search for specific filenames before we start doing filtered
             * "listfiles" which scans all the files; A movie collection with
             * all moviefiles in one directory could take tremendously longer if
             * for each moviefile found, the entire directory must be listed!!
             * Therefore, we first check for specific filenames (cfr. old
             * behaviour) before doing an entire scan of the directory -- and
             * only if the user has decided to accept any NFO file!
             */

            // Check the current directory
            fFilter = new GenericFileFilter(nfoExtRegex);
            checkRNFO(nfos, currentDir.getParentFile(), fFilter);

            // Also check the directory above, for the case where movies are in a multi-part named directory (CD/PART/DISK/Etc.)
            Matcher allNfoMatch = partPattern.matcher(currentDir.getAbsolutePath());
            if (allNfoMatch.find()) {
                logger.debug(logMessage + "Found multi-part directory, checking parent directory for NFOs");
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
     *
     * @param nfoFiles
     * @param currentDir
     * @param fFilter
     */
    private static void checkRNFO(List<File> nfoFiles, File currentDir, GenericFileFilter fFilter) {
        File[] fFiles = currentDir.listFiles(fFilter);
        if (fFiles != null && fFiles.length > 0) {
            for (File foundFile : fFiles) {
                logger.debug(logMessage + "Found " + foundFile.getName());
                nfoFiles.add(foundFile);
            }
        }
    }

    /**
     * Check to see if the passed filename exists with nfo extensions
     *
     * @param checkNFOfilename (NO EXTENSION)
     * @return blank string if not found, filename if found
     */
    private static void checkNFO(List<File> nfoFiles, String checkNFOfilename) {
        File nfoFile;

        for (String ext : NFOExtensions) {
            nfoFile = FileTools.fileCache.getFile(checkNFOfilename + "." + ext);
            if (nfoFile.exists()) {
                logger.debug(logMessage + "Found " + nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            }
        }
    }

    /**
     * Check the NFO file for any of the XML triggers that indicate it's a XML
     * file If found, process the NFO file as a XML NFO
     *
     * @param nfo
     * @param movie
     * @param nfoFile
     * @return
     */
    private static boolean parseXMLNFO(String nfo, Movie movie, File nfoFile) {
        if (nfo.indexOf("<" + TYPE_MOVIE) > -1 && parseMovieNFO(nfoFile, movie, nfo)) {
            return true;
        } else if (nfo.indexOf("<" + TYPE_TVSHOW) > -1 && parseTVNFO(nfoFile, movie)) {
            return true;
        } else if (nfo.indexOf("<" + TYPE_EPISODE) > -1 && parseTVNFO(nfoFile, movie)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create an XML reader for file. Use forced encoding if specified.
     *
     * @param nfoFile File to read.
     * @param nfo Content of the XML file. Used only for the encoding detection.
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

        XMLEventReader r = (!"AUTO".equalsIgnoreCase(forceNFOEncoding) && !fileContainsEncoding)
                ? factory.createXMLEventReader(FileTools.createFileInputStream(nfoFile), forceNFOEncoding)
                : factory.createXMLEventReader(FileTools.createFileInputStream(nfoFile));
        return r;
    }

    /**
     * Used to parse out the XBMC NFO XML data for movies The specification is
     * here: http://xbmc.org/wiki/?title=Import_-_Export_Library
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
                    if (tag.equalsIgnoreCase(TYPE_MOVIE)) {
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
                            Attribute movieDbAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            float ratingFloat = XMLHelper.parseFloat(r);
                            if (ratingFloat != 0.0f) {
                                int ratingInt;
                                if (ratingFloat <= 10f) {
                                    ratingInt = Math.round(ratingFloat * 10f);
                                } else {
                                    ratingInt = Math.round(ratingFloat * 1f);
                                }

                                if (movieDbAttribute != null) {
                                    // if we have a moviedb attribute
                                    movie.addRating(movieDbAttribute.getValue(), ratingInt);
                                } else {
                                    // Use "NFO" instead.
                                    movie.addRating(NFO_PLUGIN_ID, ratingInt);
                                }
                            }
                        } else if (tag.equalsIgnoreCase("year")) {
                            String val = XMLHelper.getCData(r);
                            if (StringUtils.isNumeric(val) && val.length() == 4) {
                                movie.setOverrideYear(true);
                                movie.setYear(val);
                            } else {
                                if (StringUtils.isNotBlank(val)) {
                                    logger.warn(logMessage + "Invalid year: '" + val + "' in " + nfoFile.getAbsolutePath());
                                }
                            }
                        } else if (tag.equalsIgnoreCase("premiered") || tag.equalsIgnoreCase("releasedate")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                try {
                                    if (val.length() == 4) {
                                        // Assume just the year an append "-01-01" to the end
                                        val += "-01-01";
                                        // Warn the user
                                        logger.debug(logMessage + "Partial date detected in premiered field of NFO for " + nfoFile.getAbsolutePath());
                                    }

                                    DateTime dateTime = new DateTime(val);

                                    movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                                    movie.setOverrideYear(true);
                                    movie.setYear(dateTime.toString("yyyy"));
                                } catch (Exception error) {
                                    logger.error(logMessage + "Failed parsing NFO file for movie: " + movie.getBaseFilename() + ". Please fix or remove it.");
                                    logger.error(logMessage + "premiered or releasedate does not contain a valid date.");
                                    logger.error(SystemTools.getStackTrace(error));
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
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setTagline(val);
                            }
                        } else if (tag.equalsIgnoreCase("runtime")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setRuntime(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb") && !skipNfoUrl) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart") && !skipNfoUrl) { // This is a custom YAMJ tag
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
                                    movie.setWatchedNFO(Boolean.parseBoolean(val));
                                } catch (Exception ignore) {
                                    // Don't change the watched status
                                }
                            }
                        } else if (tag.equalsIgnoreCase("fps")) {
                            String val = XMLHelper.getCData(r);
                            if (isValidString(val)) {
                                float fps;
                                try {
                                    fps = Float.parseFloat(val);
                                } catch (NumberFormatException error) {
                                    logger.warn("MovieNFOScanner: Error reading FPS value " + val);
                                    fps = 0.0f;
                                }
                                movie.setFps(fps);
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
                                    logger.debug(logMessage + "In parseMovieNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, val); // without attribute we assume it's an IMDB Id
                                    logger.debug(logMessage + "In parseMovieNFO Id=" + val + " found for default IMDB");
                                }

                                // Any value of 0 (Zero) or -1 will stop the movie being scraped
                                if (val.equals("0") || val.equals("-1")) {
                                    movie.setScrapeLibrary(false);
                                }
                            }
                            //} else if (tag.equalsIgnoreCase("filenameandpath")) {
                            // Not currently used
                        } else if (tag.equalsIgnoreCase("trailer") && !skipNfoTrailer) {
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
                            List<String> newGenres = StringTools.splitList(XMLHelper.getCData(r), SPLIT_GENRE);
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
                                for (String director : val.split(splitPattern)) {
                                    movie.addDirector(Movie.UNKNOWN, director, Movie.UNKNOWN);
                                }
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
                            String event = r.nextEvent().toString();
                            String name = Movie.UNKNOWN;
                            String role = Movie.UNKNOWN;

                            /*
                             * There can be multiple actors listed in the nfo in
                             * the format <actor> <name>Actor Name</name>
                             * <role>Character Name</role> <name>Actor
                             * Name</name> <role>Character Name</role> </actor>
                             */
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    // Check to see if we already have a name, and save the record if we do
                                    if (isValidString(name)) {
                                        movie.addActor(Movie.UNKNOWN, name, role, Movie.UNKNOWN, Movie.UNKNOWN);
                                        // Clear the name and role
                                        name = Movie.UNKNOWN;
                                        role = Movie.UNKNOWN;
                                    }

                                    // Get the actor name
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        name = val;
                                        role = Movie.UNKNOWN;
                                    }
                                } else if (event.equalsIgnoreCase("<role>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (isValidString(val)) {
                                        role = val;
                                    }
                                }

                                // Only write if we have a valid name and role
                                if (isValidString(name) && isValidString(role)) {
                                    movie.addActor(Movie.UNKNOWN, name, role, Movie.UNKNOWN, Movie.UNKNOWN);
                                }

                                if (r.hasNext()) {
                                    event = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                            // Save the last actor
                            if (isValidString(name)) {
                                movie.addActor(Movie.UNKNOWN, name, role, Movie.UNKNOWN, Movie.UNKNOWN);
                            }
                        } else if (tag.equalsIgnoreCase("fileinfo")) { // File Info Section
                            String fiEvent = r.nextEvent().toString();
                            String finalCodec = Movie.UNKNOWN;
                            StringBuilder finalLanguage = new StringBuilder();
                            String tmpSubtitleLanguage = Movie.UNKNOWN;
                            while (!fiEvent.equalsIgnoreCase("</fileinfo>")) {
                                if (fiEvent.equalsIgnoreCase("<video>")) {
                                    String nfoWidth = null;
                                    String nfoHeight = null;
                                    while (!fiEvent.equalsIgnoreCase("</video>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.addCodec(new Codec(Codec.CodecType.VIDEO, val));
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<aspect>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                movie.setAspectRatio(aspectTools.cleanAspectRatio(val));
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
                                    Codec audioCodec = new Codec(Codec.CodecType.AUDIO);

                                    while (!fiEvent.equalsIgnoreCase("</audio>")) {
                                        if (fiEvent.equalsIgnoreCase("<codec>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                // codec come first
                                                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                                                if (val.toLowerCase().equals(val)) {
                                                    val = val.toUpperCase();
                                                }
                                                audioCodec.setCodec(val);
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                audioCodec.setCodecLanguage(val);

                                                // Add the language to the list
                                                if (finalLanguage.length() > 0) {
                                                    finalLanguage.append(languageDelimiter);
                                                }
                                                finalLanguage.append(audioCodec.getCodecFullLanguage());
                                            }
                                        }

                                        if (fiEvent.equalsIgnoreCase("<channels>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (StringUtils.isNumeric(val) && val.length() > 0) {
                                                audioCodec.setCodecChannels(Integer.parseInt(val));
                                            }
                                        }

                                        if (r.hasNext()) {
                                            fiEvent = r.nextEvent().toString();
                                        }
                                    }
                                    // Parsing of audio end - setting data to movie.
                                    movie.addCodec(audioCodec);
                                }

                                // Add the language list to the movie
                                movie.setLanguage(finalLanguage.toString());

                                if (fiEvent.equalsIgnoreCase("<subtitle>")) {
                                    while (!fiEvent.equalsIgnoreCase("</subtitle>")) {
                                        if (fiEvent.equalsIgnoreCase("<language>")) {
                                            String val = XMLHelper.getCData(r);
                                            if (isValidString(val)) {
                                                // in case subtitle has format like "ENG / GER / ESP" process it now
                                                String[] tmpSubtitleLanguages = val.split("/");
                                                for (int i = 0; i < tmpSubtitleLanguages.length; i++) {
                                                    String subval = tmpSubtitleLanguages[i].trim();
                                                    if (isNotValidString(tmpSubtitleLanguage)) {
                                                        tmpSubtitleLanguage = MovieFilenameScanner.determineLanguage(subval);
                                                    } else {
                                                        tmpSubtitleLanguage = tmpSubtitleLanguage + subtitleDelimiter + MovieFilenameScanner.determineLanguage(subval);
                                                    }
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
                    if (e.asEndElement().getName().toString().equalsIgnoreCase(TYPE_MOVIE)) {
                        break;
                    }
                }
            }

            // We've processed all the NFO file, so work out what to do with the title and titleSort
            if (isValidString(titleMain)) {
                // We have a valid title, so set that for title and titleSort
                movie.setTitle(titleMain);
                movie.setTitleSort(titleMain);
                movie.setOverrideTitle(true);
            }

            // Now check the titleSort and overwrite it if necessary.
            if (isValidString(titleSort)) {
                movie.setTitleSort(titleSort);
            }

            return isMovieTag;
        } catch (Exception error) {
            logger.error(logMessage + "Failed parsing NFO file for video: " + movie.getBaseFilename() + ". Please fix or remove it.");
            logger.error(SystemTools.getStackTrace(error));
        }

        return false;
    }

    /**
     * Used to parse out the XBMC NFO XML data for TV series
     *
     * @param nfoFile
     * @param movie
     * @return
     */
    private static boolean parseTVNFO(File nfoFile, Movie movie) {
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getEventDocFromUrl(nfoFile);
        } catch (MalformedURLException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        } catch (IOException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        } catch (ParserConfigurationException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        } catch (SAXException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        }

        NodeList nlShows = xmlDoc.getElementsByTagName(TYPE_TVSHOW);
        Node nShow;
        NodeList nlElements;
        Node nElements;

        for (int loopMovie = 0; loopMovie < nlShows.getLength(); loopMovie++) {
            nShow = nlShows.item(loopMovie);
            if (nShow.getNodeType() == Node.ELEMENT_NODE) {
                Element eShow = (Element) nShow;

                movie.setTitle(DOMHelper.getValueFromElement(eShow, "title"));
                movie.setOriginalTitle(DOMHelper.getValueFromElement(eShow, "originaltitle"));
                movie.setTitleSort(DOMHelper.getValueFromElement(eShow, "sorttitle"));

                movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, DOMHelper.getValueFromElement(eShow, "tvdbid"));

                // Get all the IDs associated with the movie
                nlElements = eShow.getElementsByTagName("id");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eId = (Element) nElements;

                        String movieDb = eId.getAttribute("moviedb");
                        if (StringTools.isNotValidString(movieDb)) {
                            movieDb = ImdbPlugin.IMDB_PLUGIN_ID;
                        }
                        movie.setId(movieDb, eId.getTextContent());
                    }
                }   // End of ID

                try {
                    movie.setWatchedNFO(Boolean.parseBoolean(DOMHelper.getValueFromElement(eShow, "watched")));
                } catch (Exception ignore) {
                    // Don't change the watched status
                }

                // Get the sets
                nlElements = eShow.getElementsByTagName("set");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eId = (Element) nElements;

                        String setOrder = eId.getAttribute("order");
                        if (StringUtils.isNumeric(setOrder)) {
                            movie.addSet(eId.getTextContent(), Integer.parseInt(setOrder));
                        } else {
                            movie.addSet(eId.getTextContent());
                        }
                    }
                }   // End of SET

                // Rating
                String ratingString = DOMHelper.getValueFromElement(eShow, "rating");
                if (StringTools.isValidString(ratingString)) {
                    try {
                        float rating = Float.parseFloat(ratingString);
                        if (rating != 0.0f) {
                            if (rating <= 10.0f) {
                                movie.addRating(NFO_PLUGIN_ID, Math.round(rating * 10f));
                            } else {
                                movie.addRating(NFO_PLUGIN_ID, Math.round(rating * 1f));
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        logger.error("Failed parsing rating in NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
                    }
                }

                // Certification
                if (getCertificationFromMPAA) {
                    String val = DOMHelper.getValueFromElement(eShow, "mpaa");
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
                } else {
                    movie.setCertification(DOMHelper.getValueFromElement(eShow, "certification"));
                }

                // Plot
                movie.setPlot(DOMHelper.getValueFromElement(eShow, "plot"));

                // Genres - Caters for multiple genres on the same line and multiple lines
                nlElements = eShow.getElementsByTagName("genre");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eGenre = (Element) nElements;
                        movie.addGenres(StringTools.splitList(eGenre.getTextContent(), SPLIT_GENRE));
                    }
                }   // End of GENRES

                // Premiered & Release Date
                movieDate(movie, DOMHelper.getValueFromElement(eShow, "premiered"));
                movieDate(movie, DOMHelper.getValueFromElement(eShow, "releasedate"));

                movie.setQuote(DOMHelper.getValueFromElement(eShow, "quote"));
                movie.setTagline(DOMHelper.getValueFromElement(eShow, "tagline"));
                movie.setCompany(DOMHelper.getValueFromElement(eShow, "studio"));
                movie.setCompany(DOMHelper.getValueFromElement(eShow, "company"));
                movie.setCountry(DOMHelper.getValueFromElement(eShow, "country"));

                // Poster and Fanart
                if (!skipNfoUrl) {
                    movie.setPosterURL(DOMHelper.getValueFromElement(eShow, "thumb"));
                    movie.setFanartURL(DOMHelper.getValueFromElement(eShow, "fanart"));
                    // Not sure this is needed
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }

                // Trailers
                if (!skipNfoTrailer) {
                    nlElements = eShow.getElementsByTagName("trailer");
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eTrailer = (Element) nElements;

                            String trailer = eTrailer.getTextContent().trim();
                            if (!trailer.isEmpty()) {
                                ExtraFile ef = new ExtraFile();
                                ef.setNewFile(false);
                                ef.setFilename(trailer);
                                movie.addExtraFile(ef);
                            }
                        }
                    }   // End of TRAILER
                }

                // Actors
                nlElements = eShow.getElementsByTagName("actor");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eActor = (Element) nElements;

                        String aName = DOMHelper.getValueFromElement(eActor, "name");
                        String aRole = DOMHelper.getValueFromElement(eActor, "role");
                        String aThumb = DOMHelper.getValueFromElement(eActor, "thumb");

                        // This will add to the person and actor
                        movie.addActor(Movie.UNKNOWN, aName, aRole, aThumb, Movie.UNKNOWN);
                    }
                }   // End of Actors

                // Credits/Writer
                nlElements = eShow.getElementsByTagName("writer");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eWriter = (Element) nElements;
                        movie.addWriter(eWriter.getTextContent());
                    }
                }   // End of Credits/Writer

                // Director
                nlElements = eShow.getElementsByTagName("writer");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eDirector = (Element) nElements;
                        movie.addDirector(eDirector.getTextContent());
                    }
                }   // Director

                String fpsString = DOMHelper.getValueFromElement(eShow, "fps");
                if (isValidString(fpsString)) {
                    float fps;
                    try {
                        fps = Float.parseFloat(fpsString);
                    } catch (NumberFormatException error) {
                        logger.warn("MovieNFOScanner: Error reading FPS value " + fpsString);
                        fps = 0.0f;
                    }
                    movie.setFps(fps);
                }

                // VideoSource: Issue 506 - Even though it's not strictly XBMC standard
                movie.setVideoSource(DOMHelper.getValueFromElement(eShow, "VideoSource"));

                // Video Output
                movie.setVideoOutput(DOMHelper.getValueFromElement(eShow, "VideoOutput"));

                // Parse the video info
                parseFileInfo(movie, DOMHelper.getElementByName(eShow, "fileinfo"));
            }
        }   // End of TVSHOW

        parseAllEpisodeDetails(movie, xmlDoc.getElementsByTagName("episodedetails"));

        return true;
    }

    /**
     * Parse the FileInfo section
     *
     * @param movie
     * @param eFileInfo
     */
    private static void parseFileInfo(Movie movie, Element eFileInfo) {
        Element eStreamDetails = DOMHelper.getElementByName(eFileInfo, "streamdetails");

        // Video
        NodeList nlStreams = eStreamDetails.getElementsByTagName("video");
        Node nStreams;
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String temp = DOMHelper.getValueFromElement(eStreams, "codec");
                if (isValidString(temp)) {
                    movie.addCodec(new Codec(Codec.CodecType.VIDEO, temp));
                }

                temp = DOMHelper.getValueFromElement(eStreams, "aspect");
                movie.setAspectRatio(aspectTools.cleanAspectRatio(temp));

                movie.setResolution(DOMHelper.getValueFromElement(eStreams, "width"), DOMHelper.getValueFromElement(eStreams, "height"));
            }
        } // End of VIDEO

        // Audio
        nlStreams = eStreamDetails.getElementsByTagName("audio");

        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String aCodec = DOMHelper.getValueFromElement(eStreams, "codec");
                String aLanguage = DOMHelper.getValueFromElement(eStreams, "language");
                String aChannels = DOMHelper.getValueFromElement(eStreams, "channels");

                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                if (aCodec.toLowerCase().equals(aCodec)) {
                    aCodec = aCodec.toUpperCase();
                }

                if (StringTools.isValidString(aLanguage)) {
                    aLanguage = MovieFilenameScanner.determineLanguage(aLanguage);
                }

                Codec audioCodec = new Codec(Codec.CodecType.AUDIO, aCodec);
                audioCodec.setCodecLanguage(aLanguage);
                audioCodec.setCodecChannels(aChannels);
                movie.addCodec(audioCodec);
            }
        } // End of AUDIO

        // Update the language
        StringBuilder movieLanguage = new StringBuilder();
        boolean first = Boolean.TRUE;
        for (Codec codec : movie.getCodecs()) {
            if (codec.getCodecType() == Codec.CodecType.AUDIO) {
                if (first) {
                    first = Boolean.FALSE;
                } else {
                    movieLanguage.append(languageDelimiter);
                }
                movieLanguage.append(codec.getCodecLanguage());
            }
        }
        movie.setLanguage(movieLanguage.toString());

        // Subtitles
        List<String> subs = new ArrayList<String>();
        nlStreams = eStreamDetails.getElementsByTagName("subtitle");
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;
                subs.add(DOMHelper.getValueFromElement(eStreams, "language"));
            }
        }

        // If we have some subtitles, add them to the movie
        if (!subs.isEmpty()) {
            StringBuilder movieSubs = new StringBuilder();
            first = Boolean.TRUE;
            for (String subtitle : subs) {
                if (first) {
                    first = Boolean.FALSE;
                } else {
                    movieSubs.append(subtitleDelimiter);
                }
                movieSubs.append(subtitle);
            }
            movie.setSubtitles(movieSubs.toString());
        }

    }

    /**
     * Process all the Episode Details
     *
     * @param movie
     * @param nlEpisodeDetails
     */
    private static void parseAllEpisodeDetails(Movie movie, NodeList nlEpisodeDetails) {
        Node nEpisodeDetails;
        for (int looper = 0; looper < nlEpisodeDetails.getLength(); looper++) {
            nEpisodeDetails = nlEpisodeDetails.item(looper);
            if (nEpisodeDetails.getNodeType() == Node.ELEMENT_NODE) {
                Element eEpisodeDetail = (Element) nEpisodeDetails;
                parseSingleEpisodeDetail(eEpisodeDetail).updateMovie(movie);
            }
        }
    }

    /**
     * Parse a single episode detail element
     *
     * @param movie
     * @param eEpisodeDetails
     * @return
     */
    private static EpisodeDetail parseSingleEpisodeDetail(Element eEpisodeDetails) {
        EpisodeDetail epDetail = new EpisodeDetail();

        epDetail.setTitle(DOMHelper.getValueFromElement(eEpisodeDetails, "title"));

        String tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "season");
        if (StringUtils.isNumeric(tempValue)) {
            epDetail.setSeason(Integer.parseInt(tempValue));
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "episode");
        if (StringUtils.isNumeric(tempValue)) {
            epDetail.setEpisode(Integer.parseInt(tempValue));
        }

        epDetail.setPlot(DOMHelper.getValueFromElement(eEpisodeDetails, "plot"));

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "rating");
        if (StringUtils.isNotBlank(tempValue)) {
            try {
                float rating = Float.parseFloat(tempValue);
                if (rating != 0.0f) {
                    if (rating <= 10f) {
                        String val = String.valueOf(Math.round(rating * 10f));
                        epDetail.setRating(val);
                    } else {
                        String val = String.valueOf(Math.round(rating * 1f));
                        epDetail.setRating(val);
                    }
                }
            } catch (NumberFormatException ex) {
                logger.debug("Failed to convert rating '" + tempValue + "'");
            }
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "aired");
        if (isValidString(tempValue)) {
            try {
                DateTime dateTime = new DateTime(tempValue);
                epDetail.setFirstAired(dateTime.toString(Movie.dateFormatString));
            } catch (Exception ignore) {
                // Set the aired date if there is an exception
                epDetail.setFirstAired(tempValue);
            }
        }

        epDetail.setAirsAfterSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsafterseason"));
        epDetail.setAirsBeforeEpisode(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeepisode"));
        epDetail.setAirsBeforeSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeseason"));

        return epDetail;
    }

    /**
     * Convert the date string to a date and update the movie object
     *
     * @param movie
     * @param dateString
     */
    private static void movieDate(Movie movie, String dateString) {
        if (StringTools.isValidString(dateString)) {
            try {
                DateTime dateTime = new DateTime(dateString);

                movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                movie.setOverrideYear(true);
                movie.setYear(dateTime.toString("yyyy"));
            } catch (Exception ignore) {
                // Set the release date if there is an exception
                movie.setReleaseDate(dateString);
            }
        }
    }

    /**
     * Take a file and wrap it in a new root element
     *
     * @param fileString
     * @return
     */
    public static String wrapInXml(String fileString) {
        StringBuilder newOutput = new StringBuilder(fileString);

        int posMovie = fileString.indexOf("<" + TYPE_MOVIE);
        int posTvShow = fileString.indexOf("<" + TYPE_TVSHOW);
        int posEpisode = fileString.indexOf("<" + TYPE_EPISODE);
        boolean posValid = Boolean.FALSE;

        if (posMovie == -1) {
            posMovie = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posTvShow == -1) {
            posTvShow = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posEpisode == -1) {
            posEpisode = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posValid) {
            int pos = Math.min(posMovie, Math.min(posTvShow, posEpisode));
            newOutput.insert(pos, "<" + TYPE_ROOT + ">");
            newOutput.append("</").append(TYPE_ROOT).append(">");
        }

        return newOutput.toString();
    }
}
