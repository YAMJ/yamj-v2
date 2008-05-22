package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;

public interface MovieDatabasePlugin {

	void scan(Movie movie);

	void downloadPoster(String jukeboxRoot, Movie movie);

}
