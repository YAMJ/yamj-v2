/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.model;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
 * @author ilgizar
 */
public class Award {

    private String name = Movie.UNKNOWN;
    private int won = 0;
    private int nominated = 0;
    private int year = -1;
    private Collection<String> wons = new ArrayList<>();
    private Collection<String> nominations = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWon() {
        return (wons == null || wons.isEmpty()) ? won : wons.size();
    }

    public void setWon(int won) {
        this.won = won;
    }

    public int getNominated() {
        return (nominations == null || nominations.isEmpty()) ? nominated : nominations.size();
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
