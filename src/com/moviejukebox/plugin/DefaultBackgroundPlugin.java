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
