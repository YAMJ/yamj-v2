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
