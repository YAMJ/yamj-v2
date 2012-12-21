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

/**
 * The list of override flags
 *
 * @author modmax
 */
public enum OverrideFlag {

    UNKNOWN,
    ACTORS,
    ASPECTRATIO,
    CERTIFICATION,
    COMPANY,
    CONTAINER,
    COUNTRY,
    DIRECTORS,
    FPS, // frames per second
    GENRES,
    LANGUAGE,
    ORIGINALTITLE, 
    OUTLINE,
    PLOT,
    QUOTE,
    RELEASEDATE,
    RESOLUTION,
    RUNTIME,
    TAGLINE,
    TITLE,
    VIDEOOUTPUT,
    VIDEOSOURCE,
    WRITERS,
    YEAR,
    // extra for PEOPLE scraping
    PEOPLE_ACTORS,
    PEOPLE_DIRECTORS,
    PEOPLE_WRITERS;

    /**
     * Convert a string into an Enum type
     *
     * @param overrideFlag
     * @return the Enum, may be UNKNOWN if given string could not be parsed
     */
    public static OverrideFlag fromString(String overrideFlag) {
        try {
            return OverrideFlag.valueOf(overrideFlag.trim().toUpperCase());
        } catch (Exception ignore) {}
            return UNKNOWN;
    }
}
