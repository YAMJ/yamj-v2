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

public class GenreDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME   = "GENRE";
    public static final String TABLE_KEY    = "ID";
    public static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + 
                " (ID integer primary key, NAME text, FOREIGN_KEY text)";
    public static final String INSERT_TABLE = "insert into " + TABLE_NAME + 
                " (ID, NAME, FOREIGN_KEY) values (?, ?, ?)";
    public static final String DROP_TABLE   = "drop table if exists " + TABLE_NAME;

    private int     id;
    private String  name;
    private String  foreignKey;
    
    public GenreDTO() {
        this.id = 0;    // Set to the default of 0 (zero)
    }
    
    public GenreDTO(int id, String name, String foreignKey) {
        this.id = id;
        this.name = name;
        this.foreignKey = foreignKey;
    }
    
    public void populateDTO(ResultSet rs) throws Throwable {
        setId(rs.getInt("ID"));
        setName(rs.getString("NAME"));
        setForeignKey(rs.getString("FOREIGN_KEY"));
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
    
    public String getForeignKey() {
        return foreignKey;
    }
    
    public void setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
    }

    @Override
    public String toString() {
        return "GenreDTO [id=" + id + ", name=" + name + ", foreignKey=" + foreignKey + "]";
    }
    
}
