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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.StringTools;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.sanselan.ImageReadException;

/**
 * @author altman.matthew
 */
public class DefaultBackgroundPlugin implements MovieImagePlugin {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final Logger logger = Logger.getLogger(DefaultBackgroundPlugin.class);
    private String overlayResources;
    private boolean highdefDiff;
    private boolean roundCorners;
    private int cornerRadius;
    private float rcqFactor;
    private int frameSize;
    private String frameColorSD;
    private String frameColorHD;
    private String frameColor720;
    private String frameColor1080;

    public DefaultBackgroundPlugin() {
        // These are the default values for the width and height.
        // Each plugin should determine their own values
        String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        boolean skinRoot = PropertiesUtil.getBooleanProperty("mjb.overlay.skinroot", FALSE);
        String overlayRoot = PropertiesUtil.getProperty("mjb.overlay.dir", Movie.UNKNOWN);
        overlayRoot = (skinRoot ? (skinHome + File.separator) : "") + (StringTools.isValidString(overlayRoot) ? (overlayRoot + File.separator) : "");
        overlayResources = overlayRoot + PropertiesUtil.getProperty("mjb.overlay.resources", "resources") + File.separator;
        highdefDiff = PropertiesUtil.getBooleanProperty("highdef.differentiate", FALSE);
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage backgroundImage, String imageType, String perspectiveDirection) {
        // persepectiveDirection is not currently used. Needs to be here because of the way the plugins work.

        String newImageType;
        // Validate the image type.
        if (imageType == null) {
            newImageType = "fanart";
        } else {
            newImageType = imageType;
        }

        int backgroundWidth = checkWidth(movie.isTVShow(), newImageType);
        int backgroundHeight = checkHeight(movie.isTVShow(), newImageType);

        boolean upscaleImage = PropertiesUtil.getBooleanProperty(newImageType + ".upscale", TRUE);

        boolean addPerspective = PropertiesUtil.getBooleanProperty(newImageType + ".perspective", FALSE);
        boolean addOverlay = PropertiesUtil.getBooleanProperty(newImageType + ".overlay", FALSE);

        boolean addFrame = PropertiesUtil.getBooleanProperty(newImageType + ".addFrame", FALSE);
        frameSize = PropertiesUtil.getIntProperty(newImageType + ".frame.size", "5");

        frameColorSD = PropertiesUtil.getProperty(newImageType + ".frame.colorSD", "255/255/255");
        frameColorHD = PropertiesUtil.getProperty(newImageType + ".frame.colorHD", "255/255/255");
        frameColor720 = PropertiesUtil.getProperty(newImageType + ".frame.color720", "255/255/255");
        frameColor1080 = PropertiesUtil.getProperty(newImageType + ".frame.color1080", "255/255/255");

        roundCorners = PropertiesUtil.getBooleanProperty(newImageType + ".roundCorners", FALSE);
        cornerRadius = PropertiesUtil.getIntProperty(newImageType + ".cornerRadius", "25");
        int cornerQuality = PropertiesUtil.getIntProperty(newImageType + ".cornerQuality", "0");

        if (roundCorners) {
            rcqFactor = (float) cornerQuality / 10 + 1;
        } else {
            rcqFactor = 1;
        }

        BufferedImage bi = null;
        if (backgroundImage != null) {
            if (upscaleImage || (backgroundWidth > MAX_WIDTH) || (backgroundHeight > MAX_HEIGHT)) {
                bi = GraphicTools.scaleToSizeNormalized((int) (backgroundWidth * rcqFactor), (int) (backgroundHeight * rcqFactor), backgroundImage);
            } else {
                bi = backgroundImage;
            }
        }

        // addFrame before rounding the corners see Issue 1825
        if (addFrame) {
            bi = drawFrame(movie, bi);
        }

        // roundCornders after addFrame see Issue 1825
        if (roundCorners) {
            bi = drawRoundCorners(bi);

            // Don't resize if the factor is the same
            if (rcqFactor > 1.00f) {
                //roundCorner quality resizing
                bi = GraphicTools.scaleToSizeStretch((int) backgroundWidth, (int) backgroundHeight, bi);
            }
        }

        if (addOverlay) {
            bi = drawOverlay(movie, bi);
        }

        if (addPerspective) {
            String pd;
            if (perspectiveDirection == null) { // make sure the perspectiveDirection is populated
                pd = PropertiesUtil.getProperty(newImageType + ".perspectiveDirection", "right");
            } else {
                pd = perspectiveDirection;
            }

            bi = GraphicTools.create3DPicture(bi, newImageType, pd);
        }

        return bi;
    }

    /**
     * Checks for older Background width property in case the skin hasn't been
     * updated. TODO: Remove this procedure at some point
     *
     * @return the width of the fanart
     */
    public static int checkWidth(boolean isTvShow, String imageType) {
        String widthProperty;
        int backgroundWidth;

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
     * Checks for older Background width property in case the skin hasn't been
     * updated. TODO: Remove this procedure at some point
     *
     * @return the width of the fanart
     */
    public static int checkHeight(boolean isTvShow, String imageType) {
        String heightProperty;
        int backgroundHeight;

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
     *
     * @param movie
     * @param bi
     * @return
     */
    private BufferedImage drawOverlay(Movie movie, BufferedImage bi) {

        String source;
        if (movie.isTVShow() && !movie.isSetMaster()) {        // Background overlay for TV shows
            source = "tv";
        } else if (movie.isTVShow() && movie.isSetMaster()) {  // Background overlay for Set index with more than one season
            source = Library.INDEX_SET.toLowerCase();
        } else if (movie.isExtra()) {                          // Background overlay for Extras (not tested)
            source = "extra";
        } else if (movie.isSetMaster()) {                      // Background overlay for Set index with only one season
            source = Library.INDEX_SET.toLowerCase();
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

        String overlayFilename = "overlay_fanart_" + source + ".png";
        try {
            BufferedImage biOverlay = GraphicTools.loadJPEGImage(getResourcesPath() + overlayFilename);

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
        } catch (ImageReadException ex) {
            logger.warn("Failed to read " + overlayFilename + ", please ensure it is valid");
        } catch (IOException ex) {
            logger.warn("Failed drawing overlay to " + movie.getBaseName() + ". Please check that overlay_fanart_" + source + ".png is in the resources directory.");
        }

        return bi;
    }

    /**
     * Calculate the path to the resources (skin path)
     *
     * @return path to the resource directory
     */
    protected String getResourcesPath() {
        return overlayResources;
    }

    /**
     * Draw a frame around the image; color depends on resolution if wanted
     *
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
            String[] colorSD = frameColorSD.split("/");
            int lengthSD[] = new int[colorSD.length];
            for (int i = 0; i < colorSD.length; i++) {
                lengthSD[i] = Integer.parseInt(colorSD[i]);
            }
            newGraphics.setPaint(new Color(lengthSD[0], lengthSD[1], lengthSD[2]));
        } else if (highdefDiff) {
            if (movie.isHD()) {
                // Otherwise use the 720p
                String[] color720 = frameColor720.split("/");
                int length720[] = new int[color720.length];
                for (int i = 0; i < color720.length; i++) {
                    length720[i] = Integer.parseInt(color720[i]);
                }
                newGraphics.setPaint(new Color(length720[0], length720[1], length720[2]));
            }

            if (movie.isHD1080()) {
                String[] color1080 = frameColor1080.split("/");
                int length1080[] = new int[color1080.length];
                for (int i = 0; i < color1080.length; i++) {
                    length1080[i] = Integer.parseInt(color1080[i]);
                }
                newGraphics.setPaint(new Color(length1080[0], length1080[1], length1080[2]));
            }
        } else {
            // We don't care, so use the default HD logo.
            String[] colorHD = frameColorHD.split("/");
            int lengthHD[] = new int[colorHD.length];
            for (int i = 0; i < colorHD.length; i++) {
                lengthHD[i] = Integer.parseInt(colorHD[i]);
            }
            newGraphics.setPaint(new Color(lengthHD[0], lengthHD[1], lengthHD[2]));
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
     *
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
