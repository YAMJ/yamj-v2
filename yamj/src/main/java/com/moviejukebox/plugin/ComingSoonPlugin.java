/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
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
    public static final String COMINGSOON_SEARCH_URL = "film/?";
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

            StringBuilder urlBase = new StringBuilder(COMINGSOON_BASE_URL);
            urlBase.append(COMINGSOON_SEARCH_URL);
            urlBase.append(CS_TITLE_PARAM);
            urlBase.append(URLEncoder.encode(movieName.toLowerCase(), "UTF-8"));

            urlBase.append("&").append(CS_YEAR_PARAM);
            if (StringTools.isValidString(year)) {
                urlBase.append(year);
            }
            urlBase.append(COMINGSOON_SEARCH_PARAMS);

            int searchPage = 0;

            while (searchPage++ < COMINGSOON_MAX_SEARCH_PAGES) {

                StringBuilder urlPage = new StringBuilder(urlBase);
                if (searchPage > 1) {
                    urlPage.append("&p=").append(searchPage);
                }

                LOG.debug("Fetching ComingSoon search page {}/{} - URL: {}", searchPage, COMINGSOON_MAX_SEARCH_PAGES, urlPage.toString());
                String xml = httpClient.request(urlPage.toString());

                List<String[]> movieList = parseComingSoonSearchResults(xml);

                if (!movieList.isEmpty()) {

                    for (int i = 0; i < movieList.size() && currentScore > 0; i++) {
                        String lId = movieList.get(i)[0];
                        String lTitle = movieList.get(i)[1];
                        String lOrig = movieList.get(i)[2];
                        //String lYear = (String) movieList.get(i)[3];
                        int difference = compareTitles(movieName, lTitle);
                        int differenceOrig = compareTitles(movieName, lOrig);
                        difference = (differenceOrig < difference ? differenceOrig : difference);
                        if (difference < currentScore) {
                            if (difference == 0) {
                                LOG.debug("Found perfect match for: {}, {}", lTitle, lOrig);
                                searchPage = COMINGSOON_MAX_SEARCH_PAGES; //End loop
                            } else {
                                LOG.debug("Found a match for: {}, {}, difference {}", lTitle, lOrig, difference);
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

    /**
     * Parse the search results
     *
     * Search results end with "Trovati NNN Film" (found NNN movies).
     *
     * After this string, more movie URL are found, so we have to set a boundary
     *
     * @param xml
     * @return
     */
    private static List<String[]> parseComingSoonSearchResults(String xml) {
        final List<String[]> result = new ArrayList<>();
        
        int beginIndex = StringUtils.indexOfIgnoreCase(xml, "Trovate");
        int moviesFound = -1;
        if (beginIndex > 0) {
            int end = xml.indexOf(" film", beginIndex + 7);
            if (end > 0) {
                String tmp = HTMLTools.stripTags(xml.substring(beginIndex + 8, xml.indexOf(" film", beginIndex)));
                moviesFound = NumberUtils.toInt(tmp, -1);
            }
        }

        if (moviesFound < 0) {
            LOG.error("Couldn't find 'Trovate NNN film in archivio' string. Search page layout probably changed");
            return result;
        }
 
        List<String> films = HTMLTools.extractTags(xml, "box-lista-cinema", "BOX FILM RICERCA", "<a h", "</a>", false);
        if (CollectionUtils.isEmpty(films)) {
            return result;
        }
        
        LOG.debug("Search found {} movies", films.size());

        for (String film : films) {
            String comingSoonId = null;
            beginIndex = film.indexOf("ref=\"/film/");
            if (beginIndex >= 0) {
                comingSoonId = getComingSoonIdFromURL(film);
            }
            if (StringTools.isNotValidString(comingSoonId)) continue;

            String title = HTMLTools.extractTag(film, "<div class=\"h5 titolo cat-hover-color anim25\">", "</div>");
            if (StringTools.isNotValidString(title)) continue;
            
            String originalTitle = HTMLTools.extractTag(film, "<div class=\"h6 sottotitolo\">", "</div>");
            if (StringTools.isNotValidString(originalTitle)) originalTitle = Movie.UNKNOWN;
            if (originalTitle.startsWith("(")) originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            
            String year = Movie.UNKNOWN;
            beginIndex = film.indexOf("ANNO</span>:");
            if (beginIndex > 0) {
                int endIndex = film.indexOf("</li>", beginIndex);
                if (endIndex > 0) {
                    year = film.substring(beginIndex + 12, endIndex).trim();
                }
            }
            
            result.add(new String[]{comingSoonId, title, originalTitle, year});
        }

        return result;
    }

    private static String getComingSoonIdFromURL(String url) {
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
    private static int compareTitles(String searchedTitle, String returnedTitle) {
        if (StringTools.isNotValidString(returnedTitle)) {
            return COMINGSOON_MAX_DIFF;
        }

        LOG.trace("Comparing {} and {}", searchedTitle, returnedTitle);

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
            xml = httpClient.request(movieURL);
        } catch (IOException ex) {
            LOG.error("Failed retreiving ComingSoon data for movie : {}", movie.getId(COMINGSOON_PLUGIN_ID));
            LOG.error(SystemTools.getStackTrace(ex));
            return false;
        }

        // TITLE
        if (OverrideTools.checkOverwriteTitle(movie, COMINGSOON_PLUGIN_ID)) {
            int beginIndex = xml.indexOf("<h1 itemprop=\"name\"");
            if (beginIndex < 0 ) {
                LOG.error("No title found at ComingSoon page. HTML layout has changed?");
                return false;
            }
            
            String tag = xml.substring(beginIndex, xml.indexOf(">", beginIndex)+1);
            String title = HTMLTools.extractTag(xml, tag, "</h1>").trim();
            if (StringTools.isNotValidString(title)) {
                return false;
            }
            
            movie.setTitle(WordUtils.capitalizeFully(title), COMINGSOON_PLUGIN_ID);
        }

        // ORIGINAL TITLE
        if (OverrideTools.checkOverwriteOriginalTitle(movie, COMINGSOON_PLUGIN_ID)) {
            String originalTitle = HTMLTools.extractTag(xml, "Titolo originale:", "</p>").trim();
            if (originalTitle.startsWith("(")) {
                originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            }

            movie.setOriginalTitle(originalTitle, COMINGSOON_PLUGIN_ID);
        }

        // RATING
        if (movie.getRating(COMINGSOON_PLUGIN_ID) == -1) {
            String rating = HTMLTools.extractTag(xml, "<span itemprop=\"ratingValue\">", "</span>");
            if (StringTools.isValidString(rating)) {
                int ratingInt = (int) (NumberUtils.toFloat(rating.replace(',', '.'), 0) * 20); // Rating is 0 to 5, we normalize to 100
                if (ratingInt > 0) {
                    movie.addRating(COMINGSOON_PLUGIN_ID, ratingInt);
                }
            }
        }

        // RELEASE DATE
        if (OverrideTools.checkOverwriteReleaseDate(movie, COMINGSOON_PLUGIN_ID)) {
            String releaseDate = HTMLTools.stripTags(HTMLTools.extractTag(xml, "<time itemprop=\"datePublished\">", "</time>"));
            movie.setReleaseDate(releaseDate, COMINGSOON_PLUGIN_ID);
        }

        // RUNTIME
        if (OverrideTools.checkOverwriteRuntime(movie, COMINGSOON_PLUGIN_ID)) {
            String runTime = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">DURATA</span>:", "</li>"));
            if (StringTools.isValidString(runTime)) {
                StringTokenizer st = new StringTokenizer(runTime);
                movie.setRuntime(st.nextToken(), COMINGSOON_PLUGIN_ID);
            }
        }

        // COUNTRY
        if (OverrideTools.checkOverwriteCountry(movie, COMINGSOON_PLUGIN_ID)) {
            String country = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PAESE</span>:", "</li>")).trim();
            movie.setCountries(country, COMINGSOON_PLUGIN_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(movie, COMINGSOON_PLUGIN_ID)) {
            String year = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ANNO</span>:", "</li>")).trim();
            if (NumberUtils.toInt(year, 0) > 1900) {
                movie.setYear(year, COMINGSOON_PLUGIN_ID);
            }
        }

        // COMPANY
        if (OverrideTools.checkOverwriteCompany(movie, COMINGSOON_PLUGIN_ID)) {
            // TODO: Add more than one company when available in Movie model
            String companies = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PRODUZIONE</span>: ","</li>"));
            StringTokenizer st = new StringTokenizer(companies, ",");
            if (st.hasMoreTokens()) {
                movie.setCompany(st.nextToken().trim(), COMINGSOON_PLUGIN_ID);
            } else {
                movie.setCompany(companies.trim(), COMINGSOON_PLUGIN_ID);
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(movie, COMINGSOON_PLUGIN_ID)) {
            String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE</span>: ", "</li>"));
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
            int beginIndex = xml.indexOf("<div class=\"contenuto-scheda-destra");
            if (beginIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return false;
            }

            int endIndex = xml.indexOf("<div class=\"box-descrizione\"", beginIndex);
            if (endIndex < 0) {
                LOG.error("No plot found at ComingSoon page. HTML layout has changed?");
                return false;
            }

            final String xmlPlot = HTMLTools.stripTags(HTMLTools.extractTag(xml.substring(beginIndex, endIndex), "<p>", "</p>"));
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
            String directorList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">REGIA</span>:", "</li>"));

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
            String writerList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">SCENEGGIATURA</span>:", "</li>"));

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
            String castList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ATTORI</span>: ", "</li>"));

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
