/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
import com.moviejukebox.plugin.FilmUpITPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;

import org.apache.log4j.Logger;

public class FilmUpItPosterPlugin extends AbstractMoviePosterPlugin {

    private WebBrowser webBrowser;
    private FilmUpITPlugin filmupitPlugin;

    private static final Logger LOG = Logger.getLogger(FilmUpItPosterPlugin.class);
    private static final String LOG_MESSAGE = "FilmUpItPosterPlugin: ";

    public FilmUpItPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        webBrowser = new WebBrowser();
        filmupitPlugin = new FilmUpITPlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return filmupitPlugin.getMovieId(title, year);
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml;

        try {
            xml = webBrowser.request("http://filmup.leonardo.it/sc_" + id + ".htm");
            String posterPageUrl = HTMLTools.extractTag(xml, "href=\"posters/locp/", "\"");

            String baseUrl = "http://filmup.leonardo.it/posters/locp/";
            xml = webBrowser.request(baseUrl + posterPageUrl);
            String tmpPosterURL = HTMLTools.extractTag(xml, "\"../loc/", "\"");
            if (StringTools.isValidString(tmpPosterURL)) {
                posterURL = "http://filmup.leonardo.it/posters/loc/" + tmpPosterURL;
                LOG.debug(LOG_MESSAGE + "Movie PosterURL : " + posterPageUrl);
            }

        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed retreiving poster : " + id);
            LOG.error(SystemTools.getStackTrace(error));
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
