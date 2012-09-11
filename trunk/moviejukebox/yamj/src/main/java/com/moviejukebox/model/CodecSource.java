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
