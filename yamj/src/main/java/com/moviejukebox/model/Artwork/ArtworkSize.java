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

public enum ArtworkSize {
    SMALL, 
    MEDIUM,
    LARGE;
    
    public static ArtworkSize fromString(String size) {
        if (size != null) {
            try {
                return ArtworkSize.valueOf(size.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("No ArtworkSize " + size + " exists");
            }
        }
        throw new IllegalArgumentException("No ArtworkSize is null");
    }
    
}
