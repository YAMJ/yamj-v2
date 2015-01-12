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

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

/**
 * Set of classes to prepare and processing ScriptableScraper format
 *
 * @author ilgizar
 */
public class ScriptableScraper {

    public static final String ARRAY_ITEM_DIVIDER = ",-=<>=-,";
    public static final String ARRAY_GROUP_DIVIDER = ";-=<>=-;";

    private boolean debug = Boolean.FALSE;
    private String name = "";
    private String author = "";
    private String description = "";
    private String id = "";
    private String version = "";
    private String published = "";
    private String type = "";
    private String language = "";
    private final SectionSS section = new SectionSS("global", null);

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        section.setDebug(debug);
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

    public SectionSS getSection() {
        return this.section;
    }

    public Collection<SectionContentSS> getSections(String title) {
        return getSections(title, "");
    }

    public Collection<SectionContentSS> getSections(String title, String name) {
        Collection<SectionContentSS> result = new ArrayList<SectionContentSS>();
        for (SectionContentSS newSection : this.section.getContent()) {
            if (newSection.getName().equals(title)) {
                if (StringUtils.isBlank(name) || newSection.getAttribute("name").equals(name)) {
                    result.add(newSection);
                }
            }
        }

        return result;
    }

    public SectionSS addSection(String name, SectionSS parent) {
        SectionSS newSection = new SectionSS(name, parent);
        parent.addContent(newSection);

        return newSection;
    }
}
