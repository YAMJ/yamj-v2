package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
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
public class MovieFilenameScanner {

    protected static final String[] SKIP_KEYWORDS;
    protected static final boolean LANGUAGE_DETECTION = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.scanner.language.detection", "true"));

    protected static final Pattern TV_PATTERN = Pattern.compile("[Ss]{0,1}([0-9]+)([EeXx][0-9]+)+");
    protected static final Pattern EPISODE_PATTERN = Pattern.compile("[EeXx]([0-9]+)");
    protected static final Logger LOGGER = Logger.getLogger("moviejukebox");
    protected static final String TOKEN_DELIMITERS_STRING = ".[]()";
    protected static final char[] TOKEN_DELIMITERS_ARRAY = TOKEN_DELIMITERS_STRING.toCharArray();
    protected static final String WORD_DELIMITERS_STRING = TOKEN_DELIMITERS_STRING + " _-";
    protected static final char[] WORD_DELIMITERS_ARRAY = WORD_DELIMITERS_STRING.toCharArray();
    protected static final Pattern TOKEN_DELIMITERS_MATCH_PATTERN = Pattern.compile("[" + Pattern.quote(TOKEN_DELIMITERS_STRING) + "]");

    static {
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("filename.scanner.skip.keywords", ""), ",;| ");
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        SKIP_KEYWORDS = keywords.toArray(new String[] {});
    }

    protected int firstKeywordIndex = 0;

    public MovieFilenameScanner() {
    }

    public void scan(Movie movie) {
        File fileToScan = movie.getFile();
        String filename = movie.getContainerFile().getName();

        firstKeywordIndex = filename.indexOf("[");
        firstKeywordIndex = (firstKeywordIndex < 2) ? filename.length() : firstKeywordIndex;

        Collection<MovieFile> movieFiles = movie.getFiles();
        for (MovieFile movieFile : movieFiles) {
            if (movieFile.getFirstPart() == 1) {
                movieFile.setPart(getPart(filename));
            }
        }

        if (fileToScan.isFile()) {
            movie.setAudioCodec(getAudioCodec(filename));
            movie.setContainer(getContainer(filename));
            movie.setFps(getFPS(filename));
            movie.setSubtitles(hasSubtitles(fileToScan));
            movie.setVideoCodec(getVideoCodec(filename));
            movie.setVideoOutput(getVideoOutput(filename));
            movie.setVideoSource(getVideoSource(filename));
            movie.setLanguage(getLanguage(filename));
        } else {
            // For DVD images
            movie.setAudioCodec(getAudioCodec(filename));
            movie.setContainer("DVD");
            movie.setFps(getFPS(filename));
            movie.setSubtitles(hasSubtitles(fileToScan));
            movie.setVideoCodec("MPEG2");
            movie.setVideoOutput(getVideoOutput(filename));
            movie.setVideoSource("DVD");
            movie.setLanguage(getLanguage(filename));
        }

        // Skip some keywords
        findKeyword(filename, SKIP_KEYWORDS);

        // Update the movie file with interpreted movie data
        updateTrailer(filename, movie);
        updateTVShow(filename, movie);
        updateMovie(filename, movie);
    }

    /**
     * Get the main audio track codec if any
     * 
     * @param filename
     *            movie's filename to scan
     * @return the audio codec name or Unknown if not found
     */
    protected String getAudioCodec(String filename) {
        return findKeyword(filename.toUpperCase(), new String[] { "AC3", "DTS", "DD", "AAC" });
    }

    /**
     * Get the specified filenames video container. Simply return the movie file's extension.
     * 
     * @param filename
     *            movie's filename to scan
     * @return the container
     */
    protected String getContainer(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        updateFirstKeywordIndex(lastDotIndex);
        return filename.substring(lastDotIndex + 1).toUpperCase();
    }

    /**
     * @return the movie file frame rate when specified in the filename.
     * @param filename
     *            movie's filename to scan
     */
    protected int getFPS(String filename) {
        if (hasKeyword(filename, new String[] { "23p", "p23" })) {
            return 23;
        }
        if (hasKeyword(filename, new String[] { "24p", "p24" })) {
            return 24;
        }
        if (hasKeyword(filename, new String[] { "25p", "p25" })) {
            return 25;
        }
        if (hasKeyword(filename, new String[] { "29p", "p29" })) {
            return 29;
        }
        if (hasKeyword(filename, new String[] { "30p", "p30" })) {
            return 30;
        }
        if (hasKeyword(filename, new String[] { "50p", "p50" })) {
            return 50;
        }
        if (hasKeyword(filename, new String[] { "59p", "p59" })) {
            return 59;
        }
        if (hasKeyword(filename, new String[] { "60p", "p60" })) {
            return 60;
        }
        return 60;
    }

    /**
     * @return the movie file language when specified in the filename.
     * @param filename
     *            movie's filename to scan.
     */
    protected String getLanguage(String filename) {
        if (LANGUAGE_DETECTION) {
            String f = filename.toUpperCase();

            f = replaceWordDelimiters(f, '.');

            if (hasKeyword(f, new String[] { ".FRA.", ".FR.", ".FRENCH.", ".VF." })) {
                return "French";
            }

            if (hasKeyword(f, new String[] { ".GER.", ".DE.", ".GERMAN." })) {
                return "German";
            }

            if (hasKeyword(f, new String[] { ".ITA.", ".IT.", ".ITALIAN." })) {
                return "Italian";
            }

            if (hasKeyword(f, new String[] { ".SPA.", ".ES.", ".SPANISH." })) {
                return "Spanish";
            }

            if (hasKeyword(f, new String[] { ".ENG.", ".EN.", ".ENGLISH." })) {
                return "English";
            }

            if (hasKeyword(f, new String[] { ".POR.", ".PT.", ".PORTUGUESE." })) {
                return "Portuguese";
            }

            if (hasKeyword(f, new String[] { ".RUS.", ".RU.", ".RUSSIAN." })) {
                return "Russian";
            }

            if (hasKeyword(f, new String[] { ".POL.", ".PL.", ".POLISH.", "PLDUB" })) {
                return "Polish";
            }

            if (hasKeyword(f, new String[] { ".HUN.", ".HU.", ".HUNGARIAN." })) {
                return "Hungarian";
            }

            if (hasKeyword(f, new String[] { ".HEB.", ".HE.", ".HEBDUB." })) {
                return "Hebrew";
            }

            if (hasKeyword(f, new String[] { ".VO.", ".VOSTFR." })) {
                return "VO";
            }

            if (hasKeyword(f, new String[] { ".DL." })) {
                return "Dual Language";
            }
        }
        return "Unknown";
    }

    /**
     * @return the specified movie file's title.
     * @param filename
     *            movie's filename to scan.
     */
    protected String getName(String filename) {
        String name = filename.substring(0, firstKeywordIndex);
        if (name.charAt(0) == '[' && name.indexOf(']',1) > 0) {
        	name = name.substring(name.indexOf(']', 1) + 1);
        }
        name = replaceWordDelimiters(name, ' ');
        return name.trim();
    }

	private String replaceWordDelimiters(String str, char newChar) {
		for (char c : WORD_DELIMITERS_ARRAY) {
            str = str.replace(c, newChar);
		}
		return str;
	}

    /**
     * Searches the filename for the keyword, if found is checked to see if the preceding character is a delimiter.
     * 
     * @return the index position of the part if found, -1 if not
     * @param filename
     *            to search
     * @param Keyword
     *            to look for
     */
    protected int getPartKeyword(String gpFilename, String gpKeyword) {
        String gpPrev = ""; // Previous character
        int gpIndex = 0;

        // Search for the Keyword in the file name
        gpIndex = gpFilename.indexOf(gpKeyword);
        while (gpIndex > 0) {
            // We've found the keyword, but is it preceded by a delimiter and therefore not part of a word
            gpPrev = gpFilename.substring(gpIndex - 1, gpIndex);
            if (WORD_DELIMITERS_STRING.indexOf(gpPrev) <= 0) {
                // We can't find the preceding char in the delimiter string
                // so look for the next occurence of the keyword
                gpIndex = gpFilename.indexOf(gpKeyword, gpIndex + 1);
            } else {
                // We've found the keyword, and it's preceded by a delimiter, so quit.
                break;
            }
        }
        return gpIndex;
    }

    protected int getPart(String filename) {
        String f = filename.toUpperCase();
        
        final Pattern testPattern = Pattern.compile("_([0-9]+)_DVD_");
        Matcher matcher = testPattern.matcher(replaceWordDelimiters(f, '_'));
		if (matcher.find()) {
			updateFirstKeywordIndex(matcher.start(0));
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception e) {
                return 1;
            }
        }
        
        
        int index = 0;

        // Issue 259 & 286 - Only the keyword with a delimiter before it will be counted.
        String keyword = "CD";
        index = getPartKeyword(f, keyword);

        if (index == -1) {
            keyword = "DISC";
            index = getPartKeyword(f, keyword);
        }

        if (index == -1) {
            keyword = "DISK";
            index = getPartKeyword(f, keyword);
        }

        if (index == -1) {
            keyword = "PART";
            index = getPartKeyword(f, keyword);
        }

        if (index != -1) {
            updateFirstKeywordIndex(index);
            index += keyword.length();
            int end = index;
            while (end < filename.length() && Character.isDigit(filename.charAt(end))) {
                ++end;
            }
            try {
                return Integer.parseInt(filename.substring(index, end));
            } catch (Exception e) {
                return 1;
            }
        }
        return 1;
    }

    protected String getPartTitle(String filename) {
        String f = filename.toUpperCase();
        int dot = f.lastIndexOf('.');
        if (dot != -1) {
            f = f.substring(0, dot);
        } else {
            dot = f.length();
        }

        String[] keywords = { "CD", "DISC", "DISK", "PART" };
        for (String keyword : keywords) {
            int index = getPartKeyword(f, keyword);

            if (index != -1) {
                int dash = f.lastIndexOf('-');

                if (dash != -1 && dot > dash) {
                    String partTitle = filename.substring(dash + 1, dot).trim();
                    return partTitle;
                }
                return null;
            }
        }
        return null;
    }

    protected String getVideoCodec(String filename) {
        String f = filename.toUpperCase();
        if (hasKeyword(f, "XVID")) {
            return "XviD";
        }
        if (hasKeyword(f, "DIVX")) {
            return "DivX";
        }
        if (hasKeyword(f, new String[] { "H264", "H.264", "X264" })) {
            return "H.264";
        }
        return "Unknown";
    }

    protected String getVideoOutput(String filename) {

        String videoOutput = findKeyword(filename, new String[] { "720p", "1080i", "1080p" });

        int fps = getFPS(filename);
        if (!videoOutput.equalsIgnoreCase("Unknown")) {
            switch (fps) {
            case 23:
                videoOutput = "1080p 23.976Hz";
                break;
            case 24:
                videoOutput = "1080p 24Hz";
                break;
            case 25:
                videoOutput = "1080p 25Hz";
                break;
            case 29:
                videoOutput = "1080p 29.97Hz";
                break;
            case 30:
                videoOutput = "1080p 30Hz";
                break;
            case 50:
                videoOutput += " 50Hz";
                break;
            case 59:
                videoOutput += "1080p 59.94Hz";
                break;
            case 60:
                videoOutput += " 60Hz";
                break;
            default:
                videoOutput += " 60Hz";
            }
        } else {
            switch (fps) {
            case 23:
                videoOutput = "23p";
                break;
            case 24:
                videoOutput = "24p";
                break;
            case 25:
                videoOutput = "PAL";
                break;
            case 29:
                videoOutput = "NTSC";
                break;
            case 30:
                videoOutput = "NTSC";
                break;
            case 49:
                videoOutput = "PAL";
                break;
            case 50:
                videoOutput = "PAL";
                break;
            case 60:
                videoOutput = "NTSC";
                break;
            default:
                videoOutput = "NTSC";
                break;
            }
        }

        return videoOutput;
    }

    /**
     * Get the file's video source as specified in the filename.
     * 
     * @param filename
     *            filename of the movie file.
     * @return the video source as a string
     * 
     * @author jjulien, quickfinga
     */
    protected String getVideoSource(String filename) {
        String f = filename.toUpperCase();
        if (hasKeyword(f, "HDTV")) {
            return "HDTV";
        }
        if (hasKeyword(f, "PDTV")) {
            return "PDTV";
        }
        if (hasKeyword(f, new String[] { "BLURAY", "BDRIP", "BLURAYRIP", "BLU-RAY" })) {
            return "BluRay";
        }
        if (hasKeyword(f, "DVDRIP")) {
            return "DVDRip";
        }
        if (hasKeyword(f, "DVDSCR")) {
            return "DVDSCR";
        }
        if (hasKeyword(f, "DSRIP")) {
            return "DSRip";
        }
        if (hasKeyword(filename, new String[] { " TS ", ".TS." })) {
            return "TS";
        }
        if (hasKeyword(filename, "CAM")) {
            return "CAM";
        }
        if (hasKeyword(filename, "R5")) {
            return "R5";
        }
        if (hasKeyword(filename, "LINE")) {
            return "LINE";
        }
        if (hasKeyword(filename, new String[] { "HDDVD", "HD-DVD", "HDDVDRIP" })) {
            return "HDDVD";
        }
        if (hasKeyword(filename, new String[] { "DTH", "D-THEATER", "DTHEATER" })) {
            return "D-THEATER";
        }
        if (hasKeyword(filename, "HD2DVD")) {
            return "HD2DVD";
        }
        if (hasKeyword(f, new String[] { "DVD", "NTSC", "PAL" })) {
            return "DVD";
        }
        if (hasKeyword(f, new String[] { "720p", "1080p", "1080i" })) {
            return "HDTV";
        }
        return "Unknown";
    }

    protected boolean hasSubtitles(File fileToScan) {
        String path = fileToScan.getAbsolutePath();
        int index = path.lastIndexOf(".");
        String basename = path.substring(0, index + 1);

        if (index >= 0) {
            return (new File(basename + "srt").exists() || new File(basename + "SRT").exists() || new File(basename + "sub").exists()
                            || new File(basename + "SUB").exists() || new File(basename + "smi").exists() || new File(basename + "SMI").exists()
                            || new File(basename + "ssa").exists() || new File(basename + "SSA").exists());
        }

        String fn = path.toUpperCase();
        if (hasKeyword(fn, "VOST")) {
            return true;
        }
        return false;
    }

    protected void updateFirstKeywordIndex(int index) {
        if (index > 0) {
            firstKeywordIndex = (firstKeywordIndex > index) ? index : firstKeywordIndex;
        }
    }

    protected void updateTrailer(String filename, Movie movie) {
        int beginIdx = filename.indexOf("[");
        while (beginIdx > -1) {
            int endIdx = filename.indexOf("]", beginIdx);
            if (endIdx > -1) {
                String token = filename.substring(beginIdx + 1, endIdx).toUpperCase();
                if (token.indexOf("TRAILER") > -1) {
                    movie.setTrailer(true);
                    String tmp = movie.getBaseName();
                    movie.getFirstFile().setTitle(tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")));
                    break;
                }
            } else {
                break;
            }

            beginIdx = filename.indexOf("[", endIdx + 1);
        }
    }

    protected void updateMovie(String filename, Movie movie) {
        try {
            String partTitle = getPartTitle(filename);
            if (partTitle != null) {
                movie.getFirstFile().setTitle(partTitle);
            }

            partTitle = movie.getFirstFile().getTitle();
            if ((partTitle != null) && !partTitle.equals(Movie.UNKNOWN)) {
                int dash = filename.lastIndexOf('-');
                if (dash != -1) {
                    filename = filename.substring(0, dash);
                }
            }
            // Extract the 4 digit year from the file name
            StringTokenizer st = new StringTokenizer(filename, WORD_DELIMITERS_STRING);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                // Year
                if ((token.length() == 4) && token.matches("\\d{4}") && (Integer.parseInt(token) > 1919) && (Integer.parseInt(token) < 2399)) {
                    updateFirstKeywordIndex(filename.indexOf(token));
                    movie.setYear(token.substring(0, 4));
                }
            }

            st = new StringTokenizer(filename, "-");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith(" ")) {
                    updateFirstKeywordIndex(filename.indexOf(token));
                }
            }

            movie.setTitle(getName(filename));
        } catch (Exception e) {
            movie.setTitle(Movie.UNKNOWN);
        }
    }

    private int indexOfAny(String text, int startPos, String delims) {
        char[] chars = text.toCharArray();
        for (int index = startPos; index < chars.length; ++index) {
            if (delims.indexOf(chars[index]) != -1)
                return index;
        }
        return -1;
    }

    protected void updateTVShow(String filename, Movie movie) {
        try {
            Matcher matcher = TV_PATTERN.matcher(filename);
            if (matcher.find()) {
                String group0 = matcher.group(0);

                // logger.finest("It's a TV Show: " + group0);

                updateFirstKeywordIndex(matcher.start());

                String fileTitle = null;

                int end = matcher.end(0);
                int dash = filename.indexOf('-', end);
                if ((dash == end) || (dash == end + 1)) {
                    int delim = indexOfAny(filename, dash, TOKEN_DELIMITERS_STRING);
                    if (delim == -1)
                        delim = filename.length();
                    fileTitle = replaceWordDelimiters(filename.substring(dash + 1, delim), ' ').trim();
                }

                int season = Integer.parseInt(matcher.group(1));
                movie.setSeason(season);

                int firstPart = -1;
                int lastPart = -1;

                matcher = EPISODE_PATTERN.matcher(group0);
                while (matcher.find()) {
                    int episode = Integer.parseInt(matcher.group(1));
                    if (firstPart == -1) {
                        firstPart = lastPart = episode;
                    } else {
                    	if (episode < firstPart) {
                    		firstPart = episode;
                    	} else if (episode > lastPart) {
                    		lastPart = episode;
                    	}
                    }
                }

                MovieFile firstFile = movie.getFirstFile();
                firstFile.setPart(firstPart);
                firstFile.setLastPart(lastPart);

                if (fileTitle != null && !movie.isTrailer()) {
                    movie.getFirstFile().setTitle(fileTitle.trim());
                }
            }
        } catch (Exception e) {
            //
        }
    }

    protected String findKeyword(String filename, String[] strings) {
        String name = replaceWordDelimiters(filename.toUpperCase(), ' ');

        String val = "Unknown";
        for (String keyword : strings) {
            String upperKeyword = " " + keyword.toUpperCase() + " ";
            int index = name.indexOf(upperKeyword);
            if (index > 0) {
                updateFirstKeywordIndex(index);
                val = keyword;
            }
        }
        return val;
    }

    /**
     * @return true when the specified keyword exist in the specified filename
     */
    protected boolean hasKeyword(String filename, String keyword) {
        return hasKeyword(filename, new String[] { keyword });
    }

    /**
     * @return true when one of the specified keywords exist in the specified filename
     */
    protected boolean hasKeyword(String filename, String[] keywords) {
        for (String keyword : keywords) {
            int index = filename.indexOf(keyword);
            if (index > 0) {
                updateFirstKeywordIndex(index);
                return true;
            }
        }
        return false;
    }

    /**
     * @return true when one of the specified keywords exist in the specified filename
     */
    protected boolean hasKeywordAfterTitle(String filename, String[] keywords) {
        for (String keyword : keywords) {
            int index = filename.indexOf(keyword);
            if (index >= firstKeywordIndex) {
                return true;
            }
        }
        return false;
    }
}
