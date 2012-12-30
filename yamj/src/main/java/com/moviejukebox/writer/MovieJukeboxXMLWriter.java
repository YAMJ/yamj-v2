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
import com.moviejukebox.model.Attachment.*;
import com.moviejukebox.model.Comparator.CertificationComparator;
import com.moviejukebox.model.Comparator.IndexComparator;
import com.moviejukebox.model.Comparator.SortIgnorePrefixesComparator;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parse/Write XML files for movie details and library indexes
 *
 * @author Julien
 * @author Stuart.Boston
 */
public class MovieJukeboxXMLWriter {

    private static final Logger logger = Logger.getLogger(MovieJukeboxXMLWriter.class);
    private static final String LOG_MESSAGE = "XMLWriter: ";
    // Literals
    public static final String EXT_XML = ".xml";
    public static final String EXT_HTML = ".html";
    public static final String evFileSuffix = "_small";   // String to append to the eversion categories file if needed
    public static final String WON = "won";
    public static final String MOVIE = "movie";
    public static final String MOVIEDB = "moviedb";
    public static final String BASE_FILENAME = "baseFilename";
    public static final String TITLE = "title";
    public static final String ORIGINAL_TITLE = "originalTitle";
    public static final String SORT_TITLE = "titleSort";
    public static final String YEAR = "year";
    public static final String COUNTRY = "country";
    public static final String ORDER = "order";
    public static final String LANGUAGE = "language";
    public static final String YES = "YES";
    public static final String TRAILER_LAST_SCAN = "trailerLastScan";
    public static final String CHARACTER = "character";
    public static final String NAME = "name";
    public static final String DEPARTMENT = "department";
    public static final String JOB = "job";
    public static final String URL = "url";
    public static final String ID = "id_";
    public static final String SEASON = "season";
    public static final String PART = "part";
    public static final String RATING = "rating";
    public static final String COUNT = "count";
    public static final String INDEX = "index";
    public static final String ORIGINAL_NAME = "originalName";
    public static final String DETAILS = "details";
    public static final String SOURCE = "source";
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
    // Retrieve the title sort type
    private static TitleSortType titleSortType = TitleSortType.fromString(PropertiesUtil.getProperty("mjb.sortTitle", "title"));
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

        JukeboxStatistic js = JukeboxStatistic.fromString("index_" + categoryKey);
        JukeboxStatistics.setStatistic(js, indexMovies.size());

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
                                logger.trace(LOG_MESSAGE + "Forcing generation of set index.");
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
            logger.error(LOG_MESSAGE + "Failed writing index page: " + xmlFile.getName());
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
        if ((titleSortType == TitleSortType.ADOPT_ORIGINAL) && (StringTools.isValidString(movie.getOriginalTitle()))) {
            DOMHelper.appendChild(doc, eMovie, TITLE, movie.getOriginalTitle());
        } else {
            DOMHelper.appendChild(doc, eMovie, TITLE, movie.getTitle());
        }
        DOMHelper.appendChild(doc, eMovie, SORT_TITLE, movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, ORIGINAL_TITLE, movie.getOriginalTitle());
        DOMHelper.appendChild(doc, eMovie, "detailPosterFile", HTMLTools.encodeUrl(movie.getDetailPosterFilename()));
        DOMHelper.appendChild(doc, eMovie, "thumbnail", HTMLTools.encodeUrl(movie.getThumbnailFilename()));
        DOMHelper.appendChild(doc, eMovie, "bannerFile", HTMLTools.encodeUrl(movie.getBannerFilename()));
        DOMHelper.appendChild(doc, eMovie, "wideBannerFile", HTMLTools.encodeUrl(movie.getWideBannerFilename()));
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
    private Element generateElementSet(Document doc, String set, String element, Collection<String> items, Library library, String cat, String source) {

        if (items.size() > 0) {
            Element eSet = doc.createElement(set);
            eSet.setAttribute(COUNT, String.valueOf(items.size()));
            eSet.setAttribute(SOURCE, source);
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
    @SuppressWarnings("deprecation")
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
        DOMHelper.appendChild(doc, eMovie, "xmlGenerationDate", DateTimeTools.convertDateToString(new Date(), DateTimeTools.getDateFormatLongString()));
        DOMHelper.appendChild(doc, eMovie, "baseFilenameBase", movie.getBaseFilename());
        DOMHelper.appendChild(doc, eMovie, BASE_FILENAME, movie.getBaseName());
        if ((titleSortType == TitleSortType.ADOPT_ORIGINAL) && (StringTools.isValidString(movie.getOriginalTitle()))) {
            DOMHelper.appendChild(doc, eMovie, TITLE, movie.getOriginalTitle(), SOURCE, movie.getOverrideSource(OverrideFlag.TITLE));
        } else {
            DOMHelper.appendChild(doc, eMovie, TITLE, movie.getTitle(), SOURCE, movie.getOverrideSource(OverrideFlag.TITLE));
        }
        DOMHelper.appendChild(doc, eMovie, SORT_TITLE, movie.getTitleSort());
        DOMHelper.appendChild(doc, eMovie, ORIGINAL_TITLE, movie.getOriginalTitle(), SOURCE, movie.getOverrideSource(OverrideFlag.ORIGINALTITLE));

        Map<String, String> yearAttribs = new HashMap<String, String>();
        yearAttribs.put("Year", Library.getYearCategory(movie.getYear()));
        yearAttribs.put(SOURCE, movie.getOverrideSource(OverrideFlag.YEAR));
        DOMHelper.appendChild(doc, eMovie, YEAR, movie.getYear(), yearAttribs);

        DOMHelper.appendChild(doc, eMovie, "releaseDate", movie.getReleaseDate(), SOURCE, movie.getOverrideSource(OverrideFlag.RELEASEDATE));
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
        DOMHelper.appendChild(doc, eMovie, "wideBannerFile", HTMLTools.encodeUrl(movie.getWideBannerFilename()));
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

        DOMHelper.appendChild(doc, eMovie, "plot", movie.getPlot(), SOURCE, movie.getOverrideSource(OverrideFlag.PLOT));
        DOMHelper.appendChild(doc, eMovie, "outline", movie.getOutline(), SOURCE, movie.getOverrideSource(OverrideFlag.OUTLINE));
        DOMHelper.appendChild(doc, eMovie, "quote", movie.getQuote(), SOURCE, movie.getOverrideSource(OverrideFlag.QUOTE));
        DOMHelper.appendChild(doc, eMovie, "tagline", movie.getTagline(), SOURCE, movie.getOverrideSource(OverrideFlag.TAGLINE));

        Map<String, String> countryAttribs = new HashMap<String, String>();
        String countryIndex = createIndexAttribute(library, Library.INDEX_COUNTRY, movie.getCountry());
        if (countryIndex != null) {
            countryAttribs.put(INDEX, countryIndex);
        }
        countryAttribs.put(SOURCE, movie.getOverrideSource(OverrideFlag.COUNTRY));
        DOMHelper.appendChild(doc, eMovie, COUNTRY, movie.getCountry(), countryAttribs);
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

        DOMHelper.appendChild(doc, eMovie, "company", movie.getCompany(), SOURCE, movie.getOverrideSource(OverrideFlag.COMPANY));
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

        DOMHelper.appendChild(doc, eMovie, "runtime", movie.getRuntime(), SOURCE, movie.getOverrideSource(OverrideFlag.RUNTIME));
        DOMHelper.appendChild(doc, eMovie, "certification", Library.getIndexingCertification(movie.getCertification()), SOURCE, movie.getOverrideSource(OverrideFlag.CERTIFICATION));
        DOMHelper.appendChild(doc, eMovie, SEASON, Integer.toString(movie.getSeason()));

        DOMHelper.appendChild(doc, eMovie, LANGUAGE, movie.getLanguage(), SOURCE, movie.getOverrideSource(OverrideFlag.LANGUAGE));
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
                DateTime dt = new DateTime(movie.getTrailerLastScan());
                DOMHelper.appendChild(doc, eMovie, TRAILER_LAST_SCAN, DateTimeTools.convertDateToString(dt));
            } catch (Exception error) {
                DOMHelper.appendChild(doc, eMovie, TRAILER_LAST_SCAN, Movie.UNKNOWN);
            }
        }

        DOMHelper.appendChild(doc, eMovie, "container", movie.getContainer(), SOURCE, movie.getOverrideSource(OverrideFlag.CONTAINER));
        DOMHelper.appendChild(doc, eMovie, "videoCodec", movie.getVideoCodec());
        DOMHelper.appendChild(doc, eMovie, "audioCodec", movie.getAudioCodec());

        // Write codec information
        eMovie.appendChild(createCodecsElement(doc, movie.getCodecs()));
        DOMHelper.appendChild(doc, eMovie, "audioChannels", movie.getAudioChannels());
        DOMHelper.appendChild(doc, eMovie, "resolution", movie.getResolution(), SOURCE, movie.getOverrideSource(OverrideFlag.RESOLUTION));

        // If the source is unknown, use the default source
        if (StringTools.isNotValidString(movie.getVideoSource())) {
            DOMHelper.appendChild(doc, eMovie, "videoSource", defaultSource, SOURCE, Movie.UNKNOWN);
        } else {
            DOMHelper.appendChild(doc, eMovie, "videoSource", movie.getVideoSource(), SOURCE, movie.getOverrideSource(OverrideFlag.VIDEOSOURCE));
        }

        DOMHelper.appendChild(doc, eMovie, "videoOutput", movie.getVideoOutput(), SOURCE, movie.getOverrideSource(OverrideFlag.VIDEOOUTPUT));
        DOMHelper.appendChild(doc, eMovie, "aspect", movie.getAspectRatio(), SOURCE, movie.getOverrideSource(OverrideFlag.ASPECTRATIO));
        DOMHelper.appendChild(doc, eMovie, "fps", Float.toString(movie.getFps()), SOURCE, movie.getOverrideSource(OverrideFlag.FPS));

        if (movie.getFileDate() == null) {
            DOMHelper.appendChild(doc, eMovie, "fileDate", Movie.UNKNOWN);
        } else {
            // Try to catch any date re-formatting errors
            try {
                DOMHelper.appendChild(doc, eMovie, "fileDate", DateTimeTools.convertDateToString(movie.getFileDate()));
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
            eGenres.setAttribute(SOURCE, movie.getOverrideSource(OverrideFlag.GENRES));
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
        eSet = generateElementSet(doc, "directors", "director", movie.getDirectors(), library, Library.INDEX_DIRECTOR, movie.getOverrideSource(OverrideFlag.DIRECTORS));
        if (eSet != null) {
            eMovie.appendChild(eSet);
        }

        eSet = generateElementSet(doc, "writers", "writer", movie.getWriters(), library, Library.INDEX_WRITER, movie.getOverrideSource(OverrideFlag.WRITERS));
        if (eSet != null) {
            eMovie.appendChild(eSet);
        }

        eSet = generateElementSet(doc, "cast", "actor", movie.getCast(), library, Library.INDEX_CAST, movie.getOverrideSource(OverrideFlag.ACTORS));
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
                    ePerson.setAttribute(SOURCE, person.getSource());
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
                logger.debug(LOG_MESSAGE + "File length error for file " + mf.getFilename());
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
                logger.debug(LOG_MESSAGE + "getArchivename is '" + archiveName + "' for " + mf.getFilename() + " length " + archiveName.length());
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

            if (mf.getAttachments() != null && !mf.getAttachments().isEmpty()) {
                Element eAttachments = doc.createElement("attachments");
                for (Attachment att : mf.getAttachments()) {
                    Element eAttachment = doc.createElement("attachment");
                    eAttachment.setAttribute("type", att.getType().toString());
                    DOMHelper.appendChild(doc, eAttachment, "attachmentId", String.valueOf(att.getAttachmentId()));
                    DOMHelper.appendChild(doc, eAttachment, "contentType", att.getContentType().toString());
                    DOMHelper.appendChild(doc, eAttachment, "mimeType", att.getMimeType());
                    DOMHelper.appendChild(doc, eAttachment, "part", String.valueOf(att.getPart()));
                    eAttachments.appendChild(eAttachment);
                }
                eFileItem.appendChild(eAttachments);
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
            codecAttribs.put(SOURCE, codec.getCodecSource().toString());
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
     * Persist a movie into an XML file.
     *
     * Doesn't overwrite an already existing XML file for the specified movie unless, movie's data has changed (INFO,
     * RECHECK, WATCHED) or forceXMLOverwrite is true.
     */
    public void writeMovieXML(Jukebox jukebox, Movie movie, Library library) {
        String baseName = movie.getBaseName();
        File finalXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + EXT_XML);
        File tempXmlFile = new File(jukebox.getJukeboxTempLocationDetails() + File.separator + baseName + EXT_XML);

        FileTools.addJukeboxFile(finalXmlFile.getName());

        logger.debug(LOG_MESSAGE + "DirtyFlags for " + movie.getBaseName() + " are:" + movie.showDirty());
        if (!finalXmlFile.exists() || forceXMLOverwrite || movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.RECHECK) || movie.isDirty(DirtyFlag.WATCHED)) {
            Document xmlDoc;
            try {
                xmlDoc = DOMHelper.createDocument();
            } catch (ParserConfigurationException error) {
                logger.error(LOG_MESSAGE + "Failed writing " + tempXmlFile.getAbsolutePath());
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
                logger.error(LOG_MESSAGE + "Failed writing person XML for " + tempXmlFile.getName());
                logger.error(SystemTools.getStackTrace(error));
            }
        }
    }
}
