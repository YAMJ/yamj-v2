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
package com.moviejukebox.allocine;

import com.moviejukebox.allocine.jaxb.*;

/**
 *  This is the TvSeason bean for the api.allocine.fr search
 *
 *  @author Yves.Blusseau
 */
public class TvSeasonInfos extends Season {

    public TvSeasonInfos() {
        setCode(-1); // Mark the object as invalid
    }

    public boolean isValid() {
        return getCode() > -1 ? true : false;
    }

    public boolean isNotValid() {
        return getCode() > -1 ? false : true;
    }

    public Episode getEpisode(int numEpisode) {
        Episode episode = null;
        for (Episode checkEpisode : getEpisodeList()) {
            if (checkEpisode.getEpisodeNumberSeason() == numEpisode) {
                episode = checkEpisode;
                break;
            }
        }
        return episode;
    }

}
