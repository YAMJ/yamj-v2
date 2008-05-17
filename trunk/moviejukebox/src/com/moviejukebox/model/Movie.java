package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class Movie implements Comparable<Movie> {
	
	private static String UNKNOWN = "UNKNOWN";
	
	// Movie properties
	private String id = UNKNOWN;
	private String title = UNKNOWN;
	private String titleSort = UNKNOWN;
	private String year = UNKNOWN;
	private String releaseDate = UNKNOWN;
	private String rating = UNKNOWN;
	private String posterURL = UNKNOWN;
	private String posterFile = UNKNOWN;
	private String plot = UNKNOWN;
	private String director = UNKNOWN;
	private String country = UNKNOWN;
	private String company = UNKNOWN;
	private String runtime = UNKNOWN;

	private String first = UNKNOWN;
	private String previous = UNKNOWN;
	private String next = UNKNOWN;
	private String Last = UNKNOWN;
	
	private int season = -1;

	private Collection<String> genres = new ArrayList<String>();
	private Collection<String> casting = new ArrayList<String>();
	
	// Media file properties
	Collection<MovieFile> movieFiles = new TreeSet<MovieFile>();
	private String language = UNKNOWN;
	private boolean hasSubtitles = false;

	private String container = UNKNOWN;  // AVI, MKV, TS, etc.
	private String videoCodec = UNKNOWN; // DIVX, XVID, H.264, etc.
	private String audioCodec = UNKNOWN; // MP3, AC3, DTS, etc.
	private String resolution = UNKNOWN; // 1280x528
	private String videoSource = UNKNOWN;
	private String videoOutput = UNKNOWN;
	private int fps = 60;

	private String baseName;

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean hasSubtitles() {
		return hasSubtitles;
	}

	public void setSubtitles(boolean hasSubtitles) {
		this.hasSubtitles = hasSubtitles;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public String getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}

	public String getAudioCodec() {
		return audioCodec;
	}

	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getVideoSource() {
		return videoSource;
	}

	public void setVideoSource(String videoSource) {
		this.videoSource = videoSource;
	}

	public String getVideoOutput() {
		return videoOutput;
	}

	public void setVideoOutput(String videoOutput) {
		this.videoOutput = videoOutput;
	}

	public int getFps() {
		return fps;
	}

	public void setFps(int fps) {
		this.fps = fps;
	}

	public String getPosterURL() {
		return posterURL;
	}

	public void setPosterURL(String url) {
		this.posterURL = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String name) {
		this.title = name;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public Collection<String> getCasting() {
		return casting;
	}

	public void setCasting(Collection<String> casting) {
		this.casting = casting;
	}

	public String getPlot() {
		return plot;
	}

	public void setPlot(String plot) {
		this.plot = plot;
	}

	public void setDirector(String director) {
		this.director = director;
	}

	public String getDirector() {
		return director;
	}

	public void addGenre(String genre) {
		genres.add(genre);
	}

	public Collection<String> getGenres() {
		return genres;
	}

	public String getTitleSort() {
		return titleSort;
	}

	public void setTitleSort(String titleSort) {
		this.titleSort = titleSort;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public Collection<MovieFile> getFiles() {
		return movieFiles;
	}

	public void addMovieFile(MovieFile movieFile) {
		this.movieFiles.add(movieFile);
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime;
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
	

	public int getSeason() {
		return season;
	}

	public void setSeason(int season) {
		this.season = season;
	}

	public boolean isTVShow() {
		return (season!= -1);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("[Movie ");
		sb.append("[id=").append(id).append("]");
		sb.append("[title=").append(title).append("]");
		sb.append("[titleSort=").append(titleSort).append("]");
		sb.append("[year=").append(year).append("]");
/*		sb.append("[releaseDate=").append(releaseDate).append("]");
		sb.append("[rating=").append(rating).append("]");
		sb.append("[posterURL=").append(posterURL).append("]");
		sb.append("[posterFile=").append(posterFile).append("]");
		sb.append("[plot=").append(plot).append("]");
		sb.append("[director=").append(director).append("]");
		sb.append("[country=").append(country).append("]");
		sb.append("[company=").append(company).append("]");
		sb.append("[runtime=").append(runtime).append("]");
		sb.append("[season=").append(1).append("]");
		sb.append("[language=").append(language).append("]");
		sb.append("[hasSubtitles=").append(hasSubtitles).append("]");
		sb.append("[container=").append(container).append("]");  // AVI, MKV, TS, etc.
		sb.append("[videoCodec=").append(videoCodec).append("]"); // DIVX, XVID, H.264, etc.
		sb.append("[audioCodec=").append(audioCodec).append("]"); // MP3, AC3, DTS, etc.
		sb.append("[resolution=").append(resolution).append("]"); // 1280x528
		sb.append("[videoSource=").append(videoSource).append("]");
		sb.append("[videoOutput=").append(videoOutput).append("]");
		sb.append("[fps=").append(fps).append("]"); */
		sb.append("]");
		return sb.toString();
	}

	public static String getUNKNOWN() {
		return UNKNOWN;
	}

	public static void setUNKNOWN(String unknown) {
		UNKNOWN = unknown;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public String getLast() {
		return Last;
	}

	public void setLast(String last) {
		Last = last;
	}

	public Collection<MovieFile> getMovieFiles() {
		return movieFiles;
	}

	public void setMovieFiles(Collection<MovieFile> movieFiles) {
		this.movieFiles = movieFiles;
	}

	public boolean isHasSubtitles() {
		return hasSubtitles;
	}

	public void setHasSubtitles(boolean hasSubtitles) {
		this.hasSubtitles = hasSubtitles;
	}

	public void setGenres(Collection<String> genres) {
		this.genres = genres;
	}

	public void setBaseName(String filename) {
		this.baseName = filename;
	}
	
	public String getBaseName() {
		return baseName;
	}

	@Override
	public int compareTo(Movie anotherMovie) {
/*		String a = this.getTitleSort().equalsIgnoreCase("UNKNOWN")?this.getTitle():this.getTitleSort();
		String b = anotherMovie.getTitleSort().equalsIgnoreCase("UNKNOWN")?anotherMovie.getTitle():anotherMovie.getTitleSort();
		return a.compareTo(b); */
		return this.getTitle().compareTo(anotherMovie.getTitle());
	}
}
