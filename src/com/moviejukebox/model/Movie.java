package com.moviejukebox.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.FileTools;

/**
 * Movie bean
 * 
 * @author jjulien
 */
public class Movie implements Comparable<Movie> {

    private static Logger logger = Logger.getLogger("moviejukebox");
    public static String UNKNOWN = "UNKNOWN";
    public static String NOTRATED = "Not Rated";
    public static String TYPE_MOVIE = "MOVIE";
    public static String TYPE_TVSHOW = "TVSHOW";
    public static String TYPE_UNKNOWN = "UNKNOWN";
    private static ArrayList<String> sortIgnorePrefixes = new ArrayList<String>();

    static {
        String temp = PropertiesUtil.getProperty("sorting.strip.prefixes");
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                sortIgnorePrefixes.add(token.toLowerCase());
            }
        }
    }
    private String baseName;
    // Movie properties
    private Map<String, String> idMap = new HashMap<String, String>(2);
    private String title = UNKNOWN;
    private String titleSort = UNKNOWN;
    private String originalTitle = UNKNOWN;
    private String year = UNKNOWN;
    private String releaseDate = UNKNOWN;
    private int rating = -1;
    private String posterURL = UNKNOWN;
    private String posterSubimage = UNKNOWN;
    private String fanartURL = UNKNOWN;
    private String posterFilename = UNKNOWN;
    private String detailPosterFilename = UNKNOWN;
    private String thumbnailFilename = UNKNOWN;
    private String fanartFilename = UNKNOWN;
    private String plot = UNKNOWN;
    private String outline = UNKNOWN;
    private String director = UNKNOWN;
    private String country = UNKNOWN;
    private String company = UNKNOWN;
    private String runtime = UNKNOWN;
    private String language = UNKNOWN;
    private int season = -1;
    private boolean hasSubtitles = false;
    private Collection<String> genres = new TreeSet<String>();
    private Collection<String> cast = new ArrayList<String>();
    private String container = UNKNOWN; // AVI, MKV, TS, etc.
    private String videoCodec = UNKNOWN; // DIVX, XVID, H.264, etc.
    private String audioCodec = UNKNOWN; // MP3, AC3, DTS, etc.
    private String audioChannels = UNKNOWN; // Number of audio channels
    private String resolution = UNKNOWN; // 1280x528
    private String videoSource = UNKNOWN;
    private String videoOutput = UNKNOWN;
    private float fps = 60;
    private String certification = UNKNOWN;
    private boolean trailer = false;
    private boolean trailerExchange = false;
    private String libraryPath = UNKNOWN;
    private String movieType = TYPE_MOVIE;
    private boolean overrideTitle = false;
    private int top250 = -1;
    private String libraryDescription = UNKNOWN;
    private long prebuf = -1;
    // Navigation data
    private String first = UNKNOWN;
    private String previous = UNKNOWN;
    private String next = UNKNOWN;
    private String last = UNKNOWN;
    // Media file properties
    Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();
    Collection<TrailerFile> trailerFiles = new TreeSet<TrailerFile>();
    // Caching
    private boolean isDirty = false;
    private boolean isDirtyNFO = false;
    private boolean isDirtyPoster = false;
    private boolean isDirtyFanart = false;
    private File file;
    private File containerFile;

    public void addGenre(String genre) {
        if (genre != null && !trailer) {
            this.isDirty = true;
            logger.finest("Genre added : " + genre);
            genres.add(genre);
        }
    }

    public void addMovieFile(MovieFile movieFile) {
        if (movieFile != null) {
            this.isDirty = true;
            // always replace MovieFile
            for (MovieFile mf : this.movieFiles) {
                if (mf.compareTo(movieFile) == 0) {
                    movieFile.setFile(mf.getFile());
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

    public void addTrailerFile(TrailerFile movieFile) {
        if (movieFile != null) {
            this.isDirty = true;
            // always replace MovieFile
            this.trailerFiles.remove(movieFile);
            this.trailerFiles.add(movieFile);
        }
    }

    public boolean hasNewTrailerFiles() {
        for (MovieFile movieFile : trailerFiles) {
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
        if (season > 0) {
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

    public String getBaseName() {
        return baseName;
    }

    public Collection<String> getCast() {
        return cast;
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

    public Collection<TrailerFile> getTrailerFiles() {
        return trailerFiles;
    }

    public String getCertification() {
        return certification;
    }

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

    public Collection<String> getGenres() {
        return genres;
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

    public String getLanguage() {
        return language;
    }

    public String getLast() {
        return last;
    }

    public Collection<MovieFile> getMovieFiles() {
        return movieFiles;
    }

    public String getNext() {
        return next;
    }

    public String getPlot() {
        return plot;
    }

    public String getPosterURL() {
        return posterURL;
    }

    public String getPosterSubimage() {
        return posterSubimage;
    }

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

    public String getRuntime() {
        return runtime;
    }

    public int getSeason() {
        return season;
    }

    public String getTitle() {
        return title;
    }

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

    public String getYear() {
        return year;
    }

    public boolean hasSubtitles() {
        return hasSubtitles;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean isDirtyNFO() {
        return isDirtyNFO;
    }

    public boolean isDirtyPoster() {
        return isDirtyPoster;
    }

    public boolean isDirtyFanart() {
        return isDirtyFanart;
    }

    public boolean isHasSubtitles() {
        return hasSubtitles;
    }

    public boolean isTVShow() {
        // return (season != -1);
        return (this.movieType.equals(TYPE_TVSHOW) || this.season > 0);
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
        this.cast = cast;
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
        if (!trailer) {
            this.isDirty = true;
            this.genres = genres;
        }
    }

    /**
     * 
     * @deprecated replaced by setId(String key, String id). This method is kept for compatibility purpose. But you should use setId(String key, String id)
     *             instead. Ex: movie.setId(ImdbPlugin.IMDB_PLUGIN_ID,"tt12345") {@link setId(String key, String id)}
     */
    @Deprecated
    public void setId(String id) {
        if (id != null) {
            setId(ImdbPlugin.IMDB_PLUGIN_ID, id);
        }
    }

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

    public void setTrailerFiles(Collection<TrailerFile> trailerFiles) {
        this.isDirty = true;
        this.trailerFiles = trailerFiles;
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
            this.outline = outline;
        }
    }

    public void setPosterURL(String url) {
        if (url == null) {
            url = UNKNOWN;
        }
        if (!url.equalsIgnoreCase(this.posterURL)) {
            this.isDirty = true;
            this.posterURL = url;
        }
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
            if (season > 0) {
                this.setMovieType(Movie.TYPE_TVSHOW);
            }
            this.isDirty = true;
            this.season = season;
        }
    }

    public void setSubtitles(boolean hasSubtitles) {
        if (hasSubtitles != this.hasSubtitles) {
            this.isDirty = true;
            this.hasSubtitles = hasSubtitles;
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

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
        sb.append("[plot=").append(plot).append("]");
        sb.append("[outline=").append(outline).append("]");
        sb.append("[director=").append(director).append("]");
        sb.append("[country=").append(country).append("]");
        sb.append("[company=").append(company).append("]");
        sb.append("[runtime=").append(runtime).append("]");
        sb.append("[season=").append(season).append("]");
        sb.append("[language=").append(language).append("]");
        sb.append("[hasSubtitles=").append(hasSubtitles).append("]");
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
        sb.append("[genres=").append(genres).append("]");
        sb.append("[libraryDescription=").append(libraryDescription).append("]");
        sb.append("[prebuf=").append(prebuf).append("]");
        sb.append("]");
        return sb.toString();
    }

    public void setThumbnailFilename(String thumbnailFilename) {
        if (thumbnailFilename == null) {
            thumbnailFilename = UNKNOWN;
        }
        this.thumbnailFilename = thumbnailFilename;
    }

    public String getThumbnailFilename() {
        return this.thumbnailFilename;
    }

    public String getPosterFilename() {
        return posterFilename;
    }

    public void setPosterFilename(String posterFilename) {
        if (posterFilename == null) {
            posterFilename = UNKNOWN;
        }
        this.posterFilename = posterFilename;
    }

    public String getDetailPosterFilename() {
        return detailPosterFilename;
    }

    public void setDetailPosterFilename(String detailPosterFilename) {
        if (detailPosterFilename == null) {
            detailPosterFilename = UNKNOWN;
        }
        this.detailPosterFilename = detailPosterFilename;
    }

    public void setTrailer(boolean trailer) {
        this.isDirty = true;
        this.trailer = trailer;
        if (trailer) {
            genres.clear();
        }
    }

    public void setTrailerExchange(boolean trailerExchange) {
        this.isDirty = true;
        this.trailerExchange = trailerExchange;
    }

    public boolean isTrailer() {
        return trailer;
    }

    public boolean isTrailerExchange() {
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

    public String getFanartFilename() {
        return fanartFilename;
    }

    public void setFanartFilename(String fanartFilename) {
        if (fanartFilename == null) {
            fanartFilename = UNKNOWN;
        }
        this.fanartFilename = fanartFilename;
    }

    public String getFanartURL() {
        return fanartURL;
    }

    public void setFanartURL(String fanartURL) {
        if (fanartURL == null) {
            fanartURL = UNKNOWN;
        }
        this.fanartURL = fanartURL;
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
}
