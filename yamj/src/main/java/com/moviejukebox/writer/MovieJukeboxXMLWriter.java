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

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.*;
import com.moviejukebox.model.Comparator.CertificationComparator;
import com.moviejukebox.model.Comparator.IndexComparator;
import com.moviejukebox.model.Comparator.SortIgnorePrefixesComparator;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import static com.moviejukebox.tools.XMLHelper.parseCData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parse/Write XML files for movie details and library indexes
 *
 * @author Julien
 * @author Stuart.Boston
 */
public class MovieJukeboxXMLWriter {

    private static final Logger logger = Logger.getLogger(MovieJukeboxXMLWriter.class);
    private static final String EXT_XML = ".xml";
    private static final String EXT_HTML = ".html";
    private static final String evFileSuffix = "_small";   // String to append to the eversion categories file if needed
    private static final String WON = "won";
    private static final String MOVIE = "movie";
    private static final String MOVIEDB = "moviedb";
    private static final String BASE_FILENAME = "baseFilename";
    private static final String TITLE = "title";
    private static final String ORIGINAL_TITLE = "originalTitle";
    private static final String YEAR = "year";
    private static final String COUNTRY = "country";
    private static final String ORDER = "order";
    private static final String LANGUAGE = "language";
    private static final String YES = "YES";
    private static final String TRAILER_LAST_SCAN = "trailerLastScan";
    private static final String CHARACTER = "character";
    private static final String NAME = "name";
    private static final String DEPARTMENT = "department";
    private static final String JOB = "job";
    private static final String URL = "url";
    private static final String ID = "id_";
    private static final String SEASON = "season";
    private static final String PART = "part";
    private static final String RATING = "rating";
    private static final String COUNT = "count";
    private static final String INDEX = "index";
    private static final String ORIGINAL_NAME = "originalName";
    private static final String DETAILS="details";
    private static boolean forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", FALSE);
    private static boolean forceIndexOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceIndexOverwrite", FALSE);
    private int nbMoviesPerPage;
    private int nbMoviesPerLine;
    private int nbTvShowsPerPage;
    private int nbTvShowsPerLine;
    private int nbSetMoviesPerPage;
    private int nbSetMoviesPerLine;
    private int nbTVSetMoviesPerPage;
    private int nbTVSetMoviesPerLine;
    private boolean fullMovieInfoInIndexes;
    private boolean fullCategoriesInIndexes;
    private boolean includeMoviesInCategories;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private boolean includeEpisodeRating;
    private static boolean isPlayOnHD = PropertiesUtil.getBooleanProperty("mjb.PlayOnHD", FALSE);
    private static boolean isExtendedURL = PropertiesUtil.getBooleanProperty("mjb.scanner.mediainfo.rar.extended.url", FALSE);
    private static String defaultSource = PropertiesUtil.getProperty("filename.scanner.source.default", Movie.UNKNOWN);
    private List<String> categoriesExplodeSet = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.explodeSet", "").split(","));
    private boolean removeExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet", FALSE);
    private boolean keepTVExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.keepTV", TRUE);
    private boolean beforeSortExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.beforeSort", FALSE);
    private static List<String> categoriesDisplayList = initDisplayList();
    private static List<String> categoriesLimitList = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.limitList", "Cast,Director,Writer,Person").split(","));
    private static boolean writeNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.writeFiles", FALSE);
    private boolean setsExcludeTV;
    private static String peopleFolder = initPeopleFolder();
    private static boolean enableWatchScanner = PropertiesUtil.getBooleanProperty("watched.scanner.enable", TRUE);
    // Should we scrape people information
    private static boolean enablePeople = PropertiesUtil.getBooleanProperty("mjb.people", FALSE);
    private static boolean addPeopleInfo = PropertiesUtil.getBooleanProperty("mjb.people.addInfo", FALSE);
    // Should we scrape the award information
    private static boolean enableAwards = PropertiesUtil.getBooleanProperty("mjb.scrapeAwards", FALSE) || PropertiesUtil.getProperty("mjb.scrapeAwards", "").equalsIgnoreCase(WON);
    // Should we scrape the business information
    private static boolean enableBusiness = PropertiesUtil.getBooleanProperty("mjb.scrapeBusiness", FALSE);
    // Should we scrape the trivia information
    private static boolean enableTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", FALSE);
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    // Should we reindex the New / Watched / Unwatched categories?
    private boolean reindexNew = Boolean.FALSE;
    private boolean reindexWatched = Boolean.FALSE;
    private boolean reindexUnwatched = Boolean.FALSE;
    private boolean xmlCompatible = PropertiesUtil.getBooleanProperty("mjb.XMLcompatible", FALSE);
    private boolean sortLibrary = PropertiesUtil.getBooleanProperty("indexing.sort.libraries", TRUE);

    public MovieJukeboxXMLWriter() {
        nbMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbThumbnailsPerPage", "10");
        nbMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbThumbnailsPerLine", "5");
        nbTvShowsPerPage = PropertiesUtil.getIntProperty("mjb.nbTvThumbnailsPerPage", "0"); // If 0 then use the Movies setting
        nbTvShowsPerLine = PropertiesUtil.getIntProperty("mjb.nbTvThumbnailsPerLine", "0"); // If 0 then use the Movies setting
        nbSetMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbSetThumbnailsPerPage", "0"); // If 0 then use the Movies setting
        nbSetMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbSetThumbnailsPerLine", "0"); // If 0 then use the Movies setting
        nbTVSetMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbTVSetThumbnailsPerPage", "0"); // If 0 then use the TV SHOW setting
        nbTVSetMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbTVSetThumbnailsPerLine", "0"); // If 0 then use the TV SHOW setting
        fullMovieInfoInIndexes = PropertiesUtil.getBooleanProperty("mjb.fullMovieInfoInIndexes", FALSE);
        fullCategoriesInIndexes = PropertiesUtil.getBooleanProperty("mjb.fullCategoriesInIndexes", TRUE);
        includeMoviesInCategories = PropertiesUtil.getBooleanProperty("mjb.includeMoviesInCategories", FALSE);
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", FALSE);
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", FALSE);
        includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", FALSE);
        setsExcludeTV = PropertiesUtil.getBooleanProperty("mjb.sets.excludeTV", FALSE);

        if (nbTvShowsPerPage == 0) {
            nbTvShowsPerPage = nbMoviesPerPage;
        }

        if (nbTvShowsPerLine == 0) {
            nbTvShowsPerLine = nbMoviesPerLine;
        }

        if (nbSetMoviesPerPage == 0) {
            nbSetMoviesPerPage = nbMoviesPerPage;
        }

        if (nbSetMoviesPerLine == 0) {
            nbSetMoviesPerLine = nbMoviesPerLine;
        }

        if (nbTVSetMoviesPerPage == 0) {
            nbTVSetMoviesPerPage = nbTvShowsPerPage;
        }

        if (nbTVSetMoviesPerLine == 0) {
            nbTVSetMoviesPerLine = nbTvShowsPerLine;
        }
    }

    /**
     * Set up the people folder
     *
     * @return
     */
    private static String initPeopleFolder() {
        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        String folder = PropertiesUtil.getProperty("mjb.people.folder", "");
        if (StringTools.isNotValidString(folder)) {
            folder = "";
        } else if (!folder.endsWith(File.separator)) {
            folder += File.separator;
        }
        return folder;
    }

    /**
     * Set up the display list
     *
     * @return
     */
    private static List<String> initDisplayList() {
        String strCategoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.displayList", "");
        if (strCategoriesDisplayList.length() == 0) {
            strCategoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Certification,Year,Library,Set");
        }
        return Arrays.asList(strCategoriesDisplayList.split(","));
    }

    /**
     * Parse a single movie detail XML file
     */
    public boolean parseMovieXML(File xmlFile, Movie movie) {

        boolean forceDirtyFlag = Boolean.FALSE; // force dirty flag for example when extras have been deleted

        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getDocFromFile(xmlFile);
        } catch (MalformedURLException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (IOException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (ParserConfigurationException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (SAXException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        NodeList nlMovies;  // Main list of movies, there should only be 1
        Node nMovie;        // Node for the movie

        NodeList nlElements;    // Reusable NodeList for the other elements
        Node nElements;         // Reusable Node for the other elements

        nlMovies = xmlDoc.getElementsByTagName(MOVIE);
        for (int loopMovie = 0; loopMovie < nlMovies.getLength(); loopMovie++) {
            nMovie = nlMovies.item(loopMovie);
            if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                Element eMovie = (Element) nMovie;

                // Get all the IDs associated with the movie
                nlElements = eMovie.getElementsByTagName("id");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eId = (Element) nElements;

                        String movieDb = eId.getAttribute(MOVIEDB);
                        if (StringTools.isNotValidString(movieDb)) {
                            movieDb = ImdbPlugin.IMDB_PLUGIN_ID;
                        }
                        movie.setId(movieDb, eId.getTextContent());
                    }
                }   // End of ID

                // Get the Version the XML was written with
                movie.setMjbVersion(DOMHelper.getValueFromElement(eMovie, "mjbVersion"));

                // Get the Revision the XML was written with
                movie.setMjbRevision(DOMHelper.getValueFromElement(eMovie, "mjbRevision"));

                // Get the date/time the XML was written
                movie.setMjbGenerationDateString(DOMHelper.getValueFromElement(eMovie, "xmlGenerationDate"));

                if (StringTools.isNotValidString(movie.getBaseFilename())) {
                    movie.setBaseFilename(DOMHelper.getValueFromElement(eMovie, "baseFilenameBase"));
                }

                if (StringTools.isNotValidString(movie.getBaseName())) {
                    movie.setBaseName(DOMHelper.getValueFromElement(eMovie, BASE_FILENAME));
                }

                // Get the title fields
                movie.setTitle(DOMHelper.getValueFromElement(eMovie, TITLE));
                movie.setTitleSort(DOMHelper.getValueFromElement(eMovie, "titleSort"));
                movie.setOriginalTitle(DOMHelper.getValueFromElement(eMovie, ORIGINAL_TITLE));

                // Get the year. We don't care about the attribute as that is the index
                movie.setYear(DOMHelper.getValueFromElement(eMovie, YEAR));

                // Get the release date
                movie.setReleaseDate(DOMHelper.getValueFromElement(eMovie, "releaseDate"));

                // get the show status
                movie.setShowStatus(DOMHelper.getValueFromElement(eMovie, "showStatus"));

                // Get the ratings. We don't care about the RATING as this is a calulated value.
                // So just get the childnodes of the "ratings" node
                nlElements = eMovie.getElementsByTagName("ratings");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eRating = (Element) nElements;

                            String movieDb = eRating.getAttribute(MOVIEDB);
                            if (StringTools.isNotValidString(movieDb)) {
                                movieDb = ImdbPlugin.IMDB_PLUGIN_ID;
                            }
                            movie.addRating(movieDb, Integer.parseInt(eRating.getTextContent()));
                        }
                    }
                }   // End of Ratings

                // get the IMDB top250 rating
                movie.setTop250(Integer.parseInt(DOMHelper.getValueFromElement(eMovie, "top250")));

                // Get the watched flags
                // The "watched" attribute is transient, based on the status of the watched movie files
//                movie.setWatchedFile(Boolean.parseBoolean(DOMHelper.getValueFromElement(eMovie, "watched")));
                movie.setWatchedNFO(Boolean.parseBoolean(DOMHelper.getValueFromElement(eMovie, "watchedNFO")));
                movie.setWatchedFile(Boolean.parseBoolean(DOMHelper.getValueFromElement(eMovie, "watchedFile")));

                // Get artwork URLS
                movie.setPosterURL(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "posterURL")));
                movie.setFanartURL(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "fanartURL")));
                movie.setBannerURL(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "bannerURL")));
                movie.setClearArtURL(HTMLTools.decodeHtml(DOMHelper.getValueFromElement(eMovie, "clearArtURL")));
                movie.setClearLogoURL(HTMLTools.decodeHtml(DOMHelper.getValueFromElement(eMovie, "clearLogoURL")));
                movie.setTvThumbURL(HTMLTools.decodeHtml(DOMHelper.getValueFromElement(eMovie, "tvThumbURL")));
                movie.setSeasonThumbURL(HTMLTools.decodeHtml(DOMHelper.getValueFromElement(eMovie, "seasonThumbURL")));
                movie.setMovieDiscURL(HTMLTools.decodeHtml(DOMHelper.getValueFromElement(eMovie, "movieDiscURL")));

                // Get artwork files
                movie.setPosterFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "posterFile")));
                movie.setDetailPosterFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "detailPosterFile")));
                movie.setThumbnailFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "thumbnail")));
                movie.setFanartFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "fanartFile")));
                movie.setBannerFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "bannerFile")));
                movie.setClearArtFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "clearArtFile")));
                movie.setClearLogoFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "clearLogoFile")));
                movie.setTvThumbFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "tvThumbFile")));
                movie.setSeasonThumbFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "seasonThumbFile")));
                movie.setMovieDiscFilename(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "movieDiscFile")));


                // Get the plot and outline
                movie.setPlot(DOMHelper.getValueFromElement(eMovie, "plot"));
                movie.setOutline(DOMHelper.getValueFromElement(eMovie, "outline"));

                // Get the quote
                movie.setQuote(DOMHelper.getValueFromElement(eMovie, "quote"));

                // Get the tagline
                movie.setTagline(DOMHelper.getValueFromElement(eMovie, "tagline"));

                // Get the company name
                movie.setCompany(DOMHelper.getValueFromElement(eMovie, "company"));

                // get the runtime
                movie.setRuntime(DOMHelper.getValueFromElement(eMovie, "runtime"));

                // Get the directors
                nlElements = eMovie.getElementsByTagName("directors");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element ePerson = (Element) nElements;
                            movie.addDirector(ePerson.getTextContent());
                        }
                    }
                }   // End of directors

                // Get the writers
                nlElements = eMovie.getElementsByTagName("writers");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element ePerson = (Element) nElements;
                            movie.addWriter(ePerson.getTextContent());
                        }
                    }
                }   // End of writers

                // Get the country
                movie.setCountry(DOMHelper.getValueFromElement(eMovie, COUNTRY));

                // Get the genres
                nlElements = eMovie.getElementsByTagName("genres");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eGenre = (Element) nElements;
                            movie.addGenre(eGenre.getTextContent());
                        }
                    }
                }   // End of genres

                // Get the cast (actors)
                nlElements = eMovie.getElementsByTagName("cast");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element ePerson = (Element) nElements;
                            movie.addActor(ePerson.getTextContent());
                        }
                    }
                }   // End of cast

                // Process the sets
                nlElements = eMovie.getElementsByTagName("sets");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eSet = (Element) nElements;
                            String order = eSet.getAttribute(ORDER);
                            if (StringTools.isValidString(order)) {
                                movie.addSet(eSet.getTextContent(), Integer.parseInt(order));
                            } else {
                                movie.addSet(eSet.getTextContent());
                            }
                        }
                    }
                }   // End of sets

                // Get certification
                movie.setCertification(DOMHelper.getValueFromElement(eMovie, "certification"));

                // Get language
                movie.setLanguage(DOMHelper.getValueFromElement(eMovie, LANGUAGE));

                // Get subtitles
                movie.setSubtitles(DOMHelper.getValueFromElement(eMovie, "subtitles"));

                // Get the TrailerExchange
                movie.setTrailerExchange(DOMHelper.getValueFromElement(eMovie, "trailerExchange").equalsIgnoreCase(YES));

                // Get trailerLastScan date/time
                movie.setTrailerLastScan(DOMHelper.getValueFromElement(eMovie, TRAILER_LAST_SCAN));

                // Get file container
                movie.setContainer(DOMHelper.getValueFromElement(eMovie, "container"));

                nlElements = eMovie.getElementsByTagName("codecs");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            String codecType = nElements.getNodeName();
                            if (nElements.getChildNodes().getLength() > 0) {
                                for (int cLooper = 0; cLooper < nElements.getChildNodes().getLength(); cLooper++) {
                                    Node nCodec = nElements.getChildNodes().item(cLooper);
                                    if (nCodec.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eCodec = (Element) nCodec;

                                        Codec codec;
                                        if (CodecType.VIDEO.toString().equalsIgnoreCase(codecType)) {
                                            codec = new Codec(CodecType.VIDEO);
                                        } else {
                                            codec = new Codec(CodecType.AUDIO);
                                        }
                                        codec.setCodecId(eCodec.getAttribute("codecId"));
                                        codec.setCodecIdHint(eCodec.getAttribute("codecIdHint"));
                                        codec.setCodecFormat(eCodec.getAttribute("format"));
                                        codec.setCodecFormatProfile(eCodec.getAttribute("formatProfile"));
                                        codec.setCodecFormatVersion(eCodec.getAttribute("formatVersion"));
                                        codec.setCodecLanguage(eCodec.getAttribute(LANGUAGE));
                                        codec.setCodecBitRate(eCodec.getAttribute("bitrate"));
                                        String tmpValue = eCodec.getAttribute("channels");
                                        if (StringUtils.isNotBlank(tmpValue)) {
                                            codec.setCodecChannels(Integer.parseInt(eCodec.getAttribute("channels")));
                                        }
                                        codec.setCodec(eCodec.getTextContent().trim());

                                        tmpValue = eCodec.getAttribute("source");
                                        if (StringTools.isValidString(tmpValue)) {
                                            codec.setCodecSource(CodecSource.fromString(tmpValue));
                                        } else {
                                            codec.setCodecSource(CodecSource.UNKNOWN);
                                        }

                                        movie.addCodec(codec);
                                    }
                                }   // END of codec information for audio/video
                            }
                        }   // END of audio/video codec
                    }   // END of codecs loop
                }   // END of codecs

                // get the resolution
                movie.setResolution(DOMHelper.getValueFromElement(eMovie, "resolution"));

                // get the video source
                movie.setVideoSource(DOMHelper.getValueFromElement(eMovie, "videoSource"));

                // get the video output
                movie.setVideoOutput(DOMHelper.getValueFromElement(eMovie, "videoOutput"));

                // get aspect ratio
                movie.setAspectRatio(aspectTools.cleanAspectRatio(DOMHelper.getValueFromElement(eMovie, "aspect")));

                // get frames per second
                movie.setFps(Float.parseFloat(DOMHelper.getValueFromElement(eMovie, "fps")));

                // Get navigation info
                movie.setFirst(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "first")));
                movie.setPrevious(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "previous")));
                movie.setNext(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "next")));
                movie.setLast(HTMLTools.decodeUrl(DOMHelper.getValueFromElement(eMovie, "last")));

                // Get the library description
                movie.setLibraryDescription(DOMHelper.getValueFromElement(eMovie, "libraryDescription"));

                // Get prebuf
                movie.setPrebuf(Long.parseLong(DOMHelper.getValueFromElement(eMovie, "prebuf")));

                // Issue 1901: Awards
                nlElements = eMovie.getElementsByTagName("awards");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eAwardEvent = (Element) nElements;
                            AwardEvent awardEvent = new AwardEvent();
                            awardEvent.setName(eAwardEvent.getAttribute(NAME));

                            Node nAward;
                            for (int loopAwards = 0; loopAwards < eAwardEvent.getChildNodes().getLength(); loopAwards++) {
                                nAward = eAwardEvent.getChildNodes().item(loopAwards);
                                if (nAward.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eAward = (Element) nAward;
                                    Award award = new Award();

                                    award.setName(eAward.getTextContent());
                                    award.setNominated(Integer.parseInt(eAward.getAttribute("nominated")));
                                    award.setWon(Integer.parseInt(eAward.getAttribute(WON)));
                                    award.setYear(Integer.parseInt(eAward.getAttribute(YEAR)));
                                    String tmpAward = eAward.getAttribute("wons");
                                    if (StringTools.isValidString(tmpAward)) {
                                        award.setWons(Arrays.asList(tmpAward.split(Movie.SPACE_SLASH_SPACE)));
                                    }
                                    tmpAward = eAward.getAttribute("nominations");
                                    if (StringTools.isValidString(tmpAward)) {
                                        award.setNominations(Arrays.asList(tmpAward.split(Movie.SPACE_SLASH_SPACE)));
                                    }

                                    awardEvent.addAward(award);
                                }
                            }   // End of Awards

                            movie.addAward(awardEvent);
                        }
                    }
                }   // End of AwardEvents

                // Issue 1897: Cast enhancement
                nlElements = eMovie.getElementsByTagName("people");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();

                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element ePerson = (Element) nElements;
                            Filmography person = new Filmography();

                            person.setCastId(ePerson.getAttribute("cast_id"));
                            person.setCharacter(ePerson.getAttribute(CHARACTER));
                            person.setDepartment(ePerson.getAttribute(DEPARTMENT));
                            person.setDoublage(ePerson.getAttribute("doublage"));
                            person.setId(ePerson.getAttribute("id"));
                            person.setJob(ePerson.getAttribute(JOB));
                            person.setName(ePerson.getAttribute(NAME));
                            person.setOrder(ePerson.getAttribute(ORDER));
                            person.setTitle(ePerson.getAttribute(TITLE));
                            person.setUrl(ePerson.getAttribute(URL));
                            person.setPhotoFilename(ePerson.getAttribute("photoFile"));
                            person.setFilename(ePerson.getTextContent());

                            // Get any "id_???" values
                            for (int loopAttr = 0; loopAttr < ePerson.getAttributes().getLength(); loopAttr++) {
                                Node nPersonAttr = ePerson.getAttributes().item(loopAttr);
                                if (nPersonAttr.getNodeName().startsWith(ID)) {
                                    String name = nPersonAttr.getNodeName().replace(ID, "");
                                    person.setId(name, nPersonAttr.getNodeValue());
                                }
                            }
                            movie.addPerson(person);
                        }
                    }
                }   // End of Cast

                // Issue 2012: Financial information about movie
                nlElements = eMovie.getElementsByTagName("business");
                for (int looper = 0; looper < nlElements.getLength(); looper++) {
                    nElements = nlElements.item(looper);
                    if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                        Element eBusiness = (Element) nElements;
                        movie.setBudget(eBusiness.getAttribute("budget"));

                        Node nCountry;
                        for (int loopBus = 0; loopBus < eBusiness.getChildNodes().getLength(); loopBus++) {
                            nCountry = eBusiness.getChildNodes().item(loopBus);
                            if (nCountry.getNodeType() == Node.ELEMENT_NODE) {
                                Element eCountry = (Element) nCountry;
                                if (eCountry.getNodeName().equalsIgnoreCase("gross")) {
                                    movie.setGross(eCountry.getAttribute(COUNTRY), eCountry.getTextContent());
                                } else if (eCountry.getNodeName().equalsIgnoreCase("openweek")) {
                                    movie.setOpenWeek(eCountry.getAttribute(COUNTRY), eCountry.getTextContent());
                                }
                            }
                        }   // End of budget info
                    }
                }   // End of business info

                // Issue 2013: Add trivia
                if (enableTrivia) {
                    nlElements = eMovie.getElementsByTagName("trivia");
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        movie.addDidYouKnow(nElements.getTextContent());
                    }
                }   // End of trivia info

                // Get the file list
                nlElements = eMovie.getElementsByTagName("files");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eFile = (Element) nElements;
                            MovieFile movieFile = new MovieFile();

                            String attr = eFile.getAttribute(TITLE);
                            if (StringTools.isValidString(attr)) {
                                movieFile.setTitle(attr);
                            }

                            attr = eFile.getAttribute(SEASON);
                            if (StringUtils.isNumeric(attr)) {
                                movieFile.setSeason(Integer.parseInt(attr));
                            }

                            attr = eFile.getAttribute("firstPart");
                            if (StringUtils.isNumeric(attr)) {
                                movieFile.setFirstPart(Integer.parseInt(attr));
                            }

                            attr = eFile.getAttribute("lastPart");
                            if (StringUtils.isNumeric(attr)) {
                                movieFile.setLastPart(Integer.parseInt(attr));
                            }

                            attr = eFile.getAttribute("subtitlesExchange");
                            if (StringTools.isValidString(attr)) {
                                movieFile.setSubtitlesExchange(attr.equalsIgnoreCase(YES));
                            }

                            attr = eFile.getAttribute("watched");
                            if (StringTools.isValidString(attr)) {
                                movieFile.setWatched(Boolean.parseBoolean(attr));
                            }

                            try {
                                File mfFile = new File(DOMHelper.getValueFromElement(eFile, "fileLocation"));
                                // Check to see if the file exists, or we are preserving the jukebox
                                if (mfFile.exists() || MovieJukebox.isJukeboxPreserve()) {
                                    // Save the file to the MovieFile
                                    movieFile.setFile(mfFile);

                                } else {
                                    // We can't find this file anymore, so skip it.
                                    logger.debug("Missing video file in the XML file (" + mfFile.getName() + "), it may have been moved or no longer exist.");
                                    continue;
                                }
                            } catch (Exception ignore) {
                                // If there is an error creating the file then don't save anything
                                logger.debug("XMLWriter: Failed parsing file " + xmlFile.getName());
                                continue;
                            }

                            movieFile.setFilename(DOMHelper.getValueFromElement(eFile, "fileURL"));

                            if (DOMHelper.getValueFromElement(eFile, "fileArchiveName") != null) {
                                movieFile.setArchiveName(DOMHelper.getValueFromElement(eFile, "fileArchiveName"));
                            }



                            // We need to get the part from the fileTitle
                            NodeList nlFileParts = eFile.getElementsByTagName("fileTitle");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        String part = eFileParts.getAttribute(PART);
                                        if (StringUtils.isNumeric(part)) {
                                            movieFile.setTitle(NumberUtils.toInt(part, 0), eFileParts.getTextContent());
                                        } else {
                                            movieFile.setTitle(eFileParts.getTextContent());
                                        }
                                    }
                                }
                            }

                            // Get the airs info
                            nlFileParts = eFile.getElementsByTagName("airsInfo");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);

                                        movieFile.setAirsAfterSeason(part, eFileParts.getAttribute("afterSeason"));
                                        movieFile.setAirsBeforeEpisode(part, eFileParts.getAttribute("beforeEpisode"));
                                        movieFile.setAirsBeforeSeason(part, eFileParts.getAttribute("beforeSeason"));
                                    }
                                }
                            }

                            // Get first aired information
                            nlFileParts = eFile.getElementsByTagName("firstAired");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);
                                        movieFile.setFirstAired(part, eFileParts.getTextContent());
                                    }
                                }
                            }

                            // get the file Plot
                            nlFileParts = eFile.getElementsByTagName("filePlot");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);
                                        movieFile.setPlot(part, eFileParts.getTextContent());
                                    }
                                }
                            }

                            // get the file rating
                            nlFileParts = eFile.getElementsByTagName("fileRating");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);
                                        movieFile.setRating(part, eFileParts.getTextContent());
                                    }
                                }
                            }

                            // get the file image url
                            nlFileParts = eFile.getElementsByTagName("fileImageURL");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);
                                        movieFile.setVideoImageURL(part, HTMLTools.decodeUrl(eFileParts.getTextContent()));
                                    }
                                }
                            }

                            // get the file image filename
                            nlFileParts = eFile.getElementsByTagName("fileImageFile");
                            if (nlFileParts.getLength() > 0) {
                                for (int looperFile = 0; looperFile < nlFileParts.getLength(); looperFile++) {
                                    Node nFileParts = nlFileParts.item(looperFile);
                                    if (nFileParts.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eFileParts = (Element) nFileParts;
                                        int part = NumberUtils.toInt(eFileParts.getAttribute(PART), 1);
                                        movieFile.setVideoImageFilename(part, HTMLTools.decodeUrl(eFileParts.getTextContent()));
                                    }
                                }
                            }

                            movieFile.setWatchedDateString(DOMHelper.getValueFromElement(eFile, "watchedDate"));

                            // This is not a new file
                            movieFile.setNewFile(Boolean.FALSE);

                            // Add the movie file to the movie
                            movie.addMovieFile(movieFile);
                        }
                    }
                }   // END of files

                // Get the extra list
                nlElements = eMovie.getElementsByTagName("extras");
                if (nlElements.getLength() > 0) {
                    nlElements = nlElements.item(0).getChildNodes();
                    for (int looper = 0; looper < nlElements.getLength(); looper++) {
                        nElements = nlElements.item(looper);
                        if (nElements.getNodeType() == Node.ELEMENT_NODE) {
                            Element eExtra = (Element) nElements;

                            String extraTitle = eExtra.getAttribute(TITLE);
                            String extraFilename = eExtra.getTextContent();

                            if (!extraTitle.isEmpty() && !extraFilename.isEmpty()) {
                                boolean exist = Boolean.FALSE;
                                if (extraFilename.startsWith("http:")) {
                                    // This is a URL from a NFO file
                                    ExtraFile ef = new ExtraFile();
                                    ef.setNewFile(Boolean.FALSE);
                                    ef.setTitle(extraTitle);
                                    ef.setFilename(extraFilename);
                                    movie.addExtraFile(ef, Boolean.FALSE);  // Add to the movie, but it's not dirty
                                    exist = Boolean.TRUE;
                                } else {
                                    // Check for existing files
                                    for (ExtraFile ef : movie.getExtraFiles()) {
                                        // Check if the movie has already the extra file
                                        if (ef.getFilename().equals(extraFilename)) {
                                            exist = Boolean.TRUE;
                                            // the extra file is old
                                            ef.setNewFile(Boolean.FALSE);
                                            break;
                                        }
                                    }
                                }

                                if (!exist) {
                                    // the extra file has been deleted so force the dirty flag
                                    forceDirtyFlag = Boolean.TRUE;
                                }
                            }
                        }
                    }
                }   // END of extras

            }   // End of ELEMENT_NODE
        }   // End of Movie Loop

        // This is a new movie, so clear the current dirty flags
        movie.clearDirty();
        movie.setDirty(DirtyFlag.INFO, forceDirtyFlag || movie.hasNewMovieFiles() || movie.hasNewExtraFiles());

        return Boolean.TRUE;
    }

    public boolean parseSetXML(File xmlSetFile, Movie setMaster, List<Movie> moviesList) {
        boolean forceDirtyFlag = Boolean.FALSE;

        try {
            Collection<String> xmlSetMovieNames = new ArrayList<String>();
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlSetFile), "UTF-8");
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                String tag = e.toString();

                if (tag.equalsIgnoreCase("<baseFilename>")) {
                    xmlSetMovieNames.add(parseCData(r));
                }
            }

            int counter = setMaster.getSetSize();
            if (counter == xmlSetMovieNames.size()) {
                for (String movieName : xmlSetMovieNames) {
                    for (Movie movie : moviesList) {
                        if (movie.getBaseName().equals(movieName)) {
                            // See if the movie is in a collection OR isDirty
                            forceDirtyFlag |= (!movie.isTVShow() && !movie.getSetsKeys().contains(setMaster.getTitle())) || movie.isDirty(DirtyFlag.INFO);
                            counter--;
                            break;
                        }
                    }

                    // Stop if the Set is dirty, no need to check more
                    if (forceDirtyFlag) {
                        break;
                    }
                }
                forceDirtyFlag |= counter != 0;
            } else {
                forceDirtyFlag = true;
            }
        } catch (Exception error) {
            logger.error("Failed parsing " + xmlSetFile.getAbsolutePath() + ": please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        }

        setMaster.setDirty(DirtyFlag.INFO, forceDirtyFlag);

        return true;
    }

    public boolean parsePersonXML(File xmlFile, Person person) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlFile), "UTF-8");
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                String tag = e.toString();

                if (tag.toLowerCase().startsWith("<id ")) {
                    String personDatabase = ImdbPlugin.IMDB_PLUGIN_ID;
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("persondb")) {
                            personDatabase = attr.getValue();
                            continue;
                        }
                    }
                    person.setId(personDatabase, parseCData(r));
                }
                if (tag.equalsIgnoreCase("<name>")) {
                    if (StringTools.isNotValidString(person.getName())) {
                        person.setName(parseCData(r));
                    } else {
                        person.addAka(parseCData(r));
                    }
                    continue;
                }
                if (tag.equalsIgnoreCase("<title>")) {
                    person.setTitle(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<baseFilename>")) {
                    person.setFilename(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<biography>")) {
                    person.setBiography(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<birthday>")) {
                    person.setYear(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<birthplace>")) {
                    person.setBirthPlace(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<birthname>")) {
                    person.setBirthName(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<url>")) {
                    person.setUrl(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<photoFile>")) {
                    person.setPhotoFilename(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<photoURL>")) {
                    person.setPhotoURL(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<backdropFile>")) {
                    person.setBackdropFilename(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<backdropURL>")) {
                    person.setBackdropURL(parseCData(r));
                    continue;
                }
                if (tag.equalsIgnoreCase("<knownMovies>")) {
                    person.setKnownMovies(Integer.parseInt(parseCData(r)));
                    continue;
                }
                if (tag.equalsIgnoreCase("<version>")) {
                    person.setVersion(Integer.parseInt(parseCData(r)));
                    continue;
                }
                if (tag.equalsIgnoreCase("<lastModifiedAt>")) {
                    person.setLastModifiedAt(parseCData(r));
                    continue;
                }
                if (tag.toLowerCase().startsWith("<movie ")) {
                    Filmography film = new Filmography();

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("id")) {
                            film.setId(attr.getValue());
                            continue;
                        }
                        if (ns.toLowerCase().contains(ID)) {
                            person.setId(ns.substring(3), attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(NAME)) {
                            film.setName(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(TITLE)) {
                            film.setTitle(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(ORIGINAL_TITLE)) {
                            film.setOriginalTitle(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(YEAR)) {
                            film.setYear(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(RATING)) {
                            film.setRating(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(CHARACTER)) {
                            film.setCharacter(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(JOB)) {
                            film.setJob(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(DEPARTMENT)) {
                            film.setDepartment(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase(URL)) {
                            film.setUrl(attr.getValue());
                            continue;
                        }
                    }
                    film.setFilename(parseCData(r));
                    film.setDirty(false);
                    person.addFilm(film);
                }
            }
            person.setFilename();
        } catch (Exception error) {
            logger.error("Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return false;
        }

        person.setDirty(false);

        return true;
    }

    public void writeCategoryXML(Jukebox jukebox, Library library, String filename, boolean isDirty)
            throws FileNotFoundException, XMLStreamException, ParserConfigurationException {
        // Issue 1886: HTML indexes recreated every time
        File oldFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + filename + EXT_XML);

        if (oldFile.exists() && !isDirty) {
            // Even if the library is not dirty, these files need to be added to the safe jukebox list
            FileTools.addJukeboxFile(filename + EXT_XML);
            FileTools.addJukeboxFile(filename + EXT_HTML);
            return;
        }

        jukebox.getJukeboxTempLocationDetailsFile().mkdirs();

        File xmlFile = new File(jukebox.getJukeboxTempLocationDetailsFile(), filename + EXT_XML);
        FileTools.addJukeboxFile(filename + EXT_XML);

        Document xmlDoc = DOMHelper.createDocument();
        Element eLibrary = xmlDoc.createElement("library");
        int libraryCount = 0;

        // Issue 1148, generate category in the order specified in properties
        logger.info("  Indexing " + filename + "...");
        for (String categoryName : categoriesDisplayList) {
            int categoryMinCount = Library.calcMinCategoryCount(categoryName);
            int categoryCount = 0;

            for (Entry<String, Index> category : library.getIndexes().entrySet()) {
                // Category not empty and match the current cat.
                if (!category.getValue().isEmpty() && categoryName.equalsIgnoreCase(category.getKey())
                        && (filename.equals(Library.INDEX_CATEGORIES) || filename.equals(category.getKey()))) {
                    Element eCategory = xmlDoc.createElement("category");

                    eCategory.setAttribute(NAME, category.getKey());

                    if ("Other".equalsIgnoreCase(categoryName)) {
                        // Process the other category using the order listed in the category.xml file
                        Map<String, String> cm = new LinkedHashMap<String, String>(library.getCategoriesMap());

                        // Tidy up the new categories if needed
                        String newAll = cm.get(Library.INDEX_NEW);
                        String newTV = cm.get(Library.INDEX_NEW_TV);
                        String newMovie = cm.get(Library.INDEX_MOVIES);

                        // If the New-TV is named the same as the New, remove it
                        if (StringUtils.isNotBlank(newAll) && StringUtils.isNotBlank(newTV) && newAll.equalsIgnoreCase(newTV)) {
                            cm.remove(Library.INDEX_NEW_TV);
                        }

                        // If the New-Movie is named the same as the New, remove it
                        if (StringUtils.isNotBlank(newAll) && StringUtils.isNotBlank(newMovie) && newAll.equalsIgnoreCase(newMovie)) {
                            cm.remove(Library.INDEX_NEW_MOVIE);
                        }

                        // If the New-TV is named the same as the New-Movie, remove it
                        if (StringUtils.isNotBlank(newTV) && StringUtils.isNotBlank(newMovie) && newTV.equalsIgnoreCase(newMovie)) {
                            cm.remove(Library.INDEX_NEW_TV);
                        }

                        for (Map.Entry<String, String> catOriginalName : cm.entrySet()) {
                            String catNewName = catOriginalName.getValue();
                            if (category.getValue().containsKey(catNewName)) {
                                Element eCatIndex = processCategoryIndex(xmlDoc, catNewName, catOriginalName.getKey(), category.getValue().get(catNewName),
                                        categoryName, categoryMinCount, library);
                                if (eCatIndex != null) {
                                    eCategory.appendChild(eCatIndex);
                                    categoryCount++;
                                }
                            }
                        }
                    } else {
                        TreeMap<String, List<Movie>> sortedMap;

                        // Sort the certification according to certification.ordering
                        if (Library.INDEX_CERTIFICATION.equalsIgnoreCase(categoryName)) {
                            List<String> certificationOrdering = new ArrayList<String>();
                            String certificationOrder = PropertiesUtil.getProperty("certification.ordering");
                            if (StringUtils.isNotBlank(certificationOrder)) {
                                for (String cert : certificationOrder.split(",")) {
                                    certificationOrdering.add(cert.trim());
                                }
                            }

                            // Process the certification in order of the certification.ordering property
                            sortedMap = new TreeMap<String, List<Movie>>(new CertificationComparator(certificationOrdering));
                        } else if (!sortLibrary && Library.INDEX_LIBRARY.equalsIgnoreCase(categoryName)) {
                            // Issue 2359, disable sorting the list of libraries so that the entries in categories.xml are written in the same order as the list of libraries in library.xml
                            sortedMap = new TreeMap<String, List<Movie>>(new CertificationComparator(Library.getLibraryOrdering()));
                        } else {
                            // Sort the remaining categories
                            sortedMap = new TreeMap<String, List<Movie>>(new SortIgnorePrefixesComparator());
                        }
                        sortedMap.putAll(category.getValue());

                        for (Map.Entry<String, List<Movie>> index : sortedMap.entrySet()) {
                            Element eCatIndex = processCategoryIndex(xmlDoc, index.getKey(), index.getKey(), index.getValue(), categoryName, categoryMinCount,
                                    library);
                            if (eCatIndex != null) {
                                eCategory.appendChild(eCatIndex);
                                categoryCount++;
                            }
                        }

                    }

                    // If there is nothing in the category, don't write it out
                    if (categoryCount > 0) {
                        if (!isPlayOnHD) {
                            // Add the correct count to the index
                            eCategory.setAttribute(COUNT, String.valueOf(categoryCount));
                        }
                        eLibrary.appendChild(eCategory);
                        libraryCount++;
                    }
                }
            }
        }

        // Add the movie count to the library
        eLibrary.setAttribute(COUNT, String.valueOf(libraryCount));

        // Add the library node to the document
        xmlDoc.appendChild(eLibrary);

        // Add in the movies to the categories if needed
        if (includeMoviesInCategories) {
            // For the Categories file we want to split out a version without the movies for Eversion
            if (Library.INDEX_CATEGORIES.equals(filename)) {
                logger.debug("Writing non-movie categories file...");
                // Create the eversion filename
                File xmlEvFile = new File(jukebox.getJukeboxTempLocationDetailsFile(), filename + evFileSuffix + EXT_XML);
                // Add the eversion file to the cleanup list
                FileTools.addJukeboxFile(xmlEvFile.getName());

                // Write out the current index before adding the movies
                DOMHelper.writeDocumentToFile(xmlDoc, xmlEvFile);
            }

            Element eMovie;
            for (Movie movie : library.getMoviesList()) {
                if (fullMovieInfoInIndexes) {
                    eMovie = writeMovie(xmlDoc, movie, library);
                } else {
                    eMovie = writeMovieForIndex(xmlDoc, movie);
                }

                // Add the movie
                eLibrary.appendChild(eMovie);
            }
        }

        DOMHelper.writeDocumentToFile(xmlDoc, xmlFile);
    }

    private Element processCategoryIndex(Document doc, String indexName, String indexOriginalName, List<Movie> indexMovies, String categoryKey,
            int categoryMinCount, Library library) {
        List<Movie> allMovies = library.getMoviesList();
        int countMovieCat = library.getMovieCountForIndex(categoryKey, indexName);

        StringBuilder sb = new StringBuilder();
        sb.append("Index: ");
        sb.append(categoryKey);
        sb.append(", Category: ");
        sb.append(indexName);
        sb.append(", count: ");
        sb.append(indexMovies.size());
        logger.debug(sb.toString());

        // Display a message about the category we're indexing
        if (countMovieCat < categoryMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryKey)) {
            sb = new StringBuilder();
            sb.append("Category '");
            sb.append(categoryKey);
            sb.append("' '");
            sb.append(indexName);
            sb.append("' does not contain enough videos (");
            sb.append(countMovieCat);
            sb.append("/");
            sb.append(categoryMinCount);
            sb.append("), not adding to categories.xml.");

            logger.debug(sb.toString());
            return null;
        }

        if (setsExcludeTV && categoryKey.equalsIgnoreCase(Library.INDEX_SET) && indexMovies.get(0).isTVShow()) {
            // Do not include the video in the set because it's a TV show
            return null;
        }

        String indexFilename = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, indexName)) + "1";

        Element eCategory = doc.createElement(INDEX);
        eCategory.setAttribute(NAME, indexName);
        eCategory.setAttribute(ORIGINAL_NAME, indexOriginalName);

        if (includeMoviesInCategories) {
            eCategory.setAttribute("filename", indexFilename);

            for (Identifiable movie : indexMovies) {
                DOMHelper.appendChild(doc, eCategory, MOVIE, String.valueOf(allMovies.indexOf(movie)));
            }
        } else {
            eCategory.setTextContent(indexFilename);
        }

        return eCategory;
    }

    /**
     * Write the set of index XML files for the library
     *
     * @throws Throwable
     */
    public void writeIndexXML(final Jukebox jukebox, final Library library, ThreadExecutor<Void> tasks) throws Throwable {
        int indexCount = 0;
        int indexSize = library.getIndexes().size();

        final boolean setReindex = PropertiesUtil.getBooleanProperty("mjb.sets.reindex", TRUE);

        StringBuilder loggerString;

        tasks.restart();

        for (Map.Entry<String, Index> category : library.getIndexes().entrySet()) {
            final String categoryName = category.getKey();
            Map<String, List<Movie>> index = category.getValue();
            final int categoryMinCount = Library.calcMinCategoryCount(categoryName);
            int categoryMaxCount = Library.calcMaxCategoryCount(categoryName);
            final int movieMaxCount = Library.calcMaxMovieCount(categoryName);
            final boolean toLimitCategory = categoriesLimitList.contains(categoryName);

            loggerString = new StringBuilder("  Indexing ");
            loggerString.append(categoryName).append(" (").append(++indexCount).append("/").append(indexSize);
            loggerString.append(") contains ").append(index.size()).append(index.size() == 1 ? " index" : " indexes");
            loggerString.append(toLimitCategory && categoryMaxCount > 0 && index.size() > categoryMaxCount ? (" (limit to " + categoryMaxCount + ")") : "");
            logger.info(loggerString);

            ArrayList<Map.Entry<String, List<Movie>>> groupArray = new ArrayList<Map.Entry<String, List<Movie>>>(index.entrySet());
            Collections.sort(groupArray, new IndexComparator(library, categoryName));
            Iterator<Map.Entry<String, List<Movie>>> itr = groupArray.iterator();

            int currentCategoryCount = 0;
            while (itr.hasNext()) {
                final Map.Entry<String, List<Movie>> group = itr.next();
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws XMLStreamException, FileNotFoundException {
                        List<Movie> movies = group.getValue();
                        String key = FileTools.createCategoryKey(group.getKey());
                        String categoryPath = categoryName + " " + key;

                        // FIXME This is horrible! Issue 735 will get rid of it.
                        int categoryCount = library.getMovieCountForIndex(categoryName, key);
                        if (categoryCount < categoryMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryName)
                                && !Library.INDEX_SET.equalsIgnoreCase(categoryName)) {
                            StringBuilder loggerString = new StringBuilder();
                            loggerString.append("Category '").append(categoryPath).append("' does not contain enough videos (");
                            loggerString.append(categoryCount).append("/").append(categoryMinCount).append("), skipping XML generation.");
                            logger.debug(loggerString);
                            return null;
                        }
                        boolean skipIndex = !forceIndexOverwrite;

                        // Try and determine if the set contains TV shows and therefore use the TV show settings
                        // TODO have a custom property so that you can set this on a per-set basis.
                        int nbVideosPerPage = nbMoviesPerPage, nbVideosPerLine = nbMoviesPerLine;

                        if (movies.size() > 0) {
                            if (key.equalsIgnoreCase(Library.getRenamedCategory(Library.INDEX_TVSHOWS))) {
                                nbVideosPerPage = nbTvShowsPerPage;
                                nbVideosPerLine = nbTvShowsPerLine;
                            }

                            if (categoryName.equalsIgnoreCase(Library.INDEX_SET)) {
                                if (movies.get(0).isTVShow()) {
                                    nbVideosPerPage = nbTVSetMoviesPerPage;
                                    nbVideosPerLine = nbTVSetMoviesPerLine;
                                } else {
                                    nbVideosPerPage = nbSetMoviesPerPage;
                                    nbVideosPerLine = nbSetMoviesPerLine;
                                    // Issue 1886: HTML indexes recreated every time
                                    for (Movie m : library.getMoviesList()) {
                                        if (m.isSetMaster() && m.getTitle().equals(key)) {
                                            skipIndex &= !m.isDirty(DirtyFlag.INFO);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        List<Movie> tmpMovieList = movies;
                        int moviepos = 0;
                        for (Movie movie : movies) {
                            // Don't skip the index if the movie is dirty
                            if (movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.RECHECK)) {
                                skipIndex = false;
                            }

                            // Check for changes to the Watched, Unwatched and New categories whilst we are processing the All category
                            if (enableWatchScanner && key.equals(Library.getRenamedCategory(Library.INDEX_ALL))) {
                                if (movie.isWatched() && movie.isDirty(DirtyFlag.WATCHED)) {
                                    // Don't skip the index
                                    reindexWatched = true;
                                    reindexUnwatched = true;
                                    reindexNew = true;
                                }

                                if (!movie.isWatched() && movie.isDirty(DirtyFlag.WATCHED)) {
                                    // Don't skip the index
                                    reindexWatched = true;
                                    reindexUnwatched = true;
                                    reindexNew = true;
                                }
                            }

                            // Check to see if we are in one of the category indexes
                            if (reindexNew
                                    && (key.equals(Library.getRenamedCategory(Library.INDEX_NEW))
                                    || key.equals(Library.getRenamedCategory(Library.INDEX_NEW_MOVIE)) || key.equals(Library.getRenamedCategory(Library.INDEX_NEW_TV)))) {
                                skipIndex = false;
                            }

                            if (reindexWatched && key.equals(Library.getRenamedCategory(Library.INDEX_WATCHED))) {
                                skipIndex = false;
                            }

                            if (reindexUnwatched && key.equals(Library.getRenamedCategory(Library.INDEX_UNWATCHED))) {
                                skipIndex = false;
                            }

                            if (!beforeSortExplodeSet) {
                                // Issue 1263 - Allow explode of Set in category .
                                if (movie.isSetMaster() && (categoriesExplodeSet.contains(categoryName) || categoriesExplodeSet.contains(group.getKey()))
                                        && (!keepTVExplodeSet || !movie.isTVShow())) {
                                    List<Movie> boxedSetMovies = library.getIndexes().get(Library.INDEX_SET).get(movie.getTitle());
                                    boxedSetMovies = library.getMatchingMoviesList(categoryName, boxedSetMovies, key);
                                    logger.debug("Exploding set for " + categoryPath + "[" + movie.getTitle() + "] " + boxedSetMovies.size());
                                    // delay new instance
                                    if (tmpMovieList == movies) {
                                        tmpMovieList = new ArrayList<Movie>(movies);
                                    }

                                    // do we want to keep the set?
                                    // Issue 2002: remove SET item after explode of Set in category
                                    if (removeExplodeSet) {
                                        tmpMovieList.remove(moviepos);
                                    }
                                    tmpMovieList.addAll(moviepos, boxedSetMovies);
                                    moviepos += boxedSetMovies.size() - 1;
                                }
                                moviepos++;
                            }
                        }

                        if (toLimitCategory && movieMaxCount > 0 && tmpMovieList.size() > movieMaxCount) {
                            logger.debug("Limiting category " + categoryName + " " + key + " " + tmpMovieList.size() + " -> " + movieMaxCount);
                            while (tmpMovieList.size() > movieMaxCount) {
                                tmpMovieList.remove(tmpMovieList.size() - 1);
                            }
                        }

                        int last = 1 + (tmpMovieList.size() - 1) / nbVideosPerPage;
                        int previous = last;
                        moviepos = 0;
                        skipIndex = (skipIndex && Library.INDEX_LIBRARY.equalsIgnoreCase(categoryName)) ? !library.isDirtyLibrary(group.getKey()) : skipIndex;
                        IndexInfo idx = new IndexInfo(categoryName, key, last, nbVideosPerPage, nbVideosPerLine, skipIndex);

                        // Don't skip the indexing for sets as this overwrites the set files
                        if (Library.INDEX_SET.equalsIgnoreCase(categoryName) && setReindex) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Forcing generation of set index.");
                            }
                            skipIndex = false;
                        }

                        for (int current = 1; current <= last; current++) {
                            if (setReindex && Library.INDEX_SET.equalsIgnoreCase(categoryName)) {
                                idx.canSkip = false;
                            } else {
                                idx.checkSkip(current, jukebox.getJukeboxRootLocationDetails());
                                skipIndex &= idx.canSkip;
                            }
                        }

                        if (skipIndex && Library.INDEX_PERSON.equalsIgnoreCase(idx.categoryName)) {
                            for (Person person : library.getPeople()) {
                                if (!person.getName().equalsIgnoreCase(idx.key)) {
                                    continue;
                                }
                                if (!person.isDirty()) {
                                    continue;
                                }
                                skipIndex = false;
                                break;
                            }
                        }

                        StringBuilder logOutput = new StringBuilder("Category '");
                        logOutput.append(categoryPath);

                        if (skipIndex) {
                            logOutput.append("' - no change detected, skipping XML generation.");
                            logger.debug(logOutput.toString());

                            // Add the existing file to the cache so they aren't deleted
                            for (int current = 1; current <= last; current++) {
                                String name = idx.baseName + current + EXT_XML;
                                FileTools.addJukeboxFile(name);
                            }
                        } else {
                            logOutput.append("' - generating ");
                            logOutput.append(last);
                            logOutput.append(" XML file");
                            logOutput.append(last == 1 ? "." : "s.");
                            logger.debug(logOutput.toString());

                            int next;
                            for (int current = 1; current <= last; current++) {
                                // All pages are handled here
                                next = (current % last) + 1; // this gives 1 for last
                                writeIndexPage(library, tmpMovieList.subList(moviepos, Math.min(moviepos + nbVideosPerPage, tmpMovieList.size())),
                                        jukebox.getJukeboxTempLocationDetails(), idx, previous, current, next, last, tmpMovieList.size());

                                moviepos += nbVideosPerPage;
                                previous = current;
                            }
                        }

                        library.addGeneratedIndex(idx);
                        return null;
                    }
                });
                currentCategoryCount++;
                if (toLimitCategory && categoryMaxCount > 0 && currentCategoryCount >= categoryMaxCount) {
                    break;
                }
            }
        }
        tasks.waitFor();
    }

    /**
     * Write out the index pages
     *
     * @param library
     * @param movies
     * @param rootPath
     * @param idx
     * @param previous
     * @param current
     * @param next
     * @param last
     */
    public void writeIndexPage(Library library, List<Movie> movies, String rootPath, IndexInfo idx, int previous, int current, int next, int last, int indexCount) {
        String prefix = idx.baseName;
        File xmlFile = new File(rootPath, prefix + current + EXT_XML);

        Document xmlDoc;
        try {
            xmlDoc = DOMHelper.createDocument();
        } catch (ParserConfigurationException error) {
            logger.error("Failed writing index page: " + xmlFile.getName());
            logger.error(SystemTools.getStackTrace(error));
            return;
        }

        FileTools.addJukeboxFile(xmlFile.getName());
        boolean isCurrentKey;

        Element eLibrary = xmlDoc.createElement("library");
        int libraryCount = 0;

        for (Map.Entry<String, Index> category : library.getIndexes().entrySet()) {
            Element eCategory;
            int categoryCount = 0;

            String categoryKey = category.getKey();
            Map<String, List<Movie>> index = category.getValue();

            // Is this the current category?
            isCurrentKey = categoryKey.equalsIgnoreCase(idx.categoryName);
            if (!isCurrentKey && !fullCategoriesInIndexes) {
                // This isn't the current index, so we don't want it
                continue;
            }

            eCategory = xmlDoc.createElement("category");
            eCategory.setAttribute(NAME, categoryKey);
            if (isCurrentKey) {
                eCategory.setAttribute("current", TRUE);
            }

            int indexSize;
            if ("other".equalsIgnoreCase(categoryKey)) {
                // Process the other category using the order listed in the category.xml file
                Map<String, String> cm = new LinkedHashMap<String, String>(library.getCategoriesMap());

                // Tidy up the new categories if needed
                String newAll = cm.get(Library.INDEX_NEW);
                String newTV = cm.get(Library.INDEX_NEW_TV);
                String newMovie = cm.get(Library.INDEX_NEW_MOVIE);

                // If the New-TV is named the same as the New, remove it
                if (StringUtils.isNotBlank(newAll) && StringUtils.isNotBlank(newTV) && newAll.equalsIgnoreCase(newTV)) {
                    cm.remove(Library.INDEX_NEW_TV);
                }

                // If the New-Movie is named the same as the New, remove it
                if (StringUtils.isNotBlank(newAll) && StringUtils.isNotBlank(newMovie) && newAll.equalsIgnoreCase(newMovie)) {
                    cm.remove(Library.INDEX_NEW_MOVIE);
                }

                // If the New-TV is named the same as the New-Movie, remove it
                if (StringUtils.isNotBlank(newTV) && StringUtils.isNotBlank(newMovie) && newTV.equalsIgnoreCase(newMovie)) {
                    cm.remove(Library.INDEX_NEW_TV);
                }

                for (Map.Entry<String, String> catOriginalName : cm.entrySet()) {
                    String catNewName = catOriginalName.getValue();
                    if (category.getValue().containsKey(catNewName)) {
                        indexSize = index.get(catNewName).size();
                        Element eIndexCategory = processIndexCategory(xmlDoc, catNewName, categoryKey, isCurrentKey, idx, indexSize, previous, current, next,
                                last);
                        if (eIndexCategory != null) {
                            eCategory.appendChild(eIndexCategory);
                            categoryCount++;
                        }
                    }
                }

            } else {
                for (Map.Entry<String, List<Movie>> categoryName : index.entrySet()) {
                    indexSize = categoryName.getValue().size();

                    Element eIndexCategory = processIndexCategory(xmlDoc, categoryName.getKey(), categoryKey, isCurrentKey, idx, indexSize, previous, current, next, last);
                    if (eIndexCategory != null) {
                        eCategory.appendChild(eIndexCategory);
                        categoryCount++;
                    }
                }
            }

            // Only output the category if there are entries
            if (categoryCount > 0) {
                // Write the actual count of the category
                eCategory.setAttribute(COUNT, String.valueOf(categoryCount));
                eLibrary.appendChild(eCategory);
                libraryCount++;
            }
        }

        if (enablePeople && addPeopleInfo && (Library.INDEX_PERSON + Library.INDEX_CAST + Library.INDEX_DIRECTOR + Library.INDEX_WRITER).indexOf(idx.categoryName) > -1) {
            for (Person person : library.getPeople()) {
                if (!person.getName().equalsIgnoreCase(idx.key) && !person.getTitle().equalsIgnoreCase(idx.key)) {
                    boolean found = false;
                    if (!Library.INDEX_PERSON.equals(idx.categoryName)) {
                        for (String name : person.getAka()) {
                            if (name.equalsIgnoreCase(idx.key)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }
                eLibrary.appendChild(writePerson(xmlDoc, person));
                break;
            }
        }

        // FIXME: The count here is off. It needs to be correct
        Element eMovies = xmlDoc.createElement("movies");
        eMovies.setAttribute("cols", String.valueOf(idx.videosPerLine));
        eMovies.setAttribute(COUNT, String.valueOf(idx.videosPerPage));

        //eMovies.setAttribute("indexCount", String.valueOf(library.getMovieCountForIndex(idx.categoryName, idx.key)));
        eMovies.setAttribute("indexCount", String.valueOf(indexCount));
        eMovies.setAttribute("totalCount", String.valueOf(library.getMovieCountForIndex(Library.INDEX_OTHER, Library.INDEX_ALL)));

        if (fullMovieInfoInIndexes) {
            for (Movie movie : movies) {
                eMovies.appendChild(writeMovie(xmlDoc, movie, library));
            }
        } else {
            for (Movie movie : movies) {
                eMovies.appendChild(writeMovieForIndex(xmlDoc, movie));
            }
        }

        // Add the movies node to the Library node
        eLibrary.appendChild(eMovies);

        // Add the correct count to the library node
        eLibrary.setAttribute(COUNT, String.valueOf(libraryCount));

        // Add the Library node to the document
        xmlDoc.appendChild(eLibrary);

        // Save the document to file
        DOMHelper.writeDocumentToFile(xmlDoc, xmlFile);
    }

    private Element processIndexCategory(Document doc, String categoryName, String categoryKey, boolean isCurrentKey, IndexInfo idx, int indexSize,
            int previous, int current, int next, int last) {
        String encakey = FileTools.createCategoryKey(categoryName);
        boolean isCurrentCat = isCurrentKey && encakey.equalsIgnoreCase(idx.key);

        // Check to see if we need the non-current index
        if (!isCurrentCat && !fullCategoriesInIndexes) {
            // We don't need this index, so skip it
            return null;
        }

        // FIXME This is horrible! Issue 735 will get rid of it.
        if (indexSize < Library.calcMinCategoryCount(categoryName) && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryKey)) {
            return null;
        }

        String prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, encakey));

        Element eCategory = doc.createElement(INDEX);
        eCategory.setAttribute(NAME, categoryName);

        // The category changes only occur for "Other" category
        if (Library.INDEX_OTHER.equals(categoryKey)) {
            eCategory.setAttribute(ORIGINAL_NAME, Library.getOriginalCategory(encakey, Boolean.TRUE));
        }

        // if currently writing this page then add current attribute with value true
        if (isCurrentCat) {
            eCategory.setAttribute("current", TRUE);
            eCategory.setAttribute("first", prefix + '1');
            eCategory.setAttribute("previous", prefix + previous);
            eCategory.setAttribute("next", prefix + next);
            eCategory.setAttribute("last", prefix + last);
            eCategory.setAttribute("currentIndex", Integer.toString(current));
            eCategory.setAttribute("lastIndex", Integer.toString(last));
        }

        eCategory.setTextContent(prefix + '1');

        return eCategory;
    }

    /**
     * Return an Element with the movie details
     *
     * @param doc
     * @param movie
     * @return
     */
    private Element writeMovieForIndex(Document doc, Movie movie) {
        Element eMovie = doc.createElement(MOVIE);

        eMovie.setAttribute("isExtra", Boolean.toString(movie.isExtra()));
        eMovie.setAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            eMovie.setAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        eMovie.setAttribute("isTV", Boolean.toString(movie.isTVShow()));

        DOMHelper.appendChild(doc, eMovie, DETAILS, HTMLTools.encodeUrl(movie.getBaseName()) + EXT_HTML);
        DOMHelper.appendChild(doc, eMovie, "baseFilenameBase", movie.getBaseFilename());
        DOMHelper.appendChild(doc, eMovie, BASE_FILENAME, movie.getBaseName());
        DOMHelper.appendChild(doc, eMovie, TITLE, movie.getTitle());
        DOMHelper.appendChild(doc, eMovie, "titleSort", movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, ORIGINAL_TITLE, movie.getOriginalTitle());
        DOMHelper.appendChild(doc, eMovie, "detailPosterFile", HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "thumbnail", HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        DOMHelper.appendChild(doc, eMovie, "bannerFile", HTMLTools.encodeUrl(movie.getBannerFilename()));
        DOMHelper.appendChild(doc, eMovie, "certification", movie.getCertification());
        DOMHelper.appendChild(doc, eMovie, SEASON, Integer.toString(movie.getSeason()));

        // Return the generated movie
        return eMovie;
    }

    /**
     * Create an element based on a collection of items
     *
     * @param doc
     * @param set
     * @param element
     * @param items
     * @param library
     * @param cat
     * @return
     */
    private Element generateElementSet(Document doc, String set, String element, Collection<String> items, Library library, String cat) {

        if (items.size() > 0) {
            Element eSet = doc.createElement(set);
            eSet.setAttribute(COUNT, String.valueOf(items.size()));
            for (String item : items) {
                writeIndexedElement(doc, eSet, element, item, createIndexAttribute(library, cat, item));
            }
            return eSet;
        } else {
            return null;
        }
    }

    /**
     * Write the element with the indexed attribute.
     *
     * If there is a non-null value in the indexValue, this will be appended to the element.
     *
     * @param doc
     * @param parentElement
     * @param attributeName
     * @param attributeValue
     * @param indexValue
     */
    private void writeIndexedElement(Document doc, Element parentElement, String attributeName, String attributeValue, String indexValue) {
        if (indexValue == null) {
            DOMHelper.appendChild(doc, parentElement, attributeName, attributeValue);
        } else {
            DOMHelper.appendChild(doc, parentElement, attributeName, attributeValue, INDEX, indexValue);
        }
    }

    /**
     * Create the index filename for a category & value.
     *
     * Will return "null" if no index found
     *
     * @param library
     * @param categoryName
     * @param value
     * @return
     */
    private String createIndexAttribute(Library library, String categoryName, String value) {
        if (StringTools.isNotValidString(value)) {
            return null;
        }

        Index index = library.getIndexes().get(categoryName);
        if (null != index) {
            int categoryMinCount = Library.calcMinCategoryCount(categoryName);

            if (library.getMovieCountForIndex(categoryName, value) >= categoryMinCount) {
                return HTMLTools.encodeUrl(FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, value)) + 1);
            }
        }
        return null;
    }

    /**
     * Create an element with the movie details in it
     *
     * @param doc
     * @param movie
     * @param library
     * @return
     */
    private Element writeMovie(Document doc, Movie movie, Library library) {
        Element eMovie = doc.createElement(MOVIE);

        eMovie.setAttribute("isExtra", Boolean.toString(movie.isExtra()));
        eMovie.setAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            eMovie.setAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        eMovie.setAttribute("isTV", Boolean.toString(movie.isTVShow()));

        for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
            DOMHelper.appendChild(doc, eMovie, "id", e.getValue(), MOVIEDB, e.getKey());
        }

        DOMHelper.appendChild(doc, eMovie, "mjbVersion", SystemTools.getVersion());
        DOMHelper.appendChild(doc, eMovie, "mjbRevision", SystemTools.getRevision());
        DOMHelper.appendChild(doc, eMovie, "xmlGenerationDate", StringTools.convertDateToString(new Date(), StringTools.getDateFormatLongString()));
        DOMHelper.appendChild(doc, eMovie, "baseFilenameBase", movie.getBaseFilename());
        DOMHelper.appendChild(doc, eMovie, BASE_FILENAME, movie.getBaseName());
        DOMHelper.appendChild(doc, eMovie, TITLE, movie.getTitle());
        DOMHelper.appendChild(doc, eMovie, "titleSort", movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, ORIGINAL_TITLE, movie.getOriginalTitle());

        DOMHelper.appendChild(doc, eMovie, YEAR, movie.getYear(), "Year", Library.getYearCategory(movie.getYear()));

        DOMHelper.appendChild(doc, eMovie, "releaseDate", movie.getReleaseDate());
        DOMHelper.appendChild(doc, eMovie, "showStatus", movie.getShowStatus());

        // This is the main rating
        DOMHelper.appendChild(doc, eMovie, RATING, Integer.toString(movie.getRating()));

        // This is the list of ratings
        Element eRatings = doc.createElement("ratings");
        for (String site : movie.getRatings().keySet()) {
            DOMHelper.appendChild(doc, eRatings, RATING, Integer.toString(movie.getRating(site)), MOVIEDB, site);
        }
        eMovie.appendChild(eRatings);

        DOMHelper.appendChild(doc, eMovie, "watched", Boolean.toString(movie.isWatched()));
        DOMHelper.appendChild(doc, eMovie, "watchedNFO", Boolean.toString(movie.isWatchedNFO()));
        DOMHelper.appendChild(doc, eMovie, "watchedFile", Boolean.toString(movie.isWatchedFile()));
        if (movie.isWatched()) {
            DOMHelper.appendChild(doc, eMovie, "watchedDate", movie.getWatchedDateString());
        }
        DOMHelper.appendChild(doc, eMovie, "top250", Integer.toString(movie.getTop250()));
        DOMHelper.appendChild(doc, eMovie, DETAILS, HTMLTools.encodeUrl(movie.getBaseName()) + EXT_HTML);
        DOMHelper.appendChild(doc, eMovie, "posterURL", HTMLTools.encodeUrl(movie.getPosterURL()));
        DOMHelper.appendChild(doc, eMovie, "posterFile", HTMLTools.encodeUrl(movie.getPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "fanartURL", HTMLTools.encodeUrl(movie.getFanartURL()));
        DOMHelper.appendChild(doc, eMovie, "fanartFile", HTMLTools.encodeUrl(movie.getFanartFilename()));
        DOMHelper.appendChild(doc, eMovie, "detailPosterFile", HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "thumbnail", HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        DOMHelper.appendChild(doc, eMovie, "bannerURL", HTMLTools.encodeUrl(movie.getBannerURL()));
        DOMHelper.appendChild(doc, eMovie, "bannerFile", HTMLTools.encodeUrl(movie.getBannerFilename()));
        DOMHelper.appendChild(doc, eMovie, "clearLogoURL", HTMLTools.encodeUrl(movie.getClearLogoURL()));
        DOMHelper.appendChild(doc, eMovie, "clearLogoFile", HTMLTools.encodeUrl(movie.getClearLogoFilename()));
        DOMHelper.appendChild(doc, eMovie, "clearArtURL", HTMLTools.encodeUrl(movie.getClearArtURL()));
        DOMHelper.appendChild(doc, eMovie, "clearArtFile", HTMLTools.encodeUrl(movie.getClearArtFilename()));
        DOMHelper.appendChild(doc, eMovie, "tvThumbURL", HTMLTools.encodeUrl(movie.getTvThumbURL()));
        DOMHelper.appendChild(doc, eMovie, "tvThumbFile", HTMLTools.encodeUrl(movie.getTvThumbFilename()));
        DOMHelper.appendChild(doc, eMovie, "seasonThumbURL", HTMLTools.encodeUrl(movie.getSeasonThumbURL()));
        DOMHelper.appendChild(doc, eMovie, "seasonThumbFile", HTMLTools.encodeUrl(movie.getSeasonThumbFilename()));
        DOMHelper.appendChild(doc, eMovie, "movieDiscURL", HTMLTools.encodeUrl(movie.getMovieDiscURL()));
        DOMHelper.appendChild(doc, eMovie, "movieDiscFile", HTMLTools.encodeUrl(movie.getMovieDiscFilename()));

        // Removed for the time being until the artwork scanner is in place
        /*
         * Element eArtwork = doc.createElement("artwork"); for (ArtworkType artworkType : ArtworkType.values()) {
         * Collection<Artwork> artworkList = movie.getArtwork(artworkType); if (artworkList.size() > 0) { Element
         * eArtworkType;
         *
         * for (Artwork artwork : artworkList) { eArtworkType = doc.createElement(artworkType.toString());
         * eArtworkType.setAttribute(COUNT, String.valueOf(artworkList.size()));
         *
         * DOMHelper.appendChild(doc, eArtworkType, "sourceSite", artwork.getSourceSite()); DOMHelper.appendChild(doc,
         * eArtworkType, URL, artwork.getUrl());
         *
         * for (ArtworkFile artworkFile : artwork.getSizes()) { DOMHelper.appendChild(doc, eArtworkType,
         * artworkFile.getSize().toString(), artworkFile.getFilename(), "downloaded",
         * String.valueOf(artworkFile.isDownloaded())); } eArtwork.appendChild(eArtworkType); } } else { // Write a
         * dummy node Element eArtworkType = doc.createElement(artworkType.toString());
         * eArtworkType.setAttribute(COUNT, String.valueOf(0));
         *
         * DOMHelper.appendChild(doc, eArtworkType, URL, Movie.UNKNOWN); DOMHelper.appendChild(doc, eArtworkType,
         * ArtworkSize.LARGE.toString(), Movie.UNKNOWN, "downloaded", FALSE);
         *
         * eArtwork.appendChild(eArtworkType); } } eMovie.appendChild(eArtwork);
         */

        DOMHelper.appendChild(doc, eMovie, "plot", movie.getPlot());
        DOMHelper.appendChild(doc, eMovie, "outline", movie.getOutline());
        DOMHelper.appendChild(doc, eMovie, "quote", movie.getQuote());
        DOMHelper.appendChild(doc, eMovie, "tagline", movie.getTagline());

        writeIndexedElement(doc, eMovie, COUNTRY, movie.getCountry(), createIndexAttribute(library, Library.INDEX_COUNTRY, movie.getCountry()));
        if (xmlCompatible) {
            Element eCountry = doc.createElement("countries");
            int cnt = 0;
            for (String country : movie.getCountry().split(Movie.SPACE_SLASH_SPACE)) {
                writeIndexedElement(doc, eCountry, "land", country, createIndexAttribute(library, Library.INDEX_COUNTRY, country));
                cnt++;
            }
            eCountry.setAttribute(COUNT, String.valueOf(cnt));
            eMovie.appendChild(eCountry);
        }

        DOMHelper.appendChild(doc, eMovie, "company", movie.getCompany());
        if (xmlCompatible) {
            Element eCompany = doc.createElement("companies");
            int cnt = 0;
            for (String company : movie.getCompany().split(Movie.SPACE_SLASH_SPACE)) {
                DOMHelper.appendChild(doc, eCompany, "credit", company);
                cnt++;
            }
            eCompany.setAttribute(COUNT, String.valueOf(cnt));
            eMovie.appendChild(eCompany);
        }

        DOMHelper.appendChild(doc, eMovie, "runtime", movie.getRuntime());
        DOMHelper.appendChild(doc, eMovie, "certification", Library.getIndexingCertification(movie.getCertification()));
        DOMHelper.appendChild(doc, eMovie, SEASON, Integer.toString(movie.getSeason()));

        DOMHelper.appendChild(doc, eMovie, LANGUAGE, movie.getLanguage());
        if (xmlCompatible) {
            Element eLanguage = doc.createElement("languages");
            int cnt = 0;
            for (String language : movie.getLanguage().split(Movie.SPACE_SLASH_SPACE)) {
                DOMHelper.appendChild(doc, eLanguage, "lang", language);
                cnt++;
            }
            eLanguage.setAttribute(COUNT, String.valueOf(cnt));
            eMovie.appendChild(eLanguage);
        }

        DOMHelper.appendChild(doc, eMovie, "subtitles", movie.getSubtitles());
        if (xmlCompatible) {
            Element eSubtitle = doc.createElement("subs");
            int cnt = 0;
            for (String subtitle : movie.getSubtitles().split(Movie.SPACE_SLASH_SPACE)) {
                DOMHelper.appendChild(doc, eSubtitle, "subtitle", subtitle);
                cnt++;
            }
            eSubtitle.setAttribute(COUNT, String.valueOf(cnt));
            eMovie.appendChild(eSubtitle);
        }

        DOMHelper.appendChild(doc, eMovie, "trailerExchange", movie.isTrailerExchange() ? YES : "NO");

        if (movie.getTrailerLastScan() == 0) {
            DOMHelper.appendChild(doc, eMovie, TRAILER_LAST_SCAN, Movie.UNKNOWN);
        } else {
            try {
                DOMHelper.appendChild(doc, eMovie, TRAILER_LAST_SCAN, StringTools.getDateFormat().format(movie.getTrailerLastScan()));
            } catch (Exception error) {
                DOMHelper.appendChild(doc, eMovie, TRAILER_LAST_SCAN, Movie.UNKNOWN);
            }
        }

        DOMHelper.appendChild(doc, eMovie, "container", movie.getContainer());
        DOMHelper.appendChild(doc, eMovie, "videoCodec", movie.getVideoCodec());
        DOMHelper.appendChild(doc, eMovie, "audioCodec", movie.getAudioCodec());

        // Write codec information
        eMovie.appendChild(createCodecsElement(doc, movie.getCodecs()));
        DOMHelper.appendChild(doc, eMovie, "audioChannels", movie.getAudioChannels());
        DOMHelper.appendChild(doc, eMovie, "resolution", movie.getResolution());

        // If the source is unknown, use the default source
        if (StringTools.isNotValidString(movie.getVideoSource())) {
            DOMHelper.appendChild(doc, eMovie, "videoSource", defaultSource);
        } else {
            DOMHelper.appendChild(doc, eMovie, "videoSource", movie.getVideoSource());
        }

        DOMHelper.appendChild(doc, eMovie, "videoOutput", movie.getVideoOutput());
        DOMHelper.appendChild(doc, eMovie, "aspect", movie.getAspectRatio());
        DOMHelper.appendChild(doc, eMovie, "fps", Float.toString(movie.getFps()));

        if (movie.getFileDate() == null) {
            DOMHelper.appendChild(doc, eMovie, "fileDate", Movie.UNKNOWN);
        } else {
            // Try to catch any date re-formatting errors
            try {
                DOMHelper.appendChild(doc, eMovie, "fileDate", StringTools.getDateFormat().format(movie.getFileDate()));
            } catch (ArrayIndexOutOfBoundsException error) {
                DOMHelper.appendChild(doc, eMovie, "fileDate", Movie.UNKNOWN);
            }
        }
        DOMHelper.appendChild(doc, eMovie, "fileSize", movie.getFileSizeString());
        DOMHelper.appendChild(doc, eMovie, "first", HTMLTools.encodeUrl(movie.getFirst()));
        DOMHelper.appendChild(doc, eMovie, "previous", HTMLTools.encodeUrl(movie.getPrevious()));
        DOMHelper.appendChild(doc, eMovie, "next", HTMLTools.encodeUrl(movie.getNext()));
        DOMHelper.appendChild(doc, eMovie, "last", HTMLTools.encodeUrl(movie.getLast()));
        DOMHelper.appendChild(doc, eMovie, "libraryDescription", movie.getLibraryDescription());
        DOMHelper.appendChild(doc, eMovie, "prebuf", Long.toString(movie.getPrebuf()));

        if (movie.getGenres().size() > 0) {
            Element eGenres = doc.createElement("genres");
            eGenres.setAttribute(COUNT, String.valueOf(movie.getGenres().size()));
            for (String genre : movie.getGenres()) {
                writeIndexedElement(doc, eGenres, "genre", genre, createIndexAttribute(library, Library.INDEX_GENRES, Library.getIndexingGenre(genre)));
            }
            eMovie.appendChild(eGenres);
        }

        Collection<String> items = movie.getSetsKeys();
        if (items.size() > 0) {
            Element eSets = doc.createElement("sets");
            eSets.setAttribute(COUNT, String.valueOf(items.size()));
            for (String item : items) {
                Element eSetItem = doc.createElement("set");
                Integer order = movie.getSetOrder(item);
                if (null != order) {
                    eSetItem.setAttribute(ORDER, String.valueOf(order));
                }
                String index = createIndexAttribute(library, Library.INDEX_SET, item);
                if (null != index) {
                    eSetItem.setAttribute(INDEX, index);
                }

                eSetItem.setTextContent(item);
                eSets.appendChild(eSetItem);
            }
            eMovie.appendChild(eSets);
        }

        writeIndexedElement(doc, eMovie, "director", movie.getDirector(), createIndexAttribute(library, Library.INDEX_DIRECTOR, movie.getDirector()));

        Element eSet;
        eSet = generateElementSet(doc, "directors", "director", movie.getDirectors(), library, Library.INDEX_DIRECTOR);
        if (eSet != null) {
            eMovie.appendChild(eSet);
        }

        eSet = generateElementSet(doc, "writers", "writer", movie.getWriters(), library, Library.INDEX_WRITER);
        if (eSet != null) {
            eMovie.appendChild(eSet);
        }

        eSet = generateElementSet(doc, "cast", "actor", movie.getCast(), library, Library.INDEX_CAST);
        if (eSet != null) {
            eMovie.appendChild(eSet);
        }

        // Issue 1901: Awards
        if (enableAwards) {
            Collection<AwardEvent> awards = movie.getAwards();
            if (awards != null && awards.size() > 0) {
                Element eAwards = doc.createElement("awards");
                eAwards.setAttribute(COUNT, String.valueOf(awards.size()));
                for (AwardEvent event : awards) {
                    Element eEvent = doc.createElement("event");
                    eEvent.setAttribute(NAME, event.getName());
                    eEvent.setAttribute(COUNT, String.valueOf(event.getAwards().size()));
                    for (Award award : event.getAwards()) {
                        Element eAwardItem = doc.createElement("award");
                        eAwardItem.setAttribute(NAME, award.getName());
                        eAwardItem.setAttribute(WON, Integer.toString(award.getWon()));
                        eAwardItem.setAttribute("nominated", Integer.toString(award.getNominated()));
                        eAwardItem.setAttribute(YEAR, Integer.toString(award.getYear()));
                        if (award.getWons() != null && award.getWons().size() > 0) {
                            eAwardItem.setAttribute("wons", StringUtils.join(award.getWons(), Movie.SPACE_SLASH_SPACE));
                            if (xmlCompatible) {
                                for (String won : award.getWons()) {
                                    DOMHelper.appendChild(doc, eAwardItem, WON, won);
                                }
                            }
                        }
                        if (award.getNominations() != null && award.getNominations().size() > 0) {
                            eAwardItem.setAttribute("nominations", StringUtils.join(award.getNominations(), Movie.SPACE_SLASH_SPACE));
                            if (xmlCompatible) {
                                for (String nomination : award.getNominations()) {
                                    DOMHelper.appendChild(doc, eAwardItem, "nomination", nomination);
                                }
                            }
                        }
                        if (!xmlCompatible) {
                            eAwardItem.setTextContent(award.getName());
                        }
                        eEvent.appendChild(eAwardItem);
                    }
                    eAwards.appendChild(eEvent);
                }
                eMovie.appendChild(eAwards);
            }
        }

        // Issue 1897: Cast enhancement
        if (enablePeople) {
            Collection<Filmography> people = movie.getPeople();
            if (people != null && people.size() > 0) {
                Element ePeople = doc.createElement("people");
                ePeople.setAttribute(COUNT, String.valueOf(people.size()));
                for (Filmography person : people) {
                    Element ePerson = doc.createElement("person");

                    ePerson.setAttribute(NAME, person.getName());
                    ePerson.setAttribute("doublage", person.getDoublage());
                    ePerson.setAttribute(TITLE, person.getTitle());
                    ePerson.setAttribute(CHARACTER, person.getCharacter());
                    ePerson.setAttribute(JOB, person.getJob());
                    ePerson.setAttribute("id", person.getId());
                    for (Map.Entry<String, String> personID : person.getIdMap().entrySet()) {
                        if (!personID.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                            ePerson.setAttribute(ID + personID.getKey(), personID.getValue());
                        }
                    }
                    ePerson.setAttribute(DEPARTMENT, person.getDepartment());
                    ePerson.setAttribute(URL, person.getUrl());
                    ePerson.setAttribute(ORDER, Integer.toString(person.getOrder()));
                    ePerson.setAttribute("cast_id", Integer.toString(person.getCastId()));
                    ePerson.setAttribute("photoFile", person.getPhotoFilename());
                    String inx = createIndexAttribute(library, Library.INDEX_PERSON, person.getName());
                    if (inx != null) {
                        ePerson.setAttribute(INDEX, inx);
                    }
                    ePerson.setTextContent(person.getFilename());
                    ePeople.appendChild(ePerson);
                }
                eMovie.appendChild(ePeople);
            }
        }

        // Issue 2012: Financial information about movie
        if (enableBusiness) {
            Element eBusiness = doc.createElement("business");
            eBusiness.setAttribute("budget", movie.getBudget());

            for (Map.Entry<String, String> gross : movie.getGross().entrySet()) {
                DOMHelper.appendChild(doc, eBusiness, "gross", gross.getValue(), COUNTRY, gross.getKey());
            }

            for (Map.Entry<String, String> openweek : movie.getOpenWeek().entrySet()) {
                DOMHelper.appendChild(doc, eBusiness, "openweek", openweek.getValue(), COUNTRY, openweek.getKey());
            }

            eMovie.appendChild(eBusiness);
        }

        // Issue 2013: Add trivia
        if (enableTrivia) {
            Element eTrivia = doc.createElement("didyouknow");
            eTrivia.setAttribute(COUNT, String.valueOf(movie.getDidYouKnow().size()));

            for (String trivia : movie.getDidYouKnow()) {
                DOMHelper.appendChild(doc, eTrivia, "trivia", trivia);
            }

            eMovie.appendChild(eTrivia);
        }

        // Write the indexes that the movie belongs to
        Element eIndexes = doc.createElement("indexes");
        String originalName;
        for (Entry<String, String> index : movie.getIndexes().entrySet()) {
            Element eIndexEntry = doc.createElement(INDEX);
            eIndexEntry.setAttribute("type", index.getKey());
            originalName = Library.getOriginalCategory(index.getKey(), Boolean.TRUE);
            eIndexEntry.setAttribute(ORIGINAL_NAME, originalName);
            eIndexEntry.setAttribute("encoded", FileTools.makeSafeFilename(index.getValue()));
            eIndexEntry.setTextContent(index.getValue());
            eIndexes.appendChild(eIndexEntry);
        }
        eMovie.appendChild(eIndexes);

        // Write details about the files
        Element eFiles = doc.createElement("files");
        for (MovieFile mf : movie.getFiles()) {
            Element eFileItem = doc.createElement("file");
            eFileItem.setAttribute(SEASON, Integer.toString(mf.getSeason()));
            eFileItem.setAttribute("firstPart", Integer.toString(mf.getFirstPart()));
            eFileItem.setAttribute("lastPart", Integer.toString(mf.getLastPart()));
            eFileItem.setAttribute(TITLE, mf.getTitle());
            eFileItem.setAttribute("subtitlesExchange", mf.isSubtitlesExchange() ? YES : "NO");

            // Fixes an issue with null file lengths
            try {
                if (mf.getFile() == null) {
                    eFileItem.setAttribute("size", "0");
                } else {
                    eFileItem.setAttribute("size", Long.toString(mf.getSize()));
                }
            } catch (Exception error) {
                logger.debug("XML Writer: File length error for file " + mf.getFilename());
                eFileItem.setAttribute("size", "0");
            }

            // Playlink values; can be empty, but not null
            for (Map.Entry<String, String> e : mf.getPlayLink().entrySet()) {
                eFileItem.setAttribute(e.getKey().toLowerCase(), e.getValue());
            }

            eFileItem.setAttribute("watched", mf.isWatched() ? TRUE : FALSE);

            if (mf.getFile() != null) {
                DOMHelper.appendChild(doc, eFileItem, "fileLocation", mf.getFile().getAbsolutePath());
            }

            // Write the fileURL
            String filename = mf.getFilename();
            // Issue 1237: Add "VIDEO_TS.IFO" for PlayOnHD VIDEO_TS path names
            if (isPlayOnHD && filename.toUpperCase().endsWith("VIDEO_TS")) {
                filename = filename + "/VIDEO_TS.IFO";
            }

            // If attribute was set, save it back out.
            String archiveName = mf.getArchiveName();
            if (StringTools.isValidString(archiveName)) {
                logger.debug("MovieJukeboxXMLWriter: getArchivename is '" + archiveName + "' for " + mf.getFilename() + " length " + archiveName.length());
            }

            if (StringTools.isValidString(archiveName)) {
                DOMHelper.appendChild(doc, eFileItem, "fileArchiveName", archiveName);

                // If they want full URL, do so
                if (isExtendedURL && !filename.endsWith(archiveName)) {
                    filename = filename + "/" + archiveName;
                }
            }

            DOMHelper.appendChild(doc, eFileItem, "fileURL", filename);

            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); ++part) {
                DOMHelper.appendChild(doc, eFileItem, "fileTitle", mf.getTitle(part), PART, Integer.toString(part));

                // Only write out these for TV Shows
                if (movie.isTVShow()) {
                    Map<String, String> tvAttribs = new HashMap<String, String>();
                    tvAttribs.put(PART, Integer.toString(part));
                    tvAttribs.put("afterSeason", mf.getAirsAfterSeason(part));
                    tvAttribs.put("beforeSeason", mf.getAirsBeforeSeason(part));
                    tvAttribs.put("beforeEpisode", mf.getAirsBeforeEpisode(part));
                    DOMHelper.appendChild(doc, eFileItem, "airsInfo", String.valueOf(part), tvAttribs);

                    DOMHelper.appendChild(doc, eFileItem, "firstAired", mf.getFirstAired(part), PART, String.valueOf(part));
                }

                if (StringTools.isValidString(mf.getWatchedDateString())) {
                    DOMHelper.appendChild(doc, eFileItem, "watchedDate", mf.getWatchedDateString());
                }

                if (includeEpisodePlots) {
                    DOMHelper.appendChild(doc, eFileItem, "filePlot", mf.getPlot(part), PART, String.valueOf(part));
                }

                if (includeEpisodeRating) {
                    DOMHelper.appendChild(doc, eFileItem, "fileRating", mf.getRating(part), PART, String.valueOf(part));
                }

                if (includeVideoImages) {
                    DOMHelper.appendChild(doc, eFileItem, "fileImageURL", HTMLTools.encodeUrl(mf.getVideoImageURL(part)), PART, String.valueOf(part));
                    DOMHelper.appendChild(doc, eFileItem, "fileImageFile", HTMLTools.encodeUrl(mf.getVideoImageFilename(part)), PART, String.valueOf(part));
                }
            }
            eFiles.appendChild(eFileItem);
        }
        eMovie.appendChild(eFiles);

        Collection<ExtraFile> extraFiles = movie.getExtraFiles();
        if (extraFiles != null && extraFiles.size() > 0) {
            Element eExtras = doc.createElement("extras");
            for (ExtraFile ef : extraFiles) {
                Element eExtraItem = doc.createElement("extra");
                eExtraItem.setAttribute(TITLE, ef.getTitle());
                if (ef.getPlayLink() != null) {
                    // Playlink values
                    for (Map.Entry<String, String> e : ef.getPlayLink().entrySet()) {
                        eExtraItem.setAttribute(e.getKey().toLowerCase(), e.getValue());
                    }
                }
                eExtraItem.setTextContent(ef.getFilename()); // should already be URL-encoded
                eExtras.appendChild(eExtraItem);
            }
            eMovie.appendChild(eExtras);
        }

        return eMovie;
    }

    /**
     * Create an element with the codec information in it.
     *
     * @param doc
     * @param movieCodecs
     * @return
     */
    private Element createCodecsElement(Document doc, Set<Codec> movieCodecs) {
        Element eCodecs = doc.createElement("codecs");
        Element eCodecAudio = doc.createElement("audio");
        Element eCodecVideo = doc.createElement("video");
        int countAudio = 0;
        int countVideo = 0;

        HashMap<String, String> codecAttribs = new HashMap<String, String>();

        for (Codec codec : movieCodecs) {
            codecAttribs.clear();

            codecAttribs.put("format", codec.getCodecFormat());
            codecAttribs.put("formatProfile", codec.getCodecFormatProfile());
            codecAttribs.put("formatVersion", codec.getCodecFormatVersion());
            codecAttribs.put("codecId", codec.getCodecId());
            codecAttribs.put("codecIdHint", codec.getCodecIdHint());
            codecAttribs.put("source", codec.getCodecSource().toString());
            codecAttribs.put("bitrate", codec.getCodecBitRate());
            if (codec.getCodecType() == CodecType.AUDIO) {
                codecAttribs.put(LANGUAGE, codec.getCodecLanguage());
                codecAttribs.put("langugageFull", codec.getCodecFullLanguage());
                codecAttribs.put("channels", String.valueOf(codec.getCodecChannels()));
                DOMHelper.appendChild(doc, eCodecAudio, "codec", codec.getCodec(), codecAttribs);
                countAudio++;
            } else {
                DOMHelper.appendChild(doc, eCodecVideo, "codec", codec.getCodec(), codecAttribs);
                countVideo++;
            }
        }
        eCodecAudio.setAttribute(COUNT, String.valueOf(countAudio));
        eCodecVideo.setAttribute(COUNT, String.valueOf(countVideo));
        eCodecs.appendChild(eCodecAudio);
        eCodecs.appendChild(eCodecVideo);

        return eCodecs;
    }

    /**
     * Persist a movie into an XML file. Doesn't overwrite an already existing XML file for the specified movie unless,
     * movie's data has changed or forceXMLOverwrite is true.
     */
    public void writeMovieXML(Jukebox jukebox, Movie movie, Library library) {
        String baseName = movie.getBaseName();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + EXT_XML);
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + baseName + EXT_XML);

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.RECHECK) || movie.isDirty(DirtyFlag.WATCHED)) {
            Document xmlDoc;
            try {
                xmlDoc = DOMHelper.createDocument();
            } catch (ParserConfigurationException error) {
                logger.error("Failed writing " + tempXmlFile.getAbsolutePath());
                logger.error(SystemTools.getStackTrace(error));
                return;
            }

            Element eDetails = xmlDoc.createElement(DETAILS);
            xmlDoc.appendChild(eDetails);

            Element eMovie = writeMovie(xmlDoc, movie, library);

            if (eMovie != null) {
                eDetails.appendChild(eMovie);
            }

            DOMHelper.writeDocumentToFile(xmlDoc, tempXmlFile);

            if (writeNfoFiles) {
                MovieNFOWriter.writeNfoFile(jukebox, movie);
            }
        }
    }

    private Element writePerson(Document doc, Person person) {
        Element ePerson = doc.createElement("person");

        for (Map.Entry<String, String> e : person.getIdMap().entrySet()) {
            DOMHelper.appendChild(doc, ePerson, "id", e.getValue(), "persondb", e.getKey());
        }

        DOMHelper.appendChild(doc, ePerson, NAME, person.getName());
        DOMHelper.appendChild(doc, ePerson, TITLE, person.getTitle());
        DOMHelper.appendChild(doc, ePerson, BASE_FILENAME, person.getFilename());

        if (person.getAka().size() > 0) {
            Element eAka = doc.createElement("aka");
            for (String aka : person.getAka()) {
                DOMHelper.appendChild(doc, eAka, NAME, aka);
            }
            ePerson.appendChild(eAka);
        }

        DOMHelper.appendChild(doc, ePerson, "biography", person.getBiography());
        DOMHelper.appendChild(doc, ePerson, "birthday", person.getYear());
        DOMHelper.appendChild(doc, ePerson, "birthplace", person.getBirthPlace());
        DOMHelper.appendChild(doc, ePerson, "birthname", person.getBirthName());
        DOMHelper.appendChild(doc, ePerson, URL, person.getUrl());
        DOMHelper.appendChild(doc, ePerson, "photoFile", person.getPhotoFilename());
        DOMHelper.appendChild(doc, ePerson, "photoURL", person.getPhotoURL());
        DOMHelper.appendChild(doc, ePerson, "backdropFile", person.getBackdropFilename());
        DOMHelper.appendChild(doc, ePerson, "backdropURL", person.getBackdropURL());
        DOMHelper.appendChild(doc, ePerson, "knownMovies", String.valueOf(person.getKnownMovies()));

        if (person.getFilmography().size() > 0) {
            Element eFilmography = doc.createElement("filmography");

            for (Filmography film : person.getFilmography()) {
                Element eMovie = doc.createElement(MOVIE);
                eMovie.setAttribute("id", film.getId());

                for (Map.Entry<String, String> e : film.getIdMap().entrySet()) {
                    if (!e.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                        eMovie.setAttribute(ID + e.getKey(), e.getValue());
                    }
                }
                eMovie.setAttribute(NAME, film.getName());
                eMovie.setAttribute(TITLE, film.getTitle());
                eMovie.setAttribute(ORIGINAL_TITLE, film.getOriginalTitle());
                eMovie.setAttribute(YEAR, film.getYear());
                eMovie.setAttribute(RATING, film.getRating());
                eMovie.setAttribute(CHARACTER, film.getCharacter());
                eMovie.setAttribute(JOB, film.getJob());
                eMovie.setAttribute(DEPARTMENT, film.getDepartment());
                eMovie.setAttribute(URL, film.getUrl());
                eMovie.setTextContent(film.getFilename());

                eFilmography.appendChild(eMovie);
            }
            ePerson.appendChild(eFilmography);
        }

        // Write the indexes that the people belongs to
        Element eIndexes = doc.createElement("indexes");
        String originalName;
        for (Entry<String, String> index : person.getIndexes().entrySet()) {
            Element eIndexEntry = doc.createElement(INDEX);
            eIndexEntry.setAttribute("type", index.getKey());
            originalName = Library.getOriginalCategory(index.getKey(), Boolean.TRUE);
            eIndexEntry.setAttribute(ORIGINAL_NAME, originalName);
            eIndexEntry.setAttribute("encoded", FileTools.makeSafeFilename(index.getValue()));
            eIndexEntry.setTextContent(index.getValue());
            eIndexes.appendChild(eIndexEntry);
        }
        ePerson.appendChild(eIndexes);

        DOMHelper.appendChild(doc, ePerson, "version", String.valueOf(person.getVersion()));
        DOMHelper.appendChild(doc, ePerson, "lastModifiedAt", person.getLastModifiedAt());

        return ePerson;
    }

    public void writePersonXML(Jukebox jukebox, Person person, Library library) {
        String baseName = person.getFilename();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + baseName + EXT_XML);
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + baseName + EXT_XML);
        finalXmlFile.getParentFile().mkdirs();
        tempXmlFile.getParentFile().mkdirs();

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || person.isDirty()) {
            try {
                Document personDoc = DOMHelper.createDocument();
                Element eDetails = personDoc.createElement(DETAILS);
                eDetails.appendChild(writePerson(personDoc, person));
                personDoc.appendChild(eDetails);
                DOMHelper.writeDocumentToFile(personDoc, tempXmlFile);
            } catch (ParserConfigurationException error) {
                logger.error("Failed writing person XML for " + tempXmlFile.getName());
                logger.error(SystemTools.getStackTrace(error));
            }
        }
    }
}
