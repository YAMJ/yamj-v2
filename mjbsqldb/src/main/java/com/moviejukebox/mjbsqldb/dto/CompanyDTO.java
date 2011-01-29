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

public class CompanyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "COMPANY";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (ID integer primary key, COMPANY text, URL text)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (ID, COMPANY, URL) values (?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  company;
    private String  url;
    
    public CompanyDTO() {
        super();
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public CompanyDTO(int id, String company, String url) {
        super();
        this.id = id;
        this.company = company;
        this.url = url;
    }

    public void populateDTO(ResultSet rs) throws Throwable {
        setId(rs.getInt("ID"));
        setCompany(rs.getString("COMPANY"));
        setUrl(rs.getString("URL"));
    }

    public int getId() {
        return id;
    }
    
    public String getCompany() {
        return company;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "CompanyDTO [id=" + id + ", company=" + company + ", url=" + url + "]";
    }

}
