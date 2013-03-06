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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;

/**
 * @author Durin
 */
public class OfdbPlugin implements MovieDatabasePlugin {

    public static final String OFDB_PLUGIN_ID = "ofdb";
    private static final Logger LOGGER = Logger.getLogger(OfdbPlugin.class);
    private static final String LOG_MESSAGE = "OfdbPlugin: ";
    
    private ImdbPlugin imdbp;
    private WebBrowser webBrowser;

    public OfdbPlugin() {
        imdbp = new com.moviejukebox.plugin.ImdbPlugin();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getPluginID() {
        return OFDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        imdbp.scan(movie);
        
        String ofdbId = getOfdbId(movie);
        if (StringTools.isNotValidString(ofdbId)) {
            return Boolean.FALSE;
        }

        boolean returnValue = Boolean.TRUE;
        try {
            String xml = webBrowser.request(ofdbId);

            // retrieve IMDB id if not set
            String imdbId = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
            if (StringTools.isNotValidString(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID))) {
                imdbId = HTMLTools.extractTag(xml, "href=\"http://www.imdb.com/Title?", "\"");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, imdbId);
            }
            
            if (OverrideTools.checkOverwriteTitle(movie, OFDB_PLUGIN_ID)) {
                String titleShort = HTMLTools.extractTag(xml, "<title>OFDb -", "</title>");
                if (titleShort.indexOf("(") > 0) {
                    // strip year from title
                    titleShort = titleShort.substring(0, titleShort.lastIndexOf("(")).trim();
                }
                movie.setTitle(titleShort, OFDB_PLUGIN_ID);
            }

            // scrape plot and outline
            String plotMarker = HTMLTools.extractTag(xml, "<a href=\"plot/", 0, "\"");
            if (StringTools.isValidString(plotMarker) && OverrideTools.checkOneOverwrite(movie, OFDB_PLUGIN_ID, OverrideFlag.PLOT, OverrideFlag.OUTLINE)) {
                try {
                    String plotXml = webBrowser.request("http://www.ofdb.de/plot/" + plotMarker);

                    int firstindex = plotXml.indexOf("gelesen</b></b><br><br>") + 23;
                    int lastindex = plotXml.indexOf("</font>", firstindex);
                    String plot = plotXml.substring(firstindex, lastindex);
                    plot = plot.replaceAll("<br />", " ");
    
                    if (OverrideTools.checkOverwritePlot(movie, OFDB_PLUGIN_ID)) {
                        movie.setPlot(plot, OFDB_PLUGIN_ID);
                    }
    
                    if (OverrideTools.checkOverwriteOutline(movie, OFDB_PLUGIN_ID)) {
                        movie.setOutline(plot, OFDB_PLUGIN_ID);
                    }
                } catch (Exception error) {
                    LOGGER.error(SystemTools.getStackTrace(error));
                    returnValue = Boolean.FALSE;
                }
            }

            // scrape additional infos
            int beginIndex = xml.indexOf("view.php?page=film_detail");
            if (beginIndex != -1) {
                try {
                    String detailUrl = "http://www.ofdb.de/" + xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
                    String detailXml = webBrowser.request(detailUrl);

                    // resolve for additional informations
                    List<String> tags = HTMLTools.extractHtmlTags(detailXml, "<!-- Rechte Spalte -->", "</table>", "<tr", "</tr>");

                    for (String tag : tags)  {
                        if (OverrideTools.checkOverwriteOriginalTitle(movie, OFDB_PLUGIN_ID) && tag.contains("Originaltitel")) {
                            String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", "</font>")).trim();
                            movie.setOriginalTitle(scraped, OFDB_PLUGIN_ID);
                        }
                        
                        if (OverrideTools.checkOverwriteYear(movie, OFDB_PLUGIN_ID) && tag.contains("Erscheinungsjahr")) {
                            String scraped = HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", "</font>")).trim();
                            movie.setYear(scraped, OFDB_PLUGIN_ID);
                        }
                        
                        if (OverrideTools.checkOverwriteCountry(movie, OFDB_PLUGIN_ID) && tag.contains("Herstellungsland")) {
                            List<String> scraped = HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>");
                            if (scraped.size() > 0) {
                                // TODO set more countries in movie
                                movie.setCountry(HTMLTools.removeHtmlTags(scraped.get(0)).trim(), OFDB_PLUGIN_ID);
                            }
                        }

                        if (OverrideTools.checkOverwriteGenres(movie, OFDB_PLUGIN_ID) && tag.contains("Genre(s)")) {
                            List<String> scraped = HTMLTools.extractHtmlTags(tag, "class=\"Daten\"", "</td>", "<a", "</a>");
                            List<String> genres = new ArrayList<String>();
                            for (String genre : scraped) {
                                genres.add(HTMLTools.removeHtmlTags(genre).trim());
                            }
                            movie.setGenres(genres, OFDB_PLUGIN_ID);
                        }
                    }

                    // flags for overrides
                    boolean overrideNormal;
                    boolean overridePeople;

                    if (detailXml.contains("<i>Regie</i>")) {
                        overrideNormal = OverrideTools.checkOverwriteDirectors(movie, OFDB_PLUGIN_ID);
                        overridePeople = OverrideTools.checkOverwritePeopleDirectors(movie, OFDB_PLUGIN_ID);
                        if (overrideNormal || overridePeople) {
                            tags = HTMLTools.extractHtmlTags(detailXml, "<i>Regie</i>", "</table>", "<tr", "</tr>");
                            List<String> directors = new ArrayList<String>();
                            for (String tag : tags)  {
                                directors.add(HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", "</font>")).trim());
                            }
                            
                            if (overrideNormal) {
                                movie.setDirectors(directors, OFDB_PLUGIN_ID);
                            }
                            if (overridePeople) {
                                movie.setPeopleDirectors(directors, OFDB_PLUGIN_ID);
                            }
                        }
                    }

                    if (detailXml.contains("<i>Drehbuchautor(in)</i>")) {
                        overrideNormal = OverrideTools.checkOverwriteWriters(movie, OFDB_PLUGIN_ID);
                        overridePeople = OverrideTools.checkOverwritePeopleWriters(movie, OFDB_PLUGIN_ID);
                        if (overrideNormal || overridePeople) {
                            tags = HTMLTools.extractHtmlTags(detailXml, "<i>Drehbuchautor(in)</i>", "</table>", "<tr", "</tr>");
                            List<String> writers = new ArrayList<String>();
                            for (String tag : tags)  {
                                writers.add(HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", "</font>")).trim());
                            }

                            if (overrideNormal) {
                                movie.setWriters(writers, OFDB_PLUGIN_ID);
                            }
                            if (overridePeople) {
                                movie.setPeopleWriters(writers, OFDB_PLUGIN_ID);
                            }
                        }
                    }

                    if (detailXml.contains("<i>Darsteller</i>")) {
                        overrideNormal = OverrideTools.checkOverwriteActors(movie, OFDB_PLUGIN_ID);
                        overridePeople = OverrideTools.checkOverwritePeopleActors(movie, OFDB_PLUGIN_ID);
                        if (overrideNormal || overridePeople) {
                            tags = HTMLTools.extractHtmlTags(detailXml, "<i>Darsteller</i>", "</table>", "<tr", "</tr>");
                            List<String> cast = new ArrayList<String>();
                            for (String tag : tags)  {
                                cast.add(HTMLTools.removeHtmlTags(HTMLTools.extractTag(tag, "class=\"Daten\">", "</font>")).trim());
                            }
                            
                            if (overrideNormal) {
                                movie.setCast(cast, OFDB_PLUGIN_ID);
                            }
                            if (overridePeople) {
                                movie.setPeopleCast(cast, OFDB_PLUGIN_ID);
                            }
                        }
                    }

                } catch (Exception error) {
                    LOGGER.error(SystemTools.getStackTrace(error));
                    returnValue = Boolean.FALSE;                    
                }
            }
        } catch (Exception error) {
            LOGGER.error(SystemTools.getStackTrace(error));
            returnValue = Boolean.FALSE;
        }
        return returnValue;
    }

    public String getOfdbId(Movie movie) {
        String ofdbId = movie.getId(OFDB_PLUGIN_ID);
        if (StringTools.isNotValidString(ofdbId)) {
            // find by IMDb id
            String imdbId = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
            if (StringTools.isValidString(imdbId)) {
                // if IMDb id is present then use this
                ofdbId = getOfdbIdByImdbId(imdbId);
            }
            if (StringTools.isNotValidString(ofdbId)) {
                // try by title and year
                ofdbId = getObdbIdByTitleAndYear(movie.getTitle(), movie.getYear());
            }
            if (StringTools.isNotValidString(ofdbId)) {
                // try on google
                ofdbId = getOfdbIdFromGoogle(movie.getTitle(), movie.getYear());
            }
            movie.setId(OFDB_PLUGIN_ID, ofdbId);
        }
        return ofdbId;
    }

    private String getOfdbIdByImdbId(String imdbId) {
        try {
            String xml = webBrowser.request("http://www.ofdb.de/view.php?page=suchergebnis&SText=" + imdbId + "&Kat=IMDb");
            
            int beginIndex = xml.indexOf("Ergebnis der Suchanfrage");
            if (beginIndex < 0) {
                return Movie.UNKNOWN;
            }

            beginIndex = xml.indexOf("film/", beginIndex);
            if (beginIndex != -1) {
                StringBuilder sb = new StringBuilder();
                sb.append("http://www.ofdb.de/");
                sb.append(xml.substring(beginIndex, xml.indexOf("\"", beginIndex+1)));
                return sb.toString();
            }
            
        } catch (IOException error) {
            LOGGER.error(LOG_MESSAGE + "Failed retreiving OFDb url for IMDb id: " + imdbId);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    private String getObdbIdByTitleAndYear(String title, String year) {
        if (StringTools.isNotValidString(year)) {
            // title and year must be present for successful OFDb advanced search
            // expected are 2 search parameters minimum; so skip here if year is not valid
            return Movie.UNKNOWN;
        }
        
        try {
            StringBuilder sb = new StringBuilder("http://www.ofdb.de/view.php?page=fsuche&Typ=N&AB=-&Titel=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&Genre=-&HLand=-&Jahr=");
            sb.append(year);
            sb.append("&Wo=-&Land=-&Freigabe=-&Cut=A&Indiziert=A&Submit2=Suche+ausf%C3%BChren");

            String xml = webBrowser.request(sb.toString());

            int beginIndex = xml.indexOf("Liste der gefundenen Fassungen");
            if (beginIndex < 0) {
                return Movie.UNKNOWN;
            }
            
            beginIndex = xml.indexOf("href=\"film/",beginIndex);
            if (beginIndex < 0) {
                return Movie.UNKNOWN;
            }

            sb.setLength(0);
            sb.append("http://www.ofdb.de/");
            sb.append(xml.substring(beginIndex+6, xml.indexOf("\"", beginIndex+10)));
            return sb.toString();
            
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving OFDb url by search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }
    
    private String getOfdbIdFromGoogle(String title, String year) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.de/search?hl=de&q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append("+site%3Awww.ofdb.de/film");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("http://www.ofdb.de/film");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"");
                return st.nextToken();
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving OFDb url by google : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        LOGGER.debug(LOG_MESSAGE + "Scanning NFO for Imdb Id");
        int beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$<>");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            LOGGER.debug(LOG_MESSAGE + "Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$<>");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
            } else {
                LOGGER.debug(LOG_MESSAGE + "No Imdb Id found in nfo");
            }
        }
        boolean result = false;
        beginIndex = nfo.indexOf("http://www.ofdb.de/film/");

        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex), " \n\t\r\f!&é\"'(èçà)=$<>");
            movie.setId(OfdbPlugin.OFDB_PLUGIN_ID, st.nextToken());
            LOGGER.debug(LOG_MESSAGE + "Ofdb Id found in nfo = " + movie.getId(OfdbPlugin.OFDB_PLUGIN_ID));
            result = true;
        } else {
            LOGGER.debug(LOG_MESSAGE + "No Ofdb Id found in nfo");
        }
        return result;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        imdbp.scanTVShowTitles(movie);
    }

    @Override
    public boolean scan(Person person) {
        return false;
    }
}