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
package com.moviejukebox.reader;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.*;
import com.moviejukebox.model.Attachment.*;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.AspectRatioTools;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import static com.moviejukebox.tools.XMLHelper.parseCData;
import static com.moviejukebox.writer.MovieJukeboxXMLWriter.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
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

public class MovieJukeboxXMLReader {

    private static final Logger logger = Logger.getLogger(MovieJukeboxXMLReader.class);
    private static final String LOG_MESSAGE = "XMLReader: ";
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    // Should we scrape the trivia information
    private static boolean enableTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", FALSE);

    /**
     * Parse a single movie detail XML file
     */
    public boolean parseMovieXML(File xmlFile, Movie movie) {
        boolean forceDirtyFlag = Boolean.FALSE; // force dirty flag for example when extras have been deleted
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getDocFromFile(xmlFile);
        } catch (MalformedURLException error) {
            logger.error(LOG_MESSAGE + "Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (IOException error) {
            logger.error(LOG_MESSAGE + "Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (ParserConfigurationException error) {
            logger.error(LOG_MESSAGE + "Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (SAXException error) {
            logger.error(LOG_MESSAGE + "Failed parsing XML (" + xmlFile.getName() + ") for movie. Please fix it or remove it.");
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
                movie.setTitleSort(DOMHelper.getValueFromElement(eMovie, SORT_TITLE));
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

                // Get the library description, if it's not been set elsewhere (e.g. scanner)
                String tempLibraryDescription = DOMHelper.getValueFromElement(eMovie, "libraryDescription");
                if (StringTools.isNotValidString(movie.getLibraryDescription())) {
                    movie.setLibraryDescription(tempLibraryDescription);
                } else if (!movie.getLibraryDescription().equals(tempLibraryDescription)) {
                    // The current description is different to the one in the XML
                    logger.debug(LOG_MESSAGE + "Different library description! Setting dirty INFO");
                    forceDirtyFlag = Boolean.TRUE;
                }

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
                                    logger.debug(LOG_MESSAGE + "Missing video file in the XML file (" + mfFile.getName() + "), it may have been moved or no longer exist.");
                                    continue;
                                }
                            } catch (Exception ignore) {
                                // If there is an error creating the file then don't save anything
                                logger.debug(LOG_MESSAGE + "Failed parsing file " + xmlFile.getName());
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

                            NodeList nlAttachments = eMovie.getElementsByTagName("attachments");
                            if (nlAttachments.getLength() > 0) {
                                nlAttachments = nlAttachments.item(0).getChildNodes();
                                for (int looperAtt = 0; looperAtt < nlAttachments.getLength(); looperAtt++) {
                                    Node nAttachment = nlAttachments.item(looperAtt);
                                    if (nAttachment.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eAttachment = (Element) nAttachment;
                                        Attachment attachment = new Attachment();
                                        attachment.setType(AttachmentType.fromString(eAttachment.getAttribute("type")));
                                        attachment.setAttachmentId(Integer.parseInt(DOMHelper.getValueFromElement(eAttachment, "attachmentId")));
                                        attachment.setContentType(ContentType.fromString(DOMHelper.getValueFromElement(eAttachment, "contentType")));
                                        attachment.setMimeType(DOMHelper.getValueFromElement(eAttachment, "mimeType"));
                                        attachment.setPart(Integer.parseInt(DOMHelper.getValueFromElement(eAttachment, "part")));
                                        attachment.setSourceFile(movieFile.getFile());
                                        movieFile.addAttachment(attachment);
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

        XMLEventReader r = null;
        try {
            Collection<String> xmlSetMovieNames = new ArrayList<String>();
            XMLInputFactory factory = XMLInputFactory.newInstance();
            r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlSetFile), "UTF-8");
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
                forceDirtyFlag = Boolean.TRUE;
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed parsing " + xmlSetFile.getAbsolutePath() + ": please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (XMLStreamException ex) {
                    // ignore
                }
            }
        }

        setMaster.setDirty(DirtyFlag.INFO, forceDirtyFlag);

        return Boolean.TRUE;
    }

    public boolean parsePersonXML(File xmlFile, Person person) {
        XMLEventReader r = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlFile), "UTF-8");

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
                    film.setDirty(Boolean.FALSE);
                    person.addFilm(film);
                }
            }
            person.setFilename();
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
            logger.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (XMLStreamException ex) {
                    // ignore
                }
            }
        }

        person.setDirty(Boolean.FALSE);

        return Boolean.TRUE;
    }
}
