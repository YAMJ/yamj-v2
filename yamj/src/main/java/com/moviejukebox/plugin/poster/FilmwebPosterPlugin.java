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
package com.moviejukebox.plugin.poster;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.YamjHttpClient;
import com.moviejukebox.tools.YamjHttpClientBuilder;

public class FilmwebPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {

    private YamjHttpClient httpClient;
    private FilmwebPlugin filmwebPlugin;
    private static final Logger LOG = LoggerFactory.getLogger(FilmwebPosterPlugin.class);

    public FilmwebPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        try {
            httpClient = YamjHttpClientBuilder.getHttpClient();
            filmwebPlugin = new FilmwebPlugin();
            // first request to filmweb site to skip welcome screen with ad banner
            httpClient.request("http://www.filmweb.pl");
        } catch (IOException ex) {
            LOG.error("Error: {}", ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        }
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return filmwebPlugin.getMovieId(title, year);
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml;
        try {
            xml = httpClient.request(id);
            posterURL = HTMLTools.extractTag(xml, "posterLightbox", 3, "\"");
        } catch (Exception ex) {
            LOG.error("Failed retreiving filmweb poster for movie: {}", id);
            LOG.error(SystemTools.getStackTrace(ex));
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getName() {
        return FilmwebPlugin.FILMWEB_PLUGIN_ID;
    }
}
