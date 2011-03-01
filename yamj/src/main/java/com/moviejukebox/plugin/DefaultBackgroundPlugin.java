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
package com.moviejukebox.plugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * @author altman.matthew
 */
public class DefaultBackgroundPlugin implements MovieImagePlugin {

    private static Logger logger = Logger.getLogger("moviejukebox");
    private int backgroundWidth;
    private int backgroundHeight;
    private boolean addPerspective;
    private boolean addOverlay;
    private String skinHome;
    private boolean highdefDiff;
    private boolean roundCorners;
    private int cornerRadius;
    private boolean addFrame;
    private int frameSize;
    private static String frameColorHD;
    private static String frameColor720;
    private static String frameColor1080;
    private static String frameColorSD;

    public DefaultBackgroundPlugin() {
        // These are the default values for the width and height.
        // Each plugin should determine their own values
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        highdefDiff = PropertiesUtil.getBooleanProperty("highdef.differentiate", "false");
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage backgroundImage, String imageType, String perspectiveDirection) {
        // persepectiveDirection is not currently used. Needs to be here because of the way the plugins work.

        // Validate the image type.
        if (imageType == null) {
            imageType = "fanart";
        }
        
        backgroundWidth     = checkWidth(movie.isTVShow(), imageType);
        backgroundHeight    = checkHeight(movie.isTVShow(), imageType);
        addPerspective      = PropertiesUtil.getBooleanProperty(imageType + ".perspective", "false");
        
        addOverlay          = PropertiesUtil.getBooleanProperty(imageType + ".overlay", "false");

        addFrame            = PropertiesUtil.getBooleanProperty(imageType + ".addFrame", "false");
        frameSize           = PropertiesUtil.getIntProperty(imageType + ".frame.size", "5");
        frameColorSD        = PropertiesUtil.getProperty(imageType + ".frame.colorSD", "255/255/255");
        frameColorHD        = PropertiesUtil.getProperty(imageType + ".frame.colorHD", "255/255/255");
        frameColor720       = PropertiesUtil.getProperty(imageType + ".frame.color720", "255/255/255");
        frameColor1080      = PropertiesUtil.getProperty(imageType + ".frame.color1080", "255/255/255");

        BufferedImage bi = null;
        if (backgroundImage != null) {
            bi = GraphicTools.scaleToSizeNormalized(backgroundWidth, backgroundHeight, backgroundImage);
        }
        
        // addFrame before rounding the corners see Issue 1825
        if (addFrame) {
            bi = drawFrame(movie, bi);
        }
                   
        // roundCornders after addFrame see Issue 1825
        if (roundCorners) {
            bi = drawRoundCorners(bi);
        }

        if (addOverlay) {
            bi = drawOverlay(movie, bi);
        }
        
        if (addPerspective) {
            if (perspectiveDirection == null) { // make sure the perspectiveDirection is populated
                perspectiveDirection = PropertiesUtil.getProperty(imageType + ".perspectiveDirection", "right");
            }

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
            backgroundWidth = PropertiesUtil.getIntProperty("background.width", "1280");
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
            backgroundHeight = PropertiesUtil.getIntProperty("background.height", "720");
        } else {
            backgroundHeight = Integer.parseInt(heightProperty);
        }

        return backgroundHeight;
    }
    
    /** 
     * Draw an overlay on the fanarts (shading, static menu backgrounds, etc.)
     * specific for TV, Movie, SET and Extras backgrounds
     * @param movie
     * @param bi
     * @return
     */
    private BufferedImage drawOverlay(Movie movie, BufferedImage bi) {
        
        String source;
        if (movie.isTVShow() && !movie.isSetMaster()) {        // Background overlay for TV shows
            source = "tv";
        } else if (movie.isTVShow() && movie.isSetMaster()) {  // Background overlay for Set index with more than one season 
            source = "set";
        } else if (movie.isExtra()) {                          // Background overlay for Extras (not tested) 
            source = "extra";
        } else if (movie.isSetMaster()) {                      // Background overlay for Set index with only one season
            source = "set";
        } else {
            source = "movie";                                  // Background overlay for Set index with only one season;
        }
        // Don't know why, but I had to differ between Sets containing only one season and Sets with more than one Season.
        // (comments can be deleted)
        
        
        // Make sure the source is formatted correctly
        source = source.toLowerCase().trim();
        
        // Check for a blank or an UNKNOWN source and correct it
        if (StringTools.isNotValidString(source)) {
            source = "blank";
        }
            
        try {
            BufferedImage biOverlay = GraphicTools.loadJPEGImage(getResourcesPath() + "overlay_fanart_" + source + ".png");
        
            BufferedImage returnBI = new BufferedImage(biOverlay.getWidth(), biOverlay.getHeight(), BufferedImage.TYPE_INT_ARGB);  
            Graphics2D g2BI = returnBI.createGraphics();
            g2BI.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
            g2BI.drawImage(bi, 
                        0, 0, bi.getWidth(), bi.getHeight(), 
                        0, 0, bi.getWidth(), bi.getHeight(), 
                        null);
            g2BI.drawImage(biOverlay, 0, 0, null);

            g2BI.dispose();
            
            return returnBI;
        } catch (IOException error) {
            logger.warning("Failed drawing overlay to " + movie.getBaseName() + ". Please check that overlay_fanart_" + source + ".png is in the resources directory.");
        }
            
        return bi;
    }
    
    /**
     * Calculate the path to the resources (skin path)
     * 
     * @return path to the resource directory
     */
    protected String getResourcesPath() {
        return skinHome + File.separator + "resources" + File.separator;
    }

    
    /**
     * Draw a frame around the image; color depends on resolution if wanted
     * @param movie
     * @param bi
     * @return
     */        
    private BufferedImage drawFrame(Movie movie, BufferedImage bi) {
        BufferedImage newImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D newGraphics = newImg.createGraphics();
        newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cornerRadius2 = 0;
        
        if (!movie.isHD()) {
            String[] ColorSD = frameColorSD.split("/");
            int SD[] = new int[ColorSD.length];
            for (int i = 0; i < ColorSD.length; i++) {
                SD[i] = Integer.parseInt(ColorSD[i]);
            }
            newGraphics.setPaint(new Color (SD[0], SD[1], SD[2]));
        } else if (highdefDiff) {            
            if (movie.isHD()) {    
                // Otherwise use the 720p
                String[] Color720 = frameColor720.split("/");
                int LO[] = new int[Color720.length];
                for (int i = 0; i < Color720.length; i++) {
                    LO[i] = Integer.parseInt(Color720[i]);
                }
                newGraphics.setPaint(new Color (LO[0], LO[1], LO[2]));
            }
            
            if (movie.isHD1080()) {     
                String[] Color1080 = frameColor1080.split("/");
                int HI[] = new int[Color1080.length];
                for (int i = 0; i < Color1080.length; i++) {
                    HI[i] = Integer.parseInt(Color1080[i]);
                }
                newGraphics.setPaint(new Color (HI[0], HI[1], HI[2]));
            }
        } else {
            // We don't care, so use the default HD logo.
            String[] ColorHD = frameColorHD.split("/");
            int HD[] = new int[ColorHD.length];
            for (int i = 0; i < ColorHD.length; i++) {
                HD[i] = Integer.parseInt(ColorHD[i]);
            }
            newGraphics.setPaint(new Color (HD[0], HD[1], HD[2]));            
        }
        
        if (roundCorners) {
            cornerRadius2 = cornerRadius;
        }

        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight(), cornerRadius2, cornerRadius2);
        newGraphics.setClip(rect);

        // image fitted into border
        newGraphics.drawImage(bi, frameSize - 1, frameSize - 1, bi.getWidth() - (frameSize * 2) + 2, bi.getHeight() - (frameSize * 2) + 2, null);
               
        BasicStroke s4 = new BasicStroke(frameSize * 2);
            
        newGraphics.setStroke(s4);
        newGraphics.draw(rect);
        newGraphics.dispose();
        
        return newImg;
    }
    
    /**
     * Draw rounded corners on the image
     * @param bi
     * @return
     */
    protected BufferedImage drawRoundCorners(BufferedImage bi) {
        BufferedImage newImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D newGraphics = newImg.createGraphics();
        newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight(), cornerRadius, cornerRadius);
        newGraphics.setClip(rect);
        newGraphics.drawImage(bi, 0, 0, null);
        
        newGraphics.dispose();
        return newImg;
    }



}
