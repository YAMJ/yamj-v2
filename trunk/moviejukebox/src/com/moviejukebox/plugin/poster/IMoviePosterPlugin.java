package com.moviejukebox.plugin.poster;

public interface IMoviePosterPlugin extends IPosterPlugin {

    public String getIdFromMovieInfo(String title, String year);

    public String getPosterUrl(String title, String year);

    public String getPosterUrl(String id);
}
