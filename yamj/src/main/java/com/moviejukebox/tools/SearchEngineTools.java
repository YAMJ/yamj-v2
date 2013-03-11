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
package com.moviejukebox.tools;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.moviejukebox.model.Movie;

public class SearchEngineTools {

    private static final Logger LOGGER = Logger.getLogger(SearchEngineTools.class);
    private static final String LOG_MESSAGE = "SearchEngingTools: ";
    
    private WebBrowser webBrowser;
    private LinkedList<String> searchSites;
    private String searchSuffix = "";
    private String googleHost;
    private String googleCountry;
    private String yahooHost;
    private String bingHost;
    private String blekkoHost;
    private String lycosHost;
    private String webcrawlerHost;

    public SearchEngineTools() {
        webBrowser = new WebBrowser();
        // user agent should be an actual FireFox
        webBrowser.addBrowserProperty("User-Agent", "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");
        
        // sites to search for URLs
        searchSites = new LinkedList<String>();
        searchSites.addAll(Arrays.asList(PropertiesUtil.getProperty("searchengine.sites", "google,yahoo,bing,blekko,lycos,webcrawler").split(",")));
        
        // search domains
        googleHost = PropertiesUtil.getProperty("searchengine.google.host", "www.google.com");
        yahooHost = PropertiesUtil.getProperty("searchengine.yahoo.host", "search.yahoo.com");
        bingHost = PropertiesUtil.getProperty("searchengine.bing.host", "www.bing.com");
        blekkoHost = PropertiesUtil.getProperty("searchengine.blekko.host", "www.blekko.com");
        lycosHost = PropertiesUtil.getProperty("searchengine.lycos.host", "search.lycos.com");
        webcrawlerHost = PropertiesUtil.getProperty("searchengine.webcrawler.host", "www.webcrawler.com");
    }

    public void setSearchSites(String searchSites) {
        this.searchSites.clear();
        this.searchSites.addAll(Arrays.asList(searchSites.split(",")));
    }

    public void setSearchSuffix(String searchSuffix) {
        this.searchSuffix = searchSuffix;
    }

    public void setGoogleHost(String googleHost) {
        this.googleHost = googleHost;
    }
    
    public void setGoogleCountry(String googleCountry) {
        this.googleCountry = googleCountry;
    }

    public void setYahooHost(String yahooHost) {
        this.yahooHost = yahooHost;
    }

    public void setBingHost(String bingHost) {
        this.bingHost = bingHost;
    }

    public void setBlekkoHost(String blekkoHost) {
        this.blekkoHost = blekkoHost;
    }

    public void setLycosHost(String lycosHost) {
        this.lycosHost = lycosHost;
    }

    public void setWebcrawlerHost(String webcrawlerHost) {
        this.webcrawlerHost = webcrawlerHost;
    }
    
    public String searchMovieURL(String title, String year, String site) {
        String url = null;;
        for (int i=0; i<searchSites.size(); i++) {
            String engine = getNextSearchEngine();
            
            if ("yahoo".equalsIgnoreCase(engine)) {
                url = searchUrlOnYahoo(title, year, site);
            } else if ("bing".equalsIgnoreCase(engine)) {
                url = searchUrlOnBing(title, year, site);
            } else if ("blekko".equalsIgnoreCase(engine)) {
                url = searchUrlOnBlekko(title, year, site);
            } else if ("lycos".equalsIgnoreCase(engine)) {
                url = searchUrlOnLycos(title, year, site);
            } else if ("webcrawler".equalsIgnoreCase(engine)) {
                url = searchUrlOnWebcrawler(title, year, site);
            } else {
                url = searchUrlOnGoogle(title, year, site);
            }
            
            if (StringTools.isValidString(url)) {
                break;
            }
        }
        
        return url;
    }

    private String getNextSearchEngine() {
        String engine = searchSites.remove();
        searchSites.addLast(engine);
        return engine;
    }
    
    public int countSearchSites() {
        return searchSites.size();
    }
    
    public String searchUrlOnGoogle(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on google; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(googleHost);
            sb.append("/search?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (googleCountry != null) {
                sb.append("&hl=");
                sb.append(googleCountry);
            }
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by google search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnYahoo(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on yahoo; site="+site);

        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(yahooHost);
            sb.append("/search?vc=&p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);

            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("//" + site + searchSuffix);
            if (beginIndex != -1) {
                String link = xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
                if (StringTools.isValidString(link)) {
                    return "http:"+link;
                }
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by yahoo search: " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnBing(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on bing; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(bingHost);
            sb.append("/search?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by bing search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnBlekko(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on blekko; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(blekkoHost);
            sb.append("/ws/?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by bing search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }    

    public String searchUrlOnLycos(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on lycos; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(lycosHost);
            sb.append("/web/?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by lycos search: " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }    

    public String searchUrlOnWebcrawler(String title, String year, String site) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on webcrawler; site="+site);

        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(webcrawlerHost);
            sb.append("/search/web?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);

            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("<div class=\"resultDisplayUrl\"");
            if (beginIndex != -1) {
                String link = xml.substring(beginIndex, xml.indexOf("</div>", beginIndex));
                return "http://" + HTMLTools.removeHtmlTags(link);
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by webcrawler search: " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }
}
