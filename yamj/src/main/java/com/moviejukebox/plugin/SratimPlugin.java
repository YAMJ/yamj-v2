/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;

public class SratimPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(SratimPlugin.class);
    private static final String LOG_MESSAGE = "Sratim Plugin: ";
    public static final String SRATIM_PLUGIN_ID = "sratim";
    public static final String SRATIM_PLUGIN_SUBTITLE_ID = "sratim_subtitle";
    protected static String PHPSESSID = "COOKIE";
    private static AbstractStringMetric metric = new MongeElkan();
    private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*sratim.co.il[^\\s<>`\"\\[\\]]*");
    private static String[] genereStringEnglish = {"Action", "Adult", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama",
        "Family", "Fantasy", "Film-Noir", "Game-Show", "History", "Horror", "Music", "Musical", "Mystery", "News", "Reality-TV", "Romance",
        "Sci-Fi", "Short", "Sport", "Talk-Show", "Thriller", "War", "Western"};
    private static String[] genereStringHebrew = {"פעולה", "מבוגרים", "הרפתקאות", "אנימציה", "ביוגרפיה", "קומדיה", "פשע", "תיעודי", "דרמה", "משפחה", "פנטזיה",
        "אפל", "שעשועון", "היסטוריה", "אימה", "מוזיקה", "מחזמר", "מיסתורין", "חדשות", "ריאליטי", "רומנטיקה", "מדע בדיוני", "קצר", "ספורט", "אירוח",
        "מתח", "מלחמה", "מערבון"};
    private static boolean subtitleDownload = false;
    private static boolean keepEnglishGenres = false;
    private static boolean bidiSupport = true;
    protected static final String RECAPTCHA_URL = "http://www.google.com/recaptcha/api/challenge?k=6LfK1LsSAAAAACdKnQfBi_xCdaMxyd2I9qL5PRH8";
    protected static final Pattern CHALLENGE_ID = Pattern.compile("challenge : '([^']+)'");
    protected int plotLineMaxChar;
    protected int plotLineMax;
    protected TheTvDBPlugin tvdb;
    protected static String preferredPosterSearchEngine;
    protected static String lineBreak;

    public SratimPlugin() {
        super(); // use IMDB if sratim doesn't know movie

        tvdb = new TheTvDBPlugin(); // use TVDB if sratim doesn't know series

        plotLineMaxChar = PropertiesUtil.getIntProperty("sratim.plotLineMaxChar", "50");
        plotLineMax = PropertiesUtil.getIntProperty("sratim.plotLineMax", "2");

        subtitleDownload = PropertiesUtil.getBooleanProperty("sratim.subtitle", FALSE);
        preferredPosterSearchEngine = PropertiesUtil.getProperty("imdb.alternate.poster.search", "google");
        keepEnglishGenres = PropertiesUtil.getBooleanProperty("sratim.KeepEnglishGenres", FALSE);
        bidiSupport = PropertiesUtil.getBooleanProperty("sratim.BidiSupport", TRUE);

        lineBreak = PropertiesUtil.getProperty("mjb.lineBreak", "{br}");
    }

    @Override
    public String getPluginID() {
        return SRATIM_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        boolean retval = false;

        String id = movie.getId(SRATIM_PLUGIN_ID);
        if (StringTools.isNotValidString(id)) {
            // collect missing information from IMDB or TVDB before sratim
            if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                retval = super.scan(movie);
            } else {
                retval = tvdb.scan(movie);
            }

            // Translate genres to Hebrew
            translateGenres(movie);

            id = getSratimId(movie);
        }

        if (StringTools.isValidString(id)) {
            retval = updateMediaInfo(movie);
        }

        return retval;
    }

    /**
     * retrieve the sratim id for the movie
     */
    public String getSratimId(Movie movie) {

        try {
            String imdbId = updateImdbId(movie);

            if (StringTools.isNotValidString(imdbId)) {
                return Movie.UNKNOWN;
            }

            String xml = webBrowser.request("http://www.sratim.co.il/browse.php?q=imdb%3A" + imdbId, Charset.forName("UTF-8"));

            Boolean missingFromSratimDB = xml.contains("לא נמצאו תוצאות העונות לבקשתך");
            if (missingFromSratimDB) {
                return Movie.UNKNOWN;
            }

            if (subtitleDownload) {
                String subtitlesID = HTMLTools.extractTag(xml, "<a href=\"subtitles.php?", 0, "\"");
                int subid = subtitlesID.lastIndexOf("mid=");
                if (subid > -1 && subtitlesID.length() > subid) {
                    String subtitle = new String(subtitlesID.substring(subid + 4));
                    movie.setId(SRATIM_PLUGIN_SUBTITLE_ID, subtitle);
                }
            }
            
            String tmp_url = HTMLTools.extractTag(xml,"<div class=\"browse_title_name\"","</div>");           
            String detailsUrl = HTMLTools.extractTag(tmp_url, "<a href=\"view.php?", 0, "\"");
            if (StringTools.isNotValidString(detailsUrl)) {
                // try TV series view page
                detailsUrl = HTMLTools.extractTag(tmp_url, "<a href=\"viewseries.php?", 0, "\"");
            }
            if (StringTools.isNotValidString(detailsUrl)) {
                return Movie.UNKNOWN;
            }

            // update movie ids
            int id = detailsUrl.lastIndexOf("id=");

            if (id > -1 && detailsUrl.length() > id) {
                String movieId = new String(detailsUrl.substring(id + 3));
                int idEnd = movieId.indexOf("&");
                if (idEnd > -1 ) {
                    movieId = movieId.substring(0, idEnd); 
                }
                movie.setId(SRATIM_PLUGIN_ID, movieId);
                return movieId;
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retrieving sratim informations for movie : " + movie.getTitle());
            logger.error(LOG_MESSAGE + "" + error.getMessage());
        }

        return Movie.UNKNOWN;
    }

    // Translate IMDB genres to Hebrew
    protected void translateGenres(Movie movie) {
        if (keepEnglishGenres) {
            return;
        } else if (!OverrideTools.checkOverwriteGenres(movie, SRATIM_PLUGIN_ID)) {
            return;
        }

        TreeSet<String> genresHeb = new TreeSet<String>();

        // Translate genres to Hebrew
        for (String genre : movie.getGenres()) {

            int i;
            for (i = 0; i < genereStringEnglish.length; i++) {
                if (genre.equals(genereStringEnglish[i])) {
                    break;
                }
            }

            if (i < genereStringEnglish.length) {
                if (bidiSupport) { // flip genres to for visual Hebrew display
                    genresHeb.add(genereStringHebrew[i]);
                } else {
                    genresHeb.add(logicalToVisual(genereStringHebrew[i]));
                }
            } else {
                if (bidiSupport) {
                    genresHeb.add("אחר");
                } else {
                    genresHeb.add("רחא");
                }
            }
        }

        // Set translated IMDB genres
        movie.setGenres(genresHeb, SRATIM_PLUGIN_ID);
    }

    // Porting from my old code in c++
    public static final int BCT_L = 0;
    public static final int BCT_R = 1;
    public static final int BCT_N = 2;
    public static final int BCT_EN = 3;
    public static final int BCT_ES = 4;
    public static final int BCT_ET = 5;
    public static final int BCT_CS = 6;

    // Return the type of a specific character
    private static int getCharType(char charToCheck) {
        if (((charToCheck >= 'א') && (charToCheck <= 'ת'))) {
            return BCT_R;
        }

        if ((charToCheck == 0x26) || (charToCheck == 0x40) || ((charToCheck >= 0x41) && (charToCheck <= 0x5A))
                || ((charToCheck >= 0x61) && (charToCheck <= 0x7A)) || ((charToCheck >= 0xC0) && (charToCheck <= 0xD6))
                || ((charToCheck >= 0xD8) && (charToCheck <= 0xDF))) {
            return BCT_L;
        }

        if (((charToCheck >= 0x30) && (charToCheck <= 0x39))) {
            return BCT_EN;
        }

        if ((charToCheck == 0x2E) || (charToCheck == 0x2F)) {
            return BCT_ES;
        }

        if ((charToCheck == 0x23) || (charToCheck == 0x24) || ((charToCheck >= 0xA2) && (charToCheck <= 0xA5)) || (charToCheck == 0x25)
                || (charToCheck == 0x2B) || (charToCheck == 0x2D) || (charToCheck == 0xB0) || (charToCheck == 0xB1)) {
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
        int pos, checkPos;
        int before, after;

        pos = 0;

        while (pos < stringToResolve.length) {
            // Check if this is natural type and we need to cahnge it
            if (charType[pos] == BCT_N) {
                // Search for the type of the previous strong type
                checkPos = pos - 1;

                while (true) {
                    if (checkPos < 0) {
                        // Default language
                        before = defaultDirection;
                        break;
                    }

                    if (charType[checkPos] == BCT_R) {
                        before = BCT_R;
                        break;
                    }

                    if (charType[checkPos] == BCT_L) {
                        before = BCT_L;
                        break;
                    }

                    checkPos--;
                }

                checkPos = pos + 1;

                // Search for the type of the next strong type
                while (true) {
                    if (checkPos >= stringToResolve.length) {
                        // Default language
                        after = defaultDirection;
                        break;
                    }

                    if (charType[checkPos] == BCT_R) {
                        after = BCT_R;
                        break;
                    }

                    if (charType[checkPos] == BCT_L) {
                        after = BCT_L;
                        break;
                    }

                    checkPos++;
                }

                // Change the natural depanded on the strong type before and after
                if ((before == BCT_R) && (after == BCT_R)) {
                    charType[pos] = BCT_R;
                } else if ((before == BCT_L) && (after == BCT_L)) {
                    charType[pos] = BCT_L;
                } else {
                    charType[pos] = defaultDirection;
                }
            }

            pos++;
        }

        /*
         * R N R -> R R R L N L -> L L L
         *
         * L N R -> L e R (e=default) R N L -> R e L (e=default)
         */
    }

    // Resolving Implicit Levels
    private static void resolveImplictLevels(char[] stringToResolve, int[] charType, int[] level) {
        int pos;

        pos = 0;

        while (pos < stringToResolve.length) {
            if (charType[pos] == BCT_L) {
                level[pos] = 2;
            }

            if (charType[pos] == BCT_R) {
                level[pos] = 1;
            }

            if (charType[pos] == BCT_EN) {
                level[pos] = 2;
            }

            pos++;
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
        int[] charType;
        int[] level;
        int len;

        len = stringToConvert.length;

        // Allocate CharType and Level arrays
        charType = new int[len];

        level = new int[len];

        // Set the string char types
        setStringCharType(stringToConvert, charType);

        // Resolving Weak Types
        resolveWeakType(stringToConvert, charType);

        // Resolving Natural Types
        resolveNaturalType(stringToConvert, charType, defaultDirection);

        // Resolving Implicit Levels
        resolveImplictLevels(stringToConvert, charType, level);

        // Reordering Resolved Levels
        reorderResolvedLevels(stringToConvert, level);
    }

    private static boolean isCharNatural(char c) {
        if ((c == ' ') || (c == '-')) {
            return true;
        }

        return false;
    }

    private static String logicalToVisual(String text) {
        if (bidiSupport) {
            return text; // resolves issue #1706. model >A110 Bidi support imlemented - no need to flip hebrew chars
        }
        char[] ret;

        ret = text.toCharArray();
        if (containsHebrew(ret)) {
            logicalToVisual(ret, BCT_R);
        }
        return (new String(ret));
    }

    private static List<String> logicalToVisual(List<String> text) {
        List<String> ret = new ArrayList<String>();

        for (int i = 0; i < text.size(); i++) {
            ret.add(logicalToVisual(text.get(i)));

        }

        return ret;
    }

    private static String removeTrailDot(String text) {
        int dot = text.lastIndexOf('.');

        if (dot == -1) {
            return text;
        }

        return new String(text.substring(0, dot));
    }

    @SuppressWarnings("unused")
    private static String removeTrailBracket(String text) {
        int bracket = text.lastIndexOf(" (");

        if (bracket == -1) {
            return text;
        }

        return new String(text.substring(0, bracket));
    }

    private static String breakLongLines(String text, int lineMaxChar, int lineMax) {
        StringBuilder ret = new StringBuilder();

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
                    return (ret + "..." + logicalToVisual(text.substring(lineStart, lastBreakPos).trim()));
                }

                ret.append(logicalToVisual(text.substring(lineStart, lastBreakPos).trim()));

                lineStart = lastBreakPos;
                lastBreakPos = 0;

                ret.append(lineBreak);
            }

            scanPos++;
        }

        ret.append(logicalToVisual(text.substring(lineStart, scanPos).trim()));

        return ret.toString();
    }

    protected String extractTag(String src, String tagStart, String tagEnd) {
        int beginIndex = src.indexOf(tagStart);
        if (beginIndex < 0) {
            // logger.debug("extractTag value= Unknown");
            return Movie.UNKNOWN;
        }
        try {
            String subString = new String(src.substring(beginIndex + tagStart.length()));
            int endIndex = subString.indexOf(tagEnd);
            if (endIndex < 0) {
                // logger.debug("extractTag value= Unknown");
                return Movie.UNKNOWN;
            }
            subString = new String(subString.substring(0, endIndex));

            String value = HTMLTools.decodeHtml(subString.trim());
            // logger.debug("extractTag value=" + value);
            return value;
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "extractTag an exception occurred during tag extraction : " + error);
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        return src.replaceAll("\\<.*?>", "");
    }

    protected List<String> removeHtmlTags(List<String> src) {
        List<String> output = new ArrayList<String>();

        for (int i = 0; i < src.size(); i++) {
            output.add(removeHtmlTags(src.get(i)));
        }
        return output;
    }

    /**
     * Scan Sratim html page for the specified movie
     */
    private boolean updateMediaInfo(Movie movie) {
        try {

            String sratimUrl = "http://www.sratim.co.il/view.php?id=" + movie.getId(SRATIM_PLUGIN_ID);

            String xml = webBrowser.request(sratimUrl, Charset.forName("UTF-8"));

            if (xml.contains("צפייה בפרופיל סדרה")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                }
            }

            if (OverrideTools.checkOverwriteTitle(movie, SRATIM_PLUGIN_ID)) {
                String title = extractMovieTitle(xml);
                if (!"None".equalsIgnoreCase(title)) {
                    movie.setTitle(logicalToVisual(title), SRATIM_PLUGIN_ID);
                    movie.setTitleSort(title);
                }
            }

            // Prefer IMDB rating
            if (movie.getRating() == -1) {
                movie.addRating(SRATIM_PLUGIN_ID, parseRating(HTMLTools.extractTag(xml, "width=\"120\" height=\"12\" title=\"", 0, " ")));
            }

            String director = logicalToVisual(HTMLTools.getTextAfterElem(xml, "בימוי:"));
            if (StringTools.isValidString(director)) {
                if (OverrideTools.checkOverwriteDirectors(movie, SRATIM_PLUGIN_ID)) {
                    movie.setDirector(director, SRATIM_PLUGIN_ID);
                }
                if (OverrideTools.checkOverwritePeopleDirectors(movie, SRATIM_PLUGIN_ID)) {
                    movie.setPeopleDirectors(Collections.singleton(director), SRATIM_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, SRATIM_PLUGIN_ID)) {
                movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "י' בעולם:"), SRATIM_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, SRATIM_PLUGIN_ID)) {
                movie.setRuntime(logicalToVisual(removeTrailDot(HTMLTools.getTextAfterElem(xml, "אורך זמן:"))), SRATIM_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCountry(movie, SRATIM_PLUGIN_ID)) {
                movie.setCountry(logicalToVisual(HTMLTools.getTextAfterElem(xml, "מדינה:")), SRATIM_PLUGIN_ID);
            }

            // only add if no genres set until now
            if (movie.getGenres().isEmpty()) {
                String genres = HTMLTools.getTextAfterElem(xml, "ז'אנרים:");
                List<String> newGenres = new ArrayList<String>();
                for (String genre : genres.split(" *, *")) {
                    newGenres.add(logicalToVisual(Library.getIndexingGenre(genre)));
                }
                movie.setGenres(newGenres, SRATIM_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwritePlot(movie, SRATIM_PLUGIN_ID)) {
                String tmpPlot = removeHtmlTags(extractTag(xml, "<meta name=\"description\" content=\"", "\""));
                //Set Hebrew plot only if it contains substantial number of characters, otherwise IMDB plot will be used.
                if ((tmpPlot.length() > 30)) {
                    movie.setPlot(breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax), SRATIM_PLUGIN_ID);
                }
            }

            boolean overrideActors = OverrideTools.checkOverwriteActors(movie, SRATIM_PLUGIN_ID);
            boolean overridePeopleActors = OverrideTools.checkOverwritePeopleActors(movie, SRATIM_PLUGIN_ID);
            if (overrideActors || overridePeopleActors) {
                List<String> actors = logicalToVisual(removeHtmlTags(HTMLTools.extractTags(xml, "שחקנים:", "</tr>", "<a href", "</a>")));
                if (overrideActors) {
                    movie.setCast(actors, SRATIM_PLUGIN_ID);
                }
                if (overridePeopleActors) {
                    movie.setPeopleCast(actors, SRATIM_PLUGIN_ID);
                }
            }

            if (movie.isTVShow()) {
                updateTVShowInfo(movie, xml);
            } else {
                // Download subtitle from the page
                downloadSubtitle(movie, movie.getFirstFile());
            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retrieving information for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));
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

        if (StringTools.isNotValidString(imdbId)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }
        
        return imdbId;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        scanTVShowTitles(movie, null);
    }

    public void scanTVShowTitles(Movie movie, String mainXML) {
        if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
            return;
        }

        String seasonXML = null;

        String newMainXML = mainXML;
        try {
            if (newMainXML == null) {
                String sratimId = movie.getId(SRATIM_PLUGIN_ID);
                newMainXML = webBrowser.request("http://www.sratim.co.il/view.php?id=" + sratimId, Charset.forName("UTF-8"));
            }

            int season = movie.getSeason();

            int index = 0;
            int endIndex;

            String seasonUrl;
            String seasonYear;

            // Find the season URL
            while (true) {
                index = newMainXML.indexOf("<span class=\"smtext\"><a href=\"", index);
                if (index == -1) {
                    return;
                }

                index += 30;

                endIndex = newMainXML.indexOf('\"', index);
                if (endIndex == -1) {
                    return;
                }

                String scanUrl = new String(newMainXML.substring(index, endIndex));

                index = newMainXML.indexOf("class=\"smtext\">עונה ", index);
                if (index == -1) {
                    return;
                }

                index += 20;

                endIndex = newMainXML.indexOf("</a>", index);
                if (endIndex == -1) {
                    return;
                }

                String scanSeason = new String(newMainXML.substring(index, endIndex));

                index = newMainXML.indexOf("class=\"smtext\">", index);
                if (index == -1) {
                    return;
                }

                index += 15;

                endIndex = newMainXML.indexOf('<', index);
                if (endIndex == -1) {
                    return;
                }

                String scanYear = new String(newMainXML.substring(index, endIndex));

                int scanSeasontInt;
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

            if (OverrideTools.checkOverwriteYear(movie, SRATIM_PLUGIN_ID)) {
                movie.setYear(seasonYear, SRATIM_PLUGIN_ID);
            }

            seasonXML = webBrowser.request(seasonUrl, Charset.forName("UTF-8"));

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving information for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
            logger.error(SystemTools.getStackTrace(error));

            if (newMainXML == null) {
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
                int endIndex;

                // Go over the page and sacn for episode links
                while (true) {
                    index = seasonXML.indexOf("<td style=\"padding-right:6px;font-size:15px;\"><a href=\"", index);
                    if (index == -1) {
                        return;
                    }

                    index += 55;

                    endIndex = seasonXML.indexOf('\"', index);
                    if (endIndex == -1) {
                        return;
                    }

                    String scanUrl = new String(seasonXML.substring(index, endIndex));

                    index = seasonXML.indexOf("<b>פרק ", index);
                    if (index == -1) {
                        return;
                    }

                    index += 7;

                    endIndex = seasonXML.indexOf(':', index);
                    if (endIndex == -1) {
                        return;
                    }

                    String scanPart = new String(seasonXML.substring(index, endIndex));

                    index = seasonXML.indexOf("</b> ", index);
                    if (index == -1) {
                        return;
                    }

                    index += 5;

                    endIndex = seasonXML.indexOf("</a>", index);
                    if (endIndex == -1) {
                        return;
                    }

                    String scanName = new String(seasonXML.substring(index, endIndex));

                    if (scanPart.equals(Integer.toString(part))) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(Movie.SPACE_SLASH_SPACE);
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
                                    String tmpPlot = removeHtmlTags(new String(xml.substring(plotStartIndex + plotStart.length(), endPlotIndex)));
                                    file.setPlot(part, breakLongLines(tmpPlot, plotLineMaxChar, plotLineMax));
                                    logger.debug(LOG_MESSAGE + "Plot found : http://www.sratim.co.il/" + scanUrl + " - " + file.getPlot(part));
                                }
                            }

                            // Download subtitles
                            // store the subtitles id in the movie ids map, make sure to remove the prefix "1" from the id
                            int findId = scanUrl.indexOf("id=");
                            String subId = new String(scanUrl.substring(findId + 4));
                            movie.setId(SRATIM_PLUGIN_SUBTITLE_ID, subId);
                            downloadSubtitle(movie, file);

                        } catch (Exception error) {
                            logger.error(LOG_MESSAGE + "Error - " + error.getMessage());
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

    protected void updateTVShowInfo(Movie movie, String mainXML) throws IOException {
        scanTVShowTitles(movie, mainXML);
    }

    public void downloadSubtitle(Movie movie, MovieFile mf) throws IOException {

        if (!subtitleDownload) {
            mf.setSubtitlesExchange(true);
            return;
        }

        // Get the file base name
        String path = mf.getFile().getName().toUpperCase();
        int lindex = path.lastIndexOf('.');
        if (lindex == -1) {
            return;
        }

        String basename = new String(path.substring(0, lindex));

        // Check if this is a bluray file
        boolean bluRay = false;
        if (path.endsWith(".M2TS") && path.startsWith("0")) {
            bluRay = true;
        }

        if (movie.isExtra()) {
            mf.setSubtitlesExchange(true);
            return;
        }

        // Check if this movie already have subtitles for it (.srt and .sub)
        if (hasExistingSubtitles(mf, bluRay)) {
            mf.setSubtitlesExchange(true);
            return;
        }

        basename = basename.replace('.', ' ').replace('-', ' ').replace('_', ' ');

        logger.debug(LOG_MESSAGE + "downloadSubtitle: " + mf.getFile().getAbsolutePath());
        logger.debug(LOG_MESSAGE + "basename: " + basename);
        logger.debug(LOG_MESSAGE + "bluRay: " + bluRay);

        int bestFPSCount = 0;
        int bestBlurayCount = 0;
        int bestBlurayFPSCount = 0;

        String bestFPSID = "";
        String bestBlurayID = "";
        String bestBlurayFPSID = "";
        String bestFileID = "";
        String bestSimilar = "";

        // retrieve subtitles page

        String subID = movie.getId(SRATIM_PLUGIN_SUBTITLE_ID);
        String mainXML = webBrowser.request("http://www.sratim.co.il/subtitles.php?mid=" + subID, Charset.forName("UTF-8"));

        int index = 0;
        int endIndex;

        // find the end of hebrew subtitles section, to prevent downloading non-hebrew ones
        int endHebrewSubsIndex = findEndOfHebrewSubtitlesSection(mainXML);

        // Check that hebrew subtitle exist
        String hebrewSub = HTMLTools.getTextAfterElem(mainXML, "<img src=\"images/Flags/1.png");

        logger.debug(LOG_MESSAGE + "hebrewSub: " + hebrewSub);

        // Check that there is no 0 hebrew sub
        if (Movie.UNKNOWN.equals(hebrewSub)) {
            logger.debug(LOG_MESSAGE + "No Hebrew subtitles");
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

            endIndex = mainXML.indexOf('\"', index);
            if (endIndex == -1) {
                break;
            }

            String scanID = new String(mainXML.substring(index, endIndex));

            //
            // scanDiscs
            //
            index = mainXML.indexOf("src=\"images/cds/cd", index);
            if (index == -1) {
                break;
            }

            index += 18;

            endIndex = mainXML.indexOf('.', index);
            if (endIndex == -1) {
                break;
            }

            String scanDiscs = new String(mainXML.substring(index, endIndex));

            //
            // scanFileName
            //
            index = mainXML.indexOf("subtitle_title\" style=\"direction:ltr;\" title=\"", index);
            if (index == -1) {
                break;
            }

            index += 46;

            endIndex = mainXML.indexOf('\"', index);
            if (endIndex == -1) {
                break;
            }

            String scanFileName = new String(mainXML.substring(index, endIndex)).toUpperCase().replace('.', ' ');
            // removing all characters causing metric to hang.
            scanFileName = scanFileName.replaceAll("-|\u00A0", " ").replaceAll(" ++", " ");

            //
            // scanFormat
            //
            index = mainXML.indexOf("\u05e4\u05d5\u05e8\u05de\u05d8", index); // the hebrew letters for the word "format"
            if (index == -1) {
                break;
            }

            index += 6;

            endIndex = mainXML.indexOf(',', index);
            if (endIndex == -1) {
                break;
            }

            String scanFormat = new String(mainXML.substring(index, endIndex));

            //
            // scanFPS
            //
            index = mainXML.indexOf("\u05dc\u05e9\u05e0\u0027\u003a", index); // the hebrew letters for the word "for sec':" lamed shin nun ' :
            if (index == -1) {
                break;
            }

            index += 5;

            endIndex = mainXML.indexOf('<', index);
            if (endIndex == -1) {
                break;
            }

            String scanFPS = new String(mainXML.substring(index, endIndex));

            //
            // scanCount
            //
            index = mainXML.indexOf("subt_date\"><span class=\"smGray\">", index);
            if (index == -1) {
                break;
            }

            index += 32;

            endIndex = mainXML.indexOf(' ', index);
            if (endIndex == -1) {
                break;
            }

            String scanCount = new String(mainXML.substring(index, endIndex));

            // Check for best text similarity
            float result = metric.getSimilarity(basename, scanFileName);
            if (result > maxMatch) {
                maxMatch = result;
                bestSimilar = scanID;
            }

            logger.debug(LOG_MESSAGE + "scanFileName: " + scanFileName + " scanFPS: " + scanFPS + " scanID: " + scanID + " scanCount: " + scanCount
                    + " scanDiscs: " + scanDiscs + " scanFormat: " + scanFormat + " similarity: " + result);

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
                int scanCountInt;
                try {
                    scanCountInt = Integer.parseInt(scanCount);
                } catch (Exception error) {
                    scanCountInt = 0;
                }

                float scanFPSFloat;
                try {
                    scanFPSFloat = Float.parseFloat(scanFPS);
                } catch (Exception error) {
                    scanFPSFloat = 0;
                }

                logger.debug(LOG_MESSAGE + "FPS: " + movie.getFps() + " scanFPS: " + scanFPSFloat);

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
                // Ignored
            }
        }

        // Select the best subtitles ID
        String bestID;

        // Check for exact file name match
        if (!bestFileID.equals("")) {
            logger.debug(LOG_MESSAGE + "Best Filename");
            bestID = bestFileID;
        } else if (maxMatch >= matchThreshold) {
            // Check for text similarity match, similarity threshold takes precedence over FPS check
            logger.debug(LOG_MESSAGE + "Best Text Similarity threshold");
            bestID = bestSimilar;
        } else if (!bestBlurayFPSID.equals("")) {
            // Check for bluray match
            logger.debug(LOG_MESSAGE + "Best Bluray FPS");
            bestID = bestBlurayFPSID;
        } else if (!bestBlurayID.equals("")) {
            // Check for bluray match
            logger.debug(LOG_MESSAGE + "Best Bluray");
            bestID = bestBlurayID;
        } else if (!bestFPSID.equals("")) {
            // Check for fps match
            logger.debug(LOG_MESSAGE + "Best FPS");
            bestID = bestFPSID;
        } else if (maxMatch > 0) {
            // Check for text match, now just choose the best similar name
            logger.debug(LOG_MESSAGE + "Best Similar");
            bestID = bestSimilar;
        } else {
            logger.debug(LOG_MESSAGE + "No subtitle found");
            return;
        }

        logger.debug(LOG_MESSAGE + "bestID: " + bestID);

        // reconstruct movie filename with full path
        String orgName = mf.getFile().getAbsolutePath();
        File subtitleFile = new File(new String(orgName.substring(0, orgName.lastIndexOf('.'))));
        if (!downloadSubtitleZip(movie, "http://www.sratim.co.il/downloadsubtitle.php?id=" + bestID, subtitleFile, bluRay)) {
            logger.error(LOG_MESSAGE + "Error - Subtitle download failed");
            return;
        }

        mf.setSubtitlesExchange(true);
        SubtitleTools.addMovieSubtitle(movie, "YES");
    }

    public boolean downloadSubtitleZip(Movie movie, String subDownloadLink, File subtitleFile, boolean bluray) {

        boolean found = false;
        ZipInputStream zipInputStream = null;
        OutputStream fileOutputStream = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(subDownloadLink);
            connection = (HttpURLConnection) (url.openConnection());
            inputStream = connection.getInputStream();

            String contentType = connection.getContentType();

            logger.debug(LOG_MESSAGE + "contentType:" + contentType);

            // Check that the content is zip and that the site did not blocked the download
            if (!contentType.equals("application/octet-stream")) {
                logger.error(LOG_MESSAGE + "********** Error - Sratim subtitle download limit may have been reached. Suspending subtitle download.");

                subtitleDownload = false;
                return false;
            }

            Collection<MovieFile> parts = movie.getMovieFiles();
            Iterator<MovieFile> partsIter = parts.iterator();

            byte[] buf = new byte[1024];
            ZipEntry zipentry;
            zipInputStream = new ZipInputStream(inputStream);

            zipentry = zipInputStream.getNextEntry();
            while (zipentry != null) {
                // for each entry to be extracted
                String entryName = zipentry.getName();

                logger.debug(LOG_MESSAGE + "ZIP entryname: " + entryName);

                // Check if this is a subtitle file
                if (entryName.toUpperCase().endsWith(".SRT") || entryName.toUpperCase().endsWith(".SUB")) {

                    int n;

                    String entryExt = new String(entryName.substring(entryName.lastIndexOf('.')));

                    if (movie.isTVShow()) {
                        // for tv show, use the subtitleFile parameter because tv show is
                        // handled by downloading subtitle from the episode page (each episode for its own)
                        fileOutputStream = FileTools.createFileOutputStream(subtitleFile + entryExt);
                    } else {
                        // for movie, we need to save all subtitles entries
                        // from inside the zip file, and name them according to
                        // the movie file parts.
                        if (partsIter.hasNext()) {
                            MovieFile moviePart = partsIter.next();
                            String partName = moviePart.getFile().getAbsolutePath();
                            if (bluray) { // This is a BDRip, should be saved as index.EXT under BDMV dir to match PCH requirments
                                partName = new String(partName.substring(0, partName.lastIndexOf("BDMV"))) + "BDMV\\index";
                            } else {
                                partName = new String(partName.substring(0, partName.lastIndexOf('.')));
                            }
                            fileOutputStream = FileTools.createFileOutputStream(partName + entryExt);
                        } else {
                            // in case of some mismatch, use the old code
                            fileOutputStream = FileTools.createFileOutputStream(subtitleFile + entryExt);
                        }
                    }

                    while ((n = zipInputStream.read(buf, 0, 1024)) > -1) {
                        fileOutputStream.write(buf, 0, n);
                    }

                    found = true;
                }

                zipInputStream.closeEntry();
                zipentry = zipInputStream.getNextEntry();

            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Error - " + error.getMessage());
            return false;
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            if (connection != null) {
                connection.disconnect();
            }

        }

        return found;
    }

    protected StringWriter getContent(URLConnection connection) {
        StringWriter content = new StringWriter(10 * 1024);
        InputStreamReader inputStream = null;
        BufferedReader in = null;

        try {
            inputStream = new InputStreamReader(connection.getInputStream(), Charset.defaultCharset());
            in = new BufferedReader(inputStream);
            String line;
            while ((line = in.readLine()) != null) {
                content.write(line);
            }
            content.flush();
            return content;
        } catch (IOException ex) {
            return content;
        } finally {

            try {
                if (content != null) {
                    content.flush();
                    content.close();
                }
            } catch (IOException ex) {
                logger.debug(LOG_MESSAGE + "Failed to close content stream: " + ex.getMessage());
            }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.debug(LOG_MESSAGE + "Failed to close content stream: " + ex.getMessage());
            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                logger.debug(LOG_MESSAGE + "Failed to close content stream: " + ex.getMessage());
            }
        }
    }

    public static String removeChar(String str, char c) {
        StringBuilder r = new StringBuilder();
        for (int loop = 0; loop < str.length(); loop++) {
            if (str.charAt(loop) != c) {
                r.append(str.charAt(loop));
            }
        }
        return r.toString();
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean found = super.scanNFO(nfo, movie);
        if (found) {
            return true; // IMDB nfo found, no need of further scanning.
        }
        logger.debug(LOG_MESSAGE + "Scanning NFO for sratim url");
        Matcher m = nfoPattern.matcher(nfo);
        while (m.find()) {
            String url = m.group();
            if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") && !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
                found = true;
                movie.setId(SRATIM_PLUGIN_ID, url);
            }
        }
        if (found) {
            logger.debug(LOG_MESSAGE + "URL found in nfo = " + movie.getId(SRATIM_PLUGIN_ID));
        } else {
            logger.debug(LOG_MESSAGE + "No URL found in nfo!");
        }
        return found;
    }

    private int findEndOfHebrewSubtitlesSection(String mainXML) {
        int result = mainXML.length();
        boolean onlyHeb = PropertiesUtil.getBooleanProperty("sratim.downloadOnlyHebrew", FALSE);
        if (onlyHeb) {
            String pattern = "images/Flags/2.png";
            int nonHeb = mainXML.indexOf(pattern);
            if (nonHeb == -1) {
                result = mainXML.length();
            }
        }
        return result;
    }

    protected String extractMovieTitle(String xml) {
        String tmpTitle = removeHtmlTags(extractTag(xml, "<title>", "</title>"));
        int index = tmpTitle.indexOf('(');
        if (index == -1) {
            return "None";
        }

        return new String(tmpTitle.substring(0, index));
    }

    protected boolean hasExistingSubtitles(MovieFile mf, boolean bluray) {
        if (bluray) { // Check if the BDRIp folder contains subtitle file in PCH expected convention.
            int bdFolderIndex = mf.getFile().getAbsolutePath().lastIndexOf("BDMV");
            if (bdFolderIndex == -1) {
                logger.debug("Could not find BDMV FOLDER, Invalid BDRip Stracture, subtitle wont be downloaded");
                return true;
            }
            String bdFolder = new String(mf.getFile().getAbsolutePath().substring(0, bdFolderIndex));
            // String debug = "";

            File subIndex = new File(bdFolder + "BDMV//index.sub");
            File srtIndex = new File(bdFolder + "BDMV//index.srt");
            return subIndex.exists() || srtIndex.exists();
        }

        // Check if this movie already has subtitles for it
        File subtitleFile = FileTools.findSubtitles(mf.getFile());
        return subtitleFile.exists();
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
