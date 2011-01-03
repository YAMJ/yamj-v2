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
import java.nio.charset.Charset;
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
    
    private static int COMINGSOON_MAX_SEARCH_PAGES = 5;
    

    private static int COMINGSOON_RESTORE_TITLE         = 1 << 0;
    private static int COMINGSOON_RESTORE_ORIGINALTITLE = 1 << 1;
    private static int COMINGSOON_RESTORE_PLOT          = 1 << 2;
    private static int COMINGSOON_RESTORE_OUTLINE       = 1 << 3;
    private static int COMINGSOON_RESTORE_RATING        = 1 << 4;
    private static int COMINGSOON_RESTORE_RUNTIME       = 1 << 5;
    private static int COMINGSOON_RESTORE_COUNTRY       = 1 << 6;
    private static int COMINGSOON_RESTORE_YEAR          = 1 << 7;
    private static int COMINGSOON_RESTORE_COMPANY       = 1 << 8;
    private static int COMINGSOON_RESTORE_GENRES        = 1 << 9;
    private static int COMINGSOON_RESTORE_CAST          = 1 << 10;
    private static int COMINGSOON_RESTORE_DIRECTORS     = 1 << 11;
    private static int COMINGSOON_RESTORE_WRITERS       = 1 << 12;
            
    private static int COMINGSOON_RESTORE_ALL           = (2 << 13) - 1;
    
    protected int preferredPlotLength;
    protected String scanImdb;
    protected int preferImdbMask = 0;
    protected String searchId;

    public ComingSoonPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));

        searchId = PropertiesUtil.getProperty("comingsoon.id.search", "comingsoon,yahoo");
        
        scanImdb = PropertiesUtil.getProperty("comingsoon.imdb.scan", "always"); 
        String preferImdbFor = PropertiesUtil.getProperty("comingsoon.imdb.perferredFor", "rating,runtime,country,year,company,cast,writers");
        if (preferImdbFor.length() > 0) {
            StringTokenizer st = new StringTokenizer(preferImdbFor, ",");
            
            if (st.hasMoreTokens()) {
               while (st.hasMoreTokens()) {
                   preferImdbMask |= getInfoMask(st.nextToken());
               }
            } else {
                preferImdbMask |= getInfoMask(preferImdbFor);
            }
        }
    }
    
    private int getInfoMask(String infoDescription) {
        if (infoDescription.equalsIgnoreCase("title")) {
            return COMINGSOON_RESTORE_TITLE;
        } else if (infoDescription.equalsIgnoreCase("originaltitle")){
            return COMINGSOON_RESTORE_ORIGINALTITLE;
        } else if (infoDescription.equalsIgnoreCase("plot")){
            return COMINGSOON_RESTORE_PLOT;
        } else if (infoDescription.equalsIgnoreCase("outline")){
            return COMINGSOON_RESTORE_OUTLINE;
        } else if (infoDescription.equalsIgnoreCase("rating")){
            return COMINGSOON_RESTORE_RATING;
        } else if (infoDescription.equalsIgnoreCase("runtime")){
            return COMINGSOON_RESTORE_RUNTIME;
        } else if (infoDescription.equalsIgnoreCase("country")){
            return COMINGSOON_RESTORE_COUNTRY;
        } else if (infoDescription.equalsIgnoreCase("year")){
            return COMINGSOON_RESTORE_YEAR;
        } else if (infoDescription.equalsIgnoreCase("company")){
            return COMINGSOON_RESTORE_COMPANY;
        } else if (infoDescription.equalsIgnoreCase("genres")){
            return COMINGSOON_RESTORE_GENRES;
        } else if (infoDescription.equalsIgnoreCase("cast")){
            return COMINGSOON_RESTORE_CAST;
        } else if (infoDescription.equalsIgnoreCase("directors")){
            return COMINGSOON_RESTORE_DIRECTORS;
        } else if (infoDescription.equalsIgnoreCase("writers")){
            return COMINGSOON_RESTORE_WRITERS;
        } else {
            logger.fine("ComingSoon: unknown value \"" + infoDescription + "\" for property comingsoon.perferimdbfor");
            return 0;
        }
    }

    @Override
    public boolean scan(Movie movie) {

        String comingSoonId = movie.getId(COMINGSOON_PLUGIN_ID);

        // First we try on comingsoon.it
        
        if (StringTools.isNotValidString(comingSoonId)) {
            comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
            movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
            
            if (StringTools.isNotValidString(comingSoonId)) {
                logger.finer("ComingSoon: unable to find id on first scan");
            }
        }

        // Then we use IMDB to get complete information. We back up movie infos, since ComingSoon is fetched later and will eventually need
        // to ovverride IMDB infos.
               
        Movie preImdb = backupMovieInfo(movie);

        boolean firstScanImdb = false;
        
        if (scanImdb.equalsIgnoreCase("always") || (scanImdb.equalsIgnoreCase("fallback") && (StringTools.isNotValidString(comingSoonId) || comingSoonId.equals(COMINGSOON_NOT_PRESENT)))) {
            logger.finer("ComingSoon: Checking IMDB");
            firstScanImdb = super.scan(movie);      
            if (StringTools.isNotValidString(comingSoonId) && firstScanImdb) {
                // We try to fetch again ComingSoon, hopefully with more info
                logger.finer("ComingSoon: First search on ComingSoon was KO, retrying after succesful query to IMDB");
                comingSoonId = getComingSoonId(movie.getTitle(), movie.getYear());
                movie.setId(COMINGSOON_PLUGIN_ID, comingSoonId);
            }
            
            if (StringTools.isNotValidString(comingSoonId)) {
                logger.finer("ComingSoon: unable to find id on second scan");
            }
        }
        
        Movie postImdb = backupMovieInfo(movie);

        boolean scanComingSoon = false;
        if (StringTools.isValidString(comingSoonId)) {

            logger.finer("ComingSoon: Fetching movie data from ComingSoon");
            
            // Restore pre-IMDB infos except title.
            logger.finest("ComingSoon: restoring pre-IMDB infos with override");
            restoreMovieInfo(preImdb, movie, COMINGSOON_RESTORE_ALL ^ (COMINGSOON_RESTORE_TITLE | COMINGSOON_RESTORE_ORIGINALTITLE), true);
            
            scanComingSoon = updateComingSoonMediaInfo(movie);
            
            // Fill UNKNOWN vales with post-IMDB infos
            logger.finest("ComingSoon: restoring post-IMDB infos w/o override");
            restoreMovieInfo(postImdb, movie, COMINGSOON_RESTORE_ALL ^ (COMINGSOON_RESTORE_TITLE | COMINGSOON_RESTORE_ORIGINALTITLE), false);
            
            // Replace values where IMDB is preferred
            logger.finest("ComingSoon: restoring post-IMDB infos with protected override");
            restoreMovieInfo(postImdb, movie, preferImdbMask, true, true);
        }
        
        Movie postComingSoon = backupMovieInfo(movie);

        if (!firstScanImdb && scanComingSoon && scanImdb.equalsIgnoreCase("always")) {
            // Scan was successful on ComingSoon but not on IMDB, let's try again with more info

            logger.finer("ComingSoon: First scan on IMDB KO, retrying after succesful scan on ComingSoon");
            
            // Restore pre-ComingSoon infos except title and year.
            logger.finest("ComingSoon: restoring pre-IMDB infos with override");
            restoreMovieInfo(postImdb, movie, COMINGSOON_RESTORE_ALL ^ (COMINGSOON_RESTORE_TITLE | COMINGSOON_RESTORE_ORIGINALTITLE | COMINGSOON_RESTORE_YEAR), true);
            
            // Set title to original title, more likely to be found on IMDB
            if (StringTools.isValidString(movie.getOriginalTitle())) {
                movie.setTitle(movie.getOriginalTitle());
            }            

            super.scan(movie);

            logger.finest("ComingSoon: restoring comingsoon informations where needed");
            // restoreMovieInfo(postComingSoon, movie, COMINGSOON_RESTORE_TITLE | COMINGSOON_RESTORE_ORIGINALTITLE, true);
            restoreMovieInfo(postComingSoon, movie, COMINGSOON_RESTORE_ALL ^ preferImdbMask , true, true);
           

        }
        
        if (StringTools.isNotValidString(movie.getOriginalTitle())) {
            movie.setOriginalTitle(movie.getTitle());
        }
        
        return scanComingSoon || firstScanImdb; 
    }
    
    protected String getComingSoonId(String movieName, String year) {
        return getComingSoonId(movieName, year, searchId);
    }
    
    protected String getComingSoonId(String movieName, String year, String searchIdPreference) {
        
        if (searchIdPreference.equalsIgnoreCase("comingsoon")) {
            return  getComingSoonIdFromComingSoon(movieName, year);
        } else if (searchIdPreference.equalsIgnoreCase("yahoo")) {
            return getComingSoonIdFromSearch("http://search.yahoo.com/search?vc=&p=", movieName, year);
        } else if (searchIdPreference.equalsIgnoreCase("google")) {
            return getComingSoonIdFromSearch("http://www.google.com/search?hl=it&q=", movieName, year);
        } else if (searchIdPreference.indexOf(",") > 0) {
            StringTokenizer st = new StringTokenizer(searchIdPreference, ",");
            while (st.hasMoreTokens()) {
                String id = getComingSoonId(movieName, year, st.nextToken().trim());
                if (StringTools.isValidString(id)) {
                    return id;
                }
            }
        }
        return Movie.UNKNOWN;
    }

    protected String getComingSoonIdFromSearch(String searchUrl, String movieName, String year) {
        try {
            String comingSoonId = Movie.UNKNOWN;
                        
            StringBuffer sb = new StringBuffer(searchUrl);
            sb.append("\"" + URLEncoder.encode(movieName, "UTF-8") + "\"");              

            sb.append("+site%3Acomingsoon.it");

            logger.finer("ComingSoon: Fetching ComingSoon search URL: " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            
           
            int beginIndex = xml.indexOf(COMINGSOON_BASE_URL + COMINGSOON_SEARCH_URL);
            if (beginIndex > 0) {
                comingSoonId = getComingSoonIdFromURL(xml.substring(beginIndex, xml.indexOf('"', beginIndex)));
                logger.finer("ComingSoon: Found ComingSoon ID: " + comingSoonId);
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
            sb.append(URLEncoder.encode(movieName, "iso-8859-1"));              

            if (StringTools.isValidString(year)) {
                sb.append("&anno=" + year);
            }
            
            int searchPage = 0;
            
            while (searchPage++ < COMINGSOON_MAX_SEARCH_PAGES) {
                
                StringBuffer sbPage = new StringBuffer(sb);
                if (searchPage > 1) {
                    sbPage.append("&p="+searchPage);
                }

                logger.finer("ComingSoon: Fetching ComingSoon search URL: " + sbPage.toString());
                String xml = webBrowser.request(sbPage.toString(), Charset.forName("iso-8859-1"));
                
                ArrayList<String[]> movieList = parseComingSoonSearchResults(xml);
                
                if (movieList.size() > 0) {
                
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
                                searchPage = COMINGSOON_MAX_SEARCH_PAGES; //End loop
                            } else {
                                logger.finest("ComingSoon: Found a match for: " + lTitle + ", " + lOrig + ", difference " + difference);
                            }
                            comingSoonId = lId;
                            scoreToBeat = difference;
                        }
                    }
                } else {
                    break;
                }
            }
            
            if (StringTools.isValidString(year) && scoreToBeat > 0) {
                logger.finer("ComingSoon: Perfect match not found, trying removing by year...");
                String newComingSoonId = getComingSoonIdFromComingSoon(movieName, Movie.UNKNOWN, scoreToBeat);
                comingSoonId = (StringTools.isNotValidString(newComingSoonId) ? comingSoonId : newComingSoonId);
            }
            
            if (StringTools.isValidString(comingSoonId)) {
                logger.finer("ComingSoon: Found ComingSoon ID: " + comingSoonId);
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
        
        /* Search results end with "Trovati NNN Film" (found NNN movies). 
           After this string, more movie URL are found, so we have to set a boundary
        */

        ArrayList<String[]> listaFilm = new ArrayList<String[]>();
        int trovatiIndex = xml.indexOf("Trovati");
        int moviesFound = -1;
        
        while (trovatiIndex >= 0 && moviesFound < 0) {
            int filmIndex = xml.indexOf("Film", trovatiIndex);
            if (filmIndex - trovatiIndex < 15 && filmIndex > 0) {
                moviesFound = Integer.parseInt(xml.substring(trovatiIndex + 8, filmIndex - 1));
            } else {
                trovatiIndex = xml.indexOf("Trovati", trovatiIndex + 1);
            }
        }
        
        if (moviesFound < 0) {
            logger.severe("ComingSoon: couldn't find 'Trovati NNN Film' string. Search page layout probably changed");
            return listaFilm;
        } else {
            logger.finest("ComingSoon: search found " + moviesFound + " movies");
        }
        
        if (moviesFound == 0) {
            return listaFilm;
        }
        
        int beginIndex = xml.indexOf("<div id=\"BoxFilm\">");
        
        while (beginIndex >= 0 && beginIndex < trovatiIndex) {
            int urlIndex = xml.indexOf(COMINGSOON_SEARCH_URL, beginIndex);
            logger.finest("ComingSoon: Found movie URL " + xml.substring(urlIndex, xml.indexOf('"', urlIndex)));
            String comingSoonId = getComingSoonIdFromURL (xml.substring(urlIndex, xml.indexOf('"', urlIndex)));
            
            int nextIndex = xml.indexOf("<div id=\"BoxFilm\">", beginIndex + 1);
            
            String search;
            if (nextIndex > 0 && nextIndex < trovatiIndex) {
                search = xml.substring(beginIndex, nextIndex);
            } else {
                search = xml.substring(beginIndex, trovatiIndex);
            }

            String title = HTMLTools.extractTag(search, "class=\"titoloFilm\"", 0, "<>", false).trim();
            String originalTitle = HTMLTools.extractTag(search, "class=\"titoloFilm2\">", "<").trim();
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
        if (StringTools.isNotValidString(returnedTitle)) {
            return COMINGSOON_MAX_DIFF;
        }
        
        logger.finest("ComingSoon: Comparing " + searchedTitle + " and " + returnedTitle);
        
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
            String xml = webBrowser.request(movieURL, Charset.forName("iso-8859-1"));
            
            // TITLE & ORIGINAL TITLE
            
            if (!movie.isOverrideTitle()) {
                String title = HTMLTools.extractTag(xml, "<h1 class='titoloFilm'", 0, "<>", false).trim();
                String originalTitle = HTMLTools.extractTag(xml, "<h1 class='titoloFilm2'", 0, "<>", false).trim();
                if (StringTools.isNotValidString(originalTitle)) {
                    // Comingsoon layout slightly changed at some point and original title became h2
                    originalTitle = HTMLTools.extractTag(xml, "<h2 class='titoloFilm2'", 0, "<>", false).trim();
                }
                if (originalTitle.startsWith("(")) {
                    originalTitle = originalTitle.substring(1, originalTitle.length() - 1).trim();
                }
                
                if (StringTools.isNotValidString(title)) {
                    logger.severe("ComingSoon: No title found at ComingSoon page. HTML layout has changed?");
                    return false;
                }
                
                title = correctCapsTitle(title);

                if (StringTools.isValidString(originalTitle)) {
                    originalTitle = correctCapsTitle(originalTitle);
                }
                
                movie.setTitle(title);
                movie.setOriginalTitle(originalTitle);
            }
            
            // RATING
            
            if (movie.getRating() == -1) {
                String rating = HTMLTools.extractTag(xml, "<li class=\"current-rating\"", 1, "<>", false).trim();
                logger.finest("ComingSoon: found rating " + rating);
                if (StringTools.isValidString(rating)) {
                    rating = rating.substring(rating.indexOf(" ") + 1, rating.indexOf("/"));
                    int ratingInt = (int) (Float.parseFloat(rating.replace(',','.')) * 20); // Rating is 0 to 5, we normalize to 100
                    if (ratingInt > 0) {
                        movie.setRating(ratingInt);
                    }
                }
            }
            
            // RELEASE DATE
            
            if (StringTools.isNotValidString(movie.getReleaseDate())) {
                String releaseDate = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">USCITA CINEMA: ", "<br />"));
                movie.setReleaseDate(releaseDate);
            }
            
            // RUNTIME
            
            if (StringTools.isNotValidString(movie.getRuntime())) {
                String runTime = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">DURATA: ", "<br />"));
                if (StringTools.isValidString(runTime)) {
                    StringTokenizer st = new StringTokenizer(runTime);
                    movie.setRuntime(st.nextToken());
                }

            }
            
            // COUNTRY AND YEAR
            
            String countryYear = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PAESE: ", "<br />"));
            String country = Movie.UNKNOWN;
            String year = Movie.UNKNOWN;
            
            if (StringTools.isValidString(countryYear)) {

                year = countryYear.substring(countryYear.length() - 4, countryYear.length());
                StringTokenizer st = new StringTokenizer(countryYear.substring(0, countryYear.length() - 5), ",");
                // Last country seems to be the more appropriate
                while (st.hasMoreTokens()) {
                    country = st.nextToken().trim();
                }
                

                logger.finest("ComingSoon: found countryYear " + countryYear + ", country " + country + ", year " + year);
                
                if (!movie.isOverrideYear() && Integer.parseInt(year) > 1900) {
                    movie.setYear(year);
                }
                
                if (StringTools.isNotValidString(movie.getCountry()) && country.length() > 0 && StringTools.isValidString(country)) {
                    movie.setCountry(country);
                }
            }

            // COMPANY
            if (StringTools.isNotValidString(movie.getCompany())) {
                // TODO: Add more than one company when available in Movie model
                String companies = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">PRODUZIONE: ", "<br />"));
                StringTokenizer st = new StringTokenizer(companies, ",");
                if (st.hasMoreTokens()) {
                    movie.setCompany(st.nextToken().trim());
                } else {
                    movie.setCompany(companies.trim());
                }
                
            }
            
            // GENRES
            
            if (movie.getGenres().isEmpty()) {
            
                String genreList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">GENERE: ", "<br />"));
                if (StringTools.isValidString(genreList)) {
                    Collection<String> genres = new ArrayList<String>();
                    
                    StringTokenizer st = new StringTokenizer(genreList, ",");
                    while (st.hasMoreTokens()) {
                        genres.add(st.nextToken().trim());
                    }
                    movie.setGenres(genres);
                }
            }

            // PLOT AND OUTLINE
            
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
                    outline = xmlPlot;
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

            // CAST
            
            if (movie.getCast().isEmpty()) {
                String castList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">ATTORI: ", "Ruoli ed Interpreti"));
                
                if (castList.contains(",")) {
                    StringTokenizer st = new StringTokenizer(castList,",");
                    while (st.hasMoreTokens()) {
                        movie.addActor(st.nextToken());
                    }
                } else {
                    movie.addActor(castList);
                }
            }
            
            // DIRECTOR(S)
            
            if (movie.getDirectors().isEmpty()) {
                String directorList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">REGIA: ", "<br />"));
                
                if (directorList.contains(",")) {
                    StringTokenizer st = new StringTokenizer(directorList,",");
                    while (st.hasMoreTokens()) {
                        movie.addDirector(st.nextToken());
                    }
                } else {
                    movie.addDirector(directorList);
                }
            }

            // WRITER(S)
            
            if (movie.getWriters().isEmpty()) {
                String writerList = HTMLTools.stripTags(HTMLTools.extractTag(xml, ">SCENEGGIATURA: ", "<br />"));
                
                if (writerList.contains(",")) {
                    StringTokenizer st = new StringTokenizer(writerList,",");
                    while (st.hasMoreTokens()) {
                        movie.addWriter(st.nextToken());
                    }
                } else {
                    movie.addWriter(writerList);
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
    
    private Movie backupMovieInfo(Movie originalMovie) {
        Movie backup = new Movie();
        
        backup.setTitle(originalMovie.getTitle());
        backup.setOriginalTitle(originalMovie.getOriginalTitle());
        backup.setPlot(originalMovie.getPlot());
        backup.setOutline(originalMovie.getOutline());
        backup.setRating(originalMovie.getRating());
        backup.setRuntime(originalMovie.getRuntime());
        backup.setCountry(originalMovie.getCountry());
        backup.setYear(originalMovie.getYear());
        backup.setCompany(originalMovie.getCompany());
        backup.setGenres(originalMovie.getGenres());
        backup.setCast(originalMovie.getCast());
        backup.setDirectors(originalMovie.getDirectors());
        backup.setWriters(originalMovie.getWriters());
        
        return backup;
    }
    
    private void restoreMovieInfo(Movie sourceMovie, Movie targetMovie, int what, boolean override) {
        restoreMovieInfo(sourceMovie, targetMovie, what, override, false);
    }
    
    private void restoreMovieInfo(Movie sourceMovie, Movie targetMovie, int what, boolean override, boolean protect) {
        
        if ((what & COMINGSOON_RESTORE_TITLE) > 0 && (override || StringTools.isNotValidString(targetMovie.getTitle()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getTitle())) {
                logger.finest("ComingSoon: restoring title");
                targetMovie.setTitle(sourceMovie.getTitle());
            }
        }
        if ((what & COMINGSOON_RESTORE_ORIGINALTITLE) > 0 && (override || StringTools.isNotValidString(targetMovie.getOriginalTitle()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getOriginalTitle())) {
                logger.finest("ComingSoon: restoring original title");
                targetMovie.setOriginalTitle(sourceMovie.getOriginalTitle());
            }
        }
        if ((what & COMINGSOON_RESTORE_PLOT) > 0 && (override || StringTools.isNotValidString(targetMovie.getPlot()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getPlot())) {
                logger.finest("ComingSoon: restoring plot");
                targetMovie.setPlot(sourceMovie.getPlot());
            }
        }
        if ((what & COMINGSOON_RESTORE_OUTLINE) > 0 && (override || StringTools.isNotValidString(targetMovie.getOutline()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getOutline())) {
                logger.finest("ComingSoon: restoring outline");
                targetMovie.setOutline(sourceMovie.getOutline());
            }
        }
        if ((what & COMINGSOON_RESTORE_RATING) > 0 && (override || targetMovie.getRating() < 0)) {
            if (!protect || sourceMovie.getRating() >= 0) {
                logger.finest("ComingSoon: restoring rating");
                targetMovie.setRating(sourceMovie.getRating());
            }
        }
        if ((what & COMINGSOON_RESTORE_RUNTIME) > 0 && (override || StringTools.isNotValidString(targetMovie.getRuntime()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getRuntime())) {
                logger.finest("ComingSoon: restoring runtime");
                targetMovie.setRuntime(sourceMovie.getRuntime());
            }
        }
        if ((what & COMINGSOON_RESTORE_COUNTRY) > 0 && (override || StringTools.isNotValidString(targetMovie.getCountry()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getCountry())) {
                logger.finest("ComingSoon: restoring country");
                targetMovie.setCountry(sourceMovie.getCountry());
            }
        }
        if ((what & COMINGSOON_RESTORE_YEAR) > 0 && (override || StringTools.isNotValidString(targetMovie.getYear()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getYear())) {
                logger.finest("ComingSoon: restoring year");
                targetMovie.setYear(sourceMovie.getYear());
            }
        }
        if ((what & COMINGSOON_RESTORE_COMPANY) > 0 && (override || StringTools.isNotValidString(targetMovie.getCompany()))) {
            if (!protect || StringTools.isValidString(sourceMovie.getCompany())) {
                logger.finest("ComingSoon: restoring company");
                targetMovie.setCompany(sourceMovie.getCompany());
            }
        }
        if ((what & COMINGSOON_RESTORE_GENRES) > 0 && (override || targetMovie.getGenres().isEmpty())) {
            if (!protect || !sourceMovie.getGenres().isEmpty()) {
                logger.finest("ComingSoon: restoring genres");
                targetMovie.setGenres(sourceMovie.getGenres());
            }
        }
        if ((what & COMINGSOON_RESTORE_CAST) > 0 && (override || targetMovie.getCast().isEmpty())) {
            if (!protect || !sourceMovie.getCast().isEmpty()) {
                logger.finest("ComingSoon: restoring cast");
                targetMovie.setCast(sourceMovie.getCast());
            }
        }
        if ((what & COMINGSOON_RESTORE_DIRECTORS) > 0 && (override || targetMovie.getDirectors().isEmpty())) {
            if (!protect || !sourceMovie.getDirectors().isEmpty()) {
                logger.finest("ComingSoon: restoring directors");
                targetMovie.setDirectors(sourceMovie.getDirectors());
            }
        }
        if ((what & COMINGSOON_RESTORE_WRITERS) > 0 && (override || targetMovie.getWriters().isEmpty())) {
            if (!protect || !sourceMovie.getWriters().isEmpty()) {
                logger.finest("ComingSoon: restoring writers");
                targetMovie.setWriters(sourceMovie.getWriters());
            }
        }
    }

}
