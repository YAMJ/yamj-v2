/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;

public class Library implements Map<String, Movie> {

    public static final String TV_SERIES = "TVSeries";
    public static final String SET = "Set";

    @SuppressWarnings("rawtypes")
    private static class MovieSetComparator implements Comparator {
        private String set;

        private MovieSetComparator(String set) {
            this.set = set;
        }

        public int compare(Object left, Object right) {
            Movie m1 = (Movie)left;
            Movie m2 = (Movie)right;
            Integer o1 = m1.getSetOrder(set);
            Integer o2 = m2.getSetOrder(set);

            // If one is explicitly ordered and the other isn't, the ordered one comes first
            if (o1 == null && o2 != null || o1 != null && o2 == null) {
                return o2 == null ? -1 : 1;

                // If they're both ordered and the value is different, order by that
            } else if (o1 != null && !o1.equals(o2)) {
                return o1.compareTo(o2);

                // Either the order is the same, or neither have an order, so fall back to releaseDate, then titleSort
            } else {
                int c = m1.getYear().compareTo(m2.getYear());
                if (c == 0) {
                    c = m1.getTitleSort().compareTo(m2.getTitleSort());
                }
                return c;
            }
        }
    }

    private static Logger logger = Logger.getLogger("moviejukebox");
    private static boolean filterGenres;
    private static boolean singleSeriesPage;
    private static List<String> certificationOrdering = new ArrayList<String>();
    private static Map<String, String> genresMap = new HashMap<String, String>();
    private static Map<String, String> categoriesMap = new LinkedHashMap<String, String>();
    private static Map<Character, Character> charReplacementMap = new HashMap<Character, Character>();
    private static boolean charGroupEnglish = false;
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
    private static String indexList;
    private static boolean splitHD = false;
    private static boolean processExtras = true;

    // Static values for the year indexes
    private static final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private static final int finalYear = currentYear - 2;
    private static final int currentDecade = (finalYear / 10) * 10;

    static {
        minSetCount = Integer.parseInt(PropertiesUtil.getProperty("mjb.sets.minSetCount", "2"));
        setsRequireAll = PropertiesUtil.getProperty("mjb.sets.requireAll", "false").equalsIgnoreCase("true");
        filterGenres = PropertiesUtil.getProperty("mjb.filter.genres", "false").equalsIgnoreCase("true");
        singleSeriesPage = PropertiesUtil.getProperty("mjb.singleSeriesPage", "false").equalsIgnoreCase("true");
        indexList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Rating,Year,Library,Set");
        splitHD = Boolean.parseBoolean(PropertiesUtil.getProperty("highdef.differentiate", "false"));
        processExtras = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.extras.process","true"));
        String xmlGenreFile = PropertiesUtil.getProperty("mjb.xmlGenreFile", "genres-default.xml");
        fillGenreMap(xmlGenreFile);

        try {
            maxGenresPerMovie = Integer.parseInt(PropertiesUtil.getProperty("genres.max", "" + maxGenresPerMovie));
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

        String xmlCategoryFile = PropertiesUtil.getProperty("mjb.xmlCategoryFile", "categories-default.xml");
        fillCategoryMap(xmlCategoryFile);

        String temp = PropertiesUtil.getProperty("indexing.character.replacement", "");
        StringTokenizer tokenizer = new StringTokenizer(temp, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            int idx = token.indexOf("-");
            if (idx > 0) {
                try {
                    String key = token.substring(0, idx).trim();
                    String value = token.substring(idx + 1).trim();
                    if (key.length() == 1 && value.length() == 1) {
                        charReplacementMap.put(new Character(key.charAt(0)), new Character(value.charAt(0)));
                    }
                } catch (Exception ignore) {
                }
            }
        }

        charGroupEnglish = PropertiesUtil.getProperty("indexing.character.groupEnglish", "false").equalsIgnoreCase("true");
        getNewCategoryProperties();
    }
    
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
            logger.finest("New Movie category will have " + (newMovieCount > 0 ? ("the " + newMovieCount) : "all of the") + " most recent movies in the last " + newMovieDays + " days");
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newMovieDays *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        } else {
            logger.finest("New Movie category is disabled");
        }

        if (newTvDays > 0) {
            logger.finest("New TV category will have " + (newTvCount > 0 ? ("the " + newTvCount) : "all of the") + " most recent TV Shows in the last " + newTvDays + " days");
            // Convert newDays from DAYS to MILLISECONDS for comparison purposes
            newTvDays *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        } else {
            logger.finest("New category is disabled");
        }
}

    public static class IndexInfo {
        public String categoryName;
        public String key;
        public String baseName;
        public int videosPerPage, videosPerLine, pages;
        public boolean canSkip = true; // skip flags, global (all pages)

        public IndexInfo(String category, String key, int pages, int videosPerPage, int videosPerLine, boolean canSkip) {
            this.categoryName = category;
            this.key = key;
            this.pages = pages;
            this.videosPerPage = videosPerPage;
            this.videosPerLine = videosPerLine;
            this.canSkip = canSkip; // default values
            // "categ_key_"; to be combined with pageid and extension
            baseName = FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, key));
            pages = 0;
        }

        public void checkSkip(int page, String rootPath) {
            String filetest = rootPath + File.separator + baseName + page + ".xml";
            canSkip = canSkip && FileTools.fileCache.fileExists(filetest);
            FileTools.addJukeboxFile(filetest);
            // not nice, but no need to do this again in HTMLWriter
            filetest = rootPath + File.separator + baseName + page + ".html";
            canSkip = canSkip && FileTools.fileCache.fileExists(filetest);
            FileTools.addJukeboxFile(filetest);
        }

    }

    private Collection<IndexInfo> generated_indexes = Collections.synchronizedCollection(new ArrayList<IndexInfo>());

    public Library() {
    }

    public static String getMovieKey(IMovieBasicInformation movie) {
        // Issue 190
        String key = movie.getTitle() + " (" + movie.getYear() + ")";

        if (movie.isTVShow()) {
            // Issue 190
            key += " Season " + paddedFormat.format(movie.getSeason());
        }

        key = key.toLowerCase();
        return key;
    }

    // synchronized because scanning can be multi-threaded
    public synchronized void addMovie(String key, Movie movie) {
        Movie existingMovie = library.get(key);
        // logger.finest("Adding video " + key + ", new part: " + (existingMovie != null));

        if (movie.isExtra()) {
            // logger.finest("  It's an extra: " + movie.getBaseName());
            extras.put(movie.getBaseName(), movie);
        } else if (existingMovie == null) {
            library.put(key, movie);
        } else {
            MovieFile firstMovieFile = movie.getFirstFile();
            // Take care of TV-Show (order by episode). Issue 535 - Not sure it's the best place do to this.
            if (existingMovie.isTVShow()) {
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
            Movie movie = library.get(getMovieKey(extra));

            library.put(extraEntry.getKey(), extra);
            if (null != movie) {
                movie.addExtraFile(new ExtraFile(extraEntry.getValue().getFirstFile()));
            }
        }
    }

    protected static Map<String, Movie> buildIndexMasters(String prefix, Index index, List<Movie> indexMovies) {
        Map<String, Movie> masters = new HashMap<String, Movie>();

        for (Map.Entry<String, List<Movie>> index_entry : index.entrySet()) {
            String index_name = index_entry.getKey();
            List<Movie> index_list = index_entry.getValue();

            Movie indexMaster;
            try {
                indexMaster = (Movie)index_list.get(0).clone();
                indexMaster.setSetMaster(true);
                indexMaster.setSetSize(index_list.size());
                indexMaster.setTitle(index_name);
                indexMaster.setSeason(-1);
                indexMaster.setTitleSort(index_name);
                indexMaster.setOriginalTitle(index_name);
                indexMaster.setBaseFilename(createPrefix(prefix, createCategoryKey(index_name)) + "1");
                indexMaster.setBaseName(makeSafeFilename(indexMaster.getBaseFilename()));

                // set TV and HD properties of the master
                int cntTV = 0;
                int cntHD = 0;
                int top250 = -1;

                // We Can't use a TreeSet because MF.compareTo just compares part #
                // so it fails when we combine multiple seasons into one collection
                Collection<MovieFile> master_mf_col = new LinkedList<MovieFile>();
                for (Movie m : index_list) {
                    if (m.isTVShow()) {
                        ++cntTV;
                    }
                    if (m.isHD()) {
                        ++cntHD;
                    }

                    int mTop250 = m.getTop250();
                    if (mTop250 > 0 && (top250 < 0 || mTop250 < top250)) {
                        top250 = mTop250;
                    }

                    Collection<MovieFile> mf_col = m.getMovieFiles();
                    if (mf_col != null) {
                        master_mf_col.addAll(mf_col);
                    }
                    
                    // Update the master fileDate to be the latest of all the members so this indexes correctly in the New category
                    indexMaster.addFileDate(m.getFileDate());
                }

                indexMaster.setMovieType(cntTV > 1 ? Movie.TYPE_TVSHOW : null);
                indexMaster.setVideoType(cntHD > 1 ? Movie.TYPE_VIDEO_HD : null);
                logger.finest("Setting index master >" + indexMaster.getTitle() + "< - isTV: " + indexMaster.isTVShow() + " - isHD: " + indexMaster.isHD()
                                + " - top250: " + indexMaster.getTop250());
                indexMaster.setTop250(top250);
                indexMaster.setMovieFiles(master_mf_col);
                
                masters.put(index_name, indexMaster);
            } catch (CloneNotSupportedException error) {
                logger.severe("Failed building index masters");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }

        return masters;
    }

    protected static void compressSetMovies(List<Movie> movies, Index index, Map<String, Movie> masters) {
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
                movies.removeAll(lm);
                movies.add(masters.get(in_movies_entry.getKey()));
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
            Map<String, Index> dynamic_indexes = new LinkedHashMap<String, Index>();
            // Add the sets FIRST! That allows users to put series inside sets
            dynamic_indexes.put(SET, indexBySets(indexMovies));

            final Map<String, Index> syncindexes = Collections.synchronizedMap(indexes);

            for (final String indexStr : indexList.split(",")) {
                tasks.submit(new Callable<Void>() {
                    public Void call() {
                        logger.fine("  Indexing " + indexStr + "...");
                        if (indexStr.equals("Other")) {
                            syncindexes.put("Other", indexByProperties(indexMovies));
                        } else if (indexStr.equals("Genres")) {
                            syncindexes.put("Genres", indexByGenres(indexMovies));
                        } else if (indexStr.equals("Title")) {
                            syncindexes.put("Title", indexByTitle(indexMovies));
                        } else if (indexStr.equals("Rating")) {
                            syncindexes.put("Rating", indexByCertification(indexMovies));
                        } else if (indexStr.equals("Year")) {
                            syncindexes.put("Year", indexByYear(indexMovies));
                        } else if (indexStr.equals("Library")) {
                            syncindexes.put("Library", indexByLibrary(indexMovies));
                        } else if (indexStr.equals("Cast")) {
                            syncindexes.put("Cast", indexByCast(indexMovies));
                        } else if (indexStr.equals("Director")) {
                            syncindexes.put("Director", indexByDirector(indexMovies));
                        } else if (indexStr.equals("Country")) {
                            syncindexes.put("Country", indexByCountry(indexMovies));
                        } else if (indexStr.equals("Writer")) {
                            syncindexes.put("Writer", indexByWriter(indexMovies));
                        }
                        return null;
                    }
                });
            }
            tasks.waitFor();
            // Make a "copy" of uncompressed index
            this.keepUncompressedIndexes();

            Map<String, Map<String, Movie>> dyn_index_masters = new HashMap<String, Map<String, Movie>>();
            for (Map.Entry<String, Index> dyn_entry : dynamic_indexes.entrySet()) {
                Map<String, Movie> indexMasters = buildIndexMasters(dyn_entry.getKey(), dyn_entry.getValue(), indexMovies);
                dyn_index_masters.put(dyn_entry.getKey(), indexMasters);

                for (Map.Entry<String, Index> indexes_entry : indexes.entrySet()) {
                    // For each category in index, compress this one.
                    for (Map.Entry<String, List<Movie>> index_entry : indexes_entry.getValue().entrySet()) {
                        compressSetMovies(index_entry.getValue(), dyn_entry.getValue(), indexMasters);
                    }
                }
                indexes.put(dyn_entry.getKey(), dyn_entry.getValue());
                moviesList.addAll(indexMasters.values()); // so the driver knows what's an index master
            }

            // Now add the masters to the titles index
            // Issue 1018 - Check that this index was selected
            if (indexList.contains("Title")) {
                for (Map.Entry<String, Map<String, Movie>> dyn_index_masters_entry : dyn_index_masters.entrySet()) {
                    Index mastersTitlesIndex = indexByTitle(dyn_index_masters_entry.getValue().values());
                    for (Map.Entry<String, List<Movie>> index_entry : mastersTitlesIndex.entrySet()) {
                        for (Movie m : index_entry.getValue()) {
                            int setCount = dynamic_indexes.get(dyn_index_masters_entry.getKey()).get(m.getTitle()).size();
                            if (setCount >= minSetCount) {
                                indexes.get("Title").addMovie(index_entry.getKey(), m);
                            }
                        }
                    }
                }
            }
            tasks.restart();
            // OK, now that all the index masters are in-place, sort everything.
            logger.fine("  Sorting Indexes ...");
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

            // Cut off the Other/New lists if they're too long AND add them to the NEW category if required
            trimNewCategory("New-TV", newTvCount);
            trimNewCategory("New-Movie", newMovieCount);
            
            // Merge the two categories into the Master "New" category
            if (categoriesMap.get("New") != null) {
                int masterCategoryNeeded = 0; // if this is more than 1 then a sort is required
                Index otherIndexes = indexes.get("Other");
                List<Movie> newList = new ArrayList<Movie>();
                
                if ((categoriesMap.get("New-Movie") != null)  && (otherIndexes.get(categoriesMap.get("New-Movie")) != null)){
                    newList.addAll(otherIndexes.get(categoriesMap.get("New-Movie")));
                    masterCategoryNeeded++;
                }
                
                if ((categoriesMap.get("New-TV") != null)  && (otherIndexes.get(categoriesMap.get("New-TV")) != null)){
                    newList.addAll(otherIndexes.get(categoriesMap.get("New-TV")));
                    masterCategoryNeeded++;
                }
                
                if (masterCategoryNeeded > 1) {
                    logger.fine("Creating new catagory with latest Movies and TV Shows");
                    otherIndexes.put(categoriesMap.get("New"), newList);
                    Collections.sort(otherIndexes.get(categoriesMap.get("New")), cmpLast);
                }
                
            }
            
            // Now set up the index masters' posters
            for (Map.Entry<String, Map<String, Movie>> dyn_index_masters_entry : dyn_index_masters.entrySet()) {
                for (Map.Entry<String, Movie> masters_entry : dyn_index_masters_entry.getValue().entrySet()) {
                    List<Movie> set = dynamic_indexes.get(dyn_index_masters_entry.getKey()).get(masters_entry.getKey());
                    masters_entry.getValue().setPosterFilename(set.get(0).getBaseName() + ".jpg");
                    masters_entry.getValue().setFile(set.get(0).getFile()); // ensure PosterScanner looks in the right directory
                }
            }
            Collections.sort(indexMovies);
            setMovieListNavigation(indexMovies);
        }
    }
    
    /**
     * Trim the new category to the required length, add the trimmed video list to the NEW category
     * @param catName   The name of the category: "New-TV" or "New-Movie"
     * @param catCount  The maximum size of the category
     */
    private void trimNewCategory(String catName, int catCount) {
        String category = categoriesMap.get(catName);
        //logger.fine("Trimming '" + catName + "' ('" + category + "') to " + catCount + " videos");
        if (catCount > 0 && category != null) {
            Index otherIndexes = indexes.get("Other");
            if (otherIndexes != null) {
                List<Movie> newList = otherIndexes.get(category);
                //logger.fine("Current size of '" + catName + "' ('" + category + "') is " + (newList != null ? newList.size() : "NULL"));
                if ((newList != null)  && (newList.size() > catCount)) {
                        newList = newList.subList(0, catCount);
                        otherIndexes.put(category, newList);
                }
            } else {
                logger.warning("Warning : You need to enable index 'Other' to get '" + catName + "' ('" + category + "') category");
            }
        }
    }

    private void keepUncompressedIndexes() {
        this.unCompressedIndexes = new HashMap<String, Index>(indexes.size());
        Set<String> indexeskeySet = this.indexes.keySet();
        for (String key : indexeskeySet) {
            logger.finest("Copying " + key + " indexes");
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
            if (!movie.isExtra()) {
                String title = movie.getStrippedTitleSort();
                if (title.length() > 0) {
                    Character c = Character.toUpperCase(title.charAt(0));

                    if (!Character.isLetter(c)) {
                        index.addMovie("09", movie);
                        movie.addIndex("Title", "09");
                    } else if (charGroupEnglish && ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                        index.addMovie("AZ", movie);
                        movie.addIndex("Title", "AZ");
                    } else {
                        Character tempC = charReplacementMap.get(c);
                        if (tempC != null) {
                            c = tempC;
                        }
                        index.addMovie(c.toString(), movie);
                        movie.addIndex("Title", c.toString());
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
                    movie.addIndex("Year", year);
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
                movie.addIndex("Library", movie.getLibraryDescription());
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
                        movie.addIndex("Genre", getIndexingGenre(genre));
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
                index.addMovie(movie.getCertification(), movie);
                movie.addIndex("Certification", movie.getCertification());
            }
        }
        return index;
    }

    private static Index indexByProperties(Iterable<Movie> moviesList) {
        Index index = new Index();
        long now = System.currentTimeMillis();
        for (Movie movie : moviesList) {
            if (movie.isExtra()) {
                // Issue 997: Skip the processing of extras
                if (processExtras) {
                    if (categoriesMap.get("Extras") != null) {
                        index.addMovie(categoriesMap.get("Extras"), movie);
                        movie.addIndex("Property", categoriesMap.get("Extras"));
                    }
                }
            } else {
                if (movie.isHD()) {
                    if (splitHD) {
                        // Split the HD category into two categories: HD-720 and HD-1080
                        if (movie.isHD1080()) {
                            if (categoriesMap.get("HD-1080") != null) {
                                index.addMovie(categoriesMap.get("HD-1080"), movie);
                                movie.addIndex("Property", categoriesMap.get("HD-1080"));
                            }
                        } else {
                            if (categoriesMap.get("HD-720") != null) {
                                index.addMovie(categoriesMap.get("HD-720"), movie);
                                movie.addIndex("Property", categoriesMap.get("HD-720"));
                            }
                        }
                    } else {
                        if (categoriesMap.get("HD") != null) {
                            index.addMovie(categoriesMap.get("HD"), movie);
                            movie.addIndex("Property", categoriesMap.get("HD"));
                        }
                    }
                }

                if (movie.getTop250() > 0) {
                    if (categoriesMap.get("Top250") != null) {
                        index.addMovie(categoriesMap.get("Top250"), movie);
                        movie.addIndex("Property", categoriesMap.get("Top250"));
                    }
                }
                
                // Add to the New Movie category
                if (!movie.isTVShow() && (newMovieDays > 0) && (now - movie.getLastModifiedTimestamp() <= newMovieDays)) {
//                    if (categoriesMap.get("New") != null) {
//                        index.addMovie(categoriesMap.get("New"), movie);
//                        movie.addIndex("Property", categoriesMap.get("New"));
//                    }
                    
                    if (categoriesMap.get("New-Movie") != null) {
                        index.addMovie(categoriesMap.get("New-Movie"), movie);
                        movie.addIndex("Property", categoriesMap.get("New-Movie"));
                    }
                }
                
                // Add to the New TV category
                if (movie.isTVShow() && (newTvDays > 0) && (now - movie.getLastModifiedTimestamp() <= newTvDays)) {
//                    if (categoriesMap.get("New") != null) {
//                        index.addMovie(categoriesMap.get("New"), movie);
//                        movie.addIndex("Property", categoriesMap.get("New"));
//                    }
                    
                    if (categoriesMap.get("New-TV") != null) {
                        index.addMovie(categoriesMap.get("New-TV"), movie);
                        movie.addIndex("Property", categoriesMap.get("New-TV"));
                    }
                }

                if (categoriesMap.get("All") != null) {
                    index.addMovie(categoriesMap.get("All"), movie);
                    movie.addIndex("Property", categoriesMap.get("All"));
                }

                if (movie.isTVShow()) {
                    if (categoriesMap.get("TV Shows") != null) {
                        index.addMovie(categoriesMap.get("TV Shows"), movie);
                        movie.addIndex("Property", categoriesMap.get("TV Shows"));
                    }
                } else {
                    if (categoriesMap.get("Movies") != null) {
                        index.addMovie(categoriesMap.get("Movies"), movie);
                        movie.addIndex("Property", categoriesMap.get("Movies"));
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
                    movie.addIndex("Set", movie.getOriginalTitle());
                }

                for (String set_key : movie.getSetsKeys()) {
                    index.addMovie(set_key, movie);
                    movie.addIndex("Set", set_key);
                }
            }
        }

        return index;
    }

    protected static Index indexByCast(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                for (String actor : movie.getCast()) {
                    logger.finest("Adding " + movie.getTitle() + " to cast list for " + actor);
                    index.addMovie(actor, movie);
                    movie.addIndex("Actor", actor);
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
                movie.addIndex("Country", movie.getCountry());
            }
        }

        return index;
    }

    protected static Index indexByDirector(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                index.addMovie(movie.getDirector(), movie);
                movie.addIndex("Director", movie.getDirector());
            }
        }

        return index;
    }

    protected static Index indexByWriter(List<Movie> list) {
        Index index = new Index(true);
        for (Movie movie : list) {
            if (!movie.isExtra()) {
                for (String writer : movie.getWriters()) {
                    logger.finest("Adding " + movie.getTitle() + " to writer list for " + writer);
                    index.addMovie(writer, movie);
                    movie.addIndex("Writer", writer);
                }
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
     *            Genre to find the master.
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

    public void clear() {
        library.clear();
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
    private static void fillGenreMap(String xmlGenreFile) {
        File f = new File(xmlGenreFile);
        if (f.exists() && f.isFile() && xmlGenreFile.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(f);

                List<HierarchicalConfiguration> genres = c.configurationsAt("genre");
                for (HierarchicalConfiguration genre : genres) {
                    String masterGenre = genre.getString("[@name]");
                    // logger.finest("New masterGenre parsed : (" + masterGenre+ ")");
                    List<String> subgenres = genre.getList("subgenre");
                    for (String subgenre : subgenres) {
                        // logger.finest("New genre added to map : (" + subgenre+ "," + masterGenre+ ")");
                        genresMap.put(subgenre, masterGenre);
                    }

                }
            } catch (Exception error) {
                logger.severe("Failed parsing moviejukebox genre input file: " + f.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        } else {
            logger.severe("The moviejukebox genre input file you specified is invalid: " + f.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static void fillCategoryMap(String xmlCategoryFile) {
        File f = new File(xmlCategoryFile);
        if (f.exists() && f.isFile() && xmlCategoryFile.toUpperCase().endsWith("XML")) {

            try {
                XMLConfiguration c = new XMLConfiguration(f);

                List<HierarchicalConfiguration> categories = c.configurationsAt("category");
                for (HierarchicalConfiguration category : categories) {
                    boolean enabled = Boolean.parseBoolean(category.getString("enable", "true"));

                    if (enabled) {
                        String origName = category.getString("[@name]");
                        String newName = category.getString("rename", origName);
                        categoriesMap.put(origName, newName);
                        //logger.finest("Added category '" + origName + "' with name '" + newName + "'");
                    }
                }
            } catch (Exception error) {
                logger.severe("Failed parsing moviejukebox category input file: " + f.getName());
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        } else {
            logger.severe("The moviejukebox category input file you specified is invalid: " + f.getName());
        }
    }

    // Issue 436
    public String getDefaultCategory() {
        // Find the first category in the first index that has any movies in it
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

    @SuppressWarnings("unchecked")
    protected static Comparator<Movie> getComparator(String category, String key) {
        Comparator<Movie> c = null;
        if (category.equals(SET)) {
            c = new MovieSetComparator(key);
        } else if (category.equals("Other")) {
            if (key.equals(categoriesMap.get("New")) || 
                    key.equals(categoriesMap.get("New-TV")) || 
                    key.equals(categoriesMap.get("New-Movie"))) {
                c = cmpLast;
            } else if (key.equals(categoriesMap.get("Top250"))) {
                c = cmp250;
            }
        }

        return c;
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
        return Arrays.asList(new String[] { "OTHER", "RATING", "TITLE", "YEAR", "GENRES", "SET", "LIBRARY", "CAST", "DIRECTOR", "COUNTRY", "CATEGORIES" });
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
                if (filmYear.equals("" + currentYear)) {
                    yearCat = "This Year";
                } else if (filmYear.equals("" + (currentYear - 1))) {
                    yearCat = "Last Year";
                } else {
                    String beginYear = filmYear.substring(0, filmYear.length() - 1) + "0";
                    String endYear = Movie.UNKNOWN;
                    if (Integer.parseInt(filmYear) >= currentDecade) {
                        // The film year is in the current decade, so we need to adjust the end year
                        endYear = "" + finalYear;
                    } else {
                        // Otherwise it's 9
                        endYear = filmYear.substring(0, filmYear.length() - 1) + "9";
                    }
                    yearCat = beginYear + "-" + endYear.substring(endYear.length() - 2);
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
                logger.finest("Movie " + movie.getTitle() + " match for " + indexName + "[" + categorie + "]");
                response.add(movie);
            }
        }
        return response;
    }

    public void addGeneratedIndex(IndexInfo index) {
        generated_indexes.add(index);
    }

    public Collection<IndexInfo> getGeneratedIndexes() {
        return generated_indexes;
    }
}
