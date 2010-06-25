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
import java.text.Normalizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;


public class MovieCoversPosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    public MovieCoversPosterPlugin() {
        super();
      webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String returnString = Movie.UNKNOWN;
        Pattern titleregex;
        Matcher titlematch;
        
        try {
            StringBuffer sb = new StringBuffer("http://www.moviecovers.com/multicrit.html?titre=");
            sb.append(URLEncoder.encode(title, "iso-8859-1"));
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("&anneemin=");
                sb.append(URLEncoder.encode(Integer.toString(Integer.parseInt(year) - 1), "iso-8859-1"));
                sb.append("&anneemax=");
                sb.append(URLEncoder.encode(Integer.toString(Integer.parseInt(year) + 1), "iso-8859-1"));
            }
            sb.append("&slow=0&tri=Titre&listes=1");
            String content = webBrowser.request(sb.toString());

            if (content != null) {
        // Check for "no result" message...
                titleregex = Pattern.compile("/forum/index.html\\?forum=MovieCovers&vue=demande");
                titlematch = titleregex.matcher(content);
                if (!titlematch.find()) {
       // There is some results
       // Check for exact match
                    titleregex = Pattern.compile("<LI><A href=\"/film/titre_(\\w+).html\">" + URLEncoder.encode(title.toUpperCase(), "iso-8859-1") + "</A>");
                    titlematch = titleregex.matcher(content);
                    if (titlematch.find()) {
                        returnString = titlematch.group(1);
                    } else {
        // If no exact match found, check for the first match
                        titleregex = Pattern.compile("<LI><A href=\"/film/titre_(\\w+).html\">");
                        titlematch = titleregex.matcher(content);
                        if (titlematch.find()) {
                            returnString = titlematch.group(1);
                        }
                    }
                } else {
        // Search the forum if no answer
                    String formattedTitle = Normalizer.normalize(title.toUpperCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
                    if (formattedTitle.endsWith(" (TV)")) {
                        formattedTitle = formattedTitle.substring(0, formattedTitle.length() - 5);
                    }
                    String formattedTitleNormalized = formattedTitle;
                    for (String prefix : Movie.sortIgnorePrefixes) {
                        if (formattedTitle.startsWith(prefix.toUpperCase())) {
                            formattedTitleNormalized = formattedTitle.substring(prefix.length()) + " (" + prefix.toUpperCase().replace(" ","") + ")";
                            break;
                        }
                    }
                    sb = new StringBuffer("http://www.moviecovers.com/forum/search-mysql.html?forum=MovieCovers&query=");
                    sb.append(URLEncoder.encode(formattedTitle, "iso-8859-1"));
                    content = webBrowser.request(sb.toString());
                    if (content != null) {
        // Loop through the search results
                        for (String filmURL : HTMLTools.extractTags(content, "<TABLE border=\"0\" cellpadding=\"0\" cellspacing=\"0\">", "<FORM action=\"search-mysql.html\">", "<TD><A href=\"fil.html?query=", "</A></TD>", false)) {
                            if ( (filmURL.endsWith(formattedTitleNormalized)) || (filmURL.endsWith(formattedTitle)) ) {
                                content = webBrowser.request("http://www.moviecovers.com/forum/fil.html?query=" + filmURL.substring(0,filmURL.length()-formattedTitle.length()-2));
                                if (content != null) {
                                    int sizePoster = 0;
                                    int oldSizePoster = 0;
        // Search the biggest picture
                                    for (String poster : HTMLTools.extractTags(content, "</STRONG></B></FONT>", ">MovieCovers Team<", "<LI><A TARGET=\"affiche\" ", "Ko)", false)) {
                                        sizePoster = Integer.parseInt(HTMLTools.extractTag(poster, ".jpg\">Image .JPG</A> ("));
                                        if (sizePoster > oldSizePoster) {
                                            oldSizePoster = sizePoster;
                                            returnString = HTMLTools.extractTag(poster, "HREF=\"/getjpg.html/", ".jpg\">Image .JPG</A>");
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("MovieCoversPosterPlugin: Failed retreiving Moviecovers poster URL: " + title);
            logger.severe("MovieCoversPosterPlugin: Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
        logger.finer("MovieCoversPosterPlugin: retreiving Moviecovers poster URL: " + returnString);
        return returnString;
    }

    @Override
    public String getPosterUrl(String title, String year) {
       return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getPosterUrl(String id) {
        String returnString = Movie.UNKNOWN;
        try {
        if (id != null && !Movie.UNKNOWN.equalsIgnoreCase(id)) {
            logger.finer("MovieCoversPosterPlugin : Movie found on moviecovers.com" + id);
            returnString = "http://www.moviecovers.com/getjpg.html/" + id.replace("+", "%20");
        } else {
            logger.finer("MovieCoversPosterPlugin: Unable to find posters for " + id);
        }
        } catch (Exception error) {
            logger.finer("MovieCoversPosterPlugin: MovieCovers.com API Error: " + error.getMessage());
        }
        return returnString;
    }

    @Override
    public String getName() {
        return "moviecovers";
    }

}
