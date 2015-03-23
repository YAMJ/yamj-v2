/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.reader;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.EpisodeDetail;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.CodecSource;
import com.moviejukebox.model.enumerations.CodecType;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.AspectRatioTools;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.isValidString;
import com.moviejukebox.tools.SubtitleTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class to read the NFO files
 *
 * @author stuart.boston
 */
public final class MovieNFOReader {

    private static final Logger LOG = LoggerFactory.getLogger(MovieNFOReader.class);
    /**
     * Node Type: Movie
     */
    public static final String TYPE_MOVIE = "movie";
    /**
     * Node Type: TV Show
     */
    public static final String TYPE_TVSHOW = "tvshow";
    /**
     * Node Type: Episode
     */
    public static final String TYPE_EPISODE = "episodedetails";
    /**
     * Plugin ID
     */
    public static final String NFO_PLUGIN_ID = "NFO";
    // Other properties
    private static final String XML_START = "<";
    private static final String XML_END = "</";
    private static final String ERROR_FIXIT = "Failed parsing NFO file: {}. Does not seem to be an XML format. Error: {}";
    private static final boolean SKIP_NFO_URL = PropertiesUtil.getBooleanProperty("filename.nfo.skipUrl", Boolean.TRUE);
    private static final boolean SKIP_NFO_TRAILER = PropertiesUtil.getBooleanProperty("filename.nfo.skipTrailer", Boolean.FALSE);
    private static final boolean SKIP_NFO_CAST = PropertiesUtil.getBooleanProperty("filename.nfo.skipCast", Boolean.FALSE);
    private static final boolean SKIP_NFO_CREW = PropertiesUtil.getBooleanProperty("filename.nfo.skipCrew", Boolean.FALSE);
    private static final AspectRatioTools ASPECT_TOOLS = new AspectRatioTools();
    private static final boolean CERT_FROM_MPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", Boolean.TRUE);
    private static final String IMDB_PREFERRED_COUNTRY = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
    private static final String LANGUAGE_DELIMITER = PropertiesUtil.getProperty("mjb.language.delimiter", Movie.SPACE_SLASH_SPACE);
    // Fanart settings
    private static final String FANART_TOKEN = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
    private static final String FANART_EXT = PropertiesUtil.getProperty("fanart.format", "jpg");
    // Patterns
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // Caters for the case where "-/" is not wanted as part of the split
    // Max People values
    private static final int MAX_COUNT_ACTOR = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", 10);

    /**
     * This is a utility class and cannot be instantiated.
     */
    private MovieNFOReader() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Try and read a NFO file for information
     *
     * First try as XML format file, then check to see if it contains XML and
     * text and split it to read each part
     *
     * @param nfoFile
     * @param movie
     * @return
     */
    public static boolean readNfoFile(File nfoFile, Movie movie) {
        String nfoText = FileTools.readFileToString(nfoFile);
        boolean parsedNfo = Boolean.FALSE;   // Was the NFO XML parsed correctly or at all
        boolean hasXml = Boolean.FALSE;

        if (StringUtils.containsIgnoreCase(nfoText, XML_START + TYPE_MOVIE)
                || StringUtils.containsIgnoreCase(nfoText, XML_START + TYPE_TVSHOW)
                || StringUtils.containsIgnoreCase(nfoText, XML_START + TYPE_EPISODE)) {
            hasXml = Boolean.TRUE;
        }

        // If the file has XML tags in it, try reading it as a pure XML file
        if (hasXml) {
            parsedNfo = readXmlNfo(nfoFile, movie);
        }

        // If it has XML in it, but didn't parse correctly, try splitting it out
        if (hasXml && !parsedNfo) {
            int posMovie = findPosition(nfoText, TYPE_MOVIE);
            int posTv = findPosition(nfoText, TYPE_TVSHOW);
            int posEp = findPosition(nfoText, TYPE_EPISODE);
            int start = Math.min(posMovie, Math.min(posTv, posEp));

            posMovie = StringUtils.indexOf(nfoText, XML_END + TYPE_MOVIE);
            posTv = StringUtils.indexOf(nfoText, XML_END + TYPE_TVSHOW);
            posEp = StringUtils.indexOf(nfoText, XML_END + TYPE_EPISODE);
            int end = Math.max(posMovie, Math.max(posTv, posEp));

            if ((end > -1) && (end > start)) {
                end = StringUtils.indexOf(nfoText, '>', end) + 1;

                // Send text to be read
                String nfoTrimmed = StringUtils.substring(nfoText, start, end);
                parsedNfo = readXmlNfo(nfoTrimmed, movie, nfoFile.getName());

                nfoTrimmed = StringUtils.remove(nfoText, nfoTrimmed);
                if (parsedNfo && nfoTrimmed.length() > 0) {
                    // We have some text left, so scan that with the text scanner
                    readTextNfo(nfoTrimmed, movie);
                }
            }
        }

        // If the XML wasn't found or parsed correctly, then fall back to the old method
        if (parsedNfo) {
            LOG.debug("Successfully scanned {} as XML format", nfoFile.getName());
        } else {
            parsedNfo = MovieNFOReader.readTextNfo(nfoText, movie);
            if (parsedNfo) {
                LOG.debug("Successfully scanned {} as text format", nfoFile.getName());
            } else {
                LOG.debug("Failed to find any information in {}", nfoFile.getName());
            }
        }

        return Boolean.FALSE;
    }

    /**
     * Find the position of the string or return the maximum
     *
     * @param nfoText
     * @param xmlType
     * @return
     */
    private static int findPosition(final String nfoText, final String xmlType) {
        int pos = StringUtils.indexOf(nfoText, XML_START + xmlType);
        return (pos == -1 ? Integer.MAX_VALUE : pos);
    }

    /**
     * Used to parse out the XML NFO data from a string.
     *
     * @param nfoText
     * @param movie
     * @param nfoFilename
     * @return
     */
    public static boolean readXmlNfo(String nfoText, Movie movie, String nfoFilename) {
        return convertNfoToDoc(null, nfoText, movie, nfoFilename);
    }

    /**
     * Used to parse out the XML NFO data from a file.
     *
     * This is generic for movie and TV show files as they are both nearly
     * identical.
     *
     * @param nfoFile
     * @param movie
     * @return
     */
    public static boolean readXmlNfo(File nfoFile, Movie movie) {
        return convertNfoToDoc(nfoFile, null, movie, nfoFile.getName());
    }

    /**
     * Scan a text file for information
     *
     * @param nfo
     * @param movie
     * @return
     */
    public static boolean readTextNfo(String nfo, Movie movie) {
        boolean foundInfo = DatabasePluginController.scanNFO(nfo, movie);

        LOG.debug("Scanning NFO for Poster URL");
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
                        String foundUrl = nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3);

                        // Check for some invalid characters to see if the URL is valid
                        if (foundUrl.contains(" ") || foundUrl.contains("*")) {
                            urlStartIndex = currentUrlStartIndex + 3;
                        } else {
                            LOG.debug("Poster URL found in nfo = {}", foundUrl);
                            movie.setPosterURL(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                            urlStartIndex = -1;
                            movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
                            foundInfo = Boolean.TRUE;
                        }
                    } else {
                        LOG.debug("Poster URL ignored in NFO because it's a fanart URL");
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
        return foundInfo;
    }

    /**
     * Take either a file or a String and process the NFO
     *
     * @param nfoFile
     * @param nfoString
     * @param movie
     * @param nfoFilename
     * @return
     */
    private static boolean convertNfoToDoc(File nfoFile, final String nfoString, Movie movie, final String nfoFilename) {
        Document xmlDoc;

        String filename;
        if (StringUtils.isBlank(nfoFilename) && nfoFile != null) {
            filename = nfoFile.getName();
        } else {
            filename = nfoFilename;
        }

        try {
            if (nfoFile == null) {
                // Assume we're using the string
                xmlDoc = DOMHelper.getDocFromString(nfoString);
            } else {
                xmlDoc = DOMHelper.getDocFromFile(nfoFile);
            }
        } catch (SAXParseException ex) {
            LOG.debug(ERROR_FIXIT, filename, ex.getMessage());
            return Boolean.FALSE;
        } catch (MalformedURLException ex) {
            LOG.debug(ERROR_FIXIT, filename, ex.getMessage());
            return Boolean.FALSE;
        } catch (IOException ex) {
            LOG.debug(ERROR_FIXIT, filename, ex.getMessage());
            return Boolean.FALSE;
        } catch (ParserConfigurationException | SAXException ex) {
            LOG.debug(ERROR_FIXIT, filename, ex.getMessage());
            return Boolean.FALSE;
        }

        return parseXmlNfo(xmlDoc, movie, filename);

    }

    /**
     * Parse the XML document for NFO information
     *
     * @param xmlDoc
     * @param movie
     * @param nfoFilename
     * @return
     */
    private static boolean parseXmlNfo(Document xmlDoc, Movie movie, String nfoFilename) {
        NodeList nlMovies;

        // Determine if the NFO file is for a TV Show or Movie so the default ID can be set
        boolean isTv = xmlDoc.getElementsByTagName(TYPE_TVSHOW).getLength() > 0; // Issue 2559, check in NFO if the movie is a TV show, even if no Season/Episode are in filename
        if (movie.isTVShow() || isTv) {
            nlMovies = xmlDoc.getElementsByTagName(TYPE_TVSHOW);
            isTv = Boolean.TRUE;
            movie.setMovieType(Movie.TYPE_TVSHOW); // Issue 2559, check in NFO if the movie is a TV show, and override the type
        } else {
            nlMovies = xmlDoc.getElementsByTagName(TYPE_MOVIE);
            isTv = Boolean.FALSE;
        }

        Node nMovie;
        for (int loopMovie = 0; loopMovie < nlMovies.getLength(); loopMovie++) {
            nMovie = nlMovies.item(loopMovie);
            if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                Element eCommon = (Element) nMovie;

                // Get all of the title elements from the NFO file
                parseTitle(eCommon, movie);

                if (OverrideTools.checkOverwriteYear(movie, NFO_PLUGIN_ID)) {
                    String tempYear = DOMHelper.getValueFromElement(eCommon, "year");
                    if (!parseYear(tempYear, movie)) {
                        LOG.warn("Invalid year: '{}' in {}", tempYear, nfoFilename);
                    }
                }

                // ID specific to TV Shows
                if (movie.isTVShow()) {
                    String tvdbid = DOMHelper.getValueFromElement(eCommon, "tvdbid");
                    if (isValidString(tvdbid)) {
                        movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, tvdbid);
                    }
                }

                // Get all of the other IDs
                parseIds(eCommon.getElementsByTagName("id"), movie, isTv);

                // Get the watched status
                try {
                    movie.setWatchedNFO(Boolean.parseBoolean(DOMHelper.getValueFromElement(eCommon, "watched")));
                } catch (Exception ignore) {
                    // Don't change the watched status
                }

                // Get the sets
                parseSets(eCommon.getElementsByTagName("set"), movie);

                // Rating
                int rating = parseRating(DOMHelper.getValueFromElement(eCommon, "rating"));

                if (rating > -1) {
                    movie.addRating(NFO_PLUGIN_ID, rating);
                }

                // Runtime
                if (OverrideTools.checkOverwriteRuntime(movie, NFO_PLUGIN_ID)) {
                    parseRuntime(eCommon, movie);
                }

                // Certification
                if (OverrideTools.checkOverwriteCertification(movie, NFO_PLUGIN_ID)) {
                    parseCertification(eCommon, movie);
                }

                // Plot
                if (OverrideTools.checkOverwritePlot(movie, NFO_PLUGIN_ID)) {
                    movie.setPlot(DOMHelper.getValueFromElement(eCommon, "plot"), NFO_PLUGIN_ID);
                }

                // Outline
                if (OverrideTools.checkOverwriteOutline(movie, NFO_PLUGIN_ID)) {
                    movie.setOutline(DOMHelper.getValueFromElement(eCommon, "outline"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteGenres(movie, NFO_PLUGIN_ID)) {
                    List<String> newGenres = new ArrayList<>();
                    parseGenres(eCommon.getElementsByTagName("genre"), newGenres);
                    movie.setGenres(newGenres, NFO_PLUGIN_ID);
                }

                // Premiered & Release Date
                movieDate(movie, DOMHelper.getValueFromElement(eCommon, "premiered"));
                movieDate(movie, DOMHelper.getValueFromElement(eCommon, "releasedate"));

                if (OverrideTools.checkOverwriteQuote(movie, NFO_PLUGIN_ID)) {
                    movie.setQuote(DOMHelper.getValueFromElement(eCommon, "quote"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteTagline(movie, NFO_PLUGIN_ID)) {
                    movie.setTagline(DOMHelper.getValueFromElement(eCommon, "tagline"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteCompany(movie, NFO_PLUGIN_ID)) {
                    movie.setCompany(DOMHelper.getValueFromElement(eCommon, "studio"), NFO_PLUGIN_ID);
                    movie.setCompany(DOMHelper.getValueFromElement(eCommon, "company"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteCountry(movie, NFO_PLUGIN_ID)) {
                    movie.setCountries(DOMHelper.getValueFromElement(eCommon, "country"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteTop250(movie, NFO_PLUGIN_ID)) {
                    movie.setTop250(DOMHelper.getValueFromElement(eCommon, "top250"), NFO_PLUGIN_ID);
                }

                // Poster and Fanart
                if (!SKIP_NFO_URL) {
                    movie.setPosterURL(DOMHelper.getValueFromElement(eCommon, "thumb"));
                    movie.setFanartURL(DOMHelper.getValueFromElement(eCommon, "fanart"));
                    if (StringTools.isValidString(movie.getFanartURL())) {
                        movie.setFanartFilename(movie.getBaseName() + FANART_TOKEN + "." + FANART_EXT);
                    }
                }

                // Trailers
                if (!SKIP_NFO_TRAILER) {
                    parseTrailers(eCommon.getElementsByTagName("trailer"), movie);
                }

                // Director and Writers
                if (!SKIP_NFO_CREW) {
                    parseDirectors(eCommon.getElementsByTagName("director"), movie);

                    List<Node> writerNodes = new ArrayList<>();
                    // get writers list
                    NodeList nlWriters = eCommon.getElementsByTagName("writer");
                    if (nlWriters != null && nlWriters.getLength() > 0) {
                        for (int looper = 0; looper < nlWriters.getLength(); looper++) {
                            Node node = nlWriters.item(looper);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                writerNodes.add(node);
                            }
                        }
                    }
                    // get credits list (old style)
                    nlWriters = eCommon.getElementsByTagName("credits");
                    if (nlWriters != null && nlWriters.getLength() > 0) {
                        for (int looper = 0; looper < nlWriters.getLength(); looper++) {
                            Node node = nlWriters.item(looper);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                writerNodes.add(node);
                            }
                        }
                    }
                    // parse writers
                    parseWriters(writerNodes, movie);
                }

                // Actors
                if (!SKIP_NFO_CAST) {
                    parseActors(eCommon.getElementsByTagName("actor"), movie);
                }

                // FPS
                if (OverrideTools.checkOverwriteFPS(movie, NFO_PLUGIN_ID)) {
                    float tmpFps = NumberUtils.toFloat(DOMHelper.getValueFromElement(eCommon, "fps"), -1F);
                    if (tmpFps > -1F) {
                        movie.setFps(tmpFps, NFO_PLUGIN_ID);
                    }
                }

                // VideoSource: Issue 506 - Even though it's not strictly XBMC standard
                if (OverrideTools.checkOverwriteVideoSource(movie, NFO_PLUGIN_ID)) {
                    // Issue 2531: Try the alternative "videoSource"
                    movie.setVideoSource(DOMHelper.getValueFromElement(eCommon, "videosource", "videoSource"), NFO_PLUGIN_ID);
                }

                // Video Output
                if (OverrideTools.checkOverwriteVideoOutput(movie, NFO_PLUGIN_ID)) {
                    String tempString = DOMHelper.getValueFromElement(eCommon, "videooutput");
                    movie.setVideoOutput(tempString, NFO_PLUGIN_ID);
                }

                // Parse the video info
                parseFileInfo(movie, DOMHelper.getElementByName(eCommon, "fileinfo"));
            }

            // Issue 2542: There should only be 1 movie/tvshow node in the NFO
            break;
        }

        // Parse the episode details
        if (movie.isTVShow()) {
            parseAllEpisodeDetails(movie, xmlDoc.getElementsByTagName(TYPE_EPISODE));
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

        if (OverrideTools.checkOverwriteContainer(movie, NFO_PLUGIN_ID)) {
            String container = DOMHelper.getValueFromElement(eFileInfo, "container");
            movie.setContainer(container, NFO_PLUGIN_ID);
        }

        Element eStreamDetails = DOMHelper.getElementByName(eFileInfo, "streamdetails");

        if (eStreamDetails == null) {
            return;
        }

        // Video
        NodeList nlStreams = eStreamDetails.getElementsByTagName("video");
        Node nStreams;
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String temp = DOMHelper.getValueFromElement(eStreams, "codec");
                if (isValidString(temp)) {
                    Codec videoCodec = new Codec(CodecType.VIDEO);
                    videoCodec.setCodecSource(CodecSource.NFO);
                    videoCodec.setCodec(temp);
                    movie.addCodec(videoCodec);
                }

                if (OverrideTools.checkOverwriteAspectRatio(movie, NFO_PLUGIN_ID)) {
                    temp = DOMHelper.getValueFromElement(eStreams, "aspect");
                    movie.setAspectRatio(ASPECT_TOOLS.cleanAspectRatio(temp), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteResolution(movie, NFO_PLUGIN_ID)) {
                    movie.setResolution(DOMHelper.getValueFromElement(eStreams, "width"), DOMHelper.getValueFromElement(eStreams, "height"), NFO_PLUGIN_ID);
                }
            }
        } // End of VIDEO

        // Audio
        nlStreams = eStreamDetails.getElementsByTagName("audio");

        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;

                String aCodec = DOMHelper.getValueFromElement(eStreams, "codec").trim();
                String aLanguage = DOMHelper.getValueFromElement(eStreams, "language");
                String aChannels = DOMHelper.getValueFromElement(eStreams, "channels");

                // If the codec is lowercase, covert it to uppercase, otherwise leave it alone
                if (StringUtils.isAllLowerCase(aCodec)) {
                    aCodec = aCodec.toUpperCase();
                }

                if (StringTools.isValidString(aLanguage)) {
                    aLanguage = MovieFilenameScanner.determineLanguage(aLanguage);
                }

                Codec audioCodec = new Codec(CodecType.AUDIO, aCodec);
                audioCodec.setCodecSource(CodecSource.NFO);
                audioCodec.setCodecLanguage(aLanguage);
                audioCodec.setCodecChannels(aChannels);
                movie.addCodec(audioCodec);
            }
        } // End of AUDIO

        // Update the language
        if (OverrideTools.checkOverwriteLanguage(movie, NFO_PLUGIN_ID)) {
            Set<String> langs = new HashSet<>();
            // Process the languages and remove any duplicates
            for (Codec codec : movie.getCodecs()) {
                if (codec.getCodecType() == CodecType.AUDIO) {
                    langs.add(codec.getCodecLanguage());
                }
            }

            // Remove UNKNOWN if it is NOT the only entry
            if (langs.contains(Movie.UNKNOWN) && langs.size() > 1) {
                langs.remove(Movie.UNKNOWN);
            } else if (langs.isEmpty()) {
                // Add the language as UNKNOWN by default.
                langs.add(Movie.UNKNOWN);
            }

            // Build the language string
            StringBuilder movieLanguage = new StringBuilder();
            for (String lang : langs) {
                if (movieLanguage.length() > 0) {
                    movieLanguage.append(LANGUAGE_DELIMITER);
                }
                movieLanguage.append(lang);
            }
            movie.setLanguage(movieLanguage.toString(), NFO_PLUGIN_ID);
        }

        // Subtitles
        List<String> subtitles = new ArrayList<>();
        nlStreams = eStreamDetails.getElementsByTagName("subtitle");
        for (int looper = 0; looper < nlStreams.getLength(); looper++) {
            nStreams = nlStreams.item(looper);
            if (nStreams.getNodeType() == Node.ELEMENT_NODE) {
                Element eStreams = (Element) nStreams;
                subtitles.add(DOMHelper.getValueFromElement(eStreams, "language"));
            }
        }
        SubtitleTools.setMovieSubtitles(movie, subtitles);
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
        if (eEpisodeDetails == null) {
            return epDetail;
        }

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
        int rating = parseRating(tempValue);
        if (rating > -1) {
            // Looks like a valid rating
            epDetail.setRating(String.valueOf(rating));
        }

        tempValue = DOMHelper.getValueFromElement(eEpisodeDetails, "aired");
        if (isValidString(tempValue)) {
            try {
                epDetail.setFirstAired(DateTimeTools.convertDateToString(new DateTime(tempValue)));
            } catch (Exception ignore) {
                // Set the aired date if there is an exception
                epDetail.setFirstAired(tempValue);
            }
        }

        epDetail.setAirsAfterSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsafterseason", "airsAfterSeason"));
        epDetail.setAirsBeforeSeason(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeseason", "airsBeforeSeason"));
        epDetail.setAirsBeforeEpisode(DOMHelper.getValueFromElement(eEpisodeDetails, "airsbeforeepisode", "airsBeforeEpisode"));

        return epDetail;
    }

    /**
     * Convert the date string to a date and update the movie object
     *
     * @param movie
     * @param dateString
     */
    public static void movieDate(Movie movie, final String dateString) {
        String parseDate = StringUtils.normalizeSpace(dateString);

        if (StringTools.isNotValidString(parseDate)) {
            // No date, so return
            return;
        }

        Date parsedDate = null;
        boolean failedToParse = false;

        try {
            parsedDate = DateTimeTools.parseStringToDate(parseDate);
        } catch (IllegalArgumentException | ParseException ex) {
            LOG.warn(SystemTools.getStackTrace(ex));
            failedToParse = true;
        }

        if (failedToParse) {
            LOG.warn("Failed parsing NFO file for movie: {}. Please fix or remove it.", movie.getBaseFilename());
            LOG.warn("premiered or releasedate does not contain a valid date: {}", parseDate);

            if (OverrideTools.checkOverwriteReleaseDate(movie, NFO_PLUGIN_ID)) {
                movie.setReleaseDate(parseDate, NFO_PLUGIN_ID);
            }

            return;
        }

        if (StringTools.isValidString(parseDate)) {
            try {
                if (OverrideTools.checkOverwriteReleaseDate(movie, NFO_PLUGIN_ID)) {
                    movie.setReleaseDate(DateTimeTools.convertDateToString(parsedDate, "yyyy-MM-dd"), NFO_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteYear(movie, NFO_PLUGIN_ID)) {
                    movie.setYear(DateTimeTools.convertDateToString(parsedDate, "yyyy"), NFO_PLUGIN_ID);
                }
            } catch (Exception ex) {
                LOG.warn("Failed parsing NFO file for movie: {}. Please fix or remove it.", movie.getBaseFilename());
                LOG.warn("premiered or releasedate does not contain a valid date: {}", parseDate);
                LOG.warn(SystemTools.getStackTrace(ex));

                if (OverrideTools.checkOverwriteReleaseDate(movie, NFO_PLUGIN_ID)) {
                    movie.setReleaseDate(parseDate, NFO_PLUGIN_ID);
                }
            }
        }
    }

    /**
     * Parse Genres from the XML NFO file
     *
     * Caters for multiple genres on the same line and multiple lines.
     *
     * @param nlElements
     * @param movie
     */
    private static void parseGenres(NodeList nlElements, List<String> newGenres) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eGenre = (Element) nElements;
                NodeList nlNames = eGenre.getElementsByTagName("name");
                if ((nlNames != null) && (nlNames.getLength() > 0)) {
                    parseGenres(nlNames, newGenres);
                } else {
                    newGenres.addAll(StringTools.splitList(eGenre.getTextContent(), SPLIT_GENRE));
                }
            }
        }
    }

    /**
     * Parse Actors from the XML NFO file.
     *
     * @param nlElements
     * @param movie
     */
    private static void parseActors(NodeList nlElements, Movie movie) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        // check if we should override
        boolean overrideActors = OverrideTools.checkOverwriteActors(movie, NFO_PLUGIN_ID);
        boolean overridePeopleActors = OverrideTools.checkOverwritePeopleActors(movie, NFO_PLUGIN_ID);
        if (!overrideActors && !overridePeopleActors) {
            // nothing to do if nothing should be overridden
            return;
        }

        // count for already set actors
        int count = 0;
        // flag to indicate if cast must be cleared
        boolean clearCast = Boolean.TRUE;
        boolean clearPeopleCast = Boolean.TRUE;

        for (int actorLoop = 0; actorLoop < nlElements.getLength(); actorLoop++) {
            // Get all the name/role/thumb nodes
            Node nActors = nlElements.item(actorLoop);
            NodeList nlCast = nActors.getChildNodes();
            Node nElement;

            String aName = Movie.UNKNOWN;
            String aRole = Movie.UNKNOWN;
            String aThumb = Movie.UNKNOWN;
            Boolean firstActor = Boolean.TRUE;

            if (nlCast.getLength() > 1) {
                for (int looper = 0; looper < nlCast.getLength(); looper++) {
                    nElement = nlCast.item(looper);
                    if (nElement.getNodeType() == Node.ELEMENT_NODE) {
                        Element eCast = (Element) nElement;
                        if (eCast.getNodeName().equalsIgnoreCase("name")) {
                            if (firstActor) {
                                firstActor = Boolean.FALSE;
                            } else {

                                if (overrideActors) {
                                    // clear cast if not already done
                                    if (clearCast) {
                                        movie.clearCast();
                                        clearCast = Boolean.FALSE;
                                    }
                                    // add actor
                                    movie.addActor(aName, NFO_PLUGIN_ID);
                                }

                                if (overridePeopleActors && (count < MAX_COUNT_ACTOR)) {
                                    // clear people cast if not already done
                                    if (clearPeopleCast) {
                                        movie.clearPeopleCast();
                                        clearPeopleCast = Boolean.FALSE;
                                    }
                                    // add actor
                                    if (movie.addActor(Movie.UNKNOWN, aName, aRole, aThumb, Movie.UNKNOWN, NFO_PLUGIN_ID)) {
                                        count++;
                                    }
                                }
                            }
                            aName = eCast.getTextContent();
                            aRole = Movie.UNKNOWN;
                            aThumb = Movie.UNKNOWN;
                        } else if (eCast.getNodeName().equalsIgnoreCase("role") && StringUtils.isNotBlank(eCast.getTextContent())) {
                            aRole = eCast.getTextContent();
                        } else if (eCast.getNodeName().equalsIgnoreCase("thumb") && StringUtils.isNotBlank(eCast.getTextContent())) {
                            // thumb will be skipped if there's nothing in there
                            aThumb = eCast.getTextContent();
                        }
                        // There's a case where there might be a different node here that isn't name, role or thumb, but that will be ignored
                    }
                }
            } else {
                // This looks like a Mede8er node in the "<actor>Actor Name</actor>" format, so just get the text element
                aName = nActors.getTextContent();
            }

            if (overrideActors) {
                // clear cast if not already done
                if (clearCast) {
                    movie.clearCast();
                    clearCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(aName, NFO_PLUGIN_ID);
            }

            if (overridePeopleActors && (count < MAX_COUNT_ACTOR)) {
                // clear people cast if not already done
                if (clearPeopleCast) {
                    movie.clearPeopleCast();
                    clearPeopleCast = Boolean.FALSE;
                }
                // add actor
                if (movie.addActor(Movie.UNKNOWN, aName, aRole, aThumb, Movie.UNKNOWN, NFO_PLUGIN_ID)) {
                    count++;
                }
            }
        }
    }

    /**
     * Parse Writers from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseWriters(List<Node> nlWriters, Movie movie) {
        // check if we have nodes
        if (nlWriters == null || nlWriters.isEmpty()) {
            return;
        }

        // check if we should override
        boolean overrideWriters = OverrideTools.checkOverwriteWriters(movie, NFO_PLUGIN_ID);
        boolean overridePeopleWriters = OverrideTools.checkOverwritePeopleWriters(movie, NFO_PLUGIN_ID);
        if (!overrideWriters && !overridePeopleWriters) {
            // nothing to do if nothing should be overridden
            return;
        }

        Set<String> newWriters = new LinkedHashSet<>();
        for (Node nWriter : nlWriters) {
            NodeList nlChilds = ((Element) nWriter).getChildNodes();
            Node nChilds;
            for (int looper = 0; looper < nlChilds.getLength(); looper++) {
                nChilds = nlChilds.item(looper);
                if (nChilds.getNodeType() == Node.TEXT_NODE) {
                    newWriters.add(nChilds.getNodeValue());
                }
            }
        }

        if (overrideWriters) {
            movie.setWriters(newWriters, NFO_PLUGIN_ID);
        }
        if (overridePeopleWriters) {
            movie.setPeopleWriters(newWriters, NFO_PLUGIN_ID);
        }
    }

    /**
     * Parse Directors from the XML NFO file
     *
     * @param nlElements
     * @param movie
     */
    private static void parseDirectors(NodeList nlElements, Movie movie) {
        // check if we have a node
        if (nlElements == null || nlElements.getLength() == 0) {
            return;
        }

        // check if we should override
        boolean overrideDirectors = OverrideTools.checkOverwriteDirectors(movie, NFO_PLUGIN_ID);
        boolean overridePeopleDirectors = OverrideTools.checkOverwritePeopleDirectors(movie, NFO_PLUGIN_ID);
        if (!overrideDirectors && !overridePeopleDirectors) {
            // nothing to do if nothing should be overridden
            return;
        }

        List<String> newDirectors = new ArrayList<>();
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eDirector = (Element) nElements;
                newDirectors.add(eDirector.getTextContent());
            }
        }

        if (overrideDirectors) {
            movie.setDirectors(newDirectors, NFO_PLUGIN_ID);
        }

        if (overridePeopleDirectors) {
            movie.setPeopleDirectors(newDirectors, NFO_PLUGIN_ID);
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
        if (eCommon == null) {
            return;
        }
        if (!OverrideTools.checkOverwriteCertification(movie, NFO_PLUGIN_ID)) {
            return;
        }

        String tempCert;
        if (CERT_FROM_MPAA) {
            tempCert = DOMHelper.getValueFromElement(eCommon, "mpaa");
            if (isValidString(tempCert)) {
                // Issue 333
                movie.setCertification(StringTools.processMpaaCertification(tempCert), NFO_PLUGIN_ID);
            }
        } else {
            tempCert = DOMHelper.getValueFromElement(eCommon, "certification");

            if (isValidString(tempCert)) {
                int countryPos = tempCert.lastIndexOf(IMDB_PREFERRED_COUNTRY);
                if (countryPos > 0) {
                    // We've found the country, so extract just that tag
                    tempCert = tempCert.substring(countryPos);
                    int pos = tempCert.indexOf(':');
                    if (pos > 0) {
                        int endPos = tempCert.indexOf(" /");
                        if (endPos > 0) {
                            // This is in the middle of the string
                            tempCert = tempCert.substring(pos + 1, endPos);
                        } else {
                            // This is at the end of the string
                            tempCert = tempCert.substring(pos + 1);
                        }
                    }
                } else if (StringUtils.containsIgnoreCase(tempCert, "Rated")) {
                    // Extract the MPAA rating from the certification
                    tempCert = StringTools.processMpaaCertification(tempCert);
                } else {
                    // The country wasn't found in the value, so grab the last one
                    int pos = tempCert.lastIndexOf(':');
                    if (pos > 0) {
                        // Strip the country code from the rating for certification like "UK:PG-12"
                        tempCert = tempCert.substring(pos + 1);
                    }
                }

                movie.setCertification(tempCert.trim(), NFO_PLUGIN_ID);
            }
        }
    }

    /**
     * Parse Runtime from the XML NFO file
     *
     * @param eCommon
     * @param movie
     */
    public static void parseRuntime(Element eCommon, Movie movie) {
        if (OverrideTools.checkOverwriteRuntime(movie, NFO_PLUGIN_ID)) {
            String runtime = DOMHelper.getValueFromElement(eCommon, "runtime");

            // Save the first runtime to use if no preferred one is found
            String prefRuntime = null;
            // Split the runtime into individual parts
            for (String rtSingle : runtime.split("\\|")) {
                // IF we don't have a current preferred runtime, set it now.
                if (StringUtils.isBlank(prefRuntime)) {
                    prefRuntime = rtSingle;
                }

                // Check to see if we have our preferred country in the string
                if (StringUtils.containsIgnoreCase(rtSingle, IMDB_PREFERRED_COUNTRY)) {
                    // Lets get the country runtime
                    prefRuntime = rtSingle.substring(rtSingle.indexOf(IMDB_PREFERRED_COUNTRY) + IMDB_PREFERRED_COUNTRY.length() + 1);
                }
            }

            movie.setRuntime(prefRuntime, NFO_PLUGIN_ID);
        }
    }

    /**
     * Parse the rating from the passed string and normalise it
     *
     * @param ratingString
     * @param movie
     * @return true if the rating was successfully parsed.
     */
    private static int parseRating(String ratingString) {
        if (StringTools.isNotValidString(ratingString)) {
            // Rating isn't valid, so skip it
            return -1;
        } else {
            float rating = NumberUtils.toFloat(ratingString, 0.0f);
            if (rating > 0.0f) {
                if (rating <= 10.0f) {
                    return Math.round(rating * 10f);
                } else {
                    return Math.round(rating * 1f);
                }
            } else {
                // Negative or zero, so return zero
                return 0;
            }
        }
    }

    /**
     * Parse all the IDs associated with the movie from the XML NFO file
     *
     * @param nlElements
     * @param movie
     * @param isTv
     */
    private static void parseIds(NodeList nlElements, Movie movie, boolean isTv) {
        Node nElements;
        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nElements = nlElements.item(looper);
            if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                Element eId = (Element) nElements;

                String movieDb = eId.getAttribute("moviedb");
                if (StringTools.isNotValidString(movieDb)) {
                    // Decide which default plugin ID to use
                    if (isTv) {
                        movieDb = TheTvDBPlugin.THETVDB_PLUGIN_ID;
                    } else {
                        movieDb = ImdbPlugin.IMDB_PLUGIN_ID;
                    }
                }
                movie.setId(movieDb, eId.getTextContent());
                LOG.debug("Found {} ID: {}", movieDb, eId.getTextContent());

                // Process the TMDB id
                movieDb = eId.getAttribute("TMDB");
                if (StringTools.isValidString(movieDb)) {
                    LOG.debug("Found TheMovieDb ID: {}", movieDb);
                    movie.setId(TheMovieDbPlugin.TMDB_PLUGIN_ID, movieDb);
                }
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
        if (eCommon == null) {
            return;
        }

        // Determine title elements
        String titleMain = DOMHelper.getValueFromElement(eCommon, "title");
        String titleSort = DOMHelper.getValueFromElement(eCommon, "sorttitle", "sortTitle");
        String titleOrig = DOMHelper.getValueFromElement(eCommon, "originaltitle", "originalTitle");

        if (OverrideTools.checkOverwriteOriginalTitle(movie, NFO_PLUGIN_ID)) {
            movie.setOriginalTitle(titleOrig, NFO_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteTitle(movie, NFO_PLUGIN_ID)) {
            movie.setTitle(titleMain, NFO_PLUGIN_ID);
        }

        // Set the title sort
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
            if (OverrideTools.checkOverwriteYear(movie, NFO_PLUGIN_ID)) {
                movie.setYear(tempYear, NFO_PLUGIN_ID);
            }
            return Boolean.TRUE;
        } else {
            if (StringUtils.isBlank(tempYear)) {
                // The year is blank, so skip it.
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }
}
