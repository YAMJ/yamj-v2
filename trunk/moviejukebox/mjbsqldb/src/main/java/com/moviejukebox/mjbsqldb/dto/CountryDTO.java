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

public class CountryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "COUNTRY";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
                " (ID integer primary key, COUNTRY text, URL text)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME +
                " (ID, COUNTRY, URL) values (?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  country;
    private String  url;

    public CountryDTO() {
        super();
        this.id = 0;    // Set to the default of 0 (zero)
    }

    public CountryDTO(int id, String country, String url) {
        super();
        this.id = id;
        this.country = country;
        this.url = url;
    }

    public void populateDTO(ResultSet rs) throws SQLException {
        setId(rs.getInt("ID"));
        setCountry(rs.getString("COUNTRY"));
        setUrl(rs.getString("URL"));
    }

    public String getCountry() {
        return country;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "CountryDTO [id=" + id + ", country=" + country + ", url=" + url + "]";
    }


}
