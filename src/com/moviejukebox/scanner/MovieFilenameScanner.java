/*
 *      Copyright (c) 2004-2009 YAMJ Members
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.tools.PropertiesUtil;

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

    protected static final Pattern TV_PATTERN = ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,2})((?:(?:e[0-9]+)+)|(?:(?:x[0-9]+)+))");
    protected static final Pattern SEASON_PATTERN = ipatt("s{0,1}([0-9]+)[ex]");
    protected static final Pattern EPISODE_PATTERN = ipatt("[ex]([0-9]+)");

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

    private static final Map<String, Pattern> STRICT_LANGUAGES_MAP = new HashMap<String, Pattern>() {
        private void put(String key, String tokens) {
            String[] ts = tokens.split(" ");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String s : ts) {
                if (!first) {
                    sb.append('|');
                }
                sb.append(Pattern.quote(s));
                first = false;
            }
            put(key, tpatt(sb.toString()));
        }

        {
            put("Chinese", "ZH Zh zh CHI Chi chi CHINESE Chinese chinese");
            put("Dual Language", "DL dl");
            put("English", "ENG EN ENGLISH eng en english Eng");
            put("French", "FRA FR FRENCH VF fra fr french vf Fra");
            put("German", "GER DE GERMAN ger de german Ger");
            put("Hebrew", "HEB HE HEBREW HEBDUB heb he hebrew hebdub Heb");
            put("Hindi", "HI HIN HINDI hi hin hindi Hin Hindi");
            put("Hungarian", "HUN HU HUNGARIAN hun hu hungarian");
            put("Italian", "ITA IT ITALIAN ita it italian Ita");
            put("Japanese", "JPN JP JAPANESE jpn jp japanese Jpn");
            put("Norwegian", "NOR NORWEGIAN nor norwegian Norwegian");
            put("Polish", "POL PL POLISH PLDUB pol pl polish pldub Pol");
            put("Portuguese", "POR PT PORTUGUESE por pt portuguese Por");
            put("Russian", "RUS RU RUSSIAN rus ru russian Rus");
            put("Spanish", "SPA ES SPANISH spa es spanish Spa");
            put("Swedish", "SV Sv sv SWE Swe swe SWEDISH Swedish swedish");
            put("Thai", "TH Th th THA Tha tha THAI Thai thai");
            put("VO", "VO VOSTFR vo vostfr");
        }
    };

    private static final Map<String, Pattern> LOOSE_LANGUAGES_MAP = new HashMap<String, Pattern>() {
        private void put(String key, String tokens) {
            String[] ts = tokens.split(" ");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String s : ts) {
                if (!first) {
                    sb.append('|');
                }
                sb.append(Pattern.quote(s));
                first = false;
            }
            put(key, iwpatt(sb.toString()));
        }

        {
            put("Chinese", "ZH CHI CHINESE");
            put("Dual Language", "DL");
            put("English", "ENG EN ENGLISH");
            put("French", "FRA FR FRENCH");
            put("German", "GER DE GERMAN");
            put("Hebrew", "HEB HE HEBREW HEBDUB");
            put("Hindi", "HI HIN HINDI");
            put("Hungarian", "HUN HU HUNGARIAN");
            put("Italian", "ITA IT ITALIAN");
            put("Japanese", "JPN JP JAPANESE");
            put("Norwegian", "NOR NORWEGIAN");
            put("Polish", "POL PL POLISH PLDUB");
            put("Portuguese", "POR PT PORTUGUESE");
            put("Russian", "RUS RU RUSSIAN");
            put("Spanish", "SPA ES SPANISH");
            put("Swedish", "SV SWE SWEDISH");
            put("Thai", "TH THA THAI");
            put("VO", "VO VOSTFR");
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

    private static final Map<String, Pattern> VIDEO_SOURCE_MAP = new HashMap<String, Pattern>() {
        {
            String sourceKeywords = PropertiesUtil.getProperty("filename.scanner.source.keywords",
                            "HDTV,PDTV,DVDRip,DVDSCR,DSRip,CAM,R5,LINE,HD2DVD,DVD,DVD5,DVD9,HRHDTV,MVCD,VCD,TS,VHSRip,BluRay,HDDVD,D-THEATER,SDTV");

            HashMap<String, String> mappedKeywordsDefaults = new HashMap<String, String>() {
                {
                    put("SDTV", "TVRip,PAL,NTSC");
                    put("D-THEATER", "DTH,DTHEATER");
                    put("HDDVD", "HD-DVD,HDDVDRIP");
                    put("BluRay", "BDRIP,BLURAYRIP,BLU-RAY");
                    put("DVDRip", "DVDR");
                }
            };

            for (String s : sourceKeywords.split(",")) {
                // Set the default the long way to allow 'keyword.XXX=' to blank the value instead of using default
                String mappedKeywords = PropertiesUtil.getProperty("filename.scanner.source.keywords." + s, null);
                if (null == mappedKeywords) {
                    mappedKeywords = mappedKeywordsDefaults.get(s);
                }

                String patt = s;
                if (null != mappedKeywords && mappedKeywords.length() > 0) {
                    for (String t : mappedKeywords.split(",")) {
                        patt += "|" + t;
                    }
                }
                put(s, iwpatt(patt));
            }
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
    private static final Pattern iwpatt(String regex) {
        return Pattern.compile(WORD_DELIMITERS_MATCH_PATTERN + "(?:" + regex + ")" + WORD_DELIMITERS_MATCH_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case insensitive pattern with word delimiters around
     */
    @SuppressWarnings("unused")
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

                MovieFileNameDTO.Set set = new MovieFileNameDTO.Set();
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

        dto.setFps(seekPatternAndUpdateRest(FPS_MAP, dto.getFps()));
        dto.setAudioCodec(seekPatternAndUpdateRest(AUDIO_CODEC_MAP, dto.getAudioCodec()));
        dto.setVideoCodec(seekPatternAndUpdateRest(VIDEO_CODEC_MAP, dto.getVideoCodec()));
        dto.setHdResolution(seekPatternAndUpdateRest(HD_RESOLUTION_MAP, dto.getHdResolution()));
        dto.setVideoSource(seekPatternAndUpdateRest(VIDEO_SOURCE_MAP, dto.getVideoSource()));

        // LANGUAGES
        if (languageDetection) {
            for (;;) {
                String language = seekPatternAndUpdateRest(STRICT_LANGUAGES_MAP, null);
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
                        for (Map.Entry<String, Pattern> e : LOOSE_LANGUAGES_MAP.entrySet()) {
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
    
    /**
     * Decode the language tag passed in, into standard YAMJ language code
     * @param language The language tag to decode
     * @return
     */
    public static String determineLanguage(String language) {
        for (Map.Entry<String, Pattern> e : STRICT_LANGUAGES_MAP.entrySet()) {
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
            skipPatterns.add(ipatt(Pattern.quote(s)));
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

}
