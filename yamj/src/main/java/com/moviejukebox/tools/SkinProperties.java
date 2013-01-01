/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
 * @author stuart.boston
 *
 */
public class SkinProperties {
    private static final Logger logger = Logger.getLogger(SkinProperties.class);
    private static final String SKIN_VERSION_FILENAME = "version.xml";

    private static String skinName          = Movie.UNKNOWN;
    private static String skinVersion       = Movie.UNKNOWN;
    private static String skinDate          = Movie.UNKNOWN;
    private static long   fileDate          = -1;
    private static List<String> skinMessage = new ArrayList<String>();

    /**
     * Read the skin information from skinVersionFilename in the skin directory
     */
    public static void readSkinVersion() {
        String skinVersionPath = StringTools.appendToPath(PropertiesUtil.getProperty("mjb.skin.dir", ""), SKIN_VERSION_FILENAME);
        File xmlFile = new File(skinVersionPath);

        if (xmlFile.exists()) {
            logger.debug("SkinProperties: Scanning file " + xmlFile.getAbsolutePath());
        } else {
            logger.debug("SkinProperties: " + xmlFile.getAbsolutePath() + " does not exist, skipping");
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
            logger.error("SkinProperties: Failed reading version information file (" + SKIN_VERSION_FILENAME + ")");
            logger.warn(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            logger.error("SkinProperties: Failed processing version information file (" + SKIN_VERSION_FILENAME + ")");
            logger.warn(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Output the skin information
     */
    public static void printSkinVersion() {
        if (StringTools.isValidString(getSkinName())) {
            logger.info("");
            logger.info("Skin Name   : " + getSkinName());

            if (StringTools.isValidString(getSkinDate())) {
                logger.info("Skin Version: " + getSkinVersion() + " (" + getSkinDate() + ")");
            } else {
                logger.info("Skin Version: " + getSkinVersion());
            }
            for (String message : getSkinMessage()) {
                logger.info(message);
            }
            logger.info("");
        } else {
            logger.debug("SkinProperties: No version information available for the skin");
        }
    }

    public static String getSkinName() {
        return skinName;
    }

    public static String getSkinVersion() {
        return skinVersion;
    }

    public static String getSkinDate() {
        return skinDate;
    }

    public static List<String> getSkinMessage() {
        return skinMessage;
    }

    public static void setSkinName(String skinName) {
        if (StringTools.isValidString(skinName)) {
            SkinProperties.skinName = skinName;
        } else {
            SkinProperties.skinName = Movie.UNKNOWN;
        }
    }

    public static void setSkinVersion(String skinVersion) {
        if (StringTools.isValidString(skinVersion)) {
            SkinProperties.skinVersion = skinVersion;
        } else {
            SkinProperties.skinVersion = Movie.UNKNOWN;
        }
    }

    public static void setSkinDate(String skinDate) {
        if (StringTools.isValidString(skinDate)) {
            SkinProperties.skinDate = skinDate;
        } else {
            SkinProperties.skinDate = Movie.UNKNOWN;
        }
    }

    public static void setSkinMessage(List<String> skinMessage) {
        if (skinMessage != null && !skinMessage.isEmpty()) {
            SkinProperties.skinMessage = skinMessage;
        } else {
            SkinProperties.skinMessage = new ArrayList<String>();
        }
    }

    public static void addSkinMessage(String messageLine) {
        if (StringTools.isValidString(messageLine)) {
            SkinProperties.skinMessage.add(messageLine);
        }
    }

    public static long getFileDate() {
        return fileDate;
    }

    public static void setFileDate(long fileDate) {
        SkinProperties.fileDate = fileDate;
    }
}
