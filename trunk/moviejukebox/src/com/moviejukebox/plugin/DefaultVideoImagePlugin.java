package com.moviejukebox.plugin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

public class DefaultVideoImagePlugin implements MovieImagePlugin {

    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger("moviejukebox");
    private String skinHome;
    private boolean addReflectionEffect;
    private boolean addPerspective;
    private boolean normalizeVideoImages;
    private int videoimageWidth;
    private int videoimageHeight;
    private float ratio;

    public DefaultVideoImagePlugin() {
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        videoimageWidth = Integer.parseInt(PropertiesUtil.getProperty("videoimages.width", "400"));
        videoimageHeight = Integer.parseInt(PropertiesUtil.getProperty("videoimages.height", "225"));
        addReflectionEffect = Boolean.parseBoolean(PropertiesUtil.getProperty("videoimages.reflection", "false"));
        addPerspective = Boolean.parseBoolean(PropertiesUtil.getProperty("videoimages.perspective", "false"));
        normalizeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("videoimages.normalize", "false"));
        ratio = (float) videoimageWidth / (float) videoimageHeight;
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage videoImage, String perspectiveDirection) {
        BufferedImage bi = videoImage;
        
        if (videoImage != null) {
            int origWidth = videoImage.getWidth();
            int origHeight = videoImage.getHeight();
            boolean skipResize = false;
            if (origWidth < videoimageWidth && origHeight < videoimageHeight) {
                skipResize = true;
            }

            if (normalizeVideoImages) {
                if (skipResize) {
                    bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * ratio), origHeight, bi);
                } else {
                    bi = GraphicTools.scaleToSizeNormalized(videoimageWidth, videoimageHeight, bi);
                }
            } else if (!skipResize) {
                bi = GraphicTools.scaleToSizeBestFit(videoimageWidth, videoimageHeight, bi);
            }
            
            if (addReflectionEffect) {
                bi = GraphicTools.createReflectedPicture(bi, "videoimages");
            }

            if (addPerspective) {
                bi = GraphicTools.create3DPicture(bi, "videoimages", perspectiveDirection);
            }
        }

        return bi;
    }
    
    protected String getResourcesPath() {
        return skinHome + File.separator + "resources" + File.separator;
    }
}
