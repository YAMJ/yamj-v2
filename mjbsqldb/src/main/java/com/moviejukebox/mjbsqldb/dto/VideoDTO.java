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

public class VideoDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String TABLE_NAME   = "VIDEO";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (ID integer primary key, MJB_VERSION text, MJB_REVISION integer, MJB_UPDATE_DATE text, BASEFILENAME text, TITLE text, TITLE_SORT text, TITLE_ORIGINAL text, RELEASE_DATE text, RATING integer, TOP250 integer, PLOT text, OUTLINE text, QUOTE text, TAGLINE text, RUNTIME integer, VIDEO_TYPE text, SEASON integer, SUBTITLES text, LIBRARY_DESCRIPTION text, CERTIFICATION_ID integer)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (ID, MJB_VERSION, MJB_REVISION, MJB_UPDATE_DATE, BASEFILENAME, TITLE, TITLE_SORT, TITLE_ORIGINAL, RELEASE_DATE, RATING, TOP250, PLOT, OUTLINE, QUOTE, TAGLINE, RUNTIME, VIDEO_TYPE, SEASON, SUBTITLES, LIBRARY_DESCRIPTION, CERTIFICATION_ID) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    // Type of the Video
    public static final String TYPE_MOVIE  = "MOVIE";
    public static final String TYPE_TVSHOW = "TVSHOW";
    
    private int     id;             // Our generated ID
    private String  mjbVersion;     // Version of YAMJ that was used to write this record
    private int     mjbRevision;    // Revision of YAMJ
    private String  mjbUpdateDate;  // Date record was updated. http://stackoverflow.com/questions/2305973/java-util-date-vs-java-sql-date
    private String  baseFilename;   // Use this as the secondary key
    private String  title;
    private String  titleSort;
    private String  titleOriginal;
    private String  releaseDate;    // Simple date, YYYY-MM-DD
    private int     rating;
    private int     top250;
    private String  plot;
    private String  outline;
    private String  quote;
    private String  tagline;
    private int     runtime;        // in minutes, here? or in the fileDTO?
    private String  videoType;      // MOVIE or TVSHOW
    private int     season;         // This should be in a TV Show specific table or with the FileDTO?
    private String  subtitles;      // Should this be separate?
    private String  libraryDescription;
    private int     certificationId;
    
    public VideoDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public VideoDTO(int id, String mjbVersion, int mjbRevision, String mjbUpdateDate, String baseFilename, String title, String titleSort, String titleOriginal,
                    String releaseDate, int rating, int top250, String plot, String outline, String quote, String tagline, int runtime, String videoType,
                    int season, String subtitles, String libraryDescription, int certificationId) {
        this.id = id;
        this.mjbVersion = mjbVersion;
        this.mjbRevision = mjbRevision;
        this.mjbUpdateDate = mjbUpdateDate;
        this.baseFilename = baseFilename;
        this.title = title;
        this.titleSort = titleSort;
        this.titleOriginal = titleOriginal;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.top250 = top250;
        this.plot = plot;
        this.outline = outline;
        this.quote = quote;
        this.tagline = tagline;
        this.runtime = runtime;
        this.videoType = videoType;
        this.season = season;
        this.subtitles = subtitles;
        this.libraryDescription = libraryDescription;
        this.certificationId = certificationId;
    }

    public void populateDTO(ResultSet rs) throws Throwable {
        setId(rs.getInt("ID"));
        setMjbVersion(rs.getString("MJB_VERSION"));
        setMjbRevision(rs.getInt("MJB_REVISION"));
        setMjbUpdateDate(rs.getString("MJB_UPDATE_DATE"));
        setBaseFilename(rs.getString("BASEFILENAME"));
        setTitle(rs.getString("TITLE"));
        setTitleSort(rs.getString("TITLE_SORT"));
        setTitleOriginal(rs.getString("TITLE_ORIGINAL"));
        setReleaseDate(rs.getString("RELEASE_DATE"));
        setRating(rs.getInt("RATING"));
        setTop250(rs.getInt("TOP250"));
        setPlot(rs.getString("PLOT"));
        setOutline(rs.getString("OUTLINE"));
        setQuote(rs.getString("QUOTE"));
        setTagline(rs.getString("TAGLINE"));
        setRuntime(rs.getInt("RUNTIME"));
        setVideoType(rs.getString("VIDEO_TYPE"));
        setSeason(rs.getInt("SEASON"));
        setSubtitles(rs.getString("SUBTITLES"));
        setLibraryDescription(rs.getString("LIBRARY_DESCRIPTION"));
        setCertificationId(rs.getInt("CERTIFICATION_ID"));
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getPlot() {
        return plot;
    }
    
    public void setPlot(String plot) {
        this.plot = plot;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public int getRuntime() {
        return runtime;
    }
    
    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
    
    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        if ((TYPE_MOVIE + "," + TYPE_TVSHOW).contains(videoType)) {
             this.videoType = videoType;
        } else {
            this.videoType = TYPE_MOVIE;
        }
    }

    public String getMjbVersion() {
        return mjbVersion;
    }

    public int getMjbRevision() {
        return mjbRevision;
    }

    public String getMjbUpdateDate() {
        return mjbUpdateDate;
    }

    public void setMjbVersion(String mjbVersion) {
        this.mjbVersion = mjbVersion;
    }

    public void setMjbRevision(int mjbRevision) {
        this.mjbRevision = mjbRevision;
    }

    public void setMjbUpdateDate(String mjbUpdateDate) {
        this.mjbUpdateDate = mjbUpdateDate;
    }

    public String getBaseFilename() {
        return baseFilename;
    }

    public String getTitleSort() {
        return titleSort;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public int getTop250() {
        return top250;
    }

    public String getOutline() {
        return outline;
    }

    public String getQuote() {
        return quote;
    }

    public String getTagline() {
        return tagline;
    }

    public int getSeason() {
        return season;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public String getLibraryDescription() {
        return libraryDescription;
    }

    public void setBaseFilename(String baseFilename) {
        this.baseFilename = baseFilename;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setTop250(int top250) {
        this.top250 = top250;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    public void setLibraryDescription(String libraryDescription) {
        this.libraryDescription = libraryDescription;
    }

    public void setCertificationId(int certificationId) {
        this.certificationId = certificationId;
    }

    public int getCertificationId() {
        return certificationId;
    }

    @Override
    public String toString() {
        return "VideoDTO [id=" + id + ", mjbVersion=" + mjbVersion + ", mjbRevision=" + mjbRevision + ", mjbUpdateDate=" + mjbUpdateDate + ", baseFilename="
                        + baseFilename + ", title=" + title + ", titleSort=" + titleSort + ", titleOriginal=" + titleOriginal + ", releaseDate=" + releaseDate
                        + ", rating=" + rating + ", top250=" + top250 + ", plot=" + plot + ", outline=" + outline + ", quote=" + quote + ", tagline=" + tagline
                        + ", runtime=" + runtime + ", videoType=" + videoType + ", season=" + season + ", subtitles=" + subtitles + ", libraryDescription="
                        + libraryDescription + ", certificationId=" + certificationId + "]";
    }
    
}
