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
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;

/*
 * @author ilgizar
 */
public class Award {
    private String name = Movie.UNKNOWN;
    private int won = 0;
    private int nominated = 0;
    private int year = -1;
    private Collection<String> wons = new ArrayList<String>();
    private Collection<String> nominations = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWon() {
        return (wons != null && wons.size() > 0)?wons.size():won;
    }

    public void setWon(int won) {
        this.won = won;
    }

    public int getNominated() {
        return (nominations != null && nominations.size() > 0)?nominations.size():nominated;
    }

    public void setNominated(int nominated) {
        this.nominated = nominated;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void addWon(String category) {
        wons.add(category);
        won = wons.size();
    }

    public void addNomination(String category) {
        nominations.add(category);
        nominated = nominations.size();
    }

    public void clearWon() {
        wons.clear();
        won = 0;
    }

    public void clearNomination() {
        nominations.clear();
        nominated = 0;
    }

    public Collection<String> getWons() {
        return wons;
    }

    public Collection<String> getNominations() {
        return nominations;
    }

    public void setWons(Collection<String> wons) {
        if (wons != null) {
            this.wons = wons;
            won = wons.size();
        }
    }

    public void setNominations(Collection<String> nominations) {
        if (nominations != null) {
            this.nominations = nominations;
            nominated = nominations.size();
        }
    }

    @Override
    public String toString() {
        return "Award{" + "name=" + name + ", won=" + won + ", nominated=" + nominated + ", year=" + year + ", wons=" + wons + ", nominations=" + nominations + '}';
    }
}
