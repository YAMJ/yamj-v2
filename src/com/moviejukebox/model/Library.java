package com.moviejukebox.model;

import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.text.DecimalFormat;

import java.util.Calendar;
import java.util.StringTokenizer;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class Library implements Map<String, Movie> {

    private static Logger logger = Logger.getLogger("moviejukebox");
    private static boolean filterGenres;
    private static List<String> certificationOrdering = new ArrayList<String>();
    private static Map<String, String> genresMap = new HashMap<String, String>();
    private static Map<String, String> categoriesMap = new HashMap<String, String>();
    private static Map<Character, Character> charReplacementMap = new HashMap<Character, Character>();
    private TreeMap<String, Movie> library = new TreeMap<String, Movie>();
    private List<Movie> moviesList = new ArrayList<Movie>();
    private Map<String, Map<String, List<Movie>>> indexes = new LinkedHashMap<String, Map<String, List<Movie>>>();
    private static DecimalFormat paddedFormat = new DecimalFormat("000");	// Issue 190
    private static final Calendar currentCal = Calendar.getInstance();
    private static int maxGenres = 9;


    static {
        filterGenres = PropertiesUtil.getProperty("mjb.filter.genres", "false").equalsIgnoreCase("true");
        String xmlGenreFile = PropertiesUtil.getProperty("mjb.xmlGenreFile", "genres.xml");
        fillGenreMap(xmlGenreFile);

        try {
            maxGenres = Integer.parseInt(PropertiesUtil.getProperty("genres.max", "9"));
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
    }

    public Library() {
    }

    public void addMovie(Movie movie) {
//		Issue 190
//		String key = movie.getTitle();
//		added Year to movie key to handle movies like Ocean's Eleven (1960) and Ocean's Eleven (2001)
        String key = movie.getTitle() + " (" + movie.getYear() + ")";

        if (movie.isTVShow()) {
//			Issue 190
//			key += " Season " + movie.getSeason();
            key += " Season " + paddedFormat.format(movie.getSeason());
        }

        key = key.toLowerCase();

        Movie existingMovie = library.get(key);

        if (movie.isTrailer()) {
            key = movie.getBaseName();
        }

        if (existingMovie == null) {
            library.put(key, movie);
        } else {
            if (movie.isTrailer()) {
                library.put(key, movie);
                existingMovie.addTrailerFile(new TrailerFile(movie.getFirstFile()));
            } else {
                existingMovie.addMovieFile(movie.getFirstFile());
            }
        }
    }

    public void buildIndex() {
        moviesList.clear();
        indexes.clear();

        moviesList.addAll(library.values());
        if (moviesList.size() > 0) {
            sortMovieDetails();
            indexByProperties();
            indexByGenres();
            indexByTitle();
            indexByCertification();
            indexByYear();
        }
    }

    private void sortMovieDetails() {
        Collections.sort(moviesList);

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

    private void indexByTitle() {
        TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
        for (Movie movie : moviesList) {
            if (!movie.isTrailer()) {
                String title = movie.getStrippedTitleSort();
                if (title.length() > 0) {
                    Character c = Character.toUpperCase(title.charAt(0));

                    if (!Character.isLetter(c)) {
                        addMovie(index, "09", movie);
                    } else {
                        Character tempC = charReplacementMap.get(c);
                        if (tempC != null) {
                            c = tempC;
                        }
                        addMovie(index, c.toString(), movie);
                    }
                }
            }
        }
        indexes.put("Title", index);
    }

    private void indexByYear() {
        TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
        for (Movie movie : moviesList) {
            String year = movie.getYear();
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                try {
                    String beginYear = year.substring(0, year.length() - 1) + "0";
                    String endYear = year.substring(0, year.length() - 1) + "9";
                    String category = beginYear + "-" + endYear.substring(endYear.length() - 2);
                    addMovie(index, category, movie);

                    int currentYear = currentCal.get(Calendar.YEAR);
                    if (year.equals("" + currentYear)) {
                        addMovie(index, "This Year", movie);
                    } else if (year.equals("" + (currentYear - 1))) {
                        addMovie(index, "Last Year", movie);
                    }
                } catch (Exception ignore) {
                }
            }
        }
        indexes.put("Year", index);
    }

    private void indexByGenres() {
        TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
        for (Movie movie : moviesList) {
            int count = 0;
            for (String genre : movie.getGenres()) {
                addMovie(index, getIndexingGenre(genre), movie);
                count++;
                if (count >= maxGenres) {
                    break;
                }
            }
        }
        indexes.put("Genres", index);
    }

    private void indexByCertification() {
        TreeMap<String, List<Movie>> index = null;
        if (!certificationOrdering.isEmpty()) {
            index = new TreeMap<String, List<Movie>>(new CertificationComparator(certificationOrdering));
        } else {
            index = new TreeMap<String, List<Movie>>();
        }

        for (Movie movie : moviesList) {
            if (!movie.isTrailer()) {
                addMovie(index, movie.getCertification(), movie);
            }
        }
        indexes.put("Rating", index);
    }

    private void indexByProperties() {
        long oneDay = 1000 * 60 * 60 * 24; // Milliseconds * Seconds * Minutes * Hours
        // long oneWeek = oneDay * 7;
        // long oneMonth = oneDay * 30;

        String newDaysParam = PropertiesUtil.getProperty("mjb.newdays", "7");
        String newCountParam = PropertiesUtil.getProperty("mjb.newcount", "0");
        long newDays;
        int newCount;

        try {
            newDays = Long.parseLong(newDaysParam.trim());
        } catch (NumberFormatException nfe) {
            newDays = 7;
        }
        try {
            newCount = Integer.parseInt(newCountParam.trim());
        } catch (NumberFormatException nfe) {
            newCount = 0;
        }

        logger.finest("New category will have " + (newCount > 0 ? newCount : "all of the") + " most recent videos in the last " + newDays + " days");
        newDays = newDays * oneDay;

        TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
        for (Movie movie : moviesList) {
            if (movie.isTrailer()) {
                if (categoriesMap.get("Trailers") != null) {
                    addMovie(index, categoriesMap.get("Trailers"), movie);
                }
            } else {
                if (movie.getVideoOutput().indexOf("720") != -1 || movie.getVideoOutput().indexOf("1080") != -1) {
                    if (categoriesMap.get("HD") != null) {
                        addMovie(index, categoriesMap.get("HD"), movie);
                    }
                }

                if (movie.getTop250() > 0) {
                    if (categoriesMap.get("Top250") != null) {
                        addMovie(index, categoriesMap.get("Top250"), movie);
                    }
                }

                long delay = System.currentTimeMillis() - movie.getLastModifiedTimestamp();

                if (delay <= newDays) {
                    if (categoriesMap.get("New") != null) {
                        addMovie(index, categoriesMap.get("New"), movie);
                    }
                }

                if (categoriesMap.get("All") != null) {
                    addMovie(index, categoriesMap.get("All"), movie);
                }

                if (movie.isTVShow()) {
                    if (categoriesMap.get("TV Shows") != null) {
                        addMovie(index, categoriesMap.get("TV Shows"), movie);
                    }
                } else {
                    if (categoriesMap.get("Movies") != null) {
                        addMovie(index, categoriesMap.get("Movies"), movie);
                    }
                }
            }
        }

        // sort New category by lastModifiedTimestamp and then limit to the count
        if (categoriesMap.get("New") != null) {
            List<Movie> newList = index.get(categoriesMap.get("New"));
            if (newList != null) {
                Collections.sort(newList, new LastModifiedComparator());
                if (newCount > 0 && newCount < newList.size()) {
                    newList = newList.subList(0, newCount);
                    index.put(categoriesMap.get("New"), newList);
                }
            }
        }

        // sort top250 by rating instead of by title
        if (categoriesMap.get("Top250") != null) {
            List<Movie> top250List = index.get(categoriesMap.get("Top250"));
            if (top250List != null) {
                Collections.sort(top250List, new Top250Comparator());
            }
        }

        indexes.put("Other", index);
    }

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

    private void addMovie(TreeMap<String, List<Movie>> index, String category, Movie movie) {
        if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("UNKNOWN")) {
            return;
        }

        if (movie == null) {
            return;
        }

        List<Movie> list = index.get(category);

        if (list == null) {
            list = new ArrayList<Movie>();
            index.put(category, list);
        }
        if (!list.contains(movie)) {
            list.add(movie);
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

    public Map<String, Map<String, List<Movie>>> getIndexes() {
        return indexes;
    }

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
}
