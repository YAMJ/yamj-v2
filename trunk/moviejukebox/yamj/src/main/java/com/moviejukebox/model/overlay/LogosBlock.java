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
 * Logos Block Overlay
 *
 * @author Stuart
 */
public class LogosBlock {

    private static final String AUTO = "auto";
    // Properties
    private boolean dir = Boolean.FALSE;         // true - vertical, false - horizontal,
    private boolean size = Boolean.FALSE;        // true - static, false - auto
    private boolean clones = Boolean.FALSE;      // true - enabled clones
    private Integer cols = 1;            // 0 - auto count
    private Integer rows = 0;            // 0 - auto count
    private Integer hMargin = 0;
    private Integer vMargin = 0;

    public LogosBlock(boolean dir, boolean size, String cols, String rows, String hMargin, String vMargin, boolean clones) {
        this.dir = dir;
        this.size = size;
        this.clones = clones;
        this.cols = cols.equalsIgnoreCase(AUTO) ? 0 : Integer.parseInt(cols);
        this.rows = rows.equalsIgnoreCase(AUTO) ? 0 : Integer.parseInt(rows);
        this.hMargin = Integer.parseInt(hMargin);
        this.vMargin = Integer.parseInt(vMargin);
    }

    public boolean isDir() {
        return dir;
    }

    public void setDir(boolean dir) {
        this.dir = dir;
    }

    public boolean isSize() {
        return size;
    }

    public void setSize(boolean size) {
        this.size = size;
    }

    public boolean isClones() {
        return clones;
    }

    public void setClones(boolean clones) {
        this.clones = clones;
    }

    public Integer getCols() {
        return cols;
    }

    public void setCols(Integer cols) {
        this.cols = cols;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer gethMargin() {
        return hMargin;
    }

    public void sethMargin(Integer hMargin) {
        this.hMargin = hMargin;
    }

    public Integer getvMargin() {
        return vMargin;
    }

    public void setvMargin(Integer vMargin) {
        this.vMargin = vMargin;
    }
}
