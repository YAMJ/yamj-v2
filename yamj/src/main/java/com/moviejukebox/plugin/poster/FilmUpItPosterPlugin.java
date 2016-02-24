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

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.FilmUpITPlugin;
import com.moviejukebox.tools.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilmUpItPosterPlugin extends AbstractMoviePosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FilmUpItPosterPlugin.class);

    private YamjHttpClient httpClient;
    private FilmUpITPlugin filmupitPlugin;

    public FilmUpItPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        httpClient = YamjHttpClientBuilder.getHttpClient();
        filmupitPlugin = new FilmUpITPlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return filmupitPlugin.getMovieId(title);
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml;

        try {
            xml = httpClient.request("http://filmup.leonardo.it/sc_" + id + ".htm");
            String posterPageUrl = HTMLTools.extractTag(xml, "href=\"posters/locp/", "\"");

            String baseUrl = "http://filmup.leonardo.it/posters/locp/";
            xml = httpClient.request(baseUrl + posterPageUrl);
            String tmpPosterURL = HTMLTools.extractTag(xml, "\"../loc/", "\"");
            if (StringTools.isValidString(tmpPosterURL)) {
                posterURL = "http://filmup.leonardo.it/posters/loc/" + tmpPosterURL;
                LOG.debug("Movie PosterURL: {}", posterPageUrl);
            }
        } catch (IOException ex) {
            LOG.error("Failed retreiving poster: {}", id);
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
        return FilmUpITPlugin.FILMUPIT_PLUGIN_ID;
    }
}
