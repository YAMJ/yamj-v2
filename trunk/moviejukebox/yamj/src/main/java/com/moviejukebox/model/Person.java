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

import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.isNotValidString;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.moviejukebox.tools.FileTools;

/**
 *  This is the new bean for the Person
 *
 *  @author ilgizar
 *  Initial code copied from com.moviejukebox.themoviedb.model.Person
 *
 */
public class Person extends Filmography {

    private static final String UNKNOWN = Movie.UNKNOWN;

//    private Map<String, String> idMap   = new HashMap<String, String>(2);
    private String  biography           = UNKNOWN;
    private int     order               = -1;
    private int     castId              = -1;
    private int     version             = -1;
    private int     knownMovies         = -1;
    private String  birthday            = UNKNOWN;
    private String  birthPlace          = UNKNOWN;
    private String  photoURL            = UNKNOWN;
    private String  photoFilename       = UNKNOWN;
    private String  filename            = UNKNOWN;
    private boolean isDirtyPhoto        = false;
    private boolean isScrapeLibrary     = true;
    private List<Filmography> filmography = new ArrayList<Filmography>();
    private List<String>      aka         = new ArrayList<String>();
    private String  lastModifiedAt;

    public List<String> getAka() {
        return aka;
    }

    public String getBiography() {
        return biography;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public int getCastId() {
        return castId;
    }

    public List<Filmography> getFilmography() {
        return filmography;
    }

    public int getKnownMovies() {
        return knownMovies;
    }

    public String getLastModifiedAt() {
        return lastModifiedAt;
    }

    public int getOrder() {
        return order;
    }

    public int getVersion() {
        return version;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public String getPhotoFilename() {
        return photoFilename;
    }

    public String getFilename() {
        return filename;
    }

    public void addAka(String alsoKnownAs) {
        if (isValidString(alsoKnownAs)) {
            this.aka.add(alsoKnownAs);
            setDirty();
        }
    }

    public void addFilm(Filmography film) {
        if (film != null) {
            this.filmography.add(film);
            setDirty();
        }
    }

    public void setPhotoURL(String URL) {
        if (isValidString(URL) && !this.photoURL.equalsIgnoreCase(URL)) {
            this.photoURL = URL;
            setDirty();
        }
    }

    public void setPhotoFilename(String filename) {
        if (isValidString(filename) && !this.photoFilename.equalsIgnoreCase(filename)) {
            this.photoFilename = filename;
            setDirty();
        }
    }

    public void setPhotoFilename() {
        if (isValidString(getName()) && isNotValidString(photoFilename)) {
            setPhotoFilename(FileTools.makeSafeFilename(getName() + ".jpg"));
        }
    }

    public void setFilename(String filename) {
        if (isValidString(filename) && !this.filename.equalsIgnoreCase(filename)) {
            this.filename = filename;
            setDirty();
        }
    }

    public void setFilename() {
        if (isValidString(getName())) {
            setFilename(FileTools.makeSafeFilename(getName()));
        }
    }

    public void setAka(List<String> aka) {
        if (aka != null && !this.aka.equals(aka)) {
            this.aka = aka;
            setDirty();
        }
    }

    public void setBiography(String biography) {
        if (isValidString(biography) && !this.biography.equalsIgnoreCase(biography)) {
            this.biography = biography;
            setDirty();
        }
    }

    public void setBirthday(String birthday) {
        if (isValidString(birthday) && !this.birthday.equalsIgnoreCase(birthday)) {
            this.birthday = birthday;
            setDirty();
        }
    }

    public void setBirthPlace(String birthPlace) {
        if (isValidString(birthPlace) && !this.birthPlace.equalsIgnoreCase(birthPlace)) {
            this.birthPlace = birthPlace;
            setDirty();
        }
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

    public void setFilmography(List<Filmography> filmography) {
        if (filmography != null && !this.filmography.equals(filmography)) {
            this.filmography = filmography;
            setDirty();
        }
    }

    public void setKnownMovies(int knownMovies) {
        if (this.knownMovies != knownMovies) {
            this.knownMovies = knownMovies;
            setDirty();
        }
    }

    public void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public void setLastModifiedAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setLastModifiedAt(sdf.format(Calendar.getInstance().getTime()));
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

    public void setVersion(int version) {
        if (this.version != version) {
            this.version = version;
            setDirty();
        }
    }

    @Override
    public void setDirty(boolean isDirty) {
        super.setDirty(isDirty);
        setLastModifiedAt();
    }

    public boolean isDirtyPhoto() {
        return isDirtyPhoto;
    }

    public void setDirtyPhoto(boolean isDirty) {
        if (isDirtyPhoto != isDirty) {
            isDirtyPhoto = isDirty;
            setDirty();
        }
    }

    public void setDirtyPhoto() {
        setDirtyPhoto(true);
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
}
