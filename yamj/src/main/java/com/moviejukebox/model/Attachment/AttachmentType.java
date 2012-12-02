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
 * The type of attachment
 *
 * @author modmax
 */
public enum AttachmentType {

    MATROSKA;

    /**
     * Convert a string into an Enum type
     *
     * @param type
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
     */
    public static AttachmentType fromString(String type) {
        if (StringUtils.isNotBlank(type)) {
            try {
                return AttachmentType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("AttachmentType " + type + " does not exist.", ex);
            }
        }
        throw new IllegalArgumentException("AttachmentType must not be null");
    }
}
