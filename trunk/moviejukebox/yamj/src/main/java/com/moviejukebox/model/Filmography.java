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

import java.util.HashMap;
import java.util.Map;

import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.FileTools;
import static com.moviejukebox.tools.StringTools.*;

/**
 *  This is the new bean for the Person
 *
 *  @author ilgizar
 *  Initial code copied from com.moviejukebox.themoviedb.model.Filmography
 *
 */
public class Filmography {

    private static final String UNKNOWN = Movie.UNKNOWN;

    private Map<String, String> idMap   = new HashMap<String, String>(2);
    private String name                 = UNKNOWN;
    private String doublage             = UNKNOWN;
    private String filename             = UNKNOWN;
    private String job                  = UNKNOWN;
    private String character            = UNKNOWN;
    private String department           = UNKNOWN;
    private String rating               = UNKNOWN;
    private String url                  = UNKNOWN;
    private boolean isDirty             = true;

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

    public String getDoublage() {
        return doublage;
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
        if (isValidString(name) && !this.name.equalsIgnoreCase(name)) {
            this.name = name;
            setDirty();
        }
    }

    public void setDoublage(String name) {
        if (isValidString(doublage) && !doublage.equalsIgnoreCase(name)) {
            doublage = name;
            setDirty();
        }
    }

    public void setFilename(String filename) {
        if (isValidString(filename) && !this.filename.equals(filename)) {
            this.filename = filename;
            setDirty();
        }
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

    public void setDepartment() {
        if (isNotValidString(department)) {
            if (job.equalsIgnoreCase("actor") || job.equalsIgnoreCase("actress")) {
                setDepartment("Actors");
            } else if (job.toLowerCase().contains("producer") || job.toLowerCase().contains("casting")) {
                setDepartment("Production");
            } else if (job.toLowerCase().contains("director")) {
                setDepartment("Directing");
            } else if (job.toLowerCase().contains("author") || job.toLowerCase().contains("writer")) {
                setDepartment("Writing");
            } else if (job.toLowerCase().contains("editor")) {
                setDepartment("Editing");
            } else if (job.toLowerCase().contains("music")) {
                setDepartment("Sound");
            } else if (job.toLowerCase().contains("photo")) {
                setDepartment("Camera");
            }
        }
    }
}
