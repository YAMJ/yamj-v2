/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.moviejukebox.model.Movie.MovieId;

/**
 * Container of parsed data from movie file name.
 *
 * DTO. No methods. Only getters/setters.
 *
 * Contains only information which could be possibly extracted from file name.
 *
 * @author Artem.Gratchev
 */
@XmlType
public class MovieFileNameDTO {

    private String title = null;
    private int year = -1;
    private String partTitle = null;
    private String episodeTitle = null;
    private int season = -1;
    private final List<Integer> episodes = new ArrayList<>();
    private int part = -1;
    private boolean extra = false;
    private String audioCodec = null;
    private String videoCodec = null;
    private String container = null;
    private String extension = null;
    private int fps = -1;
    private String hdResolution = null;
    private String videoSource = null;
    private final Map<String, String> idMap = new HashMap<>(2);

    @XmlType
    public static class SetDTO {

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
    private final List<SetDTO> sets = new ArrayList<>();
    private final List<String> languages = new ArrayList<>();

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

    @XmlElement
    public List<SetDTO> getSets() {
        return sets;
    }

    @XmlElement
    public List<String> getLanguages() {
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
        return (result == null ? Movie.UNKNOWN : result);
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }

    public List<MovieId> getMovieIds() {
        List<MovieId> list = new ArrayList<>();
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
