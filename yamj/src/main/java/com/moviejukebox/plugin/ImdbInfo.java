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

import com.moviejukebox.model.ImdbSiteDataDefinition;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class ImdbInfo {

    private static final Logger logger = Logger.getLogger(ImdbInfo.class);
    private static final String LOG_MESSAGE = "ImdbInfo: ";
    private static final String DEFAULT_SITE = "us";
    private static final String OBJECT_MOVIE = "movie";
    private static final String OBJECT_PERSON = "person";
    protected static final Map<String, ImdbSiteDataDefinition> MATCHES_DATA_PER_SITE = new HashMap<String, ImdbSiteDataDefinition>();
    private final String imdbSite = PropertiesUtil.getProperty("imdb.site", DEFAULT_SITE);
    private final Boolean exactMatch = PropertiesUtil.getBooleanProperty("imdb.exact.match", "false");
    private String preferredSearchEngine;
    private WebBrowser webBrowser;
    private String objectType = OBJECT_MOVIE;
    private ImdbSiteDataDefinition siteDef;
//    private static final String[] SEARCH_ORDER = {"Search results", "Popular Titles", "Titles (Exact Matches)", "Titles (Partial Matches)", "Titles (Approx Matches)"};

    static {
        MATCHES_DATA_PER_SITE.put("us", new ImdbSiteDataDefinition("http://www.imdb.com/", "UTF-8", "Director|Directed by", "Cast", "Release Date", "Runtime", "Aspect Ratio", "Country",
                "Company", "Genre", "Quotes", "Plot", "Rated", "Certification", "Original Air Date", "Writer|Writing credits", "Taglines"));

        MATCHES_DATA_PER_SITE.put("fr", new ImdbSiteDataDefinition("http://www.imdb.fr/", "ISO-8859-1", "R&#xE9;alisateur|R&#xE9;alis&#xE9; par", "Ensemble", "Date de sortie", "Dur&#xE9;e", "Aspect Ratio", "Pays",
                "Soci&#xE9;t&#xE9;", "Genre", "Citation", "Intrigue", "Rated", "Classification", "Date de sortie", "Sc&#xE9;naristes|Sc&#xE9;naristes", "Taglines"));

        MATCHES_DATA_PER_SITE.put("es", new ImdbSiteDataDefinition("http://www.imdb.es/", "ISO-8859-1", "Director|Dirigida por", "Reparto", "Fecha de Estreno", "Duraci&#xF3;n", "Relaci&#xF3;n de Aspecto", "Pa&#xED;s",
                "Compa&#xF1;&#xED;a", "G&#xE9;nero", "Quotes", "Trama", "Rated", "Clasificaci&#xF3;n", "Fecha de Estreno", "Escritores|Cr&#xE9;ditos del gui&#xF3;n", "Taglines"));

        MATCHES_DATA_PER_SITE.put("de", new ImdbSiteDataDefinition("http://www.imdb.de/", "ISO-8859-1", "Regisseur|Regie", "Besetzung", "Premierendatum", "L&#xE4;nge", "Seitenverh&#xE4;ltnis", "Land",
                "Firma", "Genre", "Quotes", "Handlung", "Rated", "Altersfreigabe", "Premierendatum", "Guionista|Buch", "Taglines"));

        MATCHES_DATA_PER_SITE.put("it", new ImdbSiteDataDefinition("http://www.imdb.it/", "ISO-8859-1", "Regista|Registi|Regia di", "Cast", "Data di uscita", "Durata", "Aspect Ratio",
                "Nazionalit&#xE0;", "Compagnia", "Genere", "Quotes", "Trama", "Rated", "Divieti", "Data di uscita", "Sceneggiatore|Scritto da", "Taglines"));

        MATCHES_DATA_PER_SITE.put("pt", new ImdbSiteDataDefinition("http://www.imdb.pt/", "UTF-8", "Diretor|Dirigido por", "Elenco", "Data de Lan&#xE7;amento", "Dura&#xE7;&#xE3;o", "Aspect Ratio",
                "Pa&#xED;s", "Companhia", "G&#xEA;nero", "Quotes", "Argumento", "Rated", "Certifica&#xE7;&#xE3;o", "Data de Lan&#xE7;amento",
                "Roteirista|Cr&#xE9;ditos como roteirista", "Taglines"));

        // Use this as a workaround for English speakers abroad who get localised versions of imdb.com
        MATCHES_DATA_PER_SITE.put("labs", new ImdbSiteDataDefinition("http://akas.imdb.com/", "UTF-8", "Director|Directors|Directed by", "Cast", "Release Date", "Runtime", "Aspect Ratio", "Country",
                "Production Co", "Genres", "Quotes", "Storyline", "Rated", "Certification", "Original Air Date", "Writer|Writers|Writing credits", "Taglines"));

        // TODO: Leaving this as labs.imdb.com for the time being, but will be updated to www.imdb.com
        MATCHES_DATA_PER_SITE.put("us2", new ImdbSiteDataDefinition("http://labs.imdb.com/", "UTF-8", "Director|Directors|Directed by", "Cast", "Release Date", "Runtime", "Aspect Ratio", "Country",
                "Production Co", "Genres", "Quotes", "Storyline", "Rated", "Certification", "Original Air Date", "Writer|Writers|Writing credits", "Taglines"));

        // Not 100% sure these are correct
        MATCHES_DATA_PER_SITE.put("it2", new ImdbSiteDataDefinition("http://www.imdb.it/", "UTF-8", "Regista|Registi|Regia di", "Attori", "Data di uscita", "Durata", "Aspect Ratio",
                "Nazionalit&#xE0;", "Compagnia", "Genere", "Quotes", "Trama", "Rated", "Divieti", "Data di uscita", "Sceneggiatore|Scritto da", "Taglines"));
    }

    public void setPreferredSearchEngine(String preferredSearchEngine) {
        this.preferredSearchEngine = preferredSearchEngine;
    }

    public ImdbInfo() {
        webBrowser = new WebBrowser();

        preferredSearchEngine = PropertiesUtil.getProperty("imdb.id.search", "imdb");
        siteDef = MATCHES_DATA_PER_SITE.get(imdbSite);
        if (siteDef == null) {
            logger.warn(LOG_MESSAGE + "No site definition for " + imdbSite + " using the default instead " + DEFAULT_SITE);
            siteDef = MATCHES_DATA_PER_SITE.get(DEFAULT_SITE);
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This
     * routine is based on a IMDb request.
     */
    public String getImdbId(String movieName, String year) {
        objectType = OBJECT_MOVIE;
        
        if ("google".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromGoogle(movieName, year);
        } else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromYahoo(movieName, year);
        } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
            return Movie.UNKNOWN;
        } else {
            return getImdbIdFromImdb(movieName, year);
        }
    }

    /**
     * Get the IMDb ID for a person. Note: The job is not used in this search.
     *
     * @param movieName
     * @param job
     * @return
     */
    public String getImdbPersonId(String personName, String movieId) {
        try {
            if (StringTools.isValidString(movieId)) {
                StringBuilder sb = new StringBuilder(siteDef.getSite());
                sb.append("search/name?name=");
                sb.append(URLEncoder.encode(personName, siteDef.getCharset().displayName())).append("&role=").append(movieId);

                logger.debug(LOG_MESSAGE + "Querying IMDB for " + sb.toString());
                String xml = webBrowser.request(sb.toString());

                // Check if this is an exact match (we got a person page instead of a results list)
                Matcher titlematch = siteDef.getPersonRegex().matcher(xml);
                if (titlematch.find()) {
                    logger.debug(LOG_MESSAGE + "IMDb returned one match " + titlematch.group(1));
                    return titlematch.group(1);
                }

                String firstPersonId = HTMLTools.extractTag(HTMLTools.extractTag(xml, "<tr class=\"even detailed\">", "</tr>"), "<a href=\"/name/", "/\"");
                if (StringTools.isValidString(firstPersonId)) {
                    return firstPersonId;
                }
            }

            return getImdbPersonId(personName);
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving IMDb Id for person : " + personName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
        }

        return Movie.UNKNOWN;
    }

    /**
     * Get the IMDb ID for a person
     *
     * @param movieName
     * @return
     */
    public String getImdbPersonId(String personName) {
        objectType = OBJECT_PERSON;

        if ("google".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromGoogle(personName, Movie.UNKNOWN);
        } else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromYahoo(personName, Movie.UNKNOWN);
        } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
            return Movie.UNKNOWN;
        } else {
            return getImdbIdFromImdb(personName.toLowerCase(), Movie.UNKNOWN);
        }
    }

    /**
     * Retrieve the IMDb Id matching the specified movie name and year. This
     * routine is base on a yahoo request.
     *
     * @param movieName The name of the Movie to search for
     * @param year The year of the movie
     * @return The IMDb Id if it was found
     */
    private String getImdbIdFromYahoo(String movieName, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://search.yahoo.com/search;_ylt=A1f4cfvx9C1I1qQAACVjAQx.?p=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Aimdb.com&fr=yfp-t-501&ei=UTF-8&rd=r1");

            logger.debug(LOG_MESSAGE + "Yahoo search: " + sb.toString());

            return getImdbIdFromSearchEngine(sb.toString());

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving IMDb Id for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This
     * routine is base on a Google request.
     *
     * @param movieName The name of the Movie to search for
     * @param year The year of the movie
     * @return The IMDb Id if it was found
     */
    private String getImdbIdFromGoogle(String movieName, String year) {
        try {
            logger.debug(LOG_MESSAGE + "querying Google for " + movieName);

            StringBuilder sb = new StringBuilder("http://www.google.com/search?q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Awww.imdb.com&meta=");

            logger.debug(LOG_MESSAGE + "Google search: " + sb.toString());

            return getImdbIdFromSearchEngine(sb.toString());

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving IMDb Id for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private String getImdbIdFromSearchEngine(String requestString) throws Exception {
        String xml = webBrowser.request(requestString);
        String imdbId = Movie.UNKNOWN;

        int beginIndex = xml.indexOf(objectType.equals(OBJECT_MOVIE) ? "/title/tt" : "/name/nm");
        if (beginIndex > -1) {
            int index;
            if (objectType.equals(OBJECT_MOVIE)) {
                index = beginIndex + 7;
            } else {
                index = beginIndex + 6;
            }
            StringTokenizer st = new StringTokenizer(xml.substring(index), "/\"");
            imdbId = st.nextToken();
        }

        if (imdbId.startsWith(objectType.equals(OBJECT_MOVIE) ? "tt" : "nm")) {
            logger.debug("Found IMDb ID: " + imdbId);
            return imdbId;
        } else {
            return Movie.UNKNOWN;
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This
     * routine is base on a IMDb request.
     */
    private String getImdbIdFromImdb(String movieName, String year) {
        /*
         * IMDb matches seem to come in several "flavours".
         *
         * Firstly, if there is one exact match it returns the matching IMDb page.
         *
         * If that fails to produce a unique hit then a list of possible matches are returned categorised as:
         *      Popular Titles (Displaying ? Results)
         *      Titles (Exact Matches) (Displaying ? Results)
         *      Titles (Partial Matches) (Displaying ? Results)
         *
         * We should check the Exact match section first, then the poplar titles and finally the partial matches.
         *
         * Note: That even with exact matches there can be more than 1 hit, for example "Star Trek"
         */

//        logger.info(LOG_MESSAGE + "Movie Name: '" + movieName + "' (" + year + ")");

        StringBuilder sb = new StringBuilder(siteDef.getSite());
        sb.append("find?q=");
        try {
            sb.append(URLEncoder.encode(movieName, siteDef.getCharset().displayName()));
        } catch (UnsupportedEncodingException ex) {
            // Failed to encode the movie name for some reason!
            logger.debug(LOG_MESSAGE + "Failed to encode movie name: " + movieName);
            sb.append(movieName);
        }

        if (StringTools.isValidString(year)) {
            sb.append("+%28").append(year).append("%29");
        }
        sb.append("&s=");
        sb.append(objectType.equals(OBJECT_MOVIE) ? "tt" : "nm");
        sb.append("&site=aka");

        logger.debug(LOG_MESSAGE + "Querying IMDB for " + sb.toString());
        String xml;
        try {
            xml = webBrowser.request(sb.toString());
        } catch (IOException ex) {
            logger.error(LOG_MESSAGE + "Failed retreiving IMDb Id for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + ex.getMessage());
            return Movie.UNKNOWN;
        }

        // Check if this is an exact match (we got a movie page instead of a results list)
        Pattern titleregex = siteDef.getPersonRegex();
        if(objectType.equals(OBJECT_MOVIE)){
        	titleregex= siteDef.getTitleRegex();
        }

        Matcher titlematch = titleregex.matcher(xml);
        if (titlematch.find()) {
            logger.debug(LOG_MESSAGE + "IMDb returned one match " + titlematch.group(1));
            return titlematch.group(1);
        }

        String searchName = HTMLTools.extractTag(HTMLTools.extractTag(xml, ";ttype=ep\">", "\"</a>.</li>"), "<b>", "</b>").toLowerCase();
        final String formattedName;
        final String formattedYear;
        final String formattedExact;
        
        if (StringTools.isValidString(searchName)) {
            if (StringTools.isValidString(year) && searchName.endsWith(")") && searchName.contains("(")) {
                searchName = searchName.substring(0, searchName.lastIndexOf('(') - 1);
                formattedName = searchName.toLowerCase();
                formattedYear = "(" + year + ")";
                formattedExact = formattedName + "</a> " + formattedYear;
            } else {
                formattedName = searchName.toLowerCase();
                formattedYear = "</a>";
                formattedExact = formattedName + formattedYear;
            }
        } else {
            sb = new StringBuilder();
            try {
                sb.append(URLEncoder.encode(movieName, siteDef.getCharset().displayName()).replace("+", " "));
            } catch (UnsupportedEncodingException ex) {
                logger.debug(LOG_MESSAGE + "Failed to encode movie name: " + movieName);
                sb.append(movieName);
            }
            formattedName = sb.toString().toLowerCase();
            if (StringTools.isValidString(year)) {
                formattedYear = "(" + year + ")";
                formattedExact = formattedName + "</a> " + formattedYear;
            } else {
                formattedYear = "</a>";
                formattedExact = formattedName + formattedYear;
                
            }
            searchName = formattedExact;
        }

        // logger.debug(LOG_MESSAGE + "Search: name='" + formattedName + "', year='" + formattedYear);
        for (String searchResult : HTMLTools.extractTags(xml, "<table class=\"findList\">", "</table>", "<td class=\"result_text\">", "</td>", false)) {
            // logger.debug(LOG_MESSAGE + "Check  : '" + searchResult + "'");
            boolean foundMatch = false;
            if (exactMatch) {
                foundMatch = (searchResult.toLowerCase().indexOf(formattedExact) != -1);
            } else {
                int nameIndex = searchResult.toLowerCase().indexOf(formattedName);
                if (nameIndex != -1 ) {
                    foundMatch = (searchResult.indexOf(formattedYear) > nameIndex);
                }
            }
            
            if (foundMatch) {
                // logger.debug(LOG_MESSAGE + "Title match  : '" + searchResult + "'");
                return HTMLTools.extractTag(searchResult, "<a href=\"" + (objectType.equals(OBJECT_MOVIE) ? "/title/" : "/name/"), "/");
            } else {
                for (String otherResult : HTMLTools.extractTags(searchResult, "</';\">", "</p>", "<p class=\"find-aka\">", "</em>", false)) {
                    if (otherResult.toLowerCase().indexOf("\"" + searchName + "\"") != -1) {
                        // logger.debug(LOG_MESSAGE + "Other title match: '" + otherResult + "'");
                        return HTMLTools.extractTag(searchResult, "/images/b.gif?link=" + (objectType.equals(OBJECT_MOVIE) ? "/title/" : "/name/"), "/';\">");
                    }
                }
            }
        }

       // alternate search for person ID
       if (objectType.equals(OBJECT_PERSON)) {
            String firstPersonId = HTMLTools.extractTag(HTMLTools.extractTag(xml, "<table><tr> <td valign=\"top\">", "</td></tr></table>"), "<a href=\"/name/", "/\"");
            if (StringTools.isNotValidString(firstPersonId)) {
                // alternate approach
                int beginIndex = xml.indexOf("<a href=\"/name/nm");
                if (beginIndex > -1) {
                    StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 15), "/\"");
                    firstPersonId = st.nextToken();
                }
            }

            if (firstPersonId.startsWith("nm")) {
                logger.debug("Found IMDb ID: " + firstPersonId);
                return firstPersonId;
            }
        }

        // If we don't have an ID try google
        logger.debug(LOG_MESSAGE + "Failed to find a match on IMDb, trying Google");
        return getImdbIdFromGoogle(movieName, year);
    }

    /**
     * Retrieve the IMDb ID using the ImdbAPI.
     *
     * @param movieName
     * @param year
     * @return
     */
    /*
    private String getImdbIdFromImdbApi(final String movieName, final String year) {
        String imdbId = Movie.UNKNOWN;
        Map<String, List<SearchObject>> result = ImdbApi.getSearch(movieName);

        if (result == null || result.isEmpty()) {
            logger.debug(LOG_MESSAGE + "No results found for " + movieName);
            return Movie.UNKNOWN;
        }

        ImdbMovieDetails imdbMovie;
        for (String searchType : SEARCH_ORDER) {
            logger.trace(LOG_MESSAGE + "Checking search type: " + searchType + " for " + movieName + "(" + year + ")");
            imdbMovie = matchSearchObject(result.get(searchType), movieName, year);
            if (imdbMovie != null) {
                logger.debug(LOG_MESSAGE + "Match found in " + searchType + ": " + imdbMovie.getTitle() + " (" + imdbMovie.getYear() + ") = " + imdbMovie.getImdbId());
                imdbId = imdbMovie.getImdbId();
                break;
            }
        }

        return imdbId;
    }
    */

    /**
     * Loops around the search results looking for a close match based on year
     *
     * @param resultList
     * @param movieName
     * @param year
     * @return
     */
    /*
    private ImdbMovieDetails matchSearchObject(final List<SearchObject> resultList, final String movieName, final String year) {
        if (resultList == null || (!resultList.isEmpty() && resultList.get(0).getClass() != ImdbMovieDetails.class)) {
            logger.trace(LOG_MESSAGE + movieName + "(" + year + ") - No results found for this search type");
            return null;
        } else {
            logger.trace(LOG_MESSAGE + movieName + "(" + year + ") - Found " + resultList.size() + " results");
        }

        for (SearchObject searchResult : resultList) {
            if (searchResult.getClass() == ImdbMovieDetails.class) {
                ImdbMovieDetails imdbMovie = (ImdbMovieDetails) searchResult;
                logger.debug(LOG_MESSAGE + "Checking: " + imdbMovie.getTitle() + " (" + imdbMovie.getYear() + ") = " + imdbMovie.getImdbId());
                if (year.equals(String.valueOf(imdbMovie.getYear())) || StringTools.isNotValidString(year)) {
                    return (ImdbMovieDetails) searchResult;
                }
            }
        }

        return null;
    }
    */

    public ImdbSiteDataDefinition getSiteDef() {
        return siteDef;
    }

    /**
     * Get a specific site definition from the list
     *
     * @param requiredSiteDef
     * @return The Site definition if found, null otherwise
     */
    public ImdbSiteDataDefinition getSiteDef(String requiredSiteDef) {
        return MATCHES_DATA_PER_SITE.get(requiredSiteDef);
    }

    public String getPreferredSearchEngine() {
        return preferredSearchEngine;
    }

    public String getImdbSite() {
        return imdbSite;
    }
}
