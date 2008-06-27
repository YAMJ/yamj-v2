package com.moviejukebox.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Movie bean
 * @author jjulien
 */
public class Movie implements Comparable<Movie> {
	
	private static String UNKNOWN = "UNKNOWN";
    private static ArrayList< String > sortIgnorePrefixes = new ArrayList< String >();
    
    public static void setup( Properties props )
    {
    	String temp = props.getProperty( "sorting.strip.prefixes" );
        if ( temp == null )
        	return;
        
        StringTokenizer st = new StringTokenizer( temp, "," );
        while ( st.hasMoreTokens())
        {
        	String token = st.nextToken();
        	if (token.startsWith("\"") && token.endsWith("\""))
        		token = token.substring(1,token.length()-1);
        	sortIgnorePrefixes.add( token );
        }
    }
	
	private String baseName;
	
	// Movie properties
	private String id = UNKNOWN;
	private String title = UNKNOWN;
	private String titleSort = UNKNOWN;
	private String year = UNKNOWN;
	private String releaseDate = UNKNOWN;
	private int rating = -1;
	private String posterURL = UNKNOWN;
	private String posterFilename = UNKNOWN;
        private String detailPosterFilename = UNKNOWN;
	private String thumbnailFilename = UNKNOWN;
	private String plot = UNKNOWN;
	private String director = UNKNOWN;
	private String country = UNKNOWN;
	private String company = UNKNOWN;
	private String runtime = UNKNOWN;
	private String language = UNKNOWN;
	private int season = -1;
	private boolean hasSubtitles = false;
	private Collection<String> genres = new ArrayList<String>();
	private Collection<String> cast = new ArrayList<String>();
	private String container = UNKNOWN;  // AVI, MKV, TS, etc.
	private String videoCodec = UNKNOWN; // DIVX, XVID, H.264, etc.
	private String audioCodec = UNKNOWN; // MP3, AC3, DTS, etc.
	private String resolution = UNKNOWN; // 1280x528
	private String videoSource = UNKNOWN;
	private String videoOutput = UNKNOWN;
	private int fps = 60;
	private String certification = UNKNOWN;

	// Navigation data
	private String first = UNKNOWN;
	private String previous = UNKNOWN;
	private String next = UNKNOWN;
	private String last = UNKNOWN;
	
	// Media file properties
	Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();

	// Caching
	private boolean isDirty = false;

	private File file;
	
	public void addGenre(String genre) {
		this.isDirty = true;
		genres.add(genre);
	}

	public void addMovieFile(MovieFile movieFile) {
		this.isDirty = true;
		this.movieFiles.add(movieFile);
	}

	
	public String getStrippedTitleSort()
	{
		String text = titleSort;
		
		// Remove configured prefixed and append to the end
		for ( String prefix : sortIgnorePrefixes )
		{ 
			if ( text.startsWith(prefix))
			{
				return (text.substring(prefix.length()) + ", " + prefix).trim();
			}
		}
		return text;
	}
	
	@Override
	public int compareTo(Movie anotherMovie) {
		return this.getStrippedTitleSort().compareToIgnoreCase( anotherMovie.getStrippedTitleSort());
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

	public String getCertification() {
		return certification;
	}

	public String getFirst() {
		return first;
	}

	public MovieFile getFirstFile() {
		
		for (MovieFile part : movieFiles) {
			if (part.getPart() == 1) 
				return part;
		}

		Iterator<MovieFile> i = movieFiles.iterator();
		if (i.hasNext())
			return i.next();
		else 
			return null;
	}

	public int getFps() {
		return fps;
	}

	public Collection<String> getGenres() {
		return genres;
	}

	public String getId() {
		return id;
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

	public boolean isHasSubtitles() {
		return hasSubtitles;
	}

	public boolean isTVShow() {
		return (season!= -1);
	}

	public void setAudioCodec(String audioCodec) {
		if (!audioCodec.equalsIgnoreCase(this.audioCodec)) {
			this.isDirty = true;
			this.audioCodec = audioCodec;
		}
	}

	public void setBaseName(String filename) {
		if (!filename.equalsIgnoreCase(baseName)) {
			this.isDirty = true;
			this.baseName = filename;
		}
	}

	public void addActor(String actor) {
		this.isDirty = true;
		cast.add(actor);
	}

	public void setCast(Collection<String> cast) {
		this.isDirty = true;
		this.cast = cast;
	}

	public void setCompany(String company) {
		if (!company.equalsIgnoreCase(this.company)) {
			this.isDirty = true;
			this.company = company;
		}
	}

	public void setContainer(String container) {
		if (!container.equalsIgnoreCase(this.container)) {
			this.isDirty = true;
			this.container = container;
		}
	}

	public void setCountry(String country) {
		if (!country.equalsIgnoreCase(this.country)) {
			this.isDirty = true;
			this.country = country;
		}
	}

	public void setDirector(String director) {
		if (!director.equalsIgnoreCase(this.director)) {
			this.isDirty = true;
			this.director = director;
		}
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

	public void setFirst(String first) {
		if (!first.equalsIgnoreCase(this.first)) {
			this.isDirty = true;
			this.first = first;
		}
	}

	public void setFps(int fps) {
		if (fps != this.fps) {
			this.isDirty = true;
			this.fps = fps;
		}
	}

	public void setGenres(Collection<String> genres) {
		this.isDirty = true;
		this.genres = genres;
	}

	public void setId(String id) {
		if (!id.equalsIgnoreCase(this.id)) {
			this.isDirty = true;
			this.id = id;
		}
	}
	
	public void setLanguage(String language) {
		if (!language.equalsIgnoreCase(this.language)) {
			this.isDirty = true;
			this.language = language;
		}
	}
	
	public void setCertification(String certification) {
		this.certification = certification;
		this.isDirty = true;
	}

	public void setLast(String last) {
		if (!last.equalsIgnoreCase(this.last)) {
			this.isDirty = true;
			this.last = last;
		}
	}

	public void setMovieFiles(Collection<MovieFile> movieFiles) {
		this.isDirty = true;
		this.movieFiles = movieFiles;
	}

	public void setNext(String next) {
		if (!next.equalsIgnoreCase(this.next)) {
			this.isDirty = true;
			this.next = next;
		}
	}

	public void setPlot(String plot) {
		if (!plot.equalsIgnoreCase(this.plot)) {
			this.isDirty = true;
			this.plot = plot;
		}
	}

	public void setPosterURL(String url) {
		if (!url.equalsIgnoreCase(this.posterURL)) {
			this.isDirty = true;
			this.posterURL = url;
		}
	}

	public void setPrevious(String previous) {
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
		if (!releaseDate.equalsIgnoreCase(this.releaseDate)) {
			this.isDirty = true;
			this.releaseDate = releaseDate;
		}
	}

	public void setResolution(String resolution) {
		if (!resolution.equalsIgnoreCase(this.resolution)) {
			this.isDirty = true;
			this.resolution = resolution;
		}
	}

	public void setRuntime(String runtime) {
		if ((runtime != null) && !runtime.equalsIgnoreCase(this.runtime)) {
			this.isDirty = true;
			this.runtime = runtime;
		}
	}

	public void setSeason(int season) {
		if (season != this.season) {
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
		if (!name.equalsIgnoreCase(this.title)) {
			this.isDirty = true;
			this.title = name;
			
			setTitleSort( name );
		}
	}

	public void setTitleSort( String text )
	{
		if ( !text.equalsIgnoreCase(this.titleSort ))
		{
			//  Automatically remove enclosing quotes
			if (( text.charAt(0) == '"' ) && ( text.charAt(text.length()-1) == '"'))
				text = text.substring(1,text.length()-1);
			
			this.titleSort = text;
			this.isDirty = true;
		}
	}
	
	public void setVideoCodec(String videoCodec) {
		if (!videoCodec.equalsIgnoreCase(this.videoCodec)) {
			this.isDirty = true;
			this.videoCodec = videoCodec;
		}
	}

	public void setVideoOutput(String videoOutput) {
		if (!videoOutput.equalsIgnoreCase(this.videoOutput)) {
			this.isDirty = true;
			this.videoOutput = videoOutput;
		}
	}

	public void setVideoSource(String videoSource) {
		if (!videoSource.equalsIgnoreCase(this.videoSource)) {
			this.isDirty = true;
			this.videoSource = videoSource;
		}
	}
	
	public void setYear(String year) {
		if (!year.equalsIgnoreCase(this.year)) {
			this.isDirty = true;
			this.year = year;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[Movie ");
		sb.append("[id=").append(id).append("]");
		sb.append("[title=").append(title).append("]");
		sb.append("[titleSort=").append(titleSort).append("]");
		sb.append("[year=").append(year).append("]");
		sb.append("[releaseDate=").append(releaseDate).append("]");
		sb.append("[rating=").append(rating).append("]");
		sb.append("[posterURL=").append(posterURL).append("]");
		sb.append("[plot=").append(plot).append("]");
		sb.append("[director=").append(director).append("]");
		sb.append("[country=").append(country).append("]");
		sb.append("[company=").append(company).append("]");
		sb.append("[runtime=").append(runtime).append("]");
		sb.append("[season=").append(season).append("]");
		sb.append("[language=").append(language).append("]");
		sb.append("[hasSubtitles=").append(hasSubtitles).append("]");
		sb.append("[container=").append(container).append("]");  // AVI, MKV, TS, etc.
		sb.append("[videoCodec=").append(videoCodec).append("]"); // DIVX, XVID, H.264, etc.
		sb.append("[audioCodec=").append(audioCodec).append("]"); // MP3, AC3, DTS, etc.
		sb.append("[resolution=").append(resolution).append("]"); // 1280x528
		sb.append("[videoSource=").append(videoSource).append("]");
		sb.append("[videoOutput=").append(videoOutput).append("]");
		sb.append("[fps=").append(fps).append("]");
		sb.append("[certification=").append(certification).append("] ");
		sb.append("[cast=").append(cast).append("] ");
		sb.append("]");
		return sb.toString();
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getThumbnailFilename() {
		return thumbnailFilename;
	}

	public void setThumbnailFilename(String thumbnailFilename) {
		this.thumbnailFilename = thumbnailFilename;
	}

	public String getPosterFilename() {
		return posterFilename;
	}

	public void setPosterFilename(String posterFilename) {
		this.posterFilename = posterFilename;
	}

        public String getDetailPosterFilename() {
            return detailPosterFilename;
        }

        public void setDetailPosterFilename(String detailPosterFilename) {
            this.detailPosterFilename = detailPosterFilename;
        }
}
