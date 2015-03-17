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
 * What is the source of the codec information
 *
 * @author stuart.boston
 */
public enum CodecSource {
    // List the codecs in preference order

    MEDIAINFO,
    NFO,
    FILENAME,
    UNKNOWN;

    /**
     * Determine if this codec source is better than another
     * @param other
     * @return
     */
    public boolean isBetter(CodecSource other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Convert a string into an Enum type
     *
     * @param source
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static CodecSource fromString(String source) {
        if (StringUtils.isNotBlank(source)) {
            try {
                return CodecSource.valueOf(source.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("CodecSource " + source + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("CodecSource must not be null");
    }
}
