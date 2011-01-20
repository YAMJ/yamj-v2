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

/**
 * A class to store all the artwork associated with a movie object
 * @author stuart.boston
 *
 */
public class Artwork {
    private static final String UNKNOWN = Movie.UNKNOWN;
    
    private String      sourceSite; // Where the artwork originated from
    private ArtworkType type;       // The type of the artwork.
    private String      url;        // The original URL of the artwork (may be used as key)
    private String      filename;   // The jukebox name of the artwork. If UNKNOWN, then not downloaded
    
    public Artwork(String sourceSite, ArtworkType type, String url, String filename) {
        this.sourceSite = sourceSite;
        this.type = type;
        this.url = url;
        this.filename = filename;
    }
    
    public Artwork() {
        this.sourceSite = UNKNOWN;
        this.type       = null;
        this.url        = UNKNOWN;
        this.filename   = UNKNOWN;
    }

    /**
     * @return the sourceSite
     */
    public String getSourceSite() {
        return sourceSite;
    }

    /**
     * @return the type
     */
    public ArtworkType getType() {
        return type;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param sourceSite the sourceSite to set
     */
    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    /**
     * @param type the type to set
     */
    public void setType(ArtworkType type) {
        this.type = type;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int compareTo(Artwork anotherArtwork) {
        return this.url.length() - anotherArtwork.getUrl().length();
    }

}
