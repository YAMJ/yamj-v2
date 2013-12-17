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
package com.moviejukebox.model;

import com.moviejukebox.model.enumerations.CodecSource;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.tools.BooleanYesNoAdapter;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.UrlCodecAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;

/**
 * Movie bean
 *
 * @author jjulien
 * @author artem.gratchev
 */
@XmlType
public class Movie implements Comparable<Movie>, Identifiable, IMovieBasicInformation {
    /*
     * Static & Final variables that are used for control and don't relate
     * specifically to the Movie object
     */

    private static final Logger LOG = Logger.getLogger(Movie.class);
    public static final String UNKNOWN = "UNKNOWN";
    public static final String SOURCE_FILENAME = "filename";
    public static final String NOTRATED = "Not Rated";
    public static final String REMOVE = "Remove"; // All Movie objects with this type will be removed from library before index generation
    public static final String TYPE_MOVIE = "MOVIE";
    public static final String TYPE_TVSHOW = "TVSHOW";
    public static final String TYPE_UNKNOWN = UNKNOWN;
    public static final String TYPE_VIDEO_UNKNOWN = UNKNOWN;
    public static final String TYPE_VIDEO_HD = "HD";
    public static final String TYPE_BLURAY = "BLURAY"; // Used to indicate what physical format the video is
    public static final String TYPE_DVD = "DVD"; // Used to indicate what physical format the video is
    public static final String TYPE_FILE = "FILE"; // Used to indicate what physical format the video is
    public static final String TYPE_PERSON = "PERSON";
    public static final String SPACE_SLASH_SPACE = " / ";
    private String mjbVersion = UNKNOWN;
    private String mjbRevision = UNKNOWN;
    private DateTime mjbGenerationDate = null;
    /*
     * --------------------------------------------------------------------------------
     * Properties that control the object
     */
    private static final List<String> SORT_IGNORE_PREFIXES = new ArrayList<String>();
    private final int highdef720 = PropertiesUtil.getIntProperty("highdef.720.width", 1280);    // Get the minimum width for a high-definition movies
    private final int highdef1080 = PropertiesUtil.getIntProperty("highdef.1080.width", 1920);  // Get the minimum width for a high-definition movies
    private final String[] ratingSource = PropertiesUtil.getProperty("mjb.rating.source", "average").split(",");
    private final String tmpRatingIgnore = PropertiesUtil.getProperty("mjb.rating.ignore", "");
    private final List<String> ratingIgnore = StringTools.isValidString(tmpRatingIgnore) ? Arrays.asList(tmpRatingIgnore.split(",")) : new ArrayList<String>();
    private static final Set<String> GENRE_SKIP_LIST = new HashSet<String>();   // List of genres to ignore
    private static final TitleSortType TITLE_SORT_TYPE = TitleSortType.fromString(PropertiesUtil.getProperty("mjb.sortTitle", "title"));
    // TODO: This will be removed in the future, once hashing has been completed
    private static final Boolean DIR_HASH = PropertiesUtil.getBooleanProperty("mjb.dirHash", Boolean.FALSE);
    // checks
    private static final int MAX_COUNT_DIRECTOR = PropertiesUtil.getReplacedIntProperty("movie.director.maxCount", "plugin.people.maxCount.director", 2);
    private static final int MAX_COUNT_WRITER = PropertiesUtil.getReplacedIntProperty("movie.writer.maxCount", "plugin.people.maxCount.writer", 3);
    private static final int MAX_COUNT_ACTOR = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", 10);
    private static final int MAX_LENGTH_PLOT = PropertiesUtil.getReplacedIntProperty("movie.plot.maxLength", "plugin.plot.maxlength", 500);
    private static final int MAX_LENGTH_OUTLINE = PropertiesUtil.getReplacedIntProperty("movie.outline.maxLength", "plugin.outline.maxlength", 300);
    /*
     * --------------------------------------------------------------------------------
     * Properties related to the Movie object itself
     */
    private String baseName;        // Safe name for generated files
    private String baseFilename;    // Base name for finding posters, nfos, banners, etc.
    private Map<String, String> idMap = new HashMap<String, String>(2);
    private String title = UNKNOWN;
    private String titleSort = UNKNOWN;
    private String strippedTitleSort = UNKNOWN; // Not saved, used to speedup the sort
    private String originalTitle = UNKNOWN;
    private String year = UNKNOWN;
    private String releaseDate = UNKNOWN;
    private Map<String, Integer> ratings = new HashMap<String, Integer>();
    private String plot = UNKNOWN;
    private String outline = UNKNOWN;
    private String quote = UNKNOWN;
    private String tagline = UNKNOWN;
    private String country = UNKNOWN;
    private String company = UNKNOWN;
    private String runtime = UNKNOWN;
    private String language = UNKNOWN;
    private String videoType = UNKNOWN;
    private String subtitles = UNKNOWN;
    private Set<String> directors = new LinkedHashSet<String>();
    private Map<String, Integer> sets = new HashMap<String, Integer>();
    private Collection<String> genres = new TreeSet<String>();
    private Set<String> cast = new LinkedHashSet<String>();
    private Set<String> writers = new LinkedHashSet<String>();
    private String container = UNKNOWN; // AVI, MKV, TS, etc.
    private Set<Codec> codecs = new LinkedHashSet<Codec>();
    private String resolution = UNKNOWN; // 1280x528
    private String aspect = UNKNOWN;
    private String videoSource = UNKNOWN;
    private String videoOutput = UNKNOWN;
    private float fps = 60;
    private String certification = UNKNOWN;
    private String showStatus = UNKNOWN;    // For TV shows a status such as "Continuing" or "Ended"
    private boolean scrapeLibrary;
    private boolean extra = Boolean.FALSE;  // TODO Move extra flag to movie file
    private boolean trailerExchange = Boolean.FALSE;    // Trailers
    private long trailerLastScan = 0;           // Trailers
    private Collection<AwardEvent> awards = new ArrayList<AwardEvent>();    // Issue 1901: Awards
    private Collection<Filmography> people = new ArrayList<Filmography>();  // Issue 1897: Cast enhancement
    private String budget = UNKNOWN;                                        // Issue 2012: Financial information about movie
    private Map<String, String> openweek = new HashMap<String, String>();
    private Map<String, String> gross = new HashMap<String, String>();
    private Map<OverrideFlag, String> overrideSources = new EnumMap<OverrideFlag, String>(OverrideFlag.class);
    private List<String> didYouKnow = new ArrayList<String>();        // Issue 2013: Add trivia
    private String libraryPath = UNKNOWN;
    private String movieType = TYPE_MOVIE;
    private String formatType = TYPE_FILE;
    private int top250 = -1;
    private String libraryDescription = UNKNOWN;
    private long prebuf = -1;
    // Graphics URLs & files
    private String posterURL = UNKNOWN; // The original, unaltered, poster
    private String posterFilename = UNKNOWN; // The poster filename
    private String detailPosterFilename = UNKNOWN; // The resized poster for skins
    private String thumbnailFilename = UNKNOWN; // The thumbnail version of the poster for skins
    private List<String> footerFilename = new ArrayList<String>(); // The footer image for skins
    private String fanartURL = UNKNOWN; // The fanart URL
    private String fanartFilename = UNKNOWN; // The resized fanart file
    private String bannerURL = UNKNOWN; // The TV Show banner URL
    private String bannerFilename = UNKNOWN; // The resized banner file
    private String wideBannerFilename = UNKNOWN; // The original banner file
    private String clearArtURL = UNKNOWN;
    private String clearArtFilename = UNKNOWN;
    private String clearLogoURL = UNKNOWN;
    private String clearLogoFilename = UNKNOWN;
    private String seasonThumbURL = UNKNOWN;
    private String seasonThumbFilename = UNKNOWN;
    private String tvThumbURL = UNKNOWN;
    private String tvThumbFilename = UNKNOWN;
    private String movieDiscURL = UNKNOWN;
    private String movieDiscFilename = UNKNOWN;
    // File information
    private Date fileDate = null;
    private long fileSize = 0;
    private long lastModifiedTimeStamp = Long.valueOf(0);      // cache value
    private boolean watchedFile = Boolean.FALSE;    // Watched / Unwatched - Set from the .watched files
    private boolean watchedNFO = Boolean.FALSE;     // Watched / Unwatched - Set from the NFO file
    // Navigation data
    private String first = UNKNOWN;
    private String previous = UNKNOWN;
    private String next = UNKNOWN;
    private String last = UNKNOWN;
    private Map<String, String> indexes = new HashMap<String, String>();
    // Media file properties
    private Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();
    private Collection<ExtraFile> extraFiles = new TreeSet<ExtraFile>();
    private Set<DirtyFlag> dirtyFlags = EnumSet.noneOf(DirtyFlag.class);    // List of the dirty flags associated with the Movie
    private File file;
    private File containerFile;
    // Set information
    private boolean setMaster = Boolean.FALSE;    // True if movie actually is only a entry point to movies set.
    private int setSize = 0;                        // Amount of movies in set
    private MovieDatabasePlugin movieScanner = null;
    private boolean skipped = Boolean.FALSE;

    /*
     * --------------------------------------------------------------------------------
     * End of properties
     * --------------------------------------------------------------------------------
     */
    static {
        if (GENRE_SKIP_LIST.isEmpty()) {
            StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.genre.skip", ""), ",;|");
            while (st.hasMoreTokens()) {
                GENRE_SKIP_LIST.add(st.nextToken().toLowerCase());
            }
        }
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setMjbVersion(String mjbVersion) {
        if (StringTools.isNotValidString(mjbVersion)) {
            this.mjbVersion = SystemTools.getVersion();
        } else {
            this.mjbVersion = mjbVersion;
        }
    }

    @XmlElement
    public String getMjbVersion() {
        return mjbVersion;
    }

    public void setMjbRevision(String mjbRevision) {
        if (StringTools.isNotValidString(mjbRevision)) {
            this.mjbRevision = "0000";
        } else {
            this.mjbRevision = mjbRevision;
        }
    }

    @XmlElement
    public String getMjbRevision() {
        // If YAMJ is self compiled then the revision information may not exist.
        if (!(StringUtils.isBlank(mjbRevision) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            return mjbRevision;
        } else {
            return Movie.UNKNOWN;
        }
    }

    public void setMjbGenerationDateString(String mjbGenerationDate) {
        try {
            this.mjbGenerationDate = DateTime.parse(mjbGenerationDate);
        } catch (Exception error) {
            this.mjbGenerationDate = new DateTime();
        }
    }

    public void setMjbGenerationDate(DateTime mjbGenerationDate) {
        if (mjbGenerationDate == null) {
            this.mjbGenerationDate = new DateTime();
        } else {
            this.mjbGenerationDate = mjbGenerationDate;
        }
    }

    @XmlElement
    public String getMjbGenerationDateString() {
        return getMjbGenerationDate().toString(DateTimeTools.getDateFormatLongString());
    }

    public DateTime getMjbGenerationDate() {
        if (this.mjbGenerationDate == null) {
            this.mjbGenerationDate = new DateTime();
        }
        return this.mjbGenerationDate;
    }

    public void addSet(String set) {
        addSet(set, null);
    }

    public void addSet(String set, Integer order) {
        if (StringTools.isValidString(set)) {
            setDirty(DirtyFlag.INFO);
            LOG.debug("Set added: " + set + (order == null ? ", unordered" : ", order: " + order));
            sets.put(set, order);
        }
    }

    public void addMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            setDirty(DirtyFlag.INFO);
            // Always replace MovieFile
            for (MovieFile mf : this.movieFiles) {
                if (mf.compareTo(movieFile) == 0) {
                    movieFile.setFile(mf.getFile());
                    movieFile.setInfo(mf.getInfo());
                }
            }
            //logger.debug("Movie addMovieFile: " + movieFile.getFilename());
            this.movieFiles.remove(movieFile);
            this.movieFiles.add(movieFile);
        }
    }

    public void addAward(AwardEvent award) {
        if (award != null) {
            setDirty(DirtyFlag.INFO);
            this.awards.add(award);
        }
    }

    public boolean addPerson(Filmography person) {
        boolean added = Boolean.FALSE;
        if (person != null) {
            boolean duplicate = Boolean.FALSE;
            String name = person.getName();
            String job = person.getJob();
            for (Filmography p : people) {
                if (p.getName().equals(name) && p.getJob().equals(job)) {
                    duplicate = Boolean.TRUE;
                    break;
                }
            }
            if (!duplicate) {
                setDirty(DirtyFlag.INFO);
                added = people.add(person);
            }
        }
        return added;
    }

    public void removePerson(Filmography person) {
        if (person != null) {
            people.remove(person);
        }
    }

    public boolean addPerson(String key, String name, String url, String job, String source) {
        return addPerson(key, name, url, job, Movie.UNKNOWN, Movie.UNKNOWN, source);
    }

    public boolean addPerson(String key, String name, String url, String job, String character, String doublage, String source) {
        boolean added = Boolean.FALSE;

        if (StringUtils.isNotBlank(name)
                && StringUtils.isNotBlank(key)
                && StringUtils.isNotBlank(url)
                && StringUtils.isNotBlank(job)) {

            Filmography person = new Filmography();

            if (key.indexOf(":") > -1) {
                String[] keys = key.split(":");
                person.setId(keys[0], keys[1]);
            } else {
                person.setId(key);
            }

            if (name.indexOf(":") > -1) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[0])) {
                    person.setName(names[0]);
                    person.setTitle(names[1]);
                } else if (StringTools.isValidString(names[1])) {
                    person.setName(names[1]);
                } else {
                    person.setName(name);
                }
            } else {
                person.setName(name);
            }

            person.setUrl(url);
            person.setCharacter(character);
            person.setDoublage(doublage);
            person.setJob(job);
            person.setDepartment();

            int countActor = 0;
            if (person.getDepartment().equalsIgnoreCase(Filmography.DEPT_ACTORS)) {
                for (Filmography member : people) {
                    if (member.getDepartment().equalsIgnoreCase(Filmography.DEPT_ACTORS)) {
                        countActor++;
                    }
                }
            }

            person.setOrder(countActor);
            person.setCastId(people.size());
            person.setScrapeLibrary(scrapeLibrary);
            if (StringTools.isNotValidString(source)) {
                person.setSource(UNKNOWN);
            } else {
                person.setSource(source.toUpperCase());
            }
            added = addPerson(person);
        }
        return added;
    }

    public void removeMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            setDirty(DirtyFlag.INFO);
            for (MovieFile mf : this.movieFiles) {
                if (mf.compareTo(movieFile) == 0) {
                    this.movieFiles.remove(mf);
                    break;
                }
            }

        }
    }

    public boolean hasNewMovieFiles() {
        for (MovieFile movieFile : movieFiles) {
            if (movieFile.isNewFile()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Add a new extra file to the movie
     *
     * @param extraFile
     */
    public void addExtraFile(ExtraFile extraFile) {
        addExtraFile(extraFile, Boolean.TRUE);
    }

    /**
     * Add a new extra file to the movie without marking the movie as dirty.
     *
     * @param extraFile
     * @param isNewFile Use carefully as this will not cause the movie to be marked as dirty and may not be written out
     */
    public void addExtraFile(ExtraFile extraFile, boolean isNewFile) {
        // Only add extraFile if it doesn't already exists
        if (extraFile != null && !this.extraFiles.contains(extraFile)) {
            if (isNewFile) {
                setDirty(DirtyFlag.INFO);
            }
            this.extraFiles.add(extraFile);
        }
    }

    public boolean hasNewExtraFiles() {
        for (MovieFile movieFile : extraFiles) {
            if (movieFile.isNewFile()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public String getStrippedTitleSort() {
        // If we have a strippedTitleSort, return that
        if (StringTools.isValidString(strippedTitleSort)) {
            return strippedTitleSort;
        }

        StringBuilder text = new StringBuilder(getStrippedTitle(getTitleSort()));
        int season = getSeason();

        // Added season to handle properly sorting the season numbers
        if (season >= 0) {
            if (season < 10) {
                text.append(" 0");
            } else {
                text.append(" ");
            }
            text.append(season);
        }
        // Added Year to handle movies like Ocean's Eleven (1960) and Ocean's Eleven (2001)
        text.append(" (").append(this.getYear()).append(") ");
        strippedTitleSort = text.toString().toLowerCase();
        return strippedTitleSort;
    }

    /**
     * Remove the sorting strip prefix from the title
     *
     * @param title
     * @return
     */
    private String getStrippedTitle(String title) {
        String lowerTitle = title.toLowerCase();

        for (String prefix : SORT_IGNORE_PREFIXES) {
            if (lowerTitle.startsWith(prefix.toLowerCase())) {
                return title.substring(prefix.length());
            }
        }

        return title;
    }

    @Override
    public int compareTo(Movie anotherMovie) {
        return this.getStrippedTitleSort().compareToIgnoreCase(anotherMovie.getStrippedTitleSort());
    }

    @Deprecated
    public String getAudioCodec() {
        StringBuilder sb = new StringBuilder();
        boolean firstCodec = Boolean.TRUE;
        for (Codec audioCodec : codecs) {
            if (audioCodec.getCodecType() == CodecType.AUDIO) {
                if (firstCodec) {
                    firstCodec = Boolean.FALSE;
                } else {
                    sb.append(SPACE_SLASH_SPACE);
                }

                if (StringTools.isValidString(audioCodec.getCodecIdHint())) {
                    sb.append(audioCodec.getCodecIdHint());
                } else if (StringTools.isValidString(audioCodec.getCodec())) {
                    sb.append(audioCodec.getCodec());
                } else if (StringTools.isValidString(audioCodec.getCodecFormat())) {
                    sb.append(audioCodec.getCodecFormat());
                } else if (StringTools.isValidString(audioCodec.getCodecId())) {
                    sb.append(audioCodec.getCodecId());
                } else {
                    sb.append(Movie.UNKNOWN);
                }

                if (StringTools.isValidString(audioCodec.getCodecLanguage())) {
                    sb.append(" (");
                    sb.append(audioCodec.getCodecLanguage());
                    sb.append(")");
                }
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return UNKNOWN;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.IMovieBasicInformation#getBaseName()
     */
    @Override
    public String getBaseName() {
        return baseName;
    }

    public String getBaseFilename() {
        return baseFilename;
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

    public Collection<String> getDidYouKnow() {
        return didYouKnow;
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

    public Collection<MovieFile> getFiles() {
        return movieFiles;
    }

    @XmlElementWrapper(name = "extras")
    @XmlElement(name = "extra")
    public Collection<ExtraFile> getExtraFiles() {
        return extraFiles;
    }

    @XmlElementWrapper(name = "awards")
    @XmlElement(name = "award")
    public Collection<AwardEvent> getAwards() {
        return awards;
    }

    @XmlElementWrapper(name = "people")
    @XmlElement(name = "person")
    public Collection<Filmography> getPeople() {
        return people;
    }

    public Collection<String> getPerson(String department) {
        Collection<String> pList = new ArrayList<String>();
        for (Filmography p : people) {
            if (p.getDepartment().equals(department)) {
                pList.add(p.getTitle());
            }
        }
        return pList;
    }

    public String getCertification() {
        return certification;
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFirst() {
        return first;
    }

    /**
     * Get the first logical file of the set of videos
     *
     * @return
     */
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

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.Identifiable#getId(java.lang.String)
     */
    @Override
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

    public String getGross(String country) {
        String result = gross.get(country);
        if (result != null) {
            return result;
        } else {
            return UNKNOWN;
        }
    }

    public Map<String, String> getGross() {
        return gross;
    }

    public String getOpenWeek(String country) {
        String result = openweek.get(country);
        if (result != null) {
            return result;
        } else {
            return UNKNOWN;
        }
    }

    public Map<String, String> getOpenWeek() {
        return openweek;
    }

    /**
     * Get the source of the override item
     *
     * @param overrideFlag
     * @return
     */
    public String getOverrideSource(OverrideFlag overrideFlag) {
        String source = overrideSources.get(overrideFlag);
        return StringUtils.isBlank(source) ? Movie.UNKNOWN : source;
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
    @Override
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

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getResolution() {
        return resolution;
    }

    // Return the width of the movie
    public int getWidth() {
        int width = 0;
        if (StringTools.isValidString(getResolution()) && getResolution().contains("x")) {
            width = NumberUtils.toInt(getResolution().substring(0, getResolution().indexOf("x")), 0);
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
    @Override
    public int getSeason() {
        /*
         * Return the first season as the whole season
         *
         * This could be changed later to allow multi season movie objects. Do
         * not return a value for the set master.
         */
        if (movieFiles.size() > 0 && !setMaster) {
            return getFirstFile().getSeason();
        } else {
            // Strictly speaking this isn't "-1" its a non-existent season.
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.IMovieBasicInformation#getTitle()
     */
    @Override
    @XmlElement
    public String getTitle() {
        return title;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.IMovieBasicInformation#getTitleSort()
     */
    /**
     * Return the correct sort title based on the mjb.sortTitle parameter
     *
     * @return
     */
    @Override
    @XmlElement
    public String getTitleSort() {
        // If we have a titleSort, return that
        if (StringTools.isValidString(titleSort)) {
            return titleSort;
        }

        // There are three choices for the sort title: title, original, filename
        if (TITLE_SORT_TYPE == TitleSortType.TITLE) {
            // Set the title sort (so this is only done once)
            setTitleSort(title);
        } else if (TITLE_SORT_TYPE == TitleSortType.FILENAME) {
            // Set the title sort (so this is only done once)
            setTitleSort(baseName);
        } else if ((TITLE_SORT_TYPE == TitleSortType.ORIGINAL || TITLE_SORT_TYPE == TitleSortType.ADOPT_ORIGINAL) && StringTools.isValidString(originalTitle)) {
            // Set the title sort (so this is only done once)
            setTitleSort(originalTitle);
        }

        return titleSort;
    }

    @Override
    @XmlElement
    public String getOriginalTitle() {
        return originalTitle;
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
    @Override
    public String getYear() {
        return year;
    }

    public String getBudget() {
        return budget;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public void setDirty(DirtyFlag dirtyType, boolean dirty) {
        if (dirty) {
            dirtyFlags.add(dirtyType);
        } else {
            dirtyFlags.remove(dirtyType);
        }
    }

    public void setDirty(DirtyFlag dirtyType) {
        dirtyFlags.add(dirtyType);
    }

    /**
     * Clear ALL the dirty flags, and just set DirtyFlag.INFO to the passed value
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        clearDirty();
        setDirty(DirtyFlag.INFO, dirty);
    }

    /**
     * Returns true if ANY of the dirty flags are set. Use with caution, it's better to test individual flags as you need them,
     * rather than this generic flag
     *
     * @return
     */
    @XmlTransient
    public boolean isDirty() {
        if (dirtyFlags.isEmpty()) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }

    public String showDirty() {
        if (dirtyFlags.isEmpty()) {
            return "NOT DIRTY";
        } else {
            return dirtyFlags.toString();
        }
    }

    public Set<DirtyFlag> getDirty() {
        return dirtyFlags;
    }

    public void clearDirty() {
        dirtyFlags.clear();
    }

    public boolean isDirty(DirtyFlag dirtyType) {
        return dirtyFlags.contains(dirtyType);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.IMovieBasicInformation#isTVShow()
     */
    @XmlAttribute(name = "isTV")
    @Override
    public boolean isTVShow() {
        return (this.movieType.equals(TYPE_TVSHOW) || getSeason() != -1);
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

    @XmlTransient
    public boolean is3D() {
        return (getVideoSource().indexOf("3D") > -1);
    }

    public void setBaseName(String baseName) {
        this.baseName = validateString(baseName, this.baseName);
    }

    public void setBaseFilename(String baseFilename) {
        this.baseFilename = validateString(baseFilename, this.baseFilename);
    }

    public void addDidYouKnow(String fact) {
        if (fact != null && !didYouKnow.contains(fact)) {
            setDirty(DirtyFlag.INFO);
            didYouKnow.add(fact);
        }
    }

    public void setDidYouKnow(List<String> facts) {
        if (facts != null && !facts.isEmpty()) {
            didYouKnow = facts;
            setDirty(DirtyFlag.INFO);
        }
    }

    public void clearDidYouKnow() {
        didYouKnow.clear();
        setDirty(DirtyFlag.INFO);
    }

    public void clearAwards() {
        awards.clear();
        setDirty(DirtyFlag.INFO);
    }

    public boolean addActor(String actor, String source) {
        Boolean added = Boolean.FALSE;
        if (StringTools.isValidString(actor) && (cast.size() < MAX_COUNT_ACTOR)) {
            added = cast.add(actor.trim());
            if (added) {
                setDirty(DirtyFlag.INFO);
                setOverrideSource(OverrideFlag.ACTORS, source);
            }
        }
        return added;
    }

    public boolean addActor(String actorKey, String actorName, String character, String actorUrl, String doublage, String source) {
        boolean added = Boolean.FALSE;

        if (actorName != null) {
            String name = actorName;
            if (actorName.indexOf(":") > -1) {
                String[] names = actorName.split(":");
                if (StringTools.isValidString(names[1])) {
                    name = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    name = names[0];
                }
            }

            name = name.trim();
            boolean found = Boolean.FALSE;
            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(name) && p.getDepartment().equals(Filmography.DEPT_ACTORS)) {
                    found = Boolean.TRUE;
                    break;
                }
            }

            if (!found) {
                added = addPerson(actorKey, actorName, actorUrl, StringUtils.capitalize(Filmography.JOB_ACTOR), character, doublage, source);
                if (added) {
                    setOverrideSource(OverrideFlag.PEOPLE_ACTORS, source);
                }
            }
        }
        return added;
    }

    public void setCast(Collection<String> cast, String source) {
        if (cast != null && !cast.isEmpty()) {
            clearCast();
            for (String actor : cast) {
                addActor(actor, source);
            }
        }
    }

    public void setPeopleCast(Collection<String> cast, String source) {
        if ((MAX_COUNT_ACTOR > 0) && (cast != null) && !cast.isEmpty()) {
            clearPeopleCast();
            int count = 0;
            for (String actor : cast) {
                if (addActor(Movie.UNKNOWN, actor, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN, source)) {
                    count++;
                    if (count == MAX_COUNT_ACTOR) {
                        break;
                    }
                }
            }
        }
    }

    public void clearCast() {
        if (cast.size() > 0) {
            setDirty(DirtyFlag.INFO);
            cast.clear();
        }
    }

    public void clearPeopleCast() {
        if (people.size() > 0) {
            Collection<Filmography> pList = new ArrayList<Filmography>();
            for (Filmography p : people) {
                if (p.getDepartment().equals(Filmography.DEPT_ACTORS)) {
                    pList.add(p);
                }
            }
            for (Filmography p : pList) {
                removePerson(p);
            }
        }
    }

    public boolean addWriter(String writer, String source) {
        boolean added = Boolean.FALSE;
        if (StringTools.isValidString(writer) && (writers.size() < MAX_COUNT_WRITER)) {
            added = writers.add(writer.trim());
            if (added) {
                setDirty(DirtyFlag.INFO);
                setOverrideSource(OverrideFlag.WRITERS, source);
            }
        }
        return added;
    }

    public boolean addWriter(String writerKey, String name, String writerUrl, String source) {
        boolean added = Boolean.FALSE;

        if (name != null) {
            String writerName = name;
            if (name.indexOf(":") > -1) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[1])) {
                    writerName = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    writerName = names[0];
                }
            }
            writerName = writerName.trim();

            boolean found = Boolean.FALSE;
            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(writerName) && p.getDepartment().equals(Filmography.DEPT_WRITING)) {
                    found = Boolean.TRUE;
                    break;
                }
            }

            if (!found) {
                added = addPerson(writerKey, name, writerUrl, "Writer", source);
                if (added) {
                    setOverrideSource(OverrideFlag.PEOPLE_WRITERS, source);
                }
            }
        }
        return added;
    }

    public void setWriters(Collection<String> writers, String source) {
        if (writers != null && !writers.isEmpty()) {
            clearWriters();
            for (String writer : writers) {
                addWriter(writer, source);
            }
        }
    }

    public void setPeopleWriters(Collection<String> writers, String source) {
        if ((MAX_COUNT_WRITER > 0) && (writers != null) && !writers.isEmpty()) {
            clearPeopleWriters();
            int count = 0;
            for (String writer : writers) {
                if (addWriter(Movie.UNKNOWN, writer, Movie.UNKNOWN, source)) {
                    count++;
                    if (count == MAX_COUNT_WRITER) {
                        break;
                    }
                }
            }
        }
    }

    public void clearWriters() {
        if (writers.size() > 0) {
            setDirty(DirtyFlag.INFO);
            writers.clear();
        }
    }

    public void clearPeopleWriters() {
        if (people.size() > 0) {
            Collection<Filmography> pList = new ArrayList<Filmography>();
            for (Filmography p : people) {
                if (p.getDepartment().equals(Filmography.DEPT_WRITING)) {
                    pList.add(p);
                }
            }
            for (Filmography p : pList) {
                removePerson(p);
            }
        }
    }

    public void setCompany(String company, String source) {
        if (StringTools.isValidString(company)) {
            if (!company.equalsIgnoreCase(this.company)) {
                setDirty(DirtyFlag.INFO);
                this.company = company;
            }
            setOverrideSource(OverrideFlag.COMPANY, source);
        }
    }

    public void setContainer(String container, String source) {
        if (StringTools.isValidString(container)) {
            if (!container.equalsIgnoreCase(this.container)) {
                setDirty(DirtyFlag.INFO);
                this.container = container;
            }
            setOverrideSource(OverrideFlag.CONTAINER, source);
        }
    }

    public void setCountry(final String country, String source) {
        if (StringTools.isValidString(country)) {
            String tmpCountry;
            // Shorten country to USA
            if (country.equalsIgnoreCase("United States of America")) {
                tmpCountry = "USA";
            } else {
                tmpCountry = country;
            }

            if (!tmpCountry.equalsIgnoreCase(this.country)) {
                setDirty(DirtyFlag.INFO);
                this.country = tmpCountry;
            }
            setOverrideSource(OverrideFlag.COUNTRY, source);
        }
    }

    /**
     * Get just one director from the collection
     *
     * @return
     */
    public String getDirector() {
        if (directors != null && !directors.isEmpty()) {
            return directors.iterator().next();
        } else {
            return UNKNOWN;
        }
    }

    public Collection<String> getDirectors() {
        return directors;
    }

    public void setDirector(String director, String source) {
        if (StringTools.isValidString(director)) {
            clearDirectors();
            addDirector(director, source);
        }
    }

    public void setDirectors(Collection<String> directors, String source) {
        if (directors != null && !directors.isEmpty()) {
            clearDirectors();
            for (String director : directors) {
                addDirector(director, source);
            }
        }
    }

    public void setPeopleDirectors(Collection<String> directors, String source) {
        if ((MAX_COUNT_DIRECTOR > 0) && (directors != null) && !directors.isEmpty()) {
            clearPeopleDirectors();
            int count = 0;
            for (String director : directors) {
                if (addDirector(Movie.UNKNOWN, director, Movie.UNKNOWN, source)) {
                    count++;
                    if (count == MAX_COUNT_DIRECTOR) {
                        break;
                    }
                }
            }
        }
    }

    public void clearDirectors() {
        if (directors.size() > 0) {
            setDirty(DirtyFlag.INFO);
            directors.clear();
        }
    }

    public void clearPeopleDirectors() {
        if (people.size() > 0) {
            Collection<Filmography> pList = new ArrayList<Filmography>();
            for (Filmography p : people) {
                if (p.getDepartment().equals(Filmography.DEPT_DIRECTING)) {
                    pList.add(p);
                }
            }
            for (Filmography p : pList) {
                removePerson(p);
            }
        }
    }

    public boolean addDirector(String director, String source) {
        boolean added = Boolean.FALSE;
        if (StringTools.isValidString(director) && (directors.size() < MAX_COUNT_DIRECTOR)) {
            added = directors.add(director.trim());
            if (added) {
                setDirty(DirtyFlag.INFO);
                setOverrideSource(OverrideFlag.DIRECTORS, source);
            }
        }
        return added;
    }

    public boolean addDirector(String key, String name, String URL, String source) {
        boolean added = Boolean.FALSE;

        if (name != null) {
            String directorName = name;
            if (name.indexOf(":") > -1) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[1])) {
                    directorName = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    directorName = names[0];
                }
            }
            directorName = directorName.trim();

            boolean found = Boolean.FALSE;
            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(directorName) && p.getDepartment().equals(Filmography.DEPT_DIRECTING)) {
                    found = Boolean.TRUE;
                    break;
                }
            }

            if (!found) {
                added = addPerson(key, name, URL, "Director", source);
                if (added) {
                    setOverrideSource(OverrideFlag.PEOPLE_DIRECTORS, source);
                }
            }
        }

        return added;
    }

    public void setFirst(String first) {
        this.first = validateString(first, this.first);
    }

    public void setFps(float fps, String source) {
        // Prevent wrong result caused by floating point rounding by allowing difference of 0.1 fpsS
        if (Math.abs(fps - this.fps) > 0.1) {
            setDirty(DirtyFlag.INFO);
            this.fps = fps;
        }
        setOverrideSource(OverrideFlag.FPS, source);
    }

    public void setGenres(Collection<String> genres, String source) {
        if (!extra && (genres != null) && (genres.size() > 0)) {
            this.genres.clear();
            for (String genre : genres) {
                if (StringTools.isValidString(genre) && !GENRE_SKIP_LIST.contains(genre.toLowerCase())) {
                    this.genres.add(genre);
                }
            }
            setDirty(DirtyFlag.INFO);
            this.setOverrideSource(OverrideFlag.GENRES, source);
        }
    }

    public void setSets(Map<String, Integer> sets) {
        setDirty(DirtyFlag.INFO);
        this.sets = sets;
    }

    public Map<String, Integer> getSets() {
        return sets;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.Identifiable#setId(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void setId(final String key, final String id) {
        String tmpId = StringUtils.trim(id);    // Clean the ID
        if (StringTools.isValidString(key) && StringTools.isValidString(tmpId) && !tmpId.equalsIgnoreCase(this.getId(key))) {
            setDirty(DirtyFlag.INFO);
            this.idMap.put(key, tmpId);
        }
    }

    public void setId(String key, int id) {
        setId(key, Integer.toString(id));
    }

    public void setGross(String country, String value) {
        if (StringTools.isValidString(country) && StringTools.isValidString(value) && !value.equalsIgnoreCase(this.getGross(country))) {
            setDirty(DirtyFlag.INFO);
            this.gross.put(country, value);
        }
    }

    public void setGross(Map<String, String> gross) {
        if (gross != null) {
            this.gross = gross;
        }
    }

    public void setOpenWeek(String country, String value) {
        if (StringTools.isValidString(country) && StringTools.isValidString(value) && !value.equalsIgnoreCase(this.getOpenWeek(country))) {
            setDirty(DirtyFlag.INFO);
            this.openweek.put(country, value);
        }
    }

    public void setOpenWeek(Map<String, String> openweek) {
        if (openweek != null) {
            this.openweek = openweek;
        }
    }

    public void setOverrideSource(OverrideFlag flag, String source) {
        if (StringUtils.isBlank(source)) {
            this.overrideSources.put(flag, UNKNOWN);
        } else {
            this.overrideSources.put(flag, source.toUpperCase());
        }
    }

    public void setLanguage(String language, String source) {
        if (StringTools.isValidString(language)) {
            if (!language.equalsIgnoreCase(this.language)) {
                setDirty(DirtyFlag.INFO);
                this.language = language;
            }
            setOverrideSource(OverrideFlag.LANGUAGE, source);
        }
    }

    public void setCertification(String certification, String source) {
        if (StringTools.isValidString(certification)) {
            if (!certification.equalsIgnoreCase(this.certification)) {
                setDirty(DirtyFlag.INFO);
                this.certification = certification;
            }
            setOverrideSource(OverrideFlag.CERTIFICATION, source);
        }
    }

    public void setLast(String last) {
        this.last = validateString(last, this.last);
    }

    public void setMovieFiles(Collection<MovieFile> movieFiles) {
        setDirty(DirtyFlag.INFO);
        this.movieFiles = movieFiles;
    }

    public void setExtraFiles(Collection<ExtraFile> extraFiles) {
        setDirty(DirtyFlag.INFO);
        this.extraFiles = extraFiles;
    }

    public void setAwards(Collection<AwardEvent> awards) {
        setDirty(DirtyFlag.INFO);
        this.awards = awards;
    }

    public void setPeople(Collection<Filmography> people) {
        setDirty(DirtyFlag.INFO);
        this.people = people;
    }

    public void setNext(String next) {
        this.next = validateString(next, this.next);
    }

    /**
     * Set the movie plot.
     *
     * Will replace non-standard quotes and "&amp;" as needed
     *
     * @param plot
     * @param source
     */
    public void setPlot(String plot, String source) {
        this.setPlot(plot, source, Boolean.TRUE);
    }

    /**
     * Set the movie plot.
     *
     * Will replace non-standard quotes and "&amp;" as needed
     *
     * @param plot
     * @param source
     * @param trimToLength
     */
    public void setPlot(String plot, String source, boolean trimToLength) {
        if (StringTools.isValidString(plot)) {
            String tmpPlot = StringUtils.replace(StringTools.replaceQuotes(plot), "&amp;", "&");
            if (trimToLength) {
                tmpPlot = StringTools.trimToLength(tmpPlot, MAX_LENGTH_PLOT);
            }

            if (!tmpPlot.equalsIgnoreCase(this.plot)) {
                setDirty(DirtyFlag.INFO);
                this.plot = tmpPlot;
            }
            setOverrideSource(OverrideFlag.PLOT, source);
        }
    }

    public String getOutline() {
        return outline;
    }

    /**
     * Set the movie outline.
     *
     * Will replace non-standard quotes and "&amp;" as needed
     *
     * @param outline
     * @param source
     */
    public void setOutline(String outline, String source) {
        this.setOutline(outline, source, Boolean.TRUE);
    }

    /**
     * Set the movie outline.
     *
     * Will replace non-standard quotes and "&amp;" as needed
     *
     * @param outline
     * @param source
     * @param trimToLength
     */
    public void setOutline(String outline, String source, boolean trimToLength) {
        if (StringTools.isValidString(outline)) {
            String tmpOutline = StringUtils.replace(StringTools.replaceQuotes(outline), "&amp;", "&");
            if (trimToLength) {
                tmpOutline = StringTools.trimToLength(tmpOutline, MAX_LENGTH_OUTLINE);
            }

            if (!tmpOutline.equalsIgnoreCase(this.outline)) {
                setDirty(DirtyFlag.INFO);
                this.outline = tmpOutline;
            }
            setOverrideSource(OverrideFlag.OUTLINE, source);
        }
    }

    public void setPrevious(String previous) {
        this.previous = validateString(previous, this.previous);
    }

    public int getRating() {
        if (ratings == null || ratings.isEmpty()) {
            return -1;
        }

        for (String site : ratingSource) {
            if ("average".equalsIgnoreCase(site)) {
                // Return the average of the ratings
                int rating = 0;
                int count = 0;

                for (String ratingSite : ratings.keySet()) {
                    if (ratingIgnore.size() > 0) {
                        if (ratingIgnore.contains(ratingSite)) {
                            continue;
                        } else {
                            boolean found = Boolean.FALSE;
                            for (String ignoreName : ratingIgnore) {
                                if (ratingSite.indexOf(ignoreName) == 0) {
                                    found = Boolean.TRUE;
                                    break;
                                }
                            }
                            if (found) {
                                continue;
                            }
                        }
                    }
                    rating += ratings.get(ratingSite);
                    count++;
                }

                return (count > 0 ? (rating / count) : 0);
            }

            if (ratings.containsKey(site)) {
                return ratings.get(site);
            }
        }

        // No ratings found, so return -1
        return -1;
    }

    public int getRating(String site) {
        if (ratings.containsKey(site)) {
            return ratings.get(site);
        } else {
            return -1;
        }
    }

    public Map<String, Integer> getRatings() {
        return ratings;
    }

    public void addRating(String site, int rating) {
        if (StringTools.isValidString(site)) {
            if (ratings.containsKey(site)) {
                if (ratings.get(site) != rating) {
                    ratings.remove(site);
                    ratings.put(site, rating);
                    setDirty(DirtyFlag.INFO);
                }
            } else {
                ratings.put(site, rating);
            }
        }
    }

    public void setRatings(Map<String, Integer> ratings) {
        if (ratings != null && !ratings.isEmpty()) {
            this.ratings = ratings;
        }
    }

    public void setReleaseDate(final String releaseDate, String source) {
        if (StringTools.isNotValidString(releaseDate)) {
            // nothing to do
            return;
        }

        String parseDate = DateTimeTools.parseDateTo(releaseDate, "yyyy-MM-dd");

        if (StringTools.isValidString(parseDate)) {
            if (!parseDate.equalsIgnoreCase(this.releaseDate)) {
                setDirty(DirtyFlag.INFO);
                this.releaseDate = parseDate;
            }
            setOverrideSource(OverrideFlag.RELEASEDATE, source);
        }
    }

    public void setResolution(String resolution, String source) {
        if (StringTools.isValidString(resolution)) {
            if (!resolution.equalsIgnoreCase(this.resolution)) {
                setDirty(DirtyFlag.INFO);
                this.resolution = resolution;
            }
            setOverrideSource(OverrideFlag.RESOLUTION, source);
        }
    }

    public void setResolution(String width, String height, String source) {
        if (StringTools.isValidString(width) && StringTools.isValidString(height)) {
            setResolution(width + "x" + height, source);
        }
    }

    public void setRuntime(String runtime, String source) {
        if (StringTools.isValidString(runtime)) {
            if (!runtime.equalsIgnoreCase(this.runtime)) {
                setDirty(DirtyFlag.INFO);
                // Escape the first "0" AlloCine gives sometimes
                if (runtime.startsWith("0")) {
                    this.runtime = runtime.substring(1).trim();
                } else {
                    this.runtime = runtime.trim();
                }
            }
            setOverrideSource(OverrideFlag.RUNTIME, source);
        }
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = validateString(subtitles, this.subtitles);
    }

    public void setTitle(String title, String source) {
        if (StringTools.isValidString(title)) {
            if (!title.equals(this.title)) {
                setDirty(DirtyFlag.INFO);
                this.title = title;
            }
            setOverrideSource(OverrideFlag.TITLE, source);

            // If we don't have a original title, then use the title
            if (StringTools.isNotValidString(originalTitle)) {
                setOriginalTitle(title, source);
            }
        }
    }

    public void setTitleSort(final String title) {
        if (title.equals(titleSort)) {
            return;
        }

        String newTitle;
        if (StringUtils.isBlank(title)) {
            newTitle = UNKNOWN;
        } else {
            newTitle = title;
        }

        if (!newTitle.equals(this.titleSort)) {
            int idx = 0;
            while (idx < newTitle.length() && !Character.isLetterOrDigit(newTitle.charAt(idx))) {
                idx++;
            }

            // Issue 1908: Replace all non-standard characters in the title sort
            this.titleSort = StringTools.stringMapReplacement(newTitle.substring(idx));
            setDirty(DirtyFlag.INFO);
        }
    }

    public void setOriginalTitle(String originalTitle, String source) {
        if (StringTools.isValidString(originalTitle)) {
            if (!originalTitle.equals(this.originalTitle)) {
                setDirty(DirtyFlag.INFO);
                this.originalTitle = originalTitle;
            }
            setOverrideSource(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    /**
     * Get the video codec. You should use the getCodec methods instead
     *
     * @return
     * @deprecated
     */
    @Deprecated
    public String getVideoCodec() {
        for (Codec videoCodec : codecs) {
            if (videoCodec.getCodecType() == CodecType.VIDEO) {
                if (StringTools.isValidString(videoCodec.getCodecIdHint())) {
                    return videoCodec.getCodecIdHint();
                }
                if (StringTools.isValidString(videoCodec.getCodec())) {
                    return videoCodec.getCodec();
                }
                if (StringTools.isValidString(videoCodec.getCodecFormat())) {
                    return videoCodec.getCodecFormat();
                }
                if (StringTools.isValidString(videoCodec.getCodecId())) {
                    return videoCodec.getCodecId();
                }
            }
        }

        return UNKNOWN;
    }

    public void setVideoOutput(String videoOutput, String source) {
        if (StringTools.isValidString(videoOutput)) {
            if (!videoOutput.equalsIgnoreCase(this.videoOutput)) {
                setDirty(DirtyFlag.INFO);
                this.videoOutput = videoOutput;
            }
            setOverrideSource(OverrideFlag.VIDEOOUTPUT, source);
        }
    }

    public void setVideoSource(String videoSource, String source) {
        if (StringTools.isValidString(videoSource)) {
            if (!videoSource.equalsIgnoreCase(this.videoSource)) {
                setDirty(DirtyFlag.INFO);
                this.videoSource = videoSource;
            }
            setOverrideSource(OverrideFlag.VIDEOSOURCE, source);
        }
    }

    public void setYear(String year, String source) {
        if (StringTools.isValidString(year)) {
            if (!year.equalsIgnoreCase(this.year)) {
                setDirty(DirtyFlag.INFO);
                this.year = year;
            }
            setOverrideSource(OverrideFlag.YEAR, source);
        }
    }

    public void setBudget(String budget) {
        this.budget = validateString(budget, this.budget);
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote, String source) {
        if (StringTools.isValidString(quote)) {
            if (!quote.equalsIgnoreCase(this.quote)) {
                setDirty(DirtyFlag.INFO);
                this.quote = quote;
            }
            setOverrideSource(OverrideFlag.QUOTE, source);
        }
    }

    /**
     * Validate the testString to ensure it is correct before setting the Dirty INFO flag if it is different
     *
     * @param testString
     * @param currentString
     * @return
     */
    private String validateString(String testString, String currentString) {
        String newString;
        if (StringUtils.isBlank(testString)) {
            newString = UNKNOWN;
        } else {
            newString = testString;
        }

        if (!newString.equalsIgnoreCase(currentString)) {
            setDirty(DirtyFlag.INFO);
        }
        return newString;
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
        if (!isSetMaster()) {
            synchronized (this) {
                if (lastModifiedTimeStamp == 0L) {
                    for (MovieFile mf : getMovieFiles()) {
                        lastModifiedTimeStamp = Math.max(lastModifiedTimeStamp, mf.getLastModified());
                    }
                }
                //make sure the fileDate is correct too
                addFileDate(new Date(lastModifiedTimeStamp));
                return lastModifiedTimeStamp;
            }
        } else {
            // Sets don't hold the time/date of their files, so just return the time of the file
            return getFileDate().getTime();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[Movie ");
        for (Map.Entry<String, String> e : idMap.entrySet()) {
            sb.append("[id_").append(e.getKey()).append("=").append(e.getValue()).append("]");
        }
        sb.append("[title=").append(title).append("]");
        sb.append("[titleSort=").append(titleSort).append("]");
        sb.append("[year=").append(year).append("]");
        sb.append("[releaseDate=").append(releaseDate).append("]");
        sb.append("[ratings=").append(ratings).append("]");
        sb.append("[top250=").append(top250).append("]");
        sb.append("[posterURL=").append(posterURL).append("]");
        sb.append("[bannerURL=").append(bannerURL).append("]");
        sb.append("[fanartURL=").append(fanartURL).append("]");
        sb.append("[plot=").append(plot).append("]");
        sb.append("[outline=").append(outline).append("]");
        sb.append("[director=").append(directors.toString()).append("]");
        sb.append("[country=").append(country).append("]");
        sb.append("[company=").append(company).append("]");
        sb.append("[runtime=").append(runtime).append("]");
        sb.append("[season=").append(getSeason()).append("]");
        sb.append("[language=").append(language).append("]");
        sb.append("[subtitles=").append(subtitles).append("]");
        sb.append("[container=").append(container).append("]"); // AVI, MKV, TS, etc.
        sb.append("[codecs=").append(codecs.toString()).append("]");
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
     * @param extra Boolean flag, true=extra file, false=normal file
     */
    public void setExtra(boolean extra) {
        setDirty(DirtyFlag.INFO);
        this.extra = extra;
        if (extra) {
            genres.clear();
        }
    }

    /**
     * This function will return true if the movie can have trailers.
     *
     * @return
     */
    public boolean canHaveTrailers() {
        if (isExtra() || getMovieType().equals(Movie.TYPE_TVSHOW)) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }

    public void setTrailerExchange(Boolean trailerExchange) {
        if (this.trailerExchange != trailerExchange) {
            setDirty(DirtyFlag.INFO);
            this.trailerExchange = trailerExchange;
        }
    }

    /**
     * Set the date of the last trailers scan
     *
     * @param lastScan date of the last trailers scan
     */
    public void setTrailerLastScan(String lastScan) {
        try {
            if (StringTools.isValidString(lastScan)) {
                setTrailerLastScan(DateTime.parse(lastScan).toMillis());
            } else {
                setTrailerLastScan(0);
            }
        } catch (IllegalArgumentException ex) {
            LOG.debug("Unable to parse TrailerLastScan date from '" + lastScan + "', Error: " + ex.getMessage());
            setTrailerLastScan(0);
        }
    }

    /**
     * Set the date of the last trailers scan
     *
     * @param lastScan date of the last trailers scan (milliseconds offset from the Epoch)
     */
    public void setTrailerLastScan(long lastScan) {
        if (lastScan != this.trailerLastScan) {
            setDirty(DirtyFlag.INFO);
            this.trailerLastScan = lastScan;
        }
    }

    /**
     * Get the date of the last trailers scan
     *
     * @return the date of the last trailers scan (milliseconds offset from the Epoch)
     */
    public long getTrailerLastScan() {
        return trailerLastScan;
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
    @Override
    public Boolean isTrailerExchange() {
        return trailerExchange;
    }

    public void setMovieType(String movieType) {
        this.movieType = validateString(movieType, this.movieType);
    }

    public String getMovieType() {
        return this.movieType;
    }

    public void setFormatType(String formatType) {
        this.formatType = validateString(formatType, this.formatType);
    }

    public String getFormatType() {
        return this.formatType;
    }

    public void setVideoType(String videoType) {
        this.videoType = validateString(videoType, this.videoType);
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

    @Deprecated
    public String getAudioChannels() {
        StringBuilder sb = new StringBuilder();
        boolean firstChannel = Boolean.TRUE;

        for (Codec codec : codecs) {
            if (codec.getCodecType().equals(CodecType.AUDIO)) {
                if (firstChannel) {
                    firstChannel = Boolean.FALSE;
                } else {
                    sb.append(SPACE_SLASH_SPACE);
                }
                sb.append(codec.getCodecChannels());
            }
        }
        return sb.toString();
    }

    public int getTop250() {
        return top250;
    }

    public void setTop250(String top250, String source) {
        if (StringUtils.isNumeric(top250)) {
            setTop250(Integer.parseInt(top250), source);
        }
    }

    public void setTop250(int top250, String source) {
        if (top250 > 0) {
            if (top250 != this.top250) {
                setDirty(DirtyFlag.INFO);
                this.top250 = top250;
            }
            setOverrideSource(OverrideFlag.TOP250, source);
        }
    }

    public String getLibraryDescription() {
        return libraryDescription;
    }

    public void setLibraryDescription(String libraryDescription) {
        this.libraryDescription = validateString(libraryDescription, this.libraryDescription);
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

    public static void addSortIgnorePrefixes(String prefix) {
        SORT_IGNORE_PREFIXES.add(prefix);
    }

    /**
     * Take the passed DTO and update the movie information
     *
     * @param dto
     */
    public void mergeFileNameDTO(MovieFileNameDTO dto) {
        setExtra(dto.isExtra());

        if (OverrideTools.checkOverwriteTitle(this, SOURCE_FILENAME)) {
            setTitle(dto.getTitle(), SOURCE_FILENAME);
        }

        Codec tempCodec;
        if (StringTools.isValidString(dto.getAudioCodec())) {
            tempCodec = new Codec(CodecType.AUDIO);
            tempCodec.setCodec(dto.getAudioCodec());
            tempCodec.setCodecSource(CodecSource.FILENAME);
            addCodec(tempCodec);
        }

        if (StringTools.isValidString(dto.getVideoCodec())) {
            tempCodec = new Codec(CodecType.VIDEO);
            tempCodec.setCodec(dto.getVideoCodec());
            tempCodec.setCodecSource(CodecSource.FILENAME);
            addCodec(tempCodec);
        }

        if (OverrideTools.checkOverwriteVideoSource(this, SOURCE_FILENAME)) {
            setVideoSource(dto.getVideoSource(), SOURCE_FILENAME);
        }
        if (OverrideTools.checkOverwriteContainer(this, SOURCE_FILENAME)) {
            setContainer(dto.getContainer(), SOURCE_FILENAME);
        }
        if ((dto.getFps() > 0) && OverrideTools.checkOverwriteFPS(this, SOURCE_FILENAME)) {
            setFps(dto.getFps(), SOURCE_FILENAME);
        }

        setMovieIds(dto.getMovieIds());

        if (dto.getSeason() > -1) {
            // Mark the movie as a TV Show
            setMovieType(Movie.TYPE_TVSHOW);
        }

        for (MovieFileNameDTO.SetDTO set : dto.getSets()) {
            addSet(set.getTitle(), set.getIndex() >= 0 ? set.getIndex() : null);
        }

        if (OverrideTools.checkOverwriteYear(this, SOURCE_FILENAME)) {
            setYear(dto.getYear() > 0 ? String.valueOf(dto.getYear()) : null, SOURCE_FILENAME);
        }

        if ((dto.getLanguages().size() > 0) && OverrideTools.checkOverwriteLanguage(this, SOURCE_FILENAME)) {
            // TODO more languages?
            setLanguage(dto.getLanguages().get(0), SOURCE_FILENAME);
        }

        // set video type
        if (dto.getHdResolution() != null) {
            setVideoType(TYPE_VIDEO_HD);
        }

        // set video output
        if (OverrideTools.checkOverwriteVideoOutput(this, SOURCE_FILENAME)) {

            String videoOut = "";
            if (dto.getHdResolution() != null) {

                if (StringTools.isValidString(videoOutput)) {
                    videoOut = videoOutput;
                }

                switch (dto.getFps()) {
                    case 23:
                        videoOut = "1080p 23.976Hz";
                        break;
                    case 24:
                        videoOut = "1080p 24Hz";
                        break;
                    case 25:
                        videoOut = "1080p 25Hz";
                        break;
                    case 29:
                        videoOut = "1080p 29.97Hz";
                        break;
                    case 30:
                        videoOut = "1080p 30Hz";
                        break;
                    case 50:
                        if (StringUtils.isNotBlank(videoOut)) {
                            videoOut += " ";
                        }
                        videoOut += "50Hz";
                        break;
                    case 59:
                        videoOut = "1080p 59.94Hz";
                        break;
                    case 60:
                        if (StringUtils.isNotBlank(videoOut)) {
                            videoOut += " ";
                        }
                        videoOut += "60Hz";
                        break;
                    default:
                        if (StringUtils.isBlank(videoOut)) {
                            videoOut = Movie.UNKNOWN;
                        } else {
                            videoOut += " 60Hz";
                        }
                        break;
                }
            } else {
                switch (dto.getFps()) {
                    case 23:
                        videoOut = "23p";
                        break;
                    case 24:
                        videoOut = "24p";
                        break;
                    case 29:
                    case 30:
                    case 60:
                        videoOut = "NTSC";
                        break;
                    case 25:
                    case 49:
                    case 50:
                        videoOut = "PAL";
                        break;
                    default:
                        videoOut = Movie.UNKNOWN;
                        break;
                }
            }

            // set video output
            setVideoOutput(videoOut, SOURCE_FILENAME);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.moviejukebox.model.IMovieBasicInformation#isSetMaster()
     */
    @XmlAttribute(name = "isSet")
    @Override
    public boolean isSetMaster() {
        return setMaster;
    }

    public void setSetMaster(boolean setMaster) {
        this.setMaster = setMaster;
    }

    @XmlAttribute(name = "setSize")
    public int getSetSize() {
        return this.setSize;
    }

    public void setSetSize(final int size) {
        this.setSize = size;
    }

    /**
     * Store the latest filedate for a set of movie files. Synchronized so that the comparisons don't overlap
     *
     * @param fileDate
     */
    public synchronized void addFileDate(Date fileDate) {
        if (fileDate == null) {
            return;
        }

        if (this.fileDate == null) {
            this.fileDate = fileDate;
        } else if (fileDate.after(this.fileDate)) {
            this.fileDate = fileDate;
        }
    }

    /**
     * Overwrite the file date
     *
     * @param fileDate
     */
    public synchronized void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
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
        return StringTools.formatFileSize(fileSize);
    }

    public void setAspectRatio(String aspectRatio, String source) {
        if (StringTools.isValidString(aspectRatio)) {
            if (!aspectRatio.equalsIgnoreCase(this.aspect)) {
                setDirty(DirtyFlag.INFO);
                this.aspect = aspectRatio;
            }
            setOverrideSource(OverrideFlag.ASPECTRATIO, source);
        }
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
     * Should be called only from ArtworkScanner. Avoid calling this inside MoviePlugin Also called from MovieNFOScanner
     *
     * @param posterURL
     */
    public void setPosterURL(String posterURL) {
        if (StringTools.isValidString(posterURL)) {
            // If the artwork URL is different, then change it, otheriwse leave alone.
            if (!posterURL.equalsIgnoreCase(this.posterURL)) {
                setDirty(DirtyFlag.POSTER, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.posterURL = posterURL;
            }
        } else {
            this.posterURL = UNKNOWN;
        }
    }

    @XmlElement(name = "posterFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getPosterFilename() {
        return posterFilename;
    }

    public void setPosterFilename(String posterFilename) {
        if (StringTools.isValidString(posterFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.posterFilename = FileTools.createDirHash(posterFilename);
            } else {
                this.posterFilename = posterFilename;
            }
        } else {
            this.posterFilename = UNKNOWN;
        }
    }

    @XmlElement(name = "detailPosterFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getDetailPosterFilename() {
        return detailPosterFilename;
    }

    public void setDetailPosterFilename(String detailPosterFilename) {
        if (StringTools.isValidString(detailPosterFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.detailPosterFilename = FileTools.createDirHash(detailPosterFilename);
            } else {
                this.detailPosterFilename = detailPosterFilename;
            }
        } else {
            this.detailPosterFilename = UNKNOWN;
        }
    }

    // ***** Thumbnails
    @XmlElement(name = "thumbnail")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getThumbnailFilename() {
        return this.thumbnailFilename;
    }

    public void setThumbnailFilename(String thumbnailFilename) {
        if (StringTools.isValidString(thumbnailFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.thumbnailFilename = FileTools.createDirHash(thumbnailFilename);
            } else {
                this.thumbnailFilename = thumbnailFilename;
            }
        } else {
            this.thumbnailFilename = UNKNOWN;
        }
    }

    // ***** Footer
    @XmlElement(name = "footer")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public List<String> getFooterFilename() {
        return this.footerFilename;
    }

    public void setFooterFilename(final String footerFilename, Integer inx) {
        // Can't change the passed parameter
        String ff;
        if (StringUtils.isBlank(footerFilename)) {
            ff = UNKNOWN;
        } else {
            // create the directory hash if needed
            if (DIR_HASH) {
                ff = FileTools.createDirHash(FileTools.makeSafeFilename(footerFilename));
            } else {
                ff = FileTools.makeSafeFilename(footerFilename);
            }
        }

        if (this.footerFilename.size() <= inx) {
            while (this.footerFilename.size() < inx) {
                this.footerFilename.add(UNKNOWN);
            }
            this.footerFilename.add(ff);
        } else {
            this.footerFilename.set(inx, ff);
        }
    }

    // ***** Fanart
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartURL() {
        return fanartURL;
    }

    public void setFanartURL(String fanartURL) {
        if (StringTools.isValidString(fanartURL)) {
            if (!fanartURL.equalsIgnoreCase(this.fanartURL)) {
                setDirty(DirtyFlag.FANART, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);

                this.fanartURL = fanartURL;
            }
        } else {
            this.fanartURL = UNKNOWN;
        }
    }

    @XmlElement(name = "fanartFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartFilename() {
        return fanartFilename;
    }

    public void setFanartFilename(String fanartFilename) {
        if (StringTools.isValidString(fanartFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.fanartFilename = FileTools.createDirHash(fanartFilename);
            } else {
                this.fanartFilename = fanartFilename;
            }
        } else {
            this.fanartFilename = UNKNOWN;
        }
    }

    // ***** Banners
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerURL() {
        return bannerURL;
    }

    public void setBannerURL(String bannerURL) {
        if (StringTools.isValidString(bannerURL)) {
            if (!bannerURL.equalsIgnoreCase(this.bannerURL)) {
                setDirty(DirtyFlag.BANNER, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.bannerURL = bannerURL;
            }
        } else {
            this.bannerURL = UNKNOWN;
        }
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerFilename() {
        return bannerFilename;
    }

    public void setBannerFilename(String bannerFilename) {
        if (StringTools.isValidString(bannerFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.bannerFilename = FileTools.createDirHash(bannerFilename);
            } else {
                this.bannerFilename = bannerFilename;
            }
        } else {
            this.bannerFilename = UNKNOWN;
        }
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getWideBannerFilename() {
        return wideBannerFilename;
    }

    public void setWideBannerFilename(String wideBannerFilename) {
        if (StringTools.isValidString(wideBannerFilename)) {
            // create the directory hash if needed
            if (DIR_HASH) {
                this.wideBannerFilename = FileTools.createDirHash(wideBannerFilename);
            } else {
                this.wideBannerFilename = wideBannerFilename;
            }
        } else {
            this.wideBannerFilename = UNKNOWN;
        }
    }

    // ***** ClearLogo
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearLogoURL() {
        return clearLogoURL;
    }

    public void setClearLogoURL(String clearLogoURL) {
        if (StringTools.isValidString(clearLogoURL)) {
            if (!clearLogoURL.equalsIgnoreCase(this.clearLogoURL)) {
                setDirty(DirtyFlag.CLEARLOGO, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.clearLogoURL = clearLogoURL;
            }
        } else {
            this.clearArtURL = UNKNOWN;
        }
    }

    @XmlElement(name = "clearLogoFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearLogoFilename() {
        return clearLogoFilename;
    }

    public void setClearLogoFilename(String clearLogoFilename) {
        if (StringTools.isValidString(clearLogoFilename)) {
            this.clearLogoFilename = clearLogoFilename;
        } else {
            this.clearLogoFilename = UNKNOWN;
        }
    }

    // ***** ClearArt
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearArtURL() {
        return clearArtURL;
    }

    public void setClearArtURL(String clearArtURL) {
        if (StringTools.isValidString(clearArtURL)) {
            if (!clearArtURL.equalsIgnoreCase(this.clearArtURL)) {
                setDirty(DirtyFlag.CLEARART, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.clearArtURL = clearArtURL;
            }
        } else {
            this.clearArtURL = UNKNOWN;
        }
    }

    @XmlElement(name = "clearArtFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearArtFilename() {
        return clearArtFilename;
    }

    public void setClearArtFilename(String clearArtFilename) {
        if (StringTools.isValidString(clearArtFilename)) {
            this.clearArtFilename = clearArtFilename;
        } else {
            this.clearArtFilename = UNKNOWN;
        }
    }

    // ***** TvThumb
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getTvThumbURL() {
        return tvThumbURL;
    }

    public void setTvThumbURL(String tvThumbURL) {
        if (StringTools.isValidString(tvThumbURL)) {
            if (!tvThumbURL.equalsIgnoreCase(this.tvThumbURL)) {
                setDirty(DirtyFlag.TVTHUMB, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.tvThumbURL = tvThumbURL;
            }
        } else {
            this.tvThumbURL = UNKNOWN;
        }
    }

    @XmlElement(name = "tvThumbFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getTvThumbFilename() {
        return tvThumbFilename;
    }

    public void setTvThumbFilename(String tvThumbFilename) {
        if (StringTools.isValidString(tvThumbFilename)) {
            this.tvThumbFilename = tvThumbFilename;
        } else {
            this.tvThumbFilename = UNKNOWN;
        }
    }

    // ***** SeasonThumb
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getSeasonThumbURL() {
        return seasonThumbURL;
    }

    public void setSeasonThumbURL(String seasonThumbURL) {
        if (StringTools.isValidString(seasonThumbURL)) {
            if (!seasonThumbURL.equalsIgnoreCase(this.seasonThumbURL)) {
                setDirty(DirtyFlag.SEASONTHUMB, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.seasonThumbURL = seasonThumbURL;
            }
        } else {
            this.seasonThumbURL = UNKNOWN;
        }
    }

    @XmlElement(name = "seasonThumbFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getSeasonThumbFilename() {
        return seasonThumbFilename;
    }

    public void setSeasonThumbFilename(String seasonThumbFilename) {
        if (StringTools.isValidString(seasonThumbFilename)) {
            this.seasonThumbFilename = seasonThumbFilename;
        } else {
            this.seasonThumbFilename = UNKNOWN;
        }
    }

    // ***** MovieDisc
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getMovieDiscURL() {
        return movieDiscURL;
    }

    public void setMovieDiscURL(String movieDiscURL) {
        if (StringTools.isValidString(movieDiscURL)) {
            if (!movieDiscURL.equalsIgnoreCase(this.movieDiscURL)) {
                setDirty(DirtyFlag.MOVIEDISC, Boolean.TRUE);
                setDirty(DirtyFlag.INFO);
                this.movieDiscURL = movieDiscURL;
            }
        } else {
            this.movieDiscURL = UNKNOWN;
        }
    }

    @XmlElement(name = "movieDiscFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getMovieDiscFilename() {
        return movieDiscFilename;
    }

    public void setMovieDiscFilename(String movieDiscFilename) {
        if (StringTools.isValidString(movieDiscFilename)) {
            this.movieDiscFilename = movieDiscFilename;
        } else {
            this.movieDiscFilename = UNKNOWN;
        }
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

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline, String source) {
        if (StringTools.isValidString(tagline)) {
            if (!tagline.equalsIgnoreCase(this.tagline)) {
                setDirty(DirtyFlag.INFO);
                this.tagline = tagline;
            }
            setOverrideSource(OverrideFlag.TAGLINE, source);
        }
    }

    // Read the watched flag
    @XmlElement(name = "isWatched")
    public boolean isWatched() {
        // The watched NFO should override the watched file status
        return (watchedFile || watchedNFO);
    }

    @XmlTransient
    public boolean isWatchedNFO() {
        return watchedNFO;
    }

    @XmlTransient
    public boolean isWatchedFile() {
        return watchedFile;
    }

    // Set the watched flag for files
    public void setWatchedFile(boolean watched) {
        this.watchedFile = watched;
    }

    // Set the watched flag for NFO
    public void setWatchedNFO(boolean watched) {
        this.watchedNFO = watched;
    }

    @XmlElement
    public String getWatchedDateString() {
        long returnDate = getWatchedDate();

        if (returnDate == 0) {
            return Movie.UNKNOWN;
        } else {
            return new DateTime(returnDate).toString(DateTimeTools.getDateFormatLongString());
        }
    }

    /**
     * Look at the associated movie files and return the latest date a file was watched
     *
     * @return
     */
    public long getWatchedDate() {
        long returnDate = 0;

        for (MovieFile mf : movieFiles) {
            if (mf.isWatched() && mf.getWatchedDate() > returnDate) {
                returnDate = mf.getWatchedDate();
            }
        }

        return returnDate;
    }

    public String getShowStatus() {
        return showStatus;
    }

    public void setShowStatus(String showStatus) {
        this.showStatus = showStatus;
    }

    public void setMovieScanner(MovieDatabasePlugin movieScanner) {
        if (movieScanner != null) {
            this.movieScanner = movieScanner;
        }
    }

    @XmlTransient
    public MovieDatabasePlugin getMovieScanner() {
        return movieScanner;
    }

    /**
     * Copy the movie
     *
     * @param aMovie
     * @return
     */
    public static Movie newInstance(Movie aMovie) {
        Movie newMovie = new Movie();

        newMovie.baseName = aMovie.baseName;
        newMovie.baseFilename = aMovie.baseFilename;

        newMovie.title = aMovie.title;
        newMovie.titleSort = aMovie.titleSort;
        newMovie.originalTitle = aMovie.originalTitle;
        newMovie.year = aMovie.year;
        newMovie.releaseDate = aMovie.releaseDate;
        newMovie.plot = aMovie.plot;
        newMovie.outline = aMovie.outline;
        newMovie.quote = aMovie.quote;
        newMovie.tagline = aMovie.tagline;
        newMovie.country = aMovie.country;
        newMovie.company = aMovie.company;
        newMovie.runtime = aMovie.runtime;
        newMovie.language = aMovie.language;
        newMovie.videoType = aMovie.videoType;
        newMovie.subtitles = aMovie.subtitles;
        newMovie.container = aMovie.container;
        newMovie.resolution = aMovie.resolution;
        newMovie.aspect = aMovie.aspect;
        newMovie.videoSource = aMovie.videoSource;
        newMovie.videoOutput = aMovie.videoOutput;
        newMovie.fps = aMovie.fps;
        newMovie.certification = aMovie.certification;
        newMovie.showStatus = aMovie.showStatus;
        newMovie.scrapeLibrary = aMovie.scrapeLibrary;
        newMovie.extra = aMovie.extra;
        newMovie.trailerExchange = aMovie.trailerExchange;
        newMovie.trailerLastScan = aMovie.trailerLastScan;
        newMovie.libraryPath = aMovie.libraryPath;
        newMovie.movieType = aMovie.movieType;
        newMovie.formatType = aMovie.formatType;
        newMovie.top250 = aMovie.top250;
        newMovie.libraryDescription = aMovie.libraryDescription;
        newMovie.prebuf = aMovie.prebuf;
        newMovie.posterURL = aMovie.posterURL;
        newMovie.posterFilename = aMovie.posterFilename;
        newMovie.detailPosterFilename = aMovie.detailPosterFilename;
        newMovie.thumbnailFilename = aMovie.thumbnailFilename;
        newMovie.fanartURL = aMovie.fanartURL;
        newMovie.fanartFilename = aMovie.fanartFilename;
        newMovie.bannerURL = aMovie.bannerURL;
        newMovie.bannerFilename = aMovie.bannerFilename;
        newMovie.wideBannerFilename = aMovie.wideBannerFilename;
        newMovie.clearArtURL = aMovie.clearArtURL;
        newMovie.clearArtFilename = aMovie.clearArtFilename;
        newMovie.clearLogoURL = aMovie.clearLogoURL;
        newMovie.clearLogoFilename = aMovie.clearLogoFilename;
        newMovie.tvThumbURL = aMovie.tvThumbURL;
        newMovie.tvThumbFilename = aMovie.tvThumbFilename;
        newMovie.seasonThumbURL = aMovie.seasonThumbURL;
        newMovie.seasonThumbFilename = aMovie.seasonThumbFilename;
        newMovie.movieDiscURL = aMovie.movieDiscURL;
        newMovie.movieDiscFilename = aMovie.movieDiscFilename;
        newMovie.fileDate = aMovie.fileDate;
        newMovie.fileSize = aMovie.fileSize;
        newMovie.watchedFile = aMovie.watchedFile;
        newMovie.watchedNFO = aMovie.watchedNFO;
        newMovie.first = aMovie.first;
        newMovie.previous = aMovie.first;
        newMovie.next = aMovie.next;
        newMovie.last = aMovie.last;
        newMovie.file = aMovie.file;
        newMovie.containerFile = aMovie.containerFile;
        newMovie.setMaster = aMovie.setMaster;
        newMovie.setSize = aMovie.setSize;

        newMovie.idMap = new HashMap<String, String>(aMovie.idMap);
        newMovie.ratings = new HashMap<String, Integer>(aMovie.ratings);
        newMovie.directors = new LinkedHashSet<String>(aMovie.directors);
        newMovie.sets = new HashMap<String, Integer>(aMovie.sets);
        newMovie.genres = new TreeSet<String>(aMovie.genres);
        newMovie.cast = new LinkedHashSet<String>(aMovie.cast);
        newMovie.writers = new LinkedHashSet<String>(aMovie.writers);
        newMovie.awards = new ArrayList<AwardEvent>(aMovie.awards);
        newMovie.people = new ArrayList<Filmography>(aMovie.people);
        newMovie.indexes = new HashMap<String, String>(aMovie.indexes);
        newMovie.movieFiles = new TreeSet<MovieFile>(aMovie.movieFiles);
        newMovie.extraFiles = new TreeSet<ExtraFile>(aMovie.extraFiles);
        newMovie.dirtyFlags = EnumSet.copyOf(aMovie.dirtyFlags);
        newMovie.codecs = new LinkedHashSet<Codec>(aMovie.codecs);
        newMovie.footerFilename = new ArrayList<String>(aMovie.footerFilename);
        newMovie.overrideSources = new EnumMap<OverrideFlag, String>(aMovie.overrideSources);

        return newMovie;
    }

    public Set<Codec> getCodecs() {
        return codecs;
    }

    public void setCodecs(Set<Codec> codecs) {
        this.codecs = codecs;
        setDirty(DirtyFlag.INFO);
    }

    /**
     * Add a codec to the video file.
     *
     * @param newCodec
     */
    public void addCodec(final Codec newCodec) {
        // Check to see if the codec already exists
        boolean alreadyExists = Boolean.FALSE;
        // Store the codecs to delete in an array to prevent a concurent modification exception
        List<Codec> codecsToDelete = new ArrayList<Codec>();

        for (Codec existingCodec : codecs) {
            if (existingCodec.getCodecType() != newCodec.getCodecType()) {
                // Codecs are not the same type.
                continue;
            }

            // Checks to see if the Type, Codec, Language and Channels are the same
            if (existingCodec.equals(newCodec)) {
                // Check to see if the codec is better than an existing one
                if (existingCodec.getCodecSource().isBetter(newCodec.getCodecSource())) {
                    // Found an existing codec which is better
                    alreadyExists = Boolean.TRUE;
                } else {
                    // Found an existing codec, but newer is better source
                    codecsToDelete.add(existingCodec);
                }

                // No need to check further codecs
                break;
            }
        }

        // Delete codecs if it is not required
        for (Codec codecToDelete : codecsToDelete) {
            codecs.remove(codecToDelete);
            setDirty(DirtyFlag.INFO);
        }
        codecsToDelete.clear();

        // If the codec does not exist, add it to the list
        if (!alreadyExists) {
            this.codecs.add(newCodec);
            setDirty(DirtyFlag.INFO);
        }
    }

    public static List<String> getSortIgnorePrefixes() {
        return SORT_IGNORE_PREFIXES;
    }
}
