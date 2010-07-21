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

package com.moviejukebox.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * @author iuk
 *
 */
public class ComingSoonPlugin extends ImdbPlugin {

    public static String COMINGSOON_PLUGIN_ID = "comingsoon";
    public static String COMINGSOON_NOT_PRESENT = "na";
    private static String COMINGSOON_BASE_URL = "http://www.comingsoon.it/";
    private static String COMINGSOON_SEARCH_URL = "Film/Scheda/Trama/?";
    private static String COMINGSOON_KEY_PARAM= "key=";
    private static int COMINGSOON_MAX_DIFF = 1000;

    protected int preferredPlotLength;

    public ComingSoonPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
    }

    @Override
    public boolean scan(Movie mediaFile) {

        String comingSoonId = mediaFile.getId(COMINGSOON_PLUGIN_ID);

        // First we try on comingsoon.it
        
        if (comingSoonId == null || comingSoonId.equalsIgnoreCase(Movie.UNKNOWN)) {
            comingSoonId = getComingSoonId(mediaFile.getTitle(), mediaFile.getYear());
            mediaFile.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
        }

        // Then we use IMDB to get complete information. We back up outline and plot, since ImdbPlugin will overwrite them.
        
        String bkPlot = mediaFile.getPlot();

        logger.finest("ComingSoon: Checking IMDB");
        boolean firstScanImdb = super.scan(mediaFile);      
        if (comingSoonId.equalsIgnoreCase(Movie.UNKNOWN)) {
            // First run wasn't successful
            if (firstScanImdb) {
                // We try to fetch again ComingSoon, hopefully with more info
                logger.finest("ComingSoon: First search on ComingSoon was KO, retrying after succesful query to IMDB");
                comingSoonId = getComingSoonId(mediaFile.getTitle(), mediaFile.getYear());
                mediaFile.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
            }
        }

        boolean firstScanComingSoon = false;
        if (!comingSoonId.equalsIgnoreCase(Movie.UNKNOWN)) {

            logger.finest("ComingSoon: Fetching movie data from ComingSoon");
            
            String bkPlot2 = mediaFile.getPlot();
            mediaFile.setPlot(bkPlot);
            
            firstScanComingSoon = updateComingSoonMediaInfo(mediaFile);
            
            if (mediaFile.getPlot().equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setPlot(bkPlot2);
            }
        }
        
        if (!firstScanImdb && firstScanComingSoon) {
            // Scan was successful on ComingSoon but not on IMDB, let's try again with more info

            logger.finest("ComingSoon: First scan on IMDB KO, retrying after succesful scan on ComingSoon");
            
            String bkTitle = mediaFile.getTitle();
            String bkOriginalTitle = mediaFile.getOriginalTitle();

            super.scan(mediaFile);
            

            if (!bkOriginalTitle.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setTitle(bkOriginalTitle);
            } else if (!bkTitle.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setTitle(bkTitle);
            }
            
            if (!bkOriginalTitle.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setOriginalTitle(bkOriginalTitle);
            }

        }
        
        if (mediaFile.getOriginalTitle().equalsIgnoreCase(Movie.UNKNOWN)) {
            mediaFile.setOriginalTitle(mediaFile.getTitle());
        }
        
        
        /*logger.fine("------- FINAL RESULTS");
        logger.fine("TITLE:" + mediaFile.getTitle());
        logger.fine("ORIGINAL TITLE:" + mediaFile.getOriginalTitle());
        //logger.fine("OUTLINE:" + mediaFile.getOutline());
        logger.fine("PLOT:" + mediaFile.getPlot());
        logger.fine("----------------------------------");
        */
     
        return firstScanComingSoon || firstScanImdb; 
    }
    
    protected String getComingSoonId(String movieName, String year) {
        //return getComingSoonIdFromGoogle(movieName, year);
        return getComingSoonIdFromComingSoon(movieName, year);
    }

    protected String getComingSoonIdFromGoogle(String movieName, String year) {
        
        try {
            
            String comingSoonId = Movie.UNKNOWN;
                        
            StringBuffer sb = new StringBuffer("http://www.google.com/search?hl=it&q=");
            sb.append("\"" + URLEncoder.encode(movieName, "UTF-8") + "\"");              

            /*
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                // No year search as for now
            }*/

            sb.append("+site%3Acomingsoon.it");

            logger.finest("ComingSoon: Fetching ComingSoon search URL: " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            
           
            int beginIndex = xml.indexOf(COMINGSOON_BASE_URL + COMINGSOON_SEARCH_URL);
            if (beginIndex > 0) {
                comingSoonId = getComingSoonIdFromURL(xml.substring(beginIndex, xml.indexOf('"', beginIndex)));
                logger.finest("ComingSoon: Found ComingSoon ID: " + comingSoonId);
            }
            return comingSoonId;
            
            
        } catch (Exception error) {
            logger.severe("ComingSoon: Failed retreiving ComingSoon Id for movie : " + movieName);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return Movie.UNKNOWN;
        }
    }

    protected String getComingSoonIdFromComingSoon(String movieName, String year) {
        return getComingSoonIdFromComingSoon(movieName, year, COMINGSOON_MAX_DIFF);
    }
        
    protected String getComingSoonIdFromComingSoon(String movieName, String year, int scoreToBeat) {
        
        try {
            
            if (scoreToBeat == 0) {
                return Movie.UNKNOWN;
            }
            
            String comingSoonId = Movie.UNKNOWN;
            
            StringBuffer sb = new StringBuffer("http://www.comingsoon.it/Film/Database/?titoloFilm=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));              

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("&anno=" + year);
            }

            logger.finest("ComingSoon: Fetching ComingSoon search URL: " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            
            ArrayList<String[]> movieList = parseComingSoonSearchResults(xml);

            
            for (int i = 0; i < movieList.size() && scoreToBeat > 0; i++) {
                String lId = (String) movieList.get(i)[0];
                String lTitle = (String) movieList.get(i)[1];
                String lOrig = (String) movieList.get(i)[2];
                //String lYear = (String) movieList.get(i)[3];
                int difference = compareTitles(movieName, lTitle);
                int differenceOrig = compareTitles(movieName, lOrig);
                difference = (differenceOrig < difference ? differenceOrig : difference);
                if (difference < scoreToBeat) {
                    if (difference == 0) {
                        logger.finest("ComingSoon: Found perfect match for: " + lTitle + ", " + lOrig);
                    } else {
                        logger.finest("ComingSoon: Found a match for: " + lTitle + ", " + lOrig);
                    }
                    comingSoonId = lId;
                    scoreToBeat = difference;
                }
            }
            
            if (!year.equalsIgnoreCase(Movie.UNKNOWN) && scoreToBeat > 0) {
                logger.finest("ComingSoon: Perfect match not found, trying removing by year...");
                String newComingSoonId = getComingSoonIdFromComingSoon(movieName, Movie.UNKNOWN, scoreToBeat);
                comingSoonId = (newComingSoonId.equalsIgnoreCase(Movie.UNKNOWN) ? comingSoonId : newComingSoonId);
            }
            
            return comingSoonId;
            
        } catch (Exception error) {
            logger.severe("ComingSoon: Failed retreiving ComingSoon Id for movie : " + movieName);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return Movie.UNKNOWN;
        }
    }
    
    private ArrayList<String[]> parseComingSoonSearchResults(String xml) {
        
        int beginIndex = xml.indexOf("class=\"titoloFilm\"");
        ArrayList<String[]> listaFilm = new ArrayList<String[]>();
        
        while (beginIndex > 0) {
            int urlIndex = xml.lastIndexOf(COMINGSOON_SEARCH_URL, beginIndex);
            int beginLinkTextIndex = xml.indexOf(">", urlIndex) + 1;
            int endLinkTextIndex = xml.indexOf("</a>", urlIndex);
            
            String comingSoonId = getComingSoonIdFromURL (xml.substring(urlIndex, xml.indexOf('"', urlIndex)));

            int nextIndex = xml.indexOf("class=\"titoloFilm\"", beginIndex + 1);
            String title = HTMLTools.stripTags(xml.substring(beginLinkTextIndex, endLinkTextIndex)).replaceAll("&nbsp;", " ").trim();

            String search;
            if (nextIndex > 0) {
                search = xml.substring(beginIndex, nextIndex);
            } else {
                search = xml.substring(beginIndex);
            }
            
            String originalTitle = HTMLTools.extractTag(search, "<span class=\"titoloFilm2\"").trim();
            if (originalTitle.startsWith("(")) {
                originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            } else if (originalTitle.length() == 0) {
                originalTitle = Movie.UNKNOWN;
            }

            String year = Movie.UNKNOWN;
            int beginYearIndex = search.indexOf("ANNO PROD:</span>");
            if (beginYearIndex > 0) {
                year = HTMLTools.extractTag(search.substring(beginYearIndex), "<b").trim();
            }
            
            String[] movieData = {comingSoonId, title, originalTitle, year};
            listaFilm.add(movieData);

            beginIndex = nextIndex;
        }

        return listaFilm;
        
    }
    
    private String getComingSoonIdFromURL (String URL) {
        //logger.fine("ComingSoon: Parsing URL: " + URL);
        int beginIndex = URL.indexOf(COMINGSOON_KEY_PARAM);
        StringTokenizer st = new StringTokenizer(URL.substring(beginIndex + COMINGSOON_KEY_PARAM.length()), "&/\"");
        String comingSoonId = st.nextToken();
        
        return comingSoonId;
    }
    
    
    private int compareTitles(String searchedTitle, String returnedTitle) {
        
        /*
         *  Returns difference between two titles.
         *  
         *  Since ComingSoon returns strange results on some researches, difference is defined as follows:
         *  
         *  abs(word count difference) - (searchedTitle word count - matched words);  
         */
        
        if (returnedTitle.equals(Movie.UNKNOWN)) {
            return COMINGSOON_MAX_DIFF;
        }
        
        logger.finest("ComingSoon: Comparing: " + searchedTitle + " and : " + returnedTitle);
        
        StringTokenizer st1 = new StringTokenizer(searchedTitle);
        int lastMatchedWord = -1;
        int searchedWordCount = st1.countTokens();
        int returnedWordCount = 0;
        int matchingWords = 0;
        
        while (st1.hasMoreTokens()) {
            String candidate1 = st1.nextToken();
            
            candidate1.replaceAll("[,.\\!\\?\"']", "");

            boolean gotMatch = false;
            int wordCounter = 0;
            StringTokenizer st2 = new StringTokenizer(returnedTitle);
            returnedWordCount = st2.countTokens();
            while (st2.hasMoreTokens() && ! gotMatch) {
                String candidate2 = st2.nextToken();

                if (wordCounter > lastMatchedWord) {
                    if (candidate1.equalsIgnoreCase(candidate2)) {
                        gotMatch = true;
                        matchingWords++;
                        lastMatchedWord = wordCounter;
                    }
                }
                wordCounter++;
            }
        }
        int difference = (searchedWordCount - returnedWordCount > 0 ? searchedWordCount - returnedWordCount : returnedWordCount - searchedWordCount) + (searchedWordCount - matchingWords); 
        
        if (returnedTitle.indexOf('-') >= 0) {
            StringTokenizer st2 = new StringTokenizer(returnedTitle, "-");
            while (st2.hasMoreTokens() && difference > 0) {
                int newDiff = compareTitles(searchedTitle, st2.nextToken().trim());
                difference = newDiff < difference ? newDiff : difference;
            }
        }
        
        return difference;

    }
    
    
    protected boolean updateComingSoonMediaInfo(Movie movie) {
        
        if (movie.getId(COMINGSOON_PLUGIN_ID).equalsIgnoreCase(COMINGSOON_NOT_PRESENT)) {
            return false;
        }
        
        try {
            String movieURL = COMINGSOON_BASE_URL + COMINGSOON_SEARCH_URL +  COMINGSOON_KEY_PARAM + movie.getId(COMINGSOON_PLUGIN_ID);
            logger.finest("ComingSoon: Querying ComingSoon for " + movieURL);
            String xml = webBrowser.request(movieURL);
            
            String title = HTMLTools.extractTag(xml, "<h1 class='titoloFilm'").trim();
            String originalTitle = HTMLTools.extractTag(xml, "<h1 class='titoloFilm2'").trim();
            if (originalTitle.startsWith("(")) {
                originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
            }
            
            if (title == null || title.equalsIgnoreCase(Movie.UNKNOWN)) {
                logger.severe("ComingSoon: No title found at ComingSoon page. HTML layout has changed?");
                return false;
            }
            
            title = correctCapsTitle(title);
            originalTitle = correctCapsTitle(originalTitle);
            
            
            if (!movie.isOverrideTitle()) {
                movie.setTitle(title);
                movie.setOriginalTitle(originalTitle);
            }

            if (movie.getPlot().equals(Movie.UNKNOWN)) {
            
                int beginIndex = xml.indexOf("<span class='vociFilm'>Trama del film");
                if (beginIndex < 0) {
                    logger.severe("ComingSoon: No plot found at ComingSoon page. HTML layout has changed?");
                    return false;
                }
    
                beginIndex = xml.indexOf("</span>", beginIndex);
                if (beginIndex < 0) {
                    logger.severe("ComingSoon: No plot found at ComingSoon page. HTML layout has changed?");
                    return false;
                }
    
                int endIndex = xml.indexOf("</div>", beginIndex);
                if (endIndex < 0) {
                    logger.severe("ComingSoon: No plot found at ComingSoon page. HTML layout has changed?");
                    return false;
                }
                
                String rawPlot = xml.substring(beginIndex + 7, endIndex - 1).trim();
                String plot = HTMLTools.stripTags(rawPlot).trim();
                
                if (plot.length() > preferredPlotLength) {
                    plot = plot.substring(0, Math.min(plot.length(), preferredPlotLength - 3)) + "...";
                }
                
                movie.setPlot(plot);
            }

            return true;
            
        } catch (Exception error) {
            logger.severe("ComingSoon: Failed retreiving ComingSoon data for movie : " + movie.getId(COMINGSOON_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return false;
        } 

    }
    
    /**
     * ComingSoon has some titles all caps. We normalize them
     * @param title
     * @return
     */
    private String correctCapsTitle(String title) {
        if (title.equals(title.toUpperCase())) {
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(title);
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                sb.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                if (st.hasMoreTokens()) {
                    sb.append(' ');
                }
            }
            return sb.toString();
        } else {
            return title;
        }
        
    }
}
