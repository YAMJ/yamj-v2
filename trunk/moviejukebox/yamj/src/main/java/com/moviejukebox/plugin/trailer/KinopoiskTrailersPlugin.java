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
package com.moviejukebox.plugin.trailer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

import com.moviejukebox.plugin.KinopoiskPlugin;

public class KinopoiskTrailersPlugin extends TrailersPlugin {

    public static String KINOPOISK_PLUGIN_ID = "kinopoisk";

    private static List<String> extensions = Arrays.asList(PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV MP4 M1V M2V M4V M2P TP TRP M2T MTS ASF RMP4 IMG MK3D FLV").toUpperCase().split(" "));
    private KinopoiskPlugin pkPlugin = new KinopoiskPlugin();

    public KinopoiskTrailersPlugin() {
        super();
        trailersPluginName = "KinopoiskTrailers";
    }

    @Override
    public final boolean generate(Movie movie) {
        if (movie.isExtra() || movie.isTVShow()) {
            return false;
        }
        if (!movie.getExtraFiles().isEmpty()) {
            boolean fileExists = existsTrailerFiles(movie);
            if (fileExists) {
                logger.debug(trailersPluginName + " Plugin: Movie has trailers, skipping");
                return false;
            } else {
                Collection<ExtraFile> files = new ArrayList<ExtraFile>();
                movie.setExtraFiles(files);
            }
        }

        String trailerUrl = getTrailerUrl(movie);
        if (StringTools.isNotValidString(trailerUrl)) {
            logger.debug(trailersPluginName + " Plugin: no trailer found");
            return false;
        }

        logger.debug(trailersPluginName + " Plugin: found trailer at URL " + trailerUrl);

        movie.setTrailerLastScan(new Date().getTime());

        String title = "ru";
        MovieFile tmf = new MovieFile();
        tmf.setTitle("TRAILER-" + title);

        boolean isExchangeOk = false;

        if (getDownload()) {
            isExchangeOk = downloadTrailer(movie, trailerUrl, title, tmf);
        } else {
            tmf.setFilename(trailerUrl);
            movie.addExtraFile(new ExtraFile(tmf));
            movie.setTrailerExchange(true);
            isExchangeOk = true;
        }

        return isExchangeOk;
    }

    @Override
    public String getName() {
        return "kinopoisk";
    }

    protected String getTrailerUrl(Movie movie) {
        String trailerUrl = Movie.UNKNOWN;
        String kinopoiskId = movie.getId(KINOPOISK_PLUGIN_ID);
        if (StringTools.isNotValidString(kinopoiskId)) {
            String year = movie.getYear();
            kinopoiskId = pkPlugin.getKinopoiskId(movie.getOriginalTitle(), year, movie.getSeason());
            if (StringTools.isValidString(year) && StringTools.isNotValidString(kinopoiskId)) {
                kinopoiskId = pkPlugin.getKinopoiskId(movie.getOriginalTitle(), Movie.UNKNOWN, movie.getSeason());
            }
            if (StringTools.isNotValidString(kinopoiskId)) {
                return Movie.UNKNOWN;
            }
        }
        try {
            String siteName = "http://www.kinopoisk.ru";
            String siteSuffix = "/level/16/film/";
            String searchUrl = siteName + siteSuffix + kinopoiskId;
            logger.debug(trailersPluginName + " Plugin: searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);

            int beginIndex = xml.indexOf(siteSuffix + kinopoiskId + "/t/");
            if (beginIndex < 0) {
                // No link to movie page found. We have been redirected to the general video page
                logger.debug(trailersPluginName + " Plugin: no video found for movie " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String xmlUrl = new String(siteName + xml.substring(beginIndex, xml.indexOf("/\"", beginIndex)));
            if (StringTools.isNotValidString(xmlUrl)) {
                logger.debug(trailersPluginName + " Plugin: no downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("<a href=\"/getlink.php");
            if (beginUrl >= 0) {
                while (true) {
                    int markerUrl = trailerXml.indexOf("http://", beginUrl);
                    String tmpUrl = new String(trailerXml.substring(markerUrl, trailerXml.indexOf("\"", markerUrl)));
                    String ext = new String(tmpUrl.substring(tmpUrl.lastIndexOf(".") + 1).toUpperCase());
                    if (extensions.contains(ext)) {
                        trailerUrl = tmpUrl;
                    }
                    beginUrl = trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1);
                    if (trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1) <= 0) {
                        break;
                    }
                }
            } else {
                logger.error(trailersPluginName + " Plugin: cannot find trailer URL in XML. Layout changed?");
            }
        } catch (Exception error) {
            logger.error(trailersPluginName + " Plugin: Failed retreiving trailer for movie: " + movie.getTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return Movie.UNKNOWN;
        }
        return trailerUrl;
    }
}
