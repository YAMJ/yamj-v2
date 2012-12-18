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

import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the new bean for the Person
 *
 * @author ilgizar Initial code copied from
 * com.moviejukebox.themoviedb.model.Person
 *
 */
public class Person extends Filmography {

    private static final String UNKNOWN = Movie.UNKNOWN;
    private String biography = UNKNOWN;
    private int version = -1;
    private int knownMovies = -1;
    private String birthPlace = UNKNOWN;
    private String birthName = UNKNOWN;
    private int popularity = 1;
    private String lastModifiedAt;
    private String backdropFilename = UNKNOWN;
    private String backdropURL = UNKNOWN;
    private boolean isDirtyBackdrop = false;
    private List<Filmography> filmography = new ArrayList<Filmography>();
    private List<String> aka = new ArrayList<String>();
    private List<String> departments = new ArrayList<String>();
    private List<Movie> movies = new ArrayList<Movie>();
    private Map<String, String> indexes = new HashMap<String, String>();
    private String backdropToken = PropertiesUtil.getProperty("mjb.scanner.backdropToken", ".backdrop");

    public Person() {
    }

    public Person(Filmography person) {
        setIdMap(person.getIdMap());
        setName(person.getName());
        setDoublage(person.getDoublage());
        setTitle(person.getTitle());
        setFilename(person.getFilename());
        setJob(person.getJob());
        setCharacter(person.getCharacter());
        setDepartment(person.getDepartment());
        setRating(person.getRating());
        setUrl(person.getUrl());
        setScrapeLibrary(person.isScrapeLibrary());
        setOrder(person.getOrder());
        setCastId(person.getCastId());
        setPhotoURL(person.getPhotoURL());
        setPhotoFilename(person.getPhotoFilename());

        setDirtyPhoto(person.isDirtyPhoto());
        setDirty(person.isDirty());
    }

    public Person(Person person) {
        setIdMap(person.getIdMap());
        setName(person.getName());
        setDoublage(person.getDoublage());
        setTitle(person.getTitle());
        setFilename(person.getFilename());
        setJob(person.getJob());
        setCharacter(person.getCharacter());
        setDepartment(person.getDepartment());
        setRating(person.getRating());
        setUrl(person.getUrl());
        setScrapeLibrary(person.isScrapeLibrary());
        setOrder(person.getOrder());
        setCastId(person.getCastId());
        setYear(person.getYear());
        setPhotoURL(person.getPhotoURL());
        setPhotoFilename(person.getPhotoFilename());

        setBiography(person.getBiography());
        setVersion(person.getVersion());
        setKnownMovies(person.getKnownMovies());
        setBirthPlace(person.getBirthPlace());
        setBirthName(person.getBirthName());
        setPopularity(person.getPopularity());
        setFilmography(person.getFilmography());
        setAka(person.getAka());
        setDepartments(person.getDepartments());
        setLastModifiedAt(person.getLastModifiedAt());
        setIndexes(person.getIndexes());
        setBackdropURL(person.getBackdropURL());
        setBackdropFilename(person.getBackdropFilename());

        setDirtyPhoto(person.isDirtyPhoto());
        setDirtyBackdrop(person.isDirtyBackdrop());
        setDirty(person.isDirty());
    }

    public List<String> getAka() {
        return aka;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public String getBiography() {
        return biography;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public String getBirthName() {
        return birthName;
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

    public int getVersion() {
        return version;
    }

    public void addAka(String alsoKnownAs) {
        if (isValidString(alsoKnownAs) && !getName().equals(alsoKnownAs) && !getTitle().equals(alsoKnownAs) && !this.aka.contains(alsoKnownAs)) {
            this.aka.add(alsoKnownAs);
            setDirty();
        }
    }

    public void addDepartment(String department) {
        if (isValidString(department) && !departments.contains(department)) {
            departments.add(department);
            if (isNotValidString(getDepartment())) {
                setDepartment(department);
            }
            setDirty();
        }
    }

    public void addFilm(Filmography film) {
        if (film != null) {
            this.filmography.add(film);
            setDirty();
        }
    }

    public final void setAka(List<String> aka) {
        if (aka != null && !this.aka.equals(aka)) {
            this.aka.clear();
            for (String akaName : aka) {
                addAka(akaName);
            }
            setDirty();
        }
    }

    public final void setDepartments(List<String> departments) {
        if (departments != null && !this.departments.equals(departments)) {
            this.departments.clear();
            for (String department : departments) {
                addDepartment(department);
            }
            setDirty();
        }
    }

    public final void setBiography(String biography) {
        if (isValidString(biography) && !this.biography.equalsIgnoreCase(biography)) {
            this.biography = biography;
            setDirty();
        }
    }

    public final void setBirthPlace(String birthPlace) {
        if (isValidString(birthPlace) && !this.birthPlace.equalsIgnoreCase(birthPlace)) {
            this.birthPlace = birthPlace;
            setDirty();
        }
    }

    public final void setBirthName(String birthName) {
        if (isValidString(birthName) && !this.birthName.equalsIgnoreCase(birthName)) {
            this.birthName = birthName;
            setDirty();
        }
    }

    public final void setFilmography(List<Filmography> filmography) {
        if (filmography != null && !this.filmography.equals(filmography)) {
            this.filmography = filmography;
            setDirty();
        }
    }

    public void clearFilmography() {
        filmography.clear();
        setDirty();
    }

    public final void setKnownMovies(int knownMovies) {
        if (this.knownMovies != knownMovies) {
            this.knownMovies = knownMovies;
            setDirty();
        }
    }

    public final void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public void setLastModifiedAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setLastModifiedAt(sdf.format(Calendar.getInstance().getTime()));
    }

    public final void setVersion(int version) {
        if (this.version != version) {
            this.version = version;
            setDirty();
        }
    }

    @Override
    public final void setDirty(boolean isDirty) {
        super.setDirty(isDirty);
        setLastModifiedAt();
    }

    public final void setPopularity(Integer value) {
        popularity = value;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void popularityUp() {
        popularity++;
    }

    public void popularityUp(Integer value) {
        popularity += value;
    }

    public void popularityUp(Movie movie) {
        if (movie != null && !movies.contains(movie)) {
            movies.add(movie);
        }
        popularity = movies.size();
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public Map<String, String> getIndexes() {
        return indexes;
    }

    public void addIndex(String key, String index) {
        if (key != null && index != null) {
            indexes.put(key, index);
        }
    }

    public final void setIndexes(Map<String, String> indexes) {
        this.indexes = new HashMap<String, String>(indexes);
    }

    public String getBackdropURL() {
        return backdropURL;
    }

    public String getBackdropFilename() {
        return backdropFilename;
    }

    public final void setBackdropURL(String url) {
        if (isValidString(url) && !backdropURL.equalsIgnoreCase(url)) {
            backdropURL = url;
            setDirty();
        }
    }

    public final void setBackdropFilename(String filename) {
        if (isValidString(filename) && !this.backdropFilename.equalsIgnoreCase(FileTools.makeSafeFilename(filename))) {
            this.backdropFilename = FileTools.makeSafeFilename(filename);
            setDirty();
        }
    }

    public void setBackdropFilename() {
        if (isValidString(getFilename()) && isNotValidString(backdropFilename)) {
            setBackdropFilename(getFilename() + backdropToken + ".jpg");
        }
    }

    public void clearBackdropFilename() {
        if (!this.backdropFilename.equals(UNKNOWN)) {
            this.backdropFilename = UNKNOWN;
            setDirty();
        }
    }

    public boolean isDirtyBackdrop() {
        return isDirtyBackdrop;
    }

    public final void setDirtyBackdrop(boolean isDirty) {
        if (isDirtyBackdrop != isDirty) {
            isDirtyBackdrop = isDirty;
            setDirty();
        }
    }

    public void setDirtyBackdrop() {
        setDirtyBackdrop(true);
    }

    @Override
    public String toString() {
        StringBuilder p = new StringBuilder("Person [");
        p.append("biography=").append(biography);
        p.append(", version=").append(version);
        p.append(", knownMovies=").append(knownMovies);
        p.append(", birthPlace=").append(birthPlace);
        p.append(", birthName=").append(birthName);
        p.append(", popularity=").append(popularity);
        p.append(", lastModifiedAt=").append(lastModifiedAt);
        p.append(", backdropFilename=").append(backdropFilename);
        p.append(", backdropURL=").append(backdropURL);
        p.append(", isDirtyBackdrop=").append(isDirtyBackdrop);
        p.append(", filmography=").append(filmography.size()).append(" films");
        p.append(", aka=").append(aka);
        p.append(", departments=").append(departments);
        p.append(", movies=").append(movies.size()).append(" movies");
        p.append(", backdropToken=").append(backdropToken);
        p.append(']');
        return p.toString();
    }
}
