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
package com.moviejukebox.model;

import java.io.File;

public final class Jukebox {
    /*
     * Pointers to the target (final) Jukebox directory
     */

    private String jukeboxRootLocation;
    private File jukeboxRootLocationFile;
    private String jukeboxRootLocationDetails;
    private File jukeboxRootLocationDetailsFile;
    /*
     * Pointers to the temp Jukebox directory
     */
    private String jukeboxTempLocation;
    private File jukeboxTempLocationFile;
    private String jukeboxTempLocationDetails;
    private File jukeboxTempLocationDetailsFile;
    /*
     * The name of the details directory name
     */
    private String detailsDirName;

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
        this.jukeboxRootLocation = jukeboxRootLocation;
        this.jukeboxRootLocationDetails = addDetailsName(jukeboxRootLocation);

        // Now create the File pointers from those string directory names
        this.jukeboxRootLocationFile = new File(this.jukeboxRootLocation);
        this.jukeboxRootLocationDetailsFile = new File(this.jukeboxRootLocationDetails);
    }

    /**
     * @param jukeboxTempLocation the jukeboxTempLocation to set
     */
    public final void setJukeboxTempLocation(String jukeboxTempLocation) {
        // First set the two string directory names
        this.jukeboxTempLocation = jukeboxTempLocation;
        this.jukeboxTempLocationDetails = addDetailsName(jukeboxTempLocation);

        // Now create the File pointers from those string directory names
        this.jukeboxTempLocationFile = new File(this.jukeboxTempLocation);
        this.jukeboxTempLocationDetailsFile = new File(this.jukeboxTempLocationDetails);
    }

    /**
     * This sets the details directory name to use This MUST be set first
     *
     * @param detailsDirName the detailsDirName to set
     */
    public final void setDetailsDirName(String detailsDirName) {
        this.detailsDirName = detailsDirName;
    }

    private String addDetailsName(String rootDirectory) {
        if (rootDirectory.endsWith(File.separator)) {
            // To deal with the jukebox directory in the root of a drive and already having a "/" or "\" at the end
            return (rootDirectory + this.detailsDirName);
        } else {
            return (rootDirectory + File.separator + this.detailsDirName);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[Jukebox=");
        sb.append("[detailsDirName=").append(detailsDirName);
        sb.append("][jukeboxRootLocation=").append(jukeboxRootLocation);
        sb.append("][jukeboxRootLocationDetails=").append(jukeboxRootLocationDetails);
        sb.append("][jukeboxTempLocation=").append(jukeboxTempLocation);
        sb.append("][jukeboxTempLocationDetails").append(jukeboxTempLocationDetails);
        sb.append("]]");
        return sb.toString();
    }
}
