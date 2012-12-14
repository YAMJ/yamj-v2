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
     * @param artworkType
     * @return
     * @throws IllegalArgumentException If type is not recognised
     *
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
