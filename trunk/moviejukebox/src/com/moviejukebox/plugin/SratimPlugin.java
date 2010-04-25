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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static final String ARGUMENT_VIEWSTATE = "__VIEWSTATE";
    private static final String SEASON_FORM_NAME = "__EVENTTARGET";
    private static final String SEASON_FORM_VALUE = "ctl00$ctl00$Body$Body$Box$Menu_";
    private static final String ARGUMENT_FORM_NAME = "__EVENTARGUMENT";
    private static final String ARGUMENT_FORM_VALUE = "lbl";

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
    private static String pass = "";
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
        
        if (subtitleDownload == true && !login.equals(""))
            loadSratimCookie();
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
            mediaFile.setId(SRATIM_PLUGIN_ID, sratimUrl);
        }

        if (!sratimUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
            retval = updateMediaInfo(mediaFile);
        }

        return retval;
    }

    /**
     * retrieve the sratim url matching the specified movie name and year.
     */
    protected String getSratimUrl(Movie mediaFile, String movieName, String year) {

        try {
            String imdbId = mediaFile.getId(IMDB_PLUGIN_ID);
            if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
                imdbId = imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear());
                mediaFile.setId(IMDB_PLUGIN_ID, imdbId);
            }

            if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
                return Movie.UNKNOWN;
            }

            String sratimUrl;

            String xml = webBrowser.request("http://www.sratim.co.il/movies/search.aspx?Keyword=" + mediaFile.getId(IMDB_PLUGIN_ID));

            String detailsUrl = HTMLTools.extractTag(xml, "cellpadding=\"0\" cellspacing=\"0\" onclick=\"document.location='", 0, "'");

            if (detailsUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
                return Movie.UNKNOWN;
            }

            sratimUrl = "http://www.sratim.co.il" + detailsUrl;

            return sratimUrl;

        } catch (Exception error) {
            logger.severe("Failed retreiving sratim informations for movie : " + movieName);
            logger.severe("Error : " + error.getMessage());
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
    private static int GetCharType(char C) {
        if (((C >= 'א') && (C <= 'ת'))) {
            return BCT_R;
        }

        if ((C == 0x26) || (C == 0x40) || ((C >= 0x41) && (C <= 0x5A)) || ((C >= 0x61) && (C <= 0x7A)) || ((C >= 0xC0) && (C <= 0xD6))
                        || ((C >= 0xD8) && (C <= 0xDF))) {
            return BCT_L;
        }

        if (((C >= 0x30) && (C <= 0x39))) {
            return BCT_EN;
        }

        if ((C == 0x2E) || (C == 0x2F)) {
            return BCT_ES;
        }

        if ((C == 0x23) || (C == 0x24) || ((C >= 0xA2) && (C <= 0xA5)) || (C == 0x25) || (C == 0x2B) || (C == 0x2D) || (C == 0xB0) || (C == 0xB1)) {
            return BCT_ET;
        }

        if ((C == 0x2C) || (C == 0x3A)) {
            return BCT_CS;
        }

        // Default Natural
        return BCT_N;
    }

    // Rotate a specific part of a string
    private static void RotateString(char[] String, int StartPos, int EndPos) {
        int Pos;
        char TempChar;

        for (Pos = 0; Pos < (EndPos - StartPos + 1) / 2; Pos++) {
            TempChar = String[StartPos + Pos];

            String[StartPos + Pos] = String[EndPos - Pos];

            String[EndPos - Pos] = TempChar;
        }

    }

    // Set the string char types
    private static void SetStringCharType(char[] String, int[] CharType) {
        int Pos;

        Pos = 0;

        while (Pos < String.length) {
            CharType[Pos] = GetCharType(String[Pos]);

            // Fix "(" and ")"
            if (String[Pos] == ')') {
                String[Pos] = '(';
            } else if (String[Pos] == '(') {
                String[Pos] = ')';
            }

            Pos++;
        }

    }

    // Resolving Weak Types
    private static void ResolveWeakType(char[] String, int[] CharType) {
        int Pos;

        Pos = 0;

        while (Pos < String.length) {
            // Check that we have at least 3 chars
            if (String.length - Pos >= 3) {
                if ((CharType[Pos] == BCT_EN) && (CharType[Pos + 2] == BCT_EN) && ((CharType[Pos + 1] == BCT_ES) || (CharType[Pos + 1] == BCT_CS))) // Change
                // the char
                // type
                {
                    CharType[Pos + 1] = BCT_EN;
                }
            }

            if (String.length - Pos >= 2) {
                if ((CharType[Pos] == BCT_EN) && (CharType[Pos + 1] == BCT_ET)) // Change the char type
                {
                    CharType[Pos + 1] = BCT_EN;
                }

                if ((CharType[Pos] == BCT_ET) && (CharType[Pos + 1] == BCT_EN)) // Change the char type
                {
                    CharType[Pos] = BCT_EN;
                }
            }

            // Default change all the terminators to natural
            if ((CharType[Pos] == BCT_ES) || (CharType[Pos] == BCT_ET) || (CharType[Pos] == BCT_CS)) {
                CharType[Pos] = BCT_N;
            }

            Pos++;
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
    private static void ResolveNaturalType(char[] String, int[] CharType, int DefaultDirection) {
        int Pos, CheckPos;
        int Before, After;

        Pos = 0;

        while (Pos < String.length) {
            // Check if this is natural type and we need to cahnge it
            if (CharType[Pos] == BCT_N) {
                // Search for the type of the previous strong type
                CheckPos = Pos - 1;

                while (true) {
                    if (CheckPos < 0) {
                        // Default language
                        Before = DefaultDirection;
                        break;
                    }

                    if (CharType[CheckPos] == BCT_R) {
                        Before = BCT_R;
                        break;
                    }

                    if (CharType[CheckPos] == BCT_L) {
                        Before = BCT_L;
                        break;
                    }

                    CheckPos--;
                }

                CheckPos = Pos + 1;

                // Search for the type of the next strong type
                while (true) {
                    if (CheckPos >= String.length) {
                        // Default language
                        After = DefaultDirection;
                        break;
                    }

                    if (CharType[CheckPos] == BCT_R) {
                        After = BCT_R;
                        break;
                    }

                    if (CharType[CheckPos] == BCT_L) {
                        After = BCT_L;
                        break;
                    }

                    CheckPos++;
                }

                // Change the natural depanded on the strong type before and after
                if ((Before == BCT_R) && (After == BCT_R)) {
                    CharType[Pos] = BCT_R;
                } else if ((Before == BCT_L) && (After == BCT_L)) {
                    CharType[Pos] = BCT_L;
                } else {
                    CharType[Pos] = DefaultDirection;
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
    private static void ResolveImplictLevels(char[] String, int[] CharType, int[] Level) {
        int Pos;

        Pos = 0;

        while (Pos < String.length) {
            if (CharType[Pos] == BCT_L) {
                Level[Pos] = 2;
            }

            if (CharType[Pos] == BCT_R) {
                Level[Pos] = 1;
            }

            if (CharType[Pos] == BCT_EN) {
                Level[Pos] = 2;
            }

            Pos++;
        }
    }

    // Reordering Resolved Levels
    private static void ReorderResolvedLevels(char[] String, int[] Level) {
        int Count;
        int StartPos, EndPos, Pos;

        for (Count = 2; Count >= 1; Count--) {
            Pos = 0;

            while (Pos < String.length) {
                // Check if this is the level start
                if (Level[Pos] >= Count) {
                    StartPos = Pos;

                    // Search for the end
                    while ((Pos + 1 != String.length) && (Level[Pos + 1] >= Count)) {
                        Pos++;
                    }

                    EndPos = Pos;

                    RotateString(String, StartPos, EndPos);
                }

                Pos++;
            }
        }
    }

    // Convert logical string to visual
    private static void LogicalToVisual(char[] String, int DefaultDirection) {
        int[] CharType;
        int[] Level;

        int Len;

        Len = String.length;

        // Allocate CharType and Level arrays
        CharType = new int[Len];

        Level = new int[Len];

        // Set the string char types
        SetStringCharType(String, CharType);

        // Resolving Weak Types
        ResolveWeakType(String, CharType);

        // Resolving Natural Types
        ResolveNaturalType(String, CharType, DefaultDirection);

        // Resolving Implicit Levels
        ResolveImplictLevels(String, CharType, Level);

        // Reordering Resolved Levels
        ReorderResolvedLevels(String, Level);
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
            LogicalToVisual(ret, BCT_R);
        }
        String s = new String(ret);
        return s;
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
            logger.severe("extractTag an exception occurred during tag extraction : " + error);
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

            String sratimUrl = movie.getId(SRATIM_PLUGIN_ID);

            String xml = webBrowser.request(sratimUrl);

            if (sratimUrl.contains("series")) {
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
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "<span style=\"font-size:12pt;font-weight:bold\"><img alt=\"", 0, "/")));
            }

            movie.setDirector(logicalToVisual(HTMLTools.getTextAfterElem(xml, "במאי:")));
            movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "תאריך יציאה לקולנוע בחו\"ל:"));
            // Issue 1176 - Prevent lost of NFO Data
            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(logicalToVisual(removeTrailDot(HTMLTools.getTextAfterElem(xml, "אורך:"))));
            }
            movie.setCountry(logicalToVisual(HTMLTools.getTextAfterElem(xml, "מדינה:")));

            // Prefer IMDB genres
            if (movie.getGenres().isEmpty()) {
                String genres = HTMLTools.getTextAfterElem(xml, "ז'אנר:");
                if (!Movie.UNKNOWN.equals(genres)) {
                    for (String genre : genres.split(" *, *")) {
                        movie.addGenre(logicalToVisual(Library.getIndexingGenre(genre)));
                    }
                }
            }

            String tmpPlot = removeHtmlTags(extractTag(xml, "<b><u>תקציר:</u></b><br />", "</div>"));

            movie.setPlot(breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax));

            if (!movie.isOverrideYear()) {

                if (sratimUrl.contains("series")) {
                    movie.setYear(HTMLTools.extractTag(xml, "<span style=\"font-weight:normal\">(", 0, ")"));
                } else {
                    movie.setYear(HTMLTools.getTextAfterElem(xml, "<span id=\"ctl00_ctl00_Body_Body_Box_ProductionYear\">"));
                }
            }

            movie.setCast(logicalToVisual(removeHtmlTags(HTMLTools.extractTags(xml, "שחקנים:", "<br />", "<a href", "</a>"))));

            if (movie.isTVShow()) {
                updateTVShowInfo(movie, xml);
            } else {
                // Download subtitle from the page
                downloadSubtitle(movie, movie.getFirstFile(), xml);
            }

        } catch (Exception error) {
            logger.severe("Failed retrieving sratim informations for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
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

    @SuppressWarnings("unused")
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

        // in sratim.co.il website, seasons are displayed in tabs and since
        // on page load only the last season is displayed, we need to get the correct
        // season according to movie details.
        try {
            String sratimUrl = movie.getId(SRATIM_PLUGIN_ID);
            String html = webBrowser.request(sratimUrl);
            String viewState = HTMLTools.extractTag(html, "<input type=\"hidden\" name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"", 0, "\"");
            int season = movie.getSeason();
            String seasonUrl = buildSeasonUrl(sratimUrl, season, viewState);
            // season details needs to be retrieved using POST submission.
            // in case of exception use the xml retrieved by the parent plugin (imdb or thetvdb)
            mainXML = postRequest(seasonUrl);

        } catch (Exception error) {
            logger.severe("Failed retreiving sratim informations for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
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
                    index = mainXML.indexOf("<a href=\"/movies/view.aspx?id=", index);
                    if (index == -1)
                        break;

                    index += 30;

                    endIndex = mainXML.indexOf("\"", index);
                    if (endIndex == -1)
                        break;

                    String scanID = mainXML.substring(index, endIndex);

                    index = endIndex + 9;

                    endIndex = mainXML.indexOf("</b> - ", index);
                    if (endIndex == -1)
                        break;

                    String scanPart = mainXML.substring(index, endIndex);

                    index = endIndex + 7;

                    endIndex = mainXML.indexOf("<", index);

                    String scanName = mainXML.substring(index, endIndex);

                    if (scanPart.equals(Integer.toString(part))) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" / ");
                        }
                        sb.append(logicalToVisual(HTMLTools.decodeHtml(scanName)));

                        try {

                            // Get the episode page url
                            String xml = webBrowser.request("http://www.sratim.co.il/movies/view.aspx?id=" + scanID);
                            // System.err.println("http://www.sratim.co.il/movies/view.aspx?id=" + scanID);
                            // System.err.println(xml);

                            // Update Plot
                            // TODO Be sure this is enought to go straigh to the plot ...
                            String plotStart = "<u>תקציר:</u></b><br />";
                            int plotStartIndex = xml.indexOf(plotStart);
                            if (plotStartIndex > -1) {
                                int endPlotIndex = xml.indexOf("</div>", plotStartIndex + plotStart.length());
                                if (endPlotIndex > -1) {
                                    String tmpPlot = removeHtmlTags(xml.substring(plotStartIndex + plotStart.length(), endPlotIndex));
                                    file.setPlot(part, breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax));
                                    logger.finest("Plot found : http://www.sratim.co.il/movies/view.aspx?id=" + scanID + " - " + file.getPlot(part));
                                }
                            }

                            // Download subtitles
                            downloadSubtitle(movie, file, xml);

                        } catch (Exception error) {
                            logger.severe("Error : " + error.getMessage());
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

    public void downloadSubtitle(Movie movie, MovieFile mf, String mainXML) {

        if (subtitleDownload == false) {
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
        if (lindex == -1)
            return;

        String basename = path.substring(0, lindex);

        // Check if this is a bluray file
        boolean bluRay = false;
        if (path.endsWith(".M2TS") && path.startsWith("0"))
            bluRay = true;

        basename = basename.replace('.', ' ').replace('-', ' ').replace('_', ' ');

        logger.finest("downloadSubtitle: " + mf.getFile().getAbsolutePath());
        logger.finest("basename: " + basename);
        logger.finest("bluRay: " + bluRay);

        int bestFPSCount = 0;
        int bestBlurayCount = 0;
        int bestBlurayFPSCount = 0;

        String bestFPSID = "";
        String bestBlurayID = "";
        String bestBlurayFPSID = "";
        String bestFileID = "";
        String bestSimilar = "";

        int index = 0;
        int endIndex = 0;

        // find the end of hebrew subtitles section, to prevent downloading non-hebrew ones
        int endHebrewSubsIndex = findEndOfHebrewSubtitlesSection(mainXML);

        // Check that hebrew subtitle exist
        String hebrewSub = HTMLTools.getTextAfterElem(mainXML, "<img src=\"/images/flags/Hebrew.gif\" alt=\"");

        logger.finest("hebrewSub: " + hebrewSub);

        // Check that there is no 0 hebrew sub
        if (Movie.UNKNOWN.equals(hebrewSub) || hebrewSub.startsWith("0 ")) {
            logger.finest("No Hebrew subtitles");

            return;
        }

        float maxMatch = 0.0f;
        float matchThreshold = Float.parseFloat(PropertiesUtil.getProperty("sratim.textMatchSimilarity", "0.8"));

        while (index < endHebrewSubsIndex) {
            index = mainXML.indexOf("<a href=\"/movies/subtitles/download.aspx?", index);
            if (index == -1)
                break;

            index += 41;

            endIndex = mainXML.indexOf("\"", index);
            if (endIndex == -1)
                break;

            String scanID = mainXML.substring(index, endIndex);

            index = mainXML.indexOf("<br />", index);
            if (index == -1)
                break;

            index += 6;

            endIndex = mainXML.indexOf(" ", index);
            if (endIndex == -1)
                break;

            String scanCount = removeChar(mainXML.substring(index, endIndex), ',');

            index = mainXML.indexOf("<img src=\"/images/discs_", index);
            if (index == -1)
                break;

            index += 24;

            endIndex = mainXML.indexOf(".", index);
            if (endIndex == -1)
                break;

            String scanDiscs = mainXML.substring(index, endIndex);

            index = mainXML.indexOf("> (", index);
            if (index == -1)
                break;

            index += 3;

            endIndex = mainXML.indexOf(")", index);
            if (endIndex == -1)
                break;

            String scanFormat = mainXML.substring(index, endIndex);

            index = mainXML.indexOf("direction:ltr;text-align:left\" title=\"", index);
            if (index == -1)
                break;

            index += 38;

            endIndex = mainXML.indexOf("\"", index);
            if (endIndex == -1)
                break;

            String scanFileName = mainXML.substring(index, endIndex).toUpperCase().replace('.', ' ');
            // removing all characters causing metric to hang.
            scanFileName=scanFileName.replaceAll("-|\u00A0"," ").replaceAll(" ++"," ");

            index = mainXML.indexOf("</b> ", index);
            if (index == -1)
                break;

            index += 5;

            endIndex = mainXML.indexOf("<", index);
            if (endIndex == -1)
                break;

            String scanFPS = mainXML.substring(index, endIndex);

            // Check for best text similarity
            float result = metric.getSimilarity(basename, scanFileName);
            if (result > maxMatch) {
                maxMatch = result;
                bestSimilar = scanID;
            }

            logger.finest("scanFileName: " + scanFileName + " scanFPS: " + scanFPS + " scanID: " + scanID + " scanCount: " + scanCount + " scanDiscs: "
                            + scanDiscs + " scanFormat: " + scanFormat + " similarity: " + result);

            // Check if movie parts matches
            int nDiscs = movie.getMovieFiles().size();
            if (!String.valueOf(nDiscs).equals(scanDiscs))
                continue;

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
                }

                float scanFPSFloat = 0;
                try {
                    scanFPSFloat = Float.parseFloat(scanFPS);
                } catch (Exception error) {
                }

                logger.finest("FPS: " + movie.getFps() + " scanFPS: " + scanFPSFloat);

                if (bluRay
                                && ((scanFileName.indexOf("BRRIP") != -1) || (scanFileName.indexOf("BDRIP") != -1) || (scanFileName.indexOf("BLURAY") != -1)
                                                || (scanFileName.indexOf("BLU-RAY") != -1) || (scanFileName.indexOf("HDDVD") != -1))) {

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
            }

        }

        // Select the best subtitles ID
        String bestID = "";

        // Check for exact file name match
        if (!bestFileID.equals("")) {
            logger.finest("Best Filename");
            bestID = bestFileID;
        } else

        // Check for text similarity match, similarity threshold takes precedence over FPS check
        if (maxMatch >= matchThreshold) {
            logger.finest("Best Text Similarity threshold");
            bestID = bestSimilar;
        } else

        // Check for bluray match
        if (!bestBlurayFPSID.equals("")) {
            logger.finest("Best Bluray FPS");
            bestID = bestBlurayFPSID;
        } else

        // Check for bluray match
        if (!bestBlurayID.equals("")) {
            logger.finest("Best Bluray");
            bestID = bestBlurayID;
        } else

        // Check for fps match
        if (!bestFPSID.equals("")) {
            logger.finest("Best FPS");
            bestID = bestFPSID;
        } else

        // Check for text match, now just choose the best similar name
        if (maxMatch > 0) {
            logger.finest("Best Similar");
            bestID = bestSimilar;
        } else

        {
            logger.finest("No subtitle found");
            return;
        }

        logger.finest("bestID: " + bestID);

        // reconstruct movie filename with full path
        String orgName = mf.getFile().getAbsolutePath();
        File subtitleFile = new File(orgName.substring(0, orgName.lastIndexOf(".")));
        if (!downloadSubtitleZip(movie, "http://www.sratim.co.il/movies/subtitles/download.aspx?" + bestID, subtitleFile)) {
            logger.severe("Error - Sratim subtitle download failed");
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

            logger.finest("cookieHeader:" + cookieHeader);

            InputStream inputStream = connection.getInputStream();

            String contentType = connection.getContentType();

            logger.finest("contentType:" + contentType);

            // Check that the content iz zip and that the site did not blocked the download
            if (!contentType.equals("application/x-zip-compressed")) {
                logger.severe("********** Error - Sratim subtitle download limit may have been reached. Suspending subtitle download.");

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

                logger.finest("ZIP entryname: " + entryName);

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

                    while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
                        fileoutputstream.write(buf, 0, n);

                    fileoutputstream.close();

                    found = true;
                }

                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }

            zipinputstream.close();

        } catch (Exception error) {
            logger.severe("Error : " + error.getMessage());
            return false;
        }

        return found;
    }

    public void loadSratimCookie() {

        // Check if we already logged in and got the correct cookie
        if (!cookieHeader.equals(""))
            return;

        // Check if cookie file exist
        try {
            FileReader cookieFile = new FileReader("sratim.cookie");
            BufferedReader in = new BufferedReader(cookieFile);
            cookieHeader = in.readLine();
            in.close();
        } catch (Exception error) {
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
                    logger.finest("Sratim Subtitles Cookies Valid");
                    return;
                }

            } catch (Exception error) {
                logger.severe("Error : " + error.getMessage());
                return;
            }

            logger.severe("Sratim Cookie Use Failed - Creating new session and jpg files");

            cookieHeader = "";
            File dcookieFile = new File("sratim.cookie");
            dcookieFile.delete();
        }

        // Check if session file exist
        try {
            FileReader sessionFile = new FileReader("sratim.session");
            BufferedReader in = new BufferedReader(sessionFile);
            cookieHeader = in.readLine();
            in.close();
        } catch (Exception error) {
        }

        // Check if we don't have the verification code yet
        if (!cookieHeader.equals("")) {
            try {
                logger.finest("cookieHeader: " + cookieHeader);

                // Build the post request
                String post;
                post = "Username=" + login + "&Password=" + pass + "&VerificationCode=" + code + "&Referrer=%2Fdefault.aspx%3F";

                logger.finest("post: " + post);

                URL url = new URL("http://www.sratim.co.il/users/login.aspx");
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

                                logger.finest("cookieName:" + cookieName);
                                logger.finest("cookieValue:" + cookieValue);

                                if (!cookieHeader.equals(""))
                                    cookieHeader = cookieHeader + "; ";
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

                logger.severe("Sratim Login Failed - Creating new session and jpg files");

            } catch (Exception error) {
                logger.severe("Error : " + error.getMessage());
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

                            logger.finest("cookieName:" + cookieName);
                            logger.finest("cookieValue:" + cookieValue);

                            if (!cookieHeader.equals(""))
                                cookieHeader = cookieHeader + "; ";
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
            logger.severe("Error : " + error.getMessage());
            return;
        }

    }

    public static String removeChar(String s, char c) {
        String r = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c)
                r += s.charAt(i);
        }
        return r;
    }

    public void scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB if sratim doesn't know movie
        logger.finest("Scanning NFO for sratim url");
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
            logger.finer("Sratim url found in nfo = " + movie.getId(SRATIM_PLUGIN_ID));
        } else {
            logger.finer("No sratim url found in nfo !");
        }
    }

    private int findEndOfHebrewSubtitlesSection(String mainXML) {
        int result = mainXML.length();
        boolean onlyHeb = Boolean.parseBoolean(PropertiesUtil.getProperty("sratim.downloadOnlyHebrew", "false"));
        if (onlyHeb) {
            String pattern = "id=\"(Subtitles_[0-9][0-9]?)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(mainXML);
            while (m.find()) {
                String g = m.group(1);
                if (g.endsWith("_1")) {
                    // hebrew subtitles has id 'Subtitles_1'
                    result = m.start();
                    break;
                }
            }
        }
        return result;
    }

    protected String extractMovieTitle(String xml) {
        String result;
        int start = xml.indexOf("<td valign=\"top\" style=\"width:100%\"");
        int end = xml.indexOf("</td>", start);
        String title = xml.substring(start + 36, end);
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

    protected String buildSeasonUrl(String baseUrl, int i, String viewState) throws UnsupportedEncodingException {
        StringBuilder surl = new StringBuilder();

        surl.append(baseUrl);
        if (baseUrl.indexOf('?') > -1)
            surl.append('&');
        else
            surl.append('?');

        surl.append("ctl00$ctl00$Body$ScriptManager=ctl00$ctl00$Body$Body$Box$UpdatePanel|");
        surl.append(SEASON_FORM_VALUE);
        surl.append(i);

        surl.append('&').append(SEASON_FORM_NAME).append('=').append(SEASON_FORM_VALUE).append(i);
        surl.append('&').append(ARGUMENT_FORM_NAME).append('=').append(ARGUMENT_FORM_VALUE);
        surl.append("&ctl00$ctl00$Body$Body$Box$Comments$Header=");
        surl.append("&ctl00$ctl00$Body$Body$Box$Comments$Body=");
        surl.append("&__LASTFOCUS=");
        surl.append('&').append(ARGUMENT_VIEWSTATE).append('=').append(URLEncoder.encode(viewState, "UTF-8"));
        surl.append("&ctl00$ctl00$Body$Body$Box$SubtitlesLanguage=1"); // hebrew subtitles
        surl.append("&__ASYNCPOST=true");

        return surl.toString();
    }

    protected String postRequest(String url) {
        StringBuilder content = new StringBuilder();
        try {
            int queryStart = url.indexOf('&');
            if (queryStart == -1)
                queryStart = url.length();
            String baseUrl = url.substring(0, queryStart);
            URL ourl = new URL(baseUrl);
            String data = "";
            if (queryStart > -1)
                data = url.substring(queryStart + 1);

            // Send data
            URLConnection conn = ourl.openConnection();
            conn.setDoOutput(true);

            conn.setRequestProperty("Host", "www.sratim.co.il");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.11) Gecko/2009060215 Firefox/3.0.11");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "he");
            conn.setRequestProperty("Accept-Encoding", "deflate");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Referer", baseUrl);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data); // post data
            wr.flush();

            conn.getHeaderFields(); // unused
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;

            while ((line = rd.readLine()) != null) {
                content.append(line);
            }
            wr.close();
            rd.close();
        } catch (Exception error) {
            logger.severe("Failed retrieving sratim season episodes information.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return content.toString();
    }

    protected static boolean containsHebrew(char[] chars) {
        if (chars == null)
            return false;
        for (int i = 0; i < chars.length; i++) {
            if (GetCharType(chars[i]) == BCT_R)
                return true;
        }
        return false;
    }
}
