package com.moviejukebox.plugin;

import java.util.Properties;

import com.moviejukebox.model.Movie;

public interface MovieDatabasePlugin {

	public void init(Properties props);
	
	public void scan(Movie movie);

	public void downloadPoster(String jukeboxRoot, Movie movie);

}
