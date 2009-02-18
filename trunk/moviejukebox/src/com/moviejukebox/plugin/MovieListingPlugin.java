package com.moviejukebox.plugin;

import com.moviejukebox.model.Library;

/**
 * User: JDGJr
 * Date: Feb 15, 2009
 */
public interface MovieListingPlugin {

  public static String typeMovie = "Movie";
  public static String typeTVShow = "TV Show";
  public static String typeTVShowNoSpace = "TVShow";
  public static String typeTrailer = "Trailer";
  public static String typeAll = "All";

  public static String UNKNOWN = "UNKNOWN";

  public void generate(String tempJukeboxRoot, String jukeboxRoot, Library library);

} // interface MovieListingPlugin
