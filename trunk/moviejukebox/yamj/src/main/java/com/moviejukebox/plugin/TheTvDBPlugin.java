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
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Episode;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.Cache;
import com.moviejukebox.tools.PropertiesUtil;
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
        language = PropertiesUtil.getProperty("thetvdb.language", defaultLanguage);
        language2nd = PropertiesUtil.getProperty("thetvdb.language.secondary", defaultLanguage);
        // We do not need use the same secondary language... So clearing when equal.
        if (language2nd.equalsIgnoreCase(language)) {
            language2nd = "";
        }
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
        includeWideBanners = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", "false");
        onlySeriesBanners = PropertiesUtil.getBooleanProperty("mjb.onlySeriesBanners", "false");
        cycleSeriesBanners = PropertiesUtil.getBooleanProperty("mjb.cycleSeriesBanners", "true");
        dvdEpisodes = PropertiesUtil.getBooleanProperty("thetvdb.dvd.episodes", "false");
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", "false");
        forceFanartOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFanartOverwrite", "false");
        forceBannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", "false");
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        textBanners = PropertiesUtil.getBooleanProperty("banners.addText.season", "false");

        // We need to set the proxy parameters if set.
        tvDB.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        tvDB.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    @Override
    public boolean scan(Movie movie) {
        ThreadExecutor.enterIO(webhost);
        try {
            return doscan(movie);
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    private boolean doscan(Movie movie) {
        List<Series> seriesList = null;

        String id = movie.getId(THETVDB_PLUGIN_ID);

        if (id == null || id.equals(Movie.UNKNOWN)) {
            if (!movie.getTitle().equals(Movie.UNKNOWN)) {
                seriesList = tvDB.searchSeries(movie.getTitle(), language);
                if ((seriesList == null || seriesList.isEmpty()) && !language2nd.isEmpty()) {
                    seriesList = tvDB.searchSeries(movie.getTitle(), language2nd);
                }
            }

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
            Series series = (Series) Cache.getFromCache(Cache.generateCacheKey("Series", id, language));
            
            // No found in cache, so look online
            if (series == null) {
                series = tvDB.getSeries(id, language);
                if (series != null) {
                    // Add to the cache
                    Cache.addToCache(Cache.generateCacheKey("Series", id, language), series);
                }
            }
            
            if (series == null && !language2nd.isEmpty()) {
                series = (Series) Cache.getFromCache(Cache.generateCacheKey("Series", id, language2nd));
                
                if (series == null) {
                    series = tvDB.getSeries(id, language2nd);
                    if (series != null) {
                        // Add to the cache
                        Cache.addToCache(Cache.generateCacheKey("Series", id, language2nd), series);
                    }
                }
            }

            if (series != null) {
                Banners banners = (Banners) Cache.getFromCache(Cache.generateCacheKey("Banners", id, language));
                
                if (banners == null) {
                    banners = tvDB.getBanners(id);
                    Cache.addToCache(Cache.generateCacheKey("Banners", id, language), banners);
                }
                
                try {
                    if (!movie.isOverrideTitle()) {
                        // issue 1214 : prevent replacing data with blank when TV plugin fails
                        if (series.getSeriesName() != null && series.getSeriesName().trim().length() > 0) {
                            movie.setTitle(series.getSeriesName());
                            movie.setOriginalTitle(series.getSeriesName());
                        }
                    }

                    if (!movie.isOverrideYear()) {
                        String year = tvDB.getSeasonYear(id, movie.getSeason(), language);
                        if (year == null && !language2nd.isEmpty()) {
                            year = tvDB.getSeasonYear(id, movie.getSeason(), language2nd);
                        }
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

                    if (includeWideBanners && isNotValidString(movie.getBannerURL()) || (forceBannerOverwrite) || movie.isDirtyBanner()) {
                        final int season = movie.getSeason();
                        String urlBanner = null;

                        // If we are adding the "Season ?" text to a banner, try searching for these first
                        if (textBanners && !banners.getSeriesList().isEmpty()) {
                            // Trying to grab localized banner at first...
                            urlBanner = findBannerURL2(banners, bannerBlankType, language, season);
                            // In a case of failure - trying to grab banner in alternative language.
                            if (urlBanner == null) {
                                urlBanner = findBannerURL2(banners, bannerBlankType, language2nd, season);
                            }
                        }

                        // Get the specific season banners. If a season banner can't be found, then a generic series banner will be used
                        if (!onlySeriesBanners && !banners.getSeasonList().isEmpty()) {
                            // Trying to grab localized banner at first...
                            urlBanner = findBannerURL(banners, bannerSeasonType, language, season);
                            // In a case of failure - trying to grab banner in alternative language.
                            if (urlBanner == null) {
                                urlBanner = findBannerURL(banners, bannerSeasonType, language2nd, season);
                            }
                        }

                        // If we didn't find a season banner or only want series banners, check for a series banner
                        if (urlBanner == null && !banners.getSeriesList().isEmpty()) {
                            urlBanner = findBannerURL2(banners, bannerSeriesType, language, season);
                            // In a case of failure - trying to grab banner in alternative language.
                            if (urlBanner == null) {
                                urlBanner = findBannerURL2(banners, bannerSeriesType, language2nd, season);
                            }
                        }

                        if (urlBanner != null) {
                            movie.setBannerURL(urlBanner);
                            
                            ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, movie.getBannerFilename(), false);
                            Artwork artwork = new Artwork(ArtworkType.Banner, THETVDB_PLUGIN_ID, urlBanner, artworkFile);
                            movie.addArtwork(artwork);
                            logger.debug("TheTvDBPlugin: Used banner " + urlBanner);
                        }
                    }
                } catch (Exception e) {
                    logger.error("TheTvDBPlugin: Failed to retrieve TheTvDb Id for movie : " + movie.getTitle());
                    logger.error("Error : " + e.getMessage());
                }

                // TODO remove this once all skins are using the new fanart properties
                downloadFanart = FanartScanner.checkDownloadFanart(movie.isTVShow());

                if (downloadFanart && isNotValidString(movie.getFanartURL()) || (forceFanartOverwrite) || (movie.isDirtyFanart())) {
                    String url = null;
                    Artwork artwork = new Artwork();
                    artwork.setSourceSite(THETVDB_PLUGIN_ID);
                    artwork.setType(ArtworkType.Fanart);
                    
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
                        artwork.setUrl(url);
                    }

                    if (isValidString(movie.getFanartURL())) {
                        String artworkFilename = movie.getBaseName() + fanartToken + "." + fanartExtension;
                        movie.setFanartFilename(artworkFilename);
                        artwork.addSize(new ArtworkFile(ArtworkSize.LARGE, artworkFilename, false));
                    }
                    
                    movie.addArtwork(artwork);
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

        ThreadExecutor.enterIO(webhost);
        try {
            List<Episode> episodeList = tvDB.getSeasonEpisodes(id, movie.getSeason(), language);
            List<Episode> episodeList2ndLanguage = null; // Start this null and only populate it if needed
            
            for (MovieFile file : movie.getMovieFiles()) {
                if (movie.getSeason() >= 0) {
                    for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                        Episode episode = null;
                        if (dvdEpisodes) {
                            episode = findDvdEpisode(episodeList, movie.getSeason(), part);
                            if (episode == null && !language2nd.isEmpty()) {
                                if (episodeList2ndLanguage == null) {
                                    episodeList2ndLanguage = tvDB.getSeasonEpisodes(id, movie.getSeason(), language2nd);
                                }
                                episode = findDvdEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                            }
                        }

                        if (episode == null) {
                            //episode = tvDB.getEpisode(id, movie.getSeason(), part, language);
                            episode = findEpisode(episodeList, movie.getSeason(), part);
                            if (episode == null && !language2nd.isEmpty()) {
                                if (episodeList2ndLanguage == null) {
                                    episodeList2ndLanguage = tvDB.getSeasonEpisodes(id, movie.getSeason(), language2nd);
                                }
                                episode = findEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                          }
                        }

                        if (episode != null) {
                            // We only get the writers for the first episode, otherwise we might overwhelm the skins with data
                            // TODO Assign the writers on a per-episode basis, rather than series.
                            if (movie.getWriters().isEmpty()) {
                                movie.setWriters(episode.getWriters());
                            }

                            // TODO Assign the director to each episode.
                            if (((movie.getDirector().equals(Movie.UNKNOWN)) || (movie.getDirector().isEmpty())) && !episode.getDirectors().isEmpty()) {
                                // Director is a single entry, not a list, so only get the first director
                                movie.addDirector(episode.getDirectors().get(0));
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
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    private Episode findEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
        for (Episode episode : episodeList) {
            if (episode.getSeasonNumber() == seasonNumber && episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }
    
    private Episode findDvdEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
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

    private String findBannerURL(final Banners bannerList, final String bannerType, final String languageId, final int season) {
        for (Banner banner : bannerList.getSeasonList()) {
            if (banner.getSeason() == season) {
                if (banner.getBannerType2().equalsIgnoreCase(bannerType)) {
                    if (banner.getLanguage().equalsIgnoreCase(languageId)) {
                        return banner.getUrl();
                    }
                }
            }
        }
        return null;
    }

    private String findBannerURL2(final Banners bannerList, final String bannerType, final String languageId, final int season) {
        int counter = 0;
        String urlBanner = null;
        String savedUrl = null;
        for (Banner banner : bannerList.getSeriesList()) {
            if (banner.getBannerType2().equalsIgnoreCase(bannerType)) {
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
    
}
