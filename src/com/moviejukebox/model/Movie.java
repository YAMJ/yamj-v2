/*
 *      Copyright (c) 20042009 YAMJ Members
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Movie bean
 * 
 * @author jjulien
 * @author artem.gratchev
 */
@XmlType
public class Movie implements Comparable<Movie>, Cloneable, Identifiable, IMovieBasicInformation {
    // Preparing to JAXB

    private static Logger logger = Logger.getLogger("moviejukebox");
    public static final String UNKNOWN = "UNKNOWN";
    public static final String NOTRATED = "Not Rated";
    public static final String TYPE_MOVIE = "MOVIE";
    public static final String TYPE_TVSHOW = "TVSHOW";
    public static final String TYPE_UNKNOWN = UNKNOWN;
    public static final String TYPE_VIDEO_UNKNOWN = UNKNOWN;
    public static final String TYPE_VIDEO_HD = "HD";
    public static final String TYPE_BLURAY = "BLURAY"; // Used to indicate what physical format the video is
    public static final String TYPE_DVD = "DVD"; // Used to indicate what physical format the video is
    public static final String TYPE_FILE = "FILE"; // Used to indicate what physical format the video is
    private static final ArrayList<String> sortIgnorePrefixes = new ArrayList<String>();

    private String mjbVersion = UNKNOWN;
    private String mjbRevision = UNKNOWN;
    private Date mjbGenerationDate = null;

    private String baseName;
    private boolean scrapeLibrary;
    // Movie properties
    private Map<String, String> idMap = new HashMap<String, String>(2);
    private String title = UNKNOWN;
    private String titleSort = UNKNOWN;
    private String originalTitle = UNKNOWN;
    private String year = UNKNOWN;
    private String releaseDate = UNKNOWN;
    private int rating = -1;
    private String plot = UNKNOWN;
    private String outline = UNKNOWN;
    private String quote = UNKNOWN;
    private String director = UNKNOWN;
    private String country = UNKNOWN;
    private String company = UNKNOWN;
    private String runtime = UNKNOWN;
    private String language = UNKNOWN;
    private String videoType = UNKNOWN;
    private int season = -1;
    private String subtitles = UNKNOWN;
    private Map<String, Integer> sets = new HashMap<String, Integer>();
    private SortedSet<String> genres = new TreeSet<String>();
    private List<String> cast = new ArrayList<String>();
    private List<String> writers = new ArrayList<String>();
    private String container = UNKNOWN; // AVI, MKV, TS, etc.
    private String videoCodec = UNKNOWN; // DIVX, XVID, H.264, etc.
    private String audioCodec = UNKNOWN; // MP3, AC3, DTS, etc.
    private String audioChannels = UNKNOWN; // Number of audio channels
    private String resolution = UNKNOWN; // 1280x528
    private String aspect = UNKNOWN;
    private String videoSource = UNKNOWN;
    private String videoOutput = UNKNOWN;
    private float fps = 60;
    private String certification = UNKNOWN;
    // TODO Move extra flag to movie file
    private boolean extra = false;
    private boolean trailerExchange = false;
    private String libraryPath = UNKNOWN;
    private String movieType = TYPE_MOVIE;
    private String formatType = TYPE_FILE;
    private boolean overrideTitle = false;
    private boolean overrideYear = false;


    private int top250 = -1;
    private String libraryDescription = UNKNOWN;
    private long prebuf = -1;
    // Graphics URLs & files
    private String posterURL = UNKNOWN; // The original, unaltered, poster
    private String posterSubimage = UNKNOWN; // A cut up version of the poster (not used)
    private String posterFilename = UNKNOWN; // The poster filename
    private String detailPosterFilename = UNKNOWN; // The resized poster for skins
    private String thumbnailFilename = UNKNOWN; // The thumbnail version of the poster for skins
    private String fanartURL = UNKNOWN; // The fanart URL
    private String fanartFilename = UNKNOWN; // The resized fanart file
    private String bannerURL = UNKNOWN; // The TV Show banner URL
    private String bannerFilename = UNKNOWN; // The resized banner file
    // File information
    private Date fileDate = null;
    private long fileSize = 0;
    // Navigation data
    private String first = UNKNOWN;
    private String previous = UNKNOWN;
    private String next = UNKNOWN;
    private String last = UNKNOWN;
    private Map<String, String> indexes = new HashMap<String, String>();

    // Media file properties
    Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();
    Collection<ExtraFile> extraFiles = new TreeSet<ExtraFile>();
    // Caching
    private boolean isDirty = false;
    private boolean isDirtyNFO = false;
    private boolean isDirtyPoster = false;
    private boolean isDirtyFanart = false;
    private boolean isDirtyBanner = false;
    private File file;
    private File containerFile;
    // Get the minimum widths for a high-definition movies
    private int highdef720 = Integer.parseInt(PropertiesUtil.getProperty("highdef.720.width", "1280"));
    private int highdef1080 = Integer.parseInt(PropertiesUtil.getProperty("highdef.1080.width", "1920"));

    // True if movie actually is only a entry point to movies set.
    private boolean isSetMaster = false;

    // http://stackoverflow.com/questions/343669/how-to-let-jaxb-render-boolean-as-0-and-1-not-true-and-false
    public static class BooleanYesNoAdapter extends XmlAdapter<String, Boolean> {
        @Override
        public Boolean unmarshal(String s) {
            return s == null ? null : "YES".equalsIgnoreCase(s);
        }

        @Override
        public String marshal(Boolean c) {
            return c == null ? null : c ? "YES" : "NO";
        }
    }

    public static class UrlCodecAdapter extends XmlAdapter<String, String> {
        @Override
        public String unmarshal(String s) {
            return s == null ? null : HTMLTools.decodeUrl(s);
        }

        @Override
        public String marshal(String c) {
            return c == null ? null : HTMLTools.encodeUrl(c);
        }
    }

    public void setMjbVersion(String mjbVersion) {
        if ((mjbVersion == null) || (mjbVersion.equalsIgnoreCase(UNKNOWN))) {
            this.mjbVersion = getCurrentMjbVersion();
        } else {
            this.mjbVersion = mjbVersion;
        }
    }

    @XmlElement
    public String getMjbVersion() {
        return mjbVersion;
    }

    public String getCurrentMjbVersion() {
        return MovieJukebox.class.getPackage().getSpecificationVersion();
    }

    public void setMjbRevision(String mjbRevision) {
        if (mjbRevision == null || mjbRevision.equalsIgnoreCase(UNKNOWN)) {
            this.mjbRevision = "0";
        } else {
            this.mjbRevision = mjbRevision;
        }
    }

    @XmlElement
    public String getMjbRevision() {
        // If YAMJ is self compiled then the revision information may not exist.
        if (!((mjbRevision == null) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            return mjbRevision;
        } else {
            return Movie.UNKNOWN;
        }
    }

    public String getCurrentMjbRevision() {
        String currentRevision = MovieJukebox.class.getPackage().getImplementationVersion();

        // If YAMJ is self compiled then the revision information may not exist.
        if (!((currentRevision == null) || (currentRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            return currentRevision;
        } else {
            return Movie.UNKNOWN;
        }
    }

    public void setMjbGenerationDate(String mjbGenerationDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.mjbGenerationDate = dateFormat.parse(mjbGenerationDate);
        } catch (Exception error) {
            this.mjbGenerationDate = new Date();
        }
    }

    public void setMjbGenerationDate(Date mjbGenerationDate) {
        if (mjbGenerationDate == null) {
            this.mjbGenerationDate = new Date();
        } else {
            this.mjbGenerationDate = mjbGenerationDate;
        }
    }

    @XmlElement
    public String getMjbGenerationDateString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(getMjbGenerationDate());
    }

    public Date getMjbGenerationDate() {
        if (this.mjbGenerationDate == null) {
            this.mjbGenerationDate = new Date();
        }
        return this.mjbGenerationDate;
    }

    public void addGenre(String genre) {
        if (genre != null && !extra) {
            this.isDirty = true;
            logger.finest("Genre added : " + genre);
            genres.add(genre);
        }
    }

    public void addSet(String set) {
        addSet(set, null);
    }

    public void addSet(String set, Integer order) {
        if (set != null) {
            this.isDirty = true;
            logger.finest("Set added : " + set + ", order : " + order);
            sets.put(set, order);
        }
    }

    public void addMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            this.isDirty = true;
            // always replace MovieFile
            for (MovieFile mf : this.movieFiles) {
                if (mf.compareTo(movieFile) == 0) {
                    movieFile.setFile(mf.getFile());
                    movieFile.setInfo(mf.getInfo());
                }
            }
            this.movieFiles.remove(movieFile);
            this.movieFiles.add(movieFile);
        }
    }

    public boolean hasNewMovieFiles() {
        for (MovieFile movieFile : movieFiles) {
            if (movieFile.isNewFile()) {
                return true;
            }
        }
        return false;
    }

    public void addExtraFile(ExtraFile extraFile) {
        if (extraFile != null) {
            this.isDirty = true;
            // always replace MovieFile
            this.extraFiles.remove(extraFile);
            this.extraFiles.add(extraFile);
        }
    }

    public boolean hasNewExtraFiles() {
        for (MovieFile movieFile : extraFiles) {
            if (movieFile.isNewFile()) {
                return true;
            }
        }
        return false;
    }

    public String getStrippedTitleSort() {
        String text = titleSort;
        String lowerText = text.toLowerCase();

        for (String prefix : sortIgnorePrefixes) {
            if (lowerText.startsWith(prefix.toLowerCase())) {
                text = text.substring(prefix.length());
                break;
            }
        }

        // Added season to handle properly sorting the season numbers
        if (season >= 0) {
            if (season < 10) {
                text += " 0" + season;
            } else {
                text += " " + season;
            }
        }
        // Added Year to handle movies like Ocean's Eleven (1960) and Ocean's Eleven (2001)

        return text + " (" + this.getYear() + ") " + season;
    }

    @Override
    public int compareTo(Movie anotherMovie) {
        return this.getStrippedTitleSort().compareToIgnoreCase(anotherMovie.getStrippedTitleSort());
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getBaseName()
     */
    public String getBaseName() {
        return baseName;
    }

    @XmlElementWrapper(name = "cast")
    @XmlElement(name = "actor")
    public Collection<String> getCast() {
        return cast;
    }

    @XmlElementWrapper(name = "writers")
    @XmlElement(name = "writer")
    public Collection<String> getWriters() {
        return writers;
    }

    public String getCompany() {
        return company;
    }

    public String getContainer() {
        return container;
    }

    public String getCountry() {
        return country;
    }

    public String getDirector() {
        return director;
    }

    public Collection<MovieFile> getFiles() {
        return movieFiles;
    }

    @XmlElementWrapper(name = "extras")
    @XmlElement(name = "extra")
    public Collection<ExtraFile> getExtraFiles() {
        return extraFiles;
    }

    public String getCertification() {
        return certification;
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFirst() {
        return first;
    }

    public MovieFile getFirstFile() {

        for (MovieFile part : movieFiles) {
            if (part.getFirstPart() == 1) {
                return part;
            }
        }

        Iterator<MovieFile> i = movieFiles.iterator();
        if (i.hasNext()) {
            return i.next();
        } else {
            return null;
        }
    }

    public float getFps() {
        return fps;
    }

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    public Collection<String> getGenres() {
        return genres;
    }

    public Collection<String> getSetsKeys() {
        return sets.keySet();
    }

    public Integer getSetOrder(String set) {
        return sets.get(set);
    }

    /**
     * 
     * @deprecated replaced by getId(String key). This method is kept for compatibility purpose. But you should use getId(String key, String id) instead. Ex:
     *             movie.getId(ImdbPlugin.IMDB_PLUGIN_ID) {@link getId(String key)}
     */
    @Deprecated
    public String getId() {
        return idMap.get(ImdbPlugin.IMDB_PLUGIN_ID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.Identifiable#getId(java.lang.String)
     */
    public String getId(String key) {
        String result = idMap.get(key);
        if (result != null) {
            return result;
        } else {
            return UNKNOWN;
        }
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }

    public static class MovieId {
        @XmlAttribute
        public String movieDatabase;
        @XmlValue
        public String value;
    }

    @XmlElement(name = "id")
    public List<MovieId> getMovieIds() {
        List<MovieId> list = new ArrayList<MovieId>();
        for (Entry<String, String> e : idMap.entrySet()) {
            MovieId id = new MovieId();
            id.movieDatabase = e.getKey();
            id.value = e.getValue();
            list.add(id);
        }
        return list;
    }

    public void setMovieIds(List<MovieId> list) {
        idMap.clear();
        for (MovieId id : list) {
            idMap.put(id.movieDatabase, id.value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getLanguage()
     */
    public String getLanguage() {
        return language;
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getLast() {
        return last;
    }

    @XmlElementWrapper(name = "files")
    @XmlElement(name = "file")
    public Collection<MovieFile> getMovieFiles() {
        return movieFiles;
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getNext() {
        return next;
    }

    public String getPlot() {
        return plot;
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getPrevious() {
        return previous;
    }

    public int getRating() {
        return rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getResolution() {
        return resolution;
    }

    // Return the width of the movie
    public int getWidth() {
        int width = 0;
        try {
            width = Integer.parseInt(getResolution().substring(0, getResolution().indexOf("x")));
        } catch (Exception error) {
            // This will catch the exception if mediainfo is not installed.
            width = 0;
        }
        return width;
    }

    public String getRuntime() {
        return runtime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getSeason()
     */
    public int getSeason() {
        return season;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getTitle()
     */
    public String getTitle() {
        return title;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getTitleSort()
     */
    public String getTitleSort() {
        return titleSort;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public String getVideoOutput() {
        return videoOutput;
    }

    public String getVideoSource() {
        return videoSource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#getYear()
     */
    public String getYear() {
        return year;
    }

    public String getSubtitles() {
        return subtitles;
    }

    @XmlTransient
    public boolean isDirty() {
        return isDirty;
    }

    @XmlTransient
    public boolean isDirtyNFO() {
        return isDirtyNFO;
    }

    @XmlTransient
    public boolean isDirtyPoster() {
        return isDirtyPoster;
    }

    @XmlTransient
    public boolean isDirtyFanart() {
        return isDirtyFanart;
    }

    @XmlTransient
    public boolean isDirtyBanner() {
        return isDirtyBanner;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#isTVShow()
     */
    @XmlAttribute(name = "isTV")
    public boolean isTVShow() {
        // return (season != -1);
        return (this.movieType.equals(TYPE_TVSHOW) || this.season != -1);
    }

    @XmlTransient
    public boolean isBluray() {
        return this.formatType.equals(TYPE_BLURAY);
    }

    @XmlTransient
    public boolean isDVD() {
        return this.formatType.equals(TYPE_DVD);
    }

    @XmlTransient
    public boolean isFile() {
        return this.formatType.equals(TYPE_FILE);
    }

    @XmlTransient
    public boolean isHD() {
        // Depreciated this check in favour of the width check
        // return this.videoType.equals(TYPE_VIDEO_HD) || videoOutput.indexOf("720") != -1 || videoOutput.indexOf("1080") != -1;
        return (getWidth() >= highdef720);
    }

    @XmlTransient
    public boolean isHD1080() {
        return (getWidth() >= highdef1080);
    }

    public void setAudioCodec(String audioCodec) {
        if (audioCodec == null) {
            audioCodec = UNKNOWN;
        }
        if (!audioCodec.equalsIgnoreCase(this.audioCodec)) {
            this.isDirty = true;
            this.audioCodec = audioCodec;
        }
    }

    public void setBaseName(String filename) {
        if (filename == null) {
            filename = UNKNOWN;
        }
        if (!filename.equalsIgnoreCase(baseName)) {
            this.isDirty = true;
            this.baseName = filename;
        }
    }

    public void addActor(String actor) {
        if (actor != null) {
            this.isDirty = true;
            cast.add(actor);
        }
    }

    public void setCast(Collection<String> cast) {
        this.isDirty = true;
        this.cast = new ArrayList<String>(cast);
    }

    public void addWriter(String writer) {
        if (writer != null) {
            this.isDirty = true;
            writers.add(writer);
        }
    }

    public void setWriters(Collection<String> writers) {
        this.isDirty = true;
        this.writers = new ArrayList<String>(writers);
    }

    public void setCompany(String company) {
        if (company == null) {
            company = UNKNOWN;
        }
        if (!company.equalsIgnoreCase(this.company)) {
            this.isDirty = true;
            this.company = company;
        }
    }

    public void setContainer(String container) {
        if (container == null) {
            container = UNKNOWN;
        }
        if (!container.equalsIgnoreCase(this.container)) {
            this.isDirty = true;
            this.container = container;
        }
    }

    public void setCountry(String country) {
        if (country == null) {
            country = UNKNOWN;
        }
        if (!country.equalsIgnoreCase(this.country)) {
            this.isDirty = true;
            this.country = country;
        }
    }

    public void setDirector(String director) {
        if (director == null) {
            director = UNKNOWN;
        }

        if (!director.equalsIgnoreCase(this.director)) {
            this.isDirty = true;
            this.director = director;
        }
        this.director = director;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public void setDirtyNFO(boolean isDirtyNFO) {
        this.isDirtyNFO = isDirtyNFO;
    }

    public void setDirtyPoster(boolean isDirtyPoster) {
        // Set when the poster URL is scanned as part of the NFO scanning routine.
        // Will be used to check if the poster has changed or the URL changed to force a re-download of the poster.
        this.isDirtyPoster = isDirtyPoster;
    }

    public void setDirtyFanart(boolean isDirtyFanart) {
        // Used to check if the fanart has changed or the URL changed to force a re-download of the poster.
        this.isDirtyFanart = isDirtyFanart;
    }

    public void setDirtyBanner(boolean isDirtyBanner) {
        // Used to check if the banner has changed or the URL changed to force a re-download of the banner.
        this.isDirtyBanner = isDirtyBanner;
    }

    public void setFirst(String first) {
        if (first == null) {
            first = UNKNOWN;
        }
        if (!first.equalsIgnoreCase(this.first)) {
            this.isDirty = true;
            this.first = first;
        }
    }

    public void setFps(float fps) {
        if (fps != this.fps) {
            this.isDirty = true;
            this.fps = fps;
        }
    }

    public void setGenres(Collection<String> genres) {
        if (!extra) {
            this.isDirty = true;
            this.genres = new TreeSet<String>(genres);
        }
    }

    public void setSets(Map<String, Integer> sets) {
        this.isDirty = true;
        this.sets = sets;
    }

    /**
     * 
     * @deprecated replaced by setId(String key, String id). This method is kept for compatibility purpose. But you should use setId(String key, String id)
     *             instead. Ex: movie.setId(ImdbPlugin.IMDB_PLUGIN_ID,"tt12345") {@link setId(String key, String id)}
     */
    @Deprecated
    @XmlTransient
    public void setId(String id) {
        if (id != null) {
            setId(ImdbPlugin.IMDB_PLUGIN_ID, id);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.Identifiable#setId(java.lang.String, java.lang.String)
     */
    public void setId(String key, String id) {
        if (key != null && id != null && !id.equalsIgnoreCase(this.getId(key))) {
            this.isDirty = true;
            this.idMap.put(key, id);
        }
    }

    public void setLanguage(String language) {
        if (language == null) {
            language = UNKNOWN;
        }
        if (!language.equalsIgnoreCase(this.language)) {
            this.isDirty = true;
            this.language = language;
        }
    }

    public void setCertification(String certification) {
        if (certification == null) {
            certification = NOTRATED;
        }
        this.certification = certification;
        this.isDirty = true;
    }

    public void setLast(String last) {
        if (last == null) {
            last = UNKNOWN;
        }
        if (!last.equalsIgnoreCase(this.last)) {
            this.isDirty = true;
            this.last = last;
        }
    }

    public void setMovieFiles(Collection<MovieFile> movieFiles) {
        this.isDirty = true;
        this.movieFiles = movieFiles;
    }

    public void setExtraFiles(Collection<ExtraFile> extraFiles) {
        this.isDirty = true;
        this.extraFiles = extraFiles;
    }

    public void setNext(String next) {
        if (next == null) {
            next = UNKNOWN;
        }
        if (!next.equalsIgnoreCase(this.next)) {
            this.isDirty = true;
            this.next = next;
        }
    }

    public void setPlot(String plot) {
        if (plot == null) {
            plot = UNKNOWN;
        }
        if (!plot.equalsIgnoreCase(this.plot)) {
            this.isDirty = true;
            plot = plot.replaceAll("\"", "'");
            this.plot = plot;
        }
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        if (outline == null) {
            outline = UNKNOWN;
        }
        if (!outline.equalsIgnoreCase(this.outline)) {
            this.isDirty = true;
            outline = outline.replaceAll("\"", "'");
            this.outline = outline;
        }
    }

    public void setPrevious(String previous) {
        if (previous == null) {
            previous = UNKNOWN;
        }
        if (!previous.equalsIgnoreCase(this.previous)) {
            this.isDirty = true;
            this.previous = previous;
        }
    }

    public void setRating(int rating) {
        if (rating != this.rating) {
            this.isDirty = true;
            this.rating = rating;
        }
    }

    public void setReleaseDate(String releaseDate) {
        if (releaseDate == null) {
            releaseDate = UNKNOWN;
        }
        if (!releaseDate.equalsIgnoreCase(this.releaseDate)) {
            this.isDirty = true;
            this.releaseDate = releaseDate;
        }
    }

    public void setResolution(String resolution) {
        if (resolution == null) {
            resolution = UNKNOWN;
        }
        if (!resolution.equalsIgnoreCase(this.resolution)) {
            this.isDirty = true;
            this.resolution = resolution;
        }
    }

    public void setRuntime(String runtime) {
        if (runtime == null) {
            runtime = UNKNOWN;
        }
        if ((runtime != null) && !runtime.equalsIgnoreCase(this.runtime)) {
            this.isDirty = true;
            this.runtime = runtime;
        }
    }

    public void setSeason(int season) {
        if (season != this.season) {
            if (season >= 0) {
                this.setMovieType(Movie.TYPE_TVSHOW);
            }
            this.isDirty = true;
            this.season = season;
        }
    }

    public void setSubtitles(String subtitles) {
        if (subtitles == null) {
            subtitles = UNKNOWN;
        }
        if (!subtitles.equals(this.subtitles)) {
            this.isDirty = true;
            this.subtitles = subtitles;
        }
    }

    public void setTitle(String name) {
        if (name == null) {
            name = UNKNOWN;
        }
        if (!name.equals(this.title)) {
            this.isDirty = true;
            this.title = name;

            setTitleSort(name);

            if (originalTitle.equals(UNKNOWN))
                setOriginalTitle(name);
        }
    }

    public void setTitleSort(String text) {
        if (text == null) {
            text = UNKNOWN;
        }
        if (!text.equals(this.titleSort)) {
            int idx = 0;
            while (idx < text.length() && !Character.isLetterOrDigit(text.charAt(idx))) {
                idx++;
            }

            this.titleSort = text.substring(idx);
            this.isDirty = true;
        }
    }

    public void setOriginalTitle(String name) {
        if (name == null) {
            name = UNKNOWN;
        }
        if (!name.equals(this.originalTitle)) {
            this.isDirty = true;
            this.originalTitle = name;
        }
    }

    public void setVideoCodec(String videoCodec) {
        if (videoCodec == null) {
            videoCodec = UNKNOWN;
        }
        if (!videoCodec.equalsIgnoreCase(this.videoCodec)) {
            this.isDirty = true;
            this.videoCodec = videoCodec;
        }
    }

    public void setVideoOutput(String videoOutput) {
        if (videoOutput == null) {
            videoOutput = UNKNOWN;
        }
        if (!videoOutput.equalsIgnoreCase(this.videoOutput)) {
            this.isDirty = true;
            this.videoOutput = videoOutput;
        }
    }

    public void setVideoSource(String videoSource) {
        if (videoSource == null) {
            videoSource = UNKNOWN;
        }
        if (!videoSource.equalsIgnoreCase(this.videoSource)) {
            this.isDirty = true;
            this.videoSource = videoSource;
        }
    }

    public void setYear(String year) {
        if (year == null) {
            year = UNKNOWN;
        }
        if (!year.equalsIgnoreCase(this.year)) {
            this.isDirty = true;
            this.year = year;
        }
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        if (quote == null) {
            quote = UNKNOWN;
        }
        if (!quote.equalsIgnoreCase(this.quote)) {
            this.isDirty = true;
            this.quote = quote;
        }
    }

    @XmlTransient
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @XmlTransient
    public File getContainerFile() {
        return containerFile;
    }

    public void setContainerFile(File containerFile) {
        this.containerFile = containerFile;
    }

    public long getLastModifiedTimestamp() {
        long tmstmp = 0;
        if (getMovieFiles().size() == 1) {
            tmstmp = this.file.lastModified();
        } else {
            for (MovieFile mf : getMovieFiles()) {
                try {
                    if (mf.getFile() != null && mf.getFile().lastModified() > tmstmp) {
                        tmstmp = mf.getFile().lastModified();
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return tmstmp;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("[Movie ");
        for (Map.Entry<String, String> e : idMap.entrySet()) {
            sb.append("[id_").append(e.getKey()).append("=").append(e.getValue()).append("]");
        }
        sb.append("[title=").append(title).append("]");
        sb.append("[titleSort=").append(titleSort).append("]");
        sb.append("[year=").append(year).append("]");
        sb.append("[releaseDate=").append(releaseDate).append("]");
        sb.append("[rating=").append(rating).append("]");
        sb.append("[top250=").append(top250).append("]");
        sb.append("[posterURL=").append(posterURL).append("]");
        sb.append("[bannerURL=").append(bannerURL).append("]");
        sb.append("[fanartURL=").append(fanartURL).append("]");
        sb.append("[plot=").append(plot).append("]");
        sb.append("[outline=").append(outline).append("]");
        sb.append("[director=").append(director).append("]");
        sb.append("[country=").append(country).append("]");
        sb.append("[company=").append(company).append("]");
        sb.append("[runtime=").append(runtime).append("]");
        sb.append("[season=").append(season).append("]");
        sb.append("[language=").append(language).append("]");
        sb.append("[subtitles=").append(subtitles).append("]");
        sb.append("[container=").append(container).append("]"); // AVI, MKV, TS, etc.
        sb.append("[videoCodec=").append(videoCodec).append("]"); // DIVX, XVID, H.264, etc.
        sb.append("[audioCodec=").append(audioCodec).append("]"); // MP3, AC3, DTS, etc.
        sb.append("[audioChannels=").append(audioChannels).append("]"); // Number of audio channels
        sb.append("[resolution=").append(resolution).append("]"); // 1280x528
        sb.append("[videoSource=").append(videoSource).append("]");
        sb.append("[videoOutput=").append(videoOutput).append("]");
        sb.append("[fps=").append(fps).append("]");
        sb.append("[certification=").append(certification).append("]");
        sb.append("[cast=").append(cast).append("]");
        sb.append("[writers=").append(writers).append("]");
        sb.append("[genres=").append(genres).append("]");
        sb.append("[libraryDescription=").append(libraryDescription).append("]");
        sb.append("[prebuf=").append(prebuf).append("]");
        sb.append("]");
        return sb.toString();
    }

    /**
     * Sets the "extra" flag to mark this file as an extra. Will trigger the "dirty" setting too
     * 
     * @param extra
     *            Boolean flag, true=extra file, false=normal file
     */
    public void setExtra(boolean extra) {
        this.isDirty = true;
        this.extra = extra;
        if (extra) {
            genres.clear();
        }
    }

    public void setTrailerExchange(boolean trailerExchange) {
        this.isDirty = true;
        this.trailerExchange = trailerExchange;
    }

    /**
     * @return Boolean flag indicating if this file is an extra
     */
    @XmlAttribute(name = "isExtra")
    public boolean isExtra() {
        return extra;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#isTrailerExchange()
     */
    @XmlJavaTypeAdapter(BooleanYesNoAdapter.class)
    public Boolean isTrailerExchange() {
        return trailerExchange;
    }

    public void setMovieType(String movieType) {
        if (movieType == null) {
            movieType = TYPE_UNKNOWN;
        }
        if (!this.movieType.equals(movieType)) {
            this.isDirty = true;
            this.movieType = movieType;
        }
    }

    public String getMovieType() {
        return this.movieType;
    }

    public void setFormatType(String formatType) {
        if (formatType == null) {
            formatType = TYPE_FILE;
        }
        if (!this.formatType.equals(formatType)) {
            this.isDirty = true;
            this.formatType = formatType;
        }
    }

    public String getFormatType() {
        return this.formatType;
    }

    public void setVideoType(String videoType) {
        if (videoType == null) {
            videoType = UNKNOWN;
        }
        if (!this.videoType.equals(videoType)) {
            this.isDirty = true;
            this.videoType = videoType;
        }
    }

    public String getVideoType() {
        return this.videoType;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public String getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(String audioChannels) {
        this.audioChannels = audioChannels;
    }

    public boolean isOverrideTitle() {
        return overrideTitle;
    }

    public void setOverrideTitle(boolean overrideTitle) {
        this.overrideTitle = overrideTitle;
    }

    public int getTop250() {
        return top250;
    }

    public void setTop250(int top250) {
        if (top250 != this.top250) {
            this.isDirty = true;
            this.top250 = top250;
        }
    }

    public String getLibraryDescription() {
        return libraryDescription;
    }

    public void setLibraryDescription(String libraryDescription) {
        if (libraryDescription == null || libraryDescription.isEmpty()) {
            libraryDescription = UNKNOWN;
        }
        if (!libraryDescription.equals(this.libraryDescription)) {
            this.libraryDescription = libraryDescription;
            this.isDirty = true;
        }
    }

    public long getPrebuf() {
        return prebuf;
    }

    public void setPrebuf(long prebuf) {
        this.prebuf = prebuf;
    }

    @XmlTransient
    public boolean isScrapeLibrary() {
        return scrapeLibrary;
    }

    public void setScrapeLibrary(boolean scrapeLibrary) {
        this.scrapeLibrary = scrapeLibrary;
    }

    public static ArrayList<String> getSortIgnorePrefixes() {
        return sortIgnorePrefixes;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    public void mergeFileNameDTO(MovieFileNameDTO dto) {
        setTitle(dto.getTitle());
        setExtra(dto.isExtra());
        setAudioCodec(dto.getAudioCodec());
        setVideoCodec(dto.getVideoCodec());
        setVideoSource(dto.getVideoSource());
        setContainer(dto.getContainer());
        setFps(dto.getFps() > 0 ? dto.getFps() : 60);
        setSeason(dto.getSeason());
        for (MovieFileNameDTO.SetDTO set : dto.getSets()) {
            addSet(set.getTitle(), set.getIndex() >= 0 ? set.getIndex() : null);
        }
        setYear(dto.getYear() > 0 ? "" + dto.getYear() : null);
        setLanguage(dto.getLanguages().size() > 0 ? dto.getLanguages().get(0) : null);

        if (dto.getHdResolution() != null) {
            setVideoType(TYPE_VIDEO_HD);

            // Check if the videoOutput is UNKNOWN and clear it if it is
            if (videoOutput.equals(Movie.UNKNOWN)) {
                videoOutput = "";
            }

            switch (dto.getFps()) {
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
                if (!videoOutput.equals(""))
                    videoOutput += " ";
                videoOutput += "50Hz";
                break;
            case 59:
                videoOutput = "1080p 59.94Hz";
                break;
            case 60:
                if (!videoOutput.equals(""))
                    videoOutput += " ";
                videoOutput += "60Hz";
                break;
            default:
                if (videoOutput.equals("")) {
                    videoOutput = Movie.UNKNOWN;
                } else {
                    videoOutput += " 60Hz";
                }
            }
        } else {
            switch (dto.getFps()) {
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
                videoOutput = Movie.UNKNOWN;
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.moviejukebox.model.IMovieBasicInformation#isSetMaster()
     */
    @XmlAttribute(name = "isSet")
    public boolean isSetMaster() {
        return isSetMaster;
    }

    public void setSetMaster(boolean isSetMaster) {
        this.isSetMaster = isSetMaster;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;

        if (fileDate.after(this.fileDate)) {
            this.fileDate = fileDate;
        }
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = this.fileSize + fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileSizeString() {
        String returnSize = UNKNOWN;
        long calcFileSize = this.fileSize;

        if (calcFileSize > 1024) {
            calcFileSize = calcFileSize / 1024;
            if (calcFileSize > 1024) {
                calcFileSize = calcFileSize / 1024;
                if (calcFileSize > 1024) {
                    calcFileSize = calcFileSize / 1024;
                    if (calcFileSize > 1024) {
                        calcFileSize = calcFileSize / 1024;
                        if (calcFileSize > 1024) {
                            calcFileSize = calcFileSize / 1024;
                        } else {
                            returnSize = calcFileSize + "TB";
                        }
                    } else {
                        returnSize = calcFileSize + "GB";
                    }
                } else {
                    returnSize = calcFileSize + "MB";
                }
            } else {
                returnSize = calcFileSize + "KB";
            }
        } else {
            returnSize = calcFileSize + "Bytes";
        }

        return returnSize;
    }

    public void setAspectRatio(String aspect) {
        // Format the aspect slightly and change "16/9" to "16:9"
        aspect.replaceAll("/", ":");
        if (!aspect.contains(":")) {
            aspect += ":1";
        }

        this.aspect = aspect;
    }

    public String getAspectRatio() {
        return aspect;
    }

    // ***** All the graphics methods go here *****

    // ***** Posters
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getPosterURL() {
        return posterURL;
    }

    /**
     * Should be called only from PosterScanner. Avoid calling this inside MoviePlugin
     * Also called from MovieNFOScanner
     * 
     * @param url
     */
    public void setPosterURL(String url) {
        if (url == null) {
            url = UNKNOWN;
        }
        if (!url.equalsIgnoreCase(this.posterURL)) {
            this.isDirty = true;
            this.posterURL = url;
        }
    }

    @XmlElement(name = "posterFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getPosterFilename() {
        return posterFilename;
    }

    public void setPosterFilename(String posterFilename) {
        if (posterFilename == null) {
            posterFilename = UNKNOWN;
        }
        this.posterFilename = posterFilename;
    }

    @XmlElement(name = "detailPosterFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getDetailPosterFilename() {
        return detailPosterFilename;
    }

    public void setDetailPosterFilename(String detailPosterFilename) {
        if (detailPosterFilename == null) {
            detailPosterFilename = UNKNOWN;
        }
        this.detailPosterFilename = detailPosterFilename;
    }

    // ***** Poster Subimage
    public String getPosterSubimage() {
        return posterSubimage;
    }

    public void setPosterSubimage(String subimage) {
        if (subimage == null) {
            subimage = UNKNOWN;
        }
        if (!subimage.equalsIgnoreCase(this.posterSubimage)) {
            this.isDirty = true;
            this.posterSubimage = subimage;
        }
    }

    // ***** Thumbnails
    @XmlElement(name = "thumbnail")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getThumbnailFilename() {
        return this.thumbnailFilename;
    }

    public void setThumbnailFilename(String thumbnailFilename) {
        if (thumbnailFilename == null) {
            thumbnailFilename = UNKNOWN;
        }
        this.thumbnailFilename = thumbnailFilename;
    }

    // ***** Fanart
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartURL() {
        return fanartURL;
    }

    public void setFanartURL(String fanartURL) {
        if (fanartURL == null) {
            fanartURL = UNKNOWN;
        }
        this.fanartURL = fanartURL;
    }

    @XmlElement(name = "fanartFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartFilename() {
        return fanartFilename;
    }

    public void setFanartFilename(String fanartFilename) {
        if (fanartFilename == null) {
            fanartFilename = UNKNOWN;
        }
        this.fanartFilename = fanartFilename;
    }

    // ***** Banners
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerURL() {
        return bannerURL;
    }

    public void setBannerURL(String url) {
        if (url == null) {
            url = UNKNOWN;
        }
        if (!url.equalsIgnoreCase(this.bannerURL)) {
            this.isDirty = true;
            this.bannerURL = url;
        }
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerFilename() {
        return bannerFilename;
    }

    public void setBannerFilename(String bannerFilename) {
        if (bannerFilename == null) {
            bannerFilename = UNKNOWN;
        }
        this.bannerFilename = bannerFilename;
    }

    // ***** END of graphics *****

    public Map<String, String> getIndexes() {
        return indexes;
    }

    public void addIndex(String key, String index) {
        if (key != null && index != null) {
            indexes.put(key, index);
        }
    }

    public void setIndexes(Map<String, String> indexes) {
        this.indexes = new HashMap<String, String>(indexes);
    }
    
    public boolean isOverrideYear() {
        return overrideYear;
    }

    public void setOverrideYear(boolean overrideYear) {
        this.overrideYear = overrideYear;
    }
}
