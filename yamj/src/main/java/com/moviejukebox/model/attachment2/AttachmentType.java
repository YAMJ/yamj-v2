/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.model.attachment2;

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
