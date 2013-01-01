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
package com.moviejukebox.model.Attachment;

import org.apache.commons.lang3.StringUtils;

/**
 * The type of attachment content
 *
 * @author modmax
 */
public enum ContentType {

    NFO,
    POSTER,
    FANART,
    BANNER,
    SET_POSTER, // poster for a set
    SET_FANART, // fanart for a set
    SET_BANNER, // banner for a set
    VIDEOIMAGE;

    /**
     * Convert a string into an Enum type
     *
     * @param type
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static ContentType fromString(String type) {
        if (StringUtils.isNotBlank(type)) {
            try {
                return ContentType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ContentType " + type + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("ContentType must not be null");
    }
}
