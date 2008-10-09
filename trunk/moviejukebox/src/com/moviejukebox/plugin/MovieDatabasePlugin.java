package com.moviejukebox.plugin;

import java.util.Properties;

import com.moviejukebox.model.Movie;

/**
 * MovieDatabasePlugin classes must implement this interface in order 
 * to be integrated to moviejukebox.
 * 
 * Custom implementations classes must be registered into the 
 * moviejukebox.properties file (see property "mjb.internet.plugin").
 * 
 * Once the class is registered, is called once at the program init,
 * then for each movie in the library.
 * 
 * @author Julien
 */
public interface MovieDatabasePlugin {

	/**
	 * Called by movie jukebox at program initialisation.
	 * Contains the moviejukebox.properties
	 * 
	 * @param props moviejukebox properties
	 */
	public void init(Properties props);

	/**
	 * Called by movie jukebox when processing a movie.
	 * 
	 * Scan information for the specified movie.
	 * The provided movie object conatins at least 
	 * the following data:
	 * <ul>
	 * <li>Title
	 * <li>Year (can be unknown)
	 * <li>Season (can be unknown)
	 * <li>movieFiles (at least one)
	 * </ul>
	 * 
	 * @param movie a <tt>Movie</tt> object to update.
	 */
	public void scan(Movie movie);

	/**
	 * Called by jukebox when there are new episodes files added.
	 * Method checks if movie id exists and scan only for new MovieFiles.
	 *
	 *
	 * @param movie a <tt>Movie</tt> object to update.
	 */
	public void scanTVShowTitles(Movie movie);

	/**
	 * Scan NFO file for movie id
	 *
	 * @param nfo
	 * @param movie
	 */
	public void scanNFO(String nfo, Movie movie);
}
