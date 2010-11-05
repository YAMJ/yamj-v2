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

package com.moviejukebox.plugin.poster;

import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class FilmAffinityPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    // The AbstractMoviePosterPlugin already implements IMoviePosterPlugin 
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Pattern yearMatcher = Pattern.compile(".*\\((\\d{4})\\).*", Pattern.CASE_INSENSITIVE);
    private WebBrowser webBrowser;
    private static String idSearch = PropertiesUtil.getProperty("filmaffinity.id.search", "filmaffinity");

    public FilmAffinityPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        String firstResponse = response;
        try {
            StringBuffer sb = new StringBuffer();
            
            if (idSearch.equalsIgnoreCase("filmaffinity")) {
                sb.append("http://www.filmaffinity.com/es/search.php?stext=");

                sb.append(URLEncoder.encode(title, "UTF-8"));
                if (tvSeason > -1) {
                    sb.append("+TV");
                }
                // if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                // sb.append("+").append(year);
                // }

                sb.append("&stype=title");
            } else {
                sb.append("http://www.google.com/search?q=");

                sb.append(URLEncoder.encode(title, "UTF-8"));

                // Do not append "site:" to the search as this yeilds better results
                sb.append("+www.filmaffinity.com");

                if (tvSeason > -1) {
                    sb.append("+TV");
                } else {
                    if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                        sb.append("+(").append(year).append(")");
                    }
                }
            }
            logger.finest("FilmAffinity Search URL: " + sb.toString());
            
            String url = webBrowser.getUrl(sb.toString());
            String startSearchString = "/es/film";
            // we got a redirect due to unique result
            if (url != null && url.contains(startSearchString)) {
                response = url.substring(url.indexOf(startSearchString)+startSearchString.length(), url.indexOf(".html") + 5);
            } else {

                String xml = webBrowser.request(sb.toString());

                int startSearch = 0;
                int beginIndex;
                // While we found movie link.

                while ((beginIndex = xml.indexOf(startSearchString, startSearch)) > -1 && Movie.UNKNOWN.equals(response)) {
                    startSearch = beginIndex + startSearchString.length();

                    // Keep the first find.
                    if (Movie.UNKNOWN.equalsIgnoreCase(firstResponse)) {
                        firstResponse = xml.substring(startSearch, xml.indexOf(".html", startSearch) + 5);
                    }
                    // If we search with year.
                    if (year != null && !Movie.UNKNOWN.equalsIgnoreCase(year)) {
                        String yearFound = null;
                        String substring = xml.substring(startSearch);
                        Matcher matcher = yearMatcher.matcher(substring.substring(0, substring.indexOf("</td></tr>")));
                        if (matcher.find()) {
                            yearFound = matcher.group(1);
                        }
                        // int endIndex = xml.indexOf(")", startSearch);
                        // String yearFound = null;
                        //
                        // if (endIndex != -1) {
                        // yearFound = xml.substring(endIndex - 4, endIndex);
                        // }
                        if (yearFound != null) {
                            if (yearFound.equalsIgnoreCase(year)) {
                                response = xml.substring(startSearch, xml.indexOf(".html", startSearch) + 5);
                            }
                        }
                    } else {
                        // No year, first result.
                        response = firstResponse;
                    }
                }
            }
            // int beginIndex = xml.indexOf(startSearchString, 0); // <a href="/es/film325907.html">Platoon of the Dead</a></b> (2009) <img
            // StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 8), "/\"");
            // String filmAffinityId = st.nextToken();

            if (!Movie.UNKNOWN.equalsIgnoreCase(response)) {
                logger.finest("FilmAffinity: Found id: " + response);
                // response = response;
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving filmaffinity Id for movie : " + title);
            logger.severe("Error : " + error.getMessage());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        // <td><img src="
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.filmaffinity.com/es/film");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                posterURL = HTMLTools.extractTag(xml, "<a class=\"lightbox\" href=\"", "\"");

            } catch (Exception e) {
                logger.severe("Failed retreiving FilmAffinity poster url for movie : " + id);
                logger.severe("Error : " + e.getMessage());
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }

    @Override
    public String getName() {
        return "filmaffinity";
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return getIdFromMovieInfo(title, year, -1);
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident, movieInformation);

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            } else {
                return getPosterUrl(id);
            }
        }
        return Image.UNKNOWN;
    }

    public String getId(Identifiable ident, IMovieBasicInformation movieInformation) {
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
        return id;
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
