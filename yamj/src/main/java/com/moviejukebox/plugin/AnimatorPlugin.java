/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

/*
 Plugin to retrieve movie data from Russian animation database www.animator.ru and www.allmults.org
 Written by Ilgizar Mubassarov (based on KinopoiskPlugin.java)

 animator.sites: [all, animator, allmults] - where find movie data
 */
package com.moviejukebox.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * @author ilgizar
 */
public class AnimatorPlugin extends ImdbPlugin {

    public static String ANIMATOR_PLUGIN_ID = "animator";
//  Define plot length
    int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    String preferredSites = PropertiesUtil.getProperty("animator.sites", "all");

    String[] listSites = preferredSites.split(",");
    boolean animatorDiscovery = (preferredSites.equals("all") || ArrayUtils.indexOf(listSites, "animator") != -1);
    boolean multsDiscovery = (preferredSites.equals("all") || ArrayUtils.indexOf(listSites, "allmults") != -1);

    public AnimatorPlugin() {
        super();
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        String animatorId = mediaFile.getId(ANIMATOR_PLUGIN_ID);
        if (StringTools.isNotValidString(animatorId)) {
// It's better to remove everything after dash (-) before call of English plugins...
            final String previousTitle = mediaFile.getTitle();
            int dash = previousTitle.indexOf('-');
            if (dash != -1) {
                mediaFile.setTitle(new String(previousTitle.substring(0, dash)));
            }

            String year = mediaFile.getYear();
// Let's replace dash (-) by space ( ) in Title.
            String name = mediaFile.getTitle();
            name.replace('-', ' ');
            animatorId = getAnimatorId(name, year, mediaFile.getSeason());

            if (StringTools.isValidString(year) && StringTools.isNotValidString(animatorId)) {
// Trying without specifying the year
                animatorId = getAnimatorId(name, Movie.UNKNOWN, mediaFile.getSeason());
            }
            mediaFile.setId(ANIMATOR_PLUGIN_ID, animatorId);
        } else {
// If ID is specified in NFO, set original title to unknown
            mediaFile.setTitle(Movie.UNKNOWN);
        }

        if (StringTools.isValidString(animatorId)) {
            try {
                retval = updateAnimatorMediaInfo(mediaFile, animatorId);
            } catch (IOException ex) {
                Logger.getLogger(AnimatorPlugin.class.getName()).error(ex);
            }
        }
        return retval;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.debug("Scanning NFO for Animator Id");
        int beginIndex = nfo.indexOf("animator.ru/db/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex + 33)), "");
            movie.setId(AnimatorPlugin.ANIMATOR_PLUGIN_ID, st.nextToken());
            logger.debug("Animator Id found in nfo = " + movie.getId(AnimatorPlugin.ANIMATOR_PLUGIN_ID));
        } else {
            logger.debug("No Animator Id found in nfo !");
        }
        super.scanNFO(nfo, movie);
    }

/**
 * Retrieve Animator matching the specified movie name and year. This routine is base on a Google request.
 */
    private String getAnimatorId(String movieName, String year, int season) {
        try {
            String animatorId = Movie.UNKNOWN;
            String allmultsId = Movie.UNKNOWN;

            String sb = movieName;
// Unaccenting letters
            sb = Normalizer.normalize(sb, Normalizer.Form.NFD);
// Return simple letters 'й' & 'Й'
            sb = sb.replaceAll("И" + (char)774, "Й");
            sb = sb.replaceAll("и" + (char)774, "й");
            sb = sb.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            sb = "text=" + URLEncoder.encode(sb, "Cp1251").replace(" ", "+");

            String xml = "";
// Get ID from animator.ru
            if (animatorDiscovery) {
                String uri = "http://www.animator.ru/db/?p=search&SearchMask=1&" + sb;
                if (StringTools.isValidString(year)) {
                    uri = uri + "&year0=" + year;
                    uri = uri + "&year1=" + year;
                }
                xml = webBrowser.request(uri);
// Checking for zero results
                if (xml.indexOf("[соответствие фразы]") != -1) {
// It's search results page, searching a link to the movie page
                    int beginIndex;
                    if (xml.indexOf("Найдено ") != -1) {
                        for (String tmp : HTMLTools.extractTags(xml, "Найдено ", "</td>", "<a href=", "<br><br>")) {
                            if (tmp.indexOf("[соответствие фразы]") >= 0) {
                                beginIndex = tmp.indexOf(" г.)");
                                if (beginIndex >= 0) {
                                    String Year = new String(tmp.substring(beginIndex - 4, beginIndex));
                                    if (Year.equals(year)) {
                                        beginIndex = tmp.indexOf("http://www.animator.ru/db/?p=show_film&fid=", beginIndex);
                                        if (beginIndex >= 0) {
                                           StringTokenizer st = new StringTokenizer(new String(tmp.substring(beginIndex + 43)), " ");
                                           animatorId = st.nextToken();
                                           break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!animatorId.equals("") && animatorId != Movie.UNKNOWN) {
// Check if ID is integer
                            try {
                                Integer.parseInt(animatorId);
                            } catch (Exception ignore) {
                                // Ignore
                            }
                        }
                    }
                }
            }

// Get ID from allmults.org
            if (multsDiscovery) {
                URL url = new URL("http://allmults.org/search.php");
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(sb);
                wr.flush();

                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "cp1251"));
                xml = "";
                String line;
                while ((line = rd.readLine()) != null) {
                    xml = xml + line;
                }
                wr.close();
                rd.close();

                if (xml.indexOf("<div class=\"post\"") != -1) {
                    for (String tmp : HTMLTools.extractTags(xml, "По Вашему запросу найдено ", "<ul><li>", "<div class=\"entry\"", "</div>")) {
                        int pos = tmp.indexOf("<img ");
                        if (pos != -1) {
                            int temp = tmp.indexOf(" alt=\"");
                            if (temp != -1) {
                                String Year = new String(tmp.substring(temp + 6, tmp.indexOf("\"", temp + 6) - 1));
                                Year = new String(Year.substring(Year.length() - 4));
                                if (Year.equals(year)) {
                                    temp = tmp.indexOf(" src=\"/images/multiki/");
                                    if (temp != -1) {
                                        allmultsId = new String(tmp.substring(temp + 22, tmp.indexOf(".jpg", temp + 22)));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!allmultsId.equals("") && allmultsId != Movie.UNKNOWN) {
// Check if ID is integer
                        try {
                            Integer.parseInt(allmultsId);
                        } catch (Exception ignore) {
                            // Ignore
                        }
                    }
                }
            }

            return (animatorId == Movie.UNKNOWN && allmultsId == Movie.UNKNOWN)?Movie.UNKNOWN:animatorId + ":" + allmultsId;
        } catch (Exception error) {
            logger.error("Failed retreiving Animator Id for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

/**
 * Scan Animator html page for the specified movie
 */
    private boolean updateAnimatorMediaInfo(Movie movie, String animatorId) throws IOException {
        try {
            String originalTitle = movie.getTitle();
            String[] listID = animatorId.split(":");
            animatorId = listID[0];
            String allmultsId = listID[1];

            String xml = "";
            String xml2 = "";
            if (animatorId != Movie.UNKNOWN) {
                xml = "http://www.animator.ru/db/?p=show_film&fid=" + animatorId;
//logger.log(Level.SEVERE, "ANIMATOR URL: " + xml);
                xml = webBrowser.request(xml);
            }
            if (allmultsId != Movie.UNKNOWN) {
                xml2 = "http://allmults.org/multik.php?id=" + allmultsId;
//logger.log(Level.SEVERE, "ALLMULTS URL: " + xml2);
                xml2 = webBrowser.request(xml2);
            }

// Work-around for issue #649
            xml = xml.replace((CharSequence)"&#133;", (CharSequence)"&hellip;");
            xml = xml.replace((CharSequence)"&#151;", (CharSequence)"&mdash;");
            xml2 = xml2.replace((CharSequence)"&#133;", (CharSequence)"&hellip;");
            xml2 = xml2.replace((CharSequence)"&#151;", (CharSequence)"&mdash;");

            if (animatorId != Movie.UNKNOWN) {
// Title (animator.ru)
                for (String tit : HTMLTools.extractTags(xml, "<td align=\"left\" class=\"FilmName\">", "</B>", "«", "»")) {
                    originalTitle = tit;
                    break;
                }
            } else {
// Title (allmults.org)
                for (String tit : HTMLTools.extractTags(xml2, "<b>Название:", "<b>", "", "<br>")) {
                    originalTitle = tit;
                    break;
                }
            }

            StringBuffer plot = new StringBuffer();
// Plot (animator.ru)
            if (animatorId != Movie.UNKNOWN) {
                for (String subPlot : HTMLTools.extractTags(xml, "<td align=\"left\" class=\"FilmComments\"", "</td>")) {
                    if (!subPlot.isEmpty()) {
                        if (plot.length() > 0) {
                           plot.append(" ");
                        }
                        plot.append(subPlot);
                    }
                }

                for (String subPlot : HTMLTools.extractTags(xml, "<td class=\"FilmComments\">", "</td>")) {
                    if (!subPlot.isEmpty()) {
                        if (plot.length() > 0) {
                            plot.append(" ");
                        }
                        plot.append(subPlot);
                    }
                }
            }
// Plot (allmults.org)
            if (plot.length() == 0 && allmultsId != Movie.UNKNOWN) {
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
                newPlot = newPlot.replace("</span>", "");
            }

            newPlot = StringTools.trimToLength(newPlot, preferredPlotLength, true, plotEnding);
            movie.setPlot(newPlot);

// Genre + Run time (animator.ru)
//            String MultType = "";
            String time = Movie.UNKNOWN;
            LinkedList<String> newGenres = new LinkedList<String>();
            newGenres.addFirst("мультфильм");
            for (String tmp : HTMLTools.extractTags(xml, "class=\"FilmType\"><i", "</td>", "", "</i>")) {
                for (String temp : tmp.split(",")) {
                    if (!temp.equals("")) {
                        if (temp.indexOf(" мин.") > 0) {
                            time = new String(temp.substring(0, temp.indexOf(" мин.") + 4));
                            break;
                        }
                        newGenres.add(temp);
                    }
                }
            }
// Genre (allmults.org)
            if (newGenres.size() == 0 && allmultsId != Movie.UNKNOWN) {
                for (String tmp : HTMLTools.extractTags(xml2, "<b>Жанр:", "<b>", "", "<br>")) {
                    for (String temp : tmp.split(",")) {
                        if (!temp.equals("")) {
                            newGenres.add(temp);
                        }
                    }
                }
            }
// Run time (allmults.org)
            if (time == Movie.UNKNOWN && allmultsId != Movie.UNKNOWN) {
                for (String tmp : HTMLTools.extractTags(xml2, "<b>Продолжительность:", "<b>", "", "<br>")) {
                    time = tmp;
                    break;
                }
            }
            movie.setGenres(newGenres);
            movie.setRuntime(time);

            Collection<String> newDirectors = new ArrayList<String>();
            Collection<String> newWriters = new ArrayList<String>();
            Collection<String> newCast = new ArrayList<String>();
// Director, Writers, Cast (animator.ru)
            for (String item : HTMLTools.extractTags(xml, "<table cellpadding=0 cellspacing=0 width=380", "</table>", "<tr>", "</tr>")) {
                item = "<td>" + item + "</tr>";
// Director
                for (String tmp : HTMLTools.extractTags(item, ">режиссер", "</tr>", "<td class=\"PersonsList\"", "</td>")) {
                    for (String writer : HTMLTools.extractTags(tmp, "<span class=\"MiddleLinks\"", "</span>")) {
                        newDirectors.add(writer);
                    }
                }

// Writers
                for (String tmpWriter : HTMLTools.extractTags(item, ">сценарист", "</tr>", "<td class=\"PersonsList\"", "</td>")) {
                    for (String writer : HTMLTools.extractTags(tmpWriter, "<span class=\"MiddleLinks\"", "</span>")) {
                        newWriters.add(writer);
                    }
                }

// Cast
                for (String actor : HTMLTools.extractTags(item, ">роли озвучивали", "</tr>", "<nobr", "</nobr>")) {
                    String act = "";
                    for (String tmpAct : HTMLTools.extractTags(actor, "<span class=\"MiddleLinks\"", "</span>")) {
                        act = tmpAct;
                        break;
                    }
                    for (String tmpPers : HTMLTools.extractTags(actor, "</span> (", ")")) {
                        act = act + " (" + tmpPers + ")";
                        break;
                    }
                    if (!act.equals("")) {
                        newCast.add(act);
                    }
                }
                for (String actor : HTMLTools.extractTags(item, ">роли озвучивали", "</tr>", "<span class=\"MiddleLinks\"", "</span>")) {
                    boolean flag = true;
                    for (String tmp : newCast) {
                        if (tmp.indexOf(actor) != -1) {
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
            if (newDirectors.size() == 0 && allmultsId != Movie.UNKNOWN) {
                for (String tmp : HTMLTools.extractTags(xml2, "<b>Режиссер:", "<p ", "", "</p>")) {
                    if (!tmp.equals("")) {
                        newDirectors.add(tmp);
                    }
                }
            }
            if (newDirectors.size() > 0) {
                movie.setDirectors(newDirectors);
            }
            if (newWriters.size() > 0) {
                movie.setWriters(newWriters);
            }
            if (newCast.size() > 0) {
                movie.setCast(newCast);
            }

            String Country = Movie.UNKNOWN;
            String Year = Movie.UNKNOWN;
// Year + Country (animator.ru)
            for (String year : HTMLTools.extractTags(xml, "p=films&year=", " г.</span>")) {
                Country = (Integer.parseInt(year) > 1990)?"Россия":"СССР";
                Year = year;
                break;
            }
// Year (allmults.org)
            if (Year == Movie.UNKNOWN && allmultsId != Movie.UNKNOWN) {
                for (String tmp : HTMLTools.extractTags(xml2, "<b>Год:", "<br>", "<a href=", "</a>")) {
                    if (!tmp.equals("")) {
                        Year = tmp;
                    }
                }
            }
// Country (allmults.org)
            if (Country == Movie.UNKNOWN && allmultsId != Movie.UNKNOWN) {
                for (String tmp : HTMLTools.extractTags(xml2, "<b>Страна:", "<br>", "<a href=", "</a>")) {
                    if (!tmp.equals("")) {
                        Country = tmp;
                    }
                }
            }

// Translate russian to english for correctly show flag
//            if (Country.equals("Россия")) {
//                Country = "Russia";
//            } else if (Country.equals("СССР")) {
//                Country = "USSR";
//            }

            movie.setYear(Year);
            movie.setCountry(Country);

// Company
            String Company = Movie.UNKNOWN;
            for (String comp : HTMLTools.extractTags(xml, "onclick=\"showStudia", "</span>")) {
                Company = comp;
            }
            movie.setCompany(Company);

// Poster + Fanart (animator.ru)
            String fanURL = Movie.UNKNOWN;
            String posterURL = Movie.UNKNOWN;
            if (animatorId != Movie.UNKNOWN) {
                int tmp = xml.indexOf("<img src=\"../film_img/");
                if (tmp != -1) {
                    posterURL = "http://www.animator.ru/film_img/" + new String(xml.substring(tmp + 22, xml.indexOf("\" ", tmp)));
                } else if (xml.indexOf("<img id=SlideShow ") != -1) {
                    posterURL = "http://www.animator.ru/film_img/variants/film_" + animatorId + "_00.jpg";
                    fanURL = "http://www.animator.ru/film_img/variants/film_" + animatorId + "_01.jpg";
                    if (StringTools.isValidString(fanURL)) {
                        movie.setFanartURL(fanURL);
                        movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                    }
                }
            }
// Poster (allmults.org)
            if (posterURL == Movie.UNKNOWN && allmultsId != Movie.UNKNOWN) {
                posterURL = "http://allmults.org/images/multiki/" + allmultsId + ".jpg";
            }

            if (StringTools.isValidString(posterURL)) {
                movie.setPosterURL(posterURL);
                movie.setPosterFilename(movie.getBaseName() + ".jpg");
            }

// Finally set title
            movie.setTitle(originalTitle);
        } catch (Exception error) {
            logger.error("Failed retreiving movie data from Animator : " + animatorId);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return true;
    }
}
