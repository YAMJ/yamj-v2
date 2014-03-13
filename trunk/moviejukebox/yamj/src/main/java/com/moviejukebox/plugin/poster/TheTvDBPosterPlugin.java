/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Series;
import java.util.List;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.pojava.datetime.DateTime;
import org.slf4j.LoggerFactory;

public class TheTvDBPosterPlugin implements ITvShowPosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(TheTvDBPosterPlugin.class);
    private static final String LOG_MESSAGE = "TheTvDBPosterPlugin: ";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
    private static final String DEFAULT_LANGUAGE = "en";
    private String language;
    private String language2nd;
    private TheTVDBApi tvDB;
    private static final String WEB_HOST = "thetvdb.com";

    public TheTvDBPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        tvDB = new TheTVDBApi(API_KEY);

        // We need to set the proxy parameters if set.
        tvDB.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        tvDB.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

        language = PropertiesUtil.getProperty("thetvdb.language", DEFAULT_LANGUAGE);
        language2nd = PropertiesUtil.getProperty("thetvdb.language.secondary", DEFAULT_LANGUAGE);
        // We do not need use the same secondary language... So clearing when equal.
        if (language2nd.equalsIgnoreCase(language)) {
            language2nd = "";
        }
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        List<Series> seriesList = null;

        if (StringTools.isValidString(title)) {
            ThreadExecutor.enterIO(WEB_HOST);
            try {
                seriesList = tvDB.searchSeries(title, language);
                // Try Alternative Language
                if ((seriesList == null || seriesList.isEmpty()) && StringTools.isValidString(language2nd)) {
                    seriesList = tvDB.searchSeries(title, language2nd);
                }
            } finally {
                ThreadExecutor.leaveIO();
            }
        }

        int iYear = NumberUtils.toInt(year, -1);

        if (seriesList != null && !seriesList.isEmpty()) {
            Series series = null;
            for (Series s : seriesList) {
                if (StringUtils.isNotBlank(s.getFirstAired())) {
                    if (iYear > -1) {
                        DateTime firstAired = DateTime.parse(s.getFirstAired());
                        if (NumberUtils.toInt(firstAired.toString("yyyy"), -1) == iYear) {
                            series = s;
                            break;
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

            if (series != null) {
                response = String.valueOf(series.getId());
            }
        }

        return response;
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        String posterURL = Movie.UNKNOWN;

        ThreadExecutor.enterIO(WEB_HOST);

        try {
            if (!(id.equals(Movie.UNKNOWN) || (id.equals("-1"))) || (id.equals("0"))) {
                String urlNormal = null;

                Banners banners = TheTvDBPlugin.getBanners(id);

                if (!banners.getSeasonList().isEmpty()) {
                    // Trying to grab localized banners at first...
                    urlNormal = findPosterURL(banners, season, language);
                    if (StringTools.isNotValidString(urlNormal) && !language2nd.isEmpty()) {
                        // In a case of failure - trying to grab banner in alternative language.
                        urlNormal = findPosterURL(banners, season, language2nd);
                    }
                }

                if (StringTools.isNotValidString(urlNormal) && !banners.getPosterList().isEmpty()) {
                    urlNormal = banners.getPosterList().get(0).getUrl();
                }

                if (urlNormal == null) {
                    Series series = TheTvDBPlugin.getSeries(id);

                    if (series != null && StringTools.isValidString(series.getPoster())) {
                        urlNormal = series.getPoster();
                    }
                }

                if (urlNormal != null) {
                    posterURL = urlNormal;
                }
            }

            if (StringTools.isValidString(posterURL)) {
                LOG.debug(LOG_MESSAGE + "Used poster " + posterURL);
                return new Image(posterURL);
            }
        } finally {
            ThreadExecutor.leaveIO();
        }

        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason), tvSeason);
    }

    @Override
    public final String getName() {
        return "thetvdb";
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);

        if (StringTools.isNotValidString(id)) {
            if (movieInformation.isTVShow()) {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear(), movieInformation.getSeason());
            }
            // Id found
            if (StringTools.isValidString(id)) {
                ident.setId(getName(), id);
            }
        }

        if (StringTools.isValidString(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            }

        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }

    private String findPosterURL(final Banners bannerList, final int season, final String languageId) {
        String backupUrl = null;
        for (Banner banner : bannerList.getSeasonList()) {
            if ((banner.getSeason() == season) && (banner.getBannerType2() == BannerType.SEASON)) {
                if (banner.getLanguage().equalsIgnoreCase(languageId)) {
                    return banner.getUrl();
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    backupUrl = banner.getUrl();
                }
            }
        }

        // Log a message to indicate that this is a non-language banner
        if (backupUrl != null) {
            LOG.info(LOG_MESSAGE + "No poster found for " + languageId + ", using poster with no language: " + backupUrl);
        }

        return backupUrl;
    }

    @Override
    public final boolean isNeeded() {
        String searchPriority = PropertiesUtil.getProperty("poster.scanner.SearchPriority.tv", "");
        return searchPriority.toLowerCase().contains(this.getName());
    }
}
