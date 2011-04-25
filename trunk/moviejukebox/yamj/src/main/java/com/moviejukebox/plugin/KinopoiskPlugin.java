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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.IMovieBasicInformation;

import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebStats;


public class KinopoiskPlugin extends ImdbPlugin {

    public static String KINOPOISK_PLUGIN_ID = "kinopoisk";
    // Define plot length
    int preferredPlotLength = PropertiesUtil.getIntProperty("plugin.plot.maxlength", "500");
    String preferredRating = PropertiesUtil.getProperty("kinopoisk.rating", "imdb");
    protected TheTvDBPlugin tvdb;
    // Copied from ComingSoonPlugin.java
    protected boolean trailersScannerEnable;
    protected boolean trailerSetExchange;
    protected boolean trailerDownload;

    public KinopoiskPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Russia");
        tvdb = new TheTvDBPlugin();
        // Copied from ComingSoonPlugin.java
        trailersScannerEnable = PropertiesUtil.getBooleanProperty("trailers.scanner.enable", "true");
        trailerSetExchange = PropertiesUtil.getBooleanProperty("kinopoisk.trailer.setExchange", "false");
        trailerDownload = PropertiesUtil.getBooleanProperty("kinopoisk.trailer.download", "false");
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        String kinopoiskId = mediaFile.getId(KINOPOISK_PLUGIN_ID);
        if (StringTools.isNotValidString(kinopoiskId)) {
            // It's better to remove everything after dash (-) before call of English plugins...
            final String previousTitle = mediaFile.getTitle();
            int dash = previousTitle.indexOf('-');
            if (dash != -1) {
                mediaFile.setTitle(previousTitle.substring(0, dash));
            }
            // Get base info from imdb or tvdb
            if (!mediaFile.isTVShow()) {
                super.scan(mediaFile);
            } else {
                tvdb.scan(mediaFile);
            }

            String year = mediaFile.getYear();
            // Let's replace dash (-) by space ( ) in Title.
            String name = mediaFile.getTitle();
            name.replace('-', ' ');
            kinopoiskId = getKinopoiskId(name, year, mediaFile.getSeason());

            if (StringTools.isValidString(year) && StringTools.isNotValidString(kinopoiskId)) {
                // Trying without specifying the year
                kinopoiskId = getKinopoiskId(name, Movie.UNKNOWN, mediaFile.getSeason());
            }
            mediaFile.setId(KINOPOISK_PLUGIN_ID, kinopoiskId);
        } else {
            // If ID is specified in NFO, set original title to unknown
            mediaFile.setTitle(Movie.UNKNOWN);
        }

        if (StringTools.isValidString(kinopoiskId)) {
            // Replace some movie data by data from Kinopoisk
            retval = updateKinopoiskMediaInfo(mediaFile, kinopoiskId);
        }

        if (trailersScannerEnable) {
            generateTrailer(mediaFile);
        }
        return retval;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.debug("Scanning NFO for Kinopoisk Id");
        int beginIndex = nfo.indexOf("kinopoisk.ru/level/1/film/");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 26), "/");
            movie.setId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID, st.nextToken());
            logger.debug("Kinopoisk Id found in nfo = " + movie.getId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID));
        } else {
            logger.debug("No Kinopoisk Id found in nfo !");
        }
        super.scanNFO(nfo, movie);
    }

    /**
     * Retrieve Kinopoisk matching the specified movie name and year. This routine is base on a Google request.
     */
    private String getKinopoiskId(String movieName, String year, int season) {
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
                    try {
                        // Search for year +/-1, since the year is not always correct
                        int y = Integer.parseInt(year);
                        sb = sb + "&m_act[from_year]=" + Integer.toString(y - 1);
                        sb = sb + "&m_act[to_year]=" + Integer.toString(y + 1);
                    } catch (Exception ignore) {
                        sb = sb + "&m_act[year]=" + year;
                    }
                }
            }

            sb = "http://www.kinopoisk.ru/index.php?level=7&from=forma&result=adv&m_act[from]=forma&m_act[what]=content" + sb;

            String xml = webBrowser.request(sb);

            // Checking for zero results
            //if (xml.indexOf("найдено 0 результатов") >= 0) {
            if (xml.indexOf("class=\"search_results\"") < 0) {
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

            // Work-around for issue #649
            xml = xml.replace((CharSequence)"&#133;", (CharSequence)"&hellip;");
            xml = xml.replace((CharSequence)"&#151;", (CharSequence)"&mdash;");

            // Title
            if (!movie.isOverrideTitle()) {
                newTitle = HTMLTools.extractTag(xml, "class=\"moviename-big\">", 0, "<>");
                if (!newTitle.equals(Movie.UNKNOWN)) {
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
                    for (String s : HTMLTools.extractTags(xml, "class=\"moviename-big\">", "</span>", "<span", "</span>")) {
                        if (!s.isEmpty()) {
                            originalTitle = s;
                            newTitle = newTitle + " / " + originalTitle;
                        }
                        break;
                    }
                } else {
                    newTitle = originalTitle;
                }
            }

            // Plot
            StringBuffer plot = new StringBuffer();
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

            newPlot = StringTools.trimToLength(newPlot, preferredPlotLength, true, plotEnding);
            movie.setPlot(newPlot);

            // Cast
            Collection<String> newCast = new ArrayList<String>();

            for (String actor : HTMLTools.extractTags(xml, ">В главных ролях:", "</table>", "<a href=\"/level/4", "</a>")) {
                newCast.add(actor);
            }
            if (newCast.size() > 0) {
                movie.setCast(newCast);
            }

            for (String item : HTMLTools.extractTags(xml, "<table class=\"info\">", "</table>", "<tr>", "</tr>")) {
                item = "<td>" + item + "</tr>";
                // Genres
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
                }

                // Director
                //for (String director : HTMLTools.extractTags(item, ">режиссер<", "</tr>", "<a href=\"/level/4", "</a>")) {
                //    movie.addDirector(director);
                //    break;
                //}
                Collection<String> newDirectors = new ArrayList<String>();
                for (String writer : HTMLTools.extractTags(item, ">режиссер<", "</tr>", "<a href=\"/level/4", "</a>")) {
                    newDirectors.add(writer);
                }

                if (newDirectors.size() > 0) {
                    movie.setDirectors(newDirectors);
                }

                // Writers
                Collection<String> newWriters = new ArrayList<String>();
                for (String writer : HTMLTools.extractTags(item, ">сценарий<", "</tr>", "<a href=\"/level/4", "</a>")) {
                    newWriters.add(writer);
                }
                if (newWriters.size() > 0) {
                    movie.setWriters(newWriters);
                }

                // Certification from MPAA
                for (String mpaaTag : HTMLTools.extractTags(item, ">рейтинг MPAA<", "</tr>", "<a href=\'/level/38", "</a>")) {
                    // Now need scan for 'alt' attribute of 'img'
                    String key = "alt='рейтинг ";
                    int pos = mpaaTag.indexOf(key);
                    if (pos != -1) {
                        int start = pos + key.length();
                        pos = mpaaTag.indexOf("'", start);
                        if (pos != -1) {
                            mpaaTag = mpaaTag.substring(start, pos);
                            movie.setCertification(mpaaTag);
                        }
                    }
                    break;
                }

                // Country
                for (String country : HTMLTools.extractTags(item, ">страна<", "</tr>", "<a href=\"/level/10", "</a>")) {
                    movie.setCountry(country);
                    break;
                }

                // Year
                if (!movie.isOverrideYear()) {
                    for (String year : HTMLTools.extractTags(item, ">год<", "</tr>", "<a href=\"/level/10", "</a>")) {
                        movie.setYear(year);
                        break;
                    }
                }

                // Run time
                if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                    for (String runtime : HTMLTools.extractTags(item, ">время<", "</tr>", "<td", "</td>")) {
                        movie.setRuntime(runtime);
                        break;
                    }
                }
            }

            // Rating
            int kinopoiskRating = -1;
            for (String rating : HTMLTools.extractTags(xml, "<a href=\"/level/83/film/" + kinopoiskId + "/\"", "</a>", "", "<")) {
                try {
                    kinopoiskRating = (int)(Float.parseFloat(rating) * 10);
                } catch (Exception ignore) {
                    // Ignore
                }
                break;
            }

            int imdbRating = movie.getRating();
            if (imdbRating == -1) {
                // Get IMDB rating from kinopoisk page
                String rating = HTMLTools.extractTag(xml, ">IMDB:", 0, "<(");
                if (!rating.equals(Movie.UNKNOWN)) {
                    try {
                        imdbRating = (int)(Float.parseFloat(rating) * 10);
                    } catch (Exception ignore) {
                        // Ignore
                    }
                }
            }

            int r = kinopoiskRating;
            if (imdbRating != -1) {
                if (preferredRating.equals("imdb") || kinopoiskRating == -1) {
                    r = imdbRating;
                } else if (preferredRating.equals("average")) {
                    r = (kinopoiskRating + imdbRating) / 2;
                }
            }
            movie.setRating(r);

            // Poster
            if (StringTools.isNotValidString(movie.getPosterURL())) {
                movie.setTitle(originalTitle);
                // Removing Poster info from plugins. Use of PosterScanner routine instead.
                // movie.setPosterURL(locatePosterURL(movie, ""));
            }

            // Top250
            // Clear previous rating : if KinoPoisk is selected as search engine - 
            // it means user wants KinoPoisk's rating, not global.
            movie.setTop250(-1);
            String top250 = HTMLTools.extractTag(xml, "<a href=\"/level/20/#", 0, "\"");
            try {
                movie.setTop250(Integer.parseInt(top250));
            } catch (Exception ignore) {
                // Ignore
            }

            // Fanart
            String fanURL = movie.getFanartURL();
            if (StringTools.isNotValidString(fanURL)) {
                try {
                    fanURL = Movie.UNKNOWN;
                    // Load page with all fanarts
                    String wholeArts = webBrowser.request("http://www.kinopoisk.ru/level/13/film/" + kinopoiskId + "/");
                    if (StringTools.isValidString(wholeArts)) {
                        // Looking for photos table
                        int photosInd = wholeArts.indexOf("\"fotos\"");
                        if (photosInd != -1) {
                            String picture = HTMLTools.extractTag(wholeArts, "src=\"/images/kadr/sm_", 0, "\"");
                            if (StringTools.isValidString(picture)) {
                                fanURL = "http://www.kinopoisk.ru/images/kadr/" + picture;
                            }
                        }
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

            // Finally set title
            movie.setTitle(newTitle);

        } catch (Exception error) {
            logger.error("Failed retreiving movie data from Kinopoisk : " + kinopoiskId);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
        return true;
    }

    // Copied from ComingSoonPlugin.java
    protected void generateTrailer(Movie movie) {
        if (movie.isExtra() || movie.isTVShow()) {
            return;
        }
        if (!movie.getExtraFiles().isEmpty()) {
            logger.debug("KinoPoisk Plugin: Movie has trailers, skipping");
            return;
        }

        String trailerUrl = getTrailerUrl(movie);
        if (StringTools.isNotValidString(trailerUrl)) {
            logger.debug("KinoPoisk Plugin: no trailer found");
            if (trailerSetExchange) {
                movie.setTrailerExchange(true);
            }
            return;
        }

        logger.debug("KinoPoisk Plugin: found trailer at URL " + trailerUrl);

        MovieFile tmf = new MovieFile();
        tmf.setTitle("TRAILER-ru");

        if (trailerDownload) {

            // Copied from AppleTrailersPlugin.java

            MovieFile mf = movie.getFirstFile();
            String parentPath = mf.getFile().getParent();
            String name = mf.getFile().getName();
            String basename;

            if (mf.getFilename().toUpperCase().endsWith("/VIDEO_TS")) {
                parentPath += File.separator + name;
                basename = name;
            } else if (mf.getFile().getAbsolutePath().toUpperCase().contains("BDMV")) {
                parentPath = new String(parentPath.substring(0, parentPath.toUpperCase().indexOf("BDMV") - 1));
                basename = new String(parentPath.substring(parentPath.lastIndexOf(File.separator) + 1));
            } else {
                int index = name.lastIndexOf(".");
                basename = index == -1 ? name : new String(name.substring(0, index));
            }

            String trailerExt = new String(trailerUrl.substring(trailerUrl.lastIndexOf(".")));
            String trailerBasename = FileTools.makeSafeFilename(basename + ".[TRAILER-ru]" + trailerExt);
            String trailerFileName = parentPath + File.separator + trailerBasename;

            int slash = mf.getFilename().lastIndexOf("/");
            String playPath = slash == -1 ? mf.getFilename() : new String(mf.getFilename().substring(0, slash));
            String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);

            logger.debug("KinoPoisk Plugin: Found trailer: " + trailerUrl);
            logger.debug("KinoPoisk Plugin: Download path: " + trailerFileName);
            logger.debug("KinoPoisk Plugin:      Play URL: " + trailerPlayFileName);
            File trailerFile = new File(trailerFileName);

            // Check if the file already exists - after jukebox directory was deleted for example
            if (trailerFile.exists()) {
                logger.debug("KinoPoisk Plugin: Trailer file (" + trailerPlayFileName + ") already exist for " + movie.getBaseName());
                tmf.setFilename(trailerPlayFileName);
                movie.addExtraFile(new ExtraFile(tmf));
            } else if (trailerDownload(movie, trailerUrl, trailerFile)) {
                tmf.setFilename(trailerPlayFileName);
                movie.addExtraFile(new ExtraFile(tmf));
            }

            movie.setTrailerExchange(true);
        } else {
            tmf.setFilename(trailerUrl);
            movie.addExtraFile(new ExtraFile(tmf));
            movie.setTrailerExchange(true);
        }
    }

    protected String getTrailerUrl(Movie movie) {

        // Copied from ComingSoonPlugin.java

        String trailerUrl = Movie.UNKNOWN;
        String kinopoiskId = movie.getId(KINOPOISK_PLUGIN_ID);
        if (StringTools.isNotValidString(kinopoiskId)) {
            return Movie.UNKNOWN;
        }
        try {
            String searchUrl = "http://www.kinopoisk.ru/level/16/film/" + kinopoiskId;
            logger.debug("KinoPoisk Plugin: searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);

            int beginIndex = xml.indexOf("/level/16/film/" + kinopoiskId + "/t/");
            if (beginIndex < 0) {
                // No link to movie page found. We have been redirected to the general video page
                logger.debug("KinoPoisk Plugin: no video found for movie " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String xmlUrl = new String("http://www.kinopoisk.ru" + xml.substring(beginIndex, xml.indexOf("/\"", beginIndex)));
            if (StringTools.isNotValidString(xmlUrl)) {
                logger.debug("KinoPoisk Plugin: no downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("<a href=\"/getlink.php");
            if (beginUrl >= 0) {
                while (trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1) > 0) {
                    beginUrl = trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1);
                }
                beginUrl = trailerXml.indexOf("http://", beginUrl);
                trailerUrl = new String(trailerXml.substring(beginUrl, trailerXml.indexOf("\"", beginUrl)));
            } else {
                logger.error("KinoPoisk Plugin: cannot find trailer URL in XML. Layout changed?");
            }
        } catch (Exception error) {
            logger.error("KinoPoisk Plugin: Failed retreiving trailer for movie: " + movie.getTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return Movie.UNKNOWN;
        }
        return trailerUrl;
    }

    private boolean trailerDownload(final IMovieBasicInformation movie, String trailerUrl, File trailerFile) {

        // Copied from AppleTrailersPlugin.java

        URL url;
        try {
            url = new URL(trailerUrl);
        } catch (MalformedURLException e) {
            return false;
        }

        ThreadExecutor.enterIO(url);
        HttpURLConnection connection = null;
        Timer timer = new Timer();
        try {
            logger.info("KinoPoisk Plugin: Download trailer for " + movie.getBaseName());
            final WebStats stats = WebStats.make(url);
            // after make!
            timer.schedule(new TimerTask() {
                private String lastStatus = "";
                public void run() {
                    String status = stats.calculatePercentageComplete();
                    // only print if percentage changed
                    if (status.equals(lastStatus)) {
                        return;
                    }
                    lastStatus = status;
                    // this runs in a thread, so there is no way to output on one line...
                    // try to keep it visible at least...
                    System.out.println("Downloading trailer for " + movie.getTitle() + ": " + stats.statusString());
                }
            }, 1000, 1000);

            connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("User-Agent", "QuickTime/7.6.9");
            InputStream inputStream = connection.getInputStream();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                logger.error("KinoPoisk Plugin: Download Failed");
                return false;
            }

            FileTools.copy(inputStream, new FileOutputStream(trailerFile), stats);
            System.out.println("Downloading trailer for " + movie.getTitle() + ": " + stats.statusString()); // Output the final stat information (100%)

            return true;

        } catch (Exception error) {
            logger.error("KinoPoisk Plugin: Download Exception");
            return false;
        } finally {
            timer.cancel();         // Close the timer
            if(connection != null){
                connection.disconnect();
            }
            ThreadExecutor.leaveIO();
        }
    }
}
