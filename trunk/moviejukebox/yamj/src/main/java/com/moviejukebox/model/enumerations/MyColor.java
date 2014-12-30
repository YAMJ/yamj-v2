/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
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

import java.awt.Color;

/**
 * YAMJ Colors
 * @author Stuart
 */
public enum MyColor {

    white(Color.white), WHITE(Color.WHITE),
    lightGray(Color.lightGray), LIGHT_GRAY(Color.LIGHT_GRAY),
    gray(Color.gray), GRAY(Color.GRAY),
    darkGray(Color.darkGray), DARK_GRAY(Color.DARK_GRAY),
    black(Color.black), BLACK(Color.BLACK),
    red(Color.red), RED(Color.RED),
    pink(Color.pink), PINK(Color.PINK),
    orange(Color.orange), ORANGE(Color.ORANGE),
    yellow(Color.yellow), YELLOW(Color.YELLOW),
    green(Color.green), GREEN(Color.GREEN),
    magenta(Color.magenta), MAGENTA(Color.MAGENTA),
    cyan(Color.cyan), CYAN(Color.CYAN),
    blue(Color.blue), BLUE(Color.BLUE);
    private final Color color;

    // Constructor
    MyColor(Color color) {
        this.color = color;
    }

    public static Color get(String name) {
        for (MyColor aColor : MyColor.values()) {
            if (aColor.toString().equalsIgnoreCase(name)) {
                return aColor.color;
            }
        }
        return null;
    }
}
