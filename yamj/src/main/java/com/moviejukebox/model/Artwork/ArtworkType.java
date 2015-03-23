/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.model.Artwork;

import org.apache.commons.lang3.StringUtils;

public enum ArtworkType {
    // Define the lowercase equivalents of the Enum names

    POSTER, // Thumbnail is a sub-type of poster
    FANART,
    BANNER,
    CLEARART,
    CLEARLOGO,
    TVTHUMB,
    SEASONTHUMB,
    CHARACTERART,
    MOVIEART,
    MOVIELOGO,
    MOVIEDISC,
    VIDEOIMAGE;    // We don't store VideoImages in this artwork type as it's specific to a video file

    /**
     * Convert a string into an Enum type
     *
     * @param artworkTypeString
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static ArtworkType fromString(String artworkTypeString) {
        if (StringUtils.isNotBlank(artworkTypeString)) {
            try {
                return ArtworkType.valueOf(artworkTypeString.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ArtworkType " + artworkTypeString + " does not exist.", ex);
            }
        }
        // We've not found the type, so raise an exception
        throw new IllegalArgumentException("ArtworkType " + artworkTypeString + " does not exist.");
    }
}
