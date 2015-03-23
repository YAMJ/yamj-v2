/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.scriptablescraper.ItemSS;
import com.moviejukebox.model.scriptablescraper.MathSS;
import com.moviejukebox.model.scriptablescraper.ParseSS;
import com.moviejukebox.model.scriptablescraper.RetrieveSS;
import com.moviejukebox.model.scriptablescraper.ScriptableScraper;
import com.moviejukebox.model.scriptablescraper.SectionContentSS;
import com.moviejukebox.model.scriptablescraper.SectionSS;
import com.moviejukebox.reader.ScriptableScraperXMLReader;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin to retrieve movie data based on ScriptableScraper XML file
 */
public class ScriptableScraperPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptableScraperPlugin.class);
    public static final String SCRIPTABLESCRAPER_PLUGIN_ID = "scriptablescraper";
    private final boolean debug = PropertiesUtil.getBooleanProperty(SCRIPTABLESCRAPER_PLUGIN_ID + ".debug", Boolean.FALSE);
    private final boolean info = PropertiesUtil.getBooleanProperty(SCRIPTABLESCRAPER_PLUGIN_ID + ".info", Boolean.TRUE);
//    private final TheTvDBPlugin tvdb;
    private ScriptableScraper ssData;
    private final int maxGenres;
    private final int maxDirectors;
    private final int maxWriters;
    private final int maxActors;
    // Constants
    private static final int DEFAULT_GENRES = 9;
    private static final int DEFAULT_ACTORS = 10;
    private static final int DEFAULT_WRITERS = 3;
    private static final int DEFAULT_DIRECTORS = 2;

    public ScriptableScraperPlugin() {
        super();

        maxGenres = PropertiesUtil.getIntProperty("genres.max", DEFAULT_GENRES);
        maxDirectors = PropertiesUtil.getReplacedIntProperty("movie.director.maxCount", "plugin.people.maxCount.director", DEFAULT_DIRECTORS);
        maxWriters = PropertiesUtil.getReplacedIntProperty("movie.writer.maxCount", "plugin.people.maxCount.writer", DEFAULT_WRITERS);
        maxActors = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", DEFAULT_ACTORS);

//        tvdb = new TheTvDBPlugin();
        final ScriptableScraperXMLReader xmlReader = new ScriptableScraperXMLReader();
        String xmlData = PropertiesUtil.getProperty("scriptablescraper.data", Movie.UNKNOWN);
        if (StringTools.isValidString(xmlData)) {
            File xmlFile = new File(xmlData);
            if (xmlFile.exists()) {
                ssData = new ScriptableScraper();
                ssData.setDebug(debug);
                if (xmlReader.parseXML(xmlFile, ssData)) {
                    if (info) {
                        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
                        StackTraceElement e = stacktrace[stacktrace.length - 8];
                        if ("updateMovieData".equals(e.getMethodName())) {
                            LOG.info("");
                            LOG.info("  -=[   ScriptableScraperPlugin   ]=-");
                            LOG.info("Plugin         : {} - {}", ssData.getName(), ssData.getDescription());
                            LOG.info("Plugin version : {} ({})", ssData.getVersion(), ssData.getPublished());
                            LOG.info("Plugin authors : {}", ssData.getAuthor());
                            LOG.info("Plugin language: {}", ssData.getLanguage());
                            LOG.info("");
                        }
                    }
                } else {
                    LOG.error("Reading error XML data file : {}", xmlData);
                }
            } else {
                LOG.error("File not found : {}", xmlData);
            }
        } else {
            LOG.error("Failed mandatory parameter scriptablescraper.data");
        }
    }

    @Override
    public String getPluginID() {
        return SCRIPTABLESCRAPER_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = false;
        String movieId = mediaFile.getId(SCRIPTABLESCRAPER_PLUGIN_ID);

        if (StringTools.isNotValidString(movieId)) {
            /*
             // Get base info from imdb or tvdb
             if (!mediaFile.isTVShow()) {
             super.scan(mediaFile);
             } else {
             tvdb.scan(mediaFile);
             }
             */

            movieId = getMovieId(mediaFile);
            if (debug) {
                LOG.debug("scan: movieId: {}", movieId);
            }
            mediaFile.setId(SCRIPTABLESCRAPER_PLUGIN_ID, movieId);
        }

        if (StringTools.isValidString(movieId)) {
            retval = updateMediaInfo(mediaFile, movieId);
        }

        return retval;
    }

    public String getMovieId(Movie movie) {
        String movieId = movie.getId(SCRIPTABLESCRAPER_PLUGIN_ID);

        if (StringTools.isNotValidString(movieId)) {
            movieId = getMovieId(movie.getOriginalTitle(), movie.getYear(), movie.getSeason());
        }

        return movieId;
    }

    public String getMovieId(String movieName, String year, int season) {
        String movieId = Movie.UNKNOWN;

        ssData.getSection().setVariable("search.title", movieName);
        ssData.getSection().setVariable("search.year", StringTools.isValidString(year) ? year : "0");

        Collection<SectionContentSS> dataSections = ssData.getSections("action", "search");
        runSections(dataSections, "search");
        for (SectionContentSS section : dataSections) {
            movieId = section.getVariable("movie[0].site_id");
            if (StringTools.isValidString(movieId)) {
                ssData.getSection().setVariable("movie.site_id", movieId);
                break;
            }
        }

        if (StringTools.isValidString(movieId)) {
            return movieId;
        } else {
            return Movie.UNKNOWN;
        }
    }

    private boolean updateMediaInfo(Movie movie, String movieId) {
        Collection<SectionContentSS> sections = ssData.getSections("action", "get_details");
        if (!sections.isEmpty()) {
            runSections(sections, "get_details");

            SectionContentSS section = sections.iterator().next();

            // Title
            String value = section.getVariable("movie.title");
            if (StringTools.isValidString(value)) {
                movie.setTitle(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Year
            value = section.getVariable("movie.year");
            if (StringTools.isValidString(value)) {
                movie.setYear(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Plot
            value = section.getVariable("movie.summary");
            if (StringTools.isValidString(value)) {
                movie.setPlot(value, SCRIPTABLESCRAPER_PLUGIN_ID);
                movie.setOutline(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Genres
            value = section.getVariable("movie.genres");
            if (StringTools.isValidString(value)) {
                List<String> newGenres = new LinkedList<>();
                for (String genre : Arrays.asList(value.split("\\|"))) {
                    newGenres.add(genre);
                    if (newGenres.size() == maxGenres) {
                        break;
                    }
                }

                movie.setGenres(newGenres, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Rating
            value = section.getVariable("movie.score");
            if (StringTools.isValidString(value)) {
                movie.addRating(SCRIPTABLESCRAPER_PLUGIN_ID, StringTools.parseRating(NumberUtils.toFloat(value.replace(',', '.'), -1) / 10));
            }

            // Top 250
            value = section.getVariable("movie.top250");
            if (StringTools.isValidString(value)) {
                movie.setTop250(value, IMDB_PLUGIN_ID);
            }

            // Director
            updatePersonInfo(movie, "directors", section.getVariable("movie.directors"), maxDirectors);

            // Writer
            updatePersonInfo(movie, "writers", section.getVariable("movie.writers"), maxWriters);

            // Actors
            updatePersonInfo(movie, "actors", section.getVariable("movie.actors"), maxActors);

            // Studio/Company
            value = section.getVariable("movie.studios");
            if (StringTools.isValidString(value)) {
                movie.setCompany(value.replaceAll("\\|", Movie.SPACE_SLASH_SPACE), SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Run time
            value = section.getVariable("movie.runtime");
            if (StringTools.isValidString(value)) {
                movie.setRuntime(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Tagline
            value = section.getVariable("movie.tagline");
            if (StringTools.isValidString(value)) {
                movie.setTagline(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Certification
            value = section.getVariable("movie.certification");
            if (StringTools.isValidString(value)) {
                movie.setCertification(value, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Country
            value = section.getVariable("movie.country");
            if (StringTools.isValidString(value)) {
                List<String> countries = new ArrayList<>();
                for (String country : Arrays.asList(value.split("\\|"))) {
                    countries.add(country);
                }
                movie.setCountries(countries, SCRIPTABLESCRAPER_PLUGIN_ID);
            }

            // Quotes
            value = section.getVariable("movie.quotes");
            if (StringTools.isValidString(value)) {
                for (String quote : Arrays.asList(value.split("\\|"))) {
                    movie.setQuote(cleanStringEnding(quote), SCRIPTABLESCRAPER_PLUGIN_ID);
                    break;
                }
            }

            // Did you know
            value = section.getVariable("movie.didyouknow");
            if (StringTools.isValidString(value)) {
                for (String dyk : Arrays.asList(value.split("\\|"))) {
                    movie.addDidYouKnow(dyk);
                }
            }

            return true;
        }

        return false;
    }

    private int updatePersonInfo(Movie movie, String mode, String value, int personMax) {
        int count = 0;
        if (StringTools.isValidString(value)) {
            for (String person : Arrays.asList(value.split("\\|"))) {
                if (null != mode) {
                    switch (mode) {
                        case "directors":
                            movie.addDirector(person, SCRIPTABLESCRAPER_PLUGIN_ID);
                            break;
                        case "writers":
                            movie.addWriter(person, SCRIPTABLESCRAPER_PLUGIN_ID);
                            break;
                        case "actors":
                            movie.addActor(person, SCRIPTABLESCRAPER_PLUGIN_ID);
                            break;
                    }
                }
                count++;
                if (personMax == count) {
                    break;
                }
            }
        }
        return count;
    }

    private void runSections(Collection<SectionContentSS> sections, String sectionName) {
        try {
            if (debug) {
                LOG.debug("runSections: {}", sectionName);
            }
            String value;
//            Collection<ScriptableScraper.SectionContentSS> result = null;
            for (SectionContentSS content : sections) {
                SectionSS cSection = (SectionSS) content;
                for (int looperItem = 0; looperItem < cSection.getItems().size(); looperItem++) {
                    ItemSS item = cSection.getItem(looperItem);
                    String type = item.getType();
                    String key = item.getKey();
                    if (debug) {
                        LOG.debug("item: {} : {}", type, key);
                    }
                    if ("retrieve".equals(type)) {
                        RetrieveSS retrieve = cSection.getRetrieve(key);
                        if (retrieve != null) {
                            String url = cSection.compileValue(retrieve.getURL());
                            if (StringUtils.isNotBlank(retrieve.getCookies())) {
                                String domain = new URL(url).getHost();
                                List<String> values;
                                for (String cookie : Arrays.asList(retrieve.getCookies().split("&"))) {
                                    values = Arrays.asList(cookie.split("="));
                                    if (debug) {
                                        LOG.debug("retrieve page from domain '{}' with name '{}' and value'{}'", domain, values.get(0), values.get(1));
                                    }
                                    webBrowser.putCookie(domain, values.get(0), values.get(1));
                                }
                            }
                            if (StringTools.isValidString(url)) {
                                String page = "";
                                for (int looper = 0; looper <= retrieve.getRetries(); looper++) {
                                    page = webBrowser.request(url, retrieve.getEncoding()).replaceAll("\\r", "").replaceAll("\\n", " ");
                                    if (StringTools.isValidString(page)) {
                                        break;
                                    }
                                }
                                if (StringTools.isNotValidString(page)) {
                                    LOG.error("Page does not retrieved for '{}' with URL {}", key, url);
                                    page = "";
                                }
                                cSection.setGlobalVariable(cSection.compileValue(key), page);
                            }
                        }
                    } else if ("set".equals(type)) {
                        value = cSection.getSet(key);
                        if (debug) {
                            LOG.debug("getSet: key: {} value: {}", key, value);
                        }
                        key = cSection.compileValue(key);
                        value = cSection.compileValue(value);
                        cSection.setGlobalVariable(key, value);
                    } else if ("parse".equals(type)) {
                        ParseSS parse = cSection.getParse(key);
                        if (parse != null) {
                            key = cSection.compileValue(key);
                            value = cSection.parseInput(cSection.compileValue(parse.getInput()), cSection.compileValue(parse.getRegex()));
                            cSection.setGlobalVariable(key, value);
                        }
                    } else if ("math".equals(type)) {
                        MathSS math = cSection.getMath(key);
                        if (math != null) {
                            boolean found = false;
                            float res = -0.000001f;
                            float value1 = Float.parseFloat(cSection.compileValue(math.getValue1()));
                            float value2 = Float.parseFloat(cSection.compileValue(math.getValue2()));
                            String typeName = math.getType();
                            if (null != typeName) {
                                switch (typeName) {
                                    case "add":
                                        res = value1 + value2;
                                        found = true;
                                        break;
                                    case "subtract":
                                        res = value1 - value2;
                                        found = true;
                                        break;
                                    case "multiply":
                                        res = value1 * value2;
                                        found = true;
                                        break;
                                    case "divide":
                                        if (value2 > 0f) {
                                            res = value1 / value2;
                                        } else {
                                            res = 0f;
                                        }
                                        found = true;
                                        break;
                                    default:
                                        LOG.error("Unknown math type: {}", typeName);
                                        break;
                                }
                            }

                            if (found) {
                                if ("float".equals(math.getResultType())) {
                                    value = Float.toString(res);
                                } else {
                                    value = Integer.toString(Math.round(res));
                                }
                                cSection.setGlobalVariable(cSection.compileValue(key), value);
                            }
                        }
                    } else if ("content".equals(type)) {
                        SectionSS section = (SectionSS) cSection.getContent(Integer.parseInt(key));
                        String name = section.getName();
                        if (debug) {
                            LOG.debug("subsection: {}", name);
                        }
                        if ("if".equals(name)) {
                            String condition = section.getAttribute("test");
                            if (StringTools.isValidString(condition) && section.testCondition(condition)) {
                                runSections((Collection) Arrays.asList(section), sectionName);
                            }
                        } else if ("loop".equals(name)) {
                            String sName = section.getAttribute("name");
                            value = section.getAttribute("on");
                            if (StringTools.isValidString(sName) && StringTools.isValidString(value)) {
                                if (section.hasVariable(value)) {
                                    value = section.getVariable(value);
                                    if (debug) {
                                        LOG.debug("loop: value: {}", value);
                                    }
                                    if (StringTools.isValidString(value)) {
                                        List<String> values = Arrays.asList(value.split(ScriptableScraper.ARRAY_GROUP_DIVIDER));
                                        int limit = values.size();

                                        value = section.getAttribute("limit");
                                        if (StringTools.isValidString(value) && (limit > Integer.parseInt(value))) {
                                            limit = Integer.parseInt(value);
                                        }

                                        if (limit > 0) {
                                            for (int looper = 0; looper < limit; looper++) {
                                                if (debug) {
                                                    LOG.debug("loop: {}: {}", sName, values.get(looper));
                                                }
                                                section.setVariable(sName, values.get(looper));
                                                section.setVariable("count", Integer.toString(looper));
                                                runSections((Collection) Arrays.asList(section), sectionName);
                                            }
                                        }
                                    }
                                } else {
                                    LOG.error("Does not exist '{}' for 'loop' name=\"{}\"", section.getAttribute("on"), sName);
                                }
                            } else {
                                LOG.error("Wrong attribute 'on' value '{}' of 'loop' name=\"{}\"", section.getAttribute("on"), sName);
                            }
                        }
                    }
                }
            }
        } catch (IOException error) {
            LOG.error("Failed run section : {}", sectionName);
            LOG.error("Error : {}", error.getMessage());
        }
    }
}
