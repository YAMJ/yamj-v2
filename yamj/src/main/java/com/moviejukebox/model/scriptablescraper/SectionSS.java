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
package com.moviejukebox.model.scriptablescraper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ScriptableScraper class
 *
 * @author ilgizar
 */
public class SectionSS extends SectionContentSS {

    private final Logger LOG = LoggerFactory.getLogger(SectionSS.class);

    private final Collection<SectionContentSS> content = new ArrayList<>();
    private final SectionContentSS parent;

    public SectionSS(String name, SectionContentSS parent) {
        super(name);
        this.parent = parent;
    }

    public void addContent(SectionContentSS content) {
        if (content != null) {
            addItem("content", Integer.toString(this.content.size()));
            if (isDebug()) {
                LOG.debug("addContent(content): to {} add {} with index {} parent: {}",
                        getName(),
                        content.getName(),
                        this.content.size(),
                        parent != null ? parent.getName() : "null");
            }
            this.content.add(content);
        }
    }

    public SectionContentSS addContent(String name) {
        SectionContentSS newContent = new SectionContentSS(name);
        addItem("content", Integer.toString(this.content.size()));
        if (isDebug()) {
            LOG.debug("addContent(name): {} index: {}, parent: {}", getName(), this.content.size(), getName());
        }
        this.content.add(newContent);
        return newContent;
    }

    public Collection<SectionContentSS> getContent() {
        return content;
    }

    public SectionContentSS getContent(int index) {
        if (index >= 0 && index < content.size()) {
            return (SectionContentSS) content.toArray()[index];
        }

        return null;
    }

    @Override
    public String getVariable(String name) {
        if (StringUtils.isNotBlank(name)) {
            if (super.hasVariable(name)) {
                return super.getVariable(name);
            } 
            return getVariable(name, parent);
        }

        return "";
    }

    public String getVariable(String name, SectionContentSS section) {
        if (StringUtils.isNotBlank(name) && section != null) {
            return section.getVariable(name);
        }

        return "";
    }

    @Override
    public String getGlobalVariable(String name) {
        if (StringUtils.isNotBlank(name)) {
            if (parent != null) {
                return parent.getGlobalVariable(name);
            }
            return getVariable(name);
        }

        return "";
    }

    @Override
    public boolean hasVariable(String name) {
        if (StringUtils.isNotBlank(name)) {
            if (super.hasVariable(name)) {
                return true;
            }
            return hasVariable(name, parent);
        }

        return Boolean.FALSE;
    }

    public boolean hasVariable(String name, SectionContentSS section) {
        if (StringUtils.isNotBlank(name) && section != null) {
            return section.hasVariable(name);
        }

        return Boolean.FALSE;
    }

    @Override
    public boolean hasGlobalVariable(String name) {
        if (isDebug()) {
            LOG.debug("hasGlobalVariable: {}", name);
        }
        if (StringUtils.isNotBlank(name)) {
            if (parent != null) {
                return parent.hasGlobalVariable(name);
            }
            return hasVariable(name);
        }

        return Boolean.FALSE;
    }

    @Override
    public void setGlobalVariable(String name, String value) {
        if (StringUtils.isNotBlank(name)) {
            if (parent != null && !"action".equals(getName())) {
                parent.setGlobalVariable(name, value);
            } else {
                setVariable(name, value);
            }
        }
    }

    private static String escapeForRegex(final String text) {
        if (text.contains("$")) {
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == '$') {
                    sb.append("__DOLLAR_SIGN__");
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return text;
    }

    public String compileValue(String value) {
        if (isDebug()) {
            LOG.debug("compileValue: '{}'", value);
        }
        value = escapeForRegex(value);
        if (isDebug()) {
            LOG.debug("compileValue: escaped: '{}", value);
        }
        String result = value;

        int start, end;
        String variable;
        Pattern pattern = Pattern.compile("__DOLLAR_SIGN__\\{([^{}]+)\\}");
        while (value.contains("__DOLLAR_SIGN__")) {
            Matcher matcher = pattern.matcher(value);
            start = -1;
            while (matcher.find()) {
                for (int looper = 0; looper < matcher.groupCount(); looper++) {
                    variable = matcher.group(looper + 1);
                    if (isDebug()) {
                        LOG.debug("compileValue: matcher: '{}'", variable);
                    }
                    if (hasGlobalVariable(variable) || hasVariable(variable)) {
                        start = result.indexOf("__DOLLAR_SIGN__{" + variable);
                        end = result.indexOf("}", start);
                        if (isDebug()) {
                            LOG.debug("compileValue: start: {} end: {}", start, end);
                        }
                        variable = hasGlobalVariable(variable) ? getGlobalVariable(variable) : getVariable(variable);
                        if (variable == null || "null".equals(variable)) {
                            variable = "";
                        }
                        result = result.substring(0, start) + variable + result.substring(end + 1);
                    }
                }
            }
            if (start == -1) {
                result = result.replaceAll("__DOLLAR_SIGN__", "\\$");
            }
            value = result;
        }

        result = result.trim().replaceAll("^\\s+", "");
        if (result.contains("|")) {
            List<String> values = Arrays.asList(result.split("\\|"));
            result = "";
            for (String value1 : values) {
                if (value1.length() > 0) {
                    if (result.length() > 0) {
                        result += "|";
                    }
                    result += value1.trim().replaceAll("^\\s+", "");
                }
            }
        }
        if (isDebug()) {
            LOG.debug("compileValue: compiled: '{}'", result);
        }
        return result;
    }

    public boolean testCondition(String data) {
        Pattern pattern = Pattern.compile("(.*[^!])(!=|=|<|>)(.*)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String right, condition, left;
            left = matcher.group(1);
            condition = matcher.group(2);
            right = matcher.group(3);
            if (isDebug()) {
                LOG.debug("testCondition: data: {} left: '{}' condition: '{}' right: '{}'", data, left, condition, right);
            }
            left = compileValue(left);
            right = compileValue(right);
            if (isDebug()) {
                LOG.debug("testCondition: compilled left: '{}' compilled right: '{}'", left, right);
            }
            switch (condition) {
                case "!=":
                    return !left.equals(right);
                case "=":
                    return left.equals(right);
                case "<":
                    return Float.parseFloat(left) < Float.parseFloat(right);
                case ">":
                    return Float.parseFloat(left) > Float.parseFloat(right);
                default:
                    LOG.error("testCondition : Unsupported condition: '{}'", condition);
                    break;
            }
        }

        return false;
    }

    public String parseInput(String data, String regex) {
        if (isDebug()) {
            LOG.debug("parseInput: data: '{}'", data);
            LOG.debug("parseInput: regex: '{}'", regex);
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        int looper;
        String result = "";
        while (matcher.find()) {
            for (looper = 0; looper < matcher.groupCount(); looper++) {
                result += matcher.group(looper + 1) + ScriptableScraper.ARRAY_ITEM_DIVIDER;
            }
            result += ScriptableScraper.ARRAY_GROUP_DIVIDER;
        }

        if (isDebug()) {
            LOG.debug("parseInput: result: '{}'", result);
        }
        return result;
    }
}
