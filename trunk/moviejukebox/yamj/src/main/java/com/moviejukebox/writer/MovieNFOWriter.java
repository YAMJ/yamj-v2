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

import com.moviejukebox.model.*;
import com.moviejukebox.reader.MovieNFOReader;
import com.moviejukebox.tools.*;
import java.io.File;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author stuart.boston
 */
public class MovieNFOWriter {

    private static final Logger logger = Logger.getLogger(MovieNFOWriter.class);
    private static final String LOG_MESSAGE = "MovieNFOWriter: ";
    private static boolean writeSimpleNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.writeSimpleFiles", Boolean.FALSE.toString());
    private static boolean extractCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", Boolean.TRUE.toString());
    private static boolean enablePeople = PropertiesUtil.getBooleanProperty("mjb.people", Boolean.FALSE.toString());

    /**
     * Write a NFO file for the movie using the data gathered
     *
     * @param jukebox
     * @param movie
     */
    public static void writeNfoFile(Jukebox jukebox, Movie movie) {
        // Don't write NFO files for sets or extras
        if (movie.isSetMaster() || movie.isExtra()) {
            return;
        }

        Document docNFO;
        Element eRoot, eRatings, eCredits, eDirectors, eActors;

        try {
            docNFO = DOMHelper.createDocument();
        } catch (ParserConfigurationException error) {
            logger.warn(LOG_MESSAGE + "Failed to create NFO file for " + movie.getBaseFilename());
            logger.error(SystemTools.getStackTrace(error));
            return;
        }

        String nfoFolder = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), "NFO");
        (new File(nfoFolder)).mkdirs();
        File tempNfoFile = new File(StringTools.appendToPath(nfoFolder, movie.getBaseName() + ".nfo"));

        logger.debug(LOG_MESSAGE + "Writing " + (writeSimpleNfoFiles ? "simple " : "") + "NFO file for " + movie.getBaseName() + ".nfo");
        FileTools.addJukeboxFile(tempNfoFile.getName());

        // Define the root element
        if (movie.isTVShow()) {
            eRoot = docNFO.createElement(MovieNFOReader.TYPE_TVSHOW);
        } else {
            eRoot = docNFO.createElement(MovieNFOReader.TYPE_MOVIE);
        }
        docNFO.appendChild(eRoot);

        for (String site : movie.getIdMap().keySet()) {
            DOMHelper.appendChild(docNFO, eRoot, "id", movie.getId(site), "moviedb", site);
        }

        if (!writeSimpleNfoFiles) {
            if (StringTools.isValidString(movie.getTitle())) {
                DOMHelper.appendChild(docNFO, eRoot, "title", movie.getTitle());
            }

            if (StringTools.isValidString(movie.getOriginalTitle())) {
                DOMHelper.appendChild(docNFO, eRoot, "originaltitle", movie.getOriginalTitle());
            }

            if (StringTools.isValidString(movie.getTitleSort())) {
                DOMHelper.appendChild(docNFO, eRoot, "sorttitle", movie.getTitleSort());
            }

            if (StringTools.isValidString(movie.getYear())) {
                DOMHelper.appendChild(docNFO, eRoot, "year", movie.getYear());
            }

            if (StringTools.isValidString(movie.getOutline())) {
                DOMHelper.appendChild(docNFO, eRoot, "outline", movie.getOutline());
            }

            if (StringTools.isValidString(movie.getPlot())) {
                DOMHelper.appendChild(docNFO, eRoot, "plot", movie.getPlot());
            }

            if (StringTools.isValidString(movie.getTagline())) {
                DOMHelper.appendChild(docNFO, eRoot, "tagline", movie.getTagline());
            }

            if (StringTools.isValidString(movie.getRuntime())) {
                DOMHelper.appendChild(docNFO, eRoot, "runtime", movie.getRuntime());
            }

            if (StringTools.isValidString(movie.getReleaseDate())) {
                DOMHelper.appendChild(docNFO, eRoot, "premiered", movie.getReleaseDate());
            }

            if (StringTools.isValidString(movie.getShowStatus())) {
                DOMHelper.appendChild(docNFO, eRoot, "showStatus", movie.getReleaseDate());
            }

            if (movie.getRating() >= 0) {
                eRatings = docNFO.createElement("ratings");
                eRoot.appendChild(eRatings);

                for (String site : movie.getRatings().keySet()) {
                    DOMHelper.appendChild(docNFO, eRatings, "rating", String.valueOf(movie.getRating(site)), "moviedb", site);
                }
            }

            if (StringTools.isValidString(movie.getCertification())) {
                if (extractCertificationFromMPAA) {
                    DOMHelper.appendChild(docNFO, eRoot, "mpaa", movie.getCertification());
                } else {
                    DOMHelper.appendChild(docNFO, eRoot, "certification", movie.getCertification());
                }
            }

            if (!movie.getGenres().isEmpty()) {
                for (String genre : movie.getGenres()) {
                    DOMHelper.appendChild(docNFO, eRoot, "genre", genre);
                }
            }

            if (StringTools.isValidString(movie.getCompany())) {
                DOMHelper.appendChild(docNFO, eRoot, "company", movie.getCompany());
            }

            if (StringTools.isValidString(movie.getCountry())) {
                DOMHelper.appendChild(docNFO, eRoot, "country", movie.getCountry());
            }

            /*
             * Process the people information from the video If we are using people scraping, use that information,
             * otherwise revert to the standard people
             */
            if (enablePeople) {
                eActors = docNFO.createElement("actor");
                eCredits = docNFO.createElement("credits");
                eDirectors = docNFO.createElement("directors");
                int countActors = 0;
                int countCredits = 0;
                int countDirectors = 0;

                for (Filmography person : movie.getPeople()) {
                    if (person.getDepartment().equalsIgnoreCase(Filmography.DEPT_ACTORS)) {
                        countActors++;
                        DOMHelper.appendChild(docNFO, eActors, "name", person.getName());
                        DOMHelper.appendChild(docNFO, eActors, "role", person.getJob());
                    } else if (person.getDepartment().equalsIgnoreCase(Filmography.DEPT_DIRECTING)) {
                        countDirectors++;
                        DOMHelper.appendChild(docNFO, eDirectors, "director", person.getName());
                    } else {
                        // Add the person to the misc credits section
                        countCredits++;
                        DOMHelper.appendChild(docNFO, eCredits, person.getJob().toLowerCase(), person.getName());
                    }
                }

                // Only add the actors section if there were any
                if (countActors > 0) {
                    eRoot.appendChild(eActors);
                }

                // Only add the directors section if there were any
                if (countDirectors > 0) {
                    eRoot.appendChild(eDirectors);
                }

                // Only add the credits section if there were any
                if (countCredits > 0) {
                    eRoot.appendChild(eCredits);
                }
            } else {
                if (!movie.getCast().isEmpty()) {
                    eActors = docNFO.createElement("actor");
                    for (String actor : movie.getCast()) {
                        DOMHelper.appendChild(docNFO, eActors, "name", actor);
                        DOMHelper.appendChild(docNFO, eActors, "role", Filmography.DEPT_ACTORS);
                    }
                    eRoot.appendChild(eActors);
                }

                if (!movie.getWriters().isEmpty()) {
                    eCredits = docNFO.createElement("credits");
                    for (String writerCredit : movie.getWriters()) {
                        DOMHelper.appendChild(docNFO, eCredits, "writer", writerCredit);
                    }
                    eRoot.appendChild(eCredits);
                }

                if (!movie.getDirectors().isEmpty()) {
                    eDirectors = docNFO.createElement("directors");
                    for (String director : movie.getDirectors()) {
                        DOMHelper.appendChild(docNFO, eDirectors, "director", director);
                    }
                    eRoot.appendChild(eDirectors);
                }
            }

            // Add the subtitles
            if (StringTools.isValidString(movie.getSubtitles())) {
                DOMHelper.appendChild(docNFO, eRoot, "subtitles", movie.getSubtitles());
            }
            
            // Add the fileinfo format
            {
                Element eFileinfo = docNFO.createElement("fileinfo");
                Element eStreamDetails = docNFO.createElement("streamdetails");

                Element eCodec;
                for (Codec codec : movie.getCodecs()) {
                    if (codec.getCodecType() == CodecType.AUDIO) {
                        eCodec = docNFO.createElement("audio");
                        if (StringTools.isValidString(codec.getCodecLanguage())) {
                            DOMHelper.appendChild(docNFO, eCodec, "language", codec.getCodecLanguage());
                        }
                        if (StringTools.isValidString(codec.getCodecBitRate())) {
                            DOMHelper.appendChild(docNFO, eCodec, "bitrate", codec.getCodecBitRate());
                        }
                    } else {
                        eCodec = docNFO.createElement("video");
                        DOMHelper.appendChild(docNFO, eCodec, "aspect", movie.getAspectRatio());
                        String movieResolution = movie.getResolution();
                        if (StringTools.isValidString(movieResolution) && movieResolution.contains("x")) {
                            int locX = movieResolution.indexOf('x');
                            if (locX > 0) {
                                DOMHelper.appendChild(docNFO, eCodec, "width", movieResolution.substring(0, locX));
                                DOMHelper.appendChild(docNFO, eCodec, "height", movieResolution.substring(locX + 1));
                            }
                        }
                    }
                    DOMHelper.appendChild(docNFO, eCodec, "codec", codec.getCodec());
                    eStreamDetails.appendChild(eCodec);
                }

                eFileinfo.appendChild(eStreamDetails);


                eRoot.appendChild(eFileinfo);

            }
        }   // End of detailed NFO

        // Write out the sets
        if (movie.getSets() != null && movie.getSets().size()>0) {
            Element eSets = docNFO.createElement("sets");
            for (Entry<String,Integer> entry : movie.getSets().entrySet()) {
        	    Integer order = entry.getValue();
                if (order == null) {
                    DOMHelper.appendChild(docNFO, eSets, "set", entry.getKey());
                } else  {
                    DOMHelper.appendChild(docNFO, eSets, "set", entry.getKey(), "order", order.toString());
                }
            }
            eRoot.appendChild(eSets);
        }

        // Write out the episode details for any tv show files
        if (movie.isTVShow()) {
            for (MovieFile episodeFile : movie.getFiles()) {
                createEpisodeDetails(episodeFile, docNFO, eRoot);
            }
        }

        DOMHelper.writeDocumentToFile(docNFO, tempNfoFile.getAbsolutePath());

    }

    /**
     * Create an episode detail node for the NFO file This may actually create more than one node dependent on the
     * number of parts in the file
     *
     * @param episode
     * @param docNFO
     * @param eRoot
     */
    private static void createEpisodeDetails(MovieFile episode, Document docNFO, Element eRoot) {

        for (int part = episode.getFirstPart(); part <= episode.getLastPart(); part++) {
            Element eEpDetails = docNFO.createElement(MovieNFOReader.TYPE_EPISODE);

            appendValid(docNFO, eEpDetails, "title", episode.getTitle(part));
            appendValid(docNFO, eEpDetails, "rating", episode.getRating(part));
            appendValid(docNFO, eEpDetails, "season", String.valueOf(episode.getSeason()));
            appendValid(docNFO, eEpDetails, "episode", String.valueOf(part));
            appendValid(docNFO, eEpDetails, "plot", episode.getPlot(part));
            appendValid(docNFO, eEpDetails, "aired", episode.getFirstAired(part));

            appendValid(docNFO, eEpDetails, "airsAfterSeason", episode.getAirsAfterSeason(part));
            appendValid(docNFO, eEpDetails, "airsBeforeEpisode", episode.getAirsBeforeEpisode(part));
            appendValid(docNFO, eEpDetails, "airsBeforeSeason", episode.getAirsBeforeSeason(part));

            eRoot.appendChild(eEpDetails);
        }
    }

    /**
     * Only add the node if the value is valid
     *
     * @param docNFO
     * @param eElement
     * @param key
     * @param value
     */
    private static void appendValid(Document docNFO, Element eElement, String key, String value) {
        if (StringTools.isValidString(value)) {
            DOMHelper.appendChild(docNFO, eElement, key, value);
        }
    }
}
