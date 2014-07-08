/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
 * Set of classes to prepare and processing ScriptableScraper format
 *
 * @author ilgizar
 */


public class ScriptableScraper {
    public static final String ARRAY_ITEM_DIVIDER = ",-=<>=-,";
    public static final String ARRAY_GROUP_DIVIDER = ";-=<>=-;";

    private static final Logger LOG = LoggerFactory.getLogger(ScriptableScraper.class);
    private static final String LOG_MESSAGE = "ScriptableScraper: ";

    public class ssRetrieve {
        private String url;
        private Charset encoding;
        private String cookies;
        private int retries = 3;
        private int timeout = 3000;

        public ssRetrieve(String url) {
            super();
            setURL(url);
        }

        public ssRetrieve(String url, String encoding, int retries, int timeout, String cookies) {
            super();
            setURL(url);
            setEncoding(encoding);
            setRetries(retries);
            setTimeout(timeout);
            setCookies(cookies);
        }

        public String getURL() {
            return url;
        }

        public void setURL(String url) {
            this.url = url;
        }

        public Charset getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            if (StringUtils.isNotBlank(encoding)) {
                this.encoding = Charset.forName(encoding);
            } else {
                this.encoding = null;
            }
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            if (retries >= 0) {
                this.retries = retries;
            }
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            if (timeout >= 0) {
                this.timeout = timeout;
            }
        }

        public String getCookies() {
            return cookies;
        }

        public void setCookies(String cookies) {
            this.cookies = cookies;
        }
    }

    public class ssParse {
        private String input;
        private String regex;

        public ssParse(String input, String regex) {
            super();
            setInput(input);
            setRegex(regex);
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            if (StringUtils.isNotBlank(input)) {
                this.input = input;
            }
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }
    }

    public class ssReplace {
        private String input;
        private String pattern;
        private String with;

        public ssReplace(String input, String pattern, String with) {
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

    public class ssItem {
        private String type, key;

        public ssItem(String type, String key) {
            super();
            setType(type);
            setKey(key);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public class ssMath {
        String type;
        String value1;
        String value2;
        String result = "int";

        public ssMath(String type, String value1, String value2, String result) {
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
            } else {
                return value2;
            }
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

    public class ssSectionContent {
        private final Logger LOG = LoggerFactory.getLogger(ssSectionContent.class);
        private final String LOG_MESSAGE = "ScriptableScraper.ssSectionContent: ";

        private String name;
        private Map<String, String> attributes = new HashMap<String, String>(2);
        private Map<String, String> variables = new HashMap<String, String>(2);
        private Map<String, String> sets = new HashMap<String, String>(2);
        private Map<String, ssRetrieve> retrieves = new HashMap<String, ssRetrieve>(2);
        private Map<String, ssParse> parses = new HashMap<String, ssParse>(2);
        private Map<String, ssReplace> replaces = new HashMap<String, ssReplace>(2);
        private Map<String, ssMath> math = new HashMap<String, ssMath>(2);
        private Collection<ssItem> items = new ArrayList<ssItem>();

        public ssSectionContent(String name) {
            super();
            setName(name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            if (StringUtils.isNotBlank(name)) {
                this.name = name;
            }
        }

        public Collection<ssItem> getItems() {
            return items;
        }

        public ssItem getItem(int index) {
            return ((index >= 0) && (index < items.size()))?(ssItem) items.toArray()[index]:null;
        }

        public void addItem(String type, String key) {
            items.add(new ssItem(type, key));
        }

        private class preparedVariable {
            public String value = null;
            public int index0 = -1;
            public int index1 = -1;
        }

        private preparedVariable prepareVariable(String name) {
            preparedVariable variable = new preparedVariable();

            if (StringUtils.isNotBlank(name)) {
                String subName = name;
                if (subName.indexOf(':') > -1) {
                    subName = subName.substring(0, subName.indexOf(':'));
                }
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "prepareVariable: subName: " + subName);
                }
                if (subName.indexOf('.') == -1) {
                    Pattern pattern = Pattern.compile("\\[(\\d+)\\](\\[(\\d+)\\])?");
                    Matcher matcher = pattern.matcher(subName);
                    if (matcher.find()) {
                        variable.index0 = Integer.parseInt(matcher.group(1));
                        variable.index1 = matcher.group(3) != null?Integer.parseInt(matcher.group(3)):-1;
                        subName = subName.substring(0, subName.indexOf('['));
                        if (debug) {
                            LOG.debug(LOG_MESSAGE + "prepareVariable: subName: " + subName + " index0: " + Integer.toString(variable.index0) + " index1: " + Integer.toString(variable.index1));
                        }
                        if (this.variables.containsKey(subName)) {
                            variable.value = this.variables.get(subName);
                            return variable;
                        }
                    }
                }
                if (this.variables.containsKey(subName)) {
                    variable.value = this.variables.get(subName);
                    if (debug) {
                        LOG.debug(LOG_MESSAGE + "prepareVariable: subName: " + subName + " value: " + variable.value);
                    }
                }
            }

            return variable;
        }

        public String getVariable(String name) {
            try {
                String result = "";
                boolean safe = false;
                boolean htmldecode = false;
                boolean striptags = false;

                if (StringUtils.isNotBlank(name)) {
                    preparedVariable variable = prepareVariable(name);
                    if (debug) {
                        LOG.debug(LOG_MESSAGE + "getVariable: " + name + " value: " + variable.value + " index0: " + Integer.toString(variable.index0) + " index1: " + Integer.toString(variable.index1));
                    }
                    if (variable.value == null) {
                        return "";
                    }

                    safe = name.indexOf(":safe") > -1;
                    htmldecode = name.indexOf(":htmldecode") > -1;
                    striptags = name.indexOf(":striptags") > -1;

                    if (variable.index0 > -1) {
                        if (variable.value.indexOf(ARRAY_GROUP_DIVIDER) > -1) {
                            List<String> values = Arrays.asList(variable.value.split(ARRAY_GROUP_DIVIDER));
                            if (values.size() > variable.index0) {
                                variable.value = values.get(variable.index0);
                                if (variable.index1 > -1) {
                                    values = Arrays.asList(variable.value.split(ARRAY_ITEM_DIVIDER));
                                    if (values.size() > variable.index1) {
                                        result = values.get(variable.index1);
                                    }
                                } else {
                                    result = variable.value;
                                }
                            }
                        } else if (variable.index1 == -1) {
                            List<String> values = Arrays.asList(variable.value.split(ARRAY_ITEM_DIVIDER));
                            if (values.size() > variable.index0) {
                                result = values.get(variable.index0);
                            }
                        }
                    } else {
                        result = variable.value;
                    }
                }

                if (safe) {
                    result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace(" ", "+");
                    if (name.indexOf(":safe(") > -1) {
                        name = name.substring(name.indexOf(":safe(") + 6);
                        name = name.substring(0, name.indexOf(")"));
                        if (debug) {
                            LOG.debug(LOG_MESSAGE + "encode result to '" + name + "'");
                        }
                        result = URLEncoder.encode(result, name);
                    }
                }
                if (striptags) {
                    result = HTMLTools.removeHtmlTags(result);
                }
                if (htmldecode) {
                    result = HTMLTools.decodeHtml(result);
                }

                if (debug) {
                    LOG.debug(LOG_MESSAGE + "getVariable: result: '" + result + "'");
                }
                return result;
            } catch (IOException error) {
                LOG.error(LOG_MESSAGE + "Failed get variable : " + name);
                LOG.error(LOG_MESSAGE + "Error : " + error.getMessage());
                return "";
            }
        }

        public String getGlobalVariable(String name) {
            return getVariable(name);
        }

        public boolean hasVariable(String name) {
            if (StringUtils.isNotBlank(name)) {
                preparedVariable variable = prepareVariable(name);
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "hasVariable: " + name + " value: " + variable.value + " index0: " + Integer.toString(variable.index0) + " index1: " + Integer.toString(variable.index1));
                }
                if (variable.value == null) {
                    return Boolean.FALSE;
                }
                if (variable.index0 > -1) {
                    if (variable.value.indexOf(ARRAY_GROUP_DIVIDER) > -1) {
                        List<String> values = Arrays.asList(variable.value.split(ARRAY_GROUP_DIVIDER));
                        if (values.size() > variable.index0) {
                            variable.value = values.get(variable.index0);
                            if (variable.index1 > -1) {
                                values = Arrays.asList(variable.value.split(ARRAY_ITEM_DIVIDER));
                                return values.size() > variable.index1;
                            } else {
                                return Boolean.TRUE;
                            }
                        }
                    } else if (variable.index1 == -1) {
                        List<String> values = Arrays.asList(variable.value.split(ARRAY_ITEM_DIVIDER));
                        return values.size() > variable.index0;
                    }
                } else {
                    return Boolean.TRUE;
                }
            }

            return Boolean.FALSE;
        }

        public boolean hasGlobalVariable(String name) {
            return hasVariable(name);
        }

        public void setVariable(String name, String value) {
            if (StringUtils.isNotBlank(name)) {
                this.variables.put(name, value);
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "setVariable to " + this.name + ": name: " + name + " value: " + value);
                }
            }
        }

        public void setGlobalVariable(String name, String value) {
            setVariable(name, value);
        }

        public String getSet(String name) {
            if (StringUtils.isNotBlank(name)) {
                String result = this.sets.get(name);
                if (result != null) {
                    return result;
                }
            }

            return "";
        }

        public void setSet(String name, String value) {
            if (StringUtils.isNotBlank(name)) {
                if (!this.sets.containsKey(name)) {
                    addItem("set", name);
                }
                this.sets.put(name, value);
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "setSet to " + this.name + ": name: " + name + " value: " + value);
                }
            }
        }

        public String getAttribute(String name) {
            if (StringUtils.isNotBlank(name)) {
                String result = this.attributes.get(name);
                if (result != null) {
                    return result;
                }
            }

            return "";
        }

        public void setAttribute(String name, String value) {
            if (StringUtils.isNotBlank(name)) {
                this.attributes.put(name, value);
            }
        }

        public ssRetrieve getRetrieve(String name) {
            if (StringUtils.isNotBlank(name)) {
                return this.retrieves.get(name);
            }

            return null;
        }

        public void setRetrieve(String name, String url, String encoding, int retries, int timeout, String cookies) {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(url)) {
                if (!this.retrieves.containsKey(name)) {
                    addItem("retrieve", name);
                }
                this.retrieves.put(name, new ssRetrieve(url, encoding, retries, timeout, cookies));
            }
        }

        public void setRetrieve(String name, String url) {
            setRetrieve(name, url, "", -1, -1, "");
        }

        public ssParse getParse(String name) {
            if (StringUtils.isNotBlank(name)) {
                return this.parses.get(name);
            }

            return null;
        }

        public void setParse(String name, String input, String regex) {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(input)) {
                if (!this.parses.containsKey(name)) {
                    addItem("parse", name);
                }
                this.parses.put(name, new ssParse(input, regex));
            }
        }

        public ssReplace getReplace(String name) {
            if (StringUtils.isNotBlank(name)) {
                return this.replaces.get(name);
            }

            return null;
        }

        public void setReplace(String name, String input, String pattern, String with) {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(input) && StringUtils.isNotBlank(pattern) && StringUtils.isNotBlank(with)) {
                if (!this.replaces.containsKey(name)) {
                    addItem("replace", name);
                }
                this.replaces.put(name, new ssReplace(input, pattern, with));
            }
        }

        public ssMath getMath(String name) {
            if (StringUtils.isNotBlank(name)) {
                return this.math.get(name);
            }

            return null;
        }

        public void setMath(String name, String type, String value1, String value2, String result) {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(type) && StringUtils.isNotBlank(value1) && StringUtils.isNotBlank(value2) ) {
                if (!this.math.containsKey(name)) {
                    addItem("math", name);
                }
                this.math.put(name, new ssMath(type, value1, value2, result));
            }
        }
    }

    public class ssSection extends ssSectionContent {
        private final Logger LOG = LoggerFactory.getLogger(ssSection.class);
        private final String LOG_MESSAGE = "ScriptableScraper.ssSection: ";

        private Collection<ssSectionContent> content = new ArrayList<ssSectionContent>();
        private ssSectionContent parent;

        public ssSection(String name, ssSectionContent parent) {
            super(name);
            this.parent = parent;
        }

        public void addContent(ssSectionContent content) {
            if (content != null) {
                addItem("content", Integer.toString(this.content.size()));
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "addContent(content): to " + getName() + " add " + content.getName() + " with index " + Integer.toString(this.content.size()) + " parent: " + (parent != null?parent.getName():"null"));
                }
                this.content.add(content);
            }
        }

        public ssSectionContent addContent(String name) {
            ssSectionContent content = new ssSectionContent(name);
            addItem("content", Integer.toString(this.content.size()));
            if (debug) {
                LOG.debug(LOG_MESSAGE + "addContent(name): " + getName() + " index: " + Integer.toString(this.content.size()) + " parent: " + getName());
            }
            this.content.add(content);
            return content;
        }

        public Collection<ssSectionContent> getContent() {
            return content;
        }

        public ssSectionContent getContent(int index) {
            if (index >= 0 && index < content.size()) {
                return (ssSectionContent) content.toArray()[index];
            }

            return null;
        }

        @Override
        public String getVariable(String name) {
            if (StringUtils.isNotBlank(name)) {
                if (super.hasVariable(name)) {
                    return super.getVariable(name);
                } else {
                    return getVariable(name, parent);
                }
            }

            return "";
        }

        public String getVariable(String name, ssSectionContent section) {
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
                } else {
                    return getVariable(name);
                }
            }

            return "";
        }

        @Override
        public boolean hasVariable(String name) {
            if (StringUtils.isNotBlank(name)) {
                if (super.hasVariable(name)) {
                    return true;
                } else {
                    return hasVariable(name, parent);
                }
            }

            return Boolean.FALSE;
        }

        public boolean hasVariable(String name, ssSectionContent section) {
            if (StringUtils.isNotBlank(name) && section != null) {
                return section.hasVariable(name);
            }

            return Boolean.FALSE;
        }

        @Override
        public boolean hasGlobalVariable(String name) {
            if (debug) {
                LOG.debug(LOG_MESSAGE + "hasGlobalVariable: " + name);
            }
            if (StringUtils.isNotBlank(name)) {
                if (parent != null) {
                    return parent.hasGlobalVariable(name);
                } else {
                    return hasVariable(name);
                }
            }

            return Boolean.FALSE;
        }

        @Override
        public void setGlobalVariable(String name, String value) {
            if (StringUtils.isNotBlank(name)) {
                if (parent != null && !getName().equals("action")) {
                    parent.setGlobalVariable(name, value);
                } else {
                    setVariable(name, value);
                }
            }
        }

        private String escapeForRegex(String text) {
            if (text.contains("$")) {
                StringBuffer sb = new StringBuffer();
                for (char c : text.toCharArray()) {
                    if (c == '$') {
                        sb.append("__DOLLAR_SIGN__");
                    } else {
                        sb.append(c);
                    }
                }
                text = sb.toString();
            }

            return text;
        }

        public String compileValue(String value) {
            if (debug) {
                LOG.debug(LOG_MESSAGE + "compileValue: '" + value + "'");
            }
            value = escapeForRegex(value);
            if (debug) {
                LOG.debug(LOG_MESSAGE + "compileValue: escaped: '" + value + "'");
            }
            String result = value;

            int start, end;
            String variable;
            int begin = 0;
            Pattern pattern = Pattern.compile("__DOLLAR_SIGN__\\{([^{}]+)\\}");
            while (value.indexOf("__DOLLAR_SIGN__") > -1) {
                Matcher matcher = pattern.matcher(value);
                start = -1;
                while (matcher.find()) {
                    for (int looper = 0; looper < matcher.groupCount(); looper++) {
                        variable = matcher.group(looper + 1);
                        if (debug) {
                            LOG.debug(LOG_MESSAGE + "compileValue: matcher: '" + variable + "'");
                        }
                        if (hasGlobalVariable(variable) || hasVariable(variable)) {
                            start = result.indexOf("__DOLLAR_SIGN__{" + variable);
                            end = result.indexOf("}", start);
                            if (debug) {
                                LOG.debug(LOG_MESSAGE + "compileValue: start: " + Integer.toString(start) + " end: " + Integer.toString(end));
                            }
                            variable = hasGlobalVariable(variable)?getGlobalVariable(variable):getVariable(variable);
                            if (variable == null || variable.equals("null")) {
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
            if (result.indexOf("|") > -1) {
                List<String> values = Arrays.asList(result.split("\\|"));
                result = "";
                for (int looper = 0; looper < values.size(); looper++) {
                    if (values.get(looper).length() > 0) {
                        if (result.length() > 0) {
                            result += "|";
                        }
                        result += values.get(looper).trim().replaceAll("^\\s+", "");
                    }
                }
            }
            if (debug) {
                LOG.debug(LOG_MESSAGE + "compileValue: compiled: '" + result + "'");
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
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "testCondition: data: " + data + " left: '" + left + "' condition: '" + condition + "' right: '" + right + "'");
                }
                left = compileValue(left);
                right = compileValue(right);
                if (debug) {
                    LOG.debug(LOG_MESSAGE + "testCondition: compilled left: '" + left + "' compilled right: '" + right + "'");
                }
                if (condition.equals("!=")) {
                    return !left.equals(right);
                } else if (condition.equals("=")) {
                    return left.equals(right);
                } else if (condition.equals("<")) {
                    return Float.parseFloat(left) < Float.parseFloat(right);
                } else if (condition.equals(">")) {
                    return Float.parseFloat(left) > Float.parseFloat(right);
                } else {
                    LOG.error(LOG_MESSAGE + "testCondition : Unsupported condition: '" + condition + "'");
                }
            }

            return false;
        }

        public String parseInput(String data, String regex) {
            if (debug) {
                LOG.debug(LOG_MESSAGE + "parseInput: data: '" + data + "'");
                LOG.debug(LOG_MESSAGE + "parseInput: regex: '" + regex + "'");
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

            if (debug) {
                LOG.debug(LOG_MESSAGE + "parseInput: result: '" + result + "'");
            }
            return result;
        }
    }

    private boolean debug = Boolean.FALSE;
    private String name = "";
    private String author = "";
    private String description = "";
    private String id = "";
    private String version = "";
    private String published = "";
    private String type = "";
    private String language = "";
    private ssSection section = new ssSection("global", null);

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        }
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (StringUtils.isNotBlank(type)) {
            this.type = type;
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public ssSection getSection() {
        return this.section;
    }

    public Collection<ssSectionContent> getSections(String title) {
        return getSections(title, "");
    }

    public Collection<ssSectionContent> getSections(String title, String name) {
        Collection<ssSectionContent> result = new ArrayList<ssSectionContent>();
        for (ssSectionContent section : this.section.getContent()) {
            if (section.getName().equals(title)) {
                if (StringUtils.isBlank(name) || section.getAttribute("name").equals(name)) {
                    result.add(section);
                }
            }
        }

        return result;
    }

    public ssSection addSection(String name, ssSection parent) {
        ssSection section = new ssSection(name, parent);
        parent.addContent(section);

        return section;
    }
}
