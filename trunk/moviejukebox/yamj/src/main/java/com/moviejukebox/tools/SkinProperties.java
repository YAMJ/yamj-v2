/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 * Process the skin information file for the details about the skin
 *
 * @author stuart.boston
 *
 */
public final class SkinProperties {

    // Logger
    private static final Logger LOG = Logger.getLogger(SkinProperties.class);
    private static final String LOG_MESSAGE = "SkinProperties: ";
    // Skin location
    private static final String SKIN_HOME = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    // Skin version file
    private static final String SKIN_VERSION_FILENAME = "version.xml";
    // Skin properties
    private static String skinName = Movie.UNKNOWN;
    private static String skinVersion = Movie.UNKNOWN;
    private static String skinDate = Movie.UNKNOWN;
    private static long fileDate = -1;
    private static List<String> skinMessage = new ArrayList<String>();

    /**
     * This is a utility class
     */
    private SkinProperties() {
        throw new UnsupportedOperationException("Class cannot be initialised.");
    }

    /**
     * Read the skin information from skinVersionFilename in the skin directory
     */
    public static void readSkinVersion() {
        String skinVersionPath = StringTools.appendToPath(SKIN_HOME, SKIN_VERSION_FILENAME);
        File xmlFile = new File(skinVersionPath);

        if (xmlFile.exists()) {
            LOG.debug(LOG_MESSAGE + "Scanning file " + xmlFile.getAbsolutePath());
        } else {
            LOG.debug(LOG_MESSAGE + xmlFile.getAbsolutePath() + " does not exist, skipping");
            return;
        }

        try {
            XMLConfiguration xmlConfig = new XMLConfiguration(xmlFile);
            setSkinName(xmlConfig.getString("name"));
            setSkinVersion(xmlConfig.getString("version"));
            setSkinDate(xmlConfig.getString("date"));
            setSkinMessage(StringTools.castList(String.class, xmlConfig.getList("message")));
            setFileDate(xmlFile.lastModified());
        } catch (ConfigurationException error) {
            LOG.error(LOG_MESSAGE + "Failed reading version information file (" + SKIN_VERSION_FILENAME + ")");
            LOG.warn(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed processing version information file (" + SKIN_VERSION_FILENAME + ")");
            LOG.warn(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Output the skin information
     */
    public static void printSkinVersion() {
        if (StringTools.isValidString(getSkinName())) {
            LOG.info("");
            LOG.info("Skin Name   : " + getSkinName());

            if (StringTools.isValidString(getSkinDate())) {
                LOG.info("Skin Version: " + getSkinVersion() + " (" + getSkinDate() + ")");
            } else {
                LOG.info("Skin Version: " + getSkinVersion());
            }
            for (String message : getSkinMessage()) {
                LOG.info(message);
            }
            LOG.info("");
        } else {
            LOG.debug(LOG_MESSAGE + "No version information available for the skin");
        }
    }

    /**
     * Get the skin name
     *
     * @return
     */
    public static String getSkinName() {
        return skinName;
    }

    /**
     * Get the version of the skin
     *
     * @return
     */
    public static String getSkinVersion() {
        return skinVersion;
    }

    /**
     * Get the modification date of the skin
     *
     * @return
     */
    public static String getSkinDate() {
        return skinDate;
    }

    /**
     * Get the skin message
     *
     * @return
     */
    public static List<String> getSkinMessage() {
        return skinMessage;
    }

    /**
     * Set the name of the skin
     *
     * @param skinName
     */
    public static void setSkinName(String skinName) {
        if (StringTools.isValidString(skinName)) {
            SkinProperties.skinName = skinName;
        } else {
            SkinProperties.skinName = Movie.UNKNOWN;
        }
    }

    /**
     * Set the version of the skin
     *
     * @param skinVersion
     */
    public static void setSkinVersion(String skinVersion) {
        if (StringTools.isValidString(skinVersion)) {
            SkinProperties.skinVersion = skinVersion;
        } else {
            SkinProperties.skinVersion = Movie.UNKNOWN;
        }
    }

    /**
     * Set the release date of the skin
     *
     * @param skinDate
     */
    public static void setSkinDate(String skinDate) {
        if (StringTools.isValidString(skinDate)) {
            SkinProperties.skinDate = skinDate;
        } else {
            SkinProperties.skinDate = Movie.UNKNOWN;
        }
    }

    /**
     * Set the skin message
     *
     * @param skinMessage
     */
    public static void setSkinMessage(List<String> skinMessage) {
        if (skinMessage != null && !skinMessage.isEmpty()) {
            SkinProperties.skinMessage = skinMessage;
        } else {
            SkinProperties.skinMessage = new ArrayList<String>();
        }
    }

    /**
     * Add a line of text about the skin to the skin message
     *
     * @param messageLine
     */
    public static void addSkinMessage(String messageLine) {
        if (StringTools.isValidString(messageLine)) {
            SkinProperties.skinMessage.add(messageLine);
        }
    }

    /**
     * Get the modification date of the skin
     *
     * @return
     */
    public static long getFileDate() {
        return fileDate;
    }

    /**
     * Set the modification date of the skin
     *
     * @param fileDate
     */
    public static void setFileDate(long fileDate) {
        SkinProperties.fileDate = fileDate;
    }

    /**
     * Get the location of the skin as shown in the properties file
     *
     * @return
     */
    public static String getSkinHome() {
        return SKIN_HOME;
    }
}
