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

/**
 * The Allocine API.
 * This is for version 3 of the API as specified here: http://wiki.gromez.fr/dev/api/allocine_v3  
 */
public interface AllocineAPIHelper {

    void setProxy(String proxyHost, int proxyPort);
    
    Search searchMovieInfos(String query) throws Exception;

    Search searchTvseriesInfos(String query) throws Exception;

    MovieInfos getMovieInfos(String allocineId) throws Exception;

    TvSeriesInfos getTvSeriesInfos(String allocineId) throws Exception;

    TvSeasonInfos getTvSeasonInfos(Integer seasonCode) throws Exception;
}
