/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class FilmwebPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmwebPlugin.class);
    private static final String LOG_MESSAGE = "FilmwebPlugin: ";
    public static final String FILMWEB_PLUGIN_ID = "filmweb";
//    private static Pattern googlePattern = Pattern.compile(">(http://[^\"/?&]*filmweb.pl[^<\\s]*)");
    private static Pattern googlePattern = Pattern.compile("(http://[^\"/?&]*filmweb.pl[^\"&<\\s]*)");
    private static Pattern yahooPattern = Pattern.compile("http%3a(//[^\"/?&]*filmweb.pl[^\"]*)\"");
    private static Pattern filmwebPattern = Pattern.compile("searchResultTitle\"? href=\"([^\"]*)\"");
    private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*filmweb.pl[^\\s<>`\"\\[\\]]*");
    private String filmwebPreferredSearchEngine;
    private int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    private int preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");

    public FilmwebPlugin() {
        super(); // use IMDB if filmweb doesn't know movie
        init();
    }

    @Override
    public String getPluginID() {
        return FILMWEB_PLUGIN_ID;
    }

    public String getFilmwebPreferredSearchEngine() {
        return filmwebPreferredSearchEngine;
    }

    public void setFilmwebPreferredSearchEngine(String filmwebPreferredSearchEngine) {
        this.filmwebPreferredSearchEngine = filmwebPreferredSearchEngine;
    }

    public int getPreferredOutlineLength() {
        return preferredOutlineLength;
    }

    public void setPreferredOutlineLength(int preferredOutlineLength) {
        this.preferredOutlineLength = preferredOutlineLength;
    }

    public int getPreferredPlotLength() {
        return preferredPlotLength;
    }

    public void setPreferredPlotLength(int preferredPlotLength) {
        this.preferredPlotLength = preferredPlotLength;
    }

    public void init() {
        filmwebPreferredSearchEngine = PropertiesUtil.getProperty("filmweb.id.search", "filmweb");
        try {
            // first request to filmweb site to skip welcome screen with ad banner
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
        }
    }

    @Override
    public boolean scan(Movie mediaFile) {
        String filmwebUrl = mediaFile.getId(FILMWEB_PLUGIN_ID);
        if (StringTools.isNotValidString(filmwebUrl)) {
            filmwebUrl = getFilmwebUrl(mediaFile.getTitle(), mediaFile.getYear());
            mediaFile.setId(FILMWEB_PLUGIN_ID, filmwebUrl);
        }

        boolean retval;
        if (StringTools.isValidString(filmwebUrl)) {
            retval = updateMediaInfo(mediaFile);
        } else {
            // use IMDB if filmweb doesn't know movie
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year.
     */
    public String getFilmwebUrl(String movieName, String year) {
        if ("google".equalsIgnoreCase(filmwebPreferredSearchEngine)) {
            return getFilmwebUrlFromGoogle(movieName, year);
        } else if ("yahoo".equalsIgnoreCase(filmwebPreferredSearchEngine)) {
            return getFilmwebUrlFromYahoo(movieName, year);
        } else if ("none".equalsIgnoreCase(filmwebPreferredSearchEngine)) {
            return Movie.UNKNOWN;
        } else {
            return getFilmwebUrlFromFilmweb(movieName, year);
        }
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year. This routine is base on a yahoo request.
     */
    private String getFilmwebUrlFromYahoo(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://search.yahoo.com/search?p=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Afilmweb.pl&ei=UTF-8");

            String xml = webBrowser.request(sb.toString());
            Matcher m = yahooPattern.matcher(xml);
            if (m.find()) {
                String id = "http:" + m.group(1);
                if (id.endsWith("/cast")) {
                    return id.substring(0, id.length() - 5);
                }
                return id;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving filmweb url for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year. This routine is base on a google request.
     */
    private String getFilmwebUrlFromGoogle(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.pl/search?hl=pl&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Afilmweb.pl");

            String xml = webBrowser.request(sb.toString());
            Matcher m = googlePattern.matcher(xml);
            if (m.find()) {
                return m.group(1);
            } else {
                return Movie.UNKNOWN;
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving filmweb url for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year. This routine is base on a filmweb request.
     */
    private String getFilmwebUrlFromFilmweb(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.filmweb.pl/search?q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("&startYear=").append(year).append("&endYear=").append(year);
            }
            String xml = webBrowser.request(sb.toString());
            Matcher m = filmwebPattern.matcher(xml);
            if (m.find()) {
                return "http://www.filmweb.pl" + m.group(1).trim();
            } else {
                return Movie.UNKNOWN;
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving filmweb url for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan web page for the specified movie
     */
    protected boolean updateMediaInfo(Movie movie) {
        try {
            String xml = webBrowser.request(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

            if (HTMLTools.extractTag(xml, "<title>").contains("Serial") && !movie.isTVShow()) {
                movie.setMovieType(Movie.TYPE_TVSHOW);
                return Boolean.FALSE;
            }

            if (OverrideTools.checkOverwriteTitle(movie, FILMWEB_PLUGIN_ID)) {
                movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()></"), FILMWEB_PLUGIN_ID);
            }
                
            if (OverrideTools.checkOverwriteOriginalTitle(movie, FILMWEB_PLUGIN_ID)) {
                String metaTitle = HTMLTools.extractTag(xml, "og:title", "\">");
                if (metaTitle.contains("/")) {
                    String originalTitle = HTMLTools.extractTag(metaTitle, "/", 0, "()><");
                    if (originalTitle.endsWith(", The")) {
                        originalTitle = "The " + originalTitle.substring(0, originalTitle.length() - 5);
                    }
                    movie.setOriginalTitle(originalTitle, FILMWEB_PLUGIN_ID);
                }
            }

            if (movie.getRating() == -1) {
                movie.addRating(FILMWEB_PLUGIN_ID, parseRating(HTMLTools.getTextAfterElem(xml, "average")));
            }

            if (movie.getTop250() == -1) {
                try {
                    movie.setTop250(Integer.parseInt(HTMLTools.extractTag(xml, "worldRanking", 0, ">.")));
                } catch (NumberFormatException error) {
                    movie.setTop250(-1);
                }
            }

            if (OverrideTools.checkOverwriteDirectors(movie, FILMWEB_PLUGIN_ID)) {
                String director = HTMLTools.getTextAfterElem(xml, "yseria:");
                movie.setDirector(director, FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, FILMWEB_PLUGIN_ID)) {
                movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "filmPremiereWorld"), FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FILMWEB_PLUGIN_ID)) {
                String runtime = HTMLTools.getTextAfterElem(xml, "czas trwania:");
                movie.setRuntime(String.valueOf(DateTimeTools.processRuntime(runtime)), FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, FILMWEB_PLUGIN_ID)) {
                String country = StringUtils.join(HTMLTools.extractTags(xml, "produkcja:", "</tr", "<a ", "</a>"), ", ");
                if (country.endsWith(", ")) {
                    movie.setCountry(country.substring(0, country.length() - 2), FILMWEB_PLUGIN_ID);
                } else {
                    movie.setCountry(country, FILMWEB_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteGenres(movie, FILMWEB_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<String>();
                for (String genre : HTMLTools.extractTags(xml, "gatunek:", "premiera:", "<a ", "</a>")) {
                    newGenres.add(Library.getIndexingGenre(genre));
                }
                movie.setGenres(newGenres, FILMWEB_PLUGIN_ID);
            }

            String plot = HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "v:summary\">", "</span>"));
            if (StringTools.isValidString(plot)) {
                if (OverrideTools.checkOverwritePlot(movie, FILMWEB_PLUGIN_ID)) {
                    movie.setPlot(StringTools.trimToLength(plot, preferredPlotLength), FILMWEB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteOutline(movie, FILMWEB_PLUGIN_ID)) {
                    movie.setOutline(StringTools.trimToLength(plot, preferredOutlineLength), FILMWEB_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteYear(movie, FILMWEB_PLUGIN_ID)) {
                String year = HTMLTools.getTextAfterElem(xml, "filmYear");
                if (!Movie.UNKNOWN.equals(year)) {
                    year = year.replaceAll("[^0-9]", "");
                }
                movie.setYear(year, FILMWEB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteActors(movie, FILMWEB_PLUGIN_ID)) {
                List<String> cast = HTMLTools.extractTags(xml, "castListWrapper", "</ul>", "v:starring", "</a>");
                for (int i = 0; i < cast.size(); i++) {
                    cast.set(i, HTMLTools.removeHtmlTags(cast.get(i)).trim());
                }
                movie.setCast(cast, FILMWEB_PLUGIN_ID);
            }

            if (movie.isTVShow()) {
                updateTVShowInfo(movie, xml);
            }

            if (downloadFanart && StringTools.isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (StringTools.isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving filmweb informations for movie : " + movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
        }
        return Boolean.TRUE;
    }

    private int parseRating(String rating) {
        try {
            return Math.round(Float.parseFloat(rating.replace(",", ".")) * 10);
        } catch (Exception error) {
            return -1;
        }
    }

    private String updateImdbId(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (StringTools.isNotValidString(imdbId)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        scanTVShowTitles(movie, null);
    }

    public void scanTVShowTitles(Movie movie, String mainXML) {
        if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
            return;
        }
        String filmwebUrl = movie.getId(FILMWEB_PLUGIN_ID);

        if (StringTools.isNotValidString(filmwebUrl)) {
            // use IMDB if filmweb doesn't know episodes titles
            super.scanTVShowTitles(movie);
            return;
        }

        try {
            String xml = webBrowser.request(filmwebUrl + "/episodes");
            boolean found = Boolean.FALSE;
            boolean wasSeraching = Boolean.FALSE;
            for (MovieFile file : movie.getMovieFiles()) {
                if (!file.isNewFile() || file.hasTitle()) {
                    // don't scan episode title if it exists in XML data
                    continue;
                }
                int fromIndex = xml.indexOf("sezon " + movie.getSeason() + "<");
                StringBuilder titleBuilder = new StringBuilder();
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    wasSeraching = Boolean.TRUE;
                    String episodeName = HTMLTools.getTextAfterElem(xml, "odcinek&nbsp;" + part, 2, fromIndex);
                    if (!episodeName.equals(Movie.UNKNOWN)) {
                        if (titleBuilder.length() > 0) {
                            titleBuilder.append(Movie.SPACE_SLASH_SPACE);
                        }
                        titleBuilder.append(episodeName);
                    }
                }
                String title = titleBuilder.toString();
                if (StringUtils.isNotBlank(title)) {
                    found = Boolean.TRUE;
                    file.setTitle(title);
                }
            }
            if (wasSeraching && !found) {
                // use IMDB if filmweb doesn't know episodes titles
                updateImdbId(movie);
                super.scanTVShowTitles(movie);
            }
        } catch (IOException error) {
            logger.error(LOG_MESSAGE + "Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
        }
    }

    protected void updateTVShowInfo(Movie movie, String mainXML) throws IOException {
        scanTVShowTitles(movie, mainXML);
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB if filmweb doesn't know movie
        logger.debug(LOG_MESSAGE + "Scanning NFO for filmweb url");
        Matcher m = nfoPattern.matcher(nfo);
        boolean found = Boolean.FALSE;
        while (m.find()) {
            String url = m.group();
            if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
                found = Boolean.TRUE;
                movie.setId(FILMWEB_PLUGIN_ID, url);
            }
        }
        if (found) {
            logger.debug(LOG_MESSAGE + "Filmweb url found in NFO = " + movie.getId(FILMWEB_PLUGIN_ID));
        } else {
            logger.debug(LOG_MESSAGE + "No filmweb url found in NFO !");
        }
        return found;
    }
}
