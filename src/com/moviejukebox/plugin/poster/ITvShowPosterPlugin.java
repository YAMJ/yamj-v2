package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;

public interface ITvShowPosterPlugin extends IPosterPlugin {
    public String getIdFromMovieInfo(String title, String year, int tvSeason);

    public IImage getPosterUrl(String title, String year, int tvSeason);

    public IImage getPosterUrl(String id, int season);

}
