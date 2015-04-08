/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
