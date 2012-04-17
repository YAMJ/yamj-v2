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

import java.util.EnumSet;

public enum ArtworkType {
    // Define the lowercase equivalents of the Enum names

    Poster("poster"), // Thumbnail is a sub-type of poster
    Fanart("fanart"),
    Banner("banner"),
    ClearArt("clearart"),
    ClearLogo("clearlogo"),
    TvThumb("tvthumb"),
    SeasonThumb("seasonthumb"),
    MovieDisc("moviedisc"),
    VideoImage("videoimage");    // We don't store VideoImages in this artwork type as it's specific to a video file
    private String type;

    /**
     * Constructor
     *
     * @param type
     */
    private ArtworkType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public static ArtworkType fromString(String type) {
        if (type != null) {
            for (final ArtworkType artworkType : EnumSet.allOf(ArtworkType.class)) {
                if (type.equalsIgnoreCase(artworkType.type)) {
                    return artworkType;
                }
            }
        }
        // We've not found the type, so raise an exception
        throw new IllegalArgumentException("No ArtworkType " + type + " exists");
    }
}
