/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.tools;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author altman.matthew
 */
public class PropertiesUtil {

    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Properties props = new Properties();
    private static String propertiesFilename = "preferences.xsl";

    public static boolean setPropertiesStreamName(String streamName) {
        return setPropertiesStreamName(streamName, true);
    }

    public static boolean setPropertiesStreamName(String streamName, boolean warnFatal) {
        logger.info("Using properties file " + streamName);
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);
        Reader reader = null;

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET);
            props.load(reader);
        } catch (IOException error) {
            // Output a warning if required.
            if (warnFatal) {
                logger.error("Failed loading file " + streamName + ": Please check your configuration. The properties file should be in the classpath.");
            } else {
                logger.warn("Warning (non-fatal): User properties file '" + streamName + "', not found.");
            }
            return false;
        } finally {
            try {
                if (propertiesStream != null) {
                    propertiesStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(
                key, defaultValue);
    }

    /**
     * Return the key property as an integer
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException nfe) {
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * Return the key property as an long
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Long.parseLong(property);
        } catch (NumberFormatException nfe) {
            return Long.parseLong(defaultValue);
        }
    }

    /**
     * Return the key property as a boolean
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Boolean.parseBoolean(property);
        } catch (NumberFormatException nfe) {
            return Boolean.parseBoolean(defaultValue);
        }
    }

    /**
     * Return the key property as a float
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();

        try {
            return Float.parseFloat(property);
        } catch (NumberFormatException nfe) {
            return Float.parseFloat(defaultValue);
        }
    }

    // Issue 309
    public static Set<Entry<Object, Object>> getEntrySet() {
        // Issue 728
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<Object, Object>(props).entrySet();
    }

    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    /**
     * Store list (ordered) and keyword map.
     */
    @SuppressWarnings("serial")
    public static class KeywordMap extends HashMap<String, String> {

        private final List<String> keywords = new ArrayList<String>();

        public List<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * Collect keywords list and appropriate keyword values.
     * Example:
     * my.languages = EN,FR
     * my.languages.EN = English
     * my.languages.FR = French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap m = new KeywordMap();

        String languages = getProperty(prefix, defaultValue);
        if (!isBlank(languages)) {
            for (String lang : languages.split("[ ,]+")) {
                lang = StringUtils.trimToNull(lang);
                if (lang == null) {
                    continue;
                }
                m.keywords.add(lang);
                String values = getProperty(prefix + "." + lang);
                if (values != null) {
                    m.put(lang, values);
                }
            }
        }

        return m;
    }

    public static void writeProperties() {
        Writer out = null;

        // Save the properties in order
        List<String> propertiesList = new ArrayList<String>();
        for (Object propertyObject : props.keySet()) {
            propertiesList.add((String) propertyObject);
        }
        // Sort the properties
        Collections.sort(propertiesList);

        try {
            logger.debug("PropertiesUtil: Writing skin preferences file to " + getPropertiesFilename(true));

            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getPropertiesFilename(true)), "UTF-8"));

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<!-- This file is written automatically by YAMJ -->\n");
            out.write("<!-- Last updated: " + (new Date()) + " -->\n");

            out.write("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n");
            out.write("    <xsl:output method=\"xml\" omit-xml-declaration=\"yes\" />\n");

            for (String property : propertiesList) {
                if (!property.startsWith("API_KEY")) {
                    out.write("    <xsl:param name=\"" + property + "\" />\n");
                }
            }

            out.write("</xsl:stylesheet>\n");
            out.flush();
            out.close();
        } catch (IOException error) {
            // Can't write to file
            logger.error("PropertiesUtil: Can't write to file");
            logger.error(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            // Some other error
            logger.error("PropertiesUtil: Error with file");
            logger.error(SystemTools.getStackTrace(error));
        } finally {
            // Free up memory
            propertiesList = null;

            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception error) {
                    // ignore this error
                }
            }
        }

    }

    public static String getPropertiesFilename(boolean fullPath) {
        if (fullPath) {
            return StringTools.appendToPath(getProperty("mjb.skin.dir", "./skins/default"), propertiesFilename);
        } else {
            return propertiesFilename;
        }
    }
}
