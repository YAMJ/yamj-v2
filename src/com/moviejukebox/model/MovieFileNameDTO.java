/*
 *      Copyright (c) 2004-2010 YAMJ Members
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
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

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
}
