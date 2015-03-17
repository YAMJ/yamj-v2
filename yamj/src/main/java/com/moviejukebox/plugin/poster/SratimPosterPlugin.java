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
import com.moviejukebox.plugin.SratimPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SratimPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(SratimPosterPlugin.class);

    private WebBrowser webBrowser;
    private SratimPlugin sratimPlugin;

    public SratimPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        webBrowser = new WebBrowser();
        sratimPlugin = new SratimPlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        Movie movie = new Movie();
        movie.setTitle(title, Movie.UNKNOWN);
        movie.setYear(year, Movie.UNKNOWN);
        return sratimPlugin.getSratimId(movie);
    }

    @Override
    public IImage getPosterUrl(String id) {
        try {
            String xml = webBrowser.request("http://sratim.co.il/view.php?id=" + id);
            String posterURL = HTMLTools.extractTag(xml, "<a href=\"photos/titles/normal/", 0, "\"");

            if (StringTools.isValidString(posterURL)) {
                posterURL = "http://sratim.co.il/photos/titles/normal/" + posterURL;
                return new Image(posterURL);
            }
        } catch (IOException error) {
            LOG.error("Failed retreiving poster for movie: {}", id);
            LOG.error(SystemTools.getStackTrace(error));
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
        return SratimPlugin.SRATIM_PLUGIN_ID;
    }

}
