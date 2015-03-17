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
package com.moviejukebox.model.overlay;

/**
 * Overlay configuration XML
 *
 * Originally Issue 1937
 *
 * @author Stuart
 */
public class PositionOverlay {

    // Literals
    private static final String LEFT = "left";
    private static final String TOP = "top";
    private static final String EQUAL = "equal";
    // Properties
    private Integer left;
    private Integer top;
    private String align;
    private String valign;
    private String width;
    private String height;

    public PositionOverlay() {
        left = 0;
        top = 0;
        align = LEFT;
        valign = TOP;
        width = EQUAL;
        height = EQUAL;
    }

    public PositionOverlay(Integer left, Integer top, String align, String valign, String width, String height) {
        this.left = left;
        this.top = top;
        this.align = align;
        this.valign = valign;
        this.width = width;
        this.height = height;
    }

    public Integer getLeft() {
        return left;
    }

    public void setLeft(Integer left) {
        this.left = left;
    }

    public Integer getTop() {
        return top;
    }

    public void setTop(Integer top) {
        this.top = top;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getValign() {
        return valign;
    }

    public void setValign(String valign) {
        this.valign = valign;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }
}
