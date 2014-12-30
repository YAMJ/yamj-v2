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

import java.util.EnumSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Location of the watched files
 *
 * @author Stuart
 */
public enum WatchedWithLocation {

    /*
     * Watched file is stored with the video
     */
    WITHVIDEO,
    /*
     * Watched file is stored with the jukebox
     */
    WITHJUKEBOX,
    /*
     * Watched files are stored elsewhere
     */
    CUSTOM;

    /**
     * Convert a string into an Enum type
     *
     * @param location
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static WatchedWithLocation fromString(String location) {
        if (StringUtils.isNotBlank(location)) {
            for (final WatchedWithLocation withLocation : EnumSet.allOf(WatchedWithLocation.class)) {
                if (location.equalsIgnoreCase(withLocation.toString())) {
                    return withLocation;
                }
            }
        }
        // We've not found the type, so reutrn CUSTOM
        return CUSTOM;
    }
}
