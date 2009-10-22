/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.XMLWriter;

/**
 * Parse/Write XML files for movie details and library indexes
 * 
 * @author Julien
 */
public class MovieJukeboxXMLWriter {

    private boolean forceXMLOverwrite;
    private int nbMoviesPerPage;
    private int nbMoviesPerLine;
    private int nbTvShowsPerPage;
    private int nbTvShowsPerLine;
    private boolean fullMovieInfoInIndexes;
    private boolean includeMoviesInCategories;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private static String indexFile = "../" + PropertiesUtil.getProperty("mjb.indexFile", "index.htm");
    private static String str_categoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.displayList", "");
    private static List<String> categoriesDisplayList = Collections.emptyList();
    private static int categoriesMinCount = Integer.parseInt(PropertiesUtil.getProperty("mjb.categories.minCount", "3"));
    private static Logger logger = Logger.getLogger("moviejukebox");

    static {
        if (str_categoriesDisplayList.length() == 0) {
            str_categoriesDisplayList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Rating,Year,Library,Set");
        }
        categoriesDisplayList = Arrays.asList(str_categoriesDisplayList.split(","));
    }

    public MovieJukeboxXMLWriter() {
        forceXMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceXMLOverwrite", "false"));
        nbMoviesPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerPage", "10"));
        nbMoviesPerLine = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerLine", "5"));
        nbTvShowsPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbTvThumbnailsPerPage", "0")); // If 0 then use the Movies setting
        nbTvShowsPerLine = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbTvThumbnailsPerLine", "0")); // If 0 then use the Movies setting
        fullMovieInfoInIndexes = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.fullMovieInfoInIndexes", "false"));
        includeMoviesInCategories = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeMoviesInCategories", "false"));
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
        includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
        
        if (nbTvShowsPerPage == 0) {
            nbTvShowsPerPage = nbMoviesPerPage;
        }
        
        if (nbTvShowsPerLine == 0) {
            nbTvShowsPerLine = nbMoviesPerLine;
        }
    }

    /**
     * Parse a single movie detail xml file
     */
    @SuppressWarnings("unchecked")
    public boolean parseMovieXML(File xmlFile, Movie movie) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(new FileInputStream(xmlFile), "UTF-8");

            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                String tag = e.toString();

                if (tag.toLowerCase().startsWith("<id ")) {
                    String movieDatabase = ImdbPlugin.IMDB_PLUGIN_ID;
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("movieDatabase")) {
                            movieDatabase = attr.getValue();
                            continue;
                        }
                    }
                    movie.setId(movieDatabase, parseCData(r));
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
                if (tag.equalsIgnoreCase("<rating>")) {
                    movie.setRating(Integer.parseInt(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<top250>")) {
                    movie.setTop250(Integer.parseInt(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<posterURL>")) {
                    movie.setPosterURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<posterSubimage>")) {
                    movie.setPosterSubimage(parseCData(r));
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
                if (tag.toLowerCase().startsWith("<director ") || tag.equalsIgnoreCase("<director>")) {
                    movie.setDirector(parseCData(r));
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
                if (tag.equalsIgnoreCase("<certification>")) {
                    movie.setCertification(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<season>")) {
                    movie.setSeason(Integer.parseInt(parseCData(r)));
                }
                if (tag.equalsIgnoreCase("<language>")) {
                    movie.setLanguage(parseCData(r));
                }
                if (tag.equalsIgnoreCase("<subtitles>")) {
                    movie.setSubtitles(parseCData(r).equalsIgnoreCase("YES"));
                }
                if (tag.equalsIgnoreCase("<trailerExchange>")) {
                    movie.setTrailerExchange(parseCData(r).equalsIgnoreCase("YES"));
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
                        }
                    }
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

                        if (ns.equalsIgnoreCase("firstPart")) {
                            mf.setPart(Integer.parseInt(attr.getValue()));
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
                    }

                    while (!r.peek().toString().equalsIgnoreCase("</file>")) {
                        e = r.nextEvent();
                        tag = e.toString();
                        if (tag.equalsIgnoreCase("<fileURL>")) {
                            mf.setFilename(parseCData(r));
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
                            mf.setVideoImageURL(part, parseCData(r));
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
                            mf.setVideoImageFilename(part, parseCData(r));
                        }
                    }
                    // add or replace MovieFile based on XML data
                    movie.addMovieFile(mf);
                }

                if (tag.toLowerCase().startsWith("<extra ")) {
                    ExtraFile ef = new ExtraFile();
                    ef.setNewFile(false);

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if (ns.equalsIgnoreCase("title")) {
                            ef.setTitle(attr.getValue());
                            continue;
                        }
                    }

                    ef.setFilename(parseCData(r));
                    // add or replace extra based on XML data
                    movie.addExtraFile(ef);
                }
            }
        } catch (Exception error) {
            logger.severe("Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return false;
        }

        movie.setDirty(movie.hasNewMovieFiles() || movie.hasNewExtraFiles());
        return true;
    }

    private String parseCData(XMLEventReader r) throws XMLStreamException {
        StringBuffer sb = new StringBuffer();
        XMLEvent e;
        while ((e = r.nextEvent()) instanceof Characters) {
            sb.append(e.toString());
        }
        return HTMLTools.decodeHtml(sb.toString());
    }

    public void writeCategoryXML(String rootPath, String detailsDirName, Library library) throws FileNotFoundException, XMLStreamException {
        File folder = new File(rootPath, detailsDirName);
        folder.mkdirs();

        File xmlFile = new File(folder, "Categories.xml");

        XMLWriter writer = new XMLWriter(xmlFile);

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("library");

        List<Movie> allMovies = library.getMoviesList();

        if (includeMoviesInCategories) {
            for (Movie movie : library.getMoviesList()) {
                if (fullMovieInfoInIndexes) {
                    writeMovie(writer, movie, library);
                } else {
                    writeMovieForIndex(writer, movie);
                }
            }
        }

        for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            if (category.getValue().isEmpty() || !categoriesDisplayList.contains(category.getKey())) {
                continue;
            }

            writer.writeStartElement("category");
            writer.writeAttribute("name", category.getKey());

            for (Map.Entry<String, List<Movie>> index : category.getValue().entrySet()) {
                List<Movie> value = index.getValue();
                logger.finest("Index: " + category.getKey() + ", Category: " + index.getKey() + ", count: " + value.size());
                if (value.size() < categoriesMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(category.getKey())) {
                    logger.finest("Category " + category.getKey() + " " + index.getKey() + " does not contain enough movies, not adding to categories.xml");
                    continue;
                }

                String key = index.getKey();
                String indexFilename = FileTools.makeSafeFilename(FileTools.createPrefix(category.getKey(), key)) + "1";

                writer.writeStartElement("index");
                writer.writeAttribute("name", key);

                if (includeMoviesInCategories) {
                    writer.writeAttribute("filename", indexFilename);

                    for (Movie movie : value) {
                        writer.writeStartElement("movie");
                        writer.writeCharacters(Integer.toString(allMovies.indexOf(movie)));
                        writer.writeEndElement();
                    }
                } else {
                    writer.writeCharacters(indexFilename);
                }

                writer.writeEndElement();
            }
            writer.writeEndElement(); // category
        }
        writer.writeEndElement(); // library
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    /**
     * Write the set of index XML files for the library
     * @throws Throwable 
     */
    public void writeIndexXML(final String rootPath, String detailsDirName, final Library library, int threadcount) throws Throwable {

        ThreadExecutor<Void> tasks = new ThreadExecutor<Void>(threadcount);
        for (final Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            tasks.submit(new Callable<Void>() {
                public Void call() throws XMLStreamException, FileNotFoundException{

                    String categoryName = category.getKey();
                    Map<String, List<Movie>> index = category.getValue();

                    for (Map.Entry<String, List<Movie>> group : index.entrySet()) {
                        List<Movie> movies = group.getValue();

                        // This is horrible! Issue 735 will get rid of it.
                        if (movies.size() < categoriesMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryName)) {
                            logger.finer("Category " + categoryName + " " + group.getKey() + " does not contain enough movies, skipping XML generation.");
                            continue;
                        }

                        String key = FileTools.createCategoryKey(group.getKey());

                        int nbVideosPerPage;

                        if (key.equalsIgnoreCase("TV Shows")) {
                            nbVideosPerPage = nbTvShowsPerPage;
                        } else {
                            nbVideosPerPage = nbMoviesPerPage;
                        }

                        int previous = 1;
                        int current = 1;
                        int last = 1 + (movies.size() - 1) / nbVideosPerPage;
                        int next = Math.min(2, last);
                        int nbMoviesLeft = nbVideosPerPage;

                        List<Movie> moviesInASinglePage = new ArrayList<Movie>();
                        for (Movie movie : movies) {
                            moviesInASinglePage.add(movie);
                            nbMoviesLeft--;

                            if (nbMoviesLeft == 0) {
                                if (current == 1) {
                                    // If this is the first page, link the previous page to the last page.
                                    writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, last, current, next, last);
                                } else {
                                    // This is a "middle" page, so process as normal.
                                    writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, next, last);
                                }
                                // */writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, next, last);
                                moviesInASinglePage = new ArrayList<Movie>();
                                previous = current;
                                current = Math.min(current + 1, last);
                                next = Math.min(current + 1, last);
                                nbMoviesLeft = nbVideosPerPage;
                            }
                        }

                        if (moviesInASinglePage.size() > 0) {
                            writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, 1, last);
                        }
                    }
                    return null;
                }
            });
        }
        tasks.waitFor();
    }

    public void writeIndexPage(Library library, Collection<Movie> movies, String rootPath, String categoryName, String key, int previous, int current,
                    int next, int last) throws FileNotFoundException, XMLStreamException {
        String prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, key));
        File xmlFile = null;
        XMLWriter writer = null;
        
        // Is this a TV Show index?
        int nbVideosPerPage, nbVideosPerLine;
        
        if (key.equalsIgnoreCase("TV Shows")) {
            nbVideosPerPage = nbTvShowsPerPage;
            nbVideosPerLine = nbTvShowsPerLine;
        } else {
            nbVideosPerPage = nbMoviesPerPage;
            nbVideosPerLine = nbMoviesPerLine;
        }
        
        try {
            xmlFile = new File(rootPath, prefix + current + ".xml");
            xmlFile.getParentFile().mkdirs();        	

            writer = new XMLWriter(xmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("library");

            for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
                String categoryKey = category.getKey();
                Map<String, List<Movie>> index = category.getValue();
                writer.writeStartElement("category");
                writer.writeAttribute("name", categoryKey);
                if (categoryKey.equalsIgnoreCase(categoryName)) {
                    writer.writeAttribute("current", "true");
                }

                for (String akey : index.keySet()) {
                    String encakey = FileTools.createCategoryKey(akey);

                    // This is horrible! Issue 735 will get rid of it.
                    if (index.get(akey).size() < categoriesMinCount && !Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryKey)) {
                        continue;
                    }

                    prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, encakey));

                    writer.writeStartElement("index");
                    writer.writeAttribute("name", akey);

                    // if currently writing this page then add current attribute with value true
                    if (encakey.equalsIgnoreCase(key)) {
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

                writer.writeEndElement(); // categories
            }
            writer.writeStartElement("movies");
            writer.writeAttribute("count", "" + nbVideosPerPage);
            writer.writeAttribute("cols", "" + nbVideosPerLine);

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
        	
        } finally {
            writer.flush();
            writer.close();
        }
        
        return;
    }

    private void writeMovieForIndex(XMLWriter writer, Movie movie) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isExtra", Boolean.toString(movie.isExtra()));
        writer.writeAttribute("isSet", Boolean.toString(movie.isSetMaster()));
        writer.writeStartElement("details");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getBaseName())) + ".html");
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
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getDetailPosterFilename())));
        writer.writeEndElement();
        writer.writeStartElement("thumbnail");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getThumbnailFilename())));
        writer.writeEndElement();
        writer.writeStartElement("bannerFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getBannerFilename())));
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

    private void writeIndexAttribute(XMLWriter writer, Library l, String cat, String val) throws XMLStreamException {
        Library.Index i = l.getIndexes().get(cat);
        if (null != i) {
            List<Movie> ml = i.get(val);
            if (null != ml && ml.size() >= categoriesMinCount) {
                writer.writeAttribute("index", HTMLTools.encodeUrl(FileTools.makeSafeFilename(FileTools.createPrefix(cat, val)) + 1));
            }
        }
    }

    private void writeMovie(XMLWriter writer, Movie movie, Library library) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isExtra", Boolean.toString(movie.isExtra()));
        writer.writeAttribute("isSet", Boolean.toString(movie.isSetMaster()));

        for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
            writer.writeStartElement("id");
            writer.writeAttribute("movieDatabase", e.getKey());
            writer.writeCharacters(e.getValue());
            writer.writeEndElement();
        }
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
        writer.writeStartElement("rating");
        writer.writeCharacters(Integer.toString(movie.getRating()));
        writer.writeEndElement();
        writer.writeStartElement("top250");
        writer.writeCharacters(Integer.toString(movie.getTop250()));
        writer.writeEndElement();
        writer.writeStartElement("details");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getBaseName())) + ".html");
        writer.writeEndElement();
        writer.writeStartElement("posterURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getPosterURL()));
        writer.writeEndElement();
        writer.writeStartElement("posterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getPosterFilename())));
        writer.writeEndElement();
        writer.writeStartElement("posterSubimage");
        writer.writeCharacters(movie.getPosterSubimage());
        writer.writeEndElement();
        writer.writeStartElement("fanartURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getFanartURL()));
        writer.writeEndElement();
        writer.writeStartElement("fanartFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getFanartFilename())));
        writer.writeEndElement();
        writer.writeStartElement("detailPosterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getDetailPosterFilename())));
        writer.writeEndElement();
        writer.writeStartElement("thumbnail");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getThumbnailFilename())));
        writer.writeEndElement();
        writer.writeStartElement("bannerURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getBannerURL()));
        writer.writeEndElement();
        writer.writeStartElement("bannerFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getBannerFilename())));
        writer.writeEndElement();
        writer.writeStartElement("plot");
        writer.writeCharacters(movie.getPlot());
        writer.writeEndElement();
        writer.writeStartElement("outline");
        writer.writeCharacters(movie.getOutline());
        writer.writeEndElement();
        writer.writeStartElement("director");
        writeIndexAttribute(writer, library, "Director", movie.getDirector());
        writer.writeCharacters(movie.getDirector());
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
        writer.writeCharacters(movie.getCertification());
        writer.writeEndElement();
        writer.writeStartElement("season");
        writer.writeCharacters(Integer.toString(movie.getSeason()));
        writer.writeEndElement();
        writer.writeStartElement("language");
        writer.writeCharacters(movie.getLanguage());
        writer.writeEndElement();
        writer.writeStartElement("subtitles");
        writer.writeCharacters(movie.hasSubtitles() ? "YES" : "NO");
        writer.writeEndElement();
        writer.writeStartElement("trailerExchange");
        writer.writeCharacters(movie.isTrailerExchange() ? "YES" : "NO");
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
        writer.writeCharacters(movie.getVideoSource());
        writer.writeEndElement();
        writer.writeStartElement("videoOutput");
        writer.writeCharacters(movie.getVideoOutput());
        writer.writeEndElement();
        writer.writeStartElement("fps");
        writer.writeCharacters(Float.toString(movie.getFps()));
        writer.writeEndElement();
        writer.writeStartElement("first");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getFirst())));
        writer.writeEndElement();
        writer.writeStartElement("previous");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getPrevious())));
        writer.writeEndElement();
        writer.writeStartElement("next");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getNext())));
        writer.writeEndElement();
        writer.writeStartElement("last");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getLast())));
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
        Collection<String> items = movie.getSets();
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
        writeElementSet(writer, "cast", "actor", movie.getCast(), library, "Cast");

        writeElementSet(writer, "writers", "writer", movie.getWriters(), library, "Writers");

        writer.writeStartElement("files");
        for (MovieFile mf : movie.getFiles()) {
            writer.writeStartElement("file");
            writer.writeAttribute("firstPart", Integer.toString(mf.getFirstPart()));
            writer.writeAttribute("lastPart", Integer.toString(mf.getLastPart()));
            writer.writeAttribute("title", mf.getTitle());
            writer.writeAttribute("subtitlesExchange", mf.isSubtitlesExchange() ? "YES" : "NO");
            writer.writeStartElement("fileURL");
            writer.writeCharacters(mf.getFilename()); // should already be a URL
            writer.writeEndElement();

            if (includeEpisodePlots || includeVideoImages) {
                for (int part = mf.getFirstPart(); part <= mf.getLastPart(); ++part) {
                    if (includeEpisodePlots) {
                        writer.writeStartElement("filePlot");
                        writer.writeAttribute("part", Integer.toString(part));
                        writer.writeCharacters(mf.getPlot(part));
                        writer.writeEndElement();
                    }
                    if (includeVideoImages) {
                        writer.writeStartElement("fileImageURL");
                        writer.writeAttribute("part", Integer.toString(part));
                        writer.writeCharacters(mf.getVideoImageURL(part));
                        writer.writeEndElement();

                        writer.writeStartElement("fileImageFile");
                        writer.writeAttribute("part", Integer.toString(part));
                        writer.writeCharacters(mf.getVideoImageFilename(part));
                        writer.writeEndElement();
                    }
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
                writer.writeCharacters(ef.getFilename()); // should already be URL-encoded
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeVariable(XMLWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement("xsl:variable");
        writer.writeAttribute("name", name);
        writer.writeCharacters(value);
        writer.writeEndElement();

    }

    public void writePreferences(String rootPath) throws XMLStreamException {
        File xslFile = new File(PropertiesUtil.getProperty("mjb.skin.dir"), "preferences.xsl");
        XMLWriter writer = new XMLWriter(xslFile);

        writer.writeStartDocument("UTF-8", "1.0");

        writer.writeStartElement("xsl:stylesheet");
        writer.writeAttribute("version", "1.0");
        writer.writeAttribute("xmlns:xsl", "http://www.w3.org/1999/XSL/Transform");

        writer.writeStartElement("xsl:output");
        writer.writeAttribute("method", "xml");
        writer.writeAttribute("omit-xml-declaration", "yes");
        writer.writeEndElement();

        // Issue 436: Bounce off the index.htm to get to the real homePage
        // since homePage is now calculated by the Library.
        writeVariable(writer, "homePage", indexFile);

        // Issue 310
        writeVariable(writer, "rootPath", new File(rootPath).getAbsolutePath().replace('\\', '/'));

        // Issue 309
        for (Map.Entry<Object, Object> entry : PropertiesUtil.getEntrySet()) {
            writeVariable(writer, (String)entry.getKey(), (String)entry.getValue());
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    /**
     * Persist a movie into an XML file. Doesn't overwrite an already existing XML file for the specified movie unless, movie's data has changed or
     * forceXMLOverwrite is true.
     */
    public void writeMovieXML(String rootPath, String tempRootPath, Movie movie, Library library) throws FileNotFoundException, XMLStreamException {
        String baseName = FileTools.makeSafeFilename(movie.getBaseName());
        File finalXmlFile = new File(rootPath + File.separator + baseName + ".xml");
        File tempXmlFile = new File(tempRootPath + File.separator + baseName + ".xml");
        tempXmlFile.getParentFile().mkdirs();

        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty()) {

            XMLWriter writer = new XMLWriter(tempXmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("details");
            writeMovie(writer, movie, library);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }
    }
}
