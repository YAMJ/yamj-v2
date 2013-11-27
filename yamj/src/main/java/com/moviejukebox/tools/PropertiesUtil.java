/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.tools;

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
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.log4j.Logger;

/**
 *
 * @author altman.matthew
 */
public final class PropertiesUtil {

    private static final Logger LOG = Logger.getLogger(PropertiesUtil.class);
    private static final String LOG_MESSAGE = "PropertiesUtil: ";
    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static final String PREFERENCES_FILENAME = "preferences.xsl";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    private static final Properties props = new Properties();

    private PropertiesUtil() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    public static boolean setPropertiesStreamName(String streamName) {
        return setPropertiesStreamName(streamName, Boolean.TRUE);
    }

    public static boolean setPropertiesStreamName(String streamName, boolean warnFatal) {
        LOG.info("Using properties file " + streamName);
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
                LOG.error("Failed loading file " + streamName + ": Please check your configuration. The properties file should be in the classpath.", error);
            } else {
                LOG.debug("Warning (non-fatal): User properties file '" + streamName + "', not found.");
            }
            return Boolean.FALSE;
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
        return Boolean.TRUE;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        return convertBooleanProperty(key, props.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as integer
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, int defaultValue) {
        return convertIntegerProperty(key, props.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, long defaultValue) {
        return convertLongProperty(key, props.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, float defaultValue) {
        return convertFloatProperty(key, props.getProperty(key), defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static String getReplacedProperty(String newKey, String oldKey, String defaultValue) {
        return getReplacedKeyValue(newKey, oldKey);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static boolean getReplacedBooleanProperty(String newKey, String oldKey, boolean defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertBooleanProperty(newKey, property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static int getReplacedIntProperty(String newKey, String oldKey, int defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertIntegerProperty(newKey, property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static long getReplacedLongProperty(String newKey, String oldKey, long defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertLongProperty(oldKey, property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static float getReplacedFloatProperty(String newKey, String oldKey, float defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertFloatProperty(newKey, property, defaultValue);
    }

    /**
     * Look for both keys in the property list and warn if the old one was found
     *
     * @param newKey
     * @param oldKey
     * @return
     */
    private static String getReplacedKeyValue(String newKey, String oldKey) {
        String oldProperty = props.getProperty(oldKey);
        String newProperty = props.getProperty(newKey);
        String returnValue;

        if (newProperty == null && oldProperty != null) {
            // We found the old property, but not the new
            LOG.warn("Property '" + oldKey + "' has been deprecated and will be removed; please use '" + newKey + "' instead.");
            returnValue = oldProperty;
        } else if (newProperty != null && oldProperty != null) {
            // We found both properties, so warn about the old one
            LOG.warn("Property '" + oldKey + "' is no longer used, but was found in your configuration files, please remove it.");
            returnValue = newProperty;
        } else {
            returnValue = newProperty;
        }

        return returnValue;
    }

    /**
     * Convert the value to a Float
     *
     * Outputs warning if there was an exception during the conversion
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static float convertFloatProperty(String key, String valueToConvert, float defaultValue) {
        float value = defaultValue;

        if (StringUtils.isNotBlank(valueToConvert)) {
            try {
                value = Float.parseFloat(valueToConvert);
            } catch (NumberFormatException ex) {
                LOG.warn("Failed to convert property '" + key + "', value '" + valueToConvert + "' to a float number.");
            }
        }

        return value;
    }

    /**
     * Convert the value to a Long
     *
     * Outputs warning if there was an exception during the conversion
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static long convertLongProperty(String key, String valueToConvert, long defaultValue) {
        long value = defaultValue;

        if (StringUtils.isNotBlank(valueToConvert)) {
            try {
                value = Long.parseLong(valueToConvert);
            } catch (NumberFormatException ex) {
                LOG.warn("Failed to convert property '" + key + "', value '" + valueToConvert + "' to a long number.");
            }
        }

        return value;
    }

    /**
     * Convert the value to a Integer
     *
     * Outputs warning if there was an exception during the conversion
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static int convertIntegerProperty(String key, String valueToConvert, int defaultValue) {
        int value = defaultValue;

        if (StringUtils.isNotBlank(valueToConvert)) {
            try {
                value = Integer.parseInt(valueToConvert);
            } catch (NumberFormatException ex) {
                LOG.warn("Failed to convert property '" + key + "', value '" + valueToConvert + "' to an integer number.");
            }
        }

        return value;
    }

    /**
     * Convert the value to a Boolean
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static boolean convertBooleanProperty(String key, String valueToConvert, boolean defaultValue) {
        boolean value = defaultValue;
        if (StringUtils.isNotBlank(valueToConvert)) {
            value = Boolean.parseBoolean(valueToConvert);
        }
        return value;
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

    public static void setProperty(String key, boolean value) {
        props.setProperty(key, Boolean.toString(value));
    }

    public static void setProperty(String key, int value) {
        props.setProperty(key, Integer.toString(value));
    }

    public static void setProperty(String key, long value) {
        props.setProperty(key, Long.toString(value));
    }

    /**
     * Store list (ordered) and keyword map.
     */
    public static class KeywordMap extends HashMap<String, String> {

        private static final long serialVersionUID = 1L;
        private final transient List<String> keywords = new ArrayList<String>();

        public List<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * Collect keywords list and appropriate keyword values. Example: my.languages = EN,FR my.languages.EN = English my.languages.FR
     * = French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @param defaultValue
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap keywordMap = new KeywordMap();

        String languages = getProperty(prefix, defaultValue);
        if (!isBlank(languages)) {
            for (String lang : languages.split("[ ,]+")) {
                lang = StringUtils.trimToNull(lang);
                if (lang == null) {
                    continue;
                }
                keywordMap.keywords.add(lang);
                String values = getProperty(prefix + "." + lang);
                if (values != null) {
                    keywordMap.put(lang, values);
                }
            }
        }

        return keywordMap;
    }

    public static void writeProperties() {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        Writer out = null;

        // Save the properties in order
        List<String> propertiesList = new ArrayList<String>();
        for (Object propertyObject : props.keySet()) {
            propertiesList.add((String) propertyObject);
        }
        // Sort the properties
        Collections.sort(propertiesList);

        try {
            LOG.debug(LOG_MESSAGE + "Writing skin preferences file to " + getPropertiesFilename(Boolean.TRUE));

            fos = new FileOutputStream(getPropertiesFilename(Boolean.TRUE));
            osw = new OutputStreamWriter(fos, "UTF-8");
            out = new BufferedWriter(osw);

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<!-- This file is written automatically by YAMJ -->\n");
            out.write("<!-- Last updated: " + (new Date()) + " -->\n");

            out.write("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n");
            out.write("    <xsl:output method=\"xml\" omit-xml-declaration=\"yes\" />\n");

            for (String property : propertiesList) {
                if (!property.startsWith("API_KEY") && !property.contains("#")) {
                    out.write("    <xsl:param name=\"" + property + "\" />\n");
                }
            }

            out.write("</xsl:stylesheet>\n");
            out.flush();
        } catch (IOException error) {
            // Can't write to file
            LOG.error(LOG_MESSAGE + "Can't write to file");
            LOG.error(SystemTools.getStackTrace(error));
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }

            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }

            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }
        }
    }

    public static String getPropertiesFilename(boolean fullPath) {
        if (fullPath) {
            return StringTools.appendToPath(getProperty("mjb.skin.dir", "./skins/default"), PREFERENCES_FILENAME);
        } else {
            return PREFERENCES_FILENAME;
        }
    }
}
