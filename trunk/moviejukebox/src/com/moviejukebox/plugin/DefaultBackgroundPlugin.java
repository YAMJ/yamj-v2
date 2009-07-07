/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

    public DefaultBackgroundPlugin() {
        backgroundWidth = Integer.parseInt(PropertiesUtil.getProperty("background.width", "1280"));
        backgroundHeight = Integer.parseInt(PropertiesUtil.getProperty("background.height", "720"));
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage backgroundImage, String perspectiveDirection) {
        // perspectiveDirection not used. Needs to be here becuase of the way the plugins work.
        BufferedImage img = null;
        if (backgroundImage != null) {
            img = GraphicTools.scaleToSizeNormalized(backgroundWidth, backgroundHeight, backgroundImage);
        }
        return img;
    }
}
