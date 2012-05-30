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
package com.moviejukebox.model.Comparator;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator<Object> {

    private Map<String, Integer> base;

    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    @Override
    public int compare(Object a, Object b) {
        return (base.get(String.valueOf(a)) < base.get(String.valueOf(b))) ? 1 : -1;
    }
}
