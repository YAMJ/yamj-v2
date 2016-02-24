/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
package com.moviejukebox.model;

import static com.moviejukebox.tools.FileTools.createCategoryKey;
import static com.moviejukebox.tools.FileTools.createPrefix;
import static com.moviejukebox.tools.FileTools.makeSafeFilename;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;

import com.moviejukebox.model.Comparator.*;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Library implements Map<String, Movie> {

    private static final Logger LOG = LoggerFactory.getLogger(Library.class);
    // Constants
    public static final String TV_SERIES = "TVSeries";
    public static final String SET = "Set";
    // Library values
    private final Collection<IndexInfo> generatedIndexes = Collections.synchronizedCollection(new ArrayList<IndexInfo>());
    private static boolean filterGenres;
    private static final boolean FILTER_CERTIFICAION;
    private static boolean singleSeriesPage;
    private static final List<String> CERTIFICATION_ORDERING = new ArrayList<>();
    private static final List<String> LIBRARY_ORDERING = new ArrayList<>();
    private static final Map<String, String> GENRES_MAP = new HashMap<>();
    private static final Map<String, String> CERTIFICATIONS_MAP = new HashMap<>();
    private static String defaultCertification = null;
    private static final Map<String, String> CATEGORIES_MAP = new LinkedHashMap<>(); // This is a LinkedHashMap to ensure that the order that the items are inserted into the Map is retained
    private static boolean charGroupEnglish = false;
    private final Map<Movie, String> keys = new HashMap<>();
    private final Map<String, Movie> library = new TreeMap<>();
    private final Map<String, Movie> extras = new TreeMap<>();
    private List<Movie> moviesList = new ArrayList<>();
    private final Map<String, Index> indexes;
    private Map<String, Index> unCompressedIndexes = new LinkedHashMap<>();
    private static final DecimalFormat PADDED_FORMAT = new DecimalFormat("000"); // Issue 190
    private static int categoryMinCountMaster = 3;
    private static int categoryMaxCountMaster = 0;
    private static int movieMaxCountMaster = 0;
    private static int maxGenresPerMovie = 3;
    private static int newMovieCount;
    private static long newMovieDays;
    private static int newTvCount;
    private static long newTvDays;
    private static int minSetCount = 2;
    private static boolean setsRequireAll = false;
    private static final List<String> CATEGORIES_EXPLODE_SET;
    private static boolean removeExplodeSet = false;
    private static boolean keepTVExplodeSet = true;
    private static boolean beforeSortExplodeSet = false;
    private static boolean removeTitleExplodeSet = false;
    private static String setsRating = "first";
    private static final String INDEX_LIST;
    private static final List<String> AWARD_EVENT_LIST;
    private static final List<String> AWARD_NAME_LIST;
    private static final List<String> AWARD_NOMINATED;
    private static final List<String> AWARD_WON;
    private static boolean scrapeWonAwards = false;
    private static boolean splitHD = false;
    private static boolean processExtras = true;
    private static boolean hideWatched = true;
    private static final boolean ENABLE_WATCH_SCANNER;
    private static final List<String> DIRTY_LIBRARIES = new ArrayList<>();
    // Issue 1897: Cast enhancement
    private final Map<String, Person> people;
    private boolean isDirty = false;
    private static boolean peopleScan = false;
    private static boolean peopleExclusive = false;
    private static boolean completePerson = true;
    // Static values for the year indexes
    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
    private static final int FINAL_YEAR = CURRENT_YEAR - 2;
    private static final int CURRENT_DECADE = (FINAL_YEAR / 10) * 10;
    // Sorting params
    private static final Map<String, String> SORT_KEYS = new HashMap<>();
    private static final Map<String, Boolean> SORT_ASC = new HashMap<>();
    private static final List<String> SORT_COMP = new ArrayList<>();
    // Index Names
    public static final String INDEX_3D = "3D";
    public static final String INDEX_ACTOR = "Actor";
    public static final String INDEX_ALL = "All";
    public static final String INDEX_AWARD = "Award";
    public static final String INDEX_CAST = "Cast";
    public static final String INDEX_CATEGORIES = "Categories";
    public static final String INDEX_CERTIFICATION = "Certification";
    public static final String INDEX_COUNTRY = "Country";
    public static final String INDEX_DIRECTOR = "Director";
    public static final String INDEX_EXTRAS = "Extras";
    public static final String INDEX_GENRES = "Genres";
    public static final String INDEX_HD = "HD";
    public static final String INDEX_HD1080 = "HD-1080";
    public static final String INDEX_HD720 = "HD-720";
    public static final String INDEX_LIBRARY = "Library";
    public static final String INDEX_MOVIES = "Movies";
    public static final String INDEX_NEW = "New";
    public static final String INDEX_NEW_MOVIE = "New-Movie";
    public static final String INDEX_NEW_TV = "New-TV";
    public static final String INDEX_OTHER = "Other";
    public static final String INDEX_PERSON = "Person";
    public static final String INDEX_RATING = "Rating";
    public static final String INDEX_RATINGS = "Ratings";
    public static final String INDEX_SET = "Set";
    public static final String INDEX_SETS = "Sets";
    public static final String INDEX_TITLE = "Title";
    public static final String INDEX_TOP250 = "Top250";
    public static final String INDEX_TVSHOWS = "TV Shows";
    public static final String INDEX_UNWATCHED = "Unwatched";
    public static final String INDEX_WATCHED = "Watched";
    public static final String INDEX_WRITER = "Writer";
    public static final String INDEX_YEAR = "Year";
    // Literal Strings
    private static final String ADDING_TO_LIST = "Adding {} to {} list for {}";

    static {
        categoryMinCountMaster = PropertiesUtil.getIntProperty("mjb.categories.minCount", 3);
        categoryMaxCountMaster = PropertiesUtil.getIntProperty("mjb.categories.maxCount", 0);
        movieMaxCountMaster = PropertiesUtil.getIntProperty("mjb.movies.maxCount", 0);
        minSetCount = PropertiesUtil.getIntProperty("mjb.sets.minSetCount", 2);
        setsRequireAll = PropertiesUtil.getBooleanProperty("mjb.sets.requireAll", Boolean.FALSE);
        setsRating = PropertiesUtil.getProperty("mjb.sets.rating", "first");
        CATEGORIES_EXPLODE_SET = Arrays.asList(PropertiesUtil.getProperty("mjb.categories.explodeSet", "").split(","));
        removeExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet", Boolean.FALSE);
        keepTVExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.keepTV", Boolean.TRUE);
        beforeSortExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.beforeSort", Boolean.FALSE);
        removeTitleExplodeSet = PropertiesUtil.getBooleanProperty("mjb.categories.explodeSet.removeSet.title", Boolean.FALSE);
        singleSeriesPage = PropertiesUtil.getBooleanProperty("mjb.singleSeriesPage", Boolean.FALSE);
        INDEX_LIST = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Certification,Year,Library,Set");
        String awardTmp = PropertiesUtil.getProperty("mjb.categories.award.events", "");
        AWARD_EVENT_LIST = StringTools.isValidString(awardTmp) ? Arrays.asList(awardTmp.split(Movie.SPACE_SLASH_SPACE)) : new ArrayList<String>();
        awardTmp = PropertiesUtil.getProperty("mjb.categories.award.name", "");
        AWARD_NAME_LIST = StringTools.isValidString(awardTmp) ? Arrays.asList(awardTmp.split(Movie.SPACE_SLASH_SPACE)) : new ArrayList<String>();
        awardTmp = PropertiesUtil.getProperty("mjb.categories.award.nominated", "");
        AWARD_NOMINATED = StringTools.isValidString(awardTmp) ? Arrays.asList(awardTmp.split(Movie.SPACE_SLASH_SPACE)) : new ArrayList<String>();
        awardTmp = PropertiesUtil.getProperty("mjb.categories.award.won", "");
        AWARD_WON = StringTools.isValidString(awardTmp) ? Arrays.asList(awardTmp.split(Movie.SPACE_SLASH_SPACE)) : new ArrayList<String>();
        scrapeWonAwards = PropertiesUtil.getProperty("mjb.scrapeAwards", FALSE).equalsIgnoreCase("won");
        splitHD = PropertiesUtil.getBooleanProperty("highdef.differentiate", Boolean.FALSE);
        processExtras = PropertiesUtil.getBooleanProperty("filename.extras.process", Boolean.TRUE);
        hideWatched = PropertiesUtil.getBooleanProperty("mjb.Library.hideWatched", Boolean.TRUE);
        ENABLE_WATCH_SCANNER = PropertiesUtil.getBooleanProperty("watched.scanner.enable", Boolean.TRUE);
        filterGenres = PropertiesUtil.getBooleanProperty("mjb.filter.genres", Boolean.FALSE);
        fillGenreMap(PropertiesUtil.getProperty("mjb.xmlGenreFile", "genres-default.xml"));

        FILTER_CERTIFICAION = PropertiesUtil.getBooleanProperty("mjb.filter.certification", Boolean.FALSE);
        fillCertificationMap(PropertiesUtil.getProperty("mjb.xmlCertificationFile", "certification-default.xml"));

        maxGenresPerMovie = PropertiesUtil.getIntProperty("genres.max", 3);

        {
            String certificationOrder = PropertiesUtil.getProperty("certification.ordering");
            if (StringUtils.isNotBlank(certificationOrder)) {
                for (String cert : certificationOrder.split(",")) {
                    CERTIFICATION_ORDERING.add(cert.trim());
                }
            }
        }

        fillCategoryMap(PropertiesUtil.getProperty("mjb.xmlCategoryFile", "categories-default.xml"));

        charGroupEnglish = PropertiesUtil.getBooleanProperty("indexing.character.groupEnglish", Boolean.FALSE);
        completePerson = PropertiesUtil.getBooleanProperty("indexing.completePerson", Boolean.TRUE);
        peopleScan = PropertiesUtil.getBooleanProperty("mjb.people", Boolean.FALSE);
        peopleExclusive = PropertiesUtil.getBooleanProperty("mjb.people.exclusive", Boolean.FALSE);

        populateSortOrder();

        getNewCategoryProperties();
    }

    public Library() {
        this.people = new TreeMap<>();
        this.indexes = new LinkedHashMap<>();
    }

    /**
     * Create the sort order for the indexes
     */
    private static void populateSortOrder() {
        // Compile the sorting comparator list
        if (SORT_COMP.isEmpty()) {
            SORT_COMP.add(INDEX_NEW.toLowerCase());
            SORT_COMP.add(INDEX_TITLE.toLowerCase());
            SORT_COMP.add(INDEX_RATING.toLowerCase());
            SORT_COMP.add(INDEX_TOP250.toLowerCase());
            SORT_COMP.add(INDEX_YEAR.toLowerCase());
        }

        LOG.debug("Valid sort types are: {}", SORT_COMP.toString());

        if (SORT_KEYS.isEmpty()) {
            setSortProperty(INDEX_PERSON, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_CAST, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_DIRECTOR, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_WRITER, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_RATINGS, INDEX_RATING, Boolean.FALSE);
            setSortProperty(INDEX_GENRES, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_TITLE, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_CERTIFICATION, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_YEAR, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_LIBRARY, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_COUNTRY, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_AWARD, INDEX_TITLE, Boolean.TRUE);

            setSortProperty(INDEX_RATING, INDEX_RATING, Boolean.FALSE);
            setSortProperty(INDEX_HD, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_HD1080, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_HD720, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_3D, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_WATCHED, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_UNWATCHED, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_ALL, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_TVSHOWS, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_MOVIES, INDEX_TITLE, Boolean.TRUE);
            setSortProperty(INDEX_TOP250, INDEX_TOP250, Boolean.TRUE);

            // Sort the new categories by descending order
            setSortProperty(INDEX_NEW, INDEX_NEW, Boolean.FALSE);
            setSortProperty(INDEX_NEW_MOVIE, INDEX_NEW, Boolean.FALSE);
            setSortProperty(INDEX_NEW_TV, INDEX_NEW, Boolean.FALSE);
        }

        LOG.debug("Library sorting:");
        for (Entry<String, String> sk : SORT_KEYS.entrySet()) {
            LOG.debug("  Category='{}', OrderBy='{}' {}",
                    sk.getKey(),
                    sk.getValue(),
                    SORT_ASC.get(sk.getKey()) ? " (Ascending)" : " (Descending)");
        }
    }

    /**
     * Populate the index sorting with the key and the comparator.
     *
     * @param indexKey
     * @param defaultSort
     * @param defaultOrder
     */
    private static void setSortProperty(String indexKey, String defaultSort, boolean defaultOrder) {
        String spIndexKey;

        if (indexKey.contains(" ")) {
            spIndexKey = indexKey.replaceAll(" ", "");
        } else {
            spIndexKey = indexKey;
        }
        spIndexKey = spIndexKey.toLowerCase();

        String sortType = PropertiesUtil.getProperty("indexing.sort." + spIndexKey, defaultSort).toLowerCase();

        if (StringTools.isNotValidString(sortType) || !SORT_COMP.contains(sortType)) {
            LOG.warn("Invalid sort type '{}' for category '{}' using default of {}", sortType, spIndexKey, defaultSort);
            sortType = defaultSort.toLowerCase();
        }

        SORT_KEYS.put(indexKey, sortType);
        SORT_ASC.put(indexKey, PropertiesUtil.getBooleanProperty("indexing.sort." + spIndexKey + ".asc", defaultOrder));
    }

    /**
     * Calculate the Movie and TV New category properties
     */
    private static void getNewCategoryProperties() {
        newMovieDays = PropertiesUtil.getReplacedLongProperty("mjb.newdays.movie", "mjb.newdays", 7);
        newMovieCount = PropertiesUtil.getReplacedIntProperty("mjb.newcount.movie", "mjb.newcount", 0);

        newTvDays = PropertiesUtil.getReplacedLongProperty("mjb.newdays.tv", "mjb.newdays", 7);
        newTvCount = PropertiesUtil.getReplacedIntProperty("mjb.newcount.tv", "mjb.newcount", 0);

        if (newMovieDays > 0) {
            LOG.debug("New Movie category will have {} most recent movies in the last {} days", newMovieCount > 0 ? ("the " + newMovieCount) : "all of the", newMovieDays);
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newMovieDays = TimeUnit.DAYS.toMillis(newMovieDays);
        } else {
            LOG.debug("New Movie category is disabled");
        }

        if (newTvDays > 0) {
            LOG.debug("New TV category will have {} most recent TV Shows in the last {} days", newTvCount > 0 ? ("the " + newTvCount) : "all of the", newTvDays);
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newTvDays = TimeUnit.DAYS.toMillis(newTvDays);
        } else {
            LOG.debug("New category is disabled");
        }
    }

    public static String getMovieKey(IMovieBasicInformation movie) {
        // Issue 190
        StringBuilder key = new StringBuilder(movie.getTitle());
        key.append(" (").append(movie.getYear()).append(")");

        if (movie.isTVShow()) {
            // Issue 190
            key.append(" Season ");
            key.append(PADDED_FORMAT.format(movie.getSeason()));
        }

        return key.toString().toLowerCase();
    }

    public static List<String> getLibraryOrdering() {
        return LIBRARY_ORDERING;
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
            if (movie.getLibraryDescription().length() > 0 && !LIBRARY_ORDERING.contains(movie.getLibraryDescription())) {
                LIBRARY_ORDERING.add(movie.getLibraryDescription());
            }
        } else {
            MovieFile firstMovieFile = movie.getFirstFile();
            // Take care of TV-Show (order by episode). Issue 535 - Not sure it's the best place do to this.
            if (existingMovie.isTVShow() || existingMovie.getMovieFiles().size() > 1) {
                // The lower episode have to be the main movie.
                int newEpisodeNumber = firstMovieFile.getFirstPart();
                int oldEpisodesFirstNumber = Integer.MAX_VALUE;
                Collection<MovieFile> espisodesFiles = existingMovie.getFiles();
                for (MovieFile movieFile : espisodesFiles) {
                    if (movieFile.getFirstPart() < oldEpisodesFirstNumber) {
                        oldEpisodesFirstNumber = movieFile.getFirstPart();
                    }
                }
                // If the new episode is < than old ( episode 1 < episode 2)
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
                // Issue 2089:  cumulate filesize for a TV Show season.
                existingMovie.setFileSize(movie.getFileSize());
                // Set max file date (by calling this method)
                existingMovie.getLastModifiedTimestamp();
            } else {
                existingMovie.addMovieFile(firstMovieFile);
                // Update these counters
                existingMovie.setFileSize(movie.getFileSize());
                // Set file date
                existingMovie.addFileDate(movie.getFileDate());
            }
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

    protected static Map<String, Movie> buildIndexMasters(String prefix, Index index) {
        Map<String, Movie> masters = new HashMap<>();
        boolean dirtyInfo = Boolean.FALSE;

        for (Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
            String indexName = indexEntry.getKey();
            List<Movie> indexMovieList = indexEntry.getValue();

            // Issue 2098: put to SET information from first movie by order
            int setIndex = 0;
            if (!indexMovieList.get(setIndex).isTVShow() && indexMovieList.get(setIndex).getSetOrder(indexName) != null) {
                int setOrder = indexMovieList.get(setIndex).getSetOrder(indexName);
                if (setOrder > 1) {
                    for (int i = 1; i < indexMovieList.size(); i++) {
                        if ((indexMovieList.get(i).getSetOrder(indexName) != null) && setOrder > indexMovieList.get(i).getSetOrder(indexName)) {
                            setOrder = indexMovieList.get(i).getSetOrder(indexName);
                            setIndex = i;
                        }
                    }
                }
            }

            // We can't clone the movie because of the Collection objects in there, so we'll have to copy it
            Movie indexMaster = Movie.newInstance(indexMovieList.get(setIndex));

            indexMaster.setSetMaster(true);
            indexMaster.setSetSize(indexMovieList.size());
            indexMaster.setTitle(indexName, indexMaster.getOverrideSource(OverrideFlag.TITLE));

            // Overwrite the TitleSort with the indexname only for TV Shows as this will overwrite with changes that are made in a NFO file
            if (!indexMaster.isTVShow()) {
                indexMaster.setTitleSort(indexName);
            }
            indexMaster.setOriginalTitle(indexName, indexMaster.getOverrideSource(OverrideFlag.ORIGINALTITLE));
            indexMaster.setBaseFilename(createPrefix(prefix, createCategoryKey(indexName)) + "1");
            indexMaster.setBaseName(makeSafeFilename(indexMaster.getBaseFilename()));

            // set TV and HD properties of the master
            int countTV = 0;
            int countHD = 0;
            int top250 = -1;
            String top250source = Movie.UNKNOWN;
            boolean watched = true; // Assume watched for the check, because any false value will reset it.
            int maxRating = 0;
            int sumRating = 0;
            int currentRating;

            // Clear any set dirty flags and just use those from the files.
            indexMaster.clearDirty();

            // We Can't use a TreeSet because MF.compareTo just compares part #
            // so it fails when we combine multiple seasons into one collection
            Collection<MovieFile> masterMovieFileCollection = new LinkedList<>();
            for (Movie movie : indexMovieList) {
                if (movie.isTVShow()) {
                    countTV++;
                }

                if (movie.isHD()) {
                    countHD++;
                }

                // If watched is false for any part, then set the master to unwatched
                watched &= movie.isWatched();

                int mTop250 = movie.getTop250();
                if (mTop250 > 0 && (top250 < 0 || mTop250 < top250)) {
                    top250 = mTop250;
                    top250source = movie.getOverrideSource(OverrideFlag.TOP250);
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

                // Check the dirty status of the movie to see if the set needs to be updated
                indexMaster.getDirty().addAll(movie.getDirty());
                // the dirty info flag is overwritten by
                dirtyInfo |= movie.isDirty(DirtyFlag.INFO);
            }

            indexMaster.setMovieType(countTV > 0 ? Movie.TYPE_TVSHOW : Movie.TYPE_MOVIE);
            indexMaster.setVideoType(countHD > 0 ? Movie.TYPE_VIDEO_HD : null);
            indexMaster.setWatchedFile(watched);
            indexMaster.setTop250(top250, top250source);

            if (setsRating.equalsIgnoreCase("max") || (setsRating.equalsIgnoreCase("average") && (!indexMovieList.isEmpty()))) {
                Map<String, Integer> ratings = new HashMap<>();
                ratings.put("setrating", setsRating.equalsIgnoreCase("max") ? maxRating : (sumRating / indexMovieList.size()));
                indexMaster.setRatings(ratings);
            }

            indexMaster.setMovieFiles(masterMovieFileCollection);
            // Keep the current dirty setting
            indexMaster.setDirty(DirtyFlag.INFO, dirtyInfo);

            masters.put(indexName, indexMaster);

            if (LOG.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Setting index master '");
                sb.append(indexMaster.getTitle());
                sb.append("' - isTV: ").append(indexMaster.isTVShow());
                sb.append(" (").append(countTV).append("/").append(indexMovieList.size()).append(")");
                sb.append(" - isHD: ").append(indexMaster.isHD());
                sb.append(" (").append(countHD).append("/").append(indexMovieList.size()).append(")");
                sb.append(" - top250: ").append(indexMaster.getTop250());
                sb.append(" - watched: ").append(indexMaster.isWatched());
                sb.append(" - rating: ").append(indexMaster.getRating());
                sb.append(" - dirty: ").append(indexMaster.showDirty());
                LOG.debug(sb.toString());
            }

        }

        return masters;
    }

    protected static void compressSetMovies(List<Movie> movies, Index index, Map<String, Movie> masters, String indexName, String subIndexName) {
        // Construct an index that includes only the intersection of movies and index
        Index inMovies = new Index();
        for (Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
            for (Movie m : indexEntry.getValue()) {
                if (movies.contains(m)) {
                    inMovies.addMovie(indexEntry.getKey(), m);
                }
            }
        }

        // Now, for each list of movies in in_movies, if the list has more than the minSetCount movies
        // remove them all from the movies list, and insert the corresponding master
        for (Map.Entry<String, List<Movie>> inMoviesEntry : inMovies.entrySet()) {
            List<Movie> lm = inMoviesEntry.getValue();
            if (lm.size() >= minSetCount && (!setsRequireAll || lm.size() == index.get(inMoviesEntry.getKey()).size())) {
                boolean tvSet = keepTVExplodeSet && lm.get(0).isTVShow();
                boolean explodeSet = CATEGORIES_EXPLODE_SET.contains(indexName) || (indexName.equalsIgnoreCase(INDEX_OTHER) && CATEGORIES_EXPLODE_SET.contains(subIndexName));
                if (!beforeSortExplodeSet || !explodeSet || tvSet) {
                    movies.removeAll(lm);
                }
                if (!beforeSortExplodeSet || !explodeSet || tvSet || !removeExplodeSet) {
                    movies.add(masters.get(inMoviesEntry.getKey()));
                }
            }
        }
    }

    public void buildIndex(ThreadExecutor<Void> tasks) throws Throwable {
        moviesList.clear();
        indexes.clear();

        tasks.restart();
        final List<Movie> indexMovies = new ArrayList<>(library.values());
        moviesList.addAll(library.values());

        if (!indexMovies.isEmpty()) {
            Map<String, Index> dynamicIndexes = new LinkedHashMap<>();
            // Add the sets FIRST! That allows users to put series inside sets
            dynamicIndexes.put(SET, indexBySets(indexMovies));

            final Map<String, Index> syncindexes = Collections.synchronizedMap(indexes);

            for (final String indexStr : INDEX_LIST.split(",")) {
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        SystemTools.showMemory();
                        LOG.info("  Indexing {}...", indexStr);
                        switch (indexStr) {
                            case INDEX_OTHER:
                                syncindexes.put(INDEX_OTHER, indexByProperties(indexMovies));
                                break;
                            case INDEX_GENRES:
                                syncindexes.put(INDEX_GENRES, indexByGenres(indexMovies));
                                break;
                            case INDEX_TITLE:
                                syncindexes.put(INDEX_TITLE, indexByTitle(indexMovies));
                                break;
                            case INDEX_CERTIFICATION:
                                syncindexes.put(INDEX_CERTIFICATION, indexByCertification(indexMovies));
                                break;
                            case INDEX_YEAR:
                                syncindexes.put(INDEX_YEAR, indexByYear(indexMovies));
                                break;
                            case INDEX_LIBRARY:
                                syncindexes.put(INDEX_LIBRARY, indexByLibrary(indexMovies));
                                break;
                            case INDEX_CAST:
                                syncindexes.put(INDEX_CAST, indexByCast(indexMovies));
                                break;
                            case INDEX_DIRECTOR:
                                syncindexes.put(INDEX_DIRECTOR, indexByDirector(indexMovies));
                                break;
                            case INDEX_COUNTRY:
                                syncindexes.put(INDEX_COUNTRY, indexByCountry(indexMovies));
                                break;
                            case INDEX_WRITER:
                                syncindexes.put(INDEX_WRITER, indexByWriter(indexMovies));
                                break;
                            case INDEX_AWARD:
                                syncindexes.put(INDEX_AWARD, indexByAward(indexMovies));
                                break;
                            case INDEX_PERSON:
                                syncindexes.put(INDEX_PERSON, indexByPerson(indexMovies));
                                break;
                            case INDEX_RATINGS:
                                syncindexes.put(INDEX_RATINGS, indexByRatings(indexMovies));
                                break;
                            default:
                                break;
                        }
                        return null;
                    }
                });
            }
            tasks.waitFor();
            SystemTools.showMemory();

            // Make a "copy" of uncompressed index
            this.keepUncompressedIndexes();

            Map<String, Map<String, Movie>> dynamicIndexMasters = new HashMap<>();
            for (Map.Entry<String, Index> dynamicEntry : dynamicIndexes.entrySet()) {
                Map<String, Movie> indexMasters = buildIndexMasters(dynamicEntry.getKey(), dynamicEntry.getValue());
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
            if (INDEX_LIST.contains(INDEX_TITLE)) {
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
            LOG.info("  Sorting Indexes ...");
            for (final Map.Entry<String, Index> indexesEntry : indexes.entrySet()) {
                for (final Map.Entry<String, List<Movie>> indexEntry : indexesEntry.getValue().entrySet()) {
                    tasks.submit(new Callable<Void>() {
                        @Override
                        public Void call() {
                            Comparator<Movie> cmpMovie = getComparator(indexesEntry.getKey(), indexEntry.getKey());
                            if (cmpMovie == null) {
                                Collections.sort(indexEntry.getValue());
                            } else {
                                Collections.sort(indexEntry.getValue(), cmpMovie);
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
            if (CATEGORIES_MAP.get(INDEX_NEW) != null) {
                Index otherIndexes = indexes.get(INDEX_OTHER);
                List<Movie> newList = new ArrayList<>();
                int newMovies = 0;
                int newTVShows = 0;

                if (trimNewMovieOK && (CATEGORIES_MAP.get(INDEX_NEW_MOVIE) != null) && (otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_MOVIE)) != null)) {
                    newList.addAll(otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_MOVIE)));
                    newMovies = otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_MOVIE)).size();
                } else {
                    // Remove the empty "New Movie" category
                    if (CATEGORIES_MAP.get(INDEX_NEW_MOVIE) != null) {
                        otherIndexes.remove(CATEGORIES_MAP.get(INDEX_NEW_MOVIE));
                    }
                }

                if (trimNewTvOK && (CATEGORIES_MAP.get(INDEX_NEW_TV) != null) && (otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_TV)) != null)) {
                    newList.addAll(otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_TV)));
                    newTVShows = otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW_TV)).size();
                } else {
                    // Remove the empty "New TV" category
                    if (CATEGORIES_MAP.get(INDEX_NEW_TV) != null) {
                        otherIndexes.remove(CATEGORIES_MAP.get(INDEX_NEW_TV));
                    }
                }

                // If we have new videos, then create the super "New" category
                if ((newMovies + newTVShows) > 0) {
                    StringBuilder categoryMessage = new StringBuilder("Creating new category with ");
                    if (newMovies > 0) {
                        categoryMessage.append(newMovies).append(" new movie").append(newMovies > 1 ? "s" : "");
                    }
                    if (newTVShows > 0) {
                        categoryMessage.append(newMovies > 0 ? " & " : "");
                        categoryMessage.append(newTVShows).append(" new TV Show").append(newTVShows > 1 ? "s" : "");
                    }

                    LOG.debug(categoryMessage.toString());
                    otherIndexes.put(CATEGORIES_MAP.get(INDEX_NEW), newList);
                    Collections.sort(otherIndexes.get(CATEGORIES_MAP.get(INDEX_NEW)), new LastModifiedComparator());
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

        tasks.restart();
        final List<Person> indexPersons = new ArrayList<>(people.values());

        if (!indexPersons.isEmpty()) {
            for (final String indexStr : INDEX_LIST.split(",")) {
                if (!(INDEX_CAST + INDEX_DIRECTOR + INDEX_WRITER + INDEX_PERSON).contains(indexStr)) {
                    continue;
                }
                tasks.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        SystemTools.showMemory();
                        LOG.info("  Indexing {} (person)...", indexStr);
                        indexByJob(indexPersons, indexStr.equals(INDEX_CAST) ? Filmography.DEPT_ACTORS
                                : indexStr.equals(INDEX_DIRECTOR) ? Filmography.DEPT_DIRECTING
                                        : indexStr.equals(INDEX_WRITER) ? Filmography.DEPT_WRITING : Movie.UNKNOWN, indexStr);
                        return null;
                    }
                });
            }
            tasks.waitFor();
            SystemTools.showMemory();
        }
    }

    /**
     * Trim the new category to the required length, add the trimmed video list to the NEW category
     *
     * @param catName The name of the category: "New-TV" or "New-Movie"
     * @param catCount The maximum size of the category
     * @return
     */
    private boolean trimNewCategory(String catName, int catCount) {
        boolean trimOK = true;
        String category = CATEGORIES_MAP.get(catName);
        //logger.info("Trimming '" + catName + "' ('" + category + "') to " + catCount + " videos");
        if (catCount > 0 && category != null) {
            Index otherIndexes = indexes.get(INDEX_OTHER);
            if (otherIndexes != null) {
                List<Movie> newList = otherIndexes.get(category);
                //logger.info("Current size of '" + catName + "' ('" + category + "') is " + (newList != null ? newList.size() : "NULL"));
                if ((newList != null) && (newList.size() > catCount)) {
                    newList = newList.subList(0, catCount);
                    otherIndexes.put(category, newList);
                }
            } else {
                LOG.warn("Warning : You need to enable index 'Other' to get '{}' ('{}') category", catName, category);
                trimOK = false;
            }
        }

        return trimOK;
    }

    private void keepUncompressedIndexes() {
        this.unCompressedIndexes = new HashMap<>(indexes.size());
        Set<String> indexeskeySet = this.indexes.keySet();
        for (String key : indexeskeySet) {
            LOG.debug("Copying {} indexes", key);
            Index index = this.indexes.get(key);
            Index indexTmp = new Index();

            unCompressedIndexes.put(key, indexTmp);
            for (Map.Entry<String, List<Movie>> keyCategory : index.entrySet()) {
                List<Movie> listMovie = keyCategory.getValue();
                List<Movie> listMovieTmp = new ArrayList<>(listMovie.size());
                indexTmp.put(keyCategory.getKey(), listMovieTmp);

                for (Movie movie : listMovie) {
                    listMovieTmp.add(movie);
                }
            }
        }
    }

    private static void setMovieListNavigation(List<Movie> moviesList) {
        List<Movie> extraList = new ArrayList<>();

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
                movie.setFirst(first == null ? "" : first.getBaseName());

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

                movie.setLast(last == null ? "" : last.getBaseName());
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
        Index index;
        if (CERTIFICATION_ORDERING.isEmpty()) {
            index = new Index();
        } else {
            index = new Index(new CertificationComparator(CERTIFICATION_ORDERING));
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
     * Index the videos by the property values This is slightly different from the other indexes as there may be multiple entries
     * for each of the videos
     *
     * @param moviesList
     * @return
     */
    private static Index indexByProperties(Iterable<Movie> moviesList) {
        Index index = new Index();
        long now = System.currentTimeMillis();
        for (Movie movie : moviesList) {
            if (movie.isExtra()) {
                // Issue 997: Skip the processing of extras
                if (processExtras && CATEGORIES_MAP.get(INDEX_EXTRAS) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_EXTRAS), movie);
                    movie.addIndex(INDEX_EXTRAS, CATEGORIES_MAP.get(INDEX_EXTRAS));
                }
            } else {
                if (movie.isHD()) {
                    if (splitHD) {
                        // Split the HD category into two categories: HD-720 and HD-1080
                        if (movie.isHD1080()) {
                            if (CATEGORIES_MAP.get(INDEX_HD1080) != null) {
                                index.addMovie(CATEGORIES_MAP.get(INDEX_HD1080), movie);
                                movie.addIndex(INDEX_HD, CATEGORIES_MAP.get(INDEX_HD1080));
                            }
                        } else {
                            if (CATEGORIES_MAP.get(INDEX_HD720) != null) {
                                index.addMovie(CATEGORIES_MAP.get(INDEX_HD720), movie);
                                movie.addIndex(INDEX_HD, CATEGORIES_MAP.get(INDEX_HD720));
                            }
                        }
                    } else {
                        if (CATEGORIES_MAP.get(INDEX_HD) != null) {
                            index.addMovie(CATEGORIES_MAP.get(INDEX_HD), movie);
                            movie.addIndex(INDEX_HD, CATEGORIES_MAP.get(INDEX_HD));
                        }
                    }
                }

                if (movie.is3D() && CATEGORIES_MAP.get(INDEX_3D) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_3D), movie);
                    movie.addIndex(INDEX_3D, CATEGORIES_MAP.get(INDEX_3D));
                }

                if (movie.getTop250() > 0 && CATEGORIES_MAP.get(INDEX_TOP250) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_TOP250), movie);
                    movie.addIndex(INDEX_TOP250, CATEGORIES_MAP.get(INDEX_TOP250));
                }

                if (movie.getRating() > 0 && CATEGORIES_MAP.get(INDEX_RATING) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_RATING), movie);
                    movie.addIndex(INDEX_RATING, CATEGORIES_MAP.get(INDEX_RATING));
                }

                if (ENABLE_WATCH_SCANNER) { // Issue 1938 don't create watched/unwatched indexes if scanner is disabled
                    // Add to the Watched or Unwatched category
                    if (movie.isWatched()) {
                        index.addMovie(CATEGORIES_MAP.get(INDEX_WATCHED), movie);
                        movie.addIndex(INDEX_WATCHED, CATEGORIES_MAP.get(INDEX_WATCHED));
                    } else {
                        index.addMovie(CATEGORIES_MAP.get(INDEX_UNWATCHED), movie);
                        movie.addIndex(INDEX_UNWATCHED, CATEGORIES_MAP.get(INDEX_UNWATCHED));
                    }
                }

                // Add to the New Movie category
                if (!movie.isTVShow() && (newMovieDays > 0) && (now - movie.getLastModifiedTimestamp() <= newMovieDays) && !(movie.isWatched() && hideWatched && ENABLE_WATCH_SCANNER)) {
                    if (CATEGORIES_MAP.get(INDEX_NEW_MOVIE) != null) {
                        index.addMovie(CATEGORIES_MAP.get(INDEX_NEW_MOVIE), movie);
                        movie.addIndex(INDEX_NEW_MOVIE, CATEGORIES_MAP.get(INDEX_NEW_MOVIE));
                    }
                }

                // Add to the New TV category
                if (movie.isTVShow()
                        && (newTvDays > 0)
                        && (now - movie.getLastModifiedTimestamp() <= newTvDays)
                        && !(movie.isWatched() && hideWatched && ENABLE_WATCH_SCANNER)
                        && CATEGORIES_MAP.get(INDEX_NEW_TV) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_NEW_TV), movie);
                    movie.addIndex(INDEX_NEW_TV, CATEGORIES_MAP.get(INDEX_NEW_TV));
                }

                if (CATEGORIES_MAP.get(INDEX_ALL) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_ALL), movie);
                    movie.addIndex(INDEX_ALL, CATEGORIES_MAP.get(INDEX_ALL));
                }

                if (movie.isTVShow()) {
                    if (CATEGORIES_MAP.get(INDEX_TVSHOWS) != null) {
                        index.addMovie(CATEGORIES_MAP.get(INDEX_TVSHOWS), movie);
                        movie.addIndex(INDEX_TVSHOWS, CATEGORIES_MAP.get(INDEX_TVSHOWS));
                    }
                } else {
                    if (CATEGORIES_MAP.get(INDEX_MOVIES) != null) {
                        index.addMovie(CATEGORIES_MAP.get(INDEX_MOVIES), movie);
                        movie.addIndex(INDEX_MOVIES, CATEGORIES_MAP.get(INDEX_MOVIES));
                    }
                }

                if (!movie.isTVShow() && (!movie.getSetsKeys().isEmpty()) && CATEGORIES_MAP.get(INDEX_SETS) != null) {
                    index.addMovie(CATEGORIES_MAP.get(INDEX_SETS), movie);
                    movie.addIndex(INDEX_SETS, CATEGORIES_MAP.get(INDEX_SETS));
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

                for (String setKey : movie.getSetsKeys()) {
                    index.addMovie(setKey, movie);
                    movie.addIndex(INDEX_SET, setKey);
                }
            }
        }

        return index;
    }

    protected static Index indexByCast(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (peopleScan && peopleExclusive) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase(Filmography.DEPT_ACTORS) || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String actor = person.getTitle();
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "cast", actor);
                        index.addMovie(actor, movie);
                        movie.addIndex(INDEX_ACTOR, actor);
                    }
                } else {
                    for (String actor : movie.getCast()) {
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "cast", actor);
                        index.addMovie(actor, movie);
                        movie.addIndex(INDEX_ACTOR, actor);
                    }
                }
            }
        }

        return index;
    }

    protected void indexByJob(List<Person> list, String job, String index) {
        for (Person person : list) {
            if ((StringTools.isValidString(job) && !person.getDepartments().contains(job)) || StringTools.isNotValidString(person.getFilename())) {
                continue;
            }
            String actor = person.getTitle();
            if (getMovieCountForIndex(index, actor) < calcMinCategoryCount(index)) {
                continue;
            }
            person.addIndex(index, actor);
        }
    }

    /**
     * Calculate the minimum count for a category based on it's property value.
     *
     * @param categoryName
     * @return
     */
    public static int calcMinCategoryCount(String categoryName) {
        return calcCategoryCount(categoryName, true, false);
    }

    /**
     * Calculate the maximum count for a category based on it's property value.
     *
     * @param categoryName
     * @return
     */
    public static int calcMaxCategoryCount(String categoryName) {
        return calcCategoryCount(categoryName, false, false);
    }

    /**
     * Calculate the maximum count for a movie based on it's property value.
     *
     * @param categoryName
     * @return
     */
    public static int calcMaxMovieCount(String categoryName) {
        return calcCategoryCount(categoryName, false, true);
    }

    /**
     * Calculate the minimum/maximum count for a category/movie based on it's property value.
     *
     * @param categoryName
     * @param getMinimum
     * @param byMovie
     * @return
     */
    public static int calcCategoryCount(String categoryName, boolean getMinimum, boolean byMovie) {
        StringBuilder propertyName = new StringBuilder("mjb.");
        propertyName.append(byMovie ? "movies." : "categories.");
        propertyName.append(getMinimum ? "minCount." : "maxCount.");
        propertyName.append(categoryName);
        int defaultValue = getMinimum ? categoryMinCountMaster : (byMovie ? movieMaxCountMaster : categoryMaxCountMaster);

        return PropertiesUtil.getIntProperty(propertyName.toString(), defaultValue);
    }

    protected static Index indexByCountry(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                for (String country : movie.getCountries()) {
                    index.addMovie(country, movie);
                    movie.addIndex(INDEX_COUNTRY, country);
                }
            }
        }

        return index;
    }

    protected static Index indexByDirector(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                if (peopleScan && peopleExclusive) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase(Filmography.DEPT_DIRECTING) || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String director = person.getTitle();
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "director", director);
                        index.addMovie(director, movie);
                        movie.addIndex(INDEX_DIRECTOR, director);
                    }
                } else {
                    for (String director : movie.getDirectors()) {
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "director", director);
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
                if (peopleScan && peopleExclusive) {
                    for (Filmography person : movie.getPeople()) {
                        if (!person.getDepartment().equalsIgnoreCase(Filmography.DEPT_WRITING) || (completePerson && StringTools.isNotValidString(person.getFilename()))) {
                            continue;
                        }
                        String writer = person.getTitle();
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "writer", writer);
                        index.addMovie(writer, movie);
                        movie.addIndex(INDEX_WRITER, writer);
                    }
                } else {
                    for (String writer : movie.getWriters()) {
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "writer", writer);
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
                for (AwardEvent awardEvent : movie.getAwards()) {
                    String awardName = awardEvent.getName();
                    boolean found = AWARD_EVENT_LIST.isEmpty() && AWARD_NAME_LIST.isEmpty();
                    if (found || AWARD_EVENT_LIST.contains(awardName) || !AWARD_NAME_LIST.isEmpty() && !found) {
                        for (Award award : awardEvent.getAwards()) {
                            if (AWARD_NAME_LIST.isEmpty() || AWARD_NAME_LIST.contains(award.getName())) {
                                int flag = (scrapeWonAwards ? 0 : ((AWARD_NOMINATED.isEmpty() ? 0 : 8) + (award.getNominations().isEmpty() ? 0 : 4))) + (AWARD_WON.isEmpty() ? 0 : 2) + (award.getWons().isEmpty() ? 0 : 1);
                                found = "145".contains(Integer.toString(flag));
                                if (!found && (flag > 10)) {
                                    for (String nomination : award.getNominations()) {
                                        found = AWARD_NOMINATED.contains(nomination);
                                        if (found) {
                                            break;
                                        }
                                    }
                                }
                                if (!found && !AWARD_WON.isEmpty() && !award.getWons().isEmpty()) {
                                    for (String nomination : award.getWons()) {
                                        found = AWARD_WON.contains(nomination);
                                        if (found) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (found) {
                                break;
                            }
                        }
                    }
                    if (found) {
                        LOG.debug(ADDING_TO_LIST, movie.getTitle(), "award", awardName);
                        index.addMovie(awardName, movie);
                        movie.addIndex(INDEX_AWARD, awardName);
                    }
                }
            }
        }

        return index;
    }

    protected static Index indexByPerson(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (movie.isExtra()) {
                continue;
            }
            for (Filmography person : movie.getPeople()) {
                if (completePerson && StringTools.isNotValidString(person.getFilename())) {
                    continue;
                }
                String name = person.getName();
                LOG.debug(ADDING_TO_LIST, movie.getTitle(), "person", name);
                index.addMovie(name, movie);
                movie.addIndex(INDEX_PERSON, name);
            }
        }

        return index;
    }

    protected static Index indexByRatings(List<Movie> list) {
        LOG.info("INDEXING: Ratings");
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra() && (movie.getRating() > 0)) {
                String sRating = Double.toString(Math.floor((double) movie.getRating() / (double) 10)).replace(".0", "");    // Convert and remove the ".0"
                sRating = sRating + ".0-" + sRating + ".9";
                LOG.debug(ADDING_TO_LIST, movie.getTitle(), "ratings", sRating);
                index.addMovie(sRating, movie);
                movie.addIndex(INDEX_RATINGS, sRating);
            }
        }

        return index;
    }

    public int getMovieCountForIndex(String indexName, String category) {
        Index index = unCompressedIndexes.get(indexName);

        if (index == null) {
            index = indexes.get(indexName);
        }

        if (index == null) {
            return -1;
        }
    
        List<Movie> categoryList = index.get(category);
        if (categoryList != null) {
            return categoryList.size();
        }
        return -1;
    }

    /**
     * Checks if there is a master (will be shown in the index) genre for the specified one.
     *
     * @param genre Genre to find the master for
     * @return Genre itself or master if available.
     */
    public static String getIndexingGenre(String genre) {
        if (!filterGenres) {
            return genre;
        }

        String masterGenre = GENRES_MAP.get(genre);
        if (masterGenre != null) {
            return masterGenre;
        }
        return genre;
    }

    /**
     * Checks if there is a master (will be shown in the index) Certification for the specified one.
     *
     * @param certification Certification to find the master for
     * @return Certification itself or master if available.
     */
    public static String getIndexingCertification(String certification) {
        if (!FILTER_CERTIFICAION) {
            return certification;
        }

        String masterCertification = CERTIFICATIONS_MAP.get(certification);
        if (StringUtils.isNotBlank(masterCertification)) {
            return masterCertification;
        } else if (StringTools.isValidString(defaultCertification)) {
            return defaultCertification;
        } else {
            return certification;
        }
    }

    @Override
    public void clear() {
        library.clear();
        people.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return library.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return library.containsValue(value);
    }

    @Override
    public Set<Entry<String, Movie>> entrySet() {
        return library.entrySet();
    }

    @Override
    public Movie get(Object key) {
        return library.get(key);
    }

    @Override
    public boolean isEmpty() {
        return library.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return library.keySet();
    }

    @Override
    public Movie put(String key, Movie value) {
        return library.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Movie> m) {
        library.putAll(m);
    }

    @Override
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

    @Override
    public int size() {
        return library.size();
    }

    @Override
    public String toString() {
        return library.toString();
    }

    @Override
    public List<Movie> values() {
        return new ArrayList<>(library.values());
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

        return new ArrayList<>();
    }

    public Map<String, Index> getIndexes() {
        return indexes;
    }

    private static void fillGenreMap(String xmlGenreFilename) {
        File xmlGenreFile = new File(xmlGenreFilename);
        if (xmlGenreFile.exists() && xmlGenreFile.isFile() && xmlGenreFilename.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(xmlGenreFile);

                List<HierarchicalConfiguration> genres = c.configurationsAt("genre");
                for (HierarchicalConfiguration genre : genres) {
                    String masterGenre = genre.getString("[@name]");
//                     logger.debug("New masterGenre parsed : (" + masterGenre+ ")");
                    List<Object> subgenres = genre.getList("subgenre");
                    for (Object subgenre : subgenres) {
//                         logger.debug("New genre added to map : (" + subgenre+ "," + masterGenre+ ")");
                        GENRES_MAP.put((String) subgenre, masterGenre);
                    }

                }
            } catch (ConfigurationException error) {
                LOG.error("Failed parsing moviejukebox genre input file: {}", xmlGenreFile.getName());
                LOG.error(SystemTools.getStackTrace(error));
            }
        } else {
            LOG.error("The moviejukebox genre input file you specified is invalid: {}", xmlGenreFile.getName());
        }
    }

    private static void fillCertificationMap(String xmlCertificationFilename) {
        File xmlCertificationFile = new File(xmlCertificationFilename);
        if (xmlCertificationFile.exists() && xmlCertificationFile.isFile() && xmlCertificationFilename.toUpperCase().endsWith("XML")) {
            try {
                XMLConfiguration conf = new XMLConfiguration(xmlCertificationFile);

                List<HierarchicalConfiguration> certifications = conf.configurationsAt("certification");
                for (HierarchicalConfiguration certification : certifications) {
                    String masterCertification = certification.getString("[@name]");
                    List<Object> subcertifications = certification.getList("subcertification");
                    for (Object subcertification : subcertifications) {
                        CERTIFICATIONS_MAP.put((String) subcertification, masterCertification);
                    }

                }
                if (conf.containsKey("default")) {
                    defaultCertification = conf.getString("default");
                    LOG.info("Found default certification: {}", defaultCertification);
                }
            } catch (ConfigurationException error) {
                LOG.error("Failed parsing moviejukebox certification input file: {}", xmlCertificationFile.getName());
                LOG.error(SystemTools.getStackTrace(error));
            }
        } else {
            LOG.error("The moviejukebox certification input file you specified is invalid: {}", xmlCertificationFile.getName());
        }
    }

    private static void fillCategoryMap(String xmlCategoryFilename) {
        File xmlFile = new File(xmlCategoryFilename);
        if (xmlFile.exists() && xmlFile.isFile() && xmlCategoryFilename.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(xmlFile);

                List<HierarchicalConfiguration> categories = c.configurationsAt("category");
                for (HierarchicalConfiguration category : categories) {
                    boolean enabled = Boolean.parseBoolean(category.getString("enable", TRUE));

                    if (enabled) {
                        String origName = category.getString("[@name]");
                        String newName = category.getString("rename", origName);
                        CATEGORIES_MAP.put(origName, newName);
                        //logger.debug("Added category '" + origName + "' with name '" + newName + "'");
                    }
                }
            } catch (ConfigurationException error) {
                LOG.error("Failed parsing moviejukebox category input file: {}", xmlFile.getName());
                LOG.error(SystemTools.getStackTrace(error));
            }
        } else {
            LOG.error("The moviejukebox category input file you specified is invalid: {}", xmlFile.getName());
        }
    }

    public Map<String, String> getCategoriesMap() {
        return CATEGORIES_MAP;
    }

    /**
     * Find the first category in the first index that has any movies in it For Issue 436
     *
     * @return
     */
    public String getDefaultCategory() {
        for (Index index : indexes.values()) {
            for (String cat : CATEGORIES_MAP.values()) {
                if (index.containsKey(cat) && !index.get(cat).isEmpty()) {
                    return cat;
                }
            }
        }
        return null;
    }

    protected static Comparator<Movie> getComparator(String category, String key) {
        Comparator<Movie> cmpMovie = null;

        String originalKey = getOriginalCategory(key, Boolean.TRUE);

        switch (category) {
            case SET:
                cmpMovie = new MovieSetComparator(key);
                break;
            case INDEX_OTHER:
                if (key.equals(CATEGORIES_MAP.get(INDEX_NEW))
                        || key.equals(CATEGORIES_MAP.get(INDEX_NEW_TV))
                        || key.equals(CATEGORIES_MAP.get(INDEX_NEW_MOVIE))) {
                    cmpMovie = new LastModifiedComparator(SORT_ASC.get(originalKey));
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_TOP250))) {
                    cmpMovie = new MovieTop250Comparator(SORT_ASC.get(INDEX_TOP250));
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_ALL))) {
                    cmpMovie = getComparator(INDEX_ALL);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_TVSHOWS))) {
                    cmpMovie = getComparator(INDEX_TVSHOWS);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_MOVIES))) {
                    cmpMovie = getComparator(INDEX_MOVIES);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_WATCHED))) {
                    cmpMovie = getComparator(INDEX_WATCHED);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_UNWATCHED))) {
                    cmpMovie = getComparator(INDEX_UNWATCHED);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_RATING))) {
                    cmpMovie = getComparator(INDEX_RATING);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_HD))) {
                    cmpMovie = getComparator(INDEX_HD);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_HD1080))) {
                    cmpMovie = getComparator(INDEX_HD1080);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_HD720))) {
                    cmpMovie = getComparator(INDEX_HD720);
                } else if (key.equals(CATEGORIES_MAP.get(INDEX_3D))) {
                    cmpMovie = getComparator(INDEX_3D);
                }
                break;
            default:
                cmpMovie = getComparator(category);
                break;
        }

        return cmpMovie;
    }

    /**
     * Get the comparator for the category.
     *
     * @param category
     * @return
     */
    private static Comparator<Movie> getComparator(String category) {
        Comparator<Movie> cmpMovie = null;
        String sortKey = SORT_KEYS.get(category);
        boolean ascending = SORT_ASC.get(category);

        if (StringTools.isNotValidString(sortKey)) {
            return cmpMovie;
        }

        if (sortKey.equalsIgnoreCase(INDEX_NEW)) {
            cmpMovie = new LastModifiedComparator(ascending);
        } else if (sortKey.equalsIgnoreCase(INDEX_TITLE)) {
            cmpMovie = new MovieTitleComparator(ascending);
        } else if (sortKey.equalsIgnoreCase(INDEX_RATING)) {
            cmpMovie = new MovieRatingComparator(ascending);
        } else if (sortKey.equalsIgnoreCase(INDEX_TOP250)) {
            cmpMovie = new MovieTop250Comparator(ascending);
        } else if (sortKey.equalsIgnoreCase(INDEX_YEAR)) {
            cmpMovie = new MovieReleaseComparator(ascending);
        }

        return cmpMovie;
    }

    /**
     * Find the un-modified category name.
     *
     * The Category name could be changed by the use of the Category XML file.
     *
     * This function will return the original, unchanged name
     *
     * @param newCategory
     * @param returnCategory
     * @return
     */
    public static String getOriginalCategory(String newCategory, boolean returnCategory) {
        for (Map.Entry<String, String> singleCategory : CATEGORIES_MAP.entrySet()) {
            if (singleCategory.getValue().equals(newCategory)) {
                return singleCategory.getKey();
            }
        }

        // Check to see if we should return the original category that was passed
        if (returnCategory) {
            return newCategory;
        }
        return Movie.UNKNOWN;
    }

    /**
     * Find the renamed category name from the original name
     *
     * The Category name could be changed by the use of the Category XML file.
     *
     * This function will return the new name.
     *
     * @param newCategory
     * @return
     */
    public static String getRenamedCategory(String newCategory) {
        return CATEGORIES_MAP.get(newCategory);
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
        return Arrays.asList(new String[]{INDEX_OTHER.toUpperCase(),
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
            INDEX_RATINGS.toUpperCase()});
    }

    /**
     * Determine the year banding for the category.
     *
     * If the year is this year or last year, return those, otherwise return the decade the year resides in
     *
     * @param filmYear The year to check
     * @return "This Year", "Last Year" or the decade range (1990-1999)
     */
    public static String getYearCategory(final String filmYear) {
        StringBuilder yearCat;
        if (StringTools.isValidString(filmYear) && StringUtils.isNumeric(filmYear)) {
            if (filmYear.equals(String.valueOf(CURRENT_YEAR))) {
                yearCat = new StringBuilder("This Year");
            } else if (filmYear.equals(String.valueOf(CURRENT_YEAR - 1))) {
                yearCat = new StringBuilder("Last Year");
            } else {
                String beginYear = filmYear.substring(0, filmYear.length() - 1) + "0";
                String endYear;

                int tmpYear = NumberUtils.toInt(filmYear, -1);
                if (tmpYear < 0) {
                    LOG.debug("Year is not number: {}", filmYear);
                    return Movie.UNKNOWN;
                } else if (tmpYear >= CURRENT_DECADE) {
                    // The film year is in the current decade, so we need to adjust the end year
                    endYear = String.valueOf(FINAL_YEAR);
                } else {
                    // Otherwise it's 9
                    endYear = filmYear.substring(0, filmYear.length() - 1) + "9";
                }
                LOG.trace("Library years for categories: Begin='{}' End='{}'", beginYear, endYear);
                yearCat = new StringBuilder(beginYear);
                yearCat.append("-").append(endYear.substring(endYear.length() >= 4 ? endYear.length() - 2 : 0));
            }
        } else {
            LOG.trace("Library: Invalid year '{}'", filmYear);
            yearCat = new StringBuilder(Movie.UNKNOWN);
        }

        return yearCat.toString();
    }

    public List<Movie> getMatchingMoviesList(String indexName, List<Movie> boxedSetMovies, String categorie) {
        List<Movie> response = new ArrayList<>();
        List<Movie> list = this.unCompressedIndexes.get(indexName).get(categorie);

        if (list == null) {
            return response;
        }

        for (Movie movie : boxedSetMovies) {
            if (list.contains(movie)) {
                LOG.debug("Movie {} match for {}[{}]", movie.getTitle(), indexName, categorie);
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
        if (StringTools.isValidString(name) && !DIRTY_LIBRARIES.contains(name)) {
            DIRTY_LIBRARIES.add(name);
            setDirty();
        }
    }

    public boolean isDirtyLibrary(String name) {
        return StringTools.isValidString(name) && DIRTY_LIBRARIES.contains(name);
    }
}
