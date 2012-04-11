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
