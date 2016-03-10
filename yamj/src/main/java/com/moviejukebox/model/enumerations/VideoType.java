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
package com.moviejukebox.model.enumerations;

import java.util.EnumSet;
import org.apache.commons.lang3.StringUtils;

public enum VideoType {
    MOVIE("Movie"),
    TV_SHOW("TV Show"),
    TVSHOW("TVShow"),
    EXTRA("Extra"),
    ALL("All");

    private final String type;

    private VideoType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Convert a string into an Enum type
     *
     * @param sType
     * @return
     */
    public static VideoType fromString(String sType) {
        if (StringUtils.isNotBlank(sType)) {
            for (final VideoType extension : EnumSet.allOf(VideoType.class)) {
                if (sType.equalsIgnoreCase(extension.type)) {
                    return extension;
                }
            }
        }
        // We've not found the type, so return both
        return ALL;
    }

    /**
     * Compare the passed string to this enum
     *
     * @param test
     * @return
     */
    public boolean compare(String test) {
        return this.getType().equalsIgnoreCase(test);
    }
}
