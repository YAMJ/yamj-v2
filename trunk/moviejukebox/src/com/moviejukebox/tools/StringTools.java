/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import java.io.File;

import com.moviejukebox.model.Movie;

public class StringTools {
    /**
     * Check the string passed to see if it contains a value.
     * @param testString The string to test
     * @return False if the string is empty, null or UNKNOWN, True otherwise
     */
    public static boolean isValidString(String testString) {
        if (testString == null) {
            return false;
        }
        
        if (testString.equalsIgnoreCase(Movie.UNKNOWN)) {
            return false;
        }
        
        if (testString.trim().equals("")) {
            return false;
        }
        
        return true;
    }

    /**
     * Append a string to the end of a path ensuring that there are the correct number of File.separators
     * @param basePath
     * @param additionalPath
     * @return
     */
    public static String appendToPath(String basePath, String additionalPath) {
        return (basePath.trim() + (basePath.trim().endsWith(File.separator)?"":File.separator) + additionalPath.trim());
    }

}
