/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.writer;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.CodecType;
import com.moviejukebox.reader.MovieNFOReader;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SubtitleTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author stuart.boston
 */
public final class MovieNFOWriter {

    private static final Logger LOG = LoggerFactory.getLogger(MovieNFOWriter.class);
    private static final boolean WRITE_SIMPLE_NFO = PropertiesUtil.getBooleanProperty("filename.nfo.writeSimpleFiles", Boolean.FALSE);
    private static final boolean GET_CERT_FROM_MPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", Boolean.TRUE);
    private static final boolean ENABLE_PEOPLE = PropertiesUtil.getBooleanProperty("mjb.people", Boolean.FALSE);
    private static final String CREDITS = "credits";
    private static final String MPAA = "mpaa";
    private static final String CERTIFICATION = "certification";
    private static final String GENRE = "genre";
    private static final String COMPANY = "company";
    private static final String COUNTRY = "country";
    private static final String ACTOR = "actor";
    private static final String DIRECTORS = "directors";
    private static final String NAME = "name";
    private static final String ROLE = "role";
    private static final String DIRECTOR = "director";
    private static final String ID = "id";
    private static final String MOVIEDB = "moviedb";
    private static final String TITLE = "title";
    private static final String ORIGINALTITLE = "originaltitle";
    private static final String SORTTITLE = "sorttitle";
    private static final String YEAR = "year";
    private static final String OUTLINE = "outline";
    private static final String PLOT = "plot";
    private static final String TAGLINE = "tagline";
    private static final String RUNTIME = "runtime";
    private static final String PREMIERED = "premiered";
    private static final String SHOW_STATUS = "showStatus";
    private static final String RATINGS = "ratings";
    private static final String RATING = "rating";
    private static final String SETS = "sets";
    private static final String SET = "set";
    private static final String ORDER = "order";
    private static final String SEASON = "season";
    private static final String EPISODE = "episode";
    private static final String AIRED = "aired";
    private static final String AIRS_AFTER_SEASON = "airsAfterSeason";
    private static final String AIRS_BEFORE_EPISODE = "airsBeforeEpisode";
    private static final String AIRS_BEFORE_SEASON = "airsBeforeSeason";
    private static final String LANGUAGE = "language";
    private static final String SUBTITLE = "subtitle";
    private static final String CODEC = "codec";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String ASPECT = "aspect";
    private static final String VIDEO = "video";
    private static final String BITRATE = "bitrate";
    private static final String AUDIO = "audio";
    private static final String STREAMDETAILS = "streamdetails";
    private static final String FILEINFO = "fileinfo";

    private MovieNFOWriter() {
        throw new RuntimeException("Class cannot be instantiated");
    }

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
        Element eRoot, eRatings;

        try {
            docNFO = DOMHelper.createDocument();
        } catch (ParserConfigurationException error) {
            LOG.warn("Failed to create NFO file for {}", movie.getBaseFilename());
            LOG.error(SystemTools.getStackTrace(error));
            return;
        }

        String nfoFolder = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), "NFO");
        FileTools.makeDirs(new File(nfoFolder));
        File tempNfoFile = new File(StringTools.appendToPath(nfoFolder, movie.getBaseName() + ".nfo"));

        LOG.debug("Writing {}NFO file for {}.nfo", (WRITE_SIMPLE_NFO ? "simple " : ""), movie.getBaseName());
        FileTools.addJukeboxFile(tempNfoFile.getName());

        // Define the root element
        if (movie.isTVShow()) {
            eRoot = docNFO.createElement(MovieNFOReader.TYPE_TVSHOW);
        } else {
            eRoot = docNFO.createElement(MovieNFOReader.TYPE_MOVIE);
        }
        docNFO.appendChild(eRoot);

        for (String site : movie.getIdMap().keySet()) {
            DOMHelper.appendChild(docNFO, eRoot, ID, movie.getId(site), MOVIEDB, site);
        }

        if (!WRITE_SIMPLE_NFO) {
            if (StringTools.isValidString(movie.getTitle())) {
                DOMHelper.appendChild(docNFO, eRoot, TITLE, movie.getTitle());
            }

            if (StringTools.isValidString(movie.getOriginalTitle())) {
                DOMHelper.appendChild(docNFO, eRoot, ORIGINALTITLE, movie.getOriginalTitle());
            }

            if (StringTools.isValidString(movie.getTitleSort())) {
                DOMHelper.appendChild(docNFO, eRoot, SORTTITLE, movie.getTitleSort());
            }

            if (StringTools.isValidString(movie.getYear())) {
                DOMHelper.appendChild(docNFO, eRoot, YEAR, movie.getYear());
            }

            if (StringTools.isValidString(movie.getOutline())) {
                DOMHelper.appendChild(docNFO, eRoot, OUTLINE, movie.getOutline());
            }

            if (StringTools.isValidString(movie.getPlot())) {
                DOMHelper.appendChild(docNFO, eRoot, PLOT, movie.getPlot());
            }

            if (StringTools.isValidString(movie.getTagline())) {
                DOMHelper.appendChild(docNFO, eRoot, TAGLINE, movie.getTagline());
            }

            if (StringTools.isValidString(movie.getRuntime())) {
                DOMHelper.appendChild(docNFO, eRoot, RUNTIME, movie.getRuntime());
            }

            if (StringTools.isValidString(movie.getReleaseDate())) {
                DOMHelper.appendChild(docNFO, eRoot, PREMIERED, movie.getReleaseDate());
            }

            if (StringTools.isValidString(movie.getShowStatus())) {
                DOMHelper.appendChild(docNFO, eRoot, SHOW_STATUS, movie.getReleaseDate());
            }

            if (movie.getTop250() > 0) {
                DOMHelper.appendChild(docNFO, eRoot, "top250", Integer.toString(movie.getTop250()));
            }

            if (movie.getRating() >= 0) {
                eRatings = docNFO.createElement(RATINGS);
                eRoot.appendChild(eRatings);

                for (String site : movie.getRatings().keySet()) {
                    DOMHelper.appendChild(docNFO, eRatings, RATING, String.valueOf(movie.getRating(site)), "moviedb", site);
                }
            }

            if (StringTools.isValidString(movie.getCertification())) {
                if (GET_CERT_FROM_MPAA) {
                    DOMHelper.appendChild(docNFO, eRoot, MPAA, movie.getCertification());
                } else {
                    DOMHelper.appendChild(docNFO, eRoot, CERTIFICATION, movie.getCertification());
                }
            }

            for (String genre : movie.getGenres()) {
                DOMHelper.appendChild(docNFO, eRoot, GENRE, genre);
            }

            if (StringTools.isValidString(movie.getCompany())) {
                DOMHelper.appendChild(docNFO, eRoot, COMPANY, movie.getCompany());
            }

            if (!movie.getCountries().isEmpty()) {
                DOMHelper.appendChild(docNFO, eRoot, COUNTRY, movie.getCountriesAsString());
            }

            /*
             * Process the people information from the video If we are using people scraping, use that information,
             * otherwise revert to the standard people
             */
            if (ENABLE_PEOPLE) {
                createPeople(docNFO, movie, eRoot);
            } else {
                createNonPeople(docNFO, movie, eRoot);
            }

            // Add the fileinfo format
            createFileInfo(docNFO, movie, eRoot);
        }   // End of detailed NFO

        // Generate movie sets
        createSets(docNFO, movie, eRoot);

        // Write out the episode details for any tv show files
        if (movie.isTVShow()) {
            for (MovieFile episodeFile : movie.getFiles()) {
                createEpisodeDetails(episodeFile, docNFO, eRoot);
            }
        }

        DOMHelper.writeDocumentToFile(docNFO, tempNfoFile.getAbsolutePath());
    }

    /**
     * Create the people nodes
     *
     * @param docNFO
     * @param movie
     * @param eRoot
     */
    private static void createPeople(Document docNFO, Movie movie, Element eRoot) {
        Element eActors = docNFO.createElement(ACTOR);
        Element eCredits = docNFO.createElement(CREDITS);
        Element eDirectors = docNFO.createElement(DIRECTORS);
        int countActors = 0;
        int countCredits = 0;
        int countDirectors = 0;

        for (Filmography person : movie.getPeople()) {
            if (person.getDepartment().equalsIgnoreCase(Filmography.DEPT_ACTORS)) {
                countActors++;
                DOMHelper.appendChild(docNFO, eActors, NAME, person.getName());
                DOMHelper.appendChild(docNFO, eActors, ROLE, person.getJob());
            } else if (person.getDepartment().equalsIgnoreCase(Filmography.DEPT_DIRECTING)) {
                countDirectors++;
                DOMHelper.appendChild(docNFO, eDirectors, DIRECTOR, person.getName());
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
    }

    /**
     * Create the people nodes if "People" is not enabled
     *
     * @param docNFO
     * @param movie
     * @param eRoot
     */
    private static void createNonPeople(Document docNFO, Movie movie, Element eRoot) {
        if (!movie.getCast().isEmpty()) {
            Element eActors = docNFO.createElement(ACTOR);
            for (String actor : movie.getCast()) {
                DOMHelper.appendChild(docNFO, eActors, NAME, actor);
                DOMHelper.appendChild(docNFO, eActors, ROLE, Filmography.DEPT_ACTORS);
            }
            eRoot.appendChild(eActors);
        }

        if (!movie.getWriters().isEmpty()) {
            // Make the NFO writers be XBMC compliant
            for (String writerCredit : movie.getWriters()) {
                DOMHelper.appendChild(docNFO, eRoot, CREDITS, writerCredit);
            }
        }

        if (!movie.getDirectors().isEmpty()) {
            Element eDirectors = docNFO.createElement(DIRECTORS);
            for (String director : movie.getDirectors()) {
                DOMHelper.appendChild(docNFO, eDirectors, DIRECTOR, director);
            }
            eRoot.appendChild(eDirectors);
        }
    }

    /**
     * Create the fileinfo format
     *
     * @param docNFO
     * @param movie
     * @param eRoot
     */
    private static void createFileInfo(Document docNFO, Movie movie, Element eRoot) {
        Element eFileinfo = docNFO.createElement(FILEINFO);
        Element eStreamDetails = docNFO.createElement(STREAMDETAILS);

        Element eCodec;
        for (Codec codec : movie.getCodecs()) {
            if (codec.getCodecType() == CodecType.AUDIO) {
                eCodec = docNFO.createElement(AUDIO);
                if (StringTools.isValidString(codec.getCodecLanguage())) {
                    DOMHelper.appendChild(docNFO, eCodec, LANGUAGE, codec.getCodecLanguage());
                }
                if (StringTools.isValidString(codec.getCodecBitRate())) {
                    DOMHelper.appendChild(docNFO, eCodec, BITRATE, codec.getCodecBitRate());
                }
            } else {
                eCodec = docNFO.createElement(VIDEO);
                DOMHelper.appendChild(docNFO, eCodec, ASPECT, movie.getAspectRatio());
                String movieResolution = movie.getResolution();
                if (StringTools.isValidString(movieResolution) && movieResolution.contains("x")) {
                    int locX = movieResolution.indexOf('x');
                    if (locX > 0) {
                        DOMHelper.appendChild(docNFO, eCodec, WIDTH, movieResolution.substring(0, locX));
                        DOMHelper.appendChild(docNFO, eCodec, HEIGHT, movieResolution.substring(locX + 1));
                    }
                }
            }
            DOMHelper.appendChild(docNFO, eCodec, CODEC, codec.getCodec());
            eStreamDetails.appendChild(eCodec);
        }

        for (String subtitle : SubtitleTools.getSubtitles(movie)) {
            Element eSubtitle = docNFO.createElement(SUBTITLE);
            DOMHelper.appendChild(docNFO, eSubtitle, LANGUAGE, subtitle);
            eStreamDetails.appendChild(eSubtitle);
        }

        eFileinfo.appendChild(eStreamDetails);
        eRoot.appendChild(eFileinfo);
    }

    /**
     * Generate movie sets
     *
     * @param docNFO
     * @param movie
     * @return
     */
    private static void createSets(Document docNFO, Movie movie, Element eRoot) {
        if (movie.getSets() != null && !movie.getSets().isEmpty()) {
            Element eSets = docNFO.createElement(SETS);
            for (Entry<String, Integer> entry : movie.getSets().entrySet()) {
                Integer order = entry.getValue();
                if (order == null) {
                    DOMHelper.appendChild(docNFO, eSets, SET, entry.getKey());
                } else {
                    DOMHelper.appendChild(docNFO, eSets, SET, entry.getKey(), ORDER, order.toString());
                }
            }
            eRoot.appendChild(eSets);
        }
    }

    /**
     * Create an episode detail node for the NFO file This may actually create
     * more than one node dependent on the number of parts in the file
     *
     * @param episode
     * @param docNFO
     * @param eRoot
     */
    private static void createEpisodeDetails(MovieFile episode, Document docNFO, Element eRoot) {

        for (int part = episode.getFirstPart(); part <= episode.getLastPart(); part++) {
            Element eEpDetails = docNFO.createElement(MovieNFOReader.TYPE_EPISODE);

            appendValid(docNFO, eEpDetails, TITLE, episode.getTitle(part));
            appendValid(docNFO, eEpDetails, RATING, episode.getRating(part));
            appendValid(docNFO, eEpDetails, SEASON, String.valueOf(episode.getSeason()));
            appendValid(docNFO, eEpDetails, EPISODE, String.valueOf(part));
            appendValid(docNFO, eEpDetails, PLOT, episode.getPlot(part));
            appendValid(docNFO, eEpDetails, AIRED, episode.getFirstAired(part));

            appendValid(docNFO, eEpDetails, AIRS_AFTER_SEASON, episode.getAirsAfterSeason(part));
            appendValid(docNFO, eEpDetails, AIRS_BEFORE_EPISODE, episode.getAirsBeforeEpisode(part));
            appendValid(docNFO, eEpDetails, AIRS_BEFORE_SEASON, episode.getAirsBeforeSeason(part));

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
