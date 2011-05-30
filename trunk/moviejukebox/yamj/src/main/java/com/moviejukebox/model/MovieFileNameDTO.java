/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.moviejukebox.model.Movie.MovieId;

/**
 * Container of parsed data from movie file name.
 * DTO. No methods. Only getters/setters.
 * 
 * Contains only information which could be possibly extracted from file name.
 * 
 * @author Artem.Gratchev
 */
@XmlType public class MovieFileNameDTO {
    private String  title = null;
    private int     year = -1;
    private String  partTitle = null;
    private String  episodeTitle = null;
    private int     season = -1;
    private final List<Integer> episodes = new ArrayList<Integer>();
    private int     part = -1;
    private boolean extra = false;
    private String  audioCodec = null;
    private String  videoCodec = null;
    private String  container = null;
    private String  extension = null;
    private int     fps = -1;
    private String  hdResolution = null;
    private String  videoSource = null;
    private Map<String, String> idMap = new HashMap<String, String>(2);

    @XmlType public static class SetDTO {
        private String title = null;
        private int index = -1;

        @XmlValue
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @XmlAttribute
        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private final List<SetDTO> sets = new ArrayList<SetDTO>();
    private final List<String> languages = new ArrayList<String>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    @XmlAttribute
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

    @XmlAttribute
    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    @XmlAttribute
    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public List<Integer> getEpisodes() {
        return episodes;
    }

    @XmlAttribute
    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean extra) {
        this.extra = extra;
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

    @XmlAttribute
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @XmlAttribute
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

    @XmlElement public List<SetDTO> getSets() {
        return sets;
    }

    @XmlElement public List<String> getLanguages() {
        return languages;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle;
    }
    
    public void setId(String key, String id) {
        if (key != null && id != null && !id.equalsIgnoreCase(this.getId(key))) {
            this.idMap.put(key, id);
        }
    }

    public String getId(String key) {
        String result = idMap.get(key);
        if (result != null) {
            return result;
        } else {
            return Movie.UNKNOWN;
        }
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }

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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Title=" + title + "]");
        sb.append("[Year=" + year + "]");
        sb.append("[Parttitle=" + partTitle + "]");
        sb.append("[EpisodeTitle=" + episodeTitle + "]");
        sb.append("[Season=" + season + "]");
        sb.append("[EpisodeCount=" + episodes.size() + "]");
        sb.append("[Part=" + part + "]");
        sb.append("[Extra=" + extra + "]");
        sb.append("[AudioCodec=" + audioCodec+ "]");
        sb.append("[VideoCodec=" + videoCodec+ "]");
        sb.append("[Container=" + container+ "]");
        sb.append("[Extension=" + extension + "]");
        sb.append("[Fps=" + fps+ "]");
        sb.append("[hdResolution=" + hdResolution+ "]");
        sb.append("[VideoSource=" + videoSource + "]");
        return sb.toString();
    }
}
