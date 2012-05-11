/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.model;

import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.BooleanYesNoAdapter;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;

@SuppressWarnings("serial")
@XmlType
public class MovieFile implements Comparable<MovieFile> {

    private static final Logger logger = Logger.getLogger(MovieFile.class);
    private String filename = Movie.UNKNOWN;
    private String archiveName = null;
    private int season = -1;    // The season associated with the movie file
    private int firstPart = 1;  // #1, #2, CD1, CD2, etc.
    private int lastPart = 1;
    private boolean newFile = Boolean.TRUE;             // is new file or already exists in XML data
    private boolean subtitlesExchange = false;  // Are the subtitles for this file already downloaded/uploaded to the server
    private Map<String, String> playLink = null;
    private LinkedHashMap<Integer, String> titles = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> plots = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageURL = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageFilename = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> airsAfterSeason = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> airsBeforeSeason = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> airsBeforeEpisode = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> firstAired = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> ratings = new LinkedHashMap<Integer, String>();
    private File file;
    private MovieFileNameDTO info;
    private boolean watched = false;
    private long watchedDate = 0;
    private boolean playFullBluRayDisk = PropertiesUtil.getBooleanProperty("mjb.playFullBluRayDisk", "true");
    private boolean includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");
    private boolean includeVideoImages = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
    private boolean includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", "false");
    private String playLinkVOD = PropertiesUtil.getProperty("filename.scanner.types.suffix.VOD", "");
    private String playLinkZCD = PropertiesUtil.getProperty("filename.scanner.types.suffix.ZCD", "2");
    private static final Map<String, Pattern> TYPE_SUFFIX_MAP = new HashMap<String, Pattern>() {

        {
            String scannerTypes = PropertiesUtil.getProperty("filename.scanner.types", "ZCD,VOD");

            HashMap<String, String> scannerTypeDefaults = new HashMap<String, String>() {

                {
                    put("ZCD", "ISO,IMG,VOB,MDF,NRG,BIN");
                    put("VOD", "");
                    put("RAR", "RAR");
                }
            };

            for (String s : scannerTypes.split(",")) {
                // Set the default the long way to allow 'keyword.???=' to blank the value instead of using default
                String mappedScannerTypes = PropertiesUtil.getProperty("filename.scanner.types." + s, null);
                if (null == mappedScannerTypes) {
                    mappedScannerTypes = scannerTypeDefaults.get(s);
                }

                StringBuilder patt = new StringBuilder(s);
                if (null != mappedScannerTypes && mappedScannerTypes.length() > 0) {
                    for (String t : mappedScannerTypes.split(",")) {
                        patt.append("|").append(t);
                    }
                }
                put(s, MovieFilenameScanner.iwpatt(patt.toString()));
            }
        }
    };

    @XmlElement(name = "fileURL")
    public String getFilename() {
        return filename;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getTitle(int part) {
        String title = titles.get(part);
        return title != null ? title : Movie.UNKNOWN;
    }

    public void setTitle(int part, String title) {
        if (title == null || title.isEmpty()) {
            title = Movie.UNKNOWN;
        }
        titles.put(part, title);
    }

    public void setTitle(String title) {
        if (title != null) {
            setTitle(firstPart, title);
        }
    }

    public String getPlot(int part) {
        String plot = plots.get(part);
        return plot != null ? plot : Movie.UNKNOWN;
    }

    public void setPlot(int part, String plot) {
        if (plot == null || plot.isEmpty()) {
            plot = Movie.UNKNOWN;
        }
        plots.put(part, plot);
    }

    public String getRating(int part) {
        String rating = ratings.get(part);
        return rating != null ? rating : Movie.UNKNOWN;
    }

    public void setRating(int part, String rating) {
        if (StringUtils.isBlank(rating)) {
            rating = Movie.UNKNOWN;
        }
        ratings.put(part, rating);
    }

    public String getVideoImageURL(int part) {
        String url = videoImageURL.get(part);
        return url != null ? url : Movie.UNKNOWN;
    }

    public String getVideoImageFilename(int part) {
        String viFile = videoImageFilename.get(part);
        return viFile != null ? viFile : Movie.UNKNOWN;
    }

    public void setVideoImageURL(int part, String videoImageURL) {
        if (videoImageURL == null || videoImageURL.isEmpty()) {
            videoImageURL = Movie.UNKNOWN;
        }
        this.videoImageURL.put(part, videoImageURL);
        // Clear the videoImageFilename associated with this part
        this.videoImageFilename.put(part, Movie.UNKNOWN);
    }

    public void setVideoImageFilename(int part, String videoImageFilename) {
        if (videoImageFilename == null || videoImageFilename.isEmpty()) {
            videoImageFilename = Movie.UNKNOWN;
        }
        this.videoImageFilename.put(part, videoImageFilename);
    }

    @XmlAttribute
    public int getFirstPart() {
        return firstPart;
    }

    @XmlAttribute
    public int getLastPart() {
        return lastPart;
    }

    public void setPart(int part) {
        firstPart = lastPart = part;
    }

    public void setFirstPart(int part) {
        firstPart = part;
        if (firstPart > lastPart) {
            lastPart = firstPart;
        }
    }

    public void setLastPart(int part) {
        lastPart = part;
    }

    /**
     * Return the composite title for all parts of the movie file
     *
     * @return
     */
    @XmlAttribute
    public String getTitle() {
        if (titles.size() == 0) {
            return Movie.UNKNOWN;
        }

        if (firstPart == lastPart) {
            return (titles.get(firstPart) == null ? Movie.UNKNOWN : titles.get(firstPart));
        }

        boolean oneValidTitle = false;
        StringBuilder title = new StringBuilder();

        for (int loop = firstPart; loop <= lastPart; loop++) {
            String titlePart = getTitle(loop);
            if (StringTools.isValidString(titlePart)) {
                oneValidTitle = Boolean.TRUE;
            }

            if (title.length() > 0) {
                title.append(Movie.SPACE_SLASH_SPACE);
            }
            title.append(titlePart);
        }
        return oneValidTitle ? title.toString() : Movie.UNKNOWN;
    }

    public boolean hasTitle() {
        return !Movie.UNKNOWN.equals(getTitle());
    }

    @XmlAttribute
    public boolean isNewFile() {
        return newFile;
    }

    public void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    @XmlAttribute
    @XmlJavaTypeAdapter(BooleanYesNoAdapter.class)
    public Boolean isSubtitlesExchange() {
        return subtitlesExchange;
    }

    public void setSubtitlesExchange(Boolean subtitlesExchange) {
        this.subtitlesExchange = subtitlesExchange;
    }

    //the trick here is, files are ALWAYS FileEx instances and values are cached!
    @XmlAttribute
    public long getSize() {
        return getFile() == null ? 0 : getFile().length();
    }

    public long getLastModified() {
        return getFile() == null ? 0 : getFile().lastModified();
    }

    /*
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object. The implementor
     * must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for all x and y.
     * (This implies that x.compareTo(y) must throw an exception if
     * y.compareTo(x) throws an exception.)
     *
     * The implementor must also ensure that the relation is transitive:
     * (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
     *
     * Finally, the implementor must ensure that x.compareTo(y)==0 implies that
     * sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
     *
     * It is strongly recommended, but not strictly required that
     * (x.compareTo(y)==0) == (x.equals(y)). Generally speaking, any class that
     * implements the Comparable interface and violates this condition should
     * clearly indicate this fact. The recommended language is "Note: this class
     * has a natural ordering that is inconsistent with equals."
     *
     * In the foregoing description, the notation sgn(expression) designates the
     * mathematical signum function, which is defined to return one of -1, 0, or
     * 1 according to whether the value of expression is negative, zero or
     * positive.
     *
     *
     * Parameters: o - the object to be compared. Returns: a negative integer,
     * zero, or a positive integer as this object is less than, equal to, or
     * greater than the specified object. Throws: ClassCastException - if the
     * specified object's type prevents it from being compared to this object.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MovieFile anotherMovieFile) {
        return this.getFirstPart() - anotherMovieFile.getFirstPart();
    }

    @XmlTransient
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void mergeFileNameDTO(MovieFileNameDTO dto) {

        info = dto;

        // Set the part title, keeping each episode title
        if (dto.isExtra()) {
            setTitle(dto.getPartTitle());
        } else if (dto.getSeason() >= 0) {
            // Set the season for the movie file
            setSeason(dto.getSeason());

            // Set the episode title for each of the episodes in the file
            for (int episodeNumber : dto.getEpisodes()) {
                setTitle(episodeNumber, dto.getEpisodeTitle());
            }
        } else {
            setTitle(dto.getPartTitle());
        }

        if (dto.getEpisodes().size() > 0) {
            lastPart = 0;
            firstPart = Integer.MAX_VALUE;
            for (int e : dto.getEpisodes()) {
                if (e >= lastPart) {
                    lastPart = e;
                }
                if (e <= firstPart) {
                    firstPart = e;
                }
            }

            if (firstPart > lastPart) {
                firstPart = lastPart;
            }

        } else if (dto.getPart() > 0) {
            firstPart = lastPart = dto.getPart();
            setTitle(dto.getPartTitle());
        } else {
            firstPart = 1;
            lastPart = 1;
            setTitle(dto.getPartTitle());
        }
    }

    @XmlElement
    public MovieFileNameDTO getInfo() {
        return info;
    }

    public void setInfo(MovieFileNameDTO info) {
        this.info = info;
    }

    //this is expensive too
    public synchronized Map<String, String> getPlayLink() {
        if (playLink == null) {
            playLink = calculatePlayLink();
        }
        return playLink;
    }

    public void addPlayLink(String key, String value) {
        playLink.put(key, value);
    }

    @SuppressWarnings("unused")
    private void setPlayLink(Map<String, String> playLink) {
        if (playLink == null || playLink.containsValue("")) {
            this.playLink = calculatePlayLink();
        } else {
            this.playLink = playLink;
        }
    }

    public static class PartDataDTO {

        @XmlAttribute
        public int part;
        @XmlValue
        public String value;

        public PartDataDTO() {
        }

        public PartDataDTO(int part, String value) {
            this.part = part;
            this.value = value;
        }
    }

    @XmlElement(name = "filePlot")
    public List<PartDataDTO> getFilePlots() {
        if (!includeEpisodePlots) {
            return null;
        }

        return toPartDataList(plots);
    }

    public void setFilePlots(List<PartDataDTO> list) {
        fromPartDataList(plots, list);
    }

    @XmlElement(name = "fileRating")
    public List<PartDataDTO> getFileRating() {
        if (!includeEpisodeRating) {
            return null;
        }

        return toPartDataList(ratings);
    }

    public void setFileRating(List<PartDataDTO> list) {
        fromPartDataList(ratings, list);
    }

    @XmlElement(name = "fileImageURL")
    public List<PartDataDTO> getFileImageUrls() {
        if (!includeVideoImages) {
            return null;
        }

        return toPartDataList(videoImageURL);
    }

    public void setFileImageUrls(List<PartDataDTO> list) {
        fromPartDataList(videoImageURL, list);
    }

    @XmlElement(name = "fileImageFile")
    public List<PartDataDTO> getFileImageFiles() {
        if (!includeVideoImages) {
            return null;
        }

        return toPartDataList(videoImageFilename);
    }

    public void setFileImageFiles(List<PartDataDTO> list) {
        fromPartDataList(videoImageFilename, list);
    }

    private static List<PartDataDTO> toPartDataList(LinkedHashMap<Integer, String> map) {
        List<PartDataDTO> list = new ArrayList<PartDataDTO>();

        for (Map.Entry<Integer, String> part : map.entrySet()) {
            list.add(new PartDataDTO(part.getKey(), part.getValue()));
        }

        return list;
    }

    private static void fromPartDataList(LinkedHashMap<Integer, String> map, List<PartDataDTO> list) {
        map.clear();
        for (PartDataDTO p : list) {
            map.put(p.part, p.value);
        }
    }

    /**
     * Calculate the playlink additional information for the file
     */
    private Map<String, String> calculatePlayLink() {
        Map<String, String> playLinkMap = new HashMap<String, String>();
        File filePlayLink = this.getFile();

        // Check that the file isn't null before continuing
        if (filePlayLink == null) {
            return playLinkMap;
        }

        try {
            if (playFullBluRayDisk && filePlayLink.getAbsolutePath().toUpperCase().contains("BDMV")) {
                //logger.finest(filename + " matched to BLURAY");
                playLinkMap.put("zcd", playLinkZCD);
                // We can return at this point because there won't be additional playlinks
                return playLinkMap;
            }

            if (filePlayLink.isDirectory() && (new File(filePlayLink.getAbsolutePath() + File.separator + "VIDEO_TS").exists())) {
                //logger.finest(filename + " matched to VIDEO_TS");
                playLinkMap.put("zcd", playLinkZCD);
                // We can return at this point because there won't be additional playlinks
                return playLinkMap;
            }

            for (Map.Entry<String, Pattern> e : TYPE_SUFFIX_MAP.entrySet()) {
                Matcher matcher = e.getValue().matcher(getExtension(filePlayLink));
                if (matcher.find()) {
                    //logger.finest(filename + " matched to " + e.getKey());
                    playLinkMap.put(e.getKey(), PropertiesUtil.getProperty("filename.scanner.types.suffix." + e.getKey().toUpperCase(), ""));
                }
            }
        } catch (Exception error) {
            logger.error("Error calculating playlink for file " + filePlayLink.getName());
            logger.error(SystemTools.getStackTrace(error));
        } finally {
            // Default to VOD if there's no other type found
            if (playLinkMap.isEmpty()) {
                //logger.finest(filename + " not matched, defaulted to VOD");
                playLinkMap.put("vod", playLinkVOD);
            }
        }

        return playLinkMap;
    }

    /**
     * Return the extension of the file, this will be blank for directories
     *
     * @param file
     * @return
     */
    private String getExtension(File file) {
        String extFilename = file.getName();

        if (file.isFile()) {
            int i = extFilename.lastIndexOf(".");
            if (i > 0) {
                return new String(extFilename.substring(i + 1));
            }
        }
        return "";
    }

    // Read the watched flag
    public boolean isWatched() {
        return watched;
    }

    // Set the watched flag
    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public void setWatchedDateString(String watchedDate) {
        try {
            if (StringTools.isNotValidString(watchedDate)) {
                this.watchedDate = 0;
            } else {
                this.watchedDate = DateTime.parse(watchedDate).toMillis();
            }
        } catch (Exception error) {
            this.watchedDate = 0;
        }
    }

    public void setWatchedDate(long watchedDate) {
        this.watchedDate = watchedDate;
    }

    public String getWatchedDateString() {
        if (watchedDate == 0) {
            return Movie.UNKNOWN;
        } else {
            return new DateTime(watchedDate).toString(Movie.dateFormatLongString);
        }
    }

    public long getWatchedDate() {
        return this.watchedDate;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getAirsAfterSeason(int part) {
        String aaSeason = airsAfterSeason.get(part);
        return StringUtils.isNotBlank(aaSeason) ? aaSeason : Movie.UNKNOWN;
    }

    public void setAirsAfterSeason(int part, String season) {
        if (StringUtils.isBlank(season)) {
            season = Movie.UNKNOWN;
        }
        this.airsAfterSeason.put(part, season);
    }

    @XmlElement(name = "airsAfterSeason")
    public List<PartDataDTO> getAirsAfterSeasons() {
        return toPartDataList(airsAfterSeason);
    }

    public void setAirsAfterSeasons(List<PartDataDTO> list) {
        fromPartDataList(airsAfterSeason, list);
    }

    public String getAirsBeforeSeason(int part) {
        String abSeason = airsBeforeSeason.get(part);
        return StringUtils.isNotBlank(abSeason) ? abSeason : Movie.UNKNOWN;
    }

    public void setAirsBeforeSeason(int part, String season) {
        if (StringUtils.isBlank(season)) {
            season = Movie.UNKNOWN;
        }
        this.airsBeforeSeason.put(part, season);
    }

    @XmlElement(name = "airsAfterSeason")
    public List<PartDataDTO> getAirsBeforeSeasons() {
        return toPartDataList(airsBeforeSeason);
    }

    public void setAirsBeforeSeasons(List<PartDataDTO> list) {
        fromPartDataList(airsBeforeSeason, list);
    }

    public String getAirsBeforeEpisode(int part) {
        String abEpisode = airsBeforeEpisode.get(part);
        return StringUtils.isNotBlank(abEpisode) ? abEpisode : Movie.UNKNOWN;
    }

    public void setAirsBeforeEpisode(int part, String episode) {
        if (StringUtils.isBlank(episode)) {
            episode = Movie.UNKNOWN;
        }
        this.airsBeforeEpisode.put(part, episode);
    }

    @XmlElement(name = "airsAfterEpisode")
    public List<PartDataDTO> getAirsBeforeEpisodes() {
        return toPartDataList(airsBeforeEpisode);
    }

    public void setAirsBeforeEpisodes(List<PartDataDTO> list) {
        fromPartDataList(airsBeforeEpisode, list);
    }

    public String getFirstAired(int part) {
        String airDate = firstAired.get(part);
        return StringUtils.isNotBlank(airDate) ? airDate : Movie.UNKNOWN;
    }

    public void setFirstAired(int part, String airDate) {
        if (StringUtils.isBlank(airDate)) {
            airDate = Movie.UNKNOWN;
        }
        this.firstAired.put(part, airDate);
    }

    @XmlElement(name = "firstAired")
    public List<PartDataDTO> getFirstAired() {
        return toPartDataList(firstAired);
    }

    public void setFirstAired(List<PartDataDTO> list) {
        fromPartDataList(firstAired, list);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[MovieFile ");
        sb.append("[filename=").append(filename);
        sb.append("][season=").append(season);
        sb.append("][firstPart=").append(firstPart);
        sb.append("][lastPart=").append(lastPart);
        sb.append("][newFile=").append(newFile);
        sb.append("][subtitlesExchange=").append(subtitlesExchange);
        sb.append("][playLink=").append(playLink);
        sb.append("][titles=").append(titles);
        sb.append("][plots=").append(plots);
        sb.append("][videoImageURL=").append(videoImageURL);
        sb.append("][videoImageFilename=").append(videoImageFilename);
        sb.append("][airsAfterSeason=").append(airsAfterSeason);
        sb.append("][airsBeforeSeason=").append(airsBeforeSeason);
        sb.append("][airsBeforeEpisode=").append(airsBeforeEpisode);
        sb.append("][firstAired=").append(firstAired);
        sb.append("][ratings=").append(ratings);
        sb.append("][file=").append(file);
        sb.append("][info=").append(info);
        sb.append("][watched=").append(watched);
        sb.append("][watchedDate=").append(watchedDate);
        sb.append("][playFullBluRayDisk=").append(playFullBluRayDisk);
        sb.append("][includeEpisodePlots=").append(includeEpisodePlots);
        sb.append("][includeVideoImages=").append(includeVideoImages);
        sb.append("][includeEpisodeRating=").append(includeEpisodeRating);
        sb.append("][playLinkVOD=").append(playLinkVOD);
        sb.append("][playLinkZCD=").append(playLinkZCD);
        sb.append("][archivename=").append(archiveName);
        sb.append("]]");
        return sb.toString();
    }
}
