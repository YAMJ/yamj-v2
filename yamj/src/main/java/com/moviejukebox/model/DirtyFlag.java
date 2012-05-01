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

    NFO,
    FANART,
    POSTER,
    BANNER,
    WATCHED,
    INFO,
    RECHECK,
    CLEARART,
    CLEARLOGO,
    TVTHUMB,
    SEASONTHUMB,
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
