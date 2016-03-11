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
package com.moviejukebox.reader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.moviejukebox.model.scriptablescraper.ScriptableScraper;
import com.moviejukebox.model.scriptablescraper.SectionSS;
import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.SystemTools;

/**
 * base on MovieJukeboxXMLReader
 *
 * @author ilgizar
 */
public class ScriptableScraperXMLReader {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptableScraperXMLReader.class);
    private static final List<String> SUBSECTIONS = Arrays.asList("if", "loop");

    public Element getElementByName(Element inData, String name) {
        NodeList nlData = inData.getElementsByTagName(name);
        if (nlData.getLength() > 0) {
            Node nData = nlData.item(0);
            if (nData.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) nData;
            }
        }

        return null;
    }

    public Element getElementByName(Document inData, String name) {
        NodeList nlData = inData.getElementsByTagName(name);
        if (nlData.getLength() > 0) {
            Node nData = nlData.item(0);
            if (nData.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) nData;
            }
        }

        return null;
    }

    /**
     * Parse ScriptableScraper XML file
     *
     * @param xmlFile
     * @param ssData
     * @return
     */
    public boolean parseXML(File xmlFile, ScriptableScraper ssData) {
        Document xmlDoc;

        try {
            xmlDoc = DOMHelper.getDocFromFile(xmlFile);
        } catch (MalformedURLException error) {
            LOG.error("Failed parsing XML ({}) for ScriptableScraper. Please fix it.", xmlFile.getName());
            LOG.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (IOException error) {
            LOG.error("Failed parsing XML ({}) for ScriptableScraper. Please fix it.", xmlFile.getName());
            LOG.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        } catch (ParserConfigurationException | SAXException error) {
            LOG.error("Failed parsing XML ({}) for ScriptableScraper. Please fix it.", xmlFile.getName());
            LOG.error(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        Element eData = getElementByName(xmlDoc, "ScriptableScraper");

        Element eDetails = getElementByName(eData, "details");
        ssData.setName(DOMHelper.getValueFromElement(eDetails, "name"));
        ssData.setAuthor(DOMHelper.getValueFromElement(eDetails, "author"));
        ssData.setDescription(DOMHelper.getValueFromElement(eDetails, "description"));
        ssData.setID(DOMHelper.getValueFromElement(eDetails, "id"));
        ssData.setType(DOMHelper.getValueFromElement(eDetails, "type"));
        ssData.setLanguage(DOMHelper.getValueFromElement(eDetails, "language"));

        Element eElement = getElementByName(eDetails, "version");
        if (eElement != null) {
            ssData.setVersion(eElement.getAttribute("major") + "." + eElement.getAttribute("minor") + "." + eElement.getAttribute("point"));
        }

        eElement = getElementByName(eDetails, "published");
        if (eElement != null) {
            ssData.setPublished(eElement.getAttribute("day") + "." + eElement.getAttribute("month") + "." + eElement.getAttribute("year"));
        }

        NodeList nlSections = eData.getElementsByTagName("action");
        for (int loopSection = 0; loopSection < nlSections.getLength(); loopSection++) {
            fillSection(ssData, ssData.getSection(), nlSections.item(loopSection), "action");
        }

        return Boolean.TRUE;
    }

    private static String getAttribute(Node eSection, String name) {
        int looper;
        Node nElement;
        NamedNodeMap mapElement = eSection.getAttributes();
        for (looper = 0; looper < mapElement.getLength(); looper++) {
            nElement = mapElement.item(looper);
            if (nElement.getNodeName().equals(name)) {
                return nElement.getNodeValue();
            }
        }

        return "";
    }

    private static boolean hasAttribute(Node eSection, String name) {
        int looper;
        Node nElement;
        NamedNodeMap mapElement = eSection.getAttributes();
        for (looper = 0; looper < mapElement.getLength(); looper++) {
            nElement = mapElement.item(looper);
            if (nElement.getNodeName().equals(name)) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    private static String escapeRegex(String value) {
        return value.replaceAll("(\\\\[AbBEGQzZ])", "\\\\$1");
    }

    private void fillSection(ScriptableScraper ssData, SectionSS parentSection, Node eSection, String name) {
        Node nElement;
        int looper;
        String eName;

        SectionSS resSection = ssData.addSection(name, parentSection);

        NamedNodeMap mapElement = eSection.getAttributes();
        for (looper = 0; looper < mapElement.getLength(); looper++) {
            nElement = mapElement.item(looper);
            resSection.setAttribute(nElement.getNodeName(), nElement.getNodeValue());
        }

        NodeList nlElements = eSection.getChildNodes();
        for (looper = 0; looper < nlElements.getLength(); looper++) {
            nElement = nlElements.item(looper);
            eName = nElement.getNodeName();
            if ("set".equals(eName)) {
                resSection.setSet(
                        getAttribute(nElement, "name"),
                        hasAttribute(nElement, "value") ? getAttribute(nElement, "value") : nElement.getTextContent().trim().replaceAll("^\\s+", "")
                );
            } else if ("retrieve".equals(eName)) {
                resSection.setRetrieve(
                        getAttribute(nElement, "name"),
                        getAttribute(nElement, "url"),
                        hasAttribute(nElement, "encoding") ? getAttribute(nElement, "encoding") : "",
                        hasAttribute(nElement, "retries") ? Integer.parseInt(getAttribute(nElement, "retries")) : -1,
                        hasAttribute(nElement, "timeout_increment") ? Integer.parseInt(getAttribute(nElement, "timeout_increment")) : -1,
                        hasAttribute(nElement, "cookies") ? getAttribute(nElement, "cookies") : ""
                );
            } else if ("parse".equals(eName)) {
                resSection.setParse(
                        getAttribute(nElement, "name"),
                        getAttribute(nElement, "input"),
                        hasAttribute(nElement, "regex") ? escapeRegex(getAttribute(nElement, "regex")) : ""
                );
            } else if ("replace".equals(eName)) {
                resSection.setReplace(
                        getAttribute(nElement, "name"),
                        getAttribute(nElement, "input"),
                        getAttribute(nElement, "pattern"),
                        getAttribute(nElement, "with")
                );
            } else if ("add".equals(eName) || "subtract".equals(eName) || "multiply".equals(eName) || "divide".equals(eName)) {
                resSection.setMath(
                        getAttribute(nElement, "name"),
                        eName,
                        getAttribute(nElement, "value1"),
                        getAttribute(nElement, "value2"),
                        getAttribute(nElement, "result_type")
                );
            } else if (SUBSECTIONS.contains(eName)) {
                fillSection(ssData, resSection, nElement, eName);
            } else if ("#text".equals(eName) || "#comment".equals(eName)) {
                // nothing to do
            } else {
                LOG.error("Unknown section: {}", eName);
            }
        }
    }
}
