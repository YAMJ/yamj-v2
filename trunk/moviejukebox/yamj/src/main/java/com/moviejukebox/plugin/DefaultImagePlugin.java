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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.moviejukebox.model.Award;
import com.moviejukebox.model.AwardEvent;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Comparator.ValueComparator;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

public class DefaultImagePlugin implements MovieImagePlugin {

    private final String BANNER = "banners";
    private final String POSTER = "posters";
    private final String VIDEOIMAGE = "videoimages";
    private final String THUMBNAIL = "thumbnails";
    private final String FOOTER = "footer";
    private static Logger logger = Logger.getLogger("moviejukebox");
    private String skinHome;
    private String overlayRoot;
    private String overlayResources;
    private boolean addReflectionEffect;
    private boolean addPerspective;
    private boolean imageNormalize;
    //stretch images
    private boolean imageStretch;
    private boolean addHDLogo;
    private boolean addTVLogo;
    private boolean addSetLogo;
    private boolean addSubTitle;
    private boolean addLanguage;
    private boolean blockLanguage;
    private boolean addOverlay;
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
    private int overlayOffsetX;
    private int overlayOffsetY;
    private String imageType;
    private boolean roundCorners;
    private int cornerRadius;
    // cornerQuality/rcqfactor to improve roundCorner Quality
    private int cornerQuality;
    private float rcqFactor;
    private boolean addFrame;
    private int frameSize;
    private static String frameColorHD;
    private static String frameColor720;
    private static String frameColor1080;
    private static String frameColorSD;
    private static String overlaySource;
    // Issue 1937: Overlay configuration XML
    private List<logoOverlay> overlayLayers = new ArrayList<logoOverlay>();
    private boolean xmlOverlay;
    private boolean addRating;
    private boolean realRating;
    private boolean addVideoSource;
    private boolean addVideoOut;
    private boolean addVideoCodec;
    private boolean addAudioCodec;
    private boolean blockAudioCodec;
    private boolean addAudioChannels;
    private boolean blockAudioChannels;
    private boolean addAudioLang;
    private boolean blockAudioLang;
    private boolean addContainer;
    private boolean addAspectRatio;
    private boolean addFPS;
    private boolean addCertification;
    private boolean addWatched;
    private boolean blockWatched;
    private boolean addTop250;
    private boolean addKeywords;
    private boolean addCountry;
    private boolean blockCountry;
    private boolean addCompany;
    private boolean blockCompany;
    private boolean countSetLogo;
    private boolean addAward;
    private boolean blockAward;
    private boolean countAward;
    private boolean blockClones;
    private boolean addEpisode;
    private boolean blockEpisode;
    private HashMap<String, ArrayList<String>> keywordsRating = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsVideoSource = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsVideoOut = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsVideoCodec = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsAudioCodec = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsAudioChannels = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsAudioLang = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsContainer = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsAspectRatio = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsFPS = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsCertification = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsKeywords = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsCountry = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsCompany = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> keywordsAward = new HashMap<String, ArrayList<String>>();
    private HashMap<String, logosBlock> overlayBlocks = new HashMap<String, logosBlock>();
    private int viIndex;

    public DefaultImagePlugin() {
        // Generic properties
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        boolean skinRoot = PropertiesUtil.getBooleanProperty("mjb.overlay.skinroot", "true");
        overlayRoot = PropertiesUtil.getProperty("mjb.overlay.dir", Movie.UNKNOWN);
        overlayRoot = (skinRoot ? (skinHome + File.separator) : "") + (StringTools.isValidString(overlayRoot) ? (overlayRoot + File.separator) : "");
        overlayResources = overlayRoot + PropertiesUtil.getProperty("mjb.overlay.resources", "resources") + File.separator;
        highdefDiff = PropertiesUtil.getBooleanProperty("highdef.differentiate", "false");
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage imageGraphic, String gImageType, String perspectiveDirection) {
        imageType = gImageType.toLowerCase();

        boolean isFooter = false;
        viIndex = 0;
        if (imageType.indexOf(FOOTER) == 0) {
            isFooter = true;
            imageType = imageType.replaceFirst(FOOTER, "");
        } else if (imageType.indexOf(VIDEOIMAGE) == 0 && !imageType.equals(VIDEOIMAGE)) {
            viIndex = Integer.parseInt(imageType.replaceFirst(VIDEOIMAGE, ""));
            viIndex = (viIndex > 0 && viIndex < movie.getFiles().size()) ? (viIndex - 1) : 0;
            imageType = VIDEOIMAGE;
        } else if ((POSTER + THUMBNAIL + BANNER + VIDEOIMAGE).indexOf(imageType) < 0) {
            // This is an error with the calling function
            logger.error("YAMJ Error with calling function in DefaultImagePlugin.java");
            return imageGraphic;
        }

        // Specific Properties (dependent upon the imageType)
        imageWidth = PropertiesUtil.getIntProperty(imageType + ".width", "400");
        imageHeight = PropertiesUtil.getIntProperty(imageType + ".height", "600");
        addReflectionEffect = PropertiesUtil.getBooleanProperty(imageType + ".reflection", "false");
        addPerspective = PropertiesUtil.getBooleanProperty(imageType + ".perspective", "false");
        imageNormalize = PropertiesUtil.getBooleanProperty(imageType + ".normalize", "false");
        imageStretch = PropertiesUtil.getBooleanProperty(imageType + ".stretch", "false");
        addHDLogo = PropertiesUtil.getBooleanProperty(imageType + ".logoHD", "false");
        addTVLogo = PropertiesUtil.getBooleanProperty(imageType + ".logoTV", "false");
        addSubTitle = PropertiesUtil.getBooleanProperty(imageType + ".logoSubTitle", "false");

        String tmpLanguage = PropertiesUtil.getProperty(imageType + ".language", "false");
        blockLanguage = tmpLanguage.equalsIgnoreCase("block");
        addLanguage = tmpLanguage.equalsIgnoreCase("true") || blockLanguage;

        addOverlay = PropertiesUtil.getBooleanProperty(imageType + ".overlay", "false");

        String tmpSetLogo = PropertiesUtil.getProperty(imageType + ".logoSet", "false");
        countSetLogo = tmpSetLogo.equalsIgnoreCase("count");
        addSetLogo = tmpSetLogo.equalsIgnoreCase("true") || countSetLogo; // Note: This should only be for thumbnails

        addTextTitle = PropertiesUtil.getBooleanProperty(imageType + ".addText.title", "false");
        addTextSeason = PropertiesUtil.getBooleanProperty(imageType + ".addText.season", "false");
        addTextSetSize = PropertiesUtil.getBooleanProperty(imageType + ".addText.setSize", "false"); // Note: This should only be for thumbnails
        textAlignment = PropertiesUtil.getProperty(imageType + ".addText.alignment", "left");
        textFont = PropertiesUtil.getProperty(imageType + ".addText.font", "Helvetica");
        textFontSize = PropertiesUtil.getIntProperty(imageType + ".addText.fontSize", "36");
        textFontColor = PropertiesUtil.getProperty(imageType + ".addText.fontColor", "LIGHT_GRAY");
        textFontShadow = PropertiesUtil.getProperty(imageType + ".addText.fontShadow", "DARK_GRAY");
        textOffset = PropertiesUtil.getIntProperty(imageType + ".addText.offset", "10");
        roundCorners = PropertiesUtil.getBooleanProperty(imageType + ".roundCorners", "false");
        cornerRadius = PropertiesUtil.getIntProperty(imageType + ".cornerRadius", "25");
        cornerQuality = PropertiesUtil.getIntProperty(imageType + ".cornerQuality", "0");

        overlayOffsetX = PropertiesUtil.getIntProperty(imageType + ".overlay.offsetX", "0");
        overlayOffsetY = PropertiesUtil.getIntProperty(imageType + ".overlay.offsetY", "0");
        overlaySource = PropertiesUtil.getProperty(imageType + ".overlay.source", "default");

        addFrame = PropertiesUtil.getBooleanProperty(imageType + ".addFrame", "false");
        frameSize = PropertiesUtil.getIntProperty(imageType + ".frame.size", "5");
        frameColorSD = PropertiesUtil.getProperty(imageType + ".frame.colorSD", "255/255/255");
        frameColorHD = PropertiesUtil.getProperty(imageType + ".frame.colorHD", "255/255/255");
        frameColor720 = PropertiesUtil.getProperty(imageType + ".frame.color720", "255/255/255");
        frameColor1080 = PropertiesUtil.getProperty(imageType + ".frame.color1080", "255/255/255");

        // Issue 1937: Overlay configuration XML
        String tmpRating = PropertiesUtil.getProperty(imageType + ".rating", "false");
        realRating = tmpRating.equalsIgnoreCase("real");
        addRating = tmpRating.equalsIgnoreCase("true") || realRating;

        String tmpAudioCodec = PropertiesUtil.getProperty(imageType + ".audiocodec", "false");
        blockAudioCodec = tmpAudioCodec.equalsIgnoreCase("block");
        addAudioCodec = tmpAudioCodec.equalsIgnoreCase("true") || blockAudioCodec;

        String tmpAudioChannels = PropertiesUtil.getProperty(imageType + ".audiochannels", "false");
        blockAudioChannels = tmpAudioChannels.equalsIgnoreCase("block");
        addAudioChannels = tmpAudioChannels.equalsIgnoreCase("true") || blockAudioChannels;

        String tmpAudioLang = PropertiesUtil.getProperty(imageType + ".audiolang", "false");
        blockAudioLang = tmpAudioCodec.equalsIgnoreCase("block");
        addAudioLang = tmpAudioLang.equalsIgnoreCase("true") || blockAudioLang;

        addVideoSource = PropertiesUtil.getBooleanProperty(imageType + ".videosource", "false");
        addVideoOut = PropertiesUtil.getBooleanProperty(imageType + ".videoout", "false");
        addVideoCodec = PropertiesUtil.getBooleanProperty(imageType + ".videocodec", "false");
        addContainer = PropertiesUtil.getBooleanProperty(imageType + ".container", "false");
        addAspectRatio = PropertiesUtil.getBooleanProperty(imageType + ".aspect", "false");
        addFPS = PropertiesUtil.getBooleanProperty(imageType + ".fps", "false");
        addCertification = PropertiesUtil.getBooleanProperty(imageType + ".certification", "false");

        String tmpWatched = PropertiesUtil.getProperty(imageType + ".watched", "false");
        blockWatched = tmpWatched.equalsIgnoreCase("block");
        addWatched = tmpWatched.equalsIgnoreCase("true") || blockWatched;

        String tmpEpisode = PropertiesUtil.getProperty(imageType + ".episode", "false");
        blockEpisode = tmpEpisode.equalsIgnoreCase("block");
        addEpisode = tmpEpisode.equalsIgnoreCase("true") || blockEpisode;

        addTop250 = PropertiesUtil.getBooleanProperty(imageType + ".top250", "false");
        addKeywords = PropertiesUtil.getBooleanProperty(imageType + ".keywords", "false");
        blockClones = PropertiesUtil.getBooleanProperty(imageType + ".clones", "false");

        String tmpCountry = PropertiesUtil.getProperty(imageType + ".country", "false");
        blockCountry = tmpCountry.equalsIgnoreCase("block");
        addCountry = tmpCountry.equalsIgnoreCase("true") || blockCountry;

        String tmpCompany = PropertiesUtil.getProperty(imageType + ".company", "false");
        blockCompany = tmpCompany.equalsIgnoreCase("block");
        addCompany = tmpCompany.equalsIgnoreCase("true") || blockCompany;

        String tmpAward = PropertiesUtil.getProperty(imageType + ".award", "false");
        blockAward = tmpAward.equalsIgnoreCase("block");
        countAward = tmpAward.equalsIgnoreCase("count");
        addAward = tmpAward.equalsIgnoreCase("true") || blockAward || countAward;

        xmlOverlay = PropertiesUtil.getBooleanProperty(imageType + ".xmlOverlay", "false");
        if (xmlOverlay) {
            String tmp = PropertiesUtil.getProperty("overlay.keywords.rating", "");
            fillOverlayKeywords(keywordsRating, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.videosource", "");
            fillOverlayKeywords(keywordsVideoSource, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.videoout", "");
            fillOverlayKeywords(keywordsVideoOut, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.videocodec", "");
            fillOverlayKeywords(keywordsVideoCodec, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.audiocodec", "");
            fillOverlayKeywords(keywordsAudioCodec, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.audiochannels", "");
            fillOverlayKeywords(keywordsAudioChannels, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.audiolang", "");
            fillOverlayKeywords(keywordsAudioLang, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.container", "");
            fillOverlayKeywords(keywordsContainer, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.aspect", "");
            fillOverlayKeywords(keywordsAspectRatio, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.fps", "");
            fillOverlayKeywords(keywordsFPS, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.certification", "");
            fillOverlayKeywords(keywordsCertification, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.keywords", "");
            fillOverlayKeywords(keywordsKeywords, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.country", "");
            fillOverlayKeywords(keywordsCountry, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.company", "");
            fillOverlayKeywords(keywordsCompany, tmp);
            tmp = PropertiesUtil.getProperty("overlay.keywords.award", "");
            fillOverlayKeywords(keywordsAward, tmp);
            fillOverlayParams(PropertiesUtil.getProperty(imageType + ".xmlOverlayFile", "overlay-default.xml"));
        }

        ratio = (float) imageWidth / (float) imageHeight;

        if (roundCorners) {
            rcqFactor = (float) cornerQuality / 10 + 1;
        } else {
            rcqFactor = 1;
        }

        BufferedImage bi = imageGraphic;

        if (imageGraphic != null) {
            int origWidth = imageGraphic.getWidth();
            int origHeight = imageGraphic.getHeight();
            boolean skipResize = false;
            if (origWidth < imageWidth && origHeight < imageHeight && !addHDLogo && !addLanguage) {
                //Perhaps better: if (origWidth == imageWidth && origHeight == imageHeight && !addHDLogo && !addLanguage) {
                skipResize = true;
            }

            if (imageNormalize) {
                if (skipResize) {
                    bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * rcqFactor * ratio), (int) (origHeight * rcqFactor), bi);
                } else {
                    bi = GraphicTools.scaleToSizeNormalized((int) (imageWidth * rcqFactor), (int) (imageHeight * rcqFactor), bi);
                }
            } else if (imageStretch) {
                bi = GraphicTools.scaleToSizeStretch((int) (imageWidth * rcqFactor), (int) (imageHeight * rcqFactor), bi);

            } else if (!skipResize) {
                bi = GraphicTools.scaleToSize((int) (imageWidth * rcqFactor), (int) (imageHeight * rcqFactor), bi);
            }

            // addFrame before rounding the corners see Issue 1825
            if (addFrame) {
                bi = drawFrame(movie, bi);
            }

            // roundCornders after addFrame see Issue 1825
            if (roundCorners) {
                if (!addFrame) {
                    bi = drawRoundCorners(bi);
                }

                // Don't resize if the factor is the same
                if (rcqFactor > 1.00f) {
                    //roundCorner quality resizing
                    bi = GraphicTools.scaleToSizeStretch((int) imageWidth, (int) imageHeight, bi);
                }
            }

            if (imageType.equalsIgnoreCase(BANNER)) {
                if (addTextTitle) {
                    bi = drawText(bi, movie.getTitle(), true);
                }

                if (addTextSeason && movie.isTVShow()) {
                    bi = drawText(bi, "Season " + movie.getSeason(), false);
                }
            }

            bi = drawLogos(movie, bi, isFooter ? FOOTER : imageType, true);

            if (addOverlay) {
                bi = drawOverlay(movie, bi, overlayOffsetX, overlayOffsetY);
            }

            bi = drawLogos(movie, bi, isFooter ? FOOTER : imageType, false);

            if (addReflectionEffect) {
                bi = GraphicTools.createReflectedPicture(bi, isFooter ? FOOTER : imageType);
            }

            if (addPerspective) {
                if (perspectiveDirection == null) { // make sure the perspectiveDirection is populated {
                    perspectiveDirection = PropertiesUtil.getProperty(imageType + ".perspectiveDirection", "right");
                }

                bi = GraphicTools.create3DPicture(bi, isFooter ? FOOTER : imageType, perspectiveDirection);
            }
        }

        return bi;
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
            newGraphics.setPaint(new Color(SD[0], SD[1], SD[2]));
        } else if (highdefDiff) {
            if (movie.isHD()) {
                // Otherwise use the 720p
                String[] Color720 = frameColor720.split("/");
                int LO[] = new int[Color720.length];
                for (int i = 0; i < Color720.length; i++) {
                    LO[i] = Integer.parseInt(Color720[i]);
                }
                newGraphics.setPaint(new Color(LO[0], LO[1], LO[2]));
            }

            if (movie.isHD1080()) {
                String[] Color1080 = frameColor1080.split("/");
                int HI[] = new int[Color1080.length];
                for (int i = 0; i < Color1080.length; i++) {
                    HI[i] = Integer.parseInt(Color1080[i]);
                }
                newGraphics.setPaint(new Color(HI[0], HI[1], HI[2]));
            }
        } else {
            // We don't care, so use the default HD logo.
            String[] ColorHD = frameColorHD.split("/");
            int HD[] = new int[ColorHD.length];
            for (int i = 0; i < ColorHD.length; i++) {
                HD[i] = Integer.parseInt(ColorHD[i]);
            }
            newGraphics.setPaint(new Color(HD[0], HD[1], HD[2]));
        }

        if (roundCorners) {
            cornerRadius2 = cornerRadius;
        }

        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight(), rcqFactor * cornerRadius2, rcqFactor * cornerRadius2);
        newGraphics.setClip(rect);

        // image fitted into border
        newGraphics.drawImage(bi, (int) (rcqFactor * frameSize - 1), (int) (rcqFactor * frameSize - 1), (int) (bi.getWidth() - (rcqFactor * frameSize * 2) + 2), (int) (bi.getHeight() - (rcqFactor * frameSize * 2) + 2), null);

        BasicStroke s4 = new BasicStroke(rcqFactor * frameSize * 2);

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

        RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight(), rcqFactor * cornerRadius, rcqFactor * cornerRadius);
        newGraphics.setClip(rect);
        newGraphics.drawImage(bi, 0, 0, null);

        newGraphics.dispose();
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
    protected BufferedImage drawLogos(Movie movie, BufferedImage bi, String imageType, boolean beforeMainOverlay) {
        // Issue 1937: Overlay configuration XML
        if (xmlOverlay) {
            for (logoOverlay layer : overlayLayers) {
                if (layer.before != beforeMainOverlay) {
                    continue;
                }
                boolean flag = false;
                List<stateOverlay> states = new ArrayList<stateOverlay>();
                for (String name : layer.names) {
                    String value = Movie.UNKNOWN;
                    if (checkLogoEnabled(name)) {
                        if (name.equalsIgnoreCase("set")) {
                            value = ((imageType.equalsIgnoreCase(THUMBNAIL) || imageType.equalsIgnoreCase(BANNER) || imageType.equalsIgnoreCase(FOOTER)) && movie.isSetMaster()) ? countSetLogo ? Integer.toString(movie.getSetSize()) : "true" : countSetLogo ? "0" : "false";
                        } else if (name.equalsIgnoreCase("TV")) {
                            value = movie.isTVShow() ? "true" : "false";
                        } else if (name.equalsIgnoreCase("HD")) {
                            value = movie.isHD() ? highdefDiff ? movie.isHD1080() ? "hd1080" : "hd720" : "hd" : "false";
                        } else if (name.equalsIgnoreCase("subtitle") || name.equalsIgnoreCase("ST")) {
                            value = (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO")) ? "false" : "true";
                        } else if (name.equalsIgnoreCase("language")) {
                            value = movie.getLanguage();
                        } else if (name.equalsIgnoreCase("rating")) {
                            value = ((!movie.isTVShow() && !movie.isSetMaster()) || (movie.isTVShow() && movie.isSetMaster())) ? Integer.toString(realRating ? movie.getRating() : (movie.getRating() / 10) * 10) : Movie.UNKNOWN;
                        } else if (name.equalsIgnoreCase("videosource") || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
                            value = movie.getVideoSource();
                        } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
                            value = movie.getVideoOutput();
                        } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
                            value = movie.getVideoCodec();
                        } else if (name.equalsIgnoreCase("audiocodec") || name.equalsIgnoreCase("acodec") || name.equalsIgnoreCase("AC")) {
                            value = movie.getAudioCodec();
                            if (!blockAudioCodec) {
                                int pos = value.indexOf(" / ");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                                pos = value.indexOf(" (");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            } else {
                                while (value.indexOf(" (") > 0 && value.indexOf(" (") < value.indexOf(")")) {
                                    value = value.substring(0, value.indexOf(" (")) + value.substring(value.indexOf(")") + 1);
                                }
                            }
                        } else if (name.equalsIgnoreCase("audiolang") || name.equalsIgnoreCase("alang") || name.equalsIgnoreCase("AL")) {
                            value = "";
                            for (String tmp : movie.getAudioCodec().split(" / ")) {
                                if (tmp.indexOf(" (") > 0 && tmp.indexOf(" (") < tmp.indexOf(")")) {
                                    tmp = tmp.substring(tmp.indexOf(" (") + 2, tmp.indexOf(")"));
                                } else {
                                    tmp = Movie.UNKNOWN;
                                }
                                if (!blockAudioLang) {
                                    value = tmp;
                                    break;
                                }
                                if (!value.equals("")) {
                                    value += " / ";
                                }
                                value += tmp;
                            }
                            if (value.equals("")) {
                                value = Movie.UNKNOWN;
                            }
                        } else if (name.equalsIgnoreCase("audiochannels") || name.equalsIgnoreCase("channels")) {
                            value = movie.getAudioChannels();
                            if (!blockAudioChannels) {
                                int pos = value.indexOf(" / ");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase("container")) {
                            value = movie.getContainer();
                        } else if (name.equalsIgnoreCase("aspect")) {
                            value = movie.getAspectRatio();
                        } else if (name.equalsIgnoreCase("fps")) {
                            value = Float.toString(movie.getFps());
                        } else if (name.equalsIgnoreCase("certification")) {
                            value = movie.getCertification();
                        } else if (name.equalsIgnoreCase("watched")) {
                            if (imageType.equalsIgnoreCase(VIDEOIMAGE)) {
                                value = movie.getFiles().toArray(new MovieFile[movie.getFiles().size()])[viIndex].isWatched() ? "true" : "false";
                            } else if (movie.isTVShow() && blockWatched) {
                                StringBuilder sbWatched = new StringBuilder();
                                boolean first = true;
                                for (MovieFile mf : movie.getFiles()) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        sbWatched.append(" / ");
                                    }
                                    sbWatched.append(mf.isWatched() ? "true" : "false");
                                }
                                value = sbWatched.toString();
                            } else {
                                value = movie.isWatched() ? "true" : "false";
                            }
                        } else if (name.equalsIgnoreCase("episode")) {
                            if (movie.isTVShow()) {
                                if (blockEpisode) {
                                    StringBuilder sbEpisode = new StringBuilder();
                                    boolean first = true;
                                    int firstPart, lastPart;
                                    for (MovieFile mf : movie.getFiles()) {
                                        firstPart = mf.getFirstPart();
                                        lastPart = mf.getLastPart();
                                        for (int part = firstPart; part <= lastPart; part++ ) {
                                            if (first) {
                                                first = false;
                                            } else {
                                                sbEpisode.append(" / ");
                                            }
                                            sbEpisode.append(part);
                                        }
                                    }
                                    value = sbEpisode.toString();
                                } else {
                                    value = Integer.toString(movie.getFiles().size());
                                }
                            }
                        } else if (name.equalsIgnoreCase("top250")) {
                            value = movie.getTop250() > 0 ? "true" : "false";
                        } else if (name.equalsIgnoreCase("keywords")) {
                            value = movie.getBaseFilename().toLowerCase();
                        } else if (name.equalsIgnoreCase("country")) {
                            value = movie.getCountry();
                            if (!blockCountry) {
                                int pos = value.indexOf(" / ");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase("company")) {
                            value = movie.getCompany();
                            if (!blockCompany) {
                                int pos = value.indexOf(" / ");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase("award")) {
                            value = "";
                            int awardCount = 0;
                            HashMap<String, Integer> awards = new HashMap<String, Integer>();
                            if (!movie.isSetMaster()) {
                                for (AwardEvent awardEvent : movie.getAwards()) {
                                    for (Award award : awardEvent.getAwards()) {
                                        if (award.getWon() > 0) {
                                            if (blockAward) {
                                                awards.put(award.getName(), award.getWon());
                                            } else if (countAward) {
                                                awardCount++;
                                            } else {
                                                value = "true";
                                                break;
                                            }
                                        }
                                    }
                                    if (!blockAward && !countAward && StringTools.isValidString(value)) {
                                        break;
                                    }
                                }
                            }

                            if (blockAward) {
                                ValueComparator bvc = new ValueComparator(awards);
                                TreeMap<String, Integer> sortedAwards = new TreeMap<String, Integer>(bvc);
                                sortedAwards.putAll(awards);

                                StringBuilder sbAwards = new StringBuilder();
                                boolean first = !value.isEmpty();   // Append the separator only if the "value" is not empty

                                for (String award : sortedAwards.keySet()) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        sbAwards.append(" / ");
                                    }
                                    sbAwards.append(award);
                                }
                                value += sbAwards.toString();
                            }
                            value = (StringTools.isNotValidString(value) && !countAward) ? blockAward ? Movie.UNKNOWN : "false" : countAward ? Integer.toString(awardCount) : value;
                        } else {
                            value = PropertiesUtil.getProperty(name, Movie.UNKNOWN);
                        }
                    }
                    stateOverlay state = new stateOverlay(layer.left, layer.top, layer.align, layer.valign, layer.width, layer.height, value);
                    states.add(state);
                }

                for (int inx = 0; inx < layer.names.size(); inx++) {
                    String name = layer.names.get(inx);
                    String value = states.get(inx).value;
                    String filename = Movie.UNKNOWN;
                    if (checkLogoEnabled(name)) {
                        if (!blockLanguage && name.equalsIgnoreCase("language") && StringTools.isValidString(value)) {
                            filename = "languages/English.png";
                        }
                        String[] values = value.split(" / ");
                        for (int j = 0; j < values.length; j++) {
                            value = values[j];
                            for (imageOverlay img : layer.images) {
                                if (img.name.equalsIgnoreCase(name)) {
                                    boolean accept = false;
                                    if (img.values.size() == 1 && cmpOverlayValue(name, img.value, value)) {
                                        accept = true;
                                    } else if (img.values.size() > 1) {
                                        accept = true;
                                        for (int i = 0; i < layer.names.size(); i++) {
                                            accept = accept && cmpOverlayValue(layer.names.get(i), img.values.get(i), states.get(i).value);
                                            if (!accept) {
                                                break;
                                            }
                                        }
                                    }
                                    if (!accept) {
                                        continue;
                                    }
                                    File imageFile = new File(overlayResources + img.filename);
                                    if (imageFile.exists()) {
                                        if (StringTools.isNotValidString(filename)) {
                                            filename = img.filename;
                                        } else {
                                            filename += " / " + img.filename;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        flag = flag || StringTools.isValidString(filename);
                    }
                    states.get(inx).filename = filename;
                }

                if (!flag) {
                    continue;
                }

                if (layer.positions.size() > 0) {
                    for (conditionOverlay cond : layer.positions) {
                        flag = true;
                        for (int i = 0; i < layer.names.size(); i++) {
                            String name = layer.names.get(i);
                            String condition = cond.values.get(i);
                            String value = states.get(i).value;
                            flag = flag && cmpOverlayValue(name, condition, value);
                            if (!flag) {
                                break;
                            }
                        }
                        if (flag) {
                            for (int i = 0; i < layer.names.size(); i++) {
                                positionOverlay pos = cond.positions.get(i);
                                states.get(i).left = pos.left;
                                states.get(i).top = pos.top;
                                states.get(i).align = pos.align;
                                states.get(i).valign = pos.valign;
                            }
                            break;
                        }
                    }
                }

                for (int i = 0; i < layer.names.size(); i++) {
                    stateOverlay state = states.get(i);
                    String name = layer.names.get(i);
                    if (!blockLanguage && name.equalsIgnoreCase("language")) {
                        bi = drawLanguage(movie, bi, getOverlayX(bi.getWidth(), 62, state.left, state.align), getOverlayY(bi.getHeight(), 40, state.top, state.valign));
                        continue;
                    }

                    String filename = state.filename;
                    if (StringTools.isNotValidString(filename)) {
                        continue;
                    }

                    if (((blockAudioCodec && ((name.equalsIgnoreCase("audiocodec") || name.equalsIgnoreCase("acodec") || name.equalsIgnoreCase("AC"))))
                            || (blockAudioChannels && (name.equalsIgnoreCase("audiochannels") || name.equalsIgnoreCase("channels")))
                            || (blockAudioLang && (name.equalsIgnoreCase("audiolang") || name.equalsIgnoreCase("alang") || name.equalsIgnoreCase("AL")))
                            || (blockCountry && name.equalsIgnoreCase("country"))
                            || (blockCompany && name.equalsIgnoreCase("company"))
                            || (blockAward && name.equalsIgnoreCase("award"))
                            || (blockWatched && name.equalsIgnoreCase("watched"))
                            || (blockEpisode && name.equalsIgnoreCase("episode"))
                            || (blockLanguage && name.equalsIgnoreCase("language")))
                            && (overlayBlocks.get(name) != null)) {
                        bi = drawBlock(movie, bi, name, filename, state.left, state.align, state.width, state.top, state.valign, state.height);
                        continue;
                    }

                    try {
                        BufferedImage biSet = GraphicTools.loadJPEGImage(overlayResources + filename);

                        Graphics2D g2d = bi.createGraphics();
                        g2d.drawImage(biSet, getOverlayX(bi.getWidth(), biSet.getWidth(), state.left, state.align), getOverlayY(bi.getHeight(), biSet.getHeight(), state.top, state.valign), state.width.matches("\\d+") ? Integer.parseInt(state.width) : biSet.getWidth(), state.height.matches("\\d+") ? Integer.parseInt(state.height) : biSet.getHeight(), null);
                        g2d.dispose();
                    } catch (IOException error) {
                        logger.warn("Failed drawing overlay to image file: Please check that " + filename + " is in the resources directory.");
                    }

                    if (name.equalsIgnoreCase("set")) {
                        bi = drawSetSize(movie, bi);
                        continue;
                    }
                }
            }
        } else if (beforeMainOverlay) {
            if (addHDLogo) {
                bi = drawLogoHD(movie, bi, addTVLogo);
            }

            if (addTVLogo) {
                bi = drawLogoTV(movie, bi, addHDLogo);
            }

            if (addLanguage) {
                bi = drawLanguage(movie, bi, 1, 1);
            }

            if (addSubTitle) {
                bi = drawSubTitle(movie, bi);
            }

            // Should only really happen on set's thumbnails.
            if (imageType.equalsIgnoreCase(THUMBNAIL) && movie.isSetMaster()) {
                // Draw the set logo if requested.
                if (addSetLogo) {
                    bi = drawSet(movie, bi);
                    logger.debug("Drew set logo on " + movie.getTitle());
                }
                bi = drawSetSize(movie, bi);
            }
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
        if (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO")) {
            return bi;
        }

        String logoName = "subtitle.png";
        File logoFile = new File(getResourcesPath() + logoName);

        if (!logoFile.exists()) {
            logger.debug("Missing SubTitle logo (" + logoName + ") unable to draw logo");
            return bi;
        }

        try {
            BufferedImage biSubTitle = GraphicTools.loadJPEGImage(getResourcesPath() + logoName);
            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(biSubTitle, bi.getWidth() - biSubTitle.getWidth() - 5, 5, null);
            g2d.dispose();
        } catch (IOException error) {
            logger.warn("Failed drawing SubTitle logo to thumbnail file: Please check that " + logoName + " is in the resources directory.");
        }

        return bi;
    }

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
            logger.debug("Missing HD logo (" + logoName + ") using default hd.png");
            logoName = "hd.png";
        }

        try {
            BufferedImage biHd = GraphicTools.loadJPEGImage(getResourcesPath() + logoName);
            Graphics2D g2d = bi.createGraphics();

            if (addOtherLogo && (movie.isTVShow())) {
                // Both logos are required, so put the HD logo on the LEFT
                g2d.drawImage(biHd, 5, bi.getHeight() - biHd.getHeight() - 5, null);
                logger.debug("Drew HD logo (" + logoName + ") on the left");
            } else {
                // Only the HD logo is required so set it in the centre
                g2d.drawImage(biHd, bi.getWidth() / 2 - biHd.getWidth() / 2, bi.getHeight() - biHd.getHeight() - 5, null);
                logger.debug("Drew HD logo (" + logoName + ") in the middle");
            }

            g2d.dispose();
        } catch (IOException error) {
            logger.warn("Failed drawing HD logo to thumbnail file: Please check that " + logoName + " is in the resources directory.");
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
                Graphics2D g2d = bi.createGraphics();

                if (addOtherLogo && movie.isHD()) {
                    // Both logos are required, so put the TV logo on the RIGHT
                    g2d.drawImage(biTV, bi.getWidth() - biTV.getWidth() - 5, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.debug("Drew TV logo on the right");
                } else {
                    // Only the TV logo is required so set it in the centre
                    g2d.drawImage(biTV, bi.getWidth() / 2 - biTV.getWidth() / 2, bi.getHeight() - biTV.getHeight() - 5, null);
                    logger.debug("Drew TV logo in the middle");
                }

                g2d.dispose();
            } catch (IOException error) {
                logger.warn("Failed drawing TV logo to thumbnail file: Please check that tv.png is in the resources directory.");
                logger.error(SystemTools.getStackTrace(error));
            }
        }

        return bi;
    }

    /**
     * Draw an overlay on the image, such as a box cover
     * specific for videosource, container, certification if wanted
     * @param movie
     * @param bi
     * @param offsetY
     * @param offsetX
     * @return
     */
    private BufferedImage drawOverlay(Movie movie, BufferedImage bi, int offsetX, int offsetY) {

        String source;
        if (overlaySource.equalsIgnoreCase("videosource")) {
            source = movie.getVideoSource();
        } else if (overlaySource.equalsIgnoreCase("certification")) {
            source = movie.getCertification();
        } else if (overlaySource.equalsIgnoreCase("container")) {
            source = movie.getContainer();
        } else {
            source = "default";
        }

        // Make sure the source is formatted correctly
        source = source.toLowerCase().trim();

        // Check for a blank or an UNKNOWN source and correct it
        if (StringTools.isNotValidString(source)) {
            source = "default";
        }

        try {
            BufferedImage biOverlay = GraphicTools.loadJPEGImage(getResourcesPath() + source + "_overlay_" + imageType + ".png");

            BufferedImage returnBI = new BufferedImage(biOverlay.getWidth(), biOverlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2BI = returnBI.createGraphics();
            g2BI.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2BI.drawImage(bi,
                    offsetX, offsetY, offsetX + bi.getWidth(), offsetY + bi.getHeight(),
                    0, 0, bi.getWidth(), bi.getHeight(),
                    null);
            g2BI.drawImage(biOverlay, 0, 0, null);

            g2BI.dispose();

            return returnBI;
        } catch (IOException error) {
            logger.warn("Failed drawing overlay to " + movie.getBaseName() + ". Please check that " + source + "_overlay_" + imageType + ".png is in the resources directory.");
//            logger.error(SystemTools.getStackTrace(error));
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
    private BufferedImage drawLanguage(IMovieBasicInformation movie, BufferedImage bi, int left, int top) {
        String lang = movie.getLanguage();

        if (StringTools.isValidString(lang)) {
            String[] languages = lang.split("/");

            StringBuilder fullLanguage = new StringBuilder();
            for (String language : languages) {
                if (fullLanguage.length() > 0) {
                    fullLanguage.append("_");
                }
                fullLanguage.append(language.trim());
            }

            try {

                Graphics2D g2d = bi.createGraphics();
                File imageFile = new File(getResourcesPath() + "languages" + File.separator + fullLanguage + ".png");
                if (imageFile.exists()) {
                    BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                    g2d.drawImage(biLang, 1, 1, null);
                } else {
                    if (languages.length == 1) {
                        logger.warn("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                        logger.warn("Please check that language specific graphic (" + fullLanguage + ".png) is in the resources/languages directory.");
                    } else {
                        logger.debug("Unable to find multiple language image (" + fullLanguage
                                + ".png) in the resources/languages directory, generating it from single one.");
                        int width = -1;
                        int height = -1;
                        int nbCols = (int) Math.sqrt(languages.length);
                        int nbRows = languages.length / nbCols;

                        BufferedImage[] imageFiles = new BufferedImage[languages.length];
                        // Looking for image file
                        for (int i = 0; i < languages.length; i++) {
                            String language = languages[i].trim();
                            imageFile = new File(getResourcesPath() + "languages" + File.separator + language + ".png");
                            if (imageFile.exists()) {

                                BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                                imageFiles[i] = biLang;

                                // Determine image size.
                                if (width == -1) {

                                    width = biLang.getWidth() / nbCols;
                                    height = biLang.getHeight() / nbRows;
                                }
                            } else {
                                logger.warn("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                                logger.warn("Please check that language specific graphic (" + language + ".png) is in the resources/languages directory.");
                            }
                        }

                        for (int i = 0; i < imageFiles.length; i++) {
                            int indexCol = (i) % nbCols;
                            int indexRow = (i / nbCols);
                            g2d.drawImage(imageFiles[i], left + (width * indexCol), top + (height * indexRow), width, height, null);
                        }
                    }
                }

                g2d.dispose();
            } catch (IOException e) {
                logger.warn("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                logger.warn("Please check that language specific graphic (" + lang + ".png) is in the resources/languages directory.");
            }

        }

        return bi;
    }

    private BufferedImage drawBlock(IMovieBasicInformation movie, BufferedImage bi, String name, String files, int left, String align, String Width, int top, String valign, String Height) {
        if (StringTools.isValidString(files)) {
            String[] filenames = files.split(" / ");
            try {
                Graphics2D g2d = bi.createGraphics();
                BufferedImage biSet = GraphicTools.loadJPEGImage(overlayResources + filenames[0]);
                List<String> uniqueFiles = new ArrayList<String>();
                uniqueFiles.add(filenames[0]);
                int width = Width.matches("\\d+") ? Integer.parseInt(Width) : biSet.getWidth();
                int height = Height.matches("\\d+") ? Integer.parseInt(Height) : biSet.getHeight();
                logosBlock block = overlayBlocks.get(name);
                int cols = block.cols;
                int rows = block.rows;
                boolean clones = block.clones;
                if (filenames.length > 1) {
                    if (cols == 0 && rows == 0) {
                        cols = (int) Math.sqrt(filenames.length);
                        rows = (int) (filenames.length / cols);
                    } else if (cols == 0) {
                        cols = (int) (filenames.length / rows);
                    } else if (rows == 0) {
                        rows = (int) (filenames.length / cols);
                    }
                    if (block.size) {
                        width = (int) (width / cols);
                        height = (int) (height / rows);
                    }
                }
                int maxWidth = width;
                int maxHeight = height;
                g2d.drawImage(biSet, getOverlayX(bi.getWidth(), width, left, align), getOverlayY(bi.getHeight(), height, top, valign), width, height, null);
                if ((filenames.length > 1) && (block != null)) {
                    int col = 0;
                    int row = 0;
                    int offsetX = block.dir ? width : 0;
                    int offsetY = block.dir ? 0 : height;
                    for (int i = 1; i < filenames.length; i++) {
                        if (!clones) {
                            if (uniqueFiles.contains(filenames[i])) {
                                continue;
                            } else {
                                uniqueFiles.add(filenames[i]);
                            }
                        }
                        if (block.dir) {
                            col++;
                            if (block.cols > 0 && col >= cols) {
                                col = 0;
                                row++;
                                if (block.rows > 0 && row >= rows) {
                                    break;
                                }
                            }
                        } else {
                            row++;
                            if (block.rows > 0 && row >= rows) {
                                row = 0;
                                col++;
                                if (block.cols > 0 && col >= cols) {
                                    break;
                                }
                            }
                        }
                        biSet = GraphicTools.loadJPEGImage(overlayResources + filenames[i]);
                        if (block.size || Width.equalsIgnoreCase("equal") || Width.matches("\\d+")) {
                            offsetX = (left > 0 ? 1 : -1) * col * (width + block.hmargin);
                        } else if (Width.equalsIgnoreCase("auto")) {
                            width = biSet.getWidth();
                            offsetX = block.dir ? col == 0 ? 0 : offsetX : row == 0 ? (offsetX + maxWidth) : offsetX;
                        }
                        if (block.size || Height.equalsIgnoreCase("equal") || Height.matches("\\d+")) {
                            offsetY = (top > 0 ? 1 : -1) * row * (height + block.vmargin);
                        } else if (Height.equalsIgnoreCase("auto")) {
                            height = biSet.getHeight();
                            offsetY = block.dir ? col == 0 ? (offsetY + maxHeight) : offsetY : row == 0 ? 0 : offsetY;
                        }
                        g2d.drawImage(biSet, getOverlayX(bi.getWidth(), width, left + offsetX, align),
                                getOverlayY(bi.getHeight(), height, top + offsetY, valign),
                                width, height, null);
                        if (!block.size && Width.equalsIgnoreCase("auto")) {
                            if (block.dir) {
                                offsetX += (left > 0 ? 1 : -1) * width;
                            } else {
                                maxWidth = (maxWidth < width || row == 0) ? width : maxWidth;
                            }
                        }
                        if (!block.size && Height.equalsIgnoreCase("auto")) {
                            if (block.dir) {
                                maxHeight = (maxHeight < height || col == 0) ? height : maxHeight;
                            } else {
                                offsetY += (top > 0 ? 1 : -1) * height;
                            }
                        }
                    }
                }
                g2d.dispose();
            } catch (IOException e) {
                logger.warn("Failed drawing overlay block logo to thumbnail file: " + movie.getBaseName());
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

            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(biSet, bi.getWidth() - biSet.getWidth() - 5, 1, null);
            g2d.dispose();
        } catch (IOException error) {
            logger.warn("Failed drawing set logo to thumbnail file:" + "Please check that set graphic (set.png) is in the resources directory.");
        }

        return bi;
    }

    private BufferedImage drawSetSize(Movie movie, BufferedImage bi) {
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
            logger.debug("Size (" + movie.getSetSize() + ") of set [" + movie.getTitle() + "] was drawn");
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

    protected BufferedImage drawPerspective(Identifiable movie, BufferedImage bi) {
        return bi;
    }

    private static BufferedImage drawText(BufferedImage bi, String outputText, boolean verticalAlign) {
        Graphics2D g2d = bi.createGraphics();
        g2d.setFont(new Font(textFont, Font.BOLD, textFontSize));
        FontMetrics fm = g2d.getFontMetrics();
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
            g2d.setColor(getColor(textFontShadow, Color.DARK_GRAY));
            g2d.drawString(outputText, leftAlignment + 2, topAlignment + 2);
        }

        // Create the text itself
        g2d.setColor(getColor(textFontColor, Color.LIGHT_GRAY));
        g2d.drawString(outputText, leftAlignment, topAlignment);

        g2d.dispose();
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

    // Issue 1937: Overlay configuration XML
    private class positionOverlay {

        Integer left = 0;
        Integer top = 0;
        String align = "left";
        String valign = "top";
        String width = "equal";
        String height = "equal";

        public positionOverlay() {
        }

        public positionOverlay(Integer left, Integer top, String align, String valign, String width, String height) {
            this.left = left;
            this.top = top;
            this.align = align;
            this.valign = valign;
            this.width = width;
            this.height = height;
        }
    }

    private class imageOverlay {

        String name;
        String value;
        List<String> values = new ArrayList<String>();
        String filename;

        public imageOverlay(String name, String value, String filename, List<String> values) {
            this.name = name;
            this.value = value;
            this.filename = filename;
            this.values = values;
        }
    }

    private class conditionOverlay {

        List<String> values = new ArrayList<String>();
        List<positionOverlay> positions = new ArrayList<positionOverlay>();
    }

    private class logoOverlay extends positionOverlay {

        boolean before = true;
        List<String> names = new ArrayList<String>();
        List<imageOverlay> images = new ArrayList<imageOverlay>();
        List<conditionOverlay> positions = new ArrayList<conditionOverlay>();
    }

    private class stateOverlay extends positionOverlay {

        String value = Movie.UNKNOWN;
        String filename = Movie.UNKNOWN;

        public stateOverlay(Integer left, Integer top, String align, String valign, String width, String height, String value) {
            this.left = left;
            this.top = top;
            this.align = align;
            this.valign = valign;
            this.width = width;
            this.height = height;
            this.value = value;
        }
    }

    private class logosBlock {

        boolean dir = false;         // true - vertical, false - horizontal,
        boolean size = false;        // true - static, false - auto
        boolean clones = false;      // true - enabled clones
        Integer cols = 1;            // 0 - auto count
        Integer rows = 0;            // 0 - auto count
        Integer hmargin = 0;
        Integer vmargin = 0;

        public logosBlock(boolean dir, boolean size, String cols, String rows, String hmargin, String vmargin, boolean clones) {
            this.dir = dir;
            this.size = size;
            this.clones = clones;
            this.cols = cols.equalsIgnoreCase("auto") ? 0 : Integer.parseInt(cols);
            this.rows = rows.equalsIgnoreCase("auto") ? 0 : Integer.parseInt(rows);
            this.hmargin = Integer.parseInt(hmargin);
            this.vmargin = Integer.parseInt(vmargin);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillOverlayParams(String xmlOverlayFilename) {
        if (!xmlOverlayFilename.toUpperCase().endsWith("XML")) {
            return;
        }
        File xmlOverlayFile = new File(overlayRoot + xmlOverlayFilename);
        if (xmlOverlayFile.exists() && xmlOverlayFile.isFile()) {
            try {
                XMLConfiguration c = new XMLConfiguration(xmlOverlayFile);
                List<HierarchicalConfiguration> layers = c.configurationsAt("layer");
                overlayLayers.clear();
                int index = 0;
                for (HierarchicalConfiguration layer : layers) {
                    String name = layer.getString("name");
                    if (StringTools.isNotValidString(name)) {
                        continue;
                    }
                    logoOverlay overlay = new logoOverlay();

                    String after = layer.getString("[@after]");
                    if (StringTools.isValidString(after) && after.equalsIgnoreCase("true")) {
                        overlay.before = false;
                    }

                    String left = layer.getString("left");
                    String top = layer.getString("top");
                    String align = layer.getString("align");
                    String valign = layer.getString("valign");
                    String width = layer.getString("width");
                    String height = layer.getString("height");

                    overlay.names = Arrays.asList(name.split("/"));
                    if (StringTools.isValidString(left)) {
                        try {
                            overlay.left = Integer.parseInt(left);
                        } catch (Exception ignore) {
                        }
                    }
                    if (StringTools.isValidString(top)) {
                        try {
                            overlay.top = Integer.parseInt(top);
                        } catch (Exception ignore) {
                        }
                    }
                    if (StringTools.isValidString(align) && (align.equalsIgnoreCase("left") || align.equalsIgnoreCase("center") || align.equalsIgnoreCase("right"))) {
                        overlay.align = align;
                    }
                    if (StringTools.isValidString(valign) && (valign.equalsIgnoreCase("top") || valign.equalsIgnoreCase("center") || valign.equalsIgnoreCase("bottom"))) {
                        overlay.valign = valign;
                    }
                    if (StringTools.isValidString(width) && (width.equalsIgnoreCase("equal") || width.equalsIgnoreCase("auto") || width.matches("\\d+"))) {
                        overlay.width = width;
                    }
                    if (StringTools.isValidString(height) && (height.equalsIgnoreCase("equal") || height.equalsIgnoreCase("auto") || height.matches("\\d+"))) {
                        overlay.height = height;
                    }

                    List<HierarchicalConfiguration> images = c.configurationsAt("layer(" + index + ").images.image");
                    for (HierarchicalConfiguration image : images) {
                        name = image.getString("[@name]");
                        String value = image.getString("[@value]");
                        String filename = image.getString("[@filename]");

                        if (StringTools.isNotValidString(name)) {
                            name = overlay.names.get(0);
                        }
                        if (!overlay.names.contains(name) || StringTools.isNotValidString(value) || StringTools.isNotValidString(filename)) {
                            continue;
                        }

                        imageOverlay img = new imageOverlay(name, value, filename, Arrays.asList(value.split("/")));
                        if (img.values.size() > 1) {
                            for (int i = 0; i < overlay.names.size(); i++) {
                                if (img.values.size() <= i) {
                                    img.values.add(Movie.UNKNOWN);
                                } else if (StringTools.isNotValidString(img.values.get(i))) {
                                    img.values.set(i, Movie.UNKNOWN);
                                }
                            }
                        }
                        overlay.images.add(img);
                    }

                    if (overlay.names.size() > 1) {
                        List<HierarchicalConfiguration> positions = c.configurationsAt("layer(" + index + ").positions.position");
                        for (HierarchicalConfiguration position : positions) {
                            String value = position.getString("[@value]");
                            left = position.getString("[@left]");
                            top = position.getString("[@top]");
                            align = position.getString("[@align]");
                            valign = position.getString("[@valign]");
                            width = position.getString("[@width]");
                            height = position.getString("[@height]");

                            if (StringTools.isNotValidString(value)) {
                                continue;
                            }
                            conditionOverlay condition = new conditionOverlay();
                            condition.values = Arrays.asList(value.split("/"));
                            if (StringTools.isNotValidString(left)) {
                                left = Integer.toString(overlay.left);
                            }
                            if (StringTools.isNotValidString(top)) {
                                top = Integer.toString(overlay.top);
                            }
                            if (StringTools.isNotValidString(align)) {
                                align = overlay.align;
                            }
                            if (StringTools.isNotValidString(valign)) {
                                valign = overlay.valign;
                            }
                            if (StringTools.isNotValidString(width)) {
                                width = overlay.width;
                            }
                            if (StringTools.isNotValidString(height)) {
                                height = overlay.height;
                            }
                            List<String> lefts = Arrays.asList(left.split("/"));
                            List<String> tops = Arrays.asList(top.split("/"));
                            List<String> aligns = Arrays.asList(align.split("/"));
                            List<String> valigns = Arrays.asList(valign.split("/"));
                            List<String> widths = Arrays.asList(width.split("/"));
                            List<String> heights = Arrays.asList(height.split("/"));
                            for (int i = 0; i < overlay.names.size(); i++) {
                                if (StringTools.isNotValidString(condition.values.get(i))) {
                                    condition.values.set(i, Movie.UNKNOWN);
                                }
                                positionOverlay p = new positionOverlay((lefts.size() <= i || StringTools.isNotValidString(lefts.get(i))) ? overlay.left : Integer.parseInt(lefts.get(i)),
                                        (tops.size() <= i || StringTools.isNotValidString(tops.get(i))) ? overlay.top : Integer.parseInt(tops.get(i)),
                                        (aligns.size() <= i || StringTools.isNotValidString(aligns.get(i))) ? overlay.align : aligns.get(i),
                                        (valigns.size() <= i || StringTools.isNotValidString(valigns.get(i))) ? overlay.valign : valigns.get(i),
                                        (widths.size() <= i || StringTools.isNotValidString(widths.get(i))) ? overlay.width : widths.get(i),
                                        (heights.size() <= i || StringTools.isNotValidString(heights.get(i))) ? overlay.height : heights.get(i));
                                condition.positions.add(p);
                            }
                            overlay.positions.add(condition);
                        }
                    }
                    overlayLayers.add(overlay);
                    index++;
                }

                List<HierarchicalConfiguration> blocks = c.configurationsAt("block");
                overlayBlocks.clear();
                for (HierarchicalConfiguration block : blocks) {
                    String name = block.getString("name");
                    if (StringTools.isNotValidString(name)) {
                        continue;
                    }
                    String dir = block.getString("dir");
                    dir = StringTools.isNotValidString(dir) ? "horizontal" : dir;
                    String size = block.getString("size");
                    size = StringTools.isNotValidString(size) ? "auto" : size;
                    String cols = block.getString("cols");
                    cols = StringTools.isNotValidString(cols) ? "auto" : cols;
                    String rows = block.getString("rows");
                    rows = StringTools.isNotValidString(rows) ? "auto" : rows;
                    String hmargin = block.getString("hmargin");
                    hmargin = StringTools.isNotValidString(hmargin) ? "0" : hmargin;
                    String vmargin = block.getString("vmargin");
                    vmargin = StringTools.isNotValidString(vmargin) ? "0" : vmargin;
                    String clones = block.getString("clones");
                    overlayBlocks.put(name, new logosBlock(dir.equalsIgnoreCase("horizontal"),
                            size.equalsIgnoreCase("static"),
                            cols, rows, hmargin, vmargin, StringTools.isNotValidString(clones) ? blockClones : (clones.equalsIgnoreCase("true") ? true : (clones.equalsIgnoreCase("false") ? false : blockClones))));
                }
            } catch (Exception error) {
                logger.error("Failed parsing moviejukebox overlay configuration file: " + xmlOverlayFile.getName());
                logger.error(SystemTools.getStackTrace(error));
            }
        } else {
            logger.error("The moviejukebox overlay configuration file you specified is invalid: " + xmlOverlayFile.getName());
        }
    }

    protected boolean checkLogoEnabled(String name) {
        if (name.equalsIgnoreCase("language")) {
            return addLanguage;
        } else if (name.equalsIgnoreCase("subtitle")) {
            return addSubTitle;
        } else if (name.equalsIgnoreCase("set")) {
            return addSetLogo;
        } else if (name.equalsIgnoreCase("TV")) {
            return addTVLogo;
        } else if (name.equalsIgnoreCase("HD")) {
            return addHDLogo;
        } else if (name.equalsIgnoreCase("rating")) {
            return addRating;
        } else if (name.equalsIgnoreCase("videosource") || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
            return addVideoSource;
        } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
            return addVideoOut;
        } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
            return addVideoCodec;
        } else if (name.equalsIgnoreCase("audiocodec") || name.equalsIgnoreCase("acodec") || name.equalsIgnoreCase("AC")) {
            return addAudioCodec;
        } else if (name.equalsIgnoreCase("audiochannels") || name.equalsIgnoreCase("channels")) {
            return addAudioChannels;
        } else if (name.equalsIgnoreCase("audiolang") || name.equalsIgnoreCase("alang") || name.equalsIgnoreCase("AL")) {
            return addAudioLang;
        } else if (name.equalsIgnoreCase("container")) {
            return addContainer;
        } else if (name.equalsIgnoreCase("aspect")) {
            return addAspectRatio;
        } else if (name.equalsIgnoreCase("fps")) {
            return addFPS;
        } else if (name.equalsIgnoreCase("certification")) {
            return addCertification;
        } else if (name.equalsIgnoreCase("watched")) {
            return addWatched;
        } else if (name.equalsIgnoreCase("episode")) {
            return addEpisode;
        } else if (name.equalsIgnoreCase("top250")) {
            return addTop250;
        } else if (name.equalsIgnoreCase("keywords")) {
            return addKeywords;
        } else if (name.equalsIgnoreCase("country")) {
            return addCountry;
        } else if (name.equalsIgnoreCase("company")) {
            return addCompany;
        } else if (name.equalsIgnoreCase("award")) {
            return addAward;
        }
        return !PropertiesUtil.getProperty(name, Movie.UNKNOWN).equals(Movie.UNKNOWN);
    }

    protected int getOverlayX(int fieldWidth, int itemWidth, Integer left, String align) {
        if (align.equalsIgnoreCase("left")) {
            return (int) (left >= 0 ? left : fieldWidth + left);
        } else if (align.equalsIgnoreCase("right")) {
            return (int) (left >= 0 ? fieldWidth - left - itemWidth : -left - itemWidth);
        } else {
            return (int) (left == 0 ? ((fieldWidth - itemWidth) / 2) : left > 0 ? (fieldWidth / 2 + left) : (fieldWidth / 2 + left - itemWidth));
        }
    }

    protected int getOverlayY(int fieldHeight, int itemHeight, Integer top, String align) {
        if (align.equalsIgnoreCase("top")) {
            return (int) (top >= 0 ? top : fieldHeight + top);
        } else if (align.equalsIgnoreCase("bottom")) {
            return (int) (top >= 0 ? fieldHeight - top - itemHeight : -top - itemHeight);
        } else {
            return (int) (top == 0 ? ((fieldHeight - itemHeight) / 2) : top > 0 ? (fieldHeight / 2 + top) : (fieldHeight / 2 + top - itemHeight));
        }
    }

    protected void fillOverlayKeywords(HashMap<String, ArrayList<String>> data, String value) {
        data.clear();
        if (StringTools.isValidString(value)) {
            String[] temp = value.split(" ; ");
            for (int i = 0; i < temp.length; i++) {
                String[] values = temp[i].split(" / ");
                if (values.length > 1) {
                    ArrayList<String> arr = new ArrayList<String>(Arrays.asList(values));
                    data.put(values[0], arr);
                }
            }
        }
    }

    protected boolean cmpOverlayValue(String name, String condition, String value) {
        boolean result = ((name.equalsIgnoreCase("keywords") && value.indexOf(condition.toLowerCase()) > -1)
                || condition.equalsIgnoreCase(value)
                || condition.equalsIgnoreCase("default"));
        if (!result) {
            HashMap<String, ArrayList<String>> data;
            if (name.equalsIgnoreCase("rating")) {
                data = keywordsRating;
            } else if (name.equalsIgnoreCase("videosource") || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
                data = keywordsVideoSource;
            } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
                data = keywordsVideoOut;
            } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
                data = keywordsVideoCodec;
            } else if (name.equalsIgnoreCase("audiocodec") || name.equalsIgnoreCase("acodec") || name.equalsIgnoreCase("AC")) {
                data = keywordsAudioCodec;
            } else if (name.equalsIgnoreCase("audiochannels") || name.equalsIgnoreCase("channels")) {
                data = keywordsAudioChannels;
            } else if (name.equalsIgnoreCase("audiolang") || name.equalsIgnoreCase("alang") || name.equalsIgnoreCase("AL")) {
                data = keywordsAudioLang;
            } else if (name.equalsIgnoreCase("container")) {
                data = keywordsContainer;
            } else if (name.equalsIgnoreCase("aspect")) {
                data = keywordsAspectRatio;
            } else if (name.equalsIgnoreCase("fps")) {
                data = keywordsFPS;
            } else if (name.equalsIgnoreCase("certification")) {
                data = keywordsCertification;
            } else if (name.equalsIgnoreCase("keywords")) {
                data = keywordsKeywords;
            } else if (name.equalsIgnoreCase("country")) {
                data = keywordsCountry;
            } else if (name.equalsIgnoreCase("company")) {
                data = keywordsCompany;
            } else if (name.equalsIgnoreCase("award")) {
                data = keywordsAward;
            } else {
                return false;
            }
            ArrayList<String> arr = data.get(condition);
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    if (name.equalsIgnoreCase("keywords")) {
                        result = value.indexOf(arr.get(i).toString().toLowerCase()) > -1;
                    } else {
                        result = arr.get(i).toString().equalsIgnoreCase(value);
                    }
                    if (result) {
                        break;
                    }
                }
            }
        }
        return result;
    }
}
