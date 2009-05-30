package com.moviejukebox.plugin;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
 * @author styles
 */
public class TheTvDBPlugin extends ImdbPlugin {

    public static final String THETVDB_PLUGIN_ID = "thetvdb";
    private static final String API_KEY = "2805AD2873519EC5";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private TheTVDB tvDB;
    private String language;
    private boolean includeEpisodePlots;
    private boolean includeVideoImages;
    private boolean dvdEpisodes = false;
    protected static String fanartToken;

    public TheTvDBPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", "en");
        includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
        includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));
        dvdEpisodes = Boolean.parseBoolean(PropertiesUtil.getProperty("thetvdb.dvd.episodes", "false"));
        fanartToken = PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart");
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
                        if (movie.getYear() != null && !movie.getYear().equals(Movie.UNKNOWN)) {
                            try {
                                Date firstAired = dateFormat.parse(s.getFirstAired());
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(firstAired);
                                if (cal.get(Calendar.YEAR) == Integer.parseInt(movie.getYear())) {
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
                    movie.setTitle(series.getSeriesName());
                    movie.setOriginalTitle(series.getSeriesName());
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
                        movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
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
            if (movie.getSeason() >= 0) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    Episode episode = null;
                    if (dvdEpisodes) {
                        episode = tvDB.getDVDEpisode(id, movie.getSeason(), part, language);
                    }
                    if (episode == null) {
                        episode = tvDB.getEpisode(id, movie.getSeason(), part, language);
                    }

                    if (episode != null) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" / ");
                        }

                        // We only get the writers for the first episode, otherwise we might overwhelm the skins with data
                        // TODO Assign the writers on a per-episode basis, rather than series.
                        if ((movie.getWriters().equals(Movie.UNKNOWN)) || ( movie.getWriters().isEmpty())){
                            movie.setWriters(episode.getWriters());
                        }
                        
                        // TODO Assign the director to each episode.
                        if ((movie.getDirector().equals(Movie.UNKNOWN)) || (movie.getDirector().isEmpty())) {
                            System.out.println("Director: " + episode.getDirectors());
                            // Director is a single entry, not a list, so only get the first director
                            movie.setDirector(episode.getDirectors().get(0));
                        }
                        
                        sb.append(episode.getEpisodeName());

                        if (includeEpisodePlots) {
                            file.setPlot(part, episode.getOverview());
                        }

                        if (includeVideoImages) {
                            file.setVideoImageURL(part, episode.getFilename());
                        } else {
                            file.setVideoImageURL(part, Movie.UNKNOWN);
                        }
                    }
                }
                String title = sb.toString();
                if (!"".equals(sb) && !file.hasTitle()) {
                    file.setTitle(title);
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
                    movie.setId(THETVDB_PLUGIN_ID, id);
                    logger.finer("TheTVDB Id found in nfo = " + id);
                }
            }
        }
    }
}
