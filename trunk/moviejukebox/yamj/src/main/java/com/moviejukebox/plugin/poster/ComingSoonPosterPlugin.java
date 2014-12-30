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
import com.moviejukebox.plugin.ComingSoonPlugin;
import com.moviejukebox.tools.StringTools;

public class ComingSoonPosterPlugin extends AbstractMoviePosterPlugin {

    private static final String POSTER_BASE_URL = "http://www.comingsoon.it/imgdb/locandine/big/";

    public ComingSoonPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return Movie.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String id) {
        if (StringTools.isNotValidString(id) || id.equals(ComingSoonPlugin.COMINGSOON_NOT_PRESENT)) {
            return Image.UNKNOWN;
        }
        String posterURL;
        posterURL = POSTER_BASE_URL + id + ".jpg";
        return new Image(posterURL);

    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "comingsoon";
    }
}
