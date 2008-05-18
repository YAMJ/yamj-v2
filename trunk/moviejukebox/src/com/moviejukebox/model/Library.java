package com.moviejukebox.model;

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
	private HashMap<String, List<Movie>> indexes = new HashMap<String, List<Movie>>();
	
	public List<Movie> getMoviesList() {
		return moviesList;
	}

	public void setMoviesList(List<Movie> moviesList) {
		this.moviesList = moviesList;
	}

	public HashMap<String, List<Movie>> getIndexes() {
		return indexes;
	}

	public void setIndexes(HashMap<String, List<Movie>> indexes) {
		this.indexes = indexes;
	}

	public void addMovie(Movie movie) {
		String key = movie.getTitle();
		if (movie.isTVShow()) {
			key += " Season " + movie.getSeason();
		}

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
	
			for (Movie movie : moviesList) {				
				String title = movie.getTitle();
				if (title.length()>0) {
					Character c = Character.toUpperCase(title.charAt(0));
				
					if (!Character.isLetter(c) || (c>='A' && c<='F')) {
						addMovie("09AF", movie);
					} else if (c>='G' && c<='L') {
						addMovie("GL", movie);
					} else if (c>='M' && c<='R') {
						addMovie("MR", movie);
					} else if (c>='S' && c<='Z') {
						addMovie("SZ", movie);
					}
				}
			}
		}
	}

	private void addMovie(String category, Movie movie) {
		List<Movie> list = indexes.get(category);
		
		if (list==null) {
			list = new ArrayList<Movie>();
			indexes.put(category, list);
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
}
