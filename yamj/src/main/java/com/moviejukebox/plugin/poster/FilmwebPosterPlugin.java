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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.FilmwebPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import org.apache.log4j.Logger;

public class FilmwebPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {

    private WebBrowser webBrowser;
    private FilmwebPlugin filmwebPlugin;
    private static final Logger logger = Logger.getLogger(FilmwebPosterPlugin.class);

    public FilmwebPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        try {
            webBrowser = new WebBrowser();
            filmwebPlugin = new FilmwebPlugin();
            // first request to filmweb site to skip welcome screen with ad banner
            webBrowser.request("http://www.filmweb.pl");
        } catch (IOException error) {
            logger.error("Error : " + error.getMessage());
        }
    }

    public String getIdFromMovieInfo(String title, String year) {
        return filmwebPlugin.getFilmwebUrl(title, year);
    }

    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        String xml;
        try {
            xml = webBrowser.request(id);
            posterURL = HTMLTools.extractTag(xml, "posterLightbox", 3, "\"");
        } catch (Exception error) {
            logger.error("Failed retreiving filmweb poster for movie : " + id);
            logger.error(SystemTools.getStackTrace(error));
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    public String getName() {
        return FilmwebPlugin.FILMWEB_PLUGIN_ID;
    }
}
