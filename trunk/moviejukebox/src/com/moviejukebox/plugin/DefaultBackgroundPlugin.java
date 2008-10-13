package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 *
 * @author altman.matthew
 */
public class DefaultBackgroundPlugin implements MovieImagePlugin {

    private static Logger logger = Logger.getLogger("moviejukebox");

    private int backgroundWidth;
    private int backgroundHeight;

    public DefaultBackgroundPlugin() {
        backgroundWidth = Integer.parseInt(PropertiesUtil.getProperty("background.width", "1280"));
        backgroundHeight = Integer.parseInt(PropertiesUtil.getProperty("background.height", "720"));
    }
    
    @Override
    public BufferedImage generate(Movie movie, BufferedImage backgroundImage) {
        return GraphicTools.scaleToSizeNormalized(backgroundWidth, backgroundHeight, backgroundImage);
    }

}
