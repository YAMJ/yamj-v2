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
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MediaLibraryPath {

    private String path;
    private String playerRootPath;
    private Collection<String> excludes = Collections.synchronizedCollection(new ArrayList<String>());
    private String description;
    private boolean scrapeLibrary = true;
    private long prebuf = -1;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPlayerRootPath() {
        return playerRootPath;
    }

    public void setPlayerRootPath(String playerRootPath) {
        this.playerRootPath = playerRootPath;
    }

    public Collection<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Collection<Object> excludes) {
        if (excludes == null) {
            this.excludes.clear();
        } else {
            for (Object excludeObject : excludes) {
                this.excludes.add((String) excludeObject);
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[MediaLibraryPath");
        sb.append("[path=").append(path).append("]");
        sb.append("[playerRootPath=").append(playerRootPath).append("]");
        sb.append("[scrape=").append(scrapeLibrary).append("]");
        sb.append("[description=").append(description).append("]");
        for (String excluded : excludes) {
            sb.append("[excludes=").append(excluded).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public long getPrebuf() {
        return prebuf;
    }

    public void setPrebuf(long prebuf) {
        this.prebuf = prebuf;
    }

    public boolean isScrapeLibrary() {
        return this.scrapeLibrary;
    }

    public void setScrapeLibrary(boolean scrapeLibrary) {
        this.scrapeLibrary = scrapeLibrary;
    }
}
