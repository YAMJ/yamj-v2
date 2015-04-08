/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author altman.matthew
 */
public class ExtraFile extends MovieFile {

    public ExtraFile() {
    }

    public ExtraFile(MovieFile mf) {
        this.setFile(mf.getFile());
        this.setFilename(mf.getFilename());
        this.setPart(mf.getFirstPart());
        this.setTitle(mf.getTitle());
        this.setNewFile(mf.isNewFile());
    }

    @Override
    public int compareTo(MovieFile that) {
        return this.getFilename().compareToIgnoreCase(that.getFilename());
    }

    @Override
    public Map<String, String> getPlayLink() {
        Map<String, String> result;
        if (this.getFile() != null) {
            result = super.getPlayLink();
        } else {
            result = new HashMap<>();
            result.put("URL", this.getFilename());
        }
        return result;
    }

}
