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
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

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
    private static final String POSTER_BASE_URL = "http://www.comingsoon.it/imgdb/locandine/big/";

    protected int preferredPlotLength;

    public ComingSoonPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
    }

    @Override
    public boolean scan(Movie movie) {

        String comingSoonId = movie.getId(COMINGSOON_PLUGIN_ID);

        // First we try on comingsoon.it
        
        if (StringTools.isNotValidString(comingSoonId)) {
            comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
            movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
        }

        // Then we use IMDB to get complete information. We back up outline and plot, since ImdbPlugin will overwrite them.
        
        String bkPlot = movie.getPlot();

        logger.finest("ComingSoon: Checking IMDB");
        boolean firstScanImdb = super.scan(movie);      
        if (StringTools.isNotValidString(comingSoonId)) {
            // First run wasn't successful
            if (firstScanImdb) {
                // We try to fetch again ComingSoon, hopefully with more info
                logger.finest("ComingSoon: First search on ComingSoon was KO, retrying after succesful query to IMDB");
                comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
                movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
            }
        }

        boolean firstScanComingSoon = false;
        if (StringTools.isValidString(comingSoonId)) {

            logger.finest("ComingSoon: Fetching movie data from ComingSoon");
            
            String bkPlot2 = movie.getPlot();
            movie.setPlot(bkPlot);
            
            firstScanComingSoon = updateComingSoonMediaInfo(movie);
            
            if (StringTools.isNotValidString(movie.getPlot())) {
                movie.setPlot(bkPlot2);
            }
        }
        
        if (!firstScanImdb && firstScanComingSoon) {
            // Scan was successful on ComingSoon but not on IMDB, let's try again with more info

            logger.finest("ComingSoon: First scan on IMDB KO, retrying after succesful scan on ComingSoon");
            
            String bkTitle = movie.getTitle();
            String bkOriginalTitle = movie.getOriginalTitle();

            super.scan(movie);

            if (StringTools.isValidString(bkOriginalTitle)) {
                movie.setTitle(bkOriginalTitle);
            } else if (StringTools.isValidString(bkTitle)) {
                movie.setTitle(bkTitle);
            }
            
            if (StringTools.isValidString(bkOriginalTitle)) {
                movie.setOriginalTitle(bkOriginalTitle);
            }

        }
        
        if (StringTools.isNotValidString(movie.getOriginalTitle())) {
            movie.setOriginalTitle(movie.getTitle());
        }
        
        // Quick and dirty set the poster URL if it's blank
        if (StringTools.isValidString(comingSoonId) && StringTools.isNotValidString(movie.getPosterURL())) {
            movie.setPosterURL(POSTER_BASE_URL + comingSoonId + ".jpg");
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
        String id;
        id = getComingSoonIdFromComingSoon(movieName, year);
        if (StringTools.isValidString(id)) {
            return id;
        }
        
        id = getComingSoonIdFromSearch("http://search.yahoo.com/search?vc=&p=", movieName, year);
        if (StringTools.isNotValidString(id)) {
            // Try a Google search
            id = getComingSoonIdFromSearch("http://www.google.com/search?hl=it&q=", movieName, year);
        }
        //return getComingSoonIdFromComingSoon(movieName, year);
        return id;
    }

    protected String getComingSoonIdFromSearch(String searchUrl, String movieName, String year) {
        try {
            String comingSoonId = Movie.UNKNOWN;
                        
            StringBuffer sb = new StringBuffer(searchUrl);
            sb.append("\"" + URLEncoder.encode(movieName, "UTF-8") + "\"");              

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

            if (StringTools.isValidString(year)) {
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
            
            if (StringTools.isValidString(year) && scoreToBeat > 0) {
                logger.finest("ComingSoon: Perfect match not found, trying removing by year...");
                String newComingSoonId = getComingSoonIdFromComingSoon(movieName, Movie.UNKNOWN, scoreToBeat);
                comingSoonId = (StringTools.isNotValidString(newComingSoonId) ? comingSoonId : newComingSoonId);
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
    
    private String getComingSoonIdFromURL (String url) {
        //logger.fine("ComingSoon: Parsing URL: " + URL);
        int beginIndex = url.indexOf(COMINGSOON_KEY_PARAM);
        StringTokenizer st = new StringTokenizer(url.substring(beginIndex + COMINGSOON_KEY_PARAM.length()), "&/\"");
        String comingSoonId = st.nextToken();
        
        return comingSoonId;
    }
    
    /**
     * Returns difference between two titles.
     * Since ComingSoon returns strange results on some researches, difference is defined as follows:
     * abs(word count difference) - (searchedTitle word count - matched words);
     * 
     * @param searchedTitle
     * @param returnedTitle
     * @return
     */
    private int compareTitles(String searchedTitle, String returnedTitle) {
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
            
            if (StringTools.isNotValidString(title)) {
                logger.severe("ComingSoon: No title found at ComingSoon page. HTML layout has changed?");
                return false;
            }
            
            title = correctCapsTitle(title);
            originalTitle = correctCapsTitle(originalTitle);
            
            
            if (!movie.isOverrideTitle()) {
                movie.setTitle(title);
                movie.setOriginalTitle(originalTitle);
            }

            if (StringTools.isNotValidString(movie.getPlot())) {
            
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
                
                String xmlPlot = xml.substring(beginIndex + 7, endIndex - 1).trim();
                xmlPlot = HTMLTools.stripTags(xmlPlot).trim();
                
                /*
                 *  There are sometimes two markers in the plot to differentiate between the 
                 *  plot (TRAMA LUNGA) and the outline (TRAMA BREVE). We should extract these
                 *  and use them appropriately
                 */
                
                String plot = "";
                String outline = "";
                int plotStart = xmlPlot.toUpperCase().indexOf("TRAMA LUNGA");
                int outlineStart = xmlPlot.toUpperCase().indexOf("TRAMA BREVE");
                
                if (plotStart == -1 && outlineStart == -1) {
                    // We've found neither, so the plot stays the same
                    plot = xmlPlot;
                } else {
                    // We've found at least one of the plots
                    if (outlineStart == -1 && plotStart > 0) {
                        // We'll assume that the outline is at the beginning of the plot
                        outline = xmlPlot.substring(0, plotStart);
                    } else {
                        outline = xmlPlot.substring(11, plotStart);
                    }
                    plot = xmlPlot.substring(plotStart + 11);
                }
                
                plot = StringTools.trimToLength(plot, preferredPlotLength, true, plotEnding);
                outline = StringTools.trimToLength(outline, preferredPlotLength, true, plotEnding);

                movie.setPlot(plot);
                movie.setOutline(outline);
            }

            // GENRES
            String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE: ", "<br />"));
            if (StringTools.isValidString(genreList)) {
                Collection<String> genres = new ArrayList<String>();
                
                StringTokenizer st = new StringTokenizer(genreList, ",");
                while (st.hasMoreTokens()) {
                    genres.add(st.nextToken().trim());
                }
                movie.setGenres(genres);
            }
            
            // DIRECTOR
            if (movie.getDirectors().isEmpty()) {
                String directorList = HTMLTools.stripTags(HTMLTools.extractTag(xml, "REGIA: </span>", "</a>"));
                
                if (directorList.contains(",")) {
                    StringTokenizer st = new StringTokenizer(directorList,",");
                    while (st.hasMoreTokens()) {
                        movie.addDirector(st.nextToken());
                    }
                } else {
                    movie.addDirector(directorList);
                }
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

    @Override
    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB as basis

        int beginIndex = nfo.indexOf("?key=");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 5), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(COMINGSOON_PLUGIN_ID, st.nextToken());
            logger.finer("ComingSoon Id found in nfo = " + movie.getId(COMINGSOON_PLUGIN_ID));
        } else {
            logger.finer("No ComingSoon Id found in nfo!");
        }
    }

}
