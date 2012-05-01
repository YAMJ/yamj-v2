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
package com.moviejukebox.model.Artwork;

import org.apache.commons.lang3.StringUtils;

public enum ArtworkSize {

    SMALL,
    MEDIUM,
    LARGE;

    /**
     * Convert a string into an Enum type
     *
     * @param artworkSize
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static ArtworkSize fromString(String artworkSize) {
        if (StringUtils.isNotBlank(artworkSize)) {
            try {
                return ArtworkSize.valueOf(artworkSize.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ArtworkSize " + artworkSize + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("ArtworkSize must not be null");
    }
}
