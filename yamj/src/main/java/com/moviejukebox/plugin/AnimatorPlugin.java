/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
import com.moviejukebox.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin to retrieve movie data from Russian animation database www.animator.ru and www.allmults.org
 *
 * Written by Ilgizar Mubassarov (based on KinopoiskPlugin.java)
 *
 * animator.sites: [all, animator, allmults] - where find movie data
 *
 * @author ilgizar
 */
public class AnimatorPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AnimatorPlugin.class);
    public static final String ANIMATOR_PLUGIN_ID = "animator";
    private final String preferredSites = PropertiesUtil.getProperty("animator.sites", "all");
    private final String[] listSites = preferredSites.split(",");
    private final boolean animatorDiscovery = ("all".equals(preferredSites) || ArrayUtils.indexOf(listSites, "animator") != -1);
    private final boolean multsDiscovery = ("all".equals(preferredSites) || ArrayUtils.indexOf(listSites, "allmults") != -1);

    // Literals
    private static final String HTML_HREF = "<a href=";
    private static final String HTML_TD = "</td>";
    private static final String HTML_MIDDLE_LINKS = "<span class=\"MiddleLinks\"";
    private static final String HTML_TR = "</tr>";
    private static final String HTML_SPAN = "</span>";

    @Override
    public String getPluginID() {
        return ANIMATOR_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = false;
        String animatorId = mediaFile.getId(ANIMATOR_PLUGIN_ID);
        if (StringTools.isNotValidString(animatorId)) {
            String year = mediaFile.getYear();
            // Let's replace dash (-) by space ( ) in Title.
            String name = mediaFile.getTitle();
            name = name.replace('-', ' ');
            animatorId = getAnimatorId(name, year);

            if (StringTools.isValidString(year) && StringTools.isNotValidString(animatorId)) {
                // Trying without specifying the year
                animatorId = getAnimatorId(name, Movie.UNKNOWN);
            }
            mediaFile.setId(ANIMATOR_PLUGIN_ID, animatorId);
        }

        if (StringTools.isValidString(animatorId)) {
            retval = updateAnimatorMediaInfo(mediaFile, animatorId);
        }
        return retval;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean result = false;
        LOG.debug("Scanning NFO for Animator Id");
        int beginIndex = nfo.indexOf("animator.ru/db/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 33), "");
            movie.setId(AnimatorPlugin.ANIMATOR_PLUGIN_ID, st.nextToken());
            LOG.debug("Animator Id found in nfo = {}", movie.getId(AnimatorPlugin.ANIMATOR_PLUGIN_ID));
            result = true;
        } else {
            LOG.debug("No Animator Id found in nfo !");
        }
        super.scanNFO(nfo, movie);
        return result;
    }

    /**
     * Retrieve Animator matching the specified movie name and year.
     *
     * This routine is base on a Google request.
     */
    private String getAnimatorId(String movieName, String year) {
        try {
            String animatorId = Movie.UNKNOWN;
            String allmultsId = Movie.UNKNOWN;

            String sb = movieName;
            // Unaccenting letters
            sb = Normalizer.normalize(sb, Normalizer.Form.NFD);
            // Return simple letters 'й' & 'Й'
            sb = sb.replaceAll("И" + (char) 774, "Й");
            sb = sb.replaceAll("и" + (char) 774, "й");
            sb = sb.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            sb = "text=" + URLEncoder.encode(sb, "Cp1251").replace(" ", "+");

            // Get ID from animator.ru
            if (animatorDiscovery) {
                String uri = "http://www.animator.ru/db/?p=search&SearchMask=1&" + sb;
                if (StringTools.isValidString(year)) {
                    uri = uri + "&year0=" + year;
                    uri = uri + "&year1=" + year;
                }
                String xml = httpClient.request(uri);
                // Checking for zero results
                if (xml.contains("[соответствие фразы]")) {
                    // It's search results page, searching a link to the movie page
                    int beginIndex;
                    if (-1 != xml.indexOf("Найдено ")) {
                        for (String tmp : HTMLTools.extractTags(xml, "Найдено ", HTML_TD, HTML_HREF, "<br><br>")) {
                            if (0 < tmp.indexOf("[соответствие фразы]")) {
                                beginIndex = tmp.indexOf(" г.)");
                                if (beginIndex >= 0) {
                                    String year2 = tmp.substring(beginIndex - 4, beginIndex);
                                    if (year2.equals(year)) {
                                        beginIndex = tmp.indexOf("http://www.animator.ru/db/?p=show_film&fid=", beginIndex);
                                        if (beginIndex >= 0) {
                                            StringTokenizer st = new StringTokenizer(tmp.substring(beginIndex + 43), " ");
                                            animatorId = st.nextToken();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Get ID from allmults.org
            if (multsDiscovery) {
                URL url = new URL("http://allmults.org/search.php");
                URLConnection conn = url.openConnection(YamjHttpClientBuilder.getProxy());
                conn.setDoOutput(true);

                OutputStreamWriter osWriter = null;
                StringBuilder xmlLines = new StringBuilder();

                try {
                    osWriter = new OutputStreamWriter(conn.getOutputStream());
                    osWriter.write(sb);
                    osWriter.flush();

                    try (InputStreamReader inReader = new InputStreamReader(conn.getInputStream(), "cp1251");
                            BufferedReader bReader = new BufferedReader(inReader)) {
                        String line;
                        while ((line = bReader.readLine()) != null) {
                            xmlLines.append(line);
                        }
                    }

                    osWriter.flush();
                } finally {
                    if (osWriter != null) {
                        osWriter.close();
                    }
                }

                if (xmlLines.indexOf("<div class=\"post\"") != -1) {
                    for (String tmp : HTMLTools.extractTags(xmlLines.toString(), "По Вашему запросу найдено ", "<ul><li>", "<div class=\"entry\"", "</div>")) {
                        int pos = tmp.indexOf("<img ");
                        if (pos != -1) {
                            int temp = tmp.indexOf(" alt=\"");
                            if (temp != -1) {
                                String year2 = tmp.substring(temp + 6, tmp.indexOf("\"", temp + 6) - 1);
                                year2 = year2.substring(year2.length() - 4);
                                if (year2.equals(year)) {
                                    temp = tmp.indexOf(" src=\"/images/multiki/");
                                    if (temp != -1) {
                                        allmultsId = tmp.substring(temp + 22, tmp.indexOf(".jpg", temp + 22));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return (animatorId.equals(Movie.UNKNOWN) && allmultsId.equals(Movie.UNKNOWN)) ? Movie.UNKNOWN : animatorId + ":" + allmultsId;
        } catch (IOException error) {
            LOG.error("Failed retreiving Animator Id for movie : {}", movieName);
            LOG.error("Error : {}", error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan Animator html page for the specified movie
     */
    private boolean updateAnimatorMediaInfo(Movie movie, String animatorId) {
        try {
            String[] listID = animatorId.split(":");
            String newAnimatorId = listID[0];   // Get the first one
            String allmultsId = listID[1];

            String xml = "";
            String xml2 = "";
            if (!newAnimatorId.equals(Movie.UNKNOWN)) {
                xml = "http://www.animator.ru/db/?p=show_film&fid=" + newAnimatorId;
                //logger.log(Level.SEVERE, "ANIMATOR URL: " + xml);
                xml = httpClient.request(xml);
            }
            if (!allmultsId.equals(Movie.UNKNOWN)) {
                xml2 = "http://allmults.org/multik.php?id=" + allmultsId;
                //logger.log(Level.SEVERE, "ALLMULTS URL: " + xml2);
                xml2 = httpClient.request(xml2);
            }

            // Work-around for issue #649
            xml = xml.replace("&#133;", "&hellip;");
            xml = xml.replace("&#151;", "&mdash;");
            xml2 = xml2.replace("&#133;", "&hellip;");
            xml2 = xml2.replace("&#151;", "&mdash;");

            if (OverrideTools.checkOverwriteTitle(movie, ANIMATOR_PLUGIN_ID)) {
                String title = Movie.UNKNOWN;

                if (!newAnimatorId.equals(Movie.UNKNOWN)) {
                    // Title (animator.ru)
                    for (String tit : HTMLTools.extractTags(xml, "<td align=\"left\" class=\"FilmName\">", "</B>", "«", "»")) {
                        title = tit;
                        break;
                    }
                } else {
                    // Title (allmults.org)
                    for (String tit : HTMLTools.extractTags(xml2, "<b>Название:", "<b>", "", "<br>")) {
                        title = tit;
                        break;
                    }
                }

                movie.setTitle(title, ANIMATOR_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwritePlot(movie, ANIMATOR_PLUGIN_ID)) {
                StringBuilder plot = new StringBuilder();
                // Plot (animator.ru)
                if (!newAnimatorId.equals(Movie.UNKNOWN)) {
                    for (String subPlot : HTMLTools.extractTags(xml, "<td align=\"left\" class=\"FilmComments\"", HTML_TD)) {
                        if (!subPlot.isEmpty()) {
                            if (plot.length() > 0) {
                                plot.append(" ");
                            }
                            plot.append(subPlot);
                        }
                    }

                    for (String subPlot : HTMLTools.extractTags(xml, "<td class=\"FilmComments\">", HTML_TD)) {
                        if (!subPlot.isEmpty()) {
                            if (plot.length() > 0) {
                                plot.append(" ");
                            }
                            plot.append(subPlot);
                        }
                    }
                }
                // Plot (allmults.org)
                if (plot.length() == 0 && !allmultsId.equals(Movie.UNKNOWN)) {
                    for (String subPlot : HTMLTools.extractTags(xml2, "Описание:", "<p class=\"postinfo\">", "<br", "</p>")) {
                        if (!subPlot.isEmpty()) {
                            if (plot.length() > 0) {
                                plot.append(" ");
                            }
                            plot.append(subPlot);
                        }
                    }
                }
                String newPlot = "";
                if (plot.length() > 0) {
                    newPlot = plot.toString();
                    newPlot = newPlot.replace("<br>", " ");
                    newPlot = newPlot.replace(HTML_SPAN, "");
                }

                movie.setPlot(newPlot, ANIMATOR_PLUGIN_ID);
            }

            // Genre + Run time (animator.ru)
            // String MultType = "";
            String time = Movie.UNKNOWN;
            List<String> newGenres = new LinkedList<>();
            newGenres.add(0, "мультфильм");
            for (String tmpTagList : HTMLTools.extractTags(xml, "class=\"FilmType\"><i", HTML_TD, "", "</i>")) {
                for (String tmpTag : tmpTagList.split(",")) {
                    if (StringUtils.isNotBlank(tmpTag)) {
                        if (tmpTag.contains(" мин.")) {
                            time = tmpTag.substring(0, tmpTag.indexOf(" мин.") + 4);
                            break;
                        }
                        newGenres.add(tmpTag.replaceAll("^\\s+", "").replaceAll("\\s+$", ""));
                    }
                }
            }

            if (OverrideTools.checkOverwriteGenres(movie, ANIMATOR_PLUGIN_ID)) {
                // Genre (allmults.org)
                if (newGenres.isEmpty() && !allmultsId.equals(Movie.UNKNOWN)) {
                    for (String tmpGenreList : HTMLTools.extractTags(xml2, "<b>Жанр:", "<b>", "", "<br>")) {
                        for (String genre : tmpGenreList.split(",")) {
                            if (StringUtils.isNotBlank(genre)) {
                                newGenres.add(genre);
                            }
                        }
                    }
                }
                movie.setGenres(newGenres, ANIMATOR_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, ANIMATOR_PLUGIN_ID)) {
                // Run time (allmults.org)
                if (time.equals(Movie.UNKNOWN) && !allmultsId.equals(Movie.UNKNOWN)) {
                    for (String tmp : HTMLTools.extractTags(xml2, "<b>Продолжительность:", "<b>", "", "<br>")) {
                        time = tmp;
                        break;
                    }
                }
                movie.setRuntime(time, ANIMATOR_PLUGIN_ID);
            }

            Collection<String> newDirectors = new ArrayList<>();
            Collection<String> newWriters = new ArrayList<>();
            Collection<String> newCast = new ArrayList<>();
            // Director, Writers, Cast (animator.ru)
            for (String item : HTMLTools.extractTags(xml, "<table cellpadding=0 cellspacing=0 width=380", "</table>", "<tr>", HTML_TR)) {
                item = "<td>" + item + HTML_TR;
                // Director
                for (String tmp : HTMLTools.extractTags(item, ">режиссер", HTML_TR, "<td class=\"PersonsList\"", HTML_TD)) {
                    for (String writer : HTMLTools.extractTags(tmp, HTML_MIDDLE_LINKS, HTML_SPAN)) {
                        newDirectors.add(writer);
                    }
                }

                // Writers
                for (String tmpWriter : HTMLTools.extractTags(item, ">сценарист", HTML_TR, "<td class=\"PersonsList\"", HTML_TD)) {
                    for (String writer : HTMLTools.extractTags(tmpWriter, HTML_MIDDLE_LINKS, HTML_SPAN)) {
                        newWriters.add(writer);
                    }
                }

                // Cast
                for (String actor : HTMLTools.extractTags(item, ">роли озвучивали", HTML_TR, "<nobr", "</nobr>")) {
                    String act = "";
                    for (String tmpAct : HTMLTools.extractTags(actor, HTML_MIDDLE_LINKS, HTML_SPAN)) {
                        act = tmpAct;
                        break;
                    }
                    for (String tmpPers : HTMLTools.extractTags(actor, "</span> (", ")")) {
                        act = act + " (" + tmpPers + ")";
                        break;
                    }
                    if (StringUtils.isNotBlank(act)) {
                        newCast.add(act);
                    }
                }
                for (String actor : HTMLTools.extractTags(item, ">роли озвучивали", HTML_TR, HTML_MIDDLE_LINKS, HTML_SPAN)) {
                    boolean flag = true;
                    for (String tmp : newCast) {
                        if (tmp.contains(actor)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        newCast.add(actor);
                    }
                }
            }

            // Director (allmults.org)
            if (newDirectors.isEmpty() && !allmultsId.equals(Movie.UNKNOWN)) {
                for (String tmpDirector : HTMLTools.extractTags(xml2, "<b>Режиссер:", "<p ", "", "</p>")) {
                    if (StringUtils.isNotBlank(tmpDirector)) {
                        newDirectors.add(tmpDirector);
                    }
                }
            }

            if (OverrideTools.checkOverwriteDirectors(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setDirectors(newDirectors, ANIMATOR_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleDirectors(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setPeopleDirectors(newDirectors, ANIMATOR_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteWriters(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setWriters(newWriters, ANIMATOR_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleWriters(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setPeopleWriters(newWriters, ANIMATOR_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteActors(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setCast(newCast, ANIMATOR_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwritePeopleActors(movie, ANIMATOR_PLUGIN_ID)) {
                movie.setPeopleCast(newCast, ANIMATOR_PLUGIN_ID);
            }

            String country = Movie.UNKNOWN;
            String year2 = Movie.UNKNOWN;
            // Year + Country (animator.ru)
            for (String year : HTMLTools.extractTags(xml, "p=films&year=", " г.</span>")) {
                country = NumberUtils.toInt(year, 0) > 1990 ? "Россия" : "СССР";
                year2 = year;
                break;
            }

            if (OverrideTools.checkOverwriteYear(movie, ANIMATOR_PLUGIN_ID)) {
                // Year (allmults.org)
                if (year2.equals(Movie.UNKNOWN) && !allmultsId.equals(Movie.UNKNOWN)) {
                    for (String tmpYear : HTMLTools.extractTags(xml2, "<b>Год:", "<br>", HTML_HREF, "</a>")) {
                        if (StringUtils.isNotBlank(tmpYear)) {
                            year2 = tmpYear;
                        }
                    }
                }
                movie.setYear(year2, ANIMATOR_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, ANIMATOR_PLUGIN_ID)) {
                // Country (allmults.org)
                if (country.equals(Movie.UNKNOWN) && !allmultsId.equals(Movie.UNKNOWN)) {
                    for (String tmpCountry : HTMLTools.extractTags(xml2, "<b>Страна:", "<br>", HTML_HREF, "</a>")) {
                        if (StringUtils.isNotBlank(tmpCountry)) {
                            country = tmpCountry;
                        }
                    }
                }

                movie.setCountries(country, ANIMATOR_PLUGIN_ID);
            }

            // Company
            if (OverrideTools.checkOverwriteCompany(movie, ANIMATOR_PLUGIN_ID)) {
                String company = Movie.UNKNOWN;
                for (String comp : HTMLTools.extractTags(xml, "onclick=\"showStudia", HTML_SPAN)) {
                    company = comp;
                }
                movie.setCompany(company, ANIMATOR_PLUGIN_ID);
            }

            // Poster + Fanart (animator.ru)
            String posterURL = Movie.UNKNOWN;
            if (!newAnimatorId.equals(Movie.UNKNOWN)) {
                int tmp = xml.indexOf("<img src=\"../film_img/");
                if (tmp != -1) {
                    posterURL = "http://www.animator.ru/film_img/" + xml.substring(tmp + 22, xml.indexOf("\" ", tmp));
                } else if (xml.contains("<img id=SlideShow ")) {
                    posterURL = "http://www.animator.ru/film_img/variants/film_" + newAnimatorId + "_00.jpg";
                    String fanURL = "http://www.animator.ru/film_img/variants/film_" + newAnimatorId + "_01.jpg";
                    if (StringTools.isValidString(fanURL)) {
                        movie.setFanartURL(fanURL);
                        movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                    }
                }
            }
            // Poster (allmults.org)
            if (posterURL.equals(Movie.UNKNOWN) && !allmultsId.equals(Movie.UNKNOWN)) {
                posterURL = "http://allmults.org/images/multiki/" + allmultsId + ".jpg";
            }

            if (StringTools.isValidString(posterURL)) {
                movie.setPosterURL(posterURL);
                movie.setPosterFilename(movie.getBaseName() + ".jpg");
            }
        } catch (Exception error) {
            LOG.error("Failed retreiving movie data from Animator : {}", animatorId);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return true;
    }
}
