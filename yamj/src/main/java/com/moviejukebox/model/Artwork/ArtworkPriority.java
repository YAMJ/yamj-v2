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
package com.moviejukebox.model.Artwork;

import org.apache.commons.lang3.StringUtils;

/**
 * List of priorities to use for scanning local artwork
 *
 * @author stuart.boston
 */
public enum ArtworkPriority {

    /*
     * Artwork located with the video file
     */
    VIDEO,
    /*
     * Artwork that is named the same as the folder that it is in
     */
    FOLDER,
    /*
     * Artwork that has a fixed name, e.g. Backdrop.jpg
     */
    FIXED,
    /*
     * Artwork for a TV Series, often stored in the parent directory
     */
    SERIES,
    /*
     * Look in an alternative directory
     */
    DIRECTORY;

    /**
     * Convert a string into an Enum type
     *
     * @param artworkPriority
     * @return
     * @throws IllegalArgumentException If not recognised
     *
     */
    public static ArtworkPriority fromString(final String artworkPriority) throws IllegalArgumentException {
        if (StringUtils.isNotBlank(artworkPriority)) {
            try {
                return ArtworkPriority.valueOf(artworkPriority.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ArtworkPriority '" + artworkPriority + "' does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("ArtworkPriority must not be null");
    }
}
