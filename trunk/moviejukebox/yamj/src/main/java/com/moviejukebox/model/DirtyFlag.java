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
 * The list of dirty flags
 *
 * @author Stuart
 */
public enum DirtyFlag {

    /**
     * The NFO information has changed
     */
    NFO,
    /**
     * The Fanart has changed and may need to be updated/redownloaded
     */
    FANART,
    /**
     * The Poster has changed and may need to be updated/redownloaded
     */
    POSTER,
    /**
     * The Banner has changed and may need to be updated/redownloaded
     */
    BANNER,
    /**
     * The watched value for the video has changed and the information needs to
     * be updated
     */
    WATCHED,
    /**
     * General information about the video has changed that is not specific to
     * one of the other dirty flags
     */
    INFO,
    /**
     * The video has been marked for a recheck.
     *
     * The video should not necessarily need the information to be downloaded
     * from scratch and should instead be considered for update especially for
     * any missing (UNKNOWN) items
     */
    RECHECK,
    /**
     * The ClearArt has changed and may need to be updated/redownloaded
     */
    CLEARART,
    /**
     * The ClearLogo has changed and may need to be updated/redownloaded
     */
    CLEARLOGO,
    /**
     * The TVThumb has changed and may need to be updated/redownloaded
     */
    TVTHUMB,
    /**
     * The SeasonThumb has changed and may need to be updated/redownloaded
     */
    SEASONTHUMB,
    /**
     * The MovieDisc has changed and may need to be updated/redownloaded
     */
    MOVIEDISC;

    /**
     * Convert a string into an Enum type
     *
     * @param dirtyFlag
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static DirtyFlag fromString(String dirtyFlag) {
        if (StringUtils.isNotBlank(dirtyFlag)) {
            try {
                return DirtyFlag.valueOf(dirtyFlag.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("DirtyFlag " + dirtyFlag + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("DirtyFlag must not be null");
    }
}
