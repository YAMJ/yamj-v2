/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.mjbsqldb.dto;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    public void populateDTO(ResultSet rs) throws SQLException {
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
