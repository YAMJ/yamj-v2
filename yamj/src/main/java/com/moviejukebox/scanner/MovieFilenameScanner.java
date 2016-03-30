/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * Simple movie filename scanner.
 *
 * Scans a movie filename for keywords commonly used in scene released video files.
 *
 * Main pattern for file scanner is the following:
 *
 * {MovieTitle}[Keyword*].{container}
 *
 * The movie title is in the first position of the filename. It is followed by zero or more keywords. The file extension match the
 * container name.
 *
 * @author jjulien
 * @author quickfinga
 * @author artem.gratchev
 */
public final class MovieFilenameScanner {

    private static final Logger LOG = LoggerFactory.getLogger(MovieFilenameScanner.class);
    private static final boolean SKIP_EP_TITLE;
    private static boolean useParentRegex;
    private static final boolean ARCHIVE_SCAN_RAR;
    private static String[] skipKeywords;
    private static String[] skipRegexKeywords;
    private static final List<Pattern> SKIP_PATTERNS = new ArrayList<>();
    private static boolean languageDetection = Boolean.TRUE;
    //All symbols within brackets [] if there is an EXTRA keyword
    private static String[] extrasKeywords;
    private static final List<Pattern> EXTRAS_PATTERNS = new ArrayList<>();
    private static final Pattern USE_PARENT_PATTERN;
    private static final Pattern RAR_EXT_PATTERN = Pattern.compile("(rar|001)$");

    static {
        setExtrasKeywords(new String[]{"trailer"});
        SKIP_EP_TITLE = PropertiesUtil.getBooleanProperty("filename.scanner.skip.episodeTitle", Boolean.FALSE);
        useParentRegex = PropertiesUtil.getBooleanProperty("filename.scanner.useParentRegex", Boolean.FALSE);
        String patternString = PropertiesUtil.getProperty("filename.scanner.parentRegex", "");
        LOG.debug("useParentPattern >>{}<<", patternString);
        if (StringTools.isValidString(patternString)) {
            USE_PARENT_PATTERN = ipatt(patternString);
        } else {
            LOG.debug("Invalid parentPattern, ignoring");
            USE_PARENT_PATTERN = null;
            useParentRegex = Boolean.FALSE;
        }
        ARCHIVE_SCAN_RAR = PropertiesUtil.getBooleanProperty("mjb.scanner.archivescan.rar", Boolean.FALSE);
    }
    private static String[] movieVersionKeywords;
    private static final List<Pattern> MOVIE_VERSION_PATTERNS = new ArrayList<>();
    // Allow the use of [IMDB tt123456] to define the IMDB reference
    private static final Pattern ID_PATTERN = patt("\\[ID ([^\\[\\]]*)\\]");
    private static final Pattern IMDB_PATTERN = patt("(?i)(tt\\d{6,7})\\b");    // Search for tt followed by 6 or 7 digits and then a word boundary
    // Everything in format [SET something] (case insensitive)
    private static final Pattern SET_PATTERN = ipatt("\\[SET(?:\\s|-)([^\\[\\]]*)\\]");
    // Number at the end of string preceded with '-'
    private static final Pattern SET_INDEX_PATTERN = patt("-\\s*(\\d+)\\s*$");
    private static final String[] AUDIO_CODECS_ARRAY = new String[]{"AC3", "DTS", "DD", "AAC", "FLAC"};
    private static final Pattern TV_PATTERN = ipatt("(?<![0-9])((s[0-9]{1,4})|[0-9]{1,4})(?:(\\s|\\.|x))??((?:(e|x)\\s??[0-9]+)+)");
    private static final Pattern SEASON_PATTERN = ipatt("s{0,1}([0-9]+)(\\s|\\.)??[ex-]");
    private static final Pattern EPISODE_PATTERN = ipatt("[ex]\\s??([0-9]+)");
    private static final String TOKEN_DELIMITERS_STRING = ".[]()";
//    private static final char[] TOKEN_DELIMITERS_ARRAY = TOKEN_DELIMITERS_STRING.toCharArray();
    private static final String NOTOKEN_DELIMITERS_STRING = " _-,";
    private static final String WORD_DELIMITERS_STRING = NOTOKEN_DELIMITERS_STRING + TOKEN_DELIMITERS_STRING;
//    private static final char[] WORD_DELIMITERS_ARRAY = WORD_DELIMITERS_STRING.toCharArray();
    private static final Pattern TOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(TOKEN_DELIMITERS_STRING) + "]|$|^)");
    private static final Pattern NOTOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(NOTOKEN_DELIMITERS_STRING) + "])");
    private static final Pattern WORD_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(WORD_DELIMITERS_STRING) + "]|$|^)");
    // Last 4 digits or last 4 digits in parenthesis.
    private static final Pattern MOVIE_YEAR_PATTERN = patt("\\({0,1}(\\d{4})(?:/|\\\\|\\||-){0,1}(I*)\\){0,1}$");
    // One or more '.[]_ '
    private static final Pattern TITLE_CLEANUP_DIV_PATTERN = patt("([\\. _\\[\\]]+)");
    // '-' or '(' at the end
    private static final Pattern TITLE_CLEANUP_CUT_PATTERN = patt("-$|\\($");
    // All symbols between '-' and '/' but not after '/TVSHOW/' or '/PART/'
    private static final Pattern SECOND_TITLE_PATTERN = patt("(?<!/TVSHOW/|/PART/)-([^/]+)");
    // Parts/disks markers. CAUTION: Grouping is used for part number detection/parsing.
    private static final List<Pattern> PART_PATTERNS = new ArrayList<Pattern>() {
        private static final long serialVersionUID = 2534565160759765860L;

        {
            add(iwpatt("CD ([0-9]+)"));
            add(iwpatt("(?:(?:CD)|(?:DISC)|(?:DISK)|(?:PART))([0-9]+)"));
            add(tpatt("([0-9]{1,2})[ \\.]{0,1}DVD"));
        }
    };
    /**
     * Detect if the file/folder name is incomplete and additional info must be taken from parent folder.
     *
     * CAUTION: Grouping is used for part number detection/parsing.
     */
    private static final List<Pattern> PARENT_FOLDER_PART_PATTERNS = new ArrayList<Pattern>() {
        private static final long serialVersionUID = 6125546333783004357L;

        {
            for (Pattern p : PART_PATTERNS) {
                add(Pattern.compile("^" + p, CASE_INSENSITIVE));
            }
            add(Pattern.compile("^" + TV_PATTERN, CASE_INSENSITIVE));
        }
    };

    private abstract static class TokensPatternMap extends HashMap<String, Pattern> {

        private static final long serialVersionUID = 2239121205124537392L;

        /**
         * Generate pattern using tokens from given string.
         *
         * @param key Language id.
         * @param tokensStr Tokens list divided by comma or space.
         */
        protected void put(String key, String tokensStr) {
            List<String> tokens = new ArrayList<>();
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
                // Just pass the keyword if the map is null
                if (keywordMap.get(keyword) == null) {
                    put(keyword, keyword);
                } else {
                    put(keyword, keywordMap.get(keyword));
                }
            }
        }

        /**
         * Generate pattern using tokens from given string.
         *
         * @param key Language id.
         * @param tokens Tokens list.
         */
        protected abstract void put(String key, Collection<String> tokens);
    }
    /**
     * Mapping exact tokens to language.
     *
     * Strict mapping is case sensitive and must be obvious, it must avoid confusing movie name words and language markers.
     *
     * For example the English word "it" and Italian language marker "it", or "French" as part of the title and "french" as language
     * marker.
     *
     * However, described above is important only by file naming with token delimiters (see tokens description constants
     * TOKEN_DELIMITERS*). Language detection in non-token separated titles will be skipped automatically.
     *
     * Language markers, found with this pattern are counted as token delimiters (they will cut movie title)
     */
    private static final TokensPatternMap STRICT_LANGUAGE_MAP = new TokensPatternMap() {
        private static final long serialVersionUID = 3630995345545037071L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String s : tokens) {
                if (tokenBuilder.length() > 0) {
                    tokenBuilder.append('|');
                }
                tokenBuilder.append(Pattern.quote(s));
            }
            put(key, tpatt(tokenBuilder.toString()));
        }

        {
            put("English", "ENG EN ENGLISH eng en english Eng");
        }
    };
    /**
     * Mapping loose language markers.
     *
     * The second pass of language detection is being started after movie title detection. Language markers will be scanned with
     * loose pattern in order to find out more languages without chance to confuse with movie title.
     *
     * Markers in this map are case insensitive.
     */
    private static final TokensPatternMap LOOSE_LANGUAGE_MAP = new TokensPatternMap() {
        private static final long serialVersionUID = 1383819843117148442L;

        @Override
        protected void put(String key, Collection<String> tokens) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (String token : tokens) {
                // Only add the token if it's not there already
                String quotedToken = Pattern.quote(token.toUpperCase());
                if (tokenBuilder.indexOf(quotedToken) < 0) {
                    if (tokenBuilder.length() > 0) {
                        tokenBuilder.append('|');
                    }
                    tokenBuilder.append(quotedToken);
                }
            }
            put(key, iwpatt(tokenBuilder.toString()));
        }

        {
            // Set default values
            put("English", "ENG EN ENGLISH");
        }
    };
    private static final Map<Integer, Pattern> FPS_MAP = new HashMap<Integer, Pattern>() {
        private static final long serialVersionUID = -514057952318403685L;

        {
            for (int i : new int[]{23, 24, 25, 29, 30, 50, 59, 60}) {
                put(i, iwpatt("p" + i + "|" + i + "p"));
            }
        }
    };
    private static final Map<String, Pattern> AUDIO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 8916278631320047158L;

        {
            for (String s : AUDIO_CODECS_ARRAY) {
                put(s, iwpatt(s));
            }
        }
    };
    private static final Map<String, Pattern> VIDEO_CODEC_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 7370884465939448891L;

        {
            put("XviD", iwpatt("XVID"));
            put("DivX", iwpatt("DIVX|DIVX6"));
            put("H.264", iwpatt("H264|H\\.264|X264"));
        }
    };
    private static final Map<String, Pattern> HD_RESOLUTION_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 3476960701738952741L;

        {
            for (String s : new String[]{"720p", "1080i", "1080p", "HD", "1280x720", "1920x1080", "2160p"}) {
                put(s, iwpatt(s));
            }
        }
    };
    private static final TokensPatternMap VIDEO_SOURCE_MAP = new TokensPatternMap() {
        private static final long serialVersionUID = 4166458100829813911L;

        {
            put("SDTV", "TVRip,PAL,NTSC");
            put("D-THEATER", "DTH,DTHEATER");
            put("HDDVD", "HD-DVD,HDDVDRIP");
            put("BluRay", "BDRIP,BLURAYRIP,BLU-RAY,BD-RIP");
            put("DVDRip", "DVDR");
            put("HDTV", "");
            put("WEB-DL", "");
            put("DVD", "DVD5 DVD9");
        }

        @Override
        public void put(String key, Collection<String> tokens) {
            StringBuilder patt = new StringBuilder(key);
            for (String token : tokens) {
                patt.append("|");
                patt.append(token);
            }
            put(key, iwpatt(patt.toString()));
        }
    };
    private final MovieFileNameDTO dto = new MovieFileNameDTO();
    private final File file;
    private final String filename;
    private String rest;

    private MovieFilenameScanner(File file) {
        // CHECK FOR USE_PARENT_PATTERN matches
        if (useParentRegex && USE_PARENT_PATTERN.matcher(file.getName()).find()) {
            // Check the container to see if it's a RAR file and go up a further directory
            String rarExtensionCheck;
            try {
                rarExtensionCheck = file.getParentFile().getName().toLowerCase();
            } catch (Exception error) {
                rarExtensionCheck = Movie.UNKNOWN;
            }

            if (ARCHIVE_SCAN_RAR && RAR_EXT_PATTERN.matcher(rarExtensionCheck).find()) {
                // We need to go up two parent directories
                this.file = file.getParentFile().getParentFile();
            } else {
                // Just go up one parent directory.
                this.file = file.getParentFile();
            }

            LOG.debug("UseParentPattern matched for {} - Using parent folder name: {}", file.getName(), this.file.getName());
        } else {
            this.file = file;
        }

        this.filename = this.file.getName();
        rest = filename;
        LOG.trace("Processing filename: '{}'", rest);

        // EXTENSION AND CONTAINER
        if (this.file.isFile()) {
            // Extract and strip extension
            String ext = FilenameUtils.getExtension(rest);
            if (ext.length() > 0) {
                rest = FilenameUtils.removeExtension(rest);
            }
            dto.setExtension(ext);

            dto.setContainer(dto.getExtension().toUpperCase());
        } else {
            // For DVD images
            // no extension
            dto.setExtension("");
            dto.setContainer("DVD");
            dto.setVideoSource("DVD");
        }

        rest = cleanUp(rest);
        LOG.trace("After Extension: '{}'", rest);

        // Detect incomplete filenames and add parent folder name to parser
        for (Pattern pattern : PARENT_FOLDER_PART_PATTERNS) {
            final Matcher matcher = pattern.matcher(rest);
            if (matcher.find()) {
                final File folder = this.file.getParentFile();
                if (folder == null) {
                    break;
                }
                rest = cleanUp(folder.getName()) + "./." + rest;
                break;
            }
        }
        LOG.trace("After incomplete filename: '{}'", rest);

        // Remove version info
        for (Pattern pattern : MOVIE_VERSION_PATTERNS) {
            rest = pattern.matcher(rest).replaceAll("./.");
        }
        LOG.trace("After version info: '{}'", rest);

        // EXTRAS (Including Trailers)
        {
            for (Pattern pattern : EXTRAS_PATTERNS) {
                Matcher matcher = pattern.matcher(rest);
                if (matcher.find()) {
                    dto.setExtra(Boolean.TRUE);
                    dto.setPartTitle(matcher.group(1));
                    rest = cutMatch(rest, matcher, "./EXTRA/.");
                    break;
                }
            }
        }
        LOG.trace("After Extras: '{}'", rest);

        dto.setFps(seekPatternAndUpdateRest(FPS_MAP, dto.getFps()));
        dto.setAudioCodec(seekPatternAndUpdateRest(AUDIO_CODEC_MAP, dto.getAudioCodec()));
        dto.setVideoCodec(seekPatternAndUpdateRest(VIDEO_CODEC_MAP, dto.getVideoCodec()));
        dto.setHdResolution(seekPatternAndUpdateRest(HD_RESOLUTION_MAP, dto.getHdResolution()));
        dto.setVideoSource(seekPatternAndUpdateRest(VIDEO_SOURCE_MAP, dto.getVideoSource(), PART_PATTERNS));

        // SEASON + EPISODES
        {
            final Matcher matcher = TV_PATTERN.matcher(rest);
            if (matcher.find()) {
                if ("720".equals(matcher.group(1)) || "1080".equals(matcher.group(1)) || "2160".equals(matcher.group(1))) {
                    LOG.trace("Skipping pattern detection of '{}' because it looks like a resolution", matcher.group(0));
                } else {
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
        }
        LOG.trace("After season & episode: '{}'", rest);

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
        LOG.trace("After Part: '{}'", rest);

        // SETS
        {
            for (;;) {
                final Matcher matcher = SET_PATTERN.matcher(rest);
                if (!matcher.find()) {
                    break;
                }
                rest = cutMatch(rest, matcher, Movie.SPACE_SLASH_SPACE);

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
        LOG.trace("After Sets: '{}'", rest);

        // Movie ID detection
        {
            Matcher matcher = ID_PATTERN.matcher(rest);
            if (matcher.find()) {
                rest = cutMatch(rest, matcher, " /ID/ ");

                String[] idString = matcher.group(1).split("[-\\s+]");
                if (idString.length == 2) {
                    dto.setId(idString[0].toLowerCase(), idString[1]);
                } else {
                    LOG.debug("Error decoding ID from filename: {}", matcher.group(1));
                }
            } else {
                matcher = IMDB_PATTERN.matcher(rest);
                if (matcher.find()) {
                    rest = cutMatch(rest, matcher, " /ID/ ");
                    dto.setId(ImdbPlugin.IMDB_PLUGIN_ID, matcher.group(1));
                }
            }
        }
        LOG.trace("After Movie ID: '{}'", rest);

        // LANGUAGES
        if (languageDetection) {
            for (;;) {
                String language = seekPatternAndUpdateRest(STRICT_LANGUAGE_MAP, null);
                if (language == null) {
                    break;
                }
                dto.getLanguages().add(language);
            }
        }
        LOG.trace("After languages: '{}'", rest);

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

                boolean first = Boolean.TRUE;
                while (t.hasMoreElements()) {
                    String token = t.nextToken();
                    token = cleanUpTitle(token);
                    // Search year (must be next to a non-empty token)
                    if (first) {
                        if (token.length() > 0) {
                            try {
                                int year = Integer.parseInt(token);
                                if (year >= 1800 && year <= 3000) {
                                    dto.setYear(year);
                                }
                            } catch (NumberFormatException error) {
                                /* ignore */ }
                        }
                        first = Boolean.FALSE;
                    }

                    if (!languageDetection) {
                        break;
                    }

                    // Loose language search
                    if (token.length() >= 2 && token.indexOf('-') < 0) {
                        for (Map.Entry<String, Pattern> e : LOOSE_LANGUAGE_MAP.entrySet()) {
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
                        if (SKIP_EP_TITLE) {
                            dto.setEpisodeTitle(Movie.UNKNOWN);
                        } else {
                            dto.setEpisodeTitle(title);
                        }
                        break;
                    }
                }
            }

            // PART TITLE
            if (dto.getPart() >= 0) {
                // Just do this for no extra, already named.
                if (!dto.isExtra()) {
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
        LOG.trace("Final: {}", dto.toString());

    }

    /**
     * Used for testing
     *
     * @return
     */
    public static Pattern getTokenDelimitersMatchPattern() {
        return TOKEN_DELIMITERS_MATCH_PATTERN;
    }

    /**
     * Compile the pattern
     *
     * @param regex
     * @return Exact pattern
     */
    private static Pattern patt(String regex) {
        return Pattern.compile(regex);
    }

    /**
     * @param regex
     * @return Case insensitive pattern
     */
    private static Pattern ipatt(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case insensitive pattern matched somewhere in square brackets
     */
    private static Pattern pattInSBrackets(String regex) {
        return ipatt("\\[([^\\[\\]]*" + regex + "[^\\[]*)\\]");
    }

    /**
     * @param regex
     * @return Case insensitive pattern with word delimiters around
     */
    public static Pattern iwpatt(String regex) {
        return Pattern.compile("(?<=" + WORD_DELIMITERS_MATCH_PATTERN
                + ")(?:" + regex + ")(?="
                + WORD_DELIMITERS_MATCH_PATTERN + ")", Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case sensitive pattern with word delimiters around
     */
    public static Pattern wpatt(String regex) {
        return Pattern.compile("(?<=" + WORD_DELIMITERS_MATCH_PATTERN
                + ")(?:" + regex + ")(?="
                + WORD_DELIMITERS_MATCH_PATTERN + ")");
    }

    /**
     * @param regex
     * @return Case sensitive pattern with token delimiters around
     */
    private static Pattern tpatt(String regex) {
        return Pattern.compile(TOKEN_DELIMITERS_MATCH_PATTERN + "(?:" + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + "(?:" + regex + ")" + "(?:"
                + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + TOKEN_DELIMITERS_MATCH_PATTERN);
    }

    private static String cleanUp(String filename) {
        // SKIP
        String rFilename = filename; // We can't modify the parameter, so copy it
        for (Pattern p : SKIP_PATTERNS) {
            rFilename = p.matcher(rFilename).replaceAll("./.");
        }
        return rFilename;
    }

    /**
     * Decode the language tag passed in, into standard YAMJ language code
     *
     * @param language The language tag to decode
     * @return
     *
     */
    //TODO : Extract this from here, it's not specific on MovieFileNameScanner
    public static String determineLanguage(String language) {
        for (Map.Entry<String, Pattern> e : STRICT_LANGUAGE_MAP.entrySet()) {
            Matcher matcher = e.getValue().matcher(language);
            if (matcher.find()) {
                return e.getKey();
            }
        }
        return language;
    }

    /**
     * Get the list of loose languages associated with a language
     *
     * @param language
     * @return
     */
    //TODO : Extract this from here, it's not specific on MovieFileNameScanner
    public static String getLanguageList(String language) {
        if (LOOSE_LANGUAGE_MAP.containsKey(language)) {
            Pattern langPatt = LOOSE_LANGUAGE_MAP.get(language);
            return langPatt.toString().toLowerCase();
        }
        return "";
    }

    /**
     * Replace all dividers with spaces and trim trailing spaces and redundant braces/minuses at the end.
     *
     * @param token String to clean up.
     * @return Prepared title.
     */
    private static String cleanUpTitle(String token) {
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
     *
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
        return (rest.substring(0, matcher.start()) + rest.substring(matcher.end())).trim();
    }

    private static String cutMatch(String rest, Matcher matcher, String divider) {
        return rest.substring(0, matcher.start()) + divider + rest.substring(matcher.end());
    }

    public static MovieFileNameDTO scan(File file) {
        return new MovieFilenameScanner(file).getDto();
    }

    public static String[] getSkipKeywords() {
        return skipKeywords.clone();
    }

    public static void setSkipKeywords(String[] skipKeywords, boolean caseSensitive) {
        MovieFilenameScanner.skipKeywords = skipKeywords.clone();
        SKIP_PATTERNS.clear();
        for (String s : MovieFilenameScanner.skipKeywords) {
            if (caseSensitive) {
                SKIP_PATTERNS.add(wpatt(Pattern.quote(s)));
            } else {
                SKIP_PATTERNS.add(iwpatt(Pattern.quote(s)));
            }
        }
    }

    public static String[] getSkipRegexKeywords() {
        return skipRegexKeywords.clone();
    }

    public static void setSkipRegexKeywords(String[] skipRegexKeywords, boolean caseSensitive) {
        MovieFilenameScanner.skipRegexKeywords = skipRegexKeywords.clone();
        for (String s : MovieFilenameScanner.skipRegexKeywords) {
            if (caseSensitive) {
                SKIP_PATTERNS.add(patt(s));
            } else {
                SKIP_PATTERNS.add(ipatt(s));
            }
        }
    }

    public static String[] getExtrasKeywords() {
        return extrasKeywords.clone();
    }

    public static void setExtrasKeywords(String[] extrasKeywords) {
        MovieFilenameScanner.extrasKeywords = extrasKeywords.clone();
        EXTRAS_PATTERNS.clear();
        for (String s : MovieFilenameScanner.extrasKeywords) {
            EXTRAS_PATTERNS.add(pattInSBrackets(Pattern.quote(s)));
        }
    }

    public static String[] getMovieVersionKeywords() {
        return movieVersionKeywords.clone();
    }

    public static void setMovieVersionKeywords(String[] movieVersionKeywords) {
        MovieFilenameScanner.movieVersionKeywords = movieVersionKeywords.clone();
        MOVIE_VERSION_PATTERNS.clear();
        for (String s : MovieFilenameScanner.movieVersionKeywords) {
            MOVIE_VERSION_PATTERNS.add(
                    iwpatt(s.replace(" ", WORD_DELIMITERS_MATCH_PATTERN.pattern())));
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
        STRICT_LANGUAGE_MAP.clear();
        LOOSE_LANGUAGE_MAP.clear();
    }

    /**
     * Add new language detection pattern.
     *
     * @param key Language code.
     * @param strictPattern Exact pattern for the first-pass detection.
     * @param loosePattern Loose pattern for second-pass detection.
     */
    public static void addLanguage(String key, String strictPattern, String loosePattern) {
        STRICT_LANGUAGE_MAP.put(key, strictPattern);
        LOOSE_LANGUAGE_MAP.put(key, loosePattern);
    }

    public static void setSourceKeywords(List<String> keywords, Map<String, String> keywordMap) {
        VIDEO_SOURCE_MAP.clear();
        VIDEO_SOURCE_MAP.putAll(keywords, keywordMap);
    }
}
