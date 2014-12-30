/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.model;

import java.io.File;

/**
 * Holds information about the jukebox and the various directories associated with it
 *
 * @author Stuart
 */
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
    public String getJukeboxRootLocation() {
        return jukeboxRootLocation;
    }

    /**
     * @return the jukeboxRootLocationFile
     */
    public File getJukeboxRootLocationFile() {
        return jukeboxRootLocationFile;
    }

    /**
     * @return the jukeboxRootLocationDetails
     */
    public String getJukeboxRootLocationDetails() {
        return jukeboxRootLocationDetails;
    }

    /**
     * @return the jukeboxRootLocationDetailsFile
     */
    public File getJukeboxRootLocationDetailsFile() {
        return jukeboxRootLocationDetailsFile;
    }

    /**
     * @return the jukeboxTempLocation
     */
    public String getJukeboxTempLocation() {
        return jukeboxTempLocation;
    }

    /**
     * @return the jukeboxTempLocationFile
     */
    public File getJukeboxTempLocationFile() {
        return jukeboxTempLocationFile;
    }

    /**
     * @return the jukeboxTempLocationDetails
     */
    public String getJukeboxTempLocationDetails() {
        return jukeboxTempLocationDetails;
    }

    /**
     * @return the jukeboxTempLocationDetailsFile
     */
    public File getJukeboxTempLocationDetailsFile() {
        return jukeboxTempLocationDetailsFile;
    }

    /**
     * @return the detailsDirName
     */
    public String getDetailsDirName() {
        return detailsDirName;
    }

    /**
     * @param jukeboxRootLocation the jukeboxRootLocation to set
     */
    public void setJukeboxRootLocation(String jukeboxRootLocation) {
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
    public void setJukeboxTempLocation(String jukeboxTempLocation) {
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
    public void setDetailsDirName(String detailsDirName) {
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
