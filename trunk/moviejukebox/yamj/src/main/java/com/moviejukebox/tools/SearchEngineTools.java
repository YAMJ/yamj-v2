/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import com.moviejukebox.model.Movie;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SearchEngineTools {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineTools.class);
    private static final String LOG_MESSAGE = "SearchEngingTools: ";
    private WebBrowser webBrowser;
    private LinkedList<String> searchSites;
    private String searchSuffix = "";
    private final String country;
    private String language = "";
    private String googleHost = "www.google.com";
    private String yahooHost = "search.yahoo.com";
    private String bingHost = "www.bing.com";

    public SearchEngineTools() {
        this("us");
    }

    public SearchEngineTools(String country) {
        webBrowser = new WebBrowser();
        // user agent should be an actual FireFox
        webBrowser.addBrowserProperty("User-Agent", "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");

        // sites to search for URLs
        searchSites = new LinkedList<String>();
        searchSites.addAll(Arrays.asList(PropertiesUtil.getProperty("searchengine.sites", "google,yahoo,bing").split(",")));

        // country specific presets
        if ("us".equalsIgnoreCase(country)) {
            this.country = "us";
            // Leave the rest as defaults
        } else if ("de".equalsIgnoreCase(country)) {
            this.country = "de";
            language = "de";
            googleHost = "www.google.de";
            yahooHost = "de.search.yahoo.com";
        } else if ("it".equalsIgnoreCase(country)) {
            this.country = "it";
            language = "it";
            googleHost = "www.google.it";
            yahooHost = "it.search.yahoo.com";
            bingHost = "it.bing.com";
        } else if ("se".equalsIgnoreCase(country)) {
            this.country = "se";
            language = "sv";
            googleHost = "www.google.se";
            yahooHost = "se.search.yahoo.com";
        } else if ("pl".equalsIgnoreCase(country)) {
            this.country = "pl";
            language = "pl";
            googleHost = "www.google.pl";
            yahooHost = "pl.search.yahoo.com";
        } else if ("ru".equalsIgnoreCase(country)) {
            this.country = "ru";
            language = "ru";
            googleHost = "www.google.ru";
            yahooHost = "ru.search.yahoo.com";
        } else if ("il".equalsIgnoreCase(country)) {
            this.country = "il";
            language = "il";
            googleHost = "www.google.co.il";
        } else if ("fr".equalsIgnoreCase(country)) {
            this.country = "fr";
            language = "fr";
            googleHost = "www.google.fr";
        } else if ("nl".equalsIgnoreCase(country)) {
            this.country = "nl";
            language = "nl";
            googleHost = "www.google.nl";
        } else {
            throw new WebServiceException("Invalid country '" + country + "' specified");
        }
    }

    public void setSearchSites(String searchSites) {
        this.searchSites.clear();
        this.searchSites.addAll(Arrays.asList(searchSites.split(",")));
    }

    public void setSearchSuffix(String searchSuffix) {
        this.searchSuffix = searchSuffix;
    }

    public String searchMovieURL(String title, String year, String site) {
        return searchMovieURL(title, year, site, null);
    }

    public String searchMovieURL(String title, String year, String site, String additional) {
        String url;

        String engine = getNextSearchEngine();

        LOG.trace("Searching on " + engine + " for Title: '" + title + "', Year: '" + year + "'");

        if ("yahoo".equalsIgnoreCase(engine)) {
            url = searchUrlOnYahoo(title, year, site, additional);
        } else if ("bing".equalsIgnoreCase(engine)) {
            url = searchUrlOnBing(title, year, site, additional);
        } else {
            url = searchUrlOnGoogle(title, year, site, additional);
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

    public String getCurrentSearchEngine() {
        return searchSites.peekFirst();
    }

    public String searchUrlOnGoogle(String title, String year, String site, String additional) {
        String returnedXml = Movie.UNKNOWN;

        try {
            StringBuilder sb = new StringBuilder("https://");
            sb.append(googleHost);
            sb.append("/search?");
            if (StringUtils.isNotBlank(language)) {
                sb.append("hl=");
                sb.append(language);
                sb.append("&");
            }
            sb.append("as_q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+(");
                sb.append(year);
                sb.append(")");
            }

            if (StringUtils.isNotBlank(additional)) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            sb.append("&as_sitesearch=");
            sb.append(site);

            String xml = webBrowser.request(sb.toString());

            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                returnedXml = xml.substring(beginIndex, xml.indexOf("\"", beginIndex));

                // Remove extra characters from the end of the film URL
                if (country.equals("pl") && !returnedXml.contains("/serial/")) {
                    // Strip out anythign after the first sub-URL
                    int pos = returnedXml.indexOf(site);
                    if (pos > -1) {
                        pos = returnedXml.indexOf('/', pos + site.length() + 1);
                        if (pos > -1) {
                            returnedXml = returnedXml.substring(0, pos);
                        }
                    }
                }
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving link url by google search: " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return returnedXml;
    }

    public String searchUrlOnYahoo(String title, String year, String site, String additional) {
        LOG.debug(LOG_MESSAGE + "Searching '" + title + "' on yahoo; site=" + site);

        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(yahooHost);
            sb.append("/search?vc=");
            if (StringUtils.isNotBlank(country)) {
                sb.append(country);
                sb.append("&rd=r2");
            }
            sb.append("&ei=UTF-8&p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }

            if (StringUtils.isNotBlank(additional)) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }

            sb.append("+site%3A");
            sb.append(site);

            String xml = webBrowser.request(sb.toString());

            String link = HTMLTools.extractTag(xml, "<span class=\"url\"", "</span>");
            link = HTMLTools.removeHtmlTags(link);
            int beginIndex = link.indexOf(site + searchSuffix);
            if (beginIndex != -1) {
                link = link.substring(beginIndex);
                if (StringTools.isValidString(link)) {
                    // Remove "/info/xxx" from the end of the URL
                    beginIndex = link.indexOf("/info");
                    if (beginIndex > -1) {
                        link = link.substring(0, beginIndex);
                    }
                    return "http://" + link;
                }
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving link URL by yahoo search: " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnBing(String title, String year, String site, String additional) {
        LOG.debug(LOG_MESSAGE + "Searching '" + title + "' on bing; site=" + site);

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
            if (StringUtils.isNotBlank(additional)) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            sb.append("+site%3A");
            sb.append(site);
            if (StringUtils.isNotBlank(country)) {
                sb.append("&cc=");
                sb.append(country);
                sb.append("&filt=rf");
            }

            String xml = webBrowser.request(sb.toString());

            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving link url by bing search: " + title);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }
}
