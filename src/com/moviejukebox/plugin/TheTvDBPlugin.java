package com.moviejukebox.plugin;

import java.util.List;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Episode;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * 
 * @author styles
 */
public class TheTvDBPlugin extends ImdbPlugin {

    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = "2805AD2873519EC5";

    private TheTVDB tvDB;
    private String language;
    private boolean includeEpisodePlots;
    
    public TheTvDBPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", "en");
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
    }

    @Override
    public boolean scan(Movie movie) {
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
                        series = s;
                        break;
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
                    movie.setTitle(series.getSeriesName());
                }
                if (movie.getYear().equals(Movie.UNKNOWN)) {
                    String year = tvDB.getSeasonYear(id, movie.getSeason());
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
                if (movie.getPosterURL().equals(Movie.UNKNOWN)) {
                    String url = null;
                    if (!banners.getSeasonList().isEmpty()) {
                        for (Banner banner : banners.getSeasonList()) {
                            if (banner.getSeason() == movie.getSeason() && banner.getBannerType2().equalsIgnoreCase("season")) {
                                url = banner.getUrl();
                                break;
                            }
                        }
                    }
                    if (url == null && !banners.getPosterList().isEmpty()) {
                        url = banners.getPosterList().get(0).getUrl();
                    }
                    if (url == null && series.getPoster() != null && !series.getPoster().isEmpty()) {
                        url = series.getPoster();
                    }
                    if (url != null) {
                        movie.setPosterURL(url);
                    }
                }
                if (downloadFanart && movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
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
                        movie.setFanartFilename(movie.getBaseName() + ".fanart.jpg");
                    }
                }
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

        for (MovieFile file : movie.getMovieFiles()) {
            if (!file.isNewFile()) {
                // don't scan episode title if it exists in XML data
                continue;
            }

            if (movie.getSeason() > 0) {
                Episode episode = tvDB.getEpisode(id, movie.getSeason(), file.getPart(), language);
                if (episode != null) {
                    file.setTitle(episode.getEpisodeName());
                    if (includeEpisodePlots) {
                        file.setPlot(episode.getOverview());
                    }
                }
            }
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
            if (beginIdx < idx) {
                beginIdx = compareString.indexOf("?ID=");
            }

            if (beginIdx > idx) {
                int endIdx = compareString.indexOf("&", beginIdx + 1);
                String id = null;
                if (endIdx > -1) {
                    id = compareString.substring(beginIdx+4, endIdx);
                } else {
                    id = compareString.substring(beginIdx+4);
                }
                if (id != null && !id.isEmpty()) {
                    movie.setId(THETVDB_PLUGIN_ID, id);
                    logger.finer("TheTVDB Id found in nfo = " + id);
                }
            }
        }
    }

}
