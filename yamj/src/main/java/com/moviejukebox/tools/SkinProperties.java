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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.moviejukebox.model.Movie;

/**
 * Process the skin information file for the details about the skin
 * @author stuart.boston
 *
 */
public class SkinProperties {
    private final static Logger logger = Logger.getLogger("moviejukebox");
    private final static String skinVersionFilename = "version.xml";
    
    private static String skinName          = Movie.UNKNOWN;
    private static String skinVersion       = Movie.UNKNOWN;
    private static String skinDate          = Movie.UNKNOWN;
    private static long   fileDate          = -1;
    private static List<String> skinMessage = new ArrayList<String>();
    
    /**
     * Read the skin information from skinVersionFilename in the skin directory
     */
    public static void readSkinVersion() {
        String skinVersionPath = StringTools.appendToPath(PropertiesUtil.getProperty("mjb.skin.dir", ""), skinVersionFilename);
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
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error("SkinProperties: Failed reading version information file (" + skinVersionFilename + ")");
            logger.debug(eResult.toString());
            return;
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error("SkinProperties: Failed processing version information file (" + skinVersionFilename + ")");
            logger.debug(eResult.toString());
            return;
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
