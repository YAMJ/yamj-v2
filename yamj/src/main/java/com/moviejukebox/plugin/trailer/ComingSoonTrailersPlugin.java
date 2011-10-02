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
package com.moviejukebox.plugin.trailer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

import com.moviejukebox.plugin.ComingSoonPlugin;

/**
 * base on ComingSoonPlugin
 * @author iuk
 *
 */

public class ComingSoonTrailersPlugin extends TrailersPlugin {

    public static String COMINGSOON_PLUGIN_ID = "comingsoon";
    private static String COMINGSOON_BASE_URL = "http://www.comingsoon.it/";
    private static String COMINGSOON_SEARCH_URL = "Film/Scheda/Trama/?";
    private static String COMINGSOON_VIDEO_URL = "Film/Scheda/Video/?";
    private static String COMINGSOON_KEY_PARAM= "key=";
    private ComingSoonPlugin csPlugin = new ComingSoonPlugin();

    protected String trailerMaxResolution;
    protected String trailerPreferredFormat;
    protected boolean trailerSetExchange;
    protected boolean trailerDownload;
    protected String trailerLabel;

    public ComingSoonTrailersPlugin() {
        super();
        trailersPluginName = "ComingSoonTrailers";

        trailerMaxResolution = PropertiesUtil.getProperty("comingsoon.trailer.resolution", "");
        trailerPreferredFormat = PropertiesUtil.getProperty("comingsoon.trailer.preferredFormat", "wmv,mov");
        trailerSetExchange = PropertiesUtil.getBooleanProperty("comingsoon.trailer.setExchange", "false");
        trailerDownload = PropertiesUtil.getBooleanProperty("comingsoon.trailer.download", "false");
        trailerLabel = PropertiesUtil.getProperty("comingsoon.trailer.label", "ita");
    }
    
    @Override
    public final boolean generate(Movie movie) {
        if (trailerMaxResolution.length() == 0) {
            return false;
        }
        if (movie.isExtra() || movie.isTVShow()) {
            return false;
        }
        
        if (movie.isTrailerExchange()) {
            logger.debug(trailersPluginName + " Plugin: Movie has previously been checked for trailers, skipping");
            return false;
        }
        
        if (!movie.getExtraFiles().isEmpty()) {
            logger.debug(trailersPluginName + " Plugin: Movie has trailers, skipping");
            return false;
        }
        
        String trailerUrl = getTrailerUrl(movie);
        
        if (StringTools.isNotValidString(trailerUrl)) {
            logger.debug(trailersPluginName + " Plugin: no trailer found");
            if (trailerSetExchange) {
                movie.setTrailerExchange(true);
            }
            return false;
        }
        
        logger.debug(trailersPluginName + " Plugin: found trailer at URL " + trailerUrl);
        
        MovieFile tmf = new MovieFile();
        tmf.setTitle("TRAILER-" + trailerLabel);
        
        boolean isExchangeOk = false;
        
        if (trailerDownload) {
            isExchangeOk = downloadTrailer(movie, trailerUrl, trailerLabel, tmf);
        } else {
            tmf.setFilename(trailerUrl);
            movie.addExtraFile(new ExtraFile(tmf));
            movie.setTrailerExchange(true);
            isExchangeOk = true;
        }
        
        return isExchangeOk;
    }
    
    @Override
    public String getName() {
        return "comingsoon";
    }
    
    protected String getTrailerUrl(Movie movie) {
        String trailerUrl = Movie.UNKNOWN;
        String comingSoonId = movie.getId(COMINGSOON_PLUGIN_ID);
        if (StringTools.isNotValidString(comingSoonId)) {
            comingSoonId = csPlugin.getComingSoonId(movie.getOriginalTitle(), movie.getYear());
            if (StringTools.isNotValidString(comingSoonId)) {
                return Movie.UNKNOWN;
            }
        }
        
        try {
            String searchUrl = COMINGSOON_BASE_URL + COMINGSOON_VIDEO_URL + COMINGSOON_KEY_PARAM + comingSoonId;
            logger.debug(trailersPluginName + " Plugin: searching for trailer at URL " + searchUrl);
            String xml = webBrowser.request(searchUrl);
            if (xml.indexOf(COMINGSOON_SEARCH_URL + COMINGSOON_KEY_PARAM + comingSoonId) < 0) {
                // No link to movie page found. We have been redirected to the general video page
                logger.debug(trailersPluginName + " Plugin: no video found for movie " + movie.getTitle());
                return Movie.UNKNOWN;
            }
            
            String xmlUrl = Movie.UNKNOWN;
            
            if (trailerPreferredFormat.indexOf(",") < 0) {
                xmlUrl = parseVideoPage(xml, trailerPreferredFormat); 
            } else {
                StringTokenizer st = new StringTokenizer(trailerPreferredFormat, ",");
                while (st.hasMoreTokens()) {
                    xmlUrl = parseVideoPage(xml, st.nextToken());
                    if (StringTools.isValidString(xmlUrl)) {
                        break;
                    }
                }
            }
            
            if (StringTools.isNotValidString(xmlUrl)) {
                logger.debug("No downloadable trailer found for movie: " + movie.getTitle());
                return Movie.UNKNOWN;
            }
            
            String trailerXml = webBrowser.request(xmlUrl);
            int beginUrl = trailerXml.indexOf("http://");
            if (beginUrl >= 0) {
                trailerUrl = new String(trailerXml.substring(beginUrl, trailerXml.indexOf("\"", beginUrl)));
            } else {
                logger.error(trailersPluginName + " Plugin: cannot find trailer URL in XML. Layout changed?");
            }
            

        } catch (Exception error) {
            logger.error(trailersPluginName + " Plugin: Failed retreiving trailer for movie: " + movie.getTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return Movie.UNKNOWN;
        }
        
        return trailerUrl;
    }
    
    private String parseVideoPage(String xml, String format) {
        String trailerUrl = Movie.UNKNOWN;
        
        int indexOfTrailer = -1;
        String extension;
        
        if (format.equalsIgnoreCase("mov")) {
            extension = "qtl";
        } else if (format.equalsIgnoreCase("wmv")) {
            extension = "wvx";
        } else {
            logger.info(trailersPluginName + " Plugin: unknown trailer format " + format);
            return Movie.UNKNOWN;
        }

        if (trailerMaxResolution.equalsIgnoreCase("1080p")) {
            indexOfTrailer = xml.indexOf("1080P." + extension);
        }

        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase("720p") || trailerMaxResolution.equalsIgnoreCase("1080p"))) {
            indexOfTrailer = xml.indexOf("720P." + extension);
        }
        
        if (indexOfTrailer < 0 && (trailerMaxResolution.equalsIgnoreCase("480p") || trailerMaxResolution.equalsIgnoreCase("720p") || trailerMaxResolution.equalsIgnoreCase("1080p"))) {
            indexOfTrailer = xml.indexOf("480P." + extension);
        }
        
        if (indexOfTrailer >= 0 ) {
            int beginUrl = new String(xml.substring(0, indexOfTrailer)).lastIndexOf("http://");
            trailerUrl = new String(xml.substring(beginUrl, xml.indexOf("\"", beginUrl)));
            logger.debug(trailersPluginName + " Plugin: found trailer XML URL " + trailerUrl);
        }
        
        return trailerUrl;
    }
}
