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

import com.moviejukebox.model.Person;
import java.io.Serializable;
import java.util.Comparator;

public class PersonComparator implements Comparator<Person>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Person first, Person second) {
        return second.getPopularity().compareTo(first.getPopularity());
    }
}
