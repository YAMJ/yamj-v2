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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.concurrent.Callable;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.XMLWriter;

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
    private static boolean isPlayOnHD;
    private static String defaultSource;
    private List<String> categoriesExplodeSet = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.explodeSet", "").split(","));
    private static String strCategoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.displayList", "");
    private static List<String> categoriesDisplayList = Collections.emptyList();
    private static int categoryMinCountMaster = PropertiesUtil.getIntProperty("mjb.categories.minCount", "3");
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static boolean writeNfoFiles;
    private boolean extractCertificationFromMPAA;
    private boolean setsExcludeTV;
    private static String peopleFolder;
    private static boolean enableWatchScanner;

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
                if (tag.equalsIgnoreCase("<baseFilenameBase>") && (movie.getBaseFilename() == null || movie.getBaseFilename() == Movie.UNKNOWN)) {
                    movie.setBaseFilename(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<baseFilename>") && (movie.getBaseName() == null || movie.getBaseName() == Movie.UNKNOWN)) {
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
                    //movie.setRating(Integer.parseInt(parseCData(r)));
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
                    movie.setAspectRatio(parseCData(r));
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
                        mf.setNewFile(true);    // Mark the moviefile as new to force it to be written out
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
                    String extraTitle    = "";
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

                            // fixed by ilgizar/30.04.2011
                            // after rescan extrafiles are empty
                            ExtraFile ef = new ExtraFile();
                            ef.setTitle(extraTitle);
                            ef.setFilename(extraFilename);
                            ef.setNewFile(false);
                            movie.addExtraFile(ef);
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

    @SuppressWarnings("unchecked")
    public Map<String, Integer> parseMovieXML_set(File xmlFile) {
        Map<String, Integer> sets = new HashMap<String, Integer>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(FileTools.createFileInputStream(xmlFile), "UTF-8");
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                String tag = e.toString();

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
                    sets.put(parseCData(r), order);
                }
            }
        } catch (Exception error) {
            logger.error("Failed parsing movie xml for set : please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return sets;
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
                    for (Movie film : moviesList) {
                        if (film.getBaseName().equals(movieName)) {
                            // See if the movie is in a collection OR isDirty
                            forceDirtyFlag |= (!film.isTVShow() && !film.getSetsKeys().contains(setMaster.getBaseFilename())) || film.isDirty(Movie.DIRTY_INFO);
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

        boolean forceDirtyFlag = false; // force dirty flag for example when extras has been deleted

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

        person.setDirty(forceDirtyFlag);

        return true;
    }

    public void writeCategoryXML(Jukebox jukebox, Library library, String filename, boolean isDirty) throws FileNotFoundException, XMLStreamException {
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

        XMLWriter writer = new XMLWriter(xmlFile);

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("library");

        List<Movie> allMovies = library.getMoviesList();

        // Add the movie count to the library
        writer.writeAttribute("count", "" + allMovies.size());

        if (includeMoviesInCategories) {
            for (Movie movie : library.getMoviesList()) {
                if (fullMovieInfoInIndexes) {
                    writeMovie(writer, movie, library);
                } else {
                    writeMovieForIndex(writer, movie);
                }
            }
        }

        // Issue 1148, generate category in the order specified in properties
        logger.info("  Indexing " + filename + "...");
        for (String categoryName : categoriesDisplayList) {
            int categoryMinCount = calcMinCategoryCount(categoryName);
            boolean openedCategory = false;
            for (Entry<String, Index> category : library.getIndexes().entrySet()) {
                // Category not empty and match the current cat.
                if (!category.getValue().isEmpty() && categoryName.equalsIgnoreCase(category.getKey()) && (filename.equals("Categories") || filename.equals(category.getKey()))) {
                    openedCategory = true;
                    
                    writer.writeStartElement("category");
                    writer.writeAttribute("name", category.getKey());
                    writer.writeAttribute("count", "" + category.getValue().size());

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
                                processCategoryIndex(catNewName, catOriginalName, category.getValue().get(catNewName), categoryName, categoryMinCount, library, writer);
                            }
                        }
                    } else {
                        // Process the remaining categories
                        for (Map.Entry<String, List<Movie>> index : category.getValue().entrySet()) {
                            processCategoryIndex(index.getKey(), index.getKey(), index.getValue(), categoryName, categoryMinCount, library, writer);
                        }
                    }
                }
            }
            if (openedCategory) {
                writer.writeEndElement(); // category
            }
        }
        writer.writeEndElement(); // library
        writer.writeEndDocument();
        writer.close();
    }

    /**
     * Used in the writeCategoryXML method to process each index
     * @param indexName         The renamed index name
     * @param indexOriginalName The original index name
     * @param indexMovies       List of the movies to process
     * @param categoryKey       The name of the category
     * @param categoryMinCount
     * @param library
     * @param writer
     * @throws XMLStreamException
     */
    private void processCategoryIndex(String indexName, String indexOriginalName, List<Movie> indexMovies, String categoryKey, int categoryMinCount, Library library, XMLWriter writer) throws XMLStreamException {

        List<Movie> allMovies = library.getMoviesList();
        int countMovieCat = library.getMovieCountForIndex(categoryKey, indexName);

        logger.debug("Index: " + categoryKey + ", Category: " + indexName + ", count: " + indexMovies.size());
        // Display a message about the category we're indexing
        if (countMovieCat < categoryMinCount && !categoriesDisplayList.contains(categoryKey)) {
            logger.debug("Category " + categoryKey + " " + indexName + " does not contain enough videos (" + countMovieCat
                            + "/" + categoryMinCount + "), not adding to categories.xml.");
            return;
        }

        if (setsExcludeTV && categoryKey.equalsIgnoreCase(Library.INDEX_SET) && indexMovies.get(0).isTVShow()) {
            // Do not include the video in the set because it's a TV show
            return;
        }

        String indexFilename = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, indexName)) + "1";

        writer.writeStartElement("index");
        writer.writeAttribute("name", indexName);
        writer.writeAttribute("originalName", indexOriginalName);

        if (includeMoviesInCategories) {
            writer.writeAttribute("filename", indexFilename);

            for (Identifiable movie : indexMovies) {
                writer.writeStartElement("movie");
                writer.writeCharacters(Integer.toString(allMovies.indexOf(movie)));
                writer.writeEndElement();
            }
        } else {
            writer.writeCharacters(indexFilename);
        }

        writer.writeEndElement();
    
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
                    public Void call() throws XMLStreamException, FileNotFoundException {
                        List<Movie> movies = group.getValue();
                        String key = FileTools.createCategoryKey(group.getKey());
                        String categoryPath = categoryName + " " + key;

                        // FIXME This is horrible! Issue 735 will get rid of it.
                        int categoryCount = library.getMovieCountForIndex(categoryName, key);
                        if (categoryCount < categoryMinCount && !categoriesDisplayList.contains(categoryName) && !separateCategories) {
                            logger.debug("Category " + categoryPath + " does not contain enough videos (" + categoryCount
                                            + "/" + categoryMinCount + "), skipping XML generation.");
                            return null;
                        }
                        boolean skipIndex = !forceIndexOverwrite;

                        // Try and determine if the set contains TV shows and therefore use the TV show settings
                        // TODO have a custom property so that you can set this on a per-set basis.
                        int nbVideosPerPage = nbMoviesPerPage, nbVideosPerLine = nbMoviesPerLine;

                        if (movies.size() > 0) {
                            if (key.equalsIgnoreCase(Library.INDEX_TVSHOWS)) {
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
                                    //Issue 1886: HTML indexes recreated every time
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
                            if (movie.isDirty(Movie.DIRTY_INFO)) {
                                skipIndex = false;
                            }
                            
                            // Check for changes to the Watched, Unwatched and New categories whilst we are processing the All category
                            if (enableWatchScanner && key.equals(Library.INDEX_ALL)) {
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
                            if (reindexNew && (key.equals(Library.INDEX_NEW) || key.equals(Library.INDEX_NEW_MOVIE) || key.equals(Library.INDEX_NEW_TV) ) ) {
                                skipIndex = false;
                            }
                            
                            if (reindexWatched && key.equals(Library.INDEX_WATCHED)) {
                                skipIndex = false;
                            }
                            
                            if (reindexUnwatched && key.equals(Library.INDEX_UNWATCHED)) {
                                skipIndex = false;
                            }
                            
                            // Issue 1263 - Allow explode of Set in category .
                            if (movie.isSetMaster() && categoriesExplodeSet.contains(categoryName)) {
                                List<Movie> boxedSetMovies = library.getIndexes().get(Library.INDEX_SET).get(movie.getTitle());
                                boxedSetMovies = library.getMatchingMoviesList(categoryName, boxedSetMovies, key);
                                logger.debug("Exploding set for " + categoryPath + "[" + movie.getTitle() + "] " + boxedSetMovies.size());
                                //delay new instance
                                if(tmpMovieList == movies) {
                                    tmpMovieList = new ArrayList<Movie>(movies);
                                }

                                //do we want to keep the set?
                                //tmpMovieList.remove(moviepos);
                                tmpMovieList.addAll(moviepos, boxedSetMovies);
                                moviepos += boxedSetMovies.size() - 1; 
                            }
                            moviepos++;
                        }

                        int current = 1;
                        int last = 1 + (tmpMovieList.size() - 1) / nbVideosPerPage;
                        int next = Math.min(2, last);
                        int previous = last;
                        moviepos = 0;
                        IndexInfo idx = new IndexInfo(categoryName, key, last, nbVideosPerPage, nbVideosPerLine, skipIndex); 

                        // Don't skip the indexing for sets as this overwrites the set files
                        if (Library.INDEX_SET.equalsIgnoreCase(categoryName) && setReindex) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Forcing generation of set index.");
                            }
                            skipIndex = false;
                        }

                        for (current = 1 ; current <= last; current ++) {
                            if (Library.INDEX_SET.equalsIgnoreCase(categoryName) && setReindex) {
                                idx.canSkip = false;
                            } else {
                                idx.checkSkip(current, jukebox.getJukeboxRootLocationDetails());
                                skipIndex = skipIndex && idx.canSkip;
                            }
                        }

                        if (skipIndex) {
                            logger.debug("Category " + categoryPath + " no change detected, skipping XML generation.");
                        } else {
                            for (current = 1 ; current <= last ; current++) {
                                // All pages are handled here
                                next = (current % last) + 1; //this gives 1 for last
                                writeIndexPage(library, 
                                                tmpMovieList.subList(moviepos, Math.min(moviepos+nbVideosPerPage, tmpMovieList.size())), 
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
        };
        tasks.waitFor();
    }

    /**
     * Write out the index pages
     * 
     * @param library
     * @param movies
     * @param rootPath
     * @param categoryName
     * @param key
     * @param previous
     * @param current
     * @param next
     * @param last
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public void writeIndexPage(Library library, List<Movie> movies, String rootPath, IndexInfo idx, 
                    int previous, int current, int next, int last) throws FileNotFoundException, XMLStreamException {
        String prefix = idx.baseName;
        File xmlFile = null;
        XMLWriter writer = null;

        //FIXME: The categories are listed even if there are no entries, perhaps we should remove the empty categories at some point

        try {
            boolean isCurrentKey = false;

            xmlFile = new File(rootPath, prefix + current + ".xml");
            FileTools.addJukeboxFile(xmlFile.getName());

            writer = new XMLWriter(xmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("library");
            writer.writeAttribute("count", "" + library.getIndexes().size());

            for (Map.Entry<String, Index> category : library.getIndexes().entrySet()) {
                String categoryKey = category.getKey();
                Map<String, List<Movie>> index = category.getValue();

                // Is this the current category?
                isCurrentKey = categoryKey.equalsIgnoreCase(idx.categoryName);
                if (!isCurrentKey && !fullCategoriesInIndexes) {
                    // This isn't the current index, so we don't want it
                    continue;
                }
                
                writer.writeStartElement("category");
                writer.writeAttribute("name", categoryKey);
                if (isCurrentKey) {
                    writer.writeAttribute("current", "true");
                }
                writer.writeAttribute("count", "" + category.getValue().size());

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
                            processIndexCategory(catNewName, categoryKey, isCurrentKey, idx, writer, indexSize, previous, current, next, last);
                        }
                    }

                } else {
                    for (String categoryName : index.keySet()) {
                        indexSize = index.get(categoryName).size();
                        processIndexCategory(categoryName, categoryKey, isCurrentKey, idx, writer, indexSize, previous, current, next, last);
                    }
                }

                writer.writeEndElement(); // categories
            }
            // FIXME: The count here is off. It needs to be correct
            writer.writeStartElement("movies");
            writer.writeAttribute("count", "" + idx.videosPerPage);
            writer.writeAttribute("cols", "" + idx.videosPerLine);

            if (fullMovieInfoInIndexes) {
                for (Movie movie : movies) {
                    writeMovie(writer, movie, library);
                }
            } else {
                for (Movie movie : movies) {
                    writeMovieForIndex(writer, movie);
                }
            }
            writer.writeEndElement(); // movies

            writer.writeEndElement(); // library
            writer.writeEndDocument();
        } catch (Exception error) {
            logger.error("Failed writing index page: " + xmlFile.getName());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return;
        } finally {
            writer.close();
        }

        return;
    }
    
    private void processIndexCategory(String categoryName, String categoryKey, boolean isCurrentKey, IndexInfo idx, XMLWriter writer, int indexSize, int previous, int current, int next, int last) throws XMLStreamException {
        String encakey = FileTools.createCategoryKey(categoryName);
        boolean isCurrentCat = isCurrentKey && encakey.equalsIgnoreCase(idx.key);
        String prefix = idx.baseName;

        // Check to see if we need the non-current index
        if (!isCurrentCat && !fullCategoriesInIndexes) {
            // We don't need this index, so skip it
            return;
        }

        int categoryMinCount = calcMinCategoryCount(categoryName);

        // FIXME This is horrible! Issue 735 will get rid of it.
        if (indexSize < categoryMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryKey)) {
            return;
        }

        prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, encakey));

        writer.writeStartElement("index");
        writer.writeAttribute("name", categoryName);

        // The category changes only occur for "Other" category
        if (Library.INDEX_OTHER.equals(categoryKey)) {
            writer.writeAttribute("originalName", Library.getOriginalCategory(encakey));
        }

        // if currently writing this page then add current attribute with value true
        if (isCurrentCat) {
            writer.writeAttribute("current", "true");
            writer.writeAttribute("first", prefix + '1');
            writer.writeAttribute("previous", prefix + previous);
            writer.writeAttribute("next", prefix + next);
            writer.writeAttribute("last", prefix + last);
            writer.writeAttribute("currentIndex", Integer.toString(current));
            writer.writeAttribute("lastIndex", Integer.toString(last));
        }

        writer.writeCharacters(prefix + '1');
        writer.writeEndElement(); // index
    
    }

    private void writeMovieForIndex(XMLWriter writer, Movie movie) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isExtra", Boolean.toString(movie.isExtra()));
        writer.writeAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            writer.writeAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        writer.writeAttribute("isTV", Boolean.toString(movie.isTVShow()));
        writer.writeStartElement("details");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBaseName()) + ".html");
        writer.writeEndElement();
        writer.writeStartElement("baseFilenameBase");
        writer.writeCharacters(movie.getBaseFilename());
        writer.writeEndElement();
        writer.writeStartElement("baseFilename");
        writer.writeCharacters(movie.getBaseName());
        writer.writeEndElement();
        writer.writeStartElement("title");
        writer.writeCharacters(movie.getTitle());
        writer.writeEndElement();
        writer.writeStartElement("titleSort");
        writer.writeCharacters(movie.getTitleSort());
        writer.writeEndElement();
        writer.writeStartElement("originalTitle");
        writer.writeCharacters(movie.getOriginalTitle());
        writer.writeEndElement();
        writer.writeStartElement("detailPosterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        writer.writeEndElement();
        writer.writeStartElement("thumbnail");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        writer.writeEndElement();
        writer.writeStartElement("bannerFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBannerFilename()));
        writer.writeEndElement();
        writer.writeStartElement("certification");
        writer.writeCharacters(movie.getCertification());
        writer.writeEndElement();
        writer.writeStartElement("season");
        writer.writeCharacters(Integer.toString(movie.getSeason()));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeElementSet(XMLWriter writer, String set, String element, Collection<String> items, Library library, String cat) throws XMLStreamException {
        if (items.size() > 0) {
            writer.writeStartElement(set);
            for (String item : items) {
                writer.writeStartElement(element);
                writeIndexAttribute(writer, library, cat, item);
                writer.writeCharacters(item);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeIndexAttribute(XMLWriter writer, Library library, String category, String value) throws XMLStreamException {
        if (StringTools.isNotValidString(value)) {
            return;
        }
        
        final String index = createIndexAttribute(library, category, value);
        if (index != null) {
            writer.writeAttribute("index", index);
        }
    }

    private String createIndexAttribute(Library library, String categoryName, String val) throws XMLStreamException {
        Index index = library.getIndexes().get(categoryName);
        if (null != index) {
            int categoryMinCount = calcMinCategoryCount(categoryName);

            if (library.getMovieCountForIndex(categoryName, val) >= categoryMinCount) {
                return HTMLTools.encodeUrl(FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, val)) + 1);
            }
        }
        return null;
    }

    /**
     * Calculate the minimum count for a category based on it's property value.
     * @param categoryName
     * @return
     */
    private int calcMinCategoryCount(String categoryName) {
        int categoryMinCount = categoryMinCountMaster;
        try {
            categoryMinCount = PropertiesUtil.getIntProperty("mjb.categories.minCount." + categoryName, "" + categoryMinCountMaster);
        } catch (Exception ignore) {
            categoryMinCount = categoryMinCountMaster;
        }
        return categoryMinCount;
    }

    private void writeMovie(XMLWriter writer, Movie movie, Library library) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isExtra", Boolean.toString(movie.isExtra()));
        writer.writeAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        if (movie.isSetMaster()) {
            writer.writeAttribute("setSize", Integer.toString(movie.getSetSize()));
        }
        writer.writeAttribute("isTV", Boolean.toString(movie.isTVShow()));

        for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
            writer.writeStartElement("id");
            writer.writeAttribute("moviedb", e.getKey());
            writer.writeCharacters(e.getValue());
            writer.writeEndElement();
        }
        writer.writeStartElement("mjbVersion");
        writer.writeCharacters(movie.getCurrentMjbVersion());
        writer.writeEndElement();
        writer.writeStartElement("mjbRevision");
        writer.writeCharacters(movie.getCurrentMjbRevision());
        writer.writeEndElement();
        writer.writeStartElement("xmlGenerationDate");
        writer.writeCharacters(Movie.dateFormatLong.format(new Date()));
        writer.writeEndElement();
        writer.writeStartElement("baseFilenameBase");
        writer.writeCharacters(movie.getBaseFilename());
        writer.writeEndElement();
        writer.writeStartElement("baseFilename");
        writer.writeCharacters(movie.getBaseName());
        writer.writeEndElement();
        writer.writeStartElement("title");
        writer.writeCharacters(movie.getTitle());
        writer.writeEndElement();
        writer.writeStartElement("titleSort");
        writer.writeCharacters(movie.getTitleSort());
        writer.writeEndElement();
        writer.writeStartElement("originalTitle");
        writer.writeCharacters(movie.getOriginalTitle());
        writer.writeEndElement();
        writer.writeStartElement("year");
        writeIndexAttribute(writer, library, "Year", Library.getYearCategory(movie.getYear()));
        writer.writeCharacters(movie.getYear());
        writer.writeEndElement();
        writer.writeStartElement("releaseDate");
        writer.writeCharacters(movie.getReleaseDate());
        writer.writeEndElement();
        writer.writeStartElement("showStatus");
        writer.writeCharacters(movie.getShowStatus());
        writer.writeEndElement();
        
        // This is the main rating
        writer.writeStartElement("rating");
        writer.writeCharacters(Integer.toString(movie.getRating()));
        writer.writeEndElement();
        
        // This is the list of ratings
        writer.writeStartElement("ratings");
        for (String site : movie.getRatings().keySet()) {
            writer.writeStartElement("rating");
            writer.writeAttribute("moviedb", site);
            writer.writeCharacters(Integer.toString(movie.getRating(site)));
            writer.writeEndElement();
        }
        writer.writeEndElement();
        
        writer.writeStartElement("watched");
        writer.writeCharacters(Boolean.toString(movie.isWatched()));
        writer.writeEndElement();
        writer.writeStartElement("watchedNFO");
        writer.writeCharacters(Boolean.toString(movie.isWatchedNFO()));
        writer.writeEndElement();
        if (movie.isWatched()) {
            writer.writeStartElement("watchedDate");
            writer.writeCharacters(movie.getWatchedDateString());
            writer.writeEndElement();
        }
        writer.writeStartElement("top250");
        writer.writeCharacters(Integer.toString(movie.getTop250()));
        writer.writeEndElement();
        writer.writeStartElement("details");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBaseName()) + ".html");
        writer.writeEndElement();
        writer.writeStartElement("posterURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getPosterURL()));
        writer.writeEndElement();
        writer.writeStartElement("posterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getPosterFilename()));
        writer.writeEndElement();
        writer.writeStartElement("fanartURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getFanartURL()));
        writer.writeEndElement();
        writer.writeStartElement("fanartFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getFanartFilename()));
        writer.writeEndElement();
        writer.writeStartElement("detailPosterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        writer.writeEndElement();
        writer.writeStartElement("thumbnail");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        writer.writeEndElement();
        writer.writeStartElement("bannerURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBannerURL()));
        writer.writeEndElement();
        writer.writeStartElement("bannerFile");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBannerFilename()));
        writer.writeEndElement();

        writer.writeStartElement("artwork");
        for (ArtworkType artworkType : ArtworkType.values()) {
            Collection<Artwork> artworkList = movie.getArtwork(artworkType);
            if (artworkList.size() > 0) {
                for (Artwork artwork : artworkList) {
                    writer.writeStartElement(artworkType.toString());
                    writer.writeAttribute("count", "" + artworkList.size());

                    writer.writeStartElement("sourceSite");
                    writer.writeCharacters(artwork.getSourceSite());
                    writer.writeEndElement();

                    writer.writeStartElement("url");
                    writer.writeCharacters(artwork.getUrl());
                    writer.writeEndElement();

                    for (ArtworkFile artworkFile : artwork.getSizes()) {
                        writer.writeStartElement(artworkFile.getSize().toString());
                        writer.writeAttribute("downloaded", "" + artworkFile.isDownloaded());
                        writer.writeCharacters(artworkFile.getFilename());
                        writer.writeEndElement(); // size
                    }
                    writer.writeEndElement(); // artworkType
                }
            } else {
                // Write a dummy node
                writer.writeStartElement(artworkType.toString());
                writer.writeAttribute("count", "" + 0);

                writer.writeStartElement("url");
                writer.writeCharacters(Movie.UNKNOWN);
                writer.writeEndElement();

                writer.writeStartElement(ArtworkSize.LARGE.toString());
                writer.writeAttribute("downloaded", "false");
                writer.writeCharacters(Movie.UNKNOWN);
                writer.writeEndElement(); // size

                writer.writeEndElement(); // artworkType

            }
        }
        writer.writeEndElement(); // artwork

        writer.writeStartElement("plot");
        writer.writeCharacters(movie.getPlot());
        writer.writeEndElement();
        writer.writeStartElement("outline");
        writer.writeCharacters(movie.getOutline());
        writer.writeEndElement();
        writer.writeStartElement("quote");
        writer.writeCharacters(movie.getQuote());
        writer.writeEndElement();
        writer.writeStartElement("tagline");
        writer.writeCharacters(movie.getTagline());
        writer.writeEndElement();
        writer.writeStartElement("country");
        writeIndexAttribute(writer, library, "Country", movie.getCountry());
        writer.writeCharacters(movie.getCountry());
        writer.writeEndElement();
        writer.writeStartElement("company");
        writer.writeCharacters(movie.getCompany());
        writer.writeEndElement();
        writer.writeStartElement("runtime");
        writer.writeCharacters(movie.getRuntime());
        writer.writeEndElement();
        writer.writeStartElement("certification");
        writer.writeCharacters(Library.getIndexingCertification(movie.getCertification()));
        writer.writeEndElement();
        writer.writeStartElement("season");
        writer.writeCharacters(Integer.toString(movie.getSeason()));
        writer.writeEndElement();
        writer.writeStartElement("language");
        writer.writeCharacters(movie.getLanguage());
        writer.writeEndElement();
        writer.writeStartElement("subtitles");
        writer.writeCharacters(movie.getSubtitles());
        writer.writeEndElement();
        writer.writeStartElement("trailerExchange");
        writer.writeCharacters(movie.isTrailerExchange() ? "YES" : "NO");
        writer.writeEndElement();
        writer.writeStartElement("trailerLastScan");
        if (movie.getTrailerLastScan() == 0) {
            writer.writeCharacters(Movie.UNKNOWN);
        } else {
            writer.writeCharacters(Movie.dateFormat.format(movie.getTrailerLastScan()));
        }
        writer.writeEndElement();
        writer.writeStartElement("container");
        writer.writeCharacters(movie.getContainer());
        writer.writeEndElement(); // AVI, MKV, TS, etc.
        writer.writeStartElement("videoCodec");
        writer.writeCharacters(movie.getVideoCodec());
        writer.writeEndElement(); // DIVX, XVID, H.264, etc.
        writer.writeStartElement("audioCodec");
        writer.writeCharacters(movie.getAudioCodec());
        writer.writeEndElement(); // MP3, AC3, DTS, etc.
        writer.writeStartElement("audioChannels");
        writer.writeCharacters(movie.getAudioChannels());
        writer.writeEndElement(); // Number of audio channels
        writer.writeStartElement("resolution");
        writer.writeCharacters(movie.getResolution());
        writer.writeEndElement(); // 1280x528
        writer.writeStartElement("videoSource");
        // If the source is unknown, use the default source
        if (StringTools.isNotValidString(movie.getVideoSource())) {
            writer.writeCharacters(defaultSource);
        } else {
            writer.writeCharacters(movie.getVideoSource());
        }
        writer.writeEndElement();
        writer.writeStartElement("videoOutput");
        writer.writeCharacters(movie.getVideoOutput());
        writer.writeEndElement();
        writer.writeStartElement("aspect");
        writer.writeCharacters(movie.getAspectRatio());
        writer.writeEndElement();
        writer.writeStartElement("fps");
        writer.writeCharacters(Float.toString(movie.getFps()));
        writer.writeEndElement();
        writer.writeStartElement("fileDate");
        if (movie.getFileDate() == null) {
            writer.writeCharacters(Movie.UNKNOWN);
        } else {
            // Try to catch any date re-formatting errors
            try {
                writer.writeCharacters(Movie.dateFormat.format(movie.getFileDate()));
            } catch (ArrayIndexOutOfBoundsException error) {
                writer.writeCharacters(Movie.UNKNOWN);
            }
        }
        writer.writeEndElement();
        writer.writeStartElement("fileSize");
        writer.writeCharacters(movie.getFileSizeString());
        writer.writeEndElement();
        writer.writeStartElement("first");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getFirst()));
        writer.writeEndElement();
        writer.writeStartElement("previous");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getPrevious()));
        writer.writeEndElement();
        writer.writeStartElement("next");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getNext()));
        writer.writeEndElement();
        writer.writeStartElement("last");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getLast()));
        writer.writeEndElement();
        writer.writeStartElement("libraryDescription");
        writer.writeCharacters(movie.getLibraryDescription());
        writer.writeEndElement();
        writer.writeStartElement("prebuf");
        writer.writeCharacters(Long.toString(movie.getPrebuf()));
        writer.writeEndElement();

        if (movie.getGenres().size() > 0) {
            writer.writeStartElement("genres");
            for (String genre : movie.getGenres()) {
                writer.writeStartElement("genre");
                writeIndexAttribute(writer, library, "Genres", Library.getIndexingGenre(genre));
                writer.writeCharacters(genre);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        Collection<String> items = movie.getSetsKeys();
        if (items.size() > 0) {
            writer.writeStartElement("sets");
            for (String item : items) {
                writer.writeStartElement("set");
                Integer order = movie.getSetOrder(item);
                if (null != order) {
                    writer.writeAttribute("order", order.toString());
                }
                writeIndexAttribute(writer, library, "Set", item);
                writer.writeCharacters(item);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeStartElement("director");
        writeIndexAttribute(writer, library, "Director", movie.getDirector());
        writer.writeCharacters(movie.getDirector());
        writer.writeEndElement();

        writeElementSet(writer, "directors", "director", movie.getDirectors(), library, "Director");

        writeElementSet(writer, "writers", "writer", movie.getWriters(), library, "Writer");

        writeElementSet(writer, "cast", "actor", movie.getCast(), library, "Cast");

        // Issue 1901: Awards
        Collection<AwardEvent> awards = movie.getAwards();
        if (awards != null && awards.size() > 0) {
            writer.writeStartElement("awards");
            for (AwardEvent event : awards) {
                writer.writeStartElement("event");
                writer.writeAttribute("name", event.getName());
                for (Award award : event.getAwards()) {
                    writer.writeStartElement("award");
                    writer.writeAttribute("won", Integer.toString(award.getWon()));
                    writer.writeAttribute("nominated", Integer.toString(award.getNominated()));
                    writer.writeAttribute("year", Integer.toString(award.getYear()));
                    writer.writeCharacters(award.getName());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        // Issue 1897: Cast enhancement
        Collection<Filmography> people = movie.getPeople();
        if (people != null && people.size() > 0) {
            writer.writeStartElement("people");
            for (Filmography person : people) {
                writer.writeStartElement("person");
                writer.writeAttribute("name", person.getName());
                writer.writeAttribute("doublage", person.getDoublage());
                writer.writeAttribute("title", person.getTitle());
                writer.writeAttribute("character", person.getCharacter());
                writer.writeAttribute("job", person.getJob());
                writer.writeAttribute("id", person.getId());
                for (Map.Entry<String, String> personID : person.getIdMap().entrySet()) {
                    if (!personID.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                        writer.writeAttribute("id_" + personID.getKey(), personID.getValue());
                    }
                }
                writer.writeAttribute("department", person.getDepartment());
                writer.writeAttribute("url", person.getUrl());
                writer.writeAttribute("order", Integer.toString(person.getOrder()));
                writer.writeAttribute("cast_id", Integer.toString(person.getCastId()));
                writer.writeCharacters(person.getFilename());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        // Write the indexes that the movie belongs to
        writer.writeStartElement("indexes");
        String originalName = Movie.UNKNOWN;
        for (Entry<String, String> index : movie.getIndexes().entrySet()) {
            writer.writeStartElement("index");
            writer.writeAttribute("type", index.getKey());
            originalName = Library.getOriginalCategory(index.getKey());
            if (StringTools.isValidString(originalName)) {
                writer.writeAttribute("originalName", originalName);
            } else {
                writer.writeAttribute("originalName", index.getKey());
            }
            writer.writeAttribute("encoded", FileTools.makeSafeFilename(index.getValue()));
            writer.writeCharacters(index.getValue());
            writer.writeEndElement();
        }
        writer.writeEndElement();

        writer.writeStartElement("files");
        for (MovieFile mf : movie.getFiles()) {
            writer.writeStartElement("file");
            writer.writeAttribute("season", Integer.toString(mf.getSeason()));
            writer.writeAttribute("firstPart", Integer.toString(mf.getFirstPart()));
            writer.writeAttribute("lastPart", Integer.toString(mf.getLastPart()));
            writer.writeAttribute("title", mf.getTitle());
            writer.writeAttribute("subtitlesExchange", mf.isSubtitlesExchange() ? "YES" : "NO");
            // Fixes an issue with null file lengths
            try {
                if (mf.getFile() == null) {
                    writer.writeAttribute("size", "0");
                } else {
                    writer.writeAttribute("size", Long.toString(mf.getSize()));
                }
            } catch (Exception error) {
                logger.debug("XML Writer: File length error for file " + mf.getFilename());
                writer.writeAttribute("size", "0");
            }

            // Playlink values; can be empty, but not null
            for (Map.Entry<String, String> e : mf.getPlayLink().entrySet()) {
                writer.writeAttribute(e.getKey().toLowerCase(), e.getValue());
            }

            writer.writeAttribute("watched", mf.isWatched()?"true":"false");

            if (mf.getFile() != null) {
                writer.writeStartElement("fileLocation");
                writer.writeCharacters(mf.getFile().getAbsolutePath());
                writer.writeEndElement();
            }

            writer.writeStartElement("fileURL");
            String filename = mf.getFilename();
            // Issue 1237: Add "VIDEO_TS.IFO" for PlayOnHD VIDEO_TS path names
            if (isPlayOnHD) {
                if (filename.toUpperCase().endsWith("VIDEO_TS")) {
                    filename = filename + "/VIDEO_TS.IFO";
                }
            }
            writer.writeCharacters(filename); // should already be a URL
            writer.writeEndElement();

            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); ++part) {
                writer.writeStartElement("fileTitle");
                writer.writeAttribute("part", Integer.toString(part));
                writer.writeCharacters(mf.getTitle(part));
                writer.writeEndElement();

                // Only write out these for TV Shows
                if (movie.isTVShow()) {
                    writer.writeStartElement("airsInfo");
                    writer.writeAttribute("part", Integer.toString(part));
                    writer.writeAttribute("afterSeason", mf.getAirsAfterSeason(part));
                    writer.writeAttribute("beforeSeason", mf.getAirsBeforeSeason(part));
                    writer.writeAttribute("beforeEpisode", mf.getAirsBeforeEpisode(part));
                    writer.writeCharacters(Integer.toString(part)); // Just write the part out. Is there something better?
                    writer.writeEndElement();
                    
                    writer.writeStartElement("firstAired");
                    writer.writeAttribute("part", Integer.toString(part));
                    writer.writeCharacters(mf.getFirstAired(part));
                    writer.writeEndElement();
                }
                
                if (StringTools.isValidString(mf.getWatchedDateString())) {
                    writer.writeStartElement("watchedDate");
                    writer.writeCharacters(mf.getWatchedDateString());
                    writer.writeEndElement();
                }

                if (includeEpisodePlots) {
                    writer.writeStartElement("filePlot");
                    writer.writeAttribute("part", Integer.toString(part));
                    writer.writeCharacters(mf.getPlot(part));
                    writer.writeEndElement();
                }

                if (includeVideoImages) {
                    writer.writeStartElement("fileImageURL");
                    writer.writeAttribute("part", Integer.toString(part));
                    writer.writeCharacters(HTMLTools.encodeUrl(mf.getVideoImageURL(part)));
                    writer.writeEndElement();

                    writer.writeStartElement("fileImageFile");
                    writer.writeAttribute("part", Integer.toString(part));
                    writer.writeCharacters(HTMLTools.encodeUrl(mf.getVideoImageFilename(part)));
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();

        Collection<ExtraFile> extraFiles = movie.getExtraFiles();
        if (extraFiles != null && extraFiles.size() > 0) {
            writer.writeStartElement("extras");
            for (ExtraFile ef : extraFiles) {
                writer.writeStartElement("extra");
                writer.writeAttribute("title", ef.getTitle());
                if (ef.getPlayLink() != null) {
                    // Playlink values
                    for (Map.Entry<String, String> e : ef.getPlayLink().entrySet()) {
                        writer.writeAttribute(e.getKey().toLowerCase(), e.getValue());
                    }
                }
                writer.writeCharacters(ef.getFilename()); // should already be URL-encoded
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    /**
     * Persist a movie into an XML file. Doesn't overwrite an already existing XML file for the specified movie unless, movie's data has changed or
     * forceXMLOverwrite is true.
     */
    public void writeMovieXML(Jukebox jukebox, Movie movie, Library library) throws FileNotFoundException, XMLStreamException {
        String baseName = movie.getBaseName();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + ".xml");
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + baseName + ".xml");

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty(Movie.DIRTY_INFO)) {

            XMLWriter writer = new XMLWriter(tempXmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("details");
            writeMovie(writer, movie, library);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();

            if (writeNfoFiles) {
                writeNfoFile(jukebox, movie);
            }
        }
    }

    private void writePerson(XMLWriter writer, Person person, Library library) throws XMLStreamException {
        writer.writeStartElement("person");

        for (Map.Entry<String, String> e : person.getIdMap().entrySet()) {
            writer.writeStartElement("id");
            writer.writeAttribute("persondb", e.getKey());
            writer.writeCharacters(e.getValue());
            writer.writeEndElement();
        }

        writer.writeStartElement("name");
        writer.writeCharacters(person.getName());
        writer.writeEndElement();

        writer.writeStartElement("title");
        writer.writeCharacters(person.getTitle());
        writer.writeEndElement();

        writer.writeStartElement("baseFilename");
        writer.writeCharacters(person.getFilename());
        writer.writeEndElement();

        if (person.getAka().size() > 0) {
            writer.writeStartElement("aka");
            for (String aka : person.getAka()) {
                writer.writeStartElement("name");
                writer.writeCharacters(aka);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeStartElement("biography");
        writer.writeCharacters(person.getBiography());
        writer.writeEndElement();

        writer.writeStartElement("birthday");
        writer.writeCharacters(person.getYear());
        writer.writeEndElement();

        writer.writeStartElement("birthplace");
        writer.writeCharacters(person.getBirthPlace());
        writer.writeEndElement();

        writer.writeStartElement("url");
        writer.writeCharacters(person.getUrl());
        writer.writeEndElement();

        writer.writeStartElement("photoFile");
        writer.writeCharacters(person.getPhotoFilename());
        writer.writeEndElement();

        writer.writeStartElement("photoURL");
        writer.writeCharacters(person.getPhotoURL());
        writer.writeEndElement();

        writer.writeStartElement("knownMovies");
        writer.writeCharacters(Integer.toString(person.getKnownMovies()));
        writer.writeEndElement();

        writer.writeStartElement("filmography");
        for (Filmography film : person.getFilmography()) {
            writer.writeStartElement("movie");
            writer.writeAttribute("id", film.getId());
            for (Map.Entry<String, String> e : film.getIdMap().entrySet()) {
                if (!e.getKey().equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
                    writer.writeAttribute("id_" + e.getKey(), e.getValue());
                }
            }
            writer.writeAttribute("name", film.getName());
            writer.writeAttribute("title", film.getTitle());
            writer.writeAttribute("originalTitle", film.getOriginalTitle());
            writer.writeAttribute("year", film.getYear());
            writer.writeAttribute("rating", film.getRating());
            writer.writeAttribute("character", film.getCharacter());
            writer.writeAttribute("job", film.getJob());
            writer.writeAttribute("department", film.getDepartment());
            writer.writeAttribute("url", film.getUrl());
            writer.writeCharacters(film.getFilename());
            writer.writeEndElement();
        }
        writer.writeEndElement();

        writer.writeStartElement("version");
        writer.writeCharacters(Integer.toString(person.getVersion()));
        writer.writeEndElement();

        writer.writeStartElement("lastModifiedAt");
        writer.writeCharacters(person.getLastModifiedAt());
        writer.writeEndElement();

        writer.writeEndElement();
    }

    public void writePersonXML(Jukebox jukebox, Person person, Library library) throws FileNotFoundException, XMLStreamException {
        String baseName = person.getFilename();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + baseName + ".xml");
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + baseName + ".xml");
        finalXmlFile.getParentFile().mkdirs();
        tempXmlFile.getParentFile().mkdirs();

        FileTools.addJukeboxFile(finalXmlFile.getName());

        if (!finalXmlFile.exists() || forceXMLOverwrite || person.isDirty()) {

            XMLWriter writer = new XMLWriter(tempXmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("details");
            writePerson(writer, person, library);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        }
    }

    /**
     * Write a NFO file for the movie using the data gathered
     * @param jukebox
     * @param movie
     */
    public void writeNfoFile(Jukebox jukebox, Movie movie) {
        // Don't write NFO files for sets or extras
        if (movie.isSetMaster() || movie.isExtra()) {
            return;
        }

        String nfoFolder = StringTools.appendToPath(jukebox.getJukeboxTempLocationDetails(), "NFO");
        (new File(nfoFolder)).mkdirs();
        //File rootNfoFile = FileTools.fileCache.getFile(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), nfoBaseName));
        File tempNfoFile = new File(StringTools.appendToPath(nfoFolder, movie.getBaseName() + ".nfo"));

        logger.debug("MovieJukeboxXMLWriter: Writing NFO file for " + movie.getBaseName() + ".nfo");
        FileTools.addJukeboxFile(tempNfoFile.getName());

        XMLWriter writer = null;

        try {
            writer = new XMLWriter(tempNfoFile);

            writer.writeStartDocument("UTF-8", "1.0");
            if (movie.isTVShow()) {
                writer.writeStartElement("tvshow");
            } else {
                writer.writeStartElement("movie");
            }
            for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
                writer.writeStartElement("id");
                writer.writeAttribute("moviedb", e.getKey());
                writer.writeCharacters(e.getValue());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getTitle())) {
                writer.writeStartElement("title");
                writer.writeCharacters(movie.getTitle());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getOriginalTitle())) {
                writer.writeStartElement("originaltitle");
                writer.writeCharacters(movie.getOriginalTitle());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getTitleSort())) {
                writer.writeStartElement("sorttitle");
                writer.writeCharacters(movie.getTitleSort());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getYear())) {
                writer.writeStartElement("year");
                writer.writeCharacters(movie.getYear());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getOutline())) {
                writer.writeStartElement("outline");
                writer.writeCharacters(movie.getOutline());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getPlot())) {
                writer.writeStartElement("plot");
                writer.writeCharacters(movie.getPlot());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getTagline())) {
                writer.writeStartElement("tagline");
                writer.writeCharacters(movie.getTagline());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getRuntime())) {
                writer.writeStartElement("runtime");
                writer.writeCharacters(movie.getRuntime());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getReleaseDate())) {
                writer.writeStartElement("premiered");
                writer.writeCharacters(movie.getReleaseDate());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getShowStatus())) {
                writer.writeStartElement("showStatus");
                writer.writeCharacters(movie.getReleaseDate());
                writer.writeEndElement();
            }

            if (movie.getRating() >= 0) {
                writer.writeStartElement("rating");
                writer.writeCharacters(Integer.toString(movie.getRating()));
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getCertification())) {
                if (extractCertificationFromMPAA) {
                    writer.writeStartElement("mpaa");
                } else {
                    writer.writeStartElement("certification");
                }
                writer.writeCharacters(movie.getCertification());
                writer.writeEndElement();
            }

            if (!movie.getGenres().isEmpty()) {
                for (String genre : movie.getGenres()) {
                    writer.writeStartElement("genre");
                    writer.writeCharacters(genre);
                    writer.writeEndElement();
                }
            }

            if (!movie.getWriters().isEmpty()) {
                writer.writeStartElement("credits");
                for (String writerCredit : movie.getWriters()) {
                    writer.writeStartElement("writer");
                    writer.writeCharacters(writerCredit);
                    writer.writeEndElement();
                }
                writer.writeEndElement(); // credits
            }

            if (!movie.getDirectors().isEmpty()) {
                writer.writeStartElement("directors");
                for (String director : movie.getDirectors()) {
                    writer.writeStartElement("director");
                    writer.writeCharacters(director);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getCompany())) {
                writer.writeStartElement("company");
                writer.writeCharacters(movie.getCompany());
                writer.writeEndElement();
            }

            if (StringTools.isValidString(movie.getCountry())) {
                writer.writeStartElement("country");
                writer.writeCharacters(movie.getCountry());
                writer.writeEndElement();
            }

            if (!movie.getCast().isEmpty()) {
                writer.writeStartElement("actor");
                for (String actor : movie.getCast()) {
                    writer.writeStartElement("name");
                    writer.writeCharacters(actor);
                    writer.writeEndElement();
                    writer.writeStartElement("role");
                    writer.writeCharacters("");
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndDocument();
        } catch (Exception ignore) {
            logger.debug("MovieJukeboxXMLWriter: Error creating the NFO file: " + movie.getBaseName() + ".nfo");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}