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

public class VideoSiteDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String TABLE_NAME   = "VIDEO_SITE";
    public static final String TABLE_KEY    = "VIDEO_ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (VIDEO_ID integer, SITE text, SITE_ID text, primary key (VIDEO_ID, SITE) )";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (VIDEO_ID, SITE, SITE_ID) values (?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     videoId;
    private String  site;
    private String  siteId;
    
    public VideoSiteDTO() {
        videoId = 0;
        site = "";
        siteId = "";
    }
    
    public VideoSiteDTO(int videoId, String site, String siteId) {
        this.videoId = videoId;
        this.site = site;
        this.siteId = siteId;
    }

    public void populateDTO(ResultSet rs) throws Throwable {
        setVideoId(rs.getInt("VIDEO_ID"));
        setSite(rs.getString("SITE"));
        setSiteId(rs.getString("SITE_ID"));
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    @Override
    public String toString() {
        return "VideoSiteDTO [videoId=" + videoId + ", site=" + site + ", siteId=" + siteId + "]";
    }

}
