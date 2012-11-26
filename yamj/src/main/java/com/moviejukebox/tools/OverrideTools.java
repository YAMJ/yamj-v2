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
package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.OverrideFlag;
import java.util.Collection;

/**
 * Holds some override tools.
 *
 * @author modmax
 */
public final class OverrideTools {

    /**
     * Check if a movie value can be overwritten.
     * 
     * @param movieValue
     * @param overrideFlag
     * @param enabledOverrides
     * @return true, if movie value is unknown or override is enabled, else false
     */
    public static boolean checkOverride(String movieValue, OverrideFlag overrideFlag, Collection<OverrideFlag> enabledOverrides) {
        if (Movie.UNKNOWN.equals(movieValue)) {
            return true;
        }
        
        return isOverrideEnabled(overrideFlag, enabledOverrides);
    }

    /**
     * Checks if an override is enabled.
     * 
     * @param overrideFlag
     * @param enabledOverrides
     * @return true, if override is enabled for the flag, else false
     */
    public static boolean isOverrideEnabled(OverrideFlag overrideFlag, Collection<OverrideFlag> enabledOverrides) {
       if (overrideFlag == null) {
           return false;
       }
       
       if (enabledOverrides == null || enabledOverrides.isEmpty()) {
           return false;
       }
       
       return enabledOverrides.contains(overrideFlag);
    }
}
