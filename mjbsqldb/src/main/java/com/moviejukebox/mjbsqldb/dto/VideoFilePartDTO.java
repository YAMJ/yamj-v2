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
 * This DTO is for each individual part of a VideoFile
 * There MUST be a minimum of one of these records for each VideoFile 
 * @author stuart.boston
 *
 */
public class VideoFilePartDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "VIDEO_FILE_PART";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (ID integer primary key, FILE_ID integer, PART integer, TITLE text, PLOT text, SEASON integer)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (ID, FILE_ID, PART, TITLE, PLOT, SEASON) values (?, ?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private int     fileId;
    private int     part;
    private String  title;
    private String  plot;
    private int     season;
    
    public VideoFilePartDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public VideoFilePartDTO(int id, int fileId, int part, String title, String plot, int season) {
        this.id = id;
        this.fileId = fileId;
        this.part = part;
        this.title = title;
        this.plot = plot;
        this.season = season;
    }

    public void populateDTO(ResultSet rs) throws Throwable {
        setId(rs.getInt("ID"));
        setFileId(rs.getInt("FILE_ID"));
        setPart(rs.getInt("PART"));
        setTitle(rs.getString("TITLE"));
        setPlot(rs.getString("PLOT"));
        setSeason(rs.getInt("SEASON"));
    }

    public int getId() {
        return id;
    }
    
    public int getFileId() {
        return fileId;
    }
    
    public int getPart() {
        return part;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getPlot() {
        return plot;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
    
    public void setPart(int part) {
        this.part = part;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setPlot(String plot) {
        this.plot = plot;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    @Override
    public String toString() {
        return "VideoFilePartDTO [id=" + id + ", fileId=" + fileId + ", part=" + part + ", title=" + title + ", plot=" + plot + ", season=" + season + "]";
    }

}
