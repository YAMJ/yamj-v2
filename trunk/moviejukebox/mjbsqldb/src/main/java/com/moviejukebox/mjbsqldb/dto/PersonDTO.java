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
package com.moviejukebox.mjbsqldb.dto;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "PERSON";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
                " (ID integer primary key, NAME text, JOB text, FOREIGN_KEY text, URL text, BIOGRAPHY text, BIRTHDAY text)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME +
                " (ID, NAME, JOB, FOREIGN_KEY, URL, BIOGRAPHY, BIRTHDAY) values (?, ?, ?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  name;
    private String  job;
    private String  foreignKey;
    private String  url;
    private String  biography;
    private String  birthday;

    public PersonDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }

    public PersonDTO(int id, String name, String job, String foreignKey, String url, String biography, String birthday) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.foreignKey = foreignKey;
        this.url = url;
        this.biography = biography;
        this.birthday = birthday;
    }

    public void populateDTO(ResultSet rs) throws SQLException {
        setId(rs.getInt("ID"));
        setName(rs.getString("NAME"));
        setJob(rs.getString("JOB"));
        setForeignKey(rs.getString("FOREIGN_KEY"));
        setUrl(rs.getString("URL"));
        setBiography(rs.getString("BIOGRAPHY"));
        setBirthday(rs.getString("BIRTHDAY"));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    @Override
    public String toString() {
        return "PersonDTO [id=" + id + ", name=" + name + ", job=" + job + ", foreignKey=" + foreignKey + ", url=" + url + ", biography=" + biography
                        + ", birthday=" + birthday + "]";
    }

}
