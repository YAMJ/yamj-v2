package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of parsed data from movie file name.
 * DTO. No methods. Only getters/setters.
 * 
 * Contains only information which could be possibly extracted from file name.
 * 
 * @author Artem.Gratchev
 */
public class MovieFileNameDTO {
	private String title = null;
	private int year = -1;
	private String partTitle = null;
	private String episodeTitle = null;
	private int season = -1;
	private final List<Integer> episodes = new ArrayList<Integer>();
	private int part = -1;
	private boolean trailer = false;
	private String trailerTitle = null;
	private String audioCodec = null;
	private String videoCodec = null;
	private String container = null;
	private String extension = null;
	private int fps = -1;
	private String hdResolution = null;
	private String videoSource = null;
	public static class Set {
		private String title = null;
		private int index = -1;
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
	}
	private final List<Set> sets = new ArrayList<Set>();
	private final List<String> languages = new ArrayList<String>();
	

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public String getPartTitle() {
		return partTitle;
	}
	public void setPartTitle(String partTitle) {
		this.partTitle = partTitle;
	}
	public int getSeason() {
		return season;
	}
	public void setSeason(int season) {
		this.season = season;
	}
	public int getPart() {
		return part;
	}
	public void setPart(int part) {
		this.part = part;
	}
	public List<Integer> getEpisodes() {
		return episodes;
	}
	public boolean isTrailer() {
		return trailer;
	}
	public void setTrailer(boolean trailer) {
		this.trailer = trailer;
	}
	public String getTrailerTitle() {
		return trailerTitle;
	}
	public void setTrailerTitle(String trailerTitle) {
		this.trailerTitle = trailerTitle;
	}
	public String getAudioCodec() {
		return audioCodec;
	}
	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}
	public String getContainer() {
		return container;
	}
	public void setContainer(String container) {
		this.container = container;
	}
	public String getExtension() {
		return extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	public int getFps() {
		return fps;
	}
	public void setFps(int fps) {
		this.fps = fps;
	}
	public String getVideoCodec() {
		return videoCodec;
	}
	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}
	public String getHdResolution() {
		return hdResolution;
	}
	public void setHdResolution(String hdResolution) {
		this.hdResolution = hdResolution;
	}
	public String getVideoSource() {
		return videoSource;
	}
	public void setVideoSource(String videoSource) {
		this.videoSource = videoSource;
	}
	public List<Set> getSets() {
		return sets;
	}
	public List<String> getLanguages() {
		return languages;
	}
	public String getEpisodeTitle() {
		return episodeTitle;
	}
	public void setEpisodeTitle(String episodeTitle) {
		this.episodeTitle = episodeTitle;
	}
}
