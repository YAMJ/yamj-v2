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
package com.moviejukebox.model.enumerations;

import org.apache.commons.lang3.StringUtils;

/**
 * List of statistics available for the jukebox
 *
 * Should be extended if more statistics are required.
 *
 * @author stuart.boston
 */
public enum JukeboxStatistic {

    /*
     * Number of videos scanned
     */
    VIDEOS,
    /*
     * Number of movies in the jukebox
     */
    MOVIES,
    /*
     * Number of TV Shows in the jukebox
     */
    TVSHOWS,
    /*
     * Number of Sets in the jukebox
     */
    SETS,
    /*
     * How many new videos were scanned
     */
    NEW_VIDEOS,
    /*
     * How many existing videos were scanned
     */
    EXISTING_VIDEOS,
    /*
     * How many videos are in the "Other" index
     */
    INDEX_OTHER,
    /*
     * How many videos are in the "Genres" index
     */
    INDEX_GENRES,
    /*
     * How many videos are in the "Title" index
     */
    INDEX_TITLE,
    /*
     * How many videos are in the "Certification" index
     */
    INDEX_CERTIFICATION,
    /*
     * How many videos are in the "Year" index
     */
    INDEX_YEAR,
    /*
     * How many videos are in the "Library" index
     */
    INDEX_LIBRARY,
    /*
     * How many videos are in the "Set" index
     */
    INDEX_SET,
    /*
     * How many videos are in the "Country" index
     */
    INDEX_COUNTRY,
    /*
     * How many videos are in the "Cast" index
     */
    INDEX_CAST,
    /*
     * How many videos are in the "Director" index
     */
    INDEX_DIRECTOR,
    /*
     * How many videos are in the "Writer" index
     */
    INDEX_WRITER,
    /*
     * How many videos are in the "Award" index
     */
    INDEX_AWARD,
    /*
     * How many videos are in the "Person" index
     */
    INDEX_PERSON,
    /*
     * How many videos are in the "Ratings" index
     */
    INDEX_RATINGS;

    /**
     * Convert a string into an Enum type
     *
     * @param jukeboxStatistic
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static JukeboxStatistic fromString(String jukeboxStatistic) {
        if (StringUtils.isNotBlank(jukeboxStatistic)) {
            try {
                return JukeboxStatistic.valueOf(jukeboxStatistic.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("JukeboxStatistic " + jukeboxStatistic + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("JukeboxStatistic must not be null");
    }
}
