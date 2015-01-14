/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.model.scriptablescraper;

import static com.moviejukebox.model.scriptablescraper.ScriptableScraper.ARRAY_GROUP_DIVIDER;
import static com.moviejukebox.model.scriptablescraper.ScriptableScraper.ARRAY_ITEM_DIVIDER;
import com.moviejukebox.tools.HTMLTools;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class SectionContentSS extends AbstractScriptableScraper {

    private final Logger LOG = LoggerFactory.getLogger(SectionContentSS.class);

    private String name;
    private final Map<String, String> attributes = new HashMap<>(2);
    private final Map<String, String> variables = new HashMap<>(2);
    private final Map<String, String> sets = new HashMap<>(2);
    private final Map<String, RetrieveSS> retrieves = new HashMap<>(2);
    private final Map<String, ParseSS> parses = new HashMap<>(2);
    private final Map<String, ReplaceSS> replaces = new HashMap<>(2);
    private final Map<String, MathSS> math = new HashMap<>(2);
    private final Collection<ItemSS> items = new ArrayList<>();

    public SectionContentSS(String name) {
        super();
        setName(name);
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        }
    }

    public Collection<ItemSS> getItems() {
        return items;
    }

    public ItemSS getItem(int index) {
        return ((index >= 0) && (index < items.size())) ? (ItemSS) items.toArray()[index] : null;
    }

    public void addItem(String type, String key) {
        items.add(new ItemSS(type, key));
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
            if (isDebug()) {
                LOG.debug("prepareVariable: subName: {}", subName);
            }
            if (subName.indexOf('.') == -1) {
                Pattern pattern = Pattern.compile("\\[(\\d+)\\](\\[(\\d+)\\])?");
                Matcher matcher = pattern.matcher(subName);
                if (matcher.find()) {
                    variable.index0 = Integer.parseInt(matcher.group(1));
                    variable.index1 = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : -1;
                    subName = subName.substring(0, subName.indexOf('['));
                    if (isDebug()) {
                        LOG.debug("prepareVariable: subName: {} index0: {} index1: {}", subName, variable.index0, variable.index1);
                    }
                    if (this.variables.containsKey(subName)) {
                        variable.value = this.variables.get(subName);
                        return variable;
                    }
                }
            }
            if (this.variables.containsKey(subName)) {
                variable.value = this.variables.get(subName);
                if (isDebug()) {
                    LOG.debug("prepareVariable: subName: {} value: {}", subName, variable.value);
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
                if (isDebug()) {
                    LOG.debug("getVariable: {} value: {} index0: {} index1: {}" + name, variable.value, variable.index0, variable.index1);
                }
                if (variable.value == null) {
                    return "";
                }

                safe = name.contains(":safe");
                htmldecode = name.contains(":htmldecode");
                striptags = -1 <= name.indexOf(":striptags");

                if (variable.index0 > -1) {
                    if (variable.value.contains(ARRAY_GROUP_DIVIDER)) {
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
                if (name.contains(":safe(")) {
                    name = name.substring(name.indexOf(":safe(") + 6);
                    name = name.substring(0, name.indexOf(")"));
                    if (isDebug()) {
                        LOG.debug("encode result to ''{}'", name);
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

            if (isDebug()) {
                LOG.debug("getVariable: result: '{}'", result);
            }
            return result;
        } catch (IOException error) {
            LOG.error("Failed get variable : {}", name);
            LOG.error("Error : {}", error.getMessage());
            return "";
        }
    }

    public String getGlobalVariable(String name) {
        return getVariable(name);
    }

    public boolean hasVariable(String name) {
        if (StringUtils.isNotBlank(name)) {
            preparedVariable variable = prepareVariable(name);
            if (isDebug()) {
                LOG.debug("hasVariable: {} value: {} index0: {} index1: {}", name, variable.value, variable.index0, variable.index1);
            }
            if (variable.value == null) {
                return Boolean.FALSE;
            }
            if (variable.index0 > -1) {
                if (variable.value.contains(ARRAY_GROUP_DIVIDER)) {
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
            if (isDebug()) {
                LOG.debug("setVariable to {}: name: {} value: {}", this.name, name, value);
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
            if (isDebug()) {
                LOG.debug("setSet to {}: name: {} value: {}", this.name, name, value);
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

    public RetrieveSS getRetrieve(String name) {
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
            this.retrieves.put(name, new RetrieveSS(url, encoding, retries, timeout, cookies));
        }
    }

    public void setRetrieve(String name, String url) {
        setRetrieve(name, url, "", -1, -1, "");
    }

    public ParseSS getParse(String name) {
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
            this.parses.put(name, new ParseSS(input, regex));
        }
    }

    public ReplaceSS getReplace(String name) {
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
            this.replaces.put(name, new ReplaceSS(input, pattern, with));
        }
    }

    public MathSS getMath(String name) {
        if (StringUtils.isNotBlank(name)) {
            return this.math.get(name);
        }

        return null;
    }

    public void setMath(String name, String type, String value1, String value2, String result) {
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(type) && StringUtils.isNotBlank(value1) && StringUtils.isNotBlank(value2)) {
            if (!this.math.containsKey(name)) {
                addItem("math", name);
            }
            this.math.put(name, new MathSS(type, value1, value2, result));
        }
    }
}
