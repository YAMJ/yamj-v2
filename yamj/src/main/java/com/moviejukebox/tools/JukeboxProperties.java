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
package com.moviejukebox.tools;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Save a pre-defined list of attributes of the jukebox and properties for use in subsequent processing runs to
 * determine if an attribute has changed and force a rescan of the appropriate data
 *
 * @author stuart.boston
 *
 */
public class JukeboxProperties {

    private static final Logger logger = Logger.getLogger(JukeboxProperties.class);
    private static final String logMessage = "JukeboxProperties: ";
    private static final Collection<PropertyInformation> propInfo = new ArrayList<PropertyInformation>();
    private static final String JUKEBOX = "jukebox";
    private static final String SKIN = "skin";
    private static final String PROPERTIES = "properties";
    private static final String CATEGORY = "Category";
    private static final String GENRE = "Genre";
    private static final String CERTIFICATION = "Certification";
//    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-kk:mm:ss");
    private static final int MINIMUM_REVISION = 3061;

    static {
        propInfo.add(new PropertyInformation("userPropertiesName", EnumSet.noneOf(PropertyOverwrites.class)));
        propInfo.add(new PropertyInformation("mjb.skin.dir", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails, PropertyOverwrites.Posters, PropertyOverwrites.Index)));

        propInfo.add(new PropertyInformation("mjb.includeEpisodePlots", EnumSet.of(PropertyOverwrites.XML)));
        propInfo.add(new PropertyInformation("mjb.includeEpisodeRating", EnumSet.of(PropertyOverwrites.XML)));
        propInfo.add(new PropertyInformation("filename.scanner.skip.episodeTitle", EnumSet.of(PropertyOverwrites.XML, PropertyOverwrites.HTML)));

        propInfo.add(new PropertyInformation("mjb.categories.minCount", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Other", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Genres", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Title", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Certification", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Year", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Library", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Set", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Cast", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Director", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Writer", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.categories.minCount.Country", EnumSet.of(PropertyOverwrites.Index)));

        propInfo.add(new PropertyInformation("trailers.rescan.days", EnumSet.of(PropertyOverwrites.Trailers)));

        // Posters
        propInfo.add(new PropertyInformation("posters.width", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Posters)));
        propInfo.add(new PropertyInformation("posters.height", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Posters)));
        propInfo.add(new PropertyInformation("posters.logoHD", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Posters)));
        propInfo.add(new PropertyInformation("posters.logoTV", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Posters)));
        propInfo.add(new PropertyInformation("posters.language", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Posters)));

        // Thumbnails
        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerPage", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails, PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerLine", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails, PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerPage", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails, PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerLine", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails, PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("thumbnails.width", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.height", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoHD", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoTV", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.logoSet", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));
        propInfo.add(new PropertyInformation("thumbnails.language", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Thumbnails)));

        // Banners
        propInfo.add(new PropertyInformation("mjb.includeWideBanners", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Banners)));
        propInfo.add(new PropertyInformation("banners.width", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Banners)));
        propInfo.add(new PropertyInformation("banners.height", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Banners)));

        // Fanart
        propInfo.add(new PropertyInformation("fanart.movie.download", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Fanart)));
        propInfo.add(new PropertyInformation("fanart.tv.download", EnumSet.of(PropertyOverwrites.HTML, PropertyOverwrites.Fanart)));

        // VideoImages
        propInfo.add(new PropertyInformation("mjb.includeVideoImages", EnumSet.of(PropertyOverwrites.XML, PropertyOverwrites.VideoImages)));
        propInfo.add(new PropertyInformation("videoimages.width", EnumSet.of(PropertyOverwrites.XML, PropertyOverwrites.VideoImages)));
        propInfo.add(new PropertyInformation("videoimages.height", EnumSet.of(PropertyOverwrites.XML, PropertyOverwrites.VideoImages)));

        // Clearart
        propInfo.add(new PropertyInformation("clearart.tv.download", EnumSet.of(PropertyOverwrites.Clearart)));
        propInfo.add(new PropertyInformation("clearart.width", EnumSet.of(PropertyOverwrites.Clearart)));
        propInfo.add(new PropertyInformation("clearart.height", EnumSet.of(PropertyOverwrites.Clearart)));

        // Clearlogo
        propInfo.add(new PropertyInformation("clearlogo.tv.download", EnumSet.of(PropertyOverwrites.Clearlogo)));
        propInfo.add(new PropertyInformation("clearlogo.width", EnumSet.of(PropertyOverwrites.Clearlogo)));
        propInfo.add(new PropertyInformation("clearlogo.height", EnumSet.of(PropertyOverwrites.Clearlogo)));

        // TvThumb
        propInfo.add(new PropertyInformation("tvthumb.tv.download", EnumSet.of(PropertyOverwrites.Tvthumb)));
        propInfo.add(new PropertyInformation("tvthumb.width", EnumSet.of(PropertyOverwrites.Tvthumb)));
        propInfo.add(new PropertyInformation("tvthumb.height", EnumSet.of(PropertyOverwrites.Tvthumb)));

        // SeasonThumb
        propInfo.add(new PropertyInformation("seasonthumb.tv.download", EnumSet.of(PropertyOverwrites.Seasonthumb)));
        propInfo.add(new PropertyInformation("seasonthumb.width", EnumSet.of(PropertyOverwrites.Seasonthumb)));
        propInfo.add(new PropertyInformation("seasonthumb.height", EnumSet.of(PropertyOverwrites.Seasonthumb)));

        // MovieArt
        propInfo.add(new PropertyInformation("movieart.movie.download", EnumSet.of(PropertyOverwrites.Movieart)));
        propInfo.add(new PropertyInformation("movieart.width", EnumSet.of(PropertyOverwrites.Movieart)));
        propInfo.add(new PropertyInformation("movieart.height", EnumSet.of(PropertyOverwrites.Movieart)));

        // MovieDisc
        propInfo.add(new PropertyInformation("moviedisc.movie.download", EnumSet.of(PropertyOverwrites.Moviedisc)));
        propInfo.add(new PropertyInformation("moviedisc.width", EnumSet.of(PropertyOverwrites.Moviedisc)));
        propInfo.add(new PropertyInformation("moviedisc.height", EnumSet.of(PropertyOverwrites.Moviedisc)));

        // MovieLogo
        propInfo.add(new PropertyInformation("movielogo.movie.download", EnumSet.of(PropertyOverwrites.Movielogo)));
        propInfo.add(new PropertyInformation("movielogo.width", EnumSet.of(PropertyOverwrites.Movielogo)));
        propInfo.add(new PropertyInformation("movielogo.height", EnumSet.of(PropertyOverwrites.Movielogo)));

        // Library sorting
        propInfo.add(new PropertyInformation("indexing.sort.3d", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.3d.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.all", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.all.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.award", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.award.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.cast", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.cast.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.certification", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.certification.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.country", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.country.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.director", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.director.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.genres", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.genres.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-1080", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-1080.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-720", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd-720.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.hd.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.library", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.library.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.movies", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.movies.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-movie", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-movie.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-tv", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new-tv.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.new.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.person", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.person.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.rating", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.rating.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.ratings", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.ratings.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.title", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.title.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.top250", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.top250.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.tvshows", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.tvshows.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.unwatched", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.unwatched.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.watched", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.watched.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.writer", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.writer.asc", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.year", EnumSet.of(PropertyOverwrites.Index)));
        propInfo.add(new PropertyInformation("indexing.sort.year.asc", EnumSet.of(PropertyOverwrites.Index)));
    }

    /**
     * Check to see if the file needs to be processed (if it exists) or just created Note: This *MIGHT* cause issues
     * with some programs that assume all XML files in the jukebox folder are videos or indexes. However, they should
     * just deal with this themselves :-)
     *
     * @param jukebox
     * @param mediaLibraryPaths
     */
    public static void readDetailsFile(Jukebox jukebox, Collection<MediaLibraryPath> mediaLibraryPaths) {
        boolean monitor = PropertiesUtil.getBooleanProperty("mjb.monitorJukeboxProperties", FALSE);

        // Read the mjbDetails file that stores the jukebox properties we want to watch
        File mjbDetails = new File(jukebox.getJukeboxRootLocationDetailsFile(), "jukebox_details.xml");
        FileTools.addJukeboxFile(mjbDetails.getName());
        try {
            // If we are monitoring the file and it exists, then read and check, otherwise create the file
            if (monitor && mjbDetails.exists()) {
                PropertyInformation pi = processFile(mjbDetails, mediaLibraryPaths);

                if (pi.getOverwrites().size() > 0) {
                    logger.debug(logMessage + "Found " + pi.getOverwrites().size() + " overwites to set.");
                    for (PropertyOverwrites po : pi.getOverwrites()) {
                        logger.debug("Setting 'force" + po.toString() + "Overwrite = true' due to property file changes");
                        PropertiesUtil.setProperty("mjb.force" + po.toString() + "Overwrite", TRUE);
                    }
                } else {
                    logger.debug(logMessage + "Properties haven't changed, no updates necessary");
                }
            }
        } catch (Exception error) {
            logger.error("Failed creating " + mjbDetails.getName() + " file!");
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
            logger.debug("Creating JukeboxProperties file: " + mjbDetails.getAbsolutePath());
            if (mjbDetails.exists() && !mjbDetails.delete()) {
                logger.error(logMessage + "Failed to delete " + mjbDetails.getName() + ". Please make sure it's not read only");
                return;
            }
        } catch (Exception error) {
            logger.error(logMessage + "Failed to create/delete " + mjbDetails.getName() + ". Please make sure it's not read only");
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
            logger.error(logMessage + "Error creating " + mjbDetails.getName() + " file");
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

        int stat = library.getMovieCountForIndex(Library.INDEX_OTHER, Library.INDEX_ALL);
        DOMHelper.appendChild(doc, eStats, "Videos", String.valueOf(stat));

        stat = library.getMovieCountForIndex(Library.INDEX_OTHER, Library.INDEX_MOVIES);
        if (stat > 0) {
            DOMHelper.appendChild(doc, eStats, "Movies", String.valueOf(stat));
        }

        stat = library.getMovieCountForIndex(Library.INDEX_OTHER, Library.INDEX_TVSHOWS);
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
        boolean revisionCheckPassed = Boolean.TRUE;

        // Try to open and read the document file
        try {
            docMjbDetails = DOMHelper.getDocFromFile(mjbDetails);
        } catch (Exception error) {
            logger.warn(logMessage + "Failed creating the file, no checks performed");
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
                piReturn.mergePropertyInformation(new PropertyInformation("LibraryPath", EnumSet.of(PropertyOverwrites.Index)));
            }

            // Check the Categories file
            if (!validXmlFileDetails("mjb.xmlCategoryFile", CATEGORY, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation(CATEGORY, EnumSet.of(PropertyOverwrites.Index)));
                logger.debug(logMessage + "Categories has changed, so need to update");
            }

            // Check the Genres file
            if (!validXmlFileDetails("mjb.xmlGenreFile", GENRE, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation(GENRE, EnumSet.of(PropertyOverwrites.Index)));
                logger.debug(logMessage + "Genres has changed, so need to update");
            }

            // Check the Certifications file
            if (!validXmlFileDetails("mjb.xmlCertificationFile", CERTIFICATION, eJukebox)) {
                // Details are wrong, so overwrite
                piReturn.mergePropertyInformation(new PropertyInformation("Certifications", EnumSet.of(PropertyOverwrites.Index)));
                logger.debug(logMessage + "Certifications has changed, so need to update");
            }

            // Check the revision of YAMJ
            String mjbRevision = DOMHelper.getValueFromElement(eJukebox, "JukeboxRevision");
            if (StringUtils.isNumeric(mjbRevision) && (Integer.parseInt(mjbRevision) < MINIMUM_REVISION)) {
                revisionCheckPassed = Boolean.FALSE;
            }
        }

        /*
         * This is a temporary check for the changes to the ArtworkScanner.
         *
         * Basically if the user came from a revision <r3061 we will skip the
         * property checks. See
         * http://www.networkedmediatank.com/showthread.php?tid=60652&pid=555361
         *
         * // TODO: REMOVE this after v2.7 is released
         */
        if (!revisionCheckPassed) {
            // Stop and return what we have so far.
            return piReturn;
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

        logger.debug(logMessage + "Returning: " + piReturn.toString());
        return piReturn;
    }

    /**
     * Compare the current XML file details with the stored ones Any errors with this check will return TRUE to ensure
     * no properties are overwritten
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
                return false;
            }

            // Check to see if the file dates have changed.
            String tempFileDate = getFileDate(jukeboxFilename);
            if (!tempFileDate.equals(jukeboxDate)) {
                return false;
            }
        } catch (Exception ignore) {
            logger.warn(logMessage + "Error validating " + jukeboxPropertyCategory);
            return true;
        }

        // All the tests pass, so these are the same
        return true;
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
     * Enumeration of the Overwrite properties
     *
     * These are case sensitive and should be exactly as needed when setting the force???Overwrite property
     */
    public static enum PropertyOverwrites {

        XML, Thumbnails, Fanart, VideoImages, Trailers, HTML, Posters, Banners,
        Index, Clearart, Clearlogo, Tvthumb, Seasonthumb, Movielogo, Movieart,
        Moviedisc, Footer, Skin;
    }

    /**
     * Class to define the property name and the impact on each of the overwrite flags. If the
     *
     * @author stuart.boston
     *
     */
    public static class PropertyInformation {

        private String propertyName = Movie.UNKNOWN;
        private EnumSet<PropertyOverwrites> propertyOverwrites = EnumSet.noneOf(PropertyOverwrites.class);

        public PropertyInformation(String property, Set<PropertyOverwrites> propOverwrites) {
            this.propertyName = property;
            this.propertyOverwrites.addAll(propOverwrites);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public EnumSet<PropertyOverwrites> getOverwrites() {
            return propertyOverwrites;
        }

        public boolean isOverwrite(PropertyOverwrites overwrite) {
            if (propertyOverwrites.contains(overwrite)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }

        /**
         * Merge two PropertyInformation objects. Sets the overwrite flags to true.
         *
         * @param newPI
         */
        public void mergePropertyInformation(PropertyInformation newPI) {
            this.propertyOverwrites.addAll(newPI.getOverwrites());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(getPropertyName());
            sb.append(" Overwrites: ").append(getOverwrites().toString());
            return sb.toString();
        }
    }
}
