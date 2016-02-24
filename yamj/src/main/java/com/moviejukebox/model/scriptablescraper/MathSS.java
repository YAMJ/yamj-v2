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
public final class MathSS {

    String type;
    String value1;
    String value2;
    String result = "int";

    public MathSS(String type, String value1, String value2, String result) {
        super();
        setType(type);
        setValue1(value1);
        setValue2(value2);
        setResultType(result);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setValue(String value, int index) {
        if (index == 0) {
            value1 = value;
        } else {
            value2 = value;
        }
    }

    public void setValue1(String value) {
        setValue(value, 0);
    }

    public void setValue2(String value) {
        setValue(value, 1);
    }

    public String getValue(int index) {
        if (index == 0) {
            return value1;
        }
        return value2;
    }

    public String getValue1() {
        return getValue(0);
    }

    public String getValue2() {
        return getValue(1);
    }

    public void setResultType(String result) {
        if (StringUtils.isNotBlank(result)) {
            this.result = result;
        }
    }

    public String getResultType() {
        return result;
    }
}
