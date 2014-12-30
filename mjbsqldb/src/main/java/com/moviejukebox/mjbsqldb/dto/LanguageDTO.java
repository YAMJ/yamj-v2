/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

public class LanguageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "LANGUAGE";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
                " (ID integer primary key, LANGUAGE TEXT, SHORT_CODE TEXT, MEDIUM_CODE TEXT, LONG_CODE TEXT)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME +
                " (ID, LANGUAGE, SHORT_CODE, MEDIUM_CODE, LONG_CODE) values (?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  language;
    private String  shortCode;
    private String  mediumCode;
    private String  longCode;

    public LanguageDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }

    public LanguageDTO(int id, String language, String shortCode, String mediumCode, String longCode) {
        this.id = id;
        this.language = language;
        this.shortCode = shortCode;
        this.mediumCode = mediumCode;
        this.longCode = longCode;
    }

    public void populateDTO(ResultSet rs) throws SQLException {
        setId(rs.getInt("ID"));
        setLanguage(rs.getString("LANGUAGE"));
        setShortCode(rs.getString("SHORT_CODE"));
        setMediumCode(rs.getString("MEDIUM_CODE"));
        setLongCode(rs.getString("LONG_CODE"));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getMediumCode() {
        return mediumCode;
    }

    public void setMediumCode(String mediumCode) {
        this.mediumCode = mediumCode;
    }

    public String getLongCode() {
        return longCode;
    }

    public void setLongCode(String longCode) {
        this.longCode = longCode;
    }

    @Override
    public String toString() {
        return "LanguageDTO [id=" + id + ", language=" + language + ", shortCode=" + shortCode + ", mediumCode=" + mediumCode + ", longCode=" + longCode + "]";
    }

}
