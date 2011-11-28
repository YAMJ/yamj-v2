/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import static com.moviejukebox.tools.XMLHelper.parseCData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Award;
import com.moviejukebox.model.AwardEvent;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Index;
import com.moviejukebox.model.IndexInfo;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.Codec;
import com.moviejukebox.model.Comparator.SortIgnorePrefixesComparator;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.AspectRatioTools;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import javax.swing.text.html.parser.TagElement;

/**
 * Parse/Write XML files for movie details and library indexes
 * 
 * @author Julien
 * @author Stuart.Boston
 */
public class MovieJukeboxXMLWriter {

    private boolean forceXMLOverwrite;
    private boolean forceIndexOverwrite;
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
    private static boolean isPlayOnHD;
    private static String defaultSource;
    private List<String> categoriesExplodeSet = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.explodeSet", "").split(","));
    private boolean removeExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet", "false");
    private boolean keepTVExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.keepTV", "true");
    private boolean beforeSortExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.beforeSort", "false");
    private static String strCategoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.displayList", "");
    private static List<String> categoriesDisplayList = Collections.emptyList();
    private static int categoryMinCountMaster = PropertiesUtil.getIntProperty("mjb.categories.minCount", "3");
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static boolean writeNfoFiles;
    private static boolean writeSimpleNfoFiles;
    private boolean extractCertificationFromMPAA;
    private boolean setsExcludeTV;
    private static String peopleFolder;
    private static boolean enableWatchScanner;
    // Should we scrape the award information
    private static boolean enableAwards = PropertiesUtil.getBooleanProperty("mjb.scrapeAwards", "false");
    // Should we scrape the business information
    private static boolean enableBusiness = PropertiesUtil.getBooleanProperty("mjb.scrapeBusiness", "false");
    // Should we scrape the trivia information
    private static boolean enableTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", "false");
    private static AspectRatioTools aspectTools = new AspectRatioTools();
    // Should we reindex the New / Watched / Unwatched categories?
    private boolean reindexNew = false;
    private boolean reindexWatched = false;
    private boolean reindexUnwatched = false;

    static {
        if (strCategoriesDisplayList.length() == 0) {
            strCategoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Certification,Year,Library,Set");
        }
        categoriesDisplayList = Arrays.asList(strCategoriesDisplayList.split(","));

        writeNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.writeFiles", "false");
        writeSimpleNfoFiles = PropertiesUtil.getBooleanProperty("filename.nfo.writeSimpleFiles", "false");

        enableWatchScanner = PropertiesUtil.getBooleanProperty("watched.scanner.enable", "true");
    }

    public MovieJukeboxXMLWriter() {
        forceXMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceXMLOverwrite", "false");
        forceIndexOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceIndexOverwrite", "false");
        nbMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbThumbnailsPerPage", "10");
        nbMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbThumbnailsPerLine", "5");
        nbTvShowsPerPage = PropertiesUtil.getIntProperty("mjb.nbTvThumbnailsPerPage", "0"); // If 0 then use the Movies setting
        nbTvShowsPerLine = PropertiesUtil.getIntProperty("mjb.nbTvThumbnailsPerLine", "0"); // If 0 then use the Movies setting
        nbSetMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbSetThumbnailsPerPage", "0"); // If 0 then use the Movies setting
        nbSetMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbSetThumbnailsPerLine", "0"); // If 0 then use the Movies setting
        nbTVSetMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbTVSetThumbnailsPerPage", "0"); // If 0 then use the TV SHOW setting
        nbTVSetMoviesPerLine = PropertiesUtil.getIntProperty("mjb.nbTVSetThumbnailsPerLine", "0"); // If 0 then use the TV SHOW setting
        fullMovieInfoInIndexes = PropertiesUtil.getBooleanProperty("mjb.fullMovieInfoInIndexes", "false");
        fullCategoriesInIndexes = PropertiesUtil.getBooleanProperty("mjb.fullCategoriesInIndexes", "true");
        includeMoviesInCategories = PropertiesUtil.getBooleanProperty("mjb.includeMoviesInCategories", "false");
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
        includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", "false");
        isPlayOnHD = PropertiesUtil.getBooleanProperty("mjb.PlayOnHD", "false");
        defaultSource = PropertiesUtil.getProperty("filename.scanner.source.default", Movie.UNKNOWN);
        extractCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", "true");
        setsExcludeTV = PropertiesUtil.getBooleanProperty("mjb.sets.excludeTV", "false");

        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        peopleFolder = PropertiesUtil.getProperty("mjb.people.folder", "");
        if (StringTools.isNotValidString(peopleFolder)) {
            peopleFolder = "";
        } else if (!peopleFolder.endsWith(File.separator)) {
            peopleFolder += File.separator;
        }

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
     * Parse a single movie detail XML file
     */
    @SuppressWarnings("unchecked")
    public boolean parseMovieXML(File xmlFile, Movie movie) {

        boolean forceDirtyFlag = false; // force dirty flag for example when extras has been deleted

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlFile), "UTF-8");
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                String tag = e.toString();

                if (tag.toLowerCase().startsWith("<id ")) {
                    String movieDatabase = ImdbPlugin.IMDB_PLUGIN_ID;
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("moviedb")) {
                            movieDatabase = attr.getValue();
                            continue;
                        }
                    }
                    movie.setId(movieDatabase, parseCData(r));
                }
                if (tag.equalsIgnoreCase("<mjbVersion>")) {
                    movie.setMjbVersion(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<mjbRevision>")) {
                    movie.setMjbRevision(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<xmlGenerationDate>")) {
                    movie.setMjbGenerationDateString(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<baseFilenameBase>") && (movie.getBaseFilename() == null || movie.getBaseFilename().equalsIgnoreCase(Movie.UNKNOWN))) {
                    movie.setBaseFilename(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<baseFilename>") && (movie.getBaseName() == null || movie.getBaseName().equalsIgnoreCase(Movie.UNKNOWN))) {
                    movie.setBaseName(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<title>")) {
                    movie.setTitle(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<titleSort>")) {
                    movie.setTitleSort(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<originalTitle>")) {
                    movie.setOriginalTitle(parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<year ") || tag.equalsIgnoreCase("<year>")) {
                    movie.setYear(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<releaseDate>")) {
                    movie.setReleaseDate(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<showStatus>")) {
                    movie.setShowStatus(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<rating>")) {
                    // We don't care about the main rating as this is derived
                    // movie.setRating(Integer.parseInt(parseCData(r)));
                }
                if (tag.toLowerCase().startsWith("<rating ")) {
                    String moviedb = "";

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("moviedb")) {
                            moviedb = attr.getValue();
                            continue;
                        }
                    }
                    movie.addRating(moviedb, Integer.parseInt(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<top250>")) {
                    movie.setTop250(Integer.parseInt(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<watched>")) {
                    movie.setWatchedFile(Boolean.parseBoolean(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<watchedNFO>")) {
                    movie.setWatchedNFO(Boolean.parseBoolean(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<posterURL>")) {
                    movie.setPosterURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<fanartURL>")) {
                    movie.setFanartURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<bannerURL>")) {
                    movie.setBannerURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<bannerFile>")) {
                    movie.setBannerFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<posterFile>")) {
                    movie.setPosterFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<detailPosterFile>")) {
                    movie.setDetailPosterFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<thumbnail>")) {
                    movie.setThumbnailFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<fanartFile>")) {
                    movie.setFanartFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<plot>")) {
                    movie.setPlot(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<outline>")) {
                    movie.setOutline(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<quote>")) {
                    movie.setQuote(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<tagline>")) {
                    movie.setTagline(parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<director ") || tag.equalsIgnoreCase("<director>")) {
                    movie.addDirector(parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<country ") || tag.equalsIgnoreCase("<country>")) {
                    movie.setCountry(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<company>")) {
                    movie.setCompany(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<runtime>")) {
                    movie.setRuntime(parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<genre ") || tag.equalsIgnoreCase("<genre>")) {
                    movie.addGenre(parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<set ") || tag.equalsIgnoreCase("<set>")) {
                    // String set = null;
                    Integer order = null;

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("order")) {
                            order = Integer.parseInt(attr.getValue());
                            continue;
                        }
                    }
                    movie.addSet(parseCData(r), order);
                }
                if (tag.toLowerCase().startsWith("<actor ") || tag.equalsIgnoreCase("<actor>")) {
                    String actor = parseCData(r);
                    movie.addActor(actor);
                }
                if (tag.equalsIgnoreCase("<writer>")) {
                    String writer = parseCData(r);
                    movie.addWriter(writer);
                }
                if (tag.equalsIgnoreCase("<certification>")) {
                    movie.setCertification(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<language>")) {
                    movie.setLanguage(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<subtitles>")) {
                    movie.setSubtitles(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<trailerExchange>")) {
                    movie.setTrailerExchange(parseCData(r).equalsIgnoreCase("YES"));
                }
                if (tag.equalsIgnoreCase("<trailerLastScan>")) {
                    movie.setTrailerLastScan(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<container>")) {
                    movie.setContainer(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<videoCodec>")) {
                    movie.setVideoCodec(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<audioCodec>")) {
                    movie.setAudioCodec(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<audioChannels>")) {
                    movie.setAudioChannels(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<resolution>")) {
                    movie.setResolution(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<videoSource>")) {
                    movie.setVideoSource(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<videoOutput>")) {
                    movie.setVideoOutput(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<aspect>")) {
                    movie.setAspectRatio(aspectTools.cleanAspectRatio(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<fps>")) {
                    movie.setFps(Float.parseFloat(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<first>")) {
                    movie.setFirst(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<previous>")) {
                    movie.setPrevious(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<next>")) {
                    movie.setNext(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<last>")) {
                    movie.setLast(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<libraryDescription>")) {
                    movie.setLibraryDescription(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<prebuf>")) {
                    String prebuf = parseCData(r);
                    if (prebuf != null) {
                        try {
                            movie.setPrebuf(Long.parseLong(prebuf));
                        } catch (Exception ignore) {
                            // If we can't convert the prebuf value, leave it as blank
                        }
                    }
                }

                // Issue 1901: Awards
                if (tag.toLowerCase().startsWith("<event ")) {
                    AwardEvent event = new AwardEvent();

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("name")) {
                            event.setName(attr.getValue());
                            continue;
                        }
                    }

                    while (!r.peek().toString().equalsIgnoreCase("</event>")) {
                        e = r.nextEvent();
                        tag = e.toString();
                        if (tag.toLowerCase().startsWith("<award ")) {
                            Award award = new Award();
                            StartElement element = e.asStartElement();
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("year")) {
                                    award.setYear(Integer.parseInt(attr.getValue()));
                                    continue;
                                }

                                if (ns.equalsIgnoreCase("won")) {
                                    award.setWon(Integer.parseInt(attr.getValue()));
                                    continue;
                                }

                                if (ns.equalsIgnoreCase("nominated")) {
                                    award.setNominated(Integer.parseInt(attr.getValue()));
                                    continue;
                                }

                                if (ns.equalsIgnoreCase("wons")) {
                                    award.setWons(Arrays.asList(attr.getValue().split(" / ")));
                                    continue;
                                }

                                if (ns.equalsIgnoreCase("nominations")) {
                                    award.setNominations(Arrays.asList(attr.getValue().split(" / ")));
                                    continue;
                                }

                            }
                            award.setName(parseCData(r));
                            event.addAward(award);
                        }
                    }
                    movie.addAward(event);
                }

                // Issue 1897: Cast enhancement
                if (tag.toLowerCase().startsWith("<person ")) {
                    Filmography person = new Person();

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("name")) {
                            person.setName(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("year")) {
                            person.setYear(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("doublage")) {
                            person.setDoublage(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("title")) {
                            person.setTitle(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("character")) {
                            person.setCharacter(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("job")) {
                            person.setJob(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("id")) {
                            person.setId(attr.getValue());
                            continue;
                        }

                        if (ns.toLowerCase().contains("id_")) {
                            person.setId(ns.substring(3), attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("department")) {
                            person.setDepartment(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("url")) {
                            person.setUrl(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("order")) {
                            person.setOrder(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("cast_id")) {
                            person.setCastId(attr.getValue());
                            continue;
                        }

                    }

                    person.setFilename(parseCData(r));
                    movie.addPerson(person);
                }

                // Issue 2012: Financial information about movie
                if (tag.toLowerCase().startsWith("<business ")) {
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("budget")) {
                            movie.setBudget(attr.getValue());
                            continue;
                        }
                    }
                }
                if (tag.toLowerCase().startsWith("<gross ")) {
                    StartElement start = e.asStartElement();
                    String country = Movie.UNKNOWN;
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("country")) {
                            country = attr.getValue();
                            continue;
                        }
                    }
                    movie.setGross(country, parseCData(r));
                }
                if (tag.toLowerCase().startsWith("<openweek ")) {
                    StartElement start = e.asStartElement();
                    String country = Movie.UNKNOWN;
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("country")) {
                            country = attr.getValue();
                            continue;
                        }
                    }
                    movie.setOpenWeek(country, parseCData(r));
                }
                
                if (tag.toLowerCase().startsWith("<file ")) {
                    MovieFile mf = new MovieFile();
                    mf.setNewFile(false);

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("title")) {
                            mf.setTitle(attr.getValue());
                            continue;
                        }

                        if (ns.equalsIgnoreCase("season")) {
                            mf.setSeason(Integer.parseInt(attr.getValue()));
                        }

                        if (ns.equalsIgnoreCase("firstPart")) {
                            mf.setFirstPart(Integer.parseInt(attr.getValue()));
                            continue;
                        }

                        if (ns.equalsIgnoreCase("lastPart")) {
                            mf.setLastPart(Integer.parseInt(attr.getValue()));
                            continue;
                        }

                        if (ns.equalsIgnoreCase("subtitlesExchange")) {
                            mf.setSubtitlesExchange(attr.getValue().equalsIgnoreCase("YES"));
                            continue;
                        }

                        if (ns.equalsIgnoreCase("watched")) {
                            mf.setWatched(Boolean.parseBoolean(attr.getValue()));
                            continue;
                        }
                    }

                    // Check to see if we got the season from the file, if not populate it from the movie.
                    // FIXME: This can be removed once all XML files have been overwritten
                    if (movie.isTVShow() && mf.getSeason() == -1) {
                        mf.setSeason(movie.getSeason());
                        mf.setNewFile(true); // Mark the moviefile as new to force it to be written out
                    }

                    while (!r.peek().toString().equalsIgnoreCase("</file>")) {
                        e = r.nextEvent();
                        tag = e.toString();
                        if (tag.equalsIgnoreCase("<fileLocation>")) {
                            try {
                                File mfFile = new File(parseCData(r));

                                // Check to see if the file exists, or we are preserving the jukebox
                                if (mfFile.exists() || MovieJukebox.isJukeboxPreserve()) {
                                    // Save the file to the MovieFile
                                    mf.setFile(mfFile);
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
                        } else if (tag.equalsIgnoreCase("<fileURL>")) {
                            mf.setFilename(parseCData(r));
                        } else if (tag.equalsIgnoreCase("<watchedDate>")) {
                            mf.setWatchedDateString(parseCData(r));
                        } else if (tag.toLowerCase().startsWith("<filetitle")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setTitle(part, parseCData(r));
                        } else if (tag.toLowerCase().startsWith("<fileplot")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setPlot(part, parseCData(r));
                        } else if (tag.toLowerCase().startsWith("<filerating")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setRating(part, parseCData(r));
                        } else if (tag.toLowerCase().startsWith("<fileimageurl")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setVideoImageURL(part, HTMLTools.decodeUrl(parseCData(r)));
                        } else if (tag.toLowerCase().startsWith("<fileimagefile")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setVideoImageFilename(part, HTMLTools.decodeUrl(parseCData(r)));
                        } else if (tag.toLowerCase().startsWith("<airsinfo ")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            String afterSeason = "0";
                            String beforeSeason = "0";
                            String beforeEpisode = "0";

                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }

                                if (ns.equalsIgnoreCase("afterSeason")) {
                                    afterSeason = attr.getValue();
                                }

                                if (ns.equalsIgnoreCase("beforeSeason")) {
                                    beforeSeason = attr.getValue();
                                }

                                if (ns.equalsIgnoreCase("beforeEpisode")) {
                                    beforeEpisode = attr.getValue();
                                }
                            }
                            // There is also the part number in the value, but we already have that
                            mf.setAirsAfterSeason(part, afterSeason);
                            mf.setAirsBeforeSeason(part, beforeSeason);
                            mf.setAirsBeforeEpisode(part, beforeEpisode);
                        } else if (tag.toLowerCase().startsWith("<firstaired ")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if (ns.equalsIgnoreCase("part")) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setFirstAired(part, HTMLTools.decodeUrl(parseCData(r)));
                        }
                    }
                    // add or replace MovieFile based on XML data
                    movie.addMovieFile(mf);
                }

                if (tag.toLowerCase().startsWith("<extra ")) {
                    String extraTitle = "";
                    String extraFilename = "";
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("title")) {
                            extraTitle = attr.getValue();
                            continue;
                        }
                    }

                    // get the filename
                    extraFilename = parseCData(r);

                    // Create a new ExtraFile and add to the movie if the file exists
                    if (!extraTitle.isEmpty() && !extraFilename.isEmpty()) {
                        boolean exist = false;
                        for (ExtraFile ef : movie.getExtraFiles()) {
                            // Check if the movie has already the extra file
                            if (ef.getFilename().equals(extraFilename)) {
                                exist = true;
                                // the extra file is old
                                ef.setNewFile(false);
                                break;
                            }
                        }
                        if (!exist) {
                            // the extra file has been delete so force the dirty flag
                            forceDirtyFlag = true;
                        }
                    }
                }
            }
        } catch (Exception error) {
            logger.error("Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
        }

        movie.setDirty(Movie.DIRTY_INFO, forceDirtyFlag || movie.hasNewMovieFiles() || movie.hasNewExtraFiles());

        return true;
    }

    public Map<String, Integer> parseMovieXMLForSets(File xmlFile) {
        Map<String, Integer> sets = new HashMap<String, Integer>();

        Document xmlDoc;
        try {
            xmlDoc = DOMHelper.getEventDocFromUrl(xmlFile);
        } catch (MalformedURLException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for set. Please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return sets;
        } catch (IOException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for set. Please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return sets;
        } catch (ParserConfigurationException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for set. Please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return sets;
        } catch (SAXException error) {
            logger.error("Failed parsing XML (" + xmlFile.getName() + ") for set. Please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return sets;
        }

        NodeList nlElements;
        Node nDetails;

        nlElements = xmlDoc.getElementsByTagName("set");

        for (int looper = 0; looper < nlElements.getLength(); looper++) {
            nDetails = nlElements.item(looper);
            if (nDetails.getNodeType() == Node.ELEMENT_NODE) {
                Element eSet = (Element) nDetails;

                String setOrder = eSet.getAttribute("order");
                if (StringTools.isValidString(setOrder)) {
                    try {
                        sets.put(eSet.getTextContent(), Integer.parseInt(setOrder));
                    } catch (NumberFormatException error) {
                        sets.put(eSet.getTextContent(), null);
                    }
                } else {
                    sets.put(eSet.getTextContent(), null);
                }
            }
        }

        return sets;
    }

    public boolean parseSetXML(File xmlSetFile, Movie setMaster, List<Movie> moviesList) {

        boolean forceDirtyFlag = false;

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
                            forceDirtyFlag |= (!movie.isTVShow() && !movie.getSetsKeys().contains(setMaster.getTitle())) || movie.isDirty(Movie.DIRTY_INFO);
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
            logger.error("Failed parsing " + xmlSetFile.getAbsolutePath() + " : please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
        }

        setMaster.setDirty(Movie.DIRTY_INFO, forceDirtyFlag);

        return true;
    }

    @SuppressWarnings("unchecked")
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
                        if (ns.toLowerCase().contains("id_")) {
                            person.setId(ns.substring(3), attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("name")) {
                            film.setName(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("title")) {
                            film.setTitle(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("originalTitle")) {
                            film.setOriginalTitle(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("year")) {
                            film.setYear(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("rating")) {
                            film.setRating(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("character")) {
                            film.setCharacter(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("job")) {
                            film.setJob(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("department")) {
                            film.setDepartment(attr.getValue());
                            continue;
                        }
                        if (ns.equalsIgnoreCase("url")) {
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
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return false;
        }

        person.setDirty(false);

        return true;
    }

    public void writeCategoryXML(Jukebox jukebox, Library library, String filename, boolean isDirty)
            throws FileNotFoundException, XMLStreamException, ParserConfigurationException {
        // Issue 1886: HTML indexes recreated every time
        File oldFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + filename + ".xml");

        if (oldFile.exists() && !isDirty) {
            // Even if the library is not dirty, these files need to be added to the safe jukebox list
            FileTools.addJukeboxFile(filename + ".xml");
            FileTools.addJukeboxFile(filename + ".html");
            return;
        }

        jukebox.getJukeboxTempLocationDetailsFile().mkdirs();

        File xmlFile = new File(jukebox.getJukeboxTempLocationDetailsFile(), filename + ".xml");
        FileTools.addJukeboxFile(filename + ".xml");

        Document xmlDoc = DOMHelper.createDocument();
        Element eLibrary = xmlDoc.createElement("library");
        int libraryCount = 0;

        if (includeMoviesInCategories) {
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

        // Issue 1148, generate category in the order specified in properties
        logger.info("  Indexing " + filename + "...");
        for (String categoryName : categoriesDisplayList) {
            int categoryMinCount = calcMinCategoryCount(categoryName);
            int categoryCount = 0;

            for (Entry<String, Index> category : library.getIndexes().entrySet()) {
                // Category not empty and match the current cat.
                if (!category.getValue().isEmpty() && categoryName.equalsIgnoreCase(category.getKey())
                        && (filename.equals("Categories") || filename.equals(category.getKey()))) {
                    Element eCategory = xmlDoc.createElement("category");
                    eCategory.setAttribute("name", category.getKey());

                    if ("other".equalsIgnoreCase(categoryName)) {
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

                        for (String catOriginalName : cm.keySet()) {
                            String catNewName = cm.get(catOriginalName);
                            if (category.getValue().containsKey(catNewName)) {
                                Element eCatIndex = processCategoryIndex(xmlDoc, catNewName, catOriginalName, category.getValue().get(catNewName),
                                        categoryName, categoryMinCount, library);
                                if (eCatIndex != null) {
                                    eCategory.appendChild(eCatIndex);
                                    categoryCount++;
                                }
                            }
                        }
                    } else {
                        // Process the remaining categories
                        TreeMap<String, List<Movie>> sortedMap = new TreeMap<String, List<Movie>>(new SortIgnorePrefixesComparator());
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
                        // Add the correct count to the index
                        eCategory.setAttribute("count", String.valueOf(categoryCount));

                        eLibrary.appendChild(eCategory);
                        libraryCount++;
                    }
                }
            }
        }

        // Add the movie count to the library
        eLibrary.setAttribute("count", String.valueOf(libraryCount));

        xmlDoc.appendChild(eLibrary);
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

        Element eCategory = doc.createElement("index");
        eCategory.setAttribute("name", indexName);
        eCategory.setAttribute("originalName", indexOriginalName);

        if (includeMoviesInCategories) {
            eCategory.setAttribute("filename", indexFilename);

            for (Identifiable movie : indexMovies) {
                DOMHelper.appendChild(doc, eCategory, "movie", String.valueOf(allMovies.indexOf(movie)));
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

        // Issue 1882: Separate index files for each category
        final boolean separateCategories = PropertiesUtil.getBooleanProperty("mjb.separateCategories", "false");
        final boolean setReindex = PropertiesUtil.getBooleanProperty("mjb.sets.reindex", "true");

        tasks.restart();

        for (final Map.Entry<String, Index> category : library.getIndexes().entrySet()) {
            final String categoryName = category.getKey();
            Map<String, List<Movie>> index = category.getValue();
            final int categoryMinCount = calcMinCategoryCount(categoryName);

            logger.info("  Indexing " + categoryName + " (" + (++indexCount) + "/" + indexSize + ") contains " + index.size() + " indexes");
            for (final Map.Entry<String, List<Movie>> group : index.entrySet()) {
                tasks.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws XMLStreamException, FileNotFoundException {
                        List<Movie> movies = group.getValue();
                        String key = FileTools.createCategoryKey(group.getKey());
                        String categoryPath = categoryName + " " + key;

                        // FIXME This is horrible! Issue 735 will get rid of it.
                        int categoryCount = library.getMovieCountForIndex(categoryName, key);
                        if (categoryCount < categoryMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryName)
                                && !Library.INDEX_SET.equalsIgnoreCase(categoryName) && !separateCategories) {
                            logger.debug("Category '" + categoryPath + "' does not contain enough videos (" + categoryCount + "/" + categoryMinCount
                                    + "), skipping XML generation.");
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
                                            skipIndex &= !m.isDirty(Movie.DIRTY_INFO);
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
                            if (movie.isDirty(Movie.DIRTY_INFO) || movie.isDirty(Movie.DIRTY_RECHECK)) {
                                skipIndex = false;
                            }

                            // Check for changes to the Watched, Unwatched and New categories whilst we are processing the All category
                            if (enableWatchScanner && key.equals(Library.getRenamedCategory(Library.INDEX_ALL))) {
                                if (movie.isWatched() && movie.isDirty(Movie.DIRTY_WATCHED)) {
                                    // Don't skip the index
                                    reindexWatched = true;
                                    reindexUnwatched = true;
                                    reindexNew = true;
                                }

                                if (!movie.isWatched() && movie.isDirty(Movie.DIRTY_WATCHED)) {
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

                        int current = 1;
                        int last = 1 + (tmpMovieList.size() - 1) / nbVideosPerPage;
                        int next = Math.min(2, last);
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

                        for (current = 1; current <= last; current++) {
                            if (Library.INDEX_SET.equalsIgnoreCase(categoryName) && setReindex) {
                                idx.canSkip = false;
                            } else {
                                idx.checkSkip(current, jukebox.getJukeboxRootLocationDetails());
                                skipIndex &= idx.canSkip;
                            }
                        }

                        if (skipIndex) {
                            logger.debug("Category " + categoryPath + " no change detected, skipping XML generation.");
                        } else {
                            for (current = 1; current <= last; current++) {
                                // All pages are handled here
                                next = (current % last) + 1; // this gives 1 for last
                                writeIndexPage(library, tmpMovieList.subList(moviepos, Math.min(moviepos + nbVideosPerPage, tmpMovieList.size())),
                                        jukebox.getJukeboxTempLocationDetails(), idx, previous, current, next, last);

                                moviepos += nbVideosPerPage;
                                previous = current;
                            }
                        }

                        library.addGeneratedIndex(idx);
                        return null;
                    }
                });
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
    public void writeIndexPage(Library library, List<Movie> movies, String rootPath, IndexInfo idx, int previous, int current, int next, int last) {
        String prefix = idx.baseName;
        File xmlFile = new File(rootPath, prefix + current + ".xml");

        Document xmlDoc;
        try {
            xmlDoc = DOMHelper.createDocument();
        } catch (ParserConfigurationException error) {
            logger.error("Failed writing index page: " + xmlFile.getName());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return;
        }

        FileTools.addJukeboxFile(xmlFile.getName());
        boolean isCurrentKey = false;

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
            eCategory.setAttribute("name", categoryKey);
            if (isCurrentKey) {
                eCategory.setAttribute("current", "true");
            }

            int indexSize = 0;
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

                for (String catOriginalName : cm.keySet()) {
                    String catNewName = cm.get(catOriginalName);
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
                for (String categoryName : index.keySet()) {
                    indexSize = index.get(categoryName).size();
                    Element eIndexCategory = processIndexCategory(xmlDoc, categoryName, categoryKey, isCurrentKey, idx, indexSize, previous, current, next,
                            last);
                    if (eIndexCategory != null) {
                        eCategory.appendChild(eIndexCategory);
                        categoryCount++;
                    }
                }
            }

            // Only output the category if there are entries
            if (categoryCount > 0) {
                // Write the actual count of the category
                eCategory.setAttribute("count", String.valueOf(categoryCount));
                eLibrary.appendChild(eCategory);
                libraryCount++;
            }
        }

        // FIXME: The count here is off. It needs to be correct
        Element eMovies = xmlDoc.createElement("movies");
        eMovies.setAttribute("cols", String.valueOf(idx.videosPerLine));
        eMovies.setAttribute("count", String.valueOf(idx.videosPerPage));

        eMovies.setAttribute("indexCount", String.valueOf(library.getMovieCountForIndex(idx.categoryName, idx.key)));
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
        eLibrary.setAttribute("count", String.valueOf(libraryCount));

        // Add the Library node to the document
        xmlDoc.appendChild(eLibrary);

        // Save the document to file
        DOMHelper.writeDocumentToFile(xmlDoc, xmlFile);

        return;
    }

    private Element processIndexCategory(Document doc, String categoryName, String categoryKey, boolean isCurrentKey, IndexInfo idx, int indexSize,
            int previous, int current, int next, int last) {
        String encakey = FileTools.createCategoryKey(categoryName);
        boolean isCurrentCat = isCurrentKey && encakey.equalsIgnoreCase(idx.key);
        String prefix = idx.baseName;

        // Check to see if we need the non-current index
        if (!isCurrentCat && !fullCategoriesInIndexes) {
            // We don't need this index, so skip it
            return null;
        }

        int categoryMinCount = calcMinCategoryCount(categoryName);

        // FIXME This is horrible! Issue 735 will get rid of it.
        if (indexSize < categoryMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryKey)) {
            return null;
        }

        prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, encakey));

        Element eCategory = doc.createElement("index");
        eCategory.setAttribute("name", categoryName);

        // The category changes only occur for "Other" category
        if (Library.INDEX_OTHER.equals(categoryKey)) {
            eCategory.setAttribute("originalName", Library.getOriginalCategory(encakey));
        }

        // if currently writing this page then add current attribute with value true
        if (isCurrentCat) {
            eCategory.setAttribute("current", "true");
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
        Element eMovie = doc.createElement("movie");

        eMovie.setAttribute("isExtra", Boolean.toString(movie.isExtra()));
        eMovie.setAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            eMovie.setAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        eMovie.setAttribute("isTV", Boolean.toString(movie.isTVShow()));

        DOMHelper.appendChild(doc, eMovie, "details", HTMLTools.encodeUrl(movie.getBaseName()) + ".html");
        DOMHelper.appendChild(doc, eMovie, "baseFilenameBase", movie.getBaseFilename());
        DOMHelper.appendChild(doc, eMovie, "baseFilename", movie.getBaseName());
        DOMHelper.appendChild(doc, eMovie, "title", movie.getTitle());
        DOMHelper.appendChild(doc, eMovie, "titleSort", movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, "originalTitle", movie.getOriginalTitle());
        DOMHelper.appendChild(doc, eMovie, "detailPosterFile", HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "thumbnail", HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        DOMHelper.appendChild(doc, eMovie, "bannerFile", HTMLTools.encodeUrl(movie.getBannerFilename()));
        DOMHelper.appendChild(doc, eMovie, "certification", movie.getCertification());
        DOMHelper.appendChild(doc, eMovie, "season", Integer.toString(movie.getSeason()));

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
            eSet.setAttribute("count", String.valueOf(items.size()));
            for (String item : items) {
                writeIndexedElement(doc, eSet, element, item, createIndexAttribute(library, cat, item));
            }
            return eSet;
        } else {
            return null;
        }
    }

    /**
     * Write the element with the indexed attribute. If there is a non-null value in the indexValue, this will be appended to the element.
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
            DOMHelper.appendChild(doc, parentElement, attributeName, attributeValue, "index", indexValue);
        }
    }

    /**
     * Create the index filename for a category & value. Will return "null" if no index found
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
            int categoryMinCount = calcMinCategoryCount(categoryName);

            if (library.getMovieCountForIndex(categoryName, value) >= categoryMinCount) {
                return HTMLTools.encodeUrl(FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, value)) + 1);
            }
        }
        return null;
    }

    /**
     * Calculate the minimum count for a category based on it's property value.
     * 
     * @param categoryName
     * @return
     */
    private int calcMinCategoryCount(String categoryName) {
        int categoryMinCount = categoryMinCountMaster;
        try {
            categoryMinCount = PropertiesUtil.getIntProperty("mjb.categories.minCount." + categoryName, String.valueOf(categoryMinCountMaster));
        } catch (Exception ignore) {
            categoryMinCount = categoryMinCountMaster;
        }
        return categoryMinCount;
    }

    private Element writeMovie(Document doc, Movie movie, Library library) {
        Element eMovie = doc.createElement("movie");

        eMovie.setAttribute("isExtra", Boolean.toString(movie.isExtra()));
        eMovie.setAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            eMovie.setAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        eMovie.setAttribute("isTV", Boolean.toString(movie.isTVShow()));

        for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
            DOMHelper.appendChild(doc, eMovie, "id", e.getValue(), "moviedb", e.getKey());
        }

        DOMHelper.appendChild(doc, eMovie, "mjbVersion", movie.getCurrentMjbVersion());
        DOMHelper.appendChild(doc, eMovie, "mjbRevision", movie.getCurrentMjbRevision());
        DOMHelper.appendChild(doc, eMovie, "xmlGenerationDate", Movie.dateFormatLong.format(new Date()));
        DOMHelper.appendChild(doc, eMovie, "baseFilenameBase", movie.getBaseFilename());
        DOMHelper.appendChild(doc, eMovie, "baseFilename", movie.getBaseName());
        DOMHelper.appendChild(doc, eMovie, "title", movie.getTitle());
        DOMHelper.appendChild(doc, eMovie, "titleSort", movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, "originalTitle", movie.getOriginalTitle());

        DOMHelper.appendChild(doc, eMovie, "year", movie.getYear(), "Year", Library.getYearCategory(movie.getYear()));

        DOMHelper.appendChild(doc, eMovie, "releaseDate", movie.getReleaseDate());
        DOMHelper.appendChild(doc, eMovie, "showStatus", movie.getShowStatus());

        // This is the main rating
        DOMHelper.appendChild(doc, eMovie, "rating", Integer.toString(movie.getRating()));

        // This is the list of ratings
        Element eRatings = doc.createElement("ratings");
        for (String site : movie.getRatings().keySet()) {
            DOMHelper.appendChild(doc, eRatings, "rating", Integer.toString(movie.getRating(site)), "moviedb", site);
        }
        eMovie.appendChild(eRatings);

        DOMHelper.appendChild(doc, eMovie, "watched", Boolean.toString(movie.isWatched()));
        DOMHelper.appendChild(doc, eMovie, "watchedNFO", Boolean.toString(movie.isWatchedNFO()));
        if (movie.isWatched()) {
            DOMHelper.appendChild(doc, eMovie, "watchedDate", movie.getWatchedDateString());
        }
        DOMHelper.appendChild(doc, eMovie, "top250", Integer.toString(movie.getTop250()));
        DOMHelper.appendChild(doc, eMovie, "details", HTMLTools.encodeUrl(movie.getBaseName()) + ".html");
        DOMHelper.appendChild(doc, eMovie, "posterURL", HTMLTools.encodeUrl(movie.getPosterURL()));
        DOMHelper.appendChild(doc, eMovie, "posterFile", HTMLTools.encodeUrl(movie.getPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "fanartURL", HTMLTools.encodeUrl(movie.getFanartURL()));
        DOMHelper.appendChild(doc, eMovie, "fanartFile", HTMLTools.encodeUrl(movie.getFanartFilename()));
        DOMHelper.appendChild(doc, eMovie, "detailPosterFile", HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "thumbnail", HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        DOMHelper.appendChild(doc, eMovie, "bannerURL", HTMLTools.encodeUrl(movie.getBannerURL()));
        DOMHelper.appendChild(doc, eMovie, "bannerFile", HTMLTools.encodeUrl(movie.getBannerFilename()));

        Element eArtwork = doc.createElement("artwork");
        for (ArtworkType artworkType : ArtworkType.values()) {
            Collection<Artwork> artworkList = movie.getArtwork(artworkType);
            if (artworkList.size() > 0) {
                Element eArtworkType;

                for (Artwork artwork : artworkList) {
                    eArtworkType = doc.createElement(artworkType.toString());
                    eArtworkType.setAttribute("count", String.valueOf(artworkList.size()));

                    DOMHelper.appendChild(doc, eArtworkType, "sourceSite", artwork.getSourceSite());
                    DOMHelper.appendChild(doc, eArtworkType, "url", artwork.getUrl());

                    for (ArtworkFile artworkFile : artwork.getSizes()) {
                        DOMHelper.appendChild(doc, eArtworkType, artworkFile.getSize().toString(), artworkFile.getFilename(), "downloaded",
                                String.valueOf(artworkFile.isDownloaded()));
                    }
                    eArtwork.appendChild(eArtworkType);
                }
            } else {
                // Write a dummy node
                Element eArtworkType = doc.createElement(artworkType.toString());
                eArtworkType.setAttribute("count", String.valueOf(0));

                DOMHelper.appendChild(doc, eArtworkType, "url", Movie.UNKNOWN);
                DOMHelper.appendChild(doc, eArtworkType, ArtworkSize.LARGE.toString(), Movie.UNKNOWN, "downloaded", "false");

                eArtwork.appendChild(eArtworkType);
            }
        }
        eMovie.appendChild(eArtwork);

        DOMHelper.appendChild(doc, eMovie, "plot", movie.getPlot());
        DOMHelper.appendChild(doc, eMovie, "outline", movie.getOutline());
        DOMHelper.appendChild(doc, eMovie, "quote", movie.getQuote());
        DOMHelper.appendChild(doc, eMovie, "tagline", movie.getTagline());

        writeIndexedElement(doc, eMovie, "country", movie.getCountry(), createIndexAttribute(library, Library.INDEX_COUNTRY, movie.getCountry()));

        DOMHelper.appendChild(doc, eMovie, "company", movie.getCompany());
        DOMHelper.appendChild(doc, eMovie, "runtime", movie.getRuntime());
        DOMHelper.appendChild(doc, eMovie, "certification", Library.getIndexingCertification(movie.getCertification()));
        DOMHelper.appendChild(doc, eMovie, "season", Integer.toString(movie.getSeason()));
        DOMHelper.appendChild(doc, eMovie, "language", movie.getLanguage());
        DOMHelper.appendChild(doc, eMovie, "subtitles", movie.getSubtitles());
        DOMHelper.appendChild(doc, eMovie, "trailerExchange", movie.isTrailerExchange() ? "YES" : "NO");

        if (movie.getTrailerLastScan() == 0) {
            DOMHelper.appendChild(doc, eMovie, "trailerLastScan", Movie.UNKNOWN);
        } else {
            DOMHelper.appendChild(doc, eMovie, "trailerLastScan", Movie.dateFormat.format(movie.getTrailerLastScan()));
        }

        DOMHelper.appendChild(doc, eMovie, "container", movie.getContainer());
        DOMHelper.appendChild(doc, eMovie, "videoCodec", movie.getVideoCodec());
        DOMHelper.appendChild(doc, eMovie, "audioCodec", movie.getAudioCodec());

        // Write codec information
        Element eCodecs = doc.createElement("codecs");
        
            Element eCodecAudio = doc.createElement("audio");
            Element eCodecVideo = doc.createElement("video");

            HashMap<String, String> codecAttribs = new HashMap<String, String>();

            for (Codec codec : movie.getCodecs()) {
                codecAttribs.clear();

                codecAttribs.put("format", codec.getCodecFormat());
                codecAttribs.put("formatProfile", codec.getCodecFormatProfile());
                codecAttribs.put("formatVersion", codec.getCodecFormatVersion());
                codecAttribs.put("codecId", codec.getCodecId());
                codecAttribs.put("codecIdHint", codec.getCodecIdHint());
                codecAttribs.put("language", codec.getCodecLanguage());

                if (codec.getCodecType() == Codec.CodecType.AUDIO) {
                    DOMHelper.appendChild(doc, eCodecAudio, "codec", codec.getCodec(), codecAttribs);
                } else {
                    DOMHelper.appendChild(doc, eCodecVideo, "codec", codec.getCodec(), codecAttribs);
                }
            }
            eCodecs.appendChild(eCodecAudio);
            eCodecs.appendChild(eCodecVideo);

        eMovie.appendChild(eCodecs);

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
                DOMHelper.appendChild(doc, eMovie, "fileDate", Movie.dateFormat.format(movie.getFileDate()));
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
            eGenres.setAttribute("count", String.valueOf(movie.getGenres().size()));
            for (String genre : movie.getGenres()) {
                writeIndexedElement(doc, eGenres, "genre", genre, createIndexAttribute(library, Library.INDEX_GENRES, Library.getIndexingGenre(genre)));
            }
            eMovie.appendChild(eGenres);
        }

        Collection<String> items = movie.getSetsKeys();
        if (items.size() > 0) {
            Element eSets = doc.createElement("sets");
            eSets.setAttribute("count", String.valueOf(items.size()));
            for (String item : items) {
                Element eSetItem = doc.createElement("set");
                Integer order = movie.getSetOrder(item);
                if (null != order) {
                    eSetItem.setAttribute("order", String.valueOf(order));
                }
                String index = createIndexAttribute(library, Library.INDEX_SET, item);
                if (null != index) {
                    eSetItem.setAttribute("index", index);
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
                for (AwardEvent event : awards) {
                    Element eEvent = doc.createElement("event");
                    eEvent.setAttribute("name", event.getName());
                    for (Award award : event.getAwards()) {
                        Element eAwardItem = doc.createElement("award");
                        eAwardItem.setAttribute("won", Integer.toString(award.getWon()));
                        eAwardItem.setAttribute("nominated", Integer.toString(award.getNominated()));
                        eAwardItem.setAttribute("year", Integer.toString(award.getYear()));
                        if (award.getWons() != null && award.getWons().size() > 0) {
                            eAwardItem.setAttribute("wons", StringUtils.join(award.getWons(), " / "));
                        }
                        if (award.getNominations() != null && award.getNominations().size() > 0) {
                            eAwardItem.setAttribute("nominations", StringUtils.join(award.getNominations(), " / "));
                        }
                        eAwardItem.setTextContent(award.getName());
                        eEvent.appendChild(eAwardItem);
                    }
                    eAwards.appendChild(eEvent);
                }
                eMovie.appendChild(eAwards);
            }
        }

        // Issue 1897: Cast enhancement
        Collection<Filmography> people = movie.getPeople();
        if (people != null && people.size() > 0) {
            Element ePeople = doc.createElement("people");
            for (Filmography person : people) {
                Element ePerson = doc.createElement("person");

                ePerson.setAttribute("name", person.getName());
                ePerson.setAttribute("doublage", person.getDoublage());
                ePerson.setAttribute("title", person.getTitle());
                ePerson.setAttribute("character", person.getCharacter());
                ePerson.setAttribute("job", person.getJob());
                ePerson.setAttribute("id", person.getId());
                for (Map.Entry<String, String> personID : person.getIdMap().entrySet()) {
                    if (!personID.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                        ePerson.setAttribute("id_" + personID.getKey(), personID.getValue());
                    }
                }
                ePerson.setAttribute("department", person.getDepartment());
                ePerson.setAttribute("url", person.getUrl());
                ePerson.setAttribute("order", Integer.toString(person.getOrder()));
                ePerson.setAttribute("cast_id", Integer.toString(person.getCastId()));
                ePerson.setTextContent(person.getFilename());
                ePeople.appendChild(ePerson);
            }
            eMovie.appendChild(ePeople);
        }

        // Issue 2012: Financial information about movie
        if (enableBusiness) {
            Element eBusiness = doc.createElement("business");
            eBusiness.setAttribute("budget", movie.getBudget());

            for (Map.Entry<String, String> gross : movie.getGross().entrySet()) {
                DOMHelper.appendChild(doc, eBusiness, "gross", gross.getValue(), "country", gross.getKey());
            }

            for (Map.Entry<String, String> openweek : movie.getOpenWeek().entrySet()) {
                DOMHelper.appendChild(doc, eBusiness, "openweek", openweek.getValue(), "country", openweek.getKey());
            }

            eMovie.appendChild(eBusiness);
        }

        // Issue 2013: Add trivia
        if (enableTrivia) {
            Element eTrivia = doc.createElement("didyouknow");

            for (String trivia : movie.getDidYouKnow()) {
                DOMHelper.appendChild(doc, eTrivia, "trivia", trivia);
            }

            eMovie.appendChild(eTrivia);
        }

        // Write the indexes that the movie belongs to
        Element eIndexes = doc.createElement("indexes");
        String originalName = Movie.UNKNOWN;
        for (Entry<String, String> index : movie.getIndexes().entrySet()) {
            Element eIndexEntry = doc.createElement("index");
            eIndexEntry.setAttribute("type", index.getKey());
            originalName = Library.getOriginalCategory(index.getKey());
            if (StringTools.isValidString(originalName)) {
                eIndexEntry.setAttribute("originalName", originalName);
            } else {
                eIndexEntry.setAttribute("originalName", index.getKey());
            }
            eIndexEntry.setAttribute("encoded", FileTools.makeSafeFilename(index.getValue()));
            eIndexEntry.setTextContent(index.getValue());
            eIndexes.appendChild(eIndexEntry);
        }
        eMovie.appendChild(eIndexes);

        // Write details about the files
        Element eFiles = doc.createElement("files");
        for (MovieFile mf : movie.getFiles()) {
            Element eFileItem = doc.createElement("file");
            eFileItem.setAttribute("season", Integer.toString(mf.getSeason()));
            eFileItem.setAttribute("firstPart", Integer.toString(mf.getFirstPart()));
            eFileItem.setAttribute("lastPart", Integer.toString(mf.getLastPart()));
            eFileItem.setAttribute("title", mf.getTitle());
            eFileItem.setAttribute("subtitlesExchange", mf.isSubtitlesExchange() ? "YES" : "NO");

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

            eFileItem.setAttribute("watched", mf.isWatched() ? "true" : "false");

            if (mf.getFile() != null) {
                DOMHelper.appendChild(doc, eFileItem, "fileLocation", mf.getFile().getAbsolutePath());
            }

            // Write the fileURL
            String filename = mf.getFilename();
            // Issue 1237: Add "VIDEO_TS.IFO" for PlayOnHD VIDEO_TS path names
            if (isPlayOnHD) {
                if (filename.toUpperCase().endsWith("VIDEO_TS")) {
                    filename = filename + "/VIDEO_TS.IFO";
                }
            }
            DOMHelper.appendChild(doc, eFileItem, "fileURL", filename);

            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); ++part) {
                DOMHelper.appendChild(doc, eFileItem, "fileTitle", mf.getTitle(part), "part", Integer.toString(part));

                // Only write out these for TV Shows
                if (movie.isTVShow()) {
                    Map<String, String> tvAttribs = new HashMap<String, String>();
                    tvAttribs.put("part", Integer.toString(part));
                    tvAttribs.put("afterSeason", mf.getAirsAfterSeason(part));
                    tvAttribs.put("beforeSeason", mf.getAirsBeforeSeason(part));
                    tvAttribs.put("beforeEpisode", mf.getAirsBeforeEpisode(part));
                    DOMHelper.appendChild(doc, eFileItem, "airsInfo", String.valueOf(part), tvAttribs);

                    DOMHelper.appendChild(doc, eFileItem, "firstAired", mf.getFirstAired(part), "part", String.valueOf(part));
                }

                if (StringTools.isValidString(mf.getWatchedDateString())) {
                    DOMHelper.appendChild(doc, eFileItem, "watchedDate", mf.getWatchedDateString());
                }

                if (includeEpisodePlots) {
                    DOMHelper.appendChild(doc, eFileItem, "filePlot", mf.getPlot(part), "part", String.valueOf(part));
                }

                if (includeEpisodeRating) {
                    DOMHelper.appendChild(doc, eFileItem, "fileRating", mf.getRating(part), "part", String.valueOf(part));
                }

                if (includeVideoImages) {
                    DOMHelper.appendChild(doc, eFileItem, "fileImageURL", HTMLTools.encodeUrl(mf.getVideoImageURL(part)), "part", String.valueOf(part));
                    DOMHelper.appendChild(doc, eFileItem, "fileImageFile", HTMLTools.encodeUrl(mf.getVideoImageFilename(part)), "part", String.valueOf(part));
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
                eExtraItem.setAttribute("title", ef.getTitle());
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
     * Persist a movie into an XML file. Doesn't overwrite an already existing XML file for the specified movie unless, movie's data has changed or
     * forceXMLOverwrite is true.
     */
    public void writeMovieXML(Jukebox jukebox, Movie movie, Library library) {
        String baseName = movie.getBaseName();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + ".xml");
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + baseName + ".xml");

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty(Movie.DIRTY_INFO) || movie.isDirty(Movie.DIRTY_RECHECK)) {
            Document xmlDoc;
            try {
                xmlDoc = DOMHelper.createDocument();
            } catch (ParserConfigurationException error) {
                logger.error("Failed writing " + tempXmlFile.getAbsolutePath());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
                return;
            }

            Element eDetails = xmlDoc.createElement("details");
            xmlDoc.appendChild(eDetails);

            Element eMovie = writeMovie(xmlDoc, movie, library);

            if (eMovie != null) {
                eDetails.appendChild(eMovie);
            }

            DOMHelper.writeDocumentToFile(xmlDoc, tempXmlFile);

            if (writeNfoFiles) {
                writeNfoFile(jukebox, movie);
            }
        }
    }

    private Element writePerson(Document doc, Person person) {
        Element ePerson = doc.createElement("person");

        for (Map.Entry<String, String> e : person.getIdMap().entrySet()) {
            DOMHelper.appendChild(doc, ePerson, "id", e.getValue(), "persondb", e.getKey());
        }

        DOMHelper.appendChild(doc, ePerson, "name", person.getName());
        DOMHelper.appendChild(doc, ePerson, "title", person.getTitle());
        DOMHelper.appendChild(doc, ePerson, "baseFilename", person.getFilename());

        if (person.getAka().size() > 0) {
            Element eAka = doc.createElement("aka");
            for (String aka : person.getAka()) {
                DOMHelper.appendChild(doc, eAka, "name", aka);
            }
            ePerson.appendChild(eAka);
        }

        DOMHelper.appendChild(doc, ePerson, "biography", person.getBiography());
        DOMHelper.appendChild(doc, ePerson, "birthday", person.getYear());
        DOMHelper.appendChild(doc, ePerson, "birthplace", person.getBirthPlace());
        DOMHelper.appendChild(doc, ePerson, "url", person.getUrl());
        DOMHelper.appendChild(doc, ePerson, "photoFile", person.getPhotoFilename());
        DOMHelper.appendChild(doc, ePerson, "photoURL", person.getPhotoURL());
        DOMHelper.appendChild(doc, ePerson, "knownMovies", String.valueOf(person.getKnownMovies()));

        if (person.getFilmography().size() > 0) {
            Element eFilmography = doc.createElement("filmography");

            for (Filmography film : person.getFilmography()) {
                Element eMovie = doc.createElement("movie");
                eMovie.setAttribute("id", film.getId());

                for (Map.Entry<String, String> e : film.getIdMap().entrySet()) {
                    if (!e.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                        eMovie.setAttribute("id_" + e.getKey(), e.getValue());
                    }
                }
                eMovie.setAttribute("name", film.getName());
                eMovie.setAttribute("title", film.getTitle());
                eMovie.setAttribute("originalTitle", film.getOriginalTitle());
                eMovie.setAttribute("year", film.getYear());
                eMovie.setAttribute("rating", film.getRating());
                eMovie.setAttribute("character", film.getCharacter());
                eMovie.setAttribute("job", film.getJob());
                eMovie.setAttribute("department", film.getDepartment());
                eMovie.setAttribute("url", film.getUrl());
                eMovie.setTextContent(film.getFilename());

                eFilmography.appendChild(eMovie);
            }
            ePerson.appendChild(eFilmography);
        }

        DOMHelper.appendChild(doc, ePerson, "version", String.valueOf(person.getVersion()));
        DOMHelper.appendChild(doc, ePerson, "lastModifiedAt", person.getLastModifiedAt());

        return ePerson;
    }

    public void writePersonXML(Jukebox jukebox, Person person, Library library) {
        String baseName = person.getFilename();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + baseName + ".xml");
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + baseName + ".xml");
        finalXmlFile.getParentFile().mkdirs();
        tempXmlFile.getParentFile().mkdirs();

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || person.isDirty()) {
            try {
                Document personDoc = DOMHelper.createDocument();
                Element eDetails = personDoc.createElement("details");
                eDetails.appendChild(writePerson(personDoc, person));
                personDoc.appendChild(eDetails);
                DOMHelper.writeDocumentToFile(personDoc, tempXmlFile);
            } catch (ParserConfigurationException error) {
                logger.error("Failed writing person XML for " + tempXmlFile.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
                return;
            }
        }
    }

    /**
     * Write a NFO file for the movie using the data gathered
     * 
     * @param jukebox
     * @param movie
     */
    public void writeNfoFile(Jukebox jukebox, Movie movie) {
        // Don't write NFO files for sets or extras
        if (movie.isSetMaster() || movie.isExtra()) {
            return;
        }

        Document docNFO;
        Element eRoot, eRatings, eCredits, eDirectors, eActors;

        try {
            docNFO = DOMHelper.createDocument();
        } catch (ParserConfigurationException error) {
            logger.warn("Failed to create NFO file for " + movie.getBaseFilename());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return;
        }

        String nfoFolder = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), "NFO");
        (new File(nfoFolder)).mkdirs();
        File tempNfoFile = new File(StringTools.appendToPath(nfoFolder, movie.getBaseName() + ".nfo"));

        logger.debug("MovieJukeboxXMLWriter: Writing " + (writeSimpleNfoFiles ? "simple " : "") + "NFO file for " + movie.getBaseName() + ".nfo");
        FileTools.addJukeboxFile(tempNfoFile.getName());

        // Define the root element
        if (movie.isTVShow()) {
            eRoot = docNFO.createElement("tvshow");
        } else {
            eRoot = docNFO.createElement("movie");
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

            if (!movie.getWriters().isEmpty()) {
                eCredits = docNFO.createElement("credits");
                eRoot.appendChild(eCredits);

                for (String writerCredit : movie.getWriters()) {
                    DOMHelper.appendChild(docNFO, eCredits, "writer", writerCredit);
                }
            }

            if (!movie.getDirectors().isEmpty()) {
                eDirectors = docNFO.createElement("directors");
                eRoot.appendChild(eDirectors);
                for (String director : movie.getDirectors()) {
                    DOMHelper.appendChild(docNFO, eDirectors, "director", director);
                }
            }

            if (StringTools.isValidString(movie.getCompany())) {
                DOMHelper.appendChild(docNFO, eRoot, "company", movie.getCompany());
            }

            if (StringTools.isValidString(movie.getCountry())) {
                DOMHelper.appendChild(docNFO, eRoot, "country", movie.getCountry());
            }

            if (!movie.getCast().isEmpty()) {
                eActors = docNFO.createElement("actor");

                for (String actor : movie.getCast()) {
                    DOMHelper.appendChild(docNFO, eActors, "name", actor);
                    DOMHelper.appendChild(docNFO, eActors, "role", "");
                }
                eRoot.appendChild(eActors);
            }

        }

        DOMHelper.writeDocumentToFile(docNFO, tempNfoFile.getAbsolutePath());

    }
}