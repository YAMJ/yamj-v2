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
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class FilmwebPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmwebPlugin.class);
    public static String FILMWEB_PLUGIN_ID = "filmweb";
//    private static Pattern googlePattern = Pattern.compile(">(http://[^\"/?&]*filmweb.pl[^<\\s]*)");
    private static Pattern googlePattern = Pattern.compile("(http://[^\"/?&]*filmweb.pl[^\"&<\\s]*)");
    private static Pattern yahooPattern = Pattern.compile("http%3a(//[^\"/?&]*filmweb.pl[^\"]*)\"");
    private static Pattern filmwebPattern = Pattern.compile("searchResultTitle\"? href=\"([^\"]*)\"");
    private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*filmweb.pl[^\\s<>`\"\\[\\]]*");
    protected String filmwebPreferredSearchEngine;
    protected int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    protected int preferredOutlineLength = PropertiesUtil.getIntProperty("plugin.outline.maxlength", "300");

    public FilmwebPlugin() {
        super(); // use IMDB if filmweb doesn't know movie
        init();
    }

    @Override
    public String getPluginID() {
        return FILMWEB_PLUGIN_ID;
    }

    protected void init() {
        filmwebPreferredSearchEngine = PropertiesUtil.getProperty("filmweb.id.search", "filmweb");
        try {
            // first request to filmweb site to skip welcome screen with ad banner
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            logger.error("Error : " + error.getMessage());
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
     * retrieve the filmweb url matching the specified movie name and year. This
     * routine is base on a yahoo request.
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
            logger.error("Failed retreiving filmweb url for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year. This
     * routine is base on a google request.
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
            logger.error("Failed retreiving filmweb url for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * retrieve the filmweb url matching the specified movie name and year. This
     * routine is base on a filmweb request.
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
            logger.error("Failed retreiving filmweb url for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan web page for the specified movie
     */
    protected boolean updateMediaInfo(Movie movie) {
        try {
            String xml = webBrowser.request(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

            if (HTMLTools.extractTag(xml, "<title>").contains("Serial")) {
                if (!movie.isTVShow()) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (!movie.isOverrideTitle()) {
                movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()></"));
                String metaTitle = HTMLTools.extractTag(xml, "og:title", "\">");
                if (metaTitle.contains("/")) {
                    String originalTitle = HTMLTools.extractTag(metaTitle, "/", 0, "()><");
                    if (originalTitle.endsWith(", The")) {
                        originalTitle = "The " + originalTitle.substring(0, originalTitle.length() - 5);
                    }
                    movie.setOriginalTitle(originalTitle);
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

            if (Movie.UNKNOWN.equals(movie.getDirector())) {
                movie.addDirector(HTMLTools.getTextAfterElem(xml, "yseria:"));
            }

            if (Movie.UNKNOWN.equals(movie.getReleaseDate())) {
                movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "filmPremiereWorld"));
            }

            if (Movie.UNKNOWN.equals(movie.getRuntime())) {
                String runtime = HTMLTools.getTextAfterElem(xml, "czas trwania:");
                movie.setRuntime(String.valueOf(StringTools.processRuntime(runtime)));
            }

            if (Movie.UNKNOWN.equals(movie.getCountry())) {
                String country = StringUtils.join(HTMLTools.extractTags(xml, "produkcja:", "</tr", "<a ", "</a>"), ", ");
                if (country.endsWith(", ")) {
                    movie.setCountry(country.substring(0, country.length() - 2));
                } else {
                    movie.setCountry(country);
                }
            }

            if (movie.getGenres().isEmpty()) {
                for (String genre : HTMLTools.extractTags(xml, "gatunek:", "premiera:", "<a ", "</a>")) {
                    if (!genre.isEmpty()) {
                        movie.addGenre(Library.getIndexingGenre(genre));
                    }
                }
            }

            String plot = HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, "v:summary\">", "</span>"));

            if (StringTools.isValidString(plot)) {
                if (Movie.UNKNOWN.equals(movie.getPlot())) {
                    movie.setPlot(StringTools.trimToLength(plot, preferredPlotLength));
                }

                if (Movie.UNKNOWN.equals(movie.getOutline())) {
                    movie.setOutline(StringTools.trimToLength(plot, preferredOutlineLength));
                }
            }

            if (!movie.isOverrideYear()) {
                String year = HTMLTools.getTextAfterElem(xml, "filmYear");
                if (!Movie.UNKNOWN.equals(year)) {
                    year = year.replaceAll("[^0-9]", "");
                }
                movie.setYear(year);
            }

            if (movie.getCast().isEmpty()) {
                List<String> cast = HTMLTools.extractTags(xml, "castListWrapper", "</ul>", "v:starring", "</a>");
                for (int i = 0; i < cast.size(); i++) {
                    cast.set(i, HTMLTools.removeHtmlTags(cast.get(i)).trim());
                }
                movie.setCast(cast);
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
            logger.error("Failed retreiving filmweb informations for movie : " + movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
        }
        return true;
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
            boolean found = false;
            boolean wasSeraching = false;
            for (MovieFile file : movie.getMovieFiles()) {
                if (!file.isNewFile() || file.hasTitle()) {
                    // don't scan episode title if it exists in XML data
                    continue;
                }
                int fromIndex = xml.indexOf("sezon " + movie.getSeason() + "<");
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {
                    wasSeraching = true;
                    String episodeName = HTMLTools.getTextAfterElem(xml, "odcinek&nbsp;" + part, 2, fromIndex);
                    if (!episodeName.equals(Movie.UNKNOWN)) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(Movie.SPACE_SLASH_SPACE);
                        }
                        sb.append(episodeName);
                    }
                }
                String title = sb.toString();
                if (!"".equals(title)) {
                    found = true;
                    file.setTitle(title);
                }
            }
            if (wasSeraching && !found) {
                // use IMDB if filmweb doesn't know episodes titles
                updateImdbId(movie);
                super.scanTVShowTitles(movie);
            }
        } catch (IOException error) {
            logger.error("Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.error("Error : " + error.getMessage());
        }
    }

    protected void updateTVShowInfo(Movie movie, String mainXML) throws MalformedURLException, IOException {
        scanTVShowTitles(movie, mainXML);
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB if filmweb doesn't know movie
        logger.debug("Scanning NFO for filmweb url");
        Matcher m = nfoPattern.matcher(nfo);
        boolean found = false;
        while (m.find()) {
            String url = m.group();
            if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
                found = true;
                movie.setId(FILMWEB_PLUGIN_ID, url);
            }
        }
        if (found) {
            logger.debug("Filmweb url found in nfo = " + movie.getId(FILMWEB_PLUGIN_ID));
        } else {
            logger.debug("No filmweb url found in nfo !");
        }
        return found;
    }
}
