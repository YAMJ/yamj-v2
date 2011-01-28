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

import java.util.Collection;
import java.util.HashMap;

import com.moviejukebox.model.Movie;

/**
 * A class to store all the artwork associated with a movie object
 * @author stuart.boston
 *
 */
public class Artwork {
    private static final String UNKNOWN = Movie.UNKNOWN;
    
    private ArtworkType type;       // The type of the artwork.
    private String      sourceSite; // Where the artwork originated from
    private String      url;        // The original URL of the artwork (may be used as key)
    private HashMap<ArtworkSize, ArtworkFile> sizes; // The hash should be the size that is passed as part of the ArtworkSize 

    /**
     * Create an Artwork object with a set of sizes
     * @param type          The type of the artwork
     * @param sourceSite    The source site the artwork came from
     * @param url           The URL of the artwork
     * @param sizes         A list of the artwork files to add
     */
    public Artwork(ArtworkType type, String sourceSite, String url, Collection<ArtworkFile> sizes) {
        this.type = type;
        this.sourceSite = sourceSite;
        this.url = url;
        
        for (ArtworkFile artworkFile : sizes) {
            this.addSize(artworkFile);
        }
    }

    /**
     * Create an Artwork object with a single size
     * @param type          The type of the artwork
     * @param sourceSite    The source site the artwork came from
     * @param url           The URL of the artwork
     * @param size          An artwork files to add
     */
    public Artwork(ArtworkType type, String sourceSite, String url, ArtworkFile size) {
        this.type = type;
        this.sourceSite = sourceSite;
        this.url = url;
        this.sizes = new HashMap<ArtworkSize, ArtworkFile>();
        this.addSize(size);
    }

    /**
     * Create a blank Artwork object
     */
    public Artwork() {
        this.sourceSite = UNKNOWN;
        this.type       = null;
        this.url        = UNKNOWN;
        this.sizes      = new HashMap<ArtworkSize, ArtworkFile>();
    }
    
    /**
     * Add the ArtworkFile to the list, overwriting anything already there
     * @param size
     */
    public void addSize(ArtworkFile size) {
        sizes.put(size.getSize(), size);
    }
    
    public Collection<ArtworkFile> getSizes() {
        return sizes.values();
    }
    
    public ArtworkFile getSize(String size) {
        return sizes.get(ArtworkSize.valueOf(size.toUpperCase()));
    }
    
    public ArtworkFile getSize(ArtworkSize size) {
        return sizes.get(size);
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

    public int compareTo(Artwork anotherArtwork) {
        if (this.sourceSite.equals(anotherArtwork.getSourceSite()) &&
             this.type.equals(anotherArtwork.getType()) &&
             this.url.equals(anotherArtwork.getUrl())) {
            return 0;
        } else {
            return 1;
        }
    }

}
