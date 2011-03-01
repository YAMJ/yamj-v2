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

import java.io.File;

public final class Jukebox {
    // Pointers to the target (final) Jukebox directory
    protected static String jukeboxRootLocation;
    protected static File   jukeboxRootLocationFile;
    protected static String jukeboxRootLocationDetails;
    protected static File   jukeboxRootLocationDetailsFile;
    
    // Pointers to the temp Jukebox directory
    protected static String jukeboxTempLocation;
    protected static File   jukeboxTempLocationFile;
    protected static String jukeboxTempLocationDetails;
    protected static File   jukeboxTempLocationDetailsFile;
    
    // The name of the details directory name
    protected static String detailsDirName;

    public Jukebox(String jukeboxRootLocation, String jukeboxTempLocation, String detailsDirName) {
        setDetailsDirName(detailsDirName);
        setJukeboxRootLocation(jukeboxRootLocation);
        setJukeboxTempLocation(jukeboxTempLocation);
    }

    /**
     * @return the jukeboxRootLocation
     */
    public final String getJukeboxRootLocation() {
        return jukeboxRootLocation;
    }

    /**
     * @return the jukeboxRootLocationFile
     */
    public final File getJukeboxRootLocationFile() {
        return jukeboxRootLocationFile;
    }

    /**
     * @return the jukeboxRootLocationDetails
     */
    public final String getJukeboxRootLocationDetails() {
        return jukeboxRootLocationDetails;
    }

    /**
     * @return the jukeboxRootLocationDetailsFile
     */
    public final File getJukeboxRootLocationDetailsFile() {
        return jukeboxRootLocationDetailsFile;
    }

    /**
     * @return the jukeboxTempLocation
     */
    public final String getJukeboxTempLocation() {
        return jukeboxTempLocation;
    }

    /**
     * @return the jukeboxTempLocationFile
     */
    public final File getJukeboxTempLocationFile() {
        return jukeboxTempLocationFile;
    }

    /**
     * @return the jukeboxTempLocationDetails
     */
    public final String getJukeboxTempLocationDetails() {
        return jukeboxTempLocationDetails;
    }

    /**
     * @return the jukeboxTempLocationDetailsFile
     */
    public final File getJukeboxTempLocationDetailsFile() {
        return jukeboxTempLocationDetailsFile;
    }

    /**
     * @return the detailsDirName
     */
    public final String getDetailsDirName() {
        return detailsDirName;
    }

    /**
     * @param jukeboxRootLocation the jukeboxRootLocation to set
     */
    public final void setJukeboxRootLocation(String jukeboxRootLocation) {
        // First set the two string directory names
        Jukebox.jukeboxRootLocation = jukeboxRootLocation;
        Jukebox.jukeboxRootLocationDetails = addDetailsName(jukeboxRootLocation);
        
        // Now create the File pointers from those string directory names
        Jukebox.jukeboxRootLocationFile = new File(Jukebox.jukeboxRootLocation);
        Jukebox.jukeboxRootLocationDetailsFile = new File(Jukebox.jukeboxRootLocationDetails);
    }

    /**
     * @param jukeboxTempLocation the jukeboxTempLocation to set
     */
    public final void setJukeboxTempLocation(String jukeboxTempLocation) {
        // First set the two string directory names
        Jukebox.jukeboxTempLocation = jukeboxTempLocation;
        Jukebox.jukeboxTempLocationDetails = addDetailsName(jukeboxTempLocation);
        
        // Now create the File pointers from those string directory names
        Jukebox.jukeboxTempLocationFile = new File(Jukebox.jukeboxTempLocation);
        Jukebox.jukeboxTempLocationDetailsFile = new File(Jukebox.jukeboxTempLocationDetails);
    }

    /**
     * This sets the details directory name to use
     * This MUST be set first
     * @param detailsDirName the detailsDirName to set
     */
    public final void setDetailsDirName(String detailsDirName) {
        Jukebox.detailsDirName = detailsDirName;
    }

    private String addDetailsName(String rootDirectory) {
        if (rootDirectory.endsWith(File.separator)) {
            // To deal with the jukebox directory in the root of a drive and already having a "/" or "\" at the end
            return (rootDirectory + Jukebox.detailsDirName);
        } else {
            return (rootDirectory + File.separator + Jukebox.detailsDirName);
        }
    }
}
