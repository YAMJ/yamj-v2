/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

package com.moviejukebox.plugin.poster;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class TheTvDBPosterPlugin implements ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String language;
    private TheTVDB tvDB;

    public TheTvDBPosterPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", "en");

    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        try {
            Semaphore sem = WebBrowser.getSemaphore("thetvdb.com");
            sem.acquireUninterruptibly();
            List<Series> seriesList = null;

            if (!title.equals(Movie.UNKNOWN)) {
                seriesList = tvDB.searchSeries(title, language);
            }

            sem.release();
            if (seriesList != null && !seriesList.isEmpty()) {
                Series series = null;
                for (Series s : seriesList) {
                    if (s.getFirstAired() != null && !s.getFirstAired().isEmpty()) {
                        if (year != null && !year.equals(Movie.UNKNOWN)) {
                            try {
                                Date firstAired = dateFormat.parse(s.getFirstAired());
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(firstAired);
                                if (cal.get(Calendar.YEAR) == Integer.parseInt(year)) {
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
                response = series.getId();
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving TvDb Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id, int season) {
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            String urlNormal = null;
            Banners banners = tvDB.getBanners(id);

            if (!banners.getSeasonList().isEmpty()) {
                for (Banner banner : banners.getSeasonList()) {
                    if (banner.getSeason() == season) { // only check for the correct season
                        if (urlNormal == null && banner.getBannerType2().equalsIgnoreCase("season")) {
                            urlNormal = banner.getUrl();
                        }

                        if (urlNormal != null) {
                            break;
                        }
                    }
                }
            }
            if (urlNormal == null && !banners.getPosterList().isEmpty()) {
                urlNormal = banners.getPosterList().get(0).getUrl();
            }
            if (urlNormal == null) {
                Series series = tvDB.getSeries(id, language);
                if (series.getPoster() != null && !series.getPoster().isEmpty()) {
                    urlNormal = series.getPoster();
                }
            }

            if (urlNormal != null) {
                response = urlNormal;
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason), tvSeason);
    }

    @Override
    public String getName() {
        return "thetvdb";
    }

    @Override
    public String getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                id = getIdFromMovieInfo(movieInformation.getTitle(), movieInformation.getYear(), movieInformation.getSeason());
            }
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                ident.setId(getName(), id);
            }
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            }

        }
        return Movie.UNKNOWN;
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
}
