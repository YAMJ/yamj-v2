/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkFile;
import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.tools.cache.CacheMemory;
import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Episode;
import com.omertron.thetvdbapi.model.Series;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;

/**
 * @author styles
 */
public class TheTvDBPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(TheTvDBPlugin.class);
    private static final String LOG_MESSAGE = "TheTVDBPlugin: ";
    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
    private static final String webhost = "thetvdb.com";
    private static final String defaultLanguage = "en";
    public static final String CACHE_SERIES = "Series";
    public static final String CACHE_BANNERS = "Banners";
    private static TheTVDBApi tvDB = new TheTVDBApi(API_KEY);
    private static String language = PropertiesUtil.getProperty("thetvdb.language", defaultLanguage).trim();
    private static String language2nd = initLanguage2();
    private boolean forceBannerOverwrite;
    private boolean forceFanartOverwrite;
    private boolean includeEpisodePlots;
    private boolean includeEpisodeRating;
    private boolean includeVideoImages;
    private boolean includeWideBanners;
    private boolean onlySeriesBanners;
    private boolean cycleSeriesBanners;
    private boolean textBanners;
    private boolean dvdEpisodes = false;
    private int preferredPlotLength;

    public TheTvDBPlugin() {
        super();
        includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", FALSE);
        includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", FALSE);
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", FALSE);
        includeWideBanners = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", FALSE);
        onlySeriesBanners = PropertiesUtil.getBooleanProperty("mjb.onlySeriesBanners", FALSE);
        cycleSeriesBanners = PropertiesUtil.getBooleanProperty("mjb.cycleSeriesBanners", TRUE);
        dvdEpisodes = PropertiesUtil.getBooleanProperty("thetvdb.dvd.episodes", FALSE);
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", FALSE);
        forceFanartOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFanartOverwrite", FALSE);
        forceBannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", FALSE);
        preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
        textBanners = PropertiesUtil.getBooleanProperty("banners.addText.season", FALSE);

        // We need to set the proxy parameters if set.
        tvDB.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        tvDB.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    private static String initLanguage2() {
        String lang = PropertiesUtil.getProperty("thetvdb.language.secondary", defaultLanguage).trim();
        // We do not need use the same secondary language... So clearing when equal.
        if (lang.equalsIgnoreCase(language)) {
            lang = "";
        }
        return lang;
    }

    @Override
    public String getPluginID() {
        return THETVDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String id = findId(movie);

        if (StringTools.isValidString(id)) {
            Series series = getSeries(id);

            if (series == null) {
                logger.debug(LOG_MESSAGE + "No series information found for " + movie.getTitle());
            } else {
                if (OverrideTools.checkOverwriteTitle(movie, THETVDB_PLUGIN_ID)) {
                    movie.setTitle(series.getSeriesName(), THETVDB_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(movie, THETVDB_PLUGIN_ID)) {
                    movie.setOriginalTitle(series.getSeriesName(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteYear(movie, THETVDB_PLUGIN_ID)) {
                    String year = Movie.UNKNOWN;

                    ThreadExecutor.enterIO(webhost);
                    try {
                        year = tvDB.getSeasonYear(id, movie.getSeason(), language);
                        if (StringTools.isNotValidString(year) && StringTools.isValidString(language2nd)) {
                            year = tvDB.getSeasonYear(id, movie.getSeason(), language2nd);
                        }
                    } finally {
                        ThreadExecutor.leaveIO();
                    }

                    movie.setYear(year, THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteReleaseDate(movie, THETVDB_PLUGIN_ID)) {
                    // Set the release date to be the series first aired date
                    movie.setReleaseDate(series.getFirstAired(), THETVDB_PLUGIN_ID);
                }

                if (isNotValidString(movie.getShowStatus())) {
                    // Set the show status
                    movie.setShowStatus(series.getStatus());
                }

                if (movie.getRating(THETVDB_PLUGIN_ID) == -1 && StringUtils.isNotBlank(series.getRating())) {
                    try {
                        movie.addRating(THETVDB_PLUGIN_ID, (int) (Float.parseFloat(series.getRating()) * 10));
                    } catch (NumberFormatException ex) {
                        logger.trace(LOG_MESSAGE + "Failed to transform rating for series id " + series.getId() + " = '" + series.getRating() + "', Error: " + ex.getMessage());
                    }
                }

                if (OverrideTools.checkOverwriteRuntime(movie, THETVDB_PLUGIN_ID)) {
                    movie.setRuntime(series.getRuntime(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteCompany(movie, THETVDB_PLUGIN_ID)) {
                    movie.setCompany(series.getNetwork(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteGenres(movie, THETVDB_PLUGIN_ID)) {
                    movie.setGenres(series.getGenres(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwritePlot(movie, THETVDB_PLUGIN_ID)) {
                    movie.setPlot(series.getOverview(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteCertification(movie, THETVDB_PLUGIN_ID)) {
                    movie.setCertification(series.getContentRating(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteActors(movie, THETVDB_PLUGIN_ID)) {
                    movie.setCast(series.getActors(), THETVDB_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwritePeopleActors(movie, THETVDB_PLUGIN_ID)) {
                    movie.setPeopleCast(series.getActors(), THETVDB_PLUGIN_ID);
                }

                if (includeWideBanners && isNotValidString(movie.getBannerURL()) || (forceBannerOverwrite) || movie.isDirty(DirtyFlag.BANNER)) {
                    String bannerUrl = getBanner(movie);

                    if (StringTools.isValidString(bannerUrl)) {
                        movie.setBannerURL(bannerUrl);

                        ArtworkFile artworkFile = new ArtworkFile(ArtworkSize.LARGE, movie.getBannerFilename(), false);
                        Artwork artwork = new Artwork(ArtworkType.Banner, THETVDB_PLUGIN_ID, bannerUrl, artworkFile);
                        movie.addArtwork(artwork);
                        logger.debug(LOG_MESSAGE + "Used banner " + bannerUrl);
                    }
                }

                getFanart(movie);

                scanTVShowTitles(movie);
            }
        }

        return true;
    }

    /**
     * Get the Banner URL from TheTVDB
     *
     * @param movie
     * @return
     */
    public String getBanner(Movie movie) {
        Banners banners = getBanners(movie.getId(THETVDB_PLUGIN_ID));

        int season = movie.getSeason();
        String urlBanner = null;

        // If we are adding the "Season ?" text to a banner, try searching for these first
        if (textBanners && !banners.getSeriesList().isEmpty()) {
            // Trying to grab localized banner at first...
            urlBanner = findBannerURL2(banners, BannerType.Blank, language, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringTools.isNotValidString(urlBanner) && StringTools.isValidString(language2nd)) {
                urlBanner = findBannerURL2(banners, BannerType.Blank, language2nd, season);
            }
        }

        // Get the specific season banners. If a season banner can't be found, then a generic series banner will be used
        if (!onlySeriesBanners && !banners.getSeasonList().isEmpty()) {
            // Trying to grab localized banner at first...
            urlBanner = findBannerURL(banners, BannerType.SeasonWide, language, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringUtils.isBlank(urlBanner)) {
                urlBanner = findBannerURL(banners, BannerType.SeasonWide, language2nd, season);
            }
        }

        // If we didn't find a season banner or only want series banners, check for a series banner
        if (StringUtils.isBlank(urlBanner) && !banners.getSeriesList().isEmpty()) {
            urlBanner = findBannerURL2(banners, BannerType.Graphical, language, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringUtils.isBlank(urlBanner)) {
                urlBanner = findBannerURL2(banners, BannerType.Graphical, language2nd, season);
            }
        }

        if (StringTools.isNotValidString(urlBanner)) {
            return Movie.UNKNOWN;
        }
        return urlBanner;
    }

    private void getFanart(Movie movie) {
        if (downloadFanart && isNotValidString(movie.getFanartURL()) || (forceFanartOverwrite) || movie.isDirty(DirtyFlag.FANART)) {

            String url = FanartScanner.getFanartURL(movie);

            if (isValidString(url)) {
                movie.setFanartURL(url);
            }

            if (isValidString(movie.getFanartURL())) {
                String artworkFilename = movie.getBaseName() + fanartToken + "." + fanartExtension;
                movie.setFanartFilename(artworkFilename);
            }
        }
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        String id = movie.getId(THETVDB_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || id == null) {
            return;
        }

        List<Episode> episodeList = null;
        List<Episode> episodeList2ndLanguage = null;

        ThreadExecutor.enterIO(webhost);
        try {
            episodeList = tvDB.getSeasonEpisodes(id, (dvdEpisodes ? -1 : movie.getSeason()), language);

            if (!language.equalsIgnoreCase(language2nd) && StringTools.isValidString(language2nd)) {
                episodeList2ndLanguage = tvDB.getSeasonEpisodes(id, (dvdEpisodes ? -1 : movie.getSeason()), language2nd);
            }
        } catch (Exception error) {
            logger.warn(LOG_MESSAGE + "Error getting episode information: " + error.getMessage());
            return;
        } finally {
            ThreadExecutor.leaveIO();
        }

        boolean setDirectors = OverrideTools.checkOverwriteDirectors(movie, THETVDB_PLUGIN_ID);
        boolean setPeopleDirectors = OverrideTools.checkOverwritePeopleDirectors(movie, THETVDB_PLUGIN_ID);
        boolean setWriters = OverrideTools.checkOverwriteWriters(movie, THETVDB_PLUGIN_ID);
        boolean setPeopleWriters = OverrideTools.checkOverwritePeopleWriters(movie, THETVDB_PLUGIN_ID);

        for (MovieFile file : movie.getMovieFiles()) {
            if (movie.getSeason() >= 0) {
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    Episode episode = null;
                    if (dvdEpisodes) {
                        episode = findDvdEpisode(episodeList, movie.getSeason(), part);
                        if (episode == null && StringTools.isValidString(language2nd)) {
                            episode = findDvdEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                        }
                    }

                    if (episode == null) {
                        //episode = tvDB.getEpisode(id, movie.getSeason(), part, language);
                        episode = findEpisode(episodeList, movie.getSeason(), part);
                        if (episode == null && StringTools.isValidString(language2nd)) {
                            episode = findEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                        }
                    }

                    if (episode != null) {
                        // TODO Assign the writers on a per-episode basis, rather than series
                        if (setWriters && !episode.getWriters().isEmpty()) {
                            movie.setWriters(episode.getWriters(), THETVDB_PLUGIN_ID);
                            setWriters = false;
                        }
                        if (setPeopleWriters && !episode.getWriters().isEmpty()) {
                            movie.setPeopleWriters(episode.getWriters(), THETVDB_PLUGIN_ID);
                            setPeopleWriters = false;
                        }

                        // TODO Assign the director to each episode.
                        if (setDirectors && !episode.getDirectors().isEmpty()) {
                            movie.setDirectors(episode.getDirectors(), THETVDB_PLUGIN_ID);
                            setDirectors = false;
                        }
                        if (setPeopleDirectors && !episode.getDirectors().isEmpty()) {
                            movie.setPeopleDirectors(episode.getDirectors(), THETVDB_PLUGIN_ID);
                            setPeopleDirectors = false;
                        }

                        if (isNotValidString(file.getAirsAfterSeason(part))) {
                            file.setAirsAfterSeason(part, String.valueOf(episode.getAirsAfterSeason()));
                        }

                        if (isNotValidString(file.getAirsBeforeSeason(part))) {
                            file.setAirsBeforeSeason(part, String.valueOf(episode.getAirsBeforeSeason()));
                        }

                        if (isNotValidString(file.getAirsBeforeEpisode(part))) {
                            file.setAirsBeforeEpisode(part, String.valueOf(episode.getAirsBeforeEpisode()));
                        }

                        if (isNotValidString(file.getFirstAired(part))) {
                            file.setFirstAired(part, episode.getFirstAired());
                        }

                        // Set the title of the episode
                        if (isNotValidString(file.getTitle(part))) {
                            file.setTitle(part, episode.getEpisodeName());
                        }

                        // Set the rating of the episode
                        if (includeEpisodeRating) {
                            if (isNotValidString(file.getRating(part)) && isValidString(episode.getRating())) {
                                float episodeRating1 = new Float(episode.getRating());
                                String episodeRating2 = String.valueOf(Math.round(episodeRating1 * 10f));
                                file.setRating(part, episodeRating2);
                            }
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
    }

    /**
     * Locate the specific episode from the list of episodes
     *
     * @param episodeList
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    private Episode findEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
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
     * Locate the specific DVD episode from the list of episodes
     *
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
            int dvdSeason = NumberUtils.toInt(episode.getDvdSeason(), -1);
            int dvdEpisode = (int) NumberUtils.toFloat(episode.getDvdEpisodeNumber(), -1.0f);

            if ((dvdSeason == seasonNumber) && (dvdEpisode == episodeNumber)) {
                return episode;
            }
        }
        return null;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie);

        boolean result = false;
        logger.debug(LOG_MESSAGE + "Scanning NFO for TheTVDB Id");
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
                String id;
                if (endIdx > -1) {
                    id = new String(compareString.substring(beginIdx + length, endIdx));
                } else {
                    id = new String(compareString.substring(beginIdx + length));
                }

                if (StringUtils.isNotBlank(id)) {
                    movie.setId(THETVDB_PLUGIN_ID, id.trim());
                    logger.debug(LOG_MESSAGE + "TheTVDB Id found in nfo = " + id.trim());
                    result = true;
                }
            }
        }
        return result;
    }

    private String findBannerURL(final Banners bannerList, final BannerType bannerType, final String languageId, final int season) {
        if (StringTools.isNotValidString(languageId)) {
            return null;
        }

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

    private String findBannerURL2(final Banners bannerList, final BannerType bannerType, final String languageId, final int season) {
        if (StringTools.isNotValidString(languageId)) {
            return null;
        }

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
     * Get the series. Either from the cache or direct from TheTVDb
     *
     * @param id
     * @return
     */
    public static Series getSeries(String id) {
        Series series = (Series) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, language));

        if (series == null) {
            // Not found in cache, so look online
            ThreadExecutor.enterIO(webhost);
            try {
                series = tvDB.getSeries(id, language);
                if (series != null) {
                    // Add to the cache
                    CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, language), series);
                }

                if (series == null && !language2nd.isEmpty()) {
                    series = (Series) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, language2nd));

                    if (series == null) {
                        series = tvDB.getSeries(id, language2nd);
                        if (series != null) {
                            // Add to the cache
                            CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, language2nd), series);
                        }
                    }
                }
            } catch (Exception error) {
                logger.warn(LOG_MESSAGE + "Error getting Series: " + error.getMessage());
            } finally {
                ThreadExecutor.leaveIO();
            }
        }

        return series;
    }

    /**
     * Use the movie information to find the series and ID
     *
     * @param movie
     * @return
     */
    public static String findId(Movie movie) {
        String id = movie.getId(THETVDB_PLUGIN_ID);

        if (StringTools.isNotValidString(id)) {
            List<Series> seriesList = null;

            ThreadExecutor.enterIO(webhost);
            try {
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
            } catch (Exception error) {
                logger.warn(LOG_MESSAGE + "Error getting ID: " + error.getMessage());
            } finally {
                ThreadExecutor.leaveIO();
            }

            if (seriesList == null || seriesList.isEmpty()) {
                return Movie.UNKNOWN;
            } else {
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

                // If we can't find an exact match, select the first one
                if (series == null) {
                    series = seriesList.get(0);
                    logger.debug(LOG_MESSAGE + "No exact match for " + movie.getTitle() + " found, using " + series.getSeriesName());
                }

                id = String.valueOf(series.getId());

                series = getSeries(id);

                // Add the series to the cache (no need to get it again
                CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, language), series);

                movie.setId(THETVDB_PLUGIN_ID, id);

                if (StringTools.isValidString(series.getImdbId())) {
                    movie.setId(IMDB_PLUGIN_ID, series.getImdbId());
                }
            }
        }

        return id;
    }

    /**
     * Get the banners from the cache or TheTVDb
     *
     * @param id
     * @return
     */
    public static Banners getBanners(String id) {
        Banners banners = (Banners) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_BANNERS, id, language));

        if (banners == null) {
            ThreadExecutor.enterIO(webhost);
            try {
                banners = tvDB.getBanners(id);
                CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_BANNERS, id, language), banners);
            } catch (Exception error) {
                logger.warn(LOG_MESSAGE + "Error getting Banners: " + error.getMessage());
            } finally {
                ThreadExecutor.leaveIO();
            }
        }

        return banners;
    }
}