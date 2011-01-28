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
package com.moviejukebox.model.Artwork;

import java.awt.Dimension;

import com.moviejukebox.model.Movie;

public class ArtworkFile {
    private ArtworkSize size;       // Descriptive size of the artwork, e.g. Small, medium, large
    private Dimension   dimension;  // Target size of the artwork - populated from the properties file
    private String      filename;   // The filename to save the artwork too
    private boolean     downloaded; // Has the artwork been downloaded (to the jukebox)
    
    public ArtworkFile() {
        this.size       = ArtworkSize.LARGE;
        this.dimension  = new Dimension();
        this.filename   = Movie.UNKNOWN;
        this.downloaded = false;
    }
    
    public ArtworkFile(ArtworkSize size, String filename, boolean downloaded) {
        this.size       = size;
        this.dimension  = new Dimension();
        this.filename   = filename;
        this.downloaded = downloaded;
    }
    
    /**
     * @return the size
     */
    public ArtworkSize getSize() {
        return size;
    }
    
    /**
     * @return the dimension
     */
    public Dimension getDimension() {
        return dimension;
    }
    
    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * @return the downloaded
     */
    public boolean isDownloaded() {
        return downloaded;
    }
    
    /**
     * @param size the size to set
     */
    public void setSize(ArtworkSize size) {
        this.size = size;
    }
    
    /**
     * @param size the String size to set.
     */
    public void setSize(String size) {
        this.size = ArtworkSize.valueOf(size.toUpperCase());
    }
    
    /**
     * @param dimension the dimension to set
     */
    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }
    
    /**
     * @param dimension the dimension to set using width, height format
     */
    public void setDimension(int width, int height) {
        this.dimension = new Dimension(width, height);
    }
    
    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    /**
     * @param downloaded the downloaded to set
     */
    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

}

