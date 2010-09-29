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
package com.moviejukebox.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;

/**
 * Save a pre-defined list of attributes of the jukebox and properties 
 * for use in subsequent processing runs to determine if an attribute
 * has changed and force a rescan of the appropriate data 
 * @author stuart.boston
 *
 */
public class JukeboxProperties {
    private final static Logger logger = Logger.getLogger("moviejukebox");
    private final static Collection<PropertyInformation> propInfo = new ArrayList<PropertyInformation>();
    private final static String JUKEBOX = "jukebox";
    private final static String PROPERTIES = "properties";
    
    static {
        // Set up the properties to watch:                                xml           thumbnail     fanart        videoimage
        //                                                                       html          poster        banner
        propInfo.add(new PropertyInformation("userPropertiesName",        false, false, false, false, false, false, false));
        propInfo.add(new PropertyInformation("mjb.skin.dir",              false, true,  true,  true,  false, false, false));
        propInfo.add(new PropertyInformation("fanart.movie.download",     false, false, false, false, true,  false, false));
        propInfo.add(new PropertyInformation("fanart.tv.download",        false, false, false, false, true,  false, false));
        propInfo.add(new PropertyInformation("mjb.includeEpisodePlots",   true,  false, false, false, false, false, false));
        propInfo.add(new PropertyInformation("mjb.includeVideoImages",    true,  false, false, false, false, false, true));
        propInfo.add(new PropertyInformation("mjb.includeWideBanners",    false, false, false, false, false, true,  false));

        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerPage",   false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("mjb.nbThumbnailsPerLine",   false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerPage", false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("mjb.nbTvThumbnailsPerLine", false, true,  true,  false, false, false, false));

        propInfo.add(new PropertyInformation("thumbnails.width",          false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("thumbnails.height",         false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("thumbnails.logoHD",         false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("thumbnails.logoTV",         false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("thumbnails.logoSet",        false, true,  true,  false, false, false, false));
        propInfo.add(new PropertyInformation("thumbnails.language",       false, true,  true,  false, false, false, false));
        
        propInfo.add(new PropertyInformation("posters.width",             false, true,  false, true,  false, false, false));
        propInfo.add(new PropertyInformation("posters.height",            false, true,  false, true,  false, false, false));
        propInfo.add(new PropertyInformation("posters.logoHD",            false, true,  false, true,  false, false, false));
        propInfo.add(new PropertyInformation("posters.logoTV",            false, true,  false, true,  false, false, false));
        propInfo.add(new PropertyInformation("posters.language",          false, true,  false, true,  false, false, false));
    }
    
    /**
     * Create the mjbDetails file and populate with the attributes
     * @param mjbDetails
     * @param jukebox
     */
    public static void createFile(File mjbDetails, Jukebox jukebox) {
        Document docMjbDetails;
        Element eRoot, eJukebox, eProperties;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kk:mm:ss");
        
        try {
            logger.finest("Creating JukeboxProperties file: " + mjbDetails.getAbsolutePath());
            if (mjbDetails.exists() && !mjbDetails.delete()) {
                logger.severe("JukeboxProperties: Failed to delete " + mjbDetails.getName() + ". Please make sure it's not read only");
                return;
            }
        } catch (Exception error) {
            logger.severe("JukeboxProperties: Failed to create/delete " + mjbDetails.getName() + ". Please make sure it's not read only");
            return;
        }
        
        try {
            // Start with a blank document
            docMjbDetails = DOMHelper.createDocument();
            docMjbDetails.appendChild(docMjbDetails.createComment("This file was created on: " + dateFormat.format(System.currentTimeMillis())));
            
            //create the root element and add it to the document
            eRoot = docMjbDetails.createElement("root");
            docMjbDetails.appendChild(eRoot);
            
            //create child element, add an attribute, and add to root
            eJukebox = docMjbDetails.createElement(JUKEBOX);
            eRoot.appendChild(eJukebox);
            
            // Save the details directory name
            DOMHelper.appendChild(docMjbDetails, eJukebox, "DetailsDirName", jukebox.getDetailsDirName());
            
            // Save the jukebox location
            DOMHelper.appendChild(docMjbDetails, eJukebox, "JukeboxLocation", jukebox.getJukeboxRootLocation());

            eProperties = docMjbDetails.createElement(PROPERTIES);
            eRoot.appendChild(eProperties);
            
            Iterator<PropertyInformation> iterator = propInfo.iterator();
            while (iterator.hasNext()) {
                appendProperty(docMjbDetails, eProperties, iterator.next().getPropertyName());
            }
            
            DOMHelper.writeDocumentToFile(docMjbDetails, mjbDetails.getAbsolutePath());
        } catch (Exception error) {
            logger.severe("JukeboxProperties: Error creating " + mjbDetails.getName() + " file");
            error.printStackTrace();
        }
    }
    
    /**
     * Read the attributes from the file and compare and set any force overwrites needed
     * @param mjbDetails
     * @return PropertyInformation Containing the merged overwrite values
     */
    public static PropertyInformation readFile(File mjbDetails) {
        PropertyInformation piReturn = new PropertyInformation("RETURN", false, false, false, false, false, false, false);
        Document docMjbDetails;
        // Try to open and read the document file
        try {
            docMjbDetails = DOMHelper.getEventDocFromUrl(mjbDetails);
        } catch (Exception error) {
            logger.severe("JukeboxProperties: Failed creating the file, no checks performed");
            logger.severe(error.getMessage());
            error.getStackTrace();
            return piReturn;
        }
        
        NodeList nlElements;
        Node nDetails;
        
        /* 
         * Do we care about these properties?
        nlElements = docMjbDetails.getElementsByTagName(JUKEBOX);
        nDetails = nlElements.item(0);
        
        if (nDetails == null) {
            logger.warning("JukeboxProperties: Error reading file. Deleting and re-creating the file");
            createFile(mjbDetails, MovieJukebox.getJukebox());
            return piReturn;
        }

        if (nDetails.getNodeType() == Node.ELEMENT_NODE) {
            Element eJukebox = (Element) nDetails;
            logger.fine("DetailsDirName : " + DOMHelper.getValueFromElement(eJukebox, "DetailsDirName"));
            logger.fine("JukeboxLocation: " + DOMHelper.getValueFromElement(eJukebox, "JukeboxLocation"));
        }
        */
        
        nlElements = docMjbDetails.getElementsByTagName(PROPERTIES);
        nDetails = nlElements.item(0);
        
        if (nDetails == null) {
            logger.warning("JukeboxProperties: Error reading file. Deleting and re-creating the file");
            createFile(mjbDetails, MovieJukebox.getJukebox());
            return piReturn;
        }
        
        if (nDetails.getNodeType() == Node.ELEMENT_NODE) {
            Element eJukebox = (Element) nDetails;
            String propName, propValue, propCurrent;
            boolean propTest = false;
            
            Iterator<PropertyInformation> iterator = propInfo.iterator();
            while (iterator.hasNext()) {
                PropertyInformation pi = iterator.next();
                propName = pi.getPropertyName();
                propValue = DOMHelper.getValueFromElement(eJukebox, propName);
                propCurrent = PropertiesUtil.getProperty(propName);
                
                propTest = propValue.equalsIgnoreCase(propCurrent);
                if (!propTest) {
                    // Update the return value with the information from this property
                    piReturn.mergePropertyInformation(pi);
                }
            }
        }
        
        logger.finest("JukeboxProperties: Returning: " + piReturn.toString());
        return piReturn;
    }
    
    /**
     * Helper function to write out the property to the DOM document & Element
     * @param doc
     * @param element
     * @param propertyName
     */
    private static void appendProperty(Document doc, Element element, String propertyName) {
        DOMHelper.appendChild(doc, element, propertyName, PropertiesUtil.getProperty(propertyName));
    }

    /**
     * Class to define the property name and the impact on each of the overwrite flags.
     * If the 
     * @author stuart.boston
     *
     */
    public static class PropertyInformation {
        private String  propertyName        = Movie.UNKNOWN;
        private boolean xmlOverwrite        = false;
        private boolean htmlOverwrite       = false;
        private boolean thumbnailOverwrite  = false;
        private boolean posterOverwrite     = false;
        private boolean fanartOverwrite     = false;
        private boolean bannerOverwrite     = false;
        private boolean videoimageOverwrite = false;
        
        public PropertyInformation(String property,
                                    boolean xml,
                                    boolean html,
                                    boolean thumbnail, 
                                    boolean poster,
                                    boolean fanart,
                                    boolean banner,
                                    boolean videoimage) {
            this.propertyName        = property;
            this.xmlOverwrite        = xml;
            this.htmlOverwrite       = html;
            this.thumbnailOverwrite  = thumbnail;
            this.posterOverwrite     = poster;
            this.fanartOverwrite     = fanart;
            this.bannerOverwrite     = banner;
            this.videoimageOverwrite = videoimage;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public boolean isXmlOverwrite() {
            return xmlOverwrite;
        }

        public boolean isHtmlOverwrite() {
            return htmlOverwrite;
        }

        public boolean isThumbnailOverwrite() {
            return thumbnailOverwrite;
        }

        public boolean isPosterOverwrite() {
            return posterOverwrite;
        }

        public boolean isFanartOverwrite() {
            return fanartOverwrite;
        }

        public boolean isBannerOverwrite() {
            return bannerOverwrite;
        }

        public boolean isVideoimageOverwrite() {
            return videoimageOverwrite;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public void setXmlOverwrite(boolean xmlOverwrite) {
            this.xmlOverwrite = xmlOverwrite;
        }

        public void setHtmlOverwrite(boolean htmlOverwrite) {
            this.htmlOverwrite = htmlOverwrite;
        }

        public void setThumbnailOverwrite(boolean thumbnailOverwrite) {
            this.thumbnailOverwrite = thumbnailOverwrite;
        }

        public void setPosterOverwrite(boolean posterOverwrite) {
            this.posterOverwrite = posterOverwrite;
        }

        public void setFanartOverwrite(boolean fanartOverwrite) {
            this.fanartOverwrite = fanartOverwrite;
        }

        public void setBannerOverwrite(boolean bannerOverwrite) {
            this.bannerOverwrite = bannerOverwrite;
        }

        public void setVideoimagesOverwrite(boolean videoimageOverwrite) {
            this.videoimageOverwrite = videoimageOverwrite;
        }
        
        /**
         * Merge two PropertyInformation objects. Sets the overwrite flags to true.
         * @param newPI
         */
        public void mergePropertyInformation(PropertyInformation newPI) {
            this.xmlOverwrite        = xmlOverwrite        || newPI.isXmlOverwrite();
            this.htmlOverwrite       = htmlOverwrite       || newPI.isHtmlOverwrite();
            this.thumbnailOverwrite  = thumbnailOverwrite  || newPI.isThumbnailOverwrite();
            this.posterOverwrite     = posterOverwrite     || newPI.isPosterOverwrite();
            this.fanartOverwrite     = fanartOverwrite     || newPI.isFanartOverwrite();
            this.bannerOverwrite     = bannerOverwrite     || newPI.isBannerOverwrite();
            this.videoimageOverwrite = videoimageOverwrite || newPI.isVideoimageOverwrite();
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Name: ");
            sb.append(getPropertyName());
            sb.append(", xmlOverwrite: ");
            sb.append(isXmlOverwrite());
            sb.append(", htmlOverwrite: ");
            sb.append(isHtmlOverwrite());
            sb.append(", thumbnailOverwrite: ");
            sb.append(isThumbnailOverwrite());
            sb.append(", posterOverwrite: ");
            sb.append(isPosterOverwrite());
            sb.append(", fanartOverwrite: ");
            sb.append(isFanartOverwrite());
            sb.append(", bannerOverwrite: ");
            sb.append(isBannerOverwrite());
            sb.append(", videoimageOverwrite: ");
            sb.append(isVideoimageOverwrite());
            return sb.toString();
        }
    }
}
