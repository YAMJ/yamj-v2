/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;

import java.util.HashMap;
import java.util.Map;

import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;

/**
 *  This is the new bean for the Person
 *
 *  @author ilgizar
 *  Initial code copied from com.moviejukebox.themoviedb.model.Filmography
 *
 */
public class Filmography {

    private static final String UNKNOWN = Movie.UNKNOWN;

    // Define the list of jobs
    public static final String JOB_ACTOR      = "actor";
    public static final String JOB_ACTRESS    = "actress";
    public static final String JOB_PRODUCER   = "producer";
    public static final String JOB_CASTING    = "casting";
    public static final String JOB_DESIGN     = "design";
    public static final String JOB_DIRECTOR   = "director";
    public static final String JOB_AUTHOR     = "author";
    public static final String JOB_WRITER     = "writer";
    public static final String JOB_EDITOR     = "editor";
    public static final String JOB_MUSIC      = "music";
    public static final String JOB_COMPOSER   = "composer";
    public static final String JOB_PHOTO      = "photo";
    public static final String JOB_CAMERA     = "camera";
    public static final String JOB_OPERATOR   = "operator";
    public static final String JOB_THEMSELVES = "themselves";   // Also Himself or Herself

    // Define the list of departments
    public static final String DEPT_ACTORS     = "Actors";
    public static final String DEPT_PRODUCTION = "Production";
    public static final String DEPT_DIRECTING  = "Directing";
    public static final String DEPT_WRITING    = "Writing";
    public static final String DEPT_EDITING    = "Editing";
    public static final String DEPT_SOUND      = "Sound";
    public static final String DEPT_CAMERA     = "Camera";

    private Map<String, String> idMap   = new HashMap<String, String>(2);
    private String name                 = UNKNOWN;
    private String title                = UNKNOWN;
    private String originalTitle        = UNKNOWN;
    private String year                 = UNKNOWN;
    private String doublage             = UNKNOWN;
    private String filename             = UNKNOWN;
    private String job                  = UNKNOWN;
    private String character            = UNKNOWN;
    private String department           = UNKNOWN;
    private String rating               = UNKNOWN;
    private String url                  = UNKNOWN;
    private boolean isDirty             = true;
    private boolean isScrapeLibrary     = true;
    private int     order               = -1;
    private int     castId              = -1;

    public String getId() {
        return getId(ImdbPlugin.IMDB_PLUGIN_ID);
    }

    public String getId(String key) {
        String result = idMap.get(key);
        if (result != null) {
            return result;
        } else {
            return UNKNOWN;
        }
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getYear() {
        return year;
    }

    public String getFilename() {
        return filename;
    }

    public String getJob() {
        return job;
    }

    public String getCharacter() {
        return character;
    }

    public String getDepartment() {
        return department;
    }

    public String getRating() {
        return rating;
    }

    public String getUrl() {
        return url;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setUrl(String url) {
        if (isValidString(url) && !this.url.equalsIgnoreCase(url)) {
            this.url = url;
            setDirty();
        }
    }

    public void setName(String name) {
        if (isValidString(name) && !this.name.equalsIgnoreCase(name.trim())) {
            this.name = name.trim();

            if (isNotValidString(title)) {
                title = this.name;
            }

            if (isNotValidString(originalTitle)) {
                originalTitle = this.name;
            }

            setDirty();
        }
    }

    public void setTitle(String title) {
        if (isValidString(title) && !this.title.equalsIgnoreCase(title.trim())) {
            this.title = title.trim();
            setDirty();
        }
    }

    public void setOriginalTitle(String originalTitle) {
        if (isValidString(originalTitle) && !this.originalTitle.equalsIgnoreCase(originalTitle)) {
            this.originalTitle = originalTitle;
            if (isNotValidString(title)) {
                title = originalTitle;
            }
            setDirty();
        }
    }

    public void setFilename(String filename) {
        if (isValidString(filename) && !this.filename.equals(filename)) {
            this.filename = filename;
            setDirty();
        }
    }

    public void clearFilename() {
        this.filename = Movie.UNKNOWN;
        setDirty();
    }

    public void setFilename() {
        if (isValidString(name) && isNotValidString(filename)) {
            setFilename(FileTools.makeSafeFilename(name));
        }
    }

    public void setDepartment(String department) {
        if (isValidString(department) && !this.department.equalsIgnoreCase(department)) {
            this.department = department;
            setDirty();
        }
    }

    public void setCharacter(String character) {
        if (isValidString(character) && !this.character.equalsIgnoreCase(character)) {
            this.character = character;
            setDirty();
        }
    }

    public void setJob(String job) {
        if (isValidString(job) && !this.job.equalsIgnoreCase(job)) {
            this.job = job;
            setDirty();
        }
    }

    public void setRating(String rating) {
        if (isValidString(rating) && !this.rating.equalsIgnoreCase(rating)) {
            this.rating = rating;
            setDirty();
        }
    }

    public void setDirty() {
        setDirty(true);
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public void setIdMap(Map<String, String> idMap) {
        this.idMap = idMap;
        setDirty();
    }

    public void setId(String id) {
        setId(ImdbPlugin.IMDB_PLUGIN_ID, id);
        setDirty();
    }

    public void setId(String key, String id) {
        if (isValidString(key) && isValidString(id) && !id.equalsIgnoreCase(this.getId(key))) {
            this.idMap.put(key, id);
            setDirty();
        }
    }

    public int getCastId() {
        return castId;
    }

    public int getOrder() {
        return order;
    }

    public void setCastId(int castId) {
        if (this.castId != castId) {
            this.castId = castId;
            setDirty();
        }
    }

    public void setCastId(String castId) {
        if (isValidString(castId) && !castId.equalsIgnoreCase(Integer.toString(this.castId))) {
            try {
                this.castId = Integer.parseInt(castId);
                setDirty();
            } catch (Exception ignore) {
            }
        }
    }

    public void setOrder(int order) {
        if (this.order != order) {
            this.order = order;
            setDirty();
        }
    }

    public void setOrder(String order) {
        if (isValidString(order) && !order.equalsIgnoreCase(Integer.toString(this.order))) {
            try {
                this.order = Integer.parseInt(order);
                setDirty();
            } catch (Exception ignore) {
            }
        }
    }

    public String getDoublage() {
        return doublage;
    }

    public void setDoublage(String name) {
        if (isValidString(name) && !doublage.equalsIgnoreCase(name)) {
            doublage = name;
            setDirty();
        }
    }

    public boolean isScrapeLibrary() {
        return isScrapeLibrary;
    }

    public void setScrapeLibrary(boolean isScrapeLibrary) {
        this.isScrapeLibrary = isScrapeLibrary;
    }

    public void setScrapeLibrary() {
        setScrapeLibrary(true);
    }

    public void setYear(String year) {
        if (isValidString(year) && !this.year.equalsIgnoreCase(year)) {
            this.year = year;
            setDirty();
        }
    }

    public void setDepartment() {
        if (isNotValidString(department)) {
            if (job.equalsIgnoreCase(JOB_ACTOR) || job.equalsIgnoreCase(JOB_ACTRESS)) {
                setDepartment(DEPT_ACTORS);
            } else if (job.toLowerCase().contains(JOB_PRODUCER) || job.toLowerCase().contains(JOB_CASTING) || job.toLowerCase().contains(JOB_DESIGN)) {
                setDepartment(DEPT_PRODUCTION);
            } else if (job.toLowerCase().contains(JOB_DIRECTOR)) {
                setDepartment(DEPT_DIRECTING);
            } else if (job.toLowerCase().contains(JOB_AUTHOR) || job.toLowerCase().contains(JOB_WRITER)) {
                setDepartment(DEPT_WRITING);
            } else if (job.toLowerCase().contains(JOB_EDITOR)) {
                setDepartment(DEPT_EDITING);
            } else if (job.toLowerCase().contains(JOB_MUSIC) || job.toLowerCase().contains(JOB_COMPOSER)) {
                setDepartment(DEPT_SOUND);
            } else if (job.toLowerCase().contains(JOB_PHOTO) || job.toLowerCase().contains(JOB_CAMERA) || job.toLowerCase().contains(JOB_OPERATOR)) {
                setDepartment(DEPT_CAMERA);
            }
        }
    }
}
