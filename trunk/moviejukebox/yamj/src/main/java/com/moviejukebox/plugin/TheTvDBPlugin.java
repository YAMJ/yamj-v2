/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.trimToLength;

import java.util.List;

import org.pojava.datetime.DateTime;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.BannerType;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Episode;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.Cache;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;

/**
 * @author styles
 */
public class TheTvDBPlugin extends ImdbPlugin {

    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
    private static final String webhost = "thetvdb.com";
    private static final String defaultLanguage = "en";

    private TheTVDB tvDB;
    private String language;
    private String language2nd;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private boolean dvdEpisodes = false;
    private int preferredPlotLength;
    private List<Episode> episodeList;
    private List<Episode> episodeList2ndLanguage;
    
    public TheTvDBPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", defaultLanguage);
        language2nd = PropertiesUtil.getProperty("thetvdb.language.secondary", defaultLanguage);
        // We do not need use the same secondary language... So clearing when equal.
        if (language2nd.equalsIgnoreCase(language)) {
            language2nd = "";
        }
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
        dvdEpisodes = PropertiesUtil.getBooleanProperty("thetvdb.dvd.episodes", "false");
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", "false");
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        
        // Make sure the episode lists are empty
        episodeList = null;
        episodeList2ndLanguage = null;

        // We need to set the proxy parameters if set.
        tvDB.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        tvDB.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    @Override
    public boolean scan(Movie movie) {
        String id = getId(movie);
        
        if (isValidString(id)) {
            Series series = getSeries(id);

            if (series != null) {
                try {
                    if (!movie.isOverrideTitle()) {
                        // issue 1214 : prevent replacing data with blank when TV plugin fails
                        if (series.getSeriesName() != null && series.getSeriesName().trim().length() > 0) {
                            movie.setTitle(series.getSeriesName());
                            movie.setOriginalTitle(series.getSeriesName());
                        }
                    }

                    if (!movie.isOverrideYear()) {
                        String year = Movie.UNKNOWN;
                        
                        ThreadExecutor.enterIO(webhost);
                        try {
                            year = tvDB.getSeasonYear(id, movie.getSeason(), language);
                            if (year == null && !language2nd.isEmpty()) {
                                year = tvDB.getSeasonYear(id, movie.getSeason(), language2nd);
                            }
                        } finally {
                            ThreadExecutor.leaveIO();
                        }

                        if (StringTools.isValidString(year)) {
                            movie.setYear(year);
                        }
                    }

                    if (movie.getRating() == -1 && series.getRating() != null && !series.getRating().isEmpty()) {
                        movie.addRating(THETVDB_PLUGIN_ID, (int)(Float.parseFloat(series.getRating()) * 10));
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
                } catch (Exception e) {
                    logger.error("TheTvDBPlugin: Failed to retrieve TheTvDb Id for movie : " + movie.getTitle());
                    logger.error("Error : " + e.getMessage());
                }

                scanTVShowTitles(movie);
            } else {
                // The series or ID wasn't found
                return false;
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

        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    Episode episode = findEpisode(id, movie.getSeason(), part);
                    
                    if (episode != null) {
                        // We only get the writers for the first episode, otherwise we might overwhelm the skins with data
                        // TODO Assign the writers on a per-episode basis, rather than series.
                        if (movie.getWriters().isEmpty()) {
                            movie.setWriters(episode.getWriters());
                        }

                        // TODO Assign the director to each episode.
                        if (((movie.getDirector().equals(Movie.UNKNOWN)) || (movie.getDirector().isEmpty())) && !episode.getDirectors().isEmpty()) {
                            if (movie.getDirectors().isEmpty()) {
                                movie.setDirectors(episode.getDirectors());
                            }
                        }

                        if (isNotValidString(file.getAirsAfterSeason(part))) {
                            file.setAirsAfterSeason(part, "" + episode.getAirsAfterSeason());
                        }

                        if (isNotValidString(file.getAirsBeforeSeason(part))) {
                            file.setAirsBeforeSeason(part, "" + episode.getAirsBeforeSeason());
                        }

                        if (isNotValidString(file.getAirsBeforeEpisode(part))) {
                            file.setAirsBeforeEpisode(part, "" + episode.getAirsBeforeEpisode());
                        }
                        
                        if (isNotValidString(file.getFirstAired(part))) {
                            file.setFirstAired(part, episode.getFirstAired());
                        }

                        // Set the title of the episode
                        if (isNotValidString(file.getTitle(part))) {
                            file.setTitle(part, episode.getEpisodeName());
                        }

                        if (includeEpisodePlots) {
                            if (isNotValidString(file.getPlot(part))) {
                                String episodePlot = episode.getOverview();
                                episodePlot = trimToLength(episodePlot, preferredPlotLength, true, plotEnding);
                                file.setPlot(part, episodePlot);
                            }
                        }

                        includeVideoImages = false;
                        if (includeVideoImages) {
                            file.setVideoImageURL(part, episode.getFilename());
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    } else {
                        // This occurs if the episode is not found
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && isNotValidString(file.getPlot(part))) {
                            // This sets the zero part's title to be either the filename title or blank rather than the next episode's title
                            file.setTitle(part, "Special");
                        }
                    }
                }
            }
        }
        
        // Clear the current episode lists
        episodeList = null;
        episodeList2ndLanguage = null;
    }

    /**
     * Find the episode from the list of episodes. Will automatically load the episode lists as needed and determine which type of episode (Aired/DVD) to get
     * @param id
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    public Episode findEpisode(String id, int seasonNumber, int episodeNumber) {
        Episode episode = null;
        
        if (isNotValidString(id)) {
            return episode;
        }
        
        // If we have no episode list, try and get one
        if (episodeList == null || episodeList.isEmpty()) {
            episodeList = getEpisodeList(id, seasonNumber, language);
        }
        
        if (dvdEpisodes) {
            episode = findDvdEpisode(episodeList, seasonNumber, episodeNumber);
            if (episode == null) {
                if (episodeList2ndLanguage == null || episodeList2ndLanguage.isEmpty()) {
                    episodeList2ndLanguage = getEpisodeList(id, seasonNumber, language2nd);
                }
                episode = findDvdEpisode(episodeList2ndLanguage, seasonNumber, episodeNumber);
            }
        }

        if (episode == null) {
            episode = findAiredEpisode(episodeList, seasonNumber, episodeNumber);
            if (episode == null) {
                if (episodeList2ndLanguage == null || episodeList2ndLanguage.isEmpty()) {
                    episodeList2ndLanguage = getEpisodeList(id, seasonNumber, language2nd);
                }
                episode = findAiredEpisode(episodeList2ndLanguage, seasonNumber, episodeNumber);
          }
        }
        return episode;
    }

    /**
     * Get the aired episode information from the list of episodes
     * @param episodeList
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    private Episode findAiredEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
        if (episodeList == null || episodeList.isEmpty()) {
            return null;
        }
        
        for (Episode episode : episodeList) {
            if (episode.getSeasonNumber() == seasonNumber && episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }
    
    /**
     * Get the DVD episode imformation from the list of episode based on the DVD episode number
     * @param episodeList
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    private Episode findDvdEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
        if (episodeList == null || episodeList.isEmpty()) {
            return null;
        }
        
        for (Episode episode : episodeList) {
            if (episode.getSeasonNumber() == seasonNumber && episode.getDvdEpisodeNumber().equals(""+episodeNumber)) {
                return episode;
            }
        }
        return null;
    }
    
    @Override
    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie);

        logger.debug("Scanning NFO for TheTVDB Id");
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
                    id = new String(compareString.substring(beginIdx + length, endIdx));
                } else {
                    id = new String(compareString.substring(beginIdx + length));
                }
                if (id != null && !id.isEmpty()) {
                    movie.setId(THETVDB_PLUGIN_ID, id.trim());
                    logger.debug("TheTVDB Id found in nfo = " + id.trim());
                }
            }
        }
    }

    public String findBannerURL(final Banners bannerList, final BannerType bannerType, final String languageId, final int season) {
        for (Banner banner : bannerList.getSeasonList()) {
            if (banner.getSeason() == season) {
                if (banner.getBannerType2() == bannerType) {
                    if (banner.getLanguage().equalsIgnoreCase(languageId)) {
                        return banner.getUrl();
                    }
                }
            }
        }
        return null;
    }

    public String findBannerURL2(final Banners bannerList, final BannerType bannerType, final String languageId, final int season, boolean cycleSeriesBanners) {
        int counter = 0;
        String urlBanner = null;
        String savedUrl = null;
        for (Banner banner : bannerList.getSeriesList()) {
            if (banner.getBannerType2() == bannerType) {
                if (banner.getLanguage().equalsIgnoreCase(languageId)) {
                    // Increment the counter (before the test) and see if this is the right season
                    if (++counter == season || !cycleSeriesBanners) {
                        urlBanner = banner.getUrl();
                        break;
                    } else {
                        // Save the URL in case this is the last one we find
                        savedUrl = banner.getUrl();
                    }
                }
            }
        }
        
        // Check to see if we found a banner
        if (urlBanner == null) {
            // No banner found, so use the last banner
            urlBanner = savedUrl;
        }
        
        return urlBanner;
    }
 
    /**
     * Get a list of episodes from TheTVDB
     * @param id
     * @param seasonNumber
     * @param language
     * @return
     */
    public List<Episode> getEpisodeList(String id, int seasonNumber, String language) {
        if (isNotValidString(id) || isNotValidString(language) || seasonNumber < 0) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        List<Episode> episodeList = (List<Episode>) Cache.getFromCache(Cache.generateCacheKey("EpisodeList", id, ""+seasonNumber, language));

        // Not found in cache, so look online
        if (episodeList == null) {
            ThreadExecutor.enterIO(webhost);
            try {
                episodeList = tvDB.getSeasonEpisodes(id, seasonNumber, language);
            } finally {
                ThreadExecutor.leaveIO();
            }
            
            if (episodeList != null) {
                // Add to the cache
                Cache.addToCache(Cache.generateCacheKey("EpisodeList", id, ""+seasonNumber, language), episodeList);
            }
        }

        return episodeList;
    }
    
    /**
     * Get the banners using the ID from the Cache if possible.
     * @param id
     * @return
     */
    public Banners getBanners(String id) {
        if (isNotValidString(id)) {
            return null;
        }
        
        Banners banners = (Banners) Cache.getFromCache(Cache.generateCacheKey("Banners", id, language));
        
        // Not found in cache, so look online
        if (banners == null) {
            ThreadExecutor.enterIO(webhost);
            try {
                banners = tvDB.getBanners(id);
            } finally {
                ThreadExecutor.leaveIO();
            }
            
            if (banners != null) {
                // Add to the cache
                Cache.addToCache(Cache.generateCacheKey("Banners", id, language), banners);
                return banners;
            }
        }
        
        return banners;
    }
    
    /**
     * Get the series from the ID using the Cache if possible.
     * @param id
     * @return
     */
    public Series getSeries(String id) {
        if (isNotValidString(id)) {
            return null;
        }
        
        Series series = (Series) Cache.getFromCache(Cache.generateCacheKey("Series", id, language));
        
        // Not found in cache, so look online
        if (series == null) {
            ThreadExecutor.enterIO(webhost);
            try {
                series = tvDB.getSeries(id, language);
                if (series != null) {
                    // Add to the cache
                    Cache.addToCache(Cache.generateCacheKey("Series", id, language), series);
                    return series;
                }
            } finally {
                ThreadExecutor.leaveIO();
            }
        }
        
        if (series == null && !language2nd.isEmpty()) {
            series = (Series) Cache.getFromCache(Cache.generateCacheKey("Series", id, language2nd));
            
            if (series == null) {
                ThreadExecutor.enterIO(webhost);
                try {
                    series = tvDB.getSeries(id, language2nd);
                    if (series != null) {
                        // Add to the cache
                        Cache.addToCache(Cache.generateCacheKey("Series", id, language2nd), series);
                        return series;
                    }
                } finally {
                    ThreadExecutor.leaveIO();
                }
            }
        }

        return series;
    }
    
    /**
     * Get TheTVDB ID from the movie details.
     * @param movie
     * @return
     */
    public String getId(Movie movie) {
        String id = movie.getId(THETVDB_PLUGIN_ID);

        if (isValidString(id)) {
            return id;
        }
        
        List<Series> seriesList = null;
        
        if (isNotValidString(id)) {
            ThreadExecutor.enterIO(webhost);
            try {
                // Use the title to search
                if (isValidString(movie.getTitle())) {
                    seriesList = tvDB.searchSeries(movie.getTitle(), language);
                    if ((seriesList == null || seriesList.isEmpty()) && !language2nd.isEmpty()) {
                        seriesList = tvDB.searchSeries(movie.getTitle(), language2nd);
                    }
                }
                
                // Try to use the original title
                if (seriesList == null || seriesList.isEmpty()) {
                    if (!movie.getTitle().equalsIgnoreCase(movie.getOriginalTitle())) {
                        seriesList = tvDB.searchSeries(movie.getBaseName(), language);
                        if ((seriesList == null || seriesList.isEmpty()) && !language2nd.isEmpty()) {
                            seriesList = tvDB.searchSeries(movie.getBaseName(), language2nd);
                        }
                    }
                }
    
                // Finally use the base name to search
                if (seriesList == null || seriesList.isEmpty()) {
                    seriesList = tvDB.searchSeries(movie.getBaseName(), language);
                    if ((seriesList == null || seriesList.isEmpty()) && !language2nd.isEmpty()) {
                        seriesList = tvDB.searchSeries(movie.getBaseName(), language2nd);
                    }
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
    
                    // Default to the first series returned, and hope it's correct.
                    if (series == null) {
                        series = seriesList.get(0);
                        logger.debug("TheTvDBPlugin: No exact match found for " + movie.getTitle() + ", assuming " + series.getSeriesName() + "("+ series.getId() +")");
                    }
    
                    id = "" + series.getId();
                    movie.setId(THETVDB_PLUGIN_ID, id);
    
                    if (series.getImdbId() != null && !series.getImdbId().isEmpty()) {
                        movie.setId(IMDB_PLUGIN_ID, series.getImdbId());
                    }
                }
            } finally {
                ThreadExecutor.leaveIO();
            }
        }
        
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getLanguage2nd() {
        return language2nd;
    }
}
