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

/*
 Plugin to retrieve movie data from Russian movie database www.kinopoisk.ru
 Written by Yury Sidorov.

 First the movie data is searched in IMDB and TheTvDB.
 After that the movie is searched in kinopoisk and movie data
 is updated.

 It is possible to specify URL of the movie page on kinopoisk in
 the .nfo file. In this case movie data will be retrieved from this page only.
 */
package com.moviejukebox.plugin;

import java.io.File;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.model.Award;
import com.moviejukebox.model.AwardEvent;
import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Person;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;


public class KinopoiskPlugin extends ImdbPlugin {

    public static String KINOPOISK_PLUGIN_ID = "kinopoisk";
    // Define plot length
    int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    @Deprecated
    String preferredRating = PropertiesUtil.getProperty("kinopoisk.rating", "imdb");
    protected TheTvDBPlugin tvdb;

    // Shows what name is on the first position with respect to divider
    String titleLeader = PropertiesUtil.getProperty("kinopoisk.title.leader", "english");
    String titleDivider = PropertiesUtil.getProperty("kinopoisk.title.divider", " - ");
    boolean joinTitles = PropertiesUtil.getBooleanProperty("kinopoisk.title.join", "true");

    // Set NFO information priority
    protected boolean NFOpriority = PropertiesUtil.getBooleanProperty("kinopoisk.NFOpriority", "false");
    protected boolean NFOplot = false;
    protected boolean NFOcast = false;
    protected boolean NFOgenres = false;
    protected boolean NFOdirectors = false;
    protected boolean NFOwriters = false;
    protected boolean NFOcertification = false;
    protected boolean NFOcountry = false;
    protected String NFOyear = "";
    protected boolean NFOtagline = false;
    protected boolean NFOrating = false;
    protected boolean NFOtop250 = false;
    protected boolean NFOcompany = false;
    protected boolean NFOrelease = false;
    protected boolean NFOfanart = false;
    protected boolean NFOposter = false;
    protected boolean NFOawards = false;

    String tmpAwards = PropertiesUtil.getProperty("mjb.scrapeAwards", "false");
    boolean scrapeWonAwards = tmpAwards.equalsIgnoreCase("won");
    boolean scrapeAwards = tmpAwards.equalsIgnoreCase("true") || scrapeWonAwards;

    boolean scrapeBusiness = PropertiesUtil.getBooleanProperty("mjb.scrapeBusiness", "false");
    boolean scrapeTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", "false");

    // Set priority fanart & poster by kinopoisk.ru
    boolean fanArt = PropertiesUtil.getBooleanProperty("kinopoisk.fanart", "false");
    boolean poster = PropertiesUtil.getBooleanProperty("kinopoisk.poster", "false");
    boolean kadr = PropertiesUtil.getBooleanProperty("kinopoisk.kadr", "false");

    boolean companyAll = PropertiesUtil.getProperty("kinopoisk.company", "first").equalsIgnoreCase("all");
    boolean countryAll = PropertiesUtil.getProperty("kinopoisk.country", "first").equalsIgnoreCase("all");
    boolean clearAward = PropertiesUtil.getBooleanProperty("kinopoisk.clear.award", "false");
    boolean clearTrivia = PropertiesUtil.getBooleanProperty("kinopoisk.clear.trivia", "false");

    boolean translitCountry = PropertiesUtil.getBooleanProperty("kinopoisk.translit.country", "false");

    String etalonId = PropertiesUtil.getProperty("kinopoisk.etalon", "251733");

    // Personal information
    int peopleMax = PropertiesUtil.getIntProperty("plugin.people.maxCount", "15");
    int actorMax = PropertiesUtil.getIntProperty("plugin.people.maxCount.actor", "10");
    int directorMax = PropertiesUtil.getIntProperty("plugin.people.maxCount.director", "2");
    int writerMax = PropertiesUtil.getIntProperty("plugin.people.maxCount.writer", "3");
    int filmographyMax = PropertiesUtil.getIntProperty("plugin.filmography.max", "20");
    int biographyLength = PropertiesUtil.getIntProperty("plugin.biography.maxlength", "500");
    int filmography = PropertiesUtil.getIntProperty("plugin.filmography.max", "20");
    boolean skipVG = PropertiesUtil.getBooleanProperty("plugin.people.skip.VG", "true");
    boolean skipTV = PropertiesUtil.getBooleanProperty("plugin.people.skip.TV", "false");
    boolean skipV = PropertiesUtil.getBooleanProperty("plugin.people.skip.V", "false");
    List<String> jobsInclude = Arrays.asList(PropertiesUtil.getProperty("plugin.filmography.jobsInclude", "Director,Writer,Actor,Actress").split(","));

    int triviaMax = PropertiesUtil.getIntProperty("plugin.trivia.maxCount", "15");

    String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    String jukeboxTempLocationDetails = FileTools.getCanonicalPath(PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp") + File.separator + PropertiesUtil.getProperty("mjb.detailsDirName", "Jukebox"));

    public KinopoiskPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Russia");
        tvdb = new TheTvDBPlugin();
    }

    @Override
    public String getPluginID() {
        return KINOPOISK_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = false;
        String kinopoiskId = mediaFile.getId(KINOPOISK_PLUGIN_ID);

        if (NFOpriority) {
            // checked NFO data
            NFOplot = StringTools.isValidString(mediaFile.getPlot());
            NFOcast = mediaFile.getPerson("Actors").size() > 0;
            NFOgenres = mediaFile.getGenres().size() > 0;
            NFOdirectors = mediaFile.getPerson("Directing").size() > 0;
            NFOwriters = mediaFile.getPerson("Writing").size() > 0;
            NFOcertification = StringTools.isValidString(mediaFile.getCertification());
            NFOcountry = StringTools.isValidString(mediaFile.getCountry());
            NFOyear = StringTools.isValidString(mediaFile.getYear())?mediaFile.getYear():"";
            NFOtagline = StringTools.isValidString(mediaFile.getTagline());
            NFOrating = mediaFile.getRating() > -1;
            NFOtop250 = mediaFile.getTop250() > -1;
            NFOcompany = StringTools.isValidString(mediaFile.getCompany());
            NFOrelease = StringTools.isValidString(mediaFile.getCompany());
            NFOfanart = StringTools.isValidString(mediaFile.getFanartURL());
            NFOposter = StringTools.isValidString(mediaFile.getPosterURL());
            NFOawards = mediaFile.getAwards().size() > 0;
        }

        if (StringTools.isNotValidString(kinopoiskId)) {
            // store original russian title and year
            String name = mediaFile.getOriginalTitle();
            String year = mediaFile.getYear();

            // It's better to remove everything after dash (-) before call of English plugins...
            final String previousTitle = mediaFile.getTitle();
            int dash = previousTitle.indexOf(titleDivider);
            if (dash != -1) {
                if (titleLeader.equals("english")) {
                    mediaFile.setTitle(previousTitle.substring(0, dash));
                } else {
                    mediaFile.setTitle(previousTitle.substring(dash));
                }
            }
            // Get base info from imdb or tvdb
            if (!mediaFile.isTVShow()) {
                super.scan(mediaFile);
            } else {
                tvdb.scan(mediaFile);
            }

            // Let's replace dash (-) by space ( ) in Title.
            //name.replace(titleDivider, " ");
            dash = name.indexOf(titleDivider);
            if (dash != -1) {
                if (titleLeader.equals("english")) {
                    name = name.substring(0, dash);
                } else {
                    name = name.substring(dash);
                }
            }

            kinopoiskId = getKinopoiskId(mediaFile, name, year, mediaFile.getSeason());
            mediaFile.setId(KINOPOISK_PLUGIN_ID, kinopoiskId);
//        } else {
            // If ID is specified in NFO, set original title to unknown
//            mediaFile.setTitle(Movie.UNKNOWN);
        }

        if (StringTools.isValidString(kinopoiskId)) {
            // Replace some movie data by data from Kinopoisk
            retval = updateKinopoiskMediaInfo(mediaFile, kinopoiskId);
        }

        return retval;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean result = false;
        logger.debug("Scanning NFO for Kinopoisk Id");
        int beginIndex = nfo.indexOf("kinopoisk.ru/level/1/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 26), "/");
            movie.setId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID, st.nextToken());
            logger.debug("Kinopoisk Id found in nfo = " + movie.getId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID));
            result = true;
        } else {
            logger.debug("No Kinopoisk Id found in nfo !");
        }
        super.scanNFO(nfo, movie);
        return result;
    }

    public String getKinopoiskId(Movie movie) {
        String kinopoiskId = movie.getId(KINOPOISK_PLUGIN_ID);

        if (StringTools.isNotValidString(kinopoiskId)) {
            int season = movie.getSeason();
            String name = movie.getOriginalTitle();
            String year = movie.getYear();

            int dash = name.indexOf(titleDivider);
            if (dash > -1) {
                name = titleLeader.equals("english")?name.substring(0, dash):name.substring(dash);
            }

            getKinopoiskId(movie, name, year, season);
        }
        return kinopoiskId;
    }

    public String getKinopoiskId(Movie movie, String name, String year, int season) {
        String kinopoiskId = movie.getId(KINOPOISK_PLUGIN_ID);

        if (StringTools.isNotValidString(kinopoiskId)) {
            kinopoiskId = getKinopoiskId(name, year, season);

            if (StringTools.isValidString(year) && StringTools.isNotValidString(kinopoiskId)) {
                try {
                    // Search for year +/-1, since the year is not always correct
                    int y = Integer.parseInt(year);
                    kinopoiskId = getKinopoiskId(name, Integer.toString(y - 1) + "-" + Integer.toString(y + 1), season);
                } catch (Exception ignore) {
                }
                if (StringTools.isNotValidString(kinopoiskId)) {
                    // Trying without specifying the year
                    kinopoiskId = getKinopoiskId(name, Movie.UNKNOWN, season);
                }
            }
        }
        return kinopoiskId;
    }

    /**
     * Retrieve Kinopoisk matching the specified movie name and year. This routine is base on a Google request.
     */
    public String getKinopoiskId(String movieName, String year, int season) {
        try {
            String kinopoiskId = "";
            String sb = movieName;
            // Unaccenting letters
            sb = Normalizer.normalize(sb, Normalizer.Form.NFD);
            sb = sb.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            sb = "&m_act[find]=" + URLEncoder.encode(sb, "UTF-8").replace(" ", "+");

            if (season != -1) {
                sb = sb + "&m_act[content_find]=serial";
            } else {
                if (StringTools.isValidString(year)) {
                    if (year.indexOf("-") > -1) {
                        String[] years = year.split("-");
                        sb = sb + "&m_act[from_year]=" + years[0];
                        sb = sb + "&m_act[to_year]=" + years[1];
                    } else {
                        sb = sb + "&m_act[year]=" + year;
                    }
                }
            }

            sb = "http://s.kinopoisk.ru/index.php?level=7&from=forma&result=adv&m_act[from]=forma&m_act[what]=content" + sb;
            String xml = webBrowser.request(sb);

            // Checking for zero results
            if (xml.indexOf("class=\"search_results\"") < 0) {
                // Checking direct movie page
                if (xml.indexOf("class=\"moviename-big\"") > -1) {
                    return HTMLTools.extractTag(xml, "/level/19/film/", "/");
                }
                return Movie.UNKNOWN;
            }

            // Checking if we got the movie page directly
            int beginIndex = xml.indexOf("id_film = ");
            if (beginIndex == -1) {
                // It's search results page, searching a link to the movie page
                //beginIndex = xml.indexOf("<!-- результаты поиска");
                beginIndex = xml.indexOf("class=\"search_results\"");
                if (beginIndex == -1) {
                    return Movie.UNKNOWN;
                }

                //beginIndex = xml.indexOf("href=\"/level/1/film/", beginIndex);
                beginIndex = xml.indexOf("/level/1/film/", beginIndex);
                if (beginIndex == -1) {
                    return Movie.UNKNOWN;
                }

                //StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 20), "/\"");
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 14), "/\"");
                kinopoiskId = st.nextToken();
            } else {
                // It's the movie page
                StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 10), ";");
                kinopoiskId = st.nextToken();
            }

            if (!kinopoiskId.equals("")) {
                // Check if ID is integer
                try {
                    Integer.parseInt(kinopoiskId);
                } catch (Exception ignore) {
                    return Movie.UNKNOWN;
                }
                return kinopoiskId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.error("Failed retreiving Kinopoisk Id for movie : " + movieName);
            logger.error("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan Kinopoisk html page for the specified movie
     */
    private boolean updateKinopoiskMediaInfo(Movie movie, String kinopoiskId) {
        try {
            String originalTitle = movie.getTitle();
            String newTitle = originalTitle;
            String xml = webBrowser.request("http://www.kinopoisk.ru/level/1/film/" + kinopoiskId);
            boolean etalonFlag = kinopoiskId.equals(etalonId);
            // Work-around for issue #649
            xml = xml.replace((CharSequence)"&#133;", (CharSequence)"&hellip;");
            xml = xml.replace((CharSequence)"&#151;", (CharSequence)"&mdash;");

            // Title
            if (!movie.isOverrideTitle() || etalonFlag) {
                newTitle = HTMLTools.extractTag(xml, "class=\"moviename-big\" itemprop=\"name\">", 0, "</");
                if (!newTitle.equals(Movie.UNKNOWN)) {
                    if (!movie.isOverrideTitle()) {
                        int i = newTitle.indexOf("(сериал");
                        if (i >= 0) {
                            newTitle = newTitle.substring(0, i);
                            movie.setMovieType(Movie.TYPE_TVSHOW);
                        }
                        newTitle = newTitle.replace('\u00A0', ' ').trim();
                        if (movie.getSeason() != -1) {
                            newTitle = newTitle + ", сезон " + String.valueOf(movie.getSeason());
                        }

                        // Original title
                        originalTitle = newTitle;
                        for (String s : HTMLTools.extractTags(xml, "<span style=\"color: #666", "</span>", "font-size: 13px\" itemprop=\"alternativeHeadline\">", "</span>")) {
                            if (!s.isEmpty()) {
                                originalTitle = s;
                                if (joinTitles) {
                                    newTitle = newTitle + Movie.SPACE_SLASH_SPACE + originalTitle;
                                }
                            }
                            break;
                        }
                    } else {
                        newTitle = originalTitle;
                    }
                } else {
                    if (etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get movie title!");
                    }
                    newTitle = originalTitle;
                }
            }

            // Plot
            if (!NFOplot) {
                StringBuilder plot = new StringBuilder();
                for (String subPlot : HTMLTools.extractTags(xml, "<span class=\"_reachbanner_\"", "</span>", "", "<")) {
                    if (!subPlot.isEmpty()) {
                        if (plot.length() > 0) {
                            plot.append(" ");
                        }
                        plot.append(subPlot);
                    }
                }

                String newPlot = "";
                if (plot.length() == 0) {
                    newPlot = movie.getPlot();
                } else {
                    newPlot = plot.toString();
                }

                if (!NFOplot) {
                    newPlot = StringTools.trimToLength(newPlot, preferredPlotLength, true, plotEnding);
                    movie.setPlot(newPlot);
                }

                if (etalonFlag && (plot.length() == 0)) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get plot!");
                }
            }

            boolean valueFounded = false;
            boolean genresFounded = false;
            boolean certificationFounded = false;
            boolean countryFounded = false;
            boolean yearFounded = false;
            boolean taglineFounded = false;
            boolean releaseFounded = false;
            boolean runtimeFounded = false;
            for (String item : HTMLTools.extractTags(xml, "<table class=\"info\">", "</table>", "<tr>", "</tr>")) {
                item = "<td>" + item + "</tr>";

                // Genres
                if (!NFOgenres) {
                    LinkedList<String> newGenres = new LinkedList<String>();
                    boolean GenresFound;
                    GenresFound = false;
                    for (String genre : HTMLTools.extractTags(item, ">жанр<", "</tr>", "<a href=\"/level/10", "</a>")) {
                        GenresFound = true;
                        genre = genre.substring(0, 1).toUpperCase() + genre.substring(1, genre.length());
                        if (genre.equalsIgnoreCase("мультфильм")) {
                            newGenres.addFirst(genre);
                        } else {
                            newGenres.add(genre);
                        }
                    }
                    if (GenresFound) {
                        // Limit genres count
                        int maxGenres = 9;
                        try {
                            maxGenres = PropertiesUtil.getIntProperty("genres.max", "9");
                        } catch (Exception ignore) {
                            //
                        }
                        while (newGenres.size() > maxGenres) {
                            newGenres.removeLast();
                        }

                        movie.setGenres(newGenres);
                        genresFounded = true;
                    }
                }

                // Certification from MPAA
                if (!NFOcertification) {
                    for (String mpaaTag : HTMLTools.extractTags(xml, ">рейтинг MPAA<", "</tr>", "<a href=\"/level/38", "</a>")) {
                        // Now need scan for 'alt' attribute of 'img'
                        String key = "alt=\"рейтинг ";
                        int pos = mpaaTag.indexOf(key);
                        if (pos != -1) {
                            int start = pos + key.length();
                            pos = mpaaTag.indexOf("\"", start);
                            if (pos != -1) {
                                mpaaTag = mpaaTag.substring(start, pos);
                                movie.setCertification(mpaaTag);
                                certificationFounded = true;
                            }
                        }
                        break;
                    }
                }

                // Country
                if (!NFOcountry) {
                    Collection<String> country = HTMLTools.extractTags(item, ">страна<", "</tr>", "<a href=\"/level/10", "</a>");
                    if (country != null && country.size() > 0) {
                        String strCountry = countryAll?StringUtils.join(country, Movie.SPACE_SLASH_SPACE):new ArrayList<String>(country).get(0);
                        if (translitCountry) {
                            strCountry = FileTools.makeSafeFilename(strCountry);
                        }
                        movie.setCountry(strCountry);
                        countryFounded = true;
                    }
                }

                // Year
                if (!movie.isOverrideYear() && NFOyear.equals("")) {
                    for (String year : HTMLTools.extractTags(item, ">год<", "</tr>", "<a href=\"/level/10", "</a>")) {
                        movie.setYear(year);
                        yearFounded = true;
                        break;
                    }
                } else if (!NFOyear.equals("")) {
                    movie.setYear(NFOyear);
                }

                // Run time
                if (movie.getRuntime().equals(Movie.UNKNOWN) || etalonFlag) {
                    for (String runtime : HTMLTools.extractTags(item, ">время<", "</tr>", "<td", "</td>")) {
                        if (runtime.indexOf("<span") > 0) {
                            runtime = runtime.substring(0, runtime.indexOf("<span"));
                        }
                        movie.setRuntime(runtime);
                        runtimeFounded = true;
                        break;
                    }
                }

                // Tagline
                if (!NFOtagline) {
                    for (String tagline : HTMLTools.extractTags(item, ">слоган<", "</tr>", "<td ", "</td>")) {
                        if (tagline.length() > 0) {
                            movie.setTagline(tagline.replace("\u00AB", "\"").replace("\u00BB", "\""));
                            taglineFounded = true;
                            break;
                        }
                    }
                }

                // Release date
                if (!NFOrelease) {
                    String releaseDate = "";
                    for (String release : HTMLTools.extractTags(item, ">премьера (мир)<", "</tr>", "<a href=\"/level/80", "</a>")) {
                        releaseDate = release;
                        releaseFounded = true;
                        break;
                    }
                    if (releaseDate.equals("")) {
                        for (String release : HTMLTools.extractTags(item, ">премьера (РФ)<", "</tr>", "<a href=\"/level/8", "</a>")) {
                            releaseDate = release;
                            releaseFounded = true;
                            break;
                        }
                    }
                    if (!releaseDate.equals("")) {
                        movie.setReleaseDate(releaseDate);
                    }
                }
            }
            if (etalonFlag) {
                if (!genresFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get genres!");
                }
                if (!certificationFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get certification!");
                }
                if (!countryFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get country!");
                }
                if (!yearFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get year!");
                }
                if (!runtimeFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get runtime!");
                }
                if (!taglineFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get tagline!");
                }
                if (!releaseFounded) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get release!");
                }
            }

            // Rating
            if (!NFOrating) {
                valueFounded = false;
                for (String rating : HTMLTools.extractTags(xml, "<a href=\"/level/83/film/" + kinopoiskId + "/\"", "</a>", "<span", "</span>")) {
                    try {
                        int kinopoiskRating = (int)(Float.parseFloat(rating) * 10);
                        movie.addRating(KINOPOISK_PLUGIN_ID, kinopoiskRating);
                        valueFounded = true;
                    } catch (Exception ignore) {
                        // Ignore
                    }
                    break;
                }
                if (!valueFounded && etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get KinoPoisk rating!");
                }

                int imdbRating = movie.getRating(IMDB_PLUGIN_ID);
                if (imdbRating == -1 || etalonFlag) {
                    // Get IMDB rating from kinopoisk page
                    String rating = HTMLTools.extractTag(xml, ">IMDb:", 0, " (");
                    valueFounded = false;
                    if (StringTools.isValidString(rating)) {
                        try {
                            imdbRating = (int)(Float.parseFloat(rating) * 10);
                            movie.addRating(IMDB_PLUGIN_ID, imdbRating);
                            valueFounded = true;
                        } catch (Exception ignore) {
                            // Ignore
                        }
                    }
                    if (!valueFounded && etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get IMDB rating!");
                    }
                }

                valueFounded = false;
                for (String critics : HTMLTools.extractTags(xml, ">Рейтинг кинокритиков<", ">о рейтинге критиков<", "class=\"star\"", "</span>")) {
                    int plus = 0;
                    int minus = 0;
                    int kinopoiskCritics = 0;
                    try {
                        kinopoiskCritics = (int)(Float.parseFloat(critics) * 10);
                    } catch (Exception ignore) {
                        // Ignore
                    }
                    for (String tmp : HTMLTools.extractTags(xml, ">Рейтинг кинокритиков<", ">о рейтинге критиков<", "class=\"plus\"", "</span")) {
                        try {
                            plus = Integer.parseInt(tmp);
                        } catch (Exception ignore) {
                            // Ignore
                        }
                        break;
                    }
                    for (String tmp : HTMLTools.extractTags(xml, ">Рейтинг кинокритиков<", ">о рейтинге критиков<", "class=\"minus\"", "</span")) {
                        try {
                            minus = Integer.parseInt(tmp);
                        } catch (Exception ignore) {
                            // Ignore
                        }
                        break;
                    }
                    movie.addRating("rottentomatoes:" + plus + ":" + minus, kinopoiskCritics);
                    valueFounded = true;
                    break;
                }
                if (!valueFounded && etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get KinopoiskCritics rating!");
                }
            }

            // Top250
            // Clear previous rating : if KinoPoisk is selected as search engine -
            // it means user wants KinoPoisk's rating, not global.
            if (!NFOtop250) {
                movie.setTop250(-1);
                String top250 = HTMLTools.extractTag(xml, "<a href=\"/level/20/#", 0, "\"");
                valueFounded = false;
                try {
                    movie.setTop250(Integer.parseInt(top250));
                    valueFounded = true;
                } catch (Exception ignore) {
                    // Ignore
                }
                if (!valueFounded && etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get Top250!");
                }
            }

            // Poster
            String posterURL = movie.getPosterURL();
            if (StringTools.isNotValidString(posterURL) || (!NFOposter && poster) || etalonFlag) {
                valueFounded = false;
                if (poster || etalonFlag) {
                    String previousURL = posterURL;
                    posterURL = Movie.UNKNOWN;

                    // Load page with all poster
                    String wholeArts = webBrowser.request("http://www.kinopoisk.ru/level/17/film/" + kinopoiskId + "/");
                    if (StringTools.isValidString(wholeArts)) {
                        if (wholeArts.indexOf("<table class=\"fotos") != -1) {
                            String picture = HTMLTools.extractTag(wholeArts, "src=\"http://st.kinopoisk.ru/images/poster/sm_", 0, "\"");
                            if (StringTools.isValidString(picture)) {
                                posterURL = "http://st.kinopoisk.ru/images/poster/" + picture;
                                valueFounded = true;
                            }
                        }
                    }

                    if (StringTools.isNotValidString(posterURL)) {
                        posterURL = previousURL;
                    }

                    if (StringTools.isValidString(posterURL)) {
                        movie.setPosterURL(posterURL);
                        movie.setPosterFilename(movie.getBaseName() + ".jpg");
                        logger.debug("KinoPoisk Plugin: Set poster URL to " + posterURL + " for " + movie.getBaseName());
                    }
                }
                if (StringTools.isNotValidString(movie.getPosterURL())) {
                    movie.setTitle(originalTitle);
                    // Removing Poster info from plugins. Use of PosterScanner routine instead.
                    // movie.setPosterURL(locatePosterURL(movie, ""));
                }
                if (!valueFounded && etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get poster!");
                }
            }

            // Fanart
            String fanURL = movie.getFanartURL();
            if (StringTools.isNotValidString(fanURL) || (!NFOfanart && (fanArt || kadr)) || etalonFlag) {
                valueFounded = false;
                try {
                    String previousURL = fanURL;
                    fanURL = Movie.UNKNOWN;

                    // Load page with all wallpaper
                    String wholeArts = webBrowser.request("http://www.kinopoisk.ru/level/12/film/" + kinopoiskId + "/");
                    if (StringTools.isValidString(wholeArts)) {
                        if (wholeArts.indexOf("<table class=\"fotos") != -1) {
                            String picture = HTMLTools.extractTag(wholeArts, "src=\"http://st.kinopoisk.ru/images/wallpaper/sm_", 0, ".jpg");
                            if (StringTools.isValidString(picture)) {
                                if (picture.indexOf("_") > 0) {
                                    picture = picture.substring(0, picture.indexOf("_"));
                                }
                                String size = HTMLTools.extractTag(wholeArts, "<u><a href=\"/picture/" + picture + "/w_size/", 0, "/");
                                wholeArts = webBrowser.request("http://www.kinopoisk.ru/picture/" + picture + "/w_size/" + size);
                                if (StringTools.isValidString(wholeArts)) {
                                    picture = HTMLTools.extractTag(wholeArts, "src=\"http://st.kinopoisk.ru/im/wallpaper/", 0, "\"");
                                    if (StringTools.isValidString(picture)) {
                                        fanURL = "http://st.kinopoisk.ru/im/wallpaper/" + picture;
                                        valueFounded = true;
                                    }
                                }
                            }
                        }
                    }

                    if (kadr && StringTools.isNotValidString(fanURL)) {
                        // Load page with all videoimage
                        wholeArts = webBrowser.request("http://www.kinopoisk.ru/level/13/film/" + kinopoiskId + "/");
                        if (StringTools.isValidString(wholeArts)) {
                            // Looking for photos table
                            int photosInd = wholeArts.indexOf("<table class=\"fotos");
                            if (photosInd != -1) {
                                String picture = HTMLTools.extractTag(wholeArts, "src=\"http://st.kinopoisk.ru/images/kadr/sm_", 0, "\"");
                                if (StringTools.isValidString(picture)) {
                                    fanURL = "http://www.kinopoisk.ru/images/kadr/" + picture;
                                    valueFounded = true;
                                }
                            }
                        }
                    }

                    if (StringTools.isNotValidString(fanURL)) {
                        fanURL = previousURL;
                    }

                    if (StringTools.isValidString(fanURL)) {
                        movie.setFanartURL(fanURL);
                        movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                        logger.debug("KinoPoisk Plugin: Set fanart URL to " + fanURL + " for " + movie.getBaseName());
                    }
                } catch (Exception ignore) {
                    // Ignore
                }
            }

            // Studio/Company
            if (!NFOcompany) {
                xml = webBrowser.request("http://www.kinopoisk.ru/level/91/film/" + kinopoiskId);
                valueFounded = false;
                if (StringTools.isValidString(xml)) {
                    Collection<String> studio = new ArrayList<String>();
                    if (xml.indexOf(">Производство:<") != -1) {
                        for (String tmp : HTMLTools.extractTags(xml, ">Производство:<", "</table>", "<a href=\"/level/10/m_act%5B", "</a>")) {
                            studio.add(HTMLTools.removeHtmlTags(tmp));
                            if (!companyAll) {
                                break;
                            }
                        }
                    }
                    if (xml.indexOf(">Прокат:<") != -1 && (companyAll || studio.isEmpty())) {
                        for (String tmp : HTMLTools.extractTags(xml, ">Прокат:<", "</table>", "<a href=\"/level/10/m_act%5B", "</a>")) {
                            studio.add(HTMLTools.removeHtmlTags(tmp));
                        }
                    }
                    if (studio.size() > 0) {
                        movie.setCompany(companyAll?StringUtils.join(studio, Movie.SPACE_SLASH_SPACE):new ArrayList<String>(studio).get(0));
                        valueFounded = true;
                    }
                }
                if (!valueFounded && etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get company!");
                }
            }

            // Awards
            if ((scrapeAwards && !NFOawards) || etalonFlag) {
                if (clearAward) {
                    movie.clearAwards();
                }
                xml = webBrowser.request("http://www.kinopoisk.ru/level/94/film/" + kinopoiskId);
                Collection<AwardEvent> awards = new ArrayList<AwardEvent>();
                if (StringTools.isValidString(xml)) {
                    int beginIndex = xml.indexOf("/level/94/award/");
                    if (beginIndex != -1) {
                        for (String item : HTMLTools.extractTags(xml, "<table cellspacing=0 cellpadding=0 border=0 width=100%>", "<br /><br /><br /><br /><br /><br />", "<table cellspacing=0 cellpadding=0 border=0 width=100% ", "</table>")) {
                            String name = Movie.UNKNOWN;
                            int year = -1;
                            int won = 0;
                            int nominated = 0;
                            Collection<String> wons = new ArrayList<String>();
                            Collection<String> nominations = new ArrayList<String>();
                            for (String tmp : HTMLTools.extractTags(item, "<td height=40 class=\"news\" style=\"padding:10px\">", "</td>", "<a href=\"/level/94/award/", "</a>")) {
                                int coma = tmp.indexOf(",");
                                name = tmp.substring(0, coma);
                                year = Integer.parseInt(tmp.substring(coma + 2, coma + 6));
                                break;
                            }
                            for (String tmp : HTMLTools.extractTags(item, ">Победитель<", ":", "(", ")")) {
                                won = Integer.parseInt(tmp);
                                break;
                            }
                            if (won > 0) {
                                for (String tmp : HTMLTools.extractTags(item, ">Победитель<", "</ul>", "<li class=\"trivia\">", "</li>")) {
                                    wons.add(HTMLTools.removeHtmlTags(tmp));
                                }
                            }
                            for (String tmp : HTMLTools.extractTags(item, ">Номинации<", ":", "(", ")")) {
                                nominated = Integer.parseInt(tmp);
                                break;
                            }
                            if (nominated > 0) {
                                for (String tmp : HTMLTools.extractTags(item, ">Номинации<", "</ul>", "<li class=\"trivia\"", "</li>")) {
                                    nominations.add(HTMLTools.removeHtmlTags(tmp));
                                }
                            }
                            if (StringTools.isValidString(name) && year > 1900 && year < 2020 && (!scrapeWonAwards || (won > 0))) {
                                Award award = new Award();
                                award.setName(name);
                                award.setYear(year);
                                if (won > 0) {
                                    award.setWons(wons);
                                }
                                if (nominated > 0) {
                                    award.setNominations(nominations);
                                }

                                AwardEvent event = new AwardEvent();
                                event.setName(name);
                                event.addAward(award);
                                awards.add(event);
                            }
                        }
                    }
                }
                if (awards.size() > 0) {
                    movie.setAwards(awards);
                } else if (etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get award!");
                }
            }

            // Cast enhancement
            if (!NFOcast || !NFOdirectors || !NFOwriters) {
                xml = webBrowser.request("http://www.kinopoisk.ru/level/19/film/" + kinopoiskId);
                if (StringTools.isValidString(xml)) {
                    int peopleCount = 0;
                    if (!NFOdirectors) {
                        peopleCount = scanMoviePerson(movie, xml, "director", writerMax, peopleCount);
                    }
                    if (!NFOwriters) {
                        peopleCount = scanMoviePerson(movie, xml, "writer", writerMax, peopleCount);
                    }
                    if (!NFOcast) {
                        peopleCount = scanMoviePerson(movie, xml, "actor", actorMax, peopleCount);
                    }
                    Collection<Filmography> outcast = new ArrayList<Filmography>();
                    for (Filmography p : movie.getPeople()) {
                        if (StringTools.isNotValidString(p.getId(KINOPOISK_PLUGIN_ID))) {
                            outcast.add(p);
                        }
                    }
                    for (Filmography p : outcast) {
                        movie.removePerson(p);
                    }
                }
            }

            // Business
            if (scrapeBusiness || etalonFlag) {
                xml = webBrowser.request("http://www.kinopoisk.ru/level/85/film/" + kinopoiskId);
                if (StringTools.isValidString(xml)) {
                    valueFounded = false;
                    for (String tmp : HTMLTools.extractTags(xml, ">Итого:<", "</table>", "<font color=\"#ff6600\"", "</h3>")) {
                        if (StringTools.isValidString(tmp)) {
                            movie.setBudget(tmp.replaceAll("\u00A0", ",").replaceAll(",$", ""));
                            valueFounded = true;
                            break;
                        }
                    }
                    if (!valueFounded && etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get business: summary!");
                    }
                    valueFounded = false;
                    for (String tmp : HTMLTools.extractTags(xml, ">Первый уик-энд (США)<", "</table>",
                                    "<h3 style=\"font-size: 18px; margin: 0; padding: 0;color:#f60\"", "</h3>")) {
                        if (StringTools.isValidString(tmp)) {
                            movie.setOpenWeek("USA", tmp.replaceAll("\u00A0", ",").replaceAll(",$", ""));
                            valueFounded = true;
                            break;
                        }
                    }
                    if (!valueFounded && etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get business: first weekend!");
                    }
                    valueFounded = false;
                    for (String tmp : HTMLTools.extractTags(xml, ">Кассовые сборы<", "</table>", "<tr><td colspan=2", "</h3>")) {
                        tmp += "</h3>";
                        String country = HTMLTools.extractTag(tmp, ">", ":");
                        if (StringTools.isValidString(country)) {
                            String money = HTMLTools.removeHtmlTags("<h3 " + HTMLTools.extractTag(tmp, "<h3 ", "</h3>")).replaceAll("\u00A0", ",")
                                            .replaceAll(",$", "");
                            if (!money.equals("--")) {
                                movie.setGross(country.equals("В России") ? "Russia" : country.equals("В США") ? "USA"
                                                : country.equals("Общие сборы") ? "Worldwide" : country.equals("В других странах") ? "Others" : country,
                                                money);
                                valueFounded = true;
                            }
                        }
                    }
                    if (!valueFounded && etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get business: gross!");
                    }
                } else if (etalonFlag) {
                    logger.error("KinopoiskPlugin: Site design changed - failed get business!");
                }
            }

            // Did You Know
            if (scrapeTrivia || etalonFlag) {
                if (clearTrivia) {
                    movie.clearDidYouKnow();
                }
                if (etalonFlag && triviaMax == 0) {
                    triviaMax = 1;
                }
                if (triviaMax != 0) {
                    xml = webBrowser.request("http://www.kinopoisk.ru/level/1/film/" + kinopoiskId + "/view_info/ok/#trivia");
                    if (StringTools.isValidString(xml)) {
                        int i = 0;
                        for (String tmp : HTMLTools.extractTags(xml, ">Знаете ли вы, что...<", "</ul>", "<li class='trivia", "</li>")) {
                            if (i < triviaMax || triviaMax == -1) {
                                movie.addDidYouKnow(tmp);
                                valueFounded = true;
                                i++;
                            } else {
                                break;
                            }
                        }
                    }
                    if (!valueFounded && etalonFlag) {
                        logger.error("KinopoiskPlugin: Site design changed - failed get trivia!");
                    }
                }
            }

            // Finally set title
            movie.setTitle(newTitle);
        } catch (Exception error) {
            logger.error("Failed retreiving movie data from Kinopoisk : " + kinopoiskId);
            logger.error(SystemTools.getStackTrace(error));
        }
        return true;
    }

    protected int scanMoviePerson(Movie movie, String xml, String mode, int personMax, int peopleCount) {
        if (peopleCount < peopleMax && personMax > 0 && xml.indexOf("<a name=\"" + mode + "\">") != -1) {
            if (mode.equals("actor")) {
                movie.clearCast();
            } else if (mode.equals("director")) {
                movie.clearDirectors();
            } else if (mode.equals("writer")) {
                movie.clearWriters();
            }

            int count = 0;
            for (String item : HTMLTools.extractTags(xml, "<a name=\"" + mode + "\">", "<a href=\"#top\" class=\"continue\">", "<div class=\"dub ", "<div class=\"clear\"></div></div>")) {
                String name = HTMLTools.extractTag(item, "<div class=\"name\"><a href=\"/level/4/people/", "</a>");
                int beginIndex = name.indexOf("/\">");
                String personID = name.substring(0, beginIndex);
                name = name.substring(beginIndex + 3);
                String origName = HTMLTools.extractTag(item, "<span class=\"gray\">", "</span>").replace('\u00A0', ' ').trim();
                if (StringTools.isNotValidString(origName)) {
                    origName = name;
                }
                String role = Movie.UNKNOWN;
                String dubler = Movie.UNKNOWN;
                if (mode.equals("actor")) {
                    role = HTMLTools.extractTag(item, "<div class=\"role\">", "</div>").replaceAll("^\\.+\\s", "").replaceAll("\\s.$", "");
                    if (item.indexOf("<div class=\"dubInfo\">") > -1) {
                        dubler = HTMLTools.extractTag(HTMLTools.extractTag(item, "<div class=\"dubInfo\">", "</span></div>"), "<div class=\"name\"><a href=\"/level/4/people/", "</a>");
                        dubler = dubler.substring(dubler.indexOf("/\">") + 3);
                    }
                }
                count++;
                peopleCount++;
                boolean found = false;
                for (Filmography p : movie.getPeople()) {
                    if (p.getName().equalsIgnoreCase(origName) && p.getJob().equalsIgnoreCase(mode)) {
                        p.setId(KINOPOISK_PLUGIN_ID, personID);
                        p.setTitle(name);
                        p.setCharacter(role);
                        p.setDoublage(dubler);
                        if (mode.equals("actor")) {
                            movie.addActor(StringTools.isValidString(name)?name:origName);
                        } else if (mode.equals("director")) {
                            movie.addDirector(StringTools.isValidString(name)?name:origName);
                        } else if (mode.equals("writer")) {
                            movie.addWriter(StringTools.isValidString(name)?name:origName);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (mode.equals("actor")) {
                        movie.addActor(KINOPOISK_PLUGIN_ID + ":" + personID, origName + ":" + name, role, "http://www.kinopoisk.ru/level/4/people/" + personID + "/", dubler);
                    } else if (mode.equals("director")) {
                        movie.addDirector(KINOPOISK_PLUGIN_ID + ":" + personID, origName + ":" + name, "http://www.kinopoisk.ru/level/4/people/" + personID + "/");
                    } else if (mode.equals("writer")) {
                        movie.addWriter(KINOPOISK_PLUGIN_ID + ":" + personID, origName + ":" + name, "http://www.kinopoisk.ru/level/4/people/" + personID + "/");
                    }
                }
                if (count == personMax || peopleCount == peopleMax) {
                    break;
                }
            }
        }
        return peopleCount;
    }

    @Override
    public boolean scan(Person person) {
        String kinopoiskId = person.getId(KINOPOISK_PLUGIN_ID);
        if (StringTools.isNotValidString(kinopoiskId)) {
            kinopoiskId = getKinopoiskPersonId(person.getName(), person.getJob());
            person.setId(KINOPOISK_PLUGIN_ID, kinopoiskId);
        }

        boolean retval = super.scan(person);
        if (StringTools.isValidString(kinopoiskId)) {
            retval = updateKinopoiskPersonInfo(person);
        }
        return retval;
    }

    private boolean updateKinopoiskPersonInfo(Person person) {
        String kinopoiskId = person.getId(KINOPOISK_PLUGIN_ID);
        boolean returnStatus = false;

        try {
            String xml = webBrowser.request("http://www.kinopoisk.ru/level/4/people/" + kinopoiskId + "/view_info/ok/");
            if (StringTools.isValidString(xml)) {
                if (StringTools.isNotValidString(person.getName())) {
                    person.setName(HTMLTools.extractTag(xml, "<span style=\"font-size:13px;color:#666\">", "</span>"));
                }
                person.setTitle(HTMLTools.extractTag(xml, "<h1 style=\"padding:0px;margin:0px\" class=\"moviename-big\">", "</h1>"));

                String date = Movie.UNKNOWN;
                if (xml.indexOf("<td class=\"birth\"") > -1) {
                    String bd = HTMLTools.extractTag(xml, "<td class=\"birth\"", "</td>");
                    if (StringTools.isValidString(bd) && bd.indexOf("</a>") > -1) {
                        bd = HTMLTools.removeHtmlTags(bd).replaceAll("^[^>]*>", "");
                        if (bd.indexOf("•") > -1) {
                            bd = bd.substring(0, bd.indexOf("•")).replace(",", "").trim();
                        }
                        if (StringTools.isValidString(bd)) {
                            date = bd;
                        }
                    }
                }
                if (xml.indexOf(">дата смерти</td><td>") > -1) {
                    String dd = HTMLTools.extractTag(xml, ">дата смерти</td><td>", "</td>");
                    if (StringTools.isValidString(dd)) {
                        dd = HTMLTools.removeHtmlTags(dd);
                        dd = dd.substring(0, dd.indexOf("•")).replace(",", "").trim();
                        if (StringTools.isValidString(dd)) {
                            date += "/" + dd;
                        }
                    }
                }
                if (StringTools.isValidString(date)) {
                    person.setYear(date);
                }

                if (xml.indexOf(">место рождения</td><td>") > -1) {
                    String bp = HTMLTools.extractTag(xml, ">место рождения</td><td>", "</td>");
                    if (StringTools.isValidString(bp)) {
                        bp = HTMLTools.removeHtmlTags(bp);
                        if (StringTools.isValidString(bp) && !bp.equals("-")) {
                            person.setBirthPlace(bp);
                        }
                    }
                }

                String km = HTMLTools.extractTag(xml, ">всего фильмов</td><td>", "</td>");
                if (StringTools.isValidString(km)) {
                    person.setKnownMovies(Integer.parseInt(km));
                }

                StringBuilder bio = new StringBuilder();
                for (String item : HTMLTools.extractTags(xml, "<ul class=\"trivia\"", "</ul>", "<li class=\"trivia\"", "</li>")) {
                    bio.append(HTMLTools.removeHtmlTags(item).replaceAll("\u0097", "-")).append(" ");
                }

                if (StringTools.isValidString(bio.toString())) {
                    person.setBiography(StringTools.trimToLength(bio.toString(), biographyLength, true, plotEnding));
                }

                if (xml.indexOf("http://st.kinopoisk.ru/images/actor/" + kinopoiskId + ".jpg") > -1 && StringTools.isNotValidString(person.getPhotoURL())) {
                    person.setPhotoURL("http://st.kinopoisk.ru/images/actor/" + kinopoiskId + ".jpg");
                }

                if (xml.indexOf("/level/12/people/" + kinopoiskId + "/") > -1 && StringTools.isNotValidString(person.getBackdropURL())) {
                    xml = webBrowser.request("http://www.kinopoisk.ru/level/12/people/" + kinopoiskId + "/");
                    if (StringTools.isValidString(xml)) {
                        String size = Movie.UNKNOWN;
                        if (xml.indexOf("/w_size/1600/") > -1) {
                            size = "1600";
                        } else if (xml.indexOf("/w_size/1280/") > -1) {
                            size = "1280";
                        } else if (xml.indexOf("/w_size/1024/") > -1) {
                            size = "1024";
                        }
                        if (StringTools.isValidString(size)) {
                            String id = xml.substring(xml.indexOf("/w_size/" + size + "/") - 25, xml.indexOf("/w_size/" + size + "/"));
                            id = id.substring(id.indexOf("/picture/") + 9);
                            xml = webBrowser.request("http://www.kinopoisk.ru/picture/" + id + "/w_size/" + size + "/");
                            if (StringTools.isValidString(xml)) {
                                int beginInx = xml.indexOf("http://st.kinopoisk.ru/im/wallpaper");
                                if (beginInx > -1) {
                                    person.setBackdropURL(xml.substring(beginInx, xml.indexOf("\"", beginInx)));
                                }
                            }
                        }
                    }
                }
            }

            if (!preferredRating.equals("imdb")) {
                xml = webBrowser.request("http://www.kinopoisk.ru/level/4/people/" + kinopoiskId + "/sort/rating/");
                if (StringTools.isValidString(xml)) {
                    TreeMap<Float, Filmography> newFilmography = new TreeMap<Float, Filmography>();
                    for (String block : HTMLTools.extractTags(xml, "<center><div style='position: relative' ></div></center>", "<tr><td><br /><br /><br /><br /><br /><br /></td></tr>", "<tr><td colspan=3 height=4><spacer type=block height=4></td></tr>", "</div>        </td></tr>")) {
                        String job = HTMLTools.extractTag(block, "<div style=\"padding-left: 9px\" id=\"", "\">");
                        if (job.equals(Filmography.JOB_PRODUCER)) {
                            StringUtils.capitalize(Filmography.JOB_PRODUCER);
                        } else if (job.equals(Filmography.JOB_DIRECTOR)) {
                            StringUtils.capitalize(Filmography.JOB_DIRECTOR);
                        } else if (job.equals(Filmography.JOB_WRITER)) {
                            StringUtils.capitalize(Filmography.JOB_WRITER);
                        } else if (job.equals(Filmography.JOB_ACTOR)) {
                            StringUtils.capitalize(Filmography.JOB_ACTOR);
                        } else if (job.equals(Filmography.JOB_EDITOR)) {
                            StringUtils.capitalize(Filmography.JOB_EDITOR);
                        } else if (job.equals("himself") || job.equals("herself")) {
                            StringUtils.capitalize(Filmography.JOB_THEMSELVES);
                        } else if (job.equals(Filmography.JOB_DESIGN)) {
                            StringUtils.capitalize(Filmography.JOB_DESIGN);
                        } else if (job.equals(Filmography.JOB_OPERATOR)) {
                            StringUtils.capitalize(Filmography.JOB_OPERATOR);
                        } else if (job.equals(Filmography.JOB_COMPOSER)) {
                            StringUtils.capitalize(Filmography.JOB_MUSIC);
                        } else if (job.contains("_titr_")) {
                            StringUtils.capitalize(Filmography.JOB_ACTOR);
                        }
                        if (!jobsInclude.contains(job)) {
                            continue;
                        }
                        for (String item : HTMLTools.extractTags(block + "endmarker", "<tr><td colspan=3 style=\"padding-left:20px\">", "endmarker" , "<div class=\"item\">", "</div></div>")) {
                            String id = HTMLTools.extractTag(item, " href=\"/level/1/film/", "/\"");
                            String URL = "http://www.kinopoisk.ru/level/1/film/" + id + "/";
                            String title = HTMLTools.extractTag(item, " href=\"/level/1/film/" + id + "/\"", "</a>").replaceAll("[^>]*>", "").replaceAll("\u00A0", " ").trim();
                            if (skipTV && (title.indexOf(" (сериал)") > -1 || title.indexOf(" (ТВ)") > -1)) {
                                continue;
                            } else if (skipV && title.indexOf(" (видео)") > -1) {
                                continue;
                            }
                            String year = Movie.UNKNOWN;
                            if (title.lastIndexOf("(") > -1) {
                                year = HTMLTools.extractTag(title.substring(title.lastIndexOf("(")), "(", ")");
                                title = title.replace("(" + year + ")", "").trim();
                            }
                            String name = HTMLTools.extractTag(item, "<span class=\"role\">", "</span>").replaceAll("\u00A0", " ").trim();
                            String character = Movie.UNKNOWN;
                            if (name.indexOf("... ") > -1) {
                                String[] names = name.split("\\.\\.\\. ");
                                name = names[0].trim();
                                if (job.equals("Actor")) {
                                    character = names[1];
                                }
                            }
                            if (title.endsWith(", The")) {
                                title = "The " + title.replace(", The", "");
                            }
                            if (StringTools.isNotValidString(name)) {
                                name = title;
                            } else {
                                if (name.endsWith(", The")) {
                                    name = "The " + name.replace(", The", "");
                                }
                            }
                            String ratingStr = HTMLTools.extractTag(item, "<div class=\"rating\"><a href=\"", "</a>").replaceAll("[^>]*>", "");
                            Integer rating = 0;
                            if (StringTools.isValidString(ratingStr)) {
                                rating = (int)(Float.valueOf(ratingStr).floatValue() * 10);
                            }

                            float key = 101 - (rating + Float.valueOf("0." + id).floatValue());

                            if (newFilmography.get(key) == null) {
                                Filmography film = new Filmography();
                                film.setId(KINOPOISK_PLUGIN_ID, id);
                                film.setName(title);
                                film.setTitle(name);
                                film.setOriginalTitle(name);
                                film.setYear(year);
                                film.setJob(job);
                                film.setCharacter(character);
                                film.setDepartment();
                                film.setRating(Integer.toString(rating));
                                film.setUrl(URL);
                                newFilmography.put(key, film);
                            }
                        }
                    }

                    if (newFilmography.size() > 0) {
                        if ((person.getFilmography().size() > 0) && (preferredRating.equals("combine") || preferredRating.equals("average"))) {
                            for (Filmography film : person.getFilmography()) {
                                String name = film.getName().replace("ё", "е").replace("Ё", "Е").trim();
                                String title = film.getTitle().replace("ё", "е").replace("Ё", "Е").trim();
                                String originalTitle = film.getOriginalTitle().replace("ё", "е").replace("Ё", "Е").trim();
                                String year = film.getYear();
                                if (StringTools.isValidString(year)) {
                                    year = year.substring(0, 4);
                                }

                                for (Filmography f : newFilmography.values()) {
                                    String Name = f.getName().replace("ё", "е").replace("Ё", "Е").replace(": Часть 1 ", ": Часть I").replace(": Часть 2 ", ": Часть II").replace(": Часть 3 ", ": Часть III").replace(": Часть 4 ", ": Часть IV").replace(": Часть 5 ", ": Часть V").replace(": Часть 6 ", ": Часть VI").replace(": Часть 7 ", ": Часть VII").replace(": Часть 8 ", ": Часть VIII").replace(": Часть 9 ", ": Часть IX").replace(": Часть 10 ", ": Часть X").trim();
                                    String Title = f.getTitle().replace("ё", "е").replace("Ё", "Е").replace(": Часть 1 ", ": Часть I").replace(": Часть 2 ", ": Часть II").replace(": Часть 3 ", ": Часть III").replace(": Часть 4 ", ": Часть IV").replace(": Часть 5 ", ": Часть V").replace(": Часть 6 ", ": Часть VI").replace(": Часть 7 ", ": Часть VII").replace(": Часть 8 ", ": Часть VIII").replace(": Часть 9 ", ": Часть IX").replace(": Часть 10 ", ": Часть X").trim();
                                    String OriginalTitle = f.getOriginalTitle().replace("ё", "е").replace("Ё", "Е").replace(": Часть 1 ", ": Часть I").replace(": Часть 2 ", ": Часть II").replace(": Часть 3 ", ": Часть III").replace(": Часть 4 ", ": Часть IV").replace(": Часть 5 ", ": Часть V").replace(": Часть 6 ", ": Часть VI").replace(": Часть 7 ", ": Часть VII").replace(": Часть 8 ", ": Часть VIII").replace(": Часть 9 ", ": Часть IX").replace(": Часть 10 ", ": Часть X").trim();
                                    String Year = f.getYear();
                                    if (StringTools.isValidString(Year)) {
                                        Year = Year.substring(0, 4);
                                    }
                                    if (Name.equalsIgnoreCase(name) || Name.equalsIgnoreCase(title) || Name.equalsIgnoreCase(originalTitle) ||
                                            Title.equalsIgnoreCase(name) || Title.equalsIgnoreCase(title) || Title.equalsIgnoreCase(originalTitle) ||
                                            OriginalTitle.equalsIgnoreCase(name) || OriginalTitle.equalsIgnoreCase(title) || OriginalTitle.equalsIgnoreCase(originalTitle)) {
                                        String id = f.getId(KINOPOISK_PLUGIN_ID);
                                        film.setId(KINOPOISK_PLUGIN_ID, id);
                                        film.setName(f.getName());
                                        film.setTitle(f.getTitle());
                                        film.setOriginalTitle(f.getOriginalTitle());
                                        film.setYear(f.getYear());
                                        film.setJob(f.getJob());
                                        film.setCharacter(f.getCharacter());
                                        film.setDepartment();
                                        film.setRating(Integer.toString((int)(preferredRating.equals("combine")?(Float.valueOf(f.getRating()).floatValue() * 1000 + Float.valueOf(film.getRating()).floatValue()):((Float.valueOf(film.getRating()).floatValue() + Float.valueOf(f.getRating()).floatValue())/2))));
                                        film.setUrl(f.getUrl());
                                    }
                                }
                            }
                        } else if (preferredRating.equals("kinopoisk") || (person.getFilmography().isEmpty())) {
                            person.setFilmography(new ArrayList<Filmography>(newFilmography.values()));
                        }
                    }
                }
            }

            returnStatus = true;
        } catch (Exception error) {
            logger.error("Failed retreiving KinopoiskPlugin data for person : " + kinopoiskId);
            logger.error(SystemTools.getStackTrace(error));
        }
        return returnStatus;
    }

    protected String getKinopoiskPersonId(String person, String mode) {
        String personId = Movie.UNKNOWN;
        try {
            String sb = person;
            sb = sb.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            sb = "&m_act[find]=" + URLEncoder.encode(sb, "UTF-8").replace(" ", "+");

            for (int step = 0; step < 3 && StringTools.isNotValidString(personId); step++) {
                String xml = webBrowser.request("http://www.kinopoisk.ru/index.php?level=7&from=forma&result=adv&m_act[from]=forma" + (step < 2 ? ("&m_act[what]=actor") : "") + sb + (step == 0 ? ("&m_act[work]=" + mode) : ""));

                // Checking for zero results
                int beginIndex = xml.indexOf("class=\"search_results\"");
                if (beginIndex > 0) {
                    // Checking if we got the person page directly
                    int beginInx = xml.indexOf("id_actor = ");
                    if (beginInx == -1) {
                        // It's search results page, searching a link to the person page
                        beginIndex = xml.indexOf("/level/4/people/", beginIndex);
                        if (beginIndex != -1) {
                            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 16), "/");
                            personId = st.nextToken();
                        }
                    } else {
                        // It's the person page
                        StringTokenizer st = new StringTokenizer(xml.substring(beginInx + 11), ";");
                        personId = st.nextToken();
                    }
                }
            }
        } catch (Exception error) {
            logger.error("Error : " + error.getMessage());
        }
        return personId;
    }
}
