package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

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
 */
public class MovieFilenameScanner {

    protected static String[] skipKeywords;
    protected int firstKeywordIndex = 0;
    protected static boolean languageDetection = Boolean.parseBoolean(PropertiesUtil.getProperty("filename.scanner.language.detection", "true"));

    static {
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("filename.scanner.skip.keywords", ""), ",;| ");
        Collection<String> keywords = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        skipKeywords = keywords.toArray(new String[] {});
    }

    public MovieFilenameScanner() {
    }

    public void scan(Movie movie) {
        File fileToScan = movie.getFile();
        String filename = fileToScan.getName();

        firstKeywordIndex = filename.indexOf("[");
        firstKeywordIndex = (firstKeywordIndex == -1) ? filename.length() : firstKeywordIndex;

        Collection<MovieFile> movieFiles = movie.getFiles();
        for (MovieFile movieFile : movieFiles) {
            movieFile.setPart(getPart(filename));
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
        findKeyword(filename, skipKeywords);

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
        if (hasKeyword(filename, new String[] { "23p", "p23" }))
            return 23;
        if (hasKeyword(filename, new String[] { "24p", "p24" }))
            return 24;
        if (hasKeyword(filename, new String[] { "25p", "p25" }))
            return 25;
        if (hasKeyword(filename, new String[] { "29p", "p29" }))
            return 29;
        if (hasKeyword(filename, new String[] { "30p", "p30" }))
            return 30;
        if (hasKeyword(filename, new String[] { "50p", "p50" }))
            return 50;
        if (hasKeyword(filename, new String[] { "59p", "p59" }))
            return 59;
        if (hasKeyword(filename, new String[] { "60p", "p60" }))
            return 60;
        return 60;
    }

    /**
     * @return the movie file language when specified in the filename.
     * @param filename
     *            movie's filename to scan.
     */
    protected String getLanguage(String filename) {
        if (languageDetection) {
            String f = filename.toUpperCase();

            f = f.replace("-", ".");
            f = f.replace("_", ".");
            f = f.replace("[", ".");
            f = f.replace("]", ".");
            f = f.replace("(", ".");
            f = f.replace(")", ".");

            if (hasKeyword(f, new String[] { ".FRA.", ".FR.", ".FRENCH.", ".VF.", " VF " }))
                return "French";

            if (hasKeyword(f, new String[] { ".GER.", ".DE.", ".GERMAN." }))
                return "German";

            if (hasKeyword(f, new String[] { ".ITA.", ".IT.", ".ITALIAN." }))
                return "Italian";

            if (hasKeyword(f, new String[] { ".SPA.", ".ES.", ".SPANISH." }))
                return "Spanish";

            if (hasKeyword(f, new String[] { ".ENG.", ".EN.", ".ENGLISH." }))
                return "English";

            if (hasKeyword(f, new String[] { ".POR.", ".PT.", ".PORTUGUESE." }))
                return "Portuguese";

            if (hasKeyword(f, new String[] { ".RUS.", ".RU.", ".RUSSIAN." }))
                return "Russian";

            if (hasKeyword(f, new String[] { ".POL.", ".PL.", ".POLISH.", "PLDUB" }))
                return "Polish";

            if (hasKeyword(f, new String[] { ".HUN.", ".HU.", ".HUNGARIAN." }))
                return "Hungarian";

            if (hasKeyword(f, new String[] { ".HEB.", ".HE.", ".HEBDUB." }))
                return "Hebrew";

            if (hasKeyword(f, new String[] { ".VO.", ".VOSTFR." }))
                return "VO";

            if (hasKeyword(f, new String[] { ".DL." }))
                return "Dual Language";
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
        name = name.replace(".", " ");
        name = name.replace("_", " ");
        name = name.replace("-", " ");
        name = name.replace("[", " ");
        name = name.replace("]", " ");
        name = name.replace("(", " ");
        name = name.replace(")", " ");
        return name.trim();
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
        String gpDelim = " ._-[]()"; // List of delimiters to check for
        String gpPrev = ""; // Previous character
        int gpIndex = 0;

        // Search for the Keyword in the file name
        gpIndex = gpFilename.indexOf(gpKeyword);
        while (gpIndex > 0) {
            // We've found the keyword, but is it preceded by a delimiter and therefore not part of a word
            gpPrev = gpFilename.substring(gpIndex - 1, gpIndex);
            if (gpDelim.indexOf(gpPrev) <= 0) {
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
            while (end < filename.length() && Character.isDigit(filename.charAt(end)))
                ++end;
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
        if (dot != -1)
            f = f.substring(0, dot);

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
        if (hasKeyword(f, "XVID"))
            return "XviD";
        if (hasKeyword(f, "DIVX"))
            return "DivX";
        if (hasKeyword(f, new String[] { "H264", "H.264", "X264" }))
            return "H.264";
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
        if (hasKeyword(f, "HDTV"))
            return "HDTV";
        if (hasKeyword(f, "PDTV"))
            return "PDTV";
        if (hasKeyword(f, new String[] { "BLURAY", "BDRIP", "BLURAYRIP", "BLU-RAY" }))
            return "BluRay";
        if (hasKeyword(f, "DVDRIP"))
            return "DVDRip";
        if (hasKeyword(f, "DVDSCR"))
            return "DVDSCR";
        if (hasKeyword(f, "DSRIP"))
            return "DSRip";
        if (hasKeyword(filename, new String[] { " TS ", ".TS." }))
            return "TS";
        if (hasKeyword(filename, "CAM"))
            return "CAM";
        if (hasKeyword(filename, "R5"))
            return "R5";
        if (hasKeyword(filename, "LINE"))
            return "LINE";
        if (hasKeyword(filename, new String[] { "HDDVD", "HD-DVD", "HDDVDRIP" }))
            return "HDDVD";
        if (hasKeyword(filename, new String[] { "DTH", "D-THEATER", "DTHEATER" }))
            return "D-THEATER";
        if (hasKeyword(filename, "HD2DVD"))
            return "HD2DVD";
        if (hasKeyword(f, new String[] { "DVD", "NTSC", "PAL" }))
            return "DVD";
        if (hasKeyword(f, new String[] { "720p", "1080p", "1080i" }))
            return "HDTV";
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
        if (hasKeyword(fn, "VOST"))
            return true;
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
            StringTokenizer st = new StringTokenizer(filename, ". []()-");
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
            String partTitle = getPartTitle(filename);
            if (partTitle != null)
                movie.getFirstFile().setTitle(partTitle);
        } catch (Exception e) {
            movie.setTitle(Movie.UNKNOWN);
        }
    }

    protected void updateTVShow(String filename, Movie movie) {
        try {
            StringTokenizer st = new StringTokenizer(filename, ". []-_");
            while (st.hasMoreTokens()) {
                String origToken = st.nextToken();
                String token = origToken.toUpperCase();

                // S???E???? variable format
                int eIdx = token.indexOf("E");
                if (token.startsWith("S") && eIdx > 1 && eIdx < (token.length() - 1)) {
                    boolean isValid = true;

                    StringBuffer season = new StringBuffer();
                    String sToken = token.substring(1, eIdx);
                    for (char c : sToken.toCharArray()) {
                        if (Character.isDigit(c)) {
                            season.append(c);
                        } else {
                            isValid = false;
                            break;
                        }
                    }

                    StringBuffer episode = null;
                    if (isValid) {
                        episode = new StringBuffer();
                        String eToken = token.substring(eIdx + 1);
                        for (char c : eToken.toCharArray()) {
                            if (Character.isDigit(c)) {
                                episode.append(c);
                            } else {
                                isValid = false;
                                break;
                            }
                        }
                    }

                    if (isValid) {
                        updateFirstKeywordIndex(filename.indexOf(origToken));
                        movie.setSeason(Integer.parseInt(season.toString()));
                        movie.getFirstFile().setPart(Integer.parseInt(episode.toString()));

                        int beginIndex = filename.lastIndexOf("-");
                        int endIndex = filename.lastIndexOf(".");
                        if (beginIndex >= 0 && endIndex > beginIndex) {
                            if (!movie.isTrailer()) {
                                movie.getFirstFile().setTitle(filename.substring(beginIndex + 1, endIndex).trim());
                            }
                        } else {
                            if (!movie.isTrailer()) {
                                movie.getFirstFile().setTitle(Movie.UNKNOWN);
                            }
                        }
                    }
                }

                // ?x?? variable format
                int xIdx = token.indexOf("X");
                if (Character.isDigit(token.charAt(0)) && xIdx > 0 && xIdx < (token.length() - 1) && Character.isDigit(token.charAt(token.length() - 1))) {
                    boolean isValid = true;

                    StringBuffer season = new StringBuffer();
                    String sToken = token.substring(0, xIdx);
                    for (char c : sToken.toCharArray()) {
                        if (Character.isDigit(c)) {
                            season.append(c);
                        } else {
                            isValid = false;
                            break;
                        }
                    }

                    StringBuffer episode = null;
                    if (isValid) {
                        episode = new StringBuffer();
                        String eToken = token.substring(xIdx + 1);
                        for (char c : eToken.toCharArray()) {
                            if (Character.isDigit(c)) {
                                episode.append(c);
                            } else {
                                isValid = false;
                                break;
                            }
                        }
                    }

                    if (isValid) {
                        updateFirstKeywordIndex(filename.indexOf(origToken));
                        movie.setSeason(Integer.parseInt(season.toString()));
                        movie.getFirstFile().setPart(Integer.parseInt(episode.toString()));

                        int beginIndex = filename.lastIndexOf("-");
                        int endIndex = filename.lastIndexOf(".");
                        if (beginIndex >= 0 && endIndex > beginIndex) {
                            if (!movie.isTrailer()) {
                                movie.getFirstFile().setTitle(filename.substring(beginIndex + 1, endIndex).trim());
                            }
                        } else {
                            if (!movie.isTrailer()) {
                                movie.getFirstFile().setTitle(Movie.UNKNOWN);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //
        }
    }

    protected String findKeyword(String filename, String[] strings) {
        String name = filename.toUpperCase();
        name = name.replace(".", " ");
        name = name.replace("_", " ");
        name = name.replace("-", " ");
        name = name.replace("[", " ");
        name = name.replace("]", " ");
        name = name.replace("(", " ");
        name = name.replace(")", " ");

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
