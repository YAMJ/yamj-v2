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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

public class SetThumbnailPlugin extends DefaultThumbnailPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private Boolean addSetLogo;
    
    public SetThumbnailPlugin() {
        super();
        addSetLogo = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.logoSet", "true"));
    }
    
    @Override
    protected BufferedImage drawLogos(Movie movie, BufferedImage bi) {
        bi = super.drawLogos(movie, bi);
        if (addSetLogo) {
            bi = drawSet(movie, bi);
            logger.finest("Drew set logo on " + movie.getTitle());
        }
        
        return bi;
    }
        
    private BufferedImage drawSet(Movie movie, BufferedImage bi) {
        try {
            InputStream in = new FileInputStream(getResourcesPath() + "set.png");
            BufferedImage biSet = ImageIO.read(in);
            Graphics g = bi.getGraphics();
            g.drawImage(biSet, bi.getWidth() - biSet.getWidth() - 5, 1, null);
        } catch(IOException e) {
            logger.warning("Failed drawing set logo to thumbnail file:"
                + "Please check that set graphic (set.png) is in the resources directory.");
        }
        
        return bi;
    }
}