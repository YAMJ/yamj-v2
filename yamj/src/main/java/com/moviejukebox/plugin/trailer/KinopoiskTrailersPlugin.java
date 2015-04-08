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
package com.moviejukebox.plugin.trailer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.KinopoiskPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin to retrieve movie data from Russian movie database www.kinopoisk.ru
 *
 * @author Yury Sidorov.
 *
 * First the movie data is searched in IMDB and TheTvDB. After that the movie is
 * searched in kinopoisk and movie data is updated.
 *
 * It is possible to specify URL of the movie page on kinopoisk in the .nfo
 * file. In this case movie data will be retrieved from this page only.
 */
public class KinopoiskTrailersPlugin extends TrailerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(KinopoiskTrailersPlugin.class);
    public static String KINOPOISK_PLUGIN_ID = "kinopoisk";
    private static final List<String> EXTENSIONS = Arrays.asList(PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV MP4 M1V M2V M4V M2P TP TRP M2T MTS ASF RMP4 IMG MK3D FLV").toUpperCase().split(" "));
    private final KinopoiskPlugin pkPlugin = new KinopoiskPlugin();

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
                LOG.debug("Movie has trailers, skipping");
                return false;
            } else {
                Collection<ExtraFile> files = new ArrayList<>();
                movie.setExtraFiles(files);
            }
        }

        String trailerUrl = getTrailerUrl(movie);
        if (StringTools.isNotValidString(trailerUrl)) {
            LOG.debug("No trailer found");
            return false;
        }

        LOG.debug("Found trailer at URL {}", trailerUrl);

        movie.setTrailerLastScan(new Date().getTime());

        String title = "ru";
        ExtraFile extra = new ExtraFile();
        extra.setTitle("TRAILER-" + title);

        boolean isExchangeOk;

        if (isDownload()) {
            isExchangeOk = downloadTrailer(movie, trailerUrl, title, extra);
        } else {
            extra.setFilename(trailerUrl);
            movie.addExtraFile(extra);
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
            LOG.debug("Searching for trailer at URL {}", searchUrl);
            String xml = webBrowser.request(searchUrl);

            int beginIndex = xml.indexOf(siteSuffix + kinopoiskId + "/t/");
            if (beginIndex < 0) {
                // No link to movie page found. We have been redirected to the general video page
                LOG.debug("No video found for movie {}", movie.getTitle());
                return Movie.UNKNOWN;
            }

            String xmlUrl = siteName + xml.substring(beginIndex, xml.indexOf("/\"", beginIndex));
            if (StringTools.isNotValidString(xmlUrl)) {
                LOG.debug("No downloadable trailer found for movie: {}", movie.getTitle());
                return Movie.UNKNOWN;
            }

            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("<a href=\"/getlink.php");
            if (beginUrl >= 0) {
                while (true) {
                    int markerUrl = trailerXml.indexOf("http://", beginUrl);
                    String tmpUrl = trailerXml.substring(markerUrl, trailerXml.indexOf('\"', markerUrl));
                    String ext = tmpUrl.substring(tmpUrl.lastIndexOf('.') + 1).toUpperCase();
                    if (EXTENSIONS.contains(ext)) {
                        trailerUrl = tmpUrl;
                    }
                    beginUrl = trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1);
                    if (trailerXml.indexOf("<a href=\"/getlink.php", beginUrl + 1) <= 0) {
                        break;
                    }
                }
            } else {
                LOG.error("Cannot find trailer URL in XML. Layout changed?");
            }
        } catch (Exception error) {
            LOG.error("Failed retreiving trailer for movie: {}", movie.getTitle());
            LOG.error(SystemTools.getStackTrace(error));
            return Movie.UNKNOWN;
        }
        return trailerUrl;
    }
}
