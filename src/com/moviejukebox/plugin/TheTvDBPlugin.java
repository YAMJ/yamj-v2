/*
 *      Copyright (c) 2004-2010 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */

package com.moviejukebox.plugin;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;

import java.util.List;

import org.pojava.datetime.DateTime;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Episode;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;

/**
 * @author styles
 */
public class TheTvDBPlugin extends ImdbPlugin {

    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
    private static final String webhost = "thetvdb.com";
    private TheTVDB tvDB;
    private String language;
    private boolean forceBannerOverwrite;
    private boolean forceFanartOverwrite;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private boolean includeWideBanners;
    private boolean onlySeriesBanners;
    private boolean cycleSeriesBanners;
    private boolean textBanners;
    private boolean dvdEpisodes = false;
    private static String bannerSeasonType = "seasonwide";
    private static String bannerSeriesType = "graphical";
    private static String bannerBlankType = "blank";
    private int preferredPlotLength;

    public TheTvDBPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", "en");
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
        includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
        includeWideBanners = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeWideBanners", "false"));
        onlySeriesBanners = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.onlySeriesBanners", "false"));
        cycleSeriesBanners = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.cycleSeriesBanners", "true"));
        dvdEpisodes = Boolean.parseBoolean(PropertiesUtil.getProperty("thetvdb.dvd.episodes", "false"));
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.tv.download", "false"));
        forceFanartOverwrite = Boolean.parseBoolean(getProperty("mjb.forceFanartOverwrite", "false"));
        forceBannerOverwrite = Boolean.parseBoolean(getProperty("mjb.forceBannersOverwrite", "false"));
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
        textBanners = Boolean.parseBoolean(PropertiesUtil.getProperty("banners.addText.season", "false"));
    }

    @Override
    public boolean scan(Movie movie) {
        ThreadExecutor.EnterIO(webhost);
        try {
            return doscan(movie);
        } finally {
            ThreadExecutor.LeaveIO();
        }
    }

    private boolean doscan(Movie movie) {
        List<Series> seriesList = null;

        String id = movie.getId(THETVDB_PLUGIN_ID);

        if (id == null || id.equals(Movie.UNKNOWN)) {
            if (!movie.getTitle().equals(Movie.UNKNOWN)) {
                seriesList = tvDB.searchSeries(movie.getTitle(), language);
            }
            
            if (seriesList == null || seriesList.isEmpty()) {
                seriesList = tvDB.searchSeries(movie.getBaseName(), language);
            }
            
            if (seriesList != null && !seriesList.isEmpty()) {
                Series series = null;
                for (Series s : seriesList) {
                    if (s.getFirstAired() != null && !s.getFirstAired().isEmpty()) {
                        if (movie.getYear() != null && !movie.getYear().equals(Movie.UNKNOWN)) {
                            try {
                                DateTime firstAired = DateTime.parse(s.getFirstAired());
                                if (Integer.parseInt(firstAired.toString("yyyy")) == Integer.parseInt(movie.getYear())) {
                                    series = s;
                                    break;
                                }
                            } catch (Exception ignore) {
                            }
                        } else {
                            series = s;
                            break;
                        }
                    }
                }
                
                if (series == null) {
                    series = seriesList.get(0);
                }
                
                id = series.getId();
                movie.setId(THETVDB_PLUGIN_ID, id);
                
                if (series.getImdbId() != null && !series.getImdbId().isEmpty()) {
                    movie.setId(IMDB_PLUGIN_ID, series.getImdbId());
                }
            }
        }

        if (id != null && !id.equals(Movie.UNKNOWN)) {
            Series series = tvDB.getSeries(id, language);

            if (series != null) {

                Banners banners = tvDB.getBanners(id);

                if (!movie.isOverrideTitle()) {
                    // issue 1214 : prevent replacing data with blank when TV plugin fails
                    if (series.getSeriesName() != null && series.getSeriesName().trim().length() > 0) {
                        movie.setTitle(series.getSeriesName());
                        movie.setOriginalTitle(series.getSeriesName());
                    }
                }
                
                if (!movie.isOverrideYear()) {
                    String year = tvDB.getSeasonYear(id, movie.getSeason(), language);
                    if (year != null && !year.isEmpty()) {
                        movie.setYear(year);
                    }
                }
                
                if (movie.getRating() == -1 && series.getRating() != null && !series.getRating().isEmpty()) {
                    movie.setRating((int)(Float.parseFloat(series.getRating()) * 10));
                }
                
                if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                    movie.setRuntime(series.getRuntime());
                }
                
                if (movie.getCompany().equals(Movie.UNKNOWN)) {
                    movie.setCompany(series.getNetwork());
                }
                
                if (movie.getGenres().isEmpty()) {
                    movie.setGenres(series.getGenres());
                }
                
                if (movie.getPlot().equals(Movie.UNKNOWN)) {
                    movie.setPlot(series.getOverview());
                }
                
                if (movie.getCertification().equals(Movie.UNKNOWN)) {
                    movie.setCertification(series.getContentRating());
                }
                
                if (movie.getCast().isEmpty()) {
                    movie.setCast(series.getActors());
                }

                if (includeWideBanners && (movie.getBannerURL().equalsIgnoreCase(Movie.UNKNOWN)) || (forceBannerOverwrite) || movie.isDirtyBanner()) {
                    String urlBanner = null;

                    // If we are adding the "Season ?" text to a banner, try searching for these first 
                    if (textBanners && !banners.getSeriesList().isEmpty()) {
                        String savedUrl = null;
                        int counter = 0;

                        for (Banner banner : banners.getSeriesList()) {
                            if (banner.getBannerType2().equalsIgnoreCase(bannerBlankType)) {
                                // Increment the counter (before the test) and see if this is the right season
                                if ((++counter == movie.getSeason()) || !cycleSeriesBanners) {
                                    urlBanner = banner.getUrl();
                                    break;
                                } else {
                                    // Save the URL in case this is the last one we find
                                    savedUrl = banner.getUrl();
                                }
                            }
                        }
                        // Check to see if we found a banner
                        if (urlBanner == null) {
                            // No banner found, so use the last banner
                            urlBanner = savedUrl;
                        }
                    }
                    
                    // Get the specific season banners. If a season banner can't be found, then a generic series banner will be used
                    if (!banners.getSeasonList().isEmpty() && !onlySeriesBanners) {
                        for (Banner banner : banners.getSeasonList()) {
                            if (banner.getSeason() == movie.getSeason()) { // only check for the correct season
                                // Look for season wide banners if requested
                                if (urlBanner == null && banner.getBannerType2().equalsIgnoreCase(bannerSeasonType)) {
                                    urlBanner = banner.getUrl();
                                    break;
                                }
                            }
                        }
                    }
                    
                    // If we didn't find a season banner or only want series banners, check for a series banner
                    if (urlBanner == null && !banners.getSeriesList().isEmpty()) {
                        String savedUrl = null;
                        int counter = 0;

                        for (Banner banner : banners.getSeriesList()) {
                            if (banner.getBannerType2().equalsIgnoreCase(bannerSeriesType)) {
                                // Increment the counter (before the test) and see if this is the right season
                                if ((++counter == movie.getSeason()) || !cycleSeriesBanners) {
                                    urlBanner = banner.getUrl();
                                    break;
                                } else {
                                    // Save the URL in case this is the last one we find
                                    savedUrl = banner.getUrl();
                                }
                            }
                        }
                        // Check to see if we found a banner
                        if (urlBanner == null) {
                            // No banner found, so use the last banner
                            urlBanner = savedUrl;
                        }
                    }
                    
                    if (urlBanner != null) {
                        movie.setBannerURL(urlBanner);
                    }
                }

                // TODO remove this once all skins are using the new fanart properties
                downloadFanart = checkDownloadFanart(movie.isTVShow());

                if (downloadFanart && (movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) || (forceFanartOverwrite) || (movie.isDirtyFanart())) {
                    String url = null;
                    if (!banners.getFanartList().isEmpty()) {
                        int index = movie.getSeason();
                        if (index <= 0) {
                            index = 1;
                        } else if (index > banners.getFanartList().size()) {
                            index = banners.getFanartList().size();
                        }
                        index--;

                        url = banners.getFanartList().get(index).getUrl();
                    }
                    if (url == null && series.getFanart() != null && !series.getFanart().isEmpty()) {
                        url = series.getFanart();
                    }
                    if (url != null) {
                        movie.setFanartURL(url);
                    }

                    if (movie.getFanartURL() != null && !movie.getFanartURL().isEmpty() && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                        movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                    }
                }
                
                // we may not have here the semaphore acquired, could lead to deadlock if limit is 1 and this function also needs a slot
                scanTVShowTitles(movie);
            }
        }

        return true;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        String id = movie.getId(THETVDB_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || id == null) {
            return;
        }

        ThreadExecutor.EnterIO(webhost);
        try{
          for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    Episode episode = null;
                    if (dvdEpisodes) {
                        episode = tvDB.getDVDEpisode(id, movie.getSeason(), part, language);
                    }
                    
                    if (episode == null) {
                        episode = tvDB.getEpisode(id, movie.getSeason(), part, language);
                    }

                    if (episode != null) {
                        // We only get the writers for the first episode, otherwise we might overwhelm the skins with data
                        // TODO Assign the writers on a per-episode basis, rather than series.
                        if ((movie.getWriters().equals(Movie.UNKNOWN)) || (movie.getWriters().isEmpty())) {
                            movie.setWriters(episode.getWriters());
                        }

                        // TODO Assign the director to each episode.
                        if (((movie.getDirector().equals(Movie.UNKNOWN)) || (movie.getDirector().isEmpty())) && !episode.getDirectors().isEmpty()) {
                            // Director is a single entry, not a list, so only get the first director
                            movie.setDirector(episode.getDirectors().get(0));
                        }

                        // Set the title of the episode
                        if (file.getTitle(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            file.setTitle(part, episode.getEpisodeName());
                        }

                        if (includeEpisodePlots) {
                            if (file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                                String episodePlot = episode.getOverview();
                                if (episodePlot.length() > preferredPlotLength) {
                                    episodePlot = episodePlot.substring(0, Math.min(episodePlot.length(), preferredPlotLength - 3)) + "...";
                                }
                                file.setPlot(part, episodePlot);
                            }
                        }

                        if (includeVideoImages) {
                            file.setVideoImageURL(part, episode.getFilename());
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    } else {
                        // This occurs if the episode is not found
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && file.getPlot(part).equalsIgnoreCase(Movie.UNKNOWN)) {
                            // This sets the zero part's title to be either the filename title or blank rather than the next episode's title
                            file.setTitle(part, "Special");
                        }
                    }
                }
            }
          }
        }finally{
            ThreadExecutor.LeaveIO();
        }
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie);

        logger.finest("Scanning NFO for TheTVDB Id");
        String compareString = nfo.toUpperCase();
        int idx = compareString.indexOf("THETVDB.COM");
        if (idx > -1) {
            int beginIdx = compareString.indexOf("&ID=");
            int length = 4;
            if (beginIdx < idx) {
                beginIdx = compareString.indexOf("?ID=");
            }
            if (beginIdx < idx) {
                beginIdx = compareString.indexOf("&SERIESID=");
                length = 10;
            }
            if (beginIdx < idx) {
                beginIdx = compareString.indexOf("?SERIESID=");
                length = 10;
            }

            if (beginIdx > idx) {
                int endIdx = compareString.indexOf("&", beginIdx + 1);
                String id = null;
                if (endIdx > -1) {
                    id = compareString.substring(beginIdx + length, endIdx);
                } else {
                    id = compareString.substring(beginIdx + length);
                }
                if (id != null && !id.isEmpty()) {
                    movie.setId(THETVDB_PLUGIN_ID, id.trim());
                    logger.finer("TheTVDB Id found in nfo = " + id.trim());
                }
            }
        }
    }
}
