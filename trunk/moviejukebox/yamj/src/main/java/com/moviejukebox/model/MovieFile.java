/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package com.moviejukebox.model;

import com.moviejukebox.model.Attachment.Attachment;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.BooleanYesNoAdapter;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.pojava.datetime.DateTime;

@XmlType
public class MovieFile implements Comparable<MovieFile> {

    private String filename = Movie.UNKNOWN;
    private String archiveName = null;
    private int season = -1;    // The season associated with the movie file
    private int firstPart = 1;  // #1, #2, CD1, CD2, etc.
    private int lastPart = 1;
    private boolean newFile = Boolean.TRUE;     // is new file or already exists in XML data
    private boolean subtitlesExchange = false;  // Are the subtitles for this file already downloaded/uploaded to the server
    private Map<String, String> playLink = new HashMap<String, String>();
    private final Map<Integer, String> titles = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> plots = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> videoImageURL = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> videoImageFilename = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> airsAfterSeason = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> airsBeforeSeason = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> airsBeforeEpisode = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> firstAired = new LinkedHashMap<Integer, String>();
    private final Map<Integer, String> ratings = new LinkedHashMap<Integer, String>();
    private final Map<OverrideFlag, String> overrideSources = new EnumMap<OverrideFlag, String>(OverrideFlag.class);
    private File file;
    private MovieFileNameDTO info;
    private final List<Attachment> attachments = new ArrayList<Attachment>();
    private boolean attachmentsScanned = false;
    private boolean watched = false;
    private long watchedDate = 0;
    private final boolean playFullBluRayDisk = PropertiesUtil.getBooleanProperty("mjb.playFullBluRayDisk", Boolean.TRUE);
    private final boolean includeEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", Boolean.FALSE);
    private final boolean includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", Boolean.FALSE);
    private static final Boolean DIR_HASH = PropertiesUtil.getBooleanProperty("mjb.dirHash", Boolean.FALSE);
    private final String playLinkVOD = PropertiesUtil.getProperty("filename.scanner.types.suffix.VOD", "");
    private final String playLinkZCD = PropertiesUtil.getProperty("filename.scanner.types.suffix.ZCD", "2");

    // checks
    private static final int MAX_LENGTH_EPISODE_PLOT = PropertiesUtil.getReplacedIntProperty("movie.episodeplot.maxLength", "plugin.plot.maxlength", 500);

    private static final Map<String, Pattern> TYPE_SUFFIX_MAP = new HashMap<String, Pattern>() {
        private static final long serialVersionUID = 1247815606593469672L;

        {
            String scannerTypes = PropertiesUtil.getProperty("filename.scanner.types", "ZCD,VOD");

            Map<String, String> scannerTypeDefaults = new HashMap<String, String>() {
                private static final long serialVersionUID = -6480597100092105116L;

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

    public void setTitle(int part, String title, String source) {
        if (StringTools.isNotValidString(title)) {
            // do not overwrite existing title
            if (StringTools.isNotValidString(titles.get(part))) {
                titles.put(part, Movie.UNKNOWN);
            }
        } else {
            titles.put(part, title);
            setOverrideSource(OverrideFlag.EPISODE_TITLE, source);
        }
    }

    public void setTitle(String title) {
        setTitle(firstPart, title, Movie.UNKNOWN);
    }

    public void setTitle(String title, String source) {
        if (title != null) {
            setTitle(firstPart, title, source);
        }
    }

    public String getPlot(int part) {
        String plot = plots.get(part);
        return plot != null ? plot : Movie.UNKNOWN;
    }

    public void setPlot(int part, String plot, String source) {
        this.setPlot(part, plot, source, Boolean.FALSE);
    }

    public void setPlot(int part, String plot, String source, boolean parsedFromXml) {
        if (StringTools.isNotValidString(plot)) {
            // do not overwrite valid plot
            if (StringUtils.isBlank(plots.get(part))) {
                plots.put(part, Movie.UNKNOWN);
            }
        } else if (parsedFromXml) {
            // set plot when parsed from XML
            plots.put(part, StringTools.replaceQuotes(plot));
            setOverrideSource(OverrideFlag.EPISODE_PLOT, source);
        } else if (includeEpisodePlots) {
            // only add if parameter is set
            plots.put(part, StringTools.trimToLength(StringTools.replaceQuotes(plot), MAX_LENGTH_EPISODE_PLOT));
            setOverrideSource(OverrideFlag.EPISODE_PLOT, source);
        }
    }

    public String getRating(int part) {
        String rating = ratings.get(part);
        return rating != null ? rating : Movie.UNKNOWN;
    }

    public void setRating(int part, String rating, String source) {
        if (StringTools.isNotValidString(rating)) {
            // do not overwrite valid rating
            if (StringUtils.isBlank(ratings.get(part))) {
                ratings.put(part, Movie.UNKNOWN);
            }
        } else if (includeEpisodeRating) {
            ratings.put(part, rating);
            setOverrideSource(OverrideFlag.EPISODE_RATING, source);
        }
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
        if (StringUtils.isBlank(videoImageURL)) {
            // Use UNKNOWN as the image URL
            this.videoImageURL.put(part, Movie.UNKNOWN);
        } else {
            this.videoImageURL.put(part, videoImageURL);
        }

        // Clear the videoImageFilename associated with this part
        this.videoImageFilename.put(part, Movie.UNKNOWN);
    }

    public void setVideoImageFilename(int part, String videoImageFilename) {
        if (StringUtils.isBlank(videoImageFilename)) {
            this.videoImageFilename.put(part, Movie.UNKNOWN);
        } else {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.videoImageFilename.put(part, FileTools.createDirHash(videoImageFilename));
            } else {
                this.videoImageFilename.put(part, videoImageFilename);
            }
        }
    }

    public String getOverrideSource(OverrideFlag overrideFlag) {
        String source = overrideSources.get(overrideFlag);
        return StringUtils.isBlank(source) ? Movie.UNKNOWN : source;
    }

    public void setOverrideSource(OverrideFlag flag, String source) {
        if (StringUtils.isBlank(source)) {
            this.overrideSources.put(flag, Movie.UNKNOWN);
        } else {
            this.overrideSources.put(flag, source.toUpperCase());
        }
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
        if (titles.isEmpty()) {
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
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MovieFile anotherMovieFile) {
        return this.getFirstPart() - anotherMovieFile.getFirstPart();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + (this.filename != null ? this.filename.hashCode() : 0);
        hash = 73 * hash + this.season;
        hash = 73 * hash + this.firstPart;
        hash = 73 * hash + this.lastPart;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MovieFile other = (MovieFile) obj;
        if ((this.filename == null) ? (other.filename != null) : !this.filename.equals(other.filename)) {
            return false;
        }
        if (this.season != other.season) {
            return false;
        }
        if (this.firstPart != other.firstPart) {
            return false;
        }
        if (this.lastPart != other.lastPart) {
            return false;
        }
        return true;
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
            setTitle(dto.getPartTitle(), Movie.SOURCE_FILENAME);
        } else if (dto.getSeason() >= 0) {
            // Set the season for the movie file
            setSeason(dto.getSeason());

            // Set the episode title for each of the episodes in the file
            for (int episodeNumber : dto.getEpisodes()) {
                if (OverrideTools.checkOverwriteEpisodeTitle(this, episodeNumber, Movie.SOURCE_FILENAME)) {
                    setTitle(episodeNumber, dto.getEpisodeTitle(), Movie.SOURCE_FILENAME);
                }
            }
        } else {
            setTitle(dto.getPartTitle(), Movie.SOURCE_FILENAME);
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
            setTitle(dto.getPartTitle(), Movie.SOURCE_FILENAME);
        } else {
            firstPart = 1;
            lastPart = 1;
            setTitle(dto.getPartTitle(), Movie.SOURCE_FILENAME);
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
        if (playLink.isEmpty()) {
            playLink = calculatePlayLink();
        }
        return playLink;
    }

    public synchronized void addPlayLink(String key, String value) {
        playLink.put(key, value);
    }

    /**
     * Calculate the playlink additional information for the file
     */
    private synchronized Map<String, String> calculatePlayLink() {
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
                Matcher matcher = e.getValue().matcher(FilenameUtils.getExtension(filePlayLink.getName()));
                if (matcher.find()) {
                    //logger.finest(filename + " matched to " + e.getKey());
                    playLinkMap.put(e.getKey(), PropertiesUtil.getProperty("filename.scanner.types.suffix." + e.getKey().toUpperCase(), ""));
                }
            }
        } finally {
            // Default to VOD if there's no other type found
            if (playLinkMap.isEmpty()) {
                //logger.finest(filename + " not matched, defaulted to VOD");
                playLinkMap.put("vod", playLinkVOD);
            }
        }

        return playLinkMap;
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
        if (StringTools.isNotValidString(watchedDate)) {
            this.watchedDate = 0;
        } else {
            this.watchedDate = DateTime.parse(watchedDate).toMillis();
        }
    }

    public void setWatchedDate(long watchedDate) {
        this.watchedDate = watchedDate;
    }

    public String getWatchedDateString() {
        if (watchedDate == 0) {
            return Movie.UNKNOWN;
        } else {
            return new DateTime(watchedDate).toString(DateTimeTools.getDateFormatLongString());
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
            this.airsAfterSeason.put(part, Movie.UNKNOWN);
        } else {
            this.airsAfterSeason.put(part, season);
        }
    }

    public String getAirsBeforeSeason(int part) {
        String abSeason = airsBeforeSeason.get(part);
        return StringUtils.isNotBlank(abSeason) ? abSeason : Movie.UNKNOWN;
    }

    public void setAirsBeforeSeason(int part, String season) {
        if (StringUtils.isBlank(season)) {
            this.airsBeforeSeason.put(part, Movie.UNKNOWN);
        } else {
            this.airsBeforeSeason.put(part, season);
        }
    }

    public String getAirsBeforeEpisode(int part) {
        String abEpisode = airsBeforeEpisode.get(part);
        return StringUtils.isNotBlank(abEpisode) ? abEpisode : Movie.UNKNOWN;
    }

    public void setAirsBeforeEpisode(int part, String episode) {
        if (StringUtils.isBlank(episode)) {
            this.airsBeforeEpisode.put(part, Movie.UNKNOWN);
        } else {
            this.airsBeforeEpisode.put(part, episode);
        }
    }

    public String getFirstAired(int part) {
        String airDate = firstAired.get(part);
        return StringUtils.isNotBlank(airDate) ? airDate : Movie.UNKNOWN;
    }

    public void setFirstAired(int part, String airDate, String source) {
        if (StringTools.isNotValidString(airDate)) {
            // do not overwrite existing title
            if (StringTools.isNotValidString(firstAired.get(part))) {
                firstAired.put(part, Movie.UNKNOWN);
            }
        } else {
            firstAired.put(part, airDate);
            setOverrideSource(OverrideFlag.EPISODE_FIRST_AIRED, source);
        }
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }

    public void clearAttachments() {
        this.attachments.clear();
    }

    public boolean isAttachmentsScanned() {
        return attachmentsScanned;
    }

    public void setAttachmentsScanned(boolean attachmentsScanned) {
        this.attachmentsScanned = attachmentsScanned;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
