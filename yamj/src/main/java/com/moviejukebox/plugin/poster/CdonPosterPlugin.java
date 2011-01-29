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

package com.moviejukebox.plugin.poster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.logging.Logger;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class CdonPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    // The AbstractMoviePosterPlugin already implements IMoviePosterPlugin 
    private static Logger logger = Logger.getLogger("moviejukebox");

    protected WebBrowser webBrowser;

    public CdonPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }
    
    @Override
    public boolean isNeeded() {
        if ((searchPriorityMovie + "," + searchPriorityTv).contains(this.getName())) {
            return true;
        } else {
            return false;
        }
    }
    
    /* Implements getName for IPosterPlugin
     * @return String posterPluginName
     * @see com.moviejukebox.plugin.poster.IPosterPlugin#getName()
     */
    @Override
    public String getName() {
        return "cdon";
    }
    
    /* Implements getIdFromMovieInfo for ITvShowPosterPlugin
     * Returns an url for the Cdon movie information page
     * @return String id
     * @see com.moviejukebox.plugin.poster.ITvShowPosterPlugin#getIdFromMovieInfo(java.lang.String, java.lang.String, int)
     */
    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        String xml = null;

        // Search CDON to get an URL to the movie page
        // if title starts with "the" -> remove it to get better results
        if (title.toLowerCase().startsWith("the")) {
            title = title.substring(4, title.length());
        }
        try {
            //try getting the search results from cdon.se
            StringBuffer sb = new StringBuffer("");
            sb = new StringBuffer("http://cdon.se/search?q=");
            String searchTitle = URLEncoder.encode(title, "UTF-8");
            if (tvSeason >= 0) {
                searchTitle +=("+" + URLEncoder.encode("säsong", "UTF-8"));
                searchTitle +=("+" + tvSeason);
            }
            sb.append(searchTitle);
            //new search url format 2010-10-11
            //category=2 results in only movies being shown
            sb.append("#q=" + searchTitle);
            sb.append("&category=2");
            //get the result page from the search
            xml = webBrowser.request(sb.toString());
        } catch (Exception error) {
            //there was an error getting the web page, catch the exception
            logger.severe("CdonPosterPlugin: An exception happened while retreiving CDON id for movie : " + title);
            logger.severe("CdonPosterPlugin: the exception was caused by: " + error.getCause());
        }
         
        //check that the result page contains some movie info
        if (xml.contains("<table class=\"product-list\"")) {
            //find the movie url in the search result page
            response = findMovieFromProductList(xml, title, year);

        //else there was no results matching, return Movie.UNKNOWN
        } else {
            response = Movie.UNKNOWN;
            logger.finer("CdonPosterPlugin: could not find movie: " + title);
        } 
        return response;
    }
    
    /* function that takes the search result page from cdon and loops 
     * through the products in that page searching for a match
    */
    private String findMovieFromProductList(String xml, String title, String year) {
        //remove unused parts of resulting web page
        int beginIndex = xml.indexOf("class=\"product-list\"")+53;
        int endIndex = xml.indexOf("</table>");
        String xmlPart = xml.substring(beginIndex, endIndex);
        //the result is in a table split it on tr elements
        String[] productList = xmlPart.split("<tr>");

        //loop through the product list
        int foundAtIndex = 0;
        int firstTitleFind = 0;
        for (int i = 1; i < productList.length; i++) {
            String[] productInfo = productList[i].split("<td");
            boolean isMovie = false;
            if (productInfo.length >= 2 && (productInfo[2].toLowerCase().contains("dvd") || productInfo[2].toLowerCase().contains("blu-ray")) ){
                isMovie = true;
            }
            //only check for matches for movies
            if( isMovie ) {
                //movieInfo[3] contains title
                //movieInfo[6] contains year
                String foundTitle = HTMLTools.removeHtmlTags(productInfo[3].substring(15));
                foundTitle = foundTitle.replaceAll("\\t", ""); 
                if (foundTitle.toLowerCase().contains(title.toLowerCase())) {
                    //save first title find to fallback to if nothing else is found
                    if (firstTitleFind == 0) {
                        firstTitleFind = i;
                    }
                    //check if year matches
                    if(year != null) {
                        //year is in position 13-17
                        String date = productInfo[6].substring(14, 18);
                        if(year.equals(date)) {
                            foundAtIndex = i;
                            break;
                        }
                    }
                    //year not set - we found a match
                    else {
                        foundAtIndex = i;
                        break;
                    }
                } //end for found title
            } //end isMovie
        }
        //if we found a match return it without further ado
        if (foundAtIndex > 0 || firstTitleFind > 0) {
            //if no match with year use the first title find
            if (foundAtIndex == 0) {
                foundAtIndex = firstTitleFind;
            }
            //find the movie detail page that is in the td before the year
            return extractMovieDetailUrl(title, productList[foundAtIndex]);
        } else {
            //we got a productList but the matching of titles did not result in a match
            //take a chance and return the first product in the list
            return extractMovieDetailUrl(title, productList[1]);
        }
    }
    
    //function to extract the url of a movie detail page from the td it is in
    private String extractMovieDetailUrl(String title, String response) {
        // Split string to extract the url
        if (response.contains("http")) {
            String[] splitMovieURL = response.split("\\s");
            //movieDetailPage is in splitMovieURL[13]
            response = splitMovieURL[13].replaceAll("href|=|\"", "");
            logger.finest("CDonPosterPlugin: found cdon movie url = " + response);
        } else {
            //something went wrong and we do not have an url in the result
            //set response to Movie.UNKNOWN and write to the log
            response = Movie.UNKNOWN;
            logger.finer("CdonPosterPlugin: error extracting movie url for: " + title);
        }
        return response;
    }
    
    /* 
     * Implements getIdFromMovieInfo for IMoviePosterPlugin. 
     * Just adds an empty value for year and calls the method getIdFromMovieInfo(string, string, int); 
     * @see com.moviejukebox.plugin.poster.IMoviePosterPlugin#getIdFromMovieInfo(java.lang.String, java.lang.String)
     */
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return getIdFromMovieInfo(title, year, -1);
    }
    
    /* Implements getPosterUrl for IMoviePosterPlugin
     * The url of the Cdon movie information page is passed as id
     * @return IImage posterUrl
     * @see com.moviejukebox.plugin.poster.IMoviePosterPlugin#getPosterUrl(java.lang.String)
     */
    @Override
    public IImage getPosterUrl(String id) {
        if (!id.contains("http")) {
            id = getIdFromMovieInfo(id, null);
        }
        String posterURL = Movie.UNKNOWN;
        String xml = "";
        try {
            xml = getCdonMovieDetailsPage(id);
            // extract poster url and return it
            posterURL = extractCdonPosterUrl(xml);
        } catch (Exception error) {
            logger.severe(" CdonPosterPlugin: Failed retreiving Cdon poster for movie : " + id);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        logger.finer(" CdonPosterPlugin: No poster found for movie: " + id);
        return Image.UNKNOWN;
    }
    
    /* Implements getPosterUrl for IMoviePosterPlugin. Uses getIdFromMovieInfo 
     * to find an id and then calls getPosterUrl(String id, String year). 
     * @see com.moviejukebox.plugin.poster.ITvShowPosterPlugin#getPosterUrl(java.lang.String, java.lang.String, int)
     */
    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }
    
    /* Implements getPosterUrl for ITVShowPosterPlugin. Uses getIdFromMovieInfo 
     * to find an id and then calls getPosterUrl(String id). 
     * @see com.moviejukebox.plugin.poster.ITvShowPosterPlugin#getPosterUrl(java.lang.String, java.lang.String, int)
     */
    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }
    
    /* Implements getPosterUrl for ITVShowPosterPlugin.   
     * @see com.moviejukebox.plugin.poster.ITvShowPosterPlugin#getPosterUrl(java.lang.String, java.lang.String, int)
     */    
    @Override
    public IImage getPosterUrl(String title, int season) {
        return getPosterUrl(getIdFromMovieInfo(title, null, season));
    }
    
    /* Implements getPosterUrl for IPosterPlugin
     * @see com.moviejukebox.plugin.poster.IPosterPlugin#getPosterUrl(com.moviejukebox.model.Identifiable, com.moviejukebox.model.IMovieBasicInformation)
     */
    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear(), movieInformation.getSeason());
            } else {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            }
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                ident.setId(getName(), id);
            }
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            } else {
                return getPosterUrl(id);
            }

        }
        return Image.UNKNOWN;
    }
    
    protected String getCdonMovieDetailsPage(String movieURL) {
        String cdonMoviePage;
        try {
            // sanity check on result before trying to load details page from url
            if (!movieURL.isEmpty() && movieURL.contains("http")) {
                // fetch movie page from cdon
                StringBuffer buf = new StringBuffer(movieURL);
                cdonMoviePage = webBrowser.request(buf.toString());
                return cdonMoviePage;
            } else {
                // search didn't even find an url to the movie
                logger.finer("CdonPosterPlugin: Error in fetching movie detail page from CDON for movie: " + movieURL);
                return Movie.UNKNOWN;
            }
        } catch (Exception error) {
            logger.severe("CdonPosterPlugin: Error while retreiving CDON image for movie : " + movieURL);
            // logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    protected String extractCdonPosterUrl(String cdonMoviePage) {

        String cdonPosterURL = Movie.UNKNOWN;
        String[] htmlArray = cdonMoviePage.split("<");
        
        // check if there is an large front cover image for this movie
        if (cdonMoviePage.contains("St&#246;rre framsida")) {
            // first look for a large cover
            cdonPosterURL = findUrlString("St&#246;rre framsida", htmlArray);
        } else if (cdonMoviePage.contains("/media-dynamic/images/product/")) {
            // if not found look for a small cover
            cdonPosterURL = findUrlString("/media-dynamic/images/product/", htmlArray);
        } else {
            logger.info("CdonPosterPlugin: No CDON cover was found for movie ");
        }
        return cdonPosterURL;
    }

    protected String findUrlString(String searchString, String[] htmlArray) {
        String result;
        String[] posterURL = null;
        // all cdon pages don't look the same so
        // loop over the array to find the string with a link to an image
        int i = 0;
        for (String s : htmlArray) {
            if (s.contains(searchString)) {
                // found a matching string
                posterURL = htmlArray[i].split("\"|\\s");
                break;
            }
            i++;
        }
        // sanity check again (the found url should point to a jpg)
        if (posterURL != null && posterURL.length > 2 && posterURL[2].contains(".jpg")) {
            result = "http://cdon.se" + posterURL[2];
            logger.finest("CdonPosterPlugin:  found cdon cover: " + result);
            return result;
        } else {
            logger.info("CdonPosterPlugin: error in finding poster url.");
            return Movie.UNKNOWN;
        }
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }
    
}
