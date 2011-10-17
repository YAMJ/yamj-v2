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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * @author iuk
 *
 */

public class TrailersLandPlugin extends TrailersPlugin {

    private static String TRAILERSLAND_BASE_URL = "http://www.trailersland.com/";
    private static String TRAILERSLAND_SEARCH_URL = "cerca/ricerca=";
    private static String TRAILERSLAND_MOVIE_URL = "film/";
    private static String TRAILERSLAND_TRAILER_URL = "trailer/";
    private static String TRAILERSLAND_TRAILER_FILE_URL = "wrapping/tls.php?";
    
    protected int trailerMaxCount;
    protected String trailerMaxResolution;
    protected String trailerAllowedFormats;
    protected String trailerPreferredLanguages;
    protected String trailerPreferredTypes;

    public TrailersLandPlugin() {
        super();
        trailersPluginName = "TrailersLand";
      
        trailerMaxCount = (int) PropertiesUtil.getLongProperty("trailersland.max", "3");
        trailerMaxResolution = PropertiesUtil.getProperty("trailersland.maxResolution", "1080p");
        trailerAllowedFormats = PropertiesUtil.getProperty("trailersland.allowedFormats", "wmv,mov,mp4,avi,mkv,mpg");
        trailerPreferredLanguages = PropertiesUtil.getProperty("trailersland.preferredLanguages", "ita,sub-ita,en");
        trailerPreferredTypes = PropertiesUtil.getProperty("trailersland.preferredTypes", "trailer,teaser");
       
    }
    
    @Override
    public final boolean generate(Movie movie) {
        if (trailerMaxResolution.length() == 0) {
            return false;
        }
        
        movie.setTrailerLastScan(new Date().getTime()); // Set the last scan to now
        
        ArrayList <TrailersLandTrailer> trailerList = getTrailerUrls(movie);
        
        if (trailerList == null) {
            logger.error(trailersPluginName + " Plugin: error while scraping");
            return false;
        } else if (trailerList.isEmpty()) {
            logger.debug(trailersPluginName + " Plugin: no trailer found");
            return false;
        }
        
        for (int i = trailerList.size() - 1; i >= 0; i--) {
            TrailersLandTrailer tr = trailerList.get(i);

            String trailerUrl = tr.getUrl();
            logger.info(trailersPluginName + " Plugin: found trailer at URL " + trailerUrl);

            String trailerLabel = Integer.toString(trailerList.size() - i) + "-" + tr.getLang() + "-" + tr.getType();
            MovieFile tmf = new MovieFile();
            tmf.setTitle("TRAILER-" + trailerLabel);
            
            if (getDownload()) {
                if (!downloadTrailer(movie, trailerUrl, trailerLabel, tmf)) return false;
            } else {
                tmf.setFilename(trailerUrl);
                movie.addExtraFile(new ExtraFile(tmf));
            }
        }
        
        return true;
    }
    
    @Override
    public String getName() {
        return "trailersland";
    }
    
    protected String getTrailersLandIdFromTitle(String title) {
        String trailersLandId = Movie.UNKNOWN;
        String searchUrl;

        try {
            searchUrl = TRAILERSLAND_BASE_URL + TRAILERSLAND_SEARCH_URL + URLEncoder.encode(title, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            logger.error(trailersPluginName + " Plugin: Unsupported encoding, cannot build search URL");
            return Movie.UNKNOWN;
        } 

        logger.debug(trailersPluginName + " Plugin: searching for movie at URL " + searchUrl);
        
        String xml;
        try {
            xml = webBrowser.request(searchUrl);
        } catch (IOException e) {
            logger.error(trailersPluginName + " Plugin: failed retreiving TrailersLand Id for movie : " + title);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return Movie.UNKNOWN;
        }
        
        
        int indexRes = xml.indexOf("<div id=\"film_informazioni_ricerca\"");
        if (indexRes >= 0) {
            int indexMovieUrl = xml.indexOf(TRAILERSLAND_BASE_URL + TRAILERSLAND_MOVIE_URL, indexRes + 1);
            if (indexMovieUrl >= 0) {
                int endMovieUrl = xml.indexOf('"', indexMovieUrl + 1);
                if (endMovieUrl >= 0) {
                    trailersLandId = xml.substring(indexMovieUrl + TRAILERSLAND_BASE_URL.length() + TRAILERSLAND_MOVIE_URL.length(), endMovieUrl);
                    logger.debug(trailersPluginName + " Plugin: found Trailers Land Id " + trailersLandId);
                }
            } else {
                logger.error(trailersPluginName + " Plugin: got search result but no movie. Layout has changed?");
            }
        } else {
            logger.debug(trailersPluginName + " Plugin: no movie found.");
        }
        return trailersLandId;
        
    }
    
    protected String getTrailersLandId(Movie movie) {
        String title = movie.getTitle();
        String origTitle = movie.getOriginalTitle();
        String trailersLandId = getTrailersLandIdFromTitle(title);
        
        if (StringTools.isNotValidString(trailersLandId) && StringTools.isValidString(origTitle) && !title.equalsIgnoreCase(origTitle)) {
            trailersLandId = getTrailersLandIdFromTitle(origTitle);
        }
        
        return trailersLandId;
    }
    
    protected ArrayList <TrailersLandTrailer> getTrailerUrls(Movie movie) {

        
        
        String trailersLandId = movie.getId(getName());
        if (StringTools.isNotValidString(trailersLandId)) {
            trailersLandId = getTrailersLandId(movie);
            if (StringTools.isNotValidString(trailersLandId)) {
                return null;
            } else {
                movie.setId(getName(), trailersLandId);
            }
        }
        
        String xml;
        try {
            xml = webBrowser.request(TRAILERSLAND_BASE_URL + TRAILERSLAND_MOVIE_URL + trailersLandId);
        } catch (IOException e) {
            logger.error(trailersPluginName + " Plugin: failed retreiving movie details for movie : " + movie.getTitle());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            e.printStackTrace(printWriter);
            logger.error(eResult.toString());
            return null;
        }
        
        ArrayList <TrailersLandTrailer> trailerList = new ArrayList<TrailersLandTrailer>();
        
        int indexVideo = xml.indexOf("<div id=\"table_video\"");
        int indexEndVideo = xml.indexOf("</table", indexVideo + 1);
        
        if (indexVideo >= 0 && indexVideo < indexEndVideo) {
            int nextIndex = xml.indexOf(TRAILERSLAND_BASE_URL + TRAILERSLAND_TRAILER_URL, indexVideo);
            while (nextIndex >= 0 && nextIndex < indexEndVideo) {
                int endIndex = xml.indexOf('"', nextIndex + 1);
                String trailerPageUrl = xml.substring(nextIndex, endIndex);
                
                TrailersLandTrailer tr = new TrailersLandTrailer(trailerPageUrl);                                
                tr.parseName();
                tr.setFoundOrder(nextIndex);
                
                if (tr.validateLang() && tr.validateType()) {
                    logger.debug(trailersPluginName + " Plugin: found trailer page URL " + trailerPageUrl);
                    trailerList.add(tr);
                //} else {
                //    logger.debug(trailersPluginName + " Plugin: discarding page URL " + trailerPageUrl);
                }
                
                nextIndex = xml.indexOf(TRAILERSLAND_BASE_URL + TRAILERSLAND_TRAILER_URL, endIndex + 1);
            }
        } else {
            logger.error(trailersPluginName + " Plugin: video section not found. Layout changed?");
        }
        
        
        Collections.sort(trailerList);
                
        int remaining = trailerMaxCount;
        
        
        for (int i = trailerList.size() - 1; i >= 0; i--) {
            
            if (remaining == 0) {
               trailerList.remove(i);
            } else {
                
                TrailersLandTrailer tr = trailerList.get(i);
            
                String trailerXml;
                String trailerPageUrl = tr.getPageUrl();
                
                //logger.debug(trailersPluginName + " Plugin: evaluating page " +  trailerPageUrl);                
                
                try {
                    trailerXml = webBrowser.request(trailerPageUrl);
                } catch (IOException e) {
                    logger.error(trailersPluginName + " Plugin: failed retreiving trailer details for movie : " + movie.getTitle());
                    final Writer eResult = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(eResult);
                    e.printStackTrace(printWriter);
                    logger.error(eResult.toString());
                    return null;
                }
                
                int nextIndex = trailerXml.indexOf(TRAILERSLAND_BASE_URL + TRAILERSLAND_TRAILER_FILE_URL);
    
                if (nextIndex < 0) {
                    logger.error(trailersPluginName + " Plugin: no downloadable files found. Layout changed?");
                    trailerList.remove(i);
                } else {
                    boolean found = false;
                    while (nextIndex >= 0) {
                        
                        int endIndex = trailerXml.indexOf('"', nextIndex);
                        String url = trailerXml.substring(nextIndex, endIndex);
                        
                        //logger.debug(trailersPluginName + " Plugin: evaluating url " +  url);
                        
                        if (tr.candidateUrl(url)) {
                            if (!found) {
                                found = true;
                                remaining--;
                            }
                            //logger.debug(trailersPluginName + " Plugin: current best url is " + url);
                        }
                        
                        nextIndex = trailerXml.indexOf(TRAILERSLAND_BASE_URL + TRAILERSLAND_TRAILER_FILE_URL, endIndex + 1);
                    }                    
                    if (! found) {
                        trailerList.remove(i);
                        logger.debug(trailersPluginName + " Plugin: no valid url found at trailer page " +  trailerPageUrl);
                    }
                }
            }            
        }
        
        
        return trailerList;
  
    }
    

    
    
    
    class TrailersLandTrailer implements Comparable<TrailersLandTrailer> {

        String pageUrl;
        String url;
        String res;
        String type;
        String lang;
        int foundOrder = 0;
        
        public TrailersLandTrailer (String pageUrl) {
            this.setPageUrl(pageUrl);
            this.setLang(Movie.UNKNOWN);
            this.setRes(Movie.UNKNOWN);
            this.setType(Movie.UNKNOWN);
            this.setUrl(Movie.UNKNOWN);
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }

        
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getRes() {
            return res;
        }

        public void setRes(String res) {
            this.res = res;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public int getFoundOrder() {
            return foundOrder;
        }

        public void setFoundOrder(int foundOrder) {
            this.foundOrder = foundOrder;
        }

        public void parseName() {
            String trailerPageUrl = getPageUrl();
            int nameIndex = TRAILERSLAND_BASE_URL.length() + TRAILERSLAND_TRAILER_URL.length();
            
            if (trailerPageUrl.indexOf("teaser", nameIndex) >= 0 || trailerPageUrl.indexOf("tesaer", nameIndex) >= 0) { // Some typo are present...
                setType("teaser");
            } else if (trailerPageUrl.indexOf("trailer", nameIndex) >= 0) {
                setType("trailer");
            }
            
            if (trailerPageUrl.indexOf("sottotitolato", nameIndex) >= 0) {
                setLang("sub-ita");
            } else if (trailerPageUrl.indexOf("italiano", nameIndex) >= 0) {
                setLang("ita");
            } else if (trailerPageUrl.indexOf("francese", nameIndex) >= 0) {
                setLang("fr");
            } else {
                setLang("en");
            }
        }
        
        private boolean isResValid(String res) {
            
            if (res.equals("sd")) return true;
            if (res.equals("720p") && (trailerMaxResolution.equals("1080p") || trailerMaxResolution.equals("720p"))) return true;
            if (res.equals("1080p") && trailerMaxResolution.equals("1080p")) return true;
            
            return false;
            
        }
        
        private boolean isResBetter(String res) {
            
            String thisRes = getRes();
            if (StringTools.isNotValidString(res)) return false;
            if (StringTools.isNotValidString(getRes())) return true;
            
            if (thisRes.equals("1080p")) return false;
            if (thisRes.equals("720p") && res.equals("1080p")) return true;
            if (thisRes.equals("sd") && (res.equals("1080p") || res.equals("720p"))) return true;
            
            return false;
        }
        
        public boolean candidateUrl(String url) {

            int startIndex = url.indexOf("url=");
            
            if (startIndex >= 0) {
            
                String fileUrl = url.substring(startIndex + 4);

                String ext = fileUrl.substring(fileUrl.lastIndexOf('.') + 1);
                if (this.evaluateAgainstList(ext, trailerAllowedFormats) < 0) {
                    //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " due to invalid extension.");
                    return false;
                }
                
                String params = url.substring(0, startIndex - 1);
                
                String res;
                if (params.indexOf("sd_file") >= 0) {
                    res = "sd";
                } else if (params.indexOf("480") >= 0) {
                    res = "sd";                        
                } else if (params.indexOf("720") >= 0) {
                    res = "720p";
                } else if (params.indexOf("1080") >= 0) {
                    res = "1080p";
                } else {
                    logger.error(trailersPluginName + " Plugin: cannot guess trailer resolution for params " + params + ". Layout changed?");
                    return false;
                }
                
                if (! isResValid(res)) {
                    //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " due to resolution.");
                    return false;
                } else {
                    if (! this.isResBetter(res)) {
                        //logger.debug(trailersPluginName + " Plugin: discarding " + fileUrl + " as it's not better than actual resolution.");
                        return false;
                    }
                }
                
                setUrl(url);
                setRes(res);
                
                return true;
            } else {
                logger.error(trailersPluginName + " Plugin: couldn't find trailer url. Layout changed?");
                return false;
            }
        }
            
        
        private int evaluateAgainstList(String what, String list) {
            if (list.indexOf(',') < 0) {
                return what.equalsIgnoreCase(list) ? 1 : -1;
            } else {
                StringTokenizer st = new StringTokenizer(list, ",");
                int w = 1;
                while (st.hasMoreTokens()) {
                    if (what.equalsIgnoreCase(st.nextToken())) {
                        return w;
                    }
                    w++;
                }
                return -1;
            }
        }
        
        public boolean validateLang() {
            return evaluateAgainstList(getLang(), trailerPreferredLanguages) > 0;
        }
        
        public boolean validateType() {
            return evaluateAgainstList(getType(), trailerPreferredTypes) > 0;
        }

        @Override
        public int compareTo(TrailersLandTrailer o) {
            String thisLang = this.getLang();
            String thisType = this.getType();
            String thatLang = o.getLang();
            String thatType = o.getType();
            int thisFoundOrder = this.getFoundOrder();
            int thatFoundOder = o.getFoundOrder();
            
            int diff = evaluateAgainstList(thatLang, trailerPreferredLanguages) - evaluateAgainstList(thisLang, trailerPreferredLanguages);
            
            if (diff == 0) {
                diff = evaluateAgainstList(thatType, trailerPreferredTypes) - evaluateAgainstList(thisType, trailerPreferredTypes);
                if (diff == 0) {
                    diff = thatFoundOder - thisFoundOrder;
                }
            }
            return diff;

        }
        
    }
}
