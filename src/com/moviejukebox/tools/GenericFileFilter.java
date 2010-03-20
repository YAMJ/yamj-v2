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