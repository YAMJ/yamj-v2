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
import static com.moviejukebox.tools.StringTools.appendToPath;
import static com.moviejukebox.tools.StringTools.isValidString;
import com.moviejukebox.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
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
    private static final String xbmcTvNfoName = "tvshow";
    private static final String splitPattern = "\\||,|/";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // Caters for the case where "-/" is not wanted as part of the split
    private static boolean skipNfoUrl = PropertiesUtil.getBooleanProperty("filename.nfo.skipUrl", "true");
    private static boolean skipNfoTrailer = PropertiesUtil.getBooleanProperty("filename.nfo.skipTrailer", "false");
    // For now, this is deprecated and we should see if there are issues before looking at a solution as the DOM Parser seems a lot more stable
//    private static String forceNFOEncoding = PropertiesUtil.getProperty("mjb.forceNFOEncoding", "AUTO");
    private static String NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");
    private static boolean getCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", "true");
    private static String imdbPreferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
    private static final boolean acceptAllNFO = PropertiesUtil.getBooleanProperty("filename.nfo.acceptAllNfo", "false");
    private static String nfoExtRegex;
    private static final String[] NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").split(",");
    private static Pattern partPattern = Pattern.compile("(?i)(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)");
    private static String NFO_PLUGIN_ID = "NFO";
    private static boolean archiveScanRar = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", "false");
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    private static String languageDelimiter = PropertiesUtil.getProperty("mjb.language.delimiter", Movie.SPACE_SLASH_SPACE);
    private static String subtitleDelimiter = PropertiesUtil.getProperty("mjb.subtitle.delimiter", Movie.SPACE_SLASH_SPACE);
    private static boolean skipTvNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.skipTVNFOFiles", "false");

    static {
        if (acceptAllNFO) {
            logger.info(logMessage + "Accepting all NFO files in the directory");
        }

        // Construct regex for filtering NFO files
        // Target format is: ".*\\(ext1|ext2|ext3|..|extN)"
        {
            boolean first = Boolean.TRUE;
            StringBuilder regexBuilder = new StringBuilder("(?i).*\\.("); // Start of REGEX
            for (String ext : NFOExtensions) {
                if (first) {
                    first = Boolean.FALSE;
                } else {
                    regexBuilder.append("|"); // Add seperator
                }
                regexBuilder.append(ext).append("$"); // Add extension
            }
            regexBuilder.append(")"); // End of REGEX
            nfoExtRegex = regexBuilder.toString();
        }

        // Set the date format to dd-MM-yyyy
        DateTimeConfig.globalEuropeanDateFormat();
    }

    /**
     * Process the NFO file.
     *
     * Will either process the file as an XML NFO file or just pick out the poster and fanart URLs
     *
     * Scanning for site specific URLs should be done by each plugin
     *
     * @param movie
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.debug(logMessage + "Scanning NFO file for IDs: " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirty(DirtyFlag.NFO);

            String nfo = FileTools.readFileToString(nfoFile);
            boolean parsedXmlNfo = Boolean.FALSE;   // Was the NFO XML parsed correctly or at all

            if (StringUtils.containsIgnoreCase(nfo, "<" + TYPE_MOVIE + ">")
                    || StringUtils.containsIgnoreCase(nfo, "<" + TYPE_TVSHOW + ">")
                    || StringUtils.containsIgnoreCase(nfo, "<" + TYPE_EPISODE + ">")) {
                parsedXmlNfo = parseNfo(nfoFile, movie);
            }

            // If the XML wasn't found or parsed correctly, then fall back to the old method
            if (!parsedXmlNfo) {
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
                                    movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
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
        List<File> nfoFiles = new ArrayList<File>();
        GenericFileFilter fFilter;

        File currentDir = movie.getFirstFile().getFile();

        if (currentDir == null) {
            return nfoFiles;
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
            pathFileName = FilenameUtils.removeExtension(pathFileName);
            baseFileName = FilenameUtils.removeExtension(baseFileName);
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfoFiles, pathFileName + new String(pathFileName.substring(pathFileName.lastIndexOf(File.separator))));
        }

        // TV Show specific scanning
        if (movie.isTVShow()) {
            // Check for the "tvshow.nfo" filename in the parent directory
            checkNFO(nfoFiles, movie.getFile().getParentFile().getParent() + File.separator + xbmcTvNfoName);

            // Check for individual episode files
            if (!skipTvNfoFiles) {
                String mfFilename;

                for (MovieFile mf : movie.getMovieFiles()) {
                    mfFilename = mf.getFile().getParent().toUpperCase();

                    if (mfFilename.contains("BDMV")) {
                        mfFilename = FileTools.getParentFolder(mf.getFile());
                        mfFilename = new String(mfFilename.substring(mfFilename.lastIndexOf(File.separator) + 1));
                    } else {
                        mfFilename = FilenameUtils.removeExtension(mf.getFile().getName());
                    }

                    checkNFO(nfoFiles, mf.getFile().getParent() + File.separator + mfFilename);
                }
            }
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfoFiles, pathFileName);

        if (isValidString(NFOdirectory)) {
            // *** Next step if we still haven't found the nfo file is to
            // search the NFO directory as specified in the moviejukebox.properties file
            String sNFOPath = FileTools.getDirPathWithSeparator(movie.getLibraryPath()) + NFOdirectory;
            checkNFO(nfoFiles, sNFOPath + File.separator + baseFileName);
        }

        // *** Next step is to check for a directory wide NFO file.
        if (acceptAllNFO) {
            /*
             * If any NFO file in this directory will do, then we search for all we can find
             *
             * NOTE: for scanning efficiency, it is better to first search for specific filenames before we start doing
             * filtered "listfiles" which scans all the files;
             *
             * A movie collection with all moviefiles in one directory could take tremendously longer if for each
             * moviefile found, the entire directory must be listed!!
             *
             * Therefore, we first check for specific filenames (cfr. old behaviour) before doing an entire scan of the
             * directory -- and only if the user has decided to accept any NFO file!
             */

            // Check the current directory
            fFilter = new GenericFileFilter(nfoExtRegex);
            checkRNFO(nfoFiles, currentDir.getParentFile(), fFilter);

            // Also check the directory above, for the case where movies are in a multi-part named directory (CD/PART/DISK/Etc.)
            Matcher allNfoMatch = partPattern.matcher(currentDir.getAbsolutePath());
            if (allNfoMatch.find()) {
                logger.debug(logMessage + "Found multi-part directory, checking parent directory for NFOs");
                checkRNFO(nfoFiles, currentDir.getParentFile().getParentFile(), fFilter);
            }
        } else {
            // This file should be named the same as the directory that it is in
            // E.G. C:\TV\Chuck\Season 1\Season 1.nfo
            // We search up through all containing directories up to the library root

            if (currentDir != null) {
                // Check the current directory for the video filename
                fFilter = new GenericFileFilter("(?i)" + movie.getBaseFilename() + nfoExtRegex);
                checkRNFO(nfoFiles, currentDir, fFilter);
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
                        checkNFO(nfoFiles, appendToPath(path, currentDir.getName()));
                    }
                }
            }
        }

        // we added the most specific ones first, and we want to parse those the last,
        // so nfo files in sub-directories can override values in directories above.
        Collections.reverse(nfoFiles);

        return nfoFiles;
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
     * Used to parse out the XBMC NFO XML data for the XML NFO files.
     *
     * This is generic for movie and TV show files as they are both nearly identical.
     *
     * @param nfoFile
     * @param movie
     */
    private static boolean parseNfo(File nfoFile, Movie movie) {
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getEventDocFromUrl(nfoFile);
        } catch (MalformedURLException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (IOException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (ParserConfigurationException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (SAXException error) {
            logger.error("Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        NodeList nlMovies;
        if (movie.isTVShow()) {
            nlMovies = xmlDoc.getElementsByTagName(TYPE_TVSHOW);
        } else {
            nlMovies = xmlDoc.getElementsByTagName(TYPE_MOVIE);
        }
        Node nMovie;
        for (int loopMovie = 0; loopMovie < nlMovies.getLength(); loopMovie++) {
            nMovie = nlMovies.item(loopMovie);
            if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                Element eCommon = (Element) nMovie;

                // Get all of the title elements from the NFO file
                parseTitle(eCommon, movie);

                String tempYear = DOMHelper.getValueFromElement(eCommon, "year");
                if (!parseYear(tempYear, movie)) {
                    logger.warn(logMessage + "Invalid year: '" + tempYear + "' in " + nfoFile.getAbsolutePath());
                }

                // ID specific to TV Shows
                if (movie.isTVShow()) {
                    String tvdbid = DOMHelper.getValueFromElement(eCommon, "tvdbid");
                    if (isValidString(tvdbid)) {
                        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, tvdbid);
                    }
                }

                // Get all of the other IDs
                parseIds(eCommon.getElementsByTagName("id"), movie);

                // Get the watched status
                try {
                    movie.setWatchedNFO(Boolean.parseBoolean(DOMHelper.getValueFromElement(eCommon, "watched")));
                } catch (Exception ignore) {
                    // Don't change the watched status
                }

                // Get the sets
                parseSets(eCommon.getElementsByTagName("set"), movie);

                // Rating
                if (!parseRating(DOMHelper.getValueFromElement(eCommon, "rating"), movie)) {
                    logger.error("Failed parsing rating in NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
                }

                // Runtime
                movie.setRuntime(DOMHelper.getValueFromElement(eCommon, "runtime"));

                // Certification
                parseCertification(eCommon, movie);

                // Plot
                movie.setPlot(DOMHelper.getValueFromElement(eCommon, "plot"));

                // Outline
                movie.setOutline(DOMHelper.getValueFromElement(eCommon, "outline"));

                parseGenres(eCommon.getElementsByTagName("genre"), movie);

                // Premiered & Release Date
                movieDate(movie, DOMHelper.getValueFromElement(eCommon, "premiered"));
                movieDate(movie, DOMHelper.getValueFromElement(eCommon, "releasedate"));

                movie.setQuote(DOMHelper.getValueFromElement(eCommon, "quote"));
                movie.setTagline(DOMHelper.getValueFromElement(eCommon, "tagline"));
                movie.setCompany(DOMHelper.getValueFromElement(eCommon, "studio"));
                movie.setCompany(DOMHelper.getValueFromElement(eCommon, "company"));
                movie.setCountry(DOMHelper.getValueFromElement(eCommon, "country"));

                if (!movie.isTVShow()) {
                    String tempTop250 = DOMHelper.getValueFromElement(eCommon, "top250");
                    if (StringUtils.isNumeric(tempTop250)) {
                        movie.setTop250(Integer.parseInt(tempTop250));
                    }
                }

                // Poster and Fanart
                if (!skipNfoUrl) {
                    movie.setPosterURL(DOMHelper.getValueFromElement(eCommon, "thumb"));
                    movie.setFanartURL(DOMHelper.getValueFromElement(eCommon, "fanart"));
                    // Not sure this is needed
                    // movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }

                // Trailers
                if (!skipNfoTrailer) {
                    parseTrailers(eCommon.getElementsByTagName("trailer"), movie);
                }

                // Actors
                parseActors(eCommon.getElementsByTagName("actor"), movie);

                // Credits/Writer
                parseWriters(eCommon.getElementsByTagName("writer"), movie);

                // Director
                parseDirectors(eCommon.getElementsByTagName("director"), movie);

                String tempString = DOMHelper.getValueFromElement(eCommon, "fps");
                if (isValidString(tempString)) {
                    float fps;
                    try {
                        fps = Float.parseFloat(tempString);
                    } catch (NumberFormatException error) {
                        logger.warn("MovieNFOScanner: Error reading FPS value " + tempString);
                        fps = 0.0f;
                    }
                    movie.setFps(fps);
                }

                // VideoSource: Issue 506 - Even though it's not strictly XBMC standard
                tempString = DOMHelper.getValueFromElement(eCommon, "VideoSource");
                if (StringTools.isValidString(tempString)) {
                    movie.setVideoSource(tempString);
                }

                // Video Output
                tempString = DOMHelper.getValueFromElement(eCommon, "VideoOutput");
                if (StringTools.isValidString(tempString)) {
                    movie.setVideoOutput(tempString);
                }

                // Parse the video info
                parseFileInfo(movie, DOMHelper.getElementByName(eCommon, "fileinfo"));
            }
        }

        // Parse the episode details
        if (movie.isTVShow()) {
            parseAllEpisodeDetails(movie, xmlDoc.getElementsByTagName("episodedetails"));
        }

        return Boolean.TRUE;
    }

    /**
     * Parse the FileInfo section
     *
     * @param movie
     * @param eFileInfo
     */
    private static void parseFileInfo(Movie movie, Element eFileInfo) {
        if (eFileInfo == null) {
            return;
        }

        String container = DOMHelper.getValueFromElement(eFileInfo, "container");
        if (StringTools.isValidString(container)) {
            movie.setContainer(container);
        }

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
        for (Codec codec : movie.getCodecs()) {
            if (codec.getCodecType() == Codec.CodecType.AUDIO) {
                if (movieLanguage.length() > 0) {
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
            for (String subtitle : subs) {
                if (movieSubs.length() > 0) {
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
                if (dateString.length() == 4) {
                    // Assume just the year an append "-01-01" to the end
                    dateString += "-01-01";
                    // Warn the user
                    logger.debug(logMessage + "Partial date detected in premiered field of NFO for " + movie.getBaseFilename());
                }

                DateTime dateTime = new DateTime(dateString);

                movie.setReleaseDate(dateTime.toString(Movie.dateFormatString));
                movie.setOverrideYear(Boolean.TRUE);
                movie.setYear(dateTime.toString("yyyy"));
            } catch (Exception ex) {
                logger.warn(logMessage + "Failed parsing NFO file for movie: " + movie.getBaseFilename() + ". Please fix or remove it.");
                logger.warn(logMessage + "premiered or releasedate does not contain a valid date: " + dateString);
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

    /**
     * Parse Genres from the XML NFO file
     *
     * Caters for multiple genres on the same line and multiple lines.
     *
     * @param nlElements
     * @param movie
     */
    private static void parseGenres(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eGenre = (Element) nElements;
                movie.addGenres(StringTools.splitList(eGenre.getTextContent(), SPLIT_GENRE));
            }
        }
    }

    /**
     * Parse Actors from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseActors(NodeList nlElements, Movie movie) {
        Node nElements;
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
        }
    }

    /**
     * Parse Writers from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseWriters(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eWriter = (Element) nElements;
                movie.addWriter(eWriter.getTextContent());
            }
        }
    }

    /**
     * Parse Directors from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseDirectors(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eDirector = (Element) nElements;
                movie.addDirector(eDirector.getTextContent());
            }
        }
    }

    /**
     * Parse Trailers from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseTrailers(NodeList nlElements, Movie movie) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eTrailer = (Element) nElements;

                String trailer = eTrailer.getTextContent().trim();
                if (!trailer.isEmpty()) {
                    ExtraFile ef = new ExtraFile();
                    ef.setNewFile(Boolean.FALSE);
                    ef.setFilename(trailer);
                    movie.addExtraFile(ef);
                }
            }
        }
    }

    /**
     * Parse Sets from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseSets(NodeList nlElements, Movie movie) {
        Node nElements;
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
        }
    }

    /**
     * Parse Certification from the XML NFO file
     *
     * @param eCommon
     * @param movie
     */
    private static void parseCertification(Element eCommon, Movie movie) {
        String tempCert;
        if (getCertificationFromMPAA) {
            tempCert = DOMHelper.getValueFromElement(eCommon, "mpaa");
            if (isValidString(tempCert)) {
                // Issue 333
                if (tempCert.startsWith("Rated ")) {
                    int start = 6; // "Rated ".length()
                    int pos = tempCert.indexOf(" on appeal for ", start);

                    if (pos == -1) {
                        pos = tempCert.indexOf(" for ", start);
                    }

                    if (pos > start) {
                        tempCert = new String(tempCert.substring(start, pos));
                    } else {
                        tempCert = new String(tempCert.substring(start));
                    }
                }
                movie.setCertification(tempCert);
            }
        } else {
            tempCert = DOMHelper.getValueFromElement(eCommon, "certification");

            if (isValidString(tempCert)) {
                int countryPos = tempCert.lastIndexOf(imdbPreferredCountry);
                if (countryPos > 0) {
                    // We've found the country, so extract just that tag
                    tempCert = new String(tempCert.substring(countryPos));
                    int pos = tempCert.indexOf(":");
                    if (pos > 0) {
                        int endPos = tempCert.indexOf(" /");
                        if (endPos > 0) {
                            // This is in the middle of the string
                            tempCert = new String(tempCert.substring(pos + 1, endPos));
                        } else {
                            // This is at the end of the string
                            tempCert = new String(tempCert.substring(pos + 1));
                        }
                    }
                } else {
                    // The country wasn't found in the value, so grab the last one
                    int pos = tempCert.lastIndexOf(":");
                    if (pos > 0) {
                        // Strip the country code from the rating for certification like "UK:PG-12"
                        tempCert = new String(tempCert.substring(pos + 1));
                    }
                }

                movie.setCertification(tempCert);
            }
        }
    }

    /**
     * Parse the rating from the passed string and normalise it
     *
     * @param ratingString
     * @param movie
     * @return true if the rating was successfully parsed.
     */
    private static Boolean parseRating(String ratingString, Movie movie) {
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
                return Boolean.TRUE;
            } catch (NumberFormatException nfe) {
                return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Parse all the IDs associated with the movie from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseIds(NodeList nlElements, Movie movie) {
        Node nElements;
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
        }
    }

    /**
     * Parse all the title information from the XML NFO file
     *
     * @param eCommon
     * @param movie
     */
    private static void parseTitle(Element eCommon, Movie movie) {
        // Determine title elements
        String titleMain = DOMHelper.getValueFromElement(eCommon, "title");
        String titleSort = DOMHelper.getValueFromElement(eCommon, "sorttitle");
        String titleOrig = DOMHelper.getValueFromElement(eCommon, "originaltitle");

        if (isValidString(titleOrig)) {
            movie.setOriginalTitle(titleOrig);
        }

        // Work out what to do with the title and titleSort
        if (isValidString(titleMain)) {
            // We have a valid title, so set that for title and titleSort
            movie.setTitle(titleMain);
            movie.setTitleSort(titleMain);
            movie.setOverrideTitle(Boolean.TRUE);
        }

        // Now check the titleSort and overwrite it if necessary.
        if (isValidString(titleSort)) {
            movie.setTitleSort(titleSort);
        }
    }

    /**
     * Parse the year from the XML NFO file
     *
     * @param tempYear
     * @param movie
     * @return
     */
    private static boolean parseYear(String tempYear, Movie movie) {
        // START year
        if (StringUtils.isNumeric(tempYear) && tempYear.length() == 4) {
            movie.setOverrideYear(Boolean.TRUE);
            movie.setYear(tempYear);
            return Boolean.TRUE;
        } else {
            if (StringUtils.isNotBlank(tempYear)) {
                return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }
}
