/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.model;

import com.moviejukebox.model.Comparator.FilmographyDateComparator;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * This is the new bean for the Person
 *
 * @author ilgizar Initial code copied from com.moviejukebox.themoviedb.model.Person
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
    private final List<String> aka = new ArrayList<String>();
    private final List<String> departments = new ArrayList<String>();
    private final List<Movie> movies = new ArrayList<Movie>();
    private Map<String, String> indexes = new HashMap<String, String>();
    private final String backdropToken = PropertiesUtil.getProperty("mjb.scanner.backdropToken", ".backdrop");
    private boolean sortFilmographyAsc = PropertiesUtil.getBooleanProperty("plugin.filmography.sort.asc", false);
    private FilmographyDateComparator filmographyCmp = new FilmographyDateComparator(sortFilmographyAsc);

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
        Collections.sort(filmography, filmographyCmp);
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
        String tmpAka = StringUtils.trimToEmpty(alsoKnownAs);
        if (isValidString(tmpAka) && !getName().equals(tmpAka) && !getTitle().equals(tmpAka) && !this.aka.contains(tmpAka)) {
            this.aka.add(tmpAka);
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
