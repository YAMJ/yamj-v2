/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.model.enumerations;

import java.util.EnumSet;
import org.apache.commons.lang3.StringUtils;

public enum TitleSortType {

    TITLE,
    FILENAME,
    ORIGINAL,
    ADOPT_ORIGINAL;

    /**
     * Convert a string into an Enum type
     *
     * @param titleSortTypeString
     * @return TitleSortType
     */
    public static TitleSortType fromString(String titleSortTypeString) {
        if (StringUtils.isNotBlank(titleSortTypeString)) {
            for (final TitleSortType titleSortType : EnumSet.allOf(TitleSortType.class)) {
                if (titleSortTypeString.equalsIgnoreCase(titleSortType.toString().toLowerCase())) {
                    return titleSortType;
                }
            }
        }
        // We've not found the type, so use the default
        return TitleSortType.TITLE;
    }
}
