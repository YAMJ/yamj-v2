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
package com.moviejukebox.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.*;

public class GenericFileFilter implements FilenameFilter {
    private Pattern genericFilePattern;
    
    // Set the passed filter.
    public GenericFileFilter(String regExp) {
        setPattern(regExp);
    }
    
    // If no filter is passed, select all files
    public GenericFileFilter() {
        setPattern(".*");
    }
    
    public boolean accept (File fileDir, String name) {
        Matcher m = genericFilePattern.matcher(name);
        return m.find();
    }
    
    // Use this method to change the pattern without creating a new object
    public void setPattern(String filePattern) {
        try {
            genericFilePattern = Pattern.compile(filePattern);
        } catch (Exception ignore) {
            genericFilePattern = Pattern.compile(".*");
        }
    }
    
}