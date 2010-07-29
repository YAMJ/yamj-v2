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

package com.moviejukebox.plugin;

import java.awt.image.BufferedImage;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * @author altman.matthew
 */
public class DefaultBackgroundPlugin implements MovieImagePlugin {

// private static Logger logger = Logger.getLogger("moviejukebox");
    private int backgroundWidth;
    private int backgroundHeight;
    private boolean addPerspective;

    public DefaultBackgroundPlugin() {
        // These are the default values for the width and height.
        // Each plugin should determine their own values
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage backgroundImage, String imageType, String perspectiveDirection) {
        // persepectiveDirection is not currently used. Needs to be here because of the way the plugins work.

        // Validate the image type.
        if (imageType == null) {
            imageType = "fanart";
        }
        
        backgroundWidth = checkWidth(movie.isTVShow(), imageType);
        backgroundHeight = checkHeight(movie.isTVShow(), imageType);
        addPerspective = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".perspective", "false"));
        
        BufferedImage bi = null;
        if (backgroundImage != null) {
            bi = GraphicTools.scaleToSizeNormalized(backgroundWidth, backgroundHeight, backgroundImage);
        }
        
        if (addPerspective) {
            if (perspectiveDirection == null) // make sure the perspectiveDirection is populated
                perspectiveDirection = PropertiesUtil.getProperty(imageType + ".perspectiveDirection", "right");

            bi = GraphicTools.create3DPicture(bi, imageType, perspectiveDirection);
        }
        
        return bi;
    }

    /**
     * Checks for older Background width property in case the skin hasn't been updated.
     * TODO: Remove this procedure at some point 
     * @return the width of the fanart
     */
    public static int checkWidth(boolean isTvShow, String imageType) {
        String widthProperty = null;
        int backgroundWidth = 0;
        
        if (isTvShow) {
            widthProperty = PropertiesUtil.getProperty(imageType + ".tv.width");
        } else {
            widthProperty = PropertiesUtil.getProperty(imageType + ".movie.width");
        }
        
        //TODO remove these checks once all the skins have upgraded to the new properties
        // If this is null, then the property wasn't found, so look for the original
        if (widthProperty == null) {
            backgroundWidth = Integer.parseInt(PropertiesUtil.getProperty("background.width", "1280"));
        } else {
            backgroundWidth = Integer.parseInt(widthProperty);
        }

        return backgroundWidth;
    }
    /**
     * Checks for older Background width property in case the skin hasn't been updated.
     * TODO: Remove this procedure at some point 
     * @return the width of the fanart
     */
    public static int checkHeight(boolean isTvShow, String imageType) {
        String heightProperty = null;
        int backgroundHeight = 0;
        
        if (isTvShow) {
            heightProperty = PropertiesUtil.getProperty(imageType + ".tv.height");
        } else { 
            heightProperty = PropertiesUtil.getProperty(imageType + ".movie.height");
        }
        
        //TODO remove these checks once all the skins have upgraded to the new properties
        // If this is null, then the property wasn't found, so look for the original
        if (heightProperty == null) {
            backgroundHeight = Integer.parseInt(PropertiesUtil.getProperty("background.height", "720"));
        } else {
            backgroundHeight = Integer.parseInt(heightProperty);
        }

        return backgroundHeight;
    }
}
