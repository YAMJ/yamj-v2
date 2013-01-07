/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Class to define the property name and the impact on each of the overwrite flags. If the
 *
 * @author stuart.boston
 *
 */
public class PropertyInformation {

    private String propertyName = Movie.UNKNOWN;
    private EnumSet<PropertyOverwrites> propertyOverwrites = EnumSet.noneOf(PropertyOverwrites.class);

    public PropertyInformation(String property, Set<PropertyOverwrites> propOverwrites) {
        this.propertyName = property;
        this.propertyOverwrites.addAll(propOverwrites);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public EnumSet<PropertyOverwrites> getOverwrites() {
        return propertyOverwrites;
    }

    public boolean isOverwrite(PropertyOverwrites overwrite) {
        if (propertyOverwrites.contains(overwrite)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Merge two PropertyInformation objects. Sets the overwrite flags to true.
     *
     * @param newPI
     */
    public void mergePropertyInformation(PropertyInformation newPI) {
        this.propertyOverwrites.addAll(newPI.getOverwrites());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getPropertyName());
        sb.append(" Overwrites: ").append(getOverwrites().toString());
        return sb.toString();
    }
}
