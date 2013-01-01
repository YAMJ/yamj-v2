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
