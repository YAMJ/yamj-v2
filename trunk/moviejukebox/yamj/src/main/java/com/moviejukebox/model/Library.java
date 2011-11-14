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
package com.moviejukebox.model;

import static com.moviejukebox.tools.FileTools.createCategoryKey;
import static com.moviejukebox.tools.FileTools.createPrefix;
import static com.moviejukebox.tools.FileTools.makeSafeFilename;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.moviejukebox.model.Comparator.CertificationComparator;
import com.moviejukebox.model.Comparator.LastModifiedComparator;
import com.moviejukebox.model.Comparator.MovieSetComparator;
import com.moviejukebox.model.Comparator.Top250Comparator;
import com.moviejukebox.model.Comparator.RatingComparator;
import com.moviejukebox.model.Comparator.RatingsComparator;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.ThreadExecutor;

public class Library implements Map<String, Movie> {

    public static final String TV_SERIES = "TVSeries";
    public static final String SET = "Set";

    private Collection<IndexInfo> generatedIndexes = Collections.synchronizedCollection(new ArrayList<IndexInfo>());

    private static Logger logger = Logger.getLogger("moviejukebox");
    private static boolean filterGenres;
    private static boolean filterCertificationn;
    private static boolean singleSeriesPage;
    private static List<String> certificationOrdering = new ArrayList<String>();
    private static Map<String, String> genresMap = new HashMap<String, String>();
    private static Map<String, String> certificationsMap = new HashMap<String, String>();
    private static String defaultCertification = null;
    private static Map<String, String> categoriesMap = new LinkedHashMap<String, String>(); // This is a LinkedHashMap to ensure that the order that the items are inserted into the Map is retained
    private static boolean charGroupEnglish = false;
    private HashMap<Movie, String> keys = new HashMap<Movie, String>();
    private TreeMap<String, Movie> library = new TreeMap<String, Movie>();
    private Map<String, Movie> extras = new TreeMap<String, Movie>();
    private List<Movie> moviesList = new ArrayList<Movie>();
    private Map<String, Index> indexes = new LinkedHashMap<String, Index>();
    private Map<String, Index> unCompressedIndexes = new LinkedHashMap<String, Index>();
    private static DecimalFormat paddedFormat = new DecimalFormat("000"); // Issue 190
    private static int maxGenresPerMovie = 3;
    private static int newMovieCount;
    private static long newMovieDays;
    private static int newTvCount;
    private static long newTvDays;
    private static int minSetCount = 2;
    private static boolean setsRequireAll = false;
    private static List<String> categoriesExplodeSet;
    private static boolean removeExplodeSet = false;
    private static boolean keepTVExplodeSet = true;
    private static boolean beforeSortExplodeSet = false;
    private static boolean removeTitleExplodeSet = false;
    private static String setsRating = "first";
    private static String indexList;
    private static boolean splitHD = false;
    private static boolean processExtras = true;
    private static boolean hideWatched = true;
    private static final boolean enableWatchScanner;
    private static List<String> dirtyLibraries = new ArrayList<String>();
    // Issue 1897: Cast enhancement
    private TreeMap<String, Person> people = new TreeMap<String, Person>();
    private static boolean isDirty = false;
    private static boolean peopleScan = false;
    private static boolean completePerson = true;

    // Static values for the year indexes
    private static final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private static final int finalYear = currentYear - 2;
    private static final int currentDecade = (finalYear / 10) * 10;
    
    // Index Names
    public static final String INDEX_OTHER = "Other";
    public static final String INDEX_GENRES = "Genres";
    public static final String INDEX_TITLE = "Title";
    public static final String INDEX_CERTIFICATION = "Certification";
    public static final String INDEX_YEAR = "Year";
    public static final String INDEX_LIBRARY = "Library";
    public static final String INDEX_CAST = "Cast";
    public static final String INDEX_DIRECTOR = "Director";
    public static final String INDEX_COUNTRY = "Country";
    public static final String INDEX_WRITER = "Writer";
    public static final String INDEX_AWARD = "Award";
    public static final String INDEX_PERSON = "Person";
    public static final String INDEX_RATINGS = "Ratings";
    public static final String INDEX_NEW = "New";
    public static final String INDEX_NEW_TV = "New-TV";
    public static final String INDEX_NEW_MOVIE = "New-Movie";
    public static final String INDEX_EXTRAS = "Extras";
    public static final String INDEX_HD = "HD";
    public static final String INDEX_HD1080 = "HD-1080";
    public static final String INDEX_HD720 = "HD-720";
    public static final String INDEX_3D = "3D";
    public static final String INDEX_SETS = "Sets";
    public static final String INDEX_SET = "Set";
    public static final String INDEX_TOP250 = "Top250";
    public static final String INDEX_RATING = "Rating";
    public static final String INDEX_WATCHED = "Watched";
    public static final String INDEX_UNWATCHED = "Unwatched";
    public static final String INDEX_ALL = "All";
    public static final String INDEX_TVSHOWS = "TV Shows";
    public static final String INDEX_MOVIES = "Movies";
    public static final String INDEX_ACTOR = "Actor";
    public static final String INDEX_CATEGORIES = "Categories";

    static {
        minSetCount = PropertiesUtil.getIntProperty("mjb.sets.minSetCount", "2");
        setsRequireAll = PropertiesUtil.getBooleanProperty("mjb.sets.requireAll", "false");
        setsRating = PropertiesUtil.getProperty("mjb.sets.rating", "first");
        categoriesExplodeSet = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.explodeSet", "").split(","));
        removeExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet", "false");
        keepTVExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.keepTV", "true");
        beforeSortExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.beforeSort", "false");
        removeTitleExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet.title", "false");
        singleSeriesPage = PropertiesUtil.getBooleanProperty("mjb.singleSeriesPage", "false");
        indexList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Certification,Year,Library,Set");
        splitHD = PropertiesUtil.getBooleanProperty("highdef.differentiate", "false");
        processExtras = PropertiesUtil.getBooleanProperty("filename.extras.process","true");
        hideWatched = PropertiesUtil.getBooleanProperty("mjb.Library.hideWatched", "true");
        enableWatchScanner = PropertiesUtil.getBooleanProperty("watched.scanner.enable", "true");
        filterGenres = PropertiesUtil.getBooleanProperty("mjb.filter.genres", "false");
        fillGenreMap(PropertiesUtil.getProperty("mjb.xmlGenreFile", "genres-default.xml"));
        
        filterCertificationn = PropertiesUtil.getBooleanProperty("mjb.filter.certification", "false");
        fillCertificationMap(PropertiesUtil.getProperty("mjb.xmlCertificationFile", "certification-default.xml"));

        try {
            maxGenresPerMovie = PropertiesUtil.getIntProperty("genres.max", String.valueOf(maxGenresPerMovie));
        } catch (Exception ignore) {
            maxGenresPerMovie = 3;
        }

        {
            String temp = PropertiesUtil.getProperty("certification.ordering");
            if (temp != null && !temp.isEmpty()) {
                String[] certs = temp.split(",");
                certificationOrdering.addAll(Arrays.asList(certs));
            }
        }

        fillCategoryMap(PropertiesUtil.getProperty("mjb.xmlCategoryFile", "categories-default.xml"));

        charGroupEnglish = PropertiesUtil.getBooleanProperty("indexing.character.groupEnglish", "false");
        completePerson = PropertiesUtil.getBooleanProperty("indexing.completePerson", "true");
        peopleScan = PropertiesUtil.getBooleanProperty("mjb.people", "false");
        getNewCategoryProperties();
    }

    public Library() {}
    
    /**
     * Calculate the Movie and TV New category properties
     */
    private static void getNewCategoryProperties() {
        String defaultDays = "7";
        String defaultCount = "0";
        
        //newDays  = PropertiesUtil.getLongProperty("mjb.newdays", defaultDays);
        //newCount = PropertiesUtil.getIntProperty("mjb.newcount", defaultCount);
            
        newMovieDays = PropertiesUtil.getLongProperty("mjb.newdays.movie", "-1");
        if (newMovieDays == -1) {
            // Use the old default, if the new property isn't found
            newMovieDays = PropertiesUtil.getLongProperty("mjb.newdays", defaultDays);
        }
        
        newMovieCount = PropertiesUtil.getIntProperty("mjb.newcount.movie", "-1");
        if (newMovieCount == -1) {
            // Use the old default, if the new property isn't found
            newMovieCount = PropertiesUtil.getIntProperty("mjb.newcount", defaultCount);
        }
        
        newTvDays = PropertiesUtil.getLongProperty("mjb.newdays.tv", "-1");
        if (newTvDays == -1) {
            // Use the old default, if the new property isn't found
            newTvDays = PropertiesUtil.getLongProperty("mjb.newdays", defaultDays);
        }
        
        newTvCount = PropertiesUtil.getIntProperty("mjb.newcount.tv", "-1");
        if (newTvCount == -1) {
            // Use the old default, if the new property isn't found
            newTvCount = PropertiesUtil.getIntProperty("mjb.newcount", defaultCount);
        }
        
        if (newMovieDays > 0) {
            logger.debug("New Movie category will have " + (newMovieCount > 0 ? ("the " + newMovieCount) : "all of the") + " most recent movies in the last " + newMovieDays + " days");
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newMovieDays *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        } else {
            logger.debug("New Movie category is disabled");
        }

        if (newTvDays > 0) {
            logger.debug("New TV category will have " + (newTvCount > 0 ? ("the " + newTvCount) : "all of the") + " most recent TV Shows in the last " + newTvDays + " days");
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newTvDays *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        } else {
            logger.debug("New category is disabled");
        }
}

    public static String getMovieKey(IMovieBasicInformation movie) {
        // Issue 190
        StringBuilder key = new StringBuilder(movie.getTitle());
        key.append(" (").append(movie.getYear()).append(")");

        if (movie.isTVShow()) {
            // Issue 190
            key.append(" Season ");
            key.append(paddedFormat.format(movie.getSeason()));
        }

        return key.toString().toLowerCase();
    }

    // synchronized because scanning can be multi-threaded
    public synchronized void addMovie(String key, Movie movie) {
        Movie existingMovie = library.get(key);
        // logger.debug("Adding video " + key + ", new part: " + (existingMovie != null));

        if (movie.isExtra()) {
            // logger.debug("  It's an extra: " + movie.getBaseName());
            extras.put(movie.getBaseName(), movie);
        } else if (existingMovie == null) {
            keys.put(movie, key);
            library.put(key, movie);
        } else {
            MovieFile firstMovieFile = movie.getFirstFile();
            // Take care of TV-Show (order by episode). Issue 535 - Not sure it's the best place do to this.
            if (existingMovie.isTVShow() || existingMovie.getMovieFiles().size()>1) {
                // The lower episode have to be the main movie.
                int newEpisodeNumber = firstMovieFile.getFirstPart();
                int oldEpisodesFirstNumber = Integer.MAX_VALUE;
                Collection<MovieFile> espisodesFiles = existingMovie.getFiles();
                for (MovieFile movieFile : espisodesFiles) {
                    if (movieFile.getFirstPart() < oldEpisodesFirstNumber) {
                        oldEpisodesFirstNumber = movieFile.getFirstPart();
                    }
                }
                // If the new episode was < than old ( episode 1 < episode 2)
                if (newEpisodeNumber < oldEpisodesFirstNumber) {
                    // The New episode have to be the 'main'
                    for (MovieFile movieFile : espisodesFiles) {
                        movie.addMovieFile(movieFile);
                        library.put(key, movie); // Replace the old one by the lower.
                        keys.remove(existingMovie);
                        keys.put(movie, key);
                        existingMovie = movie;
                    }
                } else {
                    // no change
                    existingMovie.addMovieFile(firstMovieFile);
                }
            } else {
                existingMovie.addMovieFile(firstMovieFile);
                // Update these counters
                existingMovie.setFileSize(movie.getFileSize());
            }
            existingMovie.addFileDate(movie.getFileDate());
        }
    }

    public void addMovie(Movie movie) {
        addMovie(getMovieKey(movie), movie);
    }

    public void mergeExtras() {
        for (Map.Entry<String, Movie> extraEntry : extras.entrySet()) {
            Movie extra = extraEntry.getValue();
            // Add extra to the library set
            library.put(extraEntry.getKey(), extra);
            // Add the extra to the corresponding movie
            Movie movie = library.get(getMovieKey(extra));
            if (null != movie) {
                // Extra file added is mark as new. When the XML will be parse
                // the extra file will be mark as old if it is defined in XML
                movie.addExtraFile(new ExtraFile(extraEntry.getValue().getFirstFile()));
            }
        }
    }

    protected static Map<String, Movie> buildIndexMasters(String prefix, Index index, List<Movie> indexMovies) {
        Map<String, Movie> masters = new HashMap<String, Movie>();

        for (Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
            String indexName = indexEntry.getKey();
            List<Movie> indexList = indexEntry.getValue();

            // Issue 2098: put to SET information from first movie by order
            int setIndex = 0;
            if (!indexList.get(setIndex).isTVShow() && indexList.get(setIndex).getSetOrder(indexName) != null) {
                int setOrder = indexList.get(setIndex).getSetOrder(indexName);
                if (setOrder > 1) {
                    for (int i = 1; i < indexList.size(); i++) {
                        if ((indexList.get(i).getSetOrder(indexName) != null) && setOrder > indexList.get(i).getSetOrder(indexName)) {
                            setOrder = indexList.get(i).getSetOrder(indexName);
                            setIndex = i;
                        }
                    }
                }
            }

            // We can't clone the movie because of the Collection objects in there, so we'll have to copy it
            Movie indexMaster = Movie.newInstance(indexList.get(setIndex));
            indexMaster.setDirty(false);
            
            indexMaster.setSetMaster(true);
            indexMaster.setSetSize(indexList.size());
            indexMaster.setTitle(indexName);
            indexMaster.setTitleSort(indexName);
            indexMaster.setOriginalTitle(indexName);
            indexMaster.setBaseFilename(createPrefix(prefix, createCategoryKey(indexName)) + "1");
            indexMaster.setBaseName(makeSafeFilename(indexMaster.getBaseFilename()));
            
            // set TV and HD properties of the master
            int cntTV = 0;
            int cntHD = 0;
            int top250 = -1;
            boolean watched = true; // Assume watched for the check, because any false value will reset it.
            int maxRating = 0;
            int sumRating = 0;
            int currentRating;

            // We Can't use a TreeSet because MF.compareTo just compares part #
            // so it fails when we combine multiple seasons into one collection
            Collection<MovieFile> masterMovieFileCollection = new LinkedList<MovieFile>();
            for (Movie movie : indexList) {
                if (movie.isTVShow()) {
                    ++cntTV;
                }
                
                if (movie.isHD()) {
                    ++cntHD;
                }

                // If watched is false for any part, then set the master to unwatched
                watched &= movie.isWatched();
                
                int mTop250 = movie.getTop250();
                if (mTop250 > 0 && (top250 < 0 || mTop250 < top250)) {
                    top250 = mTop250;
                }

                currentRating = movie.getRating();
                if (currentRating >= 0) {
                    sumRating += currentRating;
                    if (currentRating > maxRating) {
                        maxRating = currentRating;
                    }
                }

                Collection<MovieFile> movieFileCollection = movie.getMovieFiles();
                if (movieFileCollection != null) {
                    masterMovieFileCollection.addAll(movieFileCollection);
                }
                
                // Update the master fileDate to be the latest of all the members so this indexes correctly in the New category
                indexMaster.addFileDate(movie.getFileDate());
            }

            indexMaster.setMovieType(cntTV > 1 ? Movie.TYPE_TVSHOW : Movie.TYPE_MOVIE);
            indexMaster.setVideoType(cntHD > 1 ? Movie.TYPE_VIDEO_HD : null);
            indexMaster.setWatchedFile(watched);
            indexMaster.setTop250(top250);
            if (setsRating.equalsIgnoreCase("max") || (setsRating.equalsIgnoreCase("average") && (indexList.size() > 0))) {
                HashMap<String, Integer> ratings = new HashMap<String, Integer>();
                ratings.put("setrating", setsRating.equalsIgnoreCase("max")?maxRating:(sumRating/indexList.size()));
                indexMaster.setRatings(ratings);
            }
            indexMaster.setMovieFiles(masterMovieFileCollection);
            
            masters.put(indexName, indexMaster);
            
            StringBuilder sb = new StringBuilder("Setting index master '");
            sb.append(indexMaster.getTitle());
            sb.append("' - isTV: ").append(indexMaster.isTVShow());
            sb.append(" (").append(cntTV).append("/").append(indexList.size()).append(")");
            sb.append(" - isHD: ").append(indexMaster.isHD());
            sb.append(" (").append(cntHD).append("/").append(indexList.size()).append(")");
            sb.append(" - top250: ").append(indexMaster.getTop250());
            sb.append(" - watched: ").append(indexMaster.isWatched());
            sb.append(" - rating: ").append(indexMaster.getRating());
            logger.debug(sb.toString());
            
        }

        return masters;
    }

    protected static void compressSetMovies(List<Movie> movies, Index index, Map<String, Movie> masters, String indexName, String subIndexName) {
        // Construct an index that includes only the intersection of movies and index
        Index in_movies = new Index();
        for (Map.Entry<String, List<Movie>> index_entry : index.entrySet()) {
            for (Movie m : index_entry.getValue()) {
                if (movies.contains(m)) {
                    in_movies.addMovie(index_entry.getKey(), m);
                }
            }
        }

        // Now, for each list of movies in in_movies, if the list has more than the minSetCount movies
        // remove them all from the movies list, and insert the corresponding master
        for (Map.Entry<String, List<Movie>> in_movies_entry : in_movies.entrySet()) {
            List<Movie> lm = in_movies_entry.getValue();
            if (lm.size() >= minSetCount && (!setsRequireAll || lm.size() == index.get(in_movies_entry.getKey()).size())) {
                boolean tvSet = keepTVExplodeSet && lm.get(0).isTVShow();
                boolean explodeSet = categoriesExplodeSet.contains(indexName) || (indexName.equalsIgnoreCase(INDEX_OTHER) && categoriesExplodeSet.contains(subIndexName));
                if (!beforeSortExplodeSet || !explodeSet || tvSet) {
                    movies.removeAll(lm);
                }
                if (!beforeSortExplodeSet || !explodeSet || tvSet || !removeExplodeSet) {
                    movies.add(masters.get(in_movies_entry.getKey()));
                }
            }
        }
    }

    public void buildIndex(ThreadExecutor<Void> tasks) throws Throwable {
        moviesList.clear();
        indexes.clear();

        tasks.restart();
        final List<Movie> indexMovies = new ArrayList<Movie>(library.values());
        moviesList.addAll(library.values());

        if (indexMovies.size() > 0) {
            Map<String, Index> dynamicIndexes = new LinkedHashMap<String, Index>();
            // Add the sets FIRST! That allows users to put series inside sets
            dynamicIndexes.put(SET, indexBySets(indexMovies));

            final Map<String, Index> syncindexes = Collections.synchronizedMap(indexes);

            for (final String indexStr : indexList.split(",")) {
                tasks.submit(new Callable<Void>() {
                    public Void call() {
                        SystemTools.showMemory();
                        logger.info("  Indexing " + indexStr + "...");
                        if (indexStr.equals(INDEX_OTHER)) {
                            syncindexes.put(INDEX_OTHER, indexByProperties(indexMovies));
                        } else if (indexStr.equals(INDEX_GENRES)) {
                            syncindexes.put(INDEX_GENRES, indexByGenres(indexMovies));
                        } else if (indexStr.equals(INDEX_TITLE)) {
                            syncindexes.put(INDEX_TITLE, indexByTitle(indexMovies));
                        } else if (indexStr.equals(INDEX_CERTIFICATION)) {
                            syncindexes.put(INDEX_CERTIFICATION, indexByCertification(indexMovies));
                        } else if (indexStr.equals(INDEX_YEAR)) {
                            syncindexes.put(INDEX_YEAR, indexByYear(indexMovies));
                        } else if (indexStr.equals(INDEX_LIBRARY)) {
                            syncindexes.put(INDEX_LIBRARY, indexByLibrary(indexMovies));
                        } else if (indexStr.equals(INDEX_CAST)) {
                            syncindexes.put(INDEX_CAST, indexByCast(indexMovies));
                        } else if (indexStr.equals(INDEX_DIRECTOR)) {
                            syncindexes.put(INDEX_DIRECTOR, indexByDirector(indexMovies));
                        } else if (indexStr.equals(INDEX_COUNTRY)) {
                            syncindexes.put(INDEX_COUNTRY, indexByCountry(indexMovies));
                        } else if (indexStr.equals(INDEX_WRITER)) {
                            syncindexes.put(INDEX_WRITER, indexByWriter(indexMovies));
                        } else if (indexStr.equals(INDEX_AWARD)) {
                            syncindexes.put(INDEX_AWARD, indexByAward(indexMovies));
                        } else if (indexStr.equals(INDEX_PERSON)) {
                            syncindexes.put(INDEX_PERSON, indexByPerson(indexMovies));
                        } else if (indexStr.equals(INDEX_RATINGS)) {
                            syncindexes.put(INDEX_RATINGS, indexByRatings(indexMovies));
                        }
                        return null;
                    }
                });
            }
            tasks.waitFor();
            SystemTools.showMemory();
            
            // Make a "copy" of uncompressed index
            this.keepUncompressedIndexes();

            Map<String, Map<String, Movie>> dynamicIndexMasters = new HashMap<String, Map<String, Movie>>();
            for (Map.Entry<String, Index> dynamicEntry : dynamicIndexes.entrySet()) {
                Map<String, Movie> indexMasters = buildIndexMasters(dynamicEntry.getKey(), dynamicEntry.getValue(), indexMovies);
                dynamicIndexMasters.put(dynamicEntry.getKey(), indexMasters);

                for (Map.Entry<String, Index> indexesEntry : indexes.entrySet()) {
                    // For each category in index, compress this one.
                    for (Map.Entry<String, List<Movie>> indexEntry : indexesEntry.getValue().entrySet()) {
                        compressSetMovies(indexEntry.getValue(), dynamicEntry.getValue(), indexMasters, indexesEntry.getKey(), indexEntry.getKey());
                    }
                }
                indexes.put(dynamicEntry.getKey(), dynamicEntry.getValue());
                moviesList.addAll(indexMasters.values()); // so the driver knows what's an index master
            }

            // Now add the masters to the titles index
            // Issue 1018 - Check that this index was selected
            if (indexList.contains(INDEX_TITLE)) {
                for (Map.Entry<String, Map<String, Movie>> dynamicIndexMastersEntry : dynamicIndexMasters.entrySet()) {
                    Index mastersTitlesIndex = indexByTitle(dynamicIndexMastersEntry.getValue().values());
                    for (Map.Entry<String, List<Movie>> indexEntry : mastersTitlesIndex.entrySet()) {
                        for (Movie m : indexEntry.getValue()) {
                            int setCount = dynamicIndexes.get(dynamicIndexMastersEntry.getKey()).get(m.getTitle()).size();
                            if (setCount >= minSetCount) {
                                indexes.get(INDEX_TITLE).addMovie(indexEntry.getKey(), m);
                            }
                        }
                    }
                }
            }
            SystemTools.showMemory();
            tasks.restart();
            
            // OK, now that all the index masters are in-place, sort everything.
            logger.info("  Sorting Indexes ...");
            for (final Map.Entry<String, Index> indexesEntry : indexes.entrySet()) {
                for (final Map.Entry<String, List<Movie>> indexEntry : indexesEntry.getValue().entrySet()) {
                    tasks.submit(new Callable<Void>() {
                        public Void call() {
                            Comparator<Movie> comp = getComparator(indexesEntry.getKey(), indexEntry.getKey());
                            if (null != comp) {
                                Collections.sort(indexEntry.getValue(), comp);
                            } else {
                                Collections.sort(indexEntry.getValue());
                            }
                            return null;
                        }
                    });
                }
            }
            tasks.waitFor();
            SystemTools.showMemory();

            // Cut off the Other/New lists if they're too long AND add them to the NEW category if required
            boolean trimNewTvOK = trimNewCategory(INDEX_NEW_TV, newTvCount);
            boolean trimNewMovieOK = trimNewCategory(INDEX_NEW_MOVIE, newMovieCount);
            
            // Merge the two categories into the Master "New" category
            
            if (categoriesMap.get(INDEX_NEW) != null) {
                Index otherIndexes = indexes.get(INDEX_OTHER);
                List<Movie> newList = new ArrayList<Movie>();
                int newMovies = 0;
                int newTVShows = 0;
                
                if (trimNewMovieOK && (categoriesMap.get(INDEX_NEW_MOVIE) != null)  && (otherIndexes.get(categoriesMap.get(INDEX_NEW_MOVIE)) != null)){
                    newList.addAll(otherIndexes.get(categoriesMap.get(INDEX_NEW_MOVIE)));
                    newMovies = otherIndexes.get(categoriesMap.get(INDEX_NEW_MOVIE)).size();
                } else {
                    // Remove the empty "New Movie" category
                    if (categoriesMap.get(INDEX_NEW_MOVIE) != null) {
                        otherIndexes.remove(categoriesMap.get(INDEX_NEW_MOVIE));
                    }
                }
                
                if (trimNewTvOK && (categoriesMap.get(INDEX_NEW_TV) != null)  && (otherIndexes.get(categoriesMap.get(INDEX_NEW_TV)) != null)){
                    newList.addAll(otherIndexes.get(categoriesMap.get(INDEX_NEW_TV)));
                    newTVShows = otherIndexes.get(categoriesMap.get(INDEX_NEW_TV)).size();
                } else {
                    // Remove the empty "New TV" category
                    if (categoriesMap.get(INDEX_NEW_TV) != null) {
                        otherIndexes.remove(categoriesMap.get(INDEX_NEW_TV));
                    }
                }
                
                // If we have new videos, then create the super "New" category
                if ((newMovies + newTVShows) > 0) {
                    StringBuilder logMessage = new StringBuilder("Creating new category with ");
                    if (newMovies > 0) {
                        logMessage.append(newMovies).append(" new movie").append(newMovies > 1?"s":"");
                    }
                    if (newTVShows > 0) {
                        logMessage.append(newMovies > 0?" & ":"");
                        logMessage.append(newTVShows).append(" new TV Show").append(newTVShows > 1?"s":"");
                    }
                    
                    logger.debug(logMessage.toString());
                    otherIndexes.put(categoriesMap.get(INDEX_NEW), newList);
                    Collections.sort(otherIndexes.get(categoriesMap.get(INDEX_NEW)), cmpLast);
                }
            }
            
            // Now set up the index masters' posters
            for (Map.Entry<String, Map<String, Movie>> dynamicIndexMastersEntry : dynamicIndexMasters.entrySet()) {
                for (Map.Entry<String, Movie> mastersEntry : dynamicIndexMastersEntry.getValue().entrySet()) {
                    List<Movie> set = dynamicIndexes.get(dynamicIndexMastersEntry.getKey()).get(mastersEntry.getKey());
                    mastersEntry.getValue().setPosterFilename(set.get(0).getBaseName() + ".jpg");
                    mastersEntry.getValue().setFile(set.get(0).getFile()); // ensure ArtworkScanner looks in the right directory
                }
            }
            Collections.sort(indexMovies);
            setMovieListNavigation(indexMovies);
            SystemTools.showMemory();
        }
    }
    
    /**
     * Trim the new category to the required length, add the trimmed video list to the NEW category
     * @param catName   The name of the category: "New-TV" or "New-Movie"
     * @param catCount  The maximum size of the category
     * @return 
     */
    private boolean trimNewCategory(String catName, int catCount) {
        boolean trimOK = true;
        String category = categoriesMap.get(catName);
        //logger.info("Trimming '" + catName + "' ('" + category + "') to " + catCount + " videos");
        if (catCount > 0 && category != null) {
            Index otherIndexes = indexes.get(INDEX_OTHER);
            if (otherIndexes != null) {
                List<Movie> newList = otherIndexes.get(category);
                //logger.info("Current size of '" + catName + "' ('" + category + "') is " + (newList != null ? newList.size() : "NULL"));
                if ((newList != null)  && (newList.size() > catCount)) {
                        newList = newList.subList(0, catCount);
                        otherIndexes.put(category, newList);
                }
            } else {
                logger.warn("Warning : You need to enable index 'Other' to get '" + catName + "' ('" + category + "') category");
                trimOK = false;
            }
        }
        
        return trimOK;
    }

    private void keepUncompressedIndexes() {
        this.unCompressedIndexes = new HashMap<String, Index>(indexes.size());
        Set<String> indexeskeySet = this.indexes.keySet();
        for (String key : indexeskeySet) {
            logger.debug("Copying " + key + " indexes");
            Index index = this.indexes.get(key);
            Index indexTmp = new Index();

            unCompressedIndexes.put(key, indexTmp);
            for (String keyCategorie : index.keySet()) {
                List<Movie> listMovie = index.get(keyCategorie);
                List<Movie> litMovieTmp = new ArrayList<Movie>(listMovie.size());
                indexTmp.put(keyCategorie, litMovieTmp);

                for (Movie movie : listMovie) {
                    litMovieTmp.add(movie);
                }
            }
        }
    }

    private static void setMovieListNavigation(List<Movie> moviesList) {
        List<Movie> extraList = new ArrayList<Movie>();

        IMovieBasicInformation first = null;
        IMovieBasicInformation last = null;

        // sort the extras out of the movies
        for (Movie m : moviesList) {
            if (m.isExtra()) {
                extraList.add(m);
            } else {
                if (first == null) {
                    // set the first non-extra movie
                    first = m;
                }
                // set the last non-extra movie
                last = m;
            }
        }

        // ignore the extras while sorting the other movies
        for (int j = 0; j < moviesList.size(); j++) {
            Movie movie = moviesList.get(j);
            if (!movie.isExtra()) {
                movie.setFirst(first.getBaseName());

                for (int p = j - 1; p >= 0; p--) {
                    Movie prev = moviesList.get(p);
                    if (!prev.isExtra()) {
                        movie.setPrevious(prev.getBaseName());
                        break;
                    }
                }

                for (int n = j + 1; n < moviesList.size(); n++) {
                    Movie next = moviesList.get(n);
                    if (!next.isExtra()) {
                        movie.setNext(next.getBaseName());
                        break;
                    }
                }

                movie.setLast(last.getBaseName());
            }
        }

        // sort the extras separately
        if (!extraList.isEmpty()) {
            IMovieBasicInformation firstExtra = extraList.get(0);
            IMovieBasicInformation lastExtra = extraList.get(extraList.size() - 1);
            for (int i = 0; i < extraList.size(); i++) {
                Movie movie = extraList.get(i);
                movie.setFirst(firstExtra.getBaseName());
                movie.setPrevious(i > 0 ? extraList.get(i - 1).getBaseName() : firstExtra.getBaseName());
                movie.setNext(i < extraList.size() - 1 ? extraList.get(i + 1).getBaseName() : lastExtra.getBaseName());
                movie.setLast(lastExtra.getBaseName());
            }
        }
    }

    private static Index indexByTitle(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isExtra() && (!removeTitleExplodeSet || !movie.isSetMaster())) {
                String title = movie.getStrippedTitleSort();
                if (title.length() > 0) {
                    Character firstCharacter = Character.toUpperCase(title.charAt(0));

                    if (!Character.isLetter(firstCharacter)) {
                        index.addMovie("09", movie);
                        movie.addIndex(INDEX_TITLE, "09");
                    } else if (charGroupEnglish && ((firstCharacter >= 'A' && firstCharacter <= 'Z') || (firstCharacter >= 'a' && firstCharacter <= 'z'))) {
                        index.addMovie("AZ", movie);
                        movie.addIndex(INDEX_TITLE, "AZ");
                    } else {
                        String newChar = StringTools.characterMapReplacement(firstCharacter);
                        index.addMovie(newChar, movie);
                        movie.addIndex(INDEX_TITLE, newChar);
                    }
                }
            }
        }
        return index;
    }

    private static Index indexByYear(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isExtra()) {
                String year = getYearCategory(movie.getYear());
                if (null != year) {
                    index.addMovie(year, movie);
                    movie.addIndex(INDEX_YEAR, year);
                }
            }
        }
        return index;
    }

    private static Index indexByLibrary(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isExtra() && movie.getLibraryDescription().length() > 0) {
                index.addMovie(movie.getLibraryDescription(), movie);
                movie.addIndex(INDEX_LIBRARY, movie.getLibraryDescription());
            }
        }
        return index;
    }

    private static Index indexByGenres(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isExtra()) {
                int cntGenres = 0;
                for (String genre : movie.getGenres()) {
                    if (cntGenres < maxGenresPerMovie) {
                        index.addMovie(getIndexingGenre(genre), movie);
                        movie.addIndex(INDEX_GENRES, getIndexingGenre(genre));
                        ++cntGenres;
                    }
                }
            }
        }
        return index;
    }

    private static Index indexByCertification(Iterable<Movie> moviesList) {
        Index index = null;
        if (!certificationOrdering.isEmpty()) {
            index = new Index(new CertificationComparator(certificationOrdering));
        } else {
            index = new Index();
        }

        for (Movie movie : moviesList) {
            if (!movie.isExtra()) {
                index.addMovie(getIndexingCertification(movie.getCertification()), movie);
                movie.addIndex(INDEX_CERTIFICATION, getIndexingCertification(movie.getCertification()));
            }
        }
        return index;
    }

    /**
     * Index the videos by the property values
     * This is slightly different from the other indexes as there may be multiple entries for each of the videos 
     * @param moviesList
     * @return
     */
    private static Index indexByProperties(Iterable<Movie> moviesList) {
        Index index = new Index();
        long now = System.currentTimeMillis();
        for (Movie movie : moviesList) {
            if (movie.isExtra()) {
                // Issue 997: Skip the processing of extras
                if (processExtras) {
                    if (categoriesMap.get(INDEX_EXTRAS) != null) {
                        index.addMovie(categoriesMap.get(INDEX_EXTRAS), movie);
                        movie.addIndex(INDEX_EXTRAS, categoriesMap.get(INDEX_EXTRAS));
                    }
                }
            } else {
                if (movie.isHD()) {
                    if (splitHD) {
                        // Split the HD category into two categories: HD-720 and HD-1080
                        if (movie.isHD1080()) {
                            if (categoriesMap.get(INDEX_HD1080) != null) {
                                index.addMovie(categoriesMap.get(INDEX_HD1080), movie);
                                movie.addIndex(INDEX_HD, categoriesMap.get(INDEX_HD1080));
                            }
                        } else {
                            if (categoriesMap.get(INDEX_HD720) != null) {
                                index.addMovie(categoriesMap.get(INDEX_HD720), movie);
                                movie.addIndex(INDEX_HD, categoriesMap.get(INDEX_HD720));
                            }
                        }
                    } else {
                        if (categoriesMap.get(INDEX_HD) != null) {
                            index.addMovie(categoriesMap.get(INDEX_HD), movie);
                            movie.addIndex(INDEX_HD, categoriesMap.get(INDEX_HD));
                        }
                    }
                }

                if (movie.is3D()) {
                    if (categoriesMap.get(INDEX_3D) != null) {
                        index.addMovie(categoriesMap.get(INDEX_3D), movie);
                        movie.addIndex(INDEX_3D, categoriesMap.get(INDEX_3D));
                    }
                }

                if (movie.getTop250() > 0) {
                    if (categoriesMap.get(INDEX_TOP250) != null) {
                        index.addMovie(categoriesMap.get(INDEX_TOP250), movie);
                        movie.addIndex(INDEX_TOP250, categoriesMap.get(INDEX_TOP250));
                    }
                }
                
                if (movie.getRating() > 0) {
                    if (categoriesMap.get(INDEX_RATING) != null) {
                        index.addMovie(categoriesMap.get(INDEX_RATING), movie);
                        movie.addIndex(INDEX_RATING, categoriesMap.get(INDEX_RATING));
                    }
                }
                
                if (enableWatchScanner) { // Issue 1938 don't create watched/unwatched indexes if scanner is disabled
                    // Add to the Watched or Unwatched category
                    if (movie.isWatched()) {
                        index.addMovie(categoriesMap.get(INDEX_WATCHED), movie);
                        movie.addIndex(INDEX_WATCHED, categoriesMap.get(INDEX_WATCHED));
                    } else {
                        index.addMovie(categoriesMap.get(INDEX_UNWATCHED), movie);
                        movie.addIndex(INDEX_UNWATCHED, categoriesMap.get(INDEX_UNWATCHED));
                    }
                }
                
                // Add to the New Movie category
                if (!movie.isTVShow() && (newMovieDays > 0) && (now - movie.getLastModifiedTimestamp() <= newMovieDays) && !(movie.isWatched() && hideWatched && enableWatchScanner)) {
                    if (categoriesMap.get(INDEX_NEW_MOVIE) != null) {
                        index.addMovie(categoriesMap.get(INDEX_NEW_MOVIE), movie);
                        movie.addIndex(INDEX_NEW_MOVIE, categoriesMap.get(INDEX_NEW_MOVIE));
                    }
                }
                
                // Add to the New TV category
                if (movie.isTVShow() && (newTvDays > 0) && (now - movie.getLastModifiedTimestamp() <= newTvDays) && !(movie.isWatched() && hideWatched && enableWatchScanner)) {
                    if (categoriesMap.get(INDEX_NEW_TV) != null) {
                        index.addMovie(categoriesMap.get(INDEX_NEW_TV), movie);
                        movie.addIndex(INDEX_NEW_TV, categoriesMap.get(INDEX_NEW_TV));
                    }
                }

                if (categoriesMap.get(INDEX_ALL) != null) {
                    index.addMovie(categoriesMap.get(INDEX_ALL), movie);
                    movie.addIndex(INDEX_ALL, categoriesMap.get(INDEX_ALL));
                }

                if (movie.isTVShow()) {
                    if (categoriesMap.get(INDEX_TVSHOWS) != null) {
                        index.addMovie(categoriesMap.get(INDEX_TVSHOWS), movie);
                        movie.addIndex(INDEX_TVSHOWS, categoriesMap.get(INDEX_TVSHOWS));
                    }
                } else {
                    if (categoriesMap.get(INDEX_MOVIES) != null) {
                        index.addMovie(categoriesMap.get(INDEX_MOVIES), movie);
                        movie.addIndex(INDEX_MOVIES, categoriesMap.get(INDEX_MOVIES));
                    }
                }

                if (!movie.isTVShow() && (movie.getSetsKeys().size() > 0)) {
                    if (categoriesMap.get(INDEX_SETS) != null) {
                        index.addMovie(categoriesMap.get(INDEX_SETS), movie);
                        movie.addIndex(INDEX_SETS, categoriesMap.get(INDEX_SETS));
                    }
                }
            }
        }

        return index;
    }

    protected static Index indexBySets(List<Movie> list) {
        Index index = new Index(false);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (singleSeriesPage && movie.isTVShow()) {
                    index.addMovie(movie.getOriginalTitle(), movie);
                    movie.addIndex(INDEX_SET, movie.getOriginalTitle());
                }

                for (String set_key : movie.getSetsKeys()) {
                    index.addMovie(set_key, movie);
                    movie.addIndex(INDEX_SET, set_key);
                }
            }
        }

        return index;
    }

    protected static Index indexByCast(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (peopleScan) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase("Actors") || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String actor = person.getTitle();
                        logger.debug("Adding " + movie.getTitle() + " to cast list for " + actor);
                        index.addMovie(actor, movie);
                        movie.addIndex(INDEX_ACTOR, actor);
                    }
                } else {
                    for (String actor : movie.getCast()) {
                        logger.debug("Adding " + movie.getTitle() + " to cast list for " + actor);
                        index.addMovie(actor, movie);
                        movie.addIndex(INDEX_ACTOR, actor);
                    }
                }
            }
        }

        return index;
    }

    protected static Index indexByCountry(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                index.addMovie(movie.getCountry(), movie);
                movie.addIndex(INDEX_COUNTRY, movie.getCountry());
            }
        }

        return index;
    }

    protected static Index indexByDirector(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (peopleScan) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase("Directing") || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String director = person.getTitle();
                        logger.debug("Adding " + movie.getTitle() + " to director list for " + director);
                        index.addMovie(director, movie);
                        movie.addIndex(INDEX_DIRECTOR, director);
                    }
                } else {
                    for (String director : movie.getDirectors()) {
                        logger.debug("Adding " + movie.getTitle() + " to director list for " + director);
                        index.addMovie(director, movie);
                        movie.addIndex(INDEX_DIRECTOR, director);
                    }
                }
            }
        }

        return index;
    }

    protected static Index indexByWriter(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (peopleScan) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase("Writing") || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String writer = person.getTitle();
                        logger.debug("Adding " + movie.getTitle() + " to writer list for " + writer);
                        index.addMovie(writer, movie);
                        movie.addIndex(INDEX_WRITER, writer);
                    }
                } else {
                    for (String writer : movie.getWriters()) {
                        logger.debug("Adding " + movie.getTitle() + " to writer list for " + writer);
                        index.addMovie(writer, movie);
                        movie.addIndex(INDEX_WRITER, writer);
                    }
                }
            }
        }

        return index;
    }

    protected static Index indexByAward(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                for (AwardEvent award : movie.getAwards()) {
                    String awardName = award.getName();
                    logger.debug("Adding " + movie.getTitle() + " to award list for " + awardName);
                    index.addMovie(awardName, movie);
                    movie.addIndex(INDEX_AWARD, awardName);
                }
            }
        }

        return index;
    }

    protected static Index indexByPerson(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                for (Filmography person : movie.getPeople()) {
                    if (completePerson && StringTools.isNotValidString(person.getFilename())) {
                        continue;
                    }
                    String name = person.getName();
                    logger.debug("Adding " + movie.getTitle() + " to person list for " + name);
                    index.addMovie(name, movie);
                    movie.addIndex(INDEX_PERSON, name);
                }
            }
        }

        return index;
    }

    protected static Index indexByRatings(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra() && (movie.getRating() > 0)) {
                String rating = Integer.toString((int)Math.floor(movie.getRating()/10));
                rating = rating + ".0-" + rating + ".9";
                logger.debug("Adding " + movie.getTitle() + " to ratings list for " + rating);
                index.addMovie(rating, movie);
                movie.addIndex(INDEX_RATINGS, rating);
            }
        }

        return index;
    }

    public int getMovieCountForIndex(String indexName, String category) {
        Index index = unCompressedIndexes.get(indexName);
        
        if (index == null) {
            index = indexes.get(indexName);
        }
        
        List<Movie> categoryList = index.get(category);
        
        if (categoryList != null) {
            return categoryList.size();
        } else {
            return -1;
        }
    }

    /**
     * Checks if there is a master (will be shown in the index) genre for the specified one.
     * 
     * @param genre
     *            Genre to find the master for
     * @return Genre itself or master if available.
     */
    public static String getIndexingGenre(String genre) {
        if (!filterGenres) {
            return genre;
        }

        String masterGenre = genresMap.get(genre);
        if (masterGenre != null) {
            return masterGenre;
        } else {
            return genre;
        }
    }

    /**
     * Checks if there is a master (will be shown in the index) Certification for the specified one.
     * 
     * @param certification
     *            Certification to find the master for
     * @return Certification itself or master if available.
     */
    public static String getIndexingCertification(String certification) {
        if (!filterCertificationn) {
            return certification;
        }

        String masterCertification = certificationsMap.get(certification);
        if (StringUtils.isNotBlank(masterCertification)) {
            return masterCertification;
        } else if (StringTools.isValidString(defaultCertification)) {
            return defaultCertification;
        } else {
            return certification;
        }
    }

    public void clear() {
        library.clear();
        people.clear();
    }

    public Object clone() throws CloneNotSupportedException {
        return library.clone();
    }

    public boolean containsKey(Object key) {
        return library.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return library.containsValue(value);
    }

    public Set<Entry<String, Movie>> entrySet() {
        return library.entrySet();
    }

    public boolean equals(Object arg0) {
        return library.equals(arg0);
    }

    public Movie get(Object key) {
        return library.get(key);
    }

    public int hashCode() {
        return library.hashCode();
    }

    public boolean isEmpty() {
        return library.isEmpty();
    }

    public Set<String> keySet() {
        return library.keySet();
    }

    public Movie put(String key, Movie value) {
        return library.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Movie> m) {
        library.putAll(m);
    }

    public Movie remove(Object key) {
        Movie m = library.remove(key);
        if (m != null) {
            keys.remove(m);
        }
        return m;
    }
    
    public Movie remove(Movie m) {
        String key = keys.get(m);
        return library.remove(key);
    }

    public int size() {
        return library.size();
    }

    public String toString() {
        return library.toString();
    }

    public List<Movie> values() {
        List<Movie> retour = new ArrayList<Movie>(library.values());
        return retour;
    }

    public List<Movie> getMoviesList() {
        return moviesList;
    }

    public void setMoviesList(List<Movie> moviesList) {
        this.moviesList = moviesList;
    }

    public List<Movie> getMoviesByIndexKey(String key) {
        for (Map<String, List<Movie>> index : indexes.values()) {
            List<Movie> movies = index.get(key);
            if (movies != null) {
                return movies;
            }
        }

        return new ArrayList<Movie>();
    }

    public Map<String, Index> getIndexes() {
        return indexes;
    }

    @SuppressWarnings("unchecked")
    private static void fillGenreMap(String xmlGenreFilename) {
        File xmlGenreFile = new File(xmlGenreFilename);
        if (xmlGenreFile.exists() && xmlGenreFile.isFile() && xmlGenreFilename.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(xmlGenreFile);

                List<HierarchicalConfiguration> genres = c.configurationsAt("genre");
                for (HierarchicalConfiguration genre : genres) {
                    String masterGenre = genre.getString("[@name]");
                    // logger.debug("New masterGenre parsed : (" + masterGenre+ ")");
                    List<String> subgenres = genre.getList("subgenre");
                    for (String subgenre : subgenres) {
                        // logger.debug("New genre added to map : (" + subgenre+ "," + masterGenre+ ")");
                        genresMap.put(subgenre, masterGenre);
                    }

                }
            } catch (Exception error) {
                logger.error("Failed parsing moviejukebox genre input file: " + xmlGenreFile.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
        } else {
            logger.error("The moviejukebox genre input file you specified is invalid: " + xmlGenreFile.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static void fillCertificationMap(String xmlCertificationFilename) {
        File xmlCertificationFile = new File(xmlCertificationFilename);
        if (xmlCertificationFile.exists() && xmlCertificationFile.isFile() && xmlCertificationFilename.toUpperCase().endsWith("XML")) {
            try {
                XMLConfiguration conf = new XMLConfiguration(xmlCertificationFile);

                List<HierarchicalConfiguration> certifications = conf.configurationsAt("certification");
                for (HierarchicalConfiguration certification : certifications) {
                    String masterCertification = certification.getString("[@name]");
                    List<String> subcertifications = certification.getList("subcertification");
                    for (String subcertification : subcertifications) {
                        certificationsMap.put(subcertification, masterCertification);
                    }

                }
                if (conf.containsKey("default")) {
                    defaultCertification = conf.getString("default");
                    logger.info("Found default certification: " + defaultCertification);
                }
            } catch (Exception error) {
                logger.error("Failed parsing moviejukebox certification input file: " + xmlCertificationFile.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
        } else {
            logger.error("The moviejukebox certification input file you specified is invalid: " + xmlCertificationFile.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static void fillCategoryMap(String xmlCategoryFilename) {
        File xmlFile = new File(xmlCategoryFilename);
        if (xmlFile.exists() && xmlFile.isFile() && xmlCategoryFilename.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(xmlFile);

                List<HierarchicalConfiguration> categories = c.configurationsAt("category");
                for (HierarchicalConfiguration category : categories) {
                    boolean enabled = Boolean.parseBoolean(category.getString("enable", "true"));

                    if (enabled) {
                        String origName = category.getString("[@name]");
                        String newName = category.getString("rename", origName);
                        categoriesMap.put(origName, newName);
                        //logger.debug("Added category '" + origName + "' with name '" + newName + "'");
                    }
                }
            } catch (Exception error) {
                logger.error("Failed parsing moviejukebox category input file: " + xmlFile.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
        } else {
            logger.error("The moviejukebox category input file you specified is invalid: " + xmlFile.getName());
        }
    }
    
    public Map<String, String> getCategoriesMap() {
        return categoriesMap;
    }

    /**
     * Find the first category in the first index that has any movies in it
     * For Issue 436
     */
    public String getDefaultCategory() {
        for (Index index : indexes.values()) {
            for (String cat : categoriesMap.values()) {
                if (index.containsKey(cat) && index.get(cat).size() > 0) {
                    return cat;
                }
            }
        }
        return null;
    }

    static LastModifiedComparator cmpLast = new LastModifiedComparator();
    static Top250Comparator cmp250 = new Top250Comparator();
    static RatingComparator cmpRating = new RatingComparator();
    static RatingsComparator cmpRatings = new RatingsComparator();

    protected static Comparator<Movie> getComparator(String category, String key) {
        Comparator<Movie> cmpMovie = null;
        
        if (category.equals(SET)) {
            cmpMovie = new MovieSetComparator(key);
        } else if (category.equals(INDEX_OTHER)) {
            if (key.equals(categoriesMap.get(INDEX_NEW)) || 
                    key.equals(categoriesMap.get(INDEX_NEW_TV)) || 
                    key.equals(categoriesMap.get(INDEX_NEW_MOVIE))) {
                cmpMovie = cmpLast;
            } else if (key.equals(categoriesMap.get(INDEX_TOP250))) {
                cmpMovie = cmp250;
            } else if (key.equals(categoriesMap.get(INDEX_RATING))) {
                cmpMovie = cmpRating;
            }
        } else if (category.equals(INDEX_RATINGS)) {
            cmpMovie = cmpRatings;
        }

        return cmpMovie;
    }
    
    /**
     * Find the un-modified category name.
     * The Category name could be changed by the use of the Category XML file.
     * This function will return the original, unchanged name
     * @param newCategory
     * @return
     */
    public static String getOriginalCategory(String newCategory) {
        for (Map.Entry<String, String> singleCategory : categoriesMap.entrySet()) {
            if (singleCategory.getValue().equals(newCategory)) {
                return singleCategory.getKey();
            }
        }
        
        return Movie.UNKNOWN;
    }
    
    /**
     * Find the renamed category name from the original name
     * The Category name could be changed by the use of the Category XML file.
     * This function will return the new name.
     * @param test
     * @return
     */
    public static String getRenamedCategory(String newCategory) {
        return categoriesMap.get(newCategory);
    }
    
    public static boolean isFilterGenres() {
        return filterGenres;
    }

    public static void setFilterGenres(boolean filterGenres) {
        Library.filterGenres = filterGenres;
    }

    public static boolean isSingleSeriesPage() {
        return singleSeriesPage;
    }

    public static void setSingleSeriesPage(boolean singleSeriesPage) {
        Library.singleSeriesPage = singleSeriesPage;
    }

    public static Collection<String> getPrefixes() {
        return Arrays.asList(new String[] { INDEX_OTHER.toUpperCase(), 
                        INDEX_CERTIFICATION.toUpperCase(), 
                        INDEX_TITLE.toUpperCase(), 
                        INDEX_YEAR.toUpperCase(), 
                        INDEX_GENRES.toUpperCase(), 
                        INDEX_SET.toUpperCase(), 
                        INDEX_LIBRARY.toUpperCase(), 
                        INDEX_CAST.toUpperCase(), 
                        INDEX_DIRECTOR.toUpperCase(), 
                        INDEX_COUNTRY.toUpperCase(), 
                        INDEX_CATEGORIES.toUpperCase(), 
                        INDEX_AWARD.toUpperCase(), 
                        INDEX_PERSON.toUpperCase(), 
                        INDEX_RATINGS.toUpperCase() });
    }

    /**
     * Determine the year banding for the category. If the year is this year or last year, return those, otherwise return the decade the year resides in
     * 
     * @param filmYear
     *            The year to check
     * @return "This Year", "Last Year" or the decade range (1990-1999)
     */
    public static String getYearCategory(String filmYear) {
        String yearCat = Movie.UNKNOWN;
        if (StringTools.isValidString(filmYear)) {
            try {
                if (filmYear.equals(String.valueOf(currentYear))) {
                    yearCat = "This Year";
                } else if (filmYear.equals(String.valueOf(currentYear - 1))) {
                    yearCat = "Last Year";
                } else {
                    String beginYear = new String(filmYear.substring(0, filmYear.length() - 1)) + "0";
                    String endYear = Movie.UNKNOWN;
                    if (Integer.parseInt(filmYear) >= currentDecade) {
                        // The film year is in the current decade, so we need to adjust the end year
                        endYear = String.valueOf(finalYear);
                    } else {
                        // Otherwise it's 9
                        endYear = new String(filmYear.substring(0, filmYear.length() - 1)) + "9";
                    }
                    yearCat = beginYear + "-" + new String(endYear.substring(endYear.length() - 2));
                }
            } catch (Exception ignore) {
            }
        }

        return yearCat;
    }

    public List<Movie> getMatchingMoviesList(String indexName, List<Movie> boxedSetMovies, String categorie) {
        List<Movie> response = new ArrayList<Movie>();
        List<Movie> list = this.unCompressedIndexes.get(indexName).get(categorie);
        for (Movie movie : boxedSetMovies) {
            if (list.contains(movie)) {
                logger.debug("Movie " + movie.getTitle() + " match for " + indexName + "[" + categorie + "]");
                response.add(movie);
            }
        }
        return response;
    }

    public void addGeneratedIndex(IndexInfo index) {
        generatedIndexes.add(index);
    }

    public Collection<IndexInfo> getGeneratedIndexes() {
        return generatedIndexes;
    }

    public static String getPersonKey(Person person) {
        String key = person.getName() + "/" + person.getId();
        key = key.toLowerCase();
        return key;
    }

    public void addPerson(String key, Person person) {
        if (person != null) {
            Person existingPerson = getPerson(key);
            if (existingPerson == null) {
                people.put(key, person);
            }
        }
    }

    public void addPerson(Person person) {
        addPerson(getPersonKey(person), person);
    }

    public Collection<Person> getPeople() {
        return people.values();
    }

    public void setPeople(Collection<Person> people) {
        people.clear();
        for (Person person : people) {
            addPerson(person);
        }
    }

    public Person getPerson(String key) {
        return people.get(key);
    }

    public Person getPerson(Person person) {
        return people.get(getPersonKey(person));
    }

    public Person getPersonByName(String name) {
        for (Person person : people.values()) {
            if (person.getName().equalsIgnoreCase(name)) {
                return person;
            }
        }
        return null;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty() {
        isDirty = true;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public void toggleDirty(boolean dirty) {
        isDirty |= dirty;
    }

    public void addDirtyLibrary(String name) {
        if (StringTools.isValidString(name) && !dirtyLibraries.contains(name)) {
            dirtyLibraries.add(name);
            setDirty();
        }
    }

    public boolean isDirtyLibrary(String name) {
        return StringTools.isValidString(name) && dirtyLibraries.contains(name);
    }

}
