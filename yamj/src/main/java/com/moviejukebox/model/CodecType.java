package com.moviejukebox.model;

import org.apache.commons.lang3.StringUtils;

/**
 * The type of codec
 *
 * @author stuart.boston
 */
public enum CodecType {

    AUDIO,
    VIDEO;

    /**
     * Convert a string into an Enum type
     *
     * @param type
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static CodecType fromString(String type) {
        if (StringUtils.isNotBlank(type)) {
            try {
                return CodecType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("CodecType " + type + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("CodecType must not be null");
    }
}
