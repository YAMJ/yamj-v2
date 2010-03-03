package com.moviejukebox.plugin.poster;

public interface ITvShowPosterPlugin extends IPosterPlugin {
    public String getIdFromMovieInfo(String title, String year, int tvSeason);

    public String getPosterUrl(String title, String year, int tvSeason);

    public String getPosterUrl(String id, int season);

}
