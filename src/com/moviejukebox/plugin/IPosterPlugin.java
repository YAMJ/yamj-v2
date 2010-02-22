package com.moviejukebox.plugin;

public interface IPosterPlugin {

    public String getIdFromMovieInfo(String title, String year, boolean isTvShow);

    public String getPosterUrl(String id);

    public String getPosterUrl(String title, String year, boolean isTvShow);
}
