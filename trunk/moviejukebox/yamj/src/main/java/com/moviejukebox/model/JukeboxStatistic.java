/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.model;

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
    INDEX_SET;

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
