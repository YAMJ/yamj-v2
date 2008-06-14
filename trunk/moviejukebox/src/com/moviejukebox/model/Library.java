package com.moviejukebox.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Library implements Map<String, Movie> {

	private HashMap<String, Movie> library = new HashMap<String, Movie>();
	private List<Movie> moviesList = new ArrayList<Movie>();
	private HashMap<String, HashMap<String, List<Movie>>> indexes = new HashMap<String, HashMap<String, List<Movie>>>();
	
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
			indexAlphabetically();
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

	private void indexAlphabetically() {
		HashMap<String, List<Movie>> index = new HashMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			String title = movie.getTitle();
			if (title.length()>0) {
				Character c = Character.toUpperCase(title.charAt(0));
			
				if (!Character.isLetter(c)) {
					addMovie(index, "09", movie);
				} else {
					addMovie(index, c.toString(), movie);
				} 
			}
		}
		indexes.put("Alphabetical",index);
	}

	private void indexByGenres() {
		HashMap<String, List<Movie>> index = new HashMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			if (movie.isTVShow()) {
				addMovie(index, "TV Show", movie);
			} else if (movie.getGenres().size()>0) {
				String genre = movie.getGenres().iterator().next();
				addMovie(index, genre, movie);
			}
		}
		indexes.put("Genres",index);
	}
	
	private void indexByProperties() {
		long oneDay = 1000 * 60 * 60 * 24;
		long oneWeek = oneDay * 7;
		long oneMonth = oneWeek * 30;

		HashMap<String, List<Movie>> index = new HashMap<String, List<Movie>>();
		for (Movie movie : moviesList) {				
			if (movie.getVideoOutput().indexOf("720") != -1  || movie.getVideoOutput().indexOf("1080") != -1) {
				addMovie(index, "HD" , movie);
			}
			
			File f = movie.getFile();
			long delay = System.currentTimeMillis() - f.lastModified();
			
			if (delay <= oneWeek ) {
				addMovie(index, "New this week", movie);
			} else if (delay < oneMonth) {
				addMovie(index, "New this month", movie);
			}

		}
		indexes.put("Other", index);
	}

	private void addMovie(HashMap<String, List<Movie>> index, String category, Movie movie) {
		if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("UNKNOWN"))
			return;
		
		if (movie == null)
			return;
		
		List<Movie> list = index.get(category);
		
		if (list==null) {
			list = new ArrayList<Movie>();
			index.put(category, list);
		}
		
		list.add(movie);
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
		for (HashMap<String, List<Movie>> index : indexes.values()) {
			List<Movie> movies = index.get(key);
			if (movies != null) 
				return movies;
		}
	
		return new ArrayList<Movie>();
	}

	public HashMap<String, HashMap<String, List<Movie>>> getIndexes() {
		return indexes;
	}
}
