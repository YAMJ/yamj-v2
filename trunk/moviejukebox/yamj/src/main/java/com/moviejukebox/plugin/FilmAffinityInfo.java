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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

public class FilmAffinityInfo {
    private Logger logger = Logger.getLogger("moviejukebox");    
    private WebBrowser webBrowser;
    
    /* 
     * In filmAffinity there is several possible titles in the search results:
     * Spanish title
     * Spanish title (original title)
     * original title (Spanish title)  
     * 
     * It extracts one or both titles to find an exact match. 
     */
    private Pattern titlePattern = Pattern.compile("([^\\(]+)(  \\((.+)\\))?", Pattern.CASE_INSENSITIVE);
    /*
     * To test the URL and to determine if we got a redirect due to unique result
     */
    private Pattern idPattern = Pattern.compile(".+\\/es\\/(film[0-9]{6}\\.html).*");
    /*
     * To isolate every title (with id) from search results
     */
    private Pattern linkPattern = Pattern.compile("<b><a href=\"/es/(film[0-9]{6}\\.html)\">([^<]+)</a></b>");
    
    public static final String FILMAFFINITY_PLUGIN_ID = "filmaffinity"; 
    
    
    public FilmAffinityInfo() {
        webBrowser = new WebBrowser();
    }

    
    public String getIdFromMovieInfo(String title, String year) {
        return getIdFromMovieInfo(title, year, -1);
    }

    
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        String firstResponse = response;
        Matcher titleMatcher;
        Matcher linkMatcher;
        Matcher idMatcher;
                
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("http://www.filmaffinity.com/es/advsearch.php?stext=");

            sb.append(URLEncoder.encode(title, "ISO-8859-1"));
            if (tvSeason > -1) {
                sb.append("+TV");
            }

            sb.append("&stype[]=title&genre=&country=&");
            
            // It uses the year in URL advanced search (if it knows) 
            if (StringTools.isValidString(year)) {
                sb.append("fromyear=").append(year).append("&toyear=").append(year);
            } else {
                sb.append("fromyear=&toyear=");
            }
            
            String url = webBrowser.getUrl(sb.toString());
            
            idMatcher = idPattern.matcher(url);
            if (idMatcher.matches()) {
                // we got a redirect due to unique result
                response = idMatcher.group(1); 
            } else {                
                String xml = webBrowser.request(sb.toString(), Charset.forName("ISO-8859-1"));

                linkMatcher = linkPattern.matcher(xml);
                
                while (linkMatcher.find() && Movie.UNKNOWN.equals(response)) {
                    xml = new String(xml.substring(linkMatcher.start(1)));
                    
                    if (Movie.UNKNOWN.equalsIgnoreCase(firstResponse)) {
                        firstResponse = linkMatcher.group(1);
                    }
                    
                    titleMatcher = titlePattern.matcher(linkMatcher.group(2));                    
                    if (titleMatcher.matches()) {
                        if ((titleMatcher.group(1) != null && titleMatcher.group(1).equalsIgnoreCase(title)) || (titleMatcher.group(3) != null && titleMatcher.group(3).equalsIgnoreCase(title))) { 
                            response = linkMatcher.group(1);
                        }
                    }
                    
                    linkMatcher = linkPattern.matcher(xml);
                }

                if (Movie.UNKNOWN.equalsIgnoreCase(response)) {
                    response = firstResponse;                   
                }
            }

        } catch (Exception error) {
            logger.error("Failed retreiving filmaffinity Id for movie : " + title);
            logger.error("Error : " + error.getMessage());
        }
        return response;
    }

    
    /*
     * Normalize the FilmAffinity ID.
     * This permits use several types of ID:
     * film[0-9]{6}.html (this the complete and it returns this)
     * film[0-9]{6}  
     * [0-9]{6}.html
     * [0-9]{6}
     */
    public String arrangeId(String id) {
        Matcher matcher = Pattern.compile("(film[0-9]{6}\\.html)|(film[0-9]{6})|([0-9]{6}\\.html)|([0-9]{6})").matcher(id);
        
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return matcher.group(1); 
            }
            else if (matcher.group(2) != null ) {
                return matcher.group(2)+".html";
            }  
            else if (matcher.group(3) != null) {
                return "film"+matcher.group(3);
            }
            else {
                return "film"+matcher.group(4)+".html";                
            }
        }
        else {
            return Movie.UNKNOWN;
        }        
        
    }

}
