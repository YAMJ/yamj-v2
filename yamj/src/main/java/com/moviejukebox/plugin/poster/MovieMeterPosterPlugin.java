/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieMeterPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.moviemeter.MovieMeterApi;
import com.omertron.moviemeter.MovieMeterException;
import com.omertron.moviemeter.model.FilmInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieMeterPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterPosterPlugin.class);
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_MovieMeter");
    private static CloseableHttpClient httpClient;
    private static MovieMeterApi api;

    public MovieMeterPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        try {
            httpClient = WebBrowser.getCloseableHttpClient();
            api = new MovieMeterApi(API_KEY, httpClient);
        } catch (MovieMeterException ex) {
            LOG.warn("Failed to initialise MovieMeter API: {}", ex.getMessage(), ex);
            return;
        }

        PropertiesUtil.warnDeprecatedProperty("moviemeter.id.search");
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return MovieMeterPlugin.getMovieId(api, title, year);
    }

    @Override
    public IImage getPosterUrl(String id) {
        if (!StringUtils.isNumeric(id)) {
            return Image.UNKNOWN;
        }

        String posterURL = Movie.UNKNOWN;
        FilmInfo filmInfo;

        try {
            filmInfo = api.getFilm(NumberUtils.toInt(id));
            if (filmInfo.getPosters() == null || filmInfo.getPosters().getLarge() == null) {
                LOG.debug("No MovieMeter Poster URL for movie: {}", id);
            } else {
                posterURL = filmInfo.getPosters().getLarge();
            }
        } catch (MovieMeterException ex) {
            LOG.error("Failed retreiving MovieMeter Poster URL for movie: {}", id);
            LOG.error(SystemTools.getStackTrace(ex));
        }

        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "moviemeterposter";
    }
}
