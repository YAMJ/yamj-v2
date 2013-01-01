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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.KinopoiskPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.*;
import org.apache.log4j.Logger;

/**
 Plugin to retrieve movie data from Russian movie database www.kinopoisk.ru
 @author Yury Sidorov.

 First the movie data is searched in IMDB and TheTvDB.
 After that the movie is searched in kinopoisk and movie data
 is updated.

 It is possible to specify URL of the movie page on kinopoisk in
 the .nfo file. In this case movie data will be retrieved from this page only.
 */
public class KinopoiskTrailersPlugin extends TrailerPlugin {

    private static final Logger logger = Logger.getLogger(KinopoiskTrailersPlugin.class);
    public static String KINOPOISK_PLUGIN_ID = "kinopoisk";
    private static List<String> extensions = Arrays.asList(PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV MP4 M1V M2V M4V M2P TP TRP M2T MTS ASF RMP4 IMG MK3D FLV").toUpperCase().split(" "));
    private KinopoiskPlugin pkPlugin = new KinopoiskPlugin();

    public KinopoiskTrailersPlugin() {
        super();
        trailersPluginName = "KinopoiskTrailers";
        LOG_MESSAGE = "KinopoiskTrailersPlugin: ";
    }

    @Override
    public final boolean generate(Movie movie) {
        if (movie.isExtra() || movie.isTVShow()) {
            return false;
        }

        if (!movie.getExtraFiles().isEmpty()) {
            boolean fileExists = existsTrailerFiles(movie);
            if (fileExists) {
                logger.debug(LOG_MESSAGE + "Movie has trailers, skipping");
                return false;
            } else {
                Collection<ExtraFile> files = new ArrayList<ExtraFile>();
                movie.setExtraFiles(files);
            }
        }

        String trailerUrl = getTrailerUrl(movie);
        if (StringTools.isNotValidString(trailerUrl)) {
            logger.debug(LOG_MESSAGE + "No trailer found");
            return false;
        }

        logger.debug(LOG_MESSAGE + "Found trailer at URL " + trailerUrl);

        movie.setTrailerLastScan(new Date().getTime());

        String title = "ru";
        MovieFile tmf = new MovieFile();
        tmf.setTitle("TRAILER-" + title);

        boolean isExchangeOk;

        if (isDownload()) {
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
            kinopoiskId = pkPlugin.getKinopoiskId(movie);
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
            logger.debug(LOG_MESSAGE + "Searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);

            int beginIndex = xml.indexOf(siteSuffix + kinopoiskId + "/t/");
            if (beginIndex < 0) {
                // No link to movie page found. We have been redirected to the general video page
                logger.debug(LOG_MESSAGE + "No video found for movie " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String xmlUrl = siteName + xml.substring(beginIndex, xml.indexOf("/\"", beginIndex));
            if (StringTools.isNotValidString(xmlUrl)) {
                logger.debug(LOG_MESSAGE + "No downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("<a href=\"/getlink.php");
            if (beginUrl >= 0) {
                while (true) {
                    int markerUrl = trailerXml.indexOf("http://", beginUrl);
                    String tmpUrl = new String(trailerXml.substring(markerUrl, trailerXml.indexOf('\"', markerUrl)));
                    String ext = tmpUrl.substring(tmpUrl.lastIndexOf('.') + 1).toUpperCase();
                    if (extensions.contains(ext)) {
                        trailerUrl = tmpUrl;
                    }
                    beginUrl = trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1);
                    if (trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1) <= 0) {
                        break;
                    }
                }
            } else {
                logger.error(LOG_MESSAGE + "Cannot find trailer URL in XML. Layout changed?");
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving trailer for movie: " + movie.getTitle());
            logger.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
        return trailerUrl;
    }
}
