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

package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.model.MovieFileNameDTO;

/**
 * Simple movie filename scanner. Scans a movie filename for keywords commonly used in scene released video files.
 * 
 * Main pattern for file scanner is the following:
 * 
 * <MovieTitle>[Keyword*].<container>
 * 
 * * The movie title is in the first position of the filename. * it is followed by zero or more keywords. * the file extension match the container name.
 * 
 * @author jjulien
 * @author quickfinga
 * @author artem.gratchev
 */
@SuppressWarnings("serial")
public class MovieFilenameScanner {

    protected static final Logger logger = Logger.getLogger("moviejukebox");

    private static String[] skipKeywords;
    private static final List<Pattern> skipPatterns = new ArrayList<Pattern>();
    private static boolean languageDetection = true;

    /** All symbols within brackets [] if there is an EXTRA keyword */
    private static String[] extrasKeywords;
    private static final List<Pattern> extrasPatterns = new ArrayList<Pattern>();
    static {
        setExtrasKeywords(new String[] {"trailer"});
    }

    private static String[] movieVersionKeywords;
    private static final List<Pattern> movieVersionPatterns = new ArrayList<Pattern>();
    

    /** Everything in format [SET something] */
    private static final Pattern SET_PATTERN = patt("\\[SET ([^\\[\\]]*)\\]");

    /** Number at the end of string preceded with '-' */
    private static final Pattern SET_INDEX_PATTERN = patt("-\\s*(\\d+)\\s*$");

    private static final String[] AUDIO_CODECS_ARRAY = new String[] { "AC3", "DTS", "DD", "AAC" };

    //protected static final Pattern TV_PATTERN = ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,2})((?:(?:e[0-9]+)+)|(?:(?:x[0-9]+)+))");
    protected static final Pattern TV_PATTERN = ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,2})(?:(\\s|\\.|x))??((?:(e|x)\\s??[0-9]+)+)");
    //protected static final Pattern SEASON_PATTERN = ipatt("s{0,1}([0-9]+)[ex]");
    protected static final Pattern SEASON_PATTERN = ipatt("s{0,1}([0-9]+)(\\s|\\.)??[ex-]");
    //protected static final Pattern EPISODE_PATTERN = ipatt("[ex]([0-9]+)");
    protected static final Pattern EPISODE_PATTERN = ipatt("[ex]\\s??([0-9]+)");

    protected static final String TOKEN_DELIMITERS_STRING = ".[]()";
    protected static final char[] TOKEN_DELIMITERS_ARRAY = TOKEN_DELIMITERS_STRING.toCharArray();
    private static final String NOTOKEN_DELIMITERS_STRING = " _-,";
    protected static final String WORD_DELIMITERS_STRING = NOTOKEN_DELIMITERS_STRING + TOKEN_DELIMITERS_STRING;
    protected static final char[] WORD_DELIMITERS_ARRAY = WORD_DELIMITERS_STRING.toCharArray();
    protected static final Pattern TOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(TOKEN_DELIMITERS_STRING) + "]|$|^)");
    protected static final Pattern NOTOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(NOTOKEN_DELIMITERS_STRING) + "])");
    protected static final Pattern WORD_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(WORD_DELIMITERS_STRING) + "]|$|^)");

    /** Last 4 digits or last 4 digits in parenthesis. */
    protected static final Pattern MOVIE_YEAR_PATTERN = patt("[^0-9]\\({0,1}([0-9]{4})\\){0,1}$");

    /** One or more '.[]_ ' */
    protected static final Pattern TITLE_CLEANUP_DIV_PATTERN = patt("([\\. _\\[\\]]+)");

    /** '-' or '(' at the end */
    protected static final Pattern TITLE_CLEANUP_CUT_PATTERN = patt("-$|\\($");

    /** All symbols between '-' and '/' but not after '/TVSHOW/' or '/PART/' */
    protected static final Pattern SECOND_TITLE_PATTERN = patt("(?<!/TVSHOW/|/PART/)-([^/]+)");

    private static final List<Pattern> PART_PATTERNS = new ArrayList<Pattern>() {
        {
            add(iwpatt("CD ([0-9]+)"));
            add(iwpatt("(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)"));
            add(tpatt("([0-9]{1,2})[ \\.]{0,1}DVD"));
        }
    };
    
    private static abstract class TokensPatternMap extends HashMap<String, Pattern> {
        /**
         * Generate pattern using tokens from given string.
         * @param key Language id.
         * @param tokensStr Tokens list divided by comma or space.
         */
        protected void put(String key, String tokensStr) {
            List<String> tokens = new ArrayList<String>(); 
            for (String token : tokensStr.split("[ ,]+")) {
                token = StringUtils.trimToNull(token);
                if (token != null) {
                    tokens.add(token);
                }
            }
            put(key, tokens);
        }
        
        protected void putAll(List<String> keywords, Map<String, String> keywordMap) {
            for (String keyword : keywords) {
                put(keyword, keywordMap.get(keyword));
            }
        }
        
        /**
         * Generate pattern using tokens from given string.
         * @param key Language id.
         * @param tokens Tokens list.
         */
        protected abstract void put(String key, Collection<String> tokens);
    }


    /**
     * Mapping exact tokens to language. Strict mapping is case sensitive and must be obvious. 
     * E.q. it must avoid confusing movie name words and language markers. 
     * For example the English word "it" and Italian language marker "it", or "French" as part
     * of the title and "french" as language marker.<br>
     * 
     * However, described above is important only by file naming with token delimiters 
     * (see tokens description constants TOKEN_DELIMITERS*). Language detection in non-token
     * separated titles will be skipped automatically.<br>
     * 
     * Language markers, found with this pattern are counted as token delimiters (they will cut
     * movie title)
     */
    private static final TokensPatternMap strictLanguageMap = new TokensPatternMap() {
        
        /** 
         * {@inheritDoc}
         */
        protected void put(String key, Collection<String> tokens) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String s : tokens) {
                if (!first) {
                    sb.append('|');
                }
                sb.append(Pattern.quote(s));
                first = false;
            }
            put(key, tpatt(sb.toString()));
        }

        {
            put("English", "ENG EN ENGLISH eng en english Eng");
        }
    };

    /**
     * Mapping loose language markers. The second pass of language detection is being started
     * after movie title detection. Language markers will be scanned with loose pattern in order 
     * to find out more languages without chance to confuse with movie title. Markers in this
     * map are case insensitive.
     */
    private static final TokensPatternMap looseLanguageMap = new TokensPatternMap() {

        /** 
         * {@inheritDoc}
         */
        protected void put(String key, Collection<String> tokens) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String token : tokens) {
                // Only add the token if it's not there already
                String quotedToken = Pattern.quote(token.toUpperCase());
                if (sb.indexOf(quotedToken) < 0) {
                    if (!first) {
                        sb.append('|');
                    } else {
                        first = false;
                    }
					sb.append(quotedToken);
                }
            }
            put(key, iwpatt(sb.toString()));
        }

        {
            // Set default values
            put("English", "ENG EN ENGLISH");
        }
    };

    private static final Map<Integer, Pattern> FPS_MAP = new HashMap<Integer, Pattern>() {
        {
            for (int i : new int[] { 23, 24, 25, 29, 30, 50, 59, 60 }) {
                put(i, iwpatt("p" + i + "|" + i + "p"));
            }
        }
    };

    private static final Map<String, Pattern> AUDIO_CODEC_MAP = new HashMap<String, Pattern>() {
        {
            for (String s : AUDIO_CODECS_ARRAY) {
                put(s, iwpatt(s));
            }
        }
    };

    private static final Map<String, Pattern> VIDEO_CODEC_MAP = new HashMap<String, Pattern>() {
        {
            put("XviD", iwpatt("XVID"));
            put("DivX", iwpatt("DIVX|DIVX6"));
            put("H.264", iwpatt("H264|H\\.264|X264"));
        }
    };

    private static final Map<String, Pattern> HD_RESOLUTION_MAP = new HashMap<String, Pattern>() {
        {
            for (String s : new String[] { "720p", "1080i", "1080p", "HD" }) {
                put(s, iwpatt(s));
            }
        }
    };

    private static final TokensPatternMap videoSourceMap = new TokensPatternMap() {
        {
            put("SDTV", "TVRip,PAL,NTSC");
            put("D-THEATER", "DTH,DTHEATER");
            put("HDDVD", "HD-DVD,HDDVDRIP");
            put("BluRay", "BDRIP,BLURAYRIP,BLU-RAY,BD-RIP");
            put("DVDRip", "DVDR");
            put("HDTV", "");
            put("DVD", "DVD5 DVD9");
        }

        @Override
        public void put(String key, Collection<String> tokens) {
            String patt = key;
            for (String t : tokens) {
                patt += "|" + t;
            }
            put(key, iwpatt(patt));
        }
    };

    private final MovieFileNameDTO dto = new MovieFileNameDTO();
    private final File file;
    private final String filename;
    private String rest;

    /**
     * @param regex
     * @return Exact pattern
     */
    private static final Pattern patt(String regex) {
        return Pattern.compile(regex);
    }

    /**
     * @param regex
     * @return Case insensitive pattern
     */
    private static final Pattern ipatt(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

     /**
     * @param regex
     * @return Case insensitive pattern matched somewhere in square brackets
     */
    private static final Pattern pattInSBrackets(String regex) {
        return ipatt("\\[([^\\[\\]]*" + regex + "[^\\[]*)\\]");
    }
   /**
     * @param regex
     * @return Case insensitive pattern with word delimiters around
     */
    public static final Pattern iwpatt(String regex) {
        return Pattern.compile(WORD_DELIMITERS_MATCH_PATTERN + "(?:" + regex + ")" + WORD_DELIMITERS_MATCH_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case sensitive pattern with word delimiters around
     */
    private static final Pattern wpatt(String regex) {
        return Pattern.compile(WORD_DELIMITERS_MATCH_PATTERN + "(?:" + regex + ")" + WORD_DELIMITERS_MATCH_PATTERN);
    }

    /**
     * @param regex
     * @return Case sensitive pattern with token delimiters around
     */
    private static final Pattern tpatt(String regex) {
        return Pattern.compile(TOKEN_DELIMITERS_MATCH_PATTERN + "(?:" + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + "(?:" + regex + ")" + "(?:"
                        + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + TOKEN_DELIMITERS_MATCH_PATTERN);
    }

    private MovieFilenameScanner(File file) {
        this.file = file;
        this.filename = file.getName();
        rest = filename;

        // EXTENSION AND CONTAINER
        if (file.isFile()) {
            // Extract and strip extension
            int i = rest.lastIndexOf('.');
            if (i > 0) {
                dto.setExtension(rest.substring(i + 1));
                rest = rest.substring(0, i);
            } else {
                dto.setExtension("");
            }
            dto.setContainer(dto.getExtension().toUpperCase());
        } else {
            // For DVD images
            // no extension
            dto.setExtension("");
            dto.setContainer("DVD");
            dto.setVideoSource("DVD");
        }

        // SKIP
        for (Pattern p : skipPatterns) {
            rest = p.matcher(rest).replaceAll("./.");
        }

        // Remove version info
        for (Pattern p : movieVersionPatterns) {
            rest = p.matcher(rest).replaceAll("./.");
        }

        // EXTRAS (Including Trailers)
        {
            for (Pattern pattern : extrasPatterns) {
                Matcher matcher = pattern.matcher(rest);
                if (matcher.find()) {
                    dto.setExtra(true);
                    dto.setPartTitle(matcher.group(1));
                    rest = cutMatch(rest, matcher, "./EXTRA/.");
                    break;
                }
            }
        }

        dto.setFps(seekPatternAndUpdateRest(FPS_MAP, dto.getFps()));
        dto.setAudioCodec(seekPatternAndUpdateRest(AUDIO_CODEC_MAP, dto.getAudioCodec()));
        dto.setVideoCodec(seekPatternAndUpdateRest(VIDEO_CODEC_MAP, dto.getVideoCodec()));
        dto.setHdResolution(seekPatternAndUpdateRest(HD_RESOLUTION_MAP, dto.getHdResolution()));
        dto.setVideoSource(seekPatternAndUpdateRest(videoSourceMap, dto.getVideoSource(), PART_PATTERNS));

        // SEASON + EPISODES
        {
            final Matcher matcher = TV_PATTERN.matcher(rest);
            if (matcher.find()) {
                // logger.finest("It's a TV Show: " + group0);
                rest = cutMatch(rest, matcher, "./TVSHOW/.");
                
                final Matcher smatcher = SEASON_PATTERN.matcher(matcher.group(0));
                smatcher.find();
                int season = Integer.parseInt(smatcher.group(1));
                dto.setSeason(season);

                final Matcher ematcher = EPISODE_PATTERN.matcher(matcher.group(0));
                while (ematcher.find()) {
                    dto.getEpisodes().add(Integer.parseInt(ematcher.group(1)));
                }
            }
        }

        // PART
        {
            for (Pattern pattern : PART_PATTERNS) {
                Matcher matcher = pattern.matcher(rest);
                if (matcher.find()) {
                    rest = cutMatch(rest, matcher, " /PART/ ");
                    dto.setPart(Integer.parseInt(matcher.group(1)));
                    break;
                }
            }
        }

        // SETS
        {
            for (;;) {
                final Matcher matcher = SET_PATTERN.matcher(rest);
                if (!matcher.find()) {
                    break;
                }
                rest = cutMatch(rest, matcher, " / ");

                MovieFileNameDTO.SetDTO set = new MovieFileNameDTO.SetDTO();
                dto.getSets().add(set);

                String n = matcher.group(1);
                Matcher nmatcher = SET_INDEX_PATTERN.matcher(n);
                if (nmatcher.find()) {
                    set.setIndex(Integer.parseInt(nmatcher.group(1)));
                    n = cutMatch(n, nmatcher);
                }
                set.setTitle(n.trim());
            }
        }

        // LANGUAGES
        if (languageDetection) {
            for (;;) {
                String language = seekPatternAndUpdateRest(strictLanguageMap, null);
                if (language == null) {
                    break;
                }
                dto.getLanguages().add(language);
            }
        }

        // TITLE
        {
            int iextra = dto.isExtra() ? rest.indexOf("/EXTRA/") : rest.length();
            int itvshow = dto.getSeason() >= 0 ? rest.indexOf("/TVSHOW/") : rest.length();
            int ipart = dto.getPart() >= 0 ? rest.indexOf("/PART/") : rest.length();

            {
                int min = iextra < itvshow ? iextra : itvshow;
                min = min < ipart ? min : ipart;

                // Find first token before trailer, TV show and part
                // Name should not start with '-' (exclude wrongly marked part/episode titles)
                String title = "";
                StringTokenizer t = new StringTokenizer(rest.substring(0, min), "/[]");
                while (t.hasMoreElements()) {
                    String token = t.nextToken();
                    token = cleanUpTitle(token);
                    if (token.length() >= 1 && token.charAt(0) != '-') {
                        title = token;
                        break;
                    }
                }

                boolean first = true;
                while (t.hasMoreElements()) {
                    String token = t.nextToken();
                    token = cleanUpTitle(token);
                    // Search year (must be next non-empty token)
                    if (first) {
                        if (token.length() > 0) {
                            try {
                                int year = Integer.parseInt(token);
                                if (year >= 1800 && year <= 3000) {
                                    dto.setYear(year);
                                }
                            } catch (NumberFormatException error) {
                            }
                        }
                        first = false;
                    }

                    if (!languageDetection) {
                        break;
                    }

                    // Loose language search
                    if (token.length() >= 2 && token.indexOf('-') < 0) {
                        for (Map.Entry<String, Pattern> e : looseLanguageMap.entrySet()) {
                            Matcher matcher = e.getValue().matcher(token);
                            if (matcher.find()) {
                                dto.getLanguages().add(e.getKey());
                            }
                        }
                    }
                }
                
                // Search year within title (last 4 digits or 4 digits in parenthesis)
                if (dto.getYear() < 0) {
                    Matcher ymatcher = MOVIE_YEAR_PATTERN.matcher(title);
                    if (ymatcher.find()) {
                        int year = Integer.parseInt(ymatcher.group(1));
                        if (year >= 1919 && year <= 2099) {
                            dto.setYear(year);
                            title = cutMatch(title, ymatcher);
                        }
                    }
                }
                dto.setTitle(title);
            }

            // EPISODE TITLE
            if (dto.getSeason() >= 0) {
                itvshow += 8;
                Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(itvshow));
                while (matcher.find()) {
                    String title = cleanUpTitle(matcher.group(1));
                    if (title.length() > 0) {
                        dto.setEpisodeTitle(title);
                        break;
                    }
                }
            }

            // PART TITLE
            if (dto.getPart() >= 0) {
                // Just do this for no extra, already named.
                if(!dto.isExtra()){
                    ipart += 6;
                    Matcher matcher = SECOND_TITLE_PATTERN.matcher(rest.substring(ipart));
                    while (matcher.find()) {
                        String title = cleanUpTitle(matcher.group(1));
                        if (title.length() > 0) {
                            dto.setPartTitle(title);
                            break;
                        }
                    }
                }
            }

        }
    }
    
    /**
     * Decode the language tag passed in, into standard YAMJ language code
     * @param language The language tag to decode
     * @return
     *
     */
    //TODO : Extract this from here, it's not specific on MovieFileNameScanner
    public static String determineLanguage(String language) {
        for (Map.Entry<String, Pattern> e : strictLanguageMap.entrySet()) {
            Matcher matcher = e.getValue().matcher(language);
            if (matcher.find()) {
                return e.getKey();
            }
        }
        return language;
    }

    /**
     * Replace all dividers with spaces and trim trailing spaces and redundant 
     * braces/minuses at the end. 
     * @param token String to clean up.
     * @return Prepared title.
     */
    private String cleanUpTitle(String token) {
        String title = TITLE_CLEANUP_DIV_PATTERN.matcher(token).replaceAll(" ").trim();
        return TITLE_CLEANUP_CUT_PATTERN.matcher(title).replaceAll("").trim();
    }

    private <T> T seekPatternAndUpdateRest(Map<T, Pattern> map, T oldValue) {
        for (Map.Entry<T, Pattern> e : map.entrySet()) {
            Matcher matcher = e.getValue().matcher(rest);
            if (matcher.find()) {
                rest = cutMatch(rest, matcher, "./.");
                return e.getKey();
            }
        }
        return oldValue;
    }

    /**
     * Update rest only if no interference with protected patterns.
     * @param <T> Return type.
     * @param map Keyword/pattern map.
     * @param oldValue To return if nothing found.
     * @param protectPatterns Pattern to protect.
     * @return
     */
    private <T> T seekPatternAndUpdateRest(Map<T, Pattern> map, T oldValue, Collection<Pattern> protectPatterns) {
        for (Map.Entry<T, Pattern> e : map.entrySet()) {
            Matcher matcher = e.getValue().matcher(rest);
            if (matcher.find()) {
                String restCut = cutMatch(rest, matcher, "./.");
                for (Pattern protectPattern : protectPatterns) {
                    if (protectPattern.matcher(rest).find() 
                            && !protectPattern.matcher(restCut).find()) {
                        return e.getKey();
                    }
                }
                rest = restCut;
                return e.getKey();
            }
        }
        return oldValue;
    }

    private static String cutMatch(String rest, Matcher matcher) {
        return rest.substring(0, matcher.start()) + rest.substring(matcher.end());
    }

    private static String cutMatch(String rest, Matcher matcher, String divider) {
        return rest.substring(0, matcher.start()) + divider + rest.substring(matcher.end());
    }

    public static MovieFileNameDTO scan(File file) {
        return new MovieFilenameScanner(file).getDto();
    }

    public static String[] getSkipKeywords() {
        return skipKeywords;
    }

    public static void setSkipKeywords(String[] skipKeywords) {
        MovieFilenameScanner.skipKeywords = skipKeywords;
        skipPatterns.clear();
        for (String s : skipKeywords) {
            skipPatterns.add(wpatt(Pattern.quote(s)));
        }
    }
    
    public static String[] getExtrasKeywords() {
        return extrasKeywords;
    }

    public static void setExtrasKeywords(String[] extrasKeywords) {
        MovieFilenameScanner.extrasKeywords = extrasKeywords;
        extrasPatterns.clear();
        for (String s : extrasKeywords) {
            extrasPatterns.add(pattInSBrackets(Pattern.quote(s)));
        }
    }

    public static String[] getMovieVersionKeywords() {
        return movieVersionKeywords;
    }

    public static void setMovieVersionKeywords(String[] movieVersionKeywords) {
        MovieFilenameScanner.movieVersionKeywords = movieVersionKeywords;
        movieVersionPatterns.clear();
        for (String s : movieVersionKeywords) {
            movieVersionPatterns.add(
                    iwpatt(s.replace(" ", WORD_DELIMITERS_MATCH_PATTERN.pattern()))
                    );
        }
    }

    public static boolean isLanguageDetection() {
        return languageDetection;
    }

    public static void setLanguageDetection(boolean languageDetection) {
        MovieFilenameScanner.languageDetection = languageDetection;
    }

    public MovieFileNameDTO getDto() {
        return dto;
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }
    
    /**
     * Clear language detection patterns.
     */
    public static void clearLanguages() {
        strictLanguageMap.clear();
        looseLanguageMap.clear();
    }
    
    /**
     * Add new language detection pattern.
     * @param key Language code.
     * @param strictPattern Exact pattern for the first-pass detection.
     * @param loosePattern Loose pattern for second-pass detection.   
     */
    public static void addLanguage(String key, String strictPattern, String loosePattern) {
        strictLanguageMap.put(key, strictPattern);
        looseLanguageMap.put(key, loosePattern);
    }

    public static void setSourceKeywords(List<String> keywords, Map<String, String> keywordMap) {
        videoSourceMap.clear();
        videoSourceMap.putAll(keywords, keywordMap);
    }
    
}
