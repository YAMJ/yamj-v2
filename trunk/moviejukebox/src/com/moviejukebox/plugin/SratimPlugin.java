package com.moviejukebox.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.FileTools;

public class SratimPlugin extends ImdbPlugin {

    public static String SRATIM_PLUGIN_ID = "sratim";
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*sratim.co.il[^\\s<>`\"\\[\\]]*");
    private static String[] genereStringEnglish = { "Action", "Adult", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama",
                    "Family", "Fantasy", "Film-Noir", "Game-Show", "History", "Horror", "Music", "Musical", "Mystery", "News", "Reality-TV", "Romance",
                    "Sci-Fi", "Short", "Sport", "Talk-Show", "Thriller", "War", "Western" };
    private static String[] genereStringHebrew = { "הלועפ", "םירגובמ", "תואקתפרה", "היצמינא", "היפרגויב", "הידמוק", "עשפ", "ידועית", "המרד", "החפשמ", "היזטנפ",
                    "לפא", "ןועושעש", "הירוטסיה", "המיא", "הקיזומ", "רמזחמ", "ןירותסימ", "תושדח", "יטילאיר", "הקיטנמור", "ינוידב עדמ", "רצק", "טרופס", "חוריא",
                    "חתמ", "המחלמ", "ןוברעמ" };
    private static String cookieHeader="";

    private static boolean subtitleDownload = false;
    private static String login = "";
    private static String pass = "";
    private static String code = "";

    protected int plotLineMaxChar;
    protected int plotLineMax;
    protected TheTvDBPlugin tvdb;

    public SratimPlugin() {
        super(); // use IMDB if sratim doesn't know movie

        tvdb = new TheTvDBPlugin(); // use TVDB if sratim doesn't know series
        
        plotLineMaxChar = Integer.parseInt(PropertiesUtil.getProperty("sratim.plotLineMaxChar", "50"));        
        plotLineMax = Integer.parseInt(PropertiesUtil.getProperty("sratim.plotLineMax", "2"));
        
        subtitleDownload = Boolean.parseBoolean(PropertiesUtil.getProperty("sratim.subtitlte", "false"));
        login = PropertiesUtil.getProperty("sratim.username", "");
        pass = PropertiesUtil.getProperty("sratim.password", "");
        code = PropertiesUtil.getProperty("sratim.code", "");
        
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
                imdbId = getImdbId(mediaFile.getTitle(), mediaFile.getYear());
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

            sratimUrl = "http://www.sratim.co.il/" + detailsUrl;

            return sratimUrl;

        } catch (Exception e) {
            logger.severe("Failed retreiving sratim informations for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
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
        char[] ret = new char[text.length()];

        ret = text.toCharArray();

        LogicalToVisual(ret, BCT_R);
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

                ret = ret + "{br}";
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
        } catch (Exception e) {
            logger.severe("extractTag an exception occurred during tag extraction : " + e);
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        return src.replaceAll("\\<.*?>", "");
    }

    /**
     * Scan Sratim html page for the specified movie
     */
    protected boolean updateMediaInfo(Movie movie) {
        try {
            String imdbId = movie.getId(IMDB_PLUGIN_ID);
            if (imdbId != null && !imdbId.equalsIgnoreCase(Movie.UNKNOWN) && !movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {

                if (movie.getPosterURL() == null || movie.getPosterURL().startsWith("http://")) {

                    // Try to find hebrew poster using sub-baba.com web site and the movie english name
                    getSubBabaPosterURL(movie);
                }
            }

            String sratimUrl = movie.getId(SRATIM_PLUGIN_ID);

            String xml = webBrowser.request(sratimUrl);

            if (sratimUrl.contains("series")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                }
            }

            if (!movie.isOverrideTitle()) {
                String title = removeTrailBracket(HTMLTools.extractTag(xml, "<td valign=\"top\" style=\"width:100%\"", 0, "</td>"));

                movie.setTitle(logicalToVisual(title));
                movie.setTitleSort(title);
            }

            // Prefer IMDB rating
            if (movie.getRating() == -1) {
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "<span style=\"font-size:12pt;font-weight:bold\"><img alt=\"", 0, "/")));
            }

            movie.setDirector(logicalToVisual(HTMLTools.getTextAfterElem(xml, "במאי:")));
            movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "תאריך יציאה לקולנוע בחו\"ל:"));
            movie.setRuntime(logicalToVisual(removeTrailDot(HTMLTools.getTextAfterElem(xml, "אורך:"))));
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

            if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {

                if (sratimUrl.contains("series")) {
                    movie.setYear(HTMLTools.extractTag(xml, "<span style=\"font-weight:normal\">(", 0, ")"));
                } else {
                    movie.setYear(HTMLTools.getTextAfterElem(xml, "<span id=\"ctl00_ctl00_Body_Body_Box_ProductionYear\">"));
                }
            }

            movie.setCast(logicalToVisual(HTMLTools.extractTags(xml, "שחקנים:", "<br />", "<a href", "</a>")));

            // As last resort use sratim low quality poster
            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN) || ("sratim".equals(preferredPosterSearchEngine))) {
                movie.setPosterURL("http://www.sratim.co.il/movies/" + HTMLTools.extractTag(xml, "<img src=\"/movies/", 0, "\""));
            }

            if (movie.isTVShow()) {
                updateTVShowInfo(movie, xml);
            }
            else {
                // Download subtitle from the page
                downloadSubtitle(movie, movie.getFirstFile(), xml);
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving sratim informations for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
            e.printStackTrace();
        }
        return true;
    }

    /**
     * retrieve the sub-baba.com poster url matching the specified movie name.
     */
    protected void getSubBabaPosterURL(Movie movie) {

        String movieName = movie.getTitleSort();

        try {
            String searchURL = "http://www.sub-baba.com/search?page=search&type=all&submit=%E7%F4%F9&search=" + URLEncoder.encode(movieName, "iso-8859-8");

            String xml = webBrowser.request(searchURL);

            String posterID = Movie.UNKNOWN;
            boolean dvdCover = false;

            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("<div align=\"center\"><a href=\"content?id=", index);
                if (index == -1)
                    break;

                index += 40;

                index = xml.indexOf("width=\"", index);
                if (index == -1)
                    break;

                index += 7;

                endIndex = xml.indexOf("\"", index);
                if (endIndex == -1)
                    break;

                String scanWidth = xml.substring(index, endIndex);

                index = endIndex + 1;


                index = xml.indexOf("height=\"", index);
                if (index == -1)
                    break;

                index += 8;

                endIndex = xml.indexOf("\"", index);
                if (endIndex == -1)
                    break;

                String scanHeight = xml.substring(index, endIndex);

                index = endIndex + 1;

            
                index = xml.indexOf("<a href=\"content?id=", index);
                if (index == -1)
                    break;

                index += 20;

                endIndex = xml.indexOf("\">", index);
                if (endIndex == -1)
                    break;

                String scanPosterID = xml.substring(index, endIndex);

                index = endIndex + 2;

                index = xml.indexOf("<span dir=\"ltr\">", index);
                if (index == -1)
                    break;

                index += 16;

                endIndex = xml.indexOf("</span>", index);
                if (endIndex == -1)
                    break;

                String scanName = xml.substring(index, endIndex).trim();

                index = endIndex + 7;

                if (scanName.equalsIgnoreCase(movieName)) {
                    posterID = scanPosterID;

                    if (Integer.parseInt(scanWidth)>Integer.parseInt(scanHeight))
                        dvdCover = true;
                    else
                        dvdCover = false;
                }
            }

            if (!Movie.UNKNOWN.equals(posterID)) {
                String posterURL = "http://www.sub-baba.com/site/download.php?type=1&id=" + posterID;
                movie.setPosterURL(posterURL);

                if (dvdCover) {
                    // Cut the dvd cover into normal poster using the left side of the image
                    movie.setPosterSubimage("0, 0, 47, 100");
                }
            }

        } catch (Exception e) {
            return;
        }

    }

    private int parseRating(String rating) {
        try {
            return Math.round(Float.parseFloat(rating.replace(",", "."))) * 10;
        } catch (Exception e) {
            return -1;
        }
    }

    private String updateImdbId(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            imdbId = getImdbId(movie.getTitle(), movie.getYear());
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

        if (mainXML == null) {
            try {
                String sratimUrl = movie.getId(SRATIM_PLUGIN_ID);

                mainXML = webBrowser.request(sratimUrl);
            } catch (Exception e) {
                logger.severe("Failed retreiving sratim informations for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
                e.printStackTrace();
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
                    


                    if (scanPart.equals( Integer.toString(part) )) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" / ");
                        }
                        sb.append(logicalToVisual(scanName));
                    

                        try {
                        
                            // Get the episode page url
                            String xml = webBrowser.request("http://www.sratim.co.il/movies/view.aspx?id=" + scanID);
                            
                            
                            // Download subtitles
                            downloadSubtitle(movie, file, xml);
                                                        
                        } catch (Exception e) {
                            logger.severe("Error : " + e.getMessage());
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

        if (subtitleDownload == false){
            mf.setSubtitlesExchange(true);
            return;
        }
            
        if (movie.isTrailer()){
            mf.setSubtitlesExchange(true);
            return;
        }

            
        // Check if this movie already have subtitles for it
        String path = mf.getFile().getAbsolutePath();
        int lindex = path.lastIndexOf(".");
        String basename = path.substring(0, lindex + 1);

        File subtitleFile = new File(basename + "srt");

        if (subtitleFile.exists()) {
            mf.setSubtitlesExchange(true);
            return;
        }


        // Get the file base name
        path = mf.getFile().getName().toUpperCase();
        lindex = path.lastIndexOf(".");
        if (lindex==-1)
            return;
        
        basename = path.substring(0, lindex);

        // Check if this is a bluray file
        boolean bluRay = false;
        if (path.endsWith(".M2TS") && path.startsWith("0"))
            bluRay = true;
            
        basename = basename.replace('.',' ');

        logger.finest("downloadSubtitle: " +subtitleFile.getAbsolutePath() );
        logger.finest("basename: " +basename );
        logger.finest("bluRay: " +bluRay );

        int bestFPSCount=0;
        int bestBlurayCount=0;
        int bestBlurayFPSCount=0;
        
        String bestFPSID="";
        String bestBlurayID="";
        String bestBlurayFPSID="";
        String bestFileID="";


        int index = 0;
        int endIndex = 0;
        
        
        // Check that hebrew subtitle exist
        String hebrewSub = HTMLTools.getTextAfterElem(mainXML, "<img src=\"/images/flags/Hebrew.gif\" alt=\"");

        logger.finest("hebrewSub: " +hebrewSub );
        
        // Check that there is no 0 hebrew sub
        if (hebrewSub.startsWith("0 ")) {
            logger.finest("No Hebrew subtitles");

            return; 
        }        
        
        
        while (true) {

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

            String scanFileName = mainXML.substring(index, endIndex).toUpperCase().replace('.',' ');



            index = mainXML.indexOf("</b> ", index);
            if (index == -1)
                break;

            index += 5;
            
            endIndex = mainXML.indexOf("<", index);
            if (endIndex == -1)
                break;

            String scanFPS = mainXML.substring(index, endIndex);
    
    

            logger.finest("scanFileName: " + scanFileName + " scanFPS: " + scanFPS + " scanID: " + scanID + " scanCount: "+scanCount + " scanDiscs: " + scanDiscs + " scanFormat: " +scanFormat);


            // Check if this is a format supported by the popcorn hour
            if (!scanDiscs.equals("1") || !scanFormat.equals("SubRip"))
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
                } catch (Exception e) {
                }
    
                float scanFPSFloat = 0;
                try {                
                    scanFPSFloat = Float.parseFloat(scanFPS);
                } catch (Exception e) {
                }
    
                
                logger.finest("FPS: "+movie.getFps()+" scanFPS: "+scanFPSFloat);
                
                if ( bluRay && ( (scanFileName.indexOf("BRRIP") != -1) || 
                                 (scanFileName.indexOf("BDRIP") != -1) || 
                                 (scanFileName.indexOf("BLURAY") != -1) ||
                                 (scanFileName.indexOf("BLU-RAY") != -1) ||
                                 (scanFileName.indexOf("HDDVD") != -1)
                                  )) {

                    if ((scanFPSFloat == 0 ) && (scanCountInt > bestBlurayCount)) {
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
                
            } catch (Exception e) {
            }

        }
        
        // Select the best subtitles ID
        String bestID="";
        
        // Check for exact file name match
        if (!bestFileID.equals("")) {
            logger.finest("Best Filename");        
            bestID = bestFileID;
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
        
        {
            logger.finest("No subtitle found");
            return;
        }
        
        logger.finest("bestID: " + bestID);

        if (downloadSubtitleZip("http://www.sratim.co.il/movies/subtitles/download.aspx?"+bestID, subtitleFile)==false) {
            logger.severe("Error - Sratim subtitle download failed");
            return;
        }
        
        mf.setSubtitlesExchange(true);
        movie.setSubtitles(true);
    }


    public boolean downloadSubtitleZip(String subDownloadLink, File subtitleFile) {

        try {
            URL url = new URL(subDownloadLink);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("Cookie", cookieHeader);
            
            logger.finest("cookieHeader:"+ cookieHeader );
            
            InputStream inputStream = connection.getInputStream();

            String contentType = connection.getContentType();

            logger.finest("contentType:"+ contentType );

            // Check that the content iz zip and that the site did not blocked the download
            if (!contentType.equals("application/x-zip-compressed")) {
                logger.severe("********** Error - Sratim subtitle download limit may have been reached. Suspending subtitle download.");
                
                subtitleDownload=false;
                return false;
            }
            
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(inputStream);

            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) 
            { 
                //for each entry to be extracted
                String entryName = zipentry.getName();
                
                logger.finest("ZIP entryname: " + entryName);
                
                
                // Check if this is a subtitle file
                if (entryName.toUpperCase().endsWith(".SRT")) {
                
                    int n;
                    FileOutputStream fileoutputstream;

                    fileoutputstream = new FileOutputStream(subtitleFile);             

                    while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
                        fileoutputstream.write(buf, 0, n);

                    fileoutputstream.close();

                    zipinputstream.close();
                    return true;
                }
                
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }

            zipinputstream.close();
            
        } catch (Exception e) {
            logger.severe("Error : " + e.getMessage());
            return false;
        }
        
        return false;
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
        } catch (Exception e) {
        }

        if (!cookieHeader.equals(""))
        {
            // Verify cookie by loading main page
            try {            
                URL url = new URL("http://www.sratim.co.il/");
                HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
                connection.setRequestProperty("Cookie", cookieHeader);
            
                //Get Response    
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer(); 
                while((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
                String xml=response.toString();
            
                if (xml.indexOf("logout=1")!=-1) {
                    logger.finest("Sratim Subtitles Cookies Valid");
                    return;
                }
                    
            } catch (Exception e) {
                logger.severe("Error : " + e.getMessage());
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
        } catch (Exception e) {
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
                HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(post.getBytes().length));
                connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
                connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                connection.setRequestProperty("Referer", "http://www.sratim.co.il/users/login.aspx");

                connection.setRequestProperty("Cookie", cookieHeader);
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);

                //Send request
                DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
                wr.writeBytes (post);
                wr.flush ();
                wr.close ();


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

                //Get Response    
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer(); 
                while((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
                String xml=response.toString();

                if (xml.indexOf("<h2>Object moved to <a href=\"%2fdefault.aspx%3f\">here</a>.</h2>")!=-1) {
                
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

            } catch (Exception e) {
                logger.severe("Error : " + e.getMessage());
                return;
            }

        }
        
        try {            
            URL url = new URL("http://www.sratim.co.il/users/login.aspx");
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());

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
            connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("Cookie", cookieHeader);
            
            // Write the jpg code to the file
            File imageFile = new File("sratim.jpg");
            FileTools.copy(connection.getInputStream(), new FileOutputStream(imageFile));


            // Exit and wait for the user to type the jpg code
            logger.severe("#############################################################################");
            logger.severe("### Open \"sratim.jpg\" file, and write the code in the sratim.code field ###");
            logger.severe("#############################################################################");
            System.exit(0);

        } catch (Exception e) {
            logger.severe("Error : " + e.getMessage());
            return;
        }
        
    }

    public static String removeChar(String s, char c) {
       String r = "";
       for (int i = 0; i < s.length(); i ++) {
          if (s.charAt(i) != c) r += s.charAt(i);
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
}
