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
package com.moviejukebox.writer;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.EpisodeDetail;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MovieNFOReader {

    private static final Logger logger = Logger.getLogger(MovieNFOReader.class);
    private static final String logMessage = "MovieNFOReader: ";
    // Types of nodes
    public static final String TYPE_MOVIE = "movie";
    public static final String TYPE_TVSHOW = "tvshow";
    public static final String TYPE_EPISODE = "episodedetails";
    // Plugin ID
    private static final String NFO_PLUGIN_ID = "NFO";
    // Other properties
    private static boolean skipNfoUrl = PropertiesUtil.getBooleanProperty("filename.nfo.skipUrl", "true");
    private static boolean skipNfoTrailer = PropertiesUtil.getBooleanProperty("filename.nfo.skipTrailer", "false");
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    private static boolean getCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", "true");
    private static String imdbPreferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
    private static String languageDelimiter = PropertiesUtil.getProperty("mjb.language.delimiter", Movie.SPACE_SLASH_SPACE);
    private static String subtitleDelimiter = PropertiesUtil.getProperty("mjb.subtitle.delimiter", Movie.SPACE_SLASH_SPACE);
    // Patterns
    private static final String splitPattern = "\\||,|/";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // Caters for the case where "-/" is not wanted as part of the split

    /**
     * Used to parse out the XBMC NFO XML data for the XML NFO files.
     *
     * This is generic for movie and TV show files as they are both nearly identical.
     *
     * @param nfoFile
     * @param movie
     */
    public static boolean parseNfo(File nfoFile, Movie movie) {
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getEventDocFromUrl(nfoFile);
        } catch (MalformedURLException error) {
            logger.error(logMessage + "Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (IOException error) {
            logger.error(logMessage + "Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (ParserConfigurationException error) {
            logger.error(logMessage + "Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (SAXException error) {
            logger.error(logMessage + "Failed parsing NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        NodeList nlMovies;

        // Determine if the NFO file is for a TV Show or Movie so the default ID can be set
        boolean isTv;
        if (movie.isTVShow()) {
            nlMovies = xmlDoc.getElementsByTagName(TYPE_TVSHOW);
            isTv = Boolean.TRUE;
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
                if (!parseRating(DOMHelper.getValueFromElement(eCommon, "rating"), movie)) {
                    logger.error(logMessage + "Failed parsing rating in NFO file: " + nfoFile.getName() + ". Please fix it or remove it.");
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
                        logger.warn(logMessage + "Error reading FPS value " + tempString);
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

        String container = DOMHelper.getValueFromElement(eFileInfo, "container");
        if (StringTools.isValidString(container)) {
            movie.setContainer(container);
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
                if (aCodec.equalsIgnoreCase(aCodec)) {
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
                logger.debug(logMessage + "Failed to convert rating '" + tempValue + "'");
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
                DateTime dateTime;
                if (dateString.length() == 4) {
                    // Warn the user
                    logger.debug(logMessage + "Partial date detected in premiered field of NFO for " + movie.getBaseFilename());
                    // Assume just the year an append "-01-01" to the end
                    dateTime = new DateTime(dateString + "-01-01");
                } else {
                    dateTime = new DateTime(dateString);
                }

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
        if (eCommon == null) {
            return;
        }

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
        if (StringUtils.isBlank(ratingString)) {
            // Rating is blank, so skip it
            return Boolean.TRUE;
        }

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
                logger.debug(logMessage + "Found " + movieDb + " ID: " + eId.getTextContent());
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
            if (StringUtils.isBlank(tempYear)) {
                // The year is blank, so skip it.
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }
}
