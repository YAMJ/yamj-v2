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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class SratimPlugin extends ImdbPlugin {

    public static String SRATIM_PLUGIN_ID = "sratim";
    public static String SRATIM_PLUGIN_SUBTITLE_ID = "sratim_subtitle";

    private static AbstractStringMetric metric = new MongeElkan();
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*sratim.co.il[^\\s<>`\"\\[\\]]*");
    private static String[] genereStringEnglish = { "Action", "Adult", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama",
                    "Family", "Fantasy", "Film-Noir", "Game-Show", "History", "Horror", "Music", "Musical", "Mystery", "News", "Reality-TV", "Romance",
                    "Sci-Fi", "Short", "Sport", "Talk-Show", "Thriller", "War", "Western" };
    private static String[] genereStringHebrew = { "הלועפ", "םירגובמ", "תואקתפרה", "היצמינא", "היפרגויב", "הידמוק", "עשפ", "ידועית", "המרד", "החפשמ", "היזטנפ",
                    "לפא", "ןועושעש", "הירוטסיה", "המיא", "הקיזומ", "רמזחמ", "ןירותסימ", "תושדח", "יטילאיר", "הקיטנמור", "ינוידב עדמ", "רצק", "טרופס", "חוריא",
                    "חתמ", "המחלמ", "ןוברעמ" };
    private static String cookieHeader = "";

    private static boolean subtitleDownload = false;
    private static String login = "";
    @SuppressWarnings("unused")
    private static String pass = "";
    @SuppressWarnings("unused")
    private static String code = "";

    protected int plotLineMaxChar;
    protected int plotLineMax;
    protected TheTvDBPlugin tvdb;
    protected static String preferredPosterSearchEngine;
    protected static String lineBreak;

    public SratimPlugin() {
        super(); // use IMDB if sratim doesn't know movie

        tvdb = new TheTvDBPlugin(); // use TVDB if sratim doesn't know series

        plotLineMaxChar = Integer.parseInt(PropertiesUtil.getProperty("sratim.plotLineMaxChar", "50"));
        plotLineMax = Integer.parseInt(PropertiesUtil.getProperty("sratim.plotLineMax", "2"));

        subtitleDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("sratim.subtitle", "false"));
        login = PropertiesUtil.getProperty("sratim.username", "");
        pass = PropertiesUtil.getProperty("sratim.password", "");
        code = PropertiesUtil.getProperty("sratim.code", "");
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");

        lineBreak = PropertiesUtil.getProperty("mjb.lineBreak", "{br}");
        
        if (subtitleDownload == true && !login.equals("")) {
            loadSratimCookie();
        }
    }

    public boolean scan(Movie mediaFile) {

        boolean retval = true;

        String sratimUrl = mediaFile.getId(SRATIM_PLUGIN_ID);
        if (sratimUrl == null || sratimUrl.equalsIgnoreCase(Movie.UNKNOWN)) {

            // collect missing information from IMDB or TVDB before sratim
            if (!mediaFile.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                retval = super.scan(mediaFile);
            } else {
                retval = tvdb.scan(mediaFile);
            }

            translateGenres(mediaFile);

            sratimUrl = getSratimUrl(mediaFile, mediaFile.getTitle(), mediaFile.getYear());
        }

        if (!sratimUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
            retval = updateMediaInfo(mediaFile);
        }

        return retval;
    }

    /**
     * retrieve the sratim url matching the specified movie name and year.
     */
    public String getSratimUrl(Movie mediaFile, String movieName, String year) {

        try {
            String imdbId = updateImdbId(mediaFile);
            if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
                return Movie.UNKNOWN;
            }

            String sratimUrl;

            String xml = webBrowser.request("http://www.sratim.co.il/browse.php?q=imdb%3A" + imdbId, Charset.forName("UTF-8"));

            String detailsUrl = HTMLTools.extractTag(xml, "<a href=\"view.php?", 0, "\"");
            String subtitlesID = HTMLTools.extractTag(xml, "<a href=\"subtitles.php?", 0, "\"");

            if (detailsUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
                return Movie.UNKNOWN;
            }

            //update movie ids
            int id = detailsUrl.lastIndexOf("id=");
            if(id>-1 && detailsUrl.length()>id) {
                String movieId = detailsUrl.substring(id+3);
                mediaFile.setId(SRATIM_PLUGIN_ID, movieId);
            }
            int subid = subtitlesID.lastIndexOf("mid=");
            if(subid>-1 && subtitlesID.length()>subid) {
                String subtitle = subtitlesID.substring(subid+4);
                mediaFile.setId(SRATIM_PLUGIN_SUBTITLE_ID, subtitle);
            }

            sratimUrl = "http://www.sratim.co.il/view.php?" + detailsUrl;

            return sratimUrl;

        } catch (Exception error) {
            logger.severe("Sratim Plugin: Failed retreiving sratim informations for movie : " + movieName);
            logger.severe("Sratim Plugin: " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    // Translate IMDB genres to hebrew
    protected void translateGenres(Movie movie) {
        TreeSet<String> genresHeb = new TreeSet<String>();

        // Translate genres to hebrew
        for (String genre : movie.getGenres()) {

            int i;
            for (i = 0; i < genereStringEnglish.length; i++) {
                if (genre.equals(genereStringEnglish[i])) {
                    break;
                }
            }

            if (i < genereStringEnglish.length) {
                genresHeb.add(genereStringHebrew[i]);
            } else {
                genresHeb.add("רחא");
            }
        }

        // Set translated IMDB genres
        movie.setGenres(genresHeb);
    }

    // Porting from my old code in c++
    public static final int BCT_L = 0;
    public static final int BCT_R = 1;
    public static final int BCT_N = 2;
    public static final int BCT_EN = 3;
    public static final int BCT_ES = 4;
    public static final int BCT_ET = 5;
    public static final int BCT_CS = 6;

    // Return the type of a specific charcter
    private static int getCharType(char charToCheck) {
        if (((charToCheck >= 'א') && (charToCheck <= 'ת'))) {
            return BCT_R;
        }

        if ((charToCheck == 0x26) || (charToCheck == 0x40) || ((charToCheck >= 0x41) && (charToCheck <= 0x5A)) || 
           ((charToCheck >= 0x61) && (charToCheck <= 0x7A)) || ((charToCheck >= 0xC0) && (charToCheck <= 0xD6))
                        || ((charToCheck >= 0xD8) && (charToCheck <= 0xDF))) {
            return BCT_L;
        }

        if (((charToCheck >= 0x30) && (charToCheck <= 0x39))) {
            return BCT_EN;
        }

        if ((charToCheck == 0x2E) || (charToCheck == 0x2F)) {
            return BCT_ES;
        }

        if ((charToCheck == 0x23) || (charToCheck == 0x24) || ((charToCheck >= 0xA2) && (charToCheck <= 0xA5)) || 
                        (charToCheck == 0x25) || (charToCheck == 0x2B) || (charToCheck == 0x2D) || (charToCheck == 0xB0) || (charToCheck == 0xB1)) {
            return BCT_ET;
        }

        if ((charToCheck == 0x2C) || (charToCheck == 0x3A)) {
            return BCT_CS;
        }

        // Default Natural
        return BCT_N;
    }

    // Rotate a specific part of a string
    private static void rotateString(char[] stringToRotate, int startPos, int endPos) {
        int currentPos;
        char tempChar;

        for (currentPos = 0; currentPos < (endPos - startPos + 1) / 2; currentPos++) {
            tempChar = stringToRotate[startPos + currentPos];

            stringToRotate[startPos + currentPos] = stringToRotate[endPos - currentPos];

            stringToRotate[endPos - currentPos] = tempChar;
        }

    }

    // Set the string char types
    private static void setStringCharType(char[] stringToSet, int[] charType) {
        int currentPos;

        currentPos = 0;

        while (currentPos < stringToSet.length) {
            charType[currentPos] = getCharType(stringToSet[currentPos]);

            // Fix "(" and ")"
            if (stringToSet[currentPos] == ')') {
                stringToSet[currentPos] = '(';
            } else if (stringToSet[currentPos] == '(') {
                stringToSet[currentPos] = ')';
            }

            currentPos++;
        }

    }

    // Resolving Weak Types
    private static void resolveWeakType(char[] stringType, int[] charType) {
        int pos = 0;

        while (pos < stringType.length) {
            // Check that we have at least 3 chars
            if (stringType.length - pos >= 3) {
                if ((charType[pos] == BCT_EN) && (charType[pos + 2] == BCT_EN) && ((charType[pos + 1] == BCT_ES) || (charType[pos + 1] == BCT_CS))) // Change
                // the char
                // type
                {
                    charType[pos + 1] = BCT_EN;
                }
            }

            if (stringType.length - pos >= 2) {
                if ((charType[pos] == BCT_EN) && (charType[pos + 1] == BCT_ET)) // Change the char type
                {
                    charType[pos + 1] = BCT_EN;
                }

                if ((charType[pos] == BCT_ET) && (charType[pos + 1] == BCT_EN)) // Change the char type
                {
                    charType[pos] = BCT_EN;
                }
            }

            // Default change all the terminators to natural
            if ((charType[pos] == BCT_ES) || (charType[pos] == BCT_ET) || (charType[pos] == BCT_CS)) {
                charType[pos] = BCT_N;
            }

            pos++;
        }

        /*
         * - European Numbers (FOR ES,ET,CS)
         * 
         * EN,ES,EN -> EN,EN,EN EN,CS,EN -> EN,EN,EN
         * 
         * EN,ET -> EN,EN ET,EN -> EN,EN ->>>>> ET=EN
         * 
         * 
         * else for ES,ET,CS (??)
         * 
         * L,??,EN -> L,N,EN
         */
    }

    // Resolving Natural Types
    private static void resolveNaturalType(char[] stringToResolve, int[] charType, int defaultDirection) {
        int Pos, CheckPos;
        int Before, After;

        Pos = 0;

        while (Pos < stringToResolve.length) {
            // Check if this is natural type and we need to cahnge it
            if (charType[Pos] == BCT_N) {
                // Search for the type of the previous strong type
                CheckPos = Pos - 1;

                while (true) {
                    if (CheckPos < 0) {
                        // Default language
                        Before = defaultDirection;
                        break;
                    }

                    if (charType[CheckPos] == BCT_R) {
                        Before = BCT_R;
                        break;
                    }

                    if (charType[CheckPos] == BCT_L) {
                        Before = BCT_L;
                        break;
                    }

                    CheckPos--;
                }

                CheckPos = Pos + 1;

                // Search for the type of the next strong type
                while (true) {
                    if (CheckPos >= stringToResolve.length) {
                        // Default language
                        After = defaultDirection;
                        break;
                    }

                    if (charType[CheckPos] == BCT_R) {
                        After = BCT_R;
                        break;
                    }

                    if (charType[CheckPos] == BCT_L) {
                        After = BCT_L;
                        break;
                    }

                    CheckPos++;
                }

                // Change the natural depanded on the strong type before and after
                if ((Before == BCT_R) && (After == BCT_R)) {
                    charType[Pos] = BCT_R;
                } else if ((Before == BCT_L) && (After == BCT_L)) {
                    charType[Pos] = BCT_L;
                } else {
                    charType[Pos] = defaultDirection;
                }
            }

            Pos++;
        }

        /*
         * R N R -> R R R L N L -> L L L
         * 
         * L N R -> L e R (e=default) R N L -> R e L (e=default)
         */
    }

    // Resolving Implicit Levels
    private static void resolveImplictLevels(char[] stringToResolve, int[] charType, int[] level) {
        int Pos;

        Pos = 0;

        while (Pos < stringToResolve.length) {
            if (charType[Pos] == BCT_L) {
                level[Pos] = 2;
            }

            if (charType[Pos] == BCT_R) {
                level[Pos] = 1;
            }

            if (charType[Pos] == BCT_EN) {
                level[Pos] = 2;
            }

            Pos++;
        }
    }

    // Reordering Resolved Levels
    private static void reorderResolvedLevels(char[] stringToLevel, int[] level) {
        int count;
        int startPos, endPos, currentPos;

        for (count = 2; count >= 1; count--) {
            currentPos = 0;

            while (currentPos < stringToLevel.length) {
                // Check if this is the level start
                if (level[currentPos] >= count) {
                    startPos = currentPos;

                    // Search for the end
                    while ((currentPos + 1 != stringToLevel.length) && (level[currentPos + 1] >= count)) {
                        currentPos++;
                    }

                    endPos = currentPos;

                    rotateString(stringToLevel, startPos, endPos);
                }

                currentPos++;
            }
        }
    }

    // Convert logical string to visual
    private static void logicalToVisual(char[] stringToConvert, int defaultDirection) {
        int[] CharType;
        int[] Level;

        int Len;

        Len = stringToConvert.length;

        // Allocate CharType and Level arrays
        CharType = new int[Len];

        Level = new int[Len];

        // Set the string char types
        setStringCharType(stringToConvert, CharType);

        // Resolving Weak Types
        resolveWeakType(stringToConvert, CharType);

        // Resolving Natural Types
        resolveNaturalType(stringToConvert, CharType, defaultDirection);

        // Resolving Implicit Levels
        resolveImplictLevels(stringToConvert, CharType, Level);

        // Reordering Resolved Levels
        reorderResolvedLevels(stringToConvert, Level);
    }

    private static boolean isCharNatural(char c) {
        if ((c == ' ') || (c == '-')) {
            return true;
        }

        return false;
    }

    private static String logicalToVisual(String text) {
        char[] ret;

        ret = text.toCharArray();
        if (containsHebrew(ret)) {
            logicalToVisual(ret, BCT_R);
        }
        return (new String(ret));
    }

    private static ArrayList<String> logicalToVisual(ArrayList<String> text) {
        ArrayList<String> ret = new ArrayList<String>();

        for (int i = 0; i < text.size(); i++) {
            ret.add(logicalToVisual(text.get(i)));

        }

        return ret;
    }

    private static String removeTrailDot(String text) {
        int dot = text.lastIndexOf(".");

        if (dot == -1) {
            return text;
        }

        return text.substring(0, dot);
    }

    private static String removeTrailBracket(String text) {
        int bracket = text.lastIndexOf(" (");

        if (bracket == -1) {
            return text;
        }

        return text.substring(0, bracket);
    }

    private static String breakLongLines(String text, int lineMaxChar, int lineMax) {
        String ret = new String();

        int scanPos = 0;
        int lastBreakPos = 0;
        int lineStart = 0;
        int lineCount = 0;

        while (scanPos < text.length()) {
            if (isCharNatural(text.charAt(scanPos))) {
                lastBreakPos = scanPos;
            }

            if (scanPos - lineStart > lineMaxChar) {
                // Check if no break position found
                if (lastBreakPos == 0) // Hard break on this location
                {
                    lastBreakPos = scanPos;
                }

                lineCount++;
                if (lineCount == lineMax) {
                    return ret = ret + "..." + logicalToVisual(text.substring(lineStart, lastBreakPos).trim());
                }

                ret = ret + logicalToVisual(text.substring(lineStart, lastBreakPos).trim());

                lineStart = lastBreakPos;
                lastBreakPos = 0;

                ret = ret + lineBreak;
            }

            scanPos++;
        }

        ret = ret + logicalToVisual(text.substring(lineStart, scanPos).trim());

        return ret;
    }

    protected String extractTag(String src, String tagStart, String tagEnd) {
        int beginIndex = src.indexOf(tagStart);
        if (beginIndex < 0) {
            // logger.finest("extractTag value= Unknown");
            return Movie.UNKNOWN;
        }
        try {
            String subString = src.substring(beginIndex + tagStart.length());
            int endIndex = subString.indexOf(tagEnd);
            if (endIndex < 0) {
                // logger.finest("extractTag value= Unknown");
                return Movie.UNKNOWN;
            }
            subString = subString.substring(0, endIndex);

            String value = HTMLTools.decodeHtml(subString.trim());
            // logger.finest("extractTag value=" + value);
            return value;
        } catch (Exception error) {
            logger.severe("Sratim Plugin: extractTag an exception occurred during tag extraction : " + error);
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        return src.replaceAll("\\<.*?>", "");
    }

    protected ArrayList<String> removeHtmlTags(ArrayList<String> src) {
        ArrayList<String> output = new ArrayList<String>();

        for (int i = 0; i < src.size(); i++) {
            output.add(removeHtmlTags(src.get(i)));
        }
        return output;
    }

    /**
     * Scan Sratim html page for the specified movie
     */
    protected boolean updateMediaInfo(Movie movie) {
        try {

            String sratimUrl = "http://www.sratim.co.il/view.php?id=" + movie.getId(SRATIM_PLUGIN_ID);

            String xml = webBrowser.request(sratimUrl, Charset.forName("UTF-8"));

            if (xml.contains("צפייה בפרופיל סדרה")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                }
            }

            if (!movie.isOverrideTitle()) {
                String title = extractMovieTitle(xml);

                movie.setTitle(logicalToVisual(title));
                movie.setTitleSort(title);
            }

            // Prefer IMDB rating
            if (movie.getRating() == -1) {
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "width=\"120\" height=\"12\" title=\"", 0, " ")));
            }

            movie.addDirector(logicalToVisual(HTMLTools.getTextAfterElem(xml, "בימוי:")));

            movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "י' בעולם:"));
            // Issue 1176 - Prevent lost of NFO Data
            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(logicalToVisual(removeTrailDot(HTMLTools.getTextAfterElem(xml, "אורך זמן:"))));
            }
            movie.setCountry(logicalToVisual(HTMLTools.getTextAfterElem(xml, "מדינה:")));

            // Prefer IMDB genres
            if (movie.getGenres().isEmpty()) {
                String genres = HTMLTools.getTextAfterElem(xml, "ז'אנרים:");
                if (!Movie.UNKNOWN.equals(genres)) {
                    for (String genre : genres.split(" *, *")) {
                        movie.addGenre(logicalToVisual(Library.getIndexingGenre(genre)));
                    }
                }
            }

            String tmpPlot = removeHtmlTags(extractTag(xml, "<div style=\"font-size:14px;text-align:justify;\">", "</div>"));

            movie.setPlot(breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax));

            if (!movie.isOverrideYear()) {
            	if (!movie.isTVShow()) {
                    movie.setYear(HTMLTools.getTextAfterElem(xml, "<td class=\"prod_year\" style=\"padding-left:10px;\">"));
                }
            }

            movie.setCast(logicalToVisual(removeHtmlTags(HTMLTools.extractTags(xml, "שחקנים:", "</tr>", "<a href", "</a>"))));

            if (movie.isTVShow()) {
                updateTVShowInfo(movie, xml);
            } else {
                // Download subtitle from the page
                downloadSubtitle(movie, movie.getFirstFile());
            }

        } catch (Exception error) {
            logger.severe("Sratim Plugin: Failed retrieving information for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }

    private int parseRating(String rating) {
        try {
            return Math.round(Float.parseFloat(rating.replace(",", "."))) * 10;
        } catch (Exception error) {
            return -1;
        }
    }

    private String updateImdbId(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }
        return imdbId;
    }

    public void scanTVShowTitles(Movie movie) {
        scanTVShowTitles(movie, null);
    }

    public void scanTVShowTitles(Movie movie, String mainXML) {
        if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
            return;
        }

        String seasonXML = null;
        
        try {
            if (mainXML == null) {
                String sratimId = movie.getId(SRATIM_PLUGIN_ID);
            	mainXML = webBrowser.request("http://www.sratim.co.il/view.php?id=" + sratimId, Charset.forName("UTF-8"));
            }

            int season = movie.getSeason();

            int index = 0;
            int endIndex = 0;
            
            String seasonUrl;
            String seasonYear;

            // Find the season URL
            while (true) {
                index = mainXML.indexOf("<span class=\"smtext\"><a href=\"", index);
                if (index == -1) {
                	return;
                }
                
                index += 30;
                
                endIndex = mainXML.indexOf("\"", index);
                if (endIndex == -1) {
                	return;
                }
                
                String scanUrl = mainXML.substring(index, endIndex);

                
                index = mainXML.indexOf("class=\"smtext\">עונה ", index);
                if (index == -1) {
                	return;
                }
                
                index += 20;
                
                endIndex = mainXML.indexOf("</a>", index);
                if (endIndex == -1) {
                    return;
                }
                
                String scanSeason = mainXML.substring(index, endIndex);

                
                index = mainXML.indexOf("class=\"smtext\">", index);
                if (index == -1) {
                	return;
                }
                
                index += 15;
                
                endIndex = mainXML.indexOf("<", index);
                if (endIndex == -1) {
                    return;
                }
                
                String scanYear = mainXML.substring(index, endIndex);
                
                
                int scanSeasontInt = 0;
                try {
                	scanSeasontInt = Integer.parseInt(scanSeason);
                } catch (Exception error) {
                	scanSeasontInt = 0;
                }

                if (scanSeasontInt == season) {
                	seasonYear = scanYear;
                	seasonUrl = "http://www.sratim.co.il/" + HTMLTools.decodeHtml(scanUrl);
                	break;
                }
            }

            
            if (!movie.isOverrideYear()) {
            	movie.setYear(seasonYear);
            }
            
        	seasonXML = webBrowser.request(seasonUrl, Charset.forName("UTF-8"));
            

        } catch (Exception error) {
            logger.severe("Sratim Plugin: Failed retreiving information for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            if (mainXML == null) {
                return;
            }
        }

        
        for (MovieFile file : movie.getMovieFiles()) {
            if (!file.isNewFile()) {
                // don't scan episode title if it exists in XML data
                continue;
            }
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int part = file.getFirstPart(); part <= file.getLastPart(); ++part) {

                int index = 0;
                int endIndex = 0;

                // Go over the page and sacn for episode links
                while (true) {
				    index = seasonXML.indexOf("<td style=\"padding-right:6px;font-size:15px;\"><a href=\"", index);
				    if (index == -1) {
				    	return;
				    }
				    
				    index += 55;
				    
				    endIndex = seasonXML.indexOf("\"", index);
				    if (endIndex == -1) {
				    	return;
				    }
				    
				    String scanUrl = seasonXML.substring(index, endIndex);
				
				    
				    index = seasonXML.indexOf("<b>פרק ", index);
				    if (index == -1) {
				    	return;
				    }
				    
				    index += 7;
				    
				    endIndex = seasonXML.indexOf(":", index);
				    if (endIndex == -1) {
				        return;
				    }
				    
				    String scanPart = seasonXML.substring(index, endIndex);

				    
				    index = seasonXML.indexOf("</b> ", index);
				    if (index == -1) {
				    	return;
				    }
				    
				    index += 5;
				    
				    endIndex = seasonXML.indexOf("</a>", index);
				    if (endIndex == -1) {
				        return;
				    }
				    
				    String scanName = seasonXML.substring(index, endIndex);
				    

				    if (scanPart.equals(Integer.toString(part))) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" / ");
                        }
                        sb.append(logicalToVisual(HTMLTools.decodeHtml(scanName)));

                        try {
                        	String episodeUrl = "http://www.sratim.co.il/" + HTMLTools.decodeHtml(scanUrl);
                        	
                            // Get the episode page url
                            String xml = webBrowser.request(episodeUrl, Charset.forName("UTF-8"));

                            // Update Plot
                            // TODO Be sure this is enough to go straight to the plot ...
                            String plotStart = "<div style=\"font-size:14px;text-align:justify;\">";
                            int plotStartIndex = xml.indexOf(plotStart);
                            if (plotStartIndex > -1) {
                                int endPlotIndex = xml.indexOf("</div>", plotStartIndex + plotStart.length());
                                if (endPlotIndex > -1) {
                                    String tmpPlot = removeHtmlTags(xml.substring(plotStartIndex + plotStart.length(), endPlotIndex));
                                    file.setPlot(part, breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax));
                                    logger.finest("Sratim Plugin: Plot found : http://www.sratim.co.il/" + scanUrl + " - " + file.getPlot(part));
                                }
                            }

                            // Download subtitles
                            // store the subtitles id in the movie ids map, make sure to remove the prefix "1" from the id
                            int findId = scanUrl.indexOf("id=");
                            String subId = scanUrl.substring(findId + 4);
                            movie.setId(SRATIM_PLUGIN_SUBTITLE_ID, subId);
                            downloadSubtitle(movie, file);

                        } catch (Exception error) {
                            logger.severe("Sratim Plugin: Error - " + error.getMessage());
                        }

                        break;
                    }

                }

            }
            String title = sb.toString();
            if (!"".equals(title)) {
                file.setTitle(title);
            }
        }
    }

    protected void updateTVShowInfo(Movie movie, String mainXML) throws MalformedURLException, IOException {
        scanTVShowTitles(movie, mainXML);
    }

    public void downloadSubtitle(Movie movie, MovieFile mf) throws IOException {

        if ( !subtitleDownload ) {
            mf.setSubtitlesExchange(true);
            return;
        }

        if (movie.isExtra()) {
            mf.setSubtitlesExchange(true);
            return;
        }

        // Check if this movie already have subtitles for it (.srt and .sub)
        if (hasExistingSubtitles(mf)) {
            mf.setSubtitlesExchange(true);
            return;
        }

        // Get the file base name
        String path = mf.getFile().getName().toUpperCase();
        int lindex = path.lastIndexOf(".");
        if (lindex == -1) {
            return;
        }

        String basename = path.substring(0, lindex);

        // Check if this is a bluray file
        boolean bluRay = false;
        if (path.endsWith(".M2TS") && path.startsWith("0")) {
            bluRay = true;
        }

        basename = basename.replace('.', ' ').replace('-', ' ').replace('_', ' ');

        logger.finest("Sratim Plugin: downloadSubtitle: " + mf.getFile().getAbsolutePath());
        logger.finest("Sratim Plugin: basename: " + basename);
        logger.finest("Sratim Plugin: bluRay: " + bluRay);

        int bestFPSCount = 0;
        int bestBlurayCount = 0;
        int bestBlurayFPSCount = 0;

        String bestFPSID = "";
        String bestBlurayID = "";
        String bestBlurayFPSID = "";
        String bestFileID = "";
        String bestSimilar = "";

        //retrieve subtitles page

        String subID = movie.getId(SRATIM_PLUGIN_SUBTITLE_ID);
        String mainXML = webBrowser.request("http://www.sratim.co.il/subtitles.php?mid=" + subID, Charset.forName("UTF-8"));

        int index = 0;
        int endIndex = 0;

        // find the end of hebrew subtitles section, to prevent downloading non-hebrew ones
        int endHebrewSubsIndex = findEndOfHebrewSubtitlesSection(mainXML);

        // Check that hebrew subtitle exist
        String hebrewSub = HTMLTools.getTextAfterElem(mainXML, "<img src=\"images/Flags/1.png");

        logger.finest("Sratim Plugin: hebrewSub: " + hebrewSub);

        // Check that there is no 0 hebrew sub
        if (Movie.UNKNOWN.equals(hebrewSub)) {
            logger.finest("Sratim Plugin: No Hebrew subtitles");
            return;
        }

        float maxMatch = 0.0f;
        float matchThreshold = Float.parseFloat(PropertiesUtil.getProperty("sratim.textMatchSimilarity", "0.8"));

        while (index < endHebrewSubsIndex) {

            //
            // scanID
            //
            index = mainXML.indexOf("href=\"downloadsubtitle.php?id=", index);
            if (index == -1) {
                break;
            }

            index += 30;

            endIndex = mainXML.indexOf("\"", index);
            if (endIndex == -1) {
                break;
            }

            String scanID = mainXML.substring(index, endIndex);

            //
            // scanDiscs
            //
            index = mainXML.indexOf("src=\"images/cds/cd", index);
            if (index == -1) {
                break;
            }

            index += 18;

            endIndex = mainXML.indexOf(".", index);
            if (endIndex == -1) {
                break;
            }

            String scanDiscs = mainXML.substring(index, endIndex);

            //
            // scanFileName
            //
            index = mainXML.indexOf("subtitle_title\" style=\"direction:ltr;\" title=\"", index);
            if (index == -1) {
                break;
            }

            index += 46;

            endIndex = mainXML.indexOf("\"", index);
            if (endIndex == -1) {
                break;
            }

            String scanFileName = mainXML.substring(index, endIndex).toUpperCase().replace('.', ' ');
            // removing all characters causing metric to hang.
            scanFileName = scanFileName.replaceAll("-|\u00A0"," ").replaceAll(" ++"," ");

            //
            // scanFormat
            //
            index = mainXML.indexOf("\u05e4\u05d5\u05e8\u05de\u05d8", index); //the hebrew letters for the word "format"
            if (index == -1) {
                break;
            }

            index += 6;

            endIndex = mainXML.indexOf(",", index);
            if (endIndex == -1) {
                break;
            }

            String scanFormat = mainXML.substring(index, endIndex);

            //
            // scanFPS
            //
            index = mainXML.indexOf("\u05dc\u05e9\u05e0\u0027\u003a", index); //the hebrew letters for the word "for sec':"    lamed shin nun ' :
            if (index == -1) {
                break;
            }

            index += 5;

            endIndex = mainXML.indexOf("<", index);
            if (endIndex == -1) {
                break;
            }

            String scanFPS = mainXML.substring(index, endIndex);

            //
            //scanCount
            //
            index = mainXML.indexOf("subt_date\"><span class=\"smGray\">", index);
            if (index == -1) {
                break;
            }

            index += 32;

            endIndex = mainXML.indexOf(" ", index);
            if (endIndex == -1) {
                break;
            }

            String scanCount = mainXML.substring(index, endIndex);

            // Check for best text similarity
            float result = metric.getSimilarity(basename, scanFileName);
            if (result > maxMatch) {
                maxMatch = result;
                bestSimilar = scanID;
            }

            logger.finest("Sratim Plugin: scanFileName: " + scanFileName + " scanFPS: " + scanFPS + " scanID: " + scanID + " scanCount: " + scanCount + " scanDiscs: "
                            + scanDiscs + " scanFormat: " + scanFormat + " similarity: " + result);

            // Check if movie parts matches
            int nDiscs = movie.getMovieFiles().size();
            if (!String.valueOf(nDiscs).equals(scanDiscs)) {
                continue;
            }

            // Check for exact file name
            if (scanFileName.equals(basename)) {
                bestFileID = scanID;
                break;
            }

            try {
                int scanCountInt = 0;
                try {
                    scanCountInt = Integer.parseInt(scanCount);
                } catch (Exception error) {
                    scanCountInt = 0;
                }

                float scanFPSFloat = 0;
                try {
                    scanFPSFloat = Float.parseFloat(scanFPS);
                } catch (Exception error) {
                    scanFPSFloat = 0;
                }

                logger.finest("Sratim Plugin: FPS: " + movie.getFps() + " scanFPS: " + scanFPSFloat);

                if (bluRay && ((scanFileName.indexOf("BRRIP") != -1) || 
                               (scanFileName.indexOf("BDRIP") != -1) || 
                               (scanFileName.indexOf("BLURAY") != -1) ||
                               (scanFileName.indexOf("BLU-RAY") != -1) || 
                               (scanFileName.indexOf("HDDVD") != -1))) {

                    if ((scanFPSFloat == 0) && (scanCountInt > bestBlurayCount)) {
                        bestBlurayCount = scanCountInt;
                        bestBlurayID = scanID;
                    }

                    if ((movie.getFps() == scanFPSFloat) && (scanCountInt > bestBlurayFPSCount)) {
                        bestBlurayFPSCount = scanCountInt;
                        bestBlurayFPSID = scanID;
                    }

                }

                if ((movie.getFps() == scanFPSFloat) && (scanCountInt > bestFPSCount)) {
                    bestFPSCount = scanCountInt;
                    bestFPSID = scanID;
                }

            } catch (Exception error) {
                // Ignored
            }
        }

        // Select the best subtitles ID
        String bestID = "";

        // Check for exact file name match
        if (!bestFileID.equals("")) {
            logger.finest("Sratim Plugin: Best Filename");
            bestID = bestFileID;
        } else if (maxMatch >= matchThreshold) {
            // Check for text similarity match, similarity threshold takes precedence over FPS check
            logger.finest("Sratim Plugin: Best Text Similarity threshold");
            bestID = bestSimilar;
        } else if (!bestBlurayFPSID.equals("")) {
            // Check for bluray match
            logger.finest("Sratim Plugin: Best Bluray FPS");
            bestID = bestBlurayFPSID;
        } else if (!bestBlurayID.equals("")) {
            // Check for bluray match
            logger.finest("Sratim Plugin: Best Bluray");
            bestID = bestBlurayID;
        } else if (!bestFPSID.equals("")) {
        // Check for fps match
            logger.finest("Sratim Plugin: Best FPS");
            bestID = bestFPSID;
        } else if (maxMatch > 0) {
        // Check for text match, now just choose the best similar name
            logger.finest("Sratim Plugin: Best Similar");
            bestID = bestSimilar;
        } else {
            logger.finest("Sratim Plugin: No subtitle found");
            return;
        }

        logger.finest("Sratim Plugin: bestID: " + bestID);

        // reconstruct movie filename with full path
        String orgName = mf.getFile().getAbsolutePath();
        File subtitleFile = new File(orgName.substring(0, orgName.lastIndexOf(".")));
        if (!downloadSubtitleZip(movie, "http://www.sratim.co.il/downloadsubtitle.php?id=" + bestID, subtitleFile)) {
            logger.severe("Sratim Plugin: Error - Subtitle download failed");
            return;
        }

        mf.setSubtitlesExchange(true);
        movie.setSubtitles("YES");
    }

    public boolean downloadSubtitleZip(Movie movie, String subDownloadLink, File subtitleFile) {

        boolean found = false;
        try {
            URL url = new URL(subDownloadLink);
            HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
            connection.setRequestProperty("Cookie", cookieHeader);

            logger.finest("Sratim Plugin: cookieHeader:" + cookieHeader);

            InputStream inputStream = connection.getInputStream();

            String contentType = connection.getContentType();

            logger.finest("Sratim Plugin: contentType:" + contentType);

            // Check that the content iz zip and that the site did not blocked the download
            if (!contentType.equals("application/octet-stream")) {
                logger.severe("Sratim Plugin: ********** Error - Sratim subtitle download limit may have been reached. Suspending subtitle download.");

                subtitleDownload = false;
                return false;
            }

            Collection<MovieFile> parts = movie.getMovieFiles();
            Iterator<MovieFile> partsIter = parts.iterator();

            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(inputStream);

            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                // for each entry to be extracted
                String entryName = zipentry.getName();

                logger.finest("Sratim Plugin: ZIP entryname: " + entryName);

                // Check if this is a subtitle file
                if (entryName.toUpperCase().endsWith(".SRT") || entryName.toUpperCase().endsWith(".SUB")) {

                    int n;
                    OutputStream fileoutputstream;

                    String entryExt = entryName.substring(entryName.lastIndexOf('.'));

                    if (movie.isTVShow()) {
                        // for tv show, use the subtitleFile parameter because tv show is
                        // handled by downloading subtitle from the episode page (each episode for its own)
                        fileoutputstream = FileTools.createFileOutputStream(subtitleFile + entryExt);
                    } else {
                        // for movie, we need to save all subtitles entries
                        // from inside the zip file, and name them according to
                        // the movie file parts.
                        if (partsIter.hasNext()) {
                            MovieFile moviePart = partsIter.next();
                            String partName = moviePart.getFile().getAbsolutePath();
                            partName = partName.substring(0, partName.lastIndexOf('.'));
                            fileoutputstream =FileTools.createFileOutputStream(partName + entryExt);
                        } else {
                            // in case of some mismatch, use the old code
                            fileoutputstream = FileTools.createFileOutputStream(subtitleFile + entryExt);
                        }
                    }

                    while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                        fileoutputstream.write(buf, 0, n);
                    }

                    fileoutputstream.close();

                    found = true;
                }

                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }

            zipinputstream.close();

        } catch (Exception error) {
            logger.severe("Sratim Plugin: Error - " + error.getMessage());
            return false;
        }

        return found;
    }

    public void loadSratimCookie() {

        //TODO: Fix the login process, by capturing the captcha image and saving it
        // we need to catch the captcha image ans save it, until then,
        // the limitation is to manually login by a browser,
        // get the cookie value (the PHPSESSID value) and manually save it to the "sratim.session" file
        // for example the content of the sratim.session file should be: PHPSESSID=a3b0ed6dc98723e10407cd100ab92d18
        // for now, most of the code is commented out !!

        // Check if we already logged in and got the correct cookie
        if (!cookieHeader.equals("")) {
            return;
        }

        /*
        // Check if cookie file exist
        try {
            File cookieFile = new File("sratim.cookie");
            BufferedReader in = new BufferedReader(new FileReader(cookieFile));
            cookieHeader = in.readLine();
            in.close();
        } catch (Exception error) {
            // Ignored
        }

        if (!cookieHeader.equals("")) {
            // Verify cookie by loading main page
            try {
                URL url = new URL("http://www.sratim.co.il/");
                HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
                connection.setRequestProperty("Cookie", cookieHeader);

                // Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
                String xml = response.toString();

                if (xml.indexOf("logout=1") != -1) {
                    logger.finest("Sratim Plugin: Subtitles Cookies Valid");
                    return;
                }

            } catch (Exception error) {
                logger.severe("Sratim Plugin: Error - " + error.getMessage());
                return;
            }

            logger.severe("Sratim Plugin: Cookie Use Failed - Creating new session and jpg files");

            cookieHeader = "";
            File dcookieFile = new File("sratim.cookie");
            dcookieFile.delete();
        }
        */

        // Check if session file exist
        try {
            FileReader sessionFile = new FileReader("sratim.session");
            BufferedReader in = new BufferedReader(sessionFile);
            cookieHeader = in.readLine();
            in.close();
            logger.finer("found session cookie for sratim.co.il");
        } catch (Exception error) {
            logger.severe("In order to download subtitles you need to login to sratim.co.il with a browser and get the " +
                    "cookie value (PHPSESSID value) and save into the file \"sratim.session\". you can do that by using firefox, " +
                    "after logging in to sratim.co.il, type this in the address bar: javascript:alert(document.cookie) and click enter.");
        }

        /* removed for now
        // Check if we don't have the verification code yet
        if (!cookieHeader.equals("")) {
            try {
                logger.finest("Sratim Plugin: cookieHeader: " + cookieHeader);

                // Build the post request
                String post;
                post = "Username=" + login + "&Password=" + pass + "&VerificationCode=" + code + "&Referrer=%2Fdefault.aspx%3F";

                logger.finest("Sratim Plugin: post: " + post);

                URL url = new URL("http://www.sratim.co.il/login.php");
                HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(post.getBytes().length));
                connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
                connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                connection.setRequestProperty("Referer", "http://www.sratim.co.il/users/login.aspx");

                connection.setRequestProperty("Cookie", cookieHeader);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);

                // Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(post);
                wr.flush();
                wr.close();

                cookieHeader = "";

                // read new cookies and update our cookies
                for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                    if ("Set-Cookie".equals(header.getKey())) {
                        for (String rcookieHeader : header.getValue()) {
                            String[] cookieElements = rcookieHeader.split(" *; *");
                            if (cookieElements.length >= 1) {
                                String[] firstElem = cookieElements[0].split(" *= *");
                                String cookieName = firstElem[0];
                                String cookieValue = firstElem.length > 1 ? firstElem[1] : null;

                                logger.finest("Sratim Plugin: cookieName:" + cookieName);
                                logger.finest("Sratim Plugin: cookieValue:" + cookieValue);

                                if (!cookieHeader.equals("")) {
                                    cookieHeader = cookieHeader + "; ";
                                }
                                cookieHeader = cookieHeader + cookieName + "=" + cookieValue;
                            }
                        }
                    }
                }

                // Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
                String xml = response.toString();

                if (xml.indexOf("<h2>Object moved to <a href=\"%2fdefault.aspx%3f\">here</a>.</h2>") != -1) {

                    // write the session cookie to a file
                    FileWriter cookieFile = new FileWriter("sratim.cookie");
                    cookieFile.write(cookieHeader);
                    cookieFile.close();

                    // delete the old session and jpg files
                    File dimageFile = new File("sratim.jpg");
                    dimageFile.delete();

                    File dsessionFile = new File("sratim.session");
                    dsessionFile.delete();

                    return;
                }

                logger.severe("Sratim Plugin: Login Failed - Creating new session and jpg files");

            } catch (Exception error) {
                logger.severe("Sratim Plugin: Error - " + error.getMessage());
                return;
            }

        }

        try {
            URL url = new URL("http://www.sratim.co.il/users/login.aspx");
            HttpURLConnection connection = (HttpURLConnection)(url.openConnection());

            cookieHeader = "";

            // read new cookies and update our cookies
            for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if ("Set-Cookie".equals(header.getKey())) {
                    for (String rcookieHeader : header.getValue()) {
                        String[] cookieElements = rcookieHeader.split(" *; *");
                        if (cookieElements.length >= 1) {
                            String[] firstElem = cookieElements[0].split(" *= *");
                            String cookieName = firstElem[0];
                            String cookieValue = firstElem.length > 1 ? firstElem[1] : null;

                            logger.finest("Sratim Plugin: cookieName:" + cookieName);
                            logger.finest("Sratim Plugin: cookieValue:" + cookieValue);

                            if (!cookieHeader.equals("")) {
                                cookieHeader = cookieHeader + "; ";
                            }
                            cookieHeader = cookieHeader + cookieName + "=" + cookieValue;
                        }
                    }
                }
            }

            // write the session cookie to a file
            FileWriter sessionFile = new FileWriter("sratim.session");
            sessionFile.write(cookieHeader);
            sessionFile.close();

            // Get the jpg code
            url = new URL("http://www.sratim.co.il/verificationimage.aspx");
            connection = (HttpURLConnection)(url.openConnection());
            connection.setRequestProperty("Cookie", cookieHeader);

            // Write the jpg code to the file
            File imageFile = new File("sratim.jpg");
            FileTools.copy(connection.getInputStream(), new FileOutputStream(imageFile));

            // Exit and wait for the user to type the jpg code
            logger.severe("#############################################################################");
            logger.severe("### Open \"sratim.jpg\" file, and write the code in the sratim.code field ###");
            logger.severe("#############################################################################");
            System.exit(0);

        } catch (Exception error) {
            logger.severe("Sratim Plugin: Error - " + error.getMessage());
            return;
        }
        */

    }

    public static String removeChar(String s, char c) {
        String r = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                r += s.charAt(i);
            }
        }
        return r;
    }

    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB if sratim doesn't know movie
        logger.finest("Sratim Plugin: Scanning NFO for sratim url");
        Matcher m = nfoPattern.matcher(nfo);
        boolean found = false;
        while (m.find()) {
            String url = m.group();
            if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
                found = true;
                movie.setId(SRATIM_PLUGIN_ID, url);
            }
        }
        if (found) {
            logger.finer("Sratim Plugin: URL found in nfo = " + movie.getId(SRATIM_PLUGIN_ID));
        } else {
            logger.finer("Sratim Plugin: No URL found in nfo!");
        }
    }

    private int findEndOfHebrewSubtitlesSection(String mainXML) {
        int result = mainXML.length();
        boolean onlyHeb = Boolean.parseBoolean(PropertiesUtil.getProperty("sratim.downloadOnlyHebrew", "false"));
        if (onlyHeb) {
            String pattern = "images/Flags/2.png";
            int nonHeb = mainXML.indexOf(pattern);
            if(nonHeb == -1) {
                result = mainXML.length();
            }
        }
        return result;
    }

    protected String extractMovieTitle(String xml) {
        String result;
        int start = xml.indexOf("<h1 class=\"subtext_view\">");
        int end = xml.indexOf("</h1>", start);
        String title = xml.substring(start + 25, end);
        result = HTMLTools.decodeHtml(title);
        return removeTrailBracket(result);
    }

    protected boolean hasExistingSubtitles(MovieFile mf) {
        // Check if this movie already has subtitles for it, popcorn supports .srt and .sub
        String path = mf.getFile().getAbsolutePath();
        int lindex = path.lastIndexOf(".");
        String basename = path.substring(0, lindex + 1);

        File srtFile = new File(basename + "srt");
        File subFile = new File(basename + "sub");

        return srtFile.exists() || subFile.exists();
    }



    protected static boolean containsHebrew(char[] chars) {
        if (chars == null) {
            return false;
        }
        for (int i = 0; i < chars.length; i++) {
            if (getCharType(chars[i]) == BCT_R) {
                return true;
            }
        }
        return false;
    }
}
