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

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
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

    private static final Logger LOG = Logger.getLogger(TheTvDBPlugin.class);
    private static final String LOG_MESSAGE = "TheTVDBPlugin: ";
    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
    private static final String WEBHOST = "thetvdb.com";
    private static final String LANGUAGE_DEFAULT = "en";
    public static final String CACHE_SERIES = "Series";
    public static final String CACHE_BANNERS = "Banners";
    private static final TheTVDBApi TVDB = new TheTVDBApi(API_KEY);
    private static final String LANGUAGE_PRIMARY = PropertiesUtil.getProperty("thetvdb.language", LANGUAGE_DEFAULT).trim();
    private static final String LANGUAGE_SECONDARY = initLanguage2();
    private final boolean forceBannerOverwrite;
    private final boolean forceFanartOverwrite;
    private final boolean includeVideoImages;
    private final boolean includeWideBanners;
    private final boolean onlySeriesBanners;
    private final boolean cycleSeriesBanners;
    private final boolean textBanners;
    private boolean dvdEpisodes = Boolean.FALSE;

    public TheTvDBPlugin() {
        super();
        includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
        includeWideBanners = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", Boolean.FALSE);
        onlySeriesBanners = PropertiesUtil.getBooleanProperty("mjb.onlySeriesBanners", Boolean.FALSE);
        cycleSeriesBanners = PropertiesUtil.getBooleanProperty("mjb.cycleSeriesBanners", Boolean.TRUE);
        dvdEpisodes = PropertiesUtil.getBooleanProperty("thetvdb.dvd.episodes", Boolean.FALSE);
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.tv.download", Boolean.FALSE);
        forceFanartOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceFanartOverwrite", Boolean.FALSE);
        forceBannerOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceBannersOverwrite", Boolean.FALSE);
        textBanners = PropertiesUtil.getBooleanProperty("banners.addText.season", Boolean.FALSE);

        // We need to set the proxy parameters if set.
        TVDB.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        TVDB.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    private static String initLanguage2() {
        String lang = PropertiesUtil.getProperty("thetvdb.language.secondary", LANGUAGE_DEFAULT).trim();
        // We do not need use the same secondary language... So clearing when equal.
        if (lang.equalsIgnoreCase(LANGUAGE_PRIMARY)) {
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
                LOG.debug(LOG_MESSAGE + "No series information found for " + movie.getTitle());
            } else {
                if (OverrideTools.checkOverwriteTitle(movie, THETVDB_PLUGIN_ID)) {
                    movie.setTitle(series.getSeriesName(), THETVDB_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(movie, THETVDB_PLUGIN_ID)) {
                    movie.setOriginalTitle(series.getSeriesName(), THETVDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteYear(movie, THETVDB_PLUGIN_ID)) {
                    String year = Movie.UNKNOWN;

                    ThreadExecutor.enterIO(WEBHOST);
                    try {
                        year = TVDB.getSeasonYear(id, movie.getSeason(), LANGUAGE_PRIMARY);
                        if (StringTools.isNotValidString(year) && StringTools.isValidString(LANGUAGE_SECONDARY)) {
                            year = TVDB.getSeasonYear(id, movie.getSeason(), LANGUAGE_SECONDARY);
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
                        LOG.trace(LOG_MESSAGE + "Failed to transform rating for series id " + series.getId() + " = '" + series.getRating() + "', Error: " + ex.getMessage());
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
                        LOG.trace(LOG_MESSAGE + "Used banner " + bannerUrl);
                    }
                }

                getFanart(movie);

                scanTVShowTitles(movie);
            }
        }

        return Boolean.TRUE;
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
            urlBanner = findBannerURL2(banners, BannerType.BLANK, LANGUAGE_PRIMARY, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringTools.isNotValidString(urlBanner) && StringTools.isValidString(LANGUAGE_SECONDARY)) {
                urlBanner = findBannerURL2(banners, BannerType.BLANK, LANGUAGE_SECONDARY, season);
            }
        }

        // Get the specific season banners. If a season banner can't be found, then a generic series banner will be used
        if (!onlySeriesBanners && !banners.getSeasonList().isEmpty()) {
            // Trying to grab localized banner at first...
            urlBanner = findBannerURL(banners, BannerType.SEASONWIDE, LANGUAGE_PRIMARY, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringUtils.isBlank(urlBanner)) {
                urlBanner = findBannerURL(banners, BannerType.SEASONWIDE, LANGUAGE_SECONDARY, season);
            }
        }

        // If we didn't find a season banner or only want series banners, check for a series banner
        if (StringUtils.isBlank(urlBanner) && !banners.getSeriesList().isEmpty()) {
            urlBanner = findBannerURL2(banners, BannerType.GRAPHICAL, LANGUAGE_PRIMARY, season);
            // In a case of failure - trying to grab banner in alternative language.
            if (StringUtils.isBlank(urlBanner)) {
                urlBanner = findBannerURL2(banners, BannerType.GRAPHICAL, LANGUAGE_SECONDARY, season);
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

        ThreadExecutor.enterIO(WEBHOST);
        try {
            episodeList = TVDB.getSeasonEpisodes(id, (dvdEpisodes ? -1 : movie.getSeason()), LANGUAGE_PRIMARY);

            if (!LANGUAGE_PRIMARY.equalsIgnoreCase(LANGUAGE_SECONDARY) && StringTools.isValidString(LANGUAGE_SECONDARY)) {
                episodeList2ndLanguage = TVDB.getSeasonEpisodes(id, (dvdEpisodes ? -1 : movie.getSeason()), LANGUAGE_SECONDARY);
            }
        } catch (Exception error) {
            LOG.warn(LOG_MESSAGE + "Error getting episode information: " + error.getMessage());
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
                        if (episode == null && StringTools.isValidString(LANGUAGE_SECONDARY)) {
                            episode = findDvdEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                        }
                    }

                    if (episode == null) {
                        //episode = tvDB.getEpisode(id, movie.getSeason(), part, language);
                        episode = findEpisode(episodeList, movie.getSeason(), part);
                        if (episode == null && StringTools.isValidString(LANGUAGE_SECONDARY)) {
                            episode = findEpisode(episodeList2ndLanguage, movie.getSeason(), part);
                        }
                    }

                    if (episode == null) {
                        if (movie.getSeason() > 0 && file.getFirstPart() == 0 && isNotValidString(file.getPlot(part))) {
                            file.setTitle(part, "Special", THETVDB_PLUGIN_ID);
                        }
                    } else {
                        // TODO Assign the director to each episode.
                        if (setDirectors && !episode.getDirectors().isEmpty()) {
                            movie.setDirectors(episode.getDirectors(), THETVDB_PLUGIN_ID);
                            setDirectors = Boolean.FALSE;
                        }
                        if (setPeopleDirectors && !episode.getDirectors().isEmpty()) {
                            movie.setPeopleDirectors(episode.getDirectors(), THETVDB_PLUGIN_ID);
                            setPeopleDirectors = Boolean.FALSE;
                        }

                        // TODO Assign the writers on a per-episode basis, rather than series
                        if (setWriters && !episode.getWriters().isEmpty()) {
                            movie.setWriters(episode.getWriters(), THETVDB_PLUGIN_ID);
                            setWriters = Boolean.FALSE;
                        }
                        if (setPeopleWriters && !episode.getWriters().isEmpty()) {
                            movie.setPeopleWriters(episode.getWriters(), THETVDB_PLUGIN_ID);
                            setPeopleWriters = Boolean.FALSE;
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

                        if (OverrideTools.checkOverwriteEpisodeFirstAired(file, part, THETVDB_PLUGIN_ID)) {
                            file.setFirstAired(part, episode.getFirstAired(), THETVDB_PLUGIN_ID);
                        }

                        if (OverrideTools.checkOverwriteEpisodeTitle(file, part, THETVDB_PLUGIN_ID)) {
                            file.setTitle(part, episode.getEpisodeName(), THETVDB_PLUGIN_ID);
                        }

                        if (isValidString(episode.getRating()) && OverrideTools.checkOverwriteEpisodeRating(file, part, THETVDB_PLUGIN_ID)) {
                            int episodeRating = StringTools.parseRating(episode.getRating());
                            file.setRating(part, String.valueOf(episodeRating), THETVDB_PLUGIN_ID);
                        }

                        if (OverrideTools.checkOverwriteEpisodePlot(file, part, THETVDB_PLUGIN_ID)) {
                            file.setPlot(part, episode.getOverview(), THETVDB_PLUGIN_ID);
                        }

                        if (includeVideoImages) {
                            file.setVideoImageURL(part, episode.getFilename());
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
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

        boolean result = Boolean.FALSE;
        LOG.debug(LOG_MESSAGE + "Scanning NFO for TheTVDB Id");
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
                    id = compareString.substring(beginIdx + length, endIdx);
                } else {
                    id = compareString.substring(beginIdx + length);
                }

                if (StringUtils.isNotBlank(id)) {
                    movie.setId(THETVDB_PLUGIN_ID, id.trim());
                    LOG.debug(LOG_MESSAGE + "TheTVDB Id found in nfo = " + id.trim());
                    result = Boolean.TRUE;
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
        Series series = (Series) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, LANGUAGE_PRIMARY));

        if (series == null) {
            // Not found in cache, so look online
            ThreadExecutor.enterIO(WEBHOST);
            try {
                series = TVDB.getSeries(id, LANGUAGE_PRIMARY);
                if (series != null) {
                    // Add to the cache
                    CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, LANGUAGE_PRIMARY), series);
                }

                if (series == null && !LANGUAGE_SECONDARY.isEmpty()) {
                    series = (Series) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, LANGUAGE_SECONDARY));

                    if (series == null) {
                        series = TVDB.getSeries(id, LANGUAGE_SECONDARY);
                        if (series != null) {
                            // Add to the cache
                            CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, LANGUAGE_SECONDARY), series);
                        }
                    }
                }
            } catch (Exception error) {
                LOG.warn(LOG_MESSAGE + "Error getting Series: " + error.getMessage());
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

            ThreadExecutor.enterIO(WEBHOST);
            try {
                if (!movie.getTitle().equals(Movie.UNKNOWN)) {
                    seriesList = TVDB.searchSeries(movie.getTitle(), LANGUAGE_PRIMARY);
                    if ((seriesList == null || seriesList.isEmpty()) && !LANGUAGE_SECONDARY.isEmpty()) {
                        seriesList = TVDB.searchSeries(movie.getTitle(), LANGUAGE_SECONDARY);
                    }
                }

                if (seriesList == null || seriesList.isEmpty()) {
                    seriesList = TVDB.searchSeries(movie.getBaseName(), LANGUAGE_PRIMARY);
                    if ((seriesList == null || seriesList.isEmpty()) && !LANGUAGE_SECONDARY.isEmpty()) {
                        seriesList = TVDB.searchSeries(movie.getBaseName(), LANGUAGE_SECONDARY);
                    }
                }
            } catch (Exception error) {
                LOG.warn(LOG_MESSAGE + "Error getting ID: " + error.getMessage());
            } finally {
                ThreadExecutor.leaveIO();
            }

            if (seriesList == null || seriesList.isEmpty()) {
                return Movie.UNKNOWN;
            } else {
                Series series = null;
                for (Series s : seriesList) {
                    if (StringTools.isValidString(s.getFirstAired())) {
                        if (StringTools.isValidString(movie.getYear())) {
                            try {
                                DateTime firstAired = DateTime.parse(s.getFirstAired());
                                if (Integer.parseInt(firstAired.toString("yyyy")) == Integer.parseInt(movie.getYear())) {
                                    series = s;
                                    break;
                                }
                            } catch (NumberFormatException ex) {
                                LOG.trace(LOG_MESSAGE + "Failed to convert year: '" + s.getFirstAired() + "', error: " + ex.getMessage());
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
                    LOG.debug(LOG_MESSAGE + "No exact match for " + movie.getTitle() + " found, using " + series.getSeriesName());
                }

                id = String.valueOf(series.getId());

                series = getSeries(id);

                // Add the series to the cache (no need to get it again
                CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_SERIES, id, LANGUAGE_PRIMARY), series);

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
        Banners banners = (Banners) CacheMemory.getFromCache(CacheMemory.generateCacheKey(CACHE_BANNERS, id, LANGUAGE_PRIMARY));

        if (banners == null) {
            ThreadExecutor.enterIO(WEBHOST);
            try {
                banners = TVDB.getBanners(id);
                CacheMemory.addToCache(CacheMemory.generateCacheKey(CACHE_BANNERS, id, LANGUAGE_PRIMARY), banners);
            } catch (Exception error) {
                LOG.warn(LOG_MESSAGE + "Error getting Banners: " + error.getMessage());
            } finally {
                ThreadExecutor.leaveIO();
            }
        }

        return banners;
    }
}
