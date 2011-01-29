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

public class CertificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "CERTIFICATION";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (ID integer primary key, CERTIFICATION text)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (ID, CERTIFICATION) values (?, ?, ?, ?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  certification;
    
    public CertificationDTO() {
        super();
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public CertificationDTO(int id, String certification) {
        super();
        this.id = id;
        this.certification = certification;
    }
    
    public void populateDTO(ResultSet rs) throws Throwable {
        setId(rs.getInt("ID"));
        setCertification(rs.getString("CERTIFICATION"));
    }
    
    public int getId() {
        return id;
    }
    
    public String getCertification() {
        return certification;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public void setCertification(String certification) {
        this.certification = certification;
    }

    @Override
    public String toString() {
        return "CertificationDTO [id=" + id + ", certification=" + certification + "]";
    }
    
}
