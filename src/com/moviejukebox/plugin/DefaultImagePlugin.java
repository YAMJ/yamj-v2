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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

public class DefaultImagePlugin implements MovieImagePlugin {

    private final String BANNER = "banners";
    private final String POSTER = "posters";
    private final String VIDEOIMAGE = "videoimages";
    private final String THUMBNAIL = "thumbnails";
    
    private static Logger logger = Logger.getLogger("moviejukebox");
    private String skinHome;
    private boolean addReflectionEffect;
    private boolean addPerspective;
    private boolean imageNormalize;
    private boolean addHDLogo;
    private boolean addTVLogo;
    private boolean addSetLogo;
    private boolean addSubTitle;
    private boolean addLanguage;
    private int imageWidth;
    private int imageHeight;
    private float ratio;
    private boolean highdefDiff;
    private boolean addTextTitle;
    private boolean addTextSeason;
    private boolean addTextSetSize;
    private static String textAlignment;
    private static String textFont;
    private static int textFontSize;
    private static String textFontColor;
    private static String textFontShadow;
    private static int textOffset;
    private String imageType;
    private boolean roundCorners;
    private int cornerRadius;

    public DefaultImagePlugin() {
        // Generic properties
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        highdefDiff = Boolean.parseBoolean(PropertiesUtil.getProperty("highdef.differentiate", "false"));
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage imageGraphic, String gImageType, String perspectiveDirection) {
        imageType = gImageType.toLowerCase();

        if ((POSTER + THUMBNAIL + BANNER + VIDEOIMAGE).indexOf(imageType) < 0) {
            // This is an error with the calling function
            logger.severe("YAMJ Error with calling function in DefaultImagePlugin.java");
            return imageGraphic;
        }

        // Specific Properties (dependent upon the imageType)
        imageWidth          = Integer.parseInt(PropertiesUtil.getProperty(imageType + ".width", "400"));
        imageHeight         = Integer.parseInt(PropertiesUtil.getProperty(imageType + ".height", "600"));
        addReflectionEffect = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".reflection", "false"));
        addPerspective      = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".perspective", "false"));
        imageNormalize      = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".normalize", "false"));
        addHDLogo           = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".logoHD", "false"));
        addTVLogo           = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".logoTV", "false"));
        addSetLogo          = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".logoSet", "false")); // Note: This should only be for thumbnails
        addSubTitle         = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".logoSubTitle", "false"));
        addLanguage         = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".language", "false"));

        addTextTitle        = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".addText.title", "false"));
        addTextSeason       = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".addText.season", "false"));
        addTextSetSize      = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".addText.setSize", "false")); // Note: This should only be for thumbnails
        textAlignment       = PropertiesUtil.getProperty(imageType + ".addText.alignment", "left");
        textFont            = PropertiesUtil.getProperty(imageType + ".addText.font", "Helvetica");
        textFontSize        = Integer.parseInt(PropertiesUtil.getProperty(imageType + ".addText.fontSize", "36"));
        textFontColor       = PropertiesUtil.getProperty(imageType + ".addText.fontColor", "LIGHT_GRAY");
        textFontShadow      = PropertiesUtil.getProperty(imageType + ".addText.fontShadow", "DARK_GRAY");
        textOffset          = Integer.parseInt(PropertiesUtil.getProperty(imageType + ".addText.offset", "10"));

        roundCorners = Boolean.parseBoolean(PropertiesUtil.getProperty(imageType + ".roundCorners", "false"));
        cornerRadius = Integer.parseInt(PropertiesUtil.getProperty(imageType + ".cornerRadius", "25"));
        
        ratio = (float)imageWidth / (float)imageHeight;

        BufferedImage bi = imageGraphic;

        if (imageGraphic != null) {
            int origWidth = imageGraphic.getWidth();
            int origHeight = imageGraphic.getHeight();
            boolean skipResize = false;
            if (origWidth < imageWidth && origHeight < imageHeight && !addHDLogo && !addLanguage) {
                skipResize = true;
            }

            // Check if we need to cut the poster into a sub image
            String posterSubimage = movie.getPosterSubimage();

            if (StringTools.isValidString(posterSubimage)) {
                StringTokenizer st = new StringTokenizer(posterSubimage, ", ");
                int x = Integer.parseInt(st.nextToken());
                int y = Integer.parseInt(st.nextToken());
                int l = Integer.parseInt(st.nextToken());
                int h = Integer.parseInt(st.nextToken());

                double pWidth = (double)bi.getWidth() / 100;
                double pHeight = (double)bi.getHeight() / 100;

                bi = bi.getSubimage((int)(x * pWidth), (int)(y * pHeight), (int)(l * pWidth), (int)(h * pHeight));
            }

            if (imageNormalize) {
                if (skipResize) {
                    bi = GraphicTools.scaleToSizeNormalized((int)(origHeight * ratio), origHeight, bi);
                } else {
                    bi = GraphicTools.scaleToSizeNormalized(imageWidth, imageHeight, bi);
                }
            } else if (!skipResize) {
                bi = GraphicTools.scaleToSize(imageWidth, imageHeight, bi);
            }

            if (imageType.equalsIgnoreCase(BANNER)) {
                if (addTextTitle) {
                    bi = drawText(bi, movie.getTitle(), true);
                }

                if (addTextSeason && movie.isTVShow()) {
                    bi = drawText(bi, "Season " + movie.getSeason(), false);
                }
            }
            
            if (roundCorners) {
                bi = roundCorners(bi);
            }

            bi = drawLogos(movie, bi);

            // Should only really happen on set's thumbnails.
            if (imageType.equalsIgnoreCase(THUMBNAIL) && movie.isSetMaster()) {
                // Draw the set logo if requested.
                if (addSetLogo) {
                    bi = drawSet(movie, bi);
                    logger.finest("Drew set logo on " + movie.getTitle());
                }
                // Let's draw the set's size (at bottom) if requested.
                final int size = movie.getSetSize();
                if (addTextSetSize && size > 0) {
                    String text = null;
                    // Let's draw not more than 9...
                    if (size > 9) {
                        text = "9+";
                    } else {
                        text = Integer.toString(size);
                    }
                    bi = drawText(bi, text, false);
                    logger.finest("Size (" + movie.getSetSize() + ") of set [" + movie.getTitle() + "] was drawn");
                }
            }

            if (addReflectionEffect) {
                bi = GraphicTools.createReflectedPicture(bi, imageType);
            }

            if (addPerspective) {
                if (perspectiveDirection == null) { // make sure the perspectiveDirection is populated {
                    perspectiveDirection = PropertiesUtil.getProperty(imageType + ".perspectiveDirection", "right");
                }

                bi = GraphicTools.create3DPicture(bi, imageType, perspectiveDirection);
            }
        }

        return bi;
    }
    
    protected BufferedImage roundCorners(BufferedImage bi) {
        BufferedImage newImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D newGraphics = newImg.createGraphics();
        newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight(), cornerRadius, cornerRadius);
        newGraphics.setClip(rect);
        newGraphics.drawImage(bi, 0, 0, null);
        return newImg;
    }

    /**
     * Draw the TV and HD logos onto the image
     * 
     * @param movie
     *            The source movie
     * @param bi
     *            The image to draw on
     * @return The new image with the added logos
     */
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
        
        if (addSubTitle) {
            bi = drawSubTitle(movie, bi);
        }

        return bi;
    }

    /**
     * Draw the SubTitle logo on the image
     * @param movie
     * @param bi
     * @return
     */
    private BufferedImage drawSubTitle(Movie movie, BufferedImage bi) {
        // If the doesn't have subtitles, then quit
        if (!StringTools.isValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO")) {
            return bi;
        }

        String logoName = "subtitle.png";
        File logoFile = new File(getResourcesPath() + logoName);

        if (!logoFile.exists()) {
            logger.finest("Missing SubTitle logo (" + logoName + ") unable to draw logo");
            return bi;
        }

        try {
            BufferedImage biSubTitle = GraphicTools.loadJPEGImage(getResourcesPath() + logoName);
            Graphics g = bi.getGraphics();
            g.drawImage(biSubTitle, bi.getWidth() - biSubTitle.getWidth() - 5, 5, null);
        } catch (IOException error) {
            logger.warning("Failed drawing SubTitle logo to thumbnail file: Please check that " + logoName + " is in the resources directory.");
        }

        return bi;    }
    
    /**
     * Draw the appropriate HD logo onto the image file
     * 
     * @param movie
     *            The source movie
     * @param bi
     *            The original image
     * @param addOtherLogo
     *            Do we need to draw the TV logo as well?
     * @return The new image file
     */
    private BufferedImage drawLogoHD(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        // If the movie isn't high definition, then quit
        if (!movie.isHD()) {
            return bi;
        }

        String logoName;
        File logoFile;

        // Determine which logo to use.
        if (highdefDiff) {
            if (movie.isHD1080()) {
                // Use the 1080p logo
                logoName = "hd-1080.png";
            } else {
                // Otherwise use the 720p
                logoName = "hd-720.png";
            }
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
            BufferedImage biHd = GraphicTools.loadJPEGImage(getResourcesPath() + logoName);
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
        } catch (IOException error) {
            logger.warning("Failed drawing HD logo to thumbnail file: Please check that " + logoName + " is in the resources directory.");
        }

        return bi;
    }

    /**
     * Draw the TV logo onto the image file
     * 
     * @param movie
     *            The source movie
     * @param bi
     *            The original image
     * @param addOtherLogo
     *            Do we need to draw the HD logo as well?
     * @return The new image file
     */
    private BufferedImage drawLogoTV(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        if (movie.isTVShow()) {
            try {
                BufferedImage biTV = GraphicTools.loadJPEGImage(getResourcesPath() + "tv.png");
                Graphics2D g = bi.createGraphics();

                if (addOtherLogo && movie.isHD()) {
                    // Both logos are required, so put the TV logo on the RIGHT
                    g.drawImage(biTV, bi.getWidth() - biTV.getWidth() - 5, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.finest("Drew TV logo on the right");
                } else {
                    // Only the TV logo is required so set it in the centre
                    g.drawImage(biTV, bi.getWidth() / 2 - biTV.getWidth() / 2, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.finest("Drew TV logo in the middle");
                }
            } catch (IOException error) {
                logger.warning("Failed drawing TV logo to thumbnail file: Please check that tv.png is in the resources directory.");
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }

        return bi;
    }

    /**
     * Draw the language logo to the image
     * 
     * @param movie
     *            Movie file, used to determine the language
     * @param bi
     *            The image file to draw on
     * @return The new image file with the language flag on it
     */
    private BufferedImage drawLanguage(IMovieBasicInformation movie, BufferedImage bi) {
        String lang = movie.getLanguage();

        if (StringTools.isValidString(lang)) {
            String[] languages = lang.split("/");

            String fullLanguage = "";
            for (String language : languages) {
                if (fullLanguage.length() > 0) {
                    fullLanguage += "_";
                }
                fullLanguage += language.trim();
            }
            try {

                Graphics g = bi.getGraphics();
                File imageFile = new File(skinHome + File.separator + "resources" + File.separator + "languages" + File.separator + fullLanguage + ".png");
                if (imageFile.exists()) {
                    BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                    g.drawImage(biLang, 1, 1, null);
                } else {
                    if (languages.length == 1) {
                        logger.warning("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                        logger.warning("Please check that language specific graphic (" + fullLanguage + ".png) is in the resources/languages directory.");
                    } else {
                        logger.finer("Unable to find multiple language image (" + fullLanguage
                                        + ".png) in the resources/languages directory, generating it from single one.");
                        int width = -1;
                        int height = -1;
                        int nbCols = (int)Math.sqrt(languages.length);
                        int nbRows = languages.length / nbCols;

                        BufferedImage[] imageFiles = new BufferedImage[languages.length];
                        // Looking for image file
                        for (int i = 0; i < languages.length; i++) {
                            String language = languages[i].trim();
                            imageFile = new File(skinHome + File.separator + "resources" + File.separator + "languages" + File.separator + language + ".png");
                            if (imageFile.exists()) {

                                BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                                imageFiles[i] = biLang;

                                // Determine image size.
                                if (width == -1) {

                                    width = biLang.getWidth() / nbCols;
                                    height = biLang.getHeight() / nbRows;
                                }
                            } else {
                                logger.warning("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                                logger.warning("Please check that language specific graphic (" + language + ".png) is in the resources/languages directory.");
                            }
                        }

                        for (int i = 0; i < imageFiles.length; i++) {
                            int indexCol = (i) % nbCols;
                            int indexRow = (i / nbCols);
                            g.drawImage(imageFiles[i], 1 + (width * indexCol), 1 + (height * indexRow), width, height, null);
                        }
                    }
                }
            } catch (IOException e) {
                logger.warning("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                logger.warning("Please check that language specific graphic (" + lang + ".png) is in the resources/languages directory.");
            }

        }

        return bi;
    }

    /**
     * Draw the set logo onto a poster
     * 
     * @param movie
     *            the movie to check
     * @param bi
     *            the image to draw on
     * @return the new buffered image
     */
    private BufferedImage drawSet(Identifiable movie, BufferedImage bi) {
        try {
            BufferedImage biSet = GraphicTools.loadJPEGImage(getResourcesPath() + "set.png");

            Graphics g = bi.getGraphics();
            g.drawImage(biSet, bi.getWidth() - biSet.getWidth() - 5, 1, null);
        } catch (IOException error) {
            logger.warning("Failed drawing set logo to thumbnail file:" + "Please check that set graphic (set.png) is in the resources directory.");
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

    protected BufferedImage drawPerspective(Identifiable movie, BufferedImage bi) {
        return bi;
    }

    private static BufferedImage drawText(BufferedImage bi, String outputText, boolean verticalAlign) {
        Graphics2D g2 = bi.createGraphics();
        g2.setFont(new Font(textFont, Font.BOLD, textFontSize));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(outputText);
        int imageWidth = bi.getWidth();
        int imageHeight = bi.getHeight();
        int leftAlignment = 0;
        int topAlignment = 0;

        if (textAlignment.equalsIgnoreCase("left")) {
            leftAlignment = textOffset;
        } else if (textAlignment.equalsIgnoreCase("right")) {
            leftAlignment = imageWidth - textWidth - textOffset;
        } else {
            leftAlignment = (imageWidth / 2) - (textWidth / 2);
        }

        if (verticalAlign) {
            // Align the text to the top
            topAlignment = fm.getHeight() - 10;
        } else {
            // Align the text to the bottom
            topAlignment = imageHeight - 10;
        }

        // Create the drop shadow
        if (!textFontShadow.equalsIgnoreCase("")) {
            g2.setColor(getColor(textFontShadow, Color.DARK_GRAY));
            g2.drawString(outputText, leftAlignment + 2, topAlignment + 2);
        }

        // Create the text itself
        g2.setColor(getColor(textFontColor, Color.LIGHT_GRAY));
        g2.drawString(outputText, leftAlignment, topAlignment);

        return bi;
    }

    private static Color getColor(String color, Color defaultColor) {
        Color returnColor = myColor.get(color);
        if (returnColor == null) {
            return defaultColor;
        }
        return returnColor;
    }

    enum myColor {
        white(Color.white), WHITE(Color.WHITE),
        lightGray(Color.lightGray), LIGHT_GRAY(Color.LIGHT_GRAY),
        gray(Color.gray), GRAY(Color.GRAY),
        darkGray(Color.darkGray), DARK_GRAY(Color.DARK_GRAY),
        black(Color.black), BLACK(Color.BLACK),
        red(Color.red), RED(Color.RED),
        pink(Color.pink), PINK(Color.PINK),
        orange(Color.orange), ORANGE(Color.ORANGE),
        yellow(Color.yellow), YELLOW(Color.YELLOW),
        green(Color.green), GREEN(Color.GREEN),
        magenta(Color.magenta), MAGENTA(Color.MAGENTA),
        cyan(Color.cyan), CYAN(Color.CYAN),
        blue(Color.blue), BLUE(Color.BLUE);

        private final Color color;

        // Constructor
        myColor(Color color) {
            this.color = color;
        }

        public static Color get(String name) {
            for (myColor aColor : myColor.values()) {
                if (aColor.toString().equalsIgnoreCase(name)) {
                    return aColor.color;
                }
            }
            return null;
        }
    }
}
