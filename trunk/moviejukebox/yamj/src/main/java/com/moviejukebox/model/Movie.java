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

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.tools.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime.DateTime;

/**
 * Movie bean
 *
 * @author jjulien
 * @author artem.gratchev
 */
@XmlType
public class Movie implements Comparable<Movie>, Identifiable, IMovieBasicInformation {
    /*
     * --------------------------------------------------------------------------------
     * Static & Final variables that are used for control and don't relate
     * specifically to the Movie object
     */

    public static final String dateFormatString = PropertiesUtil.getProperty("mjb.dateFormat", "yyyy-MM-dd");
    public static final String dateFormatLongString = dateFormatString + " HH:mm:ss";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    public static final SimpleDateFormat dateFormatLong = new SimpleDateFormat(dateFormatLongString);
    private static final Logger logger = Logger.getLogger(Movie.class);
    public static final String UNKNOWN = "UNKNOWN";
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
     * Caching - Dirty Flags More of these flags can be added to further
     * classify what changed
     */
    public static final String DIRTY_NFO = "NFO";
    public static final String DIRTY_FANART = "FANART";
    public static final String DIRTY_POSTER = "POSTER";
    public static final String DIRTY_BANNER = "BANNER";
    public static final String DIRTY_WATCHED = "WATCHED";
    public static final String DIRTY_INFO = "INFO"; // Information on the video (default)
    public static final String DIRTY_RECHECK = "RECHECK"; // The movie needs to be rechecked.
    public static final String DIRTY_CLEARART = "CLEARART";
    public static final String DIRTY_CLEARLOGO = "CLEARLOGO";
    public static final String DIRTY_TVTHUMB = "TVTHUMB";
    public static final String DIRTY_SEASONTHUMB = "SEASONTHUMB";
    public static final String DIRTY_CDART = "CDART";

    /*
     * --------------------------------------------------------------------------------
     * Properties that control the object
     */
    public static final ArrayList<String> sortIgnorePrefixes = new ArrayList<String>();
    private int highdef720 = PropertiesUtil.getIntProperty("highdef.720.width", "1280");    // Get the minimum width for a high-definition movies
    private int highdef1080 = PropertiesUtil.getIntProperty("highdef.1080.width", "1920");  // Get the minimum width for a high-definition movies
    private String[] ratingSource = PropertiesUtil.getProperty("mjb.rating.source", "average").split(",");
    private String tmpRatingIgnore = PropertiesUtil.getProperty("mjb.rating.ignore", "");
    private List<String> ratingIgnore = StringTools.isValidString(tmpRatingIgnore) ? Arrays.asList(tmpRatingIgnore.split(",")) : new ArrayList<String>();
    private static HashSet<String> genreSkipList = new HashSet<String>();   // List of genres to ignore
    private static String titleSortType = PropertiesUtil.getProperty("mjb.sortTitle", "title");
    /*
     * --------------------------------------------------------------------------------
     * Properties related to the Movie object itself
     */
    private String baseName;        // Safe name for generated files
    private String baseFilename;    // Base name for finding posters, nfos, banners, etc.
    private Map<String, String> idMap = new HashMap<String, String>(2);
    private String title = UNKNOWN;
    private String titleSort = UNKNOWN;
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
    private boolean extra = false;  // TODO Move extra flag to movie file
    private boolean trailerExchange = false;    // Trailers
    private long trailerLastScan = 0;           // Trailers
    private Collection<AwardEvent> awards = new ArrayList<AwardEvent>();    // Issue 1901: Awards
    private Collection<Filmography> people = new ArrayList<Filmography>();  // Issue 1897: Cast enhancement
    private String budget = UNKNOWN;                                        // Issue 2012: Financial information about movie
    private Map<String, String> openweek = new HashMap<String, String>();
    private Map<String, String> gross = new HashMap<String, String>();
    private Collection<String> DidYouKnow = new ArrayList<String>();        // Issue 2013: Add trivia
    private String libraryPath = UNKNOWN;
    private String movieType = TYPE_MOVIE;
    private String formatType = TYPE_FILE;
    private boolean overrideTitle = false;
    private boolean overrideYear = false;
    private int top250 = -1;
    private String libraryDescription = UNKNOWN;
    private long prebuf = -1;
    // Graphics URLs & files
    private Set<Artwork> artwork = new LinkedHashSet<Artwork>();
    private String posterURL = UNKNOWN; // The original, unaltered, poster
    private String posterFilename = UNKNOWN; // The poster filename
    private String detailPosterFilename = UNKNOWN; // The resized poster for skins
    private String thumbnailFilename = UNKNOWN; // The thumbnail version of the poster for skins
    private ArrayList<String> footerFilename = new ArrayList<String>(); // The footer image for skins
    private String fanartURL = UNKNOWN; // The fanart URL
    private String fanartFilename = UNKNOWN; // The resized fanart file
    private String bannerURL = UNKNOWN; // The TV Show banner URL
    private String bannerFilename = UNKNOWN; // The resized banner file
    private String clearArtURL = UNKNOWN;
    private String clearArtFilename = UNKNOWN;
    private String clearLogoURL = UNKNOWN;
    private String clearLogoFilename = UNKNOWN;
    private String seasonThumbURL = UNKNOWN;
    private String seasonThumbFilename = UNKNOWN;
    private String tvThumbURL = UNKNOWN;
    private String tvThumbFilename = UNKNOWN;
    private String cdArtURL = UNKNOWN;
    private String cdArtFilename = UNKNOWN;
    // File information
    private Date fileDate = null;
    private long fileSize = 0;
    private boolean watchedFile = false;    // Watched / Unwatched - Set from the .watched files
    private boolean watchedNFO = false; // Watched / Unwatched - Set from the NFO file
    // Navigation data
    private String first = UNKNOWN;
    private String previous = UNKNOWN;
    private String next = UNKNOWN;
    private String last = UNKNOWN;
    private Map<String, String> indexes = new HashMap<String, String>();
    // Media file properties
    Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();
    Collection<ExtraFile> extraFiles = new TreeSet<ExtraFile>();
    private Map<String, Boolean> dirtyFlags = new HashMap<String, Boolean>();   // List of the dirty flags associated with the Movie
    private File file;
    private File containerFile;
    // Set information
    private boolean isSetMaster = false;    // True if movie actually is only a entry point to movies set.
    private int setSize = 0;                // Amount of movies in set
    private MovieDatabasePlugin movieScanner = null;

    /*
     * --------------------------------------------------------------------------------
     * End of properties
     * --------------------------------------------------------------------------------
     */
    static {
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.genre.skip", ""), ",;|");
        while (st.hasMoreTokens()) {
            genreSkipList.add(st.nextToken().toLowerCase());
        }
    }

    public void setMjbVersion(String mjbVersion) {
        if (StringTools.isNotValidString(mjbVersion)) {
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
        String specificationVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
        if (StringUtils.isBlank(specificationVersion)) {
            specificationVersion = UNKNOWN;
        }
        return specificationVersion;
    }

    public void setMjbRevision(String mjbRevision) {
        if (StringTools.isNotValidString(mjbRevision)) {
            this.mjbRevision = "0";
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

    public String getCurrentMjbRevision() {
        String currentRevision = MovieJukebox.mjbRevision;
        // If YAMJ is self compiled then the revision information may not exist.
        if (StringUtils.isBlank(currentRevision) || (currentRevision.equalsIgnoreCase("${env.SVN_REVISION}"))) {
            currentRevision = Movie.UNKNOWN;
        }
        return currentRevision;
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
        return getMjbGenerationDate().toString(dateFormatLongString);
    }

    public DateTime getMjbGenerationDate() {
        if (this.mjbGenerationDate == null) {
            this.mjbGenerationDate = new DateTime();
        }
        return this.mjbGenerationDate;
    }

    public void addGenre(String genre) {
        if (StringTools.isValidString(genre) && !extra && !genreSkipList.contains(genre.toLowerCase())) {
            setDirty(DIRTY_INFO, true);
            //logger.debug("Genre added : " + genre);
            genres.add(genre);
        }
    }

    public void addSet(String set) {
        addSet(set, null);
    }

    public void addSet(String set, Integer order) {
        if (StringTools.isValidString(set)) {
            setDirty(DIRTY_INFO, true);
            logger.debug("Set added : " + set + ", order : " + order);
            sets.put(set, order);
        }
    }

    public void addMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            setDirty(DIRTY_INFO, true);
            // Always replace MovieFile
            for (MovieFile mf : this.movieFiles) {
                if (mf.compareTo(movieFile) == 0) {
                    movieFile.setFile(mf.getFile());
                    movieFile.setInfo(mf.getInfo());
                }
            }
            logger.debug("Movie addMovieFile : " + movieFile.getFilename());
            this.movieFiles.remove(movieFile);
            this.movieFiles.add(movieFile);
        }
    }

    public void addAward(AwardEvent award) {
        if (award != null) {
            setDirty(DIRTY_INFO, true);
            this.awards.add(award);
        }
    }

    public void addPerson(Filmography person) {
        if (person != null) {
            boolean duplicate = false;
            String name = person.getName();
            String job = person.getJob();
            for (Filmography p : people) {
                if (p.getName().equals(name) && p.getJob().equals(job)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                setDirty(DIRTY_INFO, true);
                people.add(person);
            }
        }
    }

    public void removePerson(Filmography person) {
        if (person != null) {
            people.remove(person);
        }
    }

    public void addPerson(String name) {
        addPerson(Movie.UNKNOWN, name, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN);
    }

    public void addPerson(String key, String name) {
        addPerson(key, name, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN);
    }

    public void addPerson(String key, String name, String URL) {
        addPerson(key, name, URL, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN);
    }

    public void addPerson(String key, String name, String URL, String job) {
        addPerson(key, name, URL, job, Movie.UNKNOWN, Movie.UNKNOWN);
    }

    public void addPerson(String key, String name, String URL, String job, String character) {
        addPerson(key, name, URL, job, character, Movie.UNKNOWN);
    }

    public void addPerson(String key, String name, String URL, String job, String character, String doublage) {
        if (StringUtils.isNotBlank(name)
                && StringUtils.isNotBlank(key)
                && StringUtils.isNotBlank(URL)
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

            person.setUrl(URL);
            person.setCharacter(character);
            person.setDoublage(doublage);
            person.setJob(job);
            person.setDepartment();

            int countActor = 0;
            if (person.getDepartment().equalsIgnoreCase("Actors")) {
                for (Filmography member : people) {
                    if (member.getDepartment().equalsIgnoreCase("Actors")) {
                        countActor++;
                    }
                }
            }

            person.setOrder(countActor);
            person.setCastId(people.size());
            person.setScrapeLibrary(scrapeLibrary);
            addPerson(person);
        }
    }

    public void removeMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            setDirty(DIRTY_INFO, true);
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
                return true;
            }
        }
        return false;
    }

    public void addExtraFile(ExtraFile extraFile) {
        // Only add extraFile if it doesn't already exists
        if (extraFile != null && !this.extraFiles.contains(extraFile)) {
            setDirty(DIRTY_INFO, true);
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
        return text.toString();
    }

    /**
     * Remove the sorting strip prefix from the title
     *
     * @param title
     * @return
     */
    private String getStrippedTitle(String title) {
        String lowerTitle = title.toLowerCase();

        for (String prefix : sortIgnorePrefixes) {
            if (lowerTitle.startsWith(prefix.toLowerCase())) {
                title = new String(title.substring(prefix.length()));
                break;
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
            if (audioCodec.getCodecType() == Codec.CodecType.AUDIO) {
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
        return DidYouKnow;
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
        int width;
        try {
            width = Integer.parseInt(new String(getResolution().substring(0, getResolution().indexOf("x"))));
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
    @Override
    public int getSeason() {
        /*
         * Return the first season as the whole season This could be changed
         * later to allow multi season movie objects. Do not return a value for
         * the set master.
         */
        if (movieFiles.size() > 0 && !isSetMaster) {
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
     */
    @Override
    public String getTitleSort() {
        // If we have a titleSort, return that
        if (StringTools.isValidString(titleSort)) {
            return titleSort;
        }

        // There are three choices for the sort title: title, original, filename

        if ("title".equalsIgnoreCase(titleSortType)) {
            // Set the title sort (so this is only done once)
            setTitleSort(title);
            return titleSort;
        }

        if ("filename".equalsIgnoreCase(titleSortType)) {
            // Set the title sort (so this is only done once)
            setTitleSort(baseName);
            return titleSort;
        }

        if ("original".equalsIgnoreCase(titleSortType)) {
            if (StringTools.isValidString(originalTitle)) {
                // Set the title sort (so this is only done once)
                setTitleSort(originalTitle);
                return titleSort;
            } else {
                setTitleSort(title);
                return titleSort;
            }
        }

        // Set the title sort (so this is only done once)
        setTitleSort(title);
        return titleSort;
    }

    @Override
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

    public void setDirty(String dirtyType, boolean dirty) {
//        SystemTools.logException(dirtyType + " " + String.valueOf(dirty));
//        logger.info(showDirty());
        dirtyFlags.put(dirtyType, dirty);
    }

    /**
     * Clear ALL the dirty flags, and just set DIRTY_INFO to the passed value
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        dirtyFlags.clear();
        setDirty(Movie.DIRTY_INFO, dirty);
    }

    /**
     * Returns true if ANY of the dirty flags are set. Use with caution, it's
     * better to test individual flags as you need them, rather than this
     * generic flag
     *
     * @return
     */
    @XmlTransient
    public boolean isDirty() {
        if (!dirtyFlags.isEmpty() && dirtyFlags.containsValue(true)) {
            return true;
        } else {
            return false;
        }
    }

//    @XmlTransient
    public String showDirty() {
        return dirtyFlags.toString();
    }

//    @XmlTransient
    public boolean isDirty(String dirtyType) {
        if (dirtyFlags.get(dirtyType) == null) {
            setDirty(dirtyType, false);
        }

        return dirtyFlags.get(dirtyType);
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
        if (StringUtils.isBlank(baseName)) {
            baseName = UNKNOWN;
        }
        if (!baseName.equalsIgnoreCase(this.baseName)) {
            setDirty(DIRTY_INFO, true);
            this.baseName = baseName;
        }
    }

    public void setBaseFilename(String filename) {
        if (StringUtils.isBlank(filename)) {
            filename = UNKNOWN;
        }

        if (!filename.equalsIgnoreCase(this.baseFilename)) {
            setDirty(DIRTY_INFO, true);
            this.baseFilename = filename;
        }
    }

    public void addDidYouKnow(String fact) {
        if (fact != null && !DidYouKnow.contains(fact)) {
            setDirty(DIRTY_INFO, true);
            DidYouKnow.add(fact);
        }
    }

    public void setDidYouKnow(Collection<String> facts) {
        if (facts != null && !facts.isEmpty()) {
            DidYouKnow = facts;
            setDirty(DIRTY_INFO, true);
        }
    }

    public void clearDidYouKnow() {
        DidYouKnow.clear();
        setDirty(DIRTY_INFO, true);
    }

    public void clearAwards() {
        awards.clear();
        setDirty(DIRTY_INFO, true);
    }

    public void addActor(String actor) {
        if (StringTools.isNotValidString(actor)) {
            return;
        }

        if (!cast.contains(actor.trim())) {
            setDirty(DIRTY_INFO, true);
            cast.add(actor.trim());
        }
    }

    public void addActor(String key, String name, String character, String URL, String doublage) {
        if (name != null) {
            String Name = name;
            if (name.indexOf(":") > -1) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[1])) {
                    Name = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    Name = names[0];
                }
            }
            Name = Name.trim();
            boolean found = false;
            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(Name) && p.getDepartment().equals("Actors")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                addActor(Name);
                addPerson(key, name, URL, "Actor", character, doublage);
            }
        }
    }

    public void setCast(Collection<String> cast) {
        if (cast != null && !cast.isEmpty()) {
            clearCast();
            this.cast.addAll(cast);
            Collection<Filmography> pList = new ArrayList<Filmography>();

            for (Filmography p : people) {
                if (p.getDepartment().equals("Actors")) {
                    pList.add(p);
                }
            }

            for (Filmography p : pList) {
                removePerson(p);
            }

            for (String member : cast) {
                addActor(Movie.UNKNOWN, member, Movie.UNKNOWN, Movie.UNKNOWN, Movie.UNKNOWN);
            }
        }
    }

    public void clearCast() {
        setDirty(DIRTY_INFO, true);
        cast.clear();
    }

    public void addWriter(String writer) {
        if (writer != null && !writers.contains(writer)) {
            setDirty(DIRTY_INFO, true);
            writers.add(writer);
        }
    }

    public void addWriter(String key, String name, String URL) {
        if (name != null) {
            String Name = name;
            if (name.indexOf(":") > 0) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[1])) {
                    Name = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    Name = names[0];
                }
            }

            Name = Name.trim();
            boolean found = false;

            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(Name) && p.getDepartment().equals("Writing")) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                addWriter(Name);
                addPerson(key, name, URL, "Writer");
            }
        }
    }

    public void setWriters(Collection<String> writers) {
        if (writers != null && !writers.isEmpty()) {
            clearWriters();
            this.writers.addAll(writers);
            Collection<Filmography> pList = new ArrayList<Filmography>();
            for (Filmography p : people) {
                if (p.getDepartment().equals("Writing")) {
                    pList.add(p);
                }
            }
            for (Filmography p : pList) {
                removePerson(p);
            }
            for (String member : writers) {
                addWriter(Movie.UNKNOWN, member, Movie.UNKNOWN);
            }
        }
    }

    public void clearWriters() {
        setDirty(DIRTY_INFO, true);
        writers.clear();
    }

    public void setCompany(String company) {
        if (StringUtils.isBlank(company)) {
            company = UNKNOWN;
        }
        if (!company.equalsIgnoreCase(this.company)) {
            setDirty(DIRTY_INFO, true);
            this.company = company;
        }
    }

    public void setContainer(String container) {
        if (StringUtils.isBlank(container)) {
            container = UNKNOWN;
        }
        if (!container.equalsIgnoreCase(this.container)) {
            setDirty(DIRTY_INFO, true);
            this.container = container;
        }
    }

    public void setCountry(String country) {
        if (StringUtils.isBlank(country)) {
            country = UNKNOWN;
        }
        if (!country.equalsIgnoreCase(this.country)) {
            setDirty(DIRTY_INFO, true);
            this.country = country;
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

    public void setDirectors(Collection<String> directors) {
        if (directors != null && !directors.isEmpty()) {
            clearDirectors();
            this.directors.addAll(directors);
            Collection<Filmography> pList = new ArrayList<Filmography>();
            for (Filmography p : people) {
                if (p.getDepartment().equals("Directing")) {
                    pList.add(p);
                }
            }
            for (Filmography p : pList) {
                removePerson(p);
            }
            for (String member : directors) {
                addDirector(Movie.UNKNOWN, member, Movie.UNKNOWN);
            }
        }
    }

    public void clearDirectors() {
        setDirty(DIRTY_INFO, true);
        directors.clear();
    }

    public void addDirector(String director) {
        if (director != null && !directors.contains(director)) {
            setDirty(DIRTY_INFO, true);
            directors.add(director);
        }
    }

    public void addDirector(String key, String name, String URL) {
        if (name != null) {
            String directorName = name;
            if (name.indexOf(":") > 0) {
                String[] names = name.split(":");
                if (StringTools.isValidString(names[1])) {
                    directorName = names[1];
                } else if (StringTools.isValidString(names[0])) {
                    directorName = names[0];
                }
            }

            directorName = directorName.trim();
            boolean found = false;
            for (Filmography p : people) {
                if (p.getName().equalsIgnoreCase(directorName) && p.getDepartment().equals("Directing")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                addDirector(directorName);
                addPerson(key, name, URL, "Director");
            }
        }
    }

    public void setFirst(String first) {
        if (StringUtils.isBlank(first)) {
            first = UNKNOWN;
        }
        if (!first.equalsIgnoreCase(this.first)) {
            setDirty(DIRTY_INFO, true);
            this.first = first;
        }
    }

    public void setFps(float fps) {
        //Prevent wrong result caused by floating point rounding by allowing difference of 0.1 fpsS
        if (Math.abs(fps - this.fps) > 0.1) {
            setDirty(DIRTY_INFO, true);
            this.fps = fps;
        }
    }

    public void setGenres(Collection<String> genresToAdd) {
        if (!extra) {
            // Only check if the skip list isn't empty
            if (!genreSkipList.isEmpty()) {
                // remove any unwanted genres from the new collection
                Collection<String> genresFinal = new TreeSet<String>();

                Iterator<String> genreIterator = genresToAdd.iterator();
                while (genreIterator.hasNext()) {
                    String genreToAdd = genreIterator.next();
                    if (!genreSkipList.contains(genreToAdd.toLowerCase())) {
                        genresFinal.add(genreToAdd);
                    }
                }

                // Add the trimmed genre list
                this.genres = genresFinal;
            } else {
                // No need to remove genres, so add them all
                this.genres = genresToAdd;
            }

            setDirty(DIRTY_INFO, true);
        }
    }

    public void setSets(Map<String, Integer> sets) {
        setDirty(DIRTY_INFO, true);
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
    public void setId(String key, String id) {
        if (StringTools.isValidString(key) && StringTools.isValidString(id) && !id.equalsIgnoreCase(this.getId(key))) {
            setDirty(DIRTY_INFO, true);
            this.idMap.put(key, id);
        }
    }

    public void setId(String key, int id) {
        setId(key, Integer.toString(id));
    }

    public void setGross(String country, String value) {
        if (StringTools.isValidString(country) && StringTools.isValidString(value) && !value.equalsIgnoreCase(this.getGross(country))) {
            setDirty(DIRTY_INFO, true);
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
            setDirty(DIRTY_INFO, true);
            this.openweek.put(country, value);
        }
    }

    public void setOpenWeek(Map<String, String> openweek) {
        if (openweek != null) {
            this.openweek = openweek;
        }
    }

    public void setLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            language = UNKNOWN;
        }
        if (!language.equalsIgnoreCase(this.language)) {
            setDirty(DIRTY_INFO, true);
            this.language = language;
        }
    }

    public void setCertification(String certification) {
        if (StringUtils.isBlank(certification)) {
            certification = NOTRATED;
        }
        this.certification = certification;
        setDirty(DIRTY_INFO, true);
    }

    public void setLast(String last) {
        if (StringUtils.isBlank(last)) {
            last = UNKNOWN;
        }
        if (!last.equalsIgnoreCase(this.last)) {
            setDirty(DIRTY_INFO, true);
            this.last = last;
        }
    }

    public void setMovieFiles(Collection<MovieFile> movieFiles) {
        setDirty(DIRTY_INFO, true);
        this.movieFiles = movieFiles;
    }

    public void setExtraFiles(Collection<ExtraFile> extraFiles) {
        setDirty(DIRTY_INFO, true);
        this.extraFiles = extraFiles;
    }

    public void setAwards(Collection<AwardEvent> awards) {
        setDirty(DIRTY_INFO, true);
        this.awards = awards;
    }

    public void setPeople(Collection<Filmography> people) {
        setDirty(DIRTY_INFO, true);
        this.people = people;
    }

    public void setNext(String next) {
        if (StringUtils.isBlank(next)) {
            next = UNKNOWN;
        }
        if (!next.equalsIgnoreCase(this.next)) {
            setDirty(DIRTY_INFO, true);
            this.next = next;
        }
    }

    public void setPlot(String plot) {
        if (StringUtils.isBlank(plot)) {
            plot = UNKNOWN;
        }
        if (!plot.equalsIgnoreCase(this.plot)) {
            setDirty(DIRTY_INFO, true);
            plot = plot.replaceAll("\"", "'");
            this.plot = plot;
        }
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        if (StringUtils.isBlank(outline)) {
            outline = UNKNOWN;
        }

        if (!outline.equalsIgnoreCase(this.outline)) {
            setDirty(DIRTY_INFO, true);
            outline = outline.replaceAll("\"", "'");
            this.outline = outline;
        }
    }

    public void setPrevious(String previous) {
        if (StringUtils.isBlank(previous)) {
            previous = UNKNOWN;
        }
        if (!previous.equalsIgnoreCase(this.previous)) {
            setDirty(DIRTY_INFO, true);
            this.previous = previous;
        }
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
                            boolean found = false;
                            for (String ignoreName : ratingIgnore) {
                                if (ratingSite.indexOf(ignoreName) == 0) {
                                    found = true;
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
                    setDirty(DIRTY_INFO, true);
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

    public void setReleaseDate(String releaseDate) {
        if (StringUtils.isBlank(releaseDate)) {
            releaseDate = UNKNOWN;
        }
        if (!releaseDate.equalsIgnoreCase(this.releaseDate)) {
            setDirty(DIRTY_INFO, true);
            this.releaseDate = releaseDate;
        }
    }

    public void setResolution(String resolution) {
        if (StringUtils.isBlank(resolution)) {
            resolution = UNKNOWN;
        }
        if (!resolution.equalsIgnoreCase(this.resolution)) {
            setDirty(DIRTY_INFO, true);
            this.resolution = resolution;
        }
    }

    public void setRuntime(String runtime) {
        if (StringUtils.isBlank(runtime)) {
            this.runtime = UNKNOWN;
            return;
        }

        if (!runtime.equalsIgnoreCase(this.runtime)) {
            setDirty(DIRTY_INFO, true);
            // Escape the first "0" AlloCine gives sometimes
            if (runtime.startsWith("0")) {
                this.runtime = new String(runtime.substring(1)).trim();
            } else {
                this.runtime = runtime.trim();
            }
        }
    }

    public void setSubtitles(String subtitles) {
        if (StringUtils.isBlank(subtitles)) {
            subtitles = UNKNOWN;
        }

        if (!subtitles.equals(this.subtitles)) {
            setDirty(DIRTY_INFO, true);
            this.subtitles = subtitles;
        }
    }

    public void setTitle(String name) {
        if (StringUtils.isBlank(name)) {
            name = UNKNOWN;
        }

        if (!name.equals(this.title)) {
            setDirty(DIRTY_INFO, true);
            this.title = name;

            // If we don't have a original title, then use the title
            if (StringTools.isNotValidString(originalTitle)) {
                setOriginalTitle(name);
            }
        }
    }

    public void setTitleSort(String text) {
        if (StringUtils.isBlank(text)) {
            text = UNKNOWN;
        }

        if (!text.equals(this.titleSort)) {
            int idx = 0;
            while (idx < text.length() && !Character.isLetterOrDigit(text.charAt(idx))) {
                idx++;
            }

            // Issue 1908: Replace all non-standard characters in the title sort
            this.titleSort = getStrippedTitle(StringTools.stringMapReplacement(new String(text.substring(idx))));
            setDirty(DIRTY_INFO, true);
        }
    }

    public void setOriginalTitle(String name) {
        if (StringUtils.isBlank(name)) {
            name = UNKNOWN;
        }

        if (!name.equals(this.originalTitle)) {
            setDirty(DIRTY_INFO, true);
            this.originalTitle = name;
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
            if (videoCodec.getCodecType() == Codec.CodecType.VIDEO) {
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

    public void setVideoOutput(String videoOutput) {
        if (StringUtils.isBlank(videoOutput)) {
            videoOutput = UNKNOWN;
        }
        if (!videoOutput.equalsIgnoreCase(this.videoOutput)) {
            setDirty(DIRTY_INFO, true);
            this.videoOutput = videoOutput;
        }
    }

    public void setVideoSource(String videoSource) {
        if (StringUtils.isBlank(videoSource)) {
            videoSource = UNKNOWN;
        }
        if (!videoSource.equalsIgnoreCase(this.videoSource)) {
            setDirty(DIRTY_INFO, true);
            this.videoSource = videoSource;
        }
    }

    public void setYear(String year) {
        if (StringUtils.isBlank(year)) {
            year = UNKNOWN;
        }
        if (!year.equalsIgnoreCase(this.year)) {
            setDirty(DIRTY_INFO, true);
            this.year = year;
        }
    }

    public void setBudget(String budget) {
        if (budget != null && !this.budget.equalsIgnoreCase(budget)) {
            setDirty(DIRTY_INFO, true);
            this.budget = budget;
        }
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        if (StringUtils.isBlank(quote)) {
            quote = UNKNOWN;
        }
        if (!quote.equalsIgnoreCase(this.quote)) {
            setDirty(DIRTY_INFO, true);
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
    private Long tmstmp = null; // cache value

    public long getLastModifiedTimestamp() {
        if (!isSetMaster()) {
            synchronized (this) {
                if (tmstmp == null) {
                    tmstmp = new Long(0);
                    for (MovieFile mf : getMovieFiles()) {
                        tmstmp = Math.max(tmstmp, mf.getLastModified());
                    }
                }
            }
            //make sure the fileDate is correct too
            addFileDate(new Date(tmstmp));
            return tmstmp;
        } else {
            // Set processing
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
     * Sets the "extra" flag to mark this file as an extra. Will trigger the
     * "dirty" setting too
     *
     * @param extra Boolean flag, true=extra file, false=normal file
     */
    public void setExtra(boolean extra) {
        setDirty(DIRTY_INFO, true);
        this.extra = extra;
        if (extra) {
            genres.clear();
        }
    }

    /**
     * This function will return true if the movie can have trailers.
     *
     * @param movie
     * @return
     */
    public boolean canHaveTrailers() {
        if (isExtra() || getMovieType().equals(Movie.TYPE_TVSHOW)) {
            return false;
        } else {
            return true;
        }
    }

    public void setTrailerExchange(Boolean trailerExchange) {
        if (this.trailerExchange != trailerExchange) {
            setDirty(DIRTY_INFO, true);
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
            setTrailerLastScan(dateFormat.parse(lastScan).getTime());
        } catch (Exception error) {
            setTrailerLastScan(0);
        }
    }

    /**
     * Set the date of the last trailers scan
     *
     * @param lastScan date of the last trailers scan (milliseconds offset from
     * the Epoch)
     */
    public void setTrailerLastScan(long lastScan) {
        if (lastScan != this.trailerLastScan) {
            setDirty(DIRTY_INFO, true);
            this.trailerLastScan = lastScan;
        }
    }

    /**
     * Get the date of the last trailers scan
     *
     * @return the date of the last trailers scan (milliseconds offset from the
     * Epoch)
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
        if (StringUtils.isBlank(movieType)) {
            movieType = TYPE_UNKNOWN;
        }
        if (!this.movieType.equals(movieType)) {
            setDirty(DIRTY_INFO, true);
            this.movieType = movieType;
        }
    }

    public String getMovieType() {
        return this.movieType;
    }

    public void setFormatType(String formatType) {
        if (StringUtils.isBlank(formatType)) {
            formatType = TYPE_FILE;
        }
        if (!this.formatType.equals(formatType)) {
            setDirty(DIRTY_INFO, true);
            this.formatType = formatType;
        }
    }

    public String getFormatType() {
        return this.formatType;
    }

    public void setVideoType(String videoType) {
        if (StringUtils.isBlank(videoType)) {
            videoType = UNKNOWN;
        }
        if (!this.videoType.equals(videoType)) {
            setDirty(DIRTY_INFO, true);
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

    @Deprecated
    public String getAudioChannels() {
        StringBuilder sb = new StringBuilder();
        boolean firstChannel = true;

        for (Codec codec : codecs) {
            if (codec.getCodecType().equals(Codec.CodecType.AUDIO)) {
                if (firstChannel) {
                    firstChannel = false;
                } else {
                    sb.append(SPACE_SLASH_SPACE);
                }
                sb.append(codec.getCodecChannels());
            }
        }
        return sb.toString();
    }

    @XmlTransient
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
            setDirty(DIRTY_INFO, true);
            this.top250 = top250;
        }
    }

    public String getLibraryDescription() {
        return libraryDescription;
    }

    public void setLibraryDescription(String libraryDescription) {
        if (StringUtils.isBlank(libraryDescription)) {
            libraryDescription = UNKNOWN;
        }
        if (!libraryDescription.equals(this.libraryDescription)) {
            this.libraryDescription = libraryDescription;
            setDirty(DIRTY_INFO, true);
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

    public static void addSortIgnorePrefixes(String prefix) {
        sortIgnorePrefixes.add(prefix);
    }

    /**
     * Take the passed DTO and update the movie information
     *
     * @param dto
     */
    public void mergeFileNameDTO(MovieFileNameDTO dto) {
        setTitle(dto.getTitle());
        setExtra(dto.isExtra());

        if (StringTools.isValidString(dto.getAudioCodec())) {
            addCodec(new Codec(Codec.CodecType.AUDIO, dto.getAudioCodec()));
        }
        if (StringTools.isValidString(dto.getVideoCodec())) {
            addCodec(new Codec(Codec.CodecType.VIDEO, dto.getVideoCodec()));
        }
        setVideoSource(dto.getVideoSource());
        setContainer(dto.getContainer());
        setFps(dto.getFps() > 0 ? dto.getFps() : 60);
        setMovieIds(dto.getMovieIds());

        if (dto.getSeason() > -1) {
            // Mark the movie as a TV Show
            setMovieType(Movie.TYPE_TVSHOW);
        }

        for (MovieFileNameDTO.SetDTO set : dto.getSets()) {
            addSet(set.getTitle(), set.getIndex() >= 0 ? set.getIndex() : null);
        }
        setYear(dto.getYear() > 0 ? String.valueOf(dto.getYear()) : null);
        setLanguage(dto.getLanguages().size() > 0 ? dto.getLanguages().get(0) : null);

        if (dto.getHdResolution() != null) {
            setVideoType(TYPE_VIDEO_HD);

            // Check if the videoOutput is UNKNOWN and clear it if it is
            if (StringTools.isNotValidString(videoOutput)) {
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
                    if (!videoOutput.equals("")) {
                        videoOutput += " ";
                    }
                    videoOutput += "50Hz";
                    break;
                case 59:
                    videoOutput = "1080p 59.94Hz";
                    break;
                case 60:
                    if (!videoOutput.equals("")) {
                        videoOutput += " ";
                    }
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
    @Override
    public boolean isSetMaster() {
        return isSetMaster;
    }

    public void setSetMaster(boolean isSetMaster) {
        this.isSetMaster = isSetMaster;
    }

    @XmlAttribute(name = "setSize")
    public int getSetSize() {
        return this.setSize;
    }

    public void setSetSize(final int size) {
        this.setSize = size;
    }

    /**
     * Store the latest filedate for a set of movie files. Synchronized so that
     * the comparisons don't overlap
     *
     * @param fileDate
     */
    synchronized public void addFileDate(Date fileDate) {
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
    public void setFileDate(Date fileDate) {
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

    public void setAspectRatio(String aspectRatio) {
        this.aspect = aspectRatio;
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
     * Should be called only from ArtworkScanner. Avoid calling this inside
     * MoviePlugin Also called from MovieNFOScanner
     *
     * @param url
     */
    public void setPosterURL(String url) {
        if (StringUtils.isBlank(url)) {
            url = UNKNOWN;
        }

        if (!url.equalsIgnoreCase(this.posterURL)) {
            setDirty(Movie.DIRTY_INFO, true);
            this.posterURL = url;
        }
    }

    @XmlElement(name = "posterFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getPosterFilename() {
        return posterFilename;
    }

    public void setPosterFilename(String posterFilename) {
        if (StringUtils.isBlank(posterFilename)) {
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
        if (StringUtils.isBlank(detailPosterFilename)) {
            detailPosterFilename = UNKNOWN;
        }
        this.detailPosterFilename = detailPosterFilename;
    }

    // ***** Thumbnails
    @XmlElement(name = "thumbnail")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getThumbnailFilename() {
        return this.thumbnailFilename;
    }

    public void setThumbnailFilename(String thumbnailFilename) {
        if (StringUtils.isBlank(thumbnailFilename)) {
            thumbnailFilename = UNKNOWN;
        }
        this.thumbnailFilename = thumbnailFilename;
    }

    // ***** Footer
    @XmlElement(name = "footer")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public ArrayList<String> getFooterFilename() {
        return this.footerFilename;
    }

    public void setFooterFilename(String footerFilename, Integer inx) {
        if (StringUtils.isBlank(footerFilename)) {
            footerFilename = UNKNOWN;
        } else {
            footerFilename = FileTools.makeSafeFilename(footerFilename);
        }
        if (this.footerFilename.size() <= inx) {
            while (this.footerFilename.size() < inx) {
                this.footerFilename.add(UNKNOWN);
            }
            this.footerFilename.add(footerFilename);
        } else {
            this.footerFilename.set(inx, footerFilename);
        }
    }

    // ***** Fanart
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartURL() {
        return fanartURL;
    }

    public void setFanartURL(String fanartURL) {
        if (StringUtils.isBlank(fanartURL)) {
            fanartURL = UNKNOWN;
        }

        if (!fanartURL.equalsIgnoreCase(this.fanartURL)) {
            setDirty(Movie.DIRTY_INFO, true);
            this.fanartURL = fanartURL;
        }
    }

    @XmlElement(name = "fanartFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getFanartFilename() {
        return fanartFilename;
    }

    public void setFanartFilename(String fanartFilename) {
        if (StringUtils.isBlank(fanartFilename)) {
            fanartFilename = UNKNOWN;
        }
        this.fanartFilename = fanartFilename;
    }

    // ***** Banners
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerURL() {
        return bannerURL;
    }

    public void setBannerURL(String bannerURL) {
        if (StringUtils.isBlank(bannerURL)) {
            bannerURL = UNKNOWN;
        }

        if (!bannerURL.equalsIgnoreCase(this.bannerURL)) {
            setDirty(DIRTY_INFO, true);
            this.bannerURL = bannerURL;
        }
    }

    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getBannerFilename() {
        return bannerFilename;
    }

    public void setBannerFilename(String bannerFilename) {
        if (StringUtils.isBlank(bannerFilename)) {
            bannerFilename = UNKNOWN;
        }
        this.bannerFilename = bannerFilename;
    }

    // ***** ClearLogo
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearLogoURL() {
        return clearLogoURL;
    }

    public void setClearLogoURL(String clearLogoURL) {
        if (StringUtils.isBlank(clearLogoURL)) {
            clearLogoURL = UNKNOWN;
        }

        if (!clearLogoURL.equalsIgnoreCase(this.clearLogoURL)) {
            setDirty(Movie.DIRTY_CLEARLOGO, true);
            this.clearLogoURL = clearLogoURL;
        }
    }

    @XmlElement(name = "clearLogoFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearLogoFilename() {
        return clearLogoFilename;
    }

    public void setClearLogoFilename(String clearLogoFilename) {
        if (StringUtils.isBlank(clearLogoFilename)) {
            clearLogoFilename = UNKNOWN;
        }
        this.clearLogoFilename = clearLogoFilename;
    }

    // ***** ClearArt
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearArtURL() {
        return clearArtURL;
    }

    public void setClearArtURL(String clearArtURL) {
        if (StringUtils.isBlank(clearArtURL)) {
            clearArtURL = UNKNOWN;
        }

        if (!clearArtURL.equalsIgnoreCase(this.clearArtURL)) {
            setDirty(Movie.DIRTY_CLEARART, true);
            this.clearArtURL = clearArtURL;
        }
    }

    @XmlElement(name = "clearArtFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getClearartFilename() {
        return clearArtFilename;
    }

    public void setClearArtFilename(String clearArtFilename) {
        if (StringUtils.isBlank(clearArtFilename)) {
            clearArtFilename = UNKNOWN;
        }
        this.clearArtFilename = clearArtFilename;
    }

    // ***** TvThumb
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getTvThumbURL() {
        return tvThumbURL;
    }

    public void setTvThumbURL(String tvThumbURL) {
        if (StringUtils.isBlank(tvThumbURL)) {
            tvThumbURL = UNKNOWN;
        }

        if (!tvThumbURL.equalsIgnoreCase(this.tvThumbURL)) {
            setDirty(Movie.DIRTY_TVTHUMB, true);
            this.tvThumbURL = tvThumbURL;
        }
    }

    @XmlElement(name = "tvThumbFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getTvThumbFilename() {
        return tvThumbFilename;
    }

    public void setTvThumbFilename(String tvThumbFilename) {
        if (StringUtils.isBlank(tvThumbFilename)) {
            tvThumbFilename = UNKNOWN;
        }
        this.tvThumbFilename = tvThumbFilename;
    }

    // ***** SeasonThumb
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getSeasonThumbURL() {
        return seasonThumbURL;
    }

    public void setSeasonThumbURL(String seasonThumbURL) {
        if (StringUtils.isBlank(seasonThumbURL)) {
            seasonThumbURL = UNKNOWN;
        }

        if (!seasonThumbURL.equalsIgnoreCase(this.seasonThumbURL)) {
            setDirty(Movie.DIRTY_SEASONTHUMB, true);
            this.seasonThumbURL = seasonThumbURL;
        }
    }

    @XmlElement(name = "seasonThumbFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getSeasonThumbFilename() {
        return seasonThumbFilename;
    }

    public void setSeasonThumbFilename(String seasonThumbFilename) {
        if (StringUtils.isBlank(seasonThumbFilename)) {
            seasonThumbFilename = UNKNOWN;
        }
        this.seasonThumbFilename = seasonThumbFilename;
    }

    // ***** CDArt
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getCdArtURL() {
        return cdArtURL;
    }

    public void setCdArtURL(String cdArtURL) {
        if (StringUtils.isBlank(cdArtURL)) {
            cdArtURL = UNKNOWN;
        }

        if (!cdArtURL.equalsIgnoreCase(this.cdArtURL)) {
            setDirty(Movie.DIRTY_CDART, true);
            this.cdArtURL = cdArtURL;
        }
    }

    @XmlElement(name = "cdArtFile")
    @XmlJavaTypeAdapter(UrlCodecAdapter.class)
    public String getCdArtFilename() {
        return cdArtFilename;
    }

    public void setCdArtFilename(String cdArtFilename) {
        if (StringUtils.isBlank(cdArtFilename)) {
            cdArtFilename = UNKNOWN;
        }
        this.cdArtFilename = cdArtFilename;
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

    @XmlTransient
    public boolean isOverrideYear() {
        return overrideYear;
    }

    public void setOverrideYear(boolean overrideYear) {
        this.overrideYear = overrideYear;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        if (StringUtils.isBlank(tagline)) {
            tagline = UNKNOWN;
        }

        if (!tagline.equalsIgnoreCase(this.tagline)) {
            setDirty(DIRTY_INFO, true);
            this.tagline = tagline;
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
            return new DateTime(returnDate).toString(dateFormatLongString);
        }
    }

    /**
     * Look at the associated movie files and return the latest date a file was
     * watched
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

    /**
     * @return the artwork
     */
    public Set<Artwork> getArtwork() {
        return artwork;
    }

    /**
     * @param artwork the artwork to set
     */
    public void setArtwork(Set<Artwork> artwork) {
        this.artwork = artwork;
    }

    public void addArtwork(Artwork artwork) {
        //TODO: Check to see if the artwork source/type/url exists and add to it rather than overwrite or append to the list
        this.artwork.add(artwork);
    }

    /**
     * Check to see if an artwork already exists.
     *
     * @param artwork
     * @return
     */
    @SuppressWarnings("unused")
    private Artwork artworkExists(Artwork artwork) {
        for (Artwork artworkTest : getArtwork(artwork.getType())) {
            if (artworkTest.equals(artwork)) {
                return artworkTest;
            }
        }
        return null;
    }

    public Collection<Artwork> getArtwork(ArtworkType artworkType) {
        Collection<Artwork> artworkList = new LinkedHashSet<Artwork>();

        for (Artwork tempArtwork : this.artwork) {
            if (tempArtwork.getType() == artworkType) {
                artworkList.add(tempArtwork);
            }
        }

        return artworkList;
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
        newMovie.overrideTitle = aMovie.overrideTitle;
        newMovie.overrideYear = aMovie.overrideYear;
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
        newMovie.isSetMaster = aMovie.isSetMaster;
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
        newMovie.artwork = new LinkedHashSet<Artwork>(aMovie.artwork);
        newMovie.indexes = new HashMap<String, String>(aMovie.indexes);
        newMovie.movieFiles = new TreeSet<MovieFile>(aMovie.movieFiles);
        newMovie.extraFiles = new TreeSet<ExtraFile>(aMovie.extraFiles);
        newMovie.dirtyFlags = new HashMap<String, Boolean>(aMovie.dirtyFlags);
        newMovie.codecs = new LinkedHashSet<Codec>(aMovie.codecs);
        newMovie.footerFilename = new ArrayList<String>(aMovie.footerFilename);

        return newMovie;
    }

    public Set<Codec> getCodecs() {
        return codecs;
    }

    public void setCodecs(Set<Codec> codecs) {
        this.codecs = codecs;
        setDirty(DIRTY_INFO, true);
    }

    public void addCodec(Codec codec) {
        // Check to see if the codec already exists
        boolean alreadyExists = false;
        for (Codec existingCodec : codecs) {
            if (existingCodec.equals(codec)) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            this.codecs.add(codec);
            setDirty(DIRTY_INFO, true);
        }
    }
}
