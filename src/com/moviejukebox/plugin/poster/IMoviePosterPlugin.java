package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;

public interface IMoviePosterPlugin extends IPosterPlugin {

    public String getIdFromMovieInfo(String title, String year);

    public IImage getPosterUrl(String title, String year);

    public IImage getPosterUrl(String id);
}
