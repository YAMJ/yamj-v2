package com.moviejukebox.model;

import java.util.Comparator;
import java.util.List;

/**
 *
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
