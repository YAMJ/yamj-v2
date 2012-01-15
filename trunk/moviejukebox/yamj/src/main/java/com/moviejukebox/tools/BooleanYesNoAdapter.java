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
package com.moviejukebox.tools;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Boolean adapter to change true/false to yes/no
 * http://stackoverflow.com/questions/343669/how-to-let-jaxb-render-boolean-as-0-and-1-not-true-and-false
 */
public class BooleanYesNoAdapter extends XmlAdapter<String, Boolean> {

    @Override
    public Boolean unmarshal(String s) {
        return s == null ? null : "YES".equalsIgnoreCase(s);
    }

    @Override
    public String marshal(Boolean c) {
        return c == null ? null : c ? "YES" : "NO";
    }
}
