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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

public class DefaultThumbnailPlugin implements MovieImagePlugin {

    private static Logger logger = Logger.getLogger("moviejukebox");
    private String skinHome;
    private boolean addReflectionEffect;
    private boolean addPerspective;
    private boolean normalizeThumbnails;
    private boolean addHDLogo;
    private boolean addTVLogo;
    private boolean addLanguage;
    private int thumbWidth;
    private int thumbHeight;
    private float ratio;
    private boolean highdefDiff;

    public DefaultThumbnailPlugin() {
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        thumbWidth = Integer.parseInt(PropertiesUtil.getProperty("thumbnails.width", "180"));
        thumbHeight = Integer.parseInt(PropertiesUtil.getProperty("thumbnails.height", "260"));
        addReflectionEffect = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.reflection", "true"));
        addPerspective = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.perspective", "false"));
        normalizeThumbnails = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.normalize", "false"));
        addHDLogo = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.logoHD", "false"));
        addTVLogo = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.logoTV", "false"));
        addLanguage = Boolean.parseBoolean(PropertiesUtil.getProperty("thumbnails.language", "false"));
        ratio = (float) thumbWidth / (float) thumbHeight;
        highdefDiff = Boolean.parseBoolean(PropertiesUtil.getProperty("highdef.differentiate", "false"));
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage moviePoster, String perspectiveDirection) {
        BufferedImage bi = moviePoster;
        
        // Check if we need to cut the poster into a sub image            
        String posterSubimage = movie.getPosterSubimage();

        if (posterSubimage != null && !posterSubimage.isEmpty() && !posterSubimage.equalsIgnoreCase("Unknown")) {
            StringTokenizer st = new StringTokenizer(posterSubimage, ", ");
            int x = Integer.parseInt(st.nextToken());
            int y = Integer.parseInt(st.nextToken());
            int l = Integer.parseInt(st.nextToken());
            int h = Integer.parseInt(st.nextToken());

            double pWidth = (double) bi.getWidth() / 100;
            double pHeight = (double) bi.getHeight() / 100;

            bi = bi.getSubimage((int)(x*pWidth), (int)(y*pHeight), (int)(l*pWidth), (int)(h*pHeight));
        }

        if (moviePoster != null) {
            int origWidth = moviePoster.getWidth();
            int origHeight = moviePoster.getHeight();
            boolean skipResize = false;
            if (origWidth < thumbWidth && origHeight < thumbHeight && !addHDLogo && !addLanguage) {
                skipResize = true;
            }

            if (normalizeThumbnails) {
                if (skipResize) {
                    bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * ratio), origHeight, bi);
                } else {
                    bi = GraphicTools.scaleToSizeNormalized(thumbWidth, thumbHeight, bi);
                }
            } else if (!skipResize) {
                bi = GraphicTools.scaleToSize(thumbWidth, thumbHeight, bi);
            }
            
            bi = drawLogos(movie, bi);

            if (addReflectionEffect) {
                bi = GraphicTools.createReflectedPicture(bi, "thumbnails");
            }

            if (addPerspective) {
                bi = GraphicTools.create3DPicture(bi, "thumbnails", perspectiveDirection);
            }
        }

        return bi;
    }
    
    protected BufferedImage drawLogos(Movie movie, BufferedImage bi) {
        if (addHDLogo) {
            bi = drawLogoHD(movie, bi, addTVLogo);
        }

        if (addTVLogo) {
            bi = drawLogoTV(movie, bi, addHDLogo);
        }

        if (addLanguage) {
            bi = drawLanguage(movie, bi);
        }
        
        return bi;
    }
    
    private BufferedImage drawLogoHD(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        // If the movie isn't high definition, then quit
        if (!movie.isHD()) {
            return bi;
        }
        
        String logoName;
        File logoFile;

        // Determine which logo to use.
        if (highdefDiff) {
            if (movie.isHD1080())
                // Use the 1080p logo
                logoName = "hd-1080.png";
            else
                // Otherwise use the 720p
                logoName = "hd-720.png";
        } else {
            // We don't care, so use the default HD logo.
            logoName = "hd.png";
        }

        logoFile = new File(getResourcesPath() + logoName);
        if (!logoFile.exists()) {
            logger.finest("Missing HD logo (" + logoName + ") using default hd.png");
            logoName = "hd.png";
        }

        try {
            InputStream in = new FileInputStream(getResourcesPath() + logoName);
            BufferedImage biHd = ImageIO.read(in);
            Graphics g = bi.getGraphics();

            if (addOtherLogo && (movie.isTVShow())) {
                // Both logos are required, so put the HD logo on the LEFT
                g.drawImage(biHd, 5, bi.getHeight() - biHd.getHeight() - 5, null);
                logger.finest("Drew HD logo (" + logoName + ") on the left");
            } else {
                // Only the HD logo is required so set it in the centre
                g.drawImage(biHd, bi.getWidth() / 2 - biHd.getWidth() / 2, bi.getHeight() - biHd.getHeight() - 5, null);
                logger.finest("Drew HD logo (" + logoName + ") in the middle");
            }
        } catch (IOException e) {
            logger.warning("Failed drawing HD logo to thumbnail file: Please check that " + logoName + " is in the resources directory.");
        }

        return bi;
    }

    // Drawing a TV label on the TV Series
    private BufferedImage drawLogoTV(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        if (movie.isTVShow()) {
            try {
                InputStream in = new FileInputStream(getResourcesPath() + "tv.png");
                BufferedImage biTV = ImageIO.read(in);
                Graphics g = bi.getGraphics();

                if (addOtherLogo && movie.isHD()) {
                    // Both logos are required, so put the TV logo on the RIGHT
                    g.drawImage(biTV, bi.getWidth() - biTV.getWidth() - 5, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.finest("Drew TV logo on the right");
                } else {
                    // Only the TV logo is required so set it in the centre
                    g.drawImage(biTV, bi.getWidth() / 2 - biTV.getWidth() / 2, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.finest("Drew TV logo in the middle");
                }

            } catch (IOException e) {
                logger.warning("Failed drawing TV logo to thumbnail file: Please check that tv.png is in the resources directory.");
                e.printStackTrace();
            }
        }
        
        return bi;
    }

    private BufferedImage drawLanguage(Movie movie, BufferedImage bi) {
        String lang = movie.getLanguage();
        if (lang != null && !lang.isEmpty() && !lang.equalsIgnoreCase("Unknown")) {

            try {
                InputStream in = new FileInputStream(skinHome + File.separator + "resources" + File.separator + "languages" + File.separator + lang + ".png");
                BufferedImage biLang = ImageIO.read(in);
                Graphics g = bi.getGraphics();
                g.drawImage(biLang, 1, 1, null);
            } catch (IOException e) {
                logger.warning("Failed drawing Language logo to thumbnail file: Please check that language specific graphic (" + lang + ".png) is in the resources/languages directory.");
            }
        }

        return bi;
    }
    
    protected String getResourcesPath() {
        return skinHome + File.separator + "resources" + File.separator;
    }
}
