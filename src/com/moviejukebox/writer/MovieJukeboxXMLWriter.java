package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.TrailerFile;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
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
    private boolean fullMovieInfoInIndexes;
    private boolean includeMoviesInCategories;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger("moviejukebox");

    public MovieJukeboxXMLWriter() {
        forceXMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceXMLOverwrite", "false"));
        nbMoviesPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerPage", "10"));
        nbMoviesPerLine = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerLine", "5"));
        fullMovieInfoInIndexes = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.fullMovieInfoInIndexes", "false"));
        includeMoviesInCategories = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeMoviesInCategories", "false"));
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
        includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
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

                if (tag.startsWith("<id ")) {
                    String movieDatabase = ImdbPlugin.IMDB_PLUGIN_ID;
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if ("movieDatabase".equals(ns)) {
                            movieDatabase = attr.getValue();
                            continue;
                        }
                    }
                    movie.setId(movieDatabase, parseCData(r));
                }
                if (tag.equals("<baseFilename>") && (movie.getBaseName() == null || movie.getBaseName() == Movie.UNKNOWN)) {
                    movie.setBaseName(parseCData(r));
                }
                if (tag.equals("<title>")) {
                    movie.setTitle(parseCData(r));
                }
                if (tag.equals("<titleSort>")) {
                    movie.setTitleSort(parseCData(r));
                }
                if (tag.equals("<originalTitle>")) {
                    movie.setOriginalTitle(parseCData(r));
                }
                if (tag.equals("<year>")) {
                    movie.setYear(parseCData(r));
                }
                if (tag.equals("<releaseDate>")) {
                    movie.setReleaseDate(parseCData(r));
                }
                if (tag.equals("<rating>")) {
                    movie.setRating(Integer.parseInt(parseCData(r)));
                }
                if (tag.equals("<top250>")) {
                    movie.setTop250(Integer.parseInt(parseCData(r)));
                }
                if (tag.equals("<posterURL>")) {
                    movie.setPosterURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<posterSubimage>")) {
                    movie.setPosterSubimage(parseCData(r));
                }
                if (tag.equals("<fanartURL>")) {
                    movie.setFanartURL(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<posterFile>")) {
                    movie.setPosterFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<detailPosterFile>")) {
                    movie.setDetailPosterFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<thumbnail>")) {
                    movie.setThumbnailFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<fanartFile>")) {
                    movie.setFanartFilename(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<plot>")) {
                    movie.setPlot(parseCData(r));
                }
                if (tag.equals("<outline>")) {
                    movie.setOutline(parseCData(r));
                }
                if (tag.equals("<director>")) {
                    movie.setDirector(parseCData(r));
                }
                if (tag.equals("<country>")) {
                    movie.setCountry(parseCData(r));
                }
                if (tag.equals("<company>")) {
                    movie.setCompany(parseCData(r));
                }
                if (tag.equals("<runtime>")) {
                    movie.setRuntime(parseCData(r));
                }
                if (tag.equals("<genre>")) {
                    movie.addGenre(parseCData(r));
                }
                if (tag.startsWith("<set ") || tag.equals("<set>")) {
// String set = null;
                    Integer order = null;
                    
                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if ("order".equals(ns)) {
                            order = Integer.parseInt(attr.getValue());
                            continue;
                        }
                    }
                        
                    movie.addSet(parseCData(r), order);
                }
                if (tag.equals("<actor>")) {
                    movie.addActor(parseCData(r));
                }
                if (tag.equals("<certification>")) {
                    movie.setCertification(parseCData(r));
                }
                if (tag.equals("<season>")) {
                    movie.setSeason(Integer.parseInt(parseCData(r)));
                }
                if (tag.equals("<language>")) {
                    movie.setLanguage(parseCData(r));
                }
                if (tag.equals("<subtitles>")) {
                    movie.setSubtitles(parseCData(r).equalsIgnoreCase("YES"));
                }
                if (tag.equals("<trailerExchange>")) {
                    movie.setTrailerExchange(parseCData(r).equalsIgnoreCase("YES"));
                }
                if (tag.equals("<container>")) {
                    movie.setContainer(parseCData(r));
                }
                if (tag.equals("<videoCodec>")) {
                    movie.setVideoCodec(parseCData(r));
                }
                if (tag.equals("<audioCodec>")) {
                    movie.setAudioCodec(parseCData(r));
                }
                if (tag.equals("<audioChannels>")) {
                    movie.setAudioChannels(parseCData(r));
                }
                if (tag.equals("<resolution>")) {
                    movie.setResolution(parseCData(r));
                }
                if (tag.equals("<videoSource>")) {
                    movie.setVideoSource(parseCData(r));
                }
                if (tag.equals("<videoOutput>")) {
                    movie.setVideoOutput(parseCData(r));
                }
                if (tag.equals("<fps>")) {
                    movie.setFps(Float.parseFloat(parseCData(r)));
                }
                if (tag.equals("<first>")) {
                    movie.setFirst(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<previous>")) {
                    movie.setPrevious(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<next>")) {
                    movie.setNext(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<last>")) {
                    movie.setLast(HTMLTools.decodeUrl(parseCData(r)));
                }
                if (tag.equals("<libraryDescription>")) {
                    movie.setLibraryDescription(parseCData(r));
                }
                if (tag.equals("<prebuf>")) {
                    String prebuf = parseCData(r);
                    if (prebuf != null) {
                        try {
                            movie.setPrebuf(Long.parseLong(prebuf));
                        } catch (Exception ignore) {
                        }
                    }
                }

                if (tag.startsWith("<file ")) {
                    MovieFile mf = new MovieFile();
                    mf.setNewFile(false);

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if ("title".equals(ns)) {
                            mf.setTitle(attr.getValue());
                            continue;
                        }

                        if ("firstPart".equals(ns)) {
                            mf.setPart(Integer.parseInt(attr.getValue()));
                            continue;
                        }

                        if ("lastPart".equals(ns)) {
                            mf.setLastPart(Integer.parseInt(attr.getValue()));
                            continue;
                        }

                        if ("subtitlesExchange".equals(ns)) {
                            mf.setSubtitlesExchange(attr.getValue().equalsIgnoreCase("YES"));
                            continue;
                        }
                    }

                    while (!r.peek().toString().equals("</file>")) {
                        e = r.nextEvent();
                        tag = e.toString();
                        if (tag.equals("<fileURL>")) {
                            mf.setFilename(parseCData(r));
                        } else if (tag.startsWith("<filePlot")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if ("part".equals(ns)) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setPlot(part, parseCData(r));
                        } else if (tag.startsWith("<fileImageURL")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if ("part".equals(ns)) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setVideoImageURL(part, parseCData(r));
                        } else if (tag.startsWith("<fileImageFile")) {
                            StartElement element = e.asStartElement();
                            int part = 1;
                            for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();) {
                                Attribute attr = i.next();
                                String ns = attr.getName().toString();

                                if ("part".equals(ns)) {
                                    part = Integer.parseInt(attr.getValue());
                                }
                            }
                            mf.setVideoImageFile(part, parseCData(r));
                        }
                    }
                    // add or replace MovieFile based on XML data
                    movie.addMovieFile(mf);
                }

                if (tag.startsWith("<trailer ")) {
                    TrailerFile tf = new TrailerFile();
                    tf.setNewFile(false);

                    StartElement start = e.asStartElement();
                    for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
                        Attribute attr = i.next();
                        String ns = attr.getName().toString();

                        if ("title".equals(ns)) {
                            tf.setTitle(attr.getValue());
                            continue;
                        }
                    }

                    tf.setFilename(parseCData(r));
                    // add or replace trailer based on XML data
                    movie.addTrailerFile(tf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
            return false;
        }

        movie.setDirty(movie.hasNewMovieFiles() || movie.hasNewTrailerFiles());
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

        writePreferences(writer, rootPath);

        List<Movie> allMovies = library.getMoviesList();

        if (includeMoviesInCategories) {
            for (Movie movie : library.getMoviesList()) {
                if (fullMovieInfoInIndexes) {
                    writeMovie(writer, movie);
                } else {
                    writeMovieForIndex(writer, movie);
                }
            }
        }

        for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            if (category.getValue().isEmpty() || !category.getValue().display()) {
                continue;
            }
            
            writer.writeStartElement("category");
            writer.writeAttribute("name", category.getKey());

            for (Map.Entry<String, List<Movie>> index : category.getValue().entrySet()) {
                String key = index.getKey();
                String indexFilename = FileTools.makeSafeFilename(FileTools.createPrefix(category.getKey(), key)) + "1";

                writer.writeStartElement("index");
                writer.writeAttribute("name", key);

                if (includeMoviesInCategories) {
                    writer.writeAttribute("filename", indexFilename);
                
                    for (Movie movie : index.getValue()) {
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
     */
    public void writeIndexXML(String rootPath, String detailsDirName, Library library) throws FileNotFoundException, XMLStreamException {

        for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            String categoryName = category.getKey();
            Map<String, List<Movie>> index = category.getValue();

            for (Map.Entry<String, List<Movie>> group : index.entrySet()) {
                String key = FileTools.createCategoryKey(group.getKey());

                List<Movie> movies = group.getValue();

                int previous = 1;
                int current = 1;
                int last;

                if (movies.size() % nbMoviesPerPage != 0) {
                    last = 1 + movies.size() / nbMoviesPerPage;
                } else {
                    last = movies.size() / nbMoviesPerPage;
                }

                int next = Math.min(2, last);
                int nbMoviesLeft = nbMoviesPerPage;

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
                        nbMoviesLeft = nbMoviesPerPage;
                    }
                }

                if (moviesInASinglePage.size() > 0) {
                    writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, 1, last);
                }
            }
        }
    }

    public void writeIndexPage(Library library, Collection<Movie> movies, String rootPath, String categoryName, String key, int previous, int current,
                    int next, int last) throws FileNotFoundException, XMLStreamException {
        String prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, key));
        File xmlFile = new File(rootPath, prefix + current + ".xml");
        xmlFile.getParentFile().mkdirs();

        XMLWriter writer = new XMLWriter(xmlFile);

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("library");
        writePreferences(writer, rootPath);

        for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            String categoryKey = category.getKey();
            Map<String, List<Movie>> index = category.getValue();
            writer.writeStartElement("category");
            writer.writeAttribute("name", categoryKey);
            if (categoryKey.equals(categoryName)) {
                writer.writeAttribute("current", "true");
            }

            for (String akey : index.keySet()) {
                String encakey = FileTools.createCategoryKey(akey);

                prefix = FileTools.makeSafeFilename(FileTools.createPrefix(categoryKey, encakey));

                writer.writeStartElement("index");
                writer.writeAttribute("name", akey);

                // if currently writing this page then add current attribute with value true
                if (encakey.equals(key)) {
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
        writer.writeAttribute("count", "" + nbMoviesPerPage);
        writer.writeAttribute("cols", "" + nbMoviesPerLine);

        if (fullMovieInfoInIndexes) {
            for (Movie movie : movies) {
                writeMovie(writer, movie);
            }
        } else {
            for (Movie movie : movies) {
                writeMovieForIndex(writer, movie);
            }
        }
        writer.writeEndElement(); // movies

        writer.writeEndElement(); // library
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void writeMovieForIndex(XMLWriter writer, Movie movie) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isTrailer", Boolean.toString(movie.isTrailer()));
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
        writer.writeStartElement("certification");
        writer.writeCharacters(movie.getCertification());
        writer.writeEndElement();
        writer.writeStartElement("season");
        writer.writeCharacters(Integer.toString(movie.getSeason()));
        writer.writeEndElement();
        writer.writeEndElement();
    }
    
    private void writeElementSet(XMLWriter writer, String set, String element, Collection<String> items) throws XMLStreamException {
        if (items.size() > 0) {
            writer.writeStartElement(set);
            for (String item : items) {
                writer.writeStartElement(element);
                writer.writeCharacters(item);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeMovie(XMLWriter writer, Movie movie) throws XMLStreamException {
        writer.writeStartElement("movie");
        writer.writeAttribute("isTrailer", Boolean.toString(movie.isTrailer()));
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
        writer.writeStartElement("posterSubimage");
        writer.writeCharacters(movie.getPosterSubimage());
        writer.writeEndElement();
        writer.writeStartElement("fanartURL");
        writer.writeCharacters(HTMLTools.encodeUrl(movie.getFanartURL()));
        writer.writeEndElement();
        writer.writeStartElement("posterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getPosterFilename())));
        writer.writeEndElement();
        writer.writeStartElement("detailPosterFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getDetailPosterFilename())));
        writer.writeEndElement();
        writer.writeStartElement("thumbnail");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getThumbnailFilename())));
        writer.writeEndElement();
        writer.writeStartElement("fanartFile");
        writer.writeCharacters(HTMLTools.encodeUrl(FileTools.makeSafeFilename(movie.getFanartFilename())));
        writer.writeEndElement();
        writer.writeStartElement("plot");
        writer.writeCharacters(movie.getPlot());
        writer.writeEndElement();
        writer.writeStartElement("outline");
        writer.writeCharacters(movie.getOutline());
        writer.writeEndElement();
        writer.writeStartElement("director");
        writer.writeCharacters(movie.getDirector());
        writer.writeEndElement();
        writer.writeStartElement("country");
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

        writeElementSet(writer, "genres", "genre", movie.getGenres());
        Collection<String> items = movie.getSets();
        if (items.size() > 0) {
            writer.writeStartElement("sets");
            for (String item : items) {
                writer.writeStartElement("set");
                Integer order = movie.getSetOrder(item);
                if (null != order) {
                    writer.writeAttribute("order", order.toString());
                }
                writer.writeCharacters(item);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writeElementSet(writer, "cast", "actor", movie.getCast());

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
                        writer.writeCharacters(mf.getVideoImageFile(part));
                        writer.writeEndElement();
                    }
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();

        Collection<TrailerFile> trailerFiles = movie.getTrailerFiles();
        if (trailerFiles != null && trailerFiles.size() > 0) {
            writer.writeStartElement("trailers");
            for (TrailerFile tf : trailerFiles) {
                writer.writeStartElement("trailer");
                writer.writeAttribute("title", tf.getTitle());
                writer.writeCharacters(tf.getFilename()); // should already be URL-encoded
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    public void writePreferences(XMLWriter writer, String rootPath) throws XMLStreamException {
        writer.writeStartElement("preferences");
        writer.writeStartElement("homePage");
        // Issue 436: Bounce off the index.htm to get to the real homePage
        // since homePage is now calculated by the Library.
        writer.writeCharacters("../index.htm");
        writer.writeEndElement();
        // Issue 310
        writer.writeStartElement("rootPath");
        File rootFile = new File(rootPath);
        writer.writeCharacters(rootFile.getAbsolutePath().replace('\\', '/'));
        writer.writeEndElement();
        // Issue 309
        for (Map.Entry<Object, Object> entry : PropertiesUtil.getEntrySet()) {
            writer.writeStartElement((String)entry.getKey());
            writer.writeCharacters((String)entry.getValue());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Persist a movie into an XML file. Doesn't overwrite an already existing XML file for the specified movie unless,
     * movie's data has changed or forceXMLOverwrite is true.
     */
    public void writeMovieXML(String rootPath, String tempRootPath, Movie movie) throws FileNotFoundException, XMLStreamException {
        String baseName = FileTools.makeSafeFilename(movie.getBaseName());
        File finalXmlFile = new File(rootPath + File.separator + baseName + ".xml");
        File tempXmlFile = new File(tempRootPath + File.separator + baseName + ".xml");
        tempXmlFile.getParentFile().mkdirs();

        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty()) {

            XMLWriter writer = new XMLWriter(tempXmlFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("details");
            writePreferences(writer, rootPath);
            writeMovie(writer, movie);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }
    }
}
