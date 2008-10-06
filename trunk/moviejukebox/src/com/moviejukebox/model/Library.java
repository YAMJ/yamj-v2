package com.moviejukebox.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class Library implements Map<String, Movie> {

	private static Logger logger = Logger.getLogger("moviejukebox");

	private Properties props = new Properties();
	private TreeMap<String, Movie> library = new TreeMap<String, Movie>();
	private List<Movie> moviesList = new ArrayList<Movie>();
	private Map<String, Map<String, List<Movie>>> indexes = new LinkedHashMap<String, Map<String, List<Movie>>>();
	private boolean filterGenres;
	private Map<String,String> genresMap = new HashMap<String, String>();
        private List<String> certificationOrdering = new ArrayList<String>();
	
	public Library(Properties props) {
            if (props != null) {
                    this.props = props;
            }

            filterGenres = props.getProperty("mjb.filter.genres", "false").equalsIgnoreCase("true");
            
            {
                String temp = props.getProperty("certification.ordering");
                if (temp != null && !temp.isEmpty()) {
                    String[] certs = temp.split(",");
                    certificationOrdering.addAll(Arrays.asList(certs));
                }
            }
	}

	public void addMovie(Movie movie) {
		String key = movie.getTitle();
		if (movie.isTVShow()) {
			key += " Season " + movie.getSeason();
		}

		key = key.toLowerCase();
		
		Movie existingMovie = library.get(key);
		if (existingMovie == null) {
			library.put(key, movie);
		} else {
			existingMovie.addMovieFile(movie.getFirstFile());
		}
	}
	
	public void buildIndex() {
		moviesList.clear();
		indexes.clear();
		
		moviesList.addAll(library.values());		
		if (moviesList.size()>0) {
			sortMovieDetails();	
			indexByProperties();
			indexByGenres();
			indexByTitle();
			indexByCertification();
		}
	}

	private void sortMovieDetails() {
  		Collections.sort(moviesList);
		
		Movie first = moviesList.get(0);
		Movie last = moviesList.get(moviesList.size()-1);
		for (int i = 0; i < moviesList.size(); i++) {
			Movie movie = moviesList.get(i);
			movie.setFirst(first.getBaseName());
			movie.setPrevious(i>0?moviesList.get(i-1).getBaseName():first.getBaseName());
			movie.setNext(i<moviesList.size()-1?moviesList.get(i+1).getBaseName():last.getBaseName());
			movie.setLast(last.getBaseName());
		}
	}

	private void indexByTitle() {
		TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			String title = movie.getStrippedTitleSort();
			if (title.length()>0) {
				Character c = Character.toUpperCase(title.charAt(0));
			
				if (!Character.isLetter(c)) {
					addMovie(index, "09", movie);
				} else {
					addMovie(index, c.toString(), movie);
				} 
			}
		}
		indexes.put("Title",index);
	}

	private void indexByGenres() {
		TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			for ( String genre : movie.getGenres()){
				addMovie(index, getIndexingGenre(genre), movie);
			}
		}
		indexes.put("Genres",index);
	}
	
	private void indexByCertification() {
            TreeMap<String, List<Movie>> index = null;
            if (!certificationOrdering.isEmpty()) {
                index = new TreeMap<String, List<Movie>>(new CertificationComparator(certificationOrdering));
            } else {
                index = new TreeMap<String, List<Movie>>();
            }
            
            for (Movie movie : moviesList) {
                addMovie(index, movie.getCertification(), movie);
            }
            indexes.put("Rating",index);
	}
	
	private void indexByProperties() {
		long oneDay = 1000 * 60 * 60 * 24;
		long oneWeek = oneDay * 7;
		// long oneMonth = oneDay * 30;

		TreeMap<String, List<Movie>> index = new TreeMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			if (movie.getVideoOutput().indexOf("720") != -1  || movie.getVideoOutput().indexOf("1080") != -1) {
				addMovie(index, "HD" , movie);
			}
			
			File f = movie.getFile();
			long delay = System.currentTimeMillis() - f.lastModified();
			
			if (delay <= oneWeek ) {
				addMovie(index, "New", movie);
			} /* else if (delay < oneMonth) {
				addMovie(index, "New this month", movie);
			} */

			addMovie(index, "All", movie);

			if (movie.isTVShow()) {
				addMovie(index, "TV Shows", movie);
			} 
			else {
				addMovie(index, "Movies", movie);
			}
		}
		indexes.put("Other", index);
	}
	
    private String getIndexingGenre(String genre) {
    	if (!filterGenres) 
    		return genre;
    	
    	String masterGenre = genresMap.get(genre);
        if (masterGenre != null ){
        	return masterGenre;
        } else {
        	return genre;
        }
    }



	private void addMovie(TreeMap<String, List<Movie>> index, String category, Movie movie) {
		if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("UNKNOWN"))
			return;
		
		if (movie == null)
			return;
		
		List<Movie> list = index.get(category);
		
		if (list==null) {
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
			if (movies != null) 
				return movies;
		}
	
		return new ArrayList<Movie>();
	}

	public Map<String, Map<String, List<Movie>>> getIndexes() {
		return indexes;
	}
	
	public void fillGenreMap(String xmlGenreFile){
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
			}catch (Exception e) {
				logger.severe("Failed parsing moviejukebox genre input file: " + f.getName());
				e.printStackTrace();
			}
		}else{
			logger.severe("The moviejukebox library input file you specified is invalid: " + f.getName());
		}
	}
}
