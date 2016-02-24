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
package com.moviejukebox.model.scriptablescraper;

import org.apache.commons.lang3.StringUtils;

/**
 * ScriptableScraper class
 *
 * @author ilgizar
 */
public final class ReplaceSS {

    private String input;
    private String pattern;
    private String with;

    public ReplaceSS(String input, String pattern, String with) {
        super();
        setInput(input);
        setPattern(pattern);
        setWith(with);
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        if (StringUtils.isNotBlank(input)) {
            this.input = input;
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (StringUtils.isNotBlank(pattern)) {
            this.pattern = pattern;
        }
    }

    public String getWith() {
        return with;
    }

    public void setWith(String with) {
        if (StringUtils.isNotBlank(with)) {
            this.with = with;
        }
    }
}
