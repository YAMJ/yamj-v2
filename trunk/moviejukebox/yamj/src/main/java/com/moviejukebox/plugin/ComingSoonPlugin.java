/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.SearchEngineTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author iuk
 *
 */
public class ComingSoonPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ComingSoonPlugin.class);
    public static final String COMINGSOON_PLUGIN_ID = "comingsoon";
    public static final String COMINGSOON_NOT_PRESENT = "na";
    public static final String COMINGSOON_BASE_URL = "http://www.comingsoon.it/";
    public static final String COMINGSOON_SEARCH_URL = "cinema/cercaFilm/?";
    private static final String COMINGSOON_FILM_URL = "Film/Scheda/?";
    public static final String COMINGSOON_SEARCH_PARAMS = "&genere=&nat=&regia=&attore=&orderby=&orderdir=asc&page=";
    public static final String CS_TITLE_PARAM = "titolo=";
    public static final String CS_YEAR_PARAM = "anno=";
    public static final String COMINGSOON_KEY_PARAM = "key=";
    private static final int COMINGSOON_MAX_DIFF = 1000;
    private static final int COMINGSOON_MAX_SEARCH_PAGES = 5;
    private final String scanImdb;
    private final String searchId;
    private final SearchEngineTools searchEngineTools;

    public ComingSoonPlugin() {
        super();
        searchEngineTools = new SearchEngineTools("it");
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
        searchId = PropertiesUtil.getProperty("comingsoon.id.search", "comingsoon,yahoo");
        scanImdb = PropertiesUtil.getProperty("comingsoon.imdb.scan", "always");

        if (!PropertiesUtil.getProperty("comingsoon.trailer.resolution", "DEPRECATED").equals("DEPRECATED")
                || !PropertiesUtil.getProperty("comingsoon.trailer.preferredFormat", "DEPRECATED").equals("DEPRECATED")
                || !PropertiesUtil.getProperty("comingsoon.trailer.setExchange", "DEPRECATED").equals("DEPRECATED")
                || !PropertiesUtil.getProperty("comingsoon.trailer.download", "DEPRECATED").equals("DEPRECATED")
                || !PropertiesUtil.getProperty("comingsoon.trailer.label", "DEPRECATED").equals("DEPRECATED")) {
            LOG.error("comingsoon.trailer.* properties are not supported anymore.");
            LOG.error("Please add comingsoon to trailers.scanner property and configure the plugin using comingsoontrailers.* properties.");
        }
    }

    @Override
    public String getPluginID() {
        return COMINGSOON_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {

        String comingSoonId = movie.getId(COMINGSOON_PLUGIN_ID);

        // First we try on comingsoon.it
        if (StringTools.isNotValidString(comingSoonId)) {
            comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
            movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);

            if (StringTools.isNotValidString(comingSoonId)) {
                LOG.debug("Unable to find ID on first scan");
            }
        }

        boolean firstScanImdb = false;

        if (scanImdb.equalsIgnoreCase("always") || (scanImdb.equalsIgnoreCase("fallback") && (StringTools.isNotValidString(comingSoonId) || comingSoonId.equals(COMINGSOON_NOT_PRESENT)))) {
            LOG.debug("Checking IMDB");
            firstScanImdb = super.scan(movie);
            if (StringTools.isNotValidString(comingSoonId) && firstScanImdb) {
                // We try to fetch again ComingSoon, hopefully with more info
                LOG.debug("First search on ComingSoon was KO, retrying after succesful query to IMDB");
                comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
                movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
            }

            if (StringTools.isNotValidString(comingSoonId)) {
                LOG.debug("unable to find id on second scan");
            }
        }

        boolean scanComingSoon = false;
        if (StringTools.isValidString(comingSoonId)) {

            LOG.debug("Fetching movie data from ComingSoon");
            scanComingSoon = updateComingSoonMediaInfo(movie);
        }

        if (!firstScanImdb && scanComingSoon && "always".equalsIgnoreCase(scanImdb)) {
            // Scan was successful on ComingSoon but not on IMDB, let's try again with more info

            LOG.debug("First scan on IMDB KO, retrying after succesful scan on ComingSoon");

            // Set title to original title, more likely to be found on IMDB
            String title = null;
            String titleSource = null;
            if (StringTools.isValidString(movie.getOriginalTitle())) {
                title = movie.getTitle();
                titleSource = movie.getOverrideSource(OverrideFlag.TITLE);
                movie.setTitle(movie.getOriginalTitle(), movie.getOverrideSource(OverrideFlag.ORIGINALTITLE));
            }

            super.scan(movie);

            // replace possible overwritten title with stored title
            if (StringTools.isValidString(title) && OverrideTools.checkOverwriteTitle(movie, titleSource)) {
                movie.setTitle(title, titleSource);
            }
        }

        if (StringTools.isNotValidString(movie.getOriginalTitle())) {
            movie.setOriginalTitle(movie.getTitle(), movie.getOverrideSource(OverrideFlag.TITLE));
        }

        return scanComingSoon || firstScanImdb;
    }

    public String getComingSoonId(String movieName, String year) {
        return getComingSoonId(movieName, year, searchId);
    }

    protected String getComingSoonId(String movieName, String year, String searchIdPreference) {

        if (searchIdPreference.equalsIgnoreCase("comingsoon")) {
            return getComingSoonIdFromComingSoon(movieName, year);
        } else if (searchIdPreference.equalsIgnoreCase("yahoo")) {
            String url = searchEngineTools.searchUrlOnYahoo(movieName, year, "www.comingsoon.it/film", null);
            return getComingSoonIdFromURL(url);
        } else if (searchIdPreference.equalsIgnoreCase("google")) {
            String url = searchEngineTools.searchUrlOnGoogle(movieName, year, "www.comingsoon.it/film", null);
            return getComingSoonIdFromURL(url);
        } else if (searchIdPreference.contains(",")) {
            StringTokenizer st = new StringTokenizer(searchIdPreference, ",");
            while (st.hasMoreTokens()) {
                String id = getComingSoonId(movieName, year, st.nextToken().trim());
                if (StringTools.isValidString(id)) {
                    return id;
                }
            }
        }
        return Movie.UNKNOWN;
    }

    protected String getComingSoonIdFromComingSoon(String movieName, String year) {
        return getComingSoonIdFromComingSoon(movieName, year, COMINGSOON_MAX_DIFF);
    }

    protected String getComingSoonIdFromComingSoon(String movieName, String year, int scoreToBeat) {
        try {
            if (scoreToBeat == 0) {
                return Movie.UNKNOWN;
            }

            int currentScore = scoreToBeat;

            String comingSoonId = Movie.UNKNOWN;

            StringBuilder sb = new StringBuilder(COMINGSOON_BASE_URL);
            sb.append(COMINGSOON_SEARCH_URL);
            sb.append(CS_TITLE_PARAM);
            sb.append(URLEncoder.encode(movieName.toLowerCase(), "UTF-8"));

            sb.append("&").append(CS_YEAR_PARAM);
            if (StringTools.isValidString(year)) {
                sb.append(year);
            }
            sb.append(COMINGSOON_SEARCH_PARAMS);

            int searchPage = 0;

            while (searchPage++ < COMINGSOON_MAX_SEARCH_PAGES) {

                StringBuilder sbPage = new StringBuilder(sb);
                if (searchPage > 1) {
                    sbPage.append("&p=").append(searchPage);
                }

                LOG.debug("Fetching ComingSoon search URL: {}", sbPage.toString());
                String xml = webBrowser.request(sbPage.toString(), Charset.forName("UTF-8"));

                List<String[]> movieList = parseComingSoonSearchResults(xml);

                if (movieList.size() > 0) {

                    for (int i = 0; i < movieList.size() && currentScore > 0; i++) {
                        String lId = (String) movieList.get(i)[0];
                        String lTitle = (String) movieList.get(i)[1];
                        String lOrig = (String) movieList.get(i)[2];
                        //String lYear = (String) movieList.get(i)[3];
                        int difference = compareTitles(movieName, lTitle);
                        int differenceOrig = compareTitles(movieName, lOrig);
                        difference = (differenceOrig < difference ? differenceOrig : difference);
                        if (difference < currentScore) {
                            if (difference == 0) {
                                LOG.debug("Found perfect match for: {}, {}", lTitle, lOrig);
                                searchPage = COMINGSOON_MAX_SEARCH_PAGES; //End loop
                            } else {
                                LOG.debug("Found a match for: {}, {], difference {}", lTitle, lOrig, difference);
                            }
                            comingSoonId = lId;
                            currentScore = difference;
                        }
                    }
                } else {
                    break;
                }
            }

            if (StringTools.isValidString(year) && currentScore > 0) {
                LOG.debug("Perfect match not found, trying removing by year...");
                String newComingSoonId = getComingSoonIdFromComingSoon(movieName, Movie.UNKNOWN, currentScore);
                comingSoonId = (StringTools.isNotValidString(newComingSoonId) ? comingSoonId : newComingSoonId);
            }

            if (StringTools.isValidString(comingSoonId)) {
                LOG.debug("Found valid ComingSoon ID: {}", comingSoonId);
            }

            return comingSoonId;

        } catch (IOException error) {
            LOG.error("Failed retreiving ComingSoon Id for movie : {}", movieName);
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
    }

    private List<String[]> parseComingSoonSearchResults(String xml) {

        /*
         * Search results end with "Trovati NNN Film" (found NNN movies). After
         * this string, more movie URL are found, so we have to set a boundary
         */
        List<String[]> listaFilm = new ArrayList<>();
        int beginIndex = StringUtils.indexOfIgnoreCase(xml, "Trovati");
        int moviesFound = -1;
        if (beginIndex > 0) {
            int end = xml.indexOf(" film", beginIndex + 7);
            if (end > 0) {
                String tmp = HTMLTools.stripTags(xml.substring(beginIndex + 8, xml.indexOf(" film", beginIndex)));
                moviesFound = NumberUtils.toInt(tmp, -1);
            }
        }

        if (moviesFound <= 0) {
            // Log an error message
            if (moviesFound < 0) {
                LOG.error("couldn't find 'Trovati NNN film in archivio' string. Search page layout probably changed");
            }
            return listaFilm;
        } else {
            LOG.debug("search found {} movies", moviesFound);
        }

        beginIndex = xml.indexOf("<section class=\"cs-components__section-contentsbox cinema\">");

        if (beginIndex >= 0) {
            // Looks like we have a movie list, so search for the start of the items
            beginIndex = xml.indexOf("<div class='cs-component__products-list-item'>", beginIndex + 1);

            while (beginIndex > 0) {
                int nextIndex = xml.indexOf("<div class='cs-component__products-list-item'>", beginIndex + 1);
                if (nextIndex < 0) {
                    nextIndex = xml.indexOf("<div class=\"central-adv-banner\">", beginIndex + 1);
                }

                String search = xml.substring(beginIndex, nextIndex);

                // Look for the ID of the movie
                int urlIndex = search.indexOf("/film");

                String comingSoonId = search.substring(urlIndex, search.indexOf('\'', urlIndex));
                comingSoonId = getComingSoonIdFromURL(comingSoonId);
                LOG.debug("Found ComingSoon ID: {}", comingSoonId);

                String title = HTMLTools.extractTag(search, "<h3>", 0, "><", false).trim();
                String originalTitle = HTMLTools.extractTag(search, "<h4>", 0, "><", false).trim();
                if (originalTitle.startsWith("(")) {
                    originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
                } else if (originalTitle.length() == 0) {
                    originalTitle = Movie.UNKNOWN;
                }

                String year = Movie.UNKNOWN;
                int beginYearIndex = search.indexOf("ANNO</span>:");
                if (beginYearIndex > 0) {
                    int end = search.indexOf("</li>", beginYearIndex);
                    if (end > 0) {
                        year = search.substring(beginYearIndex + 12, end).trim();
                    }
                }

                String[] movieData = {comingSoonId, title, originalTitle, year};
                listaFilm.add(movieData);

                beginIndex = xml.indexOf("<div class='cs-component__products-list-item'>", beginIndex + 1);
            }
        }

        return listaFilm;

    }

    private String getComingSoonIdFromURL(String url) {
        int index = url.indexOf("/scheda");
        if (index > -1) {
            String stripped = url.substring(0, index);
            index = StringUtils.lastIndexOf(stripped, '/');
            if (index > -1) {
                return stripped.substring(index + 1);
            }
        }
        return Movie.UNKNOWN;
    }

    /**
     * Returns difference between two titles.
     *
     * Since ComingSoon returns strange results on some researches, difference
     * is defined as follows: abs(word count difference) - (searchedTitle word
     * count - matched words);
     *
     * @param searchedTitle
     * @param returnedTitle
     * @return
     */
    private int compareTitles(String searchedTitle, String returnedTitle) {
        if (StringTools.isNotValidString(returnedTitle)) {
            return COMINGSOON_MAX_DIFF;
        }

        LOG.debug("Comparing {} and {}", searchedTitle, returnedTitle);

        String title1 = searchedTitle.toLowerCase().replaceAll("[,.\\!\\?\"']", "");
        String title2 = returnedTitle.toLowerCase().replaceAll("[,.\\!\\?\"']", "");
        int difference = StringUtils.getLevenshteinDistance(title1, title2);

        return difference;
    }

    protected boolean updateComingSoonMediaInfo(Movie movie) {
        if (movie.getId(COMINGSOON_PLUGIN_ID).equalsIgnoreCase(COMINGSOON_NOT_PRESENT)) {
            return false;
        }

        String xml;
        try {
            String movieURL = COMINGSOON_BASE_URL + COMINGSOON_FILM_URL + COMINGSOON_KEY_PARAM + movie.getId(COMINGSOON_PLUGIN_ID);
            LOG.debug("Querying ComingSoon for {}", movieURL);
            xml = webBrowser.request(movieURL, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            LOG.error("Failed retreiving ComingSoon data for movie : {}", movie.getId(COMINGSOON_PLUGIN_ID));
            LOG.error(SystemTools.getStackTrace(ex));
            return false;
        }

        // TITLE
        if (OverrideTools.checkOverwriteTitle(movie, COMINGSOON_PLUGIN_ID)) {
            String title = HTMLTools.extractTag(xml, "<h1 itemprop='name'>", "</h1>").trim();
            if (StringTools.isNotValidString(title)) {
                LOG.error("No title found at ComingSoon page. HTML layout has changed?");
                return false;
            }
            movie.setTitle(WordUtils.capitalizeFully(title), COMINGSOON_PLUGIN_ID);
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(movie, COMINGSOON_PLUGIN_ID)) {
            String originalTitle = HTMLTools.extractTag(xml, "<h2><em>", "</em></h2>").trim();
            if (originalTitle.startsWith("(")) {
                originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            }

            movie.setOriginalTitle(WordUtils.capitalizeFully(originalTitle), COMINGSOON_PLUGIN_ID);
        }

        // RATING
        if (movie.getRating(COMINGSOON_PLUGIN_ID) == -1) {
            String rating = HTMLTools.extractTag(xml, "<li class=\'current-rating\'", 1, "<>", false).trim();
            LOG.debug("Found rating {}", rating);
            if (StringTools.isValidString(rating)) {
                rating = rating.substring(rating.indexOf(' ') + 1, rating.indexOf('/'));
                int ratingInt = (int) (NumberUtils.toFloat(rating.replace(',', '.'), 0) * 20); // Rating is 0 to 5, we normalize to 100
                if (ratingInt > 0) {
                    movie.addRating(COMINGSOON_PLUGIN_ID, ratingInt);
                }
            }
        }

        // RELEASE DATE
        if (OverrideTools.checkOverwriteReleaseDate(movie, COMINGSOON_PLUGIN_ID)) {
            String releaseDate = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">DATA USCITA</strong>:", "</li>"));
            if (StringTools.isValidString(releaseDate)) {
                try {
                    DateTime rDate = new DateTime(releaseDate);
                    movie.setReleaseDate(rDate.toString("yyyy-MM-dd"), COMINGSOON_PLUGIN_ID);
                } catch (Exception e) {
                    LOG.warn("Failed to parse release date {}", releaseDate);
                }
            }
        }

        // RUNTIME
        if (OverrideTools.checkOverwriteRuntime(movie, COMINGSOON_PLUGIN_ID)) {
            String runTime = HTMLTools.stripTags(HTMLTools.extractTag(xml, "DURATA</strong>:", "</li>"));
            if (StringTools.isValidString(runTime)) {
                StringTokenizer st = new StringTokenizer(runTime);
                movie.setRuntime(st.nextToken(), COMINGSOON_PLUGIN_ID);
            }
        }

        // COUNTRY
        if (OverrideTools.checkOverwriteCountry(movie, COMINGSOON_PLUGIN_ID)) {
            String country = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PAESE</strong>: ", "</li>")).trim();
            movie.setCountries(country, COMINGSOON_PLUGIN_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(movie, COMINGSOON_PLUGIN_ID)) {
            String year = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ANNO</strong>: ", "</li>")).trim();
            if (NumberUtils.toInt(year, 0) > 1900) {
                movie.setYear(year, COMINGSOON_PLUGIN_ID);
            }
        }

        // COMPANY
        if (OverrideTools.checkOverwriteCompany(movie, COMINGSOON_PLUGIN_ID)) {
            // TODO: Add more than one company when available in Movie model
            String companies = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PRODUZIONE</strong>: ", "</li>"));
            StringTokenizer st = new StringTokenizer(companies, ",");
            if (st.hasMoreTokens()) {
                movie.setCompany(st.nextToken().trim(), COMINGSOON_PLUGIN_ID);
            } else {
                movie.setCompany(companies.trim(), COMINGSOON_PLUGIN_ID);
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(movie, COMINGSOON_PLUGIN_ID)) {
            String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE</strong>: ", "</li>"));
            if (StringTools.isValidString(genreList)) {
                Collection<String> genres = new ArrayList<>();

                StringTokenizer st = new StringTokenizer(genreList, ",");
                while (st.hasMoreTokens()) {
                    genres.add(st.nextToken().trim());
                }
                movie.setGenres(genres, COMINGSOON_PLUGIN_ID);
            }
        }

        // PLOT AND OUTLINE
        if (OverrideTools.checkOneOverwrite(movie, COMINGSOON_PLUGIN_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {

            int beginIndex = xml.indexOf("<div class=\"product-profile-box-toprow-text\">");
            if (beginIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return false;
            }

            int endIndex = xml.indexOf("</div>", beginIndex);
            if (endIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return false;
            }

            String xmlPlot = xml.substring(beginIndex, endIndex).trim();
            xmlPlot = HTMLTools.stripTags(xmlPlot).trim();

            if (OverrideTools.checkOverwritePlot(movie, COMINGSOON_PLUGIN_ID)) {
                movie.setPlot(xmlPlot, COMINGSOON_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteOutline(movie, COMINGSOON_PLUGIN_ID)) {
                movie.setOutline(xmlPlot, COMINGSOON_PLUGIN_ID);
            }
        }

        // DIRECTOR(S)
        boolean overrideDirectors = OverrideTools.checkOverwriteDirectors(movie, COMINGSOON_PLUGIN_ID);
        boolean overridePeopleDirectors = OverrideTools.checkOverwritePeopleDirectors(movie, COMINGSOON_PLUGIN_ID);
        if (overrideDirectors || overridePeopleDirectors) {
            String directorList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">REGIA</strong>: ", "</li>"));

            List<String> newDirectors = new ArrayList<>();
            if (directorList.contains(",")) {
                StringTokenizer st = new StringTokenizer(directorList, ",");
                while (st.hasMoreTokens()) {
                    newDirectors.add(st.nextToken());
                }
            } else {
                newDirectors.add(directorList);
            }

            if (overrideDirectors) {
                movie.setDirectors(newDirectors, COMINGSOON_PLUGIN_ID);
            }
            if (overridePeopleDirectors) {
                movie.setPeopleDirectors(newDirectors, COMINGSOON_PLUGIN_ID);
            }
        }

        // WRITER(S)
        boolean overrideWriters = OverrideTools.checkOverwriteWriters(movie, COMINGSOON_PLUGIN_ID);
        boolean overridePeopleWriters = OverrideTools.checkOverwritePeopleWriters(movie, COMINGSOON_PLUGIN_ID);

        if (overrideWriters || overridePeopleWriters) {
            String writerList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">SCENEGGIATURA</strong>: ", "</li>"));

            List<String> newWriters = new ArrayList<>();
            if (writerList.contains(",")) {
                StringTokenizer st = new StringTokenizer(writerList, ",");
                while (st.hasMoreTokens()) {
                    newWriters.add(st.nextToken());
                }
            } else {
                newWriters.add(writerList);
            }

            if (overrideWriters) {
                movie.setWriters(newWriters, COMINGSOON_PLUGIN_ID);
            }
            if (overridePeopleWriters) {
                movie.setPeopleWriters(newWriters, COMINGSOON_PLUGIN_ID);
            }
        }

        // CAST
        boolean overrideActors = OverrideTools.checkOverwriteActors(movie, COMINGSOON_PLUGIN_ID);
        boolean overridePeopleActors = OverrideTools.checkOverwritePeopleActors(movie, COMINGSOON_PLUGIN_ID);
        if (overrideActors || overridePeopleActors) {
            String castList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ATTORI</strong>: ", "</li>"));

            List<String> newActors = new ArrayList<>();
            if (castList.contains(",")) {
                StringTokenizer st = new StringTokenizer(castList, ",");
                while (st.hasMoreTokens()) {
                    newActors.add(HTMLTools.stripTags(st.nextToken()));
                }
            } else {
                newActors.add(HTMLTools.stripTags(castList));
            }

            if (overrideActors) {
                movie.setCast(newActors, COMINGSOON_PLUGIN_ID);
            }
            if (overridePeopleActors) {
                movie.setPeopleCast(newActors, COMINGSOON_PLUGIN_ID);
            }
        }

        return true;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB as basis

        boolean result = false;
        int beginIndex = nfo.indexOf("?key=");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 5), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(COMINGSOON_PLUGIN_ID, st.nextToken());
            LOG.debug("ComingSoon Id found in nfo = {}", movie.getId(COMINGSOON_PLUGIN_ID));
            result = true;
        } else {
            LOG.debug("No ComingSoon ID found in nfo!");
        }
        return result;
    }
}
