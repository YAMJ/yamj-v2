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

package com.moviejukebox.mjbsqldb.dto;

import java.io.Serializable;
import java.sql.ResultSet;

/**
 * This is the video file information that is specific to the file
 * Part information is held in VideoFilePartDTO
 * @author stuart.boston
 *
 */
public class VideoFileDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String TABLE_NAME   = "VIDEO_FILE";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
        " (ID integer primary key, VIDEO_ID integer, FILE_LOCATION text, FILE_URL text, CONTAINER text," +
        " AUDIO_CHANNELS integer, VIDEO_CODEC_ID integer, AUDIO_CODEC_ID integer, RESOLUTION text," +
        " VIDEO_SOURCE text, VIDEO_OUTPUT text, ASPECT text, FPS FLOAT, FILE_DATE TEXT, FILE_SIZE LONG," +
        " NUMBER_PARTS integer, FIRST_PART integer, LAST_PART integer)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
        "(ID, VIDEO_ID, FILE_LOCATION, FILE_URL, CONTAINER, AUDIO_CHANNELS, VIDEO_CODEC_ID, AUDIO_CODEC_ID, RESOLUTION, VIDEO_SOURCE, " +
        "VIDEO_OUTPUT, ASPECT, FPS, FILE_DATE, FILE_SIZE, NUMBER_PARTS, FIRST_PART, LAST_PART)" +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private int     videoId;
    private String  fileLocation;   // The PC location of the file
    private String  fileUrl;        // The Player location of the file
    private String  container;
    private int     audioChannels;
    private int     videoCodecId;
    private int     audioCodecId;
    private String  resolution;
    private String  videoSource;
    private String  videoOutput;
    private String  aspect;
    private float   fps;
    private String  fileDate;
    private long    fileSize;
    private int     numberParts;
    private int     firstPart;
    private int     lastPart;

    public VideoFileDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public VideoFileDTO(int id, int videoId, String fileLocation, String fileUrl, String container, 
                    int audioChannels, int videoCodecId, int audioCodecId, String resolution, 
                    String videoSource, String videoOutput, String aspect, float fps, 
                    String fileDate, long fileSize, int numberParts, int firstPart, int lastPart) {
        super();
        this.id = id;
        this.videoId = videoId;
        this.fileLocation = fileLocation;
        this.fileUrl = fileUrl;
        this.container = container;
        this.audioChannels = audioChannels;
        this.videoCodecId = videoCodecId;
        this.audioCodecId = audioCodecId;
        this.resolution = resolution;
        this.videoSource = videoSource;
        this.videoOutput = videoOutput;
        this.aspect = aspect;
        this.fps = fps;
        this.fileDate = fileDate;
        this.fileSize = fileSize;
        this.numberParts = numberParts;
        this.firstPart = firstPart;
        this.lastPart = lastPart;
    }

    public void populateDTO(ResultSet rs) throws Throwable {
         setId(rs.getInt("ID"));
         setVideoId(rs.getInt("VIDEO_ID"));
         setFileLocation(rs.getString("FILE_LOCATION"));
         setFileUrl(rs.getString("FILE_URL"));
         setContainer(rs.getString("CONTAINER "));
         setAudioChannels(rs.getInt("AUDIO_CHANNELS"));
         setVideoCodecId(rs.getInt("VIDEO_CODEC_ID"));
         setAudioCodecId(rs.getInt("AUDIO_CODEC_ID "));
         setResolution(rs.getString("RESOLUTION"));
         setVideoSource(rs.getString("VIDEO_SOURCE"));
         setVideoOutput(rs.getString("VIDEO_OUTPUT"));
         setAspect(rs.getString("ASPECT"));
         setFps(rs.getFloat("FPS"));
         setFileDate(rs.getString("FILE_DATE"));
         setFileSize(rs.getLong("FILE_SIZE"));
         setNumberParts(rs.getInt("NUMBER_PARTS"));
         setFirstPart(rs.getInt("FIRST_PART"));
         setLastPart(rs.getInt("LAST_PART"));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public int getVideoCodecId() {
        return videoCodecId;
    }

    public void setVideoCodecId(int videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    public int getAudioCodecId() {
        return audioCodecId;
    }

    public void setAudioCodecId(int audioCodecId) {
        this.audioCodecId = audioCodecId;
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

    public String getAspect() {
        return aspect;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }

    public String getFileDate() {
        return fileDate;
    }

    public void setFileDate(String string) {
        this.fileDate = string;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getNumberParts() {
        return numberParts;
    }

    public void setNumberParts(int numberParts) {
        this.numberParts = numberParts;
    }

    public int getFirstPart() {
        return firstPart;
    }

    public void setFirstPart(int firstPart) {
        this.firstPart = firstPart;
    }

    public int getLastPart() {
        return lastPart;
    }

    public void setLastPart(int lastPart) {
        this.lastPart = lastPart;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    @Override
    public String toString() {
        return "VideoFileDTO [id=" + id + ", videoId=" + videoId + ", fileLocation=" + fileLocation + ", fileUrl=" + fileUrl + ", container=" + container
                        + ", audioChannels=" + audioChannels + ", videoCodecId=" + videoCodecId + ", audioCodecId=" + audioCodecId + ", resolution="
                        + resolution + ", videoSource=" + videoSource + ", videoOutput=" + videoOutput + ", aspect=" + aspect + ", fps=" + fps + ", fileDate="
                        + fileDate + ", fileSize=" + fileSize + ", numberParts=" + numberParts + ", firstPart=" + firstPart + ", lastPart=" + lastPart + "]";
    }

}
