/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
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
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.tools;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.JukeboxStatistic;
import com.moviejukebox.model.JukeboxStatistics;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.PropertyInformation;
import com.moviejukebox.model.PropertyOverwrites;
import static com.moviejukebox.model.PropertyOverwrites.*;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Save a pre-defined list of attributes of the jukebox and properties for use in subsequent processing runs to determine if an
 * attribute has changed and force a rescan of the appropriate data
 *
 * @author stuart.boston
 *
 */
public class JukeboxProperties {

    // Logger
    private static final Logger logger = Logger.getLogger(JukeboxProperties.class);
    private static final String LOG_MESSAGE = "JukeboxProperties: ";
    // Filename
    private static final String XML_FILENAME = "jukebox_details.xml";
    // Properties
    private static final boolean MONITOR = PropertiesUtil.getBooleanProperty("mjb.monitorJukeboxProperties", Boolean.FALSE);
    private static final Collection<PropertyInformation> propInfo = new ArrayList<PropertyInformation>();
    private static boolean scanningLimitReached = Boolean.FALSE;   // Were videos skipped during processing?
    // Literals
    private static final String JUKEBOX = "jukebox";
    private static final String SKIN = "skin";
    private static final String PROPERTIES = "properties";
    private static final String CATEGORY = "Category";
    private static final String GENRE = "Genre";
    private static final String CERTIFICATION = "Certification";

    static {
        FileTools.addJukeboxFile(XML_FILENAME);

        propInfo.add(new PropertyInformation("userPropertiesName", EnumSet.noneOf(PropertyOverwrites.class)));
        propInfo.add(new PropertyInformation("mjb.skin.dir", EnumSet.of(HTML, Thumbnails, Posters, Index, Skin)));

        propInfo.add(new PropertyInformation("mjb.includeEpisodePlots", EnumSet.of(XML)));
        propInfo.add(new PropertyInformation("mjb.includeEpisodeRating", EnumSet.of(XML)));
        propInfo.add(new PropertyInformation("filename.scanner.skip.episodeTitle", EnumSet.of(XML, HTML)));

        propInfo.add(new PropertyInformation("mjb.categories.minCount", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Other", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Genres", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Title", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Certification", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Year", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Library", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Set", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Cast", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Director", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Writer", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Country", EnumSet.of(Index)));

        propInfo.add(new PropertyInformation("trailers.rescan.days", EnumSet.of(Trailers)));

        // Posters
        propInfo.add(new PropertyInformation("posters.width", EnumSet.of(HTML, Posters)));
        propInfo.add(new PropertyInformation("posters.height", EnumSet.of(HTML, Posters)));
        propInfo.add(new PropertyInformation("posters.logoHD", EnumSet.of(HTML, Posters)));
        propInfo.add(new PropertyInformation("posters.logoTV", EnumSet.of(HTML, Posters)));
        propInfo.add(new PropertyInformation("posters.language", EnumSet.of(HTML, Posters)));

        // Thumbnails
        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerPage", EnumSet.of(HTML, Thumbnails, Index)));
        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerLine", EnumSet.of(HTML, Thumbnails, Index)));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerPage", EnumSet.of(HTML, Thumbnails, Index)));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerLine", EnumSet.of(HTML, Thumbnails, Index)));
        propInfo.add(new PropertyInformation("thumbnails.width", EnumSet.of(HTML, Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.height", EnumSet.of(HTML, Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoHD", EnumSet.of(HTML, Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoTV", EnumSet.of(HTML, Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoSet", EnumSet.of(HTML, Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.language", EnumSet.of(HTML, Thumbnails)));

        // Banners
        propInfo.add(new PropertyInformation("mjb.includeWideBanners", EnumSet.of(HTML, Banners)));
        propInfo.add(new PropertyInformation("banners.width", EnumSet.of(HTML, Banners)));
        propInfo.add(new PropertyInformation("banners.height", EnumSet.of(HTML, Banners)));

        // Fanart
        propInfo.add(new PropertyInformation("fanart.movie.download", EnumSet.of(HTML, Fanart)));
        propInfo.add(new PropertyInformation("fanart.tv.download", EnumSet.of(HTML, Fanart)));

        // VideoImages
        propInfo.add(new PropertyInformation("mjb.includeVideoImages", EnumSet.of(XML, VideoImages)));
        propInfo.add(new PropertyInformation("videoimages.width", EnumSet.of(XML, VideoImages)));
        propInfo.add(new PropertyInformation("videoimages.height", EnumSet.of(XML, VideoImages)));

        // Clearart
        propInfo.add(new PropertyInformation("clearart.tv.download", EnumSet.of(Clearart)));
        propInfo.add(new PropertyInformation("clearart.width", EnumSet.of(Clearart)));
        propInfo.add(new PropertyInformation("clearart.height", EnumSet.of(Clearart)));

        // Clearlogo
        propInfo.add(new PropertyInformation("clearlogo.tv.download", EnumSet.of(Clearlogo)));
        propInfo.add(new PropertyInformation("clearlogo.width", EnumSet.of(Clearlogo)));
        propInfo.add(new PropertyInformation("clearlogo.height", EnumSet.of(Clearlogo)));

        // TvThumb
        propInfo.add(new PropertyInformation("tvthumb.tv.download", EnumSet.of(Tvthumb)));
        propInfo.add(new PropertyInformation("tvthumb.width", EnumSet.of(Tvthumb)));
        propInfo.add(new PropertyInformation("tvthumb.height", EnumSet.of(Tvthumb)));

        // SeasonThumb
        propInfo.add(new PropertyInformation("seasonthumb.tv.download", EnumSet.of(Seasonthumb)));
        propInfo.add(new PropertyInformation("seasonthumb.width", EnumSet.of(Seasonthumb)));
        propInfo.add(new PropertyInformation("seasonthumb.height", EnumSet.of(Seasonthumb)));

        // MovieArt
        propInfo.add(new PropertyInformation("movieart.movie.download", EnumSet.of(Movieart)));
        propInfo.add(new PropertyInformation("movieart.width", EnumSet.of(Movieart)));
        propInfo.add(new PropertyInformation("movieart.height", EnumSet.of(Movieart)));

        // MovieDisc
        propInfo.add(new PropertyInformation("moviedisc.movie.download", EnumSet.of(Moviedisc)));
        propInfo.add(new PropertyInformation("moviedisc.width", EnumSet.of(Moviedisc)));
        propInfo.add(new PropertyInformation("moviedisc.height", EnumSet.of(Moviedisc)));

        // MovieLogo
        propInfo.add(new PropertyInformation("movielogo.movie.download", EnumSet.of(Movielogo)));
        propInfo.add(new PropertyInformation("movielogo.width", EnumSet.of(Movielogo)));
        propInfo.add(new PropertyInformation("movielogo.height", EnumSet.of(Movielogo)));

        // Library sorting
        propInfo.add(new PropertyInformation("indexing.sort.3d", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.3d.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.all", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.all.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.award", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.award.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.cast", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.cast.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.certification", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.certification.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.country", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.country.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.director", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.director.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.genres", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.genres.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-1080", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-1080.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-720", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-720.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.library", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.library.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.movies", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.movies.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-movie", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-movie.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-tv", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-tv.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.person", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.person.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.rating", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.rating.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.ratings", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.ratings.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.title", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.title.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.top250", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.top250.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.tvshows", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.tvshows.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.unwatched", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.unwatched.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.watched", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.watched.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.writer", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.writer.asc", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.year", EnumSet.of(Index)));
        propInfo.add(new PropertyInformation("indexing.sort.year.asc", EnumSet.of(Index)));
    }

    /**
     * Check to see if the file needs to be processed (if it exists) or just created Note: This *MIGHT* cause issues with some
     * programs that assume all XML files in the jukebox folder are videos or indexes. However, they should just deal with this
     * themselves :-)
     *
     * @param jukebox
     * @param mediaLibraryPaths
     */
    public static void readDetailsFile(Jukebox jukebox, Collection<MediaLibraryPath> mediaLibraryPaths) {
        // Read the mjbDetails file that stores the jukebox properties we want to watch
        File mjbDetails = new File(jukebox.getJukeboxRootLocationDetailsFile(), XML_FILENAME);
        FileTools.addJukeboxFile(mjbDetails.getName());
        try {
            // If we are monitoring the file and it exists, then read and check, otherwise create the file
            if (MONITOR && mjbDetails.exists()) {
                PropertyInformation pi = processFile(mjbDetails, mediaLibraryPaths);

                if (pi.getOverwrites().size() > 0) {
                    logger.debug(LOG_MESSAGE + "Found " + pi.getOverwrites().size() + " overwites to set.");
                    for (PropertyOverwrites po : pi.getOverwrites()) {
                        logger.debug(LOG_MESSAGE + "Setting 'force" + po.toString() + "Overwrite = true' due to property file changes");
                        PropertiesUtil.setProperty("mjb.force" + po.toString() + "Overwrite", Boolean.TRUE);
                    }
                } else {
                    logger.debug(LOG_MESSAGE + "Properties haven't changed, no updates necessary");
                }
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed creating " + mjbDetails.getName() + " file!");
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Create the mjbDetails file and populate with the attributes
     *
     * @param mjbDetails
     * @param jukebox
     */
    public static void writeFile(Jukebox jukebox, Library library, Collection<MediaLibraryPath> mediaLibraryPaths) {
        File mjbDetails = new File(jukebox.getJukeboxRootLocationDetailsFile(), "jukebox_details.xml");
        FileTools.addJukeboxFile(mjbDetails.getName());

        Document docMjbDetails;
        Element eRoot, eJukebox, eProperties;

        try {
            logger.debug(LOG_MESSAGE + "Creating JukeboxProperties file: " + mjbDetails.getAbsolutePath());
            if (mjbDetails.exists() && !mjbDetails.delete()) {
                logger.error(LOG_MESSAGE + "Failed to delete " + mjbDetails.getName() + ". Please make sure it's not read only");
                return;
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed to create/delete " + mjbDetails.getName() + ". Please make sure it's not read only");
            return;
        }

        try {
            DateFormat df = DateTimeTools.getDateFormatLong();
            // Start with a blank document
            docMjbDetails = DOMHelper.createDocument();
            docMjbDetails.appendChild(docMjbDetails.createComment("This file was created on: " + df.format(System.currentTimeMillis())));

            //create the root element and add it to the document
            eRoot = docMjbDetails.createElement("root");
            docMjbDetails.appendChild(eRoot);

            //create child element, add an attribute, and add to root
            eJukebox = docMjbDetails.createElement(JUKEBOX);
            eRoot.appendChild(eJukebox);

            DOMHelper.appendChild(docMjbDetails, eJukebox, "JukeboxVersion", SystemTools.getVersion());

            DOMHelper.appendChild(docMjbDetails, eJukebox, "JukeboxRevision", SystemTools.getRevision());

            // Save the run date
            DOMHelper.appendChild(docMjbDetails, eJukebox, "RunTime", df.format(System.currentTimeMillis()));

            // Save the details directory name
            DOMHelper.appendChild(docMjbDetails, eJukebox, "DetailsDirName", jukebox.getDetailsDirName());

            // Save the jukebox location
            DOMHelper.appendChild(docMjbDetails, eJukebox, "JukeboxLocation", jukebox.getJukeboxRootLocation());

            // Save the root index filename
            DOMHelper.appendChild(docMjbDetails, eJukebox, "indexFile", getProperty("mjb.indexFile", "index.htm"));

            // Save the library paths. This isn't very accurate, any change to this file will cause the jukebox to be rebuilt
            DOMHelper.appendChild(docMjbDetails, eJukebox, "LibraryPath", mediaLibraryPaths.toString());

            // Save the information about any videos that were skipped
            DOMHelper.appendChild(docMjbDetails, eJukebox, "ScanningLimitReached", Boolean.toString(scanningLimitReached));

            // Save the Categories file details
            writeGenericXmlFileDetails("mjb.xmlCategoryFile", CATEGORY, docMjbDetails, eJukebox);

            // Save the Genres file details
            writeGenericXmlFileDetails("mjb.xmlGenreFile", GENRE, docMjbDetails, eJukebox);

            // save the Certification file details
            writeGenericXmlFileDetails("mjb.xmlCertificationFile", CERTIFICATION, docMjbDetails, eJukebox);

            if (StringTools.isValidString(SkinProperties.getSkinName())) {
                Element eSkin = docMjbDetails.createElement(SKIN);
                eRoot.appendChild(eSkin);

                DOMHelper.appendChild(docMjbDetails, eSkin, "name", SkinProperties.getSkinName());

                if (StringTools.isValidString(SkinProperties.getSkinVersion())) {
                    DOMHelper.appendChild(docMjbDetails, eSkin, "version", SkinProperties.getSkinVersion());
                }

                if (StringTools.isValidString(SkinProperties.getSkinDate())) {
                    DOMHelper.appendChild(docMjbDetails, eSkin, "date", SkinProperties.getSkinDate());
                }

                if (SkinProperties.getFileDate() > 0) {
                    DOMHelper.appendChild(docMjbDetails, eSkin, "filedate", df.format(SkinProperties.getFileDate()));
                }
            }

            // Create the statistics node
            eRoot.appendChild(generateStatistics(docMjbDetails, library));

            // Create the properties node
            eProperties = docMjbDetails.createElement(PROPERTIES);
            eRoot.appendChild(eProperties);

            for (PropertyInformation pi : propInfo) {
                appendProperty(docMjbDetails, eProperties, pi.getPropertyName());
            }

            DOMHelper.writeDocumentToFile(docMjbDetails, mjbDetails.getAbsolutePath());
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Error creating " + mjbDetails.getName() + " file");
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Generate some statistics for the library
     *
     * @param doc
     * @param library
     * @return
     */
    private static Element generateStatistics(Document doc, Library library) {
        Element eStats = doc.createElement("statistics");

        int stat = JukeboxStatistics.getStatistic(JukeboxStatistic.VIDEOS);
        DOMHelper.appendChild(doc, eStats, "Videos", String.valueOf(stat));

        stat = JukeboxStatistics.getStatistic(JukeboxStatistic.MOVIES);
        if (stat > 0) {
            DOMHelper.appendChild(doc, eStats, "Movies", String.valueOf(stat));
        }

        stat = JukeboxStatistics.getStatistic(JukeboxStatistic.TVSHOWS);
        if (stat > 0) {
            DOMHelper.appendChild(doc, eStats, "TVShows", String.valueOf(stat));
        }

        return eStats;
    }

    /**
     * Generic routine to save details about the supplied XML file details
     *
     * @param xmlFileProperty
     * @param jukeboxPropertyCategory
     * @param docMjbDetails
     * @param eProperties
     */
    private static void writeGenericXmlFileDetails(String xmlFileProperty, String jukeboxPropertyCategory, Document docMjbDetails, Element eProperties) {
        // Save the file name
        String tempFilename = getProperty(xmlFileProperty, Movie.UNKNOWN);
        DOMHelper.appendChild(docMjbDetails, eProperties, jukeboxPropertyCategory + "Filename", tempFilename);

        // Save the file date
        DOMHelper.appendChild(docMjbDetails, eProperties, jukeboxPropertyCategory + "ModifiedDate", getFileDate(tempFilename));
    }

    /**
     * Determine the file date from the passed filename, if the filename is invalid return UNKNOWN
     *
     * @param tempFilename
     * @return
     */
    private static String getFileDate(String tempFilename) {
        if (StringTools.isValidString(tempFilename)) {
            try {
                File tempFile = new File(tempFilename);
                DateFormat df = DateTimeTools.getDateFormatLong();
                return df.format(tempFile.lastModified());
            } catch (Exception ignore) {
                return Movie.UNKNOWN;
            }
        } else {
            return Movie.UNKNOWN;
        }
    }

    /**
     * Read the attributes from the file and compare and set any force overwrites needed
     *
     * @param mjbDetails
     * @param mediaLibraryPaths
     * @return PropertyInformation Containing the merged overwrite values
     */
    public static PropertyInformation processFile(File mjbDetails, Collection<MediaLibraryPath> mediaLibraryPaths) {
        PropertyInformation piReturn = new PropertyInformation("RETURN", EnumSet.noneOf(PropertyOverwrites.class));
        Document docMjbDetails;

        // Try to open and read the document file
        try {
            docMjbDetails = DOMHelper.getDocFromFile(mjbDetails);
        } catch (Exception error) {
            logger.warn(LOG_MESSAGE + "Failed creating the file, no checks performed");
            logger.warn(SystemTools.getStackTrace(error));
            return piReturn;
        }

        NodeList nlElements;
        Node nDetails;

        nlElements = docMjbDetails.getElementsByTagName(JUKEBOX);
        nDetails = nlElements.item(0);

        if (nDetails.getNodeType() == Node.ELEMENT_NODE) {
            Element eJukebox = (Element) nDetails;
            // logger.fine("DetailsDirName : " + DOMHelper.getValueFromElement(eJukebox, "DetailsDirName"));
            // logger.fine("JukeboxLocation: " + DOMHelper.getValueFromElement(eJukebox, "JukeboxLocation"));

            // Check the library file
            String mlp = DOMHelper.getValueFromElement(eJukebox, "LibraryPath");
            if (!mediaLibraryPaths.toString().equalsIgnoreCase(mlp)) {
                // Overwrite the indexes only.
                piReturn.mergePropertyInformation(new PropertyInformation("LibraryPath", EnumSet.of(Index)));
            }

            // Check the Categories file
            if (!validXmlFileDetails("mjb.xmlCategoryFile", CATEGORY, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation(CATEGORY, EnumSet.of(Index)));
                logger.debug(LOG_MESSAGE + "Categories has changed, so need to update");
            }

            // Check the Genres file
            if (!validXmlFileDetails("mjb.xmlGenreFile", GENRE, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation(GENRE, EnumSet.of(Index)));
                logger.debug(LOG_MESSAGE + "Genres has changed, so need to update");
            }

            // Check the Certifications file
            if (!validXmlFileDetails("mjb.xmlCertificationFile", CERTIFICATION, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation("Certifications", EnumSet.of(Index)));
                logger.debug(LOG_MESSAGE + "Certifications has changed, so need to update");
            }
        }

        nlElements = docMjbDetails.getElementsByTagName(PROPERTIES);
        nDetails = nlElements.item(0);

        if (nDetails == null) {
            // Just return the property info file as is.
            return piReturn;
        }

        if (nDetails.getNodeType() == Node.ELEMENT_NODE) {
            Element eJukebox = (Element) nDetails;
            String propName, propValue, propCurrent;

            for (PropertyInformation pi : propInfo) {
                propName = pi.getPropertyName();
                propValue = DOMHelper.getValueFromElement(eJukebox, propName);
                propCurrent = PropertiesUtil.getProperty(propName, "");

                if (!propValue.equalsIgnoreCase(propCurrent)) {
                    // Update the return value with the information from this property
                    piReturn.mergePropertyInformation(pi);
                }
            }
        }

        logger.debug(LOG_MESSAGE + "Returning: " + piReturn.toString());
        return piReturn;
    }

    /**
     * Compare the current XML file details with the stored ones.
     *
     * Any errors with this check will return TRUE to ensure no properties are overwritten
     *
     * @param eJukebox
     * @return
     */
    private static boolean validXmlFileDetails(String xmlFileProperty, String jukeboxPropertyCategory, Element eProperties) {
        String jukeboxFilename = DOMHelper.getValueFromElement(eProperties, jukeboxPropertyCategory + "Filename");
        String jukeboxDate = DOMHelper.getValueFromElement(eProperties, jukeboxPropertyCategory + "ModifiedDate");

        try {
            // Check to see if the filenames are the same
            if (!jukeboxFilename.equals(getProperty(xmlFileProperty, Movie.UNKNOWN))) {
                // Filenames don't match
                return Boolean.FALSE;
            }

            // Check to see if the file dates have changed.
            String tempFileDate = getFileDate(jukeboxFilename);
            if (!tempFileDate.equals(jukeboxDate)) {
                return Boolean.FALSE;
            }
        } catch (Exception ex) {
            logger.warn(LOG_MESSAGE + "Error validating " + jukeboxPropertyCategory);
            return Boolean.TRUE;
        }

        // All the tests pass, so these are the same
        return Boolean.TRUE;
    }

    /**
     * Helper function to write out the property to the DOM document & Element
     *
     * @param doc
     * @param element
     * @param propertyName
     */
    private static void appendProperty(Document doc, Element element, String propertyName) {
        String propValue = PropertiesUtil.getProperty(propertyName);

        // Only write valid values
        if (StringTools.isValidString(propValue)) {
            DOMHelper.appendChild(doc, element, propertyName, propValue);
        }
    }

    /**
     * Is the jukebox to be monitored
     *
     * @return
     */
    public static boolean isMonitor() {
        return MONITOR;
    }

    /**
     * Were videos skipped during the processing
     *
     * @return
     */
    public static boolean isScanningLimitReached() {
        return scanningLimitReached;
    }

    /**
     * Set the skipped videos flag
     *
     * @param scanningLimitReached
     */
    public static void setScanningLimitReached(boolean scanningLimitReached) {
        JukeboxProperties.scanningLimitReached = scanningLimitReached;
    }
}
