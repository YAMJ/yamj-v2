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

    public void populateDTO(ResultSet rs) throws Throwable {
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
