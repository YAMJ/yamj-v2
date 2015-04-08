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

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.*;

public class GenericFileFilter implements FilenameFilter {
    private Pattern genericFilePattern;

    // Set the passed filter.
    public GenericFileFilter(String regExp) {
        setPattern(regExp);
    }

    // If no filter is passed, select all files
    public GenericFileFilter() {
        setPattern(".*");
    }

    @Override
    public boolean accept (File fileDir, String name) {
        Matcher m = genericFilePattern.matcher(name);
        return m.find();
    }

    // Use this method to change the pattern without creating a new object
    private void setPattern(String filePattern) {
        try {
            genericFilePattern = Pattern.compile(filePattern);
        } catch (Exception ignore) {
            genericFilePattern = Pattern.compile(".*");
        }
    }

}