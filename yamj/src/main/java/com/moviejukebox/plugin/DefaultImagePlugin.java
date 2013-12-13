/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Award;
import com.moviejukebox.model.AwardEvent;
import com.moviejukebox.model.Comparator.ValueComparator;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.MyColor;
import com.moviejukebox.model.overlay.ConditionOverlay;
import com.moviejukebox.model.overlay.ImageOverlay;
import com.moviejukebox.model.overlay.LogoOverlay;
import com.moviejukebox.model.overlay.LogosBlock;
import com.moviejukebox.model.overlay.PositionOverlay;
import com.moviejukebox.model.overlay.StateOverlay;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.SkinProperties;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.omertron.fanarttvapi.model.FTArtworkType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.sanselan.ImageReadException;

public class DefaultImagePlugin implements MovieImagePlugin {

    private static final Logger LOG = Logger.getLogger(DefaultImagePlugin.class);
    private static final String BANNER = "banners";
    private static final String POSTER = "posters";
    private static final String VIDEOIMAGE = "videoimages";
    private static final String THUMBNAIL = "thumbnails";
    private static final String FOOTER = "footer";
    private static final String CLEARART = FTArtworkType.CLEARART.toString().toLowerCase();
    private static final String CLEARLOGO = FTArtworkType.CLEARLOGO.toString().toLowerCase();
    private static final String SEASONTHUMB = FTArtworkType.SEASONTHUMB.toString().toLowerCase();
    private static final String TVTHUMB = FTArtworkType.TVTHUMB.toString().toLowerCase();
    private static final String CHARACTERART = FTArtworkType.CHARACTERART.toString().toLowerCase();
    private static final String MOVIEART = FTArtworkType.MOVIEART.toString().toLowerCase();
    private static final String MOVIEDISC = FTArtworkType.MOVIEDISC.toString().toLowerCase();
    private static final String MOVIELOGO = FTArtworkType.MOVIELOGO.toString().toLowerCase();
    private static final List<String> validImageTypes = Collections.synchronizedList(new ArrayList<String>());
    private static final String CENTER = "center";
    private static final String BOTTOM = "bottom";
    private String overlayRoot;
    private final String overlayResources;
    // Filenames
    private static final String FILENAME_SET = "set.png";
    private static final String FILENAME_SUBTITLE = "subtitle.png";
    private static final String FILENAME_TV = "tv.png";
    private static final String FILENAME_HD1080 = "hd-1080.png";
    private static final String FILENAME_HD720 = "hd-720.png";
    private static final String FILENAME_HD = "hd.png";
    // Literals
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String BLOCK = "block";
    private static final String DEFAULT = "default";
    private static final String COLOUR_WHITE = "255/255/255";
    private static final String VIDEOSOURCE = "videosource";
    private static final String LANGUAGE = "language";
    private static final String ACODEC = "acodec";
    private static final String ALANG = "alang";
    private static final String AUDIOCODEC = "audiocodec";
    private static final String AUDIOLANG = "audiolang";
    private static final String AUDIOCHANNELS = "audiochannels";
    private static final String CHANNELS = "channels";
    private static final String CONTAINER = "container";
    private static final String CERTIFICATION = "certification";
    private static final String KEYWORDS = "keywords";
    private static final String COUNTRY = "country";
    private static final String COMPANY = "company";
    private static final String AWARD = "award";
    private static final String EQUAL = "equal";
    private static final String AUTO = "auto";
    private static final String TOP = "top";
    private static final String D_PLUS = "\\d+";
    private static final String MSG_VALID = ", please ensure it is valid";
    private static final String MSG_RESOURCES = " is in the resources directory.";
    //stretch images
    private boolean addHDLogo;
    private boolean addTVLogo;
    private boolean addSetLogo;
    private boolean addSubTitle;
    private boolean blockSubTitle;
    private boolean addLanguage;
    private boolean blockLanguage;
    private final boolean highdefDiff;
    private boolean addTextSetSize;
    private String textAlignment;
    private String textFont;
    private int textFontSize;
    private String textFontColor;
    private String textFontShadow;
    private int textOffset;
    private String imageType;
    private boolean roundCorners;
    private int cornerRadius;
    // cornerQuality/rcqfactor to improve roundCorner Quality
    private float rcqFactor;
    private int frameSize;
    private String frameColorHD;
    private String frameColor720;
    private String frameColor1080;
    private String frameColorSD;
    private String overlaySource;
    // Issue 1937: Overlay configuration XML
    private final List<LogoOverlay> overlayLayers = new ArrayList<LogoOverlay>();
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
    private boolean awardEventName;
    private boolean blockAward;
    private boolean countAward;
    private boolean blockClones;
    private boolean addEpisode;
    private boolean blockEpisode;
    private final Map<String, ArrayList<String>> keywordsRating = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsVideoSource = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsVideoOut = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsVideoCodec = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsAudioCodec = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsAudioChannels = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsAudioLang = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsContainer = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsAspectRatio = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsFPS = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsCertification = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsKeywords = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsCountry = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsCompany = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> keywordsAward = new HashMap<String, ArrayList<String>>();
    private final Map<String, LogosBlock> overlayBlocks = new HashMap<String, LogosBlock>();
    private int viIndex;

    public DefaultImagePlugin() {
        // Generic properties
        String skinHome = SkinProperties.getSkinHome();
        boolean skinRoot = PropertiesUtil.getBooleanProperty("mjb.overlay.skinroot", Boolean.TRUE);
        overlayRoot = PropertiesUtil.getProperty("mjb.overlay.dir", Movie.UNKNOWN);
        overlayRoot = (skinRoot ? (skinHome + File.separator) : "") + (StringTools.isValidString(overlayRoot) ? (overlayRoot + File.separator) : "");
        overlayResources = overlayRoot + PropertiesUtil.getProperty("mjb.overlay.resources", "resources") + File.separator;
        highdefDiff = PropertiesUtil.getBooleanProperty("highdef.differentiate", Boolean.FALSE);

        synchronized (validImageTypes) {
            if (validImageTypes.isEmpty()) {
                validImageTypes.add(BANNER);
                validImageTypes.add(POSTER);
                validImageTypes.add(VIDEOIMAGE);
                validImageTypes.add(THUMBNAIL);
                validImageTypes.add(FOOTER);
                validImageTypes.add(CLEARART);
                validImageTypes.add(CLEARLOGO);
                validImageTypes.add(SEASONTHUMB);
                validImageTypes.add(TVTHUMB);
                validImageTypes.add(CHARACTERART);
                validImageTypes.add(MOVIEART);
                validImageTypes.add(MOVIEDISC);
                validImageTypes.add(MOVIELOGO);
            }
        }
    }

    @Override
    public BufferedImage generate(Movie movie, BufferedImage imageGraphic, final String gImageType, final String perspectiveDirection) {
        imageType = gImageType.toLowerCase();

        boolean isFooter = false;

        viIndex = 0;
        if (imageType.indexOf(FOOTER) == 0) {
            isFooter = true;
            imageType = imageType.replaceFirst(FOOTER, "");
        } else if (imageType.indexOf(VIDEOIMAGE) == 0 && !imageType.equals(VIDEOIMAGE)) {
            viIndex = Integer.parseInt(imageType.replaceFirst(VIDEOIMAGE, ""));
            viIndex = (viIndex > 0 && viIndex <= movie.getFiles().size()) ? (viIndex - 1) : 0;
            imageType = VIDEOIMAGE;
        } else if (!validImageTypes.contains(imageType)) {
            // This is an error with the calling function
            LOG.error("YAMJ Error with calling function in DefaultImagePlugin.java");
            LOG.error("The image type '" + imageType + "' cannot be found");
            return imageGraphic;
        }

        // The properties must be loaded after the imageType has been determined
        boolean addReflectionEffect = PropertiesUtil.getBooleanProperty(imageType + ".reflection", Boolean.FALSE);
        boolean addPerspective = PropertiesUtil.getBooleanProperty(imageType + ".perspective", Boolean.FALSE);
        boolean imageNormalize = PropertiesUtil.getBooleanProperty(imageType + ".normalize", Boolean.FALSE);
        boolean imageStretch = PropertiesUtil.getBooleanProperty(imageType + ".stretch", Boolean.FALSE);
        boolean addOverlay = PropertiesUtil.getBooleanProperty(imageType + ".overlay", Boolean.FALSE);

        // Specific Properties (dependent upon the imageType)
        int imageWidth = PropertiesUtil.getIntProperty(imageType + ".width", 400);
        int imageHeight = PropertiesUtil.getIntProperty(imageType + ".height", 600);
        addHDLogo = PropertiesUtil.getBooleanProperty(imageType + ".logoHD", Boolean.FALSE);
        addTVLogo = PropertiesUtil.getBooleanProperty(imageType + ".logoTV", Boolean.FALSE);

        String tmpSubTitle = PropertiesUtil.getProperty(imageType + ".logoSubTitle", FALSE);
        blockSubTitle = tmpSubTitle.equalsIgnoreCase(BLOCK);
        addSubTitle = tmpSubTitle.equalsIgnoreCase(TRUE) || blockSubTitle;

        String tmpLanguage = PropertiesUtil.getProperty(imageType + ".language", FALSE);
        blockLanguage = tmpLanguage.equalsIgnoreCase(BLOCK);
        addLanguage = tmpLanguage.equalsIgnoreCase(TRUE) || blockLanguage;

        String tmpSetLogo = PropertiesUtil.getProperty(imageType + ".logoSet", FALSE);
        countSetLogo = tmpSetLogo.equalsIgnoreCase("count");
        addSetLogo = tmpSetLogo.equalsIgnoreCase(TRUE) || countSetLogo; // Note: This should only be for thumbnails

        boolean addTextTitle = PropertiesUtil.getBooleanProperty(imageType + ".addText.title", Boolean.FALSE);
        boolean addTextSeason = PropertiesUtil.getBooleanProperty(imageType + ".addText.season", Boolean.FALSE);
        addTextSetSize = PropertiesUtil.getBooleanProperty(imageType + ".addText.setSize", Boolean.FALSE); // Note: This should only be for thumbnails
        textAlignment = PropertiesUtil.getProperty(imageType + ".addText.alignment", LEFT);
        textFont = PropertiesUtil.getProperty(imageType + ".addText.font", "Helvetica");
        textFontSize = PropertiesUtil.getIntProperty(imageType + ".addText.fontSize", 36);
        textFontColor = PropertiesUtil.getProperty(imageType + ".addText.fontColor", "LIGHT_GRAY");
        textFontShadow = PropertiesUtil.getProperty(imageType + ".addText.fontShadow", "DARK_GRAY");
        textOffset = PropertiesUtil.getIntProperty(imageType + ".addText.offset", 10);
        roundCorners = PropertiesUtil.getBooleanProperty(imageType + ".roundCorners", Boolean.FALSE);
        cornerRadius = PropertiesUtil.getIntProperty(imageType + ".cornerRadius", 25);
        int cornerQuality = PropertiesUtil.getIntProperty(imageType + ".cornerQuality", 0);

        int overlayOffsetX = PropertiesUtil.getIntProperty(imageType + ".overlay.offsetX", 0);
        int overlayOffsetY = PropertiesUtil.getIntProperty(imageType + ".overlay.offsetY", 0);
        overlaySource = PropertiesUtil.getProperty(imageType + ".overlay.source", DEFAULT);

        boolean addFrame = PropertiesUtil.getBooleanProperty(imageType + ".addFrame", Boolean.FALSE);
        frameSize = PropertiesUtil.getIntProperty(imageType + ".frame.size", 5);
        frameColorSD = PropertiesUtil.getProperty(imageType + ".frame.colorSD", COLOUR_WHITE);
        frameColorHD = PropertiesUtil.getProperty(imageType + ".frame.colorHD", COLOUR_WHITE);
        frameColor720 = PropertiesUtil.getProperty(imageType + ".frame.color720", COLOUR_WHITE);
        frameColor1080 = PropertiesUtil.getProperty(imageType + ".frame.color1080", COLOUR_WHITE);

        // Issue 1937: Overlay configuration XML
        String tmpRating = PropertiesUtil.getProperty(imageType + ".rating", FALSE);
        realRating = tmpRating.equalsIgnoreCase("real");
        addRating = tmpRating.equalsIgnoreCase(TRUE) || realRating;

        String tmpAudioCodec = PropertiesUtil.getProperty(imageType + ".audiocodec", FALSE);
        blockAudioCodec = tmpAudioCodec.equalsIgnoreCase(BLOCK);
        addAudioCodec = tmpAudioCodec.equalsIgnoreCase(TRUE) || blockAudioCodec;

        String tmpAudioChannels = PropertiesUtil.getProperty(imageType + ".audiochannels", FALSE);
        blockAudioChannels = tmpAudioChannels.equalsIgnoreCase(BLOCK);
        addAudioChannels = tmpAudioChannels.equalsIgnoreCase(TRUE) || blockAudioChannels;

        String tmpAudioLang = PropertiesUtil.getProperty(imageType + ".audiolang", FALSE);
        blockAudioLang = tmpAudioCodec.equalsIgnoreCase(BLOCK);
        addAudioLang = tmpAudioLang.equalsIgnoreCase(TRUE) || blockAudioLang;

        addVideoSource = PropertiesUtil.getBooleanProperty(imageType + ".videosource", Boolean.FALSE);
        addVideoOut = PropertiesUtil.getBooleanProperty(imageType + ".videoout", Boolean.FALSE);
        addVideoCodec = PropertiesUtil.getBooleanProperty(imageType + ".videocodec", Boolean.FALSE);
        addContainer = PropertiesUtil.getBooleanProperty(imageType + ".container", Boolean.FALSE);
        addAspectRatio = PropertiesUtil.getBooleanProperty(imageType + ".aspect", Boolean.FALSE);
        addFPS = PropertiesUtil.getBooleanProperty(imageType + ".fps", Boolean.FALSE);
        addCertification = PropertiesUtil.getBooleanProperty(imageType + ".certification", Boolean.FALSE);

        String tmpWatched = PropertiesUtil.getProperty(imageType + ".watched", FALSE);
        blockWatched = tmpWatched.equalsIgnoreCase(BLOCK);
        addWatched = tmpWatched.equalsIgnoreCase(TRUE) || blockWatched;

        String tmpEpisode = PropertiesUtil.getProperty(imageType + ".episode", FALSE);
        blockEpisode = tmpEpisode.equalsIgnoreCase(BLOCK);
        addEpisode = tmpEpisode.equalsIgnoreCase(TRUE) || blockEpisode;

        addTop250 = PropertiesUtil.getBooleanProperty(imageType + ".top250", Boolean.FALSE);
        addKeywords = PropertiesUtil.getBooleanProperty(imageType + ".keywords", Boolean.FALSE);
        blockClones = PropertiesUtil.getBooleanProperty(imageType + ".clones", Boolean.FALSE);

        String tmpCountry = PropertiesUtil.getProperty(imageType + ".country", FALSE);
        blockCountry = tmpCountry.equalsIgnoreCase(BLOCK);
        addCountry = tmpCountry.equalsIgnoreCase(TRUE) || blockCountry;

        String tmpCompany = PropertiesUtil.getProperty(imageType + ".company", FALSE);
        blockCompany = tmpCompany.equalsIgnoreCase(BLOCK);
        addCompany = tmpCompany.equalsIgnoreCase(TRUE) || blockCompany;

        String tmpAward = PropertiesUtil.getProperty(imageType + ".award", FALSE);
        blockAward = tmpAward.equalsIgnoreCase(BLOCK);
        countAward = tmpAward.equalsIgnoreCase("count");
        addAward = tmpAward.equalsIgnoreCase(TRUE) || blockAward || countAward;
        awardEventName = PropertiesUtil.getBooleanProperty(imageType + ".award.useEventName", Boolean.FALSE);

        xmlOverlay = PropertiesUtil.getBooleanProperty(imageType + ".xmlOverlay", Boolean.FALSE);
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

        float ratio = (float) imageWidth / (float) imageHeight;

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

            // Normalize the image
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
                String perspDir;
                if (perspectiveDirection == null) { // make sure the perspectiveDirection is populated {
                    perspDir = PropertiesUtil.getProperty(imageType + ".perspectiveDirection", RIGHT);
                } else {
                    perspDir = perspectiveDirection;
                }

                bi = GraphicTools.create3DPicture(bi, isFooter ? FOOTER : imageType, perspDir);
            }
        }

        return bi;
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
     *
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
     * @param movie The source movie
     * @param bi The image to draw on
     * @param imageType
     * @param beforeMainOverlay
     * @return The new image with the added logos
     */
    protected BufferedImage drawLogos(Movie movie, BufferedImage bi, String imageType, boolean beforeMainOverlay) {
        BufferedImage newBi = bi;

        // Issue 1937: Overlay configuration XML
        if (xmlOverlay) {
            for (LogoOverlay layer : overlayLayers) {
                if (layer.isBefore() != beforeMainOverlay) {
                    continue;
                }

                boolean flag = false;
                List<StateOverlay> states = new ArrayList<StateOverlay>();
                for (String name : layer.getNames()) {
                    String value = Movie.UNKNOWN;
                    if (checkLogoEnabled(name)) {
                        if (name.equalsIgnoreCase("set")) {
                            value = ((imageType.equalsIgnoreCase(THUMBNAIL) || imageType.equalsIgnoreCase(BANNER) || imageType.equalsIgnoreCase(FOOTER)) && movie.isSetMaster()) ? countSetLogo ? Integer.toString(movie.getSetSize()) : TRUE : countSetLogo ? "0" : FALSE;
                        } else if (name.equalsIgnoreCase("TV")) {
                            value = movie.isTVShow() ? TRUE : FALSE;
                        } else if (name.equalsIgnoreCase("HD")) {
                            value = movie.isHD() ? highdefDiff ? movie.isHD1080() ? "hd1080" : "hd720" : "hd" : FALSE;
                        } else if (name.equalsIgnoreCase("subtitle") || name.equalsIgnoreCase("ST")) {
                            value = (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO")) ? FALSE : (blockSubTitle ? movie.getSubtitles() : TRUE);
                        } else if (name.equalsIgnoreCase(LANGUAGE)) {
                            value = movie.getLanguage();
                        } else if (name.equalsIgnoreCase("rating")) {
                            value = ((!movie.isTVShow() && !movie.isSetMaster()) || (movie.isTVShow() && movie.isSetMaster())) ? Integer.toString(realRating ? movie.getRating() : (int) (Math.floor(movie.getRating() / 10) * 10)) : Movie.UNKNOWN;
                        } else if (name.equalsIgnoreCase(VIDEOSOURCE) || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
                            value = movie.getVideoSource();
                        } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
                            value = movie.getVideoOutput();
                        } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
                            value = movie.getVideoCodec();
                        } else if (name.equalsIgnoreCase(AUDIOCODEC) || name.equalsIgnoreCase(ACODEC) || name.equalsIgnoreCase("AC")) {
                            value = movie.getAudioCodec();
                            if (!blockAudioCodec) {
                                int pos = value.indexOf(Movie.SPACE_SLASH_SPACE);
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                                pos = value.indexOf(" (");
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            } else {
                                while (value.contains(" (") && value.indexOf(" (") < value.indexOf(')')) {
                                    value = value.substring(0, value.indexOf(" (")) + value.substring(value.indexOf(')') + 1);
                                }
                            }
                        } else if (name.equalsIgnoreCase(AUDIOLANG) || name.equalsIgnoreCase(ALANG) || name.equalsIgnoreCase("AL")) {
                            value = "";
                            for (String tmp : movie.getAudioCodec().split(Movie.SPACE_SLASH_SPACE)) {
                                if (tmp.contains(" (") && tmp.indexOf(" (") < tmp.indexOf(")")) {
                                    tmp = tmp.substring(tmp.indexOf(" (") + 2, tmp.indexOf(")"));
                                } else {
                                    tmp = Movie.UNKNOWN;
                                }
                                if (!blockAudioLang) {
                                    value = tmp;
                                    break;
                                }
                                if (StringUtils.isNotBlank(value)) {
                                    value += Movie.SPACE_SLASH_SPACE;
                                }
                                value += tmp;
                            }
                            if (value.equals("")) {
                                value = Movie.UNKNOWN;
                            }
                        } else if (name.equalsIgnoreCase(AUDIOCHANNELS) || name.equalsIgnoreCase(CHANNELS)) {
                            value = movie.getAudioChannels();
                            if (!blockAudioChannels) {
                                int pos = value.indexOf(Movie.SPACE_SLASH_SPACE);
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase(CONTAINER)) {
                            value = movie.getContainer();
                        } else if (name.equalsIgnoreCase("aspect")) {
                            value = movie.getAspectRatio();
                        } else if (name.equalsIgnoreCase("fps")) {
                            value = Float.toString(movie.getFps());
                        } else if (name.equalsIgnoreCase(CERTIFICATION)) {
                            value = movie.getCertification();
                        } else if (name.equalsIgnoreCase("watched")) {
                            if (imageType.equalsIgnoreCase(VIDEOIMAGE)) {
                                value = movie.getFiles().toArray(new MovieFile[movie.getFiles().size()])[viIndex].isWatched() ? TRUE : FALSE;
                            } else if (movie.isTVShow() && blockWatched) {
                                StringBuilder sbWatched = new StringBuilder();
                                boolean first = true;
                                for (MovieFile mf : movie.getFiles()) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        sbWatched.append(Movie.SPACE_SLASH_SPACE);
                                    }
                                    sbWatched.append(mf.isWatched() ? TRUE : FALSE);
                                }
                                value = sbWatched.toString();
                            } else {
                                value = movie.isWatched() ? TRUE : FALSE;
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
                                        for (int part = firstPart; part <= lastPart; part++) {
                                            if (first) {
                                                first = false;
                                            } else {
                                                sbEpisode.append(Movie.SPACE_SLASH_SPACE);
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
                            value = movie.getTop250() > 0 ? TRUE : FALSE;
                        } else if (name.equalsIgnoreCase(KEYWORDS)) {
                            value = movie.getBaseFilename().toLowerCase();
                        } else if (name.equalsIgnoreCase(COUNTRY)) {
                            value = movie.getCountry();
                            if (!blockCountry) {
                                int pos = value.indexOf(Movie.SPACE_SLASH_SPACE);
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase(COMPANY)) {
                            value = movie.getCompany();
                            if (!blockCompany) {
                                int pos = value.indexOf(Movie.SPACE_SLASH_SPACE);
                                if (pos > -1) {
                                    value = value.substring(0, pos);
                                }
                            }
                        } else if (name.equalsIgnoreCase(AWARD)) {
                            value = "";
                            int awardCount = 0;
                            HashMap<String, Integer> awards = new HashMap<String, Integer>();
                            if (!movie.isSetMaster()) {
                                for (AwardEvent awardEvent : movie.getAwards()) {
                                    for (Award award : awardEvent.getAwards()) {
                                        if (award.getWon() > 0) {
                                            if (blockAward) {
                                                awards.put((awardEventName ? (awardEvent.getName() + " - ") : "") + award.getName(), award.getWon());
                                            } else if (countAward) {
                                                awardCount++;
                                            } else {
                                                value = TRUE;
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
                                boolean first = value.isEmpty();   // Append the separator only if the "value" is not empty

                                for (String award : sortedAwards.keySet()) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        sbAwards.append(Movie.SPACE_SLASH_SPACE);
                                    }
                                    sbAwards.append(award);
                                }
                                value += sbAwards.toString();
                            }
                            value = (StringTools.isNotValidString(value) && !countAward) ? blockAward ? Movie.UNKNOWN : FALSE : countAward ? Integer.toString(awardCount) : value;
                        } else {
                            value = PropertiesUtil.getProperty(name, Movie.UNKNOWN);
                        }
                    }
                    StateOverlay state = new StateOverlay(layer.getLeft(), layer.getTop(), layer.getAlign(), layer.getValign(), layer.getWidth(), layer.getHeight(), value);
                    states.add(state);
                }

                for (int inx = 0; inx < layer.getNames().size(); inx++) {
                    String name = layer.getNames().get(inx);
                    String value = states.get(inx).getValue();
                    String filename = Movie.UNKNOWN;
                    if (checkLogoEnabled(name)) {
                        if (!blockLanguage && name.equalsIgnoreCase(LANGUAGE) && StringTools.isValidString(value)) {
                            filename = "languages/English.png";
                        }
                        String[] values = value.split(Movie.SPACE_SLASH_SPACE);
                        for (String splitValue : values) {
                            value = splitValue;
                            for (ImageOverlay img : layer.getImages()) {
                                if (img.getName().equalsIgnoreCase(name)) {
                                    boolean accept = false;
                                    if (img.getValues().size() == 1 && cmpOverlayValue(name, img.getValue(), value)) {
                                        accept = true;
                                    } else if (img.getValues().size() > 1) {
                                        accept = true;
                                        for (int i = 0; i < layer.getNames().size(); i++) {
                                            accept = accept && cmpOverlayValue(layer.getNames().get(i), img.getValues().get(i), states.get(i).getValue());
                                            if (!accept) {
                                                break;
                                            }
                                        }
                                    }
                                    if (!accept) {
                                        continue;
                                    }
                                    File imageFile = new File(overlayResources + img.getFilename());
                                    if (imageFile.exists()) {
                                        if (StringTools.isNotValidString(filename)) {
                                            filename = img.getFilename();
                                        } else {
                                            filename += Movie.SPACE_SLASH_SPACE + img.getFilename();
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        flag = flag || StringTools.isValidString(filename);
                    }
                    states.get(inx).setFilename(filename);
                }

                if (!flag) {
                    continue;
                }

                if (layer.getPositions().size() > 0) {
                    for (ConditionOverlay cond : layer.getPositions()) {
                        flag = true;
                        for (int i = 0; i < layer.getNames().size(); i++) {
                            String name = layer.getNames().get(i);
                            String condition = cond.getValues().get(i);
                            String value = states.get(i).getValue();
                            flag = flag && cmpOverlayValue(name, condition, value);
                            if (!flag) {
                                break;
                            }
                        }
                        if (flag) {
                            for (int i = 0; i < layer.getNames().size(); i++) {
                                PositionOverlay pos = cond.getPositions().get(i);
                                states.get(i).setLeft(pos.getLeft());
                                states.get(i).setTop(pos.getTop());
                                states.get(i).setAlign(pos.getAlign());
                                states.get(i).setValign(pos.getValign());
                            }
                            break;
                        }
                    }
                }

                for (int i = 0; i < layer.getNames().size(); i++) {
                    StateOverlay state = states.get(i);
                    String name = layer.getNames().get(i);
                    if (!blockLanguage && name.equalsIgnoreCase(LANGUAGE)) {
                        newBi = drawLanguage(movie, newBi, getOverlayX(newBi.getWidth(), 62, state.getLeft(), state.getAlign()), getOverlayY(newBi.getHeight(), 40, state.getTop(), state.getValign()));
                        continue;
                    }

                    String filename = state.getFilename();
                    if (StringTools.isNotValidString(filename)) {
                        continue;
                    }

                    if (((blockAudioCodec && ((name.equalsIgnoreCase(AUDIOCODEC) || name.equalsIgnoreCase(ACODEC) || name.equalsIgnoreCase("AC"))))
                            || (blockAudioChannels && (name.equalsIgnoreCase(AUDIOCHANNELS) || name.equalsIgnoreCase(CHANNELS)))
                            || (blockAudioLang && (name.equalsIgnoreCase(AUDIOLANG) || name.equalsIgnoreCase(ALANG) || name.equalsIgnoreCase("AL")))
                            || (blockCountry && name.equalsIgnoreCase(COUNTRY))
                            || (blockCompany && name.equalsIgnoreCase(COMPANY))
                            || (blockAward && name.equalsIgnoreCase(AWARD))
                            || (blockWatched && name.equalsIgnoreCase("watched"))
                            || (blockEpisode && name.equalsIgnoreCase("episode"))
                            || (blockSubTitle && name.equalsIgnoreCase("subtitle"))
                            || (blockLanguage && name.equalsIgnoreCase(LANGUAGE)))
                            && (overlayBlocks.get(name) != null)) {
                        newBi = drawBlock(movie, newBi, name, filename, state.getLeft(), state.getAlign(), state.getWidth(), state.getTop(), state.getValign(), state.getHeight());
                        continue;
                    }

                    try {
                        BufferedImage biSet = GraphicTools.loadJPEGImage(overlayResources + filename);

                        Graphics2D g2d = newBi.createGraphics();
                        g2d.drawImage(biSet,
                                getOverlayX(newBi.getWidth(),
                                        biSet.getWidth(),
                                        state.getLeft(),
                                        state.getAlign()),
                                getOverlayY(newBi.getHeight(), biSet.getHeight(), state.getTop(), state.getValign()),
                                state.getWidth().matches(D_PLUS) ? Integer.parseInt(state.getWidth()) : biSet.getWidth(), state.getHeight().matches(D_PLUS) ? Integer.parseInt(state.getHeight()) : biSet.getHeight(), null);
                        g2d.dispose();
                    } catch (FileNotFoundException ex) {
                        LOG.warn("Failed to load " + overlayResources + filename + MSG_VALID);
                    } catch (ImageReadException ex) {
                        LOG.warn("Failed to read " + overlayResources + filename + MSG_VALID);
                    } catch (IOException ex) {
                        LOG.warn("Failed drawing overlay to image file: Please check that " + filename + MSG_RESOURCES);
                    }

                    if (name.equalsIgnoreCase("set")) {
                        newBi = drawSetSize(movie, newBi);
                    }
                }
            }
        } else if (beforeMainOverlay) {
            if (addHDLogo) {
                newBi = drawLogoHD(movie, newBi, addTVLogo);
            }

            if (addTVLogo) {
                newBi = drawLogoTV(movie, newBi, addHDLogo);
            }

            if (addLanguage) {
                newBi = drawLanguage(movie, newBi, 1, 1);
            }

            if (addSubTitle) {
                newBi = drawSubTitle(movie, newBi);
            }

            // Should only really happen on set's thumbnails.
            if (imageType.equalsIgnoreCase(THUMBNAIL) && movie.isSetMaster()) {
                // Draw the set logo if requested.
                if (addSetLogo) {
                    newBi = drawSet(movie, newBi);
                    LOG.debug("Drew set logo on " + movie.getTitle());
                }
                newBi = drawSetSize(movie, newBi);
            }
        }

        return newBi;
    }

    /**
     * Draw the SubTitle logo on the image
     *
     * @param movie
     * @param bi
     * @return
     */
    private BufferedImage drawSubTitle(Movie movie, BufferedImage bi) {
        // If the doesn't have subtitles, then quit
        if (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO")) {
            return bi;
        }

        File logoFile = new File(getResourcesPath() + FILENAME_SUBTITLE);

        if (!logoFile.exists()) {
            LOG.debug("Missing SubTitle logo (" + FILENAME_SUBTITLE + ") unable to draw logo");
            return bi;
        }

        try {
            BufferedImage biSubTitle = GraphicTools.loadJPEGImage(logoFile);
            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(biSubTitle, bi.getWidth() - biSubTitle.getWidth() - 5, 5, null);
            g2d.dispose();
        } catch (FileNotFoundException ex) {
            LOG.warn("Failed to load " + logoFile + MSG_VALID);
        } catch (ImageReadException ex) {
            LOG.warn("Failed to read " + FILENAME_SUBTITLE + MSG_VALID);
        } catch (IOException error) {
            LOG.warn("Failed drawing SubTitle logo to thumbnail file: Please check that " + FILENAME_SUBTITLE + MSG_RESOURCES);
        }

        return bi;
    }

    /**
     * Draw the appropriate HD logo onto the image file
     *
     * @param movie The source movie
     * @param bi The original image
     * @param addOtherLogo Do we need to draw the TV logo as well?
     * @return The new image file
     */
    private BufferedImage drawLogoHD(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        // If the movie isn't high definition, then quit
        if (!movie.isHD()) {
            return bi;
        }

        String logoFilename;
        File logoFile;

        // Determine which logo to use.
        if (highdefDiff) {
            if (movie.isHD1080()) {
                // Use the 1080p logo
                logoFilename = FILENAME_HD1080;
            } else {
                // Otherwise use the 720p
                logoFilename = FILENAME_HD720;
            }
        } else {
            // We don't care, so use the default HD logo.
            logoFilename = FILENAME_HD;
        }

        logoFile = new File(getResourcesPath() + logoFilename);
        if (!logoFile.exists()) {
            LOG.debug("Missing HD logo (" + logoFilename + ") using default " + FILENAME_HD);
            logoFilename = FILENAME_HD;
        }

        try {
            BufferedImage biHd = GraphicTools.loadJPEGImage(getResourcesPath() + logoFilename);
            Graphics2D g2d = bi.createGraphics();

            if (addOtherLogo && (movie.isTVShow())) {
                // Both logos are required, so put the HD logo on the LEFT
                g2d.drawImage(biHd, 5, bi.getHeight() - biHd.getHeight() - 5, null);
                LOG.debug("Drew HD logo (" + logoFilename + ") on the left");
            } else {
                // Only the HD logo is required so set it in the centre
                g2d.drawImage(biHd, bi.getWidth() / 2 - biHd.getWidth() / 2, bi.getHeight() - biHd.getHeight() - 5, null);
                LOG.debug("Drew HD logo (" + logoFilename + ") in the middle");
            }

            g2d.dispose();
        } catch (FileNotFoundException ex) {
            LOG.warn("Failed to load " + overlayResources + logoFilename + MSG_VALID);
        } catch (ImageReadException ex) {
            LOG.warn("Failed to read " + logoFilename + MSG_VALID);
        } catch (IOException ex) {
            LOG.warn("Failed drawing HD logo to thumbnail file: Please check that " + logoFilename + MSG_RESOURCES);
        }

        return bi;
    }

    /**
     * Draw the TV logo onto the image file
     *
     * @param movie The source movie
     * @param bi The original image
     * @param addOtherLogo Do we need to draw the HD logo as well?
     * @return The new image file
     */
    private BufferedImage drawLogoTV(Movie movie, BufferedImage bi, Boolean addOtherLogo) {
        if (movie.isTVShow()) {
            try {
                BufferedImage biTV = GraphicTools.loadJPEGImage(getResourcesPath() + FILENAME_TV);
                Graphics2D g2d = bi.createGraphics();

                if (addOtherLogo && movie.isHD()) {
                    // Both logos are required, so put the TV logo on the RIGHT
                    g2d.drawImage(biTV, bi.getWidth() - biTV.getWidth() - 5, bi.getHeight() - biTV.getHeight() - 5, null);
                    LOG.debug("Drew TV logo on the right");
                } else {
                    // Only the TV logo is required so set it in the centre
                    g2d.drawImage(biTV, bi.getWidth() / 2 - biTV.getWidth() / 2, bi.getHeight() - biTV.getHeight() - 5, null);
                    LOG.debug("Drew TV logo in the middle");
                }

                g2d.dispose();
            } catch (FileNotFoundException ex) {
                LOG.warn("Failed to load " + FILENAME_TV + MSG_VALID);
            } catch (ImageReadException ex) {
                LOG.warn("Failed to read " + FILENAME_TV + MSG_VALID);
            } catch (IOException error) {
                LOG.warn("Failed drawing TV logo to thumbnail file: Please check that " + FILENAME_TV + MSG_RESOURCES);
                LOG.error(SystemTools.getStackTrace(error));
            }
        }

        return bi;
    }

    /**
     * Draw an overlay on the image, such as a box cover specific for videosource, container, certification if wanted
     *
     * @param movie
     * @param bi
     * @param offsetY
     * @param offsetX
     * @return
     */
    private BufferedImage drawOverlay(Movie movie, BufferedImage bi, int offsetX, int offsetY) {

        String source;
        if (overlaySource.equalsIgnoreCase(VIDEOSOURCE)) {
            source = movie.getVideoSource();
        } else if (overlaySource.equalsIgnoreCase(CERTIFICATION)) {
            source = movie.getCertification();
        } else if (overlaySource.equalsIgnoreCase(CONTAINER)) {
            source = movie.getContainer();
        } else {
            source = DEFAULT;
        }

        // Make sure the source is formatted correctly
        source = source.toLowerCase().trim();

        // Check for a blank or an UNKNOWN source and correct it
        if (StringTools.isNotValidString(source)) {
            source = DEFAULT;
        }

        String overlayFilename = source + "_overlay_" + imageType + ".png";

        try {
            BufferedImage biOverlay = GraphicTools.loadJPEGImage(getResourcesPath() + overlayFilename);

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
        } catch (FileNotFoundException ex) {
            LOG.warn("Failed to load " + overlayFilename + MSG_VALID);
        } catch (ImageReadException ex) {
            LOG.warn("Failed to read " + overlayFilename + MSG_VALID);
        } catch (IOException ex) {
            LOG.warn("Failed drawing overlay to " + movie.getBaseName() + ". Please check that " + overlayFilename + MSG_RESOURCES);
        }

        return bi;
    }

    /**
     * Draw the language logo to the image
     *
     * @param movie Movie file, used to determine the language
     * @param bi The image file to draw on
     * @return The new image file with the language flag on it
     */
    private BufferedImage drawLanguage(IMovieBasicInformation movie, BufferedImage bi, int left, int top) {
        String lang = movie.getLanguage();

        if (StringTools.isValidString(lang)) {
            String[] languages = lang.split("/");

            StringBuilder fullLanguage = new StringBuilder();
            for (String language : languages) {
                // CHeck the language is valid before adding it
                if (StringTools.isValidString(language)) {
                    if (fullLanguage.length() > 0) {
                        fullLanguage.append("_");
                    }
                    fullLanguage.append(language.trim());
                }
            }

            // If there isn't a valid language, quit
            if (StringTools.isNotValidString(fullLanguage.toString())) {
                return bi;
            }

            String languageFilename = "languages" + File.separator + fullLanguage + ".png";

            try {
                Graphics2D g2d = bi.createGraphics();
                File imageFile = new File(getResourcesPath() + languageFilename);
                if (imageFile.exists()) {
                    BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                    g2d.drawImage(biLang, 1, 1, null);
                } else {
                    if (languages.length == 1) {
                        LOG.warn("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                        LOG.warn("Please check that language specific graphic (" + fullLanguage + ".png) is in the resources/languages directory.");
                    } else {
                        LOG.debug("Unable to find multiple language image (" + fullLanguage + ".png) in the resources/languages directory, generating it from single one.");
                        int width = -1;
                        int height = -1;
                        int nbCols = (int) Math.sqrt(languages.length);
                        int nbRows = languages.length / nbCols;

                        BufferedImage[] imageFiles = new BufferedImage[languages.length];
                        // Looking for image file
                        for (int i = 0; i < languages.length; i++) {
                            String language = languages[i].trim();
                            languageFilename = "languages" + File.separator + language + ".png";
                            imageFile = new File(getResourcesPath() + languageFilename);
                            if (imageFile.exists()) {

                                BufferedImage biLang = GraphicTools.loadJPEGImage(imageFile);
                                imageFiles[i] = biLang;

                                // Determine image size.
                                if (width == -1) {

                                    width = biLang.getWidth() / nbCols;
                                    height = biLang.getHeight() / nbRows;
                                }
                            } else {
                                LOG.warn("Failed drawing Language logo to thumbnail file: " + movie.getBaseName());
                                LOG.warn("Please check that language specific graphic (" + languageFilename + ") is in the resources/languages directory.");
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
            } catch (FileNotFoundException ex) {
                LOG.warn("Failed to load " + languageFilename + MSG_VALID);
            } catch (ImageReadException ex) {
                LOG.warn("Failed to read " + languageFilename + MSG_VALID);
            } catch (IOException ex) {
                LOG.warn("Exception drawing Language logo to thumbnail file '" + movie.getBaseName() + "': " + ex.getMessage());
                LOG.warn("Please check that language specific graphic (" + languageFilename + ") is in the resources/languages directory.");
            }
        }

        return bi;
    }

    private BufferedImage drawBlock(IMovieBasicInformation movie, BufferedImage bi, String name, String files, int left, String align, String width, int top, String valign, String height) {

        int currentFilenameNumber = 0;

        if (StringTools.isValidString(files)) {
            String[] filenames = files.split(Movie.SPACE_SLASH_SPACE);
            try {
                Graphics2D g2d = bi.createGraphics();
                BufferedImage biSet = GraphicTools.loadJPEGImage(overlayResources + filenames[currentFilenameNumber]);
                List<String> uniqueFiles = new ArrayList<String>();
                uniqueFiles.add(filenames[0]);
                int lWidth = width.matches(D_PLUS) ? Integer.parseInt(width) : biSet.getWidth();
                int lHeight = height.matches(D_PLUS) ? Integer.parseInt(height) : biSet.getHeight();
                LogosBlock block = overlayBlocks.get(name);
                int cols = block.getCols();
                int rows = block.getRows();
                boolean clones = block.isClones();
                if (filenames.length > 1) {
                    if (cols == 0 && rows == 0) {
                        cols = (int) Math.sqrt(filenames.length);
                        rows = (int) (filenames.length / cols);
                    } else if (cols == 0) {
                        cols = (int) (filenames.length / rows);
                    } else if (rows == 0) {
                        rows = (int) (filenames.length / cols);
                    }
                    if (block.isSize()) {
                        lWidth = (int) (lWidth / cols);
                        lHeight = (int) (lHeight / rows);
                    }
                }
                int maxWidth = lWidth;
                int maxHeight = lHeight;
                g2d.drawImage(biSet, getOverlayX(bi.getWidth(), lWidth, left, align), getOverlayY(bi.getHeight(), lHeight, top, valign), lWidth, lHeight, null);
                if (filenames.length > 1) {
                    int col = 0;
                    int row = 0;
                    int offsetX = block.isDir() ? lWidth : 0;
                    int offsetY = block.isDir() ? 0 : lHeight;
                    for (int i = 1; i < filenames.length; i++) {
                        if (!clones) {
                            if (uniqueFiles.contains(filenames[i])) {
                                continue;
                            } else {
                                uniqueFiles.add(filenames[i]);
                            }
                        }
                        if (block.isDir()) {
                            col++;
                            if (block.getCols() > 0 && col >= cols) {
                                col = 0;
                                row++;
                                if (block.getRows() > 0 && row >= rows) {
                                    break;
                                }
                            }
                        } else {
                            row++;
                            if (block.getRows() > 0 && row >= rows) {
                                row = 0;
                                col++;
                                if (block.getCols() > 0 && col >= cols) {
                                    break;
                                }
                            }
                        }

                        currentFilenameNumber = i;
                        biSet = GraphicTools.loadJPEGImage(overlayResources + filenames[currentFilenameNumber]);
                        if (block.isSize() || width.equalsIgnoreCase(EQUAL) || width.matches(D_PLUS)) {
                            offsetX = (left > 0 ? 1 : -1) * col * (lWidth + block.gethMargin());
                        } else if (width.equalsIgnoreCase(AUTO)) {
                            lWidth = biSet.getWidth();
                            offsetX = block.isDir() ? col == 0 ? 0 : offsetX : row == 0 ? (offsetX + maxWidth) : offsetX;
                        }
                        if (block.isSize() || height.equalsIgnoreCase(EQUAL) || height.matches(D_PLUS)) {
                            offsetY = (top > 0 ? 1 : -1) * row * (lHeight + block.getvMargin());
                        } else if (height.equalsIgnoreCase(AUTO)) {
                            lHeight = biSet.getHeight();
                            offsetY = block.isDir() ? col == 0 ? (offsetY + maxHeight) : offsetY : row == 0 ? 0 : offsetY;
                        }
                        g2d.drawImage(biSet, getOverlayX(bi.getWidth(), lWidth, left + offsetX, align),
                                getOverlayY(bi.getHeight(), lHeight, top + offsetY, valign),
                                lWidth, lHeight, null);
                        if (!block.isSize() && width.equalsIgnoreCase(AUTO)) {
                            if (block.isDir()) {
                                offsetX += (left > 0 ? 1 : -1) * lWidth;
                            } else {
                                maxWidth = (maxWidth < lWidth || row == 0) ? lWidth : maxWidth;
                            }
                        }
                        if (!block.isSize() && height.equalsIgnoreCase(AUTO)) {
                            if (block.isDir()) {
                                maxHeight = (maxHeight < lHeight || col == 0) ? lHeight : maxHeight;
                            } else {
                                offsetY += (top > 0 ? 1 : -1) * lHeight;
                            }
                        }
                    }
                }
                g2d.dispose();
            } catch (FileNotFoundException ex) {
                LOG.warn("Failed to load " + overlayResources + filenames[currentFilenameNumber] + MSG_VALID);
            } catch (ImageReadException ex) {
                LOG.warn("Failed to read " + filenames[currentFilenameNumber] + MSG_VALID);
            } catch (IOException e) {
                LOG.warn("Failed drawing overlay block logo (" + filenames[currentFilenameNumber] + ") to thumbnail file: " + movie.getBaseName());
            }
        }
        return bi;
    }

    /**
     * Draw the set logo onto a poster
     *
     * @param movie the movie to check
     * @param bi the image to draw on
     * @return the new buffered image
     */
    private BufferedImage drawSet(Movie movie, BufferedImage bi) {
        try {
            BufferedImage biSet = GraphicTools.loadJPEGImage(getResourcesPath() + FILENAME_SET);

            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(biSet, bi.getWidth() - biSet.getWidth() - 5, 1, null);
            g2d.dispose();
        } catch (FileNotFoundException ex) {
            LOG.warn("Failed to load " + FILENAME_SET + MSG_VALID);
        } catch (ImageReadException ex) {
            LOG.warn("Failed to read " + FILENAME_SET + MSG_VALID);
        } catch (IOException error) {
            LOG.warn("Failed drawing set logo to thumbnail for " + movie.getBaseFilename());
            LOG.warn("Please check that set graphic (" + FILENAME_SET + ") is in the resources directory. " + error.getMessage());
        }

        return bi;
    }

    private BufferedImage drawSetSize(Movie movie, BufferedImage bi) {
        // Let's draw the set's size (at bottom) if requested.
        final int size = movie.getSetSize();
        if (addTextSetSize && size > 0) {
            String text;
            // Let's draw not more than 9...
            if (size > 9) {
                text = "9+";
            } else {
                text = Integer.toString(size);
            }
            LOG.debug("Size (" + movie.getSetSize() + ") of set [" + movie.getTitle() + "] was drawn");
            return drawText(bi, text, false);
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

    private BufferedImage drawText(BufferedImage bi, String outputText, boolean verticalAlign) {
        Graphics2D g2d = bi.createGraphics();
        g2d.setFont(new Font(textFont, Font.BOLD, textFontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(outputText);
        int imageWidth = bi.getWidth();
        int imageHeight = bi.getHeight();
        int leftAlignment;
        int topAlignment;

        if (textAlignment.equalsIgnoreCase(LEFT)) {
            leftAlignment = textOffset;
        } else if (textAlignment.equalsIgnoreCase(RIGHT)) {
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
        Color returnColor = MyColor.get(color);
        if (returnColor == null) {
            return defaultColor;
        }
        return returnColor;
    }

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
                    LogoOverlay overlay = new LogoOverlay();

                    String after = layer.getString("[@after]");
                    if (StringTools.isValidString(after) && after.equalsIgnoreCase(TRUE)) {
                        overlay.setBefore(Boolean.FALSE);
                    }

                    String left = layer.getString(LEFT);
                    String top = layer.getString(TOP);
                    String align = layer.getString("align");
                    String valign = layer.getString("valign");
                    String width = layer.getString("width");
                    String height = layer.getString("height");

                    overlay.setNames(Arrays.asList(name.split("/")));
                    if (StringUtils.isNumeric(left)) {
                        overlay.setLeft(NumberUtils.toInt(left, 0));
                    }
                    if (StringUtils.isNumeric(top)) {
                        overlay.setTop(NumberUtils.toInt(top, 0));
                    }
                    if (StringTools.isValidString(align) && (align.equalsIgnoreCase(LEFT) || align.equalsIgnoreCase(CENTER) || align.equalsIgnoreCase(RIGHT))) {
                        overlay.setAlign(align);
                    }
                    if (StringTools.isValidString(valign) && (valign.equalsIgnoreCase(TOP) || valign.equalsIgnoreCase(CENTER) || valign.equalsIgnoreCase(BOTTOM))) {
                        overlay.setValign(valign);
                    }
                    if (StringTools.isValidString(width) && (width.equalsIgnoreCase(EQUAL) || width.equalsIgnoreCase(AUTO) || width.matches(D_PLUS))) {
                        overlay.setWidth(width);
                    }
                    if (StringTools.isValidString(height) && (height.equalsIgnoreCase(EQUAL) || height.equalsIgnoreCase(AUTO) || height.matches(D_PLUS))) {
                        overlay.setHeight(height);
                    }

                    List<HierarchicalConfiguration> images = c.configurationsAt("layer(" + index + ").images.image");
                    for (HierarchicalConfiguration image : images) {
                        name = image.getString("[@name]");
                        String value = image.getString("[@value]");
                        String filename = image.getString("[@filename]");

                        if (StringTools.isNotValidString(name)) {
                            name = overlay.getNames().get(0);
                        }
                        if (!overlay.getNames().contains(name) || StringTools.isNotValidString(value) || StringTools.isNotValidString(filename)) {
                            continue;
                        }

                        ImageOverlay img = new ImageOverlay(name, value, filename, Arrays.asList(value.split("/")));
                        if (img.getValues().size() > 1) {
                            for (int i = 0; i < overlay.getNames().size(); i++) {
                                if (img.getValues().size() <= i) {
                                    img.getValues().add(Movie.UNKNOWN);
                                } else if (StringTools.isNotValidString(img.getValues().get(i))) {
                                    img.getValues().set(i, Movie.UNKNOWN);
                                }
                            }
                        }
                        overlay.getImages().add(img);
                    }

                    if (overlay.getNames().size() > 1) {
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
                            ConditionOverlay condition = new ConditionOverlay();
                            condition.setValues(Arrays.asList(value.split("/")));
                            if (StringTools.isNotValidString(left)) {
                                left = Integer.toString(overlay.getLeft());
                            }
                            if (StringTools.isNotValidString(top)) {
                                top = Integer.toString(overlay.getTop());
                            }
                            if (StringTools.isNotValidString(align)) {
                                align = overlay.getAlign();
                            }
                            if (StringTools.isNotValidString(valign)) {
                                valign = overlay.getValign();
                            }
                            if (StringTools.isNotValidString(width)) {
                                width = overlay.getWidth();
                            }
                            if (StringTools.isNotValidString(height)) {
                                height = overlay.getHeight();
                            }
                            List<String> lefts = Arrays.asList(left.split("/"));
                            List<String> tops = Arrays.asList(top.split("/"));
                            List<String> aligns = Arrays.asList(align.split("/"));
                            List<String> valigns = Arrays.asList(valign.split("/"));
                            List<String> widths = Arrays.asList(width.split("/"));
                            List<String> heights = Arrays.asList(height.split("/"));
                            for (int i = 0; i < overlay.getNames().size(); i++) {
                                if (StringTools.isNotValidString(condition.getValues().get(i))) {
                                    condition.getValues().set(i, Movie.UNKNOWN);
                                }
                                PositionOverlay p = new PositionOverlay((lefts.size() <= i || StringTools.isNotValidString(lefts.get(i))) ? overlay.getLeft() : Integer.parseInt(lefts.get(i)),
                                        (tops.size() <= i || StringTools.isNotValidString(tops.get(i))) ? overlay.getTop() : Integer.parseInt(tops.get(i)),
                                        (aligns.size() <= i || StringTools.isNotValidString(aligns.get(i))) ? overlay.getAlign() : aligns.get(i),
                                        (valigns.size() <= i || StringTools.isNotValidString(valigns.get(i))) ? overlay.getValign() : valigns.get(i),
                                        (widths.size() <= i || StringTools.isNotValidString(widths.get(i))) ? overlay.getWidth() : widths.get(i),
                                        (heights.size() <= i || StringTools.isNotValidString(heights.get(i))) ? overlay.getHeight() : heights.get(i));
                                condition.getPositions().add(p);
                            }
                            overlay.getPositions().add(condition);
                        }
                    }
                    overlayLayers.add(overlay);
                    index++;
                }

                List<HierarchicalConfiguration> blocks = c.configurationsAt(BLOCK);
                overlayBlocks.clear();
                for (HierarchicalConfiguration block : blocks) {
                    String name = block.getString("name");
                    if (StringTools.isNotValidString(name)) {
                        continue;
                    }
                    String dir = block.getString("dir");
                    dir = StringTools.isNotValidString(dir) ? "horizontal" : dir;
                    String size = block.getString("size");
                    size = StringTools.isNotValidString(size) ? AUTO : size;
                    String cols = block.getString("cols");
                    cols = StringTools.isNotValidString(cols) ? AUTO : cols;
                    String rows = block.getString("rows");
                    rows = StringTools.isNotValidString(rows) ? AUTO : rows;
                    String hmargin = block.getString("hmargin");
                    hmargin = StringTools.isNotValidString(hmargin) ? "0" : hmargin;
                    String vmargin = block.getString("vmargin");
                    vmargin = StringTools.isNotValidString(vmargin) ? "0" : vmargin;
                    String clones = block.getString("clones");
                    overlayBlocks.put(name, new LogosBlock(dir.equalsIgnoreCase("horizontal"),
                            size.equalsIgnoreCase("static"),
                            cols, rows, hmargin, vmargin, StringTools.isNotValidString(clones) ? blockClones : (clones.equalsIgnoreCase(TRUE) ? true : (clones.equalsIgnoreCase(FALSE) ? false : blockClones))));
                }
            } catch (ConfigurationException ex) {
                LOG.error("Failed parsing moviejukebox overlay configuration file: " + xmlOverlayFile.getName());
                LOG.error(SystemTools.getStackTrace(ex));
            }
        } else {
            LOG.error("The moviejukebox overlay configuration file you specified is invalid: " + xmlOverlayFile.getAbsolutePath());
        }
    }

    protected boolean checkLogoEnabled(String name) {
        if (name.equalsIgnoreCase(LANGUAGE)) {
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
        } else if (name.equalsIgnoreCase(VIDEOSOURCE) || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
            return addVideoSource;
        } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
            return addVideoOut;
        } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
            return addVideoCodec;
        } else if (name.equalsIgnoreCase(AUDIOCODEC) || name.equalsIgnoreCase(ACODEC) || name.equalsIgnoreCase("AC")) {
            return addAudioCodec;
        } else if (name.equalsIgnoreCase(AUDIOCHANNELS) || name.equalsIgnoreCase(CHANNELS)) {
            return addAudioChannels;
        } else if (name.equalsIgnoreCase(AUDIOLANG) || name.equalsIgnoreCase(ALANG) || name.equalsIgnoreCase("AL")) {
            return addAudioLang;
        } else if (name.equalsIgnoreCase(CONTAINER)) {
            return addContainer;
        } else if (name.equalsIgnoreCase("aspect")) {
            return addAspectRatio;
        } else if (name.equalsIgnoreCase("fps")) {
            return addFPS;
        } else if (name.equalsIgnoreCase(CERTIFICATION)) {
            return addCertification;
        } else if (name.equalsIgnoreCase("watched")) {
            return addWatched;
        } else if (name.equalsIgnoreCase("episode")) {
            return addEpisode;
        } else if (name.equalsIgnoreCase("top250")) {
            return addTop250;
        } else if (name.equalsIgnoreCase(KEYWORDS)) {
            return addKeywords;
        } else if (name.equalsIgnoreCase(COUNTRY)) {
            return addCountry;
        } else if (name.equalsIgnoreCase(COMPANY)) {
            return addCompany;
        } else if (name.equalsIgnoreCase(AWARD)) {
            return addAward;
        }
        return !PropertiesUtil.getProperty(name, Movie.UNKNOWN).equals(Movie.UNKNOWN);
    }

    protected int getOverlayX(int fieldWidth, int itemWidth, Integer left, String align) {
        if (align.equalsIgnoreCase(LEFT)) {
            return (int) (left >= 0 ? left : fieldWidth + left);
        } else if (align.equalsIgnoreCase(RIGHT)) {
            return (int) (left >= 0 ? fieldWidth - left - itemWidth : -left - itemWidth);
        } else {
            return (int) (left == 0 ? ((fieldWidth - itemWidth) / 2) : left > 0 ? (fieldWidth / 2 + left) : (fieldWidth / 2 + left - itemWidth));
        }
    }

    protected int getOverlayY(int fieldHeight, int itemHeight, Integer top, String align) {
        if (align.equalsIgnoreCase(TOP)) {
            return (int) (top >= 0 ? top : fieldHeight + top);
        } else if (align.equalsIgnoreCase(BOTTOM)) {
            return (int) (top >= 0 ? fieldHeight - top - itemHeight : -top - itemHeight);
        } else {
            return (int) (top == 0 ? ((fieldHeight - itemHeight) / 2) : top > 0 ? (fieldHeight / 2 + top) : (fieldHeight / 2 + top - itemHeight));
        }
    }

    protected void fillOverlayKeywords(Map<String, ArrayList<String>> data, String keywordList) {
        data.clear();
        if (StringTools.isValidString(keywordList)) {
            for (String keyword : keywordList.split(" ; ")) {
                String[] keywordValues = keyword.split(Movie.SPACE_SLASH_SPACE);
                if (keywordValues.length > 1) {
                    ArrayList<String> arr = new ArrayList<String>(Arrays.asList(keywordValues));
                    data.put(keywordValues[0], arr);
                }
            }
        }
    }

    protected boolean cmpOverlayValue(final String name, final String condition, final String value) {
        boolean result = ((name.equalsIgnoreCase(KEYWORDS) && value.indexOf(condition.toLowerCase()) > -1)
                || condition.equalsIgnoreCase(value)
                || condition.equalsIgnoreCase(DEFAULT));
        if (!result) {
            Map<String, ArrayList<String>> data;
            if (name.equalsIgnoreCase("rating")) {
                data = keywordsRating;
            } else if (name.equalsIgnoreCase(VIDEOSOURCE) || name.equalsIgnoreCase("source") || name.equalsIgnoreCase("VS")) {
                data = keywordsVideoSource;
            } else if (name.equalsIgnoreCase("videoout") || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("VO")) {
                data = keywordsVideoOut;
            } else if (name.equalsIgnoreCase("videocodec") || name.equalsIgnoreCase("vcodec") || name.equalsIgnoreCase("VC")) {
                data = keywordsVideoCodec;
            } else if (name.equalsIgnoreCase(AUDIOCODEC) || name.equalsIgnoreCase(ACODEC) || name.equalsIgnoreCase("AC")) {
                data = keywordsAudioCodec;
            } else if (name.equalsIgnoreCase(AUDIOCHANNELS) || name.equalsIgnoreCase(CHANNELS)) {
                data = keywordsAudioChannels;
            } else if (name.equalsIgnoreCase(AUDIOLANG) || name.equalsIgnoreCase(ALANG) || name.equalsIgnoreCase("AL")) {
                data = keywordsAudioLang;
            } else if (name.equalsIgnoreCase(CONTAINER)) {
                data = keywordsContainer;
            } else if (name.equalsIgnoreCase("aspect")) {
                data = keywordsAspectRatio;
            } else if (name.equalsIgnoreCase("fps")) {
                data = keywordsFPS;
            } else if (name.equalsIgnoreCase(CERTIFICATION)) {
                data = keywordsCertification;
            } else if (name.equalsIgnoreCase(KEYWORDS)) {
                data = keywordsKeywords;
            } else if (name.equalsIgnoreCase(COUNTRY)) {
                data = keywordsCountry;
            } else if (name.equalsIgnoreCase(COMPANY)) {
                data = keywordsCompany;
            } else if (name.equalsIgnoreCase(AWARD)) {
                data = keywordsAward;
            } else {
                return false;
            }
            ArrayList<String> arr = data.get(condition);
            if (arr != null) {
                for (int loop = 0; loop < arr.size(); loop++) {
                    if (name.equalsIgnoreCase(KEYWORDS)) {
                        result = value.indexOf(arr.get(loop).toLowerCase()) > -1;
                    } else {
                        result = arr.get(loop).equalsIgnoreCase(value);
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
