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

import com.moviejukebox.allocine.model.MovieInfos;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocinePosterPlugin extends AbstractMoviePosterPlugin {

    private AllocinePlugin allocinePlugin;
    private static final Logger LOG = LoggerFactory.getLogger(AllocinePosterPlugin.class);

    public AllocinePosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        allocinePlugin = new AllocinePlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return allocinePlugin.getMovieId(title, year, -1);
    }

    @Override
    public IImage getPosterUrl(String id) {

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {

            MovieInfos movieInfos = allocinePlugin.getMovieInfos(id);

            if (movieInfos.isValid() && movieInfos.getPosterUrls().size() > 0) {
                String posterURL = movieInfos.getPosterUrls().iterator().next();
                if (StringTools.isValidString(posterURL)) {
                    return new Image(posterURL);
                }
            }
        }
        LOG.debug("No poster found at allocine for movie id '{}'", id);
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "allocine";
    }
}
