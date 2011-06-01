/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.model.Comparator;

import java.util.Comparator;
import java.util.List;

/**
 * @author altman.matthew
 */
public class CertificationComparator implements Comparator<String> {

    private List<String> ordering = null;

    public CertificationComparator(List<String> ordering) {
        this.ordering = ordering;
    }

    public int compare(String obj1, String obj2) {
        int obj1Pos = ordering.indexOf(obj1);
        int obj2Pos = ordering.indexOf(obj2);

        if (obj1Pos < 0) {
            ordering.add(obj1);
            obj1Pos = ordering.indexOf(obj1);
        }
        if (obj2Pos < 0) {
            ordering.add(obj2);
            obj2Pos = ordering.indexOf(obj2);
        }

        return obj1Pos - obj2Pos;
    }
}
