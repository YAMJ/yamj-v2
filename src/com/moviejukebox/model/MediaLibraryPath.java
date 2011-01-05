/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

public class MediaLibraryPath {

    String path;
    String playerRootPath;
    Collection<String> excludes;
    String description;
    boolean scrapeLibrary = true;
    long prebuf = -1;

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

    public void setExcludes(Collection<String> excludes) {
        if (excludes == null) {
            this.excludes = new ArrayList<String>();
        } else {
            this.excludes = excludes;
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
        StringBuffer sb = new StringBuffer("[MediaLibraryPath");
        sb.append("[path=").append(path).append("]");
        sb.append("[playerRootPath=").append(playerRootPath).append("]");
        sb.append("[scrape=").append(scrapeLibrary).append("]");
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
