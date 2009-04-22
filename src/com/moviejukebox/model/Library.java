package com.moviejukebox.model;

import java.io.File;
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
import java.util.logging.Logger;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;

public class Library implements Map<String, Movie> {
	
    public static final String TV_SERIES = "TVSeries";
    public static final String SET = "Set";
    
    public static class Index extends TreeMap<String, List<Movie>> {
        private int maxCategories = -1;
        private boolean display = true;
		
        private static final long serialVersionUID = -6240040588085931654L;

        public Index(Comparator<? super String> comp) {
            super(comp);
        }
        
        public Index() {
            super();
        }
		
        public Index(boolean display) {
            this();
            this.display = display;
        }
        
        public boolean display() {
            return display;
        }
        
        protected void addMovie(String category, Movie movie) {
            if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("UNKNOWN")) {
                return;
            }

            if (movie == null) {
                return;
            }

            List<Movie> list = get(category);

            if (list == null) {
            if (maxCategories > 0 && size() >= maxCategories) {
                return;
            }
                list = new ArrayList<Movie>();
                put(category, list);
            }
            if (!list.contains(movie)) {
                list.add(movie);
            }
        }

        public int getMaxCategories() {
            return maxCategories;
        }

        public void setMaxCategories(int maxCategories) {
            this.maxCategories = maxCategories;
        }

    }
    
    @SuppressWarnings("unchecked")
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
    private TreeMap<String, Movie> library = new TreeMap<String, Movie>();
    private Map<String, Movie> trailers = new TreeMap<String, Movie>();
    private List<Movie> moviesList = new ArrayList<Movie>();
    private Map<String, Index> indexes = new LinkedHashMap<String, Index>();
    private static DecimalFormat paddedFormat = new DecimalFormat("000");	// Issue 190
    private static final Calendar currentCal = Calendar.getInstance();
    private static int maxGenresPerMovie = 3;
    private static int newCount = 0;
    private static long newDays = 7;
    private static int minSetCount = 2;
    private static boolean setsRequireAll = false;
    
    static {
        minSetCount = Integer.parseInt(PropertiesUtil.getProperty("mjb.sets.minSetCount", "2"));
        setsRequireAll = PropertiesUtil.getProperty("mjb.sets.requireAll", "false").equalsIgnoreCase("true");
        filterGenres = PropertiesUtil.getProperty("mjb.filter.genres", "false").equalsIgnoreCase("true");
        singleSeriesPage = PropertiesUtil.getProperty("mjb.singleSeriesPage", "false").equalsIgnoreCase("true");
        String xmlGenreFile = PropertiesUtil.getProperty("mjb.xmlGenreFile", "genres.xml");
        fillGenreMap(xmlGenreFile);

        try {
            maxGenresPerMovie = Integer.parseInt(PropertiesUtil.getProperty("genres.max", "" + maxGenresPerMovie));
        } catch (Exception ignore) {
        }

        {
            String temp = PropertiesUtil.getProperty("certification.ordering");
            if (temp != null && !temp.isEmpty()) {
                String[] certs = temp.split(",");
                certificationOrdering.addAll(Arrays.asList(certs));
            }
        }

        String xmlCategoryFile = PropertiesUtil.getProperty("mjb.xmlCategoryFile", "categories.xml");
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
        
        String newDaysParam = PropertiesUtil.getProperty("mjb.newdays", "7");
        try {
            newDays = Long.parseLong(newDaysParam.trim());
        } catch (NumberFormatException nfe) {
            newDays = 7;
        }
        newDays *= 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        String newCountParam = PropertiesUtil.getProperty("mjb.newcount", "0");
        try {
            newCount = Integer.parseInt(newCountParam.trim());
        } catch (NumberFormatException nfe) {
            newCount = 0;
        }
        logger.finest("New category will have " + (newCount > 0 ? newCount : "all of the") +
                      " most recent videos in the last " + newDays + " days");
    }

    public Library() {
    }
    
    public static String getMovieKey(Movie movie) {
//    Issue 190
        String key = movie.getTitle() + " (" + movie.getYear() + ")";

        if (movie.isTVShow()) {
//      Issue 190
            key += " Season " + paddedFormat.format(movie.getSeason());
        }

        key = key.toLowerCase();
        return key;
    }    

    public void addMovie(Movie movie) {
        String key = getMovieKey(movie);
        Movie existingMovie = library.get(key);
        logger.finest("Adding movie " + key + ", new part: " + (existingMovie != null));

        if (movie.isTrailer()) {
            logger.finest("  It's a trailer: " + movie.getBaseName());
            trailers.put(movie.getBaseName(), movie);
        } else if (existingMovie == null) {
            library.put(key, movie);
        } else {
            existingMovie.addMovieFile(movie.getFirstFile());
        }
    }
    
    public void mergeTrailers() {
        for (Map.Entry<String, Movie> trailerEntry : trailers.entrySet()) {
            Movie trailer = trailerEntry.getValue();
            Movie movie = library.get(getMovieKey(trailer));
            
            library.put(trailerEntry.getKey(), trailer);
            if (null != movie) {
                movie.addTrailerFile(new TrailerFile(trailerEntry.getValue().getFirstFile()));
            }
        }        
    }
    
    protected static Map<String, Movie> buildIndexMasters(String prefix, Index index, List<Movie> indexMovies) {
        Map<String, Movie> masters = new HashMap<String, Movie>();
    
        for (Map.Entry<String, List<Movie>> index_entry : index.entrySet()) {
            String index_name = index_entry.getKey();
            List<Movie> index_list = index_entry.getValue();
            
            Movie indexMaster = (Movie)index_list.get(0).clone();
            indexMaster.setSetMaster(true);
            indexMaster.setTitle(index_name);
            indexMaster.setSeason(-1);
            indexMaster.setTitleSort(index_name);
            indexMaster.setOriginalTitle(index_name);
            indexMaster.setBaseName(
                FileTools.createPrefix(
                    prefix,
                    FileTools.createCategoryKey(index_name)
                ) + "1"
            );
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
            }
            
            indexMaster.setMovieType(cntTV > 1 ? Movie.TYPE_TVSHOW : null);
            indexMaster.setVideoType(cntHD > 1 ? Movie.TYPE_VIDEO_HD : null);
            logger.finest("Setting index master " + indexMaster.getTitle());
            logger.finest("  isTV: " + indexMaster.isTVShow());
            logger.finest("  isHD: " + indexMaster.isHD());
            logger.finest("  top250: " + indexMaster.getTop250());
            indexMaster.setTop250(top250);
            indexMaster.setMovieFiles(master_mf_col);
            masters.put(index_name, indexMaster);
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
            if (lm.size() >= minSetCount &&
                (!setsRequireAll || lm.size() == index.get(in_movies_entry.getKey()).size())) {
                movies.removeAll(lm);
                movies.add(masters.get(in_movies_entry.getKey()));
            }
        }
    }
    
    public void buildIndex() {
        moviesList.clear();
        indexes.clear();
        
        List<Movie> indexMovies = new ArrayList<Movie>(library.values());
        moviesList.addAll(library.values());
        
        if (indexMovies.size() > 0) {
            Map<String, Index> dynamic_indexes = new LinkedHashMap<String, Index>();
            // Add the sets FIRST! That allows users to put series inside sets
            dynamic_indexes.put(SET, indexBySets(indexMovies));
            
            indexes.put("Other", indexByProperties(indexMovies));
            indexes.put("Genres", indexByGenres(indexMovies));
            indexes.put("Title", indexByTitle(indexMovies));
            indexes.put("Rating", indexByCertification(indexMovies));
            indexes.put("Year", indexByYear(indexMovies));
            indexes.put("Library", indexByLibrary(indexMovies));
            
            Map<String, Map<String, Movie>> dyn_index_masters = new HashMap<String, Map<String, Movie>>();
            for (Map.Entry<String, Index> dyn_entry : dynamic_indexes.entrySet()) {
                Map<String, Movie> indexMasters = buildIndexMasters(
                    dyn_entry.getKey(),
                    dyn_entry.getValue(),
                    indexMovies
                );
                dyn_index_masters.put(dyn_entry.getKey(), indexMasters);
                
                for (Map.Entry<String, Index> indexes_entry : indexes.entrySet()) {
                    for (Map.Entry<String, List<Movie>> index_entry : indexes_entry.getValue().entrySet()) {
                        compressSetMovies(index_entry.getValue(), dyn_entry.getValue(), indexMasters);
                    }
                }
                indexes.put(dyn_entry.getKey(), dyn_entry.getValue());
                moviesList.addAll(indexMasters.values()); // so the driver knows what's an index master
            }

            // Now add the masters to the titles index
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
            
            // OK, now that all the index masters are in-place, sort everything.
            for (Map.Entry<String, Index> indexes_entry : indexes.entrySet()) {
                for (Map.Entry<String, List<Movie>> index_entry : indexes_entry.getValue().entrySet()) {
                    Comparator<Movie> comp = getComparator(indexes_entry.getKey(), index_entry.getKey());
                    if (null != comp) {
                        Collections.sort(index_entry.getValue(), comp);
                    } else {
                        Collections.sort(index_entry.getValue());
                    }
                }
            }
            
            // Cut off the Other/New list if it's too long
            String newcat = categoriesMap.get("New");
            if (newCount > 0 && newcat != null) {
                List<Movie> newList = indexes.get("Other").get("New");
                if (newList != null && newList.size() > newCount) {
                    newList = newList.subList(0, newCount);
                    indexes.get("Other").put(newcat, newList);
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

    private static void setMovieListNavigation(List<Movie> moviesList) {
        List<Movie> trailerList = new ArrayList<Movie>();

        Movie first = null;
        Movie last = null;

        // sort the trailers out of the movies
        for (Movie m : moviesList) {
            if (m.isTrailer()) {
                trailerList.add(m);
            } else {
                if (first == null) {
                    // set the first non-trailer movie
                    first = m;
                }
                // set the last non-trailer movie
                last = m;
            }
        }

        // ignore the trailers while sorting the other movies
        for (int j = 0; j < moviesList.size(); j++) {
            Movie movie = moviesList.get(j);
            if (!movie.isTrailer()) {
                movie.setFirst(first.getBaseName());

                for (int p = j - 1; p >= 0; p--) {
                    Movie prev = moviesList.get(p);
                    if (!prev.isTrailer()) {
                        movie.setPrevious(prev.getBaseName());
                        break;
                    }
                }

                for (int n = j + 1; n < moviesList.size(); n++) {
                    Movie next = moviesList.get(n);
                    if (!next.isTrailer()) {
                        movie.setNext(next.getBaseName());
                        break;
                    }
                }

                movie.setLast(last.getBaseName());
            }
        }

        // sort the trailers separately
        if (!trailerList.isEmpty()) {
            Movie firstTrailer = trailerList.get(0);
            Movie lastTrailer = trailerList.get(trailerList.size() - 1);
            for (int i = 0; i < trailerList.size(); i++) {
                Movie movie = trailerList.get(i);
                movie.setFirst(firstTrailer.getBaseName());
                movie.setPrevious(i > 0 ? trailerList.get(i - 1).getBaseName() : firstTrailer.getBaseName());
                movie.setNext(i < trailerList.size() - 1 ? trailerList.get(i + 1).getBaseName() : lastTrailer.getBaseName());
                movie.setLast(lastTrailer.getBaseName());
            }
        }
    }

    private static Index indexByTitle(Iterable<Movie> moviesList) {
    	Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isTrailer()) {
                String title = movie.getStrippedTitleSort();
                if (title.length() > 0) {
                    Character c = Character.toUpperCase(title.charAt(0));

                    if (!Character.isLetter(c)) {
                    	index.addMovie("09", movie);
                    } else {
                        Character tempC = charReplacementMap.get(c);
                        if (tempC != null) {
                            c = tempC;
                        }
                        index.addMovie(c.toString(), movie);
                    }
                }
            }
        }
        return index;
    }

    private static Index indexByYear(Iterable<Movie> moviesList) {
    	Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isTrailer()) {
                String year = movie.getYear();
                if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                    try {
                        String beginYear = year.substring(0, year.length() - 1) + "0";
                        String endYear = year.substring(0, year.length() - 1) + "9";
                        String category = beginYear + "-" + endYear.substring(endYear.length() - 2);
                        index.addMovie(category, movie);

                        int currentYear = currentCal.get(Calendar.YEAR);
                        if (year.equals("" + currentYear)) {
                            index.addMovie("This Year", movie);
                        } else if (year.equals("" + (currentYear - 1))) {
                            index.addMovie("Last Year", movie);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        return index;
    }
    
    private static Index indexByLibrary(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isTrailer() && movie.getLibraryDescription().length() > 0) {
                index.addMovie(movie.getLibraryDescription(), movie);
            }
        }
        return index;
    }

    private static Index indexByGenres(Iterable<Movie> moviesList) {
        Index index = new Index();
        for (Movie movie : moviesList) {
            if (!movie.isTrailer()) {
                int cntGenres = 0;
                for (String genre : movie.getGenres()) {
                    if (cntGenres < maxGenresPerMovie) {
                        index.addMovie(getIndexingGenre(genre), movie);
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
            if (!movie.isTrailer()) {
                index.addMovie(movie.getCertification(), movie);
            }
        }
        return index;
    }

    private static Index indexByProperties(Iterable<Movie> moviesList) {
        Index index = new Index();
        long now = System.currentTimeMillis();
        for (Movie movie : moviesList) {
            if (movie.isTrailer()) {
                if (categoriesMap.get("Trailers") != null) {
                    index.addMovie(categoriesMap.get("Trailers"), movie);
                }
            } else {
                if (movie.getVideoOutput().indexOf("720") != -1 || movie.getVideoOutput().indexOf("1080") != -1) {
                    if (categoriesMap.get("HD") != null) {
                        index.addMovie(categoriesMap.get("HD"), movie);
                    }
                }

                if (movie.getTop250() > 0) {
                    if (categoriesMap.get("Top250") != null) {
                        index.addMovie(categoriesMap.get("Top250"), movie);
                    }
                }

                if ((now - movie.getLastModifiedTimestamp() < newDays) && categoriesMap.get("New") != null) {
                    index.addMovie(categoriesMap.get("New"), movie);
                }

                if (categoriesMap.get("All") != null) {
                    index.addMovie(categoriesMap.get("All"), movie);
                }

                if (movie.isTVShow()) {
                    if (categoriesMap.get("TV Shows") != null) {
                        index.addMovie(categoriesMap.get("TV Shows"), movie);
                    }
                } else {
                    if (categoriesMap.get("Movies") != null) {
                        index.addMovie(categoriesMap.get("Movies"), movie);
                    }
                }
            }
        }

        return index;
    }
    
    protected static Index indexBySets(List<Movie> list) {
        Index index = new Index(false);
        for (Movie movie : list) {
            if (!movie.isTrailer()) {
                if(singleSeriesPage && movie.isTVShow()) {
                    index.addMovie(movie.getOriginalTitle(), movie);
                }
                
                for (String set_key : movie.getSets()) {
                    index.addMovie(set_key, movie);
                }
            }
        }
        
        return index;
    }
    
    /**
     * Checks if there is a master (will be shown in the index) genre for the specified one.
     * @param genre Genre to find the master.
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

    public Object clone() {
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

    public Collection<Movie> values() {
        return library.values();
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
//					logger.finest("New masterGenre parsed : (" +  masterGenre+ ")");
                    List<String> subgenres = genre.getList("subgenre");
                    for (String subgenre : subgenres) {
//						logger.finest("New genre added to map : (" + subgenre+ "," + masterGenre+ ")");
                        genresMap.put(subgenre, masterGenre);
                    }

                }
            } catch (Exception e) {
                logger.severe("Failed parsing moviejukebox genre input file: " + f.getName());
                e.printStackTrace();
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
                    String origName = category.getString("[@name]");
                    boolean enabled = Boolean.parseBoolean(category.getString("enable", "true"));
                    String newName = category.getString("rename", origName);

                    if (enabled) {
                        categoriesMap.put(origName, newName);
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed parsing moviejukebox category input file: " + f.getName());
                e.printStackTrace();
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
    
    @SuppressWarnings("unchecked")
    protected static Comparator<Movie> getComparator(String category, String key) {
        Comparator<Movie> c = null;
        if (category.equals(SET)) {
            c = new MovieSetComparator(key);

        } else if (category.equals("Other")) {
        
            if (key.equals(categoriesMap.get("New"))) {
                c = new LastModifiedComparator();
                
            } else if (key.equals(categoriesMap.get("Top250"))) {
                c = new Top250Comparator();
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
}
