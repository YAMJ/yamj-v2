/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

/**
 * 
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
}
